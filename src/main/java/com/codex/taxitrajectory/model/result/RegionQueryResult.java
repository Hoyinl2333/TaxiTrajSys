package com.codex.taxitrajectory.model.result;

import com.codex.taxitrajectory.model.core.GPSPoint;
import lombok.Data;

import java.util.Set;

@Data
public class RegionQueryResult {
    private int taxiCount;           // 出租车数量
    private Set<GPSPoint> gpsPoints; // 出租车点集合
}