package com.routinenest.app.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Test fixture only; deliberately never selected by the application dependency graph. */
class TestBillingAdapter : BillingAdapter {
    private val value = MutableStateFlow(BillingState(available = true, price = "Test currency 2.99"))
    override val state: StateFlow<BillingState> = value
    override fun purchase() { value.value = value.value.copy(premium = true, message = "Development fixture purchase") }
    override fun restore() { value.value = value.value.copy(message = "Development fixture restored") }
}
