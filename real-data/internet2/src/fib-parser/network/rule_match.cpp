#include <utility>
#include <map>

#include <network/rule_match.h>
#include <common/util.h>

namespace ns_network
{
	typedef unsigned int uint;
	using std::multimap;
	using std::make_pair;
	using ns_common::ip2str;

	rule_match::rule_match() : matches()
	{
		
	}

	rule_match::rule_match(const set< pair<uint, uint> >& matches) : matches(matches)
	{
		
	}

	rule_match::rule_match(const rule_match& match) : matches(match.matches)
	{
		
	}

	rule_match& rule_match::add_match(const ns_network::rule_match &match)
	{
		for(auto it = match.matches.begin(); it != match.matches.end(); it++)
			this->add_prefix(*it);
		return *this;
	}

	rule_match& rule_match::subtract_match(const ns_network::rule_match &match)
	{
		for(auto it = match.matches.begin(); it != match.matches.end(); it++)
			this->subtract_prefix(*it);
		return *this;
	}

	rule_match& rule_match::intersect_match(const ns_network::rule_match &match)
	{
		set< pair<uint, uint> > to_add;
		set< pair<uint, uint> > erase;
		for(auto it1 = this->matches.begin(); it1 != this->matches.end(); it1++)
		{
			bool has_intersect = false;
			for(auto it2 = match.matches.begin(); it2 != match.matches.end(); it2++)
			{
				if(it1->second >= it2->second && (it1->first & it2->second) == (it2->first & it2->second))
				{
					has_intersect = true;
					break;
				}
				else if(it1->second < it2->second && (it1->first & it1->second) == (it2->first & it1->second))
				{
					has_intersect = true;
					to_add.insert(*it2);
					erase.insert(*it1);
					break;
				}
			}
			if(!has_intersect)
				erase.insert(*it1);
		}
		for(auto it = erase.begin(); it != erase.end(); it++)
			this->matches.erase(*it);
		for(auto it = to_add.begin(); it != to_add.end(); it++)
			this->matches.insert(*it);
		return *this;
	}

	set< pair<uint, uint> > rule_match::get_as_prefix() const
	{
		set< pair<uint, uint> > results(this->matches.begin(), this->matches.end());
		return std::move(results);
	}

	/**
	 * private section
	 */
	void rule_match::add_prefix(const pair<uint, uint>& prefix)
	{
		bool duplicate = false;
		for(auto it = this->matches.begin(); it != this->matches.end(); ){
			if( (it->second <= prefix.second) && ((it->first & it->second) == (prefix.first & it->second)) ){
				duplicate = true;
				break;
			} else if( (it->second > prefix.second) && ((it->first & prefix.second) == (prefix.first & prefix.second)) ){
				it = this->matches.erase(it);
			} else {
				it++;
			}
		}
		if(!duplicate){
			this->matches.insert(prefix);
			this->merge_matches();			
		}

	}

	void rule_match::subtract_prefix(const pair<uint, uint>& prefix)
	{
		uint pp = prefix.first;
		uint mask = prefix.second;
		set< pair<uint, uint> > to_add;
		for(auto it = this->matches.begin(); it != this->matches.end(); ){
			if(it->second >= mask){
				if((it->first & mask) == (pp & mask)){
					it = this->matches.erase(it);
				} else {
					it++;
				}
			} else {
				if((it->first & it->second) == (pp & it->second)){
					set< pair<uint, uint> > result = subtract_prefix(*it, prefix);
					to_add.insert(result.begin(), result.end());
					it = this->matches.erase(it);
				} else {
					it++;
				}
			}
		}
		this->matches.insert(to_add.begin(), to_add.end());
	}
	
	void rule_match::merge_matches()
	{
		int merge_count = 0;
		/* multimap< pair<mask, (mask << 1) & prefix>, a single match> */
		multimap< pair<uint, uint>, pair<uint, uint> > counter_map;
		do{
			merge_count = 0;
			counter_map.clear();
			for(auto it = this->matches.begin(); it != this->matches.end(); it++){
				uint pp = it->first;
				uint mask = it->second;
				counter_map.insert(make_pair( make_pair(mask, (pp & (mask << 1))), *it));
			}
			for(auto it = counter_map.begin(); it != counter_map.end(); ){
				if(counter_map.count(it->first) == 2){
					merge_count++;
					pair<uint, uint> key = it->first;
					while(it != counter_map.upper_bound(key)){
						this->matches.erase(it->second);
						it++;
					}
					this->matches.insert(make_pair(key.second, (key.first << 1)));
				} else {
					it++;
				}
			}
		} while (merge_count != 0);
	}

	set< pair<uint, uint> > rule_match::subtract_prefix(const pair<uint, uint>& p1, const pair<uint, uint>& p2) const
	{
		set< pair<uint, uint> > result;
		if(p1.second >= p2.second){
			if( (p1.first & p2.second) == (p2.first & p2.second) ){
				return std::move(result);
			} else {
				result.insert(p1);
				return std::move(result);
			}
		} else {
			if((p1.first & p1.second) == (p2.first & p1.second)){
				uint mask = p2.second;
				uint prefix = p2.first;
				while(mask != p1.second){
					prefix = (prefix ^ (mask & ((~mask) << 1)));
					result.insert(make_pair(prefix, mask));
					mask = (mask << 1);
					prefix = prefix & mask;
				}
				return std::move(result);
			} else {
				result.insert(p1);
				return std::move(result);
			}
		}
		return std::move(result);
	}
	
	ostream& operator << (ostream& os, const rule_match& match)
	{
		set< pair<uint, uint> > prefixs = match.get_as_prefix();
		os << "[rule_match: ";
		if(prefixs.size() == 0){
			os << "empty";
		} else {
			for(auto it = prefixs.begin(); it != prefixs.end(); it++){
				os << ip2str(it->first, it->second) << ", ";
			}
		}
		os << "]";
		return os;
	}

}
