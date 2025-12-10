package org.maxicp.cp.engine.core.mdd;

import org.maxicp.state.datastructures.StateDoubleLinkedListNode;

import java.util.Objects;

public class MDDEdge{

    private MDDNode source;
    private MDDNode target;
    public StateDoubleLinkedListNode<MDDEdge> top;
    public StateDoubleLinkedListNode<MDDEdge> bottom ;
    private final int value; // change to generic type ?
    private final int id;

    public MDDEdge(MDDNode source, MDDNode target, int value, int id) {
        this.top = new StateDoubleLinkedListNode<>(source.sm, this);
        this.bottom = new StateDoubleLinkedListNode<>(source.sm, this);
        this.source = source;
        this.target = target;
        this.value = value;
        this.source.addOutEdge(this);
        this.target.addInEdge(this);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void remove() {
        this.target.getInEdges().remove(this);
        this.source.getOutEdges().remove(this);
        this.source = null;
        this.target = null;
    }

    public void removeIn() {
        this.target.getInEdges().remove(this);
        this.source = null;
        this.target = null;
    }

    public void removeOut() {
        this.source.getOutEdges().remove(this);
        this.source = null;
        this.target = null;
    }

    public MDDNode getSource() {
        return source;
    }

    public MDDNode getTarget() {
        return target;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MDDEdge edge = (MDDEdge) o;
        return value == edge.value && Objects.equals(source, edge.source) && Objects.equals(target, edge.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target, value);
    }

    @Override
    public String toString() {
        return id+" : "+ source.getId() + " -> " + target.getId() + " (" + value + ")";
    }
}
