package io.github.nicochanyo.c17fpoverlap;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.SurfaceControl;
import android.view.View;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.nicochanyo.c17fpoverlap.XposedCompat.MethodHookParam;
import io.github.nicochanyo.c17fpoverlap.XposedCompat.XC_MethodHook;

/**
 * Self-heal guard for ColorOS Lockscreen PlatformBlur on NotificationShade.
 * Ensures lockscreen widgets and bottom shortcut buttons do not render stale
 * Home Screen HardwareBuffer captures or remain stuck paused/preemptive when
 * entering lockscreen, waking from AOD, or cancelling an unlock transition.
 */
public final class LockscreenBlurGuardHook {
    private static final String C_PLATFORM_BLUR_MGR_IMPL =
            "com.oplus.systemui.panelanimation.platformblur.PlatformBlurManagerImpl";
    private static final String C_KEYGUARD_STATE_CTRL_IMPL =
            "com.android.systemui.statusbar.policy.KeyguardStateControllerImpl";
    private static final String C_WALLPAPER_ANIM_CTRL_IMPL =
            "com.oplus.systemui.keyguard.anim.OplusWallpaperAnimControllerImpl";
    private static final String C_AOD_CORE =
            "com.oplus.systemui.notification.aod.OplusPanoramicAodCoreStartable";
    private static final String C_UNLOCK_ANIM_EX_IMPL =
            "com.oplus.keyguard.OplusKeyguardUnlockAnimationControllerExImpl";
    private static final String C_POSTEFFECT_BLUR =
            "com.oplus.posteffect.Blur";
    private static final String C_BLUR_DRAWABLE_MGR =
            "com.oplus.posteffect.manager.BlurDrawableManager";
    private static final String C_SCRIM_UTIL =
            "com.oplusos.systemui.common.util.ScrimUtil";

    private static final String GUARD_REASON = "c17_kgd_blur_guard";
    private static final long MIN_UPDATE_CAPTURE_INTERVAL_MS = 160L;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Object MISSING = new Object();
    private static final Map<String, Object> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Object> METHOD_CACHE = new ConcurrentHashMap<>();

    private static volatile WeakReference<Object> sPlatformBlurMgrRef;
    private static volatile WeakReference<Object> sKeyguardStateCtrlRef;
    private static volatile WeakReference<Object> sUnlockAnimCtrlRef;
    private static volatile WeakReference<Object> sAodCoreRef;

    private static Runnable sHealPass1;
    private static Runnable sHealPass2;
    private static long sLastUpdateCaptureMs;
    private static boolean sWasKeyguardActive;
    private static boolean sLoggedFirstHeal;

    private LockscreenBlurGuardHook() {
    }

    /**
     * Installs lockscreen blur state and capture self-heal hooks into SystemUI.
     */
    public static void install(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        if (cl == null) {
            return;
        }
        int hooked = 0;
        if (hookPlatformBlurManager(cl, cfg)) {
            hooked++;
        }
        if (hookKeyguardStateController(cl, cfg)) {
            hooked++;
        }
        if (hookWallpaperAnimController(cl, cfg)) {
            hooked++;
        }
        if (hookPanoramicAodCore(cl, cfg)) {
            hooked++;
        }
        if (hookUnlockAnimController(cl, cfg)) {
            hooked++;
        }
        cfg.note("LockscreenBlurGuardHook installed targets=" + hooked + "/5");
    }

    private static boolean hookPlatformBlurManager(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> cls = XposedCompat.findClassIfExists(C_PLATFORM_BLUR_MGR_IMPL, cl);
            if (cls == null) {
                return false;
            }
            boolean ok = false;
            Method mUpdate = findMethod(cls, "updatePlatformBlurState", String.class);
            if (mUpdate != null) {
                XposedCompat.hookMethod(mUpdate, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object pbm = param.thisObject;
                        if (pbm != null) {
                            sPlatformBlurMgrRef = new WeakReference<>(pbm);
                        }
                        if (param.args.length > 0 && GUARD_REASON.equals(param.args[0])) {
                            return;
                        }
                        boolean active = isKeyguardBlurActiveNow(pbm);
                        if (active && !sWasKeyguardActive) {
                            sWasKeyguardActive = true;
                            scheduleLockscreenBlurHeal(cl, cfg, 90L, 280L);
                        } else if (!active) {
                            sWasKeyguardActive = false;
                        }
                    }
                });
                ok = true;
            }
            XC_MethodHook captureRefHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.thisObject != null) {
                        sPlatformBlurMgrRef = new WeakReference<>(param.thisObject);
                    }
                }
            };
            Method mPause = findMethod(cls, "pauseBlur", String.class, boolean.class);
            if (mPause != null) {
                XposedCompat.hookMethod(mPause, captureRefHook);
                ok = true;
            }
            Method mResume = findMethod(cls, "resumeBlur", String.class);
            if (mResume != null) {
                XposedCompat.hookMethod(mResume, captureRefHook);
                ok = true;
            }
            return ok;
        } catch (Throwable t) {
            cfg.note("LockscreenBlurGuard: PlatformBlurManagerImpl hook err: " + t);
            return false;
        }
    }

    private static boolean hookKeyguardStateController(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> cls = XposedCompat.findClassIfExists(C_KEYGUARD_STATE_CTRL_IMPL, cl);
            if (cls == null) {
                return false;
            }
            Method mNotify = findMethod(cls, "notifyKeyguardState", boolean.class, boolean.class);
            if (mNotify == null) {
                return false;
            }
            XposedCompat.hookMethod(mNotify, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.thisObject != null) {
                        sKeyguardStateCtrlRef = new WeakReference<>(param.thisObject);
                    }
                    boolean showing = param.args.length > 0 && Boolean.TRUE.equals(param.args[0]);
                    boolean occluded = param.args.length > 1 && Boolean.TRUE.equals(param.args[1]);
                    if (showing && !occluded) {
                        scheduleLockscreenBlurHeal(cl, cfg, 110L, 340L);
                    } else {
                        sWasKeyguardActive = false;
                        cancelPendingHeal();
                    }
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("LockscreenBlurGuard: KeyguardStateControllerImpl hook err: " + t);
            return false;
        }
    }

    private static boolean hookWallpaperAnimController(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> cls = XposedCompat.findClassIfExists(C_WALLPAPER_ANIM_CTRL_IMPL, cl);
            if (cls == null) {
                return false;
            }
            Method mVis = findMethod(cls, "onVisibilityChanged", int.class, boolean.class);
            if (mVis == null) {
                return false;
            }
            XposedCompat.hookMethod(mVis, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    int flags = (param.args.length > 0 && param.args[0] instanceof Integer)
                            ? (Integer) param.args[0] : 0;
                    boolean visible = param.args.length > 1 && Boolean.TRUE.equals(param.args[1]);
                    if ((flags & 2) != 0 && visible) {
                        scheduleLockscreenBlurHeal(cl, cfg, 80L, 280L);
                    }
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("LockscreenBlurGuard: OplusWallpaperAnimControllerImpl hook err: " + t);
            return false;
        }
    }

    private static boolean hookPanoramicAodCore(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> cls = XposedCompat.findClassIfExists(C_AOD_CORE, cl);
            if (cls == null) {
                return false;
            }
            Method mSetAod = findMethod(cls, "setPanoramicAod", boolean.class);
            if (mSetAod == null) {
                return false;
            }
            XposedCompat.hookMethod(mSetAod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.thisObject != null) {
                        sAodCoreRef = new WeakReference<>(param.thisObject);
                    }
                    boolean isAod = param.args.length > 0 && Boolean.TRUE.equals(param.args[0]);
                    if (!isAod) {
                        scheduleLockscreenBlurHeal(cl, cfg, 110L, 340L);
                    } else {
                        cancelPendingHeal();
                    }
                }
            });
            return true;
        } catch (Throwable t) {
            cfg.note("LockscreenBlurGuard: OplusPanoramicAodCoreStartable hook err: " + t);
            return false;
        }
    }

    private static boolean hookUnlockAnimController(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> cls = XposedCompat.findClassIfExists(C_UNLOCK_ANIM_EX_IMPL, cl);
            if (cls == null) {
                return false;
            }
            boolean ok = false;
            XC_MethodHook trackRefHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.thisObject != null) {
                        sUnlockAnimCtrlRef = new WeakReference<>(param.thisObject);
                    }
                }
            };
            if (!XposedCompat.hookAllMethods(cls, "onUnlockAnimationStarted", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.thisObject != null) {
                        sUnlockAnimCtrlRef = new WeakReference<>(param.thisObject);
                    }
                    sWasKeyguardActive = false;
                    cancelPendingHeal();
                }
            }).isEmpty()) {
                ok = true;
            }
            if (!XposedCompat.hookAllMethods(cls, "onUnlockAnimationFinished", trackRefHook).isEmpty()) {
                ok = true;
            }
            if (!XposedCompat.hookAllMethods(cls, "setBlurPreemptive", trackRefHook).isEmpty()) {
                ok = true;
            }
            return ok;
        } catch (Throwable t) {
            cfg.note("LockscreenBlurGuard: UnlockAnimController hook err: " + t);
            return false;
        }
    }

    private static void cancelPendingHeal() {
        Runnable r1 = sHealPass1;
        if (r1 != null) {
            MAIN.removeCallbacks(r1);
            sHealPass1 = null;
        }
        Runnable r2 = sHealPass2;
        if (r2 != null) {
            MAIN.removeCallbacks(r2);
            sHealPass2 = null;
        }
    }

    private static void scheduleLockscreenBlurHeal(final ClassLoader cl, final ConfigStore.HookConfig cfg,
                                                   final long delay1Ms, final long delay2Ms) {
        Runnable scheduler = new Runnable() {
            @Override
            public void run() {
                cancelPendingHeal();
                Runnable pass = new Runnable() {
                    @Override
                    public void run() {
                        runLockscreenBlurHeal(cl, cfg);
                    }
                };
                sHealPass1 = pass;
                sHealPass2 = new Runnable() {
                    @Override
                    public void run() {
                        runLockscreenBlurHeal(cl, cfg);
                    }
                };
                MAIN.postDelayed(sHealPass1, delay1Ms);
                MAIN.postDelayed(sHealPass2, delay2Ms);
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            scheduler.run();
        } else {
            MAIN.post(scheduler);
        }
    }

    private static boolean isKeyguardBlurActiveNow(Object pbm) {
        if (pbm == null) {
            return false;
        }
        try {
            Object ctxObj = readField(pbm, "context");
            if (!(ctxObj instanceof Context)) {
                return false;
            }
            PowerManager pm = ((Context) ctxObj).getSystemService(PowerManager.class);
            if (pm != null && !pm.isInteractive()) {
                return false;
            }
            Object ksc = sKeyguardStateCtrlRef != null ? sKeyguardStateCtrlRef.get() : null;
            if (ksc == null) {
                ksc = callLazyGet(readField(pbm, "keyguardStateController"));
            }
            if (ksc == null) {
                return false;
            }
            if (!Boolean.TRUE.equals(callNoArg(ksc, "isShowing"))
                    || !Boolean.TRUE.equals(callNoArg(ksc, "isVisible"))
                    || Boolean.TRUE.equals(callNoArg(ksc, "isOccluded"))) {
                return false;
            }
            Object sbCtrl = readField(pbm, "statusBarStateController");
            Object stObj = callNoArg(sbCtrl, "getState");
            return stObj instanceof Integer && ((Integer) stObj) == 1;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void runLockscreenBlurHeal(ClassLoader cl, ConfigStore.HookConfig cfg) {
        try {
            Object pbm = sPlatformBlurMgrRef != null ? sPlatformBlurMgrRef.get() : null;
            if (pbm == null) {
                return;
            }
            if (Boolean.TRUE.equals(callNoArg(pbm, "isUnsupportBlur"))) {
                return;
            }
            Object ctxObj = readField(pbm, "context");
            if (!(ctxObj instanceof Context)) {
                return;
            }
            Context ctx = (Context) ctxObj;
            PowerManager pm = ctx.getSystemService(PowerManager.class);
            if (pm != null && !pm.isInteractive()) {
                return;
            }

            Object ksc = sKeyguardStateCtrlRef != null ? sKeyguardStateCtrlRef.get() : null;
            if (ksc == null) {
                ksc = callLazyGet(readField(pbm, "keyguardStateController"));
            }
            if (ksc == null
                    || !Boolean.TRUE.equals(callNoArg(ksc, "isShowing"))
                    || !Boolean.TRUE.equals(callNoArg(ksc, "isVisible"))
                    || Boolean.TRUE.equals(callNoArg(ksc, "isOccluded"))) {
                return;
            }
            Object ex = callNoArg(ksc, "getEx");
            if (ex != null && Boolean.TRUE.equals(callNoArg(ex, "isUnlockAnimationRunning"))) {
                return;
            }

            Object sbCtrl = readField(pbm, "statusBarStateController");
            Object stObj = callNoArg(sbCtrl, "getState");
            if (!(stObj instanceof Integer) || ((Integer) stObj) != 1) {
                return;
            }

            Object aodRepo = callLazyGet(readField(pbm, "aodRepository"));
            if (aodRepo == null && sAodCoreRef != null) {
                aodRepo = sAodCoreRef.get();
            }
            if (aodRepo != null
                    && Boolean.TRUE.equals(readField(aodRepo, "isPanoramicAod"))
                    && !Boolean.TRUE.equals(callNoArg(aodRepo, "getShouldUpdateUi"))) {
                return;
            }

            Object kvmEx = callLazyGet(readField(pbm, "oplusKeyguardViewMediatorExImpl"));
            if (kvmEx != null && Boolean.TRUE.equals(callNoArg(kvmEx, "oneShotAnimRunning"))) {
                return;
            }

            if (isBAnimLevel(cl)) {
                return;
            }

            Class<?> blurCls = XposedCompat.findClassIfExists(C_POSTEFFECT_BLUR, cl);
            if (blurCls == null) {
                return;
            }

            boolean healedPreemptive = false;
            boolean healedPause = false;
            boolean refreshedCapture = false;

            Object preObj = callStaticOneArg(blurCls, "isBlurPreemptive", Context.class, ctx);
            if (Boolean.TRUE.equals(preObj)) {
                callStaticTwoArgs(blurCls, "setBlurPreemptive", Context.class, boolean.class, ctx, false);
                Object unlockCtrl = sUnlockAnimCtrlRef != null ? sUnlockAnimCtrlRef.get() : null;
                if (unlockCtrl != null) {
                    writeBooleanField(unlockCtrl, "blurPreemptiveStateOnlyInPanoramicAod", false);
                }
                healedPreemptive = true;
            }

            Object bdm = getBlurDrawableManager(cl, ctx);
            if (bdm != null && Boolean.TRUE.equals(readField(bdm, "windowBlurPaused"))) {
                writeField(pbm, "isBlurResumed", null);
                callOneArg(pbm, "updatePlatformBlurState", String.class, GUARD_REASON);
                if (Boolean.TRUE.equals(readField(bdm, "windowBlurPaused"))) {
                    callStaticOneArg(blurCls, "resumeWindowBlur", Context.class, ctx);
                    writeField(pbm, "isBlurResumed", Boolean.TRUE);
                }
                healedPause = true;
            }

            long now = SystemClock.uptimeMillis();
            if ((now - sLastUpdateCaptureMs) >= MIN_UPDATE_CAPTURE_INTERVAL_MS
                    && (bdm == null || !Boolean.TRUE.equals(readField(bdm, "windowBlurPaused")))) {
                SurfaceControl sc = resolveShadeSurfaceControl(pbm);
                if (sc != null && isSurfaceValid(sc)) {
                    callStaticTwoArgs(blurCls, "updateCapture", Context.class, SurfaceControl.class, ctx, sc);
                    sLastUpdateCaptureMs = now;
                    refreshedCapture = true;
                }
            }

            if (!sLoggedFirstHeal && (healedPreemptive || healedPause || refreshedCapture)) {
                sLoggedFirstHeal = true;
                cfg.note("LockscreenBlurGuard active: preemptive=" + healedPreemptive
                        + " pause=" + healedPause + " capture=" + refreshedCapture);
            }
        } catch (Throwable t) {
            cfg.note("LockscreenBlurGuard: heal err: " + t);
        }
    }

    private static boolean isBAnimLevel(ClassLoader cl) {
        Class<?> scrimUtilCls = XposedCompat.findClassIfExists(C_SCRIM_UTIL, cl);
        if (scrimUtilCls == null) {
            return false;
        }
        Object res = callStaticOneArg(scrimUtilCls, "isBAnimLevel", boolean.class, false);
        return Boolean.TRUE.equals(res);
    }

    private static Object getBlurDrawableManager(ClassLoader cl, Context ctx) {
        Class<?> bdmCls = XposedCompat.findClassIfExists(C_BLUR_DRAWABLE_MGR, cl);
        if (bdmCls == null) {
            return null;
        }
        Object companion = readStaticField(bdmCls, "Companion");
        if (companion == null) {
            return null;
        }
        return callOneArg(companion, "getInstance", Context.class, ctx);
    }

    private static SurfaceControl resolveShadeSurfaceControl(Object pbm) {
        Object scrimCtrl = callLazyGet(readField(pbm, "scrimController"));
        if (scrimCtrl == null) {
            return null;
        }
        Object scrimBehind = callNoArg(scrimCtrl, "getScrimBehind");
        if (!(scrimBehind instanceof View)) {
            return null;
        }
        Object vri = callNoArg(scrimBehind, "getViewRootImpl");
        if (vri == null) {
            return null;
        }
        Object scObj = callNoArg(vri, "getSurfaceControl");
        return (scObj instanceof SurfaceControl) ? (SurfaceControl) scObj : null;
    }

    private static boolean isSurfaceValid(SurfaceControl sc) {
        if (sc == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false;
        }
        return sc.isValid();
    }

    private static Object callLazyGet(Object lazy) {
        return lazy != null ? callNoArg(lazy, "get") : null;
    }

    private static Field findFieldCached(Class<?> cls, String name) {
        if (cls == null || name == null) {
            return null;
        }
        String key = cls.getName() + "#" + name;
        Object cached = FIELD_CACHE.get(key);
        if (cached == MISSING) {
            return null;
        }
        if (cached instanceof Field) {
            return (Field) cached;
        }
        Field found = null;
        Class<?> c = cls;
        while (c != null && c != Object.class && found == null) {
            try {
                found = c.getDeclaredField(name);
                found.setAccessible(true);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            } catch (Throwable ignored) {
                break;
            }
        }
        FIELD_CACHE.put(key, found != null ? found : MISSING);
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

    private static Object readStaticField(Class<?> cls, String name) {
        try {
            Field f = findFieldCached(cls, name);
            return f != null ? f.get(null) : null;
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

    private static void writeBooleanField(Object obj, String name, boolean val) {
        if (obj == null) {
            return;
        }
        try {
            Field f = findFieldCached(obj.getClass(), name);
            if (f != null && f.getType() == boolean.class) {
                f.setBoolean(obj, val);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... params) {
        if (cls == null || name == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(cls.getName()).append('#').append(name);
        if (params != null) {
            for (Class<?> p : params) {
                sb.append(':').append(p != null ? p.getName() : "null");
            }
        }
        String key = sb.toString();
        Object cached = METHOD_CACHE.get(key);
        if (cached == MISSING) {
            return null;
        }
        if (cached instanceof Method) {
            return (Method) cached;
        }
        Method found = null;
        Class<?> c = cls;
        while (c != null && c != Object.class && found == null) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                if (!Modifier.isAbstract(m.getModifiers())) {
                    m.setAccessible(true);
                    found = m;
                }
            } catch (NoSuchMethodException ignored) {
                c = c.getSuperclass();
            } catch (Throwable ignored) {
                break;
            }
        }
        METHOD_CACHE.put(key, found != null ? found : MISSING);
        return found;
    }

    private static Object callNoArg(Object obj, String name) {
        if (obj == null) {
            return null;
        }
        try {
            Method m = findMethod(obj.getClass(), name);
            return m != null ? m.invoke(obj) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object callOneArg(Object obj, String name, Class<?> p0, Object a0) {
        if (obj == null) {
            return null;
        }
        try {
            Method m = findMethod(obj.getClass(), name, p0);
            return m != null ? m.invoke(obj, a0) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object callStaticOneArg(Class<?> cls, String name, Class<?> p0, Object a0) {
        if (cls == null) {
            return null;
        }
        try {
            Method m = findMethod(cls, name, p0);
            return m != null ? m.invoke(null, a0) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object callStaticTwoArgs(Class<?> cls, String name,
                                            Class<?> p0, Class<?> p1, Object a0, Object a1) {
        if (cls == null) {
            return null;
        }
        try {
            Method m = findMethod(cls, name, p0, p1);
            return m != null ? m.invoke(null, a0, a1) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
