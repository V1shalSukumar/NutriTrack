package com.nutritrack.service;

import com.nutritrack.model.*;
import java.util.List;

public interface ScoreRule {
  String name();

  int maximum();

  int evaluate(Nutrients total, GoalProfile goal, List<FoodEntry> entries);
}
