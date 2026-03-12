package com.amalitech.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page object for the "New Request" form and the requests list pages.
 */
public class RequestPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // form locators
    private final By titleField = By.id("title");
    private final By categorySelect = By.id("category");
    private final By prioritySelect = By.id("priority");
    private final By descriptionField = By.id("description");
    private final By submitButton = By.xpath("//button[@type='submit' and contains(.,'Submit Request')]");
    private final By successBanner = By.cssSelector("div.bg-green-50.border-green-200.text-green-700");
    private final By errorBanner = By.cssSelector("div.bg-red-50.border-red-200.text-red-700");

    // list locators
    private final By newRequestLink = By.xpath("//a[contains(.,'New Request')]");
    private final By tableRows = By.cssSelector("table tbody tr");

    public RequestPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void openNewRequest(String baseUrl) {
        driver.get(baseUrl + "/requests/new");
    }

    public void enterTitle(String title) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(titleField));
        field.clear();
        field.sendKeys(title);
    }

    public void selectCategory(String category) {
        WebElement selectElem = wait.until(ExpectedConditions.elementToBeClickable(categorySelect));
        Select select = new Select(selectElem);
        select.selectByValue(category);
    }

    public void selectPriority(String priority) {
        WebElement selectElem = wait.until(ExpectedConditions.elementToBeClickable(prioritySelect));
        Select select = new Select(selectElem);
        select.selectByValue(priority);
    }

    public void enterDescription(String desc) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(descriptionField));
        field.clear();
        field.sendKeys(desc);
    }

    public void clickSubmit() {
        wait.until(ExpectedConditions.elementToBeClickable(submitButton)).click();
    }

    public void submitRequest(String title, String category, String priority, String description) {
        enterTitle(title);
        selectCategory(category);
        selectPriority(priority);
        enterDescription(description);
        clickSubmit();
    }

    public boolean isSuccessBannerDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(successBanner)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getSuccessBannerText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(successBanner)).getText();
    }

    public boolean isErrorBannerDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(errorBanner)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Return the text of the error banner if present.  If no banner is visible
     * an empty string is returned.
     */
    public String getErrorBannerText() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(errorBanner)).getText();
        } catch (Exception e) {
            return "";
        }
    }

    public void openListPage(String baseUrl) {
        driver.get(baseUrl + "/requests");
    }

    /**
     * Navigate to the admin all‑requests view. Requires ADMIN login.
     */
    public void openAdminRequests(String baseUrl) {
        driver.get(baseUrl + "/admin/requests");
    }

    /**
     * Navigate to the agent assigned‑requests view. Requires AGENT login.
     */
    public void openAssignedRequests(String baseUrl) {
        driver.get(baseUrl + "/requests/assigned");
    }

    public int countTableRows() {
        try {
            return driver.findElements(tableRows).size();
        } catch (Exception e) {
            return 0;
        }
    }

    public String getFirstRowTitle() {
        try {
            WebElement first = driver.findElement(By.cssSelector("table tbody tr td:nth-child(2)"));
            return first.getText();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Clicks the transition button in the first row of whichever table is visible.
     * Useful for agent/admin workflows.
     */
    public void clickFirstTransition() {
        try {
            WebElement btn = driver.findElement(By.xpath("//table//button[contains(.,'Transition')][1]"));
            btn.click();
        } catch (Exception ignored) {
        }
    }

    /**
     * Read the status cell text from the first row (column 5 or 6 depending on layout).
     */
    public String getFirstRowStatus() {
        try {
            WebElement cell = driver.findElement(By.xpath("//table//tr[1]/td[contains(@class,'text-foreground')][5]"));
            return cell.getText();
        } catch (Exception e) {
            return "";
        }
    }
}
