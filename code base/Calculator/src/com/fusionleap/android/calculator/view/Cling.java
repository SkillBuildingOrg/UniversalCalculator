

package com.fusionleap.android.calculator.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Window;
import android.widget.FrameLayout;

import com.fusionleap.android.calculator.Calculator;
import com.fusionleap.calculator.R;


public class Cling extends FrameLayout {
    public static final int SHOW_CLING_DURATION = 600;
    public static final int DISMISS_CLING_DURATION = 250;

    public static final String SIMPLE_CLING_DISMISSED_KEY = "cling.simple.dismissed";
    public static final String MATRIX_CLING_DISMISSED_KEY = "cling.matrix.dismissed";
    public static final String HEX_CLING_DISMISSED_KEY = "cling.hex.dismissed";
    public static final String GRAPH_CLING_DISMISSED_KEY = "cling.graph.dismissed";

    private Calculator mCalculator;
    private boolean mIsInitialized;
    private Drawable mBackground;
    private Drawable mPunchThroughGraphic;
    private Drawable mHandTouchGraphic;
    private int mPunchThroughGraphicCenterRadius;
    private float mRevealRadius;
    private int[] mPositionData;
    private boolean mShowHand;
    private boolean mDismissed;

    private Paint mErasePaint;

    public Cling(final Context context) {
        this(context, null, 0);
    }

    public Cling(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Cling(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init(final Calculator c, final int[] positionData, final float revealRadius, final boolean showHand) {
        if(!mIsInitialized) {
            mCalculator = c;
            mPositionData = positionData;
            mShowHand = showHand;
            mDismissed = false;

            Resources r = getContext().getResources();
            mPunchThroughGraphic = r.getDrawable(R.drawable.cling);
            mPunchThroughGraphicCenterRadius = r.getDimensionPixelSize(R.dimen.clingPunchThroughGraphicCenterRadius);
            mRevealRadius = revealRadius;

            mErasePaint = new Paint();
            mErasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
            mErasePaint.setColor(0xFFFFFF);
            mErasePaint.setAlpha(0);

            mIsInitialized = true;
        }
    }

    public void dismiss() {
        mDismissed = true;
    }

    boolean isDismissed() {
        return mDismissed;
    }

    public void cleanup() {
        mBackground = null;
        mPunchThroughGraphic = null;
        mHandTouchGraphic = null;
        mIsInitialized = false;
    }

    private int[] getPunchThroughPosition() {
        if(mPositionData != null) {
            return mPositionData;
        }
        return new int[] { -1, -1, -1 };
    }

    @Override
    public boolean onTouchEvent(final android.view.MotionEvent event) {
        int[] pos = getPunchThroughPosition();
        double diff = Math.sqrt(Math.pow(event.getX() - pos[0], 2) + Math.pow(event.getY() - pos[1], 2));
        if(diff < mRevealRadius) {
            return false;
        }
        return true;
    };

    @Override
    protected void dispatchDraw(final Canvas canvas) {
        if(mIsInitialized) {
            DisplayMetrics metrics = new DisplayMetrics();
            mCalculator.getWindowManager().getDefaultDisplay().getMetrics(metrics);

            // Initialize the draw buffer (to allow punching through)
            final int width = getMeasuredWidth() <= 0 ? metrics.widthPixels : getMeasuredWidth();
            final int height = getMeasuredHeight() <= 0 ? metrics.heightPixels : getMeasuredHeight();

            Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);

            // Draw the background
            if(mBackground == null) {
                mBackground = getResources().getDrawable(R.drawable.bg_cling);
            }
            if(mBackground != null) {
                mBackground.setBounds(0, 0, width, height);
                mBackground.draw(c);
            }
            else {
                c.drawColor(0x99000000);
            }

            int cx = -1;
            int cy = -1;
            int cz = -1;
            float scale = mRevealRadius / mPunchThroughGraphicCenterRadius;
            int dw = (int) (scale * mPunchThroughGraphic.getIntrinsicWidth());
            int dh = (int) (scale * mPunchThroughGraphic.getIntrinsicHeight());

            // Determine where to draw the punch through graphic
            Rect rect = new Rect();
            Window window = ((Activity) getContext()).getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rect);
            int statusBarHeight = rect.top;
            int[] pos = getPunchThroughPosition();
            cx = pos[0];
            cy = pos[1] - statusBarHeight;
            cz = pos[2];
            if(cx > -1 && cy > -1 && scale > 0) {
                c.drawCircle(cx, cy, mRevealRadius, mErasePaint);
                mPunchThroughGraphic.setBounds(cx - dw / 2, cy - dh / 2, cx + dw / 2, cy + dh / 2);
                mPunchThroughGraphic.draw(c);
            }

            // Draw the hand graphic
            if(mShowHand) {
                if(mHandTouchGraphic == null) {
                    mHandTouchGraphic = getResources().getDrawable(R.drawable.hand);
                }
                int offset = cz;
                mHandTouchGraphic.setBounds(cx + offset, cy + offset, cx + mHandTouchGraphic.getIntrinsicWidth() + offset,
                        cy + mHandTouchGraphic.getIntrinsicHeight() + offset);
                mHandTouchGraphic.draw(c);
            }

            canvas.drawBitmap(b, 0, 0, null);
            c.setBitmap(null);
            b = null;
        }

        // Draw the rest of the cling
        super.dispatchDraw(canvas);
    };
}
