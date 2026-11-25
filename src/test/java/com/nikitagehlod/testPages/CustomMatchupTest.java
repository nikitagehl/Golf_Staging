package com.nikitagehlod.testPages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.nikitagehlod.basetest.commonToAllTest;
import com.nikitagehlod.pages.CustomMatchupPage;
import com.nikitagehlod.pages.HomePage;
import com.nikitagehlod.pages.LoginPage;
import com.nikitagehlod.utils.SiteUrls;
import com.nikitagehlod.utils.propertyReader;

import io.qameta.allure.Allure;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

/**
 * TOOLS > Custom Matchups ("Golf Custom Simulation") - positive and edge cases.
 *
 * Flow: sign in with a valid username (paid user) -> hover TOOLS -> click "Custom Matchups".
 * Every behaviour checked here was seen on devbeta (Nov 2026), see CustomMatchupPage.
 *
 * KNOWN DEVBETA BUG (Nov 2026): the server answers HTTP 500 for /simulation/breakEven and
 * /simulation/expectedValue, so Break-even Odds and Expected Value stay empty.
 * TC_CM_12 and TC_CM_13 therefore FAIL until that is fixed - this is correct, they report the bug.
 */
@Feature("Custom Matchups")
public class CustomMatchupTest extends commonToAllTest {

    /** Two / three players that the simulation has data for (checked on devbeta). */
    private static final String PLAYER_A = "Scottie Scheffler";
    private static final String PLAYER_B = "Adam Scott";
    private static final String PLAYER_C = "Aaron Rai";

    private HomePage homePage;
    private LoginPage loginPage;
    private CustomMatchupPage matchup;

    @BeforeClass(alwaysRun = true)
    public void loginAndOpenCustomMatchups() {
        homePage = new HomePage(driver);
        loginPage = new LoginPage(driver);
        matchup = new CustomMatchupPage(driver);

        if (!driver.getCurrentUrl().startsWith(SiteUrls.base())) {
            driver.get(SiteUrls.of("/"));
        }
        Allure.step("Sign in with a valid username (paid user)");
        homePage.waitForPageLoad();
        homePage.clickLogin();
        loginPage.login(propertyReader.readKey("paid.username").trim(),
                propertyReader.readKey("paid.password").trim());

        Allure.step("Hover TOOLS and click Custom Matchups");
        homePage.openToolsMenuItem("Custom Matchups");
        matchup.waitForPageLoad();
    }

    // ============================================================================= helpers

    /**
     * Fresh 2-player form with Round + Void, no reload: choosing the size again empties every slot.
     * (Switching size away and back is how the page itself resets the players.)
     */
    private void resetForm() {
        if (!driver.getCurrentUrl().contains("/custom-matchup-simulator")) {
            driver.get(SiteUrls.of("/custom-matchup-simulator"));
            matchup.waitForPageLoad();
        }
        matchup.closePicker();
        matchup.selectSimType("Round");
        matchup.selectTies("Void");
        matchup.selectSize("Three");
        matchup.selectSize("Two");
    }

    private static char firstLetter(String name) {
        String plain = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return Character.toUpperCase(plain.charAt(0));
    }

    private static double pct(String v) {
        return Double.parseDouble(v.replace("%", "").trim());
    }

    private WebDriverWait waitFor(int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }

    // ======================================================================= POSITIVE: page

    @Test(priority = 1, description = "TC_CM_01: TOOLS > Custom Matchups opens the simulator for a paid user")
    @Severity(SeverityLevel.BLOCKER)
    public void verifyOpenFromToolsMenu() {
        Assert.assertTrue(driver.getCurrentUrl().contains("/custom-matchup-simulator"), "URL: " + driver.getCurrentUrl());
        Assert.assertFalse(driver.getCurrentUrl().contains("/subscribe"), "Paid user must not be sent to /subscribe.");
        Assert.assertEquals(driver.getTitle(), "Betsperts Golf | Custom Matchup Simulation", "Browser title");
        Assert.assertEquals(matchup.getHeading(), "Golf Custom Simulation", "Page heading");
    }

    @Test(priority = 2, description = "TC_CM_02: Default form - Two players, Round, Void, empty pickers, odds placeholders, Run button")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyDefaultForm() {
        resetForm();
        List<Map<String, String>> slots = matchup.readSlots();
        SoftAssert soft = new SoftAssert();
        soft.assertEquals(matchup.getActiveSize(), "Two", "Active size");
        soft.assertEquals(slots.size(), 2, "Player slots");
        soft.assertEquals(matchup.getSimType(), "Round", "Sim Type");
        soft.assertEquals(matchup.getTiesTreatedAs(), "Void", "Ties treated as");
        soft.assertEquals(matchup.getOddsPlaceholder("american"), "+120", "American odds placeholder");
        soft.assertEquals(matchup.getOddsPlaceholder("decimal"), "2.2", "Decimal odds placeholder");
        for (int i = 0; i < slots.size(); i++) {
            Map<String, String> s = slots.get(i);
            soft.assertEquals(s.get("label"), "Player #" + (i + 1), "Slot label");
            soft.assertEquals(s.get("player"), "", "Slot " + (i + 1) + " should be empty");
            soft.assertFalse(s.get("win").endsWith("%"), "No Win % before running");
            soft.assertEquals(s.get("ev"), CustomMatchupPage.EV_PLACEHOLDER, "EV placeholder");
        }
        soft.assertTrue(matchup.isRunButtonShown(), "Run Simulation button missing.");
        soft.assertFalse(matchup.isRunEnabled(), "Run Simulation should be disabled until players are chosen.");
        soft.assertAll();
    }

    @DataProvider(name = "sizes")
    public Object[][] sizes() {
        return new Object[][] {{"Two", 2}, {"Three", 3}, {"Four", 4}, {"Five", 5}, {"Six", 6}};
    }

    @Test(priority = 3, dataProvider = "sizes", description = "TC_CM_03: Each matchup size shows that many player slots")
    public void verifyMatchupSize(String size, int slots) {
        resetForm();
        if (!matchup.getSizeOptions().contains(size)) {
            throw new SkipException("'" + size + "' is only shown on wide screens (≥1200px).");
        }
        matchup.selectSize(size);
        Assert.assertEquals(matchup.getActiveSize(), size, "Active size");
        List<Map<String, String>> all = matchup.readSlots();
        Assert.assertEquals(all.size(), slots, "Player slots for " + size);
        Assert.assertEquals(all.get(slots - 1).get("label"), "Player #" + slots, "Last slot label");
    }

    @Test(priority = 4, description = "TC_CM_04: Sim Type offers Round / Event and can be switched")
    public void verifySimTypeDropdown() {
        resetForm();
        Assert.assertEquals(matchup.getSimTypeOptions(), List.of("Round", "Event"), "Sim Type options");
        matchup.selectSimType("Event");
        Assert.assertEquals(matchup.getSimType(), "Event");
        matchup.selectSimType("Round");
        Assert.assertEquals(matchup.getSimType(), "Round");
    }

    @Test(priority = 5, description = "TC_CM_05: Ties Treated as offers Void / Loss and can be switched")
    public void verifyTiesDropdown() {
        resetForm();
        Assert.assertEquals(matchup.getTiesOptions(), List.of("Void", "Loss"), "Ties options");
        matchup.selectTies("Loss");
        Assert.assertEquals(matchup.getTiesTreatedAs(), "Loss");
        matchup.selectTies("Void");
        Assert.assertEquals(matchup.getTiesTreatedAs(), "Void");
    }

    @Test(priority = 6, description = "TC_CM_06: Help tooltips explain Odds, Break-even Odds and Expected Value")
    public void verifyTooltips() {
        resetForm();
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(matchup.getTooltip("Break-even Odds").contains("expected value of zero"), "Break-even tooltip");
        soft.assertTrue(matchup.getTooltip("Expected").contains("1-unit bet"), "Expected Value tooltip");
        soft.assertAll();
    }

    // ======================================================================= POSITIVE: picker

    @Test(priority = 10, description = "TC_CM_07: Player picker lists many players, A-Z, no duplicates")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyPlayerListSorted() {
        resetForm();
        List<String> players = matchup.typeInPicker(1, "");
        matchup.closePicker();
        Allure.step("Players in the picker: " + players.size());

        Assert.assertTrue(players.size() > 50, "Expected a big player list, got " + players.size());
        Assert.assertEquals(new HashSet<>(players).size(), players.size(), "A player is listed twice.");
        // A-Z by first letter (accents removed). Full-name comparison is not used on purpose: Java and the
        // browser order punctuation differently ("A.J. Ewart" comes before "Aaron" on the site).
        for (int i = 1; i < players.size(); i++) {
            char prev = firstLetter(players.get(i - 1));
            char cur = firstLetter(players.get(i));
            Assert.assertTrue(prev <= cur, "Not in A-Z order: '" + players.get(i - 1) + "' before '" + players.get(i) + "'");
        }
    }

    @Test(priority = 11, description = "TC_CM_08: Typing in the picker filters players, ignoring upper/lower case")
    public void verifyPickerFilter() {
        resetForm();
        List<String> found = matchup.typeInPicker(1, "scott");
        matchup.closePicker();
        Allure.step("'scott' -> " + found);
        Assert.assertFalse(found.isEmpty(), "'scott' should find players.");
        Assert.assertTrue(found.stream().allMatch(n -> n.toLowerCase().contains("scott")), "Every option should contain 'scott': " + found);
        Assert.assertTrue(found.contains(PLAYER_A), PLAYER_A + " should be found by 'scott'.");
    }

    // ======================================================================= POSITIVE: odds

    @DataProvider(name = "americanToDecimal")
    public Object[][] americanToDecimal() {
        return new Object[][] {{"+120", "2.200"}, {"-200", "1.500"}, {"-110", "1.909"}, {"+100", "2.000"}, {"+250", "3.500"}};
    }

    @Test(priority = 20, dataProvider = "americanToDecimal", description = "TC_CM_09: American odds are converted to decimal")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyAmericanToDecimal(String american, String decimal) {
        resetForm();
        matchup.enterAmericanOdds(1, american);
        Assert.assertEquals(matchup.getDecimalOdds(1), decimal, american + " in decimal");
    }

    @DataProvider(name = "decimalToAmerican")
    public Object[][] decimalToAmerican() {
        return new Object[][] {{"2.2", "+120"}, {"1.5", "-200"}, {"3.5", "+250"}, {"2", "+100"}};
    }

    @Test(priority = 21, dataProvider = "decimalToAmerican", description = "TC_CM_10: Decimal odds are converted to American")
    public void verifyDecimalToAmerican(String decimal, String american) {
        resetForm();
        matchup.enterDecimalOdds(1, decimal);
        Assert.assertEquals(matchup.getAmericanOdds(1), american, decimal + " in American");
    }

    // ======================================================================= POSITIVE: simulation

    @Test(priority = 30, description = "TC_CM_11: Two-player simulation shows Win % and Tie %; with Void the win chances add up to 100%")
    @Severity(SeverityLevel.BLOCKER)
    public void verifyTwoPlayerSimulation() {
        resetForm();
        Assert.assertFalse(matchup.isRunEnabled(), "Run should be disabled before players are chosen.");
        matchup.selectPlayer(1, PLAYER_A);
        matchup.selectPlayer(2, PLAYER_B);
        Assert.assertTrue(matchup.isRunEnabled(), "Run should become enabled once every player is chosen.");
        matchup.runAndWaitForProbabilities();

        List<Map<String, String>> s = matchup.readSlots();
        Allure.step(s.get(0).get("player") + " " + s.get(0).get("win") + " vs " + s.get(1).get("player") + " " + s.get(1).get("win")
                + " (tie " + s.get(0).get("tie") + ")");
        SoftAssert soft = new SoftAssert();
        for (Map<String, String> slot : s) {
            soft.assertTrue(slot.get("win").matches("\\d{1,3}(\\.\\d+)?%"), "Win % format: " + slot.get("win"));
            soft.assertTrue(slot.get("tie").matches("\\d{1,3}(\\.\\d+)?%"), "Tie % format: " + slot.get("tie"));
        }
        double sum = pct(s.get(0).get("win")) + pct(s.get(1).get("win"));
        soft.assertTrue(Math.abs(sum - 100) <= 0.2, "With Void the two win chances should add up to 100%, got " + sum);
        soft.assertEquals(s.get(0).get("tie"), s.get(1).get("tie"), "Both players share the same tie chance.");
        soft.assertAll();
    }

    @Test(priority = 31, description = "TC_CM_12: After a run, Break-even Odds are shown for every player "
            + "(FAILS on devbeta Nov 2026: breakEven API returns HTTP 500)")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyBreakEvenOdds() {
        resetForm();
        matchup.selectPlayer(1, PLAYER_A);
        matchup.selectPlayer(2, PLAYER_B);
        matchup.runAndWaitForProbabilities();
        Assert.assertTrue(matchup.waitForBreakEven(),
                "Break-even Odds never appeared: " + matchup.readSlots() + " (on devbeta the /simulation/breakEven call returns HTTP 500)");
    }

    @Test(priority = 32, description = "TC_CM_13: With odds entered, Expected Value shows 'x per unit bet' in green (+) / red (-) "
            + "(FAILS on devbeta Nov 2026: expectedValue API returns HTTP 500)")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyExpectedValue() {
        resetForm();
        matchup.selectPlayer(1, PLAYER_A);
        matchup.selectPlayer(2, PLAYER_B);
        // odds AFTER the players: picking a player clears all odds
        matchup.enterAmericanOdds(1, "-200");
        matchup.enterAmericanOdds(2, "+250");
        matchup.runAndWaitForProbabilities();

        Assert.assertTrue(matchup.waitForExpectedValue(),
                "Expected Value never appeared: " + matchup.readSlots() + " (on devbeta the /simulation/expectedValue call returns HTTP 500)");
        for (Map<String, String> s : matchup.readSlots()) {
            double ev = Double.parseDouble(s.get("ev").replace("per unit bet", "").trim());
            String expectedColour = ev > 0 ? "green" : ev < 0 ? "red" : "";
            Assert.assertTrue(s.get("evClass").contains(expectedColour), s.get("player") + " EV " + ev + " colour: " + s.get("evClass"));
        }
    }

    @Test(priority = 33, description = "TC_CM_14: Three-player simulation shows a Win % for each player (total ≤ 100%)")
    public void verifyThreePlayerSimulation() {
        resetForm();
        matchup.selectSize("Three");
        matchup.selectPlayer(1, PLAYER_A);
        matchup.selectPlayer(2, PLAYER_B);
        matchup.selectPlayer(3, PLAYER_C);
        matchup.runAndWaitForProbabilities();

        double sum = 0;
        for (Map<String, String> s : matchup.readSlots()) {
            double win = pct(s.get("win"));
            Assert.assertTrue(win >= 0 && win <= 100, s.get("player") + " win " + win);
            sum += win;
        }
        Allure.step("Total win chance of 3 players: " + sum);
        Assert.assertTrue(sum <= 100.5, "Win chances of 3 players should not add up to more than 100%: " + sum);
    }

    @Test(priority = 34, description = "TC_CM_15: Sim Type 'Event' also gives results")
    public void verifyEventSimulation() {
        resetForm();
        matchup.selectSimType("Event");
        matchup.selectSize("Three");
        matchup.selectSize("Two");
        matchup.selectPlayer(1, PLAYER_A);
        matchup.selectPlayer(2, PLAYER_B);
        matchup.runAndWaitForProbabilities();
        Assert.assertTrue(matchup.readSlots().stream().allMatch(s -> s.get("win").endsWith("%")), "Event simulation should show Win %.");
    }

    // ======================================================================= EDGE CASES

    @Test(priority = 40, description = "TC_CM_E01: Changing the matchup size empties all players and results")
    public void verifySizeChangeClearsPlayers() {
        resetForm();
        matchup.selectPlayer(1, PLAYER_A);
        matchup.selectSize("Three");
        Assert.assertTrue(matchup.readSlots().stream().allMatch(s -> s.get("player").isEmpty()),
                "All slots should be empty after changing size: " + matchup.readSlots());
    }

    @Test(priority = 41, description = "TC_CM_E02: The same player cannot be in two slots - picking him again moves him")
    @Severity(SeverityLevel.CRITICAL)
    public void verifySamePlayerMovesSlot() {
        resetForm();
        matchup.selectPlayer(1, PLAYER_A);
        matchup.selectPlayer(2, PLAYER_A);
        Assert.assertEquals(matchup.getSelectedPlayer(2), PLAYER_A, "Player #2");
        Assert.assertEquals(matchup.getSelectedPlayer(1), "", "Player #1 should be emptied when the same player is picked again.");
    }

    @Test(priority = 42, description = "TC_CM_E03: Picking a player clears odds that were typed before")
    public void verifyPickingPlayerClearsOdds() {
        resetForm();
        matchup.enterAmericanOdds(1, "+120");
        Assert.assertEquals(matchup.getDecimalOdds(1), "2.200");
        matchup.selectPlayer(1, PLAYER_A);
        Assert.assertEquals(matchup.getAmericanOdds(1), "", "American odds should be cleared after picking a player.");
        Assert.assertEquals(matchup.getDecimalOdds(1), "", "Decimal odds should be cleared after picking a player.");
    }

    @Test(priority = 43, description = "TC_CM_E04: Odds are optional - a run without odds still gives Win % and keeps the EV hint")
    public void verifyRunWithoutOdds() {
        resetForm();
        matchup.selectPlayer(1, PLAYER_A);
        matchup.selectPlayer(2, PLAYER_B);
        matchup.runAndWaitForProbabilities();
        Assert.assertTrue(matchup.readSlots().stream().allMatch(s -> s.get("ev").equals(CustomMatchupPage.EV_PLACEHOLDER)),
                "Without odds the EV column should still say '" + CustomMatchupPage.EV_PLACEHOLDER + "'.");
    }

    // ======================================================================= NEGATIVE

    @Test(priority = 50, description = "TC_CM_N01: With no players, Run Simulation is disabled and clicking it does nothing")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyRunWithoutPlayers() {
        resetForm();
        Assert.assertFalse(matchup.isRunEnabled(), "Run Simulation should be disabled without players.");
        Assert.assertTrue(matchup.clickRunAndConfirmNothingHappens(), "Clicking the disabled Run must not run a simulation.");
    }

    @Test(priority = 51, description = "TC_CM_N02: With only Player #1, Run Simulation stays disabled")
    public void verifyRunWithOnePlayer() {
        resetForm();
        matchup.selectPlayer(1, PLAYER_A);
        Assert.assertFalse(matchup.isRunEnabled(), "Run Simulation should stay disabled until Player #2 is chosen.");
        Assert.assertTrue(matchup.clickRunAndConfirmNothingHappens(), "Clicking the disabled Run must not run a simulation.");
    }

    @Test(priority = 52, description = "TC_CM_N03: Three-player matchup with 2 players keeps Run Simulation disabled")
    public void verifyRunWithMissingThirdPlayer() {
        resetForm();
        matchup.selectSize("Three");
        matchup.selectPlayer(1, PLAYER_A);
        matchup.selectPlayer(2, PLAYER_B);
        Assert.assertFalse(matchup.isRunEnabled(), "Run Simulation should stay disabled until Player #3 is chosen.");
        Assert.assertTrue(matchup.clickRunAndConfirmNothingHappens(), "Clicking the disabled Run must not run a simulation.");
    }

    @Test(priority = 55, description = "TC_CM_N08: Changing the matchup size disables Run Simulation again")
    public void verifySizeChangeDisablesRun() {
        resetForm();
        matchup.selectPlayer(1, PLAYER_A);
        matchup.selectPlayer(2, PLAYER_B);
        Assert.assertTrue(matchup.isRunEnabled(), "Run should be enabled with both players chosen.");
        matchup.selectSize("Three");
        Assert.assertFalse(matchup.isRunEnabled(), "Run should be disabled again after changing the size.");
    }

    @DataProvider(name = "badOdds")
    public Object[][] badOdds() {
        return new Object[][] {{"abc"}, {"xyz"}, {"@#$"}};
    }

    @Test(priority = 53, dataProvider = "badOdds", description = "TC_CM_N04: Letters / symbols in the odds box are rejected")
    public void verifyInvalidOdds(String bad) {
        resetForm();
        matchup.enterAmericanOdds(1, bad);
        Assert.assertEquals(matchup.getAmericanOdds(1), "", "American odds should reject '" + bad + "'");
        Assert.assertEquals(matchup.getDecimalOdds(1), "", "Decimal odds should stay empty for '" + bad + "'");
    }

    @Test(priority = 54, description = "TC_CM_N05: A name that matches nobody shows 'No options' in the picker")
    public void verifyPickerNoMatch() {
        resetForm();
        List<String> found = matchup.typeInPicker(1, "zzqxnoplayer");
        String notice = matchup.getPickerNotice();
        matchup.closePicker();
        Assert.assertTrue(found.isEmpty(), "No options expected: " + found);
        Assert.assertEquals(notice, "No options");
    }

    // ============================================== NO ACCESS (run LAST - they log out)

    @Test(priority = 60, description = "TC_CM_N06: Logged-out visitor is sent to /subscribe (premium tool)")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyGuestRedirected() {
        openAsGuest(SiteUrls.of("/custom-matchup-simulator"));
        waitFor(30).until(d -> d.getCurrentUrl().contains("/subscribe"));
        String url = driver.getCurrentUrl();
        Assert.assertTrue(url.contains("tab=matchup-tool"), "Subscribe page should open the matchup tab: " + url);
        Assert.assertTrue(url.contains("next=/custom-matchup-simulator") || url.contains("next=%2Fcustom-matchup-simulator"),
                "Subscribe page should remember the matchup tool as 'next': " + url);
    }

    @Test(priority = 61, description = "TC_CM_N07: Free user is sent to /subscribe (premium tool)")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyFreeUserRedirected() {
        openAsGuest(SiteUrls.of("/sign-in"));
        loginPage.waitForPageLoad();
        loginPage.login(propertyReader.readKey("free.username").trim(), propertyReader.readKey("free.password").trim());
        driver.get(SiteUrls.of("/custom-matchup-simulator"));
        waitFor(30).until(d -> d.getCurrentUrl().contains("/subscribe"));
        Assert.assertTrue(driver.getCurrentUrl().contains("tab=matchup-tool"), "Free user should be sent to subscribe: " + driver.getCurrentUrl());
        openAsGuest(SiteUrls.of("/"));
    }
}
