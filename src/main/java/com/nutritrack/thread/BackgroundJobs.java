package com.nutritrack.thread;

import com.nutritrack.dao.*;
import com.nutritrack.service.*;
import com.nutritrack.util.*;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public final class BackgroundJobs implements AutoCloseable {
  private final ScheduledExecutorService executor =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r, "nutritrack-report-worker");
            t.setDaemon(true);
            return t;
          });
  private final FoodCache cache;
  private final UserDao users;
  private final NutritionService nutrition;
  private volatile String status = "Waiting for first run";
  private final AtomicLong runs = new AtomicLong();

  public BackgroundJobs(FoodCache cache, UserDao users, NutritionService nutrition) {
    this.cache = cache;
    this.users = users;
    this.nutrition = nutrition;
  }

  public void start() {
    executor.scheduleWithFixedDelay(this::run, 5, 60, TimeUnit.SECONDS);
  }

  public void trigger() {
    executor.submit(this::run);
  }

  private void run() {
    try {
      cache.refresh();
      for (var user : users.all()) new ReportWriter().write(user, nutrition.week(user));
      status =
          "Completed "
              + LocalDateTime.now().withNano(0)
              + " on "
              + Thread.currentThread().getName();
      runs.incrementAndGet();
    } catch (Exception e) {
      status = "Failed: " + e.getClass().getSimpleName();
      e.printStackTrace();
    }
  }

  public String getStatus() {
    return status;
  }

  public long getRuns() {
    return runs.get();
  }

  public void close() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow();
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
