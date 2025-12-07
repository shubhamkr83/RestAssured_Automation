package com.automation.constants;

/**
 * Centralized endpoint management for Buyer App (Navo) API.
 * Define all Buyer App API endpoints as constants here.
 */
public final class BuyerAppEndpoints {

    private BuyerAppEndpoints() {
        // Prevent instantiation
    }

    // Authentication endpoints
    public static final String LOGIN = "/api/auth/login";
    public static final String AUTH_VALIDATE = "/v1/auth/validate";

    // Feed/HomePage endpoints
    public static final String FEED_FILTERS = "/v1/feed/filters";
    public static final String FEED_FILTERS_SAVE = "/v1/feed/filters/save";
    public static final String FEED_BANNERS = "/v1/feed/banners";
    public static final String FEED_JOURNEY_COLLECTION = "/v1/feed/journey/collection";
    public static final String FEED_HOME_CATALOG = "/v1/feed/home/catalog";
    public static final String FEED_HOME_TRENDING = "/v1/feed/home/trending";
    public static final String FEED_NEW_THIS_WEEK = "/v1/feed/new-this-week";

    // Search endpoints
    public static final String USER_SEARCH = "/v1/user/search";

    // Collection endpoints
    public static final String COLLECTION_ALL = "/v1/collection/all";
    public static final String COLLECTION_TOP = "/v1/feed/collection/top";
    public static final String COLLECTION_BY_ID = "/v1/collection/{id}";

    // Profile & Config endpoints
    public static final String APP_UPDATE = "/api/appConfig/app-update";
    public static final String SUITABLE_FOR_CONFIG = "/api/appConfig/suitable-for";

    // Cart endpoints
    public static final String CART = "/v1/cart";
}
