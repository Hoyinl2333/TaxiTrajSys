package com.codex.taxitrajectory.model;

import lombok.Data;

import java.util.Set;

@Data
public class RegionQueryResult {
    private int taxiCount;           // 出租车数量
    private Set<String> taxiIds;     // 出租车ID集合

}