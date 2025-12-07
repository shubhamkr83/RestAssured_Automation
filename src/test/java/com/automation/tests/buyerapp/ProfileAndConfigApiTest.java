package com.automation.tests.buyerapp;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Buyer App Profile and Configuration APIs.
 */
@Epic("Buyer App Profile & Config")
@Feature("Profile and Config API")
public class ProfileAndConfigApiTest extends BaseTest {

    private String authToken;
    private String buyerAppBaseUrl;

    @BeforeClass
    public void setupAuth() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();

        if (BuyerLoginApiTest.buyerAppToken != null) {
            authToken = BuyerLoginApiTest.buyerAppToken;
            logger.info("Using Buyer App token from BuyerLoginApiTest");
        } else {
            throw new RuntimeException("Buyer App token not available. Please run BuyerLoginApiTest first.");
        }
    }

    @Test(description = "Verify auth validate endpoint", priority = 1)
    @Story("Auth Validate")
    @Severity(SeverityLevel.CRITICAL)
    public void testAuthValidate() {
        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .header("Authorization", "JWT " + authToken)
                .header("AppVersion", "3.2.0-debug")
                .header("AppVersionCode", "154")
                .header("User-Segment", "2")
                .header("Accept-Language", "en")
                .when()
                .get(BuyerAppEndpoints.AUTH_VALIDATE);

        // Validate response
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        assertThat("Response time should be acceptable",
                response.getTime(), lessThan((long) config.responseTimeThreshold()));

        JsonPath jsonPath = response.jsonPath();

        // Validate response structure
        assertThat("statusCode should be present", jsonPath.get("statusCode"), notNullValue());
        assertThat("message should be present", jsonPath.get("message"), notNullValue());
        assertThat("data should be present", jsonPath.get("data"), notNullValue());

        // Validate user data fields
        assertThat("_id should be string", jsonPath.get("data._id"), instanceOf(String.class));
        assertThat("phoneNumber should be string", jsonPath.get("data.phoneNumber"), instanceOf(String.class));
        assertThat("name should be string", jsonPath.get("data.name"), instanceOf(String.class));
        assertThat("businessName should be string", jsonPath.get("data.businessName"), instanceOf(String.class));
        assertThat("createdAt should be string", jsonPath.get("data.createdAt"), instanceOf(String.class));
        assertThat("updatedAt should be string", jsonPath.get("data.updatedAt"), instanceOf(String.class));
    }

    @Test(description = "Verify location object in auth validate", dependsOnMethods = "testAuthValidate")
    @Story("Auth Validate")
    @Severity(SeverityLevel.NORMAL)
    public void testAuthValidateLocation() {
        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .header("Authorization", "JWT " + authToken)
                .header("AppVersion", "3.2.0-debug")
                .header("AppVersionCode", "154")
                .header("User-Segment", "2")
                .header("Accept-Language", "en")
                .when()
                .get(BuyerAppEndpoints.AUTH_VALIDATE);

        JsonPath jsonPath = response.jsonPath();

        // Validate location object if present
        if (jsonPath.get("data.location") != null) {
            assertThat("Location should be an object", jsonPath.get("data.location"), instanceOf(Map.class));

            // Validate required location fields
            assertThat("Location should have country", jsonPath.get("data.location.country"), notNullValue());
            assertThat("Location should have city", jsonPath.get("data.location.city"), notNullValue());
            assertThat("Location should have state", jsonPath.get("data.location.state"), notNullValue());
            assertThat("Location should have pincode", jsonPath.get("data.location.pincode"), notNullValue());
            assertThat("Location should have lat", jsonPath.get("data.location.lat"), notNullValue());
            assertThat("Location should have lng", jsonPath.get("data.location.lng"), notNullValue());
        }
    }

    @Test(description = "Verify boolean fields in auth validate", dependsOnMethods = "testAuthValidate")
    @Story("Auth Validate")
    @Severity(SeverityLevel.NORMAL)
    public void testAuthValidateBooleanFields() {
        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .header("Authorization", "JWT " + authToken)
                .header("AppVersion", "3.2.0-debug")
                .header("AppVersionCode", "154")
                .header("User-Segment", "2")
                .header("Accept-Language", "en")
                .when()
                .get(BuyerAppEndpoints.AUTH_VALIDATE);

        JsonPath jsonPath = response.jsonPath();

        // Validate boolean fields if present
        String[] booleanFields = { "sellOnBizup", "businessVerified", "activated", "isDeleted" };

        for (String field : booleanFields) {
            if (jsonPath.get("data." + field) != null) {
                assertThat(field + " should be boolean",
                        jsonPath.get("data." + field), instanceOf(Boolean.class));
            }
        }
    }

    @Test(description = "Verify array fields in auth validate", dependsOnMethods = "testAuthValidate")
    @Story("Auth Validate")
    @Severity(SeverityLevel.NORMAL)
    public void testAuthValidateArrayFields() {
        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .header("Authorization", "JWT " + authToken)
                .header("AppVersion", "3.2.0-debug")
                .header("AppVersionCode", "154")
                .header("User-Segment", "2")
                .header("Accept-Language", "en")
                .when()
                .get(BuyerAppEndpoints.AUTH_VALIDATE);

        JsonPath jsonPath = response.jsonPath();

        // Validate array fields if present
        String[] arrayFields = { "category", "businessCard", "termsCondition", "fo" };

        for (String field : arrayFields) {
            if (jsonPath.get("data." + field) != null) {
                assertThat(field + " should be an array",
                        jsonPath.getList("data." + field), notNullValue());
            }
        }
    }

    @Test(description = "Verify app update configuration", priority = 2)
    @Story("App Config")
    @Severity(SeverityLevel.NORMAL)
    public void testAppUpdate() {
        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .header("Authorization", "JWT " + authToken)
                .header("AppVersion", "3.2.0-debug")
                .header("AppVersionCode", "154")
                .header("User-Segment", "2")
                .header("Accept-Language", "en")
                .when()
                .get(BuyerAppEndpoints.APP_UPDATE);

        // Validate response
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        assertThat("Response time should be acceptable",
                response.getTime(), lessThan((long) config.responseTimeThreshold()));

        assertThat("Content-Type should be application/json",
                response.getContentType(), containsString("application/json"));

        JsonPath jsonPath = response.jsonPath();

        // Validate response fields
        assertThat("Response should have code", jsonPath.get("code"), notNullValue());
        assertThat("Response should have message", jsonPath.get("message"), notNullValue());
        assertThat("Response should have data", jsonPath.get("data"), notNullValue());

        // Validate data properties
        assertThat("isForced should be boolean", jsonPath.get("data.isForced"), instanceOf(Boolean.class));
        assertThat("minVersion should be number", jsonPath.get("data.minVersion"), instanceOf(Number.class));
        assertThat("minVersionToUpdate should be number", jsonPath.get("data.minVersionToUpdate"),
                instanceOf(Number.class));

        // Validate version numbers are non-negative
        assertThat("minVersion should be non-negative",
                jsonPath.getInt("data.minVersion"), greaterThanOrEqualTo(0));
        assertThat("minVersionToUpdate should be non-negative",
                jsonPath.getInt("data.minVersionToUpdate"), greaterThanOrEqualTo(0));
    }

    @Test(description = "Verify suitable for configuration", priority = 3)
    @Story("App Config")
    @Severity(SeverityLevel.NORMAL)
    public void testSuitableForConfig() {
        Response response = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .header("Authorization", "JWT " + authToken)
                .header("AppVersion", "3.2.0-debug")
                .header("AppVersionCode", "154")
                .header("User-Segment", "2")
                .header("Accept-Language", "en")
                .when()
                .get(BuyerAppEndpoints.SUITABLE_FOR_CONFIG);

        // Validate response
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        assertThat("Content-Type header should be present",
                response.getHeader("Content-Type"), notNullValue());

        assertThat("Response time should be acceptable",
                response.getTime(), lessThan((long) config.responseTimeThreshold()));

        JsonPath jsonPath = response.jsonPath();

        // Validate response structure
        assertThat("data should be object", jsonPath.get("data"), notNullValue());
        assertThat("data.result should be array", jsonPath.getList("data.result"), notNullValue());
        assertThat("data.result should have at least 1 item",
                jsonPath.getList("data.result").size(), greaterThanOrEqualTo(1));

        // Validate result items structure
        if (!jsonPath.getList("data.result").isEmpty()) {
            assertThat("Item should have type", jsonPath.get("data.result[0].type"), instanceOf(String.class));
            assertThat("Item type should not be empty",
                    jsonPath.getString("data.result[0].type"), not(emptyString()));
            assertThat("Item should have data array", jsonPath.get("data.result[0].data"),
                    instanceOf(java.util.List.class));
            assertThat("Item should have api", jsonPath.get("data.result[0].api"), instanceOf(String.class));
            assertThat("Item should have title", jsonPath.get("data.result[0].title"), instanceOf(String.class));
            assertThat("Item should have description", jsonPath.get("data.result[0].description"),
                    instanceOf(String.class));
            assertThat("Item should have imageUrl", jsonPath.get("data.result[0].imageUrl"), instanceOf(String.class));
        }
    }
}
