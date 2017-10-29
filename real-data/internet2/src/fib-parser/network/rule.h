#ifndef __RULE_H
#define __RULE_H

#include <utility>
#include <ostream>
#include <vector>
#include <set>
#include "rule_match.h"

namespace ns_network {
	using std::ostream;
	using std::vector;
	using std::pair;
	using std::set;
	using std::pair;

	class pswitch;

	class rule
	{
	private:
		pswitch* sw;
		unsigned int prefix;
		unsigned int mask;
		int next_hop;
		unsigned int next_hop_ip;
		rule_match local_match;
    
	public:
		enum default_next_hop {DISCARD=-1, DENY = -2};
		rule();
		rule(pswitch* sw, unsigned int prefix, unsigned int mask, int next_hop, unsigned int next_hop_ip);
		rule(const rule& r);
		~rule();

		/** get the switch */
		pswitch* get_switch() const;
		/** set switch, return the old one */
		rule* set_switch(pswitch* sw);
    
		unsigned int get_prefix() const;
		// rule* set_prefix(unsigned int prefix);
    
		unsigned int get_mask() const;
		// rule* set_mask(unsigned int mask);

		std::pair<unsigned int,unsigned int> get_match() const;
		// rule* set_match(std::pair<unsigned int,unsigned int> match);

		/* local match manipulation */
		rule* reset_local_match();
		rule* add_local_match(const rule_match& match);
		rule* subtract_local_match(const rule_match& match);
		const rule_match& get_local_match() const;
		rule_match get_default_match() const;

		int get_next_hop() const;
		rule* set_next_hop(int next_hop);

		unsigned int get_next_hop_ip() const;
		rule* set_next_hop_ip(unsigned int next_hop_ip);
	};

	bool operator < (const rule& left, const rule& right);
	ostream& operator << (ostream& os, const rule& rule);

}

#endif
