package com.teplicaapp.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.annotation.NonNull;

/**
 * Утилита для проверки состояния сетевого подключения.
 */
public class NetworkUtils {

    /**
     * Проверяет наличие активного интернет-соединения
     *
     * @param context Контекст приложения
     * @return true если есть подключение к интернету
     */
    public static boolean isNetworkAvailable(@NonNull Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
    }

    /**
     * Получить тип сетевого подключения (WiFi, Mobile, и т.д.)
     *
     * @param context Контекст приложения
     * @return Строка с типом подключения или "No Connection"
     */
    @NonNull
    public static String getNetworkType(@NonNull Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return "No Connection";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return "No Connection";
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                return "No Connection";
            }

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "WiFi";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "Mobile Data";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "Ethernet";
            }
        } else {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                int type = activeNetwork.getType();
                if (type == ConnectivityManager.TYPE_WIFI) {
                    return "WiFi";
                } else if (type == ConnectivityManager.TYPE_MOBILE) {
                    return "Mobile Data";
                } else if (type == ConnectivityManager.TYPE_ETHERNET) {
                    return "Ethernet";
                }
            }
        }

        return "No Connection";
    }
}
