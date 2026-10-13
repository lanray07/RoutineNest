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
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap

data class BillingState(val premium: Boolean = false, val price: String? = null, val available: Boolean = false, val busy: Boolean = false, val message: String = "Amazon product and receipt verification are not configured.")
interface BillingAdapter { val state: StateFlow<BillingState>; fun purchase(); fun restore() }
interface ReceiptVerifier { suspend fun verify(user: String, receipt: Receipt): Entitlement? }

@Singleton
class AmazonBilling @Inject constructor(@ApplicationContext private val context: Context) : BillingAdapter, PurchasingListener {
    private val cache = EncryptedBillingSession(context, "verified_entitlement_v2")
    private val verifier: ReceiptVerifier = SupabaseReceiptVerifier(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(BillingState())
    override val state: StateFlow<BillingState> = mutableState.asStateFlow()
    @Volatile private var userId = ""
    @Volatile private var restoring = false
    private val accountRevision = AtomicLong()
    private val receiptRevisions = ConcurrentHashMap<String, AtomicLong>()
    private val reconciliation = Any()
    // Purchasing stays fail-closed until a server verifier is supplied and tested.
    private val verificationConfigured = false
    init {
        scope.launch { while (isActive) { delay(60_000); if (state.value.premium && cachedEntitlement()?.permits(Feature.MORE_ROUTINES) != true) mutableState.update { it.copy(premium = false, message = "Premium verification expired. Use Restore when online.") } } }
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
        restoring = true // Wait for the authoritative account callback before requesting receipts.
        PurchasingService.getUserData()
    }
    override fun onUserDataResponse(response: UserDataResponse) {
        if (response.requestStatus != UserDataResponse.RequestStatus.SUCCESSFUL) { fail("Amazon account unavailable."); return }
        synchronized(reconciliation) {
        val nextUser = response.userData.userId
        if (userId != nextUser) { accountRevision.incrementAndGet(); userId = nextUser }
        val cached = cachedEntitlement()
        if (cached != null && cached.userId != userId) cache.clear()
        mutableState.update { it.copy(premium = verificationConfigured && cached?.let { value -> value.userId == userId && value.permits(Feature.MORE_ROUTINES) } == true) }
        if (restoring) {
            restoring = false
            PurchasingService.getPurchaseUpdates(true)
        }
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
        if (response.userData.userId != userId) { fail("Amazon account changed. Please retry Restore."); return }
        val receipts = response.receipts.filter { it.sku == BuildConfig.PREMIUM_SKU }
        receipts.forEach { validate(response.userData.userId, it) }
        if (response.hasMore()) PurchasingService.getPurchaseUpdates(false)
        else mutableState.update { it.copy(busy = false, message = if (receipts.isEmpty() && !it.premium) "No premium purchase found." else it.message) }
    }
    private fun validate(user: String, receipt: Receipt) {
        if (receipt.sku != BuildConfig.PREMIUM_SKU || user != userId) return
        val account = accountRevision.get()
        val revision = receiptRevisions.computeIfAbsent(receipt.receiptId) { AtomicLong() }.incrementAndGet()
        if (receipt.isCanceled) {
            revoke(user, receipt.receiptId); return
        }
        scope.launch {
            val entitlement = runCatching { verifier.verify(user, receipt) }.getOrNull()
            synchronized(reconciliation) {
            if (account != accountRevision.get() || user != userId || revision != receiptRevisions[receipt.receiptId]?.get()) return@launch
            if (entitlement?.userId == user && entitlement.receiptId == receipt.receiptId && entitlement.revoked) { revoke(user, receipt.receiptId); return@launch }
            val accepted = entitlement?.let { it.userId == user && it.receiptId == receipt.receiptId && it.permits(Feature.MORE_ROUTINES) && receipt.productType == ProductType.ENTITLED } == true
            if (accepted) {
                try { cache.write(JSONObject().put("user", user).put("receipt", receipt.receiptId).put("expiresAt", entitlement!!.expiresAt)) }
                catch (_: Exception) { fail("Could not save verified purchase. Retry Restore."); return@launch }
                mutableState.update { it.copy(premium = true, busy = false, message = "Premium restored.") }
                PurchasingService.notifyFulfillment(receipt.receiptId, FulfillmentResult.FULFILLED)
            } else fail("Receipt could not be verified. Premium has not been unlocked.")
            }
        }
    }
    private fun cachedEntitlement(): Entitlement? = cache.read()?.let { Entitlement(it.optString("user"), it.optString("receipt"), true, expiresAt = it.optLong("expiresAt", 0)) }
    private fun revoke(user: String, receipt: String) = synchronized(reconciliation) {
        val current = cachedEntitlement()
        if (current == null || (current.userId == user && current.receiptId == receipt)) {
            cache.clear(); mutableState.update { it.copy(premium = false, busy = false, message = "Premium entitlement was revoked.") }
        }
    }
    private fun fail(message: String) { mutableState.update { it.copy(busy = false, message = message) } }
}
