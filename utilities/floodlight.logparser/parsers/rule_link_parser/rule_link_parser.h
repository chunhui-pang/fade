#ifndef __RULE_LINK_PARSER_H
#define __RULE_LINK_PARSER_H
#include <parser.h>
#include <parsers/rule_link_parser/rule_link.h>
#include <regex>

class rule_link_parser : public parser
{
private:
	static const std::regex RULE_LINK_REGEX;
	static const std::string PARSER_NAME;
	
	rule_link* result;
public:
	
	rule_link_parser();

	virtual const std::string& get_name() const;
	
	virtual parser::parse_result parse_line(const std::string& line, int line_counter);

	virtual const entity* pop_parse_result();

	virtual ~rule_link_parser();
};

extern "C"
{
	parser* create_parser();
}
#endif
