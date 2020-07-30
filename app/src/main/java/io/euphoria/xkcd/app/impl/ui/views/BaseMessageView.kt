package io.euphoria.xkcd.app.impl.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.RelativeLayout
import io.euphoria.xkcd.app.impl.ui.UIUtils
import io.euphoria.xkcd.app.impl.ui.data.MessageTree

/** Created by Xyzzy on 2017-10-27.  */
abstract class BaseMessageView(
    context: Context?,
    attrs: AttributeSet?
) : RelativeLayout(context, attrs) {
    private var _message: MessageTree? = null
    val message get() = _message

    private var established = false

    fun setMessage(message: MessageTree) {
        check(!established) { "Updating message of view without resetting" }
        this._message = message
        tag = message.id
        established = true
        updateDisplay()
    }

    protected abstract fun updateDisplay()
    open fun recycle() {
        _message = null
        established = false
    }

    companion object {
        private const val PADDING_PER_INDENT = 15
        fun computeIndentWidth(ctx: Context, indent: Int): Int {
            return indent * UIUtils.dpToPx(ctx, PADDING_PER_INDENT)
        }

        @SuppressLint("ResourceType")
        fun getDefaultMargins(
            context: Context,
            attrs: AttributeSet?
        ): MarginLayoutParams {
            // FIXME only resolving some of the attributes; move into custom layout manager?
            // Here be dragons
            // FIXME: move from voodoo black magic incantations to readable code (remove @SuppressLint("ResourceType") after that!!)
            val indices: IntArray = if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
                intArrayOf(
                    android.R.attr.layout_width, android.R.attr.layout_height,
                    android.R.attr.layout_marginLeft, android.R.attr.layout_marginTop,
                    android.R.attr.layout_marginRight, android.R.attr.layout_marginBottom,
                    android.R.attr.layout_marginStart, android.R.attr.layout_marginEnd
                )
            } else {
                intArrayOf(
                    android.R.attr.layout_width, android.R.attr.layout_height,
                    android.R.attr.layout_marginLeft, android.R.attr.layout_marginTop,
                    android.R.attr.layout_marginRight, android.R.attr.layout_marginBottom
                )
            }
            val values = context.obtainStyledAttributes(attrs, indices)
            val core = ViewGroup.LayoutParams(
                values.getLayoutDimension(0, LayoutParams.MATCH_PARENT),
                values.getLayoutDimension(1, LayoutParams.WRAP_CONTENT)
            )
            val ret = MarginLayoutParams(core)
            ret.setMargins(
                values.getDimensionPixelSize(2, 0), values.getDimensionPixelSize(3, 0),
                values.getDimensionPixelSize(4, 0), values.getDimensionPixelSize(5, 0)
            )
            if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
                ret.marginStart = values.getDimensionPixelSize(6, 0)
                ret.marginStart = values.getDimensionPixelSize(7, 0) // TODO: end, not start?
            }
            values.recycle()
            return ret
        }

        fun setMarginForIndent(
            context: Context,
            params: MarginLayoutParams,
            indent: Int
        ) {
            val l = computeIndentWidth(context, indent)
            params.leftMargin = l
            if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
                params.marginStart = l
            }
        }
    }
}
