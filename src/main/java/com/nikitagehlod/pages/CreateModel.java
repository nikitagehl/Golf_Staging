package com.nikitagehlod.pages;

import com.nikitagehlod.Base.BasePage;
import io.qameta.allure.Description;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public class CreateModel extends BasePage {

    private final By createModelBtn =
            By.xpath("//a[contains(@href,'custom-ranking-report-setting')]");

    private final By modelName = By.xpath("//input[@placeholder='Model Name']");

    private final By generateModelBtn =
            By.xpath("//button[normalize-space()='Save and Generate Model']");

    private final By removeIcon = By.cssSelector("div.remove-icon");

    public CreateModel(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    /** Clicks "Create Model" on the RH page and switches to the tab it opens. */
    @Description("Verify Create button is visible and clickable")
    public void clickCreateModel() {

        waitForLoader();

        WebElement button = waitForClickable(createModelBtn);

        // capture the open tabs BEFORE the click, so we can find the NEW one afterwards
        Set<String> handlesBefore = driver.getWindowHandles();

        safeClick(button);

        switchToNewWindow(handlesBefore);
    }

    /** The Create Model page loads a lot of data; on devbeta it sometimes needs more than 15s. */
    private static final int MODEL_PAGE_TIMEOUT_SECONDS = 30;

    /**
     * Waits for the Create Model page (new tab) to be ready: loader gone + "Model Name" box visible.
     * If it is still not there after 30s, the tab is refreshed ONCE and we wait again
     * (a slow / half-loaded tab was the cause of the TimeoutException in DeleteReport).
     */
    @Description("Verify Create button should open the create model page.")
    public void waitForModelPage() {

        try {
            waitForModelNameBox();
        } catch (TimeoutException firstTry) {
            System.out.println("Create Model page not ready after " + MODEL_PAGE_TIMEOUT_SECONDS
                    + "s (" + driver.getCurrentUrl() + ") - refreshing the tab once.");
            driver.navigate().refresh();
            waitForModelNameBox();
        }

        ((JavascriptExecutor) driver).executeScript("document.body.style.zoom='70%'");
    }

    private void waitForModelNameBox() {
        waitForLoader();
        new WebDriverWait(driver, Duration.ofSeconds(MODEL_PAGE_TIMEOUT_SECONDS))
                .pollingEvery(Duration.ofMillis(300))
                .until(ExpectedConditions.visibilityOfElementLocated(modelName));
    }

    @Description("Check the Report name input box clickable and enter the report name")
    public void enterModelName(String model) {
        type(modelName, model);
    }

    /**
     * Clicks "Save and Generate Model". If the app opens the result in a new tab we switch
     * to it, otherwise we stay on the current tab.
     * (Old code waited for "more than 1 tab" which was already true, so it never really
     * waited and could switch to the wrong tab.)
     */
    @Description("Verify generate report clickable and generate the report")
    public void clickGenerateModel() {

        waitForLoader();

        WebElement button = waitForVisibility(generateModelBtn);

        Set<String> handlesBefore = driver.getWindowHandles();
        String urlBefore = driver.getCurrentUrl();

        safeClick(button);

        try {
            new WebDriverWait(driver, Duration.ofSeconds(15)).until(d ->
                    d.getWindowHandles().size() > handlesBefore.size()
                            || !d.getCurrentUrl().equals(urlBefore));
        } catch (TimeoutException e) {
            System.out.println("No new tab / page change after 'Save and Generate Model'.");
        }

        if (driver.getWindowHandles().size() > handlesBefore.size()) {
            switchToNewWindow(handlesBefore);
        }
    }

    /** Removes the first saved column. Returns true when the number of columns went down. */
    @Description("Check the remove icon delete the saved column")
    public boolean clickFirstRemoveIcon() {

        List<WebElement> icons = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(removeIcon));

        int before = icons.size();

        safeClick(icons.get(0));

        boolean removed = true;
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(d -> d.findElements(removeIcon).size() < before);
        } catch (TimeoutException e) {
            removed = false;
            System.out.println("Column count did not change after clicking remove.");
        }

        waitForLoader();
        return removed;
    }
}
