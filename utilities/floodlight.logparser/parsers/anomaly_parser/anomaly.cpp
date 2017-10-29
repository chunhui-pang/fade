#include <parsers/anomaly_parser/anomaly.h>
#include <iomanip>
anomaly::anomaly(int dpid, int flow_id, const std::string& dst) : dpid(dpid), flow_id(flow_id), dst(dst)
{
	
}

void anomaly::print_out(std::ostream &os) const
{
	os << "anomaly [dst=" << this->dst << ", switch=s" << this->dpid << ", flow_id=" << this->flow_id << "]" << std::endl;
}

anomaly::~anomaly()
{
	
}

