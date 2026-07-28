package com.nikitagehlod.pages;

import com.nikitagehlod.Base.BasePage;
import io.qameta.allure.Description;
import org.openqa.selenium.*;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class EnterWeightage extends BasePage {

    WebDriver driver;


    public EnterWeightage(WebDriver driver){
        super(driver);
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }



 @Description("Verify total weightagae should be 100")
    public String VerifyTotalWeight(){
        WebElement totalWeight = driver.findElement(
                By.xpath("//input[@readonly and contains(@value,'100.00')]"));

        String total = totalWeight.getAttribute("value");
       return total;
    }




@Description("Enter the weight on input field ")
    public void enterDynamicWeightage() {

       List <WebElement> weightFields = driver.findElements(By.
               xpath("//div[@class='weight-inp-box']//input"));


        WebElement totalWeightField = driver.findElement(By.
                xpath("//input[@readonly and contains(@class,'text-danger')]"));
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


//            scrollIntoView(input);
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                    "arguments[0].scrollIntoView({behavior:'instant', block:'center'});",
                    input);

            // Wait until clickable
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(input));

            input.click();

//            input.sendKeys(Keys.CONTROL + "a");
//            input.sendKeys(Keys.DELETE);
            input.clear();

            input.sendKeys(String.valueOf(value));
        }

        // Wait until total updates to 100.00
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.attributeToBe(
                        totalWeightField, "value", "100.00"));
    }
}
