package org.maxicp.state.datastructures;

import org.junit.Before;
import org.junit.Test;
import org.maxicp.cp.CPFactory;
import org.maxicp.state.StateManager;

import static org.junit.Assert.assertEquals;

public class ReversibleStackTest {
    private StateManager sm;
    private ReversibleStack<Integer> stack;

    @Before
    public void setUp() {
        sm = CPFactory.makeSolver().getStateManager();
        stack = new ReversibleStack<Integer>(sm);
    }

    @Test
    public void testPushPop() {
        int n = 100;
        int i = 0;
        while (i < n) {
            stack.push(i);
            i++;
        }
        sm.saveState();
        while (i < 2*n) {
            stack.push(i);
            i++;
        }
        sm.restoreState();
        for(int j = 0; j < n;j++){
            int v = stack.pop();
            assertEquals(n-j-1,v);
        }
    }

    // TODO more tests
}
