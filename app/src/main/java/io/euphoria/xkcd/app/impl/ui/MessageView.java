package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import io.euphoria.xkcd.app.R;
import io.euphoria.xkcd.app.impl.ui.detail.TriangleView;

import static io.euphoria.xkcd.app.impl.ui.UIUtils.emoteColor;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.hslToRgbInt;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.isEmote;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.setColoredBackground;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.setViewBackground;

public class MessageView extends BaseMessageView {

    private static final String TAG = "MessageView";

    private final MarginLayoutParams defaultLayoutParams;

    public MessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        defaultLayoutParams = getDefaultMargins(context, attrs);
    }

    @Override
    protected void updateDisplay() {
        // TODO move to ViewHolder?
        NicknameView nickLbl = findViewById(R.id.nick_lbl);
        TextView contentLbl = findViewById(R.id.content_lbl);
        View collapser = findViewById(R.id.collapser);
        TriangleView collapserIcon = findViewById(R.id.collapser_icon);
        TextView collapserLbl = findViewById(R.id.collapser_lbl);
        MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
        if (lp == null) {
            lp = new MarginLayoutParams(defaultLayoutParams);
            setLayoutParams(lp);
        }
        MessageTree mt = getMessage();
        UIMessage msg = mt.getMessage();
        setMarginForIndent(getContext(), lp, mt.getIndent());
        if (msg != null) {
            String content = msg.getContent();
            boolean emote = isEmote(content);
            String displayContent = emote ? content.substring(3) : content;
            displayContent = displayContent.trim();
            // Apply the nickname
            nickLbl.updateParameters(emote, msg.getSenderName());
            // Apply the message's text
            contentLbl.setText(displayContent);
            setContentBackground(contentLbl, emote, emoteColor(msg.getSenderName()));
        } else {
            Resources res = getResources();
            nickLbl.setText(res.getString(R.string.not_available));
            contentLbl.setText(res.getString(R.string.not_available));
            // Make nick background red
            nickLbl.updateParameters(hslToRgbInt(0, 1, 0.5f));
            setContentBackground(contentLbl, false, -1);
            Log.e(TAG, "updateDisplay: MessageView message is null!",
                    new RuntimeException("MessageView message is null!"));
        }
        if (mt.getReplies().isEmpty()) {
            collapser.setVisibility(GONE);
            return;
        }
        int replies = mt.countVisibleUserReplies(true);
        if (replies == 0) {
            collapser.setVisibility(GONE);
            return;
        }
        collapser.setVisibility(VISIBLE);
        Resources res = getResources();
        String repliesStr = res.getQuantityString(R.plurals.collapser_replies, replies);
        if (mt.isCollapsed()) {
            collapserLbl.setText(res.getString(R.string.collapser_show, replies, repliesStr));
            collapserIcon.setPointDown(false);
        } else {
            collapserLbl.setText(res.getString(R.string.collapser_hide, replies, repliesStr));
            collapserIcon.setPointDown(true);
        }
    }

    public void recycle() {
        super.recycle();
        setTextClickListener(null);
        setCollapserClickListener(null);
    }

    public void setTextClickListener(OnClickListener l) {
        UIUtils.setSelectableOnClickListener(findViewById(R.id.nick_lbl), l);
        UIUtils.setSelectableOnClickListener(findViewById(R.id.content_lbl), l);
        findViewById(R.id.clicker).setOnClickListener(l);
    }
    public void setCollapserClickListener(OnClickListener l) {
        findViewById(R.id.collapser).setOnClickListener(l);
    }

    private static void setContentBackground(View v, boolean emote, @ColorInt int color) {
        if (emote) {
            setColoredBackground(v, R.drawable.bg_content_emote, color);
        } else {
            setViewBackground(v, R.drawable.bg_content);
        }
    }

}
