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
 * PRISM staff routes — /v1/prism/*. Any JWT passes the router; staff gating is
 * inside each handler. PRODUCTION-safe: no token / buyer JWT / invalid bodies
 * only. Never calls cart-bot analyze or catalog auto-tag (live Claude calls).
 *
 * TC-PRISM-018 and -021 send a valid-shaped body, which is safe only because
 * assertStaff is the first statement of those handlers. If the test buyer is
 * itself staff, they would reach the LLM — so both are skipped unless a
 * read-only probe through the same assertStaff returns 403 first.
 */
@Epic("Buyer App – New APIs")
@Feature("PRISM Access Control")
public class PrismAccessTest extends BaseTest {

    private static final String CHAT_INVALID_REPLY_FRAGMENT = "technical dikkat";

    private String baseUrl;
    private String buyerToken;

    @BeforeClass(alwaysRun = true)
    public void setup() {
        baseUrl = config.buyerAppBaseUrl();
        buyerToken = buyerAppTokenOrNull();
    }

    private void requireBuyerToken() {
        if (buyerToken == null || buyerToken.isEmpty()) {
            throw new SkipException("Buyer App token not available. Run LoginTest first.");
        }
    }

    private Response asBuyer(String body, String path) {
        return buyerAppGuestSpec()
                .header("Authorization", "JWT " + buyerToken).body(body).when().post(path);
    }

    /** Read-only probe through the same assertStaff; skip unless the buyer is refused. */
    private void requireBuyerIsNotStaff(String probePath) {
        Response probe = buyerAppGuestSpec()
                .header("Authorization", "JWT " + buyerToken).when().get(probePath);
        if (probe.getStatusCode() != HttpStatus.FORBIDDEN) {
            throw new SkipException("Test buyer is not refused by the staff gate (probe "
                    + probePath + " returned " + probe.getStatusCode()
                    + "); skipping to avoid a live LLM call on production.");
        }
    }

    @Test(description = "TC-PRISM-001 — /v1/prism route with no token", groups = "buyerapp")
    @TmsLink("TC-PRISM-001") @Story("Auth") @Severity(SeverityLevel.CRITICAL)
    public void noToken() {
        Response resp = buyerAppGuestSpec().when().get(BuyerAppEndpoints.PRISM_CART_BOT_SESSIONS);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }

    @Test(description = "TC-PRISM-002 — /v1/prism route with an invalid JWT", groups = "buyerapp")
    @TmsLink("TC-PRISM-002") @Story("Auth") @Severity(SeverityLevel.CRITICAL)
    public void invalidToken() {
        Response resp = buyerAppGuestSpec()
                .header("Authorization", "JWT not-a-real-token")
                .when().get(BuyerAppEndpoints.PRISM_CART_BOT_SESSIONS);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }

    @Test(description = "TC-PRISM-007 — chatbot-feedback runs with a buyer JWT", groups = "buyerapp")
    @TmsLink("TC-PRISM-007") @Story("Chatbot feedback") @Severity(SeverityLevel.CRITICAL)
    public void feedbackRunsAsBuyer() {
        requireBuyerToken();
        Response resp = buyerAppGuestSpec()
                .header("Authorization", "JWT " + buyerToken)
                .when().get(BuyerAppEndpoints.PRISM_CHATBOT_FEEDBACK_RUNS);
        assertApiError(resp, HttpStatus.FORBIDDEN);
    }

    @Test(description = "TC-PRISM-010 — chatbot-feedback conversation transcript with a buyer JWT", groups = "buyerapp")
    @TmsLink("TC-PRISM-010") @Story("Chatbot feedback") @Severity(SeverityLevel.CRITICAL)
    public void feedbackConversationAsBuyer() {
        requireBuyerToken();
        Response resp = buyerAppGuestSpec()
                .header("Authorization", "JWT " + buyerToken)
                .pathParam("evaluationId", "000000000000000000000000")
                .when().get(BuyerAppEndpoints.PRISM_CHATBOT_FEEDBACK_CONVERSATION);
        assertApiError(resp, HttpStatus.FORBIDDEN);
    }

    @Test(description = "TC-PRISM-016 — bot-trial message missing", groups = "buyerapp")
    @TmsLink("TC-PRISM-016") @Story("Bot trial") @Severity(SeverityLevel.NORMAL)
    public void botTrialMissingMessage() {
        requireBuyerToken();
        Response resp = asBuyer("{}", BuyerAppEndpoints.PRISM_BOT_TRIAL_MESSAGE);
        // The validator replaces the Joi detail with the chat-bubble reply.
        assertApiError(resp, HttpStatus.BAD_REQUEST, CHAT_INVALID_REPLY_FRAGMENT);
        assertThat("no Joi detail leaks", resp.jsonPath().getString("message"), not(containsString("required")));
    }

    @Test(description = "TC-PRISM-018 — bot-trial message with a buyer JWT", groups = "buyerapp")
    @TmsLink("TC-PRISM-018") @Story("Bot trial") @Severity(SeverityLevel.CRITICAL)
    public void botTrialAsBuyer() {
        requireBuyerToken();
        requireBuyerIsNotStaff("/v1/prism/bot-trial/sessions");
        Response resp = asBuyer("{\"mobileNumber\":\"9999999999\",\"message\":\"hi\"}",
                BuyerAppEndpoints.PRISM_BOT_TRIAL_MESSAGE);
        assertApiError(resp, HttpStatus.FORBIDDEN, "staff");
    }

    @Test(description = "TC-PRISM-020 — image-enhancement generate with an invalid body", groups = "buyerapp")
    @TmsLink("TC-PRISM-020") @Story("Image enhancement") @Severity(SeverityLevel.NORMAL)
    public void imageEnhancementInvalidBody() {
        requireBuyerToken();
        Response resp = asBuyer("{}", BuyerAppEndpoints.PRISM_IMAGE_ENHANCEMENT_GENERATE);
        assertApiError(resp, HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-PRISM-021 — image-enhancement generate with a buyer JWT", groups = "buyerapp")
    @TmsLink("TC-PRISM-021") @Story("Image enhancement") @Severity(SeverityLevel.CRITICAL)
    public void imageEnhancementAsBuyer() {
        requireBuyerToken();
        requireBuyerIsNotStaff("/v1/prism/image-enhancement/jobs/probe");
        Response resp = asBuyer(
                "{\"imageUrl\":\"https://example.com/a.jpg\",\"category\":\"saree\",\"variants\":[\"enhanced\"]}",
                BuyerAppEndpoints.PRISM_IMAGE_ENHANCEMENT_GENERATE);
        assertApiError(resp, HttpStatus.FORBIDDEN, "restricted to staff");
    }

    @Test(description = "TC-PRISM-023 — image-enhancement edit / save / remove with invalid bodies", groups = "buyerapp")
    @TmsLink("TC-PRISM-023") @Story("Image enhancement") @Severity(SeverityLevel.NORMAL)
    public void imageEnhancementOtherRoutesInvalidBody() {
        requireBuyerToken();
        for (String path : new String[]{
                BuyerAppEndpoints.PRISM_IMAGE_ENHANCEMENT_EDIT,
                BuyerAppEndpoints.PRISM_IMAGE_ENHANCEMENT_SAVE,
                BuyerAppEndpoints.PRISM_IMAGE_ENHANCEMENT_REMOVE}) {
            assertApiError(asBuyer("{}", path), HttpStatus.BAD_REQUEST);
        }
    }
}
