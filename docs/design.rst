======================
Design of FAD
======================

Overall Architecture
---------------------------

::
   FlowSelector  --------->   RuleGenerator
                                |       |
     RuleEnforcer <-- rules <---+       +-----> StatsConstraints (JEXL) ----> Analyzer

	                      Detector ----- StatsCollector

						      Flow  FlowIdManager

Question
---------------------------
1. How to express various of flows (single flow, tree-based flow, aggregated MPLS-like flows)
2. How to check whether if a detection result of a flow should dropped::
	 We can use another data structure to record which flows are changed in their detection
3. How to implement cookie such that every node of every flow have unique id::
	 cookie = assigned_flow_id + datapath_id
4. How to solve the verification error in ``StatsConstrains``? Actually, we should do fault localization in this scenario.::
	 providing subflow procedure in Flow class or get_failure_part() procedure in StatsConstrains class (the latter is prefered)
   
Interface Design
---------------------------

   
   
