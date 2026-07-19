package com.nikitagehlod.testPages;

import com.nikitagehlod.basetest.commonToAllTest;
import com.nikitagehlod.pages.*;
import com.nikitagehlod.utils.propertyReader;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Random;

public class RabbitHoleTest  extends commonToAllTest {

    @Test
    public void verifyRabbitHoleFlow() {

        HomePage homePage =
                new HomePage(driver);
        homePage.clickLogin();

        LoginPage loginPage =
                new LoginPage(driver);

        RabbitHolePage rabbitHolePage =
                new RabbitHolePage(driver);
        createModel createModelPage= new createModel(driver);

        EnterWeightage enterWeightage= new EnterWeightage(driver);

        // Step 1
        homePage.clickLogin();

        // Step 2
        loginPage.login(
                propertyReader.readKey("ValidUsername"),
                propertyReader.readKey( "ValidPass"));

        // Step 3
        rabbitHolePage.navigateToRabbitHole();

        // Step 4
       // select filter by condition dropdowns
        rabbitHolePage.clickFilterButton();
        rabbitHolePage.selectFilter("Scoring Conditions", "Easy");
        rabbitHolePage.clickApply();

        // select filters

        rabbitHolePage.selectRandomDropdownOption("View");
        rabbitHolePage.selectRandomDropdownOption("Golfers Displayed");
        rabbitHolePage.selectRandomDropdownOption("Time Frame");




        String header = rabbitHolePage.clickRandomPlus();
        System.out.println("Selected Header: " + header);

        System.out.println(header);
        rabbitHolePage.enterDescription(
                "Automation Header");

        rabbitHolePage.clickAdd();
//
        // Step 7
        System.out.println(rabbitHolePage.verifyToast());
        Assert.assertTrue(
                rabbitHolePage.verifyToast(),
                "Your column setting has been saved");


        // step 8

        createModelPage.setCreateModelBtn();

        createModelPage.navigateToSavedModalPage();

        createModelPage.enterModelName();



        //

        enterWeightage.enterDynamicWeightage();

        String total= enterWeightage.VerifyTotalWeight();
        Assert.assertEquals(total, "100.00");

        createModelPage.clickGenerateModel();
    }
}
