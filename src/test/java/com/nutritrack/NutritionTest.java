package com.nutritrack;

import static org.junit.jupiter.api.Assertions.*;

import com.nutritrack.dao.*;
import com.nutritrack.exception.*;
import com.nutritrack.model.*;
import com.nutritrack.service.*;
import com.nutritrack.util.ReportWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "NUTRITRACK_TEST_DB_URL", matches = "jdbc:mysql:.+")
class NutritionTest {
  AppServices app;
  User user;
  MysqlTestDatabase database;

  @BeforeEach
  void setup() throws Exception {
    database = MysqlTestDatabase.create();
    app = new AppServices(database.database());
    user = app.users.login("user@nutritrack.com", "user123", "USER");
  }

  @AfterEach
  void close() {
    app.close();
    try {
      database.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void dalPortionMatchesReport() throws Exception {
    Food dal =
        app.cache.snapshot().stream()
            .filter(f -> f.getName().equals("Toor Dal"))
            .findFirst()
            .orElseThrow();
    assertEquals(348, dal.portion(2).calories(), 0.001);
    assertEquals(20.4, dal.portion(2).protein(), 0.001);
  }

  @Test
  void quantityValidationRejectsZeroNegativeNanAndInfinity() {
    for (double q : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY, 101})
      assertThrows(InvalidQuantityException.class, () -> app.nutrition.entry(1, q, "Lunch"));
  }

  @Test
  void invalidLoginAndRoleAreRejected() {
    assertThrows(
        InvalidLoginException.class, () -> app.users.login("user@nutritrack.com", "wrong", "USER"));
    assertThrows(
        InvalidLoginException.class,
        () -> app.users.login("user@nutritrack.com", "user123", "ADMIN"));
  }

  @Test
  void subclassesAndRulesUseDynamicDispatch() throws Exception {
    Food fruit = new Fruit(0, "Apple", "piece", 100, new Nutrients(50, 1, 12, 0, 2));
    Food dairy = new Dairy(0, "Curd", "katori", 150, new Nutrients(60, 3, 4, 3, 0));
    assertNotEquals(fruit.getBenefit(), dairy.getBenefit());
    var summary = app.nutrition.summarize(user, LocalDate.now());
    assertEquals(5, summary.points().size());
    assertEquals(68, summary.score(), "Seed day matches the report's 68-point example");
    assertEquals(
        summary.score(), summary.points().values().stream().mapToInt(Integer::intValue).sum());
    assertTrue(summary.score() >= 0 && summary.score() <= 100);
  }

  @Test
  void mealTransactionRollsBackEveryRowOnFailure() throws Exception {
    int before = app.meals.forDay(user.getId(), LocalDate.now()).size();
    Meal meal = new Meal();
    meal.add(app.nutrition.entry(1, 1, "Lunch"));
    meal.add(
        new FoodEntry(0, new Fruit(999999, "Missing", "piece", 100, Nutrients.zero()), 1, "Lunch"));
    assertThrows(
        java.sql.SQLException.class, () -> app.meals.save(user.getId(), LocalDate.now(), meal));
    assertEquals(before, app.meals.forDay(user.getId(), LocalDate.now()).size());
  }

  @Test
  void deleteCannotAffectOtherUser() throws Exception {
    var entry = app.meals.forDay(user.getId(), LocalDate.now()).get(0);
    app.meals.delete(entry.id(), 2);
    assertTrue(
        app.meals.forDay(user.getId(), LocalDate.now()).stream()
            .anyMatch(e -> e.id() == entry.id()));
  }

  @Test
  void csvIsValidatedBeforeAnyWrite() throws Exception {
    int before = app.foods.all().size();
    String csv =
        "name,category,calories,protein,carbs,fat,fibre,unit,unit_grams\n"
            + "Valid,Fruit,50,1,10,1,1,piece,100\n"
            + "Invalid,Fruit,NaN,1,10,1,1,piece,100\n";
    assertThrows(CsvImportException.class, () -> app.csv.importFoods(new StringReader(csv)));
    assertEquals(before, app.foods.all().size());
  }

  @Test
  void csvImportSupportsQuotedNames() throws Exception {
    String csv =
        "name,category,calories,protein,carbs,fat,fibre,unit,unit_grams\n"
            + "\"Apple, fresh\",Fruit,50,1,10,1,1,piece,100\n";
    assertEquals(1, app.csv.importFoods(new StringReader(csv)));
    assertTrue(app.cache.snapshot().stream().anyMatch(f -> f.getName().equals("Apple, fresh")));
  }

  @Test
  void orderedWeekAndReportFileHaveSevenDays() throws Exception {
    var week = app.nutrition.week(user);
    assertEquals(7, week.size());
    assertEquals(LocalDate.now().minusDays(6), week.firstKey());
    var file = new ReportWriter().write(user, week);
    assertEquals(8, Files.readAllLines(file).size());
  }

  @Test
  void gapsUseRelativeShortfallAndRecommendationsAreOrdered() throws Exception {
    var summary =
        new DailySummary(
            LocalDate.now(), List.of(), new Nutrients(1500, 30, 250, 40, 27), 0, Map.of());
    var gap = app.nutrition.gap(summary, GoalProfile.standard());
    assertEquals("Protein", gap.name());
    assertEquals(30, gap.missing());
    var picks = app.nutrition.recommendations(gap);
    assertEquals(3, picks.size());
    assertTrue(
        NutritionService.contribution(picks.get(0), "Protein")
            >= NutritionService.contribution(picks.get(1), "Protein"));
  }

  @Test
  void cacheSnapshotsCannotBeMutated() {
    assertThrows(UnsupportedOperationException.class, () -> app.cache.snapshot().clear());
  }

  @Test
  void missingFoodAndGoalHaveSpecificExceptions() {
    assertThrows(FoodNotFoundException.class, () -> app.nutrition.food(99999));
    assertThrows(
        GoalNotSetException.class,
        () -> app.nutrition.summarize(new User(1, "x", "x", null), LocalDate.now()));
  }
}
