# KASHUB FUTURE ROADMAP 🗺️

Development plans for future versions of the Kashub mod.

## 📋 v1.0.0 - "First Stable Release"

**Goal:** First stable release with full documentation

### 📚 Documentation
- [ ] Full documentation in English and Russian
- [ ] Video tutorials
- [x] Interactive examples
- [x] API reference
- [ ] Best practices guide

### 🧪 Testing
- [ ] Unit tests for all commands
- [ ] Integration tests
- [ ] Performance benchmarks
- [ ] Compatibility tests (different MC versions)
- [ ] Stress tests
- [x] Automated Regression Testing Suite (`comprehensive_test.kh`)

### 🎨 UI/UX Polish
- [x] Redesigned editor
- [x] Better error messages ✅ (CrashGuard system)
- [ ] Improved autocomplete
- [ ] Keyboard shortcuts customization
- [ ] Accessibility improvements

### 🔒 Security Audit
- [ ] Code review
- [ ] Penetration testing
- [ ] Sandbox hardening
- [ ] Permission system review
- [ ] Vulnerability fixes

### 📦 Terminal System
- [x] [Terminal system concept](https://github.com/KasperStudios/kashub/blob/v1.0.0-FSR/KASHUB_OS_LAYER_CONCEPT.md)
- [x] **Core Milestone: Mojang Mappings & Multiloader Baseline** (Fabric + NeoForge compile)
- [x] **FSR: ComputerCraft Bridge** (Optional integration: Discovery, Mounting, VFS Integration)
- [x] **FSR: Neural Link & Deep Scan** (Tier 2 Peripheral Scanner & Compendium)
- [x] **FSR: Access Policy Manager** (Survival/Casual/Creative gating)
- [x] **FSR: Sandbox Hardening** (Permission-gated API methods, eval restriction)
- [x] NeoForge port with KHWrapper (Full Parity achieved)

---

## 📋 v1.1.0 - "Marketplace & Community"

**Goal:** Ecosystem for sharing scripts and packages

### 🏪 Script Marketplace
- [ ] GitHub-based script repository
- [ ] Browse scripts by category
- [ ] Search and filter
- [ ] Ratings and reviews
- [ ] Download counts
- [ ] Verified authors

### 📦 Package Manager (Basic)
- [ ] `kashub install <package>` - Install package
- [ ] `kashub update <package>` - Update package
- [ ] `kashub remove <package>` - Remove package
- [ ] `kashub list` - List installed
- [ ] `kashub search <query>` - Search packages
- [ ] Dependency resolution (basic)
- [ ] Version pinning
- [ ] Local packages support

### 📦 Package System (Deferred from v1.0.0)
- [ ] Import/Export system (`export fn`, `use "package"`)
- [ ] Package manifest (`kashub.json`)
- [ ] `kh install` CLI command
- [ ] See `PACKAGE_SYSTEM_CONCEPT.md` for full design

### 🎯 Bonus Features (Deferred from v1.0.0)
- [ ] **W2PObject** - Macro recording/playback system (record player actions, replay with speed control)
- [ ] **AutoCraft System** - Automated crafting service with recipe resolution

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

**Goal:** Transform Kashub into a library ecosystem (like npm for KHScript)

### 📦 Package Types
- [ ] **Library** - Set of functions/commands for scripts
- [ ] **Command** - CLI-like utilities
- [ ] **Preset** - Configurations/Scripts bundles
- [ ] **Extension** - Java extensions with new commands

### 🔧 Import/Export System
- [ ] `export fn name() { }` - Function export
- [ ] `use "package.name"` - Library import
- [ ] `use "package" { func1, func2 }` - Selective import
- [ ] `use "package" as alias` - Import with alias
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
- [ ] `package-lock.json` for version locking
- [ ] Conflict resolution
- [ ] Dev dependencies
- [ ] Peer dependencies
- [ ] Optional dependencies

### 🛠️ Developer Tools
- [ ] `kashub init` - Create package
- [ ] `kashub pack` - Build package
- [ ] `kashub publish` - Publish package
- [ ] `kashub test` - Testing
- [ ] `kashub lint` - Code linting
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

**See PACKAGE_SYSTEM_CONCEPT.md for details**

---

## 📋 ~~v1.3.0~~ - "AI Integration" → **Moved to standalone [KasAgent](https://github.com/KasperStudios/KasAgent) mod**

> **Note:** AI integration features have been extracted into the standalone KasAgent mod.
> KasHub will provide hooks for KasAgent but will not bundle AI features directly.

### 🤖 AI Assistant (→ KasAgent)
- [ ] Natural language to script conversion
- [ ] "Explain this code" feature
- [ ] Bug fix suggestions
- [ ] Code optimization suggestions
- [ ] Documentation generation

### 🧠 Smart Commands (→ KasAgent)
- [ ] `ai.analyze(situation)` - Situation analysis
- [ ] `ai.decide(options)` - Decision making
- [ ] `ai.pathfind(goal)` - Smart pathfinding
- [ ] `ai.combat(strategy)` - Combat strategies
- [ ] `ai.farm(crop)` - Optimal farming

### 📊 Machine Learning (→ KasAgent)
- [ ] Learn from player behavior
- [ ] Adaptive difficulty
- [ ] Pattern recognition
- [ ] Anomaly detection
- [ ] Predictive actions

### 🎮 Game AI (→ KasAgent)
- [ ] NPC behavior scripts
- [ ] Enemy AI patterns
- [ ] Companion AI
- [ ] Trading AI
- [ ] Building AI

---

## 📋 v1.3.0 - "Visual Scripting"

**Goal:** Visual editor for beginners

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

**Goal:** Advanced world manipulation features

### 🏗️ Building Commands
- [ ] `fill(x1,y1,z1, x2,y2,z2, block)` - Fill area
- [ ] `replace(area, from, to)` - Replace blocks
- [ ] `clone(src, dst)` - Copy structures
- [ ] `rotate(area, angle)` - Rotation
- [ ] `mirror(area, axis)` - Mirroring

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

**Goal:** Automation management center

### 📊 Dashboard
- [ ] Overview of all scripts
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

**Goal:** Complete architecture overhaul

### 🏗️ New Architecture
- [ ] Modular plugin system
- [ ] Hot-swappable components
- [ ] Better memory management
- [ ] Async everything
- [ ] Multi-threaded execution

### 🌐 Cross-Platform
- [ ] Forge support
- [x] NeoForge support ✅ (Achieved in v1.0.0-FSR via KHWrapper)
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

### ✅ Completed
1. v1.0.0-FSR - First Stable Release (Fabric + NeoForge)
2. VSCode extension debug integration
3. CrashGuard & error handling system
4. Context/Task architecture refactoring
5. ComputerCraft Bridge & Access Policy Manager

### High Priority (Next 3 months)
1. v1.1.0 - Package System & Marketplace foundation
2. W2PObject (macro recording/playback)
3. AutoCraft system

### Medium Priority (6 months)
1. v1.2.0 - Full package ecosystem (import/export, kashub.json)
2. Community features & user profiles

### Low Priority (1 year+)
1. AI Integration (via KasAgent mod)
2. Visual Scripting
3. Cross-platform (Quilt, Bedrock)
4. Advanced Progression / Skills (**Proposed**)

---

## 💡 COMMUNITY SUGGESTIONS

Space for community ideas:

### Requested Features
- [x] Macro recording (recording player actions) ✅ (W2PObject concept, deferred to v1.1+)
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
- [x] Better error messages ✅ (CrashGuard system)
- [x] Undo in editor ✅ (UndoManager.java)
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

- Versions may change based on priorities
- Some features might be merged or split
- User feedback influences the roadmap
- Compatibility with new Minecraft versions is a priority

---

**Last Updated:** 2026-09-19
