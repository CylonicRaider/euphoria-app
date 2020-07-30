package io.euphoria.xkcd.app.impl.ui.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import io.euphoria.xkcd.app.R
import io.euphoria.xkcd.app.impl.ui.UIUtils

// TODO: Use an xml layout with data/view-binding
class MessageView(context: Context, attrs: AttributeSet?) :
    BaseMessageView(context, attrs) {
    private val defaultLayoutParams: MarginLayoutParams = getDefaultMargins(context, attrs)
    override fun updateDisplay() {
        // TODO move to ViewHolder?
        val nickLbl = findViewById<NicknameView>(R.id.nick_lbl)
        val contentLbl = findViewById<TextView>(R.id.content_lbl)
        val collapser = findViewById<View>(R.id.collapser)
        val collapserIcon = findViewById<TriangleView>(R.id.collapser_icon)
        val collapserLbl = findViewById<TextView>(R.id.collapser_lbl)
        val lp = (layoutParams as MarginLayoutParams?) ?: MarginLayoutParams(defaultLayoutParams).also { layoutParams = it }

        val mt = message!!
        val msg = mt.message
        setMarginForIndent(context, lp, mt.indent)
        if (msg != null) {
            val content = msg.content
            val emote = UIUtils.isEmote(content)
            var displayContent = if (emote) content.substring(3) else content
            displayContent = displayContent.trim { it <= ' ' }
            // Apply the nickname
            nickLbl.updateParameters(emote, msg.senderName)
            // Apply the message's text
            contentLbl.text = displayContent
            setContentBackground(
                contentLbl,
                emote,
                UIUtils.emoteColor(msg.senderName)
            )
        } else {
            val res = resources
            nickLbl.text = res.getString(R.string.not_available)
            contentLbl.text = res.getString(R.string.not_available)
            // Make nick background red
            nickLbl.updateParameters(UIUtils.hslToRgbInt(0.0, 1.0, 0.5))
            setContentBackground(contentLbl, false, -1)
            Log.e(
                TAG, "updateDisplay: MessageView message is null!",
                RuntimeException("MessageView message is null!")
            )
        }
        if (mt.replies.isEmpty()) {
            collapser.visibility = View.GONE
            return
        }
        val replies = mt.countVisibleUserReplies(true)
        if (replies == 0) {
            collapser.visibility = View.GONE
            return
        }
        collapser.visibility = View.VISIBLE
        val res = resources
        val repliesStr = res.getQuantityString(R.plurals.collapser_replies, replies)
        if (mt.isCollapsed) {
            collapserLbl.text = res.getString(R.string.collapser_show, replies, repliesStr)
            collapserIcon.pointDown = false
        } else {
            collapserLbl.text = res.getString(R.string.collapser_hide, replies, repliesStr)
            collapserIcon.pointDown = true
        }
    }

    override fun recycle() {
        super.recycle()
        setTextClickListener(null)
        setCollapserClickListener(null)
    }

    fun setTextClickListener(l: OnClickListener?) {
        UIUtils.setSelectableOnClickListener(findViewById(R.id.nick_lbl), l)
        UIUtils.setSelectableOnClickListener(findViewById(R.id.content_lbl), l)
        findViewById<View>(R.id.clicker).setOnClickListener(l)
    }

    fun setCollapserClickListener(l: OnClickListener?) {
        findViewById<View>(R.id.collapser).setOnClickListener(l)
    }

    companion object {
        private const val TAG = "MessageView"
        private fun setContentBackground(
            v: View,
            emote: Boolean,
            @ColorInt color: Int
        ) {
            if (emote) {
                UIUtils.setColoredBackground(v, R.drawable.bg_content_emote, color)
            } else {
                UIUtils.setViewBackground(v, R.drawable.bg_content)
            }
        }
    }

}
