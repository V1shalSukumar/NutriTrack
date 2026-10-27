package com.nutritrack.model;

public final class Admin extends User {
  public Admin(long id, String name, String email, GoalProfile goal) {
    super(id, name, email, goal);
  }

  @Override
  public String getRole() {
    return "ADMIN";
  }

  @Override
  public boolean isAdmin() {
    return true;
  }
}
