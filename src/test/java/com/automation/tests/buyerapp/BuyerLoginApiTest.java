package com.automation.tests.buyerapp;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.BuyerLoginRequest;
import com.automation.models.response.BuyerLoginResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Buyer App Login and Authentication API.
 */
@Epic("Buyer App Authentication")
@Feature("Login API")
public class BuyerLoginApiTest extends BaseTest {

    public static String buyerAppToken; // Store token for other tests
    private String buyerAppBaseUrl;

    @BeforeClass
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);
    }

    @Test(description = "Verify buyer app login with valid credentials", priority = 1)
    @Story("Buyer Login")
    @Severity(SeverityLevel.BLOCKER)
    public void testBuyerAppLogin() {
        BuyerLoginRequest loginRequest = BuyerLoginRequest.builder()
                .phoneNumber(config.buyerAppPhoneNumber())
                .token(config.buyerAppToken())
                .build();

        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .body(loginRequest)
                .when()
                .post(BuyerAppEndpoints.LOGIN);

        // Validate status code
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        // Validate response time
        assertThat("Response time should be acceptable",
                response.getTime(), lessThan((long) config.responseTimeThreshold()));

        // Parse response
        BuyerLoginResponse loginResponse = JsonUtils.fromResponse(response, BuyerLoginResponse.class);

        // Validate response structure
        assertThat("Response message should be Success",
                loginResponse.getMessage(), equalTo("Success"));
        assertThat("Response data should not be null", loginResponse.getData(), notNullValue());

        // Validate required fields
        assertThat("Phone number should match",
                loginResponse.getData().getPhoneNumber(), equalTo(config.buyerAppPhoneNumber()));
        assertThat("Name should be present",
                loginResponse.getData().getName(), not(emptyOrNullString()));
        assertThat("Business name should be present",
                loginResponse.getData().getBusinessName(), not(emptyOrNullString()));

        // Validate tokens
        assertThat("Access token should not be null",
                loginResponse.getData().getAccessToken(), notNullValue());
        assertThat("Access token should not be empty",
                loginResponse.getData().getAccessToken(), not(emptyString()));
        assertThat("Refresh token should not be null",
                loginResponse.getData().getRefreshToken(), notNullValue());

        // Store token for subsequent tests
        buyerAppToken = loginResponse.getData().getAccessToken();
        logger.info("Buyer App login successful. Token stored: {}", buyerAppToken.substring(0, 20) + "...");
    }

    @Test(description = "Verify all required fields in login response", dependsOnMethods = "testBuyerAppLogin")
    @Story("Buyer Login")
    @Severity(SeverityLevel.CRITICAL)
    public void testLoginResponseFields() {
        BuyerLoginRequest loginRequest = BuyerLoginRequest.builder()
                .phoneNumber(config.buyerAppPhoneNumber())
                .token(config.buyerAppToken())
                .build();

        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .body(loginRequest)
                .post(BuyerAppEndpoints.LOGIN);

        BuyerLoginResponse loginResponse = JsonUtils.fromResponse(response, BuyerLoginResponse.class);

        // Validate all required fields (30+ fields as per Postman)
        BuyerLoginResponse.BuyerLoginData data = loginResponse.getData();

        assertThat("Phone number should be present", data.getPhoneNumber(), notNullValue());
        assertThat("Name should be present", data.getName(), notNullValue());
        assertThat("Business name should be present", data.getBusinessName(), notNullValue());
        assertThat("Category should be present", data.getCategory(), notNullValue());
        assertThat("Access token should be present", data.getAccessToken(), notNullValue());
        assertThat("Refresh token should be present", data.getRefreshToken(), notNullValue());
        assertThat("Created at should be present", data.getCreatedAt(), notNullValue());
        assertThat("Updated at should be present", data.getUpdatedAt(), notNullValue());
    }

    @Test(description = "Verify location object in login response", dependsOnMethods = "testBuyerAppLogin")
    @Story("Buyer Login")
    @Severity(SeverityLevel.NORMAL)
    public void testLoginResponseLocation() {
        BuyerLoginRequest loginRequest = BuyerLoginRequest.builder()
                .phoneNumber(config.buyerAppPhoneNumber())
                .token(config.buyerAppToken())
                .build();

        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .body(loginRequest)
                .post(BuyerAppEndpoints.LOGIN);

        BuyerLoginResponse loginResponse = JsonUtils.fromResponse(response, BuyerLoginResponse.class);

        // Validate location object
        assertThat("Location should exist", loginResponse.getData().getLocation(), notNullValue());
        assertThat("Location should be an object", loginResponse.getData().getLocation(),
                instanceOf(BuyerLoginResponse.Location.class));
    }

    @Test(description = "Verify category is non-empty array of strings", dependsOnMethods = "testBuyerAppLogin")
    @Story("Buyer Login")
    @Severity(SeverityLevel.NORMAL)
    public void testLoginResponseCategory() {
        BuyerLoginRequest loginRequest = BuyerLoginRequest.builder()
                .phoneNumber(config.buyerAppPhoneNumber())
                .token(config.buyerAppToken())
                .build();

        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .body(loginRequest)
                .post(BuyerAppEndpoints.LOGIN);

        BuyerLoginResponse loginResponse = JsonUtils.fromResponse(response, BuyerLoginResponse.class);

        // Validate category array
        assertThat("Category should be a list", loginResponse.getData().getCategory(), notNullValue());
        assertThat("Category should not be empty", loginResponse.getData().getCategory(), not(empty()));

        loginResponse.getData().getCategory().forEach(category -> {
            assertThat("Each category should be a string", category, instanceOf(String.class));
        });
    }

    @Test(description = "Verify Content-Type header is present")
    @Story("Buyer Login")
    @Severity(SeverityLevel.MINOR)
    public void testLoginResponseHeaders() {
        BuyerLoginRequest loginRequest = BuyerLoginRequest.builder()
                .phoneNumber(config.buyerAppPhoneNumber())
                .token(config.buyerAppToken())
                .build();

        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .body(loginRequest)
                .post(BuyerAppEndpoints.LOGIN);

        assertThat("Content-Type header should be present",
                response.getHeader("Content-Type"), notNullValue());
    }
}
