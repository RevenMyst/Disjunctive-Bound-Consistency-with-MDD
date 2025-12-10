package org.maxicp.cp.engine.core.mdd;

import org.maxicp.state.State;
import org.maxicp.state.StateManager;
import org.maxicp.state.datastructures.StateDoubleLinkedListNode;
import org.maxicp.state.datastructures.StateDoubleLinkedListMDD;

import java.util.List;
import java.util.Objects;


public class MDDNode{

    public static int NODE_COUNTER = 0;

    private final int layer;
    private StateDoubleLinkedListMDD<MDDEdge> outEdges;
    private StateDoubleLinkedListMDD<MDDEdge> inEdges;
    private MDDState state;
    public boolean updated = true;
    private int id;
    public State<Boolean> isActive;
    public StateManager sm;
    public StateDoubleLinkedListNode<MDDNode> lh;

    public MDDNode(int layer, MDDState state, StateManager sm) {
        this.lh = new StateDoubleLinkedListNode<>(sm, this);
        this.id = NODE_COUNTER++;
        this.layer = layer;
        this.sm = sm;
        this.outEdges = new StateDoubleLinkedListMDD<>(sm, e -> e.top);
        this.inEdges = new StateDoubleLinkedListMDD<>(sm, e -> e.bottom);
        this.state = state;
        this.isActive = sm.makeStateRef(true);
    }



    public double relaxArityIn(){
        double relax = 0;
        for(MDDEdge e : inEdges){
            if(e.getSource().isRelaxedDown()){
                relax += 1.0;
            }
        }
        return relax/inEdges.size();
    }

    public double relaxArityInCumulative(){
        double relax = 0;
        for(MDDEdge e : inEdges){
            if(e.getSource().isRelaxedDown()){
                relax += 1.0;
            }
            else{
                relax += e.getSource().relaxArityInCumulative();
            }
        }
        return relax/inEdges.size();
    }

    public double relaxArityOutCumulative(){
        double relax = 0;
        for(MDDEdge e : outEdges){
            if(e.getTarget().isRelaxedDown()){
                relax += 1.0;
            }else{
                relax += e.getTarget().relaxArityOutCumulative();
            }
        }
        return relax/outEdges.size();
    }

    public double relaxArityOut(){
        double relax = 0;
        for(MDDEdge e : outEdges){
            if(e.getTarget().isRelaxedDown()){
                relax += 1.0;
            }
        }
        return relax/outEdges.size();
    }

    public double distanceOutEdges(MDDNode n){
        if(n.outEdges.isEmpty() || this.outEdges.isEmpty()){
            return 1;
        }
        double similarity = 0;
        int same = 0;
        double toAdd = 0.0;
        for(MDDEdge e1 : outEdges){
            for(MDDEdge e2 : n.outEdges){
                if(e1.getTarget().getId() == e2.getTarget().getId() && e1.getValue() == e2.getValue()){
                    toAdd = 1.0;
                    same++;
                    break;
                } else if (e1.getTarget().getId() == e2.getTarget().getId()) {
                    toAdd = 0.5;
                }else if (e1.getValue() == e2.getValue()){
                    toAdd = 0.5;
                }
            }
            similarity += toAdd;
        }

        return -similarity/(outEdges.size()+n.outEdges.size()-same);
    }

    public double distanceInEdges(MDDNode n){
        if(n.inEdges.isEmpty() || this.inEdges.isEmpty()){
            return 1;
        }
        double similarity = 0;
        int same = 0;
        double toAdd = 0.0;
        for(MDDEdge e1 : inEdges){
            for(MDDEdge e2 : n.inEdges){
                if(e1.getSource().getId() == e2.getSource().getId() && e1.getValue() == e2.getValue()){
                    toAdd = 1.0;
                    same++;
                    break;
                } else if (e1.getSource().getId() == e2.getSource().getId()) {
                    toAdd = 0.5;
                }else if (e1.getValue() == e2.getValue()){
                toAdd = 0.5;
            }
            }
            similarity += toAdd;
        }

        return -similarity/(inEdges.size()+n.inEdges.size()-same);
    }



    public double distanceOutEdges2(MDDNode n) {
        if (this.outEdges.isEmpty() || n.outEdges.isEmpty()) {
            return 0; // No similarity if one node has no edges
        }

        List<Integer> values1 = this.outEdges.stream()
                .map(MDDEdge::getValue)
                .sorted()
                .toList();

        List<Integer> values2 = n.outEdges.stream()
                .map(MDDEdge::getValue)
                .sorted()
                .toList();

        double totalSimilarity = 0;
        int totalEdges = values1.size() + values2.size(); // Normalize by total edges

        int i = 0, j = 0;
        while (i < values1.size() && j < values2.size()) {
            double diff = Math.abs(values1.get(i) - values2.get(j));
            double similarity = 1.0 / (1.0 + diff); // Smaller diff = higher similarity
            totalSimilarity += similarity;

            // Move to next closest match
            if (values1.get(i) < values2.get(j)) {
                i++;
            } else {
                j++;
            }
        }

        // Normalize similarity
        return -(2 * totalSimilarity) / totalEdges; // Ensures symmetry
    }

    public double distanceEdges(MDDNode n){
        double similarityOut = 0;
        int sameOut = 0;
        double toAddOut = 0.0;
        for(MDDEdge e1 : outEdges){
            for(MDDEdge e2 : n.outEdges){
                if(e1.getTarget().getId() == e2.getTarget().getId() && e1.getValue() == e2.getValue()){
                    toAddOut = 1.0;
                    sameOut++;
                    break;
                } else if (e1.getTarget().getId() == e2.getTarget().getId()) {
                    toAddOut = 0.5;
                }else if (e1.getValue() == e2.getValue()){
                    toAddOut = 0.5;
                }
            }
            similarityOut += toAddOut;
        }

        double similarityIn = 0;
        int sameIn = 0;
        double toAddIn = 0.0;
        for(MDDEdge e1 : inEdges){
            for(MDDEdge e2 : n.inEdges){
                if(e1.getSource().getId() == e2.getSource().getId() && e1.getValue() == e2.getValue()){
                    toAddIn = 1.0;
                    sameIn++;
                    break;
                } else if (e1.getSource().getId() == e2.getSource().getId()) {
                    toAddIn = 0.5;
                }else if (e1.getValue() == e2.getValue()){
                    toAddIn = 0.5;
                }
            }
            similarityIn += toAddIn;
        }

        return -((similarityOut /(outEdges.size()+n.outEdges.size()- sameOut)) + (similarityIn /(inEdges.size()+n.inEdges.size()- sameIn)));
    }


    public boolean isRelaxedDown(){
        return state.isRelaxedDown();
    }

    public boolean isRelaxedUp(){
        return state.isRelaxedUp();
    }

    public boolean hasOutEdge(MDDNode target, int value) {
        for (MDDEdge e : this.outEdges) {
            if (target.getId() == e.getTarget().getId() && value == e.getValue()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasInEdge(MDDNode target, int value) {
        for (MDDEdge e : this.inEdges) {
            if (target.getId() == e.getTarget().getId() && value == e.getValue()) {
                return true;
            }
        }
        return false;
    }
    public void addOutEdge(MDDEdge edge) {
        outEdges.add(edge);
    }

    public void addInEdge(MDDEdge edge) {
        inEdges.add(edge);
    }

    public List<MDDEdge> getInEdges() {
        return this.inEdges;
    }

    public List<MDDEdge> getOutEdges() {
        return outEdges;
    }

    public int getLayer() {
        return layer;
    }

    public int getId() {
        return id;
    }

    public boolean isChildless() {
        return outEdges.isEmpty();
    }

    public boolean isOrphan() {
        return inEdges.isEmpty();
    }

    public void setState(MDDState state) {
        this.state = state;
    }

    public MDDState getState() {
        return state;
    }


    public void remove() {
        for (MDDEdge e : inEdges) {
            e.removeOut();
        }
        for (MDDEdge e : outEdges) {
            e.removeIn();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MDDNode mddNode = (MDDNode) o;
        return layer == mddNode.layer && id == mddNode.id;
    }

    public MDDEdge getOutEdgeByValue(int value, MDDNode target) {
        for (MDDEdge e : outEdges) {
            if (e.getValue() == value && e.getTarget().equals(target)) {
                return e;
            }
        }
        return null;
    }

    public MDDEdge getInEdgeByValue(int value, MDDNode source) {
        for (MDDEdge e : inEdges) {
            if (e.getValue() == value && source.equals(e.getSource())) {
                return e;
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(layer, id);
    }

}
