# README for fade

## Introduction
FADE is name after by forwarding anomaly detection environment.
It is developed based on the Floodlight controller, and designed to detect forwarding anomalies in software-defined networks.
Such forwarding anomalies would forward traffic along wrong paths, and result in security vulnerabilities.


## Directories

```
--fade
   |___ data/    topologies selected for evaluation
   |___ docs/    the preliminary framework of the implementation
   |___ experiment-scripts/  experiment scripts: the test main routine, testcases
   |___ floodlight/ the implementation of fade (net.floodlightcontroller.applications.*)
   |___ floodlight-nofad/ the original floodlight controller, we would run it for comparison purpose
   |___ journal-paper/ the journal paper
   |___ real-data/ the real topologies would be tested
   |___ slides/ the slides for single flow detection mode of fade
   |___ utilities/ the utilities for the experiments. Including data analyzer (analyzer/) and debugging tools (floodlight.logparser)
```
