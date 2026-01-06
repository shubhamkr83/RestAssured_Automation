<div align="center">

# 🚀 API Automation Framework

### **Enterprise-Grade REST API Testing with RestAssured + TestNG**

[![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=java)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-C71A36?style=for-the-badge&logo=apache-maven)](https://maven.apache.org/)
[![RestAssured](https://img.shields.io/badge/RestAssured-5.3+-green?style=for-the-badge&logo=rest)](https://rest-assured.io/)
[![TestNG](https://img.shields.io/badge/TestNG-7.8+-red?style=for-the-badge&logo=testng)](https://testng.org/)
[![Allure](https://img.shields.io/badge/Allure-2.24+-yellow?style=for-the-badge&logo=qameta)](https://docs.qameta.io/allure/)

---

### **Complete Postman to RestAssured Conversion** 

**56 Test Files** | **4 BOMB Pipelines** | **8 Buyer App Features** | **Production Ready**

</div>

---

## **Project Summary**

This is an **enterprise-grade REST API automation framework** built from the ground up to provide comprehensive testing capabilities for modern web APIs. The framework successfully converts a complete Postman collection into a robust, maintainable, and scalable Java-based testing solution.

### **What It Does**
A production-ready automation framework that tests **two major API systems** (BOMB API for admin/seller operations and Buyer App API for consumer operations), providing end-to-end validation of authentication, data management, search functionality, and business workflows.

### **Why This Framework**
- **Type-Safe Testing** - Compile-time validation with POJOs
- **Beautiful Reports** - Interactive Allure HTML reports with detailed insights
- **Maintainable Code** - Clean architecture with reusable components
- **CI/CD Ready** - Maven-based execution for easy Jenkins/GitLab integration
- **Comprehensive Coverage** - 45 test methods covering critical business scenarios

### **Key Achievements**
- **100% Postman Conversion** - All requests, validations, and test scripts migrated
- **Dual API Support** - **BOMB API:** 26 test files across 4 pipelines
- **Buyer App API:** 30 test files across 8 feature areas
- **Total:** 56 test files, organized by workflow pipelines
- **Advanced Features** - JWT authentication, VariableManager for data, retry mechanism
- **Production Quality** - Industry best practices, design patterns, and clean code

> **Perfect for:** QA Engineers, Test Automation Engineers, DevOps teams looking for a reference implementation of API testing with RestAssured + TestNG + Allure.

---

## �📊 **Framework Coverage**

<table>
<tr>
<td width="50%" valign="top">

### 🎯 **BOMB API** (Admin/Seller)
```
✅ Login Pipeline
✅ Catalog Search (6 tests)
✅ Catalog Tag Pipeline (12 tests)
✅ Video Tagging Pipeline (7 tests)

📈 4 Pipelines
📝 26 Test Files
🔗 15+ Endpoints
```

</td>
<td width="50%" valign="top">

### 🛍️ **Buyer App API** (Consumer)
```
✅ Login & Authentication
✅ HomePage (8 tests)
✅ Collections (8 tests)
✅ Search (3 tests)
✅ Profile & Config (5 tests)

📈 8 Feature Areas
📝 30 Test Files
🔗 25+ Endpoints
```

</td>
</tr>
</table>

---

## 📁 **Project Structure**

```
📦 api_automation_restassured/
│
├── 📄 pom.xml                                   # Maven Build Configuration
├── 📄 README.md                                 # This Document
├── 📄 .gitignore                                # Git Ignore Rules
│
├── 📂 src/main/java/com/automation/            # Main Source Code
│   │
│   ├── 📂 base/                                 # 🏗️ Base Classes
│   │   └── 📄 BaseTest.java                     # Base test with common setup
│   │
│   ├── 📂 config/                               # ⚙️ Configuration Management
│   │   └── 📄 ConfigManager.java                # Owner-based config loader
│   │
│   ├── 📂 constants/                            # 🔗 API Constants
│   │   ├── 📄 BombEndpoints.java                # BOMB API endpoints
│   │   ├── 📄 BuyerAppEndpoints.java            # Buyer App endpoints
│   │   ├── 📄 Endpoints.java                    # Sample endpoints
│   │   └── 📄 HttpStatus.java                   # HTTP status codes
│   │
│   ├── 📂 listeners/                            # 🎧 Test Listeners
│   │   ├── 📄 TestListener.java                 # TestNG lifecycle listener
│   │   └── 📄 RetryAnalyzer.java                # Flaky test retry mechanism
│   │
│   ├── 📂 models/                               # 📋 POJOs
│   │   ├── 📂 request/                          # Request Bodies
│   │   │   ├── 📄 LoginRequest.java             # BOMB login
│   │   │   ├── 📄 BuyerLoginRequest.java        # Buyer login
│   │   │   ├── 📄 VideoTitleRequest.java        # Video title generation
│   │   │   ├── 📄 FeedFilterSaveRequest.java    # Feed filter save
│   │   │   ├── 📄 CreateUserRequest.java        # Sample user
│   │   │   └── 📄 CreatePostRequest.java        # Sample post
│   │   │
│   │   └── 📂 response/                         # Response Bodies
│   │       ├── 📄 LoginResponse.java            # BOMB login response
│   │       ├── 📄 BuyerLoginResponse.java       # Buyer login response
│   │       ├── 📄 CatalogResponse.java          # Catalog search response
│   │       ├── 📄 FeedFilterResponse.java       # Feed filter response
│   │       ├── 📄 UserResponse.java             # Sample user
│   │       └── 📄 PostResponse.java             # Sample post
│   │
│   └── 📂 utils/                                # 🛠️ Utilities
│       ├── 📄 RestClient.java                   # HTTP client wrapper
│       ├── 📄 JsonUtils.java                    # JSON serialization
│       └── 📄 DataGenerator.java                # Test data generator
│
├── 📂 src/test/                                 # Test Source Code
│   │
│   ├── 📂 java/com/automation/tests/            # 🧪 Test Classes
│   │   │
│   │   ├── 📂 bomb/                             # BOMB API Tests
│   │   │   ├── 📄 LoginApiTest.java             # 3 tests
│   │   │   ├── 📄 CatalogSearchApiTest.java     # 6 tests
│   │   │   ├── 📄 CatalogAssignApiTest.java     # 3 tests
│   │   │   └── 📄 VideoApiTest.java             # 4 tests
│   │   │
│   │   ├── 📂 buyerapp/                         # Buyer App Tests
│   │   │   ├── 📄 BuyerLoginApiTest.java        # 5 tests
│   │   │   ├── 📄 HomePageApiTest.java          # 6 tests
│   │   │   ├── 📄 CollectionsApiTest.java       # 6 tests
│   │   │   └── 📄 ProfileAndConfigApiTest.java  # 7 tests
│   │   │
│   │   ├── 📄 UserApiTest.java                  # Sample tests
│   │   ├── 📄 PostApiTest.java                  # Sample tests
│   │   └── 📄 SchemaValidationTest.java         # Schema validation
│   │
│   └── 📂 resources/                            # Test Resources
│       ├── 📄 config.properties                 # Main configuration
│       ├── 📄 testng.xml                        # Sample suite
│       ├── 📄 testng-bomb.xml                   # BOMB API suite
│       ├── 📄 testng-buyerapp.xml               # Buyer App suite
│       ├── 📄 log4j2.xml                        # Logging config
│       ├── 📄 allure.properties                 # Allure config
│       │
│       └── 📂 schemas/                          # JSON Schemas
│           ├── 📄 user-schema.json
│           ├── 📄 post-schema.json
│           └── 📄 users-array-schema.json
│
└── 📂 target/                                   # Build Output (Git Ignored)
    ├── 📂 allure-results/                       # Allure raw data
    ├── 📂 allure-report/                        # Allure HTML report
    └── 📂 surefire-reports/                     # TestNG reports
```

---

## ✨ **Key Features**

<table>
<tr>
<td width="33%" valign="top">

### 🎯 **Testing Capabilities**
- ✅ REST API Testing
- ✅ JWT Authentication
- ✅ Custom Headers Support
- ✅ Query & Path Parameters
- ✅ Request/Response Validation
- ✅ JSON Schema Validation
- ✅ Data-Driven Testing
- ✅ Parallel Execution
- ✅ Retry Mechanism

</td>
<td width="33%" valign="top">

### 🛠️ **Framework Features**
- ✅ POJO-based Models
- ✅ Type-Safe Assertions
- ✅ Fluent API Design
- ✅ Configuration Management
- ✅ Environment Support
- ✅ Dynamic Data Handling
- ✅ Utility Classes
- ✅ Base Test Setup
- ✅ Clean Architecture

</td>
<td width="33%" valign="top">

### 📊 **Reporting & Logging**
- ✅ Allure HTML Reports
- ✅ TestNG Reports
- ✅ Log4j2 Logging
- ✅ Request/Response Logs
- ✅ Screenshot Capture
- ✅ Test Categorization
- ✅ Execution Timeline
- ✅ Failure Analysis
- ✅ CI/CD Integration

</td>
</tr>
</table>

---

## 🛠️ **Tech Stack**

<div align="center">

### **Core Technologies**

<table>
<tr>
<td align="center" width="20%">
<img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/java/java-original.svg" width="60" height="60" />
<br><b>Java 17</b>
<br><sub>Programming Language</sub>
</td>
<td align="center" width="20%">
<img src="https://maven.apache.org/images/maven-logo-black-on-white.png" width="100" height="60" />
<br><b>Maven</b>
<br><sub>Build Tool</sub>
</td>
<td align="center" width="20%">
<img src="https://rest-assured.io/img/logo-transparent.png" width="100" height="60" />
<br><b>RestAssured</b>
<br><sub>API Testing</sub>
</td>
<td align="center" width="20%">
<img src="https://testng.org/images/testng.png" width="60" height="60" />
<br><b>TestNG</b>
<br><sub>Test Framework</sub>
</td>
<td align="center" width="20%">
<img src="https://avatars.githubusercontent.com/u/5879127?s=200&v=4" width="60" height="60" />
<br><b>Allure</b>
<br><sub>Reporting</sub>
</td>
</tr>
</table>

### **Supporting Libraries**

| Library | Version | Purpose |
|---------|---------|---------|
| **Hamcrest** | 2.2 | Expressive Matchers & Assertions |
| **Jackson** | 2.15.2 | JSON Serialization/Deserialization |
| **Lombok** | 1.18.28 | Boilerplate Code Reduction |
| **Log4j2** | 2.20.0 | Advanced Logging Framework |
| **Owner** | 1.0.12 | Configuration Management |
| **JSON Path** | 2.8.0 | JSON Query & Parsing |
| **JSON Schema Validator** | 5.3.0 | API Contract Testing |

</div>

## 📋 **Prerequisites**

<table>
<tr>
<td align="center" width="33%">
<img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/java/java-original.svg" width="80"/>
<br><b>Java 17+</b>
<br><sub>JDK Required</sub>
</td>
<td align="center" width="33%">
<img src="https://maven.apache.org/images/maven-logo-black-on-white.png" width="120"/>
<br><b>Maven 3.8+</b>
<br><sub>Build Tool</sub>
</td>
<td align="center" width="33%">
<img src="https://resources.jetbrains.com/storage/products/company/brand/logos/IntelliJ_IDEA_icon.svg" width="80"/>
<br><b>IntelliJ IDEA</b>
<br><sub>IDE (Recommended)</sub>
</td>
</tr>
</table>

---

## ⚙️ **Quick Start**

### **1️⃣ Clone Repository**
```bash
git clone <repository-url>
cd api_automation_restassured
```

### **2️⃣ Install Dependencies**
```bash
mvn clean install -DskipTests
```

### **3️⃣ Update Configuration** *(Optional)*
```bash
# Edit configuration file
nano src/test/resources/config.properties

# Or use your favorite editor
code src/test/resources/config.properties
```

### **4️⃣ Run Tests**
```bash
# Run all tests
mvn clean test

# Generate Allure report
mvn allure:serve
```

---

## 🏃 **Running Tests**

### **📦 Run Complete Suites**

```bash
# 🚀 All Tests (56 tests)
mvn clean test

# 🎯 BOMB API Tests Only (26 tests)
mvn clean test -Pbomb

# 🛍️ Buyer App Tests Only (30 tests)
mvn clean test -Pbuyerapp
```

**Alternative using TestNG XML files**:
```bash
# BOMB API
mvn clean test -DsuiteXmlFile=src/test/resources/testng-bomb.xml

# Buyer App
mvn clean test -DsuiteXmlFile=src/test/resources/testng-buyerapp.xml
```

### **🎯 Run Specific Test Classes**

```bash
# BOMB API Tests
mvn clean test -Dtest=LoginApiTest
mvn clean test -Dtest=AllCatalogTest
mvn clean test -Dtest=CatalogTaggingTest

# Buyer App Tests
mvn clean test -Dtest=LoginTest
mvn clean test -Dtest=BannersTest
mvn clean test -Dtest=TopCollectionTest
```

### **📂 Run by Package**

```bash
# All BOMB API tests
mvn clean test -Dtest=com.automation.tests.bomb.*

# All Buyer App tests
mvn clean test -Dtest=com.automation.tests.buyerapp.*
```

### **⚡ Parallel Execution**

```bash
# Run tests in parallel (configured in testng.xml)
mvn clean test -Dparallel=methods -DthreadCount=5
```

### **🏗️ Jenkins Pipeline**

Run tests in Jenkins with profile selection:
- Go to Jenkins job
- Click "Build with Parameters"
- Select profile: `all`, `bomb`, or `buyerapp`
- View results in Allure report (uploaded to S3)

See [CICD-SETUP.md](CICD-SETUP.md) for details.

## 📊 **Generating Reports**

### **Allure Report Generation**

```bash
# 🚀 Generate and auto-open report in browser
mvn allure:serve

# 📁 Generate report in target/allure-report/
mvn allure:report

# 🔍 Open existing report
mvn allure:open
```

### **TestNG Report**
```bash
# Generated automatically after test execution
# Location: target/surefire-reports/index.html
```

---

## 📸 **Sample Reports**

### **🎨 Allure Report Dashboard**

<div align="center">

**Overview Dashboard**
```
╔══════════════════════════════════════════════════════════════╗
║                    ALLURE TEST REPORT                        ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  Total Tests: 56              Duration: 60.0s               ║
║  ✅ Passed: 53  (95%)         ⏱️ Avg Time: 1.07s            ║
║  ❌ Failed: 3   (5%)          📊 Success Rate: 95%          ║
║  ⏭️  Skipped: 0  (0%)         🔄 Retries: 1                 ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
```

**Test Suites Overview**
| Suite | Tests | Pass | Fail | Duration |
|-------|-------|------|------|----------|
| 🎯 BOMB API Tests | 26 | 25 | 1 | 28.0s |
| 🛍️ Buyer App Tests | 30 | 28 | 2 | 32.0s |

</div>

### **📋 Report Features**

<table>
<tr>
<td width="50%" valign="top">

#### **Visual Elements**
- 📊 **Graphs & Charts**
  - Success/Failure pie charts
  - Execution timeline
  - Duration trends
  - Category distribution

- 🎯 **Test Organization**
  - Epic-based grouping
  - Feature categorization
  - Story-level details
  - Severity indicators

</td>
<td width="50%" valign="top">

#### **Detailed Information**
- 📝 **Test Details**
  - Request/Response logs
  - Step-by-step execution
  - Parameters & data
  - Error stack traces

- 🔍 **Analysis Tools**
  - Retry history
  - Flaky test detection
  - Execution trends
  - Performance metrics

</td>
</tr>
</table>

### **🎯 Report Screenshots**

```
┌─────────────────────────────────────────────────────────┐
│ 📊 OVERVIEW                                             │
├─────────────────────────────────────────────────────────┤
│ • Test execution summary with pass/fail statistics     │
│ • Interactive pie charts and graphs                    │
│ • Duration analysis and trends                         │
│ • Environment information                              │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ 📋 SUITES                                               │
├─────────────────────────────────────────────────────────┤
│ • All test suites listed                               │
│ • Individual test results                              │
│ • Execution timeline view                              │
│ • Grouped by package/feature                           │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ 📈 GRAPHS                                               │
├─────────────────────────────────────────────────────────┤
│ • Status distribution (pie chart)                      │
│ • Severity levels (bar chart)                          │
│ • Duration trends (line graph)                         │
│ • Test categories (treemap)                            │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ 📄 BEHAVIORS                                            │
├─────────────────────────────────────────────────────────┤
│ • Epic → Feature → Story hierarchy                     │
│ • Business-focused test organization                   │
│ • User story tracking                                   │
│ • Requirement mapping                                   │
└─────────────────────────────────────────────────────────┘
```

### **🔗 Accessing Reports**

After test execution:
1. **Allure Report**: `target/allure-report/index.html`
2. **TestNG Report**: `target/surefire-reports/index.html`
3. **Console Logs**: `logs/application.log`

---

## 🚀 **CI/CD Integration**

### **Automated Testing with Jenkins**

This project is fully integrated with Jenkins for automated testing and reporting!

<div align="center">

```
┌─────────────────────────────────────────────┐
│         Jenkins Pipeline Flow               │
├─────────────────────────────────────────────┤
│                                             │
│  1. Checkout Code from Git                  │
│          ↓                                  │
│  2. Run Tests (with profile selection)      │
│          ↓                                  │
│  3. Generate Allure Report                  │
│          ↓                                  │
│  4. Upload to AWS S3 (timestamped)          │
│          ↓                                  │
│  5. Send Notifications (Email + Chat)       │
│                                             │
└─────────────────────────────────────────────┘
```

</div>

### **✨ Features**

<table>
<tr>
<td width="50%" valign="top">

#### **🎯 Test Profiles**
Run specific test suites:
- `all` - Both BOMB + Buyer App
- `bomb` - BOMB API only
- `buyerapp` - Buyer App only

```bash
# Maven profiles
mvn clean test -Pbomb
mvn clean test -Pbuyerapp
mvn clean test -Pall
```

</td>
<td width="50%" valign="top">

#### **📊 Automatic Reporting**
- ✅ Allure HTML reports
- ✅ Uploaded to AWS S3
- ✅ Timestamped folders
- ✅ Permanent links

**Example**:
`allure_2026-01-06_17_30`

</td>
</tr>
<tr>
<td width="50%" valign="top">

#### **📧 Email Notifications**
- ✅ Sent on build completion
- ✅ Success/Failure/Unstable
- ✅ Beautiful HTML format
- ✅ Quick links to reports

</td>
<td width="50%" valign="top">

#### **💬 Google Chat Alerts**
- ✅ Test summary
- ✅ Individual test results
- ✅ Build status
- ✅ Direct report links

</td>
</tr>
</table>

### **🏃 Running in Jenkins**

1. **Open Jenkins Job**
2. **Click "Build with Parameters"**
3. **Select Test Profile**:
   - `all` - Run all 56 tests
   - `bomb` - Run 26 BOMB tests
   - `buyerapp` - Run 30 Buyer App tests
4. **Click "Build"**

### **📦 What You Get**

After each build:
- 📊 **Allure Report** in S3 (timestamped)
- 📋 **Test Logs** in S3 (timestamped)
- 📄 **TestNG Results** XML file
- 📧 **Email** with results and links
- 💬 **Google Chat** notification

### **🔗 Quick Setup**

For complete setup instructions, see [CICD-SETUP.md](CICD-SETUP.md)

**Quick Start**:
```bash
# 1. Create notification config
cp scripts/notification-config.example.json scripts/notification-config.json

# 2. Edit with your details
nano scripts/notification-config.json

# 3. Setup Jenkins job pointing to this repo
# 4. Add credentials (Google Chat webhook, SMTP, AWS)
# 5. Run the pipeline!
```

---

## 🔧 Configuration

Edit `src/test/resources/config.properties`:

```properties
# Base URL
base.url=https://jsonplaceholder.typicode.com

# Authentication
auth.type=bearer
auth.token=your-api-token

# Timeouts
api.timeout=30000

# Retry
retry.count=2
```

### Authentication Types
- `none` - No authentication
- `basic` - Basic authentication (username/password)
- `bearer` - Bearer token
- `api_key` - API key in header

## 📝 Writing Tests

### Basic Test Example
```java
@Test
public void testGetUser() {
    Response response = restClient.get(Endpoints.USER_BY_ID, Map.of("id", 1));
    
    assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    
    UserResponse user = JsonUtils.fromResponse(response, UserResponse.class);
    assertThat(user.getName(), not(emptyString()));
}
```

### Data-Driven Test
```java
@DataProvider(name = "userIds")
public Object[][] userIdProvider() {
    return new Object[][]{{1}, {2}, {3}};
}

@Test(dataProvider = "userIds")
public void testGetUserDataDriven(int userId) {
    Response response = restClient.get(Endpoints.USER_BY_ID, Map.of("id", userId));
    assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
}
```

### Schema Validation
```java
@Test
public void testUserSchema() {
    Response response = restClient.get(Endpoints.USER_BY_ID, Map.of("id", 1));
    response.then()
        .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/user-schema.json"));
}
```

## 🧰 Available Utilities

### RestClient Methods
- `get(endpoint)` - GET request
- `get(endpoint, pathParams)` - GET with path params
- `getWithQueryParams(endpoint, queryParams)` - GET with query params
- `post(endpoint, body)` - POST request
- `put(endpoint, pathParams, body)` - PUT request
- `patch(endpoint, pathParams, body)` - PATCH request
- `delete(endpoint, pathParams)` - DELETE request

### JsonUtils Methods
- `toJson(object)` - Serialize to JSON
- `fromJson(json, class)` - Deserialize from JSON
- `fromResponse(response, class)` - Parse response to POJO
- `fromResponseToList(response, class)` - Parse response to List

### DataGenerator Methods
- `generateUUID()` - Random UUID
- `generateRandomString(length)` - Random string
- `generateRandomEmail()` - Random email
- `generateTimestamp()` - Current timestamp

## 📁 Adding New APIs

1. Add endpoint to `Endpoints.java`
2. Create request POJO in `models/request/`
3. Create response POJO in `models/response/`
4. Create test class in `tests/`
5. Add JSON schema in `resources/schemas/`

## 🤝 **Best Practices**

<table>
<tr>
<td width="50%" valign="top">

### **✅ Do's**
- ✔️ Use descriptive test names
- ✔️ Follow AAA pattern (Arrange, Act, Assert)
- ✔️ Use Allure annotations (`@Epic`, `@Feature`, `@Story`)
- ✔️ Keep tests independent and atomic
- ✔️ Use data providers for data-driven tests
- ✔️ Validate response structure and data types
- ✔️ Clean up test data after execution
- ✔️ Use POJOs for request/response
- ✔️ Log important test information
- ✔️ Handle exceptions gracefully

</td>
<td width="50%" valign="top">

### **❌ Don'ts**
- ❌ Don't hardcode sensitive data
- ❌ Avoid test dependencies
- ❌ Don't skip negative scenarios
- ❌ Avoid long-running tests
- ❌ Don't ignore flaky tests
- ❌ Avoid duplicate test code
- ❌ Don't commit test credentials
- ❌ Avoid magic numbers/strings
- ❌ Don't test implementation details
- ❌ Avoid excessive logging

</td>
</tr>
</table>

---

## 📚 **Additional Documentation**

| Document | Description |
|----------|-------------|
| [README-BOMB.md](README-BOMB.md) | BOMB API specific documentation |
| [README-BUYER-APP.md](README-BUYER-APP.md) | Buyer App API documentation |
| [CICD-SETUP.md](CICD-SETUP.md) | Jenkins CI/CD integration guide |


---
<div align="center">

## ⭐ **Star this repository if you find it helpful!**

### **Made with ❤️ by Test Automation Engineers**

**🛍️ Happy Testing! 🚀**

[Back to Top](#-api-automation-framework) ⬆️

</div>
