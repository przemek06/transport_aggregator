package edu.pg.intercity.rpc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pg.intercity.dto.OfferDto;
import edu.pg.intercity.dto.QueryDto;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Component
public class WebScraper {

    private final Logger logger = LoggerFactory.getLogger(WebScraper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<OfferDto> all = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            this.all = loadOffers().stream()
                    .sorted(Comparator.comparing(OfferDto::startTime))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<OfferDto> getOffers(QueryDto query) {
        return this.all.stream()
                .filter(offer -> query.src() == null || (offer.src() != null && offer.src().toLowerCase().contains(query.src().strip().toLowerCase())))
                .filter(offer -> query.dest() == null || (offer.dest() != null && offer.dest().toLowerCase().contains(query.dest().strip().toLowerCase())))
                .filter(offer -> query.time() == null || offer.startTime().after(query.time()))
                .filter(offer -> offer.startTime().after(new Date()))
                .filter(offer -> query.maxCost() == null || query.maxCost() >= offer.cost())
                .limit(5)
                .toList();
    }

    private List<OfferDto> loadOffers() throws IOException {
        ClassPathResource resource = new ClassPathResource("intercity.json");
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {});
        }
    }

}
