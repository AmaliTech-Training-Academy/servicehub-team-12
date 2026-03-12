package com.amalitech.qa.tests;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.data.TestData;
import com.amalitech.qa.pages.LoginPage;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI tests for the Login page.
 * Tests cover: page load, valid login, invalid credentials,
 * error message text, and empty form submission.
 * URL: http://localhost:8080/login
 */
@Feature("Authentication")
@Story("Login")
public class LoginPageTest extends BaseTest {

    private LoginPage loginPage;

    /**
     * Opens the login page before each test.
     */
    @BeforeEach
    public void openLoginPage() {
        loginPage = new LoginPage(driver);
        loginPage.open(BASE_URL);
    }

    /**
     * Verifies that the login page loads successfully.
     * Checks the URL ends with /login and the title contains ServiceHub.
     */
    @Test
    @Description("Verify login page loads with correct URL and title")
    public void testLoginPageLoads() {
        assertTrue(loginPage.getCurrentUrl().endsWith("/login"),
                "URL should end with /login");
        assertTrue(loginPage.getPageTitle().contains("ServiceHub"),
                "Page title should contain ServiceHub");
    }

    /**
     * Verifies that a valid login redirects away from the login page.
     * The admin user should be redirected to their dashboard after login.
     */
    @Test
    @Description("Verify valid credentials redirect to dashboard")
    public void testValidLoginRedirectsToDashboard() {
        loginPage.login(TestData.ADMIN_EMAIL, TestData.ADMIN_PASSWORD);
        assertFalse(loginPage.getCurrentUrl().contains("/login"),
                "URL should not contain /login after successful login");
    }

    /**
     * Verifies that an invalid password shows the error message.
     * The error alert should be visible when the password is wrong.
     */
    @Test
    @Description("Verify invalid password shows error message")
    public void testInvalidPasswordShowsError() {
        loginPage.login(TestData.ADMIN_EMAIL, TestData.INVALID_PASSWORD);
        assertTrue(loginPage.isErrorMessageDisplayed(),
                "Error message should be displayed for invalid password");
    }

    /**
     * Verifies that an invalid email shows the error message.
     * The error alert should be visible when the email does not exist.
     */
    @Test
    @Description("Verify invalid email shows error message")
    public void testInvalidEmailShowsError() {
        loginPage.login(TestData.NONEXISTENT_EMAIL, TestData.ADMIN_PASSWORD);
        assertTrue(loginPage.isErrorMessageDisplayed(),
                "Error message should be displayed for invalid email");
    }

    /**
     * Verifies that the error message contains the correct text.
     * The message should read "Invalid email or password. Please try again."
     */
    @Test
    @Description("Verify error message text contains correct content")
    public void testErrorMessageText() {
        loginPage.login(TestData.ADMIN_EMAIL, TestData.INVALID_PASSWORD);
        assertTrue(loginPage.getErrorMessageText().contains("Invalid email or password"),
                "Error message should contain 'Invalid email or password'");
    }

    /**
     * Verifies that submitting an empty form keeps the user on the login page.
     * HTML5 required validation should prevent form submission.
     */
    @Test
    @Description("Verify empty form submission keeps user on login page")
    public void testEmptyFormStaysOnLoginPage() {
        loginPage.clickSignIn();
        assertTrue(loginPage.getCurrentUrl().contains("/login"),
                "URL should still contain /login after empty form submission");
    }
}