package com.nikitagehlod.pages;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.nikitagehlod.Base.BasePage;

import io.qameta.allure.Step;

/** Betsperts Golf home page (header, sections, footer). Locators checked against devbeta. */
public class HomePage extends BasePage {

    // ------------------------------------------------------------ existing (used by LoginAccessTest)

    @FindBy(xpath = "//a[contains(@href,'/sign-in') and contains(@class,'py-0')]")
    private WebElement loginBtn;

    private final By rabbitHoleMenu = By.xpath("//a[contains(@href,'/the-rabbit-hole')]");

    // ------------------------------------------------------------------------------- header

    private final By logoLink = By.cssSelector("a.navbar-brand");
    private final By logoImage = By.cssSelector("a.navbar-brand img[alt='Betsperts Golf']");
    private final By signInLink = By.cssSelector("a[href='/sign-in']");
    private final By subscribeBtn = By.cssSelector("a.btn-primary[href='/price-plan']");

    // ---------------------------------------------------------------------------- main body

    private final By mainHeading = By.tagName("h1");
    private final By articleLinks = By.cssSelector("a.mob-text-lines-2");
    private final By seeAllLink = By.cssSelector("a.fs-6[href='/fantasy-golf']");
    private final By rabbitHoleBanner = By.cssSelector("a[href='/the-rabbit-hole']");
    private final By newsletterFrame = By.cssSelector("iframe[data-test-id='beehiiv-embed']");
    private final By themeToggle = By.cssSelector(".react-toggle");

    // ------------------------------------------------------------------------------- footer

    private final By footer = By.tagName("footer");
    private final By footerLinks = By.cssSelector("footer a[href]");
    private final By twitterLink = By.cssSelector("footer a[href*='twitter.com']");
    private final By instagramLink = By.cssSelector("footer a[href*='instagram.com']");

    public HomePage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    // ============================================================== existing methods (kept)

    public void openRabbitHole() {
        waitForLoader();
        click(rabbitHoleMenu);
    }

    public void clickLogin() {
        waitForLoader();
        click(loginBtn);
    }

    // ================================================================================ header

    /** Top menu link by its visible name: TOOLS, ANALYSIS, DISCORD, TUTORIALS (case-insensitive). */
    private By navMenu(String name) {
        return By.xpath("//a[contains(@class,'nav-link') and translate(normalize-space(),"
                + "'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='" + name.toUpperCase() + "']");
    }

    // The home page H1 sits inside a hidden "div.row.d-none" (it is there for SEO only),
    // so it is never "visible". Page load is checked with the Featured Articles heading instead.
    private final By featuredArticlesHeading = By.xpath("//h2[normalize-space()='Featured Articles']");

    @Step("Wait for home page to load")
    public HomePage waitForPageLoad() {
        waitForPageReady(); // reloads once if the site's spinner is stuck
        // The articles come from the server with NO spinner; on a slow devbeta this takes ~20s,
        // so this waits up to page.timeoutSeconds (45s) and reloads once before failing.
        waitForPageContent(featuredArticlesHeading, "Home page 'Featured Articles'");
        return this;
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public boolean isLogoDisplayed() {
        return waitForFirstDisplayed(logoImage).isDisplayed();
    }

    @Step("Click on the site logo")
    public void clickLogo() {
        safeClick(waitForFirstDisplayed(logoLink));
        waitForLoader();
    }

    public boolean isNavMenuDisplayed(String name) {
        try {
            return waitForFirstDisplayed(navMenu(name)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /** Hovers a top menu (e.g. TOOLS) and returns the names of the visible dropdown items. */
    @Step("Hover on menu {name}")
    public List<String> hoverMenuAndGetItems(String name) {
        WebElement menu = waitForFirstDisplayed(navMenu(name));
        com.nikitagehlod.utils.ClickHighlighter.showClick(driver, menu); // hover is not seen by the listener
        new Actions(driver).moveToElement(menu).perform();

        By items = By.xpath("(" + xpathOf(navMenu(name)) + ")/following-sibling::div[contains(@class,'dropdown-menu')]"
                + "//a[contains(@class,'dropdown-item')]");
        wait.until(d -> isDisplayedNow(items));

        List<String> names = new ArrayList<>();
        for (WebElement item : driver.findElements(items)) {
            if (item.isDisplayed() && !item.getText().isBlank()) {
                names.add(item.getText().trim());
            }
        }
        // move the mouse off the menu so the dropdown closes and can't cover the next test's clicks
        try {
            new Actions(driver).moveToElement(driver.findElement(featuredArticlesHeading)).perform();
        } catch (Exception ignored) {
            // nothing to hover - the menu will close on the next action anyway
        }
        return names;
    }

    /** Hovers TOOLS and clicks one of its items, e.g. openToolsMenuItem("Live Leaderboard"). */
    @Step("Open TOOLS > {item}")
    public void openToolsMenuItem(String item) {
        openMenuItem("TOOLS", item);
    }

    /** Hovers a top menu and clicks the dropdown item with this exact text (like a real user). */
    @Step("Open {menu} > {item}")
    public void openMenuItem(String menu, String item) {
        waitForLoader();
        WebElement menuLink = waitForFirstDisplayed(navMenu(menu));
        com.nikitagehlod.utils.ClickHighlighter.showClick(driver, menuLink);
        new Actions(driver).moveToElement(menuLink).perform();

        By itemLink = By.xpath("(" + xpathOf(navMenu(menu)) + ")/following-sibling::div[contains(@class,'dropdown-menu')]"
                + "//a[contains(@class,'dropdown-item') and normalize-space()=\"" + item + "\"]");
        WebElement link = waitForFirstDisplayed(itemLink);
        com.nikitagehlod.utils.ClickHighlighter.showClick(driver, link);
        // move + click in one mouse action, so the hover menu cannot close in between
        new Actions(driver).moveToElement(link).click().perform();
    }

    private static String xpathOf(By by) {
        // By.xpath(...).toString() is "By.xpath: <expr>"
        return by.toString().replaceFirst("^By\\.xpath:\\s*", "");
    }

    @Step("Click SIGN IN in the header")
    public void clickSignIn() {
        waitForLoader();
        safeClick(waitForFirstDisplayed(signInLink));
    }

    @Step("Click SUBSCRIBE in the header")
    public void clickSubscribe() {
        waitForLoader();
        safeClick(waitForFirstDisplayed(subscribeBtn));
    }

    // ============================================================================= main body

    /**
     * H1 text. Uses textContent (not getText) because on the home page the H1 is hidden,
     * and getText() returns "" for hidden elements. Works for visible H1s too (404 page).
     */
    public String getMainHeading() {
        WebElement h1 = waitForPresence(mainHeading);
        String text = h1.getDomProperty("textContent");
        return text == null ? "" : text.trim();
    }

    /** Section heading (h2 or h3) with this exact text is visible. */
    public boolean isSectionDisplayed(String headingText) {
        By heading = By.xpath("//*[self::h2 or self::h3][normalize-space()=\"" + headingText + "\"]");
        try {
            WebElement el = waitForPresence(heading);
            scrollIntoView(el);
            return el.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public int getArticleCount() {
        waitForFirstDisplayed(articleLinks);
        return driver.findElements(articleLinks).size();
    }

    /** Opens the first visible article and returns the title text that was clicked. */
    @Step("Open the first article on the home page")
    public String openFirstArticle() {
        WebElement article = waitForFirstDisplayed(articleLinks);
        String title = article.getText().trim();
        safeClick(article);
        waitForLoader();
        return title;
    }

    @Step("Click 'See All'")
    public void clickSeeAll() {
        safeClick(waitForFirstDisplayed(seeAllLink));
        waitForLoader();
    }

    /** Clicks the Rabbit Hole banner in the page body (the header copy is a hidden dropdown item). */
    @Step("Click the Rabbit Hole banner")
    public void clickRabbitHoleBanner() {
        waitForLoader();
        safeClick(waitForFirstDisplayed(rabbitHoleBanner));
    }

    public boolean isNewsletterDisplayed() {
        WebElement frame = waitForFirstDisplayed(newsletterFrame);
        scrollIntoView(frame);
        return frame.isDisplayed();
    }

    // ================================================================================= theme

    /** "light" or "dark" - the site stores the theme as a class on the html tag. */
    public String getCurrentTheme() {
        String cls = driver.findElement(By.tagName("html")).getDomProperty("className");
        if (cls != null && cls.contains("dark")) {
            return "dark";
        }
        return "light";
    }

    @Step("Toggle light/dark theme")
    public void toggleTheme() {
        String before = getCurrentTheme();
        WebElement toggle = waitForFirstDisplayed(themeToggle);
        scrollIntoView(toggle);
        jsClick(toggle);
        wait.until(d -> !getCurrentTheme().equals(before));
    }

    // ================================================================================ footer

    public boolean isFooterDisplayed() {
        WebElement f = waitForPresence(footer);
        scrollIntoView(f);
        return f.isDisplayed();
    }

    public String getFooterText() {
        return waitForPresence(footer).getText();
    }

    /** Clicks a footer link by its visible text, e.g. "About Us", "Terms of Use". */
    @Step("Click footer link {text}")
    public void clickFooterLink(String text) {
        WebElement link = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//footer//a[normalize-space()=\"" + text + "\"]")));
        scrollIntoView(link);
        safeClick(link);
        waitForLoader();
    }

    public String getTwitterHref() {
        return waitForPresence(twitterLink).getDomAttribute("href");
    }

    public String getInstagramHref() {
        return waitForPresence(instagramLink).getDomAttribute("href");
    }

    // ============================================================================ all links

    /**
     * Every link on the page that points to this site (absolute URLs, no duplicates,
     * no #anchors, no mailto/tel/javascript links).
     */
    public Set<String> getInternalLinks(String siteHost) {
        Set<String> links = new LinkedHashSet<>();
        for (WebElement a : driver.findElements(By.cssSelector("a[href]"))) {
            try {
                // the "href" DOM property is always the absolute URL
                String href = a.getDomProperty("href");
                if (href == null || !href.startsWith("http")) {
                    continue;
                }
                String noAnchor = href.split("#")[0];
                if (java.net.URI.create(noAnchor).getHost() != null
                        && java.net.URI.create(noAnchor).getHost().equalsIgnoreCase(siteHost)) {
                    links.add(noAnchor);
                }
            } catch (StaleElementReferenceException | IllegalArgumentException ignored) {
                // element re-rendered or odd href - skip it
            }
        }
        return links;
    }

    /** Scrolls down the page so lazy sections (footer, newsletter) are rendered. */
    public void scrollThroughPage() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Number height = (Number) js.executeScript("return document.body.scrollHeight");
        for (long y = 0; height != null && y < height.longValue(); y += 800) {
            js.executeScript("window.scrollTo(0, arguments[0]);", y);
        }
        scrollToTop();
    }
}
