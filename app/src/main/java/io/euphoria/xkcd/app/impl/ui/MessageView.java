package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.euphoria.xkcd.app.R;
import io.euphoria.xkcd.app.data.SessionView;

import static io.euphoria.xkcd.app.impl.ui.UIUtils.dpToPx;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.hslToRgbInt;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.nickColor;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.setRoundedRectBackground;

public class MessageView extends RelativeLayout {

    private static final String TAG = "MessageView";

    private static final int PADDING_PER_INDENT = 15;

    private final MarginLayoutParams defaultLayoutParams;
    private MessageTree message = null;
    private boolean established = false;

    public MessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        defaultLayoutParams = getDefaultMargins(context, attrs);
    }

    public void setMessage(@NonNull MessageTree message) {
        if (established) {
            throw new IllegalStateException("Setting message of established View that has not been reset");
        } else {
            this.message = message;
            setTag(message.getID());
            established = true;
            updateDisplay();
        }
    }

    private void updateDisplay() {
        TextView nickLbl = (TextView) findViewById(R.id.nick_lbl);
        TextView contentLbl = (TextView) findViewById(R.id.content_lbl);
        TextView collapseLbl = (TextView) findViewById(R.id.collapse_lbl);
        MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
        if (lp == null) {
            lp = new MarginLayoutParams(defaultLayoutParams);
            setLayoutParams(lp);
        }
        setMarginForIndent(getContext(), lp, message.getIndent());
        if (message != null && message.getMessage() != null) {
            contentLbl.setText(message.getMessage().getContent());
            SessionView sender = message.getMessage().getSender();
            nickLbl.setText(sender.getName());
            // Color the background of the sender's nick
            setRoundedRectBackground(nickLbl, nickColor(sender.getName()));
        } else {
            nickLbl.setText("N/A");
            contentLbl.setText("N/A");
            // Make nick background red
            setRoundedRectBackground(nickLbl, hslToRgbInt(0, 1, 0.5f));
            Log.e(TAG, "updateDisplay: MessageView message is null!",
                    new RuntimeException("MessageView message is null!"));
        }
        if (message.getReplies().isEmpty()) {
            collapseLbl.setText("");
            collapseLbl.setVisibility(GONE);
        } else {
            int replies = message.countVisibleUserReplies(true);
            if (replies == 0) {
                collapseLbl.setVisibility(GONE);
            } else {
                collapseLbl.setVisibility(VISIBLE);
                String pref = message.isCollapsed() ? "Show" : "Hide";
                String suff = replies == 1 ? "reply" : "replies";
                collapseLbl.setText(pref + " " + replies + " " + suff);
            }
        }
    }

    public void recycle() {
        if (!established) return;
        message = null;
        established = false;
        setTextClickListener(null);
        setCollapserClickListener(null);
    }

    public void setTextClickListener(OnClickListener l) {
        UIUtils.setSelectableOnClickListener(findViewById(R.id.nick_lbl), l);
        UIUtils.setSelectableOnClickListener(findViewById(R.id.content_lbl), l);
    }
    public void setCollapserClickListener(OnClickListener l) {
        findViewById(R.id.collapse_lbl).setOnClickListener(l);
    }

    public static int computeIndentWidth(Context ctx, int indent) {
        return indent * dpToPx(ctx, PADDING_PER_INDENT);
    }

    @SuppressWarnings("ResourceType")
    static MarginLayoutParams getDefaultMargins(Context context, AttributeSet attrs) {
        // FIXME only resolving some of the attributes; move into custom layout manager?
        // Here be dragons
        int[] indices;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            indices = new int[] {android.R.attr.layout_width, android.R.attr.layout_height,
                    android.R.attr.layout_marginLeft, android.R.attr.layout_marginTop,
                    android.R.attr.layout_marginRight, android.R.attr.layout_marginBottom,
                    android.R.attr.layout_marginStart, android.R.attr.layout_marginEnd};
        } else {
            indices = new int[] {android.R.attr.layout_width, android.R.attr.layout_height,
                    android.R.attr.layout_marginLeft, android.R.attr.layout_marginTop,
                    android.R.attr.layout_marginRight, android.R.attr.layout_marginBottom};
        }
        TypedArray values = context.obtainStyledAttributes(attrs, indices);
        ViewGroup.LayoutParams core = new ViewGroup.LayoutParams(
                values.getLayoutDimension(0, LayoutParams.MATCH_PARENT),
                values.getLayoutDimension(1, LayoutParams.WRAP_CONTENT));
        MarginLayoutParams ret = new MarginLayoutParams(core);
        ret.setMargins(values.getDimensionPixelSize(2, 0), values.getDimensionPixelSize(3, 0),
                values.getDimensionPixelSize(4, 0), values.getDimensionPixelSize(5, 0));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            ret.setMarginStart(values.getDimensionPixelSize(6, 0));
            ret.setMarginStart(values.getDimensionPixelSize(7, 0));
        }
        values.recycle();
        return ret;
    }

    static void setMarginForIndent(Context context, MarginLayoutParams params, int indent) {
        int l = computeIndentWidth(context, indent);
        params.leftMargin = l;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            params.setMarginStart(l);
        }
    }

}
