package org.maxicp.cp.engine.core.mdd.properties;

import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.mdd.MDDSpecs;
import org.maxicp.cp.engine.core.mdd.relaxation.RelaxFunction;
import org.maxicp.state.StateInt;

import java.util.List;
import java.util.Objects;

public class MDDInt extends MDDProperty<MDDInt,Integer> {

    private StateInt value;
    public MDDInt(MDDSpecs s, Integer init, MDDDirection direction, RelaxFunction<Integer> relaxFunction) {
        super(s, init, direction, relaxFunction);

        List<CPIntVar> vars = s.getVariables();
        value = vars.getFirst().getSolver().getStateManager().makeStateInt(init);
    }

    @Override
    public void copy(MDDProperty<MDDInt,Integer> prop) {
        this.value.setValue(prop.getValue());
    }

    @Override
    public void update(Integer value, boolean forceUpdate) {
        if(forceUpdate){
            this.value.setValue(value);
            isRelaxed = false;
        }
        else{

            if(!isRelaxed) isRelaxed = !this.value.value().equals(value);
            this.value.setValue(relaxFunction.relax(this.value.value(), value));
        }
    }

    @Override
    public Integer getValue() {
        return value.value();
    }

    @Override
    public MDDInt getProperty() {
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MDDInt mddInt = (MDDInt) o;
        return Objects.equals(getValue(), mddInt.getValue());
    }

    @Override
    public int hashCode() {
        return value.value().hashCode();
    }

    @Override
    public long toHash() {
        return value.value();
    }

    @Override
    public String toString() {
        if(value.value() == Integer.MAX_VALUE){
            return "inf";
        }
        if(value.value() == Integer.MIN_VALUE){
            return "-inf";
        }
        return String.valueOf(value);
    }
}
