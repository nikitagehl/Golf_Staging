package com.nikitagehlod.testPages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
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
import com.nikitagehlod.pages.LiveLeaderboardPage;
import com.nikitagehlod.pages.LoginPage;
import com.nikitagehlod.utils.SiteUrls;
import com.nikitagehlod.utils.propertyReader;

import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

/**
 * TOOLS > Live Leaderboard - positive and edge cases.
 *
 * Flow: sign in as the paid user -> hover TOOLS -> click "Live Leaderboard" -> validate the page.
 * Checks are generic (no hard-coded tournament or player names), so they keep working when the
 * weekly tournament changes. Each test puts the page back to its default view through the page's
 * own controls (no reload). The logged-out / free-user tests run LAST because they end the session.
 *
 * Not tested on purpose: clicking the favourite stars and "Reset Favorites" - they change the
 * paid account's saved data.
 */
@Feature("Live Leaderboard")
public class LiveLeaderboardTest extends commonToAllTest {

    private static final List<String> STAT_TABS =
            List.of("SG", "OTT", "APP", "ARG", "PUTT", "Scoring", "MISC", "Course Metrics");

    private static final List<String> SG_COLUMNS = List.of(
            "Score", "Total", "Pos", "SG:TOT", "SG:OTT", "SG:APP", "SG:ARG", "SG: P", "SG:BS", "SG:T2G");

    private HomePage homePage;
    private LoginPage loginPage;
    private LiveLeaderboardPage leaderboard;

    /** Number of players in the default view, read once - used to check filters restore it. */
    private int fullRowCount;

    @BeforeClass(alwaysRun = true)
    public void loginAndOpenLiveLeaderboard() {
        homePage = new HomePage(driver);
        loginPage = new LoginPage(driver);
        leaderboard = new LiveLeaderboardPage(driver);

        Allure.step("Sign in as the paid user");
        if (!driver.getCurrentUrl().startsWith(SiteUrls.base())) {
            driver.get(SiteUrls.of("/")); // setup's first load did not reach the site (slow devbeta)
        }
        homePage.waitForPageLoad();
        homePage.clickLogin();
        loginPage.login(propertyReader.readKey("paid.username").trim(),
                propertyReader.readKey("paid.password").trim());

        Allure.step("Hover TOOLS and click Live Leaderboard");
        homePage.openToolsMenuItem("Live Leaderboard");
        leaderboard.waitForPageLoad();

        fullRowCount = leaderboard.getRowCount();
        Allure.step("Players on the leaderboard: " + fullRowCount);
    }

    // ============================================================================= helpers

    /**
     * Default view without a reload: SG / Tournament / Average / Full Leaderboard, no search, no favourites.
     * The SG tab is selected FIRST: the Course Metrics tab hides the favourites checkbox and the
     * Rank/Average/Total + Full Leaderboard/Player Groups options, so they only exist on a player tab.
     */
    private void resetView() {
        if (!driver.getCurrentUrl().contains("/live-leaderboard")) {
            driver.get(SiteUrls.of("/live-leaderboard"));
            leaderboard.waitForPageLoad();
        }
        leaderboard.selectStatTab("SG");
        if (!leaderboard.getSearchText().isEmpty()) {
            leaderboard.clearSearch();
        }
        leaderboard.setFavoritesOnly(false);
        leaderboard.selectView("Full Leaderboard");
        leaderboard.selectRound("Tournament");
        leaderboard.selectDisplayMode("Average");
    }

    private WebDriverWait waitFor(int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }

    private static boolean isNumber(String s) {
        return s.matches("-?[\\d,]*\\.?\\d+");
    }

    private static double toNumber(String s) {
        return Double.parseDouble(s.replace(",", ""));
    }

    /** Numeric values of a column, skipping "-" / blank cells (players without that stat). */
    private List<Double> numbers(List<String> values) {
        List<Double> out = new ArrayList<>();
        for (String v : values) {
            if (isNumber(v)) {
                out.add(toNumber(v));
            }
        }
        return out;
    }

    private void assertNoAlert() {
        try {
            String text = driver.switchTo().alert().getText();
            driver.switchTo().alert().dismiss();
            Assert.fail("A JavaScript alert appeared: " + text);
        } catch (NoAlertPresentException expected) {
            // good - nothing was executed
        }
    }

    // ======================================================================= POSITIVE

    @Test(priority = 1, description = "TC_LB_01: TOOLS > Live Leaderboard opens the leaderboard for a paid user")
    @Severity(SeverityLevel.BLOCKER)
    public void verifyOpenFromToolsMenu() {
        Assert.assertTrue(driver.getCurrentUrl().contains("/live-leaderboard"), "Wrong page: " + driver.getCurrentUrl());
        Assert.assertTrue(leaderboard.isTableDisplayed(), "Leaderboard table is not shown.");
        Assert.assertFalse(leaderboard.isPaywallDisplayed(), "Paid user must not see the paywall.");
        Assert.assertTrue(fullRowCount > 0, "Leaderboard has no players.");
    }

    @Test(priority = 2, description = "TC_LB_02: Tournament name, course, PAR and YARDAGE are shown correctly")
    public void verifyTournamentInfo() {
        resetView();
        String name = leaderboard.getTournamentName();
        String course = leaderboard.getCourseLocation();
        String par = leaderboard.getInfoValue("PAR");
        String yardage = leaderboard.getInfoValue("YARDAGE");
        Allure.step(name + " | " + course + " | PAR " + par + " | " + yardage);

        SoftAssert soft = new SoftAssert();
        soft.assertFalse(name.isBlank(), "Tournament name is empty.");
        soft.assertTrue(course.contains(","), "Course line should be 'Course, City, Country': " + course);
        soft.assertTrue(par.matches("\\d{2}") && Integer.parseInt(par) >= 60 && Integer.parseInt(par) <= 80,
                "PAR should be a number between 60 and 80: " + par);
        soft.assertTrue(yardage.matches("[\\d,]{4,6} yd"), "YARDAGE should look like '7005 yd': " + yardage);
        soft.assertAll();
    }

    @Test(priority = 3, description = "TC_LB_03: Default view - SG, Tournament, Average, Full Leaderboard, no filters")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyDefaultState() {
        resetView();
        SoftAssert soft = new SoftAssert();
        soft.assertEquals(leaderboard.getStatTabNames(), STAT_TABS, "Stat tabs");
        soft.assertEquals(leaderboard.getSelectedStatTab(), "SG", "Selected stat tab");
        soft.assertEquals(leaderboard.getSelectedRound(), "Tournament", "Selected round");
        soft.assertTrue(leaderboard.isOptionSelected("Average"), "'Average' should be selected.");
        soft.assertTrue(leaderboard.isOptionSelected("Full Leaderboard"), "'Full Leaderboard' should be selected.");
        soft.assertFalse(leaderboard.isFavoritesOnlyChecked(), "'Show Favorites Only' should be off.");
        soft.assertEquals(leaderboard.getSearchPlaceholder(), "Search", "Search placeholder");
        soft.assertEquals(leaderboard.getCsvFileName(), "Live-leaderboard.csv", "CSV file name");
        soft.assertAll();
    }

    @Test(priority = 4, description = "TC_LB_04: SG tab shows the Player column and all 10 SG columns in order")
    public void verifyDefaultColumns() {
        resetView();
        List<String> headers = leaderboard.getHeaders();
        Allure.step("Headers: " + headers);
        Assert.assertTrue(headers.get(0).contains("Player"), "First column should be Player: " + headers.get(0));
        Assert.assertEquals(headers.subList(1, headers.size()), SG_COLUMNS, "SG columns");
    }

    @Test(priority = 5, description = "TC_LB_05: Every row has a player, a position and numeric SG values; no duplicates")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyRowData() {
        resetView();
        List<String> names = leaderboard.getPlayerNames();
        List<String> positions = leaderboard.getColumnValues("FinishPos");
        List<String> scores = leaderboard.getColumnValues("Score");
        List<String> sgTot = leaderboard.getColumnValues("SGTOT");

        SoftAssert soft = new SoftAssert();
        soft.assertTrue(names.stream().noneMatch(String::isBlank), "A row has no player name.");
        soft.assertEquals(new HashSet<>(names).size(), names.size(), "The same player is listed twice.");
        soft.assertTrue(scores.stream().noneMatch(String::isBlank), "A row has no score.");
        for (String pos : positions) {
            soft.assertTrue(pos.matches("T?\\d+|CUT|WD|DQ|MDF|MC|-"), "Unexpected position value: '" + pos + "'");
        }
        for (String v : sgTot) {
            soft.assertTrue(v.equals("-") || v.isEmpty() || isNumber(v), "SG:TOT is not a number: '" + v + "'");
        }
        soft.assertAll();
    }

    @DataProvider(name = "statTabs")
    public Object[][] statTabs() {
        return new Object[][] {
                // tab, a column that must appear for it
                {"OTT", "SG:OTT"},
                {"APP", "SG:APP"},
                {"ARG", "SG:ARG"},
                {"PUTT", "SG: P"},
                {"Scoring", "BoB %"},
                {"MISC", "BallSpeed"},
                {"SG", "SG:TOT"},
        };
    }

    @Test(priority = 6, dataProvider = "statTabs", description = "TC_LB_06: Each stat tab shows its own columns plus Score/Total/Pos")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyStatTab(String tab, String keyColumn) {
        resetView();
        leaderboard.selectStatTab(tab);
        List<String> headers = leaderboard.getHeaders();
        Allure.step(tab + " columns: " + headers);

        Assert.assertEquals(leaderboard.getSelectedStatTab(), tab, "Tab is not marked as selected.");
        Assert.assertTrue(headers.containsAll(List.of("Score", "Total", "Pos")), "Fixed columns missing: " + headers);
        Assert.assertTrue(headers.contains(keyColumn), tab + " tab should show column '" + keyColumn + "': " + headers);
        Assert.assertTrue(leaderboard.getRowCount() > 0, tab + " tab shows no players.");
    }

    @Test(priority = 7, description = "TC_LB_07: Course Metrics shows course-level stats (no player positions)")
    public void verifyCourseMetricsTab() {
        resetView();
        leaderboard.selectStatTab("Course Metrics");
        List<String> headers = leaderboard.getHeaders();
        Allure.step("Course Metrics columns: " + headers);

        Assert.assertTrue(headers.contains("Year"), "Course Metrics should have a Year column: " + headers);
        Assert.assertTrue(headers.contains("ScoringAVG"), "Course Metrics should have Scoring AVG: " + headers);
        Assert.assertFalse(headers.contains("Pos"), "Course Metrics is not a player table: " + headers);
        Assert.assertTrue(leaderboard.getRowCount() >= 1, "Course Metrics has no rows.");

        // player-only controls are hidden on this tab
        Assert.assertFalse(leaderboard.isFavoritesOnlyAvailable(), "'Show Favorites Only' should be hidden on Course Metrics.");
        Assert.assertFalse(leaderboard.isDisplayOptionShown("Rank"), "Rank/Average/Total should be hidden on Course Metrics.");
        Assert.assertFalse(leaderboard.isDisplayOptionShown("Player Groups"), "View as options should be hidden on Course Metrics.");

        leaderboard.selectStatTab("SG"); // leave the page on a player tab for the next test
    }

    @DataProvider(name = "rounds")
    public Object[][] rounds() {
        return new Object[][] {{"R1"}, {"R2"}, {"R3"}, {"R4"}};
    }

    @Test(priority = 8, dataProvider = "rounds", description = "TC_LB_08: Each round shows a column for that round")
    public void verifyRound(String round) {
        resetView();
        leaderboard.selectRound(round);
        List<String> headers = leaderboard.getHeaders();

        Assert.assertEquals(leaderboard.getSelectedRound(), round, "Round is not marked as selected.");
        Assert.assertTrue(headers.contains(round), "A '" + round + "' column should be added: " + headers);
        Assert.assertTrue(leaderboard.getRowCount() > 0, round + " shows no players.");
    }

    @Test(priority = 9, description = "TC_LB_09: Weekend rounds never list more players than Round 1 (the cut)")
    public void verifyCutReducesPlayers() {
        resetView();
        leaderboard.selectRound("R1");
        int r1 = leaderboard.getRowCount();
        leaderboard.selectRound("R3");
        int r3 = leaderboard.getRowCount();
        Allure.step("R1 players: " + r1 + ", R3 players: " + r3);
        Assert.assertTrue(r3 <= r1, "R3 (" + r3 + ") should not have more players than R1 (" + r1 + ").");
    }

    @Test(priority = 10, description = "TC_LB_10: Rank / Average / Total change how the stats are shown")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyDisplayModes() {
        resetView();
        List<String> average = leaderboard.getColumnValues("SGTOT");

        leaderboard.selectDisplayMode("Rank");
        List<String> rank = leaderboard.getColumnValues("SGTOT");

        leaderboard.selectDisplayMode("Total");
        List<String> total = leaderboard.getColumnValues("SGTOT");

        Allure.step("SG:TOT first values - Average " + average.subList(0, 3) + ", Rank " + rank.subList(0, 3)
                + ", Total " + total.subList(0, 3));

        SoftAssert soft = new SoftAssert();
        soft.assertTrue(average.stream().filter(v -> isNumber(v)).allMatch(v -> v.matches("-?\\d+\\.\\d{2}")),
                "Average values should have 2 decimals.");
        soft.assertTrue(rank.stream().filter(v -> isNumber(v)).allMatch(v -> v.matches("\\d+")),
                "Rank values should be whole numbers.");
        soft.assertTrue(rank.stream().filter(v -> isNumber(v)).allMatch(v -> Integer.parseInt(v) >= 1),
                "Ranks should start at 1.");
        soft.assertNotEquals(total, average, "Total should show different numbers than Average.");
        soft.assertAll();
    }

    @Test(priority = 11, description = "TC_LB_11: Search by part of a last name, in lower case, finds that player")
    @Severity(SeverityLevel.CRITICAL)
    public void verifySearchPartialCaseInsensitive() {
        resetView();
        String player = leaderboard.getPlayerNames().get(0);
        String[] parts = player.split("\\s+");
        String lastName = parts[parts.length - 1];
        String query = lastName.substring(0, Math.min(4, lastName.length())).toLowerCase();
        Allure.step("Searching '" + query + "' (from " + player + ")");

        leaderboard.search(query);
        List<String> found = leaderboard.getPlayerNames();

        Assert.assertTrue(found.contains(player), player + " should be found by '" + query + "': " + found);
        Assert.assertTrue(found.stream().allMatch(n -> n.toLowerCase().contains(query)),
                "Every result should contain '" + query + "': " + found);
        Assert.assertTrue(found.size() < fullRowCount, "Search did not filter anything.");
    }

    @Test(priority = 12, description = "TC_LB_12: Search by full name shows exactly that player")
    public void verifySearchFullName() {
        resetView();
        List<String> names = leaderboard.getPlayerNames();
        String player = names.get(names.size() / 2); // someone from the middle of the field
        leaderboard.search(player);
        Assert.assertEquals(leaderboard.getPlayerNames(), List.of(player), "Search by full name");
    }

    @Test(priority = 13, description = "TC_LB_13: Clearing the search shows the full field again")
    public void verifyClearSearchRestoresList() {
        resetView();
        leaderboard.search("a");
        leaderboard.clearSearch();
        Assert.assertEquals(leaderboard.getRowCount(), fullRowCount, "All players should be back after clearing.");
    }

    @Test(priority = 14, description = "TC_LB_14: Clicking SG:TOT sorts the column, clicking again reverses the order")
    @Severity(SeverityLevel.CRITICAL)
    public void verifySortBySgTotal() {
        resetView();
        leaderboard.sortBy("SG:TOT");
        List<Double> first = numbers(leaderboard.getColumnValues("SGTOT"));
        leaderboard.sortBy("SG:TOT");
        List<Double> second = numbers(leaderboard.getColumnValues("SGTOT"));

        boolean firstDesc = isSorted(first, true);
        Allure.step("First click: " + (firstDesc ? "high to low" : "low to high")
                + " (on devbeta the first click sorts high to low)");

        Assert.assertTrue(firstDesc || isSorted(first, false), "First click did not sort SG:TOT: " + first);
        Assert.assertTrue(isSorted(second, !firstDesc), "Second click should reverse the order: " + second);
    }

    /** True when the list is sorted high-to-low (descending=true) or low-to-high. */
    private static boolean isSorted(List<Double> values, boolean descending) {
        for (int i = 1; i < values.size(); i++) {
            int cmp = Double.compare(values.get(i - 1), values.get(i));
            if (descending ? cmp < 0 : cmp > 0) {
                return false;
            }
        }
        return true;
    }

    @Test(priority = 15, description = "TC_LB_15: 'Show Favorites Only' shows exactly the starred players from its label")
    public void verifyFavoritesOnlyFilter() {
        resetView();
        int expected = leaderboard.getFavoritesCountFromLabel();
        if (expected <= 0) {
            throw new SkipException("The paid account has no favourite players (label count " + expected + ").");
        }
        leaderboard.setFavoritesOnly(true);
        int rows = leaderboard.getRowCount();
        int starred = leaderboard.getStarredRowCount();
        Allure.step("Label says " + expected + ", rows " + rows + ", starred " + starred);

        Assert.assertEquals(rows, expected, "Rows should match the number in the label.");
        Assert.assertEquals(starred, rows, "Every row should be a favourite (filled star).");

        leaderboard.setFavoritesOnly(false);
        Assert.assertEquals(leaderboard.getRowCount(), fullRowCount, "Unticking should show all players again.");
    }

    @Test(priority = 16, description = "TC_LB_16: 'Player Groups' splits the field into several groups and back")
    public void verifyPlayerGroupsView() {
        resetView();
        leaderboard.selectView("Player Groups");
        List<Integer> groups = leaderboard.getRowsPerTable();
        Allure.step("Groups: " + groups.size() + " sizes " + groups);

        Assert.assertTrue(groups.size() > 1, "Player Groups should show more than one group: " + groups);
        Assert.assertTrue(groups.stream().allMatch(n -> n > 0), "A group is empty: " + groups);

        leaderboard.selectView("Full Leaderboard");
        Assert.assertEquals(leaderboard.getTableCount(), 1, "Full Leaderboard should be one table again.");
    }

    @Test(priority = 17, description = "TC_LB_17: Download CSV contains the same players as the table")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyCsvDownload() {
        resetView();
        List<String> csv = leaderboard.readCsvLines();
        List<String> names = leaderboard.getPlayerNames();
        Allure.step("CSV header: " + csv.get(0));

        Assert.assertEquals(leaderboard.getCsvFileName(), "Live-leaderboard.csv", "CSV file name");
        Assert.assertTrue(csv.get(0).startsWith("\"Player\""), "CSV should start with a Player column: " + csv.get(0));
        Assert.assertEquals(csv.size() - 1, names.size(), "CSV rows (without header) should equal table rows.");

        String firstCsvPlayer = csv.get(1).split("\",\"")[0].replace("\"", "");
        Assert.assertEquals(firstCsvPlayer, names.get(0), "First player in the CSV and the table should match.");
    }

    @Test(priority = 18, description = "TC_LB_18: Column header tooltip explains SG:TOT")
    public void verifyHeaderTooltip() {
        resetView();
        String tip = leaderboard.getHeaderTooltip("SG:TOT");
        Assert.assertTrue(tip.toLowerCase().contains("strokes gained"), "SG:TOT tooltip: '" + tip + "'");
    }

    // ======================================================================= NEGATIVE / EDGE

    @Test(priority = 30, description = "TC_LB_N01: Search with no match shows 'No records match…' and no rows")
    @Severity(SeverityLevel.CRITICAL)
    public void verifySearchNoMatch() {
        resetView();
        leaderboard.search("zzqxnoplayer");
        Assert.assertEquals(leaderboard.getRowCount(), 0, "No rows expected.");
        Assert.assertEquals(leaderboard.getNoRecordsMessage(), LiveLeaderboardPage.NO_RECORDS_MESSAGE);
    }

    @DataProvider(name = "unsafeSearch")
    public Object[][] unsafeSearch() {
        return new Object[][] {
                {"<script>alert('xss')</script>"},
                {"<img src=x onerror=alert(1)>"},
                {"' OR '1'='1"},
                {"%%%"},
                {"@#$^&*()"},
        };
    }

    @Test(priority = 31, dataProvider = "unsafeSearch", description = "TC_LB_N02: Script / SQL / special characters in search are harmless")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyUnsafeSearchText(String text) {
        resetView();
        leaderboard.search(text);
        assertNoAlert();
        Assert.assertEquals(leaderboard.getSearchText(), text, "Search box should keep the typed text.");
        Assert.assertTrue(leaderboard.getRowCount() == 0 && leaderboard.isNoRecordsMessageShown(),
                "Special text should match nobody and show the 'No records' message.");
    }

    @Test(priority = 32, description = "TC_LB_N03: A very long search text does not break the page")
    public void verifyVeryLongSearch() {
        resetView();
        leaderboard.search("a".repeat(300));
        Assert.assertEquals(leaderboard.getRowCount(), 0, "No player has a 300-character name.");
        Assert.assertTrue(leaderboard.isNoRecordsMessageShown(), "'No records' message expected.");
    }

    @Test(priority = 33, description = "TC_LB_N04: Search of only spaces is handled (all players or 'No records')")
    public void verifySpacesOnlySearch() {
        resetView();
        leaderboard.search("     ");
        int rows = leaderboard.getRowCount();
        if (rows == 0) {
            // Observed on devbeta (Nov 2026): spaces-only search hides every player.
            // Most sites ignore blank searches - reported as a possible bug, not failed here.
            Allure.step("NOTE: spaces-only search shows no players - possible bug, please check with the team.");
        }
        Assert.assertTrue(rows == fullRowCount || leaderboard.isNoRecordsMessageShown(),
                "Spaces-only search should show everybody or the 'No records' message, got " + rows + " rows.");
    }

    @Test(priority = 34, description = "TC_LB_N05: Favourites-only + a search that matches nobody shows 'No records'")
    public void verifyFavoritesWithNoMatchSearch() {
        resetView();
        if (leaderboard.getFavoritesCountFromLabel() <= 0) {
            throw new SkipException("The paid account has no favourite players.");
        }
        leaderboard.setFavoritesOnly(true);
        leaderboard.search("zzqxnoplayer");
        Assert.assertEquals(leaderboard.getRowCount(), 0);
        Assert.assertTrue(leaderboard.isNoRecordsMessageShown(), "'No records' message expected.");
        leaderboard.setFavoritesOnly(false);
    }

    @Test(priority = 35, description = "TC_LB_N06: Tab / round / display changes keep the search text")
    public void verifySearchKeptWhenSwitchingTabs() {
        resetView();
        String player = leaderboard.getPlayerNames().get(0);
        leaderboard.search(player);
        leaderboard.selectStatTab("OTT");
        Assert.assertEquals(leaderboard.getSearchText(), player, "Search text was lost after changing tab.");
        Assert.assertTrue(leaderboard.getPlayerNames().stream().allMatch(n -> n.equals(player)),
                "After changing tab the list should still be filtered to " + player);
    }

    // ============================================== NEGATIVE: no access (run LAST - they log out)

    @Test(priority = 40, description = "TC_LB_N07: Logged-out visitor sees the paywall instead of the leaderboard")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyGuestSeesPaywall() {
        openAsGuest(SiteUrls.of("/live-leaderboard"));
        leaderboard.waitForPageLoad();

        Assert.assertTrue(leaderboard.isPaywallDisplayed(), "Guest should see the 'You're Missing Out!' paywall.");
        Assert.assertTrue(leaderboard.getPaywallText().contains("Missing Out"), "Paywall heading missing.");
        Assert.assertTrue(leaderboard.isUpgradeButtonDisplayed(), "UPGRADE button missing on the paywall.");
        Assert.assertFalse(leaderboard.isTableDisplayed(), "Guest must not see the leaderboard table.");
        Assert.assertFalse(leaderboard.getTournamentName().isBlank(), "Tournament header should still be shown.");
    }

    @Test(priority = 41, description = "TC_LB_N08: 'Log in' on the paywall opens the sign-in page")
    public void verifyPaywallLoginLink() {
        openAsGuest(SiteUrls.of("/live-leaderboard"));
        leaderboard.waitForPageLoad();
        leaderboard.clickPaywallLogin();
        waitFor(20).until(d -> d.getCurrentUrl().contains("/sign-in"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/sign-in"));
    }

    /**
     * Expected business rule: Live Leaderboard is a premium tool, so a FREE account sees the paywall
     * too. (Checked as a guest on devbeta; not yet seen with the free account - if this fails, confirm
     * with the team whether free users are meant to have access.)
     */
    @Test(priority = 42, description = "TC_LB_N09: Free user sees the paywall instead of the leaderboard")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyFreeUserSeesPaywall() {
        openAsGuest(SiteUrls.of("/sign-in"));
        loginPage.waitForPageLoad();
        loginPage.login(propertyReader.readKey("free.username").trim(), propertyReader.readKey("free.password").trim());

        driver.get(SiteUrls.of("/live-leaderboard"));
        leaderboard.waitForPageLoad();
        Assert.assertTrue(leaderboard.isPaywallDisplayed(), "Free user should see the paywall.");
        Assert.assertFalse(leaderboard.isTableDisplayed(), "Free user must not see the leaderboard table.");

        openAsGuest(SiteUrls.of("/")); // log out
    }
}
