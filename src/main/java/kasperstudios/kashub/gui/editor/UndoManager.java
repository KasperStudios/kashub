package kasperstudios.kashub.gui.editor;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Change history manager for editor (Undo/Redo)
 */
public class UndoManager {
    
    private static final int MAX_HISTORY_SIZE = 100;
    
    private final Deque<EditorState> undoStack = new ArrayDeque<>();
    private final Deque<EditorState> redoStack = new ArrayDeque<>();
    
    private String lastSavedContent = "";
    private long lastChangeTime = 0;
    private static final long MERGE_THRESHOLD = 500; // ms - merge rapid changes
    
    /**
     * Record editor state for undo capability
     */
    public void recordState(String content, int cursorPosition) {
        long now = System.currentTimeMillis();
        
        // Merge rapid consecutive changes
        if (!undoStack.isEmpty() && (now - lastChangeTime) < MERGE_THRESHOLD) {
            // Update last state instead of adding new one
            EditorState last = undoStack.peek();
            if (last != null && isSimilarChange(last.content, content)) {
                undoStack.pop();
            }
        }
        
        // Add new state
        undoStack.push(new EditorState(content, cursorPosition, now));
        
        // Clear redo on new change
        redoStack.clear();
        
        // Limit history size
        while (undoStack.size() > MAX_HISTORY_SIZE) {
            ((ArrayDeque<EditorState>) undoStack).removeLast();
        }
        
        lastChangeTime = now;
    }
    
    /**
     * Undo last change (Ctrl+Z)
     * @return previous state or null if history is empty
     */
    public EditorState undo(String currentContent, int currentCursor) {
        if (undoStack.isEmpty()) {
            return null;
        }
        
        // Save current state to redo
        redoStack.push(new EditorState(currentContent, currentCursor, System.currentTimeMillis()));
        
        // Return previous state
        return undoStack.pop();
    }
    
    /**
     * Redo undone change (Ctrl+Y / Ctrl+Shift+Z)
     * @return next state or null if redo is empty
     */
    public EditorState redo(String currentContent, int currentCursor) {
        if (redoStack.isEmpty()) {
            return null;
        }
        
        // Save current state to undo
        undoStack.push(new EditorState(currentContent, currentCursor, System.currentTimeMillis()));
        
        // Return next state
        return redoStack.pop();
    }
    
    /**
     * Check if undo is possible
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    /**
     * Check if redo is possible
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    /**
     * Clear history
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        lastSavedContent = "";
    }
    
    /**
     * Mark current state as saved
     */
    public void markSaved(String content) {
        lastSavedContent = content;
    }
    
    /**
     * Check if there are unsaved changes
     */
    public boolean hasUnsavedChanges(String currentContent) {
        return !currentContent.equals(lastSavedContent);
    }
    
    /**
     * Get undo history step count
     */
    public int getUndoCount() {
        return undoStack.size();
    }
    
    /**
     * Get redo history step count
     */
    public int getRedoCount() {
        return redoStack.size();
    }
    
    /**
     * Check if changes are similar (for merging)
     */
    private boolean isSimilarChange(String old, String current) {
        // Merge if change is small (1-3 characters)
        int diff = Math.abs(old.length() - current.length());
        return diff <= 3;
    }
    
    /**
     * Editor state
     */
    public static class EditorState {
        public final String content;
        public final int cursorPosition;
        public final long timestamp;
        
        public EditorState(String content, int cursorPosition, long timestamp) {
            this.content = content;
            this.cursorPosition = cursorPosition;
            this.timestamp = timestamp;
        }
    }
}
