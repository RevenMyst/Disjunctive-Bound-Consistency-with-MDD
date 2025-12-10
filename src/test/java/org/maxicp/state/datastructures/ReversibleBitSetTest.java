package org.maxicp.state.datastructures;

import org.junit.Before;
import org.junit.Test;
import org.maxicp.cp.CPFactory;
import org.maxicp.state.StateManager;

import static org.junit.Assert.*;

public class ReversibleBitSetTest {

    private StateManager sm;
    private ReversibleBitSet bitset;

    @Before
    public void setUp() {
        sm = CPFactory.makeSolver().getStateManager();
        bitset = new ReversibleBitSet(sm);
    }

    @Test
    public void testSetAndContains() {
        assertFalse(bitset.contains(3));
        assertEquals(0, bitset.size());

        bitset.set(3);
        assertTrue(bitset.contains(3));
        assertEquals(1, bitset.size());
        assertEquals(1L << 3, bitset.toLong());
    }

    @Test
    public void testUnset() {
        bitset.set(5);
        assertTrue(bitset.contains(5));
        assertEquals(1, bitset.size());

        bitset.unset(5);
        assertFalse(bitset.contains(5));
        assertEquals(0, bitset.size());
        assertEquals(0L, bitset.toLong());
    }

    @Test
    public void testClear() {
        bitset.set(1);
        bitset.set(10);
        assertTrue(bitset.contains(1));
        assertTrue(bitset.contains(10));
        assertEquals(2, bitset.size());

        bitset.clear();
        assertEquals(0, bitset.size());
        assertEquals(0L, bitset.toLong());
    }

    @Test
    public void testUnion() {
        ReversibleBitSet other = new ReversibleBitSet(sm);
        bitset.set(1);
        other.set(2);

        bitset.union(other);
        assertTrue(bitset.contains(1));
        assertTrue(bitset.contains(2));
        assertEquals(2, bitset.size());
    }

    @Test
    public void testIntersect() {
        ReversibleBitSet other = new ReversibleBitSet(sm);
        bitset.set(1);
        bitset.set(2);
        other.set(2);
        other.set(3);

        bitset.intersect(other);
        assertFalse(bitset.contains(1));
        assertTrue(bitset.contains(2));
        assertFalse(bitset.contains(3));
        assertEquals(1, bitset.size());
    }

    @Test
    public void testInvert() {
        bitset.set(0);
        bitset.set(2);
        int beforeSize = bitset.size();

        long before = bitset.toLong();
        bitset.invert();
        long after = bitset.toLong();

        assertEquals(~before, after);
        // arraySize after invert = 64 - previous number of bits set
        assertEquals(Long.SIZE - beforeSize, bitset.size());
        assertFalse(bitset.contains(0));
        assertFalse(bitset.contains(2));
    }

    @Test
    public void testCopyConstructor() {
        bitset.set(4);
        ReversibleBitSet copy = new ReversibleBitSet(bitset);

        assertTrue(copy.contains(4));
        assertEquals(bitset.toLong(), copy.toLong());
        assertEquals(bitset.size(), copy.size());
    }

    @Test
    public void testReversibilitySetAndRestore() {
        bitset.set(2);
        assertEquals(1, bitset.size());

        sm.saveState();   // save with bit 2 set
        bitset.set(5);
        assertEquals(2, bitset.size());

        sm.restoreState(); // back to saved state
        assertEquals(1, bitset.size());
        assertTrue(bitset.contains(2));
        assertFalse(bitset.contains(5));
    }

    @Test
    public void testReversibilityClearAndRestore() {
        bitset.set(1);
        bitset.set(3);
        assertEquals(2, bitset.size());

        sm.saveState();   // save with {1,3}
        bitset.clear();
        assertEquals(0, bitset.size());
        assertEquals("{}", bitset.toString());

        sm.restoreState();
        assertEquals(2, bitset.size());
        assertTrue(bitset.contains(1));
        assertTrue(bitset.contains(3));
    }
    @Test
    public void testReversibilityUnionAndIntersectRestore() {
        ReversibleBitSet other = new ReversibleBitSet(sm);
        bitset.set(1);
        other.set(2);

        sm.saveState();   // save with {1}
        bitset.union(other);  // now {1,2}
        assertTrue(bitset.contains(2));

        sm.restoreState(); // back to {1}
        assertTrue(bitset.contains(1));
        assertFalse(bitset.contains(2));
    }

    @Test
    public void testReversibilityInvertRestore() {
        bitset.set(0);
        sm.saveState();

        bitset.invert(); // now everything but 0
        assertFalse(bitset.contains(0));

        sm.restoreState();
        assertTrue(bitset.contains(0));
    }

    @Test
    public void testNestedSaveRestore() {
        bitset.set(1); // {1}
        sm.saveState();

        bitset.set(2); // {1,2}
        sm.saveState();

        bitset.set(3); // {1,2,3}
        assertTrue(bitset.contains(3));

        sm.restoreState(); // back to {1,2}
        assertTrue(bitset.contains(1));
        assertTrue(bitset.contains(2));
        assertFalse(bitset.contains(3));

        sm.restoreState(); // back to {1}
        assertTrue(bitset.contains(1));
        assertFalse(bitset.contains(2));
        assertFalse(bitset.contains(3));
    }

    @Test
    public void testMultipleNestedOperations() {
        bitset.set(0); // {0}
        sm.saveState();

        bitset.set(5); // {0,5}
        sm.saveState();

        bitset.clear(); // {}
        sm.saveState();

        bitset.set(10); // {10}

        // restore to {}
        sm.restoreState();
        assertEquals("{}", bitset.toString());

        // restore to {0,5}
        sm.restoreState();
        assertTrue(bitset.contains(0));
        assertTrue(bitset.contains(5));
        assertFalse(bitset.contains(10));

        // restore to {0}
        sm.restoreState();
        assertTrue(bitset.contains(0));
        assertFalse(bitset.contains(5));
    }


}
