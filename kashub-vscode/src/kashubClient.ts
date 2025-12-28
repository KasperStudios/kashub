import axios, { AxiosInstance } from 'axios';
import WebSocket from 'ws';
import * as vscode from 'vscode';

export interface ScriptOutputEvent {
    type: 'script_output';
    taskId: number;
    message: string;
    level: string;
    timestamp: number;
}

export interface ScriptErrorEvent {
    type: 'script_error';
    taskId: number;
    error: string;
    line: number;
    timestamp: number;
}

export interface TaskStateChangeEvent {
    type: 'task_state_change';
    taskId: number;
    taskName: string;
    state: string;
    timestamp: number;
}

export interface ValidationError {
    line: number;
    column: number;
    message: string;
    severity: 'error' | 'warning' | 'info';
}

export interface ValidationResult {
    valid: boolean;
    errors: ValidationError[];
    errorCount: number;
    warningCount: number;
}

export interface CompletionItem {
    label: string;
    kind: string;
    detail: string;
    documentation: string;
    insertText: string;
}

export interface RunResult {
    success: boolean;
    taskId?: number;
    message?: string;
    error?: string;
}

export interface TaskInfo {
    id: number;
    name: string;
    state: string;
    uptime: number;
    scriptType: string;
    lastError?: string;
    tags?: string[];
}

type EventHandler<T> = (event: T) => void;

export class KashubClient {
    private http: AxiosInstance;
    private ws?: WebSocket;
    private baseUrl: string;
    private wsUrl: string;
    public connected: boolean = false;
    
    private outputHandlers: EventHandler<ScriptOutputEvent>[] = [];
    private errorHandlers: EventHandler<ScriptErrorEvent>[] = [];
    private stateChangeHandlers: EventHandler<TaskStateChangeEvent>[] = [];
    private reconnectTimer?: NodeJS.Timeout;
    
    constructor() {
        const config = vscode.workspace.getConfiguration('kashub');
        this.baseUrl = config.get('apiUrl', 'http://localhost:25566');
        this.wsUrl = config.get('wsUrl', 'ws://localhost:25567');
        
        this.http = axios.create({
            baseURL: this.baseUrl,
            timeout: 5000,
            headers: {
                'Content-Type': 'application/json'
            }
        });
    }
    
    async connect(): Promise<boolean> {
        try {
            // Test HTTP connection
            const response = await this.http.get('/api/status');
            this.connected = response.data.status === 'running';
            
            if (this.connected) {
                // Connect WebSocket
                this.connectWebSocket();
            }
            
            return this.connected;
        } catch (error) {
            this.connected = false;
            return false;
        }
    }
    
    disconnect(): void {
        if (this.ws) {
            this.ws.close();
            this.ws = undefined;
        }
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = undefined;
        }
        this.connected = false;
    }
    
    private connectWebSocket(): void {
        if (this.ws) {
            this.ws.close();
        }
        
        try {
            this.ws = new WebSocket(this.wsUrl);
            
            this.ws.on('open', () => {
                console.log('Kashub WebSocket connected');
            });
            
            this.ws.on('message', (data: WebSocket.Data) => {
                try {
                    const event = JSON.parse(data.toString());
                    this.handleEvent(event);
                } catch (e) {
                    console.error('Failed to parse WebSocket message:', e);
                }
            });
            
            this.ws.on('error', (error) => {
                console.error('Kashub WebSocket error:', error);
            });
            
            this.ws.on('close', () => {
                console.log('Kashub WebSocket disconnected');
                // Reconnect after 5 seconds
                if (this.connected) {
                    this.reconnectTimer = setTimeout(() => this.connectWebSocket(), 5000);
                }
            });
        } catch (error) {
            console.error('Failed to connect WebSocket:', error);
        }
    }
    
    private handleEvent(event: any): void {
        switch (event.type) {
            case 'script_output':
                this.outputHandlers.forEach(h => h(event as ScriptOutputEvent));
                break;
            case 'script_error':
                this.errorHandlers.forEach(h => h(event as ScriptErrorEvent));
                break;
            case 'task_state_change':
                this.stateChangeHandlers.forEach(h => h(event as TaskStateChangeEvent));
                break;
        }
    }
    
    onOutput(handler: EventHandler<ScriptOutputEvent>): void {
        this.outputHandlers.push(handler);
    }
    
    onError(handler: EventHandler<ScriptErrorEvent>): void {
        this.errorHandlers.push(handler);
    }
    
    onStateChange(handler: EventHandler<TaskStateChangeEvent>): void {
        this.stateChangeHandlers.push(handler);
    }
    
    async validate(code: string): Promise<ValidationResult> {
        if (!this.connected) {
            return this.offlineValidation(code);
        }
        
        try {
            const response = await this.http.post('/api/validate', { code });
            return response.data;
        } catch (error) {
            return this.offlineValidation(code);
        }
    }
    
    async getCompletions(
        code: string,
        prefix: string,
        line: number,
        column: number
    ): Promise<CompletionItem[]> {
        if (!this.connected) {
            return this.offlineCompletions(prefix);
        }
        
        try {
            const response = await this.http.post('/api/autocomplete', {
                code,
                prefix,
                line,
                column
            });
            return response.data.items;
        } catch (error) {
            return this.offlineCompletions(prefix);
        }
    }
    
    async runScript(code: string, filename?: string): Promise<RunResult> {
        if (!this.connected) {
            throw new Error('Not connected to Kashub');
        }
        
        const response = await this.http.post('/api/run', {
            code,
            filename
        });
        
        return response.data;
    }
    
    async getTasks(): Promise<TaskInfo[]> {
        if (!this.connected) return [];
        
        const response = await this.http.get('/api/tasks');
        return response.data.tasks;
    }
    
    async stopTask(taskId: number): Promise<void> {
        await this.http.post(`/api/tasks/${taskId}/stop`);
    }
    
    async pauseTask(taskId: number): Promise<void> {
        await this.http.post(`/api/tasks/${taskId}/pause`);
    }
    
    async resumeTask(taskId: number): Promise<void> {
        await this.http.post(`/api/tasks/${taskId}/resume`);
    }
    
    async getVariables(): Promise<Record<string, string>> {
        if (!this.connected) return {};
        
        const response = await this.http.get('/api/variables');
        return response.data.variables;
    }
    
    async getStatus(): Promise<any> {
        const response = await this.http.get('/api/status');
        return response.data;
    }
    
    private offlineValidation(code: string): ValidationResult {
        const errors: ValidationError[] = [];
        const lines = code.split('\n');
        let braceDepth = 0;
        
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i].trim();
            if (line.startsWith('//') || line === '') continue;
            
            braceDepth += (line.match(/\{/g) || []).length;
            braceDepth -= (line.match(/\}/g) || []).length;
            
            if (braceDepth < 0) {
                errors.push({
                    line: i + 1,
                    column: 0,
                    message: 'Unexpected closing brace',
                    severity: 'error'
                });
                braceDepth = 0;
            }
        }
        
        if (braceDepth > 0) {
            errors.push({
                line: lines.length,
                column: 0,
                message: `Unclosed brace(s): ${braceDepth} remaining`,
                severity: 'error'
            });
        }
        
        return {
            valid: errors.length === 0,
            errors,
            errorCount: errors.filter(e => e.severity === 'error').length,
            warningCount: errors.filter(e => e.severity === 'warning').length
        };
    }
    
    private offlineCompletions(prefix: string): CompletionItem[] {
        const COMMANDS = [
            'print', 'log', 'wait', 'jump', 'run', 'moveTo', 'lookAt',
            'chat', 'attack', 'eat', 'equipArmor', 'sneak', 'sprint',
            'drop', 'selectSlot', 'breakBlock', 'placeBlock', 'getBlock',
            'tp', 'onEvent', 'interact', 'swim', 'stop', 'loop',
            'scanner', 'vision', 'input', 'animation', 'fullbright',
            'scripts', 'eval', 'ai', 'sound', 'autoCraft', 'autoTrade'
        ];
        
        const lowerPrefix = prefix.toLowerCase();
        return COMMANDS
            .filter(c => c.toLowerCase().startsWith(lowerPrefix))
            .map(c => ({
                label: c,
                kind: 'Function',
                detail: 'Basic command (offline)',
                documentation: '',
                insertText: c + ' '
            }));
    }
}
