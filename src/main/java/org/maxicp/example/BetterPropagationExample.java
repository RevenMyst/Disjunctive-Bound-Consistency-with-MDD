package org.maxicp.example;

import org.maxicp.cp.CPFactory;
import org.maxicp.cp.engine.DefaultMDDs;
import org.maxicp.cp.engine.constraints.scheduling.NoOverlap;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPIntervalVar;
import org.maxicp.cp.engine.core.CPSolver;

import java.util.Arrays;

import static org.maxicp.cp.CPFactory.nonOverlap;

public class BetterPropagationExample {

    public static void main(String[] args) {
        CPSolver cp = CPFactory.makeSolver();
        CPIntervalVar[] tasks = CPFactory.makeIntervalVarArray(cp, 4);
        CPIntVar[] positions = CPFactory.makeIntVarArray(cp, 4,4);

        for(int i = 0; i<4;i++){
            tasks[i].setPresent();
        }

        tasks[0].setStart(4);
        tasks[0].setLength(2);

        tasks[1].setStartMin(0);
        tasks[1].setStartMax(7);
        tasks[1].setLength(3);

        tasks[2].setStartMin(0);
        tasks[2].setStartMax(6);
        tasks[2].setLength(2);

        tasks[3].setStartMin(7);
        tasks[3].setStartMax(20);
        tasks[3].setLength(5);


        System.out.println(Arrays.stream(tasks).map(t -> t.startMin()+" - "+t.endMax()).toList());
        NoOverlap noOverlap = nonOverlap(tasks);
        cp.post(noOverlap);
        DefaultMDDs.RelaxedBCFiltering(cp, 16,tasks);
        //DefaultMDDs.PrecedenceExtraction(cp, 16,tasks,noOverlap.precedenceVars());
        System.out.println(Arrays.stream(tasks).map(t -> t.startMin()+" - "+t.endMax()).toList());


    }
}
