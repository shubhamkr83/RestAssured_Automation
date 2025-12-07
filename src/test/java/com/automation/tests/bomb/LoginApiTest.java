package com.automation.tests.bomb;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.LoginRequest;
import com.automation.models.response.LoginResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Login API.
 * Handles authentication and token management.
 */
@Epic("BOMB Authentication")
@Feature("Login API")
public class LoginApiTest extends BaseTest {

    public static String bombToken; // Store token for other tests

    @Test(description = "Verify login with valid credentials", priority = 1)
    @Story("User Login")
    @Severity(SeverityLevel.BLOCKER)
    public void testSuccessfulLogin() {
        // Prepare request
        LoginRequest loginRequest = LoginRequest.builder()
                .phoneNumber(config.loginPhoneNumber())
                .token(config.loginToken())
                .build();

        // Send POST request
        Response response = RestAssured.given()
                .spec(requestSpec)
                .body(loginRequest)
                .when()
                .post(BombEndpoints.LOGIN);

        // Validate status code
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        // Validate response time
        assertThat("Response time should be within threshold",
                response.getTime(), lessThan((long) config.responseTimeThreshold()));

        // Parse response
        LoginResponse loginResponse = JsonUtils.fromResponse(response, LoginResponse.class);

        // Validate response structure
        assertThat("Response data should not be null", loginResponse.getData(), notNullValue());
        assertThat("Phone number should be present",
                loginResponse.getData().getPhoneNumber(), is(config.loginPhoneNumber()));
        assertThat("Name should be present",
                loginResponse.getData().getName(), not(emptyOrNullString()));
        assertThat("Business name should be present",
                loginResponse.getData().getBusinessName(), not(emptyOrNullString()));
        assertThat("IsDeleted flag should be false",
                loginResponse.getData().getIsDeleted(), is(false));

        // Validate tokens
        assertThat("Access token should not be null",
                loginResponse.getData().getAccessToken(), notNullValue());
        assertThat("Access token should not be empty",
                loginResponse.getData().getAccessToken(), not(emptyString()));
        assertThat("Refresh token should not be null",
                loginResponse.getData().getRefreshToken(), notNullValue());
        assertThat("Refresh token should not be empty",
                loginResponse.getData().getRefreshToken(), not(emptyString()));

        // Store token for subsequent tests
        bombToken = loginResponse.getData().getAccessToken();
        logger.info("Login successful. Token stored: {}", bombToken.substring(0, 20) + "...");
    }

    @Test(description = "Verify data types in login response", dependsOnMethods = "testSuccessfulLogin")
    @Story("User Login")
    @Severity(SeverityLevel.NORMAL)
    public void testLoginResponseDataTypes() {
        LoginRequest loginRequest = LoginRequest.builder()
                .phoneNumber(config.loginPhoneNumber())
                .token(config.loginToken())
                .build();

        Response response = RestAssured.given()
                .spec(requestSpec)
                .body(loginRequest)
                .post(BombEndpoints.LOGIN);

        LoginResponse loginResponse = JsonUtils.fromResponse(response, LoginResponse.class);

        // Validate data types
        assertThat("phoneNumber should be string",
                loginResponse.getData().getPhoneNumber(), instanceOf(String.class));
        assertThat("name should be string",
                loginResponse.getData().getName(), instanceOf(String.class));
        assertThat("businessName should be string",
                loginResponse.getData().getBusinessName(), instanceOf(String.class));
        assertThat("isDeleted should be boolean",
                loginResponse.getData().getIsDeleted(), instanceOf(Boolean.class));
        assertThat("accessToken should be string",
                loginResponse.getData().getAccessToken(), instanceOf(String.class));
        assertThat("refreshToken should be string",
                loginResponse.getData().getRefreshToken(), instanceOf(String.class));
    }

    @Test(description = "Verify phone number field is populated")
    @Story("User Login")
    @Severity(SeverityLevel.NORMAL)
    public void testPhoneNumberPopulated() {
        LoginRequest loginRequest = LoginRequest.builder()
                .phoneNumber(config.loginPhoneNumber())
                .token(config.loginToken())
                .build();

        Response response = RestAssured.given()
                .spec(requestSpec)
                .body(loginRequest)
                .post(BombEndpoints.LOGIN);

        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        LoginResponse loginResponse = JsonUtils.fromResponse(response, LoginResponse.class);
        assertThat("Phone number should not be null",
                loginResponse.getData().getPhoneNumber(), notNullValue());
        assertThat("Phone number should match request",
                loginResponse.getData().getPhoneNumber(), equalTo(config.loginPhoneNumber()));
    }
}
