package com.nikitagehlod.Base;

import static com.nikitagehlod.driver.driverManager.getDriver;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class BasePage {

    protected WebDriver driver;
    protected WebDriverWait wait;

    public BasePage(WebDriver driver) {

        this.driver = driver;

        wait = new WebDriverWait(driver,
                Duration.ofSeconds(10));
    }

    // Click

    public void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    public void click(WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element)).click();
    }

    // Type

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

    // Get Text

    public String getText(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).getText();
    }

    // Waits

    public WebElement waitForVisibility(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForPresence(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    // Loader

    public void waitForLoader() {

        By loader = By.cssSelector(".cmn-loader");

        wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
    }

    // Scroll

    public void scrollIntoView(WebElement element) {

        ((JavascriptExecutor) driver)
                .executeScript(
                        "arguments[0].scrollIntoView({block:'center'});",
                        element);
    }

    public void scrollToTop() {

        ((JavascriptExecutor) driver)
                .executeScript("window.scrollTo(0,0)");
    }

    public void scrollToBottom() {

        ((JavascriptExecutor) driver)
                .executeScript("window.scrollTo(0,document.body.scrollHeight)");
    }

    // JavaScript Click

    public void jsClick(WebElement element) {

        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", element);
    }

    // Safe Click

    public void safeClick(WebElement element) {

        scrollIntoView(element);

        wait.until(ExpectedConditions.elementToBeClickable(element));

        try {

            element.click();

        } catch (ElementClickInterceptedException e) {

            jsClick(element);
        }
    }

    // Elements

    public List<WebElement> getElements(By locator) {

        return driver.findElements(locator);
    }

    public WebElement getElement(By locator) {

        return driver.findElement(locator);
    }

    // Exists

    public boolean isElementPresent(By locator) {

        return !driver.findElements(locator).isEmpty();
    }

}