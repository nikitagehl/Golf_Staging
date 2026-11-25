package com.nikitagehlod.Base;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class BasePage {

    private static final By LOADER = By.cssSelector(".cmn-loader");

    protected WebDriver driver;
    protected WebDriverWait wait;

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeoutSeconds()));
    }
    private static final Duration LOADER_TIMEOUT = Duration.ofSeconds(45);

    /** Standard explicit-wait time: "wait.timeoutSeconds" in data.properties (10 if missing). */
    public static long defaultTimeoutSeconds() {
        String v = com.nikitagehlod.utils.propertyReader.readKey("wait.timeoutSeconds");
        try {
            return v == null ? 10 : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    // ---------------------------------------------------------------- Click

    public void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    public void click(WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element)).click();
    }

    // ----------------------------------------------------------------- Type

    public void type(By locator, String value) {
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        element.clear();
        element.sendKeys(value);
    }

    public void type(WebElement element, String value) {
        wait.until(ExpectedConditions.visibilityOf(element));
        element.clear();
        element.sendKeys(value);
    }

    // -------------------------------------------------------------- Get text

    public String getText(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).getText();
    }

    // ----------------------------------------------------------------- Waits

    public WebElement waitForVisibility(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForPresence(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Waits for the FIRST DISPLAYED element that matches the locator.
     * Useful when the same locator also matches hidden elements elsewhere on the page
     * (findElement would return the hidden one).
     */
    public WebElement waitForFirstDisplayed(By locator) {
        return wait.until(d -> {
            for (WebElement e : d.findElements(locator)) {
                try {
                    if (e.isDisplayed()) {
                        return e;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // element re-rendered while we looked at it - try again on next poll
                }
            }
            return null;
        });
    }

    /** Immediate check (no waiting): is at least one matching element displayed right now? */
    public boolean isDisplayedNow(By locator) {
        for (WebElement e : driver.findElements(locator)) {
            try {
                if (e.isDisplayed()) {
                    return true;
                }
            } catch (StaleElementReferenceException ignored) {
                // ignore and check the next one
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- Loader

    /** Waits until the loader is gone (passes immediately if it is not on the page). */
    public void waitForLoader() {
        try {
            new WebDriverWait(driver, LOADER_TIMEOUT)
                    .until(ExpectedConditions.invisibilityOfElementLocated(LOADER));
        } catch (TimeoutException e) {
            throw new TimeoutException(loaderStuckMessage(LOADER_TIMEOUT.getSeconds()), e);
        }
    }

    /** First wait before a stuck page is reloaded (see waitForPageReady). */
    private static final Duration PAGE_LOADER_FIRST_TRY = Duration.ofSeconds(30);

    /**
     * Use right after OPENING a page (driver.get / clicking a link to another page).
     * The site shows its spinner (.cmn-loader) until its server calls answer, with no time limit.
     * When devbeta's server is slow, a call can hang and the spinner never goes away - so if it is
     * still there after 30s, the page is reloaded ONCE and we wait again (up to 45s).
     * Do NOT use it in the middle of a flow (e.g. after Apply): a reload would lose the user's input.
     */
    public void waitForPageReady() {
        try {
            new WebDriverWait(driver, PAGE_LOADER_FIRST_TRY)
                    .until(ExpectedConditions.invisibilityOfElementLocated(LOADER));
        } catch (TimeoutException firstTry) {
            System.out.println("Spinner still showing after " + PAGE_LOADER_FIRST_TRY.getSeconds()
                    + "s on " + driver.getCurrentUrl() + " - reloading the page once. "
                    + slowRequestsInfo());
            driver.navigate().refresh();
            waitForLoader();
        }
    }

    /**
     * Max wait for page CONTENT that the site loads from its server without a spinner
     * (e.g. the home page articles). "page.timeoutSeconds" in data.properties, default 45.
     * devbeta's /proxy API calls sometimes take ~20s each.
     */
    public static long pageContentTimeoutSeconds() {
        String v = com.nikitagehlod.utils.propertyReader.readKey("page.timeoutSeconds");
        try {
            return v == null ? 45 : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return 45;
        }
    }

    /**
     * Waits for a piece of page content; if it is not there in time, reloads the page ONCE and waits
     * again. The final error says it was site slowness and lists the slowest server requests.
     */
    public WebElement waitForPageContent(By locator, String whatIsIt) {
        Duration timeout = Duration.ofSeconds(pageContentTimeoutSeconds());
        try {
            return new WebDriverWait(driver, timeout).pollingEvery(Duration.ofMillis(300))
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException firstTry) {
            System.out.println(whatIsIt + " not shown after " + timeout.getSeconds() + "s on "
                    + driver.getCurrentUrl() + " - reloading once. " + slowRequestsInfo());
            driver.navigate().refresh();
            waitForPageReady();
            try {
                return new WebDriverWait(driver, timeout).pollingEvery(Duration.ofMillis(300))
                        .until(ExpectedConditions.visibilityOfElementLocated(locator));
            } catch (TimeoutException secondTry) {
                throw new TimeoutException(whatIsIt + " was still not shown after a reload ("
                        + timeout.getSeconds() + "s each try) on " + driver.getCurrentUrl()
                        + ". devbeta's server is answering slowly - site slowness, not a locator problem. "
                        + slowRequestsInfo(), secondTry);
            }
        }
    }

    /** Readable error for a stuck spinner, including the slowest server requests (for the dev team). */
    private String loaderStuckMessage(long seconds) {
        return "The site's loading spinner (.cmn-loader) was still showing after " + seconds + "s on "
                + driver.getCurrentUrl() + ". devbeta's server did not answer in time - this is site "
                + "slowness, not a locator problem. " + slowRequestsInfo();
    }

    /** The 5 slowest finished requests of the current page (only requests that took over 3s). */
    private String slowRequestsInfo() {
        try {
            Object info = ((JavascriptExecutor) driver).executeScript(
                    "return performance.getEntriesByType('resource')"
                            + ".filter(function(r){return r.duration > 3000;})"
                            + ".sort(function(a,b){return b.duration - a.duration;}).slice(0,5)"
                            + ".map(function(r){return Math.round(r.duration/1000) + 's ' + r.name.split('?')[0];})"
                            + ".join(' | ');");
            String text = String.valueOf(info);
            return text.isBlank() ? "No finished request took over 3s (a request may still be pending)."
                    : "Slow requests: " + text;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Use right AFTER an action that triggers a server call (Apply, dropdown select ...).
     * Gives the loader a short moment to appear first, then waits for it to disappear.
     * Plain waitForLoader() can return too early because the loader has not shown up yet.
     */
    public void waitForLoaderCycle() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(1))
                    .pollingEvery(Duration.ofMillis(100))
                    .until(ExpectedConditions.visibilityOfElementLocated(LOADER));
        } catch (TimeoutException ignored) {
            // loader was too quick to catch, or no request was made
        }
        waitForLoader();
    }

    // ---------------------------------------------------------------- Scroll

    public void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

    public void scrollToTop() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0,0)");
    }

    public void scrollToBottom() {
        ((JavascriptExecutor) driver)
                .executeScript("window.scrollTo(0,document.body.scrollHeight)");
    }

    // ----------------------------------------------------------- JS / safe click

    public void jsClick(WebElement element) {
        // JS clicks bypass the WebDriver listener, so show them on screen here
        com.nikitagehlod.utils.ClickHighlighter.showClick(driver, element);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    public void safeClick(WebElement element) {
        scrollIntoView(element);
        wait.until(ExpectedConditions.elementToBeClickable(element));
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            jsClick(element);
        }
    }

    // -------------------------------------------------------------- Elements

    public List<WebElement> getElements(By locator) {
        return driver.findElements(locator);
    }

    public WebElement getElement(By locator) {
        return driver.findElement(locator);
    }

    public boolean isElementPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    // --------------------------------------------------------- Tabs / windows

    /**
     * Switches to the tab that was opened after 'handlesBefore' was captured.
     * Capture the handles BEFORE the click that opens the tab:
     *     Set<String> before = driver.getWindowHandles();
     *     click(...);
     *     switchToNewWindow(before);
     */
    public String switchToNewWindow(Set<String> handlesBefore) {
        wait.until(d -> d.getWindowHandles().size() > handlesBefore.size());
        for (String handle : driver.getWindowHandles()) {
            if (!handlesBefore.contains(handle)) {
                driver.switchTo().window(handle);
                return handle;
            }
        }
        throw new NoSuchWindowException("No new browser tab was found.");
    }

    /** Closes every tab except 'keepHandle' and switches back to it. */
    public void closeAllWindowsExcept(String keepHandle) {
        for (String handle : new ArrayList<>(driver.getWindowHandles())) {
            if (!handle.equals(keepHandle)) {
                driver.switchTo().window(handle);
                driver.close();
            }
        }
        driver.switchTo().window(keepHandle);
    }
}
