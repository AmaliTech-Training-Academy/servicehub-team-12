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
     * Creates a new service request and returns its UUID.
     * Used by tests that need a valid request ID to test against.
     */
    public static String createServiceRequest(String authToken) {
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


    /**
     * Registers a new test user and returns their UUID.
     * Used to get a valid requesterId for service request tests.
     */
    public static String getRequesterId(String authToken) {
        String email = "requester_" + System.currentTimeMillis() + "@amalitech.com";

        String registerBody = "{"
                + "\"firstName\": \"Test\","
                + "\"lastName\": \"Requester\","
                + "\"email\": \"" + email + "\","
                + "\"password\": \"Password123!\","
                + "\"confirmPassword\": \"Password123!\","
                + "\"department\": \"IT\""
                + "}";

        return given()
                .contentType(ContentType.JSON)
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract().path("id");
    }
}