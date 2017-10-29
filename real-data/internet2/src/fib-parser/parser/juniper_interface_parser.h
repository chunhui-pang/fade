#ifndef __JUNIPER_INTERFACE_PARSER_H
#define __JUNIPER_INTERFACE_PARSER_H

#include <regex>

#include <parser/interface_parser.h>

namespace ns_parser
{
	class juniper_interface_parser : public interface_parser
	{
	private:
		static const std::regex intf_start;
		static const std::regex intf_ipv4_addr;
		
		int id;
		bool has_name;
		bool has_ip;
		interface* current;
		void report_error() const;
	public:
		juniper_interface_parser(istream& is);
		~juniper_interface_parser();
		virtual interface* get_next();
	};
}
#endif
