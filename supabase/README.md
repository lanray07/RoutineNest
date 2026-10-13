# RoutineNest billing function

Deployed to the existing free `lanray07's Project` (`guigiqacxpvcxrnajvlz`) at:
`https://guigiqacxpvcxrnajvlz.supabase.co/functions/v1/routinenest-verify-receipt`.
`verify_jwt=true`; the handler also calls `/auth/v1/user` to validate the principal.
Only a publishable key belongs in Android; it is not an authentication token.
No service-role/secret key is used by the client or function.

Run synthetic tests with `node --test supabase/functions/routinenest-verify-receipt/handler.test.mjs`.
Deployment includes `index.ts` and `handler.mjs`. There are no npm dependencies,
tables, migrations or paid add-ons. Keep `config.toml`'s JWT gate enabled.

Missing server secrets must be set in the
[Edge Function secrets dashboard](https://supabase.com/dashboard/project/guigiqacxpvcxrnajvlz/functions/secrets):
`AMAZON_RVS_SHARED_SECRET`, `AMAZON_PREMIUM_SKU` and
`AMAZON_RVS_ENVIRONMENT=production` (or `sandbox` for App Tester).
Do not paste secrets into chat or commit them. The shared secret is distinct from
Amazon's public authentication PEM and the publisher APK private signing key.

Anonymous sign-ins are currently disabled. Before changing that setting, review
the shared project's RLS/grants against the authenticated anonymous role and
implement signup/verification abuse controls. The Android client creates sessions
only during verification, not while using local routines. No global Auth setting
has been altered by this integration.

The endpoint returns account/receipt/SKU/subject-bound decisions and six-hour expiry
over HTTPS. It does not issue a signed entitlement token or store customer IDs in
application tables. Android authenticates its cache with Keystore AES-GCM. Real
RVS/Auth integration, clock-change policy, physical purchase tests and privacy
review remain required before enabling billing.

The existing free plan may pause after a week of inactivity and has quota limits;
no paid plan was selected. Local routines work offline. See
`docs/BILLING_INTEGRATION.md` and `docs/supabase-verification.json`.
