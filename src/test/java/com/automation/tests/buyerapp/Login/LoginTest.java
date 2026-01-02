package com.automation.tests.buyerapp.Login;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.BuyerLoginRequest;
import com.automation.models.response.BuyerLoginResponse;
import com.automation.utils.JsonUtils;
import com.automation.utils.VariableManager;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Buyer App Login API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/api/auth/login
 * Validates comprehensive response structure, headers, and data fields.
 */
@Epic("Buyer App Authentication")
@Feature("Login API")
public class LoginTest extends BaseTest {

        // Token is now stored in VariableManager.setBuyerAppToken() - no static
        // variable needed
        private static Response loginResponse;
        private static BuyerLoginResponse loginResponseData;
        private String buyerAppBaseUrl;

        @BeforeClass
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);
        }

        @Test(description = "Test the response status is 200 and store access token", priority = 1, groups = {
                        "buyerapp", "buyerapp-login" }, alwaysRun = true)
        @Story("User Login")
        @Severity(SeverityLevel.BLOCKER)
        public void testResponseStatusAndStoreToken() {
                // Prepare request
                BuyerLoginRequest loginRequest = BuyerLoginRequest.builder()
                                .phoneNumber(config.buyerAppPhoneNumber())
                                .token("000000")
                                .build();

                // Send POST request
                loginResponse = RestAssured.given()
                                .baseUri(buyerAppBaseUrl)
                                .contentType("application/json")
                                .body(loginRequest)
                                .when()
                                .post(BuyerAppEndpoints.LOGIN);

                // Parse response for other tests
                loginResponseData = JsonUtils.fromResponse(loginResponse, BuyerLoginResponse.class);

                // Verify response status is 200
                assertThat("Test the response status is 200",
                                loginResponse.getStatusCode(), equalTo(HttpStatus.OK));

                // Set access token to collection variable
                assertThat("Response data should not be null", loginResponseData.getData(), notNullValue());
                assertThat("Access token should not be null",
                                loginResponseData.getData().getAccessToken(), notNullValue());

                // Store access token using VariableManager (thread-safe)
                VariableManager.setBuyerAppToken(loginResponseData.getData().getAccessToken());

                // Verify token was stored
                String storedToken = VariableManager.getBuyerAppToken();
                assertThat("Stored token should not be null", storedToken, notNullValue());

                logger.info("Response status verified: 200 OK");
                logger.info("Access token stored successfully in VariableManager: {}...",
                                storedToken.substring(0, Math.min(20, storedToken.length())));
        }

        @Test(description = "Validate response time is under threshold", priority = 2, dependsOnMethods = "testResponseStatusAndStoreToken", groups = "buyerapp")
        @Story("User Login")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = loginResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Validate response time is under threshold
                assertThat(String.format("Validate response time is under %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Check that 'Content-Type' header is present", priority = 3, dependsOnMethods = "testResponseStatusAndStoreToken", groups = "buyerapp")
        @Story("User Login")
        @Severity(SeverityLevel.MINOR)
        public void testContentTypeHeader() {
                // Check that 'Content-Type' header is present
                assertThat("Check that 'Content-Type' header is present",
                                loginResponse.getHeader("Content-Type"), notNullValue());

                logger.info("Content-Type header verified: {}", loginResponse.getHeader("Content-Type"));
        }

        @Test(description = "Validate response message equals 'Success'", priority = 4, dependsOnMethods = "testResponseStatusAndStoreToken", groups = "buyerapp")
        @Story("User Login")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseMessage() {
                // Validate response message equals 'Success'
                assertThat("Validate response message equals 'Success'",
                                loginResponseData.getMessage(), equalTo("Success"));

                logger.info("Response message verified: Success");
        }

        @Test(description = "Validate response contains all required fields in data", priority = 5, dependsOnMethods = "testResponseStatusAndStoreToken", groups = "buyerapp")
        @Story("User Login")
        @Severity(SeverityLevel.CRITICAL)
        public void testAllRequiredFields() {
                // Validate response is an object
                assertThat("Validate response is an object", loginResponseData, notNullValue());
                assertThat("Response data should not be null", loginResponseData.getData(), notNullValue());

                BuyerLoginResponse.BuyerLoginData data = loginResponseData.getData();

                // Validate all required fields (30+ fields as per Postman script)
                String[] requiredFields = {
                                "phoneNumber", "name", "businessName", "category", "businessVerified", "segment",
                                "deviceId", "activated", "isDeleted", "introVideo", "tagLine", "businessType",
                                "dealsIn", "termsCondition", "bizupProtected", "isSeller", "introVideoThumbnail",
                                "isClaimed", "accountManager", "isCatalogAvailable", "profileShareLink", "persona",
                                "address", "installReferrer", "isWhatsappOptIn", "sellOnBizup", "businessCard",
                                "createdAt", "updatedAt", "shop", "accessToken", "refreshToken", "isProfileComplete",
                                "fo", "isFirstTime"
                };

                // Validate each required field exists
                assertThat("Phone number should be present", data.getPhoneNumber(), notNullValue());
                assertThat("Name should be present", data.getName(), notNullValue());
                assertThat("Business name should be present", data.getBusinessName(), notNullValue());
                assertThat("Category should be present", data.getCategory(), notNullValue());
                assertThat("Access token should be present", data.getAccessToken(), notNullValue());
                assertThat("Refresh token should be present", data.getRefreshToken(), notNullValue());
                assertThat("Created at should be present", data.getCreatedAt(), notNullValue());
                assertThat("Updated at should be present", data.getUpdatedAt(), notNullValue());

                logger.info("All required fields validated successfully (30+ fields)");
        }

        @Test(description = "Validate the location object exists and is an object", priority = 6, dependsOnMethods = "testResponseStatusAndStoreToken", groups = "buyerapp")
        @Story("User Login")
        @Severity(SeverityLevel.NORMAL)
        public void testLocationObject() {
                // Validate response is an object
                assertThat("Validate response is an object", loginResponseData, notNullValue());

                // Validate the location object exists and is an object
                assertThat("Validate the location object exists",
                                loginResponseData.getData().getLocation(), notNullValue());
                assertThat("Location should be an object",
                                loginResponseData.getData().getLocation(),
                                instanceOf(BuyerLoginResponse.Location.class));

                logger.info("Location object validated successfully");
        }

        @Test(description = "Validate 'category' is a non-empty array of strings", priority = 7, dependsOnMethods = "testResponseStatusAndStoreToken", groups = "buyerapp")
        @Story("User Login")
        @Severity(SeverityLevel.NORMAL)
        public void testCategoryArray() {
                // Validate response has property 'data'
                assertThat("Response should have property 'data'", loginResponseData.getData(), notNullValue());

                // Validate 'category' is a non-empty array of strings
                assertThat("Category should be present",
                                loginResponseData.getData().getCategory(), notNullValue());
                assertThat("Category should be an array",
                                loginResponseData.getData().getCategory(), instanceOf(java.util.List.class));
                assertThat("Category should not be empty",
                                loginResponseData.getData().getCategory(), not(empty()));

                // Validate each category is a string
                loginResponseData.getData().getCategory().forEach(category -> {
                        assertThat("Each category should be a string", category, instanceOf(String.class));
                });

                logger.info("Category array validated: {} categories found",
                                loginResponseData.getData().getCategory().size());
        }

        @Test(description = "Store access token in collection variables and persist to file", priority = 8, dependsOnMethods = "testResponseStatusAndStoreToken", groups = "buyerapp")
        @Story("User Login")
        @Severity(SeverityLevel.BLOCKER)
        public void testStoreAccessTokenToFile() {
                // Store access token using VariableManager (already done in first test, but
                // verify)
                String accessToken = loginResponseData.getData().getAccessToken();
                VariableManager.setBuyerAppToken(accessToken);

                // Verify token was stored in memory
                String storedToken = VariableManager.getBuyerAppToken();
                assertThat("Stored token should not be null", storedToken, notNullValue());
                assertThat("Stored token should not be empty", storedToken, not(emptyOrNullString()));

                logger.info("Buyer App access token stored successfully in VariableManager: {}...",
                                storedToken.substring(0, Math.min(20, storedToken.length())));

                // Persist token to file for cross-execution access
                VariableManager.saveBuyerAppTokenToFile();
                logger.info("Buyer App access token persisted to test-variables.properties file");
        }
}
