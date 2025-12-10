package org.maxicp.cp.engine.core.mdd.constraints;

import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPIntervalVar;
import org.maxicp.cp.engine.core.mdd.*;
import org.maxicp.cp.engine.core.mdd.properties.MDDBitSet;
import org.maxicp.cp.engine.core.mdd.properties.MDDInt;
import org.maxicp.cp.engine.core.mdd.properties.MDDProperty;
import org.maxicp.cp.engine.core.mdd.relaxation.*;
import org.maxicp.state.StateManager;
import org.maxicp.state.datastructures.ReversibleBitSet;
import org.maxicp.state.datastructures.DefaultSmallBitSet;
import org.maxicp.state.datastructures.SmallBitSet;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DisjunctiveBitSetMDD extends MDDSpecs<DisjunctiveBitSetMDD> {

    private SmallBitSet temp;

    final List<CPIntervalVar> tasks;
    private final int nbTasks;

    public MDDBitSet allD;
    public MDDBitSet someD;
    public MDDBitSet allU;
    public MDDBitSet someU;
    public MDDInt earliest;
    public MDDInt latest;
    MDDInt layerID;
    public static long counter = 0;

    public DisjunctiveBitSetMDD(List<CPIntVar> positions, List<CPIntervalVar> tasks) {
        super(3, 4);
        this.tasks = tasks;
        this.nbTasks = tasks.size();
        this.variables = positions;
        StateManager sm = this.variables.getFirst().getSolver().getStateManager();
        allD = new MDDBitSet(this, new ReversibleBitSet(sm), MDDProperty.MDDDirection.DOWN, BitSetIntersectionRelax.getInstance());
        someD = new MDDBitSet(this, new ReversibleBitSet(sm), MDDProperty.MDDDirection.DOWN, BitSetUnionRelax.getInstance());
        allU = new MDDBitSet(this, new ReversibleBitSet(sm), MDDProperty.MDDDirection.UP, BitSetIntersectionRelax.getInstance());
        someU = new MDDBitSet(this, new ReversibleBitSet(sm), MDDProperty.MDDDirection.UP, BitSetUnionRelax.getInstance());
        earliest = new MDDInt(this, 0, MDDProperty.MDDDirection.DOWN, MinRelax.getInstance());
        latest = new MDDInt(this, Integer.MAX_VALUE, MDDProperty.MDDDirection.UP, MaxRelax.getInstance());
        layerID = new MDDInt(this, 0, MDDProperty.MDDDirection.DOWN, MaxRelax.getInstance());
        this.temp = new DefaultSmallBitSet(0L);

    }


    @Override
    public boolean arcExist(MDDSpecs<DisjunctiveBitSetMDD> source, MDDSpecs<DisjunctiveBitSetMDD> target, CPIntVar var, int value) {


        // ALLDIFF constraint part

        DisjunctiveBitSetMDD sourceSpec = source.getSpec();
        SmallBitSet sourceSpecAllD = sourceSpec.allD.getValue();
        // edge value is already in each of the path from root to source, cannot be used again
        if (sourceSpecAllD.contains(value))
            return false;

        DisjunctiveBitSetMDD targetSpec = target.getSpec();
        SmallBitSet targetSpecAllU = targetSpec.allU.getValue();
        // edge value is already in each of the path from target to sink, cannot be used again
        if (targetSpecAllU.contains(value))
            return false;

        SmallBitSet sourceSpecSomeD = sourceSpec.someD.getValue();
        SmallBitSet targetSpecSomeU = targetSpec.someU.getValue();
        temp.copy(sourceSpecSomeD);
        temp.union(targetSpecSomeU);
        temp.set(value);
        // requires at least the length of the path from root to sink in different values
        if (temp.size() < nbTasks)
            return false;

        int sourceSpecSomeDSize = sourceSpecSomeD.size();
        int sourceNbValue = sourceSpec.layerID.getValue();
        // requires at least the length of the path from root to a node in different values
        if (sourceNbValue >= sourceSpecSomeDSize && sourceSpecSomeD.contains(value))
            return false;

        int targetSpecSomeUSize = targetSpecSomeU.size();
        int targetNbValue = targetSpec.layerID.getValue() - variables.size();
        // requires at least the length of the path from a node to sink in different values
        if (targetNbValue >= targetSpecSomeUSize && targetSpecSomeU.contains(value))
            return false;


        if (targetNbValue == targetSpecSomeUSize)
            temp.copy(targetSpecSomeU);  // case where valid paths will have all the ones in someU
        else
            temp.copy(targetSpecAllU);
        long bits = temp.toLong();
        if (sourceNbValue == sourceSpecSomeDSize)
            temp.intersect(sourceSpecSomeD); // case where valid paths will have all the ones in someD
        else
            temp.intersect(sourceSpecAllD);
        // case where shared value between valid path from root to source and from target to sink, already know value is not there
        if (!temp.isEmpty())
            return false;


        // DISJUNCTIVE constraint part

        // task placeable
        CPIntervalVar taskToConsider = tasks.get(value);
        int earliestStart = Math.max(sourceSpec.earliest.getValue(), taskToConsider.startMin());
        // case where the earliest start is out of window for path from root to source
        if (earliestStart > taskToConsider.startMax())
            return false;
        // case where the earliest end is out of window for path from target to sink
        int earliestStartTarget = earliestStart + taskToConsider.lengthMin();
        if (earliestStartTarget > targetSpec.latest.getValue())
            return false;
        // TODO check if code below could not be better? (leading to error?)
//        int end = Math.min(sourceSpec.latest.getValue(), taskToConsider.endMax());
//        if (start + taskToConsider.lengthMin() > end)
//            return false;

        while (bits != 0L) {
            int v = Long.numberOfTrailingZeros(bits);
            // check if the task is compatible with next ones
            if (tasks.get(v).startMax() < earliestStartTarget) {
                return false;
            }
            bits &= bits - 1;
        }

        return true;

    }


    @Override
    public void transitionDown(MDDSpecs<DisjunctiveBitSetMDD> source, CPIntVar var, int value, boolean forceUpdate) {
        counter++;
        DisjunctiveBitSetMDD sourceSpec = source.getSpec();

        temp.copy(sourceSpec.allD.getValue());
        temp.set(value);
        allD.update(temp, forceUpdate);

        temp.copy(sourceSpec.someD.getValue());
        temp.set(value);
        someD.update(temp, forceUpdate);

        layerID.update(sourceSpec.layerID.getValue() + 1, forceUpdate);
        int earliestBasic = Math.max(sourceSpec.earliest.getValue(), tasks.get(value).startMin()) + tasks.get(value).lengthMin();
        earliest.update(earliestBasic, forceUpdate);
    }

    // TODO generalise to other and to other shortcut (*,!=v, >=v, <=v,...)
    /*@Override
    public void transitionDown(MDDSpecs<DisjunctiveBitSetMDD> source, CPIntVar var, int[] values, boolean forceUpdate) {
        DisjunctiveBitSetMDD sourceSpec = source.getSpec();

        if (values.length == 1) {
            temp.copy(sourceSpec.allD.getValue());
            temp.set(values[0]);
            allD.update(temp, forceUpdate);
        } else {
            temp.copy(sourceSpec.allD.getValue());
            allD.update(temp, forceUpdate);
        }


        temp.copy(sourceSpec.someD.getValue());
        for (int i = 0 ; i < values.length; i++) {
            temp.set(values[i]);
        }
        someD.update(temp, forceUpdate);

        layerID.update(sourceSpec.layerID.getValue() + 1, forceUpdate);
        int bound = Integer.MIN_VALUE;
        for (int i = 0 ; i < values.length; i++) {
            bound = Math.max(Math.max(bound, sourceSpec.earliest.getValue() + tasks.get(values[i]).lengthMin()), tasks.get(values[i]).endMin());
        }
        earliest.update(bound, forceUpdate);
    }*/

    @Override
    public void transitionUp(MDDSpecs<DisjunctiveBitSetMDD> target, CPIntVar var, int value, boolean forceUpdate) {
        DisjunctiveBitSetMDD targetSpec = target.getSpec();

        temp.copy(targetSpec.allU.getValue());
        temp.set(value);
        allU.update(temp, forceUpdate);

        temp.copy(targetSpec.someU.getValue());
        temp.set(value);
        someU.update(temp, forceUpdate);

        latest.update(Math.min(targetSpec.latest.getValue() - tasks.get(value).lengthMin(), tasks.get(value).startMax()), forceUpdate);
    }

    @Override
    public String toString() {
        return allD.toString() + " , " +
                someD.toString() + " / " +
                allU.toString() + " , " +
                someU.toString() + " / " +
                earliest.toString() + " , " +
                latest.toString() + " , " +
                layerID.toString();
    }

    @Override
    public MDDSpecs<DisjunctiveBitSetMDD> getInstance() {
        return new DisjunctiveBitSetMDD(variables, tasks);
    }

    @Override
    public DisjunctiveBitSetMDD getSpec() {
        return this;
    }


}
