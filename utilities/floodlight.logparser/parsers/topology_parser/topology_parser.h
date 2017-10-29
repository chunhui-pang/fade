#ifndef __TOPOLOGY_PARSER_H
#define __TOPOLOGY_PARSER_H

#include <parser.h>
#include <regex>
#include <parsers/topology_parser/topology.h>

class topology_parser : public parser
{
private:
	static const std::regex TOPO_BEGIN;
	static const std::regex TOPO_CONTENT;
	static const std::regex TOPO_END;
	static const std::string PARSER_NAME;

	topology* result;
	
public:
	topology_parser();

	virtual const std::string& get_name() const;
	
	virtual parser::parse_result parse_line(const std::string& line, int line_num);

	virtual const entity* pop_parse_result();

	virtual ~topology_parser();
};

#endif
