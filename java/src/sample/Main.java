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
import javafx.stage.Stage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.lang.reflect.Array;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main extends Application {
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

    public void automation(String user1, String user2, String studentName, HashMap<String,String> loginInfo, ArrayList<MasteryCheck> mcList) throws InterruptedException {
        // Optional, if not specified, WebDriver will search your path for chromedriver.
        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");

        WebDriver driver = new ChromeDriver();
        driver.get("https://radius.mathnasium.com/Account/Login");
        WebElement radiusUser = driver.findElement(By.id("UserName"));
        radiusUser.sendKeys(user1);
        WebElement radiusPass = driver.findElement(By.id("Password"));
        radiusPass.sendKeys(loginInfo.get(user1));
        radiusPass.submit();
        Thread.sleep(1000);  // Let the user actually see something!

        WebElement searchIcon = driver.findElement(By.id("SearchIcon"));
        searchIcon.click();
        Thread.sleep(1000);
        WebElement searchBar = driver.findElement(By.id("ContactSearch"));
        searchBar.sendKeys(studentName);
        WebElement searchBtn = driver.findElement(By.id("globalbtnsearch"));
        searchBtn.click();
        Thread.sleep(500);
        WebElement studentHref = driver.findElement(By.cssSelector("table.k-selectable > tbody > tr:first-child > td:nth-child(3) > a"));
        studentHref.click();
        Thread.sleep(2000);

        WebElement editLP = driver.findElement(By.cssSelector("#gridLearningPlan > table > tbody > tr:first-child > td:nth-child(6) > a"));
        editLP.click();
        Thread.sleep(4000);

        while(!mcList.isEmpty()) {
            List<WebElement> rows = driver.findElements(By.cssSelector("#gridLP > table > tbody > tr"));
            int itr = 1;
            boolean found = false;
            for (WebElement row : rows) {
                WebElement descr = driver.findElement(By.cssSelector("#gridLP > table > tbody > tr:nth-child(" + itr + ") > td:nth-child(9)"));
                System.out.println(descr.getAttribute("innerHTML"));
                if (descr.getAttribute("innerHTML").contains(mcList.get(0).num)) {
                    System.out.println("Success!");
                    WebElement dateStart = driver.findElement(By.cssSelector("#gridLP > table > tbody > tr:nth-child(" + itr + ") > td:nth-child(11) > span > span > input"));
                    WebElement dateEnd = driver.findElement(By.cssSelector("#gridLP > table > tbody > tr:nth-child(" + itr + ") > td:nth-child(12) > span > span > input"));
                    if (dateStart.getAttribute("value").equals(""))
                        dateStart.sendKeys(mcList.get(0).dateStart);
                    if (dateEnd.getAttribute("value").equals(""))
                        dateEnd.sendKeys(mcList.get(0).dateEnd);
                    found = true;
                    mcList.remove(0);
                    break;
                }
                itr++;
            }
            if (!found) {
                WebElement add = driver.findElement(By.id("additem"));
                add.click();
                Thread.sleep(2000);

                WebElement pkSearch = driver.findElement(By.id("PKSearch"));
                pkSearch.sendKeys(mcList.get(0).num);
                WebElement select = driver.findElement(By.cssSelector("#gridPK > table > tbody > tr:first-child > td:first-child > input"));
                select.click();
                WebElement savePK = driver.findElement(By.id("btnsave"));
                savePK.click();
                Thread.sleep(5000);
            }
        }

        WebElement goBack = driver.findElement(By.id("studenturl"));
        goBack.click();
        Thread.sleep(2000);

        // PRINT LP REPORT
        WebElement LPReport = driver.findElement(By.cssSelector("#gridLearningPlan > table > tbody > tr:first-child > td:nth-child(5) > a"));
        LPReport.click();
        Thread.sleep(500);
        WebElement saveReport = driver.findElement(By.id("ViewPrintChartBtn"));
        saveReport.click();
        Thread.sleep(4000);

        // PRINT ASSESSMENT
        WebElement assessment = driver.findElement(By.cssSelector("#gridAssessment > table > tbody > tr:first-child > td:nth-child(6) > a"));
        assessment.click();
        WebElement assessmentNum = driver.findElement(By.cssSelector("#gridAssessment > table > tbody > tr:first-child > td:nth-child(1)"));
        Thread.sleep(500);
        saveReport.click();
        Thread.sleep(4000);

        // RENAME FILES
        Format formatter = new SimpleDateFormat("MMMM YYYY ");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        String date = formatter.format(calendar.getTime());
        String PATH1 = "C:/Users/Mathnasium/Downloads/";
        String PATH2 = "C:/Users/Mathnasium/Documents/";
        String[] split1 = assessmentNum.getAttribute("innerHTML").split("#");
        String num = split1[1].substring(0,1);
        String parseName = studentName.replace(" ","_");

        File lpFile1 = new File(PATH1 + "Learning Plan Report_ "+studentName+"_.pdf");
        File lpFile2 = new File(PATH2 + date + "Learning Plan - " + studentName + ".pdf");
        lpFile1.renameTo(lpFile2);

        File aFile1 = new File(PATH1 + "Assessment Chart_" + parseName +"_" + num + ".pdf");
        File aFile2 = new File(PATH2 + date + "Assessment Chart - " + studentName + ".pdf");
        aFile1.renameTo(aFile2);

        File scan1 = new File(PATH2 + studentName+".pdf");
        File scan2 = new File(PATH2 + date + "Mastery Checks - " + studentName + ".pdf");
        scan1.renameTo(scan2);

//        driver.quit();

    }
    public void start(Stage primaryStage) throws Exception{

        primaryStage.setTitle("Hello World");
        GridPane grid = new GridPane();
        ScrollPane sp = new ScrollPane(grid);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Text sceneTitle = new Text("Report Automation");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(sceneTitle, 0, 0, 2, 1);

        Label radiusUser = new Label("Radius username:");
        grid.add(radiusUser, 0, 1);
        TextField radiusUserTextField = new TextField("arup.mondal");
        grid.add(radiusUserTextField, 1, 1);

        Label radiusPW = new Label("Radius password:");
        grid.add(radiusPW, 0, 2);
        TextField radiusPWTextField = new TextField("max123");
        grid.add(radiusPWTextField, 1, 2);

        Label MBUser = new Label("MindBody username:");
        grid.add(MBUser, 4, 1);
        TextField MBUserTextField = new TextField();
        grid.add(MBUserTextField, 5, 1);

        Label MBPW = new Label("MindBody password:");
        grid.add(MBPW, 4, 2);
        TextField MBPWTextField = new TextField();
        grid.add(MBPWTextField, 5, 2);

        Label studentName = new Label("Student name:");
        grid.add(studentName, 0, 3);
        TextField studentNameTextField = new TextField("taylor mills");
        grid.add(studentNameTextField, 1, 3);

        TextField[] mcNum = new TextField[12];
        TextField[] mcStart = new TextField[12];
        TextField[] mcEnd = new TextField[12];
        for (int i = 0; i < 12; i++) {
            Label MC = new Label("MC #:");
            grid.add(MC, 0, 5+i);
            TextField MCTextField = new TextField();
            grid.add(MCTextField, 1, 5+i);
            Label MCStart = new Label("MC Start:");
            grid.add(MCStart, 2, 5+i);
            TextField MCStartTextField = new TextField();
            grid.add(MCStartTextField, 3, 5+i);
            Label MCEnd = new Label("MC End:");
            grid.add(MCEnd, 4, 5+i);
            TextField MCEndTextField = new TextField();
            grid.add(MCEndTextField, 5, 5+i);

            mcNum[i] = MCTextField;
            mcStart[i] = MCStartTextField;
            mcEnd[i] = MCEndTextField;
        }

        CheckBox cb2 = new CheckBox("Satisfactory?");
        cb2.setSelected(true);
        grid.add(cb2, 1, 18);

        EventHandler<ActionEvent> event = e -> {
            try {
                primaryStage.close();
                HashMap<String,String> loginInfo = new HashMap<>();
                loginInfo.put(radiusUserTextField.getText(), radiusPWTextField.getText());
                loginInfo.put(MBUserTextField.getText(), MBPWTextField.getText());

                ArrayList<MasteryCheck> mcList = new ArrayList<>();
                for (int i = 0; i < 12; i++) {
                    if (!mcNum[i].getText().equals("")) {
                        MasteryCheck mc = new MasteryCheck(mcNum[i].getText(), mcStart[i].getText(), mcEnd[i].getText());
                        mcList.add(mc);
                    }
                }
                automation(radiusUserTextField.getText(), MBUserTextField.getText(), studentNameTextField.getText(), loginInfo, mcList);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        };

        Button btn = new Button("Start");
        btn.setOnAction(event);
        HBox hbBtn = new HBox(10);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 19);
        primaryStage.setScene(new Scene(sp, 800, 500));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
