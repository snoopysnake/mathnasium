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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by Admin on 2/13/2019.
 */
public class UpdateProperties extends Application {
    XSSFWorkbook workbook;
    XSSFSheet sheet;
    FileOutputStream fos = null;
    OutputStream output = null;
    Properties prop;
    String currentDir = System.getProperty("user.dir");
    String PATH = currentDir + "\\reports";
    boolean canExit = true;
    String cellMsg = "";
    int fails, succs;
    DateTimeFormatter fileDateFormat = DateTimeFormatter.ofPattern("M-d-yyyy");
    DateTimeFormatter cellDateFormat = DateTimeFormatter.ofPattern("M/d/yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
    DecimalFormat truncate = new DecimalFormat("#.##");
    DecimalFormat percentage = new DecimalFormat("#%");
    TextArea console;
    String cookie, requestVerificationToken;

    private class Student {
        String name;
        String startTime;
        String endTime;
        String timeFrame;
        SimpleDateFormat parser = new SimpleDateFormat("hh:mm aa");
        public Student(String n, String st, String et, String tf) {
            name = n;
            startTime = st;
            endTime = et;
            timeFrame = tf;
        }

        public void correctName() {
            if (name.equals("VINTHA, ESHAAN")) {
                name = "Eshaan Vintha"; // Name needs to be corrected
                return;
            }
//            if (name.equals("TEKEU, Sety")) {
//                name = "Sety Tekeu"; // Name needs to be corrected
//                return;
//            }
            name = name.replaceAll("[A-Z]{2,}", ""); // removes all caps words
            name = name.replaceAll("\\+", ""); // removes plus sign
            name = name.replaceAll("[0-9]", ""); // removes numbers
            name = name.replaceAll("/", ""); // removes stray forward slash
            String splitName[] = name.split(", ");

            if (splitName.length > 0) {
                splitName[0] = splitName[0].replaceAll("\\(.*\\)", ""); // removes parentheses + content inside
                splitName[0] = splitName[0].replaceAll("\\(",""); // removes stray opening parentheses
                splitName[0] = splitName[0].replaceAll("\\)","").trim(); // removes stray closing parentheses
                splitName[1] = splitName[1].replaceAll("\\(.*\\)", "");
                splitName[1] = splitName[1].replaceAll("\\(","");
                splitName[1] = splitName[1].replaceAll("\\)","").trim();

                name = splitName[1] + " " + splitName[0];
            }
            else name = "WRONG FORMAT";
        }
    }

    public HashMap<String, Student> parseSheet(File arrival, LocalDate startDate, LocalDate endDate) throws IOException, ParseException {
        HashMap<String, Student> studentsList = new HashMap<>();
        FileInputStream fis = new FileInputStream(arrival);
        workbook = new XSSFWorkbook(fis);
        sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIt = sheet.iterator();
        rowIt.next();
        while (rowIt.hasNext()) {
            Row row = rowIt.next();
            LocalDate date = LocalDate.parse(row.getCell(0).toString(),cellDateFormat);
            String timeFrame = row.getCell(2).toString();
            String id = row.getCell(3).toString();
            String name = row.getCell(4).toString();
            String checkIn = row.getCell(5).toString();

            if (!studentsList.containsKey(id))
                studentsList.put(id, new Student(name, checkIn, checkIn, timeFrame));
        }
        fis.close();

        for (String key : studentsList.keySet()) {
            Student student = studentsList.get(key);
            student.correctName();
        }

        return studentsList;
    }

    public String searchStudent(String studentName) throws Exception {
        Platform.runLater(() -> console.appendText("\nSearching for "+studentName + "..."));
        URL url = new URL("https://radius.mathnasium.com/Base/SearchGrid_Read");
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("__RequestVerificationToken", requestVerificationToken);
        params.put("sort", "");
        params.put("page", 1);
        params.put("pageSize", 10);
        params.put("group", "");
        params.put("filter", "");
        params.put("searchVal", studentName);
        params.put("centerId", "");
        params.put("filterId", 1);
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

        conn.setDoOutput(true);
        conn.getOutputStream().write(postDataBytes);
        if (conn.getResponseCode() == 200) {
            JsonReader jsonReader = Json.createReader(new InputStreamReader(conn.getInputStream()));
            JsonObject obj = jsonReader.readObject();
            jsonReader.close();
            if (obj != null) {
                JsonArray array = obj.getJsonArray("Data");
                if (array.size() == 1) {
//                    System.out.println(array.getJsonObject(0).toString());
                    String fullName = array.getJsonObject(0).getString("FullName");
                    String entityType = array.getJsonObject(0).getString("EntityType");
                    String id = array.getJsonObject(0).getString("Id");
                    if (entityType.toLowerCase().equals("student")) {
                        Platform.runLater(() -> console.appendText("\nSUCCESS: Student found! "));
                        System.out.println("SUCCESS: Student found!");
                        return id;
                    }
                    else {
                        Platform.runLater(() -> console.appendText("\nERROR: Student info mismatch! "));
                        System.out.println("ERROR: Student info mismatch!");
                        cellMsg = "Student info mismatch";
                        return "null";
                    }
                }
                else if (array.size() == 0) {
                    Platform.runLater(() -> console.appendText("\nERROR: Student not found! "));
                    System.out.println("ERROR: Student not found!");
                    cellMsg = "Student not found";
                    return "null";
                }
                else {
                    Platform.runLater(() -> console.appendText("\nERROR: Multiple students found! " +
                            "Attempting to find current enrollment..."));
                    System.out.println("ERROR: Multiple students found!");
                    cellMsg = "Multiple students found";
                    for (int i = 0; i < array.size(); i++) {
                        System.out.println(array.getJsonObject(0).toString());

                        String entityType = array.getJsonObject(0).getString("EntityType");
                        String id = array.getJsonObject(0).getString("Id");
                        if (entityType.toLowerCase().equals("student")) { // Sety has EntityType of Account
                            if (getEnrollmentID(id) > 0) {
                                Platform.runLater(() -> console.appendText("\nSUCCESS: Student found! "));
                                System.out.println("SUCCESS: Student found!");
                                return id;
                            }
                        }

                    }
                    Platform.runLater(() -> console.appendText("\nERROR: Student ID could not be found."));
                    System.out.println("ERROR: Student ID could not be found.");
                    cellMsg = "Student ID could not be found";
                    return "null";
                }
            }
            else {
                Platform.runLater(() -> console.appendText("\nERROR: Student not found! "));
                System.out.println("ERROR: Student not found!");
                cellMsg = "Student not found";
                return "null";
            }
        }
        else {
            Platform.runLater(() -> console.appendText("\nERROR: Could not make connection! "));
            System.out.println("ERROR: Could not make connection!");
            cellMsg = "Could not make connection";
            return "null";
        }
    }

    public int getEnrollmentID(String studentID) throws Exception {
        URL url = new URL("https://radius.mathnasium.com/Student/GetCurrentEnrollmentIds?studentId="+studentID);
        Map<String,Object> params = new LinkedHashMap<>();

        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String,Object> param : params.entrySet()) {
            if (postData.length() != 0) postData.append('&');
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
        }
        byte[] postDataBytes = postData.toString().getBytes("UTF-8");

        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        conn.setRequestProperty("cookie", cookie);
        conn.setDoOutput(true);
        conn.getOutputStream().write(postDataBytes);

        if (conn.getResponseCode() == 200) {
            JsonReader jsonReader = Json.createReader(new InputStreamReader(conn.getInputStream()));
            JsonArray array = jsonReader.readArray();
            jsonReader.close();
            if (array.size() > 0) {
                int enrollmentID = array.getJsonObject(0).getInt("currentEnrollmentId");
                if (enrollmentID != 0) {
                    Platform.runLater(() -> console.setText(console.getText() +
                            "\nSUCCESS: Found enrollment ID: " + enrollmentID));
                    System.out.println("SUCCESS: Found Enrollment ID: " + enrollmentID);
                    return enrollmentID;
                }
                else {
                    Platform.runLater(() -> console.appendText("\nERROR: Not currently enrolled! "));
                    System.out.println("ERROR: Not currently enrolled!");
                    cellMsg = "Not currently enrolled";
                    return 0;
                }
            }
            else {
                Platform.runLater(() -> console.appendText("\nERROR: Enrollment not found! "));
                System.out.println("ERROR: Enrollment not found!");
                cellMsg = "Enrollment not found";
                return -1;
            }
        }
        else {
            Platform.runLater(() -> console.appendText("\nERROR: Could not make connection! "));
            System.out.println("ERROR: Could not make connection!");
            cellMsg = "Could not make connection";
            return -1;
        }
    }

    public void showMainStage() throws Exception{
        Stage primaryStage = new Stage();
        primaryStage.setTitle("Update Student Properties");
        GridPane grid = new GridPane();
        ScrollPane sp = new ScrollPane(grid);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));

        Text sceneTitle = new Text("Properties Update");
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

        Button dirBtn = new Button("Change Reports Folder");
        dirBtn.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Reports Folder");
            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            if (selectedDirectory != null)
                PATH = selectedDirectory.getPath();
            System.out.println("Path changed to: " + PATH);
        });
        grid.add(dirBtn, 1, 5);

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
//        progressStage.setAlwaysOnTop(true);
        StringProperty status = new SimpleStringProperty();
        status.setValue("");

        Button btn = new Button("Start");
        btn.setOnAction(event -> {
            LocalDate startDate = attendanceDate1.getValue();
            LocalDate endDate = attendanceDate2.getValue();
            File arrival = validateExcelFile(startDate, endDate);
            succs = 0;
            fails = 0;
            if (arrival != null && arrival.exists()) {
                long startTime = System.currentTimeMillis();
                Task task = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        HashMap<String, Student> studentsList = parseSheet(arrival, startDate, endDate); // Parse through Excel
                        FileInputStream input = new FileInputStream("student.properties");
                        prop = new Properties();
                        prop.load(input);
                        input.close();

                        output = new FileOutputStream("student.properties");

                        int itr = 0;
                        int total = studentsList.size();
                        for (String key : studentsList.keySet()) {
                            Student student = studentsList.get(key);
                            updateProgress(itr, total);
                            Platform.runLater(() -> status.setValue("Adding property for "+student.name));
                            Platform.runLater(() -> console.setText(""));
                            updateMessage(percentage.format((double) itr / total));

                            if (student.name.equals("WRONG FORMAT")) {
                                fails++;
                                itr++;
                                continue;
                            } else if (prop.get(student.name) != null) {
                                if (prop.get(student.name).equals("null")) {
                                    System.out.println("ERROR: Student ID of " + student.name + " could not be found");
                                    fails++;
                                    itr++;
                                } else {
                                    System.out.print("Student key/pair exists! ");
                                    System.out.println(student.name + "=" + prop.getProperty(student.name));
                                    Platform.runLater(() -> console.setText("Student key/pair exists! "));
                                    Platform.runLater(() -> console.appendText(student.name + "=" + prop.getProperty(student.name)));
                                    succs++;
                                    itr++;
                                    // Success
                                }
                            } else {
                                String studentID = searchStudent(student.name);
                                prop.setProperty(student.name, studentID);
                                if (studentID.equals("null")) {
                                    fails++;
                                    itr++;
                                } else {
                                    succs++;
                                    itr++;
                                }
                            }
                        }
//                        prop.store(output, null);

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

                task.setOnFailed(event1 -> {
                    try {
                        stop();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    long endTime = System.currentTimeMillis();
                    btn.setDisable(false);
                    progressStage.hide();
                    canExit = true;
                    Exception e = (Exception) task.getException();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setHeaderText("Something went wrong!");
                    alert.setContentText("Please restart. Runtime of " +
                            truncate.format((double) (endTime - startTime) / 60000) + " minutes.");
                    alert.getDialogPane().setExpandableContent(new ScrollPane(new TextArea(e.toString())));
                    alert.showAndWait();
                    e.printStackTrace();
                });
                task.setOnCancelled(event2 -> {
                    btn.setDisable(false);
                    progressStage.hide();
                    canExit = true;
                    try {
                        stop();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                });
                task.setOnSucceeded(event3 -> {
                    try {
                        stop();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    long endTime = System.currentTimeMillis();
                    btn.setDisable(false);
                    progressStage.hide();
                    canExit = true;
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText("Success!");
                    alert.setContentText("Completed with " + succs +
                            " successes and " + fails + " failure(s). Runtime of " +
                            truncate.format((double) (endTime - startTime) / 60000) + " minutes.");
                    alert.showAndWait();
                });

                progressBar.progressProperty().bind(task.progressProperty());
                progressPercent.textProperty().bind(task.messageProperty());
                progressStatus.textProperty().bind(status);
                new Thread(task).start();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("File does not exist!");
                alert.setContentText("Please change the date or directory path.");
                alert.showAndWait();
            }
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

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage initStage) throws Exception {
        getCredentials();
        showMainStage();
    }

    @Override
    public void stop() throws IOException {
        System.out.println("Saving...");
        if (fos != null)
            fos.close();
        if (workbook != null)
            workbook.close();
        if (output != null) {
            prop.store(output, null);
            output.close();
        }
    }

    public File validateExcelFile(LocalDate startDate, LocalDate endDate) {
        System.out.println("Start: " + startDate.format(fileDateFormat)); // Excel log info
        System.out.println("End: " + endDate.format(fileDateFormat));
        if (endDate.isBefore(startDate)) {
            return null;
        }
        File dir1 = new File(PATH);
//        File dir1 = new File("C:\\Users\\Mathnasium\\Downloads");
        System.out.println(dir1);
        if (!dir1.exists()) {
            return null;
        }
        File[] foundFiles = dir1.listFiles((dir, name) -> name.startsWith("Client Arrivals Report " +
                startDate.format(fileDateFormat) + " - " + endDate.format(fileDateFormat)));
//        File[] foundFiles = dir1.listFiles((dir, name) -> name.startsWith("Client Arrivals Report "));
        if (foundFiles.length == 0) {
            return null;
        }
        File arrival = foundFiles[0];
        for (File f : foundFiles) {
            if (f.lastModified() > arrival.lastModified())
                arrival = f;
        }
//        System.out.println("FOUND: " + arrival.getName()); // Excel log info

        return arrival;
    }

    public void getCredentials() throws Exception {
        File file = new File("credentials.txt");
        Scanner input = new Scanner(file);
        cookie = input.nextLine();
        input.nextLine();
        requestVerificationToken = input.nextLine();
        if (searchStudent("Test Student").equals("1147558")) { // Simple verification method
            Platform.runLater(() -> console.setText(""));
            System.out.println("SUCCESS: Credentials valid!");
        }
        else {
            System.out.println("ERROR: Credentials invalid!");
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Credentials invalid!");
            alert.setContentText("Please add new verification token and cookie.");
            alert.showAndWait();
        }
        input.close();
    }
}