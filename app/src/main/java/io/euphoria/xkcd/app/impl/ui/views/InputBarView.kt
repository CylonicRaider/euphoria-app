package io.euphoria.xkcd.app.impl.ui.views

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import io.euphoria.xkcd.app.R
import io.euphoria.xkcd.app.impl.ui.UIUtils
import io.euphoria.xkcd.app.impl.ui.views.InputBarView
import io.euphoria.xkcd.app.impl.ui.views.MessageListAdapter.InputBarDirection

class InputBarView(
    context: Context,
    attrs: AttributeSet?
) : BaseMessageView(context, attrs) {
    interface NickChangeListener {
        fun onChangeNick(view: InputBarView): Boolean
    }

    interface SubmitListener {
        fun onSubmit(view: InputBarView): Boolean
    }

    private val defaultLayoutParams: MarginLayoutParams
    var lastNick: String
        private set
    var nickEntry: EditText? = null
        private set
    var messageEntry: EditText? = null
        private set
    var nickChangeListener: NickChangeListener? = null
    var submitListener: SubmitListener? = null
    override fun onFinishInflate() {
        super.onFinishInflate()
        // FIXME: work with data/view-binding to avoid so many non-null asserts
        nickEntry = findViewById(R.id.nick_entry)!!
        messageEntry = findViewById(R.id.message_entry)!!
        // Color nick-background with unsaturated nickname color as default
        val defaultColor =
            UIUtils.hslToRgbInt(0.0, 0.0, UIUtils.COLOR_SENDER_LIGHTNESS.toDouble())
        nickEntry!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
                // NOP
            }

            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                // NOP
            }

            override fun afterTextChanged(s: Editable) {
                // Recolor background
                var color = defaultColor
                if (s.isNotEmpty()) color = UIUtils.nickColor(s.toString())
                UIUtils.setColoredBackground(nickEntry, R.drawable.bg_nick, color)
            }
        })
        UIUtils.setEnterKeyListener(
            nickEntry,
            EditorInfo.IME_ACTION_NEXT,
            Runnable { messageEntry!!.requestFocus() })
        nickEntry!!.onFocusChangeListener = OnFocusChangeListener { _, hasFocus -> if (!hasFocus) doNickChange() }
        UIUtils.setEnterKeyListener(
            messageEntry,
            EditorInfo.IME_ACTION_SEND,
            Runnable {
                if (submitListener == null || submitListener!!.onSubmit(this@InputBarView)) messageEntry!!.setText(
                    ""
                )
            })
        UIUtils.setColoredBackground(nickEntry, R.drawable.bg_nick, defaultColor)
    }

    override fun updateDisplay() {
        // TODO state restoration should go here
    }

    val nickText: String
        get() = nickEntry!!.text.toString()

    val messageText: String
        get() = messageEntry!!.text.toString()

    fun setIndent(indent: Int) {
        var lp = layoutParams as MarginLayoutParams?
        if (lp == null) {
            lp = MarginLayoutParams(defaultLayoutParams)
            layoutParams = lp
        }
        BaseMessageView.Companion.setMarginForIndent(context, lp, indent)
    }

    fun requestEntryFocus(): Boolean {
        return if (nickEntry!!.text.isEmpty()) {
            nickEntry!!.requestFocus()
        } else {
            messageEntry!!.requestFocus()
        }
    }

    fun mayNavigateInput(dir: InputBarDirection): Boolean {
        val text = messageEntry!!.text.toString()
        if (dir == InputBarDirection.ROOT || text.isEmpty()) return true
        val index = messageEntry!!.selectionStart
        val indexEnd = messageEntry!!.selectionEnd
        return index == indexEnd && (dir == InputBarDirection.UP || dir == InputBarDirection.DOWN) &&
                !text.contains("\n")
    }

    private fun doNickChange() {
        // Strip whitespace
        val nickEditor = nickEntry!!.text
        val newNick = nickEditor.toString()
        val trimmedNick = newNick.trim { it <= ' ' }
        if (newNick != trimmedNick) {
            nickEditor.replace(0, newNick.length, trimmedNick)
        }
        // Invoke listener
        if (nickChangeListener != null && !nickChangeListener!!.onChangeNick(this)) {
            nickEntry!!.setText(lastNick)
        } else {
            lastNick = nickEntry!!.text.toString()
        }
    }

    init {
        defaultLayoutParams = BaseMessageView.Companion.getDefaultMargins(context, attrs)
        lastNick = ""
    }
}
