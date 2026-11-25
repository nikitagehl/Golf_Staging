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
 * /live-leaderboard (TOOLS > Live Leaderboard). Locators checked on devbeta, Nov 2026.
 *
 * Page layout:
 *  - info block: tournament name (h1), course + location, PAR, YARDAGE
 *  - search box, stat tabs (SG, OTT, APP, ARG, PUTT, Scoring, MISC, Course Metrics)
 *  - rounds (Tournament, R1-R4), "Display Results as" (Rank / Average / Total)
 *  - "Show Favorites Only (n)", "Reset Favorites", "View as" (Full Leaderboard / Player Groups)
 *  - Download CSV (a blob link, file name Live-leaderboard.csv)
 *  - table: first column = star + player, every other cell has the column key as class
 *    (Score, Total, FinishPos, SGTOT, SGOTT ...)
 *  - logged-out / free users see a "You're Missing Out!" paywall instead of the table
 */
public class LiveLeaderboardPage extends BasePage {

    public static final String NO_RECORDS_MESSAGE = "No records match your current search and filters";

    // ---------------------------------------------------------------- info block
    private final By tournamentName = By.cssSelector(".info-block h1");
    private final By courseLocation = By.cssSelector(".info-block > div > p");
    private final By infoValues = By.cssSelector(".info-block .d-flex p");

    // ------------------------------------------------------------------ controls
    private final By searchBox = By.cssSelector("input.search-input");
    private final By statTabs = By.cssSelector("ul.round-list-filter li");
    private final By roundButtons = By.cssSelector(".lead-stroke-switch button");
    private final By displayOptions = By.cssSelector("ul.display-radio-btns li");
    private final By favoritesOnlyCheckbox = By.id("showFavorite");
    private final By favoritesOnlyLabel = By.cssSelector("label[for='showFavorite']");
    private final By downloadCsvLink = By.cssSelector("a[download]");

    // --------------------------------------------------------------------- table
    private final By table = By.cssSelector("table.table");
    private final By headerCells = By.cssSelector("table thead th");
    private final By bodyRows = By.cssSelector("table tbody tr");
    private final By noRecords = By.xpath("//h5[contains(normalize-space(),'No records match')]");
    private final By favoriteStar = By.cssSelector("img.player-star");

    // ------------------------------------------------------------------- paywall
    private final By paywall = By.cssSelector(".leaderboard-paid-content-banner");
    private final By upgradeButton = By.cssSelector("button.unlock-btn");
    private final By paywallLoginLink = By.cssSelector(".paid-container a[href^='/sign-in']");

    public LiveLeaderboardPage(WebDriver driver) {
        super(driver);
    }

    // ============================================================== page loading

    /** Waits for the page: URL, spinner gone, and either the table or the paywall shown. */
    @Step("Wait for Live Leaderboard to load")
    public LiveLeaderboardPage waitForPageLoad() {
        wait.until(d -> d.getCurrentUrl().contains("/live-leaderboard"));
        waitForPageReady();
        new WebDriverWait(driver, Duration.ofSeconds(Math.max(30, defaultTimeoutSeconds())))
                .until(d -> isDisplayedNow(table) || isDisplayedNow(paywall));
        return this;
    }

    /** After a tab / round / display / view change: give the spinner time to come and go. */
    private void waitForTableUpdate() {
        waitForLoaderCycle();
        new WebDriverWait(driver, Duration.ofSeconds(Math.max(30, defaultTimeoutSeconds())))
                .until(d -> isDisplayedNow(table) || isDisplayedNow(noRecords) || isDisplayedNow(paywall));
    }

    public boolean isTableDisplayed() {
        return isDisplayedNow(table);
    }

    // ================================================================ info block

    public String getTournamentName() {
        return waitForVisibility(tournamentName).getText().trim();
    }

    public String getCourseLocation() {
        return waitForVisibility(courseLocation).getText().trim();
    }

    /** Value shown after a label in the info row, e.g. getInfoValue("PAR") -> "70". */
    public String getInfoValue(String label) {
        List<WebElement> ps = driver.findElements(infoValues);
        for (int i = 0; i < ps.size() - 1; i++) {
            if (ps.get(i).getText().trim().equalsIgnoreCase(label)) {
                return ps.get(i + 1).getText().trim();
            }
        }
        return "";
    }

    // ================================================================== options

    private WebElement optionByText(By group, String text) {
        return wait.until(d -> {
            for (WebElement e : d.findElements(group)) {
                try {
                    if (e.getText().trim().equalsIgnoreCase(text)) {
                        return e;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // re-rendered - try again
                }
            }
            return null;
        });
    }

    private boolean isSelected(By group, String text) {
        String cls = optionByText(group, text).getDomAttribute("class");
        return cls != null && cls.contains("selected");
    }

    /** The selected option of a group, e.g. the active stat tab. */
    private String selectedOf(By group) {
        for (WebElement e : driver.findElements(group)) {
            String cls = e.getDomAttribute("class");
            if (cls != null && cls.contains("selected")) {
                return e.getText().trim();
            }
        }
        return "";
    }

    private void choose(By group, String text) {
        WebElement option = optionByText(group, text);
        String cls = option.getDomAttribute("class");
        if (cls != null && cls.contains("selected")) {
            return; // already active - clicking again would not reload anything
        }
        safeClick(option);
        wait.until(d -> isSelected(group, text));
        waitForTableUpdate();
    }

    public List<String> getStatTabNames() {
        List<String> names = new ArrayList<>();
        for (WebElement e : driver.findElements(statTabs)) {
            names.add(e.getText().trim());
        }
        return names;
    }

    @Step("Open stat tab {tab}")
    public void selectStatTab(String tab) {
        choose(statTabs, tab);
    }

    public String getSelectedStatTab() {
        return selectedOf(statTabs);
    }

    @Step("Select round {round}")
    public void selectRound(String round) {
        choose(roundButtons, round);
    }

    public String getSelectedRound() {
        return selectedOf(roundButtons);
    }

    /** "Display Results as": Rank / Average / Total. */
    @Step("Display results as {mode}")
    public void selectDisplayMode(String mode) {
        choose(displayOptions, mode);
    }

    /** "View as": Full Leaderboard / Player Groups. */
    @Step("View as {view}")
    public void selectView(String view) {
        choose(displayOptions, view);
    }

    public boolean isOptionSelected(String text) {
        for (By group : new By[] {statTabs, roundButtons, displayOptions}) {
            for (WebElement e : driver.findElements(group)) {
                if (e.getText().trim().equalsIgnoreCase(text)) {
                    String cls = e.getDomAttribute("class");
                    return cls != null && cls.contains("selected");
                }
            }
        }
        return false;
    }

    // =================================================================== search

    @Step("Search for '{text}'")
    public void search(String text) {
        WebElement box = waitForVisibility(searchBox);
        box.click();
        box.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        if (text != null && !text.isEmpty()) {
            box.sendKeys(text);
        }
        // filtering is done in the browser - wait until the table/message has settled
        waitForStableRowCount();
    }

    @Step("Clear the search box")
    public void clearSearch() {
        search("");
    }

    public String getSearchText() {
        return waitForVisibility(searchBox).getDomProperty("value");
    }

    public String getSearchPlaceholder() {
        return waitForVisibility(searchBox).getDomAttribute("placeholder");
    }

    public boolean isNoRecordsMessageShown() {
        return isDisplayedNow(noRecords);
    }

    public String getNoRecordsMessage() {
        return waitForVisibility(noRecords).getText().trim();
    }

    /** Row count must be the same on two checks 400ms apart (search filters while typing). */
    private void waitForStableRowCount() {
        final int[] last = {-1};
        new WebDriverWait(driver, Duration.ofSeconds(10)).pollingEvery(Duration.ofMillis(400)).until(d -> {
            int now = d.findElements(bodyRows).size();
            boolean stable = now == last[0];
            last[0] = now;
            return stable;
        });
    }

    // ================================================================= favorites

    /** Number in "Show Favorites Only (9)", or -1 when the label has no number. */
    public int getFavoritesCountFromLabel() {
        String text = waitForVisibility(favoritesOnlyLabel).getText();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\((\\d+)\\)").matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** False when the checkbox is not on the page (the Course Metrics tab hides it). */
    public boolean isFavoritesOnlyChecked() {
        List<WebElement> boxes = driver.findElements(favoritesOnlyCheckbox);
        return !boxes.isEmpty() && boxes.get(0).isSelected();
    }

    /** "Show Favorites Only" exists only on the player tabs, not on Course Metrics. */
    public boolean isFavoritesOnlyAvailable() {
        return isDisplayedNow(favoritesOnlyCheckbox) || isDisplayedNow(favoritesOnlyLabel);
    }

    /** True when an option of "Display Results as" / "View as" (e.g. "Rank") is on screen. */
    public boolean isDisplayOptionShown(String text) {
        for (WebElement e : driver.findElements(displayOptions)) {
            if (e.isDisplayed() && e.getText().trim().equalsIgnoreCase(text)) {
                return true;
            }
        }
        return false;
    }

    /** Ticks / unticks "Show Favorites Only" (a view filter - it does not change the account). */
    @Step("Set 'Show Favorites Only' to {checked}")
    public void setFavoritesOnly(boolean checked) {
        if (!isFavoritesOnlyAvailable()) {
            if (checked) {
                throw new IllegalStateException("'Show Favorites Only' is not shown on the '"
                        + getSelectedStatTab() + "' tab.");
            }
            return; // nothing to untick
        }
        if (isFavoritesOnlyChecked() != checked) {
            safeClick(waitForVisibility(favoritesOnlyLabel));
            wait.until(d -> isFavoritesOnlyChecked() == checked);
            waitForStableRowCount();
        }
    }

    /** Number of rows whose star is the filled (favorite) star. */
    public int getStarredRowCount() {
        int count = 0;
        for (WebElement row : driver.findElements(bodyRows)) {
            List<WebElement> stars = row.findElements(favoriteStar);
            if (!stars.isEmpty()) {
                String src = stars.get(0).getDomAttribute("src");
                if (src != null && src.contains("/Star.svg")) {
                    count++;
                }
            }
        }
        return count;
    }

    // ===================================================================== table

    /** Column headers as shown (line breaks removed). The first one is "FavPlayer" / "Player". */
    public List<String> getHeaders() {
        List<String> heads = new ArrayList<>();
        for (WebElement th : driver.findElements(headerCells)) {
            heads.add(String.valueOf(th.getDomProperty("textContent")).replaceAll("\\s+", " ").trim());
        }
        return heads;
    }

    /** Header tooltip text (data-tooltip-content) of a column, "" when it has none. */
    public String getHeaderTooltip(String header) {
        for (WebElement span : driver.findElements(By.cssSelector("table thead [data-tooltip-content]"))) {
            if (String.valueOf(span.getDomProperty("textContent")).trim().equals(header)) {
                return span.getDomAttribute("data-tooltip-content");
            }
        }
        return "";
    }

    public int getRowCount() {
        return driver.findElements(bodyRows).size();
    }

    public int getTableCount() {
        return driver.findElements(table).size();
    }

    /** Rows per table (Player Groups shows one table per group). */
    public List<Integer> getRowsPerTable() {
        List<Integer> sizes = new ArrayList<>();
        for (WebElement t : driver.findElements(table)) {
            sizes.add(t.findElements(By.cssSelector("tbody tr")).size());
        }
        return sizes;
    }

    /** Player names of all rows (first cell). */
    public List<String> getPlayerNames() {
        return columnTexts(By.cssSelector("td:first-child"));
    }

    /** Cell texts of one column by its key class: Score, Total, FinishPos, SGTOT, SGOTT ... */
    public List<String> getColumnValues(String columnKey) {
        return columnTexts(By.cssSelector("td." + columnKey));
    }

    /** Reads a column in ONE JavaScript call (146 rows x findElement would be slow). */
    @SuppressWarnings("unchecked")
    private List<String> columnTexts(By cellInRow) {
        String css = cellInRow.toString().replaceFirst("^By\\.cssSelector:\\s*", "");
        Object result = ((JavascriptExecutor) driver).executeScript(
                "var sel = arguments[0];" // keep it: inside map(), 'arguments' is map's own arguments
                        + "return Array.from(document.querySelectorAll('table tbody tr')).map(function(r){"
                        + " var c = r.querySelector(sel); return c ? c.innerText.trim() : ''; });", css);
        return result == null ? new ArrayList<>() : new ArrayList<>((List<String>) result);
    }

    /** Clicks a column header to sort by it (first click = high to low, next click flips). */
    @Step("Sort by column {header}")
    public void sortBy(String header) {
        for (WebElement th : driver.findElements(headerCells)) {
            String text = String.valueOf(th.getDomProperty("textContent")).replaceAll("\\s+", " ").trim();
            if (text.equals(header)) {
                List<WebElement> clickable = th.findElements(By.cssSelector("div[style*='cursor']"));
                safeClick(clickable.isEmpty() ? th : clickable.get(0));
                waitForStableRowCount();
                return;
            }
        }
        throw new IllegalArgumentException("No column header '" + header + "'. Headers: " + getHeaders());
    }

    // ==================================================================== CSV

    public String getCsvFileName() {
        return waitForPresence(downloadCsvLink).getDomAttribute("download");
    }

    /**
     * Reads the CSV the "Download CSV" link would save, without saving a file:
     * the link is a blob: URL built in the browser, so we fetch it from the page.
     */
    public List<String> readCsvLines() {
        String href = waitForPresence(downloadCsvLink).getDomProperty("href");
        Object text = ((JavascriptExecutor) driver).executeAsyncScript(
                "var done = arguments[arguments.length - 1];"
                        + "fetch(arguments[0]).then(function(r){ return r.text(); })"
                        + ".then(function(t){ done(t); }).catch(function(e){ done('ERROR: ' + e); });", href);
        List<String> lines = new ArrayList<>();
        for (String line : String.valueOf(text).trim().split("\\r?\\n")) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    // ================================================================== paywall

    public boolean isPaywallDisplayed() {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOfElementLocated(paywall)).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    public String getPaywallText() {
        return waitForVisibility(paywall).getText();
    }

    public boolean isUpgradeButtonDisplayed() {
        return isDisplayedNow(upgradeButton);
    }

    @Step("Click 'Log in' on the paywall")
    public void clickPaywallLogin() {
        safeClick(waitForVisibility(paywallLoginLink));
    }
}
