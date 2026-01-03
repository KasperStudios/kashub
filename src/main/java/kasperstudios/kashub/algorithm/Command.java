package kasperstudios.kashub.algorithm;

import java.util.concurrent.CompletableFuture;

public interface Command {

    String getName();

    String getDescription();

    String getParameters();

    default String getDetailedHelp() {
        return "";
    }

    default String getCategory() {
        return "Other";
    }

    void execute(String[] args) throws Exception;

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