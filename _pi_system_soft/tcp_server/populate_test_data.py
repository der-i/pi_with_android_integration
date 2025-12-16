"""
Скрипт для заполнения базы данных тестовыми данными.
Создает реалистичную историю показаний за несколько дней.
"""

import time
import random
import math
from database import Database

def generate_test_data(db, days=7):
    """
    Генерирует тестовые данные за указанное количество дней.

    Args:
        db: экземпляр Database
        days: количество дней истории
    """
    print(f"Генерация тестовых данных за {days} дней...")

    current_time = int(time.time())
    start_time = current_time - (days * 24 * 3600)

    # Интервал между записями: 5 минут
    interval = 300

    records_added = 0

    # Генерируем данные с начального времени до текущего
    timestamp = start_time
    while timestamp <= current_time:
        # Вычисляем время суток (0-1)
        time_of_day = ((timestamp % 86400) / 86400.0)

        # Суточный цикл температуры (синусоида)
        # Минимум в 4 утра, максимум в 16:00
        temp_cycle = math.sin((time_of_day - 0.167) * 2 * math.pi)

        # Базовая температура с небольшими колебаниями по дням
        day_variation = math.sin((timestamp / 86400) * 0.5) * 2
        base_temp = 22.0 + day_variation

        # Амплитуда суточных колебаний
        temp_amplitude = 6.0

        # Добавляем шум
        temp_noise = random.uniform(-0.5, 0.5)

        temperature = base_temp + temp_cycle * temp_amplitude + temp_noise

        # Влажность обратно коррелирует с температурой
        humidity_base = 55.0
        humidity_cycle = -temp_cycle * 15.0
        humidity_noise = random.uniform(-2, 2)

        humidity = humidity_base + humidity_cycle + humidity_noise

        # Ограничиваем значения
        temperature = round(max(-40, min(80, temperature)), 1)
        humidity = round(max(0, min(100, humidity)), 1)

        # Определяем состояние окна (открыто если слишком жарко или влажно)
        window_state = "open" if (temperature > 26 or humidity > 70) else "closed"

        # Добавляем запись в БД (вручную указываем timestamp)
        conn = db.lock.__enter__()
        try:
            import sqlite3
            conn = sqlite3.connect(db.db_path)
            cursor = conn.cursor()
            cursor.execute('''
                INSERT INTO sensor_readings (timestamp, temperature, humidity, window_state)
                VALUES (?, ?, ?, ?)
            ''', (timestamp, temperature, humidity, window_state))
            conn.commit()
            conn.close()
            records_added += 1
        finally:
            db.lock.__exit__(None, None, None)

        # Переходим к следующему интервалу
        timestamp += interval

        # Показываем прогресс
        if records_added % 100 == 0:
            print(f"  Добавлено {records_added} записей...")

    print(f"\nГотово! Добавлено {records_added} записей")
    print(f"Период: {days} дней")
    print(f"Интервал: {interval} секунд ({interval/60} минут)")
    print(f"Всего записей в БД: {db.get_total_records()}")

if __name__ == "__main__":
    # Создаем БД
    db = Database('greenhouse.db')

    # Очищаем старые данные если есть
    current_count = db.get_total_records()
    if current_count > 0:
        response = input(f"\nВ базе уже есть {current_count} записей. Очистить? (y/n): ")
        if response.lower() == 'y':
            print("Очистка базы данных...")
            db.cleanup_old_data(days=0)
            print("База данных очищена.")

    # Спрашиваем количество дней
    try:
        days = int(input("\nСколько дней истории сгенерировать? (1-30, по умолчанию 7): ") or "7")
        if days < 1:
            days = 1
        if days > 30:
            days = 30
    except:
        days = 7

    print()
    generate_test_data(db, days=days)

    # Показываем статистику
    print("\n" + "="*60)
    print("СТАТИСТИКА")
    print("="*60)

    for period in [24, 168, days * 24]:  # 1 день, неделя, весь период
        if period > days * 24:
            continue
        stats = db.get_statistics(hours=period)
        if stats:
            print(f"\nЗа последние {period} часов ({period/24:.1f} дней):")
            print(f"  Записей: {stats['total_readings']}")
            print(f"  Температура: {stats['temperature']['min']}°C - {stats['temperature']['max']}°C "
                  f"(средняя: {stats['temperature']['avg']}°C)")
            print(f"  Влажность: {stats['humidity']['min']}% - {stats['humidity']['max']}% "
                  f"(средняя: {stats['humidity']['avg']}%)")

    print("\n" + "="*60)
    print("Тестовые данные успешно созданы!")
    print("Теперь можно запустить сервер: python main.py")
    print("="*60)
