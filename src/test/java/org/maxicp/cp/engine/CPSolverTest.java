/*
 * MaxiCP is under MIT License
 * Copyright (c)  2023 UCLouvain
 */

package org.maxicp.cp.engine;


import org.junit.jupiter.params.provider.Arguments;
import org.maxicp.cp.CPFactory;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPSolver;
import org.maxicp.cp.engine.core.MaxiCP;
import org.maxicp.cp.engine.core.mdd.MDD;
import org.maxicp.cp.engine.core.mdd.MDDSpecs;
import org.maxicp.cp.engine.core.mdd.heuristics.WidthHeuristic;
import org.maxicp.state.copy.Copier;
import org.maxicp.state.trail.Trailer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public abstract class CPSolverTest {

    public static Stream<CPSolver> getSolver() {
        return Stream.of(new MaxiCP(new Trailer()), new MaxiCP(new Copier()));
    }

    public static Stream<Arguments> solverSupplier() {
        return Stream.of(
                arguments(named(
                        new MaxiCP(new Trailer()).toString(),
                        (Supplier<CPSolver>) () -> new MaxiCP(new Trailer()))),
                arguments(named(
                        new MaxiCP(new Copier()).toString(),
                        (Supplier<CPSolver>) () -> new MaxiCP(new Copier()))));
    }

    /**
     * Gives a repeated set of suppliers of solvers
     * Each solver supplier is repeated nRepeat times. Hence, the length of the stream might be longer than nRepeat
     *
     * @param nRepeat number of occurrence for each {@link CPSolver}
     * @return stream of arraySize nRepeat * number of solvers. Each element contains one supplier of a {@link CPSolver}
     */
    public static Stream<Supplier<CPSolver>> getRepeatedSolverSuppliers(int nRepeat) {
        Stream<Supplier<CPSolver>> trailerStream = Stream.generate((Supplier<Supplier<CPSolver>>) () -> () -> new MaxiCP(new Trailer())).limit(nRepeat);
        Stream<Supplier<CPSolver>> copyStream = Stream.generate((Supplier<Supplier<CPSolver>>) () -> () -> new MaxiCP(new Copier())).limit(nRepeat);
        return Stream.concat(trailerStream, copyStream);
    }

    public static void checkMDD(CPSolver cp, MDDSpecs spec, int varToAdd, int nbSolution, Random rand, BiFunction<List<Integer>,List<Integer>, Boolean> checker){
        MDD mdd = new MDD();
        mdd.post(spec);
        ArrayList<CPIntVar> vars = new ArrayList<>();
        vars.addAll(spec.getVariables());
        for(int i = 0; i < varToAdd; i++){
            CPIntVar v = CPFactory.makeIntVar(cp, 1);
            vars.add(v);
        }
        Collections.shuffle(vars, rand);
        mdd.setVariables(vars);
        mdd.post();
        List<List<Integer>> mddSolutions = mdd.getAllSolutions();
        assertEquals(nbSolution, mddSolutions.size());
        List<Integer> indexesOfVariables = new ArrayList<>();
        for(int i = 0; i < vars.size(); i++){
            if(spec.getVariables().contains(vars.get(i))){
                indexesOfVariables.add(i);
            }
        }
        for (List<Integer> s : mddSolutions) {
            assertTrue(checker.apply(indexesOfVariables,s));
        }
    }

    public static void checkRelaxRemoveNoSolution(MDD mddExact, MDD mddRelaxed){
        mddExact.post();
        mddRelaxed.widthHeuristic = WidthHeuristic.FixedWidthHeuristic(mddExact.width()/2);
        mddRelaxed.post();
        List<List<Integer>> exactSolutions = mddExact.getAllSolutions();
        List<List<Integer>> relaxedSolutions = mddRelaxed.getAllSolutions();
        assertTrue(exactSolutions.size() <= relaxedSolutions.size());
        for(int i = 0; i < exactSolutions.size(); i++){
            assertTrue(relaxedSolutions.contains(exactSolutions.get(i)));
        }
    }
}
