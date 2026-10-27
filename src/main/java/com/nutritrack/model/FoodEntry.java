package com.nutritrack.model;

public record FoodEntry(long id, Food food, double quantity, String mealType, byte[] photo)
    implements Trackable {
  public FoodEntry(long id, Food food, double quantity, String mealType) {
    this(id, food, quantity, mealType, null);
  }

  public FoodEntry {
    photo = photo == null ? null : photo.clone();
  }

  @Override
  public byte[] photo() {
    return photo == null ? null : photo.clone();
  }

  public boolean isHasPhoto() {
    return photo != null && photo.length > 0;
  }

  public boolean isCustom() {
    return food instanceof CustomFood;
  }

  public Nutrients total() {
    return food.portion(quantity);
  }

  public long getId() {
    return id;
  }

  public Food getFood() {
    return food;
  }

  public double getQuantity() {
    return quantity;
  }

  public String getMealType() {
    return mealType;
  }

  public double getGrams() {
    return quantity * food.getUnitGrams();
  }

  public Nutrients getTotal() {
    return total();
  }
}
