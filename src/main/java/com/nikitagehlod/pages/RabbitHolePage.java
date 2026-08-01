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



    public void clickFilterButton() {
        waitForLoader();
        wait.until(
                        ExpectedConditions.elementToBeClickable(
                                filterButton))
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
//        waitForLoader();
        scrollToTop();
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
