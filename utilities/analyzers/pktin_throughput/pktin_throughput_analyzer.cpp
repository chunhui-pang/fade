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
#include <unistd.h>
#include <sys/types.h>
#include <dirent.h>

#include <common/util.h>

using namespace std;

struct analyze_options
{
	/* directories that contains the logs */
	vector<string> log_dirs;
	/* the output tick, in per flow/s */
	int output_tick;
	/* output raw data */
	string output_raw;
	analyze_options(const string& output_raw = "", int output_tick = 1000)
		: log_dirs(), output_tick(output_tick), output_raw(output_raw)
		{
		}
};

typedef std::vector<double> throughput_array_t;
bool parse_options(int argc, char *argv[], analyze_options& option);
bool parse_cbench_log(const std::string& filename, throughput_array_t& throughput);
void output_raw_throughput(const std::vector< throughput_array_t >& raw, std::ostream& os);
void output_by_tick(const std::vector< throughput_array_t >& raw, int tick, std::ostream& os);
bool is_valid_cbench_directory(const std::string& dir);
std::string get_cbench_log(const std::string& dir);

int main(int argc, char *argv[])
{
	analyze_options options;
	if(!parse_options(argc, argv, options))
		return 1;
	std::vector< throughput_array_t > raw_data;
	for(vector<string>::iterator it = options.log_dirs.begin(); it != options.log_dirs.end(); it++)
	{
		const std::string cbench_log = get_cbench_log(*it);
		throughput_array_t throughputs;
		if(!parse_cbench_log(cbench_log, throughputs))
			return false;
		raw_data.push_back(throughputs);
	}
	if(0 != options.output_raw.size())
	{
		std::ofstream os(options.output_raw);
		output_raw_throughput(raw_data, os);
	}
	output_by_tick(raw_data, options.output_tick, std::cout);
    return 0;
}

bool parse_cbench_log(const std::string& filename, throughput_array_t& throughputs)
{
	const static std::regex PKTIN_THROUGHPUT_MSG(".*flows/sec.*total\\W=\\W([\\d.]+).*");
	std::ifstream is(filename);
	std::string line;
	std::smatch sm;
	while(getline(is, line))
	{
		if(std::regex_match(line, sm, PKTIN_THROUGHPUT_MSG))
		{
			double throughput = std::atof(sm[1].str().c_str());
			throughput *= 1000;  // from per ms to per second
			throughputs.push_back(throughput);
		}
	}
	std::sort(throughputs.begin(), throughputs.end());
	return true;
}

void output_raw_throughput(const std::vector< throughput_array_t >& raw_data, std::ostream& os)
{
	for(std::vector< throughput_array_t >::const_iterator it = raw_data.begin(); it != raw_data.end(); it++)
	{
		if(0 == it->size())
			continue;
		os << '[';
		copy(it->begin(), --it->end(), std::ostream_iterator<double>(os, ", "));
		os << it->back();
		os << ']' << std::endl;
	}
}

void output_by_tick(const std::vector< throughput_array_t >& raw_data, int tick, std::ostream& os)
{
	std::vector<int> current_poses(raw_data.size(), 0);
	double min_val = LONG_MAX;
	for(std::vector< throughput_array_t >::const_iterator it = raw_data.begin(); it != raw_data.end(); it++)
		if(0 != it->size())
			if(min_val > it->front())
				min_val = it->front();
	if(LONG_MAX == min_val)
		return;
	std::vector<double> avg_throughput;
	double current_val = min_val;
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
		avg_throughput.push_back(ratio);
		current_val += tick;
	} while (has_more_values);
	std::ios::fmtflags flags = os.flags();
	os << std::setw(10) << "Throughput" << std::setw(10) << "Ratio" << std::endl;
	current_val = min_val;
	os << std::fixed;
	for(std::vector<double>::iterator it = avg_throughput.begin(); it != avg_throughput.end(); it++, current_val += tick)
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
			if(!is_valid_cbench_directory(tmp))
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
		if(!is_valid_cbench_directory(tmp))
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

bool is_valid_cbench_directory(const std::string& dir)
{
	DIR *directory;
	if (!(directory = opendir(dir.c_str())))
	{
		std::cerr << "cannot open directory " << dir << std::endl;
		return false;
	}
	closedir(directory);
	FILE* f;
	const std::string cbench_log = get_cbench_log(dir);
	if(!(f = fopen(cbench_log.c_str(), "r")))
	{
		std::cerr<< "cannot find file cbench.log in directory " << dir << std::endl;
		return false;
	}
	fclose(f);
	return true;
}
std::string get_cbench_log(const std::string& dir)
{
	const static std::string CBENCH_LOG = "cbench.log";
	return link_directory_and_file(dir, CBENCH_LOG);
}
