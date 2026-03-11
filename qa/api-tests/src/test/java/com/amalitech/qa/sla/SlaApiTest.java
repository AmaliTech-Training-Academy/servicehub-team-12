package com.amalitech.qa.sla;

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
public class SlaApiTest extends BaseTest {

    private String authToken;

    /**
     * Authenticates as admin before all tests run.
     * Extracts and stores the JWT token for use in all test methods.
     */
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

    /**
     * Helper method to create a new service request.
     * Used by all SLA tests to get a valid request ID to test against.
     * Returns the UUID of the newly created service request.
     */
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

    /**
     * Helper method to transition a service request a given number of times.
     * Each call moves the request one step forward in the status flow:
     * OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED
     *
     * @param requestId the UUID of the service request to transition
     * @param times the number of transitions to perform
     */
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

    /**
     * Tests that the SLA deadline is automatically set when a service request
     * is created. The deadline is calculated based on the category and priority
     * of the request using the SLA policy configured in the system.
     * Expected: slaDeadline field is not null in the response.
     */
    @Test
    public void testSlaDeadlineSetOnCreate() {
        String requestId = createServiceRequest();

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/service-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("slaDeadline", notNullValue());
    }

    /**
     * Tests that isSlaBreached is false immediately after a service request
     * is created. A newly created request should never be in breach since
     * the SLA deadline is set in the future at creation time.
     * Expected: isSlaBreached is false.
     */
    @Test
    public void testIsSlaBreachedFalseOnCreate() {
        String requestId = createServiceRequest();

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/service-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("isSlaBreached", equalTo(false));
    }

    /**
     * Tests that the isSlaBreached field is always present in the service
     * request response. This field must be included in the response even
     * if its value is false, so clients can always rely on it being there.
     * Expected: isSlaBreached field is not null.
     */
    @Test
    public void testIsSlaBreachedFieldPresent() {
        String requestId = createServiceRequest();

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/service-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("isSlaBreached", notNullValue());
    }

    /**
     * Tests that firstResponseAt timestamp is set when a service request
     * transitions from OPEN to ASSIGNED. This timestamp represents when
     * an agent first responded to the request and is used to calculate
     * the response time SLA metric.
     * Transition: OPEN → ASSIGNED (1 transition)
     * Expected: firstResponseAt is not null after transition.
     */
    @Test
    public void testFirstResponseAtSetOnAssigned() {
        String requestId = createServiceRequest();

        // OPEN → ASSIGNED
        transitionTimes(requestId, 1);

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/service-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("firstResponseAt", notNullValue());
    }

    /**
     * Tests that firstResponseAt timestamp is NOT overwritten on subsequent
     * transitions after ASSIGNED. Once set, firstResponseAt should remain
     * the same value regardless of further status changes.
     * Transition: OPEN → ASSIGNED → IN_PROGRESS (2 transitions)
     * Expected: firstResponseAt is still not null after further transitions.
     */
    @Test
    public void testFirstResponseAtNotOverwrittenAfterAssigned() {
        String requestId = createServiceRequest();

        // OPEN → ASSIGNED → IN_PROGRESS
        transitionTimes(requestId, 2);

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/service-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("firstResponseAt", notNullValue());
    }

    /**
     * Tests that resolvedAt timestamp is set when a service request
     * transitions to RESOLVED status. This timestamp is used to calculate
     * the resolution time SLA metric.
     * Transition: OPEN → ASSIGNED → IN_PROGRESS → RESOLVED (3 transitions)
     * Expected: resolvedAt is not null after transition to RESOLVED.
     */
    @Test
    public void testResolvedAtSetOnResolved() {
        String requestId = createServiceRequest();

        // OPEN → ASSIGNED → IN_PROGRESS → RESOLVED
        transitionTimes(requestId, 3);

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/service-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("resolvedAt", notNullValue());
    }

    /**
     * Tests that resolvedAt timestamp is null before a service request
     * reaches RESOLVED status. The field should only be populated once
     * the request has been resolved.
     * Expected: resolvedAt is null for a newly created request.
     */
    @Test
    public void testResolvedAtNullBeforeResolved() {
        String requestId = createServiceRequest();

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/service-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("resolvedAt", nullValue());
    }

    /**
     * Tests that closedAt timestamp is set when a service request
     * transitions to CLOSED status. This is the final timestamp in
     * the request lifecycle.
     * Transition: OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED (4 transitions)
     * Expected: closedAt is not null after transition to CLOSED.
     */
    @Test
    public void testClosedAtSetOnClosed() {
        String requestId = createServiceRequest();

        // OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED
        transitionTimes(requestId, 4);

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/service-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("closedAt", notNullValue());
    }

    /**
     * Tests that closedAt timestamp is null before a service request
     * reaches CLOSED status. The field should only be populated once
     * the request has been closed.
     * Expected: closedAt is null for a newly created request.
     */
    @Test
    public void testClosedAtNullBeforeClosed() {
        String requestId = createServiceRequest();

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/service-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("closedAt", nullValue());
    }
}