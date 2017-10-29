#ifndef __INTERFACE_PARSER_H
#define __INTERFACE_PARSER_H

#include <iostream>
#include <network/interface.h>

namespace ns_parser
{
	using std::istream;
	using ns_network::interface;
	
	class interface_parser
	{
	protected:
		istream& is;

		interface_parser(istream& is);
	public:
		virtual ~interface_parser();
		virtual interface* get_next() = 0;
	};
}
#endif
