#include <cstddef>
#include <stdexcept>

#include "rule.h"
#include "pswitch.h"
#include "../common/util.h"
#include "../common/exception.h"


namespace ns_network {
	using ns_common::ip2str;
	using ns_common::invalid_element;
	using std::logic_error;
	using std::make_pair;

	rule::rule() : sw(NULL), prefix(0), mask(0), next_hop(-1), next_hop_ip(0)
	{

	}

	rule::rule(pswitch* sw, unsigned int prefix, unsigned int mask, int next_hop, unsigned int next_hop_ip) :
		sw(sw), prefix(prefix), mask(mask), next_hop(next_hop), next_hop_ip(next_hop_ip)
	{
    
	}

	rule::rule(const rule& r)
	{
		this->sw = r.sw;
		this->prefix = r.prefix;
		this->mask = r.mask;
		this->next_hop = r.next_hop;
		this->next_hop_ip = r.next_hop_ip;
	}

	rule::~rule()
	{
    
	}

	pswitch* rule::get_switch() const
	{
		return this->sw;
	}

	rule* rule::set_switch(pswitch* sw)
	{
		this->sw = sw;
		return this;
	}

	unsigned int rule::get_prefix() const
	{
		return this->prefix;
	}

	// rule* rule::set_prefix(unsigned int prefix)
	// {
	// 	this->prefix = prefix;
	// 	return this;
	// }

	unsigned int rule::get_mask() const
	{
		return this->mask;
	}

	// rule* rule::set_mask(unsigned int mask)
	// {
	// 	this->mask = mask;
	// 	return this;
	// }

	std::pair<unsigned int,unsigned int> rule::get_match() const
	{
		return std::make_pair(this->prefix, this->mask);
	}

	// rule* rule::set_match(std::pair<unsigned int,unsigned int> match)
	// {
	// 	this->prefix = match.first;
	// 	this->mask = match.second;
	// 	return this;
	// }

	rule* rule::reset_local_match()
	{
		set< pair<unsigned int, unsigned int> > prefix;
		prefix.insert(make_pair(this->prefix, this->mask));
		this->local_match = rule_match(prefix);
		return this;
	}

	rule* rule::add_local_match(const rule_match& match)
	{
		this->local_match.add_match(match);
		return this;
	}

	rule* rule::subtract_local_match(const rule_match& match)
	{
		this->local_match.subtract_match(match);
		return this;
	}

	const rule_match& rule::get_local_match() const
	{
		return this->local_match;
	}

	rule_match rule::get_default_match() const
	{
		set< pair<unsigned int, unsigned int> > prefix;
		prefix.insert(make_pair(this->prefix, this->mask));
		return rule_match(prefix);
	}

	int rule::get_next_hop() const
	{
		return this->next_hop;
	}

	rule* rule::set_next_hop(int next_hop)
	{
		this->next_hop = next_hop;
		return this;
	}

	unsigned int rule::get_next_hop_ip() const
	{
		return this->next_hop_ip;
	}

	rule* rule::set_next_hop_ip(unsigned int next_hop_ip)
	{
		this->next_hop_ip = next_hop_ip;
		return this;
	}

	bool operator < (const rule& left, const rule& right)
	{
		if(left.get_switch() == NULL || right.get_switch() == NULL)
			return left.get_switch() != NULL;
		if(left.get_switch()->get_name() != right.get_switch()->get_name())
			return left.get_switch()->get_name() < right.get_switch()->get_name();
		if(left.get_prefix() != right.get_prefix())
			return left.get_prefix() < right.get_prefix();
		return left.get_mask() < right.get_mask();
	}

	ostream& operator << (ostream& os, const rule& rule)
	{
		os << "rule [" << "switch=" << rule.get_switch()->get_name() << ", prefix=" << ip2str(rule.get_prefix(), rule.get_mask()) << ", next_hop_ip=" << ip2str(rule.get_next_hop_ip()) << "]";
		return os;
	}

}
