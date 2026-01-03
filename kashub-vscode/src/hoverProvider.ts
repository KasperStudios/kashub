import * as vscode from 'vscode';
import { KashubClient } from './kashubClient';

export class KashubHoverProvider implements vscode.HoverProvider {
    constructor(private client: KashubClient) {}

    async provideHover(
        document: vscode.TextDocument,
        position: vscode.Position,
        token: vscode.CancellationToken
    ): Promise<vscode.Hover | undefined> {
        const wordRange = document.getWordRangeAtPosition(position);
        if (!wordRange) {
            return undefined;
        }

        const word = document.getText(wordRange);
        const line = document.lineAt(position.line).text;

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
