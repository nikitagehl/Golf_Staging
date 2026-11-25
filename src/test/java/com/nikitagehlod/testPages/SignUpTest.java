package com.nikitagehlod.testPages;

import java.time.Duration;
import java.util.List;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.nikitagehlod.basetest.commonToAllTest;
import com.nikitagehlod.pages.HomePage;
import com.nikitagehlod.pages.LoginPage;
import com.nikitagehlod.pages.PricePlanPage;
import com.nikitagehlod.pages.SignUpPage;
import com.nikitagehlod.utils.SiteUrls;
import com.nikitagehlod.utils.TestDataGenerator;
import com.nikitagehlod.utils.propertyReader;

import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Sign-up flow: Home -> SIGN IN -> "Sign Up Today!" -> Price plan -> GET PLAN -> Sign-up form.
 * Positive and negative cases. Every test starts as a logged-out guest.
 */
@Feature("Sign Up")
public class SignUpTest extends commonToAllTest {

    private static final String DEFAULT_PLAN = "Weekly";
    private static final String VALID_PASSWORD = "Tester@123";

    private HomePage homePage;
    private LoginPage loginPage;
    private PricePlanPage pricePlanPage;
    private SignUpPage signUpPage;

    @BeforeClass(alwaysRun = true)
    public void initPages() {
        homePage = new HomePage(driver);
        loginPage = new LoginPage(driver);
        pricePlanPage = new PricePlanPage(driver);
        signUpPage = new SignUpPage(driver);
    }

    /**
     * Gets a clean Weekly sign-up form WITHOUT reloading when possible:
     *  - form already open  -> just clear the fields (and switch back to Weekly if another plan is selected)
     *  - on the plan page    -> click GET PLAN
     *  - anywhere else       -> open the plan page once as a guest (e.g. after a real sign-up logged us in)
     */
    private void openSignUpForm() {
        if (signUpPage.isDisplayed()) {
            // Only the fields are cleared. The plan is NOT switched back: changing plan triggers a
            // slow server call (up to ~20s) and none of the form tests depend on which plan is chosen.
            signUpPage.resetForm();
            return;
        }
        openPricePlanPage();
        signUpPage = pricePlanPage.selectPlan(DEFAULT_PLAN);
    }

    /** Gets to /price-plan: no-op if already there, "Back to All Plans" from the form, else one page load. */
    private void openPricePlanPage() {
        if (driver.getCurrentUrl().contains("/price-plan") && pricePlanPage.isDisplayed()) {
            return;
        }
        if (signUpPage.isDisplayed()) {
            signUpPage.clickBackToPlans();
            return;
        }
        openAsGuest(SiteUrls.of("/price-plan"));
        pricePlanPage.waitForPageLoad();
    }

    /** Navigation tests must start on the home page - opened only if we are not there already. */
    private void openHomePage() {
        String current = driver.getCurrentUrl().split("[?#]")[0].replaceAll("/+$", "");
        if (!current.equalsIgnoreCase(SiteUrls.base())) {
            driver.get(SiteUrls.of("/"));
        }
        homePage.waitForPageLoad();
        homePage.scrollToTop();
    }

    private WebDriverWait waitFor(int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }

    /** Clicks CONTINUE and checks that one of the toasts contains the expected message. */
    private void submitAndExpectToast(String expectedMessage) {
        signUpPage.clickContinue();
        List<String> toasts = signUpPage.getAllToastMessages();
        Allure.step("Toast(s) shown: " + toasts);
        Assert.assertTrue(toasts.stream().anyMatch(t -> t.contains(expectedMessage)),
                "Expected toast '" + expectedMessage + "' but got: " + toasts);
        Assert.assertTrue(driver.getCurrentUrl().contains("/sign-up"),
                "User should stay on the sign-up page, but is on: " + driver.getCurrentUrl());
    }

    // ===================================================================== POSITIVE: navigation

    @Test(priority = 1, description = "TC_SU_01: Home > SIGN IN > 'Sign Up Today!' > GET PLAN opens the sign-up form")
    @Severity(SeverityLevel.BLOCKER)
    public void verifySignUpFromSignInPage() {
        openHomePage();

        Allure.step("Click SIGN IN on the home page");
        homePage.clickSignIn();
        waitFor(15).until(d -> d.getCurrentUrl().contains("/sign-in"));

        Allure.step("Click 'Sign Up Today!'");
        PricePlanPage plans = loginPage.clickSignUpToday();

        Allure.step("Choose the Weekly plan");
        SignUpPage form = plans.selectPlan(DEFAULT_PLAN);

        Assert.assertTrue(form.isDisplayed(), "Sign-up form is not displayed.");
    }

    @Test(priority = 2, description = "TC_SU_02: Home > SUBSCRIBE > GET PLAN opens the sign-up form")
    @Severity(SeverityLevel.CRITICAL)
    public void verifySignUpFromSubscribeButton() {
        openHomePage();

        homePage.clickSubscribe();
        pricePlanPage.waitForPageLoad();
        SignUpPage form = pricePlanPage.selectPlan(DEFAULT_PLAN);

        Assert.assertTrue(form.isDisplayed(), "Sign-up form is not displayed.");
    }

    @Test(priority = 3, description = "TC_SU_03: Price plan page lists the Weekly, Monthly and Yearly plans")
    public void verifyPlansListed() {
        openPricePlanPage();

        List<String> plans = pricePlanPage.getPlanNames();
        Allure.step("Plans shown: " + plans);

        SoftAssert soft = new SoftAssert();
        soft.assertTrue(pricePlanPage.getHeading().contains("Take your golf betting"), "Price plan heading is wrong.");
        soft.assertTrue(plans.contains("Weekly"), "Weekly plan missing");
        soft.assertTrue(plans.contains("Monthly"), "Monthly plan missing");
        soft.assertTrue(plans.contains("Yearly"), "Yearly plan missing");
        soft.assertTrue(pricePlanPage.getVisibleGetPlanButtonCount() >= 3, "Less than 3 GET PLAN buttons visible");
        soft.assertAll();
    }

    @DataProvider(name = "plans")
    public Object[][] plans() {
        return new Object[][] {{"Weekly"}, {"Monthly"}, {"Yearly"}};
    }

    @Test(priority = 4, dataProvider = "plans", description = "TC_SU_04: Chosen plan is shown in the order summary")
    public void verifySelectedPlanInSummary(String plan) {
        openPricePlanPage();
        SignUpPage form = pricePlanPage.selectPlan(plan);

        Assert.assertEquals(form.getSelectedPlan().toLowerCase(), plan.toLowerCase(),
                "Order summary shows the wrong plan.");
    }

    // ===================================================================== POSITIVE: form UI

    @Test(priority = 5, description = "TC_SU_05: Form shows labels, placeholders and a disabled CONTINUE button")
    @Severity(SeverityLevel.CRITICAL)
    public void verifySignUpFormUi() {
        openSignUpForm();

        SoftAssert soft = new SoftAssert();
        soft.assertTrue(signUpPage.areLabelsDisplayed(), "Field labels are wrong or missing.");
        soft.assertEquals(signUpPage.getPlaceholder("username"), "Username");
        soft.assertEquals(signUpPage.getPlaceholder("email"), "Email");
        soft.assertEquals(signUpPage.getPlaceholder("password"), "Password");
        soft.assertEquals(signUpPage.getContinueText().toUpperCase(), "CONTINUE");
        soft.assertFalse(signUpPage.isContinueEnabled(), "CONTINUE should be disabled on an empty form.");
        soft.assertEquals(signUpPage.getPasswordFieldType(), "password", "Password must be hidden by default.");
        soft.assertAll();
    }

    @Test(priority = 6, description = "TC_SU_06: CONTINUE becomes enabled when all fields have valid data")
    public void verifyContinueEnabledWithValidData() {
        openSignUpForm();
        signUpPage.fillForm(TestDataGenerator.uniqueUsername(), TestDataGenerator.uniqueEmail(), VALID_PASSWORD);
        Assert.assertTrue(signUpPage.isContinueEnabled(), "CONTINUE should be enabled when all fields are filled.");
    }

    @Test(priority = 7, description = "TC_SU_07: Eye icon shows and hides the password")
    public void verifyPasswordShowHide() {
        openSignUpForm();
        signUpPage.enterPassword(VALID_PASSWORD);

        signUpPage.togglePasswordVisibility();
        Assert.assertEquals(signUpPage.getPasswordFieldType(), "text", "Password should be visible after clicking the eye.");
        Assert.assertEquals(signUpPage.getFieldValue("password"), VALID_PASSWORD, "Password value changed after toggling.");

        signUpPage.togglePasswordVisibility();
        Assert.assertEquals(signUpPage.getPasswordFieldType(), "password", "Password should be hidden again.");
    }

    @Test(priority = 8, description = "TC_SU_08: CHANGE PLAN lists other plans and switches the order summary")
    public void verifyChangePlan() {
        openSignUpForm();

        // switch to a plan that is NOT the current one (the form may still be on a plan from an earlier test)
        String current = signUpPage.getSelectedPlan();
        String target = current.equalsIgnoreCase("Yearly") ? "Monthly" : "Yearly";
        Allure.step("Current plan: " + current + " -> switching to " + target);

        List<String> options = signUpPage.openChangePlanAndGetOptions();
        Allure.step("Change Plan options: " + options);
        Assert.assertTrue(options.size() >= 2, "Change Plan should offer other plans: " + options);
        Assert.assertTrue(options.stream().anyMatch(o -> o.toLowerCase().contains(target.toLowerCase())),
                target + " is not offered: " + options);

        // changePlanTo closes the open dropdown first, then switches the plan - no reload
        signUpPage.changePlanTo(target);
        Assert.assertEquals(signUpPage.getSelectedPlan().toLowerCase(), target.toLowerCase(),
                "Plan did not change to " + target);
        Assert.assertTrue(signUpPage.isDisplayed(), "Form should still be shown after changing the plan.");
    }

    @Test(priority = 9, description = "TC_SU_09: 'Back to All Plans' returns to the price plan page")
    public void verifyBackToPlans() {
        openSignUpForm();
        PricePlanPage plans = signUpPage.clickBackToPlans();
        Assert.assertTrue(plans.isDisplayed(), "Did not return to the price plan page.");
    }

    @DataProvider(name = "legalLinks")
    public Object[][] legalLinks() {
        return new Object[][] {{"Terms of Use", "/terms-of-use"}, {"Privacy Policy", "/privacy-policy"}};
    }

    @Test(priority = 10, dataProvider = "legalLinks", description = "TC_SU_10: Terms of Use / Privacy Policy open in a new tab")
    public void verifyLegalLinks(String linkName, String expectedPath) {
        openSignUpForm();
        String url = signUpPage.openLegalLinkAndGetUrl(linkName);
        Assert.assertTrue(url.contains(expectedPath), linkName + " opened the wrong page: " + url);
        Assert.assertTrue(signUpPage.isDisplayed(), "Sign-up tab should still show the form.");
    }

    // ===================================================================== POSITIVE: real sign-up

    @Test(priority = 11, description = "TC_SU_11: Sign up with a new username + email goes to the subscription (payment) page")
    @Severity(SeverityLevel.BLOCKER)
    public void verifySuccessfulSignUp() {
        // Creates a REAL account on the site every run, so it only runs when switched on.
        if (!"true".equalsIgnoreCase(String.valueOf(propertyReader.readKey("signup.createAccount")).trim())) {
            throw new SkipException("signup.createAccount is not 'true' in data.properties - skipping real account creation.");
        }

        String username = TestDataGenerator.uniqueUsername();
        String email = TestDataGenerator.uniqueEmail();
        Allure.step("Sign up as " + username + " / " + email);

        openSignUpForm();
        signUpPage.fillForm(username, email, VALID_PASSWORD);
        signUpPage.submitAndWaitForSubscriptionPage();

        // ---- 1. Redirected to the subscription (payment) page for the new email
        String url = driver.getCurrentUrl();
        Assert.assertTrue(url.contains("/subscription"), "Not on the subscription page: " + url);
        Assert.assertTrue(url.contains(email) || url.contains(email.replace("@", "%40")),
                "Subscription page URL should carry the new email: " + url);

        // ---- 2. The new user is logged in (the site stores a session key after sign-up)
        Allure.step("Verify the new user is logged in after sign-up");
        Assert.assertTrue(hasSessionKey(), "No session_key in localStorage - user is not logged in after sign-up.");

        // ---- 3. Log out, then sign in again with the NEW username + password
        Allure.step("Log out and sign in again as " + username);
        openAsGuest(SiteUrls.of("/sign-in"));
        Assert.assertFalse(hasSessionKey(), "Still logged in after clearing the session.");

        loginPage.login(username, VALID_PASSWORD); // waits until we leave the sign-in page
        Assert.assertFalse(driver.getCurrentUrl().contains("/sign-in"),
                "New account could not sign in - still on: " + driver.getCurrentUrl());
        Assert.assertTrue(hasSessionKey(), "No session after signing in with the new account.");
        Allure.step("New account " + username + " signed in successfully");

        // leave the browser logged out so the next tests start as a guest
        openAsGuest(SiteUrls.of("/price-plan"));
    }

    /** True when the site has stored a login session in the browser. */
    private boolean hasSessionKey() {
        Object key = ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("return window.localStorage.getItem('session_key');");
        return key != null && !String.valueOf(key).isBlank() && !"null".equals(String.valueOf(key));
    }

    // ===================================================================== NEGATIVE

    @DataProvider(name = "missingFields")
    public Object[][] missingFields() {
        String u = "qaautouser";
        String e = "qaauto@test.com";
        String p = VALID_PASSWORD;
        return new Object[][] {
                // username, email, password, case name
                {"", "", "", "all fields empty"},
                {u, "", "", "only username"},
                {"", e, "", "only email"},
                {"", "", p, "only password"},
                {u, e, "", "password missing"},
                {u, "", p, "email missing"},
                {"", e, p, "username missing"},
        };
    }

    @Test(priority = 20, dataProvider = "missingFields", description = "TC_SU_N01: CONTINUE stays disabled when a field is empty")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyContinueDisabledWhenFieldMissing(String user, String email, String pass, String caseName) {
        Allure.step("Case: " + caseName);
        openSignUpForm();
        signUpPage.fillForm(user, email, pass);
        Assert.assertFalse(signUpPage.isContinueEnabled(), "CONTINUE should be disabled when " + caseName);
    }

    @Test(priority = 21, description = "TC_SU_N02: Username of only spaces is treated as empty")
    public void verifySpacesOnlyUsername() {
        openSignUpForm();
        signUpPage.fillForm("     ", TestDataGenerator.uniqueEmail(), VALID_PASSWORD);

        Assert.assertEquals(signUpPage.getFieldValue("username"), "", "Spaces should be removed from username.");
        Assert.assertFalse(signUpPage.isContinueEnabled(), "CONTINUE should be disabled for a spaces-only username.");
    }

    @Test(priority = 22, description = "TC_SU_N03: Spaces typed inside username and email are removed")
    public void verifySpacesRemovedFromUsernameAndEmail() {
        openSignUpForm();
        signUpPage.enterUsername("qa auto user");
        signUpPage.enterEmail("qa auto@test.com");

        Assert.assertEquals(signUpPage.getFieldValue("username"), "qaautouser");
        Assert.assertEquals(signUpPage.getFieldValue("email"), "qaauto@test.com");
    }

    @DataProvider(name = "invalidEmails")
    public Object[][] invalidEmails() {
        return new Object[][] {
                {"plainaddress"},
                {"qaauto@"},
                {"qaauto@test"},
                {"@test.com"},
                {"qaauto@test.c"},
                {"qaauto@test.comabc"},
                {"qa#auto@test.com"},
                {"qaauto@@test.com"},
                {"qaauto@test.123"},
        };
    }

    @Test(priority = 23, dataProvider = "invalidEmails", description = "TC_SU_N04: Invalid email shows 'Invalid Email Address!'")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyInvalidEmail(String badEmail) {
        Allure.step("Email: " + badEmail);
        openSignUpForm();
        signUpPage.fillForm(TestDataGenerator.uniqueUsername(), badEmail, VALID_PASSWORD);
        submitAndExpectToast(SignUpPage.MSG_INVALID_EMAIL);
    }

    @Test(priority = 24, description = "TC_SU_N05: Password of only spaces shows 'Password is required!'")
    public void verifySpacesOnlyPassword() {
        openSignUpForm();
        signUpPage.fillForm(TestDataGenerator.uniqueUsername(), TestDataGenerator.uniqueEmail(), "      ");

        Assert.assertTrue(signUpPage.isContinueEnabled(), "CONTINUE is enabled because the field is not empty.");
        submitAndExpectToast(SignUpPage.MSG_PASSWORD_REQUIRED);
    }

    @Test(priority = 25, description = "TC_SU_N06: Username that already exists is rejected")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyExistingUsernameRejected() {
        String existingUser = propertyReader.readKey("ValidUsername").trim();
        openSignUpForm();
        signUpPage.fillForm(existingUser, TestDataGenerator.uniqueEmail(), VALID_PASSWORD);
        signUpPage.clickContinue();

        String toast = signUpPage.getToastMessage();
        Allure.step("Server message: " + toast);
        Assert.assertFalse(toast.isBlank(), "An error message should be shown for an existing username.");
        Assert.assertFalse(driver.getCurrentUrl().contains("/subscription"),
                "Sign-up must not continue with an existing username.");
    }

    @Test(priority = 26, description = "TC_SU_N07: Email that already exists is rejected")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyExistingEmailRejected() {
        String existingEmail = propertyReader.readKey("signup.existingEmail");
        if (existingEmail == null || existingEmail.isBlank()) {
            existingEmail = propertyReader.readKey("free.username");
        }
        openSignUpForm();
        signUpPage.fillForm(TestDataGenerator.uniqueUsername(), existingEmail.trim(), VALID_PASSWORD);
        signUpPage.clickContinue();

        String toast = signUpPage.getToastMessage();
        Allure.step("Server message: " + toast);
        Assert.assertFalse(toast.isBlank(), "An error message should be shown for an existing email.");
        Assert.assertFalse(driver.getCurrentUrl().contains("/subscription"),
                "Sign-up must not continue with an existing email.");
    }
}
