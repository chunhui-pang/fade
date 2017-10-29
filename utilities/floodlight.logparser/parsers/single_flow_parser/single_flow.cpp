#include <parsers/single_flow_parser/single_flow.h>

#include <sstream>
#include <iomanip>

single_flow::single_flow()
{
	
}

bool single_flow::add_flow(const std::string& dst, flow_path path)
{
	if (0 == this->flows.count(dst))
	{
		this->flows.insert( std::make_pair(dst, std::vector< flow_path >()));
	}
	this->flows.at(dst).push_back(path);
	return true;
}

void single_flow::print_out(std::ostream &os) const
{
	os << "single flows:" << std::endl;
	for (std::map<std::string, std::vector< flow_path > >::const_iterator it = this->flows.begin(); it != this->flows.end(); it++)
	{
		std::ostringstream oss;
		oss << (it->first) << ": ";
		std::string header = oss.str();
		for (std::vector< flow_path >::const_iterator fit = it->second.begin(); fit != it->second.end(); fit++)
		{
			if (fit == it->second.begin())
			{
				os << header;
			}
			else
			{
				std::streamsize old_width = os.width();
				os << std::setw(header.size()) << ' ' << std::setw(old_width);
			}
			this->print_flow_path(os, (*fit));
		}
	}
}

single_flow::~single_flow()
{
	
}

void single_flow::print_flow_path(std::ostream &os, flow_path path) const
{
	flow_path::const_iterator  it = path.begin();
	while (it != path.end())
	{
		if (it != path.begin())
		{
			os << ", ";
		}
		os << (*it);
		it++;
	}
	os << std::endl;
}
