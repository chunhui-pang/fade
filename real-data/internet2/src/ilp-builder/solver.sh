#!/bin/bash
# generate experiment data for paper (see data/ilp/*.dat)
# please setup gurobi environment ahead of time
function gen_greedy_fill_ingress_sol
{
	output="greedy-fill-ingress.dat"
	printf '%10s%10s%10s\n' "TCAM" "Run" "Usage" > $output
	for tcam in $(seq 1 50); do
		for run in $(seq 1 200); do
			./ilp-builder -t $((tcam*100)) -r $run -m flow-assign.lp -s greedy_fill_ingress.sol output > /dev/null 2>&1
			if [ $? -eq 0 ]; then
				max_usage=$(cat greedy_fill_ingress.sol | grep -oP 'Max\Wtcam\Wusage.*?\K\d+')
				printf '%10d%10d%10d\n' $((tcam*100)) $run $max_usage >> $output
				break
			fi
		done
	done
}

function gen_guribo_sol
{
	# please setup gurobi environment firstly
	output="gurobi.dat"
	printf '%10s%10s%10s\n' "TCAM" "Run" "Usage" > $output
	for tcam in $(seq 1 50); do
		for run in $(seq 1 200); do
			./ilp-builder -t $((tcam*100)) -r $run -m flow-assign.lp -s greedy_fill_ingress.sol output > /dev/null 2>&1
			gurobi_cl ResultFile=flow-assign.sol flow-assign.lp > /dev/null 2>&1
			if [ -s flow-assign.sol ]; then
				max_usage=$(head -n1 flow-assign.sol | grep -oP '.*?\K\d+.*')
				printf '%10d%10d%10d\n' $((tcam*100)) $run $max_usage >> $output
				break
			fi
		done
	done
}

gen_guribo_sol
gen_greedy_fill_ingress_sol

