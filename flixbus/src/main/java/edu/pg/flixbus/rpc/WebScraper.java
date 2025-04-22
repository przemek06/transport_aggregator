package edu.pg.flixbus.rpc;

import edu.pg.flixbus.dto.OfferDto;
import edu.pg.flixbus.dto.QueryDto;
import edu.pg.flixbus.dto.VehicleDto;
import edu.pg.flixbus.dto.VehicleType;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebScraper {

    private static final String URL = "https://flixbus.pl/";
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
        String source = query.src();
        String destination = query.dest();
        Date time = query.time();

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir);
        prefs.put("download.prompt_for_download", false);
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("safebrowsing.enabled", true);
        prefs.put("safebrowsing.disable_download_protection", true);
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");
        WebDriver webDriver = new ChromeDriver(options);
        webDriver.get(URL);

        WebDriverWait wait = new WebDriverWait(webDriver, Duration.of(20, ChronoUnit.SECONDS));

        JavascriptExecutor js = (JavascriptExecutor) webDriver;
        js.executeScript("document.getElementById('usercentrics-root')?.remove();");

        WebElement searchForm = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.className("z80U6")));

        String src = inputStartLocation(webDriver, searchForm, source);
        String dest = inputEndLocation(webDriver, searchForm, destination);
        inputDate(searchForm, time, webDriver);

        search(searchForm, webDriver);

        logger.info("Waiting for results");

        WebDriverWait longerWait = new WebDriverWait(webDriver, Duration.of(20, ChronoUnit.SECONDS));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("hcr-skeleton-12-2-0")));

        logger.info("current URL: " + webDriver.getCurrentUrl());

        WebElement results = longerWait.until(
                ExpectedConditions.visibilityOfElementLocated(By.className("ResultsList__resultsList___eGsLK"))
        );
        List<WebElement> connections = results.findElements(By.cssSelector("[data-e2e='search-result-departure-station']"));

        List<OfferDto> finalList = new ArrayList<OfferDto>();
        for(WebElement connection : results.findElements(By.className("SearchResult__searchResult___cgxzZ"))) {
            finalList.add(map(webDriver, connection, src, dest, time));
        }

        return finalList;
    }

    private String inputStartLocation(WebDriver webDriver, WebElement searchForm, String src) {
        return inputAutoComplete(webDriver, searchForm, src, "searchInput-from");
    }

    private String inputEndLocation(WebDriver webDriver, WebElement searchForm, String dest) {
        return inputAutoComplete(webDriver, searchForm, dest, "searchInput-to");
    }

    private String inputAutoComplete(WebDriver driver,
                                     WebElement searchForm,
                                     String input,
                                     String idName) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement bar = wait.until(d ->
                searchForm.findElements(By.id(idName))
                        .stream()
                        .filter(WebElement::isDisplayed)
                        .findFirst()
                        .orElse(null)
        );

        bar.click();
        bar.clear();
        bar.sendKeys(input);

        WebElement container = null;
        WebElement autocompleteElement = null;


        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                container = searchForm.findElement(By.className("CQgpK"));

                String containerHTML = container.getAttribute("outerHTML");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Interrupted during sleep");
                }

                List<WebElement> listItems = container.findElements(By.className("hcr-legacy-autocomplete__list-item-12-2-0"));

                if (!listItems.isEmpty()) {
                    for (WebElement item : listItems) {
                        WebElement optionElement = item.findElement(By.xpath(".//div[@role='option']"));
                        if (optionElement != null){
                            autocompleteElement = optionElement;
                            break;
                        }
                    }
                    break;
                }

            } catch (StaleElementReferenceException e) {
                System.out.println("Stale element, retrying");
                continue;
            } catch (TimeoutException e) {
                driver.findElement(By.cssSelector("body")).click();
                bar.click();
                bar.clear();
                bar.sendKeys(input);
            }
        }

        if (autocompleteElement == null) {
            return "";
        }

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", autocompleteElement);
        return searchForm.findElement(By.id(idName)).getAttribute("value");
    }

    private void inputDate(WebElement searchForm, Date date, WebDriver webDriver) {
        WebElement dateInput = searchForm.findElement(By.id("dateInput-from"));
        Calendar today = Calendar.getInstance();
        Calendar given = Calendar.getInstance();
        given.setTime(date);

        boolean isToday = today.get(Calendar.YEAR) == given.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == given.get(Calendar.DAY_OF_YEAR);

        String[] months = {"sty", "lut", "mar", "kwi", "maj", "cze", "lip", "sie", "wrz", "paź", "lis", "gru"};
        int day = given.get(Calendar.DAY_OF_MONTH);
        String month = months[given.get(Calendar.MONTH)];

        String dateStr;
        if (isToday) {
            dateStr = "Dzisiaj, %02d %s".formatted(day, month);
        } else {
            String[] daysOfWeek = {"Nd", "Pn", "Wt", "Śr", "Cz", "Pt", "So"};
            String dayOfWeek = daysOfWeek[given.get(Calendar.DAY_OF_WEEK) - 1];
            dateStr = "%s, %02d %s".formatted(dayOfWeek, day, month);
        }
        JavascriptExecutor js = (JavascriptExecutor) webDriver;
        String script = "document.querySelector('#dateInput-from').value='"+dateStr+"';";
        js.executeScript(script);

        String updatedValue = webDriver.findElement(By.id("dateInput-from")).getAttribute("value");

    }

    private void search(WebElement searchForm, WebDriver driver) {
        WebElement searchButton = searchForm.findElement(By.className("lKKy1"));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", searchButton);
        //searchButton.click();
    }

    private String extractStartTime(WebElement connection) {
        WebElement departureTimeElement = connection.findElement(By.cssSelector("[data-e2e='search-result-departure-time']"));
        WebElement departureTime = departureTimeElement.findElement(By.cssSelector("[aria-hidden='true']"));
        String departureTimeText = departureTime.getAttribute("textContent");
        return departureTimeText;
    }
    private String extractEndTime(WebElement connection) {
        WebElement arrivalTimeElement = connection.findElement(By.cssSelector("[data-e2e='search-result-arrival-time']"));
        WebElement arrivalTime = arrivalTimeElement.findElement(By.cssSelector("[aria-hidden='true']"));
        String arrivalTimeText = arrivalTime.getAttribute("textContent");
        return arrivalTimeText;
    }

    private Double getPrice(WebElement connection) {
        WebElement price = connection.findElement(By.cssSelector("[data-e2e='search-result-prices']"));
        String html = price.getAttribute("innerHTML") != null ? price.getAttribute("innerHTML") : "00,00";
        String text = html.replaceAll("<[^>]*>", "");
        text = text.replace("\u00A0", "").replace("&nbsp;", "").trim();
        Pattern pattern = Pattern.compile("\\d+,\\d+");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String number = matcher.group();
            return Double.valueOf(number.replace(",", "."));
        }

        return 0.0;
    }

    private String getVehicleId(WebElement connection) {
        WebElement vehicleId = connection.findElement(By.className("train-number-details"));
        return vehicleId.getAttribute("innerHTML");
    }

    private OfferDto map(WebDriver webDriver, WebElement connection, String src, String dest, Date time) {

        Date day = time;
        Date startTime = combineDateAndTime(day, extractStartTime(connection));
        Date endTime = combineDateAndTime(day, extractEndTime(connection));

        Double cost = getPrice(connection);

        List<VehicleDto> vehicles = getVehicles(connection, day, startTime, endTime);

        return new OfferDto(
                src,
                dest,
                startTime,
                endTime,
                cost,
                vehicles,
                VehicleType.BUS
        );

    }

    private int getDuration(WebElement connection) {
        WebElement time = connection.findElement(By.className("travel-time-value"));
        return parseDuration(time.getAttribute("innerHTML"));
    }

    public static int parseDuration(String timeStr) {
        timeStr = timeStr.trim().toLowerCase();
        if (timeStr.contains("h")) {
            String[] parts = timeStr.split("h");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            return hours * 60 + minutes;
        }

        String[] parts = timeStr.split(" ");
        int minutes = Integer.parseInt(parts[0]);
        return minutes;
    }

    private List<VehicleDto> getVehicles(WebElement connection, Date day, Date start, Date end) {
        return Collections.singletonList(
                new VehicleDto(
                        "FlixBus", start, end
                )
        );
    }

    private VehicleDto getVehicle(WebElement train, Date day, Date previousTime) {
        WebElement id = train.findElement(By.className("train-number-details"));
        List<WebElement> times = train.findElements(By.className("scheduled-part"));
        WebElement start = times.getFirst();
        WebElement end = times.getLast();
        Date startDate = combineDateAndTime(day, extractTime(start.getAttribute("innerHTML")));
        Date endDate = combineDateAndTime(day, extractTime(end.getAttribute("innerHTML")));

        if (previousTime != null && previousTime.after(startDate)) {
            startDate = addToDate(startDate, 1440);
        }

        if (endDate != null && endDate.before(startDate)) {
            endDate = addToDate(endDate, 1440);
        }

        return new VehicleDto(
                id.getAttribute("innerHTML"),
                startDate,
                endDate
        );
    }

    private String extractTime(String input) {
        Pattern timePattern = Pattern.compile("\\b(\\d{1,2}):(\\d{2})\\b");
        Matcher matcher = timePattern.matcher(input);

        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    private Date combineDateAndTime(Date date, String timeString) {
        System.out.println(timeString);
        System.out.println(date);
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

    private Date addToDate(Date date, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minutes);
        return calendar.getTime();
    }
}
