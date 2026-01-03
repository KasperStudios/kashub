# KASHUB FUTURE ROADMAP 🗺️

Планы развития мода Kashub на будущие версии.

---

## 📋 v0.8.0-beta - "Advanced Debugging & Profiling"

**Цель:** Профессиональные инструменты отладки для разработчиков скриптов

### 🔧 Breakpoints & Step Debugging
- [ ] Breakpoints в редакторе (клик по номеру строки)
- [ ] Step Into / Step Over / Step Out
- [ ] Continue to cursor
- [ ] Conditional breakpoints (остановка при условии)
- [ ] Logpoints (вывод без остановки)

### 📊 Variables Panel
- [ ] Watch expressions
- [ ] Variable inspection в реальном времени
- [ ] Modify variables во время выполнения
- [ ] Call stack viewer
- [ ] Scope visualization (local/global)

### ⚡ Performance Profiler
- [ ] Время выполнения каждой команды
- [ ] Memory usage tracking
- [ ] Hotspot detection (медленные участки)
- [ ] Flame graph visualization
- [ ] Export профиля в JSON

### 🔍 Code Analysis
- [ ] Dead code detection
- [ ] Unused variables warning
- [ ] Infinite loop detection
- [ ] Complexity metrics
- [ ] Suggestions for optimization

### 🔌 VSCode Extension Integration
- [x] Basic debug commands (resume, pause, step over, step into)
- [ ] Full breakpoint sync с DebugManager
- [ ] Variables panel в VSCode
- [ ] Call stack visualization
- [ ] Debug console integration
- [ ] Conditional breakpoints UI
- [ ] Watch expressions
- [ ] Inline variable values (CodeLens)
- [ ] Step Out implementation
- [ ] Профiler интеграция (показ времени выполнения)

---

## 📋 v0.9.0 - "Modpacks & Server Integration"

**Цель:** Полная поддержка создателей сборок и серверов, гибкая настройка и серверные скрипты

### 🏗️ Architecture Refactoring (PRIORITY)
**See:** [REFACTORING_PLAN.md](./REFACTORING_PLAN.md)

- [ ] **Phase 1: Create New Classes** (v0.9.0)
  - [ ] Create `ScriptExecutionContext` class
  - [ ] Create `EnvironmentVariableProvider` class
  - [ ] Update `ScriptTask` to use new context
  - [ ] Keep backward compatibility

- [ ] **Phase 2: Migrate Core Features** (v0.9.1)
  - [ ] Migrate variable system to per-script context
  - [ ] Migrate function system to per-script context
  - [ ] Migrate command execution to context-based
  - [ ] Remove global state dependencies

- [ ] **Phase 3: Update Event System** (v0.9.2)
  - [ ] Events create temporary execution contexts
  - [ ] Track event ownership per script
  - [ ] Fix event isolation issues

**Benefits:**
- ✅ Better script isolation (no variable leaks)
- ✅ Thread safety (no race conditions)
- ✅ Easier testing and debugging
- ✅ Cleaner code architecture
- ✅ Foundation for multi-threaded execution

### 🌐 Server & Modpack Scripts
- [ ] **Server-Side Execution**: Скрипты выполняются на сервере (или в контексте сборки)
- [ ] **Modpack Scripts**: Скрипты сборки считаются серверными и обязательны для всех
- [ ] **Custom Crafting API**: Создание кастомных крафтов через скрипты
- [ ] **Event Hooks**: Глобальные события сервера/сборки
- [ ] **Persistency**: Сохранение данных между перезапусками сервера

### ⚙️ Flexible Configuration (For Admin/Modpack Creators)
- [ ] **Feature Toggles**: Возможность отключить/включить редактор, дебаггер или конкретные команды
- [ ] **Restricted Mode**: Ограничение использования мода игроками (только серверные скрипты)
- [ ] **Custom Functionality**: Настройка уникальных механик сервера через конфиг
- [ ] **Client Enforcement**: Синхронизация настроек с клиентом при входе

### 🔌 Universal Integration System
- [ ] **Mod Bridge API**: Интеграция с любыми модами через рефлексию/API
- [ ] **Dynamic Compatibility**: Скрипты могут проверять наличие модов и адаптироваться
- [ ] **Item/Block Tags**: Работа с тегами из других модов
- [ ] **UI Integration**: Добавление своих кнопок/меню в интерфейсы других модов

### 👥 Player & Network Interaction
- [ ] Permissions system (кто может запускать скрипты)
- [ ] `onPlayerJoin` / `onPlayerLeave` events
- [ ] `sendMessage(player, msg)` - личные сообщения
- [ ] Network packets handling (отправка/получение пакетов)

### 📦 Package System
- [ ] [Package system concept](https://github.com/KasperStudios/kashub/blob/v0.8.0-beta/PACKAGE_SYSTEM_CONCEPT.md)

---

## 📋 v1.0.0 - "Stable Release"

**Цель:** Первый стабильный релиз с полной документацией

### 🏗️ Complete Architecture Refactoring
- [ ] **Phase 4: Update Commands** (v0.9.3)
  - [ ] Add context parameter to Command interface
  - [ ] Update all ~50 command implementations
  - [ ] Test each command with new architecture

- [ ] **Phase 5: Update Debug/API** (v0.9.4)
  - [ ] Update Debug Adapter Protocol for per-script variables
  - [ ] Update REST API endpoints
  - [ ] Update WebSocket server

- [ ] **Phase 6: Remove ScriptInterpreter** (v1.0.0)
  - [ ] Delete `ScriptInterpreter.java`
  - [ ] Remove all singleton dependencies
  - [ ] Clean up legacy code
  - [ ] Update all documentation

### 📚 Documentation
- [ ] Полная документация на русском и английском
- [ ] Video tutorials
- [ ] Interactive examples
- [ ] API reference
- [ ] Best practices guide

### 🧪 Testing
- [ ] Unit tests для всех команд
- [ ] Integration tests
- [ ] Performance benchmarks
- [ ] Compatibility tests (разные версии MC)
- [ ] Stress tests

### 🎨 UI/UX Polish
- [ ] Redesigned editor
- [ ] Better error messages
- [ ] Improved autocomplete
- [ ] Keyboard shortcuts customization
- [ ] Accessibility improvements

### 🔒 Security Audit
- [ ] Code review
- [ ] Penetration testing
- [ ] Sandbox hardening
- [ ] Permission system review
- [ ] Vulnerability fixes

---

## 📋 v1.1.0 - "Marketplace & Community"

**Цель:** Экосистема для обмена скриптами и пакетами

### 🏪 Script Marketplace
- [ ] GitHub-based script repository
- [ ] Browse scripts by category
- [ ] Search and filter
- [ ] Ratings and reviews
- [ ] Download counts
- [ ] Verified authors

### 📦 Package Manager (Basic)
- [ ] `kashub install <package>` - Установка пакета
- [ ] `kashub update <package>` - Обновление
- [ ] `kashub remove <package>` - Удаление
- [ ] `kashub list` - Список установленных
- [ ] `kashub search <query>` - Поиск пакетов
- [ ] Dependency resolution (basic)
- [ ] Version pinning
- [ ] Local packages support

### 👤 User Profiles
- [ ] Author profiles
- [ ] Published scripts list
- [ ] Followers/Following
- [ ] Activity feed
- [ ] Achievements/Badges

### 🔄 Auto-Updates
- [ ] Check for script updates
- [ ] One-click update
- [ ] Changelog viewer
- [ ] Rollback support
- [ ] Update notifications

---

## 📋 v1.2.0 - "Package System & Libraries"

**Цель:** Превратить Kashub в экосистему библиотек (как npm для KHScript)

### 📦 Package Types
- [ ] **Library** - Набор функций/команд для скриптов
- [ ] **Command** - CLI-подобные утилиты
- [ ] **Preset** - Наборы конфигов/скриптов
- [ ] **Extension** - Java-расширения с новыми командами

### 🔧 Import/Export System
- [ ] `export fn name() { }` - Экспорт функций
- [ ] `use "package.name"` - Импорт библиотеки
- [ ] `use "package" { func1, func2 }` - Выборочный импорт
- [ ] `use "package" as alias` - Импорт с алиасом
- [ ] Namespace support (`package.function()`)
- [ ] Private vs Public functions

### 📋 Package Manifest (kashub.json)
- [ ] Package metadata (name, version, author)
- [ ] Semver versioning
- [ ] Dependencies declaration
- [ ] Entry points
- [ ] Exports configuration
- [ ] Permissions/Sandbox settings
- [ ] Min Kashub/Minecraft version

### 🔄 Advanced Package Manager
- [ ] Semver version resolution
- [ ] Dependency tree
- [ ] `package-lock.json` для фиксации версий
- [ ] Conflict resolution
- [ ] Dev dependencies
- [ ] Peer dependencies
- [ ] Optional dependencies

### 🛠️ Developer Tools
- [ ] `kashub init` - Создание пакета
- [ ] `kashub pack` - Сборка пакета
- [ ] `kashub publish` - Публикация
- [ ] `kashub test` - Тестирование
- [ ] `kashub lint` - Проверка кода
- [ ] Package templates

### 🔒 Security
- [ ] Package signing
- [ ] Signature verification
- [ ] Sandbox permissions
- [ ] Audit vulnerabilities
- [ ] Trusted publishers

### 📊 Package Statistics
- [ ] Download counts
- [ ] Dependents tracking
- [ ] Usage analytics
- [ ] Health scores

**См. PACKAGE_SYSTEM_CONCEPT.md для деталей**

---

## 📋 v1.3.0 - "AI Integration"

**Цель:** Интеграция с AI для умных скриптов

### 🤖 AI Assistant
- [ ] Natural language to script conversion
- [ ] "Explain this code" feature
- [ ] Bug fix suggestions
- [ ] Code optimization suggestions
- [ ] Documentation generation

### 🧠 Smart Commands
- [ ] `ai.analyze(situation)` - анализ ситуации
- [ ] `ai.decide(options)` - принятие решений
- [ ] `ai.pathfind(goal)` - умный pathfinding
- [ ] `ai.combat(strategy)` - боевые стратегии
- [ ] `ai.farm(crop)` - оптимальное фермерство

### 📊 Machine Learning
- [ ] Learn from player behavior
- [ ] Adaptive difficulty
- [ ] Pattern recognition
- [ ] Anomaly detection
- [ ] Predictive actions

### 🎮 Game AI
- [ ] NPC behavior scripts
- [ ] Enemy AI patterns
- [ ] Companion AI
- [ ] Trading AI
- [ ] Building AI

---

## 📋 v1.3.0 - "Visual Scripting"

**Цель:** Визуальный редактор для новичков

### 🎨 Node-Based Editor
- [ ] Drag-and-drop nodes
- [ ] Visual connections
- [ ] Real-time preview
- [ ] Convert to/from text
- [ ] Templates library

### 📦 Pre-built Blocks
- [ ] Movement blocks
- [ ] Combat blocks
- [ ] Inventory blocks
- [ ] Logic blocks
- [ ] Event blocks

### 🔄 Flow Control
- [ ] Visual if/else
- [ ] Loop visualization
- [ ] Function blocks
- [ ] Event triggers
- [ ] State machines

### 📱 Mobile Support
- [ ] Touch-friendly interface
- [ ] Gesture controls
- [ ] Mobile preview
- [ ] Cloud sync
- [ ] Remote execution

---

## 📋 v1.4.0 - "World Manipulation"

**Цель:** Расширенные возможности изменения мира

### 🏗️ Building Commands
- [ ] `fill(x1,y1,z1, x2,y2,z2, block)` - заполнение области
- [ ] `replace(area, from, to)` - замена блоков
- [ ] `clone(src, dst)` - копирование структур
- [ ] `rotate(area, angle)` - поворот
- [ ] `mirror(area, axis)` - отражение

### 📐 Schematic Support
- [ ] Load .schematic files
- [ ] Save selections
- [ ] Preview before build
- [ ] Undo/Redo
- [ ] Blueprint library

### 🌍 Terrain Generation
- [ ] Custom terrain generators
- [ ] Biome manipulation
- [ ] Structure generation
- [ ] Cave generation
- [ ] Water/Lava flow

### 🎭 Entity Manipulation
- [ ] Spawn entities
- [ ] Modify entity data
- [ ] Entity AI control
- [ ] Particle effects
- [ ] Sound effects

---

## 📋 v1.5.0 - "Automation Hub"

**Цель:** Центр управления автоматизацией

### 📊 Dashboard
- [ ] Overview всех скриптов
- [ ] Resource monitoring
- [ ] Activity graphs
- [ ] Alerts and notifications
- [ ] Quick actions

### ⏰ Scheduling
- [ ] Cron-like scheduling
- [ ] Time-based triggers
- [ ] Recurring tasks
- [ ] Calendar view
- [ ] Timezone support

### 🔗 Integrations
- [ ] Discord webhooks
- [ ] Telegram notifications
- [ ] Email alerts
- [ ] Custom webhooks
- [ ] REST API callbacks

### 📈 Analytics
- [ ] Script usage stats
- [ ] Performance trends
- [ ] Error tracking
- [ ] User behavior
- [ ] Export reports

---

## 📋 v2.0.0 - "Next Generation"

**Цель:** Полная переработка архитектуры

### 🏗️ New Architecture
- [ ] Modular plugin system
- [ ] Hot-swappable components
- [ ] Better memory management
- [ ] Async everything
- [ ] Multi-threaded execution

### 🌐 Cross-Platform
- [ ] Forge support
- [ ] NeoForge support
- [ ] Quilt support
- [ ] Bedrock Edition (?)
- [ ] Standalone tool

### 🔧 Developer Tools
- [ ] SDK for extensions
- [ ] Custom command API
- [ ] Event system API
- [ ] UI component library
- [ ] Testing framework

### 🎮 Game Modes
- [ ] Adventure map scripting
- [ ] Minigame framework
- [ ] RPG elements
- [ ] Quest system
- [ ] Dialogue system

---

## 🎯 PRIORITY MATRIX

### High Priority (Next 3 months)
1. v0.8.0-beta - Debugging tools
2. VSCode extension debug integration
3. Documentation

### Medium Priority (6 months)
1. v0.9.0 - Multiplayer
2. v1.0.0 - Stable release
3. Marketplace foundation

### Low Priority (1 year+)
1. AI Integration
2. Visual Scripting
3. Cross-platform

---

## 💡 COMMUNITY SUGGESTIONS

Место для идей от сообщества:

### Requested Features
- [ ] Macro recording (запись действий игрока)
- [ ] Script templates wizard
- [ ] Dark/Light theme toggle
- [ ] Custom keybinds per script
- [ ] Script groups/folders
- [ ] Import from other mods (Baritone, etc.)
- [ ] Lua/Python scripting support
- [ ] Voice commands integration
- [ ] VR support
- [ ] Replay mod integration

### Quality of Life
- [ ] Better error messages
- [ ] Undo in editor
- [ ] Find and replace
- [ ] Code folding
- [ ] Multiple cursors
- [ ] Split view
- [ ] Minimap
- [ ] Breadcrumbs

### Performance
- [ ] Script compilation
- [ ] Caching
- [ ] Lazy loading
- [ ] Memory optimization
- [ ] Startup time reduction

---

## 📝 NOTES

- Версии могут меняться в зависимости от приоритетов
- Некоторые фичи могут быть объединены или разделены
- Feedback от пользователей влияет на roadmap
- Совместимость с новыми версиями Minecraft - приоритет

---

**Последнее обновление:** 2025-12-21
