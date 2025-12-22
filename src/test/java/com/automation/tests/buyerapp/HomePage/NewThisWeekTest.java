package com.automation.tests.buyerapp.HomePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.NewThisWeekResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static com.automation.tests.buyerapp.HomePage.FeedFilterSaveTest.suitableFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for New This Week API - Converted from Postman Script.
 * Endpoint:
 * {{navo_base}}/v1/feed/new-this-week?size=1&page=0&suitable_for={{suitable_for}}
 * Validates response structure, product data, and data types.
 */
@Epic("Buyer App Home Page")
@Feature("New This Week API")
public class NewThisWeekTest extends BaseTest {

        private static Response newThisWeekResponse;
        private static NewThisWeekResponse newThisWeekResponseData;
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

        @Test(description = "Test the response status 200", priority = 1, groups = "buyerapp")
        @Story("New This Week")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatus() {
                // Get suitable_for parameter (use from previous test or default)
                String suitableForParam = (suitableFor != null && !suitableFor.isEmpty())
                                ? suitableFor
                                : "saree"; // Default value

                // Send GET request with authentication and query parameters
                newThisWeekResponse = RestAssured.given()
                                .baseUri(buyerAppBaseUrl)
                                .contentType("application/json")
                                .header("Authorization", "JWT " + VariableManager.getBuyerAppToken())
                                .queryParam("size", 1)
                                .queryParam("page", 0)
                                .queryParam("suitable_for", suitableForParam)
                                .when()
                                .get(BuyerAppEndpoints.FEED_NEW_THIS_WEEK);

                // Parse response for other tests
                newThisWeekResponseData = JsonUtils.fromResponse(newThisWeekResponse, NewThisWeekResponse.class);

                // Test the response status 200
                assertThat("Test the response status 200",
                                newThisWeekResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Test that response does not have Unauthorized (401) status", priority = 2, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("New This Week")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseNotUnauthorized() {
                // Test that response does not have Unauthorized (401) status
                assertThat("Test that response does not have Unauthorized (401) status",
                                newThisWeekResponse.getStatusCode(), not(equalTo(401)));

                logger.info("Response is not Unauthorized (401) - validated");
        }

        @Test(description = "Test status code 200 when response contains empty data", priority = 3, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("New This Week")
        @Severity(SeverityLevel.NORMAL)
        public void testStatusCode200WithEmptyData() {
                // Test status code 200 when response contains empty data
                assertThat("Test status code 200 when response contains empty data",
                                newThisWeekResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Status code 200 validated even with empty data possibility");
        }

        @Test(description = "Test if response is in JSON format", priority = 4, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("New This Week")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseIsJson() {
                // Test if response is in JSON format
                assertThat("Response should have a body",
                                newThisWeekResponse.getBody().asString(), not(emptyOrNullString()));
                assertThat("Content-Type should include application/json",
                                newThisWeekResponse.getHeader("Content-Type"), containsString("application/json"));

                logger.info("Response format validated as JSON");
        }

        @Test(description = "Test that response time is less than threshold", priority = 5, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("New This Week")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = newThisWeekResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Test that response time is less than threshold
                assertThat(String.format("Test that response time is less than %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Test if response contains valid product data", priority = 6, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("New This Week")
        @Severity(SeverityLevel.CRITICAL)
        public void testValidProductData() {
                // Test if response contains valid product data
                assertThat("'data' should be an object", newThisWeekResponseData.getData(), notNullValue());
                assertThat("'data.result' should be an array",
                                newThisWeekResponseData.getData().getResult(), instanceOf(java.util.List.class));
                assertThat("There should be at least one product",
                                newThisWeekResponseData.getData().getResult().size(), greaterThan(0));

                logger.info("Valid product data verified: {} products found",
                                newThisWeekResponseData.getData().getResult().size());
        }

        @Test(description = "Test structure and data types of response schema", priority = 7, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("New This Week")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseSchemaStructure() {
                NewThisWeekResponse.NewThisWeekData responseData = newThisWeekResponseData.getData();

                // Test structure and data types of response schema
                assertThat("Response data should be an object", responseData, notNullValue());
                assertThat("result should be an array", responseData.getResult(), instanceOf(java.util.List.class));
                assertThat("result should have at least 1 item",
                                responseData.getResult().size(), greaterThanOrEqualTo(1));

                // Validate each item in result array
                responseData.getResult().forEach(item -> {
                        assertThat("Item should be an object", item, notNullValue());

                        assertThat("_id should be a string", item.get_id(), instanceOf(String.class));
                        assertThat("name should be a string", item.getName(), instanceOf(String.class));
                        assertThat("description should be a string", item.getDescription(), instanceOf(String.class));
                        assertThat("image should be a string", item.getImage(), instanceOf(String.class));
                        assertThat("addedThisWeek should be a number", item.getAddedThisWeek(),
                                        instanceOf(Integer.class));
                        assertThat("brandingImage should be a string", item.getBrandingImage(),
                                        instanceOf(String.class));
                        assertThat("brandingCatId should be a string", item.getBrandingCatId(),
                                        instanceOf(String.class));
                        assertThat("type should be a string", item.getType(), instanceOf(String.class));
                });

                logger.info("Response schema structure and data types validated successfully");
        }
}
