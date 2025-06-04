package edu.pg.to_import.controller;

import edu.pg.to_import.dto.ImportCommand;
import edu.pg.to_import.dto.OfferInsertCommand;
import edu.pg.to_import.dto.OfferUpdateDto;
import edu.pg.to_import.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PutMapping
    public void importData(@RequestBody ImportCommand importCommand) {
        importService.importData(importCommand);
    }

    @GetMapping("/generate/{no}")
    public List<OfferInsertCommand> generate(@PathVariable int no) {
        return importService.generate(no);
    }

    @GetMapping("/updates/last")
    public List<OfferUpdateDto> getLastUpdates() {
        return importService.getLastUpdates();
    }

    @GetMapping("/updates")
    public SseEmitter getUpdates() {
        SseEmitter emitter = new SseEmitter(-1L);
        importService.getUpdatesSink().subscribe(u -> {
            try {
                emitter.send(u);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return emitter;
    }
}
