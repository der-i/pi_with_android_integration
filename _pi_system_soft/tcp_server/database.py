"""
Модуль для работы с базой данных SQLite.
Хранит историю показаний датчиков температуры и влажности.
"""

import sqlite3
import time
from datetime import datetime, timedelta
from threading import Lock
from config import Config


class Database:
    """
    Управление базой данных для хранения истории показаний датчиков.
    """

    def __init__(self, db_path='greenhouse.db'):
        self.db_path = db_path
        self.lock = Lock()
        self._initialize_database()
        print(f"[БД] База данных инициализирована: {db_path}")

    def _initialize_database(self):
        """Создание таблиц если они не существуют"""
        with self.lock:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()

            # Таблица для хранения показаний датчиков
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS sensor_readings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    temperature REAL NOT NULL,
                    humidity REAL NOT NULL,
                    window_state TEXT NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            ''')

            # Индекс по timestamp для быстрого поиска
            cursor.execute('''
                CREATE INDEX IF NOT EXISTS idx_timestamp
                ON sensor_readings(timestamp)
            ''')

            # Таблица для хранения изменений настроек
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS settings_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    temp_min REAL NOT NULL,
                    temp_max REAL NOT NULL,
                    humidity_min REAL NOT NULL,
                    humidity_max REAL NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            ''')

            # Таблица для событий окна
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS window_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    action TEXT NOT NULL,
                    reason TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            ''')

            conn.commit()
            conn.close()

    def add_reading(self, temperature, humidity, window_state):
        """
        Добавить новое показание датчика.

        Args:
            temperature: температура в °C
            humidity: влажность в %
            window_state: состояние окна ("open" или "closed")
        """
        with self.lock:
            try:
                conn = sqlite3.connect(self.db_path)
                cursor = conn.cursor()

                timestamp = int(time.time())
                cursor.execute('''
                    INSERT INTO sensor_readings (timestamp, temperature, humidity, window_state)
                    VALUES (?, ?, ?, ?)
                ''', (timestamp, temperature, humidity, window_state))

                conn.commit()
                conn.close()
                return True
            except Exception as e:
                print(f"[БД] Ошибка добавления записи: {e}")
                return False

    def get_history(self, hours=24, limit=None):
        """
        Получить историю показаний за указанный период.

        Args:
            hours: количество часов назад от текущего времени
            limit: максимальное количество записей (None = все)

        Returns:
            list: список словарей с данными
        """
        with self.lock:
            try:
                conn = sqlite3.connect(self.db_path)
                cursor = conn.cursor()

                # Вычисляем timestamp начала периода
                start_timestamp = int(time.time() - hours * 3600)

                query = '''
                    SELECT timestamp, temperature, humidity, window_state
                    FROM sensor_readings
                    WHERE timestamp >= ?
                    ORDER BY timestamp ASC
                '''

                if limit:
                    query += f' LIMIT {limit}'

                cursor.execute(query, (start_timestamp,))
                rows = cursor.fetchall()
                conn.close()

                # Преобразуем в список словарей
                history = []
                for row in rows:
                    history.append({
                        'timestamp': row[0],
                        'temperature': row[1],
                        'humidity': row[2],
                        'window_state': row[3]
                    })

                return history
            except Exception as e:
                print(f"[БД] Ошибка получения истории: {e}")
                return []

    def get_aggregated_history(self, hours=24, interval_minutes=10):
        """
        Получить агрегированную историю (усредненные данные).
        Полезно для длительных периодов, чтобы уменьшить объем данных.

        Args:
            hours: период в часах
            interval_minutes: интервал агрегации в минутах

        Returns:
            list: список словарей с усредненными данными
        """
        with self.lock:
            try:
                conn = sqlite3.connect(self.db_path)
                cursor = conn.cursor()

                start_timestamp = int(time.time() - hours * 3600)
                interval_seconds = interval_minutes * 60

                # Группируем данные по интервалам и усредняем
                query = '''
                    SELECT
                        (timestamp / ?) * ? as interval_start,
                        AVG(temperature) as avg_temp,
                        AVG(humidity) as avg_humidity,
                        MIN(temperature) as min_temp,
                        MAX(temperature) as max_temp,
                        MIN(humidity) as min_humidity,
                        MAX(humidity) as max_humidity,
                        COUNT(*) as count
                    FROM sensor_readings
                    WHERE timestamp >= ?
                    GROUP BY interval_start
                    ORDER BY interval_start ASC
                '''

                cursor.execute(query, (interval_seconds, interval_seconds, start_timestamp))
                rows = cursor.fetchall()
                conn.close()

                history = []
                for row in rows:
                    history.append({
                        'timestamp': int(row[0]),
                        'temperature': round(row[1], 1),
                        'humidity': round(row[2], 1),
                        'temp_min': round(row[3], 1),
                        'temp_max': round(row[4], 1),
                        'humidity_min': round(row[5], 1),
                        'humidity_max': round(row[6], 1),
                        'samples': row[7]
                    })

                return history
            except Exception as e:
                print(f"[БД] Ошибка получения агрегированной истории: {e}")
                return []

    def get_statistics(self, hours=24):
        """
        Получить статистику за период.

        Args:
            hours: период в часах

        Returns:
            dict: статистика
        """
        with self.lock:
            try:
                conn = sqlite3.connect(self.db_path)
                cursor = conn.cursor()

                start_timestamp = int(time.time() - hours * 3600)

                cursor.execute('''
                    SELECT
                        AVG(temperature) as avg_temp,
                        MIN(temperature) as min_temp,
                        MAX(temperature) as max_temp,
                        AVG(humidity) as avg_humidity,
                        MIN(humidity) as min_humidity,
                        MAX(humidity) as max_humidity,
                        COUNT(*) as total_readings
                    FROM sensor_readings
                    WHERE timestamp >= ?
                ''', (start_timestamp,))

                row = cursor.fetchone()
                conn.close()

                if row and row[6] > 0:
                    return {
                        'period_hours': hours,
                        'total_readings': row[6],
                        'temperature': {
                            'avg': round(row[0], 1),
                            'min': round(row[1], 1),
                            'max': round(row[2], 1)
                        },
                        'humidity': {
                            'avg': round(row[3], 1),
                            'min': round(row[4], 1),
                            'max': round(row[5], 1)
                        }
                    }
                else:
                    return None
            except Exception as e:
                print(f"[БД] Ошибка получения статистики: {e}")
                return None

    def add_window_event(self, action, reason=None):
        """
        Записать событие изменения состояния окна.

        Args:
            action: действие ("opened" или "closed")
            reason: причина изменения
        """
        with self.lock:
            try:
                conn = sqlite3.connect(self.db_path)
                cursor = conn.cursor()

                timestamp = int(time.time())
                cursor.execute('''
                    INSERT INTO window_events (timestamp, action, reason)
                    VALUES (?, ?, ?)
                ''', (timestamp, action, reason))

                conn.commit()
                conn.close()
                return True
            except Exception as e:
                print(f"[БД] Ошибка записи события окна: {e}")
                return False

    def cleanup_old_data(self, days=30):
        """
        Удалить данные старше указанного количества дней.

        Args:
            days: количество дней для хранения
        """
        with self.lock:
            try:
                conn = sqlite3.connect(self.db_path)
                cursor = conn.cursor()

                cutoff_timestamp = int(time.time() - days * 86400)

                # Удаляем старые показания
                cursor.execute('DELETE FROM sensor_readings WHERE timestamp < ?', (cutoff_timestamp,))
                deleted_readings = cursor.rowcount

                # Удаляем старые события окна
                cursor.execute('DELETE FROM window_events WHERE timestamp < ?', (cutoff_timestamp,))
                deleted_events = cursor.rowcount

                conn.commit()
                conn.close()

                print(f"[БД] Очистка: удалено {deleted_readings} показаний и {deleted_events} событий")
                return True
            except Exception as e:
                print(f"[БД] Ошибка очистки данных: {e}")
                return False

    def get_total_records(self):
        """Получить общее количество записей"""
        with self.lock:
            try:
                conn = sqlite3.connect(self.db_path)
                cursor = conn.cursor()

                cursor.execute('SELECT COUNT(*) FROM sensor_readings')
                count = cursor.fetchone()[0]

                conn.close()
                return count
            except Exception as e:
                print(f"[БД] Ошибка получения количества записей: {e}")
                return 0


# Тестирование
if __name__ == "__main__":
    print("=== Тестирование модуля Database ===\n")

    # Создаем тестовую БД
    db = Database('test_greenhouse.db')

    print("1. Добавление тестовых данных...")
    import random
    base_time = int(time.time())

    for i in range(100):
        timestamp_offset = i * 60  # каждую минуту
        temp = 20 + random.uniform(-3, 3)
        humidity = 55 + random.uniform(-10, 10)
        window = "open" if temp > 23 else "closed"

        db.add_reading(temp, humidity, window)

    print(f"   Добавлено записей: {db.get_total_records()}")

    print("\n2. Получение истории за последний час...")
    history = db.get_history(hours=1)
    print(f"   Получено записей: {len(history)}")
    if history:
        print(f"   Первая запись: T={history[0]['temperature']}°C, H={history[0]['humidity']}%")
        print(f"   Последняя запись: T={history[-1]['temperature']}°C, H={history[-1]['humidity']}%")

    print("\n3. Получение агрегированной истории (10 мин интервалы)...")
    agg_history = db.get_aggregated_history(hours=2, interval_minutes=10)
    print(f"   Получено интервалов: {len(agg_history)}")
    if agg_history:
        first = agg_history[0]
        print(f"   Первый интервал: avg T={first['temperature']}°C, samples={first['samples']}")

    print("\n4. Статистика за период...")
    stats = db.get_statistics(hours=2)
    if stats:
        print(f"   Температура: avg={stats['temperature']['avg']}°C, "
              f"min={stats['temperature']['min']}°C, max={stats['temperature']['max']}°C")
        print(f"   Влажность: avg={stats['humidity']['avg']}%, "
              f"min={stats['humidity']['min']}%, max={stats['humidity']['max']}%")

    print("\n5. Добавление событий окна...")
    db.add_window_event("opened", "Температура выше порога")
    db.add_window_event("closed", "Температура в норме")

    print("\n6. Очистка старых данных...")
    db.cleanup_old_data(days=0)  # удалит все для теста

    print(f"\nОсталось записей: {db.get_total_records()}")
    print("\nТест завершен!")
