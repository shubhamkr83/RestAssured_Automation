package com.automation.tests.buyerapp;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogByIdDetailResponse;
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
 * Test class for Catalog By ID API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/catalog/{{live_catalog_id}}
 * Validates response structure and catalog details.
 */
@Epic("Buyer App Catalog")
@Feature("Catalog By ID API")
public class CatalogByIdTest extends BaseTest {

    private static Response catalogResponse;
    private static CatalogByIdDetailResponse catalogResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatusCode200() {
        // Send GET request with authentication
        catalogResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "Bearer " + VariableManager.getBuyerAppToken())
                .when()
                .pathParam("catalogId", VariableManager.get("live_catalog_id", "67c59d8ff22202c05e7d612e"))
                .get("/v1/catalog/{catalogId}");

        // Parse response for other tests
        catalogResponseData = JsonUtils.fromResponse(catalogResponse, CatalogByIdDetailResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                catalogResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response has valid Content-Type header", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        // Response has valid Content-Type header
        assertThat("Content-Type should include application/json",
                catalogResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Content-Type header verified: {}", catalogResponse.getHeader("Content-Type"));
    }

    @Test(description = "Response time is less than threshold", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = catalogResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Response has required top-level fields", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.CRITICAL)
    public void testTopLevelFields() {
        // Response has required top-level fields
        assertThat("Response should be an object", catalogResponseData, notNullValue());
        assertThat("statusCode should be present", catalogResponseData.getStatusCode(), notNullValue());
        assertThat("message should be present", catalogResponseData.getMessage(), notNullValue());
        assertThat("data should be present", catalogResponseData.getData(), notNullValue());

        logger.info("Top-level fields validated: statusCode, message, data");
    }

    @Test(description = "Data object has correct structure", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataObjectStructure() {
        if (catalogResponseData.getData() != null) {
            CatalogByIdDetailResponse.CatalogDetailData data = catalogResponseData.getData();
            
            assertThat("data should be an object", data, notNullValue());
            assertThat("seller should be an object", data.getSeller(), notNullValue());
            assertThat("taggedBy should be a string", data.getTaggedBy(), instanceOf(String.class));

            if (data.getThumbnail() != null) {
                assertThat("thumbnail should be an object", data.getThumbnail(), notNullValue());
            }
            if (data.getProduct() != null) {
                assertThat("product should be an array", data.getProduct(), instanceOf(java.util.List.class));
            }
            if (data.getMarket() != null) {
                assertThat("market should be an object", data.getMarket(), notNullValue());
            }
            if (data.getImages() != null) {
                assertThat("images should be an array", data.getImages(), instanceOf(java.util.List.class));
            }

            logger.info("Data object structure validated");
        }
    }

    @Test(description = "Seller object contains required fields", priority = 6, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerObjectFields() {
        if (catalogResponseData.getData() != null && catalogResponseData.getData().getSeller() != null) {
            CatalogByIdDetailResponse.Seller seller = catalogResponseData.getData().getSeller();
            
            assertThat("seller should be an object", seller, notNullValue());
            assertThat("phoneNumber should be a string", seller.getPhoneNumber(), instanceOf(String.class));
            assertThat("address should be a string", seller.getAddress(), instanceOf(String.class));
            assertThat("businessName should be a string", seller.getBusinessName(), instanceOf(String.class));
            assertThat("name should be a string", seller.getName(), instanceOf(String.class));

            if (seller.getIsSuper() != null) {
                assertThat("isSuper should be a boolean", seller.getIsSuper(), instanceOf(Boolean.class));
            }
            if (seller.getDeprioritisation_status() != null) {
                assertThat("deprioritisation_status should be a boolean", seller.getDeprioritisation_status(), instanceOf(Boolean.class));
            }
            if (seller.getIsCatalogAvailable() != null) {
                assertThat("isCatalogAvailable should be a boolean", seller.getIsCatalogAvailable(), instanceOf(Boolean.class));
            }
            if (seller.getOrdersEnabled() != null) {
                assertThat("ordersEnabled should be a boolean", seller.getOrdersEnabled(), instanceOf(Boolean.class));
            }
            if (seller.getMov() != null) {
                assertThat("mov should be a number", seller.getMov(), instanceOf(Integer.class));
            }

            logger.info("Seller object fields validated");
        }
    }

    @Test(description = "Product array contains valid items", priority = 7, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.NORMAL)
    public void testProductArrayItems() {
        if (catalogResponseData.getData() != null && catalogResponseData.getData().getProduct() != null) {
            catalogResponseData.getData().getProduct().forEach(product -> {
                assertThat("product should be an object", product, notNullValue());
                assertThat("name should be a non-empty string", 
                        product.getName(), allOf(instanceOf(String.class), not(emptyOrNullString())));
                assertThat("id should be a non-empty string", 
                        product.getId(), allOf(instanceOf(String.class), not(emptyOrNullString())));
            });

            logger.info("Product array validated: {} products", catalogResponseData.getData().getProduct().size());
        }
    }

    @Test(description = "Thumbnail object contains required fields", priority = 8, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.NORMAL)
    public void testThumbnailObjectFields() {
        if (catalogResponseData.getData() != null && catalogResponseData.getData().getThumbnail() != null) {
            CatalogByIdDetailResponse.Thumbnail thumbnail = catalogResponseData.getData().getThumbnail();
            
            assertThat("fileName should be a string", thumbnail.getFileName(), instanceOf(String.class));
            assertThat("mimeType should be a string", thumbnail.getMimeType(), instanceOf(String.class));
            assertThat("_id should be a string", thumbnail.get_id(), instanceOf(String.class));
            assertThat("url should be a string", thumbnail.getUrl(), instanceOf(String.class));

            logger.info("Thumbnail object validated");
        }
    }

    @Test(description = "Market object contains required fields", priority = 9, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.NORMAL)
    public void testMarketObjectFields() {
        if (catalogResponseData.getData() != null && catalogResponseData.getData().getMarket() != null) {
            CatalogByIdDetailResponse.Market market = catalogResponseData.getData().getMarket();
            
            assertThat("country should be present", market.getCountry(), notNullValue());
            assertThat("city should be present", market.getCity(), notNullValue());
            assertThat("state should be present", market.getState(), notNullValue());
            assertThat("name should be present", market.getName(), notNullValue());
            assertThat("marketNumber should be present", market.getMarketNumber(), notNullValue());
            assertThat("createdAt should be present", market.getCreatedAt(), notNullValue());
            assertThat("updatedAt should be present", market.getUpdatedAt(), notNullValue());

            logger.info("Market object validated: {}, {}", market.getName(), market.getCity());
        }
    }

    @Test(description = "Images array contains valid items", priority = 10, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.NORMAL)
    public void testImagesArrayItems() {
        if (catalogResponseData.getData() != null && catalogResponseData.getData().getImages() != null) {
            assertThat("images array should have at least one item",
                    catalogResponseData.getData().getImages(), hasSize(greaterThan(0)));

            logger.info("Images array validated: {} images", catalogResponseData.getData().getImages().size());
        }
    }
}
