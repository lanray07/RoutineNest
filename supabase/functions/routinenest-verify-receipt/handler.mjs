// Dependency-free handler shared by Supabase's Deno runtime and Node contract tests.
const respond = (status, body) => new Response(JSON.stringify(body), {
  status, headers: { "Content-Type": "application/json", "Cache-Control": "no-store" },
});
const identifier = value => typeof value === "string" && value.trim().length > 0 && value.length <= 4096;

async function boundedJson(response, limit) {
  if (!response.body) throw new Error("empty response");
  const reader = response.body.getReader();
  const chunks = [];
  let total = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      total += value.byteLength;
      if (total > limit) throw new Error("response too large");
      chunks.push(value);
    }
  } finally {
    await reader.cancel();
  }
  const bytes = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.length; }
  return JSON.parse(new TextDecoder("utf-8", { fatal: true }).decode(bytes));
}

export function createHandler({ env, fetch, now }) {
  return async request => {
    if (request.method !== "POST") return respond(405, { error: "method_not_allowed" });
    const authorization = request.headers.get("Authorization") ?? "";
    if (!/^Bearer [^\s]+$/.test(authorization)) return respond(401, { error: "authentication_required" });
    const projectUrl = env("SUPABASE_URL");
    const keyMap = env("SUPABASE_PUBLISHABLE_KEYS");
    let publishableKey = env("ROUTINENEST_SUPABASE_PUBLISHABLE_KEY") || env("SUPABASE_ANON_KEY");
    if (keyMap) {
      try { publishableKey = JSON.parse(keyMap).default || publishableKey; } catch { /* fail closed below */ }
    }
    if (!projectUrl || !publishableKey) return respond(503, { error: "auth_configuration_pending" });
    let principal;
    try {
      const auth = await fetch(`${projectUrl}/auth/v1/user`, {
        headers: { Authorization: authorization, apikey: publishableKey },
        redirect: "error", signal: AbortSignal.timeout(5000),
      });
      if (auth.status >= 500 || auth.status === 429) return respond(503, { error: "auth_unavailable" });
      if (!auth.ok) return respond(401, { error: "invalid_session" });
      principal = await boundedJson(auth, 65536);
      if (!identifier(principal?.id)) return respond(401, { error: "invalid_session" });
      // No authorization decisions use editable user_metadata or caller-supplied subjects.
    } catch { return respond(503, { error: "auth_unavailable" }); }

    let input;
    try {
      if (!(request.headers.get("Content-Type") ?? "").startsWith("application/json")) return respond(415, { error: "json_required" });
      input = await boundedJson(request, 16384);
      if (!identifier(input?.amazonUserId) || !identifier(input?.receiptId)) return respond(400, { error: "invalid_receipt_request" });
    } catch { return respond(400, { error: "invalid_receipt_request" }); }

    const secret = env("AMAZON_RVS_SHARED_SECRET");
    const sku = env("AMAZON_PREMIUM_SKU");
    const environment = env("AMAZON_RVS_ENVIRONMENT") || "production";
    if (!secret?.trim() || !sku?.trim() || !["production", "sandbox"].includes(environment)) {
      return respond(503, { error: "amazon_configuration_pending" });
    }
    const binding = { subject: principal.id, amazonUserId: input.amazonUserId, receiptId: input.receiptId, sku };
    const base = "https://appstore-sdk.amazon.com" + (environment === "sandbox" ? "/sandbox" : "");
    const url = `${base}/version/1.0/verifyReceiptId/developer/${encodeURIComponent(secret)}/user/${encodeURIComponent(input.amazonUserId)}/receiptId/${encodeURIComponent(input.receiptId)}`;
    try {
      const receiptResponse = await fetch(url, { redirect: "error", signal: AbortSignal.timeout(10000), headers: { Accept: "application/json" } });
      if (receiptResponse.status === 410) return respond(200, { ...binding, decision: "revoked" });
      if (receiptResponse.status === 429 || receiptResponse.status >= 500) return respond(503, { error: "verification_unavailable" });
      if (receiptResponse.status === 496) return respond(503, { error: "amazon_configuration_invalid" });
      if (receiptResponse.status !== 200) return respond(200, { ...binding, decision: "rejected" });
      const receipt = await boundedJson(receiptResponse, 65536);
      if (receipt?.receiptId !== input.receiptId || receipt?.productId !== sku || receipt?.productType !== "ENTITLED"
          || !("cancelDate" in receipt) || typeof receipt.testTransaction !== "boolean" || typeof receipt.betaProduct !== "boolean"
          || (environment === "production" && (receipt.testTransaction || receipt.betaProduct))) {
        return respond(200, { ...binding, decision: "rejected" });
      }
      if (receipt.cancelDate !== null) return respond(200, { ...binding, decision: "revoked" });
      return respond(200, { ...binding, decision: "verified", verifiedAt: now(), expiresAt: now() + 6 * 60 * 60 * 1000 });
    } catch {
      // Never echo Amazon URL/errors: its path contains the server's shared secret.
      return respond(503, { error: "verification_unavailable" });
    }
  };
}
