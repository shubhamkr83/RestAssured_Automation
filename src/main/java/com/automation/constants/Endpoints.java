package com.automation.constants;

/**
 * Centralized endpoint management.
 * Define all API endpoints as constants here.
 */
public final class Endpoints {

    private Endpoints() {
        // Prevent instantiation
    }

    // Base paths
    public static final String API_V1 = "/api/v1";

    // User endpoints (JSONPlaceholder example)
    public static final String USERS = "/users";
    public static final String USER_BY_ID = "/users/{id}";

    // Post endpoints
    public static final String POSTS = "/posts";
    public static final String POST_BY_ID = "/posts/{id}";
    public static final String POSTS_BY_USER = "/posts?userId={userId}";

    // Comment endpoints
    public static final String COMMENTS = "/comments";
    public static final String COMMENT_BY_ID = "/comments/{id}";
    public static final String COMMENTS_BY_POST = "/posts/{postId}/comments";

    // Album endpoints
    public static final String ALBUMS = "/albums";
    public static final String ALBUM_BY_ID = "/albums/{id}";

    // Todo endpoints
    public static final String TODOS = "/todos";
    public static final String TODO_BY_ID = "/todos/{id}";
}
