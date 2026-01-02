package com.automation.tests.bomb.CatalogSearch;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.CatalogEditRequest;
import com.automation.models.response.CatalogEditResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Edit endpoint.
 * Endpoint: {{bizup_base}}/v1/admin/catalog/{{catalog_id}}
 * Implements comprehensive Postman test scripts for catalog edit validation.
 */
@Epic("BOMB Catalog Management")
@Feature("Catalog Edit")
public class CatalogEditTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogEditResponse catalogEditResponse;

    // Generated test data (equivalent to collection variables in Postman)
    private String generatedTitle;
    private Integer generatedPrice;
    private String catalogId;
    private String productId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
        // Get catalog ID from VariableManager
        catalogId = VariableManager.get("catalog_id");
        if (catalogId == null || catalogId.isEmpty()) {
            throw new RuntimeException("Catalog ID not available. Please run a catalog creation test first.");
        }
        logger.info("Using catalog ID from VariableManager: {}", catalogId);
        
        // Get product ID from VariableManager
        productId = VariableManager.get("product_id");
        if (productId == null || productId.isEmpty()) {
            throw new RuntimeException("Product ID not available. Please ensure product_id is set.");
        }
        logger.info("Using product ID from VariableManager: {}", productId);
    }

    @Test(description = "Status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Edit")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Generate random test data (similar to Postman pre-request script)
        generatedTitle = generateTestTitle();
        generatedPrice = generateTestPrice();

        logger.info("Generated test title: {}", generatedTitle);
        logger.info("Generated test price: {}", generatedPrice);

        // Create image object (from the provided example)
        CatalogEditRequest.Image image = CatalogEditRequest.Image.builder()
                .description("")
                .id("6800ec14daf4e3153f26ef11")
                .image("https://ik.imagekit.io/bizup/converted/catalog/67ffd965cd59fffb60837e67/f5b90f09-d15c-4470-9bf8-6c90befd6184.png")
                .order(0)
                .isDeleted(false)
                .createdAt("2025-04-17T11:55:00.313Z")
                .updatedAt("2025-04-17T11:55:00.313Z")
                .version(0)
                .original("https://ik.imagekit.io/bizup/converted/catalog/67ffd965cd59fffb60837e67/f5b90f09-d15c-4470-9bf8-6c90befd6184.png")
                .thumbnail("https://ik.imagekit.io/bizup/converted/catalog/67ffd965cd59fffb60837e67/f5b90f09-d15c-4470-9bf8-6c90befd6184.png")
                .thumbnailHeight(100)
                .originalHeight(400)
                .build();

        // Create request body with all required fields
        CatalogEditRequest editRequest = CatalogEditRequest.builder()
                .productId(productId)
                .tags(java.util.Arrays.asList(
                        "677d26135725977833481898",
                        "677d26125725977833481897",
                        "677d26125725977833481896",
                        "67d7d8293a35944c3232364d",
                        "645cc307af2a39420d520cd2",
                        "645cc306af2a39420d51df55",
                        "677d261357259778334818a3",
                        "645cc308af2a39420d528300"
                ))
                .suggested(java.util.Collections.emptyList())
                .images(java.util.Collections.singletonList(image))
                .title(generatedTitle)
                .price(generatedPrice)
                .isQc(true)
                .isSet(false)
                .build();

        // Send POST request to edit catalog
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .body(editRequest)
                .when()
                .post(BombEndpoints.CATALOG + "/" + catalogId);

        // Log actual response for debugging
        logger.info("Actual API Response Body: {}", response.asString());
        logger.info("Response Status Code: {}", response.getStatusCode());

        // Parse response for other tests
        catalogEditResponse = JsonUtils.fromResponse(response, CatalogEditResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response is valid JSON", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Edit")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseIsValidJson() {
        // Verify response is valid JSON
        assertThat("Response should be valid JSON", catalogEditResponse, notNullValue());
        assertThat("Response content type should be JSON",
                response.getContentType(), containsString("application/json"));

        logger.info("Response is valid JSON");
    }

    @Test(description = "Response has correct structure", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Edit")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseCorrectStructure() {
        // Validate response has required properties
        assertThat("Response should have statusCode", catalogEditResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogEditResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogEditResponse.getData(), notNullValue());

        // Validate data has all required keys
        CatalogEditResponse.CatalogEditData data = catalogEditResponse.getData();
        assertThat("Data should have available", data.getAvailable(), notNullValue());
        assertThat("Data should have contentType", data.getContentType(), notNullValue());
        assertThat("Data should have isDeleted", data.getIsDeleted(), notNullValue());
        assertThat("Data should have _id", data.get_id(), notNullValue());
        assertThat("Data should have productTags", data.getProductTags(), notNullValue());
        assertThat("Data should have title", data.getTitle(), notNullValue());
        assertThat("Data should have priceText", data.getPriceText(), notNullValue());

        logger.info("Response structure validated successfully");
    }

    @Test(description = "Field data types are correct", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Edit")
    @Severity(SeverityLevel.CRITICAL)
    public void testFieldDataTypesCorrect() {
        CatalogEditResponse.CatalogEditData data = catalogEditResponse.getData();

        // Validate data types
        assertThat("Message should be a string", catalogEditResponse.getMessage(), instanceOf(String.class));
        assertThat("Available should be a boolean", data.getAvailable(), instanceOf(Boolean.class));
        assertThat("ContentType should be a string", data.getContentType(), instanceOf(String.class));
        assertThat("IsDeleted should be a boolean", data.getIsDeleted(), instanceOf(Boolean.class));
        assertThat("_id should be a string", data.get_id(), instanceOf(String.class));
        assertThat("PriceText should be a number", data.getPriceText(), instanceOf(Number.class));
        assertThat("Title should be a string", data.getTitle(), instanceOf(String.class));
        assertThat("ProductTags should be an array", data.getProductTags(), notNullValue());

        logger.info("Field data types validated successfully");
    }

    @Test(description = "Message is 'success'", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Edit")
    @Severity(SeverityLevel.CRITICAL)
    public void testMessageIsSuccess() {
        // Validate message is 'success'
        assertThat("Message should be 'success'",
                catalogEditResponse.getMessage(), equalTo("success"));

        logger.info("Message validated: success");
    }

    @Test(description = "Content type is 'image'", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Edit")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeIsImage() {
        // Validate content type is 'image'
        assertThat("Content type should be 'image'",
                catalogEditResponse.getData().getContentType(), equalTo("image"));

        logger.info("Content type validated: image");
    }

    @Test(description = "isDeleted flag should be false", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Edit")
    @Severity(SeverityLevel.NORMAL)
    public void testIsDeletedFlagFalse() {
        // Validate isDeleted is false
        assertThat("isDeleted should be false",
                catalogEditResponse.getData().getIsDeleted(), is(false));

        logger.info("isDeleted flag validated: false");
    }

    @Test(description = "Price matches expected value", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Edit")
    @Severity(SeverityLevel.NORMAL)
    public void testPriceMatchesExpected() {
        // Validate price matches the generated value from the edit request
        assertThat("Price should match expected value",
                catalogEditResponse.getData().getPriceText().intValue(), equalTo(generatedPrice));

        logger.info("Price validated: {} (Expected: {})", catalogEditResponse.getData().getPriceText(), generatedPrice);
    }

    @Test(description = "Title matches expected value", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Edit")
    @Severity(SeverityLevel.NORMAL)
    public void testTitleMatchesExpected() {
        // Validate title matches the generated value from the edit request
        assertThat("Title should match expected value",
                catalogEditResponse.getData().getTitle(), equalTo(generatedTitle));

        logger.info("Title validated: {} (Expected: {})", catalogEditResponse.getData().getTitle(), generatedTitle);
    }

    @Test(description = "Product ID matches expected value", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Edit")
    @Severity(SeverityLevel.NORMAL)
    public void testProductIdMatchesExpected() {
        // Validate that the product ID sent in request matches the first element in productTags
        assertThat("Product tags should not be empty",
                catalogEditResponse.getData().getProductTags(), not(empty()));
        assertThat("First product tag should match the product_id from request",
                catalogEditResponse.getData().getProductTags().get(0), equalTo(productId));

        logger.info("Product ID validated: {} (Expected: {})", 
                catalogEditResponse.getData().getProductTags().get(0), productId);
    }

    @Test(description = "Catalog ID matches expected value", priority = 11, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Edit")
    @Severity(SeverityLevel.CRITICAL)
    public void testCatalogIdMatchesExpected() {
        // Validate catalog ID matches the request parameter
        assertThat("Catalog ID should match request parameter",
                catalogEditResponse.getData().get_id(), equalTo(catalogId));

        logger.info("Catalog ID validated: {}", catalogEditResponse.getData().get_id());
    }

    @Test(description = "Response time is less than threshold", priority = 12, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Edit")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Get threshold from config or use default 40000ms
        long threshold = config.responseTimeThreshold();
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    /**
     * Generate realistic test title (similar to Postman pre-request script)
     */
    private String generateTestTitle() {
        String[] prefixes = { "Premium", "Standard", "Casual", "Cotton", "Best Fabric", "High Quality", "Ultimate" };
        String[] products = { "Saree", "Jeans", "Pant", "Shirt", "T-Shirt", "Lower" };
        String[] suffixes = { "S", "M", "L", "XL", "XXL", "for Men", "for Women" };

        String randomPrefix = prefixes[(int) (Math.random() * prefixes.length)];
        String randomProduct = products[(int) (Math.random() * products.length)];
        String randomSuffix = suffixes[(int) (Math.random() * suffixes.length)];

        // 70% chance to include prefix, 50% chance to include suffix
        StringBuilder titleBuilder = new StringBuilder();
        if (Math.random() < 0.7) {
            titleBuilder.append(randomPrefix).append(" ");
        }
        titleBuilder.append(randomProduct);
        if (Math.random() < 0.5) {
            titleBuilder.append(" ").append(randomSuffix);
        }

        return titleBuilder.toString();
    }

    /**
     * Generate random price between 100-999 as round number (similar to Postman
     * pre-request script)
     */
    private Integer generateTestPrice() {
        return (int) (Math.random() * 900) + 100;
    }
}
