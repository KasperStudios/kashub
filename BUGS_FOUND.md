# Найденные баги в Kashub

## 🔴 КРИТИЧНЫЕ БАГИ

### 1. Функции - Множественные проблемы

#### Проблема 1.1: Вызов функции конфликтует с командами
```java
// ScriptInterpreter.java:195
Matcher funcCallMatcher = FUNCTION_CALL_PATTERN.matcher(line);
if (funcCallMatcher.find()) {
    String funcName = funcCallMatcher.group(1);
    // ...
}
```

**Баг:** Паттерн `([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(` матчит ВСЕ, включая команды!

**Пример:**
```javascript
print("Hello")  // Думает что это функция, а не команда!
moveTo(100, 64, 200)  // Тоже!
```

**Решение:** Проверять функции ПОСЛЕ команд, или проверять что это не команда.

---

#### Проблема 1.2: Return не обрабатывается
```java
// Execute function body
parseCommands(func.getBody());
```

**Баг:** `return` не останавливает выполнение и не возвращает значение!

**Пример:**
```javascript
fn add(a, b) {
    return a + b  // Это игнорируется!
}
let result = add(5, 3)  // result будет undefined
```

**Решение:** Нужен механизм для return value и early exit.

---

#### Проблема 1.3: Аргументы не вычисляются
```java
for (int j = 0; j < params.size() && j < arguments.size(); j++) {
    variables.put(params.get(j), arguments.get(j));  // Просто строка!
}
```

**Баг:** Аргументы передаются как строки, не вычисляются!

**Пример:**
```javascript
let x = 5
let y = 3
fn add(a, b) {
    return a + b
}
add(x + 1, y * 2)  // Передаст "x + 1" и "y * 2" как строки!
```

**Решение:** Вычислять аргументы перед передачей.

---

#### Проблема 1.4: Scope не изолирован
```java
// Save current variable values
Map<String, String> oldVars = new HashMap<>(variables);
// ...
// Restore variable values
variables.clear();
variables.putAll(oldVars);
```

**Баг:** Переменные внутри функции видны снаружи до restore!

**Пример:**
```javascript
let x = 10
fn test() {
    let x = 20  // Перезаписывает внешний x!
    print x
}
test()
print x  // Будет 20 вместо 10 (до restore)
```

**Решение:** Создавать новый scope для функции.

---

### 2. If/Else - Неправильная обработка

#### Проблема 2.1: Else блок не находится
```java
// ScriptInterpreter.java:280
Matcher ifMatcher = IF_PATTERN.matcher(line);
if (ifMatcher.find()) {
    // ...
    if (evaluateCondition(condition)) {
        parseCommands(ifBlock.toString());
    }
    continue;  // Пропускает else!
}
```

**Баг:** После if блока сразу continue, else не проверяется!

**Пример:**
```javascript
if (false) {
    print "A"
} else {
    print "B"  // Никогда не выполнится!
}
```

**Решение:** Проверять else после if блока.

---

### 3. While - Бесконечные циклы

#### Проблема 3.1: Переменные не обновляются
```java
while (evaluateCondition(condition)) {
    parseCommands(whileBlock.toString());
}
```

**Баг:** Environment variables не обновляются между итерациями!

**Пример:**
```javascript
while ($PLAYER_HEALTH < 20) {
    eat
    wait 2000
    // $PLAYER_HEALTH всё ещё старое значение!
}
```

**Решение:** Обновлять переменные перед каждой проверкой условия.

---

### 4. Variables - Scope проблемы

#### Проблема 4.1: Let/Const не изолированы
```java
variableStore.declareLet(varName, varValue);
variables.put(varName, varValue); // Legacy compatibility
```

**Баг:** Переменные в двух местах, может быть рассинхронизация!

**Решение:** Использовать только variableStore.

---

## 🟡 ВАЖНЫЕ БАГИ

### 5. Commands - Проверка существования

#### Проблема 5.1: Команды проверяются после функций
```java
// Check for function call
Matcher funcCallMatcher = FUNCTION_CALL_PATTERN.matcher(line);
if (funcCallMatcher.find()) {
    // ...
}

// Process regular commands
line = processVariables(line);
List<String> parts = parseArguments(line);
String commandName = parts.get(0).toLowerCase();
Command command = CommandRegistry.getCommand(commandName);
```

**Баг:** Если функция не найдена, код продолжается и пытается выполнить как команду!

**Решение:** Проверять команды ПЕРЕД функциями.

---

### 6. GUI - ModernTextArea

#### Проблема 6.1: Курсор теряется при скролле
```java
// ModernTextArea.java
// Нет синхронизации курсора со скроллом
```

**Баг:** При скролле курсор остаётся на старой позиции визуально.

**Решение:** Пересчитывать позицию курсора при скролле.

---

#### Проблема 6.2: Выделение не работает с Shift+Arrow
```java
// Нет обработки Shift+Arrow для выделения
```

**Баг:** Shift+Arrow не выделяет текст.

**Решение:** Добавить обработку Shift модификатора.

---

### 7. Hot-Reload - Race Conditions

#### Проблема 7.1: Reload во время выполнения
```java
// ScriptTask.java
public void reload(String newCode) {
    this.code = newCode;
    // Нет проверки что скрипт не выполняется!
}
```

**Баг:** Reload может произойти во время выполнения команды!

**Решение:** Проверять state перед reload.

---

## 🟢 MINOR БАГИ

### 8. Error Messages - Неинформативные

#### Проблема 8.1: Нет номеров строк в ошибках
```java
LOGGER.warn("Function not found: {}", funcName);
```

**Баг:** Не понятно где ошибка в коде!

**Решение:** Добавлять номер строки в сообщения.

---

### 9. Memory Leaks - Потенциальные утечки

#### Проблема 9.1: Functions не очищаются
```java
private final Map<String, Function> functions = new HashMap<>();
```

**Баг:** Функции накапливаются и не удаляются!

**Решение:** Очищать при reload/stop.

---

## 📝 План исправлений

### Приоритет 1 (Сейчас) - ✅ ИСПРАВЛЕНО
1. ✅ Исправить порядок проверки (команды → функции) - DONE
2. ✅ Добавить return value для функций - DONE
3. ✅ Вычислять аргументы функций - DONE
4. ✅ Исправить if/else обработку - DONE
5. ✅ Обновлять переменные в while - DONE
6. ✅ Изолировать scope функций - DONE

### Приоритет 2 (Следующая итерация) - ✅ ИСПРАВЛЕНО
1. ✅ Исправить GUI курсор - DONE (добавлена проверка горизонтальных границ)
2. ✅ Добавить Shift+Arrow выделение - DONE (уже было реализовано)
3. ✅ Исправить hot-reload race conditions - DONE (добавлена синхронизация и проверки состояния)

### Приоритет 3 (Потом) - ✅ ИСПРАВЛЕНО
1. ✅ Улучшить error messages - DONE (номера строк уже были добавлены)
2. ✅ Исправить memory leaks - DONE (добавлена очистка functions/variables при stop)
3. ⬜ Добавить unit tests

---

## 🧪 Тестовые случаи

### Функции
```javascript
// Тест 1: Простая функция
fn greet(name) {
    print "Hello, $name!"
}
greet("Steve")
// Ожидается: "Hello, Steve!"

// Тест 2: Return value
fn add(a, b) {
    return a + b
}
let result = add(5, 3)
print result
// Ожидается: "8"

// Тест 3: Вычисление аргументов
let x = 5
fn double(n) {
    return n * 2
}
let result = double(x + 1)
print result
// Ожидается: "12"

// Тест 4: Scope
let x = 10
fn test() {
    let x = 20
    print x
}
test()
print x
// Ожидается: "20" затем "10"

// Тест 5: Рекурсия
fn factorial(n) {
    if (n <= 1) {
        return 1
    }
    return n * factorial(n - 1)
}
print factorial(5)
// Ожидается: "120"
```

### If/Else
```javascript
// Тест 1: Простой else
if (false) {
    print "A"
} else {
    print "B"
}
// Ожидается: "B"

// Тест 2: Else if
let x = 5
if (x < 0) {
    print "Negative"
} else if (x == 0) {
    print "Zero"
} else {
    print "Positive"
}
// Ожидается: "Positive"
```

### While
```javascript
// Тест: Переменные обновляются
let counter = 0
while (counter < 5) {
    counter++
    print counter
}
// Ожидается: 1, 2, 3, 4, 5
```

---

## 🔴 КРИТИЧНЫЕ БАГИ (НОВЫЕ)

### 10. Events - Не работают после stop/restart ✅ ИСПРАВЛЕНО

#### Проблема 10.1: EventManager.clear() вызывается глобально
```java
// StopCommand.java
case "all":
default:
    ScriptInterpreter.getInstance().stopProcessing();
    EventManager.getInstance().clear();  // ❌ Удаляет ВСЕ события!
```

**Баг:** При нажатии Z (stop hotkey) вызывается `EventManager.clear()` который удаляет ВСЕ события глобально, включая события которые только что были зарегистрированы при restart!

**Последовательность:**
1. Пользователь нажимает Z (stop)
2. `ScriptTask.restart()` вызывается
3. `restart()` → `stop()` → очищает `registeredEvents`
4. `restart()` → `parseAndQueue()` → регистрирует события заново
5. **НО** `StopCommand` всё ещё выполняется и вызывает `EventManager.clear()`
6. Все только что зарегистрированные события удаляются!

**Пример:**
```javascript
// test_events_restart.kh
onEvent onTick {
    print "Tick!"
}

// Первый запуск: ✅ Работает
// Stop (Z) + Restart: ❌ События не срабатывают
```

**Решение:** ✅ ИСПРАВЛЕНО
- Убрали `EventManager.clear()` из `StopCommand` для режима "all"
- Каждый `ScriptTask` теперь сам управляет своими событиями через `registeredEvents` Set
- При `stop()` скрипт вызывает `EventManager.unregisterEventScript()` для каждого своего события
- Режим `stop events` теперь только для глобальных обработчиков (не script-registered)

**Изменённые файлы:**
- `src/main/java/kasperstudios/kashub/algorithm/commands/StopCommand.java`
- `src/main/java/kasperstudios/kashub/services/runtime/ScriptTask.java` (уже был правильный cleanup)

**Тест:**
```javascript
// src/main/resources/assets/kashub/scripts/test_events_restart.kh
print "Event test script started"

onEvent onTick {
    print "Tick event fired!"
}

onEvent onHunger {
    print "Hunger changed: $event_food"
}

print "Events registered - press Z to stop, then restart to test"
```

---

**Начинаем исправления!** 🔧

---

## 🔴 КРИТИЧНЫЕ БАГИ (НОВЫЕ) - Продолжение

### 11. ScriptInterpreter - shouldStop блокирует события после stopAll() ✅ ИСПРАВЛЕНО

#### Проблема 11.1: shouldStop не сбрасывается для новых скриптов
```java
// ScriptInterpreter.java
public void stopProcessing() {
    shouldStop = true;  // ❌ Остаётся true навсегда!
    commandQueue.clear();
}

public void queueCommand(Command command, String[] args) {
    commandQueue.add(new CommandEntry(command, processedArgs));
    if (!isProcessing && !shouldStop) {  // ❌ Блокируется!
        processNextCommand();
    }
}
```

**Баг:** Когда пользователь нажимает Z (stop hotkey), вызывается `ScriptInterpreter.stopProcessing()` который устанавливает `shouldStop = true`. Этот флаг НЕ сбрасывается автоматически, и блокирует выполнение ВСЕХ последующих команд, включая события!

**Последовательность:**
1. Пользователь нажимает Z (stop)
2. `ScriptTaskManager.stopAll()` → `ScriptInterpreter.stopProcessing()`
3. `shouldStop = true` устанавливается
4. Скрипт перезапускается, события регистрируются
5. `EventManager.fireEvent()` → `interpreter.parseCommands()` → `queueCommand()`
6. **НО** `queueCommand()` проверяет `!shouldStop` и не выполняет команды!
7. События зарегистрированы, но не срабатывают

**Пример из логов:**
```
[20:25:28] Task 2 (test_events) stopped, all state cleared
[20:25:31] Started task 3: test_events
[20:25:31] Registered event handler for: onTick
// События зарегистрированы, но не срабатывают!
```

**Решение:** ✅ ИСПРАВЛЕНО
- Добавлена проверка в `queueCommand()`: если `shouldStop = true` И очередь пуста И не обрабатывается, сбросить флаг
- Добавлена аналогичная проверка в `executeQueuedCommands()`
- Теперь флаг автоматически сбрасывается когда начинается новая работа

**Изменённые файлы:**
- `src/main/java/kasperstudios/kashub/algorithm/ScriptInterpreter.java`

**Код исправления:**
```java
public void queueCommand(Command command, String[] args) {
    // Reset shouldStop flag if we're starting fresh (queue empty, not processing)
    if (shouldStop && !isProcessing && commandQueue.isEmpty()) {
        shouldStop = false;
    }
    
    // ... rest of method
}

public void executeQueuedCommands() {
    // Reset shouldStop flag to allow event scripts to execute after stopAll()
    if (shouldStop && !isProcessing && commandQueue.isEmpty()) {
        shouldStop = false;
    }
    
    // ... rest of method
}
```

**Тест:**
```javascript
// test_events_after_stop.kh
print "Starting event test"

onEvent onTick {
    print "[Tick] Current tick"
}

onEvent onHunger {
    print "[Hunger] Food level: $event_food"
}

print "Events registered"
print "Press Z to stop all scripts"
print "Then restart this script - events should still work!"
```

---

**Все критичные баги исправлены!** ✅
