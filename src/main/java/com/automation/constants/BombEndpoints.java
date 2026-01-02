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
    public static final String CATALOG_ASSIGN = "/v1/admin/catalog/assign";
    public static final String EDITOR_ASSIGN_CATALOG = "/v1/admin/editor/assign/catalog";
    public static final String CATALOG_UPLOAD = "/v1/admin/catalog/upload";
    public static final String CATALOG_GROUP_UPLOAD = "/v1/admin/catalog/group/upload";
    public static final String EDITOR_SKIP_CATALOG = "/v1/admin/editor/assign/videos/skip";
    public static final String EDITOR_DONE_CATALOG = "/v1/admin/editor/assign/videos/done";
    public static final String EDITOR_MARK_AS_DONE = "/v1/admin/editor/assign/videos/done";
    public static final String CATALOG_EDITOR_ALL = "/v1/admin/editor/all";

    // Video endpoints
    public static final String VIDEOS_BY_SELLER = "/v1/admin/editor/edit/videos/{sellerId}";
    public static final String VIDEO_TITLE_GENERATION = "/v2/ai/tags-to-text";
    public static final String VIDEO_UPLOAD = "/v1/admin/pipeline/video";
    public static final String VIDEO_ASSIGN_UPLOAD = "/v1/admin/editor/assign/videos";
    public static final String VIDEO_THUMBNAIL_UPLOAD = "/v1/admin/editor/upload/videos";
    public static final String VIDEO_UPLOAD_DONE = "/v1/admin/editor/assign/videos/done";
    public static final String VIDEO_TAGGING = "/v1/admin/editor/edit/videos";

    // Admin endpoints
    public static final String ADMIN_CATALOG = "/v1/admin/catalog";
}
