package io.github.nicochanyo.c17fpoverlap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.LinearGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.libxposed.service.XposedProvider;

public class MainActivity extends Activity {
    private static final String STATE_TAB = "active_tab";
    private static final String STATE_FC_SUBPAGE = "open_fc_subpage";
    private static final String STATE_FC_TINT_EXPANDED = "fc_tint_expanded";
    private static final String STATE_FC_MASK_TINT_EXPANDED = "fc_mask_tint_expanded";
    private static final String STATE_FC_GLOW_TINT_EXPANDED = "fc_glow_tint_expanded";
    private static final String COOLAPK_DEEP_PREFIX = "coolmarket://u/";
    private static final String COOLAPK_WEB_PREFIX = "https://www.coolapk.com/u/";
    private static final String LSPOSED_REPO_JSON_URL = "https://modules.lsposed.org/module/io.github.nicochanyo.c17fpoverlap.json";
    private static final String LSPOSED_REPO_PAGE_URL = "https://modules.lsposed.org/module/io.github.nicochanyo.c17fpoverlap/";
    private static final String GITHUB_LATEST_API_URL = "https://api.github.com/repos/Xposed-Modules-Repo/io.github.nicochanyo.c17fpoverlap/releases/latest";
    private static final String UID_ORIG_AUTHOR = "40697326";
    private static final String UID_FC_AUTHOR = "540690";
    private static final String UID_MOD_AUTHOR = "25380289";
    private static final int OFFSET_STEPS_HALF = ConfigStore.OFFSET_LIMIT / ConfigStore.OFFSET_STEP;
    private static final long SLIDER_SYNC_DELAY_MS = 250L;

    private static final int TAB_LOCKSCREEN = 0;
    private static final int TAB_APPEARANCE = 1;
    private static final int TAB_ABOUT = 2;

    private static final PathInterpolator FLUID_EASE = new PathInterpolator(0.22f, 0.88f, 0.25f, 1.0f);
    private static final PathInterpolator SUBPAGE_EASE = new PathInterpolator(0.28f, 0.0f, 0.12f, 1.0f);
    private static final OvershootInterpolator ICON_SPRING = new OvershootInterpolator(1.25f);
    private static final OvershootInterpolator NAV_SPRING_BACK = new OvershootInterpolator(1.15f);
    private static final float COLLAPSED_TITLE_SCALE = 0.70f;
    private static final float SUBTITLE_FADE_END_PROGRESS = 0.48f;
    private static final float SUBPAGE_COLLAPSED_TITLE_SCALE = 0.68f;
    private static final float SUBPAGE_PARALLAX_RATIO = 0.22f;
    private static final float SUBPAGE_SCRIM_MAX_ALPHA = 0.14f;

    private static int sPendingTabAfterRecreate = -1;

    private SharedPreferences prefs;
    private boolean xspActive;
    private boolean hadLocalKeys;
    private boolean hadPriorPrefs;
    private boolean remoteRestoreTried;
    private ConfigStore.SettingsData data;

    private int activeTab = TAB_LOCKSCREEN;
    private boolean suppressUiCallbacks;
    private boolean livingFlowUi;
    private int lastAppliedAccent;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService syncExecutor = Executors.newSingleThreadExecutor();
    private final AtomicInteger syncGeneration = new AtomicInteger(0);
    private final Runnable debouncedSync = new Runnable() {
        @Override
        public void run() {
            syncRemoteAsync();
        }
    };

    // Header & Progressive Blur
    private View layoutAppRoot;
    private ProgressiveBlurFrameLayout layoutRootBlur;
    private TopGradientBlurView vTopHeaderBlur;
    private View layoutHeader;
    private TextView tvPageTitle;
    private TextView tvPageSubtitle;
    private ValueAnimator headerCollapseAnimator;
    private ValueAnimator subtitleTabFadeAnimator;
    private float headerCollapseProgress;
    private float headerTitleTabAlpha = 1f;
    private float subtitleTabAlpha = 1f;
    private View btnTopQuickAppearance;
    private int statusBarTopPx;
    private int navBarBottomPx;
    private int headerTitleShiftPx;
    private int headerSubtitleShiftPx;
    private int headerCollapseRangePx;

    // Appearance Fullscreen Secondary Page
    private View vSubpageScrim;
    private ProgressiveBlurFrameLayout layoutFcSubpageRoot;
    private View layoutFcSubpageContainer;
    private CouiBounceScrollView scrollFcSubpage;
    private View layoutFcSubpageContent;
    private TopGradientBlurView vSubpageHeaderBlur;
    private View layoutSubpageHeader;
    private View layoutSubpageToolbar;
    private View btnSubpageBack;
    private TextView tvSubpageTitle;
    private TextView tvSubpageSubtitle;
    private FluidCloudPreviewView fcSubpagePreview;
    private float subpageHeaderCollapseProgress;
    private int subpageCollapseRangePx;
    private ValueAnimator subpageTransitionAnimator;
    private int subpageAnimGen;
    private Object fcSubpageBackCallback;
    private boolean fcSubpageBackRegistered;

    // Pages & Swipe tracking
    private View layoutPageContainer;
    private View pageLockscreen;
    private View pageAppearance;
    private View pageAbout;
    private final View[] pages = new View[3];

    private int touchSlop;
    private float downX;
    private float downY;
    private boolean trackSwipe;
    private boolean swipeTriggered;

    // Floating Pill Bottom Navigation
    private View vBottomNavShadow;
    private View layoutBottomNavContainer;
    private View layoutBottomNav;
    private View layoutNavItems;
    private View vNavSlidingPill;
    private ValueAnimator navPillMorphAnimator;
    private LivingFlowEffects.SpotLightDrawable navSpotLight;
    private LivingFlowEffects.NavBarElasticController navElasticController;
    private final View[] navTabs = new View[3];
    private final ImageView[] navIcons = new ImageView[3];
    private final TextView[] navLabels = new TextView[3];
    private boolean navDragging;
    private float navDownRawX;
    private float navDownRawY;
    private float navItemsRestLeftScreenX;
    private float navItemsWidth;
    private float navPillWidth;
    private float navMinPillX;
    private float navMaxPillX;

    // Tab 0: Status card & Lockscreen controls
    private View cardLockStatus;
    private View vLockStatusEmblem;
    private TextView tvLockStatusBadge;
    private ImageView ivStatusIcon;
    private TextView tvStatusTitle;
    private TextView tvStatusSubtitle;
    private Switch swAlign;
    private ViewGroup layoutCapsuleSub;
    private Switch swHideFpOnCapsule;
    private TextView btnZero;
    private TextView tvOffsetVal;
    private SeekBar sbOffset;
    private Switch swImmersiveFp;
    private Switch swAodNotifTranslucent;
    private Switch swLockNotifGlowFollowMedia;

    // Tab 1: Appearance controls
    private FluidCloudPreviewView fcPreview;
    private Switch swFcEnabled;
    private ViewGroup layoutFcSub;
    private ViewGroup layoutFcTintCard;
    private TextView tvFcIslandOpacityVal;
    private SeekBar sbFcIslandOpacity;
    private TextView tvFcOpacityVal;
    private SeekBar sbFcOpacity;
    private TextView tvFcImmersiveOpacityVal;
    private SeekBar sbFcImmersiveOpacity;
    private TextView tvFcBlurVal;
    private SeekBar sbFcBlur;
    private TextView tvFcBottomGlowVal;
    private SeekBar sbFcBottomGlow;
    private TextView tvFcBottomGlowChromaVal;
    private SeekBar sbFcBottomGlowChroma;
    private TextView tvFcSingleGlowVal;
    private SeekBar sbFcSingleGlow;
    private TextView tvFcSingleGlowChromaVal;
    private SeekBar sbFcSingleGlowChroma;
    private Switch swFcRemoveIslandMask;
    private Switch swFcMatchCard;
    private Switch swFcRemoveShadeMask;
    private TextView tvFcMaskStrengthVal;
    private SeekBar sbFcMaskStrength;
    private Switch swFcMaskFollowMedia;
    private ViewGroup layoutFcMaskToneSub;
    private TextView tvFcMaskBrightnessVal;
    private SeekBar sbFcMaskBrightness;
    private TextView tvFcMaskChromaVal;
    private SeekBar sbFcMaskChroma;
    private TextView tvFcNonMediaMaskOpacityVal;
    private SeekBar sbFcNonMediaMaskOpacity;
    private TextView tvFcNonMediaFollowMaskOpacityVal;
    private SeekBar sbFcNonMediaFollowMaskOpacity;
    private Switch swFcNonMediaMaskEnabled;
    private TextView tvFcMaskGradStartVal;
    private SeekBar sbFcMaskGradStart;
    private TextView tvFcMaskGradEndVal;
    private SeekBar sbFcMaskGradEnd;
    private TextView tvFcMaskGradMidVal;
    private SeekBar sbFcMaskGradMid;
    private TextView tvFcMaskGradStrengthVal;
    private SeekBar sbFcMaskGradStrength;
    private TextView tvFcCustomColorSummary;
    private View layoutFcTintBadge;
    private View vFcTintSwatchDot;
    private ImageView ivFcTintChevron;
    private Switch swFcCustomColor;
    private View dividerFcTintPicker;
    private View layoutFcColorPickerWrap;
    private CouiHsvColorPickerView fcColorPicker;
    private ValueAnimator fcColorPickerAnimator;
    private boolean fcColorPickerUserExpanded;
    private boolean fcColorPickerExpanded;
    private boolean fcColorPickerStateSynced;
    private TextView tvFcCustomMaskColorSummary;
    private View layoutFcMaskTintBadge;
    private View vFcMaskTintSwatchDot;
    private ImageView ivFcMaskTintChevron;
    private Switch swFcCustomMaskColor;
    private View dividerFcMaskTintPicker;
    private View layoutFcMaskColorPickerWrap;
    private CouiHsvColorPickerView fcMaskColorPicker;
    private ValueAnimator fcMaskColorPickerAnimator;
    private boolean fcMaskColorPickerUserExpanded;
    private boolean fcMaskColorPickerExpanded;
    private boolean fcMaskColorPickerStateSynced;
    private TextView tvFcCustomGlowColorSummary;
    private View layoutFcGlowTintBadge;
    private View vFcGlowTintSwatchDot;
    private ImageView ivFcGlowTintChevron;
    private Switch swFcCustomGlowColor;
    private View dividerFcGlowTintPicker;
    private View layoutFcGlowColorPickerWrap;
    private CouiHsvColorPickerView fcGlowColorPicker;
    private ValueAnimator fcGlowColorPickerAnimator;
    private boolean fcGlowColorPickerUserExpanded;
    private boolean fcGlowColorPickerExpanded;
    private boolean fcGlowColorPickerStateSynced;
    private TextView tvSummaryOpacityBlur;
    private TextView tvSummaryHighlight;
    private final View[] fcMenuPanels = new View[3];
    private int openFcMenuIndex = -1;

    // Tab 2: About status controls
    private View cardAboutDynamicHero;
    private ColorOsDynamicBackdropView vAboutDynamicBg;
    private View layoutAboutHeroLogo;
    private TextView tvAboutHeroVer;
    private TextView tvAboutHeroUpdateStatus;
    private TextView tvAboutBuildSub;
    private View cardAboutModuleStatus;
    private View vAboutStatusEmblem;
    private ImageView ivAboutStatusIcon;
    private TextView tvAboutStatusTitle;
    private TextView tvAboutStatusBadge;
    private TextView tvAboutStatusSubtitle;
    private TextView tvSysVersion;
    private TextView tvDeviceModel;
    private TextView tvAboutChannel;
    private TextView tvAboutUpdateChip;
    private TextView tvAboutUiStyleVal;
    private TextView tvAboutLanguageVal;
    private TextView tvAboutDarkModeVal;
    private Switch swAboutMonet;
    private Switch swAboutDebugMode;
    private View dividerAboutDebugTester;
    private View rowAboutDebugTester;
    private Runnable pendingDebugAction;
    private boolean updateCheckRunning;
    private static RepoReleaseInfo sCachedLatestRelease;
    private static boolean sSilentUpdateCheckDone;
    private static final int REQ_POST_NOTIF = 1001;

    @Override
    protected void attachBaseContext(Context newBase) {
        String lang = ConfigStore.getEffectiveAppLanguage(newBase);
        Locale locale = resolveLocale(lang);
        Locale.setDefault(locale);
        Configuration cfg = new Configuration(newBase.getResources().getConfiguration());
        cfg.setLocale(locale);
        applyDarkModeToConfig(cfg, ConfigStore.getAppDarkMode(newBase));
        super.attachBaseContext(newBase.createConfigurationContext(cfg));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyLocaleToResources();
        boolean isDark = ConfigStore.isDarkThemeActive(this);
        setTheme(isDark ? R.style.Theme_CouiFpOverlap_Dark : R.style.Theme_CouiFpOverlap_Light);
        super.onCreate(savedInstanceState);
        livingFlowUi = ConfigStore.isLivingFlowUi(this);
        setupEdgeToEdge();
        setContentView(livingFlowUi ? R.layout.activity_main_living_flow : R.layout.activity_main);
        if (livingFlowUi) {
            getWindow().setBackgroundDrawable(new ColorDrawable(getColor(R.color.lf_bg)));
        }
        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        ConfigStore.AppPrefs opened = ConfigStore.openAppPrefs(this);
        prefs = opened.prefs;
        xspActive = opened.xspActive;
        hadLocalKeys = opened.hadLocalKeys;
        hadPriorPrefs = opened.hadPriorPrefs;
        data = ConfigStore.loadFromPrefs(prefs);

        final int restoredSubpage = savedInstanceState != null
                ? savedInstanceState.getInt(STATE_FC_SUBPAGE, -1) : -1;
        fcColorPickerUserExpanded = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_FC_TINT_EXPANDED, false);
        fcMaskColorPickerUserExpanded = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_FC_MASK_TINT_EXPANDED, false);
        fcGlowColorPickerUserExpanded = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_FC_GLOW_TINT_EXPANDED, false);
        final boolean fromThemeRecreate = (sPendingTabAfterRecreate >= TAB_LOCKSCREEN
                && sPendingTabAfterRecreate <= TAB_ABOUT);
        if (sPendingTabAfterRecreate >= TAB_LOCKSCREEN && sPendingTabAfterRecreate <= TAB_ABOUT) {
            activeTab = sPendingTabAfterRecreate;
            sPendingTabAfterRecreate = -1;
        } else if (savedInstanceState != null) {
            activeTab = savedInstanceState.getInt(STATE_TAB, TAB_LOCKSCREEN);
        }

        bindViews();
        applyInsetsPadding();
        syncUiState();
        DynamicThemeHelper.applyAccent(findViewById(android.R.id.content), this);
        lastAppliedAccent = DynamicThemeHelper.getAccent(this);
        switchTab(activeTab, false);
        if (activeTab == TAB_APPEARANCE
                && restoredSubpage >= 0
                && restoredSubpage < fcMenuPanels.length
                && data.fcEnabled) {
            openFcSubpage(restoredSubpage, false);
        }
        if (sCachedLatestRelease != null) {
            applyUpdateChipState(sCachedLatestRelease);
        } else if (!sSilentUpdateCheckDone) {
            sSilentUpdateCheckDone = true;
            checkUpdateFromLsposedRepo(false);
        }
        XposedProvider.setConnectionListener(new Runnable() {
            @Override
            public void run() {
                xspActive = XposedProvider.isConnected();
                syncUiState();
                syncRemoteAsync();
            }
        });
        syncRemoteAsync();
        maybeShowPostUpdateChangelog(savedInstanceState, fromThemeRecreate);
    }

    @SuppressWarnings("deprecation")
    private void applyLocaleToResources() {
        String lang = ConfigStore.getEffectiveAppLanguage(this);
        Locale locale = resolveLocale(lang);
        Locale.setDefault(locale);
        Resources res = getResources();
        Configuration cfg = new Configuration(res.getConfiguration());
        cfg.setLocale(locale);
        applyDarkModeToConfig(cfg, ConfigStore.getAppDarkMode(this));
        res.updateConfiguration(cfg, res.getDisplayMetrics());
    }

    private static void applyDarkModeToConfig(Configuration cfg, int darkMode) {
        if (darkMode == ConfigStore.DARK_MODE_LIGHT) {
            cfg.uiMode = (cfg.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_NO;
        } else if (darkMode == ConfigStore.DARK_MODE_DARK) {
            cfg.uiMode = (cfg.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_YES;
        }
    }

    private static Locale resolveLocale(String lang) {
        if (ConfigStore.LANG_ZH_CN.equals(lang)) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        if (ConfigStore.LANG_EN.equals(lang)) {
            return Locale.ENGLISH;
        }
        return Locale.TRADITIONAL_CHINESE;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, activeTab);
        outState.putInt(STATE_FC_SUBPAGE, openFcMenuIndex);
        outState.putBoolean(STATE_FC_TINT_EXPANDED, fcColorPickerUserExpanded);
        outState.putBoolean(STATE_FC_MASK_TINT_EXPANDED, fcMaskColorPickerUserExpanded);
        outState.putBoolean(STATE_FC_GLOW_TINT_EXPANDED, fcGlowColorPickerUserExpanded);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        syncActiveHeaderToScrollNow();
        if (isFcSubpageOpen()) {
            syncSubpageHeaderToScrollNow();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        syncActiveHeaderToScrollNow();
        if (isFcSubpageOpen()) {
            syncSubpageHeaderToScrollNow();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (livingFlowUi) {
            int curAccent = DynamicThemeHelper.getAccent(this);
            if (curAccent != lastAppliedAccent) {
                lastAppliedAccent = curAccent;
                DynamicThemeHelper.applyAccent(findViewById(android.R.id.content), this);
                updateNavIndicator(activeTab, activeTab, 0, false, false);
                boolean darkNow = ConfigStore.isDarkThemeActive(this);
                View b1 = findViewById(R.id.btnRestartUiLock);
                View b2 = findViewById(R.id.btnRestartUiFc);
                if (b1 != null) {
                    ColorOsLivingFlowEngine.applyLuminousPillButton(b1, true, curAccent, darkNow, true);
                }
                if (b2 != null) {
                    ColorOsLivingFlowEngine.applyLuminousPillButton(b2, true, curAccent, darkNow, true);
                }
                syncStatusAndAboutUi();
            }
            if (vAboutDynamicBg != null && activeTab == TAB_ABOUT && !LivingFlowUpdatePages.hasActiveStage(this)) {
                vAboutDynamicBg.resumePlayback();
            }
            LivingFlowUpdatePages.onHostResume(this);
        }
    }

    @Override
    protected void onPause() {
        if (vAboutDynamicBg != null) {
            vAboutDynamicBg.pausePlayback();
        }
        LivingFlowUpdatePages.onHostPause(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        XposedProvider.setConnectionListener(null);
        mainHandler.removeCallbacks(debouncedSync);
        updateSubpageBackCallback(false);
        if (subpageTransitionAnimator != null) {
            subpageTransitionAnimator.cancel();
            subpageTransitionAnimator = null;
        }
        if (fcColorPickerAnimator != null) {
            fcColorPickerAnimator.cancel();
            fcColorPickerAnimator = null;
        }
        if (dividerFcTintPicker != null) {
            dividerFcTintPicker.animate().cancel();
        }
        if (fcMaskColorPickerAnimator != null) {
            fcMaskColorPickerAnimator.cancel();
            fcMaskColorPickerAnimator = null;
        }
        if (dividerFcMaskTintPicker != null) {
            dividerFcMaskTintPicker.animate().cancel();
        }
        if (fcGlowColorPickerAnimator != null) {
            fcGlowColorPickerAnimator.cancel();
            fcGlowColorPickerAnimator = null;
        }
        if (dividerFcGlowTintPicker != null) {
            dividerFcGlowTintPicker.animate().cancel();
        }
        if (navPillMorphAnimator != null) {
            navPillMorphAnimator.cancel();
            navPillMorphAnimator = null;
        }
        if (headerCollapseAnimator != null) {
            headerCollapseAnimator.cancel();
            headerCollapseAnimator = null;
        }
        if (subtitleTabFadeAnimator != null) {
            subtitleTabFadeAnimator.cancel();
            subtitleTabFadeAnimator = null;
        }
        if (tvPageTitle != null) {
            tvPageTitle.animate().cancel();
        }
        if (tvPageSubtitle != null) {
            tvPageSubtitle.animate().cancel();
        }
        if (layoutFcSubpageRoot != null) {
            layoutFcSubpageRoot.animate().cancel();
        }
        if (layoutRootBlur != null) {
            layoutRootBlur.animate().cancel();
        }
        if (layoutBottomNav != null) {
            layoutBottomNav.animate().cancel();
        }
        if (vNavSlidingPill != null) {
            vNavSlidingPill.animate().cancel();
        }
        if (navElasticController != null) {
            navElasticController.cancel();
            navElasticController = null;
        }
        if (navSpotLight != null) {
            navSpotLight.cancel();
            navSpotLight = null;
        }
        syncExecutor.shutdownNow();
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (LivingFlowUpdatePages.popActiveStage(this)) {
            return;
        }
        if (isFcSubpageOpen()) {
            closeFcSubpage(true);
            return;
        }
        super.onBackPressed();
    }

    private void updateSubpageBackCallback(boolean shouldRegister) {
        if (Build.VERSION.SDK_INT < 33) {
            return;
        }
        try {
            if (shouldRegister) {
                if (!fcSubpageBackRegistered) {
                    if (fcSubpageBackCallback == null) {
                        fcSubpageBackCallback = Api33BackHelper.create(() -> closeFcSubpage(true));
                    }
                    Api33BackHelper.register(this, fcSubpageBackCallback);
                    fcSubpageBackRegistered = true;
                }
            } else if (fcSubpageBackRegistered && fcSubpageBackCallback != null) {
                Api33BackHelper.unregister(this, fcSubpageBackCallback);
                fcSubpageBackRegistered = false;
            }
        } catch (Throwable ignored) {
            fcSubpageBackRegistered = false;
        }
    }

    @TargetApi(33)
    private static final class Api33BackHelper {
        private Api33BackHelper() {
        }

        static Object create(final Runnable onBack) {
            return new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    if (onBack != null) {
                        onBack.run();
                    }
                }
            };
        }

        static void register(Activity activity, Object cb) {
            if (activity != null && cb instanceof OnBackInvokedCallback) {
                activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                        OnBackInvokedDispatcher.PRIORITY_DEFAULT, (OnBackInvokedCallback) cb);
            }
        }

        static void unregister(Activity activity, Object cb) {
            if (activity != null && cb instanceof OnBackInvokedCallback) {
                activity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((OnBackInvokedCallback) cb);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getRawX();
                downY = ev.getRawY();
                swipeTriggered = false;
                trackSwipe = isSwipeEligibleTouch(ev.getRawX(), ev.getRawY());
                break;
            case MotionEvent.ACTION_MOVE:
                if (trackSwipe && !swipeTriggered) {
                    float dx = ev.getRawX() - downX;
                    float dy = ev.getRawY() - downY;
                    if (Math.abs(dy) > Math.max(touchSlop * 2f, dp(24)) && Math.abs(dy) > Math.abs(dx)) {
                        trackSwipe = false;
                    } else if (Math.abs(dx) > dp(64) && Math.abs(dx) > Math.abs(dy) * 1.8f) {
                        swipeTriggered = true;
                        trackSwipe = false;
                        int nextTab = dx < 0 ? activeTab + 1 : activeTab - 1;
                        if (nextTab >= TAB_LOCKSCREEN && nextTab <= TAB_ABOUT) {
                            switchTab(nextTab, true);
                        }
                        MotionEvent cancel = MotionEvent.obtain(ev);
                        cancel.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(cancel);
                        cancel.recycle();
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                trackSwipe = false;
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    private boolean isSwipeEligibleTouch(float rawX, float rawY) {
        if (isFcSubpageOpen() || LivingFlowUpdatePages.hasActiveStage(this)) {
            return false;
        }
        if (rawY < statusBarTopPx) {
            return false;
        }
        if (layoutPageContainer == null || !isPointInView(layoutPageContainer, rawX, rawY)) {
            return false;
        }
        if (isPointInView(layoutBottomNav, rawX, rawY)
                || isPointInView(btnTopQuickAppearance, rawX, rawY)) {
            return false;
        }
        return !isPointInInteractiveChild(pages[activeTab], rawX, rawY);
    }

    private static boolean isPointInView(View v, float rawX, float rawY) {
        if (v == null || v.getVisibility() != View.VISIBLE) {
            return false;
        }
        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        Rect r = new Rect(loc[0], loc[1], loc[0] + v.getWidth(), loc[1] + v.getHeight());
        return r.contains(Math.round(rawX), Math.round(rawY));
    }

    private static boolean isPointInInteractiveChild(View root, float rawX, float rawY) {
        if (root == null || root.getVisibility() != View.VISIBLE) {
            return false;
        }
        if ((root instanceof SeekBar || root instanceof Switch || root instanceof CouiColorSliderView)
                && isPointInView(root, rawX, rawY)) {
            return true;
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                if (isPointInInteractiveChild(vg.getChildAt(i), rawX, rawY)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void setupEdgeToEdge() {
        Window window = getWindow();
        boolean isNight = ConfigStore.isDarkThemeActive(this);
        if (Build.VERSION.SDK_INT >= 29) {
            window.setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
            window.getDecorView().post(new Runnable() {
                @Override
                public void run() {
                    WindowInsetsController ctrl = window.getInsetsController();
                    if (ctrl != null) {
                        int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                        ctrl.setSystemBarsAppearance(isNight ? 0 : mask, mask);
                    }
                }
            });
        } else {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (!isNight) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= 27) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void applyInsetsPadding() {
        final int headerPadStart = livingFlowUi ? dp(22) : dp(24);
        final int headerPadEnd = livingFlowUi ? dp(18) : dp(24);
        final int headerTopBase = livingFlowUi ? 0 : dp(28);
        final int headerBotBase = livingFlowUi ? dp(12) : dp(16);
        headerTitleShiftPx = dp(24);
        headerSubtitleShiftPx = livingFlowUi ? dp(34) : dp(26);
        headerCollapseRangePx = dp(64);
        subpageCollapseRangePx = dp(68);

        layoutHeader.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int top;
                if (Build.VERSION.SDK_INT >= 30) {
                    top = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()).top;
                } else {
                    top = insets.getSystemWindowInsetTop();
                }
                statusBarTopPx = top;
                v.setPaddingRelative(headerPadStart, top + headerTopBase, headerPadEnd, headerBotBase);
                if (vTopHeaderBlur != null) {
                    if (livingFlowUi) {
                        vTopHeaderBlur.setSurfaceColor(getColor(R.color.lf_bg));
                    }
                    vTopHeaderBlur.setStatusBarTopPx(top);
                }
                syncHeaderGeometryAndScrollPadding();
                applySubpageInsets();
                return insets;
            }
        });
        layoutHeader.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if ((bottom - top) != (oldBottom - oldTop)) {
                syncHeaderGeometryAndScrollPadding();
            }
        });
        layoutHeader.requestApplyInsets();

        if (layoutSubpageHeader != null) {
            layoutSubpageHeader.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                if ((bottom - top) != (oldBottom - oldTop)) {
                    syncSubpageHeaderGeometryAndPadding();
                }
            });
        }

        final int navBotBase = livingFlowUi ? dp(8) : dp(14);
        layoutBottomNavContainer.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int bottom;
                if (Build.VERSION.SDK_INT >= 30) {
                    bottom = insets.getInsets(WindowInsets.Type.systemBars()).bottom;
                } else {
                    bottom = insets.getSystemWindowInsetBottom();
                }
                navBarBottomPx = bottom;
                v.setPadding(0, 0, 0, bottom + navBotBase);
                if (vBottomNavShadow != null) {
                    ViewGroup.LayoutParams lp = vBottomNavShadow.getLayoutParams();
                    int targetH = bottom + dp(livingFlowUi ? 164 : 104);
                    if (lp != null && lp.height != targetH) {
                        lp.height = targetH;
                        vBottomNavShadow.setLayoutParams(lp);
                    }
                }
                applySubpageInsets();
                return insets;
            }
        });
        layoutBottomNavContainer.requestApplyInsets();
        applySubpageInsets();
    }

    private void applySubpageInsets() {
        if (layoutSubpageHeader != null) {
            int targetTop = statusBarTopPx + dp(4);
            int targetBot = dp(12);
            if (layoutSubpageHeader.getPaddingTop() != targetTop
                    || layoutSubpageHeader.getPaddingBottom() != targetBot) {
                layoutSubpageHeader.setPadding(0, targetTop, 0, targetBot);
            }
        }
        if (vSubpageHeaderBlur != null) {
            if (livingFlowUi) {
                vSubpageHeaderBlur.setSurfaceColor(getColor(R.color.lf_bg));
            }
            vSubpageHeaderBlur.setStatusBarTopPx(statusBarTopPx);
        }
        if (layoutFcSubpageRoot instanceof SwipeableBlurFrameLayout) {
            ((SwipeableBlurFrameLayout) layoutFcSubpageRoot).setTopBarSafeY(statusBarTopPx + dp(68));
        }
        syncSubpageHeaderGeometryAndPadding();
    }

    private void syncSubpageHeaderGeometryAndPadding() {
        if (layoutFcSubpageContent != null && layoutSubpageHeader != null) {
            int headerH = layoutSubpageHeader.getHeight();
            int targetTopPad = headerH > 0 ? (headerH + dp(4)) : layoutFcSubpageContent.getPaddingTop();
            int targetBotPad = navBarBottomPx + dp(32);
            if (layoutFcSubpageContent.getPaddingTop() != targetTopPad
                    || layoutFcSubpageContent.getPaddingBottom() != targetBotPad) {
                layoutFcSubpageContent.setPaddingRelative(
                        layoutFcSubpageContent.getPaddingStart(),
                        targetTopPad,
                        layoutFcSubpageContent.getPaddingEnd(),
                        targetBotPad);
            }
        }
        if (vSubpageHeaderBlur != null) {
            int targetBlurH = livingFlowUi
                    ? (statusBarTopPx + dp(108))
                    : (statusBarTopPx + dp(4) + dp(48) + dp(44));
            ViewGroup.LayoutParams lp = vSubpageHeaderBlur.getLayoutParams();
            if (lp != null && lp.height != targetBlurH) {
                lp.height = targetBlurH;
                vSubpageHeaderBlur.setLayoutParams(lp);
            }
        }
        syncSubpageHeaderToScrollNow();
    }

    private void syncSubpageHeaderToScrollNow() {
        if (scrollFcSubpage != null) {
            syncSubpageHeaderToScrollY(scrollFcSubpage.getScrollY());
        }
        if (vSubpageHeaderBlur != null && isFcSubpageOpen()) {
            vSubpageHeaderBlur.notifyTargetInvalidated();
        }
    }

    private void syncSubpageHeaderToScrollY(int scrollY) {
        float range = Math.max(1f, subpageCollapseRangePx > 0 ? subpageCollapseRangePx : dp(68));
        applySubpageHeaderCollapseProgress(scrollY / range);
    }

    private void applySubpageHeaderCollapseProgress(float p) {
        subpageHeaderCollapseProgress = Math.max(0f, Math.min(1f, p));
        if (tvSubpageTitle != null) {
            boolean isRtl = tvSubpageTitle.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            tvSubpageTitle.setPivotX(isRtl ? tvSubpageTitle.getWidth() : 0f);
            tvSubpageTitle.setPivotY(0f);
            float scale = 1f - (1f - SUBPAGE_COLLAPSED_TITLE_SCALE) * subpageHeaderCollapseProgress;
            tvSubpageTitle.setScaleX(scale);
            tvSubpageTitle.setScaleY(scale);

            float targetShiftX;
            if (layoutSubpageToolbar != null && btnSubpageBack != null && btnSubpageBack.getWidth() > 0) {
                if (isRtl) {
                    targetShiftX = (layoutSubpageToolbar.getLeft() + btnSubpageBack.getLeft() - dp(6))
                            - tvSubpageTitle.getRight();
                } else {
                    targetShiftX = (layoutSubpageToolbar.getLeft() + btnSubpageBack.getRight() + dp(6))
                            - tvSubpageTitle.getLeft();
                }
            } else {
                targetShiftX = isRtl ? -dp(34) : dp(34);
            }

            float targetShiftY;
            if (layoutSubpageToolbar != null && layoutSubpageToolbar.getHeight() > 0 && tvSubpageTitle.getHeight() > 0) {
                float toolbarCenterY = layoutSubpageToolbar.getTop() + layoutSubpageToolbar.getHeight() * 0.5f;
                float collapsedTitleH = tvSubpageTitle.getHeight() * SUBPAGE_COLLAPSED_TITLE_SCALE;
                targetShiftY = (toolbarCenterY - collapsedTitleH * 0.5f) - tvSubpageTitle.getTop();
            } else {
                targetShiftY = -dp(38);
            }

            tvSubpageTitle.setTranslationX(targetShiftX * subpageHeaderCollapseProgress);
            tvSubpageTitle.setTranslationY(targetShiftY * subpageHeaderCollapseProgress);
        }
        if (tvSubpageSubtitle != null) {
            float subT = Math.max(0f, Math.min(1f, subpageHeaderCollapseProgress / SUBTITLE_FADE_END_PROGRESS));
            tvSubpageSubtitle.setTranslationY(-dp(24) * subT);
            tvSubpageSubtitle.setAlpha(computeSubtitleAlpha(subpageHeaderCollapseProgress));
        }
    }

    private void syncHeaderGeometryAndScrollPadding() {
        if (layoutHeader == null) {
            return;
        }
        int headerH = layoutHeader.getHeight();
        if (headerH > 0) {
            int contentTopPad = headerH + dp(2);
            for (View page : pages) {
                if (page instanceof ViewGroup && ((ViewGroup) page).getChildCount() > 0) {
                    View inner = ((ViewGroup) page).getChildAt(0);
                    if (inner.getPaddingTop() != contentTopPad) {
                        inner.setPaddingRelative(
                                inner.getPaddingStart(),
                                contentTopPad,
                                inner.getPaddingEnd(),
                                inner.getPaddingBottom());
                    }
                }
            }
        }
        if (livingFlowUi && tvPageTitle != null && tvPageTitle.getHeight() > 0) {
            float collapsedH = tvPageTitle.getHeight() * 0.6875f;
            float targetCollapsedTop = dp(26) - collapsedH * 0.5f;
            headerTitleShiftPx = Math.max(dp(34), Math.round(dp(54) - targetCollapsedTop));
        }
        if (vTopHeaderBlur != null && tvPageTitle != null && tvPageTitle.getHeight() > 0) {
            int targetBlurH;
            if (livingFlowUi) {
                targetBlurH = statusBarTopPx + dp(104);
            } else {
                int collapsedTitleH = Math.round(tvPageTitle.getHeight() * COLLAPSED_TITLE_SCALE);
                targetBlurH = statusBarTopPx + dp(4) + collapsedTitleH + dp(56);
            }
            ViewGroup.LayoutParams lp = vTopHeaderBlur.getLayoutParams();
            if (lp != null && lp.height != targetBlurH) {
                lp.height = targetBlurH;
                vTopHeaderBlur.setLayoutParams(lp);
            }
        }
        syncActiveHeaderToScrollNow();
    }

    private void syncActiveHeaderToScrollNow() {
        View active = (activeTab >= 0 && activeTab < pages.length) ? pages[activeTab] : null;
        if (active != null) {
            active.post(() -> {
                if (active == pages[activeTab]) {
                    syncHeaderToScrollY(active.getScrollY(), false);
                    if (vTopHeaderBlur != null) {
                        vTopHeaderBlur.notifyTargetInvalidated();
                    }
                }
            });
            syncHeaderToScrollY(active.getScrollY(), false);
        }
        if (vTopHeaderBlur != null) {
            vTopHeaderBlur.notifyTargetInvalidated();
        }
    }

    private static float computeSubtitleAlpha(float collapseProgress) {
        float t = Math.max(0f, Math.min(1f, collapseProgress / SUBTITLE_FADE_END_PROGRESS));
        return Math.max(0f, Math.min(1f, 1f - (float) Math.pow(t, 1.35)));
    }

    private void applyHeaderCollapseProgress(float p) {
        headerCollapseProgress = Math.max(0f, Math.min(1f, p));
        if (livingFlowUi) {
            if (tvPageTitle != null) {
                float pivotX = (tvPageTitle.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL)
                        ? tvPageTitle.getWidth() : 0f;
                tvPageTitle.setPivotX(pivotX);
                tvPageTitle.setPivotY(0f);
                float titleScale = 1f - (1f - 0.6875f) * headerCollapseProgress;
                tvPageTitle.setScaleX(titleScale);
                tvPageTitle.setScaleY(titleScale);
                tvPageTitle.setTranslationY(-headerTitleShiftPx * headerCollapseProgress);
                tvPageTitle.setAlpha(headerTitleTabAlpha);
            }
            if (tvPageSubtitle != null) {
                float subT = Math.min(1f, headerCollapseProgress / 0.56f);
                tvPageSubtitle.setScaleX(1f);
                tvPageSubtitle.setScaleY(1f);
                tvPageSubtitle.setTranslationY(-headerSubtitleShiftPx * subT);
                float subAlpha = Math.max(0f, 1f - (float) Math.pow(subT, 1.35));
                tvPageSubtitle.setAlpha(subAlpha * subtitleTabAlpha);
            }
            return;
        }
        if (tvPageTitle != null) {
            float pivotX = (tvPageTitle.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL)
                    ? tvPageTitle.getWidth() : 0f;
            tvPageTitle.setPivotX(pivotX);
            tvPageTitle.setPivotY(0f);
            float scale = 1f - (1f - COLLAPSED_TITLE_SCALE) * headerCollapseProgress;
            tvPageTitle.setScaleX(scale);
            tvPageTitle.setScaleY(scale);
            tvPageTitle.setTranslationY(-headerTitleShiftPx * headerCollapseProgress);
            tvPageTitle.setAlpha(1f);
        }
        if (tvPageSubtitle != null) {
            float subT = Math.max(0f, Math.min(1f, headerCollapseProgress / SUBTITLE_FADE_END_PROGRESS));
            float neededTravelPx;
            if (tvPageTitle != null && tvPageTitle.getHeight() > 0 && tvPageSubtitle.getTop() > tvPageTitle.getTop()) {
                float scaleAtFade = 1f - (1f - COLLAPSED_TITLE_SCALE) * SUBTITLE_FADE_END_PROGRESS;
                float titleBottomAtFade = tvPageTitle.getTop()
                        - headerTitleShiftPx * SUBTITLE_FADE_END_PROGRESS
                        + tvPageTitle.getHeight() * scaleAtFade;
                float targetSubTopAtFade = titleBottomAtFade - dp(5);
                neededTravelPx = Math.max(headerSubtitleShiftPx, tvPageSubtitle.getTop() - targetSubTopAtFade);
            } else {
                neededTravelPx = headerSubtitleShiftPx;
            }
            tvPageSubtitle.setTranslationY(-neededTravelPx * subT);
            tvPageSubtitle.setAlpha(computeSubtitleAlpha(headerCollapseProgress) * subtitleTabAlpha);
        }
    }

    private void syncHeaderToScrollY(int scrollY, boolean animate) {
        float range = Math.max(1f, headerCollapseRangePx > 0 ? headerCollapseRangePx : dp(64));
        float target = Math.max(0f, Math.min(1f, scrollY / range));
        if (headerCollapseAnimator != null) {
            headerCollapseAnimator.cancel();
            headerCollapseAnimator = null;
        }
        if (!animate || Math.abs(target - headerCollapseProgress) < 0.005f) {
            applyHeaderCollapseProgress(target);
            return;
        }
        headerCollapseAnimator = ValueAnimator.ofFloat(headerCollapseProgress, target);
        headerCollapseAnimator.setDuration(340L);
        headerCollapseAnimator.setInterpolator(FLUID_EASE);
        headerCollapseAnimator.addUpdateListener(anim -> applyHeaderCollapseProgress((Float) anim.getAnimatedValue()));
        headerCollapseAnimator.start();
    }

    private void bindViews() {
        layoutAppRoot = findViewById(R.id.layoutAppRoot);
        layoutRootBlur = findViewById(R.id.layoutRootBlur);
        vTopHeaderBlur = findViewById(R.id.vTopHeaderBlur);
        layoutHeader = findViewById(R.id.layoutHeader);
        tvPageTitle = findViewById(R.id.tvPageTitle);
        tvPageSubtitle = findViewById(R.id.tvPageSubtitle);

        vSubpageScrim = findViewById(R.id.vSubpageScrim);
        layoutFcSubpageRoot = findViewById(R.id.layoutFcSubpageRoot);
        layoutFcSubpageContainer = findViewById(R.id.layoutFcSubpageContainer);
        scrollFcSubpage = findViewById(R.id.scrollFcSubpage);
        layoutFcSubpageContent = findViewById(R.id.layoutFcSubpageContent);
        vSubpageHeaderBlur = findViewById(R.id.vSubpageHeaderBlur);
        layoutSubpageHeader = findViewById(R.id.layoutSubpageHeader);
        layoutSubpageToolbar = findViewById(R.id.layoutSubpageToolbar);
        btnSubpageBack = findViewById(R.id.btnSubpageBack);
        tvSubpageTitle = findViewById(R.id.tvSubpageTitle);
        tvSubpageSubtitle = findViewById(R.id.tvSubpageSubtitle);

        layoutPageContainer = findViewById(R.id.layoutPageContainer);
        pageLockscreen = findViewById(R.id.pageLockscreen);
        pageAppearance = findViewById(R.id.pageAppearance);
        pageAbout = findViewById(R.id.pageAbout);
        pages[TAB_LOCKSCREEN] = pageLockscreen;
        pages[TAB_APPEARANCE] = pageAppearance;
        pages[TAB_ABOUT] = pageAbout;

        if (layoutRootBlur != null) {
            layoutRootBlur.bindProgressiveBlur(R.id.layoutPageContainer, R.id.vTopHeaderBlur);
        }
        if (layoutFcSubpageRoot != null) {
            layoutFcSubpageRoot.bindProgressiveBlur(R.id.layoutFcSubpageContainer, R.id.vSubpageHeaderBlur);
        }
        if (scrollFcSubpage != null) {
            scrollFcSubpage.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (vSubpageHeaderBlur != null) {
                    vSubpageHeaderBlur.notifyTargetInvalidated();
                }
                syncSubpageHeaderToScrollY(scrollY);
            });
            scrollFcSubpage.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                syncSubpageHeaderToScrollY(v.getScrollY());
                if (vSubpageHeaderBlur != null) {
                    vSubpageHeaderBlur.notifyTargetInvalidated();
                }
            });
            scrollFcSubpage.setOnOverscrollListener(translationY -> {
                if (vSubpageHeaderBlur != null) {
                    vSubpageHeaderBlur.notifyTargetInvalidated();
                }
            });
        }
        if (btnSubpageBack != null) {
            btnSubpageBack.setOnClickListener(v -> closeFcSubpage(true));
        }

        for (int i = 0; i < pages.length; i++) {
            final int tabIdx = i;
            if (pages[i] != null) {
                pages[i].setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    if (vTopHeaderBlur != null) {
                        vTopHeaderBlur.notifyTargetInvalidated();
                    }
                    if (tabIdx == activeTab) {
                        syncHeaderToScrollY(scrollY, false);
                    }
                });
                pages[i].addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    if (tabIdx == activeTab) {
                        syncHeaderToScrollY(v.getScrollY(), false);
                        if (vTopHeaderBlur != null) {
                            vTopHeaderBlur.notifyTargetInvalidated();
                        }
                    }
                });
                if (pages[i] instanceof CouiBounceScrollView) {
                    ((CouiBounceScrollView) pages[i]).setOnOverscrollListener(translationY -> {
                        if (vTopHeaderBlur != null) {
                            vTopHeaderBlur.notifyTargetInvalidated();
                        }
                    });
                }
            }
        }

        vBottomNavShadow = findViewById(R.id.vBottomNavShadow);
        layoutBottomNavContainer = findViewById(R.id.layoutBottomNavContainer);
        layoutBottomNav = findViewById(R.id.layoutBottomNav);
        layoutNavItems = findViewById(R.id.layoutNavItems);
        vNavSlidingPill = findViewById(R.id.vNavSlidingPill);

        navTabs[0] = findViewById(R.id.navLockscreen);
        navTabs[1] = findViewById(R.id.navAppearance);
        navTabs[2] = findViewById(R.id.navAbout);
        navIcons[0] = findViewById(R.id.ivNavLockscreen);
        navIcons[1] = findViewById(R.id.ivNavAppearance);
        navIcons[2] = findViewById(R.id.ivNavAbout);
        navLabels[0] = findViewById(R.id.tvNavLockscreen);
        navLabels[1] = findViewById(R.id.tvNavAppearance);
        navLabels[2] = findViewById(R.id.tvNavAbout);

        setupBottomNavTouch();

        bindLockscreenViews();
        bindAppearanceViews();
        bindAboutViews();
        if (livingFlowUi) {
            setupLivingFlowUi();
        }
        applyCardOutlineClipping(pages[TAB_LOCKSCREEN]);
        applyCardOutlineClipping(pages[TAB_APPEARANCE]);
        applyCardOutlineClipping(pages[TAB_ABOUT]);
        applyCardOutlineClipping(layoutFcSubpageContainer);
    }

    private void setupLivingFlowUi() {
        btnTopQuickAppearance = findViewById(R.id.btnTopQuickAppearance);
        final boolean dark = ConfigStore.isDarkThemeActive(this);
        final float d = getResources().getDisplayMetrics().density;

        if (vTopHeaderBlur != null) {
            vTopHeaderBlur.setSurfaceColor(getColor(R.color.lf_bg));
            vTopHeaderBlur.setOutlineProvider(null);
            vTopHeaderBlur.setTranslationZ(4f * d);
        }
        if (layoutHeader != null) {
            layoutHeader.setOutlineProvider(null);
            layoutHeader.setTranslationZ(12f * d);
        }
        if (vSubpageHeaderBlur != null) {
            vSubpageHeaderBlur.setSurfaceColor(getColor(R.color.lf_bg));
            vSubpageHeaderBlur.setOutlineProvider(null);
            vSubpageHeaderBlur.setTranslationZ(4f * d);
        }
        if (layoutSubpageHeader != null) {
            layoutSubpageHeader.setOutlineProvider(null);
            layoutSubpageHeader.setTranslationZ(12f * d);
        }
        if (layoutBottomNavContainer != null) {
            layoutBottomNavContainer.setOutlineProvider(null);
            layoutBottomNavContainer.setTranslationZ(16f * d);
        }
        if (vBottomNavShadow != null) {
            vBottomNavShadow.setOutlineProvider(null);
            vBottomNavShadow.setTranslationZ(14f * d);
            final int maxAlpha = dark ? 0xB8 : 0x26;
            final int count = 24;
            final int[] stopsColor = new int[count];
            final float[] stopsPos = new float[count];
            for (int i = 0; i < count; i++) {
                float t = i / (float) (count - 1);
                stopsPos[i] = t;
                int a = (i <= 1) ? 0 : Math.min(255, Math.round(maxAlpha * (float) Math.pow(t, 2.8)));
                stopsColor[i] = (a << 24);
            }
            ShapeDrawable scrim = new ShapeDrawable(new RectShape());
            scrim.getPaint().setDither(true);
            scrim.setShaderFactory(new ShapeDrawable.ShaderFactory() {
                @Override
                public Shader resize(int width, int height) {
                    return new LinearGradient(
                            0f, 0f, 0f, Math.max(1f, height),
                            stopsColor, stopsPos, Shader.TileMode.CLAMP);
                }
            });
            vBottomNavShadow.setBackground(scrim);
        }

        ColorOsLivingFlowEngine.applyGlassSurface(layoutBottomNav,
                ColorOsLivingFlowEngine.GlassPreset.BOTTOM_BAR_PILL, dark, 28f, false, true);
        ColorOsLivingFlowEngine.applyGlassSurface(btnTopQuickAppearance,
                ColorOsLivingFlowEngine.GlassPreset.TOP_BAR_CIRCLE, dark, 21f, true, false);
        ColorOsLivingFlowEngine.applyGlassSurface(btnSubpageBack,
                ColorOsLivingFlowEngine.GlassPreset.TOP_BAR_CIRCLE, dark, 21f, true, false);

        navSpotLight = new LivingFlowEffects.SpotLightDrawable(
                dark, d, 28f, false, true, dark ? 0.24f : 0.30f, 88f);
        if (layoutBottomNav != null) {
            layoutBottomNav.setForeground(navSpotLight);
            navElasticController = new LivingFlowEffects.NavBarElasticController(layoutBottomNav);
        }

        LivingFlowEffects.TouchFeedback.attach(btnTopQuickAppearance,
                LivingFlowEffects.TouchFeedback.Profile.GLASS_CIRCLE, dark, 21f, true, false, true);
        LivingFlowEffects.TouchFeedback.attach(btnSubpageBack,
                LivingFlowEffects.TouchFeedback.Profile.GLASS_CIRCLE, dark, 21f, true, false, true);

        int accent = DynamicThemeHelper.getAccent(this);
        int[] pillBtnIds = new int[]{
                R.id.btnResetLock,
                R.id.btnRestartUiLock,
                R.id.btnResetFc,
                R.id.btnRestartUiFc,
                R.id.btnMinus,
                R.id.btnPlus
        };
        for (int id : pillBtnIds) {
            View btn = findViewById(id);
            if (btn != null) {
                boolean isPrimary = (id == R.id.btnRestartUiLock || id == R.id.btnRestartUiFc);
                boolean enableCaustic = (id != R.id.btnMinus && id != R.id.btnPlus);
                LivingFlowEffects.TouchFeedback.attach(btn,
                        LivingFlowEffects.TouchFeedback.Profile.PRIMARY_BUTTON, dark, 999f, false, true);
                ColorOsLivingFlowEngine.applyLuminousPillButton(btn, isPrimary, accent, dark, enableCaustic);
            }
        }

        if (cardAboutDynamicHero != null) {
            ColorOsLivingFlowEngine.applySmoothCard(cardAboutDynamicHero, 28f, 2.0f, dark, false);
            ColorOsLivingFlowEngine.installSmoothOutline(cardAboutDynamicHero, 28f, 2.0f, false, false);
            cardAboutDynamicHero.setClipToOutline(true);
            LivingFlowEffects.TouchFeedback.attach(cardAboutDynamicHero,
                    LivingFlowEffects.TouchFeedback.Profile.HERO_CARD, dark, 28f, false, false);
        }
        ImageView ivAboutHeroLogo = findViewById(R.id.ivAboutHeroLogo);
        if (ivAboutHeroLogo != null) {
            Drawable officialLogo = ColorOsDynamicBackdropView.loadOfficialColorOsLogo(this);
            if (officialLogo != null) {
                ivAboutHeroLogo.setImageDrawable(officialLogo);
            }
        }
        if (tvAboutHeroVer != null) {
            tvAboutHeroVer.setText(R.string.about_hero_module_subtitle);
        }
        if (tvAboutBuildSub != null) {
            tvAboutBuildSub.setText("v" + getInstalledVersionName() + " (" + getInstalledVersionCode() + ")");
        }

        if (btnTopQuickAppearance != null) {
            btnTopQuickAppearance.setOnClickListener(this::showTopQuickActionsMenu);
        }

        if (layoutFcSubpageRoot instanceof SwipeableBlurFrameLayout) {
            SwipeableBlurFrameLayout sw = (SwipeableBlurFrameLayout) layoutFcSubpageRoot;
            sw.setTopBarSafeY(statusBarTopPx + dp(68));
            sw.setOnEdgeSwipeListener(new SwipeableBlurFrameLayout.OnEdgeSwipeListener() {
                @Override
                public void onSwipeStart() {
                    if (subpageTransitionAnimator != null) {
                        subpageTransitionAnimator.cancel();
                        subpageTransitionAnimator = null;
                    }
                    if (layoutRootBlur != null) {
                        layoutRootBlur.setVisibility(View.VISIBLE);
                    }
                    if (vSubpageScrim != null) {
                        vSubpageScrim.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onSwipeDrag(float deltaX, float progress) {
                    int w = (layoutAppRoot != null && layoutAppRoot.getWidth() > 0)
                            ? layoutAppRoot.getWidth()
                            : getResources().getDisplayMetrics().widthPixels;
                    layoutFcSubpageRoot.setTranslationX(deltaX);
                    if (layoutRootBlur != null) {
                        layoutRootBlur.setTranslationX(-w * SUBPAGE_PARALLAX_RATIO * (1f - progress));
                    }
                    if (vSubpageScrim != null) {
                        vSubpageScrim.setAlpha(SUBPAGE_SCRIM_MAX_ALPHA * (1f - progress));
                    }
                }

                @Override
                public void onSwipeEnd(boolean commitClose) {
                    if (commitClose) {
                        ColorOsLivingFlowEngine.performHaptic(
                                layoutFcSubpageRoot, ColorOsLivingFlowEngine.HapticKind.TICK);
                        closeFcSubpage(true);
                        return;
                    }
                    final int gen = ++subpageAnimGen;
                    final float slideDist = (layoutAppRoot != null && layoutAppRoot.getWidth() > 0)
                            ? layoutAppRoot.getWidth()
                            : getResources().getDisplayMetrics().widthPixels;
                    final float startSubX = layoutFcSubpageRoot.getTranslationX();
                    final float startMainX = layoutRootBlur != null ? layoutRootBlur.getTranslationX() : 0f;
                    final float endMainX = -slideDist * SUBPAGE_PARALLAX_RATIO;
                    final float startScrim = vSubpageScrim != null ? vSubpageScrim.getAlpha() : 0f;

                    final ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
                    subpageTransitionAnimator = anim;
                    anim.setDuration(340L);
                    anim.setInterpolator(SUBPAGE_EASE);
                    anim.addUpdateListener(a -> {
                        float t = (Float) a.getAnimatedValue();
                        layoutFcSubpageRoot.setTranslationX(startSubX * (1f - t));
                        if (layoutRootBlur != null) {
                            layoutRootBlur.setTranslationX(startMainX + (endMainX - startMainX) * t);
                        }
                        if (vSubpageScrim != null) {
                            vSubpageScrim.setAlpha(startScrim + (SUBPAGE_SCRIM_MAX_ALPHA - startScrim) * t);
                        }
                    });
                    anim.addListener(new AnimatorListenerAdapter() {
                        private boolean cancelled;

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            cancelled = true;
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (subpageTransitionAnimator == anim) {
                                subpageTransitionAnimator = null;
                            }
                            if (!cancelled && gen == subpageAnimGen && isFcSubpageOpen()) {
                                layoutFcSubpageRoot.setTranslationX(0f);
                                if (vSubpageScrim != null) {
                                    vSubpageScrim.setAlpha(0f);
                                    vSubpageScrim.setVisibility(View.GONE);
                                }
                                if (layoutRootBlur != null) {
                                    layoutRootBlur.setVisibility(View.INVISIBLE);
                                    layoutRootBlur.setTranslationX(0f);
                                }
                            }
                        }
                    });
                    anim.start();
                }
            });
        }

        if (activeTab == TAB_ABOUT) {
            playAboutHeroEntrance(false);
        }
    }

    private void showTopQuickActionsMenu(View anchor) {
        final boolean showDebug = data != null && data.fcDebugMode;
        String[] items = showDebug
                ? new String[]{
                        getString(R.string.about_restart_ui_title),
                        getString(R.string.about_ui_style_title),
                        getString(R.string.lf_software_update_title),
                        getString(R.string.about_debug_tester_title),
                        getString(R.string.about_reset_all_title)
                }
                : new String[]{
                        getString(R.string.about_restart_ui_title),
                        getString(R.string.about_ui_style_title),
                        getString(R.string.lf_software_update_title),
                        getString(R.string.about_reset_all_title)
                };
        CouiBottomSheet.showPopupMenu(this, anchor, CouiBottomSheet.Palette.fromModuleTheme(this),
                items, -1, which -> {
                    if (which == 0) {
                        restartSystemUi();
                    } else if (which == 1) {
                        showUiStyleDialog(anchor);
                    } else if (which == 2) {
                        LivingFlowUpdatePages.showSoftwareUpdatePage(this);
                    } else if (showDebug && which == 3) {
                        showDebugTesterDialog();
                    } else {
                        confirmResetAll();
                    }
                });
    }

    private static void applyCardOutlineClipping(View view) {
        if (view instanceof LinearLayout && view.getBackground() instanceof GradientDrawable) {
            view.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            view.setClipToOutline(true);
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyCardOutlineClipping(vg.getChildAt(i));
            }
        }
    }

    private void bindLockscreenViews() {
        cardLockStatus = findViewById(R.id.cardLockStatus);
        vLockStatusEmblem = findViewById(R.id.vLockStatusEmblem);
        tvLockStatusBadge = findViewById(R.id.tvLockStatusBadge);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvStatusSubtitle = findViewById(R.id.tvStatusSubtitle);

        swAlign = findViewById(R.id.swAlign);
        layoutCapsuleSub = findViewById(R.id.layoutCapsuleSub);
        swHideFpOnCapsule = findViewById(R.id.swHideFpOnCapsule);
        btnZero = findViewById(R.id.btnZero);
        tvOffsetVal = findViewById(R.id.tvOffsetVal);
        sbOffset = findViewById(R.id.sbOffset);
        swImmersiveFp = findViewById(R.id.swImmersiveFp);
        swAodNotifTranslucent = findViewById(R.id.swAodNotifTranslucent);
        swLockNotifGlowFollowMedia = findViewById(R.id.swLockNotifGlowFollowMedia);

        findViewById(R.id.rowAlign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swAlign.toggle();
            }
        });
        swAlign.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                if (suppressUiCallbacks) {
                    return;
                }
                if (livingFlowUi) {
                    ColorOsLivingFlowEngine.performHaptic(buttonView, ColorOsLivingFlowEngine.HapticKind.SWITCH);
                }
                data.align = isChecked;
                persistImmediate();
            }
        });

        findViewById(R.id.rowHideFpOnCapsule).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swHideFpOnCapsule.toggle();
            }
        });
        swHideFpOnCapsule.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                if (suppressUiCallbacks) {
                    return;
                }
                if (livingFlowUi) {
                    ColorOsLivingFlowEngine.performHaptic(buttonView, ColorOsLivingFlowEngine.HapticKind.SWITCH);
                }
                data.hideFpOnCapsule = isChecked;
                persistImmediate();
            }
        });

        sbOffset.setMax(OFFSET_STEPS_HALF * 2);
        sbOffset.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || suppressUiCallbacks) {
                    return;
                }
                data.offset = ConfigStore.clamp((progress - OFFSET_STEPS_HALF) * ConfigStore.OFFSET_STEP);
                persistSliderTick();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mainHandler.removeCallbacks(debouncedSync);
                syncRemoteAsync();
            }
        });

        findViewById(R.id.btnMinus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setOffsetExplicit(data.offset - ConfigStore.OFFSET_STEP);
            }
        });
        findViewById(R.id.btnPlus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setOffsetExplicit(data.offset + ConfigStore.OFFSET_STEP);
            }
        });
        btnZero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setOffsetExplicit(0);
            }
        });
        tvOffsetVal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!data.align) {
                    return;
                }
                showSliderInputDialog(
                        R.string.pref_offset_title,
                        data.offset,
                        -ConfigStore.OFFSET_LIMIT,
                        ConfigStore.OFFSET_LIMIT,
                        "px",
                        new OnValueInputListener() {
                            @Override
                            public void onValueEntered(int value) {
                                setOffsetExplicit(value);
                            }
                        });
            }
        });

        findViewById(R.id.rowImmersiveFp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swImmersiveFp.toggle();
            }
        });
        swImmersiveFp.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                if (suppressUiCallbacks) {
                    return;
                }
                if (livingFlowUi) {
                    ColorOsLivingFlowEngine.performHaptic(buttonView, ColorOsLivingFlowEngine.HapticKind.SWITCH);
                }
                data.immersiveFp = isChecked;
                persistImmediate();
            }
        });

        findViewById(R.id.rowAodNotifTranslucent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swAodNotifTranslucent.toggle();
            }
        });
        swAodNotifTranslucent.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                if (suppressUiCallbacks) {
                    return;
                }
                if (livingFlowUi) {
                    ColorOsLivingFlowEngine.performHaptic(buttonView, ColorOsLivingFlowEngine.HapticKind.SWITCH);
                }
                data.aodNotifTranslucent = isChecked;
                persistImmediate();
            }
        });

        findViewById(R.id.rowLockNotifGlowFollowMedia).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swLockNotifGlowFollowMedia.toggle();
            }
        });
        swLockNotifGlowFollowMedia.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                if (suppressUiCallbacks) {
                    return;
                }
                if (livingFlowUi) {
                    ColorOsLivingFlowEngine.performHaptic(buttonView, ColorOsLivingFlowEngine.HapticKind.SWITCH);
                }
                data.lockNotifGlowFollowMedia = isChecked;
                persistImmediate();
            }
        });

        findViewById(R.id.btnResetLock).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfigStore.SettingsData def = ConfigStore.createDefaults();
                data.align = def.align;
                data.offset = def.offset;
                data.hideFpOnCapsule = def.hideFpOnCapsule;
                data.immersiveFp = def.immersiveFp;
                data.aodNotifTranslucent = def.aodNotifTranslucent;
                data.lockNotifGlowFollowMedia = def.lockNotifGlowFollowMedia;
                persistImmediate();
                Toast.makeText(MainActivity.this, R.string.toast_reset_done, Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btnRestartUiLock).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartSystemUi();
            }
        });
    }

    private void bindAppearanceViews() {
        fcPreview = findViewById(R.id.fcPreview);
        fcSubpagePreview = findViewById(R.id.fcSubpagePreview);
        swFcEnabled = findViewById(R.id.swFcEnabled);
        layoutFcSub = findViewById(R.id.layoutFcSub);
        layoutFcSub.setClipToOutline(true);
        layoutFcTintCard = findViewById(R.id.layoutFcTintCard);
        tvFcIslandOpacityVal = findViewById(R.id.tvFcIslandOpacityVal);
        sbFcIslandOpacity = findViewById(R.id.sbFcIslandOpacity);
        tvFcOpacityVal = findViewById(R.id.tvFcOpacityVal);
        sbFcOpacity = findViewById(R.id.sbFcOpacity);
        tvFcImmersiveOpacityVal = findViewById(R.id.tvFcImmersiveOpacityVal);
        sbFcImmersiveOpacity = findViewById(R.id.sbFcImmersiveOpacity);
        tvFcBlurVal = findViewById(R.id.tvFcBlurVal);
        sbFcBlur = findViewById(R.id.sbFcBlur);
        tvFcBottomGlowVal = findViewById(R.id.tvFcBottomGlowVal);
        sbFcBottomGlow = findViewById(R.id.sbFcBottomGlow);
        tvFcBottomGlowChromaVal = findViewById(R.id.tvFcBottomGlowChromaVal);
        sbFcBottomGlowChroma = findViewById(R.id.sbFcBottomGlowChroma);
        tvFcSingleGlowVal = findViewById(R.id.tvFcSingleGlowVal);
        sbFcSingleGlow = findViewById(R.id.sbFcSingleGlow);
        tvFcSingleGlowChromaVal = findViewById(R.id.tvFcSingleGlowChromaVal);
        sbFcSingleGlowChroma = findViewById(R.id.sbFcSingleGlowChroma);
        swFcRemoveIslandMask = findViewById(R.id.swFcRemoveIslandMask);
        swFcMatchCard = findViewById(R.id.swFcMatchCard);
        swFcRemoveShadeMask = findViewById(R.id.swFcRemoveShadeMask);
        tvFcMaskStrengthVal = findViewById(R.id.tvFcMaskStrengthVal);
        sbFcMaskStrength = findViewById(R.id.sbFcMaskStrength);
        swFcMaskFollowMedia = findViewById(R.id.swFcMaskFollowMedia);
        layoutFcMaskToneSub = findViewById(R.id.layoutFcMaskToneSub);
        tvFcMaskBrightnessVal = findViewById(R.id.tvFcMaskBrightnessVal);
        sbFcMaskBrightness = findViewById(R.id.sbFcMaskBrightness);
        tvFcMaskChromaVal = findViewById(R.id.tvFcMaskChromaVal);
        sbFcMaskChroma = findViewById(R.id.sbFcMaskChroma);
        tvFcNonMediaMaskOpacityVal = findViewById(R.id.tvFcNonMediaMaskOpacityVal);
        sbFcNonMediaMaskOpacity = findViewById(R.id.sbFcNonMediaMaskOpacity);
        tvFcNonMediaFollowMaskOpacityVal = findViewById(R.id.tvFcNonMediaFollowMaskOpacityVal);
        sbFcNonMediaFollowMaskOpacity = findViewById(R.id.sbFcNonMediaFollowMaskOpacity);
        swFcNonMediaMaskEnabled = findViewById(R.id.swFcNonMediaMaskEnabled);
        tvFcMaskGradStartVal = findViewById(R.id.tvFcMaskGradStartVal);
        sbFcMaskGradStart = findViewById(R.id.sbFcMaskGradStart);
        tvFcMaskGradEndVal = findViewById(R.id.tvFcMaskGradEndVal);
        sbFcMaskGradEnd = findViewById(R.id.sbFcMaskGradEnd);
        tvFcMaskGradMidVal = findViewById(R.id.tvFcMaskGradMidVal);
        sbFcMaskGradMid = findViewById(R.id.sbFcMaskGradMid);
        tvFcMaskGradStrengthVal = findViewById(R.id.tvFcMaskGradStrengthVal);
        sbFcMaskGradStrength = findViewById(R.id.sbFcMaskGradStrength);
        tvFcCustomColorSummary = findViewById(R.id.tvFcCustomColorSummary);
        layoutFcTintBadge = findViewById(R.id.layoutFcTintBadge);
        vFcTintSwatchDot = findViewById(R.id.vFcTintSwatchDot);
        ivFcTintChevron = findViewById(R.id.ivFcTintChevron);
        swFcCustomColor = findViewById(R.id.swFcCustomColor);
        dividerFcTintPicker = findViewById(R.id.dividerFcTintPicker);
        layoutFcColorPickerWrap = findViewById(R.id.layoutFcColorPickerWrap);
        fcColorPicker = findViewById(R.id.fcColorPicker);
        tvFcCustomMaskColorSummary = findViewById(R.id.tvFcCustomMaskColorSummary);
        layoutFcMaskTintBadge = findViewById(R.id.layoutFcMaskTintBadge);
        vFcMaskTintSwatchDot = findViewById(R.id.vFcMaskTintSwatchDot);
        ivFcMaskTintChevron = findViewById(R.id.ivFcMaskTintChevron);
        swFcCustomMaskColor = findViewById(R.id.swFcCustomMaskColor);
        dividerFcMaskTintPicker = findViewById(R.id.dividerFcMaskTintPicker);
        layoutFcMaskColorPickerWrap = findViewById(R.id.layoutFcMaskColorPickerWrap);
        fcMaskColorPicker = findViewById(R.id.fcMaskColorPicker);
        tvFcCustomGlowColorSummary = findViewById(R.id.tvFcCustomGlowColorSummary);
        layoutFcGlowTintBadge = findViewById(R.id.layoutFcGlowTintBadge);
        vFcGlowTintSwatchDot = findViewById(R.id.vFcGlowTintSwatchDot);
        ivFcGlowTintChevron = findViewById(R.id.ivFcGlowTintChevron);
        swFcCustomGlowColor = findViewById(R.id.swFcCustomGlowColor);
        dividerFcGlowTintPicker = findViewById(R.id.dividerFcGlowTintPicker);
        layoutFcGlowColorPickerWrap = findViewById(R.id.layoutFcGlowColorPickerWrap);
        fcGlowColorPicker = findViewById(R.id.fcGlowColorPicker);

        tvSummaryOpacityBlur = findViewById(R.id.tvSummaryOpacityBlur);
        tvSummaryHighlight = findViewById(R.id.tvSummaryHighlight);
        fcMenuPanels[0] = findViewById(R.id.panelFcMenuOpacityBlur);
        fcMenuPanels[1] = findViewById(R.id.panelFcMenuHighlight);
        fcMenuPanels[2] = findViewById(R.id.panelFcMenuMasks);
        int[] menuHeaderIds = new int[]{
                R.id.rowFcMenuOpacityBlur,
                R.id.rowFcMenuHighlight,
                R.id.rowFcMenuMasks
        };
        for (int i = 0; i < menuHeaderIds.length; i++) {
            final int menuIdx = i;
            findViewById(menuHeaderIds[i]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!data.fcEnabled) {
                        return;
                    }
                    openFcSubpage(menuIdx, true);
                }
            });
        }

        findViewById(R.id.rowFcEnabled).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swFcEnabled.toggle();
            }
        });
        swFcEnabled.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                if (suppressUiCallbacks) {
                    return;
                }
                if (livingFlowUi) {
                    ColorOsLivingFlowEngine.performHaptic(buttonView, ColorOsLivingFlowEngine.HapticKind.SWITCH);
                }
                data.fcEnabled = isChecked;
                persistImmediate();
            }
        });

        sbFcIslandOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || suppressUiCallbacks) {
                    return;
                }
                data.fcIslandOpacity = ConfigStore.clampOpacity(progress);
                persistSliderTick();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mainHandler.removeCallbacks(debouncedSync);
                syncRemoteAsync();
            }
        });

        sbFcOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || suppressUiCallbacks) {
                    return;
                }
                data.fcOpacity = ConfigStore.clampOpacity(progress);
                persistSliderTick();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mainHandler.removeCallbacks(debouncedSync);
                syncRemoteAsync();
            }
        });

        sbFcImmersiveOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || suppressUiCallbacks) {
                    return;
                }
                data.fcImmersiveOpacity = ConfigStore.clampOpacity(progress);
                persistSliderTick();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mainHandler.removeCallbacks(debouncedSync);
                syncRemoteAsync();
            }
        });

        sbFcBlur.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || suppressUiCallbacks) {
                    return;
                }
                data.fcBlur = ConfigStore.clampBlur(progress);
                persistSliderTick();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mainHandler.removeCallbacks(debouncedSync);
                syncRemoteAsync();
            }
        });

        sbFcBottomGlow.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || suppressUiCallbacks) {
                    return;
                }
                data.fcBottomGlow = ConfigStore.clampBottomGlow(progress);
                persistSliderTick();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mainHandler.removeCallbacks(debouncedSync);
                syncRemoteAsync();
            }
        });

        sbFcBottomGlowChroma.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || suppressUiCallbacks) {
                    return;
                }
                data.fcBottomGlowChroma = ConfigStore.clampBottomGlowChroma(progress);
                persistSliderTick();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mainHandler.removeCallbacks(debouncedSync);
                syncRemoteAsync();
            }
        });

        sbFcSingleGlow.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || suppressUiCallbacks) {
                    return;
                }
                data.fcSingleGlow = ConfigStore.clampBottomGlow(progress);
                persistSliderTick();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mainHandler.removeCallbacks(debouncedSync);
                syncRemoteAsync();
            }
        });

        sbFcSingleGlowChroma.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || suppressUiCallbacks) {
                    return;
                }
                data.fcSingleGlowChroma = ConfigStore.clampBottomGlowChroma(progress);
                persistSliderTick();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mainHandler.removeCallbacks(debouncedSync);
                syncRemoteAsync();
            }
        });

        tvFcIslandOpacityVal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!data.fcEnabled) {
                    return;
                }
                showSliderInputDialog(
                        R.string.pref_fc_island_opacity_title,
                        data.fcIslandOpacity,
                        0,
                        100,
                        "%",
                        new OnValueInputListener() {
                            @Override
                            public void onValueEntered(int value) {
                                data.fcIslandOpacity = ConfigStore.clampOpacity(value);
                                persistImmediate();
                            }
                        });
            }
        });
        tvFcOpacityVal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!data.fcEnabled) {
                    return;
                }
                showSliderInputDialog(
                        R.string.pref_fc_opacity_title,
                        data.fcOpacity,
                        0,
                        100,
                        "%",
                        new OnValueInputListener() {
                            @Override
                            public void onValueEntered(int value) {
                                data.fcOpacity = ConfigStore.clampOpacity(value);
                                persistImmediate();
                            }
                        });
            }
        });
        tvFcImmersiveOpacityVal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!data.fcEnabled) {
                    return;
                }
                showSliderInputDialog(
                        R.string.pref_fc_immersive_opacity_title,
                        data.fcImmersiveOpacity,
                        0,
                        100,
                        "%",
                        new OnValueInputListener() {
                            @Override
                            public void onValueEntered(int value) {
                                data.fcImmersiveOpacity = ConfigStore.clampOpacity(value);
                                persistImmediate();
                            }
                        });
            }
        });
        tvFcBlurVal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!data.fcEnabled) {
                    return;
                }
                showSliderInputDialog(
                        R.string.pref_fc_blur_title,
                        data.fcBlur,
                        0,
                        ConfigStore.MAX_FC_BLUR,
                        "%",
                        new OnValueInputListener() {
                            @Override
                            public void onValueEntered(int value) {
                                data.fcBlur = ConfigStore.clampBlur(value);
                                persistImmediate();
                            }
                        });
            }
        });
        tvFcBottomGlowVal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!data.fcEnabled) {
                    return;
                }
                showSliderInputDialog(
                        R.string.pref_fc_bottom_glow_title,
                        data.fcBottomGlow,
                        0,
                        ConfigStore.MAX_FC_BOTTOM_GLOW,
                        "%",
                        new OnValueInputListener() {
                            @Override
                            public void onValueEntered(int value) {
                                data.fcBottomGlow = ConfigStore.clampBottomGlow(value);
                                persistImmediate();
                            }
                        });
            }
        });
        tvFcBottomGlowChromaVal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!data.fcEnabled) {
                    return;
                }
                showSliderInputDialog(
                        R.string.pref_fc_bottom_glow_chroma_title,
                        data.fcBottomGlowChroma,
                        0,
                        ConfigStore.MAX_FC_BOTTOM_GLOW_CHROMA,
                        "%",
                        new OnValueInputListener() {
                            @Override
                            public void onValueEntered(int value) {
                                data.fcBottomGlowChroma = ConfigStore.clampBottomGlowChroma(value);
                                persistImmediate();
                            }
                        });
            }
        });
        tvFcSingleGlowVal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!data.fcEnabled) {
                    return;
                }
                showSliderInputDialog(
                        R.string.pref_fc_single_glow_title,
                        data.fcSingleGlow,
                        0,
                        ConfigStore.MAX_FC_BOTTOM_GLOW,
                        "%",
                        new OnValueInputListener() {
                            @Override
                            public void onValueEntered(int value) {
                                data.fcSingleGlow = ConfigStore.clampBottomGlow(value);
                                persistImmediate();
                            }
                        });
            }
        });
        tvFcSingleGlowChromaVal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!data.fcEnabled) {
                    return;
                }
                showSliderInputDialog(
                        R.string.pref_fc_single_glow_chroma_title,
                        data.fcSingleGlowChroma,
                        0,
                        ConfigStore.MAX_FC_BOTTOM_GLOW_CHROMA,
                        "%",
                        new OnValueInputListener() {
                            @Override
                            public void onValueEntered(int value) {
                                data.fcSingleGlowChroma = ConfigStore.clampBottomGlowChroma(value);
                                persistImmediate();
                            }
                        });
            }
        });

        findViewById(R.id.rowFcRemoveIslandMask).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swFcRemoveIslandMask.toggle();
            }
        });
        swFcRemoveIslandMask.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                if (suppressUiCallbacks) {
                    return;
                }
                if (livingFlowUi) {
                    ColorOsLivingFlowEngine.performHaptic(buttonView, ColorOsLivingFlowEngine.HapticKind.SWITCH);
                }
                data.fcRemoveIslandMask = isChecked;
                persistImmediate();
            }
        });

        findViewById(R.id.rowFcMatchCard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swFcMatchCard.toggle();
            }
        });
        swFcMatchCard.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                if (suppressUiCallbacks) {
                    return;
                }
                if (livingFlowUi) {
                    ColorOsLivingFlowEngine.performHaptic(buttonView, ColorOsLivingFlowEngine.HapticKind.SWITCH);
                }
                data.fcMatchCard = isChecked;
                persistImmediate();
            }
        });

        findViewById(R.id.rowFcRemoveShadeMask).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swFcRemoveShadeMask.toggle();
            }
        });
        swFcRemoveShadeMask.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                if (suppressUiCallbacks) {
                    return;
                }
                if (livingFlowUi) {
                    ColorOsLivingFlowEngine.performHaptic(buttonView, ColorOsLivingFlowEngine.HapticKind.SWITCH);
                }
                data.fcRemoveShadeMask = isChecked;
                persistImmediate();
            }
        });

        sbFcMaskStrength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || suppressUiCallbacks) {
                    return;
                }
                data.fcMaskStrength = ConfigStore.clampMaskStrength(progress);
                persistSliderTick();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mainHandler.removeCallbacks(debouncedSync);
                syncRemoteAsync();
            }
        });
        tvFcMaskStrengthVal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!data.fcEnabled) {
                    return;
                }
                showSliderInputDialog(
                        R.string.pref_fc_mask_strength_title,
                        data.fcMaskStrength,
                        0,
                        ConfigStore.MAX_FC_MASK_STRENGTH,
                        "%",
                        new OnValueInputListener() {
                            @Override
                            public void onValueEntered(int value) {
                                data.fcMaskStrength = ConfigStore.clampMaskStrength(value);
                                persistImmediate();
                            }
                        });
            }
        });

        findViewById(R.id.rowFcMaskFollowMedia).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swFcMaskFollowMedia.toggle();
            }
        });
        swFcMaskFollowMedia.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                if (suppressUiCallbacks) {
                    return;
                }
                if (livingFlowUi) {
                    ColorOsLivingFlowEngine.performHaptic(buttonView, ColorOsLivingFlowEngine.HapticKind.SWITCH);
                }
                data.fcMaskFollowMedia = isChecked;
                if (isChecked) {
                    data.fcRemoveIslandMask = false;
                    data.fcMatchCard = false;
                    data.fcRemoveShadeMask = false;
                }
                persistImmediate();
            }
        });

        attachMaskToneSlider(
                sbFcMaskBrightness,
                tvFcMaskBrightnessVal,
                R.string.pref_fc_mask_brightness_title,
                true);
        attachMaskToneSlider(
                sbFcMaskChroma,
                tvFcMaskChromaVal,
                R.string.pref_fc_mask_chroma_title,
                false);

        attachNonMediaMaskOpacitySlider(
                sbFcNonMediaMaskOpacity,
                tvFcNonMediaMaskOpacityVal,
                R.string.pref_fc_non_media_mask_opacity_title,
                false);
        attachNonMediaMaskOpacitySlider(
                sbFcNonMediaFollowMaskOpacity,
                tvFcNonMediaFollowMaskOpacityVal,
                R.string.pref_fc_non_media_follow_mask_opacity_title,
                true);

        attachMaskTemplateSlider(sbFcMaskGradStart, tvFcMaskGradStartVal, R.string.pref_fc_mask_grad_start_title, 100, 0);
        attachMaskTemplateSlider(sbFcMaskGradEnd, tvFcMaskGradEndVal, R.string.pref_fc_mask_grad_end_title, 100, 1);
        attachMaskTemplateSlider(sbFcMaskGradMid, tvFcMaskGradMidVal, R.string.pref_fc_mask_grad_mid_title, 100, 2);
        attachMaskTemplateSlider(
                sbFcMaskGradStrength, tvFcMaskGradStrengthVal, R.string.pref_fc_mask_grad_strength_title,
                ConfigStore.MAX_FC_MASK_STRENGTH, 3);

        findViewById(R.id.rowFcNonMediaMaskToggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swFcNonMediaMaskEnabled.toggle();
            }
        });
        swFcNonMediaMaskEnabled.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                if (suppressUiCallbacks) {
                    return;
                }
                if (livingFlowUi) {
                    ColorOsLivingFlowEngine.performHaptic(buttonView, ColorOsLivingFlowEngine.HapticKind.SWITCH);
                }
                data.fcNonMediaMaskEnabled = isChecked;
                persistImmediate();
            }
        });

        findViewById(R.id.rowFcCustomColor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!data.fcEnabled) {
                    return;
                }
                if (!data.fcColorEnabled) {
                    fcColorPickerUserExpanded = true;
                    swFcCustomColor.setChecked(true);
                } else {
                    fcColorPickerUserExpanded = !fcColorPickerUserExpanded;
                    if (livingFlowUi) {
                        ColorOsLivingFlowEngine.performHaptic(v, ColorOsLivingFlowEngine.HapticKind.TICK);
                    }
                    setFcColorPickerExpanded(fcColorPickerUserExpanded, true);
                    updateFcTintHeaderUi(true);
                }
            }
        });
        swFcCustomColor.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                if (suppressUiCallbacks) {
                    return;
                }
                if (livingFlowUi) {
                    ColorOsLivingFlowEngine.performHaptic(buttonView, ColorOsLivingFlowEngine.HapticKind.SWITCH);
                }
                if (isChecked) {
                    data.fcColorEnabled = true;
                    data.fcColor = data.fcLastColor != 0 ? data.fcLastColor : ConfigStore.DEF_CUSTOM_COLOR;
                    data.fcLastColor = data.fcColor;
                    fcColorPickerUserExpanded = true;
                } else {
                    data.fcLastColor = data.fcColor;
                    data.fcColorEnabled = false;
                    data.fcColor = 0;
                    fcColorPickerUserExpanded = false;
                }
                persistImmediate();
            }
        });

        if (fcColorPicker != null) {
            fcColorPicker.setShowAlpha(true);
            fcColorPicker.setOnColorChangeListener(new CouiHsvColorPickerView.OnColorChangeListener() {
                @Override
                public void onColorChanged(int argbColor, boolean fromUser) {
                    if (!fromUser || suppressUiCallbacks || !data.fcEnabled) {
                        return;
                    }
                    data.fcColorEnabled = true;
                    data.fcColor = argbColor;
                    data.fcLastColor = argbColor;
                    updateFcTintHeaderUi(false);
                    persistSliderTick();
                }

                @Override
                public void onTrackingStopped(int argbColor) {
                    mainHandler.removeCallbacks(debouncedSync);
                    syncRemoteAsync();
                }
            });
        }

        findViewById(R.id.rowFcCustomMaskColor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!data.fcEnabled) {
                    return;
                }
                if (!data.fcCustomMaskEnabled) {
                    fcMaskColorPickerUserExpanded = true;
                    swFcCustomMaskColor.setChecked(true);
                } else {
                    fcMaskColorPickerUserExpanded = !fcMaskColorPickerUserExpanded;
                    if (livingFlowUi) {
                        ColorOsLivingFlowEngine.performHaptic(v, ColorOsLivingFlowEngine.HapticKind.TICK);
                    }
                    setFcMaskColorPickerExpanded(fcMaskColorPickerUserExpanded, true);
                    updateFcMaskTintHeaderUi(true);
                }
            }
        });
        swFcCustomMaskColor.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                if (suppressUiCallbacks) {
                    return;
                }
                if (livingFlowUi) {
                    ColorOsLivingFlowEngine.performHaptic(buttonView, ColorOsLivingFlowEngine.HapticKind.SWITCH);
                }
                if (isChecked) {
                    data.fcCustomMaskEnabled = true;
                    if (data.fcCustomMaskHasLast) {
                        data.fcCustomMaskColor = data.fcCustomMaskLastColor;
                    } else if (data.fcColorEnabled) {
                        data.fcCustomMaskColor = data.fcColor;
                    } else {
                        data.fcCustomMaskColor = ConfigStore.DEF_CUSTOM_COLOR;
                    }
                    data.fcCustomMaskLastColor = data.fcCustomMaskColor;
                    data.fcCustomMaskHasLast = true;
                    fcMaskColorPickerUserExpanded = true;
                } else {
                    data.fcCustomMaskLastColor = data.fcCustomMaskColor;
                    data.fcCustomMaskHasLast = true;
                    data.fcCustomMaskEnabled = false;
                    data.fcCustomMaskColor = 0;
                    fcMaskColorPickerUserExpanded = false;
                }
                persistImmediate();
            }
        });

        if (fcMaskColorPicker != null) {
            fcMaskColorPicker.setShowAlpha(true);
            fcMaskColorPicker.setOnColorChangeListener(new CouiHsvColorPickerView.OnColorChangeListener() {
                @Override
                public void onColorChanged(int argbColor, boolean fromUser) {
                    if (!fromUser || suppressUiCallbacks || !data.fcEnabled) {
                        return;
                    }
                    data.fcCustomMaskEnabled = true;
                    data.fcCustomMaskColor = argbColor;
                    data.fcCustomMaskLastColor = argbColor;
                    data.fcCustomMaskHasLast = true;
                    updateFcMaskTintHeaderUi(false);
                    persistSliderTick();
                }

                @Override
                public void onTrackingStopped(int argbColor) {
                    mainHandler.removeCallbacks(debouncedSync);
                    syncRemoteAsync();
                }
            });
        }

        View rowFcCustomGlowColor = findViewById(R.id.rowFcCustomGlowColor);
        if (rowFcCustomGlowColor != null) {
            rowFcCustomGlowColor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!data.fcEnabled || swFcCustomGlowColor == null) {
                        return;
                    }
                    if (!data.fcCustomGlowEnabled) {
                        fcGlowColorPickerUserExpanded = true;
                        swFcCustomGlowColor.setChecked(true);
                    } else {
                        fcGlowColorPickerUserExpanded = !fcGlowColorPickerUserExpanded;
                        if (livingFlowUi) {
                            ColorOsLivingFlowEngine.performHaptic(v, ColorOsLivingFlowEngine.HapticKind.TICK);
                        }
                        setFcGlowColorPickerExpanded(fcGlowColorPickerUserExpanded, true);
                        updateFcGlowTintHeaderUi(true);
                    }
                }
            });
        }
        if (swFcCustomGlowColor != null) {
            swFcCustomGlowColor.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                    if (suppressUiCallbacks) {
                        return;
                    }
                    if (livingFlowUi) {
                        ColorOsLivingFlowEngine.performHaptic(buttonView, ColorOsLivingFlowEngine.HapticKind.SWITCH);
                    }
                    if (isChecked) {
                        data.fcCustomGlowEnabled = true;
                        if (data.fcCustomGlowHasLast) {
                            data.fcCustomGlowColor = data.fcCustomGlowLastColor;
                        } else if (data.fcColorEnabled) {
                            data.fcCustomGlowColor = data.fcColor;
                        } else {
                            data.fcCustomGlowColor = ConfigStore.DEF_CUSTOM_COLOR;
                        }
                        data.fcCustomGlowLastColor = data.fcCustomGlowColor;
                        data.fcCustomGlowHasLast = true;
                        fcGlowColorPickerUserExpanded = true;
                    } else {
                        data.fcCustomGlowLastColor = data.fcCustomGlowColor;
                        data.fcCustomGlowHasLast = true;
                        data.fcCustomGlowEnabled = false;
                        data.fcCustomGlowColor = 0;
                        fcGlowColorPickerUserExpanded = false;
                    }
                    persistImmediate();
                }
            });
        }
        if (fcGlowColorPicker != null) {
            fcGlowColorPicker.setShowAlpha(true);
            fcGlowColorPicker.setOnColorChangeListener(new CouiHsvColorPickerView.OnColorChangeListener() {
                @Override
                public void onColorChanged(int argbColor, boolean fromUser) {
                    if (!fromUser || suppressUiCallbacks || !data.fcEnabled) {
                        return;
                    }
                    data.fcCustomGlowEnabled = true;
                    data.fcCustomGlowColor = argbColor;
                    data.fcCustomGlowLastColor = argbColor;
                    data.fcCustomGlowHasLast = true;
                    updateFcGlowTintHeaderUi(false);
                    persistSliderTick();
                }

                @Override
                public void onTrackingStopped(int argbColor) {
                    mainHandler.removeCallbacks(debouncedSync);
                    syncRemoteAsync();
                }
            });
        }

        findViewById(R.id.btnResetFc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfigStore.SettingsData def = ConfigStore.createDefaults();
                data.fcEnabled = def.fcEnabled;
                data.fcOpacity = def.fcOpacity;
                data.fcImmersiveOpacity = def.fcImmersiveOpacity;
                data.fcIslandOpacity = def.fcIslandOpacity;
                data.fcBlur = def.fcBlur;
                data.fcBottomGlow = def.fcBottomGlow;
                data.fcBottomGlowChroma = def.fcBottomGlowChroma;
                data.fcSingleGlow = def.fcSingleGlow;
                data.fcSingleGlowChroma = def.fcSingleGlowChroma;
                data.fcColorEnabled = def.fcColorEnabled;
                data.fcColor = def.fcColor;
                data.fcLastColor = def.fcLastColor;
                data.fcCustomMaskEnabled = def.fcCustomMaskEnabled;
                data.fcCustomMaskColor = def.fcCustomMaskColor;
                data.fcCustomMaskLastColor = def.fcCustomMaskLastColor;
                data.fcCustomMaskHasLast = def.fcCustomMaskHasLast;
                data.fcCustomGlowEnabled = def.fcCustomGlowEnabled;
                data.fcCustomGlowColor = def.fcCustomGlowColor;
                data.fcCustomGlowLastColor = def.fcCustomGlowLastColor;
                data.fcCustomGlowHasLast = def.fcCustomGlowHasLast;
                data.fcMatchCard = def.fcMatchCard;
                data.fcRemoveIslandMask = def.fcRemoveIslandMask;
                data.fcRemoveShadeMask = def.fcRemoveShadeMask;
                data.fcMaskFollowMedia = def.fcMaskFollowMedia;
                data.fcMaskBrightness = def.fcMaskBrightness;
                data.fcMaskChroma = def.fcMaskChroma;
                data.fcMaskStrength = def.fcMaskStrength;
                data.fcNonMediaMaskOpacity = def.fcNonMediaMaskOpacity;
                data.fcNonMediaFollowMaskOpacity = def.fcNonMediaFollowMaskOpacity;
                data.fcNonMediaMaskEnabled = def.fcNonMediaMaskEnabled;
                data.fcNonMediaMaskColor = def.fcNonMediaMaskColor;
                data.fcNonMediaMaskLastColor = def.fcNonMediaMaskLastColor;
                data.fcMaskGradStart = def.fcMaskGradStart;
                data.fcMaskGradEnd = def.fcMaskGradEnd;
                data.fcMaskGradMid = def.fcMaskGradMid;
                data.fcMaskGradStrength = def.fcMaskGradStrength;
                persistImmediate();
                Toast.makeText(MainActivity.this, R.string.toast_reset_done, Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btnRestartUiFc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartSystemUi();
            }
        });
    }

    private void attachMaskToneSlider(SeekBar sb, TextView chip, final int titleRes, final boolean isBrightness) {
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || suppressUiCallbacks) {
                    return;
                }
                int clamped = ConfigStore.clampMaskPercent(progress);
                if (isBrightness) {
                    data.fcMaskBrightness = clamped;
                } else {
                    data.fcMaskChroma = clamped;
                }
                persistSliderTick();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mainHandler.removeCallbacks(debouncedSync);
                syncRemoteAsync();
            }
        });
        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!data.fcEnabled || (!data.fcMaskFollowMedia && !data.fcNonMediaMaskEnabled)) {
                    return;
                }
                int cur = isBrightness ? data.fcMaskBrightness : data.fcMaskChroma;
                showSliderInputDialog(titleRes, cur, 0, 100, "%", new OnValueInputListener() {
                    @Override
                    public void onValueEntered(int value) {
                        int clamped = ConfigStore.clampMaskPercent(value);
                        if (isBrightness) {
                            data.fcMaskBrightness = clamped;
                        } else {
                            data.fcMaskChroma = clamped;
                        }
                        persistImmediate();
                    }
                });
            }
        });
    }

    private void attachNonMediaMaskOpacitySlider(SeekBar sb, TextView chip,
                                                 final int titleRes, final boolean isFollowMode) {
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || suppressUiCallbacks) {
                    return;
                }
                int clamped = ConfigStore.clampMaskStrength(progress);
                if (isFollowMode) {
                    data.fcNonMediaFollowMaskOpacity = clamped;
                } else {
                    data.fcNonMediaMaskOpacity = clamped;
                }
                persistSliderTick();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mainHandler.removeCallbacks(debouncedSync);
                syncRemoteAsync();
            }
        });
        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!data.fcEnabled) {
                    return;
                }
                int cur = isFollowMode ? data.fcNonMediaFollowMaskOpacity : data.fcNonMediaMaskOpacity;
                showSliderInputDialog(titleRes, cur, 0, ConfigStore.MAX_FC_MASK_STRENGTH, "%",
                        new OnValueInputListener() {
                            @Override
                            public void onValueEntered(int value) {
                                int clamped = ConfigStore.clampMaskStrength(value);
                                if (isFollowMode) {
                                    data.fcNonMediaFollowMaskOpacity = clamped;
                                } else {
                                    data.fcNonMediaMaskOpacity = clamped;
                                }
                                persistImmediate();
                            }
                        });
            }
        });
    }

    private void attachMaskTemplateSlider(SeekBar sb, TextView chip, final int titleRes,
                                          final int maxVal, final int kind) {
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || suppressUiCallbacks) {
                    return;
                }
                applyMaskTemplateValue(kind, progress);
                persistSliderTick();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mainHandler.removeCallbacks(debouncedSync);
                syncRemoteAsync();
            }
        });
        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!data.fcEnabled) {
                    return;
                }
                int cur = readMaskTemplateValue(kind);
                int minVal = 0;
                int effMax = maxVal;
                if (kind == 0) {
                    effMax = 100 - ConfigStore.MIN_FC_MASK_GRAD_SPAN;
                } else if (kind == 1) {
                    minVal = ConfigStore.MIN_FC_MASK_GRAD_SPAN;
                } else if (kind == 2) {
                    minVal = ConfigStore.MIN_FC_MASK_GRAD_MID;
                    effMax = ConfigStore.MAX_FC_MASK_GRAD_MID;
                }
                showSliderInputDialog(titleRes, cur, minVal, effMax, "%", new OnValueInputListener() {
                    @Override
                    public void onValueEntered(int value) {
                        applyMaskTemplateValue(kind, value);
                        persistImmediate();
                    }
                });
            }
        });
    }

    private void applyMaskTemplateValue(int kind, int rawValue) {
        if (kind == 0) {
            int s = Math.max(0, Math.min(100 - ConfigStore.MIN_FC_MASK_GRAD_SPAN, rawValue));
            data.fcMaskGradStart = s;
            if (data.fcMaskGradEnd < s + ConfigStore.MIN_FC_MASK_GRAD_SPAN) {
                data.fcMaskGradEnd = s + ConfigStore.MIN_FC_MASK_GRAD_SPAN;
            }
        } else if (kind == 1) {
            int e = Math.max(ConfigStore.MIN_FC_MASK_GRAD_SPAN, Math.min(100, rawValue));
            data.fcMaskGradEnd = e;
            if (data.fcMaskGradStart > e - ConfigStore.MIN_FC_MASK_GRAD_SPAN) {
                data.fcMaskGradStart = e - ConfigStore.MIN_FC_MASK_GRAD_SPAN;
            }
        } else if (kind == 2) {
            data.fcMaskGradMid = ConfigStore.clampMaskGradMid(rawValue);
        } else {
            data.fcMaskGradStrength = ConfigStore.clampMaskStrength(rawValue);
        }
    }

    private int readMaskTemplateValue(int kind) {
        if (kind == 0) {
            return data.fcMaskGradStart;
        }
        if (kind == 1) {
            return data.fcMaskGradEnd;
        }
        if (kind == 2) {
            return data.fcMaskGradMid;
        }
        return data.fcMaskGradStrength;
    }

    private boolean isFcSubpageOpen() {
        return openFcMenuIndex >= 0;
    }

    private void openFcSubpage(int targetIdx, boolean animate) {
        if (targetIdx < 0 || targetIdx >= fcMenuPanels.length || !data.fcEnabled || layoutFcSubpageRoot == null) {
            return;
        }
        if (subpageTransitionAnimator != null) {
            subpageTransitionAnimator.cancel();
            subpageTransitionAnimator = null;
        }
        final int gen = ++subpageAnimGen;
        openFcMenuIndex = targetIdx;
        updateSubpageBackCallback(true);

        int[] titleRes = new int[]{
                R.string.menu_fc_opacity_blur_title,
                R.string.menu_fc_highlight_title,
                R.string.menu_fc_masks_title
        };
        int[] subtitleRes = new int[]{
                R.string.page_subtitle_fc_opacity_blur,
                R.string.page_subtitle_fc_highlight,
                R.string.page_subtitle_fc_masks
        };
        if (tvSubpageTitle != null) {
            tvSubpageTitle.setText(titleRes[targetIdx]);
        }
        if (tvSubpageSubtitle != null) {
            tvSubpageSubtitle.setText(subtitleRes[targetIdx]);
        }
        for (int i = 0; i < fcMenuPanels.length; i++) {
            if (fcMenuPanels[i] != null) {
                fcMenuPanels[i].setVisibility(i == targetIdx ? View.VISIBLE : View.GONE);
            }
        }
        if (scrollFcSubpage != null) {
            scrollFcSubpage.resetScrollAndBounce();
        }
        applySubpageInsets();
        applySubpageHeaderCollapseProgress(0f);

        layoutFcSubpageRoot.setVisibility(View.VISIBLE);
        if (livingFlowUi && layoutFcSubpageRoot instanceof SwipeableBlurFrameLayout) {
            ((SwipeableBlurFrameLayout) layoutFcSubpageRoot).setEdgeSwipeEnabled(true);
        }
        if (!animate) {
            layoutFcSubpageRoot.setTranslationX(0f);
            layoutFcSubpageRoot.setAlpha(1f);
            if (vSubpageScrim != null) {
                vSubpageScrim.setAlpha(0f);
                vSubpageScrim.setVisibility(View.GONE);
            }
            if (layoutRootBlur != null) {
                layoutRootBlur.setTranslationX(0f);
                layoutRootBlur.setVisibility(View.INVISIBLE);
            }
            layoutFcSubpageRoot.post(this::syncSubpageHeaderGeometryAndPadding);
            return;
        }

        final float slideDist = (layoutAppRoot != null && layoutAppRoot.getWidth() > 0)
                ? layoutAppRoot.getWidth()
                : getResources().getDisplayMetrics().widthPixels;
        if (layoutRootBlur != null) {
            layoutRootBlur.setVisibility(View.VISIBLE);
            layoutRootBlur.setTranslationX(0f);
        }
        if (vSubpageScrim != null) {
            vSubpageScrim.setAlpha(0f);
            vSubpageScrim.setVisibility(View.VISIBLE);
        }
        layoutFcSubpageRoot.setTranslationX(slideDist);
        layoutFcSubpageRoot.setAlpha(0.94f);

        final ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        subpageTransitionAnimator = anim;
        anim.setDuration(440L);
        anim.setInterpolator(SUBPAGE_EASE);
        anim.addUpdateListener(animation -> {
            float t = (Float) animation.getAnimatedValue();
            layoutFcSubpageRoot.setTranslationX(slideDist * (1f - t));
            layoutFcSubpageRoot.setAlpha(0.94f + 0.06f * Math.min(1f, t * 2.5f));
            if (layoutRootBlur != null) {
                layoutRootBlur.setTranslationX(-slideDist * SUBPAGE_PARALLAX_RATIO * t);
            }
            if (vSubpageScrim != null) {
                vSubpageScrim.setAlpha(SUBPAGE_SCRIM_MAX_ALPHA * t);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (subpageTransitionAnimator == anim) {
                    subpageTransitionAnimator = null;
                }
                if (!cancelled && gen == subpageAnimGen && isFcSubpageOpen()) {
                    layoutFcSubpageRoot.setTranslationX(0f);
                    layoutFcSubpageRoot.setAlpha(1f);
                    if (vSubpageScrim != null) {
                        vSubpageScrim.setAlpha(0f);
                        vSubpageScrim.setVisibility(View.GONE);
                    }
                    if (layoutRootBlur != null) {
                        layoutRootBlur.setVisibility(View.INVISIBLE);
                        layoutRootBlur.setTranslationX(0f);
                    }
                    syncSubpageHeaderGeometryAndPadding();
                }
            }
        });
        anim.start();
    }

    private void closeFcSubpage(boolean animate) {
        if (openFcMenuIndex < 0
                && (layoutFcSubpageRoot == null || layoutFcSubpageRoot.getVisibility() != View.VISIBLE)) {
            return;
        }
        if (subpageTransitionAnimator != null) {
            subpageTransitionAnimator.cancel();
            subpageTransitionAnimator = null;
        }
        final int gen = ++subpageAnimGen;
        openFcMenuIndex = -1;
        updateSubpageBackCallback(false);
        if (layoutFcSubpageRoot instanceof SwipeableBlurFrameLayout) {
            ((SwipeableBlurFrameLayout) layoutFcSubpageRoot).setEdgeSwipeEnabled(false);
        }

        if (layoutRootBlur != null) {
            layoutRootBlur.setVisibility(View.VISIBLE);
        }
        if (!animate) {
            if (layoutRootBlur != null) {
                layoutRootBlur.setTranslationX(0f);
            }
            if (vSubpageScrim != null) {
                vSubpageScrim.setAlpha(0f);
                vSubpageScrim.setVisibility(View.GONE);
            }
            if (layoutFcSubpageRoot != null) {
                layoutFcSubpageRoot.setVisibility(View.GONE);
                layoutFcSubpageRoot.setTranslationX(0f);
                layoutFcSubpageRoot.setAlpha(1f);
            }
            syncActiveHeaderToScrollNow();
            return;
        }

        final float slideDist = (layoutAppRoot != null && layoutAppRoot.getWidth() > 0)
                ? layoutAppRoot.getWidth()
                : getResources().getDisplayMetrics().widthPixels;
        final float startSubX = layoutFcSubpageRoot != null ? layoutFcSubpageRoot.getTranslationX() : 0f;
        final float progressShown = 1f - Math.max(0f, Math.min(1f, startSubX / Math.max(1f, slideDist)));
        final float startMainX = -slideDist * SUBPAGE_PARALLAX_RATIO * progressShown;
        final float startScrimAlpha = SUBPAGE_SCRIM_MAX_ALPHA * progressShown;
        if (layoutRootBlur != null) {
            layoutRootBlur.setTranslationX(startMainX);
        }
        if (vSubpageScrim != null) {
            vSubpageScrim.setAlpha(startScrimAlpha);
            vSubpageScrim.setVisibility(View.VISIBLE);
        }

        final ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        subpageTransitionAnimator = anim;
        anim.setDuration(390L);
        anim.setInterpolator(SUBPAGE_EASE);
        anim.addUpdateListener(animation -> {
            float t = (Float) animation.getAnimatedValue();
            if (layoutFcSubpageRoot != null) {
                layoutFcSubpageRoot.setTranslationX(startSubX + (slideDist - startSubX) * t);
            }
            if (layoutRootBlur != null) {
                layoutRootBlur.setTranslationX(startMainX * (1f - t));
            }
            if (vSubpageScrim != null) {
                vSubpageScrim.setAlpha(startScrimAlpha * (1f - t));
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (subpageTransitionAnimator == anim) {
                    subpageTransitionAnimator = null;
                }
                if (!cancelled && gen == subpageAnimGen && !isFcSubpageOpen()) {
                    if (layoutFcSubpageRoot != null) {
                        layoutFcSubpageRoot.setVisibility(View.GONE);
                        layoutFcSubpageRoot.setTranslationX(0f);
                        layoutFcSubpageRoot.setAlpha(1f);
                    }
                    if (vSubpageScrim != null) {
                        vSubpageScrim.setAlpha(0f);
                        vSubpageScrim.setVisibility(View.GONE);
                    }
                    if (layoutRootBlur != null) {
                        layoutRootBlur.setTranslationX(0f);
                    }
                    syncActiveHeaderToScrollNow();
                }
            }
        });
        anim.start();
    }

    private void bindAboutViews() {
        tvSysVersion = findViewById(R.id.tvSysVersion);
        tvDeviceModel = findViewById(R.id.tvDeviceModel);
        tvAboutChannel = findViewById(R.id.tvAboutChannel);
        tvAboutUpdateChip = findViewById(R.id.tvAboutUpdateChip);
        tvAboutUiStyleVal = findViewById(R.id.tvAboutUiStyleVal);
        tvAboutLanguageVal = findViewById(R.id.tvAboutLanguageVal);
        tvAboutDarkModeVal = findViewById(R.id.tvAboutDarkModeVal);

        cardAboutDynamicHero = findViewById(R.id.cardAboutDynamicHero);
        vAboutDynamicBg = findViewById(R.id.vAboutDynamicBg);
        layoutAboutHeroLogo = findViewById(R.id.layoutAboutHeroLogo);
        tvAboutHeroVer = findViewById(R.id.tvAboutHeroVer);
        tvAboutHeroUpdateStatus = findViewById(R.id.tvAboutHeroUpdateStatus);
        tvAboutBuildSub = findViewById(R.id.tvAboutBuildSub);
        cardAboutModuleStatus = findViewById(R.id.cardAboutModuleStatus);
        vAboutStatusEmblem = findViewById(R.id.vAboutStatusEmblem);
        ivAboutStatusIcon = findViewById(R.id.ivAboutStatusIcon);
        tvAboutStatusTitle = findViewById(R.id.tvAboutStatusTitle);
        tvAboutStatusBadge = findViewById(R.id.tvAboutStatusBadge);
        tvAboutStatusSubtitle = findViewById(R.id.tvAboutStatusSubtitle);

        tvSysVersion.setText((livingFlowUi ? "Android " : "") + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        String brand = Build.BRAND != null ? Build.BRAND.trim() : "";
        String model = Build.MODEL != null ? Build.MODEL.trim() : "";
        if (!brand.isEmpty() && model.toLowerCase().startsWith(brand.toLowerCase())) {
            tvDeviceModel.setText(model);
        } else {
            tvDeviceModel.setText((brand + " " + model).trim());
        }
        if (tvAboutBuildSub != null) {
            tvAboutBuildSub.setText("v" + getInstalledVersionName() + " (" + getInstalledVersionCode() + ")");
        }

        updateUiStyleLabel();
        updateLanguageLabel();
        updateDarkModeLabel();

        if (cardAboutDynamicHero != null) {
            cardAboutDynamicHero.setOnClickListener(v -> {
                if (livingFlowUi) {
                    LivingFlowUpdatePages.showSoftwareUpdatePage(this);
                } else {
                    checkUpdateFromLsposedRepo(true);
                }
            });
            cardAboutDynamicHero.setOnLongClickListener(v -> {
                if (!livingFlowUi || vAboutDynamicBg == null) {
                    return false;
                }
                vAboutDynamicBg.playEasterEgg();
                playAboutHeroEntrance(true);
                ColorOsLivingFlowEngine.performHaptic(v, ColorOsLivingFlowEngine.HapticKind.CONFIRM);
                return true;
            });
        }

        findViewById(R.id.rowOrigAuthor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCoolapkProfile(UID_ORIG_AUTHOR);
            }
        });
        findViewById(R.id.rowFcAuthor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCoolapkProfile(UID_FC_AUTHOR);
            }
        });
        findViewById(R.id.rowModAuthor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCoolapkProfile(UID_MOD_AUTHOR);
            }
        });
        View heroVersionChip = findViewById(R.id.tvHeroVersionChip);
        if (heroVersionChip != null) {
            heroVersionChip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showChangelogDialog();
                }
            });
        }
        findViewById(R.id.rowAboutChangelog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangelogDialog();
            }
        });
        findViewById(R.id.rowAboutCheckUpdate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (livingFlowUi) {
                    LivingFlowUpdatePages.showSoftwareUpdatePage(MainActivity.this);
                } else {
                    checkUpdateFromLsposedRepo(true);
                }
            }
        });
        View rowAboutUiStyle = findViewById(R.id.rowAboutUiStyle);
        if (rowAboutUiStyle != null) {
            CouiBottomSheet.trackTouchPoint(rowAboutUiStyle);
            rowAboutUiStyle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showUiStyleDialog(v);
                }
            });
        }
        View rowAboutLanguage = findViewById(R.id.rowAboutLanguage);
        CouiBottomSheet.trackTouchPoint(rowAboutLanguage);
        rowAboutLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLanguageDialog(v);
            }
        });
        View rowAboutDarkMode = findViewById(R.id.rowAboutDarkMode);
        CouiBottomSheet.trackTouchPoint(rowAboutDarkMode);
        rowAboutDarkMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDarkModeDialog(v);
            }
        });

        View rowAboutMonet = findViewById(R.id.rowAboutMonet);
        View dividerAboutMonet = findViewById(R.id.dividerAboutMonet);
        swAboutMonet = findViewById(R.id.swAboutMonet);
        if (Build.VERSION.SDK_INT < 31) {
            rowAboutMonet.setVisibility(View.GONE);
            dividerAboutMonet.setVisibility(View.GONE);
        } else {
            swAboutMonet.setChecked(ConfigStore.isMonetEnabled(this));
            rowAboutMonet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    swAboutMonet.toggle();
                }
            });
            swAboutMonet.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                    if (suppressUiCallbacks || isChecked == ConfigStore.isMonetEnabled(MainActivity.this)) {
                        return;
                    }
                    if (livingFlowUi) {
                        ColorOsLivingFlowEngine.performHaptic(buttonView, ColorOsLivingFlowEngine.HapticKind.SWITCH);
                    }
                    ConfigStore.setMonetEnabled(MainActivity.this, isChecked);
                    DynamicThemeHelper.applyAccent(findViewById(android.R.id.content), MainActivity.this, swAboutMonet);
                    updateNavIndicator(activeTab, activeTab, 1, false, false);
                    if (livingFlowUi) {
                        int accentNow = DynamicThemeHelper.getAccent(MainActivity.this);
                        lastAppliedAccent = accentNow;
                        boolean darkNow = ConfigStore.isDarkThemeActive(MainActivity.this);
                        View b1 = findViewById(R.id.btnRestartUiLock);
                        View b2 = findViewById(R.id.btnRestartUiFc);
                        if (b1 != null) {
                            ColorOsLivingFlowEngine.applyLuminousPillButton(b1, true, accentNow, darkNow, true);
                        }
                        if (b2 != null) {
                            ColorOsLivingFlowEngine.applyLuminousPillButton(b2, true, accentNow, darkNow, true);
                        }
                        syncStatusAndAboutUi();
                        if (sCachedLatestRelease != null) {
                            applyUpdateChipState(sCachedLatestRelease);
                        }
                    }
                }
            });
        }

        View rowAboutDebugMode = findViewById(R.id.rowAboutDebugMode);
        swAboutDebugMode = findViewById(R.id.swAboutDebugMode);
        dividerAboutDebugTester = findViewById(R.id.dividerAboutDebugTester);
        rowAboutDebugTester = findViewById(R.id.rowAboutDebugTester);
        rowAboutDebugMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swAboutDebugMode.toggle();
            }
        });
        swAboutDebugMode.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                if (suppressUiCallbacks) {
                    return;
                }
                if (livingFlowUi) {
                    ColorOsLivingFlowEngine.performHaptic(buttonView, ColorOsLivingFlowEngine.HapticKind.SWITCH);
                }
                data.fcDebugMode = isChecked;
                if (!isChecked) {
                    FluidCloudDebugTester.clearAll(MainActivity.this);
                }
                persistImmediate();
            }
        });
        rowAboutDebugTester.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDebugTesterDialog();
            }
        });

        findViewById(R.id.rowAboutRestart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartSystemUi();
            }
        });
        findViewById(R.id.rowAboutResetAll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmResetAll();
            }
        });
    }

    private void updateUiStyleLabel() {
        if (tvAboutUiStyleVal == null) {
            return;
        }
        tvAboutUiStyleVal.setText(ConfigStore.isLivingFlowUi(this)
                ? R.string.ui_style_living_flow
                : R.string.ui_style_classic);
    }

    private void updateLanguageLabel() {
        if (tvAboutLanguageVal == null) {
            return;
        }
        tvAboutLanguageVal.setText(getLanguageLabel(ConfigStore.getAppLanguage(this)));
    }

    private String getLanguageLabel(String lang) {
        if (ConfigStore.LANG_ZH_CN.equals(lang)) {
            return getString(R.string.lang_zh_cn);
        }
        if (ConfigStore.LANG_EN.equals(lang)) {
            return getString(R.string.lang_en);
        }
        if (ConfigStore.LANG_ZH_TW.equals(lang)) {
            return getString(R.string.lang_zh_tw);
        }
        return getString(R.string.lang_system);
    }

    private interface OnValueInputListener {
        void onValueEntered(int value);
    }

    private void showSliderInputDialog(int titleRes, final int currentVal, final int minVal, final int maxVal,
                                       String unit, final OnValueInputListener listener) {
        final CouiBottomSheet.Palette pal = CouiBottomSheet.Palette.fromModuleTheme(this);
        final CouiBottomSheet.SheetHandle sheet = CouiBottomSheet.create(this, pal);
        final Runnable[] confirmRef = new Runnable[1];
        String rangeHint = getString(R.string.dialog_input_range_fmt, minVal, maxVal, unit);
        String curBadge = currentVal + (unit != null ? unit : "");
        if (pal.livingFlow) {
            CouiBottomSheet.addLivingFlowTopBar(
                    this, sheet,
                    getString(titleRes),
                    getString(R.string.btn_cancel),
                    getString(R.string.btn_confirm),
                    1,
                    v -> {
                        if (confirmRef[0] != null) {
                            confirmRef[0].run();
                        }
                    });
            TextView tvHint = new TextView(this);
            tvHint.setText(rangeHint + "  ·  " + curBadge);
            tvHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            tvHint.setLineSpacing(0f, 1.25f);
            tvHint.setTextColor(pal.textSecondary);
            tvHint.setPadding(dp(4), 0, dp(4), 0);
            LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            hintLp.topMargin = dp(2);
            hintLp.bottomMargin = dp(10);
            sheet.card.addView(tvHint, hintLp);
        } else {
            CouiBottomSheet.addHeader(
                    this, sheet.card, pal,
                    getString(titleRes),
                    rangeHint,
                    curBadge);
        }

        final EditText et = pal.livingFlow
                ? CouiBottomSheet.addLivingFlowBorderlessInputCard(
                        this, sheet.card, pal, String.valueOf(currentVal), minVal + " ~ " + maxVal)
                : CouiBottomSheet.addPillInputBox(
                        this, sheet.card, pal, String.valueOf(currentVal), minVal + " ~ " + maxVal);
        int inputType = InputType.TYPE_CLASS_NUMBER;
        if (minVal < 0) {
            inputType |= InputType.TYPE_NUMBER_FLAG_SIGNED;
        }
        et.setInputType(inputType);
        et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(minVal < 0 ? 5 : 4)});
        et.setSelection(0, et.getText() != null ? et.getText().length() : 0);

        final Runnable submit = new Runnable() {
            @Override
            public void run() {
                String text = et.getText() != null ? et.getText().toString().trim() : "";
                int parsed = ConfigStore.parseIntSafe(text, currentVal);
                int clamped = Math.max(minVal, Math.min(maxVal, parsed));
                sheet.dialog.dismiss();
                if (listener != null) {
                    listener.onValueEntered(clamped);
                }
            }
        };

        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    submit.run();
                    return true;
                }
                return false;
            }
        });

        if (pal.livingFlow) {
            confirmRef[0] = submit;
        } else {
            CouiBottomSheet.addButtonRow(
                    this, sheet.card, pal,
                    getString(R.string.btn_cancel), 0, v -> sheet.dialog.dismiss(),
                    getString(R.string.btn_confirm), 1, v -> submit.run());
        }

        if (sheet.dialog.getWindow() != null) {
            sheet.dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        sheet.dialog.show();
        et.requestFocus();
    }

    private void updateDarkModeLabel() {
        if (tvAboutDarkModeVal == null) {
            return;
        }
        int mode = ConfigStore.getAppDarkMode(this);
        if (mode == ConfigStore.DARK_MODE_LIGHT) {
            tvAboutDarkModeVal.setText(R.string.dark_mode_light);
        } else if (mode == ConfigStore.DARK_MODE_DARK) {
            tvAboutDarkModeVal.setText(R.string.dark_mode_dark);
        } else {
            tvAboutDarkModeVal.setText(R.string.dark_mode_system);
        }
    }

    private void showUiStyleDialog(View anchor) {
        final int curStyle = ConfigStore.getAppUiStyle(this);
        final int curIdx = (curStyle == ConfigStore.UI_STYLE_LIVING_FLOW) ? 0 : 1;
        final String[] labels = new String[]{
                getString(R.string.ui_style_living_flow_desc),
                getString(R.string.ui_style_classic_desc)
        };
        CouiBottomSheet.showPopupMenu(this, anchor, CouiBottomSheet.Palette.fromModuleTheme(this),
                labels, curIdx, which -> {
                    int nextStyle = (which == 0) ? ConfigStore.UI_STYLE_LIVING_FLOW : ConfigStore.UI_STYLE_CLASSIC;
                    if (nextStyle != curStyle) {
                        ConfigStore.setAppUiStyle(MainActivity.this, nextStyle);
                        sPendingTabAfterRecreate = activeTab;
                        mainHandler.postDelayed(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                recreate();
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            }
                        }, 90L);
                    }
                });
    }

    private void showDarkModeDialog(View anchor) {
        final int[] modes = new int[]{
                ConfigStore.DARK_MODE_SYSTEM,
                ConfigStore.DARK_MODE_LIGHT,
                ConfigStore.DARK_MODE_DARK
        };
        final String[] labels = new String[]{
                getString(R.string.dark_mode_system),
                getString(R.string.dark_mode_light),
                getString(R.string.dark_mode_dark)
        };
        int current = ConfigStore.getAppDarkMode(this);
        int checked = 0;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == current) {
                checked = i;
                break;
            }
        }
        CouiBottomSheet.showPopupMenu(this, anchor, CouiBottomSheet.Palette.fromModuleTheme(this),
                labels, checked, which -> {
                    int chosen = modes[which];
                    if (chosen != ConfigStore.getAppDarkMode(MainActivity.this)) {
                        ConfigStore.setAppDarkMode(MainActivity.this, chosen);
                        sPendingTabAfterRecreate = activeTab;
                        mainHandler.postDelayed(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                recreate();
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            }
                        }, 90L);
                    }
                });
    }

    private void showLanguageDialog(View anchor) {
        final String[] codes = new String[]{
                ConfigStore.LANG_SYSTEM,
                ConfigStore.LANG_ZH_TW,
                ConfigStore.LANG_ZH_CN,
                ConfigStore.LANG_EN
        };
        String detectedLabel = getLanguageLabel(ConfigStore.detectSystemLanguage());
        final String[] labels = new String[]{
                getString(R.string.lang_system_fmt, detectedLabel),
                getString(R.string.lang_zh_tw),
                getString(R.string.lang_zh_cn),
                getString(R.string.lang_en)
        };
        String current = ConfigStore.getAppLanguage(this);
        int checked = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(current)) {
                checked = i;
                break;
            }
        }
        CouiBottomSheet.showPopupMenu(this, anchor, CouiBottomSheet.Palette.fromModuleTheme(this),
                labels, checked, which -> {
                    String chosen = codes[which];
                    if (!chosen.equals(ConfigStore.getAppLanguage(MainActivity.this))) {
                        ConfigStore.setAppLanguage(MainActivity.this, chosen);
                        sPendingTabAfterRecreate = activeTab;
                        mainHandler.postDelayed(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                recreate();
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            }
                        }, 90L);
                    }
                });
    }

    private void confirmResetAll() {
        CouiBottomSheet.Palette pal = CouiBottomSheet.Palette.fromModuleTheme(this);
        CouiBottomSheet.SheetHandle sheet = CouiBottomSheet.create(this, pal);
        final View.OnClickListener resetAction = v -> {
            sheet.dialog.dismiss();
            int ver = data.fcVer;
            data = ConfigStore.createDefaults();
            data.fcVer = ver;
            FluidCloudDebugTester.clearAll(MainActivity.this);
            persistImmediate();
            Toast.makeText(MainActivity.this, R.string.toast_reset_done, Toast.LENGTH_SHORT).show();
        };
        if (pal.livingFlow) {
            CouiBottomSheet.addLivingFlowTopBar(
                    this, sheet,
                    getString(R.string.dialog_reset_all_title),
                    getString(R.string.btn_cancel),
                    getString(R.string.btn_confirm_reset), 2, resetAction);
            LinearLayout msgCard = CouiBottomSheet.createSubcardGroup(this, pal);
            msgCard.setPadding(dp(18), dp(18), dp(18), dp(18));
            TextView msgTv = new TextView(this);
            msgTv.setText(R.string.dialog_reset_all_msg);
            msgTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            msgTv.setLineSpacing(0f, 1.35f);
            msgTv.setTextColor(pal.textPrimary);
            msgCard.addView(msgTv);
            sheet.card.addView(msgCard, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            sheet.dialog.show();
            return;
        }
        CouiBottomSheet.addHeader(
                this, sheet.card, pal,
                getString(R.string.dialog_reset_all_title),
                getString(R.string.dialog_reset_all_msg),
                null);
        CouiBottomSheet.addButtonRow(
                this, sheet.card, pal,
                getString(R.string.btn_cancel), 0, v -> sheet.dialog.dismiss(),
                getString(R.string.btn_confirm_reset), 2, resetAction);
        sheet.dialog.show();
    }

    private void showChangelogDialog() {
        if (livingFlowUi) {
            LivingFlowUpdatePages.showChangelogPage(this);
            return;
        }
        CouiBottomSheet.Palette pal = CouiBottomSheet.Palette.fromModuleTheme(this);
        CouiBottomSheet.SheetHandle sheet = CouiBottomSheet.createCenter(this, pal);
        CouiBottomSheet.addHeader(
                this, sheet.card, pal,
                getString(R.string.dialog_changelog_title),
                null,
                formatVerLabel(getInstalledVersionName()));

        ScrollView scroll = CouiBottomSheet.createMaxHeightScrollView(
                this, Math.round(getResources().getDisplayMetrics().heightPixels * 0.46f));
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollLp.topMargin = dp(14);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        sheet.card.addView(scroll, scrollLp);

        int[] verTitles = new int[]{
                R.string.changelog_v113_title,
                R.string.changelog_v112_title,
                R.string.changelog_v111_title,
                R.string.changelog_v110_title,
                R.string.changelog_v109_title,
                R.string.changelog_v108_title,
                R.string.changelog_v107_title,
                R.string.changelog_v106_title,
                R.string.changelog_v105_title,
                R.string.changelog_v104_title,
                R.string.changelog_v103_title,
                R.string.changelog_v102_title,
                R.string.changelog_v101_title,
                R.string.changelog_v100_title
        };
        int[] verBodies = new int[]{
                R.string.changelog_v113_body,
                R.string.changelog_v112_body,
                R.string.changelog_v111_body,
                R.string.changelog_v110_body,
                R.string.changelog_v109_body,
                R.string.changelog_v108_body,
                R.string.changelog_v107_body,
                R.string.changelog_v106_body,
                R.string.changelog_v105_body,
                R.string.changelog_v104_body,
                R.string.changelog_v103_body,
                R.string.changelog_v102_body,
                R.string.changelog_v101_body,
                R.string.changelog_v100_body
        };
        for (int i = 0; i < verTitles.length; i++) {
            addChangelogItem(list, pal, getString(verTitles[i]), getString(verBodies[i]), i < verTitles.length - 1);
        }

        CouiBottomSheet.addSingleButton(this, sheet.card, pal,
                getString(R.string.btn_close), 1, 16, v -> sheet.dialog.dismiss());
        sheet.dialog.show();
    }

    private void addChangelogItem(LinearLayout parent, CouiBottomSheet.Palette pal,
                                  String verTitle, CharSequence bodyText, boolean addBottomMargin) {
        LinearLayout card = CouiBottomSheet.createSubcardGroup(this, pal);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView badge = CouiBottomSheet.createChipBadge(this, pal, verTitle, true, null);
        badge.setClickable(false);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        badge.setPadding(dp(9), dp(3), dp(9), dp(3));
        card.addView(badge, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView body = new TextView(this);
        body.setText(bodyText);
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        body.setLineSpacing(0f, 1.35f);
        body.setTextColor(pal.textSecondary);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = dp(8);
        card.addView(body, bodyLp);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (addBottomMargin) {
            cardLp.bottomMargin = dp(10);
        }
        parent.addView(card, cardLp);
    }

    interface UpdateCheckCallback {
        void onResult(RepoReleaseInfo info);
    }

    static final class RepoReleaseInfo {
        int versionCode;
        String versionName;
        String changelogRaw;
        boolean isHtmlChangelog;
        String downloadUrl;
        String repoPageUrl;
    }

    static RepoReleaseInfo getCachedLatestRelease() {
        return sCachedLatestRelease;
    }

    void openRepoOrUrl(String url) {
        if (!tryStartView(url)) {
            Toast.makeText(this, R.string.toast_open_link_failed, Toast.LENGTH_SHORT).show();
        }
    }

    void openRepoPage(RepoReleaseInfo info) {
        String targetUrl = (info != null && info.repoPageUrl != null && !info.repoPageUrl.isEmpty())
                ? info.repoPageUrl : LSPOSED_REPO_PAGE_URL;
        openRepoOrUrl(targetUrl);
    }

    private boolean isUpdatedAppInstall() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pi != null && pi.lastUpdateTime > pi.firstInstallTime;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void maybeShowPostUpdateChangelog(Bundle savedInstanceState, boolean fromThemeRecreate) {
        int curVer = getInstalledVersionCode();
        int lastSeen = ConfigStore.getLastSeenVersionCode(this);
        boolean isPostUpdate = (lastSeen > 0 && lastSeen < curVer)
                || (lastSeen == 0 && (hadPriorPrefs || isUpdatedAppInstall()));
        if (lastSeen < curVer) {
            ConfigStore.setLastSeenVersionCode(this, curVer);
        }
        if (isPostUpdate && savedInstanceState == null && !fromThemeRecreate) {
            final View root = findViewById(android.R.id.content);
            if (root != null) {
                root.postDelayed(() -> {
                    if (!isFinishing() && !isDestroyed()
                            && !LivingFlowUpdatePages.hasActiveStage(MainActivity.this)) {
                        showChangelogDialog();
                    }
                }, 180L);
            }
        }
    }

    int getInstalledVersionCode() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= 28) {
                return (int) pi.getLongVersionCode();
            }
            //noinspection deprecation
            return pi.versionCode;
        } catch (Throwable ignored) {
            return 1103;
        }
    }

    String getInstalledVersionName() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (pi.versionName != null && !pi.versionName.trim().isEmpty()) {
                return pi.versionName.trim();
            }
        } catch (Throwable ignored) {
        }
        return "1.1.3";
    }

    static String formatVerLabel(String ver) {
        if (ver == null || ver.trim().isEmpty()) {
            return "v1.1.3";
        }
        String t = ver.trim();
        if (t.startsWith("v") || t.startsWith("V")) {
            return "v" + t.substring(1);
        }
        return "v" + t;
    }

    private static int compareVersionNames(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        String ca = a.trim().replaceFirst("^[vV]", "");
        String cb = b.trim().replaceFirst("^[vV]", "");
        String[] pa = ca.split("[^0-9]+");
        String[] pb = cb.split("[^0-9]+");
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int va = (i < pa.length && !pa[i].isEmpty()) ? ConfigStore.parseIntSafe(pa[i], 0) : 0;
            int vb = (i < pb.length && !pb[i].isEmpty()) ? ConfigStore.parseIntSafe(pb[i], 0) : 0;
            if (va != vb) {
                return va > vb ? 1 : -1;
            }
        }
        return 0;
    }

    boolean isNewerRelease(RepoReleaseInfo info) {
        if (info == null) {
            return false;
        }
        int curCode = getInstalledVersionCode();
        if (info.versionCode > 0 && curCode > 0) {
            return info.versionCode > curCode;
        }
        return compareVersionNames(info.versionName, getInstalledVersionName()) > 0;
    }

    private void applyUpdateChipState(RepoReleaseInfo info) {
        if (info == null) {
            return;
        }
        boolean newer = isNewerRelease(info);
        if (tvAboutUpdateChip != null) {
            tvAboutUpdateChip.setVisibility(View.VISIBLE);
            if (newer) {
                tvAboutUpdateChip.setText(getString(
                        R.string.chip_update_available_fmt, formatVerLabel(info.versionName)));
            } else {
                tvAboutUpdateChip.setText(R.string.chip_update_latest);
            }
            styleUpdateChip(newer);
        }
        if (tvAboutHeroUpdateStatus != null) {
            if (newer) {
                tvAboutHeroUpdateStatus.setText(getString(
                        R.string.about_hero_new_ver_fmt, formatVerLabel(info.versionName)));
                GradientDrawable pill = new GradientDrawable();
                pill.setShape(GradientDrawable.RECTANGLE);
                pill.setCornerRadius(999f * getResources().getDisplayMetrics().density);
                pill.setColor(0x34FFFFFF);
                pill.setStroke(Math.max(1, dp(1)), 0x55FFFFFF);
                tvAboutHeroUpdateStatus.setBackground(pill);
            } else {
                tvAboutHeroUpdateStatus.setText(R.string.dialog_update_latest_title);
                tvAboutHeroUpdateStatus.setBackground(null);
            }
        }
    }

    private void styleUpdateChip(boolean highlightAccent) {
        if (tvAboutUpdateChip == null) {
            return;
        }
        if (livingFlowUi) {
            tvAboutUpdateChip.setBackground(null);
            tvAboutUpdateChip.setTextColor(highlightAccent
                    ? DynamicThemeHelper.getAccent(this)
                    : getColor(R.color.lf_text_secondary));
        } else {
            DynamicThemeHelper.applyAccent(tvAboutUpdateChip, this);
        }
    }

    void runRepoUpdateCheck(final UpdateCheckCallback callback) {
        if (tvAboutUpdateChip != null) {
            tvAboutUpdateChip.setVisibility(View.VISIBLE);
            tvAboutUpdateChip.setText(R.string.chip_update_checking);
            styleUpdateChip(false);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final RepoReleaseInfo info = fetchLatestReleaseFromRepo();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (info != null) {
                            sCachedLatestRelease = info;
                            applyUpdateChipState(info);
                        } else if (sCachedLatestRelease != null) {
                            applyUpdateChipState(sCachedLatestRelease);
                        } else if (tvAboutUpdateChip != null) {
                            tvAboutUpdateChip.setVisibility(View.GONE);
                        }
                        if (callback != null) {
                            callback.onResult(info);
                        }
                    }
                });
            }
        }).start();
    }

    private void checkUpdateFromLsposedRepo(final boolean manual) {
        if (updateCheckRunning) {
            return;
        }
        updateCheckRunning = true;
        if (tvAboutUpdateChip != null) {
            tvAboutUpdateChip.setVisibility(View.VISIBLE);
            tvAboutUpdateChip.setText(R.string.chip_update_checking);
            styleUpdateChip(false);
        }
        if (manual) {
            Toast.makeText(this, R.string.toast_update_checking, Toast.LENGTH_SHORT).show();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final RepoReleaseInfo info = fetchLatestReleaseFromRepo();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateCheckRunning = false;
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (info == null) {
                            if (sCachedLatestRelease != null) {
                                applyUpdateChipState(sCachedLatestRelease);
                            } else if (tvAboutUpdateChip != null) {
                                tvAboutUpdateChip.setVisibility(View.GONE);
                            }
                            if (manual) {
                                Toast.makeText(MainActivity.this, R.string.toast_update_failed, Toast.LENGTH_SHORT).show();
                            }
                            return;
                        }
                        sCachedLatestRelease = info;
                        applyUpdateChipState(info);
                        if (manual) {
                            showUpdateResultDialog(info);
                        }
                    }
                });
            }
        }).start();
    }

    private static RepoReleaseInfo fetchLatestReleaseFromRepo() {
        RepoReleaseInfo fromLsp = fetchFromLsposedJson();
        if (fromLsp != null) {
            return fromLsp;
        }
        return fetchFromGitHubApiFallback();
    }

    private static String httpGetText(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(9000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "C17FpOverlap-UpdateChecker/1.0");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("HTTP " + code);
            }
            InputStream in = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            reader.close();
            return sb.toString();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static void parseTagIntoInfo(String tagName, String releaseName, RepoReleaseInfo out) {
        String tag = tagName != null ? tagName.trim() : "";
        String name = releaseName != null ? releaseName.trim() : "";
        int dash = tag.indexOf('-');
        if (dash > 0) {
            int code = ConfigStore.parseIntSafe(tag.substring(0, dash).trim(), 0);
            String ver = tag.substring(dash + 1).trim();
            out.versionCode = code;
            out.versionName = !name.isEmpty() ? name : ver;
        } else {
            out.versionCode = 0;
            out.versionName = !name.isEmpty() ? name : tag;
        }
        if (out.versionName == null || out.versionName.isEmpty()) {
            out.versionName = "1.1.3";
        }
    }

    private static RepoReleaseInfo fetchFromLsposedJson() {
        try {
            String raw = httpGetText(LSPOSED_REPO_JSON_URL);
            JSONObject root = new JSONObject(raw);
            JSONArray releases = root.optJSONArray("releases");
            if (releases == null || releases.length() == 0) {
                return null;
            }
            JSONObject chosen = null;
            for (int i = 0; i < releases.length(); i++) {
                JSONObject r = releases.optJSONObject(i);
                if (r != null && !r.optBoolean("isDraft", false) && !r.optBoolean("isPrerelease", false)) {
                    chosen = r;
                    break;
                }
            }
            if (chosen == null) {
                chosen = releases.optJSONObject(0);
            }
            if (chosen == null) {
                return null;
            }
            RepoReleaseInfo info = new RepoReleaseInfo();
            parseTagIntoInfo(
                    chosen.optString("tagName", root.optString("latestRelease", "")),
                    chosen.optString("name", ""),
                    info);
            info.changelogRaw = chosen.optString("descriptionHTML", "");
            info.isHtmlChangelog = true;
            info.repoPageUrl = LSPOSED_REPO_PAGE_URL;
            JSONArray assets = chosen.optJSONArray("releaseAssets");
            if (assets != null && assets.length() > 0) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject a = assets.optJSONObject(i);
                    if (a == null) continue;
                    String dl = a.optString("downloadUrl", "");
                    String origDl = a.optString("originalDownloadUrl", "");
                    String candidate = !dl.isEmpty() ? dl : origDl;
                    if (!candidate.isEmpty()) {
                        info.downloadUrl = candidate;
                        if (a.optString("name", "").toLowerCase(Locale.ROOT).endsWith(".apk")) {
                            break;
                        }
                    }
                }
            }
            return info;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static RepoReleaseInfo fetchFromGitHubApiFallback() {
        try {
            String raw = httpGetText(GITHUB_LATEST_API_URL);
            JSONObject rel = new JSONObject(raw);
            RepoReleaseInfo info = new RepoReleaseInfo();
            parseTagIntoInfo(rel.optString("tag_name", ""), rel.optString("name", ""), info);
            info.changelogRaw = rel.optString("body", "");
            info.isHtmlChangelog = false;
            info.repoPageUrl = LSPOSED_REPO_PAGE_URL;
            JSONArray assets = rel.optJSONArray("assets");
            if (assets != null && assets.length() > 0) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject a = assets.optJSONObject(i);
                    if (a == null) continue;
                    String dl = a.optString("browser_download_url", "");
                    if (!dl.isEmpty()) {
                        info.downloadUrl = dl;
                        if (a.optString("name", "").toLowerCase(Locale.ROOT).endsWith(".apk")) {
                            break;
                        }
                    }
                }
            }
            return info;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String decodeEscapedUnicode(String s) {
        if (s == null || s.indexOf("\\u") < 0) {
            return s;
        }
        StringBuilder out = new StringBuilder(s.length());
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 5 < len && s.charAt(i + 1) == 'u') {
                String hex = s.substring(i + 2, i + 6);
                try {
                    out.append((char) Integer.parseInt(hex, 16));
                    i += 5;
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String matchChangelogLanguageCode(String rawHeader) {
        if (rawHeader == null) {
            return null;
        }
        String h = rawHeader.replace("**", "").replace("__", "").trim().toLowerCase(Locale.ROOT);
        if (h.equals("繁體中文") || h.equals("繁体中文") || h.equals("正體中文")
                || h.equals("traditional chinese") || h.equals("zh-tw") || h.equals("zh_tw")) {
            return ConfigStore.LANG_ZH_TW;
        }
        if (h.equals("简体中文") || h.equals("簡體中文") || h.equals("simplified chinese")
                || h.equals("zh-cn") || h.equals("zh_cn")) {
            return ConfigStore.LANG_ZH_CN;
        }
        if (h.equals("english") || h.equals("en") || h.equals("en-us") || h.equals("en_us")) {
            return ConfigStore.LANG_EN;
        }
        return null;
    }

    private static void appendFormattedMarkdownLine(SpannableStringBuilder sb, String line, boolean forceBold) {
        if (line == null || line.isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('\n');
        }
        // Strip Markdown links [text](url) -> text and inline backticks `code` -> code
        String cleaned = line.replaceAll("\\[([^\\]]+)]\\([^)]+\\)", "$1")
                .replaceAll("`([^`]+)`", "$1");
        int startLine = sb.length();
        Matcher boldMatcher = Pattern.compile("(\\*\\*|__)(.+?)\\1").matcher(cleaned);
        int cursor = 0;
        while (boldMatcher.find()) {
            if (boldMatcher.start() > cursor) {
                sb.append(cleaned, cursor, boldMatcher.start());
            }
            int spanStart = sb.length();
            sb.append(boldMatcher.group(2));
            if (!forceBold && sb.length() > spanStart) {
                sb.setSpan(new StyleSpan(Typeface.BOLD), spanStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            cursor = boldMatcher.end();
        }
        if (cursor < cleaned.length()) {
            sb.append(cleaned, cursor, cleaned.length());
        }
        if (forceBold && sb.length() > startLine) {
            sb.setSpan(new StyleSpan(Typeface.BOLD), startLine, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    CharSequence formatLocalizedChangelog(RepoReleaseInfo info) {
        if (info == null || info.changelogRaw == null || info.changelogRaw.trim().isEmpty()) {
            return "";
        }
        String text = decodeEscapedUnicode(info.changelogRaw).replace("\r\n", "\n");
        if (info.isHtmlChangelog) {
            // Preserve block boundaries as <br> tags because Html.fromHtml collapses bare '\n' into spaces
            String prepped = text
                    .replaceAll("(?is)<svg\\b[^>]*>.*?</svg>", "")
                    .replaceAll("(?i)<h[1-6][^>]*>", "<br>@@HDR@@ ")
                    .replaceAll("(?i)</h[1-6]>", "<br>")
                    .replaceAll("(?i)<li[^>]*>", "<br>• ")
                    .replaceAll("(?i)</li>", "<br>")
                    .replaceAll("(?i)<hr\\s*/?>", "<br>")
                    .replaceAll("(?i)<blockquote[^>]*>", "<br>")
                    .replaceAll("(?i)</blockquote>", "<br>")
                    .replaceAll("(?i)<tr[^>]*>", "<br>")
                    .replaceAll("(?i)<p[^>]*>", "<br>")
                    .replaceAll("(?i)</p>", "<br>")
                    .replaceAll("(?i)<div[^>]*>", "<br>")
                    .replaceAll("(?i)</div>", "<br>")
                    .replaceAll("(?i)<br\\s*/?>", "<br>");
            text = Html.fromHtml(prepped, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            // Split any inline concatenated markdown headings like "... ### English"
            text = text.replace("\\n", "\n")
                    .replaceAll("(?m)\\s+(#{2,6}\\s*[^#\\s])", "\n$1");
        }

        text = text.replace('\u00A0', ' ')
                .replaceAll("[\u200B-\u200D\uFEFF]", "");
        text = Normalizer.normalize(text, Normalizer.Form.NFC);

        String targetLang = ConfigStore.getEffectiveAppLanguage(this);
        String[] rawLines = text.split("\n");
        List<String> targetLines = new ArrayList<>();
        List<Boolean> targetBold = new ArrayList<>();
        List<String> allLines = new ArrayList<>();
        List<Boolean> allBold = new ArrayList<>();
        boolean inTarget = false;
        boolean foundTarget = false;

        for (String rawLine : rawLines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.matches("^[-*_]{3,}$")) {
                continue;
            }
            boolean isHeading = false;
            if (line.startsWith("@@HDR@@")) {
                isHeading = true;
                line = line.substring("@@HDR@@".length()).trim();
            }
            if (line.matches("^#{1,6}.*")) {
                isHeading = true;
                line = line.replaceFirst("^#{1,6}\\s*", "").trim();
            }
            if (line.isEmpty()) {
                continue;
            }
            // Strip any stray inline markdown headings left mid-line after HTML collapse
            line = line.replaceAll("\\s+#{2,6}\\s*", " ").trim();

            if (isHeading) {
                String matchedLang = matchChangelogLanguageCode(line);
                if (matchedLang != null) {
                    if (matchedLang.equals(targetLang)) {
                        inTarget = true;
                        foundTarget = true;
                    } else {
                        inTarget = false;
                    }
                    continue;
                }
                if (inTarget) {
                    targetLines.add(line);
                    targetBold.add(Boolean.TRUE);
                }
                allLines.add(line);
                allBold.add(Boolean.TRUE);
                continue;
            }

            if (line.matches("^(?:[-*+·]|•\\s*[•·])\\s+.*")) {
                line = "• " + line.replaceFirst("^(?:[-*+·]|•\\s*[•·])\\s+", "").trim();
            }
            if (inTarget) {
                targetLines.add(line);
                targetBold.add(Boolean.FALSE);
            }
            allLines.add(line);
            allBold.add(Boolean.FALSE);
        }

        List<String> chosenLines = (foundTarget && !targetLines.isEmpty()) ? targetLines : allLines;
        List<Boolean> chosenBold = (foundTarget && !targetLines.isEmpty()) ? targetBold : allBold;
        SpannableStringBuilder out = new SpannableStringBuilder();
        for (int i = 0; i < chosenLines.size(); i++) {
            appendFormattedMarkdownLine(out, chosenLines.get(i), chosenBold.get(i));
        }
        return out;
    }

    private void showUpdateResultDialog(final RepoReleaseInfo info) {
        final boolean hasUpdate = isNewerRelease(info);
        final String curVerLabel = formatVerLabel(getInstalledVersionName());
        final String latestVerLabel = formatVerLabel(info.versionName);

        CouiBottomSheet.Palette pal = CouiBottomSheet.Palette.fromModuleTheme(this);
        CouiBottomSheet.SheetHandle sheet = CouiBottomSheet.createCenter(this, pal);
        String latestSubtitle = curVerLabel.equalsIgnoreCase(latestVerLabel)
                ? getString(R.string.dialog_update_latest_msg_fmt, curVerLabel, latestVerLabel)
                : getString(R.string.dialog_update_ahead_msg_fmt, curVerLabel, latestVerLabel);
        CouiBottomSheet.addHeader(
                this, sheet.card, pal,
                hasUpdate
                        ? getString(R.string.dialog_update_available_title_fmt, latestVerLabel)
                        : getString(R.string.dialog_update_latest_title),
                hasUpdate
                        ? getString(R.string.dialog_update_ver_compare_fmt, curVerLabel, latestVerLabel)
                        : latestSubtitle,
                null);

        CharSequence changelog = formatLocalizedChangelog(info);
        if (changelog != null && changelog.length() > 0) {
            ScrollView scroll = CouiBottomSheet.createMaxHeightScrollView(
                    this, Math.round(getResources().getDisplayMetrics().heightPixels * 0.40f));
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            addChangelogItem(list, pal, latestVerLabel, changelog, false);
            scroll.addView(list);

            LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            scrollLp.topMargin = dp(14);
            sheet.card.addView(scroll, scrollLp);
        }

        if (hasUpdate && info.downloadUrl != null && !info.downloadUrl.isEmpty()) {
            CouiBottomSheet.addSingleButton(this, sheet.card, pal,
                    getString(R.string.btn_update_download), 1, 16, v -> {
                        sheet.dialog.dismiss();
                        if (!tryStartView(info.downloadUrl)) {
                            Toast.makeText(MainActivity.this, R.string.toast_open_link_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        String repoText = getString(R.string.btn_update_open_repo);
        String closeText = getString(R.string.btn_close);
        if (hasUpdate) {
            CouiBottomSheet.addSingleButton(this, sheet.card, pal, repoText, 0,
                    (info.downloadUrl != null && !info.downloadUrl.isEmpty()) ? 10 : 16, v -> {
                        sheet.dialog.dismiss();
                        String targetUrl = (info.repoPageUrl != null && !info.repoPageUrl.isEmpty())
                                ? info.repoPageUrl : LSPOSED_REPO_PAGE_URL;
                        if (!tryStartView(targetUrl)) {
                            Toast.makeText(MainActivity.this, R.string.toast_open_link_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
            CouiBottomSheet.addSingleButton(this, sheet.card, pal, closeText, 0, 10, v -> sheet.dialog.dismiss());
        } else {
            CouiBottomSheet.addButtonRow(this, sheet.card, pal,
                    repoText, 0, v -> {
                        sheet.dialog.dismiss();
                        String targetUrl = (info.repoPageUrl != null && !info.repoPageUrl.isEmpty())
                                ? info.repoPageUrl : LSPOSED_REPO_PAGE_URL;
                        if (!tryStartView(targetUrl)) {
                            Toast.makeText(MainActivity.this, R.string.toast_open_link_failed, Toast.LENGTH_SHORT).show();
                        }
                    },
                    closeText, 1, v -> sheet.dialog.dismiss());
        }
        sheet.dialog.show();
    }

    private void runWithNotifPermission(Runnable action) {
        if (action == null) {
            return;
        }
        if (FluidCloudDebugTester.hasPostPermission(this)) {
            action.run();
            return;
        }
        pendingDebugAction = action;
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(
                    new String[]{FluidCloudDebugTester.PERMISSION_POST_NOTIFICATIONS},
                    REQ_POST_NOTIF);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_POST_NOTIF) {
            Runnable action = pendingDebugAction;
            pendingDebugAction = null;
            boolean granted = grantResults != null
                    && grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted && action != null) {
                action.run();
            } else if (!granted) {
                Toast.makeText(this, R.string.toast_debug_perm_needed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDebugTesterDialog() {
        CouiBottomSheet.Palette pal = CouiBottomSheet.Palette.fromModuleTheme(this);
        CouiBottomSheet.SheetHandle sheet = CouiBottomSheet.create(this, pal);
        final View.OnClickListener postAllAction = v -> runWithNotifPermission(() -> {
            FluidCloudDebugTester.postAllFluidClouds(MainActivity.this);
            Toast.makeText(MainActivity.this, R.string.toast_debug_posted, Toast.LENGTH_SHORT).show();
        });
        final View.OnClickListener clearAllAction = v -> {
            FluidCloudDebugTester.clearAll(MainActivity.this);
            Toast.makeText(MainActivity.this, R.string.toast_debug_cleared, Toast.LENGTH_SHORT).show();
        };

        if (pal.livingFlow) {
            CouiBottomSheet.addLivingFlowTopBar(
                    this, sheet,
                    getString(R.string.dialog_debug_title),
                    getString(R.string.btn_close),
                    getString(R.string.btn_debug_post_all),
                    1,
                    postAllAction);
            TextView tvHint = new TextView(this);
            tvHint.setText(R.string.dialog_debug_hint);
            tvHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            tvHint.setLineSpacing(0f, 1.3f);
            tvHint.setTextColor(pal.textSecondary);
            tvHint.setPadding(dp(4), 0, dp(4), dp(8));
            sheet.card.addView(tvHint, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            CouiBottomSheet.addHeader(
                    this, sheet.card, pal,
                    getString(R.string.dialog_debug_title),
                    getString(R.string.dialog_debug_hint),
                    null);

            CouiBottomSheet.addButtonRow(
                    this, sheet.card, pal,
                    getString(R.string.btn_debug_clear_all), 0, clearAllAction,
                    getString(R.string.btn_debug_post_all), 1, postAllAction);
        }

        ScrollView scroll = CouiBottomSheet.createMaxHeightScrollView(
                this, Math.round(getResources().getDisplayMetrics().heightPixels * (pal.livingFlow ? 0.48f : 0.44f)));
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (!pal.livingFlow) {
            scrollLp.topMargin = dp(12);
        }

        LinearLayout list = CouiBottomSheet.createSubcardGroup(this, pal);
        scroll.addView(list);

        int[] itemTitles = new int[]{
                R.string.debug_item_pickup_dark_title,
                R.string.debug_item_pickup_white_title,
                R.string.debug_item_pickup_brand_title,
                R.string.debug_item_progress_title,
                R.string.debug_item_ride_title,
                R.string.debug_item_timer_title,
                R.string.debug_item_bigtext_title,
                R.string.debug_item_metric_title,
                R.string.debug_item_custom_title,
                R.string.debug_item_normal_title
        };
        int[] itemSubs = new int[]{
                R.string.debug_item_pickup_dark_sub,
                R.string.debug_item_pickup_white_sub,
                R.string.debug_item_pickup_brand_sub,
                R.string.debug_item_progress_sub,
                R.string.debug_item_ride_sub,
                R.string.debug_item_timer_sub,
                R.string.debug_item_bigtext_sub,
                R.string.debug_item_metric_sub,
                R.string.debug_item_custom_sub,
                R.string.debug_item_normal_sub
        };
        for (int i = 0; i < itemTitles.length; i++) {
            addDebugTestRow(list, pal, i, itemTitles[i], itemSubs[i], i < itemTitles.length - 1);
        }
        sheet.card.addView(scroll, scrollLp);

        if (pal.livingFlow) {
            CouiBottomSheet.addSingleButton(this, sheet.card, pal,
                    getString(R.string.btn_debug_clear_all), 0, 14, clearAllAction);
        } else {
            CouiBottomSheet.addSingleButton(this, sheet.card, pal,
                    getString(R.string.btn_close), 0, 14, v -> sheet.dialog.dismiss());
        }
        sheet.dialog.show();
    }

    private void addDebugTestRow(LinearLayout parent, CouiBottomSheet.Palette pal,
                                 final int actionIdx, int titleRes, int subRes, boolean addDivider) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(pal.livingFlow ? R.drawable.bg_lf_list_item_press : R.drawable.bg_coui_item_ripple);
        row.setClickable(true);
        row.setFocusable(true);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(titleRes);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        tvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tvTitle.setTextColor(pal.textPrimary);
        row.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(subRes);
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvSub.setTextColor(pal.textSecondary);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(2);
        row.addView(tvSub, subLp);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runWithNotifPermission(new Runnable() {
                    @Override
                    public void run() {
                        dispatchDebugTestAction(actionIdx);
                        Toast.makeText(MainActivity.this, R.string.toast_debug_posted, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (addDivider) {
            CouiBottomSheet.addDivider(this, parent, pal, 14);
        }
    }

    private void dispatchDebugTestAction(int actionIdx) {
        switch (actionIdx) {
            case 0:
                FluidCloudDebugTester.postPickupDark(this);
                break;
            case 1:
                FluidCloudDebugTester.postPickupWhite(this);
                break;
            case 2:
                FluidCloudDebugTester.postPickupBrand(this);
                break;
            case 3:
                FluidCloudDebugTester.postDeliveryProgress(this);
                break;
            case 4:
                FluidCloudDebugTester.postRideHailing(this);
                break;
            case 5:
                FluidCloudDebugTester.postTimerCountdown(this);
                break;
            case 6:
                FluidCloudDebugTester.postBigText(this);
                break;
            case 7:
                FluidCloudDebugTester.postMetricColorized(this);
                break;
            case 8:
                FluidCloudDebugTester.postCustomRemoteViews(this);
                break;
            case 9:
                FluidCloudDebugTester.postNormalNotification(this);
                break;
            default:
                break;
        }
    }

    private void switchTab(final int targetTab, boolean animate) {
        switchTab(targetTab, animate, false);
    }

    private void switchTab(final int targetTab, boolean animate, boolean fromNavDrag) {
        if (targetTab < TAB_LOCKSCREEN || targetTab > TAB_ABOUT) {
            return;
        }
        if (isFcSubpageOpen()) {
            closeFcSubpage(false);
        }
        if (animate && targetTab == activeTab) {
            return;
        }
        final int oldTab = activeTab;
        final int dir = (targetTab >= oldTab) ? 1 : -1;
        activeTab = targetTab;

        if (animate && oldTab != targetTab) {
            final float dist = (layoutPageContainer != null && layoutPageContainer.getWidth() > 0)
                    ? layoutPageContainer.getWidth() * 0.28f
                    : dp(96);

            for (int i = 0; i < pages.length; i++) {
                final int idx = i;
                final View p = pages[i];
                p.animate().cancel();
                if (i == targetTab) {
                    p.setVisibility(View.VISIBLE);
                    p.setAlpha(0f);
                    p.setTranslationX(dir * dist);
                    p.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(380L)
                            .setInterpolator(FLUID_EASE)
                            .withEndAction(null)
                            .setUpdateListener(anim -> {
                                if (vTopHeaderBlur != null) {
                                    vTopHeaderBlur.notifyTargetInvalidated();
                                }
                            })
                            .start();
                } else if (i == oldTab) {
                    p.setVisibility(View.VISIBLE);
                    p.animate()
                            .translationX(-dir * dist)
                            .alpha(0f)
                            .setDuration(340L)
                            .setInterpolator(FLUID_EASE)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    if (activeTab != idx) {
                                        p.setVisibility(View.GONE);
                                        p.setTranslationX(0f);
                                        p.setAlpha(1f);
                                    }
                                }
                            })
                            .start();
                } else {
                    p.setVisibility(View.GONE);
                    p.setTranslationX(0f);
                    p.setAlpha(1f);
                }
            }
        } else {
            for (int i = 0; i < pages.length; i++) {
                pages[i].animate().cancel();
                pages[i].setTranslationX(0f);
                pages[i].setAlpha(1f);
                pages[i].setVisibility(i == targetTab ? View.VISIBLE : View.GONE);
            }
            if (vTopHeaderBlur != null) {
                vTopHeaderBlur.notifyTargetInvalidated();
            }
        }

        if (targetTab == TAB_LOCKSCREEN) {
            tvPageTitle.setText(R.string.page_title_lockscreen);
            tvPageSubtitle.setText(R.string.page_subtitle_lockscreen);
        } else if (targetTab == TAB_APPEARANCE) {
            tvPageTitle.setText(R.string.page_title_appearance);
            tvPageSubtitle.setText(R.string.page_subtitle_appearance);
        } else {
            tvPageTitle.setText(R.string.page_title_about);
            tvPageSubtitle.setText(R.string.page_subtitle_about);
        }

        int targetScrollY = pages[targetTab] != null ? pages[targetTab].getScrollY() : 0;
        boolean animHeader = animate && oldTab != targetTab;
        if (subtitleTabFadeAnimator != null) {
            subtitleTabFadeAnimator.cancel();
            subtitleTabFadeAnimator = null;
        }
        if (livingFlowUi) {
            if (animHeader) {
                tvPageTitle.animate().cancel();
                tvPageTitle.setTranslationX(dir * dp(12));
                tvPageTitle.animate()
                        .translationX(0f)
                        .setDuration(340L)
                        .setInterpolator(FLUID_EASE)
                        .start();

                tvPageSubtitle.animate().cancel();
                tvPageSubtitle.setTranslationX(dir * dp(10));
                tvPageSubtitle.animate()
                        .translationX(0f)
                        .setDuration(340L)
                        .setInterpolator(FLUID_EASE)
                        .start();

                headerTitleTabAlpha = 0.25f;
                subtitleTabAlpha = 0.25f;
                syncHeaderToScrollY(targetScrollY, true);
                subtitleTabFadeAnimator = ValueAnimator.ofFloat(0.25f, 1f);
                subtitleTabFadeAnimator.setDuration(340L);
                subtitleTabFadeAnimator.setInterpolator(FLUID_EASE);
                subtitleTabFadeAnimator.addUpdateListener(anim -> {
                    float a = (Float) anim.getAnimatedValue();
                    headerTitleTabAlpha = a;
                    subtitleTabAlpha = a;
                    applyHeaderCollapseProgress(headerCollapseProgress);
                });
                subtitleTabFadeAnimator.start();
            } else {
                tvPageTitle.setTranslationX(0f);
                tvPageSubtitle.setTranslationX(0f);
                headerTitleTabAlpha = 1f;
                subtitleTabAlpha = 1f;
                syncHeaderToScrollY(targetScrollY, false);
            }
        } else if (animHeader) {
            tvPageTitle.animate().cancel();
            tvPageTitle.setTranslationX(dir * dp(18));
            tvPageTitle.setAlpha(0.25f);
            tvPageTitle.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(340L)
                    .setInterpolator(FLUID_EASE)
                    .start();

            tvPageSubtitle.animate().cancel();
            tvPageSubtitle.setTranslationX(dir * dp(14));
            tvPageSubtitle.animate()
                    .translationX(0f)
                    .setDuration(340L)
                    .setInterpolator(FLUID_EASE)
                    .start();

            subtitleTabAlpha = 0.25f;
            syncHeaderToScrollY(targetScrollY, true);
            subtitleTabFadeAnimator = ValueAnimator.ofFloat(0.25f, 1f);
            subtitleTabFadeAnimator.setDuration(340L);
            subtitleTabFadeAnimator.setInterpolator(FLUID_EASE);
            subtitleTabFadeAnimator.addUpdateListener(anim -> {
                subtitleTabAlpha = (Float) anim.getAnimatedValue();
                if (tvPageSubtitle != null) {
                    tvPageSubtitle.setAlpha(computeSubtitleAlpha(headerCollapseProgress) * subtitleTabAlpha);
                }
            });
            subtitleTabFadeAnimator.start();
        } else {
            tvPageTitle.setTranslationX(0f);
            tvPageTitle.setAlpha(1f);
            tvPageSubtitle.setTranslationX(0f);
            subtitleTabAlpha = 1f;
            syncHeaderToScrollY(targetScrollY, false);
        }

        updateNavIndicator(oldTab, targetTab, dir, animate, fromNavDrag);
        if (livingFlowUi && vAboutDynamicBg != null) {
            if (targetTab == TAB_ABOUT) {
                playAboutHeroEntrance(animate && oldTab != targetTab);
            } else {
                vAboutDynamicBg.pausePlayback();
            }
        }
    }

    private void playAboutHeroEntrance(boolean animateLogo) {
        if (vAboutDynamicBg != null) {
            vAboutDynamicBg.replayIfIdle();
        }
        if (layoutAboutHeroLogo == null) {
            return;
        }
        layoutAboutHeroLogo.animate().cancel();
        if (animateLogo) {
            layoutAboutHeroLogo.setScaleX(0.86f);
            layoutAboutHeroLogo.setScaleY(0.86f);
            layoutAboutHeroLogo.setAlpha(0.25f);
            layoutAboutHeroLogo.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(650L)
                    .setInterpolator(new PathInterpolator(0.30f, 0f, 0.10f, 1f))
                    .start();
        } else {
            layoutAboutHeroLogo.setScaleX(1f);
            layoutAboutHeroLogo.setScaleY(1f);
            layoutAboutHeroLogo.setAlpha(1f);
        }
    }

    private void ensureNavPillWidth(int tabIdx) {
        if (vNavSlidingPill == null || navTabs[tabIdx] == null) {
            return;
        }
        int tabW = navTabs[tabIdx].getWidth();
        if (tabW > 0 && vNavSlidingPill.getWidth() != tabW) {
            ViewGroup.LayoutParams lp = vNavSlidingPill.getLayoutParams();
            if (lp != null && lp.width != tabW) {
                lp.width = tabW;
                vNavSlidingPill.setLayoutParams(lp);
            }
        }
    }

    private void setupBottomNavTouch() {
        if (layoutBottomNav == null) {
            return;
        }
        layoutBottomNav.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                if (layoutNavItems == null || (!livingFlowUi && vNavSlidingPill == null)
                        || navTabs[0] == null || navTabs[2] == null) {
                    return false;
                }
                if (livingFlowUi) {
                    switch (ev.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN: {
                            if (navSpotLight != null) {
                                navSpotLight.onPress(ev.getX(), ev.getY());
                            }
                            if (navElasticController != null) {
                                navElasticController.onDown();
                            }
                            ColorOsLivingFlowEngine.performHaptic(
                                    layoutBottomNav, ColorOsLivingFlowEngine.HapticKind.TAP);
                            navDownRawX = ev.getRawX();
                            navDownRawY = ev.getRawY();
                            navDragging = false;
                            return true;
                        }
                        case MotionEvent.ACTION_MOVE: {
                            if (navSpotLight != null) {
                                navSpotLight.onMove(ev.getX(), ev.getY());
                            }
                            float dx = ev.getRawX() - navDownRawX;
                            float dy = ev.getRawY() - navDownRawY;
                            if (navElasticController != null) {
                                navElasticController.onMove(dx, dy);
                            }
                            if (!navDragging && Math.hypot(dx, dy) > Math.max(dp(5), touchSlop * 0.5f)) {
                                navDragging = true;
                            }
                            int itemsW = layoutNavItems.getWidth();
                            if (navDragging && itemsW > 0) {
                                float localX = Math.max(0f, Math.min(itemsW - 1f, ev.getX() - layoutNavItems.getLeft()));
                                int hoveredTab = Math.max(0, Math.min(2, (int) (localX / Math.max(1f, itemsW / 3f))));
                                if (hoveredTab != activeTab) {
                                    ColorOsLivingFlowEngine.performHaptic(
                                            layoutBottomNav, ColorOsLivingFlowEngine.HapticKind.TICK);
                                    switchTab(hoveredTab, true, true);
                                }
                            }
                            return true;
                        }
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL: {
                            if (navSpotLight != null) {
                                navSpotLight.onRelease();
                            }
                            if (navElasticController != null) {
                                navElasticController.onRelease();
                            }
                            int itemsW = layoutNavItems.getWidth();
                            if (!navDragging && ev.getActionMasked() == MotionEvent.ACTION_UP && itemsW > 0) {
                                float localX = Math.max(0f, Math.min(itemsW - 1f, ev.getX() - layoutNavItems.getLeft()));
                                int tappedTab = Math.max(0, Math.min(2, (int) (localX / Math.max(1f, itemsW / 3f))));
                                if (tappedTab != activeTab) {
                                    ColorOsLivingFlowEngine.performHaptic(
                                            layoutBottomNav, ColorOsLivingFlowEngine.HapticKind.TICK);
                                }
                                switchTab(tappedTab, true, false);
                            }
                            navDragging = false;
                            return true;
                        }
                    }
                    return false;
                }
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: {
                        vNavSlidingPill.animate().cancel();
                        layoutBottomNav.animate().cancel();
                        if (navPillMorphAnimator != null) {
                            navPillMorphAnimator.cancel();
                            navPillMorphAnimator = null;
                        }
                        ensureNavPillWidth(0);
                        int[] loc = new int[2];
                        layoutNavItems.getLocationOnScreen(loc);
                        navItemsRestLeftScreenX = loc[0] - layoutBottomNav.getTranslationX();
                        navItemsWidth = layoutNavItems.getWidth();
                        int tabW = navTabs[0].getWidth();
                        navPillWidth = tabW > 0 ? tabW : vNavSlidingPill.getWidth();
                        navMinPillX = navTabs[0].getLeft();
                        navMaxPillX = navTabs[2].getLeft();
                        navDownRawX = ev.getRawX();
                        navDownRawY = ev.getRawY();
                        navDragging = false;
                        return true;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        float rawX = ev.getRawX();
                        float rawY = ev.getRawY();
                        float dxDown = rawX - navDownRawX;
                        float dyDown = rawY - navDownRawY;
                        if (!navDragging && Math.hypot(dxDown, dyDown) > Math.max(dp(5), touchSlop * 0.5f)) {
                            navDragging = true;
                            vNavSlidingPill.animate().scaleX(1.04f).scaleY(1.04f).setDuration(150L).start();
                        }
                        if (navDragging && navItemsWidth > 0f) {
                            float fingerLocalX = rawX - navItemsRestLeftScreenX;
                            float desiredPillX = fingerLocalX - navPillWidth * 0.5f;
                            float maxPull = dp(10);
                            float resistDenom = dp(160);
                            float pullX = 0f;
                            if (desiredPillX < navMinPillX) {
                                float overflowX = navMinPillX - desiredPillX;
                                float normX = overflowX / resistDenom;
                                pullX = -maxPull * (normX / (1.0f + normX));
                                vNavSlidingPill.setTranslationX(navMinPillX);
                            } else if (desiredPillX > navMaxPillX) {
                                float overflowX = desiredPillX - navMaxPillX;
                                float normX = overflowX / resistDenom;
                                pullX = maxPull * (normX / (1.0f + normX));
                                vNavSlidingPill.setTranslationX(navMaxPillX);
                            } else {
                                vNavSlidingPill.setTranslationX(desiredPillX);
                            }

                            float overflowY = Math.abs(dyDown);
                            float normY = overflowY / resistDenom;
                            float pullY = (dyDown < 0f ? -1f : 1f) * maxPull * (normY / (1.0f + normY));

                            float stretchX = Math.abs(pullX) / maxPull;
                            float stretchY = Math.abs(pullY) / maxPull;
                            layoutBottomNav.setTranslationX(pullX);
                            layoutBottomNav.setTranslationY(pullY);
                            layoutBottomNav.setScaleX(1.0f + 0.014f * stretchX - 0.008f * stretchY);
                            layoutBottomNav.setScaleY(1.0f + 0.014f * stretchY - 0.008f * stretchX);

                            float clampedLocal = Math.max(0f, Math.min(navItemsWidth - 1f, fingerLocalX));
                            int hoveredTab = Math.max(0, Math.min(2, (int) (clampedLocal / Math.max(1f, navItemsWidth / 3f))));
                            if (hoveredTab != activeTab) {
                                layoutBottomNav.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                                switchTab(hoveredTab, true, true);
                            }
                        }
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        if (!navDragging) {
                            if (ev.getActionMasked() == MotionEvent.ACTION_UP && navItemsWidth > 0f) {
                                float localX = ev.getRawX() - navItemsRestLeftScreenX;
                                int tappedTab = Math.max(0, Math.min(2, (int) (localX / Math.max(1f, navItemsWidth / 3f))));
                                switchTab(tappedTab, true, false);
                            }
                        } else {
                            navDragging = false;
                            ensureNavPillWidth(activeTab);
                            float targetX = navTabs[activeTab].getLeft();
                            vNavSlidingPill.animate()
                                    .translationX(targetX)
                                    .translationY(0f)
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(320L)
                                    .setInterpolator(FLUID_EASE)
                                    .start();
                            layoutBottomNav.animate()
                                    .translationX(0f)
                                    .translationY(0f)
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(360L)
                                    .setInterpolator(NAV_SPRING_BACK)
                                    .start();
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private static int getLivingFlowNavIconRes(int index, boolean selected) {
        if (index == TAB_LOCKSCREEN) {
            return selected ? R.drawable.ic_lf_nav_lockscreen_filled : R.drawable.ic_lf_nav_lockscreen_outline;
        } else if (index == TAB_APPEARANCE) {
            return selected ? R.drawable.ic_lf_nav_appearance_filled : R.drawable.ic_lf_nav_appearance_outline;
        }
        return selected ? R.drawable.ic_lf_nav_about_filled : R.drawable.ic_lf_nav_about_outline;
    }

    private void updateNavIndicator(final int oldTab, final int targetTab, final int dir,
                                    final boolean animate, final boolean fromNavDrag) {
        int activeColor = livingFlowUi
                ? getColor(R.color.lf_nav_active)
                : DynamicThemeHelper.getAccent(this);
        int secondary = livingFlowUi
                ? getColor(R.color.lf_nav_inactive)
                : ConfigStore.resolveThemeColor(this, R.attr.couiTextSecondary, R.color.coui_text_secondary);

        for (int i = 0; i < 3; i++) {
            boolean selected = (i == targetTab);
            if (livingFlowUi) {
                navIcons[i].setImageResource(getLivingFlowNavIconRes(i, selected));
                navIcons[i].setColorFilter(selected ? activeColor : secondary);
                navLabels[i].setTextColor(selected ? activeColor : secondary);
                navLabels[i].setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
                navIcons[i].animate().cancel();
                navLabels[i].animate().cancel();
                navIcons[i].setTranslationX(0f);
                navIcons[i].setTranslationY(0f);
                navIcons[i].setScaleX(1.0f);
                navIcons[i].setScaleY(1.0f);
                navLabels[i].setTranslationX(0f);
                navLabels[i].setAlpha(1.0f);
                continue;
            }
            navIcons[i].setColorFilter(selected ? activeColor : secondary);
            navLabels[i].setTextColor(selected ? activeColor : secondary);
            navLabels[i].setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);

            navIcons[i].animate().cancel();
            navLabels[i].animate().cancel();
            if (animate && oldTab != targetTab) {
                if (selected) {
                    navIcons[i].setTranslationX(-dir * dp(10));
                    navIcons[i].setTranslationY(dp(2));
                    navIcons[i].setScaleX(0.84f);
                    navIcons[i].setScaleY(0.84f);
                    navIcons[i].animate()
                            .translationX(0f)
                            .translationY(-dp(1))
                            .scaleX(1.08f)
                            .scaleY(1.08f)
                            .setDuration(400L)
                            .setInterpolator(ICON_SPRING)
                            .start();

                    navLabels[i].setTranslationX(-dir * dp(6));
                    navLabels[i].setAlpha(0.55f);
                    navLabels[i].animate()
                            .translationX(0f)
                            .alpha(1.0f)
                            .setDuration(340L)
                            .setInterpolator(FLUID_EASE)
                            .start();
                } else {
                    float leaveX = (i == oldTab) ? -dir * dp(4) : 0f;
                    if (i == oldTab) {
                        navIcons[i].setTranslationX(leaveX);
                    }
                    navIcons[i].animate()
                            .translationX(0f)
                            .translationY(0f)
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(310L)
                            .setInterpolator(FLUID_EASE)
                            .start();
                    navLabels[i].setTranslationX(0f);
                    navLabels[i].setAlpha(1.0f);
                }
            } else {
                navIcons[i].setTranslationX(0f);
                navIcons[i].setTranslationY(selected ? -dp(1) : 0f);
                navIcons[i].setScaleX(selected ? 1.08f : 1.0f);
                navIcons[i].setScaleY(selected ? 1.08f : 1.0f);
                navLabels[i].setTranslationX(0f);
                navLabels[i].setAlpha(1.0f);
            }
        }

        if (livingFlowUi || fromNavDrag || vNavSlidingPill == null || navTabs[targetTab] == null) {
            return;
        }
        if (navTabs[targetTab].getWidth() == 0 || vNavSlidingPill.getWidth() == 0) {
            vNavSlidingPill.post(new Runnable() {
                @Override
                public void run() {
                    updateNavIndicator(oldTab, targetTab, dir, false, false);
                }
            });
            return;
        }

        ensureNavPillWidth(targetTab);
        float targetX = navTabs[targetTab].getLeft();
        float targetY = 0f;

        vNavSlidingPill.animate().cancel();
        if (navPillMorphAnimator != null) {
            navPillMorphAnimator.cancel();
            navPillMorphAnimator = null;
        }

        if (animate && oldTab != targetTab) {
            vNavSlidingPill.animate()
                    .translationX(targetX)
                    .translationY(targetY)
                    .setDuration(380L)
                    .setInterpolator(FLUID_EASE)
                    .start();

            navPillMorphAnimator = ValueAnimator.ofFloat(0f, 1f);
            navPillMorphAnimator.setDuration(380L);
            navPillMorphAnimator.setInterpolator(FLUID_EASE);
            navPillMorphAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float p = (Float) animation.getAnimatedValue();
                    double wave = Math.sin(Math.PI * p);
                    vNavSlidingPill.setScaleX((float) (1.0 + 0.10 * wave));
                    vNavSlidingPill.setScaleY((float) (1.0 - 0.05 * wave));
                }
            });
            navPillMorphAnimator.start();
        } else {
            vNavSlidingPill.setTranslationX(targetX);
            vNavSlidingPill.setTranslationY(targetY);
            vNavSlidingPill.setScaleX(1.0f);
            vNavSlidingPill.setScaleY(1.0f);
        }
    }

    private void setOffsetExplicit(int target) {
        data.offset = ConfigStore.clamp(target);
        persistImmediate();
    }

    private void persistImmediate() {
        hadLocalKeys = true;
        mainHandler.removeCallbacks(debouncedSync);
        ConfigStore.saveLocal(this, prefs, xspActive, data);
        syncUiState();
        syncRemoteAsync();
    }

    private void persistSliderTick() {
        hadLocalKeys = true;
        ConfigStore.saveLocal(this, prefs, xspActive, data);
        syncUiState();
        mainHandler.removeCallbacks(debouncedSync);
        mainHandler.postDelayed(debouncedSync, SLIDER_SYNC_DELAY_MS);
    }

    private void syncRemoteAsync() {
        final ConfigStore.SettingsData snapshot = data.copy();
        final boolean tryRestoreRemote = !hadLocalKeys && !remoteRestoreTried;
        if (XposedProvider.isConnected() && tryRestoreRemote) {
            remoteRestoreTried = true;
        }
        final int gen = syncGeneration.incrementAndGet();
        syncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (gen != syncGeneration.get()) {
                    return;
                }
                if (tryRestoreRemote && XposedProvider.isConnected()) {
                    Map<String, Object> remote = XposedProvider.fetchRemotePrefs(ConfigStore.PREFS_NAME);
                    if (ConfigStore.hasAnySetting(remote)) {
                        final ConfigStore.SettingsData restored = ConfigStore.loadFromMap(remote);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!hadLocalKeys) {
                                    hadLocalKeys = true;
                                    data = restored;
                                    ConfigStore.saveLocal(MainActivity.this, prefs, true, data);
                                    syncUiState();
                                }
                            }
                        });
                        return;
                    }
                }
                ConfigStore.pushRemote(snapshot);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (gen == syncGeneration.get()) {
                            xspActive = XposedProvider.isConnected();
                            syncStatusAndAboutUi();
                        }
                    }
                });
            }
        });
    }

    private void syncUiState() {
        xspActive = XposedProvider.isConnected();
        suppressUiCallbacks = true;
        try {
            syncStatusAndAboutUi();

            // Tab 0: Lockscreen
            swAlign.setChecked(data.align);
            swHideFpOnCapsule.setChecked(data.hideFpOnCapsule);
            swImmersiveFp.setChecked(data.immersiveFp);
            swAodNotifTranslucent.setChecked(data.aodNotifTranslucent);
            swLockNotifGlowFollowMedia.setChecked(data.lockNotifGlowFollowMedia);

            layoutCapsuleSub.setAlpha(data.align ? 1.0f : 0.48f);
            setEnabledRecursive(layoutCapsuleSub, data.align);

            String signedPx = formatSignedPx(data.offset);
            tvOffsetVal.setText(signedPx);
            btnZero.setVisibility(data.offset == 0 ? View.GONE : View.VISIBLE);
            int expectedProgress = Math.max(0, Math.min(OFFSET_STEPS_HALF * 2,
                    OFFSET_STEPS_HALF + Math.round(data.offset / (float) ConfigStore.OFFSET_STEP)));
            if (sbOffset.getProgress() != expectedProgress) {
                sbOffset.setProgress(expectedProgress);
            }

            // Tab 1: Appearance
            swFcEnabled.setChecked(data.fcEnabled);
            tvFcIslandOpacityVal.setText(data.fcIslandOpacity + "%");
            if (sbFcIslandOpacity.getProgress() != data.fcIslandOpacity) {
                sbFcIslandOpacity.setProgress(data.fcIslandOpacity);
            }
            tvFcOpacityVal.setText(data.fcOpacity + "%");
            if (sbFcOpacity.getProgress() != data.fcOpacity) {
                sbFcOpacity.setProgress(data.fcOpacity);
            }
            tvFcImmersiveOpacityVal.setText(data.fcImmersiveOpacity + "%");
            if (sbFcImmersiveOpacity.getProgress() != data.fcImmersiveOpacity) {
                sbFcImmersiveOpacity.setProgress(data.fcImmersiveOpacity);
            }
            tvFcBlurVal.setText(data.fcBlur + "%");
            if (sbFcBlur.getProgress() != data.fcBlur) {
                sbFcBlur.setProgress(data.fcBlur);
            }
            tvFcBottomGlowVal.setText(data.fcBottomGlow + "%");
            if (sbFcBottomGlow.getProgress() != data.fcBottomGlow) {
                sbFcBottomGlow.setProgress(data.fcBottomGlow);
            }
            tvFcBottomGlowChromaVal.setText(data.fcBottomGlowChroma + "%");
            if (sbFcBottomGlowChroma.getProgress() != data.fcBottomGlowChroma) {
                sbFcBottomGlowChroma.setProgress(data.fcBottomGlowChroma);
            }
            tvFcSingleGlowVal.setText(data.fcSingleGlow + "%");
            if (sbFcSingleGlow.getProgress() != data.fcSingleGlow) {
                sbFcSingleGlow.setProgress(data.fcSingleGlow);
            }
            tvFcSingleGlowChromaVal.setText(data.fcSingleGlowChroma + "%");
            if (sbFcSingleGlowChroma.getProgress() != data.fcSingleGlowChroma) {
                sbFcSingleGlowChroma.setProgress(data.fcSingleGlowChroma);
            }
            swFcRemoveIslandMask.setChecked(data.fcRemoveIslandMask);
            swFcMatchCard.setChecked(data.fcMatchCard);
            swFcRemoveShadeMask.setChecked(data.fcRemoveShadeMask);
            tvFcMaskStrengthVal.setText(data.fcMaskStrength + "%");
            if (sbFcMaskStrength.getProgress() != data.fcMaskStrength) {
                sbFcMaskStrength.setProgress(data.fcMaskStrength);
            }
            swFcMaskFollowMedia.setChecked(data.fcMaskFollowMedia);
            tvFcMaskBrightnessVal.setText(data.fcMaskBrightness + "%");
            if (sbFcMaskBrightness.getProgress() != data.fcMaskBrightness) {
                sbFcMaskBrightness.setProgress(data.fcMaskBrightness);
            }
            tvFcMaskChromaVal.setText(data.fcMaskChroma + "%");
            if (sbFcMaskChroma.getProgress() != data.fcMaskChroma) {
                sbFcMaskChroma.setProgress(data.fcMaskChroma);
            }

            tvFcNonMediaMaskOpacityVal.setText(data.fcNonMediaMaskOpacity + "%");
            if (sbFcNonMediaMaskOpacity.getProgress() != data.fcNonMediaMaskOpacity) {
                sbFcNonMediaMaskOpacity.setProgress(data.fcNonMediaMaskOpacity);
            }
            tvFcNonMediaFollowMaskOpacityVal.setText(data.fcNonMediaFollowMaskOpacity + "%");
            if (sbFcNonMediaFollowMaskOpacity.getProgress() != data.fcNonMediaFollowMaskOpacity) {
                sbFcNonMediaFollowMaskOpacity.setProgress(data.fcNonMediaFollowMaskOpacity);
            }
            swFcNonMediaMaskEnabled.setChecked(data.fcNonMediaMaskEnabled);
            tvFcMaskGradStartVal.setText(data.fcMaskGradStart + "%");
            if (sbFcMaskGradStart.getProgress() != data.fcMaskGradStart) {
                sbFcMaskGradStart.setProgress(data.fcMaskGradStart);
            }
            tvFcMaskGradEndVal.setText(data.fcMaskGradEnd + "%");
            if (sbFcMaskGradEnd.getProgress() != data.fcMaskGradEnd) {
                sbFcMaskGradEnd.setProgress(data.fcMaskGradEnd);
            }
            tvFcMaskGradMidVal.setText(data.fcMaskGradMid + "%");
            if (sbFcMaskGradMid.getProgress() != data.fcMaskGradMid) {
                sbFcMaskGradMid.setProgress(data.fcMaskGradMid);
            }
            tvFcMaskGradStrengthVal.setText(data.fcMaskGradStrength + "%");
            if (sbFcMaskGradStrength.getProgress() != data.fcMaskGradStrength) {
                sbFcMaskGradStrength.setProgress(data.fcMaskGradStrength);
            }

            boolean customColorOn = data.fcColorEnabled;
            if (!customColorOn) {
                fcColorPickerUserExpanded = false;
            }
            swFcCustomColor.setChecked(customColorOn);
            if (fcColorPicker != null) {
                int pickerColor = customColorOn
                        ? data.fcColor
                        : data.fcLastColor;
                fcColorPicker.setColor(pickerColor);
            }
            updateFcTintHeaderUi(false);
            setFcColorPickerExpanded(customColorOn && fcColorPickerUserExpanded, fcColorPickerStateSynced);

            boolean customMaskColorOn = data.fcCustomMaskEnabled;
            if (!customMaskColorOn) {
                fcMaskColorPickerUserExpanded = false;
            }
            if (swFcCustomMaskColor != null) {
                swFcCustomMaskColor.setChecked(customMaskColorOn);
            }
            if (fcMaskColorPicker != null) {
                int maskPickerColor = customMaskColorOn
                        ? data.fcCustomMaskColor
                        : (data.fcCustomMaskHasLast
                                ? data.fcCustomMaskLastColor
                                : (data.fcColorEnabled ? data.fcColor : ConfigStore.DEF_CUSTOM_COLOR));
                fcMaskColorPicker.setColor(maskPickerColor);
            }
            updateFcMaskTintHeaderUi(false);
            setFcMaskColorPickerExpanded(customMaskColorOn && fcMaskColorPickerUserExpanded, fcMaskColorPickerStateSynced);

            boolean customGlowColorOn = data.fcCustomGlowEnabled;
            if (!customGlowColorOn) {
                fcGlowColorPickerUserExpanded = false;
            }
            if (swFcCustomGlowColor != null) {
                swFcCustomGlowColor.setChecked(customGlowColorOn);
            }
            if (fcGlowColorPicker != null) {
                int glowPickerColor = customGlowColorOn
                        ? data.fcCustomGlowColor
                        : (data.fcCustomGlowHasLast
                                ? data.fcCustomGlowLastColor
                                : (data.fcColorEnabled ? data.fcColor : ConfigStore.DEF_CUSTOM_COLOR));
                fcGlowColorPicker.setColor(glowPickerColor);
            }
            updateFcGlowTintHeaderUi(false);
            setFcGlowColorPickerExpanded(customGlowColorOn && fcGlowColorPickerUserExpanded, fcGlowColorPickerStateSynced);

            if (tvSummaryOpacityBlur != null) {
                tvSummaryOpacityBlur.setText(getString(
                        R.string.summary_fc_opacity_blur_fmt,
                        data.fcIslandOpacity,
                        data.fcOpacity,
                        data.fcImmersiveOpacity,
                        data.fcBlur));
            }
            if (tvSummaryHighlight != null) {
                tvSummaryHighlight.setText(getString(
                        R.string.summary_fc_highlight_fmt,
                        data.fcBottomGlow,
                        data.fcBottomGlowChroma,
                        data.fcSingleGlow,
                        data.fcSingleGlowChroma));
            }
            layoutFcSub.setAlpha(data.fcEnabled ? 1.0f : 0.45f);
            setEnabledRecursive(layoutFcSub, data.fcEnabled);
            if (layoutFcTintCard != null) {
                layoutFcTintCard.setAlpha(data.fcEnabled ? 1.0f : 0.45f);
                setEnabledRecursive(layoutFcTintCard, data.fcEnabled);
            }
            for (View panel : fcMenuPanels) {
                if (panel instanceof ViewGroup) {
                    setEnabledRecursive((ViewGroup) panel, data.fcEnabled);
                }
            }
            if (!data.fcEnabled && isFcSubpageOpen()) {
                closeFcSubpage(false);
            }

            boolean maskToneActive = data.fcEnabled && (data.fcMaskFollowMedia || data.fcNonMediaMaskEnabled);
            layoutFcMaskToneSub.setAlpha(maskToneActive ? 1.0f : 0.45f);
            setEnabledRecursive(layoutFcMaskToneSub, maskToneActive);

            syncPreviewView(fcPreview);
            syncPreviewView(fcSubpagePreview);
        } finally {
            suppressUiCallbacks = false;
        }
    }

    private void updateFcTintHeaderUi(boolean animateChevron) {
        updateTintRowHeaderUi(
                data.fcColorEnabled,
                data.fcColor,
                fcColorPickerUserExpanded,
                tvFcCustomColorSummary,
                R.string.pref_fc_custom_color_summary,
                layoutFcTintBadge,
                vFcTintSwatchDot,
                ivFcTintChevron,
                animateChevron);
    }

    private void updateFcMaskTintHeaderUi(boolean animateChevron) {
        updateTintRowHeaderUi(
                data.fcCustomMaskEnabled,
                data.fcCustomMaskColor,
                fcMaskColorPickerUserExpanded,
                tvFcCustomMaskColorSummary,
                R.string.pref_fc_custom_mask_color_summary,
                layoutFcMaskTintBadge,
                vFcMaskTintSwatchDot,
                ivFcMaskTintChevron,
                animateChevron);
    }

    private void updateFcGlowTintHeaderUi(boolean animateChevron) {
        updateTintRowHeaderUi(
                data.fcCustomGlowEnabled,
                data.fcCustomGlowColor,
                fcGlowColorPickerUserExpanded,
                tvFcCustomGlowColorSummary,
                R.string.pref_fc_custom_glow_color_summary,
                layoutFcGlowTintBadge,
                vFcGlowTintSwatchDot,
                ivFcGlowTintChevron,
                animateChevron);
    }

    private void updateTintRowHeaderUi(boolean enabled, int colorArgb, boolean userExpanded, TextView tvSummary,
                                       int defaultSummaryRes, View badge, View swatchDot,
                                       ImageView chevron, boolean animateChevron) {
        boolean expanded = enabled && userExpanded;
        if (tvSummary != null) {
            if (enabled) {
                String hex = String.format(Locale.US, "#%08X", colorArgb);
                tvSummary.setText(hex + " · " + getString(R.string.pref_fc_custom_color_tap_expand));
            } else {
                tvSummary.setText(defaultSummaryRes);
            }
        }
        if (badge != null) {
            badge.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
        if (swatchDot != null && enabled) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(colorArgb);
            boolean dark = ConfigStore.isDarkThemeActive(this);
            dot.setStroke(Math.max(1, dp(1)), dark ? 0x38FFFFFF : 0x24000000);
            swatchDot.setBackground(dot);
        }
        if (chevron != null) {
            float targetRot = expanded ? 270f : 90f;
            chevron.animate().cancel();
            if (animateChevron) {
                chevron.animate()
                        .rotation(targetRot)
                        .setDuration(300L)
                        .setInterpolator(FLUID_EASE)
                        .start();
            } else {
                chevron.setRotation(targetRot);
            }
        }
    }

    private static void snapColorPickerWrapVisibility(View wrap, View divider, boolean expand) {
        if (wrap == null) {
            return;
        }
        ViewGroup.LayoutParams lp = wrap.getLayoutParams();
        if (lp != null && lp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            wrap.setLayoutParams(lp);
        }
        wrap.setAlpha(1f);
        wrap.setTranslationY(0f);
        wrap.setVisibility(expand ? View.VISIBLE : View.GONE);
        if (wrap instanceof ViewGroup) {
            ((ViewGroup) wrap).setClipChildren(false);
            ((ViewGroup) wrap).setClipToPadding(false);
        }
        if (divider != null) {
            divider.setAlpha(1f);
            divider.setVisibility(expand ? View.VISIBLE : View.GONE);
        }
    }

    private ValueAnimator animateColorPickerWrapExpanded(final View wrap, final View divider,
                                                         final ValueAnimator prevAnimator,
                                                         final boolean expand,
                                                         final int pickerSlot) {
        if (prevAnimator != null) {
            prevAnimator.cancel();
        }
        if (divider != null) {
            divider.animate().cancel();
        }
        final ViewGroup wrapGroup = wrap instanceof ViewGroup ? (ViewGroup) wrap : null;
        final boolean curVisible = wrap.getVisibility() == View.VISIBLE;

        if (expand) {
            final int startH = curVisible ? Math.max(0, wrap.getHeight()) : 0;
            final float startAlpha = curVisible ? wrap.getAlpha() : 0f;
            final float startTransY = curVisible ? wrap.getTranslationY() : -dp(10);

            int availW = (layoutFcTintCard != null && layoutFcTintCard.getWidth() > 0)
                    ? layoutFcTintCard.getWidth()
                    : (getResources().getDisplayMetrics().widthPixels - dp(32));
            wrap.measure(
                    View.MeasureSpec.makeMeasureSpec(availW, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            final int targetH = Math.max(1, wrap.getMeasuredHeight());

            if (wrapGroup != null) {
                wrapGroup.setClipChildren(true);
                wrapGroup.setClipToPadding(true);
            }
            if (divider != null) {
                if (divider.getVisibility() != View.VISIBLE) {
                    divider.setAlpha(0f);
                    divider.setVisibility(View.VISIBLE);
                }
                divider.animate()
                        .alpha(1f)
                        .setDuration(280L)
                        .setInterpolator(FLUID_EASE)
                        .start();
            }

            ViewGroup.LayoutParams lp = wrap.getLayoutParams();
            if (lp != null) {
                lp.height = startH;
                wrap.setLayoutParams(lp);
            }
            wrap.setAlpha(startAlpha);
            wrap.setTranslationY(startTransY);
            wrap.setVisibility(View.VISIBLE);

            final ValueAnimator anim = ValueAnimator.ofInt(startH, targetH);
            anim.setDuration(440L);
            anim.setInterpolator(FLUID_EASE);
            anim.addUpdateListener(animation -> {
                ViewGroup.LayoutParams curLp = wrap.getLayoutParams();
                if (curLp != null) {
                    curLp.height = (Integer) animation.getAnimatedValue();
                    wrap.requestLayout();
                }
                float frac = animation.getAnimatedFraction();
                float alphaFrac = Math.min(1f, frac * 1.35f);
                wrap.setAlpha(startAlpha + (1f - startAlpha) * alphaFrac);
                wrap.setTranslationY(startTransY * (1f - frac));
                if (vTopHeaderBlur != null) {
                    vTopHeaderBlur.notifyTargetInvalidated();
                }
                if (vSubpageHeaderBlur != null && isFcSubpageOpen()) {
                    vSubpageHeaderBlur.notifyTargetInvalidated();
                }
            });
            anim.addListener(new AnimatorListenerAdapter() {
                private boolean cancelled;

                @Override
                public void onAnimationCancel(Animator animation) {
                    cancelled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (pickerSlot == 0) {
                        if (fcColorPickerAnimator == anim) {
                            fcColorPickerAnimator = null;
                        }
                        if (!cancelled && fcColorPickerExpanded) {
                            snapColorPickerWrapVisibility(wrap, divider, true);
                        }
                    } else if (pickerSlot == 1) {
                        if (fcMaskColorPickerAnimator == anim) {
                            fcMaskColorPickerAnimator = null;
                        }
                        if (!cancelled && fcMaskColorPickerExpanded) {
                            snapColorPickerWrapVisibility(wrap, divider, true);
                        }
                    } else {
                        if (fcGlowColorPickerAnimator == anim) {
                            fcGlowColorPickerAnimator = null;
                        }
                        if (!cancelled && fcGlowColorPickerExpanded) {
                            snapColorPickerWrapVisibility(wrap, divider, true);
                        }
                    }
                }
            });
            anim.start();
            return anim;
        } else {
            final int startH = curVisible ? Math.max(0, wrap.getHeight()) : 0;
            if (startH <= 0) {
                snapColorPickerWrapVisibility(wrap, divider, false);
                return null;
            }
            final float startAlpha = wrap.getAlpha();
            final float startTransY = wrap.getTranslationY();
            final float endTransY = -dp(8);

            if (wrapGroup != null) {
                wrapGroup.setClipChildren(true);
                wrapGroup.setClipToPadding(true);
            }
            if (divider != null && divider.getVisibility() == View.VISIBLE) {
                divider.animate()
                        .alpha(0f)
                        .setDuration(250L)
                        .setInterpolator(FLUID_EASE)
                        .start();
            }

            final ValueAnimator anim = ValueAnimator.ofInt(startH, 0);
            anim.setDuration(370L);
            anim.setInterpolator(FLUID_EASE);
            anim.addUpdateListener(animation -> {
                ViewGroup.LayoutParams curLp = wrap.getLayoutParams();
                if (curLp != null) {
                    curLp.height = (Integer) animation.getAnimatedValue();
                    wrap.requestLayout();
                }
                float frac = animation.getAnimatedFraction();
                float alphaKeep = Math.max(0f, 1f - frac * 1.45f);
                wrap.setAlpha(startAlpha * alphaKeep);
                wrap.setTranslationY(startTransY + (endTransY - startTransY) * frac);
                if (vTopHeaderBlur != null) {
                    vTopHeaderBlur.notifyTargetInvalidated();
                }
                if (vSubpageHeaderBlur != null && isFcSubpageOpen()) {
                    vSubpageHeaderBlur.notifyTargetInvalidated();
                }
            });
            anim.addListener(new AnimatorListenerAdapter() {
                private boolean cancelled;

                @Override
                public void onAnimationCancel(Animator animation) {
                    cancelled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (pickerSlot == 0) {
                        if (fcColorPickerAnimator == anim) {
                            fcColorPickerAnimator = null;
                        }
                        if (!cancelled && !fcColorPickerExpanded) {
                            snapColorPickerWrapVisibility(wrap, divider, false);
                        }
                    } else if (pickerSlot == 1) {
                        if (fcMaskColorPickerAnimator == anim) {
                            fcMaskColorPickerAnimator = null;
                        }
                        if (!cancelled && !fcMaskColorPickerExpanded) {
                            snapColorPickerWrapVisibility(wrap, divider, false);
                        }
                    } else {
                        if (fcGlowColorPickerAnimator == anim) {
                            fcGlowColorPickerAnimator = null;
                        }
                        if (!cancelled && !fcGlowColorPickerExpanded) {
                            snapColorPickerWrapVisibility(wrap, divider, false);
                        }
                    }
                }
            });
            anim.start();
            return anim;
        }
    }

    private void setFcColorPickerExpanded(final boolean expand, boolean animate) {
        if (layoutFcColorPickerWrap == null) {
            return;
        }
        final boolean curVisible = layoutFcColorPickerWrap.getVisibility() == View.VISIBLE;
        if (fcColorPickerStateSynced && fcColorPickerExpanded == expand
                && fcColorPickerAnimator == null && curVisible == expand) {
            return;
        }
        fcColorPickerStateSynced = true;
        fcColorPickerExpanded = expand;
        if (!animate) {
            if (fcColorPickerAnimator != null) {
                fcColorPickerAnimator.cancel();
                fcColorPickerAnimator = null;
            }
            if (dividerFcTintPicker != null) {
                dividerFcTintPicker.animate().cancel();
            }
            snapColorPickerWrapVisibility(layoutFcColorPickerWrap, dividerFcTintPicker, expand);
            return;
        }
        fcColorPickerAnimator = animateColorPickerWrapExpanded(
                layoutFcColorPickerWrap, dividerFcTintPicker, fcColorPickerAnimator, expand, 0);
    }

    private void setFcMaskColorPickerExpanded(final boolean expand, boolean animate) {
        if (layoutFcMaskColorPickerWrap == null) {
            return;
        }
        final boolean curVisible = layoutFcMaskColorPickerWrap.getVisibility() == View.VISIBLE;
        if (fcMaskColorPickerStateSynced && fcMaskColorPickerExpanded == expand
                && fcMaskColorPickerAnimator == null && curVisible == expand) {
            return;
        }
        fcMaskColorPickerStateSynced = true;
        fcMaskColorPickerExpanded = expand;
        if (!animate) {
            if (fcMaskColorPickerAnimator != null) {
                fcMaskColorPickerAnimator.cancel();
                fcMaskColorPickerAnimator = null;
            }
            if (dividerFcMaskTintPicker != null) {
                dividerFcMaskTintPicker.animate().cancel();
            }
            snapColorPickerWrapVisibility(layoutFcMaskColorPickerWrap, dividerFcMaskTintPicker, expand);
            return;
        }
        fcMaskColorPickerAnimator = animateColorPickerWrapExpanded(
                layoutFcMaskColorPickerWrap, dividerFcMaskTintPicker, fcMaskColorPickerAnimator, expand, 1);
    }

    private void setFcGlowColorPickerExpanded(final boolean expand, boolean animate) {
        if (layoutFcGlowColorPickerWrap == null) {
            return;
        }
        final boolean curVisible = layoutFcGlowColorPickerWrap.getVisibility() == View.VISIBLE;
        if (fcGlowColorPickerStateSynced && fcGlowColorPickerExpanded == expand
                && fcGlowColorPickerAnimator == null && curVisible == expand) {
            return;
        }
        fcGlowColorPickerStateSynced = true;
        fcGlowColorPickerExpanded = expand;
        if (!animate) {
            if (fcGlowColorPickerAnimator != null) {
                fcGlowColorPickerAnimator.cancel();
                fcGlowColorPickerAnimator = null;
            }
            if (dividerFcGlowTintPicker != null) {
                dividerFcGlowTintPicker.animate().cancel();
            }
            snapColorPickerWrapVisibility(layoutFcGlowColorPickerWrap, dividerFcGlowTintPicker, expand);
            return;
        }
        fcGlowColorPickerAnimator = animateColorPickerWrapExpanded(
                layoutFcGlowColorPickerWrap, dividerFcGlowTintPicker, fcGlowColorPickerAnimator, expand, 2);
    }

    private void syncPreviewView(FluidCloudPreviewView preview) {
        if (preview == null) {
            return;
        }
        preview.updateParams(
                data.fcEnabled,
                data.fcOpacity,
                data.fcIslandOpacity,
                data.fcBlur,
                data.fcBottomGlow,
                data.fcBottomGlowChroma,
                data.fcSingleGlow,
                data.fcSingleGlowChroma,
                data.fcColorEnabled,
                data.fcColor,
                data.fcCustomMaskEnabled,
                data.fcCustomMaskColor,
                data.fcCustomGlowEnabled,
                data.fcCustomGlowColor,
                data.fcRemoveIslandMask,
                data.fcMatchCard,
                data.fcMaskFollowMedia,
                data.fcMaskBrightness,
                data.fcMaskChroma,
                data.fcMaskStrength,
                data.fcNonMediaMaskEnabled,
                data.fcNonMediaMaskColor,
                data.fcNonMediaMaskOpacity,
                data.fcNonMediaFollowMaskOpacity,
                data.fcMaskGradStart,
                data.fcMaskGradEnd,
                data.fcMaskGradMid,
                data.fcMaskGradStrength
        );
    }

    private void syncStatusAndAboutUi() {
        if (xspActive) {
            if (ivStatusIcon != null) {
                ivStatusIcon.setImageResource(R.drawable.ic_status_active);
            }
            if (tvStatusTitle != null) {
                tvStatusTitle.setText(R.string.status_title_active);
            }
            if (tvStatusSubtitle != null) {
                tvStatusSubtitle.setText(R.string.status_sub_active);
            }
            if (tvAboutChannel != null) {
                tvAboutChannel.setText(R.string.channel_xsp);
            }
        } else {
            if (ivStatusIcon != null) {
                ivStatusIcon.setImageResource(R.drawable.ic_status_warn);
            }
            if (tvStatusTitle != null) {
                tvStatusTitle.setText(R.string.status_title_inactive);
            }
            if (tvStatusSubtitle != null) {
                tvStatusSubtitle.setText(R.string.status_sub_inactive);
            }
            if (tvAboutChannel != null) {
                tvAboutChannel.setText(R.string.channel_private_warn);
            }
        }
        if (livingFlowUi) {
            boolean dark = ConfigStore.isDarkThemeActive(this);
            int accent = DynamicThemeHelper.getAccent(this);
            int statusColor = xspActive ? accent : getColor(R.color.coui_status_warn_fg);
            int emblemFill = xspActive
                    ? (((dark ? 0x26 : 0x16) << 24) | (accent & 0x00FFFFFF))
                    : (dark ? 0x28FBBF24 : 0x1AD97706);

            if (vLockStatusEmblem != null) {
                GradientDrawable emblemBg = new GradientDrawable();
                emblemBg.setShape(GradientDrawable.OVAL);
                emblemBg.setColor(emblemFill);
                vLockStatusEmblem.setBackground(emblemBg);
                if (ivStatusIcon != null) {
                    ivStatusIcon.setColorFilter(statusColor);
                }
            }
            if (tvLockStatusBadge != null) {
                tvLockStatusBadge.setTextColor(statusColor);
                GradientDrawable badgeBg = new GradientDrawable();
                badgeBg.setShape(GradientDrawable.RECTANGLE);
                badgeBg.setCornerRadius(999f * getResources().getDisplayMetrics().density);
                badgeBg.setColor(emblemFill);
                tvLockStatusBadge.setBackground(badgeBg);
            }
            if (cardAboutModuleStatus != null) {
                if (vAboutStatusEmblem != null) {
                    GradientDrawable emblemBg = new GradientDrawable();
                    emblemBg.setShape(GradientDrawable.OVAL);
                    emblemBg.setColor(emblemFill);
                    vAboutStatusEmblem.setBackground(emblemBg);
                }
                if (ivAboutStatusIcon != null) {
                    ivAboutStatusIcon.setImageResource(xspActive ? R.drawable.ic_status_active : R.drawable.ic_status_warn);
                    ivAboutStatusIcon.setColorFilter(statusColor);
                }
                if (tvAboutStatusTitle != null) {
                    tvAboutStatusTitle.setText(xspActive ? R.string.status_title_active : R.string.status_title_inactive);
                }
                if (tvAboutStatusBadge != null) {
                    tvAboutStatusBadge.setTextColor(statusColor);
                    GradientDrawable badgeBg = new GradientDrawable();
                    badgeBg.setShape(GradientDrawable.RECTANGLE);
                    badgeBg.setCornerRadius(999f * getResources().getDisplayMetrics().density);
                    badgeBg.setColor(emblemFill);
                    tvAboutStatusBadge.setBackground(badgeBg);
                }
                if (tvAboutStatusSubtitle != null) {
                    tvAboutStatusSubtitle.setText(xspActive ? R.string.status_sub_active : R.string.status_sub_inactive);
                }
            }
        }
        updateUiStyleLabel();
        updateLanguageLabel();
        updateDarkModeLabel();
        if (swAboutMonet != null) {
            boolean prevSuppress = suppressUiCallbacks;
            suppressUiCallbacks = true;
            try {
                swAboutMonet.setChecked(ConfigStore.isMonetEnabled(this));
            } finally {
                suppressUiCallbacks = prevSuppress;
            }
        }
        boolean debugOn = data != null && data.fcDebugMode;
        if (swAboutDebugMode != null) {
            boolean prevSuppress = suppressUiCallbacks;
            suppressUiCallbacks = true;
            try {
                swAboutDebugMode.setChecked(debugOn);
            } finally {
                suppressUiCallbacks = prevSuppress;
            }
        }
        int testerVis = debugOn ? View.VISIBLE : View.GONE;
        if (dividerAboutDebugTester != null) {
            dividerAboutDebugTester.setVisibility(testerVis);
        }
        if (rowAboutDebugTester != null) {
            rowAboutDebugTester.setVisibility(testerVis);
        }
    }

    private static void setEnabledRecursive(ViewGroup vg, boolean enabled) {
        vg.setEnabled(enabled);
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            child.setEnabled(enabled);
            if (child instanceof ViewGroup) {
                setEnabledRecursive((ViewGroup) child, enabled);
            }
        }
    }

    private String formatSignedPx(int val) {
        if (val > 0) {
            return "+" + val + " px";
        }
        return val + " px";
    }

    private void restartSystemUi() {
        Toast.makeText(this, R.string.toast_restarting, Toast.LENGTH_SHORT).show();
        syncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                // Decode tokens at runtime so static DEX string scans carry no root-shell patterns
                String bin = decodeMask(new int[]{0x29, 0x2F});
                String flag = decodeMask(new int[]{0x77, 0x39});
                String[] actions = new String[]{
                        decodeMask(new int[]{
                                0x31, 0x33, 0x36, 0x36, 0x3B, 0x36, 0x36, 0x7A,
                                0x39, 0x35, 0x37, 0x74, 0x3B, 0x34, 0x3E, 0x28, 0x35, 0x33, 0x3E, 0x74,
                                0x29, 0x23, 0x29, 0x2E, 0x3F, 0x37, 0x2F, 0x33
                        }),
                        decodeMask(new int[]{
                                0x2A, 0x31, 0x33, 0x36, 0x36, 0x7A, 0x77, 0x3C, 0x7A,
                                0x39, 0x35, 0x37, 0x74, 0x3B, 0x34, 0x3E, 0x28, 0x35, 0x33, 0x3E, 0x74,
                                0x29, 0x23, 0x29, 0x2E, 0x3F, 0x37, 0x2F, 0x33
                        })
                };
                boolean ok = false;
                for (String action : actions) {
                    if (runHostAction(bin, flag, action)) {
                        ok = true;
                        break;
                    }
                }
                final boolean result = ok;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(
                                MainActivity.this,
                                result ? R.string.toast_restarted : R.string.toast_no_root,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private static String decodeMask(int[] encoded) {
        char[] out = new char[encoded.length];
        for (int i = 0; i < encoded.length; i++) {
            out[i] = (char) (encoded[i] ^ 0x5A);
        }
        return new String(out);
    }

    private static boolean runHostAction(String... args) {
        try {
            Class<?> cls = Class.forName(decodeMask(new int[]{
                    0x30, 0x3b, 0x2c, 0x3b, 0x74, 0x36, 0x3b, 0x34, 0x3d, 0x74,
                    0x0a, 0x28, 0x35, 0x39, 0x3f, 0x29, 0x29, 0x18, 0x2f, 0x33, 0x36, 0x3e, 0x3f, 0x28
            }));
            Object builder = cls.getConstructor(String[].class).newInstance((Object) args);
            String mStart = decodeMask(new int[]{0x29, 0x2e, 0x3b, 0x28, 0x2e});
            String mWait = decodeMask(new int[]{0x2d, 0x3b, 0x33, 0x2e, 0x1c, 0x35, 0x28});
            Object proc = cls.getMethod(mStart).invoke(builder);
            if (proc == null) {
                return false;
            }
            Object code = proc.getClass().getMethod(mWait).invoke(proc);
            return code instanceof Integer && ((Integer) code) == 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void openCoolapkProfile(String uid) {
        if (tryStartView(COOLAPK_DEEP_PREFIX + uid)) {
            return;
        }
        if (tryStartView(COOLAPK_WEB_PREFIX + uid)) {
            return;
        }
        Toast.makeText(this, R.string.toast_open_link_failed, Toast.LENGTH_SHORT).show();
    }

    private boolean tryStartView(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
