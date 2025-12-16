package com.teplicaapp.ui.home;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.teplicaapp.R;
import com.teplicaapp.data.local.AppPreferences;
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
        setupMenu();
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
    
    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_home, menu);
            }
            
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.action_toggle_auto_refresh) {
                    viewModel.toggleAutoRefresh();
                    return true;
                } else if (id == R.id.action_refresh_interval) {
                    showIntervalDialog();
                    return true;
                }
                return false;
            }
            
            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                MenuItem autoRefreshItem = menu.findItem(R.id.action_toggle_auto_refresh);
                if (autoRefreshItem != null) {
                    boolean enabled = Boolean.TRUE.equals(viewModel.getIsAutoRefreshEnabled().getValue());
                    autoRefreshItem.setTitle(enabled ? R.string.action_disable_auto_refresh : R.string.action_enable_auto_refresh);
                }
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }
    
    private void showIntervalDialog() {
        AppPreferences prefs = AppPreferences.getInstance(requireContext());
        int currentIndex = prefs.getCurrentIntervalIndex();
        
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_interval_title)
                .setSingleChoiceItems(
                        AppPreferences.getIntervalNames(),
                        currentIndex,
                        (dialog, which) -> {
                            long[] intervals = AppPreferences.getAvailableIntervals();
                            viewModel.setRefreshInterval(intervals[which]);
                            dialog.dismiss();
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
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
        // Принудительно останавливаем индикатор загрузки
        binding.swipeRefresh.setRefreshing(false);

        // Анимация обновления данных
        Animation pulseAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse);

        binding.textTemperature.setText(data.getFormattedTemperature());
        binding.textTemperature.startAnimation(pulseAnim);

        binding.textHumidity.setText(data.getFormattedHumidity());
        binding.textHumidity.startAnimation(pulseAnim);

        binding.textLastUpdate.setText(
                getString(R.string.last_update_format, timeFormat.format(new Date(data.getTimestamp())))
        );

        // Валидация данных
        if (!data.isValid()) {
            binding.textWarning.setVisibility(View.VISIBLE);
            binding.textWarning.setText(R.string.warning_invalid_data);
            Animation fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in);
            binding.textWarning.startAnimation(fadeIn);
        } else {
            binding.textWarning.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        binding.textError.setVisibility(View.VISIBLE);
        String fullMessage = message + "\n" + getString(R.string.hint_tap_to_retry);
        binding.textError.setText(fullMessage);

        // Анимация появления ошибки
        Animation fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in);
        binding.textError.startAnimation(fadeIn);

        // Добавляем возможность повторить запрос по клику на сообщение об ошибке
        binding.textError.setOnClickListener(v -> {
            viewModel.refreshData();
        });
    }

    private void hideError() {
        binding.textError.setVisibility(View.GONE);
        binding.textError.setOnClickListener(null);
    }

    private void updateConnectionStatus(ConnectionStatus status) {
        binding.textConnectionStatus.setText(status.getDisplayName().toUpperCase());

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