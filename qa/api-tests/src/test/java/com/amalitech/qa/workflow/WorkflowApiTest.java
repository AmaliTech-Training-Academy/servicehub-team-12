package com.amalitech.qa.workflow;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.testdata.WorkflowTestData;
import com.amalitech.qa.utils.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WorkflowApiTest extends BaseTest {

    private String authToken;
    private String requesterId;

    /**
     * Authenticates as admin and retrieves the requesterId dynamically
     * from the login response before all tests run.
     */
    @BeforeAll
    public void authenticate() {
        authToken = TestHelper.getAuthToken();
        requesterId = TestHelper.getRequesterId();
    }

    /**
     * Testing OPEN → ASSIGNED transition.
     * A newly created request starts in OPEN status.
     * One transition should move it to ASSIGNED.
     * Expected: 200 OK
     */
    @Test
    public void testTransitionFromOpenToAssigned() {
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/api/workflow/requests/" + requestId + "/transition")
                .then()
                .statusCode(200);
    }

    /**
     * Testing ASSIGNED → IN_PROGRESS transition.
     * Transitions the request once to ASSIGNED, then tests
     * the next transition moves it to IN_PROGRESS.
     * Expected: 200 OK
     */
    @Test
    public void testTransitionFromAssignedToInProgress() {
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);
        TestHelper.transitionTimes(authToken, requestId, 1); // OPEN → ASSIGNED

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/api/workflow/requests/" + requestId + "/transition")
                .then()
                .statusCode(200);
    }

    /**
     * Testing IN_PROGRESS → RESOLVED transition.
     * Transitions the request twice to IN_PROGRESS, then tests
     * the next transition moves it to RESOLVED.
     * Expected: 200 OK
     */
    @Test
    public void testTransitionFromInProgressToResolved() {
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);
        TestHelper.transitionTimes(authToken, requestId, 2); // OPEN → ASSIGNED → IN_PROGRESS

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/api/workflow/requests/" + requestId + "/transition")
                .then()
                .statusCode(200);
    }

    /**
     * Testing RESOLVED → CLOSED transition.
     * Transitions the request three times to RESOLVED, then tests
     * the next transition moves it to CLOSED.
     * Expected: 200 OK
     */
    @Test
    public void testTransitionFromResolvedToClosed() {
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);
        TestHelper.transitionTimes(authToken, requestId, 3); // OPEN → ASSIGNED → IN_PROGRESS → RESOLVED

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/api/workflow/requests/" + requestId + "/transition")
                .then()
                .statusCode(200);
    }

    /**
     * Testing full transition flow from OPEN to CLOSED.
     * Transitions the request four times through all statuses,
     * then verifies the final status is CLOSED.
     * Flow: OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED
     * Expected: 200 OK and status equals CLOSED
     */
    @Test
    public void testFullTransitionFlow() {
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);
        TestHelper.transitionTimes(authToken, requestId, 4); // OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/service-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("status", equalTo("CLOSED"));
    }

    /**
     * Testing transition fails with a non-existent request ID.
     * Uses a zeroed-out UUID that is guaranteed not to exist in the database.
     * Expected: 404 Not Found — BUG-005 (currently returns 500)
     */
    @Test
    public void testTransitionInvalidRequestId() {
        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/api/workflow/requests/" + WorkflowTestData.INVALID_REQUEST_ID + "/transition")
                .then()
                .statusCode(404);
    }

    /**
     * Testing transition fails when no auth token is provided.
     * The endpoint should be protected and reject unauthenticated requests.
     * Expected: 401 Unauthorized
     */
    @Test
    public void testTransitionWithoutToken() {
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);

        given()
                .when()
                .post("/api/workflow/requests/" + requestId + "/transition")
                .then()
                .statusCode(401);
    }

    /**
     * Testing transition fails when the request is already CLOSED.
     * CLOSED is the final status — no further transitions are allowed.
     * Expected: 400 Bad Request — BUG-007 (currently returns 500)
     */
    @Test
    public void testTransitionFromClosedFails() {
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);
        TestHelper.transitionTimes(authToken, requestId, 4); // OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/api/workflow/requests/" + requestId + "/transition")
                .then()
                .statusCode(400);
    }
}