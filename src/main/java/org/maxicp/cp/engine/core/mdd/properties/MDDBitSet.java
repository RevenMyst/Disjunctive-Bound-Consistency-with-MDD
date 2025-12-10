package org.maxicp.cp.engine.core.mdd.properties;

import org.maxicp.cp.engine.core.mdd.MDDSpecs;
import org.maxicp.cp.engine.core.mdd.relaxation.RelaxFunction;
import org.maxicp.state.datastructures.ReversibleBitSet;
import org.maxicp.state.datastructures.SmallBitSet;

import java.util.Objects;


// Not updated to reversible
public class MDDBitSet extends MDDProperty<MDDBitSet, SmallBitSet> {


    public MDDBitSet(MDDSpecs s, ReversibleBitSet init, MDDDirection direction, RelaxFunction<SmallBitSet> relaxFunction) {
        super(s, init, direction, relaxFunction);
        this.value = new ReversibleBitSet(init);
    }

    @Override
    public void copy(MDDProperty<MDDBitSet, SmallBitSet> prop) {
        this.value.copy(prop.value);
    }

    @Override
    public void update(SmallBitSet value, boolean forceUpdate) {
        if(forceUpdate) {
            this.value.copy(value);

            isRelaxed = false;
        } else {
            if(!isRelaxed){
                isRelaxed = value.toLong() != this.value.toLong();
            }
            relaxFunction.relax(this.value, value);
        }
    }

    public SmallBitSet getValue() {
        return value;
    }

    @Override
    public MDDBitSet getProperty() {
        return this;
    }

    public void set(int i) {
        value.set(i);
    }

    public void unset(int i) {
        value.unset(i);
    }

    public void clear() {
        value.clear();
    }

    public void union(MDDBitSet other) {
        value.union(other.value);
    }

    public void intersect(MDDBitSet other) {
        value.intersect(other.value);
    }

    public void invert() {
        value.invert();
    }

    public boolean contains(int i) {
        return value.contains(i);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MDDBitSet mddBitSet = (MDDBitSet) o;
        return Objects.equals(value, mddBitSet.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public long toHash() {
        return value.toLong();
    }
}
