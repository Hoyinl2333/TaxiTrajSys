package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.GPSPoint;
import com.codex.taxitrajectory.model.TaxiRecord;
import com.codex.taxitrajectory.model.query.RegionQuery;
import com.codex.taxitrajectory.model.result.RegionQueryResult;
import com.codex.taxitrajectory.repository.DataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RegionQueryService {
    private final DataLoader dataLoader;

    private static final Logger logger = LoggerFactory.getLogger(RegionQueryService.class);
    @Value("${logging.service.enabled:true}")
    private boolean enableLogging; // 注入是否开启log配置，default:true

    public RegionQueryService(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    public RegionQueryResult getTaxisInRegion(RegionQuery query) {
        // 日志信息
        long startTime = System.currentTimeMillis();
        if (enableLogging) {
            logger.info("开始查询区域：区域边界=[{},{}, {}, {}], 时间范围=[{} - {}]",
                query.getMinLongitude(), query.getMinLatitude(),
                query.getMaxLongitude(), query.getMaxLatitude(),
                query.getStartTime(), query.getEndTime());
        }

        // 创建用于保存查询结果的集合
        Set<String> taxiIds = ConcurrentHashMap.newKeySet(); // 并发安全集合
        Set<GPSPoint> gpsPoints = ConcurrentHashMap.newKeySet();

        // 并行遍历所有出租车ID
        dataLoader.getAllTaxiIds().parallelStream().forEach(taxiId -> {
            // 根据时间范围获取该出租车的轨迹数据（已通过 TreeMap 进行排序）
            List<TaxiRecord> records = dataLoader.getRecordsByTimeRange(taxiId, query.getStartTime(), query.getEndTime());
            for (TaxiRecord record : records) {
                double lon = record.getLongitude();
                double lat = record.getLatitude();
                if (lon >= query.getMinLongitude() && lon <= query.getMaxLongitude() &&
                        lat >= query.getMinLatitude() && lat <= query.getMaxLatitude()) {
                    taxiIds.add(taxiId);
                    // 假设GPSPoint的构造函数为 (lon, lat, timestamp, taxiId)
                    GPSPoint point = new GPSPoint(lon, lat, record.getTimestamp(), taxiId);
                    gpsPoints.add(point);
                    break; // 不必再继续遍历当前出租车的后续记录
                }
            }
        });

        // 查询结束日志信息打印
        long endTime = System.currentTimeMillis();
        if(enableLogging) {
            logger.info("区域查询完成：匹配出租车数={}; 总耗时={} ms", taxiIds.size(), (endTime - startTime));
        }
        RegionQueryResult result = new RegionQueryResult();
        result.setTaxiCount(taxiIds.size());
        result.setGpsPoints(gpsPoints);
        return result;
    }
}
