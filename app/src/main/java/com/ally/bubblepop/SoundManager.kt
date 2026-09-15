package com.ally.bubblepop

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.*
import kotlin.random.Random

class SoundManager {

    private val sampleRate = 44100

    // bubbleIndex 0-4, 0=smallest/highest, 4=biggest/deepest
    fun playPop(bubbleIndex: Int) {
        Thread {
            val configs = listOf(
                floatArrayOf(1100f, 350f, 0.13f, 0f),
                floatArrayOf(850f,  220f, 0.16f, 0f),
                floatArrayOf(620f,  160f, 0.20f, 1f),
                floatArrayOf(430f,  100f, 0.23f, 1f),
                floatArrayOf(280f,   60f, 0.28f, 1f)
            )
            val (startFreq, endFreq, duration, noisy) = configs[bubbleIndex].map { it }
            val snapIntensity = 0.4f + bubbleIndex * 0.12f
            val totalDuration = duration + 0.04f
            val numSamples = (sampleRate * totalDuration).toInt()
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val progress = (t / duration).coerceIn(0f, 1f)

                // Frequency sweep
                val freq = startFreq * (1f - progress) + endFreq * progress
                val tone = sin(2.0 * PI * freq * t).toFloat()

                // Envelope
                val env = if (t < duration) exp(-t * 5f / duration) else 0f

                // Noise layer for larger bubbles
                val noise = if (noisy == 1f && t < duration) {
                    (Random.nextFloat() * 2f - 1f) * 0.25f * env
                } else 0f

                // Snap at end
                val snapStart = duration * 0.85f
                val snap = if (t >= snapStart && t < snapStart + 0.025f) {
                    val snapProgress = (t - snapStart) / 0.025f
                    val snapEnv = (1f - snapProgress).pow(3)
                    (Random.nextFloat() * 2f - 1f) * snapEnv * snapIntensity
                } else 0f

                val sample = ((tone * 0.85f * env + noise + snap) * Short.MAX_VALUE)
                    .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                samples[i] = sample.toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep((totalDuration * 1000).toLong() + 100)
            track.release()
        }.start()
    }
}