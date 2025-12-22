package com.automation.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe Variable Manager for managing test variables across the framework.
 * <p>
 * This utility provides centralized variable management with support for:
 * - Loading initial values from test-variables.properties
 * - Thread-safe get/set operations using ThreadLocal
 * - Type conversion helpers (String, int, boolean)
 * - Dynamic runtime updates (e.g., tokens from API responses)
 * - Parallel test execution support
 * </p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Reading variables
 * String token = VariableManager.get("bomb_token");
 * int timeout = VariableManager.getInt("res_time");
 * 
 * // Updating variables
 * VariableManager.set("bomb_token", newToken);
 * VariableManager.setToken(accessToken);
 * 
 * // Using in tests
 * String baseUrl = VariableManager.get("bizup_base");
 * }</pre>
 * 
 * @author API Automation Framework
 * @version 1.0.0
 */
public class VariableManager {

    private static final Logger logger = LogManager.getLogger(VariableManager.class);
    
    /**
     * ThreadLocal storage for thread-safe variable management.
     * Each thread maintains its own copy of variables.
     */
    private static final ThreadLocal<Map<String, String>> threadLocalVariables = new ThreadLocal<>();
    
    /**
     * Global properties loaded from test-variables.properties file.
     * Used as initial values for all threads.
     */
    private static final Properties globalProperties = new Properties();
    
    /**
     * Flag to track initialization status.
     */
    private static volatile boolean initialized = false;
    
    /**
     * Properties file name.
     */
    private static final String PROPERTIES_FILE = "test-variables.properties";

    // Private constructor to prevent instantiation
    private VariableManager() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Initialize the VariableManager by loading properties from test-variables.properties.
     * This method should be called once during test suite initialization.
     * Thread-safe and idempotent - multiple calls are safe.
     */
    public static synchronized void initialize() {
        if (initialized) {
            logger.debug("VariableManager already initialized, skipping...");
            return;
        }

        try (InputStream input = VariableManager.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {
            
            if (input == null) {
                logger.warn("Unable to find {}. VariableManager will work with empty initial values.", 
                    PROPERTIES_FILE);
                initialized = true;
                return;
            }

            globalProperties.load(input);
            logger.info("VariableManager initialized successfully with {} variables from {}", 
                globalProperties.size(), PROPERTIES_FILE);
            initialized = true;

        } catch (IOException e) {
            logger.error("Error loading {}: {}", PROPERTIES_FILE, e.getMessage());
            throw new RuntimeException("Failed to initialize VariableManager", e);
        }
    }

    /**
     * Get the thread-local variable map. Creates a new map if not exists for current thread.
     * Automatically loads global properties as initial values.
     * 
     * @return Thread-local variable map
     */
    private static Map<String, String> getVariablesMap() {
        Map<String, String> variables = threadLocalVariables.get();
        
        if (variables == null) {
            // Initialize thread-local map with global properties
            variables = new HashMap<>();
            
            // Copy all global properties to thread-local storage
            for (String key : globalProperties.stringPropertyNames()) {
                variables.put(key, globalProperties.getProperty(key));
            }
            
            threadLocalVariables.set(variables);
            logger.debug("Initialized thread-local variables for thread: {}", 
                Thread.currentThread().getName());
        }
        
        return variables;
    }

    /**
     * Get a variable value as String.
     * 
     * @param key Variable key
     * @return Variable value, or null if not found
     */
    public static String get(String key) {
        if (key == null || key.trim().isEmpty()) {
            logger.warn("Attempted to get variable with null or empty key");
            return null;
        }
        
        String value = getVariablesMap().get(key);
        
        if (value == null) {
            logger.debug("Variable '{}' not found", key);
        }
        
        return value;
    }

    /**
     * Get a variable value as String with default value if not found.
     * 
     * @param key Variable key
     * @param defaultValue Default value to return if key not found
     * @return Variable value, or defaultValue if not found
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get a variable value as integer.
     * 
     * @param key Variable key
     * @return Variable value as int
     * @throws NumberFormatException if value cannot be parsed as integer
     * @throws IllegalArgumentException if variable not found
     */
    public static int getInt(String key) {
        String value = get(key);
        
        if (value == null) {
            throw new IllegalArgumentException("Variable '" + key + "' not found");
        }
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.error("Failed to parse variable '{}' with value '{}' as integer", key, value);
            throw e;
        }
    }

    /**
     * Get a variable value as integer with default value.
     * 
     * @param key Variable key
     * @param defaultValue Default value to return if key not found or parsing fails
     * @return Variable value as int, or defaultValue
     */
    public static int getInt(String key, int defaultValue) {
        try {
            String value = get(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse variable '{}' as integer, using default value: {}", 
                key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Get a variable value as boolean.
     * 
     * @param key Variable key
     * @return Variable value as boolean
     * @throws IllegalArgumentException if variable not found
     */
    public static boolean getBoolean(String key) {
        String value = get(key);
        
        if (value == null) {
            throw new IllegalArgumentException("Variable '" + key + "' not found");
        }
        
        return Boolean.parseBoolean(value);
    }

    /**
     * Get a variable value as boolean with default value.
     * 
     * @param key Variable key
     * @param defaultValue Default value to return if key not found
     * @return Variable value as boolean, or defaultValue
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Set a variable value.
     * 
     * @param key Variable key
     * @param value Variable value (will be converted to String)
     */
    public static void set(String key, Object value) {
        if (key == null || key.trim().isEmpty()) {
            logger.warn("Attempted to set variable with null or empty key");
            return;
        }
        
        String stringValue = value != null ? value.toString() : null;
        getVariablesMap().put(key, stringValue);
        
        logger.debug("Variable '{}' updated to: {}", key, 
            stringValue != null && stringValue.length() > 50 
                ? stringValue.substring(0, 50) + "..." 
                : stringValue);
    }

    /**
     * Check if a variable exists.
     * 
     * @param key Variable key
     * @return true if variable exists, false otherwise
     */
    public static boolean has(String key) {
        return key != null && getVariablesMap().containsKey(key);
    }

    /**
     * Remove a variable.
     * 
     * @param key Variable key
     * @return Previous value, or null if not found
     */
    public static String remove(String key) {
        if (key == null) {
            return null;
        }
        
        String removed = getVariablesMap().remove(key);
        
        if (removed != null) {
            logger.debug("Variable '{}' removed", key);
        }
        
        return removed;
    }

    /**
     * Clear all variables for the current thread.
     */
    public static void clear() {
        Map<String, String> variables = threadLocalVariables.get();
        
        if (variables != null) {
            variables.clear();
            logger.debug("All variables cleared for thread: {}", Thread.currentThread().getName());
        }
    }

    /**
     * Get the BOMB API token (bomb_token).
     * Convenience method for accessing the authentication token.
     * 
     * @return BOMB API token
     */
    public static String getToken() {
        return get("bomb_token");
    }

    /**
     * Set the BOMB API token (bomb_token).
     * Convenience method for updating the authentication token.
     * 
     * @param token New token value
     */
    public static void setToken(String token) {
        set("bomb_token", token);
        logger.info("BOMB token updated successfully");
    }

    /**
     * Get the Buyer App token (buyer_app_token).
     * Convenience method for accessing the buyer app authentication token.
     * 
     * @return Buyer App token
     */
    public static String getBuyerAppToken() {
        return get("buyer_app_token");
    }

    /**
     * Set the Buyer App token (buyer_app_token).
     * Convenience method for updating the buyer app authentication token.
     * 
     * @param token New token value
     */
    public static void setBuyerAppToken(String token) {
        set("buyer_app_token", token);
        logger.info("Buyer App token updated successfully");
    }

    /**
     * Save the current token to the properties file for persistence across test runs.
     * This allows tokens to be shared between independent test executions.
     */
    public static synchronized void saveTokenToFile() {
        String token = getToken();
        if (token == null || token.isEmpty()) {
            logger.warn("Cannot save null or empty token to file");
            return;
        }
        
        savePropertyToFile("bomb_token", token);
    }

    /**
     * Save the buyer app token to the properties file.
     */
    public static synchronized void saveBuyerAppTokenToFile() {
        String token = getBuyerAppToken();
        if (token == null || token.isEmpty()) {
            logger.warn("Cannot save null or empty buyer app token to file");
            return;
        }
        
        savePropertyToFile("buyer_app_token", token);
    }

    /**
     * Save a specific property to the properties file.
     * This updates both the global properties and the file.
     * 
     * @param key Property key
     * @param value Property value
     */
    private static synchronized void savePropertyToFile(String key, String value) {
        try {
            // Get the properties file path from classpath (points to target/test-classes)
            String classpathPath = VariableManager.class.getClassLoader()
                    .getResource(PROPERTIES_FILE).getPath();
            
            // Handle Windows paths (remove leading slash if present)
            if (classpathPath.startsWith("/") && classpathPath.contains(":")) {
                classpathPath = classpathPath.substring(1);
            }
            
            // Convert target path to source path
            // Example: c:/project/target/test-classes/file -> c:/project/src/test/resources/file
            String propertiesPath = classpathPath.replace("target\\test-classes", "src\\test\\resources")
                                                  .replace("target/test-classes", "src/test/resources");
            
            java.io.File file = new java.io.File(propertiesPath);
            
            // If source file doesn't exist, fall back to target file (shouldn't happen in normal cases)
            if (!file.exists()) {
                logger.warn("Source properties file not found at {}, using classpath location", propertiesPath);
                file = new java.io.File(classpathPath);
            }
            
            // Load current properties
            Properties props = new Properties();
            try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
                props.load(in);
            }
            
            // Update the property
            props.setProperty(key, value);
            globalProperties.setProperty(key, value);
            
            // Save back to file
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
                props.store(out, "Test Variables Configuration - Updated by VariableManager");
            }
            
            logger.info("Successfully saved {} to {}", key, file.getAbsolutePath());
            
        } catch (Exception e) {
            logger.error("Failed to save property {} to file: {}", key, e.getMessage());
            // Don't throw exception - just log the error
        }
    }


    /**
     * Get the Bizup base URL.
     * 
     * @return Bizup base URL
     */
    public static String getBizupBaseUrl() {
        return get("bizup_base");
    }

    /**
     * Get the Navo base URL.
     * 
     * @return Navo base URL
     */
    public static String getNavoBaseUrl() {
        return get("navo_base");
    }

    /**
     * Get the phone number for testing.
     * 
     * @return Phone number
     */
    public static String getPhoneNumber() {
        return get("phoneNumber");
    }

    /**
     * Get the response timeout.
     * 
     * @return Response timeout in milliseconds
     */
    public static int getResponseTimeout() {
        return getInt("res_time", 40000);
    }

    /**
     * Get the seller ID.
     * 
     * @return Seller ID
     */
    public static String getSellerId() {
        return get("seller_id");
    }

    /**
     * Get the search seller ID.
     * 
     * @return Search seller ID
     */
    public static String getSearchSellerId() {
        return get("search_seller_id");
    }

    /**
     * Get the product ID.
     * 
     * @return Product ID
     */
    public static String getProductId() {
        return get("product_id");
    }

    /**
     * Get the search product ID.
     * 
     * @return Search product ID
     */
    public static String getSearchProductId() {
        return get("search_product_id");
    }

    /**
     * Get the video ID.
     * 
     * @return Video ID
     */
    public static String getVideoId() {
        return get("video_id");
    }

    /**
     * Get the user ID.
     * 
     * @return User ID
     */
    public static String getUserId() {
        return get("user_id");
    }

    /**
     * Cleanup thread-local variables to prevent memory leaks.
     * This should be called after test suite completion.
     */
    public static void cleanup() {
        threadLocalVariables.remove();
        logger.debug("Thread-local variables cleaned up for thread: {}", 
            Thread.currentThread().getName());
    }

    /**
     * Get all variables as a map (for debugging purposes).
     * Returns a copy to prevent external modification.
     * 
     * @return Copy of all variables
     */
    public static Map<String, String> getAllVariables() {
        return new HashMap<>(getVariablesMap());
    }

    /**
     * Get the count of variables in current thread.
     * 
     * @return Number of variables
     */
    public static int count() {
        return getVariablesMap().size();
    }

    /**
     * Check if VariableManager has been initialized.
     * 
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
