import * as vscode from 'vscode';
import { KashubClient, ScriptOutputEvent, ScriptErrorEvent } from './kashubClient';

interface ConsoleMessage {
    type: 'output' | 'error';
    level: string;
    message: string;
    taskId?: number;
    line?: number;
    timestamp: number;
}

export class KashubConsolePanel {
    public static currentPanel: KashubConsolePanel | undefined;
    private readonly _panel: vscode.WebviewPanel;
    private _disposables: vscode.Disposable[] = [];
    private messages: ConsoleMessage[] = [];
    
    public static createOrShow(
        extensionUri: vscode.Uri,
        kashubClient: KashubClient
    ): void {
        const column = vscode.ViewColumn.Two;
        
        if (KashubConsolePanel.currentPanel) {
            KashubConsolePanel.currentPanel._panel.reveal(column);
            return;
        }
        
        const panel = vscode.window.createWebviewPanel(
            'kashubConsole',
            'Kashub Console',
            column,
            {
                enableScripts: true,
                retainContextWhenHidden: true
            }
        );
        
        KashubConsolePanel.currentPanel = new KashubConsolePanel(
            panel,
            extensionUri,
            kashubClient
        );
    }
    
    private constructor(
        panel: vscode.WebviewPanel,
        extensionUri: vscode.Uri,
        kashubClient: KashubClient
    ) {
        this._panel = panel;
        
        // Set HTML content
        this._panel.webview.html = this._getHtmlForWebview();
        
        // Listen to script output
        kashubClient.onOutput((event: ScriptOutputEvent) => {
            this.addMessage({
                type: 'output',
                level: event.level || 'info',
                message: event.message,
                taskId: event.taskId,
                timestamp: event.timestamp
            });
        });
        
        // Listen to script errors
        kashubClient.onError((event: ScriptErrorEvent) => {
            this.addMessage({
                type: 'error',
                level: 'error',
                message: event.error,
                taskId: event.taskId,
                line: event.line,
                timestamp: event.timestamp
            });
        });
        
        // Handle messages from webview
        this._panel.webview.onDidReceiveMessage(
            message => {
                switch (message.command) {
                    case 'clear':
                        this.messages = [];
                        this._panel.webview.postMessage({ command: 'clear' });
                        break;
                    case 'export':
                        this.exportLogs();
                        break;
                }
            },
            null,
            this._disposables
        );
        
        this._panel.onDidDispose(() => this.dispose(), null, this._disposables);
    }
    
    private addMessage(message: ConsoleMessage): void {
        this.messages.push(message);
        
        // Limit to 1000 messages
        if (this.messages.length > 1000) {
            this.messages.shift();
        }
        
        // Send to webview
        this._panel.webview.postMessage({
            command: 'addMessage',
            message: message
        });
    }
    
    private async exportLogs(): Promise<void> {
        const content = this.messages.map(m => {
            const time = new Date(m.timestamp).toISOString();
            const task = m.taskId ? `#${m.taskId}` : '-';
            const line = m.line ? ` (line ${m.line})` : '';
            return `[${time}] [${task}] [${m.level.toUpperCase()}] ${m.message}${line}`;
        }).join('\n');
        
        const uri = await vscode.window.showSaveDialog({
            defaultUri: vscode.Uri.file('kashub-console.log'),
            filters: { 'Log files': ['log', 'txt'] }
        });
        
        if (uri) {
            await vscode.workspace.fs.writeFile(uri, Buffer.from(content, 'utf8'));
            vscode.window.showInformationMessage(`Exported ${this.messages.length} messages to ${uri.fsPath}`);
        }
    }
    
    private _getHtmlForWebview(): string {
        return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kashub Console</title>
    <style>
        * {
            box-sizing: border-box;
        }
        body {
            font-family: 'Consolas', 'Courier New', monospace;
            padding: 0;
            margin: 0;
            background-color: var(--vscode-editor-background);
            color: var(--vscode-editor-foreground);
            height: 100vh;
            display: flex;
            flex-direction: column;
        }
        
        #toolbar {
            display: flex;
            gap: 10px;
            padding: 8px 12px;
            background-color: var(--vscode-titleBar-activeBackground);
            border-bottom: 1px solid var(--vscode-panel-border);
            align-items: center;
        }
        
        button {
            padding: 4px 12px;
            background-color: var(--vscode-button-background);
            color: var(--vscode-button-foreground);
            border: none;
            cursor: pointer;
            border-radius: 2px;
            font-size: 12px;
        }
        
        button:hover {
            background-color: var(--vscode-button-hoverBackground);
        }
        
        .toolbar-spacer {
            flex: 1;
        }
        
        .status {
            font-size: 11px;
            color: var(--vscode-descriptionForeground);
        }
        
        #console {
            flex: 1;
            overflow-y: auto;
            padding: 8px 12px;
        }
        
        .message {
            padding: 4px 0;
            border-bottom: 1px solid var(--vscode-panel-border);
            display: flex;
            gap: 8px;
            font-size: 12px;
            line-height: 1.4;
        }
        
        .timestamp {
            color: var(--vscode-descriptionForeground);
            min-width: 70px;
            flex-shrink: 0;
        }
        
        .task-id {
            color: var(--vscode-textLink-foreground);
            min-width: 40px;
            flex-shrink: 0;
        }
        
        .level {
            font-weight: bold;
            min-width: 50px;
            flex-shrink: 0;
        }
        
        .level-info {
            color: #4ec9b0;
        }
        
        .level-warn {
            color: #dcdcaa;
        }
        
        .level-error {
            color: #f48771;
        }
        
        .level-debug {
            color: var(--vscode-descriptionForeground);
        }
        
        .level-success {
            color: #89d185;
        }
        
        .message-text {
            flex: 1;
            white-space: pre-wrap;
            word-break: break-word;
        }
        
        .line-number {
            color: #ce9178;
            font-style: italic;
            flex-shrink: 0;
        }
        
        .empty-state {
            display: flex;
            align-items: center;
            justify-content: center;
            height: 100%;
            color: var(--vscode-descriptionForeground);
            font-style: italic;
        }
    </style>
</head>
<body>
    <div id="toolbar">
        <button onclick="clearConsole()">Clear</button>
        <button onclick="exportLogs()">Export</button>
        <button onclick="toggleAutoscroll()">Auto-scroll: <span id="autoscroll-status">ON</span></button>
        <div class="toolbar-spacer"></div>
        <span class="status" id="message-count">0 messages</span>
    </div>
    
    <div id="console">
        <div class="empty-state" id="empty-state">
            Waiting for script output...
        </div>
    </div>
    
    <script>
        const vscode = acquireVsCodeApi();
        const consoleDiv = document.getElementById('console');
        const emptyState = document.getElementById('empty-state');
        const messageCount = document.getElementById('message-count');
        const autoscrollStatus = document.getElementById('autoscroll-status');
        let autoscroll = true;
        let count = 0;
        
        window.addEventListener('message', event => {
            const message = event.data;
            
            switch (message.command) {
                case 'addMessage':
                    addMessage(message.message);
                    break;
                case 'clear':
                    consoleDiv.innerHTML = '<div class="empty-state" id="empty-state">Console cleared</div>';
                    count = 0;
                    updateCount();
                    break;
            }
        });
        
        function addMessage(msg) {
            // Hide empty state
            const empty = document.getElementById('empty-state');
            if (empty) empty.remove();
            
            const messageDiv = document.createElement('div');
            messageDiv.className = 'message';
            
            const timestamp = new Date(msg.timestamp).toLocaleTimeString();
            const taskId = msg.taskId ? '#' + msg.taskId : '-';
            const level = (msg.level || 'info').toUpperCase();
            const levelClass = 'level-' + (msg.level || 'info').toLowerCase();
            
            let html = 
                '<span class="timestamp">' + timestamp + '</span>' +
                '<span class="task-id">' + taskId + '</span>' +
                '<span class="level ' + levelClass + '">' + level + '</span>' +
                '<span class="message-text">' + escapeHtml(msg.message) + '</span>';
            
            if (msg.line) {
                html += '<span class="line-number">Line ' + msg.line + '</span>';
            }
            
            messageDiv.innerHTML = html;
            consoleDiv.appendChild(messageDiv);
            
            count++;
            updateCount();
            
            if (autoscroll) {
                consoleDiv.scrollTop = consoleDiv.scrollHeight;
            }
        }
        
        function updateCount() {
            messageCount.textContent = count + ' message' + (count !== 1 ? 's' : '');
        }
        
        function clearConsole() {
            vscode.postMessage({ command: 'clear' });
        }
        
        function exportLogs() {
            vscode.postMessage({ command: 'export' });
        }
        
        function toggleAutoscroll() {
            autoscroll = !autoscroll;
            autoscrollStatus.textContent = autoscroll ? 'ON' : 'OFF';
        }
        
        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }
    </script>
</body>
</html>`;
    }
    
    public dispose(): void {
        KashubConsolePanel.currentPanel = undefined;
        this._panel.dispose();
        
        while (this._disposables.length) {
            const disposable = this._disposables.pop();
            if (disposable) {
                disposable.dispose();
            }
        }
    }
}
