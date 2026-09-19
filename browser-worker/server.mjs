import http from "node:http";
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { chromium } from "playwright";

const PORT = Number(process.env.PORT || 8765);
const TOKEN = process.env.DEXTER_BROWSER_WORKER_TOKEN || "";
const PROFILE = process.env.DEXTER_BROWSER_PROFILE || path.resolve("./browser-profile");
const HEADLESS = String(process.env.DEXTER_BROWSER_HEADLESS || "false").toLowerCase() === "true";
const ALLOW_PRIVATE = String(process.env.DEXTER_BROWSER_ALLOW_PRIVATE || "false").toLowerCase() === "true";

if (!TOKEN || TOKEN.length < 24) {
  console.error("DEXTER_BROWSER_WORKER_TOKEN must be set to a long random value.");
  process.exit(1);
}
fs.mkdirSync(PROFILE, { recursive: true });

let context;
async function browserContext() {
  if (!context) {
    context = await chromium.launchPersistentContext(PROFILE, {
      headless: HEADLESS,
      viewport: { width: 1440, height: 1000 },
      acceptDownloads: true
    });
  }
  return context;
}

function json(res, status, body) {
  const data = JSON.stringify(body);
  res.writeHead(status, {
    "Content-Type": "application/json",
    "Cache-Control": "no-store",
    "Content-Length": Buffer.byteLength(data)
  });
  res.end(data);
}
function safeUrl(value) {
  const u = new URL(String(value || ""));
  if (!["http:", "https:"].includes(u.protocol)) throw new Error("Only http/https URLs are allowed.");
  if (!ALLOW_PRIVATE && /^(localhost|127\.0\.0\.1|0\.0\.0\.0|::1)$/i.test(u.hostname)) throw new Error("Private/local targets are disabled.");
  return u.toString();
}
async function pageFor(session = "default") {
  const ctx = await browserContext();
  let page = ctx.pages().find(p => p.__dexterSession === session);
  if (!page) {
    page = await ctx.newPage();
    page.__dexterSession = session;
  }
  return page;
}
async function snapshot(page) {
  return await page.evaluate(() => {
    const selectors = [
      "a[href]","button","input","textarea","select","[role=button]","[contenteditable=true]"
    ];
    const els = Array.from(document.querySelectorAll(selectors.join(","))).slice(0, 250);
    return els.map((el, i) => ({
      ref: "e" + (i + 1),
      tag: el.tagName.toLowerCase(),
      text: (el.innerText || el.getAttribute("aria-label") || el.getAttribute("placeholder") || "").trim().slice(0, 300),
      type: el.getAttribute("type"),
      name: el.getAttribute("name"),
      href: el instanceof HTMLAnchorElement ? el.href : null
    }));
  });
}
async function elementByRef(page, ref) {
  const index = Number(String(ref || "").replace(/^e/, "")) - 1;
  if (!Number.isInteger(index) || index < 0) throw new Error("Invalid element ref.");
  const selectors = ["a[href]","button","input","textarea","select","[role=button]","[contenteditable=true]"];
  const locator = page.locator(selectors.join(",")).nth(index);
  if (await locator.count() < 1) throw new Error("Element ref not found; take a fresh snapshot.");
  return locator;
}
async function execute(tool, request = {}) {
  const session = String(request.session || "default").slice(0, 80);
  const page = await pageFor(session);

  if (tool === "browser.navigate") {
    await page.goto(safeUrl(request.url), { waitUntil: "domcontentloaded", timeout: 45000 });
    return { url: page.url(), title: await page.title(), elements: await snapshot(page) };
  }
  if (tool === "browser.snapshot") {
    return { url: page.url(), title: await page.title(), elements: await snapshot(page) };
  }
  if (tool === "browser.click") {
    const el = await elementByRef(page, request.ref);
    await el.click({ timeout: 15000 });
    await page.waitForTimeout(500);
    return { url: page.url(), title: await page.title(), elements: await snapshot(page) };
  }
  if (tool === "browser.fill") {
    const el = await elementByRef(page, request.ref);
    await el.fill(String(request.value ?? ""));
    return { ok: true, url: page.url() };
  }
  if (tool === "browser.select") {
    const el = await elementByRef(page, request.ref);
    await el.selectOption(String(request.value ?? ""));
    return { ok: true, url: page.url() };
  }
  if (tool === "browser.press") {
    await page.keyboard.press(String(request.key || "Enter"));
    await page.waitForTimeout(300);
    return { url: page.url(), title: await page.title(), elements: await snapshot(page) };
  }
  if (tool === "browser.text") {
    return { url: page.url(), title: await page.title(), text: (await page.locator("body").innerText()).slice(0, 60000) };
  }
  if (tool === "browser.screenshot") {
    const bytes = await page.screenshot({ fullPage: Boolean(request.fullPage) });
    return { url: page.url(), image_base64: bytes.toString("base64") };
  }
  if (tool === "browser.close") {
    await page.close();
    return { ok: true };
  }
  throw new Error("Unsupported browser tool: " + tool);
}

const server = http.createServer(async (req, res) => {
  try {
    if (req.method === "GET" && req.url === "/health") return json(res, 200, { status: "ready", browser: "chromium", headless: HEADLESS });
    if (req.method !== "POST" || req.url !== "/tool") return json(res, 404, { error: "Not found" });

    const supplied = String(req.headers.authorization || "").replace(/^Bearer\s+/i, "");
    const a = Buffer.from(supplied), b = Buffer.from(TOKEN);
    if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) return json(res, 401, { error: "Unauthorized" });

    let raw = "";
    for await (const chunk of req) {
      raw += chunk;
      if (raw.length > 1_000_000) throw new Error("Request too large.");
    }
    const body = JSON.parse(raw || "{}");
    const result = await execute(String(body.tool || ""), body.request || {});
    return json(res, 200, { result });
  } catch (e) {
    return json(res, 500, { error: String(e?.message || e) });
  }
});

server.listen(PORT, "127.0.0.1", () => {
  console.log("Dexter Browser Worker listening on http://127.0.0.1:" + PORT);
});
