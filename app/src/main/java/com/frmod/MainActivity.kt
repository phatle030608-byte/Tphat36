package com.frmod

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)

        if (!RootShell.hasRoot()) {
            setContentView(TextView(this).apply {
                text = "Cần quyền Root!\nHãy cấp quyền su trong Magisk rồi mở lại app."
                textSize = 18f; setTextColor(Color.RED); gravity = Gravity.CENTER
            })
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }

        buildUI()
    }

    private fun buildUI() {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.parseColor("#121212")) }
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        // --- TITLE ---
        ll.addView(TextView(this).apply {
            text = "TPHAT36 MENU"
            textSize = 26f
            setTextColor(Color.parseColor("#FF4444"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        // Hàm tạo nền cho EditText và Button
        fun createBg(color: String, radius: Float = 20f): GradientDrawable {
            return GradientDrawable().apply {
                setColor(Color.parseColor(color))
                cornerRadius = radius
            }
        }

        fun textField(label: String, get: () -> String, set: (String) -> Unit) {
            ll.addView(TextView(this).apply { text = label; textSize = 14f; setTextColor(Color.LTGRAY); setPadding(0, 20, 0, 10) })
            val e = EditText(this).apply {
                setText(get())
                setTextColor(Color.WHITE)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                textSize = 16f
                background = createBg("#2A2A2A")
                setPadding(30, 30, 30, 30)
            }
            e.setOnFocusChangeListener { _, has -> if (!has) set(e.text.toString().trim()) }
            ll.addView(e)
        }

        fun floatField(label: String, get: () -> Float, set: (Float) -> Unit) {
            ll.addView(TextView(this).apply { text = label; textSize = 14f; setTextColor(Color.LTGRAY); setPadding(0, 20, 0, 10) })
            val e = EditText(this).apply {
                setText(get().toString())
                setTextColor(Color.WHITE)
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                textSize = 16f
                background = createBg("#2A2A2A")
                setPadding(30, 30, 30, 30)
            }
            e.setOnFocusChangeListener { _, has -> if (!has) set(e.text.toString().toFloatOrNull() ?: 0f) }
            ll.addView(e)
        }

        // --- AUTO FILL BUTTON ---
        ll.addView(Button(this).apply {
            text = "⚡ AUTO FILL OFFSET MẪU ⚡"
            setTextColor(Color.WHITE)
            textSize = 16f
            background = createBg("#0088FF")
            setPadding(0, 30, 0, 30)
            setOnClickListener {
                Prefs.localPos = 0xA83F54
                Prefs.yaw = 0x9C2A10
                Prefs.pitch = 0x9C2A14
                Prefs.plistBase = 0xA84000
                Prefs.plistCount = 0xA84004
                Prefs.plistSize = 0x100
                Prefs.plistPos = 0x40
                Prefs.plistTeam = 0x68
                Prefs.plistHp = 0x70
                buildUI() // Load lại giao diện
                Toast.makeText(this@MainActivity, "Đã điền Offset mẫu!", Toast.LENGTH_SHORT).show()
            }
        })

        ll.addView(TextView(this).apply { text = "--- OFFSETS (HEX) ---"; textSize = 15f; setTextColor(Color.GRAY); setPadding(0, 30, 0, 10) })

        textField("Module name", { Prefs.moduleName }, { Prefs.moduleName = it })
        textField("localPos offset", { Prefs.localPos.toString(16) }) { Prefs.localPos = it.toLongOrNull(16) ?: 0L }
        textField("cam yaw offset", { Prefs.yaw.toString(16) }) { Prefs.yaw = it.toLongOrNull(16) ?: 0L }
        textField("cam pitch offset", { Prefs.pitch.toString(16) }) { Prefs.pitch = it.toLongOrNull(16) ?: 0L }
        textField("player list base", { Prefs.plistBase.toString(16) }) { Prefs.plistBase = it.toLongOrNull(16) ?: 0L }
        textField("player count", { Prefs.plistCount.toString(16) }) { Prefs.plistCount = it.toLongOrNull(16) ?: 0L }
        textField("player stride", { Prefs.plistSize.toString(16) }) { Prefs.plistSize = it.toLongOrNull(16) ?: 0L }
        textField("player pos", { Prefs.plistPos.toString(16) }) { Prefs.plistPos = it.toLongOrNull(16) ?: 0L }
        textField("player team", { Prefs.plistTeam.toString(16) }) { Prefs.plistTeam = it.toLongOrNull(16) ?: 0L }
        textField("player hp", { Prefs.plistHp.toString(16) }) { Prefs.plistHp = it.toLongOrNull(16) ?: 0L }

        ll.addView(TextView(this).apply { text = "--- CÀI ĐẶT AIM ---"; textSize = 15f; setTextColor(Color.GRAY); setPadding(0, 30, 0, 10) })

        floatField("FOV pixels", { Prefs.fov }, { Prefs.fov = it })
        floatField("Smooth (0.1 - 1.0)", { Prefs.smooth }, { Prefs.smooth = it })

        // Hàm tạo Switch (Công tắc) đẹp hơn CheckBox
        fun addSwitch(text: String, get: () -> Boolean, set: (Boolean) -> Unit) {
            val sw = Switch(this).apply {
                this.text = text
                textSize = 16f
                setTextColor(Color.WHITE)
                isChecked = get()
                setPadding(0, 20, 0, 20)
                setOnCheckedChangeListener { _, v -> set(v) }
            }
            ll.addView(sw)
        }

        addSwitch("Bật Tự Động Ngắm (Aim)", { Prefs.aimOn }, { Prefs.aimOn = it })
        addSwitch("Bật Khung ESP", { Prefs.espOn }, { Prefs.espOn = it })
        addSwitch("Bỏ qua đồng đội (Team Check)", { Prefs.teamCheck }, { Prefs.teamCheck = it })

        ll.addView(Space(this).apply { minimumHeight = 60 })

        // --- START BUTTON ---
        ll.addView(Button(this).apply {
            text = "▶ BẬT HACK (START)"
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = createBg("#00CC44") // Xanh lá
            setPadding(0, 40, 0, 40)
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                } else {
                    startForegroundService(Intent(this@MainActivity, OverlayService::class.java))
                    Toast.makeText(this@MainActivity, "Đã bật Hack. Vào FF Max thôi!", Toast.LENGTH_SHORT).show()
                }
            }
        })

        ll.addView(Space(this).apply { minimumHeight = 20 })

        // --- STOP BUTTON ---
        ll.addView(Button(this).apply {
            text = "⏹ TẮT HACK (STOP)"
            setTextColor(Color.WHITE)
            textSize = 18f
            background = createBg("#CC0000") // Đỏ
            setPadding(0, 30, 0, 30)
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
                Toast.makeText(this@MainActivity, "Đã tắt Hack.", Toast.LENGTH_SHORT).show()
            }
        })

        scroll.addView(ll)
        setContentView(scroll)
    }
}
