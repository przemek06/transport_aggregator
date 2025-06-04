package edu.pg.to.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pg.to.dto.OfferDto;
import edu.pg.to.dto.OfferInsertCommand;
import edu.pg.to.service.OfferService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataImporter {
    private final OfferService offerService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        List<String> jsonFiles = List.of("polregio.json", "intercity.json", "flixbus.json");

        try {
            for (String file : jsonFiles) {
                ClassPathResource resource = new ClassPathResource(file);
                try (InputStream inputStream = resource.getInputStream()) {
                    List<OfferInsertCommand> offers = objectMapper.readValue(inputStream, new TypeReference<>() {});
                    offers = offers.stream().map(o -> o.update(60)).toList();
                    offerService.saveOffers(offers);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load offers from JSON files", e);
        }
    }
}
