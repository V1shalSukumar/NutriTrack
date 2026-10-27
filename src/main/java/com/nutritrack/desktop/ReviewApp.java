package com.nutritrack.desktop;

import com.nutritrack.dao.Database;
import com.nutritrack.exception.InvalidLoginException;
import com.nutritrack.exception.InvalidQuantityException;
import com.nutritrack.model.*;
import com.nutritrack.service.AppServices;
import com.nutritrack.service.CustomFoodService;
import com.nutritrack.service.NutritionService;
import com.nutritrack.util.FoodPhotos;
import com.nutritrack.util.ReportWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/** Native JavaFX client for the NutriTrack nutrition application. */
public final class ReviewApp extends Application {
  private static final String GREEN = "#236b4d";
  private static final String PALE = "#f4f6f2";
  private static final String MUTED = "#67736b";
  private Stage stage;
  private final StackPane content = new StackPane();
  private final List<FoodEntry> draft = new ArrayList<>();
  private AppServices app;
  private User user;
  private Label topUser;

  @Override
  public void start(Stage primaryStage) {
    stage = primaryStage;
    primaryStage.setTitle("NutriTrack");
    primaryStage.setMinWidth(1050);
    primaryStage.setMinHeight(720);
    primaryStage.setScene(new Scene(centered(new Label("Preparing NutriTrack…")), 1200, 800));
    primaryStage.show();
    Task<AppServices> boot =
        new Task<>() {
          @Override
          protected AppServices call() throws Exception {
            return new AppServices(Database.local());
          }
        };
    boot.setOnSucceeded(
        e -> {
          app = boot.getValue();
          app.jobs.start();
          showLogin();
        });
    boot.setOnFailed(
        e -> showFatal("Could not open the NutriTrack database", boot.getException()));
    startTask(boot, "nutritrack-javafx-database-startup");
  }

  private void showLogin() {
    Label brand = new Label("NutriTrack");
    brand.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + GREEN + ";");
    Label intro = new Label("Log meals in everyday portions and understand your nutrition.");
    intro.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 14px;");
    TextField email = new TextField("user@nutritrack.com");
    PasswordField password = new PasswordField();
    password.setPromptText("Password");
    ToggleGroup roles = new ToggleGroup();
    RadioButton userRole = new RadioButton("User");
    RadioButton adminRole = new RadioButton("Admin");
    userRole.setToggleGroup(roles);
    adminRole.setToggleGroup(roles);
    userRole.setSelected(true);
    HBox roleRow = new HBox(24, userRole, adminRole);
    roleRow.setAlignment(Pos.CENTER_LEFT);
    Label message = muted("Demo accounts: user@nutritrack.com / user123 · admin@nutritrack.com / admin123");
    Button login = primary("Login");
    login.setMaxWidth(Double.MAX_VALUE);
    login.setOnAction(
        e -> {
          login.setDisable(true);
          message.setText("Signing in…");
          Task<User> task =
              new Task<>() {
                @Override
                protected User call() throws Exception {
                  return app.users.login(
                      email.getText().strip(),
                      password.getText(),
                      adminRole.isSelected() ? "ADMIN" : "USER");
                }
              };
          task.setOnSucceeded(done -> { user = task.getValue(); showShell(); showDashboard(); });
          task.setOnFailed(
              done -> {
                login.setDisable(false);
                Throwable error = task.getException();
                message.setText(error instanceof InvalidLoginException
                    ? "Email or password is incorrect."
                    : errorMessage(error));
              });
          startTask(task, "nutritrack-javafx-login");
        });
    VBox card = new VBox(16, brand, intro, label("Email"), email, label("Password"), password,
        new Separator(), roleRow, login, message);
    card.setMaxWidth(460);
    card.setPadding(new Insets(34));
    card.setStyle(panelStyle());
    StackPane page = centered(card);
    page.setStyle("-fx-background-color: " + PALE + ";");
    stage.setScene(new Scene(page, 1200, 800));
  }

  private void showShell() {
    BorderPane shell = new BorderPane();
    shell.setStyle("-fx-background-color: " + PALE + ";");
    VBox sidebar = new VBox(10);
    sidebar.setPadding(new Insets(24, 16, 20, 16));
    sidebar.setPrefWidth(215);
    sidebar.setStyle("-fx-background-color: white; -fx-border-color: #e3e9e3; -fx-border-width: 0 1 0 0;");
    Label brand = new Label("NutriTrack");
    brand.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + GREEN + ";");
    Label sub = muted("NUTRITION TRACKER");
    VBox nav = new VBox(8);
    nav.getChildren().addAll(
        navButton("Dashboard", this::showDashboard),
        navButton("Log meal", this::showLogMeal),
        navButton("Daily", this::showDaily),
        navButton("Weekly", this::showWeekly));
    if (user.isAdmin()) nav.getChildren().add(navButton("Administration", this::showAdmin));
    Region spacer = new Region();
    VBox.setVgrow(spacer, Priority.ALWAYS);
    Button logout = navButton("Log out", () -> { user = null; draft.clear(); showLogin(); });
    sidebar.getChildren().addAll(brand, sub, new Separator(), nav, spacer, logout);

    topUser = new Label(user.getName());
    topUser.setStyle("-fx-font-weight: 600; -fx-text-fill: #38433b;");
    HBox header = new HBox(topUser);
    header.setAlignment(Pos.CENTER_RIGHT);
    header.setPadding(new Insets(20, 32, 20, 32));
    header.setStyle("-fx-background-color: white; -fx-border-color: #e3e9e3; -fx-border-width: 0 0 1 0;");
    content.setPadding(new Insets(26, 32, 32, 32));
    ScrollPane scroll = new ScrollPane(content);
    scroll.setFitToWidth(true);
    scroll.setStyle("-fx-background: " + PALE + "; -fx-background-color: transparent;");
    shell.setLeft(sidebar);
    shell.setTop(header);
    shell.setCenter(scroll);
    stage.setScene(new Scene(shell, 1280, 820));
  }

  private Button navButton(String text, Runnable action) {
    Button button = new Button(text);
    button.setMaxWidth(Double.MAX_VALUE);
    button.setAlignment(Pos.CENTER_LEFT);
    button.setPadding(new Insets(12, 14, 12, 14));
    button.setStyle("-fx-background-color: transparent; -fx-text-fill: #38433b; -fx-font-size: 14px; -fx-background-radius: 8;");
    button.setOnAction(e -> action.run());
    button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #eaf2ec; -fx-text-fill: " + GREEN + "; -fx-font-size: 14px; -fx-background-radius: 8;"));
    button.setOnMouseExited(e -> button.setStyle("-fx-background-color: transparent; -fx-text-fill: #38433b; -fx-font-size: 14px; -fx-background-radius: 8;"));
    return button;
  }

  private void showDashboard() {
    background("Loading today's nutrition…", () -> app.nutrition.summarize(user, LocalDate.now()), summary -> {
      Nutrients n = summary.totals();
      GoalProfile goal = user.getGoal();
      TilePane metrics = new TilePane();
      metrics.setHgap(14); metrics.setVgap(14); metrics.setPrefColumns(2);
      metrics.getChildren().addAll(
          metric("Calories", n.calories(), goal.calories(), "kcal"),
          metric("Protein", n.protein(), goal.protein(), "g"),
          metric("Carbs", n.carbs(), goal.carbs(), "g"),
          metric("Fat", n.fat(), goal.fat(), "g"));
      Label score = new Label(summary.score() + " / 100  ·  " + summary.getRating());
      score.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + GREEN + ";");
      VBox scoreCard = card("Health score", score);
      summary.points().forEach((name, value) -> scoreCard.getChildren().add(muted(name + "   " + value)));
      NutritionServiceGap gap = gap(summary, goal);
      VBox gapCard = card("Gap detector", new Label(gap.heading()),
          muted(gap.details()));
      for (Food food : app.nutrition.recommendations(gap.asGap())) {
        Nutrients portion = food.portion(1);
        double value = switch (gap.name) { case "Protein" -> portion.protein(); case "Fibre" -> portion.fibre(); default -> portion.carbs(); };
        Label benefit = muted(food.getBenefit());
        gapCard.getChildren().add(new VBox(3,
            new Label(food.getName() + " · +" + fmt(value) + " g " + gap.name.toLowerCase() + " per " + food.getUnit()), benefit));
      }
      HBox lower = new HBox(16, scoreCard, gapCard); HBox.setHgrow(scoreCard, Priority.ALWAYS); HBox.setHgrow(gapCard, Priority.ALWAYS);
      content.getChildren().setAll(new VBox(18, pageTitle("Dashboard", "Your nutrition summary for today"), metrics, lower));
    });
  }

  private record NutritionServiceGap(String name, double missing, double fraction) {
    String heading() { return missing > 0 ? name + " is your biggest gap" : "Your tracked goals are met"; }
    String details() { return missing > 0 ? "You are " + fmt(missing) + " g short. Suggestions are ranked per portion." : "Keep logging meals to follow today's totals."; }
    NutritionService.Gap asGap() { return new NutritionService.Gap(name, missing, fraction); }
  }

  private NutritionServiceGap gap(DailySummary s, GoalProfile g) {
    var actual = app.nutrition.gap(s, g);
    return new NutritionServiceGap(actual.name(), actual.missing(), actual.fraction());
  }

  private Node metric(String name, double value, double goal, String unit) {
    Label amount = new Label(fmt(value) + " / " + fmt(goal) + " " + unit);
    amount.setStyle("-fx-font-size: 23px; -fx-font-weight: bold; -fx-text-fill: #27352b;");
    ProgressBar bar = new ProgressBar(goal > 0 ? Math.min(1, value / goal) : 0);
    bar.setMaxWidth(Double.MAX_VALUE); bar.setStyle("-fx-accent: " + GREEN + ";");
    return card(name, amount, bar, muted(fmt(goal - value > 0 ? goal - value : 0) + " " + unit + " remaining"));
  }

  private void showLogMeal() {
    background("Loading foods…", () -> app.cache.snapshot(), foods -> {
      ComboBox<Food> food = new ComboBox<>(FXCollections.observableArrayList(foods));
      food.setPromptText("Choose a food"); food.setPrefWidth(280);
      TextField search = new TextField(); search.setPromptText("Search foods"); search.setPrefWidth(220);
      search.textProperty().addListener((o,oldValue,query)->{
        String q=query==null?"":query.strip().toLowerCase(java.util.Locale.ROOT);
        List<Food> matches=foods.stream().filter(f->f.getName().toLowerCase(java.util.Locale.ROOT).contains(q)).toList();
        food.setItems(FXCollections.observableArrayList(matches));
        food.setValue(matches.size()==1?matches.get(0):null);
      });
      TextField quantity = new TextField("1"); quantity.setPrefWidth(90);
      ComboBox<String> meal = new ComboBox<>(FXCollections.observableArrayList("Breakfast", "Lunch", "Dinner", "Snack"));
      meal.setValue("Lunch");
      Label conversion = muted("Choose a food and quantity to see the conversion.");
      Runnable update = () -> {
        Food chosen = food.getValue();
        try {
          double q = Double.parseDouble(quantity.getText());
          if (chosen != null && q > 0) {
            Nutrients n = chosen.portion(q);
            conversion.setText(fmt(q * chosen.getUnitGrams()) + " g · " + fmt(n.calories()) + " kcal · " + fmt(n.protein()) + " g protein");
          }
        } catch (NumberFormatException ignored) { conversion.setText("Enter a valid quantity."); }
      };
      food.valueProperty().addListener((o,a,b) -> update.run()); quantity.textProperty().addListener((o,a,b) -> update.run());
      TableView<FoodEntry> table = foodEntryTable(false, LocalDate.now());
      Runnable refresh = () -> { table.setItems(FXCollections.observableArrayList(draft)); updateDraftTotals(table); };
      Button add = primary("Add to meal");
      add.setOnAction(e -> {
        try {
          if (food.getValue() == null) throw new IllegalArgumentException("Choose a food first.");
          draft.add(app.nutrition.entry(food.getValue().getId(), Double.parseDouble(quantity.getText()), meal.getValue()));
          refresh.run();
        } catch (Exception error) { showError("Could not add food", error); }
      });
      Button custom = secondary("Add custom item"); custom.setOnAction(e -> showCustomDialog(refresh));
      Button save = primary("Save meal");
      save.setOnAction(e -> {
        if (draft.isEmpty()) { showError("No items", new IllegalArgumentException("Add at least one item first.")); return; }
        Meal value = new Meal(); draft.forEach(value::add); save.setDisable(true);
        background("Saving meal…", () -> { app.meals.save(user.getId(), LocalDate.now(), value); return true; }, done -> { draft.clear(); showDaily(); });
      });
      HBox controls = new HBox(12, search, food, label("Quantity"), quantity, label("Meal"), meal, add, custom);
      controls.setAlignment(Pos.CENTER_LEFT);
      VBox form = card("Choose a food", controls, conversion, muted("If your food is missing, enter its nutrition totals with Add custom item."));
      VBox list = card("Items in this meal", table, save);
      VBox.setVgrow(table, Priority.ALWAYS);
      content.getChildren().setAll(new VBox(18, pageTitle("Log meal", "Add foods to today's meal"), form, list));
      refresh.run();
    });
  }

  private void showCustomDialog(Runnable refresh) {
    Dialog<FoodEntry> dialog = new Dialog<>(); dialog.initOwner(stage); dialog.setTitle("Add a custom food");
    ButtonType addType = new ButtonType("Add custom item", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);
    TextField name = new TextField(); name.setPromptText("Food name");
    TextField quantity = new TextField("1"), grams = new TextField();
    TextField calories = new TextField("0"), protein = new TextField("0"), carbs = new TextField("0"), fat = new TextField("0"), fibre = new TextField("0");
    ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("Breakfast", "Lunch", "Dinner", "Snack")); type.setValue("Lunch");
    Label photoLabel = muted("No photo selected");
    byte[][] photo = new byte[1][];
    Button choose = secondary("Attach JPG/PNG photo");
    choose.setOnAction(e -> {
      FileChooser chooser = new FileChooser(); chooser.setTitle("Choose food photo");
      chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPG or PNG", "*.jpg", "*.jpeg", "*.png"));
      var selected = chooser.showOpenDialog(stage);
      if (selected != null) try { photo[0] = FoodPhotos.validate(Files.readAllBytes(selected.toPath())); photoLabel.setText(selected.getName()); }
      catch (Exception error) { showError("Photo could not be attached", error); }
    });
    GridPane fields = new GridPane(); fields.setHgap(10); fields.setVgap(10); fields.setPadding(new Insets(8));
    int row=0; addField(fields,row++,"Name",name); addField(fields,row++,"Meal",type);
    addField(fields,row++,"Quantity",quantity); addField(fields,row++,"Total grams",grams);
    addField(fields,row++,"Calories",calories); addField(fields,row++,"Protein (g)",protein);
    addField(fields,row++,"Carbs (g)",carbs); addField(fields,row++,"Fat (g)",fat); addField(fields,row++,"Fiber (g)",fibre);
    fields.add(new HBox(8,choose,photoLabel),1,row);
    dialog.getDialogPane().setContent(new VBox(8, muted("Nutrition values and grams are totals for the amount consumed."), fields));
    dialog.setResultConverter(button -> {
      if (button != addType) return null;
      try {
        return new CustomFoodService().create(name.getText(), number(quantity), number(grams),
            new Nutrients(number(calories),number(protein),number(carbs),number(fat),number(fibre)), type.getValue(), photo[0]);
      } catch (Exception error) { showError("Check the custom food details", error); return null; }
    });
    dialog.showAndWait().ifPresent(entry -> { draft.add(entry); refresh.run(); });
  }

  private void showDaily() { showDaily(LocalDate.now()); }
  private void showDaily(LocalDate date) {
    DatePicker picker = new DatePicker(date);
    background("Loading daily meal…", () -> app.nutrition.summarize(user,date), summary -> {
      TableView<FoodEntry> table = foodEntryTable(true,date); table.setItems(FXCollections.observableArrayList(summary.entries()));
      Button refresh = secondary("Show date"); refresh.setOnAction(e -> showDaily(picker.getValue()));
      Nutrients n = summary.totals();
      HBox totals = new HBox(18, muted("Calories " + fmt(n.calories())), muted("Protein " + fmt(n.protein()) + " g"), muted("Carbs " + fmt(n.carbs()) + " g"), muted("Fat " + fmt(n.fat()) + " g"), muted("Fiber " + fmt(n.fibre()) + " g"));
      content.getChildren().setAll(new VBox(18, pageTitle("Daily log", "Foods and nutrition totals by meal"), new HBox(10,picker,refresh), totals, card("Meal entries",table)));
    });
  }

  private TableView<FoodEntry> foodEntryTable(boolean saved, LocalDate date) {
    TableView<FoodEntry> table = new TableView<>(); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    table.getColumns().add(column("Food", e -> e.food().getName()));
    table.getColumns().add(column("Meal", FoodEntry::mealType));
    table.getColumns().add(column("Quantity", e -> fmt(e.quantity()) + " " + e.food().getUnit()));
    table.getColumns().add(column("Grams", e -> fmt(e.getGrams())));
    table.getColumns().add(column("Calories", e -> fmt(e.total().calories())));
    table.getColumns().add(column("Protein", e -> fmt(e.total().protein())));
    table.getColumns().add(column("Carbs", e -> fmt(e.total().carbs())));
    table.getColumns().add(column("Fat", e -> fmt(e.total().fat())));
    table.getColumns().add(column("Fiber", e -> fmt(e.total().fibre())));
    TableColumn<FoodEntry,FoodEntry> photoCol = new TableColumn<>("Photo");
    photoCol.setCellValueFactory(v -> new javafx.beans.property.ReadOnlyObjectWrapper<>(v.getValue()));
    photoCol.setCellFactory(col -> new TableCell<>() {
      @Override protected void updateItem(FoodEntry entry, boolean empty) {
        super.updateItem(entry,empty); setGraphic(null);
        if (!empty && entry != null && entry.isHasPhoto()) {
          ImageView image = new ImageView(new Image(new ByteArrayInputStream(entry.photo()), 40, 40, true, true));
          setGraphic(image);
        }
      }
    });
    table.getColumns().add(photoCol);
    TableColumn<FoodEntry,FoodEntry> remove = new TableColumn<>(saved ? "Action" : "Remove");
    remove.setCellValueFactory(v -> new javafx.beans.property.ReadOnlyObjectWrapper<>(v.getValue()));
    remove.setCellFactory(col -> new TableCell<>() {
      private final Button button = secondary(saved ? "Remove" : "×");
      { button.setOnAction(e -> { FoodEntry entry=getItem(); if (entry == null) return;
        if (saved) background("Removing entry…", () -> { app.meals.delete(entry.id(),user.getId()); return true; }, ok -> showDaily(date));
        else { draft.remove(entry); showLogMeal(); }
      }); }
      @Override protected void updateItem(FoodEntry entry, boolean empty) { super.updateItem(entry,empty); setGraphic(empty || entry==null ? null : button); }
    });
    table.getColumns().add(remove);
    table.setPrefHeight(330);
    return table;
  }

  private TableColumn<FoodEntry,String> column(String title, java.util.function.Function<FoodEntry,String> value) {
    TableColumn<FoodEntry,String> c = new TableColumn<>(title);
    c.setCellValueFactory(row -> new javafx.beans.property.SimpleStringProperty(value.apply(row.getValue())));
    return c;
  }

  private void updateDraftTotals(TableView<FoodEntry> table) {
    if (table.getParent() instanceof VBox box) {
      box.getChildren().removeIf(n -> n.getUserData() != null && n.getUserData().equals("draftTotals"));
      Nutrients sum = draft.stream().map(FoodEntry::total).reduce(Nutrients.zero(),Nutrients::plus);
      Label total = muted("Total: " + fmt(sum.calories()) + " kcal · " + fmt(sum.protein()) + " g protein · " + fmt(sum.carbs()) + " g carbs · " + fmt(sum.fat()) + " g fat · " + fmt(sum.fibre()) + " g fiber");
      total.setUserData("draftTotals"); box.getChildren().add(Math.max(0,box.getChildren().indexOf(table)),total);
    }
  }

  private void showWeekly() {
    background("Loading seven-day history…", () -> app.nutrition.week(user), history -> {
      CategoryAxis x = new CategoryAxis(); x.setLabel("Day"); NumberAxis y = new NumberAxis(); y.setLabel("Calories");
      BarChart<String,Number> chart = new BarChart<>(x,y); chart.setLegendVisible(false); chart.setAnimated(false); chart.setPrefHeight(300);
      XYChart.Series<String,Number> series = new XYChart.Series<>();
      history.forEach((date,s) -> series.getData().add(new XYChart.Data<>(date.getDayOfWeek().toString().substring(0,3),s.totals().calories())));
      chart.getData().add(series);
      TableView<DailySummary> table = new TableView<>(); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
      table.getColumns().add(summaryColumn("Date",s -> s.date().toString()));
      table.getColumns().add(summaryColumn("Calories",s -> fmt(s.totals().calories())));
      table.getColumns().add(summaryColumn("% goal",s -> fmt(s.totals().calories()/user.getGoal().calories()*100)+"%"));
      table.getColumns().add(summaryColumn("Protein",s -> fmt(s.totals().protein())+" g"));
      table.getColumns().add(summaryColumn("Health score",s -> Integer.toString(s.score())));
      table.setItems(FXCollections.observableArrayList(history.values())); table.setPrefHeight(270);
      Button csv = secondary("Download CSV report"); csv.setOnAction(e -> saveWeeklyCsv());
      content.getChildren().setAll(new VBox(18,pageTitle("Weekly history","Seven days of nutrition totals"),card("Calories per day",chart),card("Daily totals",table,csv)));
    });
  }

  private TableColumn<DailySummary,String> summaryColumn(String title, java.util.function.Function<DailySummary,String> f) {
    TableColumn<DailySummary,String> c=new TableColumn<>(title); c.setCellValueFactory(row->new javafx.beans.property.SimpleStringProperty(f.apply(row.getValue()))); return c;
  }

  private void saveWeeklyCsv() {
    background("Preparing weekly report…", () -> new ReportWriter().write(user, app.nutrition.week(user)), file -> {
      FileChooser chooser=new FileChooser(); chooser.setTitle("Save NutriTrack weekly report"); chooser.setInitialFileName("nutritrack-weekly.csv");
      chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files","*.csv"));
      var to=chooser.showSaveDialog(stage);
      if (to != null) try { Files.copy(file,to.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
      catch(IOException error){showError("Could not save report",error);}
    });
  }

  private void showAdmin() {
    if (!user.isAdmin()) { showDashboard(); return; }
    background("Loading administration…", () -> new AdminData(app.users.all(), app.cache.snapshot()), data -> {
      VBox jobs=card("Background reports",muted(app.jobs.getStatus()),muted("Completed runs: "+app.jobs.getRuns()+" · Cache revision: "+app.cache.getRevision()));
      Button run=primary("Run report job"); run.setOnAction(e->{app.jobs.trigger(); showAdmin();});
      Button refresh=secondary("Refresh status"); refresh.setOnAction(e->showAdmin()); jobs.getChildren().addAll(new HBox(10,run,refresh));

      TableView<Food> foods=new TableView<>(); foods.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); foods.setItems(FXCollections.observableArrayList(data.foods));
      foods.getColumns().add(foodColumn("Food",Food::getName)); foods.getColumns().add(foodColumn("Category",Food::getCategory));
      foods.getColumns().add(foodColumn("Portion",f->"1 "+f.getUnit()+" · "+fmt(f.getUnitGrams())+" g"));
      foods.getColumns().add(foodColumn("kcal / 100 g",f->fmt(f.getNutrients().calories())));
      foods.getColumns().add(foodColumn("Protein / 100 g",f->fmt(f.getNutrients().protein())+" g"));
      Button archive=secondary("Archive selected food"); archive.setOnAction(e->{Food selected=foods.getSelectionModel().getSelectedItem(); if(selected==null)return;
        background("Archiving food…",()->{app.foods.deactivate(selected.getId());app.cache.refresh();return true;},ok->showAdmin());});

      TextField fname=new TextField(); fname.setPromptText("Food name");
      ComboBox<String> category=new ComboBox<>(FXCollections.observableArrayList("Prepared","Fruit","Dairy","Grain","Protein")); category.setValue("Prepared");
      TextField kcal=new TextField("0"), protein=new TextField("0"), carbs=new TextField("0"), fat=new TextField("0"), fibre=new TextField("0"), unit=new TextField("portion"), grams=new TextField("100");
      GridPane newFood=new GridPane(); newFood.setHgap(8);newFood.setVgap(8);
      newFood.add(fname,0,0);newFood.add(category,1,0);newFood.add(kcal,2,0);newFood.add(protein,3,0);newFood.add(carbs,4,0);newFood.add(fat,5,0);newFood.add(fibre,6,0);newFood.add(unit,7,0);newFood.add(grams,8,0);
      String[] prompts={"Name","Category","Calories /100g","Protein","Carbs","Fat","Fiber","Unit","Unit grams"}; Node[] nodes={fname,category,kcal,protein,carbs,fat,fibre,unit,grams};
      for(int i=0;i<nodes.length;i++){if(nodes[i] instanceof TextField t)t.setPromptText(prompts[i]); if(nodes[i] instanceof ComboBox<?> c)c.setPromptText(prompts[i]);}
      Button addFood=primary("Save food"); addFood.setOnAction(e->{try{
        String c=category.getValue(); Nutrients n=new Nutrients(number(kcal),number(protein),number(carbs),number(fat),number(fibre)); double g=number(grams);
        Food f=switch(c){case "Fruit"->new Fruit(0,fname.getText(),unit.getText(),g,n);case "Dairy"->new Dairy(0,fname.getText(),unit.getText(),g,n);case "Grain"->new Grain(0,fname.getText(),unit.getText(),g,n);case "Protein"->new ProteinFood(0,fname.getText(),unit.getText(),g,n);default->new PreparedFood(0,fname.getText(),unit.getText(),g,n);};
        background("Saving food…",()->{app.foods.saveBatch(List.of(f));app.cache.refresh();return true;},ok->showAdmin());
      }catch(Exception error){showError("Could not save food",error);}});

      Button importCsv=secondary("Import foods from CSV"); importCsv.setOnAction(e->{
        FileChooser chooser=new FileChooser();chooser.setTitle("Choose food CSV");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files","*.csv"));var file=chooser.showOpenDialog(stage);if(file==null)return;
        background("Importing and validating CSV…",()->{try(var reader=Files.newBufferedReader(file.toPath())){return app.csv.importFoods(reader);}},count->{showInfo("Food import",count+" foods imported.");showAdmin();});
      });
      TableView<User> users=new TableView<>(); users.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);users.setItems(FXCollections.observableArrayList(data.users));
      users.getColumns().add(userColumn("Name",User::getName));users.getColumns().add(userColumn("Email",User::getEmail));users.getColumns().add(userColumn("Role",User::getRole));
      TableColumn<User,User> deactivate=new TableColumn<>("Action");deactivate.setCellValueFactory(r->new javafx.beans.property.ReadOnlyObjectWrapper<>(r.getValue()));
      deactivate.setCellFactory(c->new TableCell<>(){private final Button b=secondary("Deactivate");{b.setOnAction(e->{User u=getItem();if(u==null||u.isAdmin())return;background("Deactivating account…",()->{app.users.deactivate(u.getId(),user.getId());return true;},ok->showAdmin());});}
        @Override protected void updateItem(User u,boolean empty){super.updateItem(u,empty);setGraphic(empty||u==null||u.isAdmin()?null:b);}});users.getColumns().add(deactivate);users.setPrefHeight(230);

      content.getChildren().setAll(new VBox(18,pageTitle("Administration","Manage foods, accounts and scheduled reports"),jobs,
          card("Add or update food · nutrition per 100 g",newFood,addFood),card("Food catalogue",foods,archive,new HBox(10,importCsv)),card("Users",users)));
    });
  }

  private record AdminData(List<User> users,List<Food> foods){}
  private TableColumn<Food,String> foodColumn(String title,java.util.function.Function<Food,String> f){TableColumn<Food,String>c=new TableColumn<>(title);c.setCellValueFactory(r->new javafx.beans.property.SimpleStringProperty(f.apply(r.getValue())));return c;}
  private TableColumn<User,String> userColumn(String title,java.util.function.Function<User,String> f){TableColumn<User,String>c=new TableColumn<>(title);c.setCellValueFactory(r->new javafx.beans.property.SimpleStringProperty(f.apply(r.getValue())));return c;}

  private void addField(GridPane grid,int row,String name,Node value){grid.add(label(name),0,row);grid.add(value,1,row);if(value instanceof Control c)c.setPrefWidth(260);}
  private VBox card(String title,Node...children){Label heading=new Label(title);heading.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #2c3930;");VBox box=new VBox(12);box.setPadding(new Insets(20));box.setStyle(panelStyle());box.getChildren().add(heading);box.getChildren().addAll(children);return box;}
  private String panelStyle(){return "-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8e2; -fx-border-radius: 12;";}
  private VBox pageTitle(String title,String subtitle){Label h=new Label(title);h.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #28342b;");return new VBox(4,h,muted(subtitle));}
  private Label label(String text){return new Label(text);}
  private Label muted(String text){Label l=new Label(text);l.setStyle("-fx-text-fill: "+MUTED+";");l.setWrapText(true);return l;}
  private Button primary(String text){Button b=new Button(text);b.setStyle("-fx-background-color: "+GREEN+"; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 16;");return b;}
  private Button secondary(String text){Button b=new Button(text);b.setStyle("-fx-background-color: #eaf1ec; -fx-text-fill: "+GREEN+"; -fx-background-radius: 8; -fx-padding: 8 12;");return b;}
  private StackPane centered(Node node){StackPane p=new StackPane(node);p.setPadding(new Insets(24));return p;}
  private static String fmt(double v){return String.format(java.util.Locale.US,"%.1f",v);}
  private double number(TextField field){return Double.parseDouble(field.getText().strip());}
  private String errorMessage(Throwable e){return e==null?"Unexpected error":e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}

  private <T> void background(String message,Callable<T> action,Consumer<T> success){
    Label waiting=muted(message);content.getChildren().setAll(centered(waiting));
    Task<T> task=new Task<>(){@Override protected T call()throws Exception{return action.call();}};
    task.setOnSucceeded(e->success.accept(task.getValue()));
    task.setOnFailed(e->showError("NutriTrack",task.getException()));
    startTask(task,"nutritrack-javafx-worker");
  }
  private void startTask(Task<?> task,String name){Thread t=new Thread(task,name);t.setDaemon(true);t.start();}
  private void showError(String title,Throwable error){Alert a=new Alert(Alert.AlertType.ERROR);a.initOwner(stage);a.setTitle(title);a.setHeaderText(title);a.setContentText(errorMessage(error));a.showAndWait();}
  private void showInfo(String title,String message){Alert a=new Alert(Alert.AlertType.INFORMATION,message,ButtonType.OK);a.initOwner(stage);a.setTitle(title);a.setHeaderText(title);a.showAndWait();}
  private void showFatal(String title,Throwable error){showError(title,error);stage.close();}

  @Override public void stop(){if(app!=null)app.close();}
  public static void main(String[] args){launch(args);}
}
