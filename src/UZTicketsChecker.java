import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.firefox.FirefoxProfile;

public class UZTicketsChecker
{
    public static void main(String[] args) {

        String stationFrom = "Київ";
        String stationTo = "Ковель";
        String date = "19.04.2015";

        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("intl.accept_languages", "uk");

        WebDriver driver = new FirefoxDriver(profile);

        driver.get("http://booking.uz.gov.ua");

        WebElement inputFrom = driver.findElement(By.name("station_from"));
        inputFrom.sendKeys(stationFrom);

        (new WebDriverWait(driver, 10)).until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.autosuggest div[title=" + stationFrom + "]"))
        );

        WebElement suggestionFrom = driver.findElement(By.cssSelector("div.autosuggest div[title=" + stationFrom + "]"));
        suggestionFrom.click();



        WebElement inputTo = driver.findElement(By.name("station_till"));
        inputTo.sendKeys(stationTo);

        (new WebDriverWait(driver, 10)).until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.autosuggest div[title=" + stationTo + "]"))
        );

        WebElement suggestionTo = driver.findElement(By.cssSelector("div.autosuggest div[title=" + stationTo + "]"));
        suggestionTo.click();


        ((JavascriptExecutor)driver).executeScript("document.getElementById('date_dep').value = '" + date + "'");


        WebElement buttonSearch = driver.findElement(By.name("search"));
        buttonSearch.click();


        (new WebDriverWait(driver, 60)).until(
                ExpectedConditions.invisibilityOfElementLocated(By.id("vToolsAlphaBg"))
        );


        if (driver.findElements(By.id("ts_res")).size() > 0 && driver.findElement(By.id("ts_res")).isDisplayed())
        {
            System.out.println("Tickets exists");
        }
        else
        {
            System.out.println("Tickets not found");
        }

        driver.quit();
    }
}
