package edu.pg.to_import.controller;

import edu.pg.to_import.dto.ImportCommand;
import edu.pg.to_import.dto.OfferInsertCommand;
import edu.pg.to_import.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}
