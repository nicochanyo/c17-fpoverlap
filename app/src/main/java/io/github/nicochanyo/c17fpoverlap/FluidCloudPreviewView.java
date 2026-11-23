package io.github.nicochanyo.c17fpoverlap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Live preview card illustrating both the Expanded Dynamic Island (including top
 * black-gradient removal, native glass glow, and frosted tinting) and the
 * Lockscreen Fluid Cloud pill.
 */
public class FluidCloudPreviewView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bgRect = new RectF();
    private final RectF islandRect = new RectF();
    private final RectF capsuleRect = new RectF();
    private final RectF subRect = new RectF();
    private final Path clipPath = new Path();
    private final Path islandClip = new Path();
    private final Path capsuleClip = new Path();

    private Shader bgShader;

    private boolean fcEnabled = true;
    private int fcOpacity = ConfigStore.DEF_FC_OPACITY;
    private int fcIslandOpacity = ConfigStore.DEF_FC_ISLAND_OPACITY;
    private int fcBlur = ConfigStore.DEF_FC_BLUR;
    private int fcBottomGlow = ConfigStore.DEF_FC_BOTTOM_GLOW;
    private int fcBottomGlowChroma = ConfigStore.DEF_FC_BOTTOM_GLOW_CHROMA;
    private int fcSingleGlow = ConfigStore.DEF_FC_BOTTOM_GLOW;
    private int fcSingleGlowChroma = ConfigStore.DEF_FC_BOTTOM_GLOW_CHROMA;
    private int fcColor = ConfigStore.DEF_FC_COLOR;
    private boolean fcRemoveIslandMask = true;
    private boolean fcMatchCard = true;
    private boolean fcMaskFollowMedia = false;
    private int fcMaskBrightness = ConfigStore.DEF_FC_MASK_BRIGHTNESS;
    private int fcMaskChroma = ConfigStore.DEF_FC_MASK_CHROMA;
    private int fcMaskStrength = ConfigStore.DEF_FC_MASK_STRENGTH;
    private boolean fcNonMediaMaskEnabled = false;
    private int fcNonMediaMaskColor = ConfigStore.DEF_NON_MEDIA_MASK_COLOR;
    private int fcNonMediaMaskOpacity = ConfigStore.DEF_FC_NON_MEDIA_MASK_OPACITY;
    private int fcNonMediaFollowMaskOpacity = ConfigStore.DEF_FC_NON_MEDIA_FOLLOW_MASK_OPACITY;
    private int fcMaskGradStart = ConfigStore.DEF_FC_MASK_GRAD_START;
    private int fcMaskGradEnd = ConfigStore.DEF_FC_MASK_GRAD_END;
    private int fcMaskGradMid = ConfigStore.DEF_FC_MASK_GRAD_MID;
    private int fcMaskGradStrength = ConfigStore.DEF_FC_MASK_GRAD_STRENGTH;

    public FluidCloudPreviewView(Context context) {
        super(context);
    }

    public FluidCloudPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FluidCloudPreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void updateParams(boolean enabled, int lockOpacity, int islandOpacity,
                             int blur, int bottomGlow, int bottomGlowChroma,
                             int singleGlow, int singleGlowChroma, int color,
                             boolean removeIslandMask, boolean matchCard,
                             boolean maskFollowMedia, int maskBrightness, int maskChroma,
                             int maskStrength, boolean nonMediaMaskEnabled, int nonMediaMaskColor,
                             int nonMediaMaskOpacity, int nonMediaFollowMaskOpacity,
                             int maskGradStart, int maskGradEnd, int maskGradMid,
                             int maskGradStrength) {
        this.fcEnabled = enabled;
        this.fcOpacity = ConfigStore.clampOpacity(lockOpacity);
        this.fcIslandOpacity = ConfigStore.clampOpacity(islandOpacity);
        this.fcBlur = ConfigStore.clampBlur(blur);
        this.fcBottomGlow = ConfigStore.clampBottomGlow(bottomGlow);
        this.fcBottomGlowChroma = ConfigStore.clampBottomGlowChroma(bottomGlowChroma);
        this.fcSingleGlow = ConfigStore.clampBottomGlow(singleGlow);
        this.fcSingleGlowChroma = ConfigStore.clampBottomGlowChroma(singleGlowChroma);
        this.fcColor = ConfigStore.normalizeColor(color);
        this.fcRemoveIslandMask = removeIslandMask;
        this.fcMatchCard = matchCard;
        this.fcMaskFollowMedia = maskFollowMedia;
        this.fcMaskBrightness = ConfigStore.clampMaskPercent(maskBrightness);
        this.fcMaskChroma = ConfigStore.clampMaskPercent(maskChroma);
        this.fcMaskStrength = ConfigStore.clampMaskStrength(maskStrength);
        this.fcNonMediaMaskEnabled = nonMediaMaskEnabled;
        this.fcNonMediaMaskColor = ConfigStore.normalizePickerColor(nonMediaMaskColor);
        this.fcNonMediaMaskOpacity = ConfigStore.clampMaskStrength(nonMediaMaskOpacity);
        this.fcNonMediaFollowMaskOpacity = ConfigStore.clampMaskStrength(nonMediaFollowMaskOpacity);
        int[] gradNorm = ConfigStore.normalizeGradientPct(maskGradStart, maskGradEnd, maskGradMid);
        this.fcMaskGradStart = gradNorm[0];
        this.fcMaskGradEnd = gradNorm[1];
        this.fcMaskGradMid = gradNorm[2];
        this.fcMaskGradStrength = ConfigStore.clampMaskStrength(maskGradStrength);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) {
            return;
        }
        bgShader = new LinearGradient(
                0f, 0f, w, h,
                new int[]{0xFF235CFF, 0xFF5D5CE6, 0xFFE26BA5},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        float d = getResources().getDisplayMetrics().density;
        float outerR = 16f * d;

        bgRect.set(0f, 0f, w, h);
        clipPath.reset();
        clipPath.addRoundRect(bgRect, outerR, outerR, Path.Direction.CW);

        canvas.save();
        canvas.clipPath(clipPath);

        // Simulated colorful ColorOS lockscreen wallpaper
        paint.setShader(bgShader);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        canvas.drawRect(bgRect, paint);
        paint.setShader(null);

        // Soft ambient bokeh blobs behind cards so blur & transparency changes are unmistakable
        float blurScale = fcEnabled ? (fcBlur / 100.0f) : 1.0f;
        int blobAlpha = Math.max(55, Math.min(200, Math.round(170 - blurScale * 30f)));
        paint.setColor(Color.argb(blobAlpha, 255, 226, 130));
        canvas.drawCircle(w * 0.18f, h * 0.28f, h * (0.20f + 0.04f * blurScale), paint);
        paint.setColor(Color.argb(blobAlpha, 90, 232, 255));
        canvas.drawCircle(w * 0.78f, h * 0.62f, h * (0.24f + 0.05f * blurScale), paint);
        paint.setColor(Color.argb(Math.round(blobAlpha * 0.7f), 255, 140, 200));
        canvas.drawCircle(w * 0.48f, h * 0.45f, h * 0.16f, paint);

        float lockOp = fcEnabled ? (fcOpacity / 100.0f) : 1.0f;
        float islandOp = fcEnabled ? (fcIslandOpacity / 100.0f) : 1.0f;
        float gradStrengthScale = fcEnabled ? (fcMaskGradStrength / 100.0f) : 1.0f;
        int effGradStart = fcEnabled ? fcMaskGradStart : 0;
        int effGradEnd = fcEnabled ? fcMaskGradEnd : 80;
        int effGradMid = fcEnabled ? fcMaskGradMid : 50;
        float maskStrengthScale = (fcEnabled ? (fcMaskStrength / 100.0f) : 1.0f) * gradStrengthScale;
        int glowBoostPct = fcEnabled ? fcBottomGlow : 0;
        int glowChromaPct = fcEnabled ? fcBottomGlowChroma : 0;
        int singleGlowBoostPct = fcEnabled ? fcSingleGlow : 0;
        int singleGlowChromaPct = fcEnabled ? fcSingleGlowChroma : 0;
        int baseColor = (fcEnabled && fcColor != 0) ? fcColor : 0xFF1A1C22;
        int r = Color.red(baseColor);
        int g = Color.green(baseColor);
        int b = Color.blue(baseColor);

        // 1. Top Expanded Dynamic Island card preview (uses islandOp)
        float padH = 14f * d;
        float topY = 12f * d;
        float islandH = h * 0.48f;
        islandRect.set(padH, topY, w - padH, topY + islandH);
        float cardR = 16f * d;

        drawFrostedCard(canvas, islandRect, cardR, r, g, b, islandOp, blurScale);

        // Dark or media-tinted top-to-transparent gradient overlay (shared smooth template)
        if (!fcEnabled || (!fcRemoveIslandMask && maskStrengthScale > 0.001f)) {
            boolean mediaMask = fcEnabled && fcMaskFollowMedia;
            int topBlackA = fcEnabled
                    ? Math.min(245, Math.round((mediaMask ? 205f : 195f) * maskStrengthScale))
                    : 210;
            int maskColor = mediaMask ? computePreviewMaskRgb(0xFFE9546B, fcMaskBrightness, fcMaskChroma) : 0;
            ConfigStore.SmoothGradientSpec islandSpec = ConfigStore.buildSmoothGradient(
                    maskColor, topBlackA, effGradStart, effGradEnd, effGradMid);
            islandClip.reset();
            islandClip.addRoundRect(islandRect, cardR, cardR, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(islandClip);
            LinearGradient lg = new LinearGradient(
                    0f, islandRect.top, 0f, islandRect.bottom,
                    islandSpec.colors,
                    islandSpec.positions,
                    Shader.TileMode.CLAMP);
            paint.setDither(true);
            paint.setShader(lg);
            canvas.drawRect(islandRect, paint);
            paint.setShader(null);
            paint.setDither(false);
            canvas.restore();
        }

        int islandGlowColor = (fcEnabled && fcColor != 0) ? fcColor : 0xFFE9546B;
        drawBottomBrightening(canvas, islandRect, islandClip, cardR, glowBoostPct, glowChromaPct, islandGlowColor);

        // Mini Dynamic Island card UI skeleton
        float artSize = islandH - 18f * d;
        subRect.set(islandRect.left + 10f * d, islandRect.top + 9f * d,
                islandRect.left + 10f * d + artSize, islandRect.top + 9f * d + artSize);
        paint.setColor(0xFFE9546B);
        canvas.drawRoundRect(subRect, 9f * d, 9f * d, paint);

        paint.setColor(0xF0FFFFFF);
        float textLeft = subRect.right + 10f * d;
        float titleTop = islandRect.top + 14f * d;
        subRect.set(textLeft, titleTop, textLeft + 104f * d, titleTop + 7f * d);
        canvas.drawRoundRect(subRect, 3.5f * d, 3.5f * d, paint);

        paint.setColor(0x9EFFFFFF);
        subRect.set(textLeft, titleTop + 13f * d, textLeft + 68f * d, titleTop + 18f * d);
        canvas.drawRoundRect(subRect, 2.5f * d, 2.5f * d, paint);

        // Small play pill on right side of expanded card
        float pillR = 11f * d;
        float pillCx = islandRect.right - 22f * d;
        float pillCy = islandRect.centerY();
        paint.setColor(0x40FFFFFF);
        canvas.drawCircle(pillCx, pillCy, pillR, paint);
        paint.setColor(0xF0FFFFFF);
        canvas.drawRoundRect(
                pillCx - 3.5f * d, pillCy - 4.5f * d,
                pillCx - 1f * d, pillCy + 4.5f * d, 1f * d, 1f * d, paint);
        canvas.drawRoundRect(
                pillCx + 1f * d, pillCy - 4.5f * d,
                pillCx + 3.5f * d, pillCy + 4.5f * d, 1f * d, 1f * d, paint);

        // 2. Bottom Lockscreen Capsule Fluid Cloud pill preview (uses lockOp)
        float capW = Math.min(w - 2f * padH, 168f * d);
        float capH = 34f * d;
        float capLeft = (w - capW) * 0.5f;
        float capTop = h - 12f * d - capH;
        capsuleRect.set(capLeft, capTop, capLeft + capW, capTop + capH);
        float capR = capH * 0.5f;
        drawFrostedCard(canvas, capsuleRect, capR, r, g, b, lockOp, blurScale);

        boolean nonMediaTint = fcEnabled && fcNonMediaMaskEnabled;
        int effNonMediaOp = nonMediaTint ? fcNonMediaFollowMaskOpacity : fcNonMediaMaskOpacity;
        float nonMediaOpacityScale = fcEnabled ? ((effNonMediaOp / 100.0f) * gradStrengthScale) : 1.0f;
        if (!fcEnabled || (!fcMatchCard && nonMediaOpacityScale > 0.001f)) {
            int scrimA = fcEnabled ? Math.min(235, Math.round(115f * nonMediaOpacityScale)) : 115;
            int maskColor = nonMediaTint
                    ? computePreviewMaskRgb(0xFFE9546B, fcMaskBrightness, fcMaskChroma)
                    : 0;
            if (scrimA > 0) {
                ConfigStore.SmoothGradientSpec capSpec = ConfigStore.buildSmoothGradient(
                        maskColor, scrimA, effGradStart, effGradEnd, effGradMid);
                capsuleClip.reset();
                capsuleClip.addRoundRect(capsuleRect, capR, capR, Path.Direction.CW);
                canvas.save();
                canvas.clipPath(capsuleClip);
                LinearGradient capGrad = new LinearGradient(
                        0f, capsuleRect.top, 0f, capsuleRect.bottom,
                        capSpec.colors,
                        capSpec.positions,
                        Shader.TileMode.CLAMP);
                paint.setStyle(Paint.Style.FILL);
                paint.setDither(true);
                paint.setShader(capGrad);
                canvas.drawRect(capsuleRect, paint);
                paint.setShader(null);
                paint.setDither(false);
                canvas.restore();
            }
        }

        int lockGlowColor = (fcEnabled && fcNonMediaMaskEnabled)
                ? 0xFFE9546B
                : ((fcEnabled && fcColor != 0) ? fcColor : 0xFFFFFFFF);
        drawBottomBrightening(canvas, capsuleRect, capsuleClip, capR, singleGlowBoostPct, singleGlowChromaPct, lockGlowColor);

        float dotSize = 18f * d;
        subRect.set(capsuleRect.left + 8f * d, capsuleRect.centerY() - dotSize * 0.5f,
                capsuleRect.left + 8f * d + dotSize, capsuleRect.centerY() + dotSize * 0.5f);
        paint.setColor(0xFF38D49C);
        canvas.drawRoundRect(subRect, dotSize * 0.5f, dotSize * 0.5f, paint);

        paint.setColor(0xEBFFFFFF);
        subRect.set(subRect.right + 8f * d, capsuleRect.centerY() - 3f * d,
                capsuleRect.right - 14f * d, capsuleRect.centerY() + 3f * d);
        canvas.drawRoundRect(subRect, 3f * d, 3f * d, paint);

        canvas.restore();
    }

    private void drawFrostedCard(Canvas canvas, RectF rect, float radius,
                                 int r, int g, int b, float opacity, float blurScale) {
        // Frosted inner veil (shows blur presence even when opacity is 0%)
        int frostAlpha = Math.round(14 + Math.min(3.0f, blurScale) * 14f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(frostAlpha, 255, 255, 255));
        canvas.drawRoundRect(rect, radius, radius, paint);

        // Color & opacity body
        int bodyAlpha = Math.max(0, Math.min(242, Math.round(218f * opacity)));
        paint.setColor(Color.argb(bodyAlpha, r, g, b));
        canvas.drawRoundRect(rect, radius, radius, paint);

        // Crisp COUI glass rim stroke
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1.1f * getResources().getDisplayMetrics().density);
        strokePaint.setColor(0x4DFFFFFF);
        canvas.drawRoundRect(rect, radius, radius, strokePaint);
    }

    private static int clampByte(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }

    private static int computePreviewMaskRgb(int rawColor, int brightPct, int chromaPct) {
        float[] hsv = new float[3];
        Color.colorToHSV(0xFF000000 | (rawColor & 0x00FFFFFF), hsv);
        float b = Math.max(0f, Math.min(1f, brightPct / 100.0f));
        float c = Math.max(0f, Math.min(1f, chromaPct / 100.0f));
        hsv[1] = c;
        hsv[2] = 0.10f + 0.82f * b;
        return Color.HSVToColor(hsv);
    }

    private void drawBottomBrightening(Canvas canvas, RectF rect, Path clip, float radius,
                                       int glowBoostPct, int glowChromaPct, int accentColor) {
        if (rect.width() <= 0f || rect.height() <= 0f) {
            return;
        }
        float dp = getResources().getDisplayMetrics().density;
        float brightBoost = Math.max(0f, Math.min(1f, glowBoostPct / (float) ConfigStore.MAX_FC_BOTTOM_GLOW));
        float chromaBoost = Math.max(0f, Math.min(1f, glowChromaPct / (float) ConfigStore.MAX_FC_BOTTOM_GLOW_CHROMA));
        int sheenPeakA = clampByte(28f + 98f * (float) Math.pow(brightBoost, 0.80));
        int rimA = clampByte(56f + 124f * (float) Math.pow(brightBoost, 0.75));
        if (sheenPeakA <= 0 && rimA <= 0) {
            return;
        }
        float colorMix = ((accentColor & 0x00FFFFFF) == 0x00FFFFFF)
                ? 0f
                : (0.22f + 0.72f * (float) Math.pow(chromaBoost, 1.05));
        int hiR = clampByte(255f + (Color.red(accentColor) - 255f) * colorMix);
        int hiG = clampByte(255f + (Color.green(accentColor) - 255f) * colorMix);
        int hiB = clampByte(255f + (Color.blue(accentColor) - 255f) * colorMix);
        int rimR = clampByte(255f + (hiR - 255f) * 0.84f);
        int rimG = clampByte(255f + (hiG - 255f) * 0.84f);
        int rimB = clampByte(255f + (hiB - 255f) * 0.84f);

        clip.reset();
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);

        // Soft exponential bottom glass sheen
        if (sheenPeakA > 0) {
            float bandH = Math.min(rect.height() * 0.44f, Math.max(rect.height() * 0.28f, 22f * dp));
            float y0 = rect.bottom - bandH;
            LinearGradient sheen = new LinearGradient(
                    0f, y0, 0f, rect.bottom,
                    new int[]{
                            Color.argb(0, hiR, hiG, hiB),
                            Color.argb(clampByte(sheenPeakA * 0.14f), hiR, hiG, hiB),
                            Color.argb(clampByte(sheenPeakA * 0.48f), hiR, hiG, hiB),
                            Color.argb(sheenPeakA, hiR, hiG, hiB)
                    },
                    new float[]{0f, 0.45f, 0.78f, 1f},
                    Shader.TileMode.CLAMP);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(sheen);
            canvas.drawRect(rect.left, y0, rect.right, rect.bottom, paint);
            paint.setShader(null);
        }

        // Delicate 1dp bottom edge rim fading smoothly upward
        if (rimA > 0) {
            float rimSpan = rect.height() * 0.55f;
            LinearGradient rimGrad = new LinearGradient(
                    0f, rect.bottom - rimSpan, 0f, rect.bottom,
                    new int[]{
                            Color.argb(0, rimR, rimG, rimB),
                            Color.argb(clampByte(rimA * 0.35f), rimR, rimG, rimB),
                            Color.argb(rimA, rimR, rimG, rimB)
                    },
                    new float[]{0f, 0.58f, 1f},
                    Shader.TileMode.CLAMP);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(1.0f * dp * 2f);
            strokePaint.setShader(rimGrad);
            canvas.drawRoundRect(rect, radius, radius, strokePaint);
            strokePaint.setShader(null);
        }

        canvas.restore();
    }
}
