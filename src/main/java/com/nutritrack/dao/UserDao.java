package com.nutritrack.dao;

import com.nutritrack.exception.InvalidLoginException;
import com.nutritrack.model.*;
import com.nutritrack.util.Passwords;
import java.sql.*;
import java.util.*;

public final class UserDao {
  private final Database db;

  public UserDao(Database db) {
    this.db = db;
  }

  private User map(ResultSet r) throws SQLException {
    GoalProfile g =
        new GoalProfile(r.getDouble("calorie_goal"), r.getDouble("protein_goal"), 275, 50, 25);
    return r.getString("role").equals("ADMIN")
        ? new Admin(r.getLong("id"), r.getString("name"), r.getString("email"), g)
        : new User(r.getLong("id"), r.getString("name"), r.getString("email"), g);
  }

  public User login(String email, String password, String role)
      throws SQLException, InvalidLoginException {
    try (Connection c = db.open();
        PreparedStatement p =
            c.prepareStatement("SELECT * FROM users WHERE email=? AND active=TRUE")) {
      p.setString(1, email);
      try (ResultSet r = p.executeQuery()) {
        if (r.next()
            && Passwords.matches(password, r.getString("password_hash"))
            && r.getString("role").equals(role)) return map(r);
      }
    }
    throw new InvalidLoginException("Email or password is incorrect.");
  }

  public List<User> all() throws SQLException {
    List<User> a = new ArrayList<>();
    try (Connection c = db.open();
        PreparedStatement p =
            c.prepareStatement("SELECT * FROM users WHERE active=TRUE ORDER BY id");
        ResultSet r = p.executeQuery()) {
      while (r.next()) a.add(map(r));
    }
    return a;
  }

  public void updateGoal(long id, double cal, double protein) throws SQLException {
    try (Connection c = db.open();
        PreparedStatement p =
            c.prepareStatement("UPDATE users SET calorie_goal=?,protein_goal=? WHERE id=?")) {
      p.setDouble(1, cal);
      p.setDouble(2, protein);
      p.setLong(3, id);
      p.executeUpdate();
    }
  }

  public void deactivate(long id, long current) throws SQLException {
    if (id == current)
      throw new IllegalArgumentException("You cannot deactivate your own account.");
    try (Connection c = db.open();
        PreparedStatement p =
            c.prepareStatement("UPDATE users SET active=FALSE WHERE id=? AND role='USER'")) {
      p.setLong(1, id);
      p.executeUpdate();
    }
  }
}
