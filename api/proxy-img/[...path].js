// Vercel Serverless Function - Proxy de imagens para contornar CORS
// Rota: /api/proxy-img/[...path]
export default async function handler(req, res) {
  // Extrai o caminho após /api/proxy-img/
  const { path } = req.query;
  const imagePath = Array.isArray(path) ? path.join('/') : path;

  if (!imagePath) {
    return res.status(400).json({ error: 'Path is required' });
  }

  const targetUrl = `https://sports.bzzoiro.com/img/${imagePath}`;

  try {
    const imageRes = await fetch(targetUrl, {
      headers: {
        'User-Agent': 'Mozilla/5.0',
        'Accept': 'image/*,*/*',
      },
    });

    if (!imageRes.ok) {
      return res.status(imageRes.status).end();
    }

    const contentType = imageRes.headers.get('content-type') || 'image/png';
    const buffer = await imageRes.arrayBuffer();

    res.setHeader('Content-Type', contentType);
    res.setHeader('Cache-Control', 'public, max-age=86400'); // cache 24h
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.status(200).send(Buffer.from(buffer));
  } catch (err) {
    console.error('Proxy error:', err);
    res.status(500).json({ error: 'Failed to fetch image' });
  }
}
