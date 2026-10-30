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
 * Unified chatbot — POST /v2/chatbot/message. PRODUCTION-safe: every request
 * is refused by auth or the Joi validator before the bot runs (no LLM call,
 * no session write). The validator replaces the Joi detail with the generic
 * chat-bubble reply. Rate limit is 20/min per buyer; this class sends 6.
 */
@Epic("Buyer App – New APIs")
@Feature("Chatbot v2 Validation")
public class ChatbotV2ValidationTest extends BaseTest {

    private static final String CHAT_INVALID_REPLY_FRAGMENT = "technical dikkat";

    private String baseUrl;
    private String buyerToken;

    @BeforeClass
    public void setup() {
        baseUrl = config.buyerAppBaseUrl();
        buyerToken = VariableManager.getBuyerAppToken();
    }

    private Response send(String body) {
        if (buyerToken == null || buyerToken.isEmpty()) {
            throw new SkipException("Buyer App token not available. Run LoginTest first.");
        }
        return RestAssured.given().baseUri(baseUrl).contentType("application/json")
                .header("Authorization", "JWT " + buyerToken).body(body)
                .when().post(BuyerAppEndpoints.CHATBOT_V2_MESSAGE);
    }

    private void assertGenericInvalidReply(Response resp) {
        assertApiError(resp, HttpStatus.BAD_REQUEST, CHAT_INVALID_REPLY_FRAGMENT);
        assertThat("no Joi detail leaks", resp.jsonPath().getString("message"), not(containsString("\"")));
    }

    @Test(description = "TC-CHATV2-004 — no token", groups = "buyerapp")
    @TmsLink("TC-CHATV2-004") @Story("Auth") @Severity(SeverityLevel.CRITICAL)
    public void noToken() {
        Response resp = RestAssured.given().baseUri(baseUrl).contentType("application/json")
                .body("{\"message\":\"hi\"}").when().post(BuyerAppEndpoints.CHATBOT_V2_MESSAGE);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }

    @Test(description = "TC-CHATV2-006 — message missing", groups = "buyerapp")
    @TmsLink("TC-CHATV2-006") @Story("Validation") @Severity(SeverityLevel.NORMAL)
    public void messageMissing() {
        assertGenericInvalidReply(send("{}"));
    }

    @Test(description = "TC-CHATV2-007 — empty or whitespace message", groups = "buyerapp")
    @TmsLink("TC-CHATV2-007") @Story("Validation") @Severity(SeverityLevel.NORMAL)
    public void messageWhitespace() {
        assertGenericInvalidReply(send("{\"message\":\"   \"}"));
    }

    @Test(description = "TC-CHATV2-008 — message longer than 1000 chars", groups = "buyerapp")
    @TmsLink("TC-CHATV2-008") @Story("Validation") @Severity(SeverityLevel.NORMAL)
    public void messageTooLong() {
        assertGenericInvalidReply(send("{\"message\":\"" + "a".repeat(1001) + "\"}"));
    }

    @Test(description = "TC-CHATV2-010 — imageUrl with http scheme", groups = "buyerapp")
    @TmsLink("TC-CHATV2-010") @Story("Validation") @Severity(SeverityLevel.NORMAL)
    public void imageUrlHttp() {
        assertGenericInvalidReply(send("{\"message\":\"hi\",\"imageUrl\":\"http://example.com/a.jpg\"}"));
    }

    @Test(description = "TC-CHATV2-011 — both imageUrl and imageBase64", groups = "buyerapp")
    @TmsLink("TC-CHATV2-011") @Story("Validation") @Severity(SeverityLevel.NORMAL)
    public void bothImageInputs() {
        assertGenericInvalidReply(send("{\"message\":\"hi\",\"imageUrl\":\"https://example.com/a.jpg\","
                + "\"imageBase64\":\"aGVsbG8=\"}"));
    }

    @Test(description = "TC-CHATV2-012 — supportedBlocks with more than 50 items", groups = "buyerapp")
    @TmsLink("TC-CHATV2-012") @Story("Validation") @Severity(SeverityLevel.NORMAL)
    public void tooManySupportedBlocks() {
        StringBuilder blocks = new StringBuilder("[");
        for (int i = 0; i < 51; i++) {
            blocks.append(i == 0 ? "" : ",").append("\"block").append(i).append("\"");
        }
        blocks.append("]");
        assertGenericInvalidReply(send("{\"message\":\"hi\",\"supportedBlocks\":" + blocks + "}"));
    }
}
