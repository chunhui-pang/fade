/**
 * utility to parse floodlight and mininet log to analyze the sensitivity (TPR) and specifity (TNR) of the detection.
 * We collect the time of every successful detection and failed detection, and output them as two-dimentional data.
 * The x-axis is time, and the y-axis is the TPR/TNR.
 * As time going, the TPR/TNR varies, and we sample the data every "tick", and output it as the final data.
 * If there are multiple logs, we average the TPR/TNR at every "tick", and output the average data.
 *
 * options are:
 *    -t   the time unit of output (default 1000)
 *    -r   output raw data?        (default No)
 *    -d   log directories (or just as positional arguments)
 */
#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <cstdlib>
#include <iomanip>
#include <algorithm>
#include <climits>
#include <regex>
#include <map>
#include <set>

#include <unistd.h>

#include <common/util.h>

using namespace std;

struct analyze_options
{
	/* directories that contains the logs */
	vector<string> log_dirs;
	/* the time unit of output (in ms) */
	long output_tick;
	/* output raw data */
	string output_raw;
	analyze_options(long output_tick=1000L, const string& output_raw = "")
		: log_dirs(), output_tick(output_tick), output_raw(output_raw)
		{
		}
};

struct accuracy_t
{
	long tick;  /* or time */
	double tpr;
	double tnr;
	accuracy_t(long tick, double tpr, double tnr)
		: tick(tick), tpr(tpr), tnr(tnr)
		{
		}
};

typedef vector< accuracy_t > accuracy_array_t;
typedef pair<int, unsigned> inject_point_t;  /* dpid, ip */
typedef set< inject_point_t > path_t; /* path of flow */
typedef long long llong;

bool parse_analyze_options(int argc, char *argv[], analyze_options& analyze_opts);
bool construct_accuracy_from_directory(const std::string dir, accuracy_array_t& accuracy);
bool parse_mininet_log(const std::string& file, map< inject_point_t, llong >& injects);
bool parse_floodlight_log(const std::string& file, const map< inject_point_t, llong >& injects, accuracy_array_t& accuracy);
void output_raw_data(const vector< accuracy_array_t >& accuracy, std::ostream& os);
void output_data_by_tick(const vector< accuracy_array_t >& accuracy, long tick, std::ostream& os);

int main(int argc, char *argv[])
{
    analyze_options analyze_opts;
	if(!parse_analyze_options(argc, argv, analyze_opts))
	{
		return 1;
	}
	vector< accuracy_array_t > accuracy;
	for(vector<string>::iterator it = analyze_opts.log_dirs.begin(); it != analyze_opts.log_dirs.end(); it++)
	{
		accuracy_array_t accuracy_array;
		accuracy.push_back(accuracy_array);
		if(!construct_accuracy_from_directory(*it, accuracy.back()))
			return 1;
	}
	if(analyze_opts.output_raw.size() != 0)
	{
		ofstream os(analyze_opts.output_raw);
		output_raw_data(accuracy, os);
	}
	output_data_by_tick(accuracy, analyze_opts.output_tick, std::cout);
    return 0;
}

bool construct_accuracy_from_directory(const std::string dir, accuracy_array_t& accuracy_array)
{
	map< inject_point_t, llong> injects;
	std::string mininet_log = get_mininet_log(dir);
	if(!parse_mininet_log(mininet_log, injects))
		return false;

	std::string floodlight_log = get_floodlight_log(dir);
	if(!parse_floodlight_log(floodlight_log, injects, accuracy_array))
		return false;
	return true;
}

bool parse_mininet_log(const std::string& file, map< inject_point_t, llong >& injects)
{
	const static regex INJECT_COMMAND("(\\d+-\\d+-\\d+\\W+\\d+:\\d+:\\d+),(\\d+).*add-flow\\Ws(\\d+)\\W.*nw_dst=([\\d.]+).*");
	ifstream mininet_log(file);
	std::string line;
	std::smatch sm;
	while (getline(mininet_log, line))
	{
		if(std::regex_match(line, sm, INJECT_COMMAND))
		{
			int dpid = std::atoi(sm[3].str().c_str());
			unsigned ip = string_to_ip(sm[4].str());
			if(0 == ip)
				return false;
			std::pair<int, unsigned> inject_key = make_pair(dpid, ip);
			llong time_us = string_to_time_with_us(sm[1].str(), sm[2].str());
			injects.insert(make_pair(inject_key, time_us));
		}
	}
	if(0 == injects.size())
	{
		std::cerr << "cannot find injected anomaly in file '" << file << "'" << std::endl;
		return false;
	}
	std::clog << "Mininet log parse finished" << std::endl;
	return true;
}

bool parse_floodlight_log(const std::string& file, const map< inject_point_t, llong >& injects, accuracy_array_t& accuracy)
{
	const static regex ANOMALY_FOUND_MSG("(\\d+-\\d+-\\d+\\W+\\d+:\\d+:\\d+),(\\d+).*find\\Wanomaly\\Wof\\Wflow.*datapathId=([\\d:]+).*ipv4_dst=([\\d.]+).*");
	const static regex DETECTION_START_MSG("(\\d+-\\d+-\\d+\\W+\\d+:\\d+:\\d+),(\\d+).*start\\Wnew\\Wdetection\\Wrun,\\Wrun\\Wid\\W=\\W1");
	const static regex FLOW_RULE_NODE(".*?FlowRuleNode\\{datapathId=([\\d:]+).*?ipv4_dst=([\\d.]+)(.*)");
	const static string FLOW_RULE_NODE_PREFIX = "FlowRuleNode{";
	const static regex NEGATIVE_MSG("(\\d+-\\d+-\\d+\\W+\\d+:\\d+:\\d+),(\\d+).*constraint.*flowId=(\\d+).*?(FlowRuleNode.*)constraints.*was\\Wevaluated,\\W.*result\\Wis\\Wtrue");

	llong detection_start_time = 0;
	ifstream floodlight_log(file);
	map<long, path_t > flow_id_to_path;
	set< inject_point_t > uniq_tp;
	long tp = 0, tn = 0, fp = 0, fn = 0, p = injects.size();
	string line;
	std::smatch sm;
	long counter = 0;
	std::clog << "Floodlight log: ";
	std::clog.flush();
	while (getline(floodlight_log, line))
	{
		counter++;
		if(counter % 10000 == 0)
		{
			std::clog << counter/10000 << "W ";
			std::clog.flush();
		}
		llong time_us = 0;
		if(std::regex_match(line, sm, ANOMALY_FOUND_MSG))
		{
			int dpid = string_to_dpid(sm[3].str());
			if(0 == dpid)
				return false;
			unsigned ip = string_to_ip(sm[4].str());
			if(0 == ip)
				return false;
			time_us = string_to_time_with_us(sm[1].str(), sm[2].str());
			// positive
			inject_point_t inject = std::make_pair(dpid, ip);
			if(injects.count(inject) == 0)
				fp++;
			else
			{
				uniq_tp.insert(inject);
				tp++;
			}
		}
		else if(std::regex_match(line, sm, NEGATIVE_MSG))
		{
			long flow_id = std::atoi(sm[3].str().c_str());
			time_us = string_to_time_with_us(sm[1].str(), sm[2].str());
			path_t path;
			std::string path_msg = sm[4].str();
			int _1st_pos = path_msg.find(FLOW_RULE_NODE_PREFIX);
			while(_1st_pos != string::npos && _1st_pos != path_msg.size())
			{
				int nxt_pos = path_msg.find(FLOW_RULE_NODE_PREFIX, _1st_pos+1);
				if(nxt_pos == string::npos)
					nxt_pos = path_msg.size();
				std::string rule_str = path_msg.substr(_1st_pos, nxt_pos - _1st_pos);
				_1st_pos = nxt_pos;
				if(std::regex_match(rule_str, sm, FLOW_RULE_NODE))
				{
					int dpid = string_to_dpid(sm[1].str());
					if(0 == dpid)
						return false;
					unsigned ip = string_to_ip(sm[2].str());
					if(0 == ip)
						return false;
					path.insert(std::make_pair(dpid, ip));
				}
			}
			// while(std::regex_match(path_msg, sm, FLOW_RULE_NODE))
			// {
			// 	int dpid = string_to_dpid(sm[1].str());
			// 	if(0 == dpid)
			// 		return false;
			// 	unsigned ip = string_to_ip(sm[2].str());
			// 	if(0 == ip)
			// 		return false;
			// 	path.insert(std::make_pair(dpid, ip));
			// 	path_msg = sm[3].str();
			// }
			// false negative or true negative?
			bool wrong_judgement = false;
			for(path_t::iterator it = path.begin(); it != path.end(); it++)
				if(injects.count(*it) != 0)
				{
					wrong_judgement = true;
					break;
				}
			if(wrong_judgement)
				fn++;
			else
				tn++;
		}
		else if(std::regex_match(line, sm, DETECTION_START_MSG))
		{
			detection_start_time = string_to_time_with_us(sm[1].str(), sm[2].str());
		}
		if(0 != time_us)
		{
			// recalculate tpr, tnr
			llong elapse_after_start = time_us - detection_start_time;
			/* actually, the false negative (FP) is very high */
			// std::cout << "TP: " << tp << "  TN: " << tn << "  FP: " << fp << "  FN: " << fn << std::endl;
			/* we known the number of injected malicious */
			// double tpr = ( (double)(tp) ) / ( (double) (tp + fn) );
			double tpr = ( (double)(uniq_tp.size()) ) / ( (double) (p) );   
			/* we don't know the number of normal flows as it is the inner state of FlowSelector and varies from time to time */
			double tnr = ( (double)(tn) ) / ( (double) (tn + fp) );
			accuracy_t acc(elapse_after_start, tpr, tnr);
			accuracy.push_back(acc);
		}
	}
	std::clog << std::endl;
	return true;
}


void output_raw_data(const vector< accuracy_array_t >& accuracy, std::ostream& os)
{
	for(vector< accuracy_array_t >::const_iterator it = accuracy.begin(); it != accuracy.end(); it++)
	{
		os << "Tick: ";
		for(accuracy_array_t::const_iterator data = it->begin(); data != it->end(); data++)
			os << std::setw(9) << data->tick;
		os << endl;
		os << "TPR:  ";
		for(accuracy_array_t::const_iterator data = it->begin(); data != it->end(); data++)
			os << std::setw(9) << data->tpr;
		os << endl;
		os << "TNR:  ";
		for(accuracy_array_t::const_iterator data = it->begin(); data != it->end(); data++)
			os << std::setw(9) << data->tnr;
		os << endl;
	}
}

void output_data_by_tick(const vector< accuracy_array_t >& accuracy, long output_tick, std::ostream& os)
{
	/* init */
	accuracy_array_t average;
	vector<int> idx_to_pos(accuracy.size(), 0);
	long min_tick = LONG_MAX;
	/* find the first tick */
	for(int idx = 0; idx < accuracy.size(); idx++)
	{
		int current_idx = idx_to_pos.at(idx);
		if(current_idx < accuracy.at(idx).size() && accuracy.at(idx).at(current_idx).tick < min_tick)
			min_tick = accuracy.at(idx).at(current_idx).tick;
	}
	if(LONG_MAX == min_tick)
		return;
	long current_tick = min_tick;
	bool has_more_values;
	do {
		has_more_values = false;
		double tpr = 0, tnr = 0;
		int avg_count = 0;
		for(int idx = 0; idx < accuracy.size(); idx++)
		{
			int current_idx = idx_to_pos.at(idx);
			while(current_idx < accuracy.at(idx).size() && accuracy.at(idx).at(current_idx).tick <= current_tick)
				current_idx++;
			has_more_values |= (current_idx != accuracy.at(idx).size());
			if(0 != current_idx)
			{
				current_idx--;
				idx_to_pos.at(idx) = current_idx;
				avg_count++;
				tpr += accuracy.at(idx).at(current_idx).tpr;
				tnr += accuracy.at(idx).at(current_idx).tnr;
			}
		}
		tpr = tpr/avg_count;
		tnr = tnr/avg_count;
		average.push_back(accuracy_t(current_tick, tpr, tnr));
		current_tick += output_tick;
	} while (has_more_values);
	os << std::setw(10) << "Tick" << std::setw(10) << "TPR" << std::setw(10) << "TNR" << std::endl;
	ios::fmtflags os_flag = os.flags();
	os << std::fixed;
	current_tick = min_tick;
	for(accuracy_array_t::iterator it = average.begin(); it != average.end(); it++)
	{
		os << std::setw(10) << current_tick << std::setw(10) << std::setprecision(4) << it->tpr << std::setw(10) << std::setprecision(4) << it->tnr << std::endl;
		current_tick += output_tick;
	}
	os.flags(os_flag);
}

bool parse_analyze_options(int argc, char *argv[], analyze_options& analyze_opts)
{
	char command;
	string tmp;
	while ((command = getopt (argc, argv, "t:r:d:")) != -1)
		switch (command)
		{
		case 't':
			analyze_opts.output_tick = std::atol(optarg);
			break;
		case 'r':
			analyze_opts.output_raw = optarg;
			{
				ofstream os(analyze_opts.output_raw);
				if(os.fail())
				{
					std::cerr << "invalid file '" << optarg << "' to write raw data." << std::endl;
					return false;
				}
			}
			break;
		case 'd':
			tmp = optarg;
			if(!is_valid_log_directory(tmp))
				return false;
			analyze_opts.log_dirs.push_back(tmp);
			break;
		case '?':
		default:
			return false;
      }
	while(optind < argc)
	{
		tmp = argv[optind++];
		if(!is_valid_log_directory(tmp))
			return false;
		analyze_opts.log_dirs.push_back(tmp);
	}
	if(analyze_opts.log_dirs.size() == 0)
	{
		std::cerr << "At least one log directory should be specified!" << std::endl;
		return false;
	}
	return true;
}
