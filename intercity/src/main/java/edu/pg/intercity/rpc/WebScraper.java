package edu.pg.intercity.rpc;

import edu.pg.intercity.dto.OfferDto;
import edu.pg.intercity.dto.QueryDto;
import edu.pg.intercity.dto.VehicleDto;
import edu.pg.intercity.dto.VehicleType;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PostConstruct;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WebScraper {

    private static final String URL = "https://www.intercity.pl/pl/";
    private String downloadDir = "";

    private Logger logger = LoggerFactory.getLogger(WebScraper.class);

    @PostConstruct
    public void init() {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("selenium-downloads");
            downloadDir = tempDir.toString();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<OfferDto> getOffers(QueryDto query) {
        WebDriver webDriver = null;
        try {
            String source = query.src();
            String destination = query.dest();
            Date queryDatetime = query.time();

            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", downloadDir);
            prefs.put("download.prompt_for_download", false);
            prefs.put("profile.default_content_settings.popups", 0);
            prefs.put("safebrowsing.enabled", true);
            prefs.put("safebrowsing.disable_download_protection", true);
            options.setExperimentalOption("prefs", prefs);
            options.addArguments("--headless=chrome");
            options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--disable-extensions");
            webDriver = new ChromeDriver(options);
            webDriver.get(URL);

            WebDriverWait wait = new WebDriverWait(webDriver, Duration.of(5, ChronoUnit.SECONDS));

            WebElement cookieConfirm = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.id("CybotCookiebotDialogBodyLevelButtonLevelOptinAllowAll")));
            cookieConfirm.click();

            WebElement searchForm = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.id("searchTrainForm")));

            String src = inputStartLocation(webDriver, searchForm, source);
            String dest = inputEndLocation(webDriver, searchForm, destination);
            inputDate(searchForm, queryDatetime, webDriver);

            try {
                Thread.sleep(1000); // 2000 milliseconds = 2 seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            search(searchForm);
            logger.info("Waiting for results");

            WebDriverWait longerWait = new WebDriverWait(webDriver, Duration.of(20, ChronoUnit.SECONDS));
            cookieConfirm = longerWait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.id("CybotCookiebotDialogBodyLevelButtonLevelOptinAllowAll")));
            cookieConfirm.click();

            List<OfferDto> offers = new ArrayList<>();

            List<WebElement> listItems = longerWait.until(
                    ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector("li[data-testid='TripPropositionDesktop']")));

            for (WebElement connection : listItems) {
                try {
                    // Click on the "Buy a ticket" button
                    WebElement buyTicketButton = connection.findElement(By.cssSelector("button[data-testid='TripPropositionDesktop-buy-ticket']"));
                    wait.until(ExpectedConditions.elementToBeClickable(buyTicketButton));
                    buyTicketButton.click();

                    List<Date> schedule = extractSchedule(connection, queryDatetime);
                    Date start = schedule.get(0);
                    Date end = schedule.get(1);

                    List<Double> prices = getPrices(connection);

                    List<VehicleDto> vehicles = getConnectionDetails(connection, queryDatetime);


                    OfferDto offer = new OfferDto(src, dest, start, adjustDateIfNeeded(start, end), prices.getFirst(), vehicles, VehicleType.TRAIN);
                    offers.add(offer);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return offers;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (webDriver != null) {
                webDriver.quit();
            }
        }
    }

    private String inputStartLocation(WebDriver webDriver, WebElement searchForm, String src) {
        return inputAutoComplete(webDriver, searchForm, src, "stname-0");
    }

    private String inputEndLocation(WebDriver webDriver, WebElement searchForm, String dest) {
        return inputAutoComplete(webDriver, searchForm, dest, "stname-1");
    }

    private String inputAutoComplete(WebDriver driver,
                                     WebElement searchForm,
                                     String input,
                                     String id) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(7));

        WebElement inputField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(id)));

        inputField.click();
        inputField.clear();
        inputField.sendKeys(input);

        // Get parent container of input
        WebElement container = (WebElement) ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].parentElement;", inputField);

        // Now search for the matching dropdown near this input
        WebElement dropdown = wait.until(d -> {
            List<WebElement> dropdowns = container.findElements(By.cssSelector("ul.typeahead.dropdown-menu"));
            for (WebElement ul : dropdowns) {
                if (ul.isDisplayed() && ul.findElements(By.tagName("li")).size() > 0) {
                    return ul;
                }
            }
            return null;
        });

        // Select the first autocomplete item
        WebElement firstItem = dropdown.findElement(By.cssSelector("li a"));
        String result = firstItem.getText();
        firstItem.click();

        return result;
    }

    private void inputDate(WebElement searchForm, Date date, WebDriver webDriver) {
        // Format the date and time separately
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");

        String dateStr = dateFormatter.format(date);
        String timeStr = timeFormatter.format(date);

        // Locate the inputs
        WebElement dateInput = searchForm.findElement(By.id("date_picker"));
        WebElement timeInput = searchForm.findElement(By.id("ic-seek-time"));

        // Set values using JavaScript to avoid picker interference
        JavascriptExecutor js = (JavascriptExecutor) webDriver;
        js.executeScript("arguments[0].value = arguments[1];", dateInput, dateStr);
        js.executeScript("arguments[0].value = arguments[1];", timeInput, timeStr);
    }

    private void search(WebElement searchForm) {
        WebElement searchButton = searchForm.findElement(By.cssSelector("button[type='submit']"));
        searchButton.click();
    }

    private List<Date> extractSchedule(WebElement connection, Date date) {
        List<WebElement> timeDivs = connection.findElements(By.cssSelector("[class*='tripPropositionDesktop__label_bigText']"));
        List<Date> schedule = new ArrayList<>();

        for (WebElement timeElement : timeDivs) {
            String timeText = timeElement.getText();
            Date combinedDate = combineDateAndTime(date, timeText);
            schedule.add(combinedDate);
        }

        return schedule;
    }

    private Date combineDateAndTime(Date date, String timeString) {
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            Date time = timeFormat.parse(timeString);

            Calendar dateCal = Calendar.getInstance();
            dateCal.setTime(date);

            Calendar timeCal = Calendar.getInstance();
            timeCal.setTime(time);

            dateCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            dateCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            dateCal.set(Calendar.SECOND, 0);
            dateCal.set(Calendar.MILLISECOND, 0);

            return dateCal.getTime();
        } catch (Exception e) {
            throw new RuntimeException("Error while loading/parsing dates");
        }
    }

    // If the start time is later than end time, add 1 day in the end time
    public static Date adjustDateIfNeeded(Date start, Date end) {
        if (start.after(end)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(end);
            calendar.add(Calendar.DATE, 1);
            return calendar.getTime();
        }

        return end;
    }

    private List<Double> getPrices(WebElement connection) {
        List<WebElement> priceDivs = connection.findElements(By.cssSelector("[class*='SuperPromo_class_box_price']"));
        List<Double> prices = new ArrayList<>();

        for (WebElement priceElement : priceDivs) {
            try {
                String priceText = priceElement.getText();
                double price = Double.parseDouble(priceText.replace("PLN", "").trim().replace(",", "."));
                prices.add(price);
            } catch (Exception e) {
                logger.error("Prices unavailable");
            }
        }

        return prices;
    }

    private List<VehicleDto> getConnectionDetails(WebElement connection, Date date) {
        List<WebElement> connectionDetails = connection.findElements(By.cssSelector("[class*='ConnectionTripListItem_connectionListItem__wrapper']"));
        List<VehicleDto> vehicleDtos = new ArrayList<>();

        try {
            // Connection Detail
            for (WebElement detail : connectionDetails) {

                // Get connection time
                List<WebElement> timeDivs = detail.findElements(By.cssSelector("[class*='ConnectionTripListItem_connectionListItem__time']"));
                List<Date> schedule = new ArrayList<>();

                for (WebElement timeElement : timeDivs) {
                    String timeText = timeElement.getText();
                    Date combinedDate = combineDateAndTime(date, timeText);
                    schedule.add(combinedDate);
                }

                Date start = schedule.get(0);
                Date end =  schedule.get(1);

                // Get Vehicle Detail
                String vehicleId = getVehicleId(detail);
                VehicleDto vehicleDto = new VehicleDto(vehicleId, start, adjustDateIfNeeded(start, end));
                vehicleDtos.add(vehicleDto);

            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while parsing connection details");
        }

        return vehicleDtos;
    }

    private String getVehicleId(WebElement connectionDetail) {
        WebElement trainDetails = connectionDetail.findElement(By.cssSelector("[class*='ConnectionTripListItem_connectionListItem__trainDetails']"));

        WebElement svg = trainDetails.findElement(By.tagName("svg"));
        String ariaLabel = svg.getAttribute("aria-label");

        String fullText = trainDetails.getText(); // e.g., "3534 Kazimierz"
        return ariaLabel + " " + fullText;
    }
}
