package com.ally.bubblepop

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.random.Random

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private val handler = Handler(Looper.getMainLooper())

    private val unlockSequence = listOf("TL", "TR", "BR", "BL")
    private val currentSequence = mutableListOf<String>()

    private val bubbleColors = listOf(
        intArrayOf(0xFFFFB3C6.toInt(), 0xFF8B2244.toInt()),
        intArrayOf(0xFFB3D9FF.toInt(), 0xFF1A4A7A.toInt()),
        intArrayOf(0xFFB3FFD1.toInt(), 0xFF1A6B3A.toInt()),
        intArrayOf(0xFFFFE9B3.toInt(), 0xFF7A5200.toInt()),
        intArrayOf(0xFFE0B3FF.toInt(), 0xFF5A1A8A.toInt())
    )

    private val numbers = listOf("One", "Two", "Three", "Four", "Five")
    private var bubblesPopped = 0
    private val activeBubbles = mutableListOf<View>()
    private val soundManager = SoundManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tts = TextToSpeech(this, this)
        enableKioskMode()
        disableBackButton()
        setupCornerZones()

        handler.postDelayed({ spawnBubbles() }, 600)
    }

    private fun enableKioskMode() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
    }

    private fun disableBackButton() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableKioskMode()
    }

    private fun setupCornerZones() {
        val corners = mapOf(
            R.id.cornerTL to "TL",
            R.id.cornerTR to "TR",
            R.id.cornerBL to "BL",
            R.id.cornerBR to "BR"
        )
        corners.forEach { (id, corner) ->
            findViewById<View>(id)?.setOnClickListener { onCornerTouch(corner) }
        }
    }

    private fun onCornerTouch(corner: String) {
        if (currentSequence.size < unlockSequence.size &&
            unlockSequence[currentSequence.size] == corner) {
            currentSequence.add(corner)
            if (currentSequence.size == unlockSequence.size) {
                currentSequence.clear()
            }
        } else {
            currentSequence.clear()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.UK
            tts.setSpeechRate(0.85f)
            tts.setPitch(1.3f)
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun spawnBubbles() {
        val root = findViewById<FrameLayout>(R.id.gameLayout)
        val sw = root.width.toFloat()
        val sh = root.height.toFloat()

        for (i in 0 until 5) {
            handler.postDelayed({
                val size = (sw * 0.22f).toInt().coerceIn(110, 190)
                val bubble = BubbleView(this, bubbleColors[i][0], bubbleColors[i][1], (i + 1).toString())
                val lp = FrameLayout.LayoutParams(size, size)
                lp.leftMargin = (Random.nextFloat() * (sw - size - 40) + 20).toInt()
                lp.topMargin = sh.toInt()
                root.addView(bubble, lp)
                activeBubbles.add(bubble)

                val duration = 9000 + Random.nextLong(3000)
                val rise = ObjectAnimator.ofFloat(bubble, "translationY", 0f, -(sh + size))
                rise.duration = duration
                rise.interpolator = android.view.animation.LinearInterpolator()
                rise.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        root.removeView(bubble)
                        activeBubbles.remove(bubble)
                    }
                })

                val wobble = ValueAnimator.ofFloat(1f, 1.06f, 0.95f, 1.03f, 1f)
                wobble.duration = 1800
                wobble.repeatCount = ValueAnimator.INFINITE
                wobble.addUpdateListener {
                    val v = it.animatedValue as Float
                    bubble.scaleX = v
                    bubble.scaleY = 2f - v
                }

                AnimatorSet().apply {
                    playTogether(rise, wobble)
                    start()
                }

                bubble.setOnClickListener { onBubblePop(bubble, i) }

            }, i * 1400L)
        }
    }

    private fun onBubblePop(bubble: View, index: Int) {
        if (bubble.tag == "popped") return
        bubble.tag = "popped"
        bubble.setOnClickListener(null)

        speak(numbers[index])
        soundManager.playPop(index)
        bubblesPopped++

        val sx = ObjectAnimator.ofFloat(bubble, "scaleX", 1f, 1.9f, 0f)
        val sy = ObjectAnimator.ofFloat(bubble, "scaleY", 1f, 1.9f, 0f)
        val fa = ObjectAnimator.ofFloat(bubble, "alpha", 1f, 0f)
        sx.duration = 320; sy.duration = 320; fa.duration = 320

        AnimatorSet().apply {
            playTogether(sx, sy, fa)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    (bubble.parent as? FrameLayout)?.removeView(bubble)
                    activeBubbles.remove(bubble)
                    if (bubblesPopped >= 5) {
                        bubblesPopped = 0
                        handler.postDelayed({ spawnBubbles() }, 1000)
                    }
                }
            })
            start()
        }
    }

    override fun onDestroy() {
        tts.shutdown()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}