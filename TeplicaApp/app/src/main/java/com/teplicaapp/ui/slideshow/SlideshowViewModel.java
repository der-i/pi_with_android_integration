package com.teplicaapp.ui.slideshow;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.teplicaapp.data.model.HistoryEntry;
import com.teplicaapp.data.model.HistoryResponse;
import com.teplicaapp.data.remote.ApiClient;
import com.teplicaapp.data.remote.SensorApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SlideshowViewModel extends ViewModel {

    private final MutableLiveData<List<HistoryEntry>> chartData;
    private final MutableLiveData<Boolean> isLoading;
    private final MutableLiveData<String> errorMessage;
    private final SensorApiService apiService;

    public SlideshowViewModel() {
        chartData = new MutableLiveData<>(new ArrayList<>());
        isLoading = new MutableLiveData<>(false);
        errorMessage = new MutableLiveData<>();
        apiService = ApiClient.getInstance().getSensorApi();
    }

    public LiveData<List<HistoryEntry>> getChartData() {
        return chartData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Загрузить агрегированные данные для графиков
     * @param hours количество часов
     * @param intervalMinutes интервал агрегации в минутах
     */
    public void loadChartData(int hours, int intervalMinutes) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        apiService.getAggregatedHistory(hours, intervalMinutes)
                .enqueue(new Callback<HistoryResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<HistoryResponse> call,
                                           @NonNull Response<HistoryResponse> response) {
                        isLoading.setValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            HistoryResponse historyResponse = response.body();
                            if (historyResponse.isSuccess()) {
                                chartData.setValue(historyResponse.getData());
                            } else {
                                errorMessage.setValue("Ошибка получения данных");
                            }
                        } else {
                            errorMessage.setValue("Ошибка сервера: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<HistoryResponse> call, @NonNull Throwable t) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Ошибка сети: " + t.getMessage());
                    }
                });
    }
}
