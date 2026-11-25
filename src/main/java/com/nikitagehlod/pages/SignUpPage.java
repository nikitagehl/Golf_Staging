package com.nikitagehlod.pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.nikitagehlod.Base.BasePage;

import io.qameta.allure.Step;

/**
 * /sign-up/{planId}/false page.
 *
 * How the site behaves (read from the site's own code, Nov 2026):
 *  - CONTINUE stays disabled until Username, Email and Password all have a value.
 *  - Spaces are removed from Username and Email while typing.
 *  - On CONTINUE the site checks, in this order, and shows a toast:
 *      "Username is required!", "Email is required!", "Invalid Email Address!", "Password is required!"
 *  - Server errors (username/email already taken, ...) are shown as a toast too.
 *  - On success the user is sent to /subscription?subscription_id=...&email=...
 */
public class SignUpPage extends BasePage {

    public static final String MSG_USERNAME_REQUIRED = "Username is required!";
    public static final String MSG_EMAIL_REQUIRED = "Email is required!";
    public static final String MSG_INVALID_EMAIL = "Invalid Email Address!";
    public static final String MSG_PASSWORD_REQUIRED = "Password is required!";

    /** Order summary can take ~20s to update after CHANGE PLAN (slow plan-list API on devbeta). */
    private static final int PLAN_SWITCH_TIMEOUT_SECONDS = 40;

    private final By usernameField = By.id("username");
    private final By emailField = By.cssSelector("input.sign-up-input#email");
    private final By passwordField = By.cssSelector("input.sign-up-input#password");
    private final By continueBtn = By.cssSelector("button.sign-up-btn");
    private final By passwordEye = By.cssSelector("img.sign-up-password-eye, img.sign-up-password-eye-close");

    private final By usernameLabel = By.cssSelector("label[for='username']");
    private final By emailLabel = By.cssSelector("label[for='email']");
    private final By passwordLabel = By.cssSelector("label[for='password']");

    private final By selectedPlan = By.cssSelector(".card-subscribe-plan-type");
    private final By changePlanBtn = By.cssSelector("button.plan-change-btn");
    private final By planOptions = By.cssSelector(".dropdown-menu.show .dropdown-item");
    private final By backToPlans = By.cssSelector("a.back-plan");

    private final By termsLink = By.xpath("//span[contains(@class,'sign-up-terms-dark') and normalize-space()='Terms of Use']/parent::a");
    private final By privacyLink = By.xpath("//span[contains(@class,'sign-up-terms-dark') and normalize-space()='Privacy Policy']/parent::a");

    // The site uses react-hot-toast: class names are random hashes (go3958317564 ...),
    // so the stable hook is the role="status" message div. It disappears after a few seconds.
    private final By toast = By.xpath("//div[@role='status'][normalize-space()]");

    public SignUpPage(WebDriver driver) {
        super(driver);
    }

    // ================================================================================ state

    @Step("Wait for sign-up page to load")
    public SignUpPage waitForPageLoad() {
        wait.until(d -> d.getCurrentUrl().contains("/sign-up"));
        waitForLoader();
        waitForVisibility(usernameField);
        return this;
    }

    public boolean isDisplayed() {
        return driver.getCurrentUrl().contains("/sign-up")
                && isDisplayedNow(usernameField)
                && isDisplayedNow(emailField)
                && isDisplayedNow(passwordField)
                && isDisplayedNow(continueBtn);
    }

    public boolean areLabelsDisplayed() {
        return waitForVisibility(usernameLabel).getText().trim().equalsIgnoreCase("Username")
                && waitForVisibility(emailLabel).getText().trim().equalsIgnoreCase("Email")
                && waitForVisibility(passwordLabel).getText().trim().equalsIgnoreCase("Password");
    }

    public String getPlaceholder(String field) {
        return waitForVisibility(fieldLocator(field)).getDomAttribute("placeholder");
    }

    public boolean isContinueEnabled() {
        return waitForVisibility(continueBtn).isEnabled();
    }

    public String getContinueText() {
        return waitForVisibility(continueBtn).getText().trim();
    }

    // ================================================================================ typing

    private By fieldLocator(String field) {
        switch (field.toLowerCase()) {
            case "username":
                return usernameField;
            case "email":
                return emailField;
            case "password":
                return passwordField;
            default:
                throw new IllegalArgumentException("Unknown sign-up field: " + field);
        }
    }

    /**
     * React-safe clear + type. element.clear() does not always tell React the value changed,
     * so we select-all + delete with the keyboard instead.
     */
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

    @Step("Enter username: {username}")
    public SignUpPage enterUsername(String username) {
        clearAndType(usernameField, username);
        return this;
    }

    @Step("Enter email: {email}")
    public SignUpPage enterEmail(String email) {
        clearAndType(emailField, email);
        return this;
    }

    @Step("Enter password")
    public SignUpPage enterPassword(String password) {
        clearAndType(passwordField, password);
        return this;
    }

    @Step("Fill sign-up form")
    public SignUpPage fillForm(String username, String email, String password) {
        enterUsername(username);
        enterEmail(email);
        enterPassword(password);
        return this;
    }

    public String getFieldValue(String field) {
        return waitForVisibility(fieldLocator(field)).getDomProperty("value");
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

    // ================================================================================ submit

    @Step("Click CONTINUE")
    public void clickContinue() {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(continueBtn));
        safeClick(btn);
    }

    /** Waits for a toast message and returns its text. */
    public String getToastMessage() {
        WebElement t = new WebDriverWait(driver, Duration.ofSeconds(15))
                .pollingEvery(Duration.ofMillis(200))
                .until(ExpectedConditions.visibilityOfElementLocated(toast));
        return t.getText().trim();
    }

    /** Collects the text of every toast that is visible right now (several can be stacked). */
    public List<String> getAllToastMessages() {
        List<String> messages = new ArrayList<>();
        messages.add(getToastMessage()); // waits for at least one - kept even if it closes right after
        for (WebElement t : driver.findElements(toast)) {
            try {
                String text = t.isDisplayed() ? t.getText().trim() : "";
                if (!text.isEmpty() && !messages.contains(text)) {
                    messages.add(text);
                }
            } catch (org.openqa.selenium.StaleElementReferenceException ignored) {
                // toast closed while we were reading it
            }
        }
        return messages;
    }

    /** Submits the form and waits until we land on the /subscription (payment) page. */
    @Step("Submit sign-up and wait for subscription page")
    public void submitAndWaitForSubscriptionPage() {
        clickContinue();
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(d -> d.getCurrentUrl().contains("/subscription"));
        waitForLoader();
    }

    // ================================================================================= reset

    /**
     * Puts the already-open form back to a clean state WITHOUT reloading the page:
     * waits for any toast to close (so an old message can't be read by the next test),
     * closes the Change Plan dropdown, hides the password and empties all three fields.
     */
    @Step("Reset sign-up form (no page reload)")
    public SignUpPage resetForm() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .pollingEvery(Duration.ofMillis(200))
                    .until(ExpectedConditions.invisibilityOfElementLocated(toast));
        } catch (org.openqa.selenium.TimeoutException ignored) {
            // toast is still showing after 8s - carry on, next test checks its own text
        }
        closeChangePlanDropdown();
        if ("text".equals(getPasswordFieldType())) {
            togglePasswordVisibility();
        }
        enterUsername("");
        enterEmail("");
        enterPassword("");
        scrollToTop();
        return this;
    }

    /** Closes the Change Plan dropdown if it is open (Esc, then a second click as fallback). */
    public void closeChangePlanDropdown() {
        if (!isDisplayedNow(planOptions)) {
            return;
        }
        waitForVisibility(changePlanBtn).sendKeys(Keys.ESCAPE);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2))
                    .until(ExpectedConditions.invisibilityOfElementLocated(planOptions));
        } catch (org.openqa.selenium.TimeoutException e) {
            safeClick(waitForVisibility(changePlanBtn)); // the button toggles the menu
        }
    }

    // ================================================================================== plan

    public String getSelectedPlan() {
        return waitForVisibility(selectedPlan).getText().trim();
    }

    /** Opens CHANGE PLAN and returns the option texts, e.g. "Yearly | $249.99". */
    @Step("Open Change Plan dropdown")
    public List<String> openChangePlanAndGetOptions() {
        safeClick(waitForVisibility(changePlanBtn));
        waitForFirstDisplayed(planOptions);
        List<String> options = new ArrayList<>();
        for (WebElement o : driver.findElements(planOptions)) {
            options.add(o.getText().trim());
        }
        return options;
    }

    /** Opens CHANGE PLAN and picks the option that contains planName (case-insensitive). */
    @Step("Change plan to {planName}")
    public void changePlanTo(String planName) {
        String urlBefore = driver.getCurrentUrl();
        closeChangePlanDropdown(); // the button toggles, so make sure we start from "closed"
        safeClick(waitForVisibility(changePlanBtn));
        By option = By.xpath("//div[contains(@class,'dropdown-menu') and contains(@class,'show')]"
                + "//*[contains(@class,'dropdown-item') and contains(translate(normalize-space(),"
                + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + planName.toLowerCase() + "')]");
        safeClick(waitForVisibility(option));
        wait.until(d -> !d.getCurrentUrl().equals(urlBefore));
        waitForLoader();
        // The site re-loads the whole plan list from the server after a plan change. Usually <1s,
        // but measured up to ~20s on devbeta (no loader is shown), so the summary gets a long wait.
        new WebDriverWait(driver, Duration.ofSeconds(PLAN_SWITCH_TIMEOUT_SECONDS))
                .pollingEvery(Duration.ofMillis(300))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                .until(d -> getSelectedPlan().equalsIgnoreCase(planName));
    }

    @Step("Click 'Back to All Plans'")
    public PricePlanPage clickBackToPlans() {
        safeClick(waitForVisibility(backToPlans));
        PricePlanPage plans = new PricePlanPage(driver);
        plans.waitForPageLoad();
        return plans;
    }

    // ================================================================================= links

    /** Clicks Terms of Use / Privacy Policy (opens a new tab) and returns that tab's URL. */
    @Step("Open {linkName} from the sign-up form")
    public String openLegalLinkAndGetUrl(String linkName) {
        By link = linkName.toLowerCase().contains("privacy") ? privacyLink : termsLink;
        String main = driver.getWindowHandle();
        Set<String> before = driver.getWindowHandles();

        safeClick(waitForVisibility(link));
        switchToNewWindow(before);
        wait.until(d -> !d.getCurrentUrl().equals("about:blank"));
        String url = driver.getCurrentUrl();

        closeAllWindowsExcept(main);
        return url;
    }
}
