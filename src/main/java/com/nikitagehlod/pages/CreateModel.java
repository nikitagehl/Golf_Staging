package com.nikitagehlod.pages;

import com.nikitagehlod.Base.BasePage;
import io.qameta.allure.Description;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Set;




public class CreateModel extends BasePage {

    WebDriver driver;

    public CreateModel(WebDriver driver) {
        super(driver);
        this.driver = driver;

        PageFactory.initElements(driver, this);
    }

    // Locators

    private final By createModelBtn =
            By.xpath("//a[contains(@href,'custom-ranking-report-setting')]");

    private final By modelName =
            By.xpath("//input[@placeholder='Model Name']");

    private final By generateModelBtn =
            By.xpath("//button[normalize-space()='Save and Generate Model']");

    // Click Create Model

    @Description("Verify Create button is visible and clickable ")

    public void clickCreateModel() {

        waitForLoader();

        String parentTab = driver.getWindowHandle();

        WebElement button = waitForClickable(createModelBtn);

        safeClick(button);

        wait.until(driver -> driver.getWindowHandles().size() > 1);

        switchToNewTab(parentTab);
    }

    // Wait until page loads

    @Description("Verify Create button should open the createmodel page.")
    public void waitForModelPage() {

        waitForVisibility(modelName);

        ((JavascriptExecutor) driver)
                .executeScript("document.body.style.zoom='70%'");
    }

    // Enter Model Name


    @Description("Check the Report name input box clickable and enter the report name")

    public void enterModelName(String model) {

        type(modelName, model);
    }

    // Save and Generate



      @Description("Verify generate report clickable and generate the report ")
    public void clickGenerateModel() {

        waitForLoader();

        WebElement button = waitForVisibility(generateModelBtn);

        safeClick(button);

        String parentTab = driver.getWindowHandle();

        wait.until(driver -> driver.getWindowHandles().size() > 1);

        switchToNewTab(parentTab);
    }

    // Reusable Tab Switch

    private void switchToNewTab(String parentTab) {

        Set<String> tabs = driver.getWindowHandles();

        for (String tab : tabs) {

            if (!tab.equals(parentTab)) {

                driver.switchTo().window(tab);

                break;
            }
        }
    }

    public void delete_one(String ColumnName) {
        By removeIcon = By.xpath(
                "//tr[td[contains(.,+)]]//div[contains(@class,'remove-icon')]");

        click(removeIcon);

    }


    private By removeIcon = By.cssSelector("div.remove-icon");

    @Description("Check the remove icon delete the saved column")
        public void clickFirstRemoveIcon() {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            List<WebElement> icons = wait.until(
                    ExpectedConditions.visibilityOfAllElementsLocatedBy(removeIcon));

            icons.get(0).click();

    }

}