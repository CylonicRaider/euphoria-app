package io.euphoria.xkcd.app.impl.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import io.euphoria.xkcd.app.R
import io.euphoria.xkcd.app.impl.ui.UIUtils

/** Created by Xyzzy on 2020-03-29.  */
class NicknameView(
    context: Context?,
    attrs: AttributeSet?
) : AppCompatTextView(context, attrs) {
    private var inEmoteMode = false

    @ColorInt
    private var colorOverride: Int
    fun isInEmoteMode(): Boolean {
        return inEmoteMode
    }

    fun setInEmoteMode(em: Boolean) {
        val oldMode = inEmoteMode
        inEmoteMode = em
        if (oldMode != em) updateBackground()
    }

    fun getColorOverride(): Int {
        return colorOverride
    }

    fun setColorOverride(@ColorInt c: Int) {
        val oldValue = colorOverride
        colorOverride = c
        if (oldValue != c) updateBackground()
    }

    fun updateParameters(@ColorInt color: Int) {
        setColorOverride(color)
    }

    fun updateParameters(emoteMode: Boolean, text: String?) {
        colorOverride = NO_COLOR_OVERRIDE
        inEmoteMode = emoteMode
        setText(text)
    }

    override fun onTextChanged(
        text: CharSequence?,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        updateBackground()
    }

    private fun updateBackground() {
        @ColorInt val color =
            if (colorOverride != NO_COLOR_OVERRIDE) colorOverride else UIUtils.nickColor(
                text.toString()
            )
        setNickBackground(this, inEmoteMode, color)
    }

    companion object {
        const val NO_COLOR_OVERRIDE = -1
        private fun setNickBackground(
            v: View,
            emote: Boolean,
            @ColorInt color: Int
        ) {
            UIUtils.setColoredBackground(
                v,
                if (emote) R.drawable.bg_nick_emote else R.drawable.bg_nick,
                color
            )
        }
    }

    init {
        colorOverride = NO_COLOR_OVERRIDE
        updateBackground()
    }
}
