package kasperstudios.kashub.gui.editor;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Менеджер истории изменений для редактора (Undo/Redo)
 */
public class UndoManager {
    
    private static final int MAX_HISTORY_SIZE = 100;
    
    private final Deque<EditorState> undoStack = new ArrayDeque<>();
    private final Deque<EditorState> redoStack = new ArrayDeque<>();
    
    private String lastSavedContent = "";
    private long lastChangeTime = 0;
    private static final long MERGE_THRESHOLD = 500; // мс - объединять быстрые изменения
    
    /**
     * Записывает состояние редактора для возможности отмены
     */
    public void recordState(String content, int cursorPosition) {
        long now = System.currentTimeMillis();
        
        // Объединяем быстрые последовательные изменения
        if (!undoStack.isEmpty() && (now - lastChangeTime) < MERGE_THRESHOLD) {
            // Обновляем последнее состояние вместо добавления нового
            EditorState last = undoStack.peek();
            if (last != null && isSimilarChange(last.content, content)) {
                undoStack.pop();
            }
        }
        
        // Добавляем новое состояние
        undoStack.push(new EditorState(content, cursorPosition, now));
        
        // Очищаем redo при новом изменении
        redoStack.clear();
        
        // Ограничиваем размер истории
        while (undoStack.size() > MAX_HISTORY_SIZE) {
            ((ArrayDeque<EditorState>) undoStack).removeLast();
        }
        
        lastChangeTime = now;
    }
    
    /**
     * Отменяет последнее изменение (Ctrl+Z)
     * @return предыдущее состояние или null если история пуста
     */
    public EditorState undo(String currentContent, int currentCursor) {
        if (undoStack.isEmpty()) {
            return null;
        }
        
        // Сохраняем текущее состояние в redo
        redoStack.push(new EditorState(currentContent, currentCursor, System.currentTimeMillis()));
        
        // Возвращаем предыдущее состояние
        return undoStack.pop();
    }
    
    /**
     * Повторяет отменённое изменение (Ctrl+Y / Ctrl+Shift+Z)
     * @return следующее состояние или null если redo пуст
     */
    public EditorState redo(String currentContent, int currentCursor) {
        if (redoStack.isEmpty()) {
            return null;
        }
        
        // Сохраняем текущее состояние в undo
        undoStack.push(new EditorState(currentContent, currentCursor, System.currentTimeMillis()));
        
        // Возвращаем следующее состояние
        return redoStack.pop();
    }
    
    /**
     * Проверяет, можно ли отменить
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    /**
     * Проверяет, можно ли повторить
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    /**
     * Очищает историю
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        lastSavedContent = "";
    }
    
    /**
     * Отмечает текущее состояние как сохранённое
     */
    public void markSaved(String content) {
        lastSavedContent = content;
    }
    
    /**
     * Проверяет, есть ли несохранённые изменения
     */
    public boolean hasUnsavedChanges(String currentContent) {
        return !currentContent.equals(lastSavedContent);
    }
    
    /**
     * Получает количество шагов в истории undo
     */
    public int getUndoCount() {
        return undoStack.size();
    }
    
    /**
     * Получает количество шагов в истории redo
     */
    public int getRedoCount() {
        return redoStack.size();
    }
    
    /**
     * Проверяет, похожи ли изменения (для объединения)
     */
    private boolean isSimilarChange(String old, String current) {
        // Объединяем, если изменение небольшое (1-3 символа)
        int diff = Math.abs(old.length() - current.length());
        return diff <= 3;
    }
    
    /**
     * Состояние редактора
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
