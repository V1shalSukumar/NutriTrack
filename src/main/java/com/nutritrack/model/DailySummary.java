package com.nutritrack.model;

import java.time.LocalDate;
import java.util.*;

public record DailySummary(
    LocalDate date,
    List<FoodEntry> entries,
    Nutrients totals,
    int score,
    Map<String, Integer> points) {
  public LocalDate getDate() {
    return date;
  }

  public List<FoodEntry> getEntries() {
    return entries;
  }

  public Nutrients getTotals() {
    return totals;
  }

  public int getScore() {
    return score;
  }

  public Map<String, Integer> getPoints() {
    return points;
  }

  public String getRating() {
    return score >= 85
        ? "Excellent"
        : score >= 70 ? "Good" : score >= 50 ? "Fair" : "Getting started";
  }
}
