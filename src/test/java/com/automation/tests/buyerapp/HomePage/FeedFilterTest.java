package com.automation.tests.buyerapp.HomePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.FeedFilterResponse;
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
 * Test class for Feed Filters API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/feed/filters
 * Validates response structure, filters data, and edge cases.
 */
@Epic("Buyer App Home Page")
@Feature("Feed Filters API")
public class FeedFilterTest extends BaseTest {

        private static Response feedFilterResponse;
        private static FeedFilterResponse feedFilterResponseData;
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

        @Test(description = "Test the response status is 200", priority = 1, groups = "buyerapp")
        @Story("Feed Filters")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatus() {
                // Send GET request with authentication
                feedFilterResponse = RestAssured.given()
                                .baseUri(buyerAppBaseUrl)
                                .contentType("application/json")
                                .header("Authorization", "JWT " + VariableManager.getBuyerAppToken())
                                .when()
                                .get(BuyerAppEndpoints.FEED_FILTERS);

                // Parse response for other tests
                feedFilterResponseData = JsonUtils.fromResponse(feedFilterResponse, FeedFilterResponse.class);

                // Test the response status is 200
                assertThat("Test the response status is 200",
                                feedFilterResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Validate response time is under threshold", priority = 2, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Feed Filters")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = feedFilterResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Validate response time is under threshold
                assertThat(String.format("Validate response time is under %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Validate structure and data types of the response schema", priority = 3, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Feed Filters")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseSchema() {
                FeedFilterResponse.FilterData responseData = feedFilterResponseData.getData();

                // Validate structure and data types of the response schema
                assertThat("Response data should be an object", responseData, notNullValue());
                assertThat("Response data should be an object", responseData,
                                instanceOf(FeedFilterResponse.FilterData.class));

                assertThat("productTags should be an array",
                                responseData.getProductTags(), instanceOf(java.util.List.class));
                assertThat("suitable_for should be an array",
                                responseData.getSuitable_for(), instanceOf(java.util.List.class));
                assertThat("city should be an array",
                                responseData.getCity(), instanceOf(java.util.List.class));
                assertThat("priceFilters should be an array",
                                responseData.getPriceFilters(), instanceOf(java.util.List.class));

                logger.info("Response schema structure validated successfully");
        }

        @Test(description = "Price minimum values should not be negative", priority = 4, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Feed Filters")
        @Severity(SeverityLevel.NORMAL)
        public void testPriceMinimumNotNegative() {
                FeedFilterResponse.FilterData responseData = feedFilterResponseData.getData();

                // Price minimum values should not be negative
                responseData.getPriceFilters().forEach(filter -> {
                        filter.getRanges().forEach(range -> {
                                assertThat("Price minimum should not be negative",
                                                range.getPrice_min(), greaterThanOrEqualTo(0));
                        });
                });

                logger.info("Price minimum values validated - all non-negative");
        }

        @Test(description = "Price maximum values should not be negative", priority = 5, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Feed Filters")
        @Severity(SeverityLevel.NORMAL)
        public void testPriceMaximumNotNegative() {
                FeedFilterResponse.FilterData responseData = feedFilterResponseData.getData();

                // Price maximum values should not be negative
                responseData.getPriceFilters().forEach(filter -> {
                        filter.getRanges().forEach(range -> {
                                assertThat("Price maximum should not be negative",
                                                range.getPrice_max(), greaterThanOrEqualTo(0));
                        });
                });

                logger.info("Price maximum values validated - all non-negative");
        }

        @Test(description = "Validate all required fields are present in the response", priority = 6, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Feed Filters")
        @Severity(SeverityLevel.CRITICAL)
        public void testRequiredFieldsPresence() {
                FeedFilterResponse.FilterData responseData = feedFilterResponseData.getData();

                // Validate all required fields are present in the response
                assertThat("Response data should be an object", responseData, notNullValue());

                assertThat("productTags should be present",
                                responseData.getProductTags(), notNullValue());
                assertThat("productTags should be an array",
                                responseData.getProductTags(), instanceOf(java.util.List.class));

                assertThat("suitable_for should have at least 1 item",
                                responseData.getSuitable_for(), hasSize(greaterThanOrEqualTo(1)));

                assertThat("city should have at least 1 item",
                                responseData.getCity(), hasSize(greaterThanOrEqualTo(1)));

                assertThat("priceFilters should have at least 1 item",
                                responseData.getPriceFilters(), hasSize(greaterThanOrEqualTo(1)));

                logger.info("All required fields validated successfully");
        }

        @Test(description = "Verify productTags array contains expected filter data", priority = 7, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Feed Filters")
        @Severity(SeverityLevel.CRITICAL)
        public void testProductTagsStructure() {
                FeedFilterResponse.FilterData responseData = feedFilterResponseData.getData();

                // Verify productTags array contains expected filter data
                assertThat("Response data should be an object", responseData, notNullValue());
                assertThat("productTags should exist", responseData.getProductTags(), notNullValue());
                assertThat("productTags should be an array",
                                responseData.getProductTags(), instanceOf(java.util.List.class));

                // Validate each productTag has required properties
                responseData.getProductTags().forEach(tag -> {
                        assertThat("Tag should have property 'name'", tag.getName(), notNullValue());
                        assertThat("name should be a string", tag.getName(), instanceOf(String.class));

                        assertThat("Tag should have property 'image'", tag.getImage(), notNullValue());
                        assertThat("image should be a string", tag.getImage(), instanceOf(String.class));

                        assertThat("Tag should have property 'translation'", tag.getTranslation(), notNullValue());
                        assertThat("translation should be an object", tag.getTranslation(), instanceOf(Object.class));

                        assertThat("Tag should have property 'visible'", tag.getVisible(), notNullValue());
                        assertThat("visible should be a boolean", tag.getVisible(), instanceOf(Boolean.class));

                        assertThat("Tag should have property 'selected'", tag.getSelected(), notNullValue());
                        assertThat("selected should be a boolean", tag.getSelected(), instanceOf(Boolean.class));

                        assertThat("Tag should have property 'displayName'", tag.getDisplayName(), notNullValue());
                        assertThat("displayName should be a string", tag.getDisplayName(), instanceOf(String.class));
                });

                logger.info("productTags structure validated: {} tags found", responseData.getProductTags().size());
        }
}
