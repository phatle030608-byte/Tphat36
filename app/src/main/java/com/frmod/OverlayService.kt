package com.frmod

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var espView: EspView? = null
    private var worker: Thread? = null
    @Volatile private var running = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        startForeground(1, buildNotification())

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        espView = EspView(this)
        wm.addView(espView, params)

        worker = Thread {
            var pid = 0
            while (running && pid == 0) {
                pid = RootShell.findPid("com.dts.freefireth")
                if (pid == 0) Thread.sleep(3000)
            }
            if (!running) return@Thread

            NativeBridge.hasAttached = NativeBridge.attach(pid)
            if (!NativeBridge.hasAttached) return@Thread

            val base = NativeBridge.getModuleBase(Prefs.moduleName)
            val absAddr: (Long) -> Long = { off -> if (off == 0L) 0L else base + off }

            NativeBridge.setOffsets(
                absAddr(Prefs.localBase),
                absAddr(Prefs.localPos),
                absAddr(Prefs.yaw),
                absAddr(Prefs.pitch),
                absAddr(Prefs.plistBase),
                absAddr(Prefs.plistCount),
                Prefs.plistSize,
                Prefs.plistPos,
                Prefs.plistTeam,
                Prefs.plistHp
            )

            val dm = resources.displayMetrics
            NativeBridge.setViewport(dm.widthPixels.toFloat(), dm.heightPixels.toFloat(), 90f)
            NativeBridge.setAim(Prefs.aimOn, Prefs.fov, Prefs.smooth, Prefs.teamCheck)
            NativeBridge.setEsp(Prefs.espOn)
            NativeBridge.startLoop()
        }.apply { start() }
    }

    override fun onDestroy() {
        running = false
        NativeBridge.stopLoop()
        NativeBridge.hasAttached = false
        worker?.interrupt()
        espView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val chId = "sysh_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(chId, "Helper", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(this, chId)
            .setContentTitle("Tphat36 Mod")
            .setContentText("Đang chạy...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }
}
