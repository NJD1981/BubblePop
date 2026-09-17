package com.ally.bubblepop

import android.content.Context
import android.graphics.*
import android.view.View

class BubbleView(
    context: Context,
    private val bubbleColor: Int,
    private val textColor: Int,
    private val label: String,
    private val showLabel: Boolean = true
) : View(context) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shine2Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) * 0.44f

        fillPaint.shader = RadialGradient(
            cx * 0.65f, cy * 0.6f, r * 1.4f,
            intArrayOf(
                Color.argb(240,
                    (Color.red(bubbleColor) + 60).coerceAtMost(255),
                    (Color.green(bubbleColor) + 60).coerceAtMost(255),
                    (Color.blue(bubbleColor) + 60).coerceAtMost(255)),
                Color.argb(140, Color.red(bubbleColor), Color.green(bubbleColor), Color.blue(bubbleColor)),
                Color.argb(60,
                    (Color.red(bubbleColor) - 30).coerceAtLeast(0),
                    (Color.green(bubbleColor) - 30).coerceAtLeast(0),
                    (Color.blue(bubbleColor) - 30).coerceAtLeast(0))
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, fillPaint)

        rimPaint.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(Color.TRANSPARENT,
                Color.argb(100, Color.red(bubbleColor), Color.green(bubbleColor), Color.blue(bubbleColor))),
            floatArrayOf(0.72f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, rimPaint)

        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 2.5f
        borderPaint.color = Color.argb(180, 255, 255, 255)
        canvas.drawCircle(cx, cy, r, borderPaint)

        shinePaint.color = Color.argb(190, 255, 255, 255)
        canvas.save()
        canvas.rotate(-35f, cx * 0.58f, cy * 0.52f)
        canvas.drawOval(cx * 0.28f, cy * 0.22f, cx * 0.9f, cy * 0.56f, shinePaint)
        canvas.restore()

        shine2Paint.color = Color.argb(130, 255, 255, 255)
        canvas.save()
        canvas.rotate(-35f, cx * 1.3f, cy * 0.38f)
        canvas.drawOval(cx * 1.1f, cy * 0.26f, cx * 1.52f, cy * 0.46f, shine2Paint)
        canvas.restore()

        glowPaint.color = Color.argb(70, 255, 255, 255)
        canvas.drawCircle(cx * 1.25f, cy * 1.55f, w * 0.07f, glowPaint)

        if (showLabel && label.isNotEmpty()) {
            textPaint.color = Color.argb(200, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            textPaint.textSize = r * 0.75f
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.typeface = Typeface.DEFAULT_BOLD
            val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(label, cx, textY, textPaint)
        }
    }
}