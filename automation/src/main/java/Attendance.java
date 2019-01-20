import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
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

import java.io.*;
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
    CellStyle successCell;
    CellStyle failCell;

    class Student {
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
            else name = "This student's name was improperly formatted.";
        }
    }

    public void automation(ChromeDriver driver, File arrival, String user, String pass, LocalDate startDate, LocalDate endDate) throws IOException, ParseException, InterruptedException {
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("M/d/yyyy");

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

        XSSFSheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIt = sheet.iterator();
        rowIt.next();
        while (rowIt.hasNext()) {
            Row row = rowIt.next();
            String timeFrame = row.getCell(2).toString();
            String id = row.getCell(3).toString();
            String name = row.getCell(4).toString();
            String checkIn = row.getCell(5).toString();

//            System.out.print("Time Frame: " + timeFrame); // Excel log info
//            System.out.print(", ID: " + id); // Excel log info
//            System.out.print(", Name: " + name); // Excel log info
//            System.out.println(", Check in Time: " + checkIn); // Excel log info

            if ((row.getCell(9) != null && !row.getCell(9).toString().equals("\u2714") &&
                    !row.getCell(9).toString().equals("\u2718")) || row.getCell(9) == null) {
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
//            System.out.println(student.name + " " + student.startTime + " " + student.endTime);  // Excel log info
        }

        WebDriverWait wait = new WebDriverWait(driver,10);
        driver.get("http://radius.mathnasium.com");

        if (!isLoggedIn) {
            WebElement radiusUser = driver.findElement(By.id("UserName"));
            radiusUser.sendKeys(user);
            WebElement radiusPass = driver.findElement(By.id("Password"));
            radiusPass.sendKeys(pass);
            radiusPass.submit();
//            WebElement wrongPass = driver.findElement(By.cssSelector(".validation-summary-errors > ul > li"));
        }

        for (String key : students.keySet()) {
            Student student = students.get(key);
            WebElement searchIcon = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("SearchIcon")));
            isLoggedIn = true;
            searchIcon.click();
            WebElement searchBar = wait.until(ExpectedConditions.elementToBeClickable(By.id("ContactSearch")));
            Thread.sleep(250);
            searchBar.sendKeys(student.name);
            System.out.println(searchBar.getText());

            WebElement searchBtn = driver.findElement(By.id("globalbtnsearch"));
            searchBtn.click();
            wait.until(ExpectedConditions.elementToBeClickable(By.id("globalbtnsearch")));
            List<WebElement> loadingMask = driver.findElements(By.cssSelector(".k-loading-mask"));
            for (WebElement e : loadingMask) {
                wait.until(ExpectedConditions.invisibilityOf(e));
            }

//            WebElement searchResult = driver.findElement(By.cssSelector("#gridSearch > .k-pager-wrap > .k-pager-info"));
//            if (!searchResult.getText().equals("No items to display")) {
//                WebElement studentHref = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("table.k-selectable > tbody > tr:first-child > td:nth-child(3) > a")));
//                studentHref.click();
//            }

            List<WebElement> gridSearch = driver.findElements(By.cssSelector("#gridSearch >div > table > tbody > tr"));
            if (gridSearch.size() == 1) {
                WebElement studentHref = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("table.k-selectable > tbody > tr:first-child > td:nth-child(3) > a")));
                studentHref.click();
                System.out.println("Adding attendance for " + student.name + "...");
            }
            else {
                System.out.println("No results - name does not match for " + student.name);
                rowIt = sheet.iterator();
                rowIt.next();
                fos = new FileOutputStream(arrival);
                while (rowIt.hasNext()) {
                    Row row = rowIt.next();
                    if (row.getCell(3).toString().equals(key)) {
                        row.createCell(9);
                        row.getCell(9).setCellValue("\u2718");
                        fillInCells(row, failCell);
                    }
                }
                workbook.write(fos);

                WebElement cancel = driver.findElement(By.cssSelector("#SearchModal > .m-b-sm > .btn"));
                cancel.click();
                Thread.sleep(1000);
                continue;
            }

            loadingMask = driver.findElements(By.cssSelector(".k-loading-mask"));
            for (WebElement e : loadingMask) {
                wait.until(ExpectedConditions.invisibilityOf(e));
            }
            Thread.sleep(500);
            List<WebElement> attendanceGridDates = driver.findElements(By.cssSelector("#attendanceGrid > table > tbody > tr > td:nth-child(3)"));
            boolean attendanceAdded;
            if (!attendanceGridDates.isEmpty() && attendanceGridDates.get(0).getText().equals(startDate.format(formatter2))) {
                attendanceAdded = true;
                System.out.println("Attendance already input for " + student.name + "!");
            }
            else {
                try {
                    attendanceAdded = selectDropdown(wait, driver, student, startDate);
                } catch (Exception e) {
                    e.printStackTrace();
//                    WebElement cancel = driver.findElement(By.cssSelector(".col-md-12 > .popUp > .head > .bigX"));
//                    cancel.click();
                    driver.navigate().refresh();
                    attendanceAdded = selectDropdown(wait, driver, student, startDate); // Restart method
                }

                WebElement saveAttendance = driver.findElement(By.id("SaveAttendanceBtn"));
                saveAttendance.click();
            }

            // TODO: Does this even work?
            try {
                WebElement errorMsg = driver.findElement(By.id("AttErrMsgCreate")); // Something goes wrong when trying to save attendance
                if (!errorMsg.getText().isEmpty())
                    attendanceAdded = false;
                    WebElement cancel = driver.findElement(By.cssSelector(".col-md-12 > .popUp > .head > .bigX"));
                    cancel.click();
                    Thread.sleep(1000);

            }
            catch (Exception e) {
                System.out.println("Attendance error message does not exist!");
            }

            rowIt = sheet.iterator();
            rowIt.next();
            fos = new FileOutputStream(arrival);
            while (rowIt.hasNext()) {
                Row row = rowIt.next();
                if (row.getCell(3).toString().equals(key)) {
                    row.createCell(9);
                    if (attendanceAdded) {
                        System.out.println("Added attendance for " + student.name);
                        row.getCell(9).setCellValue("\u2714");
                        fillInCells(row, successCell);
                    }
                    else {
                        row.getCell(9).setCellValue("\u2718");
                        fillInCells(row, failCell);
                    }
                }
            }
            workbook.write(fos);

            Thread.sleep(1000);
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
            if (directoryChooser.showDialog(primaryStage) != null)
                PATH = directoryChooser.showDialog(primaryStage).getPath();
            System.out.println("Path changed to: " + PATH);
        });
        grid.add(dirBtn, 1, 5);

        Button btn = new Button("Start");
        btn.setOnAction(event -> {
//            btn.setDisable(true);

            File arrival = validateExcelFile(attendanceDate1.getValue(), attendanceDate1.getValue());
            if (arrival != null && arrival.exists()) {

//            System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "\\src\\chromedriver.exe");
//            ChromeDriver driver = new ChromeDriver();
                ChromeDriver driver = ChromeDriverSingleton.getInstance(); // singleton

                long startTime = System.currentTimeMillis();
                Task task = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        automation(driver, arrival, radiusUserTextField.getText(), radiusPWTextField.getText(), attendanceDate1.getValue(), attendanceDate1.getValue());
                        return null;
                    }
                };
                task.setOnFailed(event1 -> {
                    long endTime = System.currentTimeMillis();

                    btn.setDisable(false);
                    Exception e = (Exception) task.getException();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setHeaderText("Something went wrong!");
                    alert.setContentText("Please restart automation. Runtime of " + ((double) (endTime - startTime) / 60000) + " minutes.");
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
                    long endTime = System.currentTimeMillis();

                    btn.setDisable(false);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText("Success!");
                    alert.setContentText("Completed attendance automation. Runtime of " + ((double) (endTime - startTime) / 60000) + " minutes.");
                    alert.showAndWait();

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
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void showSplash(
            final Stage initStage,
            Task<?> task,
            InitCompletionHandler initCompletionHandler) throws FileNotFoundException {
        Pane splashLayout;
        splashLayout = new VBox();
        Label splash = new Label("Mathnasium Attendance Automation");
        splash.setPadding(new Insets(0,0,15,0));
        ((VBox) splashLayout).setAlignment(Pos.CENTER);
        splashLayout.getChildren().add(splash);
        FileInputStream imgFI = new FileInputStream("img8.png");
        Image image = new Image(imgFI,75,75,true,true);
        ImageView imgView = new ImageView(image);
        splashLayout.getChildren().add(imgView);
        splashLayout.setStyle(
                "-fx-padding: 5; " +
                        "-fx-background-color: white; " +
                        "-fx-border-width:5; " +
                        "-fx-border-color: " +
                        "linear-gradient(" +
                        "to bottom, " +
                        "red, " +
                        "derive(black, 50%)" +
                        ");" +
                        "-fx-font: 28 arial-bold;"
        );
        splashLayout.setEffect(new DropShadow());

        task.stateProperty().addListener((observableValue, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                initStage.toFront();
                FadeTransition fadeSplash = new FadeTransition(Duration.seconds(1.2), splashLayout);
                fadeSplash.setFromValue(1.0);
                fadeSplash.setToValue(0.0);
                fadeSplash.setOnFinished(actionEvent -> initStage.hide());
                fadeSplash.play();

                initCompletionHandler.complete();
            } // todo add code to gracefully handle other task states.
        });

        Scene splashScene = new Scene(splashLayout, Color.TRANSPARENT);
        initStage.setScene(splashScene);
        initStage.setHeight(175);
        initStage.setWidth(500);
        initStage.initStyle(StageStyle.TRANSPARENT);
        initStage.setAlwaysOnTop(true);
        initStage.show();
    }

    public interface InitCompletionHandler {
        void complete();
    }

    @Override
    public void start(final Stage initStage) throws Exception {
        final Task<Void> friendTask = new Task<Void>() {
            @Override
            protected Void call() throws InterruptedException {
                Thread.sleep(2000);
                return null;
            }
        };

        showSplash(
                initStage,
                friendTask,
                () -> {
                    try {
                        showMainStage();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        );
        new Thread(friendTask).start();
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

    public File validateExcelFile(LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("M-d-yyyy");
//        System.out.println("Start: " + startDate.format(formatter1)); // Excel log info
//            System.out.println("End: " + endDate.format(formatter1));

        File dir1 = new File(PATH);
//        File dir1 = new File("C:\\Users\\Mathnasium\\Downloads");
        System.out.println(dir1);
        if (!dir1.exists()) {
            return null;
        }
        File[] foundFiles = dir1.listFiles((dir, name) -> name.startsWith("Client Arrivals Report " + startDate.format(formatter1) + " - " + endDate.format(formatter1)));
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

    public boolean selectDropdown(WebDriverWait wait, ChromeDriver driver, Student student, LocalDate startDate) throws InterruptedException, ParseException {
        boolean dateFound = false;

        WebElement attendanceBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("AddAttendanceBtn")));
        Thread.sleep(250); // Attendance btn sometimes does not work!
        attendanceBtn.click();
        Thread.sleep(250); // Dropdown needs to load
        WebElement defaultDate = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".col-md-8 > .k-widget > .k-dropdown-wrap > .k-input")));
        if (!defaultDate.getText().contains("--Select One--")) {
            DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("M/d/yyyy");
            String dateRange = defaultDate.getText().toLowerCase().split("[\\(\\)]")[1];
            String date1 = dateRange.split("-")[0];
            String date2 = dateRange.split("-")[1];
            LocalDate parsedDate1 = LocalDate.parse(date1, formatter3);

            if ((parsedDate1.isBefore(startDate) || parsedDate1.isEqual(startDate))) {
                if (date2.equals("recurring")) {
                    dateFound = true;
                }
                else {
                    LocalDate parsedDate2 = LocalDate.parse(date2, formatter3);
                    if (parsedDate2.isAfter(startDate) || parsedDate2.isEqual(startDate)) {
                        dateFound = true;
                    }
                }
            }
        }

        if (!dateFound) {
            WebElement openDropdown = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".col-md-8 > .searchGridDropDown")));
            Thread.sleep(500); // Dropdown needs to load
            openDropdown.sendKeys(Keys.SPACE);
            Thread.sleep(500); // Dropdown needs to load
//        WebElement select = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#EnrollmentDropDown_listbox")));
            List<WebElement> select = driver.findElements(By.cssSelector("#EnrollmentDropDown_listbox > li"));
            for (WebElement e : select) {
                DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("M/d/yyyy");
                String parentheses[] = e.getText().toLowerCase().split("[\\(\\)]");
                String dateRange = parentheses[parentheses.length - 1]; // Last pair of parentheses
                String date1 = dateRange.split("-")[0];
                String date2 = dateRange.split("-")[1];
                LocalDate parsedDate1 = LocalDate.parse(date1, formatter3);

                if ((parsedDate1.isBefore(startDate) || parsedDate1.isEqual(startDate))) {
                    if (date2.equals("recurring")) {
//                    System.out.println(e.getText());
                        e.click();
                        dateFound = true;
                        break;
                    } else {
                        LocalDate parsedDate2 = LocalDate.parse(date2, formatter3);
                        if (parsedDate2.isAfter(startDate) || parsedDate2.isEqual(startDate)) {
//                        System.out.println(e.getText());
                            e.click();
                            dateFound = true;
                            break;
                        }
                    }
                }

            }
        }

        if (dateFound) {
            DateTimeFormatter formatterM = DateTimeFormatter.ofPattern("M");
            DateTimeFormatter formatterD = DateTimeFormatter.ofPattern("d");
            DateTimeFormatter formatterY = DateTimeFormatter.ofPattern("yyyy");

            WebElement date = driver.findElement(By.id("AttendanceDate"));
            date.clear();
            date.sendKeys(Keys.BACK_SPACE);
            date.sendKeys(startDate.format(formatterM));
            date.sendKeys(Keys.ARROW_RIGHT);
            date.sendKeys(Keys.BACK_SPACE);
            date.sendKeys(startDate.format(formatterD));
            date.sendKeys(Keys.ARROW_RIGHT);
            date.sendKeys(Keys.BACK_SPACE);
            date.sendKeys(startDate.format(formatterY));
            date.sendKeys(Keys.ARROW_RIGHT);
            date.click();
            WebElement timeIn = driver.findElement(By.id("ArrivalTime"));
            timeIn.clear();
            timeIn.sendKeys(student.startTime);
            WebElement timeOut = driver.findElement(By.id("DepartureTime"));
            timeOut.clear();
            timeOut.sendKeys(student.endTime);
        }
        return dateFound;
    }

    public void fillInCells(Row row, CellStyle backgroundStyle) {
        for (int i = 0; i <= 9; i++) {
            row.getCell(i).setCellStyle(backgroundStyle);
        }
    }
}