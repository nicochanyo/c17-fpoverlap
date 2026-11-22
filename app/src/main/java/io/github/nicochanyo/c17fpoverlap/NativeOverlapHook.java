package io.github.nicochanyo.c17fpoverlap;

import io.github.nicochanyo.c17fpoverlap.XposedCompat.MethodHookParam;
import io.github.nicochanyo.c17fpoverlap.XposedCompat.XC_MethodHook;

public final class NativeOverlapHook {
    private static final String BIOMETRIC_CTRL = "com.oplus.systemui.biometrics.OplusBiometricAuthController";

    private NativeOverlapHook() {
    }

    public static void install(ClassLoader cl, final ConfigStore.HookConfig cfg) {
        try {
            XposedCompat.findAndHookMethod(
                    BIOMETRIC_CTRL,
                    cl,
                    "isNeedNotificationOverlayFingerPrint",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (Boolean.FALSE.equals(CapsulePositionHook.hasFp)) {
                                return;
                            }
                            if (cfg.isAlignEnabled()) {
                                param.setResult(Boolean.TRUE);
                            }
                        }
                    }
            );
            cfg.note("hook isNeedNotificationOverlayFingerPrint OK");
        } catch (Throwable t) {
            cfg.note("hook isNeedNotificationOverlayFingerPrint failed: " + t);
        }
    }
}
