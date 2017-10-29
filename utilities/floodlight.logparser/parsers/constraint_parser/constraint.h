#ifndef __CONSTRAINT_H
#define __CONSTRAINT_H
#include <entity.h>
#include <map>
#include <vector>
#include <string>
class constraint : public entity
{
private:
	static std::map<long, long> cookie2pkt;
	static std::map<long, long> cookie2dpid;
	static std::map<long, std::string> cookie2dst;
	int flow_id;
	std::vector<long> probe_cookies;
	bool eval_result;
	
public:
	constraint(int flow_id, bool eval_result);

	void add_probe_cookie(long cookie);

	static void put_statistics(long cookie, long pkt_count);

	static void put_cookie_dpid(long cookie, long dpid);

	static void put_cookie_dst(long cookie, const std::string& dst);

	virtual void print_out(std::ostream& os) const;

	virtual ~constraint();
};
#endif
