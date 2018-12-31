import javafx.application.Application;
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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

public class Attendance extends Application {
    String currentDir = System.getProperty("user.dir");
    String PATH = currentDir + "\\reports";
    boolean isLoggedIn = false;

    class Student {
        String name;
        String startDate;
        String endDate;
        String timeFrame;
        SimpleDateFormat parser = new SimpleDateFormat("HH:mm a");
        public Student(String n, String sd, String ed, String tf) {
            name = n;
            startDate = sd;
            endDate = ed;
            timeFrame = tf;
        }

        public void addTime(String time) throws ParseException {
            Date parseTime = parser.parse(time);
            Date parseSD = parser.parse(startDate);
            Date parseED = parser.parse(endDate);
            if (parseTime.before(parseSD) && parseTime.before(parseED)) {
                startDate = time;
            }
            else if (parseTime.after(parseSD) && parseTime.after(parseED)) {
                endDate = time;
            }
        }

        public void correctTime() {
            String parseTime[] = timeFrame.split("-");
            if (startDate.equals(endDate)) {
                startDate = parseTime[0];
                endDate = parseTime[1];
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

    public void automation(ChromeDriver driver, String user, String pass, String startDate, String endDate) throws IOException, ParseException, InterruptedException {
        File dir1 = new File(PATH);
//        File dir1 = new File("C:\\Users\\Mathnasium\\Downloads");

        File[] foundFiles = dir1.listFiles((dir, name) -> name.startsWith("Client Arrivals Report " + startDate + " - " + endDate));
//        File[] foundFiles = dir1.listFiles((dir, name) -> name.startsWith("Client Arrivals Report "));
        File arrival = foundFiles[0];
        for (File f : foundFiles) {
            if (f.lastModified() > arrival.lastModified())
                arrival = f;
        }
        System.out.println("FOUND: " + arrival.getName());

        HashMap<String, Student> students = new HashMap<>();

        FileInputStream fis = new FileInputStream(arrival);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
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

            if (!students.containsKey(id))
                students.put(id, new Student(name, checkIn, checkIn, timeFrame));
            else students.get(id).addTime(checkIn);
        }

        workbook.close();
        fis.close();

        for (String key : students.keySet()) {
            Student student = students.get(key);
            student.correctTime();
            student.correctName();
            System.out.println(student.name + " " + student.startDate + " " + student.endDate);
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

            WebElement attendanceBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("AddAttendanceBtn")));
            attendanceBtn.click();
            Thread.sleep(2000); // wait for enrollment to load
            WebElement openDropdown = driver.findElement(By.cssSelector(".col-md-8 > .searchGridDropDown"));
            WebElement select = driver.findElement(By.cssSelector("#EnrollmentDropDown_listbox"));
            openDropdown.click();
            select.click();

            WebElement date = driver.findElement(By.id("AttendanceDate"));
            if (date.getAttribute("value").equals("")) {
                date.sendKeys(startDate);
                date.click();
            }
            WebElement timeIn = driver.findElement(By.id("ArrivalTime"));
            timeIn.clear();
            timeIn.sendKeys(student.startDate);
            WebElement timeOut = driver.findElement(By.id("DepartureTime"));
            timeOut.clear();
            timeOut.sendKeys(student.endDate);

//            WebElement cancel = driver.findElement(By.cssSelector(".col-md-12 > .popUp > .head > .bigX"));
//            cancel.click();
//            Thread.sleep(2000);

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
        SimpleDateFormat formatter1 = new SimpleDateFormat("MM/dd/yyy");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1); // YESTERDAY
        attendanceDate1.getEditor().setText(formatter1.format(calendar.getTime()));
        grid.add(attendanceDate1, 1, 3);

        Label attendance2 = new Label("End Date:");
        grid.add(attendance2, 0, 4);
        DatePicker attendanceDate2 = new DatePicker();
        attendanceDate2.getEditor().setText(formatter1.format(calendar.getTime()));
        grid.add(attendanceDate2, 1, 4);

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
            ChromeDriver driver = ChromeDriverSingleton.getInstance();

            String startDate = attendanceDate1.getEditor().getText().replaceAll("/","-");
            String endDate = attendanceDate2.getEditor().getText().replaceAll("/","-");
            System.out.println("Start: " + startDate);
            System.out.println("End: " + endDate);
            try {
                automation(driver, radiusUserTextField.getText(), radiusPWTextField.getText(), startDate, endDate);
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Something went wrong!");
                alert.setContentText("Please restart automation.\n");
                alert.getDialogPane().setExpandableContent(new ScrollPane(new TextArea(e.toString())));
                alert.showAndWait();

                e.printStackTrace();
                driver.get("https://radius.mathnasium.com");
            }
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
}