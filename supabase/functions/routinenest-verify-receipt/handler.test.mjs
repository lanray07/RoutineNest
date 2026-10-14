import test from "node:test";
import assert from "node:assert/strict";
import { createHandler } from "./handler.mjs";

const receipt = { receiptId: "receipt", productId: "premium", productType: "ENTITLED", cancelDate: null, testTransaction: false, betaProduct: false };
function fixture({ authStatus = 200, amazonStatus = 200, changes = {}, missing = [], environment = "production", configured = true, outage = false, now = () => 1000 } = {}) {
  const seen = [];
  const env = { SUPABASE_URL: "https://project.supabase.co", SUPABASE_ANON_KEY: "public-key", AMAZON_PREMIUM_SKU: "premium", AMAZON_RVS_ENVIRONMENT: environment,
    AMAZON_RVS_SHARED_SECRET: configured ? "server-secret" : "" };
  const handler = createHandler({ env: name => env[name], now, rateLimit: async () => ({ allowed: true, retryAfterSeconds: 60 }), fetch: async (url, options) => {
    seen.push({ url, options });
    if (url.includes("/auth/v1/user")) return Response.json({ id: "verified-subject", user_metadata: { premium: true } }, { status: authStatus });
    if (outage) throw new Error("server-secret in request URL");
    const body = { ...receipt, ...changes };
    missing.forEach(key => delete body[key]);
    return Response.json(body, { status: amazonStatus });
  } });
  return { handler, seen };
}
const request = (body = { amazonUserId: "amazon-user", receiptId: "receipt" }, token = "session-jwt") => new Request("https://project.supabase.co/functions/v1/routinenest-verify-receipt", {
  method: "POST", headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) }, body: JSON.stringify(body),
});

test("unauthenticated caller never reaches Amazon", async () => {
  const { handler, seen } = fixture();
  assert.equal((await handler(request(undefined, ""))).status, 401); assert.equal(seen.length, 0);
});
test("invalid or deleted Supabase user never reaches Amazon", async () => {
  const { handler, seen } = fixture({ authStatus: 401 });
  assert.equal((await handler(request())).status, 401); assert.equal(seen.length, 1);
});
test("authenticated subject comes from Auth, never input metadata", async () => {
  const { handler } = fixture();
  const response = await handler(request({ amazonUserId: "amazon-user", receiptId: "receipt", subject: "attacker", premium: true }));
  const data = await response.json(); assert.equal(data.subject, "verified-subject"); assert.equal(data.decision, "verified");
  assert.equal(data.expiresAt, 21601000); assert.equal(response.headers.get("Cache-Control"), "no-store");
});
test("missing server secret returns a safe configuration failure", async () => {
  const { handler, seen } = fixture({ configured: false });
  assert.equal((await handler(request())).status, 503); assert.equal(seen.length, 1);
});
test("advancing clock cannot produce a lease longer than the client's six-hour limit", async () => {
  let time = 1000;
  const { handler } = fixture({ now: () => time++ });
  const data = await (await handler(request())).json();
  assert.equal(data.expiresAt - data.verifiedAt, 6 * 60 * 60 * 1000);
});
test("receipt, SKU and product type mismatch cannot unlock", async () => {
  for (const changes of [{ receiptId: "other" }, { productId: "other" }, { productType: "CONSUMABLE" }]) {
    const { handler } = fixture({ changes }); assert.equal((await (await handler(request())).json()).decision, "rejected");
  }
});
test("missing test/cancel markers cannot unlock", async () => {
  for (const key of ["cancelDate", "testTransaction", "betaProduct"]) {
    const { handler } = fixture({ missing: [key] }); assert.equal((await (await handler(request())).json()).decision, "rejected");
  }
});
test("revoked receipts and HTTP 410 revoke", async () => {
  for (const options of [{ changes: { cancelDate: 0 } }, { amazonStatus: 410 }]) {
    const { handler } = fixture(options); assert.equal((await (await handler(request())).json()).decision, "revoked");
  }
});
test("production rejects test purchases and sandbox admits test policy", async () => {
  for (const key of ["testTransaction", "betaProduct"]) {
    let { handler } = fixture({ changes: { [key]: true } }); assert.equal((await (await handler(request())).json()).decision, "rejected");
    ({ handler } = fixture({ changes: { [key]: true }, environment: "sandbox" })); assert.equal((await (await handler(request())).json()).decision, "verified");
  }
});
test("rate limit, outage and server-key rejection never grant access", async () => {
  for (const amazonStatus of [429, 500, 496]) {
    const { handler } = fixture({ amazonStatus }); assert.equal((await handler(request())).status, 503);
  }
});
test("identifiers are URL encoded and redirects forbidden", async () => {
  const { handler, seen } = fixture(); await handler(request({ amazonUserId: "user/part?", receiptId: "receipt#&" }));
  assert.match(seen[1].url, /user\/user%2Fpart%3F\/receiptId\/receipt%23%26$/);
  assert.equal(seen[0].options.redirect, "error"); assert.equal(seen[1].options.redirect, "error");
});
test("oversized and empty input never reaches Amazon", async () => {
  for (const input of [{ amazonUserId: "", receiptId: "receipt" }, { amazonUserId: "user", receiptId: "r".repeat(20000) }]) {
    const { handler, seen } = fixture(); assert.equal((await handler(request(input))).status, 400); assert.equal(seen.length, 1);
  }
});
test("network errors never expose server secret", async () => {
  const { handler } = fixture({ outage: true }); const response = await handler(request());
  assert.equal(response.status, 503); assert.doesNotMatch(await response.text(), /server-secret/);
});
