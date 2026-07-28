package com.nikitagehlod.pages;

import com.nikitagehlod.Base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class PremiumPage extends BasePage {

    WebDriver driver;
    WebDriverWait wait;
    public PremiumPage(WebDriver driver){

        super(driver);
        this.driver= driver;

        wait = new WebDriverWait(driver,
                Duration.ofSeconds(5));

        PageFactory.initElements(driver, this);
    }


    private By premiumHeading =
            By.xpath("//span[contains(text(),'Premium')]");

    public boolean isPremiumPageDisplayed(){

        return wait.until(ExpectedConditions.visibilityOfElementLocated(premiumHeading))
                .isDisplayed();

    }
}
