package com.ally.bubblepop

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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
    private lateinit var soundManager: SoundManager

    enum class RoundType { PLAIN, COLOUR, NUMBER }
    private var currentRound = RoundType.PLAIN
    private var cycleCount = 0
    private val maxCycles = 3
    private var roundSpeed = 1.0f
    private var bubblesPopped = 0
    private var roundInProgress = false

    private lateinit var unlockDot: View
    private var unlockStep = 0
    private var lastTapTime = 0L
    private val tapTimeout = 2500L
    private val unlockPositions = listOf(
        Gravity.TOP or Gravity.START,
        Gravity.TOP or Gravity.END,
        Gravity.BOTTOM or Gravity.END,
        Gravity.BOTTOM or Gravity.START
    )

    private val bubbleColors = listOf(
        intArrayOf(0xFFFFB3C6.toInt(), 0xFF8B2244.toInt()),
        intArrayOf(0xFFB3D9FF.toInt(), 0xFF1A4A7A.toInt()),
        intArrayOf(0xFFB3FFD1.toInt(), 0xFF1A6B3A.toInt()),
        intArrayOf(0xFFFFE9B3.toInt(), 0xFF7A5200.toInt()),
        intArrayOf(0xFFE0B3FF.toInt(), 0xFF5A1A8A.toInt())
    )
    private val colorNames = listOf("Pink", "Blue", "Green", "Yellow", "Purple")
    private val numbers = listOf("One", "Two", "Three", "Four", "Five")
    private val bubbleSizes = listOf(100, 150, 115, 190, 135)

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
    }

    private fun setupUnlockDot() {
        unlockDot = View(this)
        unlockDot.setBackgroundColor(Color.argb(60, 255, 255, 255))
        val dotSize = dpToPx(18)
        val dotParams = FrameLayout.LayoutParams(dotSize, dotSize)
        dotParams.gravity = unlockPositions[0]
        dotParams.setMargins(12, 12, 12, 12)
        root.addView(unlockDot, dotParams)

        val cornerSteps = listOf(
            Pair(Gravity.TOP or Gravity.START, 0),
            Pair(Gravity.TOP or Gravity.END, 1),
            Pair(Gravity.BOTTOM or Gravity.END, 2),
            Pair(Gravity.BOTTOM or Gravity.START, 3)
        )

        cornerSteps.forEach { (gravity, requiredStep) ->
            val zone = View(this)
            val zoneSize = dpToPx(80)
            val zp = FrameLayout.LayoutParams(zoneSize, zoneSize)
            zp.gravity = gravity
            root.addView(zone, zp)

            zone.setOnClickListener {
                val now = System.currentTimeMillis()
                if (now - lastTapTime > tapTimeout && unlockStep > 0) {
                    unlockStep = 0
                    moveDot(0)
                    lastTapTime = now
                    return@setOnClickListener
                }
                lastTapTime = now
                if (requiredStep == unlockStep) {
                    unlockStep++
                    if (unlockStep >= 4) {
                        unlockStep = 0
                        moveDot(0)
                        showRestartScreen(fromUnlock = true)
                    } else {
                        moveDot(unlockStep)
                    }
                } else {
                    unlockStep = 0
                    moveDot(0)
                }
            }
        }
    }

    private fun moveDot(step: Int) {
        val lp = unlockDot.layoutParams as FrameLayout.LayoutParams
        lp.gravity = unlockPositions[step]
        unlockDot.layoutParams = lp
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setSpeechRate(0.85f)
            tts.setPitch(1.4f)
            val femaleVoice = tts.voices?.firstOrNull {
                it.locale == Locale.US && it.name.contains("female", ignoreCase = true)
            } ?: tts.voices?.firstOrNull { it.locale == Locale.US }
            femaleVoice?.let { tts.voice = it }
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun spawnBubbles() {
        if (roundInProgress) return
        roundInProgress = true
        bubblesPopped = 0

        val sw = root.width.toFloat()
        val sh = root.height.toFloat()

        for (i in 0 until 5) {
            handler.postDelayed({
                val sizeDp = bubbleSizes[i]
                val size = dpToPx(sizeDp)
                val colorIdx = i % bubbleColors.size

                val label = when (currentRound) {
                    RoundType.NUMBER -> (i + 1).toString()
                    else -> ""
                }

                val bubble = BubbleView(
                    this,
                    bubbleColors[colorIdx][0],
                    bubbleColors[colorIdx][1],
                    label,
                    currentRound == RoundType.NUMBER
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

                val wobble = ValueAnimator.ofFloat(1f, 1.05f, 0.96f, 1.04f, 1f)
                wobble.duration = 1600
                wobble.repeatCount = ValueAnimator.INFINITE
                wobble.addUpdateListener {
                    val v = it.animatedValue as Float
                    bubble.scaleX = v
                    bubble.scaleY = 1f + (1f - v) * 0.5f
                }

                AnimatorSet().apply {
                    playTogether(rise, wobble)
                    start()
                }

                bubble.setOnClickListener { onBubblePop(bubble, i, sizeDp) }

            }, (i * 1600L / roundSpeed).toLong())
        }
    }

    private fun onBubblePop(bubble: View, index: Int, sizeDp: Int) {
        if (bubble.tag == "popped") return
        bubble.tag = "popped"
        bubble.setOnClickListener(null)

        val sortedSizes = bubbleSizes.sorted()
        val soundIndex = sortedSizes.indexOf(sizeDp).coerceIn(0, 4)
        soundManager.playPop(soundIndex)

        handler.postDelayed({
            when (currentRound) {
                RoundType.NUMBER -> speak(numbers[index])
                RoundType.COLOUR -> speak(colorNames[index % colorNames.size])
                RoundType.PLAIN -> {}
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
                    bubblesPopped++
                    if (bubblesPopped >= 5) {
                        roundInProgress = false
                        handler.postDelayed({ nextRound() }, 1000)
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
                roundSpeed += 0.4f
                RoundType.PLAIN
            }
        }
        spawnBubbles()
    }

    private fun showRestartScreen(fromUnlock: Boolean) {
        val overlay = FrameLayout(this)
        overlay.setBackgroundColor(Color.argb(220, 135, 206, 235))
        overlay.elevation = 999f

        val msg = TextView(this)
        msg.text = if (fromUnlock) "Unlocked! 🔓\nTap to exit" else "Great job! 🎉\nTap to play again!"
        msg.textSize = 42f
        msg.setTextColor(Color.WHITE)
        msg.gravity = Gravity.CENTER
        msg.typeface = android.graphics.Typeface.DEFAULT_BOLD

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        lp.gravity = Gravity.CENTER
        overlay.addView(msg, lp)

        root.addView(overlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        overlay.setOnClickListener {
            root.removeView(overlay)
            if (fromUnlock) {
                finish()
            } else {
                cycleCount = 0
                roundSpeed = 1.0f
                currentRound = RoundType.PLAIN
                roundInProgress = false
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