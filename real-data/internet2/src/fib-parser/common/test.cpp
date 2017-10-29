#include <iostream>
#include <vector>
#include <iomanip>
#include <cstdlib>
#include <iterator>
#include "util.h"
using namespace std;

vector< pair<unsigned, unsigned> > subtract(pair<unsigned, unsigned> left, pair<unsigned, unsigned> right);

int main(int argc, char *argv[])
{
    string test_cases[][2] = {
		{"10.0.0.0/8", "10.0.0.0/8"},
		{"10.0.0.0/8", "10.128.0.0/9"},
		{"10.0.0.0/8", "10.0.0.128/25"},
		{"10.0.0.0/24", "10.0.0.0/8"},
		{"10.0.0.0/16", "10.0.0.7/32"},
    };
    for(int i = 0; i < sizeof(test_cases)/sizeof(test_cases[0]); i++){
		unsigned slash_pos = test_cases[i][0].find('/');
		unsigned left_prefix = str2ip(test_cases[i][0].substr(0, slash_pos).c_str());
		unsigned left_mask = (0xFFFFFFFF << (32 - atoi(test_cases[i][0].substr(slash_pos+1).c_str())));
		
		slash_pos = test_cases[i][1].find('/');
		unsigned right_prefix = str2ip(test_cases[i][1].substr(0, slash_pos).c_str());
		unsigned right_mask = (0xFFFFFFFF << (32 - atoi(test_cases[i][1].substr(slash_pos+1).c_str())));
	
		vector< pair<unsigned, unsigned> > result = subtract(make_pair(left_prefix, left_mask), make_pair(right_prefix, right_mask));
		cout << test_cases[i][0] << " - " << test_cases[i][1] << " = " << endl;
		for(vector< pair<unsigned, unsigned> >::iterator it = result.begin(); it != result.end(); it++){
			cout << "\t" << ip2str(it->first, it->second) << endl;
		}
    }
    return 0;
}


vector< pair<unsigned, unsigned> > subtract(pair<unsigned, unsigned> left, pair<unsigned, unsigned> right)
{
    vector< pair<unsigned, unsigned> > result;
    if(left.second < right.second){
		if( (left.first & left.second) == (right.first & left.second) ){ // right \in left
			int lprefix = left.first, lmask = left.second;
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
