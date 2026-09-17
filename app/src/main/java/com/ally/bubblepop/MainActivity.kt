package com.ally.bubblepop

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.random.Random

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var root: FrameLayout
    private val handler = Handler(Looper.getMainLooper())

    enum class RoundType { PLAIN, COLOUR, NUMBER }
    private var currentRound = RoundType.PLAIN
    private var cycleCount = 0
    private val maxCycles = 3
    private var roundSpeed = 1.0f
    private var bubblesPopped = 0

    private var unlockStep = 0
    private lateinit var unlockDot: View
    private val unlockPositions = listOf(
        Gravity.TOP or Gravity.START,
        Gravity.TOP or Gravity.END,
        Gravity.BOTTOM or Gravity.END,
        Gravity.BOTTOM or Gravity.START
    )
    private var lastTapTime = 0L
    private val tapTimeout = 2000L

    private val bubbleColors = listOf(
        intArrayOf(0xFFFFB3C6.toInt(), 0xFF8B2244.toInt()),
        intArrayOf(0xFFB3D9FF.toInt(), 0xFF1A4A7A.toInt()),
        intArrayOf(0xFFB3FFD1.toInt(), 0xFF1A6B3A.toInt()),
        intArrayOf(0xFFFFE9B3.toInt(), 0xFF7A5200.toInt()),
        intArrayOf(0xFFE0B3FF.toInt(), 0xFF5A1A8A.toInt())
    )
    private val colorNames = listOf("Pink", "Blue", "Green", "Yellow", "Purple")
    private val numbers = listOf("One", "Two", "Three", "Four", "Five")
    private val bubbleSizes = listOf(100, 130, 110, 160, 120)

    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        root = FrameLayout(this)
        setContentView(root)

        val sky = SkyView(this)
        root.addView(sky, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        tts = TextToSpeech(this, this)
        soundManager = SoundManager()

        enableKioskMode()
        disableBackButton()
        setupUnlockDot()

        handler.postDelayed({ spawnBubbles() }, 800)
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

    override fun onResume() {
        super.onResume()
        enableKioskMode()
        try { startLockTask() } catch (e: Exception) { }
    }

    private fun setupUnlockDot() {
        unlockDot = View(this)
        unlockDot.setBackgroundColor(Color.argb(60, 255, 255, 255))
        val size = dpToPx(18)
        val params = FrameLayout.LayoutParams(size, size)
        params.gravity = unlockPositions[0]
        params.setMargins(12, 12, 12, 12)
        root.addView(unlockDot, params)

        unlockDot.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastTapTime > tapTimeout) unlockStep = 0
            lastTapTime = now
            unlockStep++

            if (unlockStep >= unlockPositions.size) {
                unlockStep = 0
                showRestartScreen(fromUnlock = true)
            } else {
                val lp = unlockDot.layoutParams as FrameLayout.LayoutParams
                lp.gravity = unlockPositions[unlockStep]
                unlockDot.layoutParams = lp
            }
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.UK
            tts.setSpeechRate(0.82f)
            tts.setPitch(1.2f)
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun spawnBubbles() {
        val sw = root.width.toFloat()
        val sh = root.height.toFloat()
        bubblesPopped = 0

        for (i in 0 until 5) {
            handler.postDelayed({
                val size = dpToPx(bubbleSizes[i])
                val colorIdx = i % bubbleColors.size

                val label = when (currentRound) {
                    RoundType.NUMBER -> (i + 1).toString()
                    RoundType.COLOUR -> colorNames[colorIdx][0].toString()
                    RoundType.PLAIN -> ""
                }

                val bubble = BubbleView(
                    this,
                    bubbleColors[colorIdx][0],
                    bubbleColors[colorIdx][1],
                    label,
                    currentRound != RoundType.PLAIN
                )

                val lp = FrameLayout.LayoutParams(size, size)
                lp.leftMargin = (Random.nextFloat() * (sw - size - 40) + 20).toInt()
                lp.topMargin = sh.toInt()
                root.addView(bubble, lp)

                val baseDuration = 9000 + Random.nextLong(3000)
                val duration = (baseDuration / roundSpeed).toLong()

                val rise = ObjectAnimator.ofFloat(bubble, "translationY", 0f, -(sh + size))
                rise.duration = duration
                rise.interpolator = android.view.animation.LinearInterpolator()
                rise.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (root.indexOfChild(bubble) >= 0) root.removeView(bubble)
                    }
                })

                val wobbleX = ValueAnimator.ofFloat(1f, 1.05f, 0.96f, 1.04f, 1f)
                wobbleX.duration = 1600
                wobbleX.repeatCount = ValueAnimator.INFINITE
                wobbleX.addUpdateListener {
                    val v = it.animatedValue as Float
                    bubble.scaleX = v
                    bubble.scaleY = 1f + (1f - v) * 0.5f
                }

                AnimatorSet().apply {
                    playTogether(rise, wobbleX)
                    start()
                }

                bubble.setOnClickListener { onBubblePop(bubble, i) }

            }, (i * 1600L / roundSpeed).toLong())
        }
    }

    private fun onBubblePop(bubble: View, index: Int) {
        if (bubble.tag == "popped") return
        bubble.tag = "popped"
        bubble.setOnClickListener(null)
        bubblesPopped++

        soundManager.playPop(index)

        handler.postDelayed({
            when (currentRound) {
                RoundType.NUMBER -> speak(numbers[index])
                RoundType.COLOUR -> speak(colorNames[index % colorNames.size])
                RoundType.PLAIN -> { }
            }
        }, 320L)

        val sx = ObjectAnimator.ofFloat(bubble, "scaleX", 1f, 1.9f, 0f)
        val sy = ObjectAnimator.ofFloat(bubble, "scaleY", 1f, 1.9f, 0f)
        val fa = ObjectAnimator.ofFloat(bubble, "alpha", 1f, 0f)
        sx.duration = 300; sy.duration = 300; fa.duration = 300

        AnimatorSet().apply {
            playTogether(sx, sy, fa)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (root.indexOfChild(bubble) >= 0) root.removeView(bubble)
                    if (bubblesPopped >= 5) {
                        handler.postDelayed({ nextRound() }, 800)
                    }
                }
            })
            start()
        }
    }

    private fun nextRound() {
        currentRound = when (currentRound) {
            RoundType.PLAIN -> RoundType.COLOUR
            RoundType.COLOUR -> RoundType.NUMBER
            RoundType.NUMBER -> {
                cycleCount++
                if (cycleCount >= maxCycles) {
                    showRestartScreen(fromUnlock = false)
                    return
                }
                roundSpeed += 0.15f
                RoundType.PLAIN
            }
        }
        spawnBubbles()
    }

    private fun showRestartScreen(fromUnlock: Boolean) {
        val overlay = FrameLayout(this)
        overlay.setBackgroundColor(Color.argb(220, 135, 206, 235))

        val msg = TextView(this)
        msg.text = if (fromUnlock) "Unlocked! 🔓" else "Great job! 🎉\nTap to play again!"
        msg.textSize = 42f
        msg.setTextColor(Color.WHITE)
        msg.gravity = Gravity.CENTER
        msg.typeface = android.graphics.Typeface.DEFAULT_BOLD

        overlay.addView(msg, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).also { it.gravity = Gravity.CENTER })

        root.addView(overlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        overlay.setOnClickListener {
            root.removeView(overlay)
            if (fromUnlock) {
                try { stopLockTask() } catch (e: Exception) { }
                finish()
            } else {
                cycleCount = 0
                roundSpeed = 1.0f
                currentRound = RoundType.PLAIN
                spawnBubbles()
            }
        }
    }

    override fun onDestroy() {
        tts.shutdown()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}