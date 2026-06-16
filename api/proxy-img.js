export default async function handler(req, res) {
  const { url } = req.query;

  if (!url) {
    return res.status(400).json({ error: 'Missing url parameter' });
  }

  try {
    const fetchResponse = await fetch(url);
    
    if (!fetchResponse.ok) {
      return res.status(fetchResponse.status).json({ error: 'Failed to fetch image' });
    }

    const contentType = fetchResponse.headers.get('content-type');
    res.setHeader('Content-Type', contentType || 'image/png');
    // Adicionando cabeçalhos de CORS para resolver o bloqueio do navegador
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    // Fazendo cache forte para não sobrecarregar a Serverless Function nem o provedor da imagem
    res.setHeader('Cache-Control', 'public, max-age=86400, s-maxage=86400, stale-while-revalidate=2592000');

    const arrayBuffer = await fetchResponse.arrayBuffer();
    const buffer = Buffer.from(arrayBuffer);
    
    res.send(buffer);
  } catch (error) {
    console.error('Error proxying image:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
}
