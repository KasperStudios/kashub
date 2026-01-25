# Changelog

All notable changes to the Kashub VSCode extension will be documented in this file.

## [0.9.1] - 2026-01-23

### Added
- **New Player Methods**:
  - `player.moveBy(dx, dy, dz)` - Move by relative coordinates
  - `player.stopMoving()` - Stop navigation and clear input
  - `player.input(action, type)` - Direct input control (forward, back, left, right, jump, sneak, sprint, attack, use)
- **New Snippets**: Added snippets for new player methods
  - `pmoveby` - Move by relative coordinates
  - `pstop` - Stop movement
  - `pinput` - Control player input
- **Improved Hover Documentation**: Updated player.moveTo to indicate auto-cleanup on script end

### Fixed
- **Hover Provider**: Fixed hover not working for object.method patterns (e.g., `player.moveTo`, `System.print`)
- **Word Pattern**: Added wordPattern to language configuration to properly detect object.method as single word
- Updated hover provider with new player methods
- Added input types (press, release, tap) to snippets

## [0.9.0] - 2026-01-23

### Added
- **V2 API Support**: Full support for new object-oriented syntax
  - System object (print, log, wait, memory, etc.)
  - player object (getHealth, moveTo, attack, etc.)
  - scanner object (blocks, entities)
  - vision object (nearest, getTarget, count, etc.)
  - inventory object (check, count, use, craft, etc.)
  - world object (getBlock, getTime, getWeather)
  - game object (setGamma, fullBright, setFov)
  - Math object (sqrt, abs, min, max, floor, ceil, round, random, pow)
- **Enhanced Syntax Highlighting**: Objects and methods now have distinct colors
- **New Snippets**: 15+ new code snippets for V2 API
  - System.print, player.moveTo, scanner.blocks, etc.
  - Auto-heal, combat bot, mining bot templates
  - CrashGuard block template
- **Improved Hover Documentation**: Detailed docs for all object methods with return types
- **Better Auto-completion**: Context-aware suggestions for object methods

### Changed
- Updated syntax highlighting to distinguish objects from methods
- Improved hover provider with return types and usage examples
- Enhanced snippets with V2 API patterns
- Updated to match Kashub v0.9.0-beta

### Fixed
- Fixed method highlighting for object-oriented syntax
- Corrected auto-completion for chained method calls

## [0.8.1] - 2025-12-29

### Added
- **Remote Debugging Commands** - Pause, Resume, Step Over/Into from VS Code
- **Breakpoint Support** - Toggle breakpoints via `Kashub Debug: Add Breakpoint`
- **Debug Event Integration** - Extension now listens to Kashub debug events

## [0.8.0] - 2025-12-28

### Added
- **Advanced Debugging Support** - Integration with Kashub v0.8.0-beta Debug API
- **Live Variable Inspector** - View script variables in real-time side-bar
- **Socket Protocol Update** - Updated WebSocket client for v0.8.0-beta compatibility
- **Performance Improvements** - Reduced latency in command execution

## [0.1.1] - 2025-12-21

### Fixed
- **Improved variable highlighting** - Variables with `$` now highlight as single unit
- **Better string interpolation** - Variables inside strings are now highlighted
- **Enhanced operator highlighting** - Added ternary operator and better precedence

### Added
- **Special keyword highlighting** - Keywords like `check`, `recipe`, `missing`, `list`, `toggle`, `best`, etc.
- **Command categories** - Commands now grouped by function (builtin, movement, combat, etc.)
- **Example files** - Added `syntax_showcase.kh` demonstrating all features

### Changed
- **Pattern order** - Optimized pattern matching order for better performance
- **Operator precedence** - Comparison operators now match before assignment

## [0.1.0] - 2025-12-21

### Added
- Initial release
- Full KHScript syntax highlighting
- IntelliSense with online/offline modes
- Real-time validation
- Kashub Console panel
- Run scripts from VSCode (Ctrl+Shift+K)
- Variables viewer
- Task management
- Status bar integration
- WebSocket support for live output
