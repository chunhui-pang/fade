#include <parsers/localization_parser/localization.h>

localization::localization(int flow_id) : flow_id(flow_id)
{
	
}

void localization::print_out(std::ostream &os) const
{
	os << "localization for flow " << this->flow_id << std::endl;
}

localization::~localization()
{
	
}


