package com.nutritrack.service;

import com.nutritrack.dao.*;
import com.nutritrack.exception.*;
import com.nutritrack.model.*;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

public final class NutritionService {
  private final MealDao meals;
  private final FoodCache cache;
  private final List<ScoreRule> rules =
      List.of(
          new CalorieRule(),
          new ProteinRule(),
          new VarietyRule(),
          new RegularityRule(),
          new FibreRule());

  public NutritionService(MealDao meals, FoodCache cache) {
    this.meals = meals;
    this.cache = cache;
  }

  public Food food(long id) throws FoodNotFoundException {
    return cache.snapshot().stream()
        .filter(f -> f.getId() == id)
        .findFirst()
        .orElseThrow(
            () -> new FoodNotFoundException("This food is unavailable. Choose another food."));
  }

  public FoodEntry entry(long id, double quantity, String type)
      throws FoodNotFoundException, InvalidQuantityException {
    if (!Double.isFinite(quantity) || quantity <= 0 || quantity > 100)
      throw new InvalidQuantityException(
          "Quantity must be greater than 0 and no more than 100 portions.");
    if (!List.of("Breakfast", "Lunch", "Dinner", "Snack").contains(type))
      throw new InvalidQuantityException("Choose a valid meal type.");
    return new FoodEntry(0, food(id), quantity, type);
  }

  public DailySummary summarize(User user, LocalDate day) throws SQLException, GoalNotSetException {
    GoalProfile g = user.getGoal();
    if (g == null || g.calories() <= 0 || g.protein() <= 0)
      throw new GoalNotSetException("Set positive calorie and protein goals first.");
    List<FoodEntry> entries = meals.forDay(user.getId(), day);
    Nutrients total =
        entries.stream().map(FoodEntry::total).reduce(Nutrients.zero(), Nutrients::plus);
    Map<String, Integer> points = new LinkedHashMap<>();
    int score = 0;
    for (ScoreRule rule : rules) {
      int p = rule.evaluate(total, g, entries);
      points.put(rule.name() + " / " + rule.maximum(), p);
      score += p;
    }
    return new DailySummary(day, List.copyOf(entries), total, score, points);
  }

  public TreeMap<LocalDate, DailySummary> week(User user) throws SQLException, GoalNotSetException {
    TreeMap<LocalDate, DailySummary> result = new TreeMap<>();
    for (int d = 6; d >= 0; d--) {
      LocalDate date = LocalDate.now().minusDays(d);
      result.put(date, summarize(user, date));
    }
    return result;
  }

  public Map<String, List<FoodEntry>> grouped(DailySummary s) {
    Map<String, List<FoodEntry>> groups = new LinkedHashMap<>();
    for (String type : List.of("Breakfast", "Lunch", "Dinner", "Snack"))
      groups.put(type, new ArrayList<>());
    for (FoodEntry e : s.entries()) groups.get(e.mealType()).add(e);
    return groups;
  }

  public record Gap(String name, double missing, double fraction) {
    public String getName() {
      return name;
    }

    public double getMissing() {
      return missing;
    }
  }

  public Gap gap(DailySummary s, GoalProfile g) {
    PriorityQueue<Gap> q =
        new PriorityQueue<>(Comparator.comparingDouble(Gap::fraction).reversed());
    q.add(
        new Gap(
            "Protein",
            Math.max(0, g.protein() - s.totals().protein()),
            Math.max(0, 1 - s.totals().protein() / g.protein())));
    q.add(
        new Gap(
            "Fibre",
            Math.max(0, g.fibre() - s.totals().fibre()),
            Math.max(0, 1 - s.totals().fibre() / g.fibre())));
    q.add(
        new Gap(
            "Carbs",
            Math.max(0, g.carbs() - s.totals().carbs()),
            Math.max(0, 1 - s.totals().carbs() / g.carbs())));
    return q.remove();
  }

  public List<Food> recommendations(Gap g) {
    return cache.snapshot().stream()
        .sorted(
            Comparator.comparingDouble((Food f) -> contribution(f, g.name()))
                .reversed()
                .thenComparing(Food::getName))
        .limit(3)
        .toList();
  }

  public static double contribution(Food f, String nutrient) {
    Nutrients n = f.portion(1);
    return switch (nutrient) {
      case "Protein" -> n.protein();
      case "Fibre" -> n.fibre();
      default -> n.carbs();
    };
  }
}
