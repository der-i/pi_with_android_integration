package com.teplicaapp.data.model;

/**
 * Модель данных показаний датчика DHT22.
 * Соответствует JSON ответу от Flask API: {"temperature": float, "humidity": float}
 */
public class SensorData {
    
    private final float temperature;
    private final float humidity;
    private final long timestamp;
    
    public SensorData(float temperature, float humidity) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.timestamp = System.currentTimeMillis();
    }
    
    public SensorData(float temperature, float humidity, long timestamp) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.timestamp = timestamp;
    }
    
    public float getTemperature() {
        return temperature;
    }
    
    public float getHumidity() {
        return humidity;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Проверка валидности данных.
     * DHT22 диапазоны: температура -40..80°C, влажность 0..100%
     */
    public boolean isValid() {
        return temperature >= -40 && temperature <= 80 
            && humidity >= 0 && humidity <= 100;
    }
    
    /**
     * Форматированная температура для отображения
     */
    public String getFormattedTemperature() {
        return String.format("%.1f°C", temperature);
    }
    
    /**
     * Форматированная влажность для отображения
     */
    public String getFormattedHumidity() {
        return String.format("%.1f%%", humidity);
    }
    
    @Override
    public String toString() {
        return "SensorData{" +
                "temperature=" + temperature +
                ", humidity=" + humidity +
                ", timestamp=" + timestamp +
                '}';
    }
}