package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.constants.Endpoints;
import com.automation.constants.HttpStatus;
import com.automation.listeners.RetryAnalyzer;
import com.automation.models.request.CreateUserRequest;
import com.automation.models.response.UserResponse;
import com.automation.utils.DataGenerator;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for User API endpoints.
 * Demonstrates various API testing scenarios using RestAssured and Hamcrest.
 */
@Epic("User Management")
@Feature("User API")
public class UserApiTest extends BaseTest {

    @Test(description = "Verify GET all users returns list of users")
    @Story("Get Users")
    @Severity(SeverityLevel.CRITICAL)
    public void testGetAllUsers() {
        Response response = restClient.get(Endpoints.USERS);

        // Validate status code
        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        // Validate response is a non-empty list
        List<UserResponse> users = JsonUtils.fromResponseToList(response, UserResponse.class);
        assertThat("Users list should not be empty", users, is(not(empty())));
        assertThat("Users list should have 10 users", users, hasSize(10));

        // Validate first user has required fields
        UserResponse firstUser = users.get(0);
        assertThat("User should have an ID", firstUser.getId(), notNullValue());
        assertThat("User should have a name", firstUser.getName(), not(emptyString()));
        assertThat("User should have an email", firstUser.getEmail(), containsString("@"));
    }

    @Test(description = "Verify GET user by ID returns correct user")
    @Story("Get Users")
    @Severity(SeverityLevel.CRITICAL)
    public void testGetUserById() {
        int userId = 1;

        Response response = restClient.get(Endpoints.USER_BY_ID, Map.of("id", userId));

        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        UserResponse user = JsonUtils.fromResponse(response, UserResponse.class);
        assertThat("User ID should match", user.getId(), equalTo(userId));
        assertThat("User should have name", user.getName(), not(emptyOrNullString()));
        assertThat("User should have email", user.getEmail(), not(emptyOrNullString()));
        assertThat("User should have address", user.getAddress(), notNullValue());
        assertThat("User should have company", user.getCompany(), notNullValue());
    }

    @Test(description = "Verify GET user with invalid ID returns 404")
    @Story("Get Users")
    @Severity(SeverityLevel.NORMAL)
    public void testGetUserByInvalidId() {
        int invalidUserId = 99999;

        Response response = restClient.get(Endpoints.USER_BY_ID, Map.of("id", invalidUserId));

        assertThat("Status code should be 404",
                response.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

    @Test(description = "Verify POST creates a new user")
    @Story("Create User")
    @Severity(SeverityLevel.CRITICAL)
    public void testCreateUser() {
        CreateUserRequest request = CreateUserRequest.builder()
                .name("Test User " + DataGenerator.generateRandomString(5))
                .username("testuser_" + DataGenerator.generateTimestamp())
                .email(DataGenerator.generateRandomEmail())
                .phone(DataGenerator.generateRandomPhone())
                .website("https://test.com")
                .address(CreateUserRequest.Address.builder()
                        .street("123 Test Street")
                        .suite("Suite 100")
                        .city("Test City")
                        .zipcode("12345")
                        .build())
                .company(CreateUserRequest.Company.builder()
                        .name("Test Company")
                        .catchPhrase("Testing is fun")
                        .bs("test automation")
                        .build())
                .build();

        Response response = restClient.post(Endpoints.USERS, request);

        assertThat("Status code should be 201",
                response.getStatusCode(), equalTo(HttpStatus.CREATED));

        UserResponse createdUser = JsonUtils.fromResponse(response, UserResponse.class);
        assertThat("Created user should have an ID", createdUser.getId(), notNullValue());
        assertThat("Name should match", createdUser.getName(), equalTo(request.getName()));
        assertThat("Email should match", createdUser.getEmail(), equalTo(request.getEmail()));
    }

    @Test(description = "Verify PUT updates an existing user")
    @Story("Update User")
    @Severity(SeverityLevel.CRITICAL)
    public void testUpdateUser() {
        int userId = 1;
        String updatedName = "Updated User " + DataGenerator.generateTimestamp();

        CreateUserRequest updateRequest = CreateUserRequest.builder()
                .name(updatedName)
                .username("updateduser")
                .email("updated@test.com")
                .build();

        Response response = restClient.put(Endpoints.USER_BY_ID, Map.of("id", userId), updateRequest);

        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        UserResponse updatedUser = JsonUtils.fromResponse(response, UserResponse.class);
        assertThat("User ID should remain same", updatedUser.getId(), equalTo(userId));
        assertThat("Name should be updated", updatedUser.getName(), equalTo(updatedName));
    }

    @Test(description = "Verify PATCH partially updates a user")
    @Story("Update User")
    @Severity(SeverityLevel.NORMAL)
    public void testPatchUser() {
        int userId = 1;
        String patchedName = "Patched Name " + DataGenerator.generateTimestamp();

        Response response = restClient.patch(
                Endpoints.USER_BY_ID,
                Map.of("id", userId),
                Map.of("name", patchedName));

        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        UserResponse patchedUser = JsonUtils.fromResponse(response, UserResponse.class);
        assertThat("Name should be patched", patchedUser.getName(), equalTo(patchedName));
    }

    @Test(description = "Verify DELETE removes a user")
    @Story("Delete User")
    @Severity(SeverityLevel.CRITICAL)
    public void testDeleteUser() {
        int userId = 1;

        Response response = restClient.delete(Endpoints.USER_BY_ID, Map.of("id", userId));

        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    @Test(description = "Verify response time is within acceptable limits", retryAnalyzer = RetryAnalyzer.class)
    @Story("Performance")
    @Severity(SeverityLevel.MINOR)
    public void testResponseTime() {
        Response response = restClient.get(Endpoints.USERS);

        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat("Response time should be less than 3 seconds",
                response.getTime(), lessThan(3000L));
    }
}
