package org.maxicp.cp.engine.core.mdd.constraints;

import org.maxicp.cp.engine.core.CPBoolVar;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.mdd.MDDSpecs;
import org.maxicp.cp.engine.core.mdd.properties.MDDBitSet;
import org.maxicp.cp.engine.core.mdd.properties.MDDInt;
import org.maxicp.cp.engine.core.mdd.properties.MDDProperty;
import org.maxicp.cp.engine.core.mdd.relaxation.BitSetIntersectionRelax;
import org.maxicp.cp.engine.core.mdd.relaxation.BitSetUnionRelax;
import org.maxicp.cp.engine.core.mdd.relaxation.MaxRelax;
import org.maxicp.state.StateManager;
import org.maxicp.state.datastructures.DefaultSmallBitSet;
import org.maxicp.state.datastructures.ReversibleBitSet;

import java.util.List;

public class PrecedenceMDD extends MDDSpecs<PrecedenceMDD> {



    MDDBitSet allD;
    MDDBitSet someD;
    MDDBitSet allU;
    MDDBitSet someU;
    MDDInt len;
    CPBoolVar[][] precedences;

    public PrecedenceMDD(List<CPIntVar> positions,CPBoolVar[][] precedences) {
        super(2,3);
        this.variables = positions;
        this.precedences = precedences;
        StateManager sm = this.variables.getFirst().getSolver().getStateManager();
        allD = new MDDBitSet(this, new ReversibleBitSet(sm), MDDProperty.MDDDirection.DOWN, BitSetIntersectionRelax.getInstance());
        someD = new MDDBitSet(this, new ReversibleBitSet(sm), MDDProperty.MDDDirection.DOWN, BitSetUnionRelax.getInstance());
        allU = new MDDBitSet(this, new ReversibleBitSet(sm), MDDProperty.MDDDirection.UP, BitSetIntersectionRelax.getInstance());
        someU = new MDDBitSet(this, new ReversibleBitSet(sm), MDDProperty.MDDDirection.UP, BitSetUnionRelax.getInstance());
        len = new MDDInt(this, 0, MDDProperty.MDDDirection.DOWN, MaxRelax.getInstance());


    }
    @Override
    public boolean arcExist(MDDSpecs<PrecedenceMDD> source, MDDSpecs<PrecedenceMDD> target, CPIntVar var, int value) {

        for (int i = value + 1; i < this.variables.size(); i++) {
            CPBoolVar pred = precedences[value][i];
            if(pred.isFixed() && pred.isTrue()){
                // value << i
                if(source.getSpec().allD.contains(i)
                        || (source.getSpec().len.getValue() == source.getSpec().someD.getValue().size() && source.getSpec().someD.getValue().contains(value))
                        || !target.getSpec().someU.contains(i)) {
                    return false;
                }

            }
            if(pred.isFixed() && pred.isFalse()){
                // i << value
                if(target.getSpec().allU.contains(i) ||
                        (target.getSpec().len.getValue() - variables.size() == target.getSpec().someU.getValue().size() && target.getSpec().someU.getValue().contains(value))
                        || !source.getSpec().someD.contains(i)) {
                    return false;
                }


            }

        }

        return true;

    }


    @Override
    public void transitionDown(MDDSpecs<PrecedenceMDD> source, CPIntVar var, int value, boolean forceUpdate) {
        DefaultSmallBitSet newAlld = new DefaultSmallBitSet(source.getSpec().allD.getValue().toLong());
        newAlld.set(value);
        allD.update(newAlld, forceUpdate);
        DefaultSmallBitSet newSomed = new DefaultSmallBitSet(source.getSpec().someD.getValue().toLong());
        newSomed.set(value);
        someD.update(newSomed, forceUpdate);
        len.update(source.getSpec().len.getValue() + 1, forceUpdate);
    }

    @Override
    public void transitionUp(MDDSpecs<PrecedenceMDD> target, CPIntVar var, int value, boolean forceUpdate) {
        DefaultSmallBitSet newAllU = new DefaultSmallBitSet(target.getSpec().allU.getValue().toLong());
        newAllU.set(value);
        allU.update(newAllU, forceUpdate);
        DefaultSmallBitSet newSomeU = new DefaultSmallBitSet(target.getSpec().someU.getValue().toLong());
        newSomeU.set(value);
        someU.update(newSomeU, forceUpdate);
    }

    @Override
    public String toString() {
        return allD.toString() + " , " +
                someD.toString() + " / " +
                allU.toString() + " , " +
                someU.toString() + " / " +
                len.toString();
    }

    @Override
    public MDDSpecs<PrecedenceMDD> getInstance() {
        return new PrecedenceMDD(variables, precedences);
    }

    @Override
    public PrecedenceMDD getSpec() {
        return this;
    }
}
