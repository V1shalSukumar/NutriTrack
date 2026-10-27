package com.nutritrack.dao;

import com.nutritrack.util.Passwords;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Properties;

public final class Database {
  private final String url;
  private final String username;
  private final String password;

  public Database(String url, String username, String password) {
    this.url = url;
    this.username = username;
    this.password = password;
  }

  public static Database local() {
    Properties properties = new Properties();
    Path config = Path.of("database.properties");
    if (Files.exists(config)) {
      try (InputStream input = Files.newInputStream(config)) {
        properties.load(input);
      } catch (IOException e) {
        throw new IllegalStateException("Could not read database.properties", e);
      }
    }
    String url = setting("NUTRITRACK_DB_URL", properties, "url", "jdbc:mysql://localhost:3306/nutritrack?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    String user = setting("NUTRITRACK_DB_USER", properties, "username", "root");
    String password = setting("NUTRITRACK_DB_PASSWORD", properties, "password", null);
    if (password == null)
      throw new IllegalStateException(
          "MySQL password is not configured. Copy database.properties.example to"
              + " database.properties and set its password.");
    return new Database(url, user, password);
  }

  private static String setting(String environmentName, Properties properties, String key, String fallback) {
    String environmentValue = System.getenv(environmentName);
    if (environmentValue != null) return environmentValue;
    return properties.getProperty(key, fallback);
  }

  public Connection open() throws SQLException {
    return DriverManager.getConnection(url, username, password);
  }

  public void initialize() throws SQLException {
    try (Connection c = open();
        Statement s = c.createStatement()) {
      s.execute(
          "CREATE TABLE IF NOT EXISTS users(id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
              + "name VARCHAR(100) NOT NULL,email VARCHAR(150) UNIQUE NOT NULL,password_hash"
              + " VARCHAR(256) NOT NULL,role VARCHAR(10) NOT NULL,calorie_goal DOUBLE DEFAULT"
              + " 2000,protein_goal DOUBLE DEFAULT 60,active BOOLEAN DEFAULT TRUE) ENGINE=InnoDB");
      s.execute(
          "CREATE TABLE IF NOT EXISTS food(id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
              + "name VARCHAR(100) UNIQUE NOT NULL,category VARCHAR(30) NOT NULL,calories"
              + " DOUBLE NOT NULL,protein DOUBLE NOT NULL,carbs DOUBLE NOT NULL,fat DOUBLE NOT"
              + " NULL,fibre DOUBLE NOT NULL,unit_name VARCHAR(30) NOT NULL,unit_grams DOUBLE NOT"
              + " NULL CHECK(unit_grams>0),active BOOLEAN DEFAULT TRUE) ENGINE=InnoDB");
      s.execute(
          "CREATE TABLE IF NOT EXISTS meal_log(id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
              + "user_id BIGINT,food_id BIGINT,log_date DATE NOT NULL,meal_type VARCHAR(20) NOT"
              + " NULL,quantity DOUBLE NOT NULL CHECK(quantity>0),"
              + "CONSTRAINT fk_meal_user FOREIGN KEY(user_id) REFERENCES users(id),"
              + "CONSTRAINT fk_meal_food FOREIGN KEY(food_id) REFERENCES food(id)) ENGINE=InnoDB");
      // Additive migration preserves existing users, catalogue foods and meal history.
      addColumnIfMissing(c, s, "meal_log", "custom_name", "VARCHAR(100)");
      addColumnIfMissing(c, s, "meal_log", "custom_grams", "DOUBLE");
      addColumnIfMissing(c, s, "meal_log", "custom_calories", "DOUBLE");
      addColumnIfMissing(c, s, "meal_log", "custom_protein", "DOUBLE");
      addColumnIfMissing(c, s, "meal_log", "custom_carbs", "DOUBLE");
      addColumnIfMissing(c, s, "meal_log", "custom_fat", "DOUBLE");
      addColumnIfMissing(c, s, "meal_log", "custom_fibre", "DOUBLE");
      addColumnIfMissing(c, s, "meal_log", "photo", "MEDIUMBLOB");
      try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM users")) {
        rs.next();
        if (rs.getInt(1) == 0) {
          seedUser(c, "Demo User", "user@nutritrack.com", "user123", "USER");
          seedUser(c, "Administrator", "admin@nutritrack.com", "admin123", "ADMIN");
        }
      }
      try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM food")) {
        rs.next();
        if (rs.getInt(1) == 0) {
          String[] rows = {
            "Poha,Prepared,180,3.5,30,5,1.5,plate,150",
            "Banana,Fruit,89,1.1,22.8,0.3,2.6,piece,120",
            "Roti,Grain,297,9.8,46.4,7.5,4.9,roti,40",
            "Steamed Rice,Grain,130,2.7,28,0.3,0.4,katori,150",
            "Sambar,Prepared,65,3,9,2,2.5,katori,150",
            "Plain Dosa,Grain,168,3.9,29,3.7,1,piece,80",
            "Upma,Prepared,165,4,25,5.5,1.8,plate,150",
            "Toor Dal,Protein,116,6.8,18,2,5,katori,150",
            "Chicken Curry,Protein,180,17,5,10,0,katori,150",
            "Boiled Egg,Protein,155,13,1.1,11,0,egg,50",
            "Paneer,Dairy,265,18.3,1.2,20.8,0,piece,20",
            "Apple,Fruit,52,0.3,14,0.2,2.4,piece,150",
            "Curd,Dairy,61,3.5,4.7,3.3,0,katori,150",
            "Chickpeas,Protein,164,8.9,27.4,2.6,7.6,katori,150"
          };
          try (PreparedStatement p =
              c.prepareStatement(
                  "INSERT INTO"
                      + " food(name,category,calories,protein,carbs,fat,fibre,unit_name,unit_grams)"
                      + " VALUES(?,?,?,?,?,?,?,?,?)")) {
            for (String row : rows) {
              String[] a = row.split(",");
              p.setString(1, a[0]);
              p.setString(2, a[1]);
              for (int i = 2; i < 7; i++) p.setDouble(i + 1, Double.parseDouble(a[i]));
              p.setString(8, a[7]);
              p.setDouble(9, Double.parseDouble(a[8]));
              p.addBatch();
            }
            p.executeBatch();
          }
          try (PreparedStatement p =
              c.prepareStatement(
                  "INSERT INTO meal_log(user_id,food_id,log_date,meal_type,quantity)"
                      + " VALUES(1,?,?,?,?)")) {
            for (int day = 0; day < 7; day++) {
              int[] ids = {1, 2, 3, 4, 5, 6, 7};
              String[] types = {
                "Breakfast", "Breakfast", "Lunch", "Lunch", "Lunch", "Dinner", "Snack"
              };
              double[] qs = {1, 1, 2, 1, 1, 2, 1};
              for (int i = 0; i < ids.length; i++) {
                p.setInt(1, ids[i]);
                p.setDate(2, Date.valueOf(java.time.LocalDate.now().minusDays(day)));
                p.setString(3, types[i]);
                p.setDouble(4, qs[i] * (day == 0 ? 1 : 0.9 + day * 0.06));
                p.addBatch();
              }
            }
            p.executeBatch();
          }
        }
      }
    }
  }

  private void addColumnIfMissing(Connection connection, Statement statement, String table,
      String column, String definition) throws SQLException {
    try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, table, column)) {
      if (!columns.next()) statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }
  }

  private void seedUser(Connection c, String name, String email, String password, String role)
      throws SQLException {
    try (PreparedStatement p =
        c.prepareStatement("INSERT INTO users(name,email,password_hash,role) VALUES(?,?,?,?)")) {
      p.setString(1, name);
      p.setString(2, email);
      p.setString(3, Passwords.hash(password));
      p.setString(4, role);
      p.executeUpdate();
    }
  }
}
