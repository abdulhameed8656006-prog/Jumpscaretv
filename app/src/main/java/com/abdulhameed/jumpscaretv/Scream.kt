package com.abdulhameed.jumpscaretv

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.Random
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sin

// Makes a very loud, harsh scream in code, so no audio file is needed.
object Scream {
    private const val RATE = 22050
    private const val SECS = 2.6
    private var data: ShortArray? = null
    private var track: AudioTrack? = null

    fun prepare() {
        if (data != null) return
        Thread { data = build() }.start()
    }

    fun play() {
        stop()
        val buf = data ?: build().also { data = it }
        try {
            val t = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buf.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            t.write(buf, 0, buf.size)
            t.play()
            track = t
        } catch (e: Exception) {
        }
    }

    fun stop() {
        try {
            track?.stop()
            track?.release()
        } catch (e: Exception) {
        }
        track = null
    }

    private fun build(): ShortArray {
        val n = (RATE * SECS).toInt()
        val out = ShortArray(n)
        val rnd = Random(7)
        val twoPi = 2.0 * PI
        var p1 = 0.0
        var p2 = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            val env = if (t < 0.008) t / 0.008 else if (t > SECS - 0.5) max(0.0, (SECS - t) / 0.5) else 1.0
            val bend = 1.0 + 0.9 * (t / SECS)
            val f1 = (850.0 + 420.0 * sin(twoPi * 8.0 * t)) * bend
            val f2 = (1130.0 + 300.0 * sin(twoPi * 5.5 * t + 1.0)) * bend
            p1 += twoPi * f1 / RATE
            p2 += twoPi * f2 / RATE
            val saw1 = (p1 / twoPi) % 1.0 * 2.0 - 1.0
            val saw2 = (p2 / twoPi) % 1.0 * 2.0 - 1.0
            val growl = if (sin(twoPi * 37.0 * t) > 0.0) 1.0 else 0.55
            val noise = rnd.nextDouble() * 2.0 - 1.0
            val boom = if (t < 0.5) sin(twoPi * 48.0 * t) * (1.0 - t / 0.5) * 1.4 else 0.0
            var s = (saw1 * 0.6 + saw2 * 0.45 + noise * 0.5) * growl * 2.2 * env + boom
            if (s > 1.0) s = 1.0
            if (s < -1.0) s = -1.0
            s = round(s * 24.0) / 24.0
            out[i] = (s * 30000).toInt().toShort()
        }
        return out
    }
}
