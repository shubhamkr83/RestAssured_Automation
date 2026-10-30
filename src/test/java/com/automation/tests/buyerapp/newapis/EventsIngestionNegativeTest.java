package com.automation.tests.buyerapp.newapis;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * POST /events and /events/impressions - ONLY cases rejected before the Kafka publish
 * (routines/event/createEvent.ts, routines/impression/postImpressions.ts). Go-style envelope.
 */
@Epic("Buyer App – New APIs")
@Feature("Events ingestion (negative)")
public class EventsIngestionNegativeTest extends BaseTest {

    private String baseUrl;

    @BeforeClass
    public void setup() {
        baseUrl = config.buyerAppBaseUrl();
    }

    private Response post(String path, String body) {
        return RestAssured.given().baseUri(baseUrl).contentType("application/json").body(body).when().post(path);
    }

    private void assertGoEnvelope(Response r, int status, String message, String error) {
        assertThat("HTTP status (body: " + r.asString() + ")", r.getStatusCode(), equalTo(status));
        assertThat(r.jsonPath().getInt("status"), equalTo(status));
        assertThat(r.jsonPath().getString("message"), equalTo(message));
        assertThat(r.jsonPath().getString("data.error"), equalTo(error));
    }

    @Test(description = "TC-EVENTS-003 — POST /events - array body", groups = "buyerapp")
    @TmsLink("TC-EVENTS-003") @Story("Events ingestion") @Severity(SeverityLevel.NORMAL)
    public void eventsArrayBody() {
        assertGoEnvelope(post(BuyerAppEndpoints.EVENTS, "[{\"event_name\":\"pdp_viewed\"}]"),
                HttpStatus.BAD_REQUEST, "Bad Request", "invalid payload: expected an event object");
    }

    @Test(description = "TC-EVENTS-004 — POST /events - missing time_stamp", groups = "buyerapp")
    @TmsLink("TC-EVENTS-004") @Story("Events ingestion") @Severity(SeverityLevel.NORMAL)
    public void eventsMissingTimestamp() {
        assertGoEnvelope(post(BuyerAppEndpoints.EVENTS, "{\"event_name\":\"pdp_viewed\"}"),
                HttpStatus.INTERNAL_SERVER_ERROR, "error", "Error parsing timestamp: undefined");
    }

    @Test(description = "TC-EVENTS-005 — POST /events - non-integer or numeric time_stamp", groups = "buyerapp")
    @TmsLink("TC-EVENTS-005") @Story("Events ingestion") @Severity(SeverityLevel.NORMAL)
    public void eventsBadTimestampVariants() {
        assertGoEnvelope(post(BuyerAppEndpoints.EVENTS, "{\"event_name\":\"pdp_viewed\",\"time_stamp\":\"17618.5\"}"),
                HttpStatus.INTERNAL_SERVER_ERROR, "error", "Error parsing timestamp: 17618.5");
        assertGoEnvelope(post(BuyerAppEndpoints.EVENTS, "{\"event_name\":\"pdp_viewed\",\"time_stamp\":\"abc\"}"),
                HttpStatus.INTERNAL_SERVER_ERROR, "error", "Error parsing timestamp: abc");
        assertGoEnvelope(post(BuyerAppEndpoints.EVENTS, "{\"event_name\":\"pdp_viewed\",\"time_stamp\":1761800000000}"),
                HttpStatus.INTERNAL_SERVER_ERROR, "error", "Error parsing timestamp: 1761800000000");
    }

    @Test(description = "TC-EVENTS-009 — POST /events/impressions - empty batch", groups = "buyerapp")
    @TmsLink("TC-EVENTS-009") @Story("Impressions ingestion") @Severity(SeverityLevel.NORMAL)
    public void impressionsEmptyBatch() {
        Response r = post(BuyerAppEndpoints.EVENTS_IMPRESSIONS, "{\"data\":[]}");
        assertThat("HTTP status (body: " + r.asString() + ")", r.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(r.jsonPath().getInt("status"), equalTo(200));
        assertThat(r.jsonPath().getString("message"), equalTo("success"));
        assertThat(r.jsonPath().getInt("data.count"), equalTo(0));
        assertThat(r.jsonPath().getList("data.insertedIds"), empty());
    }

    @Test(description = "TC-EVENTS-010 — POST /events/impressions - data missing or not an array", groups = "buyerapp")
    @TmsLink("TC-EVENTS-010") @Story("Impressions ingestion") @Severity(SeverityLevel.NORMAL)
    public void impressionsBadShape() {
        String err = "invalid payload: expected object with data array";
        // "[]" stands in for the bare-array variant: not an object with a data array, rejected before any publish.
        for (String body : new String[]{"{}", "{\"data\":{}}", "[]"}) {
            assertGoEnvelope(post(BuyerAppEndpoints.EVENTS_IMPRESSIONS, body), HttpStatus.BAD_REQUEST, "Bad Request", err);
        }
    }

    @Test(description = "TC-EVENTS-013 — /v1/events is not a route", groups = "buyerapp")
    @TmsLink("TC-EVENTS-013") @Story("Events ingestion") @Severity(SeverityLevel.MINOR)
    public void v1EventsNotARoute() {
        // Deliberately timestamp-less: even if it were routed it would be rejected before publish.
        Response r = post(BuyerAppEndpoints.EVENTS_V1_NOT_A_ROUTE, "{\"event_name\":\"pdp_viewed\"}");
        assertThat(r.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }
}
