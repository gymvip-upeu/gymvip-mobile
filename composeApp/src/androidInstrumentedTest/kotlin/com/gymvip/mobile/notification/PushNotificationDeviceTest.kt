package com.gymvip.mobile.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import com.gymvip.mobile.di.AppContainer
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** Local Android integration tests. They do not assert real FCM transport or use Firebase credentials. */
class PushNotificationDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val account = "00000000-0000-0000-0000-000000000001"
    private val item = "00000000-0000-0000-0000-000000000002"
    private var oldToken: String? = null
    private var oldRefresh: String? = null
    private var oldAccount: String? = null
    private var oldRoles: List<String> = emptyList()
    private var oldPreferences: Map<String, *> = emptyMap<String, String>()
    private var oldSeen: Map<String, *> = emptyMap<String, Boolean>()
    private lateinit var tag: String
    @Before fun prepare() = runBlocking {
        val store = AppContainer.secureTokenStore
        oldToken=store.getAccessToken(); oldRefresh=store.getRefreshToken(); oldAccount=store.getAccountId(); oldRoles=store.getAccountRoles()
        oldPreferences=context.getSharedPreferences("push_registration",Context.MODE_PRIVATE).all.toMap()
        oldSeen=context.getSharedPreferences("push_seen",Context.MODE_PRIVATE).all.toMap()
        // A deliberately unsigned fixture, never sent as an authenticated backend request.
        val payload=Base64.encodeToString("""{"jti":"android-test-only","exp":4102444800}""".toByteArray(),Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val fixture="fixture.$payload.invalid-signature"
        store.saveTokens(fixture,null,account); store.saveAccountRoles(listOf("CLIENTE"))
        tag=PushDeviceRuntime.sessionTag(fixture)!!
        context.getSharedPreferences("push_registration",Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("push_seen",Context.MODE_PRIVATE).edit().clear().commit()
        PushDeviceRuntime.save("android-local-only",account,tag)
        AppContainer.database.gymVipDatabaseQueries.insertOrReplaceInboxItem(item,account,"Aviso QA","Caché local de prueba","COMUNICADO","2026-11-23T12:00:00Z",0L)
        context.getSystemService(NotificationManager::class.java).cancelAll()
    }
    private fun data(accountId: String = account, session: String = tag) = mapOf(
        "type" to "GYMVIP_INBOX","accountId" to accountId,"itemId" to item,"sessionTag" to session)
    @After fun restore() = runBlocking {
        context.getSystemService(NotificationManager::class.java).cancelAll()
        val editor=context.getSharedPreferences("push_registration",Context.MODE_PRIVATE).edit().clear()
        oldPreferences.forEach { (key,value) -> if (value is String) editor.putString(key,value) }
        editor.commit()
        val seen=context.getSharedPreferences("push_seen",Context.MODE_PRIVATE).edit().clear()
        oldSeen.forEach { (key,value) -> if (value is Boolean) seen.putBoolean(key,value) }
        seen.commit()
        AppContainer.database.gymVipDatabaseQueries.deleteInboxByAccountId(account)
        val store=AppContainer.secureTokenStore
        if (oldToken != null && oldAccount != null) { store.saveTokens(oldToken!!,oldRefresh,oldAccount!!); store.saveAccountRoles(oldRoles) }
        else store.clear()
        PushNavigation.clear()
    }
    @Test fun deniedPermissionDoesNotShowNotificationOrEraseInbox() = runBlocking {
        val before=AppContainer.database.gymVipDatabaseQueries.selectInboxByAccountId(account).executeAsList()
        assertEquals(PackageManager.PERMISSION_DENIED,context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS))
        assertFalse(GymVipFirebaseMessagingService.display(context,data()))
        assertEquals(0,context.getSystemService(NotificationManager::class.java).activeNotifications.size)
        assertEquals(before,AppContainer.database.gymVipDatabaseQueries.selectInboxByAccountId(account).executeAsList())
    }
    @Test fun repeatedPushCreatesOnePrivateNotificationWithItsOwnDestination() = runBlocking {
        assertTrue(GymVipFirebaseMessagingService.display(context,data()))
        assertFalse(GymVipFirebaseMessagingService.display(context,data()))
        val notifications=context.getSystemService(NotificationManager::class.java).activeNotifications
        assertEquals(1,notifications.size)
        assertEquals(android.app.Notification.VISIBILITY_PRIVATE,notifications.single().notification.visibility)
        assertEquals("VIP CENTER FIT",notifications.single().notification.extras.getString(android.app.Notification.EXTRA_TITLE))
        assertEquals(0,AppContainer.database.gymVipDatabaseQueries.selectInboxByAccountId(account).executeAsList().count { it.isRead==1L })
    }
    @Test fun wrongOwnerAndOldSessionNeverDisplay() = runBlocking {
        assertFalse(GymVipFirebaseMessagingService.display(context,data(accountId="00000000-0000-0000-0000-000000000099")))
        assertFalse(GymVipFirebaseMessagingService.display(context,data(session="old-session")))
        assertEquals(0,context.getSystemService(NotificationManager::class.java).activeNotifications.size)
    }
    @Test fun localLogoutBlockDropsInFlightNotificationAndClearsNavigation() = runBlocking {
        PushNavigation.offer(account,item)
        assertTrue(GymVipFirebaseMessagingService.display(context,data()))
        PushDeviceRuntime.clear(tag)
        assertNull(PushNavigation.pending.value)
        assertFalse(GymVipFirebaseMessagingService.display(context,data()))
        assertEquals(0,context.getSystemService(NotificationManager::class.java).activeNotifications.size)
    }
}
