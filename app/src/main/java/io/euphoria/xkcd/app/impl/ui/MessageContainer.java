package io.euphoria.xkcd.app.impl.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import io.euphoria.xkcd.app.R;
import io.euphoria.xkcd.app.data.Message;
import io.euphoria.xkcd.app.data.SessionView;

/**
 * @author N00bySumairu
 */

@SuppressLint("ViewConstructor")
public class MessageContainer extends RelativeLayout {
    private static final String TAG = "MessageContainer";
    private static final Pattern EMOJI_RE = Pattern.compile(":[a-zA-Z!?\\-]+?:");
    private static final Map<String, Float> HUE_CACHE = new HashMap<>();
    private static final float COLOR_SENDER_SAT = 0.65f;
    private static final float COLOR_SENDER_LIGHTNESS = 0.85f;
    private static final float COLOR_AT_SAT = 0.42f;
    private static final float COLOR_AT_LIGHTNESS = 0.50f;
    private static final int PADDING_PER_INDENT = 10;

    private MessageTree message = null;
    private boolean established = false;

    public MessageContainer(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
    }

    public void setMessage(@NonNull MessageTree message) {
        if (established) {
            throw new RuntimeException("Setting message on established View which was not reset! This should not happen.");
        } else {
            this.message = message;
            setTag(message.getID());
            established = true;
            updateDisplay();
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void updateDisplay() {
        TextView nickLbl = (TextView) findViewById(R.id.nick_lbl);
        TextView contentLbl = (TextView) findViewById(R.id.content_lbl);
        this.setPadding(message.getIndent()*dpToPx(PADDING_PER_INDENT),0,0,0);
        if (message != null) {
            contentLbl.setText(message.getContent());
            SessionView sender = message.getSender();
            nickLbl.setText(sender.getName());
            // Color the background of the sender's nick
            int sdk = VERSION.SDK_INT;
            if (sdk < VERSION_CODES.LOLLIPOP) {
                Drawable roundedRect = getResources().getDrawable(R.drawable.rounded_rect);
                roundedRect = DrawableCompat.wrap(roundedRect);
                DrawableCompat.setTint(roundedRect.mutate(),
                        hslToRgbInt(hue(sender.getName()),
                                COLOR_SENDER_SAT,
                                COLOR_SENDER_LIGHTNESS));

                if (sdk < VERSION_CODES.JELLY_BEAN) {
                    nickLbl.setBackgroundDrawable(roundedRect);
                } else {
                    nickLbl.setBackground(roundedRect);
                }
            } else {
                Drawable roundedRect = getResources().getDrawable(R.drawable.rounded_rect);
                roundedRect.mutate().setTint(hslToRgbInt(hue(sender.getName()),
                        COLOR_SENDER_SAT,
                        COLOR_SENDER_LIGHTNESS));
                nickLbl.setBackground(roundedRect);
            }
        } else {
            nickLbl.setText("null");
            contentLbl.setText("null");
            // Make nick background red
            int sdk = VERSION.SDK_INT;
            if (sdk < VERSION_CODES.LOLLIPOP) {
                Drawable roundedRect = getResources().getDrawable(R.drawable.rounded_rect);
                roundedRect = DrawableCompat.wrap(roundedRect);
                DrawableCompat.setTint(roundedRect.mutate(), hslToRgbInt(0, 1, 0.5f));

                if (sdk < VERSION_CODES.JELLY_BEAN) {
                    nickLbl.setBackgroundDrawable(roundedRect);
                } else {
                    nickLbl.setBackground(roundedRect);
                }
            } else {
                Drawable roundedRect = getResources().getDrawable(R.drawable.rounded_rect);
                roundedRect.mutate().setTint(hslToRgbInt(0, 1, 0.5f));
                nickLbl.setBackground(roundedRect);
            }
            Log.e(TAG, "updateDisplay: MessageContainer message is null!", new RuntimeException("MessageContainer message is null!"));
        }
    }

    public void recycle() {
        if (!established) return;
        message = null;
        established = false;
        setOnClickListener(null);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    /*                                                                       *
     * Hue hashing code ported to java from                                  *
     * https://github.com/euphoria-io/heim/blob/master/client/lib/hueHash.js *
     *                                                                       */

    private static float hueHash(String text) {
        return hueHash(text, 0);
    }

    private static float hueHash(String text, float offset) {
        // DJBX33A-ish
        float val = 0;
        for (int i = 0; i < text.length(); i++) {
            // scramble char codes across [0-255]
            // prime multiple chosen so @greenie can green, and @redtaboo red.
            float charVal = (text.charAt(i) * 439f) % 256f;

            // multiply val by 33 while constraining within signed 32 bit int range.
            // this keeps the value within Number.MAX_SAFE_INTEGER without throwing out
            // information.
            float origVal = val;
            val = (int) val << 5;
            val += origVal;

            // add the character information to the hash.
            val += charVal;
        }

        // add the minimum possible value, to ensure that val is positive (without
        // throwing out information).
        val += Math.pow(2, 31);

        // add the calibration offset and scale within 0-254 (an arbitrary range kept
        // for consistency with prior behavior).
        return (val + offset) % 255;
    }

    private static final float greenieOffset = 148 - hueHash("greenie");

    private static float hue(String text) {
        String normalized = normalize(text);

        if (normalized.length() == 0) {
            normalized = text;
        }

        if (HUE_CACHE.containsKey(normalized)) {
            return HUE_CACHE.get(normalized);
        }

        float val = hueHash(normalized, greenieOffset);
        HUE_CACHE.put(normalized, val);
        return val;
    }

    private static String normalize(String text) {
        return EMOJI_RE.matcher(text).replaceAll("")
                .replaceAll("[^\\w_-]", "")
                .toLowerCase();
    }

    /**
     * Converts hsla color values to an integer representing the color in 0xAARRGGBB format
     * if h > 360 or h < 0: h = h%360
     * if s > 1 or s < 0: s = max(min(s,1),0)
     * if l > 1 or l < 0: l = max(min(l,1),0)
     * if a > 1 or a < 0: a = max(min(a,1),0)
     *
     * @param h
     * @param s
     * @param l
     * @return
     */
    private static int hslaToRgbaInt(float h, float s, float l, float a) {
        // ensure proper values
        h = (h % 360 + 360) % 360; // real modulus not just remainder
        s = Math.max(Math.min(s, 1f), 0f);
        l = Math.max(Math.min(l, 1f), 0f);
        a = Math.max(Math.min(a, 1f), 0f);
        // convert
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs(h / 60f % 2 - 1));
        float m = l - c / 2;
        float r = 0, g = 0, b = 0;
        if (h >= 0 && h < 60) {
            r = c;
            g = x;
            b = 0;
        } else if (h >= 60 && h < 120) {
            r = x;
            g = c;
            b = 0;
        } else if (h >= 120 && h < 180) {
            r = 0;
            g = c;
            b = x;
        } else if (h >= 180 && h < 240) {
            r = 0;
            g = x;
            b = c;
        } else if (h >= 240 && h < 300) {
            r = x;
            g = 0;
            b = c;
        } else if (h >= 300 && h < 360) {
            r = c;
            g = 0;
            b = x;
        }
        r = Math.max(Math.min((r + m) * 255, 255f), 0f);
        g = Math.max(Math.min((g + m) * 255, 255f), 0f);
        b = Math.max(Math.min((b + m) * 255, 255f), 0f);
        a = Math.max(Math.min(a*255, 255f), 0f);
        return ((int) a << 8 * 3) + ((int) r << 8 * 2) + ((int) g << 8) + (int) b;
    }

    private static int hslToRgbInt(float h, float s, float l) {
        return hslaToRgbaInt(h, s, l, 1f);
    }
}
