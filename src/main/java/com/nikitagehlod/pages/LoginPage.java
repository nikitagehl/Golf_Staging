package com.nikitagehlod.pages;

import com.nikitagehlod.Base.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    private final By emailField = By.xpath("//input[@type='text']");
    private final By passwordField = By.xpath("//input[@type='password']");
    private final By signInBtn = By.xpath("//button[contains(text(),'Sign IN')]");
    private final By signUpTodayLink = By.xpath("//a[@href='/price-plan' and contains(normalize-space(),'Sign Up Today')]");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    @Step("Login to application")
    public void login(String user, String pass) {

        type(emailField, user);
        type(passwordField, pass);
        click(signInBtn);

        waitForLoginToComplete();
    }

    public HomePage login2(String username, String pass) {

        login(username, pass);

        return new HomePage(driver);
    }

    /** "Not a Member Yet? Sign Up Today!" on the sign-in page - goes to the plan selection page. */
    @Step("Click 'Sign Up Today!' on the sign-in page")
    public PricePlanPage clickSignUpToday() {
        waitForLoader();
        safeClick(waitForVisibility(signUpTodayLink));
        PricePlanPage plans = new PricePlanPage(driver);
        plans.waitForPageLoad();
        return plans;
    }

    /**
     * Replaces the old Thread.sleep(5000): login is finished when we have left the
     * sign-in page (or the password field is gone). Fails after 10s if the login did not work.
     */
    private void waitForLoginToComplete() {
        wait.until(d -> !d.getCurrentUrl().contains("sign-in")
                || d.findElements(passwordField).isEmpty());
        waitForLoader();
    }
}
