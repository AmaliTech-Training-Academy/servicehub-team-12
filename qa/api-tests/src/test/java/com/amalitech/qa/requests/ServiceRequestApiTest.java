package com.amalitech.qa.requests;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.testdata.ServiceRequestTestData;
import com.amalitech.qa.utils.TestHelper;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ServiceRequestApiTest extends BaseTest {

    private String authToken;
    private String requesterId;

    /**
     * Authenticates as admin and retrieves the requesterId dynamically
     * from the login response before all tests run.
     * Requires id field to be present in AuthResponse.
     */
    @BeforeAll
    public void authenticate() {
        authToken = TestHelper.getAuthToken();
        requesterId = TestHelper.getRequesterId();
    }

    // Testing to confirm unauthorized access returns 401
    @Test
    public void testUnauthorizedAccess() {
        given()
                .when()
                .get("/api/service-requests")
                .then()
                .statusCode(401);
    }

    // Testing to confirm all service requests can be retrieved
    @Test
    public void testGetAllRequestsSuccessful() {
        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/service-requests")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    // Testing to confirm a service request can be created with valid data
    @Test
    public void testCreateServiceRequestSuccessful() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(ServiceRequestTestData.validCreateBody(requesterId))
                .when()
                .post("/api/service-requests")
                .then()
                .statusCode(201)
                .body("title", equalTo(ServiceRequestTestData.VALID_TITLE))
                .body("category", equalTo(ServiceRequestTestData.VALID_CATEGORY))
                .body("description", equalTo(ServiceRequestTestData.VALID_DESCRIPTION))
                .body("priority", equalTo(ServiceRequestTestData.VALID_PRIORITY))
                .body("status", equalTo("OPEN"))
                .body("id", notNullValue());
    }

    // Testing to confirm a service request can be retrieved by ID
    @Test
    public void testGetServiceRequestByIdSuccessful() {
        // First create a request to get a valid ID
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);

        // Then retrieve it by ID
        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/service-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("id", equalTo(requestId))
                .body("title", equalTo(ServiceRequestTestData.VALID_TITLE));
    }

    // Testing to confirm a service request can be updated
    @Test
    public void testUpdateServiceRequestSuccessful() {
        // First create a request
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);

        // Then update it
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(ServiceRequestTestData.validUpdateBody(requesterId))
                .when()
                .put("/api/service-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("title", equalTo("Laptop fixed"));
    }

    // Testing to confirm a service request can be deleted
    @Test
    public void testDeleteServiceRequestSuccessful() {
        // First create a request
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);

        // Then delete it
        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .delete("/api/service-requests/" + requestId)
                .then()
                .statusCode(204);
    }

    // Testing to confirm create fails when title is missing
    @Test
    public void testCreateServiceRequestMissingTitle() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(ServiceRequestTestData.missingTitleBody(requesterId))
                .when()
                .post("/api/service-requests")
                .then()
                .statusCode(400);
    }

    // Testing to confirm create fails when category is missing
    @Test
    public void testCreateServiceRequestMissingCategory() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(ServiceRequestTestData.missingCategoryBody(requesterId))
                .when()
                .post("/api/service-requests")
                .then()
                .statusCode(400);
    }

    // Testing to confirm create fails when priority is missing
    @Test
    public void testCreateServiceRequestMissingPriority() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(ServiceRequestTestData.missingPriorityBody(requesterId))
                .when()
                .post("/api/service-requests")
                .then()
                .statusCode(400);
    }

    // Testing to confirm create fails when requesterId is missing
    @Test
    public void testCreateServiceRequestMissingRequesterId() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(ServiceRequestTestData.MISSING_REQUESTER_ID_BODY)
                .when()
                .post("/api/service-requests")
                .then()
                .statusCode(400);
    }

    // Testing to confirm create fails with invalid category — BUG-004
    @Test
    public void testCreateServiceRequestInvalidCategory() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(ServiceRequestTestData.invalidCategoryBody(requesterId))
                .when()
                .post("/api/service-requests")
                .then()
                .statusCode(400);
    }

    // Testing to confirm get by invalid ID returns 404 — BUG-005
    @Test
    public void testGetServiceRequestInvalidId() {
        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/service-requests/" + ServiceRequestTestData.INVALID_ID)
                .then()
                .statusCode(404);
    }

    // Testing to confirm delete with invalid ID returns 404 — BUG-006
    @Test
    public void testDeleteServiceRequestInvalidId() {
        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .delete("/api/service-requests/" + ServiceRequestTestData.INVALID_ID)
                .then()
                .statusCode(404);
    }
}