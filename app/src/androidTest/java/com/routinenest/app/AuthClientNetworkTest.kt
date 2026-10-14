package com.routinenest.app

import androidx.test.platform.app.InstrumentationRegistry
import com.amazon.device.iap.internal.model.ReceiptBuilder
import com.amazon.device.iap.model.ProductType
import com.routinenest.app.billing.EncryptedBillingSession
import com.routinenest.app.billing.SupabaseReceiptVerifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Date

/** Explicit temporary-Auth network check. The receipt is synthetic, never a payment. */
class AuthClientNetworkTest {
    @Test fun realAnonymousSessionAndRefreshCannotUnlockInvalidReceipt() = runBlocking {
        assumeTrue("Requires explicit temporary Auth approval", InstrumentationRegistry.getArguments().getString("liveSupabase") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".debug"))
        val store = EncryptedBillingSession(context)
        assumeTrue("Do not replace an existing debug billing session", store.read() == null)
        val receipt = ReceiptBuilder().setReceiptId("routinenest-test-only-invalid-receipt")
            .setSku(BuildConfig.PREMIUM_SKU).setProductType(ProductType.ENTITLED).setPurchaseDate(Date()).build()
        val verifier = SupabaseReceiptVerifier(context)
        try {
            assertNull("Synthetic receipt must never unlock Premium", verifier.verify("routinenest-test-only", receipt))
            val first = requireNotNull(store.read()) { "The real client must create an encrypted session" }
            val subject = first.getJSONObject("user").getString("id")
            assertTrue(first.getString("access_token").isNotBlank())
            assertTrue(first.getString("refresh_token").isNotBlank())
            File(context.getExternalFilesDir(null), "auth-client-test-subject.txt").writeText(subject)
            store.write(first.put("expires_at", 0))
            assertNull(verifier.verify("routinenest-test-only", receipt))
            val refreshed = requireNotNull(store.read())
            assertEquals(subject, refreshed.getJSONObject("user").getString("id"))
            assertTrue("Expired session must refresh through real Auth", refreshed.getLong("expires_at") * 1000 > System.currentTimeMillis())
        } finally {
            store.clear()
        }
    }
}
