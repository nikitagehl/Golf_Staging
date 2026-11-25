package com.nikitagehlod.basetest;

import com.nikitagehlod.driver.driverManager;
import com.nikitagehlod.utils.propertyReader;
import listeners.TestListener;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;

/**
 * A FRESH browser for every test class: opened before the class, closed after it.
 * Every class starts clean (logged out, one tab), so a failure in one class cannot
 * break the next one, and any class can also be run on its own.
 */
@Listeners(TestListener.class)
public class commonToAllTest {

    protected WebDriver driver;

    @BeforeClass(alwaysRun = true)
    public void setup() {

        // One place creates the browser, so TestListener can also reach it for screenshots.
        driverManager.init();
        driver = driverManager.getDriver();

        // NOTE: no implicit wait on purpose. Implicit and explicit waits must not be mixed;
        // every wait in the page objects is an explicit WebDriverWait.

        // If devbeta is very slow this first load can time out. That must not fail the whole class:
        // every test class opens its own page (and waits for it properly) right after this.
        try {
            driver.get(propertyReader.readKey("URL").trim());
            ((JavascriptExecutor) driver).executeScript("document.body.style.zoom='80%'");
        } catch (Exception slowFirstLoad) {
            System.out.println("First page load in setup was slow / failed (" + slowFirstLoad.getClass().getSimpleName()
                    + ") - continuing, the test class opens its own page next.");
        }
    }

    /**
     * Opens the URL as a logged-out guest: clears cookies + local/session storage, then reloads.
     * Use it in @BeforeMethod so one test cannot leave the next one logged in.
     */
    protected void openAsGuest(String url) {
        driver.manage().deleteAllCookies();
        driver.get(url);
        try {
            // Reload only when a login was actually stored - saves one full page load per test.
            Object wasLoggedIn = ((JavascriptExecutor) driver).executeScript(
                    "var had = !!(localStorage.getItem('session_key') || localStorage.getItem('user_profile'));"
                            + " localStorage.clear(); sessionStorage.clear(); return had;");
            if (Boolean.TRUE.equals(wasLoggedIn)) {
                driver.navigate().refresh();
            }
        } catch (Exception ignored) {
            // storage not available on this page - cookies were already cleared
        }
    }

    /** Closes the browser after each test class. */
    @AfterClass(alwaysRun = true)
    public void tearDown() {
        driverManager.down();
    }
}
