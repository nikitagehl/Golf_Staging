package com.nikitagehlod.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Set;

public class createModel {


    WebDriver driver;
    WebDriverWait wait;

    public createModel(WebDriver driver) {

        this.driver = driver;
        wait = new WebDriverWait(driver,
                Duration.ofSeconds(20));
        PageFactory.initElements(driver, this);
    }



    public void setCreateModelBtn() {
        By loader = By.cssSelector(".cmn-loader");

        String parentTab = driver.getWindowHandle();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));

        WebElement createModelBtn = driver.findElement(
                By.xpath("//a[contains(@href,'custom-ranking-report-setting')]"));



// Scroll until Create Model button is in the center of the screen


        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(createModelBtn));



        createModelBtn.click();




        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(driver -> driver.getWindowHandles().size() > 1);
        Set<String> allTabs = driver.getWindowHandles();

        for (String tab : allTabs) {
            if (!tab.equals(parentTab)) {
                driver.switchTo().window(tab);
                break;
            }
        }

    }
    public void navigateToSavedModalPage() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[@placeholder='Model Name']")));

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("document.body.style.zoom='70%'");

    }

    public void enterModelName(){

        System.out.println(driver.getCurrentUrl());
        System.out.println(driver.getTitle());

//        System.out.println(modelName);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement modelNamexpath = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//input[@placeholder='Model Name']")));

        modelNamexpath.sendKeys("Automation report 1");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void clickGenerateModel(){
//

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        WebElement addButton = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//button[normalize-space()='Save and Generate Model']")));

        JavascriptExecutor js = (JavascriptExecutor) driver;

// Scroll button into center
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", addButton);

// Wait for loader to disappear (if present)
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector(".cmn-loader")));

// Wait until button is clickable
        wait.until(ExpectedConditions.elementToBeClickable(addButton));

        try {
            addButton.click();
        } catch (ElementClickInterceptedException e) {
            js.executeScript("arguments[0].click();", addButton);
        }

// Wait until clickable
//        new WebDriverWait(driver, Duration.ofSeconds(10))
//                .until(ExpectedConditions.elementToBeClickable(addButton));
//        while (!addButton.isDisplayed()) {
//            js.executeScript("window.scrollBy(0, 300);");
//        }
        String parentTab = driver.getWindowHandle();


        wait.until(driver -> driver.getWindowHandles().size() > 1);
        Set<String> allTabs = driver.getWindowHandles();

        for (String tab : allTabs) {
            if (!tab.equals(parentTab)) {
                driver.switchTo().window(tab);
                break;
            }
        }
        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Click button");


    }


}
