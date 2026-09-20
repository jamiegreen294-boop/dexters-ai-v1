const COMMAND_CENTRE = 'https://eikruaxxzzxmfjvsmwwo.supabase.co/functions/v1/ai-command-centre';

export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });

  const token = req.headers['x-dexter-token'];
  const { message, sessionId } = req.body || {};
  if (!token) return res.status(401).json({ error: 'Dexter owner access code required' });
  if (!message) return res.status(400).json({ error: 'Missing message' });

  const sid = sessionId || crypto.randomUUID();

  try {
    const response = await fetch(COMMAND_CENTRE, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-dexter-token': String(token)
      },
      body: JSON.stringify({ action: 'chat', sessionId: sid, message })
    });

    const data = await response.json().catch(() => ({ error: 'Bad Dexter command-centre response' }));
    return res.status(response.status).json({ ...data, sessionId: sid });
  } catch (error) {
    return res.status(502).json({ error: 'Dexter command centre unavailable' });
  }
}
