package com.nikitagehlod.pages;

import com.nikitagehlod.Base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

public class PremiumPage extends BasePage {

    private final By premiumHeading = By.xpath("//span[contains(text(),'Premium')]");

    public PremiumPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public boolean isPremiumPageDisplayed() {
        return waitForVisibility(premiumHeading).isDisplayed();
    }
}
