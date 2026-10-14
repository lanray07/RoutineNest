import { boundedJson } from './handler.mjs';

/** Shared Postgres counters prevent limits being reset by new Edge instances. */
export function createDatabaseLimiter({ env, fetch, crypto = globalThis.crypto }) {
  return async subject => {
    const project = env('SUPABASE_URL');
    const secret = env('SUPABASE_SERVICE_ROLE_KEY');
    if (!project || !secret) throw new Error('Rate limiter configuration pending');
    const key = await crypto.subtle.importKey('raw', new TextEncoder().encode(secret), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
    const signature = await crypto.subtle.sign('HMAC', key, new TextEncoder().encode('routinenest-receipt:' + subject));
    const hash = Array.from(new Uint8Array(signature), byte => byte.toString(16).padStart(2, '0')).join('');
    const response = await fetch(`${project}/rest/v1/rpc/routinenest_take_receipt_slot`, {
      method: 'POST', headers: { Authorization: `Bearer ${secret}`, apikey: secret, 'Content-Type': 'application/json' },
      body: JSON.stringify({ p_subject_hash: hash }), redirect: 'error', signal: AbortSignal.timeout(5000),
    });
    if (!response.ok) throw new Error('Rate limiter unavailable');
    const decision = await boundedJson(response, 4096);
    if (typeof decision?.allowed !== 'boolean' || !Number.isInteger(decision.retryAfterSeconds) || decision.retryAfterSeconds < 1 || decision.retryAfterSeconds > 600) throw new Error('Invalid rate limiter response');
    return decision;
  };
}
