import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.3";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") || "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";

// FCM_SERVICE_ACCOUNT_JSON deve ser cadastrado via Supabase Dashboard Secrets
const FCM_SERVICE_ACCOUNT_JSON = Deno.env.get("FCM_SERVICE_ACCOUNT_JSON");

// Função para obter o JWT token para chamar a API HTTP v1 do Firebase
async function getFcmAccessToken(serviceAccountJson: string): Promise<string> {
  const serviceAccount = JSON.parse(serviceAccountJson);
  
  // Create a JWT header
  const header = {
    alg: "RS256",
    typ: "JWT",
  };

  // Create a JWT payload
  const now = Math.floor(Date.now() / 1000);
  const payload = {
    iss: serviceAccount.client_email,
    sub: serviceAccount.client_email,
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600, // Token valid for 1 hour
    scope: "https://www.googleapis.com/auth/firebase.messaging",
  };

  // Encode header and payload
  const encodeBase64Url = (obj: any) => btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  const encodedHeader = encodeBase64Url(header);
  const encodedPayload = encodeBase64Url(payload);

  // Note: Deno doesn't have a built-in simple RS256 sign without Web Crypto API boilerplate.
  // For edge functions, importing a JWT library is usually better.
  const { create } = await import("https://deno.land/x/djwt@v2.8/mod.ts");
  
  const privateKeyStr = serviceAccount.private_key;
  
  // Import the private key
  const pemHeader = "-----BEGIN PRIVATE KEY-----";
  const pemFooter = "-----END PRIVATE KEY-----";
  const pemContents = privateKeyStr.substring(
    privateKeyStr.indexOf(pemHeader) + pemHeader.length,
    privateKeyStr.indexOf(pemFooter)
  ).replace(/\s/g, "");
  
  const binaryDerString = atob(pemContents);
  const binaryDer = new Uint8Array(binaryDerString.length);
  for (let i = 0; i < binaryDerString.length; i++) {
    binaryDer[i] = binaryDerString.charCodeAt(i);
  }

  const key = await crypto.subtle.importKey(
    "pkcs8",
    binaryDer,
    {
      name: "RSASSA-PKCS1-v1_5",
      hash: "SHA-256",
    },
    true,
    ["sign"]
  );

  const jwt = await create(header, payload, key);

  // Exchange JWT for an access token
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(`Failed to get FCM access token: ${JSON.stringify(data)}`);
  }

  return data.access_token;
}

serve(async (req) => {
  try {
    if (!FCM_SERVICE_ACCOUNT_JSON) {
      throw new Error("Missing FCM_SERVICE_ACCOUNT_JSON secret.");
    }

    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, {
      auth: { persistSession: false }
    });

    const now = new Date();
    // Janela: jogos que começam entre +1h e +3h
    const windowStart = new Date(now.getTime() + 1 * 60 * 60 * 1000).toISOString();
    const windowEnd = new Date(now.getTime() + 3 * 60 * 60 * 1000).toISOString();

    // 1. Encontrar partidas nesta janela
    const { data: upcomingMatches, error: matchesError } = await supabase
      .from("matches")
      .select("id, home_team_id(short_name), away_team_id(short_name), scheduled_at")
      .eq("status", "scheduled")
      .gte("scheduled_at", windowStart)
      .lte("scheduled_at", windowEnd);

    if (matchesError) throw matchesError;

    if (!upcomingMatches || upcomingMatches.length === 0) {
      return new Response(JSON.stringify({ message: "No upcoming matches in the 1h-3h window." }), {
        headers: { "Content-Type": "application/json" },
      });
    }

    const projectId = JSON.parse(FCM_SERVICE_ACCOUNT_JSON).project_id;
    let accessToken = "";
    try {
      accessToken = await getFcmAccessToken(FCM_SERVICE_ACCOUNT_JSON);
    } catch (e) {
      throw new Error(`FCM Auth Error: ${e.message}`);
    }

    let totalNotificationsSent = 0;

    for (const match of upcomingMatches) {
      const matchId = match.id;
      const homeTeam = (match as any).home_team_id.short_name;
      const awayTeam = (match as any).away_team_id.short_name;

      // 2. Encontrar os membros de QUALQUER liga
      const { data: allLeagueMembers, error: membersError } = await supabase
        .from("league_members")
        .select("user_id");
      
      if (membersError) {
        console.error("Error fetching league members:", membersError);
        continue;
      }

      // Deixar único
      const uniqueUserIds = [...new Set(allLeagueMembers.map(m => m.user_id))];

      // 3. Encontrar quem já apostou nessa partida
      const { data: predictions, error: predsError } = await supabase
        .from("predictions")
        .select("user_id")
        .eq("match_id", matchId);
      
      if (predsError) {
        console.error("Error fetching predictions:", predsError);
        continue;
      }

      const userIdsWithPrediction = new Set(predictions.map(p => p.user_id));

      // 4. Encontrar quem JÁ RECEBEU notificação dessa partida
      const { data: sentNotifs, error: notifsError } = await supabase
        .from("notifications_sent")
        .select("user_id")
        .eq("match_id", matchId);

      if (notifsError) {
        console.error("Error fetching sent notifications:", notifsError);
        continue;
      }

      const userIdsAlreadyNotified = new Set(sentNotifs.map(n => n.user_id));

      // 5. Filtrar quem precisa receber = (Está em liga) AND (Não tem aposta) AND (Não foi notificado ainda)
      const targetUserIds = uniqueUserIds.filter(id => !userIdsWithPrediction.has(id) && !userIdsAlreadyNotified.has(id));

      if (targetUserIds.length === 0) {
        continue;
      }

      // 6. Pegar os tokens de push desses usuários
      const { data: pushTokens, error: tokensError } = await supabase
        .from("push_tokens")
        .select("user_id, token")
        .in("user_id", targetUserIds);
      
      if (tokensError) {
        console.error("Error fetching push tokens:", tokensError);
        continue;
      }

      if (!pushTokens || pushTokens.length === 0) {
        continue;
      }

      // 7. Enviar notificações via FCM
      const notificationsToRecord = [];

      for (const pt of pushTokens) {
        const payload = {
          message: {
            token: pt.token,
            notification: {
              title: "Vai esquecer de palpitar?",
              body: `O jogo ${homeTeam} x ${awayTeam} está quase começando! Corra para não perder seus pontos.`,
            },
            android: {
              notification: {
                channel_id: "bolao_alerts"
              }
            },
            data: {
              click_action: "FLUTTER_NOTIFICATION_CLICK", // Opcional
              match_id: matchId,
            }
          }
        };

        const fcmResponse = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
          method: "POST",
          headers: {
            "Authorization": `Bearer ${accessToken}`,
            "Content-Type": "application/json"
          },
          body: JSON.stringify(payload)
        });

        if (fcmResponse.ok) {
          totalNotificationsSent++;
          notificationsToRecord.push({ user_id: pt.user_id, match_id: matchId });
        } else {
          const errData = await fcmResponse.json();
          console.error(`Failed to send FCM to ${pt.user_id}:`, errData);
          // Se o erro for de token inválido (UNREGISTERED), poderiamos deletar o token do banco.
          if (errData.error?.status === "NOT_FOUND" || errData.error?.details?.[0]?.errorCode === "UNREGISTERED") {
            await supabase.from("push_tokens").delete().eq("token", pt.token);
          }
        }
      }

      // 8. Registrar que enviamos
      if (notificationsToRecord.length > 0) {
        await supabase.from("notifications_sent").insert(notificationsToRecord);
      }
    }

    return new Response(JSON.stringify({ 
      message: "Notification check complete.",
      sent_count: totalNotificationsSent
    }), {
      headers: { "Content-Type": "application/json" },
    });

  } catch (error) {
    console.error("Critical error in notify-upcoming-matches:", error);
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }
});
