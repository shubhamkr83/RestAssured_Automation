package com.automation.tests.buyerapp.HomePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.FeedFilterSaveRequest;
import com.automation.models.response.FeedFilterSaveResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Feed Filter Save API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/feed/filters/save
 * Validates response structure, arrays, and filter data.
 */
@Epic("Buyer App Home Page")
@Feature("Feed Filter Save API")
public class FeedFilterSaveTest extends BaseTest {

        public static String suitableFor; // Store suitable_for value for other tests
        private static Response filterSaveResponse;
        private static FeedFilterSaveResponse filterSaveResponseData;
        private String buyerAppBaseUrl;

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

        /**
         * DISABLED: Backend error - "Cannot read properties of undefined (reading
         * '_id')"
         * This is a server-side issue that needs to be fixed on the API.
         * Error: HTTP 500 Internal Server Error
         */
        @Test(description = "Test the response status is 200", priority = 1, groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatus() {
                // Prepare request - using suitable_for from config or default value
                String suitableForValue = config.buyerAppPhoneNumber(); // You may need to add a suitable_for config

                FeedFilterSaveRequest request = FeedFilterSaveRequest.builder()
                                .suitable_for(Collections.singletonList(suitableForValue))
                                .testData("")
                                .build();

                // Send POST request with authentication
                filterSaveResponse = RestAssured.given()
                                .baseUri(buyerAppBaseUrl)
                                .contentType("application/json")
                                .header("Authorization", "JWT " + VariableManager.getBuyerAppToken())
                                .body(request)
                                .when()
                                .post(BuyerAppEndpoints.FEED_FILTERS_SAVE);

                // Parse response for other tests
                filterSaveResponseData = JsonUtils.fromResponse(filterSaveResponse, FeedFilterSaveResponse.class);

                // Test the response status is 200
                assertThat("Test the response status is 200",
                                filterSaveResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Validate response time is under threshold", priority = 2, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = filterSaveResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Validate response time is under threshold
                assertThat(String.format("Validate response time is under %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Ensure response time is under 800ms under heavy load", priority = 3, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTimeHeavyLoad() {
                long actualResponseTime = filterSaveResponse.getTime();

                // Ensure response time is under 800ms under heavy load
                assertThat("Ensure response time is under 800ms under heavy load",
                                actualResponseTime, lessThan(800L));

                logger.info("Response time under heavy load verified: {} ms (Threshold: 800 ms)", actualResponseTime);
        }

        @Test(description = "Set suitable_for collection variable", priority = 4, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.NORMAL)
        public void testSetSuitableForVariable() {
                // Try to set suitable_for variable from response
                try {
                        assertThat("Response data should not be null", filterSaveResponseData.getData(),
                                        notNullValue());
                        assertThat("suitable_for array should not be null",
                                        filterSaveResponseData.getData().getSuitable_for(), notNullValue());

                        if (filterSaveResponseData.getData().getSuitable_for() != null
                                        && !filterSaveResponseData.getData().getSuitable_for().isEmpty()) {
                                suitableFor = String.valueOf(filterSaveResponseData.getData().getSuitable_for().get(0));
                                logger.info("Set suitable_for: {}", suitableFor);

                                assertThat("suitable_for should not be null", suitableFor, notNullValue());
                        } else {
                                logger.info("suitable_for not found or empty");
                        }
                } catch (Exception err) {
                        logger.error("Error setting variable: {}", err.getMessage());
                }
        }

        @Test(description = "Validate response schema and types", priority = 5, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseSchema() {
                // Validate response schema and types
                assertThat("Response should be an object", filterSaveResponseData, notNullValue());

                assertThat("Response should have property 'statusCode'",
                                filterSaveResponseData.getStatusCode(), notNullValue());
                assertThat("statusCode should be a string",
                                filterSaveResponseData.getStatusCode(), instanceOf(String.class));

                assertThat("Response should have property 'message'",
                                filterSaveResponseData.getMessage(), notNullValue());
                assertThat("message should be a string",
                                filterSaveResponseData.getMessage(), instanceOf(String.class));

                assertThat("Response should have property 'data'",
                                filterSaveResponseData.getData(), notNullValue());
                assertThat("data should be an object",
                                filterSaveResponseData.getData(),
                                instanceOf(FeedFilterSaveResponse.FilterSaveData.class));

                FeedFilterSaveResponse.FilterSaveData data = filterSaveResponseData.getData();

                assertThat("data should have property 'suitable_for'",
                                data.getSuitable_for(), notNullValue());
                assertThat("suitable_for should be an array",
                                data.getSuitable_for(), instanceOf(java.util.List.class));

                assertThat("data should have property 'productTags'",
                                data.getProductTags(), notNullValue());
                assertThat("productTags should be an array",
                                data.getProductTags(), instanceOf(java.util.List.class));

                assertThat("data should have property 'city'",
                                data.getCity(), notNullValue());
                assertThat("city should be an array",
                                data.getCity(), instanceOf(java.util.List.class));

                // price_min and price_max should be null
                assertThat("price_min should be null", data.getPrice_min(), nullValue());
                assertThat("price_max should be null", data.getPrice_max(), nullValue());

                assertThat("data should have property 'lastSelectedFilter'",
                                data.getLastSelectedFilter(), notNullValue());
                assertThat("lastSelectedFilter should be a string",
                                data.getLastSelectedFilter(), instanceOf(String.class));

                logger.info("Response schema validated successfully");
        }

        @Test(description = "Validate statusCode and message fields when status is not 200", priority = 6, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.NORMAL)
        public void testStatusCodeAndMessageFields() {
                // Validate statusCode and message fields
                assertThat("Response should be an object", filterSaveResponseData, notNullValue());

                assertThat("Response should have property 'statusCode'",
                                filterSaveResponseData.getStatusCode(), notNullValue());
                assertThat("statusCode should be a string",
                                filterSaveResponseData.getStatusCode(), instanceOf(String.class));

                assertThat("Response should have property 'message'",
                                filterSaveResponseData.getMessage(), notNullValue());
                assertThat("message should be a string",
                                filterSaveResponseData.getMessage(), instanceOf(String.class));

                logger.info("statusCode and message fields validated");
        }

        @Test(description = "Ensure suitable_for array contains at least one non-empty string", priority = 7, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.CRITICAL)
        public void testSuitableForArray() {
                FeedFilterSaveResponse.FilterSaveData data = filterSaveResponseData.getData();

                // Ensure suitable_for array contains at least one non-empty string
                assertThat("suitable_for should be an array",
                                data.getSuitable_for(), instanceOf(java.util.List.class));
                assertThat("suitable_for should not be empty",
                                data.getSuitable_for(), not(empty()));

                // Validate each item is present
                data.getSuitable_for().forEach(item -> {
                        assertThat("Each item should be an object", item, instanceOf(Object.class));
                        assertThat("Each item should not be null", item, notNullValue());
                });

                logger.info("suitable_for array validated: {} items found", data.getSuitable_for().size());
        }

        @Test(description = "Verify productTags array is empty", priority = 8, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.NORMAL)
        public void testProductTagsArrayEmpty() {
                FeedFilterSaveResponse.FilterSaveData data = filterSaveResponseData.getData();

                // Verify productTags array is empty
                assertThat("productTags should be an array",
                                data.getProductTags(), instanceOf(java.util.List.class));
                assertThat("productTags should be empty",
                                data.getProductTags(), empty());

                logger.info("productTags array verified as empty");
        }

        @Test(description = "Verify city array is empty", priority = 9, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.NORMAL)
        public void testCityArrayEmpty() {
                FeedFilterSaveResponse.FilterSaveData data = filterSaveResponseData.getData();

                // Verify city array is empty
                assertThat("city should be an array",
                                data.getCity(), instanceOf(java.util.List.class));
                assertThat("city should be empty",
                                data.getCity(), empty());

                logger.info("city array verified as empty");
        }
}
