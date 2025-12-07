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
            logger.error("Error deserializing JSON to object: {}", e.getMessage());
            throw new RuntimeException("Failed to deserialize JSON to object", e);
        }
    }

    /**
     * Deserialize Response body to object
     */
    public static <T> T fromResponse(Response response, Class<T> clazz) {
        return fromJson(response.getBody().asString(), clazz);
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
