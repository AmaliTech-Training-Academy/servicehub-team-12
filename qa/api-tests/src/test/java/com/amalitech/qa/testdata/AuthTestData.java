package com.amalitech.qa.testdata;
/**
 * Provides authentication-related test payloads and constants.
 */

public class AuthTestData {

    // Valid credentials
    public static final String ADMIN_EMAIL = "admin@amalitech.com";
    public static final String ADMIN_PASSWORD = "password123";

    // Valid registration data
    public static final String REGISTER_FIRST_NAME = "Test";
    public static final String REGISTER_LAST_NAME = "User";
    public static final String REGISTER_EMAIL = "testuser3@amalitech.com";
    public static final String REGISTER_PASSWORD = "password123";
    public static final String REGISTER_DEPARTMENT = "IT";
    public static final String REGISTER_FULL_NAME = REGISTER_FIRST_NAME + " " + REGISTER_LAST_NAME;

    // Invalid data
    public static final String INVALID_EMAIL = "notanemail";
    public static final String INVALID_PASSWORD = "wrong";
    public static final String SHORT_PASSWORD = "pass";
    public static final String INVALID_TOKEN = "invalidtoken123";
    public static final String EMPTY = "";

    // Request bodies
    public static final String VALID_LOGIN_BODY =
            "{\"email\": \"" + ADMIN_EMAIL + "\", \"password\": \"" + ADMIN_PASSWORD + "\"}";

    public static final String VALID_REGISTER_BODY =
            "{"
                    + "\"firstName\": \"" + REGISTER_FIRST_NAME + "\","
                    + "\"lastName\": \"" + REGISTER_LAST_NAME + "\","
                    + "\"email\": \"" + REGISTER_EMAIL + "\","
                    + "\"password\": \"" + REGISTER_PASSWORD + "\","
                    + "\"confirmPassword\": \"" + REGISTER_PASSWORD + "\","
                    + "\"department\": \"" + REGISTER_DEPARTMENT + "\""
                    + "}";

    public static final String DUPLICATE_EMAIL_BODY =
            "{"
                    + "\"firstName\": \"" + REGISTER_FIRST_NAME + "\","
                    + "\"lastName\": \"" + REGISTER_LAST_NAME + "\","
                    + "\"email\": \"" + REGISTER_EMAIL + "\","
                    + "\"password\": \"" + REGISTER_PASSWORD + "\","
                    + "\"confirmPassword\": \"" + REGISTER_PASSWORD + "\","
                    + "\"department\": \"" + REGISTER_DEPARTMENT + "\""
                    + "}";

    public static final String MISSING_FIELDS_BODY =
            "{"
                    + "\"firstName\": \"" + EMPTY + "\","
                    + "\"lastName\": \"" + EMPTY + "\","
                    + "\"email\": \"" + EMPTY + "\","
                    + "\"password\": \"" + EMPTY + "\","
                    + "\"confirmPassword\": \"" + EMPTY + "\""
                    + "}";

    public static final String SHORT_PASSWORD_BODY =
            "{"
                    + "\"firstName\": \"" + REGISTER_FIRST_NAME + "\","
                    + "\"lastName\": \"" + REGISTER_LAST_NAME + "\","
                    + "\"email\": \"shortpass@amalitech.com\","
                    + "\"password\": \"" + SHORT_PASSWORD + "\","
                    + "\"confirmPassword\": \"" + SHORT_PASSWORD + "\""
                    + "}";

    public static final String INVALID_CREDENTIALS_BODY =
            "{\"email\": \"" + ADMIN_EMAIL + "\", \"password\": \"" + INVALID_PASSWORD + "\"}";

    public static final String INVALID_EMAIL_FORMAT_BODY =
            "{\"email\": \"" + INVALID_EMAIL + "\", \"password\": \"" + ADMIN_PASSWORD + "\"}";

    public static final String EMPTY_LOGIN_BODY =
            "{\"email\": \"" + EMPTY + "\", \"password\": \"" + EMPTY + "\"}";
}