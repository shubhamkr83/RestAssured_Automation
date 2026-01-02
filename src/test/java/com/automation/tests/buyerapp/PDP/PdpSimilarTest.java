package com.automation.tests.buyerapp.PDP;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.PdpSimilarResponse;
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
 * Test class for PDP Similar Catalog API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/catalog/{{live_catalog_id}}/similar?size=20&page=0
 * Validates response structure, seller details, and product information.
 */
@Epic("Buyer App PDP")
@Feature("PDP Similar Catalog API")
public class PdpSimilarTest extends BaseTest {

    private static Response pdpSimilarResponse;
    private static PdpSimilarResponse pdpSimilarResponseData;
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
    @Story("PDP Similar Catalog")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatusCode200() {
        // Send GET request with authentication and query parameters
        pdpSimilarResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "JWT " + VariableManager.getBuyerAppToken())
                .queryParam("size", 20)
                .queryParam("page", 0)
                .when()
                .pathParam("catalogId", VariableManager.get("live_catalog_id", "67c59d8ff22202c05e7d612e"))
                .get("/v1/catalog/{catalogId}/similar");

        // Parse response for other tests
        pdpSimilarResponseData = JsonUtils.fromResponse(pdpSimilarResponse, PdpSimilarResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                pdpSimilarResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("PDP Similar Catalog")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = pdpSimilarResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Data object and nested array structure are valid", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("PDP Similar Catalog")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataObjectStructure() {
        // Data object and nested array structure are valid
        assertThat("Response should be an object", pdpSimilarResponseData, notNullValue());
        assertThat("data should exist and be an object", pdpSimilarResponseData.getData(), notNullValue());
        assertThat("data.data should exist and be an array",
                pdpSimilarResponseData.getData().getData(), instanceOf(java.util.List.class));

        logger.info("Data object and nested array structure validated");
    }

    @Test(description = "Each seller contains required fields", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("PDP Similar Catalog")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerRequiredFields() {
        // Each seller contains required fields
        assertThat("data.data should be an array that is not empty",
                pdpSimilarResponseData.getData().getData(), not(empty()));

        pdpSimilarResponseData.getData().getData().forEach(product -> {
            PdpSimilarResponse.Seller seller = product.getSeller();
            assertThat("seller should have phoneNumber", seller.getPhoneNumber(), notNullValue());
            assertThat("seller should have address", seller.getAddress(), notNullValue());
            assertThat("seller should have name", seller.getName(), notNullValue());
            assertThat("seller should have isCatalogAvailable", seller.getIsCatalogAvailable(), notNullValue());
        });

        logger.info("Seller required fields validated for {} products",
                pdpSimilarResponseData.getData().getData().size());
    }

    @Test(description = "Product array items have 'name' and 'id' fields", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("PDP Similar Catalog")
    @Severity(SeverityLevel.CRITICAL)
    public void testProductArrayFields() {
        // Product array items have 'name' and 'id' fields
        assertThat("data.data should be an array",
                pdpSimilarResponseData.getData().getData(), instanceOf(java.util.List.class));

        pdpSimilarResponseData.getData().getData().forEach(productItem -> {
            assertThat("product should be an array", productItem.getProduct(), instanceOf(java.util.List.class));

            productItem.getProduct().forEach(product -> {
                assertThat("product should be an object", product, notNullValue());
                assertThat("product should have 'name' property", product.getName(), notNullValue());
                assertThat("product should have 'id' property", product.getId(), notNullValue());
            });
        });

        logger.info("Product array fields validated");
    }
}
