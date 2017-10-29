#include <iostream>
#include <vector>
#include <fstream>
#include <cstdlib>
#include <unistd.h>
#include <algorithm>
#include <iterator>
#include <map>
#include <climits>
#include <iomanip>
#include <regex>

#include <common/util.h>

using namespace std;

struct analyze_options
{
	/* directories that contains the logs */
	vector<string> log_dirs;
	/* the output tick, in ms */
	int output_tick;
	/* output raw data */
	string output_raw;
	analyze_options(const string& output_raw = "", int output_tick = 1000)
		: log_dirs(), output_tick(output_tick), output_raw(output_raw)
		{
		}
};

typedef std::vector<long> delay_array_t;
bool parse_options(int argc, char *argv[], analyze_options& option);
bool parse_floodlight_log(const std::string& filename, delay_array_t& delays);
void output_raw_delay(const std::vector< delay_array_t >& raw, std::ostream& os);
void output_by_tick(const std::vector< delay_array_t >& raw, int tick, std::ostream& os);


int main(int argc, char *argv[])
{
	analyze_options options;
	if(!parse_options(argc, argv, options))
		return 1;
	std::vector< delay_array_t > raw_data;
	for(vector<string>::iterator it = options.log_dirs.begin(); it != options.log_dirs.end(); it++)
	{
		const std::string floodlight_log = get_floodlight_log(*it);
		delay_array_t delays;
		if(!parse_floodlight_log(floodlight_log, delays))
			return false;
		raw_data.push_back(delays);
	}
	if(0 != options.output_raw.size())
	{
		std::ofstream os(options.output_raw);
		output_raw_delay(raw_data, os);
	}
	output_by_tick(raw_data, options.output_tick, std::cout);
    return 0;
}

bool parse_floodlight_log(const std::string& filename, delay_array_t& delays)
{
	const static std::regex DELAY_MSG(".*rule\\Wgraph\\Wupdate\\Wfor\\Wmessage\\W.*latency:\\W(\\d+)(ns|us|ms|s)");
	std::ifstream is(filename);
	std::string line;
	std::smatch sm;
	long line_counter = 0;
	std::clog << "Floodlight log: " << std::flush;
	while(getline(is, line))
	{
		line_counter++;
		if(line_counter % 10000 == 0)
			std::clog << line_counter/10000 << "W " << std::flush;
		if(std::regex_match(line, sm, DELAY_MSG))
		{
			long delay = std::atol(sm[1].str().c_str());
			std::string metric = sm[2].str();
			// convert all to us
			if(metric == "ns")
				delay /= 1000;
			else if(metric == "ms")
				delay *= 1000;
			else if(metric == "s")
				delay *= (1000*1000);
			delays.push_back(delay);
		}
	}
	std::clog << std::endl;
	std::sort(delays.begin(), delays.end());
	return true;
}

void output_raw_delay(const std::vector< delay_array_t >& raw_data, std::ostream& os)
{
	for(std::vector< delay_array_t >::const_iterator it = raw_data.begin(); it != raw_data.end(); it++)
	{
		if(0 == it->size())
			continue;
		os << '[';
		copy(it->begin(), --it->end(), std::ostream_iterator<int>(os, ", "));
		os << it->back();
		os << ']' << std::endl;
	}
}

void output_by_tick(const std::vector< delay_array_t >& raw_data, int tick, std::ostream& os)
{
	std::vector<int> current_poses(raw_data.size(), 0);
	long min_val = LONG_MAX;
	for(std::vector< delay_array_t >::const_iterator it = raw_data.begin(); it != raw_data.end(); it++)
		if(0 != it->size())
			if(min_val > it->front())
				min_val = it->front();
	if(LONG_MAX == min_val)
		return;
	std::vector<double> avg_delays;
	long current_val = min_val;
	bool has_more_values;
	do{
		has_more_values = false;
		double ratio = 0.0;
		int size = 0;
		for(int idx = 0; idx < raw_data.size(); idx++)
		{
			int pos = current_poses.at(idx);
			while(pos < raw_data.at(idx).size() && raw_data.at(idx).at(pos) <= current_val)
				pos++;
			has_more_values |= (pos != raw_data.at(idx).size());
			if(0 != pos)
			{
				pos--;
				current_poses.at(idx) = pos;
				size++;
				ratio += (double) (pos+1) / (double) raw_data.at(idx).size();
			}
		}
		ratio /= size;
		avg_delays.push_back(ratio);
		current_val += tick;
	} while (has_more_values);
	std::ios::fmtflags flags = os.flags();
	os << std::setw(10) << "Tick" << std::setw(10) << "Ratio" << std::endl;
	current_val = min_val;
	os << std::fixed;
	for(std::vector<double>::iterator it = avg_delays.begin(); it != avg_delays.end(); it++, current_val += tick)
		os << std::setw(10) << current_val << std::setw(10) << std::setprecision(4) << *it << std::endl;
	os.flags(flags);
}

bool parse_options(int argc, char *argv[], analyze_options& option)
{
	char command;
	std::string tmp;
	while((command = getopt(argc, argv, "r:t:d:")) != -1)
		switch(command)
		{
		case 'r':
			tmp = optarg;
			if(!is_writable(tmp))
			{
				std::cerr << "cannot write file " << tmp << std::endl;
				return false;
			}
			option.output_raw = tmp;
			break;
		case 't':
			option.output_tick = std::atoi(optarg);
			if(option.output_tick <= 0)
			{
				std::cerr << "The output tick must be a positive value." << std::endl;
				return false;
			}
			break;
		case 'd':
			tmp = optarg;
			if(!is_valid_log_directory(tmp))
			{
				std::cerr << "directory '" << tmp << "' is not a valid log directory." << std::endl;
				return false;
			}
			option.log_dirs.push_back(tmp);
			break;
		case '?':
		default:
			return false;
		}
	while(optind < argc){
		tmp = argv[optind++];
		if(!is_valid_log_directory(tmp))
			return false;
		option.log_dirs.push_back(tmp);
	}
	if(option.log_dirs.size() == 0)
	{
		std::cerr << "At least one log directory should be specified!" << std::endl;
		return false;
	}
	return true;
}
