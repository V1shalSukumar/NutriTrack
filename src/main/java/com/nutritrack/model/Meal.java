package com.nutritrack.model;

import java.util.*;

// Composition: a Meal owns a collection of FoodEntry values.
public final class Meal implements Trackable {
  private final List<FoodEntry> entries = new ArrayList<>();

  public void add(FoodEntry entry) {
    entries.add(entry);
  }

  public List<FoodEntry> getEntries() {
    return List.copyOf(entries);
  }

  public Nutrients total() {
    return entries.stream().map(FoodEntry::total).reduce(Nutrients.zero(), Nutrients::plus);
  }
}
