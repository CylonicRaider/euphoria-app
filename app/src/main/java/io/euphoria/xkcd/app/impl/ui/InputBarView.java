package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
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
import static io.euphoria.xkcd.app.impl.ui.UIUtils.hslToRgbInt;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.nickColor;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.setRoundedRectBackground;

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
                // Strip whitespace
                if (!s.toString().equals(s.toString().trim())) {
                    s.replace(0, s.length(), s.toString().trim());
                }
                // Recolor background
                int color = defaultColor;
                if (s.length() != 0)
                    color = nickColor(s.toString());
                setRoundedRectBackground(nickEntry, color);
            }
        });
        setRoundedRectBackground(nickEntry, defaultColor);
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
