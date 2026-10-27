package com.nutritrack.model;

/** User-entered nutrition for a particular meal, never added to the shared catalogue. */
public final class CustomFood extends Food {
  private final Nutrients consumed;

  public CustomFood(String name, double quantity, double grams, Nutrients consumed) {
    super(0, name, "portion", grams / quantity, consumed.scale(100 / grams));
    this.consumed = consumed;
  }

  @Override
  public String getCategory() {
    return "Custom";
  }

  @Override
  public String getBenefit() {
    return "Manually entered nutrition";
  }

  @Override
  public Nutrients portion(double quantity) {
    return consumed;
  }
}
