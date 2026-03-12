package com.amalitech.qa.testdata;

public class ServiceRequestTestData {

    // Valid request data
    public static final String VALID_TITLE = "Laptop not working";
    public static final String VALID_DESCRIPTION = "Screen is blank";
    public static final String VALID_CATEGORY = "IT_SUPPORT";
    public static final String VALID_PRIORITY = "HIGH";
    public static final String REQUESTER_ID = "d05fef47-a82d-486f-b347-6be3d84860f5"; // admin@amalitech.com

    // Invalid data
    public static final String INVALID_CATEGORY = "INVALID_CATEGORY";
    public static final String INVALID_PRIORITY = "INVALID_PRIORITY";
    public static final String INVALID_ID = "00000000-0000-0000-0000-000000000000";

    // Valid request body
    public static final String VALID_CREATE_BODY =
            "{"
                    + "\"title\": \"" + VALID_TITLE + "\","
                    + "\"description\": \"" + VALID_DESCRIPTION + "\","
                    + "\"category\": \"" + VALID_CATEGORY + "\","
                    + "\"priority\": \"" + VALID_PRIORITY + "\","
                    + "\"requesterId\": \"" + REQUESTER_ID + "\""
                    + "}";

    // Missing title body
    public static final String MISSING_TITLE_BODY =
            "{"
                    + "\"description\": \"" + VALID_DESCRIPTION + "\","
                    + "\"category\": \"" + VALID_CATEGORY + "\","
                    + "\"priority\": \"" + VALID_PRIORITY + "\","
                    + "\"requesterId\": \"" + REQUESTER_ID + "\""
                    + "}";

    // Missing category body
    public static final String MISSING_CATEGORY_BODY =
            "{"
                    + "\"title\": \"" + VALID_TITLE + "\","
                    + "\"description\": \"" + VALID_DESCRIPTION + "\","
                    + "\"priority\": \"" + VALID_PRIORITY + "\","
                    + "\"requesterId\": \"" + REQUESTER_ID + "\""
                    + "}";

    // Missing priority body
    public static final String MISSING_PRIORITY_BODY =
            "{"
                    + "\"title\": \"" + VALID_TITLE + "\","
                    + "\"description\": \"" + VALID_DESCRIPTION + "\","
                    + "\"category\": \"" + VALID_CATEGORY + "\","
                    + "\"requesterId\": \"" + REQUESTER_ID + "\""
                    + "}";

    // Missing requesterId body
    public static final String MISSING_REQUESTER_ID_BODY =
            "{"
                    + "\"title\": \"" + VALID_TITLE + "\","
                    + "\"description\": \"" + VALID_DESCRIPTION + "\","
                    + "\"category\": \"" + VALID_CATEGORY + "\","
                    + "\"priority\": \"" + VALID_PRIORITY + "\""
                    + "}";

    // Invalid category value body
    public static final String INVALID_CATEGORY_BODY =
            "{"
                    + "\"title\": \"" + VALID_TITLE + "\","
                    + "\"description\": \"" + VALID_DESCRIPTION + "\","
                    + "\"category\": \"" + INVALID_CATEGORY + "\","
                    + "\"priority\": \"" + VALID_PRIORITY + "\","
                    + "\"requesterId\": \"" + REQUESTER_ID + "\""
                    + "}";

    // Update body
    public static final String VALID_UPDATE_BODY =
            "{"
                    + "\"title\": \"Laptop fixed\","
                    + "\"description\": \"Issue resolved\","
                    + "\"category\": \"" + VALID_CATEGORY + "\","
                    + "\"priority\": \"" + VALID_PRIORITY + "\","
                    + "\"requesterId\": \"" + REQUESTER_ID + "\""
                    + "}";
}