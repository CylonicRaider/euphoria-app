package io.euphoria.xkcd.app.impl.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.TypedValue
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.view.GestureDetectorCompat
import java.util.*
import java.util.regex.Pattern

object UIUtils {
    // Color saturation and lightness for input bar and messages
    const val COLOR_SENDER_SATURATION = 0.65f
    const val COLOR_SENDER_LIGHTNESS = 0.85f

    // Color saturation and lightness for emotes
    private const val COLOR_EMOTE_SATURATION = 0.65f
    private const val COLOR_EMOTE_LIGHTNESS = 0.9f // Euphoria has 0.95f

    // Color saturation and lightness for @mentions
    const val COLOR_AT_SATURATION = 0.42f
    const val COLOR_AT_LIGHTNESS = 0.50f

    // TODO check against actual list of valid emoji
    private val EMOJI_RE =
        Regex(":[a-zA-Z!?\\-]+?:")

    // Emote message testing
    val EMOTE_RE = Regex("^/me")
    const val MAX_EMOTE_LENGTH = 240

    // Hue hash cache
    private val HUE_CACHE: MutableMap<String?, Double> =
        HashMap()

    /**
     * Convenience function for mapping density-independent pixels to effective pixels.
     *
     * @param ctx Context
     * @param dp Value to convert in dp
     * @return Converted value in px
     */
    fun dpToPx(ctx: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
            ctx.resources.displayMetrics
        ).toInt()
    }

    /**
     * Obtain a color from the given resource ID.
     *
     * @param ctx   The context w.r.t. which to resolve.
     * @param resid The resource ID of the color to obtain.
     * @return The resolved color.
     */
    @ColorInt
    fun getColor(ctx: Context, @ColorRes resid: Int): Int {
        return if (VERSION.SDK_INT >= VERSION_CODES.M) {
            ctx.getColor(resid)
        } else {
            ctx.resources.getColor(resid)
        }
    }

    /**
     * Obtain a Drawable for the given resource ID.
     *
     * @param ctx      The context w.r.t. which to resolve.
     * @param drawable The resource ID of the Drawable to obtain.
     * @return The Drawable.
     */
    private fun getDrawable(ctx: Context, @DrawableRes drawable: Int): Drawable? {
        return if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            ctx.getDrawable(drawable)
        } else {
            ctx.resources.getDrawable(drawable)
        }
    }

    /**
     * Obtain an instance of the drawable resource with the given color applied
     * @param ctx Context
     * @param drawable Resource ID of the drawable to color
     * @param color Color to apply
     * @return A mutable Drawable with the given color applied
     */
    fun colorDrawable(
        ctx: Context,
        @DrawableRes drawable: Int,
        @ColorInt color: Int
    ): Drawable? {
        val ret = getDrawable(ctx, drawable) as GradientDrawable?
        if (ret != null) {
            ret.mutate()
            ret.setColor(color)
        }
        return ret
    }

    /**
     * Convenience function for applying a drawable background to a view
     *
     *
     * Note that there might be subtle behavioral differences depending on the API level.
     *
     * @param v        The view to apply a background to
     * @param drawable The drawable to use as a background (or `null/code> to remove the background)
    ` */
    fun setViewBackground(v: View?, drawable: Drawable?) {
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            v!!.background = drawable
        } else {
            v!!.setBackgroundDrawable(drawable)
        }
    }

    /**
     * Convenience function for applying a drawable background to a view
     *
     *
     * Note that there might be subtle behavioral differences depending on the API level.
     *
     * @param v        The view to apply the background to
     * @param drawable The drawable to use as a background (or `0`> to remove the background)
     */
    fun setViewBackground(v: View, @DrawableRes drawable: Int) {
        setViewBackground(v, getDrawable(v.context, drawable))
    }

    /**
     * Convenience function for applying a tinted background to a view
     *
     * @param v        View whose background to set
     * @param drawable Resource ID of the drawable to use
     * @param color    Color to apply
     * @return The Drawable that was installed as the background
     */
    fun setColoredBackground(
        v: View?,
        @DrawableRes drawable: Int,
        @ColorInt color: Int
    ): Drawable? {
        val ret = colorDrawable(v!!.context, drawable, color)
        setViewBackground(v, ret)
        return ret
    }
    /*                                                                       *
     * Hue hashing code ported to Java from                                  *
     * https://github.com/euphoria-io/heim/blob/master/client/lib/hueHash.js *
     *                                                                       */
    /**
     * Raw hue hash implementation
     * Inline comments are original; block comments are porters' notes.
     */
    @Strictfp
    private fun hueHash(text: String?, offset: Double): Double {
        // DJBX33A-ish
        var `val` = 0.0
        for (i in 0 until text!!.length) {
            // scramble char codes across [0-255]
            // prime multiple chosen so @greenie can green, and @redtaboo red.
            val charVal = text[i].toDouble() * 439.0 % 256.0

            // multiply val by 33 while constraining within signed 32 bit int range.
            // this keeps the value within Number.MAX_SAFE_INTEGER without throwing out
            // information.
            val origVal = `val`
            /* Double cast to avoid integer saturation; recall that the shift is applied after the casts */`val` =
                (`val`.toLong().toInt() shl 5.toDouble().toInt()).toDouble()
            `val` += origVal

            // add the character information to the hash.
            `val` += charVal
        }

        // cast the result of the final character addition to a 32 bit int.
        `val` = `val`.toInt().toDouble()

        // add the minimum possible value, to ensure that val is positive (without
        // throwing out information).
        /* Original has Math.pow(2.0, 31.0) */`val` += 1L shl 31.toDouble().toInt()

        // add the calibration offset and scale within 0-254 (an arbitrary range kept
        // for consistency with prior behavior).
        return (`val` + offset) % 255.0
    }

    private val greenieOffset = 148.0 - hueHash("greenie", 0.0)

    /** Normalize a string for hue hash processing  */
    private fun normalize(text: String): String {
        return EMOJI_RE.replace(text, "").replace("[^\\w_\\-]".toRegex(), "")
            .toLowerCase(Locale.ROOT)
    }

    /**
     * Obtain the hue associated with the given nickname
     * Use the *Color methods to compose ready-to-use colors.
     * @param text The nickname whose hue to obtain
     * @return The hue corresponding to `text`
     */
    fun hue(text: String): Double {
        var normalized: String = normalize(text)
        if (normalized.isEmpty()) {
            normalized = text
        }
        var ret = HUE_CACHE[normalized]
        if (ret == null) {
            ret = hueHash(normalized, greenieOffset)
            HUE_CACHE[normalized] = ret
        }
        return ret
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
    fun hslaToRgbaInt(h: Double, s: Double, l: Double, a: Double): Int {
        // ensure proper values
        var h = h
        var s = s
        var l = l
        var a = a
        h = (h % 360 + 360) % 360 // real modulus not just remainder
        s = Math.max(Math.min(s, 1.0), 0.0)
        l = Math.max(Math.min(l, 1.0), 0.0)
        a = Math.max(Math.min(a, 1.0), 0.0)
        // convert
        val c = (1 - Math.abs(2 * l - 1)) * s
        val x = c * (1 - Math.abs(h / 60f % 2 - 1))
        val m = l - c / 2
        var r = 0.0
        var g = 0.0
        var b = 0.0
        if (h >= 0 && h < 60) {
            r = c
            g = x
            b = 0.0
        } else if (h >= 60 && h < 120) {
            r = x
            g = c
            b = 0.0
        } else if (h >= 120 && h < 180) {
            r = 0.0
            g = c
            b = x
        } else if (h >= 180 && h < 240) {
            r = 0.0
            g = x
            b = c
        } else if (h >= 240 && h < 300) {
            r = x
            g = 0.0
            b = c
        } else if (h >= 300 && h < 360) {
            r = c
            g = 0.0
            b = x
        }
        r = ((r + m) * 255).coerceAtMost(255.0).coerceAtLeast(0.0)
        g = ((g + m) * 255).coerceAtMost(255.0).coerceAtLeast(0.0)
        b = ((b + m) * 255).coerceAtMost(255.0).coerceAtLeast(0.0)
        a = (a * 255).coerceAtMost(255.0).coerceAtLeast(0.0)
        return (a.toInt() shl 8 * 3) + (r.toInt() shl 8 * 2) + (g.toInt() shl 8) + b.toInt()
    }

    /**
     * Convert an HSL color value to an integer representing the color in 0xFFRRGGBB format
     * @see .hslaToRgbaInt
     */
    @ColorInt
    fun hslToRgbInt(h: Double, s: Double, l: Double): Int {
        return hslaToRgbaInt(h, s, l, 1.0)
    }

    /**
     * Obtain the color corresponding to the given nickname for use as a background
     * @param name The nickname to colorize
     * @return The color corresponding to `name`
     */
    @ColorInt
    fun nickColor(name: String): Int {
        return hslToRgbInt(
            hue(name),
            COLOR_SENDER_SATURATION.toDouble(),
            COLOR_SENDER_LIGHTNESS.toDouble()
        )
    }

    /**
     * Obtain the color corresponding to the given nickname for use as a light background
     *
     * @param name The nickname to colorize
     * @return the color corresponding to `name`
     */
    @ColorInt
    fun emoteColor(name: String): Int {
        return hslToRgbInt(
            hue(name),
            COLOR_EMOTE_SATURATION.toDouble(),
            COLOR_EMOTE_LIGHTNESS.toDouble()
        )
    }

    /**
     * Obtain the color corresponding to the given nickname for use as a text color
     * @param name The nickname to colorize
     * @return The color corresponding to `name`
     */
    @ColorInt
    fun mentionColor(name: String): Int {
        return hslToRgbInt(
            hue(name),
            COLOR_AT_SATURATION.toDouble(),
            COLOR_AT_LIGHTNESS.toDouble()
        )
    }

    /**
     * Test whether the given message content string corresponds to an emote message
     *
     * @param text The message content to test
     * @return Whether `text` is an emote message text
     */
    fun isEmote(text: String): Boolean {
        return text.length < MAX_EMOTE_LENGTH && EMOTE_RE matches text
    }

    /**
     * Install the given OnClickListener to be invoked when the given view receives a single tap
     * This is meant to be used on TextView-s with textIsSelectable set, which swallow normal click events.
     *
     * @param v View to instrument
     * @param l Listener to install
     */
    fun setSelectableOnClickListener(
        v: View,
        l: View.OnClickListener?
    ) {
        if (l == null) {
            v.setOnTouchListener(null)
            return
        }
        val g: GestureDetector.OnGestureListener = object : SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                // HACK: Gingerbread does not deliver the ACTION_UP event unless the ACTION_DOWN one has been
                //       consumed.
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                l.onClick(v)
                return true
            }
        }
        val d = GestureDetectorCompat(v.context, g)
        v.setOnTouchListener { v, event ->
            d.onTouchEvent(event)
            // HACK: The gesture detector eats too many events.
            false
        }
    }

    /**
     * Set an OnEditorActionListener at the given view that executes the given Runnable when the Enter key is pressed.
     *
     * @param v                The View to modify.
     * @param expectedActionId The expected IME action ID (such as EditorInfo.IME_ACTION_GO).
     * @param r                The Runnable to execute.
     */
    fun setEnterKeyListener(
        v: TextView?,
        expectedActionId: Int,
        r: Runnable
    ) {
        v!!.setOnEditorActionListener { v, actionId, event ->
            if (actionId == expectedActionId || event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
            ) {
                r.run()
                true
            } else {
                false
            }
        }
    }

    /**
     * Null-safe method for comparing objects.
     *
     * @param a An arbitrary object, or null.
     * @param b An arbitrary object, or null.
     * @return `(a == null) ? (b == null) : a.equals(b)`
     */
    fun equalsOrNull(a: Any?, b: Any?): Boolean {
        return if (a == null) b == null else a == b
    }

    /**
     * Null-safe hash code calculation method.
     *
     * @param a An arbitrary object, or null.
     * @return `(a == null) ? 0 : a.hashCode()`
     */
    fun hashCodeOrNull(a: Any?): Int {
        return a?.hashCode() ?: 0
    }

    /**
     * Insert the given item into the given list using binary search.
     *
     *
     * The list must be sorted according to T's natural ordering.
     *
     * @param list A list of things.
     * @param item The item to insert.
     * @return The index into the list at which item now resides.
     */
    fun <T : Comparable<T>?> insertSorted(
        list: MutableList<T>,
        item: T
    ): Int {
        val idx = Collections.binarySearch(list, item)
        return if (idx >= 0) {
            list[idx] = item
            idx
        } else {
            list.add(-idx - 1, item)
            -idx - 1
        }
    }

    /**
     * Remove the given item from the given list using binary search.
     *
     *
     * The list must be sorted according to T's natural ordering.
     *
     * @param list A list of things.
     * @param item The item to remove.
     * @return The index at which item resided, or -1 if it had not been in the list before the call.
     */
    fun <T : Comparable<T>?> removeSorted(list: MutableList<T>, item: T): Int {
        val idx = Collections.binarySearch(list, item)
        return if (idx >= 0) {
            list.removeAt(idx)
            idx
        } else {
            -1
        }
    }
}
