package com.teplicaapp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.teplicaapp.R;
import com.teplicaapp.data.model.ConnectionStatus;
import com.teplicaapp.data.model.Resource;
import com.teplicaapp.data.model.SensorData;
import com.teplicaapp.databinding.FragmentHomeBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Главный экран с отображением текущих показаний датчика.
 */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private SimpleDateFormat timeFormat;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        
        setupSwipeRefresh();
        observeViewModel();
        
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Первоначальная загрузка данных
        viewModel.refreshData();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.startAutoRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        viewModel.stopAutoRefresh();
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
                R.color.purple_500,
                R.color.teal_200
        );
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refreshData());
    }

    private void observeViewModel() {
        // Наблюдаем за данными датчика
        viewModel.getSensorData().observe(getViewLifecycleOwner(), this::handleSensorData);
        
        // Наблюдаем за статусом соединения
        viewModel.getConnectionStatus().observe(getViewLifecycleOwner(), this::updateConnectionStatus);
    }

    private void handleSensorData(Resource<SensorData> resource) {
        // Управление индикатором загрузки
        binding.swipeRefresh.setRefreshing(resource.isLoading());
        
        if (resource.isSuccess() && resource.getData() != null) {
            showData(resource.getData());
            hideError();
        } else if (resource.isError()) {
            showError(resource.getMessage());
            // Показываем старые данные если есть
            if (resource.getData() != null) {
                showData(resource.getData());
            }
        } else if (resource.isLoading() && resource.getData() != null) {
            // Показываем кэшированные данные во время загрузки
            showData(resource.getData());
        }
    }

    private void showData(SensorData data) {
        binding.textTemperature.setText(data.getFormattedTemperature());
        binding.textHumidity.setText(data.getFormattedHumidity());
        binding.textLastUpdate.setText(
                getString(R.string.last_update_format, timeFormat.format(new Date(data.getTimestamp())))
        );
        
        // Валидация данных
        if (!data.isValid()) {
            binding.textWarning.setVisibility(View.VISIBLE);
            binding.textWarning.setText(R.string.warning_invalid_data);
        } else {
            binding.textWarning.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        binding.textError.setVisibility(View.VISIBLE);
        binding.textError.setText(message);
    }

    private void hideError() {
        binding.textError.setVisibility(View.GONE);
    }

    private void updateConnectionStatus(ConnectionStatus status) {
        binding.textConnectionStatus.setText(status.getDisplayName());
        
        int colorRes;
        switch (status) {
            case CONNECTED:
                colorRes = R.color.status_connected;
                break;
            case CONNECTING:
                colorRes = R.color.status_connecting;
                break;
            case ERROR:
                colorRes = R.color.status_error;
                break;
            default:
                colorRes = R.color.status_disconnected;
        }
        binding.textConnectionStatus.setTextColor(getResources().getColor(colorRes, null));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}