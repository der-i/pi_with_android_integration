package com.teplicaapp.ui.gallery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.teplicaapp.R;
import com.teplicaapp.data.model.HistoryEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для отображения истории показаний в RecyclerView
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<HistoryEntry> historyList = new ArrayList<>();

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryEntry entry = historyList.get(position);
        holder.bind(entry);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void setHistoryList(List<HistoryEntry> historyList) {
        this.historyList = historyList;
        notifyDataSetChanged();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView textTime;
        private final TextView textTemperature;
        private final TextView textHumidity;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textTime = itemView.findViewById(R.id.text_time);
            textTemperature = itemView.findViewById(R.id.text_temperature);
            textHumidity = itemView.findViewById(R.id.text_humidity);
        }

        public void bind(HistoryEntry entry) {
            textTime.setText(entry.getFormattedDateTime());
            textTemperature.setText(String.format("%.1f°C", entry.getTemperature()));
            textHumidity.setText(String.format("%.1f%%", entry.getHumidity()));
        }
    }
}
