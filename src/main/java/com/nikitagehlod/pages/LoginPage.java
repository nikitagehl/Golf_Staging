package com.nikitagehlod.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class LoginPage {

    WebDriver driver;

    public LoginPage(WebDriver driver) {

        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    @FindBy(xpath = "//input[@type='text']")
    WebElement email;

    @FindBy(xpath = "//input[@type='password']")
    WebElement password;

    @FindBy(xpath = "//button[contains(text(),'Sign IN')]")
    WebElement signInBtn;

    @Step("Login to application")
    public void login(String user,
                      String pass) {

        email.sendKeys(user);

        password.sendKeys(pass);

        signInBtn.click();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public HomePage login2(String username, String pass){

        email.sendKeys(username);

       password.sendKeys(pass);

        signInBtn.click();

        return new HomePage(driver);
    }
}
