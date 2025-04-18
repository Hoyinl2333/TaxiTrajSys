package com.codex.taxitrajectory.service;

import com.codex.taxitrajectory.model.core.*;
import com.codex.taxitrajectory.model.query.FrequentPathQuery;
import com.codex.taxitrajectory.model.result.FrequentPathResult;
import com.codex.taxitrajectory.repository.TaxiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FrequentPathService {

    private static final Logger logger = LoggerFactory.getLogger(FrequentPathService.class);

    // 相似度阈值 (基于 Jaccard 相似度，可调)
    private static final double SIMILARITY_THRESHOLD = 0.4;
    private static final double EARTH_RADIUS = 6371.0; // km

    private final TaxiRepository taxiRepository;
    private final Grid grid; // 持有 Grid 实例

    // 定义全局网格参数 (硬编码或从配置注入)
    // TODO: 根据实际数据范围调整这些值，实际上应该可以采用北京市区也行
    private static final double GRID_MIN_LON = 115.7;
    private static final double GRID_MIN_LAT = 39.4;
    private static final double GRID_MAX_LON = 117.4;
    private static final double GRID_MAX_LAT = 41.1;
    private static final double GRID_SIZE_KM = 3; // 网格边长（单位km）
    
    private static final long MIN_TRANSITION_FREQUENCY = 50;
    private static final int MAX_PATH_RECONSTRUCTION_LENGTH = 50;
    private static final int MAX_PATHS_PER_START_NODE = 5;

    public FrequentPathService(TaxiRepository taxiRepository) {
        this.taxiRepository = taxiRepository;
        // 初始化 Grid
        this.grid = new Grid(GRID_SIZE_KM, GRID_MIN_LON, GRID_MIN_LAT, GRID_MAX_LON, GRID_MAX_LAT);
        logger.info("FrequentPathService initialized with Grid (size={}km, rows={}, cols={}).",
                GRID_SIZE_KM, grid.getRows(), grid.getCols());
    }

    public FrequentPathResult findFrequentPaths(FrequentPathQuery query) {
        long methodStart = System.currentTimeMillis();
        logger.info(
                "Starting findFrequentPaths with parameters: k = {}, minDistance = {}, startTime = {}, endTime = {}",
                query.getK(), query.getMinDistance(), query.getStartTime(), query.getEndTime());

        int k = query.getK();
        double minDistance = query.getMinDistance();

        // 1. 获取所有 Taxi IDs
        long taxiIdStart = System.currentTimeMillis();
        Set<String> taxiIds = taxiRepository.getAllTaxiIds();
        long taxiIdDuration = System.currentTimeMillis() - taxiIdStart;
        logger.info("Retrieved {} taxi IDs in {} ms.", taxiIds.size(), taxiIdDuration);

        // 2. 并行提取所有符合条件的路径 (网格化)
        long pathExtractionStart = System.currentTimeMillis();
        List<Path> allPaths = taxiIds.parallelStream()
                .flatMap(taxiId -> {
                    List<TaxiRecord> records = taxiRepository.getRecordsByTimeRange(
                            taxiId, query.getStartTime(), query.getEndTime());
                    // 使用修正后的路径提取逻辑
                    return extractPathsForTaxi(taxiId, records, minDistance, grid).stream();
                })
                .filter(Objects::nonNull) // 过滤掉可能的 null Path
                .collect(Collectors.toList());
        long pathExtractionDuration = System.currentTimeMillis() - pathExtractionStart;
        logger.info("Extracted {} valid paths in {} ms.", allPaths.size(), pathExtractionDuration);

        if (allPaths.isEmpty()) {
            logger.warn("No paths extracted for the given query parameters. Returning empty result.");
            FrequentPathResult emptyResult = new FrequentPathResult();
            emptyResult.setPathFrequencies(Collections.emptyList());
            return emptyResult;
        }

        // 3. 预聚合相同的路径序列
        long aggregationStart = System.currentTimeMillis();
        Map<List<String>, Long> uniquePathCounts = allPaths.stream()
                .collect(Collectors.groupingBy(
                        Path::getCellIdSequence,
                        Collectors.counting()
                ));
        long aggregationDuration = System.currentTimeMillis() - aggregationStart;
        logger.info("Aggregated into {} unique path patterns in {} ms.", uniquePathCounts.size(), aggregationDuration);

        List<PathFrequency> uniquePathFrequencies = uniquePathCounts.entrySet().stream()
                .map(entry -> new PathFrequency(new Path(entry.getKey()), entry.getValue().intValue()))
                .collect(Collectors.toList());

        // 4. 对唯一的路径模式进行聚类  (传入 pathSetCache)

        // 预计算pathSet
        long setPrecomputationStart = System.currentTimeMillis();
        Map<Path, Set<String>> pathSetCache = uniquePathFrequencies.stream()
                .map(PathFrequency::getPath)
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        path -> new HashSet<>(path.getCellIdSequence())
                ));
        long setPrecomputationDuration = System.currentTimeMillis() - setPrecomputationStart;
        logger.info("Precomputed HashSet representations for {} unique paths in {} ms.", pathSetCache.size(), setPrecomputationDuration);
     

         long clusteringStart = System.currentTimeMillis();
         List<ClusterInfo> clusters = clusterUniquePaths(uniquePathFrequencies, SIMILARITY_THRESHOLD, pathSetCache); // Pass cache
         long clusteringDuration = System.currentTimeMillis() - clusteringStart;
        logger.info("Clustering completed for unique patterns: {} clusters formed in {} ms.", clusters.size(), clusteringDuration);

        // 5. 计算最终聚类频率并获取 Top K
        long finalFreqStart = System.currentTimeMillis();
        List<PathFrequency> finalTopK = calculateFinalClusterFrequencies(clusters, k);
        long finalFreqDuration = System.currentTimeMillis() - finalFreqStart;
        logger.info("Final frequency calculation completed in {} ms.", finalFreqDuration);

        // 6. 返回结果
        FrequentPathResult result = new FrequentPathResult();
        result.setPathFrequencies(finalTopK);
        long totalDuration = System.currentTimeMillis() - methodStart;
        logger.info("findFrequentPaths completed in {} ms. Returning {} top frequent paths.", totalDuration, finalTopK.size());
        return result;
    }

    /**
     * 提取指定出租车在给定记录中满足最小距离要求的路径 (网格化表示)。
     * 距离累加基于连续轨迹点。
     */
    private List<Path> extractPathsForTaxi(String taxiId, List<TaxiRecord> records, double minDistance, Grid grid) {
        List<Path> paths = new ArrayList<>();
        if (records == null || records.size() < 2) {
            return paths;
        }

        List<String> currentTripCellIds = new ArrayList<>();
        String lastCellId = null;
        double currentDistance = 0.0;
        int startIndex = 0; // 记录当前路径段的起始点索引

        for (int i = 0; i < records.size() - 1; i++) {
            TaxiRecord current = records.get(i);
            TaxiRecord next = records.get(i + 1);

            // 1. 映射到网格单元 ID
            GridCell currentCell = grid.getCellByPosition(current.getLongitude(), current.getLatitude());
            if (currentCell == null) { // 如果当前点在网格 B 外，重置当前路径段
                if (!currentTripCellIds.isEmpty()) { // 检查之前的路径段是否满足条件
                    if (currentDistance >= minDistance) {
                        // 将 next 点的 cellId 加入（如果有效且不同）
                        GridCell nextCell = grid.getCellByPosition(next.getLongitude(), next.getLatitude());
                        if(nextCell != null) {
                            String nextCellId = nextCell.getRow() + "," + nextCell.getCol();
                            if (!nextCellId.equals(lastCellId)) {
                                currentTripCellIds.add(nextCellId);
                            }
                        }
                        if (!currentTripCellIds.isEmpty()) {
                             paths.add(new Path(new ArrayList<>(currentTripCellIds)));
                        }
                    }
                }
                // 重置
                currentTripCellIds.clear();
                currentDistance = 0.0;
                lastCellId = null;
                startIndex = i + 1; // 下一个点作为新路径段的开始
                continue;
            }
            String currentCellId = currentCell.getRow() + "," + currentCell.getCol();

            // 2. 添加网格 ID (如果变化)
            if (currentTripCellIds.isEmpty()) { // 如果是新路径段的开始
                currentTripCellIds.add(currentCellId);
                lastCellId = currentCellId;
                startIndex = i; // 记录起始索引
            } else if (!currentCellId.equals(lastCellId)) {
                currentTripCellIds.add(currentCellId);
                lastCellId = currentCellId;
            }

            // 3. 累加【实际行驶距离】(current 到 next)
            currentDistance += calculateDistance(current, next);

            // 4. 检查是否满足最小距离条件
            if (currentDistance >= minDistance) {
                // 将 next 点的 cellId 加入（如果有效且不同）
                 GridCell nextCell = grid.getCellByPosition(next.getLongitude(), next.getLatitude());
                 if(nextCell != null) {
                     String nextCellId = nextCell.getRow() + "," + nextCell.getCol();
                     // 只有当 nextCell 不同于当前路径最后一个 cell 时才添加
                     if (lastCellId == null || !nextCellId.equals(lastCellId)) {
                         currentTripCellIds.add(nextCellId);
                         lastCellId = nextCellId; // 更新 lastCellId
                     }
                 }
                 // 只有在路径有效时才添加
                 if (!currentTripCellIds.isEmpty()) {
                     paths.add(new Path(new ArrayList<>(currentTripCellIds))); // 创建 Path
                 }

                // 重置，开始新的路径段查找，从 next 点开始
                currentTripCellIds.clear();
                currentDistance = 0.0;
                lastCellId = null;
                startIndex = i + 1; // 新的路径段从 next (即索引 i+1) 开始
            }
        }

        // 注意：循环结束后，不需要特殊处理剩余路径段，因为只有当距离>=minDistance时才生成路径。
        // 如果最后一段距离不足，它自然就不会被添加。

        return paths;
    }


    /**
     * 计算两点之间的球面距离 (Haversine 公式)。
     */
    private double calculateDistance(TaxiRecord r1, TaxiRecord r2) {
        double lat1 = Math.toRadians(r1.getLatitude());
        double lon1 = Math.toRadians(r1.getLongitude());
        double lat2 = Math.toRadians(r2.getLatitude());
        double lon2 = Math.toRadians(r2.getLongitude());
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return EARTH_RADIUS * c; // 返回公里数
    }

    /**
     * 用于存储聚类信息的内部类
     */
    private static class ClusterInfo {
        Path representative;
        List<PathFrequency> members;

        ClusterInfo(Path representative) {
            this.representative = representative;
            this.members = new ArrayList<>();
        }
        void addMember(PathFrequency member) { this.members.add(member); }
        int getTotalFrequency() { return members.stream().mapToInt(PathFrequency::getFrequency).sum(); }
    }

 
    /**
     * 对唯一的路径模式进行聚类 (添加了详细日志)
     */
    private List<ClusterInfo> clusterUniquePaths(List<PathFrequency> uniquePaths,
                                                double similarityThreshold,
                                                Map<Path, Set<String>> pathSetCache) { // 接收缓存
        List<ClusterInfo> clusters = new ArrayList<>();
        uniquePaths.sort((pf1, pf2) -> Integer.compare(pf2.getFrequency(), pf1.getFrequency()));

        long totalOuterLoopNanos = 0; // 外层循环总时间
        long totalSimilarityNanos = 0; // 所有相似度计算总时间
        int pathsProcessed = 0;
        final int totalUniquePaths = uniquePaths.size();
        // 更智能的日志间隔，避免在路径少时打印过多，路径多时打印过少
        final int logInterval = Math.max(1, Math.min(1000, totalUniquePaths / 20)); // 大约打印20次

        long outerLoopStartNanos = System.nanoTime(); // 外层循环开始时间

        for (PathFrequency currentPathFreq : uniquePaths) {
            Path currentPath = currentPathFreq.getPath();
            ClusterInfo bestMatchCluster = null;
            double bestSimilarity = -1.0;

            long innerLoopStartNanos = System.nanoTime(); // 内层循环（相似度计算）开始时间
            // *** 内部循环 ***
            for (ClusterInfo cluster : clusters) {
                // 使用预计算的 Set 进行相似度计算
                double similarity = calculatePathSimilarity(currentPath, cluster.representative, pathSetCache);
                if (similarity >= similarityThreshold && similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestMatchCluster = cluster;
                }
            }
            long innerLoopDurationNanos = System.nanoTime() - innerLoopStartNanos; // 内层循环耗时
            totalSimilarityNanos += innerLoopDurationNanos; // 累加相似度计算时间

            // 将路径添加到聚类
            if (bestMatchCluster != null) {
                bestMatchCluster.addMember(currentPathFreq);
            } else {
                ClusterInfo newCluster = new ClusterInfo(currentPath);
                newCluster.addMember(currentPathFreq);
                clusters.add(newCluster);
            }

            pathsProcessed++;
            // 定期打印日志
//            if (pathsProcessed % logInterval == 0 || pathsProcessed == totalUniquePaths) {
//                totalOuterLoopNanos = System.nanoTime() - outerLoopStartNanos;
//                long avgSimilarityNanos = (pathsProcessed == 0) ? 0 : totalSimilarityNanos / pathsProcessed; // 平均每次外层循环的相似度计算耗时
//                logger.info("[F7 Clustering] Processed {}/{} unique paths. Clusters: {}. Total: {} ms. Similarity Calc Total: {} ms (Avg per path: {} ns).",
//                        pathsProcessed, totalUniquePaths, clusters.size(),
//                        TimeUnit.NANOSECONDS.toMillis(totalOuterLoopNanos), // 转毫秒
//                        TimeUnit.NANOSECONDS.toMillis(totalSimilarityNanos), // 转毫秒
//                        avgSimilarityNanos);
//            }
        }
        // （可选）最终的总时间日志已在外层记录，这里不再重复

        return clusters;
    }


    /**
     * 计算 Jaccard 相似度 (使用预计算的 Set)
     */
    private double calculatePathSimilarity(Path p1, Path p2, Map<Path, Set<String>> setCache) {
        Set<String> set1 = setCache.get(p1);
        Set<String> set2 = setCache.get(p2);
 
        if (set1 == null || set2 == null) {
             logger.error("Precomputed set not found for path(s) during similarity calculation! p1: {}, p2: {}", p1.getCellIdSequence(), p2.getCellIdSequence());
             return 0.0; // Or handle error appropriately
        }
        if (set1.isEmpty() && set2.isEmpty()) return 1.0;
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;
 
        Set<String> smallerSet = (set1.size() < set2.size()) ? set1 : set2;
        Set<String> largerSet = (set1.size() < set2.size()) ? set2 : set1;
        long intersectionSize = smallerSet.stream().filter(largerSet::contains).count();
        long unionSize = (long)set1.size() + set2.size() - intersectionSize; // Use long for safety
 
        return (unionSize == 0) ? 1.0 : (double) intersectionSize / unionSize; // Avoid division by zero if both empty
     }

    /**
     * 计算最终聚类的频率并返回Top K。
     */
    private List<PathFrequency> calculateFinalClusterFrequencies(List<ClusterInfo> clusters, int k) {
        return clusters.stream()
                .map(cluster -> new PathFrequency(cluster.representative, cluster.getTotalFrequency()))
                .sorted((pf1, pf2) -> Integer.compare(pf2.getFrequency(), pf1.getFrequency()))
                .limit(k)
                .peek(pf -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Final Cluster: Rep Path ({} cells): {}, Total Freq: {}",
                                pf.getPath().getCellIdSequence().size(),
                                pf.getPath().getCellIdSequence(),
                                pf.getFrequency());
                    }
                })
                .collect(Collectors.toList());
    }
}