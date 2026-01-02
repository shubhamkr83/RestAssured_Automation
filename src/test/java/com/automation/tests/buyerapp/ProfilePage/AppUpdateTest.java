package com.automation.tests.buyerapp.ProfilePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.AppUpdateResponse;
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
 * Test class for App Update API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/api/appConfig/app-update
 * Validates response structure and app update configuration.
 */
@Epic("Buyer App Profile Page")
@Feature("App Update API")
public class AppUpdateTest extends BaseTest {

        private static Response appUpdateResponse;
        private static AppUpdateResponse appUpdateResponseData;
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

        @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
        @Story("App Update")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatusCode200() {
                // Send GET request with authentication
                appUpdateResponse = RestAssured.given()
                                .baseUri(buyerAppBaseUrl)
                                .contentType("application/json")
                                .header("Authorization", "JWT " + VariableManager.getBuyerAppToken())
                                .when()
                                .get(BuyerAppEndpoints.APP_UPDATE);

                // Parse response for other tests
                appUpdateResponseData = JsonUtils.fromResponse(appUpdateResponse, AppUpdateResponse.class);

                // Response status code is 200
                assertThat("Response status code is 200",
                                appUpdateResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("App Update")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = appUpdateResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Response time is less than threshold
                assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Content-Type header is application/json", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("App Update")
        @Severity(SeverityLevel.MINOR)
        public void testContentTypeHeader() {
                // Content-Type header is application/json
                assertThat("Content-Type should include application/json",
                                appUpdateResponse.getHeader("Content-Type"), containsString("application/json"));

                logger.info("Content-Type header verified: {}", appUpdateResponse.getHeader("Content-Type"));
        }

        @Test(description = "Response has the required fields - code, message, and data", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("App Update")
        @Severity(SeverityLevel.CRITICAL)
        public void testRequiredFields() {
                // Response has the required fields
                assertThat("code should be present", appUpdateResponseData.getCode(), notNullValue());
                assertThat("message should be present", appUpdateResponseData.getMessage(), notNullValue());
                assertThat("data should be present", appUpdateResponseData.getData(), notNullValue());

                logger.info("Required fields validated: code, message, data");
        }

        @Test(description = "Validate data properties", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("App Update")
        @Severity(SeverityLevel.CRITICAL)
        public void testDataProperties() {
                AppUpdateResponse.AppUpdateData data = appUpdateResponseData.getData();

                // Validate data properties
                assertThat("data should be an object", data, notNullValue());
                assertThat("isForced should be a boolean", data.getIsForced(), instanceOf(Boolean.class));
                assertThat("minVersion should be a number and at least 0",
                                data.getMinVersion(), allOf(instanceOf(Integer.class), greaterThanOrEqualTo(0)));
                assertThat("minVersionToUpdate should be a number and at least 0",
                                data.getMinVersionToUpdate(),
                                allOf(instanceOf(Integer.class), greaterThanOrEqualTo(0)));

                logger.info("Data properties validated: isForced={}, minVersion={}, minVersionToUpdate={}",
                                data.getIsForced(), data.getMinVersion(), data.getMinVersionToUpdate());
        }
}
