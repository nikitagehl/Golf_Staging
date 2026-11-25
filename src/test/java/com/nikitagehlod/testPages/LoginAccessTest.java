package com.nikitagehlod.testPages;

import com.nikitagehlod.basetest.commonToAllTest;
import com.nikitagehlod.pages.HomePage;
import com.nikitagehlod.pages.LoginPage;
import com.nikitagehlod.pages.PremiumPage;
import com.nikitagehlod.pages.RabbitHolePage;
import com.nikitagehlod.utils.propertyReader;
import io.qameta.allure.Allure;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LoginAccessTest extends commonToAllTest {

    private HomePage homePage;
    private LoginPage loginPage;
    private RabbitHolePage rabbitHolePage;
    private PremiumPage premiumPage;

    private String paidUser;
    private String paidPass;
    private String freeUser;
    private String freePass;

    /**
     * Page objects are created here, NOT in field initialisers: the driver only exists after
     * the base class @BeforeClass has run (field initialisers ran while driver was still null).
     */
    @BeforeClass(alwaysRun = true)
    public void initPages() {

        homePage = new HomePage(driver);
        loginPage = new LoginPage(driver);
        rabbitHolePage = new RabbitHolePage(driver);
        premiumPage = new PremiumPage(driver);

        paidUser = propertyReader.readKey("paid.username");
        paidPass = propertyReader.readKey("paid.password");
        freeUser = propertyReader.readKey("free.username");
        freePass = propertyReader.readKey("free.password");
    }

    /** Every test starts logged out, so the free-user test cannot affect the paid-user test. */
    @BeforeMethod(alwaysRun = true)
    public void startLoggedOut() {

        driver.manage().deleteAllCookies();
        driver.get(propertyReader.readKey("URL").trim());

        try {
            ((JavascriptExecutor) driver).executeScript(
                    "window.localStorage.clear(); window.sessionStorage.clear();");
        } catch (Exception ignored) {
            // storage not available on this page - cookies were already cleared
        }

        driver.navigate().refresh();
    }

    @Test(description = "Verify free user cannot access Rabbit Hole", priority = 1)
    public void verifyFreeUser() {

        Allure.step("Click on Login button");
        homePage.clickLogin();

        Allure.step("Login with free user account");
        HomePage home = loginPage.login2(freeUser, freePass);

        Allure.step("Verify that free and guest user not able to access RH");
        home.openRabbitHole();

        Allure.step("Free user should be redirected to the premium plan page");
        Assert.assertTrue(
                premiumPage.isPremiumPageDisplayed(),
                "Free user should be redirected to Premium page.");
    }

    @Test(description = "Verify paid user can access Rabbit Hole", priority = 2)
    public void verifyPaidUser() {

        Allure.step("Click on Login button");
        homePage.clickLogin();

        Allure.step("Login with paid user account");
        HomePage home = loginPage.login2(paidUser, paidPass);

        Allure.step("Navigate to RH Tool page");
        home.openRabbitHole();

        Allure.step("User able to access the RH page");
        Assert.assertTrue(
                rabbitHolePage.isRabbitHoleDisplayed(),
                "Paid user should reach Rabbit Hole.");
    }
}
