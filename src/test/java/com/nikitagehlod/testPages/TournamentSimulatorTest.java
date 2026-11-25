package com.nikitagehlod.testPages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.nikitagehlod.basetest.commonToAllTest;
import com.nikitagehlod.pages.HomePage;
import com.nikitagehlod.pages.LoginPage;
import com.nikitagehlod.pages.TournamentSimulatorPage;
import com.nikitagehlod.utils.SiteUrls;
import com.nikitagehlod.utils.propertyReader;

import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

/**
 * TOOLS > Tournament Simulation - positive and edge cases.
 *
 * Flow: sign in with a valid USERNAME (paid user) -> hover TOOLS -> click "Tournament Simulation".
 *
 * The page has two states (see TournamentSimulatorPage):
 *  - projections published (weekly, Tuesday until Thursday's tee-off) -> TC_TS_P.. tests run
 *  - no projections -> the empty-state message -> TC_TS_E.. tests run
 * Tests for the state that is NOT on screen are SKIPPED with the reason, so every run checks
 * whatever the site shows that day. Logged-out / free-user tests run LAST (they end the session).
 */
@Feature("Tournament Simulation")
public class TournamentSimulatorTest extends commonToAllTest {

    private static final List<String> COLUMNS = List.of(
            "Player", "Win %", "Top 5 %", "Top 10 %", "Top 20 %", "Top 30 %", "Top 40 %", "Make Cut");

    private HomePage homePage;
    private LoginPage loginPage;
    private TournamentSimulatorPage simulator;

    @BeforeClass(alwaysRun = true)
    public void loginAndOpenTournamentSimulation() {
        homePage = new HomePage(driver);
        loginPage = new LoginPage(driver);
        simulator = new TournamentSimulatorPage(driver);

        if (!driver.getCurrentUrl().startsWith(SiteUrls.base())) {
            driver.get(SiteUrls.of("/")); // setup's first load did not reach the site (slow devbeta)
        }

        Allure.step("Sign in with a valid username (paid user)");
        homePage.waitForPageLoad();
        homePage.clickLogin();
        loginPage.login(propertyReader.readKey("paid.username").trim(),
                propertyReader.readKey("paid.password").trim());

        Allure.step("Hover TOOLS and click Tournament Simulation");
        homePage.openToolsMenuItem("Tournament Simulation");
        simulator.waitForPageLoad();

        Allure.step(simulator.hasProjections()
                ? "Projections ARE published - running the projections tests"
                : "No projections this week - running the empty-state tests");
    }

    // ============================================================================= helpers

    /** Back on the simulator page with an empty search, without a reload when we are already there. */
    private void backToSimulator() {
        if (!driver.getCurrentUrl().contains("/tournament-simulator")) {
            driver.get(SiteUrls.of("/tournament-simulator"));
            simulator.waitForPageLoad();
        }
        if (simulator.hasProjections() || simulator.isNoRecordsShown()) {
            if (!simulator.getSearchText().isEmpty()) {
                simulator.search("");
            }
        }
    }

    private void requireProjections() {
        backToSimulator();
        if (!simulator.hasProjections()) {
            throw new SkipException("No projections published right now (they are available from Tuesday "
                    + "until the field tees off on Thursday) - projections test skipped.");
        }
    }

    private void requireEmptyState() {
        backToSimulator();
        if (!simulator.isEmptyStateShown()) {
            throw new SkipException("Projections are published right now - empty-state test skipped.");
        }
    }

    private static double pct(String value) {
        return Double.parseDouble(value.replace("%", "").trim());
    }

    private static boolean isSorted(List<Double> values, boolean descending) {
        for (int i = 1; i < values.size(); i++) {
            int cmp = Double.compare(values.get(i - 1), values.get(i));
            if (descending ? cmp < 0 : cmp > 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSortedText(List<String> values, boolean descending) {
        for (int i = 1; i < values.size(); i++) {
            int cmp = values.get(i - 1).compareToIgnoreCase(values.get(i));
            if (descending ? cmp < 0 : cmp > 0) {
                return false;
            }
        }
        return true;
    }

    private WebDriverWait waitFor(int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }

    // =================================================================== POSITIVE: both states

    @Test(priority = 1, description = "TC_TS_01: TOOLS > Tournament Simulation opens the simulator for a paid user")
    @Severity(SeverityLevel.BLOCKER)
    public void verifyOpenFromToolsMenu() {
        Assert.assertTrue(driver.getCurrentUrl().contains("/tournament-simulator"),
                "Paid user should stay on the simulator, but is on: " + driver.getCurrentUrl());
        Assert.assertFalse(driver.getCurrentUrl().contains("/subscribe"), "Paid user must not be sent to /subscribe.");
        Assert.assertTrue(driver.getTitle().contains("Tournament Simulator"), "Browser title: " + driver.getTitle());
    }

    @Test(priority = 2, description = "TC_TS_02: Page shows exactly ONE state - projections OR the 'not available' message")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyExactlyOneState() {
        backToSimulator();
        boolean projections = simulator.hasProjections();
        boolean empty = simulator.isEmptyStateShown();
        Allure.step("Projections: " + projections + ", empty state: " + empty);
        Assert.assertTrue(projections ^ empty, "Exactly one state expected (projections=" + projections
                + ", empty=" + empty + ").");
    }

    @Test(priority = 3, description = "TC_TS_03: Title contains 'Betsperts Golf Projections'")
    public void verifyPageTitle() {
        backToSimulator();
        String title = simulator.getPageTitle();
        Allure.step("Title: " + title);
        Assert.assertTrue(title.endsWith("Betsperts Golf Projections"), "Page title: '" + title + "'");
    }

    @Test(priority = 4, description = "TC_TS_04: Latest Articles shows article links and 'See All' opens all articles")
    public void verifyLatestArticles() {
        backToSimulator();
        Assert.assertTrue(simulator.isLatestArticlesShown(), "'Latest Articles' section missing.");
        List<String> links = simulator.getArticleLinks();
        Allure.step("Articles: " + links.size());
        Assert.assertFalse(links.isEmpty(), "No articles listed.");
        Assert.assertTrue(links.stream().allMatch(h -> h != null && h.matches("/[a-z-]+/[a-z0-9-]+")),
                "Article links should look like /golf-betting/<article>: " + links);

        simulator.clickSeeAll();
        Assert.assertTrue(driver.getCurrentUrl().contains("/fantasy-golf"));
        backToSimulator();
    }

    @Test(priority = 5, description = "TC_TS_05: DFS Golf Topics shows topic links")
    public void verifyDfsTopics() {
        backToSimulator();
        Assert.assertTrue(simulator.isTopicsShown(), "'DFS Golf Topics' section missing.");
        List<String> topics = simulator.getTopicLinks();
        Assert.assertFalse(topics.isEmpty(), "No topic links.");
        Assert.assertTrue(topics.stream().allMatch(h -> h.startsWith("/topic/")), "Topic links: " + topics);
    }

    // =================================================================== EMPTY STATE (no projections)

    @Test(priority = 10, description = "TC_TS_E01: No projections - message, picture and no table/search/download")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyEmptyState() {
        requireEmptyState();
        SoftAssert soft = new SoftAssert();
        soft.assertEquals(simulator.getEmptyStateMessage(), TournamentSimulatorPage.EMPTY_STATE_MESSAGE, "Message");
        soft.assertEquals(simulator.getPageTitle(), "Betsperts Golf Projections", "Title without a tournament name");
        soft.assertTrue(simulator.isEmptyStateImageShown(), "'No projections' picture missing.");
        soft.assertFalse(simulator.isTableShown(), "No table expected.");
        soft.assertFalse(simulator.isDownloadLinkShown(), "'Download Projections' should be hidden.");
        soft.assertFalse(simulator.isSearchIconShown(), "Search should be hidden.");
        soft.assertAll();
    }

    // =================================================================== PROJECTIONS STATE

    @Test(priority = 20, description = "TC_TS_P01: Title starts with the tournament name")
    public void verifyTitleHasTournamentName() {
        requireProjections();
        String title = simulator.getPageTitle();
        String tournament = title.replace("Betsperts Golf Projections", "").trim();
        Allure.step("Tournament: " + tournament);
        Assert.assertFalse(tournament.isEmpty(), "Title should start with the tournament name: '" + title + "'");
    }

    @Test(priority = 21, description = "TC_TS_P02: Table has the 8 columns in order")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyColumns() {
        requireProjections();
        Assert.assertEquals(simulator.getHeaders(), COLUMNS, "Columns");
    }

    @Test(priority = 22, description = "TC_TS_P03: Every shown row has a player link and 7 percentages between 0 and 100")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyRowValues() {
        requireProjections();
        List<List<String>> rows = simulator.getAllRowValues(true);
        List<String> links = simulator.getPlayerLinks();
        SoftAssert soft = new SoftAssert();
        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            soft.assertFalse(row.get(0).isBlank(), "Row " + (r + 1) + " has no player name.");
            soft.assertEquals(row.size(), 8, "Row " + (r + 1) + " should have 8 cells: " + row);
            for (int c = 1; c < row.size(); c++) {
                String v = row.get(c);
                soft.assertTrue(v.matches("\\d{1,3}\\.\\d{2}%") && pct(v) >= 0 && pct(v) <= 100,
                        "Row " + (r + 1) + " " + COLUMNS.get(c) + " should be 0.00%-100.00%: '" + v + "'");
            }
        }
        soft.assertTrue(links.stream().allMatch(h -> h.matches("/golfer/\\S+")),
                "Player links should be /golfer/first-last: " + links.subList(0, Math.min(5, links.size())));
        soft.assertAll();
    }

    @Test(priority = 23, description = "TC_TS_P04: Probabilities make sense - Win ≤ Top 5 ≤ Top 10 ≤ … ≤ Top 40 ≤ Make Cut")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyProbabilityOrder() {
        requireProjections();
        SoftAssert soft = new SoftAssert();
        for (List<String> row : simulator.getAllRowValues(false)) {
            for (int c = 2; c < row.size(); c++) {
                // 0.01 tolerance for rounding to 2 decimals
                soft.assertTrue(pct(row.get(c)) + 0.01 >= pct(row.get(c - 1)),
                        row.get(0) + ": " + COLUMNS.get(c) + " (" + row.get(c) + ") should be >= "
                                + COLUMNS.get(c - 1) + " (" + row.get(c - 1) + ")");
            }
        }
        soft.assertAll();
    }

    @Test(priority = 24, description = "TC_TS_P05: Default order is Win % high to low")
    public void verifyDefaultSortByWin() {
        requireProjections();
        List<Double> win = new ArrayList<>();
        for (String v : simulator.getColumn(1, false)) {
            win.add(pct(v));
        }
        Assert.assertTrue(isSorted(win, true), "Players should be sorted by Win % high to low.");
    }

    @DataProvider(name = "sortColumns")
    public Object[][] sortColumns() {
        return new Object[][] {
                {"Win %", 1}, {"Top 5 %", 2}, {"Top 10 %", 3}, {"Top 20 %", 4},
                {"Top 30 %", 5}, {"Top 40 %", 6}, {"Make Cut", 7},
        };
    }

    @Test(priority = 25, dataProvider = "sortColumns", description = "TC_TS_P06: Clicking a % column sorts it, clicking again reverses")
    public void verifySortByColumn(String header, int index) {
        requireProjections();
        simulator.sortBy(header);
        List<Double> first = new ArrayList<>();
        simulator.getColumn(index, false).forEach(v -> first.add(pct(v)));
        simulator.sortBy(header);
        List<Double> second = new ArrayList<>();
        simulator.getColumn(index, false).forEach(v -> second.add(pct(v)));

        boolean firstDesc = isSorted(first, true);
        Assert.assertTrue(firstDesc || isSorted(first, false), header + " not sorted after one click.");
        Assert.assertTrue(isSorted(second, !firstDesc), header + " should be reversed after the second click.");
    }

    @Test(priority = 26, description = "TC_TS_P07: Clicking Player sorts names, clicking again reverses")
    public void verifySortByPlayer() {
        requireProjections();
        simulator.sortBy("Player");
        List<String> first = simulator.getColumn(0, false);
        simulator.sortBy("Player");
        List<String> second = simulator.getColumn(0, false);

        boolean firstDesc = isSortedText(first, true);
        Assert.assertTrue(firstDesc || isSortedText(first, false), "Player names not sorted after one click.");
        Assert.assertTrue(isSortedText(second, !firstDesc), "Player order should be reversed after the second click.");
        simulator.sortBy("Win %"); // leave a number-sorted list for the next tests
    }

    @Test(priority = 27, description = "TC_TS_P08: 40 players are shown first; Load More shows 20 more (or is hidden for small fields)")
    public void verifyLoadMore() {
        requireProjections();
        int total = simulator.getTotalRowCount();
        int shown = simulator.getVisibleRowCount();
        Allure.step("Players: " + total + ", shown: " + shown);

        Assert.assertEquals(shown, Math.min(total, TournamentSimulatorPage.FIRST_PAGE_SIZE), "First page size");
        if (total <= TournamentSimulatorPage.FIRST_PAGE_SIZE) {
            Assert.assertFalse(simulator.isLoadMoreShown(), "Load More should be hidden when all players fit.");
            return;
        }
        Assert.assertTrue(simulator.isLoadMoreShown(), "Load More should be shown for " + total + " players.");
        simulator.clickLoadMore();
        Assert.assertEquals(simulator.getVisibleRowCount(),
                Math.min(total, TournamentSimulatorPage.FIRST_PAGE_SIZE + TournamentSimulatorPage.LOAD_MORE_STEP),
                "Load More should add 20 players.");
    }

    @Test(priority = 28, description = "TC_TS_P09: Search by part of a name filters the list; X clears it")
    @Severity(SeverityLevel.CRITICAL)
    public void verifySearchAndClear() {
        requireProjections();
        int total = simulator.getTotalRowCount();
        String player = simulator.getColumn(0, true).get(0);
        String query = player.substring(0, Math.min(4, player.length()));
        Allure.step("Searching '" + query + "' (from " + player + ")");

        simulator.search(query);
        List<String> found = simulator.getColumn(0, false);
        Assert.assertTrue(found.contains(player), player + " should be found: " + found);
        Assert.assertTrue(found.stream().allMatch(n -> n.contains(query)), "Every result should contain '" + query + "'");
        Assert.assertTrue(simulator.isSearchClearIconShown(), "The X (clear) icon should appear.");

        simulator.clearSearchWithIcon();
        Assert.assertEquals(simulator.getTotalRowCount(), total, "Clearing the search should bring back all players.");
    }

    @Test(priority = 29, description = "TC_TS_P10: Download Projections gives players-file.csv with every player")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyDownloadProjections() {
        requireProjections();
        List<String> csv = simulator.readCsvLines();
        Allure.step("CSV header: " + (csv.isEmpty() ? "-" : csv.get(0)));

        Assert.assertEquals(simulator.getCsvFileName(), TournamentSimulatorPage.CSV_FILE_NAME, "File name");
        Assert.assertTrue(csv.get(0).contains("Player") && csv.get(0).contains("Win %"), "CSV header: " + csv.get(0));
        Assert.assertEquals(csv.size() - 1, simulator.getTotalRowCount(), "CSV rows should equal the number of players.");
    }

    @Test(priority = 30, description = "TC_TS_P11: Clicking a player opens that player's page")
    public void verifyPlayerLink() {
        requireProjections();
        String href = simulator.openFirstPlayer();
        Assert.assertTrue(driver.getCurrentUrl().contains(href), "Expected " + href + ", got " + driver.getCurrentUrl());
        backToSimulator();
    }

    // =================================================================== NEGATIVE / EDGE (projections)

    @Test(priority = 35, description = "TC_TS_N01: Search with no match shows 'No records found'")
    @Severity(SeverityLevel.CRITICAL)
    public void verifySearchNoMatch() {
        requireProjections();
        simulator.search("zzqxnoplayer");
        Assert.assertTrue(simulator.isNoRecordsShown(), "'No records found' box expected.");
        Assert.assertTrue(simulator.getNoRecordsText().contains(TournamentSimulatorPage.NO_RECORDS_TITLE));
        Assert.assertTrue(simulator.getNoRecordsText().contains(TournamentSimulatorPage.NO_RECORDS_TEXT));
        Assert.assertEquals(simulator.getVisibleRowCount(), 0, "No players expected.");
        Assert.assertFalse(simulator.isLoadMoreShown(), "Load More must not show for an empty result.");
    }

    @DataProvider(name = "unsafeSearch")
    public Object[][] unsafeSearch() {
        return new Object[][] {
                {"<script>alert('xss')</script>"}, {"' OR '1'='1"}, {"%%%"}, {"@#$^&*()"}, {"a".repeat(300)},
        };
    }

    @Test(priority = 36, dataProvider = "unsafeSearch", description = "TC_TS_N02: Script / SQL / special / very long search text is harmless")
    public void verifyUnsafeSearchText(String text) {
        requireProjections();
        simulator.search(text);
        try {
            String alert = driver.switchTo().alert().getText();
            driver.switchTo().alert().dismiss();
            Assert.fail("A JavaScript alert appeared: " + alert);
        } catch (NoAlertPresentException expected) {
            // good
        }
        Assert.assertTrue(simulator.isNoRecordsShown(), "'No records found' expected for: " + text);
    }

    @Test(priority = 37, description = "TC_TS_N03: Clearing a no-match search brings the full list back")
    public void verifyClearAfterNoMatch() {
        requireProjections();
        int total = simulator.getTotalRowCount();
        simulator.search("zzqxnoplayer");
        simulator.clearSearchWithIcon();
        Assert.assertFalse(simulator.isNoRecordsShown(), "'No records found' should disappear.");
        Assert.assertEquals(simulator.getTotalRowCount(), total, "All players should be back.");
    }

    // ============================================= NO ACCESS (run LAST - they log out)

    @Test(priority = 40, description = "TC_TS_N04: Logged-out visitor is sent to /subscribe (premium tool)")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyGuestRedirectedToSubscribe() {
        openAsGuest(SiteUrls.of("/tournament-simulator"));
        waitFor(30).until(d -> d.getCurrentUrl().contains("/subscribe"));
        String url = driver.getCurrentUrl();
        Assert.assertTrue(url.contains("tab=tournament-simulator"), "Subscribe page should open the simulator tab: " + url);
        Assert.assertTrue(url.contains("next=/tournament-simulator") || url.contains("next=%2Ftournament-simulator"),
                "Subscribe page should remember the simulator as 'next': " + url);
    }

    @Test(priority = 41, description = "TC_TS_N05: Free user is sent to /subscribe (premium tool)")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyFreeUserRedirectedToSubscribe() {
        openAsGuest(SiteUrls.of("/sign-in"));
        loginPage.waitForPageLoad();
        loginPage.login(propertyReader.readKey("free.username").trim(), propertyReader.readKey("free.password").trim());

        driver.get(SiteUrls.of("/tournament-simulator"));
        waitFor(30).until(d -> d.getCurrentUrl().contains("/subscribe"));
        Assert.assertTrue(driver.getCurrentUrl().contains("tab=tournament-simulator"),
                "Free user should be sent to subscribe: " + driver.getCurrentUrl());
        openAsGuest(SiteUrls.of("/")); // log out
    }
}
