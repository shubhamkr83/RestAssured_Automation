package com.automation.tests.buyerapp;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.UpdateCartRequest;
import com.automation.models.response.UpdateCartResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Update Cart API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/user/update_cart
 * Validates response structure and cart data.
 */
@Epic("Buyer App Cart")
@Feature("Update Cart API")
public class UpdateCartTest extends BaseTest {

    private static Response updateCartResponse;
    private static UpdateCartResponse updateCartResponseData;
    private String buyerAppBaseUrl;

    // Test data
    private static final Integer QUANTITY = 1;

    @BeforeClass
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

        String token = VariableManager.getBuyerAppToken();
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("Buyer App token not available. Please run LoginTest first.");
        }
        logger.info("Using Buyer App token from VariableManager");
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatusCode200() {
        // Prepare request body
        UpdateCartRequest requestBody = UpdateCartRequest.builder()
                .quantity(QUANTITY)
                .cart_id(VariableManager.get("cart_id", "68383213df5a92a14ddba268"))
                .cat_id(VariableManager.get("live_catalog_id", "67c59d8ff22202c05e7d612e"))
                .build();

        // Send POST request with authentication
        updateCartResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "JWT " + VariableManager.getBuyerAppToken())
                .body(requestBody)
                .when()
                .post(BuyerAppEndpoints.USER_UPDATE_CART);

        // Parse response for other tests
        updateCartResponseData = JsonUtils.fromResponse(updateCartResponse, UpdateCartResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                updateCartResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response has valid Content-Type header", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        // Response has valid Content-Type header
        assertThat("Content-Type should include application/json",
                updateCartResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Content-Type header verified: {}", updateCartResponse.getHeader("Content-Type"));
    }

    @Test(description = "Response time is less than threshold", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = updateCartResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Response has required top-level fields", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.CRITICAL)
    public void testTopLevelFields() {
        // Response has required top-level fields
        assertThat("Response should be an object", updateCartResponseData, notNullValue());
        assertThat("statusCode should be present", updateCartResponseData.getStatusCode(), notNullValue());
        assertThat("message should be present", updateCartResponseData.getMessage(), notNullValue());
        assertThat("data should be present", updateCartResponseData.getData(), notNullValue());

        logger.info("Top-level fields validated: statusCode, message, data");
    }

    @Test(description = "Cart data contains expected fields", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.CRITICAL)
    public void testCartDataFields() {
        if (updateCartResponseData.getData() != null) {
            UpdateCartResponse.CartData cartData = updateCartResponseData.getData();

            assertThat("cartData should be an object", cartData, notNullValue());
            assertThat("_id should be present", cartData.get_id(), notNullValue());
            assertThat("catalogs should be present", cartData.getCatalogs(), notNullValue());
            assertThat("status should be present", cartData.getStatus(), notNullValue());
            assertThat("cartItems should be present", cartData.getCartItems(), notNullValue());
            assertThat("buyerId should be present", cartData.getBuyerId(), notNullValue());

            logger.info("Cart data fields validated");
        }
    }

    @Test(description = "CartItems array is present and not empty", priority = 6, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.CRITICAL)
    public void testCartItemsArrayNotEmpty() {
        if (updateCartResponseData.getData() != null) {
            assertThat("cartItems should be an array and not empty",
                    updateCartResponseData.getData().getCartItems(),
                    allOf(instanceOf(java.util.List.class), not(empty())));

            logger.info("CartItems array validated: {} items",
                    updateCartResponseData.getData().getCartItems().size());
        }
    }

    @Test(description = "Validate properties of each cart item", priority = 7, dependsOnMethods = "testCartItemsArrayNotEmpty", groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.CRITICAL)
    public void testCartItemProperties() {
        if (updateCartResponseData.getData() != null && updateCartResponseData.getData().getCartItems() != null) {
            updateCartResponseData.getData().getCartItems().forEach(item -> {
                assertThat("title should be present", item.getTitle(), notNullValue());
                assertThat("title should be a non-empty string",
                        item.getTitle(), allOf(instanceOf(String.class), not(emptyOrNullString())));
                assertThat("priceText should be a number", item.getPriceText(), instanceOf(Double.class));
                assertThat("quantity should be a number", item.getQuantity(), instanceOf(Integer.class));
                assertThat("sellerId should be a non-empty string",
                        item.getSellerId(), allOf(instanceOf(String.class), not(emptyOrNullString())));

                if (item.getSetQuantity() != null) {
                    assertThat("setQuantity should be a number", item.getSetQuantity(), instanceOf(Integer.class));
                }
                if (item.getTotalPrice() != null) {
                    assertThat("totalPrice should be a number", item.getTotalPrice(), instanceOf(Double.class));
                }
                if (item.getIsDeleted() != null) {
                    assertThat("isDeleted should be a boolean", item.getIsDeleted(), instanceOf(Boolean.class));
                }
                if (item.getAvailable() != null) {
                    assertThat("available should be a boolean", item.getAvailable(), instanceOf(Boolean.class));
                }
            });

            logger.info("Cart item properties validated for all items");
        }
    }

    @Test(description = "Validate numeric fields in cart data", priority = 8, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.NORMAL)
    public void testCartDataNumericFields() {
        if (updateCartResponseData.getData() != null) {
            UpdateCartResponse.CartData cartData = updateCartResponseData.getData();

            if (cartData.getCartTotal() != null) {
                assertThat("cartTotal should be a number", cartData.getCartTotal(), instanceOf(Double.class));
            }
            if (cartData.getShippingCharges() != null) {
                assertThat("shippingCharges should be a number", cartData.getShippingCharges(),
                        instanceOf(Double.class));
            }
            if (cartData.getTax() != null) {
                assertThat("tax should be a number", cartData.getTax(), instanceOf(Double.class));
            }
            if (cartData.getTotalPayable() != null) {
                assertThat("totalPayable should be a number", cartData.getTotalPayable(), instanceOf(Double.class));
            }

            logger.info("Cart data numeric fields validated");
        }
    }

    @Test(description = "Validate date fields in cart data", priority = 9, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.NORMAL)
    public void testCartDataDateFields() {
        if (updateCartResponseData.getData() != null) {
            UpdateCartResponse.CartData cartData = updateCartResponseData.getData();

            if (cartData.getCreatedAt() != null) {
                assertThat("createdAt should be a string", cartData.getCreatedAt(), instanceOf(String.class));
                assertThat("createdAt should be a valid date", isValidDate(cartData.getCreatedAt()), is(true));
            }
            if (cartData.getUpdatedAt() != null) {
                assertThat("updatedAt should be a string", cartData.getUpdatedAt(), instanceOf(String.class));
                assertThat("updatedAt should be a valid date", isValidDate(cartData.getUpdatedAt()), is(true));
            }

            logger.info("Cart data date fields validated");
        }
    }

    // Helper method to validate date string
    private boolean isValidDate(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setLenient(false);
            sdf.parse(dateString);
            return true;
        } catch (ParseException e) {
            // Try ISO 8601 format
            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                isoFormat.setLenient(false);
                isoFormat.parse(dateString);
                return true;
            } catch (ParseException ex) {
                return false;
            }
        }
    }
}
