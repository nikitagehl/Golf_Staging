package com.nikitagehlod.pages;

import com.nikitagehlod.Base.BasePage;
import com.nikitagehlod.utils.propertyReader;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Random;

public class RabbitHolePage extends BasePage {

    private static final By FILTER_COLUMNS = By.xpath("//div[@class='col filter-column']");
    private static final By TABLE_ROWS = By.xpath("//table//tbody/tr");
    private static final By NO_DATA = By.xpath("//h5[contains(text(),'No records found')]");
    private static final By APPLY_BTN = By.xpath("//button[normalize-space()='Apply']");
    private static final By PLUS_ICON =
            By.xpath(".//img[@alt='Add Column to your custom report']");

    @FindBy(xpath = "//div[@class='btn btn-bar d-flex position-relative align-items-center']")
    private WebElement filterButton;

    private final By createModelButton = By.xpath("//a[text()='Create Model']");

    private final Random random = new Random();

    public RabbitHolePage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public boolean isRabbitHoleDisplayed() {
        return waitForVisibility(createModelButton).isDisplayed();
    }

    /** Opens the Rabbit Hole page using the base URL from data.properties (no hardcoded host). */
    public void navigateToRabbitHole() {
        String base = propertyReader.readKey("URL").trim();
        if (!base.endsWith("/")) {
            base += "/";
        }
        driver.get(base + "the-rabbit-hole");
        waitForPageReady(); // reloads once if the site's spinner is stuck
    }

    // ------------------------------------------------------------- Filter panel

    /**
     * Opens the filter panel ONLY if it is not already open.
     * The old clickFilterButton() always clicked, so when the panel was already open
     * the click closed it and the next step failed (this was one cause of the flaky
     * "Close" locator error).
     */
    public void openFilterPanel() {
        waitForLoader();

        if (isDisplayedNow(FILTER_COLUMNS)) {
            return;
        }

        wait.until(ExpectedConditions.elementToBeClickable(filterButton)).click();
        wait.until(d -> isDisplayedNow(FILTER_COLUMNS));
    }

    public void clickApply() {

        // remember a row of the CURRENT table so we can tell when the new result replaces it
        WebElement oldFirstRow = driver.findElements(TABLE_ROWS).stream().findFirst().orElse(null);

        safeClick(waitForClickable(APPLY_BTN));
        waitForLoaderCycle();

        if (oldFirstRow != null) {
            try {
                new WebDriverWait(driver, Duration.ofSeconds(3))
                        .until(ExpectedConditions.stalenessOf(oldFirstRow));
            } catch (TimeoutException ignored) {
                // table was updated in place (same row elements) - nothing more to wait for
            }
        }
    }

    // ------------------------------------------------------------------- Table

    public boolean isNoDataFound() {
        return isDisplayedNow(NO_DATA);
    }

    /**
     * Waits until the table shows rows OR the "No records found" message, then reports
     * whether data is available. Call it ONCE per Apply and store the result.
     */
    public boolean verifyTableLoaded() {

        new WebDriverWait(driver, Duration.ofSeconds(45))
                .until(d -> !d.findElements(TABLE_ROWS).isEmpty() || isDisplayedNow(NO_DATA));

        if (isNoDataFound()) {
            System.out.println("No records available.");
            return false;
        }

        System.out.println("Table loaded successfully.");
        return true;
    }

    // --------------------------------------------------------------- Dropdowns

    public void selectRandomDropdownOption(String dropdownLabel) {

        waitForLoader();

        WebElement dropdown = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//label[normalize-space()='" + dropdownLabel
                                + "']/following::button[1]")));

        safeClick(dropdown);

        By optionLocator = By.xpath(
                "//div[contains(@class,'dropdown-menu') and contains(@class,'show')]//button");

        wait.until(ExpectedConditions.visibilityOfElementLocated(optionLocator));

        List<WebElement> options = driver.findElements(optionLocator);
        WebElement option = options.get(random.nextInt(options.size()));

        scrollIntoView(option);
        wait.until(ExpectedConditions.elementToBeClickable(option));

        System.out.println("Selected Option (" + dropdownLabel + ") : " + option.getText());

        new Actions(driver)
                .moveToElement(option)
                .pause(Duration.ofMillis(300))
                .click()
                .perform();

        waitForLoaderCycle();
        scrollToTop();
    }

    // ----------------------------------------------------------- Add column flow

    public String clickRandomPlus() {

        List<WebElement> headers = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.xpath("//table//thead//th[.//img[@alt='Add Column to your custom report']]")));

        WebElement header = headers.get(random.nextInt(headers.size()));

        scrollIntoView(header);
        String headerName = header.getText().trim();

        WebElement plus = header.findElements(PLUS_ICON).stream()
                .filter(WebElement::isDisplayed)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "No visible '+' icon found in column: " + headerName));

        safeClick(plus);

        return headerName;
    }

    public void enterDescription(String description) {
        type(By.xpath("//textarea[@placeholder='Enter Description']"), description);
    }

    public void clickAdd() {
        WebElement addButton = waitForVisibility(By.xpath("//button[normalize-space()='ADD']"));

        safeClick(addButton);

        scrollToTop();
    }

    /** Call ONCE: the toast disappears after a few seconds, a second call can time out. */
    public boolean verifyToast() {
        return waitForVisibility(
                By.xpath("//*[contains(text(),'Your column settings has been saved')]"))
                .isDisplayed();
    }
}
