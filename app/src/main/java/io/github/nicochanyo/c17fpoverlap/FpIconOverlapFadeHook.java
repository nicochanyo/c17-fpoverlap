package io.github.nicochanyo.c17fpoverlap;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import io.github.nicochanyo.c17fpoverlap.XposedCompat.MethodHookParam;
import io.github.nicochanyo.c17fpoverlap.XposedCompat.XC_MethodHook;

/**
 * Smoothly fades the fingerprint icon layout when a lockscreen bottom capsule
 * overlaps the fingerprint sensor area (including on Music Lockscreen), while
 * leaving optical press-to-unlock and touch detection fully active.
 */
public final class FpIconOverlapFadeHook {
    private static final String CLS_UI_MECH = "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech";
    private static final String CLS_KGD_CB = "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$keyguardStateCallback$1";
    private static final String CLS_FP_ICON = "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintIcon";
    private static final String CLS_FP_UTILS = "com.oplus.systemui.biometrics.finger.KeyguardFingerprintUtils";
    private static final String CLS_CAPSULE_CONTAINER = "com.oplus.systemui.notification.lockscreen.capsule.NotificationCapsuleContainer";
    private static final String CLS_CAPSULE_ITEM = "com.oplus.systemui.notification.lockscreen.capsule.CapsuleItemView";
    private static final String CLS_CAPSULE_CONFIG = "com.oplus.systemui.notification.lockscreen.config.CapsuleConfig";
    private static final String CLS_ANIM_HELPER = "com.oplus.systemui.notification.lockscreen.capsule.animtor.ContainerAnimatorViewHelper";
    private static final String CLS_EXT_STATE_MGR = "com.oplus.systemui.notification.lockscreen.statemanager.LockScreenNotificationExtStateManager";

    private static final long CONFIRM_DELAY_MS = 180L;
    private static final long FADE_OUT_MS = 220L;
    private static final long FADE_IN_MS = 260L;
    private static final long FRAME_STEP_MS = 8L;

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Object SCHED_LOCK = new Object();
    private static final Object MISSING = new Object();
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> FIELD_CACHE =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> METHOD_CACHE =
            new ConcurrentHashMap<>();

    private static volatile Object sUiMech;
    private static volatile View sCapsuleContainer;
    private static volatile Object sExtStateManager;
    private static volatile Class<?> sCapsuleItemClass;
    private static volatile ClassLoader sClassLoader;
    private static volatile ConfigStore.HookConfig sCfg;

    private static final Runnable EVAL_RUNNABLE = new Runnable() {
        @Override
        public void run() {
            ConfigStore.HookConfig cfg = sCfg;
            if (cfg != null) {
                evaluateOnMainThread(cfg);
            }
        }
    };
    private static final Runnable CONFIRM_RUNNABLE = new Runnable() {
        @Override
        public void run() {
            ConfigStore.HookConfig cfg = sCfg;
            if (cfg != null) {
                evaluateOnMainThread(cfg);
            }
        }
    };

    // Accessed strictly on opticalUiUpdateHandler
    private static float sAnimTarget = -1f;
    private static Runnable sOpticalAnimTask;
    private static volatile Boolean sLastShouldHide;

    private FpIconOverlapFadeHook() {
    }

    public static void install(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        sClassLoader = cl;
        sCfg = cfg;
        sCapsuleItemClass = XposedCompat.findClassIfExists(CLS_CAPSULE_ITEM, cl);

        int installed = 0;
        if (hookUiMech(cl, cfg)) installed++;
        if (hookKgdCallback(cl, cfg)) installed++;
        if (hookFpIcon(cl, cfg)) installed++;
        if (hookCapsuleContainer(cl, cfg)) installed++;
        if (hookAnimHelper(cl, cfg)) installed++;
        if (hookExtStateMgr(cl, cfg)) installed++;
        cfg.note("FpIconOverlapFade hooks installed: " + installed + "/6");
    }

    private static boolean hookUiMech(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> cls = XposedCompat.findClassIfExists(CLS_UI_MECH, cl);
            if (cls == null) {
                return false;
            }
            XposedCompat.hookAllMethods(cls, "access$initWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args.length > 0 && param.args[0] != null) {
                        sUiMech = param.args[0];
                        scheduleEvaluate(cfg);
                    }
                }
            });
            XC_MethodHook uiTrigger = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.thisObject != null) {
                        sUiMech = param.thisObject;
                        scheduleEvaluate(cfg);
                    }
                }
            };
            Method mAlpha = findMethod(cls, "updateFpIconAlphaRunnable");
            if (mAlpha != null) {
                XposedCompat.hookMethod(mAlpha, uiTrigger);
            }
            Method mTouch = findMethod(cls, "onFpTouch", boolean.class);
            if (mTouch != null) {
                XposedCompat.hookMethod(mTouch, uiTrigger);
            }
            return true;
        } catch (Throwable t) {
            cfg.note("hookUiMech fade failed: " + t);
            return false;
        }
    }

    private static boolean hookKgdCallback(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> cls = XposedCompat.findClassIfExists(CLS_KGD_CB, cl);
            if (cls == null) {
                return false;
            }
            XposedCompat.hookAllMethods(cls, "onKeyguardShowingChanged", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (sUiMech == null && param.thisObject != null) {
                        Object outer = readField(param.thisObject, "this$0");
                        if (outer != null) {
                            sUiMech = outer;
                        }
                    }
                    scheduleEvaluate(cfg);
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("hookKgdCallback fade failed: " + t);
            return false;
        }
    }

    private static boolean hookFpIcon(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> cls = XposedCompat.findClassIfExists(CLS_FP_ICON, cl);
            if (cls == null) {
                return false;
            }
            Method mVis = XposedCompat.findDeclaredMethod(cls, "setVisibility", int.class);
            if (mVis == null) {
                return false;
            }
            XposedCompat.hookMethod(mVis, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (sUiMech == null && param.thisObject != null) {
                        Object mech = readField(param.thisObject, "onScreenFingerprintUiMech");
                        if (mech != null) {
                            sUiMech = mech;
                        }
                    }
                    scheduleEvaluate(cfg);
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("hookFpIcon fade failed: " + t);
            return false;
        }
    }

    private static boolean hookCapsuleContainer(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> cls = XposedCompat.findClassIfExists(CLS_CAPSULE_CONTAINER, cl);
            if (cls == null) {
                return false;
            }
            XC_MethodHook capTrigger = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.thisObject instanceof View) {
                        sCapsuleContainer = (View) param.thisObject;
                        scheduleEvaluate(cfg);
                    }
                }
            };
            Method mShow = XposedCompat.findDeclaredMethod(cls, "setShowing", boolean.class);
            if (mShow != null) {
                XposedCompat.hookMethod(mShow, capTrigger);
            }
            Method mVis = XposedCompat.findDeclaredMethod(cls, "setVisibility", int.class);
            if (mVis != null) {
                XposedCompat.hookMethod(mVis, capTrigger);
            }
            return true;
        } catch (Throwable t) {
            cfg.note("hookCapsuleContainer fade failed: " + t);
            return false;
        }
    }

    private static boolean hookAnimHelper(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> cls = XposedCompat.findClassIfExists(CLS_ANIM_HELPER, cl);
            if (cls == null) {
                return false;
            }
            XposedCompat.hookAllMethods(cls, "access$updateChildren", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args.length > 0 && param.args[0] != null) {
                        Object host = readField(param.args[0], "hostView");
                        if (host instanceof View) {
                            sCapsuleContainer = (View) host;
                        }
                    }
                    scheduleEvaluate(cfg);
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("hookAnimHelper fade failed: " + t);
            return false;
        }
    }

    private static boolean hookExtStateMgr(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> cls = XposedCompat.findClassIfExists(CLS_EXT_STATE_MGR, cl);
            if (cls == null) {
                return false;
            }
            XposedCompat.hookAllMethods(cls, "goToExtState", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.thisObject != null) {
                        sExtStateManager = param.thisObject;
                    }
                    scheduleEvaluate(cfg);
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("hookExtStateMgr fade failed: " + t);
            return false;
        }
    }

    private static void scheduleEvaluate(final ConfigStore.HookConfig cfg) {
        if (sUiMech == null) {
            return;
        }
        if (cfg != null && !cfg.isHideFpOnCapsule() && Boolean.FALSE.equals(sLastShouldHide)) {
            return;
        }
        sCfg = cfg;
        synchronized (SCHED_LOCK) {
            MAIN_HANDLER.removeCallbacks(EVAL_RUNNABLE);
            MAIN_HANDLER.removeCallbacks(CONFIRM_RUNNABLE);
            MAIN_HANDLER.post(EVAL_RUNNABLE);
            MAIN_HANDLER.postDelayed(CONFIRM_RUNNABLE, CONFIRM_DELAY_MS);
        }
    }

    private static void evaluateOnMainThread(ConfigStore.HookConfig cfg) {
        Object mech = sUiMech;
        if (mech == null) {
            return;
        }
        boolean shouldHide = false;
        try {
            shouldHide = computeShouldHide(mech, cfg);
        } catch (Throwable ignored) {
            shouldHide = false;
        }
        Boolean prev = sLastShouldHide;
        if (prev == null || prev.booleanValue() != shouldHide) {
            sLastShouldHide = shouldHide;
            cfg.note("fpIconOverlapFade -> shouldHide=" + shouldHide);
        }
        animateFpLayout(mech, shouldHide ? 0.0f : 1.0f);
    }

    private static boolean computeShouldHide(Object mech, ConfigStore.HookConfig cfg) {
        if (!cfg.isHideFpOnCapsule()) {
            return false;
        }

        // Safety guards: never hide bouncer FP, AOD screen-off FP, or in-app biometric FP
        if (readBool(mech, "isShowFpWhenBouncer", false)) {
            return false;
        }
        if (Boolean.TRUE.equals(callNoArg(mech, "getScreenTurnedOff"))) {
            return false;
        }
        Object kgdMonitor = readField(mech, "keyguardUpdateMonitor");
        if (kgdMonitor == null || !readBool(kgdMonitor, "mKeyguardShowing", false)) {
            return false;
        }
        Object kgdState = readField(mech, "keyguardStateController");
        if (kgdState != null) {
            if (readBool(kgdState, "mPrimaryBouncerShowing", false)
                    || readBool(kgdState, "mOccluded", false)) {
                return false;
            }
        }

        int extState = readExtState(mech);
        // extState 0 (stacked/expanded notifications) and 3 (sinking stack) never have the bottom capsule
        if (extState == 0 || extState == 3) {
            return false;
        }

        View container = sCapsuleContainer;
        if (container == null
                || !container.isAttachedToWindow()
                || container.getVisibility() != View.VISIBLE
                || container.getAlpha() <= 0.05f
                || !(container instanceof ViewGroup)) {
            return false;
        }

        Object animHelper = callNoArg(container, "getAnimatorHelper");
        Object ambientState = animHelper != null ? callNoArg(animHelper, "getAmbientState") : null;
        if (ambientState != null && readBool(ambientState, "isStackState", false)) {
            return false;
        }
        Object immersiveCard = ambientState != null ? callNoArg(ambientState, "getImmersiveCard") : null;

        View visibleCapsule = findVisibleBottomCapsule((ViewGroup) container, immersiveCard);
        if (visibleCapsule == null) {
            return false;
        }

        int[] fpBounds = resolveFpVerticalBounds(mech, container);
        int fpTop = fpBounds[0];
        int fpBottom = fpBounds[1];
        if (fpBottom <= fpTop) {
            return false;
        }

        int[] capBounds = resolveCapsuleVerticalBounds(container, visibleCapsule);
        int capTop = capBounds[0];
        int capBottom = capBounds[1];
        return (capTop < fpBottom + 8) && (capBottom > fpTop - 8);
    }

    private static int readExtState(Object mech) {
        try {
            Object extMgr = sExtStateManager;
            if (extMgr == null) {
                Object lazy = readField(mech, "lockScreenNotificationExtStateManagerLazy");
                if (lazy != null) {
                    extMgr = callNoArg(lazy, "get");
                }
            }
            if (extMgr == null) {
                return -1;
            }
            Object stateFlow = callNoArg(extMgr, "getExtStateEvent");
            Object event = stateFlow != null ? callNoArg(stateFlow, "getValue") : null;
            Object newState = event != null ? callNoArg(event, "getNewState") : null;
            return newState instanceof Number ? ((Number) newState).intValue() : -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static View findVisibleBottomCapsule(ViewGroup container, Object immersiveCard) {
        Class<?> itemCls = sCapsuleItemClass;
        int count = container.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = container.getChildAt(i);
            if (itemCls != null && !itemCls.isInstance(child)) {
                continue;
            }
            if (child == immersiveCard) {
                continue;
            }
            if (child.getVisibility() != View.VISIBLE) {
                continue;
            }
            Object vs = callNoArg(child, "getViewState");
            if (vs != null) {
                if (readBool(vs, "hidden", false) || readBool(vs, "gone", false)) {
                    continue;
                }
                float vsAlpha = readViewStateAlpha(vs, child.getAlpha());
                if (vsAlpha <= 0.05f && child.getAlpha() <= 0.05f) {
                    continue;
                }
            } else if (child.getAlpha() <= 0.05f) {
                continue;
            }
            return child;
        }
        return null;
    }

    private static int[] resolveFpVerticalBounds(Object mech, View container) {
        Object pressedObj = readField(mech, "pressedIcon");
        if (pressedObj instanceof View) {
            View pressedIcon = (View) pressedObj;
            int h = pressedIcon.getHeight();
            if (h > 0 && pressedIcon.isAttachedToWindow()) {
                int[] loc = new int[2];
                pressedIcon.getLocationOnScreen(loc);
                return new int[]{loc[1], loc[1] + h};
            }
        }
        try {
            Class<?> fpUtils = XposedCompat.findClassIfExists(CLS_FP_UTILS, sClassLoader);
            if (fpUtils != null) {
                Object inst = fpUtils.getField("INSTANCE").get(null);
                int fpMargin = ((Number) fpUtils.getMethod("getIconMarginBottom").invoke(inst)).intValue();
                int pressedSize = ((Number) fpUtils.getMethod("getPressedIconSize").invoke(inst)).intValue();
                DisplayMetrics dm = container.getResources().getDisplayMetrics();
                int fpCenter = dm.heightPixels - fpMargin;
                int half = Math.max(87, pressedSize / 2);
                return new int[]{fpCenter - half, fpCenter + half};
            }
        } catch (Throwable ignored) {
        }
        return new int[]{0, 0};
    }

    private static int[] resolveCapsuleVerticalBounds(View container, View visibleCapsule) {
        int[] cLoc = new int[2];
        container.getLocationOnScreen(cLoc);
        int itemH = readItemHeight(visibleCapsule);
        Object mbObj = callNoArg(container, "getMarginBottom");
        int marginBottom = mbObj instanceof Number ? ((Number) mbObj).intValue() : -1;
        if (marginBottom >= 0 && container.getHeight() > 0) {
            int capBottom = cLoc[1] + container.getHeight() - marginBottom;
            return new int[]{capBottom - itemH, capBottom};
        }
        Object vs = callNoArg(visibleCapsule, "getViewState");
        float ty = vs != null ? readViewStateYTranslation(vs, visibleCapsule.getTranslationY()) : visibleCapsule.getTranslationY();
        int capTop = cLoc[1] + Math.round(ty);
        return new int[]{capTop, capTop + itemH};
    }

    private static int readItemHeight(View visibleCapsule) {
        try {
            Class<?> cfgCls = XposedCompat.findClassIfExists(CLS_CAPSULE_CONFIG, sClassLoader);
            if (cfgCls != null) {
                Object inst = cfgCls.getField("INSTANCE").get(null);
                Object hObj = cfgCls.getMethod("getItemHeight").invoke(inst);
                if (hObj instanceof Number && ((Number) hObj).intValue() > 0) {
                    return ((Number) hObj).intValue();
                }
            }
        } catch (Throwable ignored) {
        }
        int h = visibleCapsule.getHeight();
        return h > 0 ? h : 120;
    }

    private static void animateFpLayout(Object mech, final float targetAlpha) {
        Object layoutObj = readField(mech, "fpIconLayout");
        Object handlerObj = readField(mech, "opticalUiUpdateHandler");
        if (!(layoutObj instanceof View) || !(handlerObj instanceof Handler)) {
            return;
        }
        final View layout = (View) layoutObj;
        final Handler optHandler = (Handler) handlerObj;

        optHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Math.abs(sAnimTarget - targetAlpha) < 0.001f && sOpticalAnimTask != null) {
                    return;
                }
                sAnimTarget = targetAlpha;
                if (sOpticalAnimTask != null) {
                    optHandler.removeCallbacks(sOpticalAnimTask);
                    sOpticalAnimTask = null;
                }
                final float startAlpha = layout.getAlpha();
                if (Math.abs(startAlpha - targetAlpha) < 0.005f) {
                    layout.setAlpha(targetAlpha);
                    return;
                }
                final long startMs = SystemClock.uptimeMillis();
                final long durationMs = (targetAlpha < startAlpha) ? FADE_OUT_MS : FADE_IN_MS;

                Runnable tick = new Runnable() {
                    @Override
                    public void run() {
                        if (Math.abs(sAnimTarget - targetAlpha) > 0.001f) {
                            return;
                        }
                        float t = (SystemClock.uptimeMillis() - startMs) / (float) durationMs;
                        if (t >= 1.0f) {
                            layout.setAlpha(targetAlpha);
                            sOpticalAnimTask = null;
                            return;
                        }
                        float inv = 1.0f - Math.max(0f, t);
                        float ease = 1.0f - inv * inv * inv;
                        layout.setAlpha(startAlpha + (targetAlpha - startAlpha) * ease);
                        optHandler.postDelayed(this, FRAME_STEP_MS);
                    }
                };
                sOpticalAnimTask = tick;
                optHandler.post(tick);
            }
        });
    }

    private static float readViewStateAlpha(Object vs, float fallback) {
        Object res = callNoArg(vs, "getAlpha");
        if (res instanceof Number) {
            return ((Number) res).floatValue();
        }
        return readFloat(vs, "alpha", readFloat(vs, "mAlpha", fallback));
    }

    private static float readViewStateYTranslation(Object vs, float fallback) {
        Object res = callNoArg(vs, "getYTranslation");
        if (res instanceof Number) {
            return ((Number) res).floatValue();
        }
        return readFloat(vs, "mYTranslation", readFloat(vs, "yTranslation", fallback));
    }

    private static Object callNoArg(Object target, String name) {
        if (target == null) {
            return null;
        }
        Method m = findMethod(target.getClass(), name);
        if (m == null) {
            return null;
        }
        try {
            return m.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object readField(Object target, String name) {
        Field f = findField(target != null ? target.getClass() : null, name);
        if (f == null) {
            return null;
        }
        try {
            return f.get(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean readBool(Object target, String name, boolean fallback) {
        Field f = findField(target != null ? target.getClass() : null, name);
        if (f == null) {
            return fallback;
        }
        try {
            return f.getBoolean(target);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static float readFloat(Object target, String name, float fallback) {
        Field f = findField(target != null ? target.getClass() : null, name);
        if (f == null) {
            return fallback;
        }
        try {
            return f.getFloat(target);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static Field findField(Class<?> cls, String name) {
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
        while (c != null && c != Object.class) {
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

    private static Method findMethod(Class<?> cls, String name, Class<?>... params) {
        if (cls == null || name == null) {
            return null;
        }
        ConcurrentHashMap<String, Object> map = METHOD_CACHE.get(cls);
        if (map == null) {
            ConcurrentHashMap<String, Object> fresh = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, Object> prev = METHOD_CACHE.putIfAbsent(cls, fresh);
            map = prev != null ? prev : fresh;
        }
        String key;
        if (params == null || params.length == 0) {
            key = name;
        } else if (params.length == 1) {
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
}
