package com.amalitech.qa.workflow;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.testdata.AuthTestData;
import com.amalitech.qa.testdata.ServiceRequestTestData;
import com.amalitech.qa.testdata.WorkflowTestData;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WorkflowApiTest extends BaseTest {

    private String authToken;

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

    private String createServiceRequest() {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(ServiceRequestTestData.VALID_CREATE_BODY)
                .when()
                .post("/api/service-requests")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private void transitionTimes(String requestId, int times) {
        for (int i = 0; i < times; i++) {
            given()
                    .header("Authorization", "Bearer " + authToken)
                    .when()
                    .post("/api/workflow/requests/" + requestId + "/transition")
                    .then()
                    .statusCode(200);
        }
    }

    // Testing OPEN → ASSIGNED transition
    @Test
    public void testTransitionFromOpenToAssigned() {
        String requestId = createServiceRequest();

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/api/workflow/requests/" + requestId + "/transition")
                .then()
                .statusCode(200);
    }

    // Testing ASSIGNED → IN_PROGRESS transition
    @Test
    public void testTransitionFromAssignedToInProgress() {
        String requestId = createServiceRequest();
        transitionTimes(requestId, 1); // OPEN → ASSIGNED

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/api/workflow/requests/" + requestId + "/transition")
                .then()
                .statusCode(200);
    }

    // Testing IN_PROGRESS → RESOLVED transition
    @Test
    public void testTransitionFromInProgressToResolved() {
        String requestId = createServiceRequest();
        transitionTimes(requestId, 2); // OPEN → ASSIGNED → IN_PROGRESS

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/api/workflow/requests/" + requestId + "/transition")
                .then()
                .statusCode(200);
    }

    // Testing RESOLVED → CLOSED transition
    @Test
    public void testTransitionFromResolvedToClosed() {
        String requestId = createServiceRequest();
        transitionTimes(requestId, 3); // OPEN → ASSIGNED → IN_PROGRESS → RESOLVED

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/api/workflow/requests/" + requestId + "/transition")
                .then()
                .statusCode(200);
    }

    // Testing full transition flow OPEN → CLOSED
    @Test
    public void testFullTransitionFlow() {
        String requestId = createServiceRequest();
        transitionTimes(requestId, 4); // OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/service-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("status", equalTo("CLOSED"));
    }



    // Testing transition fails with invalid request ID
    @Test
    public void testTransitionInvalidRequestId() {
        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/api/workflow/requests/" + WorkflowTestData.INVALID_REQUEST_ID + "/transition")
                .then()
                .statusCode(404);
    }

    // Testing transition fails without token
    @Test
    public void testTransitionWithoutToken() {
        String requestId = createServiceRequest();

        given()
                .when()
                .post("/api/workflow/requests/" + requestId + "/transition")
                .then()
                .statusCode(401);
    }
}