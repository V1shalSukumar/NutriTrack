package com.nutritrack.model;

// Abstract parent: shared state is private; subclasses supply category behavior.
public abstract class Food {
  private final long id;
  private final String name, unit;
  private final double unitGrams;
  private final Nutrients nutrients;

  protected Food(long id, String name, String unit, double grams, Nutrients n) {
    this.id = id;
    this.name = name;
    this.unit = unit;
    unitGrams = grams;
    nutrients = n;
  }

  public abstract String getCategory();

  public abstract String getBenefit();

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getUnit() {
    return unit;
  }

  public double getUnitGrams() {
    return unitGrams;
  }

  public Nutrients getNutrients() {
    return nutrients;
  }

  public Nutrients portion(double quantity) {
    return nutrients.scale(quantity * unitGrams / 100);
  }

  @Override
  public String toString() {
    return name + " (" + unit + ")";
  }
}
