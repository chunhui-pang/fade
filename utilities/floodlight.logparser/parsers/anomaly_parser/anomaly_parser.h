#ifndef __ANOMALY_PARSER_H
#define __ANOMALY_PARSER_H
#include <regex>
#include <parser.h>
#include <parsers/anomaly_parser/anomaly.h>

class anomaly_parser : public parser
{
private:
	static const std::regex ANOMALY_REGEX;
	static const std::string PARSER_NAME;
	anomaly* result;
	
public:
	anomaly_parser();
	
	virtual const std::string& get_name() const;

	virtual parse_result parse_line(const std::string& line, int counter);

	virtual const entity* pop_parse_result();

	virtual ~anomaly_parser();
};

extern "C"
{
	parser* create_parser();
}

#endif
