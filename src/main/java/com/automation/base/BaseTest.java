package com.automation.base;

import com.automation.config.ConfigManager;
import com.automation.utils.VariableManager;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 * Base test class providing common setup and configuration.
 * All test classes should extend this class.
 */
public abstract class BaseTest {

    protected static final Logger logger = LogManager.getLogger(BaseTest.class);
    protected static ConfigManager config;
    protected static RequestSpecification requestSpec;
    protected static ResponseSpecification responseSpec;

    @BeforeSuite(alwaysRun = true)
    @Parameters({ "env" })
    public void beforeSuite(@Optional("default") String env) {
        logger.info("Initializing test suite with environment: {}", env);
        System.setProperty("env", env);
        config = ConfigManager.getInstance();
        
        // Initialize VariableManager for dynamic variable management
        VariableManager.initialize();
        logger.info("VariableManager initialized with {} variables", VariableManager.count());
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        // Cleanup VariableManager to prevent memory leaks
        VariableManager.cleanup();
        logger.info("Test suite execution completed, VariableManager cleaned up");
    }

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        logger.info("Setting up test class: {}", this.getClass().getSimpleName());
        initializeRestAssured();
    }

    /**
     * Initialize RestAssured with default configurations
     */
    private void initializeRestAssured() {
        RestAssured.baseURI = config.baseUrl();

        // Build request specification
        RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured());

        if (config.logRequest()) {
            requestSpecBuilder.log(LogDetail.ALL);
        }

        // Add authentication if configured
        configureAuthentication(requestSpecBuilder);

        requestSpec = requestSpecBuilder.build();

        // Build response specification
        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        if (config.logResponse()) {
            responseSpecBuilder.log(LogDetail.ALL);
        }
        responseSpec = responseSpecBuilder.build();

        RestAssured.requestSpecification = requestSpec;
        RestAssured.responseSpecification = responseSpec;

        logger.info("RestAssured initialized with base URI: {}", config.baseUrl());
    }

    /**
     * Configure authentication based on config settings
     */
    private void configureAuthentication(RequestSpecBuilder builder) {
        String authType = config.authType();

        switch (authType.toLowerCase()) {
            case "basic":
                builder.setAuth(RestAssured.basic(config.authUsername(), config.authPassword()));
                logger.info("Basic authentication configured");
                break;
            case "bearer":
            case "token":
                builder.addHeader("Authorization", "Bearer " + config.authToken());
                logger.info("Bearer token authentication configured");
                break;
            case "api_key":
                builder.addHeader("X-API-Key", config.authToken());
                logger.info("API key authentication configured");
                break;
            case "none":
            default:
                logger.info("No authentication configured");
                break;
        }
    }

    /**
     * Get a fresh request specification for custom configurations
     */
    protected RequestSpecification getRequestSpec() {
        return RestAssured.given().spec(requestSpec);
    }

    /**
     * Buyer App (Navo) request as a guest: base URL plus the headers the real app
     * sends. No Authorization header.
     */
    protected static RequestSpecification buyerAppGuestSpec() {
        return RestAssured.given()
                // A test that sets one of these headers itself replaces the default
                // instead of sending both values.
                .config(RestAssured.config().headerConfig(io.restassured.config.HeaderConfig.headerConfig()
                        .overwriteHeadersWithName("Accept", "Content-Type", "Accept-Language", "AppVersion",
                                "AppVersionCode", "User-Segment", "zone", "Authorization")))
                .baseUri(config.buyerAppBaseUrl())
                .contentType(ContentType.JSON)
                .header("Accept-Language", config.buyerAppLanguage())
                .header("AppVersion", config.buyerAppVersion())
                .header("AppVersionCode", config.buyerAppVersionCode())
                .header("User-Segment", config.buyerAppUserSegment())
                .header("zone", config.buyerAppZone());
    }

    /**
     * Buyer App (Navo) request as the logged-in test buyer. Tokens are no longer
     * persisted between runs, so if LoginTest has not run yet (e.g. an ad-hoc
     * -Dtest run) this logs in once with the configured credentials.
     */
    protected static RequestSpecification buyerAppSpec() {
        String token = VariableManager.getBuyerAppToken();
        if (token == null || token.isEmpty()) {
            token = loginBuyer();
        }
        return buyerAppGuestSpec().header("Authorization", "JWT " + token);
    }

    /** The test buyer's token, logging in on demand; null if login is not possible. */
    protected static String buyerAppTokenOrNull() {
        try {
            String token = VariableManager.getBuyerAppToken();
            return (token == null || token.isEmpty()) ? loginBuyer() : token;
        } catch (org.testng.SkipException e) {
            logger.warn(e.getMessage());
            return null;
        }
    }

    private static synchronized String loginBuyer() {
        String existing = VariableManager.getBuyerAppToken();
        if (existing != null && !existing.isEmpty()) return existing;
        if (config.buyerAppPhoneNumber() == null || config.buyerAppToken() == null) {
            throw new org.testng.SkipException("Buyer login credentials missing: set BUYER_LOGIN_PHONE / "
                    + "BUYER_LOGIN_OTP or create src/test/resources/secrets.properties.");
        }
        String token = buyerAppGuestSpec()
                .body(java.util.Map.of("phoneNumber", config.buyerAppPhoneNumber(), "token", config.buyerAppToken()))
                .post("/api/auth/login")
                .jsonPath().getString("data.accessToken");
        if (token == null || token.isEmpty()) {
            throw new org.testng.SkipException("Buyer App login failed; cannot run authenticated tests.");
        }
        VariableManager.setBuyerAppToken(token);
        VariableManager.saveBuyerAppTokenToFile(); // in-memory only: shares the token with other threads
        logger.info("Logged in as the test buyer on demand");
        return token;
    }
}
