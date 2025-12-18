package com.teplicaapp.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.teplicaapp.data.local.AppPreferences;

/**
 * ViewModel для экрана настроек.
 */
public class SettingsViewModel extends AndroidViewModel {

    private final AppPreferences preferences;

    private final MutableLiveData<Float> tempMin;
    private final MutableLiveData<Float> tempMax;
    private final MutableLiveData<Float> humidityMin;
    private final MutableLiveData<Float> humidityMax;
    private final MutableLiveData<Long> refreshInterval;
    private final MutableLiveData<Boolean> notificationsEnabled;
    private final MutableLiveData<Boolean> autoRefreshEnabled;

    public SettingsViewModel(@NonNull Application application) {
        super(application);

        preferences = AppPreferences.getInstance(application);

        tempMin = new MutableLiveData<>(preferences.getTempMin());
        tempMax = new MutableLiveData<>(preferences.getTempMax());
        humidityMin = new MutableLiveData<>(preferences.getHumidityMin());
        humidityMax = new MutableLiveData<>(preferences.getHumidityMax());
        refreshInterval = new MutableLiveData<>(preferences.getRefreshInterval());
        notificationsEnabled = new MutableLiveData<>(preferences.isNotificationsEnabled());
        autoRefreshEnabled = new MutableLiveData<>(preferences.isAutoRefreshEnabled());
    }

    public LiveData<Float> getTempMin() {
        return tempMin;
    }

    public LiveData<Float> getTempMax() {
        return tempMax;
    }

    public LiveData<Float> getHumidityMin() {
        return humidityMin;
    }

    public LiveData<Float> getHumidityMax() {
        return humidityMax;
    }

    public LiveData<Long> getRefreshInterval() {
        return refreshInterval;
    }

    public LiveData<Boolean> getNotificationsEnabled() {
        return notificationsEnabled;
    }

    public LiveData<Boolean> getAutoRefreshEnabled() {
        return autoRefreshEnabled;
    }

    public void setTempMin(float value) {
        tempMin.setValue(value);
        preferences.setTempMin(value);
    }

    public void setTempMax(float value) {
        tempMax.setValue(value);
        preferences.setTempMax(value);
    }

    public void setHumidityMin(float value) {
        humidityMin.setValue(value);
        preferences.setHumidityMin(value);
    }

    public void setHumidityMax(float value) {
        humidityMax.setValue(value);
        preferences.setHumidityMax(value);
    }

    public void setRefreshInterval(long intervalMillis) {
        refreshInterval.setValue(intervalMillis);
        preferences.setRefreshInterval(intervalMillis);
    }

    public void setNotificationsEnabled(boolean enabled) {
        notificationsEnabled.setValue(enabled);
        preferences.setNotificationsEnabled(enabled);
    }

    public void setAutoRefreshEnabled(boolean enabled) {
        autoRefreshEnabled.setValue(enabled);
        preferences.setAutoRefreshEnabled(enabled);
    }

    public int getCurrentIntervalIndex() {
        return preferences.getCurrentIntervalIndex();
    }
}
