package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.repository.TaxiRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/taxi")
public class TaxiController {

    private final TaxiRepository taxiRepository;

    public TaxiController(TaxiRepository taxiRepository) {
        this.taxiRepository = taxiRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<List<TaxiRecord>> getTaxiTrajectory(@PathVariable String id) {
        List<TaxiRecord> records = taxiRepository.getRecordsByTaxiIdAsList(id);
        return ResponseEntity.ok(records);
}
}