package com.gymvip.mobile.presentation.viewmodel

import com.gymvip.mobile.domain.model.*
import com.gymvip.mobile.domain.repository.InboxRepository
import com.gymvip.mobile.domain.usecase.GetInboxUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class InboxPushViewModelTest {
    private fun item(account: String = "mine") = InboxItem("notice",account,"Recordatorio","Contenido del servidor",InboxCategory.RECORDATORIO,"2026-11-23T12:00:00Z",false)
    private class Repository(initial: List<InboxItem> = emptyList()) : InboxRepository {
        val items = MutableStateFlow(initial)
        var response = Result.success(initial)
        var reads = 0
        override fun observeInbox(accountId: String) = items
        override suspend fun refreshInbox(accountId: String): Result<List<InboxItem>> = response.onSuccess { items.value = it }
        override suspend fun markAsRead(accountId: String, itemId: String): Result<Unit> { reads++; return Result.success(Unit) }
        override suspend fun clearInbox(accountId: String) { items.value = emptyList() }
    }
    @Test fun pushOpensCorrectNoticeWithoutMarkingItRead() = runTest {
        val repository = Repository(listOf(item()))
        val vm = InboxViewModel(GetInboxUseCase(repository),"mine",backgroundScope)
        runCurrent(); vm.openFromPush("notice"); runCurrent()
        assertEquals("notice",vm.uiState.value.openedItemId)
        assertEquals(0,repository.reads)
        vm.markAsRead("notice"); runCurrent(); assertEquals(1,repository.reads)
    }
    @Test fun anotherOwnersContentIsNotOpenedEvenIfRemoteOrCacheReturnsIt() = runTest {
        val repository = Repository(listOf(item("other")))
        val vm = InboxViewModel(GetInboxUseCase(repository),"mine",backgroundScope)
        runCurrent(); vm.openFromPush("notice"); runCurrent()
        assertTrue(vm.uiState.value.items.isEmpty())
        assertNull(vm.uiState.value.openedItemId)
        assertNotNull(vm.uiState.value.pushErrorMessage)
        assertEquals(0,repository.reads)
    }
    @Test fun offlineCanOpenOwnedCacheAndReadFailureDoesNotBecomeSuccess() = runTest {
        val repository = Repository(listOf(item()))
        val vm = InboxViewModel(GetInboxUseCase(repository),"mine",backgroundScope)
        runCurrent(); repository.response = Result.failure(IllegalStateException("offline"))
        vm.openFromPush("notice"); runCurrent()
        assertEquals("notice",vm.uiState.value.openedItemId)
        assertTrue(vm.uiState.value.loadFailed)
        assertFalse(vm.uiState.value.items.single().isRead)
        assertEquals(0,repository.reads)
    }
    @Test fun offlineWithoutCacheRetainsTargetAndRetryOpensItAfterReconnect() = runTest {
        val repository = Repository()
        val vm = InboxViewModel(GetInboxUseCase(repository),"mine",backgroundScope)
        runCurrent(); repository.response = Result.failure(IllegalStateException("offline"))
        vm.openFromPush("notice"); runCurrent()
        assertNull(vm.uiState.value.openedItemId); assertNotNull(vm.uiState.value.pushErrorMessage)
        repository.response = Result.success(listOf(item()))
        vm.refresh(); runCurrent()
        assertEquals("notice",vm.uiState.value.openedItemId)
        assertNull(vm.uiState.value.pushErrorMessage)
        vm.dismissPush(); vm.refresh(); runCurrent()
        assertNull(vm.uiState.value.openedItemId)
    }
}
