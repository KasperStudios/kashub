import * as vscode from 'vscode';
import { KashubClient, CompletionItem as KashubCompletionItem } from './kashubClient';

export class KashubCompletionProvider implements vscode.CompletionItemProvider {
    
    constructor(private kashubClient: KashubClient) {}
    
    async provideCompletionItems(
        document: vscode.TextDocument,
        position: vscode.Position,
        token: vscode.CancellationToken
    ): Promise<vscode.CompletionItem[]> {
        const linePrefix = document.lineAt(position).text.substring(0, position.character);
        const match = linePrefix.match(/(\$?[\w]+)$/);
        const prefix = match ? match[1] : '';
        
        const code = document.getText();
        
        try {
            const items = await this.kashubClient.getCompletions(
                code,
                prefix,
                position.line,
                position.character
            );
            
            return items.map(item => this.convertItem(item));
        } catch (error) {
            console.error('Completion error:', error);
            return [];
        }
    }
    
    private convertItem(item: KashubCompletionItem): vscode.CompletionItem {
        const completionItem = new vscode.CompletionItem(
            item.label,
            this.mapKind(item.kind)
        );
        
        completionItem.detail = item.detail;
        
        if (item.documentation) {
            completionItem.documentation = new vscode.MarkdownString(item.documentation);
        }
        
        // Check if insertText contains snippet placeholders
        if (item.insertText.includes('${')) {
            completionItem.insertText = new vscode.SnippetString(item.insertText);
        } else {
            completionItem.insertText = item.insertText;
        }
        
        return completionItem;
    }
    
    private mapKind(kind: string): vscode.CompletionItemKind {
        switch (kind) {
            case 'Function': return vscode.CompletionItemKind.Function;
            case 'Keyword': return vscode.CompletionItemKind.Keyword;
            case 'Variable': return vscode.CompletionItemKind.Variable;
            case 'Snippet': return vscode.CompletionItemKind.Snippet;
            default: return vscode.CompletionItemKind.Text;
        }
    }
}
