"""
Простой тестовый скрипт для проверки API.
Использует только стандартную библиотеку Python.
"""

import urllib.request
import json

BASE_URL = "http://127.0.0.1:5000"

def test_endpoint(endpoint, method="GET", data=None):
    """Тестирует эндпоинт и выводит результат"""
    url = f"{BASE_URL}{endpoint}"
    print(f"\n{'='*60}")
    print(f"{method} {endpoint}")
    print('='*60)

    try:
        headers = {'User-Agent': 'TeplicaTestClient/1.0'}

        if method == "GET":
            request = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(request) as response:
                result = json.loads(response.read().decode())
                print(json.dumps(result, indent=2, ensure_ascii=False))
        elif method == "POST":
            headers['Content-Type'] = 'application/json'
            req_data = json.dumps(data).encode('utf-8')
            request = urllib.request.Request(url, data=req_data, headers=headers, method='POST')
            with urllib.request.urlopen(request) as response:
                result = json.loads(response.read().decode())
                print(json.dumps(result, indent=2, ensure_ascii=False))

        return True
    except urllib.error.HTTPError as e:
        print(f"HTTP ОШИБКА {e.code}: {e.reason}")
        try:
            error_body = e.read().decode()
            print(f"Детали: {error_body}")
        except:
            pass
        return False
    except Exception as e:
        print(f"ОШИБКА: {e}")
        return False

def main():
    print("\n" + "="*60)
    print("ТЕСТИРОВАНИЕ API СИСТЕМЫ МОНИТОРИНГА ТЕПЛИЦЫ")
    print("="*60)

    # Тест 1: Health check
    test_endpoint("/health")

    # Тест 2: Статус системы
    test_endpoint("/status")

    # Тест 3: Получение данных датчика
    test_endpoint("/data")

    # Тест 4: Получение настроек
    test_endpoint("/settings")

    # Тест 5: Состояние окна
    test_endpoint("/window")

    # Тест 6: Обновление настроек
    new_settings = {
        "temp_min": 20.0,
        "temp_max": 26.0,
        "humidity_min": 45.0,
        "humidity_max": 65.0
    }
    test_endpoint("/settings", method="POST", data=new_settings)

    # Тест 7: Проверяем, что настройки изменились
    test_endpoint("/settings")

    # Тест 8: Открываем окно
    test_endpoint("/window/open", method="POST")

    # Тест 9: Проверяем состояние окна
    test_endpoint("/window")

    # Тест 10: Закрываем окно
    test_endpoint("/window/close", method="POST")

    # Тест 11: Проверяем состояние окна снова
    test_endpoint("/window")

    # Тест 12: Получаем данные несколько раз подряд (проверка симуляции)
    print(f"\n{'='*60}")
    print("ТЕСТ СИМУЛЯЦИИ - Получение данных 5 раз с интервалом 2 сек")
    print('='*60)

    import time
    for i in range(5):
        print(f"\nЧтение #{i+1}:")
        try:
            with urllib.request.urlopen(f"{BASE_URL}/data") as response:
                result = json.loads(response.read().decode())
                temp = result['temperature']
                hum = result['humidity']
                window_state = result['window_state']
                print(f"  Температура: {temp}°C")
                print(f"  Влажность: {hum}%")
                print(f"  Окно: {window_state}")

                # Проверяем алерты
                alerts = result['alerts']
                if any(alerts.values()):
                    print("  АЛЕРТЫ:")
                    if alerts['temp_too_low']:
                        print("    ⚠ Температура ниже порога")
                    if alerts['temp_too_high']:
                        print("    ⚠ Температура выше порога")
                    if alerts['humidity_too_low']:
                        print("    ⚠ Влажность ниже порога")
                    if alerts['humidity_too_high']:
                        print("    ⚠ Влажность выше порога")
        except Exception as e:
            print(f"  ОШИБКА: {e}")

        if i < 4:
            time.sleep(2)

    print(f"\n{'='*60}")
    print("ТЕСТИРОВАНИЕ ЗАВЕРШЕНО")
    print('='*60)

if __name__ == "__main__":
    main()
