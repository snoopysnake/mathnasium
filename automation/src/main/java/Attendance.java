import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

public class Attendance {
    public static void main(String[] args) throws IOException, ParseException {

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

        String currentDir = System.getProperty("user.dir");

        Format formatter = new SimpleDateFormat("MM-dd-yyyy");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1); // YESTERDAY
        String startDate = formatter.format(calendar.getTime());
        String endDate = formatter.format(calendar.getTime());

        File dir1 = new File(currentDir + "\\reports");

//        File[] foundFiles = dir1.listFiles((dir, name) -> name.startsWith("Client Arrivals Report " +startDate+ " - "+endDate));
        File[] foundFiles = dir1.listFiles((dir, name) -> name.startsWith("Client Arrivals Report "));
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
        while(rowIt.hasNext()) {
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

        System.setProperty("webdriver.chrome.driver", currentDir + "\\src\\chromedriver.exe");
        ChromeDriver driver = new ChromeDriver();
        driver.get("http://radius.mathnasium.com");

    }
}
