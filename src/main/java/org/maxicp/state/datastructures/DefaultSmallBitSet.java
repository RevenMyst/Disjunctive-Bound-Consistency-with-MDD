package org.maxicp.state.datastructures;

public class DefaultSmallBitSet extends SmallBitSet {

    protected Long set;

    public DefaultSmallBitSet(Long anotherBitSet) {
        this.set = anotherBitSet;
    }

    public DefaultSmallBitSet() {
        this(0L);
    }

    public DefaultSmallBitSet(SmallBitSet other) {
        this(other.toLong());
    }

    public void set(int i) {
        set |= setOne(i);
    }

    @Override
    public void copy(SmallBitSet other) {
        this.set = other.toLong();
    }

    public void unset(int i) {
        set &= ~setOne(i);
    }

    public void clear() {
        set = 0L;
    }

    public long toLong() {
        return set;
    }

    public void union(SmallBitSet other) {
        set |= other.toLong();
    }

    public void intersect(SmallBitSet other) {
        set &= other.toLong();
    }

    public void invert() {
        set = ~set;
    }

    public boolean contains(int i) {
        return (set & setOne(i)) != 0;
    }

    @Override
    public boolean isEmpty() {
        return set == 0L;
    }
}
