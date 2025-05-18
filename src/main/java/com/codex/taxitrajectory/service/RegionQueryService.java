package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.query.RegionQuery;
import com.codex.taxitrajectory.model.TaxiRecord;
import com.codex.taxitrajectory.repository.DataLoader;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RegionQueryService {

    private final DataLoader dataLoader;

    public RegionQueryService(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    /**
     * 区域范围查找 - 使用Query类参数
     */
    public int countTaxisInRegion(RegionQuery query) {
        Set<String> uniqueTaxiIds = new HashSet<>();

        // 获取所有出租车ID
        Set<String> allTaxiIds = dataLoader.getAllTaxiIds();

        for (String taxiId : allTaxiIds) {
            // 获取指定时间范围内的轨迹数据
            List<TaxiRecord> records = dataLoader.getRecordsByTimeRange(
                    taxiId,
                    query.getStartTime(),
                    query.getEndTime()
            );

            // 检查是否有轨迹点落在指定区域内
            for (TaxiRecord record : records) {
                if (record.getLongitude() >= query.getMinLongitude() &&
                        record.getLongitude() <= query.getMaxLongitude() &&
                        record.getLatitude() >= query.getMinLatitude() &&
                        record.getLatitude() <= query.getMaxLatitude()) {

                    uniqueTaxiIds.add(taxiId);
                    break; // 一旦找到一个点在区域内，就不需要继续检查该出租车
                }
            }
        }
        return uniqueTaxiIds.size();
    }
}
