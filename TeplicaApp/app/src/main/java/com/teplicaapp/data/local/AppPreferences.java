package com.teplicaapp.data.local;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Хранение настроек приложения в SharedPreferences.
 */
public class AppPreferences {
    
    private static final String PREFS_NAME = "teplica_prefs";
    
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_REFRESH_INTERVAL = "refresh_interval";
    private static final String KEY_AUTO_REFRESH_ENABLED = "auto_refresh_enabled";
    
    private static final String DEFAULT_SERVER_URL = "http://192.168.1.100:5000";
    private static final long DEFAULT_REFRESH_INTERVAL = 5000L; // 5 секунд
    
    private final SharedPreferences prefs;
    
    private static AppPreferences instance;
    
    private AppPreferences(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized AppPreferences getInstance(Context context) {
        if (instance == null) {
            instance = new AppPreferences(context);
        }
        return instance;
    }
    
    // Server URL
    
    public String getServerUrl() {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
    }
    
    public void setServerUrl(String url) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply();
    }
    
    // Refresh interval
    
    public long getRefreshInterval() {
        return prefs.getLong(KEY_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL);
    }
    
    public void setRefreshInterval(long intervalMillis) {
        prefs.edit().putLong(KEY_REFRESH_INTERVAL, intervalMillis).apply();
    }
    
    // Auto refresh
    
    public boolean isAutoRefreshEnabled() {
        return prefs.getBoolean(KEY_AUTO_REFRESH_ENABLED, true);
    }
    
    public void setAutoRefreshEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_REFRESH_ENABLED, enabled).apply();
    }
    
    /**
     * Предустановленные интервалы обновления (в миллисекундах)
     */
    public static long[] getAvailableIntervals() {
        return new long[] {
            2000L,   // 2 сек
            5000L,   // 5 сек
            10000L,  // 10 сек
            30000L,  // 30 сек
            60000L   // 1 мин
        };
    }
    
    /**
     * Названия интервалов для отображения
     */
    public static String[] getIntervalNames() {
        return new String[] {
            "2 секунды",
            "5 секунд",
            "10 секунд",
            "30 секунд",
            "1 минута"
        };
    }
    
    /**
     * Получить индекс текущего интервала в массиве
     */
    public int getCurrentIntervalIndex() {
        long current = getRefreshInterval();
        long[] intervals = getAvailableIntervals();
        for (int i = 0; i < intervals.length; i++) {
            if (intervals[i] == current) {
                return i;
            }
        }
        return 1; // default: 5 сек
    }
}