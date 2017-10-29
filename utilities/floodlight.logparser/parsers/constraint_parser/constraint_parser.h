#ifndef __CONSTRAINT_PARSER_H
#define __CONSTRAINT_PARSER_H

#include <parser.h>
#include <parsers/constraint_parser/constraint.h>
#include <regex>

class constraint_parser : public parser
{
private:
	static const std::string PARSER_NAME;
	static const std::regex FLOW_REMOVED;
	static const std::regex FLOW_STATS;
	static const std::regex STATS;
	static const std::regex STATS_EX;
	static const std::regex CONSTRAINT;
	constraint* result;
	
public:
	constraint_parser();

	virtual const std::string& get_name() const;

	virtual parse_result parse_line(const std::string& line, int n);

	virtual const entity* pop_parse_result();

	virtual ~constraint_parser();
};

extern "C"
{
	parser* create_parser();
}

#endif
