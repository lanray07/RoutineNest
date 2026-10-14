import { createHandler } from "./handler.mjs";
import { createDatabaseLimiter } from "./rate-limit.mjs";

Deno.serve(createHandler({
  env: (name: string) => Deno.env.get(name),
  fetch: globalThis.fetch,
  now: () => Date.now(),
  rateLimit: createDatabaseLimiter({ env: (name: string) => Deno.env.get(name), fetch: globalThis.fetch }),
}));
