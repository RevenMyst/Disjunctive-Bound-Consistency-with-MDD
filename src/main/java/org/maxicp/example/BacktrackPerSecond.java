package org.maxicp.example;

import org.maxicp.cp.CPFactory;
import org.maxicp.cp.engine.DefaultMDDs;
import org.maxicp.cp.engine.constraints.scheduling.NoOverlap;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPIntervalVar;
import org.maxicp.cp.engine.core.CPSolver;
import org.maxicp.search.*;

import java.io.*;
import java.util.*;

import static org.maxicp.search.Searches.*;
import static org.maxicp.search.Searches.minDomVariableSelector;

public class BacktrackPerSecond {

    public static class Instance{
        int n;
        int d;
        int w;
        int[] deadlines;
        final CPIntervalVar[] tasks;
        CPSolver cp;
        String filename;

        public Instance(String filename, CPSolver cp) {

            this.cp = cp;
            this.filename = filename;

            FileInputStream istream = null;
            try {

                istream = new FileInputStream(filename);
                BufferedReader in = new BufferedReader(new InputStreamReader(istream));
                // read instance from file
                StringTokenizer tokenizer = new StringTokenizer(in.readLine());
                n = Integer.parseInt(tokenizer.nextToken());
                d = Integer.parseInt(tokenizer.nextToken());
                w = Integer.parseInt(tokenizer.nextToken());
                tasks = CPFactory.makeIntervalVarArray(cp, n);
                this.deadlines = new int[n];
                for (int i = 0; i < n; i++) {
                    tokenizer = new StringTokenizer(in.readLine());
                    tasks[i].setStartMin(Integer.parseInt(tokenizer.nextToken()));
                    tasks[i].setLength(Integer.parseInt(tokenizer.nextToken()));
                    tasks[i].setEndMax(Integer.parseInt(tokenizer.nextToken()));
                    deadlines[i] = Integer.parseInt(tokenizer.nextToken());
                    tasks[i].setPresent();
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public void run(String filename, boolean TW, long timeout) {
        CPSolver cp = CPFactory.makeSolver();
        Instance instance = new Instance(filename, cp);

        NoOverlap noOverlap = new NoOverlap(instance.tasks);
        cp.post(noOverlap);
        if(TW){
            DefaultMDDs.RelaxedBCFiltering(cp,16, instance.tasks);
        }else{
            DefaultMDDs.PrecedenceExtraction(cp,16, instance.tasks, noOverlap.precedenceVars());
        }


        int n = instance.n;
        CPIntVar [] ends = CPFactory.makeIntVarArray(n, i -> CPFactory.end(instance.tasks[i]));
        CPIntVar [] starts = CPFactory.makeIntVarArray(n, i -> CPFactory.start(instance.tasks[i]));
        CPIntVar [] distancesToObj = CPFactory.makeIntVarArray(n, i -> CPFactory.abs(CPFactory.minus(ends[i], instance.deadlines[i])));
        CPIntVar justInTimePenalty = CPFactory.sum(distancesToObj);
        Objective objective = cp.minimize(justInTimePenalty);

        DFSearch dfs = CPFactory.makeDfs(cp, conflictOrderingSearch(minDomVariableSelector(starts),x -> x.min()));

        SearchStatistics stats = dfs.optimizeSubjectTo(objective, (SearchStatistics s) -> { return s.timeInMillis() > timeout* 1000L;}, () -> {});

        System.out.println(instance.filename+","+n+","+TW+","+stats.numberOfNodes()+","+stats.timeInMillis());




    }

    public static void main(String[] args) {

        String filename = "data/JIT/runBPS/JIT-100-25-2-0.txt";
        long timeout = 10;
        boolean TW = false;

        Map<String, String> argMap = new HashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) {
            argMap.put(args[i], args[i + 1]);
        }
        if (argMap.containsKey("-f")) filename = argMap.get("-f");
        if (argMap.containsKey("-t")) timeout = Long.parseLong(argMap.get("-t"));
        if (argMap.containsKey("-tw")) TW = Boolean.parseBoolean(argMap.get("-tw"));
        BacktrackPerSecond jitRun = new BacktrackPerSecond();
        //System.out.println(filename+" "+TW+" "+timeout);
        jitRun.run(filename, TW, timeout);

    }
}
