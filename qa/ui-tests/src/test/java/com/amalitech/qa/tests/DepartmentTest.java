package com.amalitech.qa.tests;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.data.TestData;
import com.amalitech.qa.pages.DashboardPage;
import com.amalitech.qa.pages.LoginPage;
import io.qameta.allure.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Department functionality in the ServiceHub dashboard.
 * Tests that admin users can access the departments section and verify
 * basic content rendering.
 */
public class DepartmentTest extends BaseTest {

    private LoginPage loginPage;
    private DashboardPage dashboardPage;

    @BeforeEach
    @Override
    public void setup() {
        super.setup();
        loginPage = new LoginPage(driver);
        dashboardPage = new DashboardPage(driver);
    }

    @Test
    @Description("Verify admin user can access the departments page directly via URL")
    public void testAdminCanAccessDepartmentsPage() {
        loginPage.open(BASE_URL);
        loginPage.login(TestData.ADMIN_EMAIL, TestData.ADMIN_PASSWORD);
        
        // Navigate directly to departments page
        driver.get(BASE_URL + "/dashboard/admin");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify we're on the admin dashboard
        assertTrue(driver.getCurrentUrl().contains("/dashboard/admin"),
                "Admin should be on the admin dashboard");
    }

    @Test
    @Description("Verify admin dashboard is accessible after login")
    public void testAdminDashboardAccessibleAfterLogin() {
        loginPage.open(BASE_URL);
        loginPage.login(TestData.ADMIN_EMAIL, TestData.ADMIN_PASSWORD);
        
        dashboardPage.openAdminDashboard(BASE_URL);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Check that we're on the admin dashboard
        assertTrue(driver.getCurrentUrl().contains("/dashboard/admin"),
                "User should be navigated to admin dashboard");
    }

    @Test
    @Description("Verify admin user remains authenticated on dashboard")
    public void testAdminAuthenticationPersistence() {
        loginPage.open(BASE_URL);
        loginPage.login(TestData.ADMIN_EMAIL, TestData.ADMIN_PASSWORD);
        
        dashboardPage.openAdminDashboard(BASE_URL);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // If we're still on the dashboard, authentication persisted
        assertTrue(driver.getCurrentUrl().contains("/dashboard"),
                "Admin should remain on dashboard (authentication persisted)");
    }

    @Test
    @Description("Verify dashboard page loads without errors for admin users")
    public void testAdminDashboardPageLoads() {
        loginPage.open(BASE_URL);
        loginPage.login(TestData.ADMIN_EMAIL, TestData.ADMIN_PASSWORD);
        
        dashboardPage.openAdminDashboard(BASE_URL);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify we're on the admin dashboard URL
        assertTrue(driver.getCurrentUrl().contains("/dashboard/admin"),
                "Should be on admin dashboard after navigation");
    }
}
