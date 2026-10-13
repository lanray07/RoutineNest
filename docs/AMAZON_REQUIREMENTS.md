# RoutineNest: Amazon submission requirements

Verified against official Amazon documentation on **13 October 2026**. This is preparation guidance, not evidence of submission, approval, physical-device testing, or production billing readiness.

## Supported distribution

Amazon ended its Appstore support for third-party Android mobile devices on 20 August 2025; new submissions for those devices stopped on 20 February 2025. Fire tablets, Fire TV, and Fire TV built-in products remain supported. RoutineNest should target **Fire tablets**; TV requires a separate suitable interaction design. An Android APK build does not establish current third-party Android Appstore distribution eligibility. [Amazon announcement](https://developer.amazon.com/apps-and-games/blogs/2025/02/upcoming-changes-to-amazon-appstore-for-android-devices-and-coins-program)

Submission requires an Amazon developer account, a Fire OS APK or AAB, listing copy/assets, and testing on a physical device. Vega OS uses VPKG and is a separate port, not an APK destination. [Submission requirements](https://developer.amazon.com/docs/app-submission/submitting-apps-to-amazon-appstore.html)

## Binary and compatibility

Amazon's current target SDK minimums are Fire OS 6: API 25, Fire OS 7: API 28, Fire OS 8: API 30, Fire OS 14: API 34, and Fire OS 16: API 36. These are **targetSdkVersion**, not minimum supported device API levels. Choose supported tablets explicitly and inspect the console's compatibility results. Required hardware features can exclude devices; avoid unnecessary requirements. Maintain the package name across updates and increment version codes. [Device compatibility](https://developer.amazon.com/docs/app-submission/device-filtering-and-compatibility.html)

The Developer Console collects supported devices, territorial availability, target audience, and content ratings. Complete these from the actual app; do not infer a child-directed audience simply from a family-oriented product. [Target Your App](https://developer.amazon.com/docs/app-submission/target-app.html)

## Tablet listing specifications

| Item | Official specification |
| --- | --- |
| Short description | 2,000 bytes; Amazon illustrates approximately 1,200 English or 400 Japanese/Chinese characters. Validate UTF-8 bytes. |
| Long description | Maximum 4,000 characters; plain text, no HTML. |
| Features | Three to five feature bullets. |
| Keywords | Optional, comma-separated. |
| Icons | 114×114 and 512×512; transparent PNG. |
| Screenshots | Three to ten; PNG/JPEG; landscape or portrait. Allowed dimensions: 800×480, 1024×600, 1280×720, 1280×800, 1920×1080, 1920×1200, 2560×1600. |
| Promotional image | Optional; 1024×500 PNG/JPEG; brand artwork rather than a screenshot. |

Screenshots must depict the tablet experience and use dummy personal information. File extensions must match file encoding. The display-title documentation gives no numeric cap; confirm the live console rather than inventing one. [Appstore details and assets](https://developer.amazon.com/docs/app-submission/appstore-details.html)

## Localization gap

The request references an earlier set of twelve languages, but that list was not supplied in the available context. Do not describe an assumed set as the user's chosen locales. Record actual implemented locales and obtain the intended list before claiming twelve-language completion. Store translations must match supported app languages and be reviewed by fluent speakers; this is a project acceptance recommendation.

## Amazon IAP integration

Use the native Appstore SDK. The latest documented release at verification is **3.0.9**, released 20 May 2026. [Release notes](https://developer.amazon.com/docs/appstore-sdk/release-notes.html) Billing Compatibility SDK support and maintenance ended 15 September 2026. [Compatibility SDK notice](https://developer.amazon.com/docs/in-app-purchasing/billing-compatibility-troubleshoot.html)

Resolve the pinned dependency `com.amazon.device:amazon-appstore-sdk:3.0.9` through Maven Central. Production integration needs the actual app-specific `AppstoreAuthenticationKey.pem` in the application's assets folder. This is a public key, distinct from the secret for server-side receipt verification. Do not substitute a dummy PEM. [SDK integration](https://developer.amazon.com/docs/appstore-sdk/integrate-appstore-sdk.html)

Register `PurchasingListener` before requests; handle asynchronous user, product, purchase, and purchase-update callbacks. Refresh user and purchase data on resume, account for pagination, and scope entitlements to the current Amazon user. Display localized prices from `getProductData()`. Declare `com.amazon.device.iap.ResponseReceiver` with `android:exported="true"`, permission `com.amazon.inapp.purchasing.Permission.NOTIFY`, and action `com.amazon.inapp.purchasing.NOTIFY`. Verify receipts before fulfilling and call `notifyFulfillment()` after successful delivery. Failed/canceled purchases must not unlock premium. [Implement IAP](https://developer.amazon.com/docs/in-app-purchasing/iap-implement-iap.html)

Create the app and matching, case-sensitive IAP SKUs in the console. SKU identifiers must be unique across the developer's apps, at most 150 characters, using letters, digits, underscores, periods, or dashes. Choose Entitlement for a non-consumable lifetime unlock; use Subscription only for recurring access. IAP items must be submitted before app testing/submission. [IAP item setup](https://developer.amazon.com/docs/in-app-purchasing/iap-create-and-submit-iap-items.html)

## Receipt verification and entitlement design

RVS verifies user ID plus receipt ID using the developer shared secret. Sandbox inserts `/sandbox` into the production path:

```text
https://appstore-sdk.amazon.com/[sandbox/]version/1.0/verifyReceiptId/developer/{shared-secret}/user/{user-id}/receiptId/{receipt-id}
```

HTTP 200 means identifiers validate, but inspect the returned product and cancellation/expiry data. HTTP 400 is invalid/missing; 410 is canceled; 429 is throttled; 496/497 indicate invalid secret/user; 500 is a server error. `cancelDate` can indicate cancellation or subscription expiry. Sandbox receipts and live receipts use separate environments. [RVS reference](https://developer.amazon.com/docs/in-app-purchasing/iap-rvs-for-android-apps.html)

Project architecture recommendations: keep the shared secret on a backend; encode path segments; redact request URLs from logs; bind authenticated app accounts to verified Amazon users; allowlist the expected SKU; make entitlement grants idempotent by receipt ID; retry temporary verification failures without granting access; reconcile refunds and restores. If no backend is configured, label billing as incomplete and keep purchase verification fail-closed. A local preference alone is insufficient evidence of paid entitlement.

Use App Tester, the RVS sandbox, and Live App Testing as applicable. Test reinstall/restore, account switching, cancellation, unavailable products, network failures, and duplicate callbacks before release. Test services are not proof of a production purchase. [IAP testing](https://developer.amazon.com/docs/in-app-purchasing/iap-testing-overview.html)

## Privacy and review

Since Q2 2025, new apps and updates require a completed privacy questionnaire without unresolved issues. Review third-party SDK collection as well as your own code. Apps collecting individual-associated data require a privacy policy. Do not claim that data stays entirely on the device if purchase receipts or identifiers are sent to a verification service. [Privacy labels](https://developer.amazon.com/docs/app-submission/appstore-privacy-labels.html)

Check content policy, binary preparation, privacy disclosures, and image requirements before submission. [Official presubmission checklist](https://developer.amazon.com/docs/app-submission/presubmission-checklist.html)
