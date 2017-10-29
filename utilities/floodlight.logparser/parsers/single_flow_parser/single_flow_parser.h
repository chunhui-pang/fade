#ifndef __SINGLE_FLOW_SELECTOR_PARSER_H
#define __SINGLE_fLOW_SELECTOR_PARSER_H

#include <parser.h>
#include <regex>
#include <parsers/single_flow_parser/single_flow.h>

class single_flow_selector_parser : public parser
{
private:
	static const std::string PARSER_NAME;
	static const std::regex SINGLE_FLOW_BEGIN;
	static const std::regex SINGLE_FLOW_CONTENT;
	static const std::regex SINGLE_FLOW_END;
	static const std::regex SINGLE_FLOW_NODE;
	// the extension EX means adding all redundant infos
	static const std::regex SINGLE_FLOW_NODE_EX;
	static const std::regex SINGLE_FLOW;
	static const std::regex SINGLE_FLOW_EX;

	single_flow* result;

	void parse_single_flow(const std::string& flow_content);
	
public:
	single_flow_selector_parser();

	virtual const std::string& get_name() const;
	
	virtual parser::parse_result parse_line(const std::string& line, int line_counter);

	virtual const entity* pop_parse_result();
	
	virtual ~single_flow_selector_parser();
};

extern "C"
{
	parser* create_parser();
}
#endif
