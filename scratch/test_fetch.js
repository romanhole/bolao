const url = "https://uetdonnoytbsjvuliiec.supabase.co/functions/v1/update-live-matches";
fetch(url, { method: "POST" })
  .then(r => r.json())
  .then(d => console.log(JSON.stringify(d, null, 2)))
  .catch(console.error);
