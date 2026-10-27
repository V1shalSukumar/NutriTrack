# Verification record

Historical checks on 22 October 2026 used Java 17, embedded Tomcat 9, Windows JavaFX and an H2 file database. Those results predate the PostgreSQL and MySQL migrations and do not verify MySQL connectivity.

| Check | Result | Evidence |
|---|---|---|
| Maven compilation and WAR packaging | Passed | `target/nutritrack.war` |
| JUnit backend tests | 12 passed, 0 failures/errors | `target/surefire-reports/com.nutritrack.NutritionTest.txt` |
| HTTP integration tests | 19 passed | `HTTP-TEST-RESULTS.json` |
| Native JavaFX desktop screens | Maven compilation passed after the native UI rewrite; interactive workflows have not yet been rechecked | `src/main/java/com/nutritrack/desktop/ReviewApp.java` |
| Live portion conversion | 2 katori dal → 300 g, 348 kcal, 20.4 g protein | Browser meal form |
| Initial dashboard score | 68, with 16/15/15/15/7 rule points | `screenshots/desktop.png` |
| Persistence after application restarts | Seeded meals and catalogue retained | Repeated live logins and daily views |
| Background worker | Named worker, completed timestamp and increasing run count visible | `screenshots/admin.png` |
| Desktop UI and phone dashboard | Required visual fixes resolved | `screenshots/` and independent screenshot review |

The screenshot review's final disposition was **ship within the reviewed scope**: mobile gutters, draft total row and daily subtotals were resolved. It was a screenshot/source review, not a complete accessibility audit. The automated design detector was unavailable because its helper engine was not installed.

The HTTP checks cover anonymous redirect, missing-CSRF rejection, incorrect login, correct user/admin login, admin authorization, all main JSP routes, CSS serving, server quantity validation, meal transaction submission, own-entry deletion, CSV report download, multipart import, food archival, background-job submission and logout.

JUnit covers the dal formula, invalid finite/nonfinite quantities, password/role rejection, polymorphic rules, exact initial score, rollback on a foreign-key error, cross-user deletion protection, import validation before writes, quoted CSV names, seven-day report shape, relative-gap ordering, immutable cache snapshots and the missing-food/missing-goal exceptions.

This delivery runs locally. No public hosting deployment was performed. The smoke result in `JAVAFX-TEST-RESULTS.txt` is historical evidence for the earlier native JavaFX table. The current native screen rewrite compiled successfully; its interactive workflows need a manual review. Run `run-javafx.ps1` to open it.

## Custom entry update

17 JUnit tests pass (12 existing + 5 custom food/photo tests). An additional 14 HTTP checks pass for custom nutrition, optional photos, validation, ownership, persistence and removed review route. See CUSTOM-ENTRY-TEST-RESULTS.json. Existing user meals were preserved.
