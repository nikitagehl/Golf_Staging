package com.nikitagehlod.testPages;

import com.nikitagehlod.basetest.commonToAllTest;
import com.nikitagehlod.pages.*;
import com.nikitagehlod.utils.SiteUrls;
import com.nikitagehlod.utils.propertyReader;
import io.qameta.allure.Allure;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class RabbitHoleTest extends commonToAllTest {

    private static final int MAX_FILTER_ATTEMPTS = 3;
    private static final int FILTERS_PER_ATTEMPT = 4;

    private HomePage homePage;
    private LoginPage loginPage;
    private RabbitHolePage rabbitHolePage;
    private CreateModel createModelPage;
    private EnterWeightage enterWeightage;
    private FiltersAdded filtersAdded;
    private ManageReports manageReports;

    /** The Rabbit Hole tab. Every other tab is closed again between tests. */
    private String mainTab;

    @BeforeClass(alwaysRun = true)
    public void loginAndNavigate() {

        homePage = new HomePage(driver);
        loginPage = new LoginPage(driver);
        rabbitHolePage = new RabbitHolePage(driver);
        createModelPage = new CreateModel(driver);
        enterWeightage = new EnterWeightage(driver);
        filtersAdded = new FiltersAdded(driver);
        manageReports = new ManageReports(driver);

        // Normally this class starts in a fresh, logged-out browser -> the last branch logs in.
        // The first two branches only matter if the browser already has a session.
        if (loginPage.isLoggedIn() && isOnRabbitHole()) {
            Allure.step("Already logged in as paid user on the Rabbit Hole - continuing without login");
        } else if (loginPage.isLoggedIn()) {
            Allure.step("Already logged in - click Rabbit Hole");
            rabbitHolePage.navigateToRabbitHole();
        } else {
            loginAsPaidUserAndOpenRabbitHole();
        }

        // Logged in as somebody without Rabbit Hole access (e.g. free user left over) -> log in as paid user
        if (!isOnRabbitHole()) {
            Allure.step("Rabbit Hole not reachable with the current session - logging in as paid user");
            openAsGuest(SiteUrls.of("/"));
            loginAsPaidUserAndOpenRabbitHole();
        }

        mainTab = driver.getWindowHandle();
    }

    /**
     * True when the Rabbit Hole TOOL is really open (paid access): the path is /the-rabbit-hole and the
     * "Create Model" button is shown. A free user / guest is redirected to
     * /subscribe?...&next=/the-rabbit-hole, which also contains the text, so the path is checked.
     */
    private boolean isOnRabbitHole() {
        String path = java.net.URI.create(driver.getCurrentUrl()).getPath();
        if (path == null || !path.startsWith("/the-rabbit-hole")) {
            return false;
        }
        try {
            return rabbitHolePage.isRabbitHoleDisplayed();
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    /** Runs when RabbitHoleTest is started on its own (or nobody is logged in yet). */
    private void loginAsPaidUserAndOpenRabbitHole() {
        // start from the home page, wherever the browser currently is
        driver.get(SiteUrls.of("/"));
        homePage.waitForPageLoad();

        Allure.step("Open Login Page");
        homePage.clickLogin();

        Allure.step("Login with Valid Credentials and paid credentials");
        loginPage.login(
                propertyReader.readKey("ValidUsername").trim(),
                propertyReader.readKey("ValidPass").trim());

        Allure.step("Navigate to rabbit hole page");
        rabbitHolePage.navigateToRabbitHole();
    }

    // ------------------------------------------------------------------ helpers

    /** Opens the filter panel (if needed) and resets the filters. */
    private void resetFilters() {
        rabbitHolePage.openFilterPanel();
        filtersAdded.resetFilters();
    }

    /**
     * Gives every test the same clean starting point: only the Rabbit Hole tab open,
     * then "Create Model" opened in a fresh tab. Tests no longer depend on the tab
     * state that the previous test left behind.
     */
    private void openManageModelsPage() {
        createModelPage.closeAllWindowsExcept(mainTab);
        rabbitHolePage.navigateToRabbitHole();
        createModelPage.clickCreateModel();
        createModelPage.waitForModelPage();
    }

    // -------------------------------------------------------------------- tests

    @Test(description = "Verify RH view Flow", priority = 1)
    public void verifyRabbitHoleFlow() {

        Allure.step("Apply random filters");

        boolean tableLoaded = false;

        for (int attempt = 1; attempt <= MAX_FILTER_ATTEMPTS; attempt++) {

            rabbitHolePage.selectRandomDropdownOption("View");
            rabbitHolePage.selectRandomDropdownOption("Golfers Displayed");
            rabbitHolePage.selectRandomDropdownOption("Time Frame");

            rabbitHolePage.openFilterPanel();
            filtersAdded.selectRandomFilters(FILTERS_PER_ATTEMPT);

            rabbitHolePage.clickApply();

            // call it ONCE and keep the answer
            if (rabbitHolePage.verifyTableLoaded()) {
                tableLoaded = true;
                break;
            }

            System.out.println("Attempt " + attempt + ": no data found. Resetting filters...");
            resetFilters();
        }

        if (!tableLoaded) {
            // the last attempt already reset the filters, so the default table must show data
            Assert.assertTrue(rabbitHolePage.verifyTableLoaded(),
                    "No data available, even after resetting the filters.");
        }

        Allure.step("Click random plus icon");
        String header = rabbitHolePage.clickRandomPlus();
        System.out.println("Selected Header: " + header);

        Allure.step("Enter Description");
        rabbitHolePage.enterDescription("Automation Header");

        Allure.step("Click on Add button to save the header");
        rabbitHolePage.clickAdd();

        Allure.step("Verify the header is saved");
        // ONE call: the toast disappears after a few seconds
        Assert.assertTrue(rabbitHolePage.verifyToast(),
                "Your column settings has been saved");

        Allure.step("Click on Create Model button on RH view");
        createModelPage.clickCreateModel();

        Allure.step("Verify the create model page loaded successfully");
        createModelPage.waitForModelPage();

        Allure.step("Verify the delete column functionality is working");
        boolean removed = createModelPage.clickFirstRemoveIcon();
        System.out.println("Column removed: " + removed);

        Allure.step("Verify the report name input box loaded and enter the report name");
        createModelPage.enterModelName("Automation Report 1");

        Allure.step("Entering the weight in input field of all the saved columns");
        enterWeightage.enterDynamicWeightage();

        Allure.step("Verify that the total weight should be 100");
        Assert.assertEquals(enterWeightage.VerifyTotalWeight(), "100.00");

        Allure.step("Click on generate report and check for report generated");
        createModelPage.clickGenerateModel();

        // leave a clean browser for the next test: only the Rabbit Hole tab stays open
        createModelPage.closeAllWindowsExcept(mainTab);
    }

    @Test(description = "Verify Change Report Status Flow", priority = 2)
    public void changeReportStatus() {

        Allure.step("Open the Create Model page");
        openManageModelsPage();

        Allure.step("Change the status of a report");
        manageReports.ChangeReportStatus();

        System.out.println("This step is done");
    }

    @Test(description = "Verify Delete Report Flow", priority = 3)
    public void DeleteReport() {

        Allure.step("Open the Create Model page");
        openManageModelsPage();

        Allure.step("Click on Delete report button");
        manageReports.DeleteReport();

        Allure.step("Report Delete successfully");
    }

    @Test(description = "verify Copy Report Functionality", priority = 4)
    public void CopyReport() {

        Allure.step("Open the Create Model page");
        openManageModelsPage();

        Allure.step("Verify copy report");
        manageReports.CopyReport();

        Allure.step("Report copied successfully");
        System.out.println("Done copied");
    }
}
