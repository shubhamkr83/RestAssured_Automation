package com.automation.tests.buyerapp.ProfilePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.AuthValidateResponse;
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
 * Test class for Auth Validate API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/auth/validate
 * Validates response structure, user data, and location object.
 */
@Epic("Buyer App Profile Page")
@Feature("Auth Validate API")
public class AuthValidateTest extends BaseTest {

    private static Response authValidateResponse;
    private static AuthValidateResponse authValidateResponseData;
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
    @Story("Auth Validate")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatusCode200() {
        // Send GET request with authentication
        authValidateResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "JWT " + VariableManager.getBuyerAppToken())
                .when()
                .get(BuyerAppEndpoints.AUTH_VALIDATE);

        // Parse response for other tests
        authValidateResponseData = JsonUtils.fromResponse(authValidateResponse, AuthValidateResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                authValidateResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Content-Type header is application/json", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Auth Validate")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        // Content-Type header is application/json
        assertThat("Content-Type should include application/json",
                authValidateResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Content-Type header verified: {}", authValidateResponse.getHeader("Content-Type"));
    }

    @Test(description = "Response time is less than threshold", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Auth Validate")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = authValidateResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Data object has the correct structure", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Auth Validate")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataObjectStructure() {
        // Data object has the correct structure
        assertThat("Response should be an object", authValidateResponseData, notNullValue());
        assertThat("data should be present", authValidateResponseData.getData(), notNullValue());

        AuthValidateResponse.AuthValidateData data = authValidateResponseData.getData();

        // Required fields
        assertThat("_id should be a string", data.get_id(), instanceOf(String.class));
        assertThat("phoneNumber should be a string", data.getPhoneNumber(), instanceOf(String.class));
        assertThat("name should be a string", data.getName(), instanceOf(String.class));
        assertThat("businessName should be a string", data.getBusinessName(), instanceOf(String.class));
        assertThat("createdAt should be a string", data.getCreatedAt(), instanceOf(String.class));
        assertThat("updatedAt should be a string", data.getUpdatedAt(), instanceOf(String.class));

        // Optional object fields
        if (data.getLocation() != null) {
            assertThat("location should be an object", data.getLocation(),
                    instanceOf(AuthValidateResponse.Location.class));
        }

        if (data.getAddress() != null) {
            assertThat("address should be a string", data.getAddress(), instanceOf(String.class));
        }

        // Optional boolean fields
        if (data.getSellOnBizup() != null) {
            assertThat("sellOnBizup should be a boolean", data.getSellOnBizup(), instanceOf(Boolean.class));
        }
        if (data.getBusinessVerified() != null) {
            assertThat("businessVerified should be a boolean", data.getBusinessVerified(), instanceOf(Boolean.class));
        }
        if (data.getActivated() != null) {
            assertThat("activated should be a boolean", data.getActivated(), instanceOf(Boolean.class));
        }
        if (data.getIsDeleted() != null) {
            assertThat("isDeleted should be a boolean", data.getIsDeleted(), instanceOf(Boolean.class));
        }

        // Optional array fields
        if (data.getCategory() != null) {
            assertThat("category should be an array", data.getCategory(), instanceOf(java.util.List.class));
        }
        if (data.getBusinessCard() != null) {
            assertThat("businessCard should be an array", data.getBusinessCard(), instanceOf(java.util.List.class));
        }
        if (data.getTermsCondition() != null) {
            assertThat("termsCondition should be an array", data.getTermsCondition(), instanceOf(java.util.List.class));
        }
        if (data.getFo() != null) {
            assertThat("fo should be an array", data.getFo(), instanceOf(java.util.List.class));
        }

        logger.info("Data object structure validated");
    }

    @Test(description = "Location object contains required fields when present", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Auth Validate")
    @Severity(SeverityLevel.NORMAL)
    public void testLocationObjectFields() {
        // Location object contains required fields when present
        if (authValidateResponseData.getData() != null && authValidateResponseData.getData().getLocation() != null) {
            AuthValidateResponse.Location location = authValidateResponseData.getData().getLocation();

            assertThat("location should be an object", location, notNullValue());

            // Required location fields
            assertThat("country should be present", location.getCountry(), notNullValue());
            assertThat("city should be present", location.getCity(), notNullValue());
            assertThat("state should be present", location.getState(), notNullValue());
            assertThat("pincode should be present", location.getPincode(), notNullValue());
            assertThat("lat should be present", location.getLat(), notNullValue());
            assertThat("lng should be present", location.getLng(), notNullValue());

            // Optional location fields
            if (location.getName() != null) {
                assertThat("name should be a string", location.getName(), instanceOf(String.class));
            }

            logger.info("Location object validated: {}, {}", location.getCity(), location.getState());
        } else {
            logger.info("Location object not present - skipping validation");
        }
    }
}
