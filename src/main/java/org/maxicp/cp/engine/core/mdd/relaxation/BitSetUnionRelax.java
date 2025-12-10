package org.maxicp.cp.engine.core.mdd.relaxation;

import org.maxicp.state.datastructures.SmallBitSet;


public class BitSetUnionRelax implements RelaxFunction<SmallBitSet> {

    private static BitSetUnionRelax INSTANCE;

    @Override
    public SmallBitSet relax(SmallBitSet in, SmallBitSet other) {
        in.union(other);
        return in;
    }

    public static BitSetUnionRelax getInstance(){
        if(INSTANCE == null){
            INSTANCE = new BitSetUnionRelax();
        }
        return INSTANCE;
    }
}
