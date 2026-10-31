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
    public static final String FEED_HOME_TRENDING = "/v1/feed/trending";
    public static final String FEED_NEW_THIS_WEEK = "/v1/feed/new-this-week";
    public static final String FEED_FEATURED_COLLECTION = "/v1/feed/featured-collection";
    public static final String FEED_HOME_CONFIG = "/v1/feed/home/config";
    public static final String FEED_TV = "/v1/feed/tv";

    // Search endpoints
    public static final String USER_SEARCH = "/v1/user/search";

    // Collection endpoints
    public static final String COLLECTION_ALL = "/v1/collection/all";
    public static final String COLLECTION_TOP = "/v1/collection/top";
    public static final String COLLECTION_BY_ID = "/v1/collection/{id}";

    // Profile & Config endpoints
    public static final String APP_UPDATE = "/api/appConfig/app-update";
    public static final String SUITABLE_FOR_CONFIG = "/api/appConfig/suitable-for";
    public static final String USER_PROFILE = "/api/user";
    public static final String USER_VIEWED_VIDEOS = "/v1/user/viewed/videos";
    public static final String ACTION = "/api/action";

    // Cart endpoints
    public static final String CART = "/v1/cart";
    public static final String USER_UPDATE_CART = "/v1/user/update_cart";

    // New backend-api route groups (QA sync 2026-10-30)
    public static final String BANNER_PLACEMENT = "/v1/banner/{placement}";
    public static final String MLP = "/v1/mlp";
    public static final String MLP_BY_OCCASION = "/v1/mlp/{occasion}";
    public static final String MLP_LIST = "/v1/mlp/list";
    public static final String MLP_OCCASIONS = "/v1/mlp/occasions";
    public static final String ADMIN_MLP = "/v1/admin/mlp";
    public static final String NAVO_LIVE = "/v1/navo-live";
    public static final String NAVO_LIVE_EVENT_PRODUCTS = "/v1/navo-live/events/{eventId}/products";
    // Mounted at the root in backend-api's expressLoader.ts — NOT under /v1
    public static final String EVENTS = "/events";
    public static final String EVENTS_IMPRESSIONS = "/events/impressions";
    public static final String EVENTS_V1_NOT_A_ROUTE = "/v1/events";
    public static final String SUBSELLER_REFRESH_TOKEN = "/v1/subseller/refresh/token";
    public static final String SUBSELLER_ME = "/v1/subseller/me";
    public static final String SUBSELLER_CATALOG = "/v1/subseller/catalog";
    public static final String SUBSELLER_CATALOG_BY_ID = "/v1/subseller/catalog/{id}";
    public static final String SUBSELLER_CATALOG_CONFIRM_STOCK = "/v1/subseller/catalog/{id}/confirm-stock";
    public static final String PRISM_CART_BOT_SESSIONS = "/v1/prism/cart-bot/sessions";
    public static final String PRISM_CHATBOT_FEEDBACK_RUNS = "/v1/prism/chatbot-feedback/runs";
    public static final String PRISM_CHATBOT_FEEDBACK_CONVERSATION = "/v1/prism/chatbot-feedback/conversations/{evaluationId}";
    public static final String PRISM_BOT_TRIAL_MESSAGE = "/v1/prism/bot-trial/message";
    public static final String PRISM_IMAGE_ENHANCEMENT_GENERATE = "/v1/prism/image-enhancement/generate";
    public static final String PRISM_IMAGE_ENHANCEMENT_EDIT = "/v1/prism/image-enhancement/edit";
    public static final String PRISM_IMAGE_ENHANCEMENT_SAVE = "/v1/prism/image-enhancement/save-to-catalog";
    public static final String PRISM_IMAGE_ENHANCEMENT_REMOVE = "/v1/prism/image-enhancement/remove-from-catalog";
    public static final String REFERRAL = "/v1/referral";
    public static final String CHATBOT_V2_MESSAGE = "/v2/chatbot/message";
}
