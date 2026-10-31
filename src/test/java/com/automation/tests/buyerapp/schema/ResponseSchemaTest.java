package com.automation.tests.buyerapp.schema;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.utils.TestDataResolver;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Contract checks: each response must match a JSON schema under
 * src/test/resources/schemas/buyerapp/. The schemas pin the envelope and the
 * fields the app relies on (names and types) and allow extra fields, so a
 * renamed, removed or retyped field fails here even when no per-field
 * assertion covers it. Read-only GETs.
 */
@Epic("Buyer App Contracts")
@Feature("Response Schemas")
public class ResponseSchemaTest extends BaseTest {

    private static final String SCHEMAS = "schemas/buyerapp/";

    @Test(description = "GET /v1/collection/all matches its schema", groups = "buyerapp")
    @Story("Collections") @Severity(SeverityLevel.CRITICAL)
    public void collectionAllMatchesSchema() {
        Response resp = buyerAppSpec().queryParam("suitable_for", "saree").get(BuyerAppEndpoints.COLLECTION_ALL);
        assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(resp.asString(), matchesJsonSchemaInClasspath(SCHEMAS + "collection-all.json"));
    }

    @Test(description = "GET /v1/feed/banners matches its schema", groups = "buyerapp")
    @Story("Banners") @Severity(SeverityLevel.CRITICAL)
    public void feedBannersMatchesSchema() {
        Response resp = buyerAppSpec().queryParam("suitable_for", "saree").get(BuyerAppEndpoints.FEED_BANNERS);
        assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(resp.asString(), matchesJsonSchemaInClasspath(SCHEMAS + "feed-banners.json"));
    }

    @Test(description = "GET /v1/catalog/{id} matches its schema", groups = "buyerapp")
    @Story("Catalog") @Severity(SeverityLevel.CRITICAL)
    public void catalogByIdMatchesSchema() {
        Response resp = buyerAppSpec()
                .pathParam("catalogId", TestDataResolver.liveCatalogId(BaseTest::buyerAppSpec))
                .get("/v1/catalog/{catalogId}");
        assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(resp.asString(), matchesJsonSchemaInClasspath(SCHEMAS + "catalog-by-id.json"));
    }
}
