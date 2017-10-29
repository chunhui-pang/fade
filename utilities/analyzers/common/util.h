#ifndef __ANALYZER_UTIL_H
#define __ANALYZER_UTIL_H

#include <string>

// define types
typedef long long llong;

/**
 * concat a directory and a file
 */
std::string link_directory_and_file(const std::string& dir, const std::string& filename);

/**
 * get the filename of the mininet log from a directory
 */
std::string get_mininet_log(const std::string& directory);

/**
 * get the filename of floodlight log from a directory
 */
std::string get_floodlight_log(const std::string& directory);

/**
 * if a file is writable
 */
bool is_writable(const std::string& file);

/**
 * validate if a directory is an experiment log directory 
 */
bool is_valid_log_directory(const std::string& dir_name);

/**
 * convert 2017-04-04 05:07:22,888 to llong 
 */
llong string_to_time_with_us(const std::string& _1st_part, const std::string& _2nd_part);

/**
 * convert a string dpid to int
 */
int string_to_dpid(const std::string& str_dpid);

/**
 * convert an string ip address to unsigned int
 */
unsigned string_to_ip(const std::string& str_ip);


#endif
