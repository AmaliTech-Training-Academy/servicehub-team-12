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
 * UI tests for service request workflows: creating a new request and viewing the list.
 */
@Feature("Requests")
@Story("User request workflows")
public class UserRequestTest extends BaseTest {

    private LoginPage loginPage;
    private RequestPage requestPage;

    @BeforeEach
    public void setup() {
        super.setup();
        loginPage = new LoginPage(driver);
        requestPage = new RequestPage(driver);
    }

    @Test
    @Description("Verify new request page loads and contains the form")
    public void testOpenNewRequestPage() {
        loginPage.open(BASE_URL);
        loginPage.login(TestData.USER_EMAIL, TestData.USER_PASSWORD);
        
        // Verify login succeeded before attempting to navigate to new request page
        String afterLoginUrl = driver.getCurrentUrl();
        assertTrue(!afterLoginUrl.contains("/login"), 
                "Should have been redirected from login, but was: " + afterLoginUrl);

        requestPage.openNewRequest(BASE_URL);
        assertTrue(requestPage.isSuccessBannerDisplayed() == false,
                "No success banner should be shown on page load");
        assertTrue(driver.getCurrentUrl().contains("/requests/new"),
                "URL should contain /requests/new");
    }

    @Test
    @Description("Submit a valid new request and confirm success banner")
    public void testSubmitValidRequestShowsSuccess() {
        loginPage.open(BASE_URL);
        loginPage.login(TestData.USER_EMAIL, TestData.USER_PASSWORD);

        requestPage.openNewRequest(BASE_URL);
        String title = TestData.SAMPLE_REQUEST_TITLE + " " + System.currentTimeMillis();
        requestPage.submitRequest(title,
                TestData.REQUEST_CATEGORY_IT,
                TestData.PRIORITY_LOW,
                TestData.SAMPLE_REQUEST_DESCRIPTION);

        // the app sets a success attribute on the page when the request is created
        if (!requestPage.isSuccessBannerDisplayed()) {
            String errorText = requestPage.isErrorBannerDisplayed()
                    ? requestPage.getErrorBannerText() : "(none)";
            fail("Expected success banner after submission but none found. errorBanner="
                    + errorText + " currentUrl=" + driver.getCurrentUrl());
        }
        assertTrue(requestPage.getSuccessBannerText().toLowerCase().contains("request"),
                "Banner text should mention request submission");
    }

    @Test
    @Description("Submitting an empty request form keeps user on new request page")
    public void testEmptyFormKeepsOnPage() {
        loginPage.open(BASE_URL);
        loginPage.login(TestData.USER_EMAIL, TestData.USER_PASSWORD);

        requestPage.openNewRequest(BASE_URL);
        requestPage.clickSubmit();
        assertTrue(driver.getCurrentUrl().contains("/requests/new"),
                "Should remain on /requests/new after empty submit");
    }

    @Test
    @Description("After creating a request, it appears in the list")
    public void testCreatedRequestAppearsInList() {
        loginPage.open(BASE_URL);
        loginPage.login(TestData.USER_EMAIL, TestData.USER_PASSWORD);

        // create request
        requestPage.openNewRequest(BASE_URL);
        String title = TestData.SAMPLE_REQUEST_TITLE + " " + System.currentTimeMillis();
        requestPage.submitRequest(title,
                TestData.REQUEST_CATEGORY_IT,
                TestData.PRIORITY_LOW,
                TestData.SAMPLE_REQUEST_DESCRIPTION);
        assertTrue(requestPage.isSuccessBannerDisplayed());

        // go to list
        requestPage.openListPage(BASE_URL);
        assertTrue(driver.getCurrentUrl().contains("/requests"),
                "URL should contain /requests when viewing list");
        int rows = requestPage.countTableRows();
        assertTrue(rows > 0, "There should be at least one row in the requests table");
        assertTrue(requestPage.getFirstRowTitle().contains(TestData.SAMPLE_REQUEST_TITLE),
                "The first row title should match the request just created");
    }
}
