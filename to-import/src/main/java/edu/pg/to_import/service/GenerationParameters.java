package edu.pg.to_import.service;

import java.util.List;

public interface GenerationParameters {
    Double COST_LOWER_BOUND = 10.0;
    Double COST_UPPER_BOUND = 200.0;
    Integer MAX_SEATS_LOWER_BOUND = 10;
    Integer MAX_SEATS_UPPER_BOUND = 100;
    Integer DAYS_OFFSET_LIMIT = 30;
    Integer HOURS_OFFSET_LIMIT = 24;
    Integer MINUTES_OFFSET_LIMIT = 60;
    Integer DURATION_LIMIT = 480;

    static List<String> getAllCities() {
        return List.of("Wrocław", "Gdańsk", "Poznań", "Lublin", "Warszawa", "Bydgoszcz", "Kraków", "Gdynia", "Olsztyn", "Sopot");
    }

    static List<String> getAllVehicleIds() {
        return List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20");
    }
}
