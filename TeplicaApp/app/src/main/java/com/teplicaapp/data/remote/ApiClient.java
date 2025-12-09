package com.teplicaapp.data.remote;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit клиент для подключения к Raspberry Pi.
 * По умолчанию ищет сервер на 192.168.1.100:5000
 */
public class ApiClient {
    
    private static final String DEFAULT_BASE_URL = "http://192.168.1.100:5000/";
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int READ_TIMEOUT_SECONDS = 10;
    
    private static ApiClient instance;
    private final Retrofit retrofit;
    private String baseUrl;
    
    private ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();
        
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
    
    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient(DEFAULT_BASE_URL);
        }
        return instance;
    }
    
    /**
     * Пересоздание клиента с новым адресом сервера.
     * Вызывать из настроек при смене IP Raspberry Pi.
     */
    public static synchronized void updateBaseUrl(String newBaseUrl) {
        if (!newBaseUrl.endsWith("/")) {
            newBaseUrl = newBaseUrl + "/";
        }
        if (!newBaseUrl.startsWith("http://") && !newBaseUrl.startsWith("https://")) {
            newBaseUrl = "http://" + newBaseUrl;
        }
        instance = new ApiClient(newBaseUrl);
    }
    
    public SensorApiService getSensorApi() {
        return retrofit.create(SensorApiService.class);
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
}