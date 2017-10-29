#include <iostream>
#include <string>
#include <vector>
#include <iterator>
#include <algorithm>
#include "trie.hpp"

using namespace std;
using namespace ns_trie;

void dump_result(const vector<string>& vec)
{
	cout << "[";
	copy(vec.begin(), vec.end(), ostream_iterator<string>(cout, ", "));
	cout << "]" << endl;
}

int main(int argc, char *argv[])
{
	trie<string> test;
	test.insert_prefix(make_pair(0xFF000000, 0xFF000000), "255.0.0.0/8");
	dump_result(test.search_match(make_pair(0xFF000000, 0xFF000000)));

	test.insert_prefix(make_pair(0xFFF00000, 0xFFF00000), "255.240.0.0/12");
	dump_result(test.search_match(make_pair(0xFFF00000, 0xFFF00000)));
	dump_result(test.search_match(make_pair(0xFF000000, 0xFFF00000)));
	
	test.insert_prefix(make_pair(0xF0800000, 0xFFC00000), "240.128.0.0/10");
	test.insert_prefix(make_pair(0xF0C00000, 0xFFC00000), "240.192.0.0/10");
	test.insert_prefix(make_pair(0xF0800000, 0xFF800000), "240.128.0.0/9");
	dump_result(test.search_match(make_pair(0xF0800000, 0xFF800000)));

	cout << test.remove_prefix(make_pair(0xF0800000, 0xFFC00000)) << endl;
	cout << test.remove_prefix(make_pair(0xF0C00000, 0xFFC00000)) << endl;
	cout << test.remove_prefix(make_pair(0xF0800000, 0xFF800000)) << endl;

	cout << test.remove_prefix(make_pair(0xFFF00000, 0xFFF00000)) << endl;
	cout << test.remove_prefix(make_pair(0xFF000000, 0xFF000000)) << endl;
	cout << "current size: " << test.size() << endl;
    return 0;
}





