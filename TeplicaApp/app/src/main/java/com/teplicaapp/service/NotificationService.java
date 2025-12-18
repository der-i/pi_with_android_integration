package com.teplicaapp.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.teplicaapp.MainActivity;
import com.teplicaapp.R;
import com.teplicaapp.data.local.AppPreferences;
import com.teplicaapp.data.model.SensorData;

/**
 * Сервис для отправки push-уведомлений при критических значениях.
 */
public class NotificationService {

    private static final String TAG = "NotificationService";
    private static final String CHANNEL_ID = "sensor_alerts";
    private static final int NOTIFICATION_ID_TEMPERATURE = 1;
    private static final int NOTIFICATION_ID_HUMIDITY = 2;

    private final Context context;
    private final NotificationManager notificationManager;
    private final AppPreferences preferences;

    // Флаги для предотвращения спама уведомлений
    private boolean lastTempNotificationWasHigh = false;
    private boolean lastHumidityNotificationWasHigh = false;
    private long lastTempNotificationTime = 0;
    private long lastHumidityNotificationTime = 0;
    private static final long NOTIFICATION_COOLDOWN_MS = 60000; // 1 минута

    private static NotificationService instance;

    private NotificationService(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.preferences = AppPreferences.getInstance(context);

        createNotificationChannel();
    }

    public static synchronized NotificationService getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationService(context);
        }
        return instance;
    }

    /**
     * Создание канала уведомлений (для Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_MAX  // Максимальный приоритет
            );
            channel.setDescription(context.getString(R.string.notification_channel_description));
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});

            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Проверка данных датчика и отправка уведомлений при необходимости
     */
    public void checkSensorData(SensorData data) {
        Log.d(TAG, "==================== checkSensorData called ====================");

        if (!preferences.isNotificationsEnabled()) {
            Log.w(TAG, "❌ Notifications DISABLED in settings!");
            return;
        }

        if (data == null) {
            Log.w(TAG, "❌ SensorData is NULL!");
            return;
        }

        float temp = data.getTemperature();
        float humidity = data.getHumidity();
        float tempMin = preferences.getTempMin();
        float tempMax = preferences.getTempMax();
        float humidityMin = preferences.getHumidityMin();
        float humidityMax = preferences.getHumidityMax();

        Log.i(TAG, String.format("📊 Current: temp=%.1f°C, humidity=%.1f%%", temp, humidity));
        Log.i(TAG, String.format("📏 Thresholds: temp(%.1f-%.1f°C), humidity(%.1f-%.1f%%)",
                tempMin, tempMax, humidityMin, humidityMax));

        boolean tempOutOfRange = temp < tempMin || temp > tempMax;
        boolean humidityOutOfRange = humidity < humidityMin || humidity > humidityMax;

        if (tempOutOfRange) {
            Log.w(TAG, "⚠️ Temperature OUT OF RANGE!");
        }
        if (humidityOutOfRange) {
            Log.w(TAG, "⚠️ Humidity OUT OF RANGE!");
        }

        checkTemperature(temp);
        checkHumidity(humidity);

        Log.d(TAG, "==================== checkSensorData finished ====================");
    }

    /**
     * Проверка температуры
     */
    private void checkTemperature(float temperature) {
        float min = preferences.getTempMin();
        float max = preferences.getTempMax();

        boolean isLow = temperature < min;
        boolean isHigh = temperature > max;
        long now = System.currentTimeMillis();

        // Проверяем, нужно ли отправлять уведомление
        boolean shouldNotify = false;
        boolean currentIsHigh = false;

        if (isLow) {
            // Уведомление о низкой температуре
            // Отправляем если: изменился тип (был high, стал low) ИЛИ прошло достаточно времени
            shouldNotify = lastTempNotificationWasHigh ||
                    lastTempNotificationTime == 0 ||
                    (now - lastTempNotificationTime) > NOTIFICATION_COOLDOWN_MS;
            currentIsHigh = false;
        } else if (isHigh) {
            // Уведомление о высокой температуре
            // Отправляем если: изменился тип (был low, стал high) ИЛИ прошло достаточно времени
            shouldNotify = !lastTempNotificationWasHigh ||
                    lastTempNotificationTime == 0 ||
                    (now - lastTempNotificationTime) > NOTIFICATION_COOLDOWN_MS;
            currentIsHigh = true;
        } else {
            // Температура в норме, сброс флагов
            lastTempNotificationTime = 0;
            return;
        }

        if (shouldNotify) {
            String message = isLow ?
                    context.getString(R.string.notification_temp_low, temperature, min, max) :
                    context.getString(R.string.notification_temp_high, temperature, min, max);

            Log.i(TAG, "Sending temperature notification: " + message);

            sendNotification(
                    NOTIFICATION_ID_TEMPERATURE,
                    context.getString(R.string.notification_title_temperature),
                    message
            );

            lastTempNotificationWasHigh = currentIsHigh;
            lastTempNotificationTime = now;
        } else {
            Log.d(TAG, "Temperature notification skipped (cooldown or same state)");
        }
    }

    /**
     * Проверка влажности
     */
    private void checkHumidity(float humidity) {
        float min = preferences.getHumidityMin();
        float max = preferences.getHumidityMax();

        boolean isLow = humidity < min;
        boolean isHigh = humidity > max;
        long now = System.currentTimeMillis();

        // Проверяем, нужно ли отправлять уведомление
        boolean shouldNotify = false;
        boolean currentIsHigh = false;

        if (isLow) {
            // Уведомление о низкой влажности
            // Отправляем если: изменился тип (был high, стал low) ИЛИ прошло достаточно времени
            shouldNotify = lastHumidityNotificationWasHigh ||
                    lastHumidityNotificationTime == 0 ||
                    (now - lastHumidityNotificationTime) > NOTIFICATION_COOLDOWN_MS;
            currentIsHigh = false;
        } else if (isHigh) {
            // Уведомление о высокой влажности
            // Отправляем если: изменился тип (был low, стал high) ИЛИ прошло достаточно времени
            shouldNotify = !lastHumidityNotificationWasHigh ||
                    lastHumidityNotificationTime == 0 ||
                    (now - lastHumidityNotificationTime) > NOTIFICATION_COOLDOWN_MS;
            currentIsHigh = true;
        } else {
            // Влажность в норме, сброс флагов
            lastHumidityNotificationTime = 0;
            return;
        }

        if (shouldNotify) {
            String message = isLow ?
                    context.getString(R.string.notification_humidity_low, humidity, min, max) :
                    context.getString(R.string.notification_humidity_high, humidity, min, max);

            Log.i(TAG, "Sending humidity notification: " + message);

            sendNotification(
                    NOTIFICATION_ID_HUMIDITY,
                    context.getString(R.string.notification_title_humidity),
                    message
            );

            lastHumidityNotificationWasHigh = currentIsHigh;
            lastHumidityNotificationTime = now;
        } else {
            Log.d(TAG, "Humidity notification skipped (cooldown or same state)");
        }
    }

    /**
     * Отправка уведомления
     */
    private void sendNotification(int notificationId, String title, String message) {
        Log.d(TAG, "sendNotification: id=" + notificationId + ", title=" + title);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 200, 500});

        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Notification sent successfully");
    }

    /**
     * Отмена всех уведомлений
     */
    public void cancelAllNotifications() {
        notificationManager.cancelAll();
    }
}
