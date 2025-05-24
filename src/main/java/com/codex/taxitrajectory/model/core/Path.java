package com.codex.taxitrajectory.model.core;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 表示一条抽象的行驶路径。
 * <p>
 * 该路径由一个有序的、不可变的网格单元ID序列 ({@code List<String>}) 定义。
 * 每个ID通常格式为 "row,col"，代表车辆经过的网格单元。
 * </p>
 */
@Data
public class Path {

    /**
     * 组成路径的网格单元ID的有序列表。
     * 例如：["0,1", "0,2", "1,2"]。
     * 此列表在构造后是不可变的。
     */
    private final List<String> cellIdSequence;


    /**
     * 构造函数，根据提供的网格单元ID序列创建一个新的 Path 对象。
     * <p>
     * 传入的 {@code cellIdSequence} 列表将被包装为一个不可修改的列表，
     * 以确保 {@code Path} 对象内部状态的不可变性。
     * </p>
     *
     * @param cellIdSequence 一个网格单元ID的列表。不应为 null。
     * @throws NullPointerException 如果 cellIdSequence 为 null。
     */
    public Path(List<String> cellIdSequence) {
        if (cellIdSequence == null) {
            throw new NullPointerException("路径的网格单元ID序列 (cellIdSequence) 不能为空。");
        }
        this.cellIdSequence = List.copyOf(cellIdSequence);
    }
}