package com.ally.bubblepop

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

class SkyView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sunGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val grassPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var cloudOffset1 = 0f
    private var cloudOffset2 = 0f
    private var cloudOffset3 = 0f
    private var sunGlow = 0f

    init {
        // Cloud drift animations
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 30000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { cloudOffset1 = it.animatedFraction; invalidate() }
            start()
        }
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 45000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { cloudOffset2 = it.animatedFraction }
            start()
        }
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 38000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { cloudOffset3 = it.animatedFraction }
            start()
        }
        // Sun glow pulse
        ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { sunGlow = it.animatedValue as Float }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Sky gradient
        skyPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF87CEEB.toInt(), 0xFFB8E4F7.toInt(), 0xFFE8F9FF.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, skyPaint)

        // Sun
        val sunX = w * 0.82f
        val sunY = h * 0.12f
        val sunR = w * 0.09f

        // Sun glow
        sunGlowPaint.shader = RadialGradient(
            sunX, sunY, sunR * (2.5f + sunGlow * 0.5f),
            intArrayOf(Color.argb((120 + (sunGlow * 40).toInt()), 255, 230, 100), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(sunX, sunY, sunR * (2.5f + sunGlow * 0.5f), sunGlowPaint)

        // Sun body
        sunPaint.shader = RadialGradient(
            sunX - sunR * 0.3f, sunY - sunR * 0.3f, sunR,
            intArrayOf(0xFFFFFACC.toInt(), 0xFFFFE066.toInt(), 0xFFFFB700.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(sunX, sunY, sunR, sunPaint)

        // Clouds
        drawCloud(canvas, w, (-0.3f + cloudOffset1) % 1.1f * w * 1.3f - w * 0.1f, h * 0.08f, w * 0.22f)
        drawCloud(canvas, w, (-0.1f + cloudOffset2) % 1.1f * w * 1.3f - w * 0.1f, h * 0.22f, w * 0.18f)
        drawCloud(canvas, w, (0.5f + cloudOffset3) % 1.1f * w * 1.3f - w * 0.1f, h * 0.35f, w * 0.20f)

        // Grass strip at bottom
        grassPaint.shader = LinearGradient(
            0f, h * 0.93f, 0f, h,
            intArrayOf(0xFFA8D878.toInt(), 0xFF7AB648.toInt()),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, h * 0.93f, w, h, grassPaint)
    }

    private fun drawCloud(canvas: Canvas, w: Float, x: Float, y: Float, size: Float) {
        cloudPaint.color = Color.argb(230, 255, 255, 255)
        canvas.drawCircle(x + size * 0.3f, y, size * 0.28f, cloudPaint)
        canvas.drawCircle(x + size * 0.55f, y - size * 0.1f, size * 0.35f, cloudPaint)
        canvas.drawCircle(x + size * 0.8f, y, size * 0.25f, cloudPaint)
        canvas.drawRoundRect(x, y, x + size, y + size * 0.3f, size * 0.15f, size * 0.15f, cloudPaint)
    }
}