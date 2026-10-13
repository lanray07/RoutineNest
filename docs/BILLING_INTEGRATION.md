# Amazon purchasing integration status

The app includes native Appstore SDK **3.0.9**, the required protected/exported ResponseReceiver, listener registration when a public authentication PEM is present, product-price discovery, purchase callbacks, purchase-update pagination, cancellation handling, account-bound cached entitlement storage and a `ReceiptVerifier` boundary. No fake adapter is selected in the running app. The fixture is located exclusively in `app/src/debug`; release sources cannot use it.

`verificationConfigured` remains false. The app now selects the real `SupabaseReceiptVerifier`, but checkout is blocked pending Amazon credentials, Auth setup, abuse controls and real integration testing. Neither tapping purchase nor restore enables premium yet.

The authenticated Supabase `routinenest-verify-receipt` function is deployed ACTIVE, version 1, to the user-selected free **lanray07's Project** (`guigiqacxpvcxrnajvlz`). It requires a user JWT (`verify_jwt=true`) and confirms the principal through Supabase Auth. No global Auth settings or database tables were changed; the function never queries the project's existing booking tables. Only the public URL and publishable key are included in Android. No service-role/secret key belongs in the client.

The deployed JavaScript handler and independent Python reference policy match receipt/SKU/ENTITLED/cancellation data, reject production test transactions, bound responses, disable redirects and separate retry/revocation/configuration failures. Twelve Python policy tests and twelve JavaScript endpoint tests passed with synthetic responses. The live endpoint rejected missing and invalid tokens with HTTP 401. No real Amazon receipt has been checked. Amazon binds the receipt/user pair through its successful request path, not a nonexistent `userId` JSON field. Supabase Auth identifies the app session; this is not a persistent identity mapping to the Amazon account.

The Android client checks subject/account/receipt/SKU/expiry over HTTPS, refreshes Auth sessions and encrypts tokens with Android Keystore AES-GCM. The adapter persists an authenticated encrypted lease with a six-hour server expiry, verifies the current Amazon account before restoring cached access, ignores stale callback results and periodically checks cached expiry. Restore waits for the authoritative account callback. Device clock changes, pagination/reconciliation and fulfillment retry still require real purchase tests.

Complete the following before enabling checkout:

1. Confirm publisher account, package identifier and one-time **ENTITLED** SKU. Validate the proposed UK £2.99 price against the market; provide prices in Amazon's product setup. The app uses the store-returned price and does not hardcode £2.99.
2. Get the app-specific **public** `AppstoreAuthenticationKey.pem` from the Amazon console and place it in `app/src/main/assets/`. This is distinct from publisher APK private signing keys and the RVS shared secret.
3. Set `AMAZON_RVS_SHARED_SECRET`, `AMAZON_PREMIUM_SKU` and `AMAZON_RVS_ENVIRONMENT` in Supabase Edge Function secrets, never Android/Git. Use sandbox for App Tester. Define a separate LAT policy; production currently rejects LAT/test receipts.
4. Anonymous Auth is currently disabled in this shared project. Review its existing RLS/grants before enabling anonymous users (who receive the authenticated role). Add signup abuse protection and distributed verification rate limiting before production. The current client does not implement CAPTCHA. Document privacy/provider retention and complete authenticated client/sandbox integration tests; do not enable checkout until these gates pass.
5. Turn on the configuration flag only after these changes and tests pass. Exercise real product discovery, purchase cancellation/failure, verification outage, receipt rejection, successful purchase, restore pagination, revocation, account change and cached offline access with Amazon App Tester and Live App Testing on Fire hardware.
6. Verify fulfillment is acknowledged **after** verified entitlement is durably stored. Handle retries/idempotency without losing an unacknowledged successful payment. Define support procedures for pending verification.

Executed local tests cover pure entitlement guards and debug fixture purchase/restore; they do not execute Amazon checkout, Amazon RVS, App Tester or Live App Testing. Release readiness is blocked on those missing integrations and checks. No live purchase is authorised.

Official documentation: https://developer.amazon.com/docs/appstore-sdk/integrate-appstore-sdk.html and https://developer.amazon.com/docs/in-app-purchasing/iap-rvs-for-android-apps.html . Current requirements and links are also recorded in AMAZON_REQUIREMENTS.md.
