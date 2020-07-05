package io.euphoria.xkcd.app.impl.ui.views;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.View;

import io.euphoria.xkcd.app.R;

import static io.euphoria.xkcd.app.impl.ui.UIUtils.nickColor;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.setColoredBackground;

/** Created by Xyzzy on 2020-03-29. */

public class NicknameView extends AppCompatTextView {

    public static final int NO_COLOR_OVERRIDE = -1;

    private boolean inEmoteMode;
    @ColorInt
    private int colorOverride;

    public NicknameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        colorOverride = NO_COLOR_OVERRIDE;
        updateBackground();
    }

    public boolean isInEmoteMode() {
        return inEmoteMode;
    }

    public void setInEmoteMode(boolean em) {
        boolean oldMode = inEmoteMode;
        inEmoteMode = em;
        if (oldMode != em) updateBackground();
    }

    public int getColorOverride() {
        return colorOverride;
    }

    public void setColorOverride(@ColorInt int c) {
        int oldValue = colorOverride;
        colorOverride = c;
        if (oldValue != c) updateBackground();
    }

    public void updateParameters(@ColorInt int color) {
        setColorOverride(color);
    }

    public void updateParameters(boolean emoteMode, String text) {
        colorOverride = NO_COLOR_OVERRIDE;
        inEmoteMode = emoteMode;
        setText(text);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        updateBackground();
    }

    private void updateBackground() {
        @ColorInt int color = (colorOverride != NO_COLOR_OVERRIDE) ? colorOverride : nickColor(getText().toString());
        setNickBackground(this, inEmoteMode, color);
    }

    private static void setNickBackground(View v, boolean emote, @ColorInt int color) {
        setColoredBackground(v, emote ? R.drawable.bg_nick_emote : R.drawable.bg_nick, color);
    }

}
