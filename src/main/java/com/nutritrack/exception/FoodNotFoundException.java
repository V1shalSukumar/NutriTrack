package com.nutritrack.exception;

public class FoodNotFoundException extends Exception {
  public FoodNotFoundException(String message) {
    super(message);
  }
}
