package edu.pg.to_import.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pg.to_import.dto.ImportCommand;
import edu.pg.to_import.dto.OfferInsertCommand;
import edu.pg.to_import.dto.VehicleDto;
import edu.pg.to_import.dto.VehicleType;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${rabbit.import.exchange}")
    private String exchange;

    public void importData(ImportCommand importCommand) {
        try {
            String payload = objectMapper.writeValueAsString(importCommand);
            System.out.println(importCommand);
            rabbitTemplate.convertAndSend(exchange, "", payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public List<OfferInsertCommand> generate(int no) {
        List<OfferInsertCommand> result = new ArrayList<>();

        for (int i = 0; i < no; i++) {
            Random random = new Random();
            String src = GenerationParameters.getAllCities().get(random.nextInt(GenerationParameters.getAllCities().size()));
            String dest = GenerationParameters.getAllCities().get(random.nextInt(GenerationParameters.getAllCities().size()));
            if (src.equals(dest)) {
                i--;
                continue;
            }

            double cost = random.nextDouble(GenerationParameters.COST_LOWER_BOUND, GenerationParameters.COST_UPPER_BOUND);
            cost = Math.round(cost * 100.0) / 100.0;

            Instant now = Instant.now();
            int daysOffset = random.nextInt(GenerationParameters.DAYS_OFFSET_LIMIT);
            int hoursOffset = random.nextInt(GenerationParameters.HOURS_OFFSET_LIMIT);
            int minutesOffset = random.nextInt(GenerationParameters.MINUTES_OFFSET_LIMIT);
            Date start = Date.from(now.plusSeconds(daysOffset * 86400L + hoursOffset * 3600L + minutesOffset * 60L));

            int duration = random.nextInt(GenerationParameters.DURATION_LIMIT);
            Date end = Date.from(start.toInstant().plusSeconds(duration * 60L));

            String vehicleId = GenerationParameters.getAllVehicleIds().get(random.nextInt(GenerationParameters.getAllVehicleIds().size()));
            VehicleDto vehicle = new VehicleDto(vehicleId, start, end);

            int maxSeats = random.nextInt(GenerationParameters.MAX_SEATS_LOWER_BOUND, GenerationParameters.MAX_SEATS_UPPER_BOUND);

            OfferInsertCommand dto = OfferInsertCommand.builder()
                    .src(src)
                    .dest(dest)
                    .cost(cost)
                    .startTime(start)
                    .endTime(end)
                    .maxSeats(maxSeats)
                    .type(VehicleType.TRAIN)
                    .vehicles(List.of(vehicle))
                    .build();

            result.add(dto);
        }

        return result;
    }
}
