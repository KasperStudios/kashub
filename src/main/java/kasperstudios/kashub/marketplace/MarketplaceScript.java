package kasperstudios.kashub.marketplace;

import java.util.List;

/**
 * DTO for marketplace script metadata.
 * Matches the API response from marketplace.kashub.dev
 */
public class MarketplaceScript {
    public String id;
    public String name;
    public String description;
    public String author;
    public String authorUsername;
    public int downloads;
    public int likes;
    public String category;
    public List<String> tags;
    public String version;
    public String minecraftVersion;
    public String createdAt;
    public String updatedAt;
    public boolean verified;

    // Future use
    public String thumbnailUrl;
    public String readmeUrl;
    public List<String> screenshots;
}
