import org.openqa.selenium.chrome.ChromeDriver;

/**
 * Created by Alex on 12/31/2018.
 */
public class ChromeDriverSingleton {

    public static ChromeDriver driver;

    public static ChromeDriver getInstance() {
        if (driver == null) {
            System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "\\src\\chromedriver.exe");
            driver = new ChromeDriver();
        }
        return driver;
    }

}
