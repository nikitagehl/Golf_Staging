package com.nikitagehlod.pages;

import com.nikitagehlod.Base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


import java.time.Duration;
import java.util.List;
import java.util.Random;

public class ManageReports extends BasePage {

    WebDriver driver;
    WebDriverWait wait;

    public ManageReports(WebDriver driver){
        super(driver);
        this.driver= driver;
        wait = new WebDriverWait(driver,
                Duration.ofSeconds(10));

        PageFactory.initElements(driver, this);
    }


    public void goToModelsTable(){
        WebElement manageModels = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//div[normalize-space()='Manage Models']")));

// Scroll to it
        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript(
                "arguments[0].scrollIntoView({behavior:'instant', block:'start'});",
                manageModels);

// Wait a little for scroll animation/layout

    }
    private List<WebElement> getReportRows() {

        goToModelsTable();

        By rowLocator = By.xpath(
                "//div[normalize-space()='Manage Models']" +
                        "/following-sibling::div[contains(@class,'table-report-setting')]//tbody/tr");

        List<WebElement> rows = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(rowLocator));

        if (rows.isEmpty()) {
            throw new RuntimeException("No reports available.");
        }

        return rows;
    }
    private WebElement getRandomRow() {

        List<WebElement> rows = getReportRows();

        Random random = new Random();

        return rows.get(random.nextInt(rows.size()));
    }
    private String getReportName(WebElement row) {

        return row.findElement(By.xpath("./td[2]")).getText();
    }
    private void clickActionIcon(WebElement row, String iconName) {

        WebElement icon = row.findElement(
                By.xpath(".//img[@alt='" + iconName + "']"));

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                icon);

        wait.until(ExpectedConditions.elementToBeClickable(icon));

        icon.click();
    }

    public void DeleteReport(){
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        goToModelsTable();
//
        WebElement row = getRandomRow();

        System.out.println("Deleting : " + getReportName(row));

        int beforeCount = getReportRows().size();

        clickActionIcon(row, "Remove");

        wait.until(ExpectedConditions.numberOfElementsToBeLessThan(
                By.xpath("//div[normalize-space()='Manage Models']/following-sibling::div[contains(@class,'table-report-setting')]//tbody/tr"),
                beforeCount));

        System.out.println("Report deleted successfully.");
    }
    public void verifyCopySuccessToast() {

        WebElement toast = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//div[@role='status']")));

        System.out.println("Toast : " + toast.getText());

        wait.until(ExpectedConditions.invisibilityOf(toast));
    }

    public void CopyReport(){

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        goToModelsTable();
        WebElement row = getRandomRow();

        System.out.println("Copying : " + getReportName(row));

        clickActionIcon(row, "Copy");

        wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//div[@class='modal-footer']//button[normalize-space()='Yes']")))
                .click();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.xpath("//div[@role='dialog']")));

//        verifyCopySuccessToast();

        waitForLoader();
        System.out.println("Report copied successfully.");

    }



    public void ChangeReportStatus(){
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        goToModelsTable();

        WebElement row = getRandomRow();

        System.out.println("Changing Status : " + getReportName(row));

        WebElement toggle = row.findElement(
                By.xpath(".//label[contains(@class,'switch')]"));

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                toggle);

        wait.until(ExpectedConditions.elementToBeClickable(toggle));

        toggle.click();
    }
}
