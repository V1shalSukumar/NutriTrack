package com.nutritrack.model;

public final class PreparedFood extends Food {
  public PreparedFood(long id, String name, String unit, double grams, Nutrients n) {
    super(id, name, unit, grams, n);
  }

  @Override
  public String getCategory() {
    return "Prepared";
  }

  @Override
  public String getBenefit() {
    return "Everyday Indian meal";
  }
}
