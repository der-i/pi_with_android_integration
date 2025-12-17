package com.teplicaapp.data.remote;

import com.teplicaapp.data.model.ApiResponse;
import com.teplicaapp.data.model.HistoryResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit интерфейс для Flask API на Raspberry Pi.
 */
public interface SensorApiService {

    @GET("/data")
    Call<ApiResponse> getSensorData();

    @GET("/history")
    Call<HistoryResponse> getHistory(@Query("hours") int hours, @Query("limit") Integer limit);

    @GET("/history/aggregated")
    Call<HistoryResponse> getAggregatedHistory(@Query("hours") int hours, @Query("interval") int intervalMinutes);
}