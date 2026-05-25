import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.3";

const BZZOIRO_API_KEY = Deno.env.get("BZZOIRO_API_KEY") || "";
const SUPABASE_URL = Deno.env.get("SUPABASE_URL") || "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";

serve(async (req) => {
  try {
    // 1. Fetch data from BZZOIRO API for active leagues
    const ACTIVE_LEAGUES = [27, 9];
    const events: any[] = [];

    for (const leagueId of ACTIVE_LEAGUES) {
      const response = await fetch(`https://sports.bzzoiro.com/api/v2/events/live/?league_id=${leagueId}`, {
        method: "GET",
        headers: {
          "Authorization": `Token ${BZZOIRO_API_KEY}`
        }
      });

      if (!response.ok) {
        console.error(`BZZOIRO API responded with status: ${response.status} for league ${leagueId}`);
        continue; // Skip this league and try the next one
      }

      const data = await response.json();
      if (data.events) {
        events.push(...data.events);
      }
    }

    if (events.length === 0) {
      return new Response(JSON.stringify({ message: "No live events found." }), {
        headers: { "Content-Type": "application/json" },
      });
    }

    // 2. Initialize Supabase Client with Admin privileges
    // We use the Service Role Key to bypass Row Level Security (RLS) for background updates
    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, {
      auth: { persistSession: false }
    });

    // 3. Process and map the events
    const updatePromises = events.map(async (event: any) => {
      const apiId = event.id;
      let dbStatus = "scheduled";

      switch (event.status) {
        case "notstarted":
          dbStatus = "scheduled";
          break;
        case "inprogress":
        case "penalties":
          dbStatus = "live";
          break;
        case "finished":
          dbStatus = "finished";
          break;
        default:
          dbStatus = "scheduled";
      }

      // 4. Update the matches table
      const { error } = await supabase
        .from("matches")
        .update({
          home_score: event.home_score || 0,
          away_score: event.away_score || 0,
          status: dbStatus,
          minute_played: dbStatus === "live" ? (event.current_minute || null) : null
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
