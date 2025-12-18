package com.teplicaapp.ui.home;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.teplicaapp.data.local.AppPreferences;
import com.teplicaapp.data.model.ConnectionStatus;
import com.teplicaapp.data.model.Resource;
import com.teplicaapp.data.model.SensorData;
import com.teplicaapp.data.repository.SensorRepository;
import com.teplicaapp.service.NotificationService;
import com.teplicaapp.util.NetworkUtils;

/**
 * ViewModel для главного экрана с показаниями датчика.
 */
public class HomeViewModel extends AndroidViewModel {

    private static final String TAG = "HomeViewModel";

    private final SensorRepository repository;
    private final AppPreferences preferences;
    private final NotificationService notificationService;
    private final MediatorLiveData<Resource<SensorData>> sensorData;
    private final MutableLiveData<ConnectionStatus> connectionStatus;
    private final MutableLiveData<Boolean> isAutoRefreshEnabled;
    private final MutableLiveData<Long> refreshInterval;

    private final Handler refreshHandler;
    private Runnable refreshRunnable;
    
    public HomeViewModel(@NonNull Application application) {
        super(application);

        repository = SensorRepository.getInstance();
        preferences = AppPreferences.getInstance(application);
        notificationService = NotificationService.getInstance(application);

        sensorData = new MediatorLiveData<>();
        connectionStatus = new MutableLiveData<>(ConnectionStatus.DISCONNECTED);
        isAutoRefreshEnabled = new MutableLiveData<>(preferences.isAutoRefreshEnabled());
        refreshInterval = new MutableLiveData<>(preferences.getRefreshInterval());

        refreshHandler = new Handler(Looper.getMainLooper());

        setupRefreshRunnable();
    }
    
    private void setupRefreshRunnable() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (Boolean.TRUE.equals(isAutoRefreshEnabled.getValue())) {
                    refreshData();
                    Long interval = refreshInterval.getValue();
                    if (interval != null) {
                        refreshHandler.postDelayed(this, interval);
                    }
                }
            }
        };
    }
    
    /**
     * Получить LiveData с данными датчика
     */
    public LiveData<Resource<SensorData>> getSensorData() {
        return sensorData;
    }
    
    /**
     * Получить LiveData со статусом соединения
     */
    public LiveData<ConnectionStatus> getConnectionStatus() {
        return connectionStatus;
    }
    
    /**
     * Получить LiveData с состоянием автообновления
     */
    public LiveData<Boolean> getIsAutoRefreshEnabled() {
        return isAutoRefreshEnabled;
    }
    
    /**
     * Получить LiveData с интервалом обновления
     */
    public LiveData<Long> getRefreshInterval() {
        return refreshInterval;
    }
    
    /**
     * Запросить свежие данные с сервера
     */
    public void refreshData() {
        // Проверяем наличие интернет-соединения
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            sensorData.setValue(Resource.error(
                    "Нет подключения к интернету",
                    repository.getCachedData()));
            connectionStatus.setValue(ConnectionStatus.DISCONNECTED);
            return;
        }

        connectionStatus.setValue(ConnectionStatus.CONNECTING);

        LiveData<Resource<SensorData>> source = repository.fetchSensorData();
        sensorData.addSource(source, resource -> {
            sensorData.setValue(resource);
            sensorData.removeSource(source);

            if (resource.isSuccess()) {
                connectionStatus.setValue(ConnectionStatus.CONNECTED);
                // Проверка критических значений для уведомлений
                if (resource.getData() != null) {
                    SensorData data = resource.getData();
                    Log.d(TAG, "Checking sensor data for notifications: temp=" +
                            data.getTemperature() + ", humidity=" + data.getHumidity());
                    notificationService.checkSensorData(data);
                } else {
                    Log.w(TAG, "Sensor data is null, cannot check notifications");
                }
            } else if (resource.isError()) {
                connectionStatus.setValue(ConnectionStatus.ERROR);
                Log.e(TAG, "Error fetching sensor data: " + resource.getMessage());
            }
        });
    }
    
    /**
     * Включить автообновление данных
     */
    public void startAutoRefresh() {
        if (Boolean.TRUE.equals(isAutoRefreshEnabled.getValue())) {
            refreshHandler.post(refreshRunnable);
        }
    }
    
    /**
     * Остановить автообновление
     */
    public void stopAutoRefresh() {
        refreshHandler.removeCallbacks(refreshRunnable);
    }
    
    /**
     * Переключить автообновление
     */
    public void toggleAutoRefresh() {
        boolean newState = !Boolean.TRUE.equals(isAutoRefreshEnabled.getValue());
        isAutoRefreshEnabled.setValue(newState);
        preferences.setAutoRefreshEnabled(newState);
        
        if (newState) {
            refreshHandler.post(refreshRunnable);
        } else {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }
    
    /**
     * Установить интервал автообновления
     */
    public void setRefreshInterval(long intervalMillis) {
        refreshInterval.setValue(intervalMillis);
        preferences.setRefreshInterval(intervalMillis);
        
        // Перезапуск с новым интервалом
        if (Boolean.TRUE.equals(isAutoRefreshEnabled.getValue())) {
            stopAutoRefresh();
            startAutoRefresh();
        }
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        stopAutoRefresh();
    }
}