package io.euphoria.xkcd.app.impl.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import io.euphoria.xkcd.app.R;

/** Created by Xyzzy on 2018-10-15. */

class TriangleView extends View {

    private final Path triangle;
    private final Paint paint;
    private boolean pointDown = false;

    public TriangleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        triangle = new Path();
        paint = new Paint();
        // TODO get color from attrs
        paint.setColor(ContextCompat.getColor(context, R.color.indent_line));
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
    }

    public boolean isPointDown() {
        return pointDown;
    }

    public void setPointDown(boolean pointDown) {
        this.pointDown = pointDown;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        triangle.reset();
        triangle.moveTo(0, 0);
        if (pointDown) {
            triangle.lineTo(w, 0);
            triangle.lineTo(w / 2.0f, h);
        } else {
            triangle.lineTo(0, h);
            triangle.lineTo(w, h / 2.0f);
        }
        triangle.close();
        canvas.drawPath(triangle, paint);
    }

}
