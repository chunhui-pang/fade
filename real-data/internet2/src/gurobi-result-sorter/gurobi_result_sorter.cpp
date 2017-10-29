/**
 * As for gurobi output variables of ilp model in a random order, we resort it with our service logic 
 */
#include <iostream>
#include <fstream>
#include <sstream>
#include <map>
#include <set>
#include <vector>
#include <string>
#include <string.h>
#include <stdexcept>

using namespace std;

double read_objective_value(istream& is);
pair<string, double> read_variable(istream& is);
/** comparator, the priorities of variables are as following:
 *     t_m, n_f_r, u_f_r, and variables with small $f$ or $r$ are precedent that others 
 */
class variable_comparator
{
public:
	bool operator() (const string& left, const string& right) const;
};

int main(int argc, char *argv[])
{
	if(argc != 2 || strcmp(argv[1], "-h") == 0 || strcmp(argv[1], "--help") == 0 ){
		cerr << "Usage: " << argv[0] << " solution-file" << endl;
		return 1;
	}
	ifstream is(argv[1]);
	if(!is){
		cerr << "Error: cannot open solution file: " << argv[0] << endl;
		return 2;
	}
	
	double obj_val = read_objective_value(is);
	map<string, double> values;
	set<string, variable_comparator> variables;
	while(!is.eof()){
		try{
			pair<string, double> val_pair = read_variable(is);
			values.insert(val_pair);
			variables.insert(val_pair.first);
		}catch(exception& expt){
			break;
		}
	}
	
	is.close();
	ofstream os(argv[1]);
	os << "# Objective value = " << obj_val << endl;
	for(auto it = variables.begin(); it != variables.end(); it++){
		os << (*it) << "\t" << values.at(*it) << endl;
	}
    return 0;
}

double read_objective_value(istream& is)
{
	string line;
	if(getline(is, line)){
		istringstream iss(line);
		string tmp;
		iss >> tmp >> tmp >> tmp >> tmp;
		double res;
		iss >> res;
		return res;
	} else {
		throw runtime_error("cannot read a line from file");
	}
}

pair<string, double> read_variable(istream& is)
{
	string line;
	if(getline(is, line)){
		string var;
		double val;
		istringstream iss(line);
		iss >> var >> val;
		return move(make_pair(var, val));
	} else {
		throw runtime_error("cannot read a line from file");
	}
}

bool variable_comparator::operator() (const string& left, const string& right) const
{
	/* handle t_m */
	if(left == "t_m") {
		return true;
	} else if(right == "t_m") {
		return false;
	}
	
	string major_var[2] = {"", ""};
	int idx[][2] = { {0, 0}, {0, 0} };
	int pre_pos = 0, pos = 0, cur = 0;
	while( (pos = left.find('_', pre_pos)) != string::npos ){
		switch(cur){
		case 0:
			major_var[0] = left.substr(pre_pos, pos);
			break;
		case 1:
			/* n_f_r, u_f_r, f and r are numbers */
			idx[0][0] = stoi(left.substr(pre_pos, pos));
			break;
		default:
			break;
		}
		cur++;
		pos++;
		pre_pos = pos;
	}
	idx[0][1] = stoi(left.substr(pre_pos));
	
	pre_pos = pos =  cur = 0;
	while( (pos = right.find('_', pre_pos)) != string::npos ){
		switch(cur){
		case 0:
			major_var[1] = right.substr(pre_pos, pos);
			break;
		case 1:
			idx[1][0] = stoi(right.substr(pre_pos, pos));
			break;
		default:
			break;
		}
		cur++;
		pos++;
		pre_pos = pos;
	}
	idx[1][1] = stoi(right.substr(pre_pos));
	if(major_var[0] != major_var[1]) return (major_var[0].at(0) - major_var[1].at(0)) < 0;
	if(idx[0][0] != idx[1][0]) return (idx[0][0] - idx[1][0]) < 0;
	return (idx[0][1] - idx[1][1]) < 0;
}
