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
@EqualsAndHashCode
@ToString
public class Path {

    // 核心表示：网格单元 ID 的有序列表 (例如 "row,col")
    private final List<String> cellIdSequence;

    /**
     * 构造函数，接收一个不可变的网格单元 ID 序列。
     * @param cellIdSequence 不可变的网格单元 ID 列表。
     */
    public Path(List<String> cellIdSequence) {
        // 确保传入的是不可变列表或创建一个副本
        this.cellIdSequence = Collections.unmodifiableList(cellIdSequence);
    }

}