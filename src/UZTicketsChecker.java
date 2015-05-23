import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.firefox.FirefoxProfile;
import javax.mail.MessagingException;
import java.util.Properties;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.security.GeneralSecurityException;

import java.io.ByteArrayOutputStream;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import java.util.List;

import java.lang.Thread;

public class UZTicketsChecker
{
    public static void main(String[] args) throws Exception
    {
        if (args.length < 3)
        {
            throw new Exception("You should pass all parameters. Parameters: <stationFrom> <stationTo> <date>. Example: Київ Ковель 19.04.2015");
        }

        String stationFrom = args[0]; //"Київ";
        String stationTo = args[1]; //"Ковель";
        String date = args[2]; //"19.04.2015";
        String trainNumber = args.length > 3 ? args[3] : "";
        String[] wagonTypes = {"Купе", "Плацкарт"/*, "Люкс"*/};

        UZTicketsChecker uzTicketsChecker = new UZTicketsChecker();

        for (;;)
        {
            if (uzTicketsChecker.isTicketExists(stationFrom, stationTo, date, trainNumber, wagonTypes))
            {
                String message = "There are tickets from " + stationFrom + " to " + stationTo + " for date " + date;
                System.out.println(message);
                uzTicketsChecker.sendNotification(
                    "UZTicketsChecker. Tickets found.",
                    message
                );
                break;
            }
            else
            {
                System.out.println("There aren't any tickets. Wait for 5 minutes.");
                Thread.sleep(60*5*1000);
            }
        }
    }

    /**
     *
     * @param stationFrom
     * @param stationTo
     * @param date
     * @param trainNumber
     * @param wagonTypes
     * @return
     */
    private boolean isTicketExists(String stationFrom, String stationTo, String date, String trainNumber, String[] wagonTypes)
    {
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

        boolean result = (driver.findElements(By.id("ts_res")).size() > 0 && driver.findElement(By.id("ts_res")).isDisplayed());

        //filter by traint number and wagon type
        if (result && !trainNumber.equals(""))
        {
            result = false;

            List<WebElement> trains = driver.findElements(By.cssSelector("#ts_res table#ts_res_tbl tbody tr"));

            for (WebElement train : trains)
            {
                if (trainNumber.equals(train.findElement(By.cssSelector("td.num a")).getText()))
                {
                    if (wagonTypes.length > 0)
                    {
                        for (String wagonType : wagonTypes)
                        {
                            if (train.findElements(By.cssSelector("td.place div[title=" + wagonType + "]")).size() > 0)
                            {
                                result = true;
                                break;
                            }
                        }
                    }
                    else
                    {
                        result = true;
                    }
                }

                if (result)
                {
                    break;
                }
            }
        }

        driver.quit();

        return result;
    }

    /**
     *
     * @param subject
     * @param text
     * @throws MessagingException
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private void sendNotification(String subject, String text) throws MessagingException, IOException, GeneralSecurityException
    {
        //create email
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress("myroslav.kosinski@gmail.com"));
        email.addRecipient(javax.mail.Message.RecipientType.TO,
                new InternetAddress("myroslav.kosinski@gmail.com"));
        email.setSubject(subject);
        email.setText(text);

        //create message with email
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        email.writeTo(bytes);
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray());
        Message message = new Message();
        message.setRaw(encodedEmail);

        // Load client secrets.
        JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
        HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        String APPLICATION_NAME = "UZTicketsChecker";

        java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), "tmp/UZTicketsChecker/credentials.gmail");
        FileDataStoreFactory DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);

        List<String> SCOPES = Arrays.asList(GmailScopes.MAIL_GOOGLE_COM);

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY,
                new InputStreamReader(UZTicketsChecker.class.getResourceAsStream("/client_secret.json"))
        );

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
        )
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .build();

        Credential credential = new AuthorizationCodeInstalledApp(
                flow,
                new LocalServerReceiver()
        ).authorize("user");

        //send message
        Gmail gmail = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        message = gmail.users().messages().send("me", message).execute();
    }
}
