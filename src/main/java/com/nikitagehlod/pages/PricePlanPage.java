package com.nikitagehlod.pages;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.nikitagehlod.Base.BasePage;

import io.qameta.allure.Step;

/** /price-plan page - the plan cards with the GET PLAN buttons that lead to sign-up. */
public class PricePlanPage extends BasePage {

    private final By heading = By.tagName("h1");
    private final By planTitles = By.cssSelector("h5.card-plan-title");
    private final By getPlanButtons = By.cssSelector("button.plan-button");

    public PricePlanPage(WebDriver driver) {
        super(driver);
    }

    /** GET PLAN button inside the card whose title is planName (Weekly, Monthly, Yearly). */
    private By getPlanButtonFor(String planName) {
        return By.xpath("//h5[contains(@class,'card-plan-title') and normalize-space()='" + planName + "']"
                + "/ancestor::div[contains(@class,'card-plan')][1]//button[contains(@class,'plan-button')]");
    }

    @Step("Wait for price plan page to load")
    public PricePlanPage waitForPageLoad() {
        wait.until(d -> d.getCurrentUrl().contains("/price-plan"));
        waitForLoader();
        waitForFirstDisplayed(getPlanButtons);
        return this;
    }

    public boolean isDisplayed() {
        try {
            waitForPageLoad();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getHeading() {
        return waitForVisibility(heading).getText().trim();
    }

    public List<String> getPlanNames() {
        waitForFirstDisplayed(planTitles);
        List<String> names = new ArrayList<>();
        for (WebElement title : driver.findElements(planTitles)) {
            String text = title.getText().trim();
            if (!text.isEmpty()) {
                names.add(text);
            }
        }
        return names;
    }

    public int getVisibleGetPlanButtonCount() {
        int count = 0;
        for (WebElement b : driver.findElements(getPlanButtons)) {
            if (b.isDisplayed()) {
                count++;
            }
        }
        return count;
    }

    /** Clicks GET PLAN on the given plan card and returns the sign-up page. */
    @Step("Select plan {planName}")
    public SignUpPage selectPlan(String planName) {
        WebElement button = waitForPresence(getPlanButtonFor(planName));
        safeClick(button);
        SignUpPage signUpPage = new SignUpPage(driver);
        signUpPage.waitForPageLoad();
        return signUpPage;
    }
}
