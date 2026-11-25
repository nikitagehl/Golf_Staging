# Betsperts Golf – Site Sanity Suite

Run the whole sanity suite:

```
mvn clean test -Dsurefire.suiteXmlFiles=testng-sanity.xml
allure serve allure-results
```

Or right-click `testng-sanity.xml` in IntelliJ → Run.

## New / changed files

| File | What it is |
|---|---|
| `pages/HomePage.java` | Extended: header, menus, sections, theme, footer, link collection (old `clickLogin()` / `openRabbitHole()` kept) |
| `pages/PricePlanPage.java` | New: plan cards + GET PLAN |
| `pages/SignUpPage.java` | New: sign-up form, validation toasts, change plan, legal links |
| `pages/LoginPage.java` | Added `clickSignUpToday()` |
| `utils/SiteUrls.java` | Base URL / path helper |
| `utils/TestDataGenerator.java` | Unique username + email per run |
| `utils/LinkChecker.java` | HTTP broken-link check |
| `basetest/commonToAllTest.java` | Added `openAsGuest(url)` |
| `testPages/HomePageTest.java` | Home page tests |
| `testPages/SignUpTest.java` | Sign-up tests |
| `testng-sanity.xml` | Suite: Home + Sign Up + Login access |
| `data.properties` | Added `signup.*` keys |

## Home page – `HomePageTest`

| ID | Type | Check |
|---|---|---|
| TC_HOME_01 | Positive | URL and title ("Betsperts Golf") |
| TC_HOME_02 | Positive | Logo visible; clicking it from another page returns home |
| TC_HOME_03 | Positive | H1 = "PGA Golf Betting Picks & Expert Analysis" |
| TC_HOME_04 | Positive | TOOLS, ANALYSIS, DISCORD, TUTORIALS menus visible |
| TC_HOME_05 | Positive | TOOLS hover menu lists all tools |
| TC_HOME_06 | Positive | ANALYSIS hover menu lists All Articles / Betting / DFS |
| TC_HOME_07 | Positive | SIGN IN → /sign-in |
| TC_HOME_08 | Positive | SUBSCRIBE → /price-plan |
| TC_HOME_09 | Positive | 5 sections displayed (data-driven) |
| TC_HOME_10 | Positive | Articles listed; first article opens |
| TC_HOME_11 | Positive | "See All" → /fantasy-golf |
| TC_HOME_12 | Positive | Newsletter box displayed |
| TC_HOME_13 | Positive | Light/dark theme toggle and back |
| TC_HOME_14 | Positive | Footer copyright + Twitter/Instagram links |
| TC_HOME_15 | Positive | Footer links open correct pages (data-driven) |
| TC_HOME_16 | Positive | No broken internal links (HTTP ≥ 400) |
| TC_HOME_N01 | Negative | Unknown URL → "Page Not Found" + HTTP 404 |
| TC_HOME_N02 | Negative | Guest clicking Rabbit Hole banner → /subscribe paywall |
| TC_HOME_N03 | Negative | Guest opening /the-rabbit-hole directly → /subscribe?next=/the-rabbit-hole |

## Sign up – `SignUpTest`

Flow: Home → SIGN IN → "Sign Up Today!" → Price plan → GET PLAN → form (Username, Email, Password, CONTINUE).

| ID | Type | Check |
|---|---|---|
| TC_SU_01 | Positive | Full navigation via SIGN IN → Sign Up Today → GET PLAN |
| TC_SU_02 | Positive | Navigation via SUBSCRIBE |
| TC_SU_03 | Positive | Weekly / Monthly / Yearly plans listed |
| TC_SU_04 | Positive | Chosen plan shown in order summary (data-driven) |
| TC_SU_05 | Positive | Labels, placeholders, CONTINUE disabled, password hidden |
| TC_SU_06 | Positive | CONTINUE enabled with valid data |
| TC_SU_07 | Positive | Eye icon shows/hides password |
| TC_SU_08 | Positive | CHANGE PLAN lists plans and switches to Yearly |
| TC_SU_09 | Positive | "Back to All Plans" → /price-plan |
| TC_SU_10 | Positive | Terms of Use / Privacy Policy open in new tab |
| TC_SU_11 | Positive | Real sign-up → /subscription page (**only when `signup.createAccount=true`**) |
| TC_SU_N01 | Negative | CONTINUE disabled for 7 empty-field combinations |
| TC_SU_N02 | Negative | Spaces-only username treated as empty |
| TC_SU_N03 | Negative | Spaces removed from username/email |
| TC_SU_N04 | Negative | 9 invalid emails → "Invalid Email Address!" |
| TC_SU_N05 | Negative | Spaces-only password → "Password is required!" |
| TC_SU_N06 | Negative | Existing username rejected (stays off /subscription) |
| TC_SU_N07 | Negative | Existing email rejected (stays off /subscription) |

## Notes

- `signup.createAccount=false` by default, because TC_SU_11 creates a real account on devbeta every run.
- TC_SU_N06 / N07 assert that *an* error toast appears; the exact server wording was not captured (no form was submitted while building this). Add the exact text to the assertion after the first run.
- The "Daily ($1, testing only)" plan card has a different layout and is not covered.
