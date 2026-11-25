package com.nikitagehlod.pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.nikitagehlod.Base.BasePage;

import io.qameta.allure.Step;

/**
 * /tournament-simulator (TOOLS > Tournament Simulation). Structure taken from the live page + its code (Nov 2026).
 *
 * The page has TWO states:
 *  1. Projections available (weekly, Tuesday until the field tees off on Thursday):
 *     "<Tournament> Betsperts Golf Projections", Download Projections (players-file.csv), search (icon opens
 *     a box, X clears it), table Player | Win % | Top 5 % | Top 10 % | Top 20 % | Top 30 % | Top 40 % | Make Cut,
 *     sorted by Win % high-to-low, 40 rows shown + "Load More" (+20), "No records found" for a search with no match.
 *  2. No projections: "Betsperts Golf Projections" + "Pre-tournament projections will be available weekly from
 *     Tuesday until the field tees off on Thursday."
 * Both states show "Latest Articles" and "DFS Golf Topics".
 * Premium tool: guests and free users are redirected to /subscribe?tab=tournament-simulator&next=...
 */
public class TournamentSimulatorPage extends BasePage {

    public static final String EMPTY_STATE_MESSAGE =
            "Pre-tournament projections will be available weekly from Tuesday until the field tees off on Thursday.";
    public static final String NO_RECORDS_TITLE = "No records found";
    public static final String NO_RECORDS_TEXT = "There is no record based on the details you entered. Please retry.";
    public static final String CSV_FILE_NAME = "players-file.csv";
    public static final int FIRST_PAGE_SIZE = 40;
    public static final int LOAD_MORE_STEP = 20;

    private final By pageTitle = By.cssSelector("section.t-simulator .league-title");

    // empty state (no projections) - the "no records" box also has .empty-data, but with .border-top-0
    private final By emptyState = By.cssSelector("section.t-simulator .projection-border.empty-data:not(.border-top-0)");
    private final By emptyStateMessage = By.cssSelector("section.t-simulator .projection-border.empty-data:not(.border-top-0) p");
    private final By emptyStateImage = By.cssSelector("section.t-simulator .projection-border.empty-data:not(.border-top-0) img");

    // projections state
    private final By table = By.cssSelector("section.t-simulator table");
    private final By headerCells = By.cssSelector("section.t-simulator table thead th");
    private final By allRows = By.cssSelector("section.t-simulator table tbody tr");
    private final By downloadLink = By.xpath("//section[contains(@class,'t-simulator')]//a[contains(normalize-space(),'Download Projections')]");
    private final By searchToggle = By.cssSelector("section.t-simulator button.btn-search");
    private final By searchBox = By.cssSelector("section.t-simulator .search-items input.search-input");
    private final By searchClear = By.cssSelector("section.t-simulator img.search-close-icon");
    private final By loadMore = By.cssSelector("section.t-simulator .load-more a");
    private final By noRecordsBox = By.cssSelector("section.t-simulator .projection-border.border-top-0.empty-data");

    // side content (both states)
    private final By latestArticlesHeading = By.xpath("//div[contains(@class,'latest-article')]//h2[normalize-space()='Latest Articles']");
    private final By articleTitleLinks = By.cssSelector(".latest-article a.mob-text-lines-2");
    private final By seeAllLink = By.cssSelector(".latest-article a[href='/fantasy-golf']");
    private final By topicsHeading = By.xpath("//div[contains(@class,'cmn-topic-wrapper')]//h3[normalize-space()='DFS Golf Topics']");
    private final By topicLinks = By.cssSelector(".cmn-topic-wrapper a[href^='/topic/']");

    public TournamentSimulatorPage(WebDriver driver) {
        super(driver);
    }

    // ============================================================== loading / state

    /** URL + spinner gone + EITHER the projections table OR the empty-state box shown. */
    @Step("Wait for Tournament Simulation to load")
    public TournamentSimulatorPage waitForPageLoad() {
        wait.until(d -> d.getCurrentUrl().contains("/tournament-simulator"));
        waitForPageReady();
        // the projections come from a (sometimes very slow) server call - no spinner after the first one
        new WebDriverWait(driver, Duration.ofSeconds(pageContentTimeoutSeconds()))
                .pollingEvery(Duration.ofMillis(300))
                .until(d -> hasProjections() || isEmptyStateShown());
        return this;
    }

    /** True when the projections table has player rows. */
    public boolean hasProjections() {
        return isDisplayedNow(table) && !driver.findElements(allRows).isEmpty();
    }

    public boolean isEmptyStateShown() {
        return isDisplayedNow(emptyState);
    }

    public String getPageTitle() {
        return waitForVisibility(pageTitle).getText().trim();
    }

    public String getEmptyStateMessage() {
        return waitForVisibility(emptyStateMessage).getText().trim();
    }

    public boolean isEmptyStateImageShown() {
        for (WebElement img : driver.findElements(emptyStateImage)) {
            if (img.isDisplayed() && "Betsperts Golf Projections".equals(img.getDomAttribute("alt"))) {
                return true;
            }
        }
        return false;
    }

    public boolean isTableShown() {
        return isDisplayedNow(table);
    }

    public boolean isDownloadLinkShown() {
        return isDisplayedNow(downloadLink);
    }

    public boolean isSearchIconShown() {
        return isDisplayedNow(searchToggle);
    }

    // ========================================================================= table

    public List<String> getHeaders() {
        List<String> heads = new ArrayList<>();
        for (WebElement th : driver.findElements(headerCells)) {
            heads.add(String.valueOf(th.getDomProperty("textContent")).replaceAll("\\s+", " ").trim());
        }
        return heads;
    }

    /** Rows currently shown (rows after the first 40 are in the page but hidden with d-none). */
    public int getVisibleRowCount() {
        Object n = js("return Array.from(document.querySelectorAll('section.t-simulator table tbody tr'))"
                + ".filter(function(r){ return !r.classList.contains('d-none'); }).length;");
        return ((Number) n).intValue();
    }

    /** All rows of the current list (shown + hidden). */
    public int getTotalRowCount() {
        return driver.findElements(allRows).size();
    }

    /**
     * Every row as text: [player, win, top5, top10, top20, top30, top40, makeCut] (values like "12.34%").
     * Read in ONE JavaScript call - also covers the hidden (d-none) rows.
     */
    @SuppressWarnings("unchecked")
    public List<List<String>> getAllRowValues(boolean visibleOnly) {
        Object result = js("var onlyVisible = arguments[0];"
                + "return Array.from(document.querySelectorAll('section.t-simulator table tbody tr'))"
                + ".filter(function(r){ return !onlyVisible || !r.classList.contains('d-none'); })"
                + ".map(function(r){"
                + "  var cells = [r.querySelector('th').textContent.trim()];"
                + "  r.querySelectorAll('td').forEach(function(td){ cells.push(td.textContent.trim()); });"
                + "  return cells; });", visibleOnly);
        return result == null ? new ArrayList<>() : (List<List<String>>) result;
    }

    /** Values of one column (0 = player, 1 = Win %, ... 7 = Make Cut) for all rows of the current list. */
    public List<String> getColumn(int index, boolean visibleOnly) {
        List<String> col = new ArrayList<>();
        for (List<String> row : getAllRowValues(visibleOnly)) {
            col.add(index < row.size() ? row.get(index) : "");
        }
        return col;
    }

    /** href of each player link ("/golfer/first-last"), shown rows only. */
    @SuppressWarnings("unchecked")
    public List<String> getPlayerLinks() {
        Object result = js("return Array.from(document.querySelectorAll('section.t-simulator table tbody tr'))"
                + ".filter(function(r){ return !r.classList.contains('d-none'); })"
                + ".map(function(r){ var a = r.querySelector('th a'); return a ? a.getAttribute('href') : ''; });");
        return result == null ? new ArrayList<>() : (List<String>) result;
    }

    @Step("Open player page of the first player")
    public String openFirstPlayer() {
        WebElement link = waitForFirstDisplayed(By.cssSelector("section.t-simulator table tbody tr th a"));
        String href = link.getDomAttribute("href");
        safeClick(link);
        wait.until(d -> d.getCurrentUrl().contains("/golfer/"));
        return href;
    }

    /** Clicks a column header (exact text, e.g. "Win %", "Player") to sort by it. */
    @Step("Sort by {header}")
    public void sortBy(String header) {
        for (WebElement th : driver.findElements(headerCells)) {
            String text = String.valueOf(th.getDomProperty("textContent")).replaceAll("\\s+", " ").trim();
            if (text.equals(header)) {
                List<WebElement> target = th.findElements(By.cssSelector("span[class], div.ms-2, span"));
                safeClick(target.isEmpty() ? th : target.get(0));
                waitForLoader();
                return;
            }
        }
        throw new IllegalArgumentException("No column '" + header + "'. Headers: " + getHeaders());
    }

    // ===================================================================== load more

    public boolean isLoadMoreShown() {
        return isDisplayedNow(loadMore);
    }

    @Step("Click Load More")
    public void clickLoadMore() {
        int before = getVisibleRowCount();
        safeClick(waitForVisibility(loadMore));
        wait.until(d -> getVisibleRowCount() > before);
    }

    // ======================================================================== search

    @Step("Open the search box")
    public void openSearch() {
        if (!isDisplayedNow(searchBox)) {
            safeClick(waitForVisibility(searchToggle));
            waitForVisibility(searchBox);
        }
    }

    @Step("Search for '{text}'")
    public void search(String text) {
        openSearch();
        WebElement box = waitForVisibility(searchBox);
        box.click();
        box.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        if (text != null && !text.isEmpty()) {
            box.sendKeys(text);
        }
        waitForStableRowCount();
    }

    public String getSearchText() {
        List<WebElement> boxes = driver.findElements(searchBox);
        return boxes.isEmpty() ? "" : String.valueOf(boxes.get(0).getDomProperty("value"));
    }

    public boolean isSearchClearIconShown() {
        return isDisplayedNow(searchClear);
    }

    /** Clicks the X in the search box. */
    @Step("Clear the search with the X icon")
    public void clearSearchWithIcon() {
        openSearch();
        safeClick(waitForVisibility(searchClear));
        waitForStableRowCount();
    }

    public boolean isNoRecordsShown() {
        return isDisplayedNow(noRecordsBox);
    }

    public String getNoRecordsText() {
        return waitForVisibility(noRecordsBox).getText().replaceAll("\\s+", " ").trim();
    }

    private void waitForStableRowCount() {
        final int[] last = {-1};
        new WebDriverWait(driver, Duration.ofSeconds(10)).pollingEvery(Duration.ofMillis(400)).until(d -> {
            int now = getVisibleRowCount() + (isDisplayedNow(noRecordsBox) ? 100000 : 0);
            boolean stable = now == last[0];
            last[0] = now;
            return stable;
        });
    }

    // =========================================================================== CSV

    public String getCsvFileName() {
        return waitForPresence(downloadLink).getDomAttribute("download");
    }

    /** Reads the CSV that "Download Projections" would save (blob/data URL), without saving a file. */
    public List<String> readCsvLines() {
        String href = waitForPresence(downloadLink).getDomProperty("href");
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

    // =================================================================== side content

    public boolean isLatestArticlesShown() {
        return isDisplayedNow(latestArticlesHeading);
    }

    public List<String> getArticleLinks() {
        List<String> hrefs = new ArrayList<>();
        for (WebElement a : driver.findElements(articleTitleLinks)) {
            hrefs.add(a.getDomAttribute("href"));
        }
        return hrefs;
    }

    @Step("Click 'See All' in Latest Articles")
    public void clickSeeAll() {
        safeClick(waitForVisibility(seeAllLink));
        wait.until(d -> d.getCurrentUrl().contains("/fantasy-golf"));
    }

    public boolean isTopicsShown() {
        return isDisplayedNow(topicsHeading);
    }

    public List<String> getTopicLinks() {
        List<String> hrefs = new ArrayList<>();
        for (WebElement a : driver.findElements(topicLinks)) {
            hrefs.add(a.getDomAttribute("href"));
        }
        return hrefs;
    }

    // ========================================================================= util

    private Object js(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }
}
