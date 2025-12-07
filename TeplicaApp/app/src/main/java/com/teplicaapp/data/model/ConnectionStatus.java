package com.teplicaapp.data.model;

/**
 * Статус соединения с сервером Raspberry Pi
 */
public enum ConnectionStatus {
    
    CONNECTED("Подключено"),
    CONNECTING("Подключение..."),
    DISCONNECTED("Нет соединения"),
    ERROR("Ошибка соединения");
    
    private final String displayName;
    
    ConnectionStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}