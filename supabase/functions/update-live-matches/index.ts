import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.3";

const BZZOIRO_API_KEY = Deno.env.get("BZZOIRO_API_KEY") || "";
const SUPABASE_URL = Deno.env.get("SUPABASE_URL") || "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";

serve(async (req) => {
  try {
    // 1. Initialize Supabase Client with Admin privileges
    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, {
      auth: { persistSession: false }
    });

    // 2. Fetch matches from DB that might be live (4h ago up to 15m in future)
    const now = new Date();
    const fourHoursAgo = new Date(now.getTime() - 4 * 60 * 60 * 1000).toISOString();
    const fifteenMinsFuture = new Date(now.getTime() + 15 * 60 * 1000).toISOString();

    const { data: activeMatches, error: matchError } = await supabase
      .from("matches")
      .select("api_fixture_id")
      .neq("status", "finished")
      .neq("status", "interrupted")
      .gte("scheduled_at", fourHoursAgo)
      .lte("scheduled_at", fifteenMinsFuture);

    if (matchError) {
      throw matchError;
    }

    if (!activeMatches || activeMatches.length === 0) {
      return new Response(JSON.stringify({ message: "No active matches found in DB window." }), {
        headers: { "Content-Type": "application/json" },
      });
    }

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

    await Promise.all(fetchPromises);

    if (events.length === 0) {
      return new Response(JSON.stringify({ message: "No live events found from API.", activeMatches }), {
        headers: { "Content-Type": "application/json" },
      });
    }

    const updatePromises = events.map(async (event: any) => {
      const apiId = event.id;
      let dbStatus = "scheduled";

      let rawStatus = String(event.status).toLowerCase();
      let rawPeriod = event.period ? String(event.period).toLowerCase() : "";

      if (rawStatus === "halftime" || rawStatus === "ht" || rawPeriod === "halftime" || rawPeriod === "ht" || rawPeriod === "half-time") {
        dbStatus = "halftime";
      } else if (rawStatus === "finished" || rawStatus === "ended" || rawPeriod === "finished" || rawStatus === "ft") {
        dbStatus = "finished";
      } else if (
        rawStatus === "inprogress" || rawStatus === "live" || rawStatus === "1st_half" || rawStatus === "2nd_half" ||
        rawPeriod === "1st_half" || rawPeriod === "2nd_half" || rawPeriod === "1t" || rawPeriod === "2t" ||
        rawPeriod === "extratime" || rawPeriod === "aet" || rawPeriod === "penalties"
      ) {
        dbStatus = "live";
      } else if (rawStatus === "cancelled" || rawStatus === "postponed") {
        dbStatus = "interrupted";
      } else {
        dbStatus = "scheduled";
      }

      // 4. Update the matches table
      const { error } = await supabase
        .from("matches")
        .update({
          home_score: event.home_score || 0,
          away_score: event.away_score || 0,
          status: dbStatus,
          minute_played: (dbStatus === "live" || dbStatus === "halftime") ? (event.current_minute || null) : null
        })
        .eq("api_fixture_id", apiId);

      if (error) {
        console.error(`Error updating match ${apiId}:`, error);
      }
    });

    await Promise.all(updatePromises);

    return new Response(JSON.stringify({ message: `Successfully processed ${events.length} events.` }), {
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
