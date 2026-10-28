package kasperstudios.kashub.services;

/**
 * AI Service Client - Moved to standalone KasAgent mod.
 */
public class KasHubAiClient {
    private static KasHubAiClient instance;

    public static KasHubAiClient getInstance() {
        if (instance == null) instance = new KasHubAiClient();
        return instance;
    }

    private KasHubAiClient() {}

    public void query(String prompt, java.util.function.Consumer<String> callback) {
        callback.accept("AI Agent is now available via the standalone KasAgent mod.");
    }
}
