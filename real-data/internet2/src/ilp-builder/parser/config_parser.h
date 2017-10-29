#ifndef __CONFIG_PARSER_H
#define __CONFIG_PARSER_H

#include <vector>
#include <set>
#include <map>
#include <regex>
#include <ostream>
#include <fstream>

#include <parser/option_parser.h>
#include <model/pswitch.h>
#include <model/aggregated_flow.h>

/**
 * ILP builder configurations
 */
class config_parser
{
public:
	config_parser(const option_parser& parser);
	virtual ~config_parser();
	/* parse configurations, return "" if successful. otherwise, it reutrns error message */
	virtual std::string parse_configs();
	virtual int get_maximum_run() const;
	virtual const std::vector<int>& get_maximum_tcams() const;
	virtual int get_maximum_tcams(const pswitch* psw) const;
	virtual const std::vector<pswitch*>& get_switches() const;
	virtual pswitch* get_switch_by_id(const int& id) const;
	virtual pswitch* get_switch_by_name(const std::string& name) const;
	virtual const std::vector<aggregated_flow*>& get_aggregated_flows() const;
	virtual const std::vector<aggregated_flow*>& get_aggregated_flows_by_switch(pswitch* psw) const;
	virtual aggregated_flow* get_aggregated_flow_by_id(const int& id) const;
	virtual std::ostream& get_model_output_stream();
	virtual std::ostream& get_solver_output_stream();
	
private:
	int maximum_run;
	std::vector<int> maximum_tcams; /* its size is equals to the number of switches */
	std::vector<pswitch*> switches;
	std::map<int, pswitch*> sw_id_map;
	std::map<std::string, pswitch*> sw_name_map;
	std::vector<aggregated_flow*> flows;
	std::map<pswitch*, std::vector<aggregated_flow*> > flow_map;
	std::map<int, aggregated_flow*> flow_id_map;
	option_parser opts;
	enum {UNPARSED, PARSE_SUCCESS, PARSE_FAIL} state;
	std::string parse_msg;
	std::ofstream model_output;
	std::ofstream solver_output;
	static const std::regex line_begin1;
	static const std::regex line_begin2;
	void assert_parse_state() const;
	
	/* return switch/flow id, if error, return -1 */
	bool add_switch(pswitch* psw);
	bool add_flow(aggregated_flow* flow);
};

#endif
