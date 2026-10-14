package com.routinenest.app

import androidx.test.platform.app.InstrumentationRegistry
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.*
import com.routinenest.app.billing.EncryptedBillingSession
import com.routinenest.app.billing.SupabaseReceiptVerifier
import com.routinenest.core.Feature
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Component integration through App Tester and RVS Cloud Sandbox; never a live charge. */
class NativeAmazonReceiptTest {
    @get:Rule val display = DeviceScreenRule()

    @Test fun testerReceiptVerifiesThroughRealServerAndRestores() = runBlocking {
        assumeTrue("Explicit sandbox integration run only", InstrumentationRegistry.getArguments().getString("sandboxReceipt") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".debug"))
        val mode = instrumentation.uiAutomation.executeShellCommand("getprop debug.amazon.sandboxmode").use { descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).use { String(it.readBytes(), Charsets.UTF_8).trim() }
        }
        assertEquals("Never route this test to production checkout", "debug", mode)
        val session = EncryptedBillingSession(context)
        assumeTrue("Preserve any existing debug billing session", session.read() == null)
        val leaseStore = EncryptedBillingSession(context, "rvs-sandbox-lease-test")
        val userReady = CountDownLatch(1)
        val productReady = CountDownLatch(1)
        val purchaseReady = CountDownLatch(1)
        val restoreReady = CountDownLatch(1)
        var user: UserDataResponse? = null
        var product: ProductDataResponse? = null
        var purchase: PurchaseResponse? = null
        var restored: PurchaseUpdatesResponse? = null
        var acknowledged = false
        instrumentation.runOnMainSync {
            PurchasingService.registerListener(context, object : PurchasingListener {
                override fun onUserDataResponse(response: UserDataResponse) { user = response; userReady.countDown() }
                override fun onProductDataResponse(response: ProductDataResponse) { product = response; productReady.countDown() }
                override fun onPurchaseResponse(response: PurchaseResponse) { purchase = response; purchaseReady.countDown() }
                override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) { restored = response; restoreReady.countDown() }
            })
            PurchasingService.getUserData()
            PurchasingService.getProductData(setOf(BuildConfig.PREMIUM_SKU))
        }
        try {
            assertTrue(userReady.await(30, TimeUnit.SECONDS))
            assertTrue(productReady.await(30, TimeUnit.SECONDS))
            assertEquals(UserDataResponse.RequestStatus.SUCCESSFUL, user!!.requestStatus)
            // This is the documented, fictitious App Tester User 1, never the publisher's account.
            assertEquals("l3HL7XppEMhrOGDnur9-ulvqomrSg6qyODKmah76lJU=", user!!.userData.userId)
            assertEquals(ProductDataResponse.RequestStatus.SUCCESSFUL, product!!.requestStatus)
            assertEquals(ProductType.ENTITLED, product!!.productData[BuildConfig.PREMIUM_SKU]!!.productType)
            instrumentation.runOnMainSync { PurchasingService.purchase(BuildConfig.PREMIUM_SKU) }
            assertTrue("App Tester purchase callback timed out", purchaseReady.await(90, TimeUnit.SECONDS))
            assertEquals(PurchaseResponse.RequestStatus.SUCCESSFUL, purchase!!.requestStatus)
            val response = purchase!!
            assertEquals(user!!.userData.userId, response.userData.userId)
            assertEquals(BuildConfig.PREMIUM_SKU, response.receipt.sku)
            val verifier = SupabaseReceiptVerifier(context)
            val entitlement = verifier.verify(response.userData.userId, response.receipt)
            assertTrue("Actual sandbox receipt must pass authenticated server RVS", entitlement?.permits(Feature.MORE_ROUTINES) == true)
            leaseStore.write(JSONObject().put("receipt", entitlement!!.receiptId).put("user", entitlement.userId).put("expiresAt", entitlement.expiresAt))
            assertEquals(response.receipt.receiptId, leaseStore.read()!!.getString("receipt"))
            instrumentation.runOnMainSync { PurchasingService.notifyFulfillment(response.receipt.receiptId, FulfillmentResult.FULFILLED) }
            acknowledged = true
            instrumentation.runOnMainSync { PurchasingService.getPurchaseUpdates(true) }
            assertTrue(restoreReady.await(30, TimeUnit.SECONDS))
            assertEquals(PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL, restored!!.requestStatus)
            assertEquals(response.userData.userId, restored!!.userData.userId)
            val receipt = restored!!.receipts.firstOrNull { it.sku == BuildConfig.PREMIUM_SKU && it.receiptId == response.receipt.receiptId }
            assertNotNull("Owned entitlement must be returned for restore", receipt)
            assertTrue(verifier.verify(restored!!.userData.userId, receipt!!)?.permits(Feature.MORE_ROUTINES) == true)
        } finally {
            session.read()?.optJSONObject("user")?.optString("id")?.takeIf { it.isNotBlank() }?.let {
                File(context.getExternalFilesDir(null), "sandbox-receipt-test-subject.txt").writeText(it)
            }
            if (!acknowledged && purchase?.requestStatus == PurchaseResponse.RequestStatus.SUCCESSFUL) {
                instrumentation.runOnMainSync { PurchasingService.notifyFulfillment(purchase!!.receipt.receiptId, FulfillmentResult.UNAVAILABLE) }
            }
            leaseStore.clear()
            session.clear()
        }
    }
}
