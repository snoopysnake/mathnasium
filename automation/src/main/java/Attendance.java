import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class Attendance extends Application {
    XSSFWorkbook workbook;
    FileOutputStream fos;
    String currentDir = System.getProperty("user.dir");
    String PATH = currentDir + "\\reports";
    boolean isLoggedIn = false;

    class Student {
        String name;
        String startTime;
        String endTime;
        String timeFrame;
        SimpleDateFormat parser = new SimpleDateFormat("HH:mm a");
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
                System.out.print("CORRECTED: ");
            }
        }

        public void correctName() {
            name = name.replaceAll("END", ""); // removes "END"
            name = name.replaceAll("MANUAL", ""); // removes "END"
            name = name.replaceAll("\\+", ""); // removes plus sign
            name = name.replaceAll("[0-9]", ""); // removes number
            String splitName[] = name.split(", ");
            splitName[0] = splitName[0].replaceAll("\\(.*\\)", "").trim(); // removes parentheses
            splitName[1] = splitName[1].replaceAll("\\(.*\\)", "").trim();
            name = splitName[1] + " " + splitName[0];
        }
    }

    public void automation(ChromeDriver driver, String user, String pass, LocalDate startDate, LocalDate endDate) throws IOException, ParseException, InterruptedException {
        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("MM-dd-yyyy");
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("MM/d/yyyy");
        DateTimeFormatter formatterM = DateTimeFormatter.ofPattern("M");
        DateTimeFormatter formatterD = DateTimeFormatter.ofPattern("d");
        DateTimeFormatter formatterY = DateTimeFormatter.ofPattern("yyyy");
        System.out.println("Start: " + startDate.format(formatter1));
//            System.out.println("End: " + endDate.format(formatter1));

        File dir1 = new File(PATH);
//        File dir1 = new File("C:\\Users\\Mathnasium\\Downloads");
        File[] foundFiles = dir1.listFiles((dir, name) -> name.startsWith("Client Arrivals Report " + startDate.format(formatter1) + " - " + endDate.format(formatter1)));
//        File[] foundFiles = dir1.listFiles((dir, name) -> name.startsWith("Client Arrivals Report "));
        File arrival = foundFiles[0];
        for (File f : foundFiles) {
            if (f.lastModified() > arrival.lastModified())
                arrival = f;
        }
        System.out.println("FOUND: " + arrival.getName());

        HashMap<String, Student> students = new HashMap<>();

        FileInputStream fis = new FileInputStream(arrival);
        workbook = new XSSFWorkbook(fis);
        XSSFSheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIt = sheet.iterator();
        rowIt.next();
        while (rowIt.hasNext()) {
            Row row = rowIt.next();
            String timeFrame = row.getCell(2).toString();
            String id = row.getCell(3).toString();
            String name = row.getCell(4).toString();
            String checkIn = row.getCell(5).toString();

            System.out.print("Time Frame: " + timeFrame);
            System.out.print(", ID: " + id);
            System.out.print(", Name: " + name);
            System.out.println(", Check in Time: " + checkIn);

            if ((row.getCell(9) != null && !row.getCell(9).toString().equals("\u2713")) ||
                    row.getCell(9) == null) {
                if (!students.containsKey(id))
                    students.put(id, new Student(name, checkIn, checkIn, timeFrame));
                else students.get(id).addTime(checkIn);
            }
        }

        fis.close();

        for (String key : students.keySet()) {
            Student student = students.get(key);
            student.correctTime();
            student.correctName();
            System.out.println(student.name + " " + student.startTime + " " + student.endTime);
        }

        WebDriverWait wait = new WebDriverWait(driver,6000);
        driver.get("http://radius.mathnasium.com");

        if (!isLoggedIn) {
            WebElement radiusUser = driver.findElement(By.id("UserName"));
            radiusUser.sendKeys(user);
            WebElement radiusPass = driver.findElement(By.id("Password"));
            radiusPass.sendKeys(pass);
            radiusPass.submit();
            isLoggedIn = true;
        }

        for (String key : students.keySet()) {
            Student student = students.get(key);
            WebElement searchIcon = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("SearchIcon")));
            searchIcon.click();
            WebElement searchBar = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ContactSearch")));
            searchBar.sendKeys(student.name);
            WebElement searchBtn = driver.findElement(By.id("globalbtnsearch"));
            searchBtn.click();
            WebElement studentHref = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("table.k-selectable > tbody > tr:first-child > td:nth-child(3) > a")));
            studentHref.click();

            // TODO: Check if date's attendance has already been added.
            List<WebElement> attendanceGrid = driver.findElements(By.cssSelector("#attendanceGrid > table > tbody > tr > td:nth-child(3)"));
            if (!attendanceGrid.isEmpty() && attendanceGrid.get(0).getText().equals(startDate.format(formatter2))) {
                System.out.println("Attendance already input for " + student.name + "!");
            }
            else {
                WebElement attendanceBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("AddAttendanceBtn")));
                attendanceBtn.click();
                Thread.sleep(5000);
                WebElement openDropdown = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".col-md-8 > .searchGridDropDown")));
                openDropdown.click();
                WebElement select = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#EnrollmentDropDown_listbox")));
                select.click();

                WebElement date = driver.findElement(By.id("AttendanceDate"));
                date.clear();
                date.sendKeys(Keys.ARROW_LEFT);
                date.sendKeys(startDate.format(formatterM));
                date.sendKeys(Keys.ARROW_LEFT);
                date.sendKeys(startDate.format(formatterM)); // Needs to re-input due to website code
                date.sendKeys(Keys.ARROW_RIGHT);
                date.sendKeys(startDate.format(formatterD));
                date.sendKeys(Keys.ARROW_RIGHT);
                date.sendKeys(startDate.format(formatterY));
                date.click();
                WebElement timeIn = driver.findElement(By.id("ArrivalTime"));
                timeIn.clear();
                timeIn.sendKeys(student.startTime);
                WebElement timeOut = driver.findElement(By.id("DepartureTime"));
                timeOut.clear();
                timeOut.sendKeys(student.endTime);

//                WebElement saveAttendance = driver.findElement(By.id("SaveAttendanceBtn"));
//                saveAttendance.click();
//                Thread.sleep(2500);

//                    WebElement cancel = driver.findElement(By.cssSelector(".col-md-12 > .popUp > .head > .bigX"));
//                    cancel.click();
//                    Thread.sleep(2500);
            }

            rowIt = sheet.iterator();
            rowIt.next();
            fos = new FileOutputStream(arrival);
            while (rowIt.hasNext()) {
                Row row = rowIt.next();
                if (row.getCell(3).toString().equals(key)) {
                    System.out.println("Added attendance for " + student.name);
                    row.createCell(9);
                    row.getCell(9).setCellValue("\u2713");
                }
            }
            workbook.write(fos);
            Thread.sleep(5000);
        }
    }

    public void start(Stage primaryStage) throws Exception{
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
            PATH = directoryChooser.showDialog(primaryStage).getPath();
            System.out.println("Path changed to: " + PATH);
        });
        grid.add(dirBtn, 1, 5);

        Button btn = new Button("Start");
        btn.setOnAction(event -> {
//            btn.setDisable(true);
//            System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "\\src\\chromedriver.exe");
//            ChromeDriver driver = new ChromeDriver();
            ChromeDriver driver = ChromeDriverSingleton.getInstance(); // singleton

            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    automation(driver, radiusUserTextField.getText(), radiusPWTextField.getText(), attendanceDate1.getValue(), attendanceDate1.getValue());
                    return null;
                }
            };
            task.setOnFailed(event1 -> {
                btn.setDisable(false);
                Exception e = (Exception) task.getException();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Something went wrong!");
                alert.setContentText("Please restart automation.\n");
                alert.getDialogPane().setExpandableContent(new ScrollPane(new TextArea(e.toString())));
                alert.showAndWait();

                e.printStackTrace();
                driver.get("https://radius.mathnasium.com"); // singleton
                try {
                    workbook.write(fos);
                    fos.close();
                    workbook.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });
            task.setOnCancelled(event2 -> {
                btn.setDisable(false);
                try {
                    workbook.write(fos);
                    fos.close();
                    workbook.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });
            task.setOnSucceeded(event3 -> {
                btn.setDisable(false);
                try {
                    workbook.write(fos);
                    fos.close();
                    workbook.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });
            new Thread(task).start();

        });
        HBox hbBtn = new HBox(10);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 11);
        primaryStage.setScene(new Scene(sp, 330, 330));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() throws IOException {
        System.out.println("Saving and closing...");
        if (fos != null && workbook != null) {
            workbook.write(fos);
            fos.close();
            workbook.close();
        }
    }

}