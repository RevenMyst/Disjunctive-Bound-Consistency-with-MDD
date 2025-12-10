package org.maxicp.cp.engine.core.mdd.propagation;

import org.glassfish.json.JsonUtil;
import org.maxicp.cp.CPFactory;
import org.maxicp.cp.engine.core.*;
import org.maxicp.cp.engine.core.mdd.MDDEdge;
import org.maxicp.cp.engine.core.mdd.MDDNode;
import org.maxicp.cp.engine.core.mdd.PropertyGetter;
import org.maxicp.cp.engine.core.mdd.properties.MDDBitSet;
import org.maxicp.state.State;
import org.maxicp.state.StateInt;
import org.maxicp.state.datastructures.SmallBitSet;

import java.util.*;
import java.util.stream.Collectors;

import static org.maxicp.cp.CPFactory.*;
import static org.xcsp.common.Types.TypeCtr.mdd;

public class MDDPrecedencesPropagator extends AbstractCPConstraint {
    CPIntVar[] vars;
    CPIntervalVar[] tasks;
    ReversibleMDD revMDD;

    private List<DeltaCPIntVar> deltas = new ArrayList<>();
    private int[] values;


    StateInt[] hashCodes;

    int[] minArr;
    int[] maxArr;
    HashSet<Integer> changedValues = new HashSet<>();
    HashSet<Integer> diff = new HashSet<>();


    StateInt[][] foundPrecedences;
    CPBoolVar[][] precedences;
    private DeltaCPIntVar[][] deltasPred;
    private PropertyGetter<MDDBitSet> someUGetter = null;
    private PropertyGetter<MDDBitSet> someDGetter = null;

    public MDDPrecedencesPropagator(ReversibleMDD mdd, CPIntervalVar[] tasks, CPBoolVar[][] precedences, PropertyGetter<MDDBitSet> someUGetter, PropertyGetter<MDDBitSet> someDGetter) {
        super(tasks[0].getSolver());
        this.revMDD = mdd;
        this.tasks = tasks;
        this.vars = mdd.getMDD().getVariables().toArray(new CPIntVar[0]);
        this.precedences  = precedences;
        minArr = new int[tasks.length];
        maxArr = new int[tasks.length];
        this.someUGetter = someUGetter;
        this.someDGetter = someDGetter;


        this.values = new int[revMDD.mdd.getVariables().stream().mapToInt(CPIntVar::size).max().getAsInt()];
        for(CPIntVar var : revMDD.mdd.getVariables()){
            deltas.add(var.delta(this));
        }


        this.hashCodes = new StateInt[tasks.length];
        for (int i = 0; i < tasks.length; i++) {
            this.hashCodes[i] = getSolver().getStateManager().makeStateInt(computeHash(tasks[i]));
        }

        this.foundPrecedences = new StateInt[tasks.length][tasks.length];
        for(int i = 0; i< tasks.length; i++){
            for(int j = 0; j< tasks.length; j++){
                foundPrecedences[i][j] = getSolver().getStateManager().makeStateInt(0);
            }
        }

        if(precedences != null){
            deltasPred = new DeltaCPIntVar[precedences.length][precedences.length];
            for(int i = 0; i< precedences.length; i++)
                for(int j = i + 1; j< precedences.length; j++)
                    if(precedences[i][j]!=null)
                        deltasPred[i][j] = precedences[i][j].delta(this);
        }

    }

    @Override
    public int priority() {
        return 2;
    }

    private void extractPrecedences(){
        boolean[][] extractedPrecedences = new boolean[tasks.length][tasks.length];
        // init at true
        for(int i = 0; i<tasks.length;i++){
            for(int j = 0; j<tasks.length;j++){
                extractedPrecedences[i][j] = true;
            }
        }
        for(List<MDDNode> layer : revMDD.mdd.getLayers()){
            for(MDDNode node : layer){
                SmallBitSet someU = someUGetter.getProperty(node.getState()).getValue();
                SmallBitSet someD = someDGetter.getProperty(node.getState()).getValue();
                for(int i = 0; i<tasks.length;i++){
                    for(int j = 0; j<tasks.length;j++){
                        extractedPrecedences[i][j] &= (!someU.contains(i) || !someD.contains(j));
                    }
                }
            }
        }
        for(int i = 0; i<tasks.length;i++){
            for(int j = 0; j<tasks.length;j++){
                if(extractedPrecedences[i][j] && i != j){
                    if(foundPrecedences[i][j].value() == 0){
                        foundPrecedences[i][j].setValue(1);
                        //this.getSolver().post(CPFactory.le(end(tasks[i]), start(tasks[j])),false);
                        if(this.precedences[i][j]!=null){
                            precedences[i][j].fix(true);
                        }else{
                            precedences[j][i].fix(false);
                        }

                    }
                }
            }
        }
    }

    public int computeHash(CPIntervalVar task){
        return Objects.hash(task.startMin(),task.startMax(),task.endMin(),task.endMax(),task.lengthMin(),task.lengthMax(),task.isPresent());
    }

    private void MDD4RMarker(){
        for(int i = 0; i<revMDD.mdd.getVariables().size(); i++){

            if(deltas.get(i).changed()){
                diff.clear();
                int nb = deltas.get(i).fillArray(values);
                for(int j =0;j<nb;j++){
                    diff.add(values[j]);
                }
                for(MDDNode n : revMDD.mdd.getLayers().get(i)){
                    for(MDDEdge e : revMDD.getActiveOutEdges(n)){
                        if(diff.contains(e.getValue())){
                            revMDD.remove(e);
                            revMDD.addToDownQueue(e.getTarget());
                            revMDD.addToUpQueue(e.getSource());
                        }
                    }
                }
            }
        }
    }


    private void precedencesMarker(){
        HashSet<Integer> fixed = new HashSet<>();
        for (int i = 0; i < tasks.length; i++) {
            for (int j = i + 1; j < tasks.length; j++) {
                if (deltasPred[i][j].changed()) {
                    fixed.add(i);
                    fixed.add(j);
                }
            }
        }
        for(List<MDDNode> layer : revMDD.getMDD().getLayers()) {
            for (MDDNode n : layer) {
                for (MDDEdge e : revMDD.getActiveOutEdges(n)) {
                    if (fixed.contains(e.getValue())) {
                        if (!e.getSource().getState().arcExist(e.getSource().getState(), e.getTarget().getState(), revMDD.getMDD().getVariables().get(e.getSource().getLayer()), e.getValue())) {
                            revMDD.remove(e);
                            revMDD.addToDownQueue(e.getTarget());
                            revMDD.addToUpQueue(e.getSource());
                        }
                    }
                }
            }
        }

    }

    private void MDDIntervalMarker(){
        changedValues.clear();
        for(int i = 0; i < tasks.length; i++){
            if(hashCodes[i].value() != computeHash(tasks[i])){
                hashCodes[i].setValue(computeHash(tasks[i]));
                //updateAllEdges(i);
                changedValues.add(i);
            }
        }
        if(!changedValues.isEmpty()){
            updateAllEdges(changedValues);
        }
    }

    private void updateAllEdges(HashSet<Integer> values){

        for(List<MDDNode> layer : revMDD.getMDD().getLayers()){
            for(MDDNode n : layer){
                for(MDDEdge e : revMDD.getActiveOutEdges(n)){
                    if(values.contains(e.getValue())){

                        revMDD.addToDownQueue(e.getTarget());
                        revMDD.addToUpQueue(e.getSource());
                        updateEdge(e);
                    }
                }
            }
        }

    }


    public void updateEdge(MDDEdge e){
        if(!e.getSource().getState().arcExist(e.getSource().getState(),e.getTarget().getState(),revMDD.getMDD().getVariables().get(e.getSource().getLayer()),e.getValue())){

            revMDD.remove(e);
        }
    }

    @Override
    public void post() {
        for(CPIntVar v : revMDD.mdd.getVariables()){
            v.propagateOnDomainChange(this);
        }


        for(CPIntervalVar t : tasks){
            t.propagateOnChange(this);
        }

        if(precedences != null){
            for (int i = 0; i < tasks.length; i++) {
                for (int j = i + 1; j < tasks.length; j++) {
                    precedences[i][j].propagateOnFix(this);
                }
            }
        }



        //makespan.propagateOnBoundChange(this);
        // TODO sync MDD with current dommain
        revMDD.extract(true);
        propagate();


    }



    @Override
    public void propagate() {

        setActive(false);
        MDD4RMarker();
        MDDIntervalMarker();
        precedencesMarker();
        revMDD.emptyQueues();
        revMDD.extract(false);
        //timeWindowExtractor();
        extractPrecedences();


        setActive(true);

    }

}
