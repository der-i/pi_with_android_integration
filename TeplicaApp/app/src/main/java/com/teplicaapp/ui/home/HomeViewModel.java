package com.teplicaapp.ui.home;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.teplicaapp.data.model.ConnectionStatus;
import com.teplicaapp.data.model.Resource;
import com.teplicaapp.data.model.SensorData;
import com.teplicaapp.data.repository.SensorRepository;

/**
 * ViewModel для главного экрана с показаниями датчика.
 */
public class HomeViewModel extends ViewModel {
    
    private static final long DEFAULT_REFRESH_INTERVAL = 5000; // 5 секунд
    
    private final SensorRepository repository;
    private final MediatorLiveData<Resource<SensorData>> sensorData;
    private final MutableLiveData<ConnectionStatus> connectionStatus;
    private final MutableLiveData<Boolean> isAutoRefreshEnabled;
    
    private final Handler refreshHandler;
    private Runnable refreshRunnable;
    private long refreshInterval = DEFAULT_REFRESH_INTERVAL;
    
    public HomeViewModel() {
        repository = SensorRepository.getInstance();
        sensorData = new MediatorLiveData<>();
        connectionStatus = new MutableLiveData<>(ConnectionStatus.DISCONNECTED);
        isAutoRefreshEnabled = new MutableLiveData<>(false);
        refreshHandler = new Handler(Looper.getMainLooper());
        
        setupRefreshRunnable();
    }
    
    private void setupRefreshRunnable() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (Boolean.TRUE.equals(isAutoRefreshEnabled.getValue())) {
                    refreshData();
                    refreshHandler.postDelayed(this, refreshInterval);
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
     * Запросить свежие данные с сервера
     */
    public void refreshData() {
        connectionStatus.setValue(ConnectionStatus.CONNECTING);
        
        LiveData<Resource<SensorData>> source = repository.fetchSensorData();
        sensorData.addSource(source, resource -> {
            sensorData.setValue(resource);
            sensorData.removeSource(source);
            
            // Обновляем статус соединения
            if (resource.isSuccess()) {
                connectionStatus.setValue(ConnectionStatus.CONNECTED);
            } else if (resource.isError()) {
                connectionStatus.setValue(ConnectionStatus.ERROR);
            }
        });
    }
    
    /**
     * Включить автообновление данных
     */
    public void startAutoRefresh() {
        isAutoRefreshEnabled.setValue(true);
        refreshHandler.post(refreshRunnable);
    }
    
    /**
     * Остановить автообновление
     */
    public void stopAutoRefresh() {
        isAutoRefreshEnabled.setValue(false);
        refreshHandler.removeCallbacks(refreshRunnable);
    }
    
    /**
     * Установить интервал автообновления
     */
    public void setRefreshInterval(long intervalMillis) {
        this.refreshInterval = intervalMillis;
    }
    
    /**
     * Получить текущий интервал обновления
     */
    public long getRefreshInterval() {
        return refreshInterval;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        stopAutoRefresh();
    }
}