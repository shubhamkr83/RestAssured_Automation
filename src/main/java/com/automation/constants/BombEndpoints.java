package com.automation.constants;

/**
 * Centralized endpoint management for BOMB API.
 * Define all BOMB API endpoints as constants here.
 */
public final class BombEndpoints {

    private BombEndpoints() {
        // Prevent instantiation
    }

    // Auth endpoints
    public static final String LOGIN = "/api/auth/login";

    // Catalog endpoints
    public static final String CATALOG_ALL = "/v1/admin/catalog_all";
    public static final String CATALOG = "/v1/admin/catalog";

    // Video endpoints
    public static final String VIDEOS_BY_SELLER = "/v1/admin/editor/edit/videos/{sellerId}";
    public static final String VIDEO_TITLE_GENERATION = "/v2/ai/tags-to-text";

    // Admin endpoints
    public static final String ADMIN_CATALOG = "/v1/admin/catalog";
}
