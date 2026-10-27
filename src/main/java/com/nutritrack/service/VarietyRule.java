package com.nutritrack.service;

import com.nutritrack.model.*;
import java.util.List;

public final class VarietyRule implements ScoreRule {
  public String name() {
    return "Food variety";
  }

  public int maximum() {
    return 20;
  }

  public int evaluate(Nutrients t, GoalProfile g, List<FoodEntry> e) {
    return Math.min(20, (int) e.stream().map(x -> x.food().getCategory()).distinct().count() * 5);
  }
}
