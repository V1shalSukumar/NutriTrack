	package com.nutritrack.dao;

import com.nutritrack.model.*;
import java.sql.*;
import java.util.*;

public final class FoodDao {
  private final Database db;

  public FoodDao(Database db) {
    this.db = db;
  }

  public static Food map(ResultSet r) throws SQLException {
    long id = r.getLong("id");
    String n = r.getString("name"), u = r.getString("unit_name");
    double g = r.getDouble("unit_grams");
    Nutrients v =
        new Nutrients(
            r.getDouble("calories"),
            r.getDouble("protein"),
            r.getDouble("carbs"),
            r.getDouble("fat"),
            r.getDouble("fibre"));
    return switch (r.getString("category")) {
      case "Fruit" -> new Fruit(id, n, u, g, v);
      case "Dairy" -> new Dairy(id, n, u, g, v);
      case "Grain" -> new Grain(id, n, u, g, v);
      case "Protein" -> new ProteinFood(id, n, u, g, v);
      default -> new PreparedFood(id, n, u, g, v);
    };
  }

  public List<Food> all() throws SQLException {
    List<Food> list = new ArrayList<>();
    try (Connection c = db.open();
        PreparedStatement p =
            c.prepareStatement("SELECT * FROM food WHERE active=TRUE ORDER BY name");
        ResultSet r = p.executeQuery()) {
      while (r.next()) list.add(map(r));
    }
    return List.copyOf(list);
  }

  public void saveBatch(List<Food> foods) throws SQLException {
    try (Connection c = db.open()) {
      c.setAutoCommit(false);
      try (PreparedStatement p =
          c.prepareStatement(
              "INSERT INTO"
                  + " food(name,category,calories,protein,carbs,fat,fibre,unit_name,unit_grams,active)"
                  + " VALUES(?,?,?,?,?,?,?,?,?,TRUE)"
                  + " ON DUPLICATE KEY UPDATE category=VALUES(category),"
                  + "calories=VALUES(calories),protein=VALUES(protein),carbs=VALUES(carbs),"
                  + "fat=VALUES(fat),fibre=VALUES(fibre),unit_name=VALUES(unit_name),"
                  + "unit_grams=VALUES(unit_grams),active=TRUE")) {
        for (Food f : foods) {
          p.setString(1, f.getName());
          p.setString(2, f.getCategory());
          Nutrients n = f.getNutrients();
          p.setDouble(3, n.calories());
          p.setDouble(4, n.protein());
          p.setDouble(5, n.carbs());
          p.setDouble(6, n.fat());
          p.setDouble(7, n.fibre());
          p.setString(8, f.getUnit());
          p.setDouble(9, f.getUnitGrams());
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

  public void deactivate(long id) throws SQLException {
    try (Connection c = db.open();
        PreparedStatement p = c.prepareStatement("UPDATE food SET active=FALSE WHERE id=?")) {
      p.setLong(1, id);
      p.executeUpdate();
    }
  }
}
