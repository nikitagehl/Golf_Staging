package com.nikitagehlod.pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.nikitagehlod.Base.BasePage;

import io.qameta.allure.Step;

/**
 * /sign-in page.
 *
 * How the site behaves (read from the site's own code, Nov 2026):
 *  - "Sign IN" stays disabled until Username/Email AND Password both have a value.
 *  - On click the site checks (after trimming spaces) and shows a toast:
 *      "Username/Email is required!" then "Password is required!"
 *  - Spaces are NOT removed while typing (unlike the sign-up form).
 *  - A value that looks like an email is sent as email, anything else as username.
 *  - Wrong credentials -> the server's error message is shown as a toast.
 *  - Success -> full page load to the "?next=" URL if given, otherwise to the home page "/",
 *    and a session key is stored in localStorage.
 */
public class LoginPage extends BasePage {

    public static final String MSG_USERNAME_REQUIRED = "Username/Email is required!";
    public static final String MSG_PASSWORD_REQUIRED = "Password is required!";

    // ids are unique on the sign-in page (the old //input[@type='text'] also matched the
    // password field once "show password" turns it into type=text)
    private final By emailField = By.cssSelector("input.sign-in-page-card-form-input#email");
    private final By passwordField = By.cssSelector("input.sign-in-page-card-form-input#password");
    private final By signInBtn = By.cssSelector("button.sign-in-page-form-button");
    private final By signUpTodayLink = By.xpath("//a[@href='/price-plan' and contains(normalize-space(),'Sign Up Today')]");

    private final By pageTitle = By.cssSelector("h5.sign-in-title");
    private final By pageSubtitle = By.cssSelector("p.sign-in-page-subtitle");
    private final By emailLabel = By.cssSelector("label[for='email']");
    private final By passwordLabel = By.cssSelector("label[for='password']");
    private final By passwordEye = By.cssSelector("img.sign-up-password-eye, img.sign-up-password-eye-close");
    private final By forgotPasswordLink = By.cssSelector("a.sign-in-forgot-password");

    // react-hot-toast message (class names are random hashes, role="status" is stable)
    private final By toast = By.xpath("//div[@role='status'][normalize-space()]");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    // ===================================================================== existing (kept)

    @Step("Login to application")
    public void login(String user, String pass) {
        submitCredentials(user, pass);
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
     * Login is finished when we have left the sign-in page. The site does a full page load
     * after a successful login, so we also wait for the loader of the new page.
     */
    private void waitForLoginToComplete() {
        new WebDriverWait(driver, Duration.ofSeconds(Math.max(20, defaultTimeoutSeconds())))
                .until(d -> !d.getCurrentUrl().contains("/sign-in"));
        waitForPageReady(); // the page after login - reloads once if its spinner is stuck
    }

    // ======================================================================== page state

    @Step("Wait for sign-in page to load")
    public LoginPage waitForPageLoad() {
        wait.until(d -> d.getCurrentUrl().contains("/sign-in"));
        waitForPageReady(); // reloads once if the site's spinner is stuck
        waitForVisibility(emailField);
        return this;
    }

    /** True when the sign-in form is on screen right now (no waiting). */
    public boolean isDisplayed() {
        return driver.getCurrentUrl().contains("/sign-in")
                && isDisplayedNow(emailField)
                && isDisplayedNow(passwordField)
                && isDisplayedNow(signInBtn);
    }

    public String getTitle() {
        return waitForVisibility(pageTitle).getText().trim();
    }

    public String getSubtitle() {
        return waitForVisibility(pageSubtitle).getText().trim();
    }

    public String getEmailLabel() {
        return waitForVisibility(emailLabel).getText().trim();
    }

    public String getPasswordLabel() {
        return waitForVisibility(passwordLabel).getText().trim();
    }

    public String getPlaceholder(String field) {
        return waitForVisibility(field.equalsIgnoreCase("password") ? passwordField : emailField)
                .getDomAttribute("placeholder");
    }

    public boolean isSignInEnabled() {
        return waitForVisibility(signInBtn).isEnabled();
    }

    public boolean isForgotPasswordLinkDisplayed() {
        return isDisplayedNow(forgotPasswordLink);
    }

    public boolean isSignUpTodayLinkDisplayed() {
        return isDisplayedNow(signUpTodayLink);
    }

    /** True when the site has stored a login session (set only after a successful login). */
    public boolean isLoggedIn() {
        Object key = ((JavascriptExecutor) driver)
                .executeScript("return window.localStorage.getItem('session_key');");
        String v = String.valueOf(key);
        return key != null && !v.isBlank() && !"null".equals(v);
    }

    // ============================================================================ typing

    /** React-safe clear + type (element.clear() does not always update React state). */
    private void clearAndType(By locator, String value) {
        WebElement el = waitForVisibility(locator);
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        String left = el.getDomProperty("value");
        if (left != null) {
            for (int i = 0; i < left.length(); i++) {
                el.sendKeys(Keys.BACK_SPACE);
            }
        }
        if (value != null && !value.isEmpty()) {
            el.sendKeys(value);
        }
    }

    @Step("Enter username/email: {value}")
    public LoginPage enterUsername(String value) {
        clearAndType(emailField, value);
        return this;
    }

    @Step("Enter password")
    public LoginPage enterPassword(String value) {
        clearAndType(passwordField, value);
        return this;
    }

    public LoginPage fillCredentials(String user, String pass) {
        enterUsername(user);
        enterPassword(pass);
        return this;
    }

    public String getFieldValue(String field) {
        return waitForVisibility(field.equalsIgnoreCase("password") ? passwordField : emailField)
                .getDomProperty("value");
    }

    public String getPasswordFieldType() {
        return waitForVisibility(passwordField).getDomAttribute("type");
    }

    @Step("Click the show/hide password icon")
    public void togglePasswordVisibility() {
        String before = getPasswordFieldType();
        safeClick(waitForFirstDisplayed(passwordEye));
        wait.until(d -> !before.equals(getPasswordFieldType()));
    }

    // ============================================================================ submit

    @Step("Click Sign IN")
    public void clickSignIn() {
        safeClick(wait.until(ExpectedConditions.elementToBeClickable(signInBtn)));
    }

    /** Types the credentials and clicks Sign IN, WITHOUT waiting for success (used by negative tests). */
    @Step("Submit credentials: {user}")
    public void submitCredentials(String user, String pass) {
        waitForLoader();
        fillCredentials(user, pass);
        clickSignIn();
    }

    // ============================================================================ toasts

    /** Waits for a toast and returns its text. */
    public String getToastMessage() {
        WebElement t = new WebDriverWait(driver, Duration.ofSeconds(Math.max(15, defaultTimeoutSeconds())))
                .pollingEvery(Duration.ofMillis(200))
                .until(ExpectedConditions.visibilityOfElementLocated(toast));
        return t.getText().trim();
    }

    /** All toasts visible right now (the first one is kept even if it closes while we read). */
    public List<String> getAllToastMessages() {
        List<String> messages = new ArrayList<>();
        messages.add(getToastMessage());
        for (WebElement t : driver.findElements(toast)) {
            try {
                String text = t.isDisplayed() ? t.getText().trim() : "";
                if (!text.isEmpty() && !messages.contains(text)) {
                    messages.add(text);
                }
            } catch (StaleElementReferenceException ignored) {
                // toast closed while reading
            }
        }
        return messages;
    }

    /** Waits (max 8s) until no toast is shown, so the next test cannot read an old message. */
    public void waitForToastToClose() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .pollingEvery(Duration.ofMillis(200))
                    .until(ExpectedConditions.invisibilityOfElementLocated(toast));
        } catch (TimeoutException ignored) {
            // still visible - carry on
        }
    }

    // ============================================================================= reset

    /** Puts the already-open sign-in form back to empty without reloading the page. */
    @Step("Reset sign-in form (no page reload)")
    public LoginPage resetForm() {
        waitForToastToClose();
        if ("text".equals(getPasswordFieldType())) {
            togglePasswordVisibility();
        }
        enterUsername("");
        enterPassword("");
        scrollToTop();
        return this;
    }

    // ============================================================================= links

    @Step("Click 'Forgot Password?'")
    public void clickForgotPassword() {
        safeClick(waitForVisibility(forgotPasswordLink));
        wait.until(d -> d.getCurrentUrl().contains("/forgot-password"));
        waitForLoader();
    }

    /** Title on the forgot-password page ("Reset Password"). */
    public String getResetPasswordTitle() {
        return waitForVisibility(pageTitle).getText().trim();
    }
}
