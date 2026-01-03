package kasperstudios.kashub.gui.editor;

import java.util.ArrayDeque;
import java.util.Deque;

public class UndoManager {

    private static final int MAX_HISTORY_SIZE = 100;

    private final Deque<EditorState> undoStack = new ArrayDeque<>();
    private final Deque<EditorState> redoStack = new ArrayDeque<>();

    private String lastSavedContent = "";
    private long lastChangeTime = 0;
    private static final long MERGE_THRESHOLD = 500;

    public void recordState(String content, int cursorPosition) {
        long now = System.currentTimeMillis();

        if (!undoStack.isEmpty() && (now - lastChangeTime) < MERGE_THRESHOLD) {

            EditorState last = undoStack.peek();
            if (last != null && isSimilarChange(last.content, content)) {
                undoStack.pop();
            }
        }

        undoStack.push(new EditorState(content, cursorPosition, now));

        redoStack.clear();

        while (undoStack.size() > MAX_HISTORY_SIZE) {
            ((ArrayDeque<EditorState>) undoStack).removeLast();
        }

        lastChangeTime = now;
    }

    public EditorState undo(String currentContent, int currentCursor) {
        if (undoStack.isEmpty()) {
            return null;
        }

        redoStack.push(new EditorState(currentContent, currentCursor, System.currentTimeMillis()));

        return undoStack.pop();
    }

    public EditorState redo(String currentContent, int currentCursor) {
        if (redoStack.isEmpty()) {
            return null;
        }

        undoStack.push(new EditorState(currentContent, currentCursor, System.currentTimeMillis()));

        return redoStack.pop();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        lastSavedContent = "";
    }

    public void markSaved(String content) {
        lastSavedContent = content;
    }

    public boolean hasUnsavedChanges(String currentContent) {
        return !currentContent.equals(lastSavedContent);
    }

    public int getUndoCount() {
        return undoStack.size();
    }

    public int getRedoCount() {
        return redoStack.size();
    }

    private boolean isSimilarChange(String old, String current) {

        int diff = Math.abs(old.length() - current.length());
        return diff <= 3;
    }

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
