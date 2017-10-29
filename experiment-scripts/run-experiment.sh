#!/bin/bash

scriptname=$0

function usage
{
	cat <<EOF
Usage: $scriptname [-a] [-t] [-?]
    -a run all experiments
    -t run throughput test experiment
    -p run packetin throughput test with cbench
    -l run sensitivity test with linear topology
    -L run sensitivity test with linear topology (AggregatedFlow)
    -g run sensitivity test with linear topology (no fade)
    -d run test with different length of linear topology (singleflow)
    -D run test with different length of linear topology (aggregatedflow)
    -c run test with different detection duration (duration of collecting flow statistics) (SingleFlow)
    -C run test with different detection duration (AggregatedFlow)
    -s run sensitivity test with Simulated rules (SingleFlow)
    -S run sensitivity test with Simulated rules (AggregatedFlow)
    -I run sensitivity test with Internet2 rules (AggregatedFlow)
    -B run basic aggregated flow test
    -? show this message
EOF
}

function require_root
{
    if [[ $EUID -ne 0 ]]; then
		echo "This script must be run as root" 1>&2
		exit 1
    fi
}

FADE_CONTROLLER_PATH="../floodlight/floodlight.sh"
COMMON_CONTROLLER_PATH="../floodlight-nofad/floodlight.sh"
FADE_DIR="../floodlight"
COMMON_CONTROLLER_DIR="../floodlight-nofad/"
FADE_CONFIG_PATH="../floodlight/src/main/resources/floodlightdefault.properties"
compile_status=""
DEFAULT_FADE_MODE="SingleFlow"
function clean_up_compile
{
	if [ ! "${compile_status} " == " " ]; then
		sed -i "s/detectionMode=.*/detectionMode=${DEFAULT_FADE_MODE}/g" ${FADE_CONFIG_PATH}
		echo reset detection mode to ${DEFAULT_FADE_MODE}, please rebuild it manually.
	fi
}

function compile_FADE
{
	current_detection_mode=$(cat ${FADE_CONFIG_PATH} | grep 'detectionMode=' | cut -d '=' -f2)
	target_detection_mode=""
	force=$2
	echo FADE is in ${current_detection_mode} mode
	if [ "$1" == "SingleFlow" -o "$1" == "AggregatedFlow" ]; then
		echo compile FADE into $1 mode
		target_detection_mode=$1
	elif [ ! "$1 " == " " ]; then
		echo cannot build FADE to detection mode $1
		exit 1
	else
		echo skip build FADE
		return
	fi
	if [ ${current_detection_mode} == ${target_detection_mode} -a "${force}" == "" ]; then
		echo FADE already in ${target_detection_mode}, skip building
		return
	fi
	
	# rebuild fade
	compile_status="compiling"
	trap clean_up_compile SIGHUP SIGINT SIGTERM
	sed -i "s/detectionMode=.*/detectionMode=${target_detection_mode}/g" ${FADE_CONFIG_PATH}
	cd ${FADE_DIR}
	mvn package -DskipTests
	mvn_status=$?
	if [ ${mvn_status} -ne 0 ]; then
		echo build FADE fail, please check your source code
		exit 1
	fi
	cd -
	echo rebuild FADE into ${target_detection_mode} successfully.
	compile_status=""
	trap - SIGHUP SIGINT SIGTERM
}

function set_detect_duration
{
	val=$1
	if [ ! $val -gt 0 ]; then echo detection duration must great than 0; fi
	sed -i "s/detectionDuration=.*/detectionDuration=${val}/g" ${FADE_CONFIG_PATH}
}

function run_throughput_test
{
	max_run=3
	max_topo_len=15
	compile_FADE "SingleFlow"
	echo run throughput test...
	for run in $(seq 1 ${max_run})
	do
		for len in $(seq 3 ${max_topo_len})
		do
			log_dir="../experiment-data/running/throughput/fad/run${run}/log${len}"
			if [ ! -d ${log_dir} ]; then mkdir -p ${log_dir}; fi
			python test.py -c auto -p ${FADE_CONTROLLER_PATH} -t nodes=${len} -o ${log_dir} linear
			# without fade
			log_dir="../experiment-data/running/throughput/nofad/run${run}/log${len}"
			if [ ! -d ${log_dir} ]; then mkdir -p ${log_dir}; fi
			python test.py -c auto -p ${COMMON_CONTROLLER_PATH} -t nodes=${len} -o ${log_dir} linear
			# with fade
		done 
	done
	echo throughput test DONE ^_^
}

function run_raw_test_with_packetin_cbench
{
	if [ ! $# -eq 1 ]; then exit 1; fi
	mode=$1
	max_run=10
	cmd=""
	log_dir="../experiment-data/running/pktin/"
	if [ $mode == SingleFlow ]; then
		compile_FADE "SingleFlow"
		wrk=${FADE_DIR}
		cmd=${FADE_CONTROLLER_PATH}
		log_dir="${log_dir}/SingleFlow/"
	elif [ $mode == AggregatedFlow ]; then
		compile_FADE "AggregatedFlow"
		wrk=${FADE_DIR}
		cmd=${FADE_CONTROLLER_PATH}
		log_dir="${log_dir}/AggregatedFlow/"
	elif [ $mode == Common ]; then
		wrk=${COMMON_CONTROLLER_DIR}
		cmd=${COMMON_CONTROLLER_PATH}
		log_dir="${log_dir}/Common/"
	else
		echo "unrecongnized paramter"
		exit 1
	fi
	for run in $(seq 1 ${max_run}); do
		final_log_dir="${log_dir}/run${run}/"
		if [ ! -d ${final_log_dir} ]; then mkdir -p ${final_log_dir}; fi
		cd $wrk
		bash $cmd &
		cd -
		sleep 10
		cbench -c 127.0.0.1 -p 6653 -w 3 -C 3 -l 30 -t > ${final_log_dir}/cbench.log
		pkill -f floodlight.jar
		sleep 3
	done
}

function run_test_with_packetin_cbench
{
	run_raw_test_with_packetin_cbench SingleFlow
	run_raw_test_with_packetin_cbench AggregatedFlow
	run_raw_test_with_packetin_cbench Common
}

function run_sensitivity_test_with_linear_topo
{
	max_run=10
	num_of_nodes=(5 5 5 5 5 5 5 5 5 5 5 5)
	num_of_hosts=(40 50 60 70 80 90 100 120 140 160 180 200)
	num_of_injects=(20 20 20 20 30 30 40 40 50 60 70 80)
	compile_FADE "SingleFlow"
	echo run sensitivity test with linear topo
	length=${#num_of_hosts[@]}
	for run in $(seq 1 ${max_run}); do
		for idx in $(seq 0 $((length-1))); do
			nodes=${num_of_nodes[$idx]}
			hosts=${num_of_hosts[$idx]}
			injects=${num_of_injects[$idx]}
			log_dir="../experiment-data/running/sensitivity@linear/node${nodes}_hosts${hosts}_injects${injects}/run${run}/"
			if [ ! -d ${log_dir} ]; then mkdir -p ${log_dir}; fi
			echo nodes=${nodes}, hosts=${hosts}, injects=${injects}
			python test.py -c auto -p ${FADE_CONTROLLER_PATH} -t nodes=${nodes} fake_hosts=$((hosts-1)) num_of_injects=${injects} duration=300 -o ${log_dir} linear
		done
	done
}

function run_sensitivity_test_with_linear_topo_aggregated_flow
{
	max_run=10
	num_of_nodes=(5 5 5 5 5 5 5 5 5 5 5 5)
	num_of_hosts=(40 50 60 70 80 90 100 120 140 160 180 200)
	num_of_injects=(20 20 20 20 30 30 40 40 50 60 70 80)
	compile_FADE "AggregatedFlow"
	echo run sensitivity test with linear topo
	length=${#num_of_hosts[@]}
	for run in $(seq 1 ${max_run}); do
		for idx in $(seq 0 $((length-1))); do
			nodes=${num_of_nodes[$idx]}
			hosts=${num_of_hosts[$idx]}
			injects=${num_of_injects[$idx]}
			log_dir="../experiment-data/running/sensitivity@linear-aggregated/node${nodes}_hosts${hosts}_injects${injects}/run${run}/"
			if [ ! -d ${log_dir} ]; then mkdir -p ${log_dir}; fi
			echo nodes=${nodes}, hosts=${hosts}, injects=${injects}
			python test.py -c auto -p ${FADE_CONTROLLER_PATH} -t nodes=${nodes} fake_hosts=$((hosts-1)) num_of_injects=${injects} duration=300 -o ${log_dir} linear
		done
	done
}

function run_sensitivity_test_with_linear_topo_common
{
	max_run=10
	num_of_nodes=(5 5 5 5 5 5 5 5 5 5 5 5)
	num_of_hosts=(40 50 60 70 80 90 100 120 140 160 180 200)
	num_of_injects=(20 20 20 20 30 30 40 40 50 60 70 80)
	echo run sensitivity test with floodlight-nofad
	length=${#num_of_hosts[@]}
	for run in $(seq 1 ${max_run}); do
		for idx in $(seq 0 $((length-1))); do
			nodes=${num_of_nodes[$idx]}
			hosts=${num_of_hosts[$idx]}
			injects=${num_of_injects[$idx]}
			log_dir="../experiment-data/running/sensitivity@linear-common/node${nodes}_hosts${hosts}_injects${injects}/run${run}/"
			if [ ! -d ${log_dir} ]; then mkdir -p ${log_dir}; fi
			echo nodes=${nodes}, hosts=${hosts}, injects=${injects}
			python test.py -c auto -p ${COMMON_CONTROLLER_PATH} -t nodes=${nodes} fake_hosts=$((hosts-1)) num_of_injects=${injects} duration=60 -o ${log_dir} linear
		done
	done
}

function run_test_with_different_topo_len
{
	max_run=10
	num_of_nodes="5 7 9 11 13 15"
	hosts=100
	injects=30
	compile_FADE "SingleFlow"
	echo "run test with different length of linear topo (SingleFlow)"
	for run in $(seq 1 ${max_run}); do
		for len in ${num_of_nodes}; do
			nodes=$len
			log_dir="../experiment-data/running/topo/node${nodes}_hosts${hosts}_injects${injects}/run${run}/"
			if [ ! -d ${log_dir} ]; then mkdir -p ${log_dir}; fi
			echo nodes=${nodes}, hosts=${hosts}, injects=${injects}
			python test.py -c auto -p ${FADE_CONTROLLER_PATH} -t nodes=${nodes} fake_hosts=$((hosts-1)) num_of_injects=${injects} duration=300 -o ${log_dir} linear
		done
	done
	echo "test with different lengh of linear topo (SingleFlow) DONE ^_^"
}

function run_test_with_different_topo_len_aggregated
{
	max_run=10
	num_of_nodes="5 7 9 11 13 15"
	hosts=100
	injects=30
	compile_FADE "AggregatedFlow"
	echo "run sensitivity test with linear topo (Aggregated Version)"
	for run in $(seq 1 ${max_run}); do
		for len in ${num_of_nodes}; do
			nodes=$len
			log_dir="../experiment-data/running/topo-aggregated/node${nodes}_hosts${hosts}_injects${injects}/run${run}/"
			if [ ! -d ${log_dir} ]; then mkdir -p ${log_dir}; fi
			echo nodes=${nodes}, hosts=${hosts}, injects=${injects}
			python test.py -c auto -p ${FADE_CONTROLLER_PATH} -t nodes=${nodes} fake_hosts=$((hosts-1)) num_of_injects=${injects} duration=300 -o ${log_dir} linear
		done
	done
	echo "test with different lengh of linear topo (Aggregated Version) DONE ^_^"
}

function run_test_with_different_collecting
{
	mode=SingleFlow
	if [ $# -gt 0 ]; then mode=$1; fi
	compile_FADE $mode 1
	max_run=10
	duration="1 2 4 8"
	nodes=5
	hosts=100
	injects=30
	echo "run test with different collecting (${mode})"
	for run in $(seq 1 ${max_run}); do
		for dur in ${duration}; do
			log_dir="../experiment-data/running/collecting-${mode}/duration-${dur}/run${run}/"
			set_detect_duration $dur
			if [ ! -d ${log_dir} ]; then mkdir -p ${log_dir}; fi
			python test.py -c auto -p ${FADE_CONTROLLER_PATH} -t nodes=${nodes} fake_hosts=$((hosts-1)) num_of_injects=${injects} duration=300 -o ${log_dir} linear
		done
	done
	echo "test with different collection (${mode}) DONE ^_^"
}

function run_test_with_different_collecting_aggregated
{
	run_test_with_different_collecting AggregatedFlow
}


function run_sensitivity_test_with_simulated_rules
{
	max_run=10
	topo_dir="../data/selected-topo/"
	compile_FADE "SingleFlow"
	echo run sensitivity test with simulated flow rules...
	for run in $(seq 1 ${max_run}); do
		for topo in $(ls ${topo_dir}*.gml.dat); do
			name=${topo:${#topo_dir}:-8}
			log_dir="../experiment-data/running/sensitivity@simulated/output${run}/${name}/log1/"
			if [ ! -d ${log_dir} ]; then mkdir -p ${log_dir}; fi
			python test.py -c auto -p ${FADE_CONTROLLER_PATH} -t topo_file=${topo} duration=300 -o ${log_dir} general
		done
	done
	echo sensitivity test with simulated flow rules DONE ^_^
}

function run_sensitivity_test_with_simulated_rules_aggregated
{
	max_run=10
	topo_dir="../data/selected-topo/"
	compile_FADE "AggregatedFlow"
	echo run sensitivity test with simulated flow rules...
	for run in $(seq 1 ${max_run}); do
		for topo in $(ls ${topo_dir}*.gml.dat); do
			name=${topo:${#topo_dir}:-8}
			log_dir="../experiment-data/running/sensitivity@simulated-aggregated/output${run}/${name}/log1/"
			if [ ! -d ${log_dir} ]; then mkdir -p ${log_dir}; fi
			python test.py -c auto -p ${FADE_CONTROLLER_PATH} -t topo_file=${topo} duration=300 -o ${log_dir} general
		done
	done
	echo sensitivity test with simulated flow rules DONE ^_^
}

function run_sensitivity_test_with_internet2_rules
{
	max_run=10
	topo_file="../data/internet2/internet2.gml.dat"
	host_file="../data/internet2/internet2_host_conf.json"
	rule_file="../data/internet2/internet2_rules.dat"
	compile_FADE "AggregatedFlow"
	echo run sensitivity test with Internet2 flow rules...
	for run in $(seq 1 ${max_run}); do
		log_dir="../experiment-data/running/sensitivity@internet2/output${run}/${name}/log1/"
		if [ ! -d ${log_dir} ]; then mkdir -p ${log_dir}; fi
		python test.py -c auto -p ${FADE_CONTROLLER_PATH} \
			   -t topo_file=${topo_file} host_file=${host_file} duration=900 rule_file=${rule_file} \
			   -o ${log_dir} generalaggregated
	done
	echo sensitivity test with Internet2 flow rules DONE ^_^
}

function run_basic_aggregated_flow_test
{
	max_run=3
	compile_FADE "AggregatedFlow"
	echo run basic aggregated flow test
	for run in $(seq 1 ${max_run}); do
		log_dir="../experiment-data/running/basic_aggregated_flow/output${run}/"
		if [ ! -d ${log_dir} ]; then mkdir -p ${log_dir}; fi
		python test.py -c auto -p ${FADE_CONTROLLER_PATH} -o ${log_dir} aggregated
	done
}

function all
{
	run_throughput_test
	run_test_with_packetin_cbench
	run_sensitivity_test_with_linear_topo
	run_sensitivity_test_with_linear_topo_aggregated_flow
	run_sensitivity_test_with_linear_topo_common
	run_test_with_different_topo_len
	run_test_with_different_topo_len_aggregated
	run_test_with_different_collecting
	run_test_with_different_collecting_aggregated
	run_sensitivity_test_with_simulated_rules
	run_sensitivity_test_with_internet2_rules
	run_basic_aggregated_flow_test
}


# main entry
require_root;
if [ $# -eq 0 ]
then
	usage
else
    while getopts "atplLgdDcCsSIB?" OPTION
    do
		case $OPTION in
			a)    all;;
			t)    run_throughput_test;;
			p)    run_test_with_packetin_cbench;;
			l)    run_sensitivity_test_with_linear_topo;;
			L)    run_sensitivity_test_with_linear_topo_aggregated_flow;;
			g)    run_sensitivity_test_with_linear_topo_common;;
			d)    run_test_with_different_topo_len;;
			D)    run_test_with_different_topo_len_aggregated;;
			c)    run_test_with_different_collecting;;
			C)    run_test_with_different_collecting_aggregated;;
			s)    run_sensitivity_test_with_simulated_rules;;
			S)    run_sensitivity_test_with_simulated_rules_aggregated;;
			I)    run_sensitivity_test_with_internet2_rules_aggregated;;
			B)    run_basic_aggregated_flow_test;;
			?)    usage;;
			*)    usage;;
		esac
    done
    shift $(($OPTIND - 1))
fi

