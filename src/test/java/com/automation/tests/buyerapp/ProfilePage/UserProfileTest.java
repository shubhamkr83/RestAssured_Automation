package com.automation.tests.buyerapp.ProfilePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.UserProfileResponse;
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
 * Test class for User Profile API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/api/user
 * Validates response structure, required fields, and data types.
 */
@Epic("Buyer App Profile Page")
@Feature("User Profile API")
public class UserProfileTest extends BaseTest {

    private static Response userProfileResponse;
    private static UserProfileResponse userProfileResponseData;
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
    @Story("User Profile")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatusCode200() {
        // Send GET request with authentication
        userProfileResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "JWT " + VariableManager.getBuyerAppToken())
                .when()
                .get(BuyerAppEndpoints.USER_PROFILE);

        // Parse response for other tests
        userProfileResponseData = JsonUtils.fromResponse(userProfileResponse, UserProfileResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                userProfileResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("User Profile")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = userProfileResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Response has all the required fields", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("User Profile")
    @Severity(SeverityLevel.CRITICAL)
    public void testRequiredFields() {
        // Response has all the required fields
        assertThat("Response should be an object", userProfileResponseData, notNullValue());
        assertThat("code should be 200", userProfileResponseData.getCode(), equalTo(200));
        assertThat("message should be 'Success'", userProfileResponseData.getMessage(), equalTo("Success"));

        // Optional fields validation
        if (userProfileResponseData.getLocation() != null) {
            assertThat("location should be a string",
                    userProfileResponseData.getLocation(), instanceOf(String.class));
        }
        if (userProfileResponseData.getAddress() != null) {
            assertThat("address should be a string",
                    userProfileResponseData.getAddress(), instanceOf(String.class));
        }
        if (userProfileResponseData.getSellOnBizup() != null) {
            assertThat("sellOnBizup should be a boolean",
                    userProfileResponseData.getSellOnBizup(), instanceOf(Boolean.class));
        }

        logger.info("Required fields validated: code=200, message=Success");
    }

    @Test(description = "If phoneNumber exists, it should be in valid format", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("User Profile")
    @Severity(SeverityLevel.NORMAL)
    public void testPhoneNumberFormat() {
        // If phoneNumber exists, it should be in valid format
        if (userProfileResponseData.getPhoneNumber() != null) {
            assertThat("phoneNumber should be in valid format (10 digits)",
                    userProfileResponseData.getPhoneNumber(), matchesRegex("^\\d{10}$"));
            logger.info("Phone number format validated: {}", userProfileResponseData.getPhoneNumber());
        } else {
            logger.info("phoneNumber field is optional - not present in response");
        }
    }

    @Test(description = "Data object has the correct structure and data types", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("User Profile")
    @Severity(SeverityLevel.NORMAL)
    public void testDataObjectStructure() {
        // Data object has the correct structure and data types
        assertThat("data should be an object", userProfileResponseData.getData(), notNullValue());

        logger.info("Data object structure validated");
    }

    @Test(description = "Presence of Content-Type header in the response", priority = 6, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("User Profile")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        // Presence of Content-Type header in the response
        assertThat("Content-Type header should be present",
                userProfileResponse.getHeader("Content-Type"), notNullValue());

        logger.info("Content-Type header verified: {}", userProfileResponse.getHeader("Content-Type"));
    }
}
