package com.frmod

object NativeBridge {
    init { System.loadLibrary("syshelper") }
    var hasAttached = false

    @JvmStatic external fun attach(pid: Int): Boolean
    @JvmStatic external fun getModuleBase(name: String): Long
    @JvmStatic external fun setOffsets(
        localBase: Long, localPos: Long, yaw: Long, pitch: Long,
        plistBase: Long, plistCount: Long, plistSize: Long, plistPos: Long,
        plistTeam: Long, plistHp: Long
    )
    @JvmStatic external fun startLoop(): Boolean
    @JvmStatic external fun stopLoop()
    @JvmStatic external fun setAim(enabled: Boolean, fov: Float, smooth: Float, teamCheck: Boolean, headshot: Boolean)
    @JvmStatic external fun setEsp(enabled: Boolean)
    @JvmStatic external fun getPlayers(): FloatArray
    @JvmStatic external fun getLocalPos(): FloatArray
    @JvmStatic external fun getCam(): FloatArray
    @JvmStatic external fun setViewport(w: Float, h: Float, fovY: Float)
}
