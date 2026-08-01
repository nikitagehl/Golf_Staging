package com.nikitagehlod.testPages;

import com.nikitagehlod.basetest.commonToAllTest;
import com.nikitagehlod.pages.HomePage;
import com.nikitagehlod.pages.LoginPage;
import com.nikitagehlod.pages.PremiumPage;
import com.nikitagehlod.pages.RabbitHolePage;
import com.nikitagehlod.utils.propertyReader;
import io.qameta.allure.Allure;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginAccessTest extends commonToAllTest {
    HomePage homePage =
            new HomePage(driver);
    LoginPage loginPage = new LoginPage(driver);
    RabbitHolePage rabbitHolePage = new RabbitHolePage(driver);


    String paidUser = propertyReader.readKey("paid.username");
    String paidPass = propertyReader.readKey("paid.password");

    String freeUser = propertyReader.readKey("free.username");
    String freePass = propertyReader.readKey("free.password");


    @Test(description="Verify free user cannot access Rabbit Hole",priority = 1)
    public void verifyFreeUser(){



        Allure.step("Click on Login button");
        homePage.clickLogin();

        Allure.step("Login page open");
        LoginPage loginPage = new LoginPage(driver);

        Allure.step("Login with free user account");
        HomePage home = loginPage.login2(freeUser, freePass);

        Allure.step("Verify that free and guest user not able to access RH");
        home.openRabbitHole();

        Allure.step(" Free User should redirect to premium plan page");
        PremiumPage premium = new PremiumPage(driver);

        Allure.step("Free user redirected to premium page successfully");
        Assert.assertTrue(
                premium.isPremiumPageDisplayed(),
                "Free user should be redirected to Premium page."
        );

    }

    @Test(description="Verify paid user can access Rabbit Hole", priority = 2)
    public void verifyPaidUser(){

        HomePage homePage =
                new HomePage(driver);
        Allure.step("Click on Login button");
        homePage.clickLogin();



        Allure.step("Login with paid user account ");
        HomePage home = loginPage.login2(paidUser, paidPass);

        Allure.step("Navigate to RH Tool page");
        home.openRabbitHole();

        Allure.step("user able to access the RH page");


        Assert.assertTrue(
                rabbitHolePage.isRabbitHoleDisplayed(),
                "Paid user should reach Rabbit Hole."
        );

    }
}
