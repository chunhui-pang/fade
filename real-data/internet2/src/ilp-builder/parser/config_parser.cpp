#include <parser/config_parser.h>
#include <stdexcept>
#include <fstream>
#include <sstream>
#include <string>
#include <set>
#include <iostream>

const std::regex config_parser::line_begin1 = std::regex("==============================================================================");
const std::regex config_parser::line_begin2 = std::regex("Name\\W*id\\W*size\\W*path\\W*len\\W*path\\W*");
config_parser::config_parser(const option_parser& parser) : opts(parser), maximum_run(parser.get_max_run()), maximum_tcams(parser.get_max_tcams()),
															switches(), sw_id_map(), sw_name_map(),
															flows(), flow_id_map(), flow_map(), state(UNPARSED), parse_msg(""),
															model_output(parser.get_model_output_file()), solver_output(parser.get_solver_output_file())
{
}

config_parser::~config_parser()
{
	for(pswitch* psw : this->switches){
		delete psw;
	}
	for(aggregated_flow* flow : this->flows){
		delete flow;
	}
}

std::string config_parser::parse_configs()
{
	if(this->state == PARSE_SUCCESS) {
		return "";
	} else if(this->state == PARSE_FAIL) {
		return this->parse_msg;
	}
	std::ifstream fin(this->opts.get_input_file());
	std::string line1 = "", line2 = "";
	/* skip previous lines */
	while(std::getline(fin, line2) && !(std::regex_match(line1, line_begin1) && std::regex_match(line2, line_begin2))){
		line1 = line2;
	}
	/* parse contents */
	std::set<std::string> sw_names;
	std::vector< std::pair<int, std::vector<std::string> > > flows; /* size, path */
	
	/* tmp fields */
	std::string sw_name, tmp;
	int id, flow_size, path_len;
	while(std::getline(fin, line1)){
		int head = line1.find_first_not_of(' '), tail = line1.find_last_not_of(' ');
		line1 = line1.substr(head, tail - head+1);
		std::istringstream iss(line1);
		std::vector<std::string> path;
		iss >> sw_name >> id >> flow_size >> path_len;
		sw_names.insert(sw_name);
		while(!iss.eof()){
			iss >> tmp;
			path.push_back(tmp);
		}
		flows.push_back(std::make_pair(flow_size, path));
	}
	if(this->maximum_tcams.size() != 1 && this->maximum_tcams.size() != sw_names.size()){
		return "error: the length of specified maximum tcams is not equals to that of the switches";
	}
	if (this->maximum_tcams.size() == 1) {
		for (int i = 0; i < sw_names.size() - 1; i++) {
			this->maximum_tcams.push_back(this->maximum_tcams[0]);
		}
	}
	
	/* parse switches */
	int seq = 0;
	for(std::string name : sw_names){
		pswitch* psw = new pswitch(name, this->maximum_tcams[seq++]);
		this->add_switch(psw);
	}
	/* parse flows */
	for(std::pair<int, std::vector<std::string> > flow : flows){
		if(flow.second.size() <= 2){
			/* ignore flows with length less that 3 */
			continue;
		}
		std::vector<const pswitch*> path;
		for(std::string nm : flow.second){
			path.push_back(this->sw_name_map.at(nm));
		}
		aggregated_flow* af = new aggregated_flow(flow.first, path);
		this->add_flow(af);
	}
	this->state = PARSE_SUCCESS;
	return "";
}

int config_parser::get_maximum_run() const
{
	this->assert_parse_state();
	return this->maximum_run;
}


const std::vector<int>& config_parser::get_maximum_tcams() const
{
	this->assert_parse_state();
	return this->maximum_tcams;
}

int config_parser::get_maximum_tcams(const pswitch *psw) const
{
	this->assert_parse_state();
	for(int idx = 0; idx < this->switches.size() && idx < this->maximum_tcams.size(); idx++){
		if(this->switches.at(idx) == psw){
			return this->maximum_tcams.at(idx);
		}
	}
	throw std::logic_error("cannot find the switch");
}

const std::vector<pswitch*>& config_parser::get_switches() const
{
	this->assert_parse_state();
	return this->switches;
}

pswitch* config_parser::get_switch_by_id(const int &id) const
{
	this->assert_parse_state();
	if(this->sw_id_map.count(id) != 0){
		return this->sw_id_map.at(id);
	} else {
		return nullptr;
	}
}

pswitch* config_parser::get_switch_by_name(const std::string &name) const
{
	this->assert_parse_state();
	if(this->sw_name_map.count(name) != 0){
		return this->sw_name_map.at(name);
	} else {
		return nullptr;
	}
}

const std::vector<aggregated_flow*>& config_parser::get_aggregated_flows() const
{
	this->assert_parse_state();
	return this->flows;
}

const std::vector<aggregated_flow*>& config_parser::get_aggregated_flows_by_switch(pswitch* psw) const
{
	this->assert_parse_state();
	if(this->flow_map.count(psw) == 0){
		throw std::logic_error("Sorry, we cannot recognize the switch...");
	} 
	return this->flow_map.at(psw);
}

aggregated_flow* config_parser::get_aggregated_flow_by_id(const int &id) const
{
	this->assert_parse_state();
	if(this->flow_id_map.count(id) != 0){
		return this->flow_id_map.at(id);
	} else {
		return nullptr;
	}
}

std::ostream& config_parser::get_model_output_stream()
{
	if(this->model_output)
		return this->model_output;
	return std::cout;
}

std::ostream& config_parser::get_solver_output_stream()
{
	if(this->solver_output)
		return this->solver_output;
	return std::cout;
}

void config_parser::assert_parse_state() const
{
	switch(this->state){
	case UNPARSED:
		throw std::logic_error("please call parse_configs() first");
		break;
	case PARSE_FAIL:
		throw std::logic_error(this->parse_msg);
		break;
	default:
		break;
		/* unreachable */
	}
}

bool config_parser::add_switch(pswitch* psw)
{
	if(this->sw_id_map.count(psw->get_id()) != 0){
		return false;
	}
	this->switches.push_back(psw);
	this->sw_id_map.insert(std::make_pair(psw->get_id(), psw));
	this->sw_name_map.insert(std::make_pair(psw->get_name(), psw));
	this->flow_map.insert(std::make_pair(psw, std::vector<aggregated_flow*>()));
	return true;
}

bool config_parser::add_flow(aggregated_flow* flow)
{
	if(this->flow_id_map.count(flow->get_id()) != 0){
		return false;
	}
	pswitch* owner = const_cast<pswitch*>(flow->get_path().at(0));
	if(this->flow_map.count(owner) == 0){
		this->flow_map.insert(std::make_pair(owner, std::vector<aggregated_flow*>()));
	}
	this->flow_map.at(owner).push_back(flow);
	this->flow_id_map.insert(std::make_pair(flow->get_id(), flow));
	this->flows.push_back(flow);
	return true;
}

