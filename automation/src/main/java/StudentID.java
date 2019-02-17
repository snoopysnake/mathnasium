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
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.json.Json;
import javax.json.JsonArray;
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
public class StudentID extends Application {
    ChromeDriver driver;
    WebDriverWait wait;
    XSSFWorkbook workbook;
    XSSFSheet sheet;
    FileOutputStream fos = null;
    OutputStream output = null;
    Properties prop;
    String currentDir = System.getProperty("user.dir");
    String PATH = currentDir + "\\reports";
    boolean isLoggedIn = false;
    boolean canExit = true;
    CellStyle successCell;
    CellStyle failCell;
    DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("M-d-yyyy");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
    DecimalFormat truncate = new DecimalFormat("#.##");
    DecimalFormat percentage = new DecimalFormat("#%");
    TextArea console;

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
            name = name.replaceAll("[A-Z]{2,}", ""); // removes all caps words
            name = name.replaceAll("\\+", ""); // removes plus sign
            name = name.replaceAll("[0-9]", ""); // removes numbers
            name = name.replaceAll("/", ""); // removes stray forward slash
            String splitName[] = name.split(", ");

            if (splitName.length > 0) {
                splitName[0] = splitName[0].replaceAll("\\(.*\\)", "").trim(); // removes parentheses
                splitName[1] = splitName[1].replaceAll("\\(.*\\)", "").trim();
                name = splitName[1] + " " + splitName[0];
            }
            else name = "WRONG FORMAT";
        }
    }

    public HashMap<String, Student> parseSheet(File arrival) throws IOException, ParseException {
        HashMap<String, Student> students = new HashMap<>();
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
            String timeFrame = row.getCell(2).toString();
            String id = row.getCell(3).toString();
            String name = row.getCell(4).toString();
            String checkIn = row.getCell(5).toString();
            if ((row.getCell(9) != null && !row.getCell(9).toString().equals("\u2714") &&
                    row.getCell(9).toString().equals("\u2718")) || row.getCell(9) == null) {
                if (!students.containsKey(id))
                    students.put(id, new Student(name, checkIn, checkIn, timeFrame));
                else students.get(id).addTime(checkIn);
            }

//            if (!students.containsKey(id))
//                students.put(id, new Student(name, checkIn, checkIn, timeFrame));
//            else students.get(id).addTime(checkIn);
        }
        fis.close();

        for (String key : students.keySet()) {
            Student student = students.get(key);
            student.correctTime();
            student.correctName();
//            System.out.println(student.name + " " + student.startTime + " " + student.endTime);  // Excel log info
        }
        return students;
    }

    public void login(String user, String pass) {
        if (!isLoggedIn) {
            Platform.runLater(() -> console.setText(console.getText() + "\nLogging in to Radius..."));
            driver = ChromeDriverSingleton.getInstance(); // singleton
            wait = new WebDriverWait(driver,10);
            driver.get("http://radius.mathnasium.com");
            WebElement radiusUser = driver.findElement(By.id("UserName"));
            radiusUser.sendKeys(user);
            WebElement radiusPass = driver.findElement(By.id("Password"));
            radiusPass.sendKeys(pass);
            radiusPass.submit();
            WebElement signedIn = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".m-l-md")));
            if (signedIn.getText().toLowerCase().equals("signed in as "+user)) {
                Platform.runLater(() -> console.setText(console.getText() + "\nSigned in as "+user));
                System.out.println(signedIn.getText());
                isLoggedIn = true;
            }
        }
    }

    public boolean searchStudent(String studentName) throws InterruptedException, IOException {
        Platform.runLater(() -> console.setText("Searching for " + studentName + "..."));
        System.out.print("Finding student ID for " + studentName + "... ");
        WebElement searchIcon = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("SearchIcon")));
        searchIcon.click();
        WebElement searchBar = wait.until(ExpectedConditions.elementToBeClickable(By.id("ContactSearch")));
        Thread.sleep(500); // In case name is input incorrectly
        searchBar.sendKeys(studentName);
        searchBar.sendKeys(Keys.ENTER);

        WebElement searchBtn = driver.findElement(By.id("globalbtnsearch"));
        searchBtn.click();
        wait.until(ExpectedConditions.elementToBeClickable(By.id("globalbtnsearch")));
        List<WebElement> loadingMask = driver.findElements(By.cssSelector(".k-loading-mask"));
        for (WebElement e : loadingMask) {
            wait.until(ExpectedConditions.invisibilityOf(e));
        }

        List<WebElement> gridSearch = driver.findElements(By.cssSelector("#gridSearch >div > table > tbody > tr"));
        if (gridSearch.size() == 1) {
            WebElement studentHref = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("table.k-selectable > tbody > tr:first-child > td:nth-child(3) > a")));
            studentHref.click();
            String splitURL[] =  driver.getCurrentUrl().split("/");
            prop.setProperty(studentName, splitURL[splitURL.length - 1]);
            Platform.runLater(() -> console.setText("SUCCESS: Key/pair created! " + studentName+"="+splitURL[splitURL.length - 1]));
            System.out.println(studentName+"="+splitURL[splitURL.length - 1]);
            return true;
        }
        else {
            Platform.runLater(() -> console.setText(console.getText() + "\nERROR: Multiple or no results for " + studentName));
            System.out.println("ERROR: Multiple or no results for " + studentName);
            prop.setProperty(studentName, "null");
            WebElement cancel = driver.findElement(By.cssSelector("#SearchModal > .m-b-sm > .btn"));
            cancel.click();
            Thread.sleep(1000);
            return false;
        }
    }

    public int getEnrollmentID(String studentName, String studentID, String cookie) throws Exception {
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
                    Platform.runLater(() -> console.setText("SUCCESS: Found enrollment ID: " + enrollmentID));
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
            Platform.runLater(() -> console.setText(console.getText() + "ERROR: Enrollment not found! "));
            System.out.println("ERROR: Enrollment not found!");
            return -1;
        }
    }

    public void addAttendance(String studentID, String attDate, String arrTime,
                              String depTime, int enrollmentID, String cookie) throws Exception {
        URL url = new URL("https://radius.mathnasium.com/Student/CreateAttendance");
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
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("cookie", cookie);
        conn.setDoOutput(true);
        conn.getOutputStream().write(json.getBytes("UTF-8"));
        if (conn.getResponseCode() == 200) {
            Platform.runLater(() -> console.setText(console.getText() + "\nSUCCESS: Added attendance!"));
            System.out.print("SUCCESS! ");
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

        Label radiusUser = new Label("Radius username:");
        grid.add(radiusUser, 0, 1);
        TextField radiusUserTextField = new TextField("");
        grid.add(radiusUserTextField, 1, 1);

        Label radiusPW = new Label("Radius password:");
        grid.add(radiusPW, 0, 2);
        PasswordField radiusPWTextField = new PasswordField();
        grid.add(radiusPWTextField, 1, 2);

        Label attendance1 = new Label("Start Date:");
        grid.add(attendance1, 0, 3);
        DatePicker attendanceDate1 = new DatePicker();
        attendanceDate1.setValue(LocalDate.now().minusDays(1));
        attendanceDate1.getEditor().setDisable(true);
        attendanceDate1.setStyle("-fx-opacity: 1");
        attendanceDate1.getEditor().setStyle("-fx-opacity: 1");
        grid.add(attendanceDate1, 1, 3);

//        Label attendance2 = new Label("End Date:");
//        grid.add(attendance2, 0, 4);
//        DatePicker attendanceDate2 = new DatePicker();
//        attendanceDate2.setValue(LocalDate.now().minusDays(1));
//        grid.add(attendanceDate2, 1, 4);

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
        dps.setMaxHeight(100);
        dps.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        dps.setFitToHeight(true);
        progressGrid.add(dps, 0,3);
        progressStage.setScene(new Scene(progressGrid, 350, 250));
//        progressStage.setAlwaysOnTop(true);
        StringProperty status = new SimpleStringProperty();
        status.setValue("");

        Button btn = new Button("Start");
        btn.setOnAction(event -> {
            File arrival = validateExcelFile(attendanceDate1.getValue(), attendanceDate1.getValue());
            if (arrival != null && arrival.exists()) {

//            System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "\\src\\chromedriver.exe");
//            driver = new ChromeDriver();
                long startTime = System.currentTimeMillis();
                Task task = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        HashMap<String, Student> students = parseSheet(arrival); // Parse through Excel
                        LocalDate startDate = attendanceDate1.getValue();
                        FileInputStream input = new FileInputStream("student.properties");
                        prop = new Properties();
                        prop.load(input);
                        input.close();

                        output = new FileOutputStream("student.properties");
                        int itr = 0;
                        for (String key : students.keySet()) {
                            Student student = students.get(key);
                            updateProgress(itr, students.size());
                            Platform.runLater(() -> status.setValue("Adding attendance for " + student.name));
                            updateMessage(percentage.format((double) itr/students.size()));

                            if (student.name.equals("WRONG FORMAT")) {
                                setStatus(student.name, false, sheet, arrival, key);
                                itr++;
                                continue;
                            }
                            else if (prop.get(student.name) != null) {
                                if (prop.get(student.name).equals("null")) {
                                    setStatus(student.name, false, sheet, arrival, key);
                                    itr++;
                                    continue;
                                }
                                else {
                                    System.out.print("Student key/pair exists! ");
                                    System.out.println(student.name + "=" + prop.getProperty(student.name));
                                    Platform.runLater(() -> console.setText(console.getText() + "\nStudent key/pair exists! "));
                                    Platform.runLater(() -> console.setText(console.getText() + student.name + "=" + prop.getProperty(student.name)));
                                    // Success
                                }
                            }
                            else {
                                // Login to Radius
                                login(radiusUserTextField.getText(), radiusPWTextField.getText());
                                if (!searchStudent(student.name)) {
                                    setStatus(student.name, false, sheet, arrival, key); // Search for student ID
                                    itr++;
                                    continue;
                                }
                            }
                            Platform.runLater(() -> status.setValue("Adding attendance for " + student.name));

                            String cookie = "";
                            int enrollmentID = getEnrollmentID(student.name, prop.getProperty(student.name), cookie);
                            if (enrollmentID < 0) {
                                setStatus(student.name, false, sheet, arrival, key);
                                itr++;
                                continue;
                            }
                            else if (enrollmentID == 0) {
                                setStatus(student.name, true, sheet, arrival, key); // Checks off on Excel but does not attempt to add on Radius
                                itr++;
                                continue;
                            }
                            else {
                                LocalTime arrTime = LocalTime.parse(student.startTime, timeFormatter);
                                LocalTime depTime = LocalTime.parse(student.endTime, timeFormatter);
                                LocalDateTime dateTime = startDate.atTime(5,0,0,0);
                                LocalDateTime arrDateTime = startDate.atTime(arrTime.plusHours(5));
                                LocalDateTime depDateTime = startDate.atTime(depTime.plusHours(5));
                                String jsonDate1 = dateTime.format(DateTimeFormatter.ISO_DATE_TIME)+".000Z";
                                String jsonDate2 = arrDateTime.format(DateTimeFormatter.ISO_DATE_TIME)+".000Z";
                                String jsonDate3 = depDateTime.format(DateTimeFormatter.ISO_DATE_TIME)+".000Z";
                                addAttendance(prop.getProperty(student.name), jsonDate1, jsonDate2, jsonDate3, enrollmentID, cookie);
                                // Success -> add check mark!
                                Platform.runLater(() -> status.setValue("Success! Added attendance for " + student.name));
                                setStatus(student.name, true, sheet, arrival, key);
                                itr++;
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
                    if (driver != null)
                        driver.get("https://radius.mathnasium.com"); // singleton
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
                    alert.setContentText("Completed attendance automation. Runtime of " +
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
//                driver.close();
                progressStage.close();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage initStage) throws Exception {
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
//        System.out.println("Start: " + startDate.format(dateFormat)); // Excel log info
//            System.out.println("End: " + endDate.format(dateFormat));

        File dir1 = new File(PATH);
//        File dir1 = new File("C:\\Users\\Mathnasium\\Downloads");
        System.out.println(dir1);
        if (!dir1.exists()) {
            return null;
        }
        File[] foundFiles = dir1.listFiles((dir, name) -> name.startsWith("Client Arrivals Report " +
                startDate.format(dateFormat) + " - " + endDate.format(dateFormat)));
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

    public void setStatus(String studentName, boolean attendanceAdded,
                          XSSFSheet sheet, File arrival, String key) throws IOException {
        Iterator<Row> rowIt = sheet.iterator();
        rowIt.next();
        fos = new FileOutputStream(arrival);
        while (rowIt.hasNext()) {
            Row row = rowIt.next();
            if (row.getCell(3).toString().equals(key)) {
                row.createCell(9);
                if (attendanceAdded) {
                    System.out.println("Added attendance for " + studentName);
                    row.getCell(9).setCellValue("\u2714");
                    fillInCells(row, successCell);
                } else {
                    row.getCell(9).setCellValue("\u2718");
                    fillInCells(row, failCell);
                }
            }
        }
        workbook.write(fos);

    }
}
