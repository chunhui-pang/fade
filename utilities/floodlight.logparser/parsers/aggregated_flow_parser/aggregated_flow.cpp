#include <parsers/aggregated_flow_parser/aggregated_flow.h>
#include <sstream>
#include <iomanip>

aggregated_flow::aggregated_flow() : dsts(), paths(), ports(), current_idx(-1)
{
	
}

void aggregated_flow::start_new_flow()
{
	this->current_idx++;
	this->dsts.push_back(std::set<std::string>());
	this->paths.push_back(std::vector<int>());
	this->ports.push_back(std::vector<int>());
}

bool aggregated_flow::add_dst(const std::string &dst)
{
	return this->dsts.at(this->current_idx).insert(dst).second;
}

void aggregated_flow::add_switch_node(int dpid, int port)
{
	this->paths.at(this->current_idx).push_back(dpid);
	this->ports.at(this->current_idx).push_back(port);
}

void aggregated_flow::print_out(std::ostream &os) const
{
	os << "aggregated flows: " << std::endl;
	for(int id = 0; id <= current_idx; id++)
	{
		os << std::setw(10) << ' ';
		std::ostringstream oss;
		int i = 0;
		for(std::set<std::string>::const_iterator it = this->dsts.at(id).begin(); it != this->dsts.at(id).end(); it++, i++)
		{
			oss << (*it);
			if (i != this->dsts.at(id).size()-1)
				oss << ", ";
		}
		os << "AggregatedFlow[" << oss.str() << "]: ";
		for(i = 0; i < this->paths.at(id).size(); i++)
		{
			os << this->paths.at(id).at(i) << ':' << this->ports.at(id).at(i);
			if (i != this->paths.at(id).size()-1)
				os << ", ";
		}
		os << std::endl;
	}
}

aggregated_flow::~aggregated_flow()
{
	
}
   
