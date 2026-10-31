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

import java.util.HashSet;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/** GET /v1/mlp* public reads, plus the staff-gated retired admin GET (403 before any handler). */
@Epic("Buyer App – New APIs")
@Feature("MLP public read")
public class MlpPublicReadTest extends BaseTest {

    private String baseUrl;
    private String token;
    /** An occasion that returned 200 on /v1/mlp/{occasion}; null if none has active structures. */
    private String liveOccasion;

    @BeforeClass(alwaysRun = true)
    public void setup() {
        baseUrl = config.buyerAppBaseUrl();
        token = buyerAppTokenOrNull();
        Response r = buyerAppGuestSpec().when().get(BuyerAppEndpoints.MLP_OCCASIONS);
        if (r.getStatusCode() == HttpStatus.OK) {
            List<String> occasions = r.jsonPath().getList("data.occasions");
            if (occasions != null) {
                for (String o : occasions) {
                    Response p = buyerAppGuestSpec().pathParam("occasion", o)
                            .when().get(BuyerAppEndpoints.MLP_BY_OCCASION);
                    if (p.getStatusCode() == HttpStatus.OK) {
                        liveOccasion = o;
                        break;
                    }
                }
            }
        }
    }

    private RequestSpecification spec() {
        return buyerAppGuestSpec();
    }

    private String requireLive() {
        if (liveOccasion == null) {
            throw new SkipException("No MLP occasion with active structures found on this environment");
        }
        return liveOccasion;
    }

    private void assertStructures(Response r, String occasion) {
        assertThat(r.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(r.jsonPath().getString("statusCode"), equalTo("10000"));
        assertThat(r.jsonPath().getString("data.occasion"), equalToIgnoringCase(occasion));
        assertThat(r.jsonPath().getList("data.structures"), not(empty()));
    }

    @Test(description = "TC-MLP-001 — GET /v1/mlp/:occasion - valid occasion, no token", groups = "buyerapp")
    @TmsLink("TC-MLP-001") @Story("MLP public read") @Severity(SeverityLevel.CRITICAL)
    public void byOccasionPath() {
        String o = requireLive();
        assertStructures(spec().pathParam("occasion", o).when().get(BuyerAppEndpoints.MLP_BY_OCCASION), o);
    }

    @Test(description = "TC-MLP-002 — GET /v1/mlp?occasion= - query form matches path form", groups = "buyerapp")
    @TmsLink("TC-MLP-002") @Story("MLP public read") @Severity(SeverityLevel.CRITICAL)
    public void byOccasionQuery() {
        String o = requireLive();
        Response q = spec().queryParam("occasion", o).when().get(BuyerAppEndpoints.MLP);
        assertStructures(q, o);
        Response p = spec().pathParam("occasion", o).when().get(BuyerAppEndpoints.MLP_BY_OCCASION);
        assertThat(q.jsonPath().getList("data.structures").size(),
                equalTo(p.jsonPath().getList("data.structures").size()));
    }

    @Test(description = "TC-MLP-003 — GET /v1/mlp/:occasion - utm params do not 400", groups = "buyerapp")
    @TmsLink("TC-MLP-003") @Story("MLP public read") @Severity(SeverityLevel.NORMAL)
    public void pathWithUtm() {
        String o = requireLive();
        assertStructures(spec().pathParam("occasion", o)
                .queryParam("utm_source", "wa").queryParam("utm_medium", "x").queryParam("utm_campaign", "y")
                .when().get(BuyerAppEndpoints.MLP_BY_OCCASION), o);
    }

    @Test(description = "TC-MLP-004 — GET /v1/mlp?occasion= - utm params do not 400", groups = "buyerapp")
    @TmsLink("TC-MLP-004") @Story("MLP public read") @Severity(SeverityLevel.NORMAL)
    public void queryWithUtm() {
        String o = requireLive();
        assertStructures(spec().queryParam("occasion", o).queryParam("utm_source", "wa")
                .when().get(BuyerAppEndpoints.MLP), o);
    }

    @Test(description = "TC-MLP-007 — GET /v1/mlp/:occasion - occasion with no documents", groups = "buyerapp")
    @TmsLink("TC-MLP-007") @Story("MLP public read") @Severity(SeverityLevel.NORMAL)
    public void unknownOccasion() {
        String slug = "qa-none-" + Long.toString(System.nanoTime(), 36);
        Response r = spec().pathParam("occasion", slug).when().get(BuyerAppEndpoints.MLP_BY_OCCASION);
        ApiErrorAssertions.assertApiError(r, HttpStatus.NOT_FOUND, "No structures configured for occasion");
    }

    @Test(description = "TC-MLP-008 — GET /v1/mlp - occasion query missing", groups = "buyerapp")
    @TmsLink("TC-MLP-008") @Story("MLP validation") @Severity(SeverityLevel.CRITICAL)
    public void queryMissing() {
        ApiErrorAssertions.assertApiError(spec().when().get(BuyerAppEndpoints.MLP), HttpStatus.BAD_REQUEST, "occasion");
    }

    @Test(description = "TC-MLP-009 — GET /v1/mlp/:occasion - occasion with invalid characters", groups = "buyerapp")
    @TmsLink("TC-MLP-009") @Story("MLP validation") @Severity(SeverityLevel.NORMAL)
    public void invalidOccasionChars() {
        // Pre-encoded so RestAssured does not double-encode the path.
        Response r = buyerAppGuestSpec().urlEncodingEnabled(false)
                .when().get("/v1/mlp/BAD%20OCC!");
        ApiErrorAssertions.assertApiError(r, HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-MLP-010 — GET /v1/mlp/list?occasion= - unfiltered authoring read", groups = "buyerapp")
    @TmsLink("TC-MLP-010") @Story("MLP public read") @Severity(SeverityLevel.NORMAL)
    public void listUnfiltered() {
        String o = requireLive();
        Response r = spec().queryParam("occasion", o).when().get(BuyerAppEndpoints.MLP_LIST);
        assertThat(r.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(r.jsonPath().getString("statusCode"), equalTo("10000"));
        assertThat(r.jsonPath().getString("data"), notNullValue());
    }

    @Test(description = "TC-MLP-011 — GET /v1/mlp/list - missing occasion", groups = "buyerapp")
    @TmsLink("TC-MLP-011") @Story("MLP validation") @Severity(SeverityLevel.NORMAL)
    public void listMissingOccasion() {
        ApiErrorAssertions.assertApiError(spec().when().get(BuyerAppEndpoints.MLP_LIST), HttpStatus.BAD_REQUEST, "occasion");
    }

    @Test(description = "TC-MLP-012 — GET /v1/mlp/occasions - distinct slugs", groups = "buyerapp")
    @TmsLink("TC-MLP-012") @Story("MLP public read") @Severity(SeverityLevel.NORMAL)
    public void occasionsList() {
        Response r = spec().when().get(BuyerAppEndpoints.MLP_OCCASIONS);
        assertThat(r.getStatusCode(), equalTo(HttpStatus.OK));
        List<String> list = r.jsonPath().getList("data.occasions");
        assertThat(list, notNullValue());
        assertThat("distinct", list.size(), equalTo(new HashSet<>(list).size()));
    }

    @Test(description = "TC-MLP-013 — /list and /occasions are not swallowed by /:occasion", groups = "buyerapp")
    @TmsLink("TC-MLP-013") @Story("MLP routing") @Severity(SeverityLevel.NORMAL)
    public void staticRoutesNotSwallowed() {
        Response occ = spec().when().get(BuyerAppEndpoints.MLP_OCCASIONS);
        assertThat(occ.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat("occasions payload, not an occasion lookup", occ.jsonPath().getString("data.occasions"), notNullValue());
        // /list without occasion is a 400 from the list schema; treated as occasion "list" it would be 404.
        Response list = spec().when().get(BuyerAppEndpoints.MLP_LIST);
        assertThat(list.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test(description = "TC-MLP-014 — GET /v1/admin/mlp - retired admin read, non-staff token", groups = "buyerapp")
    @TmsLink("TC-MLP-014") @Story("MLP admin gate") @Severity(SeverityLevel.CRITICAL)
    public void retiredAdminReadForbidden() {
        if (token == null || token.isEmpty()) {
            throw new SkipException("Buyer App token not available (run LoginTest first)");
        }
        Response r = spec().header("Authorization", "JWT " + token).when().get(BuyerAppEndpoints.ADMIN_MLP);
        ApiErrorAssertions.assertApiError(r, HttpStatus.FORBIDDEN, "Staff access required");
    }
}
