package org.maxicp.cp.engine.core.mdd.constraints;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.maxicp.cp.CPFactory;
import org.maxicp.cp.engine.CPSolverTest;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPIntervalVar;
import org.maxicp.cp.engine.core.CPSolver;
import org.maxicp.cp.engine.core.mdd.MDD;
import org.maxicp.search.DFSearch;
import org.maxicp.search.SearchStatistics;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.maxicp.cp.CPFactory.makeDfs;
import static org.maxicp.cp.CPFactory.nonOverlap;
import static org.maxicp.search.Searches.firstFail;

public class DisjunctiveBitSetMDDTest extends CPSolverTest {

    @ParameterizedTest
    @MethodSource("getSolver")
    public void allDiffTest(CPSolver cp){
        CPIntervalVar[] tasks = CPFactory.makeIntervalVarArray(cp, 3);
        for(int i = 0; i < tasks.length; i++){
            tasks[i].setLength(1);
            tasks[i].setStartMax(2);
        }

        CPIntVar[] positions = CPFactory.makeIntVarArray(cp, 3,3);

        MDD mdd = new MDD();
        mdd.post(new DisjunctiveBitSetMDD(Arrays.asList(positions), Arrays.asList(tasks)));

        mdd.setVariables(Arrays.asList(positions));
        mdd.post();
        mdd.saveGraphToFile("test");

        assertEquals(6, mdd.getAllSolutions().size());

    }


    @ParameterizedTest
    @MethodSource("getSolver")
    public void oneSolution(CPSolver cp){
        CPIntervalVar[] tasks = CPFactory.makeIntervalVarArray(cp, 3);
        for(int i = 0; i < tasks.length; i++){
            tasks[i].setLength(1);
            tasks[i].setStartMin(i);
            tasks[i].setStartMax(i);
        }

        CPIntVar[] positions = CPFactory.makeIntVarArray(cp, 3,3);

        MDD mdd = new MDD();
        mdd.post(new DisjunctiveBitSetMDD(Arrays.asList(positions), Arrays.asList(tasks)));

        mdd.setVariables(Arrays.asList(positions));
        mdd.post();

        assertEquals(1, mdd.getAllSolutions().size());

    }


    @ParameterizedTest
    @MethodSource("getSolver")
    public void relaxation(CPSolver cp){
        int n = 5;
        CPIntervalVar[] tasks = CPFactory.makeIntervalVarArray(cp, n);
        for(int i = 0; i < tasks.length; i++){
            tasks[i].setLength(1);
            tasks[i].setStartMin(Math.max(0,i-2));
            tasks[i].setStartMax(Math.min(n-1,i+2));
        }

        CPIntVar[] positions = CPFactory.makeIntVarArray(cp, n, n);

        MDD mddExact = new MDD();
        mddExact.post(new DisjunctiveBitSetMDD(Arrays.asList(positions), Arrays.asList(tasks)));
        mddExact.setVariables(Arrays.asList(positions));
        MDD mddRelaxed = new MDD();
        mddRelaxed.post(new DisjunctiveBitSetMDD(Arrays.asList(positions), Arrays.asList(tasks)));
        mddRelaxed.setVariables(Arrays.asList(positions));

        checkRelaxRemoveNoSolution(mddExact, mddRelaxed);

    }
}
