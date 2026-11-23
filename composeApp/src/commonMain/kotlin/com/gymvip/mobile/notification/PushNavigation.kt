package com.gymvip.mobile.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PushDestination { INBOX, ORDERS }
data class PushTarget(val accountId: String, val itemId: String, val destination: PushDestination = PushDestination.INBOX)

/** Push is a navigation hint; inbox content and ownership come from the backend/cache. */
object PushNavigation {
    private val state = MutableStateFlow<PushTarget?>(null)
    val pending = state.asStateFlow()
    private val revision = MutableStateFlow(0L)
    val inboxRevision = revision.asStateFlow()
    fun changed() { revision.value += 1 }
    private val uuid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    fun offer(accountId: String?, itemId: String?) {
        if (accountId != null && itemId != null && uuid.matches(accountId) && uuid.matches(itemId)) {
            state.value = PushTarget(accountId, itemId)
        }
    }
    fun clear() { state.value = null }
    fun offerOrders(accountId: String) {
        if (uuid.matches(accountId)) state.value = PushTarget(accountId, "ORDERS", PushDestination.ORDERS)
    }
}
