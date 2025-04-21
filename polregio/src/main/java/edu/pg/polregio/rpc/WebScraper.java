package edu.pg.polregio.rpc;

import edu.pg.polregio.dto.OfferDto;
import edu.pg.polregio.dto.QueryDto;
import edu.pg.polregio.dto.VehicleDto;
import edu.pg.polregio.dto.VehicleType;
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

    private static final String URL = "https://polregio.pl/pl/";
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
            webDriver = new ChromeDriver(options);
            webDriver.get(URL);

            WebDriverWait wait = new WebDriverWait(webDriver, Duration.of(5, ChronoUnit.SECONDS));

            WebElement cookieConfirm = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.id("CybotCookiebotDialogBodyLevelButtonLevelOptinAllowAll")));
            cookieConfirm.click();

            WebElement searchForm = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.className("koleo-widget")));

            String src = inputStartLocation(webDriver, searchForm, source);
            String dest = inputEndLocation(webDriver, searchForm, destination);
            inputDate(searchForm, time, webDriver);
            search(searchForm);
            logger.info("Waiting for results");

            WebDriverWait longerWait = new WebDriverWait(webDriver, Duration.of(15, ChronoUnit.SECONDS));
            WebElement results = longerWait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.className("search-results"))
            );

            List<WebElement> connections = results.findElements(By.className("day-connections"))
                    .stream()
                    .flatMap(dayConnection -> dayConnection.findElements(By.className("has-train-nr")).stream())
                    .toList();

            List<WebElement> relevantConnections = connections.stream()
                    .limit(3)
                    .toList();

            WebDriver finalWebDriver = webDriver;
            return relevantConnections.stream()
                    .map(connection -> map(finalWebDriver, connection, src, dest))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (webDriver != null) {
                webDriver.quit();
            }
        }

    }

    private String inputStartLocation(WebDriver webDriver, WebElement searchForm, String src) {
        return inputAutoComplete(webDriver, searchForm, src, "start_station");
    }

    private String inputEndLocation(WebDriver webDriver, WebElement searchForm, String dest) {
        return inputAutoComplete(webDriver, searchForm, dest, "end_station");
    }

    private String inputAutoComplete(WebDriver driver,
                                     WebElement searchForm,
                                     String input,
                                     String className) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        WebElement bar = wait.until(d ->
                searchForm.findElements(By.className(className))
                        .stream()
                        .filter(WebElement::isDisplayed)
                        .findFirst()
                        .orElse(null)
        );

        WebElement container = (WebElement) ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].parentElement;", searchForm);

        bar.click();
        bar.clear();
        bar.sendKeys(input);

        WebElement autocompleteElement = null;

        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                autocompleteElement = wait.until(d -> {
                    for (WebElement el : container.findElements(By.className("autocomplete"))) {
                        if (el.isDisplayed() && el.isEnabled() && isInteractable(el, driver)) {
                            return el;
                        }
                    }
                    return null;
                });
                break;
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

        List<WebElement> items = autocompleteElement.findElements(By.xpath("./*"));
        if (!items.isEmpty()) {
            String result = items.get(0).getText();
            items.get(0).click();
            return result;
        }

        return "";
    }

    private boolean isInteractable(WebElement element, WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        return (Boolean) js.executeScript(
                "const el = arguments[0];" +
                        "const style = window.getComputedStyle(el);" +
                        "return style && style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0' && el.offsetParent !== null;",
                element
        );
    }

    private void inputDate(WebElement searchForm, Date date, WebDriver webDriver) {
        WebElement dateInput = searchForm.findElement(By.className("date"));
        String format = "dd-MM-yyyy HH:mm";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        String dateStr = sdf.format(date);
        JavascriptExecutor js = (JavascriptExecutor) webDriver;
        js.executeScript("arguments[0].value='%s';".formatted(dateStr), dateInput);
    }

    private void search(WebElement searchForm) {
        WebElement searchButton = searchForm.findElement(By.className("submit"));
        searchButton.click();
    }

    private String extractStartTime(WebElement connection) {
        WebElement time = connection.findElements(By.className("scheduled-part")).getFirst();
        return time.getText();
    }

    private Double getPrice(WebElement connection) {
        WebElement price = connection.findElement(By.className("price-parts"));
        String text = price.getAttribute("innerHTML") != null ? price.getAttribute("innerHTML") : "00,00";
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

    private OfferDto map(WebDriver webDriver, WebElement connection, String src, String dest) {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.of(20, ChronoUnit.SECONDS));

        connection = wait.until(
                ExpectedConditions.elementToBeClickable(connection)
        );
        connection.click();

        Date day = getDay(connection, webDriver);
        Date startTime = combineDateAndTime(day, extractStartTime(connection));
        int minutes = getDuration(connection);
        Date endTime = addToDate(startTime, minutes);

        Double cost = getPrice(connection);

        List<VehicleDto> vehicles = getVehicles(connection, day, startTime, endTime);

        return new OfferDto(
                src,
                dest,
                startTime,
                endTime,
                cost,
                vehicles,
                VehicleType.TRAIN
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
        List<WebElement> trains = connection.findElements(By.className("train"));

        if (trains.size() == 1) {
            String vehicleId = getVehicleId(connection);
            return Collections.singletonList(
                    new VehicleDto(
                            vehicleId, start, end
                    )
            );
        }

        AtomicReference<Date> previousTime = new AtomicReference<>();
        return trains.stream()
                .map(train -> getVehicle(train, day, previousTime.get()))
                .peek(v -> {
                    previousTime.set(v.end());
                })
                .toList();
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

    private Date getDay(WebElement connection, WebDriver webDriver) {
        try {
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(20));
            WebElement calendar = wait.until(ExpectedConditions.visibilityOfNestedElementsLocatedBy(
                    connection,
                    By.className("add-to-calendar")
            )).getFirst();

            WebElement download = calendar.findElement(By.tagName("a"));
            download = wait.until(ExpectedConditions.elementToBeClickable(download));
            ((JavascriptExecutor) webDriver).executeScript("arguments[0].scrollIntoView({block: 'center'});", download);
            ((JavascriptExecutor) webDriver).executeScript("arguments[0].click();", download);

            Path path = Paths.get(downloadDir);
            File downloaded = waitForFileDownload(path, "calendar.ics");
            String content = Files.readString(downloaded.toPath());
            Date result = extractDateFromFile(content);
            cleanup(path);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while loading/parsing dates");
        }
    }

    private Date extractDateFromFile(String content) throws ParseException {
        content = content.replaceAll("\\s+", "");
        Pattern pattern = Pattern.compile("URL:.*?/(\\d{2}-\\d{2}-\\d{4})_(\\d{2}:\\d{2})", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String datePart = matcher.group(1);
            String timePart = matcher.group(2);
            String dateTimeStr = datePart + " " + timePart;

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
            sdf.setTimeZone(TimeZone.getTimeZone("Europe/Warsaw"));

            return sdf.parse(dateTimeStr);
        }

        throw new RuntimeException("Error while loading/parsing dates");
    }

    public File waitForFileDownload(Path dir, String expectedFileName) throws InterruptedException {
        File file = dir.resolve(expectedFileName).toFile();
        while (!file.exists()) {
            Thread.sleep(500);
        }
        while (new File(file.getAbsolutePath() + ".crdownload").exists()) {
            Thread.sleep(500);
        }
        return file;
    }

    private void cleanup(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
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

    private Date addToDate(Date date, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minutes);
        return calendar.getTime();
    }
}
