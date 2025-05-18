package com.codex.taxitrajectory.model.result;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import lombok.Data;

import java.util.Set;

@Data
public class RegionQueryResult {
    private int taxiCount;           // 出租车数量
    private Set<TaxiRecord> gpsPoints; // 出租车点集合
}