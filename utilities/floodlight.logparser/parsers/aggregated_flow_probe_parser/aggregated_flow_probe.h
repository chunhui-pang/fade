#ifndef __AGGREGATED_FLOW_PROBE_H
#define __AGGREGATED_FLOW_PROBE_H

/**
 * copy from single_flow_probe.h
 */
#include <vector>
#include <entity.h>

class aggregated_flow_probe : public entity
{
private:
	int flow_id;
	std::string dst;
	std::vector<int> dpids;
	std::vector<bool> is_probe;
	
public:
	aggregated_flow_probe(int flow_id, const std::string& dst);

	void append_switch(int dpid);

	bool append_probe(int dpid);

	virtual void print_out(std::ostream& os) const;

	virtual ~aggregated_flow_probe();
};
#endif
