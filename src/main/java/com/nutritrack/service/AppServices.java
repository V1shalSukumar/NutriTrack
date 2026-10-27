package com.nutritrack.service;

import com.nutritrack.dao.*;
import com.nutritrack.thread.*;

public final class AppServices implements AutoCloseable {
  public final Database db;
  public final FoodDao foods;
  public final UserDao users;
  public final MealDao meals;
  public final FoodCache cache;
  public final NutritionService nutrition;
  public final CsvService csv;
  public final BackgroundJobs jobs;

  public AppServices(Database db) throws Exception {
    this.db = db;
    db.initialize();
    foods = new FoodDao(db);
    users = new UserDao(db);
    meals = new MealDao(db);
    cache = new FoodCache(foods);
    nutrition = new NutritionService(meals, cache);
    csv = new CsvService(foods, cache);
    jobs = new BackgroundJobs(cache, users, nutrition);
  }

  public void close() {
    jobs.close();
  }
}
