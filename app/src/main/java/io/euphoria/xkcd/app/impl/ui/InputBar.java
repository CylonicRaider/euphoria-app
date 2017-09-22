package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.RelativeLayout;

import io.euphoria.xkcd.app.R;

import static io.euphoria.xkcd.app.impl.ui.UIUtils.COLOR_SENDER_LIGHTNESS;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.COLOR_SENDER_SATURATION;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.hslToRgbInt;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.hue;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.tintDrawable;

public class InputBar extends RelativeLayout {
    public InputBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init() {
        final EditText nickEntry = (EditText) findViewById(R.id.nick_entry);
        nickEntry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                // If entered String has padding whitespace, remove it
                if (!s.toString().equals(s.toString().trim())) {
                    s.replace(0, s.length() - 1, s.toString().trim());
                }
                // Recolor background
                Drawable roundedRect = tintDrawable(getContext(), R.drawable.rounded_rect,
                        hslToRgbInt(hue(s.toString()),
                                        COLOR_SENDER_SATURATION,
                                        COLOR_SENDER_LIGHTNESS));

                if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
                    nickEntry.setBackground(roundedRect);
                } else {
                    nickEntry.setBackgroundDrawable(roundedRect);
                }
            }
        });
        // Color nick-background with base color from res/values/colors.xml
        int color;
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            color = getResources().getColor(R.color.nick_entry, getContext().getTheme());
        } else {
            color = getResources().getColor(R.color.nick_entry);
        }
        Drawable roundedRect = tintDrawable(getContext(), R.drawable.rounded_rect, color);

        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            nickEntry.setBackground(roundedRect);
        } else {
            nickEntry.setBackgroundDrawable(roundedRect);
        }
    }
}
