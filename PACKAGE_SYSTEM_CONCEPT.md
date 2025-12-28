# Kashub Package System Concept

**Превращаем Kashub в экосистему библиотек и утилит (как npm для KHScript)**

---

## 📦 Типы пакетов

### 1. Library (Библиотека)
Набор функций/команд для использования в скриптах.

**Пример:** `kasper.math`, `kasper.world-tools`, `kasper.combat`

```javascript
// Использование
use "kasper.math"

let distance = kasper.math.distance(x1, y1, x2, y2)
let rounded = kasper.math.round(3.14159, 2)
```

### 2. Command (Команда)
Один CLI-подобный инструмент или утилита.

**Пример:** `auto-farm`, `mob-grinder`, `tree-chopper`

```bash
# Установка
kashub install auto-farm

# Использование в скрипте
autoFarm wheat 10 10
```

### 3. Preset (Пресет)
Набор конфигов/скриптов под конкретный режим.

**Пример:** `survival-starter`, `pvp-kit`, `minigame-lobby`

```bash
kashub install survival-starter
# Устанавливает набор скриптов для выживания
```

### 4. Extension (Расширение)
Добавляет новые команды в KHScript через Java API.

**Пример:** `redstone-api`, `worldedit-bridge`, `economy-api`

---

## 📋 Manifest (kashub.json)

```json
{
  "name": "kasper.world-tools",
  "version": "1.2.3",
  "type": "library",
  "description": "Утилиты для работы с миром",
  "author": "KasperStudios",
  "license": "MIT",
  
  "entry": "src/main.kh",
  "exports": {
    "buildCircle": "src/circle.kh",
    "buildSphere": "src/sphere.kh",
    "fillArea": "src/fill.kh"
  },
  
  "dependencies": {
    "kasper.math": "^2.0.0",
    "kasper.pathfinding": "~1.5.0"
  },
  
  "devDependencies": {
    "kashub-test": "^1.0.0"
  },
  
  "scripts": {
    "test": "kashub test",
    "build": "kashub pack"
  },
  
  "keywords": ["world", "building", "tools"],
  "repository": "https://github.com/kasper/world-tools",
  "homepage": "https://kashub.dev/packages/world-tools",
  
  "minKashubVersion": "0.8.0",
  "minecraftVersion": "1.21.1"
}
```

---

## 🔧 Синтаксис импорта/экспорта

### Экспорт функций (в библиотеке)

```javascript
// kasper.math/src/main.kh

// Экспорт функции
export fn distance(x1, y1, x2, y2) {
    let dx = x2 - x1
    let dy = y2 - y1
    return sqrt(dx * dx + dy * dy)
}

export fn round(value, decimals) {
    let multiplier = pow(10, decimals)
    return floor(value * multiplier) / multiplier
}

// Приватная функция (не экспортируется)
fn sqrt(x) {
    // Реализация
}
```

### Импорт в скрипте

```javascript
// Импорт всей библиотеки
use "kasper.math"

// Использование с неймспейсом
let dist = kasper.math.distance(0, 0, 10, 10)

// Импорт конкретных функций
use "kasper.math" { distance, round }

// Использование без неймспейса
let dist = distance(0, 0, 10, 10)

// Импорт с алиасом
use "kasper.math" as math

let dist = math.distance(0, 0, 10, 10)

// Импорт из подмодуля
use "kasper.world-tools/circle"

circle.build(100, 64, 200, 10, "stone")
```

---

## 📁 Структура папок

```
config/kashub/
├── packages/                    # Установленные пакеты
│   ├── kasper.math@2.1.0/
│   │   ├── kashub.json
│   │   ├── src/
│   │   │   ├── main.kh
│   │   │   ├── advanced.kh
│   │   │   └── utils.kh
│   │   └── README.md
│   │
│   ├── kasper.world-tools@1.2.3/
│   │   ├── kashub.json
│   │   ├── src/
│   │   │   ├── main.kh
│   │   │   ├── circle.kh
│   │   │   ├── sphere.kh
│   │   │   └── fill.kh
│   │   └── examples/
│   │       └── demo.kh
│   │
│   └── auto-farm@3.0.1/
│       ├── kashub.json
│       ├── farm.kh
│       └── config.json
│
├── package-lock.json            # Зафиксированные версии
├── scripts/                     # Пользовательские скрипты
└── config.json                  # Конфиг мода
```

---

## 🛠️ CLI команды

### Установка пакетов

```bash
# Установить пакет
kashub install kasper.math

# Установить конкретную версию
kashub install kasper.math@2.1.0

# Установить из GitHub
kashub install github:kasper/math-lib

# Установить локально (для разработки)
kashub install ./my-package

# Установить несколько
kashub install kasper.math kasper.world-tools auto-farm
```

### Обновление

```bash
# Обновить пакет
kashub update kasper.math

# Обновить все
kashub update

# Проверить устаревшие
kashub outdated
```

### Удаление

```bash
# Удалить пакет
kashub remove kasper.math

# Удалить с зависимостями
kashub remove kasper.math --cascade
```

### Информация

```bash
# Список установленных
kashub list

# Дерево зависимостей
kashub list --tree

# Информация о пакете
kashub info kasper.math

# Поиск пакетов
kashub search "world tools"
```

### Разработка

```bash
# Создать новый пакет
kashub init my-package

# Собрать пакет
kashub pack

# Опубликовать
kashub publish

# Запустить тесты
kashub test

# Линтер
kashub lint
```

---

## 🌐 Marketplace API

### Поиск пакетов

```
GET /api/packages/search?q=world&type=library
```

**Response:**
```json
{
  "results": [
    {
      "name": "kasper.world-tools",
      "version": "1.2.3",
      "type": "library",
      "description": "Утилиты для работы с миром",
      "author": "KasperStudios",
      "downloads": 15420,
      "rating": 4.8,
      "verified": true,
      "tags": ["world", "building", "tools"]
    }
  ],
  "total": 1
}
```

### Информация о пакете

```
GET /api/packages/kasper.world-tools
```

### Скачивание

```
GET /api/packages/kasper.world-tools/1.2.3/download
```

### Публикация

```
POST /api/packages/publish
Authorization: Bearer <token>
Content-Type: multipart/form-data

{
  "package": <package.tar.gz>,
  "manifest": <kashub.json>
}
```

---

## 🔐 Версионирование (Semver)

```
^1.2.3  - Совместимо с 1.x.x (>=1.2.3 <2.0.0)
~1.2.3  - Совместимо с 1.2.x (>=1.2.3 <1.3.0)
1.2.3   - Точная версия
>=1.2.3 - Больше или равно
<2.0.0  - Меньше
*       - Любая версия
```

---

## 🔄 Резолвинг зависимостей

### Алгоритм

1. Читаем `kashub.json` пакета
2. Проверяем `dependencies`
3. Для каждой зависимости:
   - Проверяем установлена ли нужная версия
   - Если нет - скачиваем и устанавливаем
   - Рекурсивно резолвим зависимости зависимостей
4. Создаём `package-lock.json` с зафиксированными версиями

### Конфликты версий

```
Package A требует kasper.math ^2.0.0
Package B требует kasper.math ^2.1.0

Решение: Устанавливаем 2.1.0 (удовлетворяет обоим)
```

```
Package A требует kasper.math ^2.0.0
Package B требует kasper.math ^3.0.0

Решение: ОШИБКА - несовместимые версии
```

---

## 📝 Пример: Создание библиотеки

### 1. Инициализация

```bash
kashub init kasper.combat-utils
cd kasper.combat-utils
```

### 2. Структура

```
kasper.combat-utils/
├── kashub.json
├── README.md
├── LICENSE
├── src/
│   ├── main.kh
│   ├── targeting.kh
│   └── combos.kh
├── examples/
│   └── pvp-bot.kh
└── tests/
    └── targeting.test.kh
```

### 3. Код (src/targeting.kh)

```javascript
// Экспорт функции поиска ближайшего врага
export fn findNearestEnemy(range) {
    vision nearest hostile range
    return $nearest_entity
}

// Экспорт функции проверки в зоне атаки
export fn isInAttackRange(entity, range) {
    let dist = distance($PLAYER_X, $PLAYER_Y, $PLAYER_Z, 
                       entity.x, entity.y, entity.z)
    return dist <= range
}

// Приватная функция
fn distance(x1, y1, z1, x2, y2, z2) {
    let dx = x2 - x1
    let dy = y2 - y1
    let dz = z2 - z1
    return sqrt(dx*dx + dy*dy + dz*dz)
}
```

### 4. Использование

```javascript
// pvp-script.kh
use "kasper.combat-utils/targeting"

loop {
    let enemy = targeting.findNearestEnemy(20)
    
    if (enemy != null && targeting.isInAttackRange(enemy, 5)) {
        attack 5 hostile 1
        wait 500
    }
    
    wait 100
}
```

### 5. Публикация

```bash
kashub pack
kashub publish
```

---

## 🎯 Интеграция в Roadmap

### v1.1.0 - Marketplace & Community
- ✅ Базовый Package Manager
- ✅ `kashub install/update/remove`
- ✅ GitHub-based repository
- ✅ Поиск и фильтрация

### v1.2.0 - Package System Extended
- ✅ Типы пакетов (library/command/preset/extension)
- ✅ Система импорта/экспорта (`use`, `export`)
- ✅ Неймспейсы
- ✅ Резолвинг зависимостей
- ✅ Semver версионирование
- ✅ `package-lock.json`

### v1.3.0 - Developer Tools
- ✅ `kashub init` - создание пакета
- ✅ `kashub pack` - сборка
- ✅ `kashub publish` - публикация
- ✅ `kashub test` - тестирование
- ✅ `kashub lint` - проверка кода
- ✅ SDK для создания расширений

### v2.0.0 - Extension API
- ✅ Java API для создания команд
- ✅ Плагин-система
- ✅ Hot-reload расширений
- ✅ Sandbox для безопасности

---

## 💡 Примеры пакетов

### kasper.math
```javascript
export fn abs(x) { return x < 0 ? -x : x }
export fn min(a, b) { return a < b ? a : b }
export fn max(a, b) { return a > b ? a : b }
export fn clamp(x, min, max) { return min(max(x, min), max) }
export fn lerp(a, b, t) { return a + (b - a) * t }
```

### kasper.world-tools
```javascript
export fn buildCircle(x, y, z, radius, block) {
    for (angle = 0; angle < 360; angle += 5) {
        let px = x + radius * cos(angle)
        let pz = z + radius * sin(angle)
        placeBlock px y pz block
    }
}

export fn fillArea(x1, y1, z1, x2, y2, z2, block) {
    for (x = x1; x <= x2; x++) {
        for (y = y1; y <= y2; y++) {
            for (z = z1; z <= z2; z++) {
                placeBlock x y z block
            }
        }
    }
}
```

### auto-farm (command)
```javascript
// Простая команда-утилита
export fn autoFarm(crop, width, length) {
    let startX = $PLAYER_X
    let startZ = $PLAYER_Z
    
    for (x = 0; x < width; x++) {
        for (z = 0; z < length; z++) {
            moveTo startX + x $PLAYER_Y startZ + z
            interact block
            wait 100
        }
    }
}
```

---

## 🔒 Безопасность

### Sandbox для пакетов

```json
{
  "permissions": {
    "fileSystem": false,
    "network": false,
    "commands": ["placeBlock", "breakBlock", "moveTo"],
    "maxMemory": "10MB",
    "maxCpu": "50%"
  }
}
```

### Подпись пакетов

```bash
kashub sign my-package
# Создаёт my-package.sig с цифровой подписью
```

### Проверка

```bash
kashub verify kasper.math
# Проверяет подпись автора
```

---

## 📊 Статистика

### Для авторов

```bash
kashub stats kasper.math

Downloads: 15,420
Rating: 4.8/5.0 (342 reviews)
Dependents: 87 packages
Last updated: 2 days ago
```

### Для пользователей

```bash
kashub audit

Found 3 vulnerabilities:
  - kasper.old-lib@1.0.0 (high severity)
  - deprecated-package@2.1.0 (medium)
  
Run 'kashub audit fix' to update
```

---

**Это превратит Kashub в полноценную экосистему!** 🚀
