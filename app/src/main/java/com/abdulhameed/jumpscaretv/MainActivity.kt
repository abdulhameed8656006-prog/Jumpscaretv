package com.abdulhameed.jumpscaretv

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Random

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var root: FrameLayout
    private lateinit var home: LinearLayout
    private lateinit var waiting: TextView
    private lateinit var btnNow: Button
    private lateinit var btnLater: Button
    private var scare: ScareView? = null
    private var pending: Runnable? = null
    private var state = 0 // 0 = home, 1 = waiting, 2 = scaring

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        buildUi()
        Scream.prepare()
    }

    override fun onResume() {
        super.onResume()
        btnNow.requestFocus()
    }

    override fun onPause() {
        super.onPause()
        if (state == 2) endScare()
        if (state == 1) cancelWait()
    }

    override fun onBackPressed() {
        when (state) {
            2 -> endScare()
            1 -> cancelWait()
            else -> super.onBackPressed()
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun paintButton(b: Button, focused: Boolean) {
        val g = GradientDrawable()
        g.cornerRadius = dp(18).toFloat()
        g.setColor(if (focused) 0xFFB00000.toInt() else 0xFF1B1B1B.toInt())
        g.setStroke(dp(3), if (focused) Color.WHITE else 0xFF6A0000.toInt())
        b.background = g
        b.setTextColor(if (focused) Color.WHITE else 0xFFFF5A5A.toInt())
    }

    private fun makeButton(text: String, size: Float, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = text
        b.textSize = size
        b.typeface = Typeface.DEFAULT_BOLD
        b.isAllCaps = false
        b.stateListAnimator = null
        b.isFocusable = true
        b.setPadding(dp(40), dp(18), dp(40), dp(18))
        paintButton(b, false)
        b.setOnFocusChangeListener { v, has ->
            paintButton(b, has)
            val s = if (has) 1.08f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(120).start()
        }
        b.setOnClickListener { onClick() }
        return b
    }

    private fun buildUi() {
        root = FrameLayout(this)
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFF000000.toInt(), 0xFF2A0000.toInt())
        )

        home = LinearLayout(this)
        home.orientation = LinearLayout.VERTICAL
        home.gravity = Gravity.CENTER

        val title = TextView(this)
        title.text = "JUMPSCARE"
        title.textSize = 56f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(0xFFE01818.toInt())
        title.letterSpacing = 0.18f
        title.gravity = Gravity.CENTER
        home.addView(title)

        val sub = TextView(this)
        sub.text = "Warning: sudden image and very loud sound. Turn the volume to a safe level."
        sub.textSize = 18f
        sub.setTextColor(0xFFAAAAAA.toInt())
        sub.gravity = Gravity.CENTER
        sub.setPadding(0, dp(10), 0, dp(36))
        home.addView(sub)

        btnNow = makeButton("PRESS", 34f) { beginScare(1100L, "") }
        btnLater = makeButton("SURPRISE ME LATER", 22f) {
            beginScare(5000L + Random().nextInt(15000), "Shhh... wait for it.")
        }
        val lp1 = LinearLayout.LayoutParams(dp(420), ViewGroup.LayoutParams.WRAP_CONTENT)
        val lp2 = LinearLayout.LayoutParams(dp(420), ViewGroup.LayoutParams.WRAP_CONTENT)
        lp2.topMargin = dp(18)
        home.addView(btnNow, lp1)
        home.addView(btnLater, lp2)

        root.addView(
            home,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        waiting = TextView(this)
        waiting.textSize = 22f
        waiting.setTextColor(0xFF3A3A3A.toInt())
        waiting.gravity = Gravity.CENTER
        waiting.setBackgroundColor(Color.BLACK)
        waiting.visibility = View.GONE
        root.addView(
            waiting,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        setContentView(root)
    }

    // Goes dark, waits, then fires the jumpscare.
    private fun beginScare(delayMs: Long, message: String) {
        if (state != 0) return
        state = 1
        home.visibility = View.GONE
        waiting.text = message
        waiting.visibility = View.VISIBLE
        val r = Runnable {
            waiting.visibility = View.GONE
            state = 0
            fireScare()
        }
        pending = r
        h.postDelayed(r, delayMs)
    }

    private fun fireScare() {
        if (state == 2) return
        state = 2
        home.visibility = View.GONE
        val v = ScareView(this)
        scare = v
        root.addView(
            v,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        Scream.play()
        v.start { endScare() }
    }

    private fun endScare() {
        scare?.cancel()
        scare?.let { root.removeView(it) }
        scare = null
        Scream.stop()
        state = 0
        home.visibility = View.VISIBLE
        btnNow.requestFocus()
    }

    private fun cancelWait() {
        pending?.let { h.removeCallbacks(it) }
        pending = null
        waiting.visibility = View.GONE
        home.visibility = View.VISIBLE
        state = 0
        btnNow.requestFocus()
    }
}
