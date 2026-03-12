package com.amalitech.qa.data;

/**
 * Centralized test data holder.
 * Contains credentials, department names, and other constants for UI tests.
 * Eliminates hardcoding of test data across test classes.
 */
public class TestData {

    // ─── Admin User ────────────────────────────────────────────────────────
    public static final String ADMIN_EMAIL = "mgr@servicehub.com";
    public static final String ADMIN_PASSWORD = "password123";
    public static final String ADMIN_FULL_NAME = "Admin User"; // Adjust if needed

    // ─── Agent User ────────────────────────────────────────────────────────
    public static final String AGENT_EMAIL = "agent@servicehub.com";
    public static final String AGENT_PASSWORD = "password123";
    public static final String AGENT_FULL_NAME = "Agent User"; // Adjust if needed

    // ─── Regular User ──────────────────────────────────────────────────────
    public static final String USER_EMAIL = "emp@servicehub.com";
    public static final String USER_PASSWORD = "password123";
    public static final String USER_FULL_NAME = "Regular User"; // Adjust if needed

    // ─── New Registration Data ─────────────────────────────────────────────
    public static final String NEW_USER_FIRST_NAME = "Test";
    public static final String NEW_USER_LAST_NAME = "Account";
    public static final String NEW_USER_PASSWORD = "Password123!";
    public static final String NEW_USER_CONFIRM_PASSWORD = "Password123!";

    // ─── Department Options ────────────────────────────────────────────────
    public static final String DEPT_IT = "IT";
    public static final String DEPT_HR = "HR";
    public static final String DEPT_FACILITIES = "Facilities";

    // ─── Invalid Test Data ─────────────────────────────────────────────────
    public static final String INVALID_EMAIL_FORMAT = "notanemail";
    public static final String INVALID_PASSWORD = "wrongpassword";
    public static final String NONEXISTENT_EMAIL = "notauser@amalitech.com";

    // ─── URL Query Parameters ──────────────────────────────────────────────
    public static final String LOGIN_PAGE_QUERY_REGISTERED = "?registered";
    public static final String LOGIN_PAGE_QUERY_LOGOUT = "?logout";
    public static final String LOGIN_PAGE_QUERY_ERROR = "?error";

    // ─── Utility: Generate unique email ────────────────────────────────────
    public static String generateUniqueEmail(String baseName) {
        return baseName + System.currentTimeMillis() + "@test.com";
    }

    // ─── Request data constants ───────────────────────────────────────────
    public static final String REQUEST_CATEGORY_IT = "IT_SUPPORT";
    public static final String REQUEST_CATEGORY_HR = "HR_REQUEST";
    public static final String REQUEST_CATEGORY_FACILITIES = "FACILITIES";

    public static final String PRIORITY_LOW = "LOW";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_CRITICAL = "CRITICAL";

    public static final String SAMPLE_REQUEST_TITLE = "Test request title";
    public static final String SAMPLE_REQUEST_DESCRIPTION = "This is a test description for a new service request.";

    // ─── Utility: Generate unique email with domain ────────────────────────
    public static String generateUniqueEmail(String baseName, String domain) {
        return baseName + System.currentTimeMillis() + "@" + domain;
    }
}
