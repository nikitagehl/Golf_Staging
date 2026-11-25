package com.nikitagehlod.pages;

import com.nikitagehlod.Base.BasePage;
import io.qameta.allure.Description;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import java.util.List;
import java.util.stream.Collectors;

public class EnterWeightage extends BasePage {

    private static final By WEIGHT_INPUTS = By.xpath("//div[@class='weight-inp-box']//input");
    private static final By TOTAL_100 = By.xpath("//input[@readonly and contains(@value,'100.00')]");

    public EnterWeightage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    /** All weight inputs the user can type in (the read-only "total" box is excluded). */
    private List<WebElement> getEditableWeightFields() {
        return driver.findElements(WEIGHT_INPUTS).stream()
                .filter(e -> e.getDomAttribute("readonly") == null && e.isEnabled())
                .collect(Collectors.toList());
    }

    /** Waits until the number of editable fields is > 0 and stops changing. */
    private int waitForStableFieldCount() {
        int[] last = {-1};
        wait.until(d -> {
            int now = getEditableWeightFields().size();
            boolean stable = now > 0 && now == last[0];
            last[0] = now;
            return stable;
        });
        return last[0];
    }

    @Description("Enter the weight on input field")
    public void enterDynamicWeightage() {

        waitForLoader();

        // The old code did findElements() straight away. If the fields were not rendered yet
        // the list was empty and the maths below went wrong (100 / -1).
        int fieldCount = waitForStableFieldCount();

        int baseValue = 100 / fieldCount;
        int remainder = 100 % fieldCount;

        for (int i = 0; i < fieldCount; i++) {

            int value = baseValue;
            if (i == fieldCount - 1) {
                value += remainder;   // last field takes the remainder so the total is 100
            }

            // find the field again each time: typing can re-render the list
            WebElement input = getEditableWeightFields().get(i);

            safeClick(input);
            input.clear();
            input.sendKeys(String.valueOf(value));
        }

        // wait until the total box shows 100.00
        wait.until(d -> !d.findElements(TOTAL_100).isEmpty());
    }

    @Description("Verify total weightage should be 100")
    public String VerifyTotalWeight() {
        return waitForVisibility(TOTAL_100).getAttribute("value");
    }
}
