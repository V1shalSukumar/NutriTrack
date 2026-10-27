# NutriTrack

A Java 17 academic project implementing the supplied NutriTrack screens with Servlet/JSP pages, MySQL persistence through JDBC, Indian food portions, nutrition insights, admin tools, CSV files, scheduled jobs and a JavaFX companion.

## Open the running website

http://localhost:8080

| Role | Email | Password |
|---|---|---|
| User | user@nutritrack.com | user123 |
| Admin | admin@nutritrack.com | admin123 |

These are local demonstration accounts. Passwords are stored using salted PBKDF2 hashes.

## Run on this computer

NutriTrack uses one MySQL database for both the website and JavaFX app. Install/start MySQL Server 8.0 or newer and MySQL Workbench. In Workbench, open a connection and run `CREATE DATABASE nutritrack CHARACTER SET utf8mb4;`. From the NutriTrack project folder, copy `database.properties.example` to `database.properties` and enter your MySQL login password. Keep that file local; it is ignored by Git. On first startup, NutriTrack creates its tables and seeds the demo accounts, catalogue foods and sample meals if the database is empty.

The example connection uses `jdbc:mysql://localhost:3306/nutritrack` and user `root`. Environment variables `NUTRITRACK_DB_URL`, `NUTRITRACK_DB_USER` and `NUTRITRACK_DB_PASSWORD` override the local properties file. MySQL Connector/J is included through Maven.

In MySQL Workbench, connect to the same server, choose the `nutritrack` schema, open a SQL tab, paste a query below, and click the lightning-bolt Execute button. The web app and JavaFX app must use the same `database.properties` values and server. For example:

```sql
SELECT id, name, email, role FROM users ORDER BY id;
SELECT id, name, category, calories, protein FROM food ORDER BY name;
SELECT m.id, u.email, m.log_date, m.meal_type,
       COALESCE(m.custom_name, f.name) AS food_name,
       m.quantity, m.custom_calories, (m.photo IS NOT NULL) AS has_photo
FROM meal_log AS m
JOIN users AS u ON u.id = m.user_id
LEFT JOIN food AS f ON f.id = m.food_id
ORDER BY m.log_date DESC, m.id DESC;
```

Catalogue meals refer to `food_id`; manually entered meals use `custom_name` and their nutrition totals are stored in the `custom_*` columns. Both front ends use this same MySQL database, so a meal saved in either one appears in Workbench.

In PowerShell, open this project directory and run:

```powershell
.\run.ps1
```

The supplied scripts find the portable JDK and Maven in the task workspace. If the website is already running, open its URL instead of starting another copy. Use `./run.ps1 -Port 8081` for a second port. Press Ctrl+C to stop a foreground server.

For the native JavaFX demonstration:

```powershell
.\run-javafx.ps1
```

The JavaFX desktop application has its own native screens for login, dashboard, meal logging, daily and weekly history, and administration. It uses the same Java services and MySQL database as the website, including custom food photos, CSV import/export and background reports. You can run both interfaces at once; both connect to MySQL.

To use Eclipse, import this folder with **File → Import → Maven → Existing Maven Projects** and select the folder containing this `pom.xml`. Keep `database.properties` beside `pom.xml`. Eclipse's Maven build should use JDK 17 or newer; run **Maven → Update Project** after import so Eclipse downloads MySQL Connector/J. For the website, run the `com.nutritrack.Launcher` main class after building dependencies, or use `run.ps1` from the project folder. For desktop, run `com.nutritrack.desktop.ReviewApp` with the JavaFX Maven configuration or use `run-javafx.ps1`.

## Build on another computer

Install JDK 17+ and Maven 3.9+, set `JAVA_HOME`, then:

```text
mvn test package dependency:copy-dependencies -DoutputDirectory=target/lib
java -cp "target/classes;target/lib/*" com.nutritrack.Launcher
```

On Linux/macOS, use `:` instead of `;` in the classpath. Run commands from the project root. To run JavaFX with Maven: `mvn javafx:run`. Rebuild dependencies on the destination OS; the prebuilt JavaFX native jars in this delivery are for Windows.

The standard deployable archive is `target/nutritrack.war`. Deploy it to Tomcat 9 on Java 17; its URL is normally `/nutritrack`. Do not deploy this `javax.servlet` project to a Jakarta-only Tomcat 10/11 container without migration. The embedded launcher binds only to 127.0.0.1.

## Features

- Login, logout, sessions, user/admin role enforcement and CSRF tokens.
- Dashboard with calorie/protein/carbohydrate/fat progress, five score rules, largest relative nutrient gap and top three suggestions.
- Search food, convert Indian portions live, build a draft and save all items in one JDBC transaction.
- Daily grouped meals and subtotals, deletion of your own entries, dated history.
- Seven-day chart and CSV report download.
- Admin food add/update/archive, user deactivation, transactional CSV import and background job trigger/status.
- Native JavaFX desktop application sharing services and the MySQL database with the Servlet/JSP website.
- Manual food entry with total nutrition and optional JPG/PNG photo, private to the user.

## Project structure

```text
src/main/java/com/nutritrack/
  model/       Abstract Food, subclasses, User/Admin, Meal and values
  dao/         Database schema/seed and JDBC queries
  service/     Nutrition, ScoreRule strategies, cache, CSV import
  exception/   Five user-defined checked exceptions
  web/         Servlet, authentication/CSRF filter, lifecycle listener
  util/        Password hashing and CSV report writer
  thread/      Scheduled background reports and cache refresh
  desktop/     JavaFX companion
src/main/webapp/WEB-INF/views/   JSP/JSTL pages
src/main/webapp/assets/          CSS and live conversion JavaScript
src/test/java/                  JUnit tests
sample-foods.csv                Import-ready example
REVIEW-GUIDE.md                 Review presentation and source map
```

## Persistence and background work

MySQL stores accounts, foods and meals in the `nutritrack` database. The application seeds the demo accounts, 14 foods and seven days of meals only when the database is initially empty. Today uses the report's seven food entries. Dates are relative to first launch; the app does not invent new meals on later days. Existing H2 or PostgreSQL data is not automatically migrated; the app no longer reads or writes either database.

Reports are written to `data/reports/weekly-user-<id>.csv`. A named `nutritrack-report-worker` starts after five seconds and repeats every 60 seconds so its behavior is visible during a short review. This is a demonstration schedule, not a weekly production schedule. It produces a seven-day report each run. A synchronized cache publishes immutable food lists; a volatile status and AtomicLong make thread progress visible.

Food changes apply to calculations for existing meal references, too. Food archival retains historical entries. Deactivation prevents new user logins; it does not forcibly terminate existing sessions. These behaviors are deliberate boundaries of this local academic implementation.

## CSV import

Log in as admin, choose `sample-foods.csv`, then select **Import CSV**. Required header:

```csv
name,category,calories,protein,carbs,fat,fibre,unit,unit_grams
```

Categories: `Fruit`, `Dairy`, `Grain`, `Protein`, `Prepared`. Nutrition numbers are per 100 g. Numbers must be finite and between 0 and 10,000; unit grams must be positive. At most 1 MB and 1,000 rows. Matching food names are updated; new names are inserted. Invalid rows reject the entire import before writing. Duplicate names in one file are rejected. Quoted commas are supported.

## Verification

```powershell
.\test.ps1
python .\http-smoke-test.py
```

The HTTP test needs the website running, uses the demo accounts, temporarily adds/removes a meal and imports/archives a test food. Run it only against this local demo. Read `HTTP-TEST-RESULTS.json`, `JAVAFX-TEST-RESULTS.txt`, and `target/surefire-reports` for the recorded results.

MySQL integration tests run when `NUTRITRACK_TEST_DB_URL`, `NUTRITRACK_TEST_DB_USER`, and `NUTRITRACK_TEST_DB_PASSWORD` point to a server account that can create/drop databases. Use a server URL without a database name, such as `jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`. Without these test-only settings, those integration tests are skipped; the application itself uses `NUTRITRACK_DB_*` or `database.properties`.

## Reference fidelity

The five original screens retain the reference's grey sidebar, green actions, white bordered panels, nutrient cards, grouped tables and weekly bars. The initial day rounds to 1,423 kcal, 35 g protein, 249 g carbs and 31 g fat. The five explicit rules produce 68/100. Recommendations rank one actual portion consistently; therefore they may differ from the PDF, which mixes one katori, four eggs and four paneer pieces. Weekly demo values are generated from saved meal quantities rather than hard-coded chart labels.

The JavaFX desktop app presents the nutrition and administration workflows with native JavaFX controls. It calls the same Java services and JDBC DAOs as the website. Nutrition values and the health score are illustrative project logic, not clinical recommendations.

Technology references: [Tomcat 9 documentation](https://tomcat.apache.org/tomcat-9.0-doc/index.html), [OpenJFX documentation](https://openjfx.io/openjfx-docs/).

## Manual food entries and photos

Open **Log meal → Can’t find your food? Add a custom item**. Enter the name, meal type, quantity, total grams, calories, protein, carbs, fat and fiber. All nutrition fields describe the **whole amount consumed**, not per piece or per 100 g. Quantity does not multiply those totals. Add the item to the draft, then choose **Save meal**.

Optional photos accept JPG/PNG up to 5 MB, 12 megapixels and 6,000 pixels per side. Uploaded images are decoded, normalized to PNG and stored in MySQL as `MEDIUMBLOB` with the meal entry. Thumbnails appear in the draft and daily log; click one to view the photo. Photo access requires the owning user’s session. Custom entries do not change the shared catalogue. Database columns are added automatically on startup without resetting existing meals.

The Project review web page and navigation link have been removed. The separate REVIEW-GUIDE.md remains available as project documentation.
