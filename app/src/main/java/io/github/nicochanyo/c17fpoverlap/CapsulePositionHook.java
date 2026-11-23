package io.github.nicochanyo.c17fpoverlap;

import java.lang.reflect.Method;

import io.github.nicochanyo.c17fpoverlap.XposedCompat.MethodHookParam;
import io.github.nicochanyo.c17fpoverlap.XposedCompat.XC_MethodHook;

public final class CapsulePositionHook {
    private static final String INTERACTOR = "com.oplus.systemui.notification.lockscreen.OplusNotificationCapsuleInteractorImpl";
    private static final String STYLE_CTRL = "com.oplus.systemui.keyguard.clockstyle.OplusKeyguardStyleBaseUIControllerImpl";
    private static final String FP_UTILS = "com.oplus.systemui.biometrics.finger.KeyguardFingerprintUtils";
    private static final String CAPSULE_CONFIG = "com.oplus.systemui.notification.lockscreen.config.CapsuleConfig";
    private static final int RAISE_OFFSET = 96;

    public static volatile Boolean hasFp = null;

    private static volatile Method sHaveFpM;
    private static volatile Object sFpInstance;
    private static volatile Method sGetIconMarginBottomM;
    private static volatile Method sGetPressedIconSizeM;
    private static volatile Object sCapsuleCfgInstance;
    private static volatile Method sGetItemHeightM;
    private static volatile int sLastLoggedOrig = Integer.MIN_VALUE;
    private static volatile int sLastLoggedTarget = Integer.MIN_VALUE;
    private static volatile Boolean sLastLoggedHasFp;
    private static volatile int sLastLoggedMarginKey = Integer.MIN_VALUE;

    private CapsulePositionHook() {
    }

    public static void install(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        hookStyleController(cl, cfg);
        hookInteractor(cl, cfg);
    }

    private static void hookStyleController(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            XposedCompat.findAndHookMethod(
                    STYLE_CTRL,
                    cl,
                    "updateCapsulePosition",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object self = param.thisObject;
                                if (self == null) {
                                    return;
                                }
                                Method haveFp = sHaveFpM;
                                if (haveFp == null || !haveFp.getDeclaringClass().isInstance(self)) {
                                    haveFp = self.getClass().getMethod("haveFp");
                                    sHaveFpM = haveFp;
                                }
                                Object res = haveFp.invoke(self);
                                if (res instanceof Boolean) {
                                    hasFp = (Boolean) res;
                                }
                            } catch (Throwable t) {
                                cfg.note("read haveFp failed: " + t);
                            }
                        }
                    }
            );
            cfg.note("hook updateCapsulePosition (haveFp) OK");
        } catch (Throwable t) {
            cfg.note("hook updateCapsulePosition failed: " + t);
        }
    }

    private static void notePositionDecision(ConfigStore.HookConfig cfg, int orig, int target, Boolean fp, String msg) {
        if (sLastLoggedOrig != orig || sLastLoggedTarget != target
                || (fp == null ? sLastLoggedHasFp != null : !fp.equals(sLastLoggedHasFp))) {
            sLastLoggedOrig = orig;
            sLastLoggedTarget = target;
            sLastLoggedHasFp = fp;
            cfg.note(msg);
        }
    }

    private static void hookInteractor(final ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            XposedCompat.findAndHookMethod(
                    INTERACTOR,
                    cl,
                    "updatePosition",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                int orig = ((Number) param.args[0]).intValue();
                                Boolean fp = hasFp;
                                if (Boolean.FALSE.equals(fp)) {
                                    notePositionDecision(cfg, orig, orig, fp,
                                            "updatePosition(" + orig + ") kept (fp unlock disabled)");
                                    return;
                                }

                                boolean align = cfg.isAlignEnabled();
                                int offset = cfg.getUserOffset();
                                if (!align && offset == 0) {
                                    notePositionDecision(cfg, orig, orig, fp,
                                            "updatePosition(" + orig + ") kept (system stock)");
                                    return;
                                }

                                int target;
                                if (align) {
                                    target = computeTargetMargin(cl, offset, cfg);
                                    if (target < 0) {
                                        notePositionDecision(cfg, orig, orig, fp,
                                                "updatePosition(" + orig + ") kept (calc failed)");
                                        return;
                                    }
                                } else {
                                    target = orig + offset;
                                }

                                notePositionDecision(cfg, orig, target, fp,
                                        "updatePosition(" + orig + ") -> " + target
                                                + " (hasFp=" + fp
                                                + " align=" + align
                                                + " offset=" + offset + ")");
                                param.args[0] = target;
                            } catch (Throwable t) {
                                cfg.note("capsule hook error: " + t);
                            }
                        }
                    }
            );
            cfg.note("hook capsule updatePosition OK");
        } catch (Throwable t) {
            cfg.note("hook capsule updatePosition failed: " + t);
        }
    }

    private static int computeTargetMargin(ClassLoader cl, int userOffset, ConfigStore.HookConfig cfg) {
        try {
            Object fpInstance = sFpInstance;
            Method getMarginM = sGetIconMarginBottomM;
            Method getPressedM = sGetPressedIconSizeM;
            if (fpInstance == null || getMarginM == null || getPressedM == null) {
                Class<?> fpCls = cl.loadClass(FP_UTILS);
                fpInstance = fpCls.getField("INSTANCE").get(null);
                getMarginM = fpCls.getMethod("getIconMarginBottom");
                getPressedM = fpCls.getMethod("getPressedIconSize");
                sFpInstance = fpInstance;
                sGetIconMarginBottomM = getMarginM;
                sGetPressedIconSizeM = getPressedM;
            }
            int fpMargin = ((Number) getMarginM.invoke(fpInstance)).intValue();
            int pressed = ((Number) getPressedM.invoke(fpInstance)).intValue();

            Object cfgInstance = sCapsuleCfgInstance;
            Method getItemM = sGetItemHeightM;
            if (cfgInstance == null || getItemM == null) {
                Class<?> cfgCls = cl.loadClass(CAPSULE_CONFIG);
                cfgInstance = cfgCls.getField("INSTANCE").get(null);
                getItemM = cfgCls.getMethod("getItemHeight");
                sCapsuleCfgInstance = cfgInstance;
                sGetItemHeightM = getItemM;
            }
            int item = ((Number) getItemM.invoke(cfgInstance)).intValue();

            if (fpMargin <= 0 || pressed <= 0 || item <= 0) {
                return -1;
            }

            int fpCenter = fpMargin - (pressed / 2);
            int target = fpCenter - (item / 2) + RAISE_OFFSET + userOffset;
            int marginKey = ((fpMargin * 31 + pressed) * 31 + item) * 31 + target;
            if (marginKey != sLastLoggedMarginKey) {
                sLastLoggedMarginKey = marginKey;
                cfg.note("fp margin=" + fpMargin
                        + " pressed=" + pressed
                        + " item=" + item
                        + " fpCenter=" + fpCenter
                        + " raise=" + RAISE_OFFSET
                        + " user=" + userOffset
                        + " target=" + target);
            }
            return target;
        } catch (Throwable t) {
            cfg.note("computeTargetMargin failed: " + t);
            return -1;
        }
    }
}
