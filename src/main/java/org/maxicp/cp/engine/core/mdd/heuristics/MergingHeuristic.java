package org.maxicp.cp.engine.core.mdd.heuristics;

import org.maxicp.cp.engine.core.mdd.MDD;
import org.maxicp.cp.engine.core.mdd.MDDNode;
import org.maxicp.state.datastructures.StateDoubleLinkedListMDD;

import java.util.*;
import java.util.function.BiFunction;

public abstract class MergingHeuristic {


    abstract public void mergeLayerFromTop(MDD mdd, int layer);

    public void mergeLayerFromBottom(MDD mdd, int layer) {
        mergeLayerFromTop(mdd, layer);
    }

    // Available heuristics


    public static RandomMerging randomMerging() {
        return new RandomMerging();
    }

    public static RandomMerging randomMerging(int seed) {
        return new RandomMerging(seed);
    }

    public static MakespanBucket MakespanBucket() {
        return new MakespanBucket();
    }

}

class RandomMerging extends MergingHeuristic{

    private final Random r;

    public RandomMerging() {
        this.r = new Random();
    }

    public RandomMerging(int seed) {
        this.r = new Random(seed);
    }

    @Override
    public void mergeLayerFromTop(MDD mdd, int layer) {

        HashSet<MDDNode> set = new HashSet<>();
        int maxWidth = mdd.widthHeuristic.width(layer) - 1;
        StateDoubleLinkedListMDD<MDDNode> diagramLayer = mdd.getLayers().get(layer);
        while ((diagramLayer.size() - set.size()) > maxWidth) {
            set.add(diagramLayer.get(r.nextInt(diagramLayer.size())));
        }
        if (!set.isEmpty()) {
            mdd.mergeNodes(set.stream().toList());
        }

    }

}

class MakespanBucket extends MergingHeuristic {

    // split the bucket in two, in bucketToSplit, the remaining elements are one half
    // split by median value
    private List<MDDNode> splitBucketsMedian(List<MDDNode> bucketToSplit, String criterium) {
        bucketToSplit.sort(Comparator.comparingDouble(
                n -> n.getState().getExposedValues().get(criterium)));
        int mid = bucketToSplit.size() / 2;

        List<MDDNode> half = new ArrayList<>(bucketToSplit.subList(mid, bucketToSplit.size()));
        bucketToSplit.subList(mid, bucketToSplit.size()).clear();

        return half;
    }

    private void mergeLayer(MDD mdd, int layer, String criterium) {
        List<MDDNode> nodes = mdd.getLayers().get(layer);
        int targetBuckets = mdd.widthHeuristic.width(layer);

        if (nodes.size() <= targetBuckets)
            return; // nothing to merge

        // --- 1. Compute min/max ---
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        Set<Double> distinctValues = new HashSet<>();
        for (MDDNode n : nodes) {
            double val = n.getState().getExposedValues().get(criterium);
            min = Math.min(min, val);
            max = Math.max(max, val);
            distinctValues.add(val);
        }

        // --- 2. Handle special cases ---
        if (min == max) {
            // All values are the same → one big bucket
            mdd.mergeNodes(nodes);
            return;
        }

        // --- 2.5 Adjust buckets for distinct values ---
        if (distinctValues.size() < targetBuckets) {
            targetBuckets = distinctValues.size();
        }


        // --- 3. Initial uniform bucketing ---
        List<List<MDDNode>> buckets = new ArrayList<>(targetBuckets);
        for (int i = 0; i < targetBuckets; i++)
            buckets.add(new ArrayList<>());

        double range = max - min;
        for (MDDNode n : nodes) {
            double val = n.getState().getExposedValues().get(criterium);
            int index = (int) ((val - min) / range * (targetBuckets - 1));
            buckets.get(index).add(n);
        }

        // --- 4. If some buckets are empty, re-balance ---
        boolean changed = true;
        int safety = 100; // avoid infinite loops
        while (changed && safety-- > 0) {
            changed = false;
            // find empty buckets
            for (int i = 0; i < buckets.size(); i++) {
                if (buckets.get(i).isEmpty()) {
                    // find the biggest bucket
                    int largest = -1;
                    int maxSize = 0;
                    for (int j = 0; j < buckets.size(); j++) {
                        if (buckets.get(j).size() > maxSize) {
                            maxSize = buckets.get(j).size();
                            largest = j;
                        }
                    }
                    if (largest == -1 || maxSize <= 1) continue;


                    // split the largest bucket by its median value
                    buckets.set(i, this.splitBucketsMedian(buckets.get(largest), criterium));

                    changed = true;
                }
            }
        }

        // --- 5. Merge nodes in each bucket ---
        for (List<MDDNode> bucket : buckets) {
            if (bucket.size() > 1) {
                mdd.mergeNodes(bucket);
            }
        }

    }

    @Override
    public void mergeLayerFromTop(MDD mdd, int layer) {
        this.mergeLayer(mdd, layer, "earliest");
    }



}
