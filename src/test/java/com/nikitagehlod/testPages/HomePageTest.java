package com.nikitagehlod.testPages;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.nikitagehlod.basetest.commonToAllTest;
import com.nikitagehlod.pages.HomePage;
import com.nikitagehlod.utils.LinkChecker;
import com.nikitagehlod.utils.SiteUrls;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

/**
 * Home page sanity - positive and negative cases.
 * Every test starts on the home page as a logged-out guest.
 */
@Feature("Home Page")
public class HomePageTest extends commonToAllTest {

    private HomePage homePage;

    /** Home page is opened ONCE (as a guest) for the whole class. */
    @BeforeClass(alwaysRun = true)
    public void initPages() {
        homePage = new HomePage(driver);
        openAsGuest(SiteUrls.of("/"));
        homePage.waitForPageLoad();
    }

    /**
     * No refresh before every test: the page is only re-opened when the previous test
     * navigated away from it (e.g. clicked SIGN IN or an article). Otherwise we just scroll to top.
     */
    @BeforeMethod(alwaysRun = true)
    public void backToHomeIfNeeded() {
        if (!isOnHomePage()) {
            driver.get(SiteUrls.of("/"));
            homePage.waitForPageLoad();
        }
        homePage.scrollToTop();
    }

    private boolean isOnHomePage() {
        String current = driver.getCurrentUrl().split("[?#]")[0].replaceAll("/+$", "");
        return current.equalsIgnoreCase(SiteUrls.base());
    }

    // =========================================================================== POSITIVE

    @Test(priority = 1, description = "TC_HOME_01: Home page opens with the correct URL and title")
    @Severity(SeverityLevel.BLOCKER)
    public void verifyHomePageLoads() {
        Allure.step("Check URL and page title");
        Assert.assertTrue(driver.getCurrentUrl().startsWith(SiteUrls.base()),
                "Home page URL is wrong: " + driver.getCurrentUrl());
        Assert.assertTrue(homePage.getTitle().contains("Betsperts Golf"),
                "Page title should contain 'Betsperts Golf' but was: " + homePage.getTitle());
    }

    @Test(priority = 2, description = "TC_HOME_02: Logo is displayed and takes the user back to the home page")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyLogoNavigatesHome() {
        Assert.assertTrue(homePage.isLogoDisplayed(), "Logo is not displayed.");

        Allure.step("Go to another page, then click the logo");
        driver.get(SiteUrls.of("/tutorials"));
        homePage.clickLogo();

        waitFor().until(d -> d.getCurrentUrl().replaceAll("/+$", "").equals(SiteUrls.base()));
        Assert.assertEquals(homePage.getMainHeading(), "PGA Golf Betting Picks & Expert Analysis");
    }

    @Test(priority = 3, description = "TC_HOME_03: Main heading text is correct")
    public void verifyMainHeading() {
        Assert.assertEquals(homePage.getMainHeading(), "PGA Golf Betting Picks & Expert Analysis",
                "Main (H1) heading text is wrong.");
    }

    @Test(priority = 4, description = "TC_HOME_04: All top menu items are visible")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyTopMenus() {
        SoftAssert soft = new SoftAssert();
        for (String menu : new String[] {"TOOLS", "ANALYSIS", "DISCORD", "TUTORIALS"}) {
            soft.assertTrue(homePage.isNavMenuDisplayed(menu), "Top menu not visible: " + menu);
        }
        soft.assertAll();
    }

    @Test(priority = 5, description = "TC_HOME_05: TOOLS dropdown shows the tool pages on hover")
    public void verifyToolsDropdown() {
        List<String> items = homePage.hoverMenuAndGetItems("TOOLS");
        Allure.step("TOOLS items: " + items);

        SoftAssert soft = new SoftAssert();
        for (String expected : new String[] {"The Rabbit Hole", "DFS Optimizer", "Live Leaderboard",
                "Tournament Simulation", "Custom Matchups", "Course Database", "Player Database",
                "Tournament Cheat Sheet"}) {
            soft.assertTrue(items.contains(expected), "TOOLS menu is missing: " + expected);
        }
        soft.assertAll();
    }

    @Test(priority = 6, description = "TC_HOME_06: ANALYSIS dropdown shows the article categories on hover")
    public void verifyAnalysisDropdown() {
        List<String> items = homePage.hoverMenuAndGetItems("ANALYSIS");
        Allure.step("ANALYSIS items: " + items);

        SoftAssert soft = new SoftAssert();
        for (String expected : new String[] {"All Articles", "Betting", "DFS"}) {
            soft.assertTrue(items.contains(expected), "ANALYSIS menu is missing: " + expected);
        }
        soft.assertAll();
    }

    @Test(priority = 7, description = "TC_HOME_07: SIGN IN opens the sign-in page")
    @Severity(SeverityLevel.CRITICAL)
    public void verifySignInNavigation() {
        homePage.clickSignIn();
        waitFor().until(d -> d.getCurrentUrl().contains("/sign-in"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/sign-in"), "SIGN IN did not open /sign-in");
    }

    @Test(priority = 8, description = "TC_HOME_08: SUBSCRIBE opens the price plan page")
    @Severity(SeverityLevel.CRITICAL)
    public void verifySubscribeNavigation() {
        homePage.clickSubscribe();
        waitFor().until(d -> d.getCurrentUrl().contains("/price-plan"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/price-plan"), "SUBSCRIBE did not open /price-plan");
    }

    @DataProvider(name = "homeSections")
    public Object[][] homeSections() {
        return new Object[][] {
                {"Featured Articles"},
                {"Latest Articles"},
                {"Pre-Tournament Player Projections"},
                {"Golf Betting Topics"},
                {"Subscribe for Free Weekly Newsletter"},
        };
    }

    @Test(priority = 9, dataProvider = "homeSections", description = "TC_HOME_09: Each home page section is displayed")
    public void verifySectionDisplayed(String section) {
        Assert.assertTrue(homePage.isSectionDisplayed(section), "Section not displayed: " + section);
    }

    @Test(priority = 10, description = "TC_HOME_10: Articles are listed and the first one opens")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyArticleOpens() {
        int count = homePage.getArticleCount();
        Allure.step("Articles on home page: " + count);
        Assert.assertTrue(count > 0, "No articles found on the home page.");

        String homeUrl = driver.getCurrentUrl();
        String title = homePage.openFirstArticle();
        Allure.step("Opened article: " + title);

        waitFor().until(d -> !d.getCurrentUrl().equals(homeUrl));
        Assert.assertFalse(driver.findElements(By.tagName("h1")).isEmpty(), "Article page has no heading.");
    }

    @Test(priority = 11, description = "TC_HOME_11: 'See All' opens the articles list")
    public void verifySeeAll() {
        homePage.clickSeeAll();
        waitFor().until(d -> d.getCurrentUrl().contains("/fantasy-golf"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/fantasy-golf"));
    }

    @Test(priority = 12, description = "TC_HOME_12: Newsletter sign-up box is displayed")
    public void verifyNewsletter() {
        Assert.assertTrue(homePage.isNewsletterDisplayed(), "Newsletter iframe not displayed.");
    }

    @Test(priority = 13, description = "TC_HOME_13: Theme toggle switches between light and dark")
    public void verifyThemeToggle() {
        String start = homePage.getCurrentTheme();
        Allure.step("Theme at start: " + start);

        homePage.toggleTheme();
        String switched = homePage.getCurrentTheme();
        Assert.assertNotEquals(switched, start, "Theme did not change after clicking the toggle.");

        homePage.toggleTheme();
        Assert.assertEquals(homePage.getCurrentTheme(), start, "Theme did not switch back.");
    }

    @Test(priority = 14, description = "TC_HOME_14: Footer shows copyright and social links")
    public void verifyFooter() {
        homePage.scrollThroughPage();
        Assert.assertTrue(homePage.isFooterDisplayed(), "Footer not displayed.");

        SoftAssert soft = new SoftAssert();
        soft.assertTrue(homePage.getFooterText().toUpperCase().contains("BETSPERTSGOLF. ALL RIGHTS RESERVED"),
                "Copyright text missing in footer.");
        soft.assertTrue(homePage.getTwitterHref().contains("twitter.com/betspertsgolf"), "Twitter link is wrong.");
        soft.assertTrue(homePage.getInstagramHref().contains("instagram.com/betspertsgolfig"), "Instagram link is wrong.");
        soft.assertAll();
    }

    @DataProvider(name = "footerLinks")
    public Object[][] footerLinks() {
        return new Object[][] {
                {"About Us", "/about-us"},
                {"Terms of Use", "/terms-of-use"},
                {"Privacy Policy", "/privacy-policy"},
                {"Contact Us", "/contact-us"},
                {"Accessibility Statement", "/accessibility-statement"},
                {"Tutorials", "/tutorials"},
        };
    }

    @Test(priority = 15, dataProvider = "footerLinks", description = "TC_HOME_15: Footer links open the right page")
    public void verifyFooterLink(String linkText, String expectedPath) {
        homePage.scrollThroughPage();
        homePage.clickFooterLink(linkText);
        waitFor().until(d -> d.getCurrentUrl().contains(expectedPath));
        Assert.assertTrue(driver.getCurrentUrl().contains(expectedPath),
                linkText + " opened " + driver.getCurrentUrl() + " instead of " + expectedPath);
    }

    @Test(priority = 16, description = "TC_HOME_16: No broken internal links on the home page")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyNoBrokenLinks() {
        homePage.scrollThroughPage();
        Set<String> links = homePage.getInternalLinks(SiteUrls.host());
        Allure.step("Checking " + links.size() + " internal links");
        Assert.assertFalse(links.isEmpty(), "No internal links found on the home page.");

        Map<String, Integer> broken = LinkChecker.findBroken(links);
        Assert.assertTrue(broken.isEmpty(), "Broken links (url=status): " + broken);
    }

    // =========================================================================== NEGATIVE

    @Test(priority = 20, description = "TC_HOME_N01: A page that does not exist shows 'Page Not Found'")
    public void verifyInvalidUrlShows404() {
        driver.get(SiteUrls.of("/this-page-does-not-exist-qa"));
        String heading = homePage.getMainHeading();
        Assert.assertTrue(heading.equalsIgnoreCase("Page Not Found"),
                "Expected 'Page Not Found' but heading was: " + heading);
        Assert.assertEquals(LinkChecker.status(SiteUrls.of("/this-page-does-not-exist-qa")), 404,
                "Invalid URL should answer with HTTP 404.");
    }

    @Test(priority = 21, description = "TC_HOME_N02: Guest clicking the Rabbit Hole banner gets the premium wall, not the tool")
    @Severity(SeverityLevel.CRITICAL)
    public void verifyGuestCannotOpenRabbitHoleFromBanner() {
        homePage.clickRabbitHoleBanner();
        waitFor().until(d -> d.getCurrentUrl().contains("/subscribe"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/subscribe"),
                "Guest should be sent to /subscribe but was on: " + driver.getCurrentUrl());
    }

    @Test(priority = 22, description = "TC_HOME_N03: Guest opening the Rabbit Hole URL directly is redirected to subscribe")
    public void verifyGuestCannotOpenRabbitHoleByUrl() {
        driver.get(SiteUrls.of("/the-rabbit-hole"));
        waitFor().until(d -> d.getCurrentUrl().contains("/subscribe"));
        Assert.assertTrue(driver.getCurrentUrl().contains("next=/the-rabbit-hole")
                        || driver.getCurrentUrl().contains("next=%2Fthe-rabbit-hole"),
                "Redirect should remember the Rabbit Hole as 'next' page: " + driver.getCurrentUrl());
    }

    // ----------------------------------------------------------------------------- helper

    private org.openqa.selenium.support.ui.WebDriverWait waitFor() {
        return new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(15));
    }
}
