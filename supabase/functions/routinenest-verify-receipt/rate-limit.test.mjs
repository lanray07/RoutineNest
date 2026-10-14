import test from 'node:test';
import assert from 'node:assert/strict';
import { createDatabaseLimiter } from './rate-limit.mjs';
import { createHandler } from './handler.mjs';

test('rate bucket identity is a server HMAC, not a stored account identifier', async () => {
  let captured;
  const limit = createDatabaseLimiter({ env: name => ({ SUPABASE_URL: 'https://example.supabase.co', SUPABASE_SERVICE_ROLE_KEY: 'server-only-key' })[name],
    fetch: async (url, options) => { captured = { url, options }; return Response.json({ allowed: true, retryAfterSeconds: 60 }); } });
  assert.deepEqual(await limit('private-subject'), { allowed: true, retryAfterSeconds: 60 });
  assert.equal(captured.url, 'https://example.supabase.co/rest/v1/rpc/routinenest_take_receipt_slot');
  assert.match(JSON.parse(captured.options.body).p_subject_hash, /^[a-f0-9]{64}$/);
  assert.equal(captured.options.body.includes('private-subject'), false);
  assert.equal(captured.options.redirect, 'error');
});

test('malformed or unavailable counter responses fail closed', async () => {
  for (const result of [{ allowed: 'yes', retryAfterSeconds: 30 }, { allowed: true, retryAfterSeconds: 0 }, { allowed: false, retryAfterSeconds: 601 }]) {
    const limit = createDatabaseLimiter({ env: () => 'configured', fetch: async () => Response.json(result) });
    await assert.rejects(limit('subject'));
  }
  const limit = createDatabaseLimiter({ env: () => 'configured', fetch: async () => new Response('', { status: 503 }) });
  await assert.rejects(limit('subject'));
});

function endpoint(rateLimit) {
  return createHandler({ env: () => 'configured', now: () => 0, rateLimit,
    fetch: async url => { assert.match(url, /\/auth\/v1\/user$/); return Response.json({ id: 'verified-subject' }); } });
}
const request = () => new Request('https://example.supabase.co/functions/v1/verify', { method: 'POST', headers: { Authorization: 'Bearer session', 'Content-Type': 'application/json' }, body: JSON.stringify({ subject: 'forged-subject', amazonUserId: 'account', receiptId: 'receipt' }) });

test('denied quota never contacts Amazon and uses the verified subject', async () => {
  let subject;
  const response = await endpoint(async value => { subject = value; return { allowed: false, retryAfterSeconds: 120 }; })(request());
  assert.equal(subject, 'verified-subject');
  assert.equal(response.status, 429);
  assert.equal(response.headers.get('Retry-After'), '120');
});

test('missing or failed rate service never verifies a receipt', async () => {
  assert.equal((await endpoint(undefined)(request())).status, 503);
  const response = await endpoint(async () => { throw new Error('private server credentials'); })(request());
  assert.equal(response.status, 503);
  assert.equal((await response.text()).includes('credentials'), false);
});
