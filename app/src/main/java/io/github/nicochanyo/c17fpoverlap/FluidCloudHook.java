package io.github.nicochanyo.c17fpoverlap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.api.XposedInterface;
import io.github.nicochanyo.c17fpoverlap.XposedCompat.MethodHookParam;
import io.github.nicochanyo.c17fpoverlap.XposedCompat.XC_MethodHook;

public final class FluidCloudHook {
    private static final String C_BLUR_CFG = "com.oplusos.systemui.common.blurability.BlurConfig";
    private static final String C_BLUR_MGR = "com.oplus.systemui.notification.blur.ViewBlurManager";
    private static final String C_CARD_TYPE = "com.oplus.systemui.notification.blur.ViewBlurManager$CardType";
    private static final String C_BLUR_MODE = "com.oplus.systemui.notification.row.material.NotificationBlurMode";
    private static final String C_PLATFORM_BLUR_MGR = "com.oplus.systemui.notification.blur.NotificationPlatFormBlurParamsManager";
    private static final String C_INNER_SHADOW_PARAMS = "com.oplus.posteffect.params.InnerShadowParams";
    private static final String C_STROKE_PARAMS = "com.oplus.posteffect.params.GradientStrokeLineParams";
    private static final String C_CAPSULE_ITEM = "com.oplus.systemui.notification.lockscreen.capsule.CapsuleItemView";
    private static final String C_CAPSULE_EAR = "com.oplus.systemui.notification.lockscreen.capsule.CapsuleEarView";
    private static final String C_CAPSULE_BG_VIEW = "com.oplus.systemui.notification.lockscreen.capsule.CapsuleBackgroundView";
    private static final String C_HELPER = "com.oplus.systemui.notification.lockscreen.capsule.CapsuleBackgroundHelper";
    private static final String C_SCRIM = "com.oplus.systemui.notification.lockscreen.capsule.CapsuleScrimView";
    private static final String C_GRAD_MASK_VIEW = "com.oplus.systemui.keyguard.gradientmask.view.GradientColorImageView";
    private static final String C_CUSTOM_ROW = "com.oplus.systemui.notification.row.NotificationBackgroundCustomRowHandler";
    private static final String C_CUSTOM_ROW_VIEW = "com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow";
    private static final String C_BG_VIEW_EXT = "com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp";
    private static final String C_ROW_PROVIDER = "com.oplus.systemui.notification.row.NotificationMenuRowProviderKt";
    private static final String C_MEDIA_CAROUSEL = "com.oplus.systemui.media.controls.ui.OplusMediaCarouselView";
    private static final String C_META_BALL = "com.oplus.posteffect.drawable.MetaBallBlurDrawable";
    private static final String C_AOD_CORE = "com.oplus.systemui.notification.aod.OplusPanoramicAodCoreStartable";
    private static final String C_AOD_COLOR_CHANGER = "com.oplus.systemui.notification.aod.notification.NotificationDetailColorChanger";
    private static final String C_CUSTOM_ROW_AOD_BG = "com.oplus.systemui.notification.aod.notification.OplusCustomRowAodBackgroundChanger";
    private static final String C_PLATFORM_BLUR_DRAWABLE = "com.oplusos.systemui.common.blurability.platformblur.PlatformBlurDrawable";
    private static final String C_SHADER_BLEND_HELPER = "com.oplusos.systemui.common.util.ShaderBlendParamHelper";
    private static final String C_MIX_CFG = "com.oplusos.systemui.common.blurability.BlurMixConfig";
    private static final String C_MIX_SHADER = "com.oplusos.systemui.common.blurability.MixColorWithShader";
    private static final String C_MIX_SINGLE = "com.oplusos.systemui.common.blurability.BlurMixConfig$BlurMixSingleWithShader";
    private static final String C_MIX_MULTI = "com.oplusos.systemui.common.blurability.BlurMixConfig$BlurMixMultiWithShader";
    private static final String C_MIX_COLOR = "com.oplusos.systemui.common.blurability.MixColor";
    private static final String C_EXPANDABLE_NOTIF_ROW = "com.android.systemui.statusbar.notification.row.ExpandableNotificationRow";
    private static final String C_NORMAL_BG_AOD_WRAPPER = "com.oplus.systemui.notification.aod.notification.NormalBgAodWrapper";
    private static final String C_NORMAL_BG_AOD_COLOR = "com.oplus.systemui.notification.aod.notification.NormalBgAodColor";
    private static final String C_BG_AOD_DATA = "com.oplus.systemui.notification.aod.notification.BgAodData";
    private static final String C_NOTIF_AOD_BG_CHANGER = "com.oplus.systemui.notification.aod.notification.NotificationAodBackgroundChanger";
    private static final String C_MEDIA_SEEDLING = "com.oplus.systemui.media.seedling.MediaSeedling";
    private static final String C_LA_PLUGIN_FILTER = "com.oplus.systemui.statusbar.notification.livealert.data.repository.OplusLiveAlertFilterByPlugin";

    // Clean neutral glass RGB matching Expanded Dynamic Island (0.10, 0.10, 0.12)
    private static final int CLEAN_GLASS_RGB = 0x1A1A1F;
    // Photometrically matched to lockscreen widgets in Fullscreen AOD (0.467 * W_Lock + 32.5 ~= 0.93 * W_AOD + 32.5)
    private static final int AOD_WIDGET_RGB = 0x3D3D3D;
    private static final int AOD_WIDGET_ALPHA = 0x88;

    private static final WeakIdentityMap<Object, Boolean> CAPSULE_CFG =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<Object, Integer> ORIG_BLUR_RADIUS =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<Object, RowExtInfo> EXT_ROW_INFO =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<Object, Object> ORIG_MIX_BY_CFG =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<Object, float[]> ORIG_SHADOW =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<Object, float[]> ORIG_STROKE =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<Object, Boolean> MEDIA_STROKE_SET =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<Object, Boolean> MEDIA_SHADOW_SET =
            new WeakIdentityMap<>();
    private static final Map<View, Boolean> MEDIA_SHADE_STATE =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final WeakIdentityMap<Object, Boolean> LOCK_CUSTOM_MIX_APPLIED =
            new WeakIdentityMap<>();
    private static final Map<View, Integer> LAST_CUSTOM_MIX_VER =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Integer> LAST_CUSTOM_MASK_VER =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, Integer> LAST_HELPER_VER =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, Integer> LAST_EAR_VER =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, int[]> LAST_MEDIA_STATE =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final WeakIdentityMap<Object, Boolean> META_DRAWABLE_IS_MEDIA =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<Object, float[]> ORIG_BLUR_CFG_LIGHT =
            new WeakIdentityMap<>();
    private static final Map<View, Boolean> ACTIVE_CUSTOM_ROWS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Boolean> ACTIVE_NORMAL_ROWS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, Boolean> ACTIVE_CAPSULE_HELPERS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Boolean> ACTIVE_CAPSULE_EARS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final WeakIdentityMap<Object, Object> NORMAL_BG_AOD_COLORS =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<Object, Boolean> BUILT_MIX_SET =
            new WeakIdentityMap<>();
    private static final Map<Object, Integer> LAST_BLUR_SCALE_BITS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Long> LAST_CUSTOM_LOCK_STATE =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Object> LAST_CUSTOM_META_REF =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final WeakIdentityMap<Object, View> META_DRAWABLE_TO_ROW =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<Object, Boolean> META_DRAWABLE_BRIGHT_WHITE =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<Object, Integer> META_DRAWABLE_FALLBACK_RGB =
            new WeakIdentityMap<>();
    private static final Map<View, Long> LAST_DRIFT_CHECK_MS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Float> LAST_ROW_IMMERSIVE_PROG =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, int[]> ORIG_ROW_IMMERSIVE_COLORS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Integer> LAST_NORMAL_ROW_KEY =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Integer> LAST_NORMAL_GLOW_KEY =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Boolean> NORMAL_GLOW_TINTED_ROWS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Boolean> ACTIVE_GRAD_MASK_VIEWS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Integer> LAST_GRAD_MASK_HASH =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final WeakIdentityMap<Object, CustomRowMaskSpec> CUSTOM_MASK_BY_META =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<Object, int[]> ORIG_META_GRAD_COLORS =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<View, CachedMaskDraw> CUSTOM_ROW_MASK_CACHE =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<View, Boolean> ATTACHED_CUSTOM_DETACH_LISTENER =
            new WeakIdentityMap<>();
    private static final View.OnAttachStateChangeListener CUSTOM_ROW_DETACH_LISTENER =
            new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                }

                @Override
                public void onViewDetachedFromWindow(View rowView) {
                    ACTIVE_CUSTOM_ROWS.remove(rowView);
                    LAST_CUSTOM_LOCK_STATE.remove(rowView);
                    LAST_CUSTOM_MIX_VER.remove(rowView);
                    LAST_CUSTOM_MASK_VER.remove(rowView);
                    LAST_CUSTOM_META_REF.remove(rowView);
                    LAST_DRIFT_CHECK_MS.remove(rowView);
                    LAST_ROW_IMMERSIVE_PROG.remove(rowView);
                    ORIG_ROW_IMMERSIVE_COLORS.remove(rowView);
                    CUSTOM_ROW_MASK_CACHE.remove(rowView);
                    if (isMediaCustomRow(rowView) || sLastLockCustomRowIsMedia) {
                        sLastLockCustomRowIsMedia = hasActiveMediaCustomRow();
                        DynamicIslandHook.bumpMediaColorVersion();
                        ConfigStore.HookConfig cfgNow = sLastFcConfig;
                        if (cfgNow != null) {
                            DynamicIslandHook.onHostMediaViewDetached(cfgNow);
                        }
                    }
                }
            };

    private static final Object MISSING = new Object();
    private static final Object CLASS_CACHE_LOCK = new Object();
    private static final Map<ClassLoader, ConcurrentHashMap<String, Object>> CLASS_CACHE = new WeakHashMap<>();
    private static final ConcurrentHashMap<String, Object> NULL_CL_CLASS_CACHE = new ConcurrentHashMap<>();
    private static volatile ClassLoader sPrimaryCl;
    private static volatile ConcurrentHashMap<String, Object> sPrimaryClassMap;
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> FIELD_CACHE =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> NO_ARG_METHOD_CACHE =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> INT_METHOD_CACHE =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> FLOAT_METHOD_CACHE =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> BOOL_METHOD_CACHE =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> PARAM_METHOD_CACHE =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> ONE_ARG_STATIC_CACHE =
            new ConcurrentHashMap<>();

    private static volatile float sDisplayDensity;
    private static volatile Object sShaderBlendHelperInstance;
    private static volatile boolean sShaderBlendHelperProbed;
    private static volatile Method sUpdateMixSingleShaderParams;
    private static volatile Method sGetMixSingleShaderParams;
    private static volatile Method sSetShaderBlendParams;
    private static volatile Object sNotifBlurParamsMgrInstance;
    private static volatile boolean sNotifBlurParamsMgrProbed;
    private static volatile Method sApplyAodProgressToBlurParamsM;

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static final Object MIX_LOCK = new Object();
    private static final Map<Integer, Object> MIX_CACHE = new HashMap<>();
    private static Constructor<?> sMixShaderCtor;
    private static Constructor<?> sMixSingleCtor;
    private static Constructor<?> sMixMultiCtor;
    private static Constructor<?> sMixColorCtor;
    private static volatile Object sStockCapsuleMix;
    private static volatile Object sStockCustomMix;
    private static volatile WeakReference<Object> sViewBlurManagerRef;
    private static volatile WeakReference<Object> sAodRepoRef;
    private static volatile WeakReference<Object> sNormalBgAodWrapperRef;
    private static volatile WeakReference<Object> sMediaSeedlingRef;
    private static volatile float sPanoramicAodProgress = 0.0f;
    private static volatile boolean sPendingSeedlingAodTrue;
    private static ValueAnimator sAodVisualAnimator;
    private static volatile boolean sAodVisualAnimatorRunning;
    private static volatile boolean sVisualAodControlled;
    private static volatile boolean sVisualAodTransitionPending;
    private static volatile float sVisualAodProgress = 0.0f;
    private static volatile float sVisualAodTarget = 0.0f;
    private static volatile boolean sGoingToSleepFromLockscreen;
    private static volatile long sGoingToSleepAtMs;
    private static final PathInterpolator AOD_ENTER_EASE = new PathInterpolator(0.30f, 0.0f, 0.25f, 1.0f);
    private static final PathInterpolator AOD_EXIT_EASE = new PathInterpolator(0.20f, 0.0f, 0.15f, 1.0f);
    private static final long AOD_ENTER_DURATION_MS = 550L;
    private static final long AOD_EXIT_DURATION_MS = 260L;
    private static volatile boolean sLoggedCustomAgslErr;
    private static volatile boolean sLastLockCustomRowIsMedia;
    private static volatile boolean sMediaColorSyncPending;
    private static final AtomicInteger CUSTOM_ROW_HANDLER_ACTIVE_COUNT = new AtomicInteger();
    private static volatile boolean sAnyMetaRowMapped;
    private static volatile boolean sHasAnyNormalGlowTinted;
    private static volatile ConfigStore.HookConfig sLastFcConfig;
    private static final ConcurrentHashMap<Object, Boolean> SHADE_MODE_CACHE =
            new ConcurrentHashMap<>();
    private static volatile Field sBlurRadiusField;
    private static volatile Method sGlobalBlurModeM;
    private static volatile Field sStatusBarStateCtrlField;
    private static volatile Field sStateIntField;
    private static volatile boolean sStateFieldProbed;
    private static volatile Method sGetStateM;
    private static volatile Method sProxyGetViewM;
    private static volatile Method sProxyGetBlurCfgM;
    private static volatile Method sBgViewGetExtM;
    private static volatile Method sExtGetBgViewM;
    private static volatile Method sExtGetProxyM;
    private static volatile Object sCardTypeCapsule;
    private static volatile Object sCardTypeCapsuleEar;
    private static volatile Object sCardTypeCustom;

    private static final ThreadLocal<RuntimeShader> TL_CUSTOM_GLOW_SHADER = new ThreadLocal<>();
    private static final ThreadLocal<Paint> TL_CUSTOM_GLOW_PAINT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IN_MASK_RESTORE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IN_DEFERRED_SEEDLING_AOD = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IN_INTERNAL_META_CALL = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IN_CUSTOM_MASK_SYNC = new ThreadLocal<>();
    private static final ThreadLocal<Float> TL_IMMERSIVE_MASK_FADE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> TL_IN_BG_DRAW_BEFORE = new ThreadLocal<>();
    private static final ThreadLocal<Integer> TL_CUSTOM_ROW_HANDLER_DEPTH = new ThreadLocal<>();
    private static volatile Method sUpdateDrawableMetaColorM;
    private static volatile Method sApplyGradientColorsWithFadeM;
    private static volatile Field sGradMaskPaintField;
    private static volatile boolean sGradMaskPaintProbed;

    private static final class CustomRowMaskSpec {
        final int rgb;
        final int peakAlpha;
        final int startPct;
        final int endPct;
        final int midPct;

        CustomRowMaskSpec(int rgb, int peakAlpha, int startPct, int endPct, int midPct) {
            this.rgb = rgb & 0x00FFFFFF;
            this.peakAlpha = clampAlpha(peakAlpha);
            this.startPct = startPct;
            this.endPct = endPct;
            this.midPct = midPct;
        }

        boolean matches(CustomRowMaskSpec other) {
            return other != null
                    && rgb == other.rgb
                    && peakAlpha == other.peakAlpha
                    && startPct == other.startPct
                    && endPct == other.endPct
                    && midPct == other.midPct;
        }
    }

    private static final class CachedMaskDraw {
        int rgb;
        int startPct;
        int endPct;
        int midPct;
        int width;
        int height;
        float cornerMode;
        float cornerRadius;
        float cornerWeight;
        Path smoothPath;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        LinearGradient shader;
    }

    // ColorOS 17 Y-UP AGSL SDF (RBox + CONIC sdBezierDistance + G2 smoothSDF_D) for lockscreen OplusCustomRow bottom highlight
    private static final String CUSTOM_ROW_BOTTOM_GLOW_AGSL =
            "uniform float2 uSize;\n" +
            "uniform float uRadius;\n" +
            "uniform float uCornerMode;\n" +
            "uniform float uWeight;\n" +
            "uniform float uRange;\n" +
            "uniform float uOffsetY;\n" +
            "uniform float2 uScale;\n" +
            "uniform float uShadowAlpha;\n" +
            "uniform float uRimAlpha;\n" +
            "uniform float uRimFade;\n" +
            "uniform float3 uGlowColor;\n" +
            "const float EPS = 1e-6;\n" +
            "const float kAbs = 1.0;\n" +
            "const float kMax = 26.6;\n" +
            "const float junctionXBand = 70.0;\n" +
            "const float junctionYBand = 60.0;\n" +
            "float RBox(float2 p, float2 b, float r) {\n" +
            "    float2 q = abs(p) - b + float2(r);\n" +
            "    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;\n" +
            "}\n" +
            "float cubicBezierDistance(float2 p, float2 p0, float2 p1, float2 p2, float2 p3) {\n" +
            "    float2 c0 = p0;\n" +
            "    float2 c1 = 3.0 * (p1 - p0);\n" +
            "    float2 c2 = 3.0 * (p2 - 2.0 * p1 + p0);\n" +
            "    float2 c3 = p3 - 3.0 * p2 + 3.0 * p1 - p0;\n" +
            "    float t = 0.5;\n" +
            "    for (int i = 0; i < 10; i++) {\n" +
            "        float2 Bt = ((c3 * t + c2) * t + c1) * t + c0;\n" +
            "        float2 dBt = (3.0 * c3 * t + 2.0 * c2) * t + c1;\n" +
            "        float2 ddBt = 6.0 * c3 * t + 2.0 * c2;\n" +
            "        float2 r = Bt - p;\n" +
            "        float f1 = dot(dBt, r);\n" +
            "        float f2 = dot(ddBt, r) + dot(dBt, dBt);\n" +
            "        float denom = max(f2, 1e-4);\n" +
            "        t = clamp(t - f1 / denom, 0.0, 1.0);\n" +
            "    }\n" +
            "    float2 Bt = ((c3 * t + c2) * t + c1) * t + c0;\n" +
            "    float2 re = Bt - p;\n" +
            "    float2 tangent = (3.0 * c3 * t + 2.0 * c2) * t + c1;\n" +
            "    float2 normal = float2(-tangent.y, tangent.x);\n" +
            "    float2 normalizedNormal = normalize(normal);\n" +
            "    float dis = length(re);\n" +
            "    return dot(re, normalizedNormal) > 0.0 ? dis : -dis;\n" +
            "}\n" +
            "float sdBezierDistance(float2 p, float2 b, float corner, float weight) {\n" +
            "    float cc = corner;\n" +
            "    if (any(lessThanEqual(abs(p), b - cc)) || cc <= 0.01) {\n" +
            "        return RBox(p, b, cc);\n" +
            "    }\n" +
            "    float point = max(cc * weight * 0.5, 0.001);\n" +
            "    return cubicBezierDistance(abs(p) - b + cc, float2(cc, 0.0), float2(cc, point), float2(point, cc), float2(0.0, cc));\n" +
            "}\n" +
            "float smoothAbs(float x, float k) {\n" +
            "    return sqrt(x * x + k * k);\n" +
            "}\n" +
            "float smoothMaxSqrt(float a, float b, float k) {\n" +
            "    float d = a - b;\n" +
            "    return 0.5 * (a + b + sqrt(d * d + k * k));\n" +
            "}\n" +
            "float sdCapsule(float2 pLocal, float2 halfSize) {\n" +
            "    bool isVertical = halfSize.x < halfSize.y;\n" +
            "    float2 hs = isVertical ? halfSize.yx : halfSize;\n" +
            "    float2 pl = isVertical ? pLocal.yx : pLocal;\n" +
            "    float2 q = float2(smoothAbs(pl.x, kAbs), smoothAbs(pl.y, kAbs));\n" +
            "    float r = hs.y;\n" +
            "    float a = max(hs.x - r, 0.0);\n" +
            "    float scale = r * 0.0125;\n" +
            "    float kMaxEff = kMax * scale;\n" +
            "    float xBandEff = junctionXBand * scale;\n" +
            "    float yBandEff = junctionYBand * scale;\n" +
            "    float s = q.x - a;\n" +
            "    float xHard = max(s, 0.0);\n" +
            "    float xSoft = (kMaxEff > 0.0) ? smoothMaxSqrt(s, 0.0, kMaxEff) : xHard;\n" +
            "    float wx = 1.0 - smoothstep(0.0, max(xBandEff, 1e-3), abs(s));\n" +
            "    float wy = smoothstep(r - max(yBandEff, 1e-3), r, q.y);\n" +
            "    float w = clamp(wx * wy, 0.0, 1.0);\n" +
            "    float x = mix(xHard, xSoft, w);\n" +
            "    return length(float2(x, q.y)) - r;\n" +
            "}\n" +
            "float powFast(float x, float n) {\n" +
            "    x = max(x, 1e-8);\n" +
            "    return exp2(log2(x) * n);\n" +
            "}\n" +
            "float lpNorm(float2 v, float n) {\n" +
            "    v = abs(v);\n" +
            "    float m = max(v.x, v.y);\n" +
            "    if (m < 1e-8) return 0.0;\n" +
            "    float2 u = v / m;\n" +
            "    float s = powFast(u.x, n) + powFast(u.y, n);\n" +
            "    return m * exp2(log2(max(s, EPS)) / n);\n" +
            "}\n" +
            "float lpNorm4(float2 v) {\n" +
            "    v = abs(v);\n" +
            "    float2 v2 = v * v;\n" +
            "    float2 v4 = v2 * v2;\n" +
            "    return sqrt(sqrt(max(v4.x + v4.y, 0.0)));\n" +
            "}\n" +
            "float sdRoundRect(float2 p, float2 halfSize, float r, float n) {\n" +
            "    float2 q = abs(p) - halfSize + float2(r);\n" +
            "    float nClamped = max(n, 2.0);\n" +
            "    float dCorner = (abs(nClamped - 4.0) < 1e-3) ? lpNorm4(max(q, 0.0)) : lpNorm(max(q, 0.0), nClamped);\n" +
            "    if (q.x > 0.0 && q.y > 0.0) return dCorner - r;\n" +
            "    else return max(q.x, q.y) - r;\n" +
            "}\n" +
            "float smoothSDF_D(float2 p, float2 b, float corner, float weight) {\n" +
            "    float rectSdf = 0.0;\n" +
            "    if (weight < 2.0) {\n" +
            "        if (weight > 1.005) {\n" +
            "            float t = smoothstep(2.0, 1.0, weight);\n" +
            "            float d_G2 = sdRoundRect(p, b, corner, 2.0);\n" +
            "            float d_Capsule = sdCapsule(p, b);\n" +
            "            rectSdf = mix(d_G2, d_Capsule, t);\n" +
            "        } else {\n" +
            "            rectSdf = sdCapsule(p, b);\n" +
            "        }\n" +
            "    } else {\n" +
            "        rectSdf = sdRoundRect(p, b, corner, weight);\n" +
            "    }\n" +
            "    return rectSdf;\n" +
            "}\n" +
            "float cardSdf(float2 p, float2 b, float r) {\n" +
            "    float rc = min(r, min(b.x, b.y));\n" +
            "    if (uCornerMode > 1.5) {\n" +
            "        return smoothSDF_D(p, b, rc, uWeight);\n" +
            "    } else if (uCornerMode > 0.5) {\n" +
            "        return sdBezierDistance(p, b, rc, uWeight);\n" +
            "    }\n" +
            "    return RBox(p, b, rc);\n" +
            "}\n" +
            "half4 main(float2 fragCoord) {\n" +
            "    float2 halfSize = uSize * 0.5;\n" +
            "    float2 coords = float2(fragCoord.x, uSize.y - fragCoord.y);\n" +
            "    float2 pos = coords - halfSize;\n" +
            "    float d = cardSdf(pos, halfSize, uRadius);\n" +
            "    if (d > 0.5) {\n" +
            "        return half4(0.0);\n" +
            "    }\n" +
            "    float2 halfSizeFade = halfSize * uScale;\n" +
            "    float fadeCorner = min(min(halfSizeFade.x, halfSizeFade.y), max(uRange * 1.2, uRadius));\n" +
            "    float2 posNew = float2(pos.x, pos.y - uOffsetY);\n" +
            "    float fadeSdf = cardSdf(posNew, halfSizeFade, fadeCorner);\n" +
            "    float glow = smoothstep(-uRange, 0.0, fadeSdf) * smoothstep(-uRange, 0.0, d);\n" +
            "    glow = pow(glow, 10.0) * 0.4 + 0.6 * glow;\n" +
            "    float shadowAlpha = glow * uShadowAlpha;\n" +
            "    float rimSdf = pow(smoothstep(-uRimFade, -2.0, d), 3.0);\n" +
            "    float bottomMask = smoothstep(-halfSize.y * 0.10, -halfSize.y * 0.80, pos.y);\n" +
            "    float rimAlpha = rimSdf * bottomMask * uRimAlpha;\n" +
            "    float edgeAA = 1.0 - smoothstep(-1.0, 0.5, d);\n" +
            "    float3 innerRgb = uGlowColor;\n" +
            "    float3 rimRgb = mix(uGlowColor, float3(1.0), 0.16);\n" +
            "    float3 col = innerRgb * shadowAlpha * (1.0 - rimAlpha) + rimRgb * rimAlpha;\n" +
            "    float outA = clamp(1.0 - (1.0 - shadowAlpha) * (1.0 - rimAlpha), 0.0, 1.0);\n" +
            "    return half4(col * edgeAA, outA * edgeAA);\n" +
            "}\n";

    private FluidCloudHook() {
    }

    public static void install(ClassLoader cl, ConfigStore.HookConfig cfg) {
        sLastFcConfig = cfg;
        int count = 0;
        if (hookScrim(cl, cfg)) count++;
        if (hookBlurProxy(cl, cfg)) count++;
        if (hookViewBlurRequireProxy(cl, cfg)) count++;
        if (hookScrimView(cl, cfg)) count++;
        if (hookFallbackColor(cl, cfg)) count++;
        if (hookDimEffect(cl, cfg)) count++;
        if (hookCapsuleEar(cl, cfg)) count++;
        if (hookKeyguardCapsuleMask(cl, cfg)) count++;
        if (hookMix(cl, cfg)) count++;
        if (hookBottomHighlight(cl, cfg)) count++;
        if (hookMediaCarouselHighlight(cl, cfg)) count++;
        if (hookCustomRow(cl, cfg)) count++;
        if (hookCustomRowView(cl, cfg)) count++;
        if (hookCustomRowBgDraw(cl, cfg)) count++;
        if (hookMetaBall(cl, cfg)) count++;
        if (hookLiveCapsuleRefresh(cl, cfg)) count++;
        if (hookAodTransition(cl, cfg)) count++;
        if (hookNormalNotifAod(cl, cfg)) count++;
        if (hookLiveAlertDebugFilterCache(cl, cfg)) count++;
        if (hookLockNotifHighlight(cl, cfg)) count++;
        cfg.note("FluidCloud hooks installed: " + count + "/20");
    }

    private static boolean hookLiveAlertDebugFilterCache(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> filterCls = loadClass(C_LA_PLUGIN_FILTER, cl);
            if (filterCls == null) {
                return false;
            }
            Method target = null;
            for (Method m : filterCls.getDeclaredMethods()) {
                if ("filterEntry".equals(m.getName()) && m.getParameterTypes().length == 1) {
                    m.setAccessible(true);
                    target = m;
                    break;
                }
            }
            if (target == null) {
                return false;
            }
            XposedCompat.hookMethod(target, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.thisObject == null || param.args.length == 0 || param.args[0] == null) {
                        return;
                    }
                    Object entry = param.args[0];
                    Object sbn = callNoArg(entry, "getSbn");
                    if (sbn instanceof android.service.notification.StatusBarNotification
                            && ConfigStore.PKG_NAME.equals(
                                    ((android.service.notification.StatusBarNotification) sbn).getPackageName())) {
                        Object key = callNoArg(entry, "getKey");
                        Object cache = readField(param.thisObject, "cache");
                        if (cache instanceof Map && key != null) {
                            ((Map<?, ?>) cache).remove(key);
                        }
                    }
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("FluidCloud LiveAlert filter cache hook err: " + t);
            return false;
        }
    }

    private static Set<View> collectActiveCustomRows(ClassLoader cl) {
        Set<View> rows = new LinkedHashSet<>();
        synchronized (ACTIVE_CUSTOM_ROWS) {
            rows.addAll(ACTIVE_CUSTOM_ROWS.keySet());
        }
        Object refMgr = sViewBlurManagerRef != null ? sViewBlurManagerRef.get() : null;
        if (refMgr != null) {
            Object ccv = readField(refMgr, "customCardViews");
            if (ccv instanceof Collection<?>) {
                Class<?> rowCls = cl != null ? loadClass(C_CUSTOM_ROW_VIEW, cl) : null;
                for (Object o : (Collection<?>) ccv) {
                    if (!(o instanceof View)) {
                        continue;
                    }
                    View v = (View) o;
                    Class<?> effRowCls = rowCls != null ? rowCls
                            : loadClass(C_CUSTOM_ROW_VIEW, v.getClass().getClassLoader());
                    View candidate = null;
                    if (effRowCls != null) {
                        if (effRowCls.isInstance(v)) {
                            candidate = v;
                        } else if (v.getParent() instanceof View && effRowCls.isInstance(v.getParent())) {
                            // ViewBlurManager.customCardViews holds NotificationBackgroundView (child), not OplusCustomRow.
                            candidate = (View) v.getParent();
                        }
                    }
                    if (candidate != null) {
                        rows.add(candidate);
                        ACTIVE_CUSTOM_ROWS.put(candidate, Boolean.TRUE);
                    }
                }
            }
        }
        return rows;
    }

    static boolean hasActiveMediaCustomRow() {
        Set<View> rows = collectActiveCustomRows(sPrimaryCl);
        for (View rowView : rows) {
            if (rowView != null && rowView.isAttachedToWindow()
                    && rowView.getVisibility() == View.VISIBLE
                    && isMediaCustomRow(rowView)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasActiveMediaCustomRowOnSurface(boolean onLockSurface) {
        boolean onLockNow = isCurrentlyOnLockscreen();
        if (onLockSurface != onLockNow) {
            return false;
        }
        ArrayList<View> rows;
        synchronized (ACTIVE_CUSTOM_ROWS) {
            if (!ACTIVE_CUSTOM_ROWS.isEmpty()) {
                rows = new ArrayList<>(ACTIVE_CUSTOM_ROWS.keySet());
            } else {
                rows = null;
            }
        }
        if (rows == null || rows.isEmpty()) {
            rows = new ArrayList<>(collectActiveCustomRows(sPrimaryCl));
        }
        for (int i = 0, n = rows.size(); i < n; i++) {
            View rowView = rows.get(i);
            if (rowView == null || !rowView.isAttachedToWindow() || !isMediaCustomRow(rowView)) {
                continue;
            }
            if (onLockSurface) {
                if (rowView.getVisibility() == View.VISIBLE) {
                    return true;
                }
            } else {
                if (rowView.isShown()) {
                    return true;
                }
            }
        }
        return false;
    }

    static void onMediaColorChanged(final ConfigStore.HookConfig cfg) {
        if (cfg == null || sMediaColorSyncPending) {
            return;
        }
        sMediaColorSyncPending = true;
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                sMediaColorSyncPending = false;
                LAST_DRIFT_CHECK_MS.clear();
                if (cfg.isFcEnabled() && (cfg.isFcMaskFollowMedia() || cfg.isFcNonMediaMaskEnabled()
                        || cfg.isFcColorEnabled() || cfg.isFcCustomMaskEnabled())) {
                    ClassLoader cl = sPrimaryCl;
                    Set<View> rows = collectActiveCustomRows(cl);
                    boolean anyLockMedia = false;
                    for (View rowView : rows) {
                        if (rowView != null && rowView.isAttachedToWindow()) {
                            Object mgr = readField(rowView, "viewBlurManager");
                            if (isCustomOnLockscreen(mgr, null)
                                    && rowView.getVisibility() == View.VISIBLE
                                    && isMediaCustomRow(rowView)) {
                                anyLockMedia = true;
                                break;
                            }
                        }
                    }
                    sLastLockCustomRowIsMedia = anyLockMedia;
                    float liveAod = resolveLiveAodProgress(null);
                    for (View rowView : rows) {
                        if (rowView == null || !rowView.isAttachedToWindow()) {
                            continue;
                        }
                        LAST_CUSTOM_LOCK_STATE.remove(rowView);
                        LAST_CUSTOM_MIX_VER.remove(rowView);
                        LAST_CUSTOM_MASK_VER.remove(rowView);
                        Object mgr = readField(rowView, "viewBlurManager");
                        ClassLoader effCl = cl != null ? cl : rowView.getClass().getClassLoader();
                        if (isCustomOnLockscreen(mgr, null) && cfg.isFcColorEnabled()) {
                            syncCustomRowLockMix(effCl, rowView, cfg, liveAod, true);
                        }
                        if (!isCustomOnLockscreen(mgr, null) || !isRowInImmersiveFade(rowView)) {
                            syncCustomRowMask(effCl, rowView, mgr, cfg, false);
                        } else if (readRowImmersiveProgress(rowView) >= 0.99f) {
                            zeroMetaColorSafely(resolveCustomRowMetaDrawable(effCl, rowView, null, null));
                        }
                        Object bgObj = readField(rowView, "mBackgroundNormal");
                        if (bgObj instanceof View) {
                            ((View) bgObj).invalidate();
                        }
                        DynamicIslandHook.invalidateMultiLightViewsInTree(rowView);
                        rowView.invalidate();
                    }
                    ArrayList<View> gradMasks;
                    synchronized (ACTIVE_GRAD_MASK_VIEWS) {
                        gradMasks = ACTIVE_GRAD_MASK_VIEWS.isEmpty()
                                ? new ArrayList<View>(0)
                                : new ArrayList<>(ACTIVE_GRAD_MASK_VIEWS.keySet());
                    }
                    for (int i = 0, n = gradMasks.size(); i < n; i++) {
                        View v = gradMasks.get(i);
                        if (v != null && v.isAttachedToWindow()) {
                            v.invalidate();
                        }
                    }
                    if (cfg.isFcColorEnabled() && isCurrentlyOnLockscreen()) {
                        ArrayList<Object> helpers;
                        synchronized (ACTIVE_CAPSULE_HELPERS) {
                            helpers = ACTIVE_CAPSULE_HELPERS.isEmpty()
                                    ? new ArrayList<Object>(0)
                                    : new ArrayList<>(ACTIVE_CAPSULE_HELPERS.keySet());
                        }
                        for (int i = 0, n = helpers.size(); i < n; i++) {
                            Object h = helpers.get(i);
                            if (h != null) {
                                LAST_HELPER_VER.remove(h);
                                syncHelperBlurProxies(cl, h, cfg, liveAod, true);
                            }
                        }
                        ArrayList<View> ears;
                        synchronized (ACTIVE_CAPSULE_EARS) {
                            ears = ACTIVE_CAPSULE_EARS.isEmpty()
                                    ? new ArrayList<View>(0)
                                    : new ArrayList<>(ACTIVE_CAPSULE_EARS.keySet());
                        }
                        for (int i = 0, n = ears.size(); i < n; i++) {
                            View ear = ears.get(i);
                            if (ear != null && ear.isAttachedToWindow()) {
                                LAST_EAR_VER.remove(ear);
                                syncHelperBlurProxies(cl, ear, cfg, liveAod, true);
                            }
                        }
                    }
                }

                if (cfg.isLockNotifGlowFollowMedia() || !NORMAL_GLOW_TINTED_ROWS.isEmpty()) {
                    ArrayList<View> normalRows;
                    synchronized (ACTIVE_NORMAL_ROWS) {
                        normalRows = ACTIVE_NORMAL_ROWS.isEmpty()
                                ? new ArrayList<View>(0)
                                : new ArrayList<>(ACTIVE_NORMAL_ROWS.keySet());
                    }
                    boolean onLock = isCustomOnLockscreen(null, null);
                    ClassLoader cl = sPrimaryCl;
                    for (int i = 0, n = normalRows.size(); i < n; i++) {
                        View nr = normalRows.get(i);
                        if (nr == null || !nr.isAttachedToWindow()) {
                            continue;
                        }
                        Object bgNormal = readField(nr, "mBackgroundNormal");
                        Object extObj = bgNormal != null ? callNoArg(bgNormal, "getExt") : null;
                        ClassLoader effCl = cl != null ? cl : nr.getClass().getClassLoader();
                        syncNormalRowHighlightColor(effCl, nr, extObj, cfg, onLock, true);
                        if (bgNormal instanceof View) {
                            ((View) bgNormal).invalidate();
                        }
                        nr.invalidate();
                    }
                }
            }
        });
    }

    private static float resolveMaskAlphaScale(ConfigStore.HookConfig cfg, boolean isMedia, boolean hasTint) {
        float catScale = isMedia
                ? cfg.getFcMaskStrengthScale()
                : cfg.getNonMediaMaskOpacityScaleForTint(hasTint);
        return catScale * cfg.getFcMaskGradStrengthScale();
    }

    private static float easeAodProgress(float raw) {
        float c = clamp01(raw);
        if (c <= 0.001f) {
            return 0.0f;
        }
        if (c >= 0.999f) {
            return 1.0f;
        }
        if (sVisualAodControlled) {
            return c;
        }
        return (float) Math.pow(c, 1.45);
    }

    private static int resolveImmBucket(float immProg) {
        if (immProg >= 0.99f) {
            return 2;
        }
        if (immProg > 0.01f) {
            return 1;
        }
        return 0;
    }

    private static int computeHelperKey(ConfigStore.HookConfig cfg, Object helperOrView, float rawAodProg) {
        int mediaBit = (cfg.isFcColorEnabled()
                && shouldSuppressCapsuleCustomColor(resolveHelperView(helperOrView), cfg)) ? 1 : 0;
        return (((cfg.getFcVersion() * 31 + cfg.getFcOpacityInt()) * 2 + mediaBit) * 512)
                + Math.round(clamp01(rawAodProg) * 255f);
    }

    private static int computeCustomMixKey(ConfigStore.HookConfig cfg, View rowView, float rawAodProg) {
        int immBucket = resolveImmBucket(readRowImmersiveProgress(rowView));
        int mediaBit = (cfg.isFcColorEnabled() && shouldSuppressLockCustomColor(rowView, cfg)) ? 1 : 0;
        int h = cfg.getFcVersion();
        h = h * 31 + cfg.getFcImmersiveOpacityInt();
        h = h * 31 + immBucket;
        h = h * 2 + mediaBit;
        return h * 512 + Math.round(clamp01(rawAodProg) * 255f);
    }

    private static int computeCustomMaskKey(View rowView, ConfigStore.HookConfig cfg,
                                            boolean onLock, float rawAodProg) {
        boolean isMedia = isMediaCustomRow(rowView);
        int mediaMaskRgb = resolveMediaMaskRgb(cfg, isMedia, onLock);
        boolean hasMediaTint = mediaMaskRgb != 0;
        boolean hasCustomMask = !hasMediaTint && isCustomMaskAllowed(cfg, isMedia, onLock);
        int customMaskArgb = hasCustomMask ? cfg.getFcCustomMaskColor() : 0;
        int effMaskRgb = hasMediaTint ? (mediaMaskRgb & 0x00FFFFFF)
                : (hasCustomMask ? (customMaskArgb & 0x00FFFFFF) : 0);
        float maskScale = resolveMaskAlphaScale(cfg, isMedia, hasMediaTint)
                * (hasCustomMask ? ConfigStore.colorAlphaScale(customMaskArgb) : 1.0f);
        int maskScaleKey = Math.round(maskScale * 1000f);
        int maskAodKey = (onLock && !cfg.isFcMatchCard() && maskScale > 0.001f)
                ? Math.round(clamp01(rawAodProg) * 255f) : 0;
        int gradKey = ((cfg.getFcMaskGradStartInt() * 101 + cfg.getFcMaskGradEndInt()) * 101
                + cfg.getFcMaskGradMidInt());
        int tintMode = (hasMediaTint ? 1 : 0) | (hasCustomMask ? 2 : 0);
        int modeFlags = (cfg.isFcEnabled() ? 1 : 0)
                | (cfg.isFcMatchCard() ? 2 : 0)
                | (cfg.isFcRemoveShadeMask() ? 4 : 0)
                | (isMedia ? 8 : 0);
        return (((((((((cfg.getFcVersion() << 1) | (onLock ? 1 : 0)) * 31) + tintMode) * 31
                + effMaskRgb) * 31) + maskScaleKey) * 31 + maskAodKey) * 31 + gradKey) * 31 + modeFlags;
    }

    private static long computeCustomLockStateKey(View rowView, ConfigStore.HookConfig cfg,
                                                  boolean onLock, float rawAodProg) {
        int fcVer = cfg.getFcVersion();
        int aodQ = onLock ? Math.round(clamp01(rawAodProg) * 255f) : 0;
        int immBucket = resolveImmBucket(readRowImmersiveProgress(rowView));
        boolean isMedia = isMediaCustomRow(rowView);
        int mediaVer = (isMedia || cfg.isFcMaskFollowMedia() || cfg.isFcNonMediaMaskEnabled()
                || cfg.isFcColorEnabled() || cfg.isFcCustomMaskEnabled())
                ? DynamicIslandHook.getMediaColorVersion() : 0;
        return (((long) fcVer) << 44)
                | (onLock ? (1L << 43) : 0L)
                | (cfg.isFcEnabled() ? (1L << 42) : 0L)
                | (isMedia ? (1L << 41) : 0L)
                | (((long) (immBucket & 0x3)) << 39)
                | (((long) (aodQ & 0xFF)) << 31)
                | (mediaVer & 0x7FFFFFFFL);
    }

    private static float resolveLiveAodProgress(Object repoOrHolder) {
        if (sVisualAodControlled || sAodVisualAnimatorRunning) {
            return clamp01(sVisualAodProgress);
        }
        try {
            Object repo = sAodRepoRef != null ? sAodRepoRef.get() : null;
            if (repo != null) {
                Object prog = callNoArg(repo, "getPanoramicAodProgress");
                if (prog instanceof Float) {
                    float val = clamp01((Float) prog);
                    sPanoramicAodProgress = val;
                    return val;
                }
            }
            if (repoOrHolder != null) {
                Object fieldRepo = readField(repoOrHolder, "oplusPanoramicAodRepository");
                if (fieldRepo != null) {
                    sAodRepoRef = new WeakReference<>(fieldRepo);
                    Object prog = callNoArg(fieldRepo, "getPanoramicAodProgress");
                    if (prog instanceof Float) {
                        float val = clamp01((Float) prog);
                        sPanoramicAodProgress = val;
                        return val;
                    }
                }
                Object directProg = callNoArg(repoOrHolder, "getPanoramicAodProgress");
                if (directProg instanceof Float) {
                    sAodRepoRef = new WeakReference<>(repoOrHolder);
                    float val = clamp01((Float) directProg);
                    sPanoramicAodProgress = val;
                    return val;
                }
            }
        } catch (Throwable ignored) {
        }
        return clamp01(sPanoramicAodProgress);
    }

    static float getCurrentAodProgress() {
        return easeAodProgress(resolveLiveAodProgress(null));
    }

    static boolean isAodVisualSettledInAod() {
        return sVisualAodControlled && !sAodVisualAnimatorRunning && sVisualAodProgress >= 0.99f;
    }

    private static void startVisualAodTransition(final ClassLoader cl, final ConfigStore.HookConfig cfg,
                                                 final boolean entering, final boolean fastExit) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    startVisualAodTransition(cl, cfg, entering, fastExit);
                }
            });
            return;
        }
        if (sAodVisualAnimator != null) {
            ValueAnimator prevAnim = sAodVisualAnimator;
            sAodVisualAnimator = null;
            prevAnim.cancel();
        }
        sVisualAodTransitionPending = false;
        if (!entering) {
            DynamicIslandHook.cancelPendingAodLightDisable();
        }
        final float end = entering ? 1.0f : 0.0f;
        final float start = clamp01(sVisualAodProgress);
        if ((!entering && fastExit) || Math.abs(start - end) <= 0.001f || !ValueAnimator.areAnimatorsEnabled()) {
            sAodVisualAnimatorRunning = false;
            sVisualAodProgress = end;
            sVisualAodTarget = end;
            sPanoramicAodProgress = end;
            sVisualAodControlled = entering;
            if (!entering) {
                sPendingSeedlingAodTrue = false;
                DynamicIslandHook.cancelPendingAodLightDisable();
            }
            dispatchAodProgressToActiveViews(cl, cfg, end);
            if (entering) {
                flushPendingSeedlingAodTrue();
            }
            return;
        }

        sVisualAodControlled = true;
        sVisualAodTarget = end;
        sAodVisualAnimatorRunning = true;
        long duration = entering
                ? Math.max(80L, Math.round(AOD_ENTER_DURATION_MS * (1.0f - start)))
                : Math.max(60L, Math.round(AOD_EXIT_DURATION_MS * start));
        final ValueAnimator anim = ValueAnimator.ofFloat(start, end);
        anim.setDuration(duration);
        anim.setInterpolator(entering ? AOD_ENTER_EASE : AOD_EXIT_EASE);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (sAodVisualAnimator != anim) {
                    return;
                }
                float val = clamp01((Float) animation.getAnimatedValue());
                sVisualAodProgress = val;
                sPanoramicAodProgress = val;
                dispatchAodProgressToActiveViews(cl, cfg, val);
                if (entering && val >= 0.99f) {
                    flushPendingSeedlingAodTrue();
                }
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (sAodVisualAnimator != anim) {
                    return;
                }
                sAodVisualAnimator = null;
                sAodVisualAnimatorRunning = false;
                sVisualAodProgress = end;
                sVisualAodTarget = end;
                sPanoramicAodProgress = end;
                sVisualAodControlled = entering;
                if (!entering) {
                    sPendingSeedlingAodTrue = false;
                    DynamicIslandHook.cancelPendingAodLightDisable();
                }
                dispatchAodProgressToActiveViews(cl, cfg, end);
                if (entering) {
                    flushPendingSeedlingAodTrue();
                }
            }
        });
        sAodVisualAnimator = anim;
        anim.start();
    }

    private static void flushPendingSeedlingAodTrue() {
        DynamicIslandHook.flushPendingAodLightDisable();
        if (!sPendingSeedlingAodTrue) {
            return;
        }
        sPendingSeedlingAodTrue = false;
        Object seedling = sMediaSeedlingRef != null ? sMediaSeedlingRef.get() : null;
        if (seedling == null) {
            return;
        }
        IN_DEFERRED_SEEDLING_AOD.set(Boolean.TRUE);
        try {
            callOneBool(seedling, "setPanoramicAod", true);
        } finally {
            IN_DEFERRED_SEEDLING_AOD.remove();
        }
    }

    private static boolean shouldSuppressLockCustomColor(View rowView, ConfigStore.HookConfig cfg) {
        if (rowView != null && isMediaCustomRow(rowView)) {
            return true;
        }
        return cfg.isFcNonMediaMaskEnabled() && DynamicIslandHook.hasActiveLockMediaFluidCloud();
    }

    private static View resolveHelperView(Object helperOrView) {
        if (helperOrView instanceof View) {
            return (View) helperOrView;
        }
        if (helperOrView == null) {
            return null;
        }
        Object proxy = readField(helperOrView, "viewBlurProxy");
        if (proxy == null) {
            proxy = readField(helperOrView, "smoothSharedViewBlurProxy");
        }
        Object vObj = proxy != null ? callNoArg(proxy, "getView") : null;
        return vObj instanceof View ? (View) vObj : null;
    }

    private static boolean isCapsuleMediaView(View view) {
        if (view == null) {
            return false;
        }
        View capsuleItem = findEnclosingCapsuleItem(view);
        View root = capsuleItem != null ? capsuleItem : view;
        return DynamicIslandHook.isViewContainingMedia(root);
    }

    private static boolean shouldSuppressCapsuleCustomColor(View capsuleView, ConfigStore.HookConfig cfg) {
        if (capsuleView != null) {
            if (isCapsuleMediaView(capsuleView)) {
                return true;
            }
            return cfg.isFcNonMediaMaskEnabled() && DynamicIslandHook.hasActiveLockMediaFluidCloud();
        }
        return DynamicIslandHook.hasActiveLockMediaFluidCloud()
                && (cfg.isFcNonMediaMaskEnabled() || DynamicIslandHook.hasActiveLockMiniMedia());
    }

    private static boolean hasSurfaceMediaActive(boolean isMedia, boolean onLock) {
        if (isMedia) {
            return true;
        }
        return onLock
                ? DynamicIslandHook.hasActiveLockMediaFluidCloud()
                : DynamicIslandHook.hasActiveShadeMediaFluidCloud();
    }

    private static int resolveMediaMaskRgb(ConfigStore.HookConfig cfg, boolean isMedia, boolean onLock) {
        boolean want = isMedia ? cfg.isFcMaskFollowMedia() : cfg.isFcNonMediaMaskEnabled();
        return want ? DynamicIslandHook.getScopedMediaMaskRgb(cfg, isMedia, false, onLock) : 0;
    }

    private static boolean isCustomMaskAllowed(ConfigStore.HookConfig cfg, boolean isMedia, boolean onLock) {
        if (isMedia || !cfg.isFcCustomMaskEnabled()) {
            return false;
        }
        return !(cfg.isFcNonMediaMaskEnabled() && hasSurfaceMediaActive(false, onLock));
    }

    private static boolean hookScrim(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> helperCls = loadClass(C_HELPER, cl);
            Method m = findMethod(helperCls, "setScrimAlpha", float.class);
            if (m == null) {
                cfg.note("FluidCloud: missing setScrimAlpha");
                return false;
            }
            XposedCompat.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.thisObject != null
                            && !Boolean.TRUE.equals(ACTIVE_CAPSULE_HELPERS.get(param.thisObject))) {
                        ACTIVE_CAPSULE_HELPERS.put(param.thisObject, Boolean.TRUE);
                    }
                    if (param.args.length > 0 && param.args[0] instanceof Float) {
                        float aodProg = clamp01((Float) param.args[0]);
                        if (!sVisualAodControlled && !sAodVisualAnimatorRunning
                                && !sVisualAodTransitionPending) {
                            sPanoramicAodProgress = aodProg;
                            resolveLiveAodProgress(param.thisObject);
                        }
                        if (cfg.isFcEnabled()) {
                            param.args[0] = 0.0f;
                        }
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (cfg.isFcEnabled()) {
                        Object sp = readField(param.thisObject, "scrimPaint");
                        if (sp instanceof Paint && ((Paint) sp).getAlpha() != 0) {
                            ((Paint) sp).setAlpha(0);
                        }
                        zeroMetaColorSafely(callNoArg(param.thisObject, "getBlurDrawable"));
                        float liveAod = resolveLiveAodProgress(param.thisObject);
                        int helperKey = computeHelperKey(cfg, param.thisObject, liveAod);
                        Integer prev = LAST_HELPER_VER.get(param.thisObject);
                        if (prev != null && (sAodVisualAnimatorRunning || prev == helperKey)) {
                            return;
                        }
                        LAST_HELPER_VER.put(param.thisObject, helperKey);
                        syncHelperBlurProxies(cl, param.thisObject, cfg, liveAod, true);
                    }
                }
            });

            Method mStroke = findMethod(helperCls, "setStrokeAndShadowAlpha",
                    float.class, float.class, Float.class);
            if (mStroke != null) {
                XposedCompat.hookMethod(mStroke, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (param.thisObject != null
                                && !Boolean.TRUE.equals(ACTIVE_CAPSULE_HELPERS.get(param.thisObject))) {
                            ACTIVE_CAPSULE_HELPERS.put(param.thisObject, Boolean.TRUE);
                        }
                        if (cfg.isFcEnabled() && (sVisualAodTransitionPending
                                || sAodVisualAnimatorRunning
                                || sVisualAodControlled
                                || resolveLiveAodProgress(param.thisObject) > 0.001f)) {
                            param.setResult(null);
                        }
                    }
                });
            }
            try {
                ClassLoader sysCl = helperCls.getClassLoader();
                Class<?> bgViewCls = Class.forName(C_CAPSULE_BG_VIEW, false, sysCl);
                Method mBgScrim = findMethod(bgViewCls, "setScrimAlpha", float.class);
                if (mBgScrim != null) {
                    XposedCompat.hookMethod(mBgScrim, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args.length > 0 && param.args[0] instanceof Float) {
                                float aodProg = clamp01((Float) param.args[0]);
                                if (!sVisualAodControlled && !sAodVisualAnimatorRunning
                                        && !sVisualAodTransitionPending) {
                                    sPanoramicAodProgress = aodProg;
                                    resolveLiveAodProgress(null);
                                }
                                if (cfg.isFcEnabled()) {
                                    param.args[0] = 0.0f;
                                }
                            }
                        }
                    });
                }
            } catch (Throwable ignored) {
            }
            return true;
        } catch (Throwable t) {
            cfg.note("FluidCloud setScrimAlpha hook err: " + t);
            return false;
        }
    }

    private static boolean hookBlurProxy(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> helperCls = loadClass(C_HELPER, cl);
            Method m = findMethod(helperCls, "requireBlurProxyForView", View.class);
            if (m == null) {
                cfg.note("FluidCloud: missing requireBlurProxyForView");
                return false;
            }
            XposedCompat.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.thisObject != null
                            && C_HELPER.equals(param.thisObject.getClass().getName())
                            && !Boolean.TRUE.equals(ACTIVE_CAPSULE_HELPERS.get(param.thisObject))) {
                        ACTIVE_CAPSULE_HELPERS.put(param.thisObject, Boolean.TRUE);
                    }
                    Object proxy = param.getResult();
                    if (proxy == null) {
                        return;
                    }
                    Object blurCfg = getProxyBlurConfig(proxy);
                    if (blurCfg != null) {
                        CAPSULE_CFG.put(blurCfg, Boolean.TRUE);
                        if (applyScaledBlurRadius(blurCfg, cfg, true)) {
                            callNoArg(proxy, "applyBlurConfig");
                        }
                    }
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("FluidCloud requireBlurProxyForView hook err: " + t);
            return false;
        }
    }

    private static Object getProxyBlurConfig(Object proxy) {
        if (proxy == null) {
            return null;
        }
        try {
            Method m = sProxyGetBlurCfgM;
            if (m == null || !m.getDeclaringClass().isInstance(proxy)) {
                m = findSingleTypedMethodCached(NO_ARG_METHOD_CACHE, proxy.getClass(), "getBlurConfig", null);
                sProxyGetBlurCfgM = m;
            }
            return m != null ? m.invoke(proxy) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field resolveBlurRadiusField(Object blurCfg) {
        Field f = sBlurRadiusField;
        if (f != null) {
            return f;
        }
        if (blurCfg == null) {
            return null;
        }
        f = findFieldCached(blurCfg.getClass(), "blurRadius");
        if (f != null) {
            sBlurRadiusField = f;
        }
        return f;
    }

    private static boolean applyScaledBlurRadius(Object blurCfg, ConfigStore.HookConfig cfg, boolean enabled) {
        if (blurCfg == null) {
            return false;
        }
        try {
            Field f = resolveBlurRadiusField(blurCfg);
            if (f == null) {
                return false;
            }
            int cur = f.getInt(blurCfg);
            if (cur <= 0 || cur > 2000) {
                return false;
            }
            Integer origObj = ORIG_BLUR_RADIUS.get(blurCfg);
            int orig;
            if (origObj == null || origObj <= 0 || origObj > 2000) {
                orig = cur;
                ORIG_BLUR_RADIUS.put(blurCfg, orig);
            } else {
                orig = origObj;
            }
            int target = (enabled && cfg != null && cfg.isFcEnabled())
                    ? Math.min(2000, Math.max(0, Math.round(orig * cfg.getFcBlurScale())))
                    : orig;
            if (cur != target) {
                f.setInt(blurCfg, target);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean hasBlurRadiusDrifted(Object blurCfg, ConfigStore.HookConfig cfg) {
        if (blurCfg == null || cfg == null || !cfg.isFcEnabled()) {
            return false;
        }
        try {
            Field f = resolveBlurRadiusField(blurCfg);
            if (f == null) {
                return false;
            }
            int cur = f.getInt(blurCfg);
            if (cur <= 0 || cur > 2000) {
                return false;
            }
            Integer origObj = ORIG_BLUR_RADIUS.get(blurCfg);
            if (origObj == null || origObj <= 0 || origObj > 2000) {
                return cfg.getFcBlurInt() != 100;
            }
            int target = Math.min(2000, Math.max(0, Math.round(origObj * cfg.getFcBlurScale())));
            return cur != target;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void ensureCardTypeConstants(Class<?> cardTypeCls) {
        if (cardTypeCls == null || (sCardTypeCapsule != null && sCardTypeCustom != null)) {
            return;
        }
        try {
            sCardTypeCapsule = Enum.valueOf((Class<Enum>) cardTypeCls, "CAPSULE");
        } catch (Throwable ignored) {
        }
        try {
            sCardTypeCapsuleEar = Enum.valueOf((Class<Enum>) cardTypeCls, "CAPSULE_EAR");
        } catch (Throwable ignored) {
        }
        try {
            sCardTypeCustom = Enum.valueOf((Class<Enum>) cardTypeCls, "CUSTOM");
        } catch (Throwable ignored) {
        }
    }

    private static boolean hookViewBlurRequireProxy(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> mgrCls = loadClass(C_BLUR_MGR, cl);
            Class<?> cardTypeCls = loadClass(C_CARD_TYPE, cl);
            Class<?> blurModeCls = loadClass(C_BLUR_MODE, cl);
            if (mgrCls == null || cardTypeCls == null || blurModeCls == null) {
                cfg.note("FluidCloud: missing ViewBlurManager#requireBlurProxyForView classes");
                return false;
            }
            ensureCardTypeConstants(cardTypeCls);
            Method m = findMethod(mgrCls, "requireBlurProxyForView",
                    View.class, cardTypeCls, float.class, blurModeCls);
            if (m == null) {
                cfg.note("FluidCloud: missing ViewBlurManager#requireBlurProxyForView");
                return false;
            }
            XposedCompat.hookDirect(m, new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object ct = chain.getArg(1);
                    Object mode = chain.getArg(3);
                    Object mgr = chain.getThisObject();
                    boolean isCap = ct != null && (ct == sCardTypeCapsule || ct == sCardTypeCapsuleEar);
                    boolean isCustom = ct != null && ct == sCardTypeCustom;
                    Object proxy = chain.proceed();
                    if ((isCap || isCustom) && proxy != null) {
                        Object blurCfg = getProxyBlurConfig(proxy);
                        if (blurCfg != null) {
                            if (isCap) {
                                CAPSULE_CFG.put(blurCfg, Boolean.TRUE);
                            }
                            boolean enableScale = isCap || isCustomOnLockscreen(mgr, mode);
                            if (applyScaledBlurRadius(blurCfg, cfg, enableScale)) {
                                callNoArg(proxy, "applyBlurConfig");
                            }
                        }
                    }
                    return proxy;
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("FluidCloud ViewBlurManager#requireBlurProxyForView hook err: " + t);
            return false;
        }
    }

    private static boolean hookScrimView(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> scrimCls = loadClass(C_SCRIM, cl);
            Method m = findMethod(scrimCls, "setScrimAlpha", float.class);
            if (m == null) {
                return false;
            }
            XposedCompat.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args.length > 0 && param.args[0] instanceof Float) {
                        if (!sVisualAodControlled && !sAodVisualAnimatorRunning
                                && !sVisualAodTransitionPending) {
                            sPanoramicAodProgress = clamp01((Float) param.args[0]);
                        }
                        if (cfg.isFcEnabled()) {
                            param.args[0] = 0.0f;
                        }
                    }
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("FluidCloud CapsuleScrimView hook err: " + t);
            return false;
        }
    }

    private static boolean hookFallbackColor(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> helperCls = loadClass(C_HELPER, cl);
            Method m = findMethod(helperCls, "getBackgroundColor");
            if (m == null) {
                cfg.note("FluidCloud: missing getBackgroundColor");
                return false;
            }
            XposedCompat.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!cfg.isFcEnabled() || !cfg.isFcLockTintOrMaskModified()) {
                        return;
                    }
                    Object res = param.getResult();
                    if (!(res instanceof Integer)) {
                        return;
                    }
                    int argb = (Integer) res;
                    int a = (argb >>> 24) & 0xFF;
                    boolean useCustom = cfg.isFcColorEnabled()
                            && !shouldSuppressCapsuleCustomColor(resolveHelperView(param.thisObject), cfg);
                    int custom = useCustom ? cfg.getFcColor() : 0;
                    float colorAlpha = useCustom ? ConfigStore.colorAlphaScale(custom) : 1.0f;
                    int newA = clampAlpha(Math.round(a * cfg.getFcOpacity() * colorAlpha));
                    int rgb = useCustom ? (custom & 0x00FFFFFF) : CLEAN_GLASS_RGB;
                    param.setResult((newA << 24) | rgb);
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("FluidCloud getBackgroundColor hook err: " + t);
            return false;
        }
    }

    private static boolean hookDimEffect(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> itemCls = loadClass(C_CAPSULE_ITEM, cl);
            Method m = findMethod(itemCls, "setDimEffect", float.class);
            if (m == null) {
                cfg.note("FluidCloud: missing setDimEffect");
                return false;
            }
            XposedCompat.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args.length > 0 && param.args[0] instanceof Float
                            && !sVisualAodControlled && !sAodVisualAnimatorRunning
                            && !sVisualAodTransitionPending) {
                        sPanoramicAodProgress = clamp01((Float) param.args[0]);
                    }
                    if (cfg.isFcEnabled()) {
                        if (Build.VERSION.SDK_INT >= 31 && param.thisObject instanceof View) {
                            ((View) param.thisObject).setRenderEffect(null);
                        }
                        param.setResult(null);
                    }
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("FluidCloud setDimEffect hook err: " + t);
            return false;
        }
    }

    private static boolean hookCapsuleEar(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> earCls = loadClass(C_CAPSULE_EAR, cl);
            if (earCls == null) {
                return false;
            }
            boolean ok = false;
            Method mAlpha = findMethod(earCls, "setScrimAlpha", float.class);
            if (mAlpha != null) {
                XposedCompat.hookMethod(mAlpha, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (param.thisObject instanceof View
                                && !Boolean.TRUE.equals(ACTIVE_CAPSULE_EARS.get(param.thisObject))) {
                            ACTIVE_CAPSULE_EARS.put((View) param.thisObject, Boolean.TRUE);
                        }
                        if (param.args.length > 0 && param.args[0] instanceof Float) {
                            if (!sVisualAodControlled && !sAodVisualAnimatorRunning
                                    && !sVisualAodTransitionPending) {
                                sPanoramicAodProgress = clamp01((Float) param.args[0]);
                            }
                            if (cfg.isFcEnabled()) {
                                param.args[0] = 0.0f;
                            }
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (cfg.isFcEnabled()) {
                            Object sp = readField(param.thisObject, "scrimPaint");
                            if (sp instanceof Paint && ((Paint) sp).getAlpha() != 0) {
                                ((Paint) sp).setAlpha(0);
                            }
                        }
                    }
                });
                ok = true;
            }

            Method mDraw = XposedCompat.findDeclaredMethod(earCls, "onDraw", Canvas.class);
            if (mDraw != null) {
                XposedCompat.hookMethod(mDraw, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Object earView = param.thisObject;
                        if (earView == null) {
                            return;
                        }
                        float aodProg = resolveLiveAodProgress(null);
                        if (cfg.isFcEnabled()) {
                            Object sp = readField(earView, "scrimPaint");
                            if (sp instanceof Paint && ((Paint) sp).getAlpha() != 0) {
                                ((Paint) sp).setAlpha(0);
                            }
                        }
                        int earKey = computeHelperKey(cfg, earView, aodProg);
                        Integer prev = LAST_EAR_VER.get(earView);
                        if (prev != null && prev == earKey) {
                            return;
                        }
                        if (earView instanceof View) {
                            ACTIVE_CAPSULE_EARS.put((View) earView, Boolean.TRUE);
                        }
                        LAST_EAR_VER.put(earView, earKey);
                        syncHelperBlurProxies(cl, earView, cfg, aodProg, true);
                    }
                });
                ok = true;
            }
            return ok;
        } catch (Throwable t) {
            cfg.note("FluidCloud CapsuleEarView hook err: " + t);
            return false;
        }
    }

    private static void enableGradMaskPaintDither(View v) {
        if (v == null) {
            return;
        }
        try {
            if (!sGradMaskPaintProbed) {
                for (Field f : v.getClass().getDeclaredFields()) {
                    if (Paint.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        sGradMaskPaintField = f;
                        break;
                    }
                }
                sGradMaskPaintProbed = true;
            }
            Field pf = sGradMaskPaintField;
            if (pf != null) {
                Object pObj = pf.get(v);
                if (pObj instanceof Paint) {
                    ((Paint) pObj).setDither(true);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean hookKeyguardCapsuleMask(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> maskCls = loadClass(C_GRAD_MASK_VIEW, cl);
            Method m = XposedCompat.findDeclaredMethod(maskCls, "onDraw", Canvas.class);
            if (m == null) {
                return false;
            }
            XposedCompat.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Object state = readField(param.thisObject, "clockSizeState");
                    // clockSizeState == 1 is specifically the 180dp dark capsule backdrop on keyguard
                    if (!(state instanceof Integer) || ((Integer) state) != 1) {
                        return;
                    }
                    if (param.thisObject instanceof View) {
                        ACTIVE_GRAD_MASK_VIEWS.put((View) param.thisObject, Boolean.TRUE);
                    }
                    if (!cfg.isFcEnabled()) {
                        if (param.thisObject instanceof View) {
                            View v = (View) param.thisObject;
                            if (Math.abs(v.getAlpha() - 1.0f) > 0.002f) {
                                v.setAlpha(1.0f);
                            }
                            if (LAST_GRAD_MASK_HASH.remove(v) != null) {
                                writeIntField(v, "bottomMaskColor", 0xFF000000);
                                writeField(v, "linearGradient", null);
                                callNoArg(v, "createMaskShader");
                            }
                        }
                        return;
                    }
                    boolean isMedia = sLastLockCustomRowIsMedia;
                    int mediaMaskRgb = resolveMediaMaskRgb(cfg, isMedia, true);
                    boolean hasMediaTint = mediaMaskRgb != 0;
                    boolean hasCustomMask = !hasMediaTint && isCustomMaskAllowed(cfg, isMedia, true);
                    int customMaskArgb = hasCustomMask ? cfg.getFcCustomMaskColor() : 0;
                    float baseMaskScale = resolveMaskAlphaScale(cfg, isMedia, hasMediaTint)
                            * (hasCustomMask ? ConfigStore.colorAlphaScale(customMaskArgb) : 1.0f);
                    float aodProg = easeAodProgress(resolveLiveAodProgress(null));
                    float maskScale = baseMaskScale * (1.0f - aodProg);
                    if (cfg.isFcMatchCard() || maskScale <= 0.001f) {
                        param.setResult(null);
                        return;
                    }
                    if (param.thisObject instanceof View) {
                        View v = (View) param.thisObject;
                        float targetAlpha = clamp01(baseMaskScale) * (1.0f - aodProg);
                        if (Math.abs(v.getAlpha() - targetAlpha) > 0.002f) {
                            v.setAlpha(targetAlpha);
                        }
                        int maskRgb = hasMediaTint ? (mediaMaskRgb & 0x00FFFFFF)
                                : (hasCustomMask ? (customMaskArgb & 0x00FFFFFF) : 0);
                        int maskH = readIntField(v, "capsuleMaskHeight", v.getHeight());
                        if (maskH <= 0) {
                            maskH = v.getHeight();
                        }
                        if (maskH > 0) {
                            int gStart = cfg.getFcMaskGradStartInt();
                            int gEnd = cfg.getFcMaskGradEndInt();
                            int gMid = cfg.getFcMaskGradMidInt();
                            int gradKey = maskH;
                            gradKey = gradKey * 31 + ((hasMediaTint ? 1 : 0) | (hasCustomMask ? 2 : 0));
                            gradKey = gradKey * 31 + maskRgb;
                            gradKey = gradKey * 31 + gStart;
                            gradKey = gradKey * 31 + gEnd;
                            gradKey = gradKey * 31 + gMid;
                            gradKey = gradKey * 31 + Math.round(Math.min(2.0f, baseMaskScale) * 100f);
                            Integer prevKey = LAST_GRAD_MASK_HASH.get(v);
                            if (prevKey == null || prevKey != gradKey) {
                                enableGradMaskPaintDither(v);
                                float boostMul = Math.min(2.0f, Math.max(1.0f, baseMaskScale));
                                int aStart = clampAlpha(Math.round(255f * Math.min(1.0f, boostMul)));
                                int cStart = (aStart << 24) | maskRgb;
                                ConfigStore.SmoothGradientSpec spec = ConfigStore.buildSmoothGradient(
                                        maskRgb, aStart, gStart, gEnd, gMid);
                                writeIntField(v, "bottomMaskColor", cStart);
                                writeField(v, "linearGradient", new LinearGradient(
                                        0f, (float) maskH, 0f, 0f,
                                        spec.colors,
                                        spec.positions,
                                        Shader.TileMode.CLAMP));
                                LAST_GRAD_MASK_HASH.put(v, gradKey);
                            }
                        }
                    }
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("FluidCloud GradientColorImageView hook err: " + t);
            return false;
        }
    }

    private static boolean hookMix(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> mgrCls = loadClass(C_BLUR_MGR, cl);
            Class<?> cardTypeCls = loadClass(C_CARD_TYPE, cl);
            Class<?> blurModeCls = loadClass(C_BLUR_MODE, cl);
            ensureCardTypeConstants(cardTypeCls);
            boolean ok = false;
            Method m = findMethod(mgrCls, "getMixConfig", cardTypeCls, blurModeCls);
            if (m == null) {
                cfg.note("FluidCloud: missing ViewBlurManager.getMixConfig");
            } else {
                XposedCompat.hookDirect(m, new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        Object ct = chain.getArg(0);
                        boolean isCapsule = ct != null && (ct == sCardTypeCapsule || ct == sCardTypeCapsuleEar);
                        boolean isCustom = ct != null && ct == sCardTypeCustom;
                        if (!isCapsule && !isCustom) {
                            return chain.proceed();
                        }
                        Object mgr = chain.getThisObject();
                        if (mgr != null) {
                            WeakReference<Object> ref = sViewBlurManagerRef;
                            if (ref == null || ref.get() != mgr) {
                                sViewBlurManagerRef = new WeakReference<>(mgr);
                            }
                        }
                        Object mode = chain.getArg(1);
                        Object stock = chain.proceed();
                        if (isCustom && !isCustomOnLockscreen(mgr, mode)) {
                            return stock;
                        }
                        if (stock != null) {
                            if (isCapsule && sStockCapsuleMix == null) {
                                sStockCapsuleMix = stock;
                            } else if (isCustom && sStockCustomMix == null) {
                                sStockCustomMix = stock;
                            }
                        }
                        float aodProg = resolveLiveAodProgress(null);
                        if (!cfg.isFcEnabled() || (!cfg.isFcLockTintOrMaskModified() && aodProg <= 0.001f)) {
                            return stock;
                        }
                        try {
                            boolean suppressCustom = isCapsule
                                    ? shouldSuppressCapsuleCustomColor(null, cfg)
                                    : (DynamicIslandHook.hasActiveLockMediaFluidCloud()
                                            && (cfg.isFcNonMediaMaskEnabled() || sLastLockCustomRowIsMedia));
                            Object replaced = buildMix(cl, cfg, stock, aodProg, !suppressCustom);
                            if (replaced != null) {
                                return replaced;
                            }
                        } catch (Throwable ignored) {
                        }
                        return stock;
                    }
                });
                ok = true;
            }

            Method mCustomMix = findMethod(mgrCls, "customCardPlatformMixConfig");
            if (mCustomMix != null) {
                XposedCompat.hookMethod(mCustomMix, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.thisObject != null) {
                            sViewBlurManagerRef = new WeakReference<>(param.thisObject);
                        }
                        if (!isCustomOnLockscreen(param.thisObject, null)) {
                            return;
                        }
                        Object stock = param.getResult();
                        if (stock != null && sStockCustomMix == null) {
                            sStockCustomMix = stock;
                        }
                        float aodProg = resolveLiveAodProgress(null);
                        if (!cfg.isFcEnabled() || (!cfg.isFcLockTintOrMaskModified() && aodProg <= 0.001f)) {
                            return;
                        }
                        try {
                            boolean suppressCustom = DynamicIslandHook.hasActiveLockMediaFluidCloud()
                                    && (cfg.isFcNonMediaMaskEnabled() || sLastLockCustomRowIsMedia);
                            Object replaced = buildMix(cl, cfg, stock, aodProg, !suppressCustom);
                            if (replaced != null) {
                                param.setResult(replaced);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                });
                ok = true;
            }
            return ok;
        } catch (Throwable t) {
            cfg.note("FluidCloud getMixConfig hook err: " + t);
            return false;
        }
    }

    private static int resolveLiveMediaColorArgb() {
        int argb = DynamicIslandHook.getLatestMediaCoverColor();
        if (argb != 0) {
            return argb;
        }
        int raw = DynamicIslandHook.getLastMediaRawRgb();
        return raw != 0 ? (0xFF000000 | raw) : 0;
    }

    private static int resolveLockNotifMediaColorArgb(boolean onLock) {
        if (!onLock || !DynamicIslandHook.hasActiveLockMediaFluidCloud()) {
            return 0;
        }
        return resolveLiveMediaColorArgb();
    }

    private static int[] computeLockNotifGlowColors(int mediaArgb) {
        if ((mediaArgb & 0x00FFFFFF) == 0) {
            return null;
        }
        float[] hsv = new float[3];
        Color.colorToHSV(0xFF000000 | (mediaArgb & 0x00FFFFFF), hsv);
        if (hsv[1] < 0.08f) {
            return null;
        }
        // GradientStrokeEffect uses EdgeType.OVERLAY (u <= 0.5 darkens): keep V=1.0 and cap S <= 0.55
        float strokeS = Math.max(0.38f, Math.min(0.55f, hsv[1] * 0.90f));
        int strokeArgb = Color.HSVToColor(new float[]{hsv[0], strokeS, 1.0f});
        // InnerShadowEffect is additive (color + uFadeColor): allow richer saturation
        float shadowS = Math.max(0.58f, Math.min(0.88f, hsv[1] * 1.25f));
        int shadowArgb = Color.HSVToColor(new float[]{hsv[0], shadowS, 1.0f});
        return new int[]{strokeArgb, shadowArgb};
    }

    private static boolean applyLockNotifHighlightColor(Object stroke, Object isp,
                                                        boolean wantTint, int mediaArgb) {
        int[] colors = wantTint ? computeLockNotifGlowColors(mediaArgb) : null;
        int strokeArgb = colors != null ? colors[0] : 0xFFFFFFFF;
        int shadowArgb = colors != null ? colors[1] : 0xFFFFFFFF;
        if (stroke != null) {
            callSetter(stroke, "setStrokeLineColor", "android.graphics.Color", null,
                    Color.valueOf(strokeArgb));
        }
        if (isp != null) {
            callSetter(isp, "setShadowColor", "android.graphics.Color", null,
                    Color.valueOf(shadowArgb));
        }
        return colors != null;
    }

    private static void syncNormalRowHighlightColor(ClassLoader cl, View row, Object extObj,
                                                    ConfigStore.HookConfig cfg,
                                                    boolean onLock, boolean force) {
        if (row == null) {
            return;
        }
        boolean wantTint = onLock && cfg.isLockNotifGlowFollowMedia()
                && DynamicIslandHook.hasActiveLockMediaFluidCloud();
        int mediaArgb = wantTint ? resolveLiveMediaColorArgb() : 0;
        int key = ((wantTint ? 1 : 0) << 31) ^ (onLock ? 0x40000000 : 0) ^ (mediaArgb & 0x00FFFFFF);
        if (!force) {
            Integer prev = LAST_NORMAL_GLOW_KEY.get(row);
            if (prev != null && prev == key) {
                return;
            }
            if (!wantTint && !Boolean.TRUE.equals(NORMAL_GLOW_TINTED_ROWS.get(row))) {
                LAST_NORMAL_GLOW_KEY.put(row, key);
                return;
            }
        }
        LAST_NORMAL_GLOW_KEY.put(row, key);
        try {
            if (extObj == null) {
                Object bgNormal = readField(row, "mBackgroundNormal");
                extObj = bgNormal != null ? callNoArg(bgNormal, "getExt") : null;
            }
            Object proxy = extObj != null ? callNoArg(extObj, "getViewBlurProxy") : null;
            Object blurCfg = proxy != null ? callNoArg(proxy, "getBlurConfig") : null;
            boolean tinted = false;
            if (blurCfg != null) {
                Object stroke = callNoArg(blurCfg, "getGradientStrokeLineParam");
                Object isp = callNoArg(blurCfg, "getInnerShadowParams");
                tinted |= applyLockNotifHighlightColor(stroke, isp, wantTint, mediaArgb);
                Object pbd = resolvePlatformBlurDrawable(cl, proxy);
                Object bd = pbd != null ? callNoArg(pbd, "getBlurDrawable") : null;
                if (bd != null) {
                    if (stroke != null) {
                        callSetter(bd, "setGradientStrokeLineParams", C_STROKE_PARAMS, cl, stroke);
                    }
                    if (isp != null) {
                        callSetter(bd, "setInnerShadowParams", C_INNER_SHADOW_PARAMS, cl, isp);
                    }
                    if (bd instanceof Drawable) {
                        ((Drawable) bd).invalidateSelf();
                    }
                }
                if (pbd instanceof Drawable) {
                    ((Drawable) pbd).invalidateSelf();
                }
                callNoArg(proxy, "applyBlurConfig");
            }
            Object multiHelper = proxy != null ? callNoArg(proxy, "getMultiPlatformBlurHelper") : null;
            Object cfgMapObj = multiHelper != null ? readField(multiHelper, "configMap") : null;
            if (cfgMapObj instanceof Map) {
                for (Object entryObj : ((Map<?, ?>) cfgMapObj).entrySet()) {
                    if (!(entryObj instanceof Map.Entry)) {
                        continue;
                    }
                    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryObj;
                    Object stackCfg = entry.getValue();
                    if (stackCfg == null) {
                        continue;
                    }
                    Object sStroke = callNoArg(stackCfg, "getGradientStrokeLineParam");
                    Object sIsp = callNoArg(stackCfg, "getInnerShadowParams");
                    tinted |= applyLockNotifHighlightColor(sStroke, sIsp, wantTint, mediaArgb);
                    Object kObj = entry.getKey();
                    if (kObj instanceof Integer) {
                        Object stackPbd = callOneInt(multiHelper, "getStackCacheBlurDrawable", (Integer) kObj);
                        Object stackBd = stackPbd != null ? callNoArg(stackPbd, "getBlurDrawable") : null;
                        if (stackBd != null) {
                            if (sStroke != null) {
                                callSetter(stackBd, "setGradientStrokeLineParams", C_STROKE_PARAMS, cl, sStroke);
                            }
                            if (sIsp != null) {
                                callSetter(stackBd, "setInnerShadowParams", C_INNER_SHADOW_PARAMS, cl, sIsp);
                            }
                            if (stackBd instanceof Drawable) {
                                ((Drawable) stackBd).invalidateSelf();
                            }
                        }
                        if (stackPbd instanceof Drawable) {
                            ((Drawable) stackPbd).invalidateSelf();
                        }
                    }
                }
            }
            if (tinted) {
                NORMAL_GLOW_TINTED_ROWS.put(row, Boolean.TRUE);
                sHasAnyNormalGlowTinted = true;
            } else {
                NORMAL_GLOW_TINTED_ROWS.remove(row);
                synchronized (NORMAL_GLOW_TINTED_ROWS) {
                    if (NORMAL_GLOW_TINTED_ROWS.isEmpty()) {
                        sHasAnyNormalGlowTinted = false;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean hookLockNotifHighlight(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> mgrCls = loadClass(C_PLATFORM_BLUR_MGR, cl);
            Class<?> cardTypeCls = loadClass(C_CARD_TYPE, cl);
            Class<?> blurModeCls = loadClass(C_BLUR_MODE, cl);
            if (mgrCls == null || blurModeCls == null) {
                return false;
            }
            boolean ok = false;
            if (cardTypeCls != null) {
                Method mStroke = findMethod(mgrCls, "getGradientStrokeLineParams", cardTypeCls, blurModeCls);
                if (mStroke != null) {
                    XposedCompat.hookMethod(mStroke, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object stroke = param.getResult();
                            if (stroke == null || !cfg.isLockNotifGlowFollowMedia()) {
                                return;
                            }
                            if (param.args.length < 2 || !"NOTIFICATION".equals(String.valueOf(param.args[0]))) {
                                return;
                            }
                            if (!isCustomOnLockscreen(null, param.args[1])) {
                                return;
                            }
                            int mediaArgb = resolveLockNotifMediaColorArgb(true);
                            if (mediaArgb != 0) {
                                applyLockNotifHighlightColor(stroke, null, true, mediaArgb);
                            }
                        }
                    });
                    ok = true;
                }
                Method mShadow = findMethod(mgrCls, "getInnerShadowParams", cardTypeCls, blurModeCls);
                if (mShadow != null) {
                    XposedCompat.hookMethod(mShadow, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object isp = param.getResult();
                            if (isp == null || !cfg.isLockNotifGlowFollowMedia()) {
                                return;
                            }
                            if (param.args.length < 2 || !"NOTIFICATION".equals(String.valueOf(param.args[0]))) {
                                return;
                            }
                            if (!isCustomOnLockscreen(null, param.args[1])) {
                                return;
                            }
                            int mediaArgb = resolveLockNotifMediaColorArgb(true);
                            if (mediaArgb != 0) {
                                applyLockNotifHighlightColor(null, isp, true, mediaArgb);
                            }
                        }
                    });
                    ok = true;
                }
            }
            Method mStackStroke = findMethod(mgrCls, "getKeyguardStackingGradientStrokeLineParams",
                    int.class, blurModeCls);
            if (mStackStroke != null) {
                XposedCompat.hookMethod(mStackStroke, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object stroke = param.getResult();
                        if (stroke == null || !cfg.isLockNotifGlowFollowMedia() || param.args.length < 2) {
                            return;
                        }
                        if (!isCustomOnLockscreen(null, param.args[1])) {
                            return;
                        }
                        int mediaArgb = resolveLockNotifMediaColorArgb(true);
                        if (mediaArgb != 0) {
                            applyLockNotifHighlightColor(stroke, null, true, mediaArgb);
                        }
                    }
                });
                ok = true;
            }
            Method mStackShadow = findMethod(mgrCls, "getKeyguardStackingInnerShadowParams",
                    int.class, blurModeCls);
            if (mStackShadow != null) {
                XposedCompat.hookMethod(mStackShadow, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object isp = param.getResult();
                        if (isp == null || !cfg.isLockNotifGlowFollowMedia() || param.args.length < 2) {
                            return;
                        }
                        if (!isCustomOnLockscreen(null, param.args[1])) {
                            return;
                        }
                        int mediaArgb = resolveLockNotifMediaColorArgb(true);
                        if (mediaArgb != 0) {
                            applyLockNotifHighlightColor(null, isp, true, mediaArgb);
                        }
                    }
                });
                ok = true;
            }
            return ok;
        } catch (Throwable t) {
            cfg.note("FluidCloud LockNotifHighlight hook err: " + t);
            return false;
        }
    }

    private static boolean hookBottomHighlight(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> mgrCls = loadClass(C_PLATFORM_BLUR_MGR, cl);
            Class<?> blurModeCls = loadClass(C_BLUR_MODE, cl);
            Class<?> innerShadowCls = loadClass(C_INNER_SHADOW_PARAMS, cl);
            Class<?> strokeCls = loadClass(C_STROKE_PARAMS, cl);
            if (mgrCls == null || strokeCls == null || innerShadowCls == null || blurModeCls == null) {
                return false;
            }
            Method mAodNotif = findMethod(mgrCls, "applyAodProgressToNotificationBlurParams",
                    strokeCls, innerShadowCls, float.class, blurModeCls);
            if (mAodNotif == null) {
                return false;
            }
            XposedCompat.hookMethod(mAodNotif, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args.length < 4 || isShadeBlurMode(param.args[3])) {
                        return;
                    }
                    float rawAodProg = (sAodVisualAnimatorRunning || sVisualAodControlled)
                            ? clamp01(sVisualAodProgress)
                            : ((param.args[2] instanceof Float) ? clamp01((Float) param.args[2]) : 0.0f);
                    float eased = easeAodProgress(rawAodProg);
                    float factor = 1.0f - eased;
                    Object stroke = param.args[0];
                    Object isp = param.args[1];
                    Object blurMode = param.args[3];
                    boolean isMediaStroke = stroke != null && Boolean.TRUE.equals(MEDIA_STROKE_SET.get(stroke));
                    boolean isMediaShadow = isp != null && Boolean.TRUE.equals(MEDIA_SHADOW_SET.get(isp));
                    if (cfg.isFcEnabled()) {
                        if (isMediaStroke) {
                            applyBottomHighlightToStroke(stroke, cfg, factor);
                        }
                        if (isMediaShadow) {
                            applyBottomHighlightToInnerShadow(isp, cfg, factor);
                        }
                    }
                    if (cfg.isLockNotifGlowFollowMedia()
                            && (!isMediaStroke || !isMediaShadow)
                            && isCustomOnLockscreen(null, blurMode)) {
                        int mediaArgb = resolveLockNotifMediaColorArgb(true);
                        applyLockNotifHighlightColor(
                                isMediaStroke ? null : stroke,
                                isMediaShadow ? null : isp,
                                mediaArgb != 0,
                                mediaArgb);
                    }
                    if (cfg.isAodNotifTranslucent()) {
                        if (stroke != null && !isMediaStroke) {
                            Object strokeGroup = staticCallOneArg(cl, C_PLATFORM_BLUR_MGR,
                                    "getCurrentNotificationStrokeLineGroup", blurMode);
                            if (strokeGroup != null) {
                                Object nearObj = callNoArg(strokeGroup, "resolveStrokeLineAlphaNear");
                                Object farObj = callNoArg(strokeGroup, "resolveStrokeLineAlphaFar");
                                float rimFactor = 1.0f - 0.65f * eased;
                                if (nearObj instanceof Float) {
                                    callOneFloat(stroke, "setStrokeLineAlphaNear", ((Float) nearObj) * rimFactor);
                                }
                                if (farObj instanceof Float) {
                                    callOneFloat(stroke, "setStrokeLineAlphaFar", ((Float) farObj) * rimFactor);
                                }
                            }
                        }
                        if (isp != null && !isMediaShadow) {
                            Object shadowGroup = staticCallOneArg(cl, C_PLATFORM_BLUR_MGR,
                                    "getCurrentNotificationInnerShadowGroup", blurMode);
                            if (shadowGroup != null) {
                                Object maxObj = callNoArg(shadowGroup, "resolveOffsetShadowMaxAlpha");
                                Object minObj = callNoArg(shadowGroup, "resolveOffsetShadowMinAlpha");
                                Object noOffObj = callNoArg(shadowGroup, "resolveNoOffsetShadowAlpha");
                                if (maxObj instanceof Float) {
                                    callOneFloat(isp, "setOffsetShadowMaxAlpha", ((Float) maxObj) * factor);
                                }
                                if (minObj instanceof Float) {
                                    callOneFloat(isp, "setOffsetShadowMinAlpha", ((Float) minObj) * factor);
                                }
                                if (noOffObj instanceof Float) {
                                    callOneFloat(isp, "setNoOffsetShadowAlpha", ((Float) noOffObj) * factor);
                                }
                            }
                        }
                    }
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("FluidCloud BottomHighlight hook err: " + t);
            return false;
        }
    }

    private static boolean hookMediaCarouselHighlight(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> mgrCls = loadClass(C_BLUR_MGR, cl);
            Class<?> proxyCls = loadClass("com.oplusos.systemui.common.blurability.ViewBlurProxy", cl);
            final Class<?> mediaCls = loadClass(C_MEDIA_CAROUSEL, cl);
            final Class<?> customRowCls = loadClass(C_CUSTOM_ROW_VIEW, cl);
            final Class<?> normalRowCls = loadClass(C_EXPANDABLE_NOTIF_ROW, cl);
            boolean ok = false;

            if (mgrCls != null && proxyCls != null) {
                Method mUpdate = findMethod(mgrCls, "updatePlatformBlurParams", proxyCls, boolean.class);
                if (mUpdate != null) {
                    XposedCompat.hookDirect(mUpdate, new XposedInterface.Hooker() {
                        @Override
                        public Object intercept(XposedInterface.Chain chain) throws Throwable {
                            Object res = chain.proceed();
                            final Object mgr = chain.getThisObject();
                            if (mgr != null) {
                                WeakReference<Object> ref = sViewBlurManagerRef;
                                if (ref == null || ref.get() != mgr) {
                                    sViewBlurManagerRef = new WeakReference<>(mgr);
                                }
                            }
                            final Object proxy = chain.getArg(0);
                            if (proxy == null) {
                                return res;
                            }
                            Method getViewM = sProxyGetViewM;
                            if (getViewM == null || !getViewM.getDeclaringClass().isInstance(proxy)) {
                                getViewM = findSingleTypedMethodCached(
                                        NO_ARG_METHOD_CACHE, proxy.getClass(), "getView", null);
                                sProxyGetViewM = getViewM;
                            }
                            Object viewObj = getViewM != null ? getViewM.invoke(proxy) : null;
                            if (!(viewObj instanceof View)) {
                                return res;
                            }
                            final View v = (View) viewObj;
                            boolean isMedia = mediaCls != null ? mediaCls.isInstance(v)
                                    : C_MEDIA_CAROUSEL.equals(v.getClass().getName());
                            if (!isMedia) {
                                ViewParent vp = v.getParent();
                                if (!(vp instanceof View)) {
                                    return res;
                                }
                                boolean isCustom = customRowCls != null ? customRowCls.isInstance(vp)
                                        : C_CUSTOM_ROW_VIEW.equals(vp.getClass().getName());
                                boolean isNormal = !isCustom && (normalRowCls != null ? normalRowCls.isInstance(vp)
                                        : C_EXPANDABLE_NOTIF_ROW.equals(vp.getClass().getName()));
                                if (!isCustom && !isNormal) {
                                    return res;
                                }
                            }
                            if (Looper.myLooper() != Looper.getMainLooper()) {
                                MAIN_HANDLER.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        handleUpdatePlatformBlurParamsAfter(cl, mgr, proxy, v, cfg);
                                    }
                                });
                                return res;
                            }
                            handleUpdatePlatformBlurParamsAfter(cl, mgr, proxy, v, cfg);
                            return res;
                        }
                    });
                    ok = true;
                }
            }

            if (mediaCls != null) {
                Method mAttach = XposedCompat.findDeclaredMethod(mediaCls, "onAttachedToWindow");
                if (mAttach != null) {
                    XposedCompat.hookMethod(mAttach, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!(param.thisObject instanceof View)) {
                                return;
                            }
                            View v = (View) param.thisObject;
                            Object mgr = readField(v, "viewBlurManager");
                            Object proxy = readField(v, "viewBlurProxy");
                            if (proxy != null) {
                                syncMediaCarouselProxy(cl, mgr, v, proxy, cfg, true);
                            }
                        }
                    });
                    ok = true;
                }

                Method mDraw = XposedCompat.findDeclaredMethod(mediaCls, "onDraw", Canvas.class);
                if (mDraw != null) {
                    XposedCompat.hookMethod(mDraw, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!(param.thisObject instanceof View)) {
                                return;
                            }
                            View v = (View) param.thisObject;
                            Object proxy = readField(v, "viewBlurProxy");
                            if (proxy == null) {
                                return;
                            }
                            int ver = cfg.getFcVersion();
                            int w = v.getWidth();
                            int h = v.getHeight();
                            boolean isShade = Boolean.TRUE.equals(MEDIA_SHADE_STATE.get(v));
                            int[] prev = LAST_MEDIA_STATE.get(v);
                            boolean stateChanged = prev == null
                                    || prev[0] != ver
                                    || prev[1] != w
                                    || prev[2] != h
                                    || prev[3] != (isShade ? 1 : 0);
                            boolean missingTrack = false;
                            if (!isShade && cfg.isFcEnabled()
                                    && cfg.getFcBottomGlowInt() > 0) {
                                Object blurCfg = callNoArg(proxy, "getBlurConfig");
                                Object stroke = blurCfg != null ? callNoArg(blurCfg, "getGradientStrokeLineParam") : null;
                                Object isp = blurCfg != null ? callNoArg(blurCfg, "getInnerShadowParams") : null;
                                missingTrack = (stroke != null && !Boolean.TRUE.equals(MEDIA_STROKE_SET.get(stroke)))
                                        || (isp != null && !Boolean.TRUE.equals(MEDIA_SHADOW_SET.get(isp)));
                            }
                            if (!stateChanged && !missingTrack) {
                                return;
                            }
                            Object mgr = readField(v, "viewBlurManager");
                            syncMediaCarouselProxy(cl, mgr, v, proxy, cfg, true);
                        }
                    });
                    ok = true;
                }
            }
            return ok;
        } catch (Throwable t) {
            cfg.note("FluidCloud OplusMediaCarouselView hook err: " + t);
            return false;
        }
    }

    private static void handleUpdatePlatformBlurParamsAfter(ClassLoader cl, Object mgr, Object proxy,
                                                          View v, ConfigStore.HookConfig cfg) {
        if (proxy == null || v == null) {
            return;
        }
        if (C_MEDIA_CAROUSEL.equals(v.getClass().getName())) {
            syncMediaCarouselProxy(cl, mgr, v, proxy, cfg, true);
        } else if (v.getParent() instanceof View) {
            View parent = (View) v.getParent();
            String parentName = parent.getClass().getName();
            if (C_CUSTOM_ROW_VIEW.equals(parentName)) {
                ACTIVE_CUSTOM_ROWS.put(parent, Boolean.TRUE);
                Object blurCfg = getProxyBlurConfig(proxy);
                boolean onLock = isCustomOnLockscreen(mgr, null);
                if (!onLock) {
                    if (blurCfg != null) {
                        LOCK_CUSTOM_MIX_APPLIED.remove(blurCfg);
                        if (applyScaledBlurRadius(blurCfg, cfg, false)) {
                            callNoArg(proxy, "applyBlurConfig");
                        }
                    }
                    LAST_CUSTOM_MIX_VER.remove(parent);
                    LAST_CUSTOM_LOCK_STATE.remove(parent);
                    LAST_CUSTOM_META_REF.remove(parent);
                } else if (cfg.isFcEnabled() && blurCfg != null) {
                    float rawAod = resolveLiveAodProgress(null);
                    if (hasCustomMixDrifted(cl, parent, cfg, rawAod)) {
                        LAST_CUSTOM_LOCK_STATE.remove(parent);
                        LAST_CUSTOM_MIX_VER.remove(parent);
                        syncCustomRowLockMix(cl, parent, cfg, rawAod, true);
                    }
                }
            } else if (C_EXPANDABLE_NOTIF_ROW.equals(parentName)) {
                ACTIVE_NORMAL_ROWS.put(parent, Boolean.TRUE);
                boolean onLock = isCustomOnLockscreen(mgr, null);
                if (!onLock) {
                    if (sHasAnyNormalGlowTinted && Boolean.TRUE.equals(NORMAL_GLOW_TINTED_ROWS.get(parent))) {
                        syncNormalRowHighlightColor(cl, parent, callNoArg(v, "getExt"), cfg, false, true);
                    }
                } else if (cfg.isLockNotifGlowFollowMedia()
                        || (sHasAnyNormalGlowTinted && Boolean.TRUE.equals(NORMAL_GLOW_TINTED_ROWS.get(parent)))) {
                    syncNormalRowHighlightColor(cl, parent, callNoArg(v, "getExt"), cfg, true, false);
                }
            }
        }
    }

    private static void syncMediaCarouselProxy(ClassLoader cl, Object viewBlurManager, View mediaView,
                                               Object proxy, ConfigStore.HookConfig cfg, boolean applyNow) {
        if (proxy == null || mediaView == null) {
            return;
        }
        try {
            Object mode = resolveMediaBlurMode(cl, viewBlurManager, mediaView);
            Object blurCfg = callNoArg(proxy, "getBlurConfig");
            if (blurCfg == null) {
                return;
            }
            Object stroke = callNoArg(blurCfg, "getGradientStrokeLineParam");
            Object isp = callNoArg(blurCfg, "getInnerShadowParams");
            boolean shade = !isCustomOnLockscreen(viewBlurManager, mode);
            int w = mediaView.getWidth();
            int h = mediaView.getHeight();
            MEDIA_SHADE_STATE.put(mediaView, shade);
            LAST_MEDIA_STATE.put(mediaView, new int[]{cfg.getFcVersion(), w, h, shade ? 1 : 0});

            if (shade || !cfg.isFcEnabled() || cfg.getFcBottomGlowInt() <= 0) {
                boolean restored = false;
                if (stroke != null) {
                    MEDIA_STROKE_SET.remove(stroke);
                    restored |= restoreStrokeParams(stroke);
                }
                if (isp != null) {
                    MEDIA_SHADOW_SET.remove(isp);
                    restored |= restoreShadowParams(isp);
                }
                if (restored && applyNow) {
                    callNoArg(proxy, "applyBlurConfig");
                }
                return;
            }

            if (stroke != null) {
                ensureOrigStrokeSaved(stroke, 1.0f);
                MEDIA_STROKE_SET.put(stroke, Boolean.TRUE);
            }
            if (isp != null) {
                ensureOrigShadowSaved(isp, 1.0f);
                MEDIA_SHADOW_SET.put(isp, Boolean.TRUE);
            }
            applyStockSizeMapping(cl, mediaView.getContext(), stroke, isp, w, h, mode);
            if (stroke != null) {
                applyBottomHighlightToStroke(stroke, cfg, 1.0f);
            }
            if (isp != null) {
                applyBottomHighlightToInnerShadow(isp, cfg, 1.0f);
            }
            if (applyNow && (stroke != null || isp != null)) {
                callNoArg(proxy, "applyBlurConfig");
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object resolveMediaBlurMode(ClassLoader cl, Object viewBlurManager, View mediaView) {
        if (viewBlurManager == null || mediaView == null) {
            return null;
        }
        try {
            Class<?> cardTypeCls = loadClass(C_CARD_TYPE, cl);
            Class<?> blurModeCls = loadClass(C_BLUR_MODE, cl);
            if (cardTypeCls == null || blurModeCls == null) {
                return null;
            }
            Method m = findMethod(viewBlurManager.getClass(), "resolveBlurMode", cardTypeCls, View.class, blurModeCls);
            if (m == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Object notifType = Enum.valueOf((Class<Enum>) cardTypeCls, "NOTIFICATION");
            return m.invoke(viewBlurManager, notifType, mediaView, null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void enterCustomRowHandler() {
        CUSTOM_ROW_HANDLER_ACTIVE_COUNT.incrementAndGet();
        Integer d = TL_CUSTOM_ROW_HANDLER_DEPTH.get();
        TL_CUSTOM_ROW_HANDLER_DEPTH.set(d == null ? 1 : d + 1);
    }

    private static void exitCustomRowHandler() {
        int v = CUSTOM_ROW_HANDLER_ACTIVE_COUNT.decrementAndGet();
        if (v < 0) {
            CUSTOM_ROW_HANDLER_ACTIVE_COUNT.compareAndSet(v, 0);
        }
        Integer d = TL_CUSTOM_ROW_HANDLER_DEPTH.get();
        if (d == null || d <= 1) {
            TL_CUSTOM_ROW_HANDLER_DEPTH.remove();
        } else {
            TL_CUSTOM_ROW_HANDLER_DEPTH.set(d - 1);
        }
    }

    private static boolean hookCustomRow(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> cls = loadClass(C_CUSTOM_ROW, cl);
            if (cls == null) {
                return false;
            }
            boolean hookedAny = false;
            for (Method m : cls.getDeclaredMethods()) {
                String name = m.getName();
                if ("getContentBgColor".equals(name)) {
                    m.setAccessible(true);
                    XposedCompat.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (shouldStripCustomRowMask(cfg, null)) {
                                param.setResult("#00000000");
                            }
                        }
                    });
                    hookedAny = true;
                } else if ("updateDrawableMetaColor".equals(name)) {
                    m.setAccessible(true);
                    XposedCompat.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            enterCustomRowHandler();
                            if (param.args.length > 2 && param.args[2] != null) {
                                boolean isMedia = isMediaEntry(param.args[1]);
                                Boolean prev = META_DRAWABLE_IS_MEDIA.put(param.args[2], isMedia);
                                if (prev == null || prev != isMedia) {
                                    DynamicIslandHook.bumpMediaColorVersion();
                                }
                                if (isCustomOnLockscreen(null, null)) {
                                    sLastLockCustomRowIsMedia = isMedia;
                                }
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                if (!shouldStripCustomRowMask(cfg, null)) {
                                    return;
                                }
                                if (param.args.length > 2 && param.args[2] != null) {
                                    zeroMetaColorSafely(param.args[2]);
                                }
                            } finally {
                                exitCustomRowHandler();
                            }
                        }
                    });
                    hookedAny = true;
                } else if ("applyGradientColorsWithFade".equals(name)) {
                    m.setAccessible(true);
                    XposedCompat.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            enterCustomRowHandler();
                            if (param.args.length > 2 && param.args[2] instanceof Float) {
                                param.setObjectExtra("prevImmFade", TL_IMMERSIVE_MASK_FADE.get());
                                TL_IMMERSIVE_MASK_FADE.set(1.0f - clamp01((Float) param.args[2]));
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                if (param.args.length > 2 && param.args[2] instanceof Float) {
                                    Object prev = param.getObjectExtra("prevImmFade");
                                    if (prev instanceof Float) {
                                        TL_IMMERSIVE_MASK_FADE.set((Float) prev);
                                    } else {
                                        TL_IMMERSIVE_MASK_FADE.remove();
                                    }
                                }
                            } finally {
                                exitCustomRowHandler();
                            }
                        }
                    });
                    hookedAny = true;
                }
            }
            return hookedAny;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hookCustomRowView(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> cls = loadClass(C_CUSTOM_ROW_VIEW, cl);
            if (cls == null) {
                return false;
            }
            boolean ok = false;
            Method setEntry = null;
            for (Method m : cls.getDeclaredMethods()) {
                if ("setEntry".equals(m.getName()) && m.getParameterTypes().length == 1) {
                    setEntry = m;
                    break;
                }
            }
            if (setEntry != null) {
                setEntry.setAccessible(true);
                XposedCompat.hookMethod(setEntry, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!(param.thisObject instanceof View)) {
                            return;
                        }
                        View rowView = (View) param.thisObject;
                        ACTIVE_CUSTOM_ROWS.put(rowView, Boolean.TRUE);
                        LAST_CUSTOM_LOCK_STATE.remove(rowView);
                        LAST_CUSTOM_MASK_VER.remove(rowView);
                        LAST_DRIFT_CHECK_MS.remove(rowView);
                        if (param.args.length > 0 && param.args[0] != null) {
                            boolean isMedia = isMediaEntry(param.args[0]);
                            if (isMedia) {
                                DynamicIslandHook.bumpMediaColorVersion();
                            }
                            Object meta = resolveCustomRowMetaDrawable(cl, rowView, null, null);
                            if (meta != null) {
                                META_DRAWABLE_IS_MEDIA.put(meta, isMedia);
                            }
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!(param.thisObject instanceof View)) {
                            return;
                        }
                        View rowView = (View) param.thisObject;
                        ACTIVE_CUSTOM_ROWS.put(rowView, Boolean.TRUE);
                        Object mgr = readField(rowView, "viewBlurManager");
                        if (mgr != null) {
                            sViewBlurManagerRef = new WeakReference<>(mgr);
                        }
                        boolean onLock = isCustomOnLockscreen(mgr, null);
                        if (!onLock) {
                            restoreShadeCustomRowIfNeeded(cl, mgr, rowView);
                        } else if (cfg.isFcEnabled()) {
                            float aodProg = resolveLiveAodProgress(null);
                            if (hasCustomMixDrifted(cl, rowView, cfg, aodProg)) {
                                LAST_CUSTOM_MIX_VER.remove(rowView);
                                syncCustomRowLockMix(cl, rowView, cfg, aodProg, true);
                            }
                        }
                        if (!onLock || !isRowInImmersiveFade(rowView)) {
                            syncCustomRowMask(cl, rowView, mgr, cfg, false);
                        } else if (readRowImmersiveProgress(rowView) >= 0.99f) {
                            LAST_CUSTOM_MASK_VER.remove(rowView);
                            zeroMetaColorSafely(resolveCustomRowMetaDrawable(cl, rowView, null, null));
                        }
                    }
                });
                ok = true;
            }

            Method mAttach = XposedCompat.findDeclaredMethod(cls, "onAttachedToWindow");
            if (mAttach != null) {
                XposedCompat.hookMethod(mAttach, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!(param.thisObject instanceof View)) {
                            return;
                        }
                        View rowView = (View) param.thisObject;
                        ACTIVE_CUSTOM_ROWS.put(rowView, Boolean.TRUE);
                        attachCustomDetachListenerIfNeeded(rowView);
                        if (isMediaCustomRow(rowView)) {
                            DynamicIslandHook.bumpMediaColorVersion();
                        }
                        Object mgr = readField(rowView, "viewBlurManager");
                        if (mgr != null) {
                            sViewBlurManagerRef = new WeakReference<>(mgr);
                        }
                        boolean onLock = isCustomOnLockscreen(mgr, null);
                        if (!onLock) {
                            restoreShadeCustomRowIfNeeded(cl, mgr, rowView);
                        } else if (cfg.isFcEnabled()) {
                            Object bgObj = readField(rowView, "mBackgroundNormal");
                            Object extObj = bgObj != null ? callNoArg(bgObj, "getExt") : null;
                            tagCustomRowMetaIsMedia(cl, rowView, bgObj, null, true);
                            float aodProg = resolveLiveAodProgress(extObj);
                            syncCustomRowLockMix(cl, rowView, cfg, aodProg, false);
                            DynamicIslandHook.setCardBackgroundViewsAlphaInTree(
                                    rowView, clamp01(1.0f - easeAodProgress(aodProg)));
                            LAST_CUSTOM_MIX_VER.put(rowView, computeCustomMixKey(cfg, rowView, aodProg));
                        }
                        if (!onLock || !isRowInImmersiveFade(rowView)) {
                            syncCustomRowMask(cl, rowView, mgr, cfg, false);
                        }
                    }
                });
                ok = true;
            }

            Method setImm = findMethod(cls, "setImmersiveProgress", float.class);
            if (setImm != null) {
                XposedCompat.hookMethod(setImm, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!(param.thisObject instanceof View)) {
                            return;
                        }
                        View rowView = (View) param.thisObject;
                        float immProg = (param.args.length > 0 && param.args[0] instanceof Float)
                                ? (Float) param.args[0] : 0.0f;
                        LAST_ROW_IMMERSIVE_PROG.put(rowView, immProg);
                        TL_IMMERSIVE_MASK_FADE.set(1.0f - clamp01(immProg));
                        Object mgr = readField(rowView, "viewBlurManager");
                        boolean onLock = isCustomOnLockscreen(mgr, null);
                        tagCustomRowMetaIsMedia(cl, rowView, null, null, onLock);
                        if (onLock && cfg.isFcEnabled()) {
                            syncRowImmersiveEndColors(rowView, cfg, true, resolveLiveAodProgress(null));
                        } else {
                            syncRowImmersiveEndColors(rowView, null, false, 0.0f);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        TL_IMMERSIVE_MASK_FADE.remove();
                        if (!(param.thisObject instanceof View)) {
                            return;
                        }
                        View rowView = (View) param.thisObject;
                        ACTIVE_CUSTOM_ROWS.put(rowView, Boolean.TRUE);
                        Object mgr = readField(rowView, "viewBlurManager");
                        if (mgr != null) {
                            sViewBlurManagerRef = new WeakReference<>(mgr);
                        }
                        float immProg = (param.args.length > 0 && param.args[0] instanceof Float)
                                ? (Float) param.args[0] : 0.0f;
                        LAST_ROW_IMMERSIVE_PROG.put(rowView, immProg);
                        LAST_DRIFT_CHECK_MS.remove(rowView);
                        boolean onLock = isCustomOnLockscreen(mgr, null);
                        if (!onLock) {
                            restoreShadeCustomRowIfNeeded(cl, mgr, rowView);
                            DynamicIslandHook.setCardBackgroundViewsAlphaInTree(rowView, 1.0f);
                            syncCustomRowMask(cl, rowView, mgr, cfg, false);
                            return;
                        }
                        if (!cfg.isFcEnabled()) {
                            syncRowImmersiveEndColors(rowView, null, false, 0.0f);
                            DynamicIslandHook.setCardBackgroundViewsAlphaInTree(rowView, 1.0f);
                            syncCustomRowMask(cl, rowView, mgr, cfg, false);
                            return;
                        }
                        Object bgObj = readField(rowView, "mBackgroundNormal");
                        Object extObj = bgObj != null ? callNoArg(bgObj, "getExt") : null;
                        float aodProg = resolveLiveAodProgress(extObj);
                        DynamicIslandHook.setCardBackgroundViewsAlphaInTree(
                                rowView, clamp01(1.0f - easeAodProgress(aodProg)));
                        syncRowImmersiveEndColors(rowView, cfg, true, aodProg);
                        LAST_CUSTOM_LOCK_STATE.put(
                                rowView, computeCustomLockStateKey(rowView, cfg, true, aodProg));
                        boolean atEndpoint = immProg <= 0.01f || immProg >= 0.99f;
                        if (atEndpoint) {
                            int mixKey = computeCustomMixKey(cfg, rowView, aodProg);
                            Integer prevMix = LAST_CUSTOM_MIX_VER.get(rowView);
                            boolean wantCustomMix = cfg.isFcLockTintOrMaskModified()
                                    || (immProg >= 0.99f && cfg.isFcImmersiveOpacityModified())
                                    || aodProg > 0.001f;
                            boolean needMixSync = wantCustomMix
                                    ? hasCustomMixDrifted(cl, rowView, cfg, aodProg)
                                    : (prevMix == null || prevMix != mixKey);
                            LAST_CUSTOM_MIX_VER.put(rowView, mixKey);
                            if (needMixSync) {
                                syncCustomRowLockMix(cl, rowView, cfg, aodProg, true);
                                if (bgObj instanceof View) {
                                    ((View) bgObj).invalidate();
                                }
                                DynamicIslandHook.invalidateMultiLightViewsInTree(rowView);
                            }
                        }
                        if (immProg <= 0.01f) {
                            Object proxy = extObj != null ? callNoArg(extObj, "getViewBlurProxy") : null;
                            Object metaDr = resolveCustomRowMetaDrawable(cl, rowView, bgObj, proxy);
                            int maskKey = computeCustomMaskKey(rowView, cfg, true, aodProg);
                            Integer prevMask = LAST_CUSTOM_MASK_VER.get(rowView);
                            if (prevMask == null || prevMask != maskKey
                                    || (metaDr != null && LAST_CUSTOM_META_REF.get(rowView) != metaDr)) {
                                syncCustomRowMask(cl, rowView, mgr, cfg, false);
                            }
                        } else {
                            LAST_CUSTOM_MASK_VER.remove(rowView);
                            if (immProg >= 0.99f) {
                                Object proxy = extObj != null ? callNoArg(extObj, "getViewBlurProxy") : null;
                                zeroMetaColorSafely(resolveCustomRowMetaDrawable(cl, rowView, bgObj, proxy));
                            }
                        }
                    }
                });
                ok = true;
            }

            Method mDispatch = XposedCompat.findDeclaredMethod(cls, "dispatchDraw", Canvas.class);
            if (mDispatch != null) {
                XposedCompat.hookDirect(mDispatch, new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        Object self = chain.getThisObject();
                        if (self instanceof View) {
                            onCustomRowDispatchDrawBefore(cl, (View) self, cfg);
                        }
                        return chain.proceed();
                    }
                });
                ok = true;
            }
            return ok;
        } catch (Throwable t) {
            cfg.note("FluidCloud OplusCustomRow hook err: " + t);
            return false;
        }
    }

    private static void onCustomRowDispatchDrawBefore(ClassLoader cl, View rowView, ConfigStore.HookConfig cfg) {
                        Object mgr = readField(rowView, "viewBlurManager");
                        if (mgr != null && (sViewBlurManagerRef == null || sViewBlurManagerRef.get() != mgr)) {
                            sViewBlurManagerRef = new WeakReference<>(mgr);
                        }
                        boolean onLock = isCustomOnLockscreen(mgr, null);
                        float rawAodProg = onLock ? resolveLiveAodProgress(null) : 0.0f;
                        long lockStateKey = computeCustomLockStateKey(rowView, cfg, onLock, rawAodProg);
                        Long prevState = LAST_CUSTOM_LOCK_STATE.get(rowView);
                        float immProg = onLock ? readRowImmersiveProgress(rowView) : 0.0f;
                        boolean inMidImm = onLock && immProg > 0.01f && immProg < 0.99f;
                        boolean inImmFade = onLock && cfg.isFcEnabled() && immProg > 0.01f;
                        boolean mixDrifted = false;
                        boolean maskDrifted = false;
                        if (prevState != null && prevState == lockStateKey) {
                            if (!onLock || !cfg.isFcEnabled() || inMidImm) {
                                return;
                            }
                            long now = SystemClock.uptimeMillis();
                            Long lastDriftMs = LAST_DRIFT_CHECK_MS.get(rowView);
                            if (lastDriftMs == null || (now - lastDriftMs) >= 500L) {
                                LAST_DRIFT_CHECK_MS.put(rowView, now);
                                mixDrifted = hasCustomMixDrifted(cl, rowView, cfg, rawAodProg);
                                if (!inImmFade) {
                                    Object metaDr = resolveCustomRowMetaDrawable(cl, rowView, null, null);
                                    if (metaDr != null) {
                                        int curMaskKey = computeCustomMaskKey(rowView, cfg, true, rawAodProg);
                                        Integer prevMaskVer = LAST_CUSTOM_MASK_VER.get(rowView);
                                        if (LAST_CUSTOM_META_REF.get(rowView) != metaDr
                                                || prevMaskVer == null || prevMaskVer != curMaskKey) {
                                            maskDrifted = true;
                                        }
                                    }
                                }
                            }
                            if (!mixDrifted && !maskDrifted) {
                                return;
                            }
                        } else {
                            if (onLock && cfg.isFcEnabled()) {
                                DynamicIslandHook.setCardBackgroundViewsAlphaInTree(
                                        rowView, clamp01(1.0f - easeAodProgress(rawAodProg)));
                                if (!inMidImm) {
                                    mixDrifted = hasCustomMixDrifted(cl, rowView, cfg, rawAodProg);
                                }
                            }
                            if (cfg.isFcEnabled() && !inImmFade) {
                                Object metaDr = resolveCustomRowMetaDrawable(cl, rowView, null, null);
                                if (metaDr != null) {
                                    int curMaskKey = computeCustomMaskKey(rowView, cfg, onLock, rawAodProg);
                                    Integer prevMaskVer = LAST_CUSTOM_MASK_VER.get(rowView);
                                    if (LAST_CUSTOM_META_REF.get(rowView) != metaDr
                                            || prevMaskVer == null || prevMaskVer != curMaskKey) {
                                        maskDrifted = true;
                                    }
                                }
                            }
                        }
                        if (mixDrifted) {
                            LAST_CUSTOM_MIX_VER.remove(rowView);
                        }
                        if (maskDrifted) {
                            LAST_CUSTOM_MASK_VER.remove(rowView);
                        }
                        LAST_CUSTOM_LOCK_STATE.put(rowView, lockStateKey);
                        ACTIVE_CUSTOM_ROWS.put(rowView, Boolean.TRUE);
                        if (!onLock) {
                            restoreShadeCustomRowIfNeeded(cl, mgr, rowView);
                            DynamicIslandHook.setCardBackgroundViewsAlphaInTree(rowView, 1.0f);
                        } else {
                            if (!cfg.isFcEnabled()) {
                                syncRowImmersiveEndColors(rowView, null, false, 0.0f);
                                DynamicIslandHook.setCardBackgroundViewsAlphaInTree(rowView, 1.0f);
                            } else {
                                syncRowImmersiveEndColors(rowView, cfg, true, rawAodProg);
                            }
                            if (!inMidImm) {
                                int mixKey = computeCustomMixKey(cfg, rowView, rawAodProg);
                                Integer prev = LAST_CUSTOM_MIX_VER.get(rowView);
                                boolean wantCustomMix = cfg.isFcEnabled()
                                        && (cfg.isFcLockTintOrMaskModified()
                                                || (immProg >= 0.99f && cfg.isFcImmersiveOpacityModified())
                                                || rawAodProg > 0.001f);
                                if (prev == null || prev != mixKey) {
                                    LAST_CUSTOM_MIX_VER.put(rowView, mixKey);
                                    if (!wantCustomMix
                                            || mixDrifted || hasCustomMixDrifted(cl, rowView, cfg, rawAodProg)) {
                                        syncCustomRowLockMix(cl, rowView, cfg, rawAodProg, true);
                                        DynamicIslandHook.invalidateMultiLightViewsInTree(rowView);
                                    }
                                }
                            }
                        }
                        if (!inImmFade) {
                            Object metaDr = resolveCustomRowMetaDrawable(cl, rowView, null, null);
                            int maskKey = computeCustomMaskKey(rowView, cfg, onLock, rawAodProg);
                            Integer prevMask = LAST_CUSTOM_MASK_VER.get(rowView);
                            if (metaDr != null && (prevMask == null || prevMask != maskKey
                                    || LAST_CUSTOM_META_REF.get(rowView) != metaDr)) {
                                syncCustomRowMask(cl, rowView, mgr, cfg, true);
                            }
                        } else {
                            LAST_CUSTOM_MASK_VER.remove(rowView);
                            if (immProg >= 0.99f) {
                                zeroMetaColorSafely(resolveCustomRowMetaDrawable(cl, rowView, null, null));
                            }
                        }
    }

    private static float resolveDisplayDensity() {
        float dp = sDisplayDensity;
        if (dp <= 0.0f) {
            dp = 3.5f;
            try {
                dp = Resources.getSystem().getDisplayMetrics().density;
            } catch (Throwable ignored) {
            }
            sDisplayDensity = dp;
        }
        return dp;
    }

    private static Object resolveCustomRowMetaDrawable(ClassLoader cl, View rowView,
                                                      Object bgNormal, Object proxy) {
        if (rowView == null) {
            return null;
        }
        try {
            if (bgNormal == null) {
                bgNormal = readField(rowView, "mBackgroundNormal");
            }
            if (proxy == null && bgNormal != null) {
                Object ext = callNoArg(bgNormal, "getExt");
                if (ext != null) {
                    proxy = callNoArg(ext, "getViewBlurProxy");
                }
            }
            if (proxy == null) {
                return null;
            }
            Drawable bgDr = (bgNormal instanceof View) ? ((View) bgNormal).getBackground() : null;
            Drawable blurDrawable = resolveBlurDrawable(proxy, null);
            if (blurDrawable == null && bgDr != null) {
                blurDrawable = resolveBlurDrawable(proxy, bgDr);
            }
            if (blurDrawable == null) {
                Object pbd = resolvePlatformBlurDrawable(cl, proxy);
                if (pbd instanceof Drawable) {
                    blurDrawable = (Drawable) pbd;
                }
            }
            Object meta = staticCallOneArg(cl, C_ROW_PROVIDER, "tryGetMetaBallBlurDrawable", blurDrawable);
            if (meta != null) {
                META_DRAWABLE_TO_ROW.put(meta, rowView);
                sAnyMetaRowMapped = true;
            }
            return meta;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static float readRowImmersiveProgress(View rowView) {
        if (rowView == null) {
            return 0.0f;
        }
        Object ex = readField(rowView, "mExpandableViewEx");
        Object prog = ex != null ? callNoArg(ex, "getImmersiveProgress") : null;
        if (prog instanceof Float) {
            return (Float) prog;
        }
        Float cached = LAST_ROW_IMMERSIVE_PROG.get(rowView);
        return cached != null ? cached : 0.0f;
    }

    private static boolean isRowInImmersiveFade(View rowView) {
        return readRowImmersiveProgress(rowView) > 0.01f;
    }

    private static void syncRowImmersiveEndColors(View rowView, ConfigStore.HookConfig cfg,
                                                  boolean onLock, float rawAodProg) {
        if (rowView == null) {
            return;
        }
        try {
            int curTop = readIntField(rowView, "immersiveEndTopLayerColor", 0);
            int curBot = readIntField(rowView, "immersiveEndBottomLayerColor", 0);
            int curTopSp = readIntField(rowView, "immersiveEndTopLayerSpecialColor", 0);
            int curBotSp = readIntField(rowView, "immersiveEndBottomLayerSpecialColor", 0);
            int[] orig = ORIG_ROW_IMMERSIVE_COLORS.get(rowView);
            if (orig == null) {
                if (curTop == 0 && curBot == 0 && curTopSp == 0 && curBotSp == 0) {
                    return;
                }
                orig = new int[]{curTop, curBot, curTopSp, curBotSp};
                ORIG_ROW_IMMERSIVE_COLORS.put(rowView, orig);
            }
            boolean applyCustom = onLock && cfg != null && cfg.isFcEnabled()
                    && (cfg.isFcLockTintOrMaskModified()
                            || cfg.isFcImmersiveOpacityModified()
                            || rawAodProg > 0.001f);
            int tTop;
            int tBot;
            int tTopSp;
            int tBotSp;
            if (applyCustom) {
                boolean customMixOn = cfg.isFcLockTintOrMaskModified()
                        || cfg.isFcImmersiveOpacityModified();
                int[] exp = computeExpectedMixFgColorsForOpacity(
                        cfg, cfg.getFcImmersiveOpacity(), clamp01(rawAodProg), customMixOn,
                        !shouldSuppressLockCustomColor(rowView, cfg));
                if (exp == null || exp.length < 2) {
                    return;
                }
                tTop = exp[0];
                tBot = exp[1];
                tTopSp = exp[0];
                tBotSp = exp[1];
            } else {
                tTop = orig[0];
                tBot = orig[1];
                tTopSp = orig[2];
                tBotSp = orig[3];
            }
            if (curTop != tTop) {
                writeIntField(rowView, "immersiveEndTopLayerColor", tTop);
            }
            if (curBot != tBot) {
                writeIntField(rowView, "immersiveEndBottomLayerColor", tBot);
            }
            if (curTopSp != tTopSp) {
                writeIntField(rowView, "immersiveEndTopLayerSpecialColor", tTopSp);
            }
            if (curBotSp != tBotSp) {
                writeIntField(rowView, "immersiveEndBottomLayerSpecialColor", tBotSp);
            }
        } catch (Throwable ignored) {
        }
    }

    private static View findEnclosingCapsuleItem(View start) {
        View cur = start;
        for (int depth = 0; depth < 24 && cur != null; depth++) {
            if (C_CAPSULE_ITEM.equals(cur.getClass().getName())) {
                return cur;
            }
            Object p = cur.getParent();
            cur = p instanceof View ? (View) p : null;
        }
        return null;
    }

    private static void tagCustomRowMetaIsMedia(ClassLoader cl, View rowView, Object bgNormal,
                                                Object proxy, boolean onLock) {
        if (rowView == null) {
            return;
        }
        Object meta = resolveCustomRowMetaDrawable(cl, rowView, bgNormal, proxy);
        if (meta != null) {
            boolean isMedia = isMediaCustomRow(rowView);
            META_DRAWABLE_IS_MEDIA.put(meta, isMedia);
            if (onLock) {
                sLastLockCustomRowIsMedia = isMedia;
            }
        }
    }

    private static boolean argbNearEqual(int c1, int c2, int tol) {
        if (c1 == c2) {
            return true;
        }
        int a1 = (c1 >>> 24) & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF;
        if (a1 == 0 && a2 == 0) {
            return true;
        }
        return Math.abs(a1 - a2) <= tol
                && Math.abs(((c1 >> 16) & 0xFF) - ((c2 >> 16) & 0xFF)) <= tol
                && Math.abs(((c1 >> 8) & 0xFF) - ((c2 >> 8) & 0xFF)) <= tol
                && Math.abs((c1 & 0xFF) - (c2 & 0xFF)) <= tol;
    }

    private static boolean isMixMatchingExpected(Object mixObj, int expTop, int expBottom) {
        if (mixObj == null) {
            return false;
        }
        Object fg = callNoArg(mixObj, "getForegroundShaderParam");
        if (fg == null) {
            return Boolean.TRUE.equals(BUILT_MIX_SET.get(mixObj));
        }
        Object topColor = callNoArg(fg, "getTopLayerColor");
        Object botColor = callNoArg(fg, "getBottomLayerColor");
        Object topMode = readField(fg, "topMode");
        Object botMode = readField(fg, "bottomMode");
        if (!(topMode instanceof Integer) || ((Integer) topMode) != 1
                || !(botMode instanceof Integer) || ((Integer) botMode) != 2
                || !(topColor instanceof Integer) || !(botColor instanceof Integer)) {
            return false;
        }
        if (!argbNearEqual((Integer) topColor, expTop, 2)
                || !argbNearEqual((Integer) botColor, expBottom, 2)) {
            return false;
        }
        Object bg = callNoArg(mixObj, "getBackgroundShaderParam");
        if (bg != null) {
            Object bgTop = callNoArg(bg, "getTopLayerColor");
            Object bgBot = callNoArg(bg, "getBottomLayerColor");
            if ((bgTop instanceof Integer && ((((Integer) bgTop) >>> 24) & 0xFF) != 0)
                    || (bgBot instanceof Integer && ((((Integer) bgBot) >>> 24) & 0xFF) != 0)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasCustomMixDrifted(ClassLoader cl, View rowView,
                                               ConfigStore.HookConfig cfg, float rawAodProg) {
        if (rowView == null || !cfg.isFcEnabled()) {
            return false;
        }
        float immProg = readRowImmersiveProgress(rowView);
        if (immProg > 0.01f && immProg < 0.99f) {
            return false;
        }
        Object bgNormal = readField(rowView, "mBackgroundNormal");
        Object ext = bgNormal != null ? callNoArg(bgNormal, "getExt") : null;
        Object proxy = ext != null ? callNoArg(ext, "getViewBlurProxy") : null;
        Object blurCfg = proxy != null ? getProxyBlurConfig(proxy) : null;
        if (blurCfg == null) {
            return false;
        }
        if (hasBlurRadiusDrifted(blurCfg, cfg)) {
            return true;
        }
        boolean atImmersive = immProg >= 0.99f;
        boolean wantActive = atImmersive
                ? (cfg.isFcLockTintOrMaskModified() || cfg.isFcImmersiveOpacityModified() || rawAodProg > 0.001f)
                : (cfg.isFcLockTintOrMaskModified() || rawAodProg > 0.001f);
        if (!wantActive) {
            return false;
        }
        Object curMix = callNoArg(blurCfg, "getPlatformMixConfig");
        if (curMix == null) {
            return false;
        }
        boolean allowCustomColor = !shouldSuppressLockCustomColor(rowView, cfg);
        int[] exp = atImmersive
                ? computeExpectedMixFgColorsForOpacity(
                        cfg,
                        cfg.getFcImmersiveOpacity(),
                        rawAodProg,
                        cfg.isFcLockTintOrMaskModified() || cfg.isFcImmersiveOpacityModified(),
                        allowCustomColor)
                : computeExpectedMixFgColors(cfg, rawAodProg, allowCustomColor);
        if (!isMixMatchingExpected(curMix, exp[0], exp[1])) {
            return true;
        }
        Object pbd = resolvePlatformBlurDrawable(cl, proxy);
        Object curPbdMix = pbd != null ? callNoArg(pbd, "getCurrentBlurMixConfig") : null;
        return curPbdMix != null && !isMixMatchingExpected(curPbdMix, exp[0], exp[1]);
    }

    private static RowExtInfo resolveExtRowInfo(Object ext, Class<?> normalRowCls, Class<?> customRowCls) {
        RowExtInfo info = EXT_ROW_INFO.get(ext);
        if (info != null) {
            View bg = info.bgRef.get();
            View parent = info.parentRef.get();
            if (bg != null && parent != null && bg.getParent() == parent) {
                return info;
            }
        }
        View bgView = null;
        try {
            Method mBg = sExtGetBgViewM;
            if (mBg == null || !mBg.getDeclaringClass().isInstance(ext)) {
                mBg = findSingleTypedMethodCached(NO_ARG_METHOD_CACHE, ext.getClass(), "getBgView", null);
                sExtGetBgViewM = mBg;
            }
            Object bgObj = mBg != null ? mBg.invoke(ext) : null;
            if (bgObj instanceof View) {
                bgView = (View) bgObj;
            }
        } catch (Throwable ignored) {
        }
        if (bgView == null) {
            return null;
        }
        ViewParent vp = bgView.getParent();
        if (!(vp instanceof View)) {
            return null;
        }
        View parentView = (View) vp;
        byte kind = 0;
        if (customRowCls != null ? customRowCls.isInstance(parentView)
                : C_CUSTOM_ROW_VIEW.equals(parentView.getClass().getName())) {
            kind = 2;
        } else if (normalRowCls != null ? normalRowCls.isInstance(parentView)
                : C_EXPANDABLE_NOTIF_ROW.equals(parentView.getClass().getName())) {
            kind = 1;
        }
        info = new RowExtInfo(bgView, parentView, kind);
        EXT_ROW_INFO.put(ext, info);
        return info;
    }

    private static void onCustomRowBgDrawBefore(ClassLoader cl, Object ext, View bgView, View parentView,
                                                Object mgr, ConfigStore.HookConfig cfg) {
        boolean onLock = isCustomOnLockscreen(mgr, null);
        float rawAodProg = onLock ? resolveLiveAodProgress(ext) : 0.0f;
        long lockStateKey = computeCustomLockStateKey(parentView, cfg, onLock, rawAodProg);
        Long prevState = LAST_CUSTOM_LOCK_STATE.get(parentView);
        float immProg = onLock ? readRowImmersiveProgress(parentView) : 0.0f;
        boolean inMidImm = onLock && immProg > 0.01f && immProg < 0.99f;
        boolean inImmFade = onLock && cfg.isFcEnabled() && immProg > 0.01f;
        if (prevState != null && prevState == lockStateKey) {
            if (!onLock || !cfg.isFcEnabled() || inMidImm) {
                return;
            }
            long now = SystemClock.uptimeMillis();
            Long lastDriftMs = LAST_DRIFT_CHECK_MS.get(parentView);
            if (lastDriftMs != null && (now - lastDriftMs) < 500L) {
                return;
            }
        }
        Object proxy = readExtProxy(ext);
        LAST_CUSTOM_LOCK_STATE.put(parentView, lockStateKey);
        if (onLock && cfg.isFcEnabled()) {
            syncRowImmersiveEndColors(parentView, cfg, true, rawAodProg);
            if (!inMidImm && hasCustomMixDrifted(cl, parentView, cfg, rawAodProg)) {
                LAST_CUSTOM_MIX_VER.remove(parentView);
                syncCustomRowLockMix(cl, parentView, cfg, rawAodProg, true);
            }
        }
        if (!inImmFade) {
            Object metaDr = resolveCustomRowMetaDrawable(cl, parentView, bgView, proxy);
            int maskKey = computeCustomMaskKey(parentView, cfg, onLock, rawAodProg);
            Integer prevMask = LAST_CUSTOM_MASK_VER.get(parentView);
            if (metaDr != null && (LAST_CUSTOM_META_REF.get(parentView) != metaDr
                    || prevMask == null || prevMask != maskKey)) {
                LAST_CUSTOM_MASK_VER.remove(parentView);
                syncCustomRowMask(cl, parentView, mgr, cfg, true);
            }
        } else {
            LAST_CUSTOM_MASK_VER.remove(parentView);
            if (immProg >= 0.99f) {
                zeroMetaColorSafely(resolveCustomRowMetaDrawable(cl, parentView, bgView, proxy));
            }
        }
    }

    private static Object readExtProxy(Object ext) {
        if (ext == null) {
            return null;
        }
        try {
            Method m = sExtGetProxyM;
            if (m == null || !m.getDeclaringClass().isInstance(ext)) {
                m = findSingleTypedMethodCached(NO_ARG_METHOD_CACHE, ext.getClass(), "getViewBlurProxy", null);
                sExtGetProxyM = m;
            }
            return m != null ? m.invoke(ext) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void attachCustomDetachListenerIfNeeded(View rowView) {
        if (rowView != null && ATTACHED_CUSTOM_DETACH_LISTENER.get(rowView) == null) {
            ATTACHED_CUSTOM_DETACH_LISTENER.put(rowView, Boolean.TRUE);
            rowView.addOnAttachStateChangeListener(CUSTOM_ROW_DETACH_LISTENER);
        }
    }

    private static float readRowTranslationX(View rowView) {
        try {
            Object txObj = callNoArg(rowView, "getTranslation");
            if (txObj instanceof Number) {
                return ((Number) txObj).floatValue();
            }
        } catch (Throwable ignored) {
        }
        return 0.0f;
    }

    private static boolean isCollapsedByHorizontalSwipe(View bgView, float absTx, float cornerRadius) {
        if (absTx <= 0.5f) {
            return false;
        }
        try {
            Object awObj = callNoArg(bgView, "getActualWidth");
            int actualW = (awObj instanceof Integer && ((Integer) awObj) > 0)
                    ? (Integer) awObj : bgView.getWidth();
            return (actualW - absTx) <= 3.0f * cornerRadius;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void drawCustomRowSmoothMask(View rowView, View bgView, Object proxy,
                                                Drawable blurDr, Canvas canvas, CustomRowMaskSpec spec) {
        if (spec == null || spec.peakAlpha <= 0) {
            return;
        }
        int effAlpha = clampAlpha(Math.round(spec.peakAlpha * (blurDr.getAlpha() / 255.0f)));
        if (effAlpha <= 0) {
            return;
        }
        Rect bounds = blurDr.getBounds();
        int w = bounds.width();
        int h = bounds.height();
        if (w < 8 || h < 8) {
            return;
        }
        CachedMaskDraw cache = CUSTOM_ROW_MASK_CACHE.get(rowView);
        if (cache == null) {
            cache = new CachedMaskDraw();
            CUSTOM_ROW_MASK_CACHE.put(rowView, cache);
        }
        float[] cc = extractCustomRowCornerConfig(blurDr, proxy, w, h, resolveDisplayDensity());
        float cornerMode = cc[0];
        float cornerRadius = cc[1];
        float cornerWeight = cc[2];
        float tx = readRowTranslationX(rowView);
        if (isCollapsedByHorizontalSwipe(bgView, Math.abs(tx), cornerRadius)) {
            return;
        }
        boolean useG2 = cornerMode > 1.5f && cornerRadius > 0.0f;
        boolean geomChanged = cache.width != w || cache.height != h
                || cache.cornerMode != cornerMode
                || cache.cornerRadius != cornerRadius
                || cache.cornerWeight != cornerWeight;
        if (useG2) {
            if (cache.smoothPath == null || geomChanged) {
                if (cache.smoothPath == null) {
                    cache.smoothPath = new Path();
                }
                DynamicIslandHook.buildSmoothRoundRectPath(
                        cache.smoothPath, 0f, 0f, (float) w, (float) h,
                        cornerRadius, cornerWeight, true);
            }
        } else {
            cache.smoothPath = null;
        }
        cache.cornerMode = cornerMode;
        cache.cornerRadius = cornerRadius;
        cache.cornerWeight = cornerWeight;
        if (cache.shader == null
                || cache.height != h
                || cache.rgb != spec.rgb
                || cache.startPct != spec.startPct
                || cache.endPct != spec.endPct
                || cache.midPct != spec.midPct) {
            ConfigStore.SmoothGradientSpec gradSpec = ConfigStore.buildSmoothGradient(
                    spec.rgb, 255, spec.startPct, spec.endPct, spec.midPct);
            cache.shader = new LinearGradient(
                    0f, 0f, 0f, (float) h,
                    gradSpec.colors, gradSpec.positions, Shader.TileMode.CLAMP);
            cache.paint.setShader(cache.shader);
            cache.rgb = spec.rgb;
            cache.startPct = spec.startPct;
            cache.endPct = spec.endPct;
            cache.midPct = spec.midPct;
            cache.height = h;
        }
        cache.width = w;
        cache.paint.setAlpha(effAlpha);
        int sc = canvas.save();
        try {
            canvas.translate(bounds.left + (int) tx, bounds.top);
            if (useG2 && cache.smoothPath != null && !cache.smoothPath.isEmpty()) {
                canvas.drawPath(cache.smoothPath, cache.paint);
            } else {
                canvas.drawRoundRect(0f, 0f, (float) w, (float) h, cornerRadius, cornerRadius, cache.paint);
            }
        } finally {
            canvas.restoreToCount(sc);
        }
    }

    private static void onCustomRowBgDrawAfter(ClassLoader cl, Object ext, View rowView, View bgView,
                                               Object mgr, Canvas canvas, Drawable fallback,
                                               ConfigStore.HookConfig cfg) {
        Object proxy = readExtProxy(ext);
        Drawable blurDr = resolveBlurDrawable(proxy, fallback);
        if (blurDr == null) {
            return;
        }
        if (!CUSTOM_MASK_BY_META.isEmpty()) {
            Object metaDr = CUSTOM_MASK_BY_META.containsKey(blurDr)
                    ? blurDr : LAST_CUSTOM_META_REF.get(rowView);
            CustomRowMaskSpec spec = metaDr != null ? CUSTOM_MASK_BY_META.get(metaDr) : null;
            if (spec == null) {
                Object resolved = resolveCustomRowMetaDrawable(cl, rowView, bgView, proxy);
                if (resolved != null) {
                    spec = CUSTOM_MASK_BY_META.get(resolved);
                }
            }
            if (spec != null) {
                drawCustomRowSmoothMask(rowView, bgView, proxy, blurDr, canvas, spec);
            }
        }
        if (Build.VERSION.SDK_INT < 33 || !isCustomOnLockscreen(mgr, null)) {
            return;
        }
        int h = blurDr.getBounds().height();
        if (h <= 0) {
            h = bgView.getHeight();
        }
        boolean isLargeMedia = DynamicIslandHook.isLargeMediaCardHeight(
                isMediaCustomRow(rowView), h, resolveDisplayDensity());
        int glowPct = isLargeMedia ? cfg.getFcBottomGlowInt() : cfg.getFcSingleGlowInt();
        if (glowPct <= 0) {
            return;
        }
        int chromaPct = isLargeMedia ? cfg.getFcBottomGlowChromaInt() : cfg.getFcSingleGlowChromaInt();
        drawCustomRowBottomGlow(ext, rowView, bgView, proxy, blurDr, canvas, glowPct, chromaPct, cfg);
    }

    private static boolean hookCustomRowBgDraw(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> extCls = loadClass(C_BG_VIEW_EXT, cl);
            if (extCls == null) {
                cfg.note("FluidCloud: missing NotificationBackgroundViewExtImp");
                return false;
            }
            Method mDraw = XposedCompat.findDeclaredMethod(extCls, "draw", Canvas.class, Drawable.class);
            if (mDraw == null) {
                cfg.note("FluidCloud: missing NotificationBackgroundViewExtImp.draw");
                return false;
            }
            final Class<?> normalRowCls = loadClass(C_EXPANDABLE_NOTIF_ROW, cl);
            final Class<?> customRowCls = loadClass(C_CUSTOM_ROW_VIEW, cl);
            XposedCompat.hookDirect(mDraw, new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object ext = chain.getThisObject();
                    if (ext == null) {
                        return chain.proceed();
                    }
                    RowExtInfo info = resolveExtRowInfo(ext, normalRowCls, customRowCls);
                    if (info == null || info.kind == 0) {
                        return chain.proceed();
                    }
                    View parentView = info.parentRef.get();
                    View bgView = info.bgRef.get();
                    if (parentView == null || bgView == null) {
                        return chain.proceed();
                    }
                    if (info.kind == 1) {
                        if (!info.registered) {
                            ACTIVE_NORMAL_ROWS.put(parentView, Boolean.TRUE);
                            info.registered = true;
                        }
                        if (cfg.isLockNotifGlowFollowMedia() || sHasAnyNormalGlowTinted) {
                            boolean onLock = isCustomOnLockscreen(null, null);
                            if (onLock || sHasAnyNormalGlowTinted) {
                                syncNormalRowHighlightColor(cl, parentView, ext, cfg, onLock, false);
                            }
                        }
                        return chain.proceed();
                    }
                    if (!info.registered) {
                        ACTIVE_CUSTOM_ROWS.put(parentView, Boolean.TRUE);
                        attachCustomDetachListenerIfNeeded(parentView);
                        info.registered = true;
                    }
                    Object mgr = info.blurMgr;
                    if (mgr == null) {
                        mgr = readField(parentView, "viewBlurManager");
                        info.blurMgr = mgr;
                    }
                    TL_IN_BG_DRAW_BEFORE.set(Boolean.TRUE);
                    try {
                        onCustomRowBgDrawBefore(cl, ext, bgView, parentView, mgr, cfg);
                    } finally {
                        TL_IN_BG_DRAW_BEFORE.remove();
                    }
                    Object res = chain.proceed();
                    if (cfg.isFcEnabled() && bgView.getVisibility() == View.VISIBLE) {
                        Object arg0 = chain.getArg(0);
                        if (arg0 instanceof Canvas) {
                            Object arg1 = chain.getArg(1);
                            Drawable fallback = arg1 instanceof Drawable ? (Drawable) arg1 : null;
                            onCustomRowBgDrawAfter(cl, ext, parentView, bgView, mgr, (Canvas) arg0, fallback, cfg);
                        }
                    }
                    return res;
                }
            });

            Method mStackMask = findMethod(extCls, "setStackedNotificationMaskColor", int.class);
            if (mStackMask != null) {
                XposedCompat.hookMethod(mStackMask, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.args.length > 0 && param.args[0] instanceof Integer
                                && ((Integer) param.args[0]) == 0) {
                            Object bgObj = callNoArg(param.thisObject, "getBgView");
                            ViewParent parent = (bgObj instanceof View) ? ((View) bgObj).getParent() : null;
                            if (parent instanceof View && C_CUSTOM_ROW_VIEW.equals(parent.getClass().getName())) {
                                View rowView = (View) parent;
                                Object mgr = readField(rowView, "viewBlurManager");
                                if (!isCustomOnLockscreen(mgr, null) || !cfg.isFcEnabled()) {
                                    return;
                                }
                                float rawAod = resolveLiveAodProgress(param.thisObject);
                                if (hasCustomMixDrifted(cl, rowView, cfg, rawAod)) {
                                    LAST_CUSTOM_LOCK_STATE.remove(rowView);
                                    LAST_CUSTOM_MIX_VER.remove(rowView);
                                }
                                if (!isRowInImmersiveFade(rowView)) {
                                    Object metaDr = resolveCustomRowMetaDrawable(
                                            cl, rowView, bgObj, callNoArg(param.thisObject, "getViewBlurProxy"));
                                    int maskKey = computeCustomMaskKey(rowView, cfg, true, rawAod);
                                    Integer prevMask = LAST_CUSTOM_MASK_VER.get(rowView);
                                    if ((metaDr != null && LAST_CUSTOM_META_REF.get(rowView) != metaDr)
                                            || prevMask == null || prevMask != maskKey) {
                                        LAST_CUSTOM_LOCK_STATE.remove(rowView);
                                        LAST_CUSTOM_MASK_VER.remove(rowView);
                                    }
                                } else {
                                    LAST_CUSTOM_MASK_VER.remove(rowView);
                                }
                            }
                        }
                    }
                });
            }
            return true;
        } catch (Throwable t) {
            cfg.note("FluidCloud CustomRow bg draw hook err: " + t);
            return false;
        }
    }

    private static boolean isBrightNearWhiteStop(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int maxC = Math.max(r, Math.max(g, b));
        int minC = Math.min(r, Math.min(g, b));
        float luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255.0f;
        return luma >= 0.80f && (maxC - minC) <= 38;
    }

    private static boolean resolveAndCacheMetaDrawableIsMedia(ClassLoader cl, Object metaDrawable) {
        if (metaDrawable == null) {
            return false;
        }
        ArrayList<View> snapshot;
        synchronized (ACTIVE_CUSTOM_ROWS) {
            if (ACTIVE_CUSTOM_ROWS.isEmpty()) {
                return false;
            }
            snapshot = new ArrayList<>(ACTIVE_CUSTOM_ROWS.keySet());
        }
        ClassLoader effCl = cl != null ? cl : sPrimaryCl;
        for (int i = 0, n = snapshot.size(); i < n; i++) {
            View r = snapshot.get(i);
            if (r != null && resolveCustomRowMetaDrawable(effCl, r, null, null) == metaDrawable) {
                boolean isMedia = isMediaCustomRow(r);
                META_DRAWABLE_IS_MEDIA.put(metaDrawable, isMedia);
                return isMedia;
            }
        }
        return false;
    }

    private static Object handleMetaBallSetGradientColors(ClassLoader cl, ConfigStore.HookConfig cfg,
                                                          XposedInterface.Chain chain,
                                                          Object metaDrawable) throws Throwable {
        Object arg0 = chain.getArg(0);
        Object arg1 = chain.getArg(1);
        Object outArg0 = arg0;
        Object outArg1 = arg1;
        boolean argsModified = false;
        boolean specChanged = false;
        View mappedRow = null;

        if (arg1 instanceof int[]) {
            int[] src = (int[]) arg1;
            if (src.length == 0 || (src.length == 1 && src[0] == 0)) {
                CustomRowMaskSpec prev = CUSTOM_MASK_BY_META.remove(metaDrawable);
                specChanged = (prev != null);
                mappedRow = META_DRAWABLE_TO_ROW.get(metaDrawable);
            } else {
                View rowView = META_DRAWABLE_TO_ROW.get(metaDrawable);
                mappedRow = rowView;
                Float tlImmFade = TL_IMMERSIVE_MASK_FADE.get();
                float immFade = tlImmFade != null
                        ? clamp01(tlImmFade)
                        : (rowView != null ? (1.0f - clamp01(readRowImmersiveProgress(rowView))) : 1.0f);
                if (immFade >= 0.95f || ORIG_META_GRAD_COLORS.get(metaDrawable) == null) {
                    ORIG_META_GRAD_COLORS.put(metaDrawable, src.clone());
                }
                if (shouldStripCustomRowMask(cfg, null)) {
                    CustomRowMaskSpec prev = CUSTOM_MASK_BY_META.remove(metaDrawable);
                    specChanged = (prev != null);
                    outArg1 = new int[]{0};
                    argsModified = true;
                } else if (!cfg.isFcEnabled()) {
                    CustomRowMaskSpec prev = CUSTOM_MASK_BY_META.remove(metaDrawable);
                    specChanged = (prev != null);
                } else {
                    Boolean isMediaObj = META_DRAWABLE_IS_MEDIA.get(metaDrawable);
                    boolean isMedia = isMediaObj != null
                            ? isMediaObj
                            : resolveAndCacheMetaDrawableIsMedia(cl, metaDrawable);
                    boolean onLock = isCustomOnLockscreen(null, null);
                    int mediaMaskRgb = resolveMediaMaskRgb(cfg, isMedia, onLock);
                    boolean hasMediaTint = mediaMaskRgb != 0;
                    boolean hasCustomMask = !hasMediaTint && isCustomMaskAllowed(cfg, isMedia, onLock);
                    int customMaskArgb = hasCustomMask ? cfg.getFcCustomMaskColor() : 0;
                    boolean hasAnyTint = hasMediaTint || hasCustomMask;
                    float maskScale = resolveMaskAlphaScale(cfg, isMedia, hasMediaTint)
                            * (hasCustomMask ? ConfigStore.colorAlphaScale(customMaskArgb) : 1.0f)
                            * immFade;
                    boolean stripForAod = false;
                    if (maskScale > 0.001f && onLock) {
                        float aodProg = easeAodProgress(resolveLiveAodProgress(null));
                        if (aodProg > 0.001f) {
                            maskScale *= (1.0f - aodProg);
                            if (maskScale <= 0.001f) {
                                stripForAod = true;
                            }
                        }
                    }

                    CustomRowMaskSpec newSpec = null;
                    if (!stripForAod && maskScale > 0.001f) {
                        boolean hasBrightWhiteStop = false;
                        if (!isMedia && !hasAnyTint) {
                            Boolean cachedBw = immFade < 0.95f
                                    ? META_DRAWABLE_BRIGHT_WHITE.get(metaDrawable) : null;
                            if (cachedBw != null) {
                                hasBrightWhiteStop = cachedBw;
                            } else {
                                for (int c : src) {
                                    if (((c >>> 24) & 0xFF) > 0 && isBrightNearWhiteStop(c)) {
                                        hasBrightWhiteStop = true;
                                        break;
                                    }
                                }
                                if (immFade >= 0.95f) {
                                    META_DRAWABLE_BRIGHT_WHITE.put(metaDrawable, hasBrightWhiteStop);
                                }
                            }
                        }
                        int peakA = 0;
                        int fallbackRgb = 0;
                        for (int c : src) {
                            int a = (c >>> 24) & 0xFF;
                            if (a > peakA) {
                                peakA = a;
                            }
                            if (fallbackRgb == 0 && a > 0 && (c & 0x00FFFFFF) != 0) {
                                fallbackRgb = c & 0x00FFFFFF;
                            }
                        }
                        if (immFade >= 0.95f && fallbackRgb != 0) {
                            META_DRAWABLE_FALLBACK_RGB.put(metaDrawable, fallbackRgb);
                        } else if (immFade < 0.95f) {
                            Integer cachedRgb = META_DRAWABLE_FALLBACK_RGB.get(metaDrawable);
                            if (cachedRgb != null) {
                                fallbackRgb = cachedRgb;
                            } else if (fallbackRgb != 0 && peakA > 0 && peakA < 255) {
                                // Un-premultiply dark fade if ColorOS scaled RGB alongside alpha.
                                float unScale = Math.min(4.0f, 1.0f / Math.max(0.15f, immFade));
                                int r = clampAlpha(Math.round(((fallbackRgb >> 16) & 0xFF) * unScale));
                                int g = clampAlpha(Math.round(((fallbackRgb >> 8) & 0xFF) * unScale));
                                int b = clampAlpha(Math.round((fallbackRgb & 0xFF) * unScale));
                                fallbackRgb = (r << 16) | (g << 8) | b;
                            }
                        }
                        if (peakA > 0) {
                            int basePeakA = hasBrightWhiteStop
                                    ? (immFade < 0.95f ? 96 : Math.min(peakA, 96))
                                    : 204;
                            float effScale = hasBrightWhiteStop ? Math.min(maskScale, 1.0f) : maskScale;
                            int aStart = clampAlpha(Math.round(basePeakA * effScale));
                            if (hasBrightWhiteStop) {
                                aStart = Math.min(0x38, clampAlpha(Math.round(aStart * 0.22f)));
                            }
                            if (aStart > 0) {
                                int rgb = hasMediaTint ? (mediaMaskRgb & 0x00FFFFFF)
                                        : (hasCustomMask ? (customMaskArgb & 0x00FFFFFF)
                                        : (fallbackRgb & 0x00FFFFFF));
                                newSpec = new CustomRowMaskSpec(
                                        rgb,
                                        aStart,
                                        cfg.getFcMaskGradStartInt(),
                                        cfg.getFcMaskGradEndInt(),
                                        cfg.getFcMaskGradMidInt());
                            }
                        }
                    }

                    CustomRowMaskSpec prev = CUSTOM_MASK_BY_META.get(metaDrawable);
                    if (newSpec != null) {
                        CUSTOM_MASK_BY_META.put(metaDrawable, newSpec);
                        specChanged = (prev == null || !prev.matches(newSpec));
                    } else {
                        CUSTOM_MASK_BY_META.remove(metaDrawable);
                        specChanged = (prev != null);
                    }
                    outArg1 = new int[]{0};
                    argsModified = true;
                }
            }
        }

        Object res = argsModified ? chain.proceed(new Object[]{outArg0, outArg1}) : chain.proceed();
        if (specChanged && metaDrawable != null && !Boolean.TRUE.equals(TL_IN_BG_DRAW_BEFORE.get())) {
            if (metaDrawable instanceof Drawable) {
                ((Drawable) metaDrawable).invalidateSelf();
            }
            View rowView = mappedRow != null ? mappedRow : META_DRAWABLE_TO_ROW.get(metaDrawable);
            if (rowView != null) {
                Object bgNormal = readField(rowView, "mBackgroundNormal");
                if (bgNormal instanceof View) {
                    ((View) bgNormal).invalidate();
                }
                rowView.invalidate();
            }
        }
        return res;
    }

    private static boolean hookMetaBall(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> cls = loadClass(C_META_BALL, cl);
            if (cls == null) {
                return false;
            }
            boolean ok = false;
            // MetaBallBlurDrawable.setMetaColor(int) delegates directly to setGradientColors(Orientation, int[]);
            // hooking only setGradientColors avoids scaling Lockscreen alpha twice.
            for (Method m : cls.getDeclaredMethods()) {
                if ("setGradientColors".equals(m.getName()) && m.getParameterTypes().length == 2) {
                    XposedCompat.hookDirect(m, new XposedInterface.Hooker() {
                        @Override
                        public Object intercept(XposedInterface.Chain chain) throws Throwable {
                            Object self = chain.getThisObject();
                            if (Boolean.TRUE.equals(IN_INTERNAL_META_CALL.get()) || !fromCardHandler(self)) {
                                return chain.proceed();
                            }
                            return handleMetaBallSetGradientColors(cl, cfg, chain, self);
                        }
                    });
                    ok = true;
                    break;
                }
            }
            return ok;
        } catch (Throwable t) {
            cfg.note("FluidCloud MetaBall hook err: " + t);
            return false;
        }
    }

    private static boolean hookLiveCapsuleRefresh(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> helperCls = loadClass(C_HELPER, cl);
            Method m = findMethod(helperCls, "drawCapsuleBg", View.class, Canvas.class);
            if (m == null) {
                return false;
            }
            XposedCompat.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Object helper = param.thisObject;
                    if (helper == null) {
                        return;
                    }
                    float aodProg = resolveLiveAodProgress(helper);
                    if (cfg.isFcEnabled()) {
                        Object sp = readField(helper, "scrimPaint");
                        if (sp instanceof Paint && ((Paint) sp).getAlpha() != 0) {
                            ((Paint) sp).setAlpha(0);
                        }
                    }
                    int helperKey = computeHelperKey(cfg, helper, aodProg);
                    Integer prev = LAST_HELPER_VER.get(helper);
                    if (prev != null && prev == helperKey) {
                        return;
                    }
                    ACTIVE_CAPSULE_HELPERS.put(helper, Boolean.TRUE);
                    LAST_HELPER_VER.put(helper, helperKey);
                    if (cfg.isFcEnabled()) {
                        zeroMetaColorSafely(callNoArg(helper, "getBlurDrawable"));
                    }
                    syncHelperBlurProxies(cl, helper, cfg, aodProg, true);
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("FluidCloud live refresh hook err: " + t);
            return false;
        }
    }

    private static boolean hookAodTransition(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        boolean ok = false;
        try {
            Class<?> aodCoreCls = loadClass(C_AOD_CORE, cl);
            if (aodCoreCls != null) {
                Method mGoingToSleep = findMethod(aodCoreCls, "onStartedGoingToSleep");
                if (mGoingToSleep != null) {
                    XposedCompat.hookMethod(mGoingToSleep, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.thisObject != null) {
                                sAodRepoRef = new WeakReference<>(param.thisObject);
                            }
                            Object lazyCtrl = readField(param.thisObject, "statusBarStateController");
                            Object ctrl = lazyCtrl != null ? callNoArg(lazyCtrl, "get") : null;
                            Object st = ctrl != null ? callNoArg(ctrl, "getState") : null;
                            int stVal = (st instanceof Integer) ? (Integer) st : -1;
                            sGoingToSleepFromLockscreen = (stVal == 1 || stVal == 2);
                            sGoingToSleepAtMs = SystemClock.uptimeMillis();
                        }
                    });
                    ok = true;
                }
                Method mSetProg = findMethod(aodCoreCls, "setPanoramicAodProgress", float.class);
                if (mSetProg != null) {
                    XposedCompat.hookMethod(mSetProg, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (param.thisObject != null) {
                                sAodRepoRef = new WeakReference<>(param.thisObject);
                            }
                            if (param.args.length > 0 && param.args[0] instanceof Float) {
                                if (sAodVisualAnimatorRunning || sVisualAodTransitionPending) {
                                    return;
                                }
                                if (sVisualAodControlled) {
                                    if (!isCustomOnLockscreen(null, null)) {
                                        sVisualAodControlled = false;
                                        sVisualAodProgress = 0.0f;
                                        sVisualAodTarget = 0.0f;
                                        sPanoramicAodProgress = 0.0f;
                                        sPendingSeedlingAodTrue = false;
                                        dispatchAodProgressToActiveViews(cl, cfg, 0.0f);
                                    }
                                    return;
                                }
                                float prog = clamp01((Float) param.args[0]);
                                sPanoramicAodProgress = prog;
                                dispatchAodProgressToActiveViews(cl, cfg, prog);
                            }
                        }
                    });
                    ok = true;
                }
                Method mSetAod = findMethod(aodCoreCls, "setPanoramicAod", boolean.class);
                if (mSetAod != null) {
                    XposedCompat.hookMethod(mSetAod, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.thisObject != null) {
                                sAodRepoRef = new WeakReference<>(param.thisObject);
                            }
                            if (param.args.length > 0 && param.args[0] instanceof Boolean) {
                                boolean entering = (Boolean) param.args[0];
                                boolean active = cfg.isFcEnabled() || cfg.isAodNotifTranslucent();
                                Object cur = readField(param.thisObject, "isPanoramicAod");
                                boolean prev = cur instanceof Boolean && (Boolean) cur;
                                if (active && entering && !prev) {
                                    sVisualAodTransitionPending = true;
                                } else if (!entering) {
                                    sVisualAodTransitionPending = false;
                                    sPendingSeedlingAodTrue = false;
                                    DynamicIslandHook.cancelPendingAodLightDisable();
                                }
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (param.thisObject != null) {
                                sAodRepoRef = new WeakReference<>(param.thisObject);
                            }
                            if (param.args.length > 0 && param.args[0] instanceof Boolean) {
                                boolean entering = (Boolean) param.args[0];
                                if (cfg.isFcEnabled() || cfg.isAodNotifTranslucent()) {
                                    boolean fastExit = !entering && !isCustomOnLockscreen(null, null);
                                    if (!entering && !fastExit) {
                                        DynamicIslandHook.syncPrimaryMediaColor(cfg);
                                    }
                                    startVisualAodTransition(cl, cfg, entering, fastExit);
                                } else {
                                    sVisualAodTransitionPending = false;
                                    sVisualAodControlled = false;
                                    sAodVisualAnimatorRunning = false;
                                    sPendingSeedlingAodTrue = false;
                                    float prog = resolveLiveAodProgress(param.thisObject);
                                    dispatchAodProgressToActiveViews(cl, cfg, prog);
                                }
                            } else {
                                float prog = resolveLiveAodProgress(param.thisObject);
                                dispatchAodProgressToActiveViews(cl, cfg, prog);
                            }
                        }
                    });
                    ok = true;
                }
            }
        } catch (Throwable t) {
            cfg.note("FluidCloud OplusPanoramicAodCoreStartable hook err: " + t);
        }

        try {
            Class<?> seedlingCls = loadClass(C_MEDIA_SEEDLING, cl);
            if (seedlingCls != null) {
                Method mSeedlingAod = findMethod(seedlingCls, "setPanoramicAod", boolean.class);
                if (mSeedlingAod != null) {
                    XposedCompat.hookMethod(mSeedlingAod, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.thisObject != null) {
                                sMediaSeedlingRef = new WeakReference<>(param.thisObject);
                            }
                            if (Boolean.TRUE.equals(IN_DEFERRED_SEEDLING_AOD.get())) {
                                return;
                            }
                            if (!cfg.isFcEnabled()) {
                                return;
                            }
                            if (param.args.length > 0 && param.args[0] instanceof Boolean) {
                                boolean entering = (Boolean) param.args[0];
                                if (!entering) {
                                    sPendingSeedlingAodTrue = false;
                                    return;
                                }
                                if (sVisualAodTransitionPending || sAodVisualAnimatorRunning
                                        || (sVisualAodControlled && sVisualAodProgress < 0.99f)) {
                                    sPendingSeedlingAodTrue = true;
                                    param.setResult(null);
                                }
                            }
                        }
                    });
                    ok = true;
                }
            }
        } catch (Throwable t) {
            cfg.note("FluidCloud MediaSeedling hook err: " + t);
        }

        try {
            Class<?> aodBgCls = loadClass(C_CUSTOM_ROW_AOD_BG, cl);
            Class<?> rowCls = loadClass(C_CUSTOM_ROW_VIEW, cl);
            if (aodBgCls != null && rowCls != null) {
                Method mMask = findMethod(aodBgCls, "applyMask", rowCls, float.class);
                if (mMask != null) {
                    XposedCompat.hookMethod(mMask, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!cfg.isFcEnabled() || param.args.length < 1 || !(param.args[0] instanceof View)) {
                                return;
                            }
                            View rowView = (View) param.args[0];
                            ACTIVE_CUSTOM_ROWS.put(rowView, Boolean.TRUE);
                            Object mgr = readField(rowView, "viewBlurManager");
                            if (!isCustomOnLockscreen(mgr, null)) {
                                return;
                            }
                            Object bgNormal = readField(rowView, "mBackgroundNormal");
                            Object ext = bgNormal != null ? callNoArg(bgNormal, "getExt") : null;
                            if (ext != null) {
                                callSetterAllowNull(ext, "setPanoramicAodDrawable",
                                        "android.graphics.drawable.Drawable", cl, null);
                            }
                            if (bgNormal instanceof View) {
                                ((View) bgNormal).invalidate();
                            }
                        }
                    });
                    ok = true;
                }
            }
        } catch (Throwable t) {
            cfg.note("FluidCloud OplusCustomRowAodBackgroundChanger hook err: " + t);
        }

        try {
            Class<?> changerCls = loadClass(C_AOD_COLOR_CHANGER, cl);
            Class<?> rowCls = loadClass(C_CUSTOM_ROW_VIEW, cl);
            if (changerCls != null && rowCls != null) {
                Method mChange = findMethod(changerCls, "changeAodCustomRowColor",
                        rowCls, float.class, boolean.class);
                if (mChange != null) {
                    XposedCompat.hookMethod(mChange, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (param.args.length < 2 || !(param.args[0] instanceof View)) {
                                return;
                            }
                            View rowView = (View) param.args[0];
                            ACTIVE_CUSTOM_ROWS.put(rowView, Boolean.TRUE);
                            boolean controlled = sAodVisualAnimatorRunning || sVisualAodControlled;
                            float rawAodProg = controlled
                                    ? clamp01(sVisualAodProgress)
                                    : ((param.args[1] instanceof Float) ? clamp01((Float) param.args[1]) : 0.0f);
                            float eased = easeAodProgress(rawAodProg);
                            if (!controlled) {
                                sPanoramicAodProgress = rawAodProg;
                            }
                            Object mgr = readField(rowView, "viewBlurManager");
                            if (mgr != null) {
                                sViewBlurManagerRef = new WeakReference<>(mgr);
                            }
                            boolean onLock = isCustomOnLockscreen(mgr, null);
                            if (!onLock) {
                                restoreShadeCustomRowIfNeeded(cl, mgr, rowView);
                                DynamicIslandHook.setCardBackgroundViewsAlphaInTree(rowView, 1.0f);
                                if (!isRowInImmersiveFade(rowView)) {
                                    syncCustomRowMask(cl, rowView, mgr, cfg, false);
                                }
                                return;
                            }
                            if (!cfg.isFcEnabled()) {
                                DynamicIslandHook.setCardBackgroundViewsAlphaInTree(rowView, 1.0f);
                                return;
                            }
                            syncCustomRowLockMix(cl, rowView, cfg, rawAodProg, true);
                            LAST_CUSTOM_MIX_VER.put(rowView, computeCustomMixKey(cfg, rowView, rawAodProg));
                            int maskKey = computeCustomMaskKey(rowView, cfg, true, rawAodProg);
                            Integer prevMask = LAST_CUSTOM_MASK_VER.get(rowView);
                            if (prevMask == null || prevMask != maskKey) {
                                syncCustomRowMask(cl, rowView, mgr, cfg, false);
                            }
                            DynamicIslandHook.setCardBackgroundViewsAlphaInTree(
                                    rowView, clamp01(1.0f - eased));
                            DynamicIslandHook.invalidateMultiLightViewsInTree(rowView);
                            Object bgObj = readField(rowView, "mBackgroundNormal");
                            if (bgObj != null) {
                                Object extObj = callNoArg(bgObj, "getExt");
                                if (extObj != null) {
                                    callSetterAllowNull(extObj, "setPanoramicAodDrawable",
                                            "android.graphics.drawable.Drawable", cl, null);
                                }
                                if (bgObj instanceof View) {
                                    ((View) bgObj).invalidate();
                                }
                            }
                            rowView.invalidate();
                        }
                    });
                    ok = true;
                }
            }
        } catch (Throwable t) {
            cfg.note("FluidCloud NotificationDetailColorChanger hook err: " + t);
        }
        return ok;
    }

    private static void zeroMetaColorSafely(Object bd) {
        if (bd == null) {
            return;
        }
        CustomRowMaskSpec prev = CUSTOM_MASK_BY_META.remove(bd);
        if (prev != null && !Boolean.TRUE.equals(TL_IN_BG_DRAW_BEFORE.get())
                && bd instanceof Drawable) {
            ((Drawable) bd).invalidateSelf();
        }
        IN_INTERNAL_META_CALL.set(Boolean.TRUE);
        try {
            callOneInt(bd, "setMetaColor", 0);
        } finally {
            IN_INTERNAL_META_CALL.remove();
        }
    }

    private static void syncProxyBlurConfig(ClassLoader cl, Object proxy, ConfigStore.HookConfig cfg,
                                            float aodProg, boolean fastTransition) {
        if (proxy == null) {
            return;
        }
        Object blurCfg = callNoArg(proxy, "getBlurConfig");
        if (blurCfg != null) {
            CAPSULE_CFG.put(blurCfg, Boolean.TRUE);
            syncBlurConfigLive(cl, blurCfg, proxy, cfg, true, aodProg, fastTransition);
        }
    }

    private static void syncHelperBlurProxies(ClassLoader cl, Object helper, ConfigStore.HookConfig cfg,
                                              float aodProg, boolean fastTransition) {
        if (helper == null) {
            return;
        }
        Object proxy = readField(helper, "viewBlurProxy");
        syncProxyBlurConfig(cl, proxy, cfg, aodProg, fastTransition);
        Object smoothProxy = readField(helper, "smoothSharedViewBlurProxy");
        if (smoothProxy != null && smoothProxy != proxy) {
            syncProxyBlurConfig(cl, smoothProxy, cfg, aodProg, fastTransition);
        }
    }

    private static boolean applyNormalRowEffectFast(ClassLoader cl, View nr, Object extObj, float rawAodProg) {
        try {
            Object proxy = extObj != null ? callNoArg(extObj, "getViewBlurProxy") : null;
            if (proxy == null) {
                return false;
            }
            Object blurCfg = callNoArg(proxy, "getBlurConfig");
            if (blurCfg == null) {
                return false;
            }
            Object pbd = resolvePlatformBlurDrawable(cl, proxy);
            Object bd = pbd != null ? callNoArg(pbd, "getBlurDrawable") : null;
            if (bd == null) {
                return false;
            }
            Object mRowEx = readField(nr, "mRowEx");
            Object blurMode = mRowEx != null ? callNoArg(mRowEx, "getNotificationBlurMode") : null;
            if (blurMode == null) {
                return false;
            }
            if (!sNotifBlurParamsMgrProbed) {
                Class<?> mgrCls = loadClass(C_PLATFORM_BLUR_MGR, cl);
                Class<?> strokeCls = loadClass(C_STROKE_PARAMS, cl);
                Class<?> ispCls = loadClass(C_INNER_SHADOW_PARAMS, cl);
                Class<?> modeCls = loadClass(C_BLUR_MODE, cl);
                if (mgrCls != null && strokeCls != null && ispCls != null && modeCls != null) {
                    sNotifBlurParamsMgrInstance = readStaticField(mgrCls, "INSTANCE");
                    sApplyAodProgressToBlurParamsM = findMethod(mgrCls,
                            "applyAodProgressToNotificationBlurParams",
                            strokeCls, ispCls, float.class, modeCls);
                }
                sNotifBlurParamsMgrProbed = true;
            }
            if (sNotifBlurParamsMgrInstance == null || sApplyAodProgressToBlurParamsM == null) {
                return false;
            }
            Object stroke = callNoArg(blurCfg, "getGradientStrokeLineParam");
            Object isp = callNoArg(blurCfg, "getInnerShadowParams");
            sApplyAodProgressToBlurParamsM.invoke(sNotifBlurParamsMgrInstance, stroke, isp, rawAodProg, blurMode);
            if (stroke != null) {
                callSetter(bd, "setGradientStrokeLineParams", C_STROKE_PARAMS, cl, stroke);
            }
            if (isp != null) {
                callSetter(bd, "setInnerShadowParams", C_INNER_SHADOW_PARAMS, cl, isp);
            }
            if (bd instanceof Drawable) {
                ((Drawable) bd).invalidateSelf();
            }
            if (pbd instanceof Drawable) {
                ((Drawable) pbd).invalidateSelf();
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void dispatchAodProgressToActiveViews(final ClassLoader cl, final ConfigStore.HookConfig cfg,
                                                       final float rawProgress) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    dispatchAodProgressToActiveViews(cl, cfg, rawProgress);
                }
            });
            return;
        }
        try {
            float rawAodProg = clamp01(rawProgress);
            float eased = easeAodProgress(rawAodProg);
            boolean endpoint = rawAodProg <= 0.001f || rawAodProg >= 0.999f;

            Set<View> rows = collectActiveCustomRows(cl);

            for (View rowView : rows) {
                if (rowView == null || !rowView.isAttachedToWindow()) {
                    continue;
                }
                Object mgr = readField(rowView, "viewBlurManager");
                if (mgr != null && (sViewBlurManagerRef == null || sViewBlurManagerRef.get() != mgr)) {
                    sViewBlurManagerRef = new WeakReference<>(mgr);
                }
                boolean onLock = isCustomOnLockscreen(mgr, null);
                if (!onLock) {
                    restoreShadeCustomRowIfNeeded(cl, mgr, rowView);
                    DynamicIslandHook.setCardBackgroundViewsAlphaInTree(rowView, 1.0f);
                    if (!isRowInImmersiveFade(rowView)) {
                        int maskKey = computeCustomMaskKey(rowView, cfg, false, 0.0f);
                        Integer prevMask = LAST_CUSTOM_MASK_VER.get(rowView);
                        if (endpoint || prevMask == null || prevMask != maskKey) {
                            syncCustomRowMask(cl, rowView, mgr, cfg, false);
                        }
                    }
                    continue;
                }
                if (!cfg.isFcEnabled()) {
                    DynamicIslandHook.setCardBackgroundViewsAlphaInTree(rowView, 1.0f);
                    continue;
                }
                int mixKey = computeCustomMixKey(cfg, rowView, rawAodProg);
                Integer prevMix = LAST_CUSTOM_MIX_VER.get(rowView);
                boolean mixChanged = endpoint || prevMix == null || prevMix != mixKey;
                if (mixChanged) {
                    LAST_CUSTOM_MIX_VER.put(rowView, mixKey);
                    syncCustomRowLockMix(cl, rowView, cfg, rawAodProg, true);
                    syncRowImmersiveEndColors(rowView, cfg, true, rawAodProg);
                }
                boolean maskChanged = false;
                if (!isRowInImmersiveFade(rowView)) {
                    int maskKey = computeCustomMaskKey(rowView, cfg, true, rawAodProg);
                    Integer prevMask = LAST_CUSTOM_MASK_VER.get(rowView);
                    maskChanged = endpoint || prevMask == null || prevMask != maskKey;
                    if (maskChanged) {
                        syncCustomRowMask(cl, rowView, mgr, cfg, false);
                    }
                } else {
                    LAST_CUSTOM_MASK_VER.remove(rowView);
                    if (readRowImmersiveProgress(rowView) >= 0.99f) {
                        zeroMetaColorSafely(resolveCustomRowMetaDrawable(cl, rowView, null, null));
                    }
                }
                DynamicIslandHook.setCardBackgroundViewsAlphaInTree(rowView, clamp01(1.0f - eased));
                if (mixChanged || maskChanged || rawAodProg > 0.001f) {
                    Object bgObj = readField(rowView, "mBackgroundNormal");
                    if (bgObj instanceof View) {
                        ((View) bgObj).invalidate();
                    }
                    rowView.invalidate();
                }
            }

            if (cfg.isFcEnabled()) {
                DynamicIslandHook.invalidateActiveLockLights();
            }

            ArrayList<Object> helpers = new ArrayList<>();
            synchronized (ACTIVE_CAPSULE_HELPERS) {
                helpers.addAll(ACTIVE_CAPSULE_HELPERS.keySet());
            }
            for (Object helper : helpers) {
                if (helper == null) {
                    continue;
                }
                Object proxy = readField(helper, "viewBlurProxy");
                Object smoothProxy = readField(helper, "smoothSharedViewBlurProxy");
                Object primaryProxy = proxy != null ? proxy : smoothProxy;
                Object vObj = primaryProxy != null ? callNoArg(primaryProxy, "getView") : null;
                if (!(vObj instanceof View) || !((View) vObj).isAttachedToWindow()) {
                    continue;
                }
                View capsuleItem = findEnclosingCapsuleItem((View) vObj);
                View capsuleRoot = capsuleItem != null ? capsuleItem : (View) vObj;
                if (!cfg.isFcEnabled()) {
                    if (LAST_HELPER_VER.remove(helper) != null) {
                        syncProxyBlurConfig(cl, proxy, cfg, 0.0f, true);
                        if (smoothProxy != null && smoothProxy != proxy) {
                            syncProxyBlurConfig(cl, smoothProxy, cfg, 0.0f, true);
                        }
                        DynamicIslandHook.setCardBackgroundViewsAlphaInTree(capsuleRoot, 1.0f);
                        DynamicIslandHook.invalidateMultiLightViewsInTree(capsuleRoot);
                        ((View) vObj).invalidate();
                    }
                    continue;
                }
                Object sp = readField(helper, "scrimPaint");
                if (sp instanceof Paint && ((Paint) sp).getAlpha() != 0) {
                    ((Paint) sp).setAlpha(0);
                }
                int helperKey = computeHelperKey(cfg, helper, rawAodProg);
                Integer prevH = LAST_HELPER_VER.get(helper);
                boolean helperChanged = endpoint || prevH == null || prevH != helperKey;
                DynamicIslandHook.setCardBackgroundViewsAlphaInTree(
                        capsuleRoot, clamp01(1.0f - eased));
                if (helperChanged) {
                    LAST_HELPER_VER.put(helper, helperKey);
                    zeroMetaColorSafely(callNoArg(helper, "getBlurDrawable"));
                    syncProxyBlurConfig(cl, proxy, cfg, rawAodProg, true);
                    if (smoothProxy != null && smoothProxy != proxy) {
                        syncProxyBlurConfig(cl, smoothProxy, cfg, rawAodProg, true);
                    }
                    ((View) vObj).invalidate();
                }
                if (helperChanged || rawAodProg > 0.001f || endpoint) {
                    DynamicIslandHook.invalidateMultiLightViewsInTree(capsuleRoot);
                }
            }

            ArrayList<View> ears = new ArrayList<>();
            synchronized (ACTIVE_CAPSULE_EARS) {
                ears.addAll(ACTIVE_CAPSULE_EARS.keySet());
            }
            for (View earView : ears) {
                if (earView == null || !earView.isAttachedToWindow()) {
                    continue;
                }
                Object proxy = readField(earView, "viewBlurProxy");
                Object smoothProxy = readField(earView, "smoothSharedViewBlurProxy");
                if (!cfg.isFcEnabled()) {
                    if (LAST_EAR_VER.remove(earView) != null) {
                        syncProxyBlurConfig(cl, proxy, cfg, 0.0f, true);
                        if (smoothProxy != null && smoothProxy != proxy) {
                            syncProxyBlurConfig(cl, smoothProxy, cfg, 0.0f, true);
                        }
                        earView.invalidate();
                    }
                    continue;
                }
                Object sp = readField(earView, "scrimPaint");
                if (sp instanceof Paint && ((Paint) sp).getAlpha() != 0) {
                    ((Paint) sp).setAlpha(0);
                }
                int earKey = computeHelperKey(cfg, earView, rawAodProg);
                Integer prevE = LAST_EAR_VER.get(earView);
                if (!endpoint && prevE != null && prevE == earKey) {
                    continue;
                }
                LAST_EAR_VER.put(earView, earKey);
                syncProxyBlurConfig(cl, proxy, cfg, rawAodProg, true);
                if (smoothProxy != null && smoothProxy != proxy) {
                    syncProxyBlurConfig(cl, smoothProxy, cfg, rawAodProg, true);
                }
                earView.invalidate();
            }

            ArrayList<View> normalRows = new ArrayList<>();
            synchronized (ACTIVE_NORMAL_ROWS) {
                normalRows.addAll(ACTIVE_NORMAL_ROWS.keySet());
            }
            if (!normalRows.isEmpty()) {
                Object wrapper = sNormalBgAodWrapperRef != null ? sNormalBgAodWrapperRef.get() : null;
                Object changer = null;
                Class<?> nrCls = loadClass(C_EXPANDABLE_NOTIF_ROW, cl);
                Method mUpdateColor = null;
                Method mChangeEffectAlpha = null;
                boolean onLock = isCustomOnLockscreen(null, null);
                boolean activeTranslucent = onLock && cfg.isAodNotifTranslucent() && rawAodProg > 0.001f;
                int endpointTag = rawAodProg >= 0.999f ? 256 : 0;
                int nrKey = (cfg.getFcVersion() << 10)
                        | (activeTranslucent ? (512 + endpointTag + Math.round(rawAodProg * 255f)) : 0);
                for (View nr : normalRows) {
                    if (nr == null || !nr.isAttachedToWindow()) {
                        continue;
                    }
                    if (nrCls != null && !nrCls.isInstance(nr)) {
                        continue;
                    }
                    Object aodColor = NORMAL_BG_AOD_COLORS.get(nr);
                    Integer prevNrKey = LAST_NORMAL_ROW_KEY.get(nr);
                    if (!activeTranslucent) {
                        if (aodColor == null && prevNrKey != null && prevNrKey == nrKey) {
                            continue;
                        }
                        LAST_NORMAL_ROW_KEY.put(nr, nrKey);
                        Object bgNormal = readField(nr, "mBackgroundNormal");
                        Object extObj = bgNormal != null ? callNoArg(bgNormal, "getExt") : null;
                        if (cfg.isAodNotifTranslucent() && extObj != null) {
                            callSetterAllowNull(extObj, "setPanoramicAodDrawable",
                                    "android.graphics.drawable.Drawable", cl, null);
                        }
                        if (wrapper != null && aodColor != null) {
                            callSetter(wrapper, "setView", C_EXPANDABLE_NOTIF_ROW, cl, nr);
                            callSetter(wrapper, "restoreColor", C_NORMAL_BG_AOD_COLOR, cl, aodColor);
                            callSetterAllowNull(wrapper, "setAodColor", C_NORMAL_BG_AOD_COLOR, cl, null);
                            NORMAL_BG_AOD_COLORS.remove(nr);
                            if (changer == null) {
                                changer = callNoArg(wrapper, "getAodStrokeInnerShadowChanger");
                            }
                            if (changer != null && nrCls != null) {
                                if (mChangeEffectAlpha == null) {
                                    mChangeEffectAlpha = findMethod(changer.getClass(), "changeAodEffectAlpha",
                                            nrCls, float.class, boolean.class);
                                }
                                if (mChangeEffectAlpha != null) {
                                    try {
                                        mChangeEffectAlpha.invoke(changer, nr, 0.0f, false);
                                    } catch (Throwable ignored) {
                                    }
                                }
                            }
                        }
                        if (bgNormal instanceof View) {
                            ((View) bgNormal).invalidate();
                        }
                        nr.invalidate();
                        continue;
                    }
                    if (!endpoint && prevNrKey != null && prevNrKey == nrKey) {
                        continue;
                    }
                    boolean enteringNr = (prevNrKey == null || (prevNrKey & 512) == 0);
                    LAST_NORMAL_ROW_KEY.put(nr, nrKey);
                    Object bgNormal = readField(nr, "mBackgroundNormal");
                    Object extObj = bgNormal != null ? callNoArg(bgNormal, "getExt") : null;
                    if (extObj != null && (enteringNr || endpoint)) {
                        callSetterAllowNull(extObj, "setPanoramicAodDrawable",
                                "android.graphics.drawable.Drawable", cl, null);
                    }
                    if (wrapper != null) {
                        callSetter(wrapper, "setView", C_EXPANDABLE_NOTIF_ROW, cl, nr);
                        if (aodColor == null) {
                            aodColor = callOneInt(wrapper, "createAodColor", 0);
                            if (aodColor == null) {
                                aodColor = callNoArg(wrapper, "getAodColor");
                            }
                            if (aodColor != null) {
                                NORMAL_BG_AOD_COLORS.put(nr, aodColor);
                            }
                        }
                        if (aodColor != null) {
                            callSetter(wrapper, "setAodColor", C_NORMAL_BG_AOD_COLOR, cl, aodColor);
                            if (mUpdateColor == null) {
                                mUpdateColor = findMethod(wrapper.getClass(), "updateColor",
                                        aodColor.getClass(), float.class);
                            }
                            if (mUpdateColor != null) {
                                try {
                                    mUpdateColor.invoke(wrapper, aodColor, rawAodProg);
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                        boolean fastDone = !endpoint && applyNormalRowEffectFast(cl, nr, extObj, rawAodProg);
                        if (!fastDone) {
                            if (changer == null) {
                                changer = callNoArg(wrapper, "getAodStrokeInnerShadowChanger");
                            }
                            if (changer != null && nrCls != null) {
                                if (mChangeEffectAlpha == null) {
                                    mChangeEffectAlpha = findMethod(changer.getClass(), "changeAodEffectAlpha",
                                            nrCls, float.class, boolean.class);
                                }
                                if (mChangeEffectAlpha != null) {
                                    try {
                                        mChangeEffectAlpha.invoke(changer, nr, rawAodProg, false);
                                    } catch (Throwable ignored) {
                                    }
                                }
                            }
                        }
                    }
                    if (bgNormal instanceof View) {
                        ((View) bgNormal).invalidate();
                    }
                    nr.invalidate();
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object resolvePlatformBlurDrawable(ClassLoader cl, Object proxy) {
        if (proxy == null) {
            return null;
        }
        try {
            Class<?> pbdCls = loadClass(C_PLATFORM_BLUR_DRAWABLE, cl);
            if (pbdCls == null) {
                return null;
            }
            Object cachedPbd = callNoArg(proxy, "getCachePlatformStaticBlurDrawable");
            if (cachedPbd != null && pbdCls.isInstance(cachedPbd)) {
                return cachedPbd;
            }
            Drawable d = resolveBlurDrawable(proxy, null);
            if (d != null && pbdCls.isInstance(d)) {
                return d;
            }
            Object vObj = callNoArg(proxy, "getView");
            if (vObj instanceof View) {
                Drawable bg = ((View) vObj).getBackground();
                if (bg != null) {
                    if (pbdCls.isInstance(bg)) {
                        return bg;
                    }
                    Drawable bd = resolveBlurDrawable(proxy, bg);
                    if (bd != null && pbdCls.isInstance(bd)) {
                        return bd;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Boolean callChangeBlurColor(ClassLoader cl, Object pbd, Object targetMix, boolean force) {
        if (pbd == null || targetMix == null) {
            return null;
        }
        try {
            Class<?> mixCls = loadClass(C_MIX_CFG, cl);
            Method m = mixCls != null
                    ? findMethod(pbd.getClass(), "changeBlurColor", mixCls, boolean.class) : null;
            if (m != null) {
                Object res = m.invoke(pbd, targetMix, force);
                return (res instanceof Boolean) ? (Boolean) res : Boolean.TRUE;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean applyBlurColorFast(ClassLoader cl, Object pbd, Object bd,
                                              Object targetMix, float blurAmount) {
        if (targetMix == null) {
            return true;
        }
        if (Boolean.TRUE.equals(callChangeBlurColor(cl, pbd, targetMix, true))) {
            callSetter(pbd, "setCurrentBlurMixConfig", C_MIX_CFG, cl, targetMix);
            if (bd instanceof Drawable) {
                ((Drawable) bd).invalidateSelf();
            }
            if (pbd instanceof Drawable) {
                ((Drawable) pbd).invalidateSelf();
            }
            return true;
        }
        try {
            Class<?> singleCls = loadClass(C_MIX_SINGLE, cl);
            if (singleCls == null || !singleCls.isInstance(targetMix)) {
                return false;
            }
            if (!sShaderBlendHelperProbed) {
                Class<?> helperCls = loadClass(C_SHADER_BLEND_HELPER, cl);
                if (helperCls != null) {
                    sShaderBlendHelperInstance = readStaticField(helperCls, "INSTANCE");
                    sUpdateMixSingleShaderParams = findMethod(helperCls, "updateMixSingleShaderParams",
                            singleCls, float.class);
                }
                sShaderBlendHelperProbed = true;
            }
            Method mUpdate = sUpdateMixSingleShaderParams;
            Method mGetParams = sGetMixSingleShaderParams;
            if (mGetParams == null) {
                mGetParams = findMethod(singleCls, "getMixSingleShaderParams");
                sGetMixSingleShaderParams = mGetParams;
            }
            Method mSetParams = sSetShaderBlendParams;
            if (mSetParams == null || !mSetParams.getDeclaringClass().isInstance(bd)) {
                mSetParams = findMethod(bd.getClass(), "setShaderBlendParams", ArrayList.class);
                sSetShaderBlendParams = mSetParams;
            }
            if (sShaderBlendHelperInstance == null || mUpdate == null || mGetParams == null || mSetParams == null) {
                return false;
            }
            mUpdate.invoke(sShaderBlendHelperInstance, targetMix, blurAmount);
            callOneBool(bd, "setEnableBlurShader", true);
            Object shaderParams = mGetParams.invoke(targetMix);
            if (shaderParams instanceof ArrayList) {
                mSetParams.invoke(bd, shaderParams);
                callSetter(pbd, "setCurrentBlurMixConfig", C_MIX_CFG, cl, targetMix);
                if (bd instanceof Drawable) {
                    ((Drawable) bd).invalidateSelf();
                }
                if (pbd instanceof Drawable) {
                    ((Drawable) pbd).invalidateSelf();
                }
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void applyBlurMixAndLight(ClassLoader cl, Object proxy, Object blurCfg,
                                             Object targetMix, boolean isCapsule,
                                             float aodForLight, float rawAodProgress,
                                             boolean fastTransition) {
        if (proxy == null || blurCfg == null) {
            return;
        }
        if (targetMix != null) {
            Object curMix = callNoArg(blurCfg, "getPlatformMixConfig");
            if (targetMix != curMix) {
                callSetter(blurCfg, "setPlatformMixConfig", C_MIX_CFG, cl, targetMix);
            }
        }
        applyAodToBlurConfigLight(blurCfg, isCapsule, aodForLight);

        float blurAmount = readFloatField(proxy, "blurAmount", 1.0f);
        int scaleBits = Float.floatToRawIntBits(blurAmount);
        Integer prevScale = LAST_BLUR_SCALE_BITS.get(proxy);
        boolean blurScaleChanged = (prevScale == null || prevScale != scaleBits);

        Object pbd = resolvePlatformBlurDrawable(cl, proxy);
        if (pbd != null) {
            callOneBool(pbd, "setAodState", false);
            callOneBool(pbd, "setAodDirty", false);
            Object curPbdMix = callNoArg(pbd, "getCurrentBlurMixConfig");
            if (fastTransition && !blurScaleChanged && curPbdMix != null) {
                Object bd = callNoArg(pbd, "getBlurDrawable");
                if (bd != null) {
                    Object stroke = callNoArg(blurCfg, "getGradientStrokeLineParam");
                    if (stroke != null) {
                        callSetter(bd, "setGradientStrokeLineParams", C_STROKE_PARAMS, cl, stroke);
                    }
                    Object isp = callNoArg(blurCfg, "getInnerShadowParams");
                    if (isp != null) {
                        callSetter(bd, "setInnerShadowParams", C_INNER_SHADOW_PARAMS, cl, isp);
                    }
                    if (applyBlurColorFast(cl, pbd, bd, targetMix, blurAmount)) {
                        return;
                    }
                }
            }
            if (targetMix != null) {
                callSetter(pbd, "setCurrentBlurMixConfig", C_MIX_CFG, cl, targetMix);
            }
        }
        LAST_BLUR_SCALE_BITS.put(proxy, scaleBits);
        callNoArg(proxy, "applyBlurConfig");
    }

    private static void syncBlurConfigLive(ClassLoader cl, Object blurCfg, Object proxy,
                                           ConfigStore.HookConfig cfg, boolean isCapsule,
                                           float aodProgress) {
        syncBlurConfigLive(cl, blurCfg, proxy, cfg, isCapsule, aodProgress, false);
    }

    private static void syncBlurConfigLive(ClassLoader cl, Object blurCfg, Object proxy,
                                           ConfigStore.HookConfig cfg, boolean isCapsule,
                                           float aodProgress, boolean fastTransition) {
        try {
            float rawAod = clamp01(aodProgress);
            float easedAod = easeAodProgress(rawAod);
            Object curMix = callNoArg(blurCfg, "getPlatformMixConfig");
            Object origMix = ORIG_MIX_BY_CFG.get(blurCfg);
            if (origMix == null) {
                Object stockFallback = isCapsule ? sStockCapsuleMix : sStockCustomMix;
                if (curMix != null && !Boolean.TRUE.equals(BUILT_MIX_SET.get(curMix)) && rawAod <= 0.001f) {
                    origMix = curMix;
                } else {
                    origMix = stockFallback != null ? stockFallback
                            : (curMix != null && !Boolean.TRUE.equals(BUILT_MIX_SET.get(curMix)) ? curMix : null);
                }
                if (origMix != null) {
                    ORIG_MIX_BY_CFG.put(blurCfg, origMix);
                }
            }
            Object targetMix = origMix;
            if (origMix != null) {
                Object vObj = proxy != null ? callNoArg(proxy, "getView") : null;
                View proxyView = vObj instanceof View ? (View) vObj : null;
                boolean allowCustom = isCapsule
                        ? !shouldSuppressCapsuleCustomColor(proxyView, cfg)
                        : !(DynamicIslandHook.hasActiveLockMediaFluidCloud()
                                && (cfg.isFcNonMediaMaskEnabled() || sLastLockCustomRowIsMedia));
                targetMix = (cfg.isFcEnabled() && (cfg.isFcLockTintOrMaskModified() || rawAod > 0.001f))
                        ? buildMix(cl, cfg, origMix, rawAod, allowCustom)
                        : origMix;
            }
            boolean radiusChanged = applyScaledBlurRadius(blurCfg, cfg, cfg.isFcEnabled());
            float aodForLight = cfg.isFcEnabled() ? easedAod : 0.0f;
            applyBlurMixAndLight(cl, proxy, blurCfg, targetMix, isCapsule, aodForLight, rawAod,
                    fastTransition && !radiusChanged);
        } catch (Throwable ignored) {
        }
    }

    private static void applyAodToBlurConfigLight(Object blurCfg, boolean isCapsule, float aodProgress) {
        if (blurCfg == null) {
            return;
        }
        try {
            Object stroke = callNoArg(blurCfg, "getGradientStrokeLineParam");
            Object isp = callNoArg(blurCfg, "getInnerShadowParams");
            if (stroke == null && isp == null) {
                return;
            }
            float aod = clamp01(aodProgress);
            float[] orig = ORIG_BLUR_CFG_LIGHT.get(blurCfg);
            if (orig == null) {
                float near = stroke != null ? readFloatField(stroke, "strokeLineAlphaNear", 0.40f) : 0.40f;
                float far = stroke != null ? readFloatField(stroke, "strokeLineAlphaFar", 0.80f) : 0.80f;
                float oMax = isp != null ? readFloatField(isp, "offsetShadowMaxAlpha", isCapsule ? 0.10f : 0.12f) : (isCapsule ? 0.10f : 0.12f);
                float oMin = isp != null ? readFloatField(isp, "offsetShadowMinAlpha", 0.0f) : 0.0f;
                float noOff = isp != null ? readFloatField(isp, "noOffsetShadowAlpha", isCapsule ? 0.10f : 0.24f) : (isCapsule ? 0.10f : 0.24f);
                boolean poisoned = aod >= 0.02f || sVisualAodTransitionPending || near < 0.10f || far < 0.20f;
                if (!poisoned) {
                    orig = new float[]{near, far, oMax, oMin, noOff};
                } else {
                    orig = new float[]{
                            0.40f,
                            0.80f,
                            isCapsule ? 0.10f : 0.12f,
                            0.0f,
                            isCapsule ? 0.10f : 0.24f
                    };
                }
                ORIG_BLUR_CFG_LIGHT.put(blurCfg, orig);
            }
            float inv = 1.0f - aod;
            float strokeFactor = isCapsule
                    ? (0.14f + 0.86f * (float) Math.pow(inv, 1.5))
                    : (1.0f - 0.65f * aod);
            float innerFactor = isCapsule
                    ? (float) Math.pow(inv, 1.65)
                    : inv;
            if (stroke != null) {
                callOneFloat(stroke, "setStrokeLineAlphaNear", orig[0] * strokeFactor);
                callOneFloat(stroke, "setStrokeLineAlphaFar", orig[1] * strokeFactor);
            }
            if (isp != null) {
                callOneFloat(isp, "setOffsetShadowMaxAlpha", orig[2] * innerFactor);
                callOneFloat(isp, "setOffsetShadowMinAlpha", orig[3] * innerFactor);
                callOneFloat(isp, "setNoOffsetShadowAlpha", orig[4] * innerFactor);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void syncCustomRowLockMix(ClassLoader cl, View rowView, ConfigStore.HookConfig cfg,
                                             float aodProgress) {
        syncCustomRowLockMix(cl, rowView, cfg, aodProgress, false);
    }

    private static void syncCustomRowLockMix(ClassLoader cl, View rowView, ConfigStore.HookConfig cfg,
                                             float aodProgress, boolean fastTransition) {
        if (rowView == null) {
            return;
        }
        try {
            Object bgNormal = readField(rowView, "mBackgroundNormal");
            Object ext = callNoArg(bgNormal, "getExt");
            Object proxy = callNoArg(ext, "getViewBlurProxy");
            if (proxy == null) {
                return;
            }
            Object blurCfg = callNoArg(proxy, "getBlurConfig");
            if (blurCfg == null) {
                return;
            }
            float immProg = readRowImmersiveProgress(rowView);
            float rawAod = clamp01(aodProgress);
            float easedAod = easeAodProgress(rawAod);
            Object curMix = callNoArg(blurCfg, "getPlatformMixConfig");
            Object origMix = ORIG_MIX_BY_CFG.get(blurCfg);
            if (origMix == null) {
                if (curMix != null && !Boolean.TRUE.equals(BUILT_MIX_SET.get(curMix))
                        && rawAod <= 0.001f
                        && !Boolean.TRUE.equals(LOCK_CUSTOM_MIX_APPLIED.get(blurCfg))) {
                    origMix = curMix;
                } else {
                    origMix = sStockCustomMix != null ? sStockCustomMix
                            : (curMix != null && !Boolean.TRUE.equals(BUILT_MIX_SET.get(curMix)) ? curMix : null);
                }
                if (origMix != null) {
                    ORIG_MIX_BY_CFG.put(blurCfg, origMix);
                }
            }
            boolean radiusChanged = applyScaledBlurRadius(blurCfg, cfg, cfg.isFcEnabled());
            boolean atImmersive = immProg >= 0.99f;
            boolean inMidImm = immProg > 0.01f && immProg < 0.99f;
            boolean wantActive = cfg.isFcEnabled() && (atImmersive
                    ? (cfg.isFcLockTintOrMaskModified() || cfg.isFcImmersiveOpacityModified() || rawAod > 0.001f)
                    : (cfg.isFcLockTintOrMaskModified() || rawAod > 0.001f));
            syncRowImmersiveEndColors(rowView, cfg, true, rawAod);
            if (inMidImm) {
                if (radiusChanged) {
                    callNoArg(proxy, "applyBlurConfig");
                }
                return;
            }
            if (wantActive) {
                if (origMix != null) {
                    float targetOp = atImmersive ? cfg.getFcImmersiveOpacity() : cfg.getFcOpacity();
                    boolean customMixOn = atImmersive
                            ? (cfg.isFcLockTintOrMaskModified() || cfg.isFcImmersiveOpacityModified())
                            : cfg.isFcLockTintOrMaskModified();
                    Object targetMix = buildMixForOpacity(cl, cfg, origMix, rawAod, targetOp, customMixOn,
                            !shouldSuppressLockCustomColor(rowView, cfg));
                    LOCK_CUSTOM_MIX_APPLIED.put(blurCfg, Boolean.TRUE);
                    applyBlurMixAndLight(cl, proxy, blurCfg, targetMix, false, easedAod, rawAod,
                            fastTransition && !radiusChanged);
                } else if (radiusChanged) {
                    callNoArg(proxy, "applyBlurConfig");
                }
                if (ext != null) {
                    callSetterAllowNull(ext, "setPanoramicAodDrawable",
                            "android.graphics.drawable.Drawable", cl, null);
                }
            } else {
                if (LOCK_CUSTOM_MIX_APPLIED.remove(blurCfg) != null
                        || (curMix != null && Boolean.TRUE.equals(BUILT_MIX_SET.get(curMix)))) {
                    if (origMix != null) {
                        applyBlurMixAndLight(cl, proxy, blurCfg, origMix, false, 0.0f, 0.0f,
                                fastTransition && !radiusChanged);
                    } else if (radiusChanged) {
                        callNoArg(proxy, "applyBlurConfig");
                    }
                } else if (radiusChanged) {
                    callNoArg(proxy, "applyBlurConfig");
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void restoreShadeCustomRowIfNeeded(ClassLoader cl, Object mgr, View rowView) {
        if (rowView == null) {
            return;
        }
        try {
            syncRowImmersiveEndColors(rowView, null, false, 0.0f);
            LAST_CUSTOM_MASK_VER.remove(rowView);
            Object bgNormal = readField(rowView, "mBackgroundNormal");
            Object ext = callNoArg(bgNormal, "getExt");
            Object proxy = callNoArg(ext, "getViewBlurProxy");
            if (proxy == null) {
                return;
            }
            Object blurCfg = callNoArg(proxy, "getBlurConfig");
            if (blurCfg == null) {
                return;
            }
            Object curMix = callNoArg(blurCfg, "getPlatformMixConfig");
            boolean hadApplied = LOCK_CUSTOM_MIX_APPLIED.remove(blurCfg) != null;
            boolean isBuilt = curMix != null && Boolean.TRUE.equals(BUILT_MIX_SET.get(curMix));
            boolean radiusRestored = applyScaledBlurRadius(blurCfg, null, false);
            if (!hadApplied && !isBuilt && !radiusRestored) {
                return;
            }
            applyAodToBlurConfigLight(blurCfg, false, 0.0f);
            Object pbd = resolvePlatformBlurDrawable(cl, proxy);
            if (pbd != null) {
                callOneBool(pbd, "setAodState", false);
                callOneBool(pbd, "setAodDirty", false);
            }
            LAST_CUSTOM_MIX_VER.remove(rowView);
            if (mgr != null) {
                Class<?> proxyCls = loadClass("com.oplusos.systemui.common.blurability.ViewBlurProxy", cl);
                if (proxyCls != null) {
                    Method mUpdate = findMethod(mgr.getClass(), "updatePlatformBlurParams", proxyCls, boolean.class);
                    if (mUpdate != null) {
                        mUpdate.invoke(mgr, proxy, Boolean.TRUE);
                        return;
                    }
                }
            }
            Object origMix = ORIG_MIX_BY_CFG.get(blurCfg);
            if (origMix != null) {
                callSetter(blurCfg, "setPlatformMixConfig", C_MIX_CFG, cl, origMix);
                if (pbd != null) {
                    callSetter(pbd, "setCurrentBlurMixConfig", C_MIX_CFG, cl, origMix);
                }
                callNoArg(proxy, "applyBlurConfig");
            }
        } catch (Throwable ignored) {
        }
    }

    private static void syncCustomRowMask(ClassLoader cl, View rowView, Object mgr,
                                            ConfigStore.HookConfig cfg, boolean fromDraw) {
        if (rowView == null) {
            return;
        }
        try {
            Object bgNormal = readField(rowView, "mBackgroundNormal");
            Object ext = callNoArg(bgNormal, "getExt");
            Object proxy = callNoArg(ext, "getViewBlurProxy");
            if (proxy == null) {
                LAST_CUSTOM_MASK_VER.remove(rowView);
                return;
            }
            Object metaDrawable = resolveCustomRowMetaDrawable(cl, rowView, bgNormal, proxy);
            if (metaDrawable == null) {
                LAST_CUSTOM_MASK_VER.remove(rowView);
                return;
            }
            LAST_CUSTOM_META_REF.put(rowView, metaDrawable);
            if (mgr != null) {
                sViewBlurManagerRef = new WeakReference<>(mgr);
            }
            boolean onLock = isCustomOnLockscreen(mgr, null);
            boolean isMedia = isMediaCustomRow(rowView);
            if (onLock) {
                sLastLockCustomRowIsMedia = isMedia;
            }
            META_DRAWABLE_IS_MEDIA.put(metaDrawable, isMedia);
            IN_CUSTOM_MASK_SYNC.set(Boolean.TRUE);
            try {
                if (shouldStripCustomRowMask(cfg, mgr)) {
                    CUSTOM_MASK_BY_META.remove(metaDrawable);
                    callOneInt(metaDrawable, "setMetaColor", 0);
                } else {
                    Object entry = readField(rowView, "entry");
                    Object appliedBgColor = null;
                    IN_MASK_RESTORE.set(Boolean.TRUE);
                    try {
                        Class<?> handlerCls = loadClass(C_CUSTOM_ROW, cl);
                        Object inst = readStaticField(handlerCls, "INSTANCE");
                        if (handlerCls != null && inst != null && entry != null) {
                            Method mUpd = sUpdateDrawableMetaColorM;
                            if (mUpd == null) {
                                for (Method m : handlerCls.getDeclaredMethods()) {
                                    if ("updateDrawableMetaColor".equals(m.getName())
                                            && m.getParameterTypes().length == 3) {
                                        m.setAccessible(true);
                                        mUpd = m;
                                        sUpdateDrawableMetaColorM = m;
                                        break;
                                    }
                                }
                            }
                            if (mUpd != null) {
                                Class<?>[] pts = mUpd.getParameterTypes();
                                if (pts[0].isAssignableFrom(Context.class)
                                        && pts[1].isInstance(entry)
                                        && pts[2].isInstance(metaDrawable)) {
                                    appliedBgColor = mUpd.invoke(inst, rowView.getContext(), entry, metaDrawable);
                                }
                            }
                        }
                        if (appliedBgColor == null && handlerCls != null && inst != null) {
                            Object savedBgColor = readField(rowView, "gradientColors");
                            if (savedBgColor == null) {
                                savedBgColor = readField(rowView, "contentColors");
                            }
                            if (savedBgColor != null) {
                                Method mFade = sApplyGradientColorsWithFadeM;
                                if (mFade == null) {
                                    for (Method m : handlerCls.getDeclaredMethods()) {
                                        if ("applyGradientColorsWithFade".equals(m.getName())
                                                && m.getParameterTypes().length == 3) {
                                            m.setAccessible(true);
                                            mFade = m;
                                            sApplyGradientColorsWithFadeM = m;
                                            break;
                                        }
                                    }
                                }
                                if (mFade != null) {
                                    Class<?>[] pts = mFade.getParameterTypes();
                                    if (pts[0].isInstance(metaDrawable)
                                            && pts[1].isInstance(savedBgColor)
                                            && pts[2] == float.class) {
                                        mFade.invoke(inst, metaDrawable, savedBgColor, 0.0f);
                                        appliedBgColor = savedBgColor;
                                    }
                                }
                            }
                        }
                    } finally {
                        IN_MASK_RESTORE.remove();
                    }
                    if (appliedBgColor == null) {
                        int[] origColors = ORIG_META_GRAD_COLORS.get(metaDrawable);
                        if (origColors != null && origColors.length > 0) {
                            Method mSetGrad = findMethod(metaDrawable.getClass(),
                                    "setGradientColors", GradientDrawable.Orientation.class, int[].class);
                            if (mSetGrad != null) {
                                mSetGrad.invoke(metaDrawable, GradientDrawable.Orientation.TOP_BOTTOM, origColors);
                                appliedBgColor = origColors;
                            }
                        }
                    }
                    if (appliedBgColor == null && cfg.isFcEnabled()) {
                        Object mbp = readField(metaDrawable, "metaBallParams");
                        Object gc = mbp != null ? callNoArg(mbp, "getGradientColor") : null;
                        int numC = gc != null ? readIntField(gc, "numColor", 0) : 0;
                        int c1 = gc != null ? readIntField(gc, "color1", 0) : 0;
                        int c2 = gc != null ? readIntField(gc, "color2", 0) : 0;
                        int c3 = gc != null ? readIntField(gc, "color3", 0) : 0;
                        int c4 = gc != null ? readIntField(gc, "color4", 0) : 0;
                        if (((c1 >>> 24) & 0xFF) > 0 || ((c2 >>> 24) & 0xFF) > 0
                                || ((c3 >>> 24) & 0xFF) > 0 || ((c4 >>> 24) & 0xFF) > 0) {
                            int count = Math.max(1, Math.min(4, numC));
                            int[] existing = new int[count];
                            if (count >= 1) existing[0] = c1;
                            if (count >= 2) existing[1] = c2;
                            if (count >= 3) existing[2] = c3;
                            if (count >= 4) existing[3] = c4;
                            Method mSetGrad = findMethod(metaDrawable.getClass(),
                                    "setGradientColors", GradientDrawable.Orientation.class, int[].class);
                            if (mSetGrad != null) {
                                mSetGrad.invoke(metaDrawable, GradientDrawable.Orientation.TOP_BOTTOM, existing);
                            }
                        }
                    }
                    if (!cfg.isFcEnabled()) {
                        CUSTOM_MASK_BY_META.remove(metaDrawable);
                    }
                }
            } finally {
                IN_CUSTOM_MASK_SYNC.remove();
            }
            float rawAodProg = onLock ? resolveLiveAodProgress(ext) : 0.0f;
            LAST_CUSTOM_MASK_VER.put(rowView, computeCustomMaskKey(rowView, cfg, onLock, rawAodProg));
            if (!fromDraw) {
                if (bgNormal instanceof View) {
                    ((View) bgNormal).invalidate();
                }
                rowView.invalidate();
            }
        } catch (Throwable ignored) {
        }
    }

    private static Drawable resolveBlurDrawable(Object proxy, Drawable fallback) {
        if (proxy != null) {
            try {
                Method m = findMethod(proxy.getClass(), "getBlurDrawable", Drawable.class);
                if (m != null) {
                    Object res = m.invoke(proxy, fallback);
                    if (res instanceof Drawable) {
                        return (Drawable) res;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return fallback;
    }

    private static void drawCustomRowBottomGlow(Object extImp, View rowView, View bgView,
                                                Object proxy, Drawable blurDr, Canvas canvas,
                                                int glowPct, int chromaPct, ConfigStore.HookConfig cfg) {
        if (DynamicIslandHook.hasActiveMultiLightInTree(rowView)) {
            return;
        }
        Rect bounds = blurDr.getBounds();
        int w = bounds.width();
        int h = bounds.height();
        if (w < 24 || h < 24) {
            return;
        }
        float alphaMul = blurDr.getAlpha() / 255.0f;
        float aodProg = easeAodProgress(resolveLiveAodProgress(extImp));
        if (aodProg > 0.01f) {
            alphaMul *= clamp01(1.0f - aodProg);
        }
        if (alphaMul <= 0.01f) {
            return;
        }

        float dp = resolveDisplayDensity();
        float s = dp / 3.0f;

        float[] cc = extractCustomRowCornerConfig(blurDr, proxy, w, h, dp);
        float cornerMode = cc[0];
        float cornerRadius = cc[1];
        float cornerWeight = cc[2];

        float tx = readRowTranslationX(rowView);
        if (isCollapsedByHorizontalSwipe(bgView, Math.abs(tx), cornerRadius)) {
            return;
        }

        try {
            RuntimeShader shader = TL_CUSTOM_GLOW_SHADER.get();
            if (shader == null) {
                shader = new RuntimeShader(CUSTOM_ROW_BOTTOM_GLOW_AGSL);
                TL_CUSTOM_GLOW_SHADER.set(shader);
            }
            Paint paint = TL_CUSTOM_GLOW_PAINT.get();
            if (paint == null) {
                paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                TL_CUSTOM_GLOW_PAINT.set(paint);
            }

            float baseW = 328.0f * dp;
            float baseH = 60.0f * dp;
            float scaleX = 1.0f + 0.3f * (baseW / Math.max(1.0f, (float) w));
            float scaleY = 1.0f + 0.8f * (baseH / Math.max(1.0f, (float) h));
            float range = 70.0f * s;
            float offsetY = 90.0f * s;
            float brightBoost = clamp01(glowPct / (float) ConfigStore.MAX_FC_BOTTOM_GLOW);
            float chromaBoost = clamp01(chromaPct / (float) ConfigStore.MAX_FC_BOTTOM_GLOW_CHROMA);

            float[] rgb = new float[]{1.0f, 1.0f, 1.0f};
            boolean colored = false;
            boolean isMediaRow = isMediaCustomRow(rowView);
            int mediaTintArgb = DynamicIslandHook.resolveScopedMediaTintArgb(isMediaRow, false, true);
            if ((isMediaRow || cfg.isFcNonMediaMaskEnabled())
                    && mediaTintArgb != 0) {
                DynamicIslandHook.boostHighlightChromaArgb(
                        mediaTintArgb, chromaBoost, brightBoost, rgb);
                colored = true;
            } else if (!shouldSuppressLockCustomColor(rowView, cfg) && cfg.isFcColorEnabled()
                    && ((cfg.getFcColor() >>> 24) & 0xFF) > 0) {
                float ca = ConfigStore.colorAlphaScale(cfg.getFcColor());
                DynamicIslandHook.boostHighlightChromaArgb(
                        cfg.getFcColor(), chromaBoost * ca, brightBoost, rgb);
                colored = true;
            }

            float shadowA = (colored ? 0.44f : 0.36f) * (float) Math.pow(brightBoost, colored ? 0.80 : 0.85) * alphaMul;
            float rimA = (colored ? 0.78f : 0.68f) * (float) Math.pow(brightBoost, colored ? 0.80 : 0.85) * alphaMul;
            float rimFade = (6.0f + 5.0f * brightBoost) * s;

            shader.setFloatUniform("uSize", (float) w, (float) h);
            shader.setFloatUniform("uRadius", cornerRadius);
            shader.setFloatUniform("uCornerMode", cornerMode);
            shader.setFloatUniform("uWeight", cornerWeight);
            shader.setFloatUniform("uRange", range);
            shader.setFloatUniform("uOffsetY", offsetY);
            shader.setFloatUniform("uScale", scaleX, scaleY);
            shader.setFloatUniform("uShadowAlpha", shadowA);
            shader.setFloatUniform("uRimAlpha", rimA);
            shader.setFloatUniform("uRimFade", rimFade);
            shader.setFloatUniform("uGlowColor", rgb[0], rgb[1], rgb[2]);
            paint.setShader(shader);

            int sc = canvas.save();
            try {
                canvas.translate(bounds.left + (int) tx, bounds.top);
                canvas.drawRect(0f, 0f, (float) w, (float) h, paint);
            } finally {
                canvas.restoreToCount(sc);
            }
        } catch (Throwable t) {
            if (!sLoggedCustomAgslErr) {
                sLoggedCustomAgslErr = true;
                cfg.note("FluidCloud: CustomRow AGSL bottom glow err: " + t);
            }
        }
    }

    private static float[] extractCustomRowCornerConfig(Drawable blurDr, Object proxy, int w, int h, float dp) {
        try {
            Object bd = callNoArg(blurDr, "getBlurDrawable");
            if (bd != null) {
                Object ds = readField(bd, "drawableShader");
                Object cp = readField(ds, "mCornerParams");
                float[] parsed = parseCornerParams(cp);
                if (parsed != null) {
                    return parsed;
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            Object blurCfg = proxy != null ? callNoArg(proxy, "getBlurConfig") : null;
            if (blurCfg != null) {
                Object strokeCp = callNoArg(blurCfg, "getGradientStrokeCornerParam");
                float[] parsed = parseCornerParams(strokeCp);
                if (parsed != null) {
                    return parsed;
                }
                float r = readFloatCall(blurCfg, "getLeftBottomCornerRadius", 0f);
                if (r <= 0f) {
                    r = readFloatCall(blurCfg, "getLeftTopCornerRadius", 0f);
                }
                if (r <= 0f) {
                    r = readFloatCall(blurCfg, "getCornerRadius", 0f);
                }
                if (r > 0f) {
                    float wt = 1.1f;
                    Object wtObj = callNoArg(blurCfg, "getRadiusWeight");
                    if (wtObj instanceof Float && ((Float) wtObj) > 0f) {
                        wt = (Float) wtObj;
                    }
                    float mode = (wt >= 2.0f) ? 2.0f : 1.0f;
                    return new float[]{mode, r, wt};
                }
            }
        } catch (Throwable ignored) {
        }

        return new float[]{1.0f, Math.min(Math.min(w, h) * 0.25f, 16.0f * dp), 1.1f};
    }

    private static float[] parseCornerParams(Object cp) {
        if (cp == null) {
            return null;
        }
        float r = readFloatField(cp, "radius", 0f);
        if (r <= 0f) {
            return null;
        }
        float wt = readFloatField(cp, "weight", 1.0f);
        if (wt <= 0f) {
            wt = 1.0f;
        }
        Object typeObj = callNoArg(cp, "getType");
        if (typeObj == null) {
            typeObj = readField(cp, "type");
        }
        String typeName = typeObj == null ? "" : String.valueOf(typeObj);
        float mode;
        if ("G2".equals(typeName)) {
            mode = 2.0f;
        } else if ("CONIC".equals(typeName)) {
            mode = 1.0f;
        } else {
            mode = 0.0f;
        }
        return new float[]{mode, r, wt};
    }

    private static float readFloatCall(Object obj, String methodName, float defVal) {
        Object val = callNoArg(obj, methodName);
        return val instanceof Float ? (Float) val : defVal;
    }

    private static void applyStockSizeMapping(ClassLoader cl, Context ctx, Object stroke, Object isp,
                                              int width, int height, Object blurMode) {
        if (ctx == null || width <= 0 || height <= 0 || blurMode == null) {
            return;
        }
        if (stroke == null && isp == null) {
            return;
        }
        try {
            Class<?> mgrCls = loadClass(C_PLATFORM_BLUR_MGR, cl);
            Class<?> blurModeCls = loadClass(C_BLUR_MODE, cl);
            Class<?> strokeCls = loadClass(C_STROKE_PARAMS, cl);
            Class<?> innerShadowCls = loadClass(C_INNER_SHADOW_PARAMS, cl);
            if (mgrCls == null || blurModeCls == null) {
                return;
            }
            Object mgrInst = readStaticField(mgrCls, "INSTANCE");
            if (mgrInst == null) {
                return;
            }
            if (stroke != null && strokeCls != null) {
                Method mStrokeSize = findMethod(mgrCls, "updateGradientStrokeLineParamsForNotificationSize",
                        Context.class, strokeCls, float.class, float.class, blurModeCls);
                if (mStrokeSize != null) {
                    mStrokeSize.invoke(mgrInst, ctx, stroke, (float) width, (float) height, blurMode);
                }
            }
            if (isp != null && innerShadowCls != null) {
                Method mShadowSize = findMethod(mgrCls, "updateInnerShadowParamsForNotificationSize",
                        Context.class, innerShadowCls, float.class, float.class, blurModeCls);
                if (mShadowSize != null) {
                    mShadowSize.invoke(mgrInst, ctx, isp, (float) width, (float) height, blurMode);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static float[] ensureOrigStrokeSaved(Object params, float aod) {
        float[] orig = ORIG_STROKE.get(params);
        if (orig != null) {
            return orig;
        }
        if (aod < 0.99f) {
            orig = new float[]{0.40f, 6.0f};
        } else {
            orig = new float[]{
                    readFloatField(params, "strokeLineAlphaNear", 0.40f),
                    readFloatField(params, "strokeLineVerticalNearFade", 6.0f)
            };
        }
        ORIG_STROKE.put(params, orig);
        return orig;
    }

    private static boolean restoreStrokeParams(Object params) {
        if (params == null) {
            return false;
        }
        float[] orig = ORIG_STROKE.get(params);
        if (orig == null) {
            return false;
        }
        try {
            callOneFloat(params, "setStrokeLineAlphaNear", orig[0]);
            callOneFloat(params, "setStrokeLineVerticalNearFade", orig[1]);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ColorOS 17 AGSL flips Y (`coords.y = H - position.y`), so Near = BOTTOM edge & Far = TOP edge
    private static void applyBottomHighlightToStroke(Object params, ConfigStore.HookConfig cfg,
                                                     float aodFactor) {
        if (params == null) {
            return;
        }
        try {
            float aod = clamp01(aodFactor);
            int boostPct = cfg.isFcEnabled() ? cfg.getFcBottomGlowInt() : 0;
            float[] orig = ensureOrigStrokeSaved(params, aod);
            float alphaNear;
            float nearFade = orig[1];
            if (boostPct <= 0) {
                alphaNear = orig[0] * aod;
            } else {
                float boost = clamp01(boostPct / (float) ConfigStore.MAX_FC_BOTTOM_GLOW);
                alphaNear = clamp01(orig[0] + (1.0f - orig[0]) * boost) * aod;
                nearFade = orig[1] * (1.0f + 0.85f * boost);
            }
            callOneFloat(params, "setStrokeLineAlphaNear", alphaNear);
            callOneFloat(params, "setStrokeLineVerticalNearFade", nearFade);
        } catch (Throwable ignored) {
        }
    }

    private static float[] ensureOrigShadowSaved(Object params, float aod) {
        float[] orig = ORIG_SHADOW.get(params);
        if (orig != null) {
            return orig;
        }
        if (aod < 0.99f) {
            orig = new float[]{0.12f, 0.24f, 1.3f, 1.8f};
        } else {
            orig = new float[]{
                    readFloatField(params, "offsetShadowMaxAlpha", 0.12f),
                    readFloatField(params, "noOffsetShadowAlpha", 0.24f),
                    readFloatField(params, "offsetShadowScaleX", 1.3f),
                    readFloatField(params, "offsetShadowScaleY", 1.8f)
            };
        }
        ORIG_SHADOW.put(params, orig);
        return orig;
    }

    private static boolean restoreShadowParams(Object params) {
        if (params == null) {
            return false;
        }
        float[] orig = ORIG_SHADOW.get(params);
        if (orig == null) {
            return false;
        }
        try {
            callOneFloat(params, "setOffsetShadowMaxAlpha", orig[0]);
            callOneFloat(params, "setNoOffsetShadowAlpha", orig[1]);
            callOneFloat(params, "setOffsetShadowScaleX", orig[2]);
            callOneFloat(params, "setOffsetShadowScaleY", orig[3]);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void applyBottomHighlightToInnerShadow(Object params, ConfigStore.HookConfig cfg,
                                                          float aodFactor) {
        if (params == null) {
            return;
        }
        try {
            float aod = clamp01(aodFactor);
            int boostPct = cfg.isFcEnabled() ? cfg.getFcBottomGlowInt() : 0;
            float[] orig = ensureOrigShadowSaved(params, aod);
            float maxA;
            float noOffA = orig[1] * aod;
            if (boostPct <= 0) {
                maxA = orig[0] * aod;
            } else {
                float boost = clamp01(boostPct / (float) ConfigStore.MAX_FC_BOTTOM_GLOW);
                maxA = clamp01(orig[0] + (0.62f - orig[0]) * boost) * aod;
            }

            callOneFloat(params, "setOffsetShadowMaxAlpha", maxA);
            callOneFloat(params, "setNoOffsetShadowAlpha", noOffA);
        } catch (Throwable ignored) {
        }
    }

    static boolean isCurrentlyOnLockscreen() {
        return isCustomOnLockscreen(null, null);
    }

    private static boolean isCustomOnLockscreen(Object mgr, Object explicitMode) {
        if (explicitMode != null && isShadeBlurMode(explicitMode)) {
            return false;
        }
        if (mgr != null) {
            WeakReference<Object> ref = sViewBlurManagerRef;
            if (ref == null || ref.get() != mgr) {
                sViewBlurManagerRef = new WeakReference<>(mgr);
            }
        } else if (sViewBlurManagerRef != null) {
            mgr = sViewBlurManagerRef.get();
        }
        if (mgr == null) {
            return false;
        }
        try {
            Field sf = sStatusBarStateCtrlField;
            if (sf == null || !sf.getDeclaringClass().isInstance(mgr)) {
                sf = findFieldCached(mgr.getClass(), "statusBarStateController");
                sStatusBarStateCtrlField = sf;
            }
            Object sbs = sf != null ? sf.get(mgr) : null;
            if (sbs != null) {
                int stateVal = -1;
                if (!sStateFieldProbed) {
                    sStateIntField = findFieldCached(sbs.getClass(), "mState");
                    sStateFieldProbed = true;
                }
                Field stf = sStateIntField;
                if (stf != null && stf.getDeclaringClass().isInstance(sbs)) {
                    stateVal = stf.getInt(sbs);
                } else {
                    Method gs = sGetStateM;
                    if (gs == null || !gs.getDeclaringClass().isInstance(sbs)) {
                        gs = findSingleTypedMethodCached(NO_ARG_METHOD_CACHE, sbs.getClass(), "getState", null);
                        sGetStateM = gs;
                    }
                    Object st = gs != null ? gs.invoke(sbs) : null;
                    if (st instanceof Integer) {
                        stateVal = (Integer) st;
                    }
                }
                // ColorOS transitions StatusBarState KEYGUARD (1) -> SHADE_LOCKED (2) when expanding lockscreen cards.
                if (stateVal != -1 && stateVal != 1 && stateVal != 2) {
                    return false;
                }
            }
            Method gm = sGlobalBlurModeM;
            if (gm == null || !gm.getDeclaringClass().isInstance(mgr)) {
                gm = findSingleTypedMethodCached(NO_ARG_METHOD_CACHE, mgr.getClass(), "globalBlurMode", null);
                sGlobalBlurModeM = gm;
            }
            if (gm != null) {
                Object globalMode = gm.invoke(mgr);
                if (globalMode != null && isShadeBlurMode(globalMode)) {
                    return false;
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    private static boolean shouldStripCustomRowMask(ConfigStore.HookConfig cfg, Object viewBlurManager) {
        if (!cfg.isFcEnabled() || Boolean.TRUE.equals(IN_MASK_RESTORE.get())) {
            return false;
        }
        if (isCustomOnLockscreen(viewBlurManager, null)) {
            return cfg.isFcMatchCard();
        }
        return cfg.isFcRemoveShadeMask();
    }

    private static boolean isMediaCustomRow(View rowView) {
        if (rowView == null) {
            return false;
        }
        return isMediaEntry(readField(rowView, "entry"));
    }

    private static boolean isMediaEntry(Object entry) {
        if (entry == null) {
            return false;
        }
        Object sidObj = callNoArg(entry, "getServiceId");
        String sid = sidObj != null ? String.valueOf(sidObj) : "";
        return "268452006".equals(sid) || "default_com.android.systemui_268452006".equals(sid);
    }

    private static boolean isShadeBlurMode(Object blurMode) {
        if (blurMode == null) {
            return false;
        }
        Boolean cached = SHADE_MODE_CACHE.get(blurMode);
        if (cached != null) {
            return cached;
        }
        String name = String.valueOf(blurMode);
        boolean res;
        if (name.startsWith("SHADE") || name.startsWith("NO_BLUR") || name.startsWith("HEADS_UP")) {
            res = true;
        } else {
            Object shade = callNoArg(blurMode, "getShade");
            res = Boolean.TRUE.equals(shade);
        }
        SHADE_MODE_CACHE.putIfAbsent(blurMode, res);
        return res;
    }

    private static boolean fromCardHandler(Object metaDrawable) {
        if (CUSTOM_ROW_HANDLER_ACTIVE_COUNT.get() > 0) {
            Integer depth = TL_CUSTOM_ROW_HANDLER_DEPTH.get();
            if (depth != null && depth > 0) {
                return true;
            }
        }
        if (Boolean.TRUE.equals(IN_CUSTOM_MASK_SYNC.get())
                || Boolean.TRUE.equals(IN_MASK_RESTORE.get())) {
            return true;
        }
        return sAnyMetaRowMapped
                && metaDrawable != null
                && META_DRAWABLE_TO_ROW.containsKey(metaDrawable);
    }

    private static final class RowExtInfo {
        final WeakReference<View> bgRef;
        final WeakReference<View> parentRef;
        final byte kind; // 0=other, 1=normal, 2=custom
        Object blurMgr;
        boolean registered;

        RowExtInfo(View bgView, View parentView, byte kind) {
            this.bgRef = new WeakReference<>(bgView);
            this.parentRef = new WeakReference<>(parentView);
            this.kind = kind;
        }
    }

    private static int[] computeExpectedMixFgColors(ConfigStore.HookConfig cfg, float aodProgress,
                                                      boolean allowCustomColor) {
        return computeExpectedMixFgColorsForOpacity(
                cfg, cfg.getFcOpacity(), aodProgress, cfg.isFcLockTintOrMaskModified(), allowCustomColor);
    }

    private static int[] computeExpectedMixFgColorsForOpacity(ConfigStore.HookConfig cfg,
                                                              float opacity,
                                                              float aodProgress,
                                                              boolean useCustomMix,
                                                              boolean allowCustomColor) {
        float aod = easeAodProgress(aodProgress);
        int lockRgb = CLEAN_GLASS_RGB;
        int lockTopA = 174;
        int lockLiftRgb = 0xB3B3B3;
        if (useCustomMix) {
            boolean useCustom = allowCustomColor && cfg.isFcColorEnabled();
            if (useCustom) {
                int customColor = cfg.getFcColor();
                float colorAlpha = ConfigStore.colorAlphaScale(customColor);
                lockRgb = customColor & 0x00FFFFFF;
                lockTopA = clampAlpha(Math.round(189.0f * opacity * colorAlpha));
                int cr = (customColor >> 16) & 0xFF;
                int cg = (customColor >> 8) & 0xFF;
                int cb = customColor & 0xFF;
                float tintR = 140.25f + 0.45f * cr;
                float tintG = 140.25f + 0.45f * cg;
                float tintB = 140.25f + 0.45f * cb;
                int lr = clampAlpha(Math.round(179.0f + (tintR - 179.0f) * colorAlpha));
                int lg = clampAlpha(Math.round(179.0f + (tintG - 179.0f) * colorAlpha));
                int lb = clampAlpha(Math.round(179.0f + (tintB - 179.0f) * colorAlpha));
                lockLiftRgb = (lr << 16) | (lg << 8) | lb;
            } else {
                lockTopA = clampAlpha(Math.round(174.0f * opacity));
            }
        }
        int rgb = lockRgb;
        int topA = lockTopA;
        if (aod > 0.001f) {
            int lr = (lockRgb >> 16) & 0xFF;
            int lg = (lockRgb >> 8) & 0xFF;
            int lb = lockRgb & 0xFF;
            int ar = (AOD_WIDGET_RGB >> 16) & 0xFF;
            int ag = (AOD_WIDGET_RGB >> 8) & 0xFF;
            int ab = AOD_WIDGET_RGB & 0xFF;
            int r = clampAlpha(Math.round(lr + (ar - lr) * aod));
            int g = clampAlpha(Math.round(lg + (ag - lg) * aod));
            int b = clampAlpha(Math.round(lb + (ab - lb) * aod));
            rgb = (r << 16) | (g << 8) | b;
            topA = clampAlpha(Math.round(lockTopA + (AOD_WIDGET_ALPHA - lockTopA) * aod));
        }
        int newTop = (topA << 24) | rgb;
        int liftA = clampAlpha(Math.round(77.0f * (1.0f - aod)));
        int liftColor = liftA > 0 ? ((liftA << 24) | lockLiftRgb) : 0;
        return new int[]{newTop, liftColor};
    }

    private static Object buildMix(ClassLoader cl, ConfigStore.HookConfig cfg, Object origMix,
                                   float aodProgress, boolean allowCustomColor) throws Exception {
        return buildMixForOpacity(
                cl, cfg, origMix, aodProgress, cfg.getFcOpacity(),
                cfg.isFcLockTintOrMaskModified(), allowCustomColor);
    }

    private static Object buildMixForOpacity(ClassLoader cl, ConfigStore.HookConfig cfg, Object origMix,
                                             float aodProgress, float opacity,
                                             boolean useCustomMix, boolean allowCustomColor) throws Exception {
        int[] fgColors = computeExpectedMixFgColorsForOpacity(
                cfg, opacity, aodProgress, useCustomMix, allowCustomColor);
        int newTop = fgColors[0];
        int liftColor = fgColors[1];
        int liftMode = 2;

        boolean isMulti = origMix != null && origMix.getClass().getName().contains("Multi");
        float mirrorScale = 1.0f;
        boolean alphaWithBlur = false;
        int placeholder = 0;

        if (isMulti) {
            Object ms = readField(origMix, "mirrorScale");
            if (ms instanceof Float) {
                mirrorScale = (Float) ms;
            }
            Object awb = readField(origMix, "alphaWithBlurAmount");
            if (awb instanceof Boolean) {
                alphaWithBlur = (Boolean) awb;
            }
            if (!cfg.isFcMatchCard()) {
                Object ph = readField(origMix, "placeHolderColor");
                if (ph instanceof Integer) {
                    placeholder = (Integer) ph;
                }
            }
        }

        int key = newTop;
        key = key * 31 + liftColor;
        key = key * 31 + liftMode;
        key = key * 31 + (isMulti ? 1 : 0);
        if (isMulti) {
            key = key * 31 + Float.floatToIntBits(mirrorScale);
            key = key * 31 + (alphaWithBlur ? 1 : 0);
            key = key * 31 + placeholder;
        }

        synchronized (MIX_LOCK) {
            Object cached = MIX_CACHE.get(key);
            if (cached != null) {
                BUILT_MIX_SET.put(cached, Boolean.TRUE);
                return cached;
            }
            ensureMixCtors(cl);
            // Mode 1 = SRC_OVER (card tint/opacity), Mode 2 = OVERLAY (frosted glass lift matching Top Expanded Card, faded out in AOD).
            // bgShader MUST stay EMPTY (0, 0, 0, 0) just like NotifiAndQsPlatformBlurExKt on Lockscreen to prevent 2x darkening.
            Object fgShader = sMixShaderCtor.newInstance(1, newTop, liftMode, liftColor);
            Object bgShader = sMixShaderCtor.newInstance(0, 0, 0, 0);
            Object built;
            if (isMulti && sMixMultiCtor != null) {
                built = sMixMultiCtor.newInstance(fgShader, bgShader, mirrorScale, alphaWithBlur, placeholder);
            } else {
                built = sMixSingleCtor.newInstance(fgShader);
            }
            if (MIX_CACHE.size() >= 320) {
                MIX_CACHE.clear();
            }
            MIX_CACHE.put(key, built);
            BUILT_MIX_SET.put(built, Boolean.TRUE);
            return built;
        }
    }

    private static boolean hookNormalNotifAod(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        boolean ok = false;
        final Class<?> rowCls = loadClass(C_EXPANDABLE_NOTIF_ROW, cl);
        if (rowCls == null) {
            return false;
        }

        try {
            Class<?> bgChangerCls = loadClass(C_NOTIF_AOD_BG_CHANGER, cl);
            if (bgChangerCls != null) {
                Method mBgChange = findMethod(bgChangerCls, "changeAodViewColor", rowCls, float.class);
                if (mBgChange != null) {
                    XposedCompat.hookMethod(mBgChange, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args.length < 1 || !(param.args[0] instanceof View)) {
                                return;
                            }
                            View row = (View) param.args[0];
                            ACTIVE_NORMAL_ROWS.put(row, Boolean.TRUE);
                            if (!cfg.isAodNotifTranslucent()) {
                                return;
                            }
                            Object bgNormal = readField(row, "mBackgroundNormal");
                            Object ext = bgNormal != null ? callNoArg(bgNormal, "getExt") : null;
                            if (ext != null) {
                                callSetterAllowNull(ext, "setPanoramicAodDrawable",
                                        "android.graphics.drawable.Drawable", cl, null);
                            }
                            if (bgNormal instanceof View) {
                                ((View) bgNormal).invalidate();
                            }
                            param.setResult(null);
                        }
                    });
                    ok = true;
                }
            }
        } catch (Throwable t) {
            cfg.note("FluidCloud NotificationAodBackgroundChanger hook err: " + t);
        }

        try {
            Class<?> detailCls = loadClass(C_AOD_COLOR_CHANGER, cl);
            if (detailCls != null) {
                Method mDetailView = findMethod(detailCls, "changeAodViewColor",
                        rowCls, float.class, boolean.class);
                if (mDetailView != null) {
                    XposedCompat.hookMethod(mDetailView, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args.length < 2 || !(param.args[0] instanceof View)) {
                                return;
                            }
                            View row = (View) param.args[0];
                            ACTIVE_NORMAL_ROWS.put(row, Boolean.TRUE);
                            Object wrapper = readField(param.thisObject, "normalBgAodWrapper");
                            if (wrapper != null) {
                                sNormalBgAodWrapperRef = new WeakReference<>(wrapper);
                            }
                            if ((cfg.isAodNotifTranslucent() || cfg.isFcEnabled())
                                    && (sAodVisualAnimatorRunning || sVisualAodControlled)
                                    && isCustomOnLockscreen(null, null)) {
                                param.args[1] = clamp01(sVisualAodProgress);
                            }
                        }
                    });
                    ok = true;
                }
            }
        } catch (Throwable t) {
            cfg.note("FluidCloud DetailColorChanger changeAodViewColor hook err: " + t);
        }

        try {
            Class<?> wrapperCls = loadClass(C_NORMAL_BG_AOD_WRAPPER, cl);
            Class<?> normalColorCls = loadClass(C_NORMAL_BG_AOD_COLOR, cl);
            if (wrapperCls != null && normalColorCls != null) {
                for (Method m : wrapperCls.getDeclaredMethods()) {
                    if ("createAodColor".equals(m.getName()) && !m.isBridge()) {
                        Class<?>[] pts = m.getParameterTypes();
                        if (pts.length == 1 && pts[0] == int.class) {
                            m.setAccessible(true);
                            XposedCompat.hookMethod(m, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) {
                                    if (param.thisObject != null) {
                                        sNormalBgAodWrapperRef = new WeakReference<>(param.thisObject);
                                    }
                                    Object v = callNoArg(param.thisObject, "getView");
                                    Object res = param.getResult();
                                    if (v instanceof View) {
                                        ACTIVE_NORMAL_ROWS.put((View) v, Boolean.TRUE);
                                        if (cfg.isAodNotifTranslucent() && res != null) {
                                            NORMAL_BG_AOD_COLORS.put(v, res);
                                        }
                                    }
                                }
                            });
                            ok = true;
                            break;
                        }
                    }
                }

                Method mUpdateColor = findMethod(wrapperCls, "updateColor", normalColorCls, float.class);
                if (mUpdateColor != null) {
                    XposedCompat.hookMethod(mUpdateColor, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.thisObject != null) {
                                sNormalBgAodWrapperRef = new WeakReference<>(param.thisObject);
                            }
                            Object v = callNoArg(param.thisObject, "getView");
                            if (v instanceof View) {
                                ACTIVE_NORMAL_ROWS.put((View) v, Boolean.TRUE);
                                if (cfg.isAodNotifTranslucent() && param.args.length > 0 && param.args[0] != null) {
                                    NORMAL_BG_AOD_COLORS.put(v, param.args[0]);
                                }
                            }
                            if (cfg.isAodNotifTranslucent()
                                    && (sAodVisualAnimatorRunning || sVisualAodControlled)
                                    && isCustomOnLockscreen(null, null)
                                    && param.args.length > 1) {
                                param.args[1] = clamp01(sVisualAodProgress);
                            }
                        }
                    });
                    ok = true;
                }

                Method mGetColor = findMethod(normalColorCls, "getColor", float.class);
                if (mGetColor != null) {
                    XposedCompat.hookMethod(mGetColor, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!cfg.isAodNotifTranslucent() || !isCustomOnLockscreen(null, null)) {
                                return;
                            }
                            float raw = (sAodVisualAnimatorRunning || sVisualAodControlled)
                                    ? clamp01(sVisualAodProgress)
                                    : ((param.args.length > 0 && param.args[0] instanceof Float)
                                    ? clamp01((Float) param.args[0]) : 0.0f);
                            Object startData = readField(param.thisObject, "start");
                            if (startData == null) {
                                return;
                            }
                            Object replaced = buildTranslucentAodBgData(cl, startData, easeAodProgress(raw));
                            if (replaced != null) {
                                param.setResult(replaced);
                            }
                        }
                    });
                    ok = true;
                }
            }
        } catch (Throwable t) {
            cfg.note("FluidCloud NormalBgAodWrapper hook err: " + t);
        }

        return ok;
    }

    private static Object buildTranslucentAodBgData(ClassLoader cl, Object startData, float t) {
        if (startData == null) {
            return null;
        }
        if (t <= 0.001f) {
            return startData;
        }
        try {
            ensureMixCtors(cl);
            ensureMixColorCtor(cl);
            Class<?> mixColorCls = loadClass(C_MIX_COLOR, cl);
            Class<?> mixCfgCls = loadClass(C_MIX_CFG, cl);
            Class<?> multiCls = loadClass(C_MIX_MULTI, cl);
            if (mixColorCls == null || mixCfgCls == null || multiCls == null) {
                return null;
            }
            Object typeObj = callNoArg(startData, "getType");
            int type = typeObj instanceof Integer ? (Integer) typeObj : 0;
            Object origin = callNoArg(startData, "getOrigin");
            Object startMix = callNoArg(startData, "getMixColor");
            Object startMulti = callNoArg(startData, "getBlurMixMultiWithShader");

            int aodA = clampAlpha(Math.round(AOD_WIDGET_ALPHA * t));
            int aodGlass = (aodA << 24) | AOD_WIDGET_RGB;
            float invT = clamp01(1.0f - t);

            Object newMix = startMix;
            if (startMix != null && sMixColorCtor != null && !(origin != null && multiCls.isInstance(origin))) {
                int topColor = readIntField(startMix, "topLayerColor", 0);
                int fadedTop = scaleArgbAlpha(topColor, invT);
                newMix = sMixColorCtor.newInstance(1, fadedTop, aodGlass);
            }

            Object newMulti = startMulti;
            if (startMulti != null && origin != null && multiCls.isInstance(origin)
                    && sMixMultiCtor != null && sMixShaderCtor != null) {
                Object fgShader = callNoArg(startMulti, "getForegroundShaderParam");
                Object newFg;
                if (fgShader != null) {
                    int topMode = readIntField(fgShader, "topMode", 0);
                    int bottomMode = readIntField(fgShader, "bottomMode", 0);
                    int fgTop = scaleArgbAlpha(readIntField(fgShader, "topLayerColor", 0), invT);
                    int fgBottom = scaleArgbAlpha(readIntField(fgShader, "bottomLayerColor", 0), invT);
                    newFg = sMixShaderCtor.newInstance(topMode, fgTop, bottomMode, fgBottom);
                } else {
                    newFg = sMixShaderCtor.newInstance(0, 0, 0, 0);
                }
                Object newBg = sMixShaderCtor.newInstance(1, aodGlass, 0, 0);
                float mirrorScale = readFloatField(startMulti, "mirrorScale", 1.0f);
                boolean alphaWithBlur = Boolean.TRUE.equals(readField(startMulti, "alphaWithBlurAmount"));
                int placeholder = readIntField(startMulti, "placeHolderColor", 0);
                newMulti = sMixMultiCtor.newInstance(newFg, newBg, mirrorScale, alphaWithBlur, placeholder);
            }

            Method mCopy = findMethod(startData.getClass(), "copy",
                    int.class, mixColorCls, mixCfgCls, multiCls);
            if (mCopy != null) {
                return mCopy.invoke(startData, type, newMix, origin, newMulti);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void ensureMixColorCtor(ClassLoader cl) throws Exception {
        if (sMixColorCtor != null) {
            return;
        }
        Class<?> mixColorCls = loadClass(C_MIX_COLOR, cl);
        if (mixColorCls != null) {
            Constructor<?> c = mixColorCls.getDeclaredConstructor(int.class, int.class, int.class);
            c.setAccessible(true);
            sMixColorCtor = c;
        }
    }

    private static int scaleArgbAlpha(int argb, float factor) {
        int a = clampAlpha(Math.round(((argb >>> 24) & 0xFF) * clamp01(factor)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static int readIntField(Object obj, String name, int defVal) {
        Object val = readField(obj, name);
        return val instanceof Integer ? (Integer) val : defVal;
    }

    private static void ensureMixCtors(ClassLoader cl) throws Exception {
        if (sMixShaderCtor != null && sMixSingleCtor != null) {
            return;
        }
        Class<?> shaderCls = loadClass(C_MIX_SHADER, cl);
        Class<?> singleCls = loadClass(C_MIX_SINGLE, cl);
        Class<?> multiCls = loadClass(C_MIX_MULTI, cl);
        if (shaderCls == null || singleCls == null) {
            throw new IllegalStateException("Missing MixColorWithShader / BlurMixSingleWithShader");
        }
        sMixShaderCtor = shaderCls.getDeclaredConstructor(int.class, int.class, int.class, int.class);
        sMixShaderCtor.setAccessible(true);

        sMixSingleCtor = singleCls.getDeclaredConstructor(shaderCls);
        sMixSingleCtor.setAccessible(true);

        if (multiCls != null) {
            sMixMultiCtor = multiCls.getDeclaredConstructor(
                    shaderCls, shaderCls, float.class, boolean.class, int.class);
            sMixMultiCtor.setAccessible(true);
        }
    }

    private static int clampAlpha(int a) {
        if (a < 0) return 0;
        if (a > 255) return 255;
        return a;
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    private static float readFloatField(Object obj, String name, float defVal) {
        Object val = readField(obj, name);
        return val instanceof Float ? (Float) val : defVal;
    }

    private static Class<?> loadClass(String name, ClassLoader cl) {
        if (name == null) {
            return null;
        }
        ConcurrentHashMap<String, Object> map;
        if (cl == null) {
            map = NULL_CL_CLASS_CACHE;
        } else if (cl == sPrimaryCl && sPrimaryClassMap != null) {
            map = sPrimaryClassMap;
        } else {
            synchronized (CLASS_CACHE_LOCK) {
                map = CLASS_CACHE.get(cl);
                if (map == null) {
                    map = new ConcurrentHashMap<>();
                    CLASS_CACHE.put(cl, map);
                }
                if (sPrimaryCl == null) {
                    sPrimaryCl = cl;
                    sPrimaryClassMap = map;
                }
            }
        }
        Object cached = map.get(name);
        if (cached != null) {
            return cached == MISSING ? null : (Class<?>) cached;
        }
        Class<?> found = null;
        try {
            found = Class.forName(name, false, cl);
        } catch (Throwable ignored) {
        }
        map.putIfAbsent(name, found != null ? found : MISSING);
        return found;
    }

    private static Field findFieldCached(Class<?> cls, String name) {
        if (cls == null || name == null) {
            return null;
        }
        ConcurrentHashMap<String, Object> map = FIELD_CACHE.get(cls);
        if (map == null) {
            ConcurrentHashMap<String, Object> fresh = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, Object> prev = FIELD_CACHE.putIfAbsent(cls, fresh);
            map = prev != null ? prev : fresh;
        }
        Object cached = map.get(name);
        if (cached != null) {
            return cached == MISSING ? null : (Field) cached;
        }
        Field found = null;
        Class<?> c = cls;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                found = f;
                break;
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            } catch (Throwable ignored) {
                break;
            }
        }
        map.putIfAbsent(name, found != null ? found : MISSING);
        return found;
    }

    private static Method findSingleTypedMethodCached(ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> cache,
                                                      Class<?> cls, String name, Class<?> paramType) {
        if (cls == null || name == null) {
            return null;
        }
        ConcurrentHashMap<String, Object> map = cache.get(cls);
        if (map == null) {
            ConcurrentHashMap<String, Object> fresh = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, Object> prev = cache.putIfAbsent(cls, fresh);
            map = prev != null ? prev : fresh;
        }
        Object cached = map.get(name);
        if (cached != null) {
            return cached == MISSING ? null : (Method) cached;
        }
        Method found = null;
        Class<?> c = cls;
        while (c != null) {
            try {
                Method m = paramType == null
                        ? c.getDeclaredMethod(name)
                        : c.getDeclaredMethod(name, paramType);
                m.setAccessible(true);
                found = m;
                break;
            } catch (NoSuchMethodException ignored) {
                c = c.getSuperclass();
            } catch (Throwable ignored) {
                break;
            }
        }
        map.putIfAbsent(name, found != null ? found : MISSING);
        return found;
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... params) {
        if (cls == null || name == null) {
            return null;
        }
        if (params == null || params.length == 0) {
            return findSingleTypedMethodCached(NO_ARG_METHOD_CACHE, cls, name, null);
        }
        if (params.length == 1) {
            Class<?> p0 = params[0];
            if (p0 == int.class) {
                return findSingleTypedMethodCached(INT_METHOD_CACHE, cls, name, int.class);
            }
            if (p0 == float.class) {
                return findSingleTypedMethodCached(FLOAT_METHOD_CACHE, cls, name, float.class);
            }
            if (p0 == boolean.class) {
                return findSingleTypedMethodCached(BOOL_METHOD_CACHE, cls, name, boolean.class);
            }
        }
        ConcurrentHashMap<String, Object> map = PARAM_METHOD_CACHE.get(cls);
        if (map == null) {
            ConcurrentHashMap<String, Object> fresh = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, Object> prev = PARAM_METHOD_CACHE.putIfAbsent(cls, fresh);
            map = prev != null ? prev : fresh;
        }
        String key;
        if (params.length == 1) {
            key = name + "|" + (params[0] != null ? params[0].getName() : "null");
        } else {
            StringBuilder sb = new StringBuilder(name);
            for (Class<?> p : params) {
                sb.append('|').append(p != null ? p.getName() : "null");
            }
            key = sb.toString();
        }
        Object cached = map.get(key);
        if (cached != null) {
            return cached == MISSING ? null : (Method) cached;
        }
        Method found = null;
        Class<?> c = cls;
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                found = m;
                break;
            } catch (NoSuchMethodException ignored) {
                c = c.getSuperclass();
            } catch (Throwable ignored) {
                break;
            }
        }
        map.putIfAbsent(key, found != null ? found : MISSING);
        return found;
    }

    private static Object readField(Object obj, String name) {
        if (obj == null) {
            return null;
        }
        try {
            Field f = findFieldCached(obj.getClass(), name);
            return f != null ? f.get(obj) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void writeField(Object obj, String name, Object val) {
        if (obj == null) {
            return;
        }
        try {
            Field f = findFieldCached(obj.getClass(), name);
            if (f != null) {
                f.set(obj, val);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void writeIntField(Object obj, String name, int val) {
        if (obj == null) {
            return;
        }
        try {
            Field f = findFieldCached(obj.getClass(), name);
            if (f != null) {
                f.setInt(obj, val);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object readStaticField(Class<?> cls, String name) {
        if (cls == null) {
            return null;
        }
        try {
            Field f = findFieldCached(cls, name);
            return f != null ? f.get(null) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object callNoArg(Object obj, String name) {
        if (obj == null) {
            return null;
        }
        try {
            Method m = findSingleTypedMethodCached(NO_ARG_METHOD_CACHE, obj.getClass(), name, null);
            return m == null ? null : m.invoke(obj);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object callOneInt(Object obj, String name, int val) {
        if (obj == null) {
            return null;
        }
        try {
            Method m = findSingleTypedMethodCached(INT_METHOD_CACHE, obj.getClass(), name, int.class);
            return m == null ? null : m.invoke(obj, val);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object callOneFloat(Object obj, String name, float val) {
        if (obj == null) {
            return null;
        }
        try {
            Method m = findSingleTypedMethodCached(FLOAT_METHOD_CACHE, obj.getClass(), name, float.class);
            return m == null ? null : m.invoke(obj, val);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object callOneBool(Object obj, String name, boolean val) {
        if (obj == null) {
            return null;
        }
        try {
            Method m = findSingleTypedMethodCached(BOOL_METHOD_CACHE, obj.getClass(), name, boolean.class);
            return m == null ? null : m.invoke(obj, val);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void callSetter(Object obj, String name, String paramClsName, ClassLoader cl, Object arg) {
        if (obj == null || arg == null) {
            return;
        }
        callSetterAllowNull(obj, name, paramClsName, cl, arg);
    }

    private static void callSetterAllowNull(Object obj, String name, String paramClsName, ClassLoader cl, Object arg) {
        if (obj == null) {
            return;
        }
        try {
            Class<?> paramCls = loadClass(paramClsName, cl);
            if (paramCls == null && arg != null) {
                paramCls = arg.getClass();
            }
            if (paramCls == null) {
                return;
            }
            Method m = findMethod(obj.getClass(), name, paramCls);
            if (m != null) {
                m.invoke(obj, arg);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object staticCallOneArg(ClassLoader cl, String clsName, String methodName, Object arg) {
        try {
            Class<?> c = loadClass(clsName, cl);
            if (c == null) {
                return null;
            }
            ConcurrentHashMap<String, Object> map = ONE_ARG_STATIC_CACHE.get(c);
            if (map == null) {
                ConcurrentHashMap<String, Object> fresh = new ConcurrentHashMap<>();
                ConcurrentHashMap<String, Object> prev = ONE_ARG_STATIC_CACHE.putIfAbsent(c, fresh);
                map = prev != null ? prev : fresh;
            }
            String key = methodName + "|" + (arg != null ? arg.getClass().getName() : "null");
            Object cached = map.get(key);
            Method target = null;
            if (cached != null) {
                if (cached == MISSING) {
                    return null;
                }
                target = (Method) cached;
            } else {
                Method fallback = null;
                for (Method m : c.getDeclaredMethods()) {
                    if (methodName.equals(m.getName()) && m.getParameterTypes().length == 1) {
                        Class<?> pt = m.getParameterTypes()[0];
                        if (arg == null || pt.isInstance(arg)) {
                            m.setAccessible(true);
                            target = m;
                            break;
                        }
                        if (fallback == null) {
                            m.setAccessible(true);
                            fallback = m;
                        }
                    }
                }
                if (target == null) {
                    target = fallback;
                }
                map.putIfAbsent(key, target != null ? target : MISSING);
            }
            return target != null ? target.invoke(null, arg) : null;
        } catch (Throwable ignored) {
        }
        return null;
    }
}
