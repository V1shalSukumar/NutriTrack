package com.nutritrack.service;

import com.nutritrack.dao.*;
import com.nutritrack.exception.*;
import com.nutritrack.model.*;
import java.io.*;
import java.sql.SQLException;
import java.util.*;
import org.apache.commons.csv.*;

public final class CsvService {
  private final FoodDao dao;
  private final FoodCache cache;

  public CsvService(FoodDao dao, FoodCache cache) {
    this.dao = dao;
    this.cache = cache;
  }

  public int importFoods(Reader reader) throws IOException, CsvImportException, SQLException {
    List<Food> foods = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    try (CSVParser parser =
        CSVFormat.DEFAULT
            .builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .build()
            .parse(reader)) {
      String[] expected = {
        "name", "category", "calories", "protein", "carbs", "fat", "fibre", "unit", "unit_grams"
      };
      for (String header : expected)
        if (!parser.getHeaderMap().containsKey(header))
          throw new CsvImportException("Missing CSV column: " + header);
      for (CSVRecord r : parser) {
        try {
          String name = r.get("name"), unit = r.get("unit"), cat = r.get("category");
          if (name.isBlank()
              || name.length() > 100
              || unit.isBlank()
              || unit.length() > 30
              || !seen.add(name.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException();
          double[] n = new double[6];
          String[] keys = {"calories", "protein", "carbs", "fat", "fibre", "unit_grams"};
          for (int i = 0; i < 6; i++) {
            n[i] = Double.parseDouble(r.get(keys[i]));
            if (!Double.isFinite(n[i]) || n[i] < 0 || n[i] > 10000)
              throw new IllegalArgumentException();
          }
          if (n[5] <= 0) throw new IllegalArgumentException();
          Nutrients v = new Nutrients(n[0], n[1], n[2], n[3], n[4]);
          Food food =
              switch (cat) {
                case "Fruit" -> new Fruit(0, name, unit, n[5], v);
                case "Dairy" -> new Dairy(0, name, unit, n[5], v);
                case "Grain" -> new Grain(0, name, unit, n[5], v);
                case "Protein" -> new ProteinFood(0, name, unit, n[5], v);
                case "Prepared" -> new PreparedFood(0, name, unit, n[5], v);
                default -> throw new IllegalArgumentException();
              };
          foods.add(food);
          if (foods.size() > 1000)
            throw new CsvImportException("Import at most 1000 foods at a time.");
        } catch (IllegalArgumentException e) {
          throw new CsvImportException(
              "Invalid CSV row "
                  + (r.getRecordNumber() + 1)
                  + ". Check category, name, portion size and nonnegative numbers.");
        }
      }
    }
    if (foods.isEmpty()) throw new CsvImportException("The CSV has no food rows.");
    dao.saveBatch(foods);
    cache.refresh();
    return foods.size();
  }
}
