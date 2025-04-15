package com.codex.taxitrajectory.service.Path;

import org.springframework.stereotype.Service;

import com.codex.taxitrajectory.model.GridCell;
import com.codex.taxitrajectory.model.PathFrequency;
import com.codex.taxitrajectory.model.TaxiRecord;
import com.codex.taxitrajectory.repository.DataLoader;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.nio.file.Path;

@Service
public class FrequentPathService {
    // TODO: f7 implementation
//    // 相似度阈值（可根据实际情况调整）
//    private static final double SIMILARITY_THRESHOLD = 0.5;
//    // 地球半径（单位：km）
//    private static final double EARTH_RADIUS = 6371.0;
//
//    private final DataLoader dataLoader;
//
//    public FrequentPathService(DataLoader dataLoader) {
//        this.dataLoader = dataLoader;
//    }
//
//    /**
//     * 统计全城最频繁的前 k 条路径
//     * 用户参数：
//     *    k：返回的路径个数
//     *    minDistance：只有累计移动距离超过该值的路径才被考虑（单位：km）
//     *
//     * @param k 返回路径数量
//     * @param minDistance 最小路径累计距离（km）
//     * @return List<PathFrequency>，每个对象包含代表路径及该路径的出现次数
//     */
//    public List<PathFrequency> findFrequentPaths(int k, double minDistance) {
//        // 1. 获取所有出租车ID
//        Set<String> taxiIds = dataLoader.getAllTaxiIds();
//
//        // 2. 对每辆出租车并行提取符合条件的路径
//        List<Path> allPaths = taxiIds.parallelStream()
//                .flatMap(taxiId -> extractPathsForTaxi(taxiId, minDistance).stream())
//                .collect(Collectors.toList());
//
//        // 3. 对所有提取的路径按照相似性进行聚类
//        List<List<Path>> clusters = clusterSimilarPaths(allPaths, SIMILARITY_THRESHOLD);
//
//        // 4. 对每个聚类统计出现次数，并用聚类中第一条路径作为代表
//        Map<Path, Integer> frequencyMap = countClusterFrequencies(clusters);
//
//        // 5. 根据频次降序排序，取前 k 个
//        return frequencyMap.entrySet().stream()
//                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
//                .limit(k)
//                .map(entry -> new PathFrequency(entry.getKey(), entry.getValue()))
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * 对某个出租车提取所有满足累计距离大于 minDistance 的路径
//     *
//     * @param taxiId 出租车ID
//     * @param minDistance 最小路径累计距离（km）
//     * @return List<Path> 该出租车提取的路径集合
//     */
//    private List<Path> extractPathsForTaxi(String taxiId, double minDistance) {
//        List<Path> paths = new ArrayList<>();
//        // 获取该出租车按时间排序的轨迹记录
//        List<TaxiRecord> records = dataLoader.getRecordsByTaxiIdAsList(taxiId);
//        if (records == null || records.isEmpty()) {
//            return paths;
//        }
//
//        List<TaxiRecord> currentTrip = new ArrayList<>();
//        double totalDistance = 0.0;
//
//        // 遍历记录，累计两点间距离
//        for (int i = 0; i < records.size() - 1; i++) {
//            TaxiRecord current = records.get(i);
//            TaxiRecord next = records.get(i + 1);
//            currentTrip.add(current);
//
//            double distance = calculateDistance(current, next);
//            totalDistance += distance;
//
//            // 若累计距离达到阈值，则构成一条路径
//            if (totalDistance >= minDistance) {
//                // 可考虑将下一记录也加入以确保路径完整
//                currentTrip.add(next);
//                Path path = convertToPath(currentTrip);
//                paths.add(path);
//
//                // 重置当前行程
//                currentTrip = new ArrayList<>();
//                totalDistance = 0.0;
//            }
//        }
//        // 若最后剩余的路径满足要求则也加入
//        if (!currentTrip.isEmpty() && totalDistance >= minDistance) {
//            paths.add(convertToPath(currentTrip));
//        }
//        return paths;
//    }
//
//    /**
//     * 利用 Haversine 公式计算两个轨迹点之间的距离（单位：km）
//     */
//    private double calculateDistance(TaxiRecord r1, TaxiRecord r2) {
//        double lat1 = Math.toRadians(r1.getLatitude());
//        double lon1 = Math.toRadians(r1.getLongitude());
//        double lat2 = Math.toRadians(r2.getLatitude());
//        double lon2 = Math.toRadians(r2.getLongitude());
//
//        double dLat = lat2 - lat1;
//        double dLon = lon2 - lon1;
//
//        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
//                + Math.cos(lat1) * Math.cos(lat2)
//                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
//        double c = 2 * Math.asin(Math.sqrt(a));
//        return EARTH_RADIUS * c;
//    }
//
//    /**
//     * 将一段连续的 TaxiRecord 列表转换为 Path 对象
//     * 为了离散化路径，这里将每个记录转换为对应的 GridCell（您可以调用Grid.getCellByPosition()方法）
//     */
//    private Path convertToPath(List<TaxiRecord> tripRecords) {
//        List<GridCell> cells = tripRecords.stream()
//                // 这里简单构造 GridCell，您也可以采用位于项目配置中的 Grid 对象进行查找
//                .map(record -> new GridCell(-1, -1, record.getLongitude(), record.getLatitude(), record.getLongitude(), record.getLatitude()))
//                .collect(Collectors.toList());
//        return new Path(cells);
//    }
//
//    /**
//     * 对所有路径进行简单聚类——将相似度大于 similarityThreshold 的路径归为同一簇
//     */
//    private List<List<Path>> clusterSimilarPaths(List<Path> paths, double similarityThreshold) {
//        List<List<Path>> clusters = new ArrayList<>();
//        for (Path path : paths) {
//            boolean added = false;
//            // 依次判断是否加入已有簇
//            for (List<Path> cluster : clusters) {
//                Path representative = cluster.get(0); // 使用第一条路径作为簇代表
//                if (calculatePathSimilarity(path, representative) >= similarityThreshold) {
//                    cluster.add(path);
//                    added = true;
//                    break;
//                }
//            }
//            if (!added) {
//                List<Path> newCluster = new ArrayList<>();
//                newCluster.add(path);
//                clusters.add(newCluster);
//            }
//        }
//        return clusters;
//    }
//
//    /**
//     * 计算两条路径的相似度，方法采用 Jaccard 相似（基于各路径中 GridCell 的集合）
//     */
//    private double calculatePathSimilarity(Path p1, Path p2) {
//        Set<GridCell> set1 = new HashSet<>(p1.getGridCells());
//        Set<GridCell> set2 = new HashSet<>(p2.getGridCells());
//        Set<GridCell> intersection = new HashSet<>(set1);
//        intersection.retainAll(set2);
//        Set<GridCell> union = new HashSet<>(set1);
//        union.addAll(set2);
//        if (union.isEmpty()) {
//            return 0.0;
//        }
//        return (double) intersection.size() / union.size();
//    }
//
//    /**
//     * 统计每个聚类的路径数量，返回以聚类中代表路径为 key、聚类大小为 value 的映射
//     */
//    private Map<Path, Integer> countClusterFrequencies(List<List<Path>> clusters) {
//        Map<Path, Integer> freqMap = new HashMap<>();
//        for (List<Path> cluster : clusters) {
//            if (!cluster.isEmpty()) {
//                freqMap.put(cluster.get(0), cluster.size());
//            }
//        }
//        return freqMap;
//    }

}
