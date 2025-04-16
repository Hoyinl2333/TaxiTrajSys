package com.codex.taxitrajectory.utils;

import com.codex.taxitrajectory.model.GPSPoint;
import lombok.Data;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;

import java.util.ArrayList;
import java.util.List;


// TODO: 完善文档

/**
 * R-Tree空间索引工具类
 * 使用JTS Topology Suite实现R-Tree索引
 * F3 F4使用
 */
public class RTreeIndex {
    private STRtree rTree;
    private List<GPSPointWrapper> points;

    public RTreeIndex() {
        this.rTree = new STRtree();
        this.points = new ArrayList<>();
    }

    /**
     * 将GPS点加入R-Tree索引
     */
    public void insert(GPSPoint point) {
        Coordinate coordinate = new Coordinate(point.getLongitude(), point.getLatitude());
        Envelope envelope = new Envelope(coordinate);
        GPSPointWrapper wrapper = new GPSPointWrapper(point);
        points.add(wrapper);
        rTree.insert(envelope, wrapper);
    }

    /**
     * 查询指定区域内的GPS点
     */
    public List<GPSPoint> query(double minLon, double minLat, double maxLon, double maxLat) {
        Envelope queryEnvelope = new Envelope(minLon, maxLon, minLat, maxLat);
        List<GPSPoint> results = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<GPSPointWrapper> items = rTree.query(queryEnvelope);

        for (GPSPointWrapper wrapper : items) {
            GPSPoint point = wrapper.getPoint();
            if (point.getLongitude() >= minLon && point.getLongitude() <= maxLon &&
                    point.getLatitude() >= minLat && point.getLatitude() <= maxLat) {
                results.add(point);
            }
        }
        return results;
    }

    /**
     * GPS点包装类，用于在R-Tree中存储GPS点
     */
    @Data
    private static class GPSPointWrapper {
        private GPSPoint point;

        public GPSPointWrapper(GPSPoint point) {
            this.point = point;
        }

    }
}


