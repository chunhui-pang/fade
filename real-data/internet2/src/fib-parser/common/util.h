#ifndef __UTIL_H
#define __UTIL_H
#include <string>
#include <vector>

namespace ns_common {
	
	unsigned int str2ip(const char* str);

	std::string ip2str(unsigned int ip, unsigned int mask = 0xFFFFFFFF);

	std::vector< std::pair<unsigned, unsigned> > subtract(std::pair<unsigned, unsigned> left, std::pair<unsigned, unsigned> right);

	unsigned random_choose_ip_from_prefix(const std::pair<unsigned, unsigned>& prefix);

	std::string to_mac(unsigned i_mac);
}

#endif
