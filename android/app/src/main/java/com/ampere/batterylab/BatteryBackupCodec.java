package com.ampere.batterylab;

import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.Set;

/** Version-neutral codec for the allow-listed local backup values. */
public final class BatteryBackupCodec {
    private BatteryBackupCodec() { }

    public static void encode(JSONObject target, SharedPreferences source, Set<String> allowedKeys) throws Exception {
        for (java.util.Map.Entry<String, ?> entry : source.getAll().entrySet()) {
            if (!allowedKeys.contains(entry.getKey())) continue;
            Object value = entry.getValue();
            JSONObject encoded = new JSONObject();
            if (value instanceof Boolean) { encoded.put("type", "boolean"); encoded.put("value", value); }
            else if (value instanceof Integer) { encoded.put("type", "int"); encoded.put("value", value); }
            else if (value instanceof Long) { encoded.put("type", "long"); encoded.put("value", value); }
            else if (value instanceof Float) { encoded.put("type", "float"); encoded.put("value", value); }
            else if (value instanceof String) { encoded.put("type", "string"); encoded.put("value", value); }
            else continue;
            target.put(entry.getKey(), encoded);
        }
    }

    public static void restore(JSONObject values, SharedPreferences.Editor editor, Set<String> allowedKeys) throws Exception {
        java.util.Iterator<String> keys = values.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!allowedKeys.contains(key)) continue;
            JSONObject encoded = values.getJSONObject(key);
            String type = encoded.optString("type");
            if ("boolean".equals(type)) editor.putBoolean(key, encoded.getBoolean("value"));
            else if ("int".equals(type)) editor.putInt(key, encoded.getInt("value"));
            else if ("long".equals(type)) editor.putLong(key, encoded.getLong("value"));
            else if ("float".equals(type)) editor.putFloat(key, (float) encoded.getDouble("value"));
            else if ("string".equals(type)) editor.putString(key, encoded.getString("value"));
        }
    }
}
