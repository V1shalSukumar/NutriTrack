package com.nutritrack.service;

import com.nutritrack.dao.FoodDao;
import com.nutritrack.model.Food;
import java.sql.SQLException;
import java.util.*;

public final class FoodCache {
  private final FoodDao dao;
  private List<Food> foods = List.of();
  private long revision;

  public FoodCache(FoodDao dao) throws SQLException {
    this.dao = dao;
    refresh();
  }

  public synchronized void refresh() throws SQLException {
    foods = List.copyOf(dao.all());
    revision++;
  }

  public synchronized List<Food> snapshot() {
    return foods;
  }

  public synchronized long getRevision() {
    return revision;
  }
}
