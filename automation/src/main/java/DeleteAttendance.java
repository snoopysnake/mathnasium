import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by Admin on 2/18/2019.
 */
public class DeleteAttendance extends Application {
    DateTimeFormatter cellDateFormat = DateTimeFormatter.ofPattern("M/d/yyyy");
    DecimalFormat truncate = new DecimalFormat("#.##");
    DecimalFormat percentage = new DecimalFormat("#%");
    boolean canExit = true;
    TextArea console;
    int succs, fails;
    String cookie, requestVerificationToken;

    public void showMainStage() throws Exception {
        Stage primaryStage = new Stage();
        primaryStage.setTitle("Attendance Deletion Automation");
        GridPane grid = new GridPane();
        ScrollPane sp = new ScrollPane(grid);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));

        Text sceneTitle = new Text("Attendance Deletion Automation");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(sceneTitle, 0, 0, 2, 1);

        Label attendance1 = new Label("Start Date:");
        grid.add(attendance1, 0, 3);
        DatePicker attendanceDate1 = new DatePicker();
        attendanceDate1.setValue(LocalDate.now().minusDays(1));
        attendanceDate1.getEditor().setDisable(true);
        attendanceDate1.setStyle("-fx-opacity: 1");
        attendanceDate1.getEditor().setStyle("-fx-opacity: 1");
        grid.add(attendanceDate1, 1, 3);

        Label attendance2 = new Label("End Date:");
        grid.add(attendance2, 0, 4);
        DatePicker attendanceDate2 = new DatePicker();
        attendanceDate2.setValue(LocalDate.now().minusDays(1));
        attendanceDate2.getEditor().setDisable(true);
        attendanceDate2.setStyle("-fx-opacity: 1");
        attendanceDate2.getEditor().setStyle("-fx-opacity: 1");
        grid.add(attendanceDate2, 1, 4);

        // Progress Bar
        Stage progressStage = new Stage();
        GridPane progressGrid = new GridPane();
        progressGrid.setAlignment(Pos.CENTER);
        StackPane stack = new StackPane();
        ProgressBar progressBar = new ProgressBar();
        progressBar.setMinWidth(290);
        Label progressPercent = new Label();
        stack.getChildren().addAll(progressBar, progressPercent);
        progressGrid.add(stack, 0,0);
        Label progressStatus = new Label("");
        progressGrid.add(progressStatus, 0,1);
        console = new TextArea();
        console.setEditable(false);
        ScrollPane dps = new ScrollPane(console);
        dps.setMaxHeight(150);
        dps.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        dps.setFitToHeight(true);
        progressGrid.add(dps, 0,3);
        progressStage.setScene(new Scene(progressGrid, 350, 250));
        StringProperty status = new SimpleStringProperty();
        status.setValue("");

        Button btn = new Button("Start");
        btn.setOnAction(event -> {
            long startTime = System.currentTimeMillis();
            LocalDate startDate = attendanceDate1.getValue();
            LocalDate endDate = attendanceDate2.getValue();
                Task task = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        succs = 0;
                        fails = 0;
                        FileInputStream input = new FileInputStream("student.properties");
                        Properties prop = new Properties();
                        prop.load(input);
                        input.close();

                        int itr = 0;
                        for (Object key : prop.keySet()) {
                            Platform.runLater(() -> status.setValue("Deleting attendance for " + key.toString()));
                            updateMessage(percentage.format((double) itr / prop.size()));
                            updateProgress(itr, prop.size());
                            System.out.println(prop.getProperty(key.toString()));
                            for (int i = 1; i <= 3; i++) {
                                int finalI = i;
                                Platform.runLater(() -> console.setText("Deletion attempt " + finalI));
                                System.out.println("Deleting attendance of " + key + " attempt " + i);
                                validateAttendance(prop.getProperty(key.toString()), startDate, endDate);
                                System.out.println();
                            }
                            itr++;
                        }

                        updateProgress(1, 1);
                        updateMessage("Done.");

                        return null;
                    }
                };
                task.setOnScheduled(event0 -> {
                    btn.setDisable(true);
                    progressStage.show();
                    canExit = false;
                });

                task.setOnSucceeded(event3 -> {
                    long endTime = System.currentTimeMillis();
                    btn.setDisable(false);
                    progressStage.hide();
                    canExit = true;
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText("Success!");
                    alert.setContentText("Completed attendance automation with " + succs +
                            " successes and " + fails + " failure(s). Runtime of " +
                            truncate.format((double) (endTime - startTime) / 60000) + " minutes.");
                    alert.showAndWait();
                });

                progressBar.progressProperty().bind(task.progressProperty());
                progressPercent.textProperty().bind(task.messageProperty());
                progressStatus.textProperty().bind(status);
                new Thread(task).start();
        });

        HBox hbBtn = new HBox(10);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 11);
        primaryStage.setScene(new Scene(sp, 330, 330));
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            if (!canExit) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Cannot exit automation.");
                alert.setContentText("Please wait for the process to finish.");
                alert.showAndWait();
                e.consume();
            }
            else {
                progressStage.close();
            }
        });

    }

    public void validateAttendance(String studentID, LocalDate startDate,
                                   LocalDate endDate) throws Exception {
        System.out.println("Validating attendance...");
        URL url = new URL("https://radius.mathnasium.com/Attendance/Attendances_Read?StudentId=" + studentID);
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("__RequestVerificationToken", requestVerificationToken);
        params.put("sort", "");
        params.put("group", "");
        params.put("filter", "");
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String,Object> param : params.entrySet()) {
            if (postData.length() != 0) postData.append('&');
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
        }
        byte[] postDataBytes = postData.toString().getBytes("UTF-8");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        conn.setRequestProperty("cookie", cookie);
        conn.setRequestProperty("origin","https://radius.mathnasium.com");
        conn.setRequestProperty("referer","https://radius.mathnasium.com/Student/Details/"+studentID);
        conn.setRequestProperty("user-agent","Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36");
        conn.setRequestProperty("x-requested-with", "XMLHttpRequest");

        conn.setDoOutput(true);
        conn.getOutputStream().write(postDataBytes);
        if (conn.getResponseCode() == 200) {
            JsonReader jsonReader = Json.createReader(new InputStreamReader(conn.getInputStream()));
            JsonObject obj = jsonReader.readObject();
            jsonReader.close();
            if (obj != null) {
                JsonArray array = obj.getJsonArray("Data");
                for (int i = 0; i < array.size(); i++) {
                    String jsonDate = array.getJsonObject(i).getString("AttendanceDateString");
                    LocalDate date = LocalDate.parse(jsonDate, cellDateFormat);
                    if ((date.equals(startDate) || date.equals(endDate))
                            || date.isBefore(endDate) && date.isAfter(startDate)) {
                        Platform.runLater(() -> console.appendText("\nDeleting attendance on " + date + "..."));
                        delete(studentID, array.getJsonObject(i));
                    }
                }
            }
        }
    }

    public void delete(String studentID, JsonObject obj) throws Exception {
        URL url = new URL("https://radius.mathnasium.com/Attendance/DeleteAttendance");
        String json = obj.toString();
//        System.out.println(obj.toString());
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("cookie", cookie);
        conn.setRequestProperty("__RequestVerificationToken", requestVerificationToken);
        conn.setRequestProperty("origin","https://radius.mathnasium.com");
        conn.setRequestProperty("referer","https://radius.mathnasium.com/Student/Details/"+studentID);
        conn.setRequestProperty("user-agent","Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36");
        conn.setRequestProperty("x-requested-with", "XMLHttpRequest");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.getOutputStream().write(json.getBytes("UTF-8"));
        if (conn.getResponseCode() == 200) {
            JsonReader jsonReader = Json.createReader(new InputStreamReader(conn.getInputStream()));
            JsonArray array = jsonReader.readArray();
            if (array.toString().equals("[{}]")) {
                Platform.runLater(() -> console.appendText("\nSUCCESS: Successfully deleted!"));
                succs++;
                System.out.print("succ ");
            }
            else {
                Platform.runLater(() -> console.appendText("\nERROR: Failed to delete!"));
                fails++;
                System.out.print("err ");
            }
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        getCredentials();
        showMainStage();
    }

    public void getCredentials() throws FileNotFoundException {
        File file = new File("credentials.txt");
        Scanner input = new Scanner(file);
        cookie = input.nextLine();
        input.nextLine();
        requestVerificationToken = input.nextLine();

        input.close();
    }

}

