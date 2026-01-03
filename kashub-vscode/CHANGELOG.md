# Changelog

All notable changes to the Kashub VSCode extension will be documented in this file.

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
