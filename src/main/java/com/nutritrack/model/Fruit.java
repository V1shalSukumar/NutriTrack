package com.nutritrack.model;

public final class Fruit extends Food {
  public Fruit(long id, String name, String unit, double grams, Nutrients n) {
    super(id, name, unit, grams, n);
  }

  @Override
  public String getCategory() {
    return "Fruit";
  }

  @Override
  public String getBenefit() {
    return "Source of fibre";
  }
}
