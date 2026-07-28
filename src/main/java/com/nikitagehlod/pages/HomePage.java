package com.nikitagehlod.pages;

import com.nikitagehlod.Base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class HomePage extends BasePage{
    WebDriver driver;
    WebDriverWait wait;

    public HomePage(WebDriver driver) {
        super(driver);
        this.driver = driver;
        wait = new WebDriverWait(driver,
                Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    @FindBy(xpath = "//a[contains(@href,'/sign-in' ) and contains(@class,'py-0')]")
    WebElement loginBtn;

    private By rabbitHoleMenu =
            By.xpath("//a[contains(@href,'/the-rabbit-hole')]");

    public void openRabbitHole(){
        By loader = By.cssSelector(".cmn-loader");
        wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
        click(rabbitHoleMenu);
    }

    public void clickLogin() {
        By loader = By.cssSelector(".cmn-loader");
        wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
        loginBtn.click();
    }
}
