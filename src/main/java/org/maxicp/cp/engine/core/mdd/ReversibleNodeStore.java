package org.maxicp.cp.engine.core.mdd;

import org.maxicp.state.StateInt;
import org.maxicp.state.StateManager;
import org.maxicp.state.datastructures.ReversibleBitSet;

import java.util.ArrayList;
import java.util.List;

public class ReversibleNodeStore {
    public MDDNode[] nodes;
    public StateInt arraySize;
    StateManager sm;
    ReversibleBitSet freeIdx;

    public ReversibleNodeStore(List<MDDNode> setup, int maxSize, StateManager sm, MDDState defaultState, int layer){
        assert(setup.size() <= maxSize);
        nodes = new MDDNode[maxSize];
        arraySize = sm.makeStateInt(setup.size());
        this.sm = sm;
        for(int i = 0; i < maxSize; i++){
            if(i < setup.size())
                nodes[i] = setup.get(i);
            else
                nodes[i] = new MDDNode(layer, defaultState.clone(), sm);
        }
        this.freeIdx = new ReversibleBitSet(sm);

    }

    public int size(){
        return arraySize.value() - freeIdx.size();
    }

    public MDDNode getNode(){
        int idx;
        if(freeIdx.size() > 0) {
            idx = freeIdx.getFirstSetBit();
            freeIdx.unset(idx);
        }else{
            if(arraySize.value() >= nodes.length){
                throw new RuntimeException("Node store exhausted");
            }
            idx = arraySize.value();
            arraySize.increment();
        }
        return nodes[idx];
    }

    public boolean contains(MDDNode n){
        for (int i = 0; i < arraySize.value(); i++) {
            if(nodes[i].getId() == n.getId() && !freeIdx.contains(i)){
                return true;
            }
        }
        return false;
    }

    public void releaseNode(MDDNode n){
        for (int i = 0; i < arraySize.value(); i++) {
            if(nodes[i].getId() == n.getId()){
                freeIdx.set(i);

                return;
            }
        }
        throw new IllegalArgumentException("Node not found in store");
    }



    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arraySize.value(); i++) {
            if(!freeIdx.contains(i)) {
                sb.append(nodes[i].getId()).append(" ");
            }
        }
        return sb.toString();
    }

    public List<MDDNode> getActiveNodes(){
        List<MDDNode> active = new ArrayList<>();
        for (int i = 0; i < arraySize.value(); i++) {
            if(!freeIdx.contains(i)) {
                active.add(nodes[i]);
            }
        }
        return active;
    }

}
