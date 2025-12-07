package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.constants.Endpoints;
import com.automation.constants.HttpStatus;
import com.automation.models.request.CreatePostRequest;
import com.automation.models.response.PostResponse;
import com.automation.utils.DataGenerator;
import com.automation.utils.JsonUtils;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class for Post API endpoints.
 * Demonstrates data-driven testing and various assertion patterns.
 */
@Epic("Content Management")
@Feature("Post API")
public class PostApiTest extends BaseTest {

    @Test(description = "Verify GET all posts returns list of posts")
    @Story("Get Posts")
    @Severity(SeverityLevel.CRITICAL)
    public void testGetAllPosts() {
        Response response = restClient.get(Endpoints.POSTS);

        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        List<PostResponse> posts = JsonUtils.fromResponseToList(response, PostResponse.class);
        assertThat("Posts list should not be empty", posts, is(not(empty())));
        assertThat("Posts list should have 100 posts", posts, hasSize(100));
    }

    @Test(description = "Verify GET post by ID returns correct post")
    @Story("Get Posts")
    @Severity(SeverityLevel.CRITICAL)
    public void testGetPostById() {
        int postId = 1;

        Response response = restClient.get(Endpoints.POST_BY_ID, Map.of("id", postId));

        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        PostResponse post = JsonUtils.fromResponse(response, PostResponse.class);
        assertThat("Post ID should match", post.getId(), equalTo(postId));
        assertThat("Post should have userId", post.getUserId(), notNullValue());
        assertThat("Post should have title", post.getTitle(), not(emptyOrNullString()));
        assertThat("Post should have body", post.getBody(), not(emptyOrNullString()));
    }

    @DataProvider(name = "postIds")
    public Object[][] postIdProvider() {
        return new Object[][] {
                { 1 }, { 2 }, { 3 }, { 5 }, { 10 }
        };
    }

    @Test(dataProvider = "postIds", description = "Verify GET post by various IDs - Data Driven")
    @Story("Get Posts")
    @Severity(SeverityLevel.NORMAL)
    public void testGetPostByIdDataDriven(int postId) {
        Response response = restClient.get(Endpoints.POST_BY_ID, Map.of("id", postId));

        assertThat("Status code should be 200 for post ID: " + postId,
                response.getStatusCode(), equalTo(HttpStatus.OK));

        PostResponse post = JsonUtils.fromResponse(response, PostResponse.class);
        assertThat("Post ID should match", post.getId(), equalTo(postId));
    }

    @Test(description = "Verify GET posts filtered by userId")
    @Story("Filter Posts")
    @Severity(SeverityLevel.NORMAL)
    public void testGetPostsByUserId() {
        int userId = 1;

        Response response = restClient.getWithQueryParams(
                Endpoints.POSTS, Map.of("userId", userId));

        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        List<PostResponse> posts = JsonUtils.fromResponseToList(response, PostResponse.class);
        assertThat("Posts list should not be empty", posts, is(not(empty())));

        // Verify all posts belong to the specified user
        posts.forEach(post -> assertThat("Post should belong to user " + userId,
                post.getUserId(), equalTo(userId)));
    }

    @Test(description = "Verify POST creates a new post")
    @Story("Create Post")
    @Severity(SeverityLevel.CRITICAL)
    public void testCreatePost() {
        CreatePostRequest request = CreatePostRequest.builder()
                .userId(1)
                .title("Test Post " + DataGenerator.generateTimestamp())
                .body("This is a test post body created during API automation testing. " +
                        DataGenerator.generateRandomString(50))
                .build();

        Response response = restClient.post(Endpoints.POSTS, request);

        assertThat("Status code should be 201",
                response.getStatusCode(), equalTo(HttpStatus.CREATED));

        PostResponse createdPost = JsonUtils.fromResponse(response, PostResponse.class);
        assertThat("Created post should have an ID", createdPost.getId(), notNullValue());
        assertThat("UserId should match", createdPost.getUserId(), equalTo(request.getUserId()));
        assertThat("Title should match", createdPost.getTitle(), equalTo(request.getTitle()));
        assertThat("Body should match", createdPost.getBody(), equalTo(request.getBody()));
    }

    @Test(description = "Verify PUT updates an existing post")
    @Story("Update Post")
    @Severity(SeverityLevel.CRITICAL)
    public void testUpdatePost() {
        int postId = 1;
        String updatedTitle = "Updated Post Title " + DataGenerator.generateTimestamp();

        CreatePostRequest updateRequest = CreatePostRequest.builder()
                .userId(1)
                .title(updatedTitle)
                .body("Updated body content")
                .build();

        Response response = restClient.put(
                Endpoints.POST_BY_ID, Map.of("id", postId), updateRequest);

        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        PostResponse updatedPost = JsonUtils.fromResponse(response, PostResponse.class);
        assertThat("Post ID should remain same", updatedPost.getId(), equalTo(postId));
        assertThat("Title should be updated", updatedPost.getTitle(), equalTo(updatedTitle));
    }

    @Test(description = "Verify DELETE removes a post")
    @Story("Delete Post")
    @Severity(SeverityLevel.CRITICAL)
    public void testDeletePost() {
        int postId = 1;

        Response response = restClient.delete(Endpoints.POST_BY_ID, Map.of("id", postId));

        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    @Test(description = "Verify GET comments for a specific post")
    @Story("Get Comments")
    @Severity(SeverityLevel.NORMAL)
    public void testGetCommentsForPost() {
        int postId = 1;

        Response response = restClient.get(
                Endpoints.COMMENTS_BY_POST, Map.of("postId", postId));

        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        // Verify response is a list (comments)
        List<?> comments = response.jsonPath().getList("$");
        assertThat("Comments list should not be empty", comments, is(not(empty())));
    }

    @Test(description = "Verify post response structure using JSON path")
    @Story("Response Validation")
    @Severity(SeverityLevel.NORMAL)
    public void testPostResponseStructure() {
        Response response = restClient.get(Endpoints.POST_BY_ID, Map.of("id", 1));

        assertThat("Status code should be 200",
                response.getStatusCode(), equalTo(HttpStatus.OK));

        // Validate using JSON Path
        assertThat("Should have 'id' field",
                response.jsonPath().get("id"), notNullValue());
        assertThat("Should have 'userId' field",
                response.jsonPath().get("userId"), notNullValue());
        assertThat("Should have 'title' field",
                response.jsonPath().get("title"), notNullValue());
        assertThat("Should have 'body' field",
                response.jsonPath().get("body"), notNullValue());
    }
}
