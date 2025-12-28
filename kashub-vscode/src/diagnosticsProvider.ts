import * as vscode from 'vscode';
import { KashubClient } from './kashubClient';

export class KashubDiagnosticsProvider implements vscode.Disposable {
    private diagnosticCollection: vscode.DiagnosticCollection;
    private debounceTimer?: NodeJS.Timeout;
    
    constructor(private kashubClient: KashubClient) {
        this.diagnosticCollection = vscode.languages.createDiagnosticCollection('khscript');
    }
    
    async updateDiagnostics(document: vscode.TextDocument): Promise<void> {
        if (document.languageId !== 'khscript') {
            return;
        }
        
        // Debounce to avoid too many requests
        if (this.debounceTimer) {
            clearTimeout(this.debounceTimer);
        }
        
        this.debounceTimer = setTimeout(async () => {
            await this.doUpdateDiagnostics(document);
        }, 300);
    }
    
    private async doUpdateDiagnostics(document: vscode.TextDocument): Promise<void> {
        const code = document.getText();
        
        try {
            const result = await this.kashubClient.validate(code);
            
            const diagnostics = result.errors.map(error => {
                const line = Math.max(0, error.line - 1);
                const lineText = document.lineAt(line).text;
                const startCol = error.column || 0;
                const endCol = lineText.length;
                
                const range = new vscode.Range(
                    line,
                    startCol,
                    line,
                    endCol
                );
                
                const severity = this.mapSeverity(error.severity);
                
                const diagnostic = new vscode.Diagnostic(
                    range,
                    error.message,
                    severity
                );
                
                diagnostic.source = 'Kashub';
                
                return diagnostic;
            });
            
            this.diagnosticCollection.set(document.uri, diagnostics);
        } catch (error) {
            console.error('Diagnostics error:', error);
        }
    }
    
    private mapSeverity(severity: string): vscode.DiagnosticSeverity {
        switch (severity) {
            case 'error': return vscode.DiagnosticSeverity.Error;
            case 'warning': return vscode.DiagnosticSeverity.Warning;
            case 'info': return vscode.DiagnosticSeverity.Information;
            default: return vscode.DiagnosticSeverity.Hint;
        }
    }
    
    dispose(): void {
        if (this.debounceTimer) {
            clearTimeout(this.debounceTimer);
        }
        this.diagnosticCollection.dispose();
    }
}
