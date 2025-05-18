package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.*;
import com.codex.taxitrajectory.model.query.DensityQuery;
import com.codex.taxitrajectory.repository.DataLoader;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 车流密度分析服务类
 */
@Service
public class DensityAnalysisService {

    private final DataLoader dataLoader;

    public DensityAnalysisService(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    /**
     * 分析区域车流密度
     * @query 使用DensityQuery类请求
     * @return 车流密度分析结果
     */
    public DensityAnalysisResult analyzeTrafficDensity(DensityQuery query) {

        // 从Query对象中提取参数
        double gridSize = query.getGridSize();
        LocalDateTime startTime = query.getStartTime();
        LocalDateTime endTime = query.getEndTime();
        int timeSlotMinutes = query.getTimeSlotMinutes();
        double minLon = query.getMinLongitude();
        double minLat = query.getMinLatitude();
        double maxLon = query.getMaxLongitude();
        double maxLat = query.getMaxLatitude();


        // 初始化网格
        Grid grid = new Grid(gridSize, minLon, minLat, maxLon, maxLat);

        // 处理所有出租车数据
        Set<String> allTaxiIds = dataLoader.getAllTaxiIds();
        for (String taxiId : allTaxiIds) {
            NavigableMap<LocalDateTime, TaxiRecord> records = dataLoader.getRecordsByTaxiId(taxiId);

            // 获取时间范围内的数据
            NavigableMap<LocalDateTime, TaxiRecord> filteredRecords =
                    records.subMap(startTime, true, endTime, true);

            for (Map.Entry<LocalDateTime, TaxiRecord> entry : filteredRecords.entrySet()) {
                TaxiRecord record = entry.getValue();

                // 找到对应的网格单元并添加车辆记录
                GridCell cell = grid.getCellByPosition(record.getLongitude(), record.getLatitude());
                if (cell != null) {
                    cell.addTaxi(taxiId, record.getTimestamp());
                }
            }
        }

        // 生成时间槽列表
        List<LocalDateTime> timeSlots = generateTimeSlots(startTime, endTime, timeSlotMinutes);

        // 构建密度结果
        DensityAnalysisResult result = new DensityAnalysisResult();
        result.setGrid(grid);
        result.setTimeSlots(timeSlots);

        // 计算每个时间槽、每个单元格的密度
        Map<LocalDateTime, Map<String, Integer>> densityMap = new HashMap<>();
        for (LocalDateTime timeSlot : timeSlots) {
            Map<String, Integer> cellDensities = new HashMap<>();

            for (GridCell cell : grid.getAllCells()) {
                int density = cell.getDensity(timeSlot);
                cellDensities.put(cell.getRow() + "," + cell.getCol(), density);
            }

            densityMap.put(timeSlot, cellDensities);
        }

        result.setDensityMap(densityMap);
        return result;
    }

    /**
     * 生成时间槽列表
     */
    private List<LocalDateTime> generateTimeSlots(
            LocalDateTime startTime,
            LocalDateTime endTime,
            int timeSlotMinutes) {

        List<LocalDateTime> timeSlots = new ArrayList<>();
        LocalDateTime current = startTime;

        while (!current.isAfter(endTime)) {
            timeSlots.add(current);
            current = current.plusMinutes(timeSlotMinutes);
        }

        return timeSlots;
    }
}
