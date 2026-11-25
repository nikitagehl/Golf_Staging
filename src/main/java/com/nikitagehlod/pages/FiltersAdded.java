package com.nikitagehlod.pages;

import com.nikitagehlod.Base.BasePage;
import io.qameta.allure.Description;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class FiltersAdded extends BasePage {

    private static final By FILTER_GROUPS = By.xpath("//div[@class='col filter-column']");
    private static final By CLOSE_LINK = By.xpath("//a[@aria-label='Close']");
    private static final By CHECKBOX = By.xpath(".//input[@type='checkbox']");

    private final Random random = new Random();

    public FiltersAdded(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    @Description("Select one random option in a random filter group")
    public void selectRandomFilter() {
        selectRandomFilters(1);
    }

    /**
     * Selects one random option in each of 'count' DIFFERENT filter groups.
     * (The old code picked a group at random 4 times, so it could hit the same checkbox twice
     * and untick it again.)
     */
    @Description("Verify the filter is selected successfully, and data loaded as per filters")
    public void selectRandomFilters(int count) {

        List<WebElement> groups = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(FILTER_GROUPS));

        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            indexes.add(i);
        }
        Collections.shuffle(indexes, random);

        int toPick = Math.min(count, indexes.size());
        for (int i = 0; i < toPick; i++) {
            selectRandomOptionInGroup(indexes.get(i));
        }
    }

    private void selectRandomOptionInGroup(int groupIndex) {

        // look the group up again every time so we never hold a stale element
        WebElement group = driver.findElements(FILTER_GROUPS).get(groupIndex);

        String filterTitle = group.getText().trim().isEmpty()
                ? "(unknown)"
                : group.getText().trim().split("\\R")[0];

        // only <li> items that really contain a checkbox (a title/header <li> has none)
        List<WebElement> options = group.findElements(By.xpath(".//li[.//input[@type='checkbox']]"));

        if (options.isEmpty()) {
            throw new RuntimeException("No options found under " + filterTitle);
        }

        // prefer an option that is not ticked yet, so the click always ADDS a filter
        List<WebElement> unchecked = options.stream()
                .filter(o -> !o.findElement(CHECKBOX).isSelected())
                .collect(Collectors.toList());
        List<WebElement> candidates = unchecked.isEmpty() ? options : unchecked;

        WebElement option = candidates.get(random.nextInt(candidates.size()));

        List<WebElement> spans = option.findElements(By.tagName("span"));
        String optionName = spans.isEmpty() ? option.getText() : spans.get(0).getText();

        WebElement checkbox = option.findElement(CHECKBOX);

        scrollIntoView(checkbox);
        jsClick(checkbox);   // JS click is the most reliable for custom checkboxes

        System.out.println("--------------------------------");
        System.out.println("Filter : " + filterTitle);
        System.out.println("Option : " + optionName);
        System.out.println("--------------------------------");
    }

    /**
     * Clicks the "Close" link of the filter panel (resets the filters).
     * The panel must be open: call RabbitHolePage.openFilterPanel() first.
     */
    @Description("Reset all filters")
    public void resetFilters() {

        WebElement close;
        try {
            // first DISPLAYED match: the same aria-label can exist on hidden elements elsewhere
            close = waitForFirstDisplayed(CLOSE_LINK);
        } catch (TimeoutException e) {
            throw new IllegalStateException(
                    "'Close' link (//a[@aria-label='Close']) is not visible. "
                            + "Is the filter panel open? Call RabbitHolePage.openFilterPanel() first.", e);
        }

        safeClick(close);
        waitForLoaderCycle();
    }
}
