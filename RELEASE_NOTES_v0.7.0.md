# Kashub v0.7.0 Beta - Release Notes

**"VSCode Integration & Developer Tools Update"**

---

## 🎉 What's New

### 🔌 VSCode Integration

Kashub теперь имеет полноценную интеграцию с VSCode! Пиши скрипты в профессиональной IDE с полной поддержкой языка.

**Основные возможности:**
- ✅ Syntax highlighting для KHScript
- ✅ IntelliSense с автодополнением команд
- ✅ Real-time валидация кода
- ✅ Kashub Console для live output
- ✅ Запуск скриптов из VSCode (Ctrl+Shift+K)
- ✅ Просмотр environment variables
- ✅ Управление задачами

### 🌐 HTTP API Server

Мод теперь предоставляет REST API для внешних инструментов:

**Endpoints:**
- `GET /api/status` - Статус мода и игрока
- `POST /api/validate` - Валидация скрипта
- `POST /api/autocomplete` - Автодополнение
- `POST /api/run` - Запуск скрипта
- `GET /api/tasks` - Список задач
- `POST /api/tasks/{id}/stop` - Остановка задачи
- `GET /api/variables` - Environment variables

**Порты:**
- HTTP API: 25566 (по умолчанию)
- WebSocket: 25567 (по умолчанию)

### 📡 WebSocket Server

Real-time коммуникация для live updates:

**События:**
- `script_output` - Вывод print/log команд
- `script_error` - Ошибки выполнения
- `task_state_change` - Изменение состояния задач
- `variable_update` - Обновление переменных

---

## 📦 Installation

### Мод (Minecraft)

1. Скачай `kashub-v0.7.0-beta.jar`
2. Помести в папку `mods`
3. Запусти Minecraft
4. API сервер запустится автоматически

### VSCode Extension

**Вариант 1: Из папки**
1. Открой VSCode
2. `F1` → "Developer: Install Extension from Location"
3. Выбери папку `kashub-vscode`

**Вариант 2: Из .vsix**
1. Перейди в `kashub-vscode`
2. Выполни `npm install && npm run compile`
3. Выполни `vsce package` (требует `npm install -g @vscode/vsce`)
4. В VSCode: Extensions → `...` → "Install from VSIX..."

---

## 🚀 Quick Start

### 1. Запусти Minecraft с модом

Проверь в логах:
```
[Kashub] Kashub API Server started on port 25566
[Kashub] Kashub WebSocket Server started on port 25567
```

### 2. Открой VSCode

Создай файл `test.kh`:
```javascript
print "Hello from VSCode!"
wait 1000
print "Health: $PLAYER_HEALTH"
```

### 3. Запусти скрипт

Нажми `Ctrl+Shift+K` или выполни команду "Kashub: Run Current Script"

### 4. Смотри результат

- В Minecraft появится сообщение в чате
- В VSCode откроется Kashub Console с live output

---

## ⚙️ Configuration

### Мод (config/kashub/config.json)

```json
{
  "apiEnabled": true,
  "apiPort": 25566,
  "apiWebSocketPort": 25567,
  "apiRequireAuth": false
}
```

### VSCode Extension

```json
{
  "kashub.apiUrl": "http://localhost:25566",
  "kashub.wsUrl": "ws://localhost:25567",
  "kashub.autoConnect": true,
  "kashub.showConsoleOnRun": true
}
```

---

## 🎨 Syntax Highlighting

Улучшенная подсветка синтаксиса:

- **Переменные** - `$PLAYER_X`, `$myVar` подсвечиваются как единое целое
- **Интерполяция** - Переменные внутри строк: `"Health: $PLAYER_HEALTH"`
- **Команды** - Все 44+ команды с категориями
- **Операторы** - Сравнение, логические, арифметические, тернарный
- **Ключевые слова** - `check`, `recipe`, `missing`, `list`, `toggle`, `best`, и т.д.

Открой `kashub-vscode/examples/syntax_showcase.kh` чтобы увидеть все возможности!

---

## 📝 VSCode Commands

| Command | Keybinding | Description |
|---------|------------|-------------|
| Kashub: Run Current Script | `Ctrl+Shift+K` | Запустить текущий скрипт |
| Kashub: Open Console | `Ctrl+Shift+\`` | Открыть консоль |
| Kashub: Show Variables | - | Показать переменные |
| Kashub: Stop All Tasks | - | Остановить все задачи |
| Kashub: Reconnect to Mod | - | Переподключиться |

---

## 🔧 Troubleshooting

### API не запускается

1. Проверь `apiEnabled: true` в конфиге
2. Проверь что порты не заняты
3. Проверь логи Minecraft

### VSCode не подключается

1. Убедись что Minecraft запущен
2. Кликни на статус бар "Kashub" для переподключения
3. Проверь URL в настройках VSCode

### Расширение не активируется

1. Проверь что файл имеет расширение `.kh`
2. Перезапусти VSCode
3. Проверь Output → Kashub на ошибки

---

## 🐛 Known Issues

- WebSocket может отключаться на медленных соединениях (авто-переподключение)
- Большие скрипты (>5000 строк) могут медленнее валидироваться
- Первое подключение может занять 1-2 секунды

---

## 🔮 What's Next?

Смотри `ROADMAP_FUTURE.md` для планов на будущие версии:

- **v0.8.0** - Advanced Debugging (breakpoints, step debugging, profiler)
- **v0.9.0** - Multiplayer & Networking
- **v1.0.0** - Stable Release
- **v1.1.0** - Marketplace & Community
- **v1.2.0** - AI Integration
- **v1.3.0** - Visual Scripting

---

## 📚 Documentation

- **README.md** - Основная документация мода
- **kashub-vscode/README.md** - Документация расширения
- **docs/KHScriptGuide.md** - Полное руководство по языку
- **kashub-vscode/examples/** - Примеры скриптов

---

## 🙏 Feedback

Нашел баг или есть идея? Создай issue на GitHub!

---

**Enjoy coding!** 🚀
