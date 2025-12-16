"""
Flask сервер для системы мониторинга теплицы.
Предоставляет REST API для Android приложения.
"""

from flask import Flask, jsonify, request
from flask_cors import CORS
import time
from datetime import datetime
import logging
from logging.handlers import RotatingFileHandler

from config import Config
from sensor_reader import get_sensor_reader
from settings_manager import SettingsManager
from window_controller import WindowController
from database import Database


# Инициализация Flask приложения
app = Flask(__name__)
CORS(app)

# Настройка логирования
def setup_logging():
    """Настройка системы логирования"""
    if not app.debug:
        file_handler = RotatingFileHandler(
            Config.LOG_FILE,
            maxBytes=1024 * 1024,  # 1MB
            backupCount=10
        )
        file_handler.setFormatter(logging.Formatter(
            '[%(asctime)s] %(levelname)s: %(message)s'
        ))
        file_handler.setLevel(logging.INFO)
        app.logger.addHandler(file_handler)
        app.logger.setLevel(logging.INFO)
        app.logger.info('Сервер теплицы запущен')

# Инициализация компонентов системы
sensor_reader = get_sensor_reader()
settings_manager = SettingsManager()
window_controller = WindowController()
database = Database()

# Последние данные (кэш)
last_sensor_data = {
    'temperature': None,
    'humidity': None,
    'timestamp': None
}


@app.route('/data', methods=['GET'])
def get_data():
    """
    GET /data
    Получить текущие показания датчика температуры и влажности.

    Ответ:
    {
        "temperature": float,
        "humidity": float,
        "timestamp": int (Unix timestamp),
        "window_state": "open" | "closed",
        "thresholds": {
            "temp_min": float,
            "temp_max": float,
            "humidity_min": float,
            "humidity_max": float
        },
        "alerts": {
            "temp_too_low": bool,
            "temp_too_high": bool,
            "humidity_too_low": bool,
            "humidity_too_high": bool
        }
    }
    """
    try:
        print("[DEBUG] /data endpoint called")
        # Чтение данных с датчика
        print("[DEBUG] Reading sensor data...")
        temperature, humidity = sensor_reader.read_data()
        print(f"[DEBUG] Got sensor data: T={temperature}, H={humidity}")

        if temperature is not None and humidity is not None:
            timestamp = int(time.time())

            # Обновление кэша
            last_sensor_data['temperature'] = temperature
            last_sensor_data['humidity'] = humidity
            last_sensor_data['timestamp'] = timestamp

            print("[DEBUG] Checking thresholds...")
            # Проверка пороговых значений
            threshold_status = settings_manager.check_thresholds(temperature, humidity)
            print(f"[DEBUG] Threshold status: {threshold_status}")

            print("[DEBUG] Processing window control...")
            # Автоматическое управление окном
            if settings_manager.is_auto_control_enabled():
                window_result = window_controller.process_sensor_data(
                    temperature, humidity, threshold_status
                )
            else:
                window_result = {'current_state': window_controller.get_state()}

            # Получение текущих настроек
            current_settings = settings_manager.get_settings()

            # Сохраняем данные в базу
            database.add_reading(temperature, humidity, window_result['current_state'])

            response = {
                "temperature": temperature,
                "humidity": humidity,
                "timestamp": timestamp,
                "window_state": window_result['current_state'],
                "thresholds": {
                    "temp_min": current_settings['temp_min'],
                    "temp_max": current_settings['temp_max'],
                    "humidity_min": current_settings['humidity_min'],
                    "humidity_max": current_settings['humidity_max']
                },
                "alerts": {
                    "temp_too_low": threshold_status['temp_too_low'],
                    "temp_too_high": threshold_status['temp_too_high'],
                    "humidity_too_low": threshold_status['humidity_too_low'],
                    "humidity_too_high": threshold_status['humidity_too_high']
                }
            }

            app.logger.info(f"Данные отправлены: T={temperature}°C, H={humidity}%")
            return jsonify(response), 200

        else:
            app.logger.error("Не удалось прочитать данные с датчика")
            return jsonify({
                "error": "Failed to read sensor data",
                "message": "Датчик не отвечает или вернул некорректные данные"
            }), 500

    except Exception as e:
        app.logger.error(f"Ошибка в /data: {str(e)}")
        return jsonify({
            "error": "Internal server error",
            "message": str(e)
        }), 500


@app.route('/settings', methods=['GET'])
def get_settings():
    """
    GET /settings
    Получить текущие пороговые настройки.

    Ответ:
    {
        "temp_min": float,
        "temp_max": float,
        "humidity_min": float,
        "humidity_max": float,
        "window_auto_control": bool
    }
    """
    try:
        settings = settings_manager.get_settings()
        app.logger.info("Настройки отправлены клиенту")
        return jsonify(settings), 200
    except Exception as e:
        app.logger.error(f"Ошибка в GET /settings: {str(e)}")
        return jsonify({"error": str(e)}), 500


@app.route('/settings', methods=['POST'])
def update_settings():
    """
    POST /settings
    Обновить пороговые настройки.

    Тело запроса (JSON):
    {
        "temp_min": float (опционально),
        "temp_max": float (опционально),
        "humidity_min": float (опционально),
        "humidity_max": float (опционально),
        "window_auto_control": bool (опционально)
    }

    Ответ:
    {
        "success": true,
        "settings": {...}
    }
    """
    try:
        new_settings = request.get_json()

        if not new_settings:
            return jsonify({
                "error": "Invalid request",
                "message": "Тело запроса должно быть в формате JSON"
            }), 400

        # Обновление настроек
        updated_settings = settings_manager.update_settings(new_settings)

        app.logger.info(f"Настройки обновлены: {updated_settings}")

        return jsonify({
            "success": True,
            "settings": updated_settings,
            "message": "Настройки успешно обновлены"
        }), 200

    except ValueError as e:
        app.logger.warning(f"Ошибка валидации настроек: {str(e)}")
        return jsonify({
            "error": "Validation error",
            "message": str(e)
        }), 400
    except Exception as e:
        app.logger.error(f"Ошибка в POST /settings: {str(e)}")
        return jsonify({
            "error": "Internal server error",
            "message": str(e)
        }), 500


@app.route('/window', methods=['GET'])
def get_window_state():
    """
    GET /window
    Получить текущее состояние окна.

    Ответ:
    {
        "state": "open" | "closed",
        "is_open": bool
    }
    """
    try:
        state = window_controller.get_state()
        return jsonify({
            "state": state,
            "is_open": window_controller.is_window_open()
        }), 200
    except Exception as e:
        app.logger.error(f"Ошибка в GET /window: {str(e)}")
        return jsonify({"error": str(e)}), 500


@app.route('/window/open', methods=['POST'])
def open_window():
    """
    POST /window/open
    Вручную открыть окно (отключает автоматическое управление на это действие).

    Ответ:
    {
        "success": true,
        "state": "open"
    }
    """
    try:
        window_controller.open_window()
        return jsonify({
            "success": True,
            "state": window_controller.get_state(),
            "message": "Окно открыто"
        }), 200
    except Exception as e:
        app.logger.error(f"Ошибка в POST /window/open: {str(e)}")
        return jsonify({"error": str(e)}), 500


@app.route('/window/close', methods=['POST'])
def close_window():
    """
    POST /window/close
    Вручную закрыть окно (отключает автоматическое управление на это действие).

    Ответ:
    {
        "success": true,
        "state": "closed"
    }
    """
    try:
        window_controller.close_window()
        return jsonify({
            "success": True,
            "state": window_controller.get_state(),
            "message": "Окно закрыто"
        }), 200
    except Exception as e:
        app.logger.error(f"Ошибка в POST /window/close: {str(e)}")
        return jsonify({"error": str(e)}), 500


@app.route('/status', methods=['GET'])
def get_status():
    """
    GET /status
    Получить общий статус системы.

    Ответ:
    {
        "status": "ok",
        "mode": "real" | "simulated",
        "uptime": int (секунды),
        "last_reading": {...}
    }
    """
    try:
        return jsonify({
            "status": "ok",
            "mode": Config.SENSOR_MODE,
            "server_time": datetime.now().isoformat(),
            "last_reading": last_sensor_data,
            "window_state": window_controller.get_state(),
            "auto_control": settings_manager.is_auto_control_enabled()
        }), 200
    except Exception as e:
        app.logger.error(f"Ошибка в /status: {str(e)}")
        return jsonify({"error": str(e)}), 500


@app.route('/health', methods=['GET'])
def health_check():
    """
    GET /health
    Проверка работоспособности сервера (для мониторинга).
    """
    return jsonify({"status": "healthy"}), 200


@app.route('/history', methods=['GET'])
def get_history():
    """
    GET /history?hours=24&limit=1000
    Получить историю показаний датчика.

    Query параметры:
    - hours: период в часах (по умолчанию 24)
    - limit: максимальное количество записей (опционально)

    Ответ:
    {
        "success": true,
        "count": int,
        "period_hours": int,
        "data": [
            {
                "timestamp": int,
                "temperature": float,
                "humidity": float,
                "window_state": string
            },
            ...
        ]
    }
    """
    try:
        # Получаем параметры из query string
        hours = request.args.get('hours', default=24, type=int)
        limit = request.args.get('limit', default=None, type=int)

        # Ограничиваем максимальный период
        if hours > 8760:  # 1 год
            hours = 8760

        # Получаем историю из базы
        history = database.get_history(hours=hours, limit=limit)

        return jsonify({
            "success": True,
            "count": len(history),
            "period_hours": hours,
            "data": history
        }), 200

    except Exception as e:
        app.logger.error(f"Ошибка в /history: {str(e)}")
        return jsonify({
            "error": "Internal server error",
            "message": str(e)
        }), 500


@app.route('/history/aggregated', methods=['GET'])
def get_aggregated_history():
    """
    GET /history/aggregated?hours=168&interval=60
    Получить агрегированную (усредненную) историю.
    Полезно для длительных периодов.

    Query параметры:
    - hours: период в часах (по умолчанию 168 = неделя)
    - interval: интервал агрегации в минутах (по умолчанию 60)

    Ответ:
    {
        "success": true,
        "count": int,
        "period_hours": int,
        "interval_minutes": int,
        "data": [
            {
                "timestamp": int,
                "temperature": float,
                "humidity": float,
                "temp_min": float,
                "temp_max": float,
                "humidity_min": float,
                "humidity_max": float,
                "samples": int
            },
            ...
        ]
    }
    """
    try:
        # Получаем параметры
        hours = request.args.get('hours', default=168, type=int)
        interval = request.args.get('interval', default=60, type=int)

        # Ограничения
        if hours > 8760:
            hours = 8760
        if interval < 1:
            interval = 1
        if interval > 1440:  # не больше суток
            interval = 1440

        # Получаем агрегированную историю
        history = database.get_aggregated_history(hours=hours, interval_minutes=interval)

        return jsonify({
            "success": True,
            "count": len(history),
            "period_hours": hours,
            "interval_minutes": interval,
            "data": history
        }), 200

    except Exception as e:
        app.logger.error(f"Ошибка в /history/aggregated: {str(e)}")
        return jsonify({
            "error": "Internal server error",
            "message": str(e)
        }), 500


@app.route('/history/statistics', methods=['GET'])
def get_history_statistics():
    """
    GET /history/statistics?hours=24
    Получить статистику за период.

    Query параметры:
    - hours: период в часах (по умолчанию 24)

    Ответ:
    {
        "success": true,
        "statistics": {
            "period_hours": int,
            "total_readings": int,
            "temperature": {
                "avg": float,
                "min": float,
                "max": float
            },
            "humidity": {
                "avg": float,
                "min": float,
                "max": float
            }
        }
    }
    """
    try:
        hours = request.args.get('hours', default=24, type=int)

        if hours > 8760:
            hours = 8760

        stats = database.get_statistics(hours=hours)

        if stats:
            return jsonify({
                "success": True,
                "statistics": stats
            }), 200
        else:
            return jsonify({
                "success": False,
                "message": "Нет данных за указанный период"
            }), 404

    except Exception as e:
        app.logger.error(f"Ошибка в /history/statistics: {str(e)}")
        return jsonify({
            "error": "Internal server error",
            "message": str(e)
        }), 500


@app.errorhandler(404)
def not_found(error):
    """Обработчик 404 ошибки"""
    return jsonify({
        "error": "Not found",
        "message": "Эндпоинт не найден"
    }), 404


@app.errorhandler(500)
def internal_error(error):
    """Обработчик 500 ошибки"""
    app.logger.error(f"Внутренняя ошибка сервера: {str(error)}")
    return jsonify({
        "error": "Internal server error",
        "message": "Произошла внутренняя ошибка сервера"
    }), 500


# Точка входа
if __name__ == '__main__':
    setup_logging()

    print("=" * 60)
    print("СИСТЕМА МОНИТОРИНГА ТЕПЛИЦЫ")
    print("=" * 60)
    print(f"Режим работы датчика: {Config.SENSOR_MODE.upper()}")
    print(f"Сервер запущен на: http://{Config.HOST}:{Config.PORT}")
    print(f"База данных: {database.db_path}")
    print(f"\nДоступные эндпоинты:")
    print(f"  GET  /data                    - Получить данные датчика")
    print(f"  GET  /settings                - Получить настройки")
    print(f"  POST /settings                - Обновить настройки")
    print(f"  GET  /window                  - Состояние окна")
    print(f"  POST /window/open             - Открыть окно")
    print(f"  POST /window/close            - Закрыть окно")
    print(f"  GET  /status                  - Общий статус системы")
    print(f"  GET  /health                  - Health check")
    print(f"  GET  /history                 - История показаний")
    print(f"  GET  /history/aggregated      - Агрегированная история")
    print(f"  GET  /history/statistics      - Статистика за период")
    print("=" * 60)
    print()

    app.run(
        host=Config.HOST,
        port=Config.PORT,
        debug=Config.DEBUG
    )
