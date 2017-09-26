package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;

import io.euphoria.xkcd.app.R;

import static io.euphoria.xkcd.app.impl.ui.UIUtils.COLOR_SENDER_LIGHTNESS;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.COLOR_SENDER_SATURATION;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.hslToRgbInt;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.hue;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.tintDrawable;

public class InputBarView extends RelativeLayout {
    public InputBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init() {
        final EditText nickEntry = (EditText) findViewById(R.id.nick_entry);
        // Color nick-background with unsaturated nickname color as default
        final int defaultColor = hslToRgbInt(0, 0, COLOR_SENDER_LIGHTNESS);
        nickEntry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // NOP
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // NOP
            }

            @Override
            public void afterTextChanged(Editable s) {
                // If entered String has padding whitespace, remove it
                if (!s.toString().equals(s.toString().trim())) {
                    s.replace(0, s.length() - 1, s.toString().trim());
                }
                // Recolor background
                int color = defaultColor;
                if (s.length() != 0)
                    color = hslToRgbInt(hue(s.toString()),
                                        COLOR_SENDER_SATURATION,
                            COLOR_SENDER_LIGHTNESS);
                Drawable roundedRect = tintDrawable(getContext(), R.drawable.rounded_rect, color);
                if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
                    nickEntry.setBackground(roundedRect);
                } else {
                    nickEntry.setBackgroundDrawable(roundedRect);
                }
            }
        });
        Drawable roundedRect = tintDrawable(getContext(), R.drawable.rounded_rect, defaultColor);
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            nickEntry.setBackground(roundedRect);
        } else {
            nickEntry.setBackgroundDrawable(roundedRect);
        }
    }

    public void setIndent(int indent) {
        MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
        if (lp == null) {
            // HACK: Ignoring anything defined in the XML file...
            lp = new MarginLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            setLayoutParams(lp);
        }
        int margin = MessageView.computeIndentWidth(getContext(), indent);
        lp.setMargins(margin, 0, 0, 0);
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
            lp.setMarginStart(margin);
        }
    }

}
