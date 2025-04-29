package com.codex.taxitrajectory.utils;

import com.codex.taxitrajectory.model.core.GPSPoint;
import com.codex.taxitrajectory.model.core.Region;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 地理工具类，提供地理计算功能
 */
@Component
public class GeoUtils {
    /**
     * 判断一个点是否在矩形区域内
     * @param longitude 点的经度
     * @param latitude 点的纬度
     * @param topLeftLongitude 矩形左上角的经度
     * @param topLeftLatitude 矩形左上角的纬度
     * @param bottomRightLongitude 矩形右下角的经度
     * @param bottomRightLatitude 矩形右下角的纬度
     * @return 如果点在矩形区域内返回 true，否则返回 false
     */
    public static boolean isInRectangle(double longitude, double latitude,
                                        double topLeftLongitude, double topLeftLatitude,
                                        double bottomRightLongitude, double bottomRightLatitude) {
        return longitude >= Math.min(topLeftLongitude, bottomRightLongitude) &&
                longitude <= Math.max(topLeftLongitude, bottomRightLongitude) &&
                latitude >= Math.min(topLeftLatitude, bottomRightLatitude) &&
                latitude <= Math.max(topLeftLatitude, bottomRightLatitude);
    }

    /**
     * 判断一个点 (latitude, longitude) 是否在给定的矩形区域内。
     * 这个方法是 TravelTimeService 中调用的入口。
     *
     * @param latitude 点的纬度
     * @param longitude 点的经度
     * @param region 矩形区域对象
     * @return 如果点在矩形区域内返回 true，否则返回 false
     */
    public static boolean isPointInRegion(double latitude, double longitude, Region region) {
        // 调用核心的判断点在矩形内的方法
        // 将 Region 对象的边界映射到 isInRectangle 方法的参数
        // 由于 isInRectangle 方法内部使用了 Math.min 和 Math.max，
        // 我们直接传递 Region 的 min/max 经纬度即可正确判断。
        return isInRectangle(longitude, latitude,
                region.getMinLon(), region.getMinLat(), // 矩形的一个角 (经度最小值, 纬度最小值)
                region.getMaxLon(), region.getMaxLat()); // 矩形的对角 (经度最大值, 纬度最大值)
    }

//    /**
//     * 计算两个集合的Jaccard相似度
//     *
//     * Jaccard相似度定义为两个集合交集大小与并集大小的比值，
//     * 适用于衡量两组数据（例如GridCell集合）之间的相似程度。
//     *
//     * @param <T> 集合元素的泛型
//     * @param set1 第一个集合
//     * @param set2 第二个集合
//     * @return Jaccard相似度（取值在0到1之间），值越高表示两个集合越相似
//     */
//    public static <T> double jaccardSimilarity(Set<T> set1, Set<T> set2) {
//        // 如果两个集合均为空，则认为相似度为1
//        if (set1.isEmpty() && set2.isEmpty()) {
//            return 1.0;
//        }
//        Set<T> intersection = new HashSet<>(set1);
//        intersection.retainAll(set2);
//        Set<T> union = new HashSet<>(set1);
//        union.addAll(set2);
//        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
//    }

//    /**
//     * 根据地理边界和网格边长计算 row 和 col 数量
//     * @param minLat 最小纬度
//     * @param maxLat 最大纬度
//     * @param minLon 最小经度
//     * @param maxLon 最大经度
//     * @param gridSizeKm 网格边长（单位：km）
//     * @return int[]{rowCount, colCount}
//     */
//    public static int[] calculateGridCount(double minLat, double maxLat, double minLon, double maxLon, double gridSizeKm) {
//        // 平均纬度用于估算经度所代表的距离
//        double avgLat = (minLat + maxLat) / 2.0;
//
//        // 每度纬度对应的千米数（大致常数）
//        double latPerKm = 1.0 / 110.574;
//
//        // 每度经度对应的千米数（取决于纬度）
//        double lonPerKm = 1.0 / (111.320 * Math.cos(Math.toRadians(avgLat)));
//
//        // 纬度差对应的距离 / 单个网格大小
//        int rowCount = (int) Math.ceil((maxLat - minLat) / (latPerKm * gridSizeKm));
//        int colCount = (int) Math.ceil((maxLon - minLon) / (lonPerKm * gridSizeKm));
//
//        return new int[]{rowCount, colCount};
//    }
}
