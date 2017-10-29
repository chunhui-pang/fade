#ifndef __LOCALIZATION_PARSER_H
#define __LOCALIZATION_PARSER_H
#include<parser.h>
#include <regex>

#include <parsers/localization_parser/localization.h>

class localization_parser : public parser
{
private:
	static const std::string PARSER_NAME;
	static const std::regex LOCALIZATION_REGEX;
	localization* result;
public:
	localization_parser();

	virtual const std::string& get_name() const;

	virtual parse_result parse_line(const std::string& str, int n);

	virtual const entity* pop_parse_result();

	virtual ~localization_parser();
};

extern "C"
{
	parser* create_parser();
}
#endif
