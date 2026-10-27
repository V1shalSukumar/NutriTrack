package com.nutritrack.model;

public final class Dairy extends Food {
  public Dairy(long id, String name, String unit, double grams, Nutrients n) {
    super(id, name, unit, grams, n);
  }

  @Override
  public String getCategory() {
    return "Dairy";
  }

  @Override
  public String getBenefit() {
    return "Source of protein and calcium";
  }
}
