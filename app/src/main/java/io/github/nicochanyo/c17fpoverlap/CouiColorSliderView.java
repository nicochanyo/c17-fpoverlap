package io.github.nicochanyo.c17fpoverlap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

/**
 * Capsule-style ColorOS 17 HSV/Alpha color bar slider with hardware-accelerated cached
 * gradients, checkerboard alpha backdrop, and white donut-ring thumb indicator.
 */
public class CouiColorSliderView extends View {
    public static final int MODE_PREVIEW = 0;
    public static final int MODE_HUE = 1;
    public static final int MODE_SATURATION = 2;
    public static final int MODE_VALUE = 3;
    public static final int MODE_ALPHA = 4;

    public interface OnValueChangeListener {
        void onValueChanged(float value, boolean fromUser);

        void onTrackingStopped(float value);
    }

    private static final int[] HUE_SPECTRUM = new int[]{
            0xFFFF0000,
            0xFFFFFF00,
            0xFF00FF00,
            0xFF00FFFF,
            0xFF0000FF,
            0xFFFF00FF,
            0xFFFF0000
    };

    private static final float[] HUE_POSITIONS = new float[]{
            0f, 1f / 6f, 2f / 6f, 3f / 6f, 4f / 6f, 5f / 6f, 1f
    };

    private static final float TRACK_HEIGHT_DP = 30f;
    private static final float SLIDER_VIEW_HEIGHT_DP = 36f;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();
    private final RectF strokeRect = new RectF();
    private final float[] hsvTmp = new float[3];

    private int mode = MODE_PREVIEW;
    private boolean showCheckerboard;
    private int previewColor = 0xFF989898;
    private float hue = 0f;
    private float sat = 0f;
    private float val = 0.6f;
    private int alpha = 255;
    private float value = 0f;
    private boolean pressed;
    private OnValueChangeListener listener;
    private LivingFlowEffects.FluidSpring thumbScaleSpring;
    private int lastHapticStep = Integer.MIN_VALUE;

    private LivingFlowEffects.FluidSpring ensureThumbSpring() {
        if (thumbScaleSpring == null) {
            thumbScaleSpring = LivingFlowEffects.FluidSpring.touchPress();
            thumbScaleSpring.setValue(1.0f);
            thumbScaleSpring.setListener(v -> invalidate(), null);
        }
        return thumbScaleSpring;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (thumbScaleSpring != null) {
            thumbScaleSpring.cancel();
        }
        super.onDetachedFromWindow();
    }

    // Cached shaders rebuilt only when geometry/theme/channel dependencies change
    private LinearGradient cachedGradient;
    private int lastW = -1;
    private int lastH = -1;
    private int lastMode = -1;
    private boolean lastRtl;
    private boolean lastGradientDark;
    private float lastGradHue = -1f;
    private float lastGradSat = -1f;
    private int lastGradRgb = Integer.MIN_VALUE;

    private BitmapShader checkerShader;
    private boolean lastCheckerDark;

    public CouiColorSliderView(Context context) {
        this(context, null);
    }

    public CouiColorSliderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CouiColorSliderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float d = getResources().getDisplayMetrics().density;
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(0.75f * d);

        shadowOuterPaint.setStyle(Paint.Style.FILL);
        shadowOuterPaint.setColor(0x10000000);

        shadowInnerPaint.setStyle(Paint.Style.FILL);
        shadowInnerPaint.setColor(0x18000000);

        thumbFillPaint.setStyle(Paint.Style.FILL);
        thumbFillPaint.setColor(Color.WHITE);

        thumbStrokePaint.setStyle(Paint.Style.STROKE);
        thumbStrokePaint.setStrokeWidth(0.75f * d);
        thumbStrokePaint.setColor(0x26000000);

        dotFillPaint.setStyle(Paint.Style.FILL);

        dotStrokePaint.setStyle(Paint.Style.STROKE);
        dotStrokePaint.setStrokeWidth(0.6f * d);
        dotStrokePaint.setColor(0x1F000000);
    }

    public void setMode(int mode) {
        if (this.mode != mode) {
            this.mode = mode;
            lastW = -1;
            requestLayout();
            invalidate();
        }
    }

    public void setShowCheckerboard(boolean showCheckerboard) {
        if (this.showCheckerboard != showCheckerboard) {
            this.showCheckerboard = showCheckerboard;
            invalidate();
        }
    }

    public void setPreviewColor(int argb) {
        if (this.previewColor != argb) {
            this.previewColor = argb;
            invalidate();
        }
    }

    public void setHsvContext(float hue, float sat, float val, int alpha) {
        this.hue = clampHue(hue);
        this.sat = clamp01(sat);
        this.val = clamp01(val);
        this.alpha = Math.max(0, Math.min(255, alpha));
        invalidate();
    }

    public void setValue(float value) {
        float clamped = clampModeValue(value);
        if (Math.abs(this.value - clamped) > 0.0001f) {
            this.value = clamped;
            invalidate();
        }
    }

    public void setOnValueChangeListener(OnValueChangeListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float d = getResources().getDisplayMetrics().density;
        float desiredDp = (mode == MODE_PREVIEW) ? TRACK_HEIGHT_DP : SLIDER_VIEW_HEIGHT_DP;
        int desiredH = Math.round(desiredDp * d);
        setMeasuredDimension(
                resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                resolveSize(desiredH, heightMeasureSpec));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || mode == MODE_PREVIEW) {
            return false;
        }
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return false;
        }
        float d = getResources().getDisplayMetrics().density;
        float r = Math.min(TRACK_HEIGHT_DP * 0.5f * d, Math.min(h * 0.5f, w * 0.25f));
        float travel = Math.max(1f, w - 2f * r);
        boolean isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                ViewParent parent = getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
                pressed = true;
                updateValueFromTouchX(event.getX(), r, travel, isRtl);
                lastHapticStep = Math.round(value * 20f);
                if (ConfigStore.isLivingFlowUi(getContext())) {
                    ensureThumbSpring().animateTo(1.12f);
                    ColorOsLivingFlowEngine.performHaptic(this, ColorOsLivingFlowEngine.HapticKind.TICK);
                } else {
                    ensureThumbSpring().setValue(1.07f);
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                ViewParent parent = getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
                updateValueFromTouchX(event.getX(), r, travel, isRtl);
                return true;
            }
            case MotionEvent.ACTION_UP: {
                pressed = false;
                if (ConfigStore.isLivingFlowUi(getContext())) {
                    ensureThumbSpring().animateTo(1.0f);
                } else {
                    ensureThumbSpring().setValue(1.0f);
                }
                invalidate();
                if (listener != null) {
                    listener.onTrackingStopped(value);
                }
                performClick();
                return true;
            }
            case MotionEvent.ACTION_CANCEL: {
                pressed = false;
                if (ConfigStore.isLivingFlowUi(getContext())) {
                    ensureThumbSpring().animateTo(1.0f);
                } else {
                    ensureThumbSpring().setValue(1.0f);
                }
                invalidate();
                if (listener != null) {
                    listener.onTrackingStopped(value);
                }
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void updateValueFromTouchX(float x, float r, float travel, boolean isRtl) {
        float frac = clamp01((x - r) / travel);
        if (isRtl) {
            frac = 1f - frac;
        }
        float newValue = (mode == MODE_HUE) ? (frac * 360f) : frac;
        if (Math.abs(this.value - newValue) > 0.0001f) {
            this.value = newValue;
            invalidate();
            if (listener != null) {
                listener.onValueChanged(this.value, true);
            }
        } else {
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        float d = getResources().getDisplayMetrics().density;
        float trackH = Math.min(TRACK_HEIGHT_DP * d, h);
        float r = Math.min(trackH * 0.5f, w * 0.25f);
        float cy = h * 0.5f;
        float top = cy - trackH * 0.5f;
        float bottom = cy + trackH * 0.5f;
        trackRect.set(0f, top, w, bottom);

        boolean dark = ConfigStore.isDarkThemeActive(getContext());
        boolean isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;

        if (mode == MODE_ALPHA || (mode == MODE_PREVIEW && showCheckerboard)) {
            ensureCheckerShader(d, dark);
            canvas.drawRoundRect(trackRect, r, r, checkerPaint);
        }

        if (mode == MODE_PREVIEW) {
            trackPaint.setShader(null);
            trackPaint.setColor(previewColor);
        } else {
            ensureTrackGradient(w, h, r, isRtl, dark);
            trackPaint.setShader(cachedGradient);
            trackPaint.setColor(Color.WHITE);
        }
        canvas.drawRoundRect(trackRect, r, r, trackPaint);

        // Crisp hairline border so white or black endpoints stay separated from card background
        float strokeInset = borderPaint.getStrokeWidth() * 0.5f;
        strokeRect.set(
                trackRect.left + strokeInset,
                trackRect.top + strokeInset,
                trackRect.right - strokeInset,
                trackRect.bottom - strokeInset);
        borderPaint.setColor(dark ? 0x24FFFFFF : 0x16000000);
        float strokeR = Math.max(0f, r - strokeInset);
        canvas.drawRoundRect(strokeRect, strokeR, strokeR, borderPaint);

        if (mode == MODE_PREVIEW) {
            return;
        }

        float frac = (mode == MODE_HUE) ? clamp01(value / 360f) : clamp01(value);
        float travel = Math.max(0f, w - 2f * r);
        float cx = isRtl ? ((w - r) - frac * travel) : (r + frac * travel);

        if (pressed && ConfigStore.isLivingFlowUi(getContext())) {
            int step = Math.round(value * 20f);
            if (step != lastHapticStep) {
                lastHapticStep = step;
                ColorOsLivingFlowEngine.performHaptic(this, ColorOsLivingFlowEngine.HapticKind.TICK);
            }
        }
        float baseOuterR = Math.max(4f * d, r - 2.5f * d);
        float outerR = baseOuterR * ensureThumbSpring().getValue();

        // Soft ambient drop shadow beneath white donut thumb
        canvas.drawCircle(cx, cy + 0.8f * d, outerR + 1.6f * d, shadowOuterPaint);
        canvas.drawCircle(cx, cy + 0.5f * d, outerR + 0.8f * d, shadowInnerPaint);

        canvas.drawCircle(cx, cy, outerR, thumbFillPaint);
        canvas.drawCircle(cx, cy, outerR, thumbStrokePaint);

        float dotR = outerR * 0.42f;
        dotFillPaint.setColor(computeThumbDotColor());
        canvas.drawCircle(cx, cy, dotR, dotFillPaint);
        canvas.drawCircle(cx, cy, dotR, dotStrokePaint);
    }

    private void ensureCheckerShader(float d, boolean dark) {
        if (checkerShader != null && lastCheckerDark == dark) {
            return;
        }
        lastCheckerDark = dark;
        int sq = Math.max(4, Math.round(5f * d));
        int tile = sq * 2;
        Bitmap bmp = Bitmap.createBitmap(tile, tile, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint();
        int cLight = dark ? 0xFF3A3A3E : 0xFFFFFFFF;
        int cDark = dark ? 0xFF242428 : 0xFFD4D4DA;
        p.setColor(cLight);
        c.drawRect(0, 0, sq, sq, p);
        c.drawRect(sq, sq, tile, tile, p);
        p.setColor(cDark);
        c.drawRect(sq, 0, tile, sq, p);
        c.drawRect(0, sq, sq, tile, p);
        checkerShader = new BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        checkerPaint.setShader(checkerShader);
    }

    private void ensureTrackGradient(int w, int h, float r, boolean isRtl, boolean dark) {
        int rgb = previewColor & 0x00FFFFFF;
        boolean sameGeo = (w == lastW && h == lastH && isRtl == lastRtl
                && mode == lastMode && dark == lastGradientDark);
        boolean sameDep = true;
        if (mode == MODE_SATURATION) {
            sameDep = Math.abs(hue - lastGradHue) < 0.001f;
        } else if (mode == MODE_VALUE) {
            sameDep = Math.abs(hue - lastGradHue) < 0.001f && Math.abs(sat - lastGradSat) < 0.0005f;
        } else if (mode == MODE_ALPHA) {
            sameDep = (rgb == lastGradRgb);
        }
        if (cachedGradient != null && sameGeo && sameDep) {
            return;
        }

        lastW = w;
        lastH = h;
        lastRtl = isRtl;
        lastMode = mode;
        lastGradientDark = dark;
        lastGradHue = hue;
        lastGradSat = sat;
        lastGradRgb = rgb;

        float x0 = isRtl ? (w - r) : r;
        float x1 = isRtl ? r : (w - r);

        if (mode == MODE_HUE) {
            cachedGradient = new LinearGradient(
                    x0, 0f, x1, 0f, HUE_SPECTRUM, HUE_POSITIONS, Shader.TileMode.CLAMP);
        } else if (mode == MODE_SATURATION) {
            hsvTmp[0] = hue;
            hsvTmp[1] = 1f;
            hsvTmp[2] = 1f;
            int endColor = Color.HSVToColor(0xFF, hsvTmp);
            cachedGradient = new LinearGradient(
                    x0, 0f, x1, 0f, Color.WHITE, endColor, Shader.TileMode.CLAMP);
        } else if (mode == MODE_VALUE) {
            hsvTmp[0] = hue;
            hsvTmp[1] = sat;
            hsvTmp[2] = 1f;
            int endColor = Color.HSVToColor(0xFF, hsvTmp);
            cachedGradient = new LinearGradient(
                    x0, 0f, x1, 0f, Color.BLACK, endColor, Shader.TileMode.CLAMP);
        } else {
            cachedGradient = new LinearGradient(
                    x0, 0f, x1, 0f, rgb, 0xFF000000 | rgb, Shader.TileMode.CLAMP);
        }
    }

    private int computeThumbDotColor() {
        if (mode == MODE_HUE) {
            hsvTmp[0] = clampHue(value);
            hsvTmp[1] = 1f;
            hsvTmp[2] = 1f;
            return Color.HSVToColor(0xFF, hsvTmp);
        }
        if (mode == MODE_SATURATION) {
            hsvTmp[0] = hue;
            hsvTmp[1] = clamp01(value);
            hsvTmp[2] = 1f;
            return Color.HSVToColor(0xFF, hsvTmp);
        }
        if (mode == MODE_VALUE) {
            hsvTmp[0] = hue;
            hsvTmp[1] = sat;
            hsvTmp[2] = clamp01(value);
            return Color.HSVToColor(0xFF, hsvTmp);
        }
        if (mode == MODE_ALPHA) {
            int baseR = 0xE0;
            int baseG = 0xE0;
            int baseB = 0xE4;
            float af = clamp01(value);
            int rC = Math.round(baseR + (Color.red(previewColor) - baseR) * af);
            int gC = Math.round(baseG + (Color.green(previewColor) - baseG) * af);
            int bC = Math.round(baseB + (Color.blue(previewColor) - baseB) * af);
            return Color.rgb(rC, gC, bC);
        }
        return previewColor;
    }

    private float clampModeValue(float v) {
        if (mode == MODE_HUE) {
            return clampHue(v);
        }
        return clamp01(v);
    }

    private static float clampHue(float h) {
        if (Float.isNaN(h)) return 0f;
        return Math.max(0f, Math.min(360f, h));
    }

    private static float clamp01(float v) {
        if (Float.isNaN(v)) return 0f;
        return Math.max(0f, Math.min(1f, v));
    }
}
