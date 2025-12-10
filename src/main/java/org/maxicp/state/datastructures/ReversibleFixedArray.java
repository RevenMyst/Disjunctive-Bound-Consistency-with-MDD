package org.maxicp.state.datastructures;

import org.maxicp.state.State;
import org.maxicp.state.StateInt;
import org.maxicp.state.StateManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ReversibleFixedArray {
    Map<Integer, State<Boolean>> array;
    StateInt size;

    // TODO change the name, this is a set not an array
    public ReversibleFixedArray(StateManager sm, int[] data, boolean initialValue) {
        array = new HashMap<>(); // TODO not sure is the best to use hashmap, but depend on the use (sparse or not data)
        for (int key : data) {
            array.put(key, sm.makeStateRef(initialValue));
        }
        size = sm.makeStateInt(initialValue ? data.length : 0);
    }

    public boolean contains(int key) {
        return array.containsKey(key) && array.get(key).value();
    }

    public ArrayList<Integer> getPossibleValues() {
        ArrayList<Integer> values = new ArrayList<>();
        for (Map.Entry<Integer, State<Boolean>> entry : array.entrySet()) {
                values.add(entry.getKey());
        }
        return values;
    }

    public void clear() {
        for (Map.Entry<Integer, State<Boolean>> entry : array.entrySet()) {
            entry.getValue().setValue(false);
        }
        size.setValue(0);
    }

    public void add(int key) {
        if(array.containsKey(key)) {
            if(!array.get(key).value()) {
                array.get(key).setValue(true);
                size.setValue(size.value() + 1);
            }

        } else {
            throw new IllegalArgumentException("value does not exist in the array: " + key);
        }
    }

    public void remove(int key) {
        if(array.containsKey(key)) {
            if(array.get(key).value()) {
                size.setValue(size.value() - 1);
                array.get(key).setValue(false);
            }
        } else {
            throw new IllegalArgumentException("value does not exist in the array: " + key);
        }
    }
}
