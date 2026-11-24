package io.github.nicochanyo.c17fpoverlap;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

/**
 * {@link ProgressiveBlurFrameLayout} supporting interactive ColorOS 36dp left/right
 * edge-swipe back for full-screen secondary pages.
 */
public class SwipeableBlurFrameLayout extends ProgressiveBlurFrameLayout {
    public interface OnEdgeSwipeListener {
        void onSwipeStart();

        void onSwipeDrag(float deltaX, float progress);

        void onSwipeEnd(boolean commitClose);
    }

    private float edgeWidthPx;
    private float topBarSafeY;
    private int touchSlop;
    private boolean edgeSwipeEnabled;
    private OnEdgeSwipeListener edgeSwipeListener;
    private VelocityTracker velocityTracker;
    private boolean tracking;
    private boolean dragging;
    private float downX;
    private float downY;

    public SwipeableBlurFrameLayout(Context context) {
        super(context);
        init(context);
    }

    public SwipeableBlurFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SwipeableBlurFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        float d = context.getResources().getDisplayMetrics().density;
        this.edgeWidthPx = 36f * d;
        this.topBarSafeY = 96f * d;
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setEdgeSwipeEnabled(boolean enabled) {
        this.edgeSwipeEnabled = enabled;
        if (!enabled) {
            tracking = false;
            dragging = false;
            recycleVt();
        }
    }

    public void setTopBarSafeY(float safeY) {
        this.topBarSafeY = safeY;
    }

    public void setOnEdgeSwipeListener(OnEdgeSwipeListener listener) {
        this.edgeSwipeListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!edgeSwipeEnabled || edgeSwipeListener == null) {
            return super.onInterceptTouchEvent(ev);
        }
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            int w = getWidth();
            float x = ev.getX();
            float y = ev.getY();
            boolean inTopBarRow = y <= topBarSafeY;
            if (w > 0 && !inTopBarRow && (x <= edgeWidthPx || x >= w - edgeWidthPx)) {
                tracking = true;
                dragging = false;
                downX = ev.getRawX();
                downY = ev.getRawY();
                initVt();
                velocityTracker.addMovement(ev);
            } else {
                tracking = false;
            }
        } else if (action == MotionEvent.ACTION_MOVE && tracking) {
            if (velocityTracker != null) {
                velocityTracker.addMovement(ev);
            }
            float dx = Math.abs(ev.getRawX() - downX);
            float dy = Math.abs(ev.getRawY() - downY);
            if (dy > touchSlop && dy > dx * 1.15f) {
                tracking = false;
                recycleVt();
            } else if (dx > touchSlop && dx > dy * 1.2f) {
                dragging = true;
                edgeSwipeListener.onSwipeStart();
                return true;
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            tracking = false;
            dragging = false;
            recycleVt();
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!dragging || edgeSwipeListener == null) {
            return super.onTouchEvent(ev);
        }
        if (velocityTracker != null) {
            velocityTracker.addMovement(ev);
        }
        int width = Math.max(1, getWidth());
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_MOVE) {
            float delta = Math.max(0f, Math.min(width, Math.abs(ev.getRawX() - downX)));
            edgeSwipeListener.onSwipeDrag(delta, delta / width);
            return true;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            float delta = Math.max(0f, Math.abs(ev.getRawX() - downX));
            float vx = 0f;
            if (velocityTracker != null) {
                velocityTracker.computeCurrentVelocity(1000);
                vx = Math.abs(velocityTracker.getXVelocity());
            }
            recycleVt();
            tracking = false;
            dragging = false;
            float minVel = 650f * getResources().getDisplayMetrics().density;
            boolean commitClose = action == MotionEvent.ACTION_UP && (delta > width * 0.30f || vx > minVel);
            edgeSwipeListener.onSwipeEnd(commitClose);
            return true;
        }
        return super.onTouchEvent(ev);
    }

    @Override
    protected void onDetachedFromWindow() {
        tracking = false;
        dragging = false;
        recycleVt();
        super.onDetachedFromWindow();
    }

    private void initVt() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        } else {
            velocityTracker.clear();
        }
    }

    private void recycleVt() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }
}
