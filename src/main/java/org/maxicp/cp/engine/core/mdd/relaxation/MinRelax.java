package org.maxicp.cp.engine.core.mdd.relaxation;

public class MinRelax implements RelaxFunction<Integer> {


    private static MinRelax INSTANCE;

    public static MinRelax getInstance(){
        if(INSTANCE == null) {
            INSTANCE = new MinRelax();
        }

        return INSTANCE;
    }
    @Override
    public Integer relax(Integer in, Integer other) {
        return Math.min(in, other);
    }
}
