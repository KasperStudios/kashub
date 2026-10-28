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

export interface DebugEvent {
    type: 'debug_event';
    event: 'PAUSED' | 'RESUMED' | 'BREAKPOINT_HIT' | 'STEP_COMPLETE';
    scriptId: number;
    line: number;
}

export interface VariablesResponse {
    type: 'variables_response';
    scriptId: number;
    variables: Record<string, string>;
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
    private debugEventHandlers: EventHandler<DebugEvent>[] = [];
    private variablesResponseHandlers: EventHandler<VariablesResponse>[] = [];
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
            case 'debug_event':
                this.debugEventHandlers.forEach(h => h(event as DebugEvent));
                break;
            case 'variables_response':
                this.variablesResponseHandlers.forEach(h => h(event as VariablesResponse));
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

    onDebugEvent(handler: EventHandler<DebugEvent>): void {
        this.debugEventHandlers.push(handler);
    }

    onVariablesResponse(handler: EventHandler<VariablesResponse>): void {
        this.variablesResponseHandlers.push(handler);
    }

    sendDebugAction(action: 'resume' | 'pause' | 'step_over' | 'step_into' | 'step_out', scriptId?: number) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify({
                type: 'debug_action',
                action,
                scriptId: scriptId ?? -1
            }));
        }
    }

    setBreakpoints(lines: number[]) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify({
                type: 'set_breakpoints',
                lines
            }));
        }
    }

    requestVariables(scriptId: number) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify({
                type: 'get_variables',
                scriptId
            }));
        }
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
        column: number,
        objectContext?: string
    ): Promise<CompletionItem[]> {
        if (!this.connected) {
            return this.offlineCompletions(prefix, objectContext);
        }

        try {
            const response = await this.http.post('/api/autocomplete', {
                code,
                prefix,
                line,
                column,
                objectContext
            });
            return response.data.items;
        } catch (error) {
            return this.offlineCompletions(prefix, objectContext);
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

    async getCommands(): Promise<any[]> {
        if (!this.connected) return [];

        try {
            const response = await this.http.get('/api/commands');
            return response.data.commands || [];
        } catch (error) {
            console.error('Failed to fetch commands:', error);
            return [];
        }
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

    private offlineCompletions(prefix: string, objectContext?: string): CompletionItem[] {
        const OBJECT_METHODS: Record<string, string[]> = {
            player: [
                'getPos()', 'getHealth()', 'getHunger()', 'getName()',
                'moveTo(x, y, z)', 'lookAt(x, y, z)', 'attack(target)',
                'breakBlock()', 'placeBlock(name)', 'chat(msg)',
                'sprint(enabled)', 'sneak(enabled)', 'jump()',
                'getInventory()', 'hasImplant()', 'getImplantTier()'
            ],
            System: [
                'print(msg)', 'log(msg)', 'wait(ms)', 'time()',
                'exit()', 'memory()', 'gc()', 'openFile(path)', 'console(cmd)'
            ],
            scanner: [
                'blocks(filter, radius)', 'entities(filter, radius)',
                'block(x, y, z)', 'nearest(filter, radius)'
            ],
            vision: [
                'nearest(filter, maxDist)', 'entities()', 'players()',
                'mobs()', 'items()', 'canSee(entity)'
            ],
            inventory: [
                'check()', 'has(item)', 'count(item)',
                'select(slot)', 'drop(slot)', 'getItems()', 'getEmptySlots()'
            ],
            world: [
                'getBlock(x, y, z)', 'getBiome(x, y, z)', 'getTime()',
                'isRaining()', 'getDimension()'
            ],
            game: [
                'setGamma(val)', 'fullBright(enabled)', 'setFov(val)'
            ],
            VFS: [
                'read(path)', 'write(path, data)', 'list(path)',
                'exists(path)', 'delete(path)', 'mkdir(path)'
            ],
            Terminal: [
                'execute(command)', 'openFile(path)', 'hasSudo()', 'hasSu()'
            ],
            Math: [
                'sqrt(x)', 'abs(x)', 'min(a, b)', 'max(a, b)',
                'floor(x)', 'ceil(x)', 'round(x)', 'random(max)', 'pow(base, exp)'
            ],
            tag: [
                'itemHas(item, tag)', 'itemsWith(tag)', 'blockHas(block, tag)'
            ]
        };

        const lowerPrefix = prefix.toLowerCase();

        // If user typed 'object.', show methods for this object
        if (objectContext && OBJECT_METHODS[objectContext]) {
            return OBJECT_METHODS[objectContext]
                .filter(m => m.toLowerCase().startsWith(lowerPrefix))
                .map(m => {
                    const label = m.substring(0, m.indexOf('('));
                    return {
                        label,
                        kind: 'Function',
                        detail: `${objectContext}.${m}`,
                        documentation: `Method of ${objectContext}`,
                        insertText: m.endsWith('()') ? `${label}()` : `${label}(`
                    };
                });
        }

        // Global symbols (objects, keywords, commands)
        const GLOBAL_SYMBOLS: Array<{ label: string; kind: string; detail: string; insertText: string }> = [
            // Objects
            { label: 'System', kind: 'Variable', detail: 'Global System object', insertText: 'System.' },
            { label: 'player', kind: 'Variable', detail: 'Local Player object', insertText: 'player.' },
            { label: 'scanner', kind: 'Variable', detail: 'Block and Entity scanner', insertText: 'scanner.' },
            { label: 'vision', kind: 'Variable', detail: 'Visual entity tracker', insertText: 'vision.' },
            { label: 'inventory', kind: 'Variable', detail: 'Player inventory manager', insertText: 'inventory.' },
            { label: 'world', kind: 'Variable', detail: 'World state and environment', insertText: 'world.' },
            { label: 'game', kind: 'Variable', detail: 'Game settings and visual tweaks', insertText: 'game.' },
            { label: 'VFS', kind: 'Variable', detail: 'Virtual File System', insertText: 'VFS.' },
            { label: 'Terminal', kind: 'Variable', detail: 'Khinux Terminal interface', insertText: 'Terminal.' },
            { label: 'Math', kind: 'Variable', detail: 'Math utilities', insertText: 'Math.' },
            { label: 'tag', kind: 'Variable', detail: 'Minecraft Tag inspector', insertText: 'tag.' },

            // Control flow keywords
            { label: 'fn', kind: 'Keyword', detail: 'Function declaration', insertText: 'fn ${1:name}(${2:params}) {\n\t$0\n}' },
            { label: 'let', kind: 'Keyword', detail: 'Variable declaration', insertText: 'let ${1:name} = ${2:value}' },
            { label: 'const', kind: 'Keyword', detail: 'Constant declaration', insertText: 'const ${1:NAME} = ${2:value}' },
            { label: 'if', kind: 'Keyword', detail: 'If conditional statement', insertText: 'if (${1:condition}) {\n\t$0\n}' },
            { label: 'else', kind: 'Keyword', detail: 'Else conditional branch', insertText: 'else {\n\t$0\n}' },
            { label: 'while', kind: 'Keyword', detail: 'While loop', insertText: 'while (${1:condition}) {\n\t$0\n}' },
            { label: 'for', kind: 'Keyword', detail: 'For loop', insertText: 'for (let ${1:i} = 0; ${1:i} < ${2:10}; ${1:i} = ${1:i} + 1) {\n\t$0\n}' },
            { label: 'break', kind: 'Keyword', detail: 'Break loop execution', insertText: 'break' },
            { label: 'continue', kind: 'Keyword', detail: 'Continue loop execution', insertText: 'continue' },
            { label: 'return', kind: 'Keyword', detail: 'Return from function', insertText: 'return ' },
            { label: 'crashguard', kind: 'Keyword', detail: 'Protected CrashGuard block', insertText: 'crashguard(timeout=${1:5000}, minFps=${2:25}) {\n\t$0\n}' },

            // Global helper functions
            { label: 'print', kind: 'Function', detail: 'Print to chat and logger', insertText: 'print("${1:message}")' },
            { label: 'log', kind: 'Function', detail: 'Log message to console', insertText: 'log("${1:message}")' },
            { label: 'wait', kind: 'Function', detail: 'Wait milliseconds', insertText: 'wait(${1:1000})' },
            { label: 'chat', kind: 'Function', detail: 'Send player chat message', insertText: 'chat("${1:message}")' },
            { label: 'time', kind: 'Function', detail: 'Current epoch time in ms', insertText: 'time()' }
        ];

        return GLOBAL_SYMBOLS
            .filter(s => s.label.toLowerCase().startsWith(lowerPrefix))
            .map(s => ({
                label: s.label,
                kind: s.kind,
                detail: s.detail,
                documentation: '',
                insertText: s.insertText
            }));
    }
}
