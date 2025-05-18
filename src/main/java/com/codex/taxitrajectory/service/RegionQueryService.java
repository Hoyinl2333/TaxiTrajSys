package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.core.TaxiRecord;
import com.codex.taxitrajectory.model.query.RegionQuery;
import com.codex.taxitrajectory.model.result.RegionQueryResult;
import com.codex.taxitrajectory.repository.TaxiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream; // 引入 Stream

@Service
public class RegionQueryService {
    private final TaxiRepository taxiRepository;

    private static final Logger logger = LoggerFactory.getLogger(RegionQueryService.class);
    @Value("${logging.service.enabled:true}")
    private boolean enableLogging; // 注入是否开启log配置，default:true

    public RegionQueryService(TaxiRepository taxiRepository) {
        this.taxiRepository = taxiRepository;
    }

    public RegionQueryResult getTaxisInRegion(RegionQuery query) {
        long startTimeMillis = System.currentTimeMillis();
        if (enableLogging) {
            logger.info("开始查询区域：区域边界=[minLon:{}, minLat:{}, maxLon:{}, maxLat:{}], 时间范围=[{} - {}]",
                    query.getMinLongitude(), query.getMinLatitude(),
                    query.getMaxLongitude(), query.getMaxLatitude(),
                    query.getStartTime(), query.getEndTime());
        }

        Set<String> taxiIds = ConcurrentHashMap.newKeySet();
        Set<TaxiRecord> gpsPoints = ConcurrentHashMap.newKeySet();

        taxiRepository.getAllTaxiIds().parallelStream().forEach(taxiId -> {
            // 使用 try-with-resources确保流能被关闭
            try (Stream<TaxiRecord> recordStream = taxiRepository.streamRecordsByTaxiId(taxiId)) {
                Optional<TaxiRecord> firstMatchingRecord = recordStream
                        .filter(record -> {
                            // 详细的 Null 检查，确保健壮性
                            if (record == null || record.getTimestamp() == null || record.getLongitude() == null || record.getLatitude() == null) {
                                if (enableLogging && record != null) { // 只在 record 不为 null 时记录 taxiId
                                    logger.trace("区域查询：跳过出租车 {} 的无效记录 (null fields): {}", taxiId, record);
                                } else if (enableLogging) {
                                    logger.trace("区域查询：跳过出租车 {} 的 null 记录对象", taxiId);
                                }
                                return false;
                            }
                            // 时间过滤 (包含开始和结束时间点)
                            boolean inTimeRange = !record.getTimestamp().isBefore(query.getStartTime()) && !record.getTimestamp().isAfter(query.getEndTime());
                            return inTimeRange;
                        })
                        .filter(record -> {
                            // 空间过滤 (假设经纬度在上一步已确保非null)
                            boolean inRegion = record.getLongitude() >= query.getMinLongitude() && record.getLongitude() <= query.getMaxLongitude() &&
                                    record.getLatitude() >= query.getMinLatitude() && record.getLatitude() <= query.getMaxLatitude();
                            return inRegion;
                        })
                        .findFirst(); // 找到第一个符合条件的记录即可

                if (firstMatchingRecord.isPresent()) {
                    taxiIds.add(taxiId);
                    gpsPoints.add(firstMatchingRecord.get());
                    if (enableLogging) {
                        logger.trace("区域查询：出租车 {} 在区域内找到匹配记录: {}", taxiId, firstMatchingRecord.get());
                    }
                }
            } catch (IOException e) {
                // 处理从 taxiRepository.streamRecordsByTaxiId(taxiId) 抛出的 IOException
                if (enableLogging) {
                    logger.error("流式查询区域时，处理出租车 {} 的数据流发生IO异常: {}", taxiId, e.getMessage(), e);
                }
                // 根据策略决定是否继续处理其他出租车，这里选择继续
            } catch (Exception e) {
                // 捕获流操作中可能发生的其他运行时异常
                if (enableLogging) {
                    logger.error("流式查询区域时，处理出租车 {} 时发生意外错误: {}", taxiId, e.getMessage(), e);
                }
            }
        });

        long endTimeMillis = System.currentTimeMillis();
        if(enableLogging) {
            logger.info("流式区域查询完成：匹配出租车数={}; 总耗时={} ms", taxiIds.size(), (endTimeMillis - startTimeMillis));
        }
        RegionQueryResult result = new RegionQueryResult();
        result.setTaxiCount(taxiIds.size());
        result.setGpsPoints(gpsPoints);
        return result;
    }
}