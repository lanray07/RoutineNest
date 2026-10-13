package com.routinenest.app.billing

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.amazon.device.iap.model.Receipt
import com.routinenest.app.BuildConfig
import com.routinenest.core.Entitlement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Sessions are created only during purchase verification, never during local routine use. */
class SupabaseReceiptVerifier(context: Context) : ReceiptVerifier {
    private val sessionStore = EncryptedBillingSession(context)
    private val lock = Mutex()
    private val base = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val key = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    override suspend fun verify(user: String, receipt: Receipt): Entitlement? = withContext(Dispatchers.IO) {
        lock.withLock {
            if (base.isBlank() || key.isBlank() || user.isBlank() || receipt.receiptId.isBlank()) return@withLock null
            val session = session()
            val payload = JSONObject().put("amazonUserId", user).put("receiptId", receipt.receiptId)
            var response = post("/functions/v1/routinenest-verify-receipt", payload, session.getString("access_token"))
            if (response.first == 401) {
                sessionStore.clear()
                val refreshed = session()
                response = post("/functions/v1/routinenest-verify-receipt", payload, refreshed.getString("access_token"))
                return@withLock accepted(response, refreshed.getJSONObject("user").getString("id"), user, receipt.receiptId)
            }
            accepted(response, session.getJSONObject("user").getString("id"), user, receipt.receiptId)
        }
    }

    private fun accepted(response: Pair<Int, JSONObject>, subject: String, user: String, receiptId: String): Entitlement? {
        if (response.first != 200) return null
        val result = response.second
        if (result.optString("subject") != subject || result.optString("amazonUserId") != user ||
            result.optString("receiptId") != receiptId || result.optString("sku") != BuildConfig.PREMIUM_SKU) return null
        if (result.optString("decision") == "revoked") return Entitlement(user, receiptId, false, revoked = true)
        val now = System.currentTimeMillis()
        val expiry = result.optLong("expiresAt", 0)
        val checked = result.optLong("verifiedAt", 0)
        if (result.optString("decision") != "verified" || checked <= 0 || checked > now + 60_000 || expiry <= now || expiry - checked > 6 * 60 * 60 * 1000) return null
        return Entitlement(user, receiptId, true, expiresAt = expiry)
    }

    private fun session(): JSONObject {
        val cached = sessionStore.read()
        if (cached != null && cached.optLong("expires_at", 0) * 1000 > System.currentTimeMillis() + 60_000) return cached
        val response = if (cached?.optString("refresh_token")?.isNotBlank() == true) {
            post("/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", cached.getString("refresh_token")))
        } else {
            // Requires anonymous sign-ins to be explicitly enabled after reviewing the shared project's RLS.
            post("/auth/v1/signup", JSONObject().put("data", JSONObject()))
        }
        if (response.first !in 200..299) {
            if (response.first in listOf(400, 401, 403)) sessionStore.clear()
            error("Billing authentication unavailable. Please try Restore later.")
        }
        val value = response.second
        require(value.optString("access_token").isNotBlank() && value.optString("refresh_token").isNotBlank() && value.optJSONObject("user")?.optString("id")?.isNotBlank() == true) { "Invalid billing session." }
        if (!value.has("expires_at")) value.put("expires_at", System.currentTimeMillis() / 1000 + value.getLong("expires_in"))
        sessionStore.write(value)
        return value
    }

    private fun post(path: String, body: JSONObject, token: String? = null): Pair<Int, JSONObject> {
        val url = URL(base + path)
        require(url.protocol == "https" && url.host.endsWith(".supabase.co")) { "Billing requires a Supabase HTTPS endpoint." }
        val connection = url.openConnection() as javax.net.ssl.HttpsURLConnection
        connection.connectTimeout = 15_000; connection.readTimeout = 20_000
        connection.instanceFollowRedirects = false
        connection.requestMethod = "POST"; connection.doOutput = true
        connection.setRequestProperty("apikey", key)
        connection.setRequestProperty("Content-Type", "application/json")
        if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val bytes = stream?.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) { val count = input.read(buffer); if (count < 0) break; require(output.size() + count <= 65536) { "Billing response exceeded limit." }; output.write(buffer, 0, count) }
                output.toByteArray()
            } ?: byteArrayOf()
            val json = runCatching { JSONObject(String(bytes, Charsets.UTF_8)) }.getOrElse { JSONObject() }
            return status to json
        } finally { connection.disconnect() }
    }
}

/** AES-GCM protects refresh tokens with a non-exportable Android Keystore key. */
class EncryptedBillingSession(context: Context, name: String = "billing_session") {
    private val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val alias = "routinenest.$name"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun write(value: JSONObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val bytes = cipher.doFinal(value.toString().toByteArray(Charsets.UTF_8))
        check(preferences.edit().putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP)).putString("value", Base64.encodeToString(bytes, Base64.NO_WRAP)).commit()) { "Could not save billing session." }
    }
    fun read(): JSONObject? = runCatching {
        val encrypted = preferences.getString("value", null) ?: return null
        val iv = preferences.getString("iv", null) ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))) }
        JSONObject(String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), Charsets.UTF_8))
    }.getOrElse { clear(); null }
    fun clear() { preferences.edit().clear().commit() }
}
