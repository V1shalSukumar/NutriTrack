package com.nutritrack.dao;

import com.nutritrack.model.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class MealDao {
  private final Database db;

  public MealDao(Database db) {
    this.db = db;
  }

  public List<FoodEntry> forDay(long user, LocalDate day) throws SQLException {
    List<FoodEntry> a = new ArrayList<>();
    try (Connection c = db.open();
        PreparedStatement p =
            c.prepareStatement(
                "SELECT f.*,m.id AS"
                    + " entry_id,m.quantity,m.meal_type,m.custom_name,m.custom_grams,m.custom_calories,m.custom_protein,m.custom_carbs,m.custom_fat,m.custom_fibre,m.photo"
                    + " FROM meal_log m LEFT JOIN food f ON f.id=m.food_id WHERE m.user_id=? AND"
                    + " m.log_date=? ORDER BY m.id")) {
      p.setLong(1, user);
      p.setDate(2, java.sql.Date.valueOf(day));
      try (ResultSet r = p.executeQuery()) {
        while (r.next())
          a.add(
              new FoodEntry(
                  r.getLong("entry_id"),
                  r.getString("custom_name") == null
                      ? FoodDao.map(r)
                      : new CustomFood(
                          r.getString("custom_name"),
                          r.getDouble("quantity"),
                          r.getDouble("custom_grams"),
                          new Nutrients(
                              r.getDouble("custom_calories"),
                              r.getDouble("custom_protein"),
                              r.getDouble("custom_carbs"),
                              r.getDouble("custom_fat"),
                              r.getDouble("custom_fibre"))),
                  r.getDouble("quantity"),
                  r.getString("meal_type"),
                  r.getBytes("photo")));
      }
    }
    return a;
  }

  public void save(long user, LocalDate date, Meal meal) throws SQLException {
    try (Connection c = db.open()) {
      c.setAutoCommit(false);
      try (PreparedStatement p =
          c.prepareStatement(
              "INSERT INTO"
                  + " meal_log(user_id,food_id,log_date,meal_type,quantity,custom_name,custom_grams,custom_calories,custom_protein,custom_carbs,custom_fat,custom_fibre,photo)"
                  + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
        for (FoodEntry e : meal.getEntries()) {
          p.setLong(1, user);
          if (e.isCustom()) p.setNull(2, Types.BIGINT);
          else p.setLong(2, e.food().getId());
          p.setDate(3, java.sql.Date.valueOf(date));
          p.setString(4, e.mealType());
          p.setDouble(5, e.quantity());
          p.setString(6, e.isCustom() ? e.food().getName() : null);
          double[] values = {
            e.getGrams(),
            e.total().calories(),
            e.total().protein(),
            e.total().carbs(),
            e.total().fat(),
            e.total().fibre()
          };
          for (int i = 0; i < values.length; i++) {
            if (e.isCustom()) p.setDouble(i + 7, values[i]);
            else p.setNull(i + 7, Types.DOUBLE);
          }
          p.setBytes(13, e.photo());
          p.addBatch();
        }
        p.executeBatch();
        c.commit();
      } catch (SQLException e) {
        c.rollback();
        throw e;
      }
    }
  }

  public byte[] photo(long id, long user) throws SQLException {
    try (Connection c = db.open();
        PreparedStatement p =
            c.prepareStatement("SELECT photo FROM meal_log WHERE id=? AND user_id=?")) {
      p.setLong(1, id);
      p.setLong(2, user);
      try (ResultSet r = p.executeQuery()) {
        return r.next() ? r.getBytes(1) : null;
      }
    }
  }

  public void delete(long id, long user) throws SQLException {
    try (Connection c = db.open();
        PreparedStatement p = c.prepareStatement("DELETE FROM meal_log WHERE id=? AND user_id=?")) {
      p.setLong(1, id);
      p.setLong(2, user);
      p.executeUpdate();
    }
  }
}
