# Amazon listing preparation checklist

Prepared 13 October 2026. Every box remains unchecked until verified against the actual release. See [verified requirements](../docs/AMAZON_REQUIREMENTS.md) for specifications and primary sources. These files do not indicate publication or approval.

## App and account

- [ ] Owner has reviewed final app name, package ID, support contact, and release version.
- [ ] Developer identity/account setup is complete.
- [ ] Payment, banking, and tax profile is complete if monetized. [Amazon pricing requirements](https://developer.amazon.com/docs/app-submission/appstore-details.html#pricing)
- [ ] Release binary exists, is signed, installs, and matches the listed version.
- [ ] Actual Fire tablets and supported OS versions are recorded with test results.
- [ ] Console-supported devices and territories match tested scope.
- [ ] Offline behavior, reminders, resume, data persistence, accessibility, and portrait/landscape behavior are checked.

## Listing copy and visuals

- [ ] Display title is validated in the console.
- [ ] Short/long description limits are validated using the linked requirements.
- [ ] Descriptions contain only features confirmed in the release.
- [ ] Feature bullets and optional keywords are reviewed.
- [ ] Required icons and screenshot set meet the linked asset specifications.
- [ ] Screenshots are actual released app screens with demonstration data.
- [ ] Optional promotional artwork is reviewed for legibility and rights.
- [ ] Metadata translations match app support; the intended twelve-language list is obtained and recorded.
- [ ] Every localized listing and screenshot has fluent-speaker review.

## Billing, privacy, and release evidence

- [ ] App-specific public key is present; native SDK dependency is pinned.
- [ ] Product IDs, type, and prices match the developer console catalog.
- [ ] RVS backend configuration is complete; secrets are absent from client artifacts.
- [ ] Verification, fulfillment, refund handling, and restore are tested.
- [ ] No mocked/local-only unlock is represented as a live verified purchase.
- [ ] Privacy policy/support URLs are owned, public, and reachable.
- [ ] Privacy answers include Amazon billing/verification and all other SDK behavior.
- [ ] Target audience, content rating, account deletion (if applicable), and content rights are reviewed.
- [ ] Required IAP and app review steps are completed with recorded evidence.
- [ ] Final release decision is recorded by the app owner before submission.

## Evidence to attach before release

| Evidence | Current status |
| --- | --- |
| Developer account and app listing ID | Not verified |
| Signed release APK/AAB checksum | Not recorded |
| Physical Fire tablet test report | Not recorded |
| Screenshots from release binary | Not verified |
| SKU catalog and live prices | Not verified |
| Sandbox and Live App Testing results | Not recorded |
| Production receipt verification service | Not verified |
| Intended twelve-language list and review | Missing from supplied context |
| Live privacy/support URLs | Not verified |
| Submission/review outcome | Not submitted by this work |
