import { createClient } from "npm:@supabase/supabase-js@2";

const BZZOIRO_API_KEY = Deno.env.get("BZZOIRO_API_KEY") || "";
const SUPABASE_URL = Deno.env.get("SUPABASE_URL") || "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";

Deno.serve(async (req) => {
  try {
    // 1. Initialize Supabase Client with Admin privileges
    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, {
      auth: { persistSession: false }
    });

    // 2. Fetch matches from DB that might be live or recently finished (6h ago up to 15m in future)
    // We include 'finished' in the window (not excluded) so that the final score can still be corrected
    // if the API updates it after marking the match as done.
    const now = new Date();
    const sixHoursAgo = new Date(now.getTime() - 6 * 60 * 60 * 1000).toISOString();
    const fifteenMinsFuture = new Date(now.getTime() + 15 * 60 * 1000).toISOString();

    const { data: activeMatches, error: matchError } = await supabase
      .from("matches")
      .select("api_fixture_id, home_score, away_score, home_score_90, away_score_90, status, scheduled_at")
      .neq("status", "interrupted")
      .gte("scheduled_at", sixHoursAgo)
      .lte("scheduled_at", fifteenMinsFuture);

    if (matchError) {
      throw matchError;
    }

    if (!activeMatches || activeMatches.length === 0) {
      return new Response(JSON.stringify({ message: "No active matches found in DB window." }), {
        headers: { "Content-Type": "application/json" },
      });
    }

    const matchesMap = new Map(activeMatches.map(m => [m.api_fixture_id, m]));

    // 3. Fetch data from BZZOIRO API for each match individually
    const events: any[] = [];
    const fetchPromises = activeMatches.map(async (match) => {
      const matchId = match.api_fixture_id;
      const response = await fetch(`https://sports.bzzoiro.com/api/v2/events/${matchId}`, {
        method: "GET",
        headers: {
          "Authorization": `Token ${BZZOIRO_API_KEY}`
        }
      });

      if (!response.ok) {
        console.error(`BZZOIRO API responded with status: ${response.status} for match ${matchId}`);
        return;
      }

      const data = await response.json();
      const matchEvents = data.events ? data.events : (Array.isArray(data) ? data : [data]);
      events.push(...matchEvents);
    });

    const debugLogs: any[] = [];

    await Promise.all(fetchPromises);

    if (events.length === 0) {
      return new Response(JSON.stringify({ message: "No live events found from API.", activeMatches }), {
        headers: { "Content-Type": "application/json" },
      });
    }

    const updatePromises = events.map(async (event: any) => {
      const apiId = String(event.id);
      const matchInDb = matchesMap.get(apiId);
      if (!matchInDb) return;

      let dbStatus = "scheduled";

      let rawStatus = String(event.status).toLowerCase();
      let rawPeriod = event.period ? String(event.period).toLowerCase() : "";

      // Time lock: only allow "live" if the match's scheduled time has actually arrived or passed
      const scheduledTime = new Date(matchInDb.scheduled_at).getTime();
      const isBeforeKickoff = Date.now() < scheduledTime;

      if (rawStatus === "halftime" || rawStatus === "ht" || rawPeriod === "halftime" || rawPeriod === "ht" || rawPeriod === "half-time") {
        dbStatus = "halftime";
      } else if (rawStatus === "finished" || rawStatus === "ended" || rawPeriod === "finished" || rawStatus === "ft") {
        dbStatus = "finished";
      } else if (rawPeriod === "extratime" || rawPeriod === "aet") {
        dbStatus = "extratime";
      } else if (rawPeriod === "et_halftime") {
        dbStatus = "et_halftime";
      } else if (rawPeriod === "penalties") {
        dbStatus = "penalties";
      } else if (
        rawStatus === "inprogress" || rawStatus === "live" || rawStatus === "1st_half" || rawStatus === "2nd_half" ||
        rawPeriod === "1st_half" || rawPeriod === "2nd_half" || rawPeriod === "1t" || rawPeriod === "2t"
      ) {
        dbStatus = isBeforeKickoff ? "scheduled" : "live";
      } else if (rawStatus === "cancelled" || rawStatus === "postponed") {
        dbStatus = "interrupted";
      } else {
        dbStatus = "scheduled";
      }

      let updateData: any = {
        status: dbStatus,
        minute_played: ["live", "halftime", "extratime", "et_halftime", "penalties"].includes(dbStatus) ? (event.current_minute ?? null) : null
      };

      const apiHomeScore = event.home_score ?? null;
      const apiAwayScore = event.away_score ?? null;

      if (["extratime", "et_halftime", "penalties"].includes(dbStatus)) {
        // In extra time: the API score includes ET goals, store it in home_score_et
        if (apiHomeScore !== null) updateData.home_score_et = apiHomeScore;
        if (apiAwayScore !== null) updateData.away_score_et = apiAwayScore;

        // Freeze the 90-min score only once (when first entering ET)
        if (matchInDb.home_score_90 === null) {
          updateData.home_score_90 = matchInDb.home_score ?? 0;
          updateData.away_score_90 = matchInDb.away_score ?? 0;
        }
      } else if (dbStatus === "finished") {
        // Always update the final score when finished so corrections from the API land correctly
        if (apiHomeScore !== null) updateData.home_score = apiHomeScore;
        if (apiAwayScore !== null) updateData.away_score = apiAwayScore;

        // If home_score_90 was never set (game ended in normal time), set it now from the final score
        if (matchInDb.home_score_90 === null) {
          // Only set 90-min score if there was no extra time recorded (i.e. pure normal-time finish)
          if (matchInDb.home_score_et === null) {
            updateData.home_score_90 = apiHomeScore ?? matchInDb.home_score ?? 0;
            updateData.away_score_90 = apiAwayScore ?? matchInDb.away_score ?? 0;
          }
        }
      } else {
        // Live / halftime: just update the running score
        if (apiHomeScore !== null) updateData.home_score = apiHomeScore;
        if (apiAwayScore !== null) updateData.away_score = apiAwayScore;
      }

      // 4. Update the matches table
      const { data, error, count } = await supabase
        .from("matches")
        .update(updateData)
        .eq("api_fixture_id", apiId)
        .select("id");

      debugLogs.push({
        match: apiId,
        updateData,
        db_updated: data?.length || 0,
        error: error ? error.message : null
      });

      console.log(`Match ${apiId}: updated=${data?.length || 0}, error=${JSON.stringify(error)}`);

      if (error) {
        console.error(`Error updating match ${apiId}:`, error);
      }
    });

    await Promise.all(updatePromises);

    return new Response(JSON.stringify({ 
      message: `Successfully processed ${events.length} events.`,
      debug: debugLogs
    }), {
      headers: { "Content-Type": "application/json" },
    });
  } catch (error) {
    console.error("Error in update-live-matches:", error);
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }
});
