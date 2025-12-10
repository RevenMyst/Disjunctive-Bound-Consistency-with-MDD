package org.maxicp.cp.engine.core.mdd.relaxation;

public interface RelaxFunction<T>{

    T relax(T in, T other);
}
