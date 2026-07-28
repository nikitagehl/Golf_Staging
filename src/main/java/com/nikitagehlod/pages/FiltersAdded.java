package com.nikitagehlod.pages;

import com.nikitagehlod.Base.BasePage;
import io.qameta.allure.Description;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Random;

public class FiltersAdded extends BasePage {


    WebDriver driver;
    WebDriverWait wait;

    public FiltersAdded(WebDriver driver){
        super(driver);
        this.driver= driver;
        PageFactory.initElements(driver, this);
        wait = new WebDriverWait(driver,
                Duration.ofSeconds(5));
    }

    @Description("Verify the filter is selected successfull, and data loaded as per filters ")
    public void selectRandomFilter() {

        Random random = new Random();

        // Get all filter groups
        List<WebElement> filterGroups = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.xpath("//div[@class='col filter-column']")
                ));

        // Pick one random filter group
        WebElement group = filterGroups.get(random.nextInt(filterGroups.size()));

        // Get filter title
        String filterTitle = group.findElement(By.tagName("ul")).getText().split("\n")[0];

        // Get all options of selected filter
        List<WebElement> options = group.findElements(By.xpath(".//li"));

        if (options.isEmpty()) {
            throw new RuntimeException("No options found under " + filterTitle);
        }

        // Pick random option
        WebElement option = options.get(random.nextInt(options.size()));

        String optionName = option.findElement(By.tagName("span")).getText();

        WebElement checkbox = option.findElement(By.xpath(".//input[@type='checkbox']"));

        // Scroll into view
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", checkbox);

        wait.until(ExpectedConditions.elementToBeClickable(checkbox));

        // Click using JS (more reliable for checkboxes)
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", checkbox);

        System.out.println("--------------------------------");
        System.out.println("Filter : " + filterTitle);
        System.out.println("Option : " + optionName);
        System.out.println("--------------------------------");
    }

    @Description("Reset all filters")
    public void resetFilters(){
        WebElement reset = driver.findElement(By.xpath("//a[@aria-label=\"Close\"]"));
        reset.click();

        waitForLoader();
    }
}


