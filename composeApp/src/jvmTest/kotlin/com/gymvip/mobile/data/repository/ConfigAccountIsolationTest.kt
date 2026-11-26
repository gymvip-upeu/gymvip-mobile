package com.gymvip.mobile.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.gymvip.mobile.data.local.GymVipDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigAccountIsolationTest {
    @Test fun agreedThemesPersistWithoutLeakingBetweenAccounts() = runTest {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GymVipDatabase.Schema.create(driver)
        try {
            val db=GymVipDatabase(driver)
            val repo=ConfigRepositoryImpl(db)
            repo.setActiveAccount("member-a")
            repo.setThemePreference("BLUE_DARK")
            repo.setActiveAccount("member-b")
            assertEquals("SYSTEM",repo.getThemePreference().first())
            repo.setThemePreference("LIGHT")
            val reopened=ConfigRepositoryImpl(db)
            reopened.setActiveAccount("member-a")
            assertEquals("BLUE_DARK",reopened.getThemePreference().first())
            reopened.setActiveAccount("member-b")
            assertEquals("LIGHT",reopened.getThemePreference().first())
        } finally { driver.close() }
    }
    @Test fun deviceOnboardingRemainsSharedAndBiometricsRemainPerAccount() = runTest {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GymVipDatabase.Schema.create(driver)
        try {
            val repo=ConfigRepositoryImpl(GymVipDatabase(driver))
            repo.setActiveAccount("member-a")
            repo.setBooleanPreference("onboarding_completed",true)
            repo.setBooleanPreference("biometric_lock_enabled",true)
            repo.setActiveAccount("member-b")
            assertTrue(repo.getBooleanPreference("onboarding_completed",false))
            assertEquals(false,repo.getBooleanPreference("biometric_lock_enabled",false))
        } finally { driver.close() }
    }
}
