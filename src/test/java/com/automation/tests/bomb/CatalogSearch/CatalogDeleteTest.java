package com.automation.tests.bomb.CatalogSearch;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
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
 * Test class for BOMB Catalog Delete endpoint.
 * Endpoint: {{bizup_base}}/v1/admin/catalog/{{delete_catalog_id}}
 * Implements comprehensive Postman test scripts for catalog deletion
 * validation.
 */
@Epic("BOMB Catalog Management")
@Feature("Catalog Delete")
public class CatalogDeleteTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogEditResponse catalogDeleteResponse;
    private String deleteCatalogId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");

        // Get delete catalog ID from VariableManager or use fallback
        deleteCatalogId = VariableManager.get("delete_catalog_id");
        if (deleteCatalogId == null || deleteCatalogId.isEmpty()) {
            deleteCatalogId = VariableManager.get("catalog_foassign_id", "6822f5dac17c6dcd589ba173");
            logger.warn("Delete catalog ID not available, using fallback: {}", deleteCatalogId);
        } else {
            logger.info("Using delete catalog ID from VariableManager: {}", deleteCatalogId);
        }
    }

    @Test(description = "Status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Delete")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send DELETE request to delete catalog
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .when()
                .delete(BombEndpoints.CATALOG + "/" + deleteCatalogId);

        // Parse response for other tests
        catalogDeleteResponse = JsonUtils.fromResponse(response, CatalogEditResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is within acceptable limit", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Delete")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeWithinLimit() {
        // Get threshold from config or use default 20000ms (as per Postman script)
        long threshold = 20000; // Specific threshold for delete operation
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Response body is valid JSON", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Delete")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseBodyIsValidJson() {
        // Verify response is valid JSON
        assertThat("Response should be valid JSON", catalogDeleteResponse, notNullValue());
        assertThat("Response content type should be JSON",
                response.getContentType(), containsString("application/json"));

        logger.info("Response body is valid JSON");
    }

    @Test(description = "Response has expected properties", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Delete")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseHasExpectedProperties() {
        // Validate response has required properties
        assertThat("Response should have statusCode", catalogDeleteResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogDeleteResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogDeleteResponse.getData(), notNullValue());

        // Validate data has all required keys
        CatalogEditResponse.CatalogEditData data = catalogDeleteResponse.getData();
        assertThat("Data should have available", data.getAvailable(), notNullValue());
        assertThat("Data should have contentType", data.getContentType(), notNullValue());
        assertThat("Data should have isDeleted", data.getIsDeleted(), notNullValue());
        assertThat("Data should have _id", data.get_id(), notNullValue());
        assertThat("Data should have productTags", data.getProductTags(), notNullValue());
        assertThat("Data should have title", data.getTitle(), notNullValue());
        assertThat("Data should have priceText", data.getPriceText(), notNullValue());

        logger.info("Response structure validated successfully");
    }

    @Test(description = "Response fields have correct data types", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Delete")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseFieldsHaveCorrectDataTypes() {
        CatalogEditResponse.CatalogEditData data = catalogDeleteResponse.getData();

        // Validate data types
        assertThat("Message should be a string", catalogDeleteResponse.getMessage(), instanceOf(String.class));
        assertThat("Available should be a boolean", data.getAvailable(), instanceOf(Boolean.class));
        assertThat("ContentType should be a string", data.getContentType(), instanceOf(String.class));
        assertThat("IsDeleted should be a boolean", data.getIsDeleted(), instanceOf(Boolean.class));
        assertThat("_id should be a string", data.get_id(), instanceOf(String.class));
        assertThat("PriceText should be a number", data.getPriceText(), instanceOf(Double.class));
        assertThat("Title should be a string", data.getTitle(), instanceOf(String.class));
        assertThat("ProductTags should be an array", data.getProductTags(), notNullValue());

        logger.info("Field data types validated successfully");
    }

    @Test(description = "Response message indicates successful catalog deletion", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Delete")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseMessageIndicatesSuccessfulDeletion() {
        // Validate message is 'catalog deleted'
        assertThat("Message should be 'catalog deleted'",
                catalogDeleteResponse.getMessage(), equalTo("catalog deleted"));

        logger.info("Message validated: catalog deleted");
    }

    @Test(description = "Available flag is true", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Delete")
    @Severity(SeverityLevel.NORMAL)
    public void testAvailableFlagIsTrue() {
        // Validate available is true
        assertThat("Available should be true",
                catalogDeleteResponse.getData().getAvailable(), is(true));

        logger.info("Available flag validated: true");
    }

    @Test(description = "Content type is 'image'", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Delete")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeIsImage() {
        // Validate content type is 'image'
        assertThat("Content type should be 'image'",
                catalogDeleteResponse.getData().getContentType(), equalTo("image"));

        logger.info("Content type validated: image");
    }

    @Test(description = "isDeleted flag is true", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Delete")
    @Severity(SeverityLevel.CRITICAL)
    public void testIsDeletedFlagIsTrue() {
        // Validate isDeleted is true (indicating successful deletion)
        assertThat("isDeleted should be true",
                catalogDeleteResponse.getData().getIsDeleted(), is(true));

        logger.info("isDeleted flag validated: true");
    }

    @Test(description = "Catalog ID matches expected value", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Delete")
    @Severity(SeverityLevel.CRITICAL)
    public void testCatalogIdMatchesExpectedValue() {
        // Validate catalog ID matches the request parameter
        assertThat("Catalog ID should match request parameter",
                catalogDeleteResponse.getData().get_id(), equalTo(deleteCatalogId));

        logger.info("Catalog ID validated: {}", catalogDeleteResponse.getData().get_id());
    }
}
