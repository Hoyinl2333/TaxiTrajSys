package com.codex.taxitrajectory.model.core;

import lombok.Data;
import lombok.EqualsAndHashCode; // 使用 CallSuper=true
import lombok.ToString; // 使用 CallSuper=true

import java.util.Collections;
import java.util.List;

/**
 * 表示一条路径，现在由一系列有序的网格单元 ID 构成。
 */
@Data
@EqualsAndHashCode // 默认情况下，Lombok 会基于所有非静态、非 transient 字段生成 equals/hashCode
@ToString // 默认情况下，Lombok 会基于所有非静态、非 transient 字段生成 toString
public class Path {

    // 核心表示：网格单元 ID 的有序列表 (例如 "row,col")
    private final List<String> cellIdSequence;

    // (可选) 保留原始点或首尾点信息，如果需要的话
    // private List<GPSPoint> originalPoints;
    // private GPSPoint startPoint;
    // private GPSPoint endPoint;

    /**
     * 构造函数，接收一个不可变的网格单元 ID 序列。
     * @param cellIdSequence 不可变的网格单元 ID 列表。
     */
    public Path(List<String> cellIdSequence) {
        // 确保传入的是不可变列表或创建一个副本
        this.cellIdSequence = Collections.unmodifiableList(cellIdSequence);
    }

    // Getter 由 @Data 生成

    // 注意: Lombok 的 @Data 会自动生成 equals, hashCode, toString。
    // 它们现在会基于 cellIdSequence 来判断两个 Path 是否相等。
    // 如果你添加了其他字段（如 originalPoints），并且希望它们也参与比较，
    // 可能需要手动实现 equals/hashCode，或者使用 Lombok 的 @EqualsAndHashCode(callSuper=true) 等注解。
}