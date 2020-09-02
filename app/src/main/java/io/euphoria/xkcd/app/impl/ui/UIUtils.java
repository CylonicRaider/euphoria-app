package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class UIUtils {

    // Color saturation and lightness for input bar and messages
    public static final float COLOR_SENDER_SATURATION = 0.65f;
    public static final float COLOR_SENDER_LIGHTNESS = 0.85f;

    // Color saturation and lightness for emotes
    private static final float COLOR_EMOTE_SATURATION = 0.65f;
    private static final float COLOR_EMOTE_LIGHTNESS = 0.9f; // Euphoria has 0.95f

    // Color saturation and lightness for @mentions
    public static final float COLOR_AT_SATURATION = 0.42f;
    public static final float COLOR_AT_LIGHTNESS = 0.50f;

    // TODO check against actual list of valid emoji
    private static final Pattern EMOJI_RE = Pattern.compile(":[a-zA-Z!?\\-]+?:");

    // Emote message testing
    public static final Pattern EMOTE_RE = Pattern.compile("^/me");
    public static final int MAX_EMOTE_LENGTH = 240;

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
     * Obtain a color from the given resource ID.
     *
     * @param ctx   The context w.r.t. which to resolve.
     * @param resid The resource ID of the color to obtain.
     * @return The resolved color.
     */
    @ColorInt
    public static int getColor(Context ctx, @ColorRes int resid) {
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            return ctx.getColor(resid);
        } else {
            return ctx.getResources().getColor(resid);
        }
    }

    /**
     * Obtain a Drawable for the given resource ID.
     *
     * @param ctx      The context w.r.t. which to resolve.
     * @param drawable The resource ID of the Drawable to obtain.
     * @return The Drawable.
     */
    private static Drawable getDrawable(Context ctx, @DrawableRes int drawable) {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            return ctx.getDrawable(drawable);
        } else {
            return ctx.getResources().getDrawable(drawable);
        }
    }

    /**
     * Obtain an instance of the drawable resource with the given color applied
     * @param ctx Context
     * @param drawable Resource ID of the drawable to color
     * @param color Color to apply
     * @return A mutable Drawable with the given color applied
     */
    public static Drawable colorDrawable(Context ctx, @DrawableRes int drawable, @ColorInt int color) {
        GradientDrawable ret = (GradientDrawable) getDrawable(ctx, drawable);
        if (ret != null) {
            ret.mutate();
            ret.setColor(color);
        }
        return ret;
    }

    /**
     * Convenience function for applying a drawable background to a view
     * <p>
     * Note that there might be subtle behavioral differences depending on the API level.
     *
     * @param v        The view to apply a background to
     * @param drawable The drawable to use as a background (or <code>null/code> to remove the background)
     */
    public static void setViewBackground(View v, Drawable drawable) {
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            v.setBackground(drawable);
        } else {
            v.setBackgroundDrawable(drawable);
        }
    }

    /**
     * Convenience function for applying a drawable background to a view
     * <p>
     * Note that there might be subtle behavioral differences depending on the API level.
     *
     * @param v        The view to apply the background to
     * @param drawable The drawable to use as a background (or <code>0</code>> to remove the background)
     */
    public static void setViewBackground(View v, @DrawableRes int drawable) {
        setViewBackground(v, getDrawable(v.getContext(), drawable));
    }

    /**
     * Convenience function for applying a tinted background to a view
     *
     * @param v        View whose background to set
     * @param drawable Resource ID of the drawable to use
     * @param color    Color to apply
     * @return The Drawable that was installed as the background
     */
    public static Drawable setColoredBackground(View v, @DrawableRes int drawable, @ColorInt int color) {
        Drawable ret = colorDrawable(v.getContext(), drawable, color);
        setViewBackground(v, ret);
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
    private static strictfp double hueHash(String text, double offset) {
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
            /* Double cast to avoid integer saturation; recall that the shift is applied after the casts */
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
     * Trim all unicode whitespace characters from the start and end of the passed String.
     *
     * String::trim() uses a non-unicode-aware (or only partially so) concept of whitespace characters.
     */
    @NonNull
    public static String trimUnicodeWhitespace(@NonNull String text) {
        return text.replaceAll("^\\p{Z}+|\\p{Z}+$", "");
    }

    /**
     * Obtain the hue associated with the given nickname
     * Use the *Color methods to compose ready-to-use colors.
     * @param text The nickname whose hue to obtain
     * @return The hue corresponding to <code>text</code>
     */
    public static double hue(String text) {
        String normalized = normalize(text);

        if (normalized.isEmpty()) {
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
     * Obtain the color corresponding to the given nickname for use as a light background
     *
     * @param name The nickname to colorize
     * @return the color corresponding to <code>name</code>
     */
    @ColorInt
    public static int emoteColor(String name) {
        return hslToRgbInt(hue(name), COLOR_EMOTE_SATURATION, COLOR_EMOTE_LIGHTNESS);
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
     * Test whether the given message content string corresponds to an emote message
     *
     * @param text The message content to test
     * @return Whether <code>text</code> is an emote message text
     */
    public static boolean isEmote(String text) {
        return text.length() < MAX_EMOTE_LENGTH && EMOTE_RE.matcher(text).find();
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
            public boolean onDown(MotionEvent e) {
                // HACK: Gingerbread does not deliver the ACTION_UP event unless the ACTION_DOWN one has been
                //       consumed.
                return true;
            }

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
                // HACK: The gesture detector eats too many events.
                return false;
            }
        });
    }

    /**
     * Set an OnEditorActionListener at the given view that executes the given Runnable when the Enter key is pressed.
     *
     * @param v                The View to modify.
     * @param expectedActionId The expected IME action ID (such as EditorInfo.IME_ACTION_GO).
     * @param r                The Runnable to execute.
     */
    public static void setEnterKeyListener(final TextView v, final int expectedActionId, final Runnable r) {
        v.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == expectedActionId || event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                        event.getAction() == KeyEvent.ACTION_DOWN) {
                    r.run();
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    /**
     * Null-safe method for comparing objects.
     *
     * @param a An arbitrary object, or null.
     * @param b An arbitrary object, or null.
     * @return {@code (a == null) ? (b == null) : a.equals(b)}
     */
    public static boolean equalsOrNull(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    /**
     * Null-safe hash code calculation method.
     *
     * @param a An arbitrary object, or null.
     * @return {@code (a == null) ? 0 : a.hashCode()}
     */
    public static int hashCodeOrNull(Object a) {
        return (a == null) ? 0 : a.hashCode();
    }

    /**
     * Insert the given item into the given list using binary search.
     * <p>
     * The list must be sorted according to T's natural ordering.
     *
     * @param list A list of things.
     * @param item The item to insert.
     * @return The index into the list at which item now resides.
     */
    public static <T extends Comparable<T>> int insertSorted(List<T> list, T item) {
        int idx = Collections.binarySearch(list, item);
        if (idx >= 0) {
            list.set(idx, item);
            return idx;
        } else {
            list.add(-idx - 1, item);
            return -idx - 1;
        }
    }

    /**
     * Remove the given item from the given list using binary search.
     * <p>
     * The list must be sorted according to T's natural ordering.
     *
     * @param list A list of things.
     * @param item The item to remove.
     * @return The index at which item resided, or -1 if it had not been in the list before the call.
     */
    public static <T extends Comparable<T>> int removeSorted(List<T> list, T item) {
        int idx = Collections.binarySearch(list, item);
        if (idx >= 0) {
            list.remove(idx);
            return idx;
        } else {
            return -1;
        }
    }

}
