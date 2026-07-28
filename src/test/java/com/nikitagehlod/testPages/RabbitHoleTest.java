package com.nikitagehlod.testPages;

import com.nikitagehlod.basetest.commonToAllTest;
import com.nikitagehlod.pages.*;
import com.nikitagehlod.utils.propertyReader;
import io.qameta.allure.Allure;
import listeners.TestListener;
import net.bytebuddy.build.Plugin;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Random;


public class RabbitHoleTest  extends commonToAllTest {





    String paidUser = propertyReader.readKey("paid.username");
    String paidPass = propertyReader.readKey("paid.password");

    String freeUser = propertyReader.readKey("free.username");
    String freePass = propertyReader.readKey("free.password");


    @Test(description="Verify free user cannot access Rabbit Hole",priority = 1)
    public void verifyFreeUser(){

        HomePage homePage =
                new HomePage(driver);


//        LoginPage loginPage =
//                new LoginPage(driver);

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

        LoginPage loginPage = new LoginPage(driver);

        Allure.step("Login with paid user account ");
        HomePage home = loginPage.login2(paidUser, paidPass);

        Allure.step("Navigate to RH Tool page");
        home.openRabbitHole();

        Allure.step("user able to access the RH page");
        RabbitHolePage rabbit = new RabbitHolePage(driver);

        Assert.assertTrue(
                rabbit.isRabbitHoleDisplayed(),
                "Paid user should reach Rabbit Hole."
        );

    }



    @Test (priority = 3)
    public void verifyRabbitHoleFlow() {

        HomePage homePage =
                new HomePage(driver);


        LoginPage loginPage =
                new LoginPage(driver);

        RabbitHolePage rabbitHolePage =
                new RabbitHolePage(driver);
        CreateModel createModelPage= new CreateModel(driver);

        EnterWeightage enterWeightage= new EnterWeightage(driver);

        FiltersAdded filtersAdded= new FiltersAdded(driver);

        Allure.step("Verify the RH view Flow");

        // Step 1
        Allure.step("Click on Login button");
        homePage.clickLogin();

        // Step 2
        Allure.step("Login to application");
        loginPage.login(
                propertyReader.readKey("ValidUsername"),
                propertyReader.readKey( "ValidPass"));



        // Step 3
        Allure.step("Navigate to Rabbit Hole page");
        rabbitHolePage.navigateToRabbitHole();

        // Step 4
       // select filter by condition dropdowns
//        rabbitHolePage.clickFilterButton();
//        rabbitHolePage.selectFilter("Scoring Conditions", "Easy");
//        rabbitHolePage.clickApply();




        // select filters
//
//        rabbitHolePage.selectRandomDropdownOption("View");
//        rabbitHolePage.selectRandomDropdownOption("Golfers Displayed");
//        rabbitHolePage.selectRandomDropdownOption("Time Frame");
//
//        rabbitHolePage.clickFilterButton();
//
//        filtersAdded.selectRandomFilter();
//        filtersAdded.selectRandomFilter();
//        filtersAdded.selectRandomFilter();
//        filtersAdded.selectRandomFilter();
//
//        rabbitHolePage.clickApply();

        Allure.step("Apply random filters");
        boolean tableLoaded = false;

        for (int attempt = 1; attempt <= 3; attempt++) {

            rabbitHolePage.selectRandomDropdownOption("View");
            rabbitHolePage.selectRandomDropdownOption("Golfers Displayed");
            rabbitHolePage.selectRandomDropdownOption("Time Frame");

            rabbitHolePage.clickFilterButton();

            filtersAdded.selectRandomFilter();
            filtersAdded.selectRandomFilter();
            filtersAdded.selectRandomFilter();
            filtersAdded.selectRandomFilter();

            rabbitHolePage.clickApply();
            System.out.println(rabbitHolePage.verifyTableLoaded());
            if (rabbitHolePage.verifyTableLoaded()) {

                tableLoaded = true;
                break;
            }

            System.out.println("No data found. Resetting filters...");

            rabbitHolePage.clickFilterButton();
            filtersAdded.resetFilters();
        }

        Assert.assertTrue(tableLoaded, "Table has no data after 3 attempts.");

//        if (!rabbitHolePage.verifyTableLoaded()) {
//
//            System.out.println("No records found for selected filters.");
//            rabbitHolePage.clickFilterButton();
//            filtersAdded.resetFilters();
//            System.out.println(!rabbitHolePage.verifyTableLoaded());
////            if (!rabbitHolePage.verifyTableLoaded()) {
////                continue;
////            }
//            return;      // Skip remaining execution
//        }






//        if (!rabbitHolePage.verifyTableLoaded()) {
//
//            System.out.println("No records found for selected filters.");
//
//            rabbitHolePage.clickFilterButton();
//            filtersAdded.resetFilters();
//
//            // Check again after resetting filters
//            System.out.println(!rabbitHolePage.verifyTableLoaded());
//            if (!rabbitHolePage.verifyTableLoaded()) {
//                System.out.println("Still no data after reset.");
//
//            }
//            return;
//        }

        Allure.step("Click random plus icon");

            String header = rabbitHolePage.clickRandomPlus();
            System.out.println("Selected Header: " + header);

            System.out.println(header);

            Allure.step("Enter Description");
            rabbitHolePage.enterDescription(
                    "Automation Header");

        Allure.step("Click on Add button to save the header");
            rabbitHolePage.clickAdd();
            System.out.println(rabbitHolePage.verifyToast());

        Allure.step("Verify the header is saved ");
            Assert.assertTrue(
                    rabbitHolePage.verifyToast(),
                    "Your column setting has been saved");




//
        // Step 7



        // step 8

        Allure.step("Click on Create Model button on RH view");

        createModelPage.clickCreateModel();

//        createModelPage.navigateToSavedModalPage();
        Allure.step("Verify the create model page loaded successfully");
        createModelPage.waitForModelPage();

        Allure.step("Verify the delte column functionality is working");
        createModelPage.clickFirstRemoveIcon();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Allure.step("Verify the report name input box loaded and enter the report name");
        createModelPage.enterModelName("Automation Report 1");



        //


        Allure.step("Entering the wieght in input field of all the saved columns ");

        enterWeightage.enterDynamicWeightage();

        Allure.step("Verify that the total weight should be 100");
        String total= enterWeightage.VerifyTotalWeight();
        Assert.assertEquals(total, "100.00");

        Allure.step("Click on generate report and check for report generated");
        createModelPage.clickGenerateModel();
    }
}
