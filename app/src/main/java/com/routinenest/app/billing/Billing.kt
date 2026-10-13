package com.routinenest.app.billing

import android.content.Context
import com.amazon.device.iap.*
import com.amazon.device.iap.model.*
import com.routinenest.app.BuildConfig
import com.routinenest.core.Entitlement
import com.routinenest.core.Feature
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

data class BillingState(val premium: Boolean = false, val price: String? = null, val available: Boolean = false, val busy: Boolean = false, val message: String = "Amazon product and receipt verification are not configured.")
interface BillingAdapter { val state: StateFlow<BillingState>; fun purchase(); fun restore() }
interface ReceiptVerifier { suspend fun verify(user: String, receipt: Receipt): Entitlement? }
// Replace with an authenticated server RVS client. Never put the Amazon shared secret in an APK.
class UnconfiguredReceiptVerifier : ReceiptVerifier { override suspend fun verify(user: String, receipt: Receipt): Entitlement? = null }

@Singleton
class AmazonBilling @Inject constructor(@ApplicationContext private val context: Context) : BillingAdapter, PurchasingListener {
    private val preferences = context.getSharedPreferences("verified_entitlement", Context.MODE_PRIVATE)
    private val verifier: ReceiptVerifier = UnconfiguredReceiptVerifier()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(BillingState(premium = Entitlement(preferences.getString("user", "").orEmpty(), preferences.getString("receipt", "").orEmpty(), preferences.getBoolean("verified", false)).permits(Feature.MORE_ROUTINES)))
    override val state: StateFlow<BillingState> = mutableState.asStateFlow()
    private var userId = ""
    // Purchasing stays fail-closed until a server verifier is supplied and tested.
    private val verificationConfigured = false
    init {
        val hasKey = context.assets.list("")?.contains("AppstoreAuthenticationKey.pem") == true
        if (hasKey) {
            PurchasingService.registerListener(context, this)
            PurchasingService.getUserData()
            PurchasingService.getProductData(setOf(BuildConfig.PREMIUM_SKU))
        }
    }
    override fun purchase() {
        if (!state.value.available) { mutableState.update { it.copy(message = "Purchasing is blocked until Amazon configuration and server verification are complete.") }; return }
        mutableState.update { it.copy(busy = true, message = "Opening Amazon checkout…") }
        PurchasingService.purchase(BuildConfig.PREMIUM_SKU)
    }
    override fun restore() {
        if (!verificationConfigured) { mutableState.update { it.copy(message = "Restore requires a configured Amazon verification service.") }; return }
        mutableState.update { it.copy(busy = true) }
        PurchasingService.getUserData()
        PurchasingService.getPurchaseUpdates(true)
    }
    override fun onUserDataResponse(response: UserDataResponse) {
        if (response.requestStatus != UserDataResponse.RequestStatus.SUCCESSFUL) { fail("Amazon account unavailable."); return }
        userId = response.userData.userId
        if (preferences.getString("user", "") != userId) {
            preferences.edit().clear().apply()
            mutableState.update { it.copy(premium = false) }
        }
    }
    override fun onProductDataResponse(response: ProductDataResponse) {
        val product = response.productData?.get(BuildConfig.PREMIUM_SKU)
        mutableState.update { it.copy(price = product?.price, available = product != null && verificationConfigured, message = if (product == null) "Premium product unavailable in this marketplace." else "Receipt verification configuration pending.") }
    }
    override fun onPurchaseResponse(response: PurchaseResponse) {
        if (response.requestStatus != PurchaseResponse.RequestStatus.SUCCESSFUL) { fail("Purchase did not complete: ${response.requestStatus}"); return }
        validate(response.userData.userId, response.receipt)
    }
    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        if (response.requestStatus != PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL) { fail("Could not restore purchases."); return }
        val receipts = response.receipts.filter { it.sku == BuildConfig.PREMIUM_SKU }
        receipts.forEach { validate(response.userData.userId, it) }
        if (response.hasMore()) PurchasingService.getPurchaseUpdates(false)
        else mutableState.update { it.copy(busy = false, message = if (receipts.isEmpty() && !it.premium) "No premium purchase found." else it.message) }
    }
    private fun validate(user: String, receipt: Receipt) {
        if (receipt.sku != BuildConfig.PREMIUM_SKU) return
        if (receipt.isCanceled) {
            preferences.edit().clear().apply(); mutableState.update { it.copy(premium = false, busy = false, message = "Premium entitlement was revoked.") }; return
        }
        scope.launch {
            val entitlement = runCatching { verifier.verify(user, receipt) }.getOrNull()
            val accepted = entitlement?.let { it.userId == user && it.receiptId == receipt.receiptId && it.permits(Feature.MORE_ROUTINES) && receipt.productType == ProductType.ENTITLED } == true
            if (accepted) {
                preferences.edit().putBoolean("verified", true).putString("user", user).putString("receipt", receipt.receiptId).apply()
                mutableState.update { it.copy(premium = true, busy = false, message = "Premium restored.") }
                PurchasingService.notifyFulfillment(receipt.receiptId, FulfillmentResult.FULFILLED)
            } else fail("Receipt could not be verified. Premium has not been unlocked.")
        }
    }
    private fun fail(message: String) { mutableState.update { it.copy(busy = false, message = message) } }
}
