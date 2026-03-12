package com.amalitech.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object Model for the Login page.
 * Encapsulates all locators and interactions for the login page.
 * URL: http://localhost:8080/login
 */
public class LoginPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // ─── Locators ─────────────────────────────────────────────────────────────

    private final By emailField = By.id("username");
    private final By passwordField = By.id("password");
    private final By submitButton = By.cssSelector("button[type='submit']");
    private final By errorMessage = By.cssSelector(
            "div.flex.items-center.gap-3.bg-red-50.border.border-red-200.text-red-700"
    );
    private final By logoutMessage = By.cssSelector(
            "div.flex.items-center.gap-3.bg-green-50.border.border-green-200.text-green-700"
    );

    // ─── Constructor ──────────────────────────────────────────────────────────

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    /**
     * Navigates to the login page.
     */
    public void open(String baseUrl) {
        driver.get(baseUrl + "/login");
    }

    /**
     * Enters the email address into the email field.
     *
     * @param email the email address to enter
     */
    public void enterEmail(String email) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(emailField));
        field.clear();
        field.sendKeys(email);
    }

    /**
     * Enters the password into the password field.
     *
     * @param password the password to enter
     */
    public void enterPassword(String password) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(passwordField));
        field.clear();
        field.sendKeys(password);
    }

    /**
     * Clicks the submit button to attempt login.
     */
    public void clickSignIn() {
        wait.until(ExpectedConditions.elementToBeClickable(submitButton)).click();
    }

    /**
     * Performs a full login with the given credentials.
     * Waits for the redirect after login to complete.
     *
     * @param email    the email address
     * @param password the password
     */
    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickSignIn();
        // Wait for redirect to complete (check that we're no longer on login page)
        try {
            wait.until(ExpectedConditions.not(
                    ExpectedConditions.urlContains("/login")));
        } catch (Exception e) {
            // If wait times out, we may still have a session cookie
        }
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    /**
     * Returns the current page URL.
     */
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Returns the current page title.
     */
    public String getPageTitle() {
        return driver.getTitle();
    }

    /**
     * Returns true if the error message is displayed on the page.
     */
    public boolean isErrorMessageDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(errorMessage)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the text content of the error message element.
     */
    public String getErrorMessageText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(errorMessage)).getText();
    }

    /**
     * Returns true if the logout success message is displayed.
     */
    public boolean isLogoutMessageDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(logoutMessage)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}