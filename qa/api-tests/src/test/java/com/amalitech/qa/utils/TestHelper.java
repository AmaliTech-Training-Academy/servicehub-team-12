package com.amalitech.qa.utils;

import com.amalitech.qa.testdata.AuthTestData;
import com.amalitech.qa.testdata.ServiceRequestTestData;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.*;

public class TestHelper {

    /**
     * Logs in as admin and returns the JWT token.
     * Used by all test classes that require authentication.
     */
    public static String getAuthToken() {
        return given()
                .contentType(ContentType.JSON)
                .body(AuthTestData.VALID_LOGIN_BODY)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract().path("token");
    }

    /**
     * Logs in as admin and returns the user ID dynamically.
     * Extracted directly from the login response — no hardcoding needed.
     * id field was added to AuthResponse by @eugeneanokye99.
     */
    public static String getRequesterId() {
        return given()
                .contentType(ContentType.JSON)
                .body(AuthTestData.VALID_LOGIN_BODY)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract().path("id");
    }

    /**
     * Creates a new service request and returns its UUID.
     * Used by tests that need a valid request ID to test against.
     *
     * @param authToken   the JWT token for authentication
     * @param requesterId the UUID of the user making the request
     */
    public static String createServiceRequest(String authToken, String requesterId) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(ServiceRequestTestData.validCreateBody(requesterId))
                .when()
                .post("/api/service-requests")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    /**
     * Transitions a service request a given number of times.
     * Each call moves the request one step forward in the status flow:
     * OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED
     *
     * @param authToken the JWT token for authentication
     * @param requestId the UUID of the service request to transition
     * @param times     the number of transitions to perform
     */
    public static void transitionTimes(String authToken, String requestId, int times) {
        for (int i = 0; i < times; i++) {
            given()
                    .header("Authorization", "Bearer " + authToken)
                    .when()
                    .post("/api/workflow/requests/" + requestId + "/transition")
                    .then()
                    .statusCode(200);
        }
    }
}