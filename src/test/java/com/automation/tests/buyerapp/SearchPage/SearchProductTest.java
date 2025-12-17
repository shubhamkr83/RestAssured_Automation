package com.automation.tests.buyerapp.SearchPage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.SearchProductResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Search Product API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/user/search?q={{search_product}}&page=1&pageSize=20
 * Validates response structure, seller details, and security.
 */
@Epic("Buyer App Search Page")
@Feature("Search Product API")
public class SearchProductTest extends BaseTest {

    private static Response searchProductResponse;
    private static SearchProductResponse searchProductResponseData;
    private String buyerAppBaseUrl;
    private static final String SEARCH_QUERY = "saree"; // Default search query

    @BeforeClass
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);
    }

    @Test(description = "Status code is 200", priority = 1, groups = "buyerapp")
    @Story("Search Product")
    @Severity(SeverityLevel.CRITICAL)
    public void testStatusCode200() {
        // Send GET request with authentication and query parameters
        searchProductResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "Bearer " + VariableManager.getBuyerAppToken())
                .queryParam("q", SEARCH_QUERY)
                .queryParam("page", 1)
                .queryParam("pageSize", 20)
                .when()
                .get(BuyerAppEndpoints.USER_SEARCH);

        // Parse response for other tests
        searchProductResponseData = JsonUtils.fromResponse(searchProductResponse, SearchProductResponse.class);

        // Status code is 200
        assertThat("Status code is 200",
                searchProductResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Search Product")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = searchProductResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Response has valid JSON body", priority = 3, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Search Product")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseHasValidJsonBody() {
        // Response has valid JSON body
        assertThat("Response should have a body",
                searchProductResponse.getBody().asString(), not(emptyOrNullString()));
        assertThat("Response should be parseable as JSON",
                searchProductResponseData, notNullValue());

        logger.info("Response has valid JSON body");
    }

    @Test(description = "Required headers are present and valid", priority = 4, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Search Product")
    @Severity(SeverityLevel.MINOR)
    public void testRequiredHeaders() {
        // Required headers are present and valid
        assertThat("Content-Type header should be present",
                searchProductResponse.getHeader("Content-Type"), notNullValue());
        assertThat("Content-Type should include application/json",
                searchProductResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Required headers validated: Content-Type = {}", 
                searchProductResponse.getHeader("Content-Type"));
    }

    @Test(description = "Validate the response schema for seller details", priority = 5, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Search Product")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseSchemaForSellerDetails() {
        // Validate the response schema for seller details
        assertThat("Response should be an object", searchProductResponseData, notNullValue());
        assertThat("data should be an object", searchProductResponseData.getData(), notNullValue());
        assertThat("data.items should be an array with length above 0",
                searchProductResponseData.getData().getItems(), 
                allOf(instanceOf(java.util.List.class), hasSize(greaterThan(0))));

        searchProductResponseData.getData().getItems().forEach(user -> {
            assertThat("User should have seller object", user.getSeller(), notNullValue());
            
            SearchProductResponse.Seller seller = user.getSeller();
            assertThat("seller.phoneNumber should be a string", seller.getPhoneNumber(), instanceOf(String.class));
            assertThat("seller.address should be a string", seller.getAddress(), instanceOf(String.class));
            assertThat("seller.businessName should be a string", seller.getBusinessName(), instanceOf(String.class));
            assertThat("seller.name should be a string", seller.getName(), instanceOf(String.class));
            assertThat("seller._id should be a string", seller.get_id(), instanceOf(String.class));
            assertThat("seller.deprioritisation_status should be a boolean", 
                    seller.getDeprioritisation_status(), instanceOf(Boolean.class));
            assertThat("seller.isCatalogAvailable should be a boolean", 
                    seller.getIsCatalogAvailable(), instanceOf(Boolean.class));
        });

        logger.info("Response schema for seller details validated: {} items found", 
                searchProductResponseData.getData().getItems().size());
    }

    @Test(description = "No sensitive data exposed and phone numbers are valid", priority = 6, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Search Product")
    @Severity(SeverityLevel.BLOCKER)
    public void testSecurityValidation() {
        String responseBody = searchProductResponse.getBody().asString();
        
        // No sensitive data exposed
        assertThat("Response should not include 'token'",
                responseBody, not(containsString("token")));

        // Phone numbers are valid
        searchProductResponseData.getData().getItems().forEach(user -> {
            if (user.getPhoneNumber() != null) {
                assertThat("Phone number should be in international format",
                        user.getPhoneNumber(), matchesRegex("^\\+[0-9]{10,15}$"));
            }
        });

        logger.info("Security validation passed: no sensitive data exposed, phone numbers valid");
    }
}
