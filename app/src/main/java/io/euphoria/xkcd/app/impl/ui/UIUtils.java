package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.v4.view.GestureDetectorCompat;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import io.euphoria.xkcd.app.R;

public class UIUtils {

    // Color saturation and lightness for input-bar and messages
    public static final float COLOR_SENDER_SATURATION = 0.65f;
    public static final float COLOR_SENDER_LIGHTNESS = 0.85f;
    // Color saturation and lightness for @mentions
    public static final float COLOR_AT_SATURATION = 0.42f;
    public static final float COLOR_AT_LIGHTNESS = 0.50f;
    // TODO check against actual list of valid emoji
    private static final Pattern EMOJI_RE = Pattern.compile(":[a-zA-Z!?\\-]+?:");
    // Hue hash cache
    private static final Map<String, Double> HUE_CACHE = new HashMap<>();

    /**
     * Convenience function for mapping density-independent pixels to effective pixels.
     *
     * @param ctx Context
     * @param dp Value to convert in dp
     * @return Converted value in px
     */
    public static int dpToPx(Context ctx, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                ctx.getResources().getDisplayMetrics());
    }

    /**
     * Obtain an instance of the drawable resource with the given color applied
     * @param ctx Context
     * @param drawable Resource ID of the drawable to color
     * @param color Color to apply
     * @return A mutable Drawable with the given color applied
     */
    public static Drawable colorDrawable(Context ctx, @DrawableRes int drawable, @ColorInt int color) {
        GradientDrawable ret;
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            ret = (GradientDrawable) ctx.getDrawable(drawable);
        } else {
            ret = (GradientDrawable) ctx.getResources().getDrawable(drawable);
        }
        if (ret != null) {
            ret.mutate();
            ret.setColor(color);
        }
        return ret;
    }

    /**
     * Convenience function for applying a background appropriate to Euphoria nicknames
     * @param v View whose background to set
     * @param color Color to apply (use {@link #nickColor(String)} to obtain the proper color for some nick)
     * @return The Drawable that was installed as the background
     */
    public static Drawable setRoundedRectBackground(View v, @ColorInt int color) {
        Drawable ret = colorDrawable(v.getContext(), R.drawable.rounded_rect, color);
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            v.setBackground(ret);
        } else {
            v.setBackgroundDrawable(ret);
        }
        return ret;
    }

    /*                                                                       *
     * Hue hashing code ported to Java from                                  *
     * https://github.com/euphoria-io/heim/blob/master/client/lib/hueHash.js *
     *                                                                       */

    /**
     * Raw hue hash implementation
     * Inline comments are original; block comments are porters' notes.
     */
    private static double hueHash(String text, double offset) {
        // DJBX33A-ish
        double val = 0.0;
        for (int i = 0; i < text.length(); i++) {
            // scramble char codes across [0-255]
            // prime multiple chosen so @greenie can green, and @redtaboo red.
            double charVal = (text.charAt(i) * 439.0) % 256.0;

            // multiply val by 33 while constraining within signed 32 bit int range.
            // this keeps the value within Number.MAX_SAFE_INTEGER without throwing out
            // information.
            double origVal = val;
            /* Double cast to avoid integer saturation; recall that shift is applied after the casts */
            val = (int) (long) val << 5;
            val += origVal;

            // add the character information to the hash.
            val += charVal;
        }

        // cast the result of the final character addition to a 32 bit int.
        val = (int) (long) val;

        // add the minimum possible value, to ensure that val is positive (without
        // throwing out information).
        /* Original has Math.pow(2.0, 31.0) */
        val += 1L << 31;

        // add the calibration offset and scale within 0-254 (an arbitrary range kept
        // for consistency with prior behavior).
        return (val + offset) % 255.0;
    }

    private static final double greenieOffset = 148.0 - hueHash("greenie", 0.0);

    /** Normalize a string for hue hash processing */
    private static String normalize(String text) {
        return EMOJI_RE.matcher(text).replaceAll("").replaceAll("[^\\w_\\-]", "").toLowerCase();
    }

    /**
     * Obtain the hue associated with the given nickname
     * Use the *Color methods to compose ready-to-use colors.
     * @param text The nickname whose hue to obtain
     * @return The hue corresponding to <code>text</code>
     */
    public static double hue(String text) {
        String normalized = normalize(text);

        if (normalized.length() == 0) {
            normalized = text;
        }

        Double ret = HUE_CACHE.get(normalized);
        if (ret == null) {
            ret = hueHash(normalized, greenieOffset);
            HUE_CACHE.put(normalized, ret);
        }
        return ret;
    }

    /**
     * Converts an HSLA color value to an integer representing the color in 0xAARRGGBB format
     * if h > 360 or h < 0: h = h%360
     * if s > 1 or s < 0: s = max(min(s,1),0)
     * if l > 1 or l < 0: l = max(min(l,1),0)
     * if a > 1 or a < 0: a = max(min(a,1),0)
     *
     * @param h Hue (0.0 -- 360.0)
     * @param s Saturation (0.0 -- 1.0)
     * @param l Lightness (0.0 -- 1.0)
     * @param a Opacity (0.0 -- 1.0)
     * @return RGB-ColorInt
     */
    @ColorInt
    public static int hslaToRgbaInt(double h, double s, double l, double a) {
        // ensure proper values
        h = (h % 360 + 360) % 360; // real modulus not just remainder
        s = Math.max(Math.min(s, 1f), 0f);
        l = Math.max(Math.min(l, 1f), 0f);
        a = Math.max(Math.min(a, 1f), 0f);
        // convert
        double c = (1 - Math.abs(2 * l - 1)) * s;
        double x = c * (1 - Math.abs(h / 60f % 2 - 1));
        double m = l - c / 2;
        double r = 0, g = 0, b = 0;
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
        a = Math.max(Math.min(a * 255, 255f), 0f);
        return ((int) a << 8 * 3) + ((int) r << 8 * 2) + ((int) g << 8) + (int) b;
    }

    /**
     * Convert an HSL color value to an integer representing the color in 0xFFRRGGBB format
     * @see #hslaToRgbaInt(double, double, double,double)
     */
    @ColorInt
    public static int hslToRgbInt(double h, double s, double l) {
        return hslaToRgbaInt(h, s, l, 1f);
    }

    /**
     * Obtain the color corresponding to the given nickname for use as a background
     * @param name The nickname to colorize
     * @return The color corresponding to <code>name</code>
     */
    @ColorInt
    public static int nickColor(String name) {
        return hslToRgbInt(hue(name), COLOR_SENDER_SATURATION, COLOR_SENDER_LIGHTNESS);
    }

    /**
     * Obtain the color corresponding to the given nickname for use as a text color
     * @param name The nickname to colorize
     * @return The color corresponding to <code>name</code>
     */
    @ColorInt
    public static int mentionColor(String name) {
        return hslToRgbInt(hue(name), COLOR_AT_SATURATION, COLOR_AT_LIGHTNESS);
    }

    /**
     * Install the given OnClickListener to be invoked when the given view receives a single tap
     * This is meant to be used on TextView-s with textIsSelectable set, which swallow normal click events.
     *
     * @param v View to instrument
     * @param l Listener to install
     */
    public static void setSelectableOnClickListener(final View v, final View.OnClickListener l) {
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
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                d.onTouchEvent(event);
                return false;
            }
        });
    }

    public static void setEnterKeyListener(final TextView v, final int expectedActionId, final Runnable r) {
        v.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == expectedActionId || event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                        event.getAction() == KeyEvent.ACTION_DOWN) {
                    r.run();
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

}
