package com.amalitech.qa.requests;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.testdata.AuthTestData;
import com.amalitech.qa.testdata.ServiceRequestTestData;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ServiceRequestApiTest extends BaseTest {

    private String authToken;
    private String createdRequestId;

    @BeforeAll
    public void authenticate() {
        authToken = given()
                .contentType(ContentType.JSON)
                .body(AuthTestData.VALID_LOGIN_BODY)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract().path("token");
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
        createdRequestId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(ServiceRequestTestData.VALID_CREATE_BODY)
                .when()
                .post("/api/service-requests")
                .then()
                .statusCode(201)
                .body("title", equalTo(ServiceRequestTestData.VALID_TITLE))
                .body("category", equalTo(ServiceRequestTestData.VALID_CATEGORY))
                .body("priority", equalTo(ServiceRequestTestData.VALID_PRIORITY))
                .body("status", equalTo("OPEN"))
                .body("id", notNullValue())
                .extract().path("id");
    }

    // Testing to confirm a service request can be retrieved by ID
    @Test
    public void testGetServiceRequestByIdSuccessful() {
        // First create a request to get a valid ID
        String requestId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(ServiceRequestTestData.VALID_CREATE_BODY)
                .when()
                .post("/api/service-requests")
                .then()
                .statusCode(201)
                .extract().path("id");

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
        String requestId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(ServiceRequestTestData.VALID_CREATE_BODY)
                .when()
                .post("/api/service-requests")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Then update it
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(ServiceRequestTestData.VALID_UPDATE_BODY)
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
        String requestId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(ServiceRequestTestData.VALID_CREATE_BODY)
                .when()
                .post("/api/service-requests")
                .then()
                .statusCode(201)
                .extract().path("id");

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
                .body(ServiceRequestTestData.MISSING_TITLE_BODY)
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
                .body(ServiceRequestTestData.MISSING_CATEGORY_BODY)
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
                .body(ServiceRequestTestData.MISSING_PRIORITY_BODY)
                .when()
                .post("/api/service-requests")
                .then()
                .statusCode(400);
    }

    //     Testing to confirm create fails with invalid category
    @Test
    public void testCreateServiceRequestInvalidCategory() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(ServiceRequestTestData.INVALID_CATEGORY_BODY)
                .when()
                .post("/api/service-requests")
                .then()
                .statusCode(400);
    }

    // Testing to confirm get by invalid ID returns 404
    @Test
    public void testGetServiceRequestInvalidId() {
        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/service-requests/" + ServiceRequestTestData.INVALID_ID)
                .then()
                .statusCode(404);
    }

    // Testing to confirm delete with invalid ID returns 404
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
