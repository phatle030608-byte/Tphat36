package com.frmod

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var espView: EspView? = null
    private var floatingIcon: View? = null
    private var menuView: View? = null
    private var worker: Thread? = null
    @Volatile private var running = true

    private var isMenuOpen = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        startForeground(1, buildNotification())

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupEspOverlay()
        setupFloatingIcon()

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
                absAddr(Prefs.localBase), absAddr(Prefs.localPos),
                absAddr(Prefs.yaw), absAddr(Prefs.pitch),
                absAddr(Prefs.plistBase), absAddr(Prefs.plistCount),
                Prefs.plistSize, Prefs.plistPos,
                Prefs.plistTeam, Prefs.plistHp
            )

            val dm = resources.displayMetrics
            NativeBridge.setViewport(dm.widthPixels.toFloat(), dm.heightPixels.toFloat(), 90f)
            NativeBridge.setAim(Prefs.aimOn, Prefs.fov, Prefs.smooth, Prefs.teamCheck, Prefs.headshotOn)
            NativeBridge.startLoop()
        }.apply { start() }
    }

    private fun setupEspOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        espView = EspView(this)
        wm.addView(espView, params)
    }

    private fun setupFloatingIcon() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100

        val icon = TextView(this).apply {
            text = "T"
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FF4444"))
                shape = GradientDrawable.OVAL
            }
            width = 150
            height = 150
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        icon.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    wm.updateViewLayout(icon, params)
                    false
                }
                MotionEvent.ACTION_UP -> {
                    if (Math.abs(event.rawX - initialTouchX) < 10 && Math.abs(event.rawY - initialTouchY) < 10) {
                        toggleMenu()
                    }
                    false
                }
                else -> false
            }
        }

        floatingIcon = icon
        wm.addView(floatingIcon, params)
    }

    private fun toggleMenu() {
        if (isMenuOpen) {
            menuView?.let { wm.removeView(it) }
            isMenuOpen = false
            return
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        val scroll = ScrollView(this).apply { 
            setBackgroundColor(Color.parseColor("#E6121212"))
            setPadding(30,30,30,30)
        }
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        ll.addView(TextView(this).apply {
            text = "TPHAT36 MENU"
            textSize = 20f; setTextColor(Color.parseColor("#FF4444"))
            gravity = Gravity.CENTER; setPadding(0, 0, 0, 20)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        fun textField(label: String, get: () -> String, set: (String) -> Unit) {
            ll.addView(TextView(this).apply { text = label; textSize = 12f; setTextColor(Color.LTGRAY) })
            val e = EditText(this).apply {
                setText(get()); setTextColor(Color.WHITE); textSize = 14f
                background = GradientDrawable().apply { setColor(Color.parseColor("#2A2A2A")); cornerRadius = 15f }
                setPadding(20, 15, 20, 15)
            }
            e.setOnFocusChangeListener { _, has -> if (!has) set(e.text.toString().trim()) }
            ll.addView(e)
        }

        fun addSwitch(text: String, get: () -> Boolean, set: (Boolean) -> Unit) {
            ll.addView(Switch(this).apply {
                this.text = text; textSize = 14f; setTextColor(Color.WHITE)
                isChecked = get(); setPadding(0, 10, 0, 10)
                setOnCheckedChangeListener { _, v -> set(v) }
            })
        }

        textField("localPos", { Prefs.localPos.toString(16) }) { Prefs.localPos = it.toLongOrNull(16) ?: 0L }
        textField("yaw", { Prefs.yaw.toString(16) }) { Prefs.yaw = it.toLongOrNull(16) ?: 0L }
        textField("pitch", { Prefs.pitch.toString(16) }) { Prefs.pitch = it.toLongOrNull(16) ?: 0L }
        textField("list base", { Prefs.plistBase.toString(16) }) { Prefs.plistBase = it.toLongOrNull(16) ?: 0L }
        textField("pos", { Prefs.plistPos.toString(16) }) { Prefs.plistPos = it.toLongOrNull(16) ?: 0L }

        addSwitch("Bật ESP", { Prefs.espOn }, { Prefs.espOn = it; NativeBridge.setEsp(it) })
        addSwitch("Bật Aim", { Prefs.aimOn }, { Prefs.aimOn = it; NativeBridge.setAim(it, Prefs.fov, Prefs.smooth, Prefs.teamCheck, Prefs.headshotOn) })
        addSwitch("Aim Đầu (Headshot)", { Prefs.headshotOn }, { Prefs.headshotOn = it; NativeBridge.setAim(Prefs.aimOn, Prefs.fov, Prefs.smooth, Prefs.teamCheck, it) })

        ll.addView(Button(this).apply {
            text = "Đóng Menu"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply { setColor(Color.parseColor("#CC0000")); cornerRadius = 15f }
            setPadding(0, 20, 0, 20)
            setOnClickListener { toggleMenu() }
        })

        scroll.addView(ll)
        menuView = scroll
        wm.addView(menuView, params)
        isMenuOpen = true
    }

    override fun onDestroy() {
        running = false
        NativeBridge.stopLoop()
        NativeBridge.hasAttached = false
        worker?.interrupt()
        espView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        floatingIcon?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        menuView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val chId = "sysh_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(chId, "Helper", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(this, chId)
            .setContentTitle("Tphat36").setContentText("Running")
            .setSmallIcon(android.R.drawable.ic_menu_compass).setOngoing(true).build()
    }
}
