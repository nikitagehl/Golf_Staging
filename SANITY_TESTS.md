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
| `pages/TournamentSimulatorPage.java` | New: Tournament Simulation – both page states, table, sort, search, Load More, CSV, side sections |
| `testPages/TournamentSimulatorTest.java` | Tournament Simulation tests |
| `pages/CustomMatchupPage.java` | New: Custom Matchups – desktop table + mobile cards, sizes, dropdowns, player picker, odds, results, alerts |
| `testPages/CustomMatchupTest.java` | Custom Matchups tests |
| `pages/CourseDatabasePage.java` | New: Course Database – picker, facts, hole stats, metrics, past leaderboards, player search |
| `testPages/CourseDatabaseTest.java` | Course Database tests |
| `pages/SignUpPage.java` | New: sign-up form, validation toasts, change plan, legal links |
| `pages/LoginPage.java` | Sign-in page: fields, toasts, show/hide password, forgot password, session check (`login()` / `login2()` kept) |
| `utils/SiteUrls.java` | Base URL / path helper |
| `utils/TestDataGenerator.java` | Unique username + email per run |
| `utils/LinkChecker.java` | HTTP broken-link check |
| `basetest/commonToAllTest.java` | Added `openAsGuest(url)` |
| `testPages/HomePageTest.java` | Home page tests |
| `testPages/SignUpTest.java` | Sign-up tests |
| `testng-sanity.xml` | Suite: Home + Sign Up + Login + Live Leaderboard + Tournament Simulation + Custom Matchups + Course Database + Rabbit Hole |
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

## Tournament Simulation – `TournamentSimulatorTest`

Flow: sign in with a valid **username** (paid user) → hover TOOLS → click **Tournament Simulation**.
The page has two states: **projections published** (weekly, Tuesday until Thursday's tee-off) or **"not available"**.
Tests for the state not on screen are **skipped** with the reason, so each run checks whatever the site shows that day.

| ID | Type | State | Check |
|---|---|---|---|
| TC_TS_01 | Positive | both | TOOLS > Tournament Simulation opens the simulator (not /subscribe), browser title |
| TC_TS_02 | Edge | both | Exactly one state is shown – projections OR the "not available" message |
| TC_TS_03 | Positive | both | Title ends with "Betsperts Golf Projections" |
| TC_TS_04 | Positive | both | Latest Articles: article links; "See All" → /fantasy-golf |
| TC_TS_05 | Positive | both | DFS Golf Topics: /topic/ links |
| TC_TS_E01 | Positive | no projections | Exact message + picture; no table, search or Download Projections |
| TC_TS_P01 | Positive | projections | Title starts with the tournament name |
| TC_TS_P02 | Positive | projections | Columns: Player, Win %, Top 5/10/20/30/40 %, Make Cut |
| TC_TS_P03 | Positive | projections | Every row: player + /golfer/ link + 7 values 0.00%–100.00% |
| TC_TS_P04 | Edge | projections | Win ≤ Top 5 ≤ Top 10 ≤ Top 20 ≤ Top 30 ≤ Top 40 ≤ Make Cut for every player |
| TC_TS_P05 | Positive | projections | Default order Win % high → low |
| TC_TS_P06 | Positive | projections | 7 % columns (data-driven): click sorts, second click reverses |
| TC_TS_P07 | Positive | projections | Player column sorts A–Z / Z–A |
| TC_TS_P08 | Edge | projections | 40 rows first; Load More +20; hidden when ≤ 40 players |
| TC_TS_P09 | Positive | projections | Partial-name search filters; X icon clears |
| TC_TS_P10 | Positive | projections | Download Projections: players-file.csv, header, one row per player |
| TC_TS_P11 | Positive | projections | Player name opens /golfer/ page |
| TC_TS_N01 | Negative | projections | No-match search → "No records found" + "There is no record…", no Load More |
| TC_TS_N02 | Negative | projections | Script / SQL / special / 300-char search: no alert, "No records found" |
| TC_TS_N03 | Edge | projections | Clearing a no-match search restores the list |
| TC_TS_N04 | Negative | – | Logged-out visitor → /subscribe?tab=tournament-simulator&next=/tournament-simulator |
| TC_TS_N05 | Negative | – | Free user → /subscribe?tab=tournament-simulator |

Note: the projections view was not published on devbeta when these tests were written (Nov 2026); its tests are built
from the page's own code and should be confirmed on the first Tuesday–Thursday run.

## Custom Matchups – `CustomMatchupTest`

Flow: sign in with a valid **username** (paid user) → hover TOOLS → click **Custom Matchups** ("Golf Custom Simulation").

| ID | Type | Check |
|---|---|---|
| TC_CM_01 | Positive | Opens for a paid user (not /subscribe), browser title, heading |
| TC_CM_02 | Positive | Default: Two players, Round, Void, empty pickers, "+120" / "2.2" placeholders, EV hint, Run button shown but disabled |
| TC_CM_03 | Positive | Two/Three/Four/Five/Six (data-driven) → 2–6 player slots (Five only on wide screens) |
| TC_CM_04 | Positive | Sim Type: Round / Event, switchable |
| TC_CM_05 | Positive | Ties Treated as: Void / Loss, switchable |
| TC_CM_06 | Positive | Tooltips explain Break-even Odds and Expected Value |
| TC_CM_07 | Positive | Player picker: > 50 players, A–Z, no duplicates (378 on devbeta) |
| TC_CM_08 | Positive | Typing filters, ignoring upper/lower case ("scott" → Scottie Scheffler …) |
| TC_CM_09 | Positive | American → decimal: +120→2.200, -200→1.500, -110→1.909, +100→2.000, +250→3.500 |
| TC_CM_10 | Positive | Decimal → American: 2.2→+120, 1.5→-200, 3.5→+250, 2→+100 |
| TC_CM_11 | Positive | Run is enabled only once every player is chosen; 2-player run: Win % + Tie %, with Void the win chances add up to 100%, same tie % |
| TC_CM_12 | Positive | Break-even Odds shown after a run — **fails on devbeta: /simulation/breakEven returns HTTP 500** |
| TC_CM_13 | Positive | Expected Value "x per unit bet", green (+) / red (−) — **fails on devbeta: /simulation/expectedValue returns HTTP 500** |
| TC_CM_14 | Positive | 3-player run: each player has Win %, total ≤ 100% |
| TC_CM_15 | Positive | Sim Type "Event" also gives results |
| TC_CM_E01 | Edge | Changing size empties all players |
| TC_CM_E02 | Edge | Same player picked in a 2nd slot is moved (1st slot emptied) |
| TC_CM_E03 | Edge | Picking a player clears odds typed before |
| TC_CM_E04 | Edge | Odds are optional: run without odds still gives Win %, EV hint stays |
| TC_CM_N01 | Negative | No players → Run Simulation disabled; clicking it does nothing (no results, no popup) |
| TC_CM_N02 | Negative | Only Player #1 → Run stays disabled, clicking does nothing |
| TC_CM_N03 | Negative | Three-player matchup with 2 players → Run stays disabled, clicking does nothing |
| TC_CM_N04 | Negative | "abc" / "xyz" / "@#$" in odds → both odds boxes stay empty |
| TC_CM_N05 | Negative | Picker with no match → "No options" |
| TC_CM_N06 | Negative | Logged-out visitor → /subscribe?tab=matchup-tool&next=/custom-matchup-simulator |
| TC_CM_N07 | Negative | Free user → /subscribe?tab=matchup-tool |
| TC_CM_N08 | Edge | Changing the matchup size disables Run again |

Bug found while writing these tests (devbeta, Nov 2026): `POST /proxy/user/simulation/breakEven` and
`POST /proxy/user/simulation/expectedValue` return **HTTP 500**, so Break-even Odds and Expected Value never appear.
Also: the page code has an alert "Please select players", but it can never show – Run Simulation is disabled until every slot has a player.

## Course Database – `CourseDatabaseTest`

Flow: sign in with a valid **username** (paid user) → hover TOOLS → click **Course Database** (opens the current tournament's course).

| ID | Type | Check |
|---|---|---|
| TC_CD_01 | Positive | Opens /course-stats/…, picker shows the course, tournament name, "Key Stats" title |
| TC_CD_02 | Positive | Course Facts: key facts present, every fact has a value, Par 69–73, yards > 5000 |
| TC_CD_03 | Positive | Correlated Courses listed, not including the course itself |
| TC_CD_04 | Positive | Hole Stats: 11 columns, holes 1–18 in order |
| TC_CD_05 | Data | Sum of hole pars = course Par in Course Facts |
| TC_CD_06 | Data | Sum of hole yards = course length (within 1%; exact gap logged) |
| TC_CD_07 | Data | Every hole: AVG +/- = AVG − Par |
| TC_CD_08 | Data | Every hole: Eagles + Birdies + Pars + Bogeys + Doubles ≈ 100% |
| TC_CD_09 | Data | Par 3/4/5, positive yards, unique positive ranks |
| TC_CD_10 | Positive | 6 Hole Stats columns (data-driven): click sorts, 2nd click reverses |
| TC_CD_11 | Positive | Course Metrics has the 11 categories |
| TC_CD_12 | Positive | Each of the 11 categories (data-driven): 5-Year AVG, PGA Tour AVG, years newest first |
| TC_CD_13 | Positive | Past Leaderboards title = tournament; years newest first; each has winner + score; newest selected |
| TC_CD_14 | Positive | LEADERBOARD columns; players link to /golfer/ in a new tab |
| TC_CD_15 | Data | Latest 3 years (data-driven): leaderboard #1 = winner and score shown in the year list |
| TC_CD_16 | Positive | All Metrics shows SG columns; LEADERBOARD switches back |
| TC_CD_17 | Positive | Player Search: last name in lower case finds the player; clearing restores |
| TC_CD_18 | Positive | Course picker: many courses A–Z, typing filters |
| TC_CD_19 | Positive | Switching course: URL, picker, tournament/facts change, 18 holes, pars add up |
| TC_CD_20 | Positive | Opening a course URL directly shows that course |
| TC_CD_N01 | Negative | Player Search no match → "No records found"; clearing removes it |
| TC_CD_N02 | Negative | Script / SQL / special / 300-char Player Search: no alert, "No records found" |
| TC_CD_N03 | Negative | Course picker no match → "No options" |
| TC_CD_N04 | Negative | Unknown course URL should say "not found" / 404 — **fails on devbeta: blank page, HTTP 200** |
| TC_CD_N05 | Edge | Browser title follows the course after switching — **fails on devbeta: title keeps the old course** |
| TC_CD_N07 | Access | Logged-out visitor stays on the page but gets the subscriber banner ("…12 key stats and correlated courses are for subscribers", Subscribe button); no Hole Stats / Past Leaderboards |
| TC_CD_N08 | Access | Free user: same subscriber banner, no course data |
| TC_CD_N09 | Edge | Guest's Correlated Courses message names the current course — **fails on devbeta: always says "Aronimink-gc"** |

Findings while writing these tests (devbeta, Nov 2026):
- `/course-stats/<unknown-course>` returns **HTTP 200 with a blank page** instead of a 404 / "not found".
- After choosing another course in the picker, the **browser title still shows the previous course**.
- For logged-out visitors, the Correlated Courses message is **hard-coded to "Aronimink-gc"** whatever course is open.
- Port Royal GC: hole yards add up to **6,821** but Course Facts say **6,828 yds** (Sea Island matches exactly).

## Notes

- `signup.createAccount=false` by default, because TC_SU_11 creates a real account on devbeta every run.
- TC_SU_N06 / N07 assert that *an* error toast appears; the exact server wording was not captured (no form was submitted while building this). Add the exact text to the assertion after the first run.
- The "Daily ($1, testing only)" plan card has a different layout and is not covered.
