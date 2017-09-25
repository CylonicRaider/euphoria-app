package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.euphoria.xkcd.app.R;
import io.euphoria.xkcd.app.data.SessionView;

import static io.euphoria.xkcd.app.impl.ui.UIUtils.COLOR_SENDER_LIGHTNESS;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.COLOR_SENDER_SATURATION;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.dpToPx;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.hslToRgbInt;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.hue;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.tintDrawable;

public class MessageView extends RelativeLayout {

    private static final String TAG = "MessageView";

    private static final int PADDING_PER_INDENT = 15;

    private MessageTree message = null;
    private boolean established = false;

    public MessageView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
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
        this.setPadding(computeIndentWidth(getContext(), message.getIndent()), 0, 0, 0);
        if (message != null && message.getMessage() != null) {
            contentLbl.setText(message.getMessage().getContent());
            SessionView sender = message.getMessage().getSender();
            nickLbl.setText(sender.getName());
            // Color the background of the sender's nick
            Drawable roundedRect = tintDrawable(getContext(), R.drawable.rounded_rect,
                    hslToRgbInt(hue(sender.getName()),
                            COLOR_SENDER_SATURATION,
                            COLOR_SENDER_LIGHTNESS));
            if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
                nickLbl.setBackground(roundedRect);
            } else {
                nickLbl.setBackgroundDrawable(roundedRect);
            }
        } else {
            nickLbl.setText("N/A");
            contentLbl.setText("N/A");
            // Make nick background red
            Drawable roundedRect = tintDrawable(getContext(), R.drawable.rounded_rect,
                    hslToRgbInt(0, 1, 0.5f));
            if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
                nickLbl.setBackground(roundedRect);
            } else {
                nickLbl.setBackgroundDrawable(roundedRect);
            }
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
        setSelectableOnClickListener(findViewById(R.id.nick_lbl), l);
        setSelectableOnClickListener(findViewById(R.id.content_lbl), l);
    }
    public void setCollapserClickListener(OnClickListener l) {
        findViewById(R.id.collapse_lbl).setOnClickListener(l);
    }

    public static int computeIndentWidth(Context ctx, int indent) {
        return indent * dpToPx(ctx, PADDING_PER_INDENT);
    }

    public static void setSelectableOnClickListener(final View v, final OnClickListener l) {
        if (l == null) {
            v.setOnTouchListener(null);
            return;
        }
        final GestureDetector.OnGestureListener g = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                l.onClick(v);
                return true;
            }
        };
        final GestureDetectorCompat d = new GestureDetectorCompat(v.getContext(), g);
        v.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                d.onTouchEvent(event);
                return false;
            }
        });
    }

}
