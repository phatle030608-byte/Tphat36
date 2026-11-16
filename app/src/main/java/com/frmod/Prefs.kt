package com.frmod

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE)
        if (!sp.getBoolean("init", false)) {
            sp.edit()
                .putString("moduleName", "libil2cpp.so")
                .putString("localBase", "0")
                .putString("localPos", "0")
                .putString("yaw", "0")
                .putString("pitch", "0")
                .putString("plistBase", "0")
                .putString("plistCount", "0")
                .putString("plistSize", "0")
                .putString("plistPos", "0")
                .putString("plistTeam", "0")
                .putString("plistHp", "0")
                .putFloat("fov", 200f)
                .putFloat("smooth", 0.4f)
                .putBoolean("aimOn", true)
                .putBoolean("espOn", true)
                .putBoolean("teamCheck", true)
                .putBoolean("init", true)
                .apply()
        }
    }

    var moduleName: String get() = sp.getString("moduleName", "libil2cpp.so")!!
        set(v) = sp.edit().putString("moduleName", v).apply()

    fun getHex(k: String): Long {
        val s = sp.getString(k, "0")?.trim() ?: "0"
        return try {
            if (s.startsWith("0x")) s.substring(2).toLong(16)
            else if (s.matches(Regex("[0-9a-fA-F]+"))) s.toLong(16)
            else s.toLongOrNull() ?: 0L
        } catch (_: Exception) { 0L }
    }
    fun setHex(k: String, v: String) = sp.edit().putString(k, v).apply()

    var localBase: Long get() = getHex("localBase"); set(v) = setHex("localBase", v.toString(16))
    var localPos: Long get() = getHex("localPos"); set(v) = setHex("localPos", v.toString(16))
    var yaw: Long get() = getHex("yaw"); set(v) = setHex("yaw", v.toString(16))
    var pitch: Long get() = getHex("pitch"); set(v) = setHex("pitch", v.toString(16))
    var plistBase: Long get() = getHex("plistBase"); set(v) = setHex("plistBase", v.toString(16))
    var plistCount: Long get() = getHex("plistCount"); set(v) = setHex("plistCount", v.toString(16))
    var plistSize: Long get() = getHex("plistSize"); set(v) = setHex("plistSize", v.toString(16))
    var plistPos: Long get() = getHex("plistPos"); set(v) = setHex("plistPos", v.toString(16))
    var plistTeam: Long get() = getHex("plistTeam"); set(v) = setHex("plistTeam", v.toString(16))
    var plistHp: Long get() = getHex("plistHp"); set(v) = setHex("plistHp", v.toString(16))

    var fov: Float get() = sp.getFloat("fov", 200f); set(v) = sp.edit().putFloat("fov", v).apply()
    var smooth: Float get() = sp.getFloat("smooth", 0.4f); set(v) = sp.edit().putFloat("smooth", v).apply()
    var aimOn: Boolean get() = sp.getBoolean("aimOn", true); set(v) = sp.edit().putBoolean("aimOn", v).apply()
    var espOn: Boolean get() = sp.getBoolean("espOn", true); set(v) = sp.edit().putBoolean("espOn", v).apply()
    var teamCheck: Boolean get() = sp.getBoolean("teamCheck", true); set(v) = sp.edit().putBoolean("teamCheck", v).apply()
}
