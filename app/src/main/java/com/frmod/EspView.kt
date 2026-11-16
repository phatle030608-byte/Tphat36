package com.frmod

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.*

class EspView(ctx: Context) : View(ctx) {
    private val paintBox = Paint().apply {
        style = Paint.Style.STROKE; strokeWidth = 3f
        isAntiAlias = true
    }
    private val paintHp = Paint().apply { style = Paint.Style.FILL }
    private val paintTxt = Paint().apply {
        color = Color.WHITE; textSize = 28f; isAntiAlias = true
        typeface = Typeface.MONOSPACE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!NativeBridge.hasAttached) { invalidate(); return }

        val players = NativeBridge.getPlayers()
        val local = NativeBridge.getLocalPos()
        val cam = NativeBridge.getCam()
        if (players.isEmpty() || local.size < 3 || cam.size < 2) { invalidate(); return }

        val pxPerDeg = width / 90f

        var i = 0
        while (i + 5 < players.size) {
            val px = players[i]; val py = players[i + 1]; val pz = players[i + 2]
            val hp = players[i + 3]; val dist = players[i + 5]
            i += 6
            if (hp <= 0f || dist < 0.5f || dist > 400f) continue

            val dx = px - local[0]; val dy = py - local[1]; val dz = pz - local[2]
            val yawTo = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()
            val pitchTo = Math.toDegrees(atan2(dy.toDouble(), sqrt((dx * dx + dz * dz).toDouble()))).toFloat()

            var dYaw = yawTo - cam[0]
            while (dYaw > 180) dYaw -= 360f
            while (dYaw < -180) dYaw += 360f
            val dPitch = pitchTo - cam[1]

            val sx = width / 2f + dYaw * pxPerDeg
            val sy = height / 2f - dPitch * pxPerDeg

            if (sx < -200f || sx > width + 200f || sy < -200f || sy > height + 200f) continue

            val boxH = (15000f / dist.coerceAtLeast(1f)).coerceIn(40f, 400f)
            val boxW = boxH * 0.5f

            paintBox.color = when {
                dist < 30f -> Color.RED
                dist < 80f -> Color.YELLOW
                else -> Color.GREEN
            }
            canvas.drawRect(sx - boxW / 2, sy - boxH, sx + boxW / 2, sy, paintBox)

            paintHp.color = Color.GREEN
            canvas.drawRect(sx - boxW / 2, sy - 6f, sx - boxW / 2 + boxW * (hp.coerceIn(0f, 100f) / 100f), sy, paintHp)

            canvas.drawText("${dist.toInt()}m", sx + boxW / 2 + 6f, sy - boxH, paintTxt)
        }

        invalidate()
    }
}
