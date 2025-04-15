package com.codex.taxitrajectory.model;

import lombok.Data;

import java.util.List;
import java.util.Objects;


@Data
public class Path {

    private List<GridCell> gridCells;

    public Path(List<GridCell> gridCells) {
        this.gridCells = gridCells;
    }

    public List<GridCell> getGridCells() {
        return gridCells;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Path)) return false;
        Path path = (Path) o;
        return Objects.equals(gridCells, path.gridCells);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gridCells);
    }

    @Override
    public String toString() {
        return "Path{" +
                "gridCells=" + gridCells +
                '}';
    }

}
