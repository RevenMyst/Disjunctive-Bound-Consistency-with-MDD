package org.maxicp.cp.engine.core.mdd.propagation;

import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPIntervalVar;
import org.maxicp.cp.engine.core.mdd.MDD;
import org.maxicp.cp.engine.core.mdd.MDDEdge;
import org.maxicp.cp.engine.core.mdd.MDDNode;
import org.maxicp.cp.engine.core.mdd.MDDState;
import org.maxicp.state.StateManager;
import org.maxicp.state.datastructures.StateSparseSet;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class RevMDD extends ReversibleMDD{
    StateManager sm;
    public HashMap<MDDNode, StateSparseSet> inEdges = new HashMap<>();
    public HashMap<MDDNode,StateSparseSet> outEdges = new HashMap<>();
    int[] values;
    public int studiedNodes = 0;

    public RevMDD(MDD mdd){
        super(mdd);
        //this.mdd.shrink();
        this.sm = mdd.getVariables().getFirst().getSolver().getStateManager();
        for(List<MDDNode> l :mdd.getLayers()){
            for(MDDNode n : l){
                if(!n.equals(mdd.getRoot()))
                    inEdges.put(n,new StateSparseSet(sm,n.getInEdges().stream()
                            .map(MDDEdge::getId)
                            .collect(Collectors.toSet())));

                if(!n.equals(mdd.getSink()))
                    outEdges.put(n,new StateSparseSet(sm,n.getOutEdges().stream()
                            .map(MDDEdge::getId)
                            .collect(Collectors.toSet())));
            }

        }

        this.values = new int[mdd.getVariables().stream().mapToInt(CPIntVar::size).max().getAsInt()];
    }

    public boolean updateDownNode(MDDNode n){
        int previousHash = n.getState().computeHashCode();
        boolean firstDown = true;
        for (int i = 0; i < n.getInEdges().size(); i++) {
            if(inEdges.get(n).contains(n.getInEdges().get(i).getId())){
                n.getState().transitionDown(n.getInEdges().get(i).getSource().getState(), mdd.getVariables().get(n.getLayer() - 1), n.getInEdges().get(i).getValue(),firstDown);
                firstDown = false;
            }
        }

        return !(n.getState().computeHashCode() == previousHash);
    }

    public boolean updateUpNode(MDDNode n){
        MDDState state = n.getState().clone();
        boolean firstDown = true;
        for (int i = 0; i < n.getOutEdges().size(); i++) {
            if(outEdges.get(n).contains(n.getOutEdges().get(i).getId())){
                n.getState().transitionUp(n.getOutEdges().get(i).getTarget().getState(), mdd.getVariables().get(n.getLayer()), n.getOutEdges().get(i).getValue(),firstDown);
                firstDown = false;
            }
        }
        return !state.equals(n.getState());
    }

    public void remove(MDDEdge e){
        inEdges.get(e.getTarget()).remove(e.getId());
        outEdges.get(e.getSource()).remove(e.getId());
        domainUpdated.set(e.getSource().getLayer());
    }


    @Override
    public List<MDDEdge> getActiveInEdges(MDDNode n) {
        return n.getInEdges().stream().filter(this::isEdgeActive).toList();
    }

    @Override
    public List<MDDEdge> getActiveOutEdges(MDDNode n) {
        return n.getOutEdges().stream().filter(this::isEdgeActive).toList();
    }


    public String activeArchToString(){
        String str = "";
        for(int i = 1; i < mdd.getVariables().size()-1; i++){
            int count = 0;
            int edges = 0;
            for(MDDNode n : mdd.getLayers().get(i)){
                if(isNodeActive(n)){
                    count++;
                    edges += inEdges.get(n).size();
                }
            }
            str += count +" ("+edges+") ";
        }
        return str.trim();
    }

    public void emptyQueues(){

        //while(!upQueue.isEmpty() || !downQueue.isEmpty()){
            while(!upQueue.isEmpty()){
                MDDNode n = upQueue.poll();
                upSet.remove(n);
                if(isChildless(n)){
                    for(MDDEdge e : n.getInEdges()){
                        if(n.equals(mdd.getRoot()) || inEdges.get(n).contains(e.getId())){
                            remove(e);
                            addToUpQueue(e.getSource());
                        }
                    }
                } else if(updateUpNode(n)) {
                    for (MDDEdge e : n.getInEdges()) {
                        if (!n.getState().arcExist(e.getSource().getState(), e.getTarget().getState(), mdd.getVariables().get(e.getSource().getLayer()), e.getValue()) && (n.equals(mdd.getRoot()) || inEdges.get(n).contains(e.getId()))) {
                            remove(e);
                            if(isOrphan(e.getTarget())){
                                //addToDownQueue(e.getTarget());
                            }
                        }
                        addToUpQueue(e.getSource());
                    }

                }

            }

            while(!downQueue.isEmpty()){
                MDDNode n = downQueue.poll();
                //System.out.println("studing Down "+n.getId()+" ("+n.getLayer()+") "+printQueue(downQueue));
                downSet.remove(n);
                if(isOrphan(n)){
                    //System.out.println("Orphan");
                    for(MDDEdge e : n.getOutEdges()){
                        if(n.equals(mdd.getSink()) || outEdges.get(n).contains(e.getId())){
                            remove(e);
                            addToDownQueue(e.getTarget());
                        }
                    }
                } else if(updateDownNode(n)) {
                    //System.out.println("updated");
                    for (MDDEdge e : n.getOutEdges()) {
                        if (!n.getState().arcExist(e.getSource().getState(), e.getTarget().getState(), mdd.getVariables().get(e.getSource().getLayer()), e.getValue()) && (n.equals(mdd.getSink()) || outEdges.get(n).contains(e.getId()))) {
                            remove(e);
                            if(isChildless(e.getSource())){
                                //addToUpQueue(e.getSource());
                            }
                            //System.out.println("edge "+e+" removed");
                            addToDownQueue(e.getTarget());
                        }
                    }
                }
            }
        //}


    }

    private boolean isOrphan(MDDNode n){
        return inEdges.get(n) == null || inEdges.get(n).isEmpty();
    }

    private boolean isChildless(MDDNode n){
        return outEdges.get(n) == null ||outEdges.get(n).isEmpty();
    }

    public boolean isNodeActive(MDDNode n){
        return (n.equals(mdd.getRoot()) || !inEdges.get(n).isEmpty()) && (n.equals(mdd.getSink()) || !outEdges.get(n).isEmpty());
    }

    private boolean isEdgeActive(MDDEdge e){
        return (e.getTarget().equals(mdd.getSink()) || inEdges.get(e.getTarget()).contains(e.getId())) && (e.getSource().equals(mdd.getRoot()) || outEdges.get(e.getSource()).contains(e.getId()));
    }

    public HashSet<Integer> getDomain(int layer){
        domain.clear();
        for(MDDNode n : mdd.getLayers().get(layer)){
            if(!(isOrphan(n) && isChildless(n)) || n.equals(mdd.getRoot()) || n.equals(mdd.getSink())) {
                for(MDDEdge e : n.getOutEdges()){
                    if(isEdgeActive(e)){
                        domain.add(e.getValue());
                    }
                }
            }

        }
        return domain;
    }


    public void saveActiveGraph(String filename) {
        ArrayList<MDDNode> allNodes = new ArrayList<>();
        for (List<MDDNode> layer : mdd.getLayers()) {
            allNodes.addAll(layer);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {

            ArrayList<MDDNode> activeNodes = new ArrayList<>();

            // Write active nodes
            for (MDDNode node : allNodes) {
                boolean hasActiveIn = inEdges.containsKey(node) && !inEdges.get(node).isEmpty();
                boolean hasActiveOut = outEdges.containsKey(node) && !outEdges.get(node).isEmpty();
                if ((hasActiveIn && hasActiveOut) || node.equals(mdd.getRoot()) || node.equals(mdd.getSink())) {
                    writer.write("N|" + node.getId() + "|" + node.getLayer() + "|" + node.getState() + "|" +
                            (inEdges.containsKey(node) ? inEdges.get(node).size() : 0) + "|" +
                            (outEdges.containsKey(node) ? outEdges.get(node).size() : 0) + "\n");
                    activeNodes.add(node);
                }
            }

            // Write active edges
            for (MDDNode node : activeNodes) {
                for (MDDEdge edge : node.getOutEdges()) {
                    if (isEdgeActive(edge)) {
                        writer.write("E|" + edge.getSource().getId() + "|" + edge.getTarget().getId() + "|" + edge.getValue() + "\n");
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }



}


