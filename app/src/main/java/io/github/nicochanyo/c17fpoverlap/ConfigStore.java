package io.github.nicochanyo.c17fpoverlap;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.LocaleList;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.TypedValue;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.service.XposedProvider;

public final class ConfigStore {
    public static final String PKG_NAME = "io.github.nicochanyo.c17fpoverlap";
    public static final String PREFS_NAME = "config";

    public static final String KEY_ALIGN = "align_enabled";
    public static final String KEY_OFFSET = "user_offset";
    public static final String KEY_IMMERSIVE_FP = "immersive_fp_enabled";
    public static final String KEY_HIDE_FP_ON_CAPSULE = "hide_fp_on_capsule";
    public static final String KEY_AOD_NOTIF_TRANSLUCENT = "aod_notif_translucent";
    public static final String KEY_LOCK_NOTIF_GLOW_FOLLOW_MEDIA = "lock_notif_glow_follow_media";

    public static final String KEY_FC_ENABLED = "fc_enabled";
    public static final String KEY_FC_OPACITY = "fc_opacity";
    public static final String KEY_FC_IMMERSIVE_OPACITY = "fc_immersive_opacity";
    public static final String KEY_FC_ISLAND_OPACITY = "fc_island_opacity";
    public static final String KEY_FC_BLUR = "fc_blur";
    public static final String KEY_FC_BOTTOM_GLOW = "fc_bottom_glow";
    public static final String KEY_FC_BOTTOM_GLOW_BOOST = "fc_bottom_glow_boost";
    public static final String KEY_FC_BOTTOM_GLOW_CHROMA = "fc_bottom_glow_chroma";
    public static final String KEY_FC_SINGLE_GLOW_BOOST = "fc_single_glow_boost";
    public static final String KEY_FC_SINGLE_GLOW_CHROMA = "fc_single_glow_chroma";
    public static final String KEY_FC_COLOR = "fc_color";
    public static final String KEY_FC_LAST_COLOR = "fc_last_color";
    public static final String KEY_FC_MATCH_CARD = "fc_match_card";
    public static final String KEY_FC_REMOVE_ISLAND_MASK = "fc_remove_island_mask";
    public static final String KEY_FC_REMOVE_SHADE_MASK = "fc_remove_shade_mask";
    public static final String KEY_FC_MASK_FOLLOW_MEDIA = "fc_mask_follow_media";
    public static final String KEY_FC_MASK_BRIGHTNESS = "fc_mask_brightness";
    public static final String KEY_FC_MASK_CHROMA = "fc_mask_chroma";
    public static final String KEY_FC_MASK_STRENGTH = "fc_mask_strength";
    public static final String KEY_FC_NON_MEDIA_MASK_ENABLED = "fc_non_media_mask_enabled";
    public static final String KEY_FC_NON_MEDIA_MASK_COLOR = "fc_non_media_mask_color";
    public static final String KEY_FC_NON_MEDIA_MASK_LAST_COLOR = "fc_non_media_mask_last_color";
    public static final String KEY_FC_NON_MEDIA_MASK_OPACITY = "fc_non_media_mask_opacity";
    public static final String KEY_FC_NON_MEDIA_FOLLOW_MASK_OPACITY = "fc_non_media_follow_mask_opacity";
    public static final String KEY_FC_MASK_GRAD_START = "fc_mask_grad_start";
    public static final String KEY_FC_MASK_GRAD_END = "fc_mask_grad_end";
    public static final String KEY_FC_MASK_GRAD_MID = "fc_mask_grad_mid";
    public static final String KEY_FC_MASK_GRAD_STRENGTH = "fc_mask_grad_strength";
    public static final String KEY_FC_DEBUG_MODE = "fc_debug_mode";
    public static final String KEY_FC_VER = "fc_ver";
    public static final String KEY_FC_MIGRATED = "fc_migrated";

    public static final String KEY_APP_LANG = "app_lang";
    public static final String KEY_APP_MONET = "app_monet";
    public static final String KEY_APP_DARK_MODE = "app_dark_mode";
    public static final String KEY_APP_UI_STYLE = "app_ui_style";
    public static final int UI_STYLE_CLASSIC = 0;
    public static final int UI_STYLE_LIVING_FLOW = 1;
    public static final int DARK_MODE_SYSTEM = 0;
    public static final int DARK_MODE_LIGHT = 1;
    public static final int DARK_MODE_DARK = 2;
    public static final String LANG_SYSTEM = "system";
    public static final String LANG_ZH_TW = "zh-TW";
    public static final String LANG_ZH_CN = "zh-CN";
    public static final String LANG_EN = "en";

    public static final int OFFSET_LIMIT = 150;
    public static final int OFFSET_STEP = 5;

    public static final int DEF_FC_OPACITY = 100;
    public static final int DEF_FC_IMMERSIVE_OPACITY = 100;
    public static final int DEF_FC_ISLAND_OPACITY = 45;
    public static final int DEF_FC_BLUR = 100;
    public static final int MAX_FC_BLUR = 300;
    public static final int DEF_FC_BOTTOM_GLOW = 0;
    public static final int MAX_FC_BOTTOM_GLOW = 200;
    public static final int DEF_FC_BOTTOM_GLOW_CHROMA = 0;
    public static final int MAX_FC_BOTTOM_GLOW_CHROMA = 200;
    public static final int DEF_FC_MASK_BRIGHTNESS = 35;
    public static final int DEF_FC_MASK_CHROMA = 70;
    public static final int DEF_FC_MASK_STRENGTH = 100;
    public static final int MAX_FC_MASK_STRENGTH = 200;
    public static final int DEF_FC_NON_MEDIA_MASK_OPACITY = 100;
    public static final int DEF_FC_NON_MEDIA_FOLLOW_MASK_OPACITY = 100;
    public static final int DEF_FC_MASK_GRAD_START = 0;
    public static final int DEF_FC_MASK_GRAD_END = 80;
    public static final int DEF_FC_MASK_GRAD_MID = 50;
    public static final int DEF_FC_MASK_GRAD_STRENGTH = 100;
    public static final int MIN_FC_MASK_GRAD_SPAN = 10;
    public static final int MIN_FC_MASK_GRAD_MID = 15;
    public static final int MAX_FC_MASK_GRAD_MID = 85;
    public static final int SMOOTH_GRAD_SAMPLES = 16;
    public static final int DEF_NON_MEDIA_MASK_COLOR = 0xFF4FC3F7;
    public static final int DEF_FC_COLOR = 0;
    public static final int DEF_CUSTOM_COLOR = 0xFF4FC3F7;

    private static final int LOG_LIMIT = 160;
    private static final long REMOTE_RETRY_INTERVAL_MS = 2000L;
    private static final long RELOAD_THROTTLE_MS = 1000L;

    private ConfigStore() {
    }

    public static final class SettingsData {
        public boolean align;
        public int offset;
        public boolean immersiveFp;
        public boolean hideFpOnCapsule;
        public boolean aodNotifTranslucent;
        public boolean lockNotifGlowFollowMedia;
        public boolean fcEnabled;
        public int fcOpacity;
        public int fcImmersiveOpacity;
        public int fcIslandOpacity;
        public int fcBlur;
        public int fcBottomGlow;
        public int fcBottomGlowChroma;
        public int fcSingleGlow;
        public int fcSingleGlowChroma;
        public int fcColor;
        public int fcLastColor;
        public boolean fcMatchCard;
        public boolean fcRemoveIslandMask;
        public boolean fcRemoveShadeMask;
        public boolean fcMaskFollowMedia;
        public int fcMaskBrightness;
        public int fcMaskChroma;
        public int fcMaskStrength;
        public boolean fcNonMediaMaskEnabled;
        public int fcNonMediaMaskColor;
        public int fcNonMediaMaskLastColor;
        public int fcNonMediaMaskOpacity;
        public int fcNonMediaFollowMaskOpacity;
        public int fcMaskGradStart;
        public int fcMaskGradEnd;
        public int fcMaskGradMid;
        public int fcMaskGradStrength;
        public boolean fcDebugMode;
        public int fcVer;

        public SettingsData copy() {
            SettingsData s = new SettingsData();
            s.align = align;
            s.offset = offset;
            s.immersiveFp = immersiveFp;
            s.hideFpOnCapsule = hideFpOnCapsule;
            s.aodNotifTranslucent = aodNotifTranslucent;
            s.lockNotifGlowFollowMedia = lockNotifGlowFollowMedia;
            s.fcEnabled = fcEnabled;
            s.fcOpacity = fcOpacity;
            s.fcImmersiveOpacity = fcImmersiveOpacity;
            s.fcIslandOpacity = fcIslandOpacity;
            s.fcBlur = fcBlur;
            s.fcBottomGlow = fcBottomGlow;
            s.fcBottomGlowChroma = fcBottomGlowChroma;
            s.fcSingleGlow = fcSingleGlow;
            s.fcSingleGlowChroma = fcSingleGlowChroma;
            s.fcColor = fcColor;
            s.fcLastColor = fcLastColor;
            s.fcMatchCard = fcMatchCard;
            s.fcRemoveIslandMask = fcRemoveIslandMask;
            s.fcRemoveShadeMask = fcRemoveShadeMask;
            s.fcMaskFollowMedia = fcMaskFollowMedia;
            s.fcMaskBrightness = fcMaskBrightness;
            s.fcMaskChroma = fcMaskChroma;
            s.fcMaskStrength = fcMaskStrength;
            s.fcNonMediaMaskEnabled = fcNonMediaMaskEnabled;
            s.fcNonMediaMaskColor = fcNonMediaMaskColor;
            s.fcNonMediaMaskLastColor = fcNonMediaMaskLastColor;
            s.fcNonMediaMaskOpacity = fcNonMediaMaskOpacity;
            s.fcNonMediaFollowMaskOpacity = fcNonMediaFollowMaskOpacity;
            s.fcMaskGradStart = fcMaskGradStart;
            s.fcMaskGradEnd = fcMaskGradEnd;
            s.fcMaskGradMid = fcMaskGradMid;
            s.fcMaskGradStrength = fcMaskGradStrength;
            s.fcDebugMode = fcDebugMode;
            s.fcVer = fcVer;
            return s;
        }
    }

    public static int clamp(int value) {
        if (value < -OFFSET_LIMIT) {
            return -OFFSET_LIMIT;
        }
        if (value > OFFSET_LIMIT) {
            return OFFSET_LIMIT;
        }
        return value;
    }

    public static int clampOpacity(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 100) {
            return 100;
        }
        return value;
    }

    public static int clampMaskPercent(int value) {
        return clampOpacity(value);
    }

    public static int clampMaskStrength(int value) {
        return Math.max(0, Math.min(MAX_FC_MASK_STRENGTH, value));
    }

    public static int clampMaskGradMid(int value) {
        return Math.max(MIN_FC_MASK_GRAD_MID, Math.min(MAX_FC_MASK_GRAD_MID, value));
    }

    /**
     * Enforces foolproof gradient mask template bounds:
     * start in [0, 90], end in [10, 100], end >= start + 10, mid in [15, 85].
     */
    public static int[] normalizeGradientPct(int startPct, int endPct, int midPct) {
        int s = Math.max(0, Math.min(100 - MIN_FC_MASK_GRAD_SPAN, startPct));
        int e = Math.max(MIN_FC_MASK_GRAD_SPAN, Math.min(100, endPct));
        if (e < s + MIN_FC_MASK_GRAD_SPAN) {
            if (s + MIN_FC_MASK_GRAD_SPAN <= 100) {
                e = s + MIN_FC_MASK_GRAD_SPAN;
            } else {
                s = 100 - MIN_FC_MASK_GRAD_SPAN;
                e = 100;
            }
        }
        int m = clampMaskGradMid(midPct);
        return new int[]{s, e, m};
    }

    /**
     * Returns normalized shader/sampler parameters [sPos, hMid, ePos] where
     * ePos - sPos >= 0.10 and hMid is in [0.15, 0.85].
     */
    public static float[] computeSmoothGradientParams(int startPct, int endPct, int midPct) {
        int[] norm = normalizeGradientPct(startPct, endPct, midPct);
        return new float[]{norm[0] / 100.0f, norm[2] / 100.0f, norm[1] / 100.0f};
    }

    /**
     * Evaluates a C2-smooth S-curve from 1.0 (at u=0) down to 0.0 (at u=1),
     * crossing exactly 0.5 at u = hMid via Schlick's rational bias + quintic smootherstep.
     */
    public static float evaluateSmoothGradientFactor(float u, float hMid) {
        float uc = Math.max(0.0f, Math.min(1.0f, u));
        float h = Math.max(MIN_FC_MASK_GRAD_MID / 100.0f, Math.min(MAX_FC_MASK_GRAD_MID / 100.0f, hMid));
        float denom = uc * (1.0f - 2.0f * h) + h;
        float w = (uc * (1.0f - h)) / Math.max(denom, 0.001f);
        float w2 = w * w;
        float smooth = w2 * w * (w * (6.0f * w - 15.0f) + 10.0f);
        return Math.max(0.0f, Math.min(1.0f, 1.0f - smooth));
    }

    public static final class SmoothGradientSpec {
        public final int[] colors;
        public final float[] positions;

        SmoothGradientSpec(int[] colors, float[] positions) {
            this.colors = colors;
            this.positions = positions;
        }
    }

    /**
     * Builds multi-stop colors and positions for LinearGradient matching the AGSL mask shader,
     * preserving constant RGB on transparent stops to prevent dark/white fringe artifacts.
     */
    public static SmoothGradientSpec buildSmoothGradient(int rgb, int peakAlpha,
                                                           int startPct, int endPct, int midPct) {
        float[] params = computeSmoothGradientParams(startPct, endPct, midPct);
        float sPos = params[0];
        float hMid = params[1];
        float ePos = params[2];
        int baseRgb = rgb & 0x00FFFFFF;
        int peakA = Math.max(0, Math.min(255, peakAlpha));
        boolean needLead = sPos > 0.001f;
        boolean needTrail = ePos < 0.999f;
        int samples = SMOOTH_GRAD_SAMPLES + 1;
        int total = samples + (needLead ? 1 : 0) + (needTrail ? 1 : 0);
        int[] colors = new int[total];
        float[] positions = new float[total];
        int idx = 0;
        if (needLead) {
            positions[idx] = 0.0f;
            colors[idx] = (peakA << 24) | baseRgb;
            idx++;
        }
        float span = ePos - sPos;
        for (int i = 0; i <= SMOOTH_GRAD_SAMPLES; i++) {
            float u = i / (float) SMOOTH_GRAD_SAMPLES;
            float fac = evaluateSmoothGradientFactor(u, hMid);
            int a = Math.max(0, Math.min(255, Math.round(peakA * fac)));
            positions[idx] = sPos + u * span;
            colors[idx] = (a << 24) | baseRgb;
            idx++;
        }
        if (needTrail) {
            positions[idx] = 1.0f;
            colors[idx] = baseRgb;
        }
        return new SmoothGradientSpec(colors, positions);
    }

    public static int normalizePickerColor(int raw) {
        int c = normalizeColor(raw);
        return c != 0 ? c : DEF_CUSTOM_COLOR;
    }

    public static int clampBlur(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > MAX_FC_BLUR) {
            return MAX_FC_BLUR;
        }
        return value;
    }

    public static int clampBottomGlow(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > MAX_FC_BOTTOM_GLOW) {
            return MAX_FC_BOTTOM_GLOW;
        }
        return value;
    }

    public static int clampBottomGlowChroma(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > MAX_FC_BOTTOM_GLOW_CHROMA) {
            return MAX_FC_BOTTOM_GLOW_CHROMA;
        }
        return value;
    }

    public static int normalizeColor(int raw) {
        if (raw == 0) {
            return 0;
        }
        return 0xFF000000 | (raw & 0x00FFFFFF);
    }

    public static int parseIntSafe(String text, int defValue) {
        if (text == null) {
            return defValue;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (Throwable ignored) {
            return defValue;
        }
    }

    public static String normalizeLanguage(String raw) {
        if (LANG_ZH_TW.equals(raw)) {
            return LANG_ZH_TW;
        }
        if (LANG_ZH_CN.equals(raw)) {
            return LANG_ZH_CN;
        }
        if (LANG_EN.equals(raw)) {
            return LANG_EN;
        }
        return LANG_SYSTEM;
    }

    public static String getAppLanguage(Context ctx) {
        if (ctx == null) {
            return LANG_SYSTEM;
        }
        try {
            SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return normalizeLanguage(prefs.getString(KEY_APP_LANG, LANG_SYSTEM));
        } catch (Throwable ignored) {
            return LANG_SYSTEM;
        }
    }

    public static String getEffectiveAppLanguage(Context ctx) {
        String pref = getAppLanguage(ctx);
        return LANG_SYSTEM.equals(pref) ? detectSystemLanguage() : pref;
    }

    /**
     * Reads the true OS system locale list from Resources.getSystem() (unaffected by
     * process-level Locale.setDefault() overrides) and picks the first supported language.
     * Falls back to English for non-Chinese locales.
     */
    public static String detectSystemLanguage() {
        try {
            Resources sysRes = Resources.getSystem();
            if (sysRes != null && sysRes.getConfiguration() != null) {
                if (Build.VERSION.SDK_INT >= 24) {
                    LocaleList locales = sysRes.getConfiguration().getLocales();
                    if (locales != null && !locales.isEmpty()) {
                        for (int i = 0; i < locales.size(); i++) {
                            String matched = matchSupportedLocale(locales.get(i));
                            if (matched != null) {
                                return matched;
                            }
                        }
                        return LANG_EN;
                    }
                } else {
                    @SuppressWarnings("deprecation")
                    Locale legacy = sysRes.getConfiguration().locale;
                    if (legacy != null) {
                        return detectLanguageFromLocale(legacy);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return detectLanguageFromLocale(Locale.getDefault());
    }

    public static String matchSupportedLocale(Locale loc) {
        if (loc == null) {
            return null;
        }
        String language = loc.getLanguage();
        if (language == null || language.isEmpty()) {
            return null;
        }
        String langLower = language.toLowerCase(Locale.US);
        if ("zh".equals(langLower)) {
            String script = loc.getScript();
            if ("Hant".equalsIgnoreCase(script)) {
                return LANG_ZH_TW;
            }
            if ("Hans".equalsIgnoreCase(script)) {
                return LANG_ZH_CN;
            }
            String country = loc.getCountry();
            if ("TW".equalsIgnoreCase(country)
                    || "HK".equalsIgnoreCase(country)
                    || "MO".equalsIgnoreCase(country)) {
                return LANG_ZH_TW;
            }
            return LANG_ZH_CN;
        }
        if ("en".equals(langLower)) {
            return LANG_EN;
        }
        return null;
    }

    public static String detectLanguageFromLocale(Locale loc) {
        String matched = matchSupportedLocale(loc);
        return matched != null ? matched : LANG_EN;
    }

    public static void setAppLanguage(Context ctx, String lang) {
        if (ctx == null) {
            return;
        }
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_APP_LANG, normalizeLanguage(lang)).commit();
    }

    public static boolean isMonetEnabled(Context ctx) {
        if (ctx == null) {
            return false;
        }
        try {
            SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getBoolean(KEY_APP_MONET, false);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void setMonetEnabled(Context ctx, boolean enabled) {
        if (ctx == null) {
            return;
        }
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_APP_MONET, enabled).commit();
    }

    public static int normalizeDarkMode(int raw) {
        if (raw == DARK_MODE_LIGHT) {
            return DARK_MODE_LIGHT;
        }
        if (raw == DARK_MODE_DARK) {
            return DARK_MODE_DARK;
        }
        return DARK_MODE_SYSTEM;
    }

    public static int getAppDarkMode(Context ctx) {
        if (ctx == null) {
            return DARK_MODE_SYSTEM;
        }
        try {
            SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return normalizeDarkMode(prefs.getInt(KEY_APP_DARK_MODE, DARK_MODE_SYSTEM));
        } catch (Throwable ignored) {
            return DARK_MODE_SYSTEM;
        }
    }

    public static void setAppDarkMode(Context ctx, int mode) {
        if (ctx == null) {
            return;
        }
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_APP_DARK_MODE, normalizeDarkMode(mode)).commit();
    }

    private static final String[] COLOROS_VERSION_PROPS = {
            "ro.build.version.oplusrom",
            "ro.build.version.oplusrom.display",
            "ro.build.version.oplusrom.confidential",
            "ro.rom.different.version"
    };
    private static volatile int sCachedColorOsMajorVersion = Integer.MIN_VALUE;
    private static volatile int sCachedDefaultUiStyle = -1;

    /**
     * Detects the ColorOS major version from Oplus ROM system properties,
     * returning {@code -1} if running on a non-ColorOS or unreadable build.
     */
    public static int getColorOsMajorVersion() {
        int cached = sCachedColorOsMajorVersion;
        if (cached != Integer.MIN_VALUE) {
            return cached;
        }
        int detected = -1;
        try {
            Class<?> spCls = Class.forName("android.os.SystemProperties");
            Method getM = spCls.getMethod("get", String.class, String.class);
            for (String key : COLOROS_VERSION_PROPS) {
                Object rawObj = getM.invoke(null, key, "");
                String raw = (rawObj instanceof String) ? (String) rawObj : "";
                int parsed = parseMajorVersion(raw);
                if (parsed > 0) {
                    detected = parsed;
                    break;
                }
            }
        } catch (Throwable ignored) {
        }
        sCachedColorOsMajorVersion = detected;
        return detected;
    }

    static int parseMajorVersion(String raw) {
        if (raw == null) {
            return -1;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return -1;
        }
        int dotted = scanVersionToken(s, true);
        if (dotted > 0) {
            return dotted;
        }
        return scanVersionToken(s, false);
    }

    private static int scanVersionToken(String s, boolean requireDot) {
        int len = s.length();
        int i = 0;
        while (i < len) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                i++;
                continue;
            }
            int j = i;
            int value = 0;
            while (j < len) {
                char d = s.charAt(j);
                if (d < '0' || d > '9') {
                    break;
                }
                if (value <= 100) {
                    value = value * 10 + (d - '0');
                }
                j++;
            }
            boolean validBoundary = requireDot
                    ? (j < len && s.charAt(j) == '.')
                    : (j == len || Character.isWhitespace(s.charAt(j)));
            if (validBoundary && value >= 1 && value <= 99) {
                return value;
            }
            i = j;
        }
        return -1;
    }

    /**
     * Resolves the OS-appropriate default UI style: Living Flow on ColorOS 17+
     * (or Android 17+ fallback) and Classic on ColorOS 16 and below.
     */
    public static int getDefaultUiStyle() {
        int cached = sCachedDefaultUiStyle;
        if (cached != -1) {
            return cached;
        }
        int major = getColorOsMajorVersion();
        int resolved;
        if (major > 0) {
            resolved = major >= 17 ? UI_STYLE_LIVING_FLOW : UI_STYLE_CLASSIC;
        } else {
            resolved = Build.VERSION.SDK_INT >= 37 ? UI_STYLE_LIVING_FLOW : UI_STYLE_CLASSIC;
        }
        sCachedDefaultUiStyle = resolved;
        return resolved;
    }

    public static int normalizeUiStyle(int raw) {
        if (raw == UI_STYLE_CLASSIC) {
            return UI_STYLE_CLASSIC;
        }
        if (raw == UI_STYLE_LIVING_FLOW) {
            return UI_STYLE_LIVING_FLOW;
        }
        return getDefaultUiStyle();
    }

    public static int getAppUiStyle(Context ctx) {
        int defaultStyle = getDefaultUiStyle();
        if (ctx == null) {
            return defaultStyle;
        }
        try {
            SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            if (!prefs.contains(KEY_APP_UI_STYLE)) {
                return defaultStyle;
            }
            return normalizeUiStyle(prefs.getInt(KEY_APP_UI_STYLE, defaultStyle));
        } catch (Throwable ignored) {
            return defaultStyle;
        }
    }

    public static void setAppUiStyle(Context ctx, int style) {
        if (ctx == null) {
            return;
        }
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_APP_UI_STYLE, normalizeUiStyle(style)).commit();
    }

    public static boolean isLivingFlowUi(Context ctx) {
        return getAppUiStyle(ctx) == UI_STYLE_LIVING_FLOW;
    }

    public static boolean isDarkThemeActive(Context ctx) {
        int mode = getAppDarkMode(ctx);
        if (mode == DARK_MODE_DARK) {
            return true;
        }
        if (mode == DARK_MODE_LIGHT) {
            return false;
        }
        return ctx != null
                && (ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    public static int resolveThemeColor(Context ctx, int attrResId, int fallbackColorResId) {
        if (ctx != null) {
            TypedValue tv = new TypedValue();
            if (ctx.getTheme() != null && ctx.getTheme().resolveAttribute(attrResId, tv, true)) {
                if (tv.resourceId != 0) {
                    try {
                        return ctx.getColor(tv.resourceId);
                    } catch (Throwable ignored) {
                    }
                }
                if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    return tv.data;
                }
            }
        }
        return ctx != null ? ctx.getColor(fallbackColorResId) : 0;
    }

    public static final class AppPrefs {
        public final SharedPreferences prefs;
        public final boolean xspActive;
        public final boolean hadLocalKeys;

        AppPrefs(SharedPreferences prefs, boolean xspActive, boolean hadLocalKeys) {
            this.prefs = prefs;
            this.xspActive = xspActive;
            this.hadLocalKeys = hadLocalKeys;
        }
    }

    public static AppPrefs openAppPrefs(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        migrateFluidCloudSettings(ctx, prefs);
        boolean hadLocal = hasAnySetting(prefs);
        boolean serviceActive = XposedProvider.isConnected();
        return new AppPrefs(prefs, serviceActive, hadLocal);
    }

    public static boolean hasAnySetting(SharedPreferences prefs) {
        return prefs != null && (prefs.contains(KEY_ALIGN)
                || prefs.contains(KEY_OFFSET)
                || prefs.contains(KEY_IMMERSIVE_FP)
                || prefs.contains(KEY_HIDE_FP_ON_CAPSULE)
                || prefs.contains(KEY_AOD_NOTIF_TRANSLUCENT)
                || prefs.contains(KEY_LOCK_NOTIF_GLOW_FOLLOW_MEDIA)
                || prefs.contains(KEY_FC_ENABLED));
    }

    public static boolean hasAnySetting(Map<String, Object> map) {
        return map != null && (map.containsKey(KEY_ALIGN)
                || map.containsKey(KEY_OFFSET)
                || map.containsKey(KEY_IMMERSIVE_FP)
                || map.containsKey(KEY_HIDE_FP_ON_CAPSULE)
                || map.containsKey(KEY_AOD_NOTIF_TRANSLUCENT)
                || map.containsKey(KEY_LOCK_NOTIF_GLOW_FOLLOW_MEDIA)
                || map.containsKey(KEY_FC_ENABLED));
    }

    private static void migrateFluidCloudSettings(Context ctx, SharedPreferences prefs) {
        if (prefs.getBoolean(KEY_FC_MIGRATED, false)) {
            return;
        }
        try {
            ContentResolver cr = ctx.getContentResolver();
            final int missing = Integer.MIN_VALUE;
            int fctEn = Settings.Global.getInt(cr, "fct_enabled", missing);
            int fctOp = Settings.Global.getInt(cr, "fct_opacity", missing);
            int fctBl = Settings.Global.getInt(cr, "fct_blur", missing);
            int fctCl = Settings.Global.getInt(cr, "fct_color", missing);
            int fctMc = Settings.Global.getInt(cr, "fct_match_card", missing);

            SharedPreferences.Editor ed = prefs.edit().putBoolean(KEY_FC_MIGRATED, true);
            boolean any = false;
            if (fctEn != missing) {
                ed.putBoolean(KEY_FC_ENABLED, fctEn != 0);
                any = true;
            }
            if (fctOp != missing) {
                ed.putInt(KEY_FC_OPACITY, clampOpacity(fctOp));
                any = true;
            }
            if (fctBl != missing) {
                ed.putInt(KEY_FC_BLUR, clampBlur(fctBl));
                any = true;
            }
            if (fctCl != missing) {
                int nc = normalizeColor(fctCl);
                ed.putInt(KEY_FC_COLOR, nc);
                if (nc != 0) {
                    ed.putInt(KEY_FC_LAST_COLOR, nc);
                }
                any = true;
            }
            if (fctMc != missing) {
                ed.putBoolean(KEY_FC_MATCH_CARD, fctMc != 0);
                ed.putBoolean(KEY_FC_REMOVE_ISLAND_MASK, fctMc != 0);
                any = true;
            }
            if (any) {
                ed.putInt(KEY_FC_VER, prefs.getInt(KEY_FC_VER, 0) + 1);
            }
            ed.apply();
        } catch (Throwable ignored) {
            prefs.edit().putBoolean(KEY_FC_MIGRATED, true).apply();
        }
    }

    public static SettingsData loadFromPrefs(SharedPreferences prefs) {
        SettingsData s = new SettingsData();
        s.align = prefs.getBoolean(KEY_ALIGN, false);
        s.offset = clamp(prefs.getInt(KEY_OFFSET, 0));
        s.immersiveFp = prefs.getBoolean(KEY_IMMERSIVE_FP, true);
        s.hideFpOnCapsule = prefs.getBoolean(KEY_HIDE_FP_ON_CAPSULE, true);
        s.aodNotifTranslucent = prefs.getBoolean(KEY_AOD_NOTIF_TRANSLUCENT, false);
        s.lockNotifGlowFollowMedia = prefs.getBoolean(KEY_LOCK_NOTIF_GLOW_FOLLOW_MEDIA, false);
        s.fcEnabled = prefs.getBoolean(KEY_FC_ENABLED, true);
        s.fcOpacity = clampOpacity(prefs.getInt(KEY_FC_OPACITY, DEF_FC_OPACITY));
        int fallbackImm = prefs.contains(KEY_FC_OPACITY) ? s.fcOpacity : DEF_FC_IMMERSIVE_OPACITY;
        s.fcImmersiveOpacity = clampOpacity(prefs.getInt(KEY_FC_IMMERSIVE_OPACITY, fallbackImm));
        int fallbackIsland = prefs.contains(KEY_FC_OPACITY) ? s.fcOpacity : DEF_FC_ISLAND_OPACITY;
        s.fcIslandOpacity = clampOpacity(prefs.getInt(KEY_FC_ISLAND_OPACITY, fallbackIsland));
        s.fcBlur = clampBlur(prefs.getInt(KEY_FC_BLUR, DEF_FC_BLUR));
        s.fcBottomGlow = readGlowBoostFromPrefs(prefs);
        s.fcBottomGlowChroma = clampBottomGlowChroma(prefs.getInt(KEY_FC_BOTTOM_GLOW_CHROMA, DEF_FC_BOTTOM_GLOW_CHROMA));
        s.fcSingleGlow = prefs.contains(KEY_FC_SINGLE_GLOW_BOOST)
                ? clampBottomGlow(prefs.getInt(KEY_FC_SINGLE_GLOW_BOOST, DEF_FC_BOTTOM_GLOW))
                : s.fcBottomGlow;
        s.fcSingleGlowChroma = prefs.contains(KEY_FC_SINGLE_GLOW_CHROMA)
                ? clampBottomGlowChroma(prefs.getInt(KEY_FC_SINGLE_GLOW_CHROMA, DEF_FC_BOTTOM_GLOW_CHROMA))
                : s.fcBottomGlowChroma;
        s.fcColor = normalizeColor(prefs.getInt(KEY_FC_COLOR, DEF_FC_COLOR));
        int last = normalizeColor(prefs.getInt(KEY_FC_LAST_COLOR, DEF_CUSTOM_COLOR));
        s.fcLastColor = last != 0 ? last : DEF_CUSTOM_COLOR;
        s.fcMatchCard = prefs.getBoolean(KEY_FC_MATCH_CARD, true);
        s.fcRemoveIslandMask = prefs.contains(KEY_FC_REMOVE_ISLAND_MASK)
                ? prefs.getBoolean(KEY_FC_REMOVE_ISLAND_MASK, true)
                : s.fcMatchCard;
        s.fcRemoveShadeMask = prefs.getBoolean(KEY_FC_REMOVE_SHADE_MASK, false);
        s.fcMaskFollowMedia = prefs.getBoolean(KEY_FC_MASK_FOLLOW_MEDIA, false);
        s.fcMaskBrightness = clampMaskPercent(prefs.getInt(KEY_FC_MASK_BRIGHTNESS, DEF_FC_MASK_BRIGHTNESS));
        s.fcMaskChroma = clampMaskPercent(prefs.getInt(KEY_FC_MASK_CHROMA, DEF_FC_MASK_CHROMA));
        s.fcMaskStrength = clampMaskStrength(prefs.getInt(KEY_FC_MASK_STRENGTH, DEF_FC_MASK_STRENGTH));
        int rawNonMedia = prefs.getInt(KEY_FC_NON_MEDIA_MASK_COLOR, DEF_NON_MEDIA_MASK_COLOR);
        s.fcNonMediaMaskEnabled = prefs.getBoolean(KEY_FC_NON_MEDIA_MASK_ENABLED, false);
        s.fcNonMediaMaskColor = normalizePickerColor(rawNonMedia);
        int lastNonMedia = prefs.getInt(KEY_FC_NON_MEDIA_MASK_LAST_COLOR, DEF_NON_MEDIA_MASK_COLOR);
        s.fcNonMediaMaskLastColor = normalizePickerColor(lastNonMedia);
        s.fcNonMediaMaskOpacity = clampMaskStrength(
                prefs.getInt(KEY_FC_NON_MEDIA_MASK_OPACITY, DEF_FC_NON_MEDIA_MASK_OPACITY));
        s.fcNonMediaFollowMaskOpacity = prefs.contains(KEY_FC_NON_MEDIA_FOLLOW_MASK_OPACITY)
                ? clampMaskStrength(prefs.getInt(KEY_FC_NON_MEDIA_FOLLOW_MASK_OPACITY,
                        DEF_FC_NON_MEDIA_FOLLOW_MASK_OPACITY))
                : s.fcNonMediaMaskOpacity;
        int[] gradNorm = normalizeGradientPct(
                prefs.getInt(KEY_FC_MASK_GRAD_START, DEF_FC_MASK_GRAD_START),
                prefs.getInt(KEY_FC_MASK_GRAD_END, DEF_FC_MASK_GRAD_END),
                prefs.getInt(KEY_FC_MASK_GRAD_MID, DEF_FC_MASK_GRAD_MID));
        s.fcMaskGradStart = gradNorm[0];
        s.fcMaskGradEnd = gradNorm[1];
        s.fcMaskGradMid = gradNorm[2];
        s.fcMaskGradStrength = clampMaskStrength(
                prefs.getInt(KEY_FC_MASK_GRAD_STRENGTH, DEF_FC_MASK_GRAD_STRENGTH));
        s.fcDebugMode = prefs.getBoolean(KEY_FC_DEBUG_MODE, false);
        s.fcVer = prefs.getInt(KEY_FC_VER, 0);
        return s;
    }

    public static SettingsData createDefaults() {
        SettingsData s = new SettingsData();
        s.align = false;
        s.offset = 0;
        s.immersiveFp = true;
        s.hideFpOnCapsule = true;
        s.aodNotifTranslucent = false;
        s.lockNotifGlowFollowMedia = false;
        s.fcEnabled = true;
        s.fcOpacity = DEF_FC_OPACITY;
        s.fcImmersiveOpacity = DEF_FC_IMMERSIVE_OPACITY;
        s.fcIslandOpacity = DEF_FC_ISLAND_OPACITY;
        s.fcBlur = DEF_FC_BLUR;
        s.fcBottomGlow = DEF_FC_BOTTOM_GLOW;
        s.fcBottomGlowChroma = DEF_FC_BOTTOM_GLOW_CHROMA;
        s.fcSingleGlow = DEF_FC_BOTTOM_GLOW;
        s.fcSingleGlowChroma = DEF_FC_BOTTOM_GLOW_CHROMA;
        s.fcColor = DEF_FC_COLOR;
        s.fcLastColor = DEF_CUSTOM_COLOR;
        s.fcMatchCard = true;
        s.fcRemoveIslandMask = true;
        s.fcRemoveShadeMask = false;
        s.fcMaskFollowMedia = false;
        s.fcMaskBrightness = DEF_FC_MASK_BRIGHTNESS;
        s.fcMaskChroma = DEF_FC_MASK_CHROMA;
        s.fcMaskStrength = DEF_FC_MASK_STRENGTH;
        s.fcNonMediaMaskEnabled = false;
        s.fcNonMediaMaskColor = DEF_NON_MEDIA_MASK_COLOR;
        s.fcNonMediaMaskLastColor = DEF_NON_MEDIA_MASK_COLOR;
        s.fcNonMediaMaskOpacity = DEF_FC_NON_MEDIA_MASK_OPACITY;
        s.fcNonMediaFollowMaskOpacity = DEF_FC_NON_MEDIA_FOLLOW_MASK_OPACITY;
        s.fcMaskGradStart = DEF_FC_MASK_GRAD_START;
        s.fcMaskGradEnd = DEF_FC_MASK_GRAD_END;
        s.fcMaskGradMid = DEF_FC_MASK_GRAD_MID;
        s.fcMaskGradStrength = DEF_FC_MASK_GRAD_STRENGTH;
        s.fcDebugMode = false;
        s.fcVer = 0;
        return s;
    }

    public static SettingsData loadFromMap(Map<String, Object> map) {
        SettingsData s = createDefaults();
        if (map == null) {
            return s;
        }
        s.align = readMapBool(map, KEY_ALIGN, false);
        s.offset = clamp(readMapInt(map, KEY_OFFSET, 0));
        s.immersiveFp = readMapBool(map, KEY_IMMERSIVE_FP, true);
        s.hideFpOnCapsule = readMapBool(map, KEY_HIDE_FP_ON_CAPSULE, true);
        s.aodNotifTranslucent = readMapBool(map, KEY_AOD_NOTIF_TRANSLUCENT, false);
        s.lockNotifGlowFollowMedia = readMapBool(map, KEY_LOCK_NOTIF_GLOW_FOLLOW_MEDIA, false);
        s.fcEnabled = readMapBool(map, KEY_FC_ENABLED, true);
        s.fcOpacity = clampOpacity(readMapInt(map, KEY_FC_OPACITY, DEF_FC_OPACITY));
        int fallbackImm = map.containsKey(KEY_FC_OPACITY) ? s.fcOpacity : DEF_FC_IMMERSIVE_OPACITY;
        s.fcImmersiveOpacity = clampOpacity(readMapInt(map, KEY_FC_IMMERSIVE_OPACITY, fallbackImm));
        int fallbackIsland = map.containsKey(KEY_FC_OPACITY) ? s.fcOpacity : DEF_FC_ISLAND_OPACITY;
        s.fcIslandOpacity = clampOpacity(readMapInt(map, KEY_FC_ISLAND_OPACITY, fallbackIsland));
        s.fcBlur = clampBlur(readMapInt(map, KEY_FC_BLUR, DEF_FC_BLUR));
        s.fcBottomGlow = readGlowBoostFromMap(map);
        s.fcBottomGlowChroma = clampBottomGlowChroma(readMapInt(map, KEY_FC_BOTTOM_GLOW_CHROMA, DEF_FC_BOTTOM_GLOW_CHROMA));
        s.fcSingleGlow = map.containsKey(KEY_FC_SINGLE_GLOW_BOOST)
                ? clampBottomGlow(readMapInt(map, KEY_FC_SINGLE_GLOW_BOOST, DEF_FC_BOTTOM_GLOW))
                : s.fcBottomGlow;
        s.fcSingleGlowChroma = map.containsKey(KEY_FC_SINGLE_GLOW_CHROMA)
                ? clampBottomGlowChroma(readMapInt(map, KEY_FC_SINGLE_GLOW_CHROMA, DEF_FC_BOTTOM_GLOW_CHROMA))
                : s.fcBottomGlowChroma;
        s.fcColor = normalizeColor(readMapInt(map, KEY_FC_COLOR, DEF_FC_COLOR));
        int last = normalizeColor(readMapInt(map, KEY_FC_LAST_COLOR, DEF_CUSTOM_COLOR));
        s.fcLastColor = last != 0 ? last : DEF_CUSTOM_COLOR;
        s.fcMatchCard = readMapBool(map, KEY_FC_MATCH_CARD, true);
        s.fcRemoveIslandMask = map.containsKey(KEY_FC_REMOVE_ISLAND_MASK)
                ? readMapBool(map, KEY_FC_REMOVE_ISLAND_MASK, true)
                : s.fcMatchCard;
        s.fcRemoveShadeMask = readMapBool(map, KEY_FC_REMOVE_SHADE_MASK, false);
        s.fcMaskFollowMedia = readMapBool(map, KEY_FC_MASK_FOLLOW_MEDIA, false);
        s.fcMaskBrightness = clampMaskPercent(readMapInt(map, KEY_FC_MASK_BRIGHTNESS, DEF_FC_MASK_BRIGHTNESS));
        s.fcMaskChroma = clampMaskPercent(readMapInt(map, KEY_FC_MASK_CHROMA, DEF_FC_MASK_CHROMA));
        s.fcMaskStrength = clampMaskStrength(readMapInt(map, KEY_FC_MASK_STRENGTH, DEF_FC_MASK_STRENGTH));
        int rawNonMedia = readMapInt(map, KEY_FC_NON_MEDIA_MASK_COLOR, DEF_NON_MEDIA_MASK_COLOR);
        s.fcNonMediaMaskEnabled = readMapBool(map, KEY_FC_NON_MEDIA_MASK_ENABLED, false);
        s.fcNonMediaMaskColor = normalizePickerColor(rawNonMedia);
        int lastNonMedia = readMapInt(map, KEY_FC_NON_MEDIA_MASK_LAST_COLOR, DEF_NON_MEDIA_MASK_COLOR);
        s.fcNonMediaMaskLastColor = normalizePickerColor(lastNonMedia);
        s.fcNonMediaMaskOpacity = clampMaskStrength(
                readMapInt(map, KEY_FC_NON_MEDIA_MASK_OPACITY, DEF_FC_NON_MEDIA_MASK_OPACITY));
        s.fcNonMediaFollowMaskOpacity = map.containsKey(KEY_FC_NON_MEDIA_FOLLOW_MASK_OPACITY)
                ? clampMaskStrength(readMapInt(map, KEY_FC_NON_MEDIA_FOLLOW_MASK_OPACITY,
                        DEF_FC_NON_MEDIA_FOLLOW_MASK_OPACITY))
                : s.fcNonMediaMaskOpacity;
        int[] gradNorm = normalizeGradientPct(
                readMapInt(map, KEY_FC_MASK_GRAD_START, DEF_FC_MASK_GRAD_START),
                readMapInt(map, KEY_FC_MASK_GRAD_END, DEF_FC_MASK_GRAD_END),
                readMapInt(map, KEY_FC_MASK_GRAD_MID, DEF_FC_MASK_GRAD_MID));
        s.fcMaskGradStart = gradNorm[0];
        s.fcMaskGradEnd = gradNorm[1];
        s.fcMaskGradMid = gradNorm[2];
        s.fcMaskGradStrength = clampMaskStrength(
                readMapInt(map, KEY_FC_MASK_GRAD_STRENGTH, DEF_FC_MASK_GRAD_STRENGTH));
        s.fcDebugMode = readMapBool(map, KEY_FC_DEBUG_MODE, false);
        s.fcVer = readMapInt(map, KEY_FC_VER, 0);
        return s;
    }

    public static Map<String, Object> toMap(SettingsData s) {
        Map<String, Object> map = new HashMap<>();
        int[] gradNorm = normalizeGradientPct(s.fcMaskGradStart, s.fcMaskGradEnd, s.fcMaskGradMid);
        map.put(KEY_ALIGN, s.align);
        map.put(KEY_OFFSET, clamp(s.offset));
        map.put(KEY_IMMERSIVE_FP, s.immersiveFp);
        map.put(KEY_HIDE_FP_ON_CAPSULE, s.hideFpOnCapsule);
        map.put(KEY_AOD_NOTIF_TRANSLUCENT, s.aodNotifTranslucent);
        map.put(KEY_LOCK_NOTIF_GLOW_FOLLOW_MEDIA, s.lockNotifGlowFollowMedia);
        map.put(KEY_FC_ENABLED, s.fcEnabled);
        map.put(KEY_FC_OPACITY, clampOpacity(s.fcOpacity));
        map.put(KEY_FC_IMMERSIVE_OPACITY, clampOpacity(s.fcImmersiveOpacity));
        map.put(KEY_FC_ISLAND_OPACITY, clampOpacity(s.fcIslandOpacity));
        map.put(KEY_FC_BLUR, clampBlur(s.fcBlur));
        map.put(KEY_FC_BOTTOM_GLOW_BOOST, clampBottomGlow(s.fcBottomGlow));
        map.put(KEY_FC_BOTTOM_GLOW_CHROMA, clampBottomGlowChroma(s.fcBottomGlowChroma));
        map.put(KEY_FC_SINGLE_GLOW_BOOST, clampBottomGlow(s.fcSingleGlow));
        map.put(KEY_FC_SINGLE_GLOW_CHROMA, clampBottomGlowChroma(s.fcSingleGlowChroma));
        map.put(KEY_FC_COLOR, normalizeColor(s.fcColor));
        map.put(KEY_FC_LAST_COLOR, normalizeColor(s.fcLastColor) != 0 ? normalizeColor(s.fcLastColor) : DEF_CUSTOM_COLOR);
        map.put(KEY_FC_MATCH_CARD, s.fcMatchCard);
        map.put(KEY_FC_REMOVE_ISLAND_MASK, s.fcRemoveIslandMask);
        map.put(KEY_FC_REMOVE_SHADE_MASK, s.fcRemoveShadeMask);
        map.put(KEY_FC_MASK_FOLLOW_MEDIA, s.fcMaskFollowMedia);
        map.put(KEY_FC_MASK_BRIGHTNESS, clampMaskPercent(s.fcMaskBrightness));
        map.put(KEY_FC_MASK_CHROMA, clampMaskPercent(s.fcMaskChroma));
        map.put(KEY_FC_MASK_STRENGTH, clampMaskStrength(s.fcMaskStrength));
        map.put(KEY_FC_NON_MEDIA_MASK_ENABLED, s.fcNonMediaMaskEnabled);
        map.put(KEY_FC_NON_MEDIA_MASK_COLOR, normalizePickerColor(s.fcNonMediaMaskColor));
        map.put(KEY_FC_NON_MEDIA_MASK_LAST_COLOR, normalizePickerColor(s.fcNonMediaMaskLastColor));
        map.put(KEY_FC_NON_MEDIA_MASK_OPACITY, clampMaskStrength(s.fcNonMediaMaskOpacity));
        map.put(KEY_FC_NON_MEDIA_FOLLOW_MASK_OPACITY, clampMaskStrength(s.fcNonMediaFollowMaskOpacity));
        map.put(KEY_FC_MASK_GRAD_START, gradNorm[0]);
        map.put(KEY_FC_MASK_GRAD_END, gradNorm[1]);
        map.put(KEY_FC_MASK_GRAD_MID, gradNorm[2]);
        map.put(KEY_FC_MASK_GRAD_STRENGTH, clampMaskStrength(s.fcMaskGradStrength));
        map.put(KEY_FC_DEBUG_MODE, s.fcDebugMode);
        map.put(KEY_FC_VER, s.fcVer);
        return map;
    }

    private static boolean readMapBool(Map<String, Object> map, String key, boolean defVal) {
        Object v = map.get(key);
        return v instanceof Boolean ? (Boolean) v : defVal;
    }

    private static int readMapInt(Map<String, Object> map, String key, int defVal) {
        Object v = map.get(key);
        return v instanceof Number ? ((Number) v).intValue() : defVal;
    }

    private static int readGlowBoostFromPrefs(SharedPreferences prefs) {
        if (prefs.contains(KEY_FC_BOTTOM_GLOW_BOOST)) {
            return clampBottomGlow(prefs.getInt(KEY_FC_BOTTOM_GLOW_BOOST, DEF_FC_BOTTOM_GLOW));
        }
        if (prefs.contains(KEY_FC_BOTTOM_GLOW)) {
            int legacy = prefs.getInt(KEY_FC_BOTTOM_GLOW, 100);
            return clampBottomGlow(Math.max(0, legacy - 100));
        }
        return DEF_FC_BOTTOM_GLOW;
    }

    private static int readGlowBoostFromMap(Map<String, Object> map) {
        if (map.containsKey(KEY_FC_BOTTOM_GLOW_BOOST)) {
            return clampBottomGlow(readMapInt(map, KEY_FC_BOTTOM_GLOW_BOOST, DEF_FC_BOTTOM_GLOW));
        }
        if (map.containsKey(KEY_FC_BOTTOM_GLOW)) {
            int legacy = readMapInt(map, KEY_FC_BOTTOM_GLOW, 100);
            return clampBottomGlow(Math.max(0, legacy - 100));
        }
        return DEF_FC_BOTTOM_GLOW;
    }

    public static void saveLocal(Context ctx, SharedPreferences prefs, boolean xspActive, SettingsData s) {
        s.offset = clamp(s.offset);
        s.fcOpacity = clampOpacity(s.fcOpacity);
        s.fcImmersiveOpacity = clampOpacity(s.fcImmersiveOpacity);
        s.fcIslandOpacity = clampOpacity(s.fcIslandOpacity);
        s.fcBlur = clampBlur(s.fcBlur);
        s.fcBottomGlow = clampBottomGlow(s.fcBottomGlow);
        s.fcBottomGlowChroma = clampBottomGlowChroma(s.fcBottomGlowChroma);
        s.fcSingleGlow = clampBottomGlow(s.fcSingleGlow);
        s.fcSingleGlowChroma = clampBottomGlowChroma(s.fcSingleGlowChroma);
        s.fcMaskBrightness = clampMaskPercent(s.fcMaskBrightness);
        s.fcMaskChroma = clampMaskPercent(s.fcMaskChroma);
        s.fcMaskStrength = clampMaskStrength(s.fcMaskStrength);
        s.fcColor = normalizeColor(s.fcColor);
        if (s.fcColor != 0) {
            s.fcLastColor = s.fcColor;
        }
        s.fcNonMediaMaskColor = normalizePickerColor(s.fcNonMediaMaskColor);
        s.fcNonMediaMaskLastColor = s.fcNonMediaMaskColor;
        s.fcNonMediaMaskOpacity = clampMaskStrength(s.fcNonMediaMaskOpacity);
        s.fcNonMediaFollowMaskOpacity = clampMaskStrength(s.fcNonMediaFollowMaskOpacity);
        int[] gradNorm = normalizeGradientPct(s.fcMaskGradStart, s.fcMaskGradEnd, s.fcMaskGradMid);
        s.fcMaskGradStart = gradNorm[0];
        s.fcMaskGradEnd = gradNorm[1];
        s.fcMaskGradMid = gradNorm[2];
        s.fcMaskGradStrength = clampMaskStrength(s.fcMaskGradStrength);
        s.fcVer = s.fcVer + 1;

        prefs.edit()
                .putBoolean(KEY_ALIGN, s.align)
                .putInt(KEY_OFFSET, s.offset)
                .putBoolean(KEY_IMMERSIVE_FP, s.immersiveFp)
                .putBoolean(KEY_HIDE_FP_ON_CAPSULE, s.hideFpOnCapsule)
                .putBoolean(KEY_AOD_NOTIF_TRANSLUCENT, s.aodNotifTranslucent)
                .putBoolean(KEY_LOCK_NOTIF_GLOW_FOLLOW_MEDIA, s.lockNotifGlowFollowMedia)
                .putBoolean(KEY_FC_ENABLED, s.fcEnabled)
                .putInt(KEY_FC_OPACITY, s.fcOpacity)
                .putInt(KEY_FC_IMMERSIVE_OPACITY, s.fcImmersiveOpacity)
                .putInt(KEY_FC_ISLAND_OPACITY, s.fcIslandOpacity)
                .putInt(KEY_FC_BLUR, s.fcBlur)
                .putInt(KEY_FC_BOTTOM_GLOW_BOOST, s.fcBottomGlow)
                .putInt(KEY_FC_BOTTOM_GLOW_CHROMA, s.fcBottomGlowChroma)
                .putInt(KEY_FC_SINGLE_GLOW_BOOST, s.fcSingleGlow)
                .putInt(KEY_FC_SINGLE_GLOW_CHROMA, s.fcSingleGlowChroma)
                .putInt(KEY_FC_COLOR, s.fcColor)
                .putInt(KEY_FC_LAST_COLOR, s.fcLastColor)
                .putBoolean(KEY_FC_MATCH_CARD, s.fcMatchCard)
                .putBoolean(KEY_FC_REMOVE_ISLAND_MASK, s.fcRemoveIslandMask)
                .putBoolean(KEY_FC_REMOVE_SHADE_MASK, s.fcRemoveShadeMask)
                .putBoolean(KEY_FC_MASK_FOLLOW_MEDIA, s.fcMaskFollowMedia)
                .putInt(KEY_FC_MASK_BRIGHTNESS, s.fcMaskBrightness)
                .putInt(KEY_FC_MASK_CHROMA, s.fcMaskChroma)
                .putInt(KEY_FC_MASK_STRENGTH, s.fcMaskStrength)
                .putBoolean(KEY_FC_NON_MEDIA_MASK_ENABLED, s.fcNonMediaMaskEnabled)
                .putInt(KEY_FC_NON_MEDIA_MASK_COLOR, s.fcNonMediaMaskColor)
                .putInt(KEY_FC_NON_MEDIA_MASK_LAST_COLOR, s.fcNonMediaMaskLastColor)
                .putInt(KEY_FC_NON_MEDIA_MASK_OPACITY, s.fcNonMediaMaskOpacity)
                .putInt(KEY_FC_NON_MEDIA_FOLLOW_MASK_OPACITY, s.fcNonMediaFollowMaskOpacity)
                .putInt(KEY_FC_MASK_GRAD_START, s.fcMaskGradStart)
                .putInt(KEY_FC_MASK_GRAD_END, s.fcMaskGradEnd)
                .putInt(KEY_FC_MASK_GRAD_MID, s.fcMaskGradMid)
                .putInt(KEY_FC_MASK_GRAD_STRENGTH, s.fcMaskGradStrength)
                .putBoolean(KEY_FC_DEBUG_MODE, s.fcDebugMode)
                .putInt(KEY_FC_VER, s.fcVer)
                .putBoolean(KEY_FC_MIGRATED, true)
                .apply();
    }

    public static boolean pushRemote(SettingsData s) {
        return XposedProvider.pushRemotePrefs(PREFS_NAME, toMap(s));
    }

    public static final class HookConfig {
        private final XposedModule module;
        private SharedPreferences remotePrefs;
        private SharedPreferences.OnSharedPreferenceChangeListener remoteListener;
        private final Handler reloadHandler = new Handler(Looper.getMainLooper());
        private final Runnable remoteReloadTask = new Runnable() {
            @Override
            public void run() {
                reload(true);
            }
        };
        private long lastRemoteInitAttemptMs;
        private volatile long lastCheckUptimeMs;

        private volatile boolean alignEnabled;
        private volatile int userOffset;
        private volatile boolean immersiveFpEnabled = true;
        private volatile boolean hideFpOnCapsule = true;
        private volatile boolean aodNotifTranslucent = false;
        private volatile boolean lockNotifGlowFollowMedia = false;

        private volatile boolean fcEnabled = true;
        private volatile int fcOpacity = DEF_FC_OPACITY;
        private volatile int fcImmersiveOpacity = DEF_FC_IMMERSIVE_OPACITY;
        private volatile int fcIslandOpacity = DEF_FC_ISLAND_OPACITY;
        private volatile int fcBlur = DEF_FC_BLUR;
        private volatile int fcBottomGlow = DEF_FC_BOTTOM_GLOW;
        private volatile int fcBottomGlowChroma = DEF_FC_BOTTOM_GLOW_CHROMA;
        private volatile int fcSingleGlow = DEF_FC_BOTTOM_GLOW;
        private volatile int fcSingleGlowChroma = DEF_FC_BOTTOM_GLOW_CHROMA;
        private volatile int fcColor = DEF_FC_COLOR;
        private volatile boolean fcMatchCard = true;
        private volatile boolean fcRemoveIslandMask = true;
        private volatile boolean fcRemoveShadeMask = false;
        private volatile boolean fcMaskFollowMedia = false;
        private volatile int fcMaskBrightness = DEF_FC_MASK_BRIGHTNESS;
        private volatile int fcMaskChroma = DEF_FC_MASK_CHROMA;
        private volatile int fcMaskStrength = DEF_FC_MASK_STRENGTH;
        private volatile boolean fcNonMediaMaskEnabled = false;
        private volatile int fcNonMediaMaskColor = DEF_NON_MEDIA_MASK_COLOR;
        private volatile int fcNonMediaMaskOpacity = DEF_FC_NON_MEDIA_MASK_OPACITY;
        private volatile int fcNonMediaFollowMaskOpacity = DEF_FC_NON_MEDIA_FOLLOW_MASK_OPACITY;
        private volatile int fcMaskGradStart = DEF_FC_MASK_GRAD_START;
        private volatile int fcMaskGradEnd = DEF_FC_MASK_GRAD_END;
        private volatile int fcMaskGradMid = DEF_FC_MASK_GRAD_MID;
        private volatile int fcMaskGradStrength = DEF_FC_MASK_GRAD_STRENGTH;
        private volatile boolean fcDebugMode = false;
        private volatile int fcVersion;
        private volatile boolean remotePrefsReady;

        private volatile String activeChannel = "none";

        private boolean firstLog = true;
        private boolean lastLoggedAlign;
        private int lastLoggedOffset;
        private boolean lastLoggedImmFp;
        private boolean lastLoggedHideFp;
        private boolean lastLoggedAodNotifTranslucent;
        private boolean lastLoggedLockNotifGlowFollowMedia;
        private boolean lastLoggedFcEnabled;
        private int lastLoggedFcOpacity;
        private int lastLoggedFcImmersiveOpacity;
        private int lastLoggedFcIslandOpacity;
        private int lastLoggedFcBlur;
        private int lastLoggedFcBottomGlow;
        private int lastLoggedFcBottomGlowChroma;
        private int lastLoggedFcSingleGlow;
        private int lastLoggedFcSingleGlowChroma;
        private int lastLoggedFcColor;
        private boolean lastLoggedFcMatchCard;
        private boolean lastLoggedFcRemoveIslandMask;
        private boolean lastLoggedFcRemoveShadeMask;
        private boolean lastLoggedFcMaskFollowMedia;
        private int lastLoggedFcMaskBrightness;
        private int lastLoggedFcMaskChroma;
        private int lastLoggedFcMaskStrength;
        private boolean lastLoggedFcNonMediaMaskEnabled;
        private int lastLoggedFcNonMediaMaskColor;
        private int lastLoggedFcNonMediaMaskOpacity;
        private int lastLoggedFcNonMediaFollowMaskOpacity;
        private int lastLoggedFcMaskGradStart;
        private int lastLoggedFcMaskGradEnd;
        private int lastLoggedFcMaskGradMid;
        private int lastLoggedFcMaskGradStrength;
        private boolean lastLoggedFcDebugMode;
        private int loggedCount;

        public HookConfig(XposedModule module) {
            this.module = module;
            reload(true);
            note("init channel=" + activeChannel
                    + " align=" + alignEnabled
                    + " offset=" + userOffset
                    + " immFp=" + immersiveFpEnabled
                    + " hideFp=" + hideFpOnCapsule
                    + " aodNotifTranslucent=" + aodNotifTranslucent
                    + " lockNotifGlowMedia=" + lockNotifGlowFollowMedia
                    + " fc=" + fcEnabled
                    + " op=" + fcOpacity
                    + " immOp=" + fcImmersiveOpacity
                    + " islandOp=" + fcIslandOpacity
                    + " blur=" + fcBlur
                    + " glow=" + fcBottomGlow
                    + " glowChroma=" + fcBottomGlowChroma
                    + " singleGlow=" + fcSingleGlow
                    + " singleGlowChroma=" + fcSingleGlowChroma
                    + " color=0x" + Integer.toHexString(fcColor)
                    + " match=" + fcMatchCard
                    + " islandMask=" + fcRemoveIslandMask
                    + " shadeMask=" + fcRemoveShadeMask
                    + " mediaMask=" + fcMaskFollowMedia
                    + " maskBright=" + fcMaskBrightness
                    + " maskChroma=" + fcMaskChroma
                    + " maskStrength=" + fcMaskStrength
                    + " nonMediaMaskEn=" + fcNonMediaMaskEnabled
                    + " nonMediaMask=0x" + Integer.toHexString(fcNonMediaMaskColor)
                    + " nonMediaOp=" + fcNonMediaMaskOpacity
                    + " nonMediaFollowOp=" + fcNonMediaFollowMaskOpacity
                    + " maskGrad=" + fcMaskGradStart + "/" + fcMaskGradEnd + "/" + fcMaskGradMid + "/" + fcMaskGradStrength
                    + " debug=" + fcDebugMode);
        }

        public boolean isAlignEnabled() {
            reload(false);
            return alignEnabled;
        }

        public int getUserOffset() {
            reload(false);
            return userOffset;
        }

        public boolean isImmersiveFpEnabled() {
            reload(false);
            return immersiveFpEnabled;
        }

        public boolean isHideFpOnCapsule() {
            reload(false);
            return hideFpOnCapsule;
        }

        public boolean isAodNotifTranslucent() {
            reload(false);
            return aodNotifTranslucent;
        }

        public boolean isLockNotifGlowFollowMedia() {
            reload(false);
            return lockNotifGlowFollowMedia;
        }

        public boolean isFcEnabled() {
            reload(false);
            return fcEnabled;
        }

        public int getFcOpacityInt() {
            reload(false);
            return fcOpacity;
        }

        public float getFcOpacity() {
            reload(false);
            return fcOpacity / 100.0f;
        }

        public int getFcImmersiveOpacityInt() {
            reload(false);
            return fcImmersiveOpacity;
        }

        public float getFcImmersiveOpacity() {
            reload(false);
            return fcImmersiveOpacity / 100.0f;
        }

        public boolean isFcImmersiveOpacityModified() {
            reload(false);
            return fcColor != 0 || fcImmersiveOpacity < 100;
        }

        public int getFcIslandOpacityInt() {
            reload(false);
            return fcIslandOpacity;
        }

        public float getFcIslandOpacity() {
            reload(false);
            return fcIslandOpacity / 100.0f;
        }

        public int getFcBlurInt() {
            reload(false);
            return fcBlur;
        }

        public float getFcBlurScale() {
            reload(false);
            return fcBlur / 100.0f;
        }

        public int getFcBottomGlowInt() {
            reload(false);
            return fcBottomGlow;
        }

        public float getFcBottomGlowBoost() {
            reload(false);
            return Math.max(0.0f, Math.min(1.0f, fcBottomGlow / (float) MAX_FC_BOTTOM_GLOW));
        }

        public int getFcBottomGlowChromaInt() {
            reload(false);
            return fcBottomGlowChroma;
        }

        public float getFcBottomGlowChromaBoost() {
            reload(false);
            return Math.max(0.0f, Math.min(1.0f, fcBottomGlowChroma / (float) MAX_FC_BOTTOM_GLOW_CHROMA));
        }

        public int getFcSingleGlowInt() {
            reload(false);
            return fcSingleGlow;
        }

        public float getFcSingleGlowBoost() {
            reload(false);
            return Math.max(0.0f, Math.min(1.0f, fcSingleGlow / (float) MAX_FC_BOTTOM_GLOW));
        }

        public int getFcSingleGlowChromaInt() {
            reload(false);
            return fcSingleGlowChroma;
        }

        public float getFcSingleGlowChromaBoost() {
            reload(false);
            return Math.max(0.0f, Math.min(1.0f, fcSingleGlowChroma / (float) MAX_FC_BOTTOM_GLOW_CHROMA));
        }

        public int getFcColor() {
            reload(false);
            return fcColor;
        }

        public boolean isFcMatchCard() {
            reload(false);
            return fcMatchCard;
        }

        public boolean isFcRemoveIslandMask() {
            reload(false);
            return fcRemoveIslandMask;
        }

        public boolean isFcRemoveShadeMask() {
            reload(false);
            return fcRemoveShadeMask;
        }

        public boolean isFcMaskFollowMedia() {
            reload(false);
            return fcMaskFollowMedia;
        }

        public int getFcMaskBrightnessInt() {
            reload(false);
            return fcMaskBrightness;
        }

        public int getFcMaskChromaInt() {
            reload(false);
            return fcMaskChroma;
        }

        public int getFcMaskStrengthInt() {
            reload(false);
            return fcMaskStrength;
        }

        public float getFcMaskStrengthScale() {
            reload(false);
            return fcMaskStrength / 100.0f;
        }

        public boolean isFcNonMediaMaskEnabled() {
            reload(false);
            return fcNonMediaMaskEnabled;
        }

        public int getFcNonMediaMaskColor() {
            reload(false);
            return fcNonMediaMaskColor;
        }

        public int getFcNonMediaMaskOpacityInt() {
            reload(false);
            return fcNonMediaMaskOpacity;
        }

        public float getFcNonMediaMaskOpacityScale() {
            reload(false);
            return fcNonMediaMaskOpacity / 100.0f;
        }

        public int getFcNonMediaFollowMaskOpacityInt() {
            reload(false);
            return fcNonMediaFollowMaskOpacity;
        }

        public float getFcNonMediaFollowMaskOpacityScale() {
            reload(false);
            return fcNonMediaFollowMaskOpacity / 100.0f;
        }

        public float getEffectiveNonMediaMaskOpacityScale() {
            reload(false);
            return (fcNonMediaMaskEnabled ? fcNonMediaFollowMaskOpacity : fcNonMediaMaskOpacity) / 100.0f;
        }

        public float getNonMediaMaskOpacityScaleForTint(boolean applyingMediaTint) {
            reload(false);
            return (applyingMediaTint ? fcNonMediaFollowMaskOpacity : fcNonMediaMaskOpacity) / 100.0f;
        }

        public int getFcMaskGradStartInt() {
            reload(false);
            return fcMaskGradStart;
        }

        public int getFcMaskGradEndInt() {
            reload(false);
            return fcMaskGradEnd;
        }

        public int getFcMaskGradMidInt() {
            reload(false);
            return fcMaskGradMid;
        }

        public int getFcMaskGradStrengthInt() {
            reload(false);
            return fcMaskGradStrength;
        }

        public float getFcMaskGradStrengthScale() {
            reload(false);
            return fcMaskGradStrength / 100.0f;
        }

        public boolean isFcMaskGradTemplateModified() {
            reload(false);
            return fcMaskGradStart != DEF_FC_MASK_GRAD_START
                    || fcMaskGradEnd != DEF_FC_MASK_GRAD_END
                    || fcMaskGradMid != DEF_FC_MASK_GRAD_MID
                    || fcMaskGradStrength != DEF_FC_MASK_GRAD_STRENGTH;
        }

        public boolean isFcDebugMode() {
            reload(false);
            return fcDebugMode;
        }

        public int getFcVersion() {
            reload(false);
            return fcVersion;
        }

        public boolean isFcLockTintOrMaskModified() {
            reload(false);
            return fcColor != 0 || fcOpacity < 100 || fcMatchCard
                    || fcMaskFollowMedia || fcNonMediaMaskEnabled
                    || fcMaskStrength != DEF_FC_MASK_STRENGTH
                    || fcNonMediaMaskOpacity != DEF_FC_NON_MEDIA_MASK_OPACITY
                    || fcNonMediaFollowMaskOpacity != DEF_FC_NON_MEDIA_FOLLOW_MASK_OPACITY
                    || isFcMaskGradTemplateModified();
        }

        public boolean isFcStockAppearance() {
            reload(false);
            return fcColor == 0 && fcOpacity >= 100 && !fcMatchCard
                    && fcBottomGlow == DEF_FC_BOTTOM_GLOW
                    && fcBottomGlowChroma == DEF_FC_BOTTOM_GLOW_CHROMA
                    && fcSingleGlow == DEF_FC_BOTTOM_GLOW
                    && fcSingleGlowChroma == DEF_FC_BOTTOM_GLOW_CHROMA;
        }

        public boolean isFcIslandStockAppearance() {
            reload(false);
            return fcColor == 0 && fcIslandOpacity >= 100 && !fcRemoveIslandMask
                    && fcBottomGlow == DEF_FC_BOTTOM_GLOW
                    && fcBottomGlowChroma == DEF_FC_BOTTOM_GLOW_CHROMA
                    && fcSingleGlow == DEF_FC_BOTTOM_GLOW
                    && fcSingleGlowChroma == DEF_FC_BOTTOM_GLOW_CHROMA;
        }

        public String getActiveChannel() {
            return activeChannel;
        }

        public synchronized void note(String message) {
            if (loggedCount >= LOG_LIMIT) {
                return;
            }
            loggedCount++;
            XposedCompat.log(message);
        }

        public void reload(boolean force) {
            if (!force && remotePrefsReady) {
                return;
            }
            if (!force) {
                long prevCheck = lastCheckUptimeMs;
                if (prevCheck != 0L && (SystemClock.uptimeMillis() - prevCheck) < RELOAD_THROTTLE_MS) {
                    return;
                }
            }
            synchronized (this) {
                if (!force && remotePrefsReady) {
                    return;
                }
                long now = SystemClock.uptimeMillis();
                if (!force && lastCheckUptimeMs != 0L && (now - lastCheckUptimeMs) < RELOAD_THROTTLE_MS) {
                    return;
                }
                lastCheckUptimeMs = now;
                tryLoadRemotePrefs(force);
            }
        }

        private boolean tryLoadRemotePrefs(boolean force) {
            try {
                long now = SystemClock.uptimeMillis();
                if (remotePrefs == null && module != null
                        && (force || lastRemoteInitAttemptMs == 0L || (now - lastRemoteInitAttemptMs) >= REMOTE_RETRY_INTERVAL_MS)) {
                    lastRemoteInitAttemptMs = now;
                    SharedPreferences sp = module.getRemotePreferences(PREFS_NAME);
                    if (sp != null) {
                        remotePrefs = sp;
                        remoteListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                            @Override
                            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                                reloadHandler.removeCallbacks(remoteReloadTask);
                                reloadHandler.postDelayed(remoteReloadTask, 16L);
                            }
                        };
                        sp.registerOnSharedPreferenceChangeListener(remoteListener);
                        remotePrefsReady = true;
                    }
                }
                if (remotePrefs == null || !hasAnySetting(remotePrefs)) {
                    return false;
                }
                alignEnabled = remotePrefs.getBoolean(KEY_ALIGN, false);
                userOffset = clamp(remotePrefs.getInt(KEY_OFFSET, 0));
                immersiveFpEnabled = remotePrefs.getBoolean(KEY_IMMERSIVE_FP, true);
                hideFpOnCapsule = remotePrefs.getBoolean(KEY_HIDE_FP_ON_CAPSULE, true);
                aodNotifTranslucent = remotePrefs.getBoolean(KEY_AOD_NOTIF_TRANSLUCENT, false);
                lockNotifGlowFollowMedia = remotePrefs.getBoolean(KEY_LOCK_NOTIF_GLOW_FOLLOW_MEDIA, false);
                fcEnabled = remotePrefs.getBoolean(KEY_FC_ENABLED, true);
                int nextOp = clampOpacity(remotePrefs.getInt(KEY_FC_OPACITY, DEF_FC_OPACITY));
                fcOpacity = nextOp;
                int fallbackImm = remotePrefs.contains(KEY_FC_OPACITY) ? nextOp : DEF_FC_IMMERSIVE_OPACITY;
                fcImmersiveOpacity = clampOpacity(remotePrefs.getInt(KEY_FC_IMMERSIVE_OPACITY, fallbackImm));
                int fallbackIsland = remotePrefs.contains(KEY_FC_OPACITY) ? nextOp : DEF_FC_ISLAND_OPACITY;
                fcIslandOpacity = clampOpacity(remotePrefs.getInt(KEY_FC_ISLAND_OPACITY, fallbackIsland));
                fcBlur = clampBlur(remotePrefs.getInt(KEY_FC_BLUR, DEF_FC_BLUR));
                fcBottomGlow = readGlowBoostFromPrefs(remotePrefs);
                fcBottomGlowChroma = clampBottomGlowChroma(
                        remotePrefs.getInt(KEY_FC_BOTTOM_GLOW_CHROMA, DEF_FC_BOTTOM_GLOW_CHROMA));
                fcSingleGlow = remotePrefs.contains(KEY_FC_SINGLE_GLOW_BOOST)
                        ? clampBottomGlow(remotePrefs.getInt(KEY_FC_SINGLE_GLOW_BOOST, DEF_FC_BOTTOM_GLOW))
                        : fcBottomGlow;
                fcSingleGlowChroma = remotePrefs.contains(KEY_FC_SINGLE_GLOW_CHROMA)
                        ? clampBottomGlowChroma(
                                remotePrefs.getInt(KEY_FC_SINGLE_GLOW_CHROMA, DEF_FC_BOTTOM_GLOW_CHROMA))
                        : fcBottomGlowChroma;
                fcColor = normalizeColor(remotePrefs.getInt(KEY_FC_COLOR, DEF_FC_COLOR));
                fcMatchCard = remotePrefs.getBoolean(KEY_FC_MATCH_CARD, true);
                fcRemoveIslandMask = remotePrefs.contains(KEY_FC_REMOVE_ISLAND_MASK)
                        ? remotePrefs.getBoolean(KEY_FC_REMOVE_ISLAND_MASK, true)
                        : fcMatchCard;
                fcRemoveShadeMask = remotePrefs.getBoolean(KEY_FC_REMOVE_SHADE_MASK, false);
                fcMaskFollowMedia = remotePrefs.getBoolean(KEY_FC_MASK_FOLLOW_MEDIA, false);
                fcMaskBrightness = clampMaskPercent(
                        remotePrefs.getInt(KEY_FC_MASK_BRIGHTNESS, DEF_FC_MASK_BRIGHTNESS));
                fcMaskChroma = clampMaskPercent(
                        remotePrefs.getInt(KEY_FC_MASK_CHROMA, DEF_FC_MASK_CHROMA));
                fcMaskStrength = clampMaskStrength(
                        remotePrefs.getInt(KEY_FC_MASK_STRENGTH, DEF_FC_MASK_STRENGTH));
                int rawNonMedia = remotePrefs.getInt(KEY_FC_NON_MEDIA_MASK_COLOR, DEF_NON_MEDIA_MASK_COLOR);
                fcNonMediaMaskEnabled = remotePrefs.getBoolean(KEY_FC_NON_MEDIA_MASK_ENABLED, false);
                fcNonMediaMaskColor = normalizePickerColor(rawNonMedia);
                fcNonMediaMaskOpacity = clampMaskStrength(
                        remotePrefs.getInt(KEY_FC_NON_MEDIA_MASK_OPACITY, DEF_FC_NON_MEDIA_MASK_OPACITY));
                fcNonMediaFollowMaskOpacity = remotePrefs.contains(KEY_FC_NON_MEDIA_FOLLOW_MASK_OPACITY)
                        ? clampMaskStrength(remotePrefs.getInt(KEY_FC_NON_MEDIA_FOLLOW_MASK_OPACITY,
                                DEF_FC_NON_MEDIA_FOLLOW_MASK_OPACITY))
                        : fcNonMediaMaskOpacity;
                int[] gradNorm = normalizeGradientPct(
                        remotePrefs.getInt(KEY_FC_MASK_GRAD_START, DEF_FC_MASK_GRAD_START),
                        remotePrefs.getInt(KEY_FC_MASK_GRAD_END, DEF_FC_MASK_GRAD_END),
                        remotePrefs.getInt(KEY_FC_MASK_GRAD_MID, DEF_FC_MASK_GRAD_MID));
                fcMaskGradStart = gradNorm[0];
                fcMaskGradEnd = gradNorm[1];
                fcMaskGradMid = gradNorm[2];
                fcMaskGradStrength = clampMaskStrength(
                        remotePrefs.getInt(KEY_FC_MASK_GRAD_STRENGTH, DEF_FC_MASK_GRAD_STRENGTH));
                fcDebugMode = remotePrefs.getBoolean(KEY_FC_DEBUG_MODE, false);
                fcVersion = remotePrefs.getInt(KEY_FC_VER, 0);

                activeChannel = "api102_remote";
                maybeNoteChange();
                return true;
            } catch (Throwable t) {
                if (force) {
                    note("remote prefs read failed: " + t);
                }
                return false;
            }
        }

        private void maybeNoteChange() {
            if (!firstLog
                    && alignEnabled == lastLoggedAlign
                    && userOffset == lastLoggedOffset
                    && immersiveFpEnabled == lastLoggedImmFp
                    && hideFpOnCapsule == lastLoggedHideFp
                    && aodNotifTranslucent == lastLoggedAodNotifTranslucent
                    && lockNotifGlowFollowMedia == lastLoggedLockNotifGlowFollowMedia
                    && fcEnabled == lastLoggedFcEnabled
                    && fcOpacity == lastLoggedFcOpacity
                    && fcImmersiveOpacity == lastLoggedFcImmersiveOpacity
                    && fcIslandOpacity == lastLoggedFcIslandOpacity
                    && fcBlur == lastLoggedFcBlur
                    && fcBottomGlow == lastLoggedFcBottomGlow
                    && fcBottomGlowChroma == lastLoggedFcBottomGlowChroma
                    && fcSingleGlow == lastLoggedFcSingleGlow
                    && fcSingleGlowChroma == lastLoggedFcSingleGlowChroma
                    && fcColor == lastLoggedFcColor
                    && fcMatchCard == lastLoggedFcMatchCard
                    && fcRemoveIslandMask == lastLoggedFcRemoveIslandMask
                    && fcRemoveShadeMask == lastLoggedFcRemoveShadeMask
                    && fcMaskFollowMedia == lastLoggedFcMaskFollowMedia
                    && fcMaskBrightness == lastLoggedFcMaskBrightness
                    && fcMaskChroma == lastLoggedFcMaskChroma
                    && fcMaskStrength == lastLoggedFcMaskStrength
                    && fcNonMediaMaskEnabled == lastLoggedFcNonMediaMaskEnabled
                    && fcNonMediaMaskColor == lastLoggedFcNonMediaMaskColor
                    && fcNonMediaMaskOpacity == lastLoggedFcNonMediaMaskOpacity
                    && fcNonMediaFollowMaskOpacity == lastLoggedFcNonMediaFollowMaskOpacity
                    && fcMaskGradStart == lastLoggedFcMaskGradStart
                    && fcMaskGradEnd == lastLoggedFcMaskGradEnd
                    && fcMaskGradMid == lastLoggedFcMaskGradMid
                    && fcMaskGradStrength == lastLoggedFcMaskGradStrength
                    && fcDebugMode == lastLoggedFcDebugMode) {
                return;
            }
            note("config -> align=" + alignEnabled
                    + " offset=" + userOffset
                    + " immFp=" + immersiveFpEnabled
                    + " hideFp=" + hideFpOnCapsule
                    + " aodNotifTranslucent=" + aodNotifTranslucent
                    + " lockNotifGlowMedia=" + lockNotifGlowFollowMedia
                    + " fc=" + fcEnabled
                    + " op=" + fcOpacity
                    + " immOp=" + fcImmersiveOpacity
                    + " islandOp=" + fcIslandOpacity
                    + " blur=" + fcBlur
                    + " glow=" + fcBottomGlow
                    + " glowChroma=" + fcBottomGlowChroma
                    + " singleGlow=" + fcSingleGlow
                    + " singleGlowChroma=" + fcSingleGlowChroma
                    + " color=0x" + Integer.toHexString(fcColor)
                    + " match=" + fcMatchCard
                    + " islandMask=" + fcRemoveIslandMask
                    + " shadeMask=" + fcRemoveShadeMask
                    + " mediaMask=" + fcMaskFollowMedia
                    + " maskBright=" + fcMaskBrightness
                    + " maskChroma=" + fcMaskChroma
                    + " maskStrength=" + fcMaskStrength
                    + " nonMediaMaskEn=" + fcNonMediaMaskEnabled
                    + " nonMediaMask=0x" + Integer.toHexString(fcNonMediaMaskColor)
                    + " nonMediaOp=" + fcNonMediaMaskOpacity
                    + " nonMediaFollowOp=" + fcNonMediaFollowMaskOpacity
                    + " maskGrad=" + fcMaskGradStart + "/" + fcMaskGradEnd + "/" + fcMaskGradMid + "/" + fcMaskGradStrength
                    + " debug=" + fcDebugMode
                    + " (channel=" + activeChannel + ")");
            lastLoggedAlign = alignEnabled;
            lastLoggedOffset = userOffset;
            lastLoggedImmFp = immersiveFpEnabled;
            lastLoggedHideFp = hideFpOnCapsule;
            lastLoggedAodNotifTranslucent = aodNotifTranslucent;
            lastLoggedLockNotifGlowFollowMedia = lockNotifGlowFollowMedia;
            lastLoggedFcEnabled = fcEnabled;
            lastLoggedFcOpacity = fcOpacity;
            lastLoggedFcImmersiveOpacity = fcImmersiveOpacity;
            lastLoggedFcIslandOpacity = fcIslandOpacity;
            lastLoggedFcBlur = fcBlur;
            lastLoggedFcBottomGlow = fcBottomGlow;
            lastLoggedFcBottomGlowChroma = fcBottomGlowChroma;
            lastLoggedFcSingleGlow = fcSingleGlow;
            lastLoggedFcSingleGlowChroma = fcSingleGlowChroma;
            lastLoggedFcColor = fcColor;
            lastLoggedFcMatchCard = fcMatchCard;
            lastLoggedFcRemoveIslandMask = fcRemoveIslandMask;
            lastLoggedFcRemoveShadeMask = fcRemoveShadeMask;
            lastLoggedFcMaskFollowMedia = fcMaskFollowMedia;
            lastLoggedFcMaskBrightness = fcMaskBrightness;
            lastLoggedFcMaskChroma = fcMaskChroma;
            lastLoggedFcMaskStrength = fcMaskStrength;
            lastLoggedFcNonMediaMaskEnabled = fcNonMediaMaskEnabled;
            lastLoggedFcNonMediaMaskColor = fcNonMediaMaskColor;
            lastLoggedFcNonMediaMaskOpacity = fcNonMediaMaskOpacity;
            lastLoggedFcNonMediaFollowMaskOpacity = fcNonMediaFollowMaskOpacity;
            lastLoggedFcMaskGradStart = fcMaskGradStart;
            lastLoggedFcMaskGradEnd = fcMaskGradEnd;
            lastLoggedFcMaskGradMid = fcMaskGradMid;
            lastLoggedFcMaskGradStrength = fcMaskGradStrength;
            lastLoggedFcDebugMode = fcDebugMode;
            firstLog = false;
        }
    }
}
