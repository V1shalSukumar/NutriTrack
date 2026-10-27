package com.nutritrack.web;

import com.nutritrack.model.*;
import com.nutritrack.service.*;
import com.nutritrack.util.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public final class PortalServlet extends HttpServlet {
  private AppServices app() {
    return (AppServices) getServletContext().getAttribute("app");
  }

  private User user(HttpServletRequest r) {
    return (User) r.getSession().getAttribute("user");
  }

  private void redirect(HttpServletRequest r, HttpServletResponse s, String path)
      throws IOException {
    s.sendRedirect(r.getContextPath() + path);
  }

  private void view(HttpServletRequest r, HttpServletResponse s, String name)
      throws ServletException, IOException {
    r.getRequestDispatcher("/WEB-INF/views/" + name + ".jsp").forward(r, s);
  }

  @Override
  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    String path = r.getServletPath();
    try {
      if (path.equals("/login")) {
        view(r, s, "login");
        return;
      }
      User u = user(r);
      r.setAttribute("today", LocalDate.now());
      r.setAttribute("path", path);
      Object flash = r.getSession().getAttribute("flash");
      r.setAttribute("flash", flash);
      r.getSession().removeAttribute("flash");
      if (path.equals("/photo")) {
        byte[] photo;
        if (r.getParameter("draft") != null) {
          synchronized (r.getSession()) {
            int index = Integer.parseInt(r.getParameter("draft"));
            photo = index >= 0 && index < draft(r).size() ? draft(r).get(index).photo() : null;
          }
        } else photo = app().meals.photo(Long.parseLong(r.getParameter("id")), u.getId());
        if (photo == null) {
          s.sendError(404);
          return;
        }
        s.setContentType("image/png");
        s.setContentLength(photo.length);
        s.getOutputStream().write(photo);
        return;
      }
      if (path.equals("/report")) {
        Path file = new ReportWriter().write(u, app().nutrition.week(u));
        s.setContentType("text/csv;charset=UTF-8");
        s.setHeader("Content-Disposition", "attachment; filename=nutritrack-weekly.csv");
        Files.copy(file, s.getOutputStream());
        return;
      }
      r.setAttribute("foods", app().cache.snapshot());
      switch (path) {
        case "/dashboard" -> {
          DailySummary summary = app().nutrition.summarize(u, LocalDate.now());
          var gap = app().nutrition.gap(summary, u.getGoal());
          r.setAttribute("summary", summary);
          r.setAttribute("gap", gap);
          r.setAttribute("picks", app().nutrition.recommendations(gap));
          view(r, s, "dashboard");
        }
        case "/log" -> {
          r.setAttribute("draft", draft(r));
          r.setAttribute(
              "draftTotals",
              draft(r).stream().map(FoodEntry::total).reduce(Nutrients.zero(), Nutrients::plus));
          r.setAttribute("draftGrams", draft(r).stream().mapToDouble(FoodEntry::getGrams).sum());
          view(r, s, "log");
        }
        case "/daily" -> {
          LocalDate day =
              r.getParameter("date") == null
                  ? LocalDate.now()
                  : LocalDate.parse(r.getParameter("date"));
          DailySummary summary = app().nutrition.summarize(u, day);
          r.setAttribute("summary", summary);
          r.setAttribute("groups", app().nutrition.grouped(summary));
          Map<String, Nutrients> groupTotals = new LinkedHashMap<>();
          Map<String, Double> groupGrams = new LinkedHashMap<>();
          app()
              .nutrition
              .grouped(summary)
              .forEach(
                  (type, entries) -> {
                    groupTotals.put(
                        type,
                        entries.stream()
                            .map(FoodEntry::total)
                            .reduce(Nutrients.zero(), Nutrients::plus));
                    groupGrams.put(type, entries.stream().mapToDouble(FoodEntry::getGrams).sum());
                  });
          r.setAttribute("groupTotals", groupTotals);
          r.setAttribute("groupGrams", groupGrams);
          view(r, s, "daily");
        }
        case "/weekly" -> {
          r.setAttribute("history", app().nutrition.week(u));
          view(r, s, "weekly");
        }
        case "/admin" -> {
          r.setAttribute("users", app().users.all());
          r.setAttribute("jobs", app().jobs);
          r.setAttribute("revision", app().cache.getRevision());
          view(r, s, "admin");
        }
        case "/" -> redirect(r, s, "/dashboard");
        default -> s.sendError(404);
      }
    } catch (IllegalArgumentException | java.time.DateTimeException e) {
      s.sendError(400, "Invalid request value.");
    } catch (Exception e) {
      getServletContext().log("Request failed", e);
      s.sendError(500, "Unable to complete request. Check the server log.");
    }
  }

  @SuppressWarnings("unchecked")
  private List<FoodEntry> draft(HttpServletRequest r) {
    var session = r.getSession();
    if (session.getAttribute("draft") == null)
      session.setAttribute("draft", new ArrayList<FoodEntry>());
    return (List<FoodEntry>) session.getAttribute("draft");
  }

  private double number(HttpServletRequest r, String name) {
    try {
      return Double.parseDouble(r.getParameter(name));
    } catch (Exception e) {
      throw new IllegalArgumentException("Enter a valid number for " + name + ".");
    }
  }

  @Override
  protected void doPost(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    String path = r.getServletPath();
    try {
      // Force multipart validation before reading action; oversized uploads may leave parameters empty.
      if (r.getContentType() != null && r.getContentType().toLowerCase(Locale.ROOT).startsWith("multipart/form-data")) {
        r.getParts();
      }
      if (path.equals("/login")) {
        User u =
            app()
                .users
                .login(r.getParameter("email"), r.getParameter("password"), r.getParameter("role"));
        r.changeSessionId();
        r.getSession().setAttribute("user", u);
        redirect(r, s, u.isAdmin() ? "/admin" : "/dashboard");
        return;
      }
      if (path.equals("/logout")) {
        r.getSession().invalidate();
        redirect(r, s, "/login");
        return;
      }
      User u = user(r);
      String action = r.getParameter("action");
      if (path.equals("/log")) {
        synchronized (r.getSession()) {
          switch (action) {
            case "add" -> {
              List<FoodEntry> list = draft(r);
              if (list.size() >= 50)
                throw new IllegalArgumentException(
                    "Save this meal before adding more than 50 items.");
              list.add(
                  app()
                      .nutrition
                      .entry(
                          Long.parseLong(r.getParameter("foodId")),
                          Double.parseDouble(r.getParameter("quantity")),
                          r.getParameter("mealType")));
            }
            case "custom" -> {
              Map<String, String> values = new HashMap<>();
              for (String key :
                  List.of(
                      "name",
                      "quantity",
                      "grams",
                      "calories",
                      "protein",
                      "carbs",
                      "fat",
                      "fibre",
                      "mealType")) values.put(key, r.getParameter(key));
              r.getSession().setAttribute("customValues", values);
              if (draft(r).size() >= 50)
                throw new IllegalArgumentException(
                    "Save this meal before adding more than 50 items.");
              FoodEntry custom =
                  new CustomFoodService()
                      .create(
                          r.getParameter("name"),
                          number(r, "quantity"),
                          number(r, "grams"),
                          new Nutrients(
                              number(r, "calories"),
                              number(r, "protein"),
                              number(r, "carbs"),
                              number(r, "fat"),
                              number(r, "fibre")),
                          r.getParameter("mealType"),
                          null);
              Part file = r.getPart("photo");
              byte[] photo = null;
              if (file != null && file.getSize() > 0) {
                if (file.getSize() > FoodPhotos.MAX_BYTES)
                  throw new IllegalArgumentException("Choose an image smaller than 5 MB.");
                try (InputStream input = file.getInputStream()) {
                  photo = FoodPhotos.validate(input.readNBytes(FoodPhotos.MAX_BYTES + 1));
                }
              }
              draft(r)
                  .add(
                      new FoodEntry(0, custom.food(), custom.quantity(), custom.mealType(), photo));
              r.getSession().removeAttribute("customValues");
              r.getSession()
                  .setAttribute("flash", "Custom item added. Save the meal to record it.");
            }
            case "remove" -> draft(r).remove(Integer.parseInt(r.getParameter("index")));
            case "save" -> {
              if (draft(r).isEmpty())
                throw new IllegalArgumentException("Add at least one food before saving.");
              Meal meal = new Meal();
              draft(r).forEach(meal::add);
              app().meals.save(u.getId(), LocalDate.now(), meal);
              draft(r).clear();
              r.getSession()
                  .setAttribute("flash", "Meal saved. Your nutrition totals have been updated.");
              redirect(r, s, "/daily");
              return;
            }
            default -> throw new IllegalArgumentException("Unknown meal action.");
          }
        }
        redirect(r, s, "/log");
        return;
      }
      if (path.equals("/daily")) {
        app().meals.delete(Long.parseLong(r.getParameter("id")), u.getId());
        r.getSession().setAttribute("flash", "Food entry removed.");
        redirect(r, s, "/daily");
        return;
      }
      if (path.equals("/admin")) {
        switch (action) {
          case "import" -> {
            Part part = r.getPart("file");
            if (part != null && part.getSize() > 1048576)
              throw new IllegalArgumentException("CSV files must be smaller than 1 MB.");
            if (part == null || part.getSize() == 0)
              throw new IllegalArgumentException("Choose a CSV file.");
            try (Reader reader =
                new InputStreamReader(
                    part.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)) {
              int count = app().csv.importFoods(reader);
              r.getSession().setAttribute("flash", count + " foods imported successfully.");
            }
          }
          case "food" -> {
            String[] values = {
              r.getParameter("name"),
              r.getParameter("category"),
              r.getParameter("calories"),
              r.getParameter("protein"),
              r.getParameter("carbs"),
              r.getParameter("fat"),
              r.getParameter("fibre"),
              r.getParameter("unit"),
              r.getParameter("unit_grams")
            };
            StringWriter writer = new StringWriter();
            try (var csv =
                new org.apache.commons.csv.CSVPrinter(
                    writer, org.apache.commons.csv.CSVFormat.DEFAULT)) {
              csv.printRecord(
                  "name",
                  "category",
                  "calories",
                  "protein",
                  "carbs",
                  "fat",
                  "fibre",
                  "unit",
                  "unit_grams");
              csv.printRecord((Object[]) values);
            }
            app().csv.importFoods(new StringReader(writer.toString()));
            r.getSession()
                .setAttribute("flash", "Food saved. Matching names update existing foods.");
          }
          case "deleteFood" -> {
            app().foods.deactivate(Long.parseLong(r.getParameter("id")));
            app().cache.refresh();
            r.getSession()
                .setAttribute("flash", "Food archived; historical entries remain available.");
          }
          case "deactivateUser" -> {
            app().users.deactivate(Long.parseLong(r.getParameter("id")), u.getId());
            r.getSession().setAttribute("flash", "User deactivated. New logins are disabled.");
          }
          case "job" -> {
            app().jobs.trigger();
            r.getSession()
                .setAttribute("flash", "Background report job queued. Refresh to see completion.");
          }
          default -> throw new IllegalArgumentException("Unknown admin action.");
        }
        redirect(r, s, "/admin");
        return;
      }
      s.sendError(404);
    } catch (com.nutritrack.exception.InvalidLoginException e) {
      r.setAttribute("error", e.getMessage());
      view(r, s, "login");
    } catch (com.nutritrack.exception.InvalidQuantityException
        | com.nutritrack.exception.FoodNotFoundException
        | com.nutritrack.exception.CsvImportException
        | IllegalArgumentException e) {
      r.getSession().setAttribute("flash", e.getMessage());
      redirect(r, s, path);
    } catch (IllegalStateException e) {
      r.getSession()
          .setAttribute(
              "flash",
              "The upload is too large. Choose a JPG or PNG image smaller than 5 MB and try"
                  + " again.");
      redirect(r, s, path);
    } catch (Exception e) {
      getServletContext().log("Save failed", e);
      r.getSession()
          .setAttribute(
              "flash",
              "Unable to save. No partial meal/import was committed. Check the server log.");
      redirect(r, s, path);
    }
  }
}
