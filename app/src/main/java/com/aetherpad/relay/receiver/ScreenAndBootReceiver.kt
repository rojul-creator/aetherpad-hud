package com.aetherpad.relay.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aetherpad.relay.GamepadRelayService
import kotlinx.coroutines.flow.MutableStateFlow

class ScreenAndBootReceiver : BroadcastReceiver() {

    companion object {
        val isScreenOn = MutableStateFlow(true)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                // Screen is turned off: switch UI to dormant state, stop rendering Canvas
                isScreenOn.value = false
            }
            Intent.ACTION_SCREEN_ON -> {
                // Screen turned on: re-enable visual rendering
                isScreenOn.value = true
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Auto-start background relay service if enabled in preferences
                val prefs = context.getSharedPreferences("aetherpad_prefs", Context.MODE_PRIVATE)
                val autoStart = prefs.getBoolean("auto_start_on_boot", false)
                if (autoStart) {
                    val serviceIntent = Intent(context, GamepadRelayService::class.java).apply {
                        action = GamepadRelayService.ACTION_START
                    }
                    context.startForegroundService(serviceIntent)
                }
            }
        }
    }
}
