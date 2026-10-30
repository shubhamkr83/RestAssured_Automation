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
 * Referrals — /v1/referral. PRODUCTION-safe: every POST here is refused by
 * createReferral before ReferralModel.create (missing/invalid phone, self
 * referral). No valid new referral is ever sent.
 */
@Epic("Buyer App – New APIs")
@Feature("Referral")
public class ReferralValidationTest extends BaseTest {

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

    private Response refer(String body) {
        return RestAssured.given().baseUri(baseUrl).contentType("application/json")
                .header("Authorization", "JWT " + buyerToken).body(body)
                .when().post(BuyerAppEndpoints.REFERRAL);
    }

    @Test(description = "TC-REFERRAL-003 — POST /v1/referral with no token", groups = "buyerapp")
    @TmsLink("TC-REFERRAL-003") @Story("Create referral") @Severity(SeverityLevel.CRITICAL)
    public void createWithoutToken() {
        Response resp = RestAssured.given().baseUri(baseUrl).contentType("application/json")
                .body("{\"referredPhone\":\"+919876543210\"}").when().post(BuyerAppEndpoints.REFERRAL);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }

    @Test(description = "TC-REFERRAL-004 — referredPhone missing", groups = "buyerapp")
    @TmsLink("TC-REFERRAL-004") @Story("Create referral") @Severity(SeverityLevel.NORMAL)
    public void phoneMissing() {
        requireBuyerToken();
        assertApiError(refer("{}"), HttpStatus.BAD_REQUEST, "referredPhone is required");
    }

    @Test(description = "TC-REFERRAL-005 — phone without +91 prefix", groups = "buyerapp")
    @TmsLink("TC-REFERRAL-005") @Story("Create referral") @Severity(SeverityLevel.NORMAL)
    public void phoneWithoutPrefix() {
        requireBuyerToken();
        assertApiError(refer("{\"referredPhone\":\"9876543210\"}"), HttpStatus.BAD_REQUEST, "+91 prefix");
    }

    @Test(description = "TC-REFERRAL-006 — first digit not 1 or 6-9", groups = "buyerapp")
    @TmsLink("TC-REFERRAL-006") @Story("Create referral") @Severity(SeverityLevel.NORMAL)
    public void phoneInvalidFirstDigit() {
        requireBuyerToken();
        assertApiError(refer("{\"referredPhone\":\"+912876543210\"}"), HttpStatus.BAD_REQUEST, "valid Indian phone");
    }

    @Test(description = "TC-REFERRAL-007 — too short / too long number", groups = "buyerapp")
    @TmsLink("TC-REFERRAL-007") @Story("Create referral") @Severity(SeverityLevel.NORMAL)
    public void phoneWrongLength() {
        requireBuyerToken();
        assertApiError(refer("{\"referredPhone\":\"+91987654321\"}"), HttpStatus.BAD_REQUEST, "valid Indian phone");
        assertApiError(refer("{\"referredPhone\":\"+9198765432100\"}"), HttpStatus.BAD_REQUEST, "valid Indian phone");
    }

    @Test(description = "TC-REFERRAL-008 — refer own number", groups = "buyerapp")
    @TmsLink("TC-REFERRAL-008") @Story("Create referral") @Severity(SeverityLevel.NORMAL)
    public void referSelf() {
        requireBuyerToken();
        String ownPhone = config.buyerAppPhoneNumber();
        if (ownPhone == null || !ownPhone.matches("^\\+91[16-9][0-9]{9}$")) {
            throw new SkipException("buyer.app.phone.number is not a +91 mobile; cannot build a self-referral.");
        }
        // If the account's stored phone differs from config, the handler still refuses
        // before create ("User already exists"), so this request never writes.
        Response resp = refer("{\"referredPhone\":\"" + ownPhone + "\"}");
        assertApiError(resp, HttpStatus.BAD_REQUEST, "Cannot refer yourself");
    }

    @Test(description = "TC-REFERRAL-013 — GET /v1/referral lists own referrals", groups = "buyerapp")
    @TmsLink("TC-REFERRAL-013") @Story("List referrals") @Severity(SeverityLevel.CRITICAL)
    public void listOwnReferrals() {
        requireBuyerToken();
        Response resp = RestAssured.given().baseUri(baseUrl)
                .header("Authorization", "JWT " + buyerToken).when().get(BuyerAppEndpoints.REFERRAL);
        assertThat("status", resp.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat("referrals array", resp.jsonPath().getList("data.referrals"), notNullValue());
        assertThat("total", resp.jsonPath().get("data.total"), notNullValue());
    }

    @Test(description = "TC-REFERRAL-015 — GET /v1/referral with no token", groups = "buyerapp")
    @TmsLink("TC-REFERRAL-015") @Story("List referrals") @Severity(SeverityLevel.NORMAL)
    public void listWithoutToken() {
        Response resp = RestAssured.given().baseUri(baseUrl).when().get(BuyerAppEndpoints.REFERRAL);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }
}
