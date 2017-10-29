#include <parsers/aggregated_flow_probe_parser/aggregated_flow_probe.h>

aggregated_flow_probe::aggregated_flow_probe(int flow_id, const std::string& dst) : flow_id(flow_id), dst(dst), dpids(), is_probe()
{
	
}

void aggregated_flow_probe::append_switch(int dpid)
{
	this->dpids.push_back(dpid);
	this->is_probe.push_back(false);
}

bool aggregated_flow_probe::append_probe(int dpid)
{
	int idx = -1;
	for(int i = 0; i < this->dpids.size(); i++)
	{
		if (dpid == this->dpids.at(i))
		{
			idx = i;
			break;
		}
	}
	if (-1 == idx || this->is_probe.at(idx))
		return false;
	this->is_probe.at(idx) = true;
	return true;
}

void aggregated_flow_probe::print_out(std::ostream &os) const
{
	os << "generate probes for flow[id=" << this->flow_id << ", dst=" << this->dst << "]: ";
	for(int i = 0; i < this->dpids.size(); i++)
	{
		if ( i == this->dpids.size() - 1 || this->is_probe.at(i))
			os << '[' << this->dpids.at(i) << ']';
		else
			os << this->dpids.at(i);
		os << "  ";
	}
	os << std::endl;
}


aggregated_flow_probe::~aggregated_flow_probe()
{
	
}

