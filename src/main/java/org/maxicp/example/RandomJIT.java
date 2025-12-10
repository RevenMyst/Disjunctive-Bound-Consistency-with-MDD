package org.maxicp.example;

import org.maxicp.cp.CPFactory;
import org.maxicp.cp.engine.DefaultMDDs;
import org.maxicp.cp.engine.constraints.scheduling.NoOverlap;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPIntervalVar;
import org.maxicp.cp.engine.core.CPSolver;
import org.maxicp.search.*;

import java.util.*;

import static org.maxicp.search.Searches.*;
import static org.maxicp.search.Searches.minDomVariableSelector;

public class RandomJIT {

    public static void main(String[] args) {

        int seed = 2;
        long timeout;

        Map<String, String> argMap = new HashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) {
            argMap.put(args[i], args[i + 1]);
        }

        if (argMap.containsKey("-s")) seed = Integer.parseInt(argMap.get("-s"));
        if (argMap.containsKey("-t")) timeout = Long.parseLong(argMap.get("-t"));
        else {
            timeout = 60;
        }


        int n = 18;
        int d = 25;
        Random r = new Random(seed);

        int [] duration = new int[n];
        int [] startMin = new int[n];
        int [] endMax = new int[n];
        int [] endObj = new int[n];

        CPSolver cp = CPFactory.makeSolver();

        CPIntervalVar [] tasks = new CPIntervalVar[n];
        for (int i = 0; i < n; i++) {
            startMin[i] = (i == 0) ? 0 : endMax[i-1];
            duration[i] = r.nextInt(d) + 1;
            endMax[i] = startMin[i] + duration[i];
        }

        int w = 2;

        for (int i = 0; i < n; i++) {
            tasks[i] = CPFactory.makeIntervalVar(cp,false, duration[i]);
            tasks[i].setStartMin(startMin[Math.max(i-w,0)]);
            tasks[i].setEndMax(i > n-w ? n*d : endMax[Math.min(i+w,n-1)]);
            tasks[i].setPresent();
            int min = startMin[i] + duration[i];
            int max = endMax[i];
            endObj[i] = r.nextInt(max - min + 1) + min;

        }

        List<CPIntervalVar> tasksList = Arrays.asList(tasks);
        // shuffle the tasks
        Collections.shuffle(tasksList,r);
        tasks = tasksList.toArray(new CPIntervalVar[tasksList.size()]);



        NoOverlap noOverlap = new NoOverlap(tasks);
        cp.post(noOverlap);

        CPIntervalVar[] finalTasks = tasks;
        CPIntVar [] ends = CPFactory.makeIntVarArray(n, i -> CPFactory.end(finalTasks[i]));
        CPIntVar [] starts = CPFactory.makeIntVarArray(n, i -> CPFactory.start(finalTasks[i]));
        CPIntVar [] distancesToObj = CPFactory.makeIntVarArray(n, i -> CPFactory.abs(CPFactory.minus(ends[i], endObj[i])));



        CPIntVar justInTimePenalty = CPFactory.sum(distancesToObj);

        Objective objective = cp.minimize(justInTimePenalty);

        DFSearch dfs = CPFactory.makeDfs(cp, conflictOrderingSearch(minDomVariableSelector(starts),x -> x.min()));

        dfs.onSolution(() -> {
            //System.out.println("SumCompletion: " + justInTimePenalty);

        });
        DFSLinearizer linearizer = new DFSLinearizer();
        SearchStatistics stats = dfs.optimizeSubjectTo(objective,linearizer, (SearchStatistics s) -> { return s.timeInMillis() > timeout* 1000L;}, () -> {});


        objective.relax();

        SearchStatistics statsTWMDD = dfs.replaySubjectTo(linearizer, tasks, ()->{
            DefaultMDDs.RelaxedBCFiltering(cp,16, finalTasks);
        }, objective);


        objective.relax();
        SearchStatistics statsPredMDD = dfs.replaySubjectTo(linearizer, tasks, ()->{
            DefaultMDDs.PrecedenceExtraction(cp,16, finalTasks, noOverlap.precedenceVars());
        }, objective);
        objective.relax();







        System.out.println(seed+","+stats.numberOfNodes()+","+stats.timeInMillis()+","+statsTWMDD.numberOfNodes()+","+statsTWMDD.timeInMillis()+","+statsPredMDD.numberOfNodes()+","+statsPredMDD.timeInMillis());







    }
}