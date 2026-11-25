package com.nikitagehlod.testPages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.nikitagehlod.basetest.commonToAllTest;
import com.nikitagehlod.pages.CourseDatabasePage;
import com.nikitagehlod.pages.HomePage;
import com.nikitagehlod.pages.LoginPage;
import com.nikitagehlod.utils.LinkChecker;
import com.nikitagehlod.utils.SiteUrls;
import com.nikitagehlod.utils.propertyReader;

import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

/**
 * TOOLS > Course Database - positive and edge cases.
 *
 * Flow: sign in with a valid username (paid user) -> hover TOOLS -> click "Course Database"
 * (opens the current tournament's course). Checks are generic - no hard-coded course values - except
 * where a test needs a second course: it uses the first "Correlated Course" that is in the picker.
 *
 * Findings on devbeta (Nov 2026), reported by tests that are EXPECTED TO FAIL until fixed:
 *  - TC_CD_N04: a course URL that does not exist shows a blank page with HTTP 200 (no "not found").
 *  - TC_CD_N05: the browser tab title keeps the OLD course name after switching course in the picker.
 */
@Feature("Course Database")
public class CourseDatabaseTest extends commonToAllTest {

    private static final List<String> HOLE_COLUMNS = List.of("Hole", "Par", "Yards", "AVG", "AVG +/-", "Rank",
            "Eagles", "Birdies", "Pars", "Bogeys", "Doubles/+");

    private static final List<String> METRIC_CATEGORIES = List.of("Off The Tee", "Approach",
            "Approach Shot Distribution", "Overall Approach Proximity", "Fairway Approach Proximity",
            "Rough Approach Proximity", "Around the Green", "Putting", "Putting Ranges (% Made)", "Scoring",
            "Par 3,4,5 Scoring");

    private static final List<String> LEADERBOARD_COLUMNS = List.of("Pos Players", "Score", "R1", "R2", "R3", "R4");

    private HomePage homePage;
    private LoginPage loginPage;
    private CourseDatabasePage course;

    /** The course page TOOLS opened (e.g. /course-stats/sea-island-gc-(seaside)) - tests come back to it. */
    private String startUrl;

    @BeforeClass(alwaysRun = true)
    public void loginAndOpenCourseDatabase() {
        homePage = new HomePage(driver);
        loginPage = new LoginPage(driver);
        course = new CourseDatabasePage(driver);

        if (!driver.getCurrentUrl().startsWith(SiteUrls.base())) {
            driver.get(SiteUrls.of("/"));
        }
        Allure.step("Sign in with a valid username (paid user)");
        homePage.waitForPageLoad();
        homePage.clickLogin();
        loginPage.login(propertyReader.readKey("paid.username").trim(),
                propertyReader.readKey("paid.password").trim());

        Allure.step("Hover TOOLS and click Course Database");
        homePage.openToolsMenuItem("Course Database");
        course.waitForPageLoad();
        startUrl = driver.getCurrentUrl();
        Allure.step("Course Database opened: " + course.getSelectedCourse() + " (" + startUrl + ")");
    }

    // ============================================================================= helpers

    /** Back on the starting course with an empty player search - reloads only if we left it. */
    private void backToStartCourse() {
        if (!driver.getCurrentUrl().equals(startUrl)) {
            driver.get(startUrl);
            course.waitForPageLoad();
        }
        if (course.isNoRecordsShown() || !course.getPlayerSearchText().isEmpty()) {
            course.searchPlayer("");
        }
    }

    private static double num(String v) {
        return Double.parseDouble(v.replace("%", "").replace(",", "").trim());
    }

    private static int parFromFacts(Map<String, String> facts) {
        for (String part : facts.getOrDefault("Course Setup/Length", "").split("\\|")) {
            if (part.trim().matches("Par \\d+")) {
                return Integer.parseInt(part.trim().substring(4));
            }
        }
        return -1;
    }

    private static int yardsFromFacts(Map<String, String> facts) {
        for (String part : facts.getOrDefault("Course Setup/Length", "").split("\\|")) {
            if (part.trim().matches("[\\d,]+ yds")) {
                return Integer.parseInt(part.trim().replace(" yds", "").replace(",", ""));
            }
        }
        return -1;
    }

    private static boolean isSorted(List<Double> v, boolean desc) {
        for (int i = 1; i < v.size(); i++) {
            int c = Double.compare(v.get(i - 1), v.get(i));
            if (desc ? c < 0 : c > 0) {
                return false;
            }
        }
        return true;
    }

    private WebDriverWait waitFor(int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }

    // =================================================================== POSITIVE: page / facts

    @Test(priority = 1, description = "TC_CD_01: TOOLS > Course Database opens a course page")
    @Severity(SeverityLevel.BLOCKER)
    public void verifyOpenFromToolsMenu() {
        Assert.assertTrue(startUrl.contains("/course-stats/"), "URL: " + startUrl);
        Assert.assertFalse(course.getSelectedCourse().isBlank(), "The course picker should show the current course.");
        Assert.assertFalse(course.getTournamentName().isBlank(), "Tournament name missing.");
        Assert.assertTrue(driver.getTitle().contains("Key Stats"), "Browser title: " + driver.getTitle());
    }

    @Test(priority = 2, description = "TC_CD_02: Course Facts - every fact has a value; Course Setup shows Par and yards")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyCourseFacts() {
        backToStartCourse();
        Map<String, String> facts = course.getCourseFacts();
        Allure.step("Facts: " + facts.size() + " - Setup: " + facts.get("Course Setup/Length"));

        SoftAssert soft = new SoftAssert();
        for (String key : List.of("Tournament", "Course Setup/Length", "Location", "Course Type", "Architect",
                "Greens Surface", "Greens Speed", "Fairway Grass", "Rough Grass", "SG: OTT", "SG: APP", "SG: ARG",
                "SG: Putting", "Par 3 Scoring", "Par 4 Scoring", "Par 5 Scoring", "Correlated Courses")) {
            soft.assertTrue(facts.containsKey(key), "Course fact missing: " + key);
        }
        facts.forEach((k, v) -> soft.assertFalse(v.isBlank(), "Course fact '" + k + "' has no value."));
        soft.assertTrue(parFromFacts(facts) >= 69 && parFromFacts(facts) <= 73, "Par should be 69-73: " + facts.get("Course Setup/Length"));
        soft.assertTrue(yardsFromFacts(facts) > 5000, "Yards look wrong: " + facts.get("Course Setup/Length"));
        soft.assertAll();
    }

    @Test(priority = 3, description = "TC_CD_03: Correlated Courses lists other courses (not the course itself)")
    public void verifyCorrelatedCourses() {
        backToStartCourse();
        List<String> correlated = course.getCorrelatedCourses();
        Allure.step("Correlated: " + correlated);
        Assert.assertFalse(correlated.isEmpty(), "No correlated courses.");
        Assert.assertFalse(correlated.contains(course.getSelectedCourse()), "A course should not be correlated with itself.");
    }

    // =================================================================== POSITIVE: hole stats

    @Test(priority = 10, description = "TC_CD_04: Hole Stats has 18 holes in order 1-18 and all 11 columns")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyHoleStatsTable() {
        backToStartCourse();
        Assert.assertEquals(course.getHoleHeaders(), HOLE_COLUMNS, "Hole Stats columns");
        List<String> holes = course.getHoleColumn(0);
        List<String> expected = new ArrayList<>();
        for (int i = 1; i <= 18; i++) {
            expected.add(String.valueOf(i));
        }
        Assert.assertEquals(holes, expected, "Holes 1-18 in order");
    }

    @Test(priority = 11, description = "TC_CD_05: Pars of the 18 holes add up to the course Par in Course Facts")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyParTotal() {
        backToStartCourse();
        int total = course.getHoleColumn(1).stream().mapToInt(Integer::parseInt).sum();
        int factPar = parFromFacts(course.getCourseFacts());
        Allure.step("Holes total par " + total + ", Course Facts par " + factPar);
        Assert.assertEquals(total, factPar, "Sum of hole pars vs course Par");
    }

    @Test(priority = 12, description = "TC_CD_06: Yards of the 18 holes match the course length (within 1%)")
    public void verifyYardsTotal() {
        backToStartCourse();
        int total = course.getHoleColumn(2).stream().mapToInt(v -> Integer.parseInt(v.replace(",", ""))).sum();
        int factYards = yardsFromFacts(course.getCourseFacts());
        Allure.step("Holes total " + total + " yds, Course Facts " + factYards + " yds (difference " + (total - factYards) + ")");
        Assert.assertTrue(Math.abs(total - factYards) <= factYards * 0.01,
                "Hole yards (" + total + ") and course length (" + factYards + ") differ by more than 1%.");
    }

    @Test(priority = 13, description = "TC_CD_07: For every hole AVG +/- equals AVG minus Par")
    public void verifyAvgPlusMinus() {
        backToStartCourse();
        SoftAssert soft = new SoftAssert();
        for (List<String> r : course.getHoleRows()) {
            double expected = num(r.get(3)) - num(r.get(1));
            soft.assertTrue(Math.abs(expected - num(r.get(4))) <= 0.011,
                    "Hole " + r.get(0) + ": AVG " + r.get(3) + " - Par " + r.get(1) + " should be " + String.format("%.2f", expected)
                            + " but AVG +/- is " + r.get(4));
        }
        soft.assertAll();
    }

    @Test(priority = 14, description = "TC_CD_08: For every hole Eagles + Birdies + Pars + Bogeys + Doubles ≈ 100%")
    public void verifyScoreDistribution() {
        backToStartCourse();
        SoftAssert soft = new SoftAssert();
        for (List<String> r : course.getHoleRows()) {
            double sum = 0;
            for (int c = 6; c <= 10; c++) {
                soft.assertTrue(r.get(c).matches("\\d{1,3}\\.\\d{2}%"), "Hole " + r.get(0) + " col " + HOLE_COLUMNS.get(c) + ": " + r.get(c));
                sum += num(r.get(c));
            }
            soft.assertTrue(Math.abs(sum - 100) <= 0.1, "Hole " + r.get(0) + " outcomes add up to " + String.format("%.2f", sum) + "%");
        }
        soft.assertAll();
    }

    @Test(priority = 15, description = "TC_CD_09: Par is 3/4/5, yards and ranks are positive, ranks are unique")
    public void verifyHoleValues() {
        backToStartCourse();
        SoftAssert soft = new SoftAssert();
        List<String> ranks = course.getHoleColumn(5);
        for (List<String> r : course.getHoleRows()) {
            soft.assertTrue(r.get(1).matches("[345]"), "Hole " + r.get(0) + " par: " + r.get(1));
            soft.assertTrue(num(r.get(2)) > 50, "Hole " + r.get(0) + " yards: " + r.get(2));
            soft.assertTrue(r.get(5).matches("\\d+") && Integer.parseInt(r.get(5)) >= 1, "Hole " + r.get(0) + " rank: " + r.get(5));
        }
        soft.assertEquals(new HashSet<>(ranks).size(), ranks.size(), "Two holes share a rank: " + ranks);
        soft.assertAll();
    }

    @DataProvider(name = "holeSort")
    public Object[][] holeSort() {
        return new Object[][] {{"Hole", 0}, {"Yards", 2}, {"AVG", 3}, {"AVG +/-", 4}, {"Rank", 5}, {"Birdies", 7}};
    }

    @Test(priority = 16, dataProvider = "holeSort", description = "TC_CD_10: Clicking a Hole Stats column sorts it, clicking again reverses")
    public void verifyHoleSort(String header, int index) {
        backToStartCourse();
        course.sortHolesBy(header);
        List<Double> first = new ArrayList<>();
        course.getHoleColumn(index).forEach(v -> first.add(num(v)));
        course.sortHolesBy(header);
        List<Double> second = new ArrayList<>();
        course.getHoleColumn(index).forEach(v -> second.add(num(v)));

        boolean firstDesc = isSorted(first, true) && !isSorted(first, false);
        Assert.assertTrue(isSorted(first, false) || isSorted(first, true), header + " not sorted: " + first);
        Assert.assertTrue(isSorted(second, !firstDesc), header + " should be reversed by the 2nd click: " + second);
        course.sortHolesBy("Hole");
        if (!course.getHoleColumn(0).get(0).equals("1")) {
            course.sortHolesBy("Hole"); // back to 1-18 for the next tests
        }
    }

    // =================================================================== POSITIVE: course metrics

    @Test(priority = 20, description = "TC_CD_11: Course Metrics offers the 11 categories")
    public void verifyMetricsCategories() {
        backToStartCourse();
        Assert.assertEquals(course.getMetricsCategories(), METRIC_CATEGORIES, "Course Metrics categories");
    }

    @DataProvider(name = "metricCategories")
    public Object[][] metricCategories() {
        Object[][] data = new Object[METRIC_CATEGORIES.size()][1];
        for (int i = 0; i < METRIC_CATEGORIES.size(); i++) {
            data[i][0] = METRIC_CATEGORIES.get(i);
        }
        return data;
    }

    @Test(priority = 21, dataProvider = "metricCategories", description = "TC_CD_12: Each category shows 5-Year AVG, PGA Tour AVG and recent years (newest first)")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyMetricsCategory(String category) {
        backToStartCourse();
        course.selectMetricsCategory(category);
        List<String> headers = course.getMetricsHeaders();
        List<String> labels = course.getMetricsRowLabels();
        Allure.step(category + ": " + headers.size() + " columns, rows " + labels);

        Assert.assertEquals(course.getMetricsCategory(), category, "Dropdown text");
        Assert.assertTrue(headers.size() > 1 && headers.get(0).equalsIgnoreCase("year"), "Headers: " + headers);
        Assert.assertTrue(labels.size() >= 3, "Too few rows: " + labels);
        Assert.assertEquals(labels.get(0), "5-Year Course AVG");
        Assert.assertEquals(labels.get(1), "PGA Tour AVG");
        List<Integer> years = new ArrayList<>();
        for (String l : labels.subList(2, labels.size())) {
            Assert.assertTrue(l.matches("\\d{4}"), "Year row expected: " + l);
            years.add(Integer.parseInt(l));
        }
        for (int i = 1; i < years.size(); i++) {
            Assert.assertTrue(years.get(i - 1) > years.get(i), "Years should be newest first: " + years);
        }
    }

    // =================================================================== POSITIVE: past leaderboards

    @Test(priority = 30, description = "TC_CD_13: Past Leaderboards - title has the tournament, years newest first, newest selected")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyPastLeaderboards() {
        backToStartCourse();
        List<List<String>> years = course.getPastYears();
        Allure.step("Years: " + years.size() + ", newest " + years.get(0));

        Assert.assertEquals(course.getPastLeaderboardsTitle(), "Past Leaderboards - " + course.getTournamentName());
        Assert.assertFalse(years.isEmpty(), "No past years listed.");
        for (int i = 1; i < years.size(); i++) {
            Assert.assertTrue(Integer.parseInt(years.get(i - 1).get(0)) > Integer.parseInt(years.get(i).get(0)),
                    "Years should be newest first: " + years.get(i - 1).get(0) + " before " + years.get(i).get(0));
        }
        for (List<String> y : years) {
            Assert.assertFalse(y.get(1).isBlank(), y.get(0) + " has no winner.");
            Assert.assertTrue(y.get(2).matches("[+-]?\\d+|E"), y.get(0) + " winning score: '" + y.get(2) + "'");
        }
        Assert.assertEquals(course.getActiveYear(), years.get(0).get(0), "Newest year should be selected.");
    }

    @Test(priority = 31, description = "TC_CD_14: LEADERBOARD columns, players link to /golfer/ (new tab)")
    public void verifyLeaderboardTable() {
        backToStartCourse();
        Assert.assertEquals(course.getLeaderboardHeaders(), LEADERBOARD_COLUMNS, "Leaderboard columns");
        List<String> links = course.getLeaderboardPlayerLinks();
        Assert.assertFalse(links.isEmpty(), "No players in the leaderboard.");
        Assert.assertTrue(links.stream().allMatch(l -> l.startsWith("/golfer/") && l.endsWith("|_blank")),
                "Player links should open /golfer/... in a new tab: " + links.subList(0, Math.min(3, links.size())));
    }

    @DataProvider(name = "yearIndexes")
    public Object[][] yearIndexes() {
        return new Object[][] {{0}, {1}, {2}};
    }

    @Test(priority = 32, dataProvider = "yearIndexes", description = "TC_CD_15: Each year's leaderboard starts with that year's winner and winning score")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyYearWinner(int index) {
        backToStartCourse();
        List<List<String>> years = course.getPastYears();
        if (index >= years.size()) {
            throw new org.testng.SkipException("Only " + years.size() + " years listed.");
        }
        List<String> y = years.get(index);
        course.selectYear(y.get(0));

        String winner = course.getLeaderboardPlayers().get(0);
        String score = course.getLeaderboardRows().get(0).get(1);
        Allure.step(y.get(0) + ": list says " + y.get(1) + " (" + y.get(2) + "), leaderboard shows " + winner + " (" + score + ")");
        Assert.assertEquals(winner.toLowerCase(), y.get(1).toLowerCase(), y.get(0) + " winner");
        Assert.assertEquals(score, y.get(2), y.get(0) + " winning score");
    }

    @Test(priority = 33, description = "TC_CD_16: 'All Metrics' shows strokes-gained columns; LEADERBOARD switches back")
    public void verifyAllMetricsView() {
        backToStartCourse();
        course.selectLeaderboardView("All Metrics");
        List<String> headers = course.getLeaderboardHeaders();
        Allure.step("All Metrics columns: " + headers);
        Assert.assertTrue(headers.contains("SG:TOT") && headers.contains("SG:APP"), "SG columns expected: " + headers);

        course.selectLeaderboardView("LEADERBOARD");
        Assert.assertEquals(course.getLeaderboardHeaders(), LEADERBOARD_COLUMNS, "Back to LEADERBOARD columns");
    }

    @Test(priority = 34, description = "TC_CD_17: Player Search finds a player by part of the name (any case); clearing restores")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyPlayerSearch() {
        backToStartCourse();
        Assert.assertEquals(course.getPlayerSearchPlaceholder(), "Player Search");
        int total = course.getLeaderboardRows().size();
        String player = course.getLeaderboardPlayers().get(0);
        String[] parts = player.split("\\s+");
        String query = parts[parts.length - 1].toLowerCase();

        course.searchPlayer(query);
        List<String> found = course.getLeaderboardPlayers();
        Allure.step("'" + query + "' -> " + found);
        Assert.assertTrue(found.contains(player), player + " should be found by '" + query + "'");
        Assert.assertTrue(found.stream().allMatch(n -> n.toLowerCase().contains(query)), "Every result should contain '" + query + "'");

        course.searchPlayer("");
        Assert.assertEquals(course.getLeaderboardRows().size(), total, "Clearing the search should show everybody again.");
    }

    // =================================================================== POSITIVE: course picker

    @Test(priority = 40, description = "TC_CD_18: Course picker lists many courses A-Z and filters by typing")
    public void verifyCoursePicker() {
        backToStartCourse();
        List<String> all = course.typeInCoursePicker("");
        Allure.step("Courses in the picker: " + all.size());
        Assert.assertTrue(all.size() > 20, "Expected many courses, got " + all.size());
        for (int i = 1; i < all.size(); i++) {
            Assert.assertTrue(Character.toUpperCase(all.get(i - 1).charAt(0)) <= Character.toUpperCase(all.get(i).charAt(0)),
                    "Courses not A-Z: '" + all.get(i - 1) + "' before '" + all.get(i) + "'");
        }
        String target = course.getSelectedCourse();
        String query = target.substring(0, Math.min(6, target.length())).toLowerCase();
        List<String> found = course.typeInCoursePicker(query);
        course.closeCoursePicker();
        Assert.assertTrue(found.contains(target), target + " should be found by '" + query + "': " + found);
    }

    @Test(priority = 41, description = "TC_CD_19: Choosing another course changes the URL, the picker and all the data")
    @Severity(SeverityLevel.CRITICAL)
    public void verifySwitchCourse() {
        backToStartCourse();
        String startCourse = course.getSelectedCourse();
        String startData = course.getTournamentName() + " | " + course.getCourseFacts();

        List<String> inPicker = course.typeInCoursePicker("");
        course.closeCoursePicker();
        String other = course.getCorrelatedCourses().stream().filter(inPicker::contains).findFirst()
                .orElse(inPicker.stream().filter(c -> !c.equals(startCourse)).findFirst().orElseThrow());
        Allure.step("Switching " + startCourse + " -> " + other);

        course.selectCourse(other);
        Assert.assertEquals(course.getSelectedCourse(), other, "Picker value");
        Assert.assertFalse(driver.getCurrentUrl().equals(startUrl), "URL should change for the new course.");
        Assert.assertTrue(driver.getCurrentUrl().contains("/course-stats/"), "URL: " + driver.getCurrentUrl());
        Assert.assertEquals(course.getHoleRows().size(), 18, "New course should show 18 holes.");
        Assert.assertNotEquals(course.getTournamentName() + " | " + course.getCourseFacts(), startData,
                "Tournament / Course Facts should change for the new course.");
        int par = course.getHoleColumn(1).stream().mapToInt(Integer::parseInt).sum();
        Assert.assertEquals(par, parFromFacts(course.getCourseFacts()), "New course: hole pars vs course Par");
    }

    @Test(priority = 42, description = "TC_CD_20: Opening a course URL directly (bookmark / shared link) shows that course")
    public void verifyDirectCourseUrl() {
        backToStartCourse();
        String name = course.getSelectedCourse();
        driver.get(startUrl);
        course.waitForPageLoad();
        Assert.assertEquals(course.getSelectedCourse(), name, "Direct URL should open the same course.");
    }

    // =================================================================== NEGATIVE / EDGE

    @Test(priority = 50, description = "TC_CD_N01: Player Search with no match shows 'No records found'")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyPlayerSearchNoMatch() {
        backToStartCourse();
        course.searchPlayer("zzqxnoplayer");
        Assert.assertTrue(course.isNoRecordsShown(), "'No records found' expected.");
        Assert.assertTrue(course.getNoRecordsText().contains(CourseDatabasePage.NO_RECORDS_TEXT), course.getNoRecordsText());
        Assert.assertTrue(course.getLeaderboardRows().isEmpty(), "No leaderboard rows expected.");
        course.searchPlayer("");
        Assert.assertFalse(course.isNoRecordsShown(), "Clearing should remove the message.");
    }

    @DataProvider(name = "unsafeSearch")
    public Object[][] unsafeSearch() {
        return new Object[][] {{"<script>alert('xss')</script>"}, {"' OR '1'='1"}, {"%%%"}, {"a".repeat(300)}};
    }

    @Test(priority = 51, dataProvider = "unsafeSearch", description = "TC_CD_N02: Script / SQL / special / very long Player Search text is harmless")
    public void verifyUnsafePlayerSearch(String text) {
        backToStartCourse();
        course.searchPlayer(text);
        try {
            String alert = driver.switchTo().alert().getText();
            driver.switchTo().alert().dismiss();
            Assert.fail("A JavaScript alert appeared: " + alert);
        } catch (NoAlertPresentException expected) {
            // good
        }
        Assert.assertTrue(course.isNoRecordsShown(), "'No records found' expected for: " + text);
    }

    @Test(priority = 52, description = "TC_CD_N03: Course picker with no match shows 'No options'")
    public void verifyCoursePickerNoMatch() {
        backToStartCourse();
        List<String> found = course.typeInCoursePicker("zzqxnocourse");
        String notice = course.getPickerNotice();
        course.closeCoursePicker();
        Assert.assertTrue(found.isEmpty(), "No courses expected: " + found);
        Assert.assertEquals(notice, "No options");
    }

    @Test(priority = 53, description = "TC_CD_N04: A course that does not exist shows 'not found' (FAILS on devbeta: blank page, HTTP 200)")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyUnknownCourseUrl() {
        String url = SiteUrls.of("/course-stats/this-course-does-not-exist-qa");
        int status = LinkChecker.status(url);
        driver.get(url);
        course.waitForPageReady();
        String body = driver.findElement(org.openqa.selenium.By.tagName("body")).getText();
        boolean notFoundShown = body.toLowerCase().contains("not found") || body.toLowerCase().contains("no course");
        Allure.step("HTTP " + status + ", course content shown: " + course.hasCourseContent() + ", 'not found' message: " + notFoundShown);

        Assert.assertFalse(course.hasCourseContent(), "No course data should be shown for an unknown course.");
        Assert.assertTrue(status == 404 || notFoundShown,
                "An unknown course should return 404 or say 'not found' - devbeta shows a blank page with HTTP " + status);
    }

    @Test(priority = 54, description = "TC_CD_N05: Browser title follows the course after switching (FAILS on devbeta: title keeps the old course)")
    public void verifyTitleAfterSwitch() {
        backToStartCourse();
        List<String> inPicker = course.typeInCoursePicker("");
        course.closeCoursePicker();
        String start = course.getSelectedCourse();
        String other = course.getCorrelatedCourses().stream().filter(inPicker::contains).findFirst()
                .orElse(inPicker.stream().filter(c -> !c.equals(start)).findFirst().orElseThrow());
        course.selectCourse(other);

        String firstWord = other.split("\\s+")[0].toLowerCase();
        Allure.step("After switching to " + other + " the title is: " + driver.getTitle());
        Assert.assertTrue(driver.getTitle().toLowerCase().contains(firstWord),
                "Browser title should name " + other + " but is: " + driver.getTitle());
    }

    // ============================================== ACCESS (run LAST - they log out)

    /**
     * Logged-out visitors are NOT blocked from the URL, but get a subscriber banner instead of the course data
     * (Hole Stats, Course Metrics, Past Leaderboards are not loaded) - checked in the page's code, Nov 2026.
     */
    @Test(priority = 60, description = "TC_CD_N07: Logged-out visitor gets the subscriber banner, not the course data")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyGuestSeesSubscriberBanner() {
        openAsGuest(startUrl);
        course.waitForSubscriberBanner();
        verifyLockedView("Logged-out visitor");
    }

    @Test(priority = 61, description = "TC_CD_N08: Free user gets the subscriber banner, not the course data")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyFreeUserSeesSubscriberBanner() {
        openAsGuest(SiteUrls.of("/sign-in"));
        loginPage.waitForPageLoad();
        loginPage.login(propertyReader.readKey("free.username").trim(), propertyReader.readKey("free.password").trim());
        driver.get(startUrl);
        course.waitForSubscriberBanner();
        verifyLockedView("Free user");
        openAsGuest(SiteUrls.of("/")); // log out again
    }

    /** What a user without a subscription must (and must not) see. */
    private void verifyLockedView(String who) {
        String heading = course.getSubscriberBannerHeading();
        Allure.step(who + " sees: " + heading);
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(driver.getCurrentUrl().contains("/course-stats/"), who + " should stay on the course page: " + driver.getCurrentUrl());
        soft.assertTrue(heading.contains("12 key stats and correlated courses are for subscribers"), "Banner heading: " + heading);
        soft.assertEquals(course.getSubscriberBannerSubText(), "You are seeing the course facts. Here is the rest.", "Banner text");
        soft.assertTrue(course.isSubscribeButtonShown(), "Subscribe button missing on the banner.");
        soft.assertTrue(course.getHoleRows().isEmpty(), who + " must not see Hole Stats.");
        soft.assertFalse(course.isPastLeaderboardsShown(), who + " must not see Past Leaderboards.");
        soft.assertAll();
    }

    @Test(priority = 62, description = "TC_CD_N09: For a logged-out visitor, the Correlated Courses message names the current course "
            + "(FAILS on devbeta: it always says 'Aronimink-gc')")
    public void verifyGuestCorrelatedMessageNamesCourse() {
        openAsGuest(startUrl);
        course.waitForSubscriberBanner();
        String message = course.getCourseFacts().getOrDefault("Correlated Courses", "");
        String slugWord = startUrl.substring(startUrl.lastIndexOf('/') + 1).split("[-(]")[0].toLowerCase(); // e.g. "sea"
        Allure.step("Correlated Courses message for a guest: " + message);
        Assert.assertTrue(message.toLowerCase().contains(slugWord),
                "Message should name the current course (" + slugWord + "...) but says: " + message);
    }
}
