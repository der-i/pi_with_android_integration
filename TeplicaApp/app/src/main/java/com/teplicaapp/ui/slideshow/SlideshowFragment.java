package com.teplicaapp.ui.slideshow;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.teplicaapp.R;
import com.teplicaapp.data.model.HistoryEntry;
import com.teplicaapp.databinding.FragmentSlideshowBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Фрагмент для отображения графиков температуры и влажности
 */
public class SlideshowFragment extends Fragment {

    private FragmentSlideshowBinding binding;
    private SlideshowViewModel viewModel;

    private static final String[] PERIOD_OPTIONS = {
            "Последние 12 часов",
            "Последние 24 часа",
            "Последние 3 дня",
            "Последняя неделя"
    };

    private static final int[] PERIOD_HOURS = {12, 24, 72, 168};
    private static final int[] INTERVAL_MINUTES = {15, 30, 60, 120}; // Интервалы агрегации

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(SlideshowViewModel.class);
        binding = FragmentSlideshowBinding.inflate(inflater, container, false);

        setupSpinner();
        setupCharts();
        setupSwipeRefresh();
        observeViewModel();

        // Загрузить данные за последние 24 часа по умолчанию
        viewModel.loadChartData(24, 30);

        return binding.getRoot();
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                PERIOD_OPTIONS
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPeriod.setAdapter(adapter);
        binding.spinnerPeriod.setSelection(1); // 24 часа по умолчанию

        binding.spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.loadChartData(PERIOD_HOURS[position], INTERVAL_MINUTES[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupCharts() {
        setupChart(binding.chartTemperature, "Температура (°C)", Color.parseColor("#FFE53935"));
        setupChart(binding.chartHumidity, "Влажность (%)", Color.parseColor("#FF1E88E5"));
    }

    private void setupChart(LineChart chart, String label, int color) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);

        // X Axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                return sdf.format(new Date((long) value * 1000));
            }
        });

        // Y Axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        chart.getAxisRight().setEnabled(false);

        chart.getLegend().setEnabled(false);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
                R.color.purple_500,
                R.color.teal_200
        );
        binding.swipeRefresh.setOnRefreshListener(() -> {
            int selectedPosition = binding.spinnerPeriod.getSelectedItemPosition();
            viewModel.loadChartData(PERIOD_HOURS[selectedPosition], INTERVAL_MINUTES[selectedPosition]);
        });
    }

    private void observeViewModel() {
        viewModel.getChartData().observe(getViewLifecycleOwner(), historyList -> {
            if (historyList != null && !historyList.isEmpty()) {
                updateCharts(historyList);
                binding.chartTemperature.setVisibility(View.VISIBLE);
                binding.chartHumidity.setVisibility(View.VISIBLE);
                binding.textEmpty.setVisibility(View.GONE);
            } else {
                binding.chartTemperature.setVisibility(View.GONE);
                binding.chartHumidity.setVisibility(View.GONE);
                binding.textEmpty.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.swipeRefresh.setRefreshing(isLoading);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCharts(List<HistoryEntry> historyList) {
        List<Entry> temperatureEntries = new ArrayList<>();
        List<Entry> humidityEntries = new ArrayList<>();

        for (HistoryEntry entry : historyList) {
            float timestamp = entry.getTimestamp();
            temperatureEntries.add(new Entry(timestamp, entry.getTemperature()));
            humidityEntries.add(new Entry(timestamp, entry.getHumidity()));
        }

        // График температуры
        LineDataSet temperatureDataSet = new LineDataSet(temperatureEntries, "Температура");
        temperatureDataSet.setColor(Color.parseColor("#FFE53935"));
        temperatureDataSet.setLineWidth(2f);
        temperatureDataSet.setDrawCircles(false);
        temperatureDataSet.setDrawValues(false);
        temperatureDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        temperatureDataSet.setCubicIntensity(0.2f);

        LineData temperatureData = new LineData(temperatureDataSet);
        binding.chartTemperature.setData(temperatureData);
        binding.chartTemperature.invalidate();

        // График влажности
        LineDataSet humidityDataSet = new LineDataSet(humidityEntries, "Влажность");
        humidityDataSet.setColor(Color.parseColor("#FF1E88E5"));
        humidityDataSet.setLineWidth(2f);
        humidityDataSet.setDrawCircles(false);
        humidityDataSet.setDrawValues(false);
        humidityDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        humidityDataSet.setCubicIntensity(0.2f);

        LineData humidityData = new LineData(humidityDataSet);
        binding.chartHumidity.setData(humidityData);
        binding.chartHumidity.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
