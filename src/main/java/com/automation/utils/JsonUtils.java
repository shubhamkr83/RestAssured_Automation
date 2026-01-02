package com.automation.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Utility class for JSON operations.
 * Provides serialization, deserialization, and JSON file handling.
 */
public class JsonUtils {

    private static final Logger logger = LogManager.getLogger(JsonUtils.class);
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private JsonUtils() {
        // Prevent instantiation
    }

    /**
     * Serialize object to JSON string
     */
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing object to JSON: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * Deserialize JSON string to object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            logger.error("========== JSON DESERIALIZATION ERROR ==========");
            logger.error("Target Class: {}", clazz.getName());
            logger.error("Error Message: {}", e.getMessage());
            logger.error("Error Location: {}", e.getLocation());
            logger.error("Problematic JSON (first 500 chars): {}", 
                json != null && json.length() > 500 ? json.substring(0, 500) + "..." : json);
            logger.error("Full Error Details: ", e);
            logger.error("================================================");
            throw new RuntimeException("Failed to deserialize JSON to " + clazz.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Deserialize Response body to object
     * Validates HTTP status code before attempting deserialization
     */
    public static <T> T fromResponse(Response response, Class<T> clazz) {
        int statusCode = response.getStatusCode();
        String responseBody = response.getBody().asString();
        
        // Check if response is successful (2xx status codes)
        if (statusCode < 200 || statusCode >= 300) {
            logger.error("========== HTTP ERROR RESPONSE ==========");
            logger.error("HTTP Status Code: {}", statusCode);
            logger.error("Status Line: {}", response.getStatusLine());
            logger.error("Target Class: {}", clazz.getName());
            logger.error("Response Body (first 1000 chars): {}", 
                responseBody != null && responseBody.length() > 1000 
                    ? responseBody.substring(0, 1000) + "..." 
                    : responseBody);
            logger.error("Content-Type: {}", response.getContentType());
            logger.error("=========================================");
            
            throw new RuntimeException(String.format(
                "Cannot deserialize to %s: HTTP %d - %s. Response body: %s",
                clazz.getSimpleName(),
                statusCode,
                response.getStatusLine(),
                responseBody != null && responseBody.length() > 200 
                    ? responseBody.substring(0, 200) + "..." 
                    : responseBody
            ));
        }
        
        // Check if response is actually JSON
        String contentType = response.getContentType();
        if (contentType != null && !contentType.toLowerCase().contains("json")) {
            logger.warn("Response Content-Type is not JSON: {}", contentType);
            logger.warn("Attempting to parse anyway, but this might fail");
        }
        
        return fromJson(responseBody, clazz);
    }

    /**
     * Deserialize JSON string to list of objects
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing JSON to list: {}", e.getMessage());
            throw new RuntimeException("Failed to deserialize JSON to list", e);
        }
    }

    /**
     * Deserialize Response body to list of objects
     */
    public static <T> List<T> fromResponseToList(Response response, Class<T> clazz) {
        return fromJsonToList(response.getBody().asString(), clazz);
    }

    /**
     * Read JSON file and return as string
     */
    public static String readJsonFile(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            logger.error("Error reading JSON file: {}", e.getMessage());
            throw new RuntimeException("Failed to read JSON file: " + filePath, e);
        }
    }

    /**
     * Read JSON file and deserialize to object
     */
    public static <T> T readJsonFile(String filePath, Class<T> clazz) {
        try {
            return objectMapper.readValue(new File(filePath), clazz);
        } catch (IOException e) {
            logger.error("Error reading JSON file: {}", e.getMessage());
            throw new RuntimeException("Failed to read JSON file: " + filePath, e);
        }
    }

    /**
     * Read JSON file from resources folder
     */
    public static String readJsonFromResources(String resourcePath) {
        try {
            ClassLoader classLoader = JsonUtils.class.getClassLoader();
            return new String(Files.readAllBytes(
                    Paths.get(classLoader.getResource(resourcePath).toURI())));
        } catch (Exception e) {
            logger.error("Error reading JSON from resources: {}", e.getMessage());
            throw new RuntimeException("Failed to read JSON from resources: " + resourcePath, e);
        }
    }

    /**
     * Get ObjectMapper instance for custom operations
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
