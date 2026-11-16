package com.frmod

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)

        if (!RootShell.hasRoot()) {
            val tv = TextView(this).apply {
                text = "Root required.\nGrant su in Magisk, then reopen."
                textSize = 18f
                setPadding(40, 80, 40, 40)
            }
            setContentView(tv)
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        }

        buildUI()
    }

    private fun buildUI() {
        val scroll = ScrollView(this)
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        // DÒNG NÀY ĐÂY: Tên của Tphat36 nè
        ll.addView(TextView(this).apply {
            text = "=== TPHAT36 MOD MENU ==="
            textSize = 22f
            setTextColor(android.graphics.Color.parseColor("#FF0000")) // Màu đỏ cho ngầu
            setPadding(0, 0, 0, 30)
        })

        fun textField(label: String, get: () -> String, set: (String) -> Unit) {
            ll.addView(TextView(this).apply { text = label; textSize = 13f; setPadding(0, 20, 0, 4) })
            val e = EditText(this).apply {
                setText(get())
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                textSize = 14f
            }
            e.setOnFocusChangeListener { _, has -> if (!has) set(e.text.toString().trim()) }
            ll.addView(e)
        }

        fun floatField(label: String, get: () -> Float, set: (Float) -> Unit) {
            ll.addView(TextView(this).apply { text = label; textSize = 13f; setPadding(0, 20, 0, 4) })
            val e = EditText(this).apply {
                setText(get().toString())
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                textSize = 14f
            }
            e.setOnFocusChangeListener { _, has ->
                if (!has) set(e.text.toString().toFloatOrNull() ?: 0f)
            }
            ll.addView(e)
        }

        ll.addView(TextView(this).apply {
            text = "--- Memory Offsets (hex) ---"
            textSize = 15f; setPadding(0, 10, 0, 10)
        })

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

        ll.addView(TextView(this).apply {
            text = "--- Aim Settings ---"
            textSize = 15f; setPadding(0, 20, 0, 10)
        })

        floatField("FOV pixels", { Prefs.fov }, { Prefs.fov = it })
        floatField("Smooth", { Prefs.smooth }, { Prefs.smooth = it })

        ll.addView(CheckBox(this).apply {
            text = "Aim on"; isChecked = Prefs.aimOn
            setOnCheckedChangeListener { _, v -> Prefs.aimOn = v }
        })
        ll.addView(CheckBox(this).apply {
            text = "ESP on"; isChecked = Prefs.espOn
            setOnCheckedChangeListener { _, v -> Prefs.espOn = v }
        })
        ll.addView(CheckBox(this).apply {
            text = "Team check"; isChecked = Prefs.teamCheck
            setOnCheckedChangeListener { _, v -> Prefs.teamCheck = v }
        })

        ll.addView(Button(this).apply {
            text = "START HACK"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                } else {
                    startForegroundService(Intent(this@MainActivity, OverlayService::class.java))
                    Toast.makeText(this@MainActivity, "Bật thành công! Vào FF thôi.", Toast.LENGTH_SHORT).show()
                }
            }
        })
        
        ll.addView(Button(this).apply {
            text = "STOP"
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
                Toast.makeText(this@MainActivity, "Đã tắt.", Toast.LENGTH_SHORT).show()
            }
        })

        scroll.addView(ll)
        setContentView(scroll)
    }
}
