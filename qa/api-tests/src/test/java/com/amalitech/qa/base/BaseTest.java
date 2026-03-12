package com.amalitech.qa.base;

import io.github.cdimascio.dotenv.Dotenv;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;

public class BaseTest {

    @BeforeAll
    public static void setup() {
        Dotenv dotenv = Dotenv.configure().directory("../../").load();
        RestAssured.baseURI = dotenv.get("BASE_URL");
    }
}