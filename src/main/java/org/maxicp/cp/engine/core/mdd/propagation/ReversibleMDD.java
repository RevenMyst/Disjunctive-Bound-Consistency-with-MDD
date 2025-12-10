package org.maxicp.cp.engine.core.mdd.propagation;

import org.glassfish.json.JsonUtil;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPIntervalVar;
import org.maxicp.cp.engine.core.mdd.MDD;
import org.maxicp.cp.engine.core.mdd.MDDEdge;
import org.maxicp.cp.engine.core.mdd.MDDNode;

import javax.swing.table.TableCellEditor;
import java.util.*;

public abstract class ReversibleMDD {

    public MDD mdd;
    public long nbPropagate = 0;
    protected PriorityQueue<MDDNode> upQueue = new PriorityQueue<>(Comparator.comparingInt(MDDNode::getLayer).reversed());
    protected PriorityQueue<MDDNode> downQueue = new PriorityQueue<>(Comparator.comparingInt(MDDNode::getLayer));
    protected HashSet<MDDNode> upSet = new HashSet<>();
    protected HashSet<MDDNode> downSet = new HashSet<>();
    protected HashSet<Integer> domain = new HashSet<>();
    protected BitSet domainUpdated;
    int[] values;
    public int[] useful;
    public int[] useless;
    public ReversibleMDD(MDD mdd) {
        this.mdd = mdd;
        mdd.post();
        //mdd.shrink();
        this.domainUpdated = new BitSet(mdd.getVariables().size());
        this.values = new int[mdd.getVariables().stream().mapToInt(CPIntVar::size).max().getAsInt()];

        useful = new int[mdd.getVariables().size() + 1];
        useless = new int[mdd.getVariables().size() + 1];
        Arrays.fill(useful, 0);
        Arrays.fill(useless, 0);
    }

    public abstract void saveActiveGraph(String filename);

    public MDD getMDD() {
        return this.mdd;
    }
    public void clearQueues(){
        upQueue.clear();
        downQueue.clear();
        upSet.clear();
        downSet.clear();
        domainUpdated.clear();

    }
    public String printQueue(PriorityQueue<MDDNode> queue){
        String res = "";
        PriorityQueue<MDDNode> copy = new PriorityQueue<>(queue);
        while (!copy.isEmpty()){
            MDDNode n = copy.poll();
            res += n.getId()+" ("+n.getLayer()+") ";
        }
        return res;
    }
    public abstract void remove(MDDEdge e);
    protected abstract HashSet<Integer> getDomain(int layer);
    public void extract(boolean firstTime) {
        for(int i = 0; i<mdd.getVariables().size(); i++){
            if(domainUpdated.get(i) || firstTime){
                domain = getDomain(i);
                int nbValues = mdd.getVariables().get(i).fillArray(values);
                for(int j = 0; j < nbValues; j++){
                    if(!domain.contains(values[j])){
                        mdd.getVariables().get(i).remove(values[j]);
                    }
                }
            }
        }

        clearQueues();

        //System.out.println("usefull/useless: "+usefull+"/"+useless);
    }
    public abstract void emptyQueues();
    public void addToDownQueue(MDDNode n){
        if(!downSet.contains(n)){
            downQueue.add(n);
            downSet.add(n);
        }
    }
    public void addToUpQueue(MDDNode n){
        if(!upSet.contains(n)){
            upQueue.add(n);
            upSet.add(n);
        }
    }
    public abstract List<MDDEdge> getActiveInEdges(MDDNode n);
    public abstract List<MDDEdge> getActiveOutEdges(MDDNode n);
    public abstract boolean isNodeActive(MDDNode n);

}
