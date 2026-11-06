package com.abdulhameed.jumpscaretv

import android.graphics.*
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import kotlin.math.min
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var jumpTask: Runnable? = null
    private var tone: ToneGenerator? = null
    private var jumpView: JumpscareView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tone = ToneGenerator(AudioManager.STREAM_MUSIC, 90)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 28)
            gravity = android.view.Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = "JUMPSCARE TV"
            textSize = 30f
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.WHITE)
        }

        val info = TextView(this).apply {
            text = "Choose a mode. Press Back to cancel a waiting surprise."
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, 12, 0, 24)
        }

        val press = Button(this).apply {
            text = "PRESS"
            setOnClickListener { showJumpscare() }
        }

        val surprise = Button(this).apply {
            text = "SURPRISE ME"
            setOnClickListener { scheduleJumpscare() }
        }

        root.setBackgroundColor(Color.BLACK)
        root.addView(title)
        root.addView(info)
        root.addView(press)
        root.addView(surprise)
        setContentView(root)
    }

    private fun showJumpscare() {
        cancelScheduledJump()
        if (jumpView == null) {
            jumpView = JumpscareView(this)
        }
        setContentView(jumpView)
        tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 900)
        handler.postDelayed({
            if (!isFinishing) {
                jumpView = null
                recreate()
            }
        }, 2200)
    }

    private fun scheduleJumpscare() {
        cancelScheduledJump()
        val delaySeconds = Random.nextInt(5, 21)
        jumpTask = Runnable { showJumpscare() }
        handler.postDelayed(jumpTask!!, delaySeconds * 1000L)
    }

    private fun cancelScheduledJump() {
        jumpTask?.let(handler::removeCallbacks)
        jumpTask = null
    }

    override fun onBackPressed() {
        if (jumpTask != null) {
            cancelScheduledJump()
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        cancelScheduledJump()
        tone?.release()
        tone = null
        super.onDestroy()
    }

    private class JumpscareView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(Color.BLACK)

            val size = min(width, height).toFloat()
            val cx = width / 2f
            val cy = height / 2f
            val radius = size * 0.42f

            paint.color = Color.WHITE
            canvas.drawCircle(cx, cy, radius, paint)

            paint.color = Color.BLACK
            canvas.drawOval(
                cx - radius * 0.55f, cy - radius * 0.35f,
                cx - radius * 0.12f, cy + radius * 0.18f, paint
            )
            canvas.drawOval(
                cx + radius * 0.12f, cy - radius * 0.35f,
                cx + radius * 0.55f, cy + radius * 0.18f, paint
            )

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = radius * 0.09f
            canvas.drawArc(
                cx - radius * 0.55f, cy - radius * 0.05f,
                cx + radius * 0.55f, cy + radius * 0.7f,
                15f, 150f, false, paint
            )
            paint.style = Paint.Style.FILL
        }
    }
}
