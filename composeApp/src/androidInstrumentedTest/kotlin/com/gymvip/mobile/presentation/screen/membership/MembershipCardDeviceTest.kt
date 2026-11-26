package com.gymvip.mobile.presentation.screen.membership

import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.gymvip.mobile.MainActivity
import com.gymvip.mobile.domain.model.MembershipStatus
import com.gymvip.mobile.domain.model.MembershipSummary
import com.gymvip.mobile.presentation.theme.GymVipTheme
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica la tarjeta con un contrato ficticio en pantalla, sin modificar datos del socio. */
class MembershipCardDeviceTest {
    @Test fun activeContractRendersAndCapturesOnDevice() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        ) as MainActivity
        try {
            instrumentation.runOnMainSync {
                activity.setContent {
                    GymVipTheme(themePreference = "DARK_GOLD", showGuides = false) {
                        Surface {
                            Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Mi membresía", style = MaterialTheme.typography.headlineSmall)
                                Text("Vista de prueba · contrato ficticio", style = MaterialTheme.typography.bodySmall)
                                val membership = MembershipSummary("fixture", "fixture-account", "VIP Mensual", "2026-11-26", "2026-12-25", MembershipStatus.VIGENTE, 28, "", durationDays = 30, conditions = "Pago presencial")
                                CurrentMembershipCard(membership)
                                MembershipContractDetails(membership, "EFECTIVO", onOpenPayments = {})
                            }
                        }
                    }
                }
            }
            instrumentation.waitForIdleSync()
            Thread.sleep(800)
            val screenshot = Bitmap.createBitmap(activity.window.decorView.width, activity.window.decorView.height, Bitmap.Config.ARGB_8888)
            val captured = CountDownLatch(1)
            var captureResult = -1
            PixelCopy.request(activity.window, screenshot, { result -> captureResult = result; captured.countDown() }, Handler(Looper.getMainLooper()))
            assertTrue("Se debe capturar la superficie real de la app", captured.await(5, TimeUnit.SECONDS))
            assertEquals(PixelCopy.SUCCESS, captureResult)
            val target = File(context.getExternalFilesDir(null), "membership-active-qa.png")
            target.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
            screenshot.recycle()
            assertTrue(target.length() > 1000)
        } finally {
            instrumentation.runOnMainSync { activity.recreate() }
        }
    }
}
