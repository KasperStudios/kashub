import * as vscode from 'vscode';
import { KashubClient } from './kashubClient';

export class KashubHoverProvider implements vscode.HoverProvider {
    constructor(private client: KashubClient) {}

    async provideHover(
        document: vscode.TextDocument,
        position: vscode.Position,
        token: vscode.CancellationToken
    ): Promise<vscode.Hover | undefined> {
        const line = document.lineAt(position.line).text;
        
        // Check for object.method pattern at cursor position
        const beforeCursor = line.substring(0, position.character);
        const afterCursor = line.substring(position.character);
        
        // Match object.method pattern around cursor
        const beforeMatch = beforeCursor.match(/(\w+)\.(\w*)$/);
        const afterMatch = afterCursor.match(/^(\w*)/);
        
        if (beforeMatch) {
            const objectName = beforeMatch[1];
            const methodStart = beforeMatch[2];
            const methodEnd = afterMatch ? afterMatch[1] : '';
            const methodName = methodStart + methodEnd;
            const fullMethod = `${objectName}.${methodName}`;
            
            // Try to get hover for the full method
            const hover = this.getOfflineCommandHover(fullMethod);
            if (hover) {
                return hover;
            }
        }
        
        // Fallback to word-based detection
        const wordRange = document.getWordRangeAtPosition(position);
        if (!wordRange) {
            return undefined;
        }

        const word = document.getText(wordRange);

        // Check if it's a variable (starts with $)
        if (line.charAt(wordRange.start.character - 1) === '$') {
            return this.getVariableHover(word);
        }

        // Check if it's a command
        return this.getCommandHover(word, line);
    }

    private async getVariableHover(varName: string): Promise<vscode.Hover | undefined> {
        if (!this.client.connected) {
            return undefined;
        }

        try {
            const variables = await this.client.getVariables();
            const value = variables[varName];

            if (value !== undefined) {
                const markdown = new vscode.MarkdownString();
                markdown.appendCodeblock(`$${varName} = ${value}`, 'khscript');
                
                // Add type info
                const type = this.inferType(value);
                markdown.appendMarkdown(`\n**Type:** \`${type}\`\n\n`);
                
                // Add description for environment variables
                const envVarDescriptions: { [key: string]: string } = {
                    'PLAYER_NAME': 'Current player name',
                    'PLAYER_X': 'Player X coordinate',
                    'PLAYER_Y': 'Player Y coordinate',
                    'PLAYER_Z': 'Player Z coordinate',
                    'PLAYER_YAW': 'Player horizontal rotation',
                    'PLAYER_PITCH': 'Player vertical rotation',
                    'PLAYER_HEALTH': 'Player health points',
                    'PLAYER_FOOD': 'Player food level',
                    'PLAYER_LEVEL': 'Player experience level',
                    'PLAYER_SPEED': 'Player movement speed',
                    'WORLD_TIME': 'Current world time in ticks',
                    'WORLD_DAY': 'Current world day',
                    'WORLD_WEATHER': 'Current weather (clear/rain/thunder)',
                    'WORLD_DIFFICULTY': 'World difficulty setting'
                };

                if (envVarDescriptions[varName]) {
                    markdown.appendMarkdown(`*${envVarDescriptions[varName]}*`);
                }

                return new vscode.Hover(markdown);
            }
        } catch (error) {
            // Silently fail - variable might not exist yet
        }

        return undefined;
    }

    private async getCommandHover(commandName: string, line: string): Promise<vscode.Hover | undefined> {
        // Try offline hover first (for object.method patterns)
        const offlineHover = this.getOfflineCommandHover(commandName);
        if (offlineHover) {
            return offlineHover;
        }

        // Check if word is at start of line or after whitespace (likely a command)
        const trimmedLine = line.trim();
        if (!trimmedLine.startsWith(commandName)) {
            return undefined;
        }

        if (!this.client.connected) {
            return this.getOfflineCommandHover(commandName);
        }

        try {
            const commands = await this.client.getCommands();
            const command = commands.find((cmd: any) => cmd.name.toLowerCase() === commandName.toLowerCase());

            if (command) {
                const markdown = new vscode.MarkdownString();
                markdown.appendMarkdown(`### ${command.name}\n\n`);
                markdown.appendMarkdown(`**Category:** ${command.category || 'Other'}\n\n`);
                markdown.appendMarkdown(`${command.description}\n\n`);
                
                if (command.parameters) {
                    markdown.appendMarkdown(`**Usage:** \`${command.name} ${command.parameters}\`\n\n`);
                }

                if (command.detailedHelp) {
                    markdown.appendMarkdown('---\n\n');
                    markdown.appendMarkdown(command.detailedHelp);
                }

                return new vscode.Hover(markdown);
            }
        } catch (error) {
            // Fall back to offline hover
            return this.getOfflineCommandHover(commandName);
        }

        return undefined;
    }

    private getOfflineCommandHover(commandName: string): vscode.Hover | undefined {
        // Object methods documentation
        const objectMethods: { [key: string]: { description: string, usage: string, returns?: string } } = {
            // System Object
            'System.print': { description: 'Output message to chat and console', usage: 'System.print(message)', returns: 'null' },
            'System.log': { description: 'Output message to console only', usage: 'System.log(message)', returns: 'null' },
            'System.chat': { description: 'Send chat message', usage: 'System.chat(message)', returns: 'null' },
            'System.wait': { description: 'Sleep for milliseconds', usage: 'System.wait(ms)', returns: 'null' },
            'System.time': { description: 'Get current timestamp', usage: 'System.time()', returns: 'number' },
            'System.exit': { description: 'Stop script execution', usage: 'System.exit()', returns: 'null' },
            'System.gc': { description: 'Request garbage collection', usage: 'System.gc()', returns: 'null' },
            'System.memory': { description: 'Get memory usage info', usage: 'System.memory()', returns: '{used, free, total, max}' },
            
            // Player Object
            'player.getHealth': { description: 'Get player health (0-20)', usage: 'player.getHealth()', returns: 'number' },
            'player.getHunger': { description: 'Get player hunger (0-20)', usage: 'player.getHunger()', returns: 'number' },
            'player.getName': { description: 'Get player name', usage: 'player.getName()', returns: 'string' },
            'player.getPos': { description: 'Get player position', usage: 'player.getPos()', returns: '{x, y, z}' },
            'player.moveTo': { description: 'Move to coordinates (auto-stops on script end)', usage: 'player.moveTo(x, y, z, [radius])', returns: 'boolean' },
            'player.moveBy': { description: 'Move by relative coordinates', usage: 'player.moveBy(dx, dy, dz)', returns: 'boolean' },
            'player.stopMoving': { description: 'Stop navigation and clear input', usage: 'player.stopMoving()', returns: 'boolean' },
            'player.input': { description: 'Control player input directly', usage: 'player.input(action, type)', returns: 'boolean' },
            'player.lookAt': { description: 'Look at coordinates', usage: 'player.lookAt(x, y, z)', returns: 'boolean' },
            'player.sprint': { description: 'Enable/disable sprint', usage: 'player.sprint(enabled)', returns: 'boolean' },
            'player.attack': { description: 'Attack entity', usage: 'player.attack([entity])', returns: 'boolean' },
            'player.breakBlock': { description: 'Break block at crosshair', usage: 'player.breakBlock()', returns: 'boolean' },
            'player.placeBlock': { description: 'Place block by name', usage: 'player.placeBlock(blockName)', returns: 'boolean' },
            'player.chat': { description: 'Send chat message', usage: 'player.chat(message)', returns: 'boolean' },
            'player.animation': { description: 'Play animation', usage: 'player.animation(action, name, [duration])', returns: 'boolean' },
            
            // Scanner Object
            'scanner.blocks': { description: 'Scan for blocks', usage: 'scanner.blocks(type, radius)', returns: 'array of {x, y, z, id, dist}' },
            'scanner.entities': { description: 'Scan for entities', usage: 'scanner.entities(type, radius)', returns: 'array of {type, id, x, y, z, dist, health}' },
            
            // Vision Object
            'vision.getTarget': { description: 'Get crosshair target', usage: 'vision.getTarget()', returns: '{type, x, y, z, ...}' },
            'vision.getNearest': { description: 'Find nearest entity', usage: 'vision.getNearest(type, maxDist, [targetPart])', returns: '{type, id, pos, x, y, z, distance, health}' },
            'vision.nearest': { description: 'Alias for getNearest', usage: 'vision.nearest(type, maxDist, [targetPart])', returns: '{type, id, pos, x, y, z, distance, health}' },
            'vision.count': { description: 'Count entities', usage: 'vision.count(type, maxDist)', returns: 'number' },
            'vision.canSee': { description: 'Check if entity is visible', usage: 'vision.canSee(type, maxDist)', returns: 'boolean' },
            'vision.isLookingAt': { description: 'Check if looking at entity', usage: 'vision.isLookingAt(type, maxDist)', returns: 'boolean' },
            
            // Inventory Object
            'inventory.check': { description: 'Get inventory status', usage: 'inventory.check()', returns: '{totalItems, emptySlots, usedSlots}' },
            'inventory.count': { description: 'Count items', usage: 'inventory.count(itemName)', returns: 'number' },
            'inventory.find': { description: 'Find item slot', usage: 'inventory.find(itemName)', returns: 'number' },
            'inventory.getItems': { description: 'Get all items', usage: 'inventory.getItems()', returns: 'array of {slot, id, count, name}' },
            'inventory.getEmptySlots': { description: 'Count empty slots', usage: 'inventory.getEmptySlots()', returns: 'number' },
            'inventory.drop': { description: 'Drop item', usage: 'inventory.drop(slot, [dropAll])', returns: 'boolean' },
            'inventory.swap': { description: 'Swap items', usage: 'inventory.swap(slot1, slot2)', returns: 'boolean' },
            'inventory.equip': { description: 'Equip armor/shield', usage: 'inventory.equip(slot)', returns: 'boolean' },
            'inventory.use': { description: 'Use item by name', usage: 'inventory.use(itemName)', returns: 'boolean' },
            'inventory.craft': { description: 'Craft item', usage: 'inventory.craft(itemName, count)', returns: 'boolean' },
            
            // World Object
            'world.getBlock': { description: 'Get block at position', usage: 'world.getBlock(x, y, z)', returns: 'string' },
            'world.getTime': { description: 'Get world time', usage: 'world.getTime()', returns: 'number' },
            'world.getWeather': { description: 'Get weather', usage: 'world.getWeather()', returns: 'string' },
            'world.defineRecipe': { description: 'Define custom recipe', usage: 'world.defineRecipe(type, id, ...)', returns: 'boolean' },
            
            // Game Object
            'game.setGamma': { description: 'Set brightness', usage: 'game.setGamma(value)', returns: 'boolean' },
            'game.fullBright': { description: 'Enable full bright', usage: 'game.fullBright()', returns: 'boolean' },
            'game.setFov': { description: 'Set field of view', usage: 'game.setFov(value)', returns: 'boolean' },
            
            // Math Object
            'Math.sqrt': { description: 'Square root', usage: 'Math.sqrt(x)', returns: 'number' },
            'Math.abs': { description: 'Absolute value', usage: 'Math.abs(x)', returns: 'number' },
            'Math.min': { description: 'Minimum of two numbers', usage: 'Math.min(a, b)', returns: 'number' },
            'Math.max': { description: 'Maximum of two numbers', usage: 'Math.max(a, b)', returns: 'number' },
            'Math.floor': { description: 'Round down', usage: 'Math.floor(x)', returns: 'number' },
            'Math.ceil': { description: 'Round up', usage: 'Math.ceil(x)', returns: 'number' },
            'Math.round': { description: 'Round to nearest', usage: 'Math.round(x)', returns: 'number' },
            'Math.random': { description: 'Random number 0-1', usage: 'Math.random()', returns: 'number' },
            'Math.pow': { description: 'Power', usage: 'Math.pow(base, exponent)', returns: 'number' }
        };

        // Check for object.method pattern
        if (objectMethods[commandName]) {
            const method = objectMethods[commandName];
            const markdown = new vscode.MarkdownString();
            markdown.appendMarkdown(`### ${commandName}\n\n`);
            markdown.appendMarkdown(`${method.description}\n\n`);
            markdown.appendMarkdown(`**Usage:** \`${method.usage}\`\n\n`);
            if (method.returns) {
                markdown.appendMarkdown(`**Returns:** \`${method.returns}\`\n\n`);
            }
            markdown.appendMarkdown('*Connect to Kashub for full documentation*');
            return new vscode.Hover(markdown);
        }

        const offlineCommands: { [key: string]: { description: string, usage: string } } = {
            'print': {
                description: 'Print message to chat',
                usage: 'print <message>'
            },
            'log': {
                description: 'Log message to console/file only',
                usage: 'log <message>'
            },
            'wait': {
                description: 'Wait for specified milliseconds',
                usage: 'wait <milliseconds>'
            },
            'moveTo': {
                description: 'Move player to coordinates',
                usage: 'moveTo <x> <y> <z>'
            },
            'lookAt': {
                description: 'Look at coordinates or entity',
                usage: 'lookAt <x> <y> <z>'
            },
            'attack': {
                description: 'Attack entity',
                usage: 'attack <range> <type> <count>'
            },
            'breakBlock': {
                description: 'Break block at coordinates',
                usage: 'breakBlock <x> <y> <z>'
            },
            'placeBlock': {
                description: 'Place block at coordinates',
                usage: 'placeBlock <x> <y> <z> <block>'
            },
            'chat': {
                description: 'Send chat message',
                usage: 'chat <message>'
            },
            'onEvent': {
                description: 'Register event handler',
                usage: 'onEvent <eventName> { <script> }'
            },
            'export': {
                description: 'Export variable for use in other scripts',
                usage: 'export <variableName> <value>'
            },
            'import': {
                description: 'Import variable from another script',
                usage: 'import <variableName> from <scriptName>'
            },
            'loop': {
                description: 'Infinite or counted loop',
                usage: 'loop [count] { <script> }'
            },
            'if': {
                description: 'Conditional execution',
                usage: 'if (<condition>) { <script> }'
            },
            'while': {
                description: 'While loop',
                usage: 'while (<condition>) { <script> }'
            },
            'for': {
                description: 'For loop',
                usage: 'for (<init>; <condition>; <increment>) { <script> }'
            }
        };

        const cmd = offlineCommands[commandName];
        if (cmd) {
            const markdown = new vscode.MarkdownString();
            markdown.appendMarkdown(`### ${commandName}\n\n`);
            markdown.appendMarkdown(`${cmd.description}\n\n`);
            markdown.appendMarkdown(`**Usage:** \`${cmd.usage}\`\n\n`);
            markdown.appendMarkdown('*Connect to Kashub for full documentation*');
            return new vscode.Hover(markdown);
        }

        return undefined;
    }

    private inferType(value: any): string {
        if (typeof value === 'number') {
            return Number.isInteger(value) ? 'integer' : 'float';
        }
        if (typeof value === 'boolean') {
            return 'boolean';
        }
        if (typeof value === 'string') {
            // Try to detect coordinate format
            if (/^-?\d+(\.\d+)?,-?\d+(\.\d+)?,-?\d+(\.\d+)?$/.test(value)) {
                return 'coordinates';
            }
            return 'string';
        }
        return 'unknown';
    }
}
