package org.maxicp.state.datastructures;

import org.maxicp.state.StateLong;
import org.maxicp.state.StateManager;

public class ReversibleBitSet extends SmallBitSet {

    protected StateManager sm;
    protected StateLong set;

    public ReversibleBitSet(StateManager sm, long initValue) {
        this.sm = sm;
        this.set = sm.makeStateLong(initValue);
    }

    public ReversibleBitSet(StateManager sm) {
        this(sm,0L);
    }

    public ReversibleBitSet(ReversibleBitSet anotherBitSet) {
        this(anotherBitSet.getStateManager(), anotherBitSet.toLong());
    }

    public void copy(SmallBitSet other) {
        this.set.setValue(other.toLong());
    }

    public StateManager getStateManager() {
        return this.sm;
    }

    public void set(int i) {
        set.setValue(set.value() | setOne(i));
    }

    public void unset(int i) {
        set.setValue(set.value() & ~setOne(i));
    }

    public void clear() {
        set.setValue(0L);
    }

    public long toLong() {
        return set.value();
    }

    public void union(SmallBitSet other) {
        set.setValue(set.value() | other.toLong());
    }

    public void intersect(SmallBitSet other) {
        set.setValue(set.value() & other.toLong());
    }

    public int getFirstSetBit() {
        if (set.value() == 0) return -1;
        return Long.numberOfTrailingZeros(set.value());
    }

    public void invert() {
        set.setValue(~set.value());
    }

    public boolean contains(int i) {
        return (set.value() & setOne(i)) != 0;
    }

    @Override
    public boolean isEmpty() {
        return this.set.value() == 0L;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ReversibleBitSet that = (ReversibleBitSet) obj;
        return this.set.value() == that.set.value();
    }
}
