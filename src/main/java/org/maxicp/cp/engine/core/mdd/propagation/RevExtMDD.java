package org.maxicp.cp.engine.core.mdd.propagation;

import org.maxicp.cp.engine.core.mdd.*;

import java.util.*;

public class RevExtMDD extends ReversibleMDD {

    int factor = 1  ;
    int callCount = 0;
    ReversibleNodeStore[] nodeStore;
    static int nbTooBig = 0;

    public RevExtMDD(MDD mdd) {
        super(mdd);
        nodeStore = new ReversibleNodeStore[mdd.getLayers().size()];
        for (int i = 0; i < mdd.getLayers().size(); i++) {
            int size = i == 0 || i==mdd.getLayers().size()-1 ? 1 : mdd.widthHeuristic.width(i);
            nodeStore[i] = new ReversibleNodeStore(mdd.getLayers().get(i),size,
                    mdd.getVariables().getFirst().getSolver().getStateManager(),
                    mdd.getLayers().get(i).getFirst().getState(),
                    i);
        }
    }






    public void extractTopDown(MDDEdge e, MDDNode n){
        if(!n.getState().arcExist(e.getSource().getState(),e.getTarget().getState(),mdd.getVariables().get(n.getLayer()-1),e.getValue())){
            remove(e);
            n.updated = true;
            updateDownNode(n);
            return;
        }

        remove(e);
        n.updated = true;
        updateDownNode(n);

        MDDNode node = nodeStore[n.getLayer()].getNode();
        node.isActive.setValue(true);
        mdd.getLayers().get(node.getLayer()).add(node);
        node.getInEdges().clear();
        node.getOutEdges().clear();


        new MDDEdge(e.getSource(),node,e.getValue(),mdd.edgeIndex++);

        node.getState().transitionDown(e.getSource().getState(),mdd.getVariables().get(n.getLayer()-1),e.getValue(),true);

        ArrayList<MDDEdge> toRemove = new ArrayList<>();
        for(MDDEdge out : n.getOutEdges()){
            if(n.getState().arcExist(node.getState(),out.getTarget().getState(),mdd.getVariables().get(n.getLayer()),out.getValue())) {
                new MDDEdge(node, out.getTarget(), out.getValue(), mdd.edgeIndex++);
            }
            if(!n.getState().arcExist(n.getState(),out.getTarget().getState(),mdd.getVariables().get(n.getLayer()),out.getValue())) {
                toRemove.add(out);
            }
        }

        for(MDDEdge eR : toRemove){
            remove(eR);
        }

        updateUpNode(node);
        addToDownQueue(node);
        addToUpQueue(node);


    }





    @Override
    public void saveActiveGraph(String filename) {
        mdd.saveGraphToFile(filename);
    }




    private boolean extendLayer(int layer) {

        //System.out.println("extending "+layer);
        boolean layerChanged = false;

        for (MDDNode n : mdd.getLayers().get(layer)) {
            //System.out.println(n.getId()+" "+n.isRelaxedDown());
            n.updated = false;
        }

        while (mdd.getLayers().get(layer).size() < mdd.widthHeuristic.width(layer) / factor) {
            MDDNode nodeToSplit = null;
            double min = 10;
            for (MDDNode n : mdd.getLayers().get(layer)) {
                double value = n.relaxArityIn();
                if (value < min) {
                    if(mdd.isTopDownLayer(layer) && (n.getInEdges().size() <= 1 || !n.isRelaxedDown())) continue;
                    if(!mdd.isTopDownLayer(layer) && (n.getOutEdges().size() <= 1 || !n.isRelaxedUp())) continue;
                    nodeToSplit = n;
                    min = value;
                }
            }
            if (nodeToSplit == null) {
                break; // No more nodes to split
            }
            addToDownQueue(nodeToSplit);
            addToUpQueue(nodeToSplit);
            for(MDDEdge e : nodeToSplit.getInEdges()) {
                addToUpQueue(e.getSource());
            }
            for(MDDEdge e : nodeToSplit.getOutEdges()) {
                addToDownQueue(e.getTarget());
            }
            while(nodeToSplit.isRelaxedDown() && mdd.getLayers().get(layer).size() < mdd.widthHeuristic.width(layer)) {
                MDDEdge e = nodeToSplit.getInEdges().getFirst();
                extractTopDown(e,nodeToSplit);
            }


            layerChanged = true;





        }



        for(MDDNode n : mdd.getLayers().get(layer)) {
            if (n.updated) {
                addToUpQueue(n);
                addToDownQueue(n);
                for(MDDEdge e : n.getInEdges()) {
                    addToUpQueue(e.getSource());
                }
                for(MDDEdge e : n.getOutEdges()) {
                    addToDownQueue(e.getTarget());
                }
            }
        }



        return layerChanged;
    }


    @Override
    public void remove(MDDEdge e) {

        e.getSource().getOutEdges().remove(e);
        e.getTarget().getInEdges().remove(e);
        domainUpdated.set(e.getSource().getLayer());

    }


    public void removeNode(MDDNode n) {
        n.isActive.setValue(false);
        mdd.getLayers().get(n.getLayer()).remove(n);
        nodeStore[n.getLayer()].releaseNode(n);
    }

    protected HashSet<Integer> getDomain(int layer) {
        domain.clear();
        for (MDDNode n : mdd.getLayers().get(layer)) {
            if (!(n.getInEdges().isEmpty() && n.getOutEdges().isEmpty()) || n.equals(mdd.getRoot()) || n.equals(mdd.getSink())) {
                for (MDDEdge e : n.getOutEdges()) {
                    domain.add(e.getValue());
                }
            }

        }
        return domain;
    }

    public boolean updateDownNode(MDDNode n) {
        long previousHash = n.getState().computeHash();
        boolean firstDown = true;
        for (int i = 0; i < n.getInEdges().size(); i++) {
            n.getState().transitionDown(n.getInEdges().get(i).getSource().getState(), mdd.getVariables().get(n.getLayer() - 1), n.getInEdges().get(i).getValue(), firstDown);
            firstDown = false;
        }
        return previousHash != n.getState().computeHash();
    }

    public boolean updateUpNode(MDDNode n) {
        long previousHash = n.getState().computeHash();
        boolean firstDown = true;
        for (int i = 0; i < n.getOutEdges().size(); i++) {
            n.getState().transitionUp(n.getOutEdges().get(i).getTarget().getState(), mdd.getVariables().get(n.getLayer()), n.getOutEdges().get(i).getValue(), firstDown);
            firstDown = false;

        }
        return previousHash != n.getState().computeHash();
    }




    public void updateMDD() {
        updateMDD(false);
    }

    public void updateMDD(boolean verbose) {

        if(verbose) System.out.println(upQueue.stream().map(MDDNode::getId).toList());
        if(verbose) System.out.println(downQueue.stream().map(MDDNode::getId).toList());

        while(!upQueue.isEmpty() || !downQueue.isEmpty()){

            while (!upQueue.isEmpty()) {
                MDDNode n = upQueue.poll();
                if(!isNodeActive(n)) continue;
                if(verbose) System.out.println("studing Up "+n.getId()+" "+n.getLayer());
                upSet.remove(n);
                if (n.isChildless()) {//Suppression node
                    if(verbose) System.out.println("removing childless "+n.getId()+" "+n.getLayer());
                    for (MDDEdge e : n.getInEdges()) {
                        addToUpQueue(e.getSource());
                        remove(e);
                    }
                    removeNode(n);
                } else if (updateUpNode(n)) {//Si maj du noeud
                    if(verbose) System.out.println("updating up "+n.getId()+" "+n.getLayer());
                    for (MDDEdge e : n.getInEdges()) {// reverifie Arc existence
                        if (!n.getState().arcExist(e.getSource().getState(), e.getTarget().getState(), mdd.getVariables().get(e.getSource().getLayer()), e.getValue())) {
                            remove(e);
                            addToDownQueue(e.getTarget());

                        }
                        addToUpQueue(e.getSource());
                    }
                }
            }

            while (!downQueue.isEmpty()) {
                MDDNode n = downQueue.poll();
                if(!isNodeActive(n)) continue;
                if(verbose) System.out.println("studing Down "+n.getId()+" "+n.getLayer());
                downSet.remove(n);
                if (n.isOrphan()) {
                    if(verbose) System.out.println("removing orphan "+n.getId()+" "+n.getLayer());
                    for (MDDEdge e : n.getOutEdges()) {
                        addToDownQueue(e.getTarget());
                        remove(e);
                    }
                    removeNode(n);
                } else if (updateDownNode(n)) {
                    if(verbose) System.out.println("updating down "+n.getId()+" "+n.getLayer());
                    for (MDDEdge e : n.getOutEdges()) {
                        if (!n.getState().arcExist(e.getSource().getState(), e.getTarget().getState(), mdd.getVariables().get(e.getSource().getLayer()), e.getValue())) {
                            remove(e);
                            addToUpQueue(e.getSource());
                        }
                        addToDownQueue(e.getTarget());

                    }
                }
            }







        }





    }


    public void emptyQueues() {


        updateMDD();

        clearQueues();

        boolean changed = false;
        for (int i = 1; i < mdd.getLayers().size() - 1; i++) {
            if (mdd.getLayers().get(i).size() <= mdd.widthHeuristic.width(i) / factor) {
                changed = extendLayer(i) || changed;
            }
        }

        if(changed) updateMDD();





    }


    @Override
    public List<MDDEdge> getActiveInEdges(MDDNode n) {
        return n.getInEdges();
    }

    @Override
    public List<MDDEdge> getActiveOutEdges(MDDNode n) {
        return n.getOutEdges();
    }

    @Override
    public boolean isNodeActive(MDDNode n) {
        return n.isActive.value();
    }

}
