#ifndef __SINGLE_FLOW_PROBE_PARSER_H
#define __SINGLE_FLOW_PROBE_PARSER_H

#include <parser.h>
#include <parsers/single_flow_probe_parser/single_flow_probe.h>

#include <regex>

class single_flow_probe_parser : public parser
{
private:
	static const std::string PARSER_NAME;
	static const std::regex SINGLE_FLOW_NODE;
	static const std::regex SINGLE_FLOW_NODE_EX;
	static const std::regex SINGLE_FLOW;
	static const std::regex SINGLE_FLOW_EX;
	static const std::regex PROBE_CONTENT;
	static const std::regex SINGLE_FLOW_PROBE;
	single_flow_probe* result;

	void parse_flow(const std::string& flow_content);

	void parse_probes(const std::string& probes_content);
	
public:
	single_flow_probe_parser();

	virtual const std::string& get_name() const;

	virtual parse_result parse_line(const std::string& line, int n);

	virtual const entity* pop_parse_result();

	virtual ~single_flow_probe_parser();
};

extern "C"
{
	parser* create_parser();
}

#endif
