#ifndef __RULE_PARSER_H
#define __RULE_PARSER_H

#include <iostream>
#include <network/rule.h>

namespace ns_parser
{
	class rule_parser
	{
	protected:
		std::istream& is;
		rule_parser(std::istream& is);
	public:
		virtual ~rule_parser();
		virtual ns_network::rule* get_next() = 0;
	};
}
#endif
