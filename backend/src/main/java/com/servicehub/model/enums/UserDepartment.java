package com.servicehub.model.enums;

import java.util.Arrays;

public enum UserDepartment {
    IT("IT", RequestCategory.IT_SUPPORT),
    HR("HR", RequestCategory.HR_REQUEST),
    FACILITIES("Facilities", RequestCategory.FACILITIES);

    private final String displayName;
    private final RequestCategory requestCategory;

    UserDepartment(String displayName, RequestCategory requestCategory) {
        this.displayName = displayName;
        this.requestCategory = requestCategory;
    }

    public String getDisplayName() {
        return displayName;
    }

    public RequestCategory getRequestCategory() {
        return requestCategory;
    }

    public static UserDepartment fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(department -> department.displayName.equalsIgnoreCase(value)
                        || department.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Department must be one of: IT, HR, Facilities"));
    }

    public static String normalize(String value) {
        UserDepartment department = fromValue(value);
        return department == null ? null : department.getDisplayName();
    }
}
