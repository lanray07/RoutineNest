# Amazon purchasing integration status

The app includes native Appstore SDK **3.0.9**, the required protected/exported ResponseReceiver, listener registration when a public authentication PEM is present, product-price discovery, purchase callbacks, purchase-update pagination, cancellation handling, account-bound cached entitlement storage and a `ReceiptVerifier` boundary. No fake adapter is selected in the running app. The fixture is located exclusively in `app/src/debug`; release sources cannot use it.

`verificationConfigured` is deliberately false. `UnconfiguredReceiptVerifier` always denies verification. The paywall explains the blocker, and neither tapping purchase nor restore enables premium. This is a buildable integration boundary, not a tested production purchase service.

The server-side `backend/rvs.py` component now implements Amazon HTTPS verification, receipt/SKU/ENTITLED/cancellation matching, production rejection of test transactions, bounded responses, disabled redirects and distinct retry/revocation/configuration outcomes. Twelve synthetic contract tests pass. It does not expose an authenticated endpoint or connect to Android yet. See `backend/README.md`; the RVS secret stays server-side. Amazon binds the user through the successful request path, not a nonexistent `userId` JSON field.

Complete the following before enabling checkout:

1. Confirm publisher account, package identifier and one-time **ENTITLED** SKU. Validate the proposed UK £2.99 price against the market; provide prices in Amazon's product setup. The app uses the store-returned price and does not hardcode £2.99.
2. Get the app-specific **public** `AppstoreAuthenticationKey.pem` from the Amazon console and place it in `app/src/main/assets/`. This is distinct from publisher APK private signing keys and the RVS shared secret.
3. Implement an authenticated HTTPS verifier server. Keep the RVS shared secret on that server only. Accept receipt/user identifiers, call Amazon RVS, validate success, user, receipt, SKU, ENTITLED product type and cancellation state. Return a bound verified result; do not trust client assertions.
4. Replace `UnconfiguredReceiptVerifier` with that client, document server retention/privacy and add contract/integration tests. Use ordered account/restore reconciliation to prevent stale callback results overwriting newer account state. A signed/verifiable offline entitlement cache and revocation refresh policy should be established for the final server design.
5. Turn on the configuration flag only after these changes and tests pass. Exercise real product discovery, purchase cancellation/failure, verification outage, receipt rejection, successful purchase, restore pagination, revocation, account change and cached offline access with Amazon App Tester and Live App Testing on Fire hardware.
6. Verify fulfillment is acknowledged **after** verified entitlement is durably stored. Handle retries/idempotency without losing an unacknowledged successful payment. Define support procedures for pending verification.

Executed local tests cover pure entitlement guards and debug fixture purchase/restore; they do not execute Amazon checkout, Amazon RVS, App Tester or Live App Testing. Release readiness is blocked on those missing integrations and checks. No live purchase is authorised.

Official documentation: https://developer.amazon.com/docs/appstore-sdk/integrate-appstore-sdk.html and https://developer.amazon.com/docs/in-app-purchasing/iap-rvs-for-android-apps.html . Current requirements and links are also recorded in AMAZON_REQUIREMENTS.md.
