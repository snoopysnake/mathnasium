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
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Created by Admin on 2/13/2019.
 */
public class Attendance extends Application {
    XSSFWorkbook workbook;
    XSSFSheet sheet;
    FileOutputStream fos = null;
    OutputStream output = null;
    Properties prop;
    String currentDir = System.getProperty("user.dir");
    String PATH = currentDir + "\\reports";
    boolean canExit = true;
    CellStyle successCell, failCell;
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

        public void addTime(String time) throws ParseException {
            Date parseTime = parser.parse(time);
            Date parseST = parser.parse(startTime);
            Date parseET = parser.parse(endTime);
            if (parseTime.before(parseST) && parseTime.before(parseET)) {
                startTime = time;
            }
            else if (parseTime.after(parseST) && parseTime.after(parseET)) {
                endTime = time;
            }
        }

        public void correctTime() {
            String parseTime[] = timeFrame.split("-");
            if (startTime.equals(endTime)) {
                startTime = parseTime[0];
                endTime = parseTime[1];
//                System.out.print("CORRECTED: "); // Excel log info
            }
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

    public ArrayList<HashMap<String, Student>> parseSheet(File arrival, LocalDate startDate, LocalDate endDate) throws IOException, ParseException {
        int days = (int) DAYS.between(startDate, endDate) + 1;
        ArrayList<HashMap<String,Student>> studentsList = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            HashMap<String, Student> students = new HashMap<>();
            studentsList.add(i, students);
        }
        FileInputStream fis = new FileInputStream(arrival);
        workbook = new XSSFWorkbook(fis);
        successCell = workbook.createCellStyle();
        successCell.setFillBackgroundColor(IndexedColors.GREEN.getIndex());
        successCell.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        successCell.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        failCell = workbook.createCellStyle();
        failCell.setFillBackgroundColor(IndexedColors.RED.getIndex());
        failCell.setFillForegroundColor(IndexedColors.RED.getIndex());
        failCell.setFillPattern(FillPatternType.SOLID_FOREGROUND);

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

            int dateIndex = (int) DAYS.between(startDate, date);
            if ((row.getCell(9) != null && !row.getCell(9).toString().equals("\u2714")) || row.getCell(9) == null) {
                if (!studentsList.get(dateIndex).containsKey(id))
                    studentsList.get(dateIndex).put(id, new Student(name, checkIn, checkIn, timeFrame));
                else studentsList.get(dateIndex).get(id).addTime(checkIn);
            }

//            if ((row.getCell(9) != null && !row.getCell(9).toString().equals("\u2714") &&
//                    row.getCell(9).toString().equals("\u2718")) || row.getCell(9) == null) {
//                if (!students.containsKey(id))
//                    students.put(id, new Student(name, checkIn, checkIn, timeFrame));
//                else students.get(id).addTime(checkIn);
//            }

//            if (!students.containsKey(id))
//                students.put(id, new Student(name, checkIn, checkIn, timeFrame));
//            else students.get(id).addTime(checkIn);
        }
        fis.close();

        for (int i = 0; i < studentsList.size(); i++) {
            for (String key : studentsList.get(i).keySet()) {
                Student student = studentsList.get(i).get(key);
                student.correctTime();
                student.correctName();
//            System.out.println(student.name + " " + student.startTime + " " + student.endTime);  // Excel log info
            }
        }
        return studentsList;
    }

    public String searchStudent(String studentName) throws Exception {
        Platform.runLater(() -> console.setText(console.getText() + "\nSearching for "+studentName + "..."));
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
                        Platform.runLater(() -> console.setText(console.getText() + "\nSUCCESS: Student found! "));
                        System.out.println("SUCCESS: Student found!");
                        return id;
                    }
                    else {
                        Platform.runLater(() -> console.setText(console.getText() + "\nERROR: Student info mismatch! "));
                        System.out.println("ERROR: Student info mismatch!");
                        return "null";
                    }
                }
                else if (array.size() == 0) {
                    Platform.runLater(() -> console.setText(console.getText() + "\nERROR: Student not found! "));
                    System.out.println("ERROR: Student not found!");
                    return "null";
                }
                else {
                    Platform.runLater(() -> console.setText(console.getText() + "\nERROR: Multiple students found! " +
                            "Attempting to find current enrollment..."));
                    System.out.println("ERROR: Multiple students found!");
                    for (int i = 0; i < array.size(); i++) {
                                            System.out.println(array.getJsonObject(0).toString());

                        String entityType = array.getJsonObject(0).getString("EntityType");
                        String id = array.getJsonObject(0).getString("Id");
                        if (entityType.toLowerCase().equals("student")) { // Sety has EntityType of Account
                            if (getEnrollmentID(id) > 0) {
                                Platform.runLater(() -> console.setText(console.getText() + "\nSUCCESS: Student found! "));
                                System.out.println("SUCCESS: Student found!");
                                return id;
                            }
                        }

                    }
                    Platform.runLater(() -> console.setText(console.getText() + "\nERROR: Student ID could not be found."));
                    System.out.println("ERROR: Student ID could not be found.");
                    return "null";
                }
            }
            else {
                Platform.runLater(() -> console.setText(console.getText() + "\nERROR: Student not found! "));
                System.out.println("ERROR: Student not found!");
                return "null";
            }
        }
        else {
            Platform.runLater(() -> console.setText(console.getText() + "\nERROR: Could not make connection! "));
            System.out.println("ERROR: Could not make connection!");
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
                    Platform.runLater(() -> console.setText(console.getText() + "\nERROR: Not currently enrolled! "));
                    System.out.println("ERROR: Not currently enrolled!");
                    return 0;
                }
            }
            else {
                Platform.runLater(() -> console.setText(console.getText() + "\nERROR: Enrollment not found! "));
                System.out.println("ERROR: Enrollment not found!");
                return -1;
            }
        }
        else {
            Platform.runLater(() -> console.setText(console.getText() + "\nERROR: Could not make connection! "));
            System.out.println("ERROR: Could not make connection!");
            return -1;
        }
    }

    public boolean addAttendance(String studentID, String attDate, String arrTime,
                              String depTime, int enrollmentID) throws Exception {
        URL url = new URL("https://radius.mathnasium.com/Attendance/CreateAttendanceStudentDetails");
        String json = "{" +
                "\"studentId\": \"" + studentID + "\"," +
                "\"attendanceDate\": \"" + attDate + "\"," +
                "\"arrivalTime\": \"" + arrTime +"\"," +
                "\"departureTime\": \"" + depTime + "\"," +
                "\"duration\": 0," +
                "\"enrollmentId\": " + enrollmentID + "," +
                "\"SupplementalAttendanceId\": null" +
                "}";
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
            if (array.size() == 0) {
                Platform.runLater(() -> console.setText(console.getText() + "\nSUCCESS: Added attendance!"));
                System.out.print("SUCCESS: ");
                return true;
            }
            else {
                String err = array.getJsonObject(0).getString("Message");
                Platform.runLater(() -> console.setText(console.getText() + "\nERROR: " + err));
                System.out.println("ERROR: " + err);
                return false;
            }
        }
        else {
            Platform.runLater(() -> console.setText(console.getText() + "\nERROR: Could not make connection! "));
            System.out.println("ERROR: Could not make connection!");
            return false;
        }
    }

    public boolean validateAttendance(String studentID, LocalDate date,
            String startTime, String endTime) throws Exception {
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
                if (array.size() > 0) {
                    for (int i = 0; i < array.size(); i++) {
                        String jsonDate = array.getJsonObject(i).getString("AttendanceDateString");
                        String jsonStartTime = array.getJsonObject(i).getString("ArrivalTimeString");
                        String jsonEndTime = array.getJsonObject(i).getString("DepartureTimeString");
                        if (date.format(cellDateFormat).equals(jsonDate)) {
                            if (jsonStartTime.equals(startTime) && jsonEndTime.equals(endTime)) {
                                Platform.runLater(() -> console.setText(console.getText() + "\nSUCCESS: Attendance exists!"));
                                System.out.println("SUCCESS: Attendance exists!");
                                return true;
                            }
                            else {
                                Platform.runLater(() -> console.setText(console.getText() + "\nERROR: Student info mismatch! "));
                                System.out.println("ERROR: Student info mismatch!");
                                return false;
                            }
                        }
                    }
                    return false;
                }
                else {
                    Platform.runLater(() -> console.setText(console.getText() + "\nERROR: Enrollment not found! "));
                    System.out.println("ERROR: Enrollment not found!");
                    return false;
                }
            }
            else {
                Platform.runLater(() -> console.setText(console.getText() + "\nERROR: Enrollment not found! "));
                System.out.println("ERROR: Enrollment not found!");
                return false;
            }
        }
        else {
            Platform.runLater(() -> console.setText(console.getText() + "\nERROR: Could not make connection! "));
            System.out.println("ERROR: Could not make connection!");
            return false;
        }
    }

    public void showMainStage() throws Exception{
        Stage primaryStage = new Stage();
        primaryStage.setTitle("Attendance Automation");
        GridPane grid = new GridPane();
        ScrollPane sp = new ScrollPane(grid);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));

        Text sceneTitle = new Text("Attendance Automation");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(sceneTitle, 0, 0, 2, 1);

//        Label radiusUser = new Label("Radius username:");
//        grid.add(radiusUser, 0, 1);
//        TextField radiusUserTextField = new TextField("");
//        grid.add(radiusUserTextField, 1, 1);
//
//        Label radiusPW = new Label("Radius password:");
//        grid.add(radiusPW, 0, 2);
//        PasswordField radiusPWTextField = new PasswordField();
//        grid.add(radiusPWTextField, 1, 2);

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
                        ArrayList<HashMap<String, Student>> studentsList = parseSheet(arrival, startDate, endDate); // Parse through Excel
                        FileInputStream input = new FileInputStream("student.properties");
                        prop = new Properties();
                        prop.load(input);
                        input.close();

                        output = new FileOutputStream("student.properties");
                        int itr = 0;
                        int total = 0;
                        for (int i = 0; i < studentsList.size(); i++) {
                            // Get total rows
                            total += studentsList.get(i).size();
                        }
                        for (int i = 0; i < studentsList.size(); i++) {
                            HashMap<String, Student> students = studentsList.get(i);
                            LocalDate date = startDate.plusDays(i);
                            for (String key : students.keySet()) {
                                Student student = students.get(key);
                                updateProgress(itr, total);
                                Platform.runLater(() -> status.setValue("Adding attendance for " + student.name +
                                        " on " + cellDateFormat.format(date)));
                                System.out.println("Adding attendance for " + student.name +
                                        " on " + cellDateFormat.format(date));
                                updateMessage(percentage.format((double) itr / total));

                                if (student.name.equals("WRONG FORMAT")) {
                                    setStatus(student.name, date, false, sheet, arrival, key);
                                    itr++;
                                    continue;
                                } else if (prop.get(student.name) != null) {
                                    if (prop.get(student.name).equals("null")) {
                                        System.out.println("ERROR: Student ID could not be found");
                                        setStatus(student.name, date, false, sheet, arrival, key);
                                        itr++;
                                        continue;
                                    } else {
                                        System.out.print("Student key/pair exists! ");
                                        System.out.println(student.name + "=" + prop.getProperty(student.name));
                                        Platform.runLater(() -> console.setText("Student key/pair exists! "));
                                        Platform.runLater(() -> console.setText(console.getText() + student.name + "=" + prop.getProperty(student.name)));
                                        // Success
                                    }
                                } else {
                                    String studentID = searchStudent(student.name);
                                    prop.setProperty(student.name, studentID);
                                    if (studentID.equals("null")) {
                                        setStatus(student.name, date, false, sheet, arrival, key);
                                        itr++;
                                        continue;
                                    }
                                }

                                if (validateAttendance(prop.getProperty(student.name), date,
                                        student.startTime, student.endTime)) {
                                    // Check if attendance already exists
                                    setStatus(student.name, date, true, sheet, arrival, key);
                                    itr++;
                                    continue;
                                }

                                int enrollmentID = getEnrollmentID(prop.getProperty(student.name));
                                if (enrollmentID < 0) {
                                    setStatus(student.name, date, false, sheet, arrival, key);
                                    itr++;
                                    continue;
                                } else if (enrollmentID == 0) {
                                    setStatus(student.name, date, true, sheet, arrival, key); // Checks off on Excel but does not attempt to add on Radius
                                    itr++;
                                    continue;
                                } else {
                                    LocalTime arrTime = LocalTime.parse(student.startTime, timeFormatter);
                                    LocalTime depTime = LocalTime.parse(student.endTime, timeFormatter);
                                    LocalDateTime dateTime = date.atTime(5, 0, 0, 0);
                                    LocalDateTime arrDateTime = date.atTime(arrTime);
                                    LocalDateTime depDateTime = date.atTime(depTime);
                                    String jsonDate1 = dateTime.format(DateTimeFormatter.ISO_DATE_TIME) + ".000Z";
                                    String jsonDate2 = arrDateTime.plusHours(5).format(DateTimeFormatter.ISO_DATE_TIME) + ".000Z";
                                    String jsonDate3 = depDateTime.plusHours(5).format(DateTimeFormatter.ISO_DATE_TIME) + ".000Z";
                                    // Three attempts to add attendance
                                    boolean attendanceAdded = false;
                                    for (int j = 0; j < 3; j++) {
                                        if (addAttendance(prop.getProperty(student.name), jsonDate1, jsonDate2, jsonDate3, enrollmentID)) {
                                            // Success -> validate entry
//                                            Thread.sleep(69);
                                            if (validateAttendance(prop.getProperty(student.name), date,
                                                    student.startTime, student.endTime)) {
                                                // Success -> add check mark!
                                                attendanceAdded = true;
                                                break;
                                            }
                                        }
                                    }

                                    if (attendanceAdded) {
                                        Platform.runLater(() -> status.setValue("Success! Added attendance for " + student.name));
                                        setStatus(student.name, date, true, sheet, arrival, key);
                                        itr++;
                                    }
                                    else {
                                        Platform.runLater(() -> status.setValue("Failed to add attendance for " + student.name));
                                        setStatus(student.name, date, false, sheet, arrival, key);
                                        itr++;
                                    }
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
                    alert.setContentText("Please restart automation. Runtime of " +
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
                    alert.setContentText("Completed attendance automation with " + succs +
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

    public void fillInCells(Row row, CellStyle backgroundStyle) {
        for (int i = 0; i <= 9; i++) {
            row.getCell(i).setCellStyle(backgroundStyle);
        }
    }

    public void setStatus(String studentName, LocalDate date, boolean attendanceAdded,
                          XSSFSheet sheet, File arrival, String key) throws IOException {
        Iterator<Row> rowIt = sheet.iterator();
        rowIt.next();
        fos = new FileOutputStream(arrival);
        while (rowIt.hasNext()) {
            Row row = rowIt.next();
            if (row.getCell(3).toString().equals(key)) {
                if (LocalDate.parse(row.getCell(0).toString(),cellDateFormat).equals(date)) {
                    row.createCell(9);
                    if (attendanceAdded) {
                        System.out.println("Added attendance for " + studentName);
                        row.getCell(9).setCellValue("\u2714");
                        fillInCells(row, successCell);
                        succs++;
                    } else {
                        row.getCell(9).setCellValue("\u2718");
                        fillInCells(row, failCell);
                        fails++;
                    }
                }
            }
        }
        workbook.write(fos);
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
