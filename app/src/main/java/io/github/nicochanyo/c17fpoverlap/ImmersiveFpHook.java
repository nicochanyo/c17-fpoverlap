package io.github.nicochanyo.c17fpoverlap;

import android.view.MotionEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.nicochanyo.c17fpoverlap.XposedCompat.MethodHookParam;
import io.github.nicochanyo.c17fpoverlap.XposedCompat.XC_MethodHook;

public final class ImmersiveFpHook {
    private static final String CLS_BIO_CTRL = "com.oplus.systemui.biometrics.OplusBiometricAuthController";
    private static final String CLS_FP_CTRL = "com.oplus.systemui.biometrics.finger.udfps.OnScreenFpControl";
    private static final String CLS_IMMERSIVE_TOUCH = "com.oplus.systemui.notification.lockscreen.immersive.OplusImmersiveTouchHandler";
    private static final String CLS_UPDATE_ALPHA_RUNNABLE = "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$updateFpIconAlpha$1";
    private static final String CLS_EXT_STATE_COLLECTOR = "com.oplus.systemui.biometrics.OplusBiometricAuthController$1$1";

    private static volatile Object sBioAuthController;
    private static volatile Method sIsFpIconVisibleMethod;
    private static volatile Method sIsInFingerViewAreaMethod;
    private static volatile Method sGetNewStateM;
    private static volatile Field sThis0Field;
    private static volatile Field sExtLazyField;
    private static volatile Method sLazyGetM;
    private static volatile Method sGetExtStateEventM;
    private static volatile Method sGetValueM;
    private static volatile Field sNewStateField;

    private static final Map<Object, Boolean> IGNORING_FP_TOUCH =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ImmersiveFpHook() {
    }

    public static void install(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        hookBioController(cl, cfg);
        hookOnScreenFpControl(cl, cfg);
        hookImmersiveTouchHandler(cl, cfg);
        hookPendingFpShow(cl, cfg);
        hookPanoramaImmersedGuard(cl, cfg);
        hookBiometricExtStateCollector(cl, cfg);
    }

    private static void hookBioController(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> bioCls = XposedCompat.findClass(CLS_BIO_CTRL, cl);
            sIsFpIconVisibleMethod = findMethod(bioCls, "isFpIconVisible");
            sIsInFingerViewAreaMethod = findMethod(bioCls, "isInFingerViewArea", int.class, int.class);

            XposedCompat.findAndHookMethod(bioCls, "setImmersed", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    sBioAuthController = param.thisObject;
                }
            });

            XposedCompat.findAndHookMethod(bioCls, "isImmersedState", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    sBioAuthController = param.thisObject;
                    if (cfg.isImmersiveFpEnabled()) {
                        param.setResult(Boolean.FALSE);
                    }
                }
            });
            cfg.note("hook ImmersiveFp bio controller OK");
        } catch (Throwable t) {
            cfg.note("hook ImmersiveFp bio controller failed: " + t);
        }
    }

    private static void hookOnScreenFpControl(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> fpCls = XposedCompat.findClass(CLS_FP_CTRL, cl);
            Class<?> bioCls = XposedCompat.findClass(CLS_BIO_CTRL, cl);
            final Field bioField = findField(fpCls, "oplusBiometricAuthController");
            final Field immersedField = findField(bioCls, "isImmersed");
            if (bioField == null || immersedField == null) {
                cfg.note("hook fpModeForCustom skipped: field missing");
                return;
            }

            XposedCompat.findAndHookMethod(fpCls, "fpModeForCustom", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!cfg.isImmersiveFpEnabled()) {
                        return;
                    }
                    try {
                        Object bio = bioField.get(param.thisObject);
                        if (bio == null) {
                            return;
                        }
                        sBioAuthController = bio;
                        if (immersedField.getBoolean(bio)) {
                            immersedField.setBoolean(bio, false);
                            param.setObjectExtra("restoredBio", bio);
                        }
                    } catch (Throwable ignored) {
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object bio = param.getObjectExtra("restoredBio");
                    if (bio == null) {
                        return;
                    }
                    try {
                        immersedField.setBoolean(bio, true);
                    } catch (Throwable ignored) {
                    }
                }
            });
            cfg.note("hook OnScreenFpControl.fpModeForCustom OK");
        } catch (Throwable t) {
            cfg.note("hook OnScreenFpControl.fpModeForCustom failed: " + t);
        }
    }

    private static void hookImmersiveTouchHandler(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            XposedCompat.findAndHookMethod(
                    CLS_IMMERSIVE_TOUCH,
                    cl,
                    "onTouchEvent",
                    MotionEvent.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!cfg.isImmersiveFpEnabled()) {
                                return;
                            }
                            Object arg0 = param.args.length > 0 ? param.args[0] : null;
                            if (!(arg0 instanceof MotionEvent)) {
                                return;
                            }
                            MotionEvent ev = (MotionEvent) arg0;
                            int action = ev.getActionMasked();
                            if (action == MotionEvent.ACTION_DOWN) {
                                IGNORING_FP_TOUCH.remove(param.thisObject);
                                Object bio = sBioAuthController;
                                if (bio != null && isFpVisibleAndHit(bio, ev)) {
                                    IGNORING_FP_TOUCH.put(param.thisObject, Boolean.TRUE);
                                    param.setResult(Boolean.FALSE);
                                }
                            } else if (Boolean.TRUE.equals(IGNORING_FP_TOUCH.get(param.thisObject))) {
                                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                                    IGNORING_FP_TOUCH.remove(param.thisObject);
                                }
                                param.setResult(Boolean.FALSE);
                            }
                        }
                    }
            );
            cfg.note("hook OplusImmersiveTouchHandler OK");
        } catch (Throwable t) {
            cfg.note("hook OplusImmersiveTouchHandler failed: " + t);
        }
    }

    private static void hookPendingFpShow(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> bioCls = XposedCompat.findClassIfExists(CLS_BIO_CTRL, cl);
            if (bioCls == null) {
                return;
            }
            XC_MethodHook clearTrueHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (cfg.isImmersiveFpEnabled()
                            && param.args.length > 0
                            && Boolean.TRUE.equals(param.args[0])) {
                        param.args[0] = Boolean.FALSE;
                    }
                }
            };
            int hooked = 0;
            for (String mName : new String[]{"setPendingFpShow", "setPendingFpState"}) {
                Method m = findMethod(bioCls, mName, boolean.class);
                if (m != null) {
                    XposedCompat.hookMethod(m, clearTrueHook);
                    hooked++;
                }
            }
            cfg.note("hook pendingFpShow OK (" + hooked + ")");
        } catch (Throwable t) {
            cfg.note("hook pendingFpShow failed: " + t);
        }
    }

    private static void hookPanoramaImmersedGuard(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> runnableCls = XposedCompat.findClassIfExists(CLS_UPDATE_ALPHA_RUNNABLE, cl);
            if (runnableCls == null) {
                cfg.note("hook PanoramaImmersedGuard skipped: class missing");
                return;
            }
            Method runMethod = XposedCompat.findDeclaredMethod(runnableCls, "run");
            if (runMethod == null) {
                cfg.note("hook PanoramaImmersedGuard skipped: method missing");
                return;
            }
            sThis0Field = findField(runnableCls, "this$0");
            XposedCompat.hookMethod(runMethod, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!cfg.isImmersiveFpEnabled() || param.thisObject == null) {
                        return;
                    }
                    try {
                        Field fThis0 = sThis0Field;
                        if (fThis0 == null || !fThis0.getDeclaringClass().isInstance(param.thisObject)) {
                            fThis0 = findField(param.thisObject.getClass(), "this$0");
                            sThis0Field = fThis0;
                        }
                        Object uiMech = fThis0 != null ? fThis0.get(param.thisObject) : null;
                        if (uiMech == null) {
                            return;
                        }
                        Field fLazy = sExtLazyField;
                        if (fLazy == null || !fLazy.getDeclaringClass().isInstance(uiMech)) {
                            fLazy = findField(uiMech.getClass(), "lockScreenExtStateControllerLazy");
                            sExtLazyField = fLazy;
                        }
                        Object lazy = fLazy != null ? fLazy.get(uiMech) : null;
                        if (lazy == null) {
                            return;
                        }
                        Method mGet = sLazyGetM;
                        if (mGet == null || !mGet.getDeclaringClass().isInstance(lazy)) {
                            mGet = findMethod(lazy.getClass(), "get");
                            sLazyGetM = mGet;
                        }
                        Object extCtrl = mGet != null ? mGet.invoke(lazy) : null;
                        if (extCtrl == null) {
                            return;
                        }
                        Method mEv = sGetExtStateEventM;
                        if (mEv == null || !mEv.getDeclaringClass().isInstance(extCtrl)) {
                            mEv = findMethod(extCtrl.getClass(), "getExtStateEvent");
                            sGetExtStateEventM = mEv;
                        }
                        Object flow = mEv != null ? mEv.invoke(extCtrl) : null;
                        if (flow == null) {
                            return;
                        }
                        Method mVal = sGetValueM;
                        if (mVal == null || !mVal.getDeclaringClass().isInstance(flow)) {
                            mVal = findMethod(flow.getClass(), "getValue");
                            sGetValueM = mVal;
                        }
                        Object event = mVal != null ? mVal.invoke(flow) : null;
                        if (event == null) {
                            return;
                        }
                        Field fState = sNewStateField;
                        if (fState == null || !fState.getDeclaringClass().isInstance(event)) {
                            fState = findField(event.getClass(), "newState");
                            sNewStateField = fState;
                        }
                        if (fState != null && fState.getInt(event) == 2) {
                            fState.setInt(event, 0);
                            param.setObjectExtra("restoredExtEvent", event);
                        }
                    } catch (Throwable ignored) {
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object event = param.getObjectExtra("restoredExtEvent");
                    if (event == null) {
                        return;
                    }
                    try {
                        Field fState = sNewStateField;
                        if (fState != null) {
                            fState.setInt(event, 2);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
            cfg.note("hook PanoramaImmersedGuard OK");
        } catch (Throwable t) {
            cfg.note("hook PanoramaImmersedGuard failed: " + t);
        }
    }

    private static void hookBiometricExtStateCollector(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            Class<?> collectorCls = XposedCompat.findClassIfExists(CLS_EXT_STATE_COLLECTOR, cl);
            if (collectorCls == null) {
                cfg.note("hook BiometricExtStateCollector skipped: class missing");
                return;
            }
            Class<?> unitCls = XposedCompat.findClassIfExists("kotlin.Unit", cl);
            final Object unitInstance = unitCls != null ? unitCls.getField("INSTANCE").get(null) : null;
            XposedCompat.hookAllMethods(collectorCls, "emit", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!cfg.isImmersiveFpEnabled() || param.args.length == 0 || param.args[0] == null) {
                        return;
                    }
                    try {
                        Object arg0 = param.args[0];
                        Method m = sGetNewStateM;
                        if (m == null || !m.getDeclaringClass().isInstance(arg0)) {
                            m = arg0.getClass().getMethod("getNewState");
                            sGetNewStateM = m;
                        }
                        Object state = m.invoke(arg0);
                        if (state instanceof Integer && ((Integer) state) == 2 && unitInstance != null) {
                            param.setResult(unitInstance);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
            cfg.note("hook BiometricExtStateCollector OK");
        } catch (Throwable t) {
            cfg.note("hook BiometricExtStateCollector failed: " + t);
        }
    }

    private static boolean isFpVisibleAndHit(Object bio, MotionEvent ev) {
        try {
            Method visM = sIsFpIconVisibleMethod;
            Method hitM = sIsInFingerViewAreaMethod;
            if (visM == null || hitM == null) {
                return false;
            }
            Object vis = visM.invoke(bio);
            if (!(vis instanceof Boolean) || !((Boolean) vis)) {
                return false;
            }
            Object hit = hitM.invoke(bio, Math.round(ev.getRawX()), Math.round(ev.getRawY()));
            return hit instanceof Boolean && (Boolean) hit;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Field findField(Class<?> cls, String name) {
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                cls = cls.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... params) {
        while (cls != null) {
            try {
                Method m = cls.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                cls = cls.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }
}
