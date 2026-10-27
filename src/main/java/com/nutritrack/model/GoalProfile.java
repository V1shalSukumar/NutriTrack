package com.nutritrack.model;

public record GoalProfile(double calories, double protein, double carbs, double fat, double fibre) {
  public static GoalProfile standard() {
    return new GoalProfile(2000, 60, 275, 50, 25);
  }

  public double getCalories() {
    return calories;
  }

  public double getProtein() {
    return protein;
  }

  public double getCarbs() {
    return carbs;
  }

  public double getFat() {
    return fat;
  }

  public double getFibre() {
    return fibre;
  }
}
