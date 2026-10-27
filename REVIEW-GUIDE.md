# NutriTrack project-review guide

## Your opening explanation (about 30 seconds)

“NutriTrack is a layered Java nutrition application. Users log familiar Indian portions, such as two rotis or one katori of dal. The system converts portions into grams, calculates nutrients, compares intake with goals, identifies the largest relative gap and recommends three foods. The website uses Servlets and JSP, both interfaces use JDBC to share a MySQL database, and a JavaFX companion shares the same backend.”

## Preparation

1. Open http://localhost:8080. Start `run.ps1` if needed.
2. Keep the source project open in your IDE. The source map below identifies exact classes and line numbers.
3. Keep `sample-foods.csv` ready for the admin import demonstration.
4. Open another PowerShell terminal in the project directory for `run-javafx.ps1`.
5. Keep `HTTP-TEST-RESULTS.json`, `JAVAFX-TEST-RESULTS.txt` and the JUnit report available as evidence.

Demo user: `user@nutritrack.com` / `user123`.
Demo admin: `admin@nutritrack.com` / `admin123`.

## Suggested 8–10 minute live demonstration

### 1. Login and access control (one minute)

Enter the user email and a wrong password. Show “Email or password is incorrect.” Then log in correctly. Explain `InvalidLoginException`, the HTTP session, password hashing and `AuthFilter`. While logged in as a user, visit `/admin`; the filter returns HTTP 403.

### 2. Inheritance and polymorphism (one minute)

Open `Food.java`: it is abstract and owns private fields and shared `portion()` logic. Open `Fruit.java`, `Dairy.java` and `Grain.java`: each extends Food and overrides `getCategory()` and `getBenefit()`. Show that recommendations hold `Food` references but display subclass-specific benefits. Open `Admin.java` for the separate User/Admin hierarchy.

Say: “Inheritance shares state and behavior. Polymorphism chooses an overridden implementation at runtime. An abstract class can contain both implemented and abstract methods, but cannot be instantiated directly.”

### 3. Portion calculation and a JDBC transaction (two minutes)

Open **Log meal**, select **Lunch**, search `dal`, choose **Toor Dal**, and enter **2**. Point to the live conversion: `2 × 150 g = 300 g`, `348 kcal`, `20.4 g protein`. Click **Add to meal**. The draft table and total row update. Add another food if desired, then **Save meal**.

Trace the save: `PortalServlet.doPost()` constructs a `Meal` → `MealDao.save()` sets auto-commit false → a PreparedStatement batch inserts FoodEntry rows → commit. On a database failure, rollback prevents a partial meal. The test `mealTransactionRollsBackEveryRowOnFailure` proves this by combining a valid food with an invalid foreign key.

### 4. Collections and score rules (one minute)

Open **Daily**: a LinkedHashMap groups meals in a stable display order. Open **Weekly**: a TreeMap sorts dates. Open **Dashboard**: a PriorityQueue ranks normalized nutrient shortfalls, and a sorted stream returns the top three foods. The five score strategies are stored in `List<ScoreRule>` and invoked through the same interface.

### 5. User-defined exception (30 seconds)

The browser blocks zero or negative quantities before sending them. To prove server validation, use the included HTTP test: it posts quantity `0` directly and checks the custom exception message. Alternatively remove the number input’s `min` attribute in browser developer tools during the review and submit `0`. `NutritionService.entry()` throws `InvalidQuantityException`; the servlet catches it and returns a recoverable message.

Other custom exceptions: `FoodNotFoundException`, `GoalNotSetException`, `InvalidLoginException`, `CsvImportException`. Explain `throw` (raise an exception), `throws` (declare it), and `catch` (handle it).

### 6. File handling, admin and multithreading (two minutes)

Download the weekly CSV from **Weekly** and open the file. It has one header plus seven daily rows. Log out and sign in as admin. Upload `sample-foods.csv`; Rajma and Orange appear in the catalogue. Re-importing updates matching names instead of adding duplicates.

Click **Run report job**, then **Refresh status**. Point to the completed timestamp, `nutritrack-report-worker` name, run count and cache revision. The worker uses `ScheduledExecutorService`; the website stays responsive while it runs. Open `BackgroundJobs.java`, then `FoodCache.java` to show synchronization.

Say: “The scheduled worker and HTTP threads can access the cache concurrently. synchronized protects refresh and snapshot access. Immutable lists avoid callers changing cached data. volatile status and AtomicLong provide safe cross-thread visibility. The executor shuts down when the web app stops.”

The automatic delay is 60 seconds for an observable classroom demo. The report covers seven days; the scheduler is not configured to wait seven days.

### 7. JavaFX (one minute)

Run `run-javafx.ps1`. Sign in with the demo user and show its native JavaFX dashboard, meal logging, custom food/photo dialog, daily and weekly views, and admin tools. Open `ReviewApp.java`: it builds the Stage, Scene, sidebar, forms, tables and chart. JavaFX Tasks run database startup and login outside the UI thread; the services and JDBC DAOs are shared with the web application.

The JavaFX interface is built from native controls. It calls the same Java services and JDBC DAOs as the Servlet/JSP website, so the two interfaces share nutrition calculations, validation and database persistence.

## Exact concept-to-source map

Line references describe this delivered version. IDE “Go to line” works even if your Markdown viewer does not navigate source anchors.

| Concept | Source | Explain this |
|---|---|---|
| Encapsulation | [model/Food.java:5](src/main/java/com/nutritrack/model/Food.java#L5) | Private fields plus getters keep Food state immutable; callers cannot replace nutrient values. |
| Abstract class | [model/Food.java:4](src/main/java/com/nutritrack/model/Food.java#L4) | Food supplies shared data and portion() logic; abstract methods require category-specific behavior. |
| Inheritance | [model/Fruit.java:3](src/main/java/com/nutritrack/model/Fruit.java#L3) | Fruit, Dairy and Grain inherit Food. Admin separately extends User. |
| Runtime polymorphism | [service/NutritionService.java:53](src/main/java/com/nutritrack/service/NutritionService.java#L53) | A ScoreRule reference invokes the actual CalorieRule, ProteinRule, VarietyRule, RegularityRule or FibreRule implementation. |
| Interface | [service/ScoreRule.java:6](src/main/java/com/nutritrack/service/ScoreRule.java#L6) | Every scoring strategy promises name(), maximum() and evaluate(). Trackable is a second interface. |
| Composition | [model/Meal.java:7](src/main/java/com/nutritrack/model/Meal.java#L7) | A Meal has many FoodEntry objects, and each FoodEntry refers to a Food. |
| Packages/layers | [web/PortalServlet.java:13](src/main/java/com/nutritrack/web/PortalServlet.java#L13) | model, dao, service, exception, web, util, thread and desktop organize distinct responsibilities. |
| User-defined exception | [exception/InvalidQuantityException.java:3](src/main/java/com/nutritrack/exception/InvalidQuantityException.java#L3) | Checked exception thrown when a portion quantity is zero, negative, nonfinite or over 100. |
| Exception handling | [web/PortalServlet.java:333](src/main/java/com/nutritrack/web/PortalServlet.java#L333) | The servlet catches expected domain errors and redirects with an escaped, readable message. |
| ArrayList | [model/Meal.java:7](src/main/java/com/nutritrack/model/Meal.java#L7) | Stores a meal’s ordered food entries. The web draft also uses ArrayList. |
| LinkedHashMap | [service/NutritionService.java:70](src/main/java/com/nutritrack/service/NutritionService.java#L70) | Preserves breakfast/lunch/dinner/snack ordering and score-rule display order. |
| TreeMap | [service/NutritionService.java:61](src/main/java/com/nutritrack/service/NutritionService.java#L61) | Stores weekly summaries sorted by LocalDate. |
| PriorityQueue | [service/NutritionService.java:89](src/main/java/com/nutritrack/service/NutritionService.java#L89) | Largest percentage shortfall is at the head of the queue. |
| HashSet | [service/CsvService.java:22](src/main/java/com/nutritrack/service/CsvService.java#L22) | Detects duplicate case-insensitive names in an import. |
| JDBC and transactions | [dao/MealDao.java:53](src/main/java/com/nutritrack/dao/MealDao.java#L53) | PreparedStatement batches all foods in a meal; commit succeeds once or rollback removes the entire batch. |
| JDBC parameter binding | [dao/UserDao.java:29](src/main/java/com/nutritrack/dao/UserDao.java#L29) | User input is bound as a parameter rather than concatenated into SQL. |
| Servlet | [web/PortalServlet.java:153](src/main/java/com/nutritrack/web/PortalServlet.java#L153) | Handles login and form submissions; GET prepares model attributes and forwards to JSP. |
| Filter and sessions | [web/AuthFilter.java:10](src/main/java/com/nutritrack/web/AuthFilter.java#L10) | Guards anonymous requests, administrator routes and CSRF tokens. Session stores the logged-in User. |
| File handling: read | [service/CsvService.java:23](src/main/java/com/nutritrack/service/CsvService.java#L23) | Reads uploaded CSV through a Reader and validates every row before a transactional batch. |
| File handling: write | [util/ReportWriter.java:17](src/main/java/com/nutritrack/util/ReportWriter.java#L17) | Writes UTF-8 CSV to a temporary file, then replaces the named weekly report. |
| Multithreading | [thread/BackgroundJobs.java:11](src/main/java/com/nutritrack/thread/BackgroundJobs.java#L11) | Named worker thread runs cache refresh and report generation separately from HTTP request threads. |
| Synchronization | [service/FoodCache.java:18](src/main/java/com/nutritrack/service/FoodCache.java#L18) | Mutual exclusion protects publication of the immutable food list and revision counter. |
| Thread visibility | [thread/BackgroundJobs.java:21](src/main/java/com/nutritrack/thread/BackgroundJobs.java#L21) | volatile publishes status to request threads; AtomicLong tracks completed runs safely. |
| JavaFX | [desktop/ReviewApp.java:53](src/main/java/com/nutritrack/desktop/ReviewApp.java#L53) | Native Stage, Scene, sidebar, forms, tables, chart and event handlers implement the desktop screens. |
| JavaFX background Task | [desktop/ReviewApp.java:59](src/main/java/com/nutritrack/desktop/ReviewApp.java#L59) | Database startup and login run off the JavaFX Application Thread; task handlers update the UI. |

JSP/JSTL evidence: `src/main/webapp/WEB-INF/views/daily.jsp` uses `c:forEach`, `c:if`, `c:out` and `fmt:formatNumber` to render server-prepared data. `WEB-INF/web.xml` registers the listener, filter, servlet mappings, upload limits and UTF-8 JSP encoding. JSPs live under WEB-INF, so they cannot be requested directly.

## Calculation rules you can defend

For each nutrient:

```text
grams = quantity × unitGrams
nutrient = grams / 100 × nutrientPer100g
```

For Toor Dal: `2 × 150 = 300 g`; `300/100 × 116 = 348 kcal`; `300/100 × 6.8 = 20.4 g protein`.

Daily goals are 2000 kcal, 60 g protein, 275 g carbohydrate, 50 g fat and 25 g fibre. These are demonstration targets, not personalized clinical advice.

| Score rule | Formula | Maximum |
|---|---|---:|
| Calories | round(max(0, 30 − 50 × absolute relative deviation from calorie goal)) | 30 |
| Protein | round(25 × min(1, protein / protein goal)) | 25 |
| Variety | min(20, number of distinct food categories × 5) | 20 |
| Regularity | min(15, number of distinct meal types × 5) | 15 |
| Fibre | round(10 × min(1, fibre / fibre goal)) | 10 |

The initial day's points are 16 + 15 + 15 + 15 + 7 = **68**. These explicit implementation rules reproduce the report’s sample; the PDF did not fully specify each scoring equation. Score bands: 85+ Excellent, 70–84 Good, 50–69 Fair, below 50 Getting started.

Gap detection compares protein, fibre and carbohydrate by **relative** deficit: `max(0, 1 − intake/goal)`. This allows nutrient goals of different sizes to be compared. Suggestions rank the contribution of **one actual food portion**, with the name as a deterministic tie-breaker. The PDF’s suggestion examples mix portion counts, so the implementation’s top three can differ while using the same UI.

## Architecture and persistence

```text
Browser → AuthFilter → PortalServlet → shared services and DAOs → MySQL
                            ↓
                      JSP + JSTL view

JavaFX controls → shared services and DAOs → MySQL
Browser → AuthFilter → PortalServlet → shared services and DAOs → MySQL
Scheduled worker → cache refresh + weekly report files
```

`users`: identity, hash, role, calorie/protein goals, active status.
`food`: per-100-g nutrients, category, portion unit, grams per unit, active status.
`meal_log`: user/food foreign keys, date, meal type, quantity.

MySQL database: `nutritrack` on the configured server. Set the connection in `database.properties` or with the `NUTRITRACK_DB_*` environment variables. Reports: `data/reports/weekly-user-<id>.csv`. The application seeds the first seven demo dates only once and persists changes across restart. Meals are references to catalogue foods; editing food nutrition changes calculations for its historical entries. Archiving hides a food from new selections while preserving history. Query `users`, `food` and `meal_log` in MySQL Workbench to verify entries from either interface.

## Likely viva questions

**Why an abstract Food and an interface ScoreRule?** Food shares actual state, construction and conversion implementation across related subclasses. ScoreRule defines a contract for independently replaceable algorithms.

**Where is dynamic method dispatch?** `rule.evaluate(...)` runs the concrete rule implementation; `food.getBenefit()` runs the concrete Food subclass’s method. The reference type alone does not determine the invoked override.

**Why PreparedStatement?** It separates SQL structure from parameter values and supports reusable statement execution and batching. It avoids SQL injection caused by string concatenation.

**Why a transaction?** Saving a meal can insert several rows. The rows must succeed together; rollback prevents only part of a meal being recorded.

**ArrayList versus Map versus TreeMap versus PriorityQueue?** ArrayList stores ordered items; Map associates meal types with entries; TreeMap maintains sorted dates; PriorityQueue efficiently exposes the highest-priority gap.

**What makes the cache thread-safe?** synchronized methods protect reading and replacing the list reference and revision; the published list is immutable. Each DAO method opens its own JDBC connection instead of sharing a Connection across threads.

**Why use a JavaFX Task?** Database startup and login run off the GUI thread, keeping the interface responsive. Task callbacks safely update JavaFX controls on the application thread.

**What is a session?** Server-side state associated with a browser session identifier. Here it stores User, CSRF token and an unsaved meal draft. Login rotates the identifier; logout invalidates the session.

**What is JSTL?** Standard tag libraries used for loops, conditional content, escaped output and numeric formatting in JSP, avoiding Java scriptlets in the view.

**Is this a production nutrition system?** No. It is a functioning local course project with illustrative food data and scoring. Default demo credentials, fixed goals, editable historical food references and a 60-second job schedule are documented demo choices.

## Finish the review

Use this document for the concept checklist and **Administration** for background-thread status. Show the current JavaFX screens and the saved backend test reports. The JavaFX smoke result in `JAVAFX-TEST-RESULTS.txt` documents the earlier table version. Close with: “Every required concept is exercised by a working feature, rather than only appearing in an unused class.”

## Custom food extension

CustomFood extends the abstract Food class. CustomFoodService validates manually entered totals. FoodPhotos decodes and normalizes optional images. MealDao stores the custom nutrition snapshot and photo in the same transaction as the meal; the photo endpoint binds entry ID and user ID to enforce ownership. The Project review web page was removed at the user’s request.
