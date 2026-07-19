package com.nikitagehlod.Pages;

import com.nikitagehlod.basetest.commonToAllTest;
import com.nikitagehlod.pages.*;
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
                "joshsmith@gmail.com",
                "Tester@123");

        // Step 3
        rabbitHolePage.navigateToRabbitHole();

        // Step 4
        rabbitHolePage.clickFilterButton();

//        rabbitHolePage.selectFilter(
//                "Very Easy");


        rabbitHolePage.selectFilter("Scoring Conditions", "Easy");
        rabbitHolePage.clickApply();

//        rabbitHolePage.selectDropdownOption("Golfers Displayed", "Active Golfers");
//        rabbitHolePage.selectDropdownOption("View", "Putting");



        rabbitHolePage.selectRandomDropdownOption("View");
        rabbitHolePage.selectRandomDropdownOption("Golfers Displayed");
        rabbitHolePage.selectRandomDropdownOption("Time Frame");

        // Step 5
//        List<String> headers =
//                rabbitHolePage.getTableHeaders();
//
//        System.out.println(
//                "Headers: " + headers);

        // Step 6

        List<String> headers = rabbitHolePage.getVisibleHeaders();

        Random random = new Random();

        String header = headers.get(random.nextInt(headers.size()));

        rabbitHolePage.clickPlusAndGetHeader(header);


//        String header = rabbitHolePage.clickPlusAndGetHeader("SG:OTT");

        System.out.println(header);

//        rabbitHolePage.clickHeaderPlusIcon(
//                "SG:TOT");

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
