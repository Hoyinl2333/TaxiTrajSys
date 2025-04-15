package com.codex.taxitrajectory.model;

import lombok.Data;

import java.nio.file.Path;

@Data
public class PathFrequency {
    private Path path;
    private int frequency;

    public PathFrequency(Path path, int frequency) {
        this.path = path;
        this.frequency = frequency;
    }

    @Override
    public String toString() {
        return "PathFrequency{" +
                "path=" + path +
                ", frequency=" + frequency +
                '}';
    }
}
