"""
Конфигурация системы теплицы.
"""

class Config:
    # Режим работы датчика: 'real' или 'simulated'
    SENSOR_MODE = 'simulated'

    # Flask сервер
    HOST = '0.0.0.0'
    PORT = 5000
    DEBUG = True

    # GPIO пины для Raspberry Pi
    DHT_SENSOR_PIN = 4  # GPIO4 (физический пин 7)
    WINDOW_RELAY_PIN = 17  # GPIO17 (физический пин 11) - реле для управления окном

    # Настройки датчика DHT22
    DHT_SENSOR_TYPE = 'DHT22'  # или 'DHT11'

    # Пороговые значения по умолчанию
    DEFAULT_TEMP_MIN = 18.0  # Минимальная температура (°C)
    DEFAULT_TEMP_MAX = 28.0  # Максимальная температура (°C)
    DEFAULT_HUMIDITY_MIN = 40.0  # Минимальная влажность (%)
    DEFAULT_HUMIDITY_MAX = 70.0  # Максимальная влажность (%)

    # Параметры симуляции
    SIMULATION_UPDATE_INTERVAL = 1.0  # Интервал обновления симулированных данных (сек)

    # Параметры для реалистичной симуляции (суточный цикл)
    SIM_TEMP_BASE = 22.0  # Базовая температура (°C)
    SIM_TEMP_AMPLITUDE = 6.0  # Амплитуда колебаний температуры
    SIM_TEMP_NOISE = 0.5  # Уровень шума температуры

    SIM_HUMIDITY_BASE = 55.0  # Базовая влажность (%)
    SIM_HUMIDITY_AMPLITUDE = 15.0  # Амплитуда колебаний влажности
    SIM_HUMIDITY_NOISE = 2.0  # Уровень шума влажности

    # Логирование
    LOG_FILE = 'greenhouse.log'
    LOG_LEVEL = 'INFO'
