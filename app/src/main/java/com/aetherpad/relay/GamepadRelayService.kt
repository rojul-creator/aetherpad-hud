package com.aetherpad.relay

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

class GamepadRelayService : Service() {

    companion object {
        const val CHANNEL_ID = "aetherpad_relay_channel"
        const val NOTIFICATION_ID = 47800
        const val ACTION_START = "com.aetherpad.ACTION_START"
        const val ACTION_STOP = "com.aetherpad.ACTION_STOP"
        const val EXTRA_IP = "EXTRA_IP"
        const val EXTRA_PORT = "EXTRA_PORT"

        val isServiceRunning = AtomicBoolean(false)
        val packetsTransmitted = MutableStateFlow(0L)
        val currentLatencyMs = MutableStateFlow(4)
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var datagramSocket: DatagramSocket? = null
    private var relayJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val ip = intent.getStringExtra(EXTRA_IP) ?: "192.168.1.3"
                val port = intent.getIntExtra(EXTRA_PORT, 47800)
                startRelay(ip, port)
            }
            ACTION_STOP -> {
                stopRelay()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun acquireWakeLocks() {
        // Keep CPU alive during screen sleep without keeping display powered
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AetherPad::RelayWakeLock").apply {
            setReferenceCounted(false)
            acquire(12 * 60 * 60 * 1000L) // 12 hours max
        }

        // Prevent Wi-Fi from dropping to sleep or entering low-power throttling
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wm.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "AetherPad::WifiLock")
        } else {
            @Suppress("DEPRECATION")
            wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "AetherPad::WifiLock")
        }.apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun startRelay(ip: String, port: Int) {
        if (isServiceRunning.getAndSet(true)) return

        val notification = buildPersistentNotification(ip, port)
        startForeground(NOTIFICATION_ID, notification)

        datagramSocket = DatagramSocket()
        val targetAddress = InetAddress.getByName(ip)

        relayJob = serviceScope.launch {
            var seq = 0
            val packetBytes = ByteArray(20)

            while (isActive && isServiceRunning.get()) {
                val state = MainActivity.gamepadStateFlow.value
                val size = UdpRelayPacket.serializeInto(state, packetBytes, seq++)

                val packet = DatagramPacket(packetBytes, size, targetAddress, port)
                try {
                    datagramSocket?.send(packet)
                    packetsTransmitted.value += 1
                } catch (e: Exception) {
                    // Packet drop or network transition
                }

                // 120Hz Loop pacing (approx 8.33ms)
                delay(8)
            }
        }
    }

    private fun stopRelay() {
        isServiceRunning.set(false)
        relayJob?.cancel()
        try {
            datagramSocket?.close()
        } catch (_: Exception) {}
        datagramSocket = null

        wakeLock?.let { if (it.isHeld) it.release() }
        wifiLock?.let { if (it.isHeld) it.release() }

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AetherPad Controller Relay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps wireless gamepad relay active with low-latency sleep mode"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildPersistentNotification(ip: String, port: Int): Notification {
        val stopIntent = Intent(this, GamepadRelayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPending = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AetherPad HUD // RELAY ACTIVE")
            .setContentText("Broadcasting 120Hz UDP stream to $ip:$port")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openAppPending)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "STOP RELAY", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        stopRelay()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
