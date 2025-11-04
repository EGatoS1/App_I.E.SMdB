package com.example.app_iesmdb

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class AttendancePieView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Porcentajes (0..100)
    private var presentPct: Float = 0f
    private var latePct: Float = 0f
    private var absentPct: Float = 0f

    private val paintPresent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.kpi_present_green)
        style = Paint.Style.FILL
    }

    private val paintLate = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.kpi_late_yellow)
        style = Paint.Style.FILL
    }

    private val paintAbsent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.kpi_absent_red)
        style = Paint.Style.FILL
    }

    private val paintEmpty = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE0E0E0.toInt() // gris clarito cuando no hay datos
        style = Paint.Style.FILL
    }

    private val rect = RectF()

    /**
     * Recibe porcentajes (0..100). No hace falta que sumen exactamente 100,
     * solo usamos la proporción.
     */
    fun setValues(present: Float, late: Float, absent: Float) {
        presentPct = present.coerceAtLeast(0f)
        latePct = late.coerceAtLeast(0f)
        absentPct = absent.coerceAtLeast(0f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = width.coerceAtMost(height).toFloat()
        val left = (width - size) / 2f
        val top = (height - size) / 2f
        rect.set(left, top, left + size, top + size)

        val total = presentPct + latePct + absentPct

        if (total <= 0f) {
            // Sin datos → círculo gris
            canvas.drawOval(rect, paintEmpty)
            return
        }

        // Calculamos los ángulos
        val presentAngle = 360f * (presentPct / total)
        val lateAngle = 360f * (latePct / total)
        val absentAngle = 360f * (absentPct / total)

        var startAngle = -90f // empezamos arriba

        // Asistencia
        canvas.drawArc(rect, startAngle, presentAngle, true, paintPresent)
        startAngle += presentAngle

        // Tardanza
        canvas.drawArc(rect, startAngle, lateAngle, true, paintLate)
        startAngle += lateAngle

        // Ausencia
        canvas.drawArc(rect, startAngle, absentAngle, true, paintAbsent)
    }
}
