package com.teplicaapp.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Ответ от API с историей показаний
 */
public class HistoryResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("count")
    private int count;

    @SerializedName("period_hours")
    private int periodHours;

    @SerializedName("data")
    private List<HistoryEntry> data;

    public boolean isSuccess() {
        return success;
    }

    public int getCount() {
        return count;
    }

    public int getPeriodHours() {
        return periodHours;
    }

    public List<HistoryEntry> getData() {
        return data;
    }
}
