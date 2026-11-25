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
| `pages/LiveLeaderboardPage.java` | New: Live Leaderboard info block, tabs, rounds, display modes, search, sort, favourites filter, groups, CSV, paywall |
| `testPages/LiveLeaderboardTest.java` | Live Leaderboard tests |
| `pages/SignUpPage.java` | New: sign-up form, validation toasts, change plan, legal links |
| `pages/LoginPage.java` | Sign-in page: fields, toasts, show/hide password, forgot password, session check (`login()` / `login2()` kept) |
| `utils/SiteUrls.java` | Base URL / path helper |
| `utils/TestDataGenerator.java` | Unique username + email per run |
| `utils/LinkChecker.java` | HTTP broken-link check |
| `basetest/commonToAllTest.java` | Added `openAsGuest(url)` |
| `testPages/HomePageTest.java` | Home page tests |
| `testPages/SignUpTest.java` | Sign-up tests |
| `testng-sanity.xml` | Suite: Home + Sign Up + Login + Live Leaderboard + Rabbit Hole |
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

## Login – `LoginAccessTest`

| ID | Type | Check |
|---|---|---|
| TC_LOGIN_01 | Positive | Title, subtitle, labels, placeholders, Sign IN disabled, password hidden, links shown |
| TC_LOGIN_02 | Positive | SIGN IN in the header opens the sign-in page |
| TC_LOGIN_03 | Positive | Sign IN enabled when both fields filled |
| TC_LOGIN_04 | Positive | Eye icon shows/hides the password |
| TC_LOGIN_05 | Positive | "Forgot Password?" → Reset Password page |
| TC_LOGIN_06 | Positive | Valid username login → home page + session |
| TC_LOGIN_07 | Positive | Valid email login works |
| TC_LOGIN_08 | Positive | Session survives a page refresh |
| TC_LOGIN_09 | Positive | `/sign-in?next=/tutorials` → back on /tutorials after login |
| TC_LOGIN_10 | Positive | Rabbit Hole paywall "Sign in here" → paid user lands in the Rabbit Hole |
| verifyFreeUser | Positive | Free user is sent to the Premium page from Rabbit Hole |
| verifyPaidUser | Positive | Paid user can open the Rabbit Hole |
| TC_LOGIN_N01 | Negative | Sign IN disabled for 3 empty-field combinations |
| TC_LOGIN_N02 | Negative | Spaces-only username → "Username/Email is required!" |
| TC_LOGIN_N03 | Negative | Spaces-only password → "Password is required!" |
| TC_LOGIN_N04 | Negative | 6 wrong-credential cases (wrong password, unknown user/email, data.properties invalid creds, SQL-injection text, script tag) → error toast, not logged in |
| TC_LOGIN_N05 | Negative | Password with swapped letter case is rejected |

Only **one** wrong-password attempt is made against the real paid account, so it cannot get locked.

## Live Leaderboard – `LiveLeaderboardTest`

Flow: sign in as the paid user → hover TOOLS → click **Live Leaderboard**. Checks are generic (no hard-coded tournament or players).

| ID | Type | Check |
|---|---|---|
| TC_LB_01 | Positive | TOOLS > Live Leaderboard opens the table for a paid user, no paywall |
| TC_LB_02 | Positive | Tournament name, "Course, City, Country", PAR (60–80), YARDAGE ("7005 yd") |
| TC_LB_03 | Positive | Default view: 8 stat tabs, SG + Tournament + Average + Full Leaderboard selected, favourites off, CSV file name |
| TC_LB_04 | Positive | SG columns in order: Score, Total, Pos, SG:TOT … SG:T2G |
| TC_LB_05 | Positive | Every row: player, score, valid position (T5 / CUT / WD …), numeric SG:TOT, no duplicate players |
| TC_LB_06 | Positive | 7 stat tabs (data-driven): tab selected, own key column + Score/Total/Pos, rows shown |
| TC_LB_07 | Positive | Course Metrics: Year + Scoring AVG, no player positions; favourites + Rank/Average/Total + View-as options hidden |
| TC_LB_08 | Positive | R1–R4 (data-driven): round selected, round column added |
| TC_LB_09 | Positive | R3 never lists more players than R1 (the cut) |
| TC_LB_10 | Positive | Rank = whole numbers ≥ 1, Average = 2 decimals, Total differs from Average |
| TC_LB_11 | Positive | Partial, lower-case last-name search finds the player; all results match |
| TC_LB_12 | Positive | Full-name search shows exactly that player |
| TC_LB_13 | Positive | Clearing search restores the full field |
| TC_LB_14 | Positive | Clicking SG:TOT sorts it; clicking again reverses the order |
| TC_LB_15 | Positive | "Show Favorites Only (n)" shows exactly n starred players; untick restores |
| TC_LB_16 | Positive | Player Groups: several non-empty groups; back to one table |
| TC_LB_17 | Positive | CSV: Live-leaderboard.csv, Player column, same player count and first player as the table |
| TC_LB_18 | Positive | SG:TOT header tooltip explains "Strokes gained" |
| TC_LB_N01 | Negative | No-match search → "No records match your current search and filters", 0 rows |
| TC_LB_N02 | Negative | 5 script / SQL / special-character searches: no alert, no rows, message shown |
| TC_LB_N03 | Negative | 300-character search does not break the page |
| TC_LB_N04 | Edge | Spaces-only search: all players or "No records" (devbeta shows none – possible bug, logged in report) |
| TC_LB_N05 | Edge | Favourites-only + no-match search → "No records" |
| TC_LB_N06 | Edge | Search text and filter are kept when switching tab / round |
| TC_LB_N07 | Negative | Logged-out visitor: paywall + UPGRADE, no table |
| TC_LB_N08 | Negative | Paywall "Log in" → sign-in page |
| TC_LB_N09 | Negative | Free user: paywall, no table (expected rule – confirm with the team if it fails) |

Not automated on purpose: clicking favourite stars and "Reset Favorites" (they change the paid account's saved favourites).

## Notes

- `signup.createAccount=false` by default, because TC_SU_11 creates a real account on devbeta every run.
- TC_SU_N06 / N07 assert that *an* error toast appears; the exact server wording was not captured (no form was submitted while building this). Add the exact text to the assertion after the first run.
- The "Daily ($1, testing only)" plan card has a different layout and is not covered.
