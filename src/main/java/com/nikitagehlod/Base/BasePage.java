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
//        wait.until(ExpectedConditions.invisibilityOfElementLocated(LOADER));
        new WebDriverWait(driver, LOADER_TIMEOUT)
                .until(ExpectedConditions.invisibilityOfElementLocated(LOADER));
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
