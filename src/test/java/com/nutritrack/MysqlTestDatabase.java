package com.nutritrack;

import com.nutritrack.dao.Database;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;

/** Creates an isolated temporary MySQL database for integration tests. */
final class MysqlTestDatabase implements AutoCloseable {
  private final String url;
  private final String username;
  private final String password;
  private final String databaseName;
  private final Database database;

  private MysqlTestDatabase(String url, String username, String password, String databaseName)
      throws SQLException {
    this.url = url;
    this.username = username;
    this.password = password;
    this.databaseName = databaseName;
    database = new Database(databaseUrl(url, databaseName), username, password);
    database.initialize();
  }

  static MysqlTestDatabase create() throws SQLException {
    String url = System.getenv("NUTRITRACK_TEST_DB_URL");
    String username = System.getenv("NUTRITRACK_TEST_DB_USER");
    String password = System.getenv("NUTRITRACK_TEST_DB_PASSWORD");
    Assumptions.assumeTrue(
        url != null && !url.isBlank() && username != null && password != null,
        "Set NUTRITRACK_TEST_DB_URL, NUTRITRACK_TEST_DB_USER and NUTRITRACK_TEST_DB_PASSWORD"
            + " to run MySQL integration tests.");

    String databaseName = "nutritrack_test_" + UUID.randomUUID().toString().replace("-", "");
    try (Connection connection = DriverManager.getConnection(url, username, password);
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE DATABASE `" + databaseName + "`");
    }
    try {
      return new MysqlTestDatabase(url, username, password, databaseName);
    } catch (SQLException e) {
      dropDatabase(url, username, password, databaseName);
      throw e;
    }
  }

  Database database() {
    return database;
  }

  @Override
  public void close() throws SQLException {
    dropDatabase(url, username, password, databaseName);
  }

  private static void dropDatabase(String url, String username, String password, String databaseName)
      throws SQLException {
    try (Connection connection = DriverManager.getConnection(url, username, password);
        Statement statement = connection.createStatement()) {
      statement.execute("DROP DATABASE IF EXISTS `" + databaseName + "`");
    }
  }

  private static String databaseUrl(String baseUrl, String databaseName) {
    int query = baseUrl.indexOf('?');
    String authority = query < 0 ? baseUrl : baseUrl.substring(0, query);
    String options = query < 0 ? "" : baseUrl.substring(query);
    while (authority.endsWith("/")) authority = authority.substring(0, authority.length() - 1);
    int databaseStart = authority.indexOf('/', "jdbc:mysql://".length());
    if (databaseStart >= 0) authority = authority.substring(0, databaseStart);
    return authority + "/" + databaseName + options;
  }
}
