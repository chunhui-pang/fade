#ifndef __ANOMALY_H
#define __ANOMALY_H

#include <entity.h>

class anomaly : public entity
{
private:
	int dpid;
	int flow_id;
	std::string dst;

public:
	anomaly(int dpid, int flow_id, const std::string& dst);

	virtual void print_out(std::ostream& os) const;

	virtual ~anomaly();
};
#endif
