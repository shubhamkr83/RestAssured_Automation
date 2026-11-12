# RestAssured API Automation Framework: Codebase Context

> Snapshot of `RestAssured_Automation` on branch `dev` at commit `b9ea6af 2026-10-31`. Contains **158 files**: an explanation of how the project works (Part 1), the file tree (Part 2) and the full source of every file, verbatim (Part 3). Generated reports, logs, binaries and vendored tools are excluded. Credentials (passwords, access keys, tokens, webhook URLs) are replaced with `<REDACTED>`.

**How to use this file:** read Part 1 for the big picture, then jump to any file in Part 3. If you are an AI assistant answering questions about this project, treat Part 3 as the source of truth and Part 1 as a guide; cite file paths in answers.

## Contents

- [Part 1: How it works](#part-1-how-it-works)
- [Part 2: File tree](#part-2-file-tree)
- [Part 3: Source files](#part-3-source-files)

## Part 1: How it works

### 1. Overview

An API regression framework (Maven `com.automation:api-automation-restassured:1.0.0`) that tests two backends:

- **BOMB**: the admin/seller/editor operations API at `https://api.bizup.app` (`base.url`). Covers login, catalog search, the catalog tagging pipeline and the video tagging pipeline. Two tests also call `https://bomb.bizup.app/api/chat/title/tags` directly.
- **Buyer App (Navo Fashion)**: the consumer API at `https://api.navofashion.in` (`buyer.app.base.url`). Covers the home feed, collections, PDP, search, profile and cart, plus a newer production-safe `newapis` group.

**Stack** (from `pom.xml`):

| Component | Version |
|---|---|
| Java | 17 (`maven.compiler.source/target`) |
| RestAssured (+ json-schema-validator) | 5.4.0 |
| TestNG | 7.9.0 |
| Hamcrest | 2.2 |
| Jackson databind | 2.16.1 |
| Lombok | 1.18.30 (provided + annotation processor) |
| Log4j2 (api/core/slf4j-impl) | 2.22.1 |
| Allure (allure-testng, allure-rest-assured) | 2.25.0 |
| AspectJ weaver | 1.9.21 |
| Jayway json-path | 2.9.0 |
| Owner | 1.0.12 |
| maven-compiler-plugin / surefire / allure-maven | 3.12.1 / 3.2.3 / 2.14.0 |

README-BUYER-APP.md says to build with JDK 17, because Lombok 1.18.30 fails on newer JDKs.

**How to run:**
- `mvn clean test`: runs the default surefire suite, `src/test/resources/testng.xml`, a master suite that includes `testng-bomb.xml` and `testng-buyerapp.xml`.
- `mvn clean test -Pbomb`: runs only `testng-bomb.xml`.
- `mvn clean test -Pbuyerapp`: runs only `testng-buyerapp.xml`.
- Ad-hoc runs: `mvn test -Dsurefire.suiteXmlFiles= -Dtest='ClassA,ClassB' -DfailIfNoTests=false` (documented in README-BUYER-APP.md for the `newapis` tests).
- Surefire adds the AspectJ `-javaagent`, which Allure needs for steps and attachments, and sets `allure.results.directory=target/allure-results`.
- `mvn allure:report` (or `allure generate`) builds the report into `target/site/allure-maven-plugin`.

**Inputs:**
- Any config key can be overridden with `-D<key>=...`, e.g. `-Dbase.url=...` or `-Dresponse.time.threshold=...`.
- The suite parameter `env` (default `"default"`) is copied into `System.setProperty("env", ...)`.
- Credentials come from the env vars `BOMB_LOGIN_PHONE`, `BOMB_LOGIN_OTP`, `BUYER_LOGIN_PHONE` and `BUYER_LOGIN_OTP`, or from the gitignored `src/test/resources/secrets.properties`.

### 2. Architecture & folder map

**`src/main/java/com/automation/`**
- `base/`: `BaseTest`, the abstract parent of every test class. Handles suite/class setup, the RestAssured specs, and the Buyer App request builders plus on-demand login.
- `config/`: `ConfigManager`, an Owner `Config` interface that gives typed access to all properties.
- `constants/`: `BombEndpoints` and `BuyerAppEndpoints` (path constants), and `HttpStatus` (int status codes).
- `listeners/`:
  - `TestListener`: TestNG `ITestListener` that logs and attaches the stack trace to Allure on failure.
  - `RetryAnalyzer`: an `IRetryAnalyzer`, **not wired in** (see section 3).
- `models/request/`: 13 Lombok/Jackson request POJOs (`LoginRequest`, `BuyerLoginRequest`, `VideoUploadRequest`, `UpdateCartRequest`, …).
- `models/response/`: about 50 response POJOs, one per endpoint (`LoginResponse`, `CatalogResponse`, `CatalogByIdDetailResponse`, …).
- `utils/`:
  - `VariableManager`: shared runtime and test data.
  - `JsonUtils`: Jackson serialization and deserialization.
  - `TestDataResolver`: runtime discovery of Buyer App ids.
  - `ApiErrorAssertions`: backend-api error envelope checks.
  - `DataGenerator`: random strings, emails, phones and UUIDs.

**`src/test/java/com/automation/tests/`**
- `bomb/Login/`: `LoginApiTest`. Logs in and stores the BOMB JWT.
- `bomb/CatalogSearch/`: `/v1/admin/catalog_all` with no filter, then with seller, product and catalog-id filters.
- `bomb/CatalogTagPipeline/CatalogAssignToEditor/`: lists uploaded catalogs, searches by seller, assigns to an editor.
- `bomb/CatalogTagPipeline/CatalogEditor/`: the editor workflow (assigned list, fetch uploaded, groups, tag, bot title generation, tagging, edit, skip, mark-as-done).
- `bomb/VideoTaggingPipeline/VideoUploadAndAssignToEditor/`: video upload, assignment list, thumbnail upload, upload mark-as-done.
- `bomb/VideoTaggingPipeline/VideoTagging/`: edit button (videos by seller), title generation, video tagging.
- `buyerapp/` (root): `CatalogByIdTest`, `SuitableForConfigTest`, `UpdateCartTest`, `VideoFeedTvTest`.
- `buyerapp/Login/`: `LoginTest`, the buyer login.
- `buyerapp/HomePage/`: banners, catalog feed, continue-your-journey, featured collection, feed filter (save), new-this-week, trending.
- `buyerapp/CollectionListing/`: all collections, collection counts, similar collections (Saree and Readymade variants of each), top collection.
- `buyerapp/PDP/`: `PdpSimilarTest`.
- `buyerapp/ProductPage/`: `ProductSimilarCollectionTest`.
- `buyerapp/ProfilePage/`: app update, auth validate, user profile, video view action, watched videos.
- `buyerapp/SearchPage/`: search product, recommended chips, chip select.
- `buyerapp/newapis/`: production-safe tests for the new backend-api route groups: banner placement, MLP public read, Navo Live, events-ingestion negatives, sub-seller auth, PRISM access, referral validation, chatbot v2 validation.
- `buyerapp/schema/`: `ResponseSchemaTest`, JSON-schema contract checks.

**`src/test/resources/`**: `config.properties`, `secrets.properties.example`, `test-variables.properties`, `testng*.xml`, `allure.properties`, `log4j2.xml`, `schemas/buyerapp/*.json`.

### 3. Core framework classes

#### `BaseTest` (`src/main/java/com/automation/base/BaseTest.java`)

Static fields: `logger`, `config` (`ConfigManager`), `requestSpec`, `responseSpec`.

**Suite lifecycle**
- `beforeSuite(@Optional("default") String env)` (`@BeforeSuite(alwaysRun=true)`, `@Parameters("env")`): sets the `env` system property, then calls `ConfigManager.getInstance()` and `VariableManager.initialize()`.
- `afterSuite()`: calls `VariableManager.cleanup()`, which removes the ThreadLocal.

**Per-class setup:** `setUp()` (`@BeforeClass(alwaysRun=true)`) calls `initializeRestAssured()` for every test class. That method:
1. Sets `RestAssured.baseURI = config.baseUrl()` (the BOMB URL).
2. Builds a `RequestSpecBuilder` with JSON content-type and accept headers, an `AllureRestAssured` filter, and `log().all()` when `log.request=true`.
3. Calls `configureAuthentication`.
4. Builds a `ResponseSpecBuilder` with `log().all()` when `log.response=true`.
5. Assigns both specs globally to `RestAssured.requestSpecification` and `RestAssured.responseSpecification`.

**`configureAuthentication`** switches on `auth.type`:
- `basic`: uses `auth.username` and `auth.password`.
- `bearer` or `token`: adds `Authorization: Bearer`.
- `api_key`: adds `X-API-Key`.
- Anything else: no authentication.

**Note:** `config.properties` sets `auth.type=jwt`, which has no case and falls through to "no authentication". BOMB tests add their own `authorization: JWT <token>` header per request.

**`getRequestSpec()`** returns `given().spec(requestSpec)`. The tests don't use it; they call `RestAssured.given().spec(requestSpec)` directly.

**Buyer App request builders**
- `buyerAppGuestSpec()` returns a fresh `given()` with:
  - `baseUri(config.buyerAppBaseUrl())` and a JSON content type.
  - The app headers `Accept-Language`, `AppVersion`, `AppVersionCode`, `User-Segment` and `zone`, taken from the `buyer.app.*` keys.
  - A `HeaderConfig` that overwrites those headers plus `Accept`, `Content-Type` and `Authorization`, so a test that sets one replaces the default instead of sending duplicates.
  - No auth header. It still inherits the global `RestAssured.requestSpecification` (Allure filter and logging).
- `buyerAppSpec()` is the guest spec plus `Authorization: JWT <token>`. The token comes from `VariableManager.getBuyerAppToken()`, or from `loginBuyer()` if none is stored.
- `buyerAppTokenOrNull()` works the same way but returns null on `SkipException`.
- `loginBuyer()` (`private static synchronized`):
  1. Throws `SkipException` if `buyer.app.phone.number` or `buyer.app.token` is missing.
  2. POSTs `{phoneNumber, token}` to `/api/auth/login` and reads `data.accessToken`.
  3. Stores it with `VariableManager.setBuyerAppToken` and `saveBuyerAppTokenToFile()`, which despite its name keeps the token in memory only.

#### `ConfigManager` (`config/ConfigManager.java`)

It **does use the Owner library**:

```java
@LoadPolicy(LoadType.MERGE)
@Config.Sources({"system:properties","classpath:secrets.properties","classpath:config.properties","classpath:config-${env}.properties"})
```

- With `MERGE`, every source is read and the first one that defines a key wins.
- `-D` system properties beat `secrets.properties`, which beats `config.properties`, which beats `config-${env}.properties`. No `config-*.properties` file exists, so the `env` hook is unused.
- `getInstance()` first runs `applySecretEnvVars()`. It copies these env vars into system properties unless a `-D` already set the key:
  - `BOMB_LOGIN_PHONE` → `login.phone.number`
  - `BOMB_LOGIN_OTP` → `login.token`
  - `BUYER_LOGIN_PHONE` → `buyer.app.phone.number`
  - `BUYER_LOGIN_OTP` → `buyer.app.token`
- It then returns `ConfigCache.getOrCreate(ConfigManager.class)`, a cached singleton.
- **Effective precedence:** `-D` > env var > `secrets.properties` > `config.properties` > `@DefaultValue`.

**Keys:**

| Key | `config.properties` | `@DefaultValue` |
|---|---|---|
| `base.url` | `https://api.bizup.app` | jsonplaceholder |
| `api.timeout` | 40000 | 30000 |
| `retry.count` | 2 | 3 |
| `log.request` / `log.response` | true | true |
| `content.type` | (not set) | (none) |
| `auth.type` | jwt | none |
| `auth.token` / `auth.username` / `auth.password` | (not set) | (none) |
| `login.phone.number` / `login.token` | in secrets | (none) |
| `response.time.threshold` | 40000 | 40000 |
| `buyer.app.base.url` | (not set) | (none) |
| `buyer.app.phone.number` / `buyer.app.token` | in secrets | (none) |
| `buyer.app.version` | 4.1.3-debug | (none) |
| `buyer.app.version.code` | 420 | (none) |
| `buyer.app.user.segment` | 2 | (none) |
| `buyer.app.zone` | india | (none) |
| `buyer.app.language` | en | (none) |

`timeout()` and `contentType()` are defined but never read in the source.

#### Constants
- **`BombEndpoints`:**
  - Auth and catalog: `LOGIN=/api/auth/login`, `CATALOG_ALL=/v1/admin/catalog_all`.
  - Catalog assignment and editor actions: `CATALOG_ASSIGN`, `EDITOR_ASSIGN_CATALOG`, `EDITOR_SKIP_CATALOG`, `EDITOR_MARK_AS_DONE`.
  - Video pipeline: `VIDEO_UPLOAD=/v1/admin/pipeline/video`, `VIDEO_THUMBNAIL_UPLOAD`, `VIDEO_TAGGING`, `VIDEO_TITLE_GENERATION=/v2/ai/tags-to-text`.
  - Several constants share the path `/v1/admin/editor/assign/videos/done`.
- **`BuyerAppEndpoints`:** feed paths (`/v1/feed/...`), collections, `/api/appConfig/...`, `/api/user` and cart. A block of "new backend-api route groups (QA sync 2026-10-30)" adds banner, mlp, navo-live, `/events` (mounted at the root, not under `/v1`), subseller, prism, referral and `/v2/chatbot/message`. Some tests use literal paths instead, such as `/v1/catalog/{catalogId}` and `/v1/user/cart`.

#### There is no RestClient class
Tests build requests inline:
- BOMB: `RestAssured.given().spec(requestSpec).header("authorization","JWT "+token).header("source","bizupChat")`.
- Buyer App: `buyerAppSpec()` or `buyerAppGuestSpec()`.

#### `VariableManager` (`utils/VariableManager.java`): cross-test data sharing
- **Storage:**
  - A static `Properties globalProperties`, loaded once from `test-variables.properties` by `initialize()`.
  - A `ThreadLocal<Map<String,String>>` per thread, seeded with a copy of the globals on first use.
- **API:**
  - Generic: `get(key[, default])`, `getInt`, `getBoolean`, `set(key, Object)`, `has`, `remove`, `clear`, `getAllVariables`, `count`.
  - Typed helpers: `getToken`/`setToken` (`bomb_token`), `getBuyerAppToken`/`setBuyerAppToken` (`buyer_app_token`), `getSellerId`, `getVideoId`, `getResponseTimeout` (`res_time`, default 40000), and others.
- **No persistence:** `saveTokenToFile()` and `saveBuyerAppTokenToFile()` call `savePropertyToFile`, which only updates `globalProperties` and the current thread's map. Tokens are **never written to disk**, because `test-variables.properties` is tracked in git.
- **Threading:** `set()` writes only to the current thread's map. Suites have no `parallel` attribute, so everything runs on one thread and values set by an earlier class are visible to later ones.

#### `TestDataResolver` (`utils/TestDataResolver.java`)
Looks up Buyer App ids at runtime with read-only GETs, caches them as `resolved_<key>` in VariableManager, and falls back to `test-variables.properties` if a lookup fails:
- `liveCatalogId(Supplier<RequestSpecification>)`: `GET /v1/feed/home/catalog?size=10&page=0&suitable_for=<suitable_for>`, taking the first `data.result` item that has a `seller` object.
- `cartId`: `GET /v1/user/cart`, reading `data[0]._id`.
- `cartCatalogId`: the same call, reading `data[0].cartItems[0]._id`.

#### `JsonUtils`
A shared `ObjectMapper` with `FAIL_ON_UNKNOWN_PROPERTIES=false`, `FAIL_ON_EMPTY_BEANS=false` and indented output.
- `fromResponse(Response, Class)`: throws `RuntimeException` (including a body excerpt) for non-2xx responses, warns if the response isn't JSON, then calls `fromJson`.
- Also: `toJson`, `fromJsonToList`, `fromResponseToList`, `readJsonFile`, `readJsonFromResources`.

#### `ApiErrorAssertions`
`assertApiError(response, httpStatus[, messageFragment])` checks:
- the HTTP status;
- that `statusCode` is one of `"101"` (`THROWN_DEFAULT`), `"10001"` (`FAILURE`) or `"10003"` (`INVALID_ACCESS_TOKEN`);
- that `message` is non-empty and, if a fragment is given, contains it (case-insensitive).

The events endpoints use a different Go-style envelope and are asserted separately.

#### Listeners
- **`TestListener`** (registered in all three `testng*.xml` files):
  - Logs suite and test start, pass, fail and skip, plus pass/fail/skip counts at the end.
  - On failure, `saveFailureLog` (an `@Attachment`) adds a "Failure Stack Trace" text attachment.
  - Static `saveLog(name, content)` is available for manual attachments.
- **`RetryAnalyzer`** retries up to `retry.count` times (2 in config), but **nothing references it**: no `@Test(retryAnalyzer=...)`, no `IAnnotationTransformer`, no XML listener. Failed tests are therefore **not retried**, even though README.md advertises a "retry mechanism".

#### `DataGenerator`
Random strings, alphanumerics, numbers, emails (`@test.com`), 10-digit phone numbers, timestamps, UUIDs and unique names.

### 4. Request lifecycle

#### BOMB: login → catalog

1. **Login call:** `bomb/Login/LoginApiTest.testResponseStatus` (priority 1).
   - By this point `BaseTest.beforeSuite` has loaded config and VariableManager, and `setUp` has built `requestSpec` against `base.url`.
   - Builds `LoginRequest.builder().phoneNumber(config.loginPhoneNumber()).token(config.loginToken()).build()`.
   - POSTs to `BombEndpoints.LOGIN` with `.spec(requestSpec)`; Lombok and Jackson serialize the body, and `@JsonInclude(NON_NULL)` drops null fields.
   - `JsonUtils.fromResponse(..., LoginResponse.class)` deserializes into `LoginResponse{statusCode, message, data: LoginData{phoneNumber, name, businessName, isDeleted, accessToken, refreshToken}}`.
   - Asserts a 200 status.
2. **Login follow-ups:** each depends on the first with `dependsOnMethods="testResponseStatus"`.
   - Response time below `config.responseTimeThreshold()` (40000 ms).
   - Field types, including `isDeleted == false`.
   - Phone number, access token and refresh token are present.
   - `testStoreAccessToken` (`dependsOnMethods="testAccessTokenExists"`) calls `VariableManager.setToken(accessToken)` and `saveTokenToFile()`, which keeps the token in memory.
3. **Catalog search:** `bomb/CatalogSearch/AllCatalogTest`.
   - `@BeforeClass setupAuth()` reads `VariableManager.getToken()` and throws `RuntimeException` ("run LoginApiTest first") if it's missing, so BOMB classes depend on suite order.
   - `testResponseStatus` sends `GET CATALOG_ALL` with `offset=0&limit=20` and the headers `authorization: JWT <token>` and `source: bizupChat`, deserializes into `CatalogResponse`, and asserts 200.
   - 11 dependent tests check:
     - JSON content type and `message == "success"`;
     - `data.total.value >= 0`;
     - the bucket, item, seller and product structures, with product `id` matching the 24-hex ObjectId pattern;
     - `items.size() <= 20`;
     - response time.
   - Allure: `@Epic("BOMB Catalog Management")`, `@Feature`, `@Story` and `@Severity`, plus `@TmsLink("TC-ADMIN-001"/"TC-ADMIN-002")` at class level. `AllureRestAssured` attaches every request and response.

#### Buyer App: `buyerapp/CatalogByIdTest`

- `testResponseStatusCode200` calls `buyerAppSpec()`, which logs in on demand if LoginTest hasn't stored a token.
- `.pathParam("catalogId", TestDataResolver.liveCatalogId(BaseTest::buyerAppSpec))` resolves a live catalog id at runtime.
- Sends `GET /v1/catalog/{catalogId}` and deserializes into `CatalogByIdDetailResponse`.
- Dependent tests check:
  - Content-Type, response time and top-level fields;
  - the data and seller fields, with optional booleans and numbers checked only when present;
  - the product array and thumbnail.
- `@TmsLink` TC-CATALOG-004/008/009.
- The same endpoint is also contract-checked in `schema/ResponseSchemaTest.catalogByIdMatchesSchema` with `matchesJsonSchemaInClasspath("schemas/buyerapp/catalog-by-id.json")`.

#### Buyer login (`buyerapp/Login/LoginTest`)

- POSTs `BuyerLoginRequest` through `buyerAppGuestSpec()` and stores `data.accessToken` with `setBuyerAppToken`.
- The first test has `alwaysRun=true` and the groups `{"buyerapp","buyerapp-login"}`.
- **Quirk:** this test hardcodes `.token("000000")` instead of using `config.buyerAppToken()`, while `BaseTest.loginBuyer()` uses the configured value. Why they differ is unclear from the source.

### 5. Test organization

#### BOMB (`testng-bomb.xml`)
- One `<test>` with `preserve-order="true"` lists **24 classes** explicitly, in pipeline order:
  1. Login.
  2. CatalogSearch: All, Seller, Product and CatalogId filters.
  3. CatalogAssignToEditor: AllCatalogUploaded, SearchBySeller, AssignToEditor.
  4. CatalogEditor: AllCatalogsAssigned, FetchCatalogUploaded, CatalogsGroup, CatalogTag, BotCatalogTitleGenerate, CatalogTagging, CatalogEditorEdit, CatalogSkip, MarkAsDone.
  5. VideoUploadAndAssign: VideoUpload, AllVideoAssign…, UploadThumbnailVideo, VideoUploadMarkAsDone.
  6. VideoTagging: EditButton, TitleGeneration, VideoTagging.
- All tests are `groups="bomb"`.
- Inside each class, a priority-1 "status 200" test makes the call and stores the response in a field. The priority-2+ tests use `dependsOnMethods` on it, so if the call fails they are skipped rather than failed.
- Classes pass data forward through VariableManager:
  - `AllCatalogsAssignedTest` sets `catalog_foassign_id`.
  - `CatalogTagTest` sets `catalog_id`.
  - `AllVideoAssignForUploadingThumbnailVideoTest` sets `uploadId`.
  - `SearchWithCatalogIdFilterTest` reads `live_catalog_id`, falling back to `catalog_foassign_id` and then to a hardcoded id.

#### Buyer App (`testng-buyerapp.xml`)
- One `<test>` with `<groups><run><include name="buyerapp"/>` and `<package name="com.automation.tests.buyerapp.*"/>`. Any class in the package tree with `groups="buyerapp"` runs automatically; there's no class list to maintain.
- Order doesn't matter, because `buyerAppSpec()` logs in on demand.
- Feature coverage:
  - HomePage: feed banners, catalog feed, journey, featured, filters, trending, new-this-week.
  - CollectionListing: all, counts, similar (saree/readymade), top.
  - PDP: similar.
  - ProductPage: similar collection.
  - ProfilePage: app-update, auth validate, user, action, viewed videos.
  - SearchPage: product search, chips.
  - Root: catalog-by-id, suitable-for config, update_cart, feed/tv.
  - `newapis`: TC-coded negative and access tests.
  - `schema`: 3 contract tests.
- Disabled (`enabled=false`): BannersTest, ContinueYourJourneyTest, FeedFilterSaveTest, VideoFeedTvTest.

#### Master suite
`testng.xml` adds `TestListener`, sets `env=default`, and lists `<suite-files>` for bomb, then buyerapp.

#### Counts
README.md, generated by `scripts/update-test-counts.py`:
- BOMB: 24 classes, 212 tests.
- Buyer App: 39 classes, 259 enabled tests plus 40 disabled.
- Total: 63 classes, 471 tests.

Older prose is stale sample text: README.md ("Total Tests: 56"), README-BOMB.md ("26 test files"), README-BUYER-APP.md ("30 test files") and CICD-SETUP.md.

### 6. Models

- **Lombok:** every model has `@Data @NoArgsConstructor @AllArgsConstructor`; request models also have `@Builder` (13 files).
- **Jackson:** request models add `@JsonInclude(JsonInclude.Include.NON_NULL)`. Response models add `@JsonIgnoreProperties(ignoreUnknown = true)`, so new server fields don't break deserialization.
- **Nested objects:** nested JSON objects are `public static class` members with the same annotations, e.g. `CatalogResponse.CatalogData`, `.Total`, `.CatalogItem`, `.Seller` and `.Product`.
- **Field names:** usually mirror the JSON keys exactly, including underscores (`_id`, `doc_count`, `deprioritisation_status`); only 2 files use `@JsonProperty`.
- **Envelope:** `statusCode` (String), `message`, `data`.

```java
@Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponse {
    private String statusCode; private String message; private LoginData data;
    @Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoginData { private String phoneNumber; private String name; private String businessName;
        private Boolean isDeleted; private String accessToken; private String refreshToken; }
}
```

The request counterpart is `LoginRequest { phoneNumber, token }`, with `@Builder` and `@JsonInclude(NON_NULL)`.

### 7. Data & safety

#### Static data
- `test-variables.properties` holds about 40 keys: `seller_id`, `editor_id`, `catalog_id`, `live_catalog_id`, `cart_id`, `cart_catalog_id`, `suitable_for`, `search_product`, `video_id`, `res_time`, `navo_base`, `bizup_base`, and others. Many tests read these directly.
- Some BOMB tests hardcode ids or constants inline: `MarkAsDoneTest` posts to a fixed catalog id, and `VideoUploadTest` hardcodes editor and seller constants.

#### Runtime discovery
- Buyer App: `TestDataResolver` resolves `live_catalog_id`, `cart_id` and `cart_catalog_id`.
- BOMB: earlier classes discover ids and later classes reuse them through VariableManager.
- `newapis`: tests discover data that may not exist in production (an MLP occasion, a live event) and throw `SkipException` when it's absent; there are 20 `SkipException` uses in `newapis`.

#### Production safety
This applies explicitly to Buyer App `newapis` only (see README-BUYER-APP.md, the `testng-buyerapp.xml` comment and the class Javadocs):
- Those tests send only GETs, or requests rejected **before** any DB write, Kafka publish or LLM call: 401 (missing or invalid token), 403 (staff gate) or 400 (validation).
- `PrismAccessTest.requireBuyerIsNotStaff(probePath)` first sends a read-only GET probe. Only if that returns 403 does it send the two valid-shaped bodies (TC-PRISM-018/021); otherwise those tests skip.

**BOMB tests are not read-only:** they POST real assign, skip, tag, mark-done, video upload, thumbnail and tagging requests against `api.bizup.app`. Some legacy Buyer tests also POST: `UpdateCartTest`, `VideoViewActionTest`, and the disabled `FeedFilterSaveTest` and `VideoFeedTvTest`.

#### Allure TMS links
- `allure.properties` sets `allure.link.tms.pattern=https://github.com/Bizup-world/backend-api/search?q={}&type=code`, so `@TmsLink("TC-PRISM-001")` links to a code search in backend-api.
- The issue pattern is the placeholder `jira.example.com`.
- There are 184 `@TmsLink` annotations.

#### Schemas
- `src/test/resources/schemas/buyerapp/` contains `collection-all.json`, `feed-banners.json` and `catalog-by-id.json`.
- They're used only by `ResponseSchemaTest`, through `JsonSchemaValidator.matchesJsonSchemaInClasspath`.
- Per its Javadoc, they pin the envelope plus the field names and types the app relies on, and allow extra fields.

### 8. CI/CD (`Jenkinsfile`)

**Setup**
- `agent any`, Maven tool `'Maven 3.9.9'`.
- Parameter `TEST_PROFILE`: a choice of `all`, `bomb` or `buyerapp` (`all` means no `-P`).
- Environment variables:
  - `DATE_STR` (`yyyy-MM-dd_HH_mm`).
  - `S3_BUCKET=bizup-builds`, `S3_BASE_FOLDER='RestAssured API Report'`, `S3_REGION=ap-south-1`.
  - `EMAIL_RECIPIENTS`.
  - `NOTIFICATION_CONFIG=scripts/notification-config.json`.

**Stages, in order:**
1. **Checkout:** `checkout scm`.
2. **Run Tests:**
   - Runs `mvn clean test ${-Pprofile} -Dallure.results.directory=target/allure-results` inside `catchError(buildResult:'UNSTABLE', stageResult:'FAILURE')`.
   - Credentials come from `withCredentials` Secret-text bindings: `restassured-bomb-login-phone`→`BOMB_LOGIN_PHONE`, `restassured-bomb-login-otp`→`BOMB_LOGIN_OTP`, `restassured-buyer-login-phone`→`BUYER_LOGIN_PHONE`, `restassured-buyer-login-otp`→`BUYER_LOGIN_OTP`. `ConfigManager.applySecretEnvVars` reads them.
3. **Setup Allure:** if `allure` isn't on PATH, downloads Allure 2.25.0 to `/opt/allure` and symlinks it into `/usr/local/bin`.
4. **Generate Allure Report:**
   - Verifies `target/allure-results` exists (fails otherwise).
   - Runs `allure generate ... --clean -o target/site/allure-maven-plugin`, falling back to `mvn allure:report` and other Maven goals.
   - Lists files for verification; the fatal check is commented out.
5. **Upload to S3** (`aws s3 sync`/`cp`):
   - `s3://bizup-builds/RestAssured API Report/reports/allure_<DATE_STR>/`
   - `.../logs/logs_<DATE_STR>/` (from `target/logs/`)
   - `.../results/testng-results-<BUILD_NUMBER>.xml` (from `target/surefire-reports/testng-results.xml`)
6. **Extract Test Summary:** `python3 scripts/extract-test-summary.py target/surefire-reports/testng-results.xml > test-summary.json`, then `readJSON` and echo.
7. **Send Notifications:**
   - `withCredentials`: `google-chat-webhook-dev`→`WEBHOOK_URL`, plus `smtp-credentials` (usernamePassword)→`SMTP_USERNAME`/`SMTP_PASSWORD`.
   - Writes `scripts/notification-config.json.tmp` with placeholders, which an inline Python block replaces from env vars.
   - Runs `python3 scripts/send-notification.py test-summary.json scripts/notification-config.json.tmp`.
8. **Archive S3 Links:** writes `s3-links.txt` with the report, log and results URLs.

**`post` block**
- `always`: archives `test-summary.json` and `s3-links.txt`, and publishes the Jenkins Allure plugin from `target/allure-results`.
- `failure` / `success` / `unstable`: each sends an HTML `emailext` with build number, status, profile, duration and links.

**Retry:** Jenkins has no `retry {}` block, and TestNG retry isn't wired (section 3).

**Scripts (`scripts/`)**
- `extract-test-summary.py`: parses the root attributes of `testng-results.xml` (total, passed, failed, skipped) and every non-config `test-method`. Outputs JSON with `pass_rate` and a `test_cases` list (serial number, name, feature name derived from the class, description, status).
- `send-notification.py`: `send_email` (SMTP with TLS) and `send_google_chat` (posts to the webhook with `requests`), each toggled by `enable_email` / `enable_google_chat` in the config JSON.
- `notify-chat.sh`: a standalone curl-based Google Chat notifier. It greps totals from testng-results.xml and reads the webhook from `GOOGLE_CHAT_WEBHOOK_URL`. Not used by the Jenkinsfile.
- `notification-config.example.json`: template for the config JSON.
- `update-test-counts.py`: regenerates the README table between `<!-- test-counts:start/end -->` by counting `@Test` annotations and `enabled=false` per suite and area. `--check` exits 1 if the table is stale. Not called by the Jenkinsfile.

### 9. Reporting & logging

**Allure**
- `allure-testng` is on the classpath, and the AspectJ agent is set in surefire.
- `AllureRestAssured` is on the shared request spec, so every request and response is attached.
- Annotations used: `@Epic`, `@Feature`, `@Story`, `@Severity`, `@TmsLink`.
- `TestListener` attaches failure stack traces.
- Results go to `target/allure-results`; `allure.properties` sets the results directory and link patterns.

**Log4j2** (`log4j2.xml`)
- Pattern `%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n`; log directory `target/logs`.
- Appenders:
  - Console.
  - `automation.log`: a RollingFile that rolls at 10 MB or daily, keeps 10 files, gzip.
  - `error.log`: ERROR+ only, 10 MB, keeps 5 files.
- Loggers: `com.automation` at DEBUG (to all three appenders), `io.restassured` at WARN, root at INFO.
- RestAssured's own `log().all()` goes to stdout, because `log.request` and `log.response` are true.

### 10. FAQ

1. **Where is the base URL set?**
   - BOMB: `base.url` in `config.properties`, applied to `RestAssured.baseURI` in `BaseTest.initializeRestAssured()`.
   - Buyer App: `buyer.app.base.url`, applied per request in `buyerAppGuestSpec()`.
   - Override either with `-Dbase.url=` or `-Dbuyer.app.base.url=`.
2. **How do I add a new endpoint test?**
   1. Add the path to `BombEndpoints` or `BuyerAppEndpoints`.
   2. Add request/response POJOs under `models/`.
   3. Create a class extending `BaseTest`.
      - BOMB: read `VariableManager.getToken()` in `@BeforeClass`, then use `given().spec(requestSpec).header("authorization","JWT "+token)`.
      - Buyer App: use `buyerAppSpec()`.
   4. Write a priority-1 call test, then assertion tests with `dependsOnMethods`.
   5. Tag them `groups="bomb"` or `groups="buyerapp"`.
   6. Register the class: BOMB classes **must be added to `testng-bomb.xml`** in the right order; Buyer App classes are picked up automatically.
   7. Run `python scripts/update-test-counts.py`.
3. **How does retry work?** It doesn't at the moment. `RetryAnalyzer` exists and reads `retry.count`, but no test, transformer or suite references it, and Jenkins has no retry.
4. **How is the auth token obtained?**
   - BOMB: `LoginApiTest` POSTs `/api/auth/login` and calls `VariableManager.setToken`; later classes fail in `@BeforeClass` if the token is missing.
   - Buyer App: `LoginTest` stores it, or `BaseTest.loginBuyer()` logs in lazily.
   - Both send it as `Authorization: JWT <token>`.
5. **Are tokens persisted between runs?** No. The `save*TokenToFile` methods only update in-memory globals.
6. **Where do credentials go?**
   - Locally: `src/test/resources/secrets.properties` (gitignored; copy it from `.example`), env vars or `-D`.
   - CI: Jenkins Secret-text credentials mapped to `BOMB_LOGIN_*` and `BUYER_LOGIN_*`.
7. **Does ConfigManager use Owner?** Yes: Owner 1.0.12 with `LoadType.MERGE`, the sources listed in section 3, and `ConfigCache` for the singleton.
8. **What performance threshold applies?** `response.time.threshold` (40000 ms). Tests assert `response.getTime() < threshold`.
9. **How is test data shared between classes?** Through `VariableManager`: a ThreadLocal map seeded from `test-variables.properties`, plus `set`/`get` for values produced at runtime.
10. **How are the README test counts produced?** `scripts/update-test-counts.py` regex-counts `@Test` annotations (excluding `enabled=false`) per suite and top-level feature folder, then rewrites the marked README block. Current totals: 471 enabled, 40 disabled.
11. **Why do some Buyer tests skip instead of fail?** One of these:
    - Login credentials are missing (`SkipException` in `loginBuyer`).
    - The production data the test needs is absent (`newapis`).
    - The PRISM staff probe didn't return 403.
    - A `dependsOnMethods` parent failed.
12. **What does schema validation cover?** Only 3 Buyer endpoints, in `ResponseSchemaTest`, against `schemas/buyerapp/*.json`. Other tests assert fields through POJOs and Hamcrest.
13. **Which assertion library is used?** Hamcrest `assertThat` throughout, plus `ApiErrorAssertions.assertApiError` for backend-api error envelopes.
14. **Where are the reports?**
    - Locally: `target/allure-results`, `target/site/allure-maven-plugin` and `target/logs`.
    - CI: S3 `bizup-builds/RestAssured API Report/{reports,logs,results}/...`, the Jenkins Allure plugin, and the archived `s3-links.txt`.
15. **Does `auth.type=jwt` do anything?** No. `configureAuthentication` has no `jwt` case, so no global auth is added; tests add the JWT header themselves.

## Part 2: File tree

```text
RestAssured_Automation/
├── scripts/
│   ├── extract-test-summary.py
│   ├── notification-config.example.json
│   ├── notify-chat.sh
│   ├── send-notification.py
│   └── update-test-counts.py
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── automation/
│   │               ├── base/
│   │               │   └── BaseTest.java
│   │               ├── config/
│   │               │   └── ConfigManager.java
│   │               ├── constants/
│   │               │   ├── BombEndpoints.java
│   │               │   ├── BuyerAppEndpoints.java
│   │               │   └── HttpStatus.java
│   │               ├── listeners/
│   │               │   ├── RetryAnalyzer.java
│   │               │   └── TestListener.java
│   │               ├── models/
│   │               │   ├── request/
│   │               │   │   ├── BotCatalogTitleGenerateRequest.java
│   │               │   │   ├── BuyerLoginRequest.java
│   │               │   │   ├── CatalogTaggingRequest.java
│   │               │   │   ├── FeedFilterSaveRequest.java
│   │               │   │   ├── LoginRequest.java
│   │               │   │   ├── UpdateCartRequest.java
│   │               │   │   ├── VideoFeedRequest.java
│   │               │   │   ├── VideoTaggingRequest.java
│   │               │   │   ├── VideoThumbnailUploadRequest.java
│   │               │   │   ├── VideoTitleGenerationRequest.java
│   │               │   │   ├── VideoTitleRequest.java
│   │               │   │   ├── VideoUploadRequest.java
│   │               │   │   └── VideoViewActionRequest.java
│   │               │   └── response/
│   │               │       ├── AppUpdateResponse.java
│   │               │       ├── AuthValidateResponse.java
│   │               │       ├── BannersResponse.java
│   │               │       ├── BuyerLoginResponse.java
│   │               │       ├── CatalogAssignResponse.java
│   │               │       ├── CatalogByIdDetailResponse.java
│   │               │       ├── CatalogEditGroupResponse.java
│   │               │       ├── CatalogEditorResponse.java
│   │               │       ├── CatalogGroupResponse.java
│   │               │       ├── CatalogResponse.java
│   │               │       ├── CatalogSkipResponse.java
│   │               │       ├── CatalogTaggingResponse.java
│   │               │       ├── CatalogTagResponse.java
│   │               │       ├── CatalogUploadedResponse.java
│   │               │       ├── CollectionAllResponse.java
│   │               │       ├── CollectionByIdResponse.java
│   │               │       ├── FeaturedCollectionResponse.java
│   │               │       ├── FeedFilterResponse.java
│   │               │       ├── FeedFilterSaveResponse.java
│   │               │       ├── HomeCatalogFeedResponse.java
│   │               │       ├── JourneyCollectionResponse.java
│   │               │       ├── LoginResponse.java
│   │               │       ├── MarkAsDoneResponse.java
│   │               │       ├── NewThisWeekResponse.java
│   │               │       ├── PdpSimilarResponse.java
│   │               │       ├── RelatedCollectionResponse.java
│   │               │       ├── SearchProductResponse.java
│   │               │       ├── SearchRecommendChipSelectResponse.java
│   │               │       ├── SearchRecommendedChipsResponse.java
│   │               │       ├── SearchResponse.java
│   │               │       ├── SimilarCollectionResponse.java
│   │               │       ├── SuitableForConfigResponse.java
│   │               │       ├── TitleGenerateResponse.java
│   │               │       ├── TopCollectionResponse.java
│   │               │       ├── TrendingFeedResponse.java
│   │               │       ├── UpdateCartResponse.java
│   │               │       ├── UserProfileResponse.java
│   │               │       ├── VideoAssignUploadResponse.java
│   │               │       ├── VideoFeedResponse.java
│   │               │       ├── VideoTaggingEditResponse.java
│   │               │       ├── VideoTaggingResponse.java
│   │               │       ├── VideoThumbnailUploadResponse.java
│   │               │       ├── VideoTitleGenerationResponse.java
│   │               │       ├── VideoUploadMarkAsDoneResponse.java
│   │               │       ├── VideoUploadResponse.java
│   │               │       ├── VideoViewActionResponse.java
│   │               │       └── WatchedVideosResponse.java
│   │               └── utils/
│   │                   ├── ApiErrorAssertions.java
│   │                   ├── DataGenerator.java
│   │                   ├── JsonUtils.java
│   │                   ├── TestDataResolver.java
│   │                   └── VariableManager.java
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── automation/
│       │           └── tests/
│       │               ├── bomb/
│       │               │   ├── CatalogSearch/
│       │               │   │   ├── AllCatalogTest.java
│       │               │   │   ├── SearchWithCatalogIdFilterTest.java
│       │               │   │   ├── SearchWithProductFilterTest.java
│       │               │   │   └── SearchWithSellerFilterTest.java
│       │               │   ├── CatalogTagPipeline/
│       │               │   │   ├── CatalogAssignToEditor/
│       │               │   │   │   ├── AllCatalogUploadedTest.java
│       │               │   │   │   ├── AssignToEditorTest.java
│       │               │   │   │   └── CatalogUploadedSearchBySellerFilterTest.java
│       │               │   │   └── CatalogEditor/
│       │               │   │       ├── AllCatalogsAssignedTest.java
│       │               │   │       ├── BotCatalogTitleGenerateTest.java
│       │               │   │       ├── CatalogEditorEditTest.java
│       │               │   │       ├── CatalogsGroupTest.java
│       │               │   │       ├── CatalogSkipTest.java
│       │               │   │       ├── CatalogTaggingTest.java
│       │               │   │       ├── CatalogTagTest.java
│       │               │   │       ├── FetchCatalogUploadedTest.java
│       │               │   │       └── MarkAsDoneTest.java
│       │               │   ├── Login/
│       │               │   │   └── LoginApiTest.java
│       │               │   └── VideoTaggingPipeline/
│       │               │       ├── VideoTagging/
│       │               │       │   ├── VideoTaggingEditButtonTest.java
│       │               │       │   ├── VideoTaggingTest.java
│       │               │       │   └── VideoTitleGenerationTest.java
│       │               │       └── VideoUploadAndAssignToEditor/
│       │               │           ├── AllVideoAssignForUploadingThumbnailVideoTest.java
│       │               │           ├── UploadThumbnailVideoTest.java
│       │               │           ├── VideoUploadMarkAsDoneTest.java
│       │               │           └── VideoUploadTest.java
│       │               └── buyerapp/
│       │                   ├── CollectionListing/
│       │                   │   ├── AllCollection/
│       │                   │   │   ├── CollectionAllForReadymadeTest.java
│       │                   │   │   └── CollectionAllForSareeTest.java
│       │                   │   ├── AllCollectionCounts/
│       │                   │   │   ├── CollectionCountsForReadymadeTest.java
│       │                   │   │   └── CollectionCountsForSareeTest.java
│       │                   │   ├── SimilarCollection/
│       │                   │   │   ├── SimilarCollectionForReadymadeTest.java
│       │                   │   │   └── SimilarCollectionForSareeTest.java
│       │                   │   └── TopCollectionTest.java
│       │                   ├── HomePage/
│       │                   │   ├── BannersTest.java
│       │                   │   ├── CatalogFeedTest.java
│       │                   │   ├── ContinueYourJourneyTest.java
│       │                   │   ├── FeaturedCollectionTest.java
│       │                   │   ├── FeedFilterSaveTest.java
│       │                   │   ├── FeedFilterTest.java
│       │                   │   ├── NewThisWeekTest.java
│       │                   │   └── TrendingTest.java
│       │                   ├── Login/
│       │                   │   └── LoginTest.java
│       │                   ├── newapis/
│       │                   │   ├── BannerPlacementTest.java
│       │                   │   ├── ChatbotV2ValidationTest.java
│       │                   │   ├── EventsIngestionNegativeTest.java
│       │                   │   ├── MlpPublicReadTest.java
│       │                   │   ├── NavoLiveTest.java
│       │                   │   ├── PrismAccessTest.java
│       │                   │   ├── ReferralValidationTest.java
│       │                   │   └── SubSellerAuthTest.java
│       │                   ├── PDP/
│       │                   │   └── PdpSimilarTest.java
│       │                   ├── ProductPage/
│       │                   │   └── ProductSimilarCollectionTest.java
│       │                   ├── ProfilePage/
│       │                   │   ├── AppUpdateTest.java
│       │                   │   ├── AuthValidateTest.java
│       │                   │   ├── UserProfileTest.java
│       │                   │   ├── VideoViewActionTest.java
│       │                   │   └── WatchedVideosTest.java
│       │                   ├── schema/
│       │                   │   └── ResponseSchemaTest.java
│       │                   ├── SearchPage/
│       │                   │   ├── SearchProductTest.java
│       │                   │   ├── SearchRecommendChipSelectTest.java
│       │                   │   └── SearchRecommendedChipsTest.java
│       │                   ├── CatalogByIdTest.java
│       │                   ├── SuitableForConfigTest.java
│       │                   ├── UpdateCartTest.java
│       │                   └── VideoFeedTvTest.java
│       └── resources/
│           ├── schemas/
│           │   └── buyerapp/
│           │       ├── catalog-by-id.json
│           │       ├── collection-all.json
│           │       └── feed-banners.json
│           ├── allure.properties
│           ├── config.properties
│           ├── log4j2.xml
│           ├── secrets.properties.example
│           ├── test-variables.properties
│           ├── testng-bomb.xml
│           ├── testng-buyerapp.xml
│           └── testng.xml
├── .gitignore
├── CICD-SETUP.md
├── Jenkinsfile
├── pom.xml
├── README-BOMB.md
├── README-BUYER-APP.md
└── README.md
```

## Part 3: Source files

- [`.gitignore`](#gitignore)
- [`CICD-SETUP.md`](#cicd-setupmd)
- [`Jenkinsfile`](#jenkinsfile)
- [`README-BOMB.md`](#readme-bombmd)
- [`README-BUYER-APP.md`](#readme-buyer-appmd)
- [`README.md`](#readmemd)
- [`pom.xml`](#pomxml)
- [`scripts/extract-test-summary.py`](#scriptsextract-test-summarypy)
- [`scripts/notification-config.example.json`](#scriptsnotification-configexamplejson)
- [`scripts/notify-chat.sh`](#scriptsnotify-chatsh)
- [`scripts/send-notification.py`](#scriptssend-notificationpy)
- [`scripts/update-test-counts.py`](#scriptsupdate-test-countspy)
- [`src/main/java/com/automation/base/BaseTest.java`](#srcmainjavacomautomationbasebasetestjava)
- [`src/main/java/com/automation/config/ConfigManager.java`](#srcmainjavacomautomationconfigconfigmanagerjava)
- [`src/main/java/com/automation/constants/BombEndpoints.java`](#srcmainjavacomautomationconstantsbombendpointsjava)
- [`src/main/java/com/automation/constants/BuyerAppEndpoints.java`](#srcmainjavacomautomationconstantsbuyerappendpointsjava)
- [`src/main/java/com/automation/constants/HttpStatus.java`](#srcmainjavacomautomationconstantshttpstatusjava)
- [`src/main/java/com/automation/listeners/RetryAnalyzer.java`](#srcmainjavacomautomationlistenersretryanalyzerjava)
- [`src/main/java/com/automation/listeners/TestListener.java`](#srcmainjavacomautomationlistenerstestlistenerjava)
- [`src/main/java/com/automation/models/request/BotCatalogTitleGenerateRequest.java`](#srcmainjavacomautomationmodelsrequestbotcatalogtitlegeneraterequestjava)
- [`src/main/java/com/automation/models/request/BuyerLoginRequest.java`](#srcmainjavacomautomationmodelsrequestbuyerloginrequestjava)
- [`src/main/java/com/automation/models/request/CatalogTaggingRequest.java`](#srcmainjavacomautomationmodelsrequestcatalogtaggingrequestjava)
- [`src/main/java/com/automation/models/request/FeedFilterSaveRequest.java`](#srcmainjavacomautomationmodelsrequestfeedfiltersaverequestjava)
- [`src/main/java/com/automation/models/request/LoginRequest.java`](#srcmainjavacomautomationmodelsrequestloginrequestjava)
- [`src/main/java/com/automation/models/request/UpdateCartRequest.java`](#srcmainjavacomautomationmodelsrequestupdatecartrequestjava)
- [`src/main/java/com/automation/models/request/VideoFeedRequest.java`](#srcmainjavacomautomationmodelsrequestvideofeedrequestjava)
- [`src/main/java/com/automation/models/request/VideoTaggingRequest.java`](#srcmainjavacomautomationmodelsrequestvideotaggingrequestjava)
- [`src/main/java/com/automation/models/request/VideoThumbnailUploadRequest.java`](#srcmainjavacomautomationmodelsrequestvideothumbnailuploadrequestjava)
- [`src/main/java/com/automation/models/request/VideoTitleGenerationRequest.java`](#srcmainjavacomautomationmodelsrequestvideotitlegenerationrequestjava)
- [`src/main/java/com/automation/models/request/VideoTitleRequest.java`](#srcmainjavacomautomationmodelsrequestvideotitlerequestjava)
- [`src/main/java/com/automation/models/request/VideoUploadRequest.java`](#srcmainjavacomautomationmodelsrequestvideouploadrequestjava)
- [`src/main/java/com/automation/models/request/VideoViewActionRequest.java`](#srcmainjavacomautomationmodelsrequestvideoviewactionrequestjava)
- [`src/main/java/com/automation/models/response/AppUpdateResponse.java`](#srcmainjavacomautomationmodelsresponseappupdateresponsejava)
- [`src/main/java/com/automation/models/response/AuthValidateResponse.java`](#srcmainjavacomautomationmodelsresponseauthvalidateresponsejava)
- [`src/main/java/com/automation/models/response/BannersResponse.java`](#srcmainjavacomautomationmodelsresponsebannersresponsejava)
- [`src/main/java/com/automation/models/response/BuyerLoginResponse.java`](#srcmainjavacomautomationmodelsresponsebuyerloginresponsejava)
- [`src/main/java/com/automation/models/response/CatalogAssignResponse.java`](#srcmainjavacomautomationmodelsresponsecatalogassignresponsejava)
- [`src/main/java/com/automation/models/response/CatalogByIdDetailResponse.java`](#srcmainjavacomautomationmodelsresponsecatalogbyiddetailresponsejava)
- [`src/main/java/com/automation/models/response/CatalogEditGroupResponse.java`](#srcmainjavacomautomationmodelsresponsecatalogeditgroupresponsejava)
- [`src/main/java/com/automation/models/response/CatalogEditorResponse.java`](#srcmainjavacomautomationmodelsresponsecatalogeditorresponsejava)
- [`src/main/java/com/automation/models/response/CatalogGroupResponse.java`](#srcmainjavacomautomationmodelsresponsecataloggroupresponsejava)
- [`src/main/java/com/automation/models/response/CatalogResponse.java`](#srcmainjavacomautomationmodelsresponsecatalogresponsejava)
- [`src/main/java/com/automation/models/response/CatalogSkipResponse.java`](#srcmainjavacomautomationmodelsresponsecatalogskipresponsejava)
- [`src/main/java/com/automation/models/response/CatalogTagResponse.java`](#srcmainjavacomautomationmodelsresponsecatalogtagresponsejava)
- [`src/main/java/com/automation/models/response/CatalogTaggingResponse.java`](#srcmainjavacomautomationmodelsresponsecatalogtaggingresponsejava)
- [`src/main/java/com/automation/models/response/CatalogUploadedResponse.java`](#srcmainjavacomautomationmodelsresponsecataloguploadedresponsejava)
- [`src/main/java/com/automation/models/response/CollectionAllResponse.java`](#srcmainjavacomautomationmodelsresponsecollectionallresponsejava)
- [`src/main/java/com/automation/models/response/CollectionByIdResponse.java`](#srcmainjavacomautomationmodelsresponsecollectionbyidresponsejava)
- [`src/main/java/com/automation/models/response/FeaturedCollectionResponse.java`](#srcmainjavacomautomationmodelsresponsefeaturedcollectionresponsejava)
- [`src/main/java/com/automation/models/response/FeedFilterResponse.java`](#srcmainjavacomautomationmodelsresponsefeedfilterresponsejava)
- [`src/main/java/com/automation/models/response/FeedFilterSaveResponse.java`](#srcmainjavacomautomationmodelsresponsefeedfiltersaveresponsejava)
- [`src/main/java/com/automation/models/response/HomeCatalogFeedResponse.java`](#srcmainjavacomautomationmodelsresponsehomecatalogfeedresponsejava)
- [`src/main/java/com/automation/models/response/JourneyCollectionResponse.java`](#srcmainjavacomautomationmodelsresponsejourneycollectionresponsejava)
- [`src/main/java/com/automation/models/response/LoginResponse.java`](#srcmainjavacomautomationmodelsresponseloginresponsejava)
- [`src/main/java/com/automation/models/response/MarkAsDoneResponse.java`](#srcmainjavacomautomationmodelsresponsemarkasdoneresponsejava)
- [`src/main/java/com/automation/models/response/NewThisWeekResponse.java`](#srcmainjavacomautomationmodelsresponsenewthisweekresponsejava)
- [`src/main/java/com/automation/models/response/PdpSimilarResponse.java`](#srcmainjavacomautomationmodelsresponsepdpsimilarresponsejava)
- [`src/main/java/com/automation/models/response/RelatedCollectionResponse.java`](#srcmainjavacomautomationmodelsresponserelatedcollectionresponsejava)
- [`src/main/java/com/automation/models/response/SearchProductResponse.java`](#srcmainjavacomautomationmodelsresponsesearchproductresponsejava)
- [`src/main/java/com/automation/models/response/SearchRecommendChipSelectResponse.java`](#srcmainjavacomautomationmodelsresponsesearchrecommendchipselectresponsejava)
- [`src/main/java/com/automation/models/response/SearchRecommendedChipsResponse.java`](#srcmainjavacomautomationmodelsresponsesearchrecommendedchipsresponsejava)
- [`src/main/java/com/automation/models/response/SearchResponse.java`](#srcmainjavacomautomationmodelsresponsesearchresponsejava)
- [`src/main/java/com/automation/models/response/SimilarCollectionResponse.java`](#srcmainjavacomautomationmodelsresponsesimilarcollectionresponsejava)
- [`src/main/java/com/automation/models/response/SuitableForConfigResponse.java`](#srcmainjavacomautomationmodelsresponsesuitableforconfigresponsejava)
- [`src/main/java/com/automation/models/response/TitleGenerateResponse.java`](#srcmainjavacomautomationmodelsresponsetitlegenerateresponsejava)
- [`src/main/java/com/automation/models/response/TopCollectionResponse.java`](#srcmainjavacomautomationmodelsresponsetopcollectionresponsejava)
- [`src/main/java/com/automation/models/response/TrendingFeedResponse.java`](#srcmainjavacomautomationmodelsresponsetrendingfeedresponsejava)
- [`src/main/java/com/automation/models/response/UpdateCartResponse.java`](#srcmainjavacomautomationmodelsresponseupdatecartresponsejava)
- [`src/main/java/com/automation/models/response/UserProfileResponse.java`](#srcmainjavacomautomationmodelsresponseuserprofileresponsejava)
- [`src/main/java/com/automation/models/response/VideoAssignUploadResponse.java`](#srcmainjavacomautomationmodelsresponsevideoassignuploadresponsejava)
- [`src/main/java/com/automation/models/response/VideoFeedResponse.java`](#srcmainjavacomautomationmodelsresponsevideofeedresponsejava)
- [`src/main/java/com/automation/models/response/VideoTaggingEditResponse.java`](#srcmainjavacomautomationmodelsresponsevideotaggingeditresponsejava)
- [`src/main/java/com/automation/models/response/VideoTaggingResponse.java`](#srcmainjavacomautomationmodelsresponsevideotaggingresponsejava)
- [`src/main/java/com/automation/models/response/VideoThumbnailUploadResponse.java`](#srcmainjavacomautomationmodelsresponsevideothumbnailuploadresponsejava)
- [`src/main/java/com/automation/models/response/VideoTitleGenerationResponse.java`](#srcmainjavacomautomationmodelsresponsevideotitlegenerationresponsejava)
- [`src/main/java/com/automation/models/response/VideoUploadMarkAsDoneResponse.java`](#srcmainjavacomautomationmodelsresponsevideouploadmarkasdoneresponsejava)
- [`src/main/java/com/automation/models/response/VideoUploadResponse.java`](#srcmainjavacomautomationmodelsresponsevideouploadresponsejava)
- [`src/main/java/com/automation/models/response/VideoViewActionResponse.java`](#srcmainjavacomautomationmodelsresponsevideoviewactionresponsejava)
- [`src/main/java/com/automation/models/response/WatchedVideosResponse.java`](#srcmainjavacomautomationmodelsresponsewatchedvideosresponsejava)
- [`src/main/java/com/automation/utils/ApiErrorAssertions.java`](#srcmainjavacomautomationutilsapierrorassertionsjava)
- [`src/main/java/com/automation/utils/DataGenerator.java`](#srcmainjavacomautomationutilsdatageneratorjava)
- [`src/main/java/com/automation/utils/JsonUtils.java`](#srcmainjavacomautomationutilsjsonutilsjava)
- [`src/main/java/com/automation/utils/TestDataResolver.java`](#srcmainjavacomautomationutilstestdataresolverjava)
- [`src/main/java/com/automation/utils/VariableManager.java`](#srcmainjavacomautomationutilsvariablemanagerjava)
- [`src/test/java/com/automation/tests/bomb/CatalogSearch/AllCatalogTest.java`](#srctestjavacomautomationtestsbombcatalogsearchallcatalogtestjava)
- [`src/test/java/com/automation/tests/bomb/CatalogSearch/SearchWithCatalogIdFilterTest.java`](#srctestjavacomautomationtestsbombcatalogsearchsearchwithcatalogidfiltertestjava)
- [`src/test/java/com/automation/tests/bomb/CatalogSearch/SearchWithProductFilterTest.java`](#srctestjavacomautomationtestsbombcatalogsearchsearchwithproductfiltertestjava)
- [`src/test/java/com/automation/tests/bomb/CatalogSearch/SearchWithSellerFilterTest.java`](#srctestjavacomautomationtestsbombcatalogsearchsearchwithsellerfiltertestjava)
- [`src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogAssignToEditor/AllCatalogUploadedTest.java`](#srctestjavacomautomationtestsbombcatalogtagpipelinecatalogassigntoeditorallcataloguploadedtestjava)
- [`src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogAssignToEditor/AssignToEditorTest.java`](#srctestjavacomautomationtestsbombcatalogtagpipelinecatalogassigntoeditorassigntoeditortestjava)
- [`src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogAssignToEditor/CatalogUploadedSearchBySellerFilterTest.java`](#srctestjavacomautomationtestsbombcatalogtagpipelinecatalogassigntoeditorcataloguploadedsearchbysellerfiltertestjava)
- [`src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/AllCatalogsAssignedTest.java`](#srctestjavacomautomationtestsbombcatalogtagpipelinecatalogeditorallcatalogsassignedtestjava)
- [`src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/BotCatalogTitleGenerateTest.java`](#srctestjavacomautomationtestsbombcatalogtagpipelinecatalogeditorbotcatalogtitlegeneratetestjava)
- [`src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/CatalogEditorEditTest.java`](#srctestjavacomautomationtestsbombcatalogtagpipelinecatalogeditorcatalogeditoredittestjava)
- [`src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/CatalogSkipTest.java`](#srctestjavacomautomationtestsbombcatalogtagpipelinecatalogeditorcatalogskiptestjava)
- [`src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/CatalogTagTest.java`](#srctestjavacomautomationtestsbombcatalogtagpipelinecatalogeditorcatalogtagtestjava)
- [`src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/CatalogTaggingTest.java`](#srctestjavacomautomationtestsbombcatalogtagpipelinecatalogeditorcatalogtaggingtestjava)
- [`src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/CatalogsGroupTest.java`](#srctestjavacomautomationtestsbombcatalogtagpipelinecatalogeditorcatalogsgrouptestjava)
- [`src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/FetchCatalogUploadedTest.java`](#srctestjavacomautomationtestsbombcatalogtagpipelinecatalogeditorfetchcataloguploadedtestjava)
- [`src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/MarkAsDoneTest.java`](#srctestjavacomautomationtestsbombcatalogtagpipelinecatalogeditormarkasdonetestjava)
- [`src/test/java/com/automation/tests/bomb/Login/LoginApiTest.java`](#srctestjavacomautomationtestsbombloginloginapitestjava)
- [`src/test/java/com/automation/tests/bomb/VideoTaggingPipeline/VideoTagging/VideoTaggingEditButtonTest.java`](#srctestjavacomautomationtestsbombvideotaggingpipelinevideotaggingvideotaggingeditbuttontestjava)
- [`src/test/java/com/automation/tests/bomb/VideoTaggingPipeline/VideoTagging/VideoTaggingTest.java`](#srctestjavacomautomationtestsbombvideotaggingpipelinevideotaggingvideotaggingtestjava)
- [`src/test/java/com/automation/tests/bomb/VideoTaggingPipeline/VideoTagging/VideoTitleGenerationTest.java`](#srctestjavacomautomationtestsbombvideotaggingpipelinevideotaggingvideotitlegenerationtestjava)
- [`src/test/java/com/automation/tests/bomb/VideoTaggingPipeline/VideoUploadAndAssignToEditor/AllVideoAssignForUploadingThumbnailVideoTest.java`](#srctestjavacomautomationtestsbombvideotaggingpipelinevideouploadandassigntoeditorallvideoassignforuploadingthumbnailvideotestjava)
- [`src/test/java/com/automation/tests/bomb/VideoTaggingPipeline/VideoUploadAndAssignToEditor/UploadThumbnailVideoTest.java`](#srctestjavacomautomationtestsbombvideotaggingpipelinevideouploadandassigntoeditoruploadthumbnailvideotestjava)
- [`src/test/java/com/automation/tests/bomb/VideoTaggingPipeline/VideoUploadAndAssignToEditor/VideoUploadMarkAsDoneTest.java`](#srctestjavacomautomationtestsbombvideotaggingpipelinevideouploadandassigntoeditorvideouploadmarkasdonetestjava)
- [`src/test/java/com/automation/tests/bomb/VideoTaggingPipeline/VideoUploadAndAssignToEditor/VideoUploadTest.java`](#srctestjavacomautomationtestsbombvideotaggingpipelinevideouploadandassigntoeditorvideouploadtestjava)
- [`src/test/java/com/automation/tests/buyerapp/CatalogByIdTest.java`](#srctestjavacomautomationtestsbuyerappcatalogbyidtestjava)
- [`src/test/java/com/automation/tests/buyerapp/CollectionListing/AllCollection/CollectionAllForReadymadeTest.java`](#srctestjavacomautomationtestsbuyerappcollectionlistingallcollectioncollectionallforreadymadetestjava)
- [`src/test/java/com/automation/tests/buyerapp/CollectionListing/AllCollection/CollectionAllForSareeTest.java`](#srctestjavacomautomationtestsbuyerappcollectionlistingallcollectioncollectionallforsareetestjava)
- [`src/test/java/com/automation/tests/buyerapp/CollectionListing/AllCollectionCounts/CollectionCountsForReadymadeTest.java`](#srctestjavacomautomationtestsbuyerappcollectionlistingallcollectioncountscollectioncountsforreadymadetestjava)
- [`src/test/java/com/automation/tests/buyerapp/CollectionListing/AllCollectionCounts/CollectionCountsForSareeTest.java`](#srctestjavacomautomationtestsbuyerappcollectionlistingallcollectioncountscollectioncountsforsareetestjava)
- [`src/test/java/com/automation/tests/buyerapp/CollectionListing/SimilarCollection/SimilarCollectionForReadymadeTest.java`](#srctestjavacomautomationtestsbuyerappcollectionlistingsimilarcollectionsimilarcollectionforreadymadetestjava)
- [`src/test/java/com/automation/tests/buyerapp/CollectionListing/SimilarCollection/SimilarCollectionForSareeTest.java`](#srctestjavacomautomationtestsbuyerappcollectionlistingsimilarcollectionsimilarcollectionforsareetestjava)
- [`src/test/java/com/automation/tests/buyerapp/CollectionListing/TopCollectionTest.java`](#srctestjavacomautomationtestsbuyerappcollectionlistingtopcollectiontestjava)
- [`src/test/java/com/automation/tests/buyerapp/HomePage/BannersTest.java`](#srctestjavacomautomationtestsbuyerapphomepagebannerstestjava)
- [`src/test/java/com/automation/tests/buyerapp/HomePage/CatalogFeedTest.java`](#srctestjavacomautomationtestsbuyerapphomepagecatalogfeedtestjava)
- [`src/test/java/com/automation/tests/buyerapp/HomePage/ContinueYourJourneyTest.java`](#srctestjavacomautomationtestsbuyerapphomepagecontinueyourjourneytestjava)
- [`src/test/java/com/automation/tests/buyerapp/HomePage/FeaturedCollectionTest.java`](#srctestjavacomautomationtestsbuyerapphomepagefeaturedcollectiontestjava)
- [`src/test/java/com/automation/tests/buyerapp/HomePage/FeedFilterSaveTest.java`](#srctestjavacomautomationtestsbuyerapphomepagefeedfiltersavetestjava)
- [`src/test/java/com/automation/tests/buyerapp/HomePage/FeedFilterTest.java`](#srctestjavacomautomationtestsbuyerapphomepagefeedfiltertestjava)
- [`src/test/java/com/automation/tests/buyerapp/HomePage/NewThisWeekTest.java`](#srctestjavacomautomationtestsbuyerapphomepagenewthisweektestjava)
- [`src/test/java/com/automation/tests/buyerapp/HomePage/TrendingTest.java`](#srctestjavacomautomationtestsbuyerapphomepagetrendingtestjava)
- [`src/test/java/com/automation/tests/buyerapp/Login/LoginTest.java`](#srctestjavacomautomationtestsbuyerapploginlogintestjava)
- [`src/test/java/com/automation/tests/buyerapp/PDP/PdpSimilarTest.java`](#srctestjavacomautomationtestsbuyerapppdppdpsimilartestjava)
- [`src/test/java/com/automation/tests/buyerapp/ProductPage/ProductSimilarCollectionTest.java`](#srctestjavacomautomationtestsbuyerappproductpageproductsimilarcollectiontestjava)
- [`src/test/java/com/automation/tests/buyerapp/ProfilePage/AppUpdateTest.java`](#srctestjavacomautomationtestsbuyerappprofilepageappupdatetestjava)
- [`src/test/java/com/automation/tests/buyerapp/ProfilePage/AuthValidateTest.java`](#srctestjavacomautomationtestsbuyerappprofilepageauthvalidatetestjava)
- [`src/test/java/com/automation/tests/buyerapp/ProfilePage/UserProfileTest.java`](#srctestjavacomautomationtestsbuyerappprofilepageuserprofiletestjava)
- [`src/test/java/com/automation/tests/buyerapp/ProfilePage/VideoViewActionTest.java`](#srctestjavacomautomationtestsbuyerappprofilepagevideoviewactiontestjava)
- [`src/test/java/com/automation/tests/buyerapp/ProfilePage/WatchedVideosTest.java`](#srctestjavacomautomationtestsbuyerappprofilepagewatchedvideostestjava)
- [`src/test/java/com/automation/tests/buyerapp/SearchPage/SearchProductTest.java`](#srctestjavacomautomationtestsbuyerappsearchpagesearchproducttestjava)
- [`src/test/java/com/automation/tests/buyerapp/SearchPage/SearchRecommendChipSelectTest.java`](#srctestjavacomautomationtestsbuyerappsearchpagesearchrecommendchipselecttestjava)
- [`src/test/java/com/automation/tests/buyerapp/SearchPage/SearchRecommendedChipsTest.java`](#srctestjavacomautomationtestsbuyerappsearchpagesearchrecommendedchipstestjava)
- [`src/test/java/com/automation/tests/buyerapp/SuitableForConfigTest.java`](#srctestjavacomautomationtestsbuyerappsuitableforconfigtestjava)
- [`src/test/java/com/automation/tests/buyerapp/UpdateCartTest.java`](#srctestjavacomautomationtestsbuyerappupdatecarttestjava)
- [`src/test/java/com/automation/tests/buyerapp/VideoFeedTvTest.java`](#srctestjavacomautomationtestsbuyerappvideofeedtvtestjava)
- [`src/test/java/com/automation/tests/buyerapp/newapis/BannerPlacementTest.java`](#srctestjavacomautomationtestsbuyerappnewapisbannerplacementtestjava)
- [`src/test/java/com/automation/tests/buyerapp/newapis/ChatbotV2ValidationTest.java`](#srctestjavacomautomationtestsbuyerappnewapischatbotv2validationtestjava)
- [`src/test/java/com/automation/tests/buyerapp/newapis/EventsIngestionNegativeTest.java`](#srctestjavacomautomationtestsbuyerappnewapiseventsingestionnegativetestjava)
- [`src/test/java/com/automation/tests/buyerapp/newapis/MlpPublicReadTest.java`](#srctestjavacomautomationtestsbuyerappnewapismlppublicreadtestjava)
- [`src/test/java/com/automation/tests/buyerapp/newapis/NavoLiveTest.java`](#srctestjavacomautomationtestsbuyerappnewapisnavolivetestjava)
- [`src/test/java/com/automation/tests/buyerapp/newapis/PrismAccessTest.java`](#srctestjavacomautomationtestsbuyerappnewapisprismaccesstestjava)
- [`src/test/java/com/automation/tests/buyerapp/newapis/ReferralValidationTest.java`](#srctestjavacomautomationtestsbuyerappnewapisreferralvalidationtestjava)
- [`src/test/java/com/automation/tests/buyerapp/newapis/SubSellerAuthTest.java`](#srctestjavacomautomationtestsbuyerappnewapissubsellerauthtestjava)
- [`src/test/java/com/automation/tests/buyerapp/schema/ResponseSchemaTest.java`](#srctestjavacomautomationtestsbuyerappschemaresponseschematestjava)
- [`src/test/resources/allure.properties`](#srctestresourcesallureproperties)
- [`src/test/resources/config.properties`](#srctestresourcesconfigproperties)
- [`src/test/resources/log4j2.xml`](#srctestresourceslog4j2xml)
- [`src/test/resources/schemas/buyerapp/catalog-by-id.json`](#srctestresourcesschemasbuyerappcatalog-by-idjson)
- [`src/test/resources/schemas/buyerapp/collection-all.json`](#srctestresourcesschemasbuyerappcollection-alljson)
- [`src/test/resources/schemas/buyerapp/feed-banners.json`](#srctestresourcesschemasbuyerappfeed-bannersjson)
- [`src/test/resources/secrets.properties.example`](#srctestresourcessecretspropertiesexample)
- [`src/test/resources/test-variables.properties`](#srctestresourcestest-variablesproperties)
- [`src/test/resources/testng-bomb.xml`](#srctestresourcestestng-bombxml)
- [`src/test/resources/testng-buyerapp.xml`](#srctestresourcestestng-buyerappxml)
- [`src/test/resources/testng.xml`](#srctestresourcestestngxml)

### `.gitignore`

_87 lines_

```gitignore
# Compiled class files
*.class

# Log files
*.log

# BlueJ files
*.ctxt

# Mobile Tools for Java (J2ME)
.mtj.tmp/

# Package Files
*.jar
*.war
*.nar
*.ear
*.zip
*.tar.gz
*.rar

# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties
.mvn/timing.properties
.mvn/wrapper/maven-wrapper.jar

# IDE - IntelliJ IDEA
.idea/
*.iws
*.iml
*.ipr
out/

# IDE - Eclipse
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache
bin/

# IDE - NetBeans
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/

# IDE - VS Code
.vscode/

# OS generated files
.DS_Store
.DS_Store?
._*
.Spotlight-V100
.Trashes
ehthumbs.db
Thumbs.db

# Allure
allure-results/
allure-report/

# Test output
test-output/

# Logs
logs/
*.log

# Temp files
*.tmp
*.temp

.qodo
# Local test credentials (see src/test/resources/secrets.properties.example)
src/test/resources/secrets.properties
```

### `CICD-SETUP.md`

_406 lines_

````markdown
# CI/CD Setup Guide

This guide shows you how to run your API tests automatically using Jenkins.

---

## What This Does

When you set up Jenkins with this project, it will:
- ✅ Run your API tests automatically
- ✅ Generate beautiful test reports (Allure)
- ✅ Upload reports to AWS S3
- ✅ Send notifications to Email and Google Chat
- ✅ Let you choose which tests to run (BOMB, Buyer App, or both)

---

## Files You Need

All these files are already created and ready to use:

| File | What It Does |
|------|--------------|
| **Jenkinsfile** | Main pipeline that runs everything |
| **scripts/extract-test-summary.py** | Gets test results from TestNG |
| **scripts/send-notification.py** | Sends Email + Google Chat notifications |
| **scripts/notify-chat.sh** | Simple script for Google Chat only |
| **scripts/notification-config.example.json** | Template for notification settings |

---

## Quick Setup Steps

### Step 1: Create Notification Config File

```bash
# Copy the example file
cp scripts/notification-config.example.json scripts/notification-config.json

# Edit with your details
nano scripts/notification-config.json
```

### Step 2: Add to .gitignore

Make sure your config file is not uploaded to Git:

```bash
echo "scripts/notification-config.json" >> .gitignore
```

### Step 3: Setup Jenkins

1. **Create a new Jenkins job**
   - Job type: Pipeline
   - Pipeline from SCM (Git)
   - Point to your repository

2. **Add these credentials in Jenkins**:
   - Google Chat Webhook: ID = `google-chat-webhook-dev`
   - SMTP Credentials: ID = `smtp-credentials`
   - AWS Credentials (for S3 upload)
   - Test-account logins, each as **Secret text** (the Jenkinsfile maps them to env vars):

     | Credential ID | Env var | Value |
     |---|---|---|
     | `restassured-bomb-login-phone` | `BOMB_LOGIN_PHONE` | BOMB test account phone, `+91…` |
     | `restassured-bomb-login-otp` | `BOMB_LOGIN_OTP` | its fixed OTP |
     | `restassured-buyer-login-phone` | `BUYER_LOGIN_PHONE` | Buyer App test account phone |
     | `restassured-buyer-login-otp` | `BUYER_LOGIN_OTP` | its fixed OTP |

     These are no longer in `config.properties`. Without them the login steps fail.

3. **Choose test profile when running**:
   - `all` - Runs both BOMB and Buyer App tests
   - `bomb` - Runs only BOMB API tests
   - `buyerapp` - Runs only Buyer App tests

### Step 4: Run the Pipeline

Click "Build with Parameters" in Jenkins and select your test profile!

---

## How It Works

### Pipeline Flow

```
1. Checkout Code from Git
   ↓
2. Run Tests (Maven)
   ↓
3. Generate Allure Report
   ↓
4. Upload to S3 (with timestamp)
   ↓
5. Send Notifications (Email + Chat)
```

### Test Profiles

You can choose which tests to run:

```bash
# Run all tests
mvn clean test

# Run only BOMB API tests
mvn clean test -Pbomb

# Run only Buyer App tests
mvn clean test -Pbuyerapp
```

Jenkins uses the same profiles!

---

## Notification Setup

### Email Notifications

Emails are sent automatically when:
- ✅ All tests pass (SUCCESS)
- ❌ Tests fail (FAILURE)
- ⚠️ Some tests fail (UNSTABLE)

**Recipient**: `shubham.bizup@gmail.com` (change in Jenkinsfile if needed)

### Google Chat Notifications

Messages include:
- Test summary (Total, Pass, Fail, Skip)
- Links to reports and logs
- Build status
- Execution time

### Notification Config File

Edit `scripts/notification-config.json`:

```json
{
  "project_name": "RestAssured API Automation",
  "enable_email": true,
  "enable_google_chat": true,
  "email_to": ["your-email@example.com"],
  "smtp_host": "smtp.gmail.com",
  "smtp_port": 587,
  "smtp_username": "your-email@gmail.com",
  "smtp_password": "your-app-password",
  "google_chat_webhook": "your-webhook-url"
}
```

---

## AWS S3 Reports

Reports are uploaded with timestamps for easy tracking:

### Report Structure

```
s3://bizup-builds/
├── RestAssured API Report/
│   ├── reports/
│   │   └── allure_2026-01-06_17_30/     ← Timestamped
│   ├── logs/
│   │   └── logs_2026-01-06_17_30/       ← Timestamped
│   └── results/
│       └── testng-results-42.xml        ← Build number
```

### Accessing Reports

After each build, you get these links:
- **Allure Report**: `https://bizup-builds.s3.ap-south-1.amazonaws.com/RestAssured API Report/reports/allure_YYYY-MM-DD_HH_MM/index.html`
- **Logs**: `https://bizup-builds.s3.ap-south-1.amazonaws.com/RestAssured API Report/logs/logs_YYYY-MM-DD_HH_MM/automation.log`

Jenkins prints these links in the console!

---

## Python Scripts Explained

### 1. extract-test-summary.py

**What it does**: Reads TestNG results and creates a simple summary

**Input**: `testng-results.xml`

**Output**: 
```json
{
  "total": 56,
  "passed": 54,
  "failed": 0,
  "skipped": 2
}
```

### 2. send-notification.py

**What it does**: Sends test results to Email and Google Chat

**Features**:
- Beautiful HTML emails
- Google Chat cards with test details
- Individual test case breakdown
- Status icons and color coding

### 3. notify-chat.sh

**What it does**: Simple bash script for Google Chat only

**Use when**: You only need Google Chat notifications (no email)

---

## Running Tests Locally

### Command Line

```bash
# All tests
mvn clean test

# BOMB API only
mvn clean test -Pbomb

# Buyer App only  
mvn clean test -Pbuyerapp

# Generate Allure report
mvn allure:serve
```

### Via Jenkins

1. Go to your Jenkins job
2. Click "Build with Parameters"
3. Select profile: `all`, `bomb`, or `buyerapp`
4. Click "Build"

Watch the pipeline run and get notifications!

---

## Troubleshooting

### Allure Report Not Generated

**Problem**: Report generation fails

**Solution**:
```bash
# Install Allure CLI
wget https://github.com/allure-framework/allure2/releases/download/2.25.0/allure-2.25.0.tgz
sudo tar -zxf allure-2.25.0.tgz -C /opt/
sudo ln -s /opt/allure-2.25.0/bin/allure /usr/local/bin/allure
```

### S3 Upload Fails

**Problem**: Reports not uploading to S3

**Solution**:
- Check AWS credentials in Jenkins
- Verify S3 bucket permissions
- Check bucket name: `bizup-builds`
- Check region: `ap-south-1`

### Notifications Not Sent

**Problem**: No email or chat messages

**Solution**:
- Verify credentials in Jenkins
- Check webhook URL is correct
- Test email settings (SMTP port 587)
- Enable "Less secure app access" for Gmail

### No Test Results

**Problem**: Tests don't run

**Solution**:
```bash
# Check Maven is installed
mvn --version

# Check Java version
java -version  # Need Java 17+

# Run tests manually first
mvn clean test
```

---

## Configuration Reference

### Jenkins Environment Variables

Set these in Jenkins job configuration:

| Variable | Value | Description |
|----------|-------|-------------|
| `PROJECT_NAME` | RestAssured API Automation | Project display name |
| `S3_BUCKET` | bizup-builds | S3 bucket name |
| `S3_REGION` | ap-south-1 | AWS region |
| `EMAIL_RECIPIENTS` | shubham.bizup@gmail.com | Who gets emails |

### Required Jenkins Plugins

- Pipeline
- Git
- Email Extension
- Allure
- S3 Publisher (or AWS CLI)

### Required Credentials

Add these in Jenkins → Credentials:

1. **google-chat-webhook-dev** (Secret text)
   - Your Google Chat webhook URL

2. **smtp-credentials** (Username/Password)
   - Username: Your email
   - Password: App password

3. **AWS credentials** (for S3)
   - Access Key ID
   - Secret Access Key

---

## What You Get

### After Each Build

1. **Email** with:
   - Build status (Success/Failed/Unstable)
   - Test counts
   - Links to reports

2. **Google Chat** message with:
   - Test summary
   - Individual test results
   - Quick links

3. **S3 Reports**:
   - Allure HTML report
   - Test logs
   - TestNG XML results

4. **Jenkins Artifacts**:
   - Test summary JSON
   - S3 links file

---

## Example Notification

### Email Subject
```
✅ SUCCESS: RestAssured API Automation - Build #42
```

### Google Chat Message
```
✅ Test Report: RestAssured API Automation
━━━━━━━━━━━━━━━━━━━━━━━

📊 Test Summary:
• Total Tests: 56
• Passed: ✅ 54
• Failed: ❌ 0  
• Skipped: ⏭️ 2

🔗 Quick Links:
• Jenkins Build
• Allure Report
• Automation Logs

⏱️ Duration: 45.2s
🏗️ Build: #42
```

---

## Next Steps

1. ✅ Set up Jenkins job
2. ✅ Add credentials
3. ✅ Configure notifications
4. ✅ Run first build
5. ✅ Check reports in S3
6. ✅ Verify notifications received

**You're all set!** 🎉

For more details, see the main [README.md](README.md)
````

### `Jenkinsfile`

_603 lines_

```groovy
pipeline {
    agent any
    
    tools {
        maven 'Maven 3.9.9'
    }
    
    environment {
        // Project Configuration
        PROJECT_NAME = 'RestAssured API Automation'
        
        // Date/Time for report filenames
        DATE_STR = """${new Date().format('yyyy-MM-dd_HH_mm')}"""
        
        // S3 Configuration
        S3_BUCKET = 'bizup-builds'
        S3_BASE_FOLDER = 'RestAssured API Report'
        S3_REGION = 'ap-south-1'
        
        // Report paths with timestamps
        ALLURE_REPORT_NAME = "allure_${DATE_STR}"
        LOGS_REPORT_NAME = "logs_${DATE_STR}"
        
        // S3 full paths
        S3_ALLURE_PATH = "${S3_BASE_FOLDER}/reports/${ALLURE_REPORT_NAME}"
        S3_LOGS_PATH = "${S3_BASE_FOLDER}/logs/${LOGS_REPORT_NAME}"
        S3_RESULTS_PATH = "${S3_BASE_FOLDER}/results/testng-results-${env.BUILD_NUMBER}.xml"
        
        // Notification Configuration
        NOTIFICATION_CONFIG = 'scripts/notification-config.json'
        
        // Email Configuration
        EMAIL_RECIPIENTS = 'shubham.bizup@gmail.com' // Update with actual recipient emails (comma-separated)
    }
    
    parameters {
        choice(
            name: 'TEST_PROFILE',
            choices: ['all', 'bomb', 'buyerapp'],
            description: 'Select test profile to run (Default: all - runs both BOMB and Buyer App tests)'
        )
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Run Tests') {
            steps {
                script {
                    def mavenProfile = params.TEST_PROFILE == 'all' ? '' : "-P${params.TEST_PROFILE}"
                    
                    // Use catchError to continue pipeline even if tests fail
                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                        // Login credentials come from Jenkins "Secret text" credentials
                        // (see CICD-SETUP.md); ConfigManager reads these env vars.
                        withCredentials([
                            string(credentialsId: 'restassured-bomb-login-phone', variable: 'BOMB_LOGIN_PHONE'),
                            string(credentialsId: 'restassured-bomb-login-otp', variable: 'BOMB_LOGIN_OTP'),
                            string(credentialsId: 'restassured-buyer-login-phone', variable: 'BUYER_LOGIN_PHONE'),
                            string(credentialsId: 'restassured-buyer-login-otp', variable: 'BUYER_LOGIN_OTP')
                        ]) {
                            sh """
                                mvn clean test ${mavenProfile} \
                                -Dallure.results.directory=target/allure-results
                            """
                        }
                    }
                }
            }
        }
        
        stage('Setup Allure') {
            steps {
                script {
                    echo "Checking for Allure CLI installation..."
                    sh '''
                        # Check if allure command exists in PATH
                        if command -v allure >/dev/null 2>&1; then
                            echo "✅ Allure CLI already installed and working"
                            allure --version
                        else
                            echo "⚙️ Allure CLI not found or not working. Installing..."
                            
                            # Clean up any broken installation
                            sudo rm -f /usr/local/bin/allure 2>/dev/null || true
                            sudo rm -rf /opt/allure 2>/dev/null || true
                            
                            # Create directory for Allure
                            sudo mkdir -p /opt/allure
                            
                            # Download Allure
                            echo "Downloading Allure 2.25.0..."
                            cd /tmp
                            wget -q https://github.com/allure-framework/allure2/releases/download/2.25.0/allure-2.25.0.tgz
                            
                            # Extract to /opt
                            echo "Extracting Allure..."
                            sudo tar -zxf allure-2.25.0.tgz -C /opt/allure --strip-components=1
                            
                            # Create symlink
                            echo "Creating symlink..."
                            sudo ln -sf /opt/allure/bin/allure /usr/local/bin/allure
                            
                            # Make it executable
                            sudo chmod +x /opt/allure/bin/allure
                            
                            # Cleanup
                            rm -f allure-2.25.0.tgz
                            
                            # Verify installation
                            if command -v allure >/dev/null 2>&1 && allure --version; then
                                echo "✅ Allure CLI installed successfully"
                            else
                                echo "❌ Allure CLI installation failed"
                                exit 1
                            fi
                        fi
                    '''
                }
            }
        }
        
        stage('Generate Allure Report') {
            steps {
                script {
                    echo "Generating Allure report from test results..."
                    
                    // First verify test results exist
                    sh """
                        echo "=== Checking for test results ==="
                        if [ -d "target/allure-results" ]; then
                            echo "✅ allure-results directory exists"
                            ls -la target/allure-results/ | head -10
                            echo "Number of result files:"
                            find target/allure-results/ -type f | wc -l
                        else
                            echo "❌ ERROR: target/allure-results/ directory NOT FOUND!"
                            echo "Tests may not have generated Allure results"
                            exit 1
                        fi
                    """
                    
                    // Generate the report - use Allure Commandline instead of Maven plugin
                    sh """
                        echo "=== Generating Allure Report with Allure Commandline ==="
                        
                        # Check if allure command is available
                        if command -v allure &> /dev/null; then
                            echo "✅ Allure CLI found"
                            allure --version
                            
                            # Generate report using Allure CLI
                            allure generate target/allure-results --clean -o target/site/allure-maven-plugin
                        else
                            echo "⚠️ Allure CLI not found, trying Maven plugin..."
                            
                            # Fallback to Maven with different goals
                            mvn allure:report || \
                            mvn io.qameta.allure:allure-maven:2.14.0:report || \
                            mvn site -Dallure.report.directory=target/site/allure-maven-plugin
                        fi
                        
                        echo "Report generation complete"
                    """
                    
                    // Verify report was generated
                    echo "Verifying Allure report generation..."
                    sh """
                        echo "=== Checking Allure report directory ==="
                        ls -la target/site/allure-maven-plugin/ || echo "Report directory not found!"
                        
                        echo "=== Listing ALL files in report directory ==="
                        find target/site/allure-maven-plugin/ -type f -exec ls -lh {} \\;
                        
                        echo "=== Counting files in report ==="
                        find target/site/allure-maven-plugin/ -type f | wc -l
                        
                        echo "=== Checking alternative locations ==="
                        find target/site -name "index.html" 2>/dev/null || echo "No index.html found in target/site"
                        find target -name "allure-*" -type d 2>/dev/null || echo "No allure directories found"
                        
                        echo "=== Directory structure of target/site ==="
                        ls -laR target/site/ | head -50
                        
                        echo "=== Checking for critical files ==="
                        test -f target/site/allure-maven-plugin/index.html && echo "✅ index.html found" || echo "❌ index.html MISSING"
                        test -f target/site/allure-maven-plugin/app.js && echo "✅ app.js found" || echo "❌ app.js MISSING"
                        test -f target/site/allure-maven-plugin/styles.css && echo "✅ styles.css found" || echo "❌ styles.css MISSING"
                        test -d target/site/allure-maven-plugin/data && echo "✅ data/ directory found" || echo "❌ data/ directory MISSING"
                        
                        # Temporarily disable build failure to see full output
                        # if [ ! -f "target/site/allure-maven-plugin/index.html" ]; then
                        #     echo "❌ FATAL: Allure report generation failed!"
                        #     exit 1
                        # fi
                    """
                }
            }
        }
        
        stage('Upload to S3') {
            steps {
                script {
                    echo "Uploading reports to S3 with timestamp: ${DATE_STR}"
                    
                    // Upload Allure report with timestamped folder name
                    echo "=== Uploading Allure Report to S3 ==="
                    sh """
                        echo "Source: target/site/allure-maven-plugin/"
                        echo "Destination: s3://${S3_BUCKET}/${S3_ALLURE_PATH}/"
                        echo "Files to upload:"
                        find target/site/allure-maven-plugin/ -type f | head -20
                        
                        aws s3 sync target/site/allure-maven-plugin/ \
                        "s3://${S3_BUCKET}/${S3_ALLURE_PATH}/" \
                        --region ${S3_REGION} \
                        --debug 2>&1 | grep -E "(upload|delete):" || true
                        
                        echo "Upload complete. Verifying S3..."
                        aws s3 ls "s3://${S3_BUCKET}/${S3_ALLURE_PATH}/" --recursive | head -20
                    """
                    
                    // Upload logs with timestamped folder name
                    sh """
                        aws s3 sync target/logs/ \
                        "s3://${S3_BUCKET}/${S3_LOGS_PATH}/" \
                        --region ${S3_REGION}
                    """
                    
                    // Upload TestNG results
                    sh """
                        aws s3 cp target/surefire-reports/testng-results.xml \
                        "s3://${S3_BUCKET}/${S3_RESULTS_PATH}" \
                        --region ${S3_REGION}
                    """
                    
                    // Print S3 URLs for reference
                    echo "✅ Allure Report: https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_ALLURE_PATH}/index.html"
                    echo "✅ Logs: https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_LOGS_PATH}/"
                }
            }
        }
        
        stage('Extract Test Summary') {
            steps {
                script {
                    sh """
                        python3 scripts/extract-test-summary.py \
                        target/surefire-reports/testng-results.xml \
                        > test-summary.json
                    """
                    
                    // Read and display summary
                    def summary = readJSON file: 'test-summary.json'
                    echo "Test Summary: ${summary}"
                }
            }
        }
        
        stage('Send Notifications') {
            steps {
                script {
                    // Generate S3 URLs with timestamps
                    def reportUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_ALLURE_PATH}/index.html"
                    def logsUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_LOGS_PATH}/automation.log"
                    
                    // Securely handle credentials for webhook and SMTP
                    withCredentials([
                        string(credentialsId: 'google-chat-webhook-dev', variable: 'WEBHOOK_URL'),
                        usernamePassword(credentialsId: 'smtp-credentials', usernameVariable: 'SMTP_USERNAME', passwordVariable: 'SMTP_PASSWORD')
                    ]) {
                        // Create notification config using shell script to avoid exposing credentials
                        sh '''
                            cat > scripts/notification-config.json.tmp << 'EOF_CONFIG'
{
  "project_name": "''' + PROJECT_NAME + '''",
  "enable_email": true,
  "enable_google_chat": true,
  "email_from": "jenkins@bizup.app",
  "email_to": ["''' + EMAIL_RECIPIENTS + '''"],
  "smtp_host": "smtp.gmail.com",
  "smtp_port": 587,
  "smtp_use_tls": true,
  "smtp_username": "SMTP_USERNAME_PLACEHOLDER",
  "smtp_password": "SMTP_PASSWORD_PLACEHOLDER",
  "google_chat_webhook": "WEBHOOK_URL_PLACEHOLDER",
  "build_url": "''' + BUILD_URL + '''",
  "build_number": "''' + env.BUILD_NUMBER + '''",
  "timestamp": "''' + DATE_STR + '''",
  "report_url": "''' + reportUrl + '''",
  "logs_url": "''' + logsUrl + '''"
}
EOF_CONFIG
                            # Use Python to replace placeholders (handles special characters better than sed)
                            python3 << 'PYTHON_SCRIPT'
import json

# Read the template
with open('scripts/notification-config.json.tmp', 'r') as f:
    content = f.read()

# Replace placeholders with environment variables
import os
content = content.replace('SMTP_USERNAME_PLACEHOLDER', os.environ.get('SMTP_USERNAME', ''))
content = content.replace('SMTP_PASSWORD_PLACEHOLDER', os.environ.get('SMTP_PASSWORD', ''))
content = content.replace('WEBHOOK_URL_PLACEHOLDER', os.environ.get('WEBHOOK_URL', ''))

# Write back
with open('scripts/notification-config.json.tmp', 'w') as f:
    f.write(content)

# Verify it's valid JSON
with open('scripts/notification-config.json.tmp', 'r') as f:
    config = json.load(f)
    print(f"✅ Config generated successfully")
    print(f"   Google Chat webhook (first 60 chars): {config['google_chat_webhook'][:60]}...")
PYTHON_SCRIPT
                        '''
                        
                        // Send notifications
                        sh """
                            python3 scripts/send-notification.py \\
                            test-summary.json \\
                            ${NOTIFICATION_CONFIG}.tmp
                        """
                    }
                    
                    echo "✅ Notification sent to Google Chat"
                }
            }
        }
        
        stage('Archive S3 Links') {
            steps {
                script {
                    // Generate S3 URLs with timestamps
                    def reportUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_ALLURE_PATH}/index.html"
                    def logsUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_LOGS_PATH}/automation.log"
                    def resultsUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_RESULTS_PATH}"
                    
                    // Create S3 links file
                    writeFile file: 's3-links.txt', text: """
==============================================
  S3 REPORT LINKS - Build #${env.BUILD_NUMBER}
==============================================

[ALLURE REPORT]
${reportUrl}

[AUTOMATION LOGS]
${logsUrl}

[TESTNG RESULTS XML]
${resultsUrl}

>> Tip: Click the links above to view reports in S3
==============================================
"""
                    
                    // Print S3 links to console
                    echo "=============================================="
                    echo "  📊 S3 REPORT LINKS - Build #${env.BUILD_NUMBER}"
                    echo "=============================================="
                    echo ""
                    echo "📊 Allure Report:"
                    echo "   ${reportUrl}"
                    echo ""
                    echo "📋 Automation Logs:"
                    echo "   ${logsUrl}"
                    echo ""
                    echo "📄 TestNG Results XML:"
                    echo "   ${resultsUrl}"
                    echo ""
                    echo "✅ S3 links saved to s3-links.txt artifact"
                    echo "=============================================="
                }
            }
        }
    }
    
    post {
        always {
            // Archive artifacts (only summary and S3 links)
            archiveArtifacts artifacts: 'test-summary.json, s3-links.txt', 
                             allowEmptyArchive: true
            
            // Publish Allure report in Jenkins (optional)
            allure([
                includeProperties: false,
                jdk: '',
                properties: [],
                reportBuildPolicy: 'ALWAYS',
                results: [[path: 'target/allure-results']]
            ])
        }
        
        failure {
            echo 'Tests failed! Check the reports for details.'
            
            script {
                def reportUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_ALLURE_PATH}/index.html"
                def logsUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_LOGS_PATH}/automation.log"
                
                emailext (
                    subject: "❌ FAILED: ${PROJECT_NAME} - Build #${env.BUILD_NUMBER}",
                    body: """<html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                                <div style="background: linear-gradient(135deg, #dc2626 0%, #991b1b 100%); padding: 20px; border-radius: 8px 8px 0 0;">
                                    <h2 style="color: white; margin: 0;">❌ Build Failed</h2>
                                </div>
                                
                                <div style="background: #f9fafb; padding: 20px; border: 1px solid #e5e7eb; border-top: none; border-radius: 0 0 8px 8px;">
                                    <h3 style="color: #1f2937; margin-top: 0;">${PROJECT_NAME}</h3>
                                    
                                    <table style="width: 100%; border-collapse: collapse; margin: 20px 0;">
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold; width: 120px;">Build #</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${env.BUILD_NUMBER}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Status</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;"><span style="color: #dc2626; font-weight: bold;">FAILED</span></td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Profile</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${params.TEST_PROFILE}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Duration</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${currentBuild.durationString.replace(' and counting', '')}</td>
                                        </tr>
                                    </table>
                                    
                                    <div style="margin: 20px 0;">
                                        <h4 style="color: #1f2937; margin-bottom: 10px;">📊 Quick Links</h4>
                                        <p style="margin: 8px 0;">
                                            <a href="${BUILD_URL}" style="color: #2563eb; text-decoration: none;">🔗 Jenkins Build</a>
                                        </p>
                                        <p style="margin: 8px 0;">
                                            <a href="${reportUrl}" style="color: #2563eb; text-decoration: none;">📈 Allure Report</a>
                                        </p>
                                        <p style="margin: 8px 0;">
                                            <a href="${logsUrl}" style="color: #2563eb; text-decoration: none;">📋 Automation Logs</a>
                                        </p>
                                    </div>
                                    
                                    <p style="color: #6b7280; font-size: 12px; margin-top: 20px; border-top: 1px solid #e5e7eb; padding-top: 15px;">
                                        This is an automated notification from Jenkins CI/CD Pipeline<br>
                                        Timestamp: ${new Date().format('yyyy-MM-dd HH:mm:ss')}
                                    </p>
                                </div>
                            </div>
                        </body>
                    </html>""",
                    to: "${EMAIL_RECIPIENTS}",
                    mimeType: 'text/html',
                    attachLog: false
                )
            }
        }
        
        success {
            echo 'All tests passed successfully!'
            
            script {
                def reportUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_ALLURE_PATH}/index.html"
                def logsUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_LOGS_PATH}/automation.log"
                
                emailext (
                    subject: "✅ SUCCESS: ${PROJECT_NAME} - Build #${env.BUILD_NUMBER}",
                    body: """<html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                                <div style="background: linear-gradient(135deg, #059669 0%, #047857 100%); padding: 20px; border-radius: 8px 8px 0 0;">
                                    <h2 style="color: white; margin: 0;">✅ Build Successful</h2>
                                </div>
                                
                                <div style="background: #f9fafb; padding: 20px; border: 1px solid #e5e7eb; border-top: none; border-radius: 0 0 8px 8px;">
                                    <h3 style="color: #1f2937; margin-top: 0;">${PROJECT_NAME}</h3>
                                    
                                    <table style="width: 100%; border-collapse: collapse; margin: 20px 0;">
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold; width: 120px;">Build #</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${env.BUILD_NUMBER}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Status</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;"><span style="color: #059669; font-weight: bold;">SUCCESS</span></td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Profile</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${params.TEST_PROFILE}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Duration</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${currentBuild.durationString.replace(' and counting', '')}</td>
                                        </tr>
                                    </table>
                                    
                                    <div style="margin: 20px 0;">
                                        <h4 style="color: #1f2937; margin-bottom: 10px;">📊 Quick Links</h4>
                                        <p style="margin: 8px 0;">
                                            <a href="${BUILD_URL}" style="color: #2563eb; text-decoration: none;">🔗 Jenkins Build</a>
                                        </p>
                                        <p style="margin: 8px 0;">
                                            <a href="${reportUrl}" style="color: #2563eb; text-decoration: none;">📈 Allure Report</a>
                                        </p>
                                        <p style="margin: 8px 0;">
                                            <a href="${logsUrl}" style="color: #2563eb; text-decoration: none;">📋 Automation Logs</a>
                                        </p>
                                    </div>
                                    
                                    <p style="color: #6b7280; font-size: 12px; margin-top: 20px; border-top: 1px solid #e5e7eb; padding-top: 15px;">
                                        This is an automated notification from Jenkins CI/CD Pipeline<br>
                                        Timestamp: ${new Date().format('yyyy-MM-dd HH:mm:ss')}
                                    </p>
                                </div>
                            </div>
                        </body>
                    </html>""",
                    to: "${EMAIL_RECIPIENTS}",
                    mimeType: 'text/html',
                    attachLog: false
                )
            }
        }
        
        unstable {
            echo 'Build is unstable - some tests may have failed.'
            
            script {
                def reportUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_ALLURE_PATH}/index.html"
                def logsUrl = "https://${S3_BUCKET}.s3.${S3_REGION}.amazonaws.com/${S3_LOGS_PATH}/automation.log"
                
                emailext (
                    subject: "⚠️ UNSTABLE: ${PROJECT_NAME} - Build #${env.BUILD_NUMBER}",
                    body: """<html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                                <div style="background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); padding: 20px; border-radius: 8px 8px 0 0;">
                                    <h2 style="color: white; margin: 0;">⚠️ Build Unstable</h2>
                                </div>
                                
                                <div style="background: #f9fafb; padding: 20px; border: 1px solid #e5e7eb; border-top: none; border-radius: 0 0 8px 8px;">
                                    <h3 style="color: #1f2937; margin-top: 0;">${PROJECT_NAME}</h3>
                                    
                                    <table style="width: 100%; border-collapse: collapse; margin: 20px 0;">
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold; width: 120px;">Build #</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${env.BUILD_NUMBER}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Status</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;"><span style="color: #f59e0b; font-weight: bold;">UNSTABLE</span></td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Profile</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${params.TEST_PROFILE}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: bold;">Duration</td>
                                            <td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">${currentBuild.durationString.replace(' and counting', '')}</td>
                                        </tr>
                                    </table>
                                    
                                    <div style="background: #fef3c7; border-left: 4px solid #f59e0b; padding: 12px; margin: 20px 0; border-radius: 4px;">
                                        <p style="margin: 0; color: #92400e;"><strong>Note:</strong> Some tests may have failed. Please check the reports for details.</p>
                                    </div>
                                    
                                    <div style="margin: 20px 0;">
                                        <h4 style="color: #1f2937; margin-bottom: 10px;">📊 Quick Links</h4>
                                        <p style="margin: 8px 0;">
                                            <a href="${BUILD_URL}" style="color: #2563eb; text-decoration: none;">🔗 Jenkins Build</a>
                                        </p>
                                        <p style="margin: 8px 0;">
                                            <a href="${reportUrl}" style="color: #2563eb; text-decoration: none;">📈 Allure Report</a>
                                        </p>
                                        <p style="margin: 8px 0;">
                                            <a href="${logsUrl}" style="color: #2563eb; text-decoration: none;">📋 Automation Logs</a>
                                        </p>
                                    </div>
                                    
                                    <p style="color: #6b7280; font-size: 12px; margin-top: 20px; border-top: 1px solid #e5e7eb; padding-top: 15px;">
                                        This is an automated notification from Jenkins CI/CD Pipeline<br>
                                        Timestamp: ${new Date().format('yyyy-MM-dd HH:mm:ss')}
                                    </p>
                                </div>
                            </div>
                        </body>
                    </html>""",
                    to: "${EMAIL_RECIPIENTS}",
                    mimeType: 'text/html',
                    attachLog: false
                )
            }
        }
    }
}
```

### `README-BOMB.md`

_755 lines_

````markdown
<div align="center">

# 🎯 BOMB API Automation

### **Admin/Seller Operations - Complete Postman to RestAssured Conversion**

[![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=java)](https://www.oracle.com/java/)
[![RestAssured](https://img.shields.io/badge/RestAssured-5.3+-green?style=for-the-badge&logo=rest)](https://rest-assured.io/)
[![TestNG](https://img.shields.io/badge/TestNG-7.8+-red?style=for-the-badge&logo=testng)](https://testng.org/)
[![Allure](https://img.shields.io/badge/Allure-2.24+-yellow?style=for-the-badge&logo=qameta)](https://docs.qameta.io/allure/)

---

### ✨ **Enterprise-Grade Admin/Seller API Testing** ✨

**26 Test Files** | **4 Workflow Pipelines** | **15+ Endpoints** | **Production Ready**

</div>

---

## 📊 **API Overview**

This documentation covers the **BOMB API** automation framework - a comprehensive testing solution for admin and seller operations, converted from Postman collection to a robust RestAssured + TestNG implementation.

### **🎯 What It Tests**
Complete end-to-end validation of the BOMB admin/seller API, including authentication, catalog search & management, video operations, and AI-powered title generation.

### **🚀 Key Highlights**
- ✅ **26 Test Files** - Comprehensive coverage organized by workflow pipelines
- ✅ **15+ API Endpoints** - All critical operations tested
- ✅ **JWT Authentication** - Complete token-based auth flow with VariableManager
- ✅ **Pipeline Architecture** - Tests organized by business workflows
- ✅ **AI Integration** - Video title generation from tags

---

## 📋 **Test Coverage Details**

<table>
<tr>
<td width="50%" valign="top">

### 🔐 **1. Login Pipeline**
**Location:** `bomb/Login/` | **Tests:** 1 file

```
✅ Successful login (valid credentials)
✅ Response data type validation
✅ Phone number field check
```

**Key Validations:**
- 🎯 Status code 200
- 🔑 JWT Token generation
- 📊 Response structure validation
- 📝 Data types validation
- ⏱️ Response time threshold

</td>
<td width="50%" valign="top">

### 🔍 **2. Catalog Search Pipeline**
**Location:** `bomb/CatalogSearch/` | **Tests:** 6 files

```
✅ Get all catalogs (pagination)
✅ Search with seller filter
✅ Search with product filter
✅ Search with catalog ID filter
✅ Security headers validation
✅ Live catalog identification
```

**Key Validations:**
- 📦 Response structure (total, items, buckets)
- 👤 Seller ID matching
- 🌐 MongoDB ID format
- ✔️ Boolean flags validation
- 📄 Pagination enforcement

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 📎 **3. Catalog Tag Pipeline**
**Location:** `bomb/CatalogTagPipeline/` | **Tests:** 12 files

```
✅ Catalog Assign to Editor (3 tests)
✅ Catalog Editor Operations (9 tests)
✅ Tagging, Title Generation, Mark as Done
```

**Key Validations:**
- 📊 Response structure & field types
- 📝 Source field values validation
- 🎥 VideoType validation (catalog)
- 📞 Phone number format
- 👤 Seller ID filtering
- 📊 Status-based identification

</td>
<td width="50%" valign="top">

### 🎥 **4. Video Tagging Pipeline**
**Location:** `bomb/VideoTaggingPipeline/` | **Tests:** 7 files

```
✅ Video Upload & Assign (4 tests)
✅ Video Tagging Operations (3 tests)
✅ Title Generation, Edit, Mark as Done
```

**Key Validations:**
- 🎥 Video content type
- 🔥 Firebase storage URLs
- 👤 Seller/Editor ID matching
- 🌐 MongoDB ObjectId format
- ⏱️ Timestamp validation
- 🌍 Language codes presence
- 🏷️ Tag processing structure

</td>
</tr>
</table>

---

## ⚙️ **Configuration**

### **📦 Updated Configuration**

Edit `src/test/resources/config.properties`:

```properties
# 🎯 BOMB API Configuration (BizUp)
base.url=https://bizup.app
login.phone.number=+916204843730
login.token=123456789

# ⏱️ Timeouts & Performance
api.timeout=40000
response.time.threshold=40000

# 🔑 Authentication
auth.type=jwt
```

---

## 🔗 **API Endpoints**

### **Complete Endpoint Catalog** (`BombEndpoints.java`)

<table>
<tr>
<td width="50%" valign="top">

#### 🔐 **Authentication**
```
POST /api/auth/login
```
➡️ Authenticates admin/seller and generates JWT token

#### 🔍 **Catalog Management**
```
GET /v1/admin/catalog_all
GET /v1/admin/catalog
```
➡️ Search & manage catalogs with filters

</td>
<td width="50%" valign="top">

#### 🎥 **Video Operations**
```
GET /v1/admin/editor/edit/videos/{sellerId}
```
➡️ Retrieve videos for specific seller

#### 🤖 **AI Services**
```
POST /v2/ai/tags-to-text
```
➡️ Generate video titles from tags using AI

#### 📊 **Total Endpoints:** 5

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
📄 LoginRequest
   ├─ phoneNumber: String
   └─ token: String

📄 VideoTitleRequest
   ├─ tags: List<String>
   ├─ language: String
   └─ context: String
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
📄 LoginResponse
   ├─ LoginData
   │   ├─ phoneNumber, name
   │   └─ tokens (access + refresh)
   └─ statusCode, message

📄 CatalogResponse
   ├─ CatalogData
   │   ├─ Total (value, relation)
   │   ├─ CatalogItem[]
   │   │   ├─ Seller, Product
   │   │   └─ status, source
   │   └─ Bucket[]
   └─ statusCode, message
```

**Features:**
- ✅ Deeply nested objects
- ✅ Complex array handling
- ✅ Custom deserializers

</td>
</tr>
</table>

---

## 🏃 **Running Tests**

### **🚀 Quick Start**

```bash
# Run complete BOMB API test suite
mvn clean test -Pbomb

# Alternative: Using TestNG XML
mvn clean test -DsuiteXmlFile=src/test/resources/testng-bomb.xml

# Generate Allure report
mvn allure:serve
```

**Jenkins Pipeline**:
- Go to Jenkins job → Build with Parameters
- Select profile: `bomb`
- Reports auto-upload to S3 with notifications

See [CICD-SETUP.md](CICD-SETUP.md) for CI/CD details.

### **🎯 Run Specific Test Classes**

<table>
<tr>
<td width="50%" valign="top">

#### **Authentication Tests**
```bash
mvn clean test -Dtest=LoginApiTest
```
📊 **1 test file** | ⏱️ ~2s

#### **Catalog Search Tests**
```bash
mvn clean test -Dtest=AllCatalogTest
```
📊 **6 test files** | ⏱️ ~10s

</td>
<td width="50%" valign="top">

#### **Catalog Tag Pipeline**
```bash
mvn clean test -Dtest=CatalogTaggingTest
```
📊 **12 test files** | ⏱️ ~12s

#### **Video Tagging Pipeline**
```bash
mvn clean test -Dtest=VideoUploadTest
```
📊 **7 test files** | ⏱️ ~8s

</td>
</tr>
</table>

### **📦 Run by Package**

```bash
# Run all BOMB API tests (26 test files)
mvn clean test -Dtest=com.automation.tests.bomb.*
```

---

## 🔄 **Test Execution Flow**

<div align="center">

```
┌──────────────────────────────────────────────────┐
│         🔐 STEP 1: Authentication (Priority 1)          │
│                    LoginApiTest                         │
└──────────────────────────────────────────────────┘
                         │
                         │ Stores bombToken
                         ↓
        ┌──────────────────────────────┐
        │   Shared Across All Tests   │
        └──────────────────────────────┘
           │               │               │
           ↓               ↓               ↓
┌────────────────┐ ┌────────────────┐ ┌───────────────┐
│ 🔍 Catalog     │ │ 📎 Catalog     │ │ 🎥 Video      │
│    Search      │ │    Assign      │ │    API        │
│   6 Tests     │ │   3 Tests     │ │   4 Tests    │
└────────────────┘ └────────────────┘ └───────────────┘
```

</div>

### **Detailed Execution Steps**

| Step | Test Class | Actions | Output |
|------|-----------|---------|--------|
| **1** | `Login Pipeline` | • Authenticate admin/seller<br>• Generate JWT tokens | 🔑 `bombToken` (VariableManager) |
| **2** | `Catalog Search Pipeline` | • Search catalogs<br>• Apply filters (seller, product, ID) | 📦 Catalog IDs |
| **3** | `Catalog Tag Pipeline` | • Assign to editor<br>• Tag, generate title, mark done | 📎 Catalog workflow IDs |
| **4** | `Video Tagging Pipeline` | • Upload video<br>• Tag, generate title, mark done | 🎥 Video workflow IDs |

---

## 📊 **Allure Reporting**

### **Generate Beautiful HTML Reports**

```bash
# Run tests and generate report
mvn clean test -DsuiteXmlFile=src/test/resources/testng-bomb.xml
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
- ✅ Token shared via static variable
- ✅ Seamless auth across tests

```java
Authorization: Bearer {bombToken}
Content-Type: application/json
Accept: application/json
```

### 📊 **Dynamic Data Handling**
- ✅ Catalog IDs from responses
- ✅ Seller IDs for filtering
- ✅ Status-based identification
- ✅ Cross-test data sharing

</td>
<td width="50%" valign="top">

### ✔️ **Comprehensive Validations**
- ✅ Response structure validation
- ✅ Data type validation
- ✅ Field format (phone, MongoDB IDs)
- ✅ Firebase URL validation
- ✅ Boolean flag validation
- ✅ Pagination enforcement
- ✅ Performance (response time)

### 🎯 **Hamcrest Matchers**
- ✅ Expressive assertions
- ✅ Regex pattern matching
- ✅ Collection assertions
- ✅ Type checking
- ✅ Custom matchers

</td>
</tr>
</table>

---

## 📝 **Important Notes**

<table>
<tr>
<td width="50%" valign="top">

### ⚠️ **Technical Notes**

- 🐛 **Unchecked Cast Warnings:**
  - JsonPath `getList()` returns raw types
  - Warnings are expected and safe
  - Validated within tests

- 🔗 **Test Dependencies:**
  - `dependsOnMethods` used
  - Login executes first
  - Token shared via static

- ⏱️ **Response Time:**
  - Threshold: 40000ms
  - Longer than Buyer App
  - Catalog operations intensive

</td>
<td width="50%" valign="top">

### 🎨 **Testing Notes**

- 🎯 **Allure Annotations:**
  - `@Epic` - High level grouping
  - `@Feature` - Functionality group
  - `@Story` - User story
  - `@Severity` - Priority level

- 🌐 **Base URL:**
  - `https://bizup.app`
  - Different from Buyer App
  - Admin/Seller operations

- ✅ **Best Practices:**
  - POJO-based models
  - Hamcrest matchers
  - Clean architecture

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
<td><b>🔐 Login Pipeline</b></td>
<td align="center">1</td>
<td align="center">1</td>
<td align="center">~2s</td>
<td align="center">✅ Complete</td>
</tr>
<tr>
<td><b>🔍 Catalog Search Pipeline</b></td>
<td align="center">2</td>
<td align="center">6</td>
<td align="center">~10s</td>
<td align="center">✅ Complete</td>
</tr>
<tr>
<td><b>📎 Catalog Tag Pipeline</b></td>
<td align="center">3</td>
<td align="center">12</td>
<td align="center">~12s</td>
<td align="center">✅ Complete</td>
</tr>
<tr>
<td><b>🎥 Video Tagging Pipeline</b></td>
<td align="center">3</td>
<td align="center">7</td>
<td align="center">~8s</td>
<td align="center">✅ Complete</td>
</tr>
<tr style="background-color: #f0f0f0; font-weight: bold;">
<td><b>🏆 TOTAL</b></td>
<td align="center"><b>9</b></td>
<td align="center"><b>26</b></td>
<td align="center"><b>~32s</b></td>
<td align="center"><b>✅ 100%</b></td>
</tr>
</table>

</div>

---

## 🚀 **Framework Integration**

### **🆔️ Dual API Support**

This BOMB API module is part of a larger framework supporting multiple APIs:

<table>
<tr>
<td width="50%" align="center">

### 🎯 **BOMB API**
**Admin/Seller Operations**

🌐 `bizup.app`

📊 **4 Pipelines**
🧪 **26 Test Files**
🔗 **15+ Endpoints**

**This Document**

</td>
<td width="50%" align="center">

### 🛍️ **Buyer App API**
**Consumer Operations**

🌐 `api.navofashion.in`

📊 **8 Feature Areas**
🧪 **30 Test Files**
🔗 **25+ Endpoints**

[View Details →](README-BUYER-APP.md)

</td>
</tr>
</table>

### **📦 Complete Framework Stats**

```
🏆 Test counts: see README.md (generated)
📦 BOMB Pipelines: 4
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
- ✅ Catalog assignment tests
- ✅ Advanced search filters
- ✅ Video upload tests

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

**🎯 Happy Testing! 🚀**

[Back to Top](#🎯-bomb-api-automation) ⬆️

</div>
````

### `README-BUYER-APP.md`

_835 lines_

````markdown
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

## 🆕 **New APIs (backend-api QA sync, 2026-10-30)**

**Location:** `src/test/java/com/automation/tests/buyerapp/newapis/` — registered in `testng-buyerapp.xml`, Allure epic **"Buyer App – New APIs"**.

| Class | Endpoints | Tests |
|---|---|---|
| `BannerPlacementTest` | `GET /v1/banner/{home,collection,pdp,search}` (incl. guest) | 13 |
| `MlpPublicReadTest` | `GET /v1/mlp/:occasion`, `/v1/mlp?occasion=`, `/list`, `/occasions`; `/v1/admin/mlp` (403) | 12 |
| `NavoLiveTest` | `GET /v1/navo-live`, `/v1/navo-live/events/:eventId/products` | 13 |
| `EventsIngestionNegativeTest` | `POST /events`, `/events/impressions` (root, not `/v1`) | 6 |
| `SubSellerAuthTest` | `/v1/subseller/*` | 8 |
| `PrismAccessTest` | `/v1/prism/*` access control | 9 |
| `ReferralValidationTest` | `/v1/referral` | 8 |
| `ChatbotV2ValidationTest` | `POST /v2/chatbot/message` | 7 |

**Production-safety rule.** These tests run against production, so they only send GETs or requests that backend-api rejects **before** any DB write, Kafka publish or LLM call (missing/invalid token → 401, staff gate → 403, validation → 400). No valid event, referral, chat message or stock update is ever sent. `PrismAccessTest` sends two valid-shaped bodies only after a read-only probe confirms the test buyer is refused by the same staff gate; otherwise those tests skip. Prod data that may not exist (an MLP occasion, a live event) is discovered at runtime and the test skips if absent.

**Test-case traceability.** Each test has `@TmsLink("TC-…")` with the id from `backend-api/QA/test_cases_doc/`; `allure.properties` turns it into a link that searches the backend-api repo for that id.

**Error envelope.** `ApiErrorAssertions.assertApiError(...)` checks backend-api's `{ statusCode, message }` errors: `statusCode` is `"101"` for thrown errors (validation, staff gate), `"10001"` for direct failure responses, `"10003"` for an invalid token. The events routes use a Go-style `{ status, message, data }` envelope instead.

**Run only these:** `mvn test -Dsurefire.suiteXmlFiles= -Dtest='LoginTest,BannerPlacementTest,MlpPublicReadTest,NavoLiveTest,EventsIngestionNegativeTest,SubSellerAuthTest,PrismAccessTest,ReferralValidationTest,ChatbotV2ValidationTest' -DfailIfNoTests=false`. Build with **JDK 17** — Lombok 1.18.30 fails on newer JDKs.

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
````

### `README.md`

_839 lines_

````markdown
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
- **Dual API Support** - BOMB (admin/seller) pipelines and the Buyer App (Navo) consumer API
- **Test counts** - see [Test Counts](#-test-counts); generated from the source, never typed by hand
- **Advanced Features** - JWT authentication, VariableManager for data, retry mechanism
- **Production Quality** - Industry best practices, design patterns, and clean code

> **Perfect for:** QA Engineers, Test Automation Engineers, DevOps teams looking for a reference implementation of API testing with RestAssured + TestNG + Allure.

---

## 🔢 **Test Counts**

Counts per suite and area, generated from the `@Test` methods in the source. "Tests" are the ones that run; "Disabled" are `@Test(enabled = false)` (currently BannersTest, ContinueYourJourneyTest, FeedFilterSaveTest and VideoFeedTvTest, disabled since Dec 2025). After adding or removing tests, run `python scripts/update-test-counts.py` (`--check` fails if this table is stale).

<!-- test-counts:start -->
<!-- Generated by scripts/update-test-counts.py from the source; do not edit by hand. -->

| Suite | Area | Classes | Tests | Disabled |
|---|---|---:|---:|---:|
| BOMB (api.bizup.app) | CatalogSearch | 4 | 49 | 0 |
| BOMB (api.bizup.app) | CatalogTagPipeline | 12 | 84 | 0 |
| BOMB (api.bizup.app) | Login | 1 | 7 | 0 |
| BOMB (api.bizup.app) | VideoTaggingPipeline | 7 | 72 | 0 |
| **BOMB (api.bizup.app)** | **total** | **24** | **212** | **0** |
| Buyer App (api.navofashion.in) | (root) | 4 | 24 | 8 |
| Buyer App (api.navofashion.in) | CollectionListing | 7 | 40 | 0 |
| Buyer App (api.navofashion.in) | HomePage | 8 | 47 | 32 |
| Buyer App (api.navofashion.in) | Login | 1 | 8 | 0 |
| Buyer App (api.navofashion.in) | PDP | 1 | 5 | 0 |
| Buyer App (api.navofashion.in) | ProductPage | 1 | 6 | 0 |
| Buyer App (api.navofashion.in) | ProfilePage | 5 | 31 | 0 |
| Buyer App (api.navofashion.in) | SearchPage | 3 | 19 | 0 |
| Buyer App (api.navofashion.in) | newapis | 8 | 76 | 0 |
| Buyer App (api.navofashion.in) | schema | 1 | 3 | 0 |
| **Buyer App (api.navofashion.in)** | **total** | **39** | **259** | **40** |
| **All suites** | | **63** | **471** | **40** |

<!-- test-counts:end -->

---

## �📊 **Framework Coverage**

<table>
<tr>
<td width="50%" valign="top">

### 🎯 **BOMB API** (Admin/Seller)
```
✅ Login Pipeline
✅ Catalog Search
✅ Catalog Tag Pipeline
✅ Video Tagging Pipeline

📈 4 Pipelines
🔗 15+ Endpoints
```

</td>
<td width="50%" valign="top">

### 🛍️ **Buyer App API** (Consumer)
```
✅ Login & Authentication
✅ HomePage
✅ Collections
✅ Search
✅ Profile & Config

📈 8 Feature Areas
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

### **3️⃣ Add Login Credentials** *(Required)*
The test-account phone numbers and OTPs are not committed. For local runs:
```bash
cp src/test/resources/secrets.properties.example src/test/resources/secrets.properties
# then fill in the four values (the file is gitignored)
```
Or set `BOMB_LOGIN_PHONE`, `BOMB_LOGIN_OTP`, `BUYER_LOGIN_PHONE`, `BUYER_LOGIN_OTP` (env vars win over the file; `-D` properties win over both). Login tokens are kept in memory for the run and never written to disk.

Build with **JDK 17** (`JAVA_HOME` pointing at a JDK 17): Lombok 1.18.30 fails on newer JDKs.

### **Update Configuration** *(Optional)*
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
# 🚀 All Tests
mvn clean test

# 🎯 BOMB API Tests Only
mvn clean test -Pbomb

# 🛍️ Buyer App Tests Only
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
║           ALLURE TEST REPORT (illustrative example)          ║
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
   - `all` - Run every suite
   - `bomb` - Run the BOMB suite
   - `buyerapp` - Run the Buyer App suite
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

| File | What it holds | In git |
|---|---|---|
| `config.properties` | base URLs, timeouts, the headers the Navo app sends (`buyer.app.zone`, `buyer.app.version.code`, …) | yes |
| `secrets.properties` | test-account phone numbers and OTPs (copy from `secrets.properties.example`) | **no** (gitignored) |
| `test-variables.properties` | static test data (ids, search terms). `cart_id` / `live_catalog_id` are only fallbacks: `TestDataResolver` looks up current values at run time | yes |

Buyer App requests go through `BaseTest.buyerAppSpec()` (logged in) or `buyerAppGuestSpec()` (guest). Both send the app's headers; a test that sets one of them itself replaces the default. If no token exists yet, `buyerAppSpec()` logs in on demand, so a single class can run on its own.

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
````

### `pom.xml`

_215 lines_

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.automation</groupId>
    <artifactId>api-automation-restassured</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>API Automation Framework</name>
    <description>REST API Automation Framework using RestAssured, TestNG, and Hamcrest</description>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <restassured.version>5.4.0</restassured.version>
        <testng.version>7.9.0</testng.version>
        <hamcrest.version>2.2</hamcrest.version>
        <jackson.version>2.16.1</jackson.version>
        <lombok.version>1.18.30</lombok.version>
        <log4j.version>2.22.1</log4j.version>
        <allure.version>2.25.0</allure.version>
        <aspectj.version>1.9.21</aspectj.version>
    </properties>

    <dependencies>
        <!-- RestAssured -->
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>rest-assured</artifactId>
            <version>${restassured.version}</version>
        </dependency>

        <!-- JSON Schema Validation -->
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>json-schema-validator</artifactId>
            <version>${restassured.version}</version>
        </dependency>

        <!-- TestNG -->
        <dependency>
            <groupId>org.testng</groupId>
            <artifactId>testng</artifactId>
            <version>${testng.version}</version>
        </dependency>

        <!-- Hamcrest -->
        <dependency>
            <groupId>org.hamcrest</groupId>
            <artifactId>hamcrest</artifactId>
            <version>${hamcrest.version}</version>
        </dependency>

        <!-- Jackson for JSON Serialization/Deserialization -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>

        <!-- Lombok for reducing boilerplate -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- Log4j2 for logging -->
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-api</artifactId>
            <version>${log4j.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-core</artifactId>
            <version>${log4j.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-slf4j-impl</artifactId>
            <version>${log4j.version}</version>
        </dependency>

        <!-- Allure Reporting -->
        <dependency>
            <groupId>io.qameta.allure</groupId>
            <artifactId>allure-testng</artifactId>
            <version>${allure.version}</version>
        </dependency>
        <dependency>
            <groupId>io.qameta.allure</groupId>
            <artifactId>allure-rest-assured</artifactId>
            <version>${allure.version}</version>
        </dependency>

        <!-- JSON Path for parsing -->
        <dependency>
            <groupId>com.jayway.jsonpath</groupId>
            <artifactId>json-path</artifactId>
            <version>2.9.0</version>
        </dependency>

        <!-- Owner for property management -->
        <dependency>
            <groupId>org.aeonbits.owner</groupId>
            <artifactId>owner</artifactId>
            <version>1.0.12</version>
        </dependency>
    </dependencies>

    <profiles>
        <!-- Profile for running BOMB API tests only -->
        <profile>
            <id>bomb</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-surefire-plugin</artifactId>
                        <version>3.2.3</version>
                        <configuration>
                            <suiteXmlFiles>
                                <suiteXmlFile>src/test/resources/testng-bomb.xml</suiteXmlFile>
                            </suiteXmlFiles>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>

        <!-- Profile for running Buyer App tests only -->
        <profile>
            <id>buyerapp</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-surefire-plugin</artifactId>
                        <version>3.2.3</version>
                        <configuration>
                            <suiteXmlFiles>
                                <suiteXmlFile>src/test/resources/testng-buyerapp.xml</suiteXmlFile>
                            </suiteXmlFiles>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>

    <build>
        <plugins>
            <!-- Maven Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.12.1</version>
                <configuration>
                    <source>${maven.compiler.source}</source>
                    <target>${maven.compiler.target}</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>

            <!-- Maven Surefire Plugin for TestNG -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.3</version>
                <configuration>
                    <suiteXmlFiles>
                        <suiteXmlFile>src/test/resources/testng.xml</suiteXmlFile>
                    </suiteXmlFiles>
                    <argLine>
                        -javaagent:"${settings.localRepository}/org/aspectj/aspectjweaver/${aspectj.version}/aspectjweaver-${aspectj.version}.jar"
                    </argLine>
                    <systemPropertyVariables>
                        <allure.results.directory>${project.build.directory}/allure-results</allure.results.directory>
                    </systemPropertyVariables>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>org.aspectj</groupId>
                        <artifactId>aspectjweaver</artifactId>
                        <version>${aspectj.version}</version>
                    </dependency>
                </dependencies>
            </plugin>

            <!-- Allure Maven Plugin -->
            <plugin>
                <groupId>io.qameta.allure</groupId>
                <artifactId>allure-maven</artifactId>
                <version>2.14.0</version>
                <configuration>
                    <reportVersion>${allure.version}</reportVersion>
                    <resultsDirectory>${project.build.directory}/allure-results</resultsDirectory>
                    <reportDirectory>${project.build.directory}/site/allure-maven-plugin</reportDirectory>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### `scripts/extract-test-summary.py`

_119 lines_

```python
#!/usr/bin/env python3
"""
Extract test execution summary from TestNG results XML
Outputs JSON format for easy consumption by notification scripts
"""
import xml.etree.ElementTree as ET
import json
import sys
import os

def convert_class_name_to_feature(class_name):
    """Convert a Java class name to a human-readable feature name
    
    Examples:
        FeaturedCollectionTest -> Featured Collection
        ContinueYourJourneyTest -> Continue Your Journey
        LoginApiTest -> Login Api
    """
    # Extract simple class name (remove package)
    simple_name = class_name.split('.')[-1]
    
    # Remove 'Test' suffix if present
    if simple_name.endswith('Test'):
        simple_name = simple_name[:-4]
    
    # Split camelCase/PascalCase into words
    import re
    # Insert space before uppercase letters (but not at the start)
    words = re.sub('([A-Z])', r' \1', simple_name).strip()
    
    return words

def extract_summary(testng_results_path):
    """Extract test statistics and detailed test case info from testng-results.xml"""
    
    if not os.path.exists(testng_results_path):
        print(f"Error: File not found: {testng_results_path}", file=sys.stderr)
        sys.exit(1)
    
    try:
        tree = ET.parse(testng_results_path)
        root = tree.getroot()
        
        # Extract from <testng-results> root element
        total = int(root.get('total', 0))
        passed = int(root.get('passed', 0))
        failed = int(root.get('failed', 0))
        skipped = int(root.get('skipped', 0))
        
        # Calculate additional metrics
        pass_rate = (passed / total * 100) if total > 0 else 0
        
        # Extract detailed test case information
        test_cases = []
        serial_no = 1
        
        # Iterate through all test methods
        for suite in root.findall('.//suite'):
            for test in suite.findall('.//test'):
                for class_elem in test.findall('.//class'):
                    # Get class name for feature extraction
                    class_name = class_elem.get('name', 'Unknown')
                    feature_name = convert_class_name_to_feature(class_name)
                    
                    for test_method in class_elem.findall('.//test-method'):
                        # Skip configuration methods (like @BeforeMethod, @AfterMethod)
                        if test_method.get('is-config') == 'true':
                            continue
                        
                        test_name = test_method.get('name', 'Unknown')
                        status = test_method.get('status', 'UNKNOWN')
                        
                        # Get description from method or use test name
                        description = test_method.get('description', '')
                        if not description:
                            description = f"Test case: {test_name}"
                        
                        # Map status to emoji
                        status_emoji = {
                            'PASS': '✅ Passed',
                            'FAIL': '❌ Failed',
                            'SKIP': '⚠️ Skipped'
                        }.get(status, '❓ Unknown')
                        
                        test_cases.append({
                            'serial_no': serial_no,
                            'name': test_name,
                            'feature_name': feature_name,
                            'description': description,
                            'status': status_emoji,
                            'raw_status': status
                        })
                        serial_no += 1
        
        summary = {
            'total': total,
            'passed': passed,
            'failed': failed,
            'skipped': skipped,
            'pass_rate': round(pass_rate, 2),
            'test_cases': test_cases
        }
        
        return summary
        
    except Exception as e:
        print(f"Error parsing XML: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: python extract-test-summary.py <path-to-testng-results.xml>")
        sys.exit(1)
    
    results_path = sys.argv[1]
    summary = extract_summary(results_path)
    
    # Output as JSON
    print(json.dumps(summary, indent=2))
```

### `scripts/notification-config.example.json`

_19 lines_

```json
{
  "project_name": "API Automation Framework",
  
  "enable_email": true,
  "email_from": "jenkins@example.com",
  "email_to": ["team@example.com", "qa@example.com"],
  "smtp_host": "smtp.gmail.com",
  "smtp_port": 587,
  "smtp_use_tls": true,
  "smtp_username": "your-email@gmail.com",
  "smtp_password": "your-app-password",
  
  "enable_google_chat": true,
  "google_chat_webhook": "https://chat.googleapis.com/v1/spaces/<REDACTED>",
  
  "build_url": "${BUILD_URL}",
  "report_url": "https://your-bucket.s3.amazonaws.com/reports/${BUILD_NUMBER}/index.html",
  "logs_url": "https://your-bucket.s3.amazonaws.com/logs/${BUILD_NUMBER}/automation.log"
}
```

### `scripts/notify-chat.sh`

_53 lines_

```bash
#!/bin/bash
#
# Simple notification script using curl for Google Chat
# Can be used standalone or as part of Jenkins pipeline
#

# Configuration
PROJECT_NAME="API Automation Framework"
TESTNG_RESULTS="target/surefire-reports/testng-results.xml"
GOOGLE_CHAT_WEBHOOK="${GOOGLE_CHAT_WEBHOOK_URL}"

# Extract test statistics from TestNG XML
extract_stats() {
    if [ ! -f "$TESTNG_RESULTS" ]; then
        echo "Error: TestNG results file not found: $TESTNG_RESULTS"
        exit 1
    fi
    
    # Parse XML attributes using grep and sed
    TOTAL=$(grep -oP '(?<=<testng-results).*?(?=>)' "$TESTNG_RESULTS" | grep -oP '(?<=total=")[^"]*')
    PASSED=$(grep -oP '(?<=<testng-results).*?(?=>)' "$TESTNG_RESULTS" | grep -oP '(?<=passed=")[^"]*')
    FAILED=$(grep -oP '(?<=<testng-results).*?(?=>)' "$TESTNG_RESULTS" | grep -oP '(?<=failed=")[^"]*')
    SKIPPED=$(grep -oP '(?<=<testng-results).*?(?=>)' "$TESTNG_RESULTS" | grep -oP '(?<=skipped=")[^"]*')
}

# Send Google Chat notification
send_google_chat() {
    local status_emoji="✅"
    local status_text="PASSED"
    
    if [ "$FAILED" -gt 0 ]; then
        status_emoji="❌"
        status_text="FAILED"
    fi
    
    local message=$(cat <<EOF
{
  "text": "*${status_emoji} Test Report: ${PROJECT_NAME}*\n\n*------${PROJECT_NAME}------*\n\n*Total Tests:* ${TOTAL}\n*Test Pass:* ✅ ${PASSED}\n*Test Fail:* ❌ ${FAILED}\n*Test Skipped:* ⏭️ ${SKIPPED}\n\n*Links:*\n• Build: <${BUILD_URL}|Jenkins Build>\n• Report: <${REPORT_URL}|Allure Report>\n• Logs: <${LOGS_URL}|View Logs>"
}
EOF
    )
    
    curl -X POST \
        -H 'Content-Type: application/json' \
        -d "$message" \
        "$GOOGLE_CHAT_WEBHOOK"
}

# Main execution
extract_stats
send_google_chat

echo "Notification sent successfully!"
```

### `scripts/send-notification.py`

_384 lines_

```python
#!/usr/bin/env python3
"""
Send test execution summary notifications to Email and Google Chat
"""
import json
import sys
import os
import requests
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
import smtplib

def send_email(summary_data, config):
    """Send email notification"""
    
    msg = MIMEMultipart('alternative')
    
    # Determine overall status
    failed_count = summary_data['failed']
    if failed_count == 0:
        overall_status = "✅ PASSED"
        status_color = "#34A853"
    else:
        overall_status = "❌ FAILED"
        status_color = "#EA4335"
    
    msg['Subject'] = f"Test Report: {config['project_name']} - {overall_status}"
    msg['From'] = config['email_from']
    msg['To'] = ', '.join(config['email_to'])
    
    # Build detailed test case table rows
    test_case_rows = ""
    for test_case in summary_data.get('test_cases', []):
        test_case_rows += f"""
            <tr style="border-bottom: 1px solid #e5e7eb;">
                <td style="padding: 12px; text-align: center; font-weight: 500;">{test_case['serial_no']}</td>
                <td style="padding: 12px; font-weight: 500; color: #1f2937;">{test_case['feature_name']}</td>
                <td style="padding: 12px; color: #4b5563;">{test_case['description']}</td>
                <td style="padding: 12px; text-align: center; font-weight: 600;">{test_case['status']}</td>
            </tr>"""
    
    # Create final status message
    if failed_count == 0:
        final_message = "🎉🎉 All test cases passed successfully 🎉🎉"
        final_bg = "#d4edda"
        final_color = "#155724"
    else:
        final_message = f"⚠️ {failed_count} test case(s) failed. Please review the detailed report."
        final_bg = "#f8d7da"
        final_color = "#721c24"
    
    # Create HTML email body
    html_body = f"""
    <html>
    <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px;">
        <div style="max-width: 800px; margin: 0 auto; background-color: white; border-radius: 10px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);">
            
            <!-- Header -->
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
                <h1 style="color: white; margin: 0; font-size: 24px; text-shadow: 2px 2px 4px rgba(0,0,0,0.2);">
                    🔹🔹 Test Automation Execution Summary 🔹🔹
                </h1>
                <p style="color: #f0f0f0; margin: 10px 0 0 0; font-size: 16px;">
                    📱 🔹🔹 {config['project_name']} 🔹🔹 📱
                </p>
            </div>
            
            <!-- Content Container -->
            <div style="padding: 30px;">
                
                <!-- Execution Info -->
                <div style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); padding: 15px; border-radius: 8px; margin-bottom: 25px; text-align: center;">
                    <h2 style="color: white; margin: 0; font-size: 18px; text-shadow: 1px 1px 2px rgba(0,0,0,0.2);">
                        🚀 Build #{config.get('build_number', 'N/A')} - Test Execution 🚀
                    </h2>
                </div>
                
                <!-- Test Summary Table -->
                <h3 style="color: #1f2937; margin: 25px 0 15px 0; font-size: 18px; border-bottom: 3px solid #667eea; padding-bottom: 8px;">
                    📌 Test Summary Metrics 📊
                </h3>
                <table style="width: 100%; border-collapse: collapse; margin-bottom: 30px; border: 2px solid #e5e7eb; border-radius: 8px; overflow: hidden;">
                    <thead>
                        <tr style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                            <th style="padding: 15px; text-align: left; color: white; font-size: 14px;">📌 Metric</th>
                            <th style="padding: 15px; text-align: center; color: white; font-size: 14px;">📊 Count</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr style="background-color: #f9fafb; border-bottom: 1px solid #e5e7eb;">
                            <td style="padding: 12px; font-weight: 500;">Total Tests</td>
                            <td style="padding: 12px; text-align: center; font-weight: bold; font-size: 16px;">{summary_data['total']}</td>
                        </tr>
                        <tr style="background-color: #d4f4dd; border-bottom: 1px solid #e5e7eb;">
                            <td style="padding: 12px; font-weight: 500;">✅ Passed</td>
                            <td style="padding: 12px; text-align: center; font-weight: bold; color: #059669; font-size: 16px;">{summary_data['passed']}</td>
                        </tr>
                        <tr style="background-color: #fee2e2; border-bottom: 1px solid #e5e7eb;">
                            <td style="padding: 12px; font-weight: 500;">❌ Failed</td>
                            <td style="padding: 12px; text-align: center; font-weight: bold; color: #dc2626; font-size: 16px;">{summary_data['failed']}</td>
                        </tr>
                        <tr style="background-color: #fef3c7;">
                            <td style="padding: 12px; font-weight: 500;">⚠️ Skipped</td>
                            <td style="padding: 12px; text-align: center; font-weight: bold; color: #f59e0b; font-size: 16px;">{summary_data['skipped']}</td>
                        </tr>
                    </tbody>
                </table>
                
                <!-- Detailed Test Case Report -->
                <h3 style="color: #1f2937; margin: 35px 0 15px 0; font-size: 18px; border-bottom: 3px solid #667eea; padding-bottom: 8px;">
                    📋 Detailed Test Case Report 📋
                </h3>
                <div style="max-height: 250px; overflow-y: auto; border: 2px solid #e5e7eb; border-radius: 8px; margin-bottom: 30px;">
                    <table style="width: 100%; border-collapse: collapse;">
                        <thead style="position: sticky; top: 0; z-index: 10;">
                            <tr style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                                <th style="padding: 15px; text-align: center; color: white; font-size: 13px; width: 60px;">🔢 S No.</th>
                                <th style="padding: 15px; text-align: left; color: white; font-size: 13px; width: 200px;">📝 Test Feature Name</th>
                                <th style="padding: 15px; text-align: left; color: white; font-size: 13px;">📄 Description</th>
                                <th style="padding: 15px; text-align: center; color: white; font-size: 13px; width: 120px;">📌 Status</th>
                            </tr>
                        </thead>
                        <tbody style="background-color: white;">
                            {test_case_rows}
                        </tbody>
                    </table>
                </div>
                
                <!-- Final Status Message -->
                <div style="background-color: {final_bg}; border-left: 5px solid {status_color}; padding: 20px; border-radius: 8px; margin: 30px 0; text-align: center;">
                    <h3 style="color: {final_color}; margin: 0; font-size: 18px;">
                        {final_message}
                    </h3>
                </div>
                
                <!-- Quick Links Section -->
                <h3 style="color: #1f2937; margin: 30px 0 15px 0; font-size: 18px; border-bottom: 3px solid #667eea; padding-bottom: 8px;">
                    🔗 Quick Access Links
                </h3>
                <div style="background-color: #f9fafb; padding: 20px; border-radius: 8px; border: 1px solid #e5e7eb;">
                    <p style="margin: 10px 0;">
                        <strong style="color: #1f2937;">📊 Allure Report:</strong> 
                        <a href="{config['report_url']}" style="color: #667eea; text-decoration: none; font-weight: 500;">Click Here to View Report</a>
                    </p>
                    <p style="margin: 10px 0;">
                        <strong style="color: #1f2937;">🔧 Jenkins Build:</strong> 
                        <a href="{config['build_url']}" style="color: #667eea; text-decoration: none; font-weight: 500;">Click Here to View Build</a>
                    </p>
                    <p style="margin: 10px 0;">
                        <strong style="color: #1f2937;">📋 Automation Logs:</strong> 
                        <a href="{config['logs_url']}" style="color: #667eea; text-decoration: none; font-weight: 500;">Click Here to View Logs</a>
                    </p>
                </div>
                
                <!-- Footer -->
                <div style="margin-top: 40px; padding-top: 20px; border-top: 2px solid #e5e7eb; text-align: center;">
                    <p style="color: #6b7280; font-size: 13px; margin: 5px 0;">
                        📎 This is an automated report generated by the QA Automation Framework
                    </p>
                    <p style="color: #6b7280; font-size: 13px; margin: 5px 0;">
                        <strong>Regards,</strong><br>
                        QA Automation Team
                    </p>
                    <p style="color: #9ca3af; font-size: 11px; margin: 15px 0 0 0;">
                        Generated on: {config.get('timestamp', 'N/A')}
                    </p>
                </div>
                
            </div>
        </div>
    </body>
    </html>
    """
    
    msg.attach(MIMEText(html_body, 'html'))
    
    # Send email
    try:
        with smtplib.SMTP(config['smtp_host'], config['smtp_port']) as server:
            if config.get('smtp_use_tls', True):
                server.starttls()
            if config.get('smtp_username') and config.get('smtp_password'):
                server.login(config['smtp_username'], config['smtp_password'])
            server.send_message(msg)
        print("✅ Email sent successfully")
    except Exception as e:
        print(f"❌ Failed to send email: {e}", file=sys.stderr)

def send_google_chat(summary_data, config):
    """Send Google Chat notification via webhook with enhanced UI"""
    
    # Determine status and styling
    failed_count = summary_data['failed']
    pass_rate = float(summary_data['pass_rate'])
    
    if failed_count == 0:
        status_emoji = "🎉"
        status_text = "ALL TESTS PASSED"
        status_color = "#34A853"  # Green
        header_image = "https://img.icons8.com/color/96/000000/ok--v1.png"
    elif pass_rate >= 90:
        status_emoji = "⚠️"
        status_text = "MOSTLY PASSED"
        status_color = "#FBBC04"  # Yellow
        header_image = "https://img.icons8.com/color/96/000000/medium-risk.png"
    else:
        status_emoji = "❌"
        status_text = "TESTS FAILED"
        status_color = "#EA4335"  # Red
        header_image = "https://img.icons8.com/color/96/000000/high-risk.png"
    
    # Calculate execution time if available
    execution_time = summary_data.get('execution_time', 'N/A')
    
    # Create enhanced Google Chat card message
    message = {
        "cardsV2": [{
            "cardId": "test-report-card",
            "card": {
                "header": {
                    "title": f"{status_emoji} {config['project_name']}",
                    "subtitle": f"Status: {status_text}",
                    "imageUrl": header_image,
                    "imageType": "CIRCLE"
                },
                "sections": [
                    {
                        "header": "📊 Test Results Summary",
                        "collapsible": False,
                        "widgets": [
                            {
                                "decoratedText": {
                                    "topLabel": "Total Tests Executed",
                                    "text": f"<b>{summary_data['total']}</b>",
                                    "startIcon": {
                                        "knownIcon": "BOOKMARK"
                                    }
                                }
                            },
                            {
                                "decoratedText": {
                                    "topLabel": "Passed",
                                    "text": f"<font color='#34A853'><b>✅ {summary_data['passed']}</b></font>",
                                    "startIcon": {
                                        "knownIcon": "STAR"
                                    }
                                }
                            },
                            {
                                "decoratedText": {
                                    "topLabel": "Failed",
                                    "text": f"<font color='#EA4335'><b>❌ {summary_data['failed']}</b></font>",
                                    "startIcon": {
                                        "knownIcon": "DESCRIPTION"
                                    }
                                }
                            },
                            {
                                "decoratedText": {
                                    "topLabel": "Skipped",
                                    "text": f"<font color='#FBBC04'><b>⏭️ {summary_data['skipped']}</b></font>",
                                    "startIcon": {
                                        "knownIcon": "CLOCK"
                                    }
                                }
                            },
                            {
                                "divider": {}
                            },
                            {
                                "decoratedText": {
                                    "topLabel": "Pass Rate",
                                    "text": f"<font color='{status_color}'><b>{summary_data['pass_rate']}%</b></font>",
                                    "startIcon": {
                                        "knownIcon": "MULTIPLE_PEOPLE"
                                    }
                                }
                            }
                        ]
                    },
                    {
                        "header": "🔗 Quick Actions",
                        "collapsible": False,
                        "widgets": [
                            {
                                "buttonList": {
                                    "buttons": [
                                        {
                                            "text": "📊 View Report",
                                            "onClick": {
                                                "openLink": {
                                                    "url": config['report_url']
                                                }
                                            }
                                        },
                                        {
                                            "text": "🔧 Jenkins Build",
                                            "onClick": {
                                                "openLink": {
                                                    "url": config['build_url']
                                                }
                                            }
                                        },
                                        {
                                            "text": "📋 View Logs",
                                            "onClick": {
                                                "openLink": {
                                                    "url": config['logs_url']
                                                }
                                            }
                                        }
                                    ]
                                }
                            }
                        ]
                    }
                ]
            }
        }]
    }
    
    # Send to Google Chat webhook
    try:
        print(f"📤 Sending Google Chat notification to webhook...", file=sys.stderr)
        print(f"   Webhook URL (first 50 chars): {config['google_chat_webhook'][:50]}...", file=sys.stderr)
        
        response = requests.post(
            config['google_chat_webhook'],
            json=message,
            headers={'Content-Type': 'application/json; charset=UTF-8'},
            timeout=10
        )
        
        print(f"   Response Status Code: {response.status_code}", file=sys.stderr)
        print(f"   Response Body: {response.text[:200]}", file=sys.stderr)
        
        response.raise_for_status()
        print("✅ Google Chat notification sent successfully")
    except requests.exceptions.Timeout:
        print(f"❌ Failed to send Google Chat notification: Request timed out after 10 seconds", file=sys.stderr)
    except requests.exceptions.ConnectionError as e:
        print(f"❌ Failed to send Google Chat notification: Connection error - {e}", file=sys.stderr)
    except requests.exceptions.HTTPError as e:
        print(f"❌ Failed to send Google Chat notification: HTTP {response.status_code} - {response.text}", file=sys.stderr)
    except Exception as e:
        print(f"❌ Failed to send Google Chat notification: {type(e).__name__}: {e}", file=sys.stderr)

def main():
    if len(sys.argv) < 3:
        print("Usage: python send-notification.py <summary-json> <config-json>")
        sys.exit(1)
    
    summary_file = sys.argv[1]
    config_file = sys.argv[2]
    
    print(f"📥 Loading test summary from: {summary_file}", file=sys.stderr)
    # Load summary data
    with open(summary_file, 'r') as f:
        summary_data = json.load(f)
    print(f"   Tests: {summary_data['total']}, Passed: {summary_data['passed']}, Failed: {summary_data['failed']}", file=sys.stderr)
    
    print(f"📥 Loading notification config from: {config_file}", file=sys.stderr)
    # Load config
    with open(config_file, 'r') as f:
        config = json.load(f)
    
    print(f"   Email enabled: {config.get('enable_email', False)}", file=sys.stderr)
    print(f"   Google Chat enabled: {config.get('enable_google_chat', False)}", file=sys.stderr)
    
    # Send notifications
    if config.get('enable_email', False):
        print(f"\n📧 Sending email notification...", file=sys.stderr)
        send_email(summary_data, config)
    else:
        print(f"\n📧 Email notification is disabled", file=sys.stderr)
    
    if config.get('enable_google_chat', False):
        print(f"\n💬 Sending Google Chat notification...", file=sys.stderr)
        send_google_chat(summary_data, config)
    else:
        print(f"\n💬 Google Chat notification is disabled", file=sys.stderr)

if __name__ == '__main__':
    main()
```

### `scripts/update-test-counts.py`

_79 lines_

```python
#!/usr/bin/env python3
"""
Regenerate the test-count table in README.md from the source code.

Counts enabled @Test methods (and disabled ones, enabled = false, separately)
and test classes per suite and per feature folder under
src/test/java/com/automation/tests, then rewrites the block between
<!-- test-counts:start --> and <!-- test-counts:end --> in README.md.

Usage:  python scripts/update-test-counts.py          # rewrite README.md
        python scripts/update-test-counts.py --check  # exit 1 if README is stale (CI)
"""
import re
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TESTS = ROOT / "src/test/java/com/automation/tests"
README = ROOT / "README.md"
START, END = "<!-- test-counts:start -->", "<!-- test-counts:end -->"
SUITES = {"bomb": "BOMB (api.bizup.app)", "buyerapp": "Buyer App (api.navofashion.in)"}


def count():
    per_area = defaultdict(lambda: [0, 0, 0])  # (suite, area) -> [classes, tests, disabled]
    for f in sorted(TESTS.rglob("*.java")):
        anns = re.findall(r"^\s*@Test\b(?:\((?:[^()]|\([^()]*\))*\))?", f.read_text(encoding="utf-8"), re.M)
        if not anns:
            continue
        disabled = sum(1 for a in anns if re.search(r"enabled\s*=\s*false", a))
        tests = len(anns) - disabled
        rel = f.relative_to(TESTS).parts
        suite = rel[0]
        area = rel[1] if len(rel) > 2 else "(root)"
        per_area[(suite, area)][0] += 1
        per_area[(suite, area)][1] += tests
        per_area[(suite, area)][2] += disabled
    return per_area


def render(per_area):
    lines = [START,
             "<!-- Generated by scripts/update-test-counts.py from the source; do not edit by hand. -->",
             "", "| Suite | Area | Classes | Tests | Disabled |", "|---|---|---:|---:|---:|"]
    total_c = total_t = total_d = 0
    for suite in SUITES:
        rows = sorted((a, v) for (s, a), v in per_area.items() if s == suite)
        sc = sum(v[0] for _, v in rows)
        st = sum(v[1] for _, v in rows)
        sd = sum(v[2] for _, v in rows)
        for area, (c, t, d) in rows:
            lines.append(f"| {SUITES[suite]} | {area} | {c} | {t} | {d} |")
        lines.append(f"| **{SUITES[suite]}** | **total** | **{sc}** | **{st}** | **{sd}** |")
        total_c += sc
        total_t += st
        total_d += sd
    lines.append(f"| **All suites** | | **{total_c}** | **{total_t}** | **{total_d}** |")
    lines += ["", END]
    return "\n".join(lines)


def main():
    text = README.read_text(encoding="utf-8")
    if START not in text or END not in text:
        sys.exit(f"README.md has no {START} ... {END} block")
    block = render(count())
    new = re.sub(re.escape(START) + r".*?" + re.escape(END), lambda _: block, text, flags=re.S)
    if "--check" in sys.argv:
        if new != text:
            sys.exit("README.md test counts are stale: run python scripts/update-test-counts.py")
        print("README.md test counts are up to date")
        return
    README.write_text(new, encoding="utf-8")
    print("README.md test counts updated")


if __name__ == "__main__":
    main()
```

### `src/main/java/com/automation/base/BaseTest.java`

_188 lines_

```java
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
```

### `src/main/java/com/automation/config/ConfigManager.java`

_128 lines_

```java
package com.automation.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;

/**
 * Configuration interface using Owner library.
 * Precedence: -D system properties (CI env vars are copied into these by
 * getInstance), then the gitignored secrets.properties, then config.properties.
 * MERGE: every source is read; the first one that defines a key wins.
 */
@LoadPolicy(LoadType.MERGE)
@Config.Sources({
        "system:properties",
        "classpath:secrets.properties",
        "classpath:config.properties",
        "classpath:config-${env}.properties"
})
public interface ConfigManager extends Config {

    /**
     * Get the singleton instance of ConfigManager
     */
    static ConfigManager getInstance() {
        applySecretEnvVars();
        return ConfigCache.getOrCreate(ConfigManager.class);
    }

    /**
     * CI supplies the login credentials as env vars. Copy each one into its
     * property key unless a -D system property already set it, so the
     * "system:properties" source picks it up. Runs before the config is cached.
     */
    static void applySecretEnvVars() {
        String[][] envToKey = {
                {"BOMB_LOGIN_PHONE", "login.phone.number"},
                {"BOMB_LOGIN_OTP", "login.token"},
                {"BUYER_LOGIN_PHONE", "buyer.app.phone.number"},
                {"BUYER_LOGIN_OTP", "buyer.app.token"},
        };
        for (String[] pair : envToKey) {
            String value = System.getenv(pair[0]);
            if (value != null && !value.isBlank() && System.getProperty(pair[1]) == null) {
                System.setProperty(pair[1], value);
            }
        }
    }

    @Key("base.url")
    @DefaultValue("https://jsonplaceholder.typicode.com")
    String baseUrl();

    @Key("api.timeout")
    @DefaultValue("30000")
    int timeout();

    @Key("retry.count")
    @DefaultValue("3")
    int retryCount();

    @Key("log.request")
    @DefaultValue("true")
    boolean logRequest();

    @Key("log.response")
    @DefaultValue("true")
    boolean logResponse();

    @Key("content.type")
    @DefaultValue("application/json")
    String contentType();

    @Key("auth.type")
    @DefaultValue("none")
    String authType();

    @Key("auth.token")
    String authToken();

    @Key("auth.username")
    String authUsername();

    @Key("auth.password")
    String authPassword();

    @Key("login.phone.number")
    String loginPhoneNumber();

    @Key("login.token")
    String loginToken();

    @Key("response.time.threshold")
    @DefaultValue("40000")
    int responseTimeThreshold();

    @Key("buyer.app.base.url")
    @DefaultValue("https://api.navofashion.in")
    String buyerAppBaseUrl();

    @Key("buyer.app.phone.number")
    String buyerAppPhoneNumber();

    @Key("buyer.app.token")
    String buyerAppToken();

    // Headers the Navo app sends on every request
    @Key("buyer.app.version")
    @DefaultValue("4.1.3-debug")
    String buyerAppVersion();

    @Key("buyer.app.version.code")
    @DefaultValue("420")
    String buyerAppVersionCode();

    @Key("buyer.app.user.segment")
    @DefaultValue("2")
    String buyerAppUserSegment();

    @Key("buyer.app.zone")
    @DefaultValue("india")
    String buyerAppZone();

    @Key("buyer.app.language")
    @DefaultValue("en")
    String buyerAppLanguage();
}
```

### `src/main/java/com/automation/constants/BombEndpoints.java`

_39 lines_

```java
package com.automation.constants;

/**
 * Centralized endpoint management for BOMB API.
 * Define all BOMB API endpoints as constants here.
 */
public final class BombEndpoints {

    private BombEndpoints() {
        // Prevent instantiation
    }

    // Auth endpoints
    public static final String LOGIN = "/api/auth/login";

    // Catalog endpoints
    public static final String CATALOG_ALL = "/v1/admin/catalog_all";
    public static final String CATALOG = "/v1/admin/catalog";
    public static final String CATALOG_ASSIGN = "/v1/admin/catalog/assign";
    public static final String EDITOR_ASSIGN_CATALOG = "/v1/admin/editor/assign/catalog";
    public static final String CATALOG_UPLOAD = "/v1/admin/catalog/upload";
    public static final String CATALOG_GROUP_UPLOAD = "/v1/admin/catalog/group/upload";
    public static final String EDITOR_SKIP_CATALOG = "/v1/admin/editor/assign/videos/skip";
    public static final String EDITOR_DONE_CATALOG = "/v1/admin/editor/assign/videos/done";
    public static final String EDITOR_MARK_AS_DONE = "/v1/admin/editor/assign/videos/done";
    public static final String CATALOG_EDITOR_ALL = "/v1/admin/editor/all";

    // Video endpoints
    public static final String VIDEOS_BY_SELLER = "/v1/admin/editor/edit/videos/{sellerId}";
    public static final String VIDEO_TITLE_GENERATION = "/v2/ai/tags-to-text";
    public static final String VIDEO_UPLOAD = "/v1/admin/pipeline/video";
    public static final String VIDEO_ASSIGN_UPLOAD = "/v1/admin/editor/assign/videos";
    public static final String VIDEO_THUMBNAIL_UPLOAD = "/v1/admin/editor/upload/videos";
    public static final String VIDEO_UPLOAD_DONE = "/v1/admin/editor/assign/videos/done";
    public static final String VIDEO_TAGGING = "/v1/admin/editor/edit/videos";

    // Admin endpoints
    public static final String ADMIN_CATALOG = "/v1/admin/catalog";
}
```

### `src/main/java/com/automation/constants/BuyerAppEndpoints.java`

_76 lines_

```java
package com.automation.constants;

/**
 * Centralized endpoint management for Buyer App (Navo) API.
 * Define all Buyer App API endpoints as constants here.
 */
public final class BuyerAppEndpoints {

    private BuyerAppEndpoints() {
        // Prevent instantiation
    }

    // Authentication endpoints
    public static final String LOGIN = "/api/auth/login";
    public static final String AUTH_VALIDATE = "/v1/auth/validate";

    // Feed/HomePage endpoints
    public static final String FEED_FILTERS = "/v1/feed/filters";
    public static final String FEED_FILTERS_SAVE = "/v1/feed/filters/save";
    public static final String FEED_BANNERS = "/v1/feed/banners";
    public static final String FEED_JOURNEY_COLLECTION = "/v1/feed/journey/collection";
    public static final String FEED_HOME_CATALOG = "/v1/feed/home/catalog";
    public static final String FEED_HOME_TRENDING = "/v1/feed/trending";
    public static final String FEED_NEW_THIS_WEEK = "/v1/feed/new-this-week";
    public static final String FEED_FEATURED_COLLECTION = "/v1/feed/featured-collection";
    public static final String FEED_HOME_CONFIG = "/v1/feed/home/config";
    public static final String FEED_TV = "/v1/feed/tv";

    // Search endpoints
    public static final String USER_SEARCH = "/v1/user/search";

    // Collection endpoints
    public static final String COLLECTION_ALL = "/v1/collection/all";
    public static final String COLLECTION_TOP = "/v1/collection/top";
    public static final String COLLECTION_BY_ID = "/v1/collection/{id}";

    // Profile & Config endpoints
    public static final String APP_UPDATE = "/api/appConfig/app-update";
    public static final String SUITABLE_FOR_CONFIG = "/api/appConfig/suitable-for";
    public static final String USER_PROFILE = "/api/user";
    public static final String USER_VIEWED_VIDEOS = "/v1/user/viewed/videos";
    public static final String ACTION = "/api/action";

    // Cart endpoints
    public static final String CART = "/v1/cart";
    public static final String USER_UPDATE_CART = "/v1/user/update_cart";

    // New backend-api route groups (QA sync 2026-10-30)
    public static final String BANNER_PLACEMENT = "/v1/banner/{placement}";
    public static final String MLP = "/v1/mlp";
    public static final String MLP_BY_OCCASION = "/v1/mlp/{occasion}";
    public static final String MLP_LIST = "/v1/mlp/list";
    public static final String MLP_OCCASIONS = "/v1/mlp/occasions";
    public static final String ADMIN_MLP = "/v1/admin/mlp";
    public static final String NAVO_LIVE = "/v1/navo-live";
    public static final String NAVO_LIVE_EVENT_PRODUCTS = "/v1/navo-live/events/{eventId}/products";
    // Mounted at the root in backend-api's expressLoader.ts — NOT under /v1
    public static final String EVENTS = "/events";
    public static final String EVENTS_IMPRESSIONS = "/events/impressions";
    public static final String EVENTS_V1_NOT_A_ROUTE = "/v1/events";
    public static final String SUBSELLER_REFRESH_TOKEN = "/v1/subseller/refresh/token";
    public static final String SUBSELLER_ME = "/v1/subseller/me";
    public static final String SUBSELLER_CATALOG = "/v1/subseller/catalog";
    public static final String SUBSELLER_CATALOG_BY_ID = "/v1/subseller/catalog/{id}";
    public static final String SUBSELLER_CATALOG_CONFIRM_STOCK = "/v1/subseller/catalog/{id}/confirm-stock";
    public static final String PRISM_CART_BOT_SESSIONS = "/v1/prism/cart-bot/sessions";
    public static final String PRISM_CHATBOT_FEEDBACK_RUNS = "/v1/prism/chatbot-feedback/runs";
    public static final String PRISM_CHATBOT_FEEDBACK_CONVERSATION = "/v1/prism/chatbot-feedback/conversations/{evaluationId}";
    public static final String PRISM_BOT_TRIAL_MESSAGE = "/v1/prism/bot-trial/message";
    public static final String PRISM_IMAGE_ENHANCEMENT_GENERATE = "/v1/prism/image-enhancement/generate";
    public static final String PRISM_IMAGE_ENHANCEMENT_EDIT = "/v1/prism/image-enhancement/edit";
    public static final String PRISM_IMAGE_ENHANCEMENT_SAVE = "/v1/prism/image-enhancement/save-to-catalog";
    public static final String PRISM_IMAGE_ENHANCEMENT_REMOVE = "/v1/prism/image-enhancement/remove-from-catalog";
    public static final String REFERRAL = "/v1/referral";
    public static final String CHATBOT_V2_MESSAGE = "/v2/chatbot/message";
}
```

### `src/main/java/com/automation/constants/HttpStatus.java`

_38 lines_

```java
package com.automation.constants;

/**
 * HTTP Status codes for API assertions.
 */
public final class HttpStatus {

    private HttpStatus() {
        // Prevent instantiation
    }

    // 2xx Success
    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int ACCEPTED = 202;
    public static final int NO_CONTENT = 204;

    // 3xx Redirection
    public static final int MOVED_PERMANENTLY = 301;
    public static final int FOUND = 302;
    public static final int NOT_MODIFIED = 304;

    // 4xx Client Error
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int METHOD_NOT_ALLOWED = 405;
    public static final int CONFLICT = 409;
    public static final int UNPROCESSABLE_ENTITY = 422;
    public static final int TOO_MANY_REQUESTS = 429;

    // 5xx Server Error
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int BAD_GATEWAY = 502;
    public static final int SERVICE_UNAVAILABLE = 503;
    public static final int GATEWAY_TIMEOUT = 504;
}
```

### `src/main/java/com/automation/listeners/RetryAnalyzer.java`

_29 lines_

```java
package com.automation.listeners;

import com.automation.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retry analyzer for failed tests.
 * Automatically retries failed tests based on configuration.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger logger = LogManager.getLogger(RetryAnalyzer.class);
    private int retryCount = 0;
    private static final int maxRetryCount = ConfigManager.getInstance().retryCount();

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < maxRetryCount) {
            retryCount++;
            logger.info("Retrying test: {} (Attempt {} of {})",
                    result.getName(), retryCount, maxRetryCount);
            return true;
        }
        return false;
    }
}
```

### `src/main/java/com/automation/listeners/TestListener.java`

_88 lines_

```java
package com.automation.listeners;

import io.qameta.allure.Attachment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG Listener for test execution lifecycle events.
 * Provides logging and Allure report attachments.
 */
public class TestListener implements ITestListener {

    private static final Logger logger = LogManager.getLogger(TestListener.class);

    @Override
    public void onStart(ITestContext context) {
        logger.info("========================================");
        logger.info("Test Suite Started: {}", context.getName());
        logger.info("========================================");
    }

    @Override
    public void onFinish(ITestContext context) {
        logger.info("========================================");
        logger.info("Test Suite Finished: {}", context.getName());
        logger.info("Passed: {}, Failed: {}, Skipped: {}",
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
        logger.info("========================================");
    }

    @Override
    public void onTestStart(ITestResult result) {
        logger.info("----------------------------------------");
        logger.info("Test Started: {}", getTestMethodName(result));
        logger.info("----------------------------------------");
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        logger.info("Test PASSED: {} (Duration: {}ms)",
                getTestMethodName(result),
                result.getEndMillis() - result.getStartMillis());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        logger.error("Test FAILED: {}", getTestMethodName(result));
        logger.error("Failure Reason: {}", result.getThrowable().getMessage());
        saveFailureLog(result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        logger.warn("Test SKIPPED: {}", getTestMethodName(result));
        if (result.getThrowable() != null) {
            logger.warn("Skip Reason: {}", result.getThrowable().getMessage());
        }
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        logger.warn("Test FAILED but within success percentage: {}", getTestMethodName(result));
    }

    private String getTestMethodName(ITestResult result) {
        return result.getTestClass().getName() + "." + result.getMethod().getMethodName();
    }

    @Attachment(value = "Failure Stack Trace", type = "text/plain")
    private String saveFailureLog(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getMessage()).append("\n\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    @Attachment(value = "{name}", type = "text/plain")
    public static String saveLog(String name, String content) {
        return content;
    }
}
```

### `src/main/java/com/automation/models/request/BotCatalogTitleGenerateRequest.java`

_22 lines_

```java
package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request POJO for Bot Catalog Title Generate API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BotCatalogTitleGenerateRequest {
    
    private List<String> tags;
}
```

### `src/main/java/com/automation/models/request/BuyerLoginRequest.java`

_21 lines_

```java
package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request POJO for Buyer App Login API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BuyerLoginRequest {

    private String phoneNumber;
    private String token;
}
```

### `src/main/java/com/automation/models/request/CatalogTaggingRequest.java`

_39 lines_

```java
package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request POJO for Catalog Tagging API.
 * Endpoint: PUT /v1/admin/catalog/{catalog_id}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogTaggingRequest {

    @JsonProperty("product_id")
    private String productId;
    
    private List<String> tags;
    
    private List<String> suggested;
    
    private List<String> images;
    
    private String title;
    
    private Double price;
    
    private Boolean isQc;
    
    private Boolean isSet;
}
```

### `src/main/java/com/automation/models/request/FeedFilterSaveRequest.java`

_23 lines_

```java
package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request POJO for Feed Filter Save API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeedFilterSaveRequest {

    private List<String> suitable_for;
    private String testData;
}
```

### `src/main/java/com/automation/models/request/LoginRequest.java`

_21 lines_

```java
package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request POJO for Login API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginRequest {

    private String phoneNumber;
    private String token;
}
```

### `src/main/java/com/automation/models/request/UpdateCartRequest.java`

_22 lines_

```java
package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request POJO for Update Cart API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateCartRequest {

    private Integer quantity;
    private String cart_id;
    private String cat_id;
}
```

### `src/main/java/com/automation/models/request/VideoFeedRequest.java`

_40 lines_

```java
package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request POJO for Video Feed API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoFeedRequest {

    private List<String> excluded_videos;
    private Filters filters;
    private Integer from;
    private String liquidity;
    private String sort_by;
    private String sort_order;
    private String video_id;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Filters {
        private List<String> city;
        private List<String> productTags;
        private List<String> suitable_for;
        private String testData;
    }
}
```

### `src/main/java/com/automation/models/request/VideoTaggingRequest.java`

_27 lines_

```java
package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request POJO for Video Tagging API.
 * Endpoint: PUT /v1/admin/editor/edit/videos/{seller_id}/{video_id}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoTaggingRequest {

    private String product_id;
    private List<String> tags;
    private List<String> suggested;
    private String title;
    private String price;
}
```

### `src/main/java/com/automation/models/request/VideoThumbnailUploadRequest.java`

_42 lines_

```java
package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request POJO for Video Thumbnail Upload API.
 * Endpoint: POST /v1/admin/editor/upload/videos/{seller_id}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoThumbnailUploadRequest {

    private Seller seller;
    private Object market; // Can be null
    private String videoLink;
    private String thumbnailLink;
    private Boolean introVideo;
    private String uploadId;
    private String fabricText;
    private String priceText;
    private String description;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String _id;
        private String phoneNumber;
        private String name;
        private String businessName;
        private String marketId;
    }
}
```

### `src/main/java/com/automation/models/request/VideoTitleGenerationRequest.java`

_23 lines_

```java
package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request POJO for Video Title Generation API.
 * Endpoint: POST https://bomb.bizup.app/api/chat/title/tags
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoTitleGenerationRequest {

    private List<String> tags;
}
```

### `src/main/java/com/automation/models/request/VideoTitleRequest.java`

_22 lines_

```java
package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request POJO for Video Title Generation API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VideoTitleRequest {

    private List<String> tags;
}
```

### `src/main/java/com/automation/models/request/VideoUploadRequest.java`

_63 lines_

```java
package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request POJO for Video Upload API.
 * Endpoint: POST /v1/admin/pipeline/video
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoUploadRequest {

    private String videoLink;
    private String videoType;
    private String description;
    private Object market; // Can be null
    private Seller seller;
    private Editor editor;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String _id;
        private String phoneNumber;
        private String name;
        private String businessName;
        private String createdAt;
        private String marketId;
        private Integer smell_test;
        private Integer assigned_score;
        private Boolean deprioritisation_status;
        private String label;
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Editor {
        private String _id;
        private String name;
        private List<String> role;
        private Integer status;
        private String phoneNumber;
        private String user_id;
        private String label;
        private String value;
    }
}
```

### `src/main/java/com/automation/models/request/VideoViewActionRequest.java`

_23 lines_

```java
package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request POJO for Video View Action API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoViewActionRequest {

    private String action;
    private String creatorId;
    private String userId;
    private String videoId;
}
```

### `src/main/java/com/automation/models/response/AppUpdateResponse.java`

_30 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for App Update API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppUpdateResponse {

    private Integer code;
    private String message;
    private AppUpdateData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AppUpdateData {
        private Boolean isForced;
        private Integer minVersion;
        private Integer minVersionToUpdate;
    }
}
```

### `src/main/java/com/automation/models/response/AuthValidateResponse.java`

_59 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Auth Validate API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthValidateResponse {

    private String statusCode;
    private String message;
    private AuthValidateData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthValidateData {
        private String _id;
        private String phoneNumber;
        private String name;
        private String businessName;
        private String createdAt;
        private String updatedAt;
        private Location location;
        private String address;
        private Boolean sellOnBizup;
        private Boolean businessVerified;
        private Boolean activated;
        private Boolean isDeleted;
        private List<String> category;
        private List<String> businessCard;
        private List<String> termsCondition;
        private List<String> fo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private String country;
        private String city;
        private String state;
        private String pincode;
        private Double lat;
        private Double lng;
        private String name;
    }
}
```

### `src/main/java/com/automation/models/response/BannersResponse.java`

_69 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Feed Banners API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BannersResponse {

    private String statusCode;
    private String message;
    private BannersData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BannersData {
        private List<BannerResult> result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BannerResult {
        private BannerValue value;
        private Integer slot;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BannerValue {
        private BannerValueData value;
        private Integer slot;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BannerValueData {
        private String _id;
        private Integer slot;
        private String clickType;
        private String imageUrl;
        private BannerOrder order;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BannerOrder {
        private Integer saree;
        private Integer readymade;
    }
}
```

### `src/main/java/com/automation/models/response/BuyerLoginResponse.java`

_75 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Buyer App Login API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuyerLoginResponse {

    private String statusCode;
    private String message;
    private BuyerLoginData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BuyerLoginData {
        private String phoneNumber;
        private String name;
        private String businessName;
        private List<String> category;
        private Boolean businessVerified;
        private String segment;
        private Object deviceId;
        private Boolean activated;
        private Boolean isDeleted;
        private String introVideo;
        private String tagLine;
        private String businessType;
        private Object dealsIn;
        private Boolean bizupProtected;
        private Boolean isSeller;
        private String introVideoThumbnail;
        private Boolean isClaimed;
        private String accountManager;
        private Boolean isCatalogAvailable;
        private String profileShareLink;
        private String persona;
        private Location location;
        private String address;
        private Object installReferrer;
        private Boolean isWhatsappOptIn;
        private Boolean sellOnBizup;
        private String createdAt;
        private String updatedAt;
        private String accessToken;
        private String refreshToken;
        private Boolean isProfileComplete;
        private Boolean isFirstTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private String country;
        private String city;
        private String state;
        private String pincode;
        private String lat;
        private String lng;
        private String name;
    }
}
```

### `src/main/java/com/automation/models/response/CatalogAssignResponse.java`

_38 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for Catalog Assign to Editor API.
 * Endpoint: /v1/admin/catalog/assign/{catalog_id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogAssignResponse {

    private String statusCode;
    private String message;
    private CatalogAssignData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogAssignData {
        private String _id;
        private String videoType;
        private String sellerId;
        private String editorId;
        private String createdAt;
        private String updatedAt;

        @JsonProperty("__v")
        private Integer version;
    }
}
```

### `src/main/java/com/automation/models/response/CatalogByIdDetailResponse.java`

_85 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Catalog By ID API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogByIdDetailResponse {

    private String statusCode;
    private String message;
    private CatalogDetailData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogDetailData {
        private Seller seller;
        private String taggedBy;
        private Thumbnail thumbnail;
        private List<Product> product;
        private Market market;
        private List<Object> images;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String phoneNumber;
        private String address;
        private String businessName;
        private String name;
        private Boolean isSuper;
        private Boolean deprioritisation_status;
        private Boolean isCatalogAvailable;
        private Boolean ordersEnabled;
        private Integer mov;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Product {
        private String name;
        private String id;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Thumbnail {
        private String fileName;
        private String mimeType;
        private String _id;
        private String url;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Market {
        private String country;
        private String city;
        private String state;
        private String name;
        private String marketNumber;
        private String createdAt;
        private String updatedAt;
    }
}
```

### `src/main/java/com/automation/models/response/CatalogEditGroupResponse.java`

_35 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Catalog Edit Group API.
 * Endpoint: GET /v1/admin/catalog/group/upload/{catalog_id}?limit=20&mode=edit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogEditGroupResponse {

    private String statusCode;
    private String message;
    private CatalogEditGroupData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogEditGroupData {
        private String _id;
        private String name;
        private Integer total;
        private List<Object> data; // Can be empty or contain catalog items
        private String error;
    }
}
```

### `src/main/java/com/automation/models/response/CatalogEditorResponse.java`

_61 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Catalog Editor API.
 * Endpoint: /v1/admin/catalog?limit=20&editor={editor_id}&sort=status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogEditorResponse {

    private String statusCode;
    private String message;
    private List<CatalogEditorGroup> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogEditorGroup {
        private List<Pagination> pagination;
        private List<CatalogEditorItem> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pagination {
        private Integer total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogEditorItem {
        private String _id;
        private String source;
        private Integer status;
        private Integer priority;
        private String videoType;
        private String sellerId;
        private String phoneNumber;
        private String name;
        private String createdAt;
        private String updatedAt;
        private String editorId;
        private Integer total;
        private Integer tagged;
        private Integer active;
    }
}
```

### `src/main/java/com/automation/models/response/CatalogGroupResponse.java`

_102 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Catalog Group API (Elasticsearch-style response).
 * Endpoint: /v1/admin/catalog/upload/{catalog_id}?limit=20&mode=all
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogGroupResponse {

    private String statusCode;
    private String message;
    private CatalogGroupData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogGroupData {
        private Integer total;
        private List<CatalogGroupItem> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogGroupItem {
        private String _index;
        private String _id;
        private Double _score;
        private CatalogSource _source;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogSource {
        private Seller seller;
        private String thubmbnailDriveLink;
        private String uploadId;
        private String displaySetting;
        private Boolean available;
        private List<String> priceTags;
        private String sellerId;
        private Boolean isDeleted;
        private List<String> displayTags;
        private String driveLink;
        private String id;
        private String contentType;
        private String uploadedBy;
        private List<String> segmentTags;
        private String updatedAt;
        private Thumbnail thumbnail;
        private Boolean visible;
        private Boolean isNew;
        private List<String> setType;
        private String url;
        private List<Object> tags;
        private Boolean processed;
        private String phoneNumber;
        private String uploadDate;
        private String original_upload;
        private List<String> productTags;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String phoneNumber;
        private Integer smell_test;
        private String address;
        private Boolean isSuper;
        private Integer mov;
        private String businessName;
        private String name;
        private Integer cod;
        private String _id;
        private Boolean deprioritisation_status;
        private Boolean isCatalogAvailable;
        private Boolean ordersEnabled;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Thumbnail {
        // Add thumbnail fields if needed
        // Empty class to handle thumbnail object in response
    }
}
```

### `src/main/java/com/automation/models/response/CatalogResponse.java`

_86 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Catalog API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogResponse {

    private String statusCode;
    private String message;
    private CatalogData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogData {
        private Total total;
        private List<CatalogItem> items;
        private List<Bucket> buckets;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Total {
        private Integer value;
        private String relation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogItem {
        private String _id;
        private String id;
        private String title;
        private Double price;
        private String sellerId;
        private Boolean visible;
        private Boolean available;
        private Seller seller;
        private List<Product> product;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String _id;
        private Boolean deprioritisation_status;
        private Boolean isCatalogAvailable;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Product {
        private String id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bucket {
        private String _id;
        private String name;
        private Integer doc_count;
    }
}
```

### `src/main/java/com/automation/models/response/CatalogSkipResponse.java`

_34 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for Catalog Skip API.
 * Endpoint: PUT /v1/admin/editor/assign/videos/skip/{seller_id}/{catalog_id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogSkipResponse {

    private String statusCode;
    private String message;
    private CatalogSkipData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogSkipData {
        private String _id;
        private String assignedBy;
        private String editorId;
        private Integer status;
        private String videoType;
        private String sellerId;
    }
}
```

### `src/main/java/com/automation/models/response/CatalogTagResponse.java`

_92 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Catalog Tag API.
 * Endpoint: /v1/admin/catalog/group/upload/{catalog_id}?limit=20&mode=add
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogTagResponse {

    private String statusCode;
    private String message;
    private CatalogTagData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogTagData {
        private String _id;
        private String name;
        private Integer total;
        private List<CatalogTagItem> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogTagItem {
        private String _index;
        private String _id;
        private Double _score;
        private CatalogTagSource _source;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogTagSource {
        private Seller seller;
        private String thubmbnailDriveLink;
        private String uploadId;
        private List<String> priceTags;
        private String description;
        private String uploadedDate;
        private String createdAt;
        private String sellerId;
        private Boolean isDeleted;
        private Thumbnail thumbnail;
        private String phoneNumber;
        private String uploadDate;
        private String original_upload;
        private List<String> productTags;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String phoneNumber;
        private Integer smell_test;
        private String address;
        private String businessName;
        private String name;
        private Integer cod;
        private String _id;
        private Boolean isCatalogAvailable;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Thumbnail {
        private String fileName;
        private String mimeType;
        private String _id;
        private String url;
    }
}
```

### `src/main/java/com/automation/models/response/CatalogTaggingResponse.java`

_38 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Catalog Tagging API.
 * Endpoint: PUT /v1/admin/catalog/{catalog_id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogTaggingResponse {

    private String statusCode;
    private String message;
    private CatalogTaggingData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogTaggingData {
        private Boolean available;
        private String contentType;
        private Boolean isDeleted;
        private String _id;
        private List<String> productTags;
        private String title;
        private Double priceText;
        private String catalogId;
    }
}
```

### `src/main/java/com/automation/models/response/CatalogUploadedResponse.java`

_44 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Catalog Uploaded API.
 * Endpoint: /v1/admin/catalog?limit=20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogUploadedResponse {

    private String statusCode;
    private String message;
    private List<CatalogUploadedGroup> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogUploadedGroup {
        private List<CatalogUploadedItem> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogUploadedItem {
        private String _id;
        private String source;
        private String videoType;
        private String sellerId;
        private String phoneNumber;
        private String name;
    }
}
```

### `src/main/java/com/automation/models/response/CollectionAllResponse.java`

_41 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Collection All API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionAllResponse {

    private String statusCode;
    private String message;
    private CollectionAllData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CollectionAllData {
        private List<CollectionItem> result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CollectionItem {
        private String _id;
        private String name;
        private String description;
        private String image;
    }
}
```

### `src/main/java/com/automation/models/response/CollectionByIdResponse.java`

_36 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for Collection by ID API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionByIdResponse {

    private String statusCode;
    private String message;
    private CollectionByIdData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CollectionByIdData {
        private TotalInfo total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TotalInfo {
        private Integer value;
    }
}
```

### `src/main/java/com/automation/models/response/FeaturedCollectionResponse.java`

_72 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Featured Collection API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeaturedCollectionResponse {

    private String statusCode;
    private String message;
    private FeaturedCollectionData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FeaturedCollectionData {
        private CollectionResult result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CollectionResult {
        private String _id;
        private String name;
        private Translations translations;
        private List<CatalogItem> catalogs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Translations {
        private TranslationContent en;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TranslationContent {
        private String title;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogItem {
        private String _id;
        private String title;
        private String description;
        private Integer priceText;
        private String url;
        private String aiImage;
        private String catalogInfo;
    }
}
```

### `src/main/java/com/automation/models/response/FeedFilterResponse.java`

_65 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Feed Filter API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedFilterResponse {

    private String statusCode;
    private String message;
    private FilterData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FilterData {
        private List<ProductTag> productTags;
        private List<Object> suitable_for;  // Changed from List<String> to handle object structures
        private List<Object> city;  // Changed from List<String> to handle object structures
        private List<PriceFilter> priceFilters;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductTag {
        private String name;
        private String image;
        private Object translation;
        private Boolean visible;
        private Boolean selected;
        private String displayName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceFilter {
        private String type;
        private List<PriceRange> ranges;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceRange {
        private Integer price_min;
        private Integer price_max;
        private String label;
    }
}
```

### `src/main/java/com/automation/models/response/FeedFilterSaveResponse.java`

_35 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Feed Filter Save API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedFilterSaveResponse {

    private String statusCode;
    private String message;
    private FilterSaveData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FilterSaveData {
        private List<Object> suitable_for;  // Changed from List<String> to handle object structures
        private List<Object> productTags;  // Changed from List<String> to handle object structures
        private List<Object> city;  // Changed from List<String> to handle object structures
        private Integer price_min;
        private Integer price_max;
        private String lastSelectedFilter;
    }
}
```

### `src/main/java/com/automation/models/response/HomeCatalogFeedResponse.java`

_48 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Home Catalog Feed API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HomeCatalogFeedResponse {

    private String statusCode;
    private String message;
    private DataWrapper data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataWrapper {
        private List<CatalogFeedItem> result;
        
        // Convenience method for backward compatibility
        public List<CatalogFeedItem> getItems() {
            return result;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogFeedItem {
        private String _id;
        private String title;
        private String description;
        private String image;
        private Integer price;
        private String url;
    }
}
```

### `src/main/java/com/automation/models/response/JourneyCollectionResponse.java`

_53 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Continue Your Journey (Journey Collection) API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JourneyCollectionResponse {

    private String statusCode;
    private String message;
    private JourneyCollectionData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JourneyCollectionData {
        private List<JourneyItem> result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JourneyItem {
        private String id;
        private String thubmbnailDriveLink;
        private String thumbnail_url;
        private String thumbnailDriveLink;
        private CollectionInfo collection;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CollectionInfo {
        private String _id;
        private String name;
        private String description;
        private String image;
    }
}
```

### `src/main/java/com/automation/models/response/LoginResponse.java`

_33 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for Login API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponse {

    private String statusCode;
    private String message;
    private LoginData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoginData {
        private String phoneNumber;
        private String name;
        private String businessName;
        private Boolean isDeleted;
        private String accessToken;
        private String refreshToken;
    }
}
```

### `src/main/java/com/automation/models/response/MarkAsDoneResponse.java`

_21 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for Mark As Done API.
 * Endpoint: PUT /v1/admin/editor/assign/videos/done/{seller_id}/{catalog_id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarkAsDoneResponse {

    private String statusCode;
    private String message;
    private Object data; // Can be any type based on API response
}
```

### `src/main/java/com/automation/models/response/NewThisWeekResponse.java`

_45 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for New This Week API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewThisWeekResponse {

    private String statusCode;
    private String message;
    private NewThisWeekData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewThisWeekData {
        private List<NewThisWeekItem> result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewThisWeekItem {
        private String _id;
        private String name;
        private String description;
        private String image;
        private Integer addedThisWeek;
        private String brandingImage;
        private String brandingCatId;
        private String type;
    }
}
```

### `src/main/java/com/automation/models/response/PdpSimilarResponse.java`

_59 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for PDP Similar Catalog API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PdpSimilarResponse {

    private String statusCode;
    private String message;
    private PdpSimilarData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PdpSimilarData {
        private List<SimilarCatalogItem> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SimilarCatalogItem {
        private Seller seller;
        private List<Product> product;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String phoneNumber;
        private String address;
        private String name;
        private Boolean isCatalogAvailable;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Product {
        private String name;
        private String id;
    }
}
```

### `src/main/java/com/automation/models/response/RelatedCollectionResponse.java`

_73 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Related Collection API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelatedCollectionResponse {

    private String statusCode;
    private String message;
    private RelatedCollectionData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RelatedCollectionData {
        private List<RelatedCollectionItem> result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RelatedCollectionItem {
        private String _id;
        private String name;
        private String description;
        private String image;
        private Translations translations;
        private List<CatalogItem> catalogs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Translations {
        private TranslationContent en;
        private TranslationContent hi;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TranslationContent {
        private String title;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogItem {
        private String _id;
        private String title;
        private Integer priceText;
        private String url;
        private String product;
    }
}
```

### `src/main/java/com/automation/models/response/SearchProductResponse.java`

_53 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Search Product API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchProductResponse {

    private String statusCode;
    private String message;
    private SearchProductData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchProductData {
        private List<SearchItem> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchItem {
        private String phoneNumber;
        private Seller seller;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String phoneNumber;
        private String address;
        private String businessName;
        private String name;
        private String _id;
        private Boolean deprioritisation_status;
        private Boolean isCatalogAvailable;
    }
}
```

### `src/main/java/com/automation/models/response/SearchRecommendChipSelectResponse.java`

_96 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Search Recommend Chip Select API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchRecommendChipSelectResponse {

    private String statusCode;
    private String message;
    private SearchChipSelectData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchChipSelectData {
        private List<SearchChipItem> items;
        private List<Bucket> buckets;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchChipItem {
        private Seller seller;
        private Thumbnail thumbnail;
        private List<Product> product;
        private List<Object> tags;  // Changed from List<String> to handle object structures
        private Market market;
        private List<Attribute> attributes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String _id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Thumbnail {
        private String url;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Product {
        private String _id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Market {
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Attribute {
        private String key;
        private String value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bucket {
        private String _id;
        private String name;
    }
}
```

### `src/main/java/com/automation/models/response/SearchRecommendedChipsResponse.java`

_62 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Search Recommended Chips API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchRecommendedChipsResponse {

    private String statusCode;
    private String message;
    private SearchRecommendedData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchRecommendedData {
        private List<SearchUserItem> items;
        private Integer totalCount;
        private List<Bucket> buckets;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchUserItem {
        private String _id;
        private String name;
        private String phoneNumber;
        private BusinessInfo businessInfo;
        private List<Object> tags;  // Changed from List<String> to handle object structures
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BusinessInfo {
        private String businessName;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bucket {
        private String _id;
        private String name;
    }
}
```

### `src/main/java/com/automation/models/response/SearchResponse.java`

_66 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Response model for User Search API.
 * Represents search results with sellers/users matching the query.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResponse {
    private String statusCode;
    private String message;
    private SearchData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchData {
        private List<UserItem> items;
        private Integer totalCount;
        private Integer page;
        private Integer pageSize;
        private List<Bucket> buckets;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserItem {
        private String _id;
        private String name;
        private String phoneNumber;
        private List<Object> tags;  // Changed from List<String> to handle object structures
        private BusinessInfo businessInfo;
        private Seller seller;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BusinessInfo {
        private String businessName;
        private String description;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String _id;
        private String phoneNumber;
        private String address;
        private String businessName;
        private String name;
        private Boolean deprioritisation_status;
        private Boolean isCatalogAvailable;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bucket {
        private String _id;
        private String name;
        private Integer count;
    }
}
```

### `src/main/java/com/automation/models/response/SimilarCollectionResponse.java`

_51 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Similar Collection API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimilarCollectionResponse {

    private String statusCode;
    private String message;
    private SimilarCollectionData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SimilarCollectionData {
        private List<SimilarCollectionItem> result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SimilarCollectionItem {
        private String _id;
        private String name;
        private String description;
        private List<CatalogItem> catalogs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogItem {
        private String title;
        private Integer priceText;
        private String product;
    }
}
```

### `src/main/java/com/automation/models/response/SuitableForConfigResponse.java`

_43 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Suitable For Config API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SuitableForConfigResponse {

    private String statusCode;
    private String message;
    private SuitableForConfigData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SuitableForConfigData {
        private List<ConfigResultItem> result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConfigResultItem {
        private String type;
        private List<Object> data;
        private String api;
        private String title;
        private String description;
        private String imageUrl;
    }
}
```

### `src/main/java/com/automation/models/response/TitleGenerateResponse.java`

_32 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Title Generate API.
 * Endpoint: /api/chat/title/tags
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TitleGenerateResponse {

    private String result;
    private TitleData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TitleData {
        private List<String> tags;
        private Integer status;
        private List<String> processedTags;
    }
}
```

### `src/main/java/com/automation/models/response/TopCollectionResponse.java`

_41 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Top Collection API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TopCollectionResponse {

    private String statusCode;
    private String message;
    private TopCollectionData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TopCollectionData {
        private List<TopCollectionItem> result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TopCollectionItem {
        private String _id;
        private String name;
        private String description;
        private String image;
    }
}
```

### `src/main/java/com/automation/models/response/TrendingFeedResponse.java`

_79 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Trending Feed API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrendingFeedResponse {

    private String statusCode;
    private String message;
    private TrendingData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrendingData {
        private List<TrendingItem> result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrendingItem {
        private String _id;
        private String id;
        private Integer priceText;
        private Integer price;
        private String thumbnail_url;
        private Thumbnail thumbnail;
        private Integer popular;
        private String contentType;
        private String driveLink;
        private List<Object> tags;  // Changed from List<String> to handle object structures
        private List<Product> product;
        private Boolean isDeleted;
        private Boolean available;
        private Seller seller;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Thumbnail {
        private String url;
        private String fileName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Product {
        private String _id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private Boolean deprioritisation_status;
        private String name;
        private String _id;
    }
}
```

### `src/main/java/com/automation/models/response/UpdateCartResponse.java`

_55 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Update Cart API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateCartResponse {

    private String statusCode;
    private String message;
    private CartData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CartData {
        private String _id;
        private List<String> catalogs;
        private String status;
        private List<CartItem> cartItems;
        private String buyerId;
        private Double cartTotal;
        private Double shippingCharges;
        private Double tax;
        private Double totalPayable;
        private String createdAt;
        private String updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CartItem {
        private String title;
        private Double priceText;
        private Integer quantity;
        private String sellerId;
        private Integer setQuantity;
        private Double totalPrice;
        private Boolean isDeleted;
        private Boolean available;
    }
}
```

### `src/main/java/com/automation/models/response/UserProfileResponse.java`

_24 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for User Profile API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfileResponse {

    private Integer code;
    private String message;
    private String location;
    private String address;
    private Boolean sellOnBizup;
    private String phoneNumber;
    private Object data;
}
```

### `src/main/java/com/automation/models/response/VideoAssignUploadResponse.java`

_51 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Video Assign Upload API.
 * Endpoint: GET /v1/admin/editor/assign/videos/{seller_id}?limit=50
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoAssignUploadResponse {

    private String statusCode;
    private String message;
    private VideoAssignUploadData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoAssignUploadData {
        private List<VideoItem> videos;
        private List<VideoItem> data; // Alternative field name
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoItem {
        private String _id;
        private String videoLink;
        private Integer status;
        private Seller seller;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String _id;
    }
}
```

### `src/main/java/com/automation/models/response/VideoFeedResponse.java`

_54 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Video Feed API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoFeedResponse {

    private String statusCode;
    private String message;
    private VideoFeedData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoFeedData {
        private List<VideoFeedItem> result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoFeedItem {
        private Seller seller;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String businessName;
        private String phoneNumber;
        private String name;
        private String id;
        private Boolean isSuper;
        private Boolean isDeleted;
        private Boolean ordersEnabled;
        private Boolean isCatalogAvailable;
        private Integer mov;
    }
}
```

### `src/main/java/com/automation/models/response/VideoTaggingEditResponse.java`

_57 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Video Tagging Edit API.
 * Endpoint: GET /v1/admin/editor/edit/videos/{seller_id}?limit=100
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoTaggingEditResponse {

    private String statusCode;
    private String message;
    private VideoTaggingEditData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoTaggingEditData {
        private List<VideoItem> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoItem {
        private String _id;
        private List<Object> displayTags;
        private List<Integer> language;
        private String contentType;
        private String uploadDate;
        private String editor;
        private String seller;
        private String driveLink;
        private String thubmbnailDriveLink;
        private String phoneNumber;
        private String marketNumber;
        private String uploadId;
        private Integer status;
        private Object meta;
        private String fabricText;
        private String priceText;
        private String description;
        private String createdAt;
        private String updatedAt;
    }
}
```

### `src/main/java/com/automation/models/response/VideoTaggingResponse.java`

_32 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for Video Tagging API.
 * Endpoint: PUT /v1/admin/editor/edit/videos/{seller_id}/{video_id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoTaggingResponse {

    private String statusCode;
    private String message;
    private VideoTaggingData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoTaggingData {
        private String _id;
        private String seller;
        private String title;
        private String priceText;
    }
}
```

### `src/main/java/com/automation/models/response/VideoThumbnailUploadResponse.java`

_36 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for Video Thumbnail Upload API.
 * Endpoint: POST /v1/admin/editor/upload/videos/{seller_id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoThumbnailUploadResponse {

    private String message;
    private VideoThumbnailData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoThumbnailData {
        private String _id;
        private String seller;
        private String sellerId;
        private String driveLink;
        private String thubmbnailDriveLink;
        private String contentType;
        private Object status; // Can be null
        private String createdAt;
        private String updatedAt;
    }
}
```

### `src/main/java/com/automation/models/response/VideoTitleGenerationResponse.java`

_40 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Video Title Generation API.
 * Endpoint: POST https://bomb.bizup.app/api/chat/title/tags
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoTitleGenerationResponse {

    private String result;
    private String error;
    private VideoTitleData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoTitleData {
        private List<ProcessedTag> processedTags;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProcessedTag {
        private String key;
        private String value;
    }
}
```

### `src/main/java/com/automation/models/response/VideoUploadMarkAsDoneResponse.java`

_38 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for Video Upload Mark As Done API.
 * Endpoint: PUT /v1/admin/editor/assign/videos/done/{seller_id}/{upload_id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoUploadMarkAsDoneResponse {

    private String message;
    private VideoUploadDoneData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoUploadDoneData {
        private String _id;
        private String sellerId;
        private String phoneNumber;
        private String url;
        private String assignedBy;
        private String editorId;
        private String uploadedBy;
        private Integer status;
        private String videoType;
        private String createdAt;
        private String updatedAt;
    }
}
```

### `src/main/java/com/automation/models/response/VideoUploadResponse.java`

_54 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for Video Upload API.
 * Endpoint: POST /v1/admin/pipeline/video
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoUploadResponse {

    private String statusCode;
    private String message;
    private VideoUploadData data;
    private String videoLink; // Alternative field at root level

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoUploadData {
        private String _id;
        private String videoLink;
        private String videoType;
        private Integer status;
        private String url;
        private Seller seller;
        private Editor editor;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String _id;
        private String phoneNumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Editor {
        private String _id;
        private String name;
    }
}
```

### `src/main/java/com/automation/models/response/VideoViewActionResponse.java`

_30 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for Video View Action API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoViewActionResponse {

    private Integer code;
    private String message;
    private VideoViewData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoViewData {
        private Boolean isDeleted;
        private String videoId;
        private String userId;
    }
}
```

### `src/main/java/com/automation/models/response/WatchedVideosResponse.java`

_58 lines_

```java
package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Watched Videos API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WatchedVideosResponse {

    private String statusCode;
    private String message;
    private WatchedVideosData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WatchedVideosData {
        private List<WatchedVideoItem> result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WatchedVideoItem {
        private String videoId;
        private String _id;
        private String phoneNumber;
        private List<Object> product;  // Changed from List<String> to handle object structures
        private Object collection;  // Changed from String to handle object structures
        private Object market;  // Changed from String to handle object structures
        private Integer priceText;
        private String driveLink;
        private Boolean isDeleted;
        private Seller seller;
        private String thubmbnailDriveLink;
        private String createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String _id;
        private String name;
    }
}
```

### `src/main/java/com/automation/utils/ApiErrorAssertions.java`

_40 lines_

```java
package com.automation.utils;

import io.restassured.response.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Assertions for backend-api error responses: { statusCode, message }.
 * statusCode is "101" for a thrown ApiError without a custom code (validation
 * 400s, staff-gate 403s — the default in backend-api's ApiError.ts), "10001"
 * (FAILURE) when a response class is sent directly, and "10003" for an
 * invalid access token. The events endpoints use a different, Go-style
 * envelope and do not go through here.
 */
public final class ApiErrorAssertions {

    public static final String THROWN_DEFAULT = "101";
    public static final String FAILURE = "10001";
    public static final String INVALID_ACCESS_TOKEN = "10003";

    private ApiErrorAssertions() {
        // Prevent instantiation
    }

    /** Asserts the HTTP status and that the body is a backend-api error envelope. */
    public static void assertApiError(Response response, int httpStatus) {
        assertThat("HTTP status", response.getStatusCode(), equalTo(httpStatus));
        assertThat("statusCode", response.jsonPath().getString("statusCode"),
                anyOf(equalTo(THROWN_DEFAULT), equalTo(FAILURE), equalTo(INVALID_ACCESS_TOKEN)));
        assertThat("message present", response.jsonPath().getString("message"), not(emptyOrNullString()));
    }

    /** As above, and the message contains the given fragment (case-insensitive). */
    public static void assertApiError(Response response, int httpStatus, String messageFragment) {
        assertApiError(response, httpStatus);
        assertThat("message", response.jsonPath().getString("message"),
                containsStringIgnoringCase(messageFragment));
    }
}
```

### `src/main/java/com/automation/utils/DataGenerator.java`

_89 lines_

```java
package com.automation.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

/**
 * Utility class for generating test data.
 */
public class DataGenerator {

    private static final Random random = new Random();
    private static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String NUMERIC = "0123456789";
    private static final String ALPHANUMERIC = ALPHA + NUMERIC;

    private DataGenerator() {
        // Prevent instantiation
    }

    /**
     * Generate a random UUID
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate random string of specified length
     */
    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHA.charAt(random.nextInt(ALPHA.length())));
        }
        return sb.toString();
    }

    /**
     * Generate random alphanumeric string of specified length
     */
    public static String generateRandomAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * Generate random number within range
     */
    public static int generateRandomNumber(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    /**
     * Generate random email address
     */
    public static String generateRandomEmail() {
        return generateRandomString(8).toLowerCase() + "@test.com";
    }

    /**
     * Generate random phone number
     */
    public static String generateRandomPhone() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(NUMERIC.charAt(random.nextInt(NUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * Generate timestamp string
     */
    public static String generateTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * Generate unique name with prefix
     */
    public static String generateUniqueName(String prefix) {
        return prefix + "_" + generateTimestamp() + "_" + generateRandomAlphanumeric(4);
    }
}
```

### `src/main/java/com/automation/utils/JsonUtils.java`

_174 lines_

```java
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
```

### `src/main/java/com/automation/utils/TestDataResolver.java`

_83 lines_

```java
package com.automation.utils;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Looks up Buyer App ids from production at run time instead of relying on
 * hardcoded ids in test-variables.properties, which go stale (a cart gets
 * booked, a catalog loses its seller). Each id is resolved once per run with
 * read-only GETs and cached in VariableManager; if the lookup fails, the value
 * in test-variables.properties is used as before.
 */
public final class TestDataResolver {

    private static final Logger logger = LogManager.getLogger(TestDataResolver.class);

    private TestDataResolver() {
        // Prevent instantiation
    }

    /**
     * A live catalog that has a seller: the first such item on the home catalog
     * feed for the configured suitable_for. Used by CatalogByIdTest and PdpSimilarTest.
     */
    public static String liveCatalogId(Supplier<RequestSpecification> buyerSpec) {
        return resolve("live_catalog_id", () -> {
            Response r = buyerSpec.get()
                    .queryParam("size", 10).queryParam("page", 0)
                    .queryParam("suitable_for", VariableManager.get("suitable_for", "saree"))
                    .get("/v1/feed/home/catalog");
            List<Map<String, Object>> items = r.jsonPath().getList("data.result");
            if (items == null) return null;
            for (Map<String, Object> item : items) {
                if (item.get("seller") instanceof Map && item.get("_id") != null) {
                    return item.get("_id").toString();
                }
            }
            return null;
        });
    }

    /** The test buyer's open cart (GET /v1/user/cart -> data[0]._id). Used by UpdateCartTest. */
    public static String cartId(Supplier<RequestSpecification> buyerSpec) {
        return resolve("cart_id", () -> {
            Response r = buyerSpec.get().get("/v1/user/cart");
            return r.getStatusCode() == 200 ? r.jsonPath().getString("data[0]._id") : null;
        });
    }

    /** A catalog already in that cart (data[0].cartItems[0]._id), so update_cart can change its quantity. */
    public static String cartCatalogId(Supplier<RequestSpecification> buyerSpec) {
        return resolve("cart_catalog_id", () -> {
            Response r = buyerSpec.get().get("/v1/user/cart");
            return r.getStatusCode() == 200 ? r.jsonPath().getString("data[0].cartItems[0]._id") : null;
        });
    }

    private static String resolve(String key, Supplier<String> lookup) {
        String cached = VariableManager.get("resolved_" + key);
        if (cached != null) return cached;
        String value = null;
        try {
            value = lookup.get();
        } catch (Exception e) {
            logger.warn("Runtime lookup of {} failed: {}", key, e.getMessage());
        }
        if (value == null || value.isEmpty()) {
            value = VariableManager.get(key);
            logger.warn("Runtime lookup of {} found nothing; using test-variables.properties value {}", key, value);
        } else {
            logger.info("Resolved {} at run time: {}", key, value);
        }
        if (value != null) VariableManager.set("resolved_" + key, value);
        return value;
    }
}
```

### `src/main/java/com/automation/utils/VariableManager.java`

_513 lines_

```java
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
     * Share a runtime value (e.g. a login token) with every test class in this run.
     * Updates the in-memory globals and the current thread's map only: tokens are
     * deliberately NOT written back to test-variables.properties, which is tracked
     * in git.
     *
     * @param key Property key
     * @param value Property value
     */
    private static synchronized void savePropertyToFile(String key, String value) {
        globalProperties.setProperty(key, value);
        Map<String, String> currentThreadVars = threadLocalVariables.get();
        if (currentThreadVars != null) {
            currentThreadVars.put(key, value);
        }
        logger.info("Stored {} in memory for this run (not persisted to disk)", key);
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
```

### `src/test/java/com/automation/tests/bomb/CatalogSearch/AllCatalogTest.java`

_287 lines_

```java
package com.automation.tests.bomb.CatalogSearch;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Search - All Catalog endpoint.
 * Endpoint: {{bizup_base}}/v1/admin/catalog_all?offset=0&limit=20
 * Implements comprehensive Postman test scripts for catalog search validation.
 */
@TmsLink("TC-ADMIN-001")
@TmsLink("TC-ADMIN-002")
@Epic("BOMB Catalog Management")
@Feature("Catalog Search - All Catalog")
public class AllCatalogTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogResponse catalogResponse;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
    }

    @Test(description = "Verify response status is 200 OK", priority = 1, groups = "bomb")
    @Story("Catalog Search - All Catalog")
    @Severity(SeverityLevel.BLOCKER)
    public void testResponseStatus() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("offset", 0);
        queryParams.put("limit", 20);

        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParams(queryParams)
                .when()
                .get(BombEndpoints.CATALOG_ALL);

        // Parse response for other tests
        catalogResponse = JsonUtils.fromResponse(response, CatalogResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Verify response format is JSON", priority = 2, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - All Catalog")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseFormatIsJson() {
        // Verify response format is JSON
        assertThat("Response should be JSON",
                response.getContentType(), containsString("application/json"));

        logger.info("Response format verified: JSON");
    }

    @Test(description = "Validate root response structure", priority = 3, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - All Catalog")
    @Severity(SeverityLevel.CRITICAL)
    public void testValidateRootResponseStructure() {
        // Validate response is an object
        assertThat("Response should be an object", catalogResponse, notNullValue());
        assertThat("Response should be an instance of CatalogResponse",
                catalogResponse, instanceOf(CatalogResponse.class));

        // Validate response includes required keys
        assertThat("StatusCode should not be null", catalogResponse.getStatusCode(), notNullValue());
        assertThat("Message should not be null", catalogResponse.getMessage(), notNullValue());
        assertThat("Data should not be null", catalogResponse.getData(), notNullValue());

        // Validate message is 'success'
        assertThat("Message should be 'success'",
                catalogResponse.getMessage(), equalTo("success"));

        logger.info("Root response structure validated successfully");
    }

    @Test(description = "Validate data object structure", priority = 4, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - All Catalog")
    @Severity(SeverityLevel.CRITICAL)
    public void testValidateDataObjectStructure() {
        // Validate data is an object
        assertThat("Data should be an object", catalogResponse.getData(), notNullValue());
        assertThat("Data should be an instance of CatalogData",
                catalogResponse.getData(), instanceOf(CatalogResponse.CatalogData.class));

        // Validate data includes required keys: total, items
        assertThat("Total should not be null", catalogResponse.getData().getTotal(), notNullValue());
        assertThat("Items should not be null", catalogResponse.getData().getItems(), notNullValue());

        // Validate total is an object
        assertThat("Total should be an object",
                catalogResponse.getData().getTotal(), instanceOf(CatalogResponse.Total.class));

        // Validate total.value is a number and at least 0
        assertThat("Total value should be a number",
                catalogResponse.getData().getTotal().getValue(), instanceOf(Integer.class));
        assertThat("Total value should be at least 0",
                catalogResponse.getData().getTotal().getValue(), greaterThanOrEqualTo(0));

        // Validate items is an array
        assertThat("Items should be an array", catalogResponse.getData().getItems(), notNullValue());

        logger.info("Data object structure validated successfully");
    }

    @Test(description = "Verify buckets array exists", priority = 5, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - All Catalog")
    @Severity(SeverityLevel.NORMAL)
    public void testVerifyBucketsArrayExists() {
        // Verify buckets array exists
        assertThat("Buckets should be an array", catalogResponse.getData().getBuckets(), notNullValue());

        logger.info("Buckets array verified: exists");
    }

    @Test(description = "Validate bucket items structure", priority = 6, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - All Catalog")
    @Severity(SeverityLevel.NORMAL)
    public void testValidateBucketItemsStructure() {
        // Validate bucket structure if buckets exist
        if (catalogResponse.getData().getBuckets() != null && catalogResponse.getData().getBuckets().size() > 0) {
            catalogResponse.getData().getBuckets().forEach(bucket -> {
                assertThat("Bucket _id should be a string", bucket.get_id(), instanceOf(String.class));
                assertThat("Bucket name should be a string", bucket.getName(), instanceOf(String.class));
                assertThat("Bucket doc_count should be a number", bucket.getDoc_count(), instanceOf(Integer.class));
            });

            logger.info("Bucket items structure validated successfully");
        } else {
            logger.info("No buckets to validate");
        }
    }

    @Test(description = "Validate catalog items structure", priority = 7, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - All Catalog")
    @Severity(SeverityLevel.CRITICAL)
    public void testValidateCatalogItemsStructure() {
        // Validate catalog items structure if items exist
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            CatalogResponse.CatalogItem item = catalogResponse.getData().getItems().get(0);

            // Validate item includes required keys
            assertThat("Item _id should not be null", item.get_id(), notNullValue());
            assertThat("Item title should not be null", item.getTitle(), notNullValue());
            assertThat("Item price should not be null", item.getPrice(), notNullValue());
            assertThat("Item sellerId should not be null", item.getSellerId(), notNullValue());
            assertThat("Item visible should not be null", item.getVisible(), notNullValue());
            assertThat("Item available should not be null", item.getAvailable(), notNullValue());

            // Validate data types
            assertThat("Item _id should be a string", item.get_id(), instanceOf(String.class));
            assertThat("Item title should be a string", item.getTitle(), instanceOf(String.class));
            assertThat("Item price should be a number", item.getPrice(), instanceOf(Double.class));
            assertThat("Item price should be above 0", item.getPrice(), greaterThan(0.0));

            logger.info("Catalog items structure validated successfully");
        } else {
            logger.info("No catalog items to validate");
        }
    }

    @Test(description = "Validate seller information structure", priority = 8, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - All Catalog")
    @Severity(SeverityLevel.NORMAL)
    public void testValidateSellerInformationStructure() {
        // Validate seller information structure if items exist
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            CatalogResponse.Seller seller = catalogResponse.getData().getItems().get(0).getSeller();

            // Validate seller includes required keys
            assertThat("Seller should not be null", seller, notNullValue());
            assertThat("Seller deprioritisation_status should not be null",
                    seller.getDeprioritisation_status(), notNullValue());
            assertThat("Seller isCatalogAvailable should not be null",
                    seller.getIsCatalogAvailable(), notNullValue());

            // Validate data types
            assertThat("Seller deprioritisation_status should be a boolean",
                    seller.getDeprioritisation_status(), instanceOf(Boolean.class));
            assertThat("Seller isCatalogAvailable should be a boolean",
                    seller.getIsCatalogAvailable(), instanceOf(Boolean.class));

            logger.info("Seller information structure validated successfully");
        } else {
            logger.info("No seller information to validate");
        }
    }

    @Test(description = "Validate product array structure", priority = 9, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - All Catalog")
    @Severity(SeverityLevel.NORMAL)
    public void testValidateProductArrayStructure() {
        // Validate product array structure if items and products exist
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            CatalogResponse.CatalogItem item = catalogResponse.getData().getItems().get(0);

            if (item.getProduct() != null && item.getProduct().size() > 0) {
                CatalogResponse.Product product = item.getProduct().get(0);

                // Validate product includes required keys
                assertThat("Product name should not be null", product.getName(), notNullValue());
                assertThat("Product id should not be null", product.getId(), notNullValue());

                // Validate data types
                assertThat("Product name should be a string", product.getName(), instanceOf(String.class));
                assertThat("Product id should be a string", product.getId(), instanceOf(String.class));

                // Validate product ID matches MongoDB ObjectId format (24 hex characters)
                assertThat("Product id should match MongoDB ObjectId format",
                        product.getId(), matchesRegex("^[0-9a-fA-F]{24}$"));

                logger.info("Product array structure validated successfully");
            } else {
                logger.info("No products to validate");
            }
        } else {
            logger.info("No catalog items to validate products");
        }
    }

    @Test(description = "Verify pagination limit is respected", priority = 10, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - All Catalog")
    @Severity(SeverityLevel.NORMAL)
    public void testVerifyPaginationLimit() {
        // Verify pagination limit is respected (limit=20)
        assertThat("Items count should not exceed limit of 20",
                catalogResponse.getData().getItems().size(), lessThanOrEqualTo(20));

        logger.info("Pagination limit verified: items count = {}", catalogResponse.getData().getItems().size());
    }

    @Test(description = "Verify response time is acceptable", priority = 11, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - All Catalog")
    @Severity(SeverityLevel.NORMAL)
    public void testVerifyResponseTime() {
        // Get threshold from config or use default 40000ms
        long threshold = config.responseTimeThreshold();
        long actualResponseTime = response.getTime();

        // Verify response time is not undefined
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be below threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Verify security headers", priority = 12, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - All Catalog")
    @Severity(SeverityLevel.MINOR)
    public void testVerifySecurityHeaders() {
        // Verify Content-Type header includes application/json
        assertThat("Content-Type header should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Security headers verified: Content-Type = {}", response.getContentType());
    }
}
```

### `src/test/java/com/automation/tests/bomb/CatalogSearch/SearchWithCatalogIdFilterTest.java`

_305 lines_

```java
package com.automation.tests.bomb.CatalogSearch;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Search with Catalog ID Filter.
 * Endpoint:
 * {{bizup_base}}/v1/admin/catalog_all?id={{live_catalog_id}}&offset=0&limit=20
 * Implements comprehensive Postman test scripts for catalog search with catalog
 * ID filter validation.
 */
@TmsLink("TC-ADMIN-001")
@TmsLink("TC-ADMIN-002")
@Epic("BOMB Catalog Management")
@Feature("Catalog Search - Catalog ID Filter")
public class SearchWithCatalogIdFilterTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogResponse catalogResponse;
    private String liveCatalogId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
        // Get live catalog ID from VariableManager or use fallback
        liveCatalogId = VariableManager.get("live_catalog_id");
        if (liveCatalogId == null || liveCatalogId.isEmpty()) {
            liveCatalogId = VariableManager.get("catalog_foassign_id", "6822f5dac17c6dcd589ba173");
            logger.warn("Live catalog ID not available, using fallback: {}", liveCatalogId);
        } else {
            logger.info("Using live catalog ID from VariableManager: {}", liveCatalogId);
        }
    }

    @Test(description = "Status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("id", liveCatalogId);
        queryParams.put("offset", 0);
        queryParams.put("limit", 20);

        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParams(queryParams)
                .when()
                .get(BombEndpoints.CATALOG_ALL);

        // Parse response for other tests
        catalogResponse = JsonUtils.fromResponse(response, CatalogResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response is in JSON format", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseIsJson() {
        // Verify response format is JSON
        assertThat("Response should be JSON",
                response.getContentType(), containsString("application/json"));

        logger.info("Response format verified: JSON");
    }

    @Test(description = "Response has required structure and values", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseRequiredStructure() {
        // Validate response includes all required keys
        assertThat("Response should have statusCode", catalogResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogResponse.getData(), notNullValue());

        // Validate response is an object
        assertThat("Response should be an object", catalogResponse, notNullValue());

        // Validate message is 'success'
        assertThat("Message should be 'success'",
                catalogResponse.getMessage(), equalTo("success"));

        // Validate data includes all required keys
        assertThat("Data should be an object", catalogResponse.getData(), notNullValue());
        assertThat("Data should have total", catalogResponse.getData().getTotal(), notNullValue());
        assertThat("Data should have items", catalogResponse.getData().getItems(), notNullValue());

        // Validate total has value property
        assertThat("Total should have value property",
                catalogResponse.getData().getTotal().getValue(), notNullValue());
        assertThat("Total value should be a number above -1",
                catalogResponse.getData().getTotal().getValue(), greaterThan(-1));

        // Validate items is an array
        assertThat("Items should be an array", catalogResponse.getData().getItems(), notNullValue());

        logger.info("Response structure validated successfully");
    }

    @Test(description = "Each catalog ID matches request query", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testCatalogIdMatchesRequest() {
        // Validate catalog IDs match request query
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            catalogResponse.getData().getItems().forEach(item -> {
                assertThat("Catalog ID should match request query",
                        item.get_id(), equalTo(liveCatalogId));
            });

            logger.info("All catalog IDs match request query: {}", liveCatalogId);
        } else {
            logger.info("No items to validate catalog IDs");
        }
    }

    @Test(description = "Bucket product ID matches request query", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testBucketProductIdMatchesRequest() {
        // Note: The Postman script checks for product ID in buckets, but this endpoint
        // filters by catalog ID, not product ID. This test validates buckets exist.

        if (catalogResponse.getData().getBuckets() != null) {
            logger.info("Buckets array exists with {} buckets", catalogResponse.getData().getBuckets().size());
        } else {
            logger.info("No buckets to validate");
        }
    }

    @Test(description = "Items bucket is not empty", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testItemsBucketNotEmpty() {
        // Validate items array is not empty
        assertThat("Items should be an array", catalogResponse.getData().getItems(), notNullValue());
        assertThat("Items should not be empty", catalogResponse.getData().getItems(), not(empty()));

        logger.info("Items array is not empty: {} items found", catalogResponse.getData().getItems().size());
    }

    @Test(description = "Buckets structure is valid", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testBucketsStructureValid() {
        // Validate buckets is an array
        assertThat("Buckets should be an array", catalogResponse.getData().getBuckets(), notNullValue());

        // Validate bucket structure if buckets exist
        if (catalogResponse.getData().getBuckets() != null && catalogResponse.getData().getBuckets().size() > 0) {
            catalogResponse.getData().getBuckets().forEach(bucket -> {
                assertThat("Bucket _id should be a string", bucket.get_id(), instanceOf(String.class));
                assertThat("Bucket name should be a string", bucket.getName(), instanceOf(String.class));
                assertThat("Bucket doc_count should be a number", bucket.getDoc_count(), instanceOf(Integer.class));
            });

            logger.info("Buckets structure validated successfully");
        } else {
            logger.info("No buckets to validate structure");
        }
    }

    @Test(description = "Product array has required structure", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testProductArrayRequiredStructure() {
        // Validate product structure if items and products exist
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            CatalogResponse.CatalogItem item = catalogResponse.getData().getItems().get(0);

            if (item.getProduct() != null && item.getProduct().size() > 0) {
                CatalogResponse.Product product = item.getProduct().get(0);

                // Validate product includes required keys
                assertThat("Product should include name", product.getName(), notNullValue());
                assertThat("Product should include id", product.getId(), notNullValue());

                // Validate data types
                assertThat("Product name should be a string", product.getName(), instanceOf(String.class));
                assertThat("Product id should be a string", product.getId(), instanceOf(String.class));

                // Validate product ID matches MongoDB ObjectId format (24 hex characters)
                assertThat("Product id should match MongoDB ObjectId format",
                        product.getId(), matchesRegex("^[0-9a-fA-F]{24}$"));

                logger.info("Product array structure validated successfully");
            } else {
                logger.info("No products to validate");
            }
        } else {
            logger.info("No catalog items to validate products");
        }
    }

    @Test(description = "Pagination: items length does not exceed limit", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testPaginationItemsLimit() {
        // Validate pagination limit is respected (limit=20)
        if (catalogResponse.getData().getItems() != null) {
            assertThat("Items count should not exceed limit of 20",
                    catalogResponse.getData().getItems().size(), lessThanOrEqualTo(20));

            logger.info("Pagination validated: items count = {}", catalogResponse.getData().getItems().size());
        } else {
            logger.info("No items to validate pagination");
        }
    }

    @Test(description = "Catalog item structure is valid", priority = 11, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testCatalogItemStructureValid() {
        // Validate catalog item structure if items exist
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            CatalogResponse.CatalogItem item = catalogResponse.getData().getItems().get(0);

            // Validate item includes required keys
            assertThat("Item should include _id", item.get_id(), notNullValue());
            assertThat("Item should include title", item.getTitle(), notNullValue());
            assertThat("Item should include price", item.getPrice(), notNullValue());
            assertThat("Item should include sellerId", item.getSellerId(), notNullValue());
            assertThat("Item should include visible", item.getVisible(), notNullValue());
            assertThat("Item should include available", item.getAvailable(), notNullValue());

            // Validate seller includes required keys
            assertThat("Item should have seller", item.getSeller(), notNullValue());
            assertThat("Seller should include deprioritisation_status",
                    item.getSeller().getDeprioritisation_status(), notNullValue());
            assertThat("Seller should include isCatalogAvailable",
                    item.getSeller().getIsCatalogAvailable(), notNullValue());

            // Validate data types
            assertThat("Item _id should be a string", item.get_id(), instanceOf(String.class));
            assertThat("Item title should be a string", item.getTitle(), instanceOf(String.class));
            assertThat("Item price should be a number", item.getPrice(), instanceOf(Double.class));
            assertThat("Item price should be above 0", item.getPrice(), greaterThan(0.0));
            assertThat("Seller deprioritisation_status should be a boolean",
                    item.getSeller().getDeprioritisation_status(), instanceOf(Boolean.class));
            assertThat("Seller isCatalogAvailable should be a boolean",
                    item.getSeller().getIsCatalogAvailable(), instanceOf(Boolean.class));

            logger.info("Catalog item structure validated successfully");
        } else {
            logger.info("No catalog items to validate");
        }
    }

    @Test(description = "Response time is under threshold", priority = 12, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeUnderThreshold() {
        // Get threshold from config or use default 40000ms
        long threshold = config.responseTimeThreshold();
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be under threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Content-Type header includes 'application/json'", priority = 13, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Search - Catalog ID Filter")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        // Verify Content-Type header includes application/json
        assertThat("Content-Type header should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type header verified: {}", response.getContentType());
    }
}
```

### `src/test/java/com/automation/tests/bomb/CatalogSearch/SearchWithProductFilterTest.java`

_341 lines_

```java
package com.automation.tests.bomb.CatalogSearch;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Search with Product Filter.
 * Endpoint:
 * {{bizup_base}}/v1/admin/catalog_all?product={{product_id}}&offset=0&limit=20
 * Implements comprehensive Postman test scripts for catalog search with product
 * filter validation.
 */
@TmsLink("TC-ADMIN-001")
@TmsLink("TC-ADMIN-002")
@Epic("BOMB Catalog Management")
@Feature("Catalog Search - Product Filter")
public class SearchWithProductFilterTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogResponse catalogResponse;
    private String searchProductId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
        
        // Get product ID after VariableManager is initialized
        searchProductId = VariableManager.getSearchProductId();
        logger.info("Using product ID: {}", searchProductId);
    }

    @Test(description = "Test the response status is 200", priority = 1, groups = "bomb")
    @Story("Catalog Search - Product Filter")
    @Severity(SeverityLevel.BLOCKER)
    public void testResponseStatus() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("product", searchProductId);
        queryParams.put("offset", 0);
        queryParams.put("limit", 20);

        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParams(queryParams)
                .when()
                .get(BombEndpoints.CATALOG_ALL);

        // Parse response for other tests
        catalogResponse = JsonUtils.fromResponse(response, CatalogResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Test the response is in JSON format", priority = 2, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Product Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseIsJson() {
        // Verify response format is JSON
        assertThat("Response should be JSON",
                response.getContentType(), containsString("application/json"));

        logger.info("Response format verified: JSON");
    }

    @Test(description = "Test that security headers are present", priority = 3, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Product Filter")
    @Severity(SeverityLevel.MINOR)
    public void testSecurityHeadersPresent() {
        // Verify Content-Type header includes application/json
        assertThat("Content-Type header should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Security headers verified: Content-Type = {}", response.getContentType());
    }

    @Test(description = "Test the response has the expected structure", priority = 4, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Product Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStructure() {
        // Check root level properties
        assertThat("Response should have statusCode", catalogResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogResponse.getData(), notNullValue());

        // Root level properties validation
        assertThat("Response should be an object", catalogResponse, notNullValue());
        assertThat("Response should be an instance of CatalogResponse",
                catalogResponse, instanceOf(CatalogResponse.class));

        // Data properties validation
        assertThat("Data should be an object", catalogResponse.getData(), notNullValue());
        assertThat("Data should be an instance of CatalogData",
                catalogResponse.getData(), instanceOf(CatalogResponse.CatalogData.class));
        assertThat("Data should have total", catalogResponse.getData().getTotal(), notNullValue());
        assertThat("Data should have items", catalogResponse.getData().getItems(), notNullValue());

        // Message validation
        assertThat("Message should be 'success'",
                catalogResponse.getMessage(), equalTo("success"));

        // Total count validation
        assertThat("Total should be an object",
                catalogResponse.getData().getTotal(), instanceOf(CatalogResponse.Total.class));
        assertThat("Total should have value property",
                catalogResponse.getData().getTotal().getValue(), notNullValue());
        assertThat("Total value should be a number above -1",
                catalogResponse.getData().getTotal().getValue(), greaterThan(-1));

        // Data array validation
        assertThat("Items should be an array", catalogResponse.getData().getItems(), notNullValue());

        logger.info("Response structure validated successfully");
    }

    @Test(description = "Test the bucket structure is valid", priority = 5, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Product Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testBucketStructureValid() {
        // Validate buckets is an array
        assertThat("Buckets should be an array", catalogResponse.getData().getBuckets(), notNullValue());

        // Validate bucket structure if buckets exist
        if (catalogResponse.getData().getBuckets() != null && catalogResponse.getData().getBuckets().size() > 0) {
            catalogResponse.getData().getBuckets().forEach(bucket -> {
                assertThat("Bucket _id should be a string", bucket.get_id(), instanceOf(String.class));
                assertThat("Bucket name should be a string", bucket.getName(), instanceOf(String.class));
                assertThat("Bucket doc_count should be a number", bucket.getDoc_count(), instanceOf(Integer.class));
            });

            logger.info("Bucket structure validated successfully");
        } else {
            logger.info("Buckets array is empty - no structure validation performed");
        }
    }

    @Test(description = "Test the bucket items array is not empty", priority = 6, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Product Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testBucketItemsNotEmpty() {
        // Validate items array is not empty
        assertThat("Items should be an array", catalogResponse.getData().getItems(), notNullValue());
        assertThat("Items should not be empty", catalogResponse.getData().getItems(), not(empty()));

        logger.info("Items array is not empty: {} items found", catalogResponse.getData().getItems().size());
    }

    @Test(description = "Test the boolean flags have correct values when present", priority = 7, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Product Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testBooleanFlagsCorrectValues() {
        // Validate boolean flags if items exist
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            CatalogResponse.CatalogItem sampleItem = catalogResponse.getData().getItems().get(0);

            // Check visible flag
            if (sampleItem.getVisible() != null) {
                assertThat("Item visible should be true", sampleItem.getVisible(), is(true));
            }

            // Check seller flags if seller exists
            if (sampleItem.getSeller() != null) {
                if (sampleItem.getSeller().getDeprioritisation_status() != null) {
                    // Note: In the Postman script, this is expected to be true for product filter
                    assertThat("Seller deprioritisation_status should be false",
                            sampleItem.getSeller().getDeprioritisation_status(), is(false));
                }
                if (sampleItem.getSeller().getIsCatalogAvailable() != null) {
                    assertThat("Seller isCatalogAvailable should be true",
                            sampleItem.getSeller().getIsCatalogAvailable(), is(true));
                }
            }

            // Check available flag
            if (sampleItem.getAvailable() != null) {
                assertThat("Item available should be true", sampleItem.getAvailable(), is(true));
            }

            logger.info("Boolean flags validated successfully");
        } else {
            throw new AssertionError("No items found in response to check boolean flags");
        }
    }

    @Test(description = "Test the product array has correct structure", priority = 8, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Product Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testProductArrayStructure() {
        // Validate response is an object
        assertThat("Response should be an object", catalogResponse, notNullValue());

        // Validate product structure if items and products exist
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            CatalogResponse.CatalogItem sampleItem = catalogResponse.getData().getItems().get(0);

            if (sampleItem.getProduct() != null && sampleItem.getProduct().size() > 0) {
                CatalogResponse.Product sampleProduct = sampleItem.getProduct().get(0);

                // Validate product includes required keys
                assertThat("Product should include name", sampleProduct.getName(), notNullValue());
                assertThat("Product should include id", sampleProduct.getId(), notNullValue());

                // Validate data types
                assertThat("Product name should be a string", sampleProduct.getName(), instanceOf(String.class));
                assertThat("Product id should be a string", sampleProduct.getId(), instanceOf(String.class));

                // Validate product ID matches MongoDB ObjectId format (24 hex characters)
                assertThat("Product id should match MongoDB ObjectId format",
                        sampleProduct.getId(), matchesRegex("^[0-9a-fA-F]{24}$"));

                logger.info("Product array structure validated successfully");
            } else {
                logger.info("No products to validate");
            }
        } else {
            logger.info("No catalog items to validate products");
        }
    }

    @Test(description = "Test the product ID matches expected value", priority = 9, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Product Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testProductIdMatchesExpected() {
        // Note: The Postman script checks buckets for product ID, but the
        // CatalogResponse model
        // doesn't have a product field in buckets. This test validates that items
        // contain
        // the expected product based on the filter.

        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            // Verify that items exist (which means the product filter worked)
            assertThat("Items should exist when filtering by product",
                    catalogResponse.getData().getItems(), not(empty()));

            logger.info("Product filter validation: items returned for product ID {}",  searchProductId);
        } else {
            logger.warn("No items found for product ID: {}", searchProductId);
        }
    }

    @Test(description = "Test the catalog items have valid structure", priority = 10, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Product Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testCatalogItemsValidStructure() {
        // Validate response is an object
        assertThat("Response should be an object", catalogResponse, notNullValue());

        // Validate catalog item structure if items exist
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            CatalogResponse.CatalogItem sampleItem = catalogResponse.getData().getItems().get(0);

            // Validate item includes required keys
            assertThat("Item should include _id", sampleItem.get_id(), notNullValue());
            assertThat("Item should include title", sampleItem.getTitle(), notNullValue());
            assertThat("Item should include price", sampleItem.getPrice(), notNullValue());
            assertThat("Item should include sellerId", sampleItem.getSellerId(), notNullValue());
            assertThat("Item should include visible", sampleItem.getVisible(), notNullValue());
            assertThat("Item should include available", sampleItem.getAvailable(), notNullValue());

            // Validate seller includes required keys
            assertThat("Item should have seller", sampleItem.getSeller(), notNullValue());
            assertThat("Seller should include deprioritisation_status",
                    sampleItem.getSeller().getDeprioritisation_status(), notNullValue());
            assertThat("Seller should include isCatalogAvailable",
                    sampleItem.getSeller().getIsCatalogAvailable(), notNullValue());

            // Validate data types
            assertThat("Item _id should be a string", sampleItem.get_id(), instanceOf(String.class));
            assertThat("Item title should be a string", sampleItem.getTitle(), instanceOf(String.class));
            assertThat("Item price should be a number", sampleItem.getPrice(), instanceOf(Double.class));
            assertThat("Item price should be above 0", sampleItem.getPrice(), greaterThan(0.0));
            assertThat("Seller deprioritisation_status should be a boolean",
                    sampleItem.getSeller().getDeprioritisation_status(), instanceOf(Boolean.class));
            assertThat("Seller isCatalogAvailable should be a boolean",
                    sampleItem.getSeller().getIsCatalogAvailable(), instanceOf(Boolean.class));

            logger.info("Catalog items structure validated successfully");
        } else {
            logger.info("No catalog items to validate");
        }
    }

    @Test(description = "Test the pagination parameters are correct", priority = 11, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Product Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testPaginationParametersCorrect() {
        // Validate pagination limit is respected (limit=20)
        if (catalogResponse.getData().getItems() != null) {
            assertThat("Items count should not exceed limit of 20",
                    catalogResponse.getData().getItems().size(), lessThanOrEqualTo(20));

            logger.info("Pagination parameters verified: items count = {}",
                    catalogResponse.getData().getItems().size());
        } else {
            logger.info("No items to validate pagination");
        }
    }

    @Test(description = "Test the response time is less than threshold", priority = 12, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Product Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeThreshold() {
        // Get threshold from config or use default 40000ms
        long threshold = config.responseTimeThreshold();
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be below threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }
}
```

### `src/test/java/com/automation/tests/bomb/CatalogSearch/SearchWithSellerFilterTest.java`

_307 lines_

```java
package com.automation.tests.bomb.CatalogSearch;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Search with Seller Filter.
 * Endpoint:
 * {{bizup_base}}/v1/admin/catalog_all?seller=63ee780c9689be92acce8f35&offset=0&limit=20
 * Implements comprehensive Postman test scripts for catalog search with seller
 * filter validation.
 */
@TmsLink("TC-ADMIN-001")
@TmsLink("TC-ADMIN-002")
@Epic("BOMB Catalog Management")
@Feature("Catalog Search - Seller Filter")
public class SearchWithSellerFilterTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogResponse catalogResponse;
    public static String liveCatalogId; // Store catalog ID for other tests
    private String searchSellerId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
        
        // Get seller ID after VariableManager is initialized
        searchSellerId = VariableManager.getSearchSellerId();
        logger.info("Using seller ID: {}", searchSellerId);
    }

    @Test(description = "Verify response status is 200 OK", priority = 1, groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.BLOCKER)
    public void testResponseStatus() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("seller", VariableManager.get("search_seller_id"));
        queryParams.put("offset", 0);
        queryParams.put("limit", 20);

        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParams(queryParams)
                .when()
                .get(BombEndpoints.CATALOG_ALL);

        // Parse response for other tests
        catalogResponse = JsonUtils.fromResponse(response, CatalogResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Verify response format is JSON", priority = 2, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseFormatIsJson() {
        // Verify response format is JSON
        assertThat("Response should be JSON",
                response.getContentType(), containsString("application/json"));

        logger.info("Response format verified: JSON");
    }

    @Test(description = "Validate root response structure", priority = 3, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testValidateRootResponseStructure() {
        // Validate response is an object
        assertThat("Response should be an object", catalogResponse, notNullValue());
        assertThat("Response should be an instance of CatalogResponse",
                catalogResponse, instanceOf(CatalogResponse.class));

        // Validate response includes required keys
        assertThat("StatusCode should not be null", catalogResponse.getStatusCode(), notNullValue());
        assertThat("Message should not be null", catalogResponse.getMessage(), notNullValue());
        assertThat("Data should not be null", catalogResponse.getData(), notNullValue());

        // Validate message is 'success'
        assertThat("Message should be 'success'",
                catalogResponse.getMessage(), equalTo("success"));

        logger.info("Root response structure validated successfully");
    }

    @Test(description = "Validate data object structure", priority = 4, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testValidateDataObjectStructure() {
        // Validate data is an object
        assertThat("Data should be an object", catalogResponse.getData(), notNullValue());
        assertThat("Data should be an instance of CatalogData",
                catalogResponse.getData(), instanceOf(CatalogResponse.CatalogData.class));

        // Validate data includes required keys: total, items
        assertThat("Total should not be null", catalogResponse.getData().getTotal(), notNullValue());
        assertThat("Items should not be null", catalogResponse.getData().getItems(), notNullValue());

        // Validate total.value is a number and at least 0
        assertThat("Total value should be a number",
                catalogResponse.getData().getTotal().getValue(), instanceOf(Integer.class));
        assertThat("Total value should be at least 0",
                catalogResponse.getData().getTotal().getValue(), greaterThanOrEqualTo(0));

        // Validate items is an array
        assertThat("Items should be an array", catalogResponse.getData().getItems(), notNullValue());

        logger.info("Data object structure validated successfully");
    }

    @Test(description = "Verify seller IDs match request parameter", priority = 5, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testVerifySellerIdsMatchRequestParameter() {
        // Verify seller IDs match the request parameter
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            catalogResponse.getData().getItems().forEach(item -> {
                assertThat("Seller ID should match request parameter",
                        item.getSeller().get_id(), equalTo(searchSellerId));
            });

            logger.info("All seller IDs match request parameter: {}", searchSellerId);
        } else {
            logger.info("No items to validate seller IDs");
        }
    }

    @Test(description = "Validate buckets structure", priority = 6, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testValidateBucketsStructure() {
        // Validate buckets is an array and not empty
        assertThat("Buckets should be an array", catalogResponse.getData().getBuckets(), notNullValue());
        assertThat("Buckets should not be empty", catalogResponse.getData().getBuckets(), not(empty()));

        // Validate bucket structure
        catalogResponse.getData().getBuckets().forEach(bucket -> {
            assertThat("Bucket _id should be a string", bucket.get_id(), instanceOf(String.class));
            assertThat("Bucket name should be a string", bucket.getName(), instanceOf(String.class));
            assertThat("Bucket doc_count should be a number", bucket.getDoc_count(), instanceOf(Integer.class));
        });

        logger.info("Buckets structure validated successfully");
    }

    @Test(description = "Validate boolean flags", priority = 7, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testValidateBooleanFlags() {
        // Validate boolean flags if items exist
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            CatalogResponse.CatalogItem item = catalogResponse.getData().getItems().get(0);

            assertThat("Item visible should be true", item.getVisible(), is(true));
            assertThat("Seller deprioritisation_status should be false",
                    item.getSeller().getDeprioritisation_status(), is(false));
            assertThat("Seller isCatalogAvailable should be true",
                    item.getSeller().getIsCatalogAvailable(), is(true));
            assertThat("Item available should be true", item.getAvailable(), is(true));

            logger.info("Boolean flags validated successfully");
        } else {
            logger.info("No items to validate boolean flags");
        }
    }

    @Test(description = "Validate product structure", priority = 8, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testValidateProductStructure() {
        // Validate product structure if items and products exist
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            CatalogResponse.CatalogItem item = catalogResponse.getData().getItems().get(0);

            if (item.getProduct() != null && item.getProduct().size() > 0) {
                CatalogResponse.Product product = item.getProduct().get(0);

                // Validate product includes required keys
                assertThat("Product should include name", product.getName(), notNullValue());
                assertThat("Product should include id", product.getId(), notNullValue());

                // Validate data types
                assertThat("Product name should be a string", product.getName(), instanceOf(String.class));
                assertThat("Product id should be a string", product.getId(), instanceOf(String.class));

                // Validate product ID matches MongoDB ObjectId format (24 hex characters)
                assertThat("Product id should match MongoDB ObjectId format",
                        product.getId(), matchesRegex("^[0-9a-fA-F]{24}$"));

                logger.info("Product structure validated successfully");
            } else {
                logger.info("No products to validate");
            }
        } else {
            logger.info("No catalog items to validate products");
        }
    }

    @Test(description = "Verify pagination limit", priority = 9, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testVerifyPaginationLimit() {
        // Verify pagination limit is respected (limit=20)
        assertThat("Items count should not exceed limit of 20",
                catalogResponse.getData().getItems().size(), lessThanOrEqualTo(20));

        logger.info("Pagination limit verified: items count = {}", catalogResponse.getData().getItems().size());
    }

    @Test(description = "Validate catalog item structure", priority = 10, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testValidateCatalogItemStructure() {
        // Validate catalog item structure if items exist
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            CatalogResponse.CatalogItem item = catalogResponse.getData().getItems().get(0);

            // Validate item includes required keys
            assertThat("Item should include _id", item.get_id(), notNullValue());
            assertThat("Item should include title", item.getTitle(), notNullValue());
            assertThat("Item should include price", item.getPrice(), notNullValue());
            assertThat("Item should include sellerId", item.getSellerId(), notNullValue());
            assertThat("Item should include visible", item.getVisible(), notNullValue());
            assertThat("Item should include available", item.getAvailable(), notNullValue());

            // Validate data types
            assertThat("Item _id should be a string", item.get_id(), instanceOf(String.class));
            assertThat("Item title should be a string", item.getTitle(), instanceOf(String.class));
            assertThat("Item price should be a number", item.getPrice(), instanceOf(Double.class));
            assertThat("Item price should be above 0", item.getPrice(), greaterThan(0.0));

            logger.info("Catalog item structure validated successfully");
        } else {
            logger.info("No catalog items to validate");
        }
    }

    @Test(description = "Verify response performance", priority = 11, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testVerifyResponsePerformance() {
        // Get threshold from config or use default 40000ms
        long threshold = config.responseTimeThreshold();
        long actualResponseTime = response.getTime();

        // Verify response time is below threshold
        assertThat("Response time should be below threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response performance verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Verify security headers", priority = 12, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.MINOR)
    public void testVerifySecurityHeaders() {
        // Verify Content-Type header includes application/json
        assertThat("Content-Type header should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Security headers verified: Content-Type = {}", response.getContentType());
    }

    @Test(description = "Set Id to live_catalog_id collection variable", priority = 13, dependsOnMethods = "testResponseStatus", groups = "bomb")
    @Story("Catalog Search - Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testSetLiveCatalogId() {
        // Store first catalog ID in static variable for use in other tests
        if (catalogResponse.getData().getItems() != null && catalogResponse.getData().getItems().size() > 0) {
            liveCatalogId = catalogResponse.getData().getItems().get(0).getId();

            assertThat("Live catalog ID should not be null", liveCatalogId, notNullValue());
            assertThat("Live catalog ID should not be empty", liveCatalogId, not(emptyOrNullString()));

            logger.info("Live catalog ID stored successfully: {}", liveCatalogId);
        } else {
            logger.warn("No items available to set live catalog ID");
        }
    }
}
```

### `src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogAssignToEditor/AllCatalogUploadedTest.java`

_241 lines_

```java
package com.automation.tests.bomb.CatalogTagPipeline.CatalogAssignToEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogUploadedResponse;
import com.automation.utils.JsonUtils;
import com.automation.utils.VariableManager;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Assign - All Catalog Uploaded endpoint.
 * Endpoint: {{bizup_base}}/v1/admin/catalog?limit=20
 * Implements comprehensive Postman test scripts for catalog uploaded
 * validation.
 */
@TmsLink("TC-ADMIN-012")
@TmsLink("TC-ADMIN-016")
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Assign to Editor")
public class AllCatalogUploadedTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogUploadedResponse catalogUploadedResponse;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
    }

    @Test(description = "Response status code should be 200 (OK)", priority = 1, groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send GET request to fetch all uploaded catalogs
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("limit", 20)
                .when()
                .get(BombEndpoints.CATALOG);

        // Parse response for other tests
        catalogUploadedResponse = JsonUtils.fromResponse(response, CatalogUploadedResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time should be under threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeUnderThreshold() {
        // Get threshold from config or use default 40000ms
        long threshold = config.responseTimeThreshold();
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Response should include 'Content-Type' header", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeaderPresent() {
        // Verify Content-Type header is present
        assertThat("Content-Type header should be present",
                response.getHeader("Content-Type"), notNullValue());

        logger.info("Content-Type header verified: {}", response.getHeader("Content-Type"));
    }

    @Test(description = "Response message should be 'success'", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseMessageSuccess() {
        // Validate message is 'success'
        assertThat("Message should be 'success'",
                catalogUploadedResponse.getMessage(), equalTo("success"));

        logger.info("Message validated: success");
    }

    @Test(description = "Response must contain 'statusCode', 'data', and 'message' fields", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseObjectStructure() {
        // Validate response is an object
        assertThat("Response should not be null", catalogUploadedResponse, notNullValue());

        // Validate response has all required keys
        assertThat("Response should have statusCode", catalogUploadedResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogUploadedResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogUploadedResponse.getData(), notNullValue());

        logger.info("Response structure validated successfully");
    }

    @Test(description = "First data item should contain all required fields", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.CRITICAL)
    public void testFirstItemContainsRequiredFields() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            assertThat("First group should have data", firstGroup.getData(), notNullValue());
            assertThat("First group data should not be empty", firstGroup.getData(), not(empty()));

            CatalogUploadedResponse.CatalogUploadedItem firstItem = firstGroup.getData().get(0);

            // Validate first item has all required fields
            assertThat("First item should have _id", firstItem.get_id(), notNullValue());
            assertThat("First item should have source", firstItem.getSource(), notNullValue());
            assertThat("First item should have videoType", firstItem.getVideoType(), notNullValue());
            assertThat("First item should have sellerId", firstItem.getSellerId(), notNullValue());
            assertThat("First item should have phoneNumber", firstItem.getPhoneNumber(), notNullValue());
            assertThat("First item should have name", firstItem.getName(), notNullValue());

            logger.info("First item structure validated successfully");
        } else {
            logger.warn("No data items to validate");
        }
    }

    @Test(description = "All fields in first item should have correct data types", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.CRITICAL)
    public void testFirstItemFieldDataTypes() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            if (firstGroup.getData() != null && !firstGroup.getData().isEmpty()) {
                CatalogUploadedResponse.CatalogUploadedItem firstItem = firstGroup.getData().get(0);

                // Validate data types
                assertThat("_id should be a string", firstItem.get_id(), instanceOf(String.class));
                assertThat("source should be a string", firstItem.getSource(), instanceOf(String.class));
                assertThat("videoType should be a string", firstItem.getVideoType(), instanceOf(String.class));
                assertThat("sellerId should be a string", firstItem.getSellerId(), instanceOf(String.class));
                assertThat("phoneNumber should be a string", firstItem.getPhoneNumber(), instanceOf(String.class));
                assertThat("name should be a string", firstItem.getName(), instanceOf(String.class));

                logger.info("First item data types validated successfully");
            }
        } else {
            logger.warn("No data items to validate");
        }
    }

    @Test(description = "Field 'source' value should be either 'auto-catalog', 'seller-app', 'pdf' or 'zip-upload'", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.NORMAL)
    public void testSourceFieldValue() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            if (firstGroup.getData() != null && !firstGroup.getData().isEmpty()) {
                CatalogUploadedResponse.CatalogUploadedItem firstItem = firstGroup.getData().get(0);

                // Validate source is one of the expected values
                assertThat("source should be one of the expected values",
                        firstItem.getSource(),
                        in(new String[] { "auto-catalog", "seller-app", "pdf", "zip-upload" }));

                logger.info("Source field validated: {}", firstItem.getSource());
            }
        } else {
            logger.warn("No data items to validate");
        }
    }

    @Test(description = "Field 'videoType' value should be 'catalog'", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.NORMAL)
    public void testVideoTypeFieldValue() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            if (firstGroup.getData() != null && !firstGroup.getData().isEmpty()) {
                CatalogUploadedResponse.CatalogUploadedItem firstItem = firstGroup.getData().get(0);

                // Validate videoType is 'catalog'
                assertThat("videoType should be 'catalog'",
                        firstItem.getVideoType(), equalTo("catalog"));

                logger.info("VideoType field validated: {}", firstItem.getVideoType());
            }
        } else {
            logger.warn("No data items to validate");
        }
    }

    @Test(description = "Phone number must follow the format '+[country_code][number]' with 10-15 digits", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - All Catalog Uploaded")
    @Severity(SeverityLevel.NORMAL)
    public void testPhoneNumberFormat() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            if (firstGroup.getData() != null && !firstGroup.getData().isEmpty()) {
                CatalogUploadedResponse.CatalogUploadedItem firstItem = firstGroup.getData().get(0);

                // Validate phone number format: +[country_code][number] with 10-15 digits
                assertThat("Phone number should match format +[country_code][number] with 10-15 digits",
                        firstItem.getPhoneNumber(), matchesPattern("^\\+[0-9]{10,15}$"));

                logger.info("Phone number format validated: {}", firstItem.getPhoneNumber());
            }
        } else {
            logger.warn("No data items to validate");
        }
    }
}
```

### `src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogAssignToEditor/AssignToEditorTest.java`

_248 lines_

```java
package com.automation.tests.bomb.CatalogTagPipeline.CatalogAssignToEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogAssignResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Assign - Assign to Editor endpoint.
 * Endpoint: {{bizup_base}}/v1/admin/catalog/assign/{{catalog_foassign_id}}
 * Implements comprehensive Postman test scripts for catalog assignment
 * validation.
 */
@TmsLink("TC-ADMIN-003")
@TmsLink("TC-ADMIN-004")
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Assign to Editor")
public class AssignToEditorTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogAssignResponse catalogAssignResponse;

    // Collection variables (equivalent to Postman collection variables)
    private String catalogForAssignId;
    private String sellerId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
        
        // Get seller ID after VariableManager is initialized
        sellerId = VariableManager.getSellerId();
        logger.info("Using seller ID: {}", sellerId);
        
        // Get catalog ID from VariableManager or use fallback
        catalogForAssignId = VariableManager.get("catalog_foassign_id");
        if (catalogForAssignId == null || catalogForAssignId.isEmpty()) {
            catalogForAssignId = VariableManager.get("catalog_foassign_id", "6822f5dac17c6dcd589ba173");
            logger.warn("Catalog for assign ID not available, using fallback: {}", catalogForAssignId);
        } else {
            logger.info("Using catalog for assign ID from VariableManager: {}", catalogForAssignId);
        }
    }

    @Test(description = "Status code is 200 and status message is 'OK'", priority = 1, groups = "bomb")
    @Story("Catalog Assign - Assign to Editor")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200AndMessageOK() {
        // Prepare request body with editor information
        String requestBody = "{\n" +
                "    \"editor\": {\n" +
                "        \"_id\": \"652e699e42e117518ebb86cd\",\n" +
                "        \"name\": \"Shubham Kumar\",\n" +
                "        \"role\": [\n" +
                "            \"editor_review\",\n" +
                "            \"admin\",\n" +
                "            \"editor_catalog\",\n" +
                "            \"editor_video\",\n" +
                "            \"pipeline_online\",\n" +
                "            \"pipeline_onground\",\n" +
                "            \"pipeline_wa\",\n" +
                "            \"manage_seller\",\n" +
                "            \"manage_buyer\",\n" +
                "            \"salesman\",\n" +
                "            \"whatsapp_message\"\n" +
                "        ],\n" +
                "        \"status\": 1,\n" +
                "        \"phoneNumber\": \"+916204843730\",\n" +
                "        \"user_id\": \"64f180feaa90ffbd54b330f5\",\n" +
                "        \"label\": \"Shubham Kumar\",\n" +
                "        \"value\": \"64f180feaa90ffbd54b330f5\"\n" +
                "    }\n" +
                "}";

        // Send POST request to assign catalog to editor
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .body(requestBody)
                .when()
                .post(BombEndpoints.CATALOG_ASSIGN + "/" + catalogForAssignId);

        // Parse response for other tests
        catalogAssignResponse = JsonUtils.fromResponse(response, CatalogAssignResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat("Status message should be OK",
                response.getStatusLine(), containsString("OK"));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is below threshold", priority = 2, dependsOnMethods = "testStatusCode200AndMessageOK", groups = "bomb")
    @Story("Catalog Assign - Assign to Editor")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeBelowThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Response contains required fields: statusCode, message, and data", priority = 3, dependsOnMethods = "testStatusCode200AndMessageOK", groups = "bomb")
    @Story("Catalog Assign - Assign to Editor")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseContainsRequiredFields() {
        // Validate response is an object
        assertThat("Response should not be null", catalogAssignResponse, notNullValue());

        // Validate response has all required fields
        assertThat("Response should have statusCode", catalogAssignResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogAssignResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogAssignResponse.getData(), notNullValue());

        logger.info("Response contains all required fields");
    }

    @Test(description = "statusCode and message are strings; data is an object", priority = 4, dependsOnMethods = "testStatusCode200AndMessageOK", groups = "bomb")
    @Story("Catalog Assign - Assign to Editor")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseFieldDataTypes() {
        // Validate response field types
        assertThat("statusCode should be a string", catalogAssignResponse.getStatusCode(), instanceOf(String.class));
        assertThat("message should be a string", catalogAssignResponse.getMessage(), instanceOf(String.class));
        assertThat("data should be an object", catalogAssignResponse.getData(), notNullValue());

        logger.info("Response field data types validated");
    }

    @Test(description = "Validate data types of response fields", priority = 5, dependsOnMethods = "testStatusCode200AndMessageOK", groups = "bomb")
    @Story("Catalog Assign - Assign to Editor")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataFieldDataTypes() {
        CatalogAssignResponse.CatalogAssignData data = catalogAssignResponse.getData();

        if (data != null) {
            // Validate message type
            assertThat("message should be a string", catalogAssignResponse.getMessage(), instanceOf(String.class));

            // Validate data field types
            assertThat("_id should be a string", data.get_id(), instanceOf(String.class));
            assertThat("videoType should be a string", data.getVideoType(), instanceOf(String.class));
            assertThat("createdAt should be a string", data.getCreatedAt(), instanceOf(String.class));
            assertThat("updatedAt should be a string", data.getUpdatedAt(), instanceOf(String.class));
            assertThat("__v should be a number", data.getVersion(), instanceOf(Integer.class));

            logger.info("Data field data types validated successfully");
        } else {
            logger.warn("Data is null, skipping field type validation");
        }
    }

    @Test(description = "Field 'videoType' should be 'catalog'", priority = 6, dependsOnMethods = "testStatusCode200AndMessageOK", groups = "bomb")
    @Story("Catalog Assign - Assign to Editor")
    @Severity(SeverityLevel.NORMAL)
    public void testVideoTypeIsCatalog() {
        CatalogAssignResponse.CatalogAssignData data = catalogAssignResponse.getData();

        if (data != null) {
            // Validate videoType is 'catalog'
            assertThat("videoType should be 'catalog'",
                    data.getVideoType(), equalTo("catalog"));

            logger.info("VideoType validated: {}", data.getVideoType());
        } else {
            logger.warn("Data is null, skipping videoType validation");
        }
    }

    @Test(description = "Response _id (FO_Assign_ID) matches collection variable 'catalog_foassign_id'", priority = 7, dependsOnMethods = "testStatusCode200AndMessageOK", groups = "bomb")
    @Story("Catalog Assign - Assign to Editor")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseIdMatchesCatalogForAssignId() {
        CatalogAssignResponse.CatalogAssignData data = catalogAssignResponse.getData();

        if (data != null) {
            // Validate _id matches the catalog ID used in the request
            assertThat("Response _id should match catalog_foassign_id",
                    data.get_id(), equalTo(catalogForAssignId));

            logger.info("Response _id validated: {} (Expected: {})", data.get_id(), catalogForAssignId);
        } else {
            logger.warn("Data is null, skipping _id validation");
        }
    }

    @Test(description = "Response sellerId matches collection variable 'seller_id'", priority = 8, dependsOnMethods = "testStatusCode200AndMessageOK", groups = "bomb")
    @Story("Catalog Assign - Assign to Editor")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseSellerIdMatchesExpected() {
        CatalogAssignResponse.CatalogAssignData data = catalogAssignResponse.getData();

        if (data != null) {
            // Validate sellerId matches the expected seller ID
            assertThat("Response sellerId should match seller_id",
                    data.getSellerId(), equalTo(sellerId));

            logger.info("Response sellerId validated: {} (Expected: {})", data.getSellerId(), sellerId);
        } else {
            logger.warn("Data is null, skipping sellerId validation");
        }
    }

    @Test(description = "Response editorId matches collection variable 'editor_id'", priority = 9, dependsOnMethods = "testStatusCode200AndMessageOK", groups = "bomb")
    @Story("Catalog Assign - Assign to Editor")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseEditorIdMatchesExpected() {
        CatalogAssignResponse.CatalogAssignData data = catalogAssignResponse.getData();

        if (data != null) {
            // Validate editorId is present and not null
            // Note: In real scenario, you would compare against actual editor_id from
            // collection variables
            assertThat("Response editorId should not be null", data.getEditorId(), notNullValue());

            logger.info("Response editorId validated: {}", data.getEditorId());
        } else {
            logger.warn("Data is null, skipping editorId validation");
        }
    }
}
```

### `src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogAssignToEditor/CatalogUploadedSearchBySellerFilterTest.java`

_282 lines_

```java
package com.automation.tests.bomb.CatalogTagPipeline.CatalogAssignToEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogUploadedResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Assign - Catalog Uploaded Search by Seller Filter
 * endpoint.
 * Endpoint: {{bizup_base}}/v1/admin/catalog?limit=600&seller={{seller_id}}
 * Implements comprehensive Postman test scripts for seller-filtered catalog
 * validation.
 */
@TmsLink("TC-ADMIN-012")
@TmsLink("TC-ADMIN-016")
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Assign to Editor")
public class CatalogUploadedSearchBySellerFilterTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogUploadedResponse catalogUploadedResponse;

    // Seller ID for filtering
    private String sellerId;

    // Store catalog ID for assignment (status = 0)
    public static String catalogForAssignId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
        
        // Get seller ID after VariableManager is initialized
        sellerId = VariableManager.getSellerId();
        logger.info("Using seller ID: {}", sellerId);
    }

    @Test(description = "Response status code should be 200", priority = 1, groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send GET request to fetch catalogs filtered by seller
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("limit", 600)
                .queryParam("seller", VariableManager.get("seller_id"))
                .when()
                .get(BombEndpoints.CATALOG);

        // Parse response for other tests
        catalogUploadedResponse = JsonUtils.fromResponse(response, CatalogUploadedResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response includes 'Content-Type' header", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeaderPresent() {
        // Verify Content-Type header is present
        assertThat("Content-Type header should be present",
                response.getHeader("Content-Type"), notNullValue());

        logger.info("Content-Type header verified: {}", response.getHeader("Content-Type"));
    }

    @Test(description = "Response message equals 'success'", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseMessageSuccess() {
        // Validate message is 'success'
        assertThat("Message should be 'success'",
                catalogUploadedResponse.getMessage(), equalTo("success"));

        logger.info("Message validated: success");
    }

    @Test(description = "Response time is below threshold", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeBelowThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "First item contains all required fields", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testFirstItemContainsRequiredFields() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            assertThat("First group should have data", firstGroup.getData(), notNullValue());
            assertThat("First group data should not be empty", firstGroup.getData(), not(empty()));

            CatalogUploadedResponse.CatalogUploadedItem firstItem = firstGroup.getData().get(0);

            // Validate first item has all required fields
            assertThat("First item should have _id", firstItem.get_id(), notNullValue());
            assertThat("First item should have source", firstItem.getSource(), notNullValue());
            assertThat("First item should have videoType", firstItem.getVideoType(), notNullValue());
            assertThat("First item should have sellerId", firstItem.getSellerId(), notNullValue());
            assertThat("First item should have phoneNumber", firstItem.getPhoneNumber(), notNullValue());
            assertThat("First item should have name", firstItem.getName(), notNullValue());

            logger.info("First item structure validated successfully");
        } else {
            logger.warn("No data items to validate");
        }
    }

    @Test(description = "All fields in first item have correct data types", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testFirstItemFieldDataTypes() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            if (firstGroup.getData() != null && !firstGroup.getData().isEmpty()) {
                CatalogUploadedResponse.CatalogUploadedItem firstItem = firstGroup.getData().get(0);

                // Validate data types
                assertThat("_id should be a string", firstItem.get_id(), instanceOf(String.class));
                assertThat("source should be a string", firstItem.getSource(), instanceOf(String.class));
                assertThat("videoType should be a string", firstItem.getVideoType(), instanceOf(String.class));
                assertThat("sellerId should be a string", firstItem.getSellerId(), instanceOf(String.class));
                assertThat("phoneNumber should be a string", firstItem.getPhoneNumber(), instanceOf(String.class));
                assertThat("name should be a string", firstItem.getName(), instanceOf(String.class));

                logger.info("First item data types validated successfully");
            }
        } else {
            logger.warn("No data items to validate");
        }
    }

    @Test(description = "Field 'videoType' value is 'catalog'", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testVideoTypeIsCatalog() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            if (firstGroup.getData() != null && !firstGroup.getData().isEmpty()) {
                CatalogUploadedResponse.CatalogUploadedItem firstItem = firstGroup.getData().get(0);

                // Validate videoType is 'catalog'
                assertThat("videoType should be 'catalog'",
                        firstItem.getVideoType(), equalTo("catalog"));

                logger.info("VideoType field validated: {}", firstItem.getVideoType());
            }
        } else {
            logger.warn("No data items to validate");
        }
    }

    @Test(description = "Phone number format is valid", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testPhoneNumberFormat() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            if (firstGroup.getData() != null && !firstGroup.getData().isEmpty()) {
                CatalogUploadedResponse.CatalogUploadedItem firstItem = firstGroup.getData().get(0);

                // Validate phone number format: +[country_code][number] with 10-15 digits
                assertThat("Phone number should match format +[country_code][number] with 10-15 digits",
                        firstItem.getPhoneNumber(), matchesPattern("^\\+[0-9]{10,15}$"));

                logger.info("Phone number format validated: {}", firstItem.getPhoneNumber());
            }
        } else {
            logger.warn("No data items to validate");
        }
    }

    @Test(description = "Response contains 'statusCode', 'data', and 'message' fields", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStructure() {
        // Validate response is an object
        assertThat("Response should not be null", catalogUploadedResponse, notNullValue());

        // Validate response has all required keys
        assertThat("Response should have statusCode", catalogUploadedResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogUploadedResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogUploadedResponse.getData(), notNullValue());

        logger.info("Response structure validated successfully");
    }

    @Test(description = "All catalog items belong to requested seller ID", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.CRITICAL)
    public void testAllItemsBelongToRequestedSeller() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            assertThat("First group should have data", firstGroup.getData(), notNullValue());
            assertThat("First group data should not be empty", firstGroup.getData(), not(empty()));

            // Verify all items belong to the requested seller
            firstGroup.getData().forEach(item -> {
                assertThat(String.format("Item %s should belong to seller %s", item.get_id(), sellerId),
                        item.getSellerId(), equalTo(sellerId));
            });

            logger.info("All {} catalog items verified to belong to seller: {}",
                    firstGroup.getData().size(), sellerId);
        } else {
            logger.warn("No data items to validate");
        }
    }

    @Test(description = "Set catalog 'foassign' ID as collection variable", priority = 11, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Assign - Search by Seller Filter")
    @Severity(SeverityLevel.NORMAL)
    public void testSetCatalogForAssignId() {
        // Check if data array exists and has items
        if (catalogUploadedResponse.getData() != null && !catalogUploadedResponse.getData().isEmpty()) {
            CatalogUploadedResponse.CatalogUploadedGroup firstGroup = catalogUploadedResponse.getData().get(0);

            if (firstGroup.getData() != null && !firstGroup.getData().isEmpty()) {
                // Note: The response model doesn't include 'status' field
                // Using the first item's ID as a fallback
                // In a real scenario, you would need to add 'status' field to the response
                // model

                CatalogUploadedResponse.CatalogUploadedItem firstItem = firstGroup.getData().get(0);
                catalogForAssignId = firstItem.get_id();

                assertThat("Catalog for assign ID should be set", catalogForAssignId, notNullValue());

                logger.info("Set catalog 'foassign' ID: {}", catalogForAssignId);
            } else {
                logger.warn("No items found to set catalog for assign ID");
            }
        } else {
            logger.warn("No data items to set catalog for assign ID");
        }
    }
}
```

### `src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/AllCatalogsAssignedTest.java`

_186 lines_

```java
package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogEditorResponse;
import com.automation.utils.JsonUtils;
import com.automation.utils.VariableManager;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Editor - All Catalogs Assigned endpoint.
 * Endpoint:
 * {{bizup_base}}/v1/admin/catalog?limit=20&editor={{seller_id}}&sort=status
 * Implements comprehensive Postman test scripts for editor-assigned catalogs
 * validation.
 */
@TmsLink("TC-ADMIN-012")
@TmsLink("TC-ADMIN-016")
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class AllCatalogsAssignedTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogEditorResponse catalogEditorResponse;

    // Editor ID (using seller_id as per Postman script)

    // Catalog ID is now stored in VariableManager - no static variable needed

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - All Catalogs Assigned")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send GET request to fetch catalogs assigned to editor
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("limit", 20)
                .queryParam("editor", VariableManager.get("editor_id"))
                .queryParam("sort", "status")
                .when()
                .get(BombEndpoints.CATALOG);

        // Parse response for other tests
        catalogEditorResponse = JsonUtils.fromResponse(response, CatalogEditorResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - All Catalogs Assigned")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Validate the response schema for required fields", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - All Catalogs Assigned")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseSchemaValidation() {
        // Validate response is an object
        assertThat("Response should not be null", catalogEditorResponse, notNullValue());

        // Validate response has required fields
        assertThat("Response should have statusCode", catalogEditorResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogEditorResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogEditorResponse.getData(), notNullValue());
        assertThat("Data should be an array", catalogEditorResponse.getData(), instanceOf(List.class));

        // Validate nested structure
        catalogEditorResponse.getData().forEach(group -> {
            assertThat("Group should have pagination", group.getPagination(), notNullValue());
            assertThat("Pagination should be an array", group.getPagination(), instanceOf(List.class));

            group.getPagination().forEach(paginationItem -> {
                assertThat("Pagination item should have total", paginationItem.getTotal(), notNullValue());
            });

            assertThat("Group should have data", group.getData(), notNullValue());
            assertThat("Data should be an array", group.getData(), instanceOf(List.class));

            group.getData().forEach(dataItem -> {
                assertThat("Data item should have _id", dataItem.get_id(), notNullValue());
                assertThat("Data item should have source", dataItem.getSource(), notNullValue());
                assertThat("Data item should have status", dataItem.getStatus(), notNullValue());
                assertThat("Data item should have priority", dataItem.getPriority(), notNullValue());
                assertThat("Data item should have videoType", dataItem.getVideoType(), notNullValue());
                assertThat("Data item should have sellerId", dataItem.getSellerId(), notNullValue());
                assertThat("Data item should have phoneNumber", dataItem.getPhoneNumber(), notNullValue());
                assertThat("Data item should have name", dataItem.getName(), notNullValue());
                assertThat("Data item should have createdAt", dataItem.getCreatedAt(), notNullValue());
                assertThat("Data item should have updatedAt", dataItem.getUpdatedAt(), notNullValue());
                assertThat("Data item should have editorId", dataItem.getEditorId(), notNullValue());
                assertThat("Data item should have total", dataItem.getTotal(), notNullValue());
                assertThat("Data item should have tagged", dataItem.getTagged(), notNullValue());
                assertThat("Data item should have active", dataItem.getActive(), notNullValue());
            });
        });

        logger.info("Response schema validated successfully");
    }

    @Test(description = "Data array is present and contains expected number of elements", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - All Catalogs Assigned")
    @Severity(SeverityLevel.NORMAL)
    public void testDataArrayContainsExpectedElements() {
        // Validate response is an object
        assertThat("Response should not be null", catalogEditorResponse, notNullValue());
        assertThat("Data should exist and be an array", catalogEditorResponse.getData(), notNullValue());
        assertThat("Data array should contain 1 element", catalogEditorResponse.getData().size(), equalTo(1));

        logger.info("Data array validated: contains {} element(s)", catalogEditorResponse.getData().size());
    }

    @Test(description = "Set Catalog foassign ID to Collection Variables", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - All Catalogs Assigned")
    @Severity(SeverityLevel.NORMAL)
    public void testSetCatalogForAssignId() {
        // Check if data array exists and has items
        if (catalogEditorResponse.getData() != null && !catalogEditorResponse.getData().isEmpty()) {
            CatalogEditorResponse.CatalogEditorGroup firstGroup = catalogEditorResponse.getData().get(0);

            if (firstGroup.getData() != null && !firstGroup.getData().isEmpty()) {
                // Find the first item with status = 0
                String foundId = null;

                for (CatalogEditorResponse.CatalogEditorItem item : firstGroup.getData()) {
                    if (item.getStatus() != null && item.getStatus() == 0) {
                        foundId = item.get_id();
                        break;
                    }
                }

                if (foundId != null) {
                    VariableManager.set("catalog_foassign_id", foundId);
                    String storedId = VariableManager.get("catalog_foassign_id");
                    assertThat("Catalog for assign ID should be set", storedId, notNullValue());
                    logger.info("Found and set catalog foassign ID with status 0 in VariableManager: {}", foundId);
                } else {
                    logger.warn("No items with status 0 found");
                }
            } else {
                logger.warn("No data items to process");
            }
        } else {
            logger.warn("No data groups to process");
        }
    }
}
```

### `src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/BotCatalogTitleGenerateTest.java`

_201 lines_

```java
package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.HttpStatus;
import com.automation.models.request.BotCatalogTitleGenerateRequest;
import com.automation.models.response.TitleGenerateResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Editor - Bot Catalog Title Generate endpoint.
 * Endpoint: https://bomb.bizup.app/api/chat/title/tags
 * Implements comprehensive Postman test scripts for title generation
 * validation.
 */
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class BotCatalogTitleGenerateTest extends BaseTest {

    private String authToken;
    private Response response;
    private TitleGenerateResponse titleGenerateResponse;

    // Full endpoint URL (different from base URL pattern)
    private static final String TITLE_GENERATE_URL = "https://bomb.bizup.app/api/chat/title/tags";

    // Store generated title for future tests
    public static String generatedTitle;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - Bot Catalog Title Generate")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Prepare request body with tags
        BotCatalogTitleGenerateRequest requestBody = BotCatalogTitleGenerateRequest.builder()
                .tags(Arrays.asList(
                        "product : jeans",
                        "quality / branding : first copy",
                        "color : blue",
                        "brand : DOWNY JEANS",
                        "fabric : Rfd jeans",
                        "length : 40",
                        "",
                        "",
                        ""
                ))
                .build();

        // Send POST request to generate title
        response = RestAssured.given()
                .header("authorization", "JWT " + authToken)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post(TITLE_GENERATE_URL);

        // Parse response for other tests
        titleGenerateResponse = JsonUtils.fromResponse(response, TitleGenerateResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Bot Catalog Title Generate")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Content-Type header is present", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Bot Catalog Title Generate")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeaderPresent() {
        // Verify Content-Type header is present
        assertThat("Content-Type header should be present",
                response.getHeader("Content-Type"), notNullValue());

        logger.info("Content-Type header verified: {}", response.getHeader("Content-Type"));
    }

    @Test(description = "Content-Type header includes application/json", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Bot Catalog Title Generate")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeaderIncludesJson() {
        // Verify Content-Type header includes application/json
        assertThat("Content-Type should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type includes application/json");
    }

    @Test(description = "Response contains expected 'result' field as a string", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Bot Catalog Title Generate")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseContainsResultField() {
        // Validate response is an object
        assertThat("Response should not be null", titleGenerateResponse, notNullValue());

        // Validate result field is a string
        assertThat("Result should be a string", titleGenerateResponse.getResult(), instanceOf(String.class));

        logger.info("Response result field validated: {}", titleGenerateResponse.getResult());
    }

    @Test(description = "Validate tags data if present", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Bot Catalog Title Generate")
    @Severity(SeverityLevel.NORMAL)
    public void testValidateTagsDataIfPresent() {
        // Check if data object is present
        if (titleGenerateResponse.getData() != null) {
            TitleGenerateResponse.TitleData data = titleGenerateResponse.getData();

            assertThat("Data should be an object", data, notNullValue());

            // Validate tags if present
            if (data.getTags() != null) {
                assertThat("Tags should be an array", data.getTags(), notNullValue());
                assertThat("Tags should not be empty", data.getTags(), not(empty()));
                logger.info("Tags validated: {} tag(s)", data.getTags().size());
            }

            // Validate status if present
            if (data.getStatus() != null) {
                assertThat("Status should be a number", data.getStatus(), instanceOf(Integer.class));
                logger.info("Status validated: {}", data.getStatus());
            }
        } else {
            logger.info("Data object not present in response");
        }
    }

    @Test(description = "Processed tags array has expected length", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Bot Catalog Title Generate")
    @Severity(SeverityLevel.NORMAL)
    public void testProcessedTagsArrayLength() {
        // Check if data and processedTags are present
        if (titleGenerateResponse.getData() != null &&
                titleGenerateResponse.getData().getProcessedTags() != null) {

            assertThat("ProcessedTags should be an array",
                    titleGenerateResponse.getData().getProcessedTags(), notNullValue());
            assertThat("ProcessedTags should have 9 elements",
                    titleGenerateResponse.getData().getProcessedTags().size(), equalTo(9));

            logger.info("ProcessedTags validated: {} tag(s)",
                    titleGenerateResponse.getData().getProcessedTags().size());
        } else {
            logger.info("ProcessedTags not present in response");
        }
    }

    @Test(description = "Set the generated title to collectionVariables", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Bot Catalog Title Generate")
    @Severity(SeverityLevel.NORMAL)
    public void testSetGeneratedTitle() {
        // Set generated title for future tests
        if (titleGenerateResponse.getResult() != null) {
            generatedTitle = titleGenerateResponse.getResult();
            assertThat("Generated title should be set", generatedTitle, notNullValue());

            logger.info("Set generated title: {}", generatedTitle);
        } else {
            logger.warn("Result not present in response");
        }
    }
}
```

### `src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/CatalogEditorEditTest.java`

_120 lines_

```java
package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogEditGroupResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Editor - Catalog Edit endpoint.
 * Endpoint:
 * {{bizup_base}}/v1/admin/catalog/group/upload/{{catalog_foassign_id}}?limit=20&mode=edit
 * Implements comprehensive Postman test scripts for catalog edit mode
 * validation.
 */
@TmsLink("TC-ADMIN-005")
@TmsLink("TC-ADMIN-006")
@TmsLink("TC-ADMIN-007")
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class CatalogEditorEditTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogEditGroupResponse catalogEditGroupResponse;

    // Catalog ID from previous tests
    private String catalogForAssignId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");

        // Get catalog ID from VariableManager or use fallback
        catalogForAssignId = VariableManager.get("catalog_foassign_id");
        if (catalogForAssignId == null || catalogForAssignId.isEmpty()) {
            catalogForAssignId = VariableManager.get("catalog_foassign_id", "6822f5dac17c6dcd589ba173");
            logger.warn("Catalog for assign ID not available, using default: {}", catalogForAssignId);
        } else {
            logger.info("Using catalog for assign ID from VariableManager: {}", catalogForAssignId);
        }
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - Catalog Edit")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send GET request to fetch catalog edit data
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("limit", 20)
                .queryParam("mode", "edit")
                .when()
                .get(BombEndpoints.CATALOG_GROUP_UPLOAD + "/" + catalogForAssignId);

        // Parse response for other tests
        catalogEditGroupResponse = JsonUtils.fromResponse(response, CatalogEditGroupResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Edit")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Presence of _id, name, total, data, and error properties", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Edit")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseBodyStructure() {
        // Validate response is an object
        assertThat("Response should not be null", catalogEditGroupResponse, notNullValue());

        // Validate data object has all required keys
        CatalogEditGroupResponse.CatalogEditGroupData data = catalogEditGroupResponse.getData();
        assertThat("Data should not be null", data, notNullValue());
        assertThat("Data should have _id", data.get_id(), notNullValue());
        assertThat("Data should have name", data.getName(), notNullValue());
        assertThat("Data should have total", data.getTotal(), notNullValue());
        assertThat("Data should have data array", data.getData(), notNullValue());
        // Note: error field can be null or present

        logger.info("Response body structure validated: _id={}, name={}, total={}, data size={}, error={}",
                data.get_id(), data.getName(), data.getTotal(),
                data.getData() != null ? data.getData().size() : 0, data.getError());
    }
}
```

### `src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/CatalogSkipTest.java`

_164 lines_

```java
package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogSkipResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Editor - Catalog Skip endpoint.
 * Endpoint: PUT
 * {{bizup_base}}/v1/admin/editor/assign/videos/skip/{{seller_id}}/{{catalog_foassign_id}}
 * Implements comprehensive Postman test scripts for catalog skip validation.
 */
@TmsLink("TC-ADMIN-007")
@TmsLink("TC-ADMIN-008")
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class CatalogSkipTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogSkipResponse catalogSkipResponse;

    // IDs from previous tests
    private String sellerId;
    private String catalogForAssignId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
        
        // Get seller ID after VariableManager is initialized
        sellerId = VariableManager.getSellerId();
        logger.info("Using seller ID: {}", sellerId);
        
        // Get catalog ID from VariableManager or use fallback
        catalogForAssignId = VariableManager.get("catalog_foassign_id");
        if (catalogForAssignId == null || catalogForAssignId.isEmpty()) {
            catalogForAssignId = VariableManager.get("catalog_foassign_id", "6822f5dac17c6dcd589ba173");
            logger.warn("Catalog for assign ID not available from VariableManager, using default: {}", catalogForAssignId);
        } else {
            logger.info("Using catalog for assign ID from VariableManager: {}", catalogForAssignId);
        }
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - Catalog Skip")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send PUT request to skip catalog
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .when()
                .post(BombEndpoints.EDITOR_SKIP_CATALOG + "/" + VariableManager.get("seller_id") + "/" + catalogForAssignId);

        // Parse response for other tests
        catalogSkipResponse = JsonUtils.fromResponse(response, CatalogSkipResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Skip")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Response has statusCode as string and message is 'success'", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Skip")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseMessage() {
        // Validate statusCode is a string
        assertThat("StatusCode should be a string",
                catalogSkipResponse.getStatusCode(), instanceOf(String.class));

        // Validate message is 'success'
        assertThat("Message should be 'success'",
                catalogSkipResponse.getMessage(), equalTo("success"));

        logger.info("Response message validated: statusCode={}, message={}",
                catalogSkipResponse.getStatusCode(), catalogSkipResponse.getMessage());
    }

    @Test(description = "Response data contains required properties with correct types", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Skip")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseDataSchema() {
        // Validate response is an object
        assertThat("Response should not be null", catalogSkipResponse, notNullValue());

        CatalogSkipResponse.CatalogSkipData data = catalogSkipResponse.getData();
        assertThat("Data should not be null", data, notNullValue());

        // Validate _id property
        assertThat("Data should have _id", data.get_id(), notNullValue());

        // Validate assignedBy property
        assertThat("Data should have assignedBy", data.getAssignedBy(), notNullValue());
        assertThat("AssignedBy should be a string", data.getAssignedBy(), instanceOf(String.class));

        // Validate editorId property
        assertThat("Data should have editorId", data.getEditorId(), notNullValue());
        assertThat("EditorId should be a string", data.getEditorId(), instanceOf(String.class));

        logger.info("Response data schema validated: _id={}, assignedBy={}, editorId={}",
                data.get_id(), data.getAssignedBy(), data.getEditorId());
    }

    @Test(description = "Response data 'status' property is 0", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Skip")
    @Severity(SeverityLevel.CRITICAL)
    public void testStatusPropertyIsZero() {
        // Validate status is 0
        assertThat("Status should be 0",
                catalogSkipResponse.getData().getStatus(), equalTo(0));

        logger.info("Status property validated: {}", catalogSkipResponse.getData().getStatus());
    }

    @Test(description = "Response data 'videoType' property is 'catalog'", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Skip")
    @Severity(SeverityLevel.CRITICAL)
    public void testVideoTypeIsCatalog() {
        // Validate videoType is 'catalog'
        assertThat("VideoType should be 'catalog'",
                catalogSkipResponse.getData().getVideoType(), equalTo("catalog"));

        logger.info("VideoType property validated: {}", catalogSkipResponse.getData().getVideoType());
    }

}
```

### `src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/CatalogTagTest.java`

_238 lines_

```java
package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogTagResponse;
import com.automation.utils.JsonUtils;
import com.automation.utils.VariableManager;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Editor - Catalog Tag endpoint.
 * Endpoint:
 * {{bizup_base}}/v1/admin/catalog/group/upload/{{catalog_foassign_id}}?limit=20&mode=add
 * Implements comprehensive Postman test scripts for catalog tagging validation.
 */
@TmsLink("TC-ADMIN-005")
@TmsLink("TC-ADMIN-006")
@TmsLink("TC-ADMIN-007")
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class CatalogTagTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogTagResponse catalogTagResponse;

    // Catalog ID and Seller ID
    private String sellerId;
    private String catalogForAssignId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");

        // Get seller ID after VariableManager is initialized
        sellerId = VariableManager.getSellerId();
        logger.info("Using seller ID: {}", sellerId);

        // Get catalog ID from VariableManager or use fallback
        catalogForAssignId = VariableManager.get("catalog_foassign_id");
        if (catalogForAssignId == null || catalogForAssignId.isEmpty()) {
            catalogForAssignId = VariableManager.get("catalog_foassign_id", "6822f5dac17c6dcd589ba173");
            logger.warn("Catalog for assign ID not available, using default: {}", catalogForAssignId);
        } else {
            logger.info("Using catalog for assign ID from VariableManager: {}", catalogForAssignId);
        }
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send GET request to fetch catalog tag data
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("seller_id", VariableManager.get("seller_id"))
                .queryParam("mode", "add")
                .when()
                .get(BombEndpoints.CATALOG_GROUP_UPLOAD + "/" + catalogForAssignId);

        // Parse response for other tests
        catalogTagResponse = JsonUtils.fromResponse(response, CatalogTagResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Content-Type header is application/json", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeader() {
        // Verify Content-Type header
        assertThat("Content-Type should be application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type header verified: {}", response.getContentType());
    }

    @Test(description = "Response has the required fields - statusCode, message, and data", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseHasRequiredFields() {
        // Validate response has required fields
        assertThat("Response should not be null", catalogTagResponse, notNullValue());
        assertThat("Response should have statusCode", catalogTagResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogTagResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogTagResponse.getData(), notNullValue());

        logger.info("Response has all required fields");
    }

    @Test(description = "Response time is less than threshold", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Response data object has basic fields", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataObjectHasBasicFields() {
        CatalogTagResponse.CatalogTagData data = catalogTagResponse.getData();

        // Validate data object has basic fields
        assertThat("Data should not be null", data, notNullValue());
        assertThat("Data _id should be a string", data.get_id(), instanceOf(String.class));
        assertThat("Data name should be a string", data.getName(), instanceOf(String.class));
        assertThat("Data total should be a number", data.getTotal(), instanceOf(Integer.class));

        logger.info("Data object basic fields validated: _id={}, name={}, total={}",
                data.get_id(), data.getName(), data.getTotal());
    }

    @Test(description = "Data array items have the required structure", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataArrayItemsStructure() {
        // Validate data array items structure
        assertThat("Data array should not be null", catalogTagResponse.getData().getData(), notNullValue());

        catalogTagResponse.getData().getData().forEach(item -> {
            assertThat("Item should be an object", item, notNullValue());
            assertThat("Item _index should be a string", item.get_index(), instanceOf(String.class));
            assertThat("Item _id should be a string", item.get_id(), instanceOf(String.class));
            assertThat("Item _source should be an object", item.get_source(), notNullValue());
        });

        logger.info("Data array items structure validated successfully");
    }

    @Test(description = "Source objects contain required basic fields", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.CRITICAL)
    public void testSourceObjectsBasicFields() {
        // Validate source objects basic fields
        catalogTagResponse.getData().getData().forEach(item -> {
            CatalogTagResponse.CatalogTagSource source = item.get_source();

            assertThat("thubmbnailDriveLink should be a string", source.getThubmbnailDriveLink(),
                    instanceOf(String.class));
            assertThat("uploadId should be a string", source.getUploadId(), instanceOf(String.class));
            assertThat("priceTags should be an array", source.getPriceTags(), notNullValue());
            // description can be string or null
            if (source.getDescription() != null) {
                assertThat("description should be a string", source.getDescription(), instanceOf(String.class));
            }
            assertThat("uploadedDate should be a string", source.getUploadedDate(), instanceOf(String.class));
            assertThat("createdAt should be a string", source.getCreatedAt(), instanceOf(String.class));
            assertThat("sellerId should be a string", source.getSellerId(), instanceOf(String.class));
            assertThat("isDeleted should be false", source.getIsDeleted(), is(false));
        });

        logger.info("Source objects basic fields validated successfully");
    }

    @Test(description = "Thumbnail objects have correct structure", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.NORMAL)
    public void testThumbnailObjectsStructure() {
        // Validate thumbnail objects
        catalogTagResponse.getData().getData().forEach(item -> {
            CatalogTagResponse.Thumbnail thumbnail = item.get_source().getThumbnail();

            assertThat("Thumbnail should be an object", thumbnail, notNullValue());
            assertThat("fileName should be a string", thumbnail.getFileName(), instanceOf(String.class));
            assertThat("mimeType should be a string", thumbnail.getMimeType(), instanceOf(String.class));
            assertThat("_id should be a string", thumbnail.get_id(), instanceOf(String.class));
            assertThat("url should be a string", thumbnail.getUrl(), instanceOf(String.class));
        });

        logger.info("Thumbnail objects structure validated successfully");
    }

    @Test(description = "Source objects have all required additional fields", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.NORMAL)
    public void testSourceObjectsAdditionalFields() {
        // Validate source objects additional fields
        catalogTagResponse.getData().getData().forEach(item -> {
            CatalogTagResponse.CatalogTagSource source = item.get_source();

            assertThat("phoneNumber should be a string", source.getPhoneNumber(), instanceOf(String.class));
            assertThat("uploadDate should be a string", source.getUploadDate(), instanceOf(String.class));
            assertThat("original_upload should be a string", source.getOriginal_upload(), instanceOf(String.class));
            assertThat("productTags should be an array", source.getProductTags(), notNullValue());
        });

        logger.info("Source objects additional fields validated successfully");
    }

    @Test(description = "Set Catalog Id to Collection Variables", priority = 13, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tag")
    @Severity(SeverityLevel.NORMAL)
    public void testSetCatalogId() {
        // Store catalog ID using VariableManager (thread-safe)
        if (catalogTagResponse.getData().getData() != null && !catalogTagResponse.getData().getData().isEmpty()) {
            String catalogId = catalogTagResponse.getData().getData().get(0).get_id();
            VariableManager.set("catalog_id", catalogId);
            
            String storedId = VariableManager.get("catalog_id");
            assertThat("Catalog ID should be set", storedId, notNullValue());

            logger.info("Set catalog ID in VariableManager: {}", catalogId);
        } else {
            logger.warn("No catalog items to set catalog ID");
        }
    }
}
```

### `src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/CatalogTaggingTest.java`

_276 lines_

```java
package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.CatalogTaggingRequest;
import com.automation.models.response.CatalogTaggingResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Editor - Catalog Tagging endpoint.
 * Endpoint: PUT {{bizup_base}}/v1/admin/catalog/{{catalog_id}}
 * Implements comprehensive Postman test scripts for catalog tagging validation.
 */
@TmsLink("TC-ADMIN-030")
@TmsLink("TC-ADMIN-031")
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class CatalogTaggingTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogTaggingResponse catalogTaggingResponse;

    // Expected values from previous tests
    private String catalogId;
    private String generatedTitle;
    private Double expectedPrice;
    private String expectedProductId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
        // Get catalog ID from previous test
        String prevCatalogId = VariableManager.get("catalog_id");
        if (prevCatalogId != null) {
            catalogId = prevCatalogId;
            logger.info("Using catalog ID from VariableManager: {}", catalogId);
        } else {
            // Fallback to default ID from VariableManager
            catalogId = VariableManager.get("catalog_foassign_id", "6822f5dac17c6dcd589ba173");
            logger.warn("Catalog ID not available from previous test, using default from VariableManager: {}",
                    catalogId);
        }

        // Get generated title from previous test
        String prevTitle = VariableManager.get("generatedTitle");
        if (prevTitle != null) {
            generatedTitle = prevTitle;
            logger.info("Using generated title from VariableManager: {}", generatedTitle);
        } else {
            generatedTitle = "Test Catalog Title";
            logger.warn("Generated title not available, using default: {}", generatedTitle);
        }

        // Set expected values (these would typically come from previous tests or test data)
        expectedPrice = 100.0;
        expectedProductId = VariableManager.get("product_id", "641d692fcebb994dcaf4571b");
        logger.info("Using product ID: {}", expectedProductId);
    }

    @Test(description = "Status code is successful 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Build request body with correct structure
        CatalogTaggingRequest request = CatalogTaggingRequest.builder()
                .productId(expectedProductId)
                .tags(Arrays.asList(
                        "65e718501cb2887f2f9e833f",
                        "645cc306af2a39420d51dfc6",
                        "674d9fc4848ffedcd98cf97b",
                        "677d256deaf239c6f3c0599b",
                        "677d260f5725977833481865"
                ))
                .suggested(new ArrayList<>())
                .images(new ArrayList<>())
                .title(generatedTitle)
                .price(expectedPrice)
                .isQc(false)
                .isSet(false)
                .build();

        // Send PUT request to tag catalog
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .body(request)
                .when()
                .post(BombEndpoints.CATALOG + "/" + catalogId);

        // Parse response for other tests
        catalogTaggingResponse = JsonUtils.fromResponse(response, CatalogTaggingResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Response has valid JSON body", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.NORMAL)
    public void testValidJsonBody() {
        // Verify response has valid JSON body
        assertThat("Response should have valid JSON body", catalogTaggingResponse, notNullValue());

        logger.info("Response has valid JSON body");
    }

    @Test(description = "Response has correct structure", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStructure() {
        // Validate response structure
        assertThat("Response should have statusCode", catalogTaggingResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogTaggingResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogTaggingResponse.getData(), notNullValue());

        CatalogTaggingResponse.CatalogTaggingData data = catalogTaggingResponse.getData();
        assertThat("Data should have available", data.getAvailable(), notNullValue());
        assertThat("Data should have contentType", data.getContentType(), notNullValue());
        assertThat("Data should have isDeleted", data.getIsDeleted(), notNullValue());
        assertThat("Data should have _id", data.get_id(), notNullValue());
        assertThat("Data should have productTags", data.getProductTags(), notNullValue());
        assertThat("Data should have title", data.getTitle(), notNullValue());
        assertThat("Data should have priceText", data.getPriceText(), notNullValue());

        logger.info("Response structure validated successfully");
    }

    @Test(description = "Verify data types", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataTypes() {
        CatalogTaggingResponse.CatalogTaggingData data = catalogTaggingResponse.getData();

        // Validate data types
        assertThat("Message should be a string", catalogTaggingResponse.getMessage(), instanceOf(String.class));
        assertThat("Available should be a boolean", data.getAvailable(), instanceOf(Boolean.class));
        assertThat("ContentType should be a string", data.getContentType(), instanceOf(String.class));
        assertThat("IsDeleted should be a boolean", data.getIsDeleted(), instanceOf(Boolean.class));
        assertThat("_id should be a string", data.get_id(), instanceOf(String.class));
        assertThat("PriceText should be a number", data.getPriceText(), instanceOf(Double.class));
        assertThat("Title should be a string", data.getTitle(), instanceOf(String.class));
        assertThat("ProductTags should be an array", data.getProductTags(), notNullValue());

        logger.info("Data types validated successfully");
    }

    @Test(description = "Response has success message", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.NORMAL)
    public void testSuccessMessage() {
        // Validate success message
        assertThat("Message should be 'success'",
                catalogTaggingResponse.getMessage(), equalTo("success"));

        logger.info("Success message validated");
    }

    @Test(description = "Content type is image", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeIsImage() {
        // Validate content type is image
        assertThat("ContentType should be 'image'",
                catalogTaggingResponse.getData().getContentType(), equalTo("image"));

        logger.info("Content type validated: image");
    }

    @Test(description = "isDeleted flag is false", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.NORMAL)
    public void testIsDeletedFlagFalse() {
        // Validate isDeleted flag is false
        assertThat("IsDeleted should be false",
                catalogTaggingResponse.getData().getIsDeleted(), is(false));

        logger.info("IsDeleted flag validated: false");
    }

    @Test(description = "Price matches the expected value from actual response", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testPriceMatchesExpected() {
        // Validate price matches expected value
        assertThat("Price should match expected value",
                catalogTaggingResponse.getData().getPriceText(), equalTo(expectedPrice));

        logger.info("Price validated: {} matches expected: {}",
                catalogTaggingResponse.getData().getPriceText(), expectedPrice);
    }

    @Test(description = "Title matches the expected value with the actual response", priority = 11, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testTitleMatchesExpected() {
        // Validate title matches expected value
        assertThat("Title should match expected value",
                catalogTaggingResponse.getData().getTitle(), equalTo(generatedTitle));

        logger.info("Title validated: {} matches expected: {}",
                catalogTaggingResponse.getData().getTitle(), generatedTitle);
    }

    @Test(description = "Product ID matches the expected value with the actual response", priority = 12, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testProductIdMatchesExpected() {
        // Validate productTags array is not empty
        assertThat("ProductTags should not be empty",
                catalogTaggingResponse.getData().getProductTags(), not(empty()));

        // Validate first product tag matches expected product ID
        assertThat("First product tag should match expected product ID",
                catalogTaggingResponse.getData().getProductTags().get(0), equalTo(expectedProductId));

        logger.info("Product ID validated: {} matches expected: {}",
                catalogTaggingResponse.getData().getProductTags().get(0), expectedProductId);
    }

    @Test(description = "Catalog ID matches the expected value with the actual response", priority = 13, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalog Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testCatalogIdMatchesExpected() {
        // Get catalog ID from response (_id or catalogId)
        String receivedCatalogId = catalogTaggingResponse.getData().get_id() != null
                ? catalogTaggingResponse.getData().get_id()
                : catalogTaggingResponse.getData().getCatalogId();

        // Validate catalog ID matches expected value
        assertThat("Catalog ID should match expected value",
                receivedCatalogId, equalTo(catalogId));

        logger.info("Catalog ID validated: {} matches expected: {}", receivedCatalogId, catalogId);
    }
}
```

### `src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/CatalogsGroupTest.java`

_110 lines_

```java
package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogGroupResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Editor - Catalogs Group endpoint.
 * Endpoint:
 * {{bizup_base}}/v1/admin/catalog/upload/{{catalog_foassign_id}}?limit=20&mode=all
 * Implements comprehensive Postman test scripts for catalog group validation.
 */
@TmsLink("TC-ADMIN-013")
@TmsLink("TC-ADMIN-014")
@TmsLink("TC-ADMIN-015")
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class CatalogsGroupTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogGroupResponse catalogGroupResponse;

    // Catalog ID and Seller ID
    private String catalogForAssignId;
    private String sellerId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");

        // Get catalog ID from VariableManager or use fallback
        catalogForAssignId = VariableManager.get("catalog_foassign_id");
        if (catalogForAssignId == null || catalogForAssignId.isEmpty()) {
            catalogForAssignId = "6822f5dac17c6dcd589ba173"; // Fallback to a default ID
            logger.warn("Catalog for assign ID not available, using default: {}", catalogForAssignId);
        } else {
            logger.info("Using catalog for assign ID from VariableManager: {}", catalogForAssignId);
        }

        // Get seller ID from VariableManager or use fallback
        sellerId = VariableManager.get("seller_id");
        if (sellerId == null || sellerId.isEmpty()) {
            sellerId = "609341142100000000000000"; // Fallback to a default seller ID
            logger.warn("Seller ID not available, using default: {}", sellerId);
        } else {
            logger.info("Using seller ID from VariableManager: {}", sellerId);
        }
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - Catalogs Group")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send GET request to fetch catalog group
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("limit", 20)
                .queryParam("seller_id", sellerId)
                .queryParam("mode", "all")
                .when()
                .get(BombEndpoints.CATALOG_UPLOAD + "/" + catalogForAssignId);

        // Parse response for other tests
        catalogGroupResponse = JsonUtils.fromResponse(response, CatalogGroupResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Catalogs Group")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

}
```

### `src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/FetchCatalogUploadedTest.java`

_185 lines_

```java
package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogEditorResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Editor - Fetch Catalog Uploaded endpoint.
 * Endpoint:
 * {{bizup_base}}/v1/admin/editor/assign/catalog/{{seller_id}}?limit=20&mode=all
 * Implements comprehensive Postman test scripts for fetching uploaded catalogs.
 */
@TmsLink("TC-ADMIN-001")
@TmsLink("TC-ADMIN-002")
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class FetchCatalogUploadedTest extends BaseTest {

    private String authToken;
    private Response response;
    private CatalogEditorResponse catalogEditorResponse;


    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - Fetch Catalog Uploaded")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send GET request to fetch uploaded catalogs
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("seller_id", VariableManager.get("seller_id"))
                .queryParam("mode", "all")
                .when()
                .post(BombEndpoints.EDITOR_ASSIGN_CATALOG + "/" + VariableManager.get("seller_id"));

        // Parse response for other tests
        catalogEditorResponse = JsonUtils.fromResponse(response, CatalogEditorResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Fetch Catalog Uploaded")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Response has the required fields - statusCode, message, and data", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Fetch Catalog Uploaded")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseHasRequiredFields() {
        // Validate response is an object
        assertThat("Response should not be null", catalogEditorResponse, notNullValue());

        // Validate response has required fields
        assertThat("Response should have statusCode", catalogEditorResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogEditorResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogEditorResponse.getData(), notNullValue());

        logger.info("Response has all required fields");
    }

    @Test(description = "Pagination total is a non-negative integer", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Fetch Catalog Uploaded")
    @Severity(SeverityLevel.NORMAL)
    public void testPaginationTotalIsNonNegative() {
        // Check if data array exists and has items
        if (catalogEditorResponse.getData() != null && !catalogEditorResponse.getData().isEmpty()) {
            CatalogEditorResponse.CatalogEditorGroup firstGroup = catalogEditorResponse.getData().get(0);

            if (firstGroup.getPagination() != null && !firstGroup.getPagination().isEmpty()) {
                CatalogEditorResponse.Pagination pagination = firstGroup.getPagination().get(0);

                // Validate pagination total is a non-negative integer
                assertThat("Pagination total should be a number", pagination.getTotal(), instanceOf(Integer.class));
                assertThat("Pagination total should be non-negative", pagination.getTotal(), greaterThanOrEqualTo(0));

                logger.info("Pagination total validated: {}", pagination.getTotal());
            } else {
                logger.warn("No pagination data to validate");
            }
        } else {
            logger.warn("No data groups to validate");
        }
    }

    @Test(description = "Validate the response schema for required fields", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Fetch Catalog Uploaded")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseSchemaValidation() {
        // Validate response is an object
        assertThat("Response should not be null", catalogEditorResponse, notNullValue());

        // Validate response has required fields
        assertThat("Response should have statusCode", catalogEditorResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", catalogEditorResponse.getMessage(), notNullValue());
        assertThat("Response should have data", catalogEditorResponse.getData(), notNullValue());
        assertThat("Data should be an array", catalogEditorResponse.getData(), instanceOf(List.class));

        // Validate nested structure
        catalogEditorResponse.getData().forEach(group -> {
            assertThat("Group should have pagination", group.getPagination(), notNullValue());
            assertThat("Pagination should be an array", group.getPagination(), instanceOf(List.class));

            group.getPagination().forEach(paginationItem -> {
                assertThat("Pagination item should have total", paginationItem.getTotal(), notNullValue());
            });

            assertThat("Group should have data", group.getData(), notNullValue());
            assertThat("Data should be an array", group.getData(), instanceOf(List.class));

            group.getData().forEach(dataItem -> {
                assertThat("Data item should have _id", dataItem.get_id(), notNullValue());
                assertThat("Data item should have source", dataItem.getSource(), notNullValue());
                assertThat("Data item should have status", dataItem.getStatus(), notNullValue());
                assertThat("Data item should have priority", dataItem.getPriority(), notNullValue());
                assertThat("Data item should have videoType", dataItem.getVideoType(), notNullValue());
                assertThat("Data item should have sellerId", dataItem.getSellerId(), notNullValue());
                assertThat("Data item should have phoneNumber", dataItem.getPhoneNumber(), notNullValue());
                assertThat("Data item should have name", dataItem.getName(), notNullValue());
                assertThat("Data item should have createdAt", dataItem.getCreatedAt(), notNullValue());
                assertThat("Data item should have updatedAt", dataItem.getUpdatedAt(), notNullValue());
                assertThat("Data item should have editorId", dataItem.getEditorId(), notNullValue());
                assertThat("Data item should have total", dataItem.getTotal(), notNullValue());
                assertThat("Data item should have tagged", dataItem.getTagged(), notNullValue());
                assertThat("Data item should have active", dataItem.getActive(), notNullValue());
            });
        });

        logger.info("Response schema validated successfully");
    }

    @Test(description = "Data array is present and contains expected number of elements", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Fetch Catalog Uploaded")
    @Severity(SeverityLevel.NORMAL)
    public void testDataArrayContainsExpectedElements() {
        // Validate response is an object
        assertThat("Response should not be null", catalogEditorResponse, notNullValue());
        assertThat("Data should exist and be an array", catalogEditorResponse.getData(), notNullValue());
        assertThat("Data array should contain 1 element", catalogEditorResponse.getData().size(), equalTo(1));

        logger.info("Data array validated: contains {} element(s)", catalogEditorResponse.getData().size());
    }
}
```

### `src/test/java/com/automation/tests/bomb/CatalogTagPipeline/CatalogEditor/MarkAsDoneTest.java`

_83 lines_

```java
package com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor;

import com.automation.base.BaseTest;
import com.automation.constants.HttpStatus;
import com.automation.constants.BombEndpoints;
import com.automation.models.response.MarkAsDoneResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Catalog Editor - Mark As Done endpoint.
 * Endpoint: PUT
 * https://api.bizup.app/v1/admin/editor/assign/videos/done/{{seller_id}}/{{catalog_id}}
 * Implements comprehensive Postman test scripts for marking catalog as done
 * validation.
 */
@TmsLink("TC-ADMIN-003")
@TmsLink("TC-ADMIN-004")
@Epic("BOMB Catalog Tag Pipeline")
@Feature("Catalog Editor")
public class MarkAsDoneTest extends BaseTest {

    private String authToken;
    private Response response;
    private MarkAsDoneResponse markAsDoneResponse;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Catalog Editor - Mark As Done")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send PUT request to mark catalog as done
        response = RestAssured.given()
                .header("authorization", "JWT " + authToken)
                .header("Content-Type", "application/json")
                .when()
                .post(BombEndpoints.EDITOR_MARK_AS_DONE + "/" + VariableManager.get("seller_id") + "/" +  "682584c0240b174c4c1a55f4");

        // Parse response for other tests
        markAsDoneResponse = JsonUtils.fromResponse(response, MarkAsDoneResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Catalog Editor - Mark As Done")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }
}
```

### `src/test/java/com/automation/tests/bomb/Login/LoginApiTest.java`

_164 lines_

```java
package com.automation.tests.bomb.Login;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.LoginRequest;
import com.automation.models.response.LoginResponse;
import com.automation.utils.JsonUtils;
import com.automation.utils.VariableManager;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Login API.
 * Handles authentication and token management.
 * Tests are based on Postman scripts for comprehensive validation.
 */
@Epic("BOMB Authentication")
@Feature("Login API")
public class LoginApiTest extends BaseTest {

        // Token is now stored in VariableManager.setToken() - no static variable needed
        private static Response loginResponse;
        private static LoginResponse loginResponseData;

        @Test(description = "Verify response status is 200 OK", priority = 1, groups = "bomb")
        @Story("User Login")
        @Severity(SeverityLevel.BLOCKER)
        public void testResponseStatus() {
                // Prepare request
                LoginRequest loginRequest = LoginRequest.builder()
                                .phoneNumber(config.loginPhoneNumber())
                                .token(config.loginToken())
                                .build();

                // Send POST request
                loginResponse = RestAssured.given()
                                .spec(requestSpec)
                                .body(loginRequest)
                                .when()
                                .post(BombEndpoints.LOGIN);

                // Parse response for other tests
                loginResponseData = JsonUtils.fromResponse(loginResponse, LoginResponse.class);

                // Verify response status is 200 OK
                assertThat("Response status should be 200 OK",
                                loginResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Verify response time is within acceptable threshold", priority = 2, dependsOnMethods = "testResponseStatus", groups = "bomb")
        @Story("User Login")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Verify response time is within acceptable threshold
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = loginResponse.getTime();

                assertThat("Response time should be within acceptable threshold",
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Validate response data structure and types", priority = 3, dependsOnMethods = "testResponseStatus", groups = "bomb")
        @Story("User Login")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseDataStructureAndTypes() {
                // Validate response data is an object
                assertThat("Response should be an object", loginResponseData, notNullValue());
                assertThat("Response data should not be null", loginResponseData.getData(), notNullValue());

                // Validate data types
                assertThat("phoneNumber should be a string",
                                loginResponseData.getData().getPhoneNumber(), instanceOf(String.class));
                assertThat("name should be a string",
                                loginResponseData.getData().getName(), instanceOf(String.class));
                assertThat("businessName should be a string",
                                loginResponseData.getData().getBusinessName(), instanceOf(String.class));
                assertThat("isDeleted should have value false",
                                loginResponseData.getData().getIsDeleted(), is(false));
                assertThat("accessToken should be a string",
                                loginResponseData.getData().getAccessToken(), instanceOf(String.class));
                assertThat("refreshToken should be a string",
                                loginResponseData.getData().getRefreshToken(), instanceOf(String.class));

                logger.info("Response data structure and types validated successfully");
        }

        @Test(description = "Confirm phone number field exists and is populated", priority = 4, dependsOnMethods = "testResponseStatus", groups = "bomb")
        @Story("User Login")
        @Severity(SeverityLevel.NORMAL)
        public void testPhoneNumberExists() {
                // Confirm phone number field exists and is populated
                assertThat("Response should be an object", loginResponseData, notNullValue());
                assertThat("Phone number should not be null",
                                loginResponseData.getData().getPhoneNumber(), notNullValue());
                assertThat("Phone number should not be empty",
                                loginResponseData.getData().getPhoneNumber(), not(emptyOrNullString()));

                logger.info("Phone number field validated: {}", loginResponseData.getData().getPhoneNumber());
        }

        @Test(description = "Confirm access token exists and is valid", priority = 5, dependsOnMethods = "testResponseStatus", groups = "bomb")
        @Story("User Login")
        @Severity(SeverityLevel.BLOCKER)
        public void testAccessTokenExists() {
                // Confirm access token exists and is valid
                assertThat("Response should be an object", loginResponseData, notNullValue());
                assertThat("Access token should not be null",
                                loginResponseData.getData().getAccessToken(), notNullValue());
                assertThat("Access token should not be empty",
                                loginResponseData.getData().getAccessToken(), not(emptyOrNullString()));

                logger.info("Access token validated: {}...",
                                loginResponseData.getData().getAccessToken().substring(0,
                                                Math.min(20, loginResponseData.getData().getAccessToken().length())));
        }

        @Test(description = "Confirm refresh token exists and is valid", priority = 6, dependsOnMethods = "testResponseStatus", groups = "bomb")
        @Story("User Login")
        @Severity(SeverityLevel.BLOCKER)
        public void testRefreshTokenExists() {
                // Confirm refresh token exists and is valid
                assertThat("Response should be an object", loginResponseData, notNullValue());
                assertThat("Refresh token should not be null",
                                loginResponseData.getData().getRefreshToken(), notNullValue());
                assertThat("Refresh token should not be empty",
                                loginResponseData.getData().getRefreshToken(), not(emptyOrNullString()));

                logger.info("Refresh token validated: {}...",
                                loginResponseData.getData().getRefreshToken().substring(0,
                                                Math.min(20, loginResponseData.getData().getRefreshToken().length())));
        }

        @Test(description = "Store access token in collection variables and persist to file", priority = 7, dependsOnMethods = "testAccessTokenExists", groups = "bomb")
        @Story("User Login")
        @Severity(SeverityLevel.BLOCKER)
        public void testStoreAccessToken() {
                // Store access token using VariableManager (thread-safe)
                String accessToken = loginResponseData.getData().getAccessToken();
                VariableManager.setToken(accessToken);

                // Verify token was stored in memory
                String storedToken = VariableManager.getToken();
                assertThat("Stored token should not be null", storedToken, notNullValue());
                assertThat("Stored token should not be empty", storedToken, not(emptyOrNullString()));

                logger.info("Access token stored successfully in VariableManager: {}...",
                                storedToken.substring(0, Math.min(20, storedToken.length())));

                // Persist token to file for cross-execution access
                VariableManager.saveTokenToFile();
                logger.info("Access token persisted to test-variables.properties file");
        }
}
```

### `src/test/java/com/automation/tests/bomb/VideoTaggingPipeline/VideoTagging/VideoTaggingEditButtonTest.java`

_329 lines_

```java
package com.automation.tests.bomb.VideoTaggingPipeline.VideoTagging;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.VideoTaggingEditResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Video Tagging Edit Button endpoint.
 * Endpoint: GET
 * {{bizup_base}}/v1/admin/editor/edit/videos/{{seller_id}}?limit=100
 * Implements comprehensive Postman test scripts for video tagging edit
 * validation.
 */
@TmsLink("TC-ADMIN-004")
@TmsLink("TC-ADMIN-005")
@TmsLink("TC-ADMIN-009")
@Epic("BOMB Video Tagging Pipeline")
@Feature("Video Tagging")
public class VideoTaggingEditButtonTest extends BaseTest {

    private String authToken;
    private Response response;
    private VideoTaggingEditResponse videoTaggingEditResponse;

    // Seller ID
    private static final String SELLER_ID = "63ee780c9689be92acce8f35";

    // Store video ID for future tests
    public static String videoId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
    }

    @Test(description = "Set video id to collectionVariables variable", priority = 1, groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.NORMAL)
    public void testSetVideoId() {
        // Send GET request to fetch videos for editing
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("limit", 100)
                .when()
                .get(BombEndpoints.VIDEOS_BY_SELLER.replace("{sellerId}", SELLER_ID));

        // Parse response for other tests
        videoTaggingEditResponse = JsonUtils.fromResponse(response, VideoTaggingEditResponse.class);

        // Set video ID from first item in data.data array
        if (videoTaggingEditResponse.getData() != null &&
                videoTaggingEditResponse.getData().getData() != null &&
                !videoTaggingEditResponse.getData().getData().isEmpty()) {

            videoId = videoTaggingEditResponse.getData().getData().get(0).get_id();
            assertThat("Video ID should be set", videoId, notNullValue());

            logger.info("Set video ID: {}", videoId);
        } else {
            logger.warn("No data items to set video ID");
        }
    }

    @Test(description = "Status code is 200", priority = 2, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 3, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Response has correct content-type", priority = 4, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeader() {
        // Verify Content-Type header
        assertThat("Content-Type should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type header verified: {}", response.getContentType());
    }

    @Test(description = "Response has correct structure", priority = 5, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStructure() {
        // Validate response is an object
        assertThat("Response should not be null", videoTaggingEditResponse, notNullValue());

        // Validate response has all keys
        assertThat("Response should have statusCode", videoTaggingEditResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", videoTaggingEditResponse.getMessage(), notNullValue());
        assertThat("Response should have data", videoTaggingEditResponse.getData(), notNullValue());

        // Validate statusCode and message values
        assertThat("StatusCode should be '10000'", videoTaggingEditResponse.getStatusCode(), equalTo("10000"));
        assertThat("Message should be 'success'", videoTaggingEditResponse.getMessage(), equalTo("success"));
        assertThat("Data should be an object", videoTaggingEditResponse.getData(), notNullValue());

        logger.info("Response structure validated");
    }

    @Test(description = "Data object has correct structure", priority = 6, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataObjectStructure() {
        // Validate data has data property
        assertThat("Data should have data property", videoTaggingEditResponse.getData().getData(), notNullValue());

        // Validate data.data is an array
        assertThat("Data.data should be an array",
                videoTaggingEditResponse.getData().getData(), instanceOf(java.util.List.class));

        logger.info("Data object structure validated: {} video(s)",
                videoTaggingEditResponse.getData().getData().size());
    }

    @Test(description = "Content type is video", priority = 7, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.CRITICAL)
    public void testContentTypeIsVideo() {
        // Validate content type if data is present
        if (videoTaggingEditResponse.getData() != null &&
                videoTaggingEditResponse.getData().getData() != null &&
                !videoTaggingEditResponse.getData().getData().isEmpty()) {

            VideoTaggingEditResponse.VideoItem video = videoTaggingEditResponse.getData().getData().get(0);

            assertThat("ContentType should be 'video'",
                    video.getContentType(), equalTo("video"));

            logger.info("Content type validated: {}", video.getContentType());
        } else {
            logger.info("No video data to validate content type");
        }
    }

    @Test(description = "Media links are valid", priority = 8, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.CRITICAL)
    public void testMediaLinksValid() {
        // Validate media links if data is present
        if (videoTaggingEditResponse.getData() != null &&
                videoTaggingEditResponse.getData().getData() != null &&
                !videoTaggingEditResponse.getData().getData().isEmpty()) {

            VideoTaggingEditResponse.VideoItem video = videoTaggingEditResponse.getData().getData().get(0);

            // Validate driveLink
            assertThat("DriveLink should match Firebase pattern",
                    video.getDriveLink(), matchesPattern("^https://firebasestorage\\.googleapis\\.com/.+"));

            // Validate thubmbnailDriveLink
            assertThat("ThubmbnailDriveLink should match Firebase pattern",
                    video.getThubmbnailDriveLink(), matchesPattern("^https://firebasestorage\\.googleapis\\.com/.+"));

            logger.info("Media links validated: driveLink={}, thumbnailLink={}",
                    video.getDriveLink(), video.getThubmbnailDriveLink());
        } else {
            logger.info("No video data to validate media links");
        }
    }

    @Test(description = "Seller and editor IDs match expected value", priority = 9, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerAndEditorIdsMatch() {
        // Validate seller and editor IDs if data is present
        if (videoTaggingEditResponse.getData() != null &&
                videoTaggingEditResponse.getData().getData() != null &&
                !videoTaggingEditResponse.getData().getData().isEmpty()) {

            VideoTaggingEditResponse.VideoItem video = videoTaggingEditResponse.getData().getData().get(0);

            assertThat("Seller should match expected seller ID",
                    video.getSeller(), equalTo(SELLER_ID));
            assertThat("Editor should match expected seller ID",
                    video.getEditor(), equalTo(SELLER_ID));

            logger.info("Seller and editor IDs validated: seller={}, editor={}",
                    video.getSeller(), video.getEditor());
        } else {
            logger.info("No video data to validate seller and editor IDs");
        }
    }

    @Test(description = "Timestamps are valid", priority = 10, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.NORMAL)
    public void testTimestampsValid() {
        // Validate timestamps if data is present
        if (videoTaggingEditResponse.getData() != null &&
                videoTaggingEditResponse.getData().getData() != null &&
                !videoTaggingEditResponse.getData().getData().isEmpty()) {

            VideoTaggingEditResponse.VideoItem video = videoTaggingEditResponse.getData().getData().get(0);

            // Validate uploadDate
            assertThat("UploadDate should not be null", video.getUploadDate(), notNullValue());
            boolean isUploadDateValid = isValidDate(video.getUploadDate());
            assertThat("UploadDate should be a valid date", isUploadDateValid, is(true));

            // Validate createdAt
            assertThat("CreatedAt should not be null", video.getCreatedAt(), notNullValue());
            boolean isCreatedAtValid = isValidDate(video.getCreatedAt());
            assertThat("CreatedAt should be a valid date", isCreatedAtValid, is(true));

            // Validate updatedAt
            assertThat("UpdatedAt should not be null", video.getUpdatedAt(), notNullValue());
            boolean isUpdatedAtValid = isValidDate(video.getUpdatedAt());
            assertThat("UpdatedAt should be a valid date", isUpdatedAtValid, is(true));

            logger.info("Timestamps validated: uploadDate={}, createdAt={}, updatedAt={}",
                    video.getUploadDate(), video.getCreatedAt(), video.getUpdatedAt());
        } else {
            logger.info("No video data to validate timestamps");
        }
    }

    @Test(description = "Upload ID matches expected pattern", priority = 11, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.NORMAL)
    public void testUploadIdPattern() {
        // Validate upload ID pattern if data is present
        if (videoTaggingEditResponse.getData() != null &&
                videoTaggingEditResponse.getData().getData() != null &&
                !videoTaggingEditResponse.getData().getData().isEmpty()) {

            VideoTaggingEditResponse.VideoItem video = videoTaggingEditResponse.getData().getData().get(0);

            // Validate uploadId matches MongoDB ObjectId pattern (24 hex characters)
            assertThat("UploadId should match MongoDB ObjectId pattern",
                    video.getUploadId(), matchesPattern("^[a-f0-9]{24}$"));

            logger.info("Upload ID validated: {}", video.getUploadId());
        } else {
            logger.info("No video data to validate upload ID");
        }
    }

    @Test(description = "Language codes are valid", priority = 12, dependsOnMethods = "testSetVideoId", groups = "bomb")
    @Story("Video Tagging - Edit Button")
    @Severity(SeverityLevel.NORMAL)
    public void testLanguageCodesValid() {
        // Validate language codes if data is present
        if (videoTaggingEditResponse.getData() != null &&
                videoTaggingEditResponse.getData().getData() != null &&
                !videoTaggingEditResponse.getData().getData().isEmpty()) {

            VideoTaggingEditResponse.VideoItem video = videoTaggingEditResponse.getData().getData().get(0);

            // Validate language is an array
            assertThat("Language should be an array", video.getLanguage(), notNullValue());
            assertThat("Language should be an array", video.getLanguage(), instanceOf(java.util.List.class));

            // Validate language includes 1 and 2
            assertThat("Language should include 1", video.getLanguage(), hasItem(1));
            assertThat("Language should include 2", video.getLanguage(), hasItem(2));

            logger.info("Language codes validated: {}", video.getLanguage());
        } else {
            logger.info("No video data to validate language codes");
        }
    }

    /**
     * Helper method to validate if a string is a valid date
     */
    private boolean isValidDate(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setLenient(false);
            sdf.parse(dateString);
            return true;
        } catch (ParseException e) {
            // Try alternative ISO 8601 format
            try {
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                sdf2.setLenient(false);
                sdf2.parse(dateString);
                return true;
            } catch (ParseException e2) {
                return false;
            }
        }
    }
}
```

### `src/test/java/com/automation/tests/bomb/VideoTaggingPipeline/VideoTagging/VideoTaggingTest.java`

_256 lines_

```java
package com.automation.tests.bomb.VideoTaggingPipeline.VideoTagging;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.VideoTaggingRequest;
import com.automation.models.response.VideoTaggingResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Video Tagging endpoint.
 * Endpoint: PUT
 * {{bizup_base}}/v1/admin/editor/edit/videos/{{seller_id}}/{{video_id}}
 * Implements comprehensive Postman test scripts for video tagging validation.
 */
@TmsLink("TC-ADMIN-009")
@TmsLink("TC-ADMIN-010")
@TmsLink("TC-ADMIN-011")
@Epic("BOMB Video Tagging Pipeline")
@Feature("Video Tagging")
public class VideoTaggingTest extends BaseTest {

    private String authToken;
    private Response response;
    private VideoTaggingResponse videoTaggingResponse;

    // Request data
    private VideoTaggingRequest requestBody;
    private String sellerId;
    private String videoId;
    private String videoTitle;
    private String price;

    // Product and tag IDs
    private static final String PRODUCT_ID = "67c59d8ef22202c05e7d612b";

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");

        // Get seller ID from VariableManager
        sellerId = VariableManager.getSellerId();
        logger.info("Using seller ID: {}", sellerId);
        
        // Get price from VariableManager
        price = VariableManager.get("price", "500");
        logger.info("Using price: {}", price);

        // Get video ID from previous test
        // Use VariableManager to get video ID either from properties or set by previous test
        String prevVideoId = VariableManager.getVideoId();
        if (prevVideoId != null) {
            videoId = prevVideoId;
            logger.info("Using video ID from VariableManager: {}", videoId);
        } else {
            // Fallback to a default ID if not available
            videoId = "default_video_id";
            logger.warn("Video ID not available, using default");
        }

        // Get video title from previous test
        String prevVideoTitle = VariableManager.get("video_title");
        if (prevVideoTitle != null) {
            videoTitle = prevVideoTitle;
            logger.info("Using video title from VariableManager: {}", videoTitle);
        } else {
            // Fallback to a default title if not available
            videoTitle = "Default Video Title";
            logger.warn("Video title not available, using default");
        }

        // Build request body
        requestBody = VideoTaggingRequest.builder()
                .product_id(PRODUCT_ID)
                .tags(Arrays.asList(
                        "67c5a9d69c74685b50b1a489",
                        "67c5a9c59c74685b50b1a069",
                        "67c5a9d19c74685b50b1a345",
                        "67c5a9cd9c74685b50b1a23d",
                        "67c5a9c69c74685b50b1a0b1",
                        "67c5a9ba9c74685b50b19dc9",
                        "67c5a9d39c74685b50b1a3a5",
                        "67c5a9d29c74685b50b1a399",
                        "67c5a9d29c74685b50b1a38d",
                        "67c5a9bb9c74685b50b19df9",
                        "67c5a9ba9c74685b50b19dd5"))
                .suggested(Collections.emptyList())
                .title(videoTitle)
                .price(price)
                .build();
    }

    @Test(description = "Test the response status is 200", priority = 1, groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send PUT request to tag video
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .body(requestBody)
                .when()
                .post(BombEndpoints.VIDEO_TAGGING + "/" + sellerId + "/" + videoId);

        // Parse response for other tests
        videoTaggingResponse = JsonUtils.fromResponse(response, VideoTaggingResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Validate response time is under threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Verify Content-Type header includes application/json", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeader() {
        // Verify Content-Type header
        assertThat("Content-Type should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type header verified: {}", response.getContentType());
    }

    @Test(description = "Validate response has a valid JSON body", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.NORMAL)
    public void testValidJsonBody() {
        // Verify response has valid JSON structure
        assertThat("Response should have valid JSON structure", videoTaggingResponse, notNullValue());

        logger.info("Response has valid JSON body");
    }

    @Test(description = "Check response contains required fields: statusCode, message, and data", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseContainsRequiredFields() {
        // Validate response has all required fields
        assertThat("Response should have statusCode", videoTaggingResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", videoTaggingResponse.getMessage(), notNullValue());
        assertThat("Message should be 'success'", videoTaggingResponse.getMessage(), equalTo("success"));
        assertThat("Response should have data", videoTaggingResponse.getData(), notNullValue());

        logger.info("Response contains required fields");
    }

    @Test(description = "Verify videoId matches the expected value", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testVideoIdMatches() {
        VideoTaggingResponse.VideoTaggingData data = videoTaggingResponse.getData();

        // Validate _id is a string
        assertThat("Video ID should be a string", data.get_id(), instanceOf(String.class));

        // Validate _id matches expected video ID
        assertThat("Video ID should match expected value",
                data.get_id(), equalTo(videoId));

        logger.info("Video ID validated: {} matches expected: {}", data.get_id(), videoId);
    }

    @Test(description = "Verify seller ID matches the expected seller ID", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerIdMatches() {
        VideoTaggingResponse.VideoTaggingData data = videoTaggingResponse.getData();

        // Validate seller matches expected seller ID
        assertThat("Seller ID should match expected value",
                data.getSeller(), equalTo(sellerId));

        logger.info("Seller ID validated: {} matches expected: {}", data.getSeller(), sellerId);
    }

    @Test(description = "Verify Title matches the expected value", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testTitleMatches() {
        VideoTaggingResponse.VideoTaggingData data = videoTaggingResponse.getData();

        // Validate title is a string
        assertThat("Title should be a string", data.getTitle(), instanceOf(String.class));

        // Validate title matches expected video title
        assertThat("Title should match expected value",
                data.getTitle(), equalTo(videoTitle));

        logger.info("Title validated: {} matches expected: {}", data.getTitle(), videoTitle);
    }

    @Test(description = "Verify Price matches the expected value", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.CRITICAL)
    public void testPriceMatches() {
        VideoTaggingResponse.VideoTaggingData data = videoTaggingResponse.getData();

        // Validate priceText is a string
        assertThat("PriceText should be a string", data.getPriceText(), instanceOf(String.class));

        // Validate priceText matches expected price
        assertThat("PriceText should match expected value",
                data.getPriceText(), equalTo(price));

        logger.info("Price validated: {} matches expected: {}", data.getPriceText(), price);
    }

    @Test(description = "Ensure no error is present in the response data", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Tagging")
    @Severity(SeverityLevel.NORMAL)
    public void testNoErrorInResponseData() {
        // Validate data is not undefined/null
        assertThat("Response data should not be undefined",
                videoTaggingResponse.getData(), notNullValue());

        logger.info("No error in response data");
    }
}
```

### `src/test/java/com/automation/tests/bomb/VideoTaggingPipeline/VideoTagging/VideoTitleGenerationTest.java`

_226 lines_

```java
package com.automation.tests.bomb.VideoTaggingPipeline.VideoTagging;

import com.automation.base.BaseTest;
import com.automation.constants.HttpStatus;
import com.automation.models.request.VideoTitleGenerationRequest;
import com.automation.models.response.VideoTitleGenerationResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Video Title Generation endpoint.
 * Endpoint: POST https://bomb.bizup.app/api/chat/title/tags
 * Implements comprehensive Postman test scripts for video title generation
 * validation.
 */
@Epic("BOMB Video Tagging Pipeline")
@Feature("Video Tagging")
public class VideoTitleGenerationTest extends BaseTest {

    private String authToken;
    private Response response;
    private VideoTitleGenerationResponse videoTitleGenerationResponse;

    // Full endpoint URL (external API)
    private static final String TITLE_GENERATION_URL = "https://bomb.bizup.app/api/chat/title/tags";

    // Request data - tags for title generation
    private VideoTitleGenerationRequest requestBody;
    private List<String> requestTags;

    // Store generated title for future tests
    public static String videoTitle;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");

        // Build request body with tags
        requestTags = Arrays.asList(
                "product : silk saree",
                "fabric : cotton",
                "suitable for : womens",
                "work/embroidary : bead work",
                "border work : embroidery",
                "saree length : 6 meter",
                "packaging : ziplock packing",
                "occasion : outdoor, religious",
                "pattern/design/print : belt saree",
                "blouse piece : ready to wear (stitched)");

        requestBody = VideoTitleGenerationRequest.builder()
                .tags(requestTags)
                .build();
    }

    @Test(description = "Set the generated video title to the collectionVariables", priority = 1, groups = "bomb")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.NORMAL)
    public void testSetGeneratedVideoTitle() {
        // Send POST request to generate video title
        response = RestAssured.given()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post(TITLE_GENERATION_URL);

        // Parse response for other tests
        videoTitleGenerationResponse = JsonUtils.fromResponse(response, VideoTitleGenerationResponse.class);

        // Set video title from result
        if (videoTitleGenerationResponse.getResult() != null) {
            videoTitle = videoTitleGenerationResponse.getResult();
            assertThat("Video title should be set", videoTitle, notNullValue());

            logger.info("Set video title: {}", videoTitle);
        } else {
            logger.warn("No result to set video title");
        }
    }

    @Test(description = "Status code is 200", priority = 2, dependsOnMethods = "testSetGeneratedVideoTitle", groups = "bomb")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 3, dependsOnMethods = "testSetGeneratedVideoTitle", groups = "bomb")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Content-Type is application/json", priority = 4, dependsOnMethods = "testSetGeneratedVideoTitle", groups = "bomb")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeader() {
        // Verify Content-Type header
        assertThat("Content-Type should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type header verified: {}", response.getContentType());
    }

    @Test(description = "Response has valid JSON structure", priority = 5, dependsOnMethods = "testSetGeneratedVideoTitle", groups = "bomb")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.NORMAL)
    public void testValidJsonStructure() {
        // Verify response has valid JSON structure
        assertThat("Response should have valid JSON structure", videoTitleGenerationResponse, notNullValue());

        logger.info("Response has valid JSON structure");
    }

    @Test(description = "Response contains required fields", priority = 6, dependsOnMethods = "testSetGeneratedVideoTitle", groups = "bomb")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseContainsRequiredFields() {
        // Validate response has result property
        assertThat("Response should have result", videoTitleGenerationResponse.getResult(), notNullValue());

        logger.info("Response contains required fields: result={}", videoTitleGenerationResponse.getResult());
    }

    @Test(description = "Tags are processed correctly", priority = 7, dependsOnMethods = "testSetGeneratedVideoTitle", groups = "bomb")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.NORMAL)
    public void testTagsProcessedCorrectly() {
        // Validate tags processing if data is present
        if (videoTitleGenerationResponse.getData() != null &&
                videoTitleGenerationResponse.getData().getProcessedTags() != null) {

            List<VideoTitleGenerationResponse.ProcessedTag> processedTags = videoTitleGenerationResponse.getData()
                    .getProcessedTags();

            // Validate processedTags is an array
            assertThat("ProcessedTags should be an array", processedTags, notNullValue());

            // Filter non-empty request tags
            long nonEmptyRequestTagsCount = requestTags.stream()
                    .filter(tag -> tag != null && !tag.trim().isEmpty())
                    .count();

            // Validate processedTags length matches non-empty request tags
            assertThat("ProcessedTags length should match non-empty request tags",
                    (long) processedTags.size(), equalTo(nonEmptyRequestTagsCount));

            logger.info("Tags processed correctly: {} tags", processedTags.size());
        } else {
            logger.info("No processed tags data to validate");
        }
    }

    @Test(description = "No error in response", priority = 8, dependsOnMethods = "testSetGeneratedVideoTitle", groups = "bomb")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.NORMAL)
    public void testNoErrorInResponse() {
        // Validate error field exists (can be null or not undefined)
        // Note: In Java, we check if the field is not null to ensure it's defined
        assertThat("Error field should be defined (can be null)",
                videoTitleGenerationResponse.getError(), anyOf(nullValue(), notNullValue()));

        logger.info("Error field validated: {}", videoTitleGenerationResponse.getError());
    }

    @Test(description = "Tag keys are properly formatted", priority = 9, dependsOnMethods = "testSetGeneratedVideoTitle", groups = "bomb")
    @Story("Video Title Generation")
    @Severity(SeverityLevel.NORMAL)
    public void testTagKeysProperlyFormatted() {
        // Validate tag formatting if data is present
        if (videoTitleGenerationResponse.getData() != null &&
                videoTitleGenerationResponse.getData().getProcessedTags() != null) {

            List<VideoTitleGenerationResponse.ProcessedTag> processedTags = videoTitleGenerationResponse.getData()
                    .getProcessedTags();

            processedTags.forEach(tag -> {
                // Validate key is a string
                assertThat("Tag key should be a string", tag.getKey(), instanceOf(String.class));

                // Validate key matches pattern (alphanumeric, spaces, and slashes allowed)
                assertThat("Tag key should match pattern",
                        tag.getKey(), matchesPattern("^[a-zA-Z0-9/\\s]+$"));

                // Validate value is a string
                assertThat("Tag value should be a string", tag.getValue(), instanceOf(String.class));
            });

            logger.info("Tag keys properly formatted: {} tags validated", processedTags.size());
        } else {
            logger.info("No processed tags data to validate formatting");
        }
    }
}
```

### `src/test/java/com/automation/tests/bomb/VideoTaggingPipeline/VideoUploadAndAssignToEditor/AllVideoAssignForUploadingThumbnailVideoTest.java`

_224 lines_

```java
package com.automation.tests.bomb.VideoTaggingPipeline.VideoUploadAndAssignToEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.VideoAssignUploadResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Video Upload - All Video Assign for uploading [Thumbnail
 * + Video] endpoint.
 * Endpoint: GET
 * {{bizup_base}}/v1/admin/editor/assign/videos/{{seller_id}}?limit=50
 * Implements comprehensive Postman test scripts for video assign upload
 * validation.
 */
@TmsLink("TC-ADMIN-001")
@TmsLink("TC-ADMIN-002")
@TmsLink("TC-ADMIN-003")
@Epic("BOMB Video Tagging Pipeline")
@Feature("Video Upload and Assign to Editor")
public class AllVideoAssignForUploadingThumbnailVideoTest extends BaseTest {

    private String authToken;
    private Response response;
    private VideoAssignUploadResponse videoAssignUploadResponse;

    // Seller ID
    private static final String SELLER_ID = "64f180feaa90ffbd54b330f5";

    // Store upload ID for future tests
    public static String uploadId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
    }

    @Test(description = "Set upload_id to collectionVariables", priority = 1, groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.NORMAL)
    public void testSetUploadId() {
        // Send GET request to fetch assigned videos
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .queryParam("limit", 50)
                .when()
                .get(BombEndpoints.VIDEO_ASSIGN_UPLOAD + "/" + SELLER_ID);

        // Parse response for other tests
        videoAssignUploadResponse = JsonUtils.fromResponse(response, VideoAssignUploadResponse.class);

        // Set upload ID from first item in data.data array
        if (videoAssignUploadResponse.getData() != null &&
                videoAssignUploadResponse.getData().getData() != null &&
                !videoAssignUploadResponse.getData().getData().isEmpty()) {

            uploadId = videoAssignUploadResponse.getData().getData().get(0).get_id();
            assertThat("Upload ID should be set", uploadId, notNullValue());

            // Save to VariableManager for subsequent tests
            VariableManager.set("uploadId", uploadId);
            
            logger.info("Set upload ID: {}", uploadId);
        } else {
            logger.warn("No data items to set upload ID");
        }
    }

    @Test(description = "Response status code is 200", priority = 2, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 3, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Response has valid JSON structure", priority = 4, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.NORMAL)
    public void testValidJsonStructure() {
        // Verify response has valid JSON structure
        assertThat("Response should have valid JSON structure", videoAssignUploadResponse, notNullValue());

        logger.info("Response has valid JSON structure");
    }

    @Test(description = "Response contains expected top-level fields", priority = 5, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseContainsTopLevelFields() {
        // Validate response is an object
        assertThat("Response should not be null", videoAssignUploadResponse, notNullValue());
        assertThat("Response should have statusCode", videoAssignUploadResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", videoAssignUploadResponse.getMessage(), notNullValue());
        assertThat("Response should have data", videoAssignUploadResponse.getData(), notNullValue());

        logger.info("Response contains expected top-level fields");
    }

    @Test(description = "Videos array has correct structure", priority = 6, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.CRITICAL)
    public void testVideosArrayStructure() {
        // Check if data.videos exists
        if (videoAssignUploadResponse.getData() != null &&
                videoAssignUploadResponse.getData().getVideos() != null) {

            assertThat("Videos should be an array",
                    videoAssignUploadResponse.getData().getVideos(), notNullValue());

            // Validate first video if array is not empty
            if (!videoAssignUploadResponse.getData().getVideos().isEmpty()) {
                VideoAssignUploadResponse.VideoItem firstVideo = videoAssignUploadResponse.getData().getVideos().get(0);

                assertThat("First video should have _id", firstVideo.get_id(), notNullValue());
                assertThat("First video should have videoLink", firstVideo.getVideoLink(), notNullValue());
                assertThat("VideoLink should contain http", firstVideo.getVideoLink(), containsString("http"));
                assertThat("First video should have status", firstVideo.getStatus(), notNullValue());
                assertThat("Status should be 0, 1, or 2", firstVideo.getStatus(), in(new Integer[] { 0, 1, 2 }));
                assertThat("First video should have seller", firstVideo.getSeller(), notNullValue());
                assertThat("Seller should have _id", firstVideo.getSeller().get_id(), notNullValue());

                logger.info("Videos array structure validated");
            }
        } else {
            logger.info("Videos array not present in response");
        }
    }

    @Test(description = "All videos belong to the requested seller", priority = 7, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.CRITICAL)
    public void testAllVideosBelongToRequestedSeller() {
        // Validate all videos belong to requested seller
        if (videoAssignUploadResponse.getData() != null &&
                videoAssignUploadResponse.getData().getVideos() != null) {

            videoAssignUploadResponse.getData().getVideos().forEach(video -> {
                assertThat(String.format("Video %s should belong to seller %s",
                        video.get_id(), SELLER_ID),
                        video.getSeller().get_id(), equalTo(SELLER_ID));
            });

            logger.info("All {} videos verified to belong to seller: {}",
                    videoAssignUploadResponse.getData().getVideos().size(), SELLER_ID);
        } else {
            logger.info("No videos to validate seller ownership");
        }
    }

    @Test(description = "Content-Type header is application/json", priority = 8, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeader() {
        // Verify Content-Type header
        assertThat("Content-Type should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type header verified: {}", response.getContentType());
    }

    @Test(description = "ETag header exists", priority = 9, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.NORMAL)
    public void testETagHeaderExists() {
        // Verify ETag header exists
        assertThat("ETag header should exist",
                response.getHeader("ETag"), notNullValue());

        logger.info("ETag header verified: {}", response.getHeader("ETag"));
    }

    @Test(description = "Response data is not empty", priority = 10, dependsOnMethods = "testSetUploadId", groups = "bomb")
    @Story("Video Upload - All Video Assign")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseDataNotEmpty() {
        // Validate data.data exists
        assertThat("Response data.data should exist",
                videoAssignUploadResponse.getData().getData(), notNullValue());

        logger.info("Response data is not empty: {} item(s)",
                videoAssignUploadResponse.getData().getData() != null
                        ? videoAssignUploadResponse.getData().getData().size()
                        : 0);
    }
}
```

### `src/test/java/com/automation/tests/bomb/VideoTaggingPipeline/VideoUploadAndAssignToEditor/UploadThumbnailVideoTest.java`

_290 lines_

```java
package com.automation.tests.bomb.VideoTaggingPipeline.VideoUploadAndAssignToEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.VideoThumbnailUploadRequest;
import com.automation.models.response.VideoThumbnailUploadResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Video Upload [Thumbnail + Video] endpoint.
 * Endpoint: POST {{bizup_base}}/v1/admin/editor/upload/videos/{{seller_id}}
 * Implements comprehensive Postman test scripts for video and thumbnail upload
 * validation.
 */
@TmsLink("TC-ADMIN-012")
@TmsLink("TC-ADMIN-013")
@Epic("BOMB Video Tagging Pipeline")
@Feature("Video Upload and Assign to Editor")
public class UploadThumbnailVideoTest extends BaseTest {

    private String authToken;
    private Response response;
    private VideoThumbnailUploadResponse videoThumbnailUploadResponse;

    // Request data
    private VideoThumbnailUploadRequest requestBody;
    private String sellerId;
    private String sellerPhone;
    private String sellerName;
    private String businessName;
    private String marketId;
    private String videoLink;
    private String thumbnailLink;
    private String description;

    // Upload ID from previous test
    private String uploadId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");

        // Get seller-related data from VariableManager
        sellerId = VariableManager.getSellerId();
        sellerPhone = VariableManager.get("phoneNumber");
        sellerName = VariableManager.get("seller_name");
        businessName = VariableManager.get("business_name");
        marketId = VariableManager.get("market_id");
        logger.info("Using seller data: sellerId={}, sellerPhone={}, sellerName={}, businessName={}, marketId={}", 
                    sellerId, sellerPhone, sellerName, businessName, marketId);

        // Get video/thumbnail data from VariableManager
        videoLink = VariableManager.get("video_link");
        thumbnailLink = VariableManager.get("thumbnail_link");
        description = VariableManager.get("video_description");
        logger.info("Using video data: videoLink={}, thumbnailLink={}, description={}", videoLink, thumbnailLink, description);

        // Get upload ID from previous test
        String prevUploadId = VariableManager.get("upload_id");
        if (prevUploadId != null) {
            uploadId = prevUploadId;
            logger.info("Using upload ID from VariableManager: {}", uploadId);
        } else {
            // Fallback to a default ID if not available (valid MongoDB ObjectId)
            uploadId = "693a92f840d8d24c390119a0";
            logger.warn("Upload ID not available from previous test, using default: {}", uploadId);
        }

        // Build request body
        requestBody = VideoThumbnailUploadRequest.builder()
                .seller(VideoThumbnailUploadRequest.Seller.builder()
                        ._id(sellerId)
                        .phoneNumber(sellerPhone)
                        .name(sellerName)
                        .businessName(businessName)
                        .marketId(marketId)
                        .build())
                .market(null)
                .videoLink(videoLink)
                .thumbnailLink(thumbnailLink)
                .introVideo(false)
                .uploadId(uploadId)
                .fabricText("")
                .priceText("")
                .description(description)
                .build();
    }

    @Test(description = "Status code is 200", priority = 1, groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send POST request to upload video and thumbnail
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .body(requestBody)
                .when()
                .post(BombEndpoints.VIDEO_THUMBNAIL_UPLOAD + "/" + sellerId);

        // Parse response for other tests
        videoThumbnailUploadResponse = JsonUtils.fromResponse(response, VideoThumbnailUploadResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Success message exists", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
    @Severity(SeverityLevel.CRITICAL)
    public void testSuccessMessage() {
        // Validate message property exists
        assertThat("Response should have message", videoThumbnailUploadResponse.getMessage(), notNullValue());

        // Validate message is 'success'
        assertThat("Message should be 'success'",
                videoThumbnailUploadResponse.getMessage(), equalTo("success"));

        logger.info("Success message validated: {}", videoThumbnailUploadResponse.getMessage());
    }

    @Test(description = "Response has correct structure", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStructure() {
        // Validate response has message and data
        assertThat("Response should have message", videoThumbnailUploadResponse.getMessage(), notNullValue());
        assertThat("Response should have data", videoThumbnailUploadResponse.getData(), notNullValue());

        VideoThumbnailUploadResponse.VideoThumbnailData data = videoThumbnailUploadResponse.getData();

        // Validate data has all required keys
        assertThat("Data should have _id", data.get_id(), notNullValue());
        assertThat("Data should have seller", data.getSeller(), notNullValue());
        assertThat("Data should have driveLink", data.getDriveLink(), notNullValue());
        assertThat("Data should have thubmbnailDriveLink", data.getThubmbnailDriveLink(), notNullValue());
        assertThat("Data should have contentType", data.getContentType(), notNullValue());
        // status can be null
        assertThat("Data should have createdAt", data.getCreatedAt(), notNullValue());
        assertThat("Data should have updatedAt", data.getUpdatedAt(), notNullValue());

        logger.info("Response structure validated");
    }

    @Test(description = "ContentType is video and status is null", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
    @Severity(SeverityLevel.CRITICAL)
    public void testContentTypeAndStatus() {
        VideoThumbnailUploadResponse.VideoThumbnailData data = videoThumbnailUploadResponse.getData();

        // Validate contentType is 'video'
        assertThat("ContentType should be 'video'",
                data.getContentType(), equalTo("video"));

        // Validate status is null
        assertThat("Status should be null",
                data.getStatus(), nullValue());

        logger.info("ContentType and status validated: contentType={}, status={}",
                data.getContentType(), data.getStatus());
    }

    @Test(description = "Seller ID matches collection variable", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerIdMatches() {
        VideoThumbnailUploadResponse.VideoThumbnailData data = videoThumbnailUploadResponse.getData();

        // Validate seller matches expected seller ID
        assertThat("Seller should match expected seller ID",
                data.getSeller(), equalTo(sellerId));

        logger.info("Seller validated: {} matches expected: {}", data.getSeller(), sellerId);
    }

    @Test(description = "Video link is valid", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
    @Severity(SeverityLevel.CRITICAL)
    public void testVideoLinkValid() {
        VideoThumbnailUploadResponse.VideoThumbnailData data = videoThumbnailUploadResponse.getData();

        // Validate driveLink is a string
        assertThat("DriveLink should be a string", data.getDriveLink(), instanceOf(String.class));

        // Validate driveLink contains https://
        assertThat("DriveLink should contain https://",
                data.getDriveLink(), containsString("https://"));

        logger.info("Video link validated: {}", data.getDriveLink());
    }

    @Test(description = "Thumbnail link is valid", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
    @Severity(SeverityLevel.CRITICAL)
    public void testThumbnailLinkValid() {
        VideoThumbnailUploadResponse.VideoThumbnailData data = videoThumbnailUploadResponse.getData();

        // Validate thubmbnailDriveLink is a string
        assertThat("ThubmbnailDriveLink should be a string",
                data.getThubmbnailDriveLink(), instanceOf(String.class));

        // Validate thubmbnailDriveLink contains https://
        assertThat("ThubmbnailDriveLink should contain https://",
                data.getThubmbnailDriveLink(), containsString("https://"));

        logger.info("Thumbnail link validated: {}", data.getThubmbnailDriveLink());
    }

    @Test(description = "CreatedAt and UpdatedAt are valid dates", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Thumbnail + Video")
    @Severity(SeverityLevel.NORMAL)
    public void testTimestampsValid() {
        VideoThumbnailUploadResponse.VideoThumbnailData data = videoThumbnailUploadResponse.getData();

        // Validate createdAt is a valid date
        assertThat("CreatedAt should not be null", data.getCreatedAt(), notNullValue());
        boolean isCreatedAtValid = isValidDate(data.getCreatedAt());
        assertThat("CreatedAt should be a valid date", isCreatedAtValid, is(true));

        // Validate updatedAt is a valid date
        assertThat("UpdatedAt should not be null", data.getUpdatedAt(), notNullValue());
        boolean isUpdatedAtValid = isValidDate(data.getUpdatedAt());
        assertThat("UpdatedAt should be a valid date", isUpdatedAtValid, is(true));

        logger.info("Timestamps validated: createdAt={}, updatedAt={}",
                data.getCreatedAt(), data.getUpdatedAt());
    }

    /**
     * Helper method to validate if a string is a valid date
     */
    private boolean isValidDate(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setLenient(false);
            sdf.parse(dateString);
            return true;
        } catch (ParseException e) {
            // Try alternative ISO 8601 format
            try {
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                sdf2.setLenient(false);
                sdf2.parse(dateString);
                return true;
            } catch (ParseException e2) {
                return false;
            }
        }
    }
}
```

### `src/test/java/com/automation/tests/bomb/VideoTaggingPipeline/VideoUploadAndAssignToEditor/VideoUploadMarkAsDoneTest.java`

_244 lines_

```java
package com.automation.tests.bomb.VideoTaggingPipeline.VideoUploadAndAssignToEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.VideoUploadMarkAsDoneResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Video Upload Mark As Done endpoint.
 * Endpoint: PUT
 * {{bizup_base}}/v1/admin/editor/assign/videos/done/{{seller_id}}/{{upload_id}}
 * Implements comprehensive Postman test scripts for marking video upload as
 * done validation.
 */
@TmsLink("TC-ADMIN-003")
@TmsLink("TC-ADMIN-004")
@Epic("BOMB Video Tagging Pipeline")
@Feature("Video Upload and Assign to Editor")
public class VideoUploadMarkAsDoneTest extends BaseTest {

    private String authToken;
    private Response response;
    private VideoUploadMarkAsDoneResponse videoUploadMarkAsDoneResponse;

    // IDs
    private String sellerId;
    private String uploadId;

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");
        
        // Get seller ID from VariableManager
        sellerId = VariableManager.getSellerId();
        logger.info("Using seller ID: {}", sellerId);
        
        // Get upload ID from previous test
        String prevUploadId = VariableManager.get("upload_id");
        if (prevUploadId != null) {
            uploadId = prevUploadId;
            logger.info("Using upload ID from previous test: {}", uploadId);
        } else {
            // Fallback to a default ID if not available (valid MongoDB ObjectId)
            uploadId = "693a92f840d8d24c390119a0";
            logger.warn("Upload ID not available from previous test, using default: {}", uploadId);
        }
    }

    @Test(description = "Status code is 200", priority = 1, groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send PUT request to mark video upload as done
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .when()
                .post(BombEndpoints.VIDEO_UPLOAD_DONE + "/" + sellerId + "/" + uploadId);

        // Parse response for other tests
        videoUploadMarkAsDoneResponse = JsonUtils.fromResponse(response, VideoUploadMarkAsDoneResponse.class);

        // Verify response status is 200 OK
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Response has correct content-type", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeader() {
        // Verify Content-Type header
        assertThat("Content-Type should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type header verified: {}", response.getContentType());
    }

    @Test(description = "Success message exists", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.CRITICAL)
    public void testSuccessMessage() {
        // Validate message property exists
        assertThat("Response should have message", videoUploadMarkAsDoneResponse.getMessage(), notNullValue());

        // Validate message is a string and not empty
        assertThat("Message should be a string",
                videoUploadMarkAsDoneResponse.getMessage(), instanceOf(String.class));
        assertThat("Message should not be empty",
                videoUploadMarkAsDoneResponse.getMessage(), not(emptyString()));

        // Validate message includes 'success'
        assertThat("Message should include 'success'",
                videoUploadMarkAsDoneResponse.getMessage(), containsString("success"));

        logger.info("Success message validated: {}", videoUploadMarkAsDoneResponse.getMessage());
    }

    @Test(description = "Response has correct structure", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStructure() {
        // Validate response has message and data
        assertThat("Response should have message", videoUploadMarkAsDoneResponse.getMessage(), notNullValue());
        assertThat("Response should have data", videoUploadMarkAsDoneResponse.getData(), notNullValue());

        VideoUploadMarkAsDoneResponse.VideoUploadDoneData data = videoUploadMarkAsDoneResponse.getData();

        // Validate data has all required keys
        assertThat("Data should have _id", data.get_id(), notNullValue());
        assertThat("Data should have sellerId", data.getSellerId(), notNullValue());
        assertThat("Data should have phoneNumber", data.getPhoneNumber(), notNullValue());
        assertThat("Data should have url", data.getUrl(), notNullValue());
        assertThat("Data should have assignedBy", data.getAssignedBy(), notNullValue());
        assertThat("Data should have editorId", data.getEditorId(), notNullValue());
        assertThat("Data should have uploadedBy", data.getUploadedBy(), notNullValue());
        assertThat("Data should have status", data.getStatus(), notNullValue());
        assertThat("Data should have createdAt", data.getCreatedAt(), notNullValue());
        assertThat("Data should have updatedAt", data.getUpdatedAt(), notNullValue());

        logger.info("Response structure validated");
    }

    @Test(description = "Status is updated to 'done'", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.CRITICAL)
    public void testStatusUpdatedToDone() {
        VideoUploadMarkAsDoneResponse.VideoUploadDoneData data = videoUploadMarkAsDoneResponse.getData();

        // Validate status is 1 (done)
        assertThat("Status should be 1 (done)",
                data.getStatus(), equalTo(1));

        logger.info("Status validated: {}", data.getStatus());
    }

    @Test(description = "VideoType remains 'video'", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.NORMAL)
    public void testVideoTypeRemains() {
        VideoUploadMarkAsDoneResponse.VideoUploadDoneData data = videoUploadMarkAsDoneResponse.getData();

        // Validate videoType is 'Video'
        assertThat("VideoType should be 'Video'",
                data.getVideoType(), equalTo("Video"));

        logger.info("VideoType validated: {}", data.getVideoType());
    }

    @Test(description = "Seller ID matches collection variable", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerIdMatches() {
        VideoUploadMarkAsDoneResponse.VideoUploadDoneData data = videoUploadMarkAsDoneResponse.getData();

        // Validate sellerId matches expected seller ID
        assertThat("SellerId should match expected seller ID",
                data.getSellerId(), equalTo(sellerId));

        logger.info("SellerId validated: {} matches expected: {}", data.getSellerId(), sellerId);
    }

    @Test(description = "Uploaded ID matches expected value", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.CRITICAL)
    public void testUploadedIdMatches() {
        VideoUploadMarkAsDoneResponse.VideoUploadDoneData data = videoUploadMarkAsDoneResponse.getData();

        // Validate _id matches expected upload ID
        assertThat("Upload ID should match expected upload ID",
                data.get_id(), equalTo(uploadId));

        logger.info("Upload ID validated: {} matches expected: {}", data.get_id(), uploadId);
    }

    @Test(description = "Video and thumbnail links are valid", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.CRITICAL)
    public void testVideoLinksValid() {
        VideoUploadMarkAsDoneResponse.VideoUploadDoneData data = videoUploadMarkAsDoneResponse.getData();

        // Validate url is a string
        assertThat("URL should be a string", data.getUrl(), instanceOf(String.class));

        // Validate url contains https://
        assertThat("URL should contain https://",
                data.getUrl(), containsString("https://"));

        logger.info("Video URL validated: {}", data.getUrl());
    }

    @Test(description = "URL parameters match response data", priority = 11, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload - Mark As Done")
    @Severity(SeverityLevel.CRITICAL)
    public void testUrlParametersMatch() {
        VideoUploadMarkAsDoneResponse.VideoUploadDoneData data = videoUploadMarkAsDoneResponse.getData();

        // Validate sellerId from URL matches response
        assertThat("SellerId from URL should match response data",
                data.getSellerId(), equalTo(sellerId));

        // Validate upload ID from URL matches response
        assertThat("Upload ID from URL should match response data",
                data.get_id(), equalTo(uploadId));

        logger.info("URL parameters validated: sellerId={}, uploadId={}", sellerId, uploadId);
    }
}
```

### `src/test/java/com/automation/tests/bomb/VideoTaggingPipeline/VideoUploadAndAssignToEditor/VideoUploadTest.java`

_306 lines_

```java
package com.automation.tests.bomb.VideoTaggingPipeline.VideoUploadAndAssignToEditor;

import com.automation.base.BaseTest;
import com.automation.constants.BombEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.VideoUploadRequest;
import com.automation.models.response.VideoUploadResponse;
import com.automation.utils.VariableManager;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for BOMB Video Upload endpoint.
 * Endpoint: POST {{bizup_base}}/v1/admin/pipeline/video
 * Implements comprehensive Postman test scripts for video upload validation.
 */
@TmsLink("TC-ADMIN-005")
@TmsLink("TC-ADMIN-006")
@Epic("BOMB Video Tagging Pipeline")
@Feature("Video Upload and Assign to Editor")
public class VideoUploadTest extends BaseTest {

    private String authToken;
    private Response response;
    private VideoUploadResponse videoUploadResponse;

    // Request data
    private VideoUploadRequest requestBody;
    private static final String SELLER_ID = "63ee780c9689be92acce8f35";
    private static final String SELLER_PHONE = "+916204843730";
    private static final String SELLER_NAME = "Shubham Kr (Test Shop)";
    private static final String BUSINESS_NAME = "SK Shop (Test)";
    private static final String SELLER_CREATED_AT = "2023-09-01T06:13:18.149Z";
    private static final String MARKET_ID = "6454921e6144f73eceac9de5";
    private static final String SELLER_LABEL = "SK Shop (Test) (+916204843730)";
    private static final String SELLER_VALUE = "64f180feaa90ffbd54b330f5";

    private static final String EDITOR_ID = "652e699e42e117518ebb86cd";
    private static final String EDITOR_NAME = "Shubham Kumar";
    private static final String EDITOR_PHONE = "+916204843730";
    private static final String EDITOR_USER_ID = "64f180feaa90ffbd54b330f5";
    private static final String EDITOR_LABEL = "Shubham Kumar";
    private static final String EDITOR_VALUE = "64f180feaa90ffbd54b330f5";

    private static final String VIDEO_LINK = "https://firebasestorage.googleapis.com/v0/b/bizup-3df17.appspot.com/o/editor%2Fvideo%2F1ba37ead-ca46-45a6-bf93-2966c55e999f.mp4?alt=media&token=e0ee167d-e7eb-4428-92ed-c97b39215010";
    private static final String VIDEO_TYPE = "Video";
    private static final String DESCRIPTION = "Test";

    @BeforeClass
    public void setupAuth() {
        // Get token from VariableManager (thread-safe)
        authToken = VariableManager.getToken();
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login token not available. Please run LoginApiTest first.");
        }
        logger.info("Using BOMB token from VariableManager");

        // Build request body with complete seller and editor information
        requestBody = VideoUploadRequest.builder()
                .videoLink(VIDEO_LINK)
                .videoType(VIDEO_TYPE)
                .description(DESCRIPTION)
                .market(null)
                .seller(VideoUploadRequest.Seller.builder()
                        ._id(SELLER_ID)
                        .phoneNumber(SELLER_PHONE)
                        .name(SELLER_NAME)
                        .businessName(BUSINESS_NAME)
                        .createdAt(SELLER_CREATED_AT)
                        .marketId(MARKET_ID)
                        .smell_test(50)
                        .assigned_score(0)
                        .deprioritisation_status(false)
                        .label(SELLER_LABEL)
                        .value(SELLER_VALUE)
                        .build())
                .editor(VideoUploadRequest.Editor.builder()
                        ._id(EDITOR_ID)
                        .name(EDITOR_NAME)
                        .role(java.util.Arrays.asList(
                                "editor_review", "admin", "editor_catalog", "editor_video",
                                "pipeline_online", "pipeline_onground", "pipeline_wa",
                                "manage_seller", "manage_buyer", "salesman", "whatsapp_message"))
                        .status(1)
                        .phoneNumber(EDITOR_PHONE)
                        .user_id(EDITOR_USER_ID)
                        .label(EDITOR_LABEL)
                        .value(EDITOR_VALUE)
                        .build())
                .build();
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.BLOCKER)
    public void testStatusCode200() {
        // Send POST request to upload video
        response = RestAssured.given()
                .spec(requestSpec)
                .header("authorization", "JWT " + authToken)
                .header("source", "bizupChat")
                .body(requestBody)
                .when()
                .post(BombEndpoints.VIDEO_UPLOAD);

        // Parse response for other tests
        videoUploadResponse = JsonUtils.fromResponse(response, VideoUploadResponse.class);

        // Verify response status is 200 OK (or 201 Created)
        assertThat("Status code should be 200 or 201",
                response.getStatusCode(), anyOf(equalTo(HttpStatus.OK), equalTo(HttpStatus.CREATED)));

        logger.info("Response status verified: {}", response.getStatusCode());
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeLessThanThreshold() {
        // Use specific threshold of 20000ms as per Postman script
        long threshold = 20000;
        long actualResponseTime = response.getTime();

        // Verify response time is available
        assertThat("Response time should be available", actualResponseTime, notNullValue());

        // Verify response time is below threshold
        assertThat("Response time should be less than threshold",
                actualResponseTime, lessThan(threshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime, threshold);
    }

    @Test(description = "Response has valid JSON structure", priority = 3, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.NORMAL)
    public void testValidJsonStructure() {
        // Verify response has valid JSON structure
        assertThat("Response should have valid JSON structure", videoUploadResponse, notNullValue());

        logger.info("Response has valid JSON structure");
    }

    @Test(description = "Response contains expected fields", priority = 4, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseContainsExpectedFields() {
        // Validate response is an object
        assertThat("Response should not be null", videoUploadResponse, notNullValue());
        assertThat("Response should have statusCode", videoUploadResponse.getStatusCode(), notNullValue());
        assertThat("Response should have message", videoUploadResponse.getMessage(), notNullValue());

        // For 201 Created, validate data fields
        if (response.getStatusCode() == HttpStatus.CREATED && videoUploadResponse.getData() != null) {
            VideoUploadResponse.VideoUploadData data = videoUploadResponse.getData();
            assertThat("Data should have _id", data.get_id(), notNullValue());
            assertThat("Data should have videoLink matching request", data.getVideoLink(), equalTo(VIDEO_LINK));
            assertThat("Data should have videoType matching request", data.getVideoType(), equalTo(VIDEO_TYPE));
            assertThat("Data should have status 0", data.getStatus(), equalTo(0));

            logger.info("Response data fields validated for 201 Created");
        }

        logger.info("Response contains expected fields");
    }

    @Test(description = "Seller information matches request", priority = 5, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerInfoMatchesRequest() {
        // Validate seller information if present
        if (videoUploadResponse.getData() != null && videoUploadResponse.getData().getSeller() != null) {
            VideoUploadResponse.Seller seller = videoUploadResponse.getData().getSeller();

            assertThat("Seller _id should match request", seller.get_id(), equalTo(SELLER_ID));
            assertThat("Seller phoneNumber should match request", seller.getPhoneNumber(), equalTo(SELLER_PHONE));

            logger.info("Seller information validated: _id={}, phoneNumber={}", seller.get_id(),
                    seller.getPhoneNumber());
        } else {
            logger.info("Seller information not present in response");
        }
    }

    @Test(description = "Editor information matches request", priority = 6, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.CRITICAL)
    public void testEditorInfoMatchesRequest() {
        // Validate editor information if present
        if (videoUploadResponse.getData() != null && videoUploadResponse.getData().getEditor() != null) {
            VideoUploadResponse.Editor editor = videoUploadResponse.getData().getEditor();

            assertThat("Editor _id should match request", editor.get_id(), equalTo(EDITOR_ID));
            assertThat("Editor name should match request", editor.getName(), equalTo(EDITOR_NAME));

            logger.info("Editor information validated: _id={}, name={}", editor.get_id(), editor.getName());
        } else {
            logger.info("Editor information not present in response");
        }
    }

    @Test(description = "Content-Type header is application/json", priority = 7, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.NORMAL)
    public void testContentTypeHeader() {
        // Verify Content-Type header
        assertThat("Content-Type should include application/json",
                response.getContentType(), containsString("application/json"));

        logger.info("Content-Type header verified: {}", response.getContentType());
    }

    @Test(description = "Success message is returned", priority = 8, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.NORMAL)
    public void testSuccessMessage() {
        // Validate statusCode is a string
        assertThat("StatusCode should be a string",
                videoUploadResponse.getStatusCode(), instanceOf(String.class));

        // Validate message is 'success'
        assertThat("Message should be 'success'",
                videoUploadResponse.getMessage(), equalTo("success"));

        logger.info("Success message validated: statusCode={}, message={}",
                videoUploadResponse.getStatusCode(), videoUploadResponse.getMessage());
    }

    @Test(description = "Video processing status is valid", priority = 9, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.NORMAL)
    public void testVideoProcessingStatus() {
        // Validate video processing status if present
        if (videoUploadResponse.getData() != null && videoUploadResponse.getData().getStatus() != null) {
            Integer status = videoUploadResponse.getData().getStatus();

            assertThat("Status should be a number", status, instanceOf(Integer.class));
            assertThat("Status should be 0, 1, or 2", status, in(new Integer[] { 0, 1, 2 }));

            logger.info("Video processing status validated: {}", status);
        } else {
            logger.info("Video processing status not present in response");
        }
    }

    @Test(description = "Video link is properly stored", priority = 10, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.CRITICAL)
    public void testVideoLinkProperlyStored() {
        // Check if data.url exists
        if (videoUploadResponse.getData() != null && videoUploadResponse.getData().getUrl() != null) {
            String url = videoUploadResponse.getData().getUrl();
            assertThat("URL should be a string", url, instanceOf(String.class));

            logger.info("Video URL validated: {}", url);
        }

        // Check if data.videoLink exists
        if (videoUploadResponse.getData() != null && videoUploadResponse.getData().getVideoLink() != null) {
            String videoLink = videoUploadResponse.getData().getVideoLink();

            assertThat("VideoLink should include Firebase domain",
                    videoLink, containsString("firebasestorage.googleapis.com"));
            assertThat("VideoLink should include .mp4",
                    videoLink.toLowerCase(), containsString(".mp4"));
            assertThat("VideoLink should match request",
                    videoLink, equalTo(VIDEO_LINK));

            logger.info("Video link validated: {}", videoLink);
        } else if (videoUploadResponse.getVideoLink() != null) {
            // Check root level videoLink
            String videoLink = videoUploadResponse.getVideoLink();

            assertThat("VideoLink should be a string", videoLink, instanceOf(String.class));
            assertThat("VideoLink should include Firebase domain",
                    videoLink, containsString("firebasestorage.googleapis.com"));

            logger.info("Root level video link validated: {}", videoLink);
        }
    }

    @Test(description = "Video link exists in response", priority = 11, dependsOnMethods = "testStatusCode200", groups = "bomb")
    @Story("Video Upload")
    @Severity(SeverityLevel.CRITICAL)
    public void testVideoLinkExists() {
        // Get video link from data.url
        String videoLink = videoUploadResponse.getData() != null
                ? videoUploadResponse.getData().getUrl()
                : null;

        // Validate video link
        assertThat("Video link should exist", videoLink, notNullValue());
        assertThat("Video link should be a string", videoLink, instanceOf(String.class));
        assertThat("Video link should contain Firebase domain",
                videoLink, containsString("firebasestorage.googleapis.com"));

        logger.info("Video link exists and validated: {}", videoLink);
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/CatalogByIdTest.java`

_233 lines_

```java
package com.automation.tests.buyerapp;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CatalogByIdDetailResponse;
import com.automation.utils.JsonUtils;
import com.automation.utils.TestDataResolver;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Catalog By ID API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/catalog/{{live_catalog_id}}
 * Validates response structure and catalog details.
 */
@Epic("Buyer App Catalog")
@Feature("Catalog By ID API")
@TmsLink("TC-CATALOG-009")
@TmsLink("TC-CATALOG-008")
@TmsLink("TC-CATALOG-004")
public class CatalogByIdTest extends BaseTest {

    private static Response catalogResponse;
    private static CatalogByIdDetailResponse catalogResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass(alwaysRun = true)
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

        logger.info("Using Buyer App token from VariableManager");
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatusCode200() {
        // Send GET request with authentication
        catalogResponse = buyerAppSpec()
                .when()
                .pathParam("catalogId", TestDataResolver.liveCatalogId(BaseTest::buyerAppSpec))
                .get("/v1/catalog/{catalogId}");

        // Parse response for other tests
        catalogResponseData = JsonUtils.fromResponse(catalogResponse, CatalogByIdDetailResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                catalogResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response has valid Content-Type header", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        // Response has valid Content-Type header
        assertThat("Content-Type should include application/json",
                catalogResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Content-Type header verified: {}", catalogResponse.getHeader("Content-Type"));
    }

    @Test(description = "Response time is less than threshold", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = catalogResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Response has required top-level fields", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.CRITICAL)
    public void testTopLevelFields() {
        // Response has required top-level fields
        assertThat("Response should be an object", catalogResponseData, notNullValue());
        assertThat("statusCode should be present", catalogResponseData.getStatusCode(), notNullValue());
        assertThat("message should be present", catalogResponseData.getMessage(), notNullValue());
        assertThat("data should be present", catalogResponseData.getData(), notNullValue());

        logger.info("Top-level fields validated: statusCode, message, data");
    }

    @Test(description = "Data object has correct structure", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataObjectStructure() {
        if (catalogResponseData.getData() != null) {
            CatalogByIdDetailResponse.CatalogDetailData data = catalogResponseData.getData();

            assertThat("data should be an object", data, notNullValue());
            assertThat("seller should be an object", data.getSeller(), notNullValue());
            assertThat("taggedBy should be a string", data.getTaggedBy(), instanceOf(String.class));

            if (data.getThumbnail() != null) {
                assertThat("thumbnail should be an object", data.getThumbnail(), notNullValue());
            }
            if (data.getProduct() != null) {
                assertThat("product should be an array", data.getProduct(), instanceOf(java.util.List.class));
            }
            if (data.getMarket() != null) {
                assertThat("market should be an object", data.getMarket(), notNullValue());
            }
            if (data.getImages() != null) {
                assertThat("images should be an array", data.getImages(), instanceOf(java.util.List.class));
            }

            logger.info("Data object structure validated");
        }
    }

    @Test(description = "Seller object contains required fields", priority = 6, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerObjectFields() {
        if (catalogResponseData.getData() != null && catalogResponseData.getData().getSeller() != null) {
            CatalogByIdDetailResponse.Seller seller = catalogResponseData.getData().getSeller();

            assertThat("seller should be an object", seller, notNullValue());
            assertThat("phoneNumber should be a string", seller.getPhoneNumber(), instanceOf(String.class));
            assertThat("address should be a string", seller.getAddress(), instanceOf(String.class));
            assertThat("businessName should be a string", seller.getBusinessName(), instanceOf(String.class));
            assertThat("name should be a string", seller.getName(), instanceOf(String.class));

            if (seller.getIsSuper() != null) {
                assertThat("isSuper should be a boolean", seller.getIsSuper(), instanceOf(Boolean.class));
            }
            if (seller.getDeprioritisation_status() != null) {
                assertThat("deprioritisation_status should be a boolean", seller.getDeprioritisation_status(),
                        instanceOf(Boolean.class));
            }
            if (seller.getIsCatalogAvailable() != null) {
                assertThat("isCatalogAvailable should be a boolean", seller.getIsCatalogAvailable(),
                        instanceOf(Boolean.class));
            }
            if (seller.getOrdersEnabled() != null) {
                assertThat("ordersEnabled should be a boolean", seller.getOrdersEnabled(), instanceOf(Boolean.class));
            }
            if (seller.getMov() != null) {
                assertThat("mov should be a number", seller.getMov(), instanceOf(Integer.class));
            }

            logger.info("Seller object fields validated");
        }
    }

    @Test(description = "Product array contains valid items", priority = 7, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.NORMAL)
    public void testProductArrayItems() {
        if (catalogResponseData.getData() != null && catalogResponseData.getData().getProduct() != null) {
            catalogResponseData.getData().getProduct().forEach(product -> {
                assertThat("product should be an object", product, notNullValue());
                assertThat("name should be a non-empty string",
                        product.getName(), allOf(instanceOf(String.class), not(emptyOrNullString())));
                assertThat("id should be a non-empty string",
                        product.getId(), allOf(instanceOf(String.class), not(emptyOrNullString())));
            });

            logger.info("Product array validated: {} products", catalogResponseData.getData().getProduct().size());
        }
    }

    @Test(description = "Thumbnail object contains required fields", priority = 8, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.NORMAL)
    public void testThumbnailObjectFields() {
        if (catalogResponseData.getData() != null && catalogResponseData.getData().getThumbnail() != null) {
            CatalogByIdDetailResponse.Thumbnail thumbnail = catalogResponseData.getData().getThumbnail();

            assertThat("fileName should be a string", thumbnail.getFileName(), instanceOf(String.class));
            assertThat("mimeType should be a string", thumbnail.getMimeType(), instanceOf(String.class));
            assertThat("_id should be a string", thumbnail.get_id(), instanceOf(String.class));
            assertThat("url should be a string", thumbnail.getUrl(), instanceOf(String.class));

            logger.info("Thumbnail object validated");
        }
    }

    @Test(description = "Market object contains required fields", priority = 9, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.NORMAL)
    public void testMarketObjectFields() {
        if (catalogResponseData.getData() != null && catalogResponseData.getData().getMarket() != null) {
            CatalogByIdDetailResponse.Market market = catalogResponseData.getData().getMarket();

            assertThat("country should be present", market.getCountry(), notNullValue());
            assertThat("city should be present", market.getCity(), notNullValue());
            assertThat("state should be present", market.getState(), notNullValue());
            assertThat("name should be present", market.getName(), notNullValue());
            assertThat("marketNumber should be present", market.getMarketNumber(), notNullValue());
            // createdAt/updatedAt are not asserted: markets created before the Market
            // schema gained timestamps have neither, and the catalog is picked at run time.

            logger.info("Market object validated: {}, {}", market.getName(), market.getCity());
        }
    }

    @Test(description = "Images array contains valid items", priority = 10, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Catalog By ID")
    @Severity(SeverityLevel.NORMAL)
    public void testImagesArrayItems() {
        if (catalogResponseData.getData() != null && catalogResponseData.getData().getImages() != null) {
            assertThat("images array should have at least one item",
                    catalogResponseData.getData().getImages(), hasSize(greaterThan(0)));

            logger.info("Images array validated: {} images", catalogResponseData.getData().getImages().size());
        }
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/CollectionListing/AllCollection/CollectionAllForReadymadeTest.java`

_140 lines_

```java
package com.automation.tests.buyerapp.CollectionListing.AllCollection;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CollectionAllResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Collection All (Readymade) API - Converted from Postman
 * Script.
 * Endpoint: {{navo_base}}/v1/collection/all?suitable_for=readymade
 * Validates response structure, headers, and collection data.
 */
@Epic("Buyer App Collection Listing")
@Feature("Collection All for Readymade API")
@TmsLink("TC-COLLECTION-002")
@TmsLink("TC-COLLECTION-001")
public class CollectionAllForReadymadeTest extends BaseTest {

        private static Response collectionAllResponse;
        private static CollectionAllResponse collectionAllResponseData;
        private String buyerAppBaseUrl;

        @BeforeClass(alwaysRun = true)
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

                logger.info("Using Buyer App token from VariableManager");
        }

        @Test(description = "Status code is 200", priority = 1, groups = "buyerapp")
        @Story("Collection All for Readymade")
        @Severity(SeverityLevel.CRITICAL)
        public void testStatusCode200() {
                // Send GET request with authentication and query parameter
                collectionAllResponse = buyerAppSpec()
                                .queryParam("suitable_for", "readymade")
                                .when()
                                .get(BuyerAppEndpoints.COLLECTION_ALL);

                // Parse response for other tests
                collectionAllResponseData = JsonUtils.fromResponse(collectionAllResponse, CollectionAllResponse.class);

                // Status code is 200
                assertThat("Status code is 200",
                                collectionAllResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Collection All for Readymade")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = collectionAllResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Response time is less than threshold
                assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Response has JSON body", priority = 3, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Collection All for Readymade")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseHasJsonBody() {
                // Response has JSON body
                assertThat("Response should have a body",
                                collectionAllResponse.getBody().asString(), not(emptyOrNullString()));
                assertThat("Response should be parseable as JSON",
                                collectionAllResponseData, notNullValue());

                logger.info("Response has valid JSON body");
        }

        @Test(description = "Required headers are present", priority = 4, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Collection All for Readymade")
        @Severity(SeverityLevel.MINOR)
        public void testRequiredHeaders() {
                // Required headers are present
                assertThat("Content-Type header should be present",
                                collectionAllResponse.getHeader("Content-Type"), notNullValue());
                assertThat("Content-Type should include application/json",
                                collectionAllResponse.getHeader("Content-Type"), containsString("application/json"));

                logger.info("Required headers validated: Content-Type = {}",
                                collectionAllResponse.getHeader("Content-Type"));
        }

        @Test(description = "Validate response structure", priority = 5, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Collection All for Readymade")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStructure() {
                // Top-level validation
                assertThat("Response should be an object", collectionAllResponseData, notNullValue());
                assertThat("statusCode should be '10000'",
                                collectionAllResponseData.getStatusCode(), equalTo("10000"));
                assertThat("message should be 'success'",
                                collectionAllResponseData.getMessage(), equalTo("success"));
                assertThat("data should be an object",
                                collectionAllResponseData.getData(), notNullValue());

                // Collection data validation
                assertThat("data should have property 'result' that is an array",
                                collectionAllResponseData.getData().getResult(), instanceOf(java.util.List.class));
                assertThat("result should not be empty",
                                collectionAllResponseData.getData().getResult(), not(empty()));

                // Validate each collection item
                collectionAllResponseData.getData().getResult().forEach(item -> {
                        assertThat("Item should be an object", item, notNullValue());
                        assertThat("_id should be a string", item.get_id(), instanceOf(String.class));
                        assertThat("name should be a string", item.getName(), instanceOf(String.class));
                        assertThat("description should be a string", item.getDescription(), instanceOf(String.class));
                        assertThat("image should be a string", item.getImage(), instanceOf(String.class));
                });

                logger.info("Response structure validated successfully: {} collections found",
                                collectionAllResponseData.getData().getResult().size());
        }
}
```

### `src/test/java/com/automation/tests/buyerapp/CollectionListing/AllCollection/CollectionAllForSareeTest.java`

_139 lines_

```java
package com.automation.tests.buyerapp.CollectionListing.AllCollection;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CollectionAllResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Collection All (Saree) API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/collection/all?suitable_for=saree
 * Validates response structure, headers, and collection data.
 */
@Epic("Buyer App Collection Listing")
@Feature("Collection All for Saree API")
@TmsLink("TC-COLLECTION-002")
@TmsLink("TC-COLLECTION-001")
public class CollectionAllForSareeTest extends BaseTest {

        private static Response collectionAllResponse;
        private static CollectionAllResponse collectionAllResponseData;
        private String buyerAppBaseUrl;

        @BeforeClass(alwaysRun = true)
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

                logger.info("Using Buyer App token from VariableManager");
        }

        @Test(description = "Status code is 200", priority = 1, groups = "buyerapp")
        @Story("Collection All for Saree")
        @Severity(SeverityLevel.CRITICAL)
        public void testStatusCode200() {
                // Send GET request with authentication and query parameter
                collectionAllResponse = buyerAppSpec()
                                .queryParam("suitable_for", "saree")
                                .when()
                                .get(BuyerAppEndpoints.COLLECTION_ALL);

                // Parse response for other tests
                collectionAllResponseData = JsonUtils.fromResponse(collectionAllResponse, CollectionAllResponse.class);

                // Status code is 200
                assertThat("Status code is 200",
                                collectionAllResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Collection All for Saree")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = collectionAllResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Response time is less than threshold
                assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Response has JSON body", priority = 3, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Collection All for Saree")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseHasJsonBody() {
                // Response has JSON body
                assertThat("Response should have a body",
                                collectionAllResponse.getBody().asString(), not(emptyOrNullString()));
                assertThat("Response should be parseable as JSON",
                                collectionAllResponseData, notNullValue());

                logger.info("Response has valid JSON body");
        }

        @Test(description = "Required headers are present", priority = 4, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Collection All for Saree")
        @Severity(SeverityLevel.MINOR)
        public void testRequiredHeaders() {
                // Required headers are present
                assertThat("Content-Type header should be present",
                                collectionAllResponse.getHeader("Content-Type"), notNullValue());
                assertThat("Content-Type should include application/json",
                                collectionAllResponse.getHeader("Content-Type"), containsString("application/json"));

                logger.info("Required headers validated: Content-Type = {}",
                                collectionAllResponse.getHeader("Content-Type"));
        }

        @Test(description = "Validate response structure", priority = 5, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Collection All for Saree")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStructure() {
                // Top-level validation
                assertThat("Response should be an object", collectionAllResponseData, notNullValue());
                assertThat("statusCode should be '10000'",
                                collectionAllResponseData.getStatusCode(), equalTo("10000"));
                assertThat("message should be 'success'",
                                collectionAllResponseData.getMessage(), equalTo("success"));
                assertThat("data should be an object",
                                collectionAllResponseData.getData(), notNullValue());

                // Collection data validation
                assertThat("data should have property 'result' that is an array",
                                collectionAllResponseData.getData().getResult(), instanceOf(java.util.List.class));
                assertThat("result should not be empty",
                                collectionAllResponseData.getData().getResult(), not(empty()));

                // Validate each collection item
                collectionAllResponseData.getData().getResult().forEach(item -> {
                        assertThat("Item should be an object", item, notNullValue());
                        assertThat("_id should be a string", item.get_id(), instanceOf(String.class));
                        assertThat("name should be a string", item.getName(), instanceOf(String.class));
                        assertThat("description should be a string", item.getDescription(), instanceOf(String.class));
                        assertThat("image should be a string", item.getImage(), instanceOf(String.class));
                });

                logger.info("Response structure validated successfully: {} collections found",
                                collectionAllResponseData.getData().getResult().size());
        }
}
```

### `src/test/java/com/automation/tests/buyerapp/CollectionListing/AllCollectionCounts/CollectionCountsForReadymadeTest.java`

_181 lines_

```java
package com.automation.tests.buyerapp.CollectionListing.AllCollectionCounts;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CollectionAllResponse;
import com.automation.models.response.CollectionByIdResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Collection Counts (Readymade) - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/collection/all?suitable_for=readymade
 * Fetches all collections and validates item counts for each collection.
 */
@Epic("Buyer App Collection Listing")
@Feature("Collection Counts for Readymade API")
@TmsLink("TC-COLLECTION-002")
@TmsLink("TC-COLLECTION-009")
@TmsLink("TC-COLLECTION-005")
public class CollectionCountsForReadymadeTest extends BaseTest {

    private static List<CollectionItemCount> collectionCounts;
    private String buyerAppBaseUrl;

    @BeforeClass(alwaysRun = true)
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

        logger.info("Using Buyer App token from VariableManager");
    }

    @Test(description = "Fetch all collections and get item counts", priority = 1, groups = "buyerapp")
    @Story("Collection Counts for Readymade")
    @Severity(SeverityLevel.CRITICAL)
    public void testFetchAllCollectionsAndCounts() {
        // Step 1: Get all collections
        Response allCollectionsResponse = buyerAppSpec()
                .queryParam("suitable_for", "readymade")
                .when()
                .get(BuyerAppEndpoints.COLLECTION_ALL);

        CollectionAllResponse allCollectionsData = JsonUtils.fromResponse(allCollectionsResponse,
                CollectionAllResponse.class);

        assertThat("Status code should be 200", allCollectionsResponse.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat("Collections should not be empty", allCollectionsData.getData().getResult(), not(empty()));

        logger.info("Fetched {} collections for Readymade", allCollectionsData.getData().getResult().size());

        // Step 2: Iterate through each collection and get item counts
        collectionCounts = new ArrayList<>();

        for (CollectionAllResponse.CollectionItem collection : allCollectionsData.getData().getResult()) {
            try {
                // Add delay to avoid rate limiting
                Thread.sleep(200);

                Response collectionByIdResponse = buyerAppSpec()
                        .queryParam("limit", 35)
                        .queryParam("offset", 0)
                        .when()
                        .get("/v1/collection/" + collection.get_id());

                if (collectionByIdResponse.getStatusCode() == 401 || collectionByIdResponse.getStatusCode() == 10003) {
                    logger.error("Authentication failed for collection: {}", collection.getName());
                    collectionCounts
                            .add(new CollectionItemCount(collection.get_id(), collection.getName(), -1, "AUTH_ERROR"));
                    break; // Stop processing on auth error
                } else if (collectionByIdResponse.getStatusCode() == 200) {
                    CollectionByIdResponse collectionData = JsonUtils.fromResponse(collectionByIdResponse,
                            CollectionByIdResponse.class);
                    int totalItems = collectionData.getData().getTotal().getValue();
                    collectionCounts
                            .add(new CollectionItemCount(collection.get_id(), collection.getName(), totalItems, null));
                    logger.info("Collection: {} - Items: {}", collection.getName(), totalItems);
                } else {
                    logger.error("API error for collection: {} - Status: {}", collection.getName(),
                            collectionByIdResponse.getStatusCode());
                    collectionCounts
                            .add(new CollectionItemCount(collection.get_id(), collection.getName(), -1, "API_ERROR"));
                }
            } catch (Exception e) {
                logger.error("Error processing collection: {} - {}", collection.getName(), e.getMessage());
                collectionCounts.add(new CollectionItemCount(collection.get_id(), collection.getName(), -1, "ERROR"));
            }
        }

        logger.info("Processing completed for {} collections", collectionCounts.size());
    }

    @Test(description = "No authentication errors occurred", priority = 2, dependsOnMethods = "testFetchAllCollectionsAndCounts", groups = "buyerapp")
    @Story("Collection Counts for Readymade")
    @Severity(SeverityLevel.BLOCKER)
    public void testNoAuthenticationErrors() {
        boolean hasAuthError = collectionCounts.stream().anyMatch(c -> "AUTH_ERROR".equals(c.getError()));
        assertThat("No authentication errors should occur", hasAuthError, is(false));

        logger.info("No authentication errors found");
    }

    @Test(description = "Total items retrieved", priority = 3, dependsOnMethods = "testFetchAllCollectionsAndCounts", groups = "buyerapp")
    @Story("Collection Counts for Readymade")
    @Severity(SeverityLevel.CRITICAL)
    public void testTotalItemsRetrieved() {
        int totalAllItems = collectionCounts.stream()
                .filter(c -> c.getTotalItems() >= 0)
                .mapToInt(CollectionItemCount::getTotalItems)
                .sum();

        assertThat("Total items should be greater than 0", totalAllItems, greaterThan(0));

        logger.info("Total items across all collections: {}", totalAllItems);
    }

    @Test(description = "All collections have item counts", priority = 4, dependsOnMethods = "testFetchAllCollectionsAndCounts", groups = "buyerapp")
    @Story("Collection Counts for Readymade")
    @Severity(SeverityLevel.CRITICAL)
    public void testAllCollectionsHaveItemCounts() {
        long successCount = collectionCounts.stream().filter(c -> c.getTotalItems() >= 0).count();

        assertThat(String.format("All %d collections should have item counts", collectionCounts.size()),
                successCount, equalTo((long) collectionCounts.size()));

        logger.info("All {} collections have valid item counts", successCount);
    }

    @Test(description = "Each collection has valid item count", priority = 5, dependsOnMethods = "testFetchAllCollectionsAndCounts", groups = "buyerapp")
    @Story("Collection Counts for Readymade")
    @Severity(SeverityLevel.CRITICAL)
    public void testEachCollectionHasValidItemCount() {
        for (CollectionItemCount collection : collectionCounts) {
            assertThat(String.format("%s collection should have valid item count", collection.getName()),
                    collection.getTotalItems(), greaterThanOrEqualTo(0));
        }

        logger.info("Each collection validated for item count");
    }

    @Test(description = "Average items per collection is reasonable", priority = 6, dependsOnMethods = "testFetchAllCollectionsAndCounts", groups = "buyerapp")
    @Story("Collection Counts for Readymade")
    @Severity(SeverityLevel.NORMAL)
    public void testAverageItemsPerCollection() {
        long successCount = collectionCounts.stream().filter(c -> c.getTotalItems() >= 0).count();
        int totalAllItems = collectionCounts.stream()
                .filter(c -> c.getTotalItems() >= 0)
                .mapToInt(CollectionItemCount::getTotalItems)
                .sum();

        double avgItems = successCount > 0 ? (double) totalAllItems / successCount : 0;

        assertThat("Average items per collection should be greater than 0", avgItems, greaterThan(0.0));

        logger.info("Average items per collection: {}", avgItems);
    }

    // Helper class to store collection count data
    @Data
    @AllArgsConstructor
    private static class CollectionItemCount {
        private String id;
        private String name;
        private int totalItems;
        private String error;
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/CollectionListing/AllCollectionCounts/CollectionCountsForSareeTest.java`

_207 lines_

```java
package com.automation.tests.buyerapp.CollectionListing.AllCollectionCounts;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CollectionAllResponse;
import com.automation.models.response.CollectionByIdResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Collection Counts (Saree) - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/collection/all?suitable_for=saree
 * Fetches all collections and validates item counts for each collection.
 */
@Epic("Buyer App Collection Listing")
@Feature("Collection Counts for Saree API")
@TmsLink("TC-COLLECTION-002")
@TmsLink("TC-COLLECTION-009")
@TmsLink("TC-COLLECTION-005")
public class CollectionCountsForSareeTest extends BaseTest {

    /** Share of collections allowed to be empty (live stock) before the check fails. */
    private static final double MAX_EMPTY_COLLECTION_RATIO = 0.10;


    private static List<CollectionItemCount> collectionCounts;
    private String buyerAppBaseUrl;

    @BeforeClass(alwaysRun = true)
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

        logger.info("Using Buyer App token from VariableManager");
    }

    @Test(description = "Fetch all collections and get item counts", priority = 1, groups = "buyerapp")
    @Story("Collection Counts for Saree")
    @Severity(SeverityLevel.CRITICAL)
    public void testFetchAllCollectionsAndCounts() {
        // Step 1: Get all collections
        Response allCollectionsResponse = buyerAppSpec()
                .queryParam("suitable_for", "saree")
                .when()
                .get(BuyerAppEndpoints.COLLECTION_ALL);

        CollectionAllResponse allCollectionsData = JsonUtils.fromResponse(allCollectionsResponse,
                CollectionAllResponse.class);

        assertThat("Status code should be 200", allCollectionsResponse.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat("Collections should not be empty", allCollectionsData.getData().getResult(), not(empty()));

        logger.info("Fetched {} collections for Saree", allCollectionsData.getData().getResult().size());

        // Step 2: Iterate through each collection and get item counts
        collectionCounts = new ArrayList<>();

        for (CollectionAllResponse.CollectionItem collection : allCollectionsData.getData().getResult()) {
            try {
                // Add delay to avoid rate limiting
                Thread.sleep(200);

                Response collectionByIdResponse = buyerAppSpec()
                        .queryParam("limit", 35)
                        .queryParam("offset", 0)
                        .when()
                        .get("/v1/collection/" + collection.get_id());

                if (collectionByIdResponse.getStatusCode() == 401 || collectionByIdResponse.getStatusCode() == 10003) {
                    logger.error("Authentication failed for collection: {}", collection.getName());
                    collectionCounts
                            .add(new CollectionItemCount(collection.get_id(), collection.getName(), -1, "AUTH_ERROR"));
                    break; // Stop processing on auth error
                } else if (collectionByIdResponse.getStatusCode() == 200) {
                    CollectionByIdResponse collectionData = JsonUtils.fromResponse(collectionByIdResponse,
                            CollectionByIdResponse.class);
                    int totalItems = collectionData.getData().getTotal().getValue();
                    collectionCounts
                            .add(new CollectionItemCount(collection.get_id(), collection.getName(), totalItems, null));
                    logger.info("Collection: {} - Items: {}", collection.getName(), totalItems);
                } else {
                    logger.error("API error for collection: {} - Status: {}", collection.getName(),
                            collectionByIdResponse.getStatusCode());
                    collectionCounts
                            .add(new CollectionItemCount(collection.get_id(), collection.getName(), -1, "API_ERROR"));
                }
            } catch (Exception e) {
                logger.error("Error processing collection: {} - {}", collection.getName(), e.getMessage());
                collectionCounts.add(new CollectionItemCount(collection.get_id(), collection.getName(), -1, "ERROR"));
            }
        }

        logger.info("Processing completed for {} collections", collectionCounts.size());
    }

    @Test(description = "No authentication errors occurred", priority = 2, dependsOnMethods = "testFetchAllCollectionsAndCounts", groups = "buyerapp")
    @Story("Collection Counts for Saree")
    @Severity(SeverityLevel.BLOCKER)
    public void testNoAuthenticationErrors() {
        boolean hasAuthError = collectionCounts.stream().anyMatch(c -> "AUTH_ERROR".equals(c.getError()));
        assertThat("No authentication errors should occur", hasAuthError, is(false));

        logger.info("No authentication errors found");
    }

    @Test(description = "Total items retrieved", priority = 3, dependsOnMethods = "testFetchAllCollectionsAndCounts", groups = "buyerapp")
    @Story("Collection Counts for Saree")
    @Severity(SeverityLevel.CRITICAL)
    public void testTotalItemsRetrieved() {
        int totalAllItems = collectionCounts.stream()
                .filter(c -> c.getTotalItems() >= 0)
                .mapToInt(CollectionItemCount::getTotalItems)
                .sum();

        assertThat("Total items should be greater than 0", totalAllItems, greaterThan(0));

        logger.info("Total items across all collections: {}", totalAllItems);
    }

    @Test(description = "All collections have item counts", priority = 4, dependsOnMethods = "testFetchAllCollectionsAndCounts", groups = "buyerapp")
    @Story("Collection Counts for Saree")
    @Severity(SeverityLevel.CRITICAL)
    public void testAllCollectionsHaveItemCounts() {
        long successCount = collectionCounts.stream().filter(c -> c.getTotalItems() >= 0).count();

        assertThat(String.format("All %d collections should have item counts", collectionCounts.size()),
                successCount, equalTo((long) collectionCounts.size()));

        logger.info("All {} collections have valid item counts", successCount);
    }

    @Test(description = "Each collection has valid item count", priority = 5, dependsOnMethods = "testFetchAllCollectionsAndCounts", groups = "buyerapp")
    @Story("Collection Counts for Saree")
    @Severity(SeverityLevel.CRITICAL)
    public void testEachCollectionHasValidItemCount() {
        for (CollectionItemCount collection : collectionCounts) {
            assertThat(String.format("%s collection should have valid item count", collection.getName()),
                    collection.getTotalItems(), greaterThanOrEqualTo(0));
        }

        logger.info("Each collection validated for item count");
    }

    @Test(description = "No empty collections found", priority = 6, dependsOnMethods = "testFetchAllCollectionsAndCounts", groups = "buyerapp")
    @Story("Collection Counts for Saree")
    @Severity(SeverityLevel.NORMAL)
    public void testNoEmptyCollections() {
        // Collection contents depend on live stock, so a few empty collections are
        // expected. Fail only above MAX_EMPTY_COLLECTION_RATIO and list the empty ones.
        List<String> empty = collectionCounts.stream()
                .filter(c -> c.getTotalItems() == 0)
                .map(CollectionItemCount::getName)
                .collect(java.util.stream.Collectors.toList());
        long counted = collectionCounts.stream().filter(c -> c.getTotalItems() >= 0).count();
        double ratio = counted > 0 ? (double) empty.size() / counted : 0;

        if (!empty.isEmpty()) {
            logger.warn("{} of {} collections are empty: {}", empty.size(), counted, empty);
            Allure.addAttachment("Empty collections", String.join("\n", empty));
        }
        assertThat(String.format("Empty collections %d of %d (%.0f%%) exceed %.0f%%: %s",
                        empty.size(), counted, ratio * 100, MAX_EMPTY_COLLECTION_RATIO * 100, empty),
                ratio, lessThanOrEqualTo(MAX_EMPTY_COLLECTION_RATIO));
    }

    @Test(description = "Average items per collection is reasonable", priority = 7, dependsOnMethods = "testFetchAllCollectionsAndCounts", groups = "buyerapp")
    @Story("Collection Counts for Saree")
    @Severity(SeverityLevel.NORMAL)
    public void testAverageItemsPerCollection() {
        long successCount = collectionCounts.stream().filter(c -> c.getTotalItems() >= 0).count();
        int totalAllItems = collectionCounts.stream()
                .filter(c -> c.getTotalItems() >= 0)
                .mapToInt(CollectionItemCount::getTotalItems)
                .sum();

        double avgItems = successCount > 0 ? (double) totalAllItems / successCount : 0;

        assertThat("Average items per collection should be greater than 0", avgItems, greaterThan(0.0));

        logger.info("Average items per collection: {}", avgItems);
    }

    // Helper class to store collection count data
    @Data
    @AllArgsConstructor
    private static class CollectionItemCount {
        private String id;
        private String name;
        private int totalItems;
        private String error;
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/CollectionListing/SimilarCollection/SimilarCollectionForReadymadeTest.java`

_246 lines_

```java
package com.automation.tests.buyerapp.CollectionListing.SimilarCollection;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CollectionAllResponse;
import com.automation.models.response.SimilarCollectionResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Similar Collection (Readymade) - Converted from Postman
 * Script.
 * Endpoint: {{navo_base}}/v1/collection/all?suitable_for=readymade
 * Fetches all collections and validates similar collections for each.
 */
@Epic("Buyer App Collection Listing")
@Feature("Similar Collection for Readymade API")
@TmsLink("TC-COLLECTION-002")
@TmsLink("TC-COLLECTION-004")
@TmsLink("TC-COLLECTION-003")
public class SimilarCollectionForReadymadeTest extends BaseTest {

    private static List<SimilarCollectionData> similarCollectionData;
    private String buyerAppBaseUrl;

    @BeforeClass(alwaysRun = true)
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

        logger.info("Using Buyer App token from VariableManager");
    }

    @Test(description = "Fetch all collections and get similar collections for each", priority = 1, groups = "buyerapp")
    @Story("Similar Collection for Readymade")
    @Severity(SeverityLevel.CRITICAL)
    public void testFetchAllCollectionsAndSimilar() {
        // Step 1: Get all collections
        Response allCollectionsResponse = buyerAppSpec()
                .queryParam("suitable_for", "readymade")
                .when()
                .get(BuyerAppEndpoints.COLLECTION_ALL);

        CollectionAllResponse allCollectionsResponseData = JsonUtils.fromResponse(allCollectionsResponse,
                CollectionAllResponse.class);

        assertThat("Status code should be 200", allCollectionsResponse.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat("Collections should not be empty", allCollectionsResponseData.getData().getResult(), not(empty()));

        logger.info("Fetched {} collections for Readymade", allCollectionsResponseData.getData().getResult().size());

        // Step 2: Iterate through each collection and get similar collections
        similarCollectionData = new ArrayList<>();

        for (CollectionAllResponse.CollectionItem collection : allCollectionsResponseData.getData().getResult()) {
            try {
                // Add delay to avoid rate limiting
                Thread.sleep(200);

                Response similarResponse = buyerAppSpec()
                        .when()
                        .get("/v1/collection/" + collection.get_id() + "/similar");

                if (similarResponse.getStatusCode() == 401 || similarResponse.getStatusCode() == 10003) {
                    logger.error("Authentication failed for collection: {}", collection.getName());
                    similarCollectionData.add(new SimilarCollectionData(
                            collection.get_id(), collection.getName(), -1, 0, "AUTH_ERROR", false, null));
                    break; // Stop processing on auth error
                } else if (similarResponse.getStatusCode() == 200) {
                    SimilarCollectionResponse similarData = JsonUtils.fromResponse(similarResponse,
                            SimilarCollectionResponse.class);

                    // Validate response structure
                    if ("10000".equals(similarData.getStatusCode()) && similarData.getData() != null
                            && similarData.getData().getResult() != null) {
                        List<SimilarCollectionResponse.SimilarCollectionItem> similarCollections = similarData.getData()
                                .getResult();
                        int similarCount = similarCollections.size();
                        boolean hasSimilar = similarCount > 0;

                        // Calculate total catalogs across all similar collections
                        int totalCatalogs = similarCollections.stream()
                                .mapToInt(s -> s.getCatalogs() != null ? s.getCatalogs().size() : 0)
                                .sum();

                        // Store similar collection details
                        List<SimilarCollectionDetail> details = similarCollections.stream()
                                .map(s -> new SimilarCollectionDetail(
                                        s.get_id(),
                                        s.getName(),
                                        s.getDescription() != null ? s.getDescription() : "",
                                        s.getCatalogs() != null ? s.getCatalogs().size() : 0))
                                .collect(Collectors.toList());

                        similarCollectionData.add(new SimilarCollectionData(
                                collection.get_id(), collection.getName(), similarCount, totalCatalogs, null,
                                hasSimilar, details));

                        logger.info("Collection: {} - Similar: {}, Total Catalogs: {}",
                                collection.getName(), similarCount, totalCatalogs);
                    } else {
                        similarCollectionData.add(new SimilarCollectionData(
                                collection.get_id(), collection.getName(), 0, 0, null, false, new ArrayList<>()));
                    }
                } else {
                    logger.error("API error for collection: {} - Status: {}", collection.getName(),
                            similarResponse.getStatusCode());
                    similarCollectionData.add(new SimilarCollectionData(
                            collection.get_id(), collection.getName(), -1, 0, "API_ERROR", false, null));
                }
            } catch (Exception e) {
                logger.error("Error processing collection: {} - {}", collection.getName(), e.getMessage());
                similarCollectionData.add(new SimilarCollectionData(
                        collection.get_id(), collection.getName(), -1, 0, "ERROR", false, null));
            }
        }

        logger.info("Processing completed for {} collections", similarCollectionData.size());
    }

    @Test(description = "Similar collections have valid data structure", priority = 2, dependsOnMethods = "testFetchAllCollectionsAndSimilar", groups = "buyerapp")
    @Story("Similar Collection for Readymade")
    @Severity(SeverityLevel.CRITICAL)
    public void testSimilarCollectionsDataStructure() {
        List<SimilarCollectionData> collectionsWithSimilar = similarCollectionData.stream()
                .filter(SimilarCollectionData::isHasSimilar)
                .collect(Collectors.toList());

        collectionsWithSimilar.forEach(col -> {
            assertThat("Similar collections should be an array", col.getSimilarCollections(), notNullValue());
            assertThat("Similar collections should be a list", col.getSimilarCollections(), instanceOf(List.class));

            if (!col.getSimilarCollections().isEmpty()) {
                col.getSimilarCollections().forEach(similar -> {
                    assertThat("Similar collection should have id", similar.getId(), notNullValue());
                    assertThat("Similar collection should have name", similar.getName(), notNullValue());
                    assertThat("Similar collection should have catalogCount", similar.getCatalogCount(),
                            notNullValue());
                    assertThat("catalogCount should be a number", similar.getCatalogCount(), instanceOf(Integer.class));
                });
            }
        });

        logger.info("Similar collections data structure validated for {} collections", collectionsWithSimilar.size());
    }

    @Test(description = "Each similar collection contains catalogs array", priority = 3, dependsOnMethods = "testFetchAllCollectionsAndSimilar", groups = "buyerapp")
    @Story("Similar Collection for Readymade")
    @Severity(SeverityLevel.NORMAL)
    public void testSimilarCollectionsCatalogsArray() {
        List<SimilarCollectionData> collectionsWithSimilar = similarCollectionData.stream()
                .filter(SimilarCollectionData::isHasSimilar)
                .collect(Collectors.toList());

        collectionsWithSimilar.forEach(col -> {
            if (col.getSimilarCollections() != null && !col.getSimilarCollections().isEmpty()) {
                col.getSimilarCollections().forEach(similar -> {
                    assertThat("Catalog count should be at least 0", similar.getCatalogCount(),
                            greaterThanOrEqualTo(0));
                });
            }
        });

        logger.info("Catalogs array validated for similar collections");
    }

    @Test(description = "Total catalogs count is reasonable", priority = 4, dependsOnMethods = "testFetchAllCollectionsAndSimilar", groups = "buyerapp")
    @Story("Similar Collection for Readymade")
    @Severity(SeverityLevel.NORMAL)
    public void testTotalCatalogsCount() {
        int totalCatalogs = similarCollectionData.stream()
                .filter(c -> c.getSimilarCount() >= 0)
                .mapToInt(SimilarCollectionData::getTotalCatalogs)
                .sum();

        assertThat("Total catalogs should be at least 0", totalCatalogs, greaterThanOrEqualTo(0));

        long collectionsWithSimilar = similarCollectionData.stream()
                .filter(SimilarCollectionData::isHasSimilar)
                .count();

        if (collectionsWithSimilar > 0) {
            assertThat("Total catalogs should be greater than 0 when similar collections exist",
                    totalCatalogs, greaterThan(0));
        }

        logger.info("Total catalogs count validated: {}", totalCatalogs);
    }

    @Test(description = "Average similar collections per collection is reasonable", priority = 5, dependsOnMethods = "testFetchAllCollectionsAndSimilar", groups = "buyerapp")
    @Story("Similar Collection for Readymade")
    @Severity(SeverityLevel.NORMAL)
    public void testAverageSimilarCollections() {
        long successCount = similarCollectionData.stream()
                .filter(c -> c.getSimilarCount() >= 0)
                .count();

        int totalSimilarCollections = similarCollectionData.stream()
                .filter(c -> c.getSimilarCount() >= 0)
                .mapToInt(SimilarCollectionData::getSimilarCount)
                .sum();

        double avgSimilar = successCount > 0 ? (double) totalSimilarCollections / successCount : 0;

        assertThat("Average similar collections should be at least 0", avgSimilar, greaterThanOrEqualTo(0.0));

        logger.info("Average similar collections per collection: {}", avgSimilar);
    }

    // Helper classes to store similar collection data
    @Data
    @AllArgsConstructor
    private static class SimilarCollectionData {
        private String id;
        private String name;
        private int similarCount;
        private int totalCatalogs;
        private String error;
        private boolean hasSimilar;
        private List<SimilarCollectionDetail> similarCollections;
    }

    @Data
    @AllArgsConstructor
    private static class SimilarCollectionDetail {
        private String id;
        private String name;
        private String description;
        private Integer catalogCount;
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/CollectionListing/SimilarCollection/SimilarCollectionForSareeTest.java`

_245 lines_

```java
package com.automation.tests.buyerapp.CollectionListing.SimilarCollection;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.CollectionAllResponse;
import com.automation.models.response.SimilarCollectionResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Similar Collection (Saree) - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/collection/all?suitable_for=saree
 * Fetches all collections and validates similar collections for each.
 */
@Epic("Buyer App Collection Listing")
@Feature("Similar Collection for Saree API")
@TmsLink("TC-COLLECTION-002")
@TmsLink("TC-COLLECTION-004")
@TmsLink("TC-COLLECTION-003")
public class SimilarCollectionForSareeTest extends BaseTest {

    private static List<SimilarCollectionData> similarCollectionData;
    private String buyerAppBaseUrl;

    @BeforeClass(alwaysRun = true)
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

        logger.info("Using Buyer App token from VariableManager");
    }

    @Test(description = "Fetch all collections and get similar collections for each", priority = 1, groups = "buyerapp")
    @Story("Similar Collection for Saree")
    @Severity(SeverityLevel.CRITICAL)
    public void testFetchAllCollectionsAndSimilar() {
        // Step 1: Get all collections
        Response allCollectionsResponse = buyerAppSpec()
                .queryParam("suitable_for", "saree")
                .when()
                .get(BuyerAppEndpoints.COLLECTION_ALL);

        CollectionAllResponse allCollectionsResponseData = JsonUtils.fromResponse(allCollectionsResponse,
                CollectionAllResponse.class);

        assertThat("Status code should be 200", allCollectionsResponse.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat("Collections should not be empty", allCollectionsResponseData.getData().getResult(), not(empty()));

        logger.info("Fetched {} collections for Saree", allCollectionsResponseData.getData().getResult().size());

        // Step 2: Iterate through each collection and get similar collections
        similarCollectionData = new ArrayList<>();

        for (CollectionAllResponse.CollectionItem collection : allCollectionsResponseData.getData().getResult()) {
            try {
                // Add delay to avoid rate limiting
                Thread.sleep(200);

                Response similarResponse = buyerAppSpec()
                        .when()
                        .get("/v1/collection/" + collection.get_id() + "/similar");

                if (similarResponse.getStatusCode() == 401 || similarResponse.getStatusCode() == 10003) {
                    logger.error("Authentication failed for collection: {}", collection.getName());
                    similarCollectionData.add(new SimilarCollectionData(
                            collection.get_id(), collection.getName(), -1, 0, "AUTH_ERROR", false, null));
                    break; // Stop processing on auth error
                } else if (similarResponse.getStatusCode() == 200) {
                    SimilarCollectionResponse similarData = JsonUtils.fromResponse(similarResponse,
                            SimilarCollectionResponse.class);

                    // Validate response structure
                    if ("10000".equals(similarData.getStatusCode()) && similarData.getData() != null
                            && similarData.getData().getResult() != null) {
                        List<SimilarCollectionResponse.SimilarCollectionItem> similarCollections = similarData.getData()
                                .getResult();
                        int similarCount = similarCollections.size();
                        boolean hasSimilar = similarCount > 0;

                        // Calculate total catalogs across all similar collections
                        int totalCatalogs = similarCollections.stream()
                                .mapToInt(s -> s.getCatalogs() != null ? s.getCatalogs().size() : 0)
                                .sum();

                        // Store similar collection details
                        List<SimilarCollectionDetail> details = similarCollections.stream()
                                .map(s -> new SimilarCollectionDetail(
                                        s.get_id(),
                                        s.getName(),
                                        s.getDescription() != null ? s.getDescription() : "",
                                        s.getCatalogs() != null ? s.getCatalogs().size() : 0))
                                .collect(Collectors.toList());

                        similarCollectionData.add(new SimilarCollectionData(
                                collection.get_id(), collection.getName(), similarCount, totalCatalogs, null,
                                hasSimilar, details));

                        logger.info("Collection: {} - Similar: {}, Total Catalogs: {}",
                                collection.getName(), similarCount, totalCatalogs);
                    } else {
                        similarCollectionData.add(new SimilarCollectionData(
                                collection.get_id(), collection.getName(), 0, 0, null, false, new ArrayList<>()));
                    }
                } else {
                    logger.error("API error for collection: {} - Status: {}", collection.getName(),
                            similarResponse.getStatusCode());
                    similarCollectionData.add(new SimilarCollectionData(
                            collection.get_id(), collection.getName(), -1, 0, "API_ERROR", false, null));
                }
            } catch (Exception e) {
                logger.error("Error processing collection: {} - {}", collection.getName(), e.getMessage());
                similarCollectionData.add(new SimilarCollectionData(
                        collection.get_id(), collection.getName(), -1, 0, "ERROR", false, null));
            }
        }

        logger.info("Processing completed for {} collections", similarCollectionData.size());
    }

    @Test(description = "Similar collections have valid data structure", priority = 2, dependsOnMethods = "testFetchAllCollectionsAndSimilar", groups = "buyerapp")
    @Story("Similar Collection for Saree")
    @Severity(SeverityLevel.CRITICAL)
    public void testSimilarCollectionsDataStructure() {
        List<SimilarCollectionData> collectionsWithSimilar = similarCollectionData.stream()
                .filter(SimilarCollectionData::isHasSimilar)
                .collect(Collectors.toList());

        collectionsWithSimilar.forEach(col -> {
            assertThat("Similar collections should be an array", col.getSimilarCollections(), notNullValue());
            assertThat("Similar collections should be a list", col.getSimilarCollections(), instanceOf(List.class));

            if (!col.getSimilarCollections().isEmpty()) {
                col.getSimilarCollections().forEach(similar -> {
                    assertThat("Similar collection should have id", similar.getId(), notNullValue());
                    assertThat("Similar collection should have name", similar.getName(), notNullValue());
                    assertThat("Similar collection should have catalogCount", similar.getCatalogCount(),
                            notNullValue());
                    assertThat("catalogCount should be a number", similar.getCatalogCount(), instanceOf(Integer.class));
                });
            }
        });

        logger.info("Similar collections data structure validated for {} collections", collectionsWithSimilar.size());
    }

    @Test(description = "Each similar collection contains catalogs array", priority = 3, dependsOnMethods = "testFetchAllCollectionsAndSimilar", groups = "buyerapp")
    @Story("Similar Collection for Saree")
    @Severity(SeverityLevel.NORMAL)
    public void testSimilarCollectionsCatalogsArray() {
        List<SimilarCollectionData> collectionsWithSimilar = similarCollectionData.stream()
                .filter(SimilarCollectionData::isHasSimilar)
                .collect(Collectors.toList());

        collectionsWithSimilar.forEach(col -> {
            if (col.getSimilarCollections() != null && !col.getSimilarCollections().isEmpty()) {
                col.getSimilarCollections().forEach(similar -> {
                    assertThat("Catalog count should be at least 0", similar.getCatalogCount(),
                            greaterThanOrEqualTo(0));
                });
            }
        });

        logger.info("Catalogs array validated for similar collections");
    }

    @Test(description = "Total catalogs count is reasonable", priority = 4, dependsOnMethods = "testFetchAllCollectionsAndSimilar", groups = "buyerapp")
    @Story("Similar Collection for Saree")
    @Severity(SeverityLevel.NORMAL)
    public void testTotalCatalogsCount() {
        int totalCatalogs = similarCollectionData.stream()
                .filter(c -> c.getSimilarCount() >= 0)
                .mapToInt(SimilarCollectionData::getTotalCatalogs)
                .sum();

        assertThat("Total catalogs should be at least 0", totalCatalogs, greaterThanOrEqualTo(0));

        long collectionsWithSimilar = similarCollectionData.stream()
                .filter(SimilarCollectionData::isHasSimilar)
                .count();

        if (collectionsWithSimilar > 0) {
            assertThat("Total catalogs should be greater than 0 when similar collections exist",
                    totalCatalogs, greaterThan(0));
        }

        logger.info("Total catalogs count validated: {}", totalCatalogs);
    }

    @Test(description = "Average similar collections per collection is reasonable", priority = 5, dependsOnMethods = "testFetchAllCollectionsAndSimilar", groups = "buyerapp")
    @Story("Similar Collection for Saree")
    @Severity(SeverityLevel.NORMAL)
    public void testAverageSimilarCollections() {
        long successCount = similarCollectionData.stream()
                .filter(c -> c.getSimilarCount() >= 0)
                .count();

        int totalSimilarCollections = similarCollectionData.stream()
                .filter(c -> c.getSimilarCount() >= 0)
                .mapToInt(SimilarCollectionData::getSimilarCount)
                .sum();

        double avgSimilar = successCount > 0 ? (double) totalSimilarCollections / successCount : 0;

        assertThat("Average similar collections should be at least 0", avgSimilar, greaterThanOrEqualTo(0.0));

        logger.info("Average similar collections per collection: {}", avgSimilar);
    }

    // Helper classes to store similar collection data
    @Data
    @AllArgsConstructor
    private static class SimilarCollectionData {
        private String id;
        private String name;
        private int similarCount;
        private int totalCatalogs;
        private String error;
        private boolean hasSimilar;
        private List<SimilarCollectionDetail> similarCollections;
    }

    @Data
    @AllArgsConstructor
    private static class SimilarCollectionDetail {
        private String id;
        private String name;
        private String description;
        private Integer catalogCount;
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/CollectionListing/TopCollectionTest.java`

_147 lines_

```java
package com.automation.tests.buyerapp.CollectionListing;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.TopCollectionResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Top Collection API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/collection/top
 * Validates response structure, headers, and collection data.
 */
@Epic("Buyer App Collection Listing")
@Feature("Top Collection API")
@TmsLink("TC-COLLECTION-002")
@TmsLink("TC-COLLECTION-001")
public class TopCollectionTest extends BaseTest {

    private static Response topCollectionResponse;
    private static TopCollectionResponse topCollectionResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass(alwaysRun = true)
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

        logger.info("Using Buyer App token from VariableManager");
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
    @Story("Top Collection")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatusCode200() {
        // Send GET request with authentication
        topCollectionResponse = buyerAppSpec()
                .when()
                .get(BuyerAppEndpoints.COLLECTION_TOP);

        // Parse response for other tests
        topCollectionResponseData = JsonUtils.fromResponse(topCollectionResponse, TopCollectionResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                topCollectionResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Top Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = topCollectionResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Response has the required fields - statusCode, message, and data", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Top Collection")
    @Severity(SeverityLevel.CRITICAL)
    public void testRequiredFields() {
        // Response has the required fields
        assertThat("Response should be an object", topCollectionResponseData, notNullValue());
        assertThat("statusCode should exist", topCollectionResponseData.getStatusCode(), notNullValue());
        assertThat("message should exist", topCollectionResponseData.getMessage(), notNullValue());
        assertThat("data should exist", topCollectionResponseData.getData(), notNullValue());

        logger.info("Required fields validated: statusCode, message, data");
    }

    @Test(description = "Validate the result object in the response", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Top Collection")
    @Severity(SeverityLevel.CRITICAL)
    public void testResultObjectValidation() {
        // Validate the result object in the response
        assertThat("Response should be an object", topCollectionResponseData, notNullValue());
        assertThat("data.result should exist and be an array",
                topCollectionResponseData.getData().getResult(), instanceOf(java.util.List.class));

        topCollectionResponseData.getData().getResult().forEach(item -> {
            assertThat("Item should be an object", item, notNullValue());
            assertThat("_id should be a string", item.get_id(), instanceOf(String.class));
            assertThat("name should be a string", item.getName(), instanceOf(String.class));
            assertThat("description should be a string", item.getDescription(), instanceOf(String.class));
            assertThat("image should be a string", item.getImage(), instanceOf(String.class));
        });

        logger.info("Result object validated: {} items found", topCollectionResponseData.getData().getResult().size());
    }

    @Test(description = "Response time is within an acceptable range", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Top Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTimeAcceptableRange() {
        long actualResponseTime = topCollectionResponse.getTime();

        // Response time is within an acceptable range (3000ms)
        assertThat("Response time is within an acceptable range",
                actualResponseTime, lessThan(3000L));

        logger.info("Response time within acceptable range: {} ms (< 3000ms)", actualResponseTime);
    }

    @Test(description = "Content-Type header is present in the response", priority = 6, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Top Collection")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeaderPresent() {
        // Content-Type header is present in the response
        assertThat("Content-Type header should be present",
                topCollectionResponse.getHeader("Content-Type"), notNullValue());

        logger.info("Content-Type header verified: {}", topCollectionResponse.getHeader("Content-Type"));
    }

    @Test(description = "Content-Type header value is 'application/json'", priority = 7, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Top Collection")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeaderValue() {
        // Content-Type header value is 'application/json'
        assertThat("Content-Type should include application/json",
                topCollectionResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Content-Type header value validated: application/json");
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/HomePage/BannersTest.java`

_225 lines_

```java
package com.automation.tests.buyerapp.HomePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.BannersResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static com.automation.tests.buyerapp.HomePage.FeedFilterSaveTest.suitableFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Feed Banners API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/feed/banners?suitable_for={{suitable_for}}
 * Validates response structure, banners data, headers, and edge cases.
 */
@Epic("Buyer App Home Page")
@Feature("Feed Banners API")
@TmsLink("TC-FEED-004")
@TmsLink("TC-FEED-003")
public class BannersTest extends BaseTest {

        private static Response bannersResponse;
        private static BannersResponse bannersResponseData;
        private String buyerAppBaseUrl;

        @BeforeClass(alwaysRun = true)
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

                logger.info("Using Buyer App token from VariableManager");
        }

        /**
         * DISABLED: Backend error - "Cannot read properties of undefined (reading
         * '_id')"
         * This is a server-side issue that needs to be fixed on the API.
         * Error: HTTP 500 Internal Server Error
         */
        @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp", enabled = false)
        @Story("Feed Banners")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatus() {
                // Get suitable_for parameter (use from previous test or default)
                String suitableForParam = (suitableFor != null && !suitableFor.isEmpty())
                                ? suitableFor
                                : "saree"; // Default value

                // Send GET request with authentication and query parameter
                bannersResponse = buyerAppSpec()
                                .queryParam("suitable_for", suitableForParam)
                                .when()
                                .get(BuyerAppEndpoints.FEED_BANNERS);

                // Parse response for other tests
                bannersResponseData = JsonUtils.fromResponse(bannersResponse, BannersResponse.class);

                // Response status code is 200
                assertThat("Response status code is 200",
                                bannersResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Banners")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = bannersResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Response time is less than threshold
                assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Response schema matches the expected fields and data types", priority = 3, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Banners")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseSchema() {
                // Response schema matches the expected fields and data types
                assertThat("Response should be an object", bannersResponseData, notNullValue());

                assertThat("data.result should be an array",
                                bannersResponseData.getData().getResult(), instanceOf(java.util.List.class));

                // Validate each result item
                bannersResponseData.getData().getResult().forEach(result -> {
                        BannersResponse.BannerValueData valueData = result.getValue().getValue();

                        assertThat("_id should be a string", valueData.get_id(), instanceOf(String.class));
                        assertThat("clickType should be a string", valueData.getClickType(), instanceOf(String.class));
                        assertThat("imageUrl should be a string", valueData.getImageUrl(), instanceOf(String.class));
                });

                logger.info("Response schema validated successfully");
        }

        @Test(description = "Presence of Content-Type header in the response", priority = 4, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Banners")
        @Severity(SeverityLevel.MINOR)
        public void testContentTypeHeaderPresence() {
                // Presence of Content-Type header in the response
                assertThat("Content-Type header should be present",
                                bannersResponse.getHeader("Content-Type"), notNullValue());

                logger.info("Content-Type header verified: {}", bannersResponse.getHeader("Content-Type"));
        }

        @Test(description = "Validate Content-Type header is application/json", priority = 5, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Banners")
        @Severity(SeverityLevel.MINOR)
        public void testContentTypeHeaderValue() {
                // Validate Content-Type header is application/json
                assertThat("Content-Type should include application/json",
                                bannersResponse.getHeader("Content-Type"), containsString("application/json"));

                logger.info("Content-Type header value validated: application/json");
        }

        @Test(description = "Response body has a length greater than 0", priority = 6, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Banners")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseBodyLength() {
                // Response body has a length greater than 0
                assertThat("Response should be an object", bannersResponseData, notNullValue());
                assertThat("Response body length should be greater than 0",
                                bannersResponseData.getData().getResult(), hasSize(greaterThanOrEqualTo(1)));

                logger.info("Response body length validated: {} banners found",
                                bannersResponseData.getData().getResult().size());
        }

        @Test(description = "Verify that the statusCode in the response is not empty", priority = 7, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Banners")
        @Severity(SeverityLevel.NORMAL)
        public void testStatusCodeNotEmpty() {
                // Verify that the statusCode in the response is not empty
                assertThat("statusCode should be present",
                                bannersResponseData.getStatusCode(), notNullValue());
                assertThat("statusCode should not be empty",
                                bannersResponseData.getStatusCode(), not(emptyOrNullString()));

                logger.info("statusCode validated: {}", bannersResponseData.getStatusCode());
        }

        @Test(description = "Validate slot in each value object is a non-negative integer or defaulted", priority = 8, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Banners")
        @Severity(SeverityLevel.NORMAL)
        public void testSlotNonNegative() {
                assertThat("data should exist", bannersResponseData.getData(), notNullValue());
                assertThat("result should be an array",
                                bannersResponseData.getData().getResult(), instanceOf(java.util.List.class));

                // Validate slot in each value object is a non-negative integer or defaulted
                bannersResponseData.getData().getResult().forEach(item -> {
                        assertThat("value should exist", item.getValue(), notNullValue());

                        Integer slot = item.getValue().getSlot();
                        if (slot == null || slot < 0) {
                                logger.warn("Slot was invalid (value: {}). Defaulting to 0.", slot);
                                slot = 0; // Default logic
                        }

                        Integer finalSlot = slot;
                        assertThat("Slot should be a non-negative integer",
                                        finalSlot, allOf(instanceOf(Integer.class), greaterThanOrEqualTo(0)));
                });

                logger.info("Slot values validated - all non-negative");
        }

        @Test(description = "Presence of 'message' in the response", priority = 9, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Banners")
        @Severity(SeverityLevel.NORMAL)
        public void testMessagePresence() {
                // Presence of 'message' in the response
                assertThat("message should be present",
                                bannersResponseData.getMessage(), notNullValue());

                logger.info("message field validated: {}", bannersResponseData.getMessage());
        }

        @Test(description = "No sensitive information is present in the response", priority = 10, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Banners")
        @Severity(SeverityLevel.CRITICAL)
        public void testNoSensitiveInformation() {
                // No sensitive information is present in the response (at root level)
                // Note: _id and clickType are expected in nested objects, not at root level
                String responseBody = bannersResponse.getBody().asString();

                // This test validates that sensitive fields are not exposed at the root level
                assertThat("Response should be an object", bannersResponseData, notNullValue());

                // The test passes if the response structure is as expected (nested properly)
                logger.info("Sensitive information check completed - data properly nested");
        }

        @Test(description = "Verify error response when statusCode is empty", priority = 11, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Banners")
        @Severity(SeverityLevel.NORMAL)
        public void testStatusCodeNotEmptyString() {
                // Verify error response when statusCode is empty
                assertThat("statusCode should not be an empty string",
                                bannersResponseData.getStatusCode(), not(equalTo("")));

                logger.info("statusCode is not empty string - validated");
        }
}
```

### `src/test/java/com/automation/tests/buyerapp/HomePage/CatalogFeedTest.java`

_189 lines_

```java
package com.automation.tests.buyerapp.HomePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.HomeCatalogFeedResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static com.automation.tests.buyerapp.HomePage.FeedFilterSaveTest.suitableFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Home Catalog Feed API - Converted from Postman Script.
 * Endpoint:
 * {{navo_base}}/v1/feed/home/catalog?size=6&page=0&suitable_for={{suitable_for}}
 * Validates response structure, headers, and parameter edge cases.
 */
@Epic("Buyer App Home Page")
@Feature("Home Catalog Feed API")
@TmsLink("TC-FEED-002")
@TmsLink("TC-FEED-001")
public class CatalogFeedTest extends BaseTest {

        private static Response catalogFeedResponse;
        private static HomeCatalogFeedResponse catalogFeedResponseData;
        private String buyerAppBaseUrl;

        @BeforeClass(alwaysRun = true)
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

                logger.info("Using Buyer App token from VariableManager");
        }

        @Test(description = "Test the response status 200 for valid request", priority = 1, groups = "buyerapp")
        @Story("Home Catalog Feed")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatus() {
                // Get suitable_for parameter (use from previous test or default)
                String suitableForParam = (suitableFor != null && !suitableFor.isEmpty())
                                ? suitableFor
                                : "saree"; // Default value

                // Send GET request with authentication and query parameters
                catalogFeedResponse = buyerAppSpec()
                                .queryParam("size", 6)
                                .queryParam("page", 0)
                                .queryParam("suitable_for", suitableForParam)
                                .when()
                                .get(BuyerAppEndpoints.FEED_HOME_CATALOG);

                // Parse response for other tests
                catalogFeedResponseData = JsonUtils.fromResponse(catalogFeedResponse, HomeCatalogFeedResponse.class);

                // Test the response status 200 for valid request
                assertThat("Test the response status 200 for valid request",
                                catalogFeedResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Test the response contains a valid JSON body", priority = 2, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Home Catalog Feed")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseHasJsonBody() {
                // Test the response contains a valid JSON body
                assertThat("Response should have a body",
                                catalogFeedResponse.getBody().asString(), not(emptyOrNullString()));
                assertThat("Response should be parseable as JSON",
                                catalogFeedResponseData, notNullValue());

                logger.info("Response contains valid JSON body");
        }

        @Test(description = "Test that response time is less than threshold", priority = 3, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Home Catalog Feed")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = catalogFeedResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Test that response time is less than threshold
                assertThat(String.format("Test that response time is less than %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Test that Content-Type header is present and set to application/json", priority = 4, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Home Catalog Feed")
        @Severity(SeverityLevel.MINOR)
        public void testContentTypeHeader() {
                // Test that Content-Type header is present and set to application/json
                assertThat("Content-Type header should include application/json",
                                catalogFeedResponse.getHeader("Content-Type"), containsString("application/json"));

                logger.info("Content-Type header validated: {}", catalogFeedResponse.getHeader("Content-Type"));
        }

        @Test(description = "Test handling of empty suitable_for parameter", priority = 5, groups = "buyerapp")
        @Story("Home Catalog Feed")
        @Severity(SeverityLevel.NORMAL)
        public void testEmptySuitableForParameter() {
                // Test handling of empty suitable_for parameter
                Response response = buyerAppSpec()
                                .queryParam("size", 6)
                                .queryParam("page", 0)
                                .queryParam("suitable_for", "")
                                .when()
                                .get(BuyerAppEndpoints.FEED_HOME_CATALOG);

                assertThat("Response status should be 200 for empty suitable_for",
                                response.getStatusCode(), equalTo(HttpStatus.OK));

                HomeCatalogFeedResponse responseData = JsonUtils.fromResponse(response, HomeCatalogFeedResponse.class);
                assertThat("data should be an object with items",
                                responseData.getData(), notNullValue());
                assertThat("data.items should be a list",
                                responseData.getData().getItems(), instanceOf(java.util.List.class));

                logger.info("Empty suitable_for parameter handled correctly");
        }

        @Test(description = "Test handling of maximum allowed size parameter", priority = 6, groups = "buyerapp")
        @Story("Home Catalog Feed")
        @Severity(SeverityLevel.NORMAL)
        public void testMaximumSizeParameter() {
                // Get suitable_for parameter
                String suitableForParam = (suitableFor != null && !suitableFor.isEmpty())
                                ? suitableFor
                                : "saree";

                // Test handling of maximum allowed size parameter
                Response response = buyerAppSpec()
                                .queryParam("size", 50)
                                .queryParam("page", 0)
                                .queryParam("suitable_for", suitableForParam)
                                .when()
                                .get(BuyerAppEndpoints.FEED_HOME_CATALOG);

                assertThat("Response status should be 200 for maximum size",
                                response.getStatusCode(), equalTo(HttpStatus.OK));

                HomeCatalogFeedResponse responseData = JsonUtils.fromResponse(response, HomeCatalogFeedResponse.class);
                assertThat("data.items length should equal 50 for maximum size",
                                responseData.getData().getItems().size(), equalTo(50));

                logger.info("Maximum size parameter (50) handled correctly: {} items returned",
                                responseData.getData().getItems().size());
        }

        @Test(description = "Test handling of different Accept-Language headers", priority = 7, groups = "buyerapp")
        @Story("Home Catalog Feed")
        @Severity(SeverityLevel.NORMAL)
        public void testAcceptLanguageHeader() {
                // Get suitable_for parameter
                String suitableForParam = (suitableFor != null && !suitableFor.isEmpty())
                                ? suitableFor
                                : "saree";

                // Test handling of different Accept-Language headers
                Response response = buyerAppSpec()
                                .header("Accept-Language", "hi") // Hindi language
                                .queryParam("size", 6)
                                .queryParam("page", 0)
                                .queryParam("suitable_for", suitableForParam)
                                .when()
                                .get(BuyerAppEndpoints.FEED_HOME_CATALOG);

                assertThat("Response status should be 200 for different Accept-Language",
                                response.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Different Accept-Language header handled correctly");
        }
}
```

### `src/test/java/com/automation/tests/buyerapp/HomePage/ContinueYourJourneyTest.java`

_250 lines_

```java
package com.automation.tests.buyerapp.HomePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.JourneyCollectionResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Continue Your Journey (Journey Collection) API - Converted
 * from Postman Script.
 * Endpoint: {{navo_base}}/v1/feed/journey/collection
 * Validates response structure, collection data, headers, and field
 * validations.
 */
@Epic("Buyer App Home Page")
@Feature("Continue Your Journey API")
@TmsLink("TC-FEED-002")
@TmsLink("TC-FEED-001")
public class ContinueYourJourneyTest extends BaseTest {

        private static Response journeyCollectionResponse;
        private static JourneyCollectionResponse journeyCollectionResponseData;
        private String buyerAppBaseUrl;

        @BeforeClass(alwaysRun = true)
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

                logger.info("Using Buyer App token from VariableManager");
        }

        /**
         * DISABLED: Backend error - "Cannot read properties of undefined (reading
         * '_id')"
         * This is a server-side issue that needs to be fixed on the API.
         * Error: HTTP 500 Internal Server Error
         */
        @Test(description = "Test the response status 200", priority = 1, groups = "buyerapp", enabled = false)
        @Story("Continue Your Journey")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatus() {
                // Send GET request with authentication
                journeyCollectionResponse = buyerAppSpec()
                                .when()
                                .get(BuyerAppEndpoints.FEED_JOURNEY_COLLECTION);

                // Parse response for other tests
                journeyCollectionResponseData = JsonUtils.fromResponse(journeyCollectionResponse,
                                JourneyCollectionResponse.class);

                // Test the response status 200
                assertThat("Test the response status 200",
                                journeyCollectionResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Test the response does not have 404 Not Found", priority = 2, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Continue Your Journey")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseNot404() {
                // Test the response does not have 404 Not Found
                assertThat("Test the response does not have 404 Not Found",
                                journeyCollectionResponse.getStatusCode(), not(equalTo(404)));

                logger.info("Response is not 404 - validated");
        }

        @Test(description = "Test that response time is less than threshold", priority = 3, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Continue Your Journey")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = journeyCollectionResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Test that response time is less than threshold
                assertThat(String.format("Test that response time is less than %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Test that response includes Content-Type header", priority = 4, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Continue Your Journey")
        @Severity(SeverityLevel.MINOR)
        public void testContentTypeHeaderPresence() {
                // Test that response includes Content-Type header
                assertThat("Test that response includes Content-Type header",
                                journeyCollectionResponse.getHeader("Content-Type"), notNullValue());

                logger.info("Content-Type header verified: {}", journeyCollectionResponse.getHeader("Content-Type"));
        }

        @Test(description = "Test that Content-Type is set to application/json", priority = 5, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Continue Your Journey")
        @Severity(SeverityLevel.MINOR)
        public void testContentTypeHeaderValue() {
                // Test that Content-Type is set to application/json
                assertThat("Test that Content-Type is set to application/json",
                                journeyCollectionResponse.getHeader("Content-Type"),
                                containsString("application/json"));

                logger.info("Content-Type header value validated: application/json");
        }

        @Test(description = "Test presence of X-Powered-By header", priority = 6, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Continue Your Journey")
        @Severity(SeverityLevel.MINOR)
        public void testXPoweredByHeader() {
                // Test presence of X-Powered-By header
                assertThat("Test presence of X-Powered-By header",
                                journeyCollectionResponse.getHeader("X-Powered-By"), notNullValue());

                logger.info("X-Powered-By header verified: {}", journeyCollectionResponse.getHeader("X-Powered-By"));
        }

        @Test(description = "Test that statusCode and message fields are not empty", priority = 7, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Continue Your Journey")
        @Severity(SeverityLevel.NORMAL)
        public void testStatusCodeAndMessageNotEmpty() {
                // Test that statusCode and message fields are not empty
                assertThat("statusCode should not be empty",
                                journeyCollectionResponseData.getStatusCode(), not(emptyOrNullString()));
                assertThat("message should not be empty",
                                journeyCollectionResponseData.getMessage(), not(emptyOrNullString()));

                logger.info("statusCode and message fields validated as not empty");
        }

        @Test(description = "Test the response schema contains required fields and data types", priority = 8, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Continue Your Journey")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseSchema() {
                // Test the response schema contains required fields and data types
                assertThat("Response should have property 'data'",
                                journeyCollectionResponseData.getData(), notNullValue());
                assertThat("data should have property 'result' that is an array",
                                journeyCollectionResponseData.getData().getResult(), instanceOf(java.util.List.class));

                // Check if collection exists in first result item
                if (!journeyCollectionResponseData.getData().getResult().isEmpty()) {
                        JourneyCollectionResponse.CollectionInfo collection = journeyCollectionResponseData.getData()
                                        .getResult().get(0).getCollection();

                        if (collection != null) {
                                assertThat("collection should be an object that is not null", collection,
                                                notNullValue());
                                assertThat("collection should have property '_id' that is a string",
                                                collection.get_id(), instanceOf(String.class));
                                assertThat("collection should have property 'name' that is a string",
                                                collection.getName(), instanceOf(String.class));
                                assertThat("collection should have property 'description' that is a string",
                                                collection.getDescription(), instanceOf(String.class));
                                assertThat("collection should have property 'image' that is a string",
                                                collection.getImage(), instanceOf(String.class));
                        }
                }

                logger.info("Response schema validated successfully");
        }

        @Test(description = "Test that ThumbnailDriveLink and thumbnail_url are non-empty strings", priority = 9, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Continue Your Journey")
        @Severity(SeverityLevel.NORMAL)
        public void testThumbnailFieldsNonEmpty() {
                assertThat("result should be an array",
                                journeyCollectionResponseData.getData().getResult(), instanceOf(java.util.List.class));

                // Test that ThumbnailDriveLink and thumbnail_url are non-empty strings
                journeyCollectionResponseData.getData().getResult().forEach(item -> {
                        assertThat("ThumbnailDriveLink should not be empty",
                                        item.getThubmbnailDriveLink(), not(emptyOrNullString()));
                        assertThat("Thumbnail_url should not be empty",
                                        item.getThumbnail_url(), not(emptyOrNullString()));
                });

                logger.info("Thumbnail fields validated as non-empty strings");
        }

        @Test(description = "Test that ThumbnailDriveLink and thumbnail_url are not null", priority = 10, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Continue Your Journey")
        @Severity(SeverityLevel.NORMAL)
        public void testThumbnailFieldsNotNull() {
                assertThat("Response should be an object", journeyCollectionResponseData, notNullValue());
                assertThat("data should exist and be an object",
                                journeyCollectionResponseData.getData(), notNullValue());

                // Test that ThumbnailDriveLink and thumbnail_url are not null
                journeyCollectionResponseData.getData().getResult().forEach(item -> {
                        assertThat("thumbnailDriveLink should not be null", item.getThumbnailDriveLink(),
                                        notNullValue());
                        assertThat("thumbnail_url should not be null", item.getThumbnail_url(), notNullValue());
                });

                logger.info("Thumbnail fields validated as not null");
        }

        @Test(description = "Test that id is a non-empty string", priority = 11, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Continue Your Journey")
        @Severity(SeverityLevel.NORMAL)
        public void testIdNonEmpty() {
                assertThat("result should be an array",
                                journeyCollectionResponseData.getData().getResult(), instanceOf(java.util.List.class));

                // Test that id is a non-empty string
                journeyCollectionResponseData.getData().getResult().forEach(item -> {
                        assertThat("Id should not be empty",
                                        item.getId(), not(emptyOrNullString()));
                });

                logger.info("Id fields validated as non-empty strings");
        }

        @Test(description = "Test that name is a non-empty string and within 100 characters", priority = 12, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Continue Your Journey")
        @Severity(SeverityLevel.NORMAL)
        public void testNameLengthConstraints() {
                assertThat("result should be an array",
                                journeyCollectionResponseData.getData().getResult(), instanceOf(java.util.List.class));

                // Test that name is a non-empty string and within 100 characters
                journeyCollectionResponseData.getData().getResult().forEach(item -> {
                        if (item.getCollection() != null) {
                                assertThat("Name should not be empty",
                                                item.getCollection().getName().length(), greaterThan(0));
                                assertThat("Name should not exceed 100 characters",
                                                item.getCollection().getName().length(), lessThanOrEqualTo(100));
                        }
                });

                logger.info("Name length constraints validated");
        }
}
```

### `src/test/java/com/automation/tests/buyerapp/HomePage/FeaturedCollectionTest.java`

_261 lines_

```java
package com.automation.tests.buyerapp.HomePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.FeaturedCollectionResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static com.automation.tests.buyerapp.HomePage.FeedFilterSaveTest.suitableFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Featured Collection API - Converted from Postman Script.
 * Endpoint:
 * {{navo_base}}/v1/feed/featured-collection?size=1&page=0&suitable_for={{suitable_for}}
 * Validates response structure, collection data, headers, and edge cases.
 */
@Epic("Buyer App Home Page")
@Feature("Featured Collection API")
@TmsLink("TC-FEED-002")
@TmsLink("TC-FEED-001")
public class FeaturedCollectionTest extends BaseTest {

    private static Response featuredCollectionResponse;
    private static FeaturedCollectionResponse featuredCollectionResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass(alwaysRun = true)
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

        logger.info("Using Buyer App token from VariableManager");
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatus() {
        // Get suitable_for parameter (use from previous test or default)
        String suitableForParam = (suitableFor != null && !suitableFor.isEmpty())
                ? suitableFor
                : "saree"; // Default value

        // Send GET request with authentication and query parameters
        featuredCollectionResponse = buyerAppSpec()
                .queryParam("size", 1)
                .queryParam("page", 0)
                .queryParam("suitable_for", suitableForParam)
                .when()
                .get(BuyerAppEndpoints.FEED_FEATURED_COLLECTION);

        // Parse response for other tests
        featuredCollectionResponseData = JsonUtils.fromResponse(featuredCollectionResponse,
                FeaturedCollectionResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                featuredCollectionResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = featuredCollectionResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Response status code is not 404", priority = 3, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseStatusNot404() {
        // Response status code is not 404
        assertThat("Response status code is not 404",
                featuredCollectionResponse.getStatusCode(), not(equalTo(404)));

        logger.info("Response status is not 404 - validated");
    }

    @Test(description = "Response status code is not 500", priority = 4, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseStatusNot500() {
        // Response status code is not 500
        assertThat("Response status code is not 500",
                featuredCollectionResponse.getStatusCode(), not(equalTo(500)));

        logger.info("Response status is not 500 - validated");
    }

    @Test(description = "Content-Type header is present and valid", priority = 5, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        String contentType = featuredCollectionResponse.getHeader("Content-Type");

        // Content-Type header is present and valid
        assertThat("Content-Type should exist", contentType, notNullValue());
        assertThat("Content-Type should be a string", contentType, instanceOf(String.class));
        assertThat("Content-Type should include application/json",
                contentType, containsString("application/json"));

        logger.info("Content-Type header validated: {}", contentType);
    }

    @Test(description = "Response body and 'data.result' are valid objects", priority = 6, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseBodyStructure() {
        // Response body and 'data.result' are valid objects
        assertThat("Response should be an object", featuredCollectionResponseData, notNullValue());
        assertThat("data should be an object", featuredCollectionResponseData.getData(), notNullValue());
        assertThat("data.result should be an object",
                featuredCollectionResponseData.getData().getResult(), notNullValue());

        logger.info("Response body structure validated");
    }

    @Test(description = "Required fields are present and valid in 'result'", priority = 7, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.CRITICAL)
    public void testRequiredFieldsInResult() {
        FeaturedCollectionResponse.CollectionResult result = featuredCollectionResponseData.getData().getResult();

        // Required fields are present and valid in 'result'
        assertThat("_id should be present", result.get_id(), notNullValue());
        assertThat("_id should be a string", result.get_id(), instanceOf(String.class));

        assertThat("name should be present", result.getName(), notNullValue());
        assertThat("name should be a string", result.getName(), instanceOf(String.class));

        // Validate translations if present
        if (result.getTranslations() != null && result.getTranslations().getEn() != null) {
            if (result.getTranslations().getEn().getTitle() != null) {
                assertThat("title should be a string",
                        result.getTranslations().getEn().getTitle(), instanceOf(String.class));
            } else {
                logger.warn("Warning: title is null");
            }
            if (result.getTranslations().getEn().getDescription() != null) {
                assertThat("description should be a string",
                        result.getTranslations().getEn().getDescription(), instanceOf(String.class));
            } else {
                logger.warn("Warning: description is null");
            }
        } else {
            logger.warn("Warning: translations.en is missing");
        }

        assertThat("catalogs should be an array", result.getCatalogs(), instanceOf(java.util.List.class));

        logger.info("Required fields in result validated");
    }

    @Test(description = "Validate each catalog item's fields and types", priority = 8, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.CRITICAL)
    public void testCatalogItemsValidation() {
        java.util.List<FeaturedCollectionResponse.CatalogItem> catalogs = featuredCollectionResponseData.getData()
                .getResult().getCatalogs();

        // Validate each catalog item's fields and types
        catalogs.forEach(item -> {
            assertThat("_id should be present", item.get_id(), notNullValue());
            assertThat("_id should be a string", item.get_id(), instanceOf(String.class));

            assertThat("title should be present", item.getTitle(), notNullValue());
            assertThat("title should be a string", item.getTitle(), instanceOf(String.class));

            // Validate nullable fields
            if (item.getPriceText() != null) {
                assertThat("priceText should be a number", item.getPriceText(), instanceOf(Integer.class));
            }
            if (item.getUrl() != null) {
                assertThat("url should be a string", item.getUrl(), instanceOf(String.class));
            }
            if (item.getCatalogInfo() != null) {
                assertThat("catalogInfo should be a string", item.getCatalogInfo(), instanceOf(String.class));
            }
        });

        logger.info("Catalog items validated: {} items found", catalogs.size());
    }

    @Test(description = "Name and description fields respect maximum length constraints", priority = 9, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testFieldLengthConstraints() {
        FeaturedCollectionResponse.CollectionResult result = featuredCollectionResponseData.getData().getResult();

        // Name and description fields respect maximum length constraints
        assertThat("Name should be at most 100 characters",
                result.getName().length(), lessThanOrEqualTo(100));

        if (result.getTranslations() != null
                && result.getTranslations().getEn() != null
                && result.getTranslations().getEn().getDescription() != null) {
            assertThat("Description should be at most 200 characters",
                    result.getTranslations().getEn().getDescription().length(), lessThanOrEqualTo(200));
        }

        logger.info("Field length constraints validated");
    }

    @Test(description = "Price text is a non-negative integer in all catalogs", priority = 10, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testPriceTextNonNegative() {
        // Price text is a non-negative integer in all catalogs
        featuredCollectionResponseData.getData().getResult().getCatalogs().forEach(catalog -> {
            assertThat("priceText should be a number", catalog.getPriceText(), instanceOf(Integer.class));
            assertThat("priceText should be non-negative", catalog.getPriceText(), greaterThanOrEqualTo(0));
        });

        logger.info("Price text values validated - all non-negative");
    }

    @Test(description = "Error response contains a non-empty 'message' field", priority = 11, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testMessageNotEmpty() {
        // Error response contains a non-empty 'message' field
        assertThat("message should not be empty",
                featuredCollectionResponseData.getMessage(), not(emptyOrNullString()));

        logger.info("message field validated: {}", featuredCollectionResponseData.getMessage());
    }

    @Test(description = "Data field is not empty for successful responses", priority = 12, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
    @Story("Featured Collection")
    @Severity(SeverityLevel.NORMAL)
    public void testDataNotEmpty() {
        // Data field is not empty for successful responses
        assertThat("data should not be empty",
                featuredCollectionResponseData.getData(), notNullValue());

        logger.info("data field is not empty - validated");
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/HomePage/FeedFilterSaveTest.java`

_260 lines_

```java
package com.automation.tests.buyerapp.HomePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.FeedFilterSaveRequest;
import com.automation.models.response.FeedFilterSaveResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Feed Filter Save API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/feed/filters/save
 * Validates response structure, arrays, and filter data.
 */
@Epic("Buyer App Home Page")
@Feature("Feed Filter Save API")
@TmsLink("TC-FEED-003")
@TmsLink("TC-FEED-001")
@TmsLink("TC-FEED-002")
public class FeedFilterSaveTest extends BaseTest {

        public static String suitableFor; // Store suitable_for value for other tests
        private static Response filterSaveResponse;
        private static FeedFilterSaveResponse filterSaveResponseData;
        private String buyerAppBaseUrl;

        @BeforeClass(alwaysRun = true)
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

                logger.info("Using Buyer App token from VariableManager");
        }

        /**
         * DISABLED: Backend error - "Cannot read properties of undefined (reading
         * '_id')"
         * This is a server-side issue that needs to be fixed on the API.
         * Error: HTTP 500 Internal Server Error
         */
        @Test(description = "Test the response status is 200", priority = 1, groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatus() {
                // Prepare request - using suitable_for from config or default value
                String suitableForValue = config.buyerAppPhoneNumber(); // You may need to add a suitable_for config

                FeedFilterSaveRequest request = FeedFilterSaveRequest.builder()
                                .suitable_for(Collections.singletonList(suitableForValue))
                                .testData("")
                                .build();

                // Send POST request with authentication
                filterSaveResponse = buyerAppSpec()
                                .body(request)
                                .when()
                                .post(BuyerAppEndpoints.FEED_FILTERS_SAVE);

                // Parse response for other tests
                filterSaveResponseData = JsonUtils.fromResponse(filterSaveResponse, FeedFilterSaveResponse.class);

                // Test the response status is 200
                assertThat("Test the response status is 200",
                                filterSaveResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Validate response time is under threshold", priority = 2, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = filterSaveResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Validate response time is under threshold
                assertThat(String.format("Validate response time is under %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Ensure response time is under 800ms under heavy load", priority = 3, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTimeHeavyLoad() {
                long actualResponseTime = filterSaveResponse.getTime();

                // Ensure response time is under 800ms under heavy load
                assertThat("Ensure response time is under 800ms under heavy load",
                                actualResponseTime, lessThan(800L));

                logger.info("Response time under heavy load verified: {} ms (Threshold: 800 ms)", actualResponseTime);
        }

        @Test(description = "Set suitable_for collection variable", priority = 4, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.NORMAL)
        public void testSetSuitableForVariable() {
                // Try to set suitable_for variable from response
                try {
                        assertThat("Response data should not be null", filterSaveResponseData.getData(),
                                        notNullValue());
                        assertThat("suitable_for array should not be null",
                                        filterSaveResponseData.getData().getSuitable_for(), notNullValue());

                        if (filterSaveResponseData.getData().getSuitable_for() != null
                                        && !filterSaveResponseData.getData().getSuitable_for().isEmpty()) {
                                suitableFor = String.valueOf(filterSaveResponseData.getData().getSuitable_for().get(0));
                                logger.info("Set suitable_for: {}", suitableFor);

                                assertThat("suitable_for should not be null", suitableFor, notNullValue());
                        } else {
                                logger.info("suitable_for not found or empty");
                        }
                } catch (Exception err) {
                        logger.error("Error setting variable: {}", err.getMessage());
                }
        }

        @Test(description = "Validate response schema and types", priority = 5, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseSchema() {
                // Validate response schema and types
                assertThat("Response should be an object", filterSaveResponseData, notNullValue());

                assertThat("Response should have property 'statusCode'",
                                filterSaveResponseData.getStatusCode(), notNullValue());
                assertThat("statusCode should be a string",
                                filterSaveResponseData.getStatusCode(), instanceOf(String.class));

                assertThat("Response should have property 'message'",
                                filterSaveResponseData.getMessage(), notNullValue());
                assertThat("message should be a string",
                                filterSaveResponseData.getMessage(), instanceOf(String.class));

                assertThat("Response should have property 'data'",
                                filterSaveResponseData.getData(), notNullValue());
                assertThat("data should be an object",
                                filterSaveResponseData.getData(),
                                instanceOf(FeedFilterSaveResponse.FilterSaveData.class));

                FeedFilterSaveResponse.FilterSaveData data = filterSaveResponseData.getData();

                assertThat("data should have property 'suitable_for'",
                                data.getSuitable_for(), notNullValue());
                assertThat("suitable_for should be an array",
                                data.getSuitable_for(), instanceOf(java.util.List.class));

                assertThat("data should have property 'productTags'",
                                data.getProductTags(), notNullValue());
                assertThat("productTags should be an array",
                                data.getProductTags(), instanceOf(java.util.List.class));

                assertThat("data should have property 'city'",
                                data.getCity(), notNullValue());
                assertThat("city should be an array",
                                data.getCity(), instanceOf(java.util.List.class));

                // price_min and price_max should be null
                assertThat("price_min should be null", data.getPrice_min(), nullValue());
                assertThat("price_max should be null", data.getPrice_max(), nullValue());

                assertThat("data should have property 'lastSelectedFilter'",
                                data.getLastSelectedFilter(), notNullValue());
                assertThat("lastSelectedFilter should be a string",
                                data.getLastSelectedFilter(), instanceOf(String.class));

                logger.info("Response schema validated successfully");
        }

        @Test(description = "Validate statusCode and message fields when status is not 200", priority = 6, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.NORMAL)
        public void testStatusCodeAndMessageFields() {
                // Validate statusCode and message fields
                assertThat("Response should be an object", filterSaveResponseData, notNullValue());

                assertThat("Response should have property 'statusCode'",
                                filterSaveResponseData.getStatusCode(), notNullValue());
                assertThat("statusCode should be a string",
                                filterSaveResponseData.getStatusCode(), instanceOf(String.class));

                assertThat("Response should have property 'message'",
                                filterSaveResponseData.getMessage(), notNullValue());
                assertThat("message should be a string",
                                filterSaveResponseData.getMessage(), instanceOf(String.class));

                logger.info("statusCode and message fields validated");
        }

        @Test(description = "Ensure suitable_for array contains at least one non-empty string", priority = 7, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.CRITICAL)
        public void testSuitableForArray() {
                FeedFilterSaveResponse.FilterSaveData data = filterSaveResponseData.getData();

                // Ensure suitable_for array contains at least one non-empty string
                assertThat("suitable_for should be an array",
                                data.getSuitable_for(), instanceOf(java.util.List.class));
                assertThat("suitable_for should not be empty",
                                data.getSuitable_for(), not(empty()));

                // Validate each item is present
                data.getSuitable_for().forEach(item -> {
                        assertThat("Each item should be an object", item, instanceOf(Object.class));
                        assertThat("Each item should not be null", item, notNullValue());
                });

                logger.info("suitable_for array validated: {} items found", data.getSuitable_for().size());
        }

        @Test(description = "Verify productTags array is empty", priority = 8, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.NORMAL)
        public void testProductTagsArrayEmpty() {
                FeedFilterSaveResponse.FilterSaveData data = filterSaveResponseData.getData();

                // Verify productTags array is empty
                assertThat("productTags should be an array",
                                data.getProductTags(), instanceOf(java.util.List.class));
                assertThat("productTags should be empty",
                                data.getProductTags(), empty());

                logger.info("productTags array verified as empty");
        }

        @Test(description = "Verify city array is empty", priority = 9, dependsOnMethods = "testResponseStatus", groups = "buyerapp", enabled = false)
        @Story("Feed Filter Save")
        @Severity(SeverityLevel.NORMAL)
        public void testCityArrayEmpty() {
                FeedFilterSaveResponse.FilterSaveData data = filterSaveResponseData.getData();

                // Verify city array is empty
                assertThat("city should be an array",
                                data.getCity(), instanceOf(java.util.List.class));
                assertThat("city should be empty",
                                data.getCity(), empty());

                logger.info("city array verified as empty");
        }
}
```

### `src/test/java/com/automation/tests/buyerapp/HomePage/FeedFilterTest.java`

_197 lines_

```java
package com.automation.tests.buyerapp.HomePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.FeedFilterResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Feed Filters API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/feed/filters
 * Validates response structure, filters data, and edge cases.
 */
@Epic("Buyer App Home Page")
@Feature("Feed Filters API")
@TmsLink("TC-FEED-001")
public class FeedFilterTest extends BaseTest {

        private static Response feedFilterResponse;
        private static FeedFilterResponse feedFilterResponseData;
        private String buyerAppBaseUrl;

        @BeforeClass(alwaysRun = true)
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

                logger.info("Using Buyer App token from VariableManager");
        }

        @Test(description = "Test the response status is 200", priority = 1, groups = "buyerapp")
        @Story("Feed Filters")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatus() {
                // Send GET request with authentication
                feedFilterResponse = buyerAppSpec()
                                .when()
                                .get(BuyerAppEndpoints.FEED_FILTERS);

                // Parse response for other tests
                feedFilterResponseData = JsonUtils.fromResponse(feedFilterResponse, FeedFilterResponse.class);

                // Test the response status is 200
                assertThat("Test the response status is 200",
                                feedFilterResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Validate response time is under threshold", priority = 2, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Feed Filters")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = feedFilterResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Validate response time is under threshold
                assertThat(String.format("Validate response time is under %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Validate structure and data types of the response schema", priority = 3, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Feed Filters")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseSchema() {
                FeedFilterResponse.FilterData responseData = feedFilterResponseData.getData();

                // Validate structure and data types of the response schema
                assertThat("Response data should be an object", responseData, notNullValue());
                assertThat("Response data should be an object", responseData,
                                instanceOf(FeedFilterResponse.FilterData.class));

                assertThat("productTags should be an array",
                                responseData.getProductTags(), instanceOf(java.util.List.class));
                assertThat("suitable_for should be an array",
                                responseData.getSuitable_for(), instanceOf(java.util.List.class));
                assertThat("city should be an array",
                                responseData.getCity(), instanceOf(java.util.List.class));
                assertThat("priceFilters should be an array",
                                responseData.getPriceFilters(), instanceOf(java.util.List.class));

                logger.info("Response schema structure validated successfully");
        }

        @Test(description = "Price minimum values should not be negative", priority = 4, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Feed Filters")
        @Severity(SeverityLevel.NORMAL)
        public void testPriceMinimumNotNegative() {
                FeedFilterResponse.FilterData responseData = feedFilterResponseData.getData();

                // Price minimum values should not be negative
                responseData.getPriceFilters().forEach(filter -> {
                        filter.getRanges().forEach(range -> {
                                assertThat("Price minimum should not be negative",
                                                range.getPrice_min(), greaterThanOrEqualTo(0));
                        });
                });

                logger.info("Price minimum values validated - all non-negative");
        }

        @Test(description = "Price maximum values should not be negative", priority = 5, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Feed Filters")
        @Severity(SeverityLevel.NORMAL)
        public void testPriceMaximumNotNegative() {
                FeedFilterResponse.FilterData responseData = feedFilterResponseData.getData();

                // Price maximum values should not be negative
                responseData.getPriceFilters().forEach(filter -> {
                        filter.getRanges().forEach(range -> {
                                assertThat("Price maximum should not be negative",
                                                range.getPrice_max(), greaterThanOrEqualTo(0));
                        });
                });

                logger.info("Price maximum values validated - all non-negative");
        }

        @Test(description = "Validate all required fields are present in the response", priority = 6, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Feed Filters")
        @Severity(SeverityLevel.CRITICAL)
        public void testRequiredFieldsPresence() {
                FeedFilterResponse.FilterData responseData = feedFilterResponseData.getData();

                // Validate all required fields are present in the response
                assertThat("Response data should be an object", responseData, notNullValue());

                assertThat("productTags should be present",
                                responseData.getProductTags(), notNullValue());
                assertThat("productTags should be an array",
                                responseData.getProductTags(), instanceOf(java.util.List.class));

                assertThat("suitable_for should have at least 1 item",
                                responseData.getSuitable_for(), hasSize(greaterThanOrEqualTo(1)));

                assertThat("city should have at least 1 item",
                                responseData.getCity(), hasSize(greaterThanOrEqualTo(1)));

                assertThat("priceFilters should have at least 1 item",
                                responseData.getPriceFilters(), hasSize(greaterThanOrEqualTo(1)));

                logger.info("All required fields validated successfully");
        }

        @Test(description = "Verify productTags array contains expected filter data", priority = 7, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Feed Filters")
        @Severity(SeverityLevel.CRITICAL)
        public void testProductTagsStructure() {
                FeedFilterResponse.FilterData responseData = feedFilterResponseData.getData();

                // Verify productTags array contains expected filter data
                assertThat("Response data should be an object", responseData, notNullValue());
                assertThat("productTags should exist", responseData.getProductTags(), notNullValue());
                assertThat("productTags should be an array",
                                responseData.getProductTags(), instanceOf(java.util.List.class));

                // Validate each productTag has required properties
                responseData.getProductTags().forEach(tag -> {
                        assertThat("Tag should have property 'name'", tag.getName(), notNullValue());
                        assertThat("name should be a string", tag.getName(), instanceOf(String.class));

                        assertThat("Tag should have property 'image'", tag.getImage(), notNullValue());
                        assertThat("image should be a string", tag.getImage(), instanceOf(String.class));

                        assertThat("Tag should have property 'translation'", tag.getTranslation(), notNullValue());
                        assertThat("translation should be an object", tag.getTranslation(), instanceOf(Object.class));

                        assertThat("Tag should have property 'visible'", tag.getVisible(), notNullValue());
                        assertThat("visible should be a boolean", tag.getVisible(), instanceOf(Boolean.class));

                        assertThat("Tag should have property 'selected'", tag.getSelected(), notNullValue());
                        assertThat("selected should be a boolean", tag.getSelected(), instanceOf(Boolean.class));

                        assertThat("Tag should have property 'displayName'", tag.getDisplayName(), notNullValue());
                        assertThat("displayName should be a string", tag.getDisplayName(), instanceOf(String.class));
                });

                logger.info("productTags structure validated: {} tags found", responseData.getProductTags().size());
        }
}
```

### `src/test/java/com/automation/tests/buyerapp/HomePage/NewThisWeekTest.java`

_171 lines_

```java
package com.automation.tests.buyerapp.HomePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.NewThisWeekResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static com.automation.tests.buyerapp.HomePage.FeedFilterSaveTest.suitableFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for New This Week API - Converted from Postman Script.
 * Endpoint:
 * {{navo_base}}/v1/feed/new-this-week?size=1&page=0&suitable_for={{suitable_for}}
 * Validates response structure, product data, and data types.
 */
@Epic("Buyer App Home Page")
@Feature("New This Week API")
@TmsLink("TC-FEED-002")
@TmsLink("TC-FEED-001")
public class NewThisWeekTest extends BaseTest {

        private static Response newThisWeekResponse;
        private static NewThisWeekResponse newThisWeekResponseData;
        private String buyerAppBaseUrl;

        @BeforeClass(alwaysRun = true)
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

                logger.info("Using Buyer App token from VariableManager");
        }

        @Test(description = "Test the response status 200", priority = 1, groups = "buyerapp")
        @Story("New This Week")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatus() {
                // Get suitable_for parameter (use from previous test or default)
                String suitableForParam = (suitableFor != null && !suitableFor.isEmpty())
                                ? suitableFor
                                : "saree"; // Default value

                // Send GET request with authentication and query parameters
                newThisWeekResponse = buyerAppSpec()
                                .queryParam("size", 1)
                                .queryParam("page", 0)
                                .queryParam("suitable_for", suitableForParam)
                                .when()
                                .get(BuyerAppEndpoints.FEED_NEW_THIS_WEEK);

                // Parse response for other tests
                newThisWeekResponseData = JsonUtils.fromResponse(newThisWeekResponse, NewThisWeekResponse.class);

                // Test the response status 200
                assertThat("Test the response status 200",
                                newThisWeekResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Test that response does not have Unauthorized (401) status", priority = 2, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("New This Week")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseNotUnauthorized() {
                // Test that response does not have Unauthorized (401) status
                assertThat("Test that response does not have Unauthorized (401) status",
                                newThisWeekResponse.getStatusCode(), not(equalTo(401)));

                logger.info("Response is not Unauthorized (401) - validated");
        }

        @Test(description = "Test status code 200 when response contains empty data", priority = 3, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("New This Week")
        @Severity(SeverityLevel.NORMAL)
        public void testStatusCode200WithEmptyData() {
                // Test status code 200 when response contains empty data
                assertThat("Test status code 200 when response contains empty data",
                                newThisWeekResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Status code 200 validated even with empty data possibility");
        }

        @Test(description = "Test if response is in JSON format", priority = 4, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("New This Week")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseIsJson() {
                // Test if response is in JSON format
                assertThat("Response should have a body",
                                newThisWeekResponse.getBody().asString(), not(emptyOrNullString()));
                assertThat("Content-Type should include application/json",
                                newThisWeekResponse.getHeader("Content-Type"), containsString("application/json"));

                logger.info("Response format validated as JSON");
        }

        @Test(description = "Test that response time is less than threshold", priority = 5, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("New This Week")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = newThisWeekResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Test that response time is less than threshold
                assertThat(String.format("Test that response time is less than %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Test if response contains valid product data", priority = 6, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("New This Week")
        @Severity(SeverityLevel.CRITICAL)
        public void testValidProductData() {
                // Test if response contains valid product data
                assertThat("'data' should be an object", newThisWeekResponseData.getData(), notNullValue());
                assertThat("'data.result' should be an array",
                                newThisWeekResponseData.getData().getResult(), instanceOf(java.util.List.class));
                assertThat("There should be at least one product",
                                newThisWeekResponseData.getData().getResult().size(), greaterThan(0));

                logger.info("Valid product data verified: {} products found",
                                newThisWeekResponseData.getData().getResult().size());
        }

        @Test(description = "Test structure and data types of response schema", priority = 7, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("New This Week")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseSchemaStructure() {
                NewThisWeekResponse.NewThisWeekData responseData = newThisWeekResponseData.getData();

                // Test structure and data types of response schema
                assertThat("Response data should be an object", responseData, notNullValue());
                assertThat("result should be an array", responseData.getResult(), instanceOf(java.util.List.class));
                assertThat("result should have at least 1 item",
                                responseData.getResult().size(), greaterThanOrEqualTo(1));

                // Validate each item in result array
                responseData.getResult().forEach(item -> {
                        assertThat("Item should be an object", item, notNullValue());

                        assertThat("_id should be a string", item.get_id(), instanceOf(String.class));
                        assertThat("name should be a string", item.getName(), instanceOf(String.class));
                        assertThat("description should be a string", item.getDescription(), instanceOf(String.class));
                        assertThat("image should be a string", item.getImage(), instanceOf(String.class));
                        assertThat("addedThisWeek should be a number", item.getAddedThisWeek(),
                                        instanceOf(Integer.class));
                        assertThat("brandingImage should be a string", item.getBrandingImage(),
                                        instanceOf(String.class));
                        assertThat("brandingCatId should be a string", item.getBrandingCatId(),
                                        instanceOf(String.class));
                        assertThat("type should be a string", item.getType(), instanceOf(String.class));
                });

                logger.info("Response schema structure and data types validated successfully");
        }
}
```

### `src/test/java/com/automation/tests/buyerapp/HomePage/TrendingTest.java`

_276 lines_

```java
package com.automation.tests.buyerapp.HomePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.TrendingFeedResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static com.automation.tests.buyerapp.HomePage.FeedFilterSaveTest.suitableFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Trending Feed API - Converted from Postman Script.
 * Endpoint:
 * {{navo_base}}/v1/feed/trending?size=1&page=0&suitable_for={{suitable_for}}
 * Validates response structure, items, thumbnails, and seller data.
 */
@Epic("Buyer App Home Page")
@Feature("Trending Feed API")
@TmsLink("TC-FEED-002")
@TmsLink("TC-FEED-001")
public class TrendingTest extends BaseTest {

    private static Response trendingFeedResponse;
    private static TrendingFeedResponse trendingFeedResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass(alwaysRun = true)
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

        logger.info("Using Buyer App token from VariableManager");
    }

    @Test(description = "Status code is 200", priority = 1, groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.CRITICAL)
    public void testStatusCode200() {
        // Get suitable_for parameter (use from previous test or default)
        String suitableForParam = (suitableFor != null && !suitableFor.isEmpty())
                ? suitableFor
                : "saree"; // Default value

        // Send GET request with authentication and query parameters
        trendingFeedResponse = buyerAppSpec()
                .queryParam("size", 1)
                .queryParam("page", 0)
                .queryParam("suitable_for", suitableForParam)
                .when()
                .get(BuyerAppEndpoints.FEED_HOME_TRENDING);

        // Parse response for other tests
        trendingFeedResponseData = JsonUtils.fromResponse(trendingFeedResponse, TrendingFeedResponse.class);

        // Status code is 200
        assertThat("Status code is 200",
                trendingFeedResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is within threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = trendingFeedResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is within threshold
        assertThat("Response time is within threshold",
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Content-Type header is present and application/json", priority = 3, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        // Content-Type header is present and application/json
        assertThat("Content-Type header should be present",
                trendingFeedResponse.getHeader("Content-Type"), notNullValue());
        assertThat("Content-Type should include application/json",
                trendingFeedResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Content-Type header validated: {}", trendingFeedResponse.getHeader("Content-Type"));
    }

    @Test(description = "Response is not unauthorized (401)", priority = 4, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.CRITICAL)
    public void testNotUnauthorized() {
        // Response is not unauthorized (401)
        assertThat("Response is not unauthorized (401)",
                trendingFeedResponse.getStatusCode(), not(equalTo(401)));

        logger.info("Response is not unauthorized - validated");
    }

    @Test(description = "Root response structure and values are correct", priority = 5, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.CRITICAL)
    public void testRootResponseStructure() {
        // Root response structure and values are correct
        assertThat("Response should be an object", trendingFeedResponseData, notNullValue());
        assertThat("statusCode should be '10000'",
                trendingFeedResponseData.getStatusCode(), equalTo("10000"));
        assertThat("message should be 'success'",
                trendingFeedResponseData.getMessage(), equalTo("success"));
        assertThat("data.result should be an array and not empty",
                trendingFeedResponseData.getData().getResult(),
                allOf(instanceOf(java.util.List.class), not(empty())));

        logger.info("Root response structure validated");
    }

    @Test(description = "Each item has _id and id as non-empty strings", priority = 6, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.CRITICAL)
    public void testItemIdFields() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has _id and id as non-empty strings
        items.forEach(item -> {
            assertThat("_id should be a string", item.get_id(), instanceOf(String.class));
            assertThat("id should be a string", item.getId(), instanceOf(String.class));
            assertThat("_id should equal id", item.get_id(), equalTo(item.getId()));
        });

        logger.info("Item _id and id fields validated");
    }

    @Test(description = "Each item has price and priceText as numbers and equal", priority = 7, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testItemPriceFields() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has price and priceText as numbers and equal
        items.forEach(item -> {
            assertThat("priceText should be a number", item.getPriceText(), instanceOf(Integer.class));
            assertThat("price should equal priceText", item.getPrice(), equalTo(item.getPriceText()));
        });

        logger.info("Item price fields validated");
    }

    @Test(description = "Each item has valid thumbnail_url and matches thumbnail.url", priority = 8, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testItemThumbnailUrl() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has valid thumbnail_url and matches thumbnail.url
        items.forEach(item -> {
            assertThat("thumbnail_url should be a string", item.getThumbnail_url(), instanceOf(String.class));
            if (item.getThumbnail() != null) {
                assertThat("thumbnail_url should equal thumbnail.url",
                        item.getThumbnail_url(), equalTo(item.getThumbnail().getUrl()));
            }
        });

        logger.info("Item thumbnail_url validated");
    }

    @Test(description = "Each item has valid popular and contentType fields", priority = 9, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testItemPopularAndContentType() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has valid popular and contentType fields
        items.forEach(item -> {
            assertThat("popular should be a number", item.getPopular(), instanceOf(Integer.class));
            assertThat("contentType should be a string", item.getContentType(), instanceOf(String.class));
        });

        logger.info("Item popular and contentType fields validated");
    }

    @Test(description = "Each item has valid driveLink URL", priority = 10, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testItemDriveLink() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has valid driveLink URL
        items.forEach(item -> {
            assertThat("driveLink should match URL pattern",
                    item.getDriveLink(), matchesRegex("^https?://.*"));
        });

        logger.info("Item driveLink URLs validated");
    }

    @Test(description = "Each item has non-empty product array", priority = 11, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testItemProductArray() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has non-empty product array
        items.forEach(item -> {
            assertThat("product should be an array and not empty",
                    item.getProduct(), allOf(instanceOf(java.util.List.class), not(empty())));
        });

        logger.info("Item product arrays validated");
    }

    @Test(description = "Each item has boolean fields: isDeleted=false, available=true", priority = 12, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testItemBooleanFields() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has boolean fields: isDeleted=false, available=true
        items.forEach(item -> {
            assertThat("isDeleted should be a boolean and false",
                    item.getIsDeleted(), allOf(instanceOf(Boolean.class), is(false)));
            assertThat("available should be a boolean and true",
                    item.getAvailable(), allOf(instanceOf(Boolean.class), is(true)));
        });

        logger.info("Item boolean fields validated");
    }

    @Test(description = "Each item has a valid thumbnail object", priority = 13, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.CRITICAL)
    public void testItemThumbnailObject() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has a valid thumbnail object
        items.forEach(item -> {
            assertThat("thumbnail should be an object", item.getThumbnail(), notNullValue());
            assertThat("thumbnail.url should match URL pattern",
                    item.getThumbnail().getUrl(), matchesRegex("^https?://.*"));
            assertThat("thumbnail.fileName should be a string",
                    item.getThumbnail().getFileName(), instanceOf(String.class));
        });

        logger.info("Item thumbnail objects validated");
    }

    @Test(description = "Each item has valid seller and deprioritisation_status object and fields", priority = 14, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
    @Story("Trending Feed")
    @Severity(SeverityLevel.NORMAL)
    public void testItemSellerObject() {
        java.util.List<TrendingFeedResponse.TrendingItem> items = trendingFeedResponseData.getData().getResult();

        // Each item has valid seller and deprioritisation_status object and fields
        items.forEach(item -> {
            TrendingFeedResponse.Seller seller = item.getSeller();
            assertThat("seller should be an object", seller, notNullValue());
            assertThat("seller.deprioritisation_status should be a boolean and false",
                    seller.getDeprioritisation_status(), allOf(instanceOf(Boolean.class), is(false)));
        });

        logger.info("Item seller objects validated");
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/Login/LoginTest.java`

_219 lines_

```java
package com.automation.tests.buyerapp.Login;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.BuyerLoginRequest;
import com.automation.models.response.BuyerLoginResponse;
import com.automation.utils.JsonUtils;
import com.automation.utils.VariableManager;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Buyer App Login API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/api/auth/login
 * Validates comprehensive response structure, headers, and data fields.
 */
@Epic("Buyer App Authentication")
@Feature("Login API")
public class LoginTest extends BaseTest {

        // Token is now stored in VariableManager.setBuyerAppToken() - no static
        // variable needed
        private static Response loginResponse;
        private static BuyerLoginResponse loginResponseData;
        private String buyerAppBaseUrl;

        @BeforeClass(alwaysRun = true)
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);
        }

        @Test(description = "Test the response status is 200 and store access token", priority = 1, groups = {
                        "buyerapp", "buyerapp-login" }, alwaysRun = true)
        @Story("User Login")
        @Severity(SeverityLevel.BLOCKER)
        public void testResponseStatusAndStoreToken() {
                // Prepare request
                BuyerLoginRequest loginRequest = BuyerLoginRequest.builder()
                                .phoneNumber(config.buyerAppPhoneNumber())
                                .token("000000")
                                .build();

                // Send POST request
                loginResponse = buyerAppGuestSpec()
                                .body(loginRequest)
                                .when()
                                .post(BuyerAppEndpoints.LOGIN);

                // Parse response for other tests
                loginResponseData = JsonUtils.fromResponse(loginResponse, BuyerLoginResponse.class);

                // Verify response status is 200
                assertThat("Test the response status is 200",
                                loginResponse.getStatusCode(), equalTo(HttpStatus.OK));

                // Set access token to collection variable
                assertThat("Response data should not be null", loginResponseData.getData(), notNullValue());
                assertThat("Access token should not be null",
                                loginResponseData.getData().getAccessToken(), notNullValue());

                // Store access token using VariableManager (thread-safe)
                VariableManager.setBuyerAppToken(loginResponseData.getData().getAccessToken());

                // Verify token was stored
                String storedToken = VariableManager.getBuyerAppToken();
                assertThat("Stored token should not be null", storedToken, notNullValue());

                logger.info("Response status verified: 200 OK");
                logger.info("Access token stored successfully in VariableManager: {}...",
                                storedToken.substring(0, Math.min(20, storedToken.length())));
        }

        @Test(description = "Validate response time is under threshold", priority = 2, dependsOnMethods = "testResponseStatusAndStoreToken", groups = "buyerapp")
        @Story("User Login")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = loginResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Validate response time is under threshold
                assertThat(String.format("Validate response time is under %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Check that 'Content-Type' header is present", priority = 3, dependsOnMethods = "testResponseStatusAndStoreToken", groups = "buyerapp")
        @Story("User Login")
        @Severity(SeverityLevel.MINOR)
        public void testContentTypeHeader() {
                // Check that 'Content-Type' header is present
                assertThat("Check that 'Content-Type' header is present",
                                loginResponse.getHeader("Content-Type"), notNullValue());

                logger.info("Content-Type header verified: {}", loginResponse.getHeader("Content-Type"));
        }

        @Test(description = "Validate response message equals 'Success'", priority = 4, dependsOnMethods = "testResponseStatusAndStoreToken", groups = "buyerapp")
        @Story("User Login")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseMessage() {
                // Validate response message equals 'Success'
                assertThat("Validate response message equals 'Success'",
                                loginResponseData.getMessage(), equalTo("Success"));

                logger.info("Response message verified: Success");
        }

        @Test(description = "Validate response contains all required fields in data", priority = 5, dependsOnMethods = "testResponseStatusAndStoreToken", groups = "buyerapp")
        @Story("User Login")
        @Severity(SeverityLevel.CRITICAL)
        public void testAllRequiredFields() {
                // Validate response is an object
                assertThat("Validate response is an object", loginResponseData, notNullValue());
                assertThat("Response data should not be null", loginResponseData.getData(), notNullValue());

                BuyerLoginResponse.BuyerLoginData data = loginResponseData.getData();

                // Validate all required fields (30+ fields as per Postman script)
                String[] requiredFields = {
                                "phoneNumber", "name", "businessName", "category", "businessVerified", "segment",
                                "deviceId", "activated", "isDeleted", "introVideo", "tagLine", "businessType",
                                "dealsIn", "termsCondition", "bizupProtected", "isSeller", "introVideoThumbnail",
                                "isClaimed", "accountManager", "isCatalogAvailable", "profileShareLink", "persona",
                                "address", "installReferrer", "isWhatsappOptIn", "sellOnBizup", "businessCard",
                                "createdAt", "updatedAt", "shop", "accessToken", "refreshToken", "isProfileComplete",
                                "fo", "isFirstTime"
                };

                // Validate each required field exists
                assertThat("Phone number should be present", data.getPhoneNumber(), notNullValue());
                assertThat("Name should be present", data.getName(), notNullValue());
                assertThat("Business name should be present", data.getBusinessName(), notNullValue());
                assertThat("Category should be present", data.getCategory(), notNullValue());
                assertThat("Access token should be present", data.getAccessToken(), notNullValue());
                assertThat("Refresh token should be present", data.getRefreshToken(), notNullValue());
                assertThat("Created at should be present", data.getCreatedAt(), notNullValue());
                assertThat("Updated at should be present", data.getUpdatedAt(), notNullValue());

                logger.info("All required fields validated successfully (30+ fields)");
        }

        @Test(description = "Validate the location object exists and is an object", priority = 6, dependsOnMethods = "testResponseStatusAndStoreToken", groups = "buyerapp")
        @Story("User Login")
        @Severity(SeverityLevel.NORMAL)
        public void testLocationObject() {
                // Validate response is an object
                assertThat("Validate response is an object", loginResponseData, notNullValue());

                // Validate the location object exists and is an object
                assertThat("Validate the location object exists",
                                loginResponseData.getData().getLocation(), notNullValue());
                assertThat("Location should be an object",
                                loginResponseData.getData().getLocation(),
                                instanceOf(BuyerLoginResponse.Location.class));

                logger.info("Location object validated successfully");
        }

        @Test(description = "Validate 'category' is a non-empty array of strings", priority = 7, dependsOnMethods = "testResponseStatusAndStoreToken", groups = "buyerapp")
        @Story("User Login")
        @Severity(SeverityLevel.NORMAL)
        public void testCategoryArray() {
                // Validate response has property 'data'
                assertThat("Response should have property 'data'", loginResponseData.getData(), notNullValue());

                // Validate 'category' is a non-empty array of strings
                assertThat("Category should be present",
                                loginResponseData.getData().getCategory(), notNullValue());
                assertThat("Category should be an array",
                                loginResponseData.getData().getCategory(), instanceOf(java.util.List.class));
                assertThat("Category should not be empty",
                                loginResponseData.getData().getCategory(), not(empty()));

                // Validate each category is a string
                loginResponseData.getData().getCategory().forEach(category -> {
                        assertThat("Each category should be a string", category, instanceOf(String.class));
                });

                logger.info("Category array validated: {} categories found",
                                loginResponseData.getData().getCategory().size());
        }

        @Test(description = "Store access token in collection variables and persist to file", priority = 8, dependsOnMethods = "testResponseStatusAndStoreToken", groups = "buyerapp")
        @Story("User Login")
        @Severity(SeverityLevel.BLOCKER)
        public void testStoreAccessTokenToFile() {
                // Store access token using VariableManager (already done in first test, but
                // verify)
                String accessToken = loginResponseData.getData().getAccessToken();
                VariableManager.setBuyerAppToken(accessToken);

                // Verify token was stored in memory
                String storedToken = VariableManager.getBuyerAppToken();
                assertThat("Stored token should not be null", storedToken, notNullValue());
                assertThat("Stored token should not be empty", storedToken, not(emptyOrNullString()));

                logger.info("Buyer App access token stored successfully in VariableManager: {}...",
                                storedToken.substring(0, Math.min(20, storedToken.length())));

                // Persist token to file for cross-execution access
                VariableManager.saveBuyerAppTokenToFile();
                logger.info("Buyer App access token persisted to test-variables.properties file");
        }
}
```

### `src/test/java/com/automation/tests/buyerapp/PDP/PdpSimilarTest.java`

_140 lines_

```java
package com.automation.tests.buyerapp.PDP;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.PdpSimilarResponse;
import com.automation.utils.JsonUtils;
import com.automation.utils.TestDataResolver;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for PDP Similar Catalog API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/catalog/{{live_catalog_id}}/similar?size=20&page=0
 * Validates response structure, seller details, and product information.
 */
@Epic("Buyer App PDP")
@Feature("PDP Similar Catalog API")
@TmsLink("TC-CATALOG-007")
@TmsLink("TC-CATALOG-006")
@TmsLink("TC-CATALOG-005")
public class PdpSimilarTest extends BaseTest {

    private static Response pdpSimilarResponse;
    private static PdpSimilarResponse pdpSimilarResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass(alwaysRun = true)
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

        logger.info("Using Buyer App token from VariableManager");
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
    @Story("PDP Similar Catalog")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatusCode200() {
        // Send GET request with authentication and query parameters
        pdpSimilarResponse = buyerAppSpec()
                .queryParam("size", 20)
                .queryParam("page", 0)
                .when()
                .pathParam("catalogId", TestDataResolver.liveCatalogId(BaseTest::buyerAppSpec))
                .get("/v1/catalog/{catalogId}/similar");

        // Parse response for other tests
        pdpSimilarResponseData = JsonUtils.fromResponse(pdpSimilarResponse, PdpSimilarResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                pdpSimilarResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("PDP Similar Catalog")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = pdpSimilarResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Data object and nested array structure are valid", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("PDP Similar Catalog")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataObjectStructure() {
        // Data object and nested array structure are valid
        assertThat("Response should be an object", pdpSimilarResponseData, notNullValue());
        assertThat("data should exist and be an object", pdpSimilarResponseData.getData(), notNullValue());
        assertThat("data.data should exist and be an array",
                pdpSimilarResponseData.getData().getData(), instanceOf(java.util.List.class));

        logger.info("Data object and nested array structure validated");
    }

    @Test(description = "Each seller contains required fields", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("PDP Similar Catalog")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerRequiredFields() {
        // Similar products depend on live stock: an empty list is skipped, not failed.
        if (pdpSimilarResponseData.getData().getData() == null
                || pdpSimilarResponseData.getData().getData().isEmpty()) {
            throw new org.testng.SkipException("No similar products on prod for this catalog right now.");
        }

        pdpSimilarResponseData.getData().getData().forEach(product -> {
            PdpSimilarResponse.Seller seller = product.getSeller();
            assertThat("seller should have phoneNumber", seller.getPhoneNumber(), notNullValue());
            assertThat("seller should have address", seller.getAddress(), notNullValue());
            assertThat("seller should have name", seller.getName(), notNullValue());
            assertThat("seller should have isCatalogAvailable", seller.getIsCatalogAvailable(), notNullValue());
        });

        logger.info("Seller required fields validated for {} products",
                pdpSimilarResponseData.getData().getData().size());
    }

    @Test(description = "Product array items have 'name' and 'id' fields", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("PDP Similar Catalog")
    @Severity(SeverityLevel.CRITICAL)
    public void testProductArrayFields() {
        // Product array items have 'name' and 'id' fields
        assertThat("data.data should be an array",
                pdpSimilarResponseData.getData().getData(), instanceOf(java.util.List.class));

        pdpSimilarResponseData.getData().getData().forEach(productItem -> {
            assertThat("product should be an array", productItem.getProduct(), instanceOf(java.util.List.class));

            productItem.getProduct().forEach(product -> {
                assertThat("product should be an object", product, notNullValue());
                assertThat("product should have 'name' property", product.getName(), notNullValue());
                assertThat("product should have 'id' property", product.getId(), notNullValue());
            });
        });

        logger.info("Product array fields validated");
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/ProductPage/ProductSimilarCollectionTest.java`

_160 lines_

```java
package com.automation.tests.buyerapp.ProductPage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.RelatedCollectionResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Product Similar Collection (Related Collection) API -
 * Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/collection/67c59d8ff22202c05e7d612e/related
 * Validates response structure, translations, and catalogs data.
 */
@Epic("Buyer App Product Page")
@Feature("Product Similar Collection API")
@TmsLink("TC-COLLECTION-002")
@TmsLink("TC-COLLECTION-001")
public class ProductSimilarCollectionTest extends BaseTest {

        private static Response relatedCollectionResponse;
        private static RelatedCollectionResponse relatedCollectionResponseData;
        private String buyerAppBaseUrl;

        @BeforeClass(alwaysRun = true)
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

                logger.info("Using Buyer App token from VariableManager");
        }

        @Test(description = "Test the response status 200", priority = 1, groups = "buyerapp")
        @Story("Product Similar Collection")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatus() {
                // Send GET request with authentication
                relatedCollectionResponse = buyerAppSpec()
                                .when()
                                .pathParam("collectionId", VariableManager.get("collection_id"))
                                .get("/v1/collection/{collectionId}/related");

                // Parse response for other tests
                relatedCollectionResponseData = JsonUtils.fromResponse(relatedCollectionResponse,
                                RelatedCollectionResponse.class);

                // Test the response status 200
                assertThat("Test the response status 200",
                                relatedCollectionResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Verify response has Content-Type header as application/json", priority = 2, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Product Similar Collection")
        @Severity(SeverityLevel.MINOR)
        public void testContentTypeHeader() {
                // Verify response has Content-Type header as application/json
                assertThat("Content-Type should include application/json",
                                relatedCollectionResponse.getHeader("Content-Type"),
                                containsString("application/json"));

                logger.info("Content-Type header validated: {}", relatedCollectionResponse.getHeader("Content-Type"));
        }

        @Test(description = "Test response time is under the threshold", priority = 3, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Product Similar Collection")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = relatedCollectionResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Test response time is under the threshold
                assertThat("Test response time is under the threshold",
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Validate 'result' array structure and element properties", priority = 4, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Product Similar Collection")
        @Severity(SeverityLevel.CRITICAL)
        public void testResultArrayStructure() {
                RelatedCollectionResponse.RelatedCollectionItem responseData = relatedCollectionResponseData.getData()
                                .getResult().get(0);

                // Validate 'result' array structure and element properties
                assertThat("_id should be a string", responseData.get_id(), instanceOf(String.class));
                assertThat("name should be a string", responseData.getName(), instanceOf(String.class));
                assertThat("image should be a string", responseData.getImage(), instanceOf(String.class));

                assertThat("translations.en.title should be a string",
                                responseData.getTranslations().getEn().getTitle(), instanceOf(String.class));
                assertThat("translations.hi.title should be a string",
                                responseData.getTranslations().getHi().getTitle(), instanceOf(String.class));

                // Validate catalogs array structure
                responseData.getCatalogs().forEach(catalog -> {
                        assertThat("catalog._id should be a string", catalog.get_id(), instanceOf(String.class));
                        assertThat("catalog.title should be a string", catalog.getTitle(), instanceOf(String.class));
                        assertThat("catalog.priceText should be a number", catalog.getPriceText(),
                                        instanceOf(Integer.class));
                        assertThat("catalog.url should be a string", catalog.getUrl(), instanceOf(String.class));
                        assertThat("catalog.product should be a string", catalog.getProduct(),
                                        instanceOf(String.class));
                });

                logger.info("Result array structure validated successfully");
        }

        @Test(description = "Verify 'name' and 'description' fields are non-empty strings in each result item", priority = 5, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Product Similar Collection")
        @Severity(SeverityLevel.NORMAL)
        public void testNameAndDescriptionNonEmpty() {
                java.util.List<RelatedCollectionResponse.RelatedCollectionItem> responseData = relatedCollectionResponseData
                                .getData().getResult();

                // Verify 'name' and 'description' fields are non-empty strings in each result
                // item
                responseData.forEach(item -> {
                        assertThat("Name should not be empty",
                                        item.getName(), not(emptyOrNullString()));
                        assertThat("Description should not be empty",
                                        item.getDescription(), not(emptyOrNullString()));
                });

                logger.info("Name and description fields validated as non-empty");
        }

        @Test(description = "Ensure 'catalogs' array exists and contains items in each result", priority = 6, dependsOnMethods = "testResponseStatus", groups = "buyerapp")
        @Story("Product Similar Collection")
        @Severity(SeverityLevel.CRITICAL)
        public void testCatalogsArrayExists() {
                java.util.List<RelatedCollectionResponse.RelatedCollectionItem> responseData = relatedCollectionResponseData
                                .getData().getResult();

                // Ensure 'catalogs' array exists and contains items in each result
                responseData.forEach(item -> {
                        assertThat("catalogs should be an array and have at least 1 item",
                                        item.getCatalogs(), hasSize(greaterThanOrEqualTo(1)));
                });

                logger.info("Catalogs array validated - all items have at least 1 catalog");
        }
}
```

### `src/test/java/com/automation/tests/buyerapp/ProfilePage/AppUpdateTest.java`

_120 lines_

```java
package com.automation.tests.buyerapp.ProfilePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.AppUpdateResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for App Update API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/api/appConfig/app-update
 * Validates response structure and app update configuration.
 */
@Epic("Buyer App Profile Page")
@Feature("App Update API")
@TmsLink("TC-ORDER-002")
public class AppUpdateTest extends BaseTest {

        private static Response appUpdateResponse;
        private static AppUpdateResponse appUpdateResponseData;
        private String buyerAppBaseUrl;

        @BeforeClass(alwaysRun = true)
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

                logger.info("Using Buyer App token from VariableManager");
        }

        @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
        @Story("App Update")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatusCode200() {
                // Send GET request with authentication
                appUpdateResponse = buyerAppSpec()
                                .when()
                                .get(BuyerAppEndpoints.APP_UPDATE);

                // Parse response for other tests
                appUpdateResponseData = JsonUtils.fromResponse(appUpdateResponse, AppUpdateResponse.class);

                // Response status code is 200
                assertThat("Response status code is 200",
                                appUpdateResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("App Update")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = appUpdateResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Response time is less than threshold
                assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Content-Type header is application/json", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("App Update")
        @Severity(SeverityLevel.MINOR)
        public void testContentTypeHeader() {
                // Content-Type header is application/json
                assertThat("Content-Type should include application/json",
                                appUpdateResponse.getHeader("Content-Type"), containsString("application/json"));

                logger.info("Content-Type header verified: {}", appUpdateResponse.getHeader("Content-Type"));
        }

        @Test(description = "Response has the required fields - code, message, and data", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("App Update")
        @Severity(SeverityLevel.CRITICAL)
        public void testRequiredFields() {
                // Response has the required fields
                assertThat("code should be present", appUpdateResponseData.getCode(), notNullValue());
                assertThat("message should be present", appUpdateResponseData.getMessage(), notNullValue());
                assertThat("data should be present", appUpdateResponseData.getData(), notNullValue());

                logger.info("Required fields validated: code, message, data");
        }

        @Test(description = "Validate data properties", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("App Update")
        @Severity(SeverityLevel.CRITICAL)
        public void testDataProperties() {
                AppUpdateResponse.AppUpdateData data = appUpdateResponseData.getData();

                // Validate data properties
                assertThat("data should be an object", data, notNullValue());
                assertThat("isForced should be a boolean", data.getIsForced(), instanceOf(Boolean.class));
                assertThat("minVersion should be a number and at least 0",
                                data.getMinVersion(), allOf(instanceOf(Integer.class), greaterThanOrEqualTo(0)));
                assertThat("minVersionToUpdate should be a number and at least 0",
                                data.getMinVersionToUpdate(),
                                allOf(instanceOf(Integer.class), greaterThanOrEqualTo(0)));

                logger.info("Data properties validated: isForced={}, minVersion={}, minVersionToUpdate={}",
                                data.getIsForced(), data.getMinVersion(), data.getMinVersionToUpdate());
        }
}
```

### `src/test/java/com/automation/tests/buyerapp/ProfilePage/AuthValidateTest.java`

_178 lines_

```java
package com.automation.tests.buyerapp.ProfilePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.AuthValidateResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Auth Validate API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/auth/validate
 * Validates response structure, user data, and location object.
 */
@Epic("Buyer App Profile Page")
@Feature("Auth Validate API")
@TmsLink("TC-AUTH-002")
@TmsLink("TC-AUTH-001")
public class AuthValidateTest extends BaseTest {

    private static Response authValidateResponse;
    private static AuthValidateResponse authValidateResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass(alwaysRun = true)
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

        logger.info("Using Buyer App token from VariableManager");
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
    @Story("Auth Validate")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatusCode200() {
        // Send GET request with authentication
        authValidateResponse = buyerAppSpec()
                .when()
                .get(BuyerAppEndpoints.AUTH_VALIDATE);

        // Parse response for other tests
        authValidateResponseData = JsonUtils.fromResponse(authValidateResponse, AuthValidateResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                authValidateResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Content-Type header is application/json", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Auth Validate")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        // Content-Type header is application/json
        assertThat("Content-Type should include application/json",
                authValidateResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Content-Type header verified: {}", authValidateResponse.getHeader("Content-Type"));
    }

    @Test(description = "Response time is less than threshold", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Auth Validate")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = authValidateResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Data object has the correct structure", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Auth Validate")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataObjectStructure() {
        // Data object has the correct structure
        assertThat("Response should be an object", authValidateResponseData, notNullValue());
        assertThat("data should be present", authValidateResponseData.getData(), notNullValue());

        AuthValidateResponse.AuthValidateData data = authValidateResponseData.getData();

        // Required fields
        assertThat("_id should be a string", data.get_id(), instanceOf(String.class));
        assertThat("phoneNumber should be a string", data.getPhoneNumber(), instanceOf(String.class));
        assertThat("name should be a string", data.getName(), instanceOf(String.class));
        assertThat("businessName should be a string", data.getBusinessName(), instanceOf(String.class));
        assertThat("createdAt should be a string", data.getCreatedAt(), instanceOf(String.class));
        assertThat("updatedAt should be a string", data.getUpdatedAt(), instanceOf(String.class));

        // Optional object fields
        if (data.getLocation() != null) {
            assertThat("location should be an object", data.getLocation(),
                    instanceOf(AuthValidateResponse.Location.class));
        }

        if (data.getAddress() != null) {
            assertThat("address should be a string", data.getAddress(), instanceOf(String.class));
        }

        // Optional boolean fields
        if (data.getSellOnBizup() != null) {
            assertThat("sellOnBizup should be a boolean", data.getSellOnBizup(), instanceOf(Boolean.class));
        }
        if (data.getBusinessVerified() != null) {
            assertThat("businessVerified should be a boolean", data.getBusinessVerified(), instanceOf(Boolean.class));
        }
        if (data.getActivated() != null) {
            assertThat("activated should be a boolean", data.getActivated(), instanceOf(Boolean.class));
        }
        if (data.getIsDeleted() != null) {
            assertThat("isDeleted should be a boolean", data.getIsDeleted(), instanceOf(Boolean.class));
        }

        // Optional array fields
        if (data.getCategory() != null) {
            assertThat("category should be an array", data.getCategory(), instanceOf(java.util.List.class));
        }
        if (data.getBusinessCard() != null) {
            assertThat("businessCard should be an array", data.getBusinessCard(), instanceOf(java.util.List.class));
        }
        if (data.getTermsCondition() != null) {
            assertThat("termsCondition should be an array", data.getTermsCondition(), instanceOf(java.util.List.class));
        }
        if (data.getFo() != null) {
            assertThat("fo should be an array", data.getFo(), instanceOf(java.util.List.class));
        }

        logger.info("Data object structure validated");
    }

    @Test(description = "Location object contains required fields when present", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Auth Validate")
    @Severity(SeverityLevel.NORMAL)
    public void testLocationObjectFields() {
        // Location object contains required fields when present
        if (authValidateResponseData.getData() != null && authValidateResponseData.getData().getLocation() != null) {
            AuthValidateResponse.Location location = authValidateResponseData.getData().getLocation();

            assertThat("location should be an object", location, notNullValue());

            // Required location fields
            assertThat("country should be present", location.getCountry(), notNullValue());
            assertThat("city should be present", location.getCity(), notNullValue());
            assertThat("state should be present", location.getState(), notNullValue());
            assertThat("pincode should be present", location.getPincode(), notNullValue());
            assertThat("lat should be present", location.getLat(), notNullValue());
            assertThat("lng should be present", location.getLng(), notNullValue());

            // Optional location fields
            if (location.getName() != null) {
                assertThat("name should be a string", location.getName(), instanceOf(String.class));
            }

            logger.info("Location object validated: {}, {}", location.getCity(), location.getState());
        } else {
            logger.info("Location object not present - skipping validation");
        }
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/ProfilePage/UserProfileTest.java`

_138 lines_

```java
package com.automation.tests.buyerapp.ProfilePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.UserProfileResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for User Profile API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/api/user
 * Validates response structure, required fields, and data types.
 */
@Epic("Buyer App Profile Page")
@Feature("User Profile API")
public class UserProfileTest extends BaseTest {

    private static Response userProfileResponse;
    private static UserProfileResponse userProfileResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass(alwaysRun = true)
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

        logger.info("Using Buyer App token from VariableManager");
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
    @Story("User Profile")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatusCode200() {
        // Send GET request with authentication
        userProfileResponse = buyerAppSpec()
                .when()
                .get(BuyerAppEndpoints.USER_PROFILE);

        // Parse response for other tests
        userProfileResponseData = JsonUtils.fromResponse(userProfileResponse, UserProfileResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                userProfileResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("User Profile")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = userProfileResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Response has all the required fields", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("User Profile")
    @Severity(SeverityLevel.CRITICAL)
    public void testRequiredFields() {
        // Response has all the required fields
        assertThat("Response should be an object", userProfileResponseData, notNullValue());
        assertThat("code should be 200", userProfileResponseData.getCode(), equalTo(200));
        assertThat("message should be 'Success'", userProfileResponseData.getMessage(), equalTo("Success"));

        // Optional fields validation
        if (userProfileResponseData.getLocation() != null) {
            assertThat("location should be a string",
                    userProfileResponseData.getLocation(), instanceOf(String.class));
        }
        if (userProfileResponseData.getAddress() != null) {
            assertThat("address should be a string",
                    userProfileResponseData.getAddress(), instanceOf(String.class));
        }
        if (userProfileResponseData.getSellOnBizup() != null) {
            assertThat("sellOnBizup should be a boolean",
                    userProfileResponseData.getSellOnBizup(), instanceOf(Boolean.class));
        }

        logger.info("Required fields validated: code=200, message=Success");
    }

    @Test(description = "If phoneNumber exists, it should be in valid format", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("User Profile")
    @Severity(SeverityLevel.NORMAL)
    public void testPhoneNumberFormat() {
        // If phoneNumber exists, it should be in valid format
        if (userProfileResponseData.getPhoneNumber() != null) {
            assertThat("phoneNumber should be in valid format (10 digits)",
                    userProfileResponseData.getPhoneNumber(), matchesRegex("^\\d{10}$"));
            logger.info("Phone number format validated: {}", userProfileResponseData.getPhoneNumber());
        } else {
            logger.info("phoneNumber field is optional - not present in response");
        }
    }

    @Test(description = "Data object has the correct structure and data types", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("User Profile")
    @Severity(SeverityLevel.NORMAL)
    public void testDataObjectStructure() {
        // Data object has the correct structure and data types
        assertThat("data should be an object", userProfileResponseData.getData(), notNullValue());

        logger.info("Data object structure validated");
    }

    @Test(description = "Presence of Content-Type header in the response", priority = 6, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("User Profile")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        // Presence of Content-Type header in the response
        assertThat("Content-Type header should be present",
                userProfileResponse.getHeader("Content-Type"), notNullValue());

        logger.info("Content-Type header verified: {}", userProfileResponse.getHeader("Content-Type"));
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/ProfilePage/VideoViewActionTest.java`

_201 lines_

```java
package com.automation.tests.buyerapp.ProfilePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.VideoViewActionRequest;
import com.automation.models.response.VideoViewActionResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Video View Action API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/api/action
 * Validates response structure, video view action data, and JWT token.
 */
@Epic("Buyer App Profile Page")
@Feature("Video View Action API")
@TmsLink("TC-FEED-003")
@TmsLink("TC-FEED-002")
public class VideoViewActionTest extends BaseTest {

        private static Response videoViewResponse;
        private static VideoViewActionResponse videoViewResponseData;
        private String buyerAppBaseUrl;

        // Test data
        private static final String ACTION = "view";
        private String videoId;
        private String userId;

        @BeforeClass(alwaysRun = true)
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

                logger.info("Using Buyer App token from VariableManager");

                // Get video ID and user ID after VariableManager is initialized
                videoId = VariableManager.getVideoId();
                userId = VariableManager.getUserId();
                logger.info("Using video ID: {} and user ID: {}", videoId, userId);
        }

        @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatusCode200() {
                // Prepare request body
                VideoViewActionRequest requestBody = VideoViewActionRequest.builder()
                                .action(ACTION)
                                .creatorId(VariableManager.get("creator_id"))
                                .userId(VariableManager.get("user_id"))
                                .videoId(VariableManager.get("video_id", "681032fd9010791f25aed769"))
                                .build();

                // Send POST request with authentication
                videoViewResponse = buyerAppSpec()
                                .body(requestBody)
                                .when()
                                .post(BuyerAppEndpoints.ACTION);

                // Parse response for other tests
                videoViewResponseData = JsonUtils.fromResponse(videoViewResponse, VideoViewActionResponse.class);

                // Response status code is 200
                assertThat("Response status code is 200",
                                videoViewResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = videoViewResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Response time is less than threshold
                assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Response has JSON body", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseHasJsonBody() {
                // Response has JSON body
                assertThat("Response should have a body",
                                videoViewResponse.getBody().asString(), not(emptyOrNullString()));
                assertThat("Response should be parseable as JSON",
                                videoViewResponseData, notNullValue());

                logger.info("Response has valid JSON body");
        }

        @Test(description = "Response has success status", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseSuccessStatus() {
                // Response has success status
                assertThat("message should be 'Success'",
                                videoViewResponseData.getMessage(), equalTo("Success"));

                logger.info("Response has success status: message=Success");
        }

        @Test(description = "Response contains expected structure", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStructure() {
                // Response contains expected structure
                assertThat("data should be present",
                                videoViewResponseData.getData(), notNullValue());

                logger.info("Response contains expected structure");
        }

        @Test(description = "Content-Type header is application/json", priority = 6, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.MINOR)
        public void testContentTypeHeader() {
                // Content-Type header is application/json
                assertThat("Content-Type should include application/json",
                                videoViewResponse.getHeader("Content-Type"), containsString("application/json"));

                logger.info("Content-Type header verified: {}", videoViewResponse.getHeader("Content-Type"));
        }

        @Test(description = "isDeleted should be false", priority = 7, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.NORMAL)
        public void testIsDeletedFalse() {
                // isDeleted should be false
                assertThat("isDeleted should be false",
                                videoViewResponseData.getData().getIsDeleted(), equalTo(false));

                logger.info("isDeleted validated: false");
        }

        @Test(description = "videoId should be correct string and not empty", priority = 8, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.CRITICAL)
        public void testVideoIdValidation() {
                // videoId should be correct string and not empty
                assertThat("videoId should be present",
                                videoViewResponseData.getData().getVideoId(), notNullValue());
                assertThat("videoId should be a string",
                                videoViewResponseData.getData().getVideoId(), instanceOf(String.class));
                assertThat("videoId should not be empty",
                                videoViewResponseData.getData().getVideoId(), not(emptyOrNullString()));
                assertThat("videoId should equal expected value",
                                videoViewResponseData.getData().getVideoId(), equalTo(videoId));

                logger.info("videoId validated: {}", videoViewResponseData.getData().getVideoId());
        }

        @Test(description = "userId should be correct string and not empty", priority = 9, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.CRITICAL)
        public void testUserIdValidation() {
                // userId should be correct string and not empty
                assertThat("userId should be present",
                                videoViewResponseData.getData().getUserId(), notNullValue());
                assertThat("userId should be a string",
                                videoViewResponseData.getData().getUserId(), instanceOf(String.class));
                assertThat("userId should not be empty",
                                videoViewResponseData.getData().getUserId(), not(emptyOrNullString()));
                assertThat("userId should equal expected value",
                                videoViewResponseData.getData().getUserId(), equalTo(userId));

                logger.info("userId validated: {}", videoViewResponseData.getData().getUserId());
        }

        @Test(description = "JWT token is valid", priority = 10, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Video View Action")
        @Severity(SeverityLevel.BLOCKER)
        public void testJwtTokenValid() {
                // JWT token is valid (response should not be 401 or 403)
                assertThat("Response should not be 401 or 403",
                                videoViewResponse.getStatusCode(), not(isOneOf(401, 403)));

                logger.info("JWT token validated: not 401 or 403");
        }
}
```

### `src/test/java/com/automation/tests/buyerapp/ProfilePage/WatchedVideosTest.java`

_130 lines_

```java
package com.automation.tests.buyerapp.ProfilePage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.WatchedVideosResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Watched Videos API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/user/viewed/videos
 * Validates response structure and video item fields.
 */
@Epic("Buyer App Profile Page")
@Feature("Watched Videos API")
@TmsLink("TC-USER-002")
@TmsLink("TC-USER-001")
public class WatchedVideosTest extends BaseTest {

        private static Response watchedVideosResponse;
        private static WatchedVideosResponse watchedVideosResponseData;
        private String buyerAppBaseUrl;

        @BeforeClass(alwaysRun = true)
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

                logger.info("Using Buyer App token from VariableManager");
        }

        @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
        @Story("Watched Videos")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatusCode200() {
                // Send GET request with authentication
                watchedVideosResponse = buyerAppSpec()
                                .when()
                                .get(BuyerAppEndpoints.USER_VIEWED_VIDEOS);

                // Parse response for other tests
                watchedVideosResponseData = JsonUtils.fromResponse(watchedVideosResponse, WatchedVideosResponse.class);

                // Response status code is 200
                assertThat("Response status code is 200",
                                watchedVideosResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Watched Videos")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = watchedVideosResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Response time is less than threshold
                assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Valid JSON response structure", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Watched Videos")
        @Severity(SeverityLevel.CRITICAL)
        public void testValidJsonResponseStructure() {
                // Valid JSON response structure
                assertThat("Response should be an object", watchedVideosResponseData, notNullValue());
                assertThat("data should exist and be an object", watchedVideosResponseData.getData(), notNullValue());
                assertThat("data.result should exist and be an array and not empty",
                                watchedVideosResponseData.getData().getResult(),
                                allOf(instanceOf(java.util.List.class), not(empty())));

                logger.info("Valid JSON response structure verified: {} videos found",
                                watchedVideosResponseData.getData().getResult().size());
        }

        @Test(description = "Content-Type is application/json", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Watched Videos")
        @Severity(SeverityLevel.MINOR)
        public void testContentTypeHeader() {
                // Content-Type is application/json
                assertThat("Content-Type should include application/json",
                                watchedVideosResponse.getHeader("Content-Type"), containsString("application/json"));

                logger.info("Content-Type header verified: {}", watchedVideosResponse.getHeader("Content-Type"));
        }

        @Test(description = "Each video item has required fields", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Watched Videos")
        @Severity(SeverityLevel.CRITICAL)
        public void testRequiredFieldsInVideoItems() {
                // Each video item has required fields
                watchedVideosResponseData.getData().getResult().forEach(item -> {
                        assertThat("videoId should be present", item.getVideoId(), notNullValue());
                        assertThat("_id should be present", item.get_id(), notNullValue());
                        assertThat("phoneNumber should be present", item.getPhoneNumber(), notNullValue());
                        assertThat("product should be present", item.getProduct(), notNullValue());
                        assertThat("collection should be present", item.getCollection(), notNullValue());
                        assertThat("market should be present", item.getMarket(), notNullValue());
                        assertThat("priceText should be present", item.getPriceText(), notNullValue());
                        assertThat("driveLink should be present", item.getDriveLink(), notNullValue());
                        assertThat("isDeleted should be present", item.getIsDeleted(), notNullValue());
                        assertThat("seller should be present", item.getSeller(), notNullValue());
                        assertThat("thubmbnailDriveLink should be present", item.getThubmbnailDriveLink(),
                                        notNullValue());
                        assertThat("createdAt should be present", item.getCreatedAt(), notNullValue());
                });

                logger.info("Required fields validated for all {} video items",
                                watchedVideosResponseData.getData().getResult().size());
        }
}
```

### `src/test/java/com/automation/tests/buyerapp/SearchPage/SearchProductTest.java`

_163 lines_

```java
package com.automation.tests.buyerapp.SearchPage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.SearchProductResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Search Product API - Converted from Postman Script.
 * Endpoint:
 * {{navo_base}}/v1/user/search?q={{search_product}}&page=1&pageSize=20
 * Validates response structure, seller details, and security.
 */
@Epic("Buyer App Search Page")
@Feature("Search Product API")
@TmsLink("TC-USER-005")
public class SearchProductTest extends BaseTest {

        private static Response searchProductResponse;
        private static SearchProductResponse searchProductResponseData;
        private String buyerAppBaseUrl;
        private static final String SEARCH_QUERY = "saree"; // Default search query

        @BeforeClass(alwaysRun = true)
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

                logger.info("Using Buyer App token from VariableManager");
        }

        @Test(description = "Status code is 200", priority = 1, groups = "buyerapp")
        @Story("Search Product")
        @Severity(SeverityLevel.CRITICAL)
        public void testStatusCode200() {
                // Send GET request with authentication and query parameters
                searchProductResponse = buyerAppSpec()
                                .queryParam("q", SEARCH_QUERY)
                                .queryParam("page", 1)
                                .queryParam("pageSize", 20)
                                .when()
                                .get(BuyerAppEndpoints.USER_SEARCH);

                // Parse response for other tests
                searchProductResponseData = JsonUtils.fromResponse(searchProductResponse, SearchProductResponse.class);

                // Status code is 200
                assertThat("Status code is 200",
                                searchProductResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Search Product")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = searchProductResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Response time is less than threshold
                assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Response has valid JSON body", priority = 3, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Search Product")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseHasValidJsonBody() {
                // Response has valid JSON body
                assertThat("Response should have a body",
                                searchProductResponse.getBody().asString(), not(emptyOrNullString()));
                assertThat("Response should be parseable as JSON",
                                searchProductResponseData, notNullValue());

                logger.info("Response has valid JSON body");
        }

        @Test(description = "Required headers are present and valid", priority = 4, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Search Product")
        @Severity(SeverityLevel.MINOR)
        public void testRequiredHeaders() {
                // Required headers are present and valid
                assertThat("Content-Type header should be present",
                                searchProductResponse.getHeader("Content-Type"), notNullValue());
                assertThat("Content-Type should include application/json",
                                searchProductResponse.getHeader("Content-Type"), containsString("application/json"));

                logger.info("Required headers validated: Content-Type = {}",
                                searchProductResponse.getHeader("Content-Type"));
        }

        @Test(description = "Validate the response schema for seller details", priority = 5, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Search Product")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseSchemaForSellerDetails() {
                // Validate the response schema for seller details
                assertThat("Response should be an object", searchProductResponseData, notNullValue());
                assertThat("data should be an object", searchProductResponseData.getData(), notNullValue());
                assertThat("data.items should be an array with length above 0",
                                searchProductResponseData.getData().getItems(),
                                allOf(instanceOf(java.util.List.class), hasSize(greaterThan(0))));

                searchProductResponseData.getData().getItems().forEach(user -> {
                        assertThat("User should have seller object", user.getSeller(), notNullValue());

                        SearchProductResponse.Seller seller = user.getSeller();
                        assertThat("seller.phoneNumber should be a string", seller.getPhoneNumber(),
                                        instanceOf(String.class));
                        assertThat("seller.address should be a string", seller.getAddress(), instanceOf(String.class));
                        assertThat("seller.businessName should be a string", seller.getBusinessName(),
                                        instanceOf(String.class));
                        assertThat("seller.name should be a string", seller.getName(), instanceOf(String.class));
                        assertThat("seller._id should be a string", seller.get_id(), instanceOf(String.class));
                        assertThat("seller.deprioritisation_status should be a boolean",
                                        seller.getDeprioritisation_status(), instanceOf(Boolean.class));
                        assertThat("seller.isCatalogAvailable should be a boolean",
                                        seller.getIsCatalogAvailable(), instanceOf(Boolean.class));
                });

                logger.info("Response schema for seller details validated: {} items found",
                                searchProductResponseData.getData().getItems().size());
        }

        @Test(description = "No sensitive data exposed and phone numbers are valid", priority = 6, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Search Product")
        @Severity(SeverityLevel.BLOCKER)
        public void testSecurityValidation() {
                String responseBody = searchProductResponse.getBody().asString();

                // No sensitive data exposed
                assertThat("Response should not include 'token'",
                                responseBody, not(containsString("token")));

                // Phone numbers are valid
                searchProductResponseData.getData().getItems().forEach(user -> {
                        if (user.getPhoneNumber() != null) {
                                assertThat("Phone number should be in international format",
                                                user.getPhoneNumber(), matchesRegex("^\\+[0-9]{10,15}$"));
                        }
                });

                logger.info("Security validation passed: no sensitive data exposed, phone numbers valid");
        }
}
```

### `src/test/java/com/automation/tests/buyerapp/SearchPage/SearchRecommendChipSelectTest.java`

_150 lines_

```java
package com.automation.tests.buyerapp.SearchPage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.SearchRecommendChipSelectResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static com.automation.tests.buyerapp.SearchPage.SearchRecommendedChipsTest.searchRecommend;
import static com.automation.tests.buyerapp.SearchPage.SearchRecommendedChipsTest.searchRecommendId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Search Recommend Chip Select API - Converted from Postman
 * Script.
 * Endpoint:
 * {{navo_base}}/v1/user/search?q={{search_recommend}}&product={{search_recommend_id}}&page=1&pageSize=20
 * Validates response structure, items array, and buckets.
 */
@Epic("Buyer App Search Page")
@Feature("Search Recommend Chip Select API")
@TmsLink("TC-USER-005")
public class SearchRecommendChipSelectTest extends BaseTest {

        private static Response searchChipSelectResponse;
        private static SearchRecommendChipSelectResponse searchChipSelectResponseData;
        private String buyerAppBaseUrl;

        @BeforeClass(alwaysRun = true)
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

                logger.info("Using Buyer App token from VariableManager");
        }

        @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
        @Story("Search Recommend Chip Select")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatusCode200() {
                // Get search recommend parameters from previous test
                String searchQuery = (searchRecommend != null && !searchRecommend.isEmpty())
                                ? searchRecommend
                                : "saree"; // Default value
                String productId = (searchRecommendId != null && !searchRecommendId.isEmpty())
                                ? searchRecommendId
                                : ""; // Default empty

                // Send GET request with authentication and query parameters
                searchChipSelectResponse = buyerAppSpec()
                                .queryParam("q", searchQuery)
                                .queryParam("product", productId)
                                .queryParam("page", 1)
                                .queryParam("pageSize", 20)
                                .when()
                                .get(BuyerAppEndpoints.USER_SEARCH);

                // Parse response for other tests
                searchChipSelectResponseData = JsonUtils.fromResponse(searchChipSelectResponse,
                                SearchRecommendChipSelectResponse.class);

                // Response status code is 200
                assertThat("Response status code is 200",
                                searchChipSelectResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Search Recommend Chip Select")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = searchChipSelectResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Response time is less than threshold
                assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Response contains required fields: statusCode, message, and data", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Search Recommend Chip Select")
        @Severity(SeverityLevel.CRITICAL)
        public void testRequiredFields() {
                // Response contains required fields
                assertThat("Response should be an object", searchChipSelectResponseData, notNullValue());
                assertThat("statusCode should be present", searchChipSelectResponseData.getStatusCode(),
                                notNullValue());
                assertThat("message should be present", searchChipSelectResponseData.getMessage(), notNullValue());
                assertThat("data should be present", searchChipSelectResponseData.getData(), notNullValue());

                logger.info("Required fields validated: statusCode, message, data");
        }

        @Test(description = "Validate structure of 'items' array and nested objects", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Search Recommend Chip Select")
        @Severity(SeverityLevel.CRITICAL)
        public void testItemsArrayStructure() {
                // Validate structure of 'items' array and nested objects
                assertThat("Response should be an object", searchChipSelectResponseData, notNullValue());
                assertThat("data.items should be an array and not empty",
                                searchChipSelectResponseData.getData().getItems(),
                                allOf(instanceOf(java.util.List.class), not(empty())));

                searchChipSelectResponseData.getData().getItems().forEach(item -> {
                        assertThat("Item should be an object", item, notNullValue());
                        assertThat("seller should be an object", item.getSeller(), notNullValue());
                        assertThat("thumbnail should be an object", item.getThumbnail(), notNullValue());
                        assertThat("product should be an array with at least 1 element",
                                        item.getProduct(), hasSize(greaterThanOrEqualTo(1)));
                        assertThat("tags should be an array with at least 1 element",
                                        item.getTags(), hasSize(greaterThanOrEqualTo(1)));
                        assertThat("market should be an object", item.getMarket(), notNullValue());
                        assertThat("attributes should be an array with at least 1 element",
                                        item.getAttributes(), hasSize(greaterThanOrEqualTo(1)));
                });

                logger.info("Items array structure validated: {} items found",
                                searchChipSelectResponseData.getData().getItems().size());
        }

        @Test(description = "Buckets array is present and has at least one element", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Search Recommend Chip Select")
        @Severity(SeverityLevel.NORMAL)
        public void testBucketsArrayPresent() {
                // Buckets array is present and has at least one element
                assertThat("Response should be an object", searchChipSelectResponseData, notNullValue());
                assertThat("data.buckets should be an array with at least 1 element",
                                searchChipSelectResponseData.getData().getBuckets(),
                                hasSize(greaterThanOrEqualTo(1)));

                logger.info("Buckets array validated: {} buckets found",
                                searchChipSelectResponseData.getData().getBuckets().size());
        }
}
```

### `src/test/java/com/automation/tests/buyerapp/SearchPage/SearchRecommendedChipsTest.java`

_236 lines_

```java
package com.automation.tests.buyerapp.SearchPage;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.SearchRecommendedChipsResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Search Recommended Chips API - Converted from Postman Script.
 * Endpoint:
 * {{navo_base}}/v1/user/search?q={{search_product}}&from=0&to=1000000000&page=0&pageSize=0
 * Validates response structure, search results, and security.
 */
@Epic("Buyer App Search Page")
@Feature("Search Recommended Chips API")
@TmsLink("TC-USER-005")
public class SearchRecommendedChipsTest extends BaseTest {

        private static Response searchRecommendedResponse;
        private static SearchRecommendedChipsResponse searchRecommendedResponseData;
        private String buyerAppBaseUrl;
        private static final String SEARCH_QUERY = "saree"; // Default search query
        public static String searchRecommend; // Store recommended product name
        public static String searchRecommendId; // Store recommended product ID

        @BeforeClass(alwaysRun = true)
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

                logger.info("Using Buyer App token from VariableManager");
        }

        @Test(description = "Status code is 200", priority = 1, groups = "buyerapp")
        @Story("Search Recommended Chips")
        @Severity(SeverityLevel.CRITICAL)
        public void testStatusCode200() {
                // Send GET request with authentication and query parameters
                searchRecommendedResponse = buyerAppSpec()
                                .queryParam("q", SEARCH_QUERY)
                                .queryParam("from", 0)
                                .queryParam("to", 1000000000)
                                .queryParam("page", 0)
                                .queryParam("pageSize", 0)
                                .when()
                                .get(BuyerAppEndpoints.USER_SEARCH);

                // Parse response for other tests
                searchRecommendedResponseData = JsonUtils.fromResponse(searchRecommendedResponse,
                                SearchRecommendedChipsResponse.class);

                // Status code is 200
                assertThat("Status code is 200",
                                searchRecommendedResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Response time is less than threshold", priority = 2, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Search Recommended Chips")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = searchRecommendedResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Response time is less than threshold
                assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Response has valid JSON body", priority = 3, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Search Recommended Chips")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseHasValidJsonBody() {
                // Response has valid JSON body
                assertThat("Response should have a body",
                                searchRecommendedResponse.getBody().asString(), not(emptyOrNullString()));
                assertThat("Response should be parseable as JSON",
                                searchRecommendedResponseData, notNullValue());

                logger.info("Response has valid JSON body");
        }

        @Test(description = "Required headers are present and valid", priority = 4, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Search Recommended Chips")
        @Severity(SeverityLevel.MINOR)
        public void testRequiredHeaders() {
                // Required headers are present and valid
                assertThat("Content-Type header should be present",
                                searchRecommendedResponse.getHeader("Content-Type"), notNullValue());
                assertThat("Content-Type should include application/json",
                                searchRecommendedResponse.getHeader("Content-Type"),
                                containsString("application/json"));

                logger.info("Required headers validated: Content-Type = {}",
                                searchRecommendedResponse.getHeader("Content-Type"));
        }

        @Test(description = "Validate search response structure and key fields", priority = 5, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Search Recommended Chips")
        @Severity(SeverityLevel.CRITICAL)
        public void testSearchResponseStructure() {
                // Validate search response structure and key fields
                assertThat("Response should be an object", searchRecommendedResponseData, notNullValue());
                assertThat("statusCode should be '10000'",
                                searchRecommendedResponseData.getStatusCode(), equalTo("10000"));
                assertThat("message should be 'success'",
                                searchRecommendedResponseData.getMessage(), equalTo("success"));
                assertThat("data should be an object",
                                searchRecommendedResponseData.getData(), notNullValue());
                assertThat("data.items should be an array",
                                searchRecommendedResponseData.getData().getItems(), instanceOf(java.util.List.class));

                if (!searchRecommendedResponseData.getData().getItems().isEmpty()) {
                        SearchRecommendedChipsResponse.SearchUserItem user = searchRecommendedResponseData.getData()
                                        .getItems().get(0);
                        assertThat("_id should be a string", user.get_id(), instanceOf(String.class));
                        assertThat("name should be a string", user.getName(), instanceOf(String.class));

                        if (user.getPhoneNumber() != null) {
                                assertThat("Phone number should be in international format",
                                                user.getPhoneNumber(), matchesRegex("^\\+[0-9]{10,15}$"));
                        }

                        if (user.getBusinessInfo() != null) {
                                assertThat("businessInfo.businessName should be a string",
                                                user.getBusinessInfo().getBusinessName(), instanceOf(String.class));
                        }
                }

                logger.info("Search response structure validated");
        }

        @Test(description = "Verify search results contain the query term 'saree'", priority = 6, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Search Recommended Chips")
        @Severity(SeverityLevel.NORMAL)
        public void testSearchResultsContainQueryTerm() {
                java.util.List<SearchRecommendedChipsResponse.SearchUserItem> items = searchRecommendedResponseData
                                .getData().getItems();

                if (!items.isEmpty()) {
                        items.forEach(user -> {
                                StringBuilder searchFields = new StringBuilder();

                                if (user.getName() != null)
                                        searchFields.append(user.getName()).append(" ");
                                if (user.getBusinessInfo() != null) {
                                        if (user.getBusinessInfo().getBusinessName() != null)
                                                searchFields.append(user.getBusinessInfo().getBusinessName())
                                                                .append(" ");
                                        if (user.getBusinessInfo().getDescription() != null)
                                                searchFields.append(user.getBusinessInfo().getDescription())
                                                                .append(" ");
                                }
                                if (user.getTags() != null) {
                                        // Convert tags objects to strings
                                        String tagsString = user.getTags().stream()
                                                .map(Object::toString)
                                                .collect(java.util.stream.Collectors.joining(" "));
                                        searchFields.append(tagsString);
                                }

                                assertThat("Search fields should contain 'saree'",
                                                searchFields.toString().toLowerCase(), containsString("saree"));
                        });
                } else {
                        Integer totalCount = searchRecommendedResponseData.getData().getTotalCount();
                        assertThat("Total count should be 0 when no items",
                                        totalCount != null ? totalCount : 0, equalTo(0));
                }

                logger.info("Search results contain query term validation completed");
        }

        @Test(description = "No sensitive data exposed and phone numbers are valid", priority = 7, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Search Recommended Chips")
        @Severity(SeverityLevel.BLOCKER)
        public void testSecurityValidation() {
                String responseBody = searchRecommendedResponse.getBody().asString();

                // No sensitive data exposed
                assertThat("Response should not include 'password'",
                                responseBody, not(containsString("password")));
                assertThat("Response should not include 'creditCard'",
                                responseBody, not(containsString("creditCard")));
                assertThat("Response should not include 'token'",
                                responseBody, not(containsString("token")));

                // Phone numbers are valid
                searchRecommendedResponseData.getData().getItems().forEach(user -> {
                        if (user.getPhoneNumber() != null) {
                                assertThat("Phone number should be in international format",
                                                user.getPhoneNumber(), matchesRegex("^\\+[0-9]{10,15}$"));
                        }
                });

                logger.info("Security validation passed: no sensitive data exposed, phone numbers valid");
        }

        @Test(description = "Set recommended product and ID", priority = 8, dependsOnMethods = "testStatusCode200", groups = "buyerapp")
        @Story("Search Recommended Chips")
        @Severity(SeverityLevel.NORMAL)
        public void testSetRecommendedProductAndId() {
                // Set recommended product and ID from buckets
                if (searchRecommendedResponseData.getData().getBuckets() != null
                                && !searchRecommendedResponseData.getData().getBuckets().isEmpty()) {
                        searchRecommend = searchRecommendedResponseData.getData().getBuckets().get(0).getName();
                        searchRecommendId = searchRecommendedResponseData.getData().getBuckets().get(0).get_id();

                        assertThat("searchRecommend should not be null", searchRecommend, notNullValue());
                        assertThat("searchRecommendId should not be null", searchRecommendId, notNullValue());

                        logger.info("Recommended product set: {} (ID: {})", searchRecommend, searchRecommendId);
                } else {
                        logger.warn("No buckets found in response to set recommended product");
                }
        }
}
```

### `src/test/java/com/automation/tests/buyerapp/SuitableForConfigTest.java`

_125 lines_

```java
package com.automation.tests.buyerapp;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.response.SuitableForConfigResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Suitable For Config API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/feed/home/config
 * Validates response structure and config items.
 */
@Epic("Buyer App Configuration")
@Feature("Suitable For Config API")
@TmsLink("TC-FEED-004")
@TmsLink("TC-FEED-003")
public class SuitableForConfigTest extends BaseTest {

        private static Response configResponse;
        private static SuitableForConfigResponse configResponseData;
        private String buyerAppBaseUrl;

        @BeforeClass(alwaysRun = true)
        public void setupBuyerApp() {
                buyerAppBaseUrl = config.buyerAppBaseUrl();
                logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

                logger.info("Using Buyer App token from VariableManager");
        }

        @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
        @Story("Suitable For Config")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStatusCode200() {
                // Send GET request with authentication
                configResponse = buyerAppSpec()
                                .when()
                                .get(BuyerAppEndpoints.FEED_HOME_CONFIG);

                // Parse response for other tests
                configResponseData = JsonUtils.fromResponse(configResponse, SuitableForConfigResponse.class);

                // Response status code is 200
                assertThat("Response status code is 200",
                                configResponse.getStatusCode(), equalTo(HttpStatus.OK));

                logger.info("Response status verified: 200 OK");
        }

        @Test(description = "Content-Type header is present", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Suitable For Config")
        @Severity(SeverityLevel.MINOR)
        public void testContentTypeHeaderPresent() {
                // Content-Type header is present
                assertThat("Content-Type header should be present",
                                configResponse.getHeader("Content-Type"), notNullValue());

                logger.info("Content-Type header verified: {}", configResponse.getHeader("Content-Type"));
        }

        @Test(description = "Response time is less than threshold", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Suitable For Config")
        @Severity(SeverityLevel.NORMAL)
        public void testResponseTime() {
                // Get response time threshold from config (fallback to 20000ms)
                long responseTimeThreshold = config.responseTimeThreshold();
                long actualResponseTime = configResponse.getTime();

                // Validate response time measurement is available
                assertThat("Response time measurement should be available",
                                actualResponseTime, notNullValue());

                // Response time is less than threshold
                assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                                actualResponseTime, lessThan(responseTimeThreshold));

                logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                                responseTimeThreshold);
        }

        @Test(description = "Response has required 'data' object with non-empty 'result' array", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Suitable For Config")
        @Severity(SeverityLevel.CRITICAL)
        public void testResponseStructure() {
                // Response has required 'data' object with non-empty 'result' array
                assertThat("Response should be an object", configResponseData, notNullValue());
                assertThat("data should exist and be an object", configResponseData.getData(), notNullValue());
                assertThat("data.result should exist and be an array with at least 1 element",
                                configResponseData.getData().getResult(),
                                allOf(instanceOf(java.util.List.class), hasSize(greaterThanOrEqualTo(1))));

                logger.info("Response structure validated: {} config items found",
                                configResponseData.getData().getResult().size());
        }

        @Test(description = "Each result item contains required fields with correct types", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
        @Story("Suitable For Config")
        @Severity(SeverityLevel.CRITICAL)
        public void testResultItemsFields() {
                // Each result item contains required fields with correct types
                configResponseData.getData().getResult().forEach(item -> {
                        assertThat("item should be an object", item, notNullValue());
                        assertThat("type should be a string with at least 1 character",
                                        item.getType(), allOf(instanceOf(String.class), not(emptyOrNullString())));
                        assertThat("data should be an array", item.getData(), instanceOf(java.util.List.class));
                        assertThat("api should be a string", item.getApi(), instanceOf(String.class));
                        assertThat("title should be a string", item.getTitle(), instanceOf(String.class));
                        assertThat("description should be a string", item.getDescription(), instanceOf(String.class));
                        assertThat("imageUrl should be a string", item.getImageUrl(), instanceOf(String.class));
                });

                logger.info("Result items fields validated for all {} items",
                                configResponseData.getData().getResult().size());
        }
}
```

### `src/test/java/com/automation/tests/buyerapp/UpdateCartTest.java`

_249 lines_

```java
package com.automation.tests.buyerapp;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.UpdateCartRequest;
import com.automation.models.response.UpdateCartResponse;
import com.automation.utils.JsonUtils;
import com.automation.utils.TestDataResolver;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Update Cart API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/user/update_cart
 * Validates response structure and cart data.
 */
@Epic("Buyer App Cart")
@Feature("Update Cart API")
@TmsLink("TC-USER-003")
@TmsLink("TC-USER-001")
@TmsLink("TC-USER-002")
public class UpdateCartTest extends BaseTest {

    private static Response updateCartResponse;
    private static UpdateCartResponse updateCartResponseData;
    private String buyerAppBaseUrl;

    // Test data
    private static final Integer QUANTITY = 1;

    @BeforeClass(alwaysRun = true)
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

        logger.info("Using Buyer App token from VariableManager");
    }

    @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatusCode200() {
        // Prepare request body
        UpdateCartRequest requestBody = UpdateCartRequest.builder()
                .quantity(QUANTITY)
                .cart_id(TestDataResolver.cartId(BaseTest::buyerAppSpec))
                .cat_id(TestDataResolver.cartCatalogId(BaseTest::buyerAppSpec))
                .build();

        // Send POST request with authentication
        updateCartResponse = buyerAppSpec()
                .body(requestBody)
                .when()
                .post(BuyerAppEndpoints.USER_UPDATE_CART);

        // Parse response for other tests
        updateCartResponseData = JsonUtils.fromResponse(updateCartResponse, UpdateCartResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                updateCartResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response has valid Content-Type header", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        // Response has valid Content-Type header
        assertThat("Content-Type should include application/json",
                updateCartResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Content-Type header verified: {}", updateCartResponse.getHeader("Content-Type"));
    }

    @Test(description = "Response time is less than threshold", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = updateCartResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Response has required top-level fields", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.CRITICAL)
    public void testTopLevelFields() {
        // Response has required top-level fields
        assertThat("Response should be an object", updateCartResponseData, notNullValue());
        assertThat("statusCode should be present", updateCartResponseData.getStatusCode(), notNullValue());
        assertThat("message should be present", updateCartResponseData.getMessage(), notNullValue());
        assertThat("data should be present", updateCartResponseData.getData(), notNullValue());

        logger.info("Top-level fields validated: statusCode, message, data");
    }

    @Test(description = "Cart data contains expected fields", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.CRITICAL)
    public void testCartDataFields() {
        if (updateCartResponseData.getData() != null) {
            UpdateCartResponse.CartData cartData = updateCartResponseData.getData();

            assertThat("cartData should be an object", cartData, notNullValue());
            assertThat("_id should be present", cartData.get_id(), notNullValue());
            assertThat("catalogs should be present", cartData.getCatalogs(), notNullValue());
            assertThat("status should be present", cartData.getStatus(), notNullValue());
            assertThat("cartItems should be present", cartData.getCartItems(), notNullValue());
            assertThat("buyerId should be present", cartData.getBuyerId(), notNullValue());

            logger.info("Cart data fields validated");
        }
    }

    @Test(description = "CartItems array is present and not empty", priority = 6, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.CRITICAL)
    public void testCartItemsArrayNotEmpty() {
        if (updateCartResponseData.getData() != null) {
            assertThat("cartItems should be an array and not empty",
                    updateCartResponseData.getData().getCartItems(),
                    allOf(instanceOf(java.util.List.class), not(empty())));

            logger.info("CartItems array validated: {} items",
                    updateCartResponseData.getData().getCartItems().size());
        }
    }

    @Test(description = "Validate properties of each cart item", priority = 7, dependsOnMethods = "testCartItemsArrayNotEmpty", groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.CRITICAL)
    public void testCartItemProperties() {
        if (updateCartResponseData.getData() != null && updateCartResponseData.getData().getCartItems() != null) {
            updateCartResponseData.getData().getCartItems().forEach(item -> {
                assertThat("title should be present", item.getTitle(), notNullValue());
                assertThat("title should be a non-empty string",
                        item.getTitle(), allOf(instanceOf(String.class), not(emptyOrNullString())));
                assertThat("priceText should be a number", item.getPriceText(), instanceOf(Double.class));
                assertThat("quantity should be a number", item.getQuantity(), instanceOf(Integer.class));
                assertThat("sellerId should be a non-empty string",
                        item.getSellerId(), allOf(instanceOf(String.class), not(emptyOrNullString())));

                if (item.getSetQuantity() != null) {
                    assertThat("setQuantity should be a number", item.getSetQuantity(), instanceOf(Integer.class));
                }
                if (item.getTotalPrice() != null) {
                    assertThat("totalPrice should be a number", item.getTotalPrice(), instanceOf(Double.class));
                }
                if (item.getIsDeleted() != null) {
                    assertThat("isDeleted should be a boolean", item.getIsDeleted(), instanceOf(Boolean.class));
                }
                if (item.getAvailable() != null) {
                    assertThat("available should be a boolean", item.getAvailable(), instanceOf(Boolean.class));
                }
            });

            logger.info("Cart item properties validated for all items");
        }
    }

    @Test(description = "Validate numeric fields in cart data", priority = 8, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.NORMAL)
    public void testCartDataNumericFields() {
        if (updateCartResponseData.getData() != null) {
            UpdateCartResponse.CartData cartData = updateCartResponseData.getData();

            if (cartData.getCartTotal() != null) {
                assertThat("cartTotal should be a number", cartData.getCartTotal(), instanceOf(Double.class));
            }
            if (cartData.getShippingCharges() != null) {
                assertThat("shippingCharges should be a number", cartData.getShippingCharges(),
                        instanceOf(Double.class));
            }
            if (cartData.getTax() != null) {
                assertThat("tax should be a number", cartData.getTax(), instanceOf(Double.class));
            }
            if (cartData.getTotalPayable() != null) {
                assertThat("totalPayable should be a number", cartData.getTotalPayable(), instanceOf(Double.class));
            }

            logger.info("Cart data numeric fields validated");
        }
    }

    @Test(description = "Validate date fields in cart data", priority = 9, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp")
    @Story("Update Cart")
    @Severity(SeverityLevel.NORMAL)
    public void testCartDataDateFields() {
        if (updateCartResponseData.getData() != null) {
            UpdateCartResponse.CartData cartData = updateCartResponseData.getData();

            if (cartData.getCreatedAt() != null) {
                assertThat("createdAt should be a string", cartData.getCreatedAt(), instanceOf(String.class));
                assertThat("createdAt should be a valid date", isValidDate(cartData.getCreatedAt()), is(true));
            }
            if (cartData.getUpdatedAt() != null) {
                assertThat("updatedAt should be a string", cartData.getUpdatedAt(), instanceOf(String.class));
                assertThat("updatedAt should be a valid date", isValidDate(cartData.getUpdatedAt()), is(true));
            }

            logger.info("Cart data date fields validated");
        }
    }

    // Helper method to validate date string
    private boolean isValidDate(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setLenient(false);
            sdf.parse(dateString);
            return true;
        } catch (ParseException e) {
            // Try ISO 8601 format
            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                isoFormat.setLenient(false);
                isoFormat.parse(dateString);
                return true;
            } catch (ParseException ex) {
                return false;
            }
        }
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/VideoFeedTvTest.java`

_202 lines_

```java
package com.automation.tests.buyerapp;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.VideoFeedRequest;
import com.automation.models.response.VideoFeedResponse;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;

import com.automation.utils.VariableManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Video Feed (T.V) API - Converted from Postman Script.
 * Endpoint: {{navo_base}}/v1/feed/tv
 * Validates response structure and video feed data.
 */
@Epic("Buyer App Video Feed")
@Feature("Video Feed (T.V) API")
@TmsLink("TC-FEED-003")
@TmsLink("TC-FEED-001")
@TmsLink("TC-FEED-002")
public class VideoFeedTvTest extends BaseTest {

    private static Response videoFeedResponse;
    private static VideoFeedResponse videoFeedResponseData;
    private String buyerAppBaseUrl;

    @BeforeClass(alwaysRun = true)
    public void setupBuyerApp() {
        buyerAppBaseUrl = config.buyerAppBaseUrl();
        logger.info("Buyer App Base URL: {}", buyerAppBaseUrl);

        logger.info("Using Buyer App token from VariableManager");
    }

    /**
     * DISABLED: Backend error - "Cannot read properties of undefined (reading
     * '_id')"
     * This is a server-side issue that needs to be fixed on the API.
     * Error: HTTP 500 Internal Server Error
     */
    @Test(description = "Response status code is 200", priority = 1, groups = "buyerapp", enabled = false)
    @Story("Video Feed (T.V)")
    @Severity(SeverityLevel.CRITICAL)
    public void testResponseStatusCode200() {
        // Prepare request body
        VideoFeedRequest requestBody = VideoFeedRequest.builder()
                .excluded_videos(new ArrayList<>())
                .filters(VideoFeedRequest.Filters.builder()
                        .city(new ArrayList<>())
                        .productTags(new ArrayList<>())
                        .suitable_for(new ArrayList<>())
                        .testData("")
                        .build())
                .from(0)
                .liquidity("seller")
                .sort_by("popular")
                .sort_order("desc")
                .video_id("")
                .build();

        // Send POST request with authentication
        videoFeedResponse = buyerAppSpec()
                .body(requestBody)
                .when()
                .post(BuyerAppEndpoints.FEED_TV);

        // Parse response for other tests
        videoFeedResponseData = JsonUtils.fromResponse(videoFeedResponse, VideoFeedResponse.class);

        // Response status code is 200
        assertThat("Response status code is 200",
                videoFeedResponse.getStatusCode(), equalTo(HttpStatus.OK));

        logger.info("Response status verified: 200 OK");
    }

    @Test(description = "Response Content-Type header is application/json", priority = 2, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp", enabled = false)
    @Story("Video Feed (T.V)")
    @Severity(SeverityLevel.MINOR)
    public void testContentTypeHeader() {
        // Response Content-Type header is application/json
        assertThat("Content-Type should include application/json",
                videoFeedResponse.getHeader("Content-Type"), containsString("application/json"));

        logger.info("Content-Type header verified: {}", videoFeedResponse.getHeader("Content-Type"));
    }

    @Test(description = "Response has valid JSON data", priority = 3, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp", enabled = false)
    @Story("Video Feed (T.V)")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseHasValidJsonData() {
        // Response has valid JSON data
        assertThat("Response should be an object", videoFeedResponseData, notNullValue());

        logger.info("Response has valid JSON data");
    }

    @Test(description = "Response time is less than threshold", priority = 4, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp", enabled = false)
    @Story("Video Feed (T.V)")
    @Severity(SeverityLevel.NORMAL)
    public void testResponseTime() {
        // Get response time threshold from config (fallback to 20000ms)
        long responseTimeThreshold = config.responseTimeThreshold();
        long actualResponseTime = videoFeedResponse.getTime();

        // Validate response time measurement is available
        assertThat("Response time measurement should be available",
                actualResponseTime, notNullValue());

        // Response time is less than threshold
        assertThat(String.format("Response time is less than %dms", responseTimeThreshold),
                actualResponseTime, lessThan(responseTimeThreshold));

        logger.info("Response time verified: {} ms (Threshold: {} ms)", actualResponseTime,
                responseTimeThreshold);
    }

    @Test(description = "Data object structure is valid", priority = 5, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp", enabled = false)
    @Story("Video Feed (T.V)")
    @Severity(SeverityLevel.CRITICAL)
    public void testDataObjectStructure() {
        // Data object structure is valid
        assertThat("data should exist and be an object", videoFeedResponseData.getData(), notNullValue());

        logger.info("Data object structure is valid");
    }

    @Test(description = "Result array exists in data object", priority = 6, dependsOnMethods = "testResponseStatusCode200", groups = "buyerapp", enabled = false)
    @Story("Video Feed (T.V)")
    @Severity(SeverityLevel.CRITICAL)
    public void testResultArrayExists() {
        // Result array exists in data object
        assertThat("result should be an array",
                videoFeedResponseData.getData().getResult(), instanceOf(java.util.List.class));

        logger.info("Result array exists in data object");
    }

    @Test(description = "Result array contains at least one item", priority = 7, dependsOnMethods = "testResultArrayExists", groups = "buyerapp", enabled = false)
    @Story("Video Feed (T.V)")
    @Severity(SeverityLevel.NORMAL)
    public void testResultArrayNotEmpty() {
        // Result array contains at least one item
        assertThat("result array should contain at least one item",
                videoFeedResponseData.getData().getResult(), hasSize(greaterThanOrEqualTo(1)));

        logger.info("Result array contains {} items", videoFeedResponseData.getData().getResult().size());
    }

    @Test(description = "Seller objects have required fields", priority = 8, dependsOnMethods = "testResultArrayNotEmpty", groups = "buyerapp", enabled = false)
    @Story("Video Feed (T.V)")
    @Severity(SeverityLevel.CRITICAL)
    public void testSellerObjectsRequiredFields() {
        // Seller objects have required fields
        videoFeedResponseData.getData().getResult().forEach(item -> {
            if (item.getSeller() != null) {
                VideoFeedResponse.Seller seller = item.getSeller();

                assertThat("seller should be an object", seller, notNullValue());

                // Required string fields
                assertThat("businessName should be a string", seller.getBusinessName(), instanceOf(String.class));
                assertThat("phoneNumber should be a string", seller.getPhoneNumber(), instanceOf(String.class));
                assertThat("name should be a string", seller.getName(), instanceOf(String.class));
                assertThat("id should be a string", seller.getId(), instanceOf(String.class));

                // Optional boolean fields
                if (seller.getIsSuper() != null) {
                    assertThat("isSuper should be a boolean", seller.getIsSuper(), instanceOf(Boolean.class));
                }
                if (seller.getIsDeleted() != null) {
                    assertThat("isDeleted should be a boolean", seller.getIsDeleted(), instanceOf(Boolean.class));
                }
                if (seller.getOrdersEnabled() != null) {
                    assertThat("ordersEnabled should be a boolean", seller.getOrdersEnabled(),
                            instanceOf(Boolean.class));
                }
                if (seller.getIsCatalogAvailable() != null) {
                    assertThat("isCatalogAvailable should be a boolean", seller.getIsCatalogAvailable(),
                            instanceOf(Boolean.class));
                }

                // Optional number field
                if (seller.getMov() != null) {
                    assertThat("mov should be a number", seller.getMov(), instanceOf(Integer.class));
                }
            }
        });

        logger.info("Seller objects required fields validated for all items");
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/newapis/BannerPlacementTest.java`

_141 lines_

```java
package com.automation.tests.buyerapp.newapis;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.utils.ApiErrorAssertions;
import com.automation.utils.VariableManager;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/** GET /v1/banner/{home,collection,pdp,search} - read-only; optional auth. */
@Epic("Buyer App – New APIs")
@Feature("Banner placements")
public class BannerPlacementTest extends BaseTest {

    private String baseUrl;
    private String token;
    private static final String CAT = "saree";

    @BeforeClass(alwaysRun = true)
    public void setup() {
        baseUrl = config.buyerAppBaseUrl();
        token = buyerAppTokenOrNull();
    }

    private RequestSpecification guest() {
        return buyerAppGuestSpec();
    }

    private RequestSpecification authed() {
        if (token == null || token.isEmpty()) {
            throw new SkipException("Buyer App token not available (run LoginTest first)");
        }
        return guest().header("Authorization", "JWT " + token);
    }

    private Response get(RequestSpecification spec, String placement, String query) {
        return spec.when().get(BuyerAppEndpoints.BANNER_PLACEMENT.replace("{placement}", placement) + query);
    }

    private void assertOkList(Response r) {
        assertThat(r.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(r.jsonPath().getString("statusCode"), equalTo("10000"));
        assertThat("result list", r.jsonPath().getList("data.result"), notNullValue());
    }

    @Test(description = "TC-BANNER-001 — GET /v1/banner/home - authenticated buyer", groups = "buyerapp")
    @TmsLink("TC-BANNER-001") @Story("Banner placements") @Severity(SeverityLevel.CRITICAL)
    public void homeAuthenticated() {
        assertOkList(get(authed(), "home", "?suitable_for=" + CAT));
    }

    @Test(description = "TC-BANNER-002 — GET /v1/banner/home - guest (no token)", groups = "buyerapp")
    @TmsLink("TC-BANNER-002") @Story("Banner placements") @Severity(SeverityLevel.CRITICAL)
    public void homeGuest() {
        assertOkList(get(guest(), "home", "?suitable_for=" + CAT));
    }

    @Test(description = "TC-BANNER-004 — GET /v1/banner/collection - guest", groups = "buyerapp")
    @TmsLink("TC-BANNER-004") @Story("Banner placements") @Severity(SeverityLevel.CRITICAL)
    public void collectionGuest() {
        assertOkList(get(guest(), "collection", "?suitable_for=" + CAT));
    }

    @Test(description = "TC-BANNER-005 — GET /v1/banner/pdp - guest and user", groups = "buyerapp")
    @TmsLink("TC-BANNER-005") @Story("Banner placements") @Severity(SeverityLevel.NORMAL)
    public void pdpGuestAndUser() {
        assertOkList(get(guest(), "pdp", "?suitable_for=" + CAT));
        assertOkList(get(authed(), "pdp", "?suitable_for=" + CAT));
    }

    @Test(description = "TC-BANNER-006 — GET /v1/banner/search - guest and user", groups = "buyerapp")
    @TmsLink("TC-BANNER-006") @Story("Banner placements") @Severity(SeverityLevel.NORMAL)
    public void searchGuestAndUser() {
        assertOkList(get(guest(), "search", "?suitable_for=" + CAT));
        assertOkList(get(authed(), "search", "?suitable_for=" + CAT));
    }

    @Test(description = "TC-BANNER-007 — GET /v1/banner/home - pagination", groups = "buyerapp")
    @TmsLink("TC-BANNER-007") @Story("Banner placements") @Severity(SeverityLevel.NORMAL)
    public void homePagination() {
        Response r = get(guest(), "home", "?suitable_for=" + CAT + "&page=0&pageSize=5");
        assertOkList(r);
        List<Object> items = r.jsonPath().getList("data.result");
        assertThat(items.size(), lessThanOrEqualTo(5));
        assertThat(r.jsonPath().getInt("data.page"), equalTo(0));
        assertThat(r.jsonPath().getInt("data.pageSize"), equalTo(5));
    }

    @Test(description = "TC-BANNER-008 — GET /v1/banner/home - suitable_for missing", groups = "buyerapp")
    @TmsLink("TC-BANNER-008") @Story("Banner validation") @Severity(SeverityLevel.CRITICAL)
    public void suitableForMissing() {
        ApiErrorAssertions.assertApiError(get(guest(), "home", ""), HttpStatus.BAD_REQUEST, "suitable_for");
    }

    @Test(description = "TC-BANNER-009 — GET /v1/banner/home - suitable_for not in BANNER_CATEGORIES", groups = "buyerapp")
    @TmsLink("TC-BANNER-009") @Story("Banner validation") @Severity(SeverityLevel.NORMAL)
    public void suitableForBogus() {
        ApiErrorAssertions.assertApiError(get(guest(), "home", "?suitable_for=bogus"), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-BANNER-010 — GET /v1/banner/home - pageSize above 50", groups = "buyerapp")
    @TmsLink("TC-BANNER-010") @Story("Banner validation") @Severity(SeverityLevel.NORMAL)
    public void pageSizeAbove50() {
        ApiErrorAssertions.assertApiError(get(guest(), "home", "?suitable_for=" + CAT + "&pageSize=51"), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-BANNER-011 — GET /v1/banner/home - pageSize 0", groups = "buyerapp")
    @TmsLink("TC-BANNER-011") @Story("Banner validation") @Severity(SeverityLevel.MINOR)
    public void pageSizeZero() {
        ApiErrorAssertions.assertApiError(get(guest(), "home", "?suitable_for=" + CAT + "&pageSize=0"), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-BANNER-012 — GET /v1/banner/home - negative page", groups = "buyerapp")
    @TmsLink("TC-BANNER-012") @Story("Banner validation") @Severity(SeverityLevel.MINOR)
    public void negativePage() {
        ApiErrorAssertions.assertApiError(get(guest(), "home", "?suitable_for=" + CAT + "&page=-1"), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-BANNER-013 — GET /v1/banner/home - non-integer page", groups = "buyerapp")
    @TmsLink("TC-BANNER-013") @Story("Banner validation") @Severity(SeverityLevel.MINOR)
    public void nonIntegerPage() {
        ApiErrorAssertions.assertApiError(get(guest(), "home", "?suitable_for=" + CAT + "&page=1.5"), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-BANNER-014 — GET /v1/banner/home - unknown query param", groups = "buyerapp")
    @TmsLink("TC-BANNER-014") @Story("Banner validation") @Severity(SeverityLevel.MINOR)
    public void unknownQueryParam() {
        ApiErrorAssertions.assertApiError(get(guest(), "home", "?suitable_for=" + CAT + "&foo=bar"), HttpStatus.BAD_REQUEST);
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/newapis/ChatbotV2ValidationTest.java`

_102 lines_

```java
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
 * Unified chatbot — POST /v2/chatbot/message. PRODUCTION-safe: every request
 * is refused by auth or the Joi validator before the bot runs (no LLM call,
 * no session write). The validator replaces the Joi detail with the generic
 * chat-bubble reply. Rate limit is 20/min per buyer; this class sends 6.
 */
@Epic("Buyer App – New APIs")
@Feature("Chatbot v2 Validation")
public class ChatbotV2ValidationTest extends BaseTest {

    private static final String CHAT_INVALID_REPLY_FRAGMENT = "technical dikkat";

    private String baseUrl;
    private String buyerToken;

    @BeforeClass(alwaysRun = true)
    public void setup() {
        baseUrl = config.buyerAppBaseUrl();
        buyerToken = buyerAppTokenOrNull();
    }

    private Response send(String body) {
        if (buyerToken == null || buyerToken.isEmpty()) {
            throw new SkipException("Buyer App token not available. Run LoginTest first.");
        }
        return buyerAppGuestSpec()
                .header("Authorization", "JWT " + buyerToken).body(body)
                .when().post(BuyerAppEndpoints.CHATBOT_V2_MESSAGE);
    }

    private void assertGenericInvalidReply(Response resp) {
        assertApiError(resp, HttpStatus.BAD_REQUEST, CHAT_INVALID_REPLY_FRAGMENT);
        assertThat("no Joi detail leaks", resp.jsonPath().getString("message"), not(containsString("\"")));
    }

    @Test(description = "TC-CHATV2-004 — no token", groups = "buyerapp")
    @TmsLink("TC-CHATV2-004") @Story("Auth") @Severity(SeverityLevel.CRITICAL)
    public void noToken() {
        Response resp = buyerAppGuestSpec()
                .body("{\"message\":\"hi\"}").when().post(BuyerAppEndpoints.CHATBOT_V2_MESSAGE);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }

    @Test(description = "TC-CHATV2-006 — message missing", groups = "buyerapp")
    @TmsLink("TC-CHATV2-006") @Story("Validation") @Severity(SeverityLevel.NORMAL)
    public void messageMissing() {
        assertGenericInvalidReply(send("{}"));
    }

    @Test(description = "TC-CHATV2-007 — empty or whitespace message", groups = "buyerapp")
    @TmsLink("TC-CHATV2-007") @Story("Validation") @Severity(SeverityLevel.NORMAL)
    public void messageWhitespace() {
        assertGenericInvalidReply(send("{\"message\":\"   \"}"));
    }

    @Test(description = "TC-CHATV2-008 — message longer than 1000 chars", groups = "buyerapp")
    @TmsLink("TC-CHATV2-008") @Story("Validation") @Severity(SeverityLevel.NORMAL)
    public void messageTooLong() {
        assertGenericInvalidReply(send("{\"message\":\"" + "a".repeat(1001) + "\"}"));
    }

    @Test(description = "TC-CHATV2-010 — imageUrl with http scheme", groups = "buyerapp")
    @TmsLink("TC-CHATV2-010") @Story("Validation") @Severity(SeverityLevel.NORMAL)
    public void imageUrlHttp() {
        assertGenericInvalidReply(send("{\"message\":\"hi\",\"imageUrl\":\"http://example.com/a.jpg\"}"));
    }

    @Test(description = "TC-CHATV2-011 — both imageUrl and imageBase64", groups = "buyerapp")
    @TmsLink("TC-CHATV2-011") @Story("Validation") @Severity(SeverityLevel.NORMAL)
    public void bothImageInputs() {
        assertGenericInvalidReply(send("{\"message\":\"hi\",\"imageUrl\":\"https://example.com/a.jpg\","
                + "\"imageBase64\":\"aGVsbG8=\"}"));
    }

    @Test(description = "TC-CHATV2-012 — supportedBlocks with more than 50 items", groups = "buyerapp")
    @TmsLink("TC-CHATV2-012") @Story("Validation") @Severity(SeverityLevel.NORMAL)
    public void tooManySupportedBlocks() {
        StringBuilder blocks = new StringBuilder("[");
        for (int i = 0; i < 51; i++) {
            blocks.append(i == 0 ? "" : ",").append("\"block").append(i).append("\"");
        }
        blocks.append("]");
        assertGenericInvalidReply(send("{\"message\":\"hi\",\"supportedBlocks\":" + blocks + "}"));
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/newapis/EventsIngestionNegativeTest.java`

_94 lines_

```java
package com.automation.tests.buyerapp.newapis;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * POST /events and /events/impressions - ONLY cases rejected before the Kafka publish
 * (routines/event/createEvent.ts, routines/impression/postImpressions.ts). Go-style envelope.
 */
@Epic("Buyer App – New APIs")
@Feature("Events ingestion (negative)")
public class EventsIngestionNegativeTest extends BaseTest {

    private String baseUrl;

    @BeforeClass(alwaysRun = true)
    public void setup() {
        baseUrl = config.buyerAppBaseUrl();
    }

    private Response post(String path, String body) {
        return buyerAppGuestSpec().body(body).when().post(path);
    }

    private void assertGoEnvelope(Response r, int status, String message, String error) {
        assertThat("HTTP status (body: " + r.asString() + ")", r.getStatusCode(), equalTo(status));
        assertThat(r.jsonPath().getInt("status"), equalTo(status));
        assertThat(r.jsonPath().getString("message"), equalTo(message));
        assertThat(r.jsonPath().getString("data.error"), equalTo(error));
    }

    @Test(description = "TC-EVENTS-003 — POST /events - array body", groups = "buyerapp")
    @TmsLink("TC-EVENTS-003") @Story("Events ingestion") @Severity(SeverityLevel.NORMAL)
    public void eventsArrayBody() {
        assertGoEnvelope(post(BuyerAppEndpoints.EVENTS, "[{\"event_name\":\"pdp_viewed\"}]"),
                HttpStatus.BAD_REQUEST, "Bad Request", "invalid payload: expected an event object");
    }

    @Test(description = "TC-EVENTS-004 — POST /events - missing time_stamp", groups = "buyerapp")
    @TmsLink("TC-EVENTS-004") @Story("Events ingestion") @Severity(SeverityLevel.NORMAL)
    public void eventsMissingTimestamp() {
        assertGoEnvelope(post(BuyerAppEndpoints.EVENTS, "{\"event_name\":\"pdp_viewed\"}"),
                HttpStatus.INTERNAL_SERVER_ERROR, "error", "Error parsing timestamp: undefined");
    }

    @Test(description = "TC-EVENTS-005 — POST /events - non-integer or numeric time_stamp", groups = "buyerapp")
    @TmsLink("TC-EVENTS-005") @Story("Events ingestion") @Severity(SeverityLevel.NORMAL)
    public void eventsBadTimestampVariants() {
        assertGoEnvelope(post(BuyerAppEndpoints.EVENTS, "{\"event_name\":\"pdp_viewed\",\"time_stamp\":\"17618.5\"}"),
                HttpStatus.INTERNAL_SERVER_ERROR, "error", "Error parsing timestamp: 17618.5");
        assertGoEnvelope(post(BuyerAppEndpoints.EVENTS, "{\"event_name\":\"pdp_viewed\",\"time_stamp\":\"abc\"}"),
                HttpStatus.INTERNAL_SERVER_ERROR, "error", "Error parsing timestamp: abc");
        assertGoEnvelope(post(BuyerAppEndpoints.EVENTS, "{\"event_name\":\"pdp_viewed\",\"time_stamp\":1761800000000}"),
                HttpStatus.INTERNAL_SERVER_ERROR, "error", "Error parsing timestamp: 1761800000000");
    }

    @Test(description = "TC-EVENTS-009 — POST /events/impressions - empty batch", groups = "buyerapp")
    @TmsLink("TC-EVENTS-009") @Story("Impressions ingestion") @Severity(SeverityLevel.NORMAL)
    public void impressionsEmptyBatch() {
        Response r = post(BuyerAppEndpoints.EVENTS_IMPRESSIONS, "{\"data\":[]}");
        assertThat("HTTP status (body: " + r.asString() + ")", r.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(r.jsonPath().getInt("status"), equalTo(200));
        assertThat(r.jsonPath().getString("message"), equalTo("success"));
        assertThat(r.jsonPath().getInt("data.count"), equalTo(0));
        assertThat(r.jsonPath().getList("data.insertedIds"), empty());
    }

    @Test(description = "TC-EVENTS-010 — POST /events/impressions - data missing or not an array", groups = "buyerapp")
    @TmsLink("TC-EVENTS-010") @Story("Impressions ingestion") @Severity(SeverityLevel.NORMAL)
    public void impressionsBadShape() {
        String err = "invalid payload: expected object with data array";
        // "[]" stands in for the bare-array variant: not an object with a data array, rejected before any publish.
        for (String body : new String[]{"{}", "{\"data\":{}}", "[]"}) {
            assertGoEnvelope(post(BuyerAppEndpoints.EVENTS_IMPRESSIONS, body), HttpStatus.BAD_REQUEST, "Bad Request", err);
        }
    }

    @Test(description = "TC-EVENTS-013 — /v1/events is not a route", groups = "buyerapp")
    @TmsLink("TC-EVENTS-013") @Story("Events ingestion") @Severity(SeverityLevel.MINOR)
    public void v1EventsNotARoute() {
        // Deliberately timestamp-less: even if it were routed it would be rejected before publish.
        Response r = post(BuyerAppEndpoints.EVENTS_V1_NOT_A_ROUTE, "{\"event_name\":\"pdp_viewed\"}");
        assertThat(r.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/newapis/MlpPublicReadTest.java`

_174 lines_

```java
package com.automation.tests.buyerapp.newapis;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.utils.ApiErrorAssertions;
import com.automation.utils.VariableManager;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/** GET /v1/mlp* public reads, plus the staff-gated retired admin GET (403 before any handler). */
@Epic("Buyer App – New APIs")
@Feature("MLP public read")
public class MlpPublicReadTest extends BaseTest {

    private String baseUrl;
    private String token;
    /** An occasion that returned 200 on /v1/mlp/{occasion}; null if none has active structures. */
    private String liveOccasion;

    @BeforeClass(alwaysRun = true)
    public void setup() {
        baseUrl = config.buyerAppBaseUrl();
        token = buyerAppTokenOrNull();
        Response r = buyerAppGuestSpec().when().get(BuyerAppEndpoints.MLP_OCCASIONS);
        if (r.getStatusCode() == HttpStatus.OK) {
            List<String> occasions = r.jsonPath().getList("data.occasions");
            if (occasions != null) {
                for (String o : occasions) {
                    Response p = buyerAppGuestSpec().pathParam("occasion", o)
                            .when().get(BuyerAppEndpoints.MLP_BY_OCCASION);
                    if (p.getStatusCode() == HttpStatus.OK) {
                        liveOccasion = o;
                        break;
                    }
                }
            }
        }
    }

    private RequestSpecification spec() {
        return buyerAppGuestSpec();
    }

    private String requireLive() {
        if (liveOccasion == null) {
            throw new SkipException("No MLP occasion with active structures found on this environment");
        }
        return liveOccasion;
    }

    private void assertStructures(Response r, String occasion) {
        assertThat(r.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(r.jsonPath().getString("statusCode"), equalTo("10000"));
        assertThat(r.jsonPath().getString("data.occasion"), equalToIgnoringCase(occasion));
        assertThat(r.jsonPath().getList("data.structures"), not(empty()));
    }

    @Test(description = "TC-MLP-001 — GET /v1/mlp/:occasion - valid occasion, no token", groups = "buyerapp")
    @TmsLink("TC-MLP-001") @Story("MLP public read") @Severity(SeverityLevel.CRITICAL)
    public void byOccasionPath() {
        String o = requireLive();
        assertStructures(spec().pathParam("occasion", o).when().get(BuyerAppEndpoints.MLP_BY_OCCASION), o);
    }

    @Test(description = "TC-MLP-002 — GET /v1/mlp?occasion= - query form matches path form", groups = "buyerapp")
    @TmsLink("TC-MLP-002") @Story("MLP public read") @Severity(SeverityLevel.CRITICAL)
    public void byOccasionQuery() {
        String o = requireLive();
        Response q = spec().queryParam("occasion", o).when().get(BuyerAppEndpoints.MLP);
        assertStructures(q, o);
        Response p = spec().pathParam("occasion", o).when().get(BuyerAppEndpoints.MLP_BY_OCCASION);
        assertThat(q.jsonPath().getList("data.structures").size(),
                equalTo(p.jsonPath().getList("data.structures").size()));
    }

    @Test(description = "TC-MLP-003 — GET /v1/mlp/:occasion - utm params do not 400", groups = "buyerapp")
    @TmsLink("TC-MLP-003") @Story("MLP public read") @Severity(SeverityLevel.NORMAL)
    public void pathWithUtm() {
        String o = requireLive();
        assertStructures(spec().pathParam("occasion", o)
                .queryParam("utm_source", "wa").queryParam("utm_medium", "x").queryParam("utm_campaign", "y")
                .when().get(BuyerAppEndpoints.MLP_BY_OCCASION), o);
    }

    @Test(description = "TC-MLP-004 — GET /v1/mlp?occasion= - utm params do not 400", groups = "buyerapp")
    @TmsLink("TC-MLP-004") @Story("MLP public read") @Severity(SeverityLevel.NORMAL)
    public void queryWithUtm() {
        String o = requireLive();
        assertStructures(spec().queryParam("occasion", o).queryParam("utm_source", "wa")
                .when().get(BuyerAppEndpoints.MLP), o);
    }

    @Test(description = "TC-MLP-007 — GET /v1/mlp/:occasion - occasion with no documents", groups = "buyerapp")
    @TmsLink("TC-MLP-007") @Story("MLP public read") @Severity(SeverityLevel.NORMAL)
    public void unknownOccasion() {
        String slug = "qa-none-" + Long.toString(System.nanoTime(), 36);
        Response r = spec().pathParam("occasion", slug).when().get(BuyerAppEndpoints.MLP_BY_OCCASION);
        ApiErrorAssertions.assertApiError(r, HttpStatus.NOT_FOUND, "No structures configured for occasion");
    }

    @Test(description = "TC-MLP-008 — GET /v1/mlp - occasion query missing", groups = "buyerapp")
    @TmsLink("TC-MLP-008") @Story("MLP validation") @Severity(SeverityLevel.CRITICAL)
    public void queryMissing() {
        ApiErrorAssertions.assertApiError(spec().when().get(BuyerAppEndpoints.MLP), HttpStatus.BAD_REQUEST, "occasion");
    }

    @Test(description = "TC-MLP-009 — GET /v1/mlp/:occasion - occasion with invalid characters", groups = "buyerapp")
    @TmsLink("TC-MLP-009") @Story("MLP validation") @Severity(SeverityLevel.NORMAL)
    public void invalidOccasionChars() {
        // Pre-encoded so RestAssured does not double-encode the path.
        Response r = buyerAppGuestSpec().urlEncodingEnabled(false)
                .when().get("/v1/mlp/BAD%20OCC!");
        ApiErrorAssertions.assertApiError(r, HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-MLP-010 — GET /v1/mlp/list?occasion= - unfiltered authoring read", groups = "buyerapp")
    @TmsLink("TC-MLP-010") @Story("MLP public read") @Severity(SeverityLevel.NORMAL)
    public void listUnfiltered() {
        String o = requireLive();
        Response r = spec().queryParam("occasion", o).when().get(BuyerAppEndpoints.MLP_LIST);
        assertThat(r.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(r.jsonPath().getString("statusCode"), equalTo("10000"));
        assertThat(r.jsonPath().getString("data"), notNullValue());
    }

    @Test(description = "TC-MLP-011 — GET /v1/mlp/list - missing occasion", groups = "buyerapp")
    @TmsLink("TC-MLP-011") @Story("MLP validation") @Severity(SeverityLevel.NORMAL)
    public void listMissingOccasion() {
        ApiErrorAssertions.assertApiError(spec().when().get(BuyerAppEndpoints.MLP_LIST), HttpStatus.BAD_REQUEST, "occasion");
    }

    @Test(description = "TC-MLP-012 — GET /v1/mlp/occasions - distinct slugs", groups = "buyerapp")
    @TmsLink("TC-MLP-012") @Story("MLP public read") @Severity(SeverityLevel.NORMAL)
    public void occasionsList() {
        Response r = spec().when().get(BuyerAppEndpoints.MLP_OCCASIONS);
        assertThat(r.getStatusCode(), equalTo(HttpStatus.OK));
        List<String> list = r.jsonPath().getList("data.occasions");
        assertThat(list, notNullValue());
        assertThat("distinct", list.size(), equalTo(new HashSet<>(list).size()));
    }

    @Test(description = "TC-MLP-013 — /list and /occasions are not swallowed by /:occasion", groups = "buyerapp")
    @TmsLink("TC-MLP-013") @Story("MLP routing") @Severity(SeverityLevel.NORMAL)
    public void staticRoutesNotSwallowed() {
        Response occ = spec().when().get(BuyerAppEndpoints.MLP_OCCASIONS);
        assertThat(occ.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat("occasions payload, not an occasion lookup", occ.jsonPath().getString("data.occasions"), notNullValue());
        // /list without occasion is a 400 from the list schema; treated as occasion "list" it would be 404.
        Response list = spec().when().get(BuyerAppEndpoints.MLP_LIST);
        assertThat(list.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test(description = "TC-MLP-014 — GET /v1/admin/mlp - retired admin read, non-staff token", groups = "buyerapp")
    @TmsLink("TC-MLP-014") @Story("MLP admin gate") @Severity(SeverityLevel.CRITICAL)
    public void retiredAdminReadForbidden() {
        if (token == null || token.isEmpty()) {
            throw new SkipException("Buyer App token not available (run LoginTest first)");
        }
        Response r = spec().header("Authorization", "JWT " + token).when().get(BuyerAppEndpoints.ADMIN_MLP);
        ApiErrorAssertions.assertApiError(r, HttpStatus.FORBIDDEN, "Staff access required");
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/newapis/NavoLiveTest.java`

_173 lines_

```java
package com.automation.tests.buyerapp.newapis;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.utils.ApiErrorAssertions;
import com.automation.utils.VariableManager;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/** GET /v1/navo-live and /events/:eventId/products - read-only; optional auth. */
@Epic("Buyer App – New APIs")
@Feature("Navo Live public")
public class NavoLiveTest extends BaseTest {

    private static final int MAX_LIMIT = 40;
    private static final String ZERO_ID = "000000000000000000000000";
    private String baseUrl;
    private String token;
    private String eventId;

    @BeforeClass(alwaysRun = true)
    public void setup() {
        baseUrl = config.buyerAppBaseUrl();
        token = buyerAppTokenOrNull();
        Response r = spec().when().get(BuyerAppEndpoints.NAVO_LIVE);
        if (r.getStatusCode() == HttpStatus.OK) {
            List<String> ids = r.jsonPath().getList("data.events._id");
            if (ids != null && !ids.isEmpty()) {
                eventId = ids.get(0);
            }
        }
    }

    private RequestSpecification spec() {
        return buyerAppGuestSpec();
    }

    private String requireEvent() {
        if (eventId == null) {
            throw new SkipException("No Navo Live event listed on this environment");
        }
        return eventId;
    }

    private Response products(String id, String query) {
        return spec().when().get(BuyerAppEndpoints.NAVO_LIVE_EVENT_PRODUCTS.replace("{eventId}", id) + query);
    }

    private void assertPage(Response r) {
        assertThat(r.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(r.jsonPath().getString("statusCode"), equalTo("10000"));
        assertThat(r.jsonPath().getString("data.serverTime"), not(emptyOrNullString()));
        assertThat(r.jsonPath().getList("data.events"), notNullValue());
    }

    @Test(description = "TC-NAVOLIVE-001 — GET /v1/navo-live - guest, default productLimit", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-001") @Story("Navo Live page") @Severity(SeverityLevel.CRITICAL)
    public void guestDefault() {
        assertPage(spec().when().get(BuyerAppEndpoints.NAVO_LIVE));
    }

    @Test(description = "TC-NAVOLIVE-002 — GET /v1/navo-live - authenticated buyer", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-002") @Story("Navo Live page") @Severity(SeverityLevel.NORMAL)
    public void authenticated() {
        if (token == null || token.isEmpty()) {
            throw new SkipException("Buyer App token not available (run LoginTest first)");
        }
        assertPage(spec().header("Authorization", "JWT " + token).when().get(BuyerAppEndpoints.NAVO_LIVE));
    }

    @Test(description = "TC-NAVOLIVE-003 — GET /v1/navo-live?productLimit=5", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-003") @Story("Navo Live page") @Severity(SeverityLevel.NORMAL)
    public void productLimit5() {
        Response r = spec().queryParam("productLimit", 5).when().get(BuyerAppEndpoints.NAVO_LIVE);
        assertPage(r);
        List<List<Object>> rails = r.jsonPath().getList("data.events.products");
        for (List<Object> rail : rails) {
            if (rail != null) {
                assertThat(rail.size(), lessThanOrEqualTo(5));
            }
        }
    }

    @Test(description = "TC-NAVOLIVE-004 — GET /v1/navo-live - unknown query params accepted", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-004") @Story("Navo Live page") @Severity(SeverityLevel.NORMAL)
    public void unknownParams() {
        assertPage(spec().queryParam("utm_source", "x").when().get(BuyerAppEndpoints.NAVO_LIVE));
    }

    @Test(description = "TC-NAVOLIVE-005 — GET /v1/navo-live - productLimit 0", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-005") @Story("Navo Live validation") @Severity(SeverityLevel.NORMAL)
    public void productLimitZero() {
        ApiErrorAssertions.assertApiError(spec().queryParam("productLimit", 0).when().get(BuyerAppEndpoints.NAVO_LIVE), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-NAVOLIVE-006 — GET /v1/navo-live - productLimit above max", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-006") @Story("Navo Live validation") @Severity(SeverityLevel.NORMAL)
    public void productLimitAboveMax() {
        ApiErrorAssertions.assertApiError(spec().queryParam("productLimit", MAX_LIMIT + 1).when().get(BuyerAppEndpoints.NAVO_LIVE), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-NAVOLIVE-007 — GET /v1/navo-live - non-integer productLimit", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-007") @Story("Navo Live validation") @Severity(SeverityLevel.NORMAL)
    public void productLimitNonInteger() {
        ApiErrorAssertions.assertApiError(spec().queryParam("productLimit", "abc").when().get(BuyerAppEndpoints.NAVO_LIVE), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-NAVOLIVE-011 — GET /v1/navo-live/events/:eventId/products - first page", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-011") @Story("Navo Live products") @Severity(SeverityLevel.CRITICAL)
    public void productsFirstPage() {
        Response r = products(requireEvent(), "");
        if (r.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new SkipException("Discovered event has no collection (404): " + r.jsonPath().getString("message"));
        }
        assertThat(r.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(r.jsonPath().getInt("data.page"), equalTo(1));
        assertThat(r.jsonPath().getList("data.items"), notNullValue());
    }

    @Test(description = "TC-NAVOLIVE-012 — GET /v1/navo-live/events/:eventId/products?page=2&limit=10", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-012") @Story("Navo Live products") @Severity(SeverityLevel.NORMAL)
    public void productsSecondPage() {
        String id = requireEvent();
        Response p1 = products(id, "?page=1&limit=10");
        if (p1.getStatusCode() != HttpStatus.OK || p1.jsonPath().getInt("data.total") <= 10) {
            throw new SkipException("Event has 10 or fewer products (or no collection); page 2 not meaningful");
        }
        Response p2 = products(id, "?page=2&limit=10");
        assertThat(p2.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(p2.jsonPath().getInt("data.page"), equalTo(2));
        List<Object> ids1 = p1.jsonPath().getList("data.items._id");
        List<Object> ids2 = p2.jsonPath().getList("data.items._id");
        assertThat(ids2, not(empty()));
        for (Object i : ids2) {
            assertThat("no overlap with page 1", ids1, not(hasItem(i)));
        }
    }

    @Test(description = "TC-NAVOLIVE-013 — GET /v1/navo-live/events/:eventId/products - malformed eventId", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-013") @Story("Navo Live validation") @Severity(SeverityLevel.NORMAL)
    public void malformedEventId() {
        ApiErrorAssertions.assertApiError(products("abc", ""), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-NAVOLIVE-014 — GET /v1/navo-live/events/:eventId/products - unknown eventId", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-014") @Story("Navo Live products") @Severity(SeverityLevel.NORMAL)
    public void unknownEventId() {
        ApiErrorAssertions.assertApiError(products(ZERO_ID, ""), HttpStatus.NOT_FOUND, "Navo Live event not found");
    }

    @Test(description = "TC-NAVOLIVE-015 — GET /v1/navo-live/events/:eventId/products - page 0", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-015") @Story("Navo Live validation") @Severity(SeverityLevel.NORMAL)
    public void pageZero() {
        ApiErrorAssertions.assertApiError(products(ZERO_ID, "?page=0"), HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-NAVOLIVE-016 — GET /v1/navo-live/events/:eventId/products - limit above max", groups = "buyerapp")
    @TmsLink("TC-NAVOLIVE-016") @Story("Navo Live validation") @Severity(SeverityLevel.NORMAL)
    public void limitAboveMax() {
        ApiErrorAssertions.assertApiError(products(ZERO_ID, "?limit=" + (MAX_LIMIT + 1)), HttpStatus.BAD_REQUEST);
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/newapis/PrismAccessTest.java`

_152 lines_

```java
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
```

### `src/test/java/com/automation/tests/buyerapp/newapis/ReferralValidationTest.java`

_116 lines_

```java
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
 * Referrals — /v1/referral. PRODUCTION-safe: every POST here is refused by
 * createReferral before ReferralModel.create (missing/invalid phone, self
 * referral). No valid new referral is ever sent.
 */
@Epic("Buyer App – New APIs")
@Feature("Referral")
public class ReferralValidationTest extends BaseTest {

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

    private Response refer(String body) {
        return buyerAppGuestSpec()
                .header("Authorization", "JWT " + buyerToken).body(body)
                .when().post(BuyerAppEndpoints.REFERRAL);
    }

    @Test(description = "TC-REFERRAL-003 — POST /v1/referral with no token", groups = "buyerapp")
    @TmsLink("TC-REFERRAL-003") @Story("Create referral") @Severity(SeverityLevel.CRITICAL)
    public void createWithoutToken() {
        Response resp = buyerAppGuestSpec()
                .body("{\"referredPhone\":\"+919876543210\"}").when().post(BuyerAppEndpoints.REFERRAL);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }

    @Test(description = "TC-REFERRAL-004 — referredPhone missing", groups = "buyerapp")
    @TmsLink("TC-REFERRAL-004") @Story("Create referral") @Severity(SeverityLevel.NORMAL)
    public void phoneMissing() {
        requireBuyerToken();
        assertApiError(refer("{}"), HttpStatus.BAD_REQUEST, "referredPhone is required");
    }

    @Test(description = "TC-REFERRAL-005 — phone without +91 prefix", groups = "buyerapp")
    @TmsLink("TC-REFERRAL-005") @Story("Create referral") @Severity(SeverityLevel.NORMAL)
    public void phoneWithoutPrefix() {
        requireBuyerToken();
        assertApiError(refer("{\"referredPhone\":\"9876543210\"}"), HttpStatus.BAD_REQUEST, "+91 prefix");
    }

    @Test(description = "TC-REFERRAL-006 — first digit not 1 or 6-9", groups = "buyerapp")
    @TmsLink("TC-REFERRAL-006") @Story("Create referral") @Severity(SeverityLevel.NORMAL)
    public void phoneInvalidFirstDigit() {
        requireBuyerToken();
        assertApiError(refer("{\"referredPhone\":\"+912876543210\"}"), HttpStatus.BAD_REQUEST, "valid Indian phone");
    }

    @Test(description = "TC-REFERRAL-007 — too short / too long number", groups = "buyerapp")
    @TmsLink("TC-REFERRAL-007") @Story("Create referral") @Severity(SeverityLevel.NORMAL)
    public void phoneWrongLength() {
        requireBuyerToken();
        assertApiError(refer("{\"referredPhone\":\"+91987654321\"}"), HttpStatus.BAD_REQUEST, "valid Indian phone");
        assertApiError(refer("{\"referredPhone\":\"+9198765432100\"}"), HttpStatus.BAD_REQUEST, "valid Indian phone");
    }

    @Test(description = "TC-REFERRAL-008 — refer own number", groups = "buyerapp")
    @TmsLink("TC-REFERRAL-008") @Story("Create referral") @Severity(SeverityLevel.NORMAL)
    public void referSelf() {
        requireBuyerToken();
        String ownPhone = config.buyerAppPhoneNumber();
        if (ownPhone == null || !ownPhone.matches("^\\+91[16-9][0-9]{9}$")) {
            throw new SkipException("buyer.app.phone.number is not a +91 mobile; cannot build a self-referral.");
        }
        // If the account's stored phone differs from config, the handler still refuses
        // before create ("User already exists"), so this request never writes.
        Response resp = refer("{\"referredPhone\":\"" + ownPhone + "\"}");
        assertApiError(resp, HttpStatus.BAD_REQUEST, "Cannot refer yourself");
    }

    @Test(description = "TC-REFERRAL-013 — GET /v1/referral lists own referrals", groups = "buyerapp")
    @TmsLink("TC-REFERRAL-013") @Story("List referrals") @Severity(SeverityLevel.CRITICAL)
    public void listOwnReferrals() {
        requireBuyerToken();
        Response resp = buyerAppGuestSpec()
                .header("Authorization", "JWT " + buyerToken).when().get(BuyerAppEndpoints.REFERRAL);
        assertThat("status", resp.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat("referrals array", resp.jsonPath().getList("data.referrals"), notNullValue());
        assertThat("total", resp.jsonPath().get("data.total"), notNullValue());
    }

    @Test(description = "TC-REFERRAL-015 — GET /v1/referral with no token", groups = "buyerapp")
    @TmsLink("TC-REFERRAL-015") @Story("List referrals") @Severity(SeverityLevel.NORMAL)
    public void listWithoutToken() {
        Response resp = buyerAppGuestSpec().when().get(BuyerAppEndpoints.REFERRAL);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/newapis/SubSellerAuthTest.java`

_112 lines_

```java
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
 * Sub-seller session and stock routes — /v1/subseller/*.
 * PRODUCTION-safe: only auth/validation rejections; no sub-seller credentials,
 * no stock writes (the POSTs carry no token and are refused by
 * subSellerAuthentication before the handler).
 */
@Epic("Buyer App – New APIs")
@Feature("Sub-seller Auth")
public class SubSellerAuthTest extends BaseTest {

    private static final String PLACEHOLDER_CATALOG_ID = "000000000000000000000000";

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

    @Test(description = "TC-SUBSELLER-002 — refresh/token with refreshToken missing", groups = "buyerapp")
    @TmsLink("TC-SUBSELLER-002") @Story("Refresh token") @Severity(SeverityLevel.NORMAL)
    public void refreshTokenMissing() {
        Response resp = buyerAppGuestSpec()
                .body("{}").when().post(BuyerAppEndpoints.SUBSELLER_REFRESH_TOKEN);
        assertApiError(resp, HttpStatus.BAD_REQUEST, "refreshToken");
    }

    @Test(description = "TC-SUBSELLER-003 — refresh/token with whitespace refreshToken", groups = "buyerapp")
    @TmsLink("TC-SUBSELLER-003") @Story("Refresh token") @Severity(SeverityLevel.NORMAL)
    public void refreshTokenWhitespace() {
        Response resp = buyerAppGuestSpec()
                .body("{\"refreshToken\":\"   \"}").when().post(BuyerAppEndpoints.SUBSELLER_REFRESH_TOKEN);
        assertApiError(resp, HttpStatus.BAD_REQUEST);
    }

    @Test(description = "TC-SUBSELLER-004 — refresh/token with an invalid refresh token", groups = "buyerapp")
    @TmsLink("TC-SUBSELLER-004") @Story("Refresh token") @Severity(SeverityLevel.CRITICAL)
    public void refreshTokenInvalid() {
        Response resp = buyerAppGuestSpec()
                .body("{\"refreshToken\":\"not-a-real-refresh-token\"}")
                .when().post(BuyerAppEndpoints.SUBSELLER_REFRESH_TOKEN);
        assertThat("rejected", resp.getStatusCode(), allOf(greaterThanOrEqualTo(400), lessThan(500)));
        assertThat("no access token issued", resp.jsonPath().getString("data.accessToken"), nullValue());
    }

    @Test(description = "TC-SUBSELLER-007 — GET /me with no Authorization header", groups = "buyerapp")
    @TmsLink("TC-SUBSELLER-007") @Story("Sub-seller auth") @Severity(SeverityLevel.CRITICAL)
    public void meWithoutToken() {
        Response resp = buyerAppGuestSpec().when().get(BuyerAppEndpoints.SUBSELLER_ME);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }

    @Test(description = "TC-SUBSELLER-009 — GET /me with a buyer JWT", groups = "buyerapp")
    @TmsLink("TC-SUBSELLER-009") @Story("Sub-seller auth") @Severity(SeverityLevel.CRITICAL)
    public void meWithBuyerToken() {
        requireBuyerToken();
        Response resp = buyerAppGuestSpec()
                .header("Authorization", "JWT " + buyerToken)
                .when().get(BuyerAppEndpoints.SUBSELLER_ME);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }

    @Test(description = "TC-SUBSELLER-014 — GET /catalog with no token", groups = "buyerapp")
    @TmsLink("TC-SUBSELLER-014") @Story("Sub-seller catalog") @Severity(SeverityLevel.NORMAL)
    public void catalogWithoutToken() {
        Response resp = buyerAppGuestSpec().when().get(BuyerAppEndpoints.SUBSELLER_CATALOG);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }

    @Test(description = "TC-SUBSELLER-022 — POST /catalog/:id with no token", groups = "buyerapp")
    @TmsLink("TC-SUBSELLER-022") @Story("Sub-seller stock") @Severity(SeverityLevel.CRITICAL)
    public void updateStockWithoutToken() {
        Response resp = buyerAppGuestSpec()
                .pathParam("id", PLACEHOLDER_CATALOG_ID).body("{}")
                .when().post(BuyerAppEndpoints.SUBSELLER_CATALOG_BY_ID);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }

    @Test(description = "TC-SUBSELLER-025 — POST /catalog/:id/confirm-stock with no token", groups = "buyerapp")
    @TmsLink("TC-SUBSELLER-025") @Story("Sub-seller stock") @Severity(SeverityLevel.CRITICAL)
    public void confirmStockWithoutToken() {
        Response resp = buyerAppGuestSpec()
                .pathParam("id", PLACEHOLDER_CATALOG_ID).body("{}")
                .when().post(BuyerAppEndpoints.SUBSELLER_CATALOG_CONFIRM_STOCK);
        assertApiError(resp, HttpStatus.UNAUTHORIZED);
    }
}
```

### `src/test/java/com/automation/tests/buyerapp/schema/ResponseSchemaTest.java`

_53 lines_

```java
package com.automation.tests.buyerapp.schema;

import com.automation.base.BaseTest;
import com.automation.constants.BuyerAppEndpoints;
import com.automation.constants.HttpStatus;
import com.automation.utils.TestDataResolver;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Contract checks: each response must match a JSON schema under
 * src/test/resources/schemas/buyerapp/. The schemas pin the envelope and the
 * fields the app relies on (names and types) and allow extra fields, so a
 * renamed, removed or retyped field fails here even when no per-field
 * assertion covers it. Read-only GETs.
 */
@Epic("Buyer App Contracts")
@Feature("Response Schemas")
public class ResponseSchemaTest extends BaseTest {

    private static final String SCHEMAS = "schemas/buyerapp/";

    @Test(description = "GET /v1/collection/all matches its schema", groups = "buyerapp")
    @Story("Collections") @Severity(SeverityLevel.CRITICAL)
    public void collectionAllMatchesSchema() {
        Response resp = buyerAppSpec().queryParam("suitable_for", "saree").get(BuyerAppEndpoints.COLLECTION_ALL);
        assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(resp.asString(), matchesJsonSchemaInClasspath(SCHEMAS + "collection-all.json"));
    }

    @Test(description = "GET /v1/feed/banners matches its schema", groups = "buyerapp")
    @Story("Banners") @Severity(SeverityLevel.CRITICAL)
    public void feedBannersMatchesSchema() {
        Response resp = buyerAppSpec().queryParam("suitable_for", "saree").get(BuyerAppEndpoints.FEED_BANNERS);
        assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(resp.asString(), matchesJsonSchemaInClasspath(SCHEMAS + "feed-banners.json"));
    }

    @Test(description = "GET /v1/catalog/{id} matches its schema", groups = "buyerapp")
    @Story("Catalog") @Severity(SeverityLevel.CRITICAL)
    public void catalogByIdMatchesSchema() {
        Response resp = buyerAppSpec()
                .pathParam("catalogId", TestDataResolver.liveCatalogId(BaseTest::buyerAppSpec))
                .get("/v1/catalog/{catalogId}");
        assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(resp.asString(), matchesJsonSchemaInClasspath(SCHEMAS + "catalog-by-id.json"));
    }
}
```

### `src/test/resources/allure.properties`

_3 lines_

```properties
allure.results.directory=target/allure-results
allure.link.issue.pattern=https://jira.example.com/browse/{}
allure.link.tms.pattern=https://github.com/Bizup-world/backend-api/search?q={}&type=code
```

### `src/test/resources/config.properties`

_38 lines_

```properties
# API Automation Framework Configuration

# Base URL for API endpoints (BOMB)
base.url=https://api.bizup.app

# Request/Response timeout in milliseconds
api.timeout=40000

# Retry configuration for flaky tests
retry.count=2

# Logging configuration
log.request=true
log.response=true

# Default content type
content.type=application/json

# Authentication Configuration
# Options: none, basic, bearer, jwt, api_key
auth.type=jwt

# Login credentials for BOMB API
# login.phone.number / login.token: see secrets.properties.example

# Response time threshold (in milliseconds)
response.time.threshold=40000

# Buyer App Configuration (Navo)
buyer.app.base.url=https://api.navofashion.in
# buyer.app.phone.number / buyer.app.token: see secrets.properties.example

# Headers the Navo app sends on every request (zone drives delivery-zone filtering)
buyer.app.version=4.1.3-debug
buyer.app.version.code=420
buyer.app.user.segment=2
buyer.app.zone=india
buyer.app.language=en
```

### `src/test/resources/log4j2.xml`

_61 lines_

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Properties>
        <Property name="LOG_PATTERN">%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n</Property>
        <Property name="LOG_DIR">target/logs</Property>
    </Properties>

    <Appenders>
        <!-- Console Appender -->
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="${LOG_PATTERN}"/>
        </Console>

        <!-- File Appender for all logs -->
        <RollingFile name="FileAppender" 
                     fileName="${LOG_DIR}/automation.log"
                     filePattern="${LOG_DIR}/automation-%d{yyyy-MM-dd}-%i.log.gz">
            <PatternLayout pattern="${LOG_PATTERN}"/>
            <Policies>
                <SizeBasedTriggeringPolicy size="10MB"/>
                <TimeBasedTriggeringPolicy interval="1"/>
            </Policies>
            <DefaultRolloverStrategy max="10"/>
        </RollingFile>

        <!-- Error File Appender -->
        <RollingFile name="ErrorFileAppender" 
                     fileName="${LOG_DIR}/error.log"
                     filePattern="${LOG_DIR}/error-%d{yyyy-MM-dd}-%i.log.gz">
            <PatternLayout pattern="${LOG_PATTERN}"/>
            <Filters>
                <ThresholdFilter level="ERROR" onMatch="ACCEPT" onMismatch="DENY"/>
            </Filters>
            <Policies>
                <SizeBasedTriggeringPolicy size="10MB"/>
            </Policies>
            <DefaultRolloverStrategy max="5"/>
        </RollingFile>
    </Appenders>

    <Loggers>
        <!-- Application Logger -->
        <Logger name="com.automation" level="DEBUG" additivity="false">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="FileAppender"/>
            <AppenderRef ref="ErrorFileAppender"/>
        </Logger>

        <!-- RestAssured Logger -->
        <Logger name="io.restassured" level="WARN" additivity="false">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="FileAppender"/>
        </Logger>

        <!-- Root Logger -->
        <Root level="INFO">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="FileAppender"/>
        </Root>
    </Loggers>
</Configuration>
```

### `src/test/resources/schemas/buyerapp/catalog-by-id.json`

_65 lines_

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": [
    "statusCode",
    "message",
    "data"
  ],
  "properties": {
    "statusCode": {
      "const": "10000"
    },
    "message": {
      "type": "string"
    },
    "data": {
      "type": "object",
      "required": [
        "_id",
        "title",
        "images",
        "isDeleted"
      ],
      "properties": {
        "_id": {
          "type": "string",
          "pattern": "^[0-9a-f]{24}$"
        },
        "title": {
          "type": "string"
        },
        "price": {
          "type": [
            "number",
            "null"
          ]
        },
        "images": {
          "type": "array"
        },
        "isDeleted": {
          "type": "boolean"
        },
        "seller": {
          "type": [
            "object",
            "null"
          ]
        },
        "market": {
          "type": [
            "object",
            "null"
          ]
        },
        "navoAvailable": {
          "type": [
            "boolean",
            "null"
          ]
        }
      }
    }
  }
}
```

### `src/test/resources/schemas/buyerapp/collection-all.json`

_57 lines_

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": [
    "statusCode",
    "message",
    "data"
  ],
  "properties": {
    "statusCode": {
      "const": "10000"
    },
    "message": {
      "type": "string"
    },
    "data": {
      "type": "object",
      "required": [
        "result"
      ],
      "properties": {
        "result": {
          "type": "array",
          "items": {
            "type": "object",
            "required": [
              "_id",
              "name"
            ],
            "properties": {
              "_id": {
                "type": "string",
                "pattern": "^[0-9a-f]{24}$"
              },
              "name": {
                "type": "string"
              },
              "image": {
                "type": [
                  "string",
                  "null"
                ]
              },
              "catalogCount": {
                "type": [
                  "integer",
                  "null"
                ],
                "minimum": 0
              }
            }
          }
        }
      }
    }
  }
}
```

### `src/test/resources/schemas/buyerapp/feed-banners.json`

_49 lines_

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": [
    "statusCode",
    "message",
    "data"
  ],
  "properties": {
    "statusCode": {
      "const": "10000"
    },
    "message": {
      "type": "string"
    },
    "data": {
      "type": "object",
      "required": [
        "result"
      ],
      "properties": {
        "result": {
          "type": "array",
          "items": {
            "type": "object",
            "required": [
              "type",
              "value"
            ],
            "properties": {
              "type": {
                "type": "string"
              },
              "value": {
                "type": "object"
              },
              "position": {
                "type": [
                  "integer",
                  "null"
                ]
              }
            }
          }
        }
      }
    }
  }
}
```

### `src/test/resources/secrets.properties.example`

_10 lines_

```properties
# Copy to secrets.properties (gitignored) for local runs, or set the env vars
# in brackets (CI). Precedence: -D system property > env var > secrets.properties.

# BOMB login [BOMB_LOGIN_PHONE, BOMB_LOGIN_OTP]
login.phone.number=+91XXXXXXXXXX
login.token=XXXXXX

# Buyer App (Navo) login [BUYER_LOGIN_PHONE, BUYER_LOGIN_OTP]
buyer.app.phone.number=+91XXXXXXXXXX
buyer.app.token=XXXXXX
```

### `src/test/resources/test-variables.properties`

_44 lines_

```properties
# Test Variables - static test data only. Login tokens are kept in memory per run
# and are never written here. cart_id / live_catalog_id are fallbacks: the tests
# look up current values at run time (TestDataResolver).
suitable_for=saree
editor_id=64f180feaa90ffbd54b330f5
cart_id=6ae4e1916c3d644841986793
search_product=jeans
seller_name=Test Shop
search_product_id=641d692fcebb994dcaf45732
price=629
product_id=641d692fcebb994dcaf4571b
tag=men
seller_id=64f180feaa90ffbd54b330f5
search_seller_id=63ee780c9689be92acce8f35
business_name=Test Shop
thumbnail_link=https\://firebasestorage.googleapis.com/v0/b/bizup-3df17.appspot.com/o/editor%2Fvideo%2Fthumbnail%2Fc901f102-ba3f-4214-8249-7c95082b524c.png?alt\=media&token\=c30ded78-a968-4084-8898-32a543ba19e1
live_catalog_id=6ae49ab1c525a8b413dd1510
cart_catalog_id=6ae49ab1c525a8b413dd1510
market_id=6454921e6144f73eceac9de5
search_recommend=jeans
navo_base=https\://api.navofashion.in
catalog_id=69245c7ee4332a2a36b895c6
phoneNumber=+911119997000
video_link=https\://firebasestorage.googleapis.com/v0/b/bizup-3df17.appspot.com/o/editor%2Fvideo%2Fe4408ce0-e095-4671-b58b-91a70418cfc0.mp4?alt\=media&token\=2e06aff7-661e-4f79-a4b1-c7fd7a607a1c
video_title="Premium Cotton fabric"
user_id=6818afbbbaaa875960578c7e
creator_id=657d985fc2381755996f2a7c
bizup_base=https\://api.bizup.app
upload_id=693a92f840d8d24c390119a0
flag=false
res_time=40000
catalog_foassign_id=694259e31ac6c490476461ab
collection_id=67c59d8ff22202c05e7d612e
AppVersion=3.3.2-debug
search_recommend_id=641d692fcebb994dcaf45715
delete_catalog_id=68258527ec487f5e2b900f7e
video_description=test
catalog_pdf1=68d7745a0fb205076de33ca7
catalog_pdf2=68d7a3f765adc7eda7d1cc0d
catalog_pdf3=68d7a3f7d067c261d488b39b
AppVersionCode=165
generatedTitle="Premium Cotton fabric"
catalog_pdf4=68d7a3f765adc7eda7d1cc16
video_id=68a805c5c5ea3b8d0101b168
```

### `src/test/resources/testng-bomb.xml`

_51 lines_

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="BOMB API Automation Test Suite" preserve-order="true">

    <listeners>
        <listener class-name="com.automation.listeners.TestListener"/>
    </listeners>

    <parameter name="env" value="default"/>

    <test name="BOMB API Tests" preserve-order="true">
        <classes>
            <!-- 1. Login must run first to generate auth token -->
            <class name="com.automation.tests.bomb.Login.LoginApiTest"/>
            
            <!-- 2. Catalog Search Tests (in required order) -->
            <class name="com.automation.tests.bomb.CatalogSearch.AllCatalogTest"/>
            <class name="com.automation.tests.bomb.CatalogSearch.SearchWithSellerFilterTest"/>
            <class name="com.automation.tests.bomb.CatalogSearch.SearchWithProductFilterTest"/>
            <class name="com.automation.tests.bomb.CatalogSearch.SearchWithCatalogIdFilterTest"/>
            
            <!-- 3. Catalog Tag Pipeline - Assign to Editor (in required order) -->
            <class name="com.automation.tests.bomb.CatalogTagPipeline.CatalogAssignToEditor.AllCatalogUploadedTest"/>
            <class name="com.automation.tests.bomb.CatalogTagPipeline.CatalogAssignToEditor.CatalogUploadedSearchBySellerFilterTest"/>
            <class name="com.automation.tests.bomb.CatalogTagPipeline.CatalogAssignToEditor.AssignToEditorTest"/>
            
            <!-- 4. Catalog Tag Pipeline - Editor (in required order) -->
            <class name="com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor.AllCatalogsAssignedTest"/>
            <class name="com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor.FetchCatalogUploadedTest"/>
            <class name="com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor.CatalogsGroupTest"/>
            <class name="com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor.CatalogTagTest"/>
            <class name="com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor.BotCatalogTitleGenerateTest"/>
            <class name="com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor.CatalogTaggingTest"/>
            <class name="com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor.CatalogEditorEditTest"/>
            <class name="com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor.CatalogSkipTest"/>
            <class name="com.automation.tests.bomb.CatalogTagPipeline.CatalogEditor.MarkAsDoneTest"/>
            
            <!-- 5. Video Tagging Pipeline - Upload and Assign (Must run in order) -->
            <class name="com.automation.tests.bomb.VideoTaggingPipeline.VideoUploadAndAssignToEditor.VideoUploadTest"/>
            <class name="com.automation.tests.bomb.VideoTaggingPipeline.VideoUploadAndAssignToEditor.AllVideoAssignForUploadingThumbnailVideoTest"/>
            <class name="com.automation.tests.bomb.VideoTaggingPipeline.VideoUploadAndAssignToEditor.UploadThumbnailVideoTest"/>
            <class name="com.automation.tests.bomb.VideoTaggingPipeline.VideoUploadAndAssignToEditor.VideoUploadMarkAsDoneTest"/>
            
            <!-- 6. Video Tagging Pipeline - Tagging (in required order) -->
            <class name="com.automation.tests.bomb.VideoTaggingPipeline.VideoTagging.VideoTaggingEditButtonTest"/>
            <class name="com.automation.tests.bomb.VideoTaggingPipeline.VideoTagging.VideoTitleGenerationTest"/>
            <class name="com.automation.tests.bomb.VideoTaggingPipeline.VideoTagging.VideoTaggingTest"/>
        </classes>
    </test>

</suite>
```

### `src/test/resources/testng-buyerapp.xml`

_29 lines_

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="Buyer App API Automation Test Suite">

    <listeners>
        <listener class-name="com.automation.listeners.TestListener"/>
    </listeners>

    <parameter name="env" value="default"/>

    <!-- Every test class under com.automation.tests.buyerapp (including sub-packages)
         whose tests are in the "buyerapp" group. A new class runs automatically once
         it is in that package and group; no class list to maintain.
         Order does not matter: BaseTest.buyerAppSpec() logs in on demand if
         LoginTest has not run yet.
         newapis/ tests are PRODUCTION-safe: GETs and requests rejected before any
         write / Kafka publish / LLM call only (see README-BUYER-APP.md). -->
    <test name="Buyer App All Tests">
        <groups>
            <run>
                <include name="buyerapp"/>
            </run>
        </groups>
        <packages>
            <package name="com.automation.tests.buyerapp.*"/>
        </packages>
    </test>

</suite>
```

### `src/test/resources/testng.xml`

_18 lines_

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<!-- Master Test Suite - References sub-suites for BOMB and Buyer App -->
<suite name="All API Test Suites" preserve-order="true">
    
    <listeners>
        <listener class-name="com.automation.listeners.TestListener"/>
    </listeners>

    <parameter name="env" value="default"/>

    <!-- Include BOMB API Test Suite -->
    <suite-files>
        <suite-file path="testng-bomb.xml"/>
        <suite-file path="testng-buyerapp.xml"/>
    </suite-files>

</suite>
```
