package kasperstudios.kashub.algorithm;

import java.util.concurrent.CompletableFuture;

/**
 * Интерфейс для определения команд скрипта KHScript.
 */
public interface Command {
    /**
     * Возвращает имя команды
     */
    String getName();

    /**
     * Возвращает описание команды
     */
    String getDescription();

    /**
     * Возвращает строку с описанием параметров команды
     */
    String getParameters();

    /**
     * Returns detailed help for the command (examples, argument types, etc.)
     */
    default String getDetailedHelp() {
        return "";
    }
    
    /**
     * Returns command category for documentation grouping
     */
    default String getCategory() {
        return "Other";
    }

    /**
     * Синхронное выполнение команды
     */
    void execute(String[] args) throws Exception;

    /**
     * Асинхронное выполнение команды
     */
    default CompletableFuture<Void> executeAsync(String[] args) {
        return CompletableFuture.runAsync(() -> {
            try {
                execute(args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}