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
                                Method haveFp = param.thisObject.getClass().getMethod("haveFp");
                                Object res = haveFp.invoke(param.thisObject);
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
                                    cfg.note("updatePosition(" + orig + ") kept (fp unlock disabled)");
                                    return;
                                }

                                boolean align = cfg.isAlignEnabled();
                                int offset = cfg.getUserOffset();
                                if (!align && offset == 0) {
                                    cfg.note("updatePosition(" + orig + ") kept (system stock)");
                                    return;
                                }

                                int target;
                                if (align) {
                                    target = computeTargetMargin(cl, offset, cfg);
                                    if (target < 0) {
                                        cfg.note("updatePosition(" + orig + ") kept (calc failed)");
                                        return;
                                    }
                                } else {
                                    target = orig + offset;
                                }

                                cfg.note("updatePosition(" + orig + ") -> " + target
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
            Class<?> fpCls = cl.loadClass(FP_UTILS);
            Object fpInstance = fpCls.getField("INSTANCE").get(null);
            int fpMargin = ((Number) fpCls.getMethod("getIconMarginBottom").invoke(fpInstance)).intValue();
            int pressed = ((Number) fpCls.getMethod("getPressedIconSize").invoke(fpInstance)).intValue();

            Class<?> cfgCls = cl.loadClass(CAPSULE_CONFIG);
            Object cfgInstance = cfgCls.getField("INSTANCE").get(null);
            int item = ((Number) cfgCls.getMethod("getItemHeight").invoke(cfgInstance)).intValue();

            if (fpMargin <= 0 || pressed <= 0 || item <= 0) {
                return -1;
            }

            int fpCenter = fpMargin - (pressed / 2);
            int target = fpCenter - (item / 2) + RAISE_OFFSET + userOffset;
            cfg.note("fp margin=" + fpMargin
                    + " pressed=" + pressed
                    + " item=" + item
                    + " fpCenter=" + fpCenter
                    + " raise=" + RAISE_OFFSET
                    + " user=" + userOffset
                    + " target=" + target);
            return target;
        } catch (Throwable t) {
            cfg.note("computeTargetMargin failed: " + t);
            return -1;
        }
    }
}
