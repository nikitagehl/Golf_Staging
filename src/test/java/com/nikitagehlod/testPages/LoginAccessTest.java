package com.nikitagehlod.testPages;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.nikitagehlod.basetest.commonToAllTest;
import com.nikitagehlod.pages.HomePage;
import com.nikitagehlod.pages.LoginPage;
import com.nikitagehlod.pages.PremiumPage;
import com.nikitagehlod.pages.RabbitHolePage;
import com.nikitagehlod.utils.SiteUrls;
import com.nikitagehlod.utils.propertyReader;

import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

/**
 * Login - positive and negative cases, plus free/paid access to the Rabbit Hole.
 *
 * The sign-in page is NOT reloaded between the negative tests: the form is just cleared.
 * Tests that really log in always log out again (openAsGuest) so the next test starts as a guest.
 */
@Feature("Login")
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

        paidUser = propertyReader.readKey("paid.username").trim();
        paidPass = propertyReader.readKey("paid.password").trim();
        freeUser = propertyReader.readKey("free.username").trim();
        freePass = propertyReader.readKey("free.password").trim();
    }

    // ============================================================================= helpers

    /** Clean, logged-out sign-in form. Re-uses the open page when possible (no reload). */
    private void openSignInPage() {
        if (loginPage.isDisplayed() && !loginPage.isLoggedIn()) {
            loginPage.resetForm();
            return;
        }
        openAsGuest(SiteUrls.of("/sign-in"));
        loginPage.waitForPageLoad();
    }

    /** Home page as a logged-out guest (used by the free / paid user flows). */
    private void openHomeAsGuest() {
        openAsGuest(SiteUrls.of("/"));
        homePage.waitForPageLoad();
    }

    /** Logs out by clearing the session, so the next test starts as a guest. */
    private void logout() {
        openAsGuest(SiteUrls.of("/sign-in"));
        loginPage.waitForPageLoad();
    }

    private WebDriverWait waitFor(int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }

    /** Clicks Sign IN and checks a toast with the expected text appears and we are NOT logged in. */
    private void submitAndExpectToast(String expectedMessage) {
        loginPage.clickSignIn();
        List<String> toasts = loginPage.getAllToastMessages();
        Allure.step("Toast(s) shown: " + toasts);
        Assert.assertTrue(toasts.stream().anyMatch(t -> t.contains(expectedMessage)),
                "Expected toast '" + expectedMessage + "' but got: " + toasts);
        assertStillLoggedOut();
    }

    private void assertStillLoggedOut() {
        Assert.assertTrue(driver.getCurrentUrl().contains("/sign-in"),
                "User should stay on the sign-in page, but is on: " + driver.getCurrentUrl());
        Assert.assertFalse(loginPage.isLoggedIn(), "User must NOT be logged in.");
    }

    // ===================================================================== POSITIVE: page

    @Test(priority = 1, description = "TC_LOGIN_01: Sign-in page shows title, labels, placeholders and links")
    @Severity(SeverityLevel.CRITICAL)
    public void verifySignInPageUi() {
        openSignInPage();

        SoftAssert soft = new SoftAssert();
        soft.assertEquals(loginPage.getTitle(), "Hi, Welcome Back!", "Page title");
        soft.assertEquals(loginPage.getSubtitle(), "Sign in to access premium content and tools.", "Subtitle");
        soft.assertEquals(loginPage.getEmailLabel(), "Username/Email", "Username label");
        soft.assertEquals(loginPage.getPasswordLabel(), "Password", "Password label");
        soft.assertEquals(loginPage.getPlaceholder("email"), "Username/Email", "Username placeholder");
        soft.assertEquals(loginPage.getPlaceholder("password"), "Password", "Password placeholder");
        soft.assertFalse(loginPage.isSignInEnabled(), "Sign IN should be disabled on an empty form.");
        soft.assertEquals(loginPage.getPasswordFieldType(), "password", "Password must be hidden by default.");
        soft.assertTrue(loginPage.isForgotPasswordLinkDisplayed(), "'Forgot Password?' link missing.");
        soft.assertTrue(loginPage.isSignUpTodayLinkDisplayed(), "'Sign Up Today!' link missing.");
        soft.assertAll();
    }

    @Test(priority = 2, description = "TC_LOGIN_02: SIGN IN in the header opens the sign-in page")
    public void verifySignInFromHeader() {
        openHomeAsGuest();
        homePage.clickSignIn();
        loginPage.waitForPageLoad();
        Assert.assertTrue(loginPage.isDisplayed(), "Sign-in form is not displayed.");
    }

    @Test(priority = 3, description = "TC_LOGIN_03: Sign IN becomes enabled when both fields have a value")
    public void verifySignInEnabledWhenFilled() {
        openSignInPage();
        loginPage.fillCredentials(paidUser, "anything");
        Assert.assertTrue(loginPage.isSignInEnabled(), "Sign IN should be enabled when both fields are filled.");
    }

    @Test(priority = 4, description = "TC_LOGIN_04: Eye icon shows and hides the password")
    public void verifyPasswordShowHide() {
        openSignInPage();
        loginPage.enterPassword("Secret@123");

        loginPage.togglePasswordVisibility();
        Assert.assertEquals(loginPage.getPasswordFieldType(), "text", "Password should be visible after clicking the eye.");
        Assert.assertEquals(loginPage.getFieldValue("password"), "Secret@123", "Password value changed after toggling.");

        loginPage.togglePasswordVisibility();
        Assert.assertEquals(loginPage.getPasswordFieldType(), "password", "Password should be hidden again.");
    }

    @Test(priority = 5, description = "TC_LOGIN_05: 'Forgot Password?' opens the Reset Password page")
    public void verifyForgotPasswordLink() {
        openSignInPage();
        loginPage.clickForgotPassword();

        Assert.assertTrue(driver.getCurrentUrl().contains("/forgot-password"));
        Assert.assertEquals(loginPage.getResetPasswordTitle(), "Reset Password");
        Assert.assertFalse(driver.findElements(By.cssSelector("input#email")).isEmpty(),
                "Reset Password page should ask for the Username/Email.");
    }

    // ===================================================================== NEGATIVE

    @DataProvider(name = "emptyFields")
    public Object[][] emptyFields() {
        return new Object[][] {
                // username, password, case name
                {"", "", "both fields empty"},
                {"josh4", "", "password empty"},
                {"", "Secret@123", "username empty"},
        };
    }

    @Test(priority = 10, dataProvider = "emptyFields", description = "TC_LOGIN_N01: Sign IN stays disabled when a field is empty")
    @Severity(SeverityLevel.CRITICAL)
    public void verifySignInDisabledWhenFieldEmpty(String user, String pass, String caseName) {
        Allure.step("Case: " + caseName);
        openSignInPage();
        loginPage.fillCredentials(user, pass);
        Assert.assertFalse(loginPage.isSignInEnabled(), "Sign IN should be disabled when " + caseName);
    }

    @Test(priority = 11, description = "TC_LOGIN_N02: Username of only spaces shows 'Username/Email is required!'")
    public void verifySpacesOnlyUsername() {
        openSignInPage();
        loginPage.fillCredentials("     ", "Secret@123");
        Assert.assertTrue(loginPage.isSignInEnabled(), "Spaces count as a value, so Sign IN is enabled.");
        submitAndExpectToast(LoginPage.MSG_USERNAME_REQUIRED);
    }

    @Test(priority = 12, description = "TC_LOGIN_N03: Password of only spaces shows 'Password is required!'")
    public void verifySpacesOnlyPassword() {
        openSignInPage();
        loginPage.fillCredentials(paidUser, "      ");
        submitAndExpectToast(LoginPage.MSG_PASSWORD_REQUIRED);
    }

    @DataProvider(name = "wrongCredentials")
    public Object[][] wrongCredentials() {
        String badUser = propertyReader.readKey("inValidUsername");
        String badPass = propertyReader.readKey("inValidPass");
        return new Object[][] {
                // Only ONE wrong-password attempt for the real account, so it cannot get locked.
                {paidUser, "Wrong@Pass999", "valid username + wrong password"},
                {"qa_no_such_user_98765", "Secret@123", "username that does not exist"},
                {"qa.no.such.user98765@test.com", "Secret@123", "email that does not exist"},
                {badUser == null ? "admin" : badUser.trim(), badPass == null ? "12345678" : badPass.trim(),
                        "invalid credentials from data.properties"},
                {"' OR '1'='1", "' OR '1'='1", "SQL injection text"},
                {"<script>alert(1)</script>", "Secret@123", "script tag as username"},
        };
    }

    @Test(priority = 13, dataProvider = "wrongCredentials", description = "TC_LOGIN_N04: Wrong credentials are rejected with an error")
    @Severity(SeverityLevel.BLOCKER)
    public void verifyWrongCredentialsRejected(String user, String pass, String caseName) {
        Allure.step("Case: " + caseName);
        openSignInPage();
        loginPage.submitCredentials(user, pass);

        String toast = loginPage.getToastMessage();
        Allure.step("Server message: " + toast);
        Assert.assertFalse(toast.isBlank(), "An error message should be shown for: " + caseName);
        assertStillLoggedOut();
    }

    @Test(priority = 14, description = "TC_LOGIN_N05: Password is case-sensitive")
    public void verifyPasswordIsCaseSensitive() {
        String changedCase = swapCase(freePass);
        if (changedCase.equals(freePass)) {
            throw new SkipException("free.password has no letters, so case cannot be changed.");
        }
        openSignInPage();
        loginPage.submitCredentials(freeUser, changedCase);

        String toast = loginPage.getToastMessage();
        Allure.step("Server message: " + toast);
        assertStillLoggedOut();
    }

    private static String swapCase(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            sb.append(Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c));
        }
        return sb.toString();
    }

    // ===================================================================== POSITIVE: real logins

    @Test(priority = 20, description = "TC_LOGIN_06: Login with a valid USERNAME goes to the home page and creates a session")
    @Severity(SeverityLevel.BLOCKER)
    public void verifyLoginWithUsername() {
        openSignInPage();
        loginPage.login(paidUser, paidPass);

        String current = driver.getCurrentUrl().split("[?#]")[0].replaceAll("/+$", "");
        Assert.assertEquals(current, SiteUrls.base(), "After login the user should land on the home page.");
        Assert.assertTrue(loginPage.isLoggedIn(), "No session after a valid login.");
        logout();
    }

    @Test(priority = 21, description = "TC_LOGIN_07: Login with a valid EMAIL works")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyLoginWithEmail() {
        if (!freeUser.contains("@")) {
            throw new SkipException("free.username is not an email address.");
        }
        openSignInPage();
        loginPage.login(freeUser, freePass);

        Assert.assertFalse(driver.getCurrentUrl().contains("/sign-in"), "Still on the sign-in page.");
        Assert.assertTrue(loginPage.isLoggedIn(), "No session after logging in with an email.");
        logout();
    }

    @Test(priority = 22, description = "TC_LOGIN_08: User stays logged in after a page refresh")
    public void verifySessionSurvivesRefresh() {
        openSignInPage();
        loginPage.login(paidUser, paidPass);

        driver.navigate().refresh();
        homePage.waitForPageLoad();
        Assert.assertTrue(loginPage.isLoggedIn(), "Session was lost after refreshing the page.");
        logout();
    }

    @Test(priority = 23, description = "TC_LOGIN_09: After login the user is sent to the '?next=' page")
    public void verifyRedirectToNextPage() {
        openAsGuest(SiteUrls.of("/sign-in?next=/tutorials"));
        loginPage.waitForPageLoad();
        loginPage.login(paidUser, paidPass);

        waitFor(20).until(d -> d.getCurrentUrl().contains("/tutorials"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/tutorials"),
                "Should return to /tutorials after login, but is on: " + driver.getCurrentUrl());
        logout();
    }

    @Test(priority = 24, description = "TC_LOGIN_10: Paywall 'Sign in here' logs a paid user straight into the Rabbit Hole")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyPaywallSignInReturnsToRabbitHole() {
        openAsGuest(SiteUrls.of("/the-rabbit-hole"));
        waitFor(20).until(d -> d.getCurrentUrl().contains("/subscribe"));

        Allure.step("Click 'Already upgraded? Sign in here'");
        homePage.safeClick(homePage.waitForFirstDisplayed(By.cssSelector("a[href^='/sign-in?next=']")));
        loginPage.waitForPageLoad();
        Assert.assertTrue(driver.getCurrentUrl().contains("next="), "Sign-in link should carry the 'next' page.");

        loginPage.login(paidUser, paidPass);
        waitFor(20).until(d -> d.getCurrentUrl().contains("/the-rabbit-hole"));
        Assert.assertTrue(rabbitHolePage.isRabbitHoleDisplayed(), "Paid user should land in the Rabbit Hole.");
        logout();
    }

    // ============================================================ existing: free / paid access

    @Test(priority = 30, description = "Verify free user cannot access Rabbit Hole")
    public void verifyFreeUser() {

        openHomeAsGuest();

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
        logout();
    }

    @Test(priority = 31, description = "Verify paid user can access Rabbit Hole")
    public void verifyPaidUser() {

        openHomeAsGuest();

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
        logout();
    }
}
