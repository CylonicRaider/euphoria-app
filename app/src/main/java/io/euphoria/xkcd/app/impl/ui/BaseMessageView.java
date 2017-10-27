package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import static io.euphoria.xkcd.app.impl.ui.UIUtils.dpToPx;

/** Created by Xyzzy on 2017-10-27. */

abstract class BaseMessageView extends RelativeLayout {

    private static final int PADDING_PER_INDENT = 15;

    private MessageTree message;
    private boolean established;

    public BaseMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageTree getMessage() {
        return message;
    }

    public void setMessage(@NonNull MessageTree message) {
        if (established) {
            throw new IllegalStateException("Updating message of view without resetting");
        } else {
            this.message = message;
            if (message.getID() != null) setTag(message.getID());
            established = true;
            updateDisplay();
        }
    }

    protected abstract void updateDisplay();

    public void recycle() {
        message = null;
        established = false;
    }

    public static int computeIndentWidth(Context ctx, int indent) {
        return indent * dpToPx(ctx, PADDING_PER_INDENT);
    }

    @SuppressWarnings({"ResourceType", "UnnecessaryFullyQualifiedName"})
    static MarginLayoutParams getDefaultMargins(Context context, AttributeSet attrs) {
        // FIXME only resolving some of the attributes; move into custom layout manager?
        // Here be dragons
        int[] indices;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            indices = new int[] {android.R.attr.layout_width, android.R.attr.layout_height,
                    android.R.attr.layout_marginLeft, android.R.attr.layout_marginTop,
                    android.R.attr.layout_marginRight, android.R.attr.layout_marginBottom,
                    android.R.attr.layout_marginStart, android.R.attr.layout_marginEnd};
        } else {
            indices = new int[] {android.R.attr.layout_width, android.R.attr.layout_height,
                    android.R.attr.layout_marginLeft, android.R.attr.layout_marginTop,
                    android.R.attr.layout_marginRight, android.R.attr.layout_marginBottom};
        }
        TypedArray values = context.obtainStyledAttributes(attrs, indices);
        ViewGroup.LayoutParams core = new ViewGroup.LayoutParams(
                values.getLayoutDimension(0, LayoutParams.MATCH_PARENT),
                values.getLayoutDimension(1, LayoutParams.WRAP_CONTENT));
        MarginLayoutParams ret = new MarginLayoutParams(core);
        ret.setMargins(values.getDimensionPixelSize(2, 0), values.getDimensionPixelSize(3, 0),
                values.getDimensionPixelSize(4, 0), values.getDimensionPixelSize(5, 0));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            ret.setMarginStart(values.getDimensionPixelSize(6, 0));
            ret.setMarginStart(values.getDimensionPixelSize(7, 0));
        }
        values.recycle();
        return ret;
    }

    static void setMarginForIndent(Context context, MarginLayoutParams params, int indent) {
        int l = computeIndentWidth(context, indent);
        params.leftMargin = l;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            params.setMarginStart(l);
        }
    }

}
