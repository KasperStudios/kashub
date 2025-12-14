# CrashGuard - Система защиты от крашей

CrashGuard — модуль защиты для KHScript, предотвращающий краши игры от некорректных скриптов.

## Проблемы, которые решает CrashGuard

| Проблема | Детектор | Действие |
|----------|----------|----------|
| Бесконечные циклы | Infinite Loop Detector | Прерывание цикла |
| Спам действий | Action Rate Limiter | Throttling |
| Высокая нагрузка CPU | CPU Time Monitor | Пауза скрипта |
| Утечки памяти | Memory Guard | Cleanup |
| Глубокая рекурсия | Recursion Depth Guard | Прерывание |

## Уровни строгости

| Уровень | CPU Limit | Actions/sec | Loop Iterations | Описание |
|---------|-----------|-------------|-----------------|----------|
| `off` | ∞ | ∞ | ∞ | Защита отключена |
| `loose` | 50ms | 50 | 10000 | Минимальные ограничения |
| `medium` | 10ms | 20 | 5000 | Баланс (по умолчанию) |
| `strict` | 5ms | 10 | 2000 | Строгие ограничения |
| `paranoid` | 2ms | 5 | 500 | Максимальная защита |

## Использование

### Команды в скриптах

```javascript
// Показать статус
crashGuard status

// Установить глобальную строгость
crashGuard config strictness=medium

// Установить строгость для скрипта
crashGuard strictness loose my_script.kh

// Временно отключить защиту (5 сек)
crashGuard pause 5000

// Экстренная остановка скрипта
crashGuard stop my_script.kh

// Очистить ресурсы
crashGuard cleanup

// Подробный отчёт
crashGuard report
```

### Директивы в скриптах

```javascript
// В начале скрипта можно указать настройки
#crashguard strictness=loose
#crashguard allowInfiniteLoops=true

// Ваш код...
loop {
    scanner blocks diamond_ore radius=64
    sleep 1000  // Важно! Добавляйте sleep в циклах
}
```

## Детекторы

### 1. CPU Time Monitor

Отслеживает время выполнения скрипта на каждом тике.

**Пороги:**
- Warning: >5ms
- Pause: >10ms (strict mode)
- Stop: >50ms

**Переменные:**
- `$crashGuard_cpuTotal` — общее CPU время

### 2. Action Rate Limiter

Ограничивает частоту действий (атаки, прыжки, взаимодействия).

**Лимиты по умолчанию:**
- `loose`: 50/сек
- `medium`: 20/сек
- `strict`: 5/сек

**Пример проблемного кода:**
```javascript
// ❌ Плохо - спам атак
loop { input attack }

// ✅ Хорошо - с задержкой
loop { 
    input attack 
    sleep 100
}
```

### 3. Infinite Loop Detector

Обнаруживает циклы без `sleep`/`wait`.

**Логика:**
1. Считает итерации за 10 секунд
2. Warning при >1000 итераций без yield
3. Break при >5000 итераций

**Пример:**
```javascript
// ❌ Будет прервано
while (true) { x = x + 1 }

// ✅ Работает
while (true) { 
    x = x + 1 
    sleep 10
}
```

### 4. Memory Guard

Контролирует рост памяти скрипта.

**Пороги:**
- Warning: >500KB/мин
- Cleanup: >2MB
- Stop: >10MB

### 5. Recursion Depth Guard

Защита от глубокой рекурсии.

**Лимит по умолчанию:** 100 уровней

```javascript
// ❌ Будет прервано
function recurse(n) { recurse(n + 1) }
recurse(1)

// ✅ С проверкой глубины
function recurse(n) { 
    if (n > 50) return
    recurse(n + 1) 
}
```

## HUD Мониторинг

CrashGuard отображает статус в HUD:

```
[CrashGuard Status]
Script: auto_farm.kh    CPU: 3.2ms/tick  Actions: 12/s  OK ✓
Script: kill_aura.kh    CPU: 8.1ms/tick  Actions: 45/s  WARNING ⚠️
Script: mega_miner.kh   Memory: 1.8MB    Loops: 1200/10s PAUSED ⏸️

Global: medium | CPU:14ms | Actions:32/s
```

**Иконки:**
- ✓ — OK
- ⚠ — Warning
- ⏸ — Paused
- ✗ — Stopped

## Логирование

Логи сохраняются в `logs/crashguard/<script_name>.log`:

```
[2025-12-09 17:03] WARNING cpu_limit_exceeded: 12.3ms > 10ms
[2025-12-09 17:04] INFO auto_paused: resuming in 1000ms
[2025-12-09 17:05] ACTION_RATE rate_limited: attack throttled (45/sec > 20/sec)
```

## Переменные окружения

| Переменная | Описание |
|------------|----------|
| `$crashGuard_strictness` | Текущий уровень строгости |
| `$crashGuard_cpuTotal` | Общее CPU время (ms) |
| `$crashGuard_actionsTotal` | Общее кол-во действий/сек |
| `$crashGuard_scriptCount` | Кол-во активных скриптов |
| `$crashGuard_paused` | Защита на паузе (true/false) |

## Интеграция с другими модулями

### AutoTrade
```javascript
autoTrade config throttle=true  // Авто-throttle торговли
```

### Scanner
```javascript
scanner blocks diamond_ore radius=64 cache=true  // Кэширование результатов
```

### Pathfind
```javascript
pathfind 100 64 200 timeout=5000 maxNodes=10000  // Лимиты поиска пути
```

## Рекомендации

1. **Всегда добавляйте `sleep` в циклах** — даже 10ms достаточно
2. **Используйте кэширование** для сканеров и pathfinding
3. **Ограничивайте радиус сканирования** — 64 блока обычно достаточно
4. **Проверяйте глубину рекурсии** в функциях
5. **Используйте `crashGuard status`** для мониторинга

## Отключение защиты

Для тестирования можно отключить защиту:

```javascript
crashGuard config strictness=off
```

⚠️ **Внимание:** Отключение защиты может привести к крашам игры!
