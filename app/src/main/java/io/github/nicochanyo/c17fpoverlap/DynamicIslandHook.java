package io.github.nicochanyo.c17fpoverlap;

import android.animation.ValueAnimator;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.PathInterpolator;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;
import io.github.nicochanyo.c17fpoverlap.XposedCompat.MethodHookParam;
import io.github.nicochanyo.c17fpoverlap.XposedCompat.XC_MethodHook;

/**
 * Hooks dynamically loaded Expanded Dynamic Island views (SystemUIPlugin.apk via
 * SeedlingPluginManager) inside com.android.systemui to apply Fluid Cloud tinting,
 * blur scaling, top black gradient removal, clean translucent glass parameters,
 * and smooth black-to-transparent transitions.
 */
public final class DynamicIslandHook {
    private static final String CLS_SEEDLING_MGR = "com.oplus.systemui.statusbar.seeding.SeedlingPluginManager";
    private static final String CLS_CARD_BG = "com.oplus.systemui.plugins.seedling.card.ui.view.CardBackgroundView";
    private static final String CLS_CARD_GRAD = "com.oplus.systemui.plugins.seedling.card.ui.view.CardGradientOverlayView";
    private static final String CLS_GRAD_DRAWABLE = "com.oplus.systemui.plugins.seedling.card.ui.view.f";
    private static final String CLS_INSTANT = "com.oplus.systemui.plugins.seedling.card.ui.view.InstantContainer";
    private static final String CLS_ANIM_FRAGMENT = "com.oplus.systemui.plugins.seedling.card.animation.b";
    private static final String CLS_SCENE_BUILDER_HELPER = "com.heytap.log.collect.auto.d";
    private static final String CLS_BG_HELPER = "com.heytap.log.nx.obus.a";
    private static final String CLS_MULTI_LIGHT = "com.oplus.systemui.plugins.shared.template.component.light.f";
    private static final String CLS_MEDIA_BG_VIEW = "com.oplus.systemui.plugins.shared.template.component.media.view.MediaBackGroundView";
    private static final String CLS_PAGE_BG_VIEW = "com.oplus.systemui.plugins.shared.template.page.view.PageBackgroundView";
    private static final String CLS_LIGHT_EFFECT_MGR = "com.oplus.systemui.plugins.seedling.state.g";
    private static final String CLS_SYSUI_STATE_HELPER = "com.oplus.systemui.plugins.seedling.state.q";
    private static final String CLS_LA_RULE_MGR = "com.oplus.systemui.plugins.seedling.notification.rule.d";
    private static final String CLS_MEDIA_MULTI_CARD_SECTION =
            "com.oplus.systemui.plugins.shared.template.section.media.j1";
    private static final String CLS_MEDIA_MULTI_ADAPTER =
            "com.oplus.systemui.plugins.shared.template.section.media.e1";
    private static final String CLS_MEDIA_PLAYER_SECTION =
            "com.oplus.systemui.plugins.shared.template.section.media.x1";
    private static final String CLS_MEDIA_MINI_SECTION =
            "com.oplus.systemui.plugins.shared.template.section.media.M0";
    private static final String CLS_VIEW_PAGER2 = "androidx.viewpager2.widget.ViewPager2";
    private static final String CLS_CAPSULE_CONTAINER =
            "com.oplus.systemui.plugins.seedling.capsule.ui.view.CapsuleContainer";
    private static final String CLS_CARD_CONTAINER =
            "com.oplus.systemui.plugins.seedling.card.ui.view.CardContainer";
    private static final String MEDIA_SERVICE_ID = "268452006";
    private static final String EXTRA_DEBUG_LIVE_ALERT = "c17.debug.live_alert";

    private static final Map<ClassLoader, Boolean> HOOKED_LOADERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static final Map<Object, View> VRM_TO_VIEW =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Object> VIEW_TO_VRM =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Boolean> TAGGED_ISLAND_VIEW =
            Collections.synchronizedMap(new WeakHashMap<>());

    // Keyed directly by ViewRootManager (vrm) or mutable parameter objects with identity hashing
    private static final WeakIdentityMap<Object, Integer> ORIG_RADIUS =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<Object, Object> LAST_BLUR_PARAM =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<Object, float[]> ORIG_PARAM_FLOATS =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<Object, float[][]> ORIG_PARAM_COLORS =
            new WeakIdentityMap<>();
    private static final WeakIdentityMap<Object, Integer> LAST_VRM_HASH =
            new WeakIdentityMap<>();

    private static final Map<Drawable, int[]> ORIG_GRAD_STOPS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Drawable, Integer> LAST_GRAD_HASH =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Drawable, Long> LAST_GRAD_FAST_KEY =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Integer> LAST_BG_VIEW_FAST_HASH =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, Integer> LIGHT_COVER_COLOR =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, LightBaseline> LIGHT_BASELINES =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Integer> LAST_CARD_LIGHT_HASH =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Integer> CARD_MEDIA_RAW_RGB =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Drawable, Boolean> ACTIVE_LOCK_LIGHT_DRAWABLES =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, Boolean> ACTIVE_MEDIA_CAROUSELS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, Boolean> ACTIVE_MEDIA_SECTIONS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, Boolean> ACTIVE_MINI_SECTIONS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Boolean> ACTIVE_CAPSULE_CONTAINERS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Boolean> ACTIVE_CARD_CONTAINERS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Boolean> TAGGED_MEDIA_ATTACH_VIEWS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Drawable, Boolean> ACTIVE_MEDIA_LIGHT_DRAWABLES =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, String> ACTIVE_MEDIA_HOSTS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Object> MEDIA_BG_TO_SECTION =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Boolean> ACTIVE_GRADIENT_VIEWS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final ThreadLocal<Boolean> IN_REAPPLY = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IN_DEFERRED_LIGHT_EFFECT = new ThreadLocal<>();

    private static volatile ClassLoader sLastPluginCl;
    private static volatile int sSeedlingBaseBlurRadius = -1;
    private static volatile Method sVrmSetBlurParamsM;
    private static volatile Method sVrmSetBlurRadiusM;
    private static final Map<View, RowBgCache> ROW_BG_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Object MISSING = new Object();
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> FIELD_CACHE =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> NO_ARG_METHOD_CACHE =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> PARAM_METHOD_CACHE =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Object> GRAD_REF_CACHE =
            new ConcurrentHashMap<>();
    private static volatile Field sFldLightName;
    private static volatile Field sFldLightParams;
    private static volatile Field sFldUShadow;
    private static volatile Field sFldUEdge;
    private static volatile Field sFldShadowArr;
    private static volatile Field sFldEdgeArr;
    private static volatile Field sFldShadowDirty;
    private static volatile Field sFldEdgeDirty;
    private static final ThreadLocal<float[]> TL_LIGHT_RGB = new ThreadLocal<>();
    private static final ThreadLocal<float[]> TL_HSV_SCRATCH = new ThreadLocal<>();
    private static final int[] SHADOW_RESTORE_IDX = {0, 1, 2, 3, 4, 11, 12, 13};
    private static final int[] EDGE_RESTORE_IDX = {0, 1, 2, 3, 4, 5, 7, 10, 11};

    private static volatile boolean sLoggedFirstVrmApply;
    private static volatile boolean sLoggedAgslErr;
    private static volatile int sLatestMediaCoverColor;
    private static volatile int sLastMediaRawRgb;
    private static volatile int sMediaColorVersion = 1;
    private static volatile boolean sTopMediaFluidCloudActive;
    private static volatile boolean sHasTrackedTopContainers;
    private static volatile long sLastTopMediaProbeMs;
    private static volatile boolean sLastLockMediaActive;
    private static volatile long sLastLockMediaProbeMs;
    private static volatile boolean sLastShadeMediaActive;
    private static volatile long sLastShadeMediaProbeMs;
    private static volatile int sCachedMaskInRaw;
    private static volatile int sCachedMaskInBright = -1;
    private static volatile int sCachedMaskInChroma = -1;
    private static volatile int sCachedMaskOutRgb;
    private static volatile long sLastMediaColorPromotedAtMs;
    private static volatile long sLastActiveCheckMs;
    private static volatile boolean sEverTrackedMediaSurface;
    private static volatile ConfigStore.HookConfig sLastHookConfig;
    private static volatile WeakReference<Object> sLightEffectMgrRef;
    private static volatile Class<?> sSysUiStateHelperCls;
    private static volatile boolean sPendingLightEffectRefresh;
    private static volatile boolean sMediaColorRefreshPending;
    private static final Runnable MEDIA_RECONCILE_TASK = new Runnable() {
        @Override
        public void run() {
            reconcileActiveMediaColor(sLastHookConfig);
        }
    };
    private static final View.OnAttachStateChangeListener MEDIA_VIEW_ATTACH_LISTENER =
            new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    sLastMediaColorPromotedAtMs = 0L;
                    scheduleMediaColorReconcile(sLastHookConfig);
                }
            };
    private static final Runnable LIGHT_EFFECT_FALLBACK = new Runnable() {
        @Override
        public void run() {
            flushPendingAodLightDisable();
        }
    };

    private static final LinearGradient TRANSPARENT_SHADER = new LinearGradient(
            0f, 0f, 0f, 1f, 0, 0, Shader.TileMode.CLAMP);
    private static final WeakIdentityMap<Drawable, Object> LAST_PATH_TRIPLE = new WeakIdentityMap<>();
    private static volatile Field sSmoothPathRectField;
    private static volatile boolean sOplusPathAdapterProbed;
    private static volatile Constructor<?> sOplusPathAdapterCtor;
    private static volatile Method sAddSmoothRoundRectM;

    static boolean buildSmoothRoundRectPath(Path dest, float left, float top, float right, float bottom,
                                            float radius, float weight, boolean useG2) {
        if (dest == null) {
            return false;
        }
        dest.rewind();
        if (right <= left || bottom <= top) {
            return false;
        }
        if (useG2 && radius > 0.0f) {
            try {
                if (!sOplusPathAdapterProbed) {
                    sOplusPathAdapterProbed = true;
                    Class<?> cls = Class.forName("com.oplus.graphics.OplusPathAdapter");
                    Constructor<?> ctor = cls.getConstructor(Path.class, int.class);
                    Method m = cls.getMethod("addSmoothRoundRect",
                            float.class, float.class, float.class, float.class,
                            float.class, float.class, float.class, Path.Direction.class);
                    sOplusPathAdapterCtor = ctor;
                    sAddSmoothRoundRectM = m;
                }
                Constructor<?> ctor = sOplusPathAdapterCtor;
                Method m = sAddSmoothRoundRectM;
                if (ctor != null && m != null) {
                    Object adapter = ctor.newInstance(dest, 1);
                    float effWeight = weight > 0.0f ? weight : 3.0f;
                    m.invoke(adapter, left, top, right, bottom,
                            radius, radius, effWeight, Path.Direction.CCW);
                    if (!dest.isEmpty()) {
                        return true;
                    }
                    dest.rewind();
                }
            } catch (Throwable ignored) {
                dest.rewind();
            }
        }
        float r = Math.max(0.0f, radius);
        dest.addRoundRect(new RectF(left, top, right, bottom), r, r, Path.Direction.CW);
        return false;
    }

    // ColorOS 17 Y-UP AGSL SDF (G2 smoothSDF_D + RBox fallback) bottom inner glow + bottom-arc specular rim for Expanded Dynamic Island
    private static final String ISLAND_BOTTOM_GLOW_AGSL =
            "uniform float2 uSize;\n" +
            "uniform float uRadius;\n" +
            "uniform float uSmoothMode;\n" +
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
            "float rbox(float2 p, float2 b, float r) {\n" +
            "    float2 q = abs(p) - b + float2(r);\n" +
            "    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;\n" +
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
            "    if (uSmoothMode > 0.5) {\n" +
            "        return smoothSDF_D(p, b, rc, uWeight);\n" +
            "    }\n" +
            "    return rbox(p, b, rc);\n" +
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

    private static final ThreadLocal<RuntimeShader> TL_GLOW_SHADER = new ThreadLocal<>();
    private static final ThreadLocal<Paint> TL_GLOW_PAINT = new ThreadLocal<>();

    private DynamicIslandHook() {
    }

    public static void install(ClassLoader sysUiCl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> mgrCls = XposedCompat.findClassIfExists(CLS_SEEDLING_MGR, sysUiCl);
            if (mgrCls == null) {
                cfg.note("DynamicIsland: SeedlingPluginManager not found");
                return;
            }
            XposedCompat.hookAllMethods(mgrCls, "onPluginConnected", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args.length == 0 || param.args[0] == null) {
                        return;
                    }
                    ClassLoader pluginCl = param.args[0].getClass().getClassLoader();
                    if (pluginCl != null) {
                        installPluginHooks(pluginCl, cfg);
                    }
                }
            });
            cfg.note("DynamicIsland: SeedlingPluginManager hooked OK");
        } catch (Throwable t) {
            cfg.note("DynamicIsland: SeedlingPluginManager hook failed: " + t);
        }
    }

    private static void installPluginHooks(ClassLoader pluginCl, ConfigStore.HookConfig cfg) {
        sLastPluginCl = pluginCl;
        synchronized (HOOKED_LOADERS) {
            if (Boolean.TRUE.equals(HOOKED_LOADERS.get(pluginCl))) {
                return;
            }
            HOOKED_LOADERS.put(pluginCl, Boolean.TRUE);
        }

        int count = 0;
        if (hookBackgroundHelper(pluginCl, cfg)) count++;
        if (hookGradientOverlayDrawable(pluginCl, cfg)) count++;
        if (hookCardBackgroundView(pluginCl, cfg)) count++;
        if (hookCardGradientOverlayView(pluginCl, cfg)) count++;
        if (hookInstantContainer(pluginCl, cfg)) count++;
        if (hookTransitionAnimatorFragments(pluginCl, cfg)) count++;
        if (hookMultiLightDrawable(pluginCl, cfg)) count++;
        if (hookMediaMultiCardSection(pluginCl, cfg)) count++;
        if (hookMediaPlayerSection(pluginCl, cfg)) count++;
        if (hookMediaMiniSection(pluginCl, cfg)) count++;
        if (hookLightEffectManager(pluginCl, cfg)) count++;
        if (hookLiveAlertDebugWhitelist(pluginCl, cfg)) count++;
        if (hookTopFluidCloudContainers(pluginCl, cfg)) count++;
        cfg.note("DynamicIsland plugin hooks installed: " + count + "/13");
    }

    private static boolean hookTopFluidCloudContainers(ClassLoader pluginCl, final ConfigStore.HookConfig cfg) {
        int hooked = 0;
        try {
            Class<?> capCls = XposedCompat.findClassIfExists(CLS_CAPSULE_CONTAINER, pluginCl);
            if (capCls != null) {
                XC_MethodHook capTrackHook = new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.thisObject instanceof View) {
                            View v = (View) param.thisObject;
                            ACTIVE_CAPSULE_CONTAINERS.put(v, Boolean.TRUE);
                            sHasTrackedTopContainers = true;
                            refreshTopMediaFluidCloudState(cfg);
                        }
                    }
                };
                XposedCompat.hookAllConstructors(capCls, capTrackHook);
                XposedCompat.hookAllMethods(capCls, "onFinishInflate", capTrackHook);
                XposedCompat.hookAllMethods(capCls, "h", capTrackHook);
                XposedCompat.hookAllMethods(capCls, "onViewAdded", capTrackHook);
                XposedCompat.hookAllMethods(capCls, "onViewRemoved", capTrackHook);
                XposedCompat.hookAllMethods(capCls, "onDetachedFromWindow", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.thisObject instanceof View) {
                            ACTIVE_CAPSULE_CONTAINERS.remove(param.thisObject);
                            refreshTopMediaFluidCloudState(cfg);
                        }
                    }
                });
                hooked++;
            }
        } catch (Throwable t) {
            cfg.note("DynamicIsland: CapsuleContainer hook failed: " + t);
        }

        try {
            Class<?> cardCls = XposedCompat.findClassIfExists(CLS_CARD_CONTAINER, pluginCl);
            if (cardCls != null) {
                XC_MethodHook cardTrackHook = new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.thisObject instanceof View) {
                            View v = (View) param.thisObject;
                            ACTIVE_CARD_CONTAINERS.put(v, Boolean.TRUE);
                            sHasTrackedTopContainers = true;
                            refreshTopMediaFluidCloudState(cfg);
                        }
                    }
                };
                XposedCompat.hookAllConstructors(cardCls, cardTrackHook);
                XposedCompat.hookAllMethods(cardCls, "onAttachedToWindow", cardTrackHook);
                XposedCompat.hookAllMethods(cardCls, "i", cardTrackHook);
                XposedCompat.hookAllMethods(cardCls, "onViewAdded", cardTrackHook);
                XposedCompat.hookAllMethods(cardCls, "onDetachedFromWindow", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.thisObject instanceof View) {
                            ACTIVE_CARD_CONTAINERS.remove(param.thisObject);
                            refreshTopMediaFluidCloudState(cfg);
                        }
                    }
                });
                hooked++;
            }
        } catch (Throwable t) {
            cfg.note("DynamicIsland: CardContainer hook failed: " + t);
        }
        return hooked > 0;
    }

    private static boolean hasMediaServiceKey(String key) {
        return key != null && key.contains(MEDIA_SERVICE_ID);
    }

    private static boolean probeTopMediaFluidCloudActiveNow() {
        List<View> capsules;
        synchronized (ACTIVE_CAPSULE_CONTAINERS) {
            capsules = ACTIVE_CAPSULE_CONTAINERS.isEmpty()
                    ? Collections.<View>emptyList()
                    : new ArrayList<>(ACTIVE_CAPSULE_CONTAINERS.keySet());
        }
        for (int i = 0, n = capsules.size(); i < n; i++) {
            View c = capsules.get(i);
            if (c == null || !c.isAttachedToWindow() || c.getVisibility() != View.VISIBLE) {
                continue;
            }
            Object listObj = readObjectField(c, "k");
            if (listObj instanceof List<?>) {
                List<?> items = (List<?>) listObj;
                for (int j = 0, m = items.size(); j < m; j++) {
                    Object item = items.get(j);
                    if (item == null) {
                        continue;
                    }
                    Object keyObj = readObjectField(item, "a");
                    if (keyObj instanceof String && hasMediaServiceKey((String) keyObj)) {
                        View itemView = readViewField(item, "b");
                        if (itemView == null || itemView.getVisibility() == View.VISIBLE) {
                            return true;
                        }
                    }
                }
            } else {
                Object keysObj = invokeNoArg(c, "getShowingKeyList");
                if (keysObj instanceof List<?>) {
                    List<?> keys = (List<?>) keysObj;
                    for (int j = 0, m = keys.size(); j < m; j++) {
                        Object ko = keys.get(j);
                        if (ko instanceof String && hasMediaServiceKey((String) ko)) {
                            return true;
                        }
                    }
                }
            }
        }

        List<View> cards;
        synchronized (ACTIVE_CARD_CONTAINERS) {
            cards = ACTIVE_CARD_CONTAINERS.isEmpty()
                    ? Collections.<View>emptyList()
                    : new ArrayList<>(ACTIVE_CARD_CONTAINERS.keySet());
        }
        for (int i = 0, n = cards.size(); i < n; i++) {
            View c = cards.get(i);
            if (c == null || !c.isAttachedToWindow() || !c.isShown()) {
                continue;
            }
            Object listObj = readObjectField(c, "c");
            if (listObj instanceof Iterable<?>) {
                for (Object item : (Iterable<?>) listObj) {
                    if (item == null) {
                        continue;
                    }
                    Object keyObj = readObjectField(item, "a");
                    if (keyObj instanceof String && hasMediaServiceKey((String) keyObj)) {
                        return true;
                    }
                }
            } else {
                Object keysObj = invokeNoArg(c, "getShowingKeyList");
                if (keysObj instanceof List<?>) {
                    List<?> keys = (List<?>) keysObj;
                    for (int j = 0, m = keys.size(); j < m; j++) {
                        Object ko = keys.get(j);
                        if (ko instanceof String && hasMediaServiceKey((String) ko)) {
                            return true;
                        }
                    }
                }
            }
        }

        List<Object> sections;
        synchronized (ACTIVE_MEDIA_SECTIONS) {
            sections = ACTIVE_MEDIA_SECTIONS.isEmpty()
                    ? Collections.emptyList()
                    : new ArrayList<>(ACTIVE_MEDIA_SECTIONS.keySet());
        }
        for (int i = 0, n = sections.size(); i < n; i++) {
            Object sec = sections.get(i);
            if (sec == null) {
                continue;
            }
            View mediaBg = readViewField(sec, "s");
            if (mediaBg == null || !mediaBg.isAttachedToWindow() || !mediaBg.isShown()) {
                continue;
            }
            Object lightObj = readObjectField(mediaBg, "F");
            Drawable lightDr = lightObj instanceof Drawable ? (Drawable) lightObj : null;
            if ("MP_StatusBar".equals(resolveMediaSectionName(sec, mediaBg, lightDr))) {
                return true;
            }
        }
        return false;
    }

    static void refreshTopMediaFluidCloudState(ConfigStore.HookConfig cfg) {
        refreshTopMediaFluidCloudState(cfg, true);
    }

    static void refreshTopMediaFluidCloudState(ConfigStore.HookConfig cfg, boolean force) {
        long now = SystemClock.uptimeMillis();
        if (!force && sHasTrackedTopContainers && (now - sLastTopMediaProbeMs) < 150L) {
            return;
        }
        sLastTopMediaProbeMs = now;
        boolean active = probeTopMediaFluidCloudActiveNow();
        if (active != sTopMediaFluidCloudActive) {
            sTopMediaFluidCloudActive = active;
            sMediaColorVersion++;
            LAST_GRAD_FAST_KEY.clear();
            LAST_BG_VIEW_FAST_HASH.clear();
            ConfigStore.HookConfig eff = cfg != null ? cfg : sLastHookConfig;
            if (eff != null) {
                notifyMediaColorChanged(eff);
            }
        }
    }

    static int getMediaColorVersion() {
        return sMediaColorVersion;
    }

    static void bumpMediaColorVersion() {
        sMediaColorVersion++;
        sLastTopMediaProbeMs = 0L;
        sLastLockMediaProbeMs = 0L;
        sLastShadeMediaProbeMs = 0L;
    }

    static boolean hasActiveTopMediaFluidCloud() {
        refreshTopMediaFluidCloudState(sLastHookConfig, false);
        return sTopMediaFluidCloudActive;
    }

    static boolean hasActiveLockMediaFluidCloud() {
        if (!FluidCloudHook.isCurrentlyOnLockscreen()) {
            sLastLockMediaActive = false;
            sLastLockMediaProbeMs = 0L;
            return false;
        }
        long now = SystemClock.uptimeMillis();
        if ((now - sLastLockMediaProbeMs) < 150L) {
            return sLastLockMediaActive;
        }
        boolean active = probeLockMediaFluidCloudActiveNow();
        sLastLockMediaActive = active;
        sLastLockMediaProbeMs = now;
        return active;
    }

    private static boolean probeLockMediaFluidCloudActiveNow() {
        if (FluidCloudHook.hasActiveMediaCustomRowOnSurface(true)) {
            return true;
        }
        List<Object> minis;
        synchronized (ACTIVE_MINI_SECTIONS) {
            minis = ACTIVE_MINI_SECTIONS.isEmpty()
                    ? Collections.emptyList()
                    : new ArrayList<>(ACTIVE_MINI_SECTIONS.keySet());
        }
        for (int i = 0, n = minis.size(); i < n; i++) {
            Object m0 = minis.get(i);
            if (m0 == null) {
                continue;
            }
            View c = readViewField(m0, "c");
            if (c != null && c.isAttachedToWindow() && c.getVisibility() == View.VISIBLE) {
                return true;
            }
        }
        List<Object> sections;
        synchronized (ACTIVE_MEDIA_SECTIONS) {
            sections = ACTIVE_MEDIA_SECTIONS.isEmpty()
                    ? Collections.emptyList()
                    : new ArrayList<>(ACTIVE_MEDIA_SECTIONS.keySet());
        }
        for (int i = 0, n = sections.size(); i < n; i++) {
            Object sec = sections.get(i);
            if (sec == null) {
                continue;
            }
            View mediaBg = readViewField(sec, "s");
            if (mediaBg == null || !mediaBg.isAttachedToWindow() || mediaBg.getVisibility() != View.VISIBLE) {
                continue;
            }
            Object lightObj = readObjectField(mediaBg, "F");
            Drawable lightDr = lightObj instanceof Drawable ? (Drawable) lightObj : null;
            if ("MP_Notification".equals(resolveMediaSectionName(sec, mediaBg, lightDr))) {
                return true;
            }
        }
        return false;
    }

    static boolean hasActiveLockMiniMedia() {
        List<Object> minis;
        synchronized (ACTIVE_MINI_SECTIONS) {
            minis = ACTIVE_MINI_SECTIONS.isEmpty()
                    ? Collections.emptyList()
                    : new ArrayList<>(ACTIVE_MINI_SECTIONS.keySet());
        }
        for (int i = 0, n = minis.size(); i < n; i++) {
            Object m0 = minis.get(i);
            if (m0 == null) {
                continue;
            }
            View c = readViewField(m0, "c");
            if (c != null && c.isAttachedToWindow() && c.getVisibility() == View.VISIBLE) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDescendantOf(View child, View ancestor) {
        View cur = child;
        for (int depth = 0; depth < 20 && cur != null; depth++) {
            if (cur == ancestor) {
                return true;
            }
            ViewParent p = cur.getParent();
            cur = p instanceof View ? (View) p : null;
        }
        return false;
    }

    static boolean isViewContainingMedia(View root) {
        if (root == null) {
            return false;
        }
        if (hasMediaBgViewInTree(root)) {
            return true;
        }
        List<Object> minis;
        synchronized (ACTIVE_MINI_SECTIONS) {
            minis = ACTIVE_MINI_SECTIONS.isEmpty()
                    ? Collections.emptyList()
                    : new ArrayList<>(ACTIVE_MINI_SECTIONS.keySet());
        }
        for (int i = 0, n = minis.size(); i < n; i++) {
            Object m0 = minis.get(i);
            if (m0 == null) {
                continue;
            }
            View c = readViewField(m0, "c");
            if (c != null && c.isAttachedToWindow() && c.getVisibility() == View.VISIBLE
                    && isDescendantOf(c, root)) {
                return true;
            }
        }
        List<Object> sections;
        synchronized (ACTIVE_MEDIA_SECTIONS) {
            sections = ACTIVE_MEDIA_SECTIONS.isEmpty()
                    ? Collections.emptyList()
                    : new ArrayList<>(ACTIVE_MEDIA_SECTIONS.keySet());
        }
        for (int i = 0, n = sections.size(); i < n; i++) {
            Object sec = sections.get(i);
            if (sec == null) {
                continue;
            }
            View s = readViewField(sec, "s");
            if (s != null && s.isAttachedToWindow() && s.getVisibility() == View.VISIBLE
                    && isDescendantOf(s, root)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasActiveShadeMediaFluidCloud() {
        if (FluidCloudHook.isCurrentlyOnLockscreen()) {
            sLastShadeMediaActive = false;
            sLastShadeMediaProbeMs = 0L;
            return false;
        }
        long now = SystemClock.uptimeMillis();
        if ((now - sLastShadeMediaProbeMs) < 150L) {
            return sLastShadeMediaActive;
        }
        boolean active = probeShadeMediaFluidCloudActiveNow();
        sLastShadeMediaActive = active;
        sLastShadeMediaProbeMs = now;
        return active;
    }

    private static boolean probeShadeMediaFluidCloudActiveNow() {
        if (FluidCloudHook.hasActiveMediaCustomRowOnSurface(false)) {
            return true;
        }
        List<Object> sections;
        synchronized (ACTIVE_MEDIA_SECTIONS) {
            sections = ACTIVE_MEDIA_SECTIONS.isEmpty()
                    ? Collections.emptyList()
                    : new ArrayList<>(ACTIVE_MEDIA_SECTIONS.keySet());
        }
        for (int i = 0, n = sections.size(); i < n; i++) {
            Object sec = sections.get(i);
            if (sec == null) {
                continue;
            }
            View mediaBg = readViewField(sec, "s");
            if (mediaBg == null || !mediaBg.isAttachedToWindow() || !mediaBg.isShown()) {
                continue;
            }
            Object lightObj = readObjectField(mediaBg, "F");
            Drawable lightDr = lightObj instanceof Drawable ? (Drawable) lightObj : null;
            if ("MP_Notification".equals(resolveMediaSectionName(sec, mediaBg, lightDr))) {
                return true;
            }
        }
        return false;
    }

    static int getScopedMediaMaskRgb(ConfigStore.HookConfig cfg, boolean isMedia,
                                     boolean isTopSurface, boolean onLock) {
        if (cfg == null) {
            return 0;
        }
        if (!isMedia) {
            boolean surfaceActive = isTopSurface
                    ? hasActiveTopMediaFluidCloud()
                    : (onLock ? hasActiveLockMediaFluidCloud() : hasActiveShadeMediaFluidCloud());
            if (!surfaceActive) {
                return 0;
            }
        }
        return getLastMediaMaskRgb(cfg);
    }

    static int resolveScopedMediaTintArgb(boolean isMedia, boolean isTopSurface, boolean onLock) {
        if (!isMedia) {
            boolean surfaceActive = isTopSurface
                    ? hasActiveTopMediaFluidCloud()
                    : (onLock ? hasActiveLockMediaFluidCloud() : hasActiveShadeMediaFluidCloud());
            if (!surfaceActive) {
                return 0;
            }
        }
        return resolveMediaTintArgb();
    }

    private static boolean hookLiveAlertDebugWhitelist(ClassLoader pluginCl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> ruleCls = XposedCompat.findClassIfExists(CLS_LA_RULE_MGR, pluginCl);
            if (ruleCls == null) {
                return false;
            }
            Method target = null;
            Class<?> styleCls = null;
            for (Method m : ruleCls.getDeclaredMethods()) {
                if ("h".equals(m.getName())) {
                    Class<?>[] pts = m.getParameterTypes();
                    if (pts.length == 2 && android.service.notification.StatusBarNotification.class.isAssignableFrom(pts[0])) {
                        m.setAccessible(true);
                        target = m;
                        styleCls = pts[1];
                        break;
                    }
                }
            }
            if (target == null) {
                return false;
            }
            Object noneStyleTmp = null;
            try {
                Field fNone = styleCls.getDeclaredField("b");
                fNone.setAccessible(true);
                noneStyleTmp = fNone.get(null);
            } catch (Throwable ignored) {
            }
            final Object noneStyle = noneStyleTmp;
            XposedCompat.hookMethod(target, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!cfg.isFcDebugMode() || param.args.length < 2) {
                        return;
                    }
                    Object sbnObj = param.args[0];
                    Object styleObj = param.args[1];
                    if (!(sbnObj instanceof android.service.notification.StatusBarNotification) || styleObj == null) {
                        return;
                    }
                    if ((noneStyle != null && styleObj == noneStyle) || "NONE".equals(String.valueOf(styleObj))) {
                        return;
                    }
                    android.service.notification.StatusBarNotification sbn =
                            (android.service.notification.StatusBarNotification) sbnObj;
                    if (!ConfigStore.PKG_NAME.equals(sbn.getPackageName())) {
                        return;
                    }
                    android.app.Notification n = sbn.getNotification();
                    if (n == null || n.extras == null || !n.extras.getBoolean(EXTRA_DEBUG_LIVE_ALERT, false)) {
                        return;
                    }
                    if ((n.flags & android.app.Notification.FLAG_GROUP_SUMMARY) != 0) {
                        return;
                    }
                    param.setResult(Boolean.TRUE);
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("DynamicIsland: LiveAlert debug whitelist hook failed: " + t);
            return false;
        }
    }

    private static boolean hookLightEffectManager(ClassLoader pluginCl, final ConfigStore.HookConfig cfg) {
        try {
            sSysUiStateHelperCls = XposedCompat.findClassIfExists(CLS_SYSUI_STATE_HELPER, pluginCl);
            Class<?> mgrCls = XposedCompat.findClassIfExists(CLS_LIGHT_EFFECT_MGR, pluginCl);
            if (mgrCls == null) {
                return false;
            }
            Method mUpdate = findMethod(mgrCls, "g");
            if (mUpdate == null) {
                return false;
            }
            XposedCompat.hookMethod(mUpdate, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.thisObject != null) {
                        sLightEffectMgrRef = new WeakReference<>(param.thisObject);
                    }
                    if (Boolean.TRUE.equals(IN_DEFERRED_LIGHT_EFFECT.get()) || !cfg.isFcEnabled()) {
                        return;
                    }
                    boolean pluginPanAod = isPluginPanoramicAod();
                    if (!pluginPanAod) {
                        cancelPendingAodLightDisable();
                        return;
                    }
                    if (!FluidCloudHook.isAodVisualSettledInAod()) {
                        sPendingLightEffectRefresh = true;
                        MAIN_HANDLER.removeCallbacks(LIGHT_EFFECT_FALLBACK);
                        MAIN_HANDLER.postDelayed(LIGHT_EFFECT_FALLBACK, 1000L);
                        param.setResult(null);
                    }
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("DynamicIsland LightEffectManager hook err: " + t);
            return false;
        }
    }

    private static boolean isPluginPanoramicAod() {
        Class<?> helperCls = sSysUiStateHelperCls;
        if (helperCls == null) {
            return false;
        }
        try {
            Field fInst = helperCls.getDeclaredField("j");
            fInst.setAccessible(true);
            Object helper = fInst.get(null);
            if (helper == null) {
                return false;
            }
            Method mPan = findMethod(helper.getClass(), "c");
            Object res = mPan != null ? mPan.invoke(helper) : null;
            return Boolean.TRUE.equals(res);
        } catch (Throwable ignored) {
            return false;
        }
    }

    static void flushPendingAodLightDisable() {
        MAIN_HANDLER.removeCallbacks(LIGHT_EFFECT_FALLBACK);
        if (!sPendingLightEffectRefresh) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    flushPendingAodLightDisable();
                }
            });
            return;
        }
        sPendingLightEffectRefresh = false;
        Object mgr = sLightEffectMgrRef != null ? sLightEffectMgrRef.get() : null;
        if (mgr == null) {
            return;
        }
        IN_DEFERRED_LIGHT_EFFECT.set(Boolean.TRUE);
        try {
            Method mUpdate = findMethod(mgr.getClass(), "g");
            if (mUpdate != null) {
                mUpdate.invoke(mgr);
            }
        } catch (Throwable ignored) {
        } finally {
            IN_DEFERRED_LIGHT_EFFECT.remove();
        }
    }

    static void cancelPendingAodLightDisable() {
        sPendingLightEffectRefresh = false;
        MAIN_HANDLER.removeCallbacks(LIGHT_EFFECT_FALLBACK);
    }

    static void invalidateActiveLockLights() {
        List<Drawable> list = new ArrayList<>();
        synchronized (ACTIVE_LOCK_LIGHT_DRAWABLES) {
            if (!ACTIVE_LOCK_LIGHT_DRAWABLES.isEmpty()) {
                list.addAll(ACTIVE_LOCK_LIGHT_DRAWABLES.keySet());
            }
        }
        synchronized (LIGHT_BASELINES) {
            if (!LIGHT_BASELINES.isEmpty()) {
                for (Object k : LIGHT_BASELINES.keySet()) {
                    if (k instanceof Drawable && !list.contains(k)) {
                        list.add((Drawable) k);
                    }
                }
            }
        }
        for (int i = 0, n = list.size(); i < n; i++) {
            Drawable d = list.get(i);
            if (d == null) {
                continue;
            }
            LightBaseline bl = LIGHT_BASELINES.get(d);
            View host = (bl != null && bl.hostViewRef != null) ? bl.hostViewRef.get() : null;
            if (host == null) {
                host = resolveDrawableHostView(d);
            }
            if (host != null && (!host.isAttachedToWindow() || !host.isShown())) {
                continue;
            }
            d.invalidateSelf();
            if (host != null) {
                host.invalidate();
            }
        }
    }

    private static boolean isMediaLightName(String name) {
        return "MP_StatusBar".equals(name)
                || "MP_Notification".equals(name)
                || "MediaBackGroundView".equals(name);
    }

    private static int resolveMediaTintArgb() {
        ensureMediaStillActive();
        int argb = sLatestMediaCoverColor;
        if (argb != 0) {
            return argb;
        }
        int raw = sLastMediaRawRgb;
        return raw != 0 ? (0xFF000000 | raw) : 0;
    }

    static boolean isLargeMediaCardHeight(boolean isMedia, int heightPx, float density) {
        if (!isMedia) {
            return false;
        }
        float dp = density > 0.0f ? density : 3.0f;
        return heightPx <= 0 || heightPx >= Math.round(110.0f * dp);
    }

    private static void notifyMediaColorChanged(final ConfigStore.HookConfig cfg) {
        if (cfg == null || sMediaColorRefreshPending) {
            return;
        }
        sMediaColorRefreshPending = true;
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                sMediaColorRefreshPending = false;
                LAST_GRAD_FAST_KEY.clear();
                LAST_BG_VIEW_FAST_HASH.clear();
                boolean fcOn = cfg.isFcEnabled();
                if (!fcOn && !cfg.isLockNotifGlowFollowMedia()) {
                    FluidCloudHook.onMediaColorChanged(cfg);
                    return;
                }
                if (fcOn) {
                    List<View> gradViews;
                    synchronized (ACTIVE_GRADIENT_VIEWS) {
                        gradViews = ACTIVE_GRADIENT_VIEWS.isEmpty()
                                ? Collections.<View>emptyList()
                                : new ArrayList<>(ACTIVE_GRADIENT_VIEWS.keySet());
                    }
                    for (int i = 0, n = gradViews.size(); i < n; i++) {
                        View gv = gradViews.get(i);
                        if (gv != null && gv.isAttachedToWindow()) {
                            patchGradientOverlay(gv, cfg);
                        }
                    }
                    List<View> islandViews;
                    synchronized (TAGGED_ISLAND_VIEW) {
                        islandViews = TAGGED_ISLAND_VIEW.isEmpty()
                                ? Collections.<View>emptyList()
                                : new ArrayList<>(TAGGED_ISLAND_VIEW.keySet());
                    }
                    for (int i = 0, n = islandViews.size(); i < n; i++) {
                        View iv = islandViews.get(i);
                        if (iv != null && iv.isAttachedToWindow()) {
                            patchViewBackgroundLayers(iv, cfg);
                            iv.invalidate();
                        }
                    }
                    if (cfg.isFcColorEnabled() || cfg.isFcCustomMaskEnabled()) {
                        List<Object> vrms;
                        synchronized (VRM_TO_VIEW) {
                            vrms = VRM_TO_VIEW.isEmpty()
                                    ? Collections.emptyList()
                                    : new ArrayList<>(VRM_TO_VIEW.keySet());
                        }
                        for (int i = 0, n = vrms.size(); i < n; i++) {
                            Object vrm = vrms.get(i);
                            View vrmHost = vrm != null ? VRM_TO_VIEW.get(vrm) : null;
                            if (vrm != null && vrmHost != null && vrmHost.isAttachedToWindow()) {
                                ensureVrmSynced(vrm, cfg);
                            }
                        }
                    }
                    invalidateActiveLockLights();
                }
                FluidCloudHook.onMediaColorChanged(cfg);
            }
        });
    }

    private static boolean hookMultiLightDrawable(ClassLoader pluginCl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> lightCls = XposedCompat.findClassIfExists(CLS_MULTI_LIGHT, pluginCl);
            if (lightCls == null) {
                return false;
            }
            boolean hooked = false;
            XC_MethodHook paramHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.thisObject == null || param.args.length == 0 || param.args[0] == null) {
                        if (param.thisObject != null) {
                            LIGHT_COVER_COLOR.remove(param.thisObject);
                        }
                        return;
                    }
                    Object nameObj = readObjectField(param.thisObject, "b");
                    String name = nameObj != null ? String.valueOf(nameObj) : "";
                    if ("MP_MiniCard".equals(name)) {
                        return;
                    }
                    Object cObj = readObjectField(param.args[0], "c");
                    if (cObj instanceof Integer) {
                        int argb = (Integer) cObj;
                        int rgb = argb & 0x00FFFFFF;
                        if (rgb != 0 && rgb != 0x00FFFFFF) {
                            LIGHT_COVER_COLOR.put(param.thisObject, argb);
                            if (isMediaLightName(name)) {
                                Drawable lightDr = param.thisObject instanceof Drawable
                                        ? (Drawable) param.thisObject : null;
                                if (lightDr != null) {
                                    ACTIVE_MEDIA_LIGHT_DRAWABLES.put(lightDr, Boolean.TRUE);
                                }
                                View hostView = lightDr != null ? resolveDrawableHostView(lightDr) : null;
                                LightBaseline bl = lightDr != null ? LIGHT_BASELINES.get(lightDr) : null;
                                if (canMediaSurfaceUpdateGlobalColor(hostView, name, bl, false)) {
                                    promotePrimaryMediaColor(lightDr, hostView, argb, toRawMediaRgb(argb), cfg);
                                }
                            }
                        }
                    }
                }
            };
            for (Method m : lightCls.getDeclaredMethods()) {
                String mn = m.getName();
                if (("m".equals(mn) || "n".equals(mn)) && m.getParameterTypes().length == 1) {
                    XposedCompat.hookMethod(m, paramHook);
                    hooked = true;
                }
            }

            Method mDraw = XposedCompat.findDeclaredMethod(lightCls, "draw", Canvas.class);
            if (mDraw != null) {
                XposedCompat.hookDirect(mDraw, new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        Object self = chain.getThisObject();
                        if (self instanceof Drawable) {
                            applyMultiLightDrawHook((Drawable) self, cfg);
                        }
                        return chain.proceed();
                    }
                });
                hooked = true;
            }
            return hooked;
        } catch (Throwable t) {
            cfg.note("DynamicIsland: MultiLightDrawable hook failed: " + t);
            return false;
        }
    }

    private static void applyMultiLightDrawHook(Drawable lightDr, ConfigStore.HookConfig cfg) {
        try {
                            Field fName = sFldLightName;
                            if (fName == null || !fName.getDeclaringClass().isInstance(lightDr)) {
                                fName = findFieldCached(lightDr.getClass(), "b");
                                sFldLightName = fName;
                            }
                            Object nameObj = fName != null ? fName.get(lightDr) : null;
                            String name = nameObj != null ? String.valueOf(nameObj) : "";
                            Field fParams = sFldLightParams;
                            if (fParams == null || !fParams.getDeclaringClass().isInstance(lightDr)) {
                                fParams = findFieldCached(lightDr.getClass(), "e");
                                sFldLightParams = fParams;
                            }
                            Object params = fParams != null ? fParams.get(lightDr) : null;
                            if (params == null) {
                                return;
                            }
                            Field fUShadow = sFldUShadow;
                            if (fUShadow == null || !fUShadow.getDeclaringClass().isInstance(params)) {
                                fUShadow = findFieldCached(params.getClass(), "f");
                                sFldUShadow = fUShadow;
                            }
                            Field fUEdge = sFldUEdge;
                            if (fUEdge == null || !fUEdge.getDeclaringClass().isInstance(params)) {
                                fUEdge = findFieldCached(params.getClass(), "k");
                                sFldUEdge = fUEdge;
                            }
                            float uShadow = 0.0f;
                            if (fUShadow != null) {
                                if (fUShadow.getType() == float.class) {
                                    uShadow = fUShadow.getFloat(params);
                                } else {
                                    Object v = fUShadow.get(params);
                                    if (v instanceof Float) {
                                        uShadow = (Float) v;
                                    }
                                }
                            }
                            float uEdge = 0.0f;
                            if (fUEdge != null) {
                                if (fUEdge.getType() == float.class) {
                                    uEdge = fUEdge.getFloat(params);
                                } else {
                                    Object v = fUEdge.get(params);
                                    if (v instanceof Float) {
                                        uEdge = (Float) v;
                                    }
                                }
                            }
                            if (uShadow <= 0.5f && uEdge <= 0.5f) {
                                return;
                            }
                            Field fShadowArr = sFldShadowArr;
                            if (fShadowArr == null || !fShadowArr.getDeclaringClass().isInstance(params)) {
                                fShadowArr = findFieldCached(params.getClass(), "i");
                                sFldShadowArr = fShadowArr;
                            }
                            Field fEdgeArr = sFldEdgeArr;
                            if (fEdgeArr == null || !fEdgeArr.getDeclaringClass().isInstance(params)) {
                                fEdgeArr = findFieldCached(params.getClass(), "n");
                                sFldEdgeArr = fEdgeArr;
                            }
                            Object sa = fShadowArr != null ? fShadowArr.get(params) : null;
                            Object ea = fEdgeArr != null ? fEdgeArr.get(params) : null;
                            float[] shadowArr = sa instanceof float[] ? (float[]) sa : null;
                            float[] edgeArr = ea instanceof float[] ? (float[]) ea : null;

                            LightBaseline bl = LIGHT_BASELINES.get(lightDr);
                            if (bl == null) {
                                bl = new LightBaseline();
                                LIGHT_BASELINES.put(lightDr, bl);
                            }
                            View hostView = bl.hostViewRef != null ? bl.hostViewRef.get() : null;
                            if (hostView == null || !hostView.isAttachedToWindow()) {
                                View resolved = resolveDrawableHostView(lightDr);
                                if (resolved != hostView) {
                                    hostView = resolved;
                                    bl.hostViewRef = resolved != null ? new WeakReference<>(resolved) : null;
                                    bl.hostIsPageBgView = resolved != null
                                            && CLS_PAGE_BG_VIEW.equals(resolved.getClass().getName());
                                    bl.lastRecordedRawRgb = 0;
                                }
                                bl.customRowProbed = false;
                                bl.insideCustomRow = false;
                                bl.insideCapsuleItem = false;
                                bl.carouselProbed = false;
                                bl.vpRef = null;
                                bl.rvRef = null;
                                bl.pageItemRef = null;
                            }
                            boolean isLockscreenCard = false;
                            boolean inShade = false;
                            if ("MP_Notification".equals(name)) {
                                isLockscreenCard = FluidCloudHook.isCurrentlyOnLockscreen();
                                inShade = !isLockscreenCard;
                            } else if (hostView != null) {
                                if (!bl.customRowProbed) {
                                    probeLockscreenHostFlags(hostView, bl);
                                    bl.customRowProbed = true;
                                }
                                if (bl.insideCapsuleItem) {
                                    isLockscreenCard = true;
                                    inShade = false;
                                } else if (bl.insideCustomRow) {
                                    isLockscreenCard = FluidCloudHook.isCurrentlyOnLockscreen();
                                    inShade = !isLockscreenCard;
                                }
                            }
                            if (isLockscreenCard && !bl.registeredActiveLock) {
                                ACTIVE_LOCK_LIGHT_DRAWABLES.put(lightDr, Boolean.TRUE);
                                bl.registeredActiveLock = true;
                            }
                            float aodProg = (isLockscreenCard && cfg.isFcEnabled())
                                    ? FluidCloudHook.getCurrentAodProgress() : 0.0f;
                            boolean hostFadedByViewAlpha = bl.hostIsPageBgView
                                    && hostView != null
                                    && hostView.getAlpha() < 0.995f;
                            float aodLightScale = 1.0f;
                            if (aodProg > 0.001f && !hostFadedByViewAlpha) {
                                aodLightScale = bl.insideCapsuleItem
                                        ? clamp01((float) Math.pow(Math.max(0.0f, 1.0f - aodProg), 1.6))
                                        : clamp01(1.0f - aodProg);
                            }

                            syncLightBaseline(bl, shadowArr, edgeArr);
                            if ("MP_MiniCard".equals(name)) {
                                bl.baselineDirty = false;
                                if (!isLockscreenCard || !cfg.isFcEnabled() || aodLightScale >= 0.999f) {
                                    if (bl.boosted) {
                                        restoreLightBaseline(params, shadowArr, edgeArr, bl);
                                        bl.boosted = false;
                                    }
                                    return;
                                }
                                if (uShadow > 0.5f && shadowArr != null && shadowArr.length >= 14 && bl.baseShadow != null) {
                                    shadowArr[0] = clamp01(bl.baseShadow[0] * aodLightScale);
                                    shadowArr[1] = clamp01(bl.baseShadow[1] * aodLightScale);
                                    setParamDirtyFlag(params, true, true);
                                    bl.lastShadow = copyArrayInPlace(shadowArr, bl.lastShadow);
                                }
                                if (uEdge > 0.5f && edgeArr != null && edgeArr.length >= 12 && bl.baseEdge != null) {
                                    edgeArr[10] = clamp01(bl.baseEdge[10] * aodLightScale);
                                    edgeArr[11] = clamp01(bl.baseEdge[11] * aodLightScale);
                                    setParamDirtyFlag(params, false, true);
                                    bl.lastEdge = copyArrayInPlace(edgeArr, bl.lastEdge);
                                }
                                bl.boosted = true;
                                return;
                            }

                            boolean isMediaLight = isMediaLightName(name);
                            boolean wasBaselineDirty = bl.baselineDirty;
                            if (isMediaLight) {
                                if (!bl.registeredMediaLight) {
                                    ACTIVE_MEDIA_LIGHT_DRAWABLES.put(lightDr, Boolean.TRUE);
                                    bl.registeredMediaLight = true;
                                }
                                Integer covObj = LIGHT_COVER_COLOR.get(lightDr);
                                int covArgb = covObj != null ? covObj : 0;
                                int rawRgb = (bl.baselineDirty || bl.lastRecordedRawRgb == 0)
                                        ? extractRawMediaRgb(bl) : bl.lastRecordedRawRgb;
                                bl.baselineDirty = false;
                                if (covArgb != 0) {
                                    int covRaw = toRawMediaRgb(covArgb);
                                    if (covRaw != 0) {
                                        rawRgb = covRaw;
                                    }
                                }
                                int liveArgb = covArgb != 0 ? covArgb : (rawRgb != 0 ? (0xFF000000 | rawRgb) : 0);
                                bl.lastRecordedRawRgb = rawRgb;
                                if ((liveArgb != 0 || rawRgb != 0)
                                        && (rawRgb != sLastMediaRawRgb
                                                || (liveArgb != 0 && liveArgb != sLatestMediaCoverColor))
                                        && canMediaSurfaceUpdateGlobalColor(hostView, name, bl, false)) {
                                    promotePrimaryMediaColor(lightDr, hostView, liveArgb, rawRgb, cfg);
                                }
                            } else {
                                bl.baselineDirty = false;
                            }

                            int effH = (hostView != null && hostView.getHeight() > 0)
                                    ? hostView.getHeight()
                                    : lightDr.getBounds().height();
                            float dp = (hostView != null && hostView.getResources() != null)
                                    ? hostView.getResources().getDisplayMetrics().density
                                    : 3.0f;
                            boolean isLargeMedia = isLargeMediaCardHeight(isMediaLight, effH, dp);
                            boolean surfaceHasMedia = false;
                            if (!isMediaLight && cfg.isFcEnabled() && cfg.isFcNonMediaMaskEnabled()) {
                                if (isLockscreenCard) {
                                    surfaceHasMedia = hasActiveLockMediaFluidCloud();
                                } else if (inShade) {
                                    surfaceHasMedia = hasActiveShadeMediaFluidCloud();
                                } else {
                                    surfaceHasMedia = hasActiveTopMediaFluidCloud();
                                }
                            }
                            int mediaTintArgb = surfaceHasMedia ? resolveMediaTintArgb() : 0;
                            boolean wantNonMediaTint = !isMediaLight
                                    && surfaceHasMedia
                                    && mediaTintArgb != 0;

                            int brightPct = cfg.isFcEnabled()
                                    ? (isLargeMedia ? cfg.getFcBottomGlowInt() : cfg.getFcSingleGlowInt())
                                    : 0;
                            int chromaPct = cfg.isFcEnabled()
                                    ? (isLargeMedia ? cfg.getFcBottomGlowChromaInt() : cfg.getFcSingleGlowChromaInt())
                                    : 0;
                            if (inShade || (!wantNonMediaTint && brightPct <= 0 && chromaPct <= 0 && aodLightScale >= 0.999f)) {
                                if (bl.boosted) {
                                    restoreLightBaseline(params, shadowArr, edgeArr, bl);
                                    bl.boosted = false;
                                }
                                bl.lastAppliedKey = 0;
                                return;
                            }

                            int appliedKey = cfg.getFcVersion();
                            appliedKey = appliedKey * 31 + sMediaColorVersion;
                            appliedKey = appliedKey * 31 + Math.round(aodLightScale * 255f);
                            appliedKey = appliedKey * 31 + (wantNonMediaTint ? 1 : 0);
                            appliedKey = appliedKey * 31 + (isLargeMedia ? 1 : 0);
                            appliedKey = appliedKey * 31 + (uShadow > 0.5f ? 2 : 0) + (uEdge > 0.5f ? 1 : 0);
                            if (!wasBaselineDirty && bl.boosted && bl.lastAppliedKey == appliedKey) {
                                return;
                            }

                            float brightBoost = clamp01(brightPct / (float) ConfigStore.MAX_FC_BOTTOM_GLOW);
                            float chromaBoost = clamp01(chromaPct / (float) ConfigStore.MAX_FC_BOTTOM_GLOW_CHROMA);
                            float[] rgb = TL_LIGHT_RGB.get();
                            if (rgb == null) {
                                rgb = new float[3];
                                TL_LIGHT_RGB.set(rgb);
                            }
                            if (uShadow > 0.5f && shadowArr != null && shadowArr.length >= 14 && bl.baseShadow != null) {
                                if (wantNonMediaTint) {
                                    boostHighlightChromaArgb(mediaTintArgb, chromaBoost, brightBoost, rgb);
                                    shadowArr[2] = rgb[0];
                                    shadowArr[3] = rgb[1];
                                    shadowArr[4] = rgb[2];
                                    shadowArr[11] = rgb[0];
                                    shadowArr[12] = rgb[1];
                                    shadowArr[13] = rgb[2];
                                } else {
                                    boostHighlightChroma(bl.baseShadow[2], bl.baseShadow[3], bl.baseShadow[4],
                                            chromaBoost, brightBoost, rgb);
                                    shadowArr[2] = rgb[0];
                                    shadowArr[3] = rgb[1];
                                    shadowArr[4] = rgb[2];
                                    boostHighlightChroma(bl.baseShadow[11], bl.baseShadow[12], bl.baseShadow[13],
                                            chromaBoost, brightBoost, rgb);
                                    shadowArr[11] = rgb[0];
                                    shadowArr[12] = rgb[1];
                                    shadowArr[13] = rgb[2];
                                }
                                if (brightBoost > 0.001f && bl.baseShadow[0] > 0.001f) {
                                    shadowArr[0] = clamp01((bl.baseShadow[0]
                                            + (0.85f - bl.baseShadow[0]) * (float) Math.pow(brightBoost, 0.80))
                                            * aodLightScale);
                                } else {
                                    shadowArr[0] = clamp01(bl.baseShadow[0] * aodLightScale);
                                }
                                shadowArr[1] = clamp01(bl.baseShadow[1] * aodLightScale);
                                setParamDirtyFlag(params, true, true);
                                bl.lastShadow = copyArrayInPlace(shadowArr, bl.lastShadow);
                            }
                            if (uEdge > 0.5f && edgeArr != null && edgeArr.length >= 12 && bl.baseEdge != null) {
                                if (wantNonMediaTint) {
                                    boostHighlightChromaArgb(mediaTintArgb, chromaBoost, brightBoost, rgb);
                                    edgeArr[0] = rgb[0];
                                    edgeArr[1] = rgb[1];
                                    edgeArr[2] = rgb[2];
                                    edgeArr[3] = rgb[0];
                                    edgeArr[4] = rgb[1];
                                    edgeArr[5] = rgb[2];
                                } else {
                                    boostHighlightChroma(bl.baseEdge[0], bl.baseEdge[1], bl.baseEdge[2],
                                            chromaBoost, brightBoost, rgb);
                                    edgeArr[0] = rgb[0];
                                    edgeArr[1] = rgb[1];
                                    edgeArr[2] = rgb[2];
                                    boostHighlightChroma(bl.baseEdge[3], bl.baseEdge[4], bl.baseEdge[5],
                                            chromaBoost, brightBoost, rgb);
                                    edgeArr[3] = rgb[0];
                                    edgeArr[4] = rgb[1];
                                    edgeArr[5] = rgb[2];
                                }
                                if (brightBoost > 0.001f && bl.baseEdge[10] > 0.001f) {
                                    edgeArr[10] = clamp01((bl.baseEdge[10]
                                            + (1.00f - bl.baseEdge[10]) * (float) Math.pow(brightBoost, 0.75))
                                            * aodLightScale);
                                } else {
                                    edgeArr[10] = clamp01(bl.baseEdge[10] * aodLightScale);
                                }
                                edgeArr[11] = clamp01(bl.baseEdge[11] * aodLightScale);
                                edgeArr[7] = bl.baseEdge[7] * (1.0f + 0.80f * brightBoost);
                                setParamDirtyFlag(params, false, true);
                                bl.lastEdge = copyArrayInPlace(edgeArr, bl.lastEdge);
                            }
                            bl.boosted = true;
                            bl.lastAppliedKey = appliedKey;
                        } catch (Throwable ignored) {
                        }
    }

    private static void registerMediaViewAttachListener(View v) {
        if (v == null) {
            return;
        }
        if (!Boolean.TRUE.equals(TAGGED_MEDIA_ATTACH_VIEWS.put(v, Boolean.TRUE))) {
            try {
                v.addOnAttachStateChangeListener(MEDIA_VIEW_ATTACH_LISTENER);
            } catch (Throwable ignored) {
            }
        }
    }

    static void onHostMediaViewDetached(ConfigStore.HookConfig cfg) {
        if (cfg != null) {
            sLastHookConfig = cfg;
        }
        sLastMediaColorPromotedAtMs = 0L;
        scheduleMediaColorReconcile(cfg != null ? cfg : sLastHookConfig);
    }

    static void scheduleMediaColorReconcile(ConfigStore.HookConfig cfg) {
        if (cfg != null) {
            sLastHookConfig = cfg;
        }
        MAIN_HANDLER.removeCallbacks(MEDIA_RECONCILE_TASK);
        MAIN_HANDLER.postDelayed(MEDIA_RECONCILE_TASK, 16L);
        MAIN_HANDLER.postDelayed(MEDIA_RECONCILE_TASK, 180L);
    }

    private static boolean isAnyMediaSurfaceActive() {
        long now = SystemClock.uptimeMillis();
        if (now - sLastMediaColorPromotedAtMs < 600L
                && (!ACTIVE_MEDIA_SECTIONS.isEmpty() || !ACTIVE_MINI_SECTIONS.isEmpty())) {
            return true;
        }
        List<Object> sections;
        synchronized (ACTIVE_MEDIA_SECTIONS) {
            sections = ACTIVE_MEDIA_SECTIONS.isEmpty()
                    ? Collections.emptyList()
                    : new ArrayList<>(ACTIVE_MEDIA_SECTIONS.keySet());
        }
        for (int i = 0, n = sections.size(); i < n; i++) {
            Object sec = sections.get(i);
            if (sec == null) {
                continue;
            }
            View mediaBg = readViewField(sec, "s");
            if (mediaBg != null && mediaBg.isAttachedToWindow()) {
                return true;
            }
        }
        List<Object> minis;
        synchronized (ACTIVE_MINI_SECTIONS) {
            minis = ACTIVE_MINI_SECTIONS.isEmpty()
                    ? Collections.emptyList()
                    : new ArrayList<>(ACTIVE_MINI_SECTIONS.keySet());
        }
        for (int i = 0, n = minis.size(); i < n; i++) {
            Object m0 = minis.get(i);
            if (m0 == null) {
                continue;
            }
            View c = readViewField(m0, "c");
            if (c != null && c.isAttachedToWindow()) {
                return true;
            }
        }
        List<Object> carousels;
        synchronized (ACTIVE_MEDIA_CAROUSELS) {
            carousels = ACTIVE_MEDIA_CAROUSELS.isEmpty()
                    ? Collections.emptyList()
                    : new ArrayList<>(ACTIVE_MEDIA_CAROUSELS.keySet());
        }
        for (int i = 0, n = carousels.size(); i < n; i++) {
            Object j1 = carousels.get(i);
            if (j1 == null) {
                continue;
            }
            View vp = readViewField(j1, "u");
            Object adapter = readObjectField(j1, "v");
            Object countObj = invokeNoArg(adapter, "getItemCount");
            if (vp != null && vp.isAttachedToWindow()
                    && countObj instanceof Integer && ((Integer) countObj) > 0) {
                return true;
            }
        }
        return FluidCloudHook.hasActiveMediaCustomRow();
    }

    private static void ensureMediaStillActive() {
        if ((sLatestMediaCoverColor == 0 && sLastMediaRawRgb == 0) || !sEverTrackedMediaSurface) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (now - sLastActiveCheckMs < 150L) {
            return;
        }
        sLastActiveCheckMs = now;
        if (!isAnyMediaSurfaceActive()) {
            CARD_MEDIA_RAW_RGB.clear();
            boolean hadColor = (sLatestMediaCoverColor != 0 || sLastMediaRawRgb != 0);
            sLatestMediaCoverColor = 0;
            sLastMediaRawRgb = 0;
            if (hadColor) {
                sMediaColorVersion++;
                ConfigStore.HookConfig cfg = sLastHookConfig;
                if (cfg != null) {
                    notifyMediaColorChanged(cfg);
                }
            }
        }
    }

    private static void reconcileActiveMediaColor(ConfigStore.HookConfig cfg) {
        ConfigStore.HookConfig effCfg = cfg != null ? cfg : sLastHookConfig;
        sLastActiveCheckMs = SystemClock.uptimeMillis();

        // 1. Check active MediaPlayerSection (x1) instances attached to a window
        List<Object> sections;
        synchronized (ACTIVE_MEDIA_SECTIONS) {
            sections = ACTIVE_MEDIA_SECTIONS.isEmpty()
                    ? Collections.emptyList()
                    : new ArrayList<>(ACTIVE_MEDIA_SECTIONS.keySet());
        }
        Object fallbackSec = null;
        View fallbackBg = null;
        Drawable fallbackLight = null;
        int fallbackArgb = 0;
        int fallbackRaw = 0;

        for (int i = 0, n = sections.size(); i < n; i++) {
            Object sec = sections.get(i);
            if (sec == null) {
                continue;
            }
            View mediaBg = readViewField(sec, "s");
            if (mediaBg == null || !mediaBg.isAttachedToWindow()) {
                continue;
            }
            Object lightObj = readObjectField(mediaBg, "F");
            Drawable lightDr = lightObj instanceof Drawable ? (Drawable) lightObj : null;
            LightBaseline bl = lightDr != null ? LIGHT_BASELINES.get(lightDr) : null;
            if (!isHostOnSelectedCarouselPage(mediaBg, bl)) {
                continue;
            }
            int[] colors = new int[2];
            extractMediaColorFromSection(sec, lightDr, colors);
            int argb = colors[0];
            int raw = colors[1] != 0 ? colors[1] : toRawMediaRgb(argb);
            if (argb == 0 && raw == 0) {
                continue;
            }
            String name = resolveMediaSectionName(sec, mediaBg, lightDr);
            if (mediaBg.isShown() && canMediaSurfaceUpdateGlobalColor(mediaBg, name, bl, false)) {
                promotePrimaryMediaColor(lightDr, mediaBg, argb, raw, effCfg);
                return;
            }
            if (fallbackSec == null) {
                fallbackSec = sec;
                fallbackBg = mediaBg;
                fallbackLight = lightDr;
                fallbackArgb = argb;
                fallbackRaw = raw;
            }
        }

        // 2. Check active MediaMiniPlayerSection (M0) instances attached to a window
        List<Object> minis;
        synchronized (ACTIVE_MINI_SECTIONS) {
            minis = ACTIVE_MINI_SECTIONS.isEmpty()
                    ? Collections.emptyList()
                    : new ArrayList<>(ACTIVE_MINI_SECTIONS.keySet());
        }
        for (int i = 0, n = minis.size(); i < n; i++) {
            Object m0 = minis.get(i);
            if (m0 == null) {
                continue;
            }
            View c = readViewField(m0, "c");
            if (c == null || !c.isAttachedToWindow()) {
                continue;
            }
            int argb = readArtColorsArgb(readObjectField(m0, "b"), "l");
            if (argb != 0) {
                promotePrimaryMediaColor(null, c, argb, toRawMediaRgb(argb), effCfg);
                return;
            }
        }

        // 3. Fallback to any attached selected x1 section (e.g. shade collapsed while music active)
        if (fallbackSec != null) {
            promotePrimaryMediaColor(fallbackLight, fallbackBg, fallbackArgb, fallbackRaw, effCfg);
            return;
        }

        // 4. If no active media surface remains, reset media colors to 0 and restore default non-media / rim colors
        if (!isAnyMediaSurfaceActive()) {
            CARD_MEDIA_RAW_RGB.clear();
            boolean hadColor = (sLatestMediaCoverColor != 0 || sLastMediaRawRgb != 0);
            sLatestMediaCoverColor = 0;
            sLastMediaRawRgb = 0;
            if (hadColor) {
                sMediaColorVersion++;
                if (effCfg != null) {
                    notifyMediaColorChanged(effCfg);
                }
            }
        }
    }

    /**
     * Hooks MediaPlayerMultiCardSection (j1) page selection and teardown so active media color
     * immediately tracks swipes across multiple players in ViewPager2 carousels.
     */
    private static boolean hookMediaMultiCardSection(ClassLoader pluginCl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> multiCls = XposedCompat.findClassIfExists(CLS_MEDIA_MULTI_CARD_SECTION, pluginCl);
            if (multiCls == null) {
                return false;
            }
            Method mSync = findMethod(multiCls, "w");
            if (mSync == null) {
                return false;
            }
            XposedCompat.hookMethod(mSync, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.thisObject != null) {
                        sEverTrackedMediaSurface = true;
                        sLastHookConfig = cfg;
                        ACTIVE_MEDIA_CAROUSELS.put(param.thisObject, Boolean.TRUE);
                        syncActivePageColorFromCarousel(param.thisObject, cfg, true, 0);
                    }
                }
            });
            Method mBind = findMethod(multiCls, "b");
            if (mBind != null) {
                XposedCompat.hookMethod(mBind, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.thisObject != null) {
                            sEverTrackedMediaSurface = true;
                            sLastHookConfig = cfg;
                            ACTIVE_MEDIA_CAROUSELS.put(param.thisObject, Boolean.TRUE);
                        }
                    }
                });
            }
            Method mDispose = findMethod(multiCls, "dispose");
            if (mDispose != null) {
                XposedCompat.hookMethod(mDispose, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (param.thisObject != null) {
                            ACTIVE_MEDIA_CAROUSELS.remove(param.thisObject);
                        }
                        sLastMediaColorPromotedAtMs = 0L;
                        scheduleMediaColorReconcile(cfg);
                    }
                });
            }
            Class<?> adapterCls = XposedCompat.findClassIfExists(CLS_MEDIA_MULTI_ADAPTER, pluginCl);
            if (adapterCls != null) {
                for (Method m : adapterCls.getDeclaredMethods()) {
                    if ("b".equals(m.getName()) && m.getParameterTypes().length == 2) {
                        XposedCompat.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                if (param.args.length > 0 && (param.args[0] == null
                                        || (param.args[0] instanceof List && ((List<?>) param.args[0]).isEmpty()))) {
                                    sLastMediaColorPromotedAtMs = 0L;
                                }
                                scheduleMediaColorReconcile(cfg);
                            }
                        });
                        break;
                    }
                }
            }
            return true;
        } catch (Throwable t) {
            cfg.note("DynamicIsland: MediaMultiCardSection hook failed: " + t);
            return false;
        }
    }

    /**
     * Hooks MediaPlayerSection (x1) color sync and lifecycle so album art colors are still
     * captured from J0.O (ArtColors) even when system Flowing Contour / Highlight is disabled.
     */
    private static boolean hookMediaPlayerSection(ClassLoader pluginCl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> secCls = XposedCompat.findClassIfExists(CLS_MEDIA_PLAYER_SECTION, pluginCl);
            if (secCls == null) {
                return false;
            }
            boolean hooked = false;
            Method mSyncLight = findMethod(secCls, "s", secCls);
            if (mSyncLight != null) {
                XposedCompat.hookMethod(mSyncLight, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.args.length > 0 && param.args[0] != null) {
                            syncMediaSectionColor(param.args[0], cfg, false);
                        }
                    }
                });
                hooked = true;
            }
            Method mSeekColor = findMethod(secCls, "O");
            if (mSeekColor != null) {
                XposedCompat.hookMethod(mSeekColor, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.thisObject != null) {
                            syncMediaSectionColor(param.thisObject, cfg, false);
                        }
                    }
                });
                hooked = true;
            }
            Method mBind = findMethod(secCls, "b");
            if (mBind != null) {
                XposedCompat.hookMethod(mBind, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.thisObject != null) {
                            syncMediaSectionColor(param.thisObject, cfg, false);
                        }
                    }
                });
                hooked = true;
            }
            Method mDispose = findMethod(secCls, "dispose");
            if (mDispose != null) {
                XposedCompat.hookMethod(mDispose, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (param.thisObject != null) {
                            ACTIVE_MEDIA_SECTIONS.remove(param.thisObject);
                        }
                        View mediaBg = readViewField(param.thisObject, "s");
                        if (mediaBg != null) {
                            ACTIVE_MEDIA_HOSTS.remove(mediaBg);
                            MEDIA_BG_TO_SECTION.remove(mediaBg);
                            Object lightObj = readObjectField(mediaBg, "F");
                            if (lightObj != null) {
                                ACTIVE_MEDIA_LIGHT_DRAWABLES.remove(lightObj);
                                LIGHT_COVER_COLOR.remove(lightObj);
                            }
                            View cardRoot = resolveEnclosingCardRoot(mediaBg);
                            if (cardRoot != null) {
                                CARD_MEDIA_RAW_RGB.remove(cardRoot);
                            }
                        }
                        sLastMediaColorPromotedAtMs = 0L;
                        scheduleMediaColorReconcile(cfg);
                    }
                });
                hooked = true;
            }
            return hooked;
        } catch (Throwable t) {
            cfg.note("DynamicIsland: MediaPlayerSection hook failed: " + t);
            return false;
        }
    }

    /**
     * Hooks MediaMiniPlayerSection (M0) as a fallback color source when no full media card
     * surface is currently visible and selected.
     */
    private static boolean hookMediaMiniSection(ClassLoader pluginCl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> miniCls = XposedCompat.findClassIfExists(CLS_MEDIA_MINI_SECTION, pluginCl);
            if (miniCls == null) {
                return false;
            }
            XC_MethodHook miniActiveHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.thisObject == null) {
                        return;
                    }
                    sEverTrackedMediaSurface = true;
                    sLastHookConfig = cfg;
                    ACTIVE_MINI_SECTIONS.put(param.thisObject, Boolean.TRUE);
                    View container = readViewField(param.thisObject, "c");
                    registerMediaViewAttachListener(container);
                    if (hasVisibleSelectedMediaSurface(null, null)) {
                        return;
                    }
                    int argb = readArtColorsArgb(readObjectField(param.thisObject, "b"), "l");
                    if (argb != 0) {
                        promotePrimaryMediaColor(null, container, argb, toRawMediaRgb(argb), cfg);
                    }
                }
            };
            Method mApply = findMethod(miniCls, "m");
            if (mApply == null) {
                return false;
            }
            XposedCompat.hookMethod(mApply, miniActiveHook);
            Method mBind = findMethod(miniCls, "b");
            if (mBind != null) {
                XposedCompat.hookMethod(mBind, miniActiveHook);
            }
            Method mDispose = findMethod(miniCls, "dispose");
            if (mDispose != null) {
                XposedCompat.hookMethod(mDispose, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (param.thisObject != null) {
                            ACTIVE_MINI_SECTIONS.remove(param.thisObject);
                            Object lightObj = readObjectField(param.thisObject, "p");
                            if (lightObj != null) {
                                LIGHT_COVER_COLOR.remove(lightObj);
                                ACTIVE_LOCK_LIGHT_DRAWABLES.remove(lightObj);
                            }
                        }
                        sLastMediaColorPromotedAtMs = 0L;
                        scheduleMediaColorReconcile(cfg);
                    }
                });
            }
            return true;
        } catch (Throwable t) {
            cfg.note("DynamicIsland: MediaMiniSection hook failed: " + t);
            return false;
        }
    }

    private static String resolveMediaSectionName(Object section, View mediaBg, Drawable lightDr) {
        if (lightDr != null) {
            Object nameObj = readObjectField(lightDr, "b");
            if (nameObj != null) {
                String s = String.valueOf(nameObj);
                if ("MP_Notification".equals(s) || "MP_StatusBar".equals(s) || "MP_MiniCard".equals(s)) {
                    return s;
                }
            }
        }
        if (section != null) {
            Object vm = readObjectField(section, "l");
            Object vHolder = readObjectField(vm, "V");
            Object vFlow = readObjectField(vHolder, "b");
            Object vVal = invokeNoArg(vFlow, "getValue");
            if (vVal instanceof Boolean) {
                return ((Boolean) vVal) ? "MP_Notification" : "MP_StatusBar";
            }
        }
        return isViewInsideCustomRow(mediaBg) ? "MP_Notification" : "MP_StatusBar";
    }

    private static void syncMediaSectionColor(Object section,
                                              ConfigStore.HookConfig cfg,
                                              boolean fromUserPageSelect) {
        if (section == null || cfg == null) {
            return;
        }
        View mediaBg = readViewField(section, "s");
        if (mediaBg == null) {
            return;
        }
        Object lightObj = readObjectField(mediaBg, "F");
        Drawable lightDr = lightObj instanceof Drawable ? (Drawable) lightObj : null;
        String name = resolveMediaSectionName(section, mediaBg, lightDr);
        if ("MP_MiniCard".equals(name)) {
            return;
        }
        sEverTrackedMediaSurface = true;
        sLastHookConfig = cfg;
        ACTIVE_MEDIA_SECTIONS.put(section, Boolean.TRUE);
        ACTIVE_MEDIA_HOSTS.put(mediaBg, name);
        MEDIA_BG_TO_SECTION.put(mediaBg, section);
        registerMediaViewAttachListener(mediaBg);
        if (lightDr != null) {
            ACTIVE_MEDIA_LIGHT_DRAWABLES.put(lightDr, Boolean.TRUE);
        }
        int[] colors = new int[2];
        extractMediaColorFromSection(section, lightDr, colors);
        if (colors[0] == 0 && colors[1] == 0) {
            return;
        }
        int effRaw = colors[1] != 0 ? colors[1] : toRawMediaRgb(colors[0]);
        LightBaseline bl = lightDr != null ? LIGHT_BASELINES.get(lightDr) : null;
        if (canMediaSurfaceUpdateGlobalColor(mediaBg, name, bl, fromUserPageSelect)) {
            promotePrimaryMediaColor(lightDr, mediaBg, colors[0], effRaw, cfg);
        } else {
            updateCardMediaRawRgb(resolveEnclosingCardRoot(mediaBg), effRaw, cfg);
        }
    }

    private static boolean hookTransitionAnimatorFragments(ClassLoader pluginCl, final ConfigStore.HookConfig cfg) {
        boolean hooked = false;
        try {
            Class<?> fragCls = XposedCompat.findClassIfExists(CLS_ANIM_FRAGMENT, pluginCl);
            if (fragCls != null) {
                Constructor<?> ctor = null;
                for (Constructor<?> c : fragCls.getDeclaredConstructors()) {
                    Class<?>[] pts = c.getParameterTypes();
                    if (pts.length == 4
                            && pts[0] == Drawable.class
                            && pts[1] == float.class
                            && pts[2] == float.class
                            && pts[3] == long.class) {
                        ctor = c;
                        break;
                    }
                }
                if (ctor != null) {
                    XposedCompat.hookMethod(ctor, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args.length >= 4
                                    && param.args[1] instanceof Float
                                    && param.args[2] instanceof Float
                                    && param.args[3] instanceof Long) {
                                float startA = (Float) param.args[1];
                                float endA = (Float) param.args[2];
                                long origDur = (Long) param.args[3];
                                if (startA > endA) {
                                    if (cfg.isFcEnabled()) {
                                        if (startA >= 0.95f && endA <= 0.05f) {
                                            param.args[3] = 300L;
                                        } else {
                                            param.args[3] = Math.max(origDur, 240L);
                                        }
                                    }
                                } else if (startA < endA) {
                                    if (cfg.isFcEnabled()) {
                                        if (startA <= 0.05f && endA >= 0.95f) {
                                            param.args[3] = 360L;
                                        } else {
                                            param.args[3] = Math.max(origDur, 240L);
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!cfg.isFcEnabled() || param.thisObject == null
                                    || param.args.length < 3
                                    || !(param.args[1] instanceof Float)
                                    || !(param.args[2] instanceof Float)) {
                                return;
                            }
                            float startA = (Float) param.args[1];
                            float endA = (Float) param.args[2];
                            if (startA == endA) {
                                return;
                            }
                            try {
                                Field fInterp = param.thisObject.getClass().getDeclaredField("i");
                                if (PathInterpolator.class.isAssignableFrom(fInterp.getType())) {
                                    fInterp.setAccessible(true);
                                    PathInterpolator interp = (startA > endA)
                                            ? new PathInterpolator(0.15f, 0.0f, 0.45f, 1.0f)
                                            : new PathInterpolator(0.55f, 0.0f, 0.85f, 1.0f);
                                    fInterp.set(param.thisObject, interp);
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    });
                    hooked = true;
                }

                Method mBuildAnim = null;
                for (Method m : fragCls.getDeclaredMethods()) {
                    if ("g".equals(m.getName()) && m.getParameterTypes().length == 1) {
                        mBuildAnim = m;
                        break;
                    }
                }
                if (mBuildAnim != null) {
                    XposedCompat.hookMethod(mBuildAnim, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (param.thisObject == null || !cfg.isFcEnabled()) {
                                return;
                            }
                            Object kindObj = readObjectField(param.thisObject, "d");
                            if (!(kindObj instanceof Integer) || ((Integer) kindObj) != 4) {
                                return;
                            }
                            Object drawableObj = readObjectField(param.thisObject, "e");
                            Object res = param.getResult();
                            if (!(drawableObj instanceof ColorDrawable) || !(res instanceof ValueAnimator)) {
                                return;
                            }
                            final ColorDrawable cd = (ColorDrawable) drawableObj;
                            final float startA = readFloatField(param.thisObject, "f", 0.0f);
                            final float endA = readFloatField(param.thisObject, "g", 0.0f);
                            ValueAnimator va = (ValueAnimator) res;
                            va.removeAllUpdateListeners();
                            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    float frac = animation.getAnimatedFraction();
                                    float t = clamp01((1.0f - frac) * startA + frac * endA);
                                    if (!cfg.isFcEnabled()) {
                                        cd.setColor(0xFF000000);
                                        cd.setAlpha(Math.max(0, Math.min(255, Math.round(255.0f * t))));
                                        return;
                                    }
                                    float alphaCurve = (float) Math.pow(t, 1.25);
                                    float colorMix = (float) Math.pow(1.0f - t, 1.15);
                                    int outAlpha = Math.max(0, Math.min(255, Math.round(255.0f * alphaCurve)));
                                    boolean useCustom = cfg.isFcColorEnabled()
                                            && !shouldSuppressTopCustomColor(resolveDrawableHostView(cd), cfg);
                                    int fcColor = useCustom ? cfg.getFcColor() : 0;
                                    float colorAlpha = useCustom ? ConfigStore.colorAlphaScale(fcColor) : 0.0f;
                                    int baseRgb = 0x1E2026;
                                    int targetRgb;
                                    if (useCustom && colorAlpha > 0.001f) {
                                        int tr = Math.round(((baseRgb >> 16) & 0xFF)
                                                + (((fcColor >> 16) & 0xFF) - ((baseRgb >> 16) & 0xFF)) * colorAlpha);
                                        int tg = Math.round(((baseRgb >> 8) & 0xFF)
                                                + (((fcColor >> 8) & 0xFF) - ((baseRgb >> 8) & 0xFF)) * colorAlpha);
                                        int tb = Math.round((baseRgb & 0xFF)
                                                + ((fcColor & 0xFF) - (baseRgb & 0xFF)) * colorAlpha);
                                        targetRgb = (tr << 16) | (tg << 8) | tb;
                                    } else {
                                        targetRgb = baseRgb;
                                    }
                                    int r = Math.round(((targetRgb >> 16) & 0xFF) * colorMix);
                                    int g = Math.round(((targetRgb >> 8) & 0xFF) * colorMix);
                                    int b = Math.round((targetRgb & 0xFF) * colorMix);
                                    cd.setColor(0xFF000000 | (r << 16) | (g << 8) | b);
                                    cd.setAlpha(outAlpha);
                                }
                            });
                        }
                    });
                    hooked = true;
                }
            }
        } catch (Throwable t) {
            cfg.note("DynamicIsland: DrawableAlphaFragment hook failed: " + t);
        }

        try {
            Class<?> helperCls = XposedCompat.findClassIfExists(CLS_SCENE_BUILDER_HELPER, pluginCl);
            if (helperCls != null) {
                XposedCompat.hookAllMethods(helperCls, "P", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!cfg.isFcEnabled() || param.args.length < 2
                                || param.args[0] == null || !(param.args[1] instanceof Long)) {
                            return;
                        }
                        if (!CLS_ANIM_FRAGMENT.equals(param.args[0].getClass().getName())) {
                            return;
                        }
                        Object kind = readObjectField(param.args[0], "d");
                        if (kind instanceof Integer && ((Integer) kind) == 4 && ((Long) param.args[1]) > 0L) {
                            param.args[1] = 0L;
                        }
                    }
                });
                hooked = true;
            }
        } catch (Throwable t) {
            cfg.note("DynamicIsland: SceneBuilder delay hook failed: " + t);
        }
        return hooked;
    }

    private static int readSeedlingBaseBlurRadius(ClassLoader pluginCl) {
        int cached = sSeedlingBaseBlurRadius;
        if (cached > 0) {
            return cached;
        }
        int base = 540;
        ClassLoader cl = pluginCl != null ? pluginCl : sLastPluginCl;
        if (cl != null) {
            try {
                Class<?> dCls = XposedCompat.findClassIfExists("com.oplus.systemui.plugins.seedling.util.d", cl);
                if (dCls != null) {
                    Field f = dCls.getDeclaredField("a");
                    f.setAccessible(true);
                    Object lazy = f.get(null);
                    if (lazy != null) {
                        Object val = invokeNoArg(lazy, "getValue");
                        if (Boolean.TRUE.equals(val)) {
                            base = 100;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        sSeedlingBaseBlurRadius = base;
        return base;
    }

    private static void invokeVrmSetBlurParams(Object vrm, Object blurParam) {
        if (vrm == null || blurParam == null) {
            return;
        }
        try {
            Method m = sVrmSetBlurParamsM;
            if (m == null || !m.getDeclaringClass().isInstance(vrm)) {
                m = findMethod(vrm.getClass(), "setBlurParams", blurParam.getClass());
                sVrmSetBlurParamsM = m;
            }
            if (m != null) {
                m.invoke(vrm, blurParam);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void invokeVrmSetBlurRadius(Object vrm, int radius) {
        if (vrm == null) {
            return;
        }
        try {
            Method m = sVrmSetBlurRadiusM;
            if (m == null || !m.getDeclaringClass().isInstance(vrm)) {
                m = findMethod(vrm.getClass(), "setBlurRadius", int.class);
                sVrmSetBlurRadiusM = m;
            }
            if (m != null) {
                m.invoke(vrm, radius);
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean hookBackgroundHelper(final ClassLoader pluginCl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> helperCls = XposedCompat.findClassIfExists(CLS_BG_HELPER, pluginCl);
            if (helperCls == null) {
                return false;
            }
            Method targetB = null;
            for (Method m : helperCls.getDeclaredMethods()) {
                if ("b".equals(m.getName()) && m.getParameterTypes().length == 3) {
                    Class<?>[] pts = m.getParameterTypes();
                    if (View.class.isAssignableFrom(pts[0])) {
                        targetB = m;
                        break;
                    }
                }
            }
            if (targetB == null) {
                return false;
            }
            XposedCompat.hookMethod(targetB, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args.length < 2) {
                        return;
                    }
                    View bgView = param.args[0] instanceof View ? (View) param.args[0] : null;
                    Object vrm = param.args[1];
                    if (bgView != null) {
                        TAGGED_ISLAND_VIEW.put(bgView, Boolean.TRUE);
                    }
                    if (vrm != null && bgView != null) {
                        VRM_TO_VIEW.put(vrm, bgView);
                        VIEW_TO_VRM.put(bgView, vrm);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args.length < 3) {
                        return;
                    }
                    View bgView = param.args[0] instanceof View ? (View) param.args[0] : null;
                    Object vrm = param.args[1];
                    View gradView = param.args[2] instanceof View ? (View) param.args[2] : null;
                    if (gradView != null) {
                        patchGradientOverlay(gradView, cfg);
                    }
                    if (bgView != null) {
                        patchViewBackgroundLayers(bgView, cfg);
                    }
                    if (vrm != null) {
                        if (LAST_BLUR_PARAM.get(vrm) == null) {
                            Object baseParam = extractBlurParamFromVrm(vrm);
                            if (baseParam != null) {
                                LAST_BLUR_PARAM.put(vrm, baseParam);
                                ensureOrigParamSaved(vrm, baseParam);
                            }
                        }
                        if (!ORIG_RADIUS.containsKey(vrm)) {
                            ORIG_RADIUS.put(vrm, readSeedlingBaseBlurRadius(pluginCl));
                        }
                        LAST_VRM_HASH.remove(vrm);
                        ensureVrmSynced(vrm, cfg);
                    }
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("DynamicIsland: BackgroundHelper hook failed: " + t);
            return false;
        }
    }

    private static long computeGradFastKey(Drawable d, ConfigStore.HookConfig cfg) {
        Rect bounds = d.getBounds();
        int bw = bounds != null ? bounds.width() : 0;
        int bh = bounds != null ? bounds.height() : 0;
        if (bw <= 0 || bh <= 0) {
            GradReflection ref = inspectGradientDrawable(d.getClass());
            if (ref != null) {
                try {
                    if (bw <= 0 && ref.widthField != null) {
                        bw = ref.widthField.getInt(d);
                    }
                    if (bh <= 0 && ref.heightField != null) {
                        bh = ref.heightField.getInt(d);
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        int cfgHash = computeConfigHash(cfg)
                ^ (sMediaColorVersion * 1315423911)
                ^ (sTopMediaFluidCloudActive ? 0x5A5A5A5A : 0);
        return (((long) cfgHash) << 32)
                | (((bw & 0xFFFFL) << 16) | (bh & 0xFFFFL));
    }

    private static boolean hookGradientOverlayDrawable(ClassLoader pluginCl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> gradCls = XposedCompat.findClassIfExists(CLS_GRAD_DRAWABLE, pluginCl);
            if (gradCls == null) {
                return false;
            }
            Method mDrawGrad = XposedCompat.findDeclaredMethod(gradCls, "draw", Canvas.class);
            if (mDrawGrad != null) {
                XposedCompat.hookDirect(mDrawGrad, new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        Object self = chain.getThisObject();
                        if (self instanceof Drawable) {
                            Drawable d = (Drawable) self;
                            long fastKey = computeGradFastKey(d, cfg);
                            Long prevFast = LAST_GRAD_FAST_KEY.get(d);
                            if (prevFast == null || prevFast != fastKey) {
                                patchGradientDrawable(d, cfg);
                            }
                        }
                        Object res = chain.proceed();
                        if (self instanceof Drawable) {
                            Object arg0 = chain.getArg(0);
                            if (arg0 instanceof Canvas) {
                                drawIslandBottomGlow((Drawable) self, (Canvas) arg0, cfg);
                            }
                        }
                        return res;
                    }
                });
            }
            Method mSize = XposedCompat.findDeclaredMethod(gradCls, "a", int.class, int.class);
            if (mSize != null) {
                XposedCompat.hookMethod(mSize, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.thisObject instanceof Drawable) {
                            Drawable d = (Drawable) param.thisObject;
                            if (!cfg.isFcEnabled() && ORIG_GRAD_STOPS.get(d) == null) {
                                return;
                            }
                            LAST_GRAD_HASH.remove(d);
                            LAST_GRAD_FAST_KEY.remove(d);
                            patchGradientDrawable(d, cfg);
                        }
                    }
                });
            }
            return true;
        } catch (Throwable t) {
            cfg.note("DynamicIsland: gradient drawable hook failed: " + t);
            return false;
        }
    }

    private static boolean hookCardBackgroundView(final ClassLoader pluginCl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> cls = XposedCompat.findClassIfExists(CLS_CARD_BG, pluginCl);
            if (cls == null) {
                return false;
            }
            XC_MethodHook bgHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!(param.thisObject instanceof View)) {
                        return;
                    }
                    View bgView = (View) param.thisObject;
                    TAGGED_ISLAND_VIEW.put(bgView, Boolean.TRUE);
                    LAST_BG_VIEW_FAST_HASH.remove(bgView);
                    syncViewAndGradient(bgView, cfg);
                }
            };
            XposedCompat.hookAllMethods(cls, "onAttachedToWindow", bgHook);
            Method mCfg = XposedCompat.findDeclaredMethod(cls, "onConfigurationChanged", Configuration.class);
            if (mCfg != null) {
                XposedCompat.hookMethod(mCfg, bgHook);
            }
            Method mFollow = XposedCompat.findDeclaredMethod(cls, "setBlurRadiusFollowAlpha", float.class);
            if (mFollow != null) {
                XposedCompat.hookDirect(mFollow, new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        Object res = chain.proceed();
                        Object self = chain.getThisObject();
                        if (self instanceof View) {
                            View bgView = (View) self;
                            Object arg0 = chain.getArg(0);
                            float alpha = arg0 instanceof Float ? (Float) arg0 : 1.0f;
                            Object vrm = resolveVrmFromView(bgView);
                            if (vrm != null) {
                                int origRad = (int) (readSeedlingBaseBlurRadius(pluginCl) * alpha);
                                ORIG_RADIUS.put(vrm, origRad);
                                if (cfg.isFcEnabled() && cfg.getFcBlurInt() != 100) {
                                    invokeVrmSetBlurRadius(vrm, Math.max(0, Math.round(origRad * cfg.getFcBlurScale())));
                                }
                            }
                            syncViewAndGradientIfChanged(bgView, cfg);
                        }
                        return res;
                    }
                });
            }
            Method mDraw = XposedCompat.findDeclaredMethod(cls, "draw", Canvas.class);
            if (mDraw != null) {
                XposedCompat.hookDirect(mDraw, new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        Object self = chain.getThisObject();
                        if (self instanceof View) {
                            syncViewAndGradientIfChanged((View) self, cfg);
                        }
                        return chain.proceed();
                    }
                });
            }
            return true;
        } catch (Throwable t) {
            cfg.note("DynamicIsland: CardBackgroundView hook failed: " + t);
            return false;
        }
    }

    private static boolean hookCardGradientOverlayView(ClassLoader pluginCl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> cls = XposedCompat.findClassIfExists(CLS_CARD_GRAD, pluginCl);
            if (cls == null) {
                return false;
            }
            XC_MethodHook gradHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.thisObject instanceof View) {
                        patchGradientOverlay((View) param.thisObject, cfg);
                    }
                }
            };
            XposedCompat.hookAllMethods(cls, "onAttachedToWindow", gradHook);
            XposedCompat.hookAllMethods(cls, "onLayout", gradHook);
            XposedCompat.hookAllMethods(cls, "onSizeChanged", gradHook);
            XposedCompat.hookAllMethods(cls, "setBackground", gradHook);
            XposedCompat.hookAllMethods(cls, "setVisibility", gradHook);
            return true;
        } catch (Throwable t) {
            cfg.note("DynamicIsland: CardGradientOverlayView hook failed: " + t);
            return false;
        }
    }

    private static boolean hookInstantContainer(ClassLoader pluginCl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> cls = XposedCompat.findClassIfExists(CLS_INSTANT, pluginCl);
            if (cls == null) {
                return false;
            }
            XC_MethodHook instantHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!(param.thisObject instanceof View)) {
                        return;
                    }
                    View v = (View) param.thisObject;
                    LAST_BG_VIEW_FAST_HASH.remove(v);
                    syncInstantContainer(v, cfg);
                }
            };
            XposedCompat.hookAllMethods(cls, "onAttachedToWindow", instantHook);
            XposedCompat.hookAllMethods(cls, "setBackground", instantHook);
            XposedCompat.hookAllMethods(cls, "u", instantHook);
            Method mDispatch = XposedCompat.findDeclaredMethod(cls, "dispatchDraw", Canvas.class);
            if (mDispatch != null) {
                XposedCompat.hookDirect(mDispatch, new XposedInterface.Hooker() {
                    @Override
                    public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        Object self = chain.getThisObject();
                        if (self instanceof View) {
                            syncInstantContainerIfChanged((View) self, cfg);
                        }
                        return chain.proceed();
                    }
                });
            }
            Method mUpdateBg = XposedCompat.findDeclaredMethod(cls, "p", View.class, Rect.class, float.class, int.class, int.class);
            if (mUpdateBg != null) {
                XposedCompat.hookMethod(mUpdateBg, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!cfg.isFcEnabled() || param.args.length < 5) {
                            return;
                        }
                        if (param.args[2] instanceof Float
                                && param.args[3] instanceof Integer
                                && param.args[4] instanceof Integer) {
                            float frac = (Float) param.args[2];
                            int sA = (Integer) param.args[3];
                            int eA = (Integer) param.args[4];
                            if (sA < eA) {
                                param.args[2] = (float) Math.pow(clamp01(frac), 0.65);
                            }
                        }
                    }
                });
            }
            return true;
        } catch (Throwable t) {
            cfg.note("DynamicIsland: InstantContainer hook failed: " + t);
            return false;
        }
    }

    private static int computeFastBgHash(ConfigStore.HookConfig cfg) {
        return computeConfigHash(cfg)
                ^ (sMediaColorVersion * 1315423911)
                ^ (sTopMediaFluidCloudActive ? 0x5A5A5A5A : 0);
    }

    private static void syncViewAndGradientIfChanged(View view, ConfigStore.HookConfig cfg) {
        int fastHash = computeFastBgHash(cfg);
        Integer prev = LAST_BG_VIEW_FAST_HASH.get(view);
        if (prev != null && prev == fastHash) {
            return;
        }
        syncViewAndGradient(view, cfg);
        LAST_BG_VIEW_FAST_HASH.put(view, fastHash);
    }

    private static void syncInstantContainerIfChanged(View container, ConfigStore.HookConfig cfg) {
        int fastHash = computeFastBgHash(cfg);
        Integer prev = LAST_BG_VIEW_FAST_HASH.get(container);
        if (prev != null && prev == fastHash) {
            return;
        }
        syncInstantContainer(container, cfg);
        LAST_BG_VIEW_FAST_HASH.put(container, fastHash);
    }

    private static void syncViewAndGradient(View view, ConfigStore.HookConfig cfg) {
        Object vrm = resolveVrmFromView(view);
        if (vrm != null) {
            ensureVrmSynced(vrm, cfg);
        }
        patchViewBackgroundLayers(view, cfg);
        reapplySiblingGradient(view, cfg);
        syncCardMultiLightRefresh(view, cfg);
        LAST_BG_VIEW_FAST_HASH.put(view, computeFastBgHash(cfg));
    }

    private static void syncInstantContainer(View container, ConfigStore.HookConfig cfg) {
        Object vrm = findVrmField(container);
        if (vrm != null) {
            VRM_TO_VIEW.put(vrm, container);
            VIEW_TO_VRM.put(container, vrm);
            ensureVrmSynced(vrm, cfg);
        }
        View instantView = readViewField(container, "g");
        if (instantView != null) {
            TAGGED_ISLAND_VIEW.put(instantView, Boolean.TRUE);
            patchViewBackgroundLayers(instantView, cfg);
        }
        patchViewBackgroundLayers(container, cfg);
        if (container instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) container;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                patchViewBackgroundLayers(child, cfg);
            }
        }
        syncCardMultiLightRefresh(container, cfg);
        LAST_BG_VIEW_FAST_HASH.put(container, computeFastBgHash(cfg));
    }

    private static void syncCardMultiLightRefresh(View view, ConfigStore.HookConfig cfg) {
        View cardRoot = resolveEnclosingCardRoot(view);
        if (cardRoot == null) {
            cardRoot = view;
        }
        if (cardRoot == null) {
            return;
        }
        int cfgHash = computeConfigHash(cfg);
        Integer prev = LAST_CARD_LIGHT_HASH.get(cardRoot);
        if (prev == null || prev != cfgHash) {
            LAST_CARD_LIGHT_HASH.put(cardRoot, cfgHash);
            invalidateMultiLightViewsInTree(cardRoot);
        }
    }

    private static boolean shouldSuppressTopCustomColor(View vrmHost, ConfigStore.HookConfig cfg) {
        if (vrmHost != null) {
            View cardRoot = resolveEnclosingCardRoot(vrmHost);
            View scopeRoot = cardRoot != null ? cardRoot : vrmHost;
            if (scopeRoot != null && isViewContainingMedia(scopeRoot)) {
                return true;
            }
            return cfg.isFcNonMediaMaskEnabled() && hasActiveTopMediaFluidCloud();
        }
        return hasActiveTopMediaFluidCloud();
    }

    private static void ensureVrmSynced(Object vrm, ConfigStore.HookConfig cfg) {
        if (vrm == null || Boolean.TRUE.equals(IN_REAPPLY.get())) {
            return;
        }
        boolean suppressCustom = shouldSuppressTopCustomColor(VRM_TO_VIEW.get(vrm), cfg);
        int desiredHash = computeConfigHash(cfg) * 31 + (suppressCustom ? 1 : 0);
        Integer prevHash = LAST_VRM_HASH.get(vrm);
        if (prevHash != null && prevHash == desiredHash) {
            return;
        }
        syncVrmNow(vrm, cfg, desiredHash);
    }

    private static void syncVrmNow(Object vrm, ConfigStore.HookConfig cfg, int desiredHash) {
        if (vrm == null || Boolean.TRUE.equals(IN_REAPPLY.get())) {
            return;
        }
        IN_REAPPLY.set(Boolean.TRUE);
        try {
            Object baseParam = LAST_BLUR_PARAM.get(vrm);
            if (baseParam == null) {
                baseParam = extractBlurParamFromVrm(vrm);
                if (baseParam != null) {
                    LAST_BLUR_PARAM.put(vrm, baseParam);
                    ensureOrigParamSaved(vrm, baseParam);
                }
            }

            if (baseParam != null) {
                // Must pass a distinct OplusBlurParam copy so BackgroundBlurDrawableExtImpl.setBlurParams sees !equals()
                Object clonedParam = cloneBlurParam(vrm, baseParam);
                if (clonedParam != null) {
                    if (!cfg.isFcEnabled() || cfg.isFcIslandStockAppearance()) {
                        restoreBlurParam(vrm, clonedParam);
                    } else {
                        applyTintToBlurParam(vrm, clonedParam, cfg);
                    }
                    invokeVrmSetBlurParams(vrm, clonedParam);
                    if (!sLoggedFirstVrmApply) {
                        sLoggedFirstVrmApply = true;
                        cfg.note("DynamicIsland: applied OplusBlurParam tint islandOp="
                                + cfg.getFcIslandOpacityInt()
                                + " blur=" + cfg.getFcBlurInt()
                                + " color=0x" + Integer.toHexString(cfg.getFcColor()));
                    }
                }
            }

            Integer origRad = ORIG_RADIUS.get(vrm);
            if (origRad == null && baseParam != null) {
                origRad = readSeedlingBaseBlurRadius(sLastPluginCl);
                ORIG_RADIUS.put(vrm, origRad);
            }
            if (origRad != null) {
                int targetRad = (cfg.isFcEnabled() && cfg.getFcBlurInt() != 100)
                        ? Math.max(0, Math.round(origRad * cfg.getFcBlurScale()))
                        : origRad;
                invokeVrmSetBlurRadius(vrm, targetRad);
            }
            LAST_VRM_HASH.put(vrm, desiredHash);
        } catch (Throwable ignored) {
        } finally {
            IN_REAPPLY.remove();
        }
    }

    private static void syncVrmNow(Object vrm, ConfigStore.HookConfig cfg) {
        boolean suppressCustom = shouldSuppressTopCustomColor(VRM_TO_VIEW.get(vrm), cfg);
        syncVrmNow(vrm, cfg, computeConfigHash(cfg) * 31 + (suppressCustom ? 1 : 0));
    }

    private static Object cloneBlurParam(Object vrm, Object sourceParam) {
        try {
            Class<?> paramCls = sourceParam.getClass();
            Constructor<?> ctor = paramCls.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object fresh = ctor.newInstance();

            float[] origFloats = ORIG_PARAM_FLOATS.get(vrm);
            if (origFloats == null) {
                Method toArr = findMethod(paramCls, "toFloatArray");
                if (toArr != null) {
                    Object res = toArr.invoke(sourceParam);
                    if (res instanceof float[]) {
                        origFloats = (float[]) res;
                    }
                }
            }
            if (origFloats != null) {
                Method fromArr = findMethod(paramCls, "fromFloatArray", float[].class);
                if (fromArr != null) {
                    fromArr.invoke(fresh, origFloats.clone());
                }
            }
            return fresh;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object extractBlurParamFromVrm(Object vrm) {
        try {
            Method getDrawable = findMethod(vrm.getClass(), "getBackgroundBlurDrawable");
            Object bbd = getDrawable != null ? getDrawable.invoke(vrm) : readObjectField(vrm, "mBackgroundBlurDrawable");
            if (bbd == null) {
                return null;
            }
            Method getWrapper = findMethod(bbd.getClass(), "getWrapper");
            if (getWrapper == null) {
                return null;
            }
            Object wrapper = getWrapper.invoke(bbd);
            if (wrapper == null) {
                return null;
            }
            Method getExt = findMethod(wrapper.getClass(), "getExtImpl");
            if (getExt == null) {
                return null;
            }
            Object ext = getExt.invoke(wrapper);
            if (ext == null) {
                return null;
            }
            return readObjectField(ext, "mBlurParams");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int computeConfigHash(ConfigStore.HookConfig cfg) {
        int h = cfg.isFcEnabled() ? 1 : 0;
        h = h * 31 + cfg.getFcIslandOpacityInt();
        h = h * 31 + cfg.getFcBlurInt();
        h = h * 31 + cfg.getFcBottomGlowInt();
        h = h * 31 + cfg.getFcBottomGlowChromaInt();
        h = h * 31 + (cfg.isFcColorEnabled() ? 1 : 0);
        h = h * 31 + cfg.getFcColor();
        h = h * 31 + (cfg.isFcCustomMaskEnabled() ? 1 : 0);
        h = h * 31 + cfg.getFcCustomMaskColor();
        h = h * 31 + (cfg.isFcRemoveIslandMask() ? 1 : 0);
        h = h * 31 + (cfg.isFcMaskFollowMedia() ? 1 : 0);
        h = h * 31 + cfg.getFcMaskBrightnessInt();
        h = h * 31 + cfg.getFcMaskChromaInt();
        h = h * 31 + cfg.getFcMaskStrengthInt();
        h = h * 31 + (cfg.isFcNonMediaMaskEnabled() ? 1 : 0);
        h = h * 31 + cfg.getFcNonMediaMaskColor();
        h = h * 31 + cfg.getFcNonMediaMaskOpacityInt();
        h = h * 31 + cfg.getFcNonMediaFollowMaskOpacityInt();
        h = h * 31 + cfg.getFcMaskGradStartInt();
        h = h * 31 + cfg.getFcMaskGradEndInt();
        h = h * 31 + cfg.getFcMaskGradMidInt();
        h = h * 31 + cfg.getFcMaskGradStrengthInt();
        if (cfg.isFcMaskFollowMedia() || cfg.isFcNonMediaMaskEnabled()
                || cfg.isFcColorEnabled() || cfg.isFcCustomMaskEnabled()) {
            h = h * 31 + (sTopMediaFluidCloudActive ? 1 : 0);
            h = h * 31 + sLatestMediaCoverColor;
            h = h * 31 + sLastMediaRawRgb;
        }
        return h;
    }

    private static void patchViewBackgroundLayers(View v, ConfigStore.HookConfig cfg) {
        Drawable bg = v.getBackground();
        if (bg == null) {
            return;
        }
        if (bg instanceof LayerDrawable) {
            LayerDrawable ld = (LayerDrawable) bg;
            for (int i = 0; i < ld.getNumberOfLayers(); i++) {
                if (patchGradientDrawable(ld.getDrawable(i), cfg)) {
                    v.invalidate();
                }
            }
        } else if (patchGradientDrawable(bg, cfg)) {
            v.invalidate();
        }
    }

    private static void reapplySiblingGradient(View bgView, ConfigStore.HookConfig cfg) {
        ViewParent parent = bgView.getParent();
        if (!(parent instanceof ViewGroup)) {
            return;
        }
        ViewGroup vg = (ViewGroup) parent;
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            if (child.getClass().getName().endsWith(".CardGradientOverlayView")) {
                patchGradientOverlay(child, cfg);
            }
        }
    }

    private static void patchGradientOverlay(View gradView, ConfigStore.HookConfig cfg) {
        if (gradView == null) {
            return;
        }
        ACTIVE_GRADIENT_VIEWS.put(gradView, Boolean.TRUE);
        Drawable bg = gradView.getBackground();
        if (bg == null) {
            return;
        }
        if (patchGradientDrawable(bg, cfg)) {
            gradView.invalidate();
        }
    }

    private static boolean patchGradientDrawable(Drawable drawable, ConfigStore.HookConfig cfg) {
        if (drawable == null) {
            return false;
        }
        View host = resolveDrawableHostView(drawable);
        View cardRoot = resolveEnclosingCardRoot(host);
        boolean isMedia = cardRoot != null && isViewContainingMedia(cardRoot);
        boolean wantMediaTint = isMedia
                ? cfg.isFcMaskFollowMedia()
                : cfg.isFcNonMediaMaskEnabled();
        int mediaMaskRgb = wantMediaTint ? resolveMediaRawToMaskRgb(cardRoot, isMedia, cfg) : 0;
        boolean hasMediaMaskTint = wantMediaTint && mediaMaskRgb != 0;
        boolean suppressCustom = isMedia
                || (cfg.isFcNonMediaMaskEnabled() && hasActiveTopMediaFluidCloud());
        boolean hasCustomMask = !hasMediaMaskTint && !suppressCustom && cfg.isFcCustomMaskEnabled();
        int customMaskArgb = hasCustomMask ? cfg.getFcCustomMaskColor() : 0;
        boolean hasMaskTint = hasMediaMaskTint || hasCustomMask;
        int maskRgb = hasMediaMaskTint ? mediaMaskRgb : (hasCustomMask ? customMaskArgb : 0);
        float catScale = isMedia
                ? cfg.getFcMaskStrengthScale()
                : cfg.getNonMediaMaskOpacityScaleForTint(hasMediaMaskTint);
        float customAlphaScale = hasCustomMask ? ConfigStore.colorAlphaScale(customMaskArgb) : 1.0f;
        float maskScale = catScale * cfg.getFcMaskGradStrengthScale() * customAlphaScale;

        GradReflection ref = inspectGradientDrawable(drawable.getClass());
        if (ref == null) {
            return false;
        }
        Rect bounds = drawable.getBounds();
        int bw = bounds != null ? bounds.width() : 0;
        int bh = bounds != null ? bounds.height() : 0;
        try {
            if (bw <= 0 && ref.widthField != null) {
                int fw = ref.widthField.getInt(drawable);
                if (fw > 0) {
                    bw = fw;
                }
            }
            if (bh <= 0 && ref.heightField != null) {
                int fh = ref.heightField.getInt(drawable);
                if (fh > 0) {
                    bh = fh;
                }
            }
        } catch (Throwable ignored) {
        }

        long fastKey = computeGradFastKey(drawable, cfg);
        int hash = computeConfigHash(cfg);
        hash = hash * 31 + ((hasMediaMaskTint ? 1 : 0) | (hasCustomMask ? 2 : 0));
        hash = hash * 31 + maskRgb;
        hash = hash * 31 + Float.floatToIntBits(maskScale);
        hash = hash * 31 + bw;
        hash = hash * 31 + bh;
        Integer prev = LAST_GRAD_HASH.get(drawable);
        if (prev != null && prev == hash) {
            LAST_GRAD_FAST_KEY.put(drawable, fastKey);
            return false;
        }
        try {
            int[] origStops = ORIG_GRAD_STOPS.get(drawable);
            if (origStops == null) {
                origStops = new int[]{
                        ref.colorFields[0].getInt(drawable),
                        ref.colorFields[1].getInt(drawable),
                        ref.colorFields[2].getInt(drawable)
                };
                ORIG_GRAD_STOPS.put(drawable, origStops);
            }

            if (!cfg.isFcEnabled()) {
                Object curShader = ref.shaderField.get(drawable);
                boolean alreadyRestored = ref.colorFields[0].getInt(drawable) == origStops[0]
                        && ref.colorFields[1].getInt(drawable) == origStops[1]
                        && ref.colorFields[2].getInt(drawable) == origStops[2]
                        && curShader != TRANSPARENT_SHADER
                        && curShader != null;
                if (alreadyRestored) {
                    LAST_GRAD_HASH.put(drawable, hash);
                    LAST_GRAD_FAST_KEY.put(drawable, fastKey);
                    return false;
                }
                for (int i = 0; i < 3; i++) {
                    ref.colorFields[i].setInt(drawable, origStops[i]);
                }
                ref.shaderField.set(drawable, null);
                if (ref.paintField != null) {
                    Object pObj = ref.paintField.get(drawable);
                    if (pObj instanceof Paint) {
                        ((Paint) pObj).setShader(null);
                    }
                }
                if (ref.widthField != null) {
                    ref.widthField.setInt(drawable, -1);
                }
                if (ref.heightField != null) {
                    ref.heightField.setInt(drawable, -1);
                }
                drawable.setAlpha(255);
                LAST_GRAD_HASH.put(drawable, hash);
                LAST_GRAD_FAST_KEY.put(drawable, fastKey);
                return true;
            }

            if (cfg.isFcRemoveIslandMask() || maskScale <= 0.001f) {
                for (int i = 0; i < 3; i++) {
                    ref.colorFields[i].setInt(drawable, 0);
                }
                Shader activeShader = (bw > 0 && bh > 0) ? TRANSPARENT_SHADER : null;
                int activeW = (bw > 0 && bh > 0) ? bw : -1;
                int activeH = (bw > 0 && bh > 0) ? bh : -1;
                ref.shaderField.set(drawable, activeShader);
                if (ref.paintField != null) {
                    Object pObj = ref.paintField.get(drawable);
                    if (pObj instanceof Paint) {
                        ((Paint) pObj).setShader(activeShader);
                    }
                }
                if (ref.widthField != null) {
                    ref.widthField.setInt(drawable, activeW);
                }
                if (ref.heightField != null) {
                    ref.heightField.setInt(drawable, activeH);
                }
                drawable.setAlpha(255);
                LAST_GRAD_HASH.put(drawable, hash);
                LAST_GRAD_FAST_KEY.put(drawable, fastKey);
                return true;
            }

            int rgb = 0;
            if (hasMaskTint) {
                rgb = maskRgb & 0x00FFFFFF;
            } else {
                for (int i = 0; i < 3; i++) {
                    int cRgb = origStops[i] & 0x00FFFFFF;
                    if (cRgb != 0) {
                        rgb = cRgb;
                        break;
                    }
                }
            }
            int basePeakA = 204;
            int aStart = Math.max(0, Math.min(255, Math.round(basePeakA * maskScale)));
            int aMid = Math.max(0, Math.min(255, Math.round(basePeakA * 0.5f * maskScale)));
            int aEnd = 0;
            int cStart = (aStart << 24) | rgb;
            int cMid = (aMid << 24) | rgb;
            int cEnd = (aEnd << 24) | rgb;
            ref.colorFields[0].setInt(drawable, cStart);
            ref.colorFields[1].setInt(drawable, cMid);
            ref.colorFields[2].setInt(drawable, cEnd);

            if (bw > 0 && bh > 0) {
                ConfigStore.SmoothGradientSpec spec = ConfigStore.buildSmoothGradient(
                        rgb,
                        aStart,
                        cfg.getFcMaskGradStartInt(),
                        cfg.getFcMaskGradEndInt(),
                        cfg.getFcMaskGradMidInt());
                LinearGradient lg = new LinearGradient(
                        0f, 0f, 0f, (float) bh,
                        spec.colors,
                        spec.positions,
                        Shader.TileMode.CLAMP);
                ref.shaderField.set(drawable, lg);
                if (ref.paintField != null) {
                    Object pObj = ref.paintField.get(drawable);
                    if (pObj instanceof Paint) {
                        ((Paint) pObj).setDither(true);
                        ((Paint) pObj).setShader(lg);
                    }
                }
                if (ref.widthField != null) {
                    ref.widthField.setInt(drawable, bw);
                }
                if (ref.heightField != null) {
                    ref.heightField.setInt(drawable, bh);
                }
            } else {
                ref.shaderField.set(drawable, null);
                if (ref.paintField != null) {
                    Object pObj = ref.paintField.get(drawable);
                    if (pObj instanceof Paint) {
                        ((Paint) pObj).setShader(null);
                    }
                }
                if (ref.widthField != null) {
                    ref.widthField.setInt(drawable, -1);
                }
                if (ref.heightField != null) {
                    ref.heightField.setInt(drawable, -1);
                }
            }
            drawable.setAlpha(255);
            LAST_GRAD_HASH.put(drawable, hash);
            LAST_GRAD_FAST_KEY.put(drawable, fastKey);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static GradReflection inspectGradientDrawable(Class<?> cls) {
        if (cls == null) {
            return null;
        }
        Object cached = GRAD_REF_CACHE.get(cls);
        if (cached != null) {
            return cached == MISSING ? null : (GradReflection) cached;
        }
        Field shaderField = null;
        Field paintField = null;
        Field widthField = null;
        Field heightField = null;
        List<Field> finalIntFields = new ArrayList<>();
        List<Field> mutableIntFields = new ArrayList<>();
        for (Field f : cls.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            Class<?> ft = f.getType();
            if (ft == LinearGradient.class || ft == Shader.class) {
                f.setAccessible(true);
                shaderField = f;
            } else if (ft == Paint.class) {
                f.setAccessible(true);
                paintField = f;
            } else if (ft == int.class && Modifier.isFinal(f.getModifiers())) {
                f.setAccessible(true);
                finalIntFields.add(f);
            } else if (ft == int.class) {
                f.setAccessible(true);
                String fn = f.getName();
                if ("j".equals(fn)) {
                    widthField = f;
                } else if ("k".equals(fn)) {
                    heightField = f;
                } else {
                    mutableIntFields.add(f);
                }
            }
        }
        GradReflection res = null;
        if (shaderField != null && finalIntFields.size() == 3) {
            if (widthField == null && mutableIntFields.size() >= 1) {
                widthField = mutableIntFields.get(0);
            }
            if (heightField == null && mutableIntFields.size() >= 2) {
                heightField = mutableIntFields.get(1);
            }
            res = new GradReflection(shaderField, paintField, finalIntFields.toArray(new Field[3]), widthField, heightField);
        }
        GRAD_REF_CACHE.putIfAbsent(cls, res != null ? res : MISSING);
        return res;
    }

    private static void ensureOrigParamSaved(Object vrm, Object blurParam) {
        if (!ORIG_PARAM_COLORS.containsKey(vrm)) {
            try {
                float[] blend = readFloatArray(blurParam, "mBlendColor");
                float[] mix = readFloatArray(blurParam, "mMixColor");
                float[][] saved = new float[][]{
                        blend != null ? blend.clone() : null,
                        mix != null ? mix.clone() : null
                };
                ORIG_PARAM_COLORS.put(vrm, saved);
            } catch (Throwable ignored) {
            }
        }
        if (!ORIG_PARAM_FLOATS.containsKey(vrm)) {
            try {
                Method toArr = findMethod(blurParam.getClass(), "toFloatArray");
                if (toArr != null) {
                    Object res = toArr.invoke(blurParam);
                    if (res instanceof float[]) {
                        ORIG_PARAM_FLOATS.put(vrm, ((float[]) res).clone());
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void restoreBlurParam(Object vrm, Object blurParam) {
        float[][] orig = ORIG_PARAM_COLORS.get(vrm);
        if (orig == null) {
            return;
        }
        float[] blend = readFloatArray(blurParam, "mBlendColor");
        float[] mix = readFloatArray(blurParam, "mMixColor");
        if (blend != null && orig[0] != null) {
            System.arraycopy(orig[0], 0, blend, 0, Math.min(blend.length, orig[0].length));
        }
        if (mix != null && orig[1] != null) {
            System.arraycopy(orig[1], 0, mix, 0, Math.min(mix.length, orig[1].length));
        }
    }

    private static void applyTintToBlurParam(Object vrm, Object blurParam, ConfigStore.HookConfig cfg) {
        float[] blend = readFloatArray(blurParam, "mBlendColor");
        float[] mix = readFloatArray(blurParam, "mMixColor");
        if (blend == null || mix == null || blend.length < 4 || mix.length < 4) {
            return;
        }

        float[][] orig = ORIG_PARAM_COLORS.get(vrm);
        float[] origBlend = (orig != null && orig[0] != null && orig[0].length >= 4)
                ? orig[0] : new float[]{0.702f, 0.702f, 0.702f, 0.302f};
        float[] origMix = (orig != null && orig[1] != null && orig[1].length >= 4)
                ? orig[1] : new float[]{0.149f, 0.149f, 0.149f, 0.702f};

        float op = cfg.getFcIslandOpacity();
        boolean useCustom = cfg.isFcColorEnabled()
                && !shouldSuppressTopCustomColor(VRM_TO_VIEW.get(vrm), cfg);
        int fcColor = useCustom ? cfg.getFcColor() : 0;
        float colorAlpha = useCustom ? ConfigStore.colorAlphaScale(fcColor) : 0.0f;
        float r = useCustom ? (((fcColor >> 16) & 0xFF) / 255.0f) : 0.0f;
        float g = useCustom ? (((fcColor >> 8) & 0xFF) / 255.0f) : 0.0f;
        float b = useCustom ? ((fcColor & 0xFF) / 255.0f) : 0.0f;

        if (!useCustom && cfg.getFcIslandOpacityInt() >= 100 && !cfg.isFcRemoveIslandMask()) {
            System.arraycopy(origMix, 0, mix, 0, 4);
        } else if (useCustom) {
            mix[0] = r;
            mix[1] = g;
            mix[2] = b;
            mix[3] = clamp01(0.74f * op * colorAlpha);
        } else {
            // Preserve clean neutral frosted translucency without ash-grey mix pollution
            mix[0] = 0.10f;
            mix[1] = 0.10f;
            mix[2] = 0.12f;
            mix[3] = clamp01(0.68f * op);
        }

        // Preserve ColorOS 17 native OplusBlurParam GLOW_OVERLAY highlight pipeline without weakening
        float tintR = 0.55f + 0.45f * r;
        float tintG = 0.55f + 0.45f * g;
        float tintB = 0.55f + 0.45f * b;
        blend[0] = useCustom ? (origBlend[0] + (tintR - origBlend[0]) * colorAlpha) : origBlend[0];
        blend[1] = useCustom ? (origBlend[1] + (tintG - origBlend[1]) * colorAlpha) : origBlend[1];
        blend[2] = useCustom ? (origBlend[2] + (tintB - origBlend[2]) * colorAlpha) : origBlend[2];
        blend[3] = origBlend[3];
    }

    private static void drawIslandBottomGlow(Drawable drawable, Canvas canvas, ConfigStore.HookConfig cfg) {
        if (drawable == null || canvas == null || !cfg.isFcEnabled() || Build.VERSION.SDK_INT < 33) {
            return;
        }
        Rect bounds = drawable.getBounds();
        int w = bounds.width();
        int h = bounds.height();
        if (w < 24 || h < 24) {
            return;
        }
        float alphaMul = drawable.getAlpha() / 255.0f;
        if (alphaMul <= 0.01f) {
            return;
        }

        View hostView = resolveDrawableHostView(drawable);
        View cardRoot = resolveEnclosingCardRoot(hostView);
        View scopeRoot = cardRoot != null ? cardRoot : hostView;
        if (scopeRoot != null && hasActiveMultiLightInTree(scopeRoot)) {
            return;
        }

        float dp = 3.0f;
        try {
            dp = Resources.getSystem().getDisplayMetrics().density;
        } catch (Throwable ignored) {
        }
        boolean isMediaCard = scopeRoot != null && isViewContainingMedia(scopeRoot);
        boolean isLargeMedia = isLargeMediaCardHeight(isMediaCard, h, dp);
        int boostPct = isLargeMedia ? cfg.getFcBottomGlowInt() : cfg.getFcSingleGlowInt();
        int chromaPct = isLargeMedia ? cfg.getFcBottomGlowChromaInt() : cfg.getFcSingleGlowChromaInt();
        if (boostPct <= 0) {
            return;
        }

        float s = dp / 3.0f;
        float[] cornerCfg = extractCornerConfig(drawable, w, h);
        float smoothMode = (Math.round(cornerCfg[0]) == 2) ? 1.0f : 0.0f;
        float cornerRadius = cornerCfg[1];
        float cornerWeight = cornerCfg[2] > 0.0f ? cornerCfg[2] : 3.0f;

        Path clipPath = readSmoothPath(drawable);
        Rect pathRect = readSmoothPathRect(drawable);
        Object curTriple = readObjectField(drawable, "e");
        Object prevTriple = LAST_PATH_TRIPLE.get(drawable);
        boolean tripleChanged = curTriple != null && prevTriple != null && !curTriple.equals(prevTriple);
        if (tripleChanged && pathRect != null) {
            pathRect.setEmpty();
        }
        boolean validClip = clipPath != null && !clipPath.isEmpty()
                && (pathRect == null || pathRect.equals(bounds))
                && !tripleChanged;
        if (!validClip) {
            clipPath = new Path();
            buildSmoothRoundRectPath(clipPath, bounds.left, bounds.top, bounds.right, bounds.bottom,
                    cornerRadius, cornerWeight, smoothMode > 0.5f);
        }
        if (curTriple != null) {
            LAST_PATH_TRIPLE.put(drawable, curTriple);
        }

        try {
            RuntimeShader shader = TL_GLOW_SHADER.get();
            if (shader == null) {
                shader = new RuntimeShader(ISLAND_BOTTOM_GLOW_AGSL);
                TL_GLOW_SHADER.set(shader);
            }
            Paint paint = TL_GLOW_PAINT.get();
            if (paint == null) {
                paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                TL_GLOW_PAINT.set(paint);
            }

            float baseW = 328.0f * dp;
            float baseH = 60.0f * dp;
            float scaleX = 1.0f + 0.3f * (baseW / Math.max(1.0f, (float) w));
            float scaleY = 1.0f + 0.8f * (baseH / Math.max(1.0f, (float) h));
            float range = 70.0f * s;
            float offsetY = 90.0f * s;
            float brightBoost = clamp01(boostPct / (float) ConfigStore.MAX_FC_BOTTOM_GLOW);
            float chromaBoost = clamp01(chromaPct / (float) ConfigStore.MAX_FC_BOTTOM_GLOW_CHROMA);

            float[] rgb = new float[]{1.0f, 1.0f, 1.0f};
            boolean colored = false;
            if (isMediaCard || cfg.isFcNonMediaMaskEnabled()) {
                int mediaTintArgb = resolveScopedMediaTintArgb(isMediaCard, true, false);
                if (mediaTintArgb != 0) {
                    boostHighlightChromaArgb(mediaTintArgb, chromaBoost, brightBoost, rgb);
                    colored = true;
                }
            }
            if (!colored && !isMediaCard && !(cfg.isFcNonMediaMaskEnabled() && hasActiveTopMediaFluidCloud())
                    && cfg.isFcColorEnabled() && ((cfg.getFcColor() >>> 24) & 0xFF) > 0) {
                float ca = ConfigStore.colorAlphaScale(cfg.getFcColor());
                boostHighlightChromaArgb(cfg.getFcColor(), chromaBoost * ca, brightBoost, rgb);
                colored = true;
            }

            float shadowA = (colored ? 0.44f : 0.34f) * (float) Math.pow(brightBoost, colored ? 0.80 : 0.85) * alphaMul;
            float rimA = (colored ? 0.78f : 0.68f) * (float) Math.pow(brightBoost, colored ? 0.80 : 0.85) * alphaMul;
            float rimFade = (6.0f + 5.0f * brightBoost) * s;

            shader.setFloatUniform("uSize", (float) w, (float) h);
            shader.setFloatUniform("uRadius", cornerRadius);
            shader.setFloatUniform("uSmoothMode", smoothMode);
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
                canvas.clipPath(clipPath);
                canvas.translate(bounds.left, bounds.top);
                canvas.drawRect(0f, 0f, (float) w, (float) h, paint);
            } finally {
                canvas.restoreToCount(sc);
            }
        } catch (Throwable t) {
            if (!sLoggedAgslErr) {
                sLoggedAgslErr = true;
                cfg.note("DynamicIsland: AGSL bottom glow err: " + t);
            }
        }
    }

    private static Number readTripleNumber(Object triple, String obfMethod, String ktMethod, String fieldName) {
        try {
            Method m = findMethod(triple.getClass(), obfMethod);
            if (m != null && m.getParameterTypes().length == 0) {
                Object val = m.invoke(triple);
                if (val instanceof Number) {
                    return (Number) val;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Method m = findMethod(triple.getClass(), ktMethod);
            if (m != null && m.getParameterTypes().length == 0) {
                Object val = m.invoke(triple);
                if (val instanceof Number) {
                    return (Number) val;
                }
            }
        } catch (Throwable ignored) {
        }
        Object fv = readObjectField(triple, fieldName);
        return fv instanceof Number ? (Number) fv : null;
    }

    private static float[] extractCornerConfig(Drawable drawable, int w, int h) {
        float cornerType = 2.0f;
        float radius = Math.min(w, h) * 0.25f;
        float weight = 3.0f;
        boolean gotRadius = false;
        boolean gotType = false;
        boolean gotWeight = false;
        try {
            Object triple = readObjectField(drawable, "e");
            if (triple != null) {
                Number nType = readTripleNumber(triple, "d", "getFirst", "first");
                if (nType != null) {
                    cornerType = nType.floatValue();
                    gotType = true;
                }
                Number nRadius = readTripleNumber(triple, "e", "getSecond", "second");
                if (nRadius != null) {
                    float r = nRadius.floatValue();
                    if (r > 0f) {
                        radius = r;
                        gotRadius = true;
                    }
                }
                Number nWeight = readTripleNumber(triple, "f", "getThird", "third");
                if (nWeight != null) {
                    float wt = nWeight.floatValue();
                    if (wt > 0f) {
                        weight = wt;
                        gotWeight = true;
                    }
                }
                if (!gotRadius || !gotType || !gotWeight) {
                    for (Field f : triple.getClass().getDeclaredFields()) {
                        if (Modifier.isStatic(f.getModifiers())) {
                            continue;
                        }
                        f.setAccessible(true);
                        Object v = f.get(triple);
                        if (!gotType && v instanceof Integer) {
                            cornerType = ((Integer) v).floatValue();
                            gotType = true;
                        } else if (v instanceof Float) {
                            float fv = (Float) v;
                            if (!gotRadius && fv > 10.0f) {
                                radius = fv;
                                gotRadius = true;
                            } else if (!gotWeight && fv > 0.0f && fv <= 10.0f) {
                                weight = fv;
                                gotWeight = true;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        if (Math.round(cornerType) == 2 && weight <= 0.0f) {
            weight = 3.0f;
        }
        return new float[]{cornerType, radius, weight};
    }

    private static volatile Field sSmoothPathField;

    private static Path readSmoothPath(Drawable drawable) {
        try {
            Field f = sSmoothPathField;
            if (f == null) {
                f = drawable.getClass().getDeclaredField("g");
                if (!Path.class.isAssignableFrom(f.getType())) {
                    return null;
                }
                f.setAccessible(true);
                sSmoothPathField = f;
            }
            Object val = f.get(drawable);
            return val instanceof Path ? (Path) val : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Rect readSmoothPathRect(Drawable drawable) {
        try {
            Field f = sSmoothPathRectField;
            if (f == null) {
                f = drawable.getClass().getDeclaredField("h");
                if (!Rect.class.isAssignableFrom(f.getType())) {
                    return null;
                }
                f.setAccessible(true);
                sSmoothPathRectField = f;
            }
            Object val = f.get(drawable);
            return val instanceof Rect ? (Rect) val : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object resolveVrmFromView(View view) {
        if (view == null) {
            return null;
        }
        Object vrm = VIEW_TO_VRM.get(view);
        if (vrm == null) {
            vrm = findVrmField(view);
            if (vrm != null) {
                VIEW_TO_VRM.put(view, vrm);
                VRM_TO_VIEW.put(vrm, view);
            }
        }
        return vrm;
    }

    private static Object findVrmField(View view) {
        Class<?> cls = view.getClass();
        while (cls != null && cls != View.class && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                if (f.getType().getName().endsWith(".ViewRootManager")) {
                    try {
                        f.setAccessible(true);
                        return f.get(view);
                    } catch (Throwable ignored) {
                    }
                }
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    private static View readViewField(Object obj, String fieldName) {
        Object val = readObjectField(obj, fieldName);
        return val instanceof View ? (View) val : null;
    }

    private static Field findFieldCached(Class<?> cls, String fieldName) {
        if (cls == null || fieldName == null) {
            return null;
        }
        ConcurrentHashMap<String, Object> map = FIELD_CACHE.get(cls);
        if (map == null) {
            ConcurrentHashMap<String, Object> fresh = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, Object> prev = FIELD_CACHE.putIfAbsent(cls, fresh);
            map = prev != null ? prev : fresh;
        }
        Object cached = map.get(fieldName);
        if (cached != null) {
            return cached == MISSING ? null : (Field) cached;
        }
        Field found = null;
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                found = f;
                break;
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            } catch (Throwable ignored) {
                break;
            }
        }
        map.putIfAbsent(fieldName, found != null ? found : MISSING);
        return found;
    }

    private static Object readObjectField(Object obj, String fieldName) {
        if (obj == null) {
            return null;
        }
        try {
            Field f = findFieldCached(obj.getClass(), fieldName);
            return f != null ? f.get(obj) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static float[] readFloatArray(Object obj, String fieldName) {
        Object v = readObjectField(obj, fieldName);
        return v instanceof float[] ? (float[]) v : null;
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... params) {
        if (cls == null || name == null) {
            return null;
        }
        if (params == null || params.length == 0) {
            ConcurrentHashMap<String, Object> map = NO_ARG_METHOD_CACHE.get(cls);
            if (map == null) {
                ConcurrentHashMap<String, Object> fresh = new ConcurrentHashMap<>();
                ConcurrentHashMap<String, Object> prev = NO_ARG_METHOD_CACHE.putIfAbsent(cls, fresh);
                map = prev != null ? prev : fresh;
            }
            Object cached = map.get(name);
            if (cached != null) {
                return cached == MISSING ? null : (Method) cached;
            }
            Method found = null;
            Class<?> c = cls;
            while (c != null && c != Object.class) {
                try {
                    Method m = c.getDeclaredMethod(name);
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
        while (c != null && c != Object.class) {
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

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    static int getLatestMediaCoverColor() {
        ensureMediaStillActive();
        return sLatestMediaCoverColor;
    }

    static int getLastMediaRawRgb() {
        ensureMediaStillActive();
        return sLastMediaRawRgb;
    }

    static int computeMediaMaskRgb(int rawRgb, int brightPct, int chromaPct) {
        int raw = rawRgb & 0x00FFFFFF;
        if (raw == 0) {
            return 0;
        }
        if (raw == sCachedMaskInRaw
                && brightPct == sCachedMaskInBright
                && chromaPct == sCachedMaskInChroma) {
            return sCachedMaskOutRgb;
        }
        float[] hsv = TL_HSV_SCRATCH.get();
        if (hsv == null) {
            hsv = new float[3];
            TL_HSV_SCRATCH.set(hsv);
        }
        Color.colorToHSV(0xFF000000 | raw, hsv);
        float b = clamp01(brightPct / 100.0f);
        float c = clamp01(chromaPct / 100.0f);
        float v = 0.10f + 0.82f * b;
        int out = Color.HSVToColor(new float[]{hsv[0], c, v}) & 0x00FFFFFF;
        sCachedMaskInRaw = raw;
        sCachedMaskInBright = brightPct;
        sCachedMaskInChroma = chromaPct;
        sCachedMaskOutRgb = out;
        return out;
    }

    static int getLastMediaMaskRgb(ConfigStore.HookConfig cfg) {
        if (cfg == null) {
            return 0;
        }
        ensureMediaStillActive();
        int raw = sLastMediaRawRgb;
        if (raw == 0 && sLatestMediaCoverColor != 0) {
            raw = toRawMediaRgb(sLatestMediaCoverColor);
        }
        return computeMediaMaskRgb(raw, cfg.getFcMaskBrightnessInt(), cfg.getFcMaskChromaInt());
    }

    private static int resolveMediaRawToMaskRgb(View cardRoot, boolean isMedia, ConfigStore.HookConfig cfg) {
        if (isMedia && cardRoot != null) {
            Integer perCardRaw = CARD_MEDIA_RAW_RGB.get(cardRoot);
            if (perCardRaw != null && perCardRaw != 0) {
                return computeMediaMaskRgb(perCardRaw, cfg.getFcMaskBrightnessInt(), cfg.getFcMaskChromaInt());
            }
            RowBgCache cache = resolveRowBgCache(cardRoot);
            if (cache != null) {
                for (int i = 0, n = cache.mediaBgRefs.size(); i < n; i++) {
                    View mediaBg = cache.mediaBgRefs.get(i).get();
                    if (mediaBg == null) {
                        continue;
                    }
                    Object sec = MEDIA_BG_TO_SECTION.get(mediaBg);
                    if (sec == null) {
                        continue;
                    }
                    Object lightObj = readObjectField(mediaBg, "F");
                    Drawable lightDr = lightObj instanceof Drawable ? (Drawable) lightObj : null;
                    int[] colors = new int[2];
                    extractMediaColorFromSection(sec, lightDr, colors);
                    int raw = colors[1] != 0 ? colors[1] : toRawMediaRgb(colors[0]);
                    if (raw != 0) {
                        CARD_MEDIA_RAW_RGB.put(cardRoot, raw);
                        return computeMediaMaskRgb(raw, cfg.getFcMaskBrightnessInt(), cfg.getFcMaskChromaInt());
                    }
                }
            }
        }
        if (!isMedia && !hasActiveTopMediaFluidCloud()) {
            return 0;
        }
        return getLastMediaMaskRgb(cfg);
    }

    /**
     * Re-syncs active primary media color across tracked MediaPlayerMultiCardSection carousels.
     */
    static void syncPrimaryMediaColor(ConfigStore.HookConfig cfg) {
        if (cfg == null) {
            return;
        }
        List<Object> carousels;
        synchronized (ACTIVE_MEDIA_CAROUSELS) {
            if (ACTIVE_MEDIA_CAROUSELS.isEmpty()) {
                return;
            }
            carousels = new ArrayList<>(ACTIVE_MEDIA_CAROUSELS.keySet());
        }
        for (int i = 0, n = carousels.size(); i < n; i++) {
            Object j1Obj = carousels.get(i);
            if (j1Obj != null) {
                syncActivePageColorFromCarousel(j1Obj, cfg, false, 2);
            }
        }
    }

    private static void syncActivePageColorFromCarousel(final Object j1Obj,
                                                        final ConfigStore.HookConfig cfg,
                                                        final boolean fromPageSelect,
                                                        final int retryCount) {
        if (j1Obj == null || cfg == null) {
            return;
        }
        Object vpObj = readObjectField(j1Obj, "u");
        if (!(vpObj instanceof View)) {
            return;
        }
        View vp = (View) vpObj;
        Object curObj = invokeNoArg(vp, "getCurrentItem");
        if (!(curObj instanceof Integer)) {
            return;
        }
        int curIdx = (Integer) curObj;
        Object adapter = readObjectField(j1Obj, "v");
        Object vh = adapter != null ? invokeOneArg(adapter, "a", int.class, curIdx) : null;
        if (vh == null && vp instanceof ViewGroup && ((ViewGroup) vp).getChildCount() > 0) {
            View rv = ((ViewGroup) vp).getChildAt(0);
            vh = invokeOneArg(rv, "findViewHolderForAdapterPosition", int.class, curIdx);
        }
        Object section = readObjectField(vh, "b");
        View mediaBg = readViewField(section, "s");
        Object lightObj = readObjectField(mediaBg, "F");
        Drawable lightDr = lightObj instanceof Drawable ? (Drawable) lightObj : null;
        if (section == null || mediaBg == null) {
            scheduleCarouselSyncRetry(j1Obj, cfg, fromPageSelect, retryCount);
            return;
        }
        String name = resolveMediaSectionName(section, mediaBg, lightDr);
        if ("MP_MiniCard".equals(name)) {
            return;
        }
        ACTIVE_MEDIA_SECTIONS.put(section, Boolean.TRUE);
        ACTIVE_MEDIA_HOSTS.put(mediaBg, name);
        MEDIA_BG_TO_SECTION.put(mediaBg, section);
        registerMediaViewAttachListener(mediaBg);
        if (lightDr != null) {
            ACTIVE_MEDIA_LIGHT_DRAWABLES.put(lightDr, Boolean.TRUE);
        }
        LightBaseline bl = lightDr != null ? LIGHT_BASELINES.get(lightDr) : null;
        if (!canMediaSurfaceUpdateGlobalColor(mediaBg, name, bl, fromPageSelect)) {
            return;
        }
        int[] colors = new int[2];
        extractMediaColorFromSection(section, lightDr, colors);
        if (colors[0] == 0 && colors[1] == 0) {
            scheduleCarouselSyncRetry(j1Obj, cfg, fromPageSelect, retryCount);
            return;
        }
        promotePrimaryMediaColor(lightDr, mediaBg, colors[0], colors[1], cfg);
    }

    private static void scheduleCarouselSyncRetry(Object j1Obj,
                                                  final ConfigStore.HookConfig cfg,
                                                  final boolean fromPageSelect,
                                                  final int retryCount) {
        if (retryCount >= 2 || j1Obj == null) {
            return;
        }
        final WeakReference<Object> ref = new WeakReference<>(j1Obj);
        MAIN_HANDLER.postDelayed(new Runnable() {
            @Override
            public void run() {
                Object target = ref.get();
                if (target != null && ACTIVE_MEDIA_CAROUSELS.containsKey(target)) {
                    syncActivePageColorFromCarousel(target, cfg, fromPageSelect, retryCount + 1);
                }
            }
        }, 80L);
    }

    private static boolean isHostOnSelectedCarouselPage(View hostView, LightBaseline bl) {
        if (hostView == null) {
            return true;
        }
        View vp = null;
        View rv = null;
        View pageItem = null;
        if (bl != null && bl.carouselProbed && hostView.isAttachedToWindow()) {
            if (bl.vpRef == null) {
                return true;
            }
            vp = bl.vpRef.get();
            rv = bl.rvRef != null ? bl.rvRef.get() : null;
            pageItem = bl.pageItemRef != null ? bl.pageItemRef.get() : null;
            if (vp == null || rv == null || pageItem == null
                    || pageItem.getParent() != rv || rv.getParent() != vp) {
                vp = null;
                rv = null;
                pageItem = null;
                bl.carouselProbed = false;
            }
        }
        if (vp == null) {
            View prev2 = null;
            View prev1 = hostView;
            ViewParent parent = hostView.getParent();
            for (int depth = 0; depth < 20 && parent != null; depth++) {
                if (parent instanceof View && isInstanceOfClassName(parent, CLS_VIEW_PAGER2)) {
                    vp = (View) parent;
                    rv = prev1;
                    pageItem = prev2;
                    break;
                }
                if (!(parent instanceof View)) {
                    break;
                }
                prev2 = prev1;
                prev1 = (View) parent;
                parent = prev1.getParent();
            }
            if (bl != null && (vp != null || hostView.isAttachedToWindow())) {
                bl.vpRef = vp != null ? new WeakReference<>(vp) : null;
                bl.rvRef = rv != null ? new WeakReference<>(rv) : null;
                bl.pageItemRef = pageItem != null ? new WeakReference<>(pageItem) : null;
                bl.carouselProbed = true;
            }
        }
        if (vp == null) {
            return true;
        }
        if (rv == null || pageItem == null) {
            return false;
        }
        Object curObj = invokeNoArg(vp, "getCurrentItem");
        if (!(curObj instanceof Integer)) {
            return true;
        }
        int currentItem = (Integer) curObj;
        int pagePos = -1;
        Object lm = invokeNoArg(rv, "getLayoutManager");
        if (lm != null) {
            Object posObj = invokeOneArg(lm, "getPosition", View.class, pageItem);
            if (posObj instanceof Integer) {
                pagePos = (Integer) posObj;
            }
        }
        if (pagePos < 0) {
            Object vh = invokeOneArg(rv, "getChildViewHolder", View.class, pageItem);
            Object sec = readObjectField(vh, "b");
            pagePos = readIntField(sec, "p0", -1);
        }
        return pagePos >= 0 && pagePos == currentItem;
    }

    private static boolean canMediaSurfaceUpdateGlobalColor(View hostView,
                                                            String name,
                                                            LightBaseline bl,
                                                            boolean fromUserPageSelect) {
        if (hostView != null && !isHostOnSelectedCarouselPage(hostView, bl)) {
            return false;
        }
        boolean thisShown = hostView != null && hostView.isAttachedToWindow() && hostView.isShown();
        if (fromUserPageSelect && thisShown) {
            return true;
        }
        boolean onLock = FluidCloudHook.isCurrentlyOnLockscreen();
        if (onLock) {
            if ("MP_StatusBar".equals(name)
                    && (!thisShown || hasVisibleSelectedMediaSurface("MP_Notification", hostView))) {
                return false;
            }
        } else {
            if ("MP_Notification".equals(name)
                    && !thisShown
                    && hasVisibleSelectedMediaSurface("MP_StatusBar", hostView)) {
                return false;
            }
        }
        return thisShown || !hasVisibleSelectedMediaSurface(null, hostView);
    }

    private static boolean hasVisibleSelectedMediaSurface(String requiredNameOrNull, View excludeHost) {
        List<Drawable> list;
        synchronized (ACTIVE_MEDIA_LIGHT_DRAWABLES) {
            if (ACTIVE_MEDIA_LIGHT_DRAWABLES.isEmpty()) {
                return false;
            }
            list = new ArrayList<>(ACTIVE_MEDIA_LIGHT_DRAWABLES.keySet());
        }
        for (int i = 0, n = list.size(); i < n; i++) {
            Drawable d = list.get(i);
            if (d == null) {
                continue;
            }
            if (requiredNameOrNull != null) {
                Object nameObj = readObjectField(d, "b");
                if (!requiredNameOrNull.equals(nameObj != null ? String.valueOf(nameObj) : "")) {
                    continue;
                }
            }
            LightBaseline bl = LIGHT_BASELINES.get(d);
            View h = (bl != null && bl.hostViewRef != null) ? bl.hostViewRef.get() : null;
            if (h == null || !h.isAttachedToWindow()) {
                h = resolveDrawableHostView(d);
            }
            if (h == null || h == excludeHost || !h.isAttachedToWindow() || !h.isShown()) {
                continue;
            }
            if (isHostOnSelectedCarouselPage(h, bl)) {
                return true;
            }
        }
        List<View> hostViews = null;
        List<String> hostNames = null;
        synchronized (ACTIVE_MEDIA_HOSTS) {
            if (!ACTIVE_MEDIA_HOSTS.isEmpty()) {
                hostViews = new ArrayList<>(ACTIVE_MEDIA_HOSTS.size());
                hostNames = new ArrayList<>(ACTIVE_MEDIA_HOSTS.size());
                for (Map.Entry<View, String> e : ACTIVE_MEDIA_HOSTS.entrySet()) {
                    View k = e.getKey();
                    if (k != null) {
                        hostViews.add(k);
                        hostNames.add(e.getValue());
                    }
                }
            }
        }
        if (hostViews != null) {
            for (int i = 0, n = hostViews.size(); i < n; i++) {
                View h = hostViews.get(i);
                String entryName = hostNames.get(i);
                if (h == null || h == excludeHost || !h.isAttachedToWindow() || !h.isShown()) {
                    continue;
                }
                if (requiredNameOrNull != null && !requiredNameOrNull.equals(entryName)) {
                    continue;
                }
                Object lightObj = readObjectField(h, "F");
                LightBaseline bl = (lightObj instanceof Drawable) ? LIGHT_BASELINES.get(lightObj) : null;
                if (isHostOnSelectedCarouselPage(h, bl)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int readArtColorsArgb(Object vm, String holderField) {
        if (vm == null) {
            return 0;
        }
        Object holder = readObjectField(vm, holderField);
        Object flow = readObjectField(holder, "b");
        Object model = invokeNoArg(flow, "getValue");
        Object aObj = readObjectField(model, "a");
        if (aObj instanceof Integer) {
            int argb = (Integer) aObj;
            int rgb = argb & 0x00FFFFFF;
            if (rgb != 0 && rgb != 0x00FFFFFF) {
                return argb;
            }
        }
        return 0;
    }

    private static void extractMediaColorFromSection(Object section, Drawable lightDr, int[] outArgbAndRaw) {
        outArgbAndRaw[0] = 0;
        outArgbAndRaw[1] = 0;
        int argb = 0;
        if (section != null) {
            int vmArgb = readArtColorsArgb(readObjectField(section, "l"), "O");
            if (vmArgb != 0) {
                argb = vmArgb;
                if (lightDr != null) {
                    LIGHT_COVER_COLOR.put(lightDr, vmArgb);
                }
            }
        }
        if (argb == 0 && lightDr != null) {
            Integer cov = LIGHT_COVER_COLOR.get(lightDr);
            if (cov != null) {
                argb = cov;
            }
        }
        int rawRgb = argb != 0 ? toRawMediaRgb(argb) : 0;
        if (rawRgb == 0 && lightDr != null) {
            LightBaseline bl = LIGHT_BASELINES.get(lightDr);
            if (bl != null) {
                rawRgb = bl.lastRecordedRawRgb != 0 ? bl.lastRecordedRawRgb : extractRawMediaRgb(bl);
            }
            if (rawRgb == 0) {
                Object params = readObjectField(lightDr, "e");
                float[] sa = readFloatArray(params, "i");
                float[] ea = readFloatArray(params, "n");
                rawRgb = extractRawRgbFromArrays(sa, ea);
            }
        }
        if (argb == 0 && rawRgb != 0) {
            argb = 0xFF000000 | rawRgb;
        }
        outArgbAndRaw[0] = argb;
        outArgbAndRaw[1] = rawRgb;
    }

    private static void updateCardMediaRawRgb(View cardRoot, int effRaw, ConfigStore.HookConfig cfg) {
        if (cardRoot == null || cfg == null) {
            return;
        }
        if (effRaw != 0) {
            Integer prev = CARD_MEDIA_RAW_RGB.put(cardRoot, effRaw);
            if ((prev == null || prev != effRaw)
                    && cfg.isFcEnabled()
                    && (cfg.isFcMaskFollowMedia() || cfg.isFcNonMediaMaskEnabled())
                    && !cfg.isFcRemoveIslandMask()) {
                repatchCardGradientsInTree(cardRoot, cfg);
            }
        } else {
            Integer prev = CARD_MEDIA_RAW_RGB.remove(cardRoot);
            if (prev != null && cfg.isFcEnabled()) {
                repatchCardGradientsInTree(cardRoot, cfg);
            }
        }
    }

    private static boolean promotePrimaryMediaColor(Drawable lightDr,
                                                    View hostView,
                                                    int argb,
                                                    int rawRgb,
                                                    ConfigStore.HookConfig cfg) {
        int effRaw = rawRgb != 0 ? rawRgb : (argb != 0 ? toRawMediaRgb(argb) : 0);
        int effArgb = argb != 0 ? argb : (effRaw != 0 ? (0xFF000000 | effRaw) : 0);
        if (effArgb == 0 && effRaw == 0) {
            return false;
        }
        boolean changed = (sLatestMediaCoverColor != effArgb) || (sLastMediaRawRgb != effRaw);
        sLatestMediaCoverColor = effArgb;
        sLastMediaRawRgb = effRaw;
        if (changed) {
            sMediaColorVersion++;
        }
        sLastMediaColorPromotedAtMs = SystemClock.uptimeMillis();
        sEverTrackedMediaSurface = true;
        if (cfg != null) {
            sLastHookConfig = cfg;
        }

        View host = hostView != null ? hostView : (lightDr != null ? resolveDrawableHostView(lightDr) : null);
        registerMediaViewAttachListener(host);
        updateCardMediaRawRgb(resolveEnclosingCardRoot(host), effRaw, cfg);
        if (changed) {
            notifyMediaColorChanged(cfg);
        }
        return true;
    }

    private static void repatchCardGradientsInTree(View root, ConfigStore.HookConfig cfg) {
        if (root == null) {
            return;
        }
        if (root.getClass().getName().endsWith(".CardGradientOverlayView")) {
            patchGradientOverlay(root, cfg);
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                repatchCardGradientsInTree(vg.getChildAt(i), cfg);
            }
        }
    }

    private static int extractRawMediaRgb(LightBaseline bl) {
        if (bl == null) {
            return 0;
        }
        return extractRawRgbFromArrays(bl.baseShadow, bl.baseEdge);
    }

    private static int extractRawRgbFromArrays(float[] shadowArr, float[] edgeArr) {
        float[] bestHsv = null;
        float bestScore = 0f;
        float[][] candidates = new float[][]{
                (edgeArr != null && edgeArr.length >= 3)
                        ? new float[]{edgeArr[0], edgeArr[1], edgeArr[2]} : null,
                (edgeArr != null && edgeArr.length >= 6)
                        ? new float[]{edgeArr[3], edgeArr[4], edgeArr[5]} : null,
                (shadowArr != null && shadowArr.length >= 5)
                        ? new float[]{shadowArr[2], shadowArr[3], shadowArr[4]} : null,
                (shadowArr != null && shadowArr.length >= 14)
                        ? new float[]{shadowArr[11], shadowArr[12], shadowArr[13]} : null
        };
        float[] tmp = new float[3];
        for (float[] c : candidates) {
            if (c == null) {
                continue;
            }
            float score = inspectTripletHsv(c[0], c[1], c[2], tmp, bestScore);
            if (score > bestScore) {
                bestScore = score;
                bestHsv = tmp.clone();
            }
        }
        return bestHsv != null ? (Color.HSVToColor(bestHsv) & 0x00FFFFFF) : 0;
    }

    private static float inspectTripletHsv(float r, float g, float b, float[] outHsv, float bestScore) {
        Color.RGBToHSV(
                Math.round(clamp01(r) * 255f),
                Math.round(clamp01(g) * 255f),
                Math.round(clamp01(b) * 255f),
                outHsv);
        if (outHsv[1] < 0.06f || outHsv[2] < 0.04f) {
            return bestScore;
        }
        float score = outHsv[1] * outHsv[2];
        return score > bestScore ? score : bestScore;
    }

    static int toRawMediaRgb(int argb) {
        int raw = argb & 0x00FFFFFF;
        if (raw == 0 || raw == 0x00FFFFFF) {
            return 0;
        }
        float[] hsv = TL_HSV_SCRATCH.get();
        if (hsv == null) {
            hsv = new float[3];
            TL_HSV_SCRATCH.set(hsv);
        }
        Color.colorToHSV(0xFF000000 | raw, hsv);
        if (hsv[1] < 0.06f || hsv[2] < 0.04f) {
            return 0;
        }
        return raw;
    }

    static void boostHighlightChromaArgb(int argb, float chromaBoost, float brightBoost, float[] outRgb) {
        boostHighlightChroma(
                ((argb >> 16) & 0xFF) / 255.0f,
                ((argb >> 8) & 0xFF) / 255.0f,
                (argb & 0xFF) / 255.0f,
                chromaBoost,
                brightBoost,
                outRgb);
    }

    static void boostHighlightChroma(float r, float g, float b, float chromaBoost, float brightBoost, float[] outRgb) {
        float cr = clamp01(r);
        float cg = clamp01(g);
        float cb = clamp01(b);
        float cbClamped = clamp01(chromaBoost);
        float bbClamped = clamp01(brightBoost);
        if (cbClamped <= 0.001f && bbClamped <= 0.001f) {
            outRgb[0] = cr;
            outRgb[1] = cg;
            outRgb[2] = cb;
            return;
        }
        float[] hsv = TL_HSV_SCRATCH.get();
        if (hsv == null) {
            hsv = new float[3];
            TL_HSV_SCRATCH.set(hsv);
        }
        Color.RGBToHSV(Math.round(cr * 255.0f), Math.round(cg * 255.0f), Math.round(cb * 255.0f), hsv);
        if (hsv[1] < 0.04f) {
            outRgb[0] = cr;
            outRgb[1] = cg;
            outRgb[2] = cb;
            return;
        }
        if (cbClamped > 0.001f && hsv[1] < 0.88f) {
            float satLift = (float) Math.pow(cbClamped, 1.10);
            hsv[1] = Math.min(0.88f, hsv[1] + (0.88f - hsv[1]) * 0.82f * satLift);
        }
        if (bbClamped > 0.001f) {
            float targetV = Math.min(1.0f, Math.max(hsv[2], 0.92f + 0.08f * bbClamped));
            hsv[2] = hsv[2] + (targetV - hsv[2]) * bbClamped;
        }
        int c = Color.HSVToColor(hsv);
        outRgb[0] = ((c >> 16) & 0xFF) / 255.0f;
        outRgb[1] = ((c >> 8) & 0xFF) / 255.0f;
        outRgb[2] = (c & 0xFF) / 255.0f;
    }

    static boolean isMultiLightActive(Object lightDrawable) {
        if (lightDrawable == null) {
            return false;
        }
        Object params = readObjectField(lightDrawable, "e");
        if (params == null) {
            return false;
        }
        return readFloatField(params, "f", 0.0f) > 0.5f || readFloatField(params, "k", 0.0f) > 0.5f;
    }

    private static RowBgCache resolveRowBgCache(View root) {
        if (root == null) {
            return null;
        }
        int curChildCount = (root instanceof ViewGroup) ? ((ViewGroup) root).getChildCount() : 0;
        long nowMs = SystemClock.uptimeMillis();
        RowBgCache entry = ROW_BG_CACHE.get(root);
        if (entry != null && entry.childCount == curChildCount) {
            boolean hasAny = !entry.pageBgRefs.isEmpty() || !entry.mediaBgRefs.isEmpty();
            if (!hasAny) {
                if (nowMs - entry.lastScanMs < 500L) {
                    return entry;
                }
            } else {
                boolean valid = true;
                for (int i = 0, n = entry.pageBgRefs.size(); i < n; i++) {
                    View v = entry.pageBgRefs.get(i).get();
                    if (v == null || !v.isAttachedToWindow()) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    for (int i = 0, n = entry.mediaBgRefs.size(); i < n; i++) {
                        View v = entry.mediaBgRefs.get(i).get();
                        if (v == null || !v.isAttachedToWindow()) {
                            valid = false;
                            break;
                        }
                    }
                }
                if (valid) {
                    return entry;
                }
            }
        }
        RowBgCache fresh = new RowBgCache();
        fresh.childCount = curChildCount;
        fresh.lastScanMs = nowMs;
        scanRowBgTree(root, fresh);
        ROW_BG_CACHE.put(root, fresh);
        return fresh;
    }

    private static void scanRowBgTree(View node, RowBgCache out) {
        if (node == null) {
            return;
        }
        String cn = node.getClass().getName();
        if (CLS_PAGE_BG_VIEW.equals(cn)) {
            out.pageBgRefs.add(new WeakReference<>(node));
        } else if (CLS_MEDIA_BG_VIEW.equals(cn)) {
            out.mediaBgRefs.add(new WeakReference<>(node));
        }
        if (node instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) node;
            for (int i = 0, n = vg.getChildCount(); i < n; i++) {
                scanRowBgTree(vg.getChildAt(i), out);
            }
        }
    }

    static boolean hasActiveMultiLightInTree(View root) {
        if (root == null || root.getVisibility() != View.VISIBLE) {
            return false;
        }
        RowBgCache cache = resolveRowBgCache(root);
        if (cache == null) {
            return false;
        }
        for (int i = 0, n = cache.mediaBgRefs.size(); i < n; i++) {
            View v = cache.mediaBgRefs.get(i).get();
            if (v != null && v.getVisibility() == View.VISIBLE
                    && isMultiLightActive(readObjectField(v, "F"))) {
                return true;
            }
        }
        for (int i = 0, n = cache.pageBgRefs.size(); i < n; i++) {
            View v = cache.pageBgRefs.get(i).get();
            if (v != null && v.getVisibility() == View.VISIBLE) {
                Object wrapper = readObjectField(v, "b");
                if (isMultiLightActive(readObjectField(wrapper, "c"))) {
                    return true;
                }
            }
        }
        return false;
    }

    static void invalidateMultiLightViewsInTree(View root) {
        RowBgCache cache = resolveRowBgCache(root);
        if (cache == null) {
            return;
        }
        for (int i = 0, n = cache.mediaBgRefs.size(); i < n; i++) {
            View v = cache.mediaBgRefs.get(i).get();
            if (v != null) {
                Object d = readObjectField(v, "F");
                if (d instanceof Drawable) {
                    ((Drawable) d).invalidateSelf();
                }
                v.invalidate();
            }
        }
        for (int i = 0, n = cache.pageBgRefs.size(); i < n; i++) {
            View v = cache.pageBgRefs.get(i).get();
            if (v != null) {
                Object w = readObjectField(v, "b");
                Object d = readObjectField(w, "c");
                if (d instanceof Drawable) {
                    ((Drawable) d).invalidateSelf();
                }
                v.invalidate();
            }
        }
    }

    static void setCardBackgroundViewsAlphaInTree(View root, float alpha) {
        RowBgCache cache = resolveRowBgCache(root);
        if (cache == null) {
            return;
        }
        float clamped = clamp01(alpha);
        for (int i = 0, n = cache.pageBgRefs.size(); i < n; i++) {
            View v = cache.pageBgRefs.get(i).get();
            if (v != null && Math.abs(v.getAlpha() - clamped) > 0.002f) {
                v.setAlpha(clamped);
            }
        }
        for (int i = 0, n = cache.mediaBgRefs.size(); i < n; i++) {
            View v = cache.mediaBgRefs.get(i).get();
            if (v != null && Math.abs(v.getAlpha() - 1.0f) > 0.002f) {
                v.setAlpha(1.0f);
            }
        }
    }

    private static boolean hasMediaBgViewInTree(View root) {
        if (root == null || root.getVisibility() != View.VISIBLE) {
            return false;
        }
        RowBgCache cache = resolveRowBgCache(root);
        if (cache == null) {
            return false;
        }
        for (int i = 0, n = cache.mediaBgRefs.size(); i < n; i++) {
            View v = cache.mediaBgRefs.get(i).get();
            if (v != null && v.getVisibility() == View.VISIBLE) {
                return true;
            }
        }
        return false;
    }

    private static View resolveDrawableHostView(Drawable drawable) {
        Drawable cur = drawable;
        for (int hop = 0; hop < 4 && cur != null; hop++) {
            Drawable.Callback cb = cur.getCallback();
            if (cb instanceof View) {
                return (View) cb;
            }
            if (cb instanceof Drawable) {
                cur = (Drawable) cb;
            } else {
                break;
            }
        }
        return null;
    }

    private static View resolveEnclosingCardRoot(View v) {
        View cur = v;
        for (int depth = 0; depth < 16 && cur != null; depth++) {
            String cn = cur.getClass().getName();
            if (cn.endsWith(".CardView") || cn.endsWith(".InstantContainer")) {
                return cur;
            }
            ViewParent p = cur.getParent();
            cur = p instanceof View ? (View) p : null;
        }
        return null;
    }

    private static boolean isInstanceOfClassName(Object obj, String className) {
        if (obj == null || className == null) {
            return false;
        }
        Class<?> c = obj.getClass();
        while (c != null && c != Object.class) {
            if (className.equals(c.getName())) {
                return true;
            }
            c = c.getSuperclass();
        }
        return false;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null) {
            return null;
        }
        try {
            Method m = findMethod(target.getClass(), methodName);
            return m != null ? m.invoke(target) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeOneArg(Object target, String methodName, Class<?> paramType, Object arg) {
        if (target == null || methodName == null || paramType == null) {
            return null;
        }
        try {
            Method m = findMethod(target.getClass(), methodName, paramType);
            return m != null ? m.invoke(target, arg) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int readIntField(Object obj, String fieldName, int defVal) {
        if (obj == null) {
            return defVal;
        }
        try {
            Field f = findFieldCached(obj.getClass(), fieldName);
            if (f != null) {
                if (f.getType() == int.class) {
                    return f.getInt(obj);
                }
                Object v = f.get(obj);
                if (v instanceof Integer) {
                    return (Integer) v;
                }
            }
        } catch (Throwable ignored) {
        }
        return defVal;
    }

    private static boolean isViewInsideCustomRow(View start) {
        View cur = start;
        for (int depth = 0; depth < 24 && cur != null; depth++) {
            if ("com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow".equals(cur.getClass().getName())) {
                return true;
            }
            ViewParent p = cur.getParent();
            cur = p instanceof View ? (View) p : null;
        }
        return false;
    }

    private static void probeLockscreenHostFlags(View start, LightBaseline bl) {
        bl.insideCustomRow = false;
        bl.insideCapsuleItem = false;
        View cur = start;
        for (int depth = 0; depth < 24 && cur != null; depth++) {
            String cn = cur.getClass().getName();
            if ("com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow".equals(cn)) {
                bl.insideCustomRow = true;
                return;
            }
            if ("com.oplus.systemui.notification.lockscreen.capsule.CapsuleItemView".equals(cn)) {
                bl.insideCapsuleItem = true;
                return;
            }
            ViewParent p = cur.getParent();
            cur = p instanceof View ? (View) p : null;
        }
    }

    private static boolean isDrawableInsideCustomRow(Drawable drawable) {
        return isViewInsideCustomRow(resolveDrawableHostView(drawable));
    }

    private static float readFloatField(Object obj, String fieldName, float defVal) {
        if (obj == null) {
            return defVal;
        }
        try {
            Field f = findFieldCached(obj.getClass(), fieldName);
            if (f != null) {
                if (f.getType() == float.class) {
                    return f.getFloat(obj);
                }
                Object v = f.get(obj);
                if (v instanceof Float) {
                    return (Float) v;
                }
            }
        } catch (Throwable ignored) {
        }
        return defVal;
    }

    private static void writeBooleanField(Object obj, String fieldName, boolean value) {
        if (obj == null) {
            return;
        }
        try {
            Field f = findFieldCached(obj.getClass(), fieldName);
            if (f != null) {
                f.setBoolean(obj, value);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void setParamDirtyFlag(Object params, boolean shadowDirty, boolean value) {
        if (params == null) {
            return;
        }
        try {
            Field f = shadowDirty ? sFldShadowDirty : sFldEdgeDirty;
            if (f == null || !f.getDeclaringClass().isInstance(params)) {
                f = findFieldCached(params.getClass(), shadowDirty ? "j" : "o");
                if (shadowDirty) {
                    sFldShadowDirty = f;
                } else {
                    sFldEdgeDirty = f;
                }
            }
            if (f != null) {
                f.setBoolean(params, value);
            }
        } catch (Throwable ignored) {
        }
    }

    private static float[] copyArrayInPlace(float[] src, float[] dst) {
        if (src == null) {
            return dst;
        }
        if (dst == null || dst.length != src.length) {
            return src.clone();
        }
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    private static void syncLightBaseline(LightBaseline bl, float[] shadowArr, float[] edgeArr) {
        if (shadowArr != null && shadowArr.length >= 14) {
            if (bl.baseShadow == null || bl.lastShadow == null || bl.lastShadow.length < 14) {
                bl.baseShadow = shadowArr.clone();
                bl.lastShadow = copyArrayInPlace(shadowArr, bl.lastShadow);
                bl.baselineDirty = true;
            } else {
                if (copyIdx1(shadowArr, bl.lastShadow, bl.baseShadow, 0)) bl.baselineDirty = true;
                if (copyIdx1(shadowArr, bl.lastShadow, bl.baseShadow, 1)) bl.baselineDirty = true;
                if (copyIdx3(shadowArr, bl.lastShadow, bl.baseShadow, 2, 3, 4)) bl.baselineDirty = true;
                if (copyIdx3(shadowArr, bl.lastShadow, bl.baseShadow, 11, 12, 13)) bl.baselineDirty = true;
            }
        }
        if (edgeArr != null && edgeArr.length >= 12) {
            if (bl.baseEdge == null || bl.lastEdge == null || bl.lastEdge.length < 12) {
                bl.baseEdge = edgeArr.clone();
                bl.lastEdge = copyArrayInPlace(edgeArr, bl.lastEdge);
                bl.baselineDirty = true;
            } else {
                if (copyIdx3(edgeArr, bl.lastEdge, bl.baseEdge, 0, 1, 2)) bl.baselineDirty = true;
                if (copyIdx3(edgeArr, bl.lastEdge, bl.baseEdge, 3, 4, 5)) bl.baselineDirty = true;
                if (copyIdx1(edgeArr, bl.lastEdge, bl.baseEdge, 7)) bl.baselineDirty = true;
                if (copyIdx1(edgeArr, bl.lastEdge, bl.baseEdge, 10)) bl.baselineDirty = true;
                if (copyIdx1(edgeArr, bl.lastEdge, bl.baseEdge, 11)) bl.baselineDirty = true;
            }
        }
    }

    private static boolean copyIdx1(float[] live, float[] last, float[] base, int idx) {
        if (Math.abs(live[idx] - last[idx]) > 1e-4f) {
            base[idx] = live[idx];
            last[idx] = live[idx];
            return true;
        }
        return false;
    }

    private static boolean copyIdx3(float[] live, float[] last, float[] base, int i0, int i1, int i2) {
        if (Math.abs(live[i0] - last[i0]) > 1e-4f
                || Math.abs(live[i1] - last[i1]) > 1e-4f
                || Math.abs(live[i2] - last[i2]) > 1e-4f) {
            base[i0] = live[i0];
            last[i0] = live[i0];
            base[i1] = live[i1];
            last[i1] = live[i1];
            base[i2] = live[i2];
            last[i2] = live[i2];
            return true;
        }
        return false;
    }

    private static void restoreLightBaseline(Object params, float[] shadowArr, float[] edgeArr, LightBaseline bl) {
        if (shadowArr != null && shadowArr.length >= 14 && bl.baseShadow != null) {
            for (int idx : SHADOW_RESTORE_IDX) {
                shadowArr[idx] = bl.baseShadow[idx];
            }
            setParamDirtyFlag(params, true, true);
            bl.lastShadow = copyArrayInPlace(shadowArr, bl.lastShadow);
        }
        if (edgeArr != null && edgeArr.length >= 12 && bl.baseEdge != null) {
            for (int idx : EDGE_RESTORE_IDX) {
                edgeArr[idx] = bl.baseEdge[idx];
            }
            setParamDirtyFlag(params, false, true);
            bl.lastEdge = copyArrayInPlace(edgeArr, bl.lastEdge);
        }
    }

    private static final class LightBaseline {
        float[] baseShadow;
        float[] lastShadow;
        float[] baseEdge;
        float[] lastEdge;
        boolean boosted;
        boolean baselineDirty;
        WeakReference<View> hostViewRef;
        boolean hostIsPageBgView;
        boolean insideCustomRow;
        boolean insideCapsuleItem;
        boolean customRowProbed;
        boolean registeredActiveLock;
        boolean registeredMediaLight;
        boolean carouselProbed;
        WeakReference<View> vpRef;
        WeakReference<View> rvRef;
        WeakReference<View> pageItemRef;
        int lastRecordedRawRgb;
        int lastAppliedKey;
    }

    private static final class RowBgCache {
        final ArrayList<WeakReference<View>> pageBgRefs = new ArrayList<>(2);
        final ArrayList<WeakReference<View>> mediaBgRefs = new ArrayList<>(2);
        int childCount;
        long lastScanMs;
    }

    private static final class GradReflection {
        final Field shaderField;
        final Field paintField;
        final Field[] colorFields;
        final Field widthField;
        final Field heightField;

        GradReflection(Field shaderField, Field paintField, Field[] colorFields,
                       Field widthField, Field heightField) {
            this.shaderField = shaderField;
            this.paintField = paintField;
            this.colorFields = colorFields;
            this.widthField = widthField;
            this.heightField = heightField;
        }
    }
}
