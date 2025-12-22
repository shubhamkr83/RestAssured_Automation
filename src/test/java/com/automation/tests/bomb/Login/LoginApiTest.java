package com.automation.tests.bomb.Login;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.LoginRequest;
import com.automation.models.response.LoginResponse;
import com.automation.utils.JsonUtils;
import com.automation.utils.VariableManager;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Login API.
 * Handles authentication and token management.
 * Tests are based on Postman scripts for comprehensive validation.
 */
@Epic("BOMB Authentication")
@Feature("Login API")
public class LoginApiTest extends BaseTest {

        // Token is now stored in VariableManager.setToken() - no static variable needed
        private static Response loginResponse;
        private static LoginResponse loginResponseData;

        @Test(description = "Verify response status is 200 OK", priority = 1, groups = "bomb")
        @Story("User Login")
        @Severity(SeverityLevel.BLOCKER)
        public void testResponseStatus() {
                // Prepare request
                LoginRequest loginRequest = LoginRequest.builder()
                                .phoneNumber(config.loginPhoneNumber())
                                .token(config.loginToken())
                                .build();

                // Send POST request
                loginResponse = RestAssured.given()
                                .spec(requestSpec)
                                .body(loginRequest)
                                .when()
                                .post(BombEndpoints.LOGIN);

                // Parse response for other tests
                loginResponseData = JsonUtils.fromResponse(loginResponse, LoginResponse.class);

                // Verify response status is 200 OK
                assertThat("Response status should be 200 OK",
                                loginResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Verify response time is within acceptable threshold", priority = 2, dependsOnMethods = "testResponseStatus", groups = "bomb")
        @Story("User Login")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Verify response time is within acceptable threshold
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = loginResponse.getTime();

                assertThat("Response time should be within acceptable threshold",
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Validate response data structure and types", priority = 3, dependsOnMethods = "testResponseStatus", groups = "bomb")
        @Story("User Login")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseDataStructureAndTypes() {
                // Validate response data is an object
                assertThat("Response should be an object", loginResponseData, notNullValue());
                assertThat("Response data should not be null", loginResponseData.getData(), notNullValue());

                // Validate data types
                assertThat("phoneNumber should be a string",
                                loginResponseData.getData().getPhoneNumber(), instanceOf(String.class));
                assertThat("name should be a string",
                                loginResponseData.getData().getName(), instanceOf(String.class));
                assertThat("businessName should be a string",
                                loginResponseData.getData().getBusinessName(), instanceOf(String.class));
                assertThat("isDeleted should have value false",
                                loginResponseData.getData().getIsDeleted(), is(false));
                assertThat("accessToken should be a string",
                                loginResponseData.getData().getAccessToken(), instanceOf(String.class));
                assertThat("refreshToken should be a string",
                                loginResponseData.getData().getRefreshToken(), instanceOf(String.class));

                logger.info("Response data structure and types validated successfully");
        }

        @Test(description = "Confirm phone number field exists and is populated", priority = 4, dependsOnMethods = "testResponseStatus", groups = "bomb")
        @Story("User Login")
        @Severity(SeverityLevel.NORMAL)
        public void testPhoneNumberExists() {
                // Confirm phone number field exists and is populated
                assertThat("Response should be an object", loginResponseData, notNullValue());
                assertThat("Phone number should not be null",
                                loginResponseData.getData().getPhoneNumber(), notNullValue());
                assertThat("Phone number should not be empty",
                                loginResponseData.getData().getPhoneNumber(), not(emptyOrNullString()));

                logger.info("Phone number field validated: {}", loginResponseData.getData().getPhoneNumber());
        }

        @Test(description = "Confirm access token exists and is valid", priority = 5, dependsOnMethods = "testResponseStatus", groups = "bomb")
        @Story("User Login")
        @Severity(SeverityLevel.BLOCKER)
        public void testAccessTokenExists() {
                // Confirm access token exists and is valid
                assertThat("Response should be an object", loginResponseData, notNullValue());
                assertThat("Access token should not be null",
                                loginResponseData.getData().getAccessToken(), notNullValue());
                assertThat("Access token should not be empty",
                                loginResponseData.getData().getAccessToken(), not(emptyOrNullString()));

                logger.info("Access token validated: {}...",
                                loginResponseData.getData().getAccessToken().substring(0,
                                                Math.min(20, loginResponseData.getData().getAccessToken().length())));
        }

        @Test(description = "Confirm refresh token exists and is valid", priority = 6, dependsOnMethods = "testResponseStatus", groups = "bomb")
        @Story("User Login")
        @Severity(SeverityLevel.BLOCKER)
        public void testRefreshTokenExists() {
                // Confirm refresh token exists and is valid
                assertThat("Response should be an object", loginResponseData, notNullValue());
                assertThat("Refresh token should not be null",
                                loginResponseData.getData().getRefreshToken(), notNullValue());
                assertThat("Refresh token should not be empty",
                                loginResponseData.getData().getRefreshToken(), not(emptyOrNullString()));

                logger.info("Refresh token validated: {}...",
                                loginResponseData.getData().getRefreshToken().substring(0,
                                                Math.min(20, loginResponseData.getData().getRefreshToken().length())));
        }

        @Test(description = "Store access token in collection variables and persist to file", priority = 7, dependsOnMethods = "testAccessTokenExists", groups = "bomb")
        @Story("User Login")
        @Severity(SeverityLevel.BLOCKER)
        public void testStoreAccessToken() {
                // Store access token using VariableManager (thread-safe)
                String accessToken = loginResponseData.getData().getAccessToken();
                VariableManager.setToken(accessToken);

                // Verify token was stored in memory
                String storedToken = VariableManager.getToken();
                assertThat("Stored token should not be null", storedToken, notNullValue());
                assertThat("Stored token should not be empty", storedToken, not(emptyOrNullString()));

                logger.info("Access token stored successfully in VariableManager: {}...",
                                storedToken.substring(0, Math.min(20, storedToken.length())));

                // Persist token to file for cross-execution access
                VariableManager.saveTokenToFile();
                logger.info("Access token persisted to test-variables.properties file");
        }
}
