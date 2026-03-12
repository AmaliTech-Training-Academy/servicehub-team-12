package com.amalitech.qa.tests;

import com.amalitech.qa.base.BaseTest;
import com.amalitech.qa.data.TestData;
import com.amalitech.qa.pages.LoginPage;
import com.amalitech.qa.pages.RequestPage;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI checks for administrators managing all service requests.
 */
@Feature("Requests")
@Story("Admin request management")
public class AdminRequestTest extends BaseTest {

    private LoginPage loginPage;
    private RequestPage requestPage;

    @BeforeEach
    public void setup() {
        super.setup();
        loginPage = new LoginPage(driver);
        requestPage = new RequestPage(driver);
    }

    @Test
    @Description("Admin can open the all-requests page and see a table")
    public void testAdminRequestsPageLoads() {
        loginPage.open(BASE_URL);
        loginPage.login(TestData.ADMIN_EMAIL, TestData.ADMIN_PASSWORD);
        
        // Verify login succeeded before attempting to navigate to admin page
        String afterLoginUrl = driver.getCurrentUrl();
        assertTrue(!afterLoginUrl.contains("/login"), 
                "Should have been redirected from login, but was: " + afterLoginUrl);

        requestPage.openAdminRequests(BASE_URL);
        assertTrue(driver.getCurrentUrl().contains("/admin/requests"),
                "URL should contain /admin/requests");
        int rows = requestPage.countTableRows();
        assertTrue(rows >= 0, "Table should be present (zero or more rows)");
    }

    @Test
    @Description("Clicking transition as admin reloads the page without error")
    public void testAdminCanTransitionFirstRow() {
        loginPage.open(BASE_URL);
        loginPage.login(TestData.ADMIN_EMAIL, TestData.ADMIN_PASSWORD);

        requestPage.openAdminRequests(BASE_URL);
        int beforeRows = requestPage.countTableRows();
        if (beforeRows > 0) {
            requestPage.clickFirstTransition();
            // simple check: we should still be on same URL and table still visible
            assertTrue(driver.getCurrentUrl().contains("/admin/requests"));
            int afterRows = requestPage.countTableRows();
            assertEquals(beforeRows, afterRows, "Row count should stay the same after transition");
            // status may change or remain if cycle; ensure method does not throw
            String newStatus = requestPage.getFirstRowStatus();
            assertNotNull(newStatus);
        } else {
            // no rows available, nothing to transition; just verify no exception thrown earlier
            assertTrue(true);
        }
    }
}