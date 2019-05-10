import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Admin on 5/10/2019.
 */
public class AllStudentIDs {
    static String token;
    static String cookie;

    public static void main(String[] args ) throws Exception {
        int ellicott = 30;
        int germantown = 32;
        int northpoto = 29;
        int potomac = 3050;
        int rockville = 31;
        Workbook workbook = new XSSFWorkbook();
        login("", "");
        getAllStudentIDs(workbook, ellicott);
        getAllStudentIDs(workbook, germantown);
        getAllStudentIDs(workbook, northpoto);
        getAllStudentIDs(workbook, potomac);
        getAllStudentIDs(workbook, rockville);
        FileOutputStream fileOut = new FileOutputStream("Radius-Student-IDs.xlsx");
        workbook.write(fileOut);
        fileOut.close();
        workbook.close();
    }
    public static boolean login(String user, String pass) throws Exception {
        int responseCode;
        // Get cookies and token from login page
        HttpURLConnection conn = (HttpURLConnection) new URL("https://radius.mathnasium.com/Account/Login?ReturnUrl=%2F").openConnection();
        List<String> cookies = conn.getHeaderFields().get("Set-Cookie");
        String wholeCookie = "";
        for (String c : cookies) {
            wholeCookie += c.split(";")[0] + "; "; // remove trailing info from cookie
        }
        cookie = wholeCookie;
        Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuffer response = new StringBuffer();
        for (int c; (c = in.read()) >= 0;)
            response.append((char)c);
        String regexString = Pattern.quote("<form action=\"/Account/Login?ReturnUrl=%2F\" class=\"form-horizontal\" method=\"post\" role=\"form\"><input name=\"__RequestVerificationToken\" type=\"hidden\" value=\"") + "(.*?)" + Pattern.quote("\" />");
        Pattern pattern = Pattern.compile(regexString);
        Matcher matcher = pattern.matcher(response);
        while (matcher.find()) {
            token = matcher.group(1); // first token
            break;
        }

        // login using cookie + token + login info
        URL url = new URL("https://radius.mathnasium.com/Account/Login");
        String urlParameters = "UserName=" + user + "&Password=" + pass + "&RememberMe=true&__RequestVerificationToken=" + token;
        byte[] postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
        conn = (HttpURLConnection)url.openConnection();
        conn.setReadTimeout(5000);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
        conn.setRequestProperty("origin","https://radius.mathnasium.com");
        conn.setRequestProperty("referer","https://radius.mathnasium.com/Account/Login");
        conn.setRequestProperty("user-agent","Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("upgrade-insecure-requests", "1");
        conn.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        conn.setRequestProperty("accept-encoding", "gzip, deflate, br");
        conn.setRequestProperty("accept-language", "en-US,en;q=0.9");
        conn.setRequestProperty("Cache-Control", "max-age=0");
        conn.setRequestProperty("Cookie", cookie);
        conn.setDoOutput(true);
        // x www url encoded
        try(DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write( postData );
        }
        responseCode = conn.getResponseCode();
        if (responseCode == 302) {
            cookies = conn.getHeaderFields().get("Set-Cookie");
            wholeCookie = "";
            for (String c : cookies) {
                wholeCookie += c.split(";")[0] + "; ";
            }
            // add new cookies
            cookie += wholeCookie;
        }

        // get new token
        url = new URL("https://radius.mathnasium.com/");
        conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("cookie", cookie);
        conn.setDoOutput(true);
        in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        response = new StringBuffer();
        for (int c; (c = in.read()) >= 0;)
            response.append((char)c);
        regexString = Pattern.quote("<input name=\"__RequestVerificationToken\" type=\"hidden\" value=\"") + "(.*?)" + Pattern.quote("\" />");
        pattern = Pattern.compile(regexString);
        matcher = pattern.matcher(response);
        while (matcher.find()) {
            token = matcher.group(1);
            break;
        }

        return (responseCode == 302) && token.length() > 0 && cookie.length() > 0;
    }


    public static void getAllStudentIDs(Workbook workbook, int location) throws Exception {
        String locationName = "";
        switch(location) {
            case 30: locationName="Ellicott City"; break;
            case 32: locationName="Germantown"; break;
            case 29: locationName="North Potomac"; break;
            case 3050: locationName="Potomac"; break;
            case 31: locationName="Rockville"; break;
        }
        System.out.print("Searching " + locationName + "...");
        Sheet sheet = workbook.createSheet(locationName);
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Student Name");
        header.createCell(1).setCellValue("Student ID");
        URL url = new URL("https://radius.mathnasium.com/Attendance/StudentAttendances_Read?centerId="+location);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(true);
        HttpURLConnection.setFollowRedirects(true);
        conn.setRequestProperty("cookie", cookie);
        conn.setDoOutput(true);
        JsonReader jsonReader = Json.createReader(new InputStreamReader(conn.getInputStream()));
        JsonObject obj = jsonReader.readObject();
        if (obj != null) {
            JsonArray array = obj.getJsonArray("Data");
            int lastRow = sheet.getLastRowNum();
            for (int i = 0; i < array.size(); i++) {
                int studentID = array.getJsonObject(i).getInt("StudentID");
                String studentName = array.getJsonObject(i).getString("StudentName");
                Row row = sheet.createRow(++lastRow);
                // Create Other rows and cells with employees data
                row.createCell(0).setCellValue(studentName);
                row.createCell(1).setCellValue(studentID);
            }
        }
        sheet.autoSizeColumn(0);
        jsonReader.close();
    }

}
