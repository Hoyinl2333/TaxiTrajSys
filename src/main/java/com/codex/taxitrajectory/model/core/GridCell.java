package com.codex.taxitrajectory.model.core;

import lombok.Data;
import lombok.Getter;
import lombok.EqualsAndHashCode; // Import if needed
import lombok.ToString; // Import if needed

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory

/**
 * 网格单元类 (修正版本)
 */
@Data
@EqualsAndHashCode(of = {"row", "col"}) // Example: equals/hashCode based only on row and col
@ToString(exclude = {"timeSlotTaxiIds", "taxiIdToIndex", "indexToTaxiId", "nextTaxiIdIndex"}) // Exclude large maps from toString
public class GridCell {
    private static final Logger logger = LoggerFactory.getLogger(GridCell.class); // Add logger if needed

    private final int row;
    private final int col;
    private final double minLon;
    private final double minLat;
    private final double maxLon;
    private final double maxLat;

    @Getter private final double centerLon; // Ensure Getter exists
    @Getter private final double centerLat; // Ensure Getter exists

    private final Map<LocalDateTime, BitSet> timeSlotTaxiIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> taxiIdToIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> indexToTaxiId = new ConcurrentHashMap<>();
    private final AtomicInteger nextTaxiIdIndex = new AtomicInteger(0);

    public GridCell(int row, int col, double minLon, double minLat, double maxLon, double maxLat) {
        this.row = row;
        this.col = col;
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        // Calculate center point
        this.centerLon = minLon + (maxLon - minLon) / 2.0;
        this.centerLat = minLat + (maxLat - minLat) / 2.0;
        // No taxiVisits initialization needed
    }

    public boolean contains(double lon, double lat) {
        return lon >= minLon && lon < maxLon && lat >= minLat && lat < maxLat;
        // Using < for max boundary is generally safer to prevent overlap issues
        // Also handle the case where lon==maxLon or lat==maxLat maps to the edge cell index
        // This needs coordination with Grid.getCellByPosition logic.
        // Let's stick to the safer < max boundary for contains check.
    }


    public void addTaxi(String taxiId, LocalDateTime timeSlot) {
        Integer taxiIndex = taxiIdToIndex.computeIfAbsent(taxiId, id -> {
            int index = nextTaxiIdIndex.getAndIncrement();
            indexToTaxiId.put(index, id);
            return index;
        });
        BitSet bitSet = timeSlotTaxiIds.computeIfAbsent(timeSlot, ts -> new BitSet());
        synchronized (bitSet) { // Keep synchronization for safety
            bitSet.set(taxiIndex);
        }
    }

    public int getDensity(LocalDateTime timeSlot) {
        BitSet bitSet = timeSlotTaxiIds.get(timeSlot);
        return (bitSet == null) ? 0 : bitSet.cardinality();
    }

    public Set<String> getTaxiIds(LocalDateTime timeSlot) {
        BitSet bitSet = timeSlotTaxiIds.get(timeSlot);
        if (bitSet == null) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
            String taxiId = indexToTaxiId.get(i);
            if (taxiId != null) {
                result.add(taxiId);
            } else {
                logger.warn("Could not find taxiId for index {} in GridCell ({},{})", i, row, col);
            }
            if (i == Integer.MAX_VALUE) break;
        }
        return result;
    }
    // Getters for centerLon/Lat provided by @Getter or @Data
}