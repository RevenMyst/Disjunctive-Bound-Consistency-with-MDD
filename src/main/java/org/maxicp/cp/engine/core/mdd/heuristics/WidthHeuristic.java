package org.maxicp.cp.engine.core.mdd.heuristics;

import org.maxicp.cp.engine.core.mdd.MDD;

import java.util.Arrays;
import java.util.stream.IntStream;

public abstract class WidthHeuristic {

    public abstract int width(int layer);

    public void effectiveSize(int layer, int size) {
    }

    public static FixedWidthHeuristic FixedWidthHeuristic(int size) {
        return new FixedWidthHeuristic(size);
    }


}

class FixedWidthHeuristic extends WidthHeuristic {
    int size;

    public FixedWidthHeuristic(int size) {
        this.size = size;
    }

    @Override
    public int width(int layer) {
        return size;
    }
}

