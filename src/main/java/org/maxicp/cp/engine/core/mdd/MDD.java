package org.maxicp.cp.engine.core.mdd;

import org.maxicp.cp.engine.core.*;
import org.maxicp.cp.engine.core.mdd.heuristics.MergingHeuristic;
import org.maxicp.cp.engine.core.mdd.heuristics.WidthHeuristic;
import org.maxicp.state.StateManager;
import org.maxicp.state.datastructures.StateDoubleLinkedListMDD;
import org.maxicp.state.trail.Trailer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class MDD {

    protected StateManager sm;
    protected List<CPIntVar> variables = new ArrayList<>();
    protected List<StateDoubleLinkedListMDD<MDDNode>> layers = new ArrayList<>();
    protected MDDNode root;
    protected MDDNode sink;
    private final List<MDDSpecs> specs = new ArrayList<>();

    protected List<Map<MDDState, MDDNode>> layerMaps = new ArrayList<>();
    public int edgeIndex;
    public int maxWidth;
    public MergingHeuristic mergingHeuristic = MergingHeuristic.randomMerging();
    public WidthHeuristic widthHeuristic = WidthHeuristic.FixedWidthHeuristic(Integer.MAX_VALUE);
    // for printing purpose
    String[] variableNames = new String[0];

    LinkedList<MDDNode> toRemove = new LinkedList<>();

    // constructors
    public MDD() {
        this.sm = new Trailer();
    }

    public MDD(StateManager sm) {
        this.sm = sm;
    }

    public StateManager getStateManager() {
        return sm;
    }

    public List<CPIntVar> getVariables() {
        return this.variables;
    }

    public void setVariables(List<CPIntVar> variables) {
        this.variables = variables;
    }

    // TODO toberemove
    public List<CPIntVar> gatherVariables() {
        Set<CPIntVar> set = new HashSet<CPIntVar>();
        for (MDDSpecs spec : specs) {
            set.addAll(spec.getVariables());
        }
        return set.stream().toList();
    }

    // TODO toberemove
    private void initVariableList() {
        Set<CPIntVar> set = new HashSet<CPIntVar>();
        for (MDDSpecs spec : specs) {
            set.addAll(spec.getVariables());
        }
        variables.addAll(set);
    }

    public List<StateDoubleLinkedListMDD<MDDNode>> getLayers() {
        return layers;
    }

    public MDDNode getRoot() {
        return root;
    }

    public MDDNode getSink() {
        return sink;
    }

    public void post(MDDSpecs s) {
        specs.add(s);
        // TODO add assert to check if var are in mdd
    }

    public List<MDDSpecs> getSpecs() {
        return specs;
    }

    public int width() {
        return this.getArch().stream().mapToInt(Integer::intValue).max().getAsInt();
    }

    public int size() {
        return this.getArch().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isTopDownLayer(int layer) {
        return true;
    }

    /**
     * Initializes the universe of the MDD by creating the root node, adding variables,
     * and generating the initial structure of the MDD.
     */
    protected void initUniverse() {
        layers.add(new StateDoubleLinkedListMDD<>(sm, e -> e.lh));
        layerMaps.add(new HashMap<>());
        MDDState state = new MDDState();
        for (MDDSpecs s : specs) {
            state.addSpecs(s);
        }

        this.root = new MDDNode(0, state, sm);
        addNode(this.root);

        int[] values = new int[variables.stream().map(CPIntVar::size).max(Integer::compareTo).orElse(0)];

        for (int i = 1; i <= variables.size(); i++) {
            MDDState previousState = layers.getLast().getFirst().getState();


            int nbValues = variables.get(i - 1).fillArray(values);

            MDDState nState = previousState.clone();

            MDDNode node = new MDDNode(i, nState, sm);

            node.getState().transitionDown(previousState, variables.get(i - 1), values, nbValues, true);
            for (int j = 0; j < nbValues; j++) {
                MDDEdge e = new MDDEdge(layers.getLast().getFirst(), node, values[j], edgeIndex++);
            }

            layers.add(new StateDoubleLinkedListMDD<>(sm, e -> e.lh));
            layerMaps.add(new HashMap<>());
            addNode(node);
        }
        sink = layers.getLast().getFirst();

        for (int i = layers.size() - 2; i >= 0; i--) {
            updateUpNode(layers.get(i).getFirst());
        }
    }

    /**
     * Main method to build the MDD.
     * This method initializes the universe of the MDD, splits down nodes, merges layers if necessary,
     * updates down nodes, and finally updates up nodes.
     */
    public void post() {
        initUniverse();

        for (int i = 1; i < layers.size() - 1; i++) {
            splitDownNode(layers.get(i).getFirst());
            if (layers.get(i).size() > widthHeuristic.width(i)) {
                mergingHeuristic.mergeLayerFromTop(this, i);
            }
            widthHeuristic.effectiveSize(i, layers.get(i).size());
        }

        updateDownNode(sink);

        for (int i = layers.size() - 2; i >= 0; i--) {
            toRemove.clear();
            for (MDDNode n : layers.get(i)) {
                if (n.isChildless()) {
                    toRemove.add(n);
                } else {
                    updateUpNode(n);
                }
            }
            for (MDDNode n : toRemove) {
                removeNode(n);
            }
        }

    }


    public void addNode(MDDNode node) {
        try {
            int layerID = node.getLayer();
            layers.get(layerID).add(node);
            layerMaps.get(layerID).put(node.getState(), node);
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException("Adding node to non existing layer");
        }
    }


    public void splitDownNode(MDDNode node) {
        CPIntVar var = variables.get(node.getLayer() - 1);
        // Removing node so that it wont be selected as same node when generating
        layers.get(node.getLayer()).remove(node);
        layerMaps.get(node.getLayer()).remove(node.getState());
        for (MDDEdge e : node.getInEdges()) {
            generateDownNode(e.getSource(), e.getTarget(), var, e.getValue());
        }
        node.remove();
    }

    protected void splitUpNode(MDDNode node) {
        CPIntVar var = variables.get(node.getLayer());
        // Removing node so that it wont be selected as same node when generating
        layers.get(node.getLayer()).remove(node);
        layerMaps.get(node.getLayer()).remove(node.getState());
        for (MDDEdge e : node.getOutEdges()) {
            generateUpNode(e.getTarget(), e.getSource(), var, e.getValue());
        }
        node.remove();
    }

    public void updateUpNode(MDDNode node) {
        for (int i = 0; i < node.getOutEdges().size(); i++) {
            node.getState().transitionUp(node.getOutEdges().get(i).getTarget().getState(), variables.get(node.getLayer()), node.getOutEdges().get(i).getValue(), i == 0);
        }
    }

    public void updateDownNode(MDDNode node) {
        for (int i = 0; i < node.getInEdges().size(); i++) {
            node.getState().transitionDown(node.getInEdges().get(i).getSource().getState(), variables.get(node.getLayer() - 1), node.getInEdges().get(i).getValue(), i == 0);


        }
    }

    /**
     * Retrieves a node from the MDD based on its state and layer.
     * If the node does not exist, it creates a new one.
     * used on splitting
     *
     * @param state The state of the node to retrieve.
     * @param layer The layer of the node to retrieve.
     * @return The MDDNode corresponding to the given state and layer.
     */
    public MDDNode getNodeFromState(MDDState state, int layer) {

        if (layerMaps.get(layer).containsKey(state)) {
            return layerMaps.get(layer).get(state);
        }
        MDDNode newNode = new MDDNode(layer, state, sm);
        addNode(newNode);
        return newNode;
    }

    public void generateDownNode(MDDNode from, MDDNode to, CPIntVar var, int val) {
        if (from.getState().arcExist(from.getState(), to.getState(), var, val)) {
            MDDState state = from.getState().clone();
            state.transitionDown(from.getState(), var, val, true);
            MDDNode newNode = getNodeFromState(state, to.getLayer());
            for (MDDEdge e : to.getOutEdges()) {
                if (state.arcExist(state, e.getTarget().getState(), variables.get(e.getTarget().getLayer() - 1), e.getValue()) &&
                        !newNode.hasOutEdge(e.getTarget(), e.getValue())) {
                    MDDEdge nE = new MDDEdge(newNode, e.getTarget(), e.getValue(), edgeIndex++);
                }
            }
            if (!newNode.equals(to)) {
                MDDEdge nE = new MDDEdge(from, newNode, val, edgeIndex++);
            }
            if (newNode.getOutEdges().isEmpty()) {
                removeNode(newNode);
            }
        }

    }

    private void generateUpNode(MDDNode from, MDDNode to, CPIntVar var, int val) {
        if (to.getState().arcExist(to.getState(), from.getState(), var, val)) {
            MDDState state = from.getState().clone();
            state.transitionUp(from.getState(), var, val, true);
            MDDNode newNode = getNodeFromState(state, to.getLayer());
            for (MDDEdge e : to.getInEdges()) {
                if (state.arcExist(e.getSource().getState(), state, variables.get(e.getTarget().getLayer() - 1), e.getValue()) &&
                        !e.getSource().hasOutEdge(newNode, e.getValue())) {
                    MDDEdge nE = new MDDEdge(e.getSource(), newNode, e.getValue(), edgeIndex++);
                }
            }
            if (!newNode.equals(to)) {
                MDDEdge nE = new MDDEdge(newNode, from, val, edgeIndex++);
            }
            if (newNode.getInEdges().isEmpty()) {
                layers.get(newNode.getLayer()).remove(newNode);
                newNode.remove();
            }
        }

    }

    public void removeNode(MDDNode node) {
        layers.get(node.getLayer()).remove(node);
        layerMaps.get(node.getLayer()).remove(node.getState());
        node.remove();
    }

    public void recursiveRemoveNode(MDDNode node) {
        PriorityQueue<MDDNode> upQueue = new PriorityQueue<>(Comparator.comparingInt(MDDNode::getLayer));
        PriorityQueue<MDDNode> downQueue = new PriorityQueue<>(Comparator.comparingInt(MDDNode::getLayer).reversed());

        for (MDDEdge e : node.getInEdges()) {
            upQueue.add(e.getSource());
            e.getSource().getOutEdges().remove(e);
        }
        for (MDDEdge e : node.getOutEdges()) {
            downQueue.add(e.getTarget());
            e.getTarget().getInEdges().remove(e);
        }
        layers.get(node.getLayer()).remove(node);
        while (!upQueue.isEmpty()) {
            MDDNode upNode = upQueue.poll();
            if (upNode.isChildless()) {
                for (MDDEdge e : upNode.getInEdges()) {
                    upQueue.add(e.getSource());
                    e.getTarget().getOutEdges().remove(e);
                }
                layers.get(upNode.getLayer()).remove(upNode);
            }
        }
        while (!downQueue.isEmpty()) {
            MDDNode downNode = downQueue.poll();
            if (downNode.isOrphan()) {
                for (MDDEdge e : downNode.getOutEdges()) {
                    downQueue.add(e.getTarget());
                    e.getSource().getInEdges().remove(e);
                }
                layers.get(downNode.getLayer()).remove(downNode);
            }
            // update the node


        }
    }

    public void splitDownNodeReversible(MDDNode node) {
        CPIntVar var = variables.get(node.getLayer() - 1);
        // Removing node so that it wont be selected as same node when generating
        layers.get(node.getLayer()).remove(node);
        for (MDDEdge e : node.getInEdges()) {
            generateDownNode(e.getSource(), e.getTarget(), var, e.getValue());
        }
        for (MDDEdge e : node.getInEdges()) {
            e.getSource().getOutEdges().remove(e);
        }
        for (MDDEdge e : node.getOutEdges()) {
            e.getTarget().getInEdges().remove(e);
        }
    }

    public void saveGraphToFile(String filename) {
        ArrayList<MDDNode> nodes = new ArrayList<>();
        for (List<MDDNode> layer : layers) {
            nodes.addAll(layer);
        }
        ArrayList<MDDEdge> edges = new ArrayList<>();
        for (MDDNode node : nodes) {
            edges.addAll(node.getOutEdges());
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            if (variableNames.length > 0) {
                writer.write("V|");
                for (String s : variableNames) {
                    writer.write(s + " ");
                }
                writer.write("\n");
            }
            // Write nodes
            for (MDDNode node : nodes) {
                writer.write("N|" + node.getId() + "|" + node.getLayer() + "|" + node.getState() + /*"|" + node.isRelaxedDown() +*/ "|" + node.getInEdges().size() + "|" + node.getOutEdges().size() + "\n");
            }
            for (MDDEdge edge : edges) {
                String source = (edge.getSource() != null) ? String.valueOf(edge.getSource().getId()) : null;
                String target = (edge.getTarget() != null) ? String.valueOf(edge.getTarget().getId()) : null;
                writer.write("E|" + source + "|" + target + "|" + edge.getValue() + "\n");
            }

        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }


    public void shrink() {


        for (int i = layers.size() - 2; i > 0; i--) {

            List<List<MDDNode>> nodesToMerge = new ArrayList<>();
            List<MDDNode> usedElements = new ArrayList<>();

            for (int j = 0; j < layers.get(i).size() - 1; j++) {
                MDDNode n1 = layers.get(i).get(j);
                if (usedElements.contains(n1)) continue;

                List<MDDNode> group = new ArrayList<>();
                group.add(n1);


                for (int k = j + 1; k < layers.get(i).size(); k++) {
                    MDDNode n2 = layers.get(i).get(k);
                    if (n1.getOutEdges().size() == n2.getOutEdges().size()) {
                        boolean allSame = true;
                        for (int l = 0; l < n1.getOutEdges().size(); l++) {
                            MDDEdge e = n2.getOutEdgeByValue(n1.getOutEdges().get(l).getValue(), n1.getOutEdges().get(l).getTarget());
                            if (e == null || !e.getTarget().equals(n1.getOutEdges().get(l).getTarget())) {
                                allSame = false;
                            }
                        }
                        if (allSame) {
                            group.add(n2);
                            usedElements.add(n2);
                        }
                    }
                }

                if (group.size() > 1) {
                    nodesToMerge.add(group);
                }
            }

            for (List<MDDNode> lst : nodesToMerge) {
                mergeNodesFromTop(lst);
            }
        }

        //System.out.println("arraySize : "+arraySize()+" width : "+width());
    }

    protected void mergeNodesFromTop(List<MDDNode> lst) {
        MDDNode baseNode = lst.getFirst();
        for (int i = 1; i < lst.size(); i++) {
            for (MDDEdge e : lst.get(i).getInEdges()) {
                MDDEdge in = baseNode.getInEdgeByValue(e.getValue(), e.getSource());
                if (in == null || !in.getSource().equals(e.getSource())) {
                    MDDEdge nE = new MDDEdge(e.getSource(), baseNode, e.getValue(), edgeIndex++);
                }
            }
            // should not be null except when built from trie
            if (baseNode.getState() != null) baseNode.getState().relax(lst.get(i).getState());
            removeNode(lst.get(i));
        }

    }

    public MDDNode mergeNodes(List<MDDNode> lst) {
        MDDNode baseNode = lst.getFirst();
        for (int i = 1; i < lst.size(); i++) {
            for (MDDEdge e : lst.get(i).getOutEdges()) {
                MDDEdge out = baseNode.getOutEdgeByValue(e.getValue(), e.getTarget());
                if (out == null) {
                    MDDEdge nE = new MDDEdge(baseNode, e.getTarget(), e.getValue(), edgeIndex++);
                }
            }
            for (MDDEdge e : lst.get(i).getInEdges()) {
                MDDEdge in = baseNode.getInEdgeByValue(e.getValue(), e.getSource());
                if (in == null || !in.getSource().equals(e.getSource())) {
                    MDDEdge nE = new MDDEdge(e.getSource(), baseNode, e.getValue(), edgeIndex++);
                }
            }
            // should not be null except when built from trie
            if (baseNode.getState() != null) baseNode.getState().relax(lst.get(i).getState());
            removeNode(lst.get(i));
        }
        baseNode.updated = true;
        return baseNode;
    }

    protected void mergeNodesFromBottom(List<MDDNode> lst) {
        MDDNode baseNode = lst.getFirst();
        for (int i = 1; i < lst.size(); i++) {
            for (MDDEdge e : lst.get(i).getOutEdges()) {
                MDDEdge out = baseNode.getOutEdgeByValue(e.getValue(), e.getTarget());
                if (out == null) {
                    MDDEdge nE = new MDDEdge(baseNode, e.getTarget(), e.getValue(), edgeIndex++);
                }
            }
            // should not be null except when built from trie
            if (baseNode.getState() != null) baseNode.getState().relax(lst.get(i).getState());
            removeNode(lst.get(i));
        }

    }


    public List<List<Integer>> getAllSolutions() {
        List<List<Integer>> solutions = new ArrayList<>();
        List<Integer> currentSolution = new ArrayList<>();
        traverse(root, currentSolution, solutions);
        return solutions;

    }

    public BigInteger numberOfSolutions() {
        Map<MDDNode, BigInteger> memo = new HashMap<>();
        return countPaths(root, memo);
    }

    // Recursive helper method to count paths from a given node to the sink
    private BigInteger countPaths(MDDNode node, Map<MDDNode, BigInteger> memo) {
        // If we reach the sink, there's exactly one path
        if (node == sink) {
            return BigInteger.valueOf(1);
        }

        // If this node has already been processed, return the stored result
        if (memo.containsKey(node)) {
            return memo.get(node);
        }

        // Initialize the path count for this node
        BigInteger pathCount = BigInteger.valueOf(0);

        // Explore all outgoing edges and count the paths from each target node
        for (MDDEdge edge : node.getOutEdges()) {
            pathCount = pathCount.add(countPaths(edge.getTarget(), memo));
        }

        // Store the computed result for this node to avoid recomputation
        memo.put(node, pathCount);

        return pathCount;
    }

    private void traverse(MDDNode node, List<Integer> currentSolution, List<List<Integer>> solutions) {
        // If we reach the sink, add the current solution to the list of solutions
        if (node == sink) {
            solutions.add(new ArrayList<>(currentSolution)); // Add a copy of the current solution
            return;
        }

        // Explore all outgoing edges
        for (MDDEdge edge : node.getOutEdges()) {
            currentSolution.add(edge.getValue()); // Add edge value to the current solution
            traverse(edge.getTarget(), currentSolution, solutions); // Recurse into the next node
            currentSolution.remove(currentSolution.size() - 1); // Backtrack after recursion
        }
    }

    public boolean equals(MDD mdd) {
        if (mdd.numberOfSolutions() != numberOfSolutions()) return false;
        boolean same = true;
        List<List<Integer>> solutions = mdd.getAllSolutions();
        for (List<Integer> s : getAllSolutions()) {
            same &= solutions.contains(s);
        }
        return same;
    }

    public ArrayList<Integer> getArch() {
        ArrayList<Integer> arch = new ArrayList<>();
        for (List<MDDNode> l : layers) {
            arch.add(l.size());
        }
        return arch;
    }


    public void reset() {
        this.layers = new ArrayList<>();
        this.sink = null;
        this.root = null;
    }

}
