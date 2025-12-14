# KHScript Audit Report & Development Plan

**Дата:** Декабрь 2025
**Версия проекта:** Kashub для Minecraft 1.21+ Fabric  
**Автор аудита:** AI Assistant

---

## Этап 1: Аудит текущего функционала

### 1.1 Общая структура проекта

```
kashub/
├── algorithm/
│   ├── Command.java              # Интерфейс команд
│   ├── CommandRegistry.java      # Реестр всех команд (39 команд)
│   ├── ScriptInterpreter.java    # Главный интерпретатор (~745 строк)
│   ├── EnvironmentVariable.java  # Переменные окружения
│   ├── Function.java             # Пользовательские функции
│   └── commands/                 # 39 команд
├── api/
│   └── VisionAPI.java            # API для raycast и сканирования
├── config/
│   └── KashubConfig.java         # Конфигурация мода
├── gui/
│   ├── CodeCompletionManager.java # Автодополнение кода
│   ├── editor/                   # Редактор скриптов
│   ├── dialogs/                  # Диалоги
│   ├── theme/                    # Темы редактора
│   └── widgets/                  # UI виджеты
└── ...
```

### 1.2 Зарегистрированные команды (39 шт.)

| Категория | Команды |
|-----------|---------|
| **Движение** | `moveTo`, `runTo`, `pathfind`, `jump`, `sneak`, `sprint`, `swim`, `teleport` |
| **Бой** | `attack`, `lookAt` |
| **Инвентарь** | `inventory`, `selectSlot`, `equipArmor`, `dropItem`, `eat`, `useItem` |
| **Мир** | `breakBlock`, `placeBlock`, `getBlock`, `scan` |
| **Зрение** | `vision` |
| **Утилиты** | `print`, `log`, `chat`, `wait`, `sound` |
| **Управление** | `loop`, `onEvent`, `stop`, `loadScript`, `scripts` |
| **Сеть** | `http` |
| **AI** | `ai` |
| **Читы** | `speedHack`, `fullBright`, `setHealth` |
| **Анимации** | `animation` |
| **Ввод** | `input`, `interact` |

### 1.3 Модуль Pathfinding

**Файл:** `PathfindCommand.java` (318 строк)

**Текущий функционал:**
- ✅ A* алгоритм поиска пути
- ✅ Базовое избегание препятствий
- ✅ Поддержка `sethome` / `home`
- ✅ Переменные: `$pathfind_active`, `$pathfind_success`, `$pathfind_length`
- ✅ Tick-based движение по пути
- ✅ Автоматический прыжок при подъёме
- ✅ Автоспринт на дальних дистанциях

**Проблемы (Priority):**

| Приоритет | Проблема |
|-----------|----------|
| **HIGH** | `maxIterations = 100` — слишком мало для длинных путей |
| **HIGH** | Нет проверки опасных блоков (лава, кактусы, огонь) |
| **MEDIUM** | Нет кэширования путей |
| **MEDIUM** | Синхронное вычисление — может вызвать фризы |
| **LOW** | Нет поддержки плавания |
| **LOW** | Нет настройки максимальной высоты падения |

### 1.4 Модуль сканирования блоков

**Файл:** `ScanCommand.java` (425 строк)

**Текущий функционал:**
- ✅ `scan blocks <filter> <radius>` — сканирование блоков
- ✅ `scan view <filter> <distance>` — сканирование в направлении взгляда
- ✅ `scan nearest <filter> <radius>` — поиск ближайшего блока
- ✅ `scan ores <radius>` — поиск руд с приоритизацией
- ✅ `scan count <filter> <radius>` — подсчёт блоков
- ✅ Wildcard фильтры (`*_ore`, `diamond*`)
- ✅ Проверка видимости (raycast) при `allowCheats = false`
- ✅ Переменные: `$scan_count`, `$scan_found`, `$scan_nearest_*`

**Проблемы:**

| Приоритет | Проблема |
|-----------|----------|
| **MEDIUM** | Радиус ограничен 32 блоками — может быть мало |
| **MEDIUM** | Нет асинхронного сканирования |
| **LOW** | Нет кэширования результатов |
| **LOW** | Нет фильтрации по Y-диапазону |
| **LOW** | Нет сортировки результатов |

### 1.5 Модуль сканирования сущностей

**Файл:** `VisionCommand.java` (261 строка)

**Текущий функционал:**
- ✅ `vision target` — что под прицелом
- ✅ `vision scan <radius> [filter]` — сканирование сущностей
- ✅ `vision nearest <type> <dist>` — ближайшая сущность
- ✅ `vision count <type> <radius>` — подсчёт сущностей
- ✅ `vision isLookingAt <type> <id>` — проверка взгляда
- ✅ Фильтры: `hostile`, `living`, `all`, конкретный тип
- ✅ Переменные: `$nearest_*`, `$scan_*`, `$target_*`

**Проблемы:**

| Приоритет | Проблема |
|-----------|----------|
| **LOW** | Нет фильтрации по здоровью |
| **LOW** | Нет проверки `hasAI` |
| **LOW** | Hardcoded список hostile мобов |

### 1.6 Управляющие конструкции

**Файл:** `ScriptInterpreter.java`

**Текущий функционал:**
- ✅ `if (condition) { ... }` с `else`
- ✅ `while (condition) { ... }`
- ✅ `for (init; condition; increment) { ... }`
- ✅ `loop N { ... }` (через LoopCommand)
- ✅ Логические операторы: `&&`, `||`
- ✅ Операторы сравнения: `==`, `!=`, `>`, `<`, `>=`, `<=`
- ✅ Арифметика: `+`, `-`, `*`, `/`, `%`
- ✅ Пользовательские функции: `function name(params) { ... }`

**Проблемы:**

| Приоритет | Проблема |
|-----------|----------|
| **CRITICAL** | Нет типизации — все значения строки |
| **HIGH** | Нет `break` / `continue` в циклах (только через LoopCommand) |
| **MEDIUM** | Нет вложенных условий `else if` |
| **LOW** | Нет тернарного оператора |

### 1.7 Переменные окружения

**Текущие переменные:**

| Категория | Переменные |
|-----------|------------|
| **Игрок** | `$PLAYER_NAME`, `$PLAYER_X/Y/Z`, `$PLAYER_YAW/PITCH`, `$PLAYER_HEALTH`, `$PLAYER_FOOD`, `$PLAYER_XP`, `$PLAYER_SPEED`, `$IS_SNEAKING`, `$IS_SPRINTING` |
| **Мир** | `$WORLD_TIME`, `$WORLD_DAY`, `$WORLD_WEATHER`, `$WORLD_DIFFICULTY`, `$GAME_MODE`, `$DIMENSION` |
| **Скрипт** | `$SCRIPT_NAME`, `$SCRIPT_PATH`, `$SCRIPT_DIR` |

### 1.8 Автодополнение кода

**Файл:** `CodeCompletionManager.java` (204 строки)

**Текущий функционал:**
- ✅ Автодополнение команд
- ✅ Автодополнение переменных окружения (`$PLAYER_*`)
- ✅ Автодополнение пользовательских переменных
- ✅ Сниппеты: `if`, `ifelse`, `while`, `for`, `function`, `loop`
- ✅ Описания команд и параметров

**Проблемы:**

| Приоритет | Проблема |
|-----------|----------|
| **HIGH** | Нет триггера по Tab (только при наборе) |
| **MEDIUM** | Нет Ctrl+Z / Undo |
| **MEDIUM** | Нет механизма черновиков |
| **LOW** | Нет автодополнения для подкоманд |

---

## Сводка найденных проблем

### CRITICAL (требуют немедленного исправления)
1. **Отсутствие типизации** — все значения строки, нет проверок типов

### HIGH (важные улучшения)
1. Pathfinding: малый лимит итераций (100)
2. Pathfinding: нет проверки опасных блоков
3. Нет `break`/`continue` в интерпретаторе
4. Нет триггера автодополнения по Tab

### MEDIUM (желательные улучшения)
1. Pathfinding: нет кэширования, синхронное вычисление
2. Сканеры: нет асинхронности, ограниченный радиус
3. Нет Ctrl+Z и черновиков в редакторе
4. Нет `else if`

### LOW (косметические улучшения)
1. Нет плавания в pathfinding
2. Нет фильтрации по Y-диапазону в сканерах
3. Hardcoded список hostile мобов

---

## Этап 2-6: План разработки

### Этап 2: Авто-торговля + Типизация

**Новые файлы:**
- `AutoTradeCommand.java` — команда авто-торговли
- `TypeSystem.java` — система типов
- `TypeChecker.java` — проверка типов

**API:**
```javascript
autoTrade.config({
  radius: 32,
  targetItems: ["diamond", "enchanted_book"],
  maxEmeraldsPerTrade: 10,
  priorityMode: "cheapest"
})
autoTrade.start()
autoTrade.stop()
```

### Этап 3: Авто-крафтинг + Улучшение редактора

**Новые файлы:**
- `AutoCraftCommand.java` — команда авто-крафтинга
- `RecipeManager.java` — менеджер рецептов
- `DraftManager.java` — менеджер черновиков

**API:**
```javascript
autoCraft.recipe({
  item: "diamond_pickaxe",
  count: 5,
  useTable: true,
  autoGather: false
})
autoCraft.start()
```

### Этап 4: Улучшение Pathfinding

**Изменения в `PathfindCommand.java`:**
- Увеличить `maxIterations` до 1000+
- Добавить проверку опасных блоков
- Асинхронное вычисление
- Кэширование путей
- Поддержка плавания

**Новый API:**
```javascript
pathfind.to(x, y, z, {
  avoidDanger: true,
  allowParkour: false,
  maxFallDistance: 3,
  sprint: true
})
```

### Этап 5: Улучшение сканеров

**Изменения:**
- Асинхронное сканирование
- Spatial indexing (chunk-based)
- Кэширование результатов
- Расширенные фильтры

**Новый API:**
```javascript
scanner.blocks({
  types: ["diamond_ore", "ancient_debris"],
  radius: 64,
  yRange: [0, 16],
  excludeAir: true,
  sortBy: "distance"
})

scanner.entities({
  types: ["villager", "wandering_trader"],
  radius: 50,
  healthMin: 20,
  hasAI: true
})
```

### Этап 6: Тестирование и документация

**Тесты:**
- Unit-тесты для каждого модуля
- Интеграционные тесты
- Целевое покрытие: 70%+

**Документация:**
- Обновить `README.md`
- Обновить `docs/KHScriptGuide.md`
- Добавить API-справочник
- Добавить примеры скриптов

---

## Статус реализации

1. ✅ **Этап 1: Аудит** — Завершён
2. ✅ **Этап 2: AutoTrade + Типизация** — Завершён
   - `AutoTradeCommand.java` — полная реализация
   - `KHType.java`, `TypedValue.java`, `TypeChecker.java` — система типов
3. ✅ **Этап 3: AutoCraft + Редактор** — Завершён
   - `AutoCraftCommand.java` — авто-крафтинг
   - `DraftManager.java` — система черновиков
   - `UndoManager.java` — Undo/Redo
4. ✅ **Этап 4: Pathfinding** — Завершён
   - Увеличен `maxIterations` до 2000
   - Добавлено избегание опасных блоков
   - Поддержка лестниц, лиан, плавания, паркура
   - Асинхронный поиск пути
   - Кэширование путей
5. ✅ **Этап 5: Сканеры** — Завершён
   - `ScannerCommand.java` — расширенный сканер
   - Асинхронное сканирование по чанкам
   - Фильтрация по Y-диапазону, здоровью, AI
   - Кэширование результатов
6. ✅ **Этап 6: Документация** — Завершён
   - Обновлён `docs/KHScriptGuide.md`
   - Добавлены примеры скриптов

---

## Новые файлы

### Система типов
- `algorithm/types/KHType.java` — базовые типы
- `algorithm/types/TypedValue.java` — обёртка значений
- `algorithm/types/TypeChecker.java` — проверка типов

### Новые команды
- `algorithm/commands/AutoTradeCommand.java` — авто-торговля
- `algorithm/commands/AutoCraftCommand.java` — авто-крафтинг
- `algorithm/commands/ScannerCommand.java` — расширенный сканер

### Редактор
- `gui/editor/DraftManager.java` — черновики
- `gui/editor/UndoManager.java` — Undo/Redo

### Примеры скриптов
- `assets/kashub/scripts/example_autotrade.kh`
- `assets/kashub/scripts/example_autocraft.kh`
- `assets/kashub/scripts/example_pathfind_advanced.kh`
- `assets/kashub/scripts/example_scanner_advanced.kh`

---

## Следующие шаги

1. **Тестирование в игре** — проверить все новые команды в Minecraft 1.21+
2. **Unit-тесты** — добавить тесты для TypeChecker, PathfindCommand
3. **Интеграция Undo/Redo** — подключить UndoManager к GUI редактора
4. **Интеграция черновиков** — подключить DraftManager к GUI
5. **Рекурсивный крафт** — автоматический крафт компонентов
