#include <iostream>
#include <set>
#include <vector>
#include <utility>
#include "rule_match.h"

using namespace std;
using namespace ns_network;

typedef unsigned int uint;

int main(int argc, char *argv[])
{
	set< pair<uint, uint> > p1;
	p1.insert(make_pair(0xA0A00000, 0xFFFF0000));
    rule_match rm1(p1);

	set< pair<uint, uint> > p2;
	p2.insert(make_pair(0xA0A01000, 0xFFFFF000));
	rule_match rm2(p2);

	set< pair<uint, uint> > p3;
	p3.insert(make_pair(0xA0A08000, 0xFFFF8000));
	rule_match rm3(p3);
	cout << "rm1 = " << rm1 << endl;
	cout << "rm2 = " << rm2 << endl;
	cout << "rm3 = " << rm3 << endl;
	cout << "rm1.subtract_match(rm2) =" << (rm1.subtract_match(rm2)) << endl;
	cout << "rm1.subtract_match(rm3) = " << (rm1.subtract_match(rm3)) << endl;
	cout << "rm1.add_match(rm2) = " << rm1.add_match(rm2) << endl;
	cout << "rm1.add_match(rm3) = " << rm1.add_match(rm3) << endl;
	/** outputs
	 * rm1 = [rule_match: 160.160.0.0/16, ]
	 * rm2 = [rule_match: 160.160.16.0/20, ]
	 * rm3 = [rule_match: 160.160.128.0/17, ]
	 * rm1.subtract_match(rm2) =[rule_match: 160.160.0.0/20, 160.160.32.0/19, 160.160.64.0/18, 160.160.128.0/17, ]
	 * rm1.subtract_match(rm3) = [rule_match: 160.160.0.0/20, 160.160.32.0/19, 160.160.64.0/18, ]
	 * rm1.add_match(rm2) = [rule_match: 160.160.0.0/17, ]
	 * rm1.add_match(rm3) = [rule_match: 160.160.0.0/16, ]
	 */
    return 0;
}

