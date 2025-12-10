package org.maxicp.cp.engine.core.mdd;

import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.mdd.properties.MDDInt;
import org.maxicp.state.datastructures.SmallBitSet;

import java.util.*;

import static org.xcsp.common.predicates.MatcherInterface.val;


public class MDDState {

    private List<MDDSpecs> specs;
    private Integer hashCode = null;

    public MDDState() {
        specs = new ArrayList<MDDSpecs>();
    }

    public void addSpecs(MDDSpecs s) {
        this.specs.add(s);
    }



    public List<MDDSpecs> getSpecs() {
        return specs;
    }


    /**
     * Transitions down the MDD state
     * if the specificator applies to the variable, calls the transitionDown method of the specificator,
     * otherwise copies the down properties from the source state to this state.
     * @param source the source MDDState
     * @param var the variable to transition down
     * @param val the value to transition down
     * @param forceUpdate if true, forces the update of the properties, otherwise uses the relaxation function
     */
    public void transitionDown(MDDState source, CPIntVar var, int val, boolean forceUpdate) {
        for (int i = 0; i < specs.size(); i++) {
            if (specs.get(i).getVariables().contains(var)){
                specs.get(i).transitionDown(source.specs.get(i), var, val, forceUpdate);
            }else{
                specs.get(i).updateDownProperties(source.specs.get(i), forceUpdate);
            }
        }
    }

    /**
     * Transitions down the MDD state
     * if the specificator applies to the variable, calls the transitionDown method of the specificator,
     * otherwise copies the down properties from the source state to this state.
     * @param source the source MDDState
     * @param var the variable to transition down
     * @param vals the values to transition down
     * @param forceUpdate if true, forces the update of the properties, otherwise uses the relaxation function
     */
    public void transitionDown(MDDState source, CPIntVar var, int[] vals, int nbVals, boolean forceUpdate) {
        for (int i = 0; i < specs.size(); i++) {
            if (specs.get(i).getVariables().contains(var)){
                specs.get(i).transitionDown(source.specs.get(i), var, vals, nbVals, forceUpdate);
            } else {
                specs.get(i).updateDownProperties(source.specs.get(i), forceUpdate);
            }
        }
    }
    public void transitionDown(MDDState source, CPIntVar var, int[] vals, boolean forceUpdate) {
        this.transitionDown(source, var, vals, vals.length, forceUpdate);
    }

    /**
     * Transitions up the MDD state
     * if the specificator applies to the variable, calls the transitionUp method of the specificator,
     * otherwise copies the up properties from the target state to this state.
     * @param target the target MDDState
     * @param var the variable to transition up
     * @param val the value to transition up
     * @param forceUpdate if true, forces the update of the properties, otherwise uses the relaxation function
     */
    public void transitionUp(MDDState target, CPIntVar var, int val, boolean forceUpdate) {
        for (int i = 0; i < specs.size(); i++) {
            if (specs.get(i).getVariables().contains(var)){
                specs.get(i).transitionUp(target.specs.get(i), var, val, forceUpdate);
            }else{
                specs.get(i).updateUpProperties(target.specs.get(i), forceUpdate);
            }
        }
    }

    /**
     * Transitions up the MDD state
     * if the specificator applies to the variable, calls the transitionUp method of the specificator,
     * otherwise copies the up properties from the target state to this state.
     * @param target the target MDDState
     * @param var the variable to transition up
     * @param vals the values to transition up
     * @param forceUpdate if true, forces the update of the properties, otherwise uses the relaxation function
     */
    public void transitionUp(MDDState target, CPIntVar var, int[] vals, int nbVals, boolean forceUpdate) {
        for (int i = 0; i < specs.size(); i++) {
            if (specs.get(i).getVariables().contains(var)){
                specs.get(i).transitionUp(target.specs.get(i), var, vals, nbVals, forceUpdate);
            }else{
                specs.get(i).updateUpProperties(target.specs.get(i), forceUpdate);
            }
        }
    }
    public void transitionUp(MDDState target, CPIntVar var, int[] vals, boolean forceUpdate) {
        this.transitionUp(target, var, vals, vals.length, forceUpdate);
    }

    /**
     * Checks if an arc exists between the source and target states for the given variable and value.
     * It checks each specificator in the MDDState and returns true if the arc exists in all specificators.
     *
     * @param source The source MDDState.
     * @param target The target MDDState.
     * @param var    The variable for which the arc is checked.
     * @param val    The value of the variable for which the arc is checked.
     * @return true if the arc exists, false otherwise.
     */
    public boolean arcExist(MDDState source, MDDState target, CPIntVar var, int val) {
        boolean res = true;
        for (int i = 0; i < specs.size(); i++) {
            if (specs.get(i).getVariables().contains(var))
                res &= specs.get(i).arcExist(source.specs.get(i), target.specs.get(i), var, val);
        }
        return res;
    }


    public Map<String, Integer> getExposedValues(){
        Map<String, Integer> result = new HashMap<>();
        for (MDDSpecs s : specs) {
            result.putAll(s.getExposedValues());
        }
        return result;
    }

    public Map<String, MDDInt> getExposedProperties(){
        Map<String, MDDInt> result = new HashMap<>();
        for (MDDSpecs s : specs) {
            result.putAll(s.getExposedProperty());
        }
        return result;
    }

    public Map<String, SmallBitSet> getExposedSmallBitSet(){
        Map<String, SmallBitSet> result = new HashMap<>();
        for (MDDSpecs s : specs) {
            result.putAll(s.getExposedSmallBitSet());
        }
        return result;
    }

    public void relax(MDDState other) {
        for (int i = 0; i < specs.size(); i++) {
            specs.get(i).relax(other.specs.get(i));
        }
    }


    public boolean isRelaxedDown() {
        for(MDDSpecs s : specs) {
            if (s.isRelaxedDown()) return true;
        }
        return false;
    }

    public boolean isRelaxedUp() {
        for(MDDSpecs s : specs) {
            if (s.isRelaxedUp()) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        String str = "";
        for (MDDSpecs s : specs) {
            str += "{" + s.toString() + "}";
        }
        return str;
    }

    public MDDState clone() {
        MDDState nState = new MDDState();
        for (MDDSpecs s : specs) {
            nState.addSpecs(s.clone());
        }
        return nState;
    }
    /**
     * Updates the down properties of this state with the properties of another state.
     * This was used to do downward passing in the MDD when already built.
     *
     * @param other The other MDDState whose properties will be used to update this state.
     */
    public void updateDownProperties(MDDState other) {
        for (int i = 0; i < specs.size(); i++) {
            specs.get(i).updateDownProperties(other.specs.get(i),true);
        }
    }

    /**
     * Updates the up properties of this state with the properties of another state.
     * This was used to do upward passing in the MDD when already built.
     *
     * @param other The other MDDState whose properties will be used to update this state.
     */
    public void updateUpProperties(MDDState other) {
        for (int i = 0; i < specs.size(); i++) {
            specs.get(i).updateUpProperties(other.specs.get(i),true);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MDDState mddState = (MDDState) o;
        return Objects.equals(specs, mddState.specs);
    }

    public long computeHash(){
        long h = 0x9E3779B97F4A7C15L; // golden ratio seed
        for (MDDSpecs s : specs) {
            h ^= s.computeHash();
            h = Long.rotateLeft(h, 27) * 0x165667B19E3779F9L;
        }
        return h;
    }

    public int computeHashCode() {
        return specs.hashCode();
    }

    @Override
    public int hashCode() {
        if (hashCode == null) {
            hashCode = computeHashCode();
        }
        return hashCode;
    }
}
