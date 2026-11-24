package io.github.nicochanyo.c17fpoverlap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RecordingCanvas;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

/**
 * Hardware-accelerated progressive Gaussian blur + vertical surface scrim overlay
 * for the collapsing top header. Captures the scrolling content beneath it into a
 * RenderNode, renders dual soft/strong RenderEffect Gaussian bands with alpha gradient
 * masks (API 31+), and falls back to downsampled software blur on older APIs.
 */
public class TopGradientBlurView extends View {
    private static final float SOFT_BLUR_DP = 4.5f;
    private static final float STRONG_BLUR_DP = 11.5f;
    private static final float BLUR_PAD_DP = 32f;
    private static final int FALLBACK_DOWNSAMPLE = 8;

    private View targetView;
    private boolean isCapturing;
    private int statusBarTopPx;
    private int bgColor;
    private boolean translucentBackdrop = false;
    private boolean dynamicBackdrop = false;

    // Cached geometry/theme signature for RenderEffects and Shaders
    private int lastW = -1;
    private int lastH = -1;
    private int lastStatusTop = -1;
    private int lastBgColor;
    private boolean lastTranslucent = false;
    private boolean lastDynamicBackdrop = false;

    // API 31+ GPU RenderNodes
    private RenderNode sourceNode;
    private RenderNode softBlurNode;
    private RenderNode strongBlurNode;
    private RenderEffect softMaskedEffect;
    private RenderEffect strongMaskedEffect;

    // Top surface gradient scrim & software fallback paints
    private final Paint scrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fallbackBitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint fallbackMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap fallbackCaptureBmp;
    private Canvas fallbackCaptureCanvas;

    public TopGradientBlurView(Context context) {
        super(context);
        init();
    }

    public TopGradientBlurView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TopGradientBlurView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        scrimPaint.setDither(true);
        fallbackMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        refreshThemeSurfaceColor();
    }

    public void setBlurTargetView(View target) {
        this.targetView = target;
        invalidate();
    }

    public void setStatusBarTopPx(int topPx) {
        int clamped = Math.max(0, topPx);
        if (this.statusBarTopPx != clamped) {
            this.statusBarTopPx = clamped;
            lastW = -1;
            invalidate();
        }
    }

    public void refreshThemeSurfaceColor() {
        try {
            int resolved = ConfigStore.resolveThemeColor(getContext(), R.attr.couiBg, R.color.coui_bg);
            if (this.bgColor != resolved || this.translucentBackdrop) {
                this.bgColor = resolved;
                this.translucentBackdrop = false;
                lastW = -1;
                invalidate();
            }
        } catch (Throwable ignored) {
        }
    }

    public void setSurfaceColor(int color) {
        boolean isTranslucent = Color.alpha(color) == 0;
        int nextBg = isTranslucent ? (color & 0x00FFFFFF) : color;
        if (this.translucentBackdrop != isTranslucent || this.bgColor != nextBg) {
            this.translucentBackdrop = isTranslucent;
            this.bgColor = nextBg;
            lastW = -1;
            invalidate();
        }
    }

    public void setDynamicBackdrop(boolean dynamic) {
        if (this.dynamicBackdrop != dynamic) {
            this.dynamicBackdrop = dynamic;
            lastW = -1;
            invalidate();
        }
    }

    public boolean isCapturingTarget() {
        return isCapturing;
    }

    public void notifyTargetInvalidated() {
        if (!isCapturing) {
            invalidate();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (fallbackCaptureBmp != null && !fallbackCaptureBmp.isRecycled()) {
            fallbackCaptureBmp.recycle();
        }
        fallbackCaptureBmp = null;
        fallbackCaptureCanvas = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isCapturing || targetView == null) {
            return;
        }
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        ensureShadersAndEffects(w, h);

        if (Build.VERSION.SDK_INT >= 31 && canvas.isHardwareAccelerated()) {
            drawHardwareProgressiveBlur(canvas, w, h);
        } else {
            drawSoftwareFallbackBlur(canvas, w, h);
        }
    }

    private void ensureShadersAndEffects(int w, int h) {
        if (w == lastW && h == lastH && statusBarTopPx == lastStatusTop
                && bgColor == lastBgColor && translucentBackdrop == lastTranslucent
                && dynamicBackdrop == lastDynamicBackdrop) {
            return;
        }
        lastW = w;
        lastH = h;
        lastStatusTop = statusBarTopPx;
        lastBgColor = bgColor;
        lastTranslucent = translucentBackdrop;
        lastDynamicBackdrop = dynamicBackdrop;

        float d = getResources().getDisplayMetrics().density;
        boolean compactEmbedded = (statusBarTopPx <= 0);
        float fh = h;
        float fadeBase = compactEmbedded
                ? 0.30f
                : Math.min(0.46f, (statusBarTopPx + 16f * d) / fh);

        if (dynamicBackdrop) {
            scrimPaint.setShader(null);
            scrimPaint.setColor(Color.TRANSPARENT);
        } else {
            int topAlpha = compactEmbedded
                    ? (translucentBackdrop ? 0xCC : 0xFA)
                    : (translucentBackdrop ? 0x78 : 0xAC);
            float solidFrac = compactEmbedded
                    ? 0.26f
                    : Math.min(0.42f, (statusBarTopPx + 10f * d) / fh);
            int rgb = bgColor & 0x00FFFFFF;
            final int scrimSteps = 10;
            float[] scrimStops = new float[2 + scrimSteps];
            int[] scrimColors = new int[2 + scrimSteps];
            int startAlpha = Math.round(topAlpha * 0.90f);
            scrimStops[0] = 0f;
            scrimColors[0] = (topAlpha << 24) | rgb;
            scrimStops[1] = clampPos(solidFrac);
            scrimColors[1] = (startAlpha << 24) | rgb;
            for (int i = 1; i <= scrimSteps; i++) {
                float u = i / (float) scrimSteps;
                float pos = solidFrac + (1f - solidFrac) * u;
                int a = (i == scrimSteps)
                        ? 0
                        : Math.round(startAlpha * 0.5f * (1f + (float) Math.cos(Math.PI * Math.pow(u, 0.85))));
                scrimStops[1 + i] = ensureMonotonicStop(clampPos(pos), scrimStops[i]);
                scrimColors[1 + i] = (Math.max(0, Math.min(255, a)) << 24) | rgb;
            }
            scrimStops[scrimStops.length - 1] = 1f;
            scrimColors[scrimColors.length - 1] = rgb;

            scrimPaint.setColor(Color.BLACK);
            scrimPaint.setShader(new LinearGradient(0f, 0f, 0f, fh, scrimColors, scrimStops, Shader.TileMode.CLAMP));
        }

        if (Build.VERSION.SDK_INT >= 31) {
            float rSoft = Math.max(1f, SOFT_BLUR_DP * d);
            float rStrong = Math.max(1f, STRONG_BLUR_DP * d);

            float[] softStops;
            int[] softMaskColors;
            float[] strongStops;
            int[] strongMaskColors;
            if (dynamicBackdrop) {
                softStops = new float[] { 0f, 0.46f, 0.64f, 0.82f, 0.94f, 1f };
                softMaskColors = new int[] {
                        0x00FFFFFF, 0x00FFFFFF, 0x78FFFFFF, 0xFFFFFFFF, 0x70FFFFFF, 0x00FFFFFF
                };
                strongStops = new float[] { 0f, 0.36f, 0.54f, 0.72f, 0.88f, 1f };
                strongMaskColors = new int[] {
                        0x00FFFFFF, 0x00FFFFFF, 0xA8FFFFFF, 0xFFFFFFFF, 0x40FFFFFF, 0x00FFFFFF
                };
            } else {
                // Smooth proportional cosine feathering down to exact 0 alpha before y = h
                float strongStart = fadeBase;
                float strongEnd = strongStart + (1f - strongStart) * 0.72f;
                strongStops = buildCosineMaskStops(strongStart, strongEnd, 7);
                strongMaskColors = buildCosineMaskColors(10);

                float softStart = fadeBase + (1f - fadeBase) * 0.16f;
                float softEnd = 0.98f;
                softStops = buildCosineMaskStops(softStart, softEnd, 7);
                softMaskColors = buildCosineMaskColors(10);
            }
            LinearGradient softGrad = new LinearGradient(
                    0f, 0f, 0f, fh, softMaskColors, softStops, Shader.TileMode.CLAMP);
            softMaskedEffect = RenderEffect.createBlendModeEffect(
                    RenderEffect.createBlurEffect(rSoft, rSoft, Shader.TileMode.CLAMP),
                    RenderEffect.createShaderEffect(softGrad),
                    BlendMode.DST_IN);

            LinearGradient strongGrad = new LinearGradient(
                    0f, 0f, 0f, fh, strongMaskColors, strongStops, Shader.TileMode.CLAMP);
            strongMaskedEffect = RenderEffect.createBlendModeEffect(
                    RenderEffect.createBlurEffect(rStrong, rStrong, Shader.TileMode.CLAMP),
                    RenderEffect.createShaderEffect(strongGrad),
                    BlendMode.DST_IN);

            if (softBlurNode != null) {
                softBlurNode.setRenderEffect(softMaskedEffect);
            }
            if (strongBlurNode != null) {
                strongBlurNode.setRenderEffect(strongMaskedEffect);
            }
        } else {
            float[] fbStops = dynamicBackdrop
                    ? new float[] { 0f, 0.38f, 0.68f, 0.88f, 1f }
                    : new float[] { 0f, clampPos(fadeBase), clampPos(fadeBase + (1f - fadeBase) * 0.50f), 0.98f, 1f };
            int[] fbColors = dynamicBackdrop
                    ? new int[] { 0x00FFFFFF, 0x00FFFFFF, 0xD8FFFFFF, 0x60FFFFFF, 0x00FFFFFF }
                    : new int[] { 0xFFFFFFFF, 0xE6FFFFFF, 0x66FFFFFF, 0x00FFFFFF, 0x00FFFFFF };
            fallbackMaskPaint.setShader(new LinearGradient(0f, 0f, 0f, fh, fbColors, fbStops, Shader.TileMode.CLAMP));
        }
    }

    private void drawHardwareProgressiveBlur(Canvas canvas, int w, int h) {
        if (Build.VERSION.SDK_INT < 31) {
            return;
        }
        float d = getResources().getDisplayMetrics().density;
        int captureH = h + Math.round(BLUR_PAD_DP * d);

        if (sourceNode == null) {
            sourceNode = new RenderNode("C17FpTopBlurSource");
            softBlurNode = new RenderNode("C17FpTopBlurSoft");
            strongBlurNode = new RenderNode("C17FpTopBlurStrong");
            softBlurNode.setRenderEffect(softMaskedEffect);
            strongBlurNode.setRenderEffect(strongMaskedEffect);
        }

        sourceNode.setPosition(0, 0, w, captureH);
        sourceNode.setClipToBounds(true);

        boolean clearTransparent = dynamicBackdrop || translucentBackdrop;
        isCapturing = true;
        try {
            RecordingCanvas srcRc = sourceNode.beginRecording(w, captureH);
            if (clearTransparent) {
                srcRc.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            } else {
                srcRc.drawColor(bgColor);
            }
            float dx = targetView.getLeft() - getLeft() + targetView.getTranslationX() - targetView.getScrollX();
            float dy = targetView.getTop() - getTop() + targetView.getTranslationY() - targetView.getScrollY();
            if (dx != 0f || dy != 0f) {
                srcRc.translate(dx, dy);
            }
            targetView.draw(srcRc);
            sourceNode.endRecording();
        } catch (Throwable ignored) {
            isCapturing = false;
            if (!dynamicBackdrop) {
                canvas.drawRect(0f, 0f, w, h, scrimPaint);
            }
            return;
        } finally {
            isCapturing = false;
        }

        softBlurNode.setPosition(0, 0, w, captureH);
        softBlurNode.setClipToBounds(true);
        RecordingCanvas softRc = softBlurNode.beginRecording(w, captureH);
        softRc.drawRenderNode(sourceNode);
        softBlurNode.endRecording();

        strongBlurNode.setPosition(0, 0, w, captureH);
        strongBlurNode.setClipToBounds(true);
        RecordingCanvas strongRc = strongBlurNode.beginRecording(w, captureH);
        strongRc.drawRenderNode(sourceNode);
        strongBlurNode.endRecording();

        int sc = canvas.save();
        canvas.clipRect(0, 0, w, h);
        canvas.drawRenderNode(softBlurNode);
        canvas.drawRenderNode(strongBlurNode);
        if (!dynamicBackdrop) {
            canvas.drawRect(0f, 0f, w, h, scrimPaint);
        }
        canvas.restoreToCount(sc);
    }

    private void drawSoftwareFallbackBlur(Canvas canvas, int w, int h) {
        int sw = Math.max(1, w / FALLBACK_DOWNSAMPLE);
        int sh = Math.max(1, h / FALLBACK_DOWNSAMPLE);
        if (fallbackCaptureBmp == null || fallbackCaptureBmp.getWidth() != sw || fallbackCaptureBmp.getHeight() != sh) {
            if (fallbackCaptureBmp != null && !fallbackCaptureBmp.isRecycled()) {
                fallbackCaptureBmp.recycle();
            }
            fallbackCaptureBmp = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
            fallbackCaptureCanvas = new Canvas(fallbackCaptureBmp);
        }

        boolean clearTransparent = dynamicBackdrop || translucentBackdrop;
        isCapturing = true;
        try {
            fallbackCaptureCanvas.save();
            fallbackCaptureCanvas.scale(1f / FALLBACK_DOWNSAMPLE, 1f / FALLBACK_DOWNSAMPLE);
            if (clearTransparent) {
                fallbackCaptureCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            } else {
                fallbackCaptureCanvas.drawColor(bgColor);
            }
            float dx = targetView.getLeft() - getLeft() + targetView.getTranslationX() - targetView.getScrollX();
            float dy = targetView.getTop() - getTop() + targetView.getTranslationY() - targetView.getScrollY();
            if (dx != 0f || dy != 0f) {
                fallbackCaptureCanvas.translate(dx, dy);
            }
            targetView.draw(fallbackCaptureCanvas);
            fallbackCaptureCanvas.restore();
            boxBlurInPlace(fallbackCaptureBmp, 4);
        } catch (Throwable ignored) {
            isCapturing = false;
            if (!dynamicBackdrop) {
                canvas.drawRect(0f, 0f, w, h, scrimPaint);
            }
            return;
        } finally {
            isCapturing = false;
        }

        int layer = canvas.saveLayer(0f, 0f, w, h, null);
        canvas.save();
        canvas.scale((float) w / sw, (float) h / sh);
        canvas.drawBitmap(fallbackCaptureBmp, 0f, 0f, fallbackBitmapPaint);
        canvas.restore();
        canvas.drawRect(0f, 0f, w, h, fallbackMaskPaint);
        canvas.restoreToCount(layer);
        if (!dynamicBackdrop) {
            canvas.drawRect(0f, 0f, w, h, scrimPaint);
        }
    }

    private static void boxBlurInPlace(Bitmap bmp, int radius) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        if (w <= 1 || h <= 1) return;
        int[] src = new int[w * h];
        int[] tmp = new int[w * h];
        bmp.getPixels(src, 0, w, 0, 0, w, h);
        for (int pass = 0; pass < 2; pass++) {
            for (int y = 0; y < h; y++) {
                int row = y * w;
                for (int x = 0; x < w; x++) {
                    int r = 0, g = 0, b = 0, count = 0;
                    int x0 = Math.max(0, x - radius);
                    int x1 = Math.min(w - 1, x + radius);
                    for (int xi = x0; xi <= x1; xi++) {
                        int c = src[row + xi];
                        r += (c >> 16) & 0xFF;
                        g += (c >> 8) & 0xFF;
                        b += c & 0xFF;
                        count++;
                    }
                    tmp[row + x] = 0xFF000000 | ((r / count) << 16) | ((g / count) << 8) | (b / count);
                }
            }
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    int r = 0, g = 0, b = 0, count = 0;
                    int y0 = Math.max(0, y - radius);
                    int y1 = Math.min(h - 1, y + radius);
                    for (int yi = y0; yi <= y1; yi++) {
                        int c = tmp[yi * w + x];
                        r += (c >> 16) & 0xFF;
                        g += (c >> 8) & 0xFF;
                        b += c & 0xFF;
                        count++;
                    }
                    src[y * w + x] = 0xFF000000 | ((r / count) << 16) | ((g / count) << 8) | (b / count);
                }
            }
        }
        bmp.setPixels(src, 0, w, 0, 0, w, h);
    }

    private static float clampPos(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static float ensureMonotonicStop(float candidate, float prev) {
        return Math.max(prev, Math.min(1f, candidate));
    }

    private static float[] buildCosineMaskStops(float fadeStart, float fadeEnd, int fadeSteps) {
        float start = clampPos(fadeStart);
        float end = Math.max(start, Math.min(0.995f, fadeEnd));
        float[] stops = new float[2 + fadeSteps + 1];
        stops[0] = 0f;
        stops[1] = start;
        for (int i = 1; i <= fadeSteps; i++) {
            float u = i / (float) fadeSteps;
            stops[1 + i] = ensureMonotonicStop(start + (end - start) * u, stops[i]);
        }
        stops[stops.length - 1] = 1f;
        return stops;
    }

    private static int[] buildCosineMaskColors(int totalCount) {
        int[] colors = new int[totalCount];
        colors[0] = 0xFFFFFFFF;
        colors[1] = 0xFFFFFFFF;
        int fadeSteps = totalCount - 3;
        for (int i = 1; i <= fadeSteps; i++) {
            float u = i / (float) fadeSteps;
            int a = (i == fadeSteps)
                    ? 0
                    : Math.round(255f * 0.5f * (1f + (float) Math.cos(Math.PI * u)));
            colors[1 + i] = (Math.max(0, Math.min(255, a)) << 24) | 0x00FFFFFF;
        }
        colors[totalCount - 1] = 0x00FFFFFF;
        return colors;
    }
}
