package com.nikitagehlod.pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.nikitagehlod.Base.BasePage;

import io.qameta.allure.Step;

/**
 * /course-stats/{course} (TOOLS > Course Database). Checked on devbeta, Nov 2026.
 *
 * - Course picker (react-select, "Select Course", ~120 courses A-Z): choosing a course changes the URL to
 *   /course-stats/{slug} and reloads the data (no full page load).
 * - Tournament name, "Course Facts" (label .detail-title + values .tornament-title), "Correlated Courses".
 * - Hole Stats: 18 rows, Hole | Par | Yards | AVG | AVG +/- | Rank | Eagles | Birdies | Pars | Bogeys | Doubles/+,
 *   every header is a sort button (1st click low-to-high, 2nd reverses).
 * - Course Metrics: category dropdown (11 categories) + table: 5-Year Course AVG, PGA Tour AVG, last 5 years.
 * - Past Leaderboards - {tournament}: list of years (winner + score, selected = .active), LEADERBOARD / All Metrics,
 *   "Player Search", leaderboard table (Pos Players | Score | R1..R4), players link to /golfer/...
 *   A search with no match shows "No records found" and the leaderboard table disappears.
 * - Logged-out and FREE users stay on the page but get a subscriber banner (.main-paid-container,
 *   "<course>'s 12 key stats and correlated courses are for subscribers") instead of Hole Stats,
 *   Course Metrics and Past Leaderboards.
 */
public class CourseDatabasePage extends BasePage {

    public static final String NO_RECORDS_TITLE = "No records found";
    public static final String NO_RECORDS_TEXT = "There is no record based on the details you entered. Please retry.";

    private final By section = By.cssSelector("section.section-tstats");
    private final By coursePickerValue = By.cssSelector("section.section-tstats [class*='playerName__single-value']");
    private final By coursePickerControl = By.cssSelector("section.section-tstats [class*='playerName__control']");
    private final By coursePickerInput = By.cssSelector("section.section-tstats input.playerName__input");
    private final By pickerOptions = By.cssSelector("[class*='playerName__option']");
    private final By pickerNotice = By.cssSelector("[class*='playerName__menu-notice']");
    private final By tournamentName = By.cssSelector("section.section-tstats .fw-bolder.f20");
    private final By factTitles = By.cssSelector("section.section-tstats .detail-title");
    private final By metricsDropdown = By.cssSelector("section.section-tstats button.grey_b_btn");
    private final By openDropdownItems = By.cssSelector("section.section-tstats .dropdown-menu.show .dropdown-item");
    private final By yearItems = By.cssSelector("section.section-tstats .list-item");
    private final By pastLeaderboardTitle = By.xpath("//section[contains(@class,'section-tstats')]//p[contains(@class,'league-title') and starts-with(normalize-space(),'Past Leaderboards')]");
    private final By playerSearch = By.cssSelector("section.section-tstats input.search-input");

    // subscriber banner shown to logged-out / free users
    private final By paidBanner = By.cssSelector(".main-paid-container");
    private final By paidBannerHeading = By.cssSelector(".main-paid-container .heading");
    private final By paidBannerSubText = By.cssSelector(".main-paid-container .sub-text");
    private final By paidBannerSubscribe = By.cssSelector(".main-paid-container .subscribe-btn");

    public CourseDatabasePage(WebDriver driver) {
        super(driver);
    }

    /** For logged-out / free users: URL + spinner gone + subscriber banner shown (no course data to wait for). */
    @Step("Wait for the Course Database subscriber banner")
    public CourseDatabasePage waitForSubscriberBanner() {
        wait.until(d -> d.getCurrentUrl().contains("/course-stats/"));
        waitForPageReady();
        waitForPageContent(paidBanner, "Course Database subscriber banner");
        return this;
    }

    public boolean isSubscriberBannerShown() {
        return isDisplayedNow(paidBanner);
    }

    public String getSubscriberBannerHeading() {
        return waitForVisibility(paidBannerHeading).getText().trim();
    }

    public String getSubscriberBannerSubText() {
        return waitForVisibility(paidBannerSubText).getText().trim();
    }

    public boolean isSubscribeButtonShown() {
        return isDisplayedNow(paidBannerSubscribe);
    }

    public boolean isPastLeaderboardsShown() {
        return isDisplayedNow(pastLeaderboardTitle);
    }

    // ================================================================== loading

    /** URL + spinner gone + course facts and the hole table shown (data comes from slow server calls). */
    @Step("Wait for Course Database to load")
    public CourseDatabasePage waitForPageLoad() {
        wait.until(d -> d.getCurrentUrl().contains("/course-stats/"));
        waitForPageReady();
        new WebDriverWait(driver, Duration.ofSeconds(pageContentTimeoutSeconds()))
                .pollingEvery(Duration.ofMillis(300))
                .until(d -> !d.findElements(factTitles).isEmpty() && getHoleRows().size() > 0);
        return this;
    }

    public boolean hasCourseContent() {
        return !driver.findElements(factTitles).isEmpty();
    }

    // ============================================================ header / facts

    public String getSelectedCourse() {
        List<WebElement> v = driver.findElements(coursePickerValue);
        return v.isEmpty() ? "" : v.get(0).getText().trim();
    }

    public String getTournamentName() {
        return waitForVisibility(tournamentName).getText().trim();
    }

    /** All Course Facts as label -> values ("Par 70 | 7005 yds | Short"), in page order. */
    @SuppressWarnings("unchecked")
    public Map<String, String> getCourseFacts() {
        Object r = js("var out = [];"
                + "document.querySelectorAll('section.section-tstats .detail-title').forEach(function(t){"
                + "  var v = t.nextElementSibling;"
                + "  var parts = v ? Array.from(v.querySelectorAll('div')).filter(function(d){ return !d.querySelector('div'); })"
                + "      .map(function(d){ return d.textContent.trim(); }) : [];"
                + "  if (v && parts.length === 0) { parts = [v.textContent.trim()]; }"
                + "  out.push([t.textContent.trim(), parts.join(' | ')]); });"
                + "return out;");
        Map<String, String> facts = new LinkedHashMap<>();
        for (List<String> pair : (List<List<String>>) r) {
            facts.put(pair.get(0), pair.get(1));
        }
        return facts;
    }

    /** The "Correlated Courses" names. */
    public List<String> getCorrelatedCourses() {
        String text = getCourseFacts().getOrDefault("Correlated Courses", "");
        List<String> names = new ArrayList<>();
        for (String n : text.split(",")) {
            if (!n.isBlank()) {
                names.add(n.trim());
            }
        }
        return names;
    }

    // ================================================================== tables

    /**
     * Finds a table by the text of its first header: "Hole" (Hole Stats), "year" (Course Metrics),
     * "Pos" (Past Leaderboards). Returns the rows as cell texts.
     */
    @SuppressWarnings("unchecked")
    private List<List<String>> tableRows(String firstHeaderStartsWith) {
        Object r = js("var want = arguments[0].toLowerCase();"
                + "var t = Array.from(document.querySelectorAll('section.section-tstats table')).find(function(t){"
                + "  var th = t.querySelector('thead th'); return th && th.textContent.trim().toLowerCase().indexOf(want) === 0; });"
                + "if (!t) return [];"
                + "return Array.from(t.querySelectorAll('tbody tr')).map(function(tr){"
                + "  return Array.from(tr.children).map(function(td){ return td.textContent.replace(/\\s+/g,' ').trim(); }); });",
                firstHeaderStartsWith);
        return r == null ? new ArrayList<>() : (List<List<String>>) r;
    }

    @SuppressWarnings("unchecked")
    private List<String> tableHeaders(String firstHeaderStartsWith) {
        Object r = js("var want = arguments[0].toLowerCase();"
                + "var t = Array.from(document.querySelectorAll('section.section-tstats table')).find(function(t){"
                + "  var th = t.querySelector('thead th'); return th && th.textContent.trim().toLowerCase().indexOf(want) === 0; });"
                + "if (!t) return [];"
                + "return Array.from(t.querySelectorAll('thead th')).map(function(th){ return th.textContent.replace(/\\s+/g,' ').trim(); });",
                firstHeaderStartsWith);
        return r == null ? new ArrayList<>() : (List<String>) r;
    }

    // -------------------------------------------------------------- hole stats

    public List<String> getHoleHeaders() {
        return tableHeaders("Hole");
    }

    /** Rows: [hole, par, yards, avg, avg+/-, rank, eagles%, birdies%, pars%, bogeys%, doubles%]. */
    public List<List<String>> getHoleRows() {
        return tableRows("Hole");
    }

    public List<String> getHoleColumn(int index) {
        List<String> col = new ArrayList<>();
        for (List<String> row : getHoleRows()) {
            col.add(row.get(index));
        }
        return col;
    }

    /** Clicks a Hole Stats header button (e.g. "AVG", "Yards") to sort by it. */
    @Step("Sort Hole Stats by {header}")
    public void sortHolesBy(String header) {
        WebElement button = wait.until(d -> {
            for (WebElement b : d.findElements(By.cssSelector("section.section-tstats table thead button"))) {
                if (b.getText().trim().equals(header)) {
                    return b;
                }
            }
            return null;
        });
        safeClick(button);
        waitForLoader();
    }

    // ---------------------------------------------------------- course metrics

    public String getMetricsCategory() {
        return visibleMetricsDropdown().getText().trim();
    }

    private WebElement visibleMetricsDropdown() {
        return wait.until(d -> {
            for (WebElement b : d.findElements(metricsDropdown)) {
                if (b.isDisplayed()) {
                    return b;
                }
            }
            return null;
        });
    }

    public List<String> getMetricsCategories() {
        safeClick(visibleMetricsDropdown());
        waitForFirstDisplayed(openDropdownItems);
        List<String> items = new ArrayList<>();
        for (WebElement i : driver.findElements(openDropdownItems)) {
            items.add(i.getText().trim());
        }
        visibleMetricsDropdown().sendKeys(Keys.ESCAPE);
        if (isDisplayedNow(openDropdownItems)) {
            safeClick(visibleMetricsDropdown());
        }
        return items;
    }

    @Step("Course Metrics category: {category}")
    public void selectMetricsCategory(String category) {
        if (getMetricsCategory().equals(category)) {
            return;
        }
        safeClick(visibleMetricsDropdown());
        WebElement item = wait.until(d -> {
            for (WebElement i : d.findElements(openDropdownItems)) {
                if (i.isDisplayed() && i.getText().trim().equals(category)) {
                    return i;
                }
            }
            return null;
        });
        safeClick(item);
        wait.until(d -> getMetricsCategory().equals(category));
        waitForLoaderCycle();
    }

    public List<String> getMetricsHeaders() {
        return tableHeaders("year");
    }

    /** First column of the metrics table: "5-Year Course AVG", "PGA Tour AVG", 2026, 2025 ... */
    public List<String> getMetricsRowLabels() {
        List<String> labels = new ArrayList<>();
        for (List<String> row : tableRows("year")) {
            labels.add(row.get(0));
        }
        return labels;
    }

    // ------------------------------------------------------- past leaderboards

    public String getPastLeaderboardsTitle() {
        return waitForVisibility(pastLeaderboardTitle).getText().trim();
    }

    /** Years in the list, each as [year, winner, score] e.g. ["2025", "Sami Valimaki", "-23"]. */
    @SuppressWarnings("unchecked")
    public List<List<String>> getPastYears() {
        Object r = js("return Array.from(document.querySelectorAll('section.section-tstats .list-item')).map(function(li){"
                + "  var year = (li.querySelector('.player-title') || {}).textContent || '';"
                + "  var rest = li.textContent.replace(year, '').trim();"
                + "  var m = rest.match(/^(.*)\\(([^)]*)\\)\\s*$/);"
                + "  return [year.trim(), m ? m[1].trim() : rest, m ? m[2].trim() : '']; });");
        return r == null ? new ArrayList<>() : (List<List<String>>) r;
    }

    /** Year of the list item marked .active ("" if none). */
    public String getActiveYear() {
        for (WebElement li : driver.findElements(yearItems)) {
            if (String.valueOf(li.getDomAttribute("class")).matches(".*(^|\\s)active(\\s|$).*")) {
                return li.findElement(By.cssSelector(".player-title")).getText().trim();
            }
        }
        return "";
    }

    /** Clicks a year in Past Leaderboards and waits until its leaderboard is shown. */
    @Step("Show the {year} leaderboard")
    public void selectYear(String year) {
        WebElement item = wait.until(d -> {
            for (WebElement li : d.findElements(yearItems)) {
                if (li.getText().trim().startsWith(year)) {
                    return li;
                }
            }
            return null;
        });
        scrollIntoView(item);
        // a REAL click on the link inside the item (a JavaScript click does not switch the year)
        item.findElement(By.cssSelector("a")).click();
        new WebDriverWait(driver, Duration.ofSeconds(pageContentTimeoutSeconds()))
                .pollingEvery(Duration.ofMillis(300))
                .until(d -> year.equals(getActiveYear()));
        waitForLoaderCycle();
    }

    /** LEADERBOARD / All Metrics switch. */
    @Step("Past Leaderboards view: {view}")
    public void selectLeaderboardView(String view) {
        WebElement button = wait.until(d -> {
            for (WebElement b : d.findElements(By.cssSelector("section.section-tstats button"))) {
                if (b.isDisplayed() && b.getText().trim().equalsIgnoreCase(view)) {
                    return b;
                }
            }
            return null;
        });
        safeClick(button);
        wait.until(d -> String.valueOf(button.getDomAttribute("class")).contains("selected"));
        waitForLoaderCycle();
    }

    public List<String> getLeaderboardHeaders() {
        return tableHeaders("Pos");
    }

    /** Leaderboard rows as cell texts; first cell is "position player", e.g. "1 Marco Penge". */
    public List<List<String>> getLeaderboardRows() {
        return tableRows("Pos");
    }

    /** Player names of the leaderboard (link texts). */
    @SuppressWarnings("unchecked")
    public List<String> getLeaderboardPlayers() {
        Object r = js("var t = Array.from(document.querySelectorAll('section.section-tstats table')).find(function(t){"
                + "  var th = t.querySelector('thead th'); return th && th.textContent.trim().indexOf('Pos') === 0; });"
                + "if (!t) return [];"
                + "return Array.from(t.querySelectorAll('tbody tr a[href^=\"/golfer/\"]')).map(function(a){ return a.textContent.trim(); });");
        return r == null ? new ArrayList<>() : (List<String>) r;
    }

    @SuppressWarnings("unchecked")
    public List<String> getLeaderboardPlayerLinks() {
        Object r = js("return Array.from(document.querySelectorAll('section.section-tstats table tbody tr a[href^=\"/golfer/\"]'))"
                + ".map(function(a){ return a.getAttribute('href') + '|' + (a.getAttribute('target') || ''); });");
        return r == null ? new ArrayList<>() : (List<String>) r;
    }

    @Step("Player Search: '{text}'")
    public void searchPlayer(String text) {
        // Past Leaderboards loads after the rest of the page (slow on devbeta) - wait for its search box
        WebElement box = new WebDriverWait(driver, Duration.ofSeconds(pageContentTimeoutSeconds()))
                .pollingEvery(Duration.ofMillis(300))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                .until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(playerSearch));
        scrollIntoView(box);
        box.click();
        box.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        if (!text.isEmpty()) {
            box.sendKeys(text);
        }
        final int[] last = {-2};
        new WebDriverWait(driver, Duration.ofSeconds(10)).pollingEvery(Duration.ofMillis(400)).until(d -> {
            int now = getLeaderboardRows().size() + (isNoRecordsShown() ? 100000 : 0);
            boolean stable = now == last[0];
            last[0] = now;
            return stable;
        });
    }

    public String getPlayerSearchText() {
        List<WebElement> box = driver.findElements(playerSearch);
        return box.isEmpty() ? "" : String.valueOf(box.get(0).getDomProperty("value"));
    }

    public String getPlayerSearchPlaceholder() {
        return waitForVisibility(playerSearch).getDomAttribute("placeholder");
    }

    public boolean isNoRecordsShown() {
        Object r = js("return Array.from(document.querySelectorAll('section.section-tstats h5'))"
                + ".some(function(h){ return h.offsetParent && h.textContent.trim() === 'No records found'; });");
        return Boolean.TRUE.equals(r);
    }

    public String getNoRecordsText() {
        Object r = js("var h = Array.from(document.querySelectorAll('section.section-tstats h5'))"
                + ".find(function(h){ return h.textContent.trim() === 'No records found'; });"
                + "return h ? h.parentElement.textContent.replace(/\\s+/g,' ').trim() : '';");
        return String.valueOf(r);
    }

    // ============================================================ course picker

    /** Opens the course picker, types the text and returns the options shown. */
    @Step("Type '{text}' in the course picker")
    public List<String> typeInCoursePicker(String text) {
        WebElement control = waitForVisibility(coursePickerControl);
        scrollIntoView(control);
        safeClick(control);
        WebElement input = driver.findElement(coursePickerInput);
        openMenuIfClosed(input);
        clearPickerSearch(input);
        if (!text.isEmpty()) {
            input.sendKeys(text);
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(d -> !d.findElements(pickerOptions).isEmpty() || !d.findElements(pickerNotice).isEmpty());
        } catch (TimeoutException ignored) {
            // menu did not open - empty list
        }
        return getPickerOptions();
    }

    /**
     * The click on the picker does not always open its menu (it only reacts to a real mouse press, not to
     * the JavaScript click fallback) - that made TC_CD_18 see 0 courses. If no menu after 2s, press Down
     * in the search box, which always opens it without changing the selected course.
     */
    private void openMenuIfClosed(WebElement input) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2))
                    .until(d -> !d.findElements(pickerOptions).isEmpty() || !d.findElements(pickerNotice).isEmpty());
        } catch (TimeoutException notOpen) {
            input.sendKeys(Keys.ARROW_DOWN);
        }
    }

    /**
     * Empties the picker's SEARCH text only (Backspace once per typed character, never on an empty box,
     * so the selected course is never touched).
     */
    private static void clearPickerSearch(WebElement input) {
        String typed = String.valueOf(input.getDomProperty("value"));
        for (int i = 0; i < typed.length() && !"null".equals(typed); i++) {
            input.sendKeys(Keys.BACK_SPACE);
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getPickerOptions() {
        Object r = js("return Array.from(document.querySelectorAll(\"[class*='playerName__option']\"))"
                + ".map(function(o){ return o.textContent.trim(); });");
        return r == null ? new ArrayList<>() : (List<String>) r;
    }

    public String getPickerNotice() {
        List<WebElement> n = driver.findElements(pickerNotice);
        return n.isEmpty() ? "" : n.get(0).getText().trim();
    }

    public void closeCoursePicker() {
        List<WebElement> input = driver.findElements(coursePickerInput);
        if (!input.isEmpty()) {
            input.get(0).sendKeys(Keys.ESCAPE);
        }
    }

    /** Chooses a course by exact name and waits until its data is shown. */
    @Step("Switch to course {course}")
    public void selectCourse(String course) {
        String factsBefore = getCourseFacts().toString();
        typeInCoursePicker(course);
        WebElement option = wait.until(d -> {
            for (WebElement o : d.findElements(pickerOptions)) {
                if (o.getText().trim().equals(course)) {
                    return o;
                }
            }
            return null;
        });
        safeClick(option);
        new WebDriverWait(driver, Duration.ofSeconds(pageContentTimeoutSeconds()))
                .pollingEvery(Duration.ofMillis(400))
                .until(d -> course.equals(getSelectedCourse()));
        waitForLoaderCycle();
        new WebDriverWait(driver, Duration.ofSeconds(pageContentTimeoutSeconds()))
                .pollingEvery(Duration.ofMillis(400))
                .until(d -> getHoleRows().size() > 0 && hasCourseContent());
        // the new course's data comes from a slow server call - wait until the facts really changed
        try {
            new WebDriverWait(driver, Duration.ofSeconds(pageContentTimeoutSeconds()))
                    .pollingEvery(Duration.ofMillis(500))
                    .until(d -> !getCourseFacts().toString().equals(factsBefore));
        } catch (TimeoutException sameFacts) {
            // two courses can have identical facts - carry on, the test checks what it needs
        }
    }

    // ===================================================================== util

    private Object js(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }
}
