package com.abdulhameed.jumpscaretv

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator
import java.util.Random
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// The scary face is drawn with code, so the app needs no image file.
class ScareView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()
    private val total = 2800f
    private var ms = 0f
    private var animator: ValueAnimator? = null

    // fixed random details so the face looks the same in every frame
    private val seeded = Random(4)
    private val hairX = FloatArray(34) { (seeded.nextFloat() - 0.5f) * 0.64f }
    private val veinAngle = FloatArray(14) { seeded.nextFloat() * 6.2831855f }
    private val teethTop = FloatArray(11) { 0.04f + seeded.nextFloat() * 0.06f }
    private val teethBottom = FloatArray(11) { 0.04f + seeded.nextFloat() * 0.06f }

    fun start(done: () -> Unit) {
        val a = ValueAnimator.ofFloat(0f, total)
        a.duration = total.toLong()
        a.interpolator = LinearInterpolator()
        a.addUpdateListener {
            ms = it.animatedValue as Float
            invalidate()
        }
        a.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                done()
            }
        })
        animator = a
        a.start()
    }

    fun cancel() {
        animator?.removeAllListeners()
        animator?.cancel()
    }

    private fun oval(c: Canvas, cx: Float, cy: Float, rx: Float, ry: Float) {
        rect.set(cx - rx, cy - ry, cx + rx, cy + ry)
        c.drawOval(rect, paint)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        c.drawColor(Color.BLACK)

        // very fast pop-in, then a slow creeping zoom
        val zoom = if (ms < 90f) 0.25f + 0.95f * (ms / 90f)
        else 1.2f + 0.3f * ((ms - 90f) / (total - 90f))
        val u = h * zoom

        // the face twitches to a new spot every 140 ms
        val jr = Random((ms / 140f).toInt() * 7919L)
        val jit = if (ms < 90f) 0f else 1f
        val dx = (jr.nextFloat() - 0.5f) * 2f * h * 0.012f * jit + sin(ms / 17f) * h * 0.004f
        val dy = (jr.nextFloat() - 0.5f) * 2f * h * 0.012f * jit + cos(ms / 13f) * h * 0.004f
        val rot = (jr.nextFloat() - 0.5f) * 6f * jit
        val cx = w / 2f + dx
        val cy = h / 2f + 0.02f * u + dy

        c.save()
        c.rotate(rot, w / 2f, h / 2f)

        // long stringy hair behind the head
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(2f, 0.012f * u)
        paint.color = 0xFF080808.toInt()
        for (k in hairX.indices) {
            val x0 = cx + hairX[k] * u
            path.reset()
            for (j in 0..8) {
                val t = j / 8f
                val x = x0 + sin(t * 3f + k) * 0.02f * u + (x0 - cx) * 0.35f * t
                val y = cy - 0.5f * u + t * 1.1f * u
                if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            c.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = 0xFF060606.toInt()
        oval(c, cx, cy - 0.36f * u, 0.38f * u, 0.26f * u)

        // pale, long face
        c.save()
        c.scale(0.62f, 1f, cx, cy)
        paint.shader = RadialGradient(
            cx, cy, 0.5f * u,
            intArrayOf(0xFFCDD2C0.toInt(), 0xFF7E8272.toInt(), 0xFF161A14.toInt()),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        )
        oval(c, cx, cy, 0.5f * u, 0.5f * u)
        paint.shader = null
        c.restore()

        // hollow cheeks
        paint.color = 0x6E0A0E0A.toInt()
        for (s in intArrayOf(-1, 1)) {
            oval(c, cx + s * 0.19f * u, cy + 0.11f * u, 0.07f * u, 0.11f * u)
        }

        // forehead wrinkles
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(2f, 0.004f * u)
        paint.color = 0xC8281E19.toInt()
        for (i in 0..3) {
            val y = cy - 0.40f * u + i * 0.035f * u
            rect.set(cx - 0.2f * u, y - 0.03f * u, cx + 0.2f * u, y + 0.03f * u)
            c.drawArc(rect, 200f, 140f, false, paint)
        }

        // bloodshot eyes
        for (s in intArrayOf(-1, 1)) {
            val ex = cx + s * 0.125f * u
            val ey = cy - 0.17f * u
            paint.style = Paint.Style.FILL
            paint.color = 0x96000000.toInt()
            oval(c, ex, ey, 0.15f * u, 0.12f * u)

            c.save()
            c.scale(1f, 0.75f, ex, ey)
            paint.shader = RadialGradient(
                ex, ey, 0.1f * u,
                intArrayOf(0xFFF5F5EB.toInt(), 0xFFF5F5EB.toInt(), 0xFF960A0A.toInt()),
                floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP
            )
            c.drawCircle(ex, ey, 0.1f * u, paint)
            paint.shader = null
            c.restore()

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = max(1f, 0.003f * u)
            paint.color = 0xDCA00000.toInt()
            for (v in 0 until 7) {
                val a = veinAngle[(if (s < 0) 0 else 7) + v]
                c.drawLine(
                    ex + cos(a) * 0.1f * u, ey + sin(a) * 0.075f * u,
                    ex + cos(a) * 0.045f * u, ey + sin(a) * 0.034f * u, paint
                )
            }

            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            c.drawCircle(ex, ey, 0.024f * u, paint)
            paint.color = Color.WHITE
            c.drawCircle(ex + 0.008f * u, ey - 0.008f * u, 0.006f * u, paint)

            // heavy angry brow
            paint.color = 0xFF0A0808.toInt()
            val ix = ex - s * 0.12f * u
            val ox = ex + s * 0.12f * u
            path.reset()
            path.moveTo(ix, ey - 0.088f * u)
            path.lineTo(ox, ey - 0.14f * u)
            path.lineTo(ox, ey - 0.10f * u)
            path.lineTo(ix, ey - 0.05f * u)
            path.close()
            c.drawPath(path, paint)

            // dark bags under the eyes
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.012f * u
            paint.color = 0xC8140A14.toInt()
            rect.set(ex - 0.1f * u, ey + 0.03f * u, ex + 0.1f * u, ey + 0.15f * u)
            c.drawArc(rect, 20f, 140f, false, paint)
        }

        // nose
        paint.style = Paint.Style.FILL
        paint.color = 0xFF050505.toInt()
        for (s in intArrayOf(-1, 1)) {
            oval(c, cx + s * 0.03f * u, cy + 0.0425f * u, 0.012f * u, 0.0225f * u)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(2f, 0.006f * u)
        paint.color = 0xA01E1E19.toInt()
        c.drawLine(cx - 0.015f * u, cy - 0.07f * u, cx - 0.03f * u, cy + 0.02f * u, paint)

        // wide open screaming mouth
        val mx = cx
        val my = cy + 0.30f * u
        val rx = 0.21f * u
        val ry = 0.2f * u
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            mx, my, rx,
            intArrayOf(0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF5A0000.toInt()),
            floatArrayOf(0f, 0.8f, 1f), Shader.TileMode.CLAMP
        )
        oval(c, mx, my, rx, ry)
        paint.shader = null
        paint.color = Color.BLACK
        oval(c, mx, my, rx * 0.92f, ry * 0.92f)
        paint.color = 0xFF46000A.toInt()
        oval(c, mx, my + 0.13f * u, 0.1f * u, 0.08f * u)

        // jagged teeth, top and bottom
        paint.color = 0xFFE2DEB8.toInt()
        for (row in 0..1) {
            val heights = if (row == 0) teethTop else teethBottom
            for (i in 0 until 11) {
                val t0 = i / 11f
                val t1 = (i + 1) / 11f
                val a0 = if (row == 0) 200f + 140f * t0 else 160f - 140f * t0
                val a1 = if (row == 0) 200f + 140f * t1 else 160f - 140f * t1
                val r0 = Math.toRadians(a0.toDouble())
                val r1 = Math.toRadians(a1.toDouble())
                val rm = (r0 + r1) / 2.0
                val hgt = heights[i] * u
                path.reset()
                path.moveTo(mx + rx * cos(r0).toFloat(), my + ry * sin(r0).toFloat())
                path.lineTo(mx + rx * cos(r1).toFloat(), my + ry * sin(r1).toFloat())
                path.lineTo(mx + (rx - hgt) * cos(rm).toFloat(), my + (ry - hgt) * sin(rm).toFloat())
                path.close()
                c.drawPath(path, paint)
            }
        }

        // stretched lines around the mouth
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(2f, 0.005f * u)
        paint.color = 0xFF19100C.toInt()
        for (s in intArrayOf(-1, 1)) {
            c.drawLine(mx + s * rx * 0.95f, my - 0.02f * u, mx + s * rx * 1.25f, my - 0.2f * u, paint)
            c.drawLine(mx + s * 0.07f * u, cy + 0.09f * u, mx + s * rx * 1.05f, my - 0.06f * u, paint)
        }
        c.restore()

        // old camera look: thin dark lines across the screen
        path.reset()
        var y = 0f
        while (y < h) {
            path.moveTo(0f, y)
            path.lineTo(w, y)
            y += 6f
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = 0x46000000
        c.drawPath(path, paint)

        // dark edges
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            w / 2f, h / 2f, max(w, h) * 0.75f,
            intArrayOf(0x00000000, 0xFF000000.toInt()),
            floatArrayOf(0.4f, 1f), Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // blood-red tint, with one stronger flash at the very start
        c.drawColor(Color.argb(40, 160, 0, 0))
        if (ms < 260f) {
            c.drawColor(Color.argb(((1f - ms / 260f) * 150f).toInt(), 255, 0, 0))
        }
        // fade to black at the end
        if (ms > 2500f) {
            val a = ((ms - 2500f) / 300f).coerceIn(0f, 1f)
            c.drawColor(Color.argb((a * 255f).toInt(), 0, 0, 0))
        }
    }
}
