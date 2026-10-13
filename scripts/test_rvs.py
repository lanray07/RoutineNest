"""Contract tests use synthetic Amazon JSON; no live purchases or credentials."""
import json
from pathlib import Path
import sys
import unittest
from unittest.mock import patch
from urllib.error import URLError, HTTPError

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from backend.rvs import Config, Decision, NoRedirect, Verifier, amazon_get
from test_support import record


class RvsTests(unittest.TestCase):
    def receipt(self, **changes):
        receipt = dict(receiptId="receipt", productId="premium", productType="ENTITLED",
                       cancelDate=None, betaProduct=False, testTransaction=False)
        receipt.update(changes)
        return json.dumps(receipt).encode()

    def verify(self, body=None, status=200, sandbox=False):
        return Verifier(Config("server-secret", "premium", sandbox),
                        lambda url: (status, self.receipt() if body is None else body)).verify("user", "receipt")

    def test_verified_result_bound_to_requested_user_and_receipt(self):
        result = self.verify()
        self.assertTrue(result.premium)
        self.assertEqual(("user", "receipt"), (result.user_id, result.receipt_id))

    def test_wrong_receipt_sku_or_product_type_cannot_unlock(self):
        for changes in (dict(receiptId="another"), dict(productId="other"),
                        dict(productType="CONSUMABLE"), dict(productType="SUBSCRIPTION")):
            with self.subTest(changes=changes):
                self.assertEqual(Decision.REJECTED, self.verify(self.receipt(**changes)).decision)

    def test_cancellation_and_gone_receipt_revoke(self):
        for date in (0, 1700000000000):
            self.assertEqual(Decision.REVOKED, self.verify(self.receipt(cancelDate=date)).decision)
        self.assertEqual(Decision.REVOKED, self.verify(status=410).decision)

    def test_production_rejects_sandbox_and_lat_transactions(self):
        for flag in ("testTransaction", "betaProduct"):
            self.assertFalse(self.verify(self.receipt(**{flag: True})).premium)
            self.assertTrue(self.verify(self.receipt(**{flag: True}), sandbox=True).premium)

    def test_missing_or_malformed_policy_fields_deny(self):
        for key in ("cancelDate", "testTransaction", "betaProduct", "receiptId", "productType", "productId"):
            receipt = json.loads(self.receipt())
            del receipt[key]
            self.assertFalse(self.verify(json.dumps(receipt).encode()).premium)
        self.assertFalse(self.verify(self.receipt(testTransaction="false")).premium)

    def test_malformed_or_oversized_response_deny(self):
        for body in (b"not JSON", b"[]", b"null", b"\xff", b" " * 65537):
            self.assertFalse(self.verify(body).premium)

    def test_retryable_outage_is_distinct_from_invalid_receipt(self):
        for status in (429, 500, 503):
            self.assertEqual(Decision.RETRY, self.verify(status=status).decision)
        for status in (400, 497, 302):
            self.assertEqual(Decision.REJECTED, self.verify(status=status).decision)
        self.assertEqual(Decision.CONFIGURATION_ERROR, self.verify(status=496).decision)

    def test_request_has_fixed_host_and_encoded_path_segments(self):
        seen = []
        verifier = Verifier(Config("secret/part", "premium", True),
                            lambda url: (seen.append(url) or 400, b""))
        verifier.verify("user/../?", "receipt#&")
        self.assertEqual(["https://appstore-sdk.amazon.com/sandbox/version/1.0/verifyReceiptId/developer/secret%2Fpart/user/user%2F..%2F%3F/receiptId/receipt%23%26"], seen)

    def test_invalid_identifiers_do_not_contact_amazon(self):
        def must_not_call(url):
            self.fail("invalid input contacted Amazon")
        verifier = Verifier(Config("secret", "premium"), must_not_call)
        for user, receipt in (("", "receipt"), ("user", " "), (None, "receipt"), ("u" * 4097, "r")):
            self.assertFalse(verifier.verify(user, receipt).premium)

    def test_server_configuration_required_and_secret_not_in_repr(self):
        for secret, sku in (("", "premium"), ("secret", "")):
            with self.assertRaises(ValueError):
                Config(secret, sku)
        self.assertNotIn("server-secret", repr(Config("server-secret", "premium")))
        with patch.dict("os.environ", {"AMAZON_RVS_ENVIRONMENT": "typo"}):
            with self.assertRaises(ValueError):
                Config.from_environment()

    def test_transport_sanitizes_network_failure_and_http_error(self):
        with patch("backend.rvs.build_opener") as opener:
            opener.return_value.open.side_effect = URLError("secret in URL")
            self.assertEqual((503, b""), amazon_get("https://example.invalid/secret"))
            opener.return_value.open.side_effect = HTTPError("secret", 410, "gone", {}, None)
            self.assertEqual((410, b""), amazon_get("https://example.invalid/secret"))

    def test_redirect_is_never_followed(self):
        self.assertIsNone(NoRedirect().redirect_request(None, None, 302, "", {}, "https://other.invalid"))


if __name__ == "__main__":
    program = unittest.main(exit=False)
    record("Amazon RVS server contract (synthetic transport)", program.result)
    sys.exit(0 if program.result.wasSuccessful() else 1)
