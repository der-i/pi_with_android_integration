package com.teplicaapp.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Модель ответа от Flask API.
 * Успешный ответ: {"temperature": float, "humidity": float}
 * Ошибка: {"error": "message"}
 */
public class ApiResponse {
    
    @SerializedName("temperature")
    private Float temperature;
    
    @SerializedName("humidity")
    private Float humidity;
    
    @SerializedName("error")
    private String error;
    
    public Float getTemperature() {
        return temperature;
    }
    
    public Float getHumidity() {
        return humidity;
    }
    
    public String getError() {
        return error;
    }
    
    public boolean isSuccess() {
        return error == null && temperature != null && humidity != null;
    }
    
    /**
     * Конвертация в SensorData при успешном ответе
     */
    public SensorData toSensorData() {
        if (!isSuccess()) {
            return null;
        }
        return new SensorData(temperature, humidity);
    }
}