package com.nutritrack.model;

public final class Grain extends Food {
  public Grain(long id, String name, String unit, double grams, Nutrients n) {
    super(id, name, unit, grams, n);
  }

  @Override
  public String getCategory() {
    return "Grain";
  }

  @Override
  public String getBenefit() {
    return "Source of energy";
  }
}
