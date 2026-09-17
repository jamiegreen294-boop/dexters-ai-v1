const OPENAI_URL = "https://api.openai.com/v1/responses";
const BUSINESS_TABLES = {
  menu: "loyalty_menu_categories?select=id,name,sort_order,active&active=eq.true&order=sort_order",
  items: "loyalty_menu_items?select=id,category_id,name,price_text,description,active,in_stock&active=eq.true&order=sort_order",
  roast: "sunday_roast_settings?select=collection_date,enabled,cutoff_mode,slots,stock&order=collection_date.desc&limit=2",
  offers: "app_news_banner?select=message,active,updated_at&active=eq.true&limit=1",
  business: "backoffice_business_settings?select=tenant_key,settings&limit=1",
  suppliers: "pc_pos_suppliers_test?select=name,notes,active&active=eq.true&order=name",
  recipes: "cost_recipes?select=id,name,category,selling_price,yield_qty,method,notes,archived&archived=eq.false&order=name"
};

function json(res, status, body) {
  res.status(status).setHeader("Cache-Control", "no-store").json(body);
}
async function sb(url, key, path, options={}) {
  const r = await fetch(`${url}/rest/v1/${path}`, {
    ...options,
    headers: { apikey:key, Authorization:`Bearer ${key}`, "Content-Type":"application/json", Prefer:"return=representation", ...(options.headers||{}) }
  });
  if (!r.ok) throw new Error(`Database request failed (${r.status})`);
  const text = await r.text();
  return text ? JSON.parse(text) : null;
}
function outputText(data) {
  if (data.output_text) return data.output_text;
  return (data.output||[]).flatMap(x=>x.content||[]).filter(x=>x.type==="output_text").map(x=>x.text).join("\n");
}
export default async function handler(req,res) {
  if (req.method!=="POST") return json(res,405,{error:"Method not allowed"});
  const expected=process.env.DEXTER_ACCESS_TOKEN;
  const supplied=(req.headers.authorization||"").replace(/^Bearer\s+/i,"");
  if (!expected || supplied!==expected) return json(res,401,{error:"Dexter access code is missing or incorrect."});
  const {message,sessionId,role="owner"}=req.body||{};
  if (!message || typeof message!=="string" || message.length>4000) return json(res,400,{error:"Enter a message (maximum 4,000 characters)."});
  if (!process.env.OPENAI_API_KEY || !process.env.DEXTER_AI_SUPABASE_URL || !process.env.DEXTER_AI_SUPABASE_SERVICE_ROLE_KEY)
    return json(res,503,{error:"Dexter's secure backend configuration is incomplete."});
  const aiUrl=process.env.DEXTER_AI_SUPABASE_URL, aiKey=process.env.DEXTER_AI_SUPABASE_SERVICE_ROLE_KEY;
  const dataUrl=process.env.DEXTERS_DATA_SUPABASE_URL, dataKey=process.env.DEXTERS_DATA_SUPABASE_SERVICE_ROLE_KEY;
  const sid=sessionId||crypto.randomUUID();
  try {
    let context={};
    if (dataUrl && dataKey) {
      const wanted = role==="owner"||role==="manager" ? Object.entries(BUSINESS_TABLES) : Object.entries(BUSINESS_TABLES).filter(([k])=>["menu","items","roast","offers","business"].includes(k));
      const results=await Promise.allSettled(wanted.map(async([k,p])=>[k,await sb(dataUrl,dataKey,p)]));
      for (const r of results) if(r.status==="fulfilled") context[r.value[0]]=r.value[1];
    }
    const history=await sb(aiUrl,aiKey,`dexter_messages?select=role,content&session_id=eq.${encodeURIComponent(sid)}&order=created_at.asc&limit=20`);
    const system=`You are Dexter, the private AI assistant for Dexters food business in Glasgow.
Style: helpful, accurate and concise, with light cheeky Glaswegian humour when suitable. Be professional for HR, safety, finance or customer complaints. Never insult or swear unless the user swears first, and never escalate.
Business facts and personality are separate: never invent prices, allergens, opening hours, offers, recipes or procedures. Use only BUSINESS DATA below. If data is absent or ambiguous, say you cannot confirm and recommend checking the live system.
The current permission role is ${role}. Do not reveal staff procedures, suppliers, recipes, source code, credentials, personal data or internal systems to customer role users.
Never claim a live change was made. Any deployment, database write, code merge, order, refund, staff-data access, or other consequential action requires explicit owner approval.
BUSINESS DATA (read-only snapshot): ${JSON.stringify(context)}`;
    const input=[{role:"system",content:system},...(history||[]).map(x=>({role:x.role,content:x.content})),{role:"user",content:message}];
    const ai=await fetch(OPENAI_URL,{method:"POST",headers:{"Content-Type":"application/json",Authorization:`Bearer ${process.env.OPENAI_API_KEY}`},body:JSON.stringify({model:process.env.OPENAI_MODEL||"gpt-5-mini",input,max_output_tokens:1200})});
    const payload=await ai.json();
    if(!ai.ok) throw new Error(payload?.error?.message||"AI model request failed");
    const reply=outputText(payload)||"I could not form a reply. Try that again.";
    await sb(aiUrl,aiKey,"dexter_sessions?on_conflict=id",{method:"POST",headers:{Prefer:"resolution=merge-duplicates,return=minimal"},body:JSON.stringify({id:sid,role,last_active_at:new Date().toISOString()})});
    await sb(aiUrl,aiKey,"dexter_messages",{method:"POST",headers:{Prefer:"return=minimal"},body:JSON.stringify([{session_id:sid,role:"user",content:message},{session_id:sid,role:"assistant",content:reply}])});
    return json(res,200,{reply,sessionId:sid});
  } catch(e) {
    console.error("dexter-chat",e.message);
    return json(res,500,{error:"Dexter hit a backend problem. No live change was made.",detail:process.env.NODE_ENV==="development"?e.message:undefined});
  }
}