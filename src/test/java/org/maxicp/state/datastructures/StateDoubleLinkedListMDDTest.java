package org.maxicp.state.datastructures;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.maxicp.cp.engine.CPSolverTest;
import org.maxicp.cp.engine.core.CPSolver;

import java.util.Arrays;
import java.util.Iterator;

import static org.junit.Assert.*;

public class StateDoubleLinkedListMDDTest extends CPSolverTest {

    class Dummy {
        int value;
        public StateDoubleLinkedListNode handle;
        public Dummy(int value, CPSolver cp) {
            this.value = value;
            this.handle = new StateDoubleLinkedListNode<>(cp.getStateManager(), this);
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Dummy dummy = (Dummy) o;
            return value == dummy.value;
        }

        @Override
        public String toString() {
            return value + "";
        }
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void SimpleAddRemove(CPSolver cp) {
        StateDoubleLinkedListMDD<Dummy> list = new StateDoubleLinkedListMDD<>(cp.getStateManager(), h -> h.handle);
        cp.getStateManager().saveState();

        Dummy d1 = new Dummy(1, cp);

        list.add(d1);
        assertEquals(1, list.size());
        assertTrue(list.contains(new Dummy(1, cp)));

        cp.getStateManager().saveState();

        list.add(new Dummy(2, cp));
        assertEquals(2, list.size());
        assertTrue(list.contains(new Dummy(2, cp)));

        list.remove(d1);
        assertEquals(1, list.size());
        assertFalse(list.contains(d1));

        cp.getStateManager().restoreState();
        assertEquals(1, list.size());
        assertTrue(list.contains(d1));
        assertFalse(list.contains(new Dummy(2, cp)));

        cp.getStateManager().restoreState();
        assertEquals(0, list.size());
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void AddRemoveMultipleRestore(CPSolver cp) {
        StateDoubleLinkedListMDD<Dummy> list = new StateDoubleLinkedListMDD<>(cp.getStateManager(), h -> h.handle);
        cp.getStateManager().saveState();

        Dummy d1 = new Dummy(1, cp);
        list.add(d1);
        assertEquals(1, list.size());
        assertTrue(list.contains(new Dummy(1, cp)));

        cp.getStateManager().saveState();

        Dummy d2 = new Dummy(2, cp);
        list.add(d2);
        assertEquals(2, list.size());
        assertTrue(list.contains(new Dummy(2, cp)));

        cp.getStateManager().saveState();

        Dummy d3 = new Dummy(3, cp);
        list.add(d3);
        assertEquals(3, list.size());
        assertTrue(list.contains(new Dummy(3, cp)));

        // remove in the latest state
        assertTrue(list.remove(d2));
        assertEquals(2, list.size());
        assertFalse(list.contains(d2));
        assertTrue(list.contains(d3));

        // restore to state saved just after adding d2 (d1,d2)
        cp.getStateManager().restoreState();
        assertEquals(2, list.size());
        assertTrue(list.contains(new Dummy(1, cp)));
        assertTrue(list.contains(new Dummy(2, cp)));
        assertFalse(list.contains(new Dummy(3, cp)));

        // restore to state with only d1
        cp.getStateManager().restoreState();
        assertEquals(1, list.size());
        assertTrue(list.contains(new Dummy(1, cp)));
        assertFalse(list.contains(new Dummy(2, cp)));

        // restore to initial empty state
        cp.getStateManager().restoreState();
        assertEquals(0, list.size());
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void IteratorAndGetWithRestore(CPSolver cp) {
        StateDoubleLinkedListMDD<Dummy> list = new StateDoubleLinkedListMDD<>(cp.getStateManager(), h -> h.handle);
        cp.getStateManager().saveState();

        Dummy d1 = new Dummy(1, cp);
        Dummy d2 = new Dummy(2, cp);
        list.add(d1);
        list.add(d2);

        // save after two elements
        cp.getStateManager().saveState();

        Dummy d3 = new Dummy(3, cp);
        list.add(d3);

        // iterator should return in insertion order
        Iterator<Dummy> it = list.iterator();
        assertTrue(it.hasNext());
        assertEquals(new Dummy(1, cp), it.next());
        assertTrue(it.hasNext());
        assertEquals(new Dummy(2, cp), it.next());
        assertTrue(it.hasNext());
        assertEquals(new Dummy(3, cp), it.next());
        assertFalse(it.hasNext());

        // restore to state with only d1,d2
        cp.getStateManager().restoreState();
        assertEquals(2, list.size());
        assertEquals(new Dummy(1, cp), list.get(0));
        assertEquals(new Dummy(2, cp), list.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void ToArrayAndGenericToArrayWithRestore(CPSolver cp) {
        StateDoubleLinkedListMDD<Dummy> list = new StateDoubleLinkedListMDD<>(cp.getStateManager(), h -> h.handle);
        cp.getStateManager().saveState();

        Dummy d1 = new Dummy(1, cp);
        list.add(d1);

        cp.getStateManager().saveState();

        Dummy d2 = new Dummy(2, cp);
        list.add(d2);

        Object[] arr = list.toArray();
        assertEquals(2, arr.length);
        assertEquals(new Dummy(1, cp), arr[0]);
        assertEquals(new Dummy(2, cp), arr[1]);

        Dummy[] typed = list.toArray(new Dummy[0]);
        assertEquals(2, typed.length);
        assertEquals(new Dummy(1, cp), typed[0]);
        assertEquals(new Dummy(2, cp), typed[1]);

        // restore -> only d1
        cp.getStateManager().restoreState();
        Object[] arrAfterRestore = list.toArray();
        assertEquals(1, arrAfterRestore.length);
        assertEquals(new Dummy(1, cp), arrAfterRestore[0]);
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void ClearAndRestore(CPSolver cp) {
        StateDoubleLinkedListMDD<Dummy> list = new StateDoubleLinkedListMDD<>(cp.getStateManager(), h -> h.handle);
        cp.getStateManager().saveState();

        Dummy d1 = new Dummy(1, cp);
        Dummy d2 = new Dummy(2, cp);
        list.add(d1);
        list.add(d2);

        cp.getStateManager().saveState();

        // clear in a later state
        list.clear();
        assertEquals(0, list.size());
        assertFalse(list.contains(new Dummy(1, cp)));

        // restore -> both elements should be back
        cp.getStateManager().restoreState();
        assertEquals(2, list.size());
        assertTrue(list.contains(new Dummy(1, cp)));
        assertTrue(list.contains(new Dummy(2, cp)));
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void ContainsAllAndRestore(CPSolver cp) {
        StateDoubleLinkedListMDD<Dummy> list = new StateDoubleLinkedListMDD<>(cp.getStateManager(), h -> h.handle);
        cp.getStateManager().saveState();

        Dummy d1 = new Dummy(1, cp);
        Dummy d2 = new Dummy(2, cp);
        Dummy d3 = new Dummy(3, cp);

        list.add(d1);
        list.add(d2);
        list.add(d3);

        cp.getStateManager().saveState();

        // remove d2 then verify containsAll fails
        list.remove(d2);
        assertFalse(list.containsAll(Arrays.asList(new Dummy(1, cp), new Dummy(2, cp))));

        // restore -> containsAll should succeed again
        cp.getStateManager().restoreState();
        assertTrue(list.containsAll(Arrays.asList(new Dummy(1, cp), new Dummy(2, cp))));
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void RemoveNonExistingDoesNothing(CPSolver cp) {
        StateDoubleLinkedListMDD<Dummy> list = new StateDoubleLinkedListMDD<>(cp.getStateManager(), h -> h.handle);
        cp.getStateManager().saveState();

        Dummy d1 = new Dummy(1, cp);
        list.add(d1);

        // attempt to remove an element that was never added
        boolean removed = list.remove(new Dummy(99, cp));
        assertFalse(removed);
        assertEquals(1, list.size());
        assertTrue(list.contains(new Dummy(1, cp)));

        // saving/restoring should keep the same
        cp.getStateManager().saveState();
        cp.getStateManager().restoreState();
        assertEquals(1, list.size());
        assertTrue(list.contains(new Dummy(1, cp)));
    }

}
