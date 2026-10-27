package com.nutritrack;

import static org.junit.jupiter.api.Assertions.*;

import com.nutritrack.dao.*;
import com.nutritrack.exception.InvalidQuantityException;
import com.nutritrack.model.*;
import com.nutritrack.service.*;
import com.nutritrack.util.FoodPhotos;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class CustomFoodTest {
  byte[] png() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB), "png", out);
    return out.toByteArray();
  }

  @Test
  void totalsAreNotMultipliedByQuantity() throws Exception {
    var e =
        new CustomFoodService()
            .create("Sandwich", 2, 300, new Nutrients(500, 20, 60, 18, 5), "Lunch", null);
    assertEquals(300, e.getGrams(), 0.001);
    assertEquals(500, e.total().calories());
    assertEquals(5, e.total().fibre());
  }

  @Test
  void invalidCustomNumbersAreRejected() {
    var service = new CustomFoodService();
    assertThrows(
        InvalidQuantityException.class,
        () -> service.create("x", 0, 100, Nutrients.zero(), "Lunch", null));
    assertThrows(
        InvalidQuantityException.class,
        () -> service.create("x", 1, Double.NaN, Nutrients.zero(), "Lunch", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.create("x", 1, 100, new Nutrients(-1, 0, 0, 0, 0), "Lunch", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.create("x", 1, 100, new Nutrients(1, Double.NaN, 0, 0, 0), "Lunch", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.create(" ", 1, 100, Nutrients.zero(), "Lunch", null));
  }

  @Test
  void validImageIsNormalizedAndInvalidImageRejected() throws Exception {
    assertNotNull(FoodPhotos.validate(png()));
    assertThrows(
        IllegalArgumentException.class, () -> FoodPhotos.validate("<svg></svg>".getBytes()));
    assertThrows(
        IllegalArgumentException.class,
        () -> FoodPhotos.validate(new byte[FoodPhotos.MAX_BYTES + 1]));
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "NUTRITRACK_TEST_DB_URL", matches = "jdbc:mysql:.+")
  void customEntryAndImagePersistWithoutChangingCatalogue() throws Exception {
    try (MysqlTestDatabase testDatabase = MysqlTestDatabase.create()) {
      Database db = testDatabase.database();
      db.initialize();
      MealDao dao = new MealDao(db);
      int foods = new FoodDao(db).all().size();
      byte[] photo = FoodPhotos.validate(png());
      Meal meal = new Meal();
      meal.add(
          new CustomFoodService()
              .create("My lunch", 2, 300, new Nutrients(500, 20, 60, 18, 5), "Lunch", photo));
      LocalDate date = LocalDate.of(2020, 1, 1);
      dao.save(1, date, meal);
      var e = new MealDao(db).forDay(1, date).get(0);
      assertTrue(e.isCustom());
      assertTrue(e.isHasPhoto());
      assertEquals("My lunch", e.food().getName());
      assertEquals(500, e.total().calories());
      assertEquals(300, e.getGrams(), 0.001);
      assertArrayEquals(photo, dao.photo(e.id(), 1));
      assertNull(dao.photo(e.id(), 2));
      assertEquals(foods, new FoodDao(db).all().size());
      dao.delete(e.id(), 1);
      assertNull(dao.photo(e.id(), 1));
    }
  }

  @Test
  void photoArraysAreDefensivelyCopied() throws Exception {
    byte[] bytes = png();
    var e = new CustomFoodService().create("x", 1, 100, Nutrients.zero(), "Lunch", bytes);
    byte expected = bytes[0];
    bytes[0] = 0;
    e.photo()[0] = 0;
    assertEquals(expected, e.photo()[0]);
  }
}
