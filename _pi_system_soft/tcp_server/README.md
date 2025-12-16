# Backend сервер системы мониторинга теплицы

Полноценный Flask REST API для системы управления теплицей на базе Raspberry Pi с датчиком DHT22.

## Установка

### 1. Установка зависимостей

```bash
cd _pi_system_soft/tcp_server
pip install -r ../../requirements.txt
```

### 2. Настройка GPIO (только для реального Raspberry Pi)

Убедитесь, что GPIO правильно настроены:
- **DHT22 датчик** подключен к GPIO4 (физический пин 7)
- **Реле для окна** подключено к GPIO17 (физический пин 11)

### 3. Выбор режима работы

Откройте файл [config.py](config.py) и установите режим:

```python
# Для тестирования без оборудования
SENSOR_MODE = 'simulated'

# Для работы с реальным датчиком
SENSOR_MODE = 'real'
```

## Запуск сервера

```bash
python main.py
```

Сервер запустится на `http://0.0.0.0:5000`

## API Endpoints

### 1. Получить данные датчика

**GET** `/data`

Возвращает текущие показания датчика, состояние окна и пороговые значения.

**Ответ:**
```json
{
  "temperature": 23.5,
  "humidity": 58.2,
  "timestamp": 1734384000,
  "window_state": "closed",
  "thresholds": {
    "temp_min": 18.0,
    "temp_max": 28.0,
    "humidity_min": 40.0,
    "humidity_max": 70.0
  },
  "alerts": {
    "temp_too_low": false,
    "temp_too_high": false,
    "humidity_too_low": false,
    "humidity_too_high": false
  }
}
```

### 2. Получить настройки

**GET** `/settings`

**Ответ:**
```json
{
  "temp_min": 18.0,
  "temp_max": 28.0,
  "humidity_min": 40.0,
  "humidity_max": 70.0,
  "window_auto_control": true
}
```

### 3. Обновить настройки

**POST** `/settings`

**Тело запроса:**
```json
{
  "temp_min": 20.0,
  "temp_max": 26.0,
  "humidity_min": 45.0,
  "humidity_max": 65.0,
  "window_auto_control": true
}
```

**Ответ:**
```json
{
  "success": true,
  "settings": { /* обновленные настройки */ },
  "message": "Настройки успешно обновлены"
}
```

### 4. Получить состояние окна

**GET** `/window`

**Ответ:**
```json
{
  "state": "closed",
  "is_open": false
}
```

### 5. Открыть окно вручную

**POST** `/window/open`

**Ответ:**
```json
{
  "success": true,
  "state": "open",
  "message": "Окно открыто"
}
```

### 6. Закрыть окно вручную

**POST** `/window/close`

**Ответ:**
```json
{
  "success": true,
  "state": "closed",
  "message": "Окно закрыто"
}
```

### 7. Общий статус системы

**GET** `/status`

**Ответ:**
```json
{
  "status": "ok",
  "mode": "simulated",
  "server_time": "2025-12-16T23:20:00",
  "last_reading": {
    "temperature": 23.5,
    "humidity": 58.2,
    "timestamp": 1734384000
  },
  "window_state": "closed",
  "auto_control": true
}
```

### 8. Health check

**GET** `/health`

**Ответ:**
```json
{
  "status": "healthy"
}
```

## Логика управления окном

Система автоматически управляет окном на основе пороговых значений:

### Открытие окна (проветривание)
- Температура **выше** максимума
- **ИЛИ** влажность **выше** максимума

### Закрытие окна (сохранение микроклимата)
- Температура **ниже** минимума
- **ИЛИ** влажность **ниже** минимума

### Примеры

| T (°C) | H (%) | Пороги T | Пороги H | Действие |
|--------|-------|----------|----------|----------|
| 30     | 55    | 18-28    | 40-70    | Открыть окно (T высокая) |
| 22     | 75    | 18-28    | 40-70    | Открыть окно (H высокая) |
| 15     | 55    | 18-28    | 40-70    | Закрыть окно (T низкая) |
| 22     | 35    | 18-28    | 40-70    | Закрыть окно (H низкая) |
| 22     | 55    | 18-28    | 40-70    | Без изменений (норма) |

## Режим симуляции

В режиме симуляции данные генерируются по реалистичным функциям:

- **Суточный цикл**: Температура выше днем, ниже ночью
- **Обратная зависимость**: Когда температура растет, влажность падает
- **Шум**: Добавляется случайный шум для реалистичности
- **Ускоренное время**: 10 минут = 1 сутки (для демонстрации)

### Настройка симуляции

В [config.py](config.py):

```python
SIM_TEMP_BASE = 22.0          # Средняя температура
SIM_TEMP_AMPLITUDE = 6.0      # Амплитуда колебаний
SIM_TEMP_NOISE = 0.5          # Уровень шума

SIM_HUMIDITY_BASE = 55.0      # Средняя влажность
SIM_HUMIDITY_AMPLITUDE = 15.0 # Амплитуда колебаний
SIM_HUMIDITY_NOISE = 2.0      # Уровень шума
```

## Конфигурация

Все настройки в файле [config.py](config.py):

```python
class Config:
    # Режим работы
    SENSOR_MODE = 'simulated'  # или 'real'

    # Сервер
    HOST = '0.0.0.0'
    PORT = 5000
    DEBUG = True

    # GPIO пины
    DHT_SENSOR_PIN = 4         # GPIO4
    WINDOW_RELAY_PIN = 17      # GPIO17

    # Пороговые значения по умолчанию
    DEFAULT_TEMP_MIN = 18.0
    DEFAULT_TEMP_MAX = 28.0
    DEFAULT_HUMIDITY_MIN = 40.0
    DEFAULT_HUMIDITY_MAX = 70.0
```