package com.teplicaapp.data.remote;

import com.teplicaapp.data.model.ApiResponse;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Retrofit интерфейс для Flask API на Raspberry Pi.
 * Endpoint: GET /data -> {"temperature": float, "humidity": float}
 */
public interface SensorApiService {
    
    @GET("/data")
    Call<ApiResponse> getSensorData();
}