#ifndef __PARSER_H
#define __PARSER_H

#include <string>
#include <entity.h>


class parser
{
public:
	enum parse_result
	{
		SUCCESS_AND_CONTINUE,
		SUCCESS_AND_STOP,
		SKIP
	};

	parser();
	
	virtual const std::string& get_name() const=0;
	/**
	 * parse a line. Return true if it is successfully parsed, and then the subsequent lines would be passed to this class until it return false
	 */
	virtual parse_result parse_line(const std::string& str, int n) = 0;
	
	/**
	 * get the parse result
	 */
	virtual const entity* pop_parse_result() = 0;

	/**
	 * deconstructor
	 */
	virtual ~parser();
};

extern "C"
{
	typedef parser* parser_factory();
	parser* create_parser();
}

#endif
