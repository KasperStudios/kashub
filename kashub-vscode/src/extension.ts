import * as vscode from 'vscode';
import * as path from 'path';
import { KashubClient } from './kashubClient';
import { KashubCompletionProvider } from './completionProvider';
import { KashubDiagnosticsProvider } from './diagnosticsProvider';
import { KashubConsolePanel } from './consolePanel';
import { KashubStatusBar } from './statusBar';

let kashubClient: KashubClient;
let statusBar: KashubStatusBar;
let diagnosticsProvider: KashubDiagnosticsProvider;

export function activate(context: vscode.ExtensionContext) {
    console.log('Kashub extension is now active!');
    
    // Initialize client
    kashubClient = new KashubClient();
    
    // Initialize status bar
    statusBar = new KashubStatusBar(kashubClient);
    context.subscriptions.push(statusBar);
    
    // Initialize diagnostics provider
    diagnosticsProvider = new KashubDiagnosticsProvider(kashubClient);
    context.subscriptions.push(diagnosticsProvider);
    
    // Register completion provider
    const completionProvider = new KashubCompletionProvider(kashubClient);
    context.subscriptions.push(
        vscode.languages.registerCompletionItemProvider(
            'khscript',
            completionProvider,
            '.', '$'
        )
    );
    
    // Auto-connect if enabled
    const config = vscode.workspace.getConfiguration('kashub');
    if (config.get('autoConnect', true)) {
        kashubClient.connect();
    }
    
    // Update diagnostics on document change
    context.subscriptions.push(
        vscode.workspace.onDidChangeTextDocument(event => {
            if (event.document.languageId === 'khscript') {
                diagnosticsProvider.updateDiagnostics(event.document);
            }
        })
    );
    
    // Update diagnostics on document open
    context.subscriptions.push(
        vscode.workspace.onDidOpenTextDocument(document => {
            if (document.languageId === 'khscript') {
                diagnosticsProvider.updateDiagnostics(document);
            }
        })
    );
    
    // Command: Run Script
    context.subscriptions.push(
        vscode.commands.registerCommand('kashub.runScript', async () => {
            const editor = vscode.window.activeTextEditor;
            if (!editor || editor.document.languageId !== 'khscript') {
                vscode.window.showErrorMessage('No KHScript file open');
                return;
            }
            
            if (!kashubClient.connected) {
                const retry = await vscode.window.showWarningMessage(
                    'Not connected to Kashub. Try to connect?',
                    'Connect', 'Cancel'
                );
                if (retry === 'Connect') {
                    await kashubClient.connect();
                }
                if (!kashubClient.connected) {
                    return;
                }
            }
            
            try {
                const code = editor.document.getText();
                const filename = path.basename(editor.document.fileName);
                const result = await kashubClient.runScript(code, filename);
                
                if (result.success) {
                    vscode.window.showInformationMessage(
                        `✅ Script started (Task #${result.taskId})`
                    );
                    
                    // Open console if configured
                    if (config.get('showConsoleOnRun', true)) {
                        KashubConsolePanel.createOrShow(context.extensionUri, kashubClient);
                    }
                } else {
                    vscode.window.showErrorMessage(`❌ ${result.error}`);
                }
            } catch (error: any) {
                vscode.window.showErrorMessage(`❌ ${error.message}`);
            }
        })
    );
    
    // Command: Open Console
    context.subscriptions.push(
        vscode.commands.registerCommand('kashub.openConsole', () => {
            KashubConsolePanel.createOrShow(context.extensionUri, kashubClient);
        })
    );
    
    // Command: Show Variables
    context.subscriptions.push(
        vscode.commands.registerCommand('kashub.showVariables', async () => {
            if (!kashubClient.connected) {
                vscode.window.showWarningMessage('Not connected to Kashub');
                return;
            }
            
            try {
                const variables = await kashubClient.getVariables();
                
                const quickPick = vscode.window.createQuickPick();
                quickPick.items = Object.entries(variables).map(([key, value]) => ({
                    label: '$' + key,
                    description: String(value)
                }));
                quickPick.placeholder = 'Environment Variables';
                quickPick.onDidAccept(() => {
                    const selected = quickPick.selectedItems[0];
                    if (selected) {
                        vscode.env.clipboard.writeText(selected.label);
                        vscode.window.showInformationMessage(`Copied ${selected.label} to clipboard`);
                    }
                    quickPick.hide();
                });
                quickPick.show();
            } catch (error: any) {
                vscode.window.showErrorMessage(`Failed to get variables: ${error.message}`);
            }
        })
    );
    
    // Command: Stop All Tasks
    context.subscriptions.push(
        vscode.commands.registerCommand('kashub.stopAllTasks', async () => {
            if (!kashubClient.connected) {
                vscode.window.showWarningMessage('Not connected to Kashub');
                return;
            }
            
            try {
                const tasks = await kashubClient.getTasks();
                let stopped = 0;
                for (const task of tasks) {
                    if (task.state === 'RUNNING' || task.state === 'PAUSED') {
                        await kashubClient.stopTask(task.id);
                        stopped++;
                    }
                }
                vscode.window.showInformationMessage(`Stopped ${stopped} task(s)`);
            } catch (error: any) {
                vscode.window.showErrorMessage(`Failed to stop tasks: ${error.message}`);
            }
        })
    );
    
    // Command: Reconnect
    context.subscriptions.push(
        vscode.commands.registerCommand('kashub.reconnect', async () => {
            const connected = await kashubClient.connect();
            if (connected) {
                vscode.window.showInformationMessage('Connected to Kashub!');
            } else {
                vscode.window.showWarningMessage('Failed to connect to Kashub. Is Minecraft running with the mod?');
            }
        })
    );
}

export function deactivate() {
    if (kashubClient) {
        kashubClient.disconnect();
    }
}
