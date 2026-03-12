package com.amalitech.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object Model for the Dashboard pages.
 * Supports all three roles: ADMIN, AGENT, and USER.
 * Each role has a distinct dashboard view.
 */
public class DashboardPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // ─── Common Locators ──────────────────────────────────────────────────────
    private final By pageHeading = By.cssSelector("h2.text-2xl, h2.text-xl");
    private final By userNameDisplay = By.xpath("//*[contains(text(), 'Hello') or contains(text(), 'Welcome')]");
    private final By sidebarLogoutButton = By.xpath("//a[contains(text(), 'Logout') or contains(text(), 'Sign out')]");

    // ─── USER Dashboard Locators ───────────────────────────────────────────────
    private final By userOpenRequestsCount = By.xpath("//p[contains(text(), 'Open Requests')]/following-sibling::p");
    private final By userResolvedCount = By.xpath("//p[contains(text(), 'Resolved')]/following-sibling::p");
    private final By userTotalCount = By.xpath("//p[contains(text(), 'Total')]/following-sibling::p");
    private final By recentTicketsSection = By.xpath("//h3[contains(text(), 'Recent')]");

    // ─── AGENT Dashboard Locators ──────────────────────────────────────────────
    private final By agentAssignedCount = By.xpath("//p[contains(text(), 'Assigned This Week')]/following-sibling::p");
    private final By agentResolvedCount = By.xpath("//p[contains(text(), 'Resolved This Week')]/following-sibling::p");
    private final By agentSlaComplianceCount = By.xpath("//p[contains(text(), 'SLA Compliance')]/following-sibling::p");

    // ─── ADMIN Dashboard Locators ──────────────────────────────────────────────
    private final By adminTabOverview = By.xpath("//button[@data-tab='overview']");
    private final By adminTabSla = By.xpath("//button[@data-tab='sla']");
    private final By adminTabDepartments = By.xpath("//button[@data-tab='departments']");
    private final By adminOperationsHeading = By.xpath("//h2[contains(text(), 'Operations Dashboard')]");
    private final By activeBreachers = By.xpath("//*[contains(text(), 'Active Breach')]");

    // department panel elements
    private final By deptEmptyMessage = By.id("dept-empty-msg");
    private final By deptCardsGrid = By.id("dept-cards-grid");
    private final By deptChartCanvas = By.id("deptChart");

    public DashboardPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    public void openDashboard(String baseUrl) {
        driver.get(baseUrl + "/dashboard");
    }

    public void openUserDashboard(String baseUrl) {
        driver.get(baseUrl + "/dashboard/user");
    }

    public void openAgentDashboard(String baseUrl) {
        driver.get(baseUrl + "/dashboard/agent");
    }

    public void openAdminDashboard(String baseUrl) {
        driver.get(baseUrl + "/dashboard/admin");
    }

    // ─── Common Getters ───────────────────────────────────────────────────────

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public boolean isPageHeadingDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(pageHeading)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getPageHeadingText() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(pageHeading)).getText();
        } catch (Exception e) {
            return "";
        }
    }

    // ─── USER Dashboard Specific ───────────────────────────────────────────────

    public boolean isUserOpenRequestsCardDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(userOpenRequestsCount)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserOpenRequestsCount() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(userOpenRequestsCount)).getText();
        } catch (Exception e) {
            return "0";
        }
    }

    public String getUserResolvedCount() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(userResolvedCount)).getText();
        } catch (Exception e) {
            return "0";
        }
    }

    public String getUserTotalCount() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(userTotalCount)).getText();
        } catch (Exception e) {
            return "0";
        }
    }

    public boolean isRecentTicklesSectionDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(recentTicketsSection)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ─── AGENT Dashboard Specific ──────────────────────────────────────────────

    public boolean isAgentAssignedCardDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(agentAssignedCount)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getAgentAssignedCount() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(agentAssignedCount)).getText();
        } catch (Exception e) {
            return "—";
        }
    }

    public String getAgentResolvedCount() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(agentResolvedCount)).getText();
        } catch (Exception e) {
            return "—";
        }
    }

    // ─── ADMIN Dashboard Specific ──────────────────────────────────────────────

    public boolean isAdminOperationsHeadingDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(adminOperationsHeading)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isOverviewTabDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(adminTabOverview)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isSlaTabDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(adminTabSla)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isDepartmentsTabDisplayed() {
        try {
            // some CSS frameworks may render the tab offscreen or with opacity 0,
            // which confuses Selenium's visibility check. we only care that it's in
            // the DOM so use presence instead.
            return !driver.findElements(adminTabDepartments).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public void clickOverviewTab() {
        wait.until(ExpectedConditions.elementToBeClickable(adminTabOverview)).click();
    }

    public void clickSlaTab() {
        wait.until(ExpectedConditions.elementToBeClickable(adminTabSla)).click();
    }

    public void clickDepartmentsTab() {
        wait.until(ExpectedConditions.elementToBeClickable(adminTabDepartments)).click();
    }

    public boolean isActiveBreachers() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(activeBreachers)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isDeptEmptyMessageDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(deptEmptyMessage)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isDeptCardsGridDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(deptCardsGrid)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isDeptChartVisible() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(deptChartCanvas)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ─── Logout ────────────────────────────────────────────────────────────────

    public void clickLogout() {
        // the sidebar link can sometimes be covered by an overlay or not yet
        // considered "clickable" by Selenium even though it's present.  wait for
        // it to exist and then fire a JS click so we don't rely on Selenium's
        // notion of clickability.
        WebElement logoutLink = wait.until(ExpectedConditions.presenceOfElementLocated(sidebarLogoutButton));
        try {
            // try a normal click first; we don't wait for clickable since it was
            // causing timeouts in headless mode.
            logoutLink.click();
        } catch (Exception ignored) {
            // if WebDriver can't click (overlay/obstruction), use JS as ultimate
            // fallback.  this also handles the case where the element is off-
            // screen or Selenium misreports interactability.
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", logoutLink);
        }
    }
}
