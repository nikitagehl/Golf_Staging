package com.nikitagehlod.testPages;

import com.nikitagehlod.basetest.commonToAllTest;
import com.nikitagehlod.pages.*;
import com.nikitagehlod.utils.propertyReader;
import io.qameta.allure.Allure;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.testng.Assert;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.PriorityQueue;

public class RabbitHoleTest  extends commonToAllTest {
    private static final Logger logger= (Logger) LogManager.getLogger(RabbitHoleTest.class);



    private HomePage homePage;
    private LoginPage loginPage;
    private RabbitHolePage rabbitHolePage;
    private CreateModel createModelPage;
    private EnterWeightage enterWeightage;
    private FiltersAdded filtersAdded;



    @BeforeClass(alwaysRun = true)

    public void loginAndNavigate() {
        homePage = new HomePage(driver);
        loginPage = new LoginPage(driver);
        rabbitHolePage = new RabbitHolePage(driver);
        createModelPage = new CreateModel(driver);
        enterWeightage = new EnterWeightage(driver);
        filtersAdded = new FiltersAdded(driver);



        Allure.step("Open Login Page ");
        homePage.clickLogin();
        Allure.step("Login with Valid Credentials and paid credentials");
        loginPage.login(
                propertyReader.readKey("ValidUsername"),
                propertyReader.readKey("ValidPass"));

        Allure.step("Navigate to rabbit hole page");

        rabbitHolePage.navigateToRabbitHole();
    }




    @Test (description="Verify RH view Flow", priority = 1)
    public void verifyRabbitHoleFlow() {

        logger.info("Starting the test");

        Allure.step("Apply random filters");
        boolean tableLoaded = false;
        logger.info("Selecting the filters ");
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


//
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

        Allure.step("Verify the delete column functionality is working");
        createModelPage.clickFirstRemoveIcon();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Allure.step("Verify the report name input box loaded and enter the report name");
        createModelPage.enterModelName("Automation Report 1");



        //


        Allure.step("Entering the weight in input field of all the saved columns ");

        enterWeightage.enterDynamicWeightage();

        Allure.step("Verify that the total weight should be 100");
        String total= enterWeightage.VerifyTotalWeight();
        Assert.assertEquals(total, "100.00");

        Allure.step("Click on generate report and check for report generated");
        createModelPage.clickGenerateModel();
    }





    @Test(priority = 2)
    public void changeReportStatus(){
        createModelPage.clickCreateModel();
        Allure.step("Click on Create Model button on RH view");
      ManageReports  manageReport = new ManageReports(driver);
      manageReport.ChangeReportStatus();
        System.out.println("This step is done");
    }

    @Test(description="Verify Delete Report Flow", priority = 3)
    public void DeleteReport(){
//        createModelPage.clickCreateModel();
        Allure.step("Click on Create Model button on RH view");


        ManageReports  manageReport = new ManageReports(driver);
        Allure.step("Click on Delete report button");
        manageReport.DeleteReport();
        Allure.step("Report Delete successfully");
    }

  @Test(description="verify Copy Report Functionality ",priority=4)
    public void CopyReport(){
//      createModelPage.clickCreateModel();
      Allure.step("Click on Create Model button on RH view");


      ManageReports  manageReport = new ManageReports(driver);
      Allure.step("Verify copy report");
      manageReport.CopyReport();
      Allure.step("Report copied successfully");
      System.out.println("Done copied ");
    }
}




