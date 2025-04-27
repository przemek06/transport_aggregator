package edu.pg.scraper;

import edu.pg.model.OfferDto;
import edu.pg.model.QueryDto;

import java.util.List;

public interface WebScraper {
    List<OfferDto> getOffers(QueryDto query);
    String getFileName();
}
