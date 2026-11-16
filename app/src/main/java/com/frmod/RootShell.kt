package com.frmod

object RootShell {
    fun exec(cmd: String): String = try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        p.waitFor()
        p.inputStream.bufferedReader().readText()
    } catch (_: Exception) { "" }

    fun hasRoot(): Boolean {
        val out = exec("id")
        return out.contains("uid=0")
    }

    fun findPid(pkg: String): Int {
        val raw = exec("pidof $pkg").trim()
        if (raw.isNotEmpty()) return raw.split(" ").first().toIntOrNull() ?: 0
        val r = exec("ps -A | grep $pkg")
        return Regex("\\s(\\d+)\\s").find(r)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }
}
