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

struct dedicated_rule_usage
{
	long tick;
	long current;
	long total;
	dedicated_rule_usage(long tick, long current, long total)
		: tick(tick), current(current), total(total)
		{
		}
};

typedef std::vector<dedicated_rule_usage> usage_array_t;
bool parse_options(int argc, char *argv[], analyze_options& option);
bool parse_floodlight_log(const std::string& filename, usage_array_t& usages);
void output_raw_delay(const std::vector< usage_array_t >& raw, std::ostream& os);
void output_by_tick(const std::vector< usage_array_t >& raw, int tick, std::ostream& os);

int main(int argc, char *argv[])
{
	analyze_options options;
	if(!parse_options(argc, argv, options))
		return 1;
	std::vector< usage_array_t > raw_data;
	for(vector<string>::iterator it = options.log_dirs.begin(); it != options.log_dirs.end(); it++)
	{
		const std::string floodlight_log = get_floodlight_log(*it);
		usage_array_t usages;
		if(!parse_floodlight_log(floodlight_log, usages))
			return false;
		raw_data.push_back(usages);
	}
	if(0 != options.output_raw.size())
	{
		std::ofstream os(options.output_raw);
		output_raw_delay(raw_data, os);
	}
	output_by_tick(raw_data, options.output_tick, std::cout);
    return 0;
}

bool parse_floodlight_log(const std::string& filename, usage_array_t& usages)
{
	const static std::regex DETECTION_START_MSG("(\\d+-\\d+-\\d+\\W+\\d+:\\d+:\\d+),(\\d+).*start\\Wnew\\Wdetection\\Wrun,\\Wrun\\Wid\\W=\\W1");
	const static std::regex USAGE_MSG("(\\d+-\\d+-\\d+\\W+\\d+:\\d+:\\d+),(\\d+).*usage\\Wof\\Wdedicated\\Wflow\\Wrules,\\Wcurrent size:\\W(\\d+),\\Wall\\Wusage:\\W(\\d+)");
	std::ifstream is(filename);
	std::string line;
	std::smatch sm;
	bool detection_start = false;
	llong detection_start_time = 0;
	long line_counter = 0;
	std::clog << "Floodlight log: " << std::flush;
	while(getline(is, line))
	{
		line_counter++;
		if(line_counter % 10000 == 0)
			std::clog << line_counter/10000 << "W " << std::flush;
		if(std::regex_match(line, sm, USAGE_MSG))
		{
			llong current_time = string_to_time_with_us(sm[1].str(), sm[2].str());
			long current_usage = std::atol(sm[3].str().c_str());
			long current_total = std::atol(sm[4].str().c_str());
			if(0 != detection_start_time)
			{
				dedicated_rule_usage usage(current_time - detection_start_time, current_usage, current_total);
				usages.push_back(usage);
			}
		}
		else if(!detection_start && std::regex_match(line, sm, DETECTION_START_MSG))
		{
			detection_start_time = string_to_time_with_us(sm[1].str(), sm[2].str());
			detection_start = true;
		}
	}
	std::clog << std::endl;
	return true;
}

void output_raw_delay(const std::vector< usage_array_t >& raw_data, std::ostream& os)
{
	for(std::vector< usage_array_t >::const_iterator it = raw_data.begin(); it != raw_data.end(); it++)
	{
		os << "Tick: ";
		for(usage_array_t::const_iterator ait = it->begin(); ait != it->end(); ait++)
			os << ait->tick << ' ';
		os << std::endl;
		os << "Current: ";
		for(usage_array_t::const_iterator ait = it->begin(); ait != it->end(); ait++)
			os << ait->current << ' ';
		os << std::endl;
		os << "Total: ";
		for(usage_array_t::const_iterator ait = it->begin(); ait != it->end(); ait++)
			os << ait->total << ' ';
		os << std::endl;
	}
}

void output_by_tick(const std::vector< usage_array_t >& raw_data, int tick, std::ostream& os)
{
	std::vector<int> current_poses(raw_data.size(), 0);
	long min_tick = LONG_MAX;
	for(std::vector< usage_array_t >::const_iterator it = raw_data.begin(); it != raw_data.end(); it++)
		if(0 != it->size())
			if(it->front().tick < min_tick)
				min_tick = it->front().tick;
	if(LONG_MAX == min_tick)
		return;
	std::vector<dedicated_rule_usage> avg_usage;
	long current_tick = min_tick;
	bool has_more_values;
	do{
		has_more_values = false;
		double avg_current = 0, avg_total = 0;
		int size = 0;
		for(int idx = 0; idx < raw_data.size(); idx++)
		{
			int pos = current_poses.at(idx);
			while(pos < raw_data.at(idx).size() && raw_data.at(idx).at(pos).tick <= current_tick)
				pos++;
			has_more_values |= (pos != raw_data.at(idx).size());
			if(0 != pos)
			{
				pos--;
				current_poses.at(idx) = pos;
				size++;
				avg_current += raw_data.at(idx).at(pos).current;
				avg_total += raw_data.at(idx).at(pos).total;
			}
		}
		avg_current /= size;
		avg_total /= size;
		avg_usage.push_back(dedicated_rule_usage(current_tick, avg_current, avg_total));
		current_tick += tick;
	} while (has_more_values);
	std::ios::fmtflags flags = os.flags();
	os << std::setw(10) << "Tick" << std::setw(10) << "Total" << std::setw(10) << "Now" << std::endl;
	current_tick = min_tick;
	for(std::vector<dedicated_rule_usage>::const_iterator it = avg_usage.begin(); it != avg_usage.end(); it++, current_tick += tick)
		os << std::setw(10) << it->tick << std::setw(10) << it->total << std::setw(10) << it->current << std::endl;
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
