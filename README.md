# Disjunctive-Bound-Consistency-with-MDD

This repository contains the sources of our paper "Towards Bound Consistency for the No-Overlap Constraint Using MDDs"
[Paper]()

## How to run it

The project contains multiple example files in ``src/main/java/org/maxicp/example``:

- ``BetterPropagationExample.java`` is the running example of the paper.
- ``JITRun.java`` is the main class called by ``runJIT.sh`` to run on all instances
- ``RandomJIT.java`` generates a random instance and solves it.
- ``JITGeneratorjava`` is the script that allowed us to create instances

## Other sources

The implementation is based on the work of Rebecca Gentzel and Laurent Michel : [Haddock](https://github.com/ldmbouge/minicpp), [Paper](https://link.springer.com/chapter/10.1007/978-3-030-58475-7_31)

The solver (used as a dependency) is [MaxiCP](https://github.com/aia-uclouvain/maxicp)

The Precedence extraction method comes from Andre A. Ciré and Willem-Jan van Hoeve : [Paper](https://cdn.aaai.org/ojs/13521/13521-40-17039-1-2-20201228.pdf)
