package kasperstudios.kashub.marketplace;

import java.time.Instant;
import java.util.List;

/**
 * Represents a verified script from the Kashub Script Marketplace.
 * 
 * Verified scripts are:
 * - Reviewed for safety and quality
 * - Signed with a verification signature
 * - Hosted on the official GitHub repository
 * - Versioned and updatable
 */
public class VerifiedScript {
    private final String id;
    private final String name;
    private final String description;
    private final String author;
    private final String version;
    private final String category;
    private final List<String> tags;
    private final String downloadUrl;
    private final String signature;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final int downloads;
    private final double rating;
    private final int ratingCount;
    
    /**
     * Create a new VerifiedScript instance.
     */
    public VerifiedScript(
            String id,
            String name,
            String description,
            String author,
            String version,
            String category,
            List<String> tags,
            String downloadUrl,
            String signature,
            Instant createdAt,
            Instant updatedAt,
            int downloads,
            double rating,
            int ratingCount
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.author = author;
        this.version = version;
        this.category = category;
        this.tags = tags;
        this.downloadUrl = downloadUrl;
        this.signature = signature;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.downloads = downloads;
        this.rating = rating;
        this.ratingCount = ratingCount;
    }
    
    // Builder pattern for easier construction
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public String getVersion() { return version; }
    public String getCategory() { return category; }
    public List<String> getTags() { return tags; }
    public String getDownloadUrl() { return downloadUrl; }
    public String getSignature() { return signature; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getDownloads() { return downloads; }
    public double getRating() { return rating; }
    public int getRatingCount() { return ratingCount; }
    
    /**
     * Check if this script has a valid signature.
     * 
     * @return true if signature is present and valid
     */
    public boolean isSignatureValid() {
        // TODO: Implement signature verification
        return signature != null && !signature.isEmpty();
    }
    
    /**
     * Get a formatted rating string (e.g., "4.5 (123 reviews)")
     * 
     * @return Formatted rating string
     */
    public String getFormattedRating() {
        if (ratingCount == 0) {
            return "No ratings yet";
        }
        return String.format("%.1f (%d reviews)", rating, ratingCount);
    }
    
    /**
     * Get a formatted download count (e.g., "1.2K downloads")
     * 
     * @return Formatted download count
     */
    public String getFormattedDownloads() {
        if (downloads >= 1_000_000) {
            return String.format("%.1fM downloads", downloads / 1_000_000.0);
        } else if (downloads >= 1_000) {
            return String.format("%.1fK downloads", downloads / 1_000.0);
        }
        return downloads + " downloads";
    }
    
    @Override
    public String toString() {
        return "VerifiedScript{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", author='" + author + '\'' +
                '}';
    }
    
    /**
     * Builder class for VerifiedScript
     */
    public static class Builder {
        private String id;
        private String name;
        private String description = "";
        private String author = "Unknown";
        private String version = "1.0.0";
        private String category = "misc";
        private List<String> tags = List.of();
        private String downloadUrl;
        private String signature;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();
        private int downloads = 0;
        private double rating = 0.0;
        private int ratingCount = 0;
        
        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder author(String author) { this.author = author; return this; }
        public Builder version(String version) { this.version = version; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder tags(List<String> tags) { this.tags = tags; return this; }
        public Builder downloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; return this; }
        public Builder signature(String signature) { this.signature = signature; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder downloads(int downloads) { this.downloads = downloads; return this; }
        public Builder rating(double rating) { this.rating = rating; return this; }
        public Builder ratingCount(int ratingCount) { this.ratingCount = ratingCount; return this; }
        
        public VerifiedScript build() {
            if (id == null || name == null) {
                throw new IllegalStateException("id and name are required");
            }
            return new VerifiedScript(
                id, name, description, author, version, category, tags,
                downloadUrl, signature, createdAt, updatedAt, downloads, rating, ratingCount
            );
        }
    }
}
