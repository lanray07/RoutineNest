# Server-side Amazon receipt verification

`rvs.py` is an executable, dependency-free verification component for a future
authenticated service. It is not a deployed service or a public HTTP endpoint.
The Android app still uses `UnconfiguredReceiptVerifier` and cannot unlock premium.

Set `AMAZON_RVS_SHARED_SECRET`, `AMAZON_PREMIUM_SKU` and
`AMAZON_RVS_ENVIRONMENT=production` in the **server** environment. Use `sandbox`
only for App Tester integration. Never place the secret in Android resources,
BuildConfig, a committed `.env` file, telemetry or HTTP access logs. Amazon's RVS
URL includes the secret in its path; transport errors are reduced to status codes.
Redirects are disabled, response bodies are bounded and requests have a timeout.

```python
from backend.rvs import Config, Verifier

verifier = Verifier(Config.from_environment())
# First authenticate the caller and establish its Amazon user binding.
result = verifier.verify(amazon_user_id, receipt_id)
# Only result.premium permits access. Other decisions must never grant premium.
```

The policy requires Amazon HTTP 200, matching receipt and configured SKU,
`ENTITLED`, an explicit null cancellation date, and correctly typed test markers.
Production rejects App Tester and Live App Testing transactions. Amazon's JSON
does not contain a user ID: HTTP 200 validates the user in the request path.
HTTP 410 revokes; 429/5xx and network outages request retry; 496 identifies server
configuration failure. Retry decisions do not establish a new entitlement.
The component is stateless and does not persist customer identifiers.

Before exposing an endpoint, implement caller authentication, Amazon-account
binding, rate limiting, a minimal request/response contract and privacy/retention
policy. Do not authenticate the endpoint with a shared API key embedded in an APK.
Add integration tests against RVS sandbox, production configuration separation,
client account changes, ordered restore pages, revocation and retry/fulfillment.
Define signed offline access and refresh/expiry policy with the final service.
Deployment and store/device integration remain release blockers.

Run `python scripts/test_rvs.py`. Tests use synthetic payloads and mocked network
failures; no Amazon endpoint, live receipt or paid transaction is exercised.

Protocol source: [Amazon RVS request, fields and status codes](https://developer.amazon.com/docs/in-app-purchasing/iap-rvs-for-android-apps.html), checked 13 October 2026.
