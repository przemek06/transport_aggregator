package edu.pg.scraper;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pg.model.OfferDto;
import edu.pg.model.QueryDto;
import edu.pg.util.Pair;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

import static edu.pg.scraper.SearchParameters.*;

@RequiredArgsConstructor
public class ResultGenerator {

    private final static String DIRECTORY = "C:\\Users\\swiat\\Projects\\HADS\\transport-aggregator\\jsons\\";
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<WebScraper> webScrapers;

    public void generateResults() throws ParseException {
        for (WebScraper scraper: webScrapers) {
            generateResults(scraper, scraper.getFileName());
        }
    }

    public void generateResults(WebScraper webScraper, String fileName) throws ParseException {
        Date start = new SimpleDateFormat("dd.MM.yyyy").parse(START_DATE);
        List<Date> dates = generateDates(
                start,
                DAYS,
                HOURS
        );

        List<OfferDto> results = STATIONS.stream()
                .flatMap(pair ->
                    dates.stream().flatMap(date -> {
                        QueryDto query = new QueryDto(pair.getFirst(), pair.getSecond(), date);
                        System.out.println("Processing next");
                        for (int retry = 0; retry < 3; retry++) {
                            try {
                                return webScraper.getOffers(query).stream();
                            } catch (Exception e) {
                                System.out.println("Retrying");
                                e.printStackTrace();
                            }
                        }
                        return Collections.<OfferDto>emptyList().stream();
                    })
                )
                .distinct()
                .toList();

        writeListToJsonFile(results, fileName);
    }

    private List<Date> generateDates(Date start, int days, List<String> hours) {
        List<Date> dateList = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);

        for (int i = 0; i < days; i++) {
            for (String hour : hours) {
                String[] hourParts = hour.split(":");
                int hourOfDay = Integer.parseInt(hourParts[0]);
                int minute = Integer.parseInt(hourParts[1]);

                Calendar dateTime = (Calendar) calendar.clone();
                dateTime.add(Calendar.DAY_OF_MONTH, i);
                dateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                dateTime.set(Calendar.MINUTE, minute);

                dateList.add(dateTime.getTime());
            }
        }

        return dateList;
    }

    private List<Pair<String, String>> getAllPairs(List<String> inputList) {
        List<Pair<String, String>> pairsList = new ArrayList<>();

        for (String first : inputList) {
            for (String second : inputList) {
                if (!first.equals(second)) {
                    pairsList.add(new Pair<>(first, second));
                }
            }
        }

        return pairsList;
    }

    private void writeListToJsonFile(List<OfferDto> list, String fileName) {
        File file = new File(DIRECTORY + fileName);

        try {
            mapper.writeValue(file, list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
