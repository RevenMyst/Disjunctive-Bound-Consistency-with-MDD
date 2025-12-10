package org.maxicp.cp.engine.core.mdd.properties;

import org.maxicp.cp.engine.core.mdd.MDDSpecs;
import org.maxicp.cp.engine.core.mdd.relaxation.RelaxFunction;

// T = the type of the property (e.g., MDDInt, MDDBool, etc.)
// U = the type of the value stored in the property (e.g., Integer, Boolean, etc.)
public abstract class MDDProperty<T extends MDDProperty, U> {
// TODO refactor to MDDProperty (generic class)
// TODO then MDDPropertyImmutable (cannot be changed)
// TODO and MDDPropertyMutable based on state values

    public enum MDDDirection {
        UP, DOWN
    }

    protected RelaxFunction<U> relaxFunction;
    public MDDDirection direction;
    protected U value;
    protected boolean isRelaxed = false;

    public MDDProperty(MDDSpecs s, U init, MDDDirection direction, RelaxFunction<U> relaxFunction) {
        this.relaxFunction = relaxFunction;
        this.direction = direction;
        this.value = init;
        s.addProperty(this);
    }

    public void update(U value, boolean forceUpdate){
        if(forceUpdate){
            this.value = value;
            isRelaxed = false;
        }
        else{
            if(!isRelaxed) isRelaxed = !this.value.equals(value);
            this.value = relaxFunction.relax(this.value, value);
        }
    }

    public boolean isRelaxed() {
        return isRelaxed;
    }

    public U getValue() {
        return value;
    }

    public RelaxFunction<U> getRelaxFunction() {
        return relaxFunction;
    }

    public abstract void copy(MDDProperty<T,U> prop);

    public abstract T getProperty();


    public boolean isUpProperty() {
        return direction == MDDDirection.UP;
    }

    public boolean isDownProperty() {
        return direction == MDDDirection.DOWN;
    }

    @Override
    public int hashCode() {
        return getProperty().hashCode();
    }

    public abstract long toHash();
}

