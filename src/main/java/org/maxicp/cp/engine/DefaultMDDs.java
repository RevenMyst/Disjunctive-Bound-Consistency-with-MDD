package org.maxicp.cp.engine;

import org.maxicp.cp.CPFactory;
import org.maxicp.cp.engine.core.CPBoolVar;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPIntervalVar;
import org.maxicp.cp.engine.core.CPSolver;
import org.maxicp.cp.engine.core.mdd.MDD;
import org.maxicp.cp.engine.core.mdd.PropertyGetter;
import org.maxicp.cp.engine.core.mdd.constraints.DisjunctiveBitSetMDD;
import org.maxicp.cp.engine.core.mdd.heuristics.MergingHeuristic;
import org.maxicp.cp.engine.core.mdd.heuristics.WidthHeuristic;
import org.maxicp.cp.engine.core.mdd.propagation.*;
import org.maxicp.cp.engine.core.mdd.properties.MDDBitSet;

import java.util.Arrays;
import java.util.List;

public class DefaultMDDs {

    public static void RelaxedBCFiltering(CPSolver cp, int width, CPIntervalVar[] tasks) {
        MDD mdd = new MDD(cp.getStateManager());
        mdd.widthHeuristic= WidthHeuristic.FixedWidthHeuristic(width);
        mdd.mergingHeuristic = MergingHeuristic.MakespanBucket();
        CPIntVar[] positions = CPFactory.makeIntVarArray(cp, tasks.length, tasks.length);
        DisjunctiveBitSetMDD mddSpecs = new DisjunctiveBitSetMDD(List.of(positions), Arrays.asList(tasks));
        mddSpecs.addExposure("earliest", spec -> spec.earliest.getValue());
        mdd.post(mddSpecs);
        mdd.setVariables(List.of(positions));
        RevExtMDD revMDD = new RevExtMDD(mdd);


        MDDGeneralPropagator propTW = new MDDGeneralPropagator(revMDD, tasks);
        propTW.setPropertyGetters(new PropertyGetter<>(mddSpecs.earliest, mdd.getRoot().getState()),new PropertyGetter<>(mddSpecs.latest, mdd.getRoot().getState()));
        cp.post(propTW);
    }

    public static void PrecedenceExtraction(CPSolver cp, int width, CPIntervalVar[] tasks, CPBoolVar[] noOverlapPrecedenceVars) {
        MDD mdd = new MDD(cp.getStateManager());
        mdd.widthHeuristic= WidthHeuristic.FixedWidthHeuristic(width);
        mdd.mergingHeuristic = MergingHeuristic.MakespanBucket();
        CPIntVar[] positions = CPFactory.makeIntVarArray(cp, tasks.length, tasks.length);
        DisjunctiveBitSetMDD mddSpecs = new DisjunctiveBitSetMDD(List.of(positions), Arrays.asList(tasks));
        mddSpecs.addExposure("earliest", spec -> spec.earliest.getValue());
        mdd.post(mddSpecs);
        mdd.setVariables(List.of(positions));
        RevExtMDD revMDD = new RevExtMDD(mdd);


        CPBoolVar[][] pred  = new CPBoolVar[tasks.length][tasks.length];
        int k = 0;
        for (int i = 0; i < tasks.length; i++) {
            for (int j = i + 1; j < tasks.length; j++) {
                pred[i][j] = noOverlapPrecedenceVars[k++];
            }
        }
        MDDPrecedencesPropagator propPred = new MDDPrecedencesPropagator(revMDD,
                tasks,
                pred,
                new PropertyGetter<MDDBitSet>(mddSpecs.someU, mdd.getRoot().getState()),
                new PropertyGetter<MDDBitSet>(mddSpecs.someD, mdd.getRoot().getState()));
        cp.post(propPred);
    }

}
