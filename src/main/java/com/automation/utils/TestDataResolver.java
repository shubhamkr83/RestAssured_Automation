package com.automation.utils;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Looks up Buyer App ids from production at run time instead of relying on
 * hardcoded ids in test-variables.properties, which go stale (a cart gets
 * booked, a catalog loses its seller). Each id is resolved once per run with
 * read-only GETs and cached in VariableManager; if the lookup fails, the value
 * in test-variables.properties is used as before.
 */
public final class TestDataResolver {

    private static final Logger logger = LogManager.getLogger(TestDataResolver.class);

    private TestDataResolver() {
        // Prevent instantiation
    }

    /**
     * A live catalog that has a seller: the first such item on the home catalog
     * feed for the configured suitable_for. Used by CatalogByIdTest and PdpSimilarTest.
     */
    public static String liveCatalogId(Supplier<RequestSpecification> buyerSpec) {
        return resolve("live_catalog_id", () -> {
            Response r = buyerSpec.get()
                    .queryParam("size", 10).queryParam("page", 0)
                    .queryParam("suitable_for", VariableManager.get("suitable_for", "saree"))
                    .get("/v1/feed/home/catalog");
            List<Map<String, Object>> items = r.jsonPath().getList("data.result");
            if (items == null) return null;
            for (Map<String, Object> item : items) {
                if (item.get("seller") instanceof Map && item.get("_id") != null) {
                    return item.get("_id").toString();
                }
            }
            return null;
        });
    }

    /** The test buyer's open cart (GET /v1/user/cart -> data[0]._id). Used by UpdateCartTest. */
    public static String cartId(Supplier<RequestSpecification> buyerSpec) {
        return resolve("cart_id", () -> {
            Response r = buyerSpec.get().get("/v1/user/cart");
            return r.getStatusCode() == 200 ? r.jsonPath().getString("data[0]._id") : null;
        });
    }

    /** A catalog already in that cart (data[0].cartItems[0]._id), so update_cart can change its quantity. */
    public static String cartCatalogId(Supplier<RequestSpecification> buyerSpec) {
        return resolve("cart_catalog_id", () -> {
            Response r = buyerSpec.get().get("/v1/user/cart");
            return r.getStatusCode() == 200 ? r.jsonPath().getString("data[0].cartItems[0]._id") : null;
        });
    }

    private static String resolve(String key, Supplier<String> lookup) {
        String cached = VariableManager.get("resolved_" + key);
        if (cached != null) return cached;
        String value = null;
        try {
            value = lookup.get();
        } catch (Exception e) {
            logger.warn("Runtime lookup of {} failed: {}", key, e.getMessage());
        }
        if (value == null || value.isEmpty()) {
            value = VariableManager.get(key);
            logger.warn("Runtime lookup of {} found nothing; using test-variables.properties value {}", key, value);
        } else {
            logger.info("Resolved {} at run time: {}", key, value);
        }
        if (value != null) VariableManager.set("resolved_" + key, value);
        return value;
    }
}
