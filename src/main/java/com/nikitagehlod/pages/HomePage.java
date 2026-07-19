package com.nikitagehlod.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class HomePage {
    WebDriver driver;
    WebDriverWait wait;

    public HomePage(WebDriver driver) {

        this.driver = driver;
        wait = new WebDriverWait(driver,
                Duration.ofSeconds(20));
        PageFactory.initElements(driver, this);
    }

    @FindBy(xpath = "//a[contains(@href,'/sign-in' ) and contains(@class,'py-0')]")
    WebElement loginBtn;

    public void clickLogin() {
        By loader = By.cssSelector(".cmn-loader");
        wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
        loginBtn.click();
    }
}
