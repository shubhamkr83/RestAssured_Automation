package com.automation.tests.buyerapp.ProductPage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.RelatedCollectionResponse;
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
 * Test class for Product Similar Collection (Related Collection) API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/collection/67c59d8ff22202c05e7d612e/related
 * Validates response structure, translations, and catalogs data.
 */
@Epic("Buyer App Product Page")
@Feature("Product Similar Collection API")
public class ProductSimilarCollectionTest extends BaseTest {

    private static Response relatedCollectionResponse;
    private static RelatedCollectionResponse relatedCollectionResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);
    }

    @Test(description = "Test the response status 200", priority = 1, groups = "buyerapp")
    @Story("Product Similar Collection")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatus() {
        // Send GET request with authentication
        relatedCollectionResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "Bearer " + VariableManager.getBuyerAppToken())
                .when()
                .pathParam("collectionId", VariableManager.get("collection_id"))
                .get("/v1/collection/{collectionId}/related");

        // Parse response for other tests
        relatedCollectionResponseData = JsonUtils.fromResponse(relatedCollectionResponse, RelatedCollectionResponse.class);

        // Test the response status 200
        assertThat("Test the response status 200",
                relatedCollectionResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Verify response has Content-Type header as application/json", priority = 2, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Product Similar Collection")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        // Verify response has Content-Type header as application/json
        assertThat("Content-Type should include application/json",
                relatedCollectionResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Content-Type header validated: {}", relatedCollectionResponse.getHeader("Content-Type"));
    }

    @Test(description = "Test response time is under the threshold", priority = 3, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Product Similar Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = relatedCollectionResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Test response time is under the threshold
        assertThat("Test response time is under the threshold",
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Validate 'result' array structure and element properties", priority = 4, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Product Similar Collection")
    @Severity(SeverityLevel.CRITICAL)
    public void testResultArrayStructure() {
        RelatedCollectionResponse.RelatedCollectionItem responseData = 
                relatedCollectionResponseData.getData().getResult().get(0);

        // Validate 'result' array structure and element properties
        assertThat("_id should be a string", responseData.get_id(), instanceOf(String.class));
        assertThat("name should be a string", responseData.getName(), instanceOf(String.class));
        assertThat("image should be a string", responseData.getImage(), instanceOf(String.class));

        assertThat("translations.en.title should be a string",
                responseData.getTranslations().getEn().getTitle(), instanceOf(String.class));
        assertThat("translations.hi.title should be a string",
                responseData.getTranslations().getHi().getTitle(), instanceOf(String.class));

        // Validate catalogs array structure
        responseData.getCatalogs().forEach(catalog -> {
            assertThat("catalog._id should be a string", catalog.get_id(), instanceOf(String.class));
            assertThat("catalog.title should be a string", catalog.getTitle(), instanceOf(String.class));
            assertThat("catalog.priceText should be a number", catalog.getPriceText(), instanceOf(Integer.class));
            assertThat("catalog.url should be a string", catalog.getUrl(), instanceOf(String.class));
            assertThat("catalog.product should be a string", catalog.getProduct(), instanceOf(String.class));
        });

        logger.info("Result array structure validated successfully");
    }

    @Test(description = "Verify 'name' and 'description' fields are non-empty strings in each result item", priority = 5, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Product Similar Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testNameAndDescriptionNonEmpty() {
        java.util.List<RelatedCollectionResponse.RelatedCollectionItem> responseData = 
                relatedCollectionResponseData.getData().getResult();

        // Verify 'name' and 'description' fields are non-empty strings in each result item
        responseData.forEach(item -> {
            assertThat("Name should not be empty",
                    item.getName(), not(emptyOrNullString()));
            assertThat("Description should not be empty",
                    item.getDescription(), not(emptyOrNullString()));
        });

        logger.info("Name and description fields validated as non-empty");
    }

    @Test(description = "Ensure 'catalogs' array exists and contains items in each result", priority = 6, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Product Similar Collection")
    @Severity(SeverityLevel.CRITICAL)
    public void testCatalogsArrayExists() {
        java.util.List<RelatedCollectionResponse.RelatedCollectionItem> responseData = 
                relatedCollectionResponseData.getData().getResult();

        // Ensure 'catalogs' array exists and contains items in each result
        responseData.forEach(item -> {
            assertThat("catalogs should be an array and have at least 1 item",
                    item.getCatalogs(), hasSize(greaterThanOrEqualTo(1)));
        });

        logger.info("Catalogs array validated - all items have at least 1 catalog");
    }
}
