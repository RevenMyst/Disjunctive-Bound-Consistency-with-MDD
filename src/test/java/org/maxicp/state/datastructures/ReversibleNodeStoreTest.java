package org.maxicp.state.datastructures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.maxicp.cp.CPFactory;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPIntVarImpl;
import org.maxicp.cp.engine.core.CPSolver;
import org.maxicp.cp.engine.core.mdd.*;
import org.maxicp.cp.engine.core.mdd.properties.MDDInt;
import org.maxicp.cp.engine.core.mdd.properties.MDDProperty;
import org.maxicp.cp.engine.core.mdd.relaxation.MinRelax;
import org.maxicp.state.trail.Trailer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReversibleNodeStoreTest {

    private Trailer trailer;
    private MDDState defaultState;
    private ReversibleNodeStore store;

    @BeforeEach
    void setUp() {
        trailer = new Trailer(); // Assuming default constructor
        defaultState = new MDDState(); // Assuming default constructor
        List<MDDNode> initialNodes = new ArrayList<>();
        store = new ReversibleNodeStore(initialNodes, 5, trailer, defaultState, 0);
    }

    @Test
    void testConstructorInitialSizeMatchesSetup() {
        trailer = new Trailer();
        defaultState = new MDDState();

        List<MDDNode> initialNodes = Arrays.asList(
                new MDDNode(0, defaultState.clone(), trailer),
                new MDDNode(0, defaultState.clone(), trailer),
                new MDDNode(0, defaultState.clone(), trailer)
        );

        ReversibleNodeStore storeWithSetup =
                new ReversibleNodeStore(initialNodes, 5, trailer, defaultState, 0);

        assertEquals(3, storeWithSetup.size(),
                "Size should be initialized to the number of nodes in the setup list");
    }

    @Test
    void testGetNodeIncreasesSize() {
        trailer.saveState();
        int initialSize = store.size();
        MDDNode n = store.getNode();
        assertNotNull(n);
        assertEquals(initialSize + 1, store.size());
        trailer.restoreState();
        // State should be restored
        assertEquals(initialSize, store.size());
    }

    @Test
    void testReleaseNodeDecreasesSize() {
        MDDNode n1 = store.getNode();
        MDDNode n2 = store.getNode();
        int sizeAfterTwo = store.size();
        assertEquals(2, sizeAfterTwo);

        trailer.saveState();
        store.releaseNode(n1);
        assertEquals(1, store.size());
        trailer.restoreState();
        // Back to original
        assertEquals(2, store.size());
    }

    @Test
    void testGetNodeBeyondMaxThrows() {
        // Fill to max
        for (int i = 0; i < 5; i++) {
            store.getNode();
        }
        assertThrows(RuntimeException.class, () -> store.getNode());
    }

    @Test
    void testSaveRestoreMultipleNodes() {
        MDDNode n1 = store.getNode();
        MDDNode n2 = store.getNode();
        int sizeBefore = store.size();

        trailer.saveState();
        store.releaseNode(n2);
        store.releaseNode(n1);
        assertEquals(sizeBefore - 2, store.size());

        trailer.restoreState();
        // Both should be restored
        assertEquals(sizeBefore, store.size());
    }

    @Test
    void testComplexMultipleOperationsWithNestedStates() {
        MDDNode n1 = store.getNode();
        MDDNode n2 = store.getNode();
        MDDNode n3 = store.getNode();
        assertEquals(3, store.size());

        // First save
        trailer.saveState();

        store.releaseNode(n2);
        assertEquals(2, store.size());

        // Second save
        trailer.saveState();

        store.releaseNode(n1);
        assertEquals(1, store.size());

        MDDNode n4 = store.getNode();
        assertEquals(n1,n4);
        assertEquals(2, store.size());

        // Restore inner save → undo release(n1) and getNode(n4)
        trailer.restoreState();
        assertEquals(2, store.size());
        assertTrue(containsNode(store, n1)); // n1 back

        // Restore outer save → undo release(n2)
        trailer.restoreState();
        assertEquals(3, store.size());
        assertTrue(containsNode(store, n1));
        assertTrue(containsNode(store, n2));
        assertTrue(containsNode(store, n3));
    }

    @Test
    void testNodeEdition() {
        CPSolver cp = CPFactory.makeSolver();
        trailer = (Trailer) cp.getStateManager();
        defaultState = new MDDState();
        defaultState.addSpecs(new MDDSpecs() {
            MDDInt prop;
            {
                variables = new ArrayList<>();
                variables.add(CPFactory.makeIntVar(cp, 5));
                prop = new MDDInt(this,0, MDDProperty.MDDDirection.DOWN, MinRelax.getInstance());
            }
            @Override
            public boolean arcExist(MDDSpecs source, MDDSpecs target, CPIntVar var, int value) {
                return false;
            }

            @Override
            public boolean linkExist(MDDSpecs source, MDDSpecs target) {
                return false;
            }

            @Override
            public void transitionDown(MDDSpecs source, CPIntVar var, int value, boolean forceUpdate) {

            }

            @Override
            public void transitionUp(MDDSpecs target, CPIntVar var, int value, boolean forceUpdate) {

            }

            @Override
            public MDDSpecs getInstance() {
                return this;
            }

            @Override
            public Object getSpec() {
                return this;
            }
        });
        MDDNode baseNode = new MDDNode(0, defaultState.clone(), trailer);
        List<MDDNode> initialNodes = Arrays.asList(
               baseNode
        );

        store = new ReversibleNodeStore(initialNodes, 5, trailer, defaultState, 0);
        trailer.saveState();
        store.releaseNode(baseNode);
        MDDNode newNode = store.getNode();
        MDDProperty<MDDInt, Integer> prop = (MDDProperty) newNode.getState().getSpecs().getFirst().getDownProperties()[0];
        prop.update(5,true);
        assertEquals(5, prop.getValue());
        trailer.restoreState();

        assertEquals(0, prop.getValue());
    }

    private boolean containsNode(ReversibleNodeStore store, MDDNode node) {
        for (int i = 0; i < store.size(); i++) {
            if (store.nodes[i].getId() == node.getId()) {
                return true;
            }
        }
        return false;
    }


}
