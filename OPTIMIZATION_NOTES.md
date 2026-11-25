# Golf Stage RH View - Optimized

## What changed
- Set implicit wait to zero and use the framework's explicit waits to avoid mixed-wait delays.
- Removed fixed `Thread.sleep()` pauses from login, report actions, and the create-model flow; replaced them with state-based waits.
- Centralized explicit wait configuration in `BasePage` and reduced polling interval to 200 ms.
- Removed a duplicate table-load check in `RabbitHoleTest`.
- Reused the inherited wait in weightage entry instead of repeatedly creating new waits.
- Cached `data.properties` once and closed its input stream correctly.
- Fixed `LoginAccessTest` page-object initialization so page objects are created after the browser is initialized.
- Removed generated `target`, `.git`, and `allure-results` content from the source ZIP to keep the deliverable small. These are regenerated during a new build/run.

## Important
- The code has not been compiled or executed in this environment because Maven is not installed here.
- Run `mvn clean test` locally to validate against your application and environment.
- Wait durations are upper bounds, not fixed delays. If the application itself is slow (network/backend), Selenium cannot remove that underlying latency.
