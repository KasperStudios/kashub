import * as vscode from 'vscode';
import WebSocket from 'ws';
import * as path from 'path';
import * as fs from 'fs';

/**
 * Kashub Debug Adapter
 * Translates VSCode DAP messages to Kashub WebSocket JSON protocol
 */
export class KashubDebugAdapter implements vscode.DebugAdapter {
    private _sendMessage = new vscode.EventEmitter<any>();
    readonly onDidSendMessage: vscode.Event<any> = this._sendMessage.event;

    private ws: WebSocket | null = null;
    private seq = 1;
    private isConnected = false;
    private pendingBreakpoints: any[] = [];
    private launchArgs: any = null;
    private pendingRequests = new Map<number, (response: any) => void>();

    // Request ID mapping (VSCode seq -> Custom handler)
    // For simplicity in this version, we handle most things with fire-and-forget or specific event listeners

    constructor(private wsUrl: string) {
        this.connect();
    }

    private connect() {
        this.ws = new WebSocket(this.wsUrl);

        this.ws.on('open', () => {
            console.log('Connected to Kashub Debugger');
            this.isConnected = true;
            this._sendEvent('output', { category: 'console', output: 'Connected to Kashub Debugger\n' });

            // Send pending stuff if any
            if (this.launchArgs) {
                this.handleLaunchWait(this.launchArgs);
            }
        });

        this.ws.on('message', (data: string) => {
            try {
                const json = JSON.parse(data.toString());
                this.handleBackendMessage(json);
            } catch (e) {
                console.error('Failed to parse message from backend', e);
            }
        });

        this.ws.on('error', (err) => {
            console.error('WebSocket error', err);
            this._sendEvent('output', { category: 'stderr', output: `WebSocket Error: ${err.message}\n` });
        });

        this.ws.on('close', () => {
            console.log('WebSocket closed');
            this.isConnected = false;
            this._sendEvent('terminated');
        });
    }

    dispose() {
        if (this.ws) {
            this.ws.close();
        }
    }

    handleMessage(message: any): void {
        if (message.type === 'request') {
            this.dispatchRequest(message);
        }
    }

    private dispatchRequest(request: any) {
        const command = request.command;
        const args = request.arguments;
        const seq = request.seq;

        try {
            switch (command) {
                case 'initialize':
                    this.sendResponse(seq, command, {
                        supportsConfigurationDoneRequest: true,
                        supportsEvaluateForHovers: false,
                        supportsStepBack: false,
                        supportsCancelRequest: false
                    });
                    this._sendEvent('initialized');
                    break;

                case 'launch':
                    this.handleLaunch(seq, command, args);
                    break;

                case 'disconnect':
                    this.sendResponse(seq, command);
                    if (this.ws) this.ws.close();
                    break;

                case 'setBreakpoints':
                    this.handleSetBreakpoints(seq, command, args);
                    break;

                case 'configurationDone':
                    this.sendResponse(seq, command);
                    break;

                case 'threads':
                    this.sendResponse(seq, command, {
                        threads: [{ id: 1, name: "Main Thread" }]
                    });
                    break;

                case 'stackTrace':
                    this.handleStackTrace(seq, command, args);
                    break;

                case 'scopes':
                    this.handleScopes(seq, command, args);
                    break;

                case 'variables':
                    this.handleVariables(seq, command, args);
                    break;

                case 'continue':
                    this.sendBackendAction('resume');
                    this.sendResponse(seq, command, { allThreadsContinued: true });
                    break;

                case 'next':
                    this.sendBackendAction('step_over');
                    this.sendResponse(seq, command);
                    break;

                case 'stepIn':
                    this.sendBackendAction('step_into');
                    this.sendResponse(seq, command);
                    break;

                case 'pause':
                    this.sendBackendAction('pause');
                    this.sendResponse(seq, command);
                    break;

                default:
                    // Unknown command
                    this.sendResponse(seq, command, null, false, "Unknown command");
                    break;
            }
        } catch (e: any) {
            this.sendResponse(seq, command, null, false, e.message);
        }
    }

    // --- Message Handlers ---

    private handleLaunch(seq: number, command: string, args: any) {
        // We might not be connected yet. 
        if (!this.isConnected) {
            this.launchArgs = { seq, command, args };
            return;
        }
        this.handleLaunchWait({ seq, command, args });
    }

    private handleLaunchWait(req: any) {
        const args = req.args;
        const filePath = args.program;

        let code = '';
        try {
            code = fs.readFileSync(filePath, 'utf8');
        } catch (e) {
            this.sendResponse(req.seq, req.command, null, false, "Could not read file: " + filePath);
            return;
        }

        const msg = {
            type: 'launch',
            program: path.basename(filePath), // Backend uses name for ID
            code: code,
            scriptId: -1 // New script
        };

        this.sendBackend(msg);
        this.sendResponse(req.seq, req.command);
    }

    private handleSetBreakpoints(seq: number, command: string, args: any) {
        const lines = args.breakpoints.map((bp: any) => bp.line);
        const sourceName = args.source?.name || 'debug_script.kh';

        this.sendBackend({
            type: 'set_breakpoints',
            lines: lines,
            program: sourceName
        });

        // Respond immediately confirming breakpoints (optimistic)
        const breakpoints = lines.map((l: number) => ({ verified: true, line: l }));
        this.sendResponse(seq, command, { breakpoints });
    }

    private handleStackTrace(seq: number, command: string, args: any) {
        // We need to ask backend for stack trace
        // But DAP expects request-response.
        // We'll store the pending request seq and handle it when backend responds
        // For simplicity: We use specific "stackTrace_response" from backend
        this.pendingRequests.set(seq, (response: any) => {
            this.sendResponse(seq, command, response);
        });

        const scriptId = 1; // Default for now, need tracking?
        // Actually backend needs scriptId. 
        // We'll broadcast to all running scripts or track active one?
        // For MVP, ask backend and backend handles finding script.
        // Pass explicit scriptId if available.
        // VSCode usually requests stackTrace for a specific threadId.
        // ThreadId 1 = Main.

        this.sendBackend({
            type: 'stackTrace',
            scriptId: this.currentScriptId // We need to track this
        });

        // Store sequence to respond later
        this._pendingStackTraceSeq = seq;
        this._pendingStackTraceCommand = command;
    }
    private _pendingStackTraceSeq: number | null = null;
    private _pendingStackTraceCommand: string | null = null;

    private handleScopes(seq: number, command: string, args: any) {
        this.sendBackend({
            type: 'scopes',
            frameId: args.frameId
        });
        this._pendingScopesSeq = seq;
        this._pendingScopesCommand = command;
    }
    private _pendingScopesSeq: number | null = null;
    private _pendingScopesCommand: string | null = null;

    private handleVariables(seq: number, command: string, args: any) {
        this.sendBackend({
            type: 'get_variables',
            variablesReference: args.variablesReference,
            // Map ref to scriptId?
            // Simple mapping: ref 1 = local, 2 = global.
            // Backend needs scriptId.
            scriptId: this.currentScriptId
        });
        this._pendingVariablesSeq = seq;
        this._pendingVariablesCommand = command;
    }
    private _pendingVariablesSeq: number | null = null;
    private _pendingVariablesCommand: string | null = null;

    // --- Backend Message Handling ---

    private currentScriptId = -1;

    private handleBackendMessage(json: any) {
        if (!json.type) return;

        switch (json.type) {
            case 'launch_response':
                if (json.taskId) {
                    this.currentScriptId = json.taskId;
                }
                break;

            case 'debug_event':
                this.handleDebugEvent(json);
                break;

            case 'stackTrace_response':
                if (this._pendingStackTraceSeq !== null) {
                    this.sendResponse(this._pendingStackTraceSeq, this._pendingStackTraceCommand!, {
                        stackFrames: json.stackFrames || [],
                        totalFrames: (json.stackFrames || []).length
                    });
                    this._pendingStackTraceSeq = null;
                }
                break;

            case 'scopes_response':
                if (this._pendingScopesSeq !== null) {
                    this.sendResponse(this._pendingScopesSeq, this._pendingScopesCommand!, {
                        scopes: json.scopes || []
                    });
                    this._pendingScopesSeq = null;
                }
                break;

            case 'variables_response':
                if (this._pendingVariablesSeq !== null) {
                    this.sendResponse(this._pendingVariablesSeq, this._pendingVariablesCommand!, {
                        variables: this.transformVariables(json.variables)
                    });
                    this._pendingVariablesSeq = null;
                }
                break;
        }
    }

    private handleDebugEvent(json: any) {
        const event = json.event; // PAUSED, RESUMED
        this.currentScriptId = json.scriptId;

        if (event === 'PAUSED') {
            this._sendEvent('stopped', {
                reason: 'breakpoint',
                threadId: 1
            });
        } else if (event === 'RESUMED') {
            // DAP doesn't always strictly require 'continued' event but useful
            // _sendEvent('continued', { threadId: 1 });
        }
    }

    private transformVariables(varsObj: any): any[] {
        if (!varsObj) return [];
        return Object.keys(varsObj).map(key => ({
            name: key,
            value: varsObj[key],
            variablesReference: 0
        }));
    }

    // --- Helpers ---

    private sendBackendAction(action: string) {
        this.sendBackend({
            type: 'debug_action',
            action: action,
            scriptId: this.currentScriptId
        });
    }

    private sendBackend(msg: any) {
        if (this.ws && this.isConnected) {
            this.ws.send(JSON.stringify(msg));
        }
    }

    private sendResponse(seq: number, command: string, body?: any, success: boolean = true, message?: string) {
        this._sendMessage.fire({
            type: 'response',
            seq: this.seq++,
            request_seq: seq,
            command: command,
            success: success,
            message: message,
            body: body
        });
    }

    private _sendEvent(event: string, body?: any) {
        this._sendMessage.fire({
            type: 'event',
            seq: this.seq++,
            event: event,
            body: body
        });
    }
}
