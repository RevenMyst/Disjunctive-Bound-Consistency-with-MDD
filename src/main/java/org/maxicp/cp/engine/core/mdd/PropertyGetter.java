package org.maxicp.cp.engine.core.mdd;

import org.maxicp.cp.engine.core.mdd.properties.MDDProperty;

public class PropertyGetter<T extends MDDProperty> {

    private int specIndex;
    private boolean isDown;
    private int propertyIndex;

    public PropertyGetter(T property, MDDState state) {
        isDown = property.direction == MDDProperty.MDDDirection.DOWN;
        for (int i = 0; i < state.getSpecs().size(); i++) {
            if(isDown){
                for (int j = 0; j < state.getSpecs().get(i).getDownProperties().length; j++) {
                    if(state.getSpecs().get(i).getDownProperties()[j] == property) {
                        specIndex = i;
                        propertyIndex = j;
                        return;
                    }
                }
            }else {
                for (int j = 0; j < state.getSpecs().get(i).getUpProperties().length; j++) {
                    if (state.getSpecs().get(i).getUpProperties()[j] == property) {
                        specIndex = i;
                        propertyIndex = j;
                        return;
                    }
                }
            }

        }
    }

    public T getProperty(MDDState state) {
        if(isDown)
            return (T) state.getSpecs().get(specIndex).getDownProperties()[propertyIndex];
        else
            return (T) state.getSpecs().get(specIndex).getUpProperties()[propertyIndex];
    }

    @Override
    public String toString() {
        return "PropertyGetter{" +
                "specIndex=" + specIndex +
                ", isDown=" + isDown +
                ", propertyIndex=" + propertyIndex +
                '}';
    }
}
