package com.automation.base;

import com.automation.config.ConfigManager;
import com.automation.utils.RestClient;
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
    protected RestClient restClient;

    @BeforeSuite(alwaysRun = true)
    @Parameters({ "env" })
    public void beforeSuite(@Optional("default") String env) {
        logger.info("Initializing test suite with environment: {}", env);
        System.setProperty("env", env);
        config = ConfigManager.getInstance();
    }

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        logger.info("Setting up test class: {}", this.getClass().getSimpleName());
        initializeRestAssured();
        restClient = new RestClient();
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
}
