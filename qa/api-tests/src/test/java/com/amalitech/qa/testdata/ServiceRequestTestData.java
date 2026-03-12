package com.amalitech.qa.testdata;

/**
 * Test data class for Service Request API tests.
 * Contains constants for valid and invalid test data,
 * and dynamic body builder methods that accept a requesterId
 * at runtime to avoid hardcoding user-specific UUIDs.
 */
public class ServiceRequestTestData {

    // ─── Valid Request Data ───────────────────────────────────────────────────

    /** Valid title for a service request */
    public static final String VALID_TITLE = "Laptop not working";

    /** Valid description for a service request */
    public static final String VALID_DESCRIPTION = "Screen is blank";

    /** Valid category — must match RequestCategory enum in the backend */
    public static final String VALID_CATEGORY = "IT_SUPPORT";

    /** Valid priority — must match RequestPriority enum in the backend */
    public static final String VALID_PRIORITY = "HIGH";

    // ─── Invalid Request Data ─────────────────────────────────────────────────

    /** Invalid category value — not a valid RequestCategory enum value */
    public static final String INVALID_CATEGORY = "INVALID_CATEGORY";

    /** Invalid priority value — not a valid RequestPriority enum value */
    public static final String INVALID_PRIORITY = "INVALID_PRIORITY";

    /**
     * Invalid UUID used to test 404 responses.
     * This UUID is guaranteed to never exist in the database.
     */
    public static final String INVALID_ID = "00000000-0000-0000-0000-000000000000";

    // ─── Dynamic Request Body Builders ───────────────────────────────────────

    /**
     * Builds a valid service request body with all required fields.
     * requesterId is passed dynamically from the login response
     * to avoid hardcoding UUIDs across different environments.
     *
     * @param requesterId the UUID of the user making the request
     * @return JSON string with all required fields
     */
    public static String validCreateBody(String requesterId) {
        return "{"
                + "\"title\": \"" + VALID_TITLE + "\","
                + "\"description\": \"" + VALID_DESCRIPTION + "\","
                + "\"category\": \"" + VALID_CATEGORY + "\","
                + "\"priority\": \"" + VALID_PRIORITY + "\","
                + "\"requesterId\": \"" + requesterId + "\""
                + "}";
    }

    /**
     * Builds a request body with the title field missing.
     * Used to test that the backend returns 400 when title is absent.
     *
     * @param requesterId the UUID of the user making the request
     * @return JSON string missing the title field
     */
    public static String missingTitleBody(String requesterId) {
        return "{"
                + "\"description\": \"" + VALID_DESCRIPTION + "\","
                + "\"category\": \"" + VALID_CATEGORY + "\","
                + "\"priority\": \"" + VALID_PRIORITY + "\","
                + "\"requesterId\": \"" + requesterId + "\""
                + "}";
    }

    /**
     * Builds a request body with the category field missing.
     * Used to test that the backend returns 400 when category is absent.
     *
     * @param requesterId the UUID of the user making the request
     * @return JSON string missing the category field
     */
    public static String missingCategoryBody(String requesterId) {
        return "{"
                + "\"title\": \"" + VALID_TITLE + "\","
                + "\"description\": \"" + VALID_DESCRIPTION + "\","
                + "\"priority\": \"" + VALID_PRIORITY + "\","
                + "\"requesterId\": \"" + requesterId + "\""
                + "}";
    }

    /**
     * Builds a request body with the priority field missing.
     * Used to test that the backend returns 400 when priority is absent.
     *
     * @param requesterId the UUID of the user making the request
     * @return JSON string missing the priority field
     */
    public static String missingPriorityBody(String requesterId) {
        return "{"
                + "\"title\": \"" + VALID_TITLE + "\","
                + "\"description\": \"" + VALID_DESCRIPTION + "\","
                + "\"category\": \"" + VALID_CATEGORY + "\","
                + "\"requesterId\": \"" + requesterId + "\""
                + "}";
    }

    /**
     * Builds a request body with an invalid category value.
     * Used to test that the backend returns 400 when the category
     * does not match any value in the RequestCategory enum.
     *
     * @param requesterId the UUID of the user making the request
     * @return JSON string with an invalid category value
     */
    public static String invalidCategoryBody(String requesterId) {
        return "{"
                + "\"title\": \"" + VALID_TITLE + "\","
                + "\"description\": \"" + VALID_DESCRIPTION + "\","
                + "\"category\": \"" + INVALID_CATEGORY + "\","
                + "\"priority\": \"" + VALID_PRIORITY + "\","
                + "\"requesterId\": \"" + requesterId + "\""
                + "}";
    }

    /**
     * Builds a valid update request body.
     * Used to test that an existing service request can be updated
     * successfully with new title and description values.
     *
     * @param requesterId the UUID of the user making the request
     * @return JSON string with updated field values
     */
    public static String validUpdateBody(String requesterId) {
        return "{"
                + "\"title\": \"Laptop fixed\","
                + "\"description\": \"Issue resolved\","
                + "\"category\": \"" + VALID_CATEGORY + "\","
                + "\"priority\": \"" + VALID_PRIORITY + "\","
                + "\"requesterId\": \"" + requesterId + "\""
                + "}";
    }

    // ─── Static Body (No requesterId needed) ─────────────────────────────────

    /**
     * Request body with the requesterId field intentionally absent.
     * Used to test that the backend returns 400 when requesterId is missing.
     * This body is static since no requesterId is needed for this test case.
     */
    public static final String MISSING_REQUESTER_ID_BODY =
            "{"
                    + "\"title\": \"" + VALID_TITLE + "\","
                    + "\"description\": \"" + VALID_DESCRIPTION + "\","
                    + "\"category\": \"" + VALID_CATEGORY + "\","
                    + "\"priority\": \"" + VALID_PRIORITY + "\""
                    + "}";
}