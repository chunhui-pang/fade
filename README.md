# README for fade

## Introduction
FADE is name after by forwarding anomaly detection environment.
It is developed based on the Floodlight controller, and designed to detect forwarding anomalies in software-defined networks.
Such forwarding anomalies would forward traffic along wrong paths, and result in security vulnerabilities.

## Detection mechanism
1. Build a rule graph which reflect how traffic go through flow rules
2. Identify every flows in the rule graph, and select part of flows to detect
3. Select several flow rules as probes for every selected flow
4. Generate several flow rules (dedicated flow rules) with higher priority and some hard timeout for each probe, and then install them to the open vSwitches.
5. Generate flow statistics constraints for these dedicated flows, these constraints would be evaluated after the flow statistics of these dedicated flows received.
6. Some suspicious sub-flows would be identified if any constraint fails..
7. If the suspicious sub-flows is short enough, we could identify the abnormal flow rules. Otherwise, we would start another similar detection task (localization task)

```
----------      -------------      ----------------       -----------------       ----------------        ---------------------         ---------------------        ---------------------       ----------------------      ----------
|Topology | --> | rule graph | --> | flow selection | --> | probe selection | --> | rule generation | --> | constraint generator | --> | statistics collector | --> | constraint evaluator | --> | suspicious sub-flow | --> | anomaly |
----------      -------------      ----------------       -----------------       -----------------       ----------------------       -----------------------       ----------------------      ----------------------      ----------
                                                                 ^                                                                                                                                       ^
                                                                 |_______________________________________________________________________________________________________________________________________|
```


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

## Run Experiments

1. Dependencies: mininet, open vSwitch, maven, cmake
2. Build binaries:

   1. go to floodlight and floodlight-nofad directory, run ```mvn package -DskipTests=true``` (ocasionally, there are problems)
   2. go to utilities/analyzerrs directory, build analyzer for fade (```mkdir build; cd build; cmake ../; make```)
   3. go to utilities/floodlight.logparser, build debug tools for fade (```mkdir build; cd build; cmake ../; make```)
   
3. Run experiments with the scripts in experiment-scripts directory. use ```./run-experiments -?``` to see the help. After the experiments, all log data are stored in experiment-data/ directory.
4. After experiments finishes, you could use analyzers in utilities directory to show the result. The results are shown with text outputs. However, the formats are designed for latex and it could be easily to use tikz to draw pictures from these data.
5. If there are any problems, the logparser in the utilities directory may be helpful.

## Utilities
1. The analyzers

    1. accuracy_analyzer: show the results of detection accuracy: true positive and true negative are included.
    2. dedicated_rule_uage_analyzer: show the result of dedicated rule usage at different time.
    3. delay_analyzer: show the delay of maintaining rule graph.
    4. pktin_throughput_analyzer: show the packet in throughput of the controll plane.
    5. throughput_analyzer: show the throughput of data plane.

2. logparser
    The log parser is designed as a framework. It's default behaviour is no operation.
    All authentic parsers are designed as plugins (dynamic share library) which could be loaded in to the framework.

     1. topology_parser: dump network topology when any related event happens
     2. rule_link_parser： dump dependency links between flow rules. The dependency links between rules form the rule graph
     3. single_flow_parser: dump selected  flows for detecting (single flow mode)
     4. aggregated_flow_parser: dump selected flows for detecting (aggregated flow mode)
     5. single_flow_probe_parser: show selected probes of every detection run (single flow mode)
     6. aggregated_flow_probe_parser: show selected probles of every detection run (aggregated flow mode)
     7. constraint_parser: dump generated constraint among the flow statistics that should be collected by dedicated flow rules
     8. localization_parser: show flow localization messages
     9. anomaly_parser: show detected anomalies.




