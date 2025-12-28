import * as vscode from 'vscode';
import { KashubClient } from './kashubClient';

export class KashubStatusBar implements vscode.Disposable {
    private statusBarItem: vscode.StatusBarItem;
    private updateInterval?: NodeJS.Timeout;
    
    constructor(private kashubClient: KashubClient) {
        this.statusBarItem = vscode.window.createStatusBarItem(
            vscode.StatusBarAlignment.Right,
            100
        );
        this.statusBarItem.command = 'kashub.reconnect';
        this.statusBarItem.show();
        
        this.updateStatus();
        
        // Update status every 5 seconds
        this.updateInterval = setInterval(() => this.updateStatus(), 5000);
    }
    
    private async updateStatus(): Promise<void> {
        if (this.kashubClient.connected) {
            try {
                const status = await this.kashubClient.getStatus();
                const taskCount = status.tasks?.running || 0;
                
                this.statusBarItem.text = `$(check) Kashub`;
                if (taskCount > 0) {
                    this.statusBarItem.text += ` (${taskCount} running)`;
                }
                this.statusBarItem.backgroundColor = undefined;
                this.statusBarItem.tooltip = this.formatTooltip(status);
            } catch (error) {
                // Connection lost
                this.kashubClient.connected = false;
                this.showDisconnected();
            }
        } else {
            // Try to reconnect
            const connected = await this.kashubClient.connect();
            if (connected) {
                this.updateStatus();
            } else {
                this.showDisconnected();
            }
        }
    }
    
    private showDisconnected(): void {
        this.statusBarItem.text = '$(circle-slash) Kashub';
        this.statusBarItem.backgroundColor = new vscode.ThemeColor(
            'statusBarItem.warningBackground'
        );
        this.statusBarItem.tooltip = 'Not connected to Kashub\nStart Minecraft with Kashub mod\nClick to retry';
    }
    
    private formatTooltip(status: any): string {
        const lines = [
            `Kashub ${status.version}`,
            '',
            status.inWorld ? `Player: ${status.player?.name}` : 'Not in world',
        ];
        
        if (status.inWorld && status.player) {
            lines.push(`Health: ${status.player.health}/${status.player.maxHealth}`);
            lines.push(`Position: ${Math.floor(status.player.x)}, ${Math.floor(status.player.y)}, ${Math.floor(status.player.z)}`);
        }
        
        if (status.tasks) {
            lines.push('');
            lines.push(`Tasks: ${status.tasks.running} running, ${status.tasks.total} total`);
        }
        
        lines.push('');
        lines.push('Click to reconnect');
        
        return lines.join('\n');
    }
    
    dispose(): void {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
        }
        this.statusBarItem.dispose();
    }
}
