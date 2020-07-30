package io.euphoria.xkcd.app.impl.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import io.euphoria.xkcd.app.R

/** Created by Xyzzy on 2018-10-15.  */
internal class TriangleView(
    context: Context?,
    attrs: AttributeSet?
) : View(context, attrs) {
    private val triangle: Path = Path()
    private val paint: Paint = Paint().apply {
        // TODO get color from attrs
        color = ContextCompat.getColor(context!!, R.color.indent_line)
        flags = Paint.ANTI_ALIAS_FLAG
    }
    private var _pointDown = false
    var pointDown
        get() = _pointDown
        set(pointDown) {
            _pointDown = pointDown
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        val w = width
        val h = height
        triangle.reset()
        triangle.moveTo(0f, 0f)
        if (_pointDown) {
            triangle.lineTo(w.toFloat(), 0f)
            triangle.lineTo(w / 2.0f, h.toFloat())
        } else {
            triangle.lineTo(0f, h.toFloat())
            triangle.lineTo(w.toFloat(), h / 2.0f)
        }
        triangle.close()
        canvas.drawPath(triangle, paint)
    }
}
