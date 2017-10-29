#include <common/util.h>

#include <regex>
#include <iostream>

#include <time.h>
#include <unistd.h>
#include <sys/types.h>
#include <dirent.h>

std::string link_directory_and_file(const std::string& directory, const std::string& filename)
{
	static const char DIR_SEPARATOR = '/';
	if(0 == directory.size())
		return "./" + filename;
	if(DIR_SEPARATOR == directory.at(directory.size()-1))
		return directory + filename;
	else
		return directory + "/" + filename;
}

std::string get_mininet_log(const std::string& directory)
{
	const static std::string MININET_LOG_FILE = "mininet.log";
	return link_directory_and_file(directory, MININET_LOG_FILE);
}

std::string get_floodlight_log(const std::string& directory)
{
	const static std::string FLOODLIGHT_LOG_FILE = "floodlight.log";
	return link_directory_and_file(directory, FLOODLIGHT_LOG_FILE);
}

bool is_writable(const std::string& file)
{
	FILE* f = fopen(file.c_str(), "w");
	if(!f)
		fclose(f);
	return !f;
}

bool is_valid_log_directory(const std::string& dir_name)
{
	DIR *directory;
	if (!(directory = opendir(dir_name.c_str())))
	{
		std::cerr << "cannot open directory " << dir_name << std::endl;
		return false;
	}
	closedir(directory);
	FILE* f;
	const std::string mininet_log = get_mininet_log(dir_name);
	if(!(f = fopen(mininet_log.c_str(), "r")))
	{
		std::cerr<< "cannot find file mininet.log in directory " << dir_name << std::endl;
		return false;
	}
	fclose(f);
	const std::string floodlight_log = get_floodlight_log(dir_name);
	if(!(f=fopen(floodlight_log.c_str(), "r")))
	{
		std::cerr << "cannot find file floodlight.log in directory " << dir_name << std::endl;
		return false;
	}
	fclose(f);
	return true;
}

llong string_to_time_with_us(const std::string& _1st_part, const std::string& _2nd_part)
{
	const llong SEC_TO_USEC = 1000;
	struct tm tm;
	strptime(_1st_part.c_str(), "%Y-%m-%d %H:%M:%S", &tm);
	time_t time_no_us = mktime(&tm);
	llong tmp = time_no_us;
	return SEC_TO_USEC * tmp + std::atoi(_2nd_part.c_str());
}

int string_to_dpid(const std::string& str_dpid)
{
	const static std::regex DPID("(\\d+):(\\d+):(\\d+):(\\d+):(\\d+):(\\d+):(\\d+):(\\d+)");
	std::smatch sm;
	if(std::regex_match(str_dpid, sm, DPID))
	{
		int dpid = 0;
		for(int i = 0; i < 8; i++)
		{
			dpid *= 100;
			dpid += std::atoi(sm[1+i].str().c_str());
		}
		return dpid;
	}
	std::cerr << "unrecongnized datapath " << str_dpid << std::endl;
	return 0;
}

unsigned string_to_ip(const std::string& str_ip)
{
	const static std::regex IP("(\\d+).(\\d+).(\\d+).(\\d+)");
	std::smatch sm;
	if(std::regex_match(str_ip, sm, IP))
	{
		unsigned ip = 0;
		for(int i = 0; i < 4; i++)
		{
			ip = (ip << 8);
			ip = ip + std::atoi(sm[1+i].str().c_str());
		}
		return ip;
	}
	std::cerr << "unrecongnized ip " << str_ip << std::endl;
	return 0;
}

