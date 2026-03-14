package com.amalitech.qa.base;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
/**
 * Provides shared test setup utilities.
 */

public class BaseTest {

    protected WebDriver driver;
    protected String BASE_URL;

    /**
     * Sets up ChromeDriver before each test.
     * Loads BASE_URL from .env file.
     * Uses headless mode for faster execution.
     */
    @BeforeEach
    public void setup() {
        Dotenv dotenv = Dotenv.configure().directory("../../").load();
        BASE_URL = dotenv.get("BASE_URL");

        // quick connectivity check so that tests fail with a clear message if the
        // backend is not running at the expected address.  retry for a short period
        // in case the server is still starting up when Maven invokes the suite.
        boolean reachable = false;
        long deadline = System.currentTimeMillis() + 30_000; // 30 seconds max
        Exception lastEx = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                java.net.URL url = new java.net.URL(BASE_URL);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();
                // any response (401/200/etc) means server is up
                if (code >= 200 && code < 600) {
                    reachable = true;
                    break;
                }
            } catch (Exception e) {
                lastEx = e;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (!reachable) {
            throw new RuntimeException("Cannot reach backend at " + BASE_URL + ". " +
                    "Please start the ServiceHub application before running UI tests.", lastEx);
        }

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        // run in headless mode so the browser does not display on screen
        // keep the newer headless implementation explicitly as well
       options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
    }

    /**
     * Quits the browser after each test.
     * Ensures no browser instances are left open between tests.
     */
    @AfterEach
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
