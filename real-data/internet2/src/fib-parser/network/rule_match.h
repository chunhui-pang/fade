#ifndef __RULE_MATCH_H
#define __RULE_MATCH_H

#include <utility>
#include <set>
#include <iostream>

namespace ns_network
{
	using std::pair;
	using std::set;
	using std::ostream;

	class rule_match
	{
	private:
		/**
		 * These matches will NOT intersect with each other for the sake of simplicity
		 */
		set< pair<unsigned int, unsigned int> > matches;

		void merge_matches();

		/** utility function */
		set< pair<unsigned int, unsigned int> > subtract_prefix(const pair<unsigned int, unsigned int>& p1, const pair<unsigned int, unsigned int>& p2) const;
		
	public:
		rule_match();
		rule_match(const set< pair<unsigned int, unsigned int> >& matches);
		rule_match(const rule_match& match);

		void add_prefix(const pair<unsigned int, unsigned int>& prefix);
		void subtract_prefix(const pair<unsigned int, unsigned int>& prefix);

		rule_match& add_match(const rule_match& match);
		rule_match& subtract_match(const rule_match& match);
		rule_match& intersect_match(const rule_match& match);

		set< pair<unsigned int, unsigned int> > get_as_prefix() const;

		
	};

	ostream& operator << (ostream& os, const rule_match& match);

}

#endif
