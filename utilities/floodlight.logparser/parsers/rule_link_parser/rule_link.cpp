#include <parsers/rule_link_parser/rule_link.h>

rule_link::rule_link(const std::string& dst, int src_sw, int dst_sw) : dst(dst), src_sw(src_sw), dst_sw(dst_sw)
{
	
}

void rule_link::print_out(std::ostream &os) const
{
	os << "rule_link[ " << this->dst << ": " << src_sw << " --> " << dst_sw << " ]" << std::endl;
}

rule_link::~rule_link()
{
	
}

