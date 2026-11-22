package io.github.libxposed.service;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.Process;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * LibXposed Service API 102 ContentProvider receiving IXposedService binder from LSPosed daemon.
 */
public final class XposedProvider extends ContentProvider {
    private static final String METHOD_SEND_BINDER = "SendBinder";
    public static final String METHOD_EXPORT_PREFS = "ExportLocalPrefs";
    public static final String METHOD_IMPORT_PREFS = "ImportLocalPrefs";
    public static final String LEGACY_AUTHORITY_URI = "content://c17.fpoverlap.XposedService";
    private static final String PREFS_NAME = "config";
    private static final String KEY_BINDER = "binder";
    private static final String INTERFACE_TOKEN = "io.github.libxposed.service.IXposedService";
    private static final int TX_REQUEST_REMOTE_PREFS = 0x15;
    private static final int TX_UPDATE_REMOTE_PREFS = 0x16;

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Object LOCK = new Object();

    private static volatile IBinder sBinder;
    private static volatile Runnable sConnectionListener;

    @Override
    public boolean onCreate() {
        return true;
    }

    private boolean isCallerAuthorized() {
        int callingUid = Binder.getCallingUid();
        if (callingUid == Process.myUid() || callingUid == Process.SHELL_UID || callingUid == 0) {
            return true;
        }
        Context ctx = getContext();
        if (ctx == null) {
            return false;
        }
        PackageManager pm = ctx.getPackageManager();
        if (pm == null) {
            return false;
        }
        if (pm.checkSignatures(Process.myUid(), callingUid) == PackageManager.SIGNATURE_MATCH) {
            return true;
        }
        String callingPkg = getCallingPackage();
        return callingPkg != null
                && pm.checkSignatures(ctx.getPackageName(), callingPkg) == PackageManager.SIGNATURE_MATCH;
    }

    /**
     * Copies supported preference types (Boolean, Integer, Long, Float, String) from a map
     * into the given SharedPreferences synchronously.
     */
    public static int writePrefsFromMap(SharedPreferences sp, Map<?, ?> src) {
        if (sp == null || src == null || src.isEmpty()) {
            return 0;
        }
        SharedPreferences.Editor ed = sp.edit();
        int count = 0;
        for (Map.Entry<?, ?> e : src.entrySet()) {
            if (!(e.getKey() instanceof String)) {
                continue;
            }
            String k = (String) e.getKey();
            Object v = e.getValue();
            if (v instanceof Boolean) {
                ed.putBoolean(k, (Boolean) v);
                count++;
            } else if (v instanceof Integer) {
                ed.putInt(k, (Integer) v);
                count++;
            } else if (v instanceof Long) {
                ed.putLong(k, (Long) v);
                count++;
            } else if (v instanceof Float) {
                ed.putFloat(k, (Float) v);
                count++;
            } else if (v instanceof String) {
                ed.putString(k, (String) v);
                count++;
            }
        }
        if (count > 0) {
            ed.commit();
        }
        return count;
    }

    private static String encodePrefsToBase64(Map<String, ?> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        try {
            int count = 0;
            for (Map.Entry<String, ?> e : map.entrySet()) {
                Object v = e.getValue();
                if (e.getKey() != null && (v instanceof Boolean || v instanceof Integer
                        || v instanceof Long || v instanceof Float || v instanceof String)) {
                    count++;
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(count);
            for (Map.Entry<String, ?> e : map.entrySet()) {
                String k = e.getKey();
                Object v = e.getValue();
                if (k == null) {
                    continue;
                }
                if (v instanceof Boolean) {
                    dos.writeByte('Z');
                    dos.writeUTF(k);
                    dos.writeBoolean((Boolean) v);
                } else if (v instanceof Integer) {
                    dos.writeByte('I');
                    dos.writeUTF(k);
                    dos.writeInt((Integer) v);
                } else if (v instanceof Long) {
                    dos.writeByte('J');
                    dos.writeUTF(k);
                    dos.writeLong((Long) v);
                } else if (v instanceof Float) {
                    dos.writeByte('F');
                    dos.writeUTF(k);
                    dos.writeFloat((Float) v);
                } else if (v instanceof String) {
                    dos.writeByte('S');
                    dos.writeUTF(k);
                    dos.writeUTF((String) v);
                }
            }
            dos.flush();
            return Base64.encodeToString(
                    baos.toByteArray(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static int decodeBase64ToPrefs(SharedPreferences sp, String b64) {
        if (sp == null || b64 == null || b64.isEmpty()) {
            return 0;
        }
        try {
            StringBuilder padded = new StringBuilder(b64.trim());
            while (padded.length() % 4 != 0) {
                padded.append('=');
            }
            byte[] data = Base64.decode(padded.toString(), Base64.URL_SAFE | Base64.NO_WRAP);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            int count = dis.readInt();
            SharedPreferences.Editor ed = sp.edit();
            int written = 0;
            for (int i = 0; i < count; i++) {
                byte type = dis.readByte();
                String key = dis.readUTF();
                if (type == 'Z') {
                    ed.putBoolean(key, dis.readBoolean());
                } else if (type == 'I') {
                    ed.putInt(key, dis.readInt());
                } else if (type == 'J') {
                    ed.putLong(key, dis.readLong());
                } else if (type == 'F') {
                    ed.putFloat(key, dis.readFloat());
                } else if (type == 'S') {
                    ed.putString(key, dis.readUTF());
                } else {
                    break;
                }
                written++;
            }
            ed.commit();
            return written;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (METHOD_EXPORT_PREFS.equals(method)) {
            if (!isCallerAuthorized() || getContext() == null) {
                return null;
            }
            SharedPreferences sp = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            HashMap<String, Object> copy = new HashMap<>(sp.getAll());
            Bundle out = new Bundle();
            out.putString("b64", encodePrefsToBase64(copy));
            out.putString("summary", copy.toString());
            out.putSerializable("map", copy);
            return out;
        }
        if (METHOD_IMPORT_PREFS.equals(method)) {
            if (!isCallerAuthorized() || getContext() == null) {
                return null;
            }
            SharedPreferences sp = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            if (arg != null && !arg.isEmpty()) {
                int count = decodeBase64ToPrefs(sp, arg);
                HashMap<String, Object> current = new HashMap<>(sp.getAll());
                Bundle out = new Bundle();
                out.putInt("importedCount", count);
                out.putString("summary", current.toString());
                return out;
            }
            Map<?, ?> srcMap = null;
            if (extras != null) {
                extras.setClassLoader(XposedProvider.class.getClassLoader());
                Object obj = extras.getSerializable("map");
                if (obj instanceof Map<?, ?>) {
                    srcMap = (Map<?, ?>) obj;
                }
            }
            if (srcMap == null) {
                try {
                    Bundle remote = getContext().getContentResolver().call(
                            Uri.parse(LEGACY_AUTHORITY_URI), METHOD_EXPORT_PREFS, null, null);
                    if (remote != null) {
                        remote.setClassLoader(XposedProvider.class.getClassLoader());
                        Object obj = remote.getSerializable("map");
                        if (obj instanceof Map<?, ?>) {
                            srcMap = (Map<?, ?>) obj;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            int count = writePrefsFromMap(sp, srcMap);
            HashMap<String, Object> current = new HashMap<>(sp.getAll());
            Bundle out = new Bundle();
            out.putInt("importedCount", count);
            out.putString("summary", current.toString());
            return out;
        }
        if (!METHOD_SEND_BINDER.equals(method) || extras == null) {
            return null;
        }
        final IBinder binder = extras.getBinder(KEY_BINDER);
        if (binder == null) {
            return new Bundle();
        }
        synchronized (LOCK) {
            sBinder = binder;
            try {
                binder.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        synchronized (LOCK) {
                            if (sBinder == binder) {
                                sBinder = null;
                            }
                        }
                        notifyListener();
                    }
                }, 0);
            } catch (Throwable ignored) {
            }
        }
        notifyListener();
        return new Bundle();
    }

    public static boolean isConnected() {
        IBinder b = sBinder;
        return b != null && b.isBinderAlive();
    }

    public static void setConnectionListener(Runnable listener) {
        sConnectionListener = listener;
        if (listener != null && isConnected()) {
            MAIN_HANDLER.post(listener);
        }
    }

    private static void notifyListener() {
        final Runnable listener = sConnectionListener;
        if (listener != null) {
            MAIN_HANDLER.post(listener);
        }
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    public static Map<String, Object> fetchRemotePrefs(String group) {
        IBinder binder = sBinder;
        if (binder == null || !binder.isBinderAlive() || group == null) {
            return null;
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(INTERFACE_TOKEN);
            data.writeString(group);
            binder.transact(TX_REQUEST_REMOTE_PREFS, data, reply, 0);
            reply.readException();
            if (reply.readInt() == 0) {
                return null;
            }
            Bundle bundle = Bundle.CREATOR.createFromParcel(reply);
            if (bundle == null) {
                return null;
            }
            bundle.setClassLoader(XposedProvider.class.getClassLoader());
            Object mapObj = bundle.getSerializable("map");
            if (mapObj instanceof Map<?, ?>) {
                return new HashMap<>((Map<String, Object>) mapObj);
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public static boolean pushRemotePrefs(String group, Map<String, Object> values) {
        IBinder binder = sBinder;
        if (binder == null || !binder.isBinderAlive() || group == null || values == null) {
            return false;
        }
        Bundle diff = new Bundle();
        diff.putBoolean("clear", false);
        diff.putSerializable("delete", new HashSet<String>());
        diff.putSerializable("put", new HashMap<String, Object>(values));

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(INTERFACE_TOKEN);
            data.writeString(group);
            data.writeInt(1);
            diff.writeToParcel(data, 0);
            binder.transact(TX_UPDATE_REMOTE_PREFS, data, reply, 0);
            reply.readException();
            return true;
        } catch (Throwable ignored) {
            return false;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
