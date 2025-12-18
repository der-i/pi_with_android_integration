package com.teplicaapp.ui.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.teplicaapp.R;
import com.teplicaapp.data.local.AppPreferences;
import com.teplicaapp.databinding.FragmentSettingsBinding;
import com.teplicaapp.service.NotificationService;
import com.teplicaapp.data.model.SensorData;

/**
 * Fragment для экрана настроек.
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupObservers();
        setupListeners();

        return root;
    }

    private void setupObservers() {
        // Temperature thresholds
        viewModel.getTempMin().observe(getViewLifecycleOwner(), tempMin -> {
            String current = binding.editTempMin.getText() != null ?
                    binding.editTempMin.getText().toString() : "";
            String newValue = String.valueOf(tempMin);
            if (!current.equals(newValue)) {
                binding.editTempMin.setText(newValue);
            }
        });

        viewModel.getTempMax().observe(getViewLifecycleOwner(), tempMax -> {
            String current = binding.editTempMax.getText() != null ?
                    binding.editTempMax.getText().toString() : "";
            String newValue = String.valueOf(tempMax);
            if (!current.equals(newValue)) {
                binding.editTempMax.setText(newValue);
            }
        });

        // Humidity thresholds
        viewModel.getHumidityMin().observe(getViewLifecycleOwner(), humidityMin -> {
            String current = binding.editHumidityMin.getText() != null ?
                    binding.editHumidityMin.getText().toString() : "";
            String newValue = String.valueOf(humidityMin);
            if (!current.equals(newValue)) {
                binding.editHumidityMin.setText(newValue);
            }
        });

        viewModel.getHumidityMax().observe(getViewLifecycleOwner(), humidityMax -> {
            String current = binding.editHumidityMax.getText() != null ?
                    binding.editHumidityMax.getText().toString() : "";
            String newValue = String.valueOf(humidityMax);
            if (!current.equals(newValue)) {
                binding.editHumidityMax.setText(newValue);
            }
        });

        // Refresh interval
        viewModel.getRefreshInterval().observe(getViewLifecycleOwner(), interval -> {
            String intervalText = formatInterval(interval);
            binding.btnRefreshInterval.setText(intervalText);
        });

        // Switches
        viewModel.getAutoRefreshEnabled().observe(getViewLifecycleOwner(), enabled -> {
            if (binding.switchAutoRefresh.isChecked() != enabled) {
                binding.switchAutoRefresh.setChecked(enabled);
            }
        });

        viewModel.getNotificationsEnabled().observe(getViewLifecycleOwner(), enabled -> {
            if (binding.switchNotifications.isChecked() != enabled) {
                binding.switchNotifications.setChecked(enabled);
            }
        });
    }

    private void setupListeners() {
        // Auto refresh switch
        binding.switchAutoRefresh.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setAutoRefreshEnabled(isChecked);
        });

        // Notifications switch
        binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setNotificationsEnabled(isChecked);
        });

        // Refresh interval button
        binding.btnRefreshInterval.setOnClickListener(v -> showIntervalDialog());

        // Save button
        binding.btnSave.setOnClickListener(v -> saveSettings());

        // Test notification button
        binding.btnTestNotification.setOnClickListener(v -> testNotification());
    }

    private void showIntervalDialog() {
        String[] intervalNames = AppPreferences.getIntervalNames();
        int currentIndex = viewModel.getCurrentIntervalIndex();

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_interval_title)
                .setSingleChoiceItems(intervalNames, currentIndex, (dialog, which) -> {
                    long[] intervals = AppPreferences.getAvailableIntervals();
                    viewModel.setRefreshInterval(intervals[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void saveSettings() {
        try {
            // Parse temperature values
            float tempMin = Float.parseFloat(binding.editTempMin.getText().toString());
            float tempMax = Float.parseFloat(binding.editTempMax.getText().toString());

            // Parse humidity values
            float humidityMin = Float.parseFloat(binding.editHumidityMin.getText().toString());
            float humidityMax = Float.parseFloat(binding.editHumidityMax.getText().toString());

            // Validate ranges
            if (tempMin >= tempMax) {
                Toast.makeText(requireContext(),
                        R.string.settings_error_invalid_range,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (humidityMin >= humidityMax) {
                Toast.makeText(requireContext(),
                        R.string.settings_error_invalid_range,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Save values
            viewModel.setTempMin(tempMin);
            viewModel.setTempMax(tempMax);
            viewModel.setHumidityMin(humidityMin);
            viewModel.setHumidityMax(humidityMax);

            Toast.makeText(requireContext(),
                    R.string.settings_saved,
                    Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(),
                    "Введите корректные значения",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private String formatInterval(long intervalMillis) {
        long[] intervals = AppPreferences.getAvailableIntervals();
        String[] names = AppPreferences.getIntervalNames();

        for (int i = 0; i < intervals.length; i++) {
            if (intervals[i] == intervalMillis) {
                return names[i];
            }
        }

        return String.valueOf(intervalMillis / 1000) + " сек";
    }

    private void testNotification() {
        // Создаем тестовые данные с экстремальными значениями
        NotificationService notificationService = NotificationService.getInstance(requireContext());

        // Получаем текущие пороги
        float tempMin = viewModel.getTempMin().getValue();
        float tempMax = viewModel.getTempMax().getValue();

        // Создаем тестовые данные ниже минимума температуры
        SensorData testData = new SensorData(
                tempMin - 5.0f,  // Температура ниже минимума
                50.0f            // Нормальная влажность
        );

        Toast.makeText(requireContext(),
                "Тестирование уведомления с температурой " + testData.getTemperature() + "°C",
                Toast.LENGTH_SHORT).show();

        notificationService.checkSensorData(testData);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
