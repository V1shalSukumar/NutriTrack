package com.nutritrack.model;

public class User {
  private final long id;
  private final String name, email;
  private final GoalProfile goal;

  public User(long id, String name, String email, GoalProfile goal) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.goal = goal;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public GoalProfile getGoal() {
    return goal;
  }

  public String getRole() {
    return "USER";
  }

  public boolean isAdmin() {
    return false;
  }
}
