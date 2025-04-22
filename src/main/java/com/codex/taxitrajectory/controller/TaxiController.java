package com.codex.taxitrajectory.controller;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.repository.TaxiRepository;
import com.codex.taxitrajectory.service.TrafficAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/taxi")
public class TaxiController {

    private final TaxiRepository taxiRepository;
    private final TrafficAnalysisService trafficAnalysisService;

    public TaxiController(TaxiRepository taxiRepository, TrafficAnalysisService trafficAnalysisService) {
        this.taxiRepository = taxiRepository;
        this.trafficAnalysisService = trafficAnalysisService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<List<TaxiRecord>> getTaxiTrajectory(@PathVariable String id) {
        List<TaxiRecord> records = taxiRepository.getRecordsByTaxiIdAsList(id);
        return ResponseEntity.ok(records);
}
}