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

/** GET /v1/navo-live and /events/:eventId/products - read-only; optional auth. */
@Epic("Buyer App – New APIs")
@Feature("Navo Live public")
public class NavoLiveTest extends BaseTest {

    private static final int MAX_LIMIT = 40;
    private static final String ZERO_ID = "000000000000000000000000";
    private String baseUrl;
    private String token;
    private String eventId;

    @BeforeClass(alwaysRun = true)
    public void setup() {
        baseUrl = config.buyerAppBaseUrl();
        token = buyerAppTokenOrNull();
        Response r = spec().when().get(BuyerAppEndpoints.NAVO_LIVE);
        if (r.getStatusCode() == HttpStatus.OK) {
            List<String> ids = r.jsonPath().getList("data.events._id");
            if (ids != null && !ids.isEmpty()) {
                eventId = ids.get(0);
            }
        }
    }

    private RequestSpecification spec() {
        return buyerAppGuestSpec();
    }

    private String requireEvent() {
        if (eventId == null) {
            throw new SkipException("No Navo Live event listed on this environment");
        }
        return eventId;
    }

    private Response products(String id, String query) {
        return spec().when().get(BuyerAppEndpoints.NAVO_LIVE_EVENT_PRODUCTS.replace("{eventId}", id) + query);
    }

    private void assertPage(Response r) {
        assertThat(r.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(r.jsonPath().getString("statusCode"), equalTo("10000"));
        assertThat(r.jsonPath().getString("data.serverTime"), not(emptyOrNullString()));
        assertThat(r.jsonPath().getList("data.events"), notNullValue());
    }

    @Test(description = "TC-NAVOLIVE-001 — GET /v1/navo-live - guest, default productLimit", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-001") @Story("Navo Live page") @Severity(SeverityLevel.CRITICAL)
    public void guestDefault() {
        assertPage(spec().when().get(BuyerAppEndpoints.NAVO_LIVE));
    }

    @Test(description = "TC-NAVOLIVE-002 — GET /v1/navo-live - authenticated buyer", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-002") @Story("Navo Live page") @Severity(SeverityLevel.NORMAL)
    public void authenticated() {
        if (token == null || token.isEmpty()) {
            throw new SkipException("Buyer App token not available (run LoginTest first)");
        }
        assertPage(spec().header("Authorization", "JWT " + token).when().get(BuyerAppEndpoints.NAVO_LIVE));
    }

    @Test(description = "TC-NAVOLIVE-003 — GET /v1/navo-live?productLimit=5", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-003") @Story("Navo Live page") @Severity(SeverityLevel.NORMAL)
    public void productLimit5() {
        Response r = spec().queryParam("productLimit", 5).when().get(BuyerAppEndpoints.NAVO_LIVE);
        assertPage(r);
        List<List<Object>> rails = r.jsonPath().getList("data.events.products");
        for (List<Object> rail : rails) {
            if (rail != null) {
                assertThat(rail.size(), lessThanOrEqualTo(5));
            }
        }
    }

    @Test(description = "TC-NAVOLIVE-004 — GET /v1/navo-live - unknown query params accepted", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-004") @Story("Navo Live page") @Severity(SeverityLevel.NORMAL)
    public void unknownParams() {
        assertPage(spec().queryParam("utm_source", "x").when().get(BuyerAppEndpoints.NAVO_LIVE));
    }

    @Test(description = "TC-NAVOLIVE-005 — GET /v1/navo-live - productLimit 0", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-005") @Story("Navo Live validation") @Severity(SeverityLevel.NORMAL)
    public void productLimitZero() {
        ApiErrorAssertions.assertApiError(spec().queryParam("productLimit", 0).when().get(BuyerAppEndpoints.NAVO_LIVE), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-NAVOLIVE-006 — GET /v1/navo-live - productLimit above max", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-006") @Story("Navo Live validation") @Severity(SeverityLevel.NORMAL)
    public void productLimitAboveMax() {
        ApiErrorAssertions.assertApiError(spec().queryParam("productLimit", MAX_LIMIT + 1).when().get(BuyerAppEndpoints.NAVO_LIVE), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-NAVOLIVE-007 — GET /v1/navo-live - non-integer productLimit", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-007") @Story("Navo Live validation") @Severity(SeverityLevel.NORMAL)
    public void productLimitNonInteger() {
        ApiErrorAssertions.assertApiError(spec().queryParam("productLimit", "abc").when().get(BuyerAppEndpoints.NAVO_LIVE), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-NAVOLIVE-011 — GET /v1/navo-live/events/:eventId/products - first page", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-011") @Story("Navo Live products") @Severity(SeverityLevel.CRITICAL)
    public void productsFirstPage() {
        Response r = products(requireEvent(), "");
        if (r.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new SkipException("Discovered event has no collection (404): " + r.jsonPath().getString("message"));
        }
        assertThat(r.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(r.jsonPath().getInt("data.page"), equalTo(1));
        assertThat(r.jsonPath().getList("data.items"), notNullValue());
    }

    @Test(description = "TC-NAVOLIVE-012 — GET /v1/navo-live/events/:eventId/products?page=2&limit=10", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-012") @Story("Navo Live products") @Severity(SeverityLevel.NORMAL)
    public void productsSecondPage() {
        String id = requireEvent();
        Response p1 = products(id, "?page=1&limit=10");
        if (p1.getStatusCode() != HttpStatus.OK || p1.jsonPath().getInt("data.total") <= 10) {
            throw new SkipException("Event has 10 or fewer products (or no collection); page 2 not meaningful");
        }
        Response p2 = products(id, "?page=2&limit=10");
        assertThat(p2.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(p2.jsonPath().getInt("data.page"), equalTo(2));
        List<Object> ids1 = p1.jsonPath().getList("data.items._id");
        List<Object> ids2 = p2.jsonPath().getList("data.items._id");
        assertThat(ids2, not(empty()));
        for (Object i : ids2) {
            assertThat("no overlap with page 1", ids1, not(hasItem(i)));
        }
    }

    @Test(description = "TC-NAVOLIVE-013 — GET /v1/navo-live/events/:eventId/products - malformed eventId", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-013") @Story("Navo Live validation") @Severity(SeverityLevel.NORMAL)
    public void malformedEventId() {
        ApiErrorAssertions.assertApiError(products("abc", ""), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-NAVOLIVE-014 — GET /v1/navo-live/events/:eventId/products - unknown eventId", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-014") @Story("Navo Live products") @Severity(SeverityLevel.NORMAL)
    public void unknownEventId() {
        ApiErrorAssertions.assertApiError(products(ZERO_ID, ""), HttpStatus.NOT_FOUND, "Navo Live event not found");
    }

    @Test(description = "TC-NAVOLIVE-015 — GET /v1/navo-live/events/:eventId/products - page 0", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-015") @Story("Navo Live validation") @Severity(SeverityLevel.NORMAL)
    public void pageZero() {
        ApiErrorAssertions.assertApiError(products(ZERO_ID, "?page=0"), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-NAVOLIVE-016 — GET /v1/navo-live/events/:eventId/products - limit above max", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-016") @Story("Navo Live validation") @Severity(SeverityLevel.NORMAL)
    public void limitAboveMax() {
        ApiErrorAssertions.assertApiError(products(ZERO_ID, "?limit=" + (MAX_LIMIT + 1)), HttpStatus.BAD_REQUEST);
    }
}
