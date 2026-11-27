package io.github.nicochanyo.c17fpoverlap;

import io.github.libxposed.api.XposedModule;

/**
 * LibXposed API 102 module entry point for C17短焦鎖屏優化.
 */
public final class FpOverlapModule extends XposedModule {
    private static final String TARGET_SYSTEMUI = "com.android.systemui";
    private boolean initialized;

    public FpOverlapModule() {
        super();
    }

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        XposedCompat.init(this);
    }

    @Override
    public synchronized void onPackageReady(PackageReadyParam param) {
        if (param == null || !TARGET_SYSTEMUI.equals(param.getPackageName()) || initialized) {
            return;
        }
        initialized = true;
        XposedCompat.init(this);

        ConfigStore.HookConfig cfg = new ConfigStore.HookConfig(this);
        cfg.note("=== C17短焦锁屏优化 1.0.1 (LibXposed API 102 / COUI) loaded in systemui ===");
        ClassLoader cl = param.getClassLoader();
        CapsulePositionHook.install(cl, cfg);
        NativeOverlapHook.install(cl, cfg);
        ImmersiveFpHook.install(cl, cfg);
        FpIconOverlapFadeHook.install(cl, cfg);
        FluidCloudHook.install(cl, cfg);
        DynamicIslandHook.install(cl, cfg);
        LockscreenBlurGuardHook.install(cl, cfg);
    }
}
