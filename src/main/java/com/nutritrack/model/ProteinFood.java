package com.nutritrack.model;

public final class ProteinFood extends Food {
  public ProteinFood(long id, String name, String unit, double grams, Nutrients n) {
    super(id, name, unit, grams, n);
  }

  @Override
  public String getCategory() {
    return "Protein";
  }

  @Override
  public String getBenefit() {
    return "Source of protein";
  }
}
