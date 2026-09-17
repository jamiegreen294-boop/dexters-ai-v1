export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { message } = req.body || {};

  if (!message) {
    return res.status(400).json({ error: 'Missing message' });
  }

  if (!process.env.OPENAI_API_KEY) {
    return res.status(503).json({
      reply: 'Dexter is ready, but the AI brain key still needs connecting.'
    });
  }

  try {
    const response = await fetch('https://api.openai.com/v1/responses', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${process.env.OPENAI_API_KEY}`
      },
      body: JSON.stringify({
        model: 'gpt-5-mini',
        input: [
          {
            role: 'system',
            content: 'You are Dexter, a helpful Scottish business assistant for Dexters. Be friendly, witty and useful.'
          },
          {
            role: 'user',
            content: message
          }
        ]
      })
    });

    const data = await response.json();
    res.json({ reply: data.output_text || 'Dexter could not get a response.' });
  } catch (error) {
    res.status(500).json({ error: 'AI connection failed' });
  }
}
