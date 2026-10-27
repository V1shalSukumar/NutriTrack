package com.nutritrack.util;

import com.nutritrack.model.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.apache.commons.csv.*;

public final class ReportWriter {
  public Path write(User user, SortedMap<LocalDate, DailySummary> days) throws IOException {
    Path folder = Path.of("data/reports");
    Files.createDirectories(folder);
    Path file = folder.resolve("weekly-user-" + user.getId() + ".csv");
    Path temp = Files.createTempFile(folder, "report-", ".tmp");
    try {
      try (Writer out = Files.newBufferedWriter(temp);
          CSVPrinter csv =
              new CSVPrinter(
                  out,
                  CSVFormat.DEFAULT
                      .builder()
                      .setHeader(
                          "date", "calories", "protein_g", "carbs_g", "fat_g", "fibre_g", "score")
                      .build())) {
        for (var entry : days.entrySet()) {
          DailySummary s = entry.getValue();
          Nutrients n = s.totals();
          csv.printRecord(
              entry.getKey(), n.calories(), n.protein(), n.carbs(), n.fat(), n.fibre(), s.score());
        }
      }
      Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temp);
    }
    return file;
  }
}
