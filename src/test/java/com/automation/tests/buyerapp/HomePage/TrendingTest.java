package com.automation.tests.buyerapp.HomePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.TrendingFeedResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static com.automation.tests.buyerapp.HomePage.FeedFilterSaveTest.suitableFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Trending Feed API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/feed/trending?size=1&page=0&suitable_for={{suitable_for}}
 * Validates response structure, items, thumbnails, and seller data.
 */
@Epic("Buyer App Home Page")
@Feature("Trending Feed API")
public class TrendingTest extends BaseTest {

    private static Response trendingFeedResponse;
    private static TrendingFeedResponse trendingFeedResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);
    }

    @Test(description = "Status code is 200", priority = 1, groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.CRITICAL)
    public void testStatusCode200() {
        // Get suitable_for parameter (use from previous test or default)
        String suitableForParam = (suitableFor != null && !suitableFor.isEmpty()) 
                ? suitableFor 
                : "saree"; // Default value

        // Send GET request with authentication and query parameters
        trendingFeedResponse = RestAssured.given()
                .baseUri(buyerAppBaseUrl)
                .contentType("application/json")
                .header("Authorization", "Bearer " + VariableManager.getBuyerAppToken())
                .queryParam("size", 1)
                .queryParam("page", 0)
                .queryParam("suitable_for", suitableForParam)
                .when()
                .get(BuyerAppEndpoints.FEED_HOME_TRENDING);

        // Parse response for other tests
        trendingFeedResponseData = JsonUtils.fromResponse(trendingFeedResponse, TrendingFeedResponse.class);

        // Status code is 200
        assertThat("Status code is 200",
                trendingFeedResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is within threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = trendingFeedResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is within threshold
        assertThat("Response time is within threshold",
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Content-Type header is present and application/json", priority = 3, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        // Content-Type header is present and application/json
        assertThat("Content-Type header should be present",
                trendingFeedResponse.getHeader("Content-Type"), notNullValue());
        assertThat("Content-Type should include application/json",
                trendingFeedResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Content-Type header validated: {}", trendingFeedResponse.getHeader("Content-Type"));
    }

    @Test(description = "Response is not unauthorized (401)", priority = 4, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.CRITICAL)
    public void testNotUnauthorized() {
        // Response is not unauthorized (401)
        assertThat("Response is not unauthorized (401)",
                trendingFeedResponse.getStatusCode(), not(equalTo(401)));

        logger.info("Response is not unauthorized - validated");
    }

    @Test(description = "Root response structure and values are correct", priority = 5, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.CRITICAL)
    public void testRootResponseStructure() {
        // Root response structure and values are correct
        assertThat("Response should be an object", trendingFeedResponseData, notNullValue());
        assertThat("statusCode should be '10000'", 
                trendingFeedResponseData.getStatusCode(), equalTo("10000"));
        assertThat("message should be 'success'", 
                trendingFeedResponseData.getMessage(), equalTo("success"));
        assertThat("data.result should be an array and not empty",
                trendingFeedResponseData.getData().getResult(), 
                allOf(instanceOf(java.util.List.class), not(empty())));

        logger.info("Root response structure validated");
    }

    @Test(description = "Each item has _id and id as non-empty strings", priority = 6, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.CRITICAL)
    public void testItemIdFields() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has _id and id as non-empty strings
        items.forEach(item -> {
            assertThat("_id should be a string", item.get_id(), instanceOf(String.class));
            assertThat("id should be a string", item.getId(), instanceOf(String.class));
            assertThat("_id should equal id", item.get_id(), equalTo(item.getId()));
        });

        logger.info("Item _id and id fields validated");
    }

    @Test(description = "Each item has price and priceText as numbers and equal", priority = 7, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testItemPriceFields() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has price and priceText as numbers and equal
        items.forEach(item -> {
            assertThat("priceText should be a number", item.getPriceText(), instanceOf(Integer.class));
            assertThat("price should equal priceText", item.getPrice(), equalTo(item.getPriceText()));
        });

        logger.info("Item price fields validated");
    }

    @Test(description = "Each item has valid thumbnail_url and matches thumbnail.url", priority = 8, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testItemThumbnailUrl() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has valid thumbnail_url and matches thumbnail.url
        items.forEach(item -> {
            assertThat("thumbnail_url should be a string", item.getThumbnail_url(), instanceOf(String.class));
            if (item.getThumbnail() != null) {
                assertThat("thumbnail_url should equal thumbnail.url", 
                        item.getThumbnail_url(), equalTo(item.getThumbnail().getUrl()));
            }
        });

        logger.info("Item thumbnail_url validated");
    }

    @Test(description = "Each item has valid popular and contentType fields", priority = 9, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testItemPopularAndContentType() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has valid popular and contentType fields
        items.forEach(item -> {
            assertThat("popular should be a number", item.getPopular(), instanceOf(Integer.class));
            assertThat("contentType should be a string", item.getContentType(), instanceOf(String.class));
        });

        logger.info("Item popular and contentType fields validated");
    }

    @Test(description = "Each item has valid driveLink URL", priority = 10, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testItemDriveLink() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has valid driveLink URL
        items.forEach(item -> {
            assertThat("driveLink should match URL pattern", 
                    item.getDriveLink(), matchesRegex("^https?://.*"));
        });

        logger.info("Item driveLink URLs validated");
    }

    @Test(description = "Each item has non-empty product array", priority = 11, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testItemProductArray() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has non-empty product array
        items.forEach(item -> {
            assertThat("product should be an array and not empty",
                    item.getProduct(), allOf(instanceOf(java.util.List.class), not(empty())));
        });

        logger.info("Item product arrays validated");
    }

    @Test(description = "Each item has boolean fields: isDeleted=false, available=true", priority = 12, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testItemBooleanFields() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has boolean fields: isDeleted=false, available=true
        items.forEach(item -> {
            assertThat("isDeleted should be a boolean and false",
                    item.getIsDeleted(), allOf(instanceOf(Boolean.class), is(false)));
            assertThat("available should be a boolean and true",
                    item.getAvailable(), allOf(instanceOf(Boolean.class), is(true)));
        });

        logger.info("Item boolean fields validated");
    }

    @Test(description = "Each item has a valid thumbnail object", priority = 13, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.CRITICAL)
    public void testItemThumbnailObject() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has a valid thumbnail object
        items.forEach(item -> {
            assertThat("thumbnail should be an object", item.getThumbnail(), notNullValue());
            assertThat("thumbnail.url should match URL pattern",
                    item.getThumbnail().getUrl(), matchesRegex("^https?://.*"));
            assertThat("thumbnail.fileName should be a string",
                    item.getThumbnail().getFileName(), instanceOf(String.class));
        });

        logger.info("Item thumbnail objects validated");
    }

    @Test(description = "Each item has valid seller and deprioritisation_status object and fields", priority = 14, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testItemSellerObject() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has valid seller and deprioritisation_status object and fields
        items.forEach(item -> {
            TrendingFeedResponse.Seller seller = item.getSeller();
            assertThat("seller should be an object", seller, notNullValue());
            assertThat("seller.deprioritisation_status should be a boolean and false",
                    seller.getDeprioritisation_status(), allOf(instanceOf(Boolean.class), is(false)));
        });

        logger.info("Item seller objects validated");
    }
}
