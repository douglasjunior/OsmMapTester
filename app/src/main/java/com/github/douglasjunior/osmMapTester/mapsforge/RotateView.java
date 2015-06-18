package com.github.douglasjunior.osmMapTester.mapsforge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;


/**
 * Created by douglas on 17/06/15.
 */
public class RotateView extends ViewGroup {

    private float heading = 0;
    private final Matrix matrix = new Matrix();
    private final float[] points = new float[2];
    private final SmoothCanvas smoothCanvas = new SmoothCanvas();

    public RotateView(Context context) {
        this(context, null);
    }

    public RotateView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.rotate(-heading, getWidth() * 0.5f, getHeight() * 0.5f);
        smoothCanvas.delegate = canvas;
        super.dispatchDraw(smoothCanvas);
        canvas.restore();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        MotionEvent rotatedEvent = rotateTouchEvent(event, -heading,
                getWidth(), getHeight());
        try {
            return super.dispatchTouchEvent(rotatedEvent);
        } finally {
            if (rotatedEvent != event)
                rotatedEvent.recycle();
        }
    }

    public float getHeading() {
        return heading;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = getWidth();
        int height = getHeight();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            int childWidth = view.getMeasuredWidth();
            int childHeight = view.getMeasuredHeight();
            int childLeft = (width - childWidth) / 2;
            int childTop = (height - childHeight) / 2;
            view.layout(childLeft, childTop, childLeft + childWidth, childTop
                    + childHeight);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int h = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        int sizeSpec = MeasureSpec.makeMeasureSpec((int) Math.hypot(w, h),
                MeasureSpec.EXACTLY);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(sizeSpec, sizeSpec);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private MotionEvent rotateTouchEvent(MotionEvent ev, float mapOrientation,
                                         int width, int height) {
        matrix.setRotate(-mapOrientation, width / 2, height / 2);

        MotionEvent rotatedEvent = MotionEvent.obtain(ev);
        points[0] = ev.getX();
        points[1] = ev.getY();
        matrix.mapPoints(points);
        rotatedEvent.setLocation(points[0], points[1]);
        return rotatedEvent;
    }

    public void setHeading(float heading) {
        this.heading = heading;
    }
}