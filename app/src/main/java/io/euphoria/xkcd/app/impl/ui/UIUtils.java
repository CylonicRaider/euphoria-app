package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.TypedValue;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class UIUtils {

    // Color saturation and lightness for input-bar and messages
    public static final float COLOR_SENDER_SAT = 0.65f;
    public static final float COLOR_SENDER_LIGHTNESS = 0.85f;
    // Color saturation and lightness for @mentions
    public static final float COLOR_AT_SAT = 0.42f;
    public static final float COLOR_AT_LIGHTNESS = 0.50f;
    // TODO possibly check againts actual list of valid emoji
    private static final Pattern EMOJI_RE = Pattern.compile(":[a-zA-Z!?\\-]+?:");
    // Cache for hue-hashes
    private static final Map<String, Double> HUE_CACHE = new HashMap<>();

    public static int dpToPx(Context ctx, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics());
    }

    // Deal with the tinting drawables, either with the API for v21+ or with appcompat
    public static @Nullable Drawable tintDrawable(Context ctx, @DrawableRes int drawable, @ColorInt int color) {
        Drawable tintedDrawable;
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            tintedDrawable = ctx.getDrawable(drawable);
            if (tintedDrawable != null) {
                tintedDrawable.mutate().setTint(color);
            }
        } else {
            tintedDrawable = ctx.getResources().getDrawable(drawable);
            if (tintedDrawable != null) {
                tintedDrawable = DrawableCompat.wrap(tintedDrawable);
                DrawableCompat.setTint(tintedDrawable.mutate(), color);
            }
        }
        return tintedDrawable;
    }

    /*                                                                       *
     * Hue hashing code ported to java from                                  *
     * https://github.com/euphoria-io/heim/blob/master/client/lib/hueHash.js *
     *                                                                       */

    private static double hueHash(String text) {
        return hueHash(text, 0);
    }

    private static double hueHash(String text, double offset) {
        // DJBX33A-ish
        double val = 0d;
        for (int i = 0; i < text.length(); i++) {
            // scramble char codes across [0-255]
            // prime multiple chosen so @greenie can green, and @redtaboo red.
            double charVal = (text.charAt(i) * 439d) % 256d;

            // multiply val by 33 while constraining within signed 32 bit int range.
            // this keeps the value within Number.MAX_SAFE_INTEGER without throwing out
            // information.
            double origVal = val;
            val = (int) (long) val << 5;
            val += origVal;

            // add the character information to the hash.
            val += charVal;
        }

        val = (int) (long) val;

        // add the minimum possible value, to ensure that val is positive (without
        // throwing out information).
        val += Math.pow(2d, 31d);

        // add the calibration offset and scale within 0-254 (an arbitrary range kept
        // for consistency with prior behavior).
        return (val + offset) % 255d;
    }

    private static final double greenieOffset = 148d - hueHash("greenie");

    public static double hue(String text) {
        String normalized = normalize(text);

        if (normalized.length() == 0) {
            normalized = text;
        }

        if (HUE_CACHE.containsKey(normalized)) {
            return HUE_CACHE.get(normalized);
        }

        double val = hueHash(normalized, greenieOffset);
        HUE_CACHE.put(normalized, val);
        return val;
    }

    private static String normalize(String text) {
        return EMOJI_RE.matcher(text).replaceAll("")
                .replaceAll("[^\\w_\\-]", "")
                .toLowerCase();
    }

    /**
     * Converts hsla color values to an integer representing the color in 0xAARRGGBB format
     * if h > 360 or h < 0: h = h%360
     * if s > 1 or s < 0: s = max(min(s,1),0)
     * if l > 1 or l < 0: l = max(min(l,1),0)
     * if a > 1 or a < 0: a = max(min(a,1),0)
     *
     * @param h Hue
     * @param s Saturation
     * @param l Lightness
     * @param a Opacity
     * @return RGB-ColorInt
     */
    public static @ColorInt int hslaToRgbaInt(double h, double s, double l, double a) {
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
        a = Math.max(Math.min(a*255, 255f), 0f);
        return ((int) a << 8 * 3) + ((int) r << 8 * 2) + ((int) g << 8) + (int) b;
    }


    /**
     * Converts hsl color values to an integer representing the color in 0xFFRRGGBB format
     * if h > 360 or h < 0: h = h%360
     * if s > 1 or s < 0: s = max(min(s,1),0)
     * if l > 1 or l < 0: l = max(min(l,1),0)
     *
     * @param h Hue
     * @param s Saturation
     * @param l Lightness
     * @return RGB-ColorInt
     */
    public static @ColorInt int hslToRgbInt(double h, double s, double l) {
        return hslaToRgbaInt(h, s, l, 1f);
    }
}
