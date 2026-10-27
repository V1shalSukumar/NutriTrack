package com.nutritrack.model;

public record Nutrients(double calories, double protein, double carbs, double fat, double fibre) {
  public Nutrients scale(double factor) {
    return new Nutrients(
        calories * factor, protein * factor, carbs * factor, fat * factor, fibre * factor);
  }

  public Nutrients plus(Nutrients n) {
    return new Nutrients(
        calories + n.calories, protein + n.protein, carbs + n.carbs, fat + n.fat, fibre + n.fibre);
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

  public static Nutrients zero() {
    return new Nutrients(0, 0, 0, 0, 0);
  }
}
