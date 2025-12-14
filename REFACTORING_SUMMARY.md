# KHScript Refactoring Summary

## Обзор изменений

Проведён масштабный рефакторинг языка KHScript для приближения синтаксиса к стилю Rust/JavaScript, улучшения безопасности и добавления AI-интеграции.

---

## 1. Новый синтаксис KHScript (Rust/JS-style)

### 1.1 Объявление переменных

**Новое:**
```javascript
let x = 5              // Мутабельная переменная
const MAX_RANGE = 64   // Иммутабельная константа
x = 10                 // Переприсваивание (работает только для let)
```

**Старое (всё ещё работает):**
```javascript
x = 5  // Legacy assignment
```

**Особенности:**
- `const` переменные нельзя переприсваивать (выбросит ошибку)
- `let` переменные можно переприсваивать
- Старый синтаксис без ключевых слов работает как `let`

### 1.2 Условные конструкции

**Rust-style (новое):**
```rust
if hp < 5 {
    chat "Low health!"
} else if hp < 10 {
    chat "Medium health"
} else {
    chat "Good health"
}
```

**Legacy style (всё ещё работает):**
```javascript
if (hp < 5) {
    chat "Low health!"
}
```

### 1.3 Циклы

**Rust-style:**
```rust
while hp > 0 {
    wait 1
}

for (let i = 0; i < 10; i++) {
    print $i
}
```

### 1.4 Тернарный оператор

```javascript
let hpColor = hp < 5 ? "red" : "green"
let status = isAlive ? "alive" : "dead"
```

### 1.5 Логические операторы

```javascript
// AND
if hp > 5 && food > 10 {
    sprint on
}

// OR
if hp < 5 || food < 3 {
    eat
}

// NOT
if !isMoving {
    moveTo 100 64 200
}
```

### 1.6 Приоритет операций

Теперь правильный приоритет как в JS:
1. Скобки `()`
2. Унарные: `!`, `-`
3. Мультипликативные: `*`, `/`, `%`
4. Аддитивные: `+`, `-`
5. Сравнение: `<`, `>`, `<=`, `>=`
6. Равенство: `==`, `!=`
7. Логическое AND: `&&`
8. Логическое OR: `||`
9. Тернарный: `? :`

**Примеры:**
```javascript
let result = 2 + 3 * 4        // = 14 (не 20!)
let check = (2 + 3) * 4       // = 20
let complex = hp > 5 && (food > 10 || hasPotion)
```

---

## 2. Новые классы

### 2.1 ExpressionParser.java

Полноценный парсер выражений с правильным приоритетом операций:
- Recursive descent parsing
- Поддержка всех операторов (арифметика, сравнение, логика)
- Тернарный оператор
- Скобки
- Переменные и константы

**Расположение:** `kasperstudios.kashub.algorithm.ExpressionParser`

### 2.2 VariableStore.java

Хранилище переменных с поддержкой `let`/`const`:
- Отслеживание типа переменной (let/const/legacy)
- Запрет переприсваивания const
- Поддержка вложенных scope (для будущих улучшений)

**Расположение:** `kasperstudios.kashub.algorithm.VariableStore`

### 2.3 KasHubAiClient.java

Мульти-провайдерный AI клиент:
- Поддержка Groq API
- Поддержка MegaLLM API
- Поддержка любых OpenAI-совместимых endpoints
- Автоматическое получение списка моделей
- Генерация системного промпта с контекстом KHScript

**Расположение:** `kasperstudios.kashub.services.KasHubAiClient`

---

## 3. Улучшенные команды

### 3.1 EvalCommand

**Добавлено:**
- ✅ Blacklist опасных операций (`Runtime.exec`, `System.exit`, `Files.delete`, и т.д.)
- ✅ Safe mode с whitelist пакетов (только `net.minecraft.*`, `java.util.*`, `kasperstudios.kashub.*`)
- ✅ Внутренняя статистика (счётчики вызовов, таймаутов, ошибок)
- ✅ Улучшенная обработка ошибок компиляции и runtime
- ✅ Чёткие сообщения об ошибках в чат
- ✅ Статистика через `EvalCommand.getStats()`

**Безопасность:**
```java
// Заблокированные операции:
Runtime.getRuntime().exec(...)  // ❌
System.exit(0)                   // ❌
Files.delete(...)                // ❌
ProcessBuilder                   // ❌
```

### 3.2 FullBrightCommand

**Исправлено:**
- ✅ Правильное сохранение оригинальной гаммы
- ✅ Корректное восстановление при отключении
- ✅ Защита от повторного сохранения
- ✅ Метод `forceDisable()` для очистки при остановке скрипта
- ✅ Feedback в чат с цветовой индикацией

**Использование:**
```javascript
fullbright on      // Включить
fullbright off     // Выключить
fullbright toggle  // Переключить
```

### 3.3 SpeedHackCommand

**Исправлено:**
- ✅ Правильное сохранение оригинальной скорости
- ✅ Безопасные лимиты (0.1x - 10.0x)
- ✅ Применение множителя к оригинальной скорости (не к текущей!)
- ✅ Метод `forceDisable()` для очистки
- ✅ Детальная справка и примеры

**Использование:**
```javascript
speed 2.0   // Удвоить скорость
speed 0.5   // Половинная скорость
speed off   // Вернуть нормальную
```

### 3.4 SetHealthCommand

**Улучшено:**
- ✅ Валидация диапазона (0.0 - 20.0)
- ✅ Автоматический clamp значений
- ✅ Улучшенные сообщения об ошибках
- ✅ Feedback в чат
- ✅ Детальная справка

---

## 4. Обновлённый ScriptInterpreter

### 4.1 Новые паттерны

```java
// Поддержка let/const
LET_PATTERN = "^\\s*let\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$"
CONST_PATTERN = "^\\s*const\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$"

// Поддержка Rust-style и legacy
IF_PATTERN = "^\\s*if\\s+(.+?)\\s*\\{\\s*$|^\\s*if\\s*\\((.*)\\)\\s*\\{?\\s*$"
WHILE_PATTERN = "^\\s*while\\s+(.+?)\\s*\\{\\s*$|^\\s*while\\s*\\((.*)\\)\\s*\\{?\\s*$"

// Поддержка fn и function
FUNCTION_PATTERN = "^\\s*(?:fn|function)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\((.*?)\\)\\s*\\{?\\s*$"
```

### 4.2 Новые методы

```java
// Использует ExpressionParser для вычисления
private String evaluateExpression(String expression)

// Использует ExpressionParser для условий
private boolean evaluateCondition(String condition)

// Резолвит переменные из всех источников
private String resolveVariable(String name)
```

---

## 5. Конфигурация AI

### 5.1 Новые настройки в KashubConfig

```java
public enum AiProvider { OFF, GROQ, MEGALLM, CUSTOM }

public AiProvider aiProvider = AiProvider.OFF;
public String aiApiKey = "";
public String aiBaseUrl = "";           // Пусто = дефолт провайдера
public String aiModel = "";             // Пусто = первая доступная
public boolean aiEnableTools = true;    // Включить tools для AI
public boolean aiEnableGui = true;      // Включить GUI чат
public int aiMaxTokens = 2000;
public double aiTemperature = 0.7;
public int aiContextMaxLength = 8000;
```

### 5.2 Настройка провайдеров

**Groq:**
```json
{
  "aiProvider": "GROQ",
  "aiApiKey": "gsk_...",
  "aiModel": "mixtral-8x7b-32768"
}
```

**MegaLLM:**
```json
{
  "aiProvider": "MEGALLM",
  "aiApiKey": "your-key",
  "aiModel": "gpt-4"
}
```

**Custom (любой OpenAI-совместимый):**
```json
{
  "aiProvider": "CUSTOM",
  "aiApiKey": "your-key",
  "aiBaseUrl": "https://your-api.com/v1",
  "aiModel": "your-model"
}
```

---

## 6. Примеры использования

### 6.1 Новый синтаксис

```javascript
// Константы и переменные
const FARM_X = 100
const FARM_Z = 200
let currentY = 64

// Тернарный оператор
let action = hp < 10 ? "heal" : "continue"

// Сложные условия
if (hp < 5 || food < 3) && !hasPotion {
    chat "Emergency! Need supplies"
    moveTo $SPAWN_X $SPAWN_Y $SPAWN_Z
}

// Выражения с приоритетом
let damage = baseDamage * (1 + critChance / 100)
let isLowResources = (hp < maxHp * 0.3) || (food < 5)

// Функции с новым синтаксисом
function checkStatus() {
    let hpPercent = hp / 20 * 100
    let status = hpPercent > 75 ? "good" : hpPercent > 25 ? "medium" : "critical"
    print "Status: $status"
}
```

### 6.2 Использование AI

```java
// В коде
KasHubAiClient client = KasHubAiClient.getInstance();
Map<String, String> context = new HashMap<>();
context.put("current_script", "autofarm.kh");
context.put("player_hp", "15.5");

String response = client.generateResponse(
    "Создай скрипт для автоматической добычи шалкеров",
    context
);
```

---

## 7. Что НЕ реализовано (для будущих версий)

### 7.1 AI GUI Chat Interface
- Отдельное окно чата с KasHub Agent
- История сообщений
- Кнопки быстрых действий

### 7.2 AI Tools
- `open_script(name)` - открыть скрипт
- `save_script(name, content)` - сохранить
- `run_script(name)` - запустить
- `list_scripts()` - список скриптов

### 7.3 AutoShoot Commands
- `autoShoot` для лука/арбалета/трезубца
- Автонаведение на цели
- Предсказание движения

### 7.4 Улучшенный AICommand
Текущий `AICommand` остался без изменений. Для полной реализации нужно:
- Переделать в GUI-интерфейс
- Добавить tools для манипуляции скриптами
- Интегрировать с `KasHubAiClient`

---

## 8. Миграция существующих скриптов

### 8.1 Обратная совместимость

✅ **Все старые скрипты продолжат работать!**

Старый синтаксис полностью поддерживается:
```javascript
// Старое - работает
x = 5
if (x > 3) {
    print "yes"
}

// Новое - тоже работает
let x = 5
if x > 3 {
    print "yes"
}
```

### 8.2 Рекомендации по обновлению

1. **Используйте `const` для констант:**
   ```javascript
   // Было
   MAX_RANGE = 64
   
   // Стало
   const MAX_RANGE = 64
   ```

2. **Используйте `let` для переменных:**
   ```javascript
   // Было
   counter = 0
   
   // Стало
   let counter = 0
   ```

3. **Упростите условия (опционально):**
   ```javascript
   // Было
   if (hp < 5) {
   
   // Можно
   if hp < 5 {
   ```

4. **Используйте тернарный оператор:**
   ```javascript
   // Было
   if hp < 10 {
       status = "low"
   } else {
       status = "ok"
   }
   
   // Стало
   let status = hp < 10 ? "low" : "ok"
   ```

---

## 9. Безопасность

### 9.1 EvalCommand Security

**Blacklist (всегда активен):**
- Runtime.getRuntime()
- System.exit
- ProcessBuilder
- Files.delete/write/move/copy
- FileOutputStream/FileWriter
- Class.forName
- System.setSecurityManager/setProperty
- Unsafe, sun.misc

**Safe Mode (если `sandboxMode = true`):**
Разрешены только пакеты:
- `net.minecraft.*`
- `java.util.*`
- `java.lang.Math`
- `java.lang.String`
- `kasperstudios.kashub.*`

### 9.2 Рекомендации

1. Включите `sandboxMode` в конфиге
2. Не давайте `allowEval = true` ненадёжным пользователям
3. Используйте CrashGuard для мониторинга eval вызовов

---

## 10. Тестирование

### 10.1 Тесты для нового синтаксиса

```javascript
// Тест 1: let/const
let x = 5
const Y = 10
x = 7        // OK
// Y = 20    // ERROR: Cannot reassign const

// Тест 2: Тернарный
let result = 5 > 3 ? "yes" : "no"
print $result  // "yes"

// Тест 3: Приоритет операций
let calc = 2 + 3 * 4
print $calc    // 14 (не 20)

// Тест 4: Сложные условия
if (5 > 3 && 10 < 20) || false {
    print "Complex condition works"
}

// Тест 5: Rust-style if
if 5 > 3 {
    print "Rust style works"
}
```

### 10.2 Проверка команд

```javascript
// FullBright
fullbright on
wait 2
fullbright off

// Speed
speed 2.0
wait 5
speed off

// SetHealth
setHealth 10
wait 1
setHealth 20
```

---

## 11. Производительность

### 11.1 ExpressionParser

- Recursive descent - O(n) где n = длина выражения
- Кэширование не требуется для коротких выражений
- Для длинных выражений (>1000 символов) может быть медленнее старого парсера

### 11.2 VariableStore

- HashMap lookup - O(1)
- Минимальный overhead по сравнению со старым подходом

---

## 12. Известные ограничения

1. **Else if chain:** Работает, но может быть медленнее для очень длинных цепочек (>50 else if)
2. **Expression parsing:** Не поддерживает функции в выражениях (например, `Math.sqrt(x)`)
3. **AI streaming:** Пока не реализован (только batch responses)
4. **AI tools:** Не реализованы (нужно добавить в будущем)

---

## 13. Roadmap (будущие улучшения)

### Приоритет 1 (критично)
- [ ] AI GUI Chat Interface
- [ ] AI Tools для манипуляции скриптами
- [ ] Обновление AICommand

### Приоритет 2 (важно)
- [ ] AutoShoot команды
- [ ] Streaming responses для AI
- [ ] Улучшенная обработка ошибок в ScriptTask

### Приоритет 3 (желательно)
- [ ] Функции в выражениях (`Math.sqrt`, `String.length`)
- [ ] Деструктуризация (`let [x, y, z] = position`)
- [ ] Spread operator (`...args`)
- [ ] Arrow functions (`let f = (x) => x * 2`)

---

## 14. Контакты и поддержка

При возникновении проблем:
1. Проверьте логи в `logs/kashub.log`
2. Включите debug logging в конфиге
3. Проверьте CrashGuard события

**Основные файлы для отладки:**
- `ScriptInterpreter.java` - парсинг и выполнение
- `ExpressionParser.java` - вычисление выражений
- `VariableStore.java` - хранение переменных
- `KasHubAiClient.java` - AI интеграция

---

**Версия:** 1.0.0  
**Дата:** 2024-12-10  
**Автор:** KasperStudios + AI Assistant
