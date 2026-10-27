package com.nutritrack.service;

import com.nutritrack.exception.InvalidQuantityException;
import com.nutritrack.model.*;
import java.util.List;

public final class CustomFoodService {
  public FoodEntry create(
      String name, double quantity, double grams, Nutrients totals, String mealType, byte[] photo)
      throws InvalidQuantityException {
    if (name == null || name.isBlank() || name.strip().length() > 100)
      throw new IllegalArgumentException("Enter an item name of 1–100 characters.");
    if (!Double.isFinite(quantity) || quantity <= 0 || quantity > 100)
      throw new InvalidQuantityException(
          "Quantity must be greater than 0 and no more than 100 portions.");
    if (!Double.isFinite(grams) || grams <= 0 || grams > 10000)
      throw new InvalidQuantityException(
          "Total grams must be greater than 0 and no more than 10,000.");
    if (!List.of("Breakfast", "Lunch", "Dinner", "Snack").contains(mealType))
      throw new IllegalArgumentException("Choose a valid meal type.");
    for (double n :
        new double[] {
          totals.calories(), totals.protein(), totals.carbs(), totals.fat(), totals.fibre()
        })
      if (!Double.isFinite(n) || n < 0 || n > 10000)
        throw new IllegalArgumentException("Nutrition values must be between 0 and 10,000.");
    return new FoodEntry(
        0, new CustomFood(name.strip(), quantity, grams, totals), quantity, mealType, photo);
  }
}
