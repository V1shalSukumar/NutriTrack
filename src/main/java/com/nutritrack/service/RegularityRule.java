package com.nutritrack.service;

import com.nutritrack.model.*;
import java.util.List;

public final class RegularityRule implements ScoreRule {
  public String name() {
    return "Regular meals";
  }

  public int maximum() {
    return 15;
  }

  public int evaluate(Nutrients t, GoalProfile g, List<FoodEntry> e) {
    return Math.min(15, (int) e.stream().map(FoodEntry::mealType).distinct().count() * 5);
  }
}
