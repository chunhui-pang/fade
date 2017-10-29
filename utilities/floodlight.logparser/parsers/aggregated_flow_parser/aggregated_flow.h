#ifndef __AGGREGATED_FLOW_H
#define __AGGREGATED_FLOW_H

#include <entity.h>
#include <vector>
#include <set>
#include <string>

class aggregated_flow : public entity
{
private:
	std::vector< std::set<std::string> > dsts;
	std::vector< std::vector<int> > paths;
	std::vector< std::vector<int> > ports;
	int current_idx;
	
public:
	aggregated_flow();

	void start_new_flow();

	bool add_dst(const std::string& dst);

	void add_switch_node(int dpid, int port);

	virtual void print_out(std::ostream& os) const;

	virtual ~aggregated_flow();
};
#endif
