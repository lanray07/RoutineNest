package com.routinenest.app

import androidx.test.platform.app.InstrumentationRegistry
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Explicit App Tester run only. Never opens checkout or acknowledges a purchase. */
class NativeAmazonSandboxTest {
    @Test fun actualSdkDiscoversTesterAccountAndPremiumProduct() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".debug"))
        assumeTrue("Run explicitly with -e appTester true", InstrumentationRegistry.getArguments().getString("appTester") == "true")
        val mode = instrumentation.uiAutomation.executeShellCommand("getprop debug.amazon.sandboxmode").use { descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).use { String(it.readBytes(), Charsets.UTF_8).trim() }
        }
        assertEquals("The SDK probe must never use the production store", "debug", mode)
        val userReady = CountDownLatch(1)
        val productReady = CountDownLatch(1)
        var user: UserDataResponse? = null
        var products: ProductDataResponse? = null
        instrumentation.runOnMainSync {
            PurchasingService.registerListener(context, object : PurchasingListener {
                override fun onUserDataResponse(response: UserDataResponse) { user = response; userReady.countDown() }
                override fun onProductDataResponse(response: ProductDataResponse) { products = response; productReady.countDown() }
                override fun onPurchaseResponse(response: PurchaseResponse) { error("This probe must never purchase") }
                override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) { error("This probe must never restore") }
            })
            PurchasingService.getUserData()
            PurchasingService.getProductData(setOf(BuildConfig.PREMIUM_SKU))
        }
        assertTrue("App Tester account callback timed out", userReady.await(30, TimeUnit.SECONDS))
        assertTrue("App Tester product callback timed out", productReady.await(30, TimeUnit.SECONDS))
        assertEquals(UserDataResponse.RequestStatus.SUCCESSFUL, user!!.requestStatus)
        assertFalse("Tester must return an account identifier", user!!.userData.userId.isBlank())
        assertEquals(ProductDataResponse.RequestStatus.SUCCESSFUL, products!!.requestStatus)
        val product = products!!.productData[BuildConfig.PREMIUM_SKU]
        assertNotNull("The real SDK must discover the configured SKU", product)
        assertEquals(ProductType.ENTITLED, product!!.productType)
        assertFalse("Price must be supplied by the SDK", product.price.isBlank())
    }
}
