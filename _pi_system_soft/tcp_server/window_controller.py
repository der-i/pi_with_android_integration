"""
Модуль управления окном теплицы.
Управляет реле для открытия/закрытия окна на основе показаний датчика и пороговых значений.
"""

import time
from threading import Lock
from config import Config


class WindowController:
    """
    Контроллер окна теплицы.
    Управляет состоянием окна через GPIO реле.
    """

    def __init__(self):
        self.is_open = False
        self.lock = Lock()
        self.gpio_available = False
        self.gpio = None

        # Попытка инициализации GPIO
        try:
            if Config.SENSOR_MODE == 'real':
                import RPi.GPIO as GPIO
                self.gpio = GPIO
                GPIO.setmode(GPIO.BCM)
                GPIO.setup(Config.WINDOW_RELAY_PIN, GPIO.OUT)
                GPIO.output(Config.WINDOW_RELAY_PIN, GPIO.LOW)  # Изначально закрыто
                self.gpio_available = True
                print(f"[ОКНО] GPIO инициализировано на пине {Config.WINDOW_RELAY_PIN}")
            else:
                print("[ОКНО] Режим симуляции - GPIO не используется")
        except ImportError:
            print("[ОКНО] RPi.GPIO не доступен - работаю в режиме симуляции")
        except Exception as e:
            print(f"[ОКНО] Ошибка инициализации GPIO: {e}")

    def open_window(self):
        """Открыть окно"""
        with self.lock:
            if not self.is_open:
                if self.gpio_available:
                    self.gpio.output(Config.WINDOW_RELAY_PIN, self.gpio.HIGH)
                    print("[ОКНО] 🔓 Окно ОТКРЫТО (GPIO HIGH)")
                else:
                    print("[ОКНО] 🔓 Окно ОТКРЫТО (симуляция)")
                self.is_open = True
                return True
            else:
                print("[ОКНО] ℹ️  Окно уже открыто")
                return False

    def close_window(self):
        """Закрыть окно"""
        with self.lock:
            if self.is_open:
                if self.gpio_available:
                    self.gpio.output(Config.WINDOW_RELAY_PIN, self.gpio.LOW)
                    print("[ОКНО] 🔒 Окно ЗАКРЫТО (GPIO LOW)")
                else:
                    print("[ОКНО] 🔒 Окно ЗАКРЫТО (симуляция)")
                self.is_open = False
                return True
            else:
                print("[ОКНО] ℹ️  Окно уже закрыто")
                return False

    def get_state(self):
        """Получить текущее состояние окна"""
        with self.lock:
            return "open" if self.is_open else "closed"

    def is_window_open(self):
        """Проверить, открыто ли окно"""
        with self.lock:
            return self.is_open

    def process_sensor_data(self, temperature, humidity, threshold_status):
        """
        Обработка данных датчика и принятие решения об открытии/закрытии окна.

        Логика управления:
        - Если температура слишком высокая ИЛИ влажность слишком высокая -> открыть окно
        - Если температура слишком низкая ИЛИ влажность слишком низкая -> закрыть окно
        - Если все в норме -> оставить как есть

        threshold_status: результат от settings_manager.check_thresholds()
        """
        with self.lock:
            action_taken = None

            # Критические условия для открытия окна (проветривание)
            should_open = threshold_status['temp_too_high'] or threshold_status['humidity_too_high']

            # Критические условия для закрытия окна (сохранение тепла/влажности)
            should_close = threshold_status['temp_too_low'] or threshold_status['humidity_too_low']

            if should_open and not self.is_open:
                if self.gpio_available:
                    self.gpio.output(Config.WINDOW_RELAY_PIN, self.gpio.HIGH)
                self.is_open = True
                action_taken = "opened"
                reasons = []
                if threshold_status['temp_too_high']:
                    reasons.append(f"температура {temperature}°C выше порога")
                if threshold_status['humidity_too_high']:
                    reasons.append(f"влажность {humidity}% выше порога")
                print(f"[ОКНО] Окно ОТКРЫТО: {', '.join(reasons)}")

            elif should_close and self.is_open:
                if self.gpio_available:
                    self.gpio.output(Config.WINDOW_RELAY_PIN, self.gpio.LOW)
                self.is_open = False
                action_taken = "closed"
                reasons = []
                if threshold_status['temp_too_low']:
                    reasons.append(f"температура {temperature}°C ниже порога")
                if threshold_status['humidity_too_low']:
                    reasons.append(f"влажность {humidity}% ниже порога")
                print(f"[ОКНО] Окно ЗАКРЫТО: {', '.join(reasons)}")

            elif not threshold_status['action_needed']:
                current_state_str = "open" if self.is_open else "closed"
                print(f"[ОКНО] Параметры в норме (T={temperature}°C, H={humidity}%), состояние: {current_state_str}")

            return {
                'action_taken': action_taken,
                'current_state': "open" if self.is_open else "closed",
                'reasons': threshold_status
            }

    def __del__(self):
        """Очистка ресурсов при удалении объекта"""
        if self.gpio_available and self.gpio:
            try:
                self.gpio.cleanup(Config.WINDOW_RELAY_PIN)
                print("[ОКНО] GPIO очищен")
            except:
                pass


# Тестирование
if __name__ == "__main__":
    print("=== Тестирование WindowController ===\n")

    controller = WindowController()

    print("1. Проверка начального состояния:")
    print(f"   Состояние окна: {controller.get_state()}")

    print("\n2. Тест открытия окна:")
    controller.open_window()
    print(f"   Состояние окна: {controller.get_state()}")

    print("\n3. Тест повторного открытия:")
    controller.open_window()

    print("\n4. Тест закрытия окна:")
    controller.close_window()
    print(f"   Состояние окна: {controller.get_state()}")

    print("\n5. Тест повторного закрытия:")
    controller.close_window()

    print("\n6. Тест обработки данных датчика:")

    test_scenarios = [
        {
            'temp': 30.0,
            'humidity': 55.0,
            'status': {
                'temp_too_low': False,
                'temp_too_high': True,
                'humidity_too_low': False,
                'humidity_too_high': False,
                'action_needed': True
            },
            'description': 'Высокая температура'
        },
        {
            'temp': 22.0,
            'humidity': 75.0,
            'status': {
                'temp_too_low': False,
                'temp_too_high': False,
                'humidity_too_low': False,
                'humidity_too_high': True,
                'action_needed': True
            },
            'description': 'Высокая влажность'
        },
        {
            'temp': 15.0,
            'humidity': 55.0,
            'status': {
                'temp_too_low': True,
                'temp_too_high': False,
                'humidity_too_low': False,
                'humidity_too_high': False,
                'action_needed': True
            },
            'description': 'Низкая температура'
        },
        {
            'temp': 22.0,
            'humidity': 55.0,
            'status': {
                'temp_too_low': False,
                'temp_too_high': False,
                'humidity_too_low': False,
                'humidity_too_high': False,
                'action_needed': False
            },
            'description': 'Норма'
        }
    ]

    for scenario in test_scenarios:
        print(f"\n   Сценарий: {scenario['description']}")
        result = controller.process_sensor_data(
            scenario['temp'],
            scenario['humidity'],
            scenario['status']
        )
        print(f"   Результат: {result['action_taken'] or 'нет действий'}")
        print(f"   Текущее состояние: {result['current_state']}")
        time.sleep(1)
