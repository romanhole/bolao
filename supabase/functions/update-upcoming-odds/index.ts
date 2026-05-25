import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.3";

const BZZOIRO_API_KEY = Deno.env.get("BZZOIRO_API_KEY") || "";
const SUPABASE_URL = Deno.env.get("SUPABASE_URL") || "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";

function addLog(msg: string) {
  console.log(`[${new Date().toISOString()}] ${msg}`);
}

serve(async (req) => {
  try {
    addLog("Starting upcoming odds synchronization...");
    
    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, {
      auth: { persistSession: false }
    });

    // 1. Buscamos jogos agendados que ocorrerão nos próximos 7 dias.
    const now = new Date();
    const next7Days = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
    const next48Hours = new Date(now.getTime() + 48 * 60 * 60 * 1000);

    const { data: matches, error: fetchError } = await supabase
      .from("matches")
      .select("id, api_fixture_id, scheduled_at, home_odd")
      .eq("status", "scheduled")
      .gte("scheduled_at", now.toISOString())
      .lte("scheduled_at", next7Days.toISOString());

    if (fetchError) {
      throw new Error(`Error fetching matches: ${fetchError.message}`);
    }

    if (!matches || matches.length === 0) {
      return new Response(JSON.stringify({ message: "No upcoming matches in the next 7 days." }), {
        headers: { "Content-Type": "application/json" }
      });
    }

    addLog(`Found ${matches.length} matches in the 7-day window.`);

    // 2. Peneira de Congelamento de 48 horas
    const matchesToUpdate = matches.filter((match) => {
      const matchDate = new Date(match.scheduled_at);
      // Se não tem odd ainda, PRECISA buscar (mesmo se < 48h para não zerar os pontos)
      if (match.home_odd === null) return true;
      // Se já tem odd, mas faltam MAIS de 48h, PODE atualizar
      if (matchDate > next48Hours) return true;
      // Se já tem odd e faltam MENOS de 48h, CONGELA (ignora)
      return false;
    });

    addLog(`${matchesToUpdate.length} matches are eligible for odds update (unfrozen).`);

    // 3. Busca odds para cada partida elegível
    let updatedCount = 0;
    for (const match of matchesToUpdate) {
      try {
        const response = await fetch(`https://sports.bzzoiro.com/api/v2/events/${match.api_fixture_id}/odds/`, {
          method: "GET",
          headers: {
            "Authorization": `Token ${BZZOIRO_API_KEY}`
          }
        });

        if (!response.ok) {
          addLog(`BZZOIRO API error for match ${match.api_fixture_id}: ${response.status}`);
          continue;
        }

        const oddsJson = await response.json();
        const markets = oddsJson.markets || [];
        
        // Procura mercado 1X2 Principal
        const market1x2 = markets.find((m: any) => 
          m.name?.toLowerCase() === "1x2" || m.id === 1
        );

        if (market1x2 && market1x2.selections) {
          let homeOdd = null;
          let drawOdd = null;
          let awayOdd = null;

          market1x2.selections.forEach((sel: any) => {
            if (sel.name === "1" || sel.name === "Home") homeOdd = sel.odd;
            if (sel.name === "X" || sel.name === "Draw") drawOdd = sel.odd;
            if (sel.name === "2" || sel.name === "Away") awayOdd = sel.odd;
          });

          // Atualiza no banco as 3 odds preenchidas
          if (homeOdd && drawOdd && awayOdd) {
            const { error: updateError } = await supabase
              .from("matches")
              .update({
                home_odd: homeOdd,
                draw_odd: drawOdd,
                away_odd: awayOdd
              })
              .eq("id", match.id);

            if (!updateError) {
              updatedCount++;
            }
          }
        }
      } catch (err: any) {
        addLog(`Failed to process match ${match.api_fixture_id}: ${err.message}`);
      }
      
      // Delay de 300ms entre as chamadas para respeitar o Rate Limit da Bzzoiro
      await new Promise(resolve => setTimeout(resolve, 300));
    }

    addLog(`Successfully updated odds for ${updatedCount} matches.`);

    return new Response(JSON.stringify({ 
      message: `Checked ${matchesToUpdate.length} matches, updated ${updatedCount}.`,
      frozen_matches_skipped: matches.length - matchesToUpdate.length
    }), {
      headers: { "Content-Type": "application/json" }
    });
  } catch (error: any) {
    console.error("Critical Error:", error);
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { "Content-Type": "application/json" }
    });
  }
});
