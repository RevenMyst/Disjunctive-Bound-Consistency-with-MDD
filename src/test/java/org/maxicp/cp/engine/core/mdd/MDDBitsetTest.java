package org.maxicp.cp.engine.core.mdd;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.maxicp.cp.CPFactory;
import org.maxicp.cp.engine.CPSolverTest;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPSolver;
import org.maxicp.cp.engine.core.mdd.properties.MDDBitSet;
import org.maxicp.cp.engine.core.mdd.properties.MDDProperty;
import org.maxicp.state.datastructures.DefaultSmallBitSet;
import org.maxicp.state.datastructures.ReversibleBitSet;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class MDDBitsetTest extends CPSolverTest {


    class DummySpec extends MDDSpecs<DummySpec> {
        public MDDBitSet unionSet;
        public MDDBitSet interSet;

        public DummySpec(CPIntVar var) {
            variables = List.of(new CPIntVar[]{var});
            unionSet = new MDDBitSet(this, new ReversibleBitSet(var.getSolver().getStateManager()), MDDProperty.MDDDirection.DOWN, org.maxicp.cp.engine.core.mdd.relaxation.BitSetUnionRelax.getInstance());
            interSet = new MDDBitSet(this, new ReversibleBitSet(var.getSolver().getStateManager()), MDDProperty.MDDDirection.UP, org.maxicp.cp.engine.core.mdd.relaxation.BitSetIntersectionRelax.getInstance());
        }

        @Override
        public boolean arcExist(MDDSpecs<DummySpec> source, MDDSpecs<DummySpec> target, CPIntVar var, int value) {
            return false;
        }

        @Override
        public boolean linkExist(MDDSpecs<DummySpec> source, MDDSpecs<DummySpec> target) {
            return false;
        }

        @Override
        public void transitionDown(MDDSpecs<DummySpec> source, CPIntVar var, int value, boolean forceUpdate) {

        }

        @Override
        public void transitionUp(MDDSpecs<DummySpec> target, CPIntVar var, int value, boolean forceUpdate) {

        }

        @Override
        public MDDSpecs<DummySpec> getInstance() {
            return new DummySpec(variables.getFirst());
        }

        @Override
        public DummySpec getSpec() {
            return this;
        }
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void testBitset(CPSolver cp) {
        DummySpec spec = new DummySpec(CPFactory.makeIntVar(cp, 5));
        DefaultSmallBitSet bitSet = new DefaultSmallBitSet();
        bitSet.set(1);
        spec.unionSet.update(bitSet, true);
        assertTrue(spec.unionSet.contains(1));
        cp.getStateManager().saveState();
        bitSet.unset(1);
        bitSet.set(2);
        spec.unionSet.update(bitSet, false);
        assertTrue(spec.unionSet.contains(1));
        assertTrue(spec.unionSet.contains(2));
        cp.getStateManager().restoreState();
        assertTrue(spec.unionSet.contains(1));
        assertFalse(spec.unionSet.contains(2));
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void testForceTrueOverwrites(CPSolver cp) {
        DummySpec spec = new DummySpec(CPFactory.makeIntVar(cp, 10));

        // First add bits 1 and 2
        DefaultSmallBitSet bs = new DefaultSmallBitSet();
        bs.set(1);
        bs.set(2);
        spec.unionSet.update(bs, true); // full overwrite
        assertTrue(spec.unionSet.contains(1));
        assertTrue(spec.unionSet.contains(2));

        // Now overwrite with {4} only
        DefaultSmallBitSet overwrite = new DefaultSmallBitSet();
        overwrite.set(4);
        spec.unionSet.update(overwrite, true); // should completely replace
        assertTrue(spec.unionSet.contains(4));
        assertFalse(spec.unionSet.contains(1));
        assertFalse(spec.unionSet.contains(2));
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void testRelaxationMergeForUnion(CPSolver cp) {
        DummySpec spec = new DummySpec(CPFactory.makeIntVar(cp, 10));

        // Start with {1}
        DefaultSmallBitSet bs = new DefaultSmallBitSet();
        bs.set(1);
        spec.unionSet.update(bs, true);

        // Relax (merge) with {2,3}
        DefaultSmallBitSet addMore = new DefaultSmallBitSet();
        addMore.set(2);
        addMore.set(3);
        spec.unionSet.update(addMore, false); // union relax should keep 1 and add 2,3
        assertTrue(spec.unionSet.contains(1));
        assertTrue(spec.unionSet.contains(2));
        assertTrue(spec.unionSet.contains(3));
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void testRelaxationForIntersection(CPSolver cp) {
        DummySpec spec = new DummySpec(CPFactory.makeIntVar(cp, 10));

        // Start with {1,2,3}
        DefaultSmallBitSet bs = new DefaultSmallBitSet();
        bs.set(1);
        bs.set(2);
        bs.set(3);
        spec.interSet.update(bs, true);

        // Relax with {2,3,4} should keep intersection {2,3}
        DefaultSmallBitSet next = new DefaultSmallBitSet();
        next.set(2);
        next.set(3);
        next.set(4);
        spec.interSet.update(next, false);
        assertFalse(spec.interSet.contains(1));
        assertTrue(spec.interSet.contains(2));
        assertTrue(spec.interSet.contains(3));
        assertFalse(spec.interSet.contains(4));
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void testSaveRestoreWithForceAndRelax(CPSolver cp) {
        DummySpec spec = new DummySpec(CPFactory.makeIntVar(cp, 10));
        DefaultSmallBitSet bs = new DefaultSmallBitSet();
        bs.set(1);
        spec.unionSet.update(bs, true);

        cp.getStateManager().saveState();

        // Relax to add 2
        DefaultSmallBitSet add2 = new DefaultSmallBitSet();
        add2.set(2);
        spec.unionSet.update(add2, false);
        assertTrue(spec.unionSet.contains(1));
        assertTrue(spec.unionSet.contains(2));

        // Overwrite completely with {3}
        DefaultSmallBitSet only3 = new DefaultSmallBitSet();
        only3.set(3);
        spec.unionSet.update(only3, true);
        assertFalse(spec.unionSet.contains(1));
        assertFalse(spec.unionSet.contains(2));
        assertTrue(spec.unionSet.contains(3));

        // Restore should bring us back to the state *after first overwrite* but before relax/overwrite
        cp.getStateManager().restoreState();
        assertTrue(spec.unionSet.contains(1));
        assertFalse(spec.unionSet.contains(2));
        assertFalse(spec.unionSet.contains(3));
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void testManyNestedSaveRestore(CPSolver cp) {
        DummySpec spec = new DummySpec(CPFactory.makeIntVar(cp, 200));

        DefaultSmallBitSet tmp = new DefaultSmallBitSet();

        // Add an initial value
        tmp.set(0);
        spec.unionSet.update(tmp, true);
        assertTrue(spec.unionSet.contains(0));

        // Perform a long chain of save/modify steps
        for (int i = 1; i <= 20; i++) {
            cp.getStateManager().saveState();

            // Add a new value by relaxation (merge)
            tmp.unset(i - 1);
            tmp.set(i);
            spec.unionSet.update(tmp, false);
            // Ensure all values from 0..i are present because we only merge
            for (int v = 0; v <= i; v++) {
                assertTrue(spec.unionSet.contains(v));
            }
        }

        // Now restore one level at a time and check that higher bits disappear
        for (int i = 20; i >= 1; i--) {
            cp.getStateManager().restoreState();
            // After restoring i-th save, only 0..i-1 should remain
            for (int v = 0; v < i; v++) {
                assertTrue(spec.unionSet.contains(v));
            }
            // The bit i that was added at that level must be gone
            assertFalse(spec.unionSet.contains(i));
        }

        // Final check: back to the original single bit
        assertTrue(spec.unionSet.contains(0));
        for (int v = 1; v <= 20; v++) {
            assertFalse(spec.unionSet.contains(v));
        }
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void testManyNestedSaveRestoreIntersection(CPSolver cp) {
        DummySpec spec = new DummySpec(CPFactory.makeIntVar(cp, 200));

        DefaultSmallBitSet tmp = new DefaultSmallBitSet();

        // Start with a wide set: bits 0..20 set
        for (int v = 0; v <= 20; v++) tmp.set(v);
        spec.interSet.update(tmp, true);     // full overwrite with all bits
        for (int v = 0; v <= 20; v++) {
            assertTrue(spec.interSet.contains(v));
        }

        // Repeatedly save and *narrow* the intersection
        for (int depth = 1; depth <= 20; depth++) {
            cp.getStateManager().saveState();

            // Build a new bitset that drops the lowest value each time
            tmp.unset(depth - 1);
            spec.interSet.update(tmp, false);  // intersection: should drop depth-1
            for (int v = depth; v <= 20; v++) {
                assertTrue(spec.interSet.contains(v));
            }
            for (int v = 0; v < depth; v++) {
                assertFalse(spec.interSet.contains(v));
            }

        }

        // Restore levels one by one and confirm bits reappear
        for (int depth = 20; depth >= 1; depth--) {
            cp.getStateManager().restoreState();

            // After restoring, bits 0..(depth-1) should be back
            for (int v = 0; v < depth-1; v++) {
                assertFalse(spec.interSet.contains(v));
            }
            for (int v = depth; v <= 20; v++) {
                assertTrue(spec.interSet.contains(v));
            }
        }

        // Final check: we should be back to the full original set
        for (int v = 0; v <= 20; v++) {
            assertTrue(spec.interSet.contains(v));
        }
    }
}


