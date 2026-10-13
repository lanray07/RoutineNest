package com.routinenest.app

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.routinenest.app.billing.EncryptedBillingSession
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class BillingSessionTest {
    @Test fun encryptedSessionPersistsAndRejectsTampering() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".debug"))
        val name = "billing_session_instrumentation"
        val store = EncryptedBillingSession(context, name)
        store.clear()
        try {
            store.write(JSONObject().put("refresh_token", "synthetic-test-token"))
            val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            assertFalse(preferences.getString("value", "")!!.contains("synthetic-test-token"))
            assertEquals("synthetic-test-token", EncryptedBillingSession(context, name).read()!!.getString("refresh_token"))
            preferences.edit().putString("value", "AAAA").commit()
            assertNull(store.read())
            assertFalse(preferences.contains("value"))
        } finally { store.clear() }
    }
}
