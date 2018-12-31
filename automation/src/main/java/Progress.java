import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class Progress extends Application {
    String currentDir = System.getProperty("user.dir");
    String PATH1 = "C:\\Users\\Mathnasium\\Downloads\\";
    String PATH2 = "C:\\Users\\Mathnasium\\Documents\\";
//    String PATH2 = currentDir + "\\reports";
    boolean isLoggedIn = false;

    class MasteryCheck {
        String num;
        String dateStart;
        String dateEnd;

        public MasteryCheck(String num, String dateStart, String dateEnd) {
            this.num = num;
            this.dateStart = dateStart;
            this.dateEnd = dateEnd;
        }
    }

    public void automation(WebDriver driver1, String user1, String user2, String studentName, HashMap<String,String> loginInfo, ArrayList<MasteryCheck> mcList) throws InterruptedException {
        WebDriverWait wait1 = new WebDriverWait(driver1,6000);

        if (!isLoggedIn) {
            WebElement radiusUser = driver1.findElement(By.id("UserName"));
            radiusUser.sendKeys(user1);
            WebElement radiusPass = driver1.findElement(By.id("Password"));
            radiusPass.sendKeys(loginInfo.get(user1));
            radiusPass.submit();
            isLoggedIn = true;
        }

        WebElement searchIcon = wait1.until(ExpectedConditions.visibilityOfElementLocated(By.id("SearchIcon")));
        searchIcon.click();
        WebElement searchBar = wait1.until(ExpectedConditions.visibilityOfElementLocated(By.id("ContactSearch")));
        searchBar.sendKeys(studentName);
        WebElement searchBtn = driver1.findElement(By.id("globalbtnsearch"));
        searchBtn.click();
        WebElement studentHref = wait1.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("table.k-selectable > tbody > tr:first-child > td:nth-child(3) > a")));
        studentHref.click();

        WebElement editLP = wait1.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#gridLearningPlan > table > tbody > tr:first-child > td:nth-child(6) > a")));
        editLP.click();

        while(!mcList.isEmpty()) {
            WebElement LP = wait1.until(ExpectedConditions.visibilityOfElementLocated(By.id("gridLP")));
            List<WebElement> rows = driver1.findElements(By.cssSelector("#gridLP > table > tbody > tr"));
            int itr = 1;
            boolean found = false;
            for (WebElement row : rows) {
                WebElement descr = wait1.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#gridLP > table > tbody > tr:nth-child(" + itr + ") > td:nth-child(9)")));
                System.out.println(descr.getAttribute("innerHTML"));
                if (descr.getAttribute("innerHTML").contains(mcList.get(0).num)) {
                    System.out.println("Success!");
                    WebElement dateStart = driver1.findElement(By.cssSelector("#gridLP > table > tbody > tr:nth-child(" + itr + ") > td:nth-child(11) > span > span > input"));
                    WebElement dateEnd = driver1.findElement(By.cssSelector("#gridLP > table > tbody > tr:nth-child(" + itr + ") > td:nth-child(12) > span > span > input"));
                    WebElement isMastered = driver1.findElement(By.cssSelector("#gridLP > table > tbody > tr:nth-child(" + itr + ") > td:nth-child(15) > input"));
                    if (dateStart.getAttribute("value").equals("")) {
                        dateStart.sendKeys(mcList.get(0).dateStart);
                        dateStart.click();
                    }
                    if (dateEnd.getAttribute("value").equals("")) {
                        dateEnd.sendKeys(mcList.get(0).dateEnd);
                        dateEnd.click();
                    }
                    if (!isMastered.isSelected())
                        isMastered.click();
                    found = true;
                    mcList.remove(0);
                    break;
                }
                itr++;
            }
            if (!found) {
                WebElement add = wait1.until(ExpectedConditions.visibilityOfElementLocated(By.id("additem")));
                add.click();

                WebElement pkSearch = wait1.until(ExpectedConditions.visibilityOfElementLocated(By.id("PKSearch")));
                pkSearch.clear();
                pkSearch.sendKeys(mcList.get(0).num);
                Thread.sleep(2500);
                WebElement select = wait1.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#gridPK > table > tbody > tr:first-child > td:first-child > input")));
                select.click();

                WebElement savePK = driver1.findElement(By.id("btnsave"));
                savePK.click();
                Thread.sleep(5000);
                found = true;
            }
        }

        WebElement goBack = driver1.findElement(By.id("studenturl"));
        goBack.click();

        // PRINT LP REPORT
        WebElement LPReport = wait1.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#gridLearningPlan > table > tbody > tr:first-child > td:nth-child(5) > a")));
        LPReport.click();
        Thread.sleep(500);
        WebElement saveReport = driver1.findElement(By.id("ViewPrintChartBtn"));
        saveReport.click();
        Thread.sleep(5000);

        // PRINT ASSESSMENT
        WebElement assessment = wait1.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#gridAssessment > table > tbody > tr:first-child > td:nth-child(6) > a")));
        assessment.click();
        Thread.sleep(500);
        saveReport.click();
        Thread.sleep(5000);

        // RENAME FILES
        Format formatter = new SimpleDateFormat("MMMM YYYY ");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        String date = formatter.format(calendar.getTime());
        String parseName = studentName.replace(" ","_");

        File studentDir = new File(PATH2 + studentName + " Reports\\");
        studentDir.mkdir();

        File dir1 = new File(PATH1);
        File[] foundFiles = dir1.listFiles((dir, name) -> name.startsWith("Learning Plan Report_ "+studentName));
        File lpFile1 = foundFiles[0];
        System.out.println(lpFile1.getName());
        for (File f : foundFiles) {
            if (f.lastModified() > lpFile1.lastModified())
                lpFile1 = f;
        }
        File lpFile2 = new File(PATH2 + studentName + " Reports\\" + date + "Learning Plan - " + studentName + ".pdf");
        lpFile1.renameTo(lpFile2);

        File dir2 = new File(PATH1);
        foundFiles = dir2.listFiles((dir, name) -> name.startsWith("Assessment Chart_" + parseName));
        File aFile1 = foundFiles[0];
        System.out.println(aFile1.getName());
        for (File f : foundFiles) {
            if (f.lastModified() > aFile1.lastModified())
                aFile1 = f;
        }
        File aFile2 = new File(PATH2 + studentName + " Reports\\" + date + "Assessment Chart - " + studentName + ".pdf");
        aFile1.renameTo(aFile2);

        File scan1 = new File(PATH1  + studentName + ".pdf");
        File scan2 = new File(PATH2 + studentName + " Reports\\" + date + "Mastery Checks - " + studentName + ".pdf");
        scan1.renameTo(scan2);

        // Go to parent's email
        WebElement parent = driver1.findElement(By.cssSelector("dl.dl-horizontal > dd > a"));
        parent.click();

        WebElement sendEmail = driver1.findElement(By.id("SendEmail"));
        sendEmail.click();
        Thread.sleep(500);
        WebElement template = driver1.findElement(By.id("TemplateName"));
        Select dropdown= new Select(template);
        dropdown.selectByVisibleText("Progress Report Satisfactory");

//        driver1.quit();

        // MindBody automation
//        WebDriver driver2 = new ChromeDriver();
//        driver2.get("https://clients.mindbodyonline.com/ASP/su1.asp?studioid=767884");
//        WebElement login = driver2.findElement(By.id("requiredtxtUserName"));
//        login.sendKeys(user2);
//        WebElement password = driver2.findElement(By.id("requiredtxtPassword"));
//        password.sendKeys(loginInfo.get(user2));
//        password.submit();
//        Thread.sleep(5000);
//
//        WebElement tabs = driver2.findElement(By.cssSelector("#tabs > ul > li:nth-child(6) > a"));
//        tabs.click();
//        Thread.sleep(3000);
//
//        WebElement clientSearch = driver2.findElement(By.id("txtClientSearch"));
//        clientSearch.sendKeys(studentName);
//        WebElement searchBtn = driver2.findElement(By.id("btnSearch"));
//        searchBtn.click();
//        Thread.sleep(3000);
//
//        WebElement accountDetails = driver2.findElement(By.id("btnClientProfile"));
//        accountDetails.click();
//        Thread.sleep(2000);
//
//        WebElement clientIndexes = driver2.findElement(By.cssSelector("#clientindexes > legend > a > span"));
//        clientIndexes.click();
    }
    public void start(Stage primaryStage) throws Exception{

        primaryStage.setTitle("Progress Report Automation");
        GridPane grid = new GridPane();
        ScrollPane sp = new ScrollPane(grid);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));

        Text sceneTitle = new Text("Progress Report Automation");
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

        Label MBUser = new Label("MindBody username:");
        grid.add(MBUser, 2, 1);
        TextField MBUserTextField = new TextField();
        grid.add(MBUserTextField, 3, 1);

        Label MBPW = new Label("MindBody password:");
        grid.add(MBPW, 2, 2);
        PasswordField MBPWTextField = new PasswordField();
        grid.add(MBPWTextField, 3, 2);

        Button PATH1Btn = new Button("Change Downloads Folder");
        grid.add(PATH1Btn, 5, 1);
        Button PATH2Btn = new Button("Change Reports Folder");
        grid.add(PATH2Btn, 5, 2);

        EventHandler<ActionEvent> choosePath1 = e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Downloads Folder");
            PATH1 = directoryChooser.showDialog(primaryStage).getPath();
            System.out.println("Path changed to: " + PATH1);
        };
        PATH1Btn.setOnAction(choosePath1);

        EventHandler<ActionEvent> choosePath2 = e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Reports Folder");
            PATH2 = directoryChooser.showDialog(primaryStage).getPath();
            System.out.println("Path changed to: " + PATH2);
        };
        PATH2Btn.setOnAction(choosePath2);

        Label studentName = new Label("Student name:");
        grid.add(studentName, 0, 3);
        TextField studentNameTextField = new TextField();
        studentNameTextField.setText("Test Student");
        grid.add(studentNameTextField, 1, 3);

        TextField[] mcNum = new TextField[12];
        DatePicker[] mcStart = new DatePicker[12];
        DatePicker[] mcEnd = new DatePicker[12];
        for (int i = 0; i < 12; i++) {
            Label MC = new Label("\tMC #:");
            grid.add(MC, 0, 5+i);
            TextField MCTextField = new TextField();
            grid.add(MCTextField, 1, 5+i);
            Label MCStart = new Label("MC Start:");
            grid.add(MCStart, 2, 5+i);
            DatePicker MCStartDate = new DatePicker();
            grid.add(MCStartDate, 3, 5+i);
            Label MCEnd = new Label("\tMC End:");
            grid.add(MCEnd, 4, 5+i);
            DatePicker MCEndDate = new DatePicker();
            grid.add(MCEndDate, 5, 5+i);
            mcNum[i] = MCTextField;
            mcStart[i] = MCStartDate;
            mcEnd[i] = MCEndDate;
        }

        CheckBox cb2 = new CheckBox("Satisfactory?");
        cb2.setSelected(true);
        grid.add(cb2, 1, 18);

        EventHandler<ActionEvent> event = e -> {
            WebDriver driver1 = ChromeDriverSingleton.getInstance();
            driver1.get("https://radius.mathnasium.com");

            try {
//                primaryStage.close();
                HashMap<String,String> loginInfo = new HashMap<>();
                loginInfo.put(radiusUserTextField.getText(), radiusPWTextField.getText());
                loginInfo.put(MBUserTextField.getText(), MBPWTextField.getText());

                ArrayList<MasteryCheck> mcList = new ArrayList<>();
                for (int i = 0; i < 12; i++) {
                    if (!mcNum[i].getText().equals("")) {
                        MasteryCheck mc = new MasteryCheck(mcNum[i].getText(), mcStart[i].getEditor().getText(), mcEnd[i].getEditor().getText());
                        mcList.add(mc);
                    }
                }
                automation(driver1, radiusUserTextField.getText(), MBUserTextField.getText(), studentNameTextField.getText(), loginInfo, mcList);
            } catch (Exception el) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Something went wrong!");
                alert.setContentText("Please restart automation.\n");
                alert.getDialogPane().setExpandableContent(new ScrollPane(new TextArea(el.toString())));
                alert.showAndWait();

                el.printStackTrace();
                driver1.get("https://radius.mathnasium.com");
            }
        };

        Button btn = new Button("Start");
        btn.setOnAction(event);
        HBox hbBtn = new HBox(10);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 19);
        primaryStage.setScene(new Scene(sp, 880, 660));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}