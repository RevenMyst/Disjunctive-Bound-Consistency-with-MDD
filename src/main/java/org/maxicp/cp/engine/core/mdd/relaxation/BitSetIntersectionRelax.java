package org.maxicp.cp.engine.core.mdd.relaxation;

import org.maxicp.state.datastructures.SmallBitSet;


public class BitSetIntersectionRelax implements RelaxFunction<SmallBitSet> {

    private static BitSetIntersectionRelax INSTANCE;

    @Override
    public SmallBitSet relax(SmallBitSet in, SmallBitSet other) {
        in.intersect(other);
        return in;
    }

    public static BitSetIntersectionRelax getInstance(){
        if(INSTANCE == null){
            INSTANCE = new BitSetIntersectionRelax();
        }
        return INSTANCE;
    }
}
