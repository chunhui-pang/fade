#include <sstream>
#include <random>
#include <chrono>
#include <climits>
#include <iomanip>

#include "util.h"
#include "exception.h"
using std::ostringstream;
using std::string;
using std::vector;
using std::pair;
using std::make_pair;

namespace ns_common {
	unsigned int str2ip(const char* str)
	{
		int dots = 0;
		const char* p = str;
		unsigned int res = 0;
		do{
			unsigned int d = 0;
			while('0' <= *p && *p <= '9'){
				d = d*10 + (*p-'0');
				p++;
			}
			res = (res << 8);
			res += d;
			if(*p == '.'){
				dots++;
				p++;
			}
			if((*p > '9' || *p < '0') && *p != '\0' && *p != '.'){
				std::string msg("unrecognized ip string '");
				msg.append(str);
				msg.append("'");
				throw invalid_element( msg );
			}
		}while(*p != '\0');
		if(dots > 3){
			std::string msg("unrecognized ip string '");
			msg.append(str);
			msg.append("'");
			throw invalid_element( msg );
		}else{
			res = (res << (8*(3-dots)));
		}
		return res;
	}

	string ip2str(unsigned int ip, unsigned int mask)
	{
		ostringstream oss;
		oss << ((ip >> 24) & 0xFF) << "." << ((ip >> 16) & 0xFF) << "." << ((ip >> 8) & 0xFF) << "." << (ip & 0xFF);
		if(0xFFFFFFFF != mask){
			int bits = 0;
			while(mask % 2 == 0){
				bits++;
				mask = (mask >> 1);
			}
			oss << '/' << (32-bits);
		}
		return oss.str();
	}


	vector< pair<unsigned, unsigned> > subtract(pair<unsigned, unsigned> left, pair<unsigned, unsigned> right)
	{
		vector< pair<unsigned, unsigned> > result;
		if(left.second < right.second){
			if( (left.first & left.second) == (right.first & left.second) ){ // right \in left
				unsigned int lprefix = left.first, lmask = left.second;
				while(lmask != right.second || (left.first & lmask) == (right.first & lmask)){
					unsigned new_mask = ( (lmask >> 1) | 0x80000000 );
					unsigned prefix0 = lprefix;
					unsigned prefix1 = ( lprefix | (lmask ^ new_mask) );
					if( (prefix0 & new_mask) == (right.first & new_mask) ){
						lprefix = prefix0;
						result.push_back(make_pair(prefix1, new_mask));
					}else{
						lprefix = prefix1;
						result.push_back(make_pair(prefix0, new_mask));
					}
					lmask = new_mask;
				}
			}
		}else{ // left.second > right.second
			if( (left.first & left.second) != (right.first & right.second) ) // empty intersection
				result.push_back(left);
		}
		return result;
	}

	unsigned random_choose_ip_from_prefix(const std::pair<unsigned, unsigned>& prefix)
	{
		if(0xFFFFFFFF == prefix.second)
			return prefix.first;
		unsigned min_host_val = 1;
		unsigned max_host_val = UINT_MAX - prefix.second;
		unsigned seed = std::chrono::system_clock::now().time_since_epoch().count();
		std::default_random_engine generator (seed);
		std::uniform_int_distribution<int> distribution(min_host_val, max_host_val);
		return prefix.first + distribution(generator);
	}

	std::string to_mac(unsigned i_mac)
	{
		const int MAC_DIGIT = 6;
		std::ostringstream oss;
		int max_digit = sizeof(unsigned);
		for(int i = 0; i < MAC_DIGIT - max_digit; i++)
			oss << "00:";
		while(--max_digit >= 0)
		{
			oss << std::setw(2) << std::setfill('0') << std::hex << ( (i_mac >> (max_digit*8)) & 0xFF );
			if(max_digit != 0)
				oss << ":";
		}
		return oss.str();
	}

}
