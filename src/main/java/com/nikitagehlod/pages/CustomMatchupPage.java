package com.nikitagehlod.pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.nikitagehlod.Base.BasePage;

import io.qameta.allure.Step;

/**
 * /custom-matchup-simulator (TOOLS > Custom Matchups) - "Golf Custom Simulation". Checked on devbeta, Nov 2026.
 *
 * - Matchup size buttons: Two, Three, Four, Five (wide screens only), Six -> that many player slots.
 *   Changing the size empties every slot.
 * - "Sim Type" dropdown: Round / Event.  "Ties Treated as" dropdown: Void / Loss.
 * - Each slot: player picker (react-select "Select or type", 378 players A-Z, "No options" when nothing matches),
 *   odds in American (#american, "+120") OR decimal (#decimal, "2.2") - typing one fills the other,
 *   results: Win %, Tie %, Break-even Odds, Expected Value ("Enter odds to calculate EV" / "x per unit bet").
 *   Picking a player clears the odds of every slot. A player picked in a 2nd slot is removed from the 1st.
 * - "Run Simulation" is DISABLED (div.run-simulation.disabled, the button has no action) until EVERY slot has a
 *   player; changing the matchup size disables it again. (The page code also contains an alert
 *   "Please select players", but it can never be reached because of this - checked on devbeta, Nov 2026.)
 * - The page is drawn twice (desktop table for wide screens, player cards for small ones); this page object
 *   always uses the VISIBLE one.
 * - Premium tool: guests and free users go to /subscribe?tab=matchup-tool&next=/custom-matchup-simulator.
 */
public class CustomMatchupPage extends BasePage {

    public static final String MSG_SELECT_PLAYERS = "Please select players";
    public static final String EV_PLACEHOLDER = "Enter odds to calculate EV";

    private final By section = By.cssSelector("section.matchup-simulation");
    private final By heading = By.cssSelector("section.matchup-simulation .sim-head");
    private final By runButton = By.cssSelector("section.matchup-simulation .run-simulation button");
    private final By dropdownButtons = By.cssSelector("section.matchup-simulation button.grey_b_btn");
    private final By openDropdownItems = By.cssSelector("section.matchup-simulation .dropdown-menu.show .dropdown-item");
    private final By pickerOptions = By.cssSelector("[class*='playerName__option']");
    private final By pickerNoOptions = By.cssSelector("[class*='playerName__menu-notice']");

    public CustomMatchupPage(WebDriver driver) {
        super(driver);
    }

    // ================================================================= loading

    @Step("Wait for Custom Matchups to load")
    public CustomMatchupPage waitForPageLoad() {
        wait.until(d -> d.getCurrentUrl().contains("/custom-matchup-simulator"));
        waitForPageReady();
        waitForPageContent(runButton, "Custom Matchups 'Run Simulation' button");
        return this;
    }

    public String getHeading() {
        return waitForVisibility(heading).getText().trim();
    }

    // ============================================================ slots (visible layout)

    /** JS that returns the visible player slots: desktop table rows, or the mobile cards. */
    private static final String VISIBLE_SLOTS_JS =
            "var s = document.querySelector('section.matchup-simulation');"
          + "var desk = s.querySelector('.players-table.desktop-view');"
          + "var slots = (desk && desk.offsetParent) ? Array.from(desk.querySelectorAll('tbody tr'))"
          + "  : Array.from(s.querySelectorAll('.player-card')).filter(function(c){ return c.offsetParent; });";

    @SuppressWarnings("unchecked")
    private List<WebElement> slots() {
        return (List<WebElement>) js(VISIBLE_SLOTS_JS + "return slots;");
    }

    public int getSlotCount() {
        return slots().size();
    }

    private WebElement slot(int playerNo) {
        List<WebElement> all = slots();
        if (playerNo < 1 || playerNo > all.size()) {
            throw new IllegalArgumentException("No Player #" + playerNo + " (there are " + all.size() + " slots)");
        }
        return all.get(playerNo - 1);
    }

    /**
     * Everything shown in every visible slot, read in one call:
     * label, player, american, decimal, win, tie, breakEven, ev, evClass.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> readSlots() {
        Object result = js(VISIBLE_SLOTS_JS
                + "var isDesk = !!(desk && desk.offsetParent);"
                + "function t(e){ return e ? e.textContent.trim() : ''; }"
                + "return slots.map(function(r){"
                + "  var tds = r.querySelectorAll('td');"
                + "  var evBox = r.querySelector('.expected-value');"
                + "  return {"
                + "   label: t(r.querySelector('.player_count')) || '',"
                + "   player: t(r.querySelector('[class*=playerName__single-value]')),"
                + "   american: (r.querySelector('#american') || {}).value || '',"
                + "   decimal: (r.querySelector('#decimal') || {}).value || '',"
                + "   win: isDesk ? t(r.querySelector('.win-probablity p:last-child'))"
                + "               : t(r.querySelector('.win-perc-column p:last-child')),"
                + "   tie: isDesk ? t(tds[2] && tds[2].querySelector('.d-flex > div:nth-child(2) p:last-child'))"
                + "               : t(r.querySelector('.tie-perc-column p:last-child')),"
                + "   breakEven: isDesk ? t(tds[3] && tds[3].querySelector('p'))"
                + "                     : t(r.querySelector('.break-even-odds-wraaper p:last-child')),"
                + "   ev: t(evBox),"
                + "   evClass: evBox ? ((evBox.tagName === 'P' ? evBox : evBox.querySelector('p')) || evBox).className"
                + "                      + ' ' + evBox.className : ''"
                + "  }; });");
        return result == null ? new ArrayList<>() : (List<Map<String, String>>) result;
    }

    // ======================================================================= options

    /** Size buttons shown on screen: Two, Three, Four, (Five), Six. */
    public List<String> getSizeOptions() {
        List<String> names = new ArrayList<>();
        for (String n : List.of("Two", "Three", "Four", "Five", "Six")) {
            if (sizeButton(n) != null) {
                names.add(n);
            }
        }
        return names;
    }

    private WebElement sizeButton(String name) {
        for (WebElement b : driver.findElements(By.cssSelector("section.matchup-simulation button"))) {
            if (b.isDisplayed() && b.getText().trim().equalsIgnoreCase(name)) {
                return b;
            }
        }
        return null;
    }

    public String getActiveSize() {
        for (String n : List.of("Two", "Three", "Four", "Five", "Six")) {
            WebElement b = sizeButton(n);
            if (b != null && String.valueOf(b.getDomAttribute("class")).contains("active")) {
                return n;
            }
        }
        return "";
    }

    @Step("Choose matchup size {name}")
    public void selectSize(String name) {
        WebElement b = sizeButton(name);
        if (b == null) {
            throw new IllegalStateException("Size '" + name + "' is not shown (Five only appears on wide screens).");
        }
        safeClick(b);
        int expected = List.of("Two", "Three", "Four", "Five", "Six").indexOf(name) + 2;
        wait.until(d -> getSlotCount() == expected);
    }

    /** Current text of the Sim Type (index 0) or Ties (index 1) dropdown button. */
    private WebElement dropdown(int index) {
        List<WebElement> shown = new ArrayList<>();
        for (WebElement b : driver.findElements(dropdownButtons)) {
            if (b.isDisplayed()) {
                shown.add(b);
            }
        }
        return shown.get(index);
    }

    public String getSimType() {
        return dropdown(0).getText().trim();
    }

    public String getTiesTreatedAs() {
        return dropdown(1).getText().trim();
    }

    public List<String> getSimTypeOptions() {
        return openAndReadDropdown(0);
    }

    public List<String> getTiesOptions() {
        return openAndReadDropdown(1);
    }

    private List<String> openAndReadDropdown(int index) {
        safeClick(dropdown(index));
        waitForFirstDisplayed(openDropdownItems);
        List<String> items = new ArrayList<>();
        for (WebElement i : driver.findElements(openDropdownItems)) {
            items.add(i.getText().trim());
        }
        // close it again: Escape, and if the menu is still open, click the toggle button once more
        dropdown(index).sendKeys(Keys.ESCAPE);
        if (isDisplayedNow(openDropdownItems)) {
            safeClick(dropdown(index));
        }
        return items;
    }

    @Step("Sim Type: {value}")
    public void selectSimType(String value) {
        chooseFromDropdown(0, value);
    }

    @Step("Ties treated as: {value}")
    public void selectTies(String value) {
        chooseFromDropdown(1, value);
    }

    private void chooseFromDropdown(int index, String value) {
        if (dropdown(index).getText().trim().equalsIgnoreCase(value)) {
            return;
        }
        safeClick(dropdown(index));
        WebElement item = wait.until(d -> {
            for (WebElement i : d.findElements(openDropdownItems)) {
                if (i.isDisplayed() && i.getText().trim().equalsIgnoreCase(value)) {
                    return i;
                }
            }
            return null;
        });
        safeClick(item);
        wait.until(d -> dropdown(index).getText().trim().equalsIgnoreCase(value));
    }

    /** data-tooltip-content of a column header / label containing this text ("" if none). */
    public String getTooltip(String labelText) {
        Object tip = js("var want = arguments[0];"
                + "var hits = Array.from(document.querySelectorAll('section.matchup-simulation [data-tooltip-content]'))"
                + ".filter(function(e){ var label = e.textContent + ' ' + (e.parentElement ? e.parentElement.textContent : '');"
                + "  return e.offsetParent && label.indexOf(want) >= 0; });"
                + "return hits.length ? hits[0].getAttribute('data-tooltip-content') : '';", labelText);
        return String.valueOf(tip);
    }

    // =================================================================== player picker

    /** Opens the picker of Player #n, types the text and returns the option names shown. */
    @Step("Type '{text}' in the Player #{playerNo} picker")
    public List<String> typeInPicker(int playerNo, String text) {
        WebElement control = slot(playerNo).findElement(By.cssSelector("[class*='playerName__control']"));
        safeClick(control);
        WebElement input = slot(playerNo).findElement(By.cssSelector("input.playerName__input"));
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        if (!text.isEmpty()) {
            input.sendKeys(text);
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(d -> !d.findElements(pickerOptions).isEmpty() || !d.findElements(pickerNoOptions).isEmpty());
        } catch (TimeoutException ignored) {
            // the menu did not open - the empty list says so
        }
        return getPickerOptions();
    }

    @SuppressWarnings("unchecked")
    public List<String> getPickerOptions() {
        Object names = js("return Array.from(document.querySelectorAll(\"[class*='playerName__option']\"))"
                + ".map(function(o){ return o.textContent.trim(); });");
        return names == null ? new ArrayList<>() : (List<String>) names;
    }

    public String getPickerNotice() {
        List<WebElement> notice = driver.findElements(pickerNoOptions);
        return notice.isEmpty() ? "" : notice.get(0).getText().trim();
    }

    /** Closes an open picker menu without choosing. */
    public void closePicker() {
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();
    }

    /** Picks a player by exact name for Player #n (typing it, then clicking the matching option). */
    @Step("Player #{playerNo}: {name}")
    public void selectPlayer(int playerNo, String name) {
        typeInPicker(playerNo, name);
        WebElement option = wait.until(d -> {
            for (WebElement o : d.findElements(pickerOptions)) {
                if (o.getText().trim().equals(name)) {
                    return o;
                }
            }
            return null;
        });
        safeClick(option);
        wait.until(d -> name.equals(readSlots().get(playerNo - 1).get("player")));
    }

    public String getSelectedPlayer(int playerNo) {
        return readSlots().get(playerNo - 1).get("player");
    }

    // ========================================================================= odds

    private void typeInto(WebElement input, String value) {
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        if (value != null && !value.isEmpty()) {
            input.sendKeys(value);
        }
    }

    @Step("Player #{playerNo} American odds: {odds}")
    public void enterAmericanOdds(int playerNo, String odds) {
        typeInto(slot(playerNo).findElement(By.cssSelector("input#american")), odds);
    }

    @Step("Player #{playerNo} decimal odds: {odds}")
    public void enterDecimalOdds(int playerNo, String odds) {
        typeInto(slot(playerNo).findElement(By.cssSelector("input#decimal")), odds);
    }

    public String getAmericanOdds(int playerNo) {
        return readSlots().get(playerNo - 1).get("american");
    }

    public String getDecimalOdds(int playerNo) {
        return readSlots().get(playerNo - 1).get("decimal");
    }

    public String getOddsPlaceholder(String type) {
        return slot(1).findElement(By.cssSelector("input#" + type)).getDomAttribute("placeholder");
    }

    // ========================================================================= run

    public boolean isRunButtonShown() {
        return isDisplayedNow(runButton);
    }

    /** True when Run Simulation is active (its container has no 'disabled' class) - only when every slot has a player. */
    public boolean isRunEnabled() {
        for (WebElement container : driver.findElements(By.cssSelector("section.matchup-simulation .run-simulation"))) {
            if (container.isDisplayed()) {
                return !String.valueOf(container.getDomAttribute("class")).contains("disabled");
            }
        }
        return false;
    }

    /**
     * Clicks Run and waits a few seconds: true when NOTHING happened (no Win % appeared in any slot).
     * Used for the disabled button.
     */
    public boolean clickRunAndConfirmNothingHappens() {
        String message = runAndReadAlert(); // also catches an alert, if the site ever shows one
        try {
            new WebDriverWait(driver, Duration.ofSeconds(4)).pollingEvery(Duration.ofMillis(300))
                    .until(d -> readSlots().stream().anyMatch(s -> s.get("win").endsWith("%")));
            return false; // results appeared
        } catch (TimeoutException expected) {
            return message.isEmpty();
        }
    }

    @Step("Click Run Simulation")
    public void clickRun() {
        safeClick(waitForVisibility(runButton));
    }

    /** Runs and waits until every slot shows a Win %. (devbeta can take ~15-20s.) */
    @Step("Run Simulation and wait for the results")
    public void runAndWaitForProbabilities() {
        if (!isRunEnabled()) {
            throw new IllegalStateException("Run Simulation is disabled - every player slot needs a player. Slots: " + readSlots());
        }
        clickRun();
        new WebDriverWait(driver, Duration.ofSeconds(pageContentTimeoutSeconds()))
                .pollingEvery(Duration.ofMillis(500))
                .until(d -> readSlots().stream().allMatch(s -> s.get("win").endsWith("%")));
    }

    /** Waits (max page.timeoutSeconds) until every slot shows a break-even value; false if it never does. */
    public boolean waitForBreakEven() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(pageContentTimeoutSeconds()))
                    .pollingEvery(Duration.ofMillis(500))
                    .until(d -> readSlots().stream().allMatch(s -> !s.get("breakEven").matches("-*|")));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Waits until every slot shows "... per unit bet"; false if it never does. */
    public boolean waitForExpectedValue() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(pageContentTimeoutSeconds()))
                    .pollingEvery(Duration.ofMillis(500))
                    .until(d -> readSlots().stream().allMatch(s -> s.get("ev").contains("per unit bet")));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Clicks Run and returns the message the page shows with alert() (e.g. "Please select players"), or "".
     *
     * The page's alert() is replaced just before the click by a small function that only RECORDS the message,
     * so no real browser popup opens. A real popup is fragile in Selenium: it is closed automatically as soon
     * as any other command (scroll, highlight, JS click) reaches the page, and then its text is lost.
     * The original alert() is put back afterwards.
     */
    @Step("Run Simulation and read the alert message")
    public String runAndReadAlert() {
        js("window.__qaAlerts = [];"
                + "if (!window.__qaOriginalAlert) { window.__qaOriginalAlert = window.alert; }"
                + "window.alert = function(msg) { window.__qaAlerts.push(String(msg)); };");
        try {
            clickRun();
            return new WebDriverWait(driver, Duration.ofSeconds(5))
                    .pollingEvery(Duration.ofMillis(200))
                    .until(d -> {
                        Object first = js("return (window.__qaAlerts && window.__qaAlerts.length) ? window.__qaAlerts[0] : null;");
                        return first == null ? null : String.valueOf(first);
                    });
        } catch (TimeoutException noMessage) {
            return "";
        } finally {
            js("if (window.__qaOriginalAlert) { window.alert = window.__qaOriginalAlert; }");
        }
    }

    // ========================================================================= util

    private Object js(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }
}
