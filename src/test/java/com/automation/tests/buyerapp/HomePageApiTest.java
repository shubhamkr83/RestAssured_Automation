package com.automation.tests.buyerapp;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.FeedFilterSaveRequest;
import com.automation.models.response.FeedFilterResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Buyer App HomePage/Feed APIs.
 */
@Epic("Buyer App Home Feed")
@Feature("HomePage API")
public class HomePageApiTest extends BaseTest {

        private String authToken;
        private String buyerAppBaseUrl;
        public static String suitableFor;

        @BeforeClass
        public void setupAuth() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();

                if (BuyerLoginApiTest.buyerAppToken != null) {
                        authToken = BuyerLoginApiTest.buyerAppToken;
                        logger.info("Using Buyer App token from BuyerLoginApiTest");
                } else {
                        throw new RuntimeException(
                                        "Buyer App token not available. Please run BuyerLoginApiTest first.");
                }
        }

        @Test(description = "Verify feed filter save", priority = 1)
        @Story("Feed Filters")
        @Severity(SeverityLevel.CRITICAL)
        public void testFeedFilterSave() {
                FeedFilterSaveRequest request = FeedFilterSaveRequest.builder()
                                .suitable_for(Arrays.asList("saree"))
                                .testData("")
                                .build();

                Response response = RestAssured.given()
                                .baseUri(buyerAppBaseUrl)
                                .header("Authorization", "JWT " + authToken)
                                .header("AppVersion", "3.2.0-debug")
                                .header("AppVersionCode", "154")
                                .header("User-Segment", "2")
                                .header("Accept-Language", "en")
                                .contentType("application/json")
                                .body(request)
                                .when()
                                .post(BuyerAppEndpoints.FEED_FILTERS_SAVE);

                // Validate response
                assertThat("Status code should be 200",
                                response.getStatusCode(), equalTo(HttpStatus.OK));

                assertThat("Response time should be under 800ms",
                                response.getTime(), lessThan(800L));

                JsonPath jsonPath = response.jsonPath();

                // Validate response schema
                assertThat("statusCode should be present", jsonPath.get("statusCode"), notNullValue());
                assertThat("message should be present", jsonPath.get("message"), notNullValue());
                assertThat("data should be object", jsonPath.get("data"), instanceOf(Map.class));

                // Validate data structure
                assertThat("suitable_for should be array", jsonPath.getList("data.suitable_for"), notNullValue());
                assertThat("productTags should be array", jsonPath.getList("data.productTags"), notNullValue());
                assertThat("city should be array", jsonPath.getList("data.city"), notNullValue());

                // Store suitable_for for subsequent tests
                if (jsonPath.getList("data.suitable_for") != null && !jsonPath.getList("data.suitable_for").isEmpty()) {
                        suitableFor = (String) jsonPath.getList("data.suitable_for").get(0);
                        logger.info("Stored suitable_for: {}", suitableFor);
                }
        }

        @Test(description = "Verify feed filters get", priority = 2)
        @Story("Feed Filters")
        @Severity(SeverityLevel.CRITICAL)
        public void testFeedFiltersGet() {
                Response response = RestAssured.given()
                                .baseUri(buyerAppBaseUrl)
                                .header("Authorization", "JWT " + authToken)
                                .header("AppVersion", "3.2.0-debug")
                                .header("AppVersionCode", "154")
                                .header("User-Segment", "2")
                                .header("Accept-Language", "en")
                                .when()
                                .get(BuyerAppEndpoints.FEED_FILTERS);

                // Validate response
                assertThat("Status code should be 200",
                                response.getStatusCode(), equalTo(HttpStatus.OK));

                assertThat("Response time should be acceptable",
                                response.getTime(), lessThan((long) config.responseTimeThreshold()));

                // Parse response
                FeedFilterResponse filterResponse = JsonUtils.fromResponse(response, FeedFilterResponse.class);

                // Validate structure
                assertThat("Data should not be null", filterResponse.getData(), notNullValue());
                assertThat("productTags should be array", filterResponse.getData().getProductTags(), notNullValue());
                assertThat("suitable_for should be array", filterResponse.getData().getSuitable_for(), notNullValue());
                assertThat("city should be array", filterResponse.getData().getCity(), notNullValue());
                assertThat("priceFilters should be array", filterResponse.getData().getPriceFilters(), notNullValue());

                // Validate suitable_for array
                assertThat("suitable_for should have at least 1 item",
                                filterResponse.getData().getSuitable_for().size(), greaterThanOrEqualTo(1));

                // Validate city array
                assertThat("city should have at least 1 item",
                                filterResponse.getData().getCity().size(), greaterThanOrEqualTo(1));

                // Validate price filters
                if (!filterResponse.getData().getPriceFilters().isEmpty()) {
                        filterResponse.getData().getPriceFilters().forEach(filter -> {
                                filter.getRanges().forEach(range -> {
                                        assertThat("Price min should not be negative",
                                                        range.getPrice_min(), greaterThanOrEqualTo(0));
                                        assertThat("Price max should not be negative",
                                                        range.getPrice_max(), greaterThanOrEqualTo(0));
                                });
                        });
                }

                // Validate productTags structure
                if (!filterResponse.getData().getProductTags().isEmpty()) {
                        FeedFilterResponse.ProductTag tag = filterResponse.getData().getProductTags().get(0);
                        assertThat("Tag name should be string", tag.getName(), instanceOf(String.class));
                        assertThat("Tag image should be string", tag.getImage(), instanceOf(String.class));
                        assertThat("Tag visible should be boolean", tag.getVisible(), instanceOf(Boolean.class));
                        assertThat("Tag selected should be boolean", tag.getSelected(), instanceOf(Boolean.class));
                        assertThat("Tag displayName should be string", tag.getDisplayName(), instanceOf(String.class));
                }
        }

        @Test(description = "Verify feed banners", priority = 3)
        @Story("Feed Banners")
        @Severity(SeverityLevel.NORMAL)
        public void testFeedBanners() {
                Map<String, Object> queryParams = new HashMap<>();
                queryParams.put("suitable_for", suitableFor != null ? suitableFor : "saree");

                Response response = RestAssured.given()
                                .baseUri(buyerAppBaseUrl)
                                .header("Authorization", "JWT " + authToken)
                                .header("AppVersion", "3.2.0-debug")
                                .header("AppVersionCode", "154")
                                .header("User-Segment", "2")
                                .header("Accept-Language", "en")
                                .queryParams(queryParams)
                                .when()
                                .get(BuyerAppEndpoints.FEED_BANNERS);

                // Validate response
                assertThat("Status code should be 200",
                                response.getStatusCode(), equalTo(HttpStatus.OK));

                assertThat("Response time should be acceptable",
                                response.getTime(), lessThan((long) config.responseTimeThreshold()));

                // Validate Content-Type
                assertThat("Content-Type should be application/json",
                                response.getContentType(), containsString("application/json"));

                JsonPath jsonPath = response.jsonPath();

                // Validate response structure
                assertThat("statusCode should not be empty", jsonPath.getString("statusCode"),
                                not(emptyOrNullString()));
                assertThat("message should be present", jsonPath.getString("message"), notNullValue());
                assertThat("data.result should be array", jsonPath.getList("data.result"), notNullValue());

                // Validate banner structure
                if (jsonPath.getList("data.result") != null && !jsonPath.getList("data.result").isEmpty()) {
                        assertThat("Result should have at least 1 banner",
                                        jsonPath.getList("data.result").size(), greaterThanOrEqualTo(1));

                        // Validate first banner structure
                        assertThat("Banner should have imageUrl",
                                        jsonPath.getString("data.result[0].value.value.imageUrl"), notNullValue());
                        assertThat("Banner should have clickType",
                                        jsonPath.getString("data.result[0].value.value.clickType"), notNullValue());
                }
        }

        @Test(description = "Verify featured collection", priority = 4)
        @Story("Featured Collection")
        @Severity(SeverityLevel.NORMAL)
        public void testFeaturedCollection() {
                Response response = RestAssured.given()
                                .baseUri(buyerAppBaseUrl)
                                .header("Authorization", "JWT " + authToken)
                                .header("AppVersion", "3.2.0-debug")
                                .header("AppVersionCode", "154")
                                .header("User-Segment", "2")
                                .header("Accept-Language", "en")
                                .when()
                                .get(BuyerAppEndpoints.FEED_JOURNEY_COLLECTION);

                // Validate response
                assertThat("Status code should be 200",
                                response.getStatusCode(), equalTo(HttpStatus.OK));

                assertThat("Response time should be acceptable",
                                response.getTime(), lessThan((long) config.responseTimeThreshold()));

                assertThat("Content-Type header should be present",
                                response.getHeader("Content-Type"), notNullValue());

                JsonPath jsonPath = response.jsonPath();

                // Validate required fields
                assertThat("statusCode should be present", jsonPath.get("statusCode"), notNullValue());
                assertThat("message should be present", jsonPath.get("message"), notNullValue());
                assertThat("data should be present", jsonPath.get("data"), notNullValue());
        }

        @Test(description = "Verify catalog feed with pagination", priority = 5)
        @Story("Catalog Feed")
        @Severity(SeverityLevel.CRITICAL)
        public void testCatalogFeed() {
                Map<String, Object> queryParams = new HashMap<>();
                queryParams.put("size", 6);
                queryParams.put("page", 0);
                queryParams.put("suitable_for", suitableFor != null ? suitableFor : "saree");

                Response response = RestAssured.given()
                                .baseUri(buyerAppBaseUrl)
                                .header("Authorization", "JWT " + authToken)
                                .header("AppVersion", "3.2.0-debug")
                                .header("AppVersionCode", "154")
                                .header("User-Segment", "2")
                                .header("Accept-Language", "en")
                                .queryParams(queryParams)
                                .when()
                                .get(BuyerAppEndpoints.FEED_HOME_CATALOG);

                // Validate response
                assertThat("Status code should be 200",
                                response.getStatusCode(), equalTo(HttpStatus.OK));

                assertThat("Response should have JSON body", response.getBody().asString(), not(emptyOrNullString()));

                assertThat("Response time should be acceptable",
                                response.getTime(), lessThan((long) config.responseTimeThreshold()));

                assertThat("Content-Type should be application/json",
                                response.getContentType(), containsString("application/json"));
        }

        @Test(description = "Verify trending items", priority = 6)
        @Story("Trending")
        @Severity(SeverityLevel.NORMAL)
        public void testTrendingItems() {
                Map<String, Object> queryParams = new HashMap<>();
                queryParams.put("suitable_for", suitableFor != null ? suitableFor : "saree");

                Response response = RestAssured.given()
                                .baseUri(buyerAppBaseUrl)
                                .header("Authorization", "JWT " + authToken)
                                .header("AppVersion", "3.2.0-debug")
                                .header("AppVersionCode", "154")
                                .header("User-Segment", "2")
                                .header("Accept-Language", "en")
                                .queryParams(queryParams)
                                .when()
                                .get(BuyerAppEndpoints.FEED_HOME_TRENDING);

                // Validate response
                assertThat("Status code should be 200",
                                response.getStatusCode(), equalTo(HttpStatus.OK));

                assertThat("Response time should be acceptable",
                                response.getTime(), lessThan((long) config.responseTimeThreshold()));

                assertThat("Content-Type should be application/json",
                                response.getContentType(), containsString("application/json"));

                assertThat("Response should not be unauthorized",
                                response.getStatusCode(), not(equalTo(401)));

                JsonPath jsonPath = response.jsonPath();

                // Validate root structure
                assertThat("statusCode should be 10000", jsonPath.getString("statusCode"), equalTo("10000"));
                assertThat("message should be success", jsonPath.getString("message"), equalTo("success"));
                assertThat("data.result should be array and not empty",
                                jsonPath.getList("data.result"), not(empty()));

                // Validate items structure
                if (!jsonPath.getList("data.result").isEmpty()) {
                        assertThat("Item should have _id", jsonPath.get("data.result[0]._id"), notNullValue());
                        assertThat("Item should have id", jsonPath.get("data.result[0].id"), notNullValue());
                        assertThat("Item should have price", jsonPath.get("data.result[0].price"), notNullValue());
                        assertThat("Item should have thumbnail_url", jsonPath.get("data.result[0].thumbnail_url"),
                                        notNullValue());
                }
        }

        @Test(description = "Verify new this week items", priority = 7)
        @Story("New This Week")
        @Severity(SeverityLevel.NORMAL)
        public void testNewThisWeek() {
                Map<String, Object> queryParams = new HashMap<>();
                queryParams.put("size", 1);
                queryParams.put("page", 0);
                queryParams.put("suitable_for", suitableFor != null ? suitableFor : "saree");

                Response response = RestAssured.given()
                                .baseUri(buyerAppBaseUrl)
                                .header("Authorization", "JWT " + authToken)
                                .header("AppVersion", "3.2.0-debug")
                                .header("AppVersionCode", "154")
                                .header("User-Segment", "2")
                                .header("Accept-Language", "en")
                                .queryParams(queryParams)
                                .when()
                                .get(BuyerAppEndpoints.FEED_NEW_THIS_WEEK);

                // Validate response
                assertThat("Status code should be 200",
                                response.getStatusCode(), equalTo(HttpStatus.OK));

                assertThat("Response time should be acceptable",
                                response.getTime(), lessThan((long) config.responseTimeThreshold()));

                assertThat("Content-Type should be application/json",
                                response.getContentType(), containsString("application/json"));

                JsonPath jsonPath = response.jsonPath();

                // Validate response structure
                assertThat("statusCode should be present", jsonPath.get("statusCode"), notNullValue());
                assertThat("statusCode should be 10000", jsonPath.getString("statusCode"), equalTo("10000"));
                assertThat("message should be present", jsonPath.get("message"), notNullValue());
                assertThat("message should be success", jsonPath.getString("message"), equalTo("success"));
                assertThat("data should be present", jsonPath.get("data"), notNullValue());

                // Validate data structure
                assertThat("data.result should be array", jsonPath.getList("data.result"), notNullValue());

                // Validate pagination in data
                assertThat("data should have total", jsonPath.get("data.total"), notNullValue());
                assertThat("data should have size", jsonPath.get("data.size"), notNullValue());
                assertThat("data should have page", jsonPath.get("data.page"), notNullValue());

                // Validate items if present
                if (!jsonPath.getList("data.result").isEmpty()) {
                        assertThat("Result should have at least 1 item",
                                        jsonPath.getList("data.result").size(), greaterThanOrEqualTo(1));

                        // Validate item structure
                        Map<String, Object> item = jsonPath.getMap("data.result[0]");
                        assertThat("Item should have _id", item.get("_id"), notNullValue());
                        assertThat("Item should have id", item.get("id"), notNullValue());

                        // Validate price if present
                        if (item.containsKey("price")) {
                                assertThat("Item price should not be negative",
                                                ((Number) item.get("price")).intValue(), greaterThanOrEqualTo(0));
                        }

                        // Validate thumbnail URL
                        if (item.containsKey("thumbnail_url")) {
                                assertThat("thumbnail_url should be string",
                                                item.get("thumbnail_url"), instanceOf(String.class));
                        }

                        // Validate driveLink if present
                        if (item.containsKey("driveLink")) {
                                assertThat("driveLink should be string",
                                                item.get("driveLink"), instanceOf(String.class));
                        }
                }

                logger.info("New This Week test completed successfully");
        }
}
