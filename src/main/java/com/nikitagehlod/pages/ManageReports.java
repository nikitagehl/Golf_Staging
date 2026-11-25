package com.nikitagehlod.pages;

import com.nikitagehlod.Base.BasePage;
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
import java.util.stream.Collectors;

public class ManageReports extends BasePage {

    private static final String ROWS_XPATH =
            "//div[normalize-space()='Manage Models']"
                    + "/following-sibling::div[contains(@class,'table-report-setting')]//tbody/tr";

    private static final By ROWS = By.xpath(ROWS_XPATH);

    /** Reports created by the automation contain this text; they are preferred for delete/copy/toggle. */
    private static final String AUTOMATION_TAG = "Automation";

    private final Random random = new Random();

    public ManageReports(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public void goToModelsTable() {

        WebElement manageModels = waitForPresence(
                By.xpath("//div[normalize-space()='Manage Models']"));

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant', block:'start'});",
                manageModels);
    }

    private List<WebElement> getReportRows() {

        goToModelsTable();

        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(ROWS));
    }

    /**
     * Picks a random row. Rows created by the automation ("Automation Report ...") are
     * preferred so the test does not delete / change real reports of the account.
     */
    private WebElement pickRandomRow(List<WebElement> rows) {

        List<WebElement> own = rows.stream()
                .filter(r -> getReportName(r).contains(AUTOMATION_TAG))
                .collect(Collectors.toList());

        List<WebElement> pool = own.isEmpty() ? rows : own;

        return pool.get(random.nextInt(pool.size()));
    }

    private String getReportName(WebElement row) {
        return row.findElement(By.xpath("./td[2]")).getText();
    }

    private void clickActionIcon(WebElement row, String iconName) {

        WebElement icon = row.findElement(By.xpath(".//img[@alt='" + iconName + "']"));

        safeClick(icon);
    }

    public void DeleteReport() {

        waitForLoader();

        List<WebElement> rows = getReportRows();
        int beforeCount = rows.size();

        WebElement row = pickRandomRow(rows);

        System.out.println("Deleting : " + getReportName(row));

        clickActionIcon(row, "Remove");

        // Copy asks "Yes / No" before it runs - if Delete does the same, confirm it
        confirmDialogIfShown();

        wait.until(ExpectedConditions.numberOfElementsToBeLessThan(ROWS, beforeCount));

        System.out.println("Report deleted successfully.");
    }

    /** Clicks "Yes" in the confirmation popup when one appears within 3s; does nothing otherwise. */
    private void confirmDialogIfShown() {
        By yesButton = By.xpath("//div[@class='modal-footer']//button[normalize-space()='Yes']");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.elementToBeClickable(yesButton))
                    .click();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[@role='dialog']")));
            waitForLoader();
        } catch (org.openqa.selenium.TimeoutException noDialog) {
            // no confirmation popup for this action - nothing to do
        }
    }

    public void CopyReport() {

        waitForLoader();

        WebElement row = pickRandomRow(getReportRows());

        System.out.println("Copying : " + getReportName(row));

        clickActionIcon(row, "Copy");

        wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//div[@class='modal-footer']//button[normalize-space()='Yes']")))
                .click();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.xpath("//div[@role='dialog']")));

        waitForLoader();

        System.out.println("Report copied successfully.");
    }

    public void ChangeReportStatus() {

        waitForLoader();

        WebElement row = pickRandomRow(getReportRows());

        System.out.println("Changing Status : " + getReportName(row));

        WebElement toggle = row.findElement(By.xpath(".//label[contains(@class,'switch')]"));

        safeClick(toggle);
    }
}
