"""
Модуль для чтения данных с датчика DHT22.
Поддерживает реальное чтение и симуляцию данных.
"""

import time
import math
import random
from datetime import datetime
from config import Config


class SensorReader:
    """Базовый класс для чтения данных с датчика"""

    def read_data(self):
        """Возвращает кортеж (temperature, humidity) или (None, None) при ошибке"""
        raise NotImplementedError()


class RealSensorReader(SensorReader):
    """Чтение данных с реального датчика DHT22"""

    def __init__(self):
        try:
            import board
            import adafruit_dht

            self.dht_device = adafruit_dht.DHT22(getattr(board, f'D{Config.DHT_SENSOR_PIN}'))
            print(f"[DHT22] Инициализирован на GPIO{Config.DHT_SENSOR_PIN}")
        except ImportError:
            print("[DHT22] ОШИБКА: Библиотека adafruit-circuitpython-dht не установлена")
            print("[DHT22] Установите: pip install adafruit-circuitpython-dht")
            raise
        except Exception as e:
            print(f"[DHT22] ОШИБКА инициализации: {e}")
            raise

    def read_data(self):
        """Читает данные с DHT22 датчика"""
        try:
            temperature = self.dht_device.temperature
            humidity = self.dht_device.humidity

            if temperature is not None and humidity is not None:
                # Проверка валидности данных
                if -40 <= temperature <= 80 and 0 <= humidity <= 100:
                    return round(temperature, 1), round(humidity, 1)
                else:
                    print(f"[DHT22] Некорректные данные: T={temperature}, H={humidity}")
                    return None, None
            else:
                print("[DHT22] Датчик вернул None")
                return None, None

        except RuntimeError as e:
            # DHT22 иногда дает RuntimeError при чтении
            print(f"[DHT22] Ошибка чтения: {e}")
            return None, None
        except Exception as e:
            print(f"[DHT22] Неожиданная ошибка: {e}")
            return None, None

    def __del__(self):
        """Очистка ресурсов"""
        if hasattr(self, 'dht_device'):
            self.dht_device.exit()


class SimulatedSensorReader(SensorReader):
    """
    Симуляция данных датчика с реалистичными паттернами.
    Имитирует суточный цикл температуры и влажности с шумом.
    """

    def __init__(self):
        self.start_time = time.time()
        print("[СИМУЛЯЦИЯ] Режим симуляции активирован")
        print(f"[СИМУЛЯЦИЯ] Базовая температура: {Config.SIM_TEMP_BASE}°C ± {Config.SIM_TEMP_AMPLITUDE}°C")
        print(f"[СИМУЛЯЦИЯ] Базовая влажность: {Config.SIM_HUMIDITY_BASE}% ± {Config.SIM_HUMIDITY_AMPLITUDE}%")

    def read_data(self):
        """
        Генерирует реалистичные данные на основе синусоидального паттерна.
        Симулирует суточный цикл: температура выше днем, влажность выше ночью.
        """
        current_time = time.time()
        elapsed = current_time - self.start_time

        # Суточный цикл (период 24 часа = 86400 сек)
        # Для демонстрации используем ускоренный цикл: 10 минут = 1 сутки
        day_cycle = 600  # секунд (10 минут)
        phase = (elapsed % day_cycle) / day_cycle * 2 * math.pi

        # Температура: максимум в середине дня, минимум ночью
        temp_cycle = math.sin(phase) * Config.SIM_TEMP_AMPLITUDE
        temp_noise = random.uniform(-Config.SIM_TEMP_NOISE, Config.SIM_TEMP_NOISE)
        temperature = Config.SIM_TEMP_BASE + temp_cycle + temp_noise

        # Влажность: обратная зависимость от температуры
        # Когда температура растет, влажность падает
        humidity_cycle = -math.sin(phase) * Config.SIM_HUMIDITY_AMPLITUDE
        humidity_noise = random.uniform(-Config.SIM_HUMIDITY_NOISE, Config.SIM_HUMIDITY_NOISE)
        humidity = Config.SIM_HUMIDITY_BASE + humidity_cycle + humidity_noise

        # Ограничиваем значения в реалистичных пределах
        temperature = max(-40, min(80, temperature))
        humidity = max(0, min(100, humidity))

        return round(temperature, 1), round(humidity, 1)


def get_sensor_reader():
    """
    Фабричная функция для создания соответствующего читателя датчика
    на основе конфигурации.
    """
    if Config.SENSOR_MODE == 'real':
        try:
            return RealSensorReader()
        except Exception as e:
            print(f"[ОШИБКА] Не удалось инициализировать реальный датчик: {e}")
            print("[ПРЕДУПРЕЖДЕНИЕ] Переключаюсь на режим симуляции")
            return SimulatedSensorReader()
    elif Config.SENSOR_MODE == 'simulated':
        return SimulatedSensorReader()
    else:
        raise ValueError(f"Неизвестный режим датчика: {Config.SENSOR_MODE}")


# Тестирование модуля
if __name__ == "__main__":
    print("=== Тестирование модуля sensor_reader ===\n")

    reader = get_sensor_reader()

    print(f"Режим: {Config.SENSOR_MODE}")
    print("Читаю данные 10 раз с интервалом 2 секунды...\n")

    for i in range(10):
        temp, hum = reader.read_data()
        timestamp = datetime.now().strftime("%H:%M:%S")

        if temp is not None and hum is not None:
            print(f"[{timestamp}] Температура: {temp:5.1f}°C | Влажность: {hum:5.1f}%")
        else:
            print(f"[{timestamp}] ОШИБКА чтения данных")

        time.sleep(2)
