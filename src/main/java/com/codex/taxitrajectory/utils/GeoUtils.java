package com.codex.taxitrajectory.utils;


import com.codex.taxitrajectory.model.core.Region;
import com.codex.taxitrajectory.model.core.TaxiRecord;
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

    private static final double EARTH_RADIUS_METERS = 6371000.0; // 地球平均半径 (米)

    /**
     * 使用 Haversine 公式计算两个 GPS 坐标点之间的距离.
     *
     * @param lat1 第一个点的纬度 (十进制度数).
     * @param lon1 第一个点的经度 (十进制度数).
     * @param lat2 第二个点的纬度 (十进制度数).
     * @param lon2 第二个点的经度 (十进制度数).
     * @return 两点之间的距离 (单位: 千米).
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 将度数转换为弧度
        double radLat1 = Math.toRadians(lat1);
        double radLon1 = Math.toRadians(lon1);
        double radLat2 = Math.toRadians(lat2);
        double radLon2 = Math.toRadians(lon2);

        // 计算经纬度差的弧度
        double deltaLat = radLat2 - radLat1;
        double deltaLon = radLon2 - radLon1;

        // Haversine 公式核心计算
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(radLat1) * Math.cos(radLat2) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // 计算距离
        return EARTH_RADIUS_METERS * c / 1000;
    }

    /**
     * (重载方法，方便直接传入 TaxiRecord - 可选)
     * 计算两个 TaxiRecord 点之间的距离.
     * @param r1 第一个 TaxiRecord.
     * @param r2 第二个 TaxiRecord.
     * @return 两点之间的距离 (米)，如果任一记录或坐标无效则返回 0.0.
     */
    public static double calculateDistance(TaxiRecord r1, TaxiRecord r2) {
        if (r1 == null || r2 == null) return 0.0;
        // 可以添加对经纬度是否为 0 或无效值的检查
        return calculateDistance(r1.getLatitude(), r1.getLongitude(), r2.getLatitude(), r2.getLongitude());
    }

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

}
