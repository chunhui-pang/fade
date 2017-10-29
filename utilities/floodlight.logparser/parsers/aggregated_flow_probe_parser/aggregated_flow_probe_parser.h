#ifndef __AGGREGATED_FLOW_PROBE_PARSER_H
#define __AGGREGATED_FLOW_PROBE_PARSER_H

#include <parser.h>
#include <parsers/aggregated_flow_probe_parser/aggregated_flow_probe.h>

#include <regex>

class aggregated_flow_probe_parser : public parser
{
private:
	static const std::string PARSER_NAME;
	static const std::regex AGGREGATED_FLOW_NODE;
	static const std::regex AGGREGATED_FLOW_NODE_EX;
	static const std::regex AGGREGATED_FLOW;
	static const std::regex AGGREGATED_FLOW_EX;
	static const std::regex PROBE_CONTENT;
	static const std::regex AGGREGATED_FLOW_PROBE;
	aggregated_flow_probe* result;

	void parse_flow(const std::string& flow_content);

	void parse_probes(const std::string& probes_content);
	
public:
	aggregated_flow_probe_parser();

	virtual const std::string& get_name() const;

	virtual parse_result parse_line(const std::string& line, int n);

	virtual const entity* pop_parse_result();

	virtual ~aggregated_flow_probe_parser();
};

extern "C"
{
	parser* create_parser();
}

#endif
