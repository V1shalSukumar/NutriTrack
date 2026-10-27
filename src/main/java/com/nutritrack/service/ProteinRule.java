package com.nutritrack.service;

import com.nutritrack.model.*;
import java.util.List;

public final class ProteinRule implements ScoreRule {
  public String name() {
    return "Protein goal";
  }

  public int maximum() {
    return 25;
  }

  public int evaluate(Nutrients t, GoalProfile g, List<FoodEntry> e) {
    return (int) Math.round(25 * Math.min(1, t.protein() / g.protein()));
  }
}
