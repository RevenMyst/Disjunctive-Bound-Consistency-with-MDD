package org.maxicp.state.datastructures;

import org.maxicp.state.StateInt;
import org.maxicp.state.StateManager;

import java.util.ArrayList;


public class ReversibleStack<E> {
    private final StateInt size;
    public ArrayList<E> stack;

    // Build the stack with a given number of slot from the start
    public ReversibleStack(StateManager sm, int maxsize) {
        this.size = sm.makeStateInt(0);
        this.stack = new ArrayList<E>(maxsize);
    }

    // Build the stack
    public ReversibleStack(StateManager sm) {
        this(sm,10);
    }

    public void push(E elem) {
        int s = this.size.value();
        if (this.stack.size() > s) {
            this.stack.set(s, elem);
        } else {
            this.stack.add(elem);
        }
        this.size.increment();
    }

    public E pop() {
        if (this.isEmpty())
            throw new IllegalStateException("Stack is empty");
        this.size.decrement();
        return this.stack.get(this.size.value());
    }

    public boolean isEmpty() {
        return this.size.value() == 0;
    }

    public int size() {
        return this.size.value();
    }

    public E get(int index) {
        return this.stack.get(index);
    }
}

