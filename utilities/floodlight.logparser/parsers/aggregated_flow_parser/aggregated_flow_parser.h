#ifndef __AGGREGATED_FLOW_PARSER_H
#define __AGGREGATED_FLOW_PARSER_H

#include <parser.h>
#include <parsers/aggregated_flow_parser/aggregated_flow.h>
#include <regex>

class aggregated_flow_parser : public parser
{
private:
	static const std::string PARSER_NAME;
	static const std::regex DUMP_BEGIN;
	static const std::regex DUMP_END;
	static const std::regex FLOW_RULE_NODE;
	static const std::regex FLOW_RULE_NODE_EX;
	static const std::regex AGGREGATED_FLOW_NODE;
	static const std::regex AGGREGATED_FLOW_NODE_EX;
	static const std::regex AGGREGATED_FLOW;
	
	aggregated_flow* result;
	
	bool parse_flow_node(const std::string& node_content, int line_number);
	
public:
	aggregated_flow_parser();
	
	virtual const std::string& get_name() const;

	virtual parse_result parse_line(const std::string& line, int n);

	virtual const entity* pop_parse_result();

	virtual ~aggregated_flow_parser();
};


extern "C"
{
	parser* create_parser();
}

#endif
