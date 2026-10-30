package com.automation.tests.buyerapp.newapis;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.utils.VariableManager;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.automation.utils.ApiErrorAssertions.assertApiError;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Sub-seller session and stock routes — /v1/subseller/*.
 * PRODUCTION-safe: only auth/validation rejections; no sub-seller credentials,
 * no stock writes (the POSTs carry no token and are refused by
 * subSellerAuthentication before the handler).
 */
@Epic("Buyer App – New APIs")
@Feature("Sub-seller Auth")
public class SubSellerAuthTest extends BaseTest {

    private static final String PLACEHOLDER_CATALOG_ID = "000000000000000000000000";

    private String baseUrl;
    private String buyerToken;

    @BeforeClass
    public void setup() {
        baseUrl = config.buyerAppBaseUrl();
        buyerToken = VariableManager.getBuyerAppToken();
    }

    private void requireBuyerToken() {
        if (buyerToken == null || buyerToken.isEmpty()) {
            throw new SkipException("Buyer App token not available. Run LoginTest first.");
        }
    }

    @Test(description = "TC-SUBSELLER-002 — refresh/token with refreshToken missing", groups = "buyerapp")
    @TmsLink("TC-SUBSELLER-002") @Story("Refresh token") @Severity(SeverityLevel.NORMAL)
    public void refreshTokenMissing() {
        Response resp = RestAssured.given().baseUri(baseUrl).contentType("application/json")
                .body("{}").when().post(BuyerAppEndpoints.SUBSELLER_REFRESH_TOKEN);
        assertApiError(resp, HttpStatus.BAD_REQUEST, "refreshToken");
    }

    @Test(description = "TC-SUBSELLER-003 — refresh/token with whitespace refreshToken", groups = "buyerapp")
    @TmsLink("TC-SUBSELLER-003") @Story("Refresh token") @Severity(SeverityLevel.NORMAL)
    public void refreshTokenWhitespace() {
        Response resp = RestAssured.given().baseUri(baseUrl).contentType("application/json")
                .body("{\"refreshToken\":\"   \"}").when().post(BuyerAppEndpoints.SUBSELLER_REFRESH_TOKEN);
        assertApiError(resp, HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-SUBSELLER-004 — refresh/token with an invalid refresh token", groups = "buyerapp")
    @TmsLink("TC-SUBSELLER-004") @Story("Refresh token") @Severity(SeverityLevel.CRITICAL)
    public void refreshTokenInvalid() {
        Response resp = RestAssured.given().baseUri(baseUrl).contentType("application/json")
                .body("{\"refreshToken\":\"not-a-real-refresh-token\"}")
                .when().post(BuyerAppEndpoints.SUBSELLER_REFRESH_TOKEN);
        assertThat("rejected", resp.getStatusCode(), allOf(greaterThanOrEqualTo(400), lessThan(500)));
        assertThat("no access token issued", resp.jsonPath().getString("data.accessToken"), nullValue());
    }

    @Test(description = "TC-SUBSELLER-007 — GET /me with no Authorization header", groups = "buyerapp")
    @TmsLink("TC-SUBSELLER-007") @Story("Sub-seller auth") @Severity(SeverityLevel.CRITICAL)
    public void meWithoutToken() {
        Response resp = RestAssured.given().baseUri(baseUrl).when().get(BuyerAppEndpoints.SUBSELLER_ME);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }

    @Test(description = "TC-SUBSELLER-009 — GET /me with a buyer JWT", groups = "buyerapp")
    @TmsLink("TC-SUBSELLER-009") @Story("Sub-seller auth") @Severity(SeverityLevel.CRITICAL)
    public void meWithBuyerToken() {
        requireBuyerToken();
        Response resp = RestAssured.given().baseUri(baseUrl)
                .header("Authorization", "JWT " + buyerToken)
                .when().get(BuyerAppEndpoints.SUBSELLER_ME);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }

    @Test(description = "TC-SUBSELLER-014 — GET /catalog with no token", groups = "buyerapp")
    @TmsLink("TC-SUBSELLER-014") @Story("Sub-seller catalog") @Severity(SeverityLevel.NORMAL)
    public void catalogWithoutToken() {
        Response resp = RestAssured.given().baseUri(baseUrl).when().get(BuyerAppEndpoints.SUBSELLER_CATALOG);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }

    @Test(description = "TC-SUBSELLER-022 — POST /catalog/:id with no token", groups = "buyerapp")
    @TmsLink("TC-SUBSELLER-022") @Story("Sub-seller stock") @Severity(SeverityLevel.CRITICAL)
    public void updateStockWithoutToken() {
        Response resp = RestAssured.given().baseUri(baseUrl).contentType("application/json")
                .pathParam("id", PLACEHOLDER_CATALOG_ID).body("{}")
                .when().post(BuyerAppEndpoints.SUBSELLER_CATALOG_BY_ID);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }

    @Test(description = "TC-SUBSELLER-025 — POST /catalog/:id/confirm-stock with no token", groups = "buyerapp")
    @TmsLink("TC-SUBSELLER-025") @Story("Sub-seller stock") @Severity(SeverityLevel.CRITICAL)
    public void confirmStockWithoutToken() {
        Response resp = RestAssured.given().baseUri(baseUrl).contentType("application/json")
                .pathParam("id", PLACEHOLDER_CATALOG_ID).body("{}")
                .when().post(BuyerAppEndpoints.SUBSELLER_CATALOG_CONFIRM_STOCK);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }
}
