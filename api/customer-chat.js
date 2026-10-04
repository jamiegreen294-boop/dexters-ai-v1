const PROJECT_ID = "prj_jHa0ZVvB2Eu8eMqWBQkDgZFehuCA";
const TEAM_ID = "team_WWL2LfVdJc0U8F9QD29n6xXj";

function clean(value, max = 5000) {
  return String(value ?? "").replace(/\u0000/g, "").slice(0, max);
}

function outputText(data) {
  if (typeof data?.output_text === "string" && data.output_text.trim()) {
    return data.output_text.trim();
  }
  const out = [];
  for (const item of data?.output || []) {
    for (const part of item?.content || []) {
      if (part?.type === "output_text" && part?.text) out.push(String(part.text));
    }
  }
  return out.join("\n").trim();
}

async function authorisedVercelToken(token) {
  if (!token || token.length < 20) return false;
  const r = await fetch(
    "https://api.vercel.com/v9/projects/" + PROJECT_ID + "?teamId=" + TEAM_ID,
    { headers: { Authorization: "Bearer " + token } }
  );
  if (!r.ok) return false;
  const data = await r.json().catch(() => null);
  return data?.id === PROJECT_ID;
}

export default async function handler(req, res) {
  res.setHeader("Cache-Control", "no-store");
  if (req.method !== "POST") return res.status(405).json({ ok: false, error: "Method not allowed" });

  const auth = String(req.headers.authorization || "");
  const token = auth.replace(/^Bearer\s+/i, "").trim();
  if (!(await authorisedVercelToken(token))) {
    return res.status(403).json({ ok: false, error: "Forbidden" });
  }

  const apiKey = process.env.OPENAI_API_KEY || "";
  if (!apiKey) return res.status(503).json({ ok: false, error: "Cloud AI is not configured" });

  const body = req.body || {};
  const system = clean(body.system, 16000);
  const history = Array.isArray(body.history)
    ? body.history.slice(-12).map((m) => ({
        role: m?.role === "assistant" ? "assistant" : "user",
        content: clean(m?.content, 4000)
      }))
    : [];
  const message = clean(body.message, 5000).trim();
  if (!message) return res.status(400).json({ ok: false, error: "Message required" });

  const model = process.env.DEXTER_AI_MODEL || "gpt-6-luna";
  const maxOutputTokens = Math.max(80, Math.min(700, Number(body.max_output_tokens) || 360));

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 15000);
  try {
    const r = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      signal: controller.signal,
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer " + apiKey
      },
      body: JSON.stringify({
        model,
        instructions: system || undefined,
        input: [...history, { role: "user", content: message }],
        max_output_tokens: maxOutputTokens
      })
    });

    const data = await r.json().catch(() => ({}));
    if (!r.ok) {
      return res.status(502).json({
        ok: false,
        error: data?.error?.message || "Cloud model request failed"
      });
    }
    const reply = outputText(data);
    if (!reply) return res.status(502).json({ ok: false, error: "Cloud model returned no text" });
    return res.status(200).json({ ok: true, reply, model, provider: "vercel-openai" });
  } catch (error) {
    const message = error?.name === "AbortError" ? "Cloud model timed out" : "Cloud model unavailable";
    return res.status(503).json({ ok: false, error: message });
  } finally {
    clearTimeout(timer);
  }
}
