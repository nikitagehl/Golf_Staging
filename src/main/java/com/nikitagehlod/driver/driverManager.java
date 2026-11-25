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

public class driverManager {

    private static WebDriver driver;

    public static WebDriver getDriver() {
        return driver;
    }

    public static void setDriver(WebDriver driver) {
        driverManager.driver = driver;
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
                driver = new EdgeDriver(edgeOptions);
                break;

            case "chrome":
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--start-maximized");
                // EAGER: driver.get() returns once the page HTML is ready, without waiting for
                // ads / analytics / newsletter iframes. Our explicit waits handle the rest.
                chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
                driver = new ChromeDriver(chromeOptions);
                break;

            case "firefox":
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                firefoxOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
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
