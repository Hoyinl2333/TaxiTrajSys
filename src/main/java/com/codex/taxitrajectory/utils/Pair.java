package com.codex.taxitrajectory.utils; // 或者其他合适的包

import java.util.Objects;

public record Pair<L, R>(L left, R right) {
    // Record 自动提供构造函数、getter、equals、hashCode、toString
    // 确保 L 和 R 类型正确实现了 equals 和 hashCode (String 可以)
}

// 如果你不能使用 Java Record (低于 Java 16)，则使用普通类：
/*
package com.codex.taxitrajectory.utils;

import java.util.Objects;

public final class Pair<L, R> {
    public final L left;
    public final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() { return left; }
    public R getRight() { return right; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(left, pair.left) && Objects.equals(right, pair.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    @Override
    public String toString() {
        return "Pair{" + "left=" + left + ", right=" + right + '}';
    }
}
*/