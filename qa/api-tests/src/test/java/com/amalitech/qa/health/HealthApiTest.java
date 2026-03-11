package com.amalitech.qa.health;

import com.amalitech.qa.base.BaseTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.*;
        import static org.hamcrest.Matchers.*;

public class HealthApiTest extends BaseTest {

    //    Testing to confirm if the health endpoint returns 200 status code

    @Test
    public void testHealthEndpointReturnsHttp200() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/health")
                .then()
                .statusCode(200);
    }

//   Testing to confirm if health endpoint returns UP as status message when status code is 200

    @Test
    public void testHealthEndpointReturnsStatusUP() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }


    //     Testing to confirm if the health endpoint returns OK as the response message when status code is 200

    @Test
    public void testHealthEndpointReturnsMessageOK() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/health")
                .then()
                .statusCode(200)
                .body("message", equalTo("OK"));
    }

    //     Testing to confirm if the health endpoint returns timestamp when status code is 200

    @Test
    public void testHealthEndpointReturnsTimestamp() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/health")
                .then()
                .statusCode(200)
                .body("timestamp", notNullValue());
    }

    //   Testing to confirm if the health endpoint returns all required body fields when status code is 200

    @Test
    public void testHealthEndpointReturnsAllFields() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("message", equalTo("OK"))
                .body("timestamp", notNullValue());
    }

//   Testing to confirm if the health endpoint is still accessible without token

    @Test
    public void testHealthEndpointIsPubliclyAccessible() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/health")
                .then()
                .statusCode(200);
    }
}