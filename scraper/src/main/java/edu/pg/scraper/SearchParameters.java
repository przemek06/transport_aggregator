package edu.pg.scraper;

import edu.pg.util.Pair;

import java.util.Arrays;
import java.util.List;

public interface SearchParameters {
    int DAYS = 30;
    List<String> HOURS = Arrays.asList("08:00", "17:00");
    String START_DATE = "30.04.2025";
    List<Pair<String, String>> STATIONS = Arrays.asList(
            Pair.of("Gdańsk", "Wrocław"),
            Pair.of("Gdańsk", "Szczecin"),
            Pair.of("Gdańsk", "Warszawa"),
            Pair.of("Gdańsk", "Kraków"),
            Pair.of("Wrocław", "Szklarska Poręba"),
            Pair.of("Wrocław", "Wałbrzych"),
            Pair.of("Wałbrzych", "Szklarska Poręba"),
            Pair.of("Gdynia", "Bydgoszcz"),
            Pair.of("Poznań", "Tczew"),
            Pair.of("Poznań", "Gdańsk"),
            Pair.of("Wrocław", "Gdańsk")
            );
}
