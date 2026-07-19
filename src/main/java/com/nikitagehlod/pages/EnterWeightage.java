package com.nikitagehlod.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class EnterWeightage {

    WebDriver driver;


    public EnterWeightage(WebDriver driver){
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }



    public void enterWeightage()  {


         List<WebElement> weightInputs=   driver.findElements(By.xpath
                ("//div[@class='weight-inp-box']//input"));

        System.out.println(weightInputs.size());

        int totalFields = weightInputs.size();
        int value = 100 / (totalFields-1);
        int remainder = 100 % (totalFields-1);

        System.out.println(weightInputs);
        for (int i = 0; i < totalFields -1 ; i++) {

            int currentValue = value;

            // Last field gets remaining value
            if (i == totalFields - 1) {
                currentValue += remainder;
            }
            WebElement input = weightInputs.get(i);
            System.out.println(input);
            input.clear();
            input.sendKeys(String.valueOf(currentValue));
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String VerifyTotalWeight(){
        WebElement totalWeight = driver.findElement(
                By.xpath("//input[@readonly and contains(@value,'100.00')]"));

        String total = totalWeight.getAttribute("value");
       return total;
    }





    public void enterDynamicWeightage() {

       List <WebElement> weightFields = driver.findElements(By.xpath("//div[@class='weight-inp-box']//input"));

        WebElement totalWeightField = driver.findElement(By.xpath("//input[@readonly and contains(@class,'text-danger')]"));
        // Wait until all editable input fields are visible
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.visibilityOfAllElements(weightFields));

        int fieldCount = weightFields.size() -1;

        if (fieldCount == 0) {
            throw new RuntimeException("No editable weight fields found.");
        }

        int baseValue = 100 / fieldCount;
        int remainder = 100 % fieldCount;

        for (int i = 0; i < fieldCount; i++) {

            int value = baseValue;

            // Add remaining value to last field
            if (i == fieldCount - 1) {
                value += remainder;
            }

            WebElement input = weightFields.get(i);

            input.click();
            input.sendKeys(Keys.CONTROL + "a");
            input.sendKeys(Keys.DELETE);
            input.sendKeys(String.valueOf(value));
        }

        // Wait until total updates to 100.00
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.attributeToBe(
                        totalWeightField, "value", "100.00"));
    }
}
