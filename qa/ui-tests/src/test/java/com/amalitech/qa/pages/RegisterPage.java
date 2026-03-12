package com.amalitech.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object Model for the registration page.
 * Encapsulates locators and interactions for /register.
 */
public class RegisterPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // locators
    private final By firstNameField = By.id("firstName");
    private final By lastNameField = By.id("lastName");
    private final By emailField = By.id("email");
    private final By departmentSelect = By.id("department");
    private final By passwordField = By.id("newPassword");
    private final By confirmPasswordField = By.id("confirmPassword");
    private final By submitButton = By.cssSelector("button[type='submit']");
    private final By errorMessage = By.cssSelector(
            "div.flex.items-center.gap-3.bg-red-50.border.border-red-200.text-red-700"
    );

    public RegisterPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void open(String baseUrl) {
        driver.get(baseUrl + "/register");
    }

    public void enterFirstName(String first) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(firstNameField));
        field.clear();
        field.sendKeys(first);
    }

    public void enterLastName(String last) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(lastNameField));
        field.clear();
        field.sendKeys(last);
    }

    public void enterEmail(String email) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(emailField));
        field.clear();
        field.sendKeys(email);
    }

    public void selectDepartment(String dept) {
        WebElement selectElem = wait.until(ExpectedConditions.elementToBeClickable(departmentSelect));
        Select select = new Select(selectElem);
        select.selectByVisibleText(dept);
    }

    public void enterPassword(String password) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(passwordField));
        field.clear();
        field.sendKeys(password);
    }

    public void enterConfirmPassword(String password) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(confirmPasswordField));
        field.clear();
        field.sendKeys(password);
    }

    public void clickRegister() {
        wait.until(ExpectedConditions.elementToBeClickable(submitButton)).click();
    }

    public void register(String first, String last, String email, String dept, String password, String confirm) {
        enterFirstName(first);
        enterLastName(last);
        enterEmail(email);
        selectDepartment(dept);
        enterPassword(password);
        enterConfirmPassword(confirm);
        clickRegister();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public boolean isErrorMessageDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(errorMessage)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getErrorMessageText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(errorMessage)).getText();
    }
}
