package com.gymvip.mobile.notification

import kotlin.test.*

class PushNavigationTest {
    private val account = "00000000-0000-0000-0000-000000000001"
    private val item = "00000000-0000-0000-0000-000000000002"
    @AfterTest fun clear() = PushNavigation.clear()
    @Test fun invalidDestinationCannotBecomeARoute() {
        PushNavigation.offer(account, "https://example.com")
        assertNull(PushNavigation.pending.value)
    }
    @Test fun repeatedNotificationKeepsOnePendingDestination() {
        repeat(3) { PushNavigation.offer(account, item) }
        assertEquals(PushTarget(account, item), PushNavigation.pending.value)
        PushNavigation.clear()
        assertNull(PushNavigation.pending.value)
    }
    @Test fun existingOrderPushRetainsCatalogueDestination() {
        PushNavigation.offerOrders(account)
        assertEquals(PushDestination.ORDERS,PushNavigation.pending.value?.destination)
    }
}
