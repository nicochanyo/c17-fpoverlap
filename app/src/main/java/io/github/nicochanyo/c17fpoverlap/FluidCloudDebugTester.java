package io.github.nicochanyo.c17fpoverlap;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Posts and manages genuine native ColorOS 17 Fluid Cloud test notifications
 * across all supported styles for debugging lockscreen, AOD, shade, and island hooks.
 */
public final class FluidCloudDebugTester {
    public static final String PERMISSION_POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";
    public static final String EXTRA_DEBUG_LIVE_ALERT = "c17.debug.live_alert";

    private static final String CH_LIVE_ALERT = "fc_debug_livealert";
    private static final String CH_NORMAL = "fc_debug_normal";

    public static final int ID_PICKUP_DARK = 9101;
    public static final int ID_PICKUP_WHITE = 9102;
    public static final int ID_PICKUP_BRAND = 9103;
    public static final int ID_PROGRESS = 9104;
    public static final int ID_RIDE = 9105;
    public static final int ID_TIMER = 9106;
    public static final int ID_BIG_TEXT = 9107;
    public static final int ID_METRIC = 9108;
    public static final int ID_CUSTOM = 9109;
    public static final int ID_NORMAL = 9110;

    private static final int[] PROGRESS_STEPS = new int[]{25, 55, 85, 100};
    private static final String[] BRAND_BG_COLORS = new String[]{
            "colorInt/FF00704A",
            "colorInt/FFFF8F00",
            "colorInt/FFD32F2F",
            "colorInt/FF1E88E5"
    };
    private static final String[] BRAND_NAMES = new String[]{
            "星巴克綠底",
            "琥珀飲品底",
            "品牌紅底",
            "品牌藍底"
    };

    private static int sBrandStep = 0;
    private static int sProgressStep = 0;

    private FluidCloudDebugTester() {
    }

    public static boolean hasPostPermission(Context ctx) {
        if (ctx == null || Build.VERSION.SDK_INT < 33) {
            return true;
        }
        return ctx.checkSelfPermission(PERMISSION_POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private static void ensureChannels(Context ctx) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        NotificationChannel chLive = new NotificationChannel(
                CH_LIVE_ALERT,
                "Fluid Cloud Debug (LiveAlert)",
                NotificationManager.IMPORTANCE_HIGH);
        chLive.setDescription("Debug test cards for ColorOS Fluid Cloud");
        chLive.setSound(null, null);
        chLive.enableVibration(false);
        chLive.enableLights(false);
        chLive.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(chLive);

        NotificationChannel chNorm = new NotificationChannel(
                CH_NORMAL,
                "Fluid Cloud Debug (Normal)",
                NotificationManager.IMPORTANCE_DEFAULT);
        chNorm.setDescription("Normal lockscreen notification comparison for AOD translucency");
        chNorm.setSound(null, null);
        chNorm.enableVibration(false);
        chNorm.enableLights(false);
        chNorm.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(chNorm);
    }

    private static PendingIntent createOpenAppPendingIntent(Context ctx, int reqCode) {
        Intent i = new Intent(ctx, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                ctx,
                reqCode,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static Notification.Builder baseBuilder(Context ctx, String channelId, int reqCode,
                                                    boolean asLiveAlert, boolean ongoing) {
        ensureChannels(ctx);
        PendingIntent pi = createOpenAppPendingIntent(ctx, reqCode);
        Notification.Builder b = new Notification.Builder(ctx, channelId)
                .setSmallIcon(R.drawable.ic_notif_cloud_debug)
                .setContentIntent(pi)
                .setOngoing(ongoing)
                .setOnlyAlertOnce(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis())
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(asLiveAlert ? Notification.CATEGORY_PROGRESS : Notification.CATEGORY_MESSAGE);

        Bundle extras = new Bundle();
        extras.putBoolean(EXTRA_DEBUG_LIVE_ALERT, asLiveAlert);
        if (asLiveAlert) {
            extras.putBoolean("android.requestPromotedOngoing", true);
            extras.putParcelable("ic_dbg", Icon.createWithResource(ctx, R.drawable.ic_notif_cloud_debug));
            extras.putParcelable("act_dbg", pi);
        }
        b.addExtras(extras);
        applyPromotedOngoingIfSupported(b, asLiveAlert);
        return b;
    }

    private static void applyPromotedOngoingIfSupported(Notification.Builder b, boolean promoted) {
        if (!promoted) {
            return;
        }
        try {
            Method m = b.getClass().getMethod("setRequestPromotedOngoing", boolean.class);
            m.invoke(b, true);
        } catch (Throwable ignored) {
        }
    }

    private static void applyShortCriticalText(Notification.Builder b, Bundle extras, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        extras.putString("android.shortCriticalText", text);
        try {
            Method m = b.getClass().getMethod("setShortCriticalText", String.class);
            m.invoke(b, text);
        } catch (Throwable ignored) {
        }
    }

    private static JSONObject makeLabelText(String text, boolean isTitle) throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("desc", "LabelText");
        if (isTitle) {
            obj.put("style", "TEXT");
            obj.put("type", "TEXT");
        }
        obj.put("text", text);
        return obj;
    }

    private static JSONObject makeIconObj(String size) throws Exception {
        JSONObject inner = new JSONObject();
        inner.put("icon", "icon/ic_dbg");
        inner.put("size", size);
        JSONObject wrapper = new JSONObject();
        wrapper.put("Icon", inner);
        return wrapper;
    }

    private static String buildStandardCardJson(String title, String labelLine, String btnText,
                                                String bgColorToken, String primaryColorToken) {
        try {
            JSONObject primary = new JSONObject();
            primary.put("type", "t_standard");
            primary.put("title", makeLabelText(title, true));

            JSONArray row = new JSONArray();
            row.put(makeLabelText(labelLine, false));
            JSONArray labels = new JSONArray();
            labels.put(row);
            primary.put("labels", labels);
            primary.put("icon", makeIconObj("SMALL"));

            JSONObject sub = new JSONObject();
            sub.put("type", "t_actions");
            JSONObject btn = new JSONObject();
            btn.put("desc", "LabelButton");
            btn.put("intent", "pi/act_dbg");
            btn.put("state", "ON");
            btn.put("style", "BUTTON");
            btn.put("text", btnText);
            btn.put("type", "TEXT_LABEL");
            btn.put("textColor", "colorInt/FFFFFFFF");
            JSONArray btns = new JSONArray();
            btns.put(btn);
            sub.put("btns", btns);

            JSONObject root = new JSONObject();
            root.put("primaryData", primary);
            root.put("subData", sub);
            if (bgColorToken != null || primaryColorToken != null) {
                JSONObject attr = new JSONObject();
                if (bgColorToken != null) {
                    attr.put("background", bgColorToken);
                }
                if (primaryColorToken != null) {
                    attr.put("primaryColor", primaryColorToken);
                }
                root.put("attr", attr);
            }
            return root.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String buildSymCapsuleJson(String capsuleText, String bgColorToken) {
        try {
            JSONObject cap = new JSONObject();
            cap.put("type", "t_sym");
            cap.put("firstIcon", makeIconObj("MINI"));
            JSONObject lastTitle = new JSONObject();
            lastTitle.put("text", capsuleText);
            cap.put("lastTitle", lastTitle);

            JSONObject root = new JSONObject();
            root.put("capsuleData", cap);
            if (bgColorToken != null) {
                JSONObject attr = new JSONObject();
                attr.put("background", bgColorToken);
                root.put("attr", attr);
            }
            return root.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void notifyNow(Context ctx, int id, Notification n) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null && n != null) {
            nm.notify(id, n);
        }
    }

    /** Pickup code card with default dark ColorOS Fluid Cloud scrim. */
    public static void postPickupDark(Context ctx) {
        Notification.Builder b = baseBuilder(ctx, CH_LIVE_ALERT, ID_PICKUP_DARK, true, true)
                .setContentTitle("取餐碼 A128")
                .setContentText("瑞幸咖啡 · 已製作完成，請前往櫃台取餐");
        Bundle extras = new Bundle();
        applyShortCriticalText(b, extras, "A128");
        String cardJson = buildStandardCardJson(
                "取餐碼 A128",
                "瑞幸咖啡 · 請前往櫃台取餐",
                "查看憑證",
                null,
                "colorInt/FF2196F3");
        String capJson = buildSymCapsuleJson("A128", null);
        if (cardJson != null) extras.putString("oplus.livealert.card", cardJson);
        if (capJson != null) extras.putString("oplus.livealert.capsule", capJson);
        b.addExtras(extras);
        notifyNow(ctx, ID_PICKUP_DARK, b.build());
    }

    /**
     * Pickup code card with pure white background (#FFFFFFFF), used specifically to test
     * and verify Fix #2 (preventing overly white background on Fluid Cloud cards).
     */
    public static void postPickupWhite(Context ctx) {
        Notification.Builder b = baseBuilder(ctx, CH_LIVE_ALERT, ID_PICKUP_WHITE, true, true)
                .setContentTitle("取餐碼 2095 · 珍珠奶茶（中杯）")
                .setContentText("白底 #FFFFFF 漸變測試 · 驗證白字可讀性");
        Bundle extras = new Bundle();
        applyShortCriticalText(b, extras, "2095");
        String cardJson = buildStandardCardJson(
                "珍珠奶茶（中杯）· 2095",
                "白底 #FFFFFF 漸變測試 · 驗證白字是否過白",
                "確認取餐",
                "colorInt/FFFFFFFF",
                "colorInt/FFFF6D00");
        String capJson = buildSymCapsuleJson("2095", "colorInt/FFFFFFFF");
        if (cardJson != null) extras.putString("oplus.livealert.card", cardJson);
        if (capJson != null) extras.putString("oplus.livealert.capsule", capJson);
        b.addExtras(extras);
        notifyNow(ctx, ID_PICKUP_WHITE, b.build());
    }

    /** Pickup code card cycling through bright brand background colors. */
    public static void postPickupBrand(Context ctx) {
        int idx = sBrandStep % BRAND_BG_COLORS.length;
        sBrandStep = (sBrandStep + 1) % BRAND_BG_COLORS.length;
        String bg = BRAND_BG_COLORS[idx];
        String label = BRAND_NAMES[idx];

        Notification.Builder b = baseBuilder(ctx, CH_LIVE_ALERT, ID_PICKUP_BRAND, true, true)
                .setContentTitle("取餐碼 C888")
                .setContentText(label + " · 再次點擊切換品牌色");
        Bundle extras = new Bundle();
        applyShortCriticalText(b, extras, "C888");
        String cardJson = buildStandardCardJson(
                "取餐碼 C888",
                label + " · 點擊測試再次換色",
                "完成",
                bg,
                bg);
        String capJson = buildSymCapsuleJson("C888", bg);
        if (cardJson != null) extras.putString("oplus.livealert.card", cardJson);
        if (capJson != null) extras.putString("oplus.livealert.capsule", capJson);
        b.addExtras(extras);
        notifyNow(ctx, ID_PICKUP_BRAND, b.build());
    }

    /** Food delivery progress card using Android 16+ Notification.ProgressStyle via reflection. */
    public static void postDeliveryProgress(Context ctx) {
        int pct = PROGRESS_STEPS[sProgressStep % PROGRESS_STEPS.length];
        sProgressStep = (sProgressStep + 1) % PROGRESS_STEPS.length;

        String status = pct >= 100 ? "已送達指定地點" : ("騎手距您 " + ((100 - pct) * 12) + "m");
        Notification.Builder b = baseBuilder(ctx, CH_LIVE_ALERT, ID_PROGRESS, true, true)
                .setContentTitle("外送配送中 · " + pct + "%")
                .setContentText(status + " · 點擊推進進度");

        boolean styled = tryApplyProgressStyle(b, pct);
        if (!styled) {
            b.setProgress(100, pct, false);
        }

        Bundle extras = new Bundle();
        applyShortCriticalText(b, extras, pct + "%");
        String capJson = buildSymCapsuleJson("送達 " + pct + "%", null);
        if (capJson != null) extras.putString("oplus.livealert.capsule", capJson);
        b.addExtras(extras);
        notifyNow(ctx, ID_PROGRESS, b.build());
    }

    private static boolean tryApplyProgressStyle(Notification.Builder b, int pct) {
        try {
            Class<?> psCls = Class.forName("android.app.Notification$ProgressStyle");
            Object ps = psCls.getConstructor().newInstance();
            psCls.getMethod("setProgress", int.class).invoke(ps, pct);
            psCls.getMethod("setStyledByProgress", boolean.class).invoke(ps, true);
            try {
                Class<?> segCls = Class.forName("android.app.Notification$ProgressStyle$Segment");
                Constructor<?> segCtor = segCls.getConstructor(int.class);
                Object seg = segCtor.newInstance(100);
                segCls.getMethod("setColor", int.class).invoke(seg, 0xFF4FC3F7);
                psCls.getMethod("addProgressSegment", segCls).invoke(ps, seg);
            } catch (Throwable ignored) {
            }
            if (ps instanceof Notification.Style) {
                b.setStyle((Notification.Style) ps);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** Ride-hailing card with license plate emphasis and action button. */
    public static void postRideHailing(Context ctx) {
        Notification.Builder b = baseBuilder(ctx, CH_LIVE_ALERT, ID_RIDE, true, true)
                .setContentTitle("京A·88888 · 距您 300m")
                .setContentText("白色新能源 · 預計 2 分鐘到達上車點");
        Bundle extras = new Bundle();
        applyShortCriticalText(b, extras, "2分鐘");
        String cardJson = buildStandardCardJson(
                "京A·88888 · 2 分鐘到達",
                "白色新能源 · 距上車點 300m",
                "聯繫司機",
                null,
                "colorInt/FF00BFA5");
        String capJson = buildSymCapsuleJson("2分鐘", null);
        if (cardJson != null) extras.putString("oplus.livealert.card", cardJson);
        if (capJson != null) extras.putString("oplus.livealert.capsule", capJson);
        b.addExtras(extras);
        notifyNow(ctx, ID_RIDE, b.build());
    }

    /** Countdown timer using native Chronometer countdown formatting. */
    public static void postTimerCountdown(Context ctx) {
        long targetMs = System.currentTimeMillis() + 600_000L;
        Notification.Builder b = baseBuilder(ctx, CH_LIVE_ALERT, ID_TIMER, true, true)
                .setContentTitle("專注倒數計時器")
                .setContentText("10 分鐘計時進行中")
                .setWhen(targetMs)
                .setShowWhen(true)
                .setUsesChronometer(true);
        try {
            b.setChronometerCountDown(true);
        } catch (Throwable ignored) {
        }
        Bundle extras = new Bundle();
        applyShortCriticalText(b, extras, "10:00");
        b.addExtras(extras);
        notifyNow(ctx, ID_TIMER, b.build());
    }

    /** Multi-line BigTextStyle Fluid Cloud notification. */
    public static void postBigText(Context ctx) {
        Notification.Builder b = baseBuilder(ctx, CH_LIVE_ALERT, ID_BIG_TEXT, true, true)
                .setContentTitle("航班登機提醒 · MU5101")
                .setContentText("請於 14:20 前前往 T2 航廈 B12 登機口")
                .setStyle(new Notification.BigTextStyle()
                        .bigText("預計 14:20 開始登機 · 座位 32A\nT2 航廈 B12 登機口 · 準點起飛"));
        Bundle extras = new Bundle();
        applyShortCriticalText(b, extras, "B12");
        b.addExtras(extras);
        notifyNow(ctx, ID_BIG_TEXT, b.build());
    }

    /** Metric / colorized notification style test. */
    public static void postMetricColorized(Context ctx) {
        Notification.Builder b = baseBuilder(ctx, CH_LIVE_ALERT, ID_METRIC, true, true)
                .setContentTitle("戶外跑步 · 3.2 公里")
                .setContentText("配速 5'12\" · 心率 148 bpm · 已用時 16:40")
                .setColorized(true)
                .setColor(0xFF1565C0);

        tryApplyMetricStyle(b);

        Bundle extras = new Bundle();
        applyShortCriticalText(b, extras, "3.2km");
        b.addExtras(extras);
        notifyNow(ctx, ID_METRIC, b.build());
    }

    private static void tryApplyMetricStyle(Notification.Builder b) {
        try {
            Class<?> msCls = Class.forName("android.app.Notification$MetricStyle");
            Class<?> metricCls = Class.forName("android.app.Notification$Metric");
            Class<?> fixedFloatCls = Class.forName("android.app.Notification$Metric$FixedFloat");
            Constructor<?> valCtor = fixedFloatCls.getConstructor(float.class);
            Object metricVal = valCtor.newInstance(3.2f);
            Class<?> metricValBase = Class.forName("android.app.Notification$Metric$MetricValue");
            Constructor<?> mCtor = metricCls.getConstructor(metricValBase, CharSequence.class);
            Object metric = mCtor.newInstance(metricVal, "km · 配速 5'12\"");
            Object ms = msCls.getConstructor().newInstance();
            msCls.getMethod("addMetric", metricCls).invoke(ms, metric);
            if (ms instanceof Notification.Style) {
                b.setStyle((Notification.Style) ms);
            }
        } catch (Throwable ignored) {
        }
    }

    /** Custom RemoteViews notification (CustomStyle in ColorOS Fluid Cloud). */
    public static void postCustomRemoteViews(Context ctx) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.notif_debug_custom);
        Notification.Builder b = baseBuilder(ctx, CH_LIVE_ALERT, ID_CUSTOM, true, true)
                .setContentTitle("CustomStyle 流體雲測試")
                .setContentText("RemoteViews 自訂版面測試")
                .setCustomContentView(rv);
        Bundle extras = new Bundle();
        applyShortCriticalText(b, extras, "自訂");
        b.addExtras(extras);
        notifyNow(ctx, ID_CUSTOM, b.build());
    }

    /** Regular non-Fluid-Cloud notification for testing AOD translucent glass contrast. */
    public static void postNormalNotification(Context ctx) {
        Notification.Builder b = baseBuilder(ctx, CH_NORMAL, ID_NORMAL, false, false)
                .setContentTitle("一般鎖屏通知（非流體雲）")
                .setContentText("用於對照全屏 AOD 一般通知毛玻璃透明與標準排版")
                .setColor(Color.TRANSPARENT);
        notifyNow(ctx, ID_NORMAL, b.build());
    }

    /** Posts a comprehensive representative set across all major Fluid Cloud types. */
    public static void postAllFluidClouds(Context ctx) {
        postPickupDark(ctx);
        postPickupWhite(ctx);
        postDeliveryProgress(ctx);
        postRideHailing(ctx);
        postTimerCountdown(ctx);
        postBigText(ctx);
        postCustomRemoteViews(ctx);
        postNormalNotification(ctx);
    }

    /** Cancels all debug notifications posted by this helper. */
    public static void clearAll(Context ctx) {
        if (ctx == null) {
            return;
        }
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        for (int id = ID_PICKUP_DARK; id <= ID_NORMAL; id++) {
            try {
                nm.cancel(id);
            } catch (Throwable ignored) {
            }
        }
    }
}
