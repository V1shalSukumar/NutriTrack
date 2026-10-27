package com.nutritrack.service;

import com.nutritrack.model.*;
import java.util.List;

public final class FibreRule implements ScoreRule {
  public String name() {
    return "Fibre";
  }

  public int maximum() {
    return 10;
  }

  public int evaluate(Nutrients t, GoalProfile g, List<FoodEntry> e) {
    return (int) Math.round(10 * Math.min(1, t.fibre() / g.fibre()));
  }
}
