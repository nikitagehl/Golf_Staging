package com.nikitagehlod.pages;

import com.nikitagehlod.Base.BasePage;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RabbitHolePage  extends BasePage {
    WebDriver driver;
    WebDriverWait wait;

    public RabbitHolePage( WebDriver driver) {
        super(driver);
       this.driver= driver;

        wait = new WebDriverWait(driver,
                Duration.ofSeconds(10));

        PageFactory.initElements(driver, this);
    }

    @FindBy(xpath = "//div[@class='btn btn-bar d-flex position-relative align-items-center']")
    WebElement filterButton;

    private By createModelButton =
            By.xpath("//a[text()='Create Model']");

    public boolean isRabbitHoleDisplayed(){

        return wait.until(ExpectedConditions.visibilityOfElementLocated(createModelButton))
                .isDisplayed();

    }

    public void navigateToRabbitHole() {

        driver.get(
                "https://devbeta.betspertsgolf.com/the-rabbit-hole");


    }

    public void selectDropdown(String value) {

        By option =
                By.xpath("//*[contains(text(),'" + value + "')]");

        wait.until(
                        ExpectedConditions.elementToBeClickable(option))
                .click();
    }

    public void clickFilterButton() {
        waitForLoader();
        wait.until(
                        ExpectedConditions.elementToBeClickable(
                                filterButton))
                .click();

    }

    public void selectFilter(String filterName) {

        By filter =
                By.xpath("//*[contains(text(),'"
                        + filterName + "')]");
        //input[starts-with(@value, '4')]

        wait.until(
                        ExpectedConditions.elementToBeClickable(filter))
                .click();
    }

    public void selectFilter(String filterTitle, String option) {
        String xpath = "//ul[contains(normalize-space(.),'" + filterTitle + "')]" +
                "//li[.//span[normalize-space()='" + option + "']]" +
                "//input[@type='checkbox']";

        driver.findElement(By.xpath(xpath)).click();
    }

    public List<String> getTableHeaders() {

        List<WebElement> headers =
                driver.findElements(
                        By.xpath("//table//th"));

        return headers.stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    public void clickHeaderPlusIcon(
            String headerName) {

        String xpath =
                "//th[contains(.,'"
                        + headerName +
                        "')]//*[local-name()='svg']";
        WebElement element = driver.findElement(By.xpath("//button[text()='Sign In']"));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView(true);", element);


        wait.until(
                        ExpectedConditions.elementToBeClickable(
                                By.xpath(xpath)))
                .click();
    }

    public void enterDescription(
            String description) {
        type(
                By.xpath("//textarea[@placeholder='Enter Description']"),
                description);

    }

    public void clickAdd() {
        WebElement addButton = waitForVisibility(
                By.xpath("//button[normalize-space()='ADD']"));

        safeClick(addButton);

        scrollToTop();
    }

    public boolean verifyToast() {

        return wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath(
                                        "//*[contains(text(),'Your column settings has been saved')]")))
                .isDisplayed();

    }

    public void clickApply() {
        driver.findElement(
                        By.xpath("//button[normalize-space()='Apply']")
                )
                .click();
        waitForLoader();
    }

    public void selectDropdownOption(String dropdownLabel, String optionText) {

        // Open the required dropdown
        driver.findElement(By.xpath(
                "//label[normalize-space()='" + dropdownLabel + "']/following::button[1]"
        )).click();

        // Click the required option
        driver.findElement(By.xpath(
                "//div[contains(@class,'dropdown-menu') and contains(@class,'show')]//button[normalize-space()='" + optionText + "']"
        )).click();
        waitForLoader();

    }

    public void selectRandomDropdownOption(String dropdownLabel) {

        waitForLoader();

        WebElement dropdown = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//label[normalize-space()='" + dropdownLabel + "']/following::button[1]")
                ));
        scrollIntoView(dropdown);

        dropdown.click();

        // Wait until the dropdown is visible
        By optionLocator = By.xpath(
                "//div[contains(@class,'dropdown-menu') and contains(@class,'show')]//button");

        wait.until(ExpectedConditions.visibilityOfElementLocated(optionLocator));

        // Get all options
        List<WebElement> options = driver.findElements(optionLocator);
        Random random = new Random();
        int index = random.nextInt(options.size());
        WebElement option = options.get(index);

        // Scroll the selected option into view
        scrollIntoView(option);
        wait.until(ExpectedConditions.elementToBeClickable(option));
        System.out.println("Selected Option : " + option.getText());
        Actions actions = new Actions(driver);
        actions.moveToElement(option).pause(Duration.ofMillis(300)).click().perform();
//        option.click();
        waitForLoader();
        scrollToTop();
    }
    public void clickPlusAndGetHeader(String columnName) {

        WebElement header = driver.findElement(By.xpath(
                "//th[contains(.,'" + columnName + "')]"));

        String headerText = header.getText().trim();

        WebElement plus = header.findElement(
                By.xpath(".//img[@alt='Add Column to your custom report']"));
        wait.until(
                        ExpectedConditions.elementToBeClickable(
                                (plus)))
                .click();

        WebElement addButton = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//button[normalize-space()='ADD']")));

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", addButton);
    }

    public String clickRandomPlus() {

        List<WebElement> headers = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.xpath("//table//thead//th[.//img[@alt='Add Column to your custom report']]")));

        Random random = new Random();
        WebElement header = headers.get(random.nextInt(headers.size()));

        String headerName = header.getText().trim();

        List<WebElement> plusIcons = header.findElements(
                By.xpath(".//img[@alt='Add Column to your custom report']"));

        for (WebElement plus : plusIcons) {
            if (plus.isDisplayed()) {
                wait.until(ExpectedConditions.elementToBeClickable(plus)).click();
                break;
            }
        }
        return headerName;
    }




    public List<String> getVisibleHeaders() {

        List<WebElement> elements = driver.findElements(
                By.xpath("//table//thead//th"));

        List<String> headers = new ArrayList<>();

        for (WebElement e : elements) {

            String text = e.getText().trim();

            if (!text.isEmpty()) {
                headers.add(text);
            }
        }

        return headers;
    }



        private By noDataMessage =
                By.xpath("//h5[contains(text(),'No records found')]");

        public boolean isNoDataFound() {

            return !getElements(noDataMessage).isEmpty();
        }


    public boolean verifyTableLoaded() {
         By noDataMessage = By.xpath("//h5[contains(text(),'No records found')]");

        // Table
         By tableRows = By.xpath("//table//tbody/tr");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        wait.until(driver ->
                driver.findElements(tableRows).size() > 0 ||
                        driver.findElements(noDataMessage).size() > 0
        );

        if (isNoDataFound()) {
            System.out.println("No records available.");
            return false;
        }

        System.out.println("Table loaded successfully.");
        return true;
    }




}
