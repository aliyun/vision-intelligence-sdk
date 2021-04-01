package com.aliyun.ai.viapi.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.Map;

public class PreferenceUtils {
    private static final String SP_NAME = "com.aliyun.ai.viapi.sp";

    public static void put(Context context, String key, Object object) {
        put(context, SP_NAME, key, object);
    }

    public static void put(Context context, String spName, String key, Object object) {
        SharedPreferences sp = context.getSharedPreferences(spName,
                Context.MODE_PRIVATE);
        Editor editor = sp.edit();

        if (null == object) {
            editor.putString(key, "");
        } else if (object instanceof String) {
            editor.putString(key, (String) object);
        } else if (object instanceof Integer) {
            editor.putInt(key, (Integer) object);
        } else if (object instanceof Boolean) {
            editor.putBoolean(key, (Boolean) object);
        } else if (object instanceof Float) {
            editor.putFloat(key, (Float) object);
        } else if (object instanceof Long) {
            editor.putLong(key, (Long) object);
        } else {
            editor.putString(key, object.toString());
        }
        editSubmit(editor);
    }

    private static void editSubmit(Editor editor) {
        editor.commit();
    }

    public static Object get(Context context, String key, Object defaultObject) {
        return get(context, SP_NAME, key, defaultObject);
    }

    public static Object get(Context context, String spName, String key, Object defaultObject) {
        SharedPreferences sp = context.getSharedPreferences(spName,
                Context.MODE_PRIVATE);

        if (defaultObject instanceof String) {
            return sp.getString(key, (String) defaultObject);
        } else if (defaultObject instanceof Integer) {
            return sp.getInt(key, (Integer) defaultObject);
        } else if (defaultObject instanceof Boolean) {
            return sp.getBoolean(key, (Boolean) defaultObject);
        } else if (defaultObject instanceof Float) {
            return sp.getFloat(key, (Float) defaultObject);
        } else if (defaultObject instanceof Long) {
            return sp.getLong(key, (Long) defaultObject);
        }
        return null;
    }

    public static void remove(Context context, String key) {
        remove(context, SP_NAME, key);
    }

    public static void remove(Context context, String spName, String key) {
        SharedPreferences sp = context.getSharedPreferences(spName,
                Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.remove(key);
        editSubmit(editor);
    }

    public static void clear(Context context) {
        clear(context, SP_NAME);
    }

    public static void clear(Context context, String spName) {
        SharedPreferences sp = context.getSharedPreferences(spName, Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.clear();
        editSubmit(editor);
    }

    public static boolean contains(Context context, String key) {
        return contains(context, SP_NAME, key);
    }

    public static boolean contains(Context context, String spName, String key) {
        SharedPreferences sp = context.getSharedPreferences(spName, Context.MODE_PRIVATE);
        return sp.contains(key);
    }

    public static Map<String, ?> getAll(Context context) {
        return getAll(context, SP_NAME);
    }

    public static Map<String, ?> getAll(Context context, String spName) {
        SharedPreferences sp = context.getSharedPreferences(spName,
                Context.MODE_PRIVATE);
        return sp.getAll();
    }
}
