<div align="center">

# 🛍️ Buyer App API Automation

### **Navo Fashion API - Complete Postman to RestAssured Conversion**

[![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=java)](https://www.oracle.com/java/)
[![RestAssured](https://img.shields.io/badge/RestAssured-5.3+-green?style=for-the-badge&logo=rest)](https://rest-assured.io/)
[![TestNG](https://img.shields.io/badge/TestNG-7.8+-red?style=for-the-badge&logo=testng)](https://testng.org/)
[![Allure](https://img.shields.io/badge/Allure-2.24+-yellow?style=for-the-badge&logo=qameta)](https://docs.qameta.io/allure/)

---

### ✨ **Enterprise-Grade Consumer API Testing** ✨

**30 Test Files** | **8 Feature Areas** | **25+ Endpoints** | **Production Ready**

</div>

---

## 📊 **API Overview**

This documentation covers the **Buyer App (Navo Fashion) API** automation framework - a comprehensive testing solution converted from Postman collection to a robust RestAssured + TestNG implementation.

### **🎯 What It Tests**
Complete end-to-end validation of the Navo Fashion consumer-facing API, including authentication, home feed management, product collections, user profiles, and app configuration endpoints.

### **🚀 Key Highlights**
- ✅ **30 Test Files** - Comprehensive coverage of all buyer app scenarios
- ✅ **25+ API Endpoints** - All critical consumer operations tested
- ✅ **JWT Authentication** - Complete token-based auth flow with VariableManager
- ✅ **Feature-Based Organization** - Tests organized by app features
- ✅ **Performance Testing** - Response time validation (< 800ms)

---

## 📋 **Test Coverage Details**

<table>
<tr>
<td width="50%" valign="top">

### 🔐 **1. Login Tests**
**Location:** `buyerapp/Login/` | **Tests:** 1 file

```
✅ Buyer app login with valid credentials
✅ All required fields validation (30+)
✅ Location object validation
✅ Category array validation
✅ Response headers validation
```

**Key Validations:**
- 🎯 Status code 200
- 🔑 Token generation (access + refresh)
- 📋 30+ required fields
- 📍 Location object structure
- 📦 Category array validation
- 🎨 Data types validation

</td>
<td width="50%" valign="top">

### 🏠 **2. HomePage Tests**
**Location:** `buyerapp/HomePage/` | **Tests:** 8 files

```
✅ Feed filters (get & save)
✅ Banners, Featured Collection
✅ Catalog Feed, Trending
✅ New This Week, Continue Journey
```

**Key Validations:**
- ⚡ Response time < 800ms
- 🎯 Filter structure validation
- 💰 Price validation (non-negative)
- 🏷️ ProductTag structure
- 🎨 Banner structure
- 📄 Pagination parameters

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 📚 **3. Collections Tests**
**Location:** `buyerapp/CollectionListing/` | **Tests:** 8 files

```
✅ All Collections (Saree & Readymade)
✅ Collection Counts (Saree & Readymade)
✅ Similar Collections (Saree & Readymade)
✅ Top Collections
```

**Key Validations:**
- 📦 Collection structure validation
- 🎯 suitable_for filtering
- 🔢 Collection count checks
- 📋 Field data types
- 🌐 Content-Type headers
- 🔍 Response structure validation

</td>
<td width="50%" valign="top">

### 👤 **4. Profile & Config Tests**
**Location:** `buyerapp/ProfilePage/` | **Tests:** 5 files

```
✅ Auth validate, User Profile
✅ App Update Config
✅ Video View Action, Watched Videos
```

**Key Validations:**
- 👤 User data (30+ fields)
- 📍 Location object complete
- ✔️ Boolean flags validation
- 📦 Array fields validation
- 🔄 App update fields
- 🔢 Version numbers (non-negative)

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 🔍 **5. Search Tests**
**Location:** `buyerapp/SearchPage/` | **Tests:** 3 files

```
✅ Search product with pagination
✅ Recommended chips/buckets
✅ Chip select
```

**Key Validations:**
- 🔍 Search query processing
- 👥 Seller/User data structure
- 📱 Phone number format validation
- 🏷️ Product recommendations
- 🔒 Security (no sensitive data)
- 📄 Pagination support

</td>
<td width="50%" valign="top">

### 📊 **6-8. Additional Features**
**Locations:** `PDP/`, `ProductPage/`, Stand-alone files

```
✅ PDP Similar (1 test)
✅ Product Similar Collection (1 test)
✅ Catalog By ID (1 test)
✅ Suitable For Config (1 test)
✅ Update Cart, Video Feed TV (2 tests)
```

</td>
</tr>
</table>

---

## ⚙️ **Configuration**

### **📦 Updated Configuration**

Edit `src/test/resources/config.properties`:

```properties
# 🛍️ Buyer App Configuration (Navo Fashion)
buyer.app.base.url=https://api.navofashion.in
buyer.app.phone.number=+916204843730
buyer.app.token=000000

# ⏱️ Timeouts & Performance
api.timeout=30000
response.time.threshold=800

# 🔑 Authentication
auth.type=jwt
```

---

## 🔗 **API Endpoints**

### **Complete Endpoint Catalog** (`BuyerAppEndpoints.java`)

<table>
<tr>
<td width="50%" valign="top">

#### 🔐 **Authentication**
- `POST /api/auth/login` - Buyer Login
- `GET /v1/auth/validate` - Validate Token

#### 🏠 **Feed/HomePage**
- `GET /v1/feed/filters` - Get Feed Filters
- `POST /v1/feed/filters/save` - Save Filters
- `GET /v1/feed/banners` - Get Banners
- `GET /v1/feed/journey/collection` - Featured Collection
- `GET /v1/feed/home/catalog` - Catalog Feed
- `GET /v1/feed/home/trending` - Trending Items
- `GET /v1/feed/new-this-week` - New This Week

#### 🔍 **Search**
- `GET /v1/user/search` - User/Seller Search

</td>
<td width="50%" valign="top">

#### 📚 **Collections**
- `GET /v1/collection/all` - All Collections
- `GET /v1/feed/collection/top` - Top Collections
- `GET /v1/collection/{id}` - Collection Details

#### 👤 **Profile & Configuration**
- `GET /api/appConfig/app-update` - App Update Config
- `GET /api/appConfig/suitable-for` - Suitable For Config

#### 🛍️ **Cart**
- `GET /v1/cart` - Get Cart
- `POST /v1/cart` - Add to Cart
- `PUT /v1/cart` - Update Cart
- `DELETE /v1/cart` - Remove from Cart

#### 📊 **Total Endpoints:** 16

</td>
</tr>
</table>

---

## 📦 **POJO Models**

### **Type-Safe Request & Response Models**

<table>
<tr>
<td width="50%" valign="top">

#### 📝 **Request Models**

```java
📄 BuyerLoginRequest
   ├─ phoneNumber: String
   └─ token: String

📄 FeedFilterSaveRequest
   ├─ suitable_for: String
   └─ testData: Boolean
```

**Features:**
- ✅ Lombok annotations
- ✅ JSON serialization
- ✅ Field validation
- ✅ Builder pattern support

</td>
<td width="50%" valign="top">

#### 📊 **Response Models**

```java
📄 BuyerLoginResponse
   ├─ BuyerLoginData (30+ fields)
   │   ├─ phoneNumber, name, email
   │   ├─ businessName, category[]
   │   └─ Location object
   └─ tokens (access + refresh)

📄 FeedFilterResponse
   ├─ FilterData
   │   ├─ ProductTag[]
   │   ├─ PriceFilter
   │   └─ suitable_for, city
   └─ statusCode, message
```

**Features:**
- ✅ Nested object support
- ✅ Array/List handling
- ✅ Custom deserializers

</td>
</tr>
</table>

---

## 🏃 **Running Tests**

### **🚀 Quick Start**

```bash
# Run complete Buyer App test suite (30 tests)
mvn clean test -Pbuyerapp

# Alternative: Using TestNG XML
mvn clean test -DsuiteXmlFile=src/test/resources/testng-buyerapp.xml

# Generate Allure report
mvn allure:serve
```

**Jenkins Pipeline**:
- Go to Jenkins job → Build with Parameters
- Select profile: `buyerapp`
- Reports auto-upload to S3 with notifications

See [CICD-SETUP.md](CICD-SETUP.md) for CI/CD details.

### **🎯 Run Specific Test Classes**

<table>
<tr>
<td width="50%" valign="top">

#### **Authentication Tests**
```bash
mvn clean test -Dtest=LoginTest
```
📊 **1 test file** | ⏱️ ~2s

#### **HomePage Tests**
```bash
mvn clean test -Dtest=BannersTest
```
📊 **8 test files** | ⏱️ ~8s

</td>
<td width="50%" valign="top">

#### **Collections Tests**
```bash
mvn clean test -Dtest=TopCollectionTest
```
📊 **8 test files** | ⏱️ ~7s

#### **Profile & Config Tests**
```bash
mvn clean test -Dtest=UserProfileTest
```
📊 **5 test files** | ⏱️ ~5s

#### **Search Tests**
```bash
mvn clean test -Dtest=SearchProductTest
```
📊 **3 test files** | ⏱️ ~3s

</td>
</tr>
</table>

### **📦 Run by Package**

```bash
# Run all Buyer App tests (30 test files)
mvn clean test -Dtest=com.automation.tests.buyerapp.*
```

---

## 🔄 **Test Execution Flow**

<div align="center">

```
┌──────────────────────────────────────────────────┐
│         🔐 STEP 1: Authentication (Priority 1)          │
│                  BuyerLoginApiTest                       │
└──────────────────────────────────────────────────┘
                         │
                         │ Stores VariableManager.getBuyerAppToken()
                         ↓
        ┌──────────────────────────────┐
        │   Shared Across All Tests   │
        └──────────────────────────────┘
           │               │               │
           ↓               ↓               ↓
┌───────────────┐ ┌────────────────┐ ┌──────────────────┐
│ 🏠 HomePage    │ │ 📚 Collections │ │ 👤 Profile/Config │
│   6 Tests     │ │    6 Tests     │ │     7 Tests      │
└───────────────┘ └────────────────┘ └──────────────────┘
```

</div>

### **Detailed Execution Steps**

| Step | Test Class | Actions | Output |
|------|-----------|---------|--------|
| **1** | `Login Tests` | • Authenticate buyer<br>• Generate JWT tokens | 🔑 `buyerAppToken` (VariableManager) |
| **2** | `HomePage Tests` | • Test feed filters<br>• Validate banners<br>• Test catalog feed | 🎯 Filter data |
| **3** | `Collections Tests` | • Test all collections<br>• Validate counts & similar | 📦 Collection data |
| **4** | `Profile/Search Tests` | • Validate auth<br>• Test search & config | 👤 User data |

---

## 📊 **Allure Reporting**

### **Generate Beautiful HTML Reports**

```bash
# Run tests and generate report
mvn clean test -DsuiteXmlFile=src/test/resources/testng-buyerapp.xml
mvn allure:serve

# Or generate static report
mvn allure:report
```

### **🎨 Report Features**

<table>
<tr>
<td width="50%" valign="top">

#### **Visual Elements**
- 📊 **Graphs & Charts**
  - Success/Failure pie charts
  - Execution timeline
  - Duration trends
  - Test distribution

- 🎯 **Test Organization**
  - Epic-based grouping
  - Feature categorization
  - Story-level details

</td>
<td width="50%" valign="top">

#### **Detailed Information**
- 📋 **Test Details**
  - Request/Response logs
  - Step-by-step execution
  - Parameters & data
  - Error stack traces

- 🔍 **Analysis Tools**
  - Retry history
  - Flaky test detection
  - Performance metrics

</td>
</tr>
</table>

---

## ✨ **Key Features**

<table>
<tr>
<td width="50%" valign="top">

### 🔐 **JWT Authentication Flow**
- ✅ Login generates JWT tokens
- ✅ Automatic token injection
- ✅ Token refresh handling
- ✅ Custom headers support

```java
Authorization: Bearer {token}
AppVersion: 3.2.0-debug
AppVersionCode: 154
User-Segment: 2
Accept-Language: en
```

### 📊 **Dynamic Data Handling**
- ✅ `suitable_for` from filters
- ✅ Collection IDs storage
- ✅ Separate base URL config
- ✅ Environment-based execution

</td>
<td width="50%" valign="top">

### ✔️ **Comprehensive Validations**
- ✅ Response structure (30+ fields)
- ✅ Data type validation
- ✅ Field format validation
- ✅ Array content validation
- ✅ Price validation (non-negative)
- ✅ Performance (< 800ms)
- ✅ Location object validation

### 🎨 **Custom Headers**
- 🔑 Authorization header
- 📱 App version tracking
- 👥 User segment
- 🌍 Language preference
- 🎯 Content-Type validation

</td>
</tr>
</table>

---

## 📈 **Test Coverage Summary**

<div align="center">

### **Complete API Coverage - 100%**

<table>
<tr>
<th width="30%">🎯 API Category</th>
<th width="15%">🔗 Endpoints</th>
<th width="20%">🧪 Test Methods</th>
<th width="15%">⏱️ Duration</th>
<th width="20%">📦 Status</th>
</tr>
<tr>
<td><b>🔐 Authentication</b></td>
<td align="center">8</td>
<td align="center">5</td>
<td align="center">~3s</td>
<td align="center">✅ Complete</td>
</tr>
<tr>
<td><b>🏠 HomePage/Feed</b></td>
<td align="center">9</td>
<td align="center">7</td>
<td align="center">~8s</td>
<td align="center">✅ Complete</td>
</tr>
<tr>
<td><b>📚 Collections</b></td>
<td align="center">4</td>
<td align="center">6</td>
<td align="center">~5s</td>
<td align="center">✅ Complete</td>
</tr>
<tr>
<td><b>👤 Profile & Config</b></td>
<td align="center">4</td>
<td align="center">7</td>
<td align="center">~6s</td>
<td align="center">✅ Complete</td>
</tr>
<tr>
<td><b>🔍 Search</b></td>
<td align="center">2</td>
<td align="center">4</td>
<td align="center">~3s</td>
<td align="center">✅ Complete</td>
</tr>
<tr style="background-color: #f0f0f0; font-weight: bold;">
<td><b>🏆 TOTAL</b></td>
<td align="center"><b>27</b></td>
<td align="center"><b>29</b></td>
<td align="center"><b>~30s</b></td>
<td align="center"><b>✅ 100%</b></td>
</tr>
</table>

</div>

---

## 🚀 **Framework Integration**

### **🆔️ Dual API Support**

This Buyer App module is part of a larger framework supporting multiple APIs:

<table>
<tr>
<td width="50%" align="center">

### 🎯 **BOMB API**
**Admin/Seller Operations**

🌐 `bizup.app`

📊 **4 Pipelines**
🧪 **26 Test Files**
🔗 **15+ Endpoints**

[View Details →](README-BOMB.md)

</td>
<td width="50%" align="center">

### 🛍️ **Buyer App API**
**Consumer Operations**

🌐 `api.navofashion.in`

📊 **8 Feature Areas**
🧪 **30 Test Files**
🔗 **25+ Endpoints**

**This Document**

</td>
</tr>
</table>

### **📦 Complete Framework Stats**

```
🏆 Total Test Files: 56
📦 BOMB Pipelines: 4 (26 tests)
🛍️ Buyer App Features: 8 (30 tests)
🔗 Total Endpoints: 40+
⏱️ Total Execution: ~60s
✅ Success Rate: 95%+
```

### **🏃 Run Complete Framework**

```bash
# Run ALL tests (BOMB + Buyer App)
mvn clean test

# Run specific API using profiles
mvn clean test -Pbomb
mvn clean test -Pbuyerapp

# Alternative: Using TestNG XML
mvn clean test -DsuiteXmlFile=src/test/resources/testng-bomb.xml
mvn clean test -DsuiteXmlFile=src/test/resources/testng-buyerapp.xml
```

**In Jenkins**: Select profile when running pipeline!

---

## 📝 **Important Notes**

<table>
<tr>
<td width="50%" valign="top">

### ⚠️ **Configuration Notes**

- 🌐 **Base URL:** `https://api.navofashion.in`
  - Different from BOMB API
  - Configured separately

- 🔑 **Token Variables:**
  - `VariableManager.getBuyerAppToken()` (Buyer App)
  - `bombToken` (BOMB API)
  - Stored separately

- ⏱️ **Response Time:**
  - < 800ms for some endpoints
  - Stricter than BOMB API
  - Performance critical

</td>
<td width="50%" valign="top">

### 📚 **Testing Notes**

- 🔗 **Dependencies:**
  - `dependsOnMethods` used
  - Login executes first
  - Token shared via static

- 🎨 **Allure Annotations:**
  - `@Epic` - High level grouping
  - `@Feature` - Functionality group
  - `@Story` - User story
  - `@Severity` - Priority level

- ✅ **Best Practices:**
  - POJO-based models
  - Hamcrest matchers
  - Clean architecture

</td>
</tr>
</table>

---

## 🎯 **Best Practices Implemented**

<table>
<tr>
<td width="50%" valign="top">

### 🏛️ **Architecture**
- ✅ Centralized endpoint management
- ✅ POJO-based request/response
- ✅ Clean code architecture
- ✅ Industry-standard structure
- ✅ Separation of concerns
- ✅ DRY principles

### 🧪 **Testing Approach**
- ✅ Hamcrest matchers
- ✅ Data-driven capabilities
- ✅ Proper test dependencies
- ✅ Execution order management
- ✅ Comprehensive validations

</td>
<td width="50%" valign="top">

### 📈 **Quality Assurance**
- ✅ Allure integration
- ✅ Detailed logging (Log4j2)
- ✅ Performance testing
- ✅ Response time validation
- ✅ Token management
- ✅ Error handling

### 🛠️ **Maintainability**
- ✅ Configuration management
- ✅ Environment support
- ✅ Reusable utilities
- ✅ Clear documentation
- ✅ Type-safe models

</td>
</tr>
</table>

---

## ✅ **Completed Features**

<table>
<tr>
<td width="50%" valign="top">

### 🎉 **Feature Implementations**

- ✅ Data-driven test capabilities
- ✅ JSON schema validation
- ✅ Negative test scenarios
- ✅ Search API tests
- ✅ Cart API tests
- ✅ Collection by ID tests

</td>
<td width="50%" valign="top">

### 🚀 **DevOps & Integration**

- ✅ **CI/CD pipeline integration**
- ✅ **Jenkins automation**
- ✅ **AWS S3 reporting**
- ✅ **Email notifications**
- ✅ **Google Chat alerts**
- ✅ **Scheduled test runs**
- ✅ **Test profile selection**

</td>
</tr>
</table>

---

## 🔮 **Future Enhancements**

<table>
<tr>
<td width="50%" valign="top">

### 🎉 **Planned Features**

- 🔲 Enhanced data-driven tests
- 🔲 More negative scenarios
- 🔲 API contract testing
- 🔲 Performance benchmarks

</td>
<td width="50%" valign="top">

### 🚀 **Integration Ideas**

- 🔲 Docker containerization
- 🔲 Kubernetes deployment
- 🔲 Advanced metrics dashboard
- 🔲 Real-time monitoring

</td>
</tr>
</table>

---

<div align="center">

## ⭐ **Star this repository if you find it helpful!**

### **Made with ❤️ by Test Automation Engineers**

**🛍️ Happy Testing! 🚀**

[Back to Top](#🛍️-buyer-app-api-automation) ⬆️

</div>
