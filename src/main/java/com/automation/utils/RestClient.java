package com.automation.utils;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * REST Client utility class for making HTTP requests.
 * Provides a fluent interface for API interactions.
 */
public class RestClient {

    private static final Logger logger = LogManager.getLogger(RestClient.class);

    /**
     * Perform GET request
     */
    public Response get(String endpoint) {
        logger.info("Performing GET request to: {}", endpoint);
        return RestAssured.given()
                .when()
                .get(endpoint);
    }

    /**
     * Perform GET request with path parameters
     */
    public Response get(String endpoint, Map<String, ?> pathParams) {
        logger.info("Performing GET request to: {} with path params: {}", endpoint, pathParams);
        return RestAssured.given()
                .pathParams(pathParams)
                .when()
                .get(endpoint);
    }

    /**
     * Perform GET request with query parameters
     */
    public Response getWithQueryParams(String endpoint, Map<String, ?> queryParams) {
        logger.info("Performing GET request to: {} with query params: {}", endpoint, queryParams);
        return RestAssured.given()
                .queryParams(queryParams)
                .when()
                .get(endpoint);
    }

    /**
     * Perform POST request with body
     */
    public Response post(String endpoint, Object body) {
        logger.info("Performing POST request to: {}", endpoint);
        return RestAssured.given()
                .body(body)
                .when()
                .post(endpoint);
    }

    /**
     * Perform POST request with body and headers
     */
    public Response post(String endpoint, Object body, Map<String, String> headers) {
        logger.info("Performing POST request to: {} with custom headers", endpoint);
        return RestAssured.given()
                .headers(headers)
                .body(body)
                .when()
                .post(endpoint);
    }

    /**
     * Perform PUT request with body
     */
    public Response put(String endpoint, Object body) {
        logger.info("Performing PUT request to: {}", endpoint);
        return RestAssured.given()
                .body(body)
                .when()
                .put(endpoint);
    }

    /**
     * Perform PUT request with path parameters and body
     */
    public Response put(String endpoint, Map<String, ?> pathParams, Object body) {
        logger.info("Performing PUT request to: {} with path params: {}", endpoint, pathParams);
        return RestAssured.given()
                .pathParams(pathParams)
                .body(body)
                .when()
                .put(endpoint);
    }

    /**
     * Perform PATCH request with body
     */
    public Response patch(String endpoint, Object body) {
        logger.info("Performing PATCH request to: {}", endpoint);
        return RestAssured.given()
                .body(body)
                .when()
                .patch(endpoint);
    }

    /**
     * Perform PATCH request with path parameters and body
     */
    public Response patch(String endpoint, Map<String, ?> pathParams, Object body) {
        logger.info("Performing PATCH request to: {} with path params: {}", endpoint, pathParams);
        return RestAssured.given()
                .pathParams(pathParams)
                .body(body)
                .when()
                .patch(endpoint);
    }

    /**
     * Perform DELETE request
     */
    public Response delete(String endpoint) {
        logger.info("Performing DELETE request to: {}", endpoint);
        return RestAssured.given()
                .when()
                .delete(endpoint);
    }

    /**
     * Perform DELETE request with path parameters
     */
    public Response delete(String endpoint, Map<String, ?> pathParams) {
        logger.info("Performing DELETE request to: {} with path params: {}", endpoint, pathParams);
        return RestAssured.given()
                .pathParams(pathParams)
                .when()
                .delete(endpoint);
    }

    /**
     * Get a RequestSpecification for custom request building
     */
    public RequestSpecification given() {
        return RestAssured.given();
    }
}
