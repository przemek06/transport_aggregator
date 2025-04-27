package edu.pg;

import edu.pg.model.OfferDto;
import edu.pg.model.QueryDto;
import edu.pg.scraper.FlixBusScraper;
import edu.pg.scraper.IntercityWebScraper;
import edu.pg.scraper.PolregioWebScraper;
import edu.pg.scraper.ResultGenerator;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        PolregioWebScraper polregioWebScraper = new PolregioWebScraper();
        IntercityWebScraper intercityWebScraper = new IntercityWebScraper();
        FlixBusScraper flixBusScraper = new FlixBusScraper();
        ResultGenerator resultGenerator = new ResultGenerator(
                Arrays.asList(
//                        intercityWebScraper,
//                        flixBusScraper,
                        polregioWebScraper
                        )
        );
        resultGenerator.generateResults();
    }
}