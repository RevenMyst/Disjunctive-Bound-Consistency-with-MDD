package org.maxicp.cp.engine.core.mdd.relaxation;

public class MaxRelax implements RelaxFunction<Integer> {

    private static MaxRelax INSTANCE;

    public static MaxRelax getInstance(){
        if(INSTANCE == null) {
            INSTANCE = new MaxRelax();
        }

        return INSTANCE;
    }

    @Override
    public Integer relax(Integer in, Integer other) {
        return Math.max(in, other);
    }
}
