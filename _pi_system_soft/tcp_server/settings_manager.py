"""
Менеджер настроек теплицы.
Управляет пороговыми значениями температуры и влажности.
"""

import json
import os
from threading import Lock
from config import Config


class SettingsManager:
    """
    Управление настройками теплицы.
    Singleton класс для хранения пороговых значений.
    """

    _instance = None
    _lock = Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        if not hasattr(self, 'initialized'):
            self.settings_file = 'greenhouse_settings.json'
            self.settings = self._load_settings()
            self.initialized = True
            print(f"[НАСТРОЙКИ] Загружены: {self.settings}")

    def _load_settings(self):
        """Загрузка настроек из файла или создание дефолтных"""
        if os.path.exists(self.settings_file):
            try:
                with open(self.settings_file, 'r', encoding='utf-8') as f:
                    settings = json.load(f)
                    print(f"[НАСТРОЙКИ] Загружены из {self.settings_file}")
                    return settings
            except Exception as e:
                print(f"[НАСТРОЙКИ] Ошибка загрузки файла: {e}")

        # Дефолтные настройки
        default_settings = {
            'temp_min': Config.DEFAULT_TEMP_MIN,
            'temp_max': Config.DEFAULT_TEMP_MAX,
            'humidity_min': Config.DEFAULT_HUMIDITY_MIN,
            'humidity_max': Config.DEFAULT_HUMIDITY_MAX,
            'window_auto_control': True  # Автоматическое управление окном
        }
        self._save_settings(default_settings)
        print(f"[НАСТРОЙКИ] Созданы дефолтные настройки")
        return default_settings

    def _save_settings(self, settings):
        """Сохранение настроек в файл"""
        try:
            with open(self.settings_file, 'w', encoding='utf-8') as f:
                json.dump(settings, f, indent=4, ensure_ascii=False)
            print(f"[НАСТРОЙКИ] Сохранены в {self.settings_file}")
        except Exception as e:
            print(f"[НАСТРОЙКИ] Ошибка сохранения: {e}")

    def get_settings(self):
        """Получить текущие настройки"""
        with self._lock:
            return self.settings.copy()

    def update_settings(self, new_settings):
        """
        Обновить настройки.
        new_settings: dict с ключами temp_min, temp_max, humidity_min, humidity_max
        """
        with self._lock:
            # Валидация входных данных
            if 'temp_min' in new_settings:
                temp_min = float(new_settings['temp_min'])
                if not (-40 <= temp_min <= 80):
                    raise ValueError(f"temp_min должен быть в диапазоне -40..80°C")
                self.settings['temp_min'] = temp_min

            if 'temp_max' in new_settings:
                temp_max = float(new_settings['temp_max'])
                if not (-40 <= temp_max <= 80):
                    raise ValueError(f"temp_max должен быть в диапазоне -40..80°C")
                self.settings['temp_max'] = temp_max

            if 'humidity_min' in new_settings:
                humidity_min = float(new_settings['humidity_min'])
                if not (0 <= humidity_min <= 100):
                    raise ValueError(f"humidity_min должен быть в диапазоне 0..100%")
                self.settings['humidity_min'] = humidity_min

            if 'humidity_max' in new_settings:
                humidity_max = float(new_settings['humidity_max'])
                if not (0 <= humidity_max <= 100):
                    raise ValueError(f"humidity_max должен быть в диапазоне 0..100%")
                self.settings['humidity_max'] = humidity_max

            if 'window_auto_control' in new_settings:
                self.settings['window_auto_control'] = bool(new_settings['window_auto_control'])

            # Проверка логичности значений
            if self.settings['temp_min'] >= self.settings['temp_max']:
                raise ValueError("temp_min должен быть меньше temp_max")

            if self.settings['humidity_min'] >= self.settings['humidity_max']:
                raise ValueError("humidity_min должен быть меньше humidity_max")

            self._save_settings(self.settings)
            print(f"[НАСТРОЙКИ] Обновлены: {self.settings}")
            return self.settings.copy()

    def check_thresholds(self, temperature, humidity):
        """
        Проверка превышения порогов.
        Возвращает dict с информацией о нарушениях:
        {
            'temp_too_low': bool,
            'temp_too_high': bool,
            'humidity_too_low': bool,
            'humidity_too_high': bool,
            'action_needed': bool  # требуется ли действие
        }
        """
        with self._lock:
            result = {
                'temp_too_low': temperature < self.settings['temp_min'],
                'temp_too_high': temperature > self.settings['temp_max'],
                'humidity_too_low': humidity < self.settings['humidity_min'],
                'humidity_too_high': humidity > self.settings['humidity_max']
            }

            # Требуется действие, если любой порог нарушен
            result['action_needed'] = any([
                result['temp_too_low'],
                result['temp_too_high'],
                result['humidity_too_low'],
                result['humidity_too_high']
            ])

            return result

    def get_temp_range(self):
        """Получить диапазон температуры"""
        with self._lock:
            return self.settings['temp_min'], self.settings['temp_max']

    def get_humidity_range(self):
        """Получить диапазон влажности"""
        with self._lock:
            return self.settings['humidity_min'], self.settings['humidity_max']

    def is_auto_control_enabled(self):
        """Проверить, включено ли автоматическое управление"""
        with self._lock:
            return self.settings.get('window_auto_control', True)


# Тестирование
if __name__ == "__main__":
    print("=== Тестирование SettingsManager ===\n")

    manager = SettingsManager()

    print("1. Текущие настройки:")
    print(json.dumps(manager.get_settings(), indent=2, ensure_ascii=False))

    print("\n2. Обновление настроек:")
    new_settings = {
        'temp_min': 20.0,
        'temp_max': 26.0,
        'humidity_min': 50.0,
        'humidity_max': 65.0
    }
    manager.update_settings(new_settings)

    print("\n3. Проверка пороговых значений:")
    test_cases = [
        (22.0, 55.0, "Норма"),
        (18.0, 55.0, "Температура низкая"),
        (28.0, 55.0, "Температура высокая"),
        (22.0, 45.0, "Влажность низкая"),
        (22.0, 70.0, "Влажность высокая"),
    ]

    for temp, hum, description in test_cases:
        result = manager.check_thresholds(temp, hum)
        print(f"\n{description} (T={temp}°C, H={hum}%):")
        print(f"  Действие требуется: {result['action_needed']}")
        if result['temp_too_low']:
            print("  ⚠ Температура ниже минимума")
        if result['temp_too_high']:
            print("  ⚠ Температура выше максимума")
        if result['humidity_too_low']:
            print("  ⚠ Влажность ниже минимума")
        if result['humidity_too_high']:
            print("  ⚠ Влажность выше максимума")
