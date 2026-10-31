package com.automation.tests.buyerapp.newapis;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.utils.ApiErrorAssertions;
import com.automation.utils.VariableManager;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/** GET /v1/banner/{home,collection,pdp,search} - read-only; optional auth. */
@Epic("Buyer App – New APIs")
@Feature("Banner placements")
public class BannerPlacementTest extends BaseTest {

    private String baseUrl;
    private String token;
    private static final String CAT = "saree";

    @BeforeClass(alwaysRun = true)
    public void setup() {
        baseUrl = config.buyerAppBaseUrl();
        token = buyerAppTokenOrNull();
    }

    private RequestSpecification guest() {
        return buyerAppGuestSpec();
    }

    private RequestSpecification authed() {
        if (token == null || token.isEmpty()) {
            throw new SkipException("Buyer App token not available (run LoginTest first)");
        }
        return guest().header("Authorization", "JWT " + token);
    }

    private Response get(RequestSpecification spec, String placement, String query) {
        return spec.when().get(BuyerAppEndpoints.BANNER_PLACEMENT.replace("{placement}", placement) + query);
    }

    private void assertOkList(Response r) {
        assertThat(r.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(r.jsonPath().getString("statusCode"), equalTo("10000"));
        assertThat("result list", r.jsonPath().getList("data.result"), notNullValue());
    }

    @Test(description = "TC-BANNER-001 — GET /v1/banner/home - authenticated buyer", groups = "buyerapp")
    @TmsLink("TC-BANNER-001") @Story("Banner placements") @Severity(SeverityLevel.CRITICAL)
    public void homeAuthenticated() {
        assertOkList(get(authed(), "home", "?suitable_for=" + CAT));
    }

    @Test(description = "TC-BANNER-002 — GET /v1/banner/home - guest (no token)", groups = "buyerapp")
    @TmsLink("TC-BANNER-002") @Story("Banner placements") @Severity(SeverityLevel.CRITICAL)
    public void homeGuest() {
        assertOkList(get(guest(), "home", "?suitable_for=" + CAT));
    }

    @Test(description = "TC-BANNER-004 — GET /v1/banner/collection - guest", groups = "buyerapp")
    @TmsLink("TC-BANNER-004") @Story("Banner placements") @Severity(SeverityLevel.CRITICAL)
    public void collectionGuest() {
        assertOkList(get(guest(), "collection", "?suitable_for=" + CAT));
    }

    @Test(description = "TC-BANNER-005 — GET /v1/banner/pdp - guest and user", groups = "buyerapp")
    @TmsLink("TC-BANNER-005") @Story("Banner placements") @Severity(SeverityLevel.NORMAL)
    public void pdpGuestAndUser() {
        assertOkList(get(guest(), "pdp", "?suitable_for=" + CAT));
        assertOkList(get(authed(), "pdp", "?suitable_for=" + CAT));
    }

    @Test(description = "TC-BANNER-006 — GET /v1/banner/search - guest and user", groups = "buyerapp")
    @TmsLink("TC-BANNER-006") @Story("Banner placements") @Severity(SeverityLevel.NORMAL)
    public void searchGuestAndUser() {
        assertOkList(get(guest(), "search", "?suitable_for=" + CAT));
        assertOkList(get(authed(), "search", "?suitable_for=" + CAT));
    }

    @Test(description = "TC-BANNER-007 — GET /v1/banner/home - pagination", groups = "buyerapp")
    @TmsLink("TC-BANNER-007") @Story("Banner placements") @Severity(SeverityLevel.NORMAL)
    public void homePagination() {
        Response r = get(guest(), "home", "?suitable_for=" + CAT + "&page=0&pageSize=5");
        assertOkList(r);
        List<Object> items = r.jsonPath().getList("data.result");
        assertThat(items.size(), lessThanOrEqualTo(5));
        assertThat(r.jsonPath().getInt("data.page"), equalTo(0));
        assertThat(r.jsonPath().getInt("data.pageSize"), equalTo(5));
    }

    @Test(description = "TC-BANNER-008 — GET /v1/banner/home - suitable_for missing", groups = "buyerapp")
    @TmsLink("TC-BANNER-008") @Story("Banner validation") @Severity(SeverityLevel.CRITICAL)
    public void suitableForMissing() {
        ApiErrorAssertions.assertApiError(get(guest(), "home", ""), HttpStatus.BAD_REQUEST, "suitable_for");
    }

    @Test(description = "TC-BANNER-009 — GET /v1/banner/home - suitable_for not in BANNER_CATEGORIES", groups = "buyerapp")
    @TmsLink("TC-BANNER-009") @Story("Banner validation") @Severity(SeverityLevel.NORMAL)
    public void suitableForBogus() {
        ApiErrorAssertions.assertApiError(get(guest(), "home", "?suitable_for=bogus"), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-BANNER-010 — GET /v1/banner/home - pageSize above 50", groups = "buyerapp")
    @TmsLink("TC-BANNER-010") @Story("Banner validation") @Severity(SeverityLevel.NORMAL)
    public void pageSizeAbove50() {
        ApiErrorAssertions.assertApiError(get(guest(), "home", "?suitable_for=" + CAT + "&pageSize=51"), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-BANNER-011 — GET /v1/banner/home - pageSize 0", groups = "buyerapp")
    @TmsLink("TC-BANNER-011") @Story("Banner validation") @Severity(SeverityLevel.MINOR)
    public void pageSizeZero() {
        ApiErrorAssertions.assertApiError(get(guest(), "home", "?suitable_for=" + CAT + "&pageSize=0"), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-BANNER-012 — GET /v1/banner/home - negative page", groups = "buyerapp")
    @TmsLink("TC-BANNER-012") @Story("Banner validation") @Severity(SeverityLevel.MINOR)
    public void negativePage() {
        ApiErrorAssertions.assertApiError(get(guest(), "home", "?suitable_for=" + CAT + "&page=-1"), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-BANNER-013 — GET /v1/banner/home - non-integer page", groups = "buyerapp")
    @TmsLink("TC-BANNER-013") @Story("Banner validation") @Severity(SeverityLevel.MINOR)
    public void nonIntegerPage() {
        ApiErrorAssertions.assertApiError(get(guest(), "home", "?suitable_for=" + CAT + "&page=1.5"), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-BANNER-014 — GET /v1/banner/home - unknown query param", groups = "buyerapp")
    @TmsLink("TC-BANNER-014") @Story("Banner validation") @Severity(SeverityLevel.MINOR)
    public void unknownQueryParam() {
        ApiErrorAssertions.assertApiError(get(guest(), "home", "?suitable_for=" + CAT + "&foo=bar"), HttpStatus.BAD_REQUEST);
    }
}
