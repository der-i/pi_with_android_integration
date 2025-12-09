package com.teplicaapp.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.teplicaapp.data.model.ApiResponse;
import com.teplicaapp.data.model.Resource;
import com.teplicaapp.data.model.SensorData;
import com.teplicaapp.data.remote.ApiClient;
import com.teplicaapp.data.remote.SensorApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository для получения данных с датчика.
 * Единая точка доступа к данным для ViewModel.
 */
public class SensorRepository {
    
    private static SensorRepository instance;
    private final SensorApiService apiService;
    
    // Кэш последних данных
    private SensorData lastSensorData;
    private long lastFetchTime;
    
    private SensorRepository() {
        apiService = ApiClient.getInstance().getSensorApi();
    }
    
    public static synchronized SensorRepository getInstance() {
        if (instance == null) {
            instance = new SensorRepository();
        }
        return instance;
    }
    
    /**
     * Пересоздание репозитория после смены URL сервера
     */
    public static synchronized void resetInstance() {
        instance = null;
    }
    
    /**
     * Получение текущих показаний датчика.
     * Возвращает LiveData с состояниями: LOADING -> SUCCESS/ERROR
     */
    public LiveData<Resource<SensorData>> fetchSensorData() {
        MutableLiveData<Resource<SensorData>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(lastSensorData));
        
        apiService.getSensorData().enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, 
                                   @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    
                    if (apiResponse.isSuccess()) {
                        SensorData data = apiResponse.toSensorData();
                        lastSensorData = data;
                        lastFetchTime = System.currentTimeMillis();
                        result.setValue(Resource.success(data));
                    } else {
                        String error = apiResponse.getError() != null 
                                ? apiResponse.getError() 
                                : "Неизвестная ошибка сервера";
                        result.setValue(Resource.error(error, lastSensorData));
                    }
                } else {
                    result.setValue(Resource.error(
                            "Ошибка сервера: " + response.code(), 
                            lastSensorData));
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, 
                                  @NonNull Throwable t) {
                String message = parseErrorMessage(t);
                result.setValue(Resource.error(message, lastSensorData));
            }
        });
        
        return result;
    }
    
    /**
     * Получение кэшированных данных без запроса к серверу
     */
    public SensorData getCachedData() {
        return lastSensorData;
    }
    
    /**
     * Время последнего успешного запроса
     */
    public long getLastFetchTime() {
        return lastFetchTime;
    }
    
    /**
     * Проверка актуальности кэша
     */
    public boolean isCacheValid(long maxAgeMillis) {
        if (lastSensorData == null) {
            return false;
        }
        return System.currentTimeMillis() - lastFetchTime < maxAgeMillis;
    }
    
    private String parseErrorMessage(Throwable t) {
        if (t instanceof java.net.ConnectException) {
            return "Нет соединения с сервером";
        } else if (t instanceof java.net.SocketTimeoutException) {
            return "Превышено время ожидания";
        } else if (t instanceof java.net.UnknownHostException) {
            return "Сервер не найден";
        }
        return "Ошибка сети: " + t.getMessage();
    }
}