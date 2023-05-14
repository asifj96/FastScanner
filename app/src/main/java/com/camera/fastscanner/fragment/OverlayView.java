package com.camera.fastscanner.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;


public class OverlayView extends View {

    private final Paint paint = new Paint();
    private int alpha = 85;
    private final RectArea rectArea = new RectArea();

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.BLACK);
        paint.setAlpha(alpha);
    }


    public void setAlpha(int alpha) {
        this.alpha = alpha;
        paint.setAlpha(alpha);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Draw 4 rects
        float width = getWidth();
        float height = getHeight();


        if (getWidth() < getHeight()) {
            // Portrait
            rectArea.width = (getRight() - getLeft());
            rectArea.height = (getBottom() - getTop());
        } else {
            // Landscape
            rectArea.width = (getRight() - getLeft());
            rectArea.height = (getBottom() - getTop());

        }
        Log.i("OverlayView", "onDraw: " + rectArea.width + "?" + rectArea.height);


        // Draw around the rect.
        canvas.drawRect(0, 0, width, height / 2 - rectArea.height, paint);
        canvas.drawRect(0, rectArea.height + (height / 2), width, height, paint);
        canvas.drawRect(0, height / 2 - rectArea.height, width / 2 - rectArea.width, height / 2 + rectArea.height, paint);
        canvas.drawRect(width / 2 + rectArea.width, height / 2 - rectArea.height, width, height / 2 + rectArea.height, paint);

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    public static class RectArea {
        private int width;
        private int height;

        public RectArea(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public RectArea() {}
    }


}
