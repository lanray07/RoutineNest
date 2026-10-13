"""Amazon RVS transport and one-time entitlement policy; no public HTTP endpoint.

An authenticated API must bind its caller to amazon_user_id before using this
module. A successful Amazon response binds the receipt to that user through the
request URL; Amazon does not return a userId field in the receipt JSON.
"""
from dataclasses import dataclass, field
from enum import Enum
import json
import os
from urllib.error import HTTPError, URLError
from urllib.parse import quote
from urllib.request import Request, HTTPRedirectHandler, build_opener


class Decision(str, Enum):
    VERIFIED = "verified"
    REJECTED = "rejected"
    REVOKED = "revoked"
    RETRY = "retry"
    CONFIGURATION_ERROR = "configuration_error"


@dataclass(frozen=True)
class Result:
    decision: Decision
    user_id: str
    receipt_id: str

    @property
    def premium(self):
        return self.decision is Decision.VERIFIED


@dataclass(frozen=True)
class Config:
    shared_secret: str = field(repr=False)
    premium_sku: str
    sandbox: bool = False

    def __post_init__(self):
        if not self.shared_secret.strip() or not self.premium_sku.strip():
            raise ValueError("RVS server secret and premium SKU must be configured")

    @classmethod
    def from_environment(cls):
        environment = os.environ.get("AMAZON_RVS_ENVIRONMENT", "production")
        if environment not in ("production", "sandbox"):
            raise ValueError("AMAZON_RVS_ENVIRONMENT must be production or sandbox")
        return cls(os.environ.get("AMAZON_RVS_SHARED_SECRET", ""),
                   os.environ.get("AMAZON_PREMIUM_SKU", ""), environment == "sandbox")


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        # The URL contains a server secret. Never forward it to another URL.
        return None


def amazon_get(url):
    """Return status/body without surfacing URLs, identifiers or secrets in errors."""
    try:
        request = Request(url, headers={"Accept": "application/json"})
        with build_opener(NoRedirect()).open(request, timeout=10) as response:
            body = response.read(65537)
            return response.status, body if len(body) <= 65536 else b""
    except HTTPError as error:
        error.close()
        return error.code, b""
    except (URLError, TimeoutError, OSError):
        return 503, b""


class Verifier:
    def __init__(self, config, transport=amazon_get):
        self.config = config
        self.transport = transport

    def verify(self, amazon_user_id, receipt_id):
        if not all(isinstance(value, str) and value.strip() and len(value) <= 4096
                   for value in (amazon_user_id, receipt_id)):
            return Result(Decision.REJECTED, "", "")
        base = "https://appstore-sdk.amazon.com"
        if self.config.sandbox:
            base += "/sandbox"
        url = (base + "/version/1.0/verifyReceiptId/developer/"
               + quote(self.config.shared_secret, safe="") + "/user/"
               + quote(amazon_user_id, safe="") + "/receiptId/" + quote(receipt_id, safe=""))
        status, body = self.transport(url)
        decision = Decision.REJECTED
        if status == 410:
            decision = Decision.REVOKED
        elif status == 496:
            decision = Decision.CONFIGURATION_ERROR
        elif status == 429 or status >= 500:
            decision = Decision.RETRY
        elif status == 200:
            decision = self._check_receipt(body, receipt_id)
        return Result(decision, amazon_user_id, receipt_id)

    def _check_receipt(self, body, receipt_id):
        try:
            if len(body) > 65536:
                return Decision.REJECTED
            receipt = json.loads(body)
            if not isinstance(receipt, dict):
                return Decision.REJECTED
            if (receipt.get("receiptId") != receipt_id
                    or receipt.get("productId") != self.config.premium_sku
                    or receipt.get("productType") != "ENTITLED"):
                return Decision.REJECTED
            # Missing/malformed cancellation and test markers must fail closed.
            if "cancelDate" not in receipt:
                return Decision.REJECTED
            if receipt["cancelDate"] is not None:
                return Decision.REVOKED
            if not all(type(receipt.get(key)) is bool for key in ("testTransaction", "betaProduct")):
                return Decision.REJECTED
            if not self.config.sandbox and (receipt["testTransaction"] or receipt["betaProduct"]):
                return Decision.REJECTED
            return Decision.VERIFIED
        except (ValueError, TypeError, UnicodeError):
            return Decision.REJECTED
