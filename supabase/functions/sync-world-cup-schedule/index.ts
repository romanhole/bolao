import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.3";

const BZZOIRO_API_KEY = Deno.env.get("BZZOIRO_API_KEY") || "";
const SUPABASE_URL = Deno.env.get("SUPABASE_URL") || "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";

function generateShortName(name: string, apiId: string, existingShortNames: Set<string>): string {
  let cleanName = name.replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
  if (cleanName.length >= 3 && cleanName.length <= 4) {
    if (!existingShortNames.has(cleanName)) {
      existingShortNames.add(cleanName);
      return cleanName;
    }
  }

  let base = cleanName.substring(0, 3);
  if (base.length < 3) {
    base = (base + apiId).substring(0, 3);
  }

  if (!existingShortNames.has(base)) {
    existingShortNames.add(base);
    return base;
  }

  for (let i = 1; i <= 9; i++) {
    const candidate = base.substring(0, 2) + i;
    if (!existingShortNames.has(candidate)) {
      existingShortNames.add(candidate);
      return candidate;
    }
  }

  let fallback = apiId.slice(-3);
  while (fallback.length < 3) {
    fallback = "0" + fallback;
  }
  let attempt = 0;
  while (existingShortNames.has(fallback)) {
    fallback = String(Number(fallback) + 1);
    attempt++;
    if (attempt > 100) {
      fallback = "T" + String(attempt).substring(0, 2);
      break;
    }
  }
  existingShortNames.add(fallback);
  return fallback;
}

serve(async (req) => {
  const executionLogs: string[] = [];
  const addLog = (msg: string) => {
    console.log(msg);
    executionLogs.push(msg);
  };

  try {
    addLog("Starting synchronization...");
    let url = "https://sports.bzzoiro.com/api/v2/events/?league_id=27&date_from=2026-06-11&date_to=2026-07-19";
    const events: any[] = [];
    
    // 1. Fetch all pages of matches from BZZOIRO API
    while (url) {
      addLog(`Fetching page: ${url}`);
      const response = await fetch(url, {
        method: "GET",
        headers: {
          "Authorization": `Token ${BZZOIRO_API_KEY}`
        }
      });

      if (!response.ok) {
        throw new Error(`BZZOIRO API responded with status: ${response.status}`);
      }

      const data = await response.json();
      const pageEvents = data.results || data.events || [];
      events.push(...pageEvents);

      url = data.next || null;
    }

    addLog(`Fetched total of ${events.length} events from API.`);

    if (events.length === 0) {
      return new Response(JSON.stringify({ message: "No events found.", logs: executionLogs }), {
        headers: { "Content-Type": "application/json" },
      });
    }

    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, {
      auth: { persistSession: false }
    });

    // 2. Query existing teams from Database to prevent short_name conflicts
    addLog("Querying existing teams from database...");
    const { data: dbTeams, error: dbTeamsError } = await supabase
      .from('teams')
      .select('api_team_id, short_name');

    if (dbTeamsError) {
      addLog(`Error fetching existing teams: ${JSON.stringify(dbTeamsError)}`);
      throw new Error(`Failed to fetch existing teams: ${dbTeamsError.message}`);
    }

    addLog(`Found ${dbTeams?.length || 0} existing teams in database.`);

    const existingShortNames = new Set<string>();
    const existingTeamsMap = new Map<string, string>(); // api_team_id -> short_name
    dbTeams.forEach((t: any) => {
      if (t.short_name) existingShortNames.add(t.short_name.toUpperCase());
      if (t.api_team_id) existingTeamsMap.set(t.api_team_id, t.short_name);
    });

    // 3. Extract unique teams to UPSERT
    addLog("Processing teams from events...");
    const teamsMap = new Map();
    events.forEach((event: any) => {
      // Home Team
      let homeId = "";
      let homeName = "";
      if (event.home_team && typeof event.home_team === 'object') {
        homeId = String(event.home_team.id);
        homeName = event.home_team.name || `Team ${homeId}`;
      } else if (event.home_team_id) {
        homeId = String(event.home_team_id);
        homeName = event.home_team || `Team ${homeId}`;
      }
      
      if (homeId && !teamsMap.has(homeId)) {
        let shortName = existingTeamsMap.get(homeId);
        if (!shortName) {
          shortName = generateShortName(homeName, homeId, existingShortNames);
        }
        teamsMap.set(homeId, {
          api_team_id: homeId,
          name: homeName,
          short_name: shortName,
          logo_url: event.home_team?.logo || event.home_team?.logo_url || ""
        });
      }

      // Away Team
      let awayId = "";
      let awayName = "";
      if (event.away_team && typeof event.away_team === 'object') {
        awayId = String(event.away_team.id);
        awayName = event.away_team.name || `Team ${awayId}`;
      } else if (event.away_team_id) {
        awayId = String(event.away_team_id);
        awayName = event.away_team || `Team ${awayId}`;
      }
      
      if (awayId && !teamsMap.has(awayId)) {
        let shortName = existingTeamsMap.get(awayId);
        if (!shortName) {
          shortName = generateShortName(awayName, awayId, existingShortNames);
        }
        teamsMap.set(awayId, {
          api_team_id: awayId,
          name: awayName,
          short_name: shortName,
          logo_url: event.away_team?.logo || event.away_team?.logo_url || ""
        });
      }
    });

    const uniqueTeams = Array.from(teamsMap.values());
    addLog(`Prepared ${uniqueTeams.length} unique teams to upsert.`);

    // 4. UPSERT Teams into Database
    let dbTeamMap = new Map(); // api_team_id -> UUID
    if (uniqueTeams.length > 0) {
      addLog("Upserting teams to database...");
      const { data: upsertedTeams, error: teamsError } = await supabase
        .from('teams')
        .upsert(uniqueTeams, { onConflict: 'api_team_id', ignoreDuplicates: false })
        .select('id, api_team_id');

      if (teamsError) {
        addLog(`Error upserting teams: ${JSON.stringify(teamsError)}`);
        throw new Error(`Failed to upsert teams: ${teamsError.message}`);
      }

      upsertedTeams.forEach((t: any) => {
        dbTeamMap.set(t.api_team_id, t.id);
      });
      addLog(`Upserted ${upsertedTeams.length} teams successfully.`);
    }

    // 5. Map events to the matches schema
    addLog("Mapping matches...");
    const matchesToUpsert = events.map((event: any) => {
      let dbStatus = "scheduled";
      switch (event.status) {
        case "notstarted": dbStatus = "scheduled"; break;
        case "inprogress":
        case "penalties": dbStatus = "live"; break;
        case "finished": dbStatus = "finished"; break;
        case "cancelled": dbStatus = "cancelled"; break;
        default: dbStatus = "scheduled";
      }

      const homeId = event.home_team_id ? String(event.home_team_id) : (event.home_team?.id ? String(event.home_team.id) : "");
      const awayId = event.away_team_id ? String(event.away_team_id) : (event.away_team?.id ? String(event.away_team.id) : "");

      return {
        api_fixture_id: String(event.id),
        home_team_id: dbTeamMap.get(homeId),
        away_team_id: dbTeamMap.get(awayId),
        home_score: typeof event.home_score === 'number' ? event.home_score : null,
        away_score: typeof event.away_score === 'number' ? event.away_score : null,
        status: dbStatus,
        minute_played: event.current_minute || null,
        scheduled_at: event.event_date || event.scheduled_at || new Date().toISOString(),
        competition_id: "copa_do_mundo_2026",
        competition: "World Cup",
        round: event.group_name || event.round_name || event.round || "Group Stage"
      };
    }).filter((m: any) => m.home_team_id && m.away_team_id); // Filter out matches where teams couldn't be resolved

    addLog(`Prepared ${matchesToUpsert.length} matches to upsert.`);

    // 6. UPSERT Matches into Database
    if (matchesToUpsert.length > 0) {
      addLog("Upserting matches to database...");
      const { error: matchesError } = await supabase
        .from('matches')
        .upsert(matchesToUpsert, { onConflict: 'api_fixture_id', ignoreDuplicates: false });

      if (matchesError) {
        addLog(`Error upserting matches: ${JSON.stringify(matchesError)}`);
        throw new Error(`Failed to upsert matches: ${matchesError.message}`);
      }
      addLog(`Upserted matches successfully.`);
    }

    addLog("Synchronization completed successfully!");
    return new Response(JSON.stringify({ 
      message: `Successfully synchronized ${uniqueTeams.length} teams and ${matchesToUpsert.length} matches.`,
      logs: executionLogs
    }), {
      headers: { "Content-Type": "application/json" },
    });
  } catch (error) {
    addLog(`Error: ${error.message}`);
    return new Response(JSON.stringify({ 
      error: error.message, 
      stack: error.stack,
      logs: executionLogs
    }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }
});
