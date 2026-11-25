package com.nikitagehlod.driver;

import com.nikitagehlod.utils.ClickHighlighter;
import com.nikitagehlod.utils.propertyReader;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.events.EventFiringDecorator;

import java.util.HashMap;
import java.util.Map;

public class driverManager {

    private static WebDriver driver;

    public static WebDriver getDriver() {
        return driver;
    }

    public static void setDriver(WebDriver driver) {
        driverManager.driver = driver;
    }

    /** Turns off Chrome/Edge password leak detection ("Change your password" popup). */
    private static final String DISABLE_PASSWORD_FEATURES =
            "--disable-features=PasswordLeakDetection,PasswordCheck,PasswordManagerOnboarding";

    /** Browser preferences that stop the Password Manager from offering to save or check passwords. */
    private static Map<String, Object> noPasswordPopupPrefs() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);              // no "Save password?" bubble
        prefs.put("profile.password_manager_enabled", false);        // password manager off
        prefs.put("profile.password_manager_leak_detection", false); // no "found in a data breach" popup
        return prefs;
    }

    /** Starts the browser configured by the "browser" key in data.properties. */
    public static void init() {

        if (driver != null) {
            return;
        }

        String browser = propertyReader.readKey("browser").trim().toLowerCase();

        switch (browser) {
            case "edge":
                EdgeOptions edgeOptions = new EdgeOptions();
                edgeOptions.addArguments("--start-maximized");
                edgeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
                edgeOptions.setExperimentalOption("prefs", noPasswordPopupPrefs());
                edgeOptions.addArguments(DISABLE_PASSWORD_FEATURES);
                driver = new EdgeDriver(edgeOptions);
                break;

            case "chrome":
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--start-maximized");
                // EAGER: driver.get() returns once the page HTML is ready, without waiting for
                // ads / analytics / newsletter iframes. Our explicit waits handle the rest.
                chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
                // No "Change your password" / "Save password?" popups from Chrome's Password Manager:
                // they are browser windows (not part of the page), Selenium cannot click them and
                // they can block the test.
                chromeOptions.setExperimentalOption("prefs", noPasswordPopupPrefs());
                chromeOptions.addArguments(DISABLE_PASSWORD_FEATURES);
                driver = new ChromeDriver(chromeOptions);
                break;

            case "firefox":
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                firefoxOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
                firefoxOptions.addPreference("signon.rememberSignons", false);
                firefoxOptions.addPreference("signon.management.page.breach-alerts.enabled", false);
                driver = new FirefoxDriver(firefoxOptions);
                break;

            default:
                throw new IllegalArgumentException(
                        "Unsupported browser in data.properties: '" + browser
                                + "' (use chrome, edge or firefox)");
        }

        // Show every click / typing step on screen (show.clicks=true in data.properties).
        if (ClickHighlighter.isEnabled()) {
            WebDriver raw = driver;
            driver = new EventFiringDecorator<WebDriver>(
                    new ClickHighlighter(raw, ClickHighlighter.configuredDelayMs())).decorate(raw);
        }

        driver.manage().window().maximize();
    }

    /** Closes the browser. */
    public static void down() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }
}
