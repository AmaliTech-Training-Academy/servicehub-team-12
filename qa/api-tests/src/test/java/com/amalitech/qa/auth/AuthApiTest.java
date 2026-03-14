package com.amalitech.qa.auth;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.testdata.AuthTestData;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
/**
 * Covers authentication API scenarios.
 */

public class AuthApiTest extends BaseTest {

    private String authToken;
    private String refreshToken;

    // Testing to confirm a new user can register successfully
    @Test
    public void testUserRegistrationSuccessful() {
        given()
                .contentType(ContentType.JSON)
                .body(AuthTestData.VALID_REGISTER_BODY)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("refreshToken", notNullValue())
                .body("email", equalTo(AuthTestData.REGISTER_EMAIL))
                .body("role", notNullValue());
    }

    // Testing to confirm registration fails when email is already in use
    @Test
    public void testUserRegistrationDuplicateEmail() {
        given()
                .contentType(ContentType.JSON)
                .body(AuthTestData.DUPLICATE_EMAIL_BODY)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(409);
    }

    // Testing to confirm registration fails when required fields are missing
    @Test
    public void testUserRegistrationMissingFields() {
        given()
                .contentType(ContentType.JSON)
                .body(AuthTestData.MISSING_FIELDS_BODY)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(400);
    }

    // Testing to confirm registration fails when password is less than 8 characters
    @Test
    public void testUserRegistrationPasswordTooShort() {
        given()
                .contentType(ContentType.JSON)
                .body(AuthTestData.SHORT_PASSWORD_BODY)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(400);
    }

    // Testing to confirm a user can login with valid credentials
    @Test
    public void testUserLoginSuccessful() {
        authToken = given()
                .contentType(ContentType.JSON)
                .body(AuthTestData.VALID_LOGIN_BODY)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("refreshToken", notNullValue())
                .body("email", equalTo(AuthTestData.ADMIN_EMAIL))
                .body("role", equalTo("ADMIN"))
                .extract().path("token");
    }

    // Testing to confirm login fails with invalid credentials
    @Test
    public void testUserLoginInvalidCredentials() {
        given()
                .contentType(ContentType.JSON)
                .body(AuthTestData.INVALID_CREDENTIALS_BODY)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(401);
    }

    // Testing to confirm login fails with invalid email format
    @Test
    public void testUserLoginInvalidEmailFormat() {
        given()
                .contentType(ContentType.JSON)
                .body(AuthTestData.INVALID_EMAIL_FORMAT_BODY)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(400);
    }

    // Testing to confirm login fails when fields are empty
    @Test
    public void testUserLoginEmptyFields() {
        given()
                .contentType(ContentType.JSON)
                .body(AuthTestData.EMPTY_LOGIN_BODY)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(400);
    }

    // Testing to confirm token can be refreshed successfully
    @Test
    public void testRefreshTokenSuccessful() {
        refreshToken = given()
                .contentType(ContentType.JSON)
                .body(AuthTestData.VALID_LOGIN_BODY)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract().path("refreshToken");

        given()
                .contentType(ContentType.JSON)
                .body("{\"refreshToken\": \"" + refreshToken + "\"}")
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("refreshToken", notNullValue());
    }

    // Testing to confirm refresh fails with invalid token
    @Test
    public void testRefreshTokenInvalid() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"refreshToken\": \"" + AuthTestData.INVALID_TOKEN + "\"}")
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(401);
    }

    // Testing to confirm a user can logout successfully
    @Test
    public void testUserLogoutSuccessful() {
        String token = given()
                .contentType(ContentType.JSON)
                .body(AuthTestData.VALID_LOGIN_BODY)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract().path("token");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/api/v1/auth/logout")
                .then()
                .statusCode(204);
    }

    // Testing to confirm logout fails without token
    @Test
    public void testUserLogoutWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v1/auth/logout")
                .then()
                .statusCode(401);
    }

    // Testing to confirm protected endpoints reject requests without token
    @Test
    public void testUnauthorizedAccessWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/service-requests")
                .then()
                .statusCode(401);
    }

    // Testing to confirm protected endpoints reject requests with invalid token
    @Test
    public void testUnauthorizedAccessWithInvalidToken() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + AuthTestData.INVALID_TOKEN)
                .when()
                .get("/api/service-requests")
                .then()
                .statusCode(401);
    }
}
