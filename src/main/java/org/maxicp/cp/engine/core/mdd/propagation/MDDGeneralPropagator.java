package org.maxicp.cp.engine.core.mdd.propagation;

import org.glassfish.json.JsonUtil;
import org.maxicp.cp.engine.core.*;
import org.maxicp.cp.engine.core.mdd.MDDEdge;
import org.maxicp.cp.engine.core.mdd.MDDNode;
import org.maxicp.cp.engine.core.mdd.PropertyGetter;
import org.maxicp.cp.engine.core.mdd.constraints.DisjunctiveBitSetMDD;
import org.maxicp.cp.engine.core.mdd.properties.MDDInt;
import org.maxicp.state.StateInt;

import java.util.*;
import java.util.stream.Collectors;

import static org.xcsp.common.Types.TypeCtr.mdd;

public class MDDGeneralPropagator extends AbstractCPConstraint {
    CPIntVar[] vars;
    CPIntervalVar[] tasks;
    ReversibleMDD revMDD;
    CPIntVar makespan;
    StateInt previousMakespan;

    private List<DeltaCPIntVar> deltas = new ArrayList<>();
    private int[] values;


    StateInt[] hashCodes;
    public static long counter = 0;

    int[] minArr;
    int[] maxArr;
    HashSet<Integer> changedValues = new HashSet<>();
    HashSet<Integer> diff = new HashSet<>();

    PropertyGetter<MDDInt> earliestGetter = null;
    PropertyGetter<MDDInt> latestGetter = null;

    public MDDGeneralPropagator(ReversibleMDD mdd, CPIntervalVar[] tasks) {
        super(tasks[0].getSolver());
        this.revMDD = mdd;
        this.tasks = tasks;
        this.vars = mdd.getMDD().getVariables().toArray(new CPIntVar[0]);
        //this.makespan = makespan;
        //this.previousMakespan = getSolver().getStateManager().makeStateInt(makespan.max());
        minArr = new int[tasks.length];
        maxArr = new int[tasks.length];


        this.values = new int[revMDD.mdd.getVariables().stream().mapToInt(CPIntVar::size).max().getAsInt()];
        for(CPIntVar var : revMDD.mdd.getVariables()){
            deltas.add(var.delta(this));
        }


        this.hashCodes = new StateInt[tasks.length];
        for (int i = 0; i < tasks.length; i++) {
            this.hashCodes[i] = getSolver().getStateManager().makeStateInt(computeHash(tasks[i]));
        }

    }

    @Override
    public int priority() {
        return 2;
    }

    public void setPropertyGetters(PropertyGetter<MDDInt> earliestGetter, PropertyGetter<MDDInt> latestGetter){
        this.earliestGetter = earliestGetter;
        this.latestGetter = latestGetter;
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

        //makespan.propagateOnBoundChange(this);
        // TODO sync MDD with current dommain
        revMDD.extract(true);
        propagate();


    }

    private void timeWindowExtractor(){

// initialize
        Arrays.fill(minArr, Integer.MAX_VALUE);
        Arrays.fill(maxArr, Integer.MIN_VALUE);
        for(List<MDDNode> layer : revMDD.mdd.getLayers()){
            for(MDDNode n : layer){

                int earliest = 0;
                if(earliestGetter != null){
                    counter++;
                    earliest = earliestGetter.getProperty(n.getState()).getValue();
                }else {
                    earliest = n.getState().getExposedValues().get("earliest");
                }
                for(MDDEdge e : revMDD.getActiveOutEdges(n)){
                    minArr[e.getValue()] =  Math.min(earliest,minArr[e.getValue()]);
                    int latest = 0;
                    if(latestGetter != null){
                        latest = latestGetter.getProperty(e.getTarget().getState()).getValue();
                    }
                    else {
                        latest = e.getTarget().getState().getExposedValues().get("latest");
                    }
                    maxArr[e.getValue()] =  Math.max(latest,maxArr[e.getValue()]);
                }
            }
        }

        for(int i = 0; i< tasks.length; i++){
            //System.out.println("Task " + i + " " + minArr[i] + " - " + maxArr[i]);
            tasks[i].setStartMin(Math.max(tasks[i].startMin(), minArr[i]));
            tasks[i].setEndMax(Math.min(tasks[i].endMax(), maxArr[i]));
        }



    }

    @Override
    public void propagate() {

            setActive(false);
            MDD4RMarker();
            MDDIntervalMarker();
            revMDD.emptyQueues();
            revMDD.extract(false);
            timeWindowExtractor();
            setActive(true);
        //System.out.println("extraction "+counter+" transition "+((DisjunctiveBitSetMDD)(revMDD.mdd.getSpecs().get(0))).counter);

    }

        }
