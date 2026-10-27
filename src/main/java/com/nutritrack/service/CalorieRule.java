package com.nutritrack.service;

import com.nutritrack.model.*;
import java.util.List;

public final class CalorieRule implements ScoreRule {
  public String name() {
    return "Calories near goal";
  }

  public int maximum() {
    return 30;
  }

  public int evaluate(Nutrients t, GoalProfile g, List<FoodEntry> e) {
    // Deduct 0.5 points per percentage point away from the calorie goal.
    return (int)
        Math.round(Math.max(0, 30 - 50 * Math.abs(t.calories() - g.calories()) / g.calories()));
  }
}
