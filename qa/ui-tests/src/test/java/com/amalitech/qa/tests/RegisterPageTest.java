package com.amalitech.qa.tests;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.data.TestData;
import com.amalitech.qa.pages.RegisterPage;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI tests for the registration page.  Covers basic loading, happy path,
 * validation errors and duplicate‑account behaviour.
 */
@Feature("Authentication")
@Story("Registration")
public class RegisterPageTest extends BaseTest {

    private RegisterPage registerPage;

    @BeforeEach
    public void openRegisterPage() {
        registerPage = new RegisterPage(driver);
        registerPage.open(BASE_URL);
    }

    @Test
    @Description("Verify registration page loads with correct URL and title")
    public void testRegisterPageLoads() {
        assertTrue(registerPage.getCurrentUrl().endsWith("/register"),
                "URL should end with /register");
        assertTrue(registerPage.getPageTitle().contains("Create Account") || registerPage.getPageTitle().contains("ServiceHub"),
                "Page title should contain 'Create Account' or 'ServiceHub'");
    }

    @Test
    @Description("Verify valid registration with unique email redirects to login with registered query")
    public void testValidRegistrationRedirectsToLogin() {
        String uniqueEmail = TestData.generateUniqueEmail("user");
        registerPage.register(TestData.NEW_USER_FIRST_NAME, TestData.NEW_USER_LAST_NAME, uniqueEmail, TestData.DEPT_IT, TestData.NEW_USER_PASSWORD, TestData.NEW_USER_CONFIRM_PASSWORD);
        // successful register should redirect to login with query parameter
        assertTrue(registerPage.getCurrentUrl().contains("/login?registered"),
                "URL should contain '/login?registered' after successful registration");
    }

    @Test
    @Description("Verify invalid email format shows error message")
    public void testInvalidEmailShowsError() {
        registerPage.register(TestData.NEW_USER_FIRST_NAME, TestData.NEW_USER_LAST_NAME, TestData.INVALID_EMAIL_FORMAT, TestData.DEPT_IT, TestData.NEW_USER_PASSWORD, TestData.NEW_USER_CONFIRM_PASSWORD);
        
        // Invalid email should either show error message or keep user on register page
        boolean hasError = registerPage.isErrorMessageDisplayed() || registerPage.getCurrentUrl().contains("/register");
        assertTrue(hasError,
                "Error message should be displayed or user should stay on register page for invalid email");
    }

    @Test
    @Description("Verify duplicate email address shows error message")
    public void testDuplicateEmailShowsError() {
        registerPage.register(TestData.NEW_USER_FIRST_NAME, TestData.NEW_USER_LAST_NAME, TestData.ADMIN_EMAIL, TestData.DEPT_IT, TestData.NEW_USER_PASSWORD, TestData.NEW_USER_CONFIRM_PASSWORD);
        
        // Duplicate email should either show error message or keep user on register page
        boolean hasErrorOrStaysOnRegister = registerPage.isErrorMessageDisplayed() || registerPage.getCurrentUrl().contains("/register");
        assertTrue(hasErrorOrStaysOnRegister,
                "Error message should be displayed or user should stay on register page for duplicate email");
        
        // If error is displayed, verify it mentions email
        if (registerPage.isErrorMessageDisplayed()) {
            String text = registerPage.getErrorMessageText().toLowerCase();
            assertTrue(text.contains("email") || text.contains("exists"),
                    "Error message should mention email or existence");
        }
    }

    @Test
    @Description("Verify empty form submission keeps user on registration page")
    public void testEmptyFormStaysOnRegisterPage() {
        registerPage.clickRegister();
        assertTrue(registerPage.getCurrentUrl().contains("/register"),
                "URL should still contain /register after empty form submission");
    }
}
