package com.codex.taxitrajectory.model.core; // 确认包名是否正确

import com.codex.taxitrajectory.model.core.PointCoordinate; // 引入我们刚创建的类
import lombok.Data;

import java.util.List;
import java.util.Objects;

@Data
public class PathFrequency {
    // private Path path; // 旧的字段，可以选择移除或保留
    private List<PointCoordinate> pathCoordinates; // 新增：存储路径的网格中心点坐标序列
    private int frequency;

    // Jackson等库进行JSON序列化时可能需要一个无参构造函数
    public PathFrequency() {
    }

    // 修改构造函数以接收坐标列表
    public PathFrequency(List<PointCoordinate> pathCoordinates, int frequency) {
        // this.path = null; // 如果移除了旧字段
        this.pathCoordinates = pathCoordinates;
        this.frequency = frequency;
    }
}