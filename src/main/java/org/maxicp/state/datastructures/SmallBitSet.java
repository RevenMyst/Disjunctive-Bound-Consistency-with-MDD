package org.maxicp.state.datastructures;

import java.util.function.IntConsumer;

public abstract class SmallBitSet {

    abstract public void set(int i);

    abstract public void copy(SmallBitSet other);

    abstract public void unset(int i);

    abstract public void clear();

    abstract public long toLong();

    abstract public void union(SmallBitSet other);

    abstract public void intersect(SmallBitSet other);

    abstract public void invert();

    abstract public boolean contains(int i);

    protected long setOne(int id) {
        return 1L << id;
    }

    public int size() {
        return Long.bitCount(this.toLong());
    }

    abstract public boolean isEmpty();

    public boolean equals(SmallBitSet other) {
        return this.toLong() == other.toLong();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (int i = 0; i < Long.SIZE; i++) {
            if (contains(i)) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(i);
                first = false;
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public void forEach(IntConsumer action) {
        long bits = toLong();
        while (bits != 0) {
            int value = Long.numberOfTrailingZeros(bits);
            action.accept(value);
            bits &= bits - 1; // clear lowest set bit
        }
    }

}
