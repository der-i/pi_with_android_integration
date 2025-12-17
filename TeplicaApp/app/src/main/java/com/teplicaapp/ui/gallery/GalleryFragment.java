package com.teplicaapp.ui.gallery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.teplicaapp.R;
import com.teplicaapp.databinding.FragmentGalleryBinding;

/**
 * Фрагмент для отображения истории показаний датчика
 */
public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private GalleryViewModel viewModel;
    private HistoryAdapter adapter;

    private static final String[] PERIOD_OPTIONS = {
            "Последние 3 часа",
            "Последние 6 часов",
            "Последние 12 часов",
            "Последние 24 часа",
            "Последние 3 дня",
            "Последняя неделя"
    };

    private static final int[] PERIOD_HOURS = {3, 6, 12, 24, 72, 168};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(GalleryViewModel.class);
        binding = FragmentGalleryBinding.inflate(inflater, container, false);

        setupRecyclerView();
        setupSpinner();
        setupSwipeRefresh();
        observeViewModel();

        // Загрузить данные за последние 24 часа по умолчанию
        viewModel.loadHistory(24);

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                PERIOD_OPTIONS
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPeriod.setAdapter(spinnerAdapter);
        binding.spinnerPeriod.setSelection(3); // 24 часа по умолчанию

        binding.spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.loadHistory(PERIOD_HOURS[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
                R.color.purple_500,
                R.color.teal_200
        );
        binding.swipeRefresh.setOnRefreshListener(() -> {
            int selectedPosition = binding.spinnerPeriod.getSelectedItemPosition();
            viewModel.loadHistory(PERIOD_HOURS[selectedPosition]);
        });
    }

    private void observeViewModel() {
        viewModel.getHistoryData().observe(getViewLifecycleOwner(), historyList -> {
            adapter.setHistoryList(historyList);
            binding.textEmpty.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
            binding.recyclerView.setVisibility(historyList.isEmpty() ? View.GONE : View.VISIBLE);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
