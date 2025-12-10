# compile the project and run the unit tests
# run from the root of the project
echo "compiling and running tests..."
mvn clean package -q
echo "compilation done and tests performed"
# path of the executable
launch_solver="java --enable-preview -cp target/maxicp-0.0.1.jar:target/maxicp-0.0.1-jar-with-dependencies.jar org.maxicp.example.JITRun"
currentDate=$(date +%Y-%m-%d_%H-%M-%S);  #
gitShortHash=$(git rev-parse --short HEAD)
outFileOpt="results/jit/jit-${currentDate}"  # filename for the results (with the date at the end of the filename)

# configurations that must be tested, for each input
mkdir -p "results/jit"  # where the results will be written
rm -f $outFileOpt  # delete filename of the results if it already existed (does not delete past results, unless their datetime is the same)

# the solver must print only one line when it is finished, otherwise we won't get a CSV at the end
# this is the header of the csv. This header needs to change depending on the solver / type of experiment that is being run
# all rows need to be printed by the solver itself
# the id of the commit can be retrieved with the command `git rev-parse HEAD`
echo "filename,n,mddSize,nbNodesBase,timeBase,nbNodesTW,timeTW,nbNodesPred,timePred" >> $outFileOpt
#echo "instance | choicesBase | choicesMDD | choicesMDDNoCost | timeBase | timeMDD | timeMDDNoBuild | timeMDD2 | timeMDDNoBuild2" >> $outFileOpt
timeout=60  # timeout in seconds
iter=1  # number of iterations, to take randomness into account (run each instance iter times with a given configuration)
mddSize=(8 16 32)
echo "writing inputs"
# write all the configs into a temporary file
inputFile="inputFileSM"

rm -f $inputFile  # delete previous temporary file if it existed
for (( i=1; i<=iter; i++ )); do
    for ms in "${mddSize[@]}"; do
      find data/JIT/run/ -type f | sed "s/$/,$ms/"  >> $inputFile
    done
done


echo "launching experiments in parallel"

# the command line arguments for launching the solver. In this case, the solver is run using
# ./executable -f instance_filename -t timeout -rt relaxationType -d decisionType -s seed
# change this depending on your solver
# the number ({1}, {2}) corresponds to the columns present in the inputFile, beginning at index 1 (i.e. in this case 4 columns, so 1, 2, 3 and 4 are valid columns)
cat $inputFile | parallel -j 2 --colsep ',' $launch_solver -f {1} -t $timeout -s {2} >> $outFileOpt
# delete the temporary file
echo "experiments have been run"
rm -f $inputFile