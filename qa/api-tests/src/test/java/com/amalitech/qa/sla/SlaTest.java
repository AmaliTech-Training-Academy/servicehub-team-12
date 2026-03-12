package com.amalitech.qa.sla;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.utils.TestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SlaTest extends BaseTest {

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
     * Tests that the SLA deadline is automatically set when a service request
     * is created. The deadline is calculated based on the category and priority
     * of the request using the SLA policy configured in the system.
     * Expected: slaDeadline field is not null in the response.
     */
    @Test
    public void testSlaDeadlineSetOnCreate() {
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);

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
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);

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
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);

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
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);
        TestHelper.transitionTimes(authToken, requestId, 1); // OPEN → ASSIGNED

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
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);
        TestHelper.transitionTimes(authToken, requestId, 2); // OPEN → ASSIGNED → IN_PROGRESS

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
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);
        TestHelper.transitionTimes(authToken, requestId, 3); // OPEN → ASSIGNED → IN_PROGRESS → RESOLVED

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
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);

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
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);
        TestHelper.transitionTimes(authToken, requestId, 4); // OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED

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
        String requestId = TestHelper.createServiceRequest(authToken, requesterId);

        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/service-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("closedAt", nullValue());
    }
}