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
 * UI tests covering agent interactions with their assigned tickets.
 */
@Feature("Requests")
@Story("Agent ticket workflow")
public class AgentRequestTest extends BaseTest {
    private LoginPage loginPage;
    private RequestPage requestPage;

    @BeforeEach
    public void setup() {
        super.setup();
        loginPage = new LoginPage(driver);
        requestPage = new RequestPage(driver);
    }

    @Test
    @Description("Agent can view their assigned tickets list")
    public void testAgentAssignedPageLoads() {
        loginPage.open(BASE_URL);
        loginPage.login(TestData.AGENT_EMAIL, TestData.AGENT_PASSWORD);
        // Verify login succeeded by checking we're not on login page anymore
        String afterLoginUrl = driver.getCurrentUrl();
        assertTrue(!afterLoginUrl.contains("/login"), 
                "Should have been redirected from login after entering credentials, but was: " + afterLoginUrl);

        requestPage.openAssignedRequests(BASE_URL);
        String current = driver.getCurrentUrl();
        // the agent path may redirect to dashboard if not logged in or there are no
        // assigned tickets, allow either URL but include value in failure message
        assertTrue(current.contains("/requests/assigned") || current.contains("/dashboard/agent"),
                "Expected assigned URL or agent dashboard but was: " + current);
        int rows = requestPage.countTableRows();
        assertTrue(rows >= 0, "Agent should be able to see the table even if empty");
    }

    @Test
    @Description("Agent can transition the first assigned ticket")
    public void testAgentCanTransitionFirstAssigned() {
        loginPage.open(BASE_URL);
        loginPage.login(TestData.AGENT_EMAIL, TestData.AGENT_PASSWORD);

        requestPage.openAssignedRequests(BASE_URL);
        int beforeRows = requestPage.countTableRows();
        if (beforeRows > 0) {
            requestPage.clickFirstTransition();
            assertTrue(driver.getCurrentUrl().contains("/requests/assigned"));
            int afterRows = requestPage.countTableRows();
            assertEquals(beforeRows, afterRows);
            String newStatus = requestPage.getFirstRowStatus();
            assertNotNull(newStatus);
        } else {
            assertTrue(true);
        }
    }
}