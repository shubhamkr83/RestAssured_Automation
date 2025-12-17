# VariableManager - Usage Guide

## Overview

`VariableManager` is a thread-safe utility for managing test variables across the API automation framework. It provides centralized variable storage with support for dynamic runtime updates, making it perfect for managing tokens, IDs, and other values that change during test execution.

---

## Key Features

✅ **Thread-Safe** - Full support for parallel test execution using ThreadLocal storage  
✅ **Dynamic Updates** - Variables can be updated during test runtime  
✅ **Type Conversion** - Built-in support for String, int, and boolean types  
✅ **Convenience Methods** - Quick access to common variables like tokens and URLs  
✅ **Auto-Initialization** - Automatically loads 43 variables from `test-variables.properties`  
✅ **Memory Safe** - Automatic cleanup prevents ThreadLocal memory leaks  

---

## Quick Start

### 1. Initialization (Automatic)

VariableManager is automatically initialized in `BaseTest.beforeSuite()`. No manual initialization needed!

```java
// Automatically called by BaseTest
@BeforeSuite
public void beforeSuite() {
    VariableManager.initialize(); // ✅ Happens automatically
}
```

### 2. Reading Variables

```java
// Get String variables
String token = VariableManager.get("bomb_token");
String baseUrl = VariableManager.get("bizup_base");
String phoneNumber = VariableManager.get("phoneNumber");

// Get with default value
String tag = VariableManager.get("tag", "defaultTag");

// Get integer variables
int timeout = VariableManager.getInt("res_time");
int price = VariableManager.getInt("price");

// Get with default value
int retryCount = VariableManager.getInt("retry_count", 3);

// Get boolean variables
boolean flag = VariableManager.getBoolean("flag");
```

### 3. Updating Variables

```java
// Set/Update any variable
VariableManager.set("bomb_token", newTokenValue);
VariableManager.set("catalog_id", "12345");
VariableManager.set("price", 999);

// Update integers or other types (automatically converted to String)
VariableManager.set("res_time", 50000);
VariableManager.set("flag", true);
```

### 4. Convenience Methods

```java
// Token management
String bombToken = VariableManager.getToken();
VariableManager.setToken(newToken);

String buyerToken = VariableManager.getVariableManager.getBuyerAppToken()();
VariableManager.setVariableManager.getBuyerAppToken()(newBuyerToken);

// Base URLs
String bizupUrl = VariableManager.getBizupBaseUrl();
String navoUrl = VariableManager.getNavoBaseUrl();

// Phone number
String phone = VariableManager.getPhoneNumber();

// Response timeout
int timeout = VariableManager.getResponseTimeout(); // Default 40000ms
```

---

## Common Use Cases

### Use Case 1: Managing Authentication Tokens

```java
@Epic("BOMB Authentication")
@Feature("Login API")
public class LoginApiTest extends BaseTest {

    @Test(priority = 1)
    public void testBombLogin() {
        // Read credentials from VariableManager
        String phoneNumber = VariableManager.getPhoneNumber();
        
        LoginRequest request = LoginRequest.builder()
            .phoneNumber(phoneNumber)
            .token("123456789")
            .build();

        Response response = RestAssured.given()
            .spec(requestSpec)
            .body(request)
            .post(BombEndpoints.LOGIN);

        LoginResponse loginData = JsonUtils.fromResponse(response, LoginResponse.class);
        
        // Store the new token for other tests
        String accessToken = loginData.getData().getAccessToken();
        VariableManager.setToken(accessToken);
        
        logger.info("✅ BOMB token updated successfully");
    }
}
```

### Use Case 2: Using Tokens in Other Tests

```java
@Epic("BOMB Catalog Management")
@Feature("Catalog Search")
public class CatalogSearchTest extends BaseTest {

    @Test
    public void testSearchCatalogs() {
        // Get the token stored by LoginApiTest
        String token = VariableManager.getToken();
        String baseUrl = VariableManager.getBizupBaseUrl();
        
        Response response = RestAssured.given()
            .baseUri(baseUrl)
            .header("Authorization", "Bearer " + token)
            .queryParam("sellerId", VariableManager.get("seller_id"))
            .get("/api/catalogs/search");

        assertThat(response.getStatusCode(), equalTo(200));
    }
}
```

### Use Case 3: Dynamic ID Management

```java
@Test(priority = 1)
public void testCreateCatalog() {
    // Create a new catalog
    Response response = restClient.post(
        BombEndpoints.CREATE_CATALOG, 
        catalogRequest
    );
    
    // Extract and store the new catalog ID
    String newCatalogId = response.jsonPath().getString("data._id");
    VariableManager.set("catalog_id", newCatalogId);
    
    logger.info("Created catalog with ID: {}", newCatalogId);
}

@Test(priority = 2, dependsOnMethods = "testCreateCatalog")
public void testUpdateCatalog() {
    // Use the catalog ID from previous test
    String catalogId = VariableManager.get("catalog_id");
    
    Response response = restClient.put(
        BombEndpoints.UPDATE_CATALOG,
        Map.of("id", catalogId),
        updateRequest
    );
    
    assertThat(response.getStatusCode(), equalTo(200));
}
```

### Use Case 4: Environment-Specific Configuration

```java
@Test
public void testWithEnvironmentConfig() {
    // Read configuration values
    String baseUrl = VariableManager.get("bizup_base");
    int timeout = VariableManager.getResponseTimeout();
    
    RequestSpecification spec = RestAssured.given()
        .baseUri(baseUrl)
        .timeout(timeout, TimeUnit.MILLISECONDS);
    
    Response response = spec.get("/api/health");
    assertThat(response.getStatusCode(), equalTo(200));
}
```

### Use Case 5: Data-Driven Testing

```java
@Test
public void testProductSearch() {
    // Get test data from VariableManager
    String searchTerm = VariableManager.get("search_product");
    String suitableFor = VariableManager.get("suitable_for");
    
    Response response = RestAssured.given()
        .spec(requestSpec)
        .queryParam("query", searchTerm)
        .queryParam("suitableFor", suitableFor)
        .get(BuyerAppEndpoints.SEARCH_PRODUCTS);

    assertThat(response.getStatusCode(), equalTo(200));
    assertThat(response.jsonPath().getList("data"), not(empty()));
}
```

---

## Available Variables

All 43 variables from your Postman collection are available:

### Authentication & Tokens
- `bomb_token` - BOMB API authentication token
- `buyer_app_token` - Buyer App authentication token

### Base URLs
- `bizup_base` - Bizup API base URL (https://api.bizup.app)
- `navo_base` - Navo Fashion API base URL (https://api.navofashion.in)

### Configuration
- `res_time` - Response timeout (40000ms)
- `phoneNumber` - Test phone number

### Catalog Management
- `seller_id`, `product_id`, `price`
- `catalog_id`, `live_catalog_id`, `delete_catalog_id`
- `catalog_foassign_id`
- `catalog_pdf1`, `catalog_pdf2`, `catalog_pdf3`, `catalog_pdf4`

### Tagging & Metadata
- `tag`, `title`, `editor_id`

### Video Management
- `upload_id`, `video_id`, `video_title`

### Search & Recommendations
- `search_product`, `search_recommend`, `search_recommend_id`
- `suitable_for`

### Flags & Counters
- `flag`
- `totalSuccessfulCollections`, `totalFailedCollections`
- `totalItemsAcrossCollections`, `emptyCollectionsCount`
- And more...

See `test-variables.properties` for the complete list with descriptions.

---

## Utility Methods Reference

### Read Operations

| Method | Description | Example |
|--------|-------------|---------|
| `get(key)` | Get String value | `VariableManager.get("bomb_token")` |
| `get(key, default)` | Get String with default | `VariableManager.get("tag", "default")` |
| `getInt(key)` | Get int value | `VariableManager.getInt("price")` |
| `getInt(key, default)` | Get int with default | `VariableManager.getInt("timeout", 5000)` |
| `getBoolean(key)` | Get boolean value | `VariableManager.getBoolean("flag")` |
| `getBoolean(key, default)` | Get boolean with default | `VariableManager.getBoolean("flag", false)` |

### Write Operations

| Method | Description | Example |
|--------|-------------|---------|
| `set(key, value)` | Set any value (auto-converts to String) | `VariableManager.set("token", newToken)` |
| `setToken(value)` | Set BOMB token | `VariableManager.setToken(accessToken)` |
| `setVariableManager.getBuyerAppToken()(value)` | Set Buyer App token | `VariableManager.setVariableManager.getBuyerAppToken()(token)` |

### Convenience Methods

| Method | Description | Example |
|--------|-------------|---------|
| `getToken()` | Get BOMB token | `VariableManager.getToken()` |
| `getVariableManager.getBuyerAppToken()()` | Get Buyer App token | `VariableManager.getVariableManager.getBuyerAppToken()()` |
| `getBizupBaseUrl()` | Get Bizup base URL | `VariableManager.getBizupBaseUrl()` |
| `getNavoBaseUrl()` | Get Navo base URL | `VariableManager.getNavoBaseUrl()` |
| `getPhoneNumber()` | Get test phone number | `VariableManager.getPhoneNumber()` |
| `getResponseTimeout()` | Get response timeout | `VariableManager.getResponseTimeout()` |

### Utility Methods

| Method | Description | Example |
|--------|-------------|---------|
| `has(key)` | Check if variable exists | `VariableManager.has("token")` |
| `remove(key)` | Remove a variable | `VariableManager.remove("temp_id")` |
| `clear()` | Clear all variables | `VariableManager.clear()` |
| `count()` | Get variable count | `VariableManager.count()` |
| `getAllVariables()` | Get all variables as Map | `VariableManager.getAllVariables()` |

---

## Migration from Static Variables

### ❌ Old Pattern (Static Variables)

```java
public class LoginApiTest {
    public static String bombToken; // Not thread-safe!
    
    @Test
    public void testLogin() {
        bombToken = response.getData().getAccessToken();
    }
}

public class OtherTest {
    @Test
    public void testSomething() {
        String token = LoginApiTest.bombToken; // Dependency on another class
    }
}
```

### ✅ New Pattern (VariableManager)

```java
public class LoginApiTest {
    @Test
    public void testLogin() {
        String token = response.getData().getAccessToken();
        VariableManager.setToken(token); // Thread-safe!
    }
}

public class OtherTest {
    @Test
    public void testSomething() {
        String token = VariableManager.getToken(); // No class dependency
    }
}
```

**Benefits:**
- ✅ Thread-safe for parallel execution
- ✅ No cross-class dependencies
- ✅ Centralized variable management
- ✅ Easier to debug and maintain

---

## Thread Safety & Parallel Execution

VariableManager uses `ThreadLocal` storage, meaning:

- ✅ Each thread has its own copy of variables
- ✅ No race conditions or thread interference
- ✅ Perfect for parallel test execution
- ✅ Automatic cleanup prevents memory leaks

```java
// TestNG parallel execution example
// Each thread maintains separate variable state
@Test(threadPoolSize = 3, invocationCount = 10)
public void testParallelExecution() {
    String token = VariableManager.getToken();
    // Each thread has its own token value
    // No conflicts!
}
```

---

## Best Practices

### ✅ Do's

1. **Use VariableManager for dynamic values** (tokens, IDs generated at runtime)
2. **Use ConfigManager for static config** (base URLs, timeouts from config.properties)
3. **Update tokens after login tests** to keep them fresh
4. **Use convenience methods** (`getToken()` instead of `get("bomb_token")`)
5. **Check variable existence** with `has()` before critical operations
6. **Use type-specific getters** (`getInt()`, `getBoolean()`) for proper type conversion

### ❌ Don'ts

1. **Don't modify `test-variables.properties` during test execution** (read-only)
2. **Don't forget to initialize** VariableManager (but it's automatic in BaseTest)
3. **Don't manually call `cleanup()`** (automatic in BaseTest.afterSuite)
4. **Don't use for large binary data** (designed for strings, IDs, tokens)

---

## Troubleshooting

### Issue: Variable not found

```java
// ❌ Will return null
String value = VariableManager.get("typo_key");

// ✅ Safe way with default
String value = VariableManager.get("typo_key", "defaultValue");

// ✅ Or check first
if (VariableManager.has("key")) {
    String value = VariableManager.get("key");
}
```

### Issue: NumberFormatException

```java
// ❌ Variable contains non-numeric value
int value = VariableManager.getInt("non_numeric_key"); // Throws exception

// ✅ Use default value
int value = VariableManager.getInt("non_numeric_key", 0);
```

### Issue: Variables not initialized

```java
// Check initialization status
if (!VariableManager.isInitialized()) {
    VariableManager.initialize();
}
```

---

## Advanced Usage

### Debugging Variables

```java
// Get all variables for debugging
Map<String, String> allVars = VariableManager.getAllVariables();
logger.debug("Current variables: {}", allVars);

// Get variable count
int count = VariableManager.count();
logger.info("Total variables: {}", count);
```

### Conditional Variable Updates

```java
// Only update if token expired or missing
if (!VariableManager.has("bomb_token") || isTokenExpired()) {
    performLogin();
    VariableManager.setToken(newToken);
}
```

### Temporary Variables

```java
// Set temporary variable for test session
VariableManager.set("temp_catalog_id", catalogId);

// Use it in other tests
String tempId = VariableManager.get("temp_catalog_id");

// Clean up when done
VariableManager.remove("temp_catalog_id");
```

---

## Summary

`VariableManager` provides a powerful, thread-safe solution for managing dynamic test variables. It seamlessly integrates with your existing framework and supports all common use cases for API automation testing.

**Key Takeaways:**
- 🎯 Automatically initialized with 43 variables from `test-variables.properties`
- 🔒 Thread-safe using ThreadLocal storage
- 🚀 Perfect for managing tokens, IDs, and dynamic test data
- 📦 Ready to use - no manual setup required
- 🧹 Automatic cleanup prevents memory leaks

Happy Testing! 🚀
