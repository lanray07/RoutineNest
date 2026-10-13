package com.routinenest.app.billing

import org.junit.Assert.*
import org.junit.Test

class TestBillingTest {
    @Test fun fixturePurchaseAndRestore() {
        val adapter = TestBillingAdapter()
        assertFalse(adapter.state.value.premium)
        adapter.purchase(); assertTrue(adapter.state.value.premium)
        adapter.restore(); assertTrue(adapter.state.value.premium)
    }
}
