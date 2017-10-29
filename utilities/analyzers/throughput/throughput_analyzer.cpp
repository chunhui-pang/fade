/**
 * utility to parse mininet output: throughput analysis
 */
#include <iostream>
#include <fstream>
#include <common/util.h>
#include <unistd.h>
#include <regex>
#include <cstdlib>
#include <vector>
#include <string>

using namespace std;

bool parse_options(int argc, char *argv[], std::vector<std::string>& log_dirs);
double parse_log_directory(const std::string& dir);

int main(int argc, char *argv[])
{
	std::vector<std::string> log_dirs;
	if(!parse_options(argc, argv, log_dirs))
		return false;
	double sum_throughput = 0;
	for(std::vector<std::string>::iterator it = log_dirs.begin(); it != log_dirs.end(); it++)
		sum_throughput += parse_log_directory(*it);
	sum_throughput /= log_dirs.size();
	std::cout << sum_throughput  << std::endl;
    return 0;
}

double parse_log_directory(const std::string& dir)
{
	const static std::regex THROUGHPUT_MSG(".*iperf\\Wbetween.*\\['([\\d.]+)\\W(G|M|K).*?([\\d.]+)\\W(G|M|K).*\\]");
	std::string mininet_log = get_mininet_log(dir);
	std::ifstream is(mininet_log);
	std::string line;
	std::smatch sm;
	double sum_throughput = 0;
	int size = 0;
	while(getline(is, line))
	{
		if(std::regex_match(line, sm, THROUGHPUT_MSG))
		{
			double throughput = std::atof(sm[1].str().c_str());
			char metric = sm[2].str().at(0);
			if('M' == metric) throughput /= 1000;
			if('K' == metric) throughput /= (1000*1000);
			sum_throughput += throughput;
			throughput = std::atof(sm[3].str().c_str());
			metric = sm[4].str().at(0);
			if('M' == metric) throughput /= 1000;
			if('K' == metric) throughput /= (1000*1000);
			sum_throughput += throughput;
			size += 2;
		}
	}
	if (size == 0)
	{
		std::cerr << "cannot find throughput message in the mininet file '" << mininet_log << "'" << std::endl;
		return -1.0f;
	}
	sum_throughput /= size;
	return sum_throughput;
}

bool parse_options(int argc, char *argv[], std::vector<std::string>& log_dirs)
{
	char command;
	std::string tmp;
	while((command = getopt(argc, argv, "d:")) != -1)
	{
		switch(command)
		{
		case 'd':
			tmp = optarg;
			if(!is_valid_log_directory(tmp))
				return false;
			log_dirs.push_back(tmp);
			break;
		case '?':
		default:
			return false;
		}
	}
	while(optind < argc)
	{
		tmp = argv[optind++];
		if(!is_valid_log_directory(tmp))
			return false;
		log_dirs.push_back(tmp);
	}
	if(log_dirs.size() == 0)
	{
		std::cerr << "At least one log directory should be specified!" << std::endl;
		return false;
	}
	return true;
}
