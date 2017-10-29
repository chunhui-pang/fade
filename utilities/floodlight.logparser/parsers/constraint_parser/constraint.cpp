#include <parsers/constraint_parser/constraint.h>
#include <sstream>
#include <iomanip>

std::map<long, long> constraint::cookie2pkt;
std::map<long, long> constraint::cookie2dpid;
std::map<long, std::string> constraint::cookie2dst;

constraint::constraint(int flow_id, bool eval_result) : flow_id(flow_id), eval_result(eval_result), probe_cookies()
{
	
}

void constraint::add_probe_cookie(long cookie)
{
	for(int i = 0; i < this->probe_cookies.size(); i++)
		if (cookie == this->probe_cookies.at(i))
			return;
	this->probe_cookies.push_back(cookie);
}

void constraint::put_statistics(long cookie, long pkt_count)
{
	cookie2pkt.insert(std::make_pair(cookie, pkt_count));
}

void constraint::put_cookie_dpid(long cookie, long dpid)
{
	cookie2dpid.insert(std::make_pair(cookie, dpid));
}

void constraint::put_cookie_dst(long cookie, const std::string &dst)
{
	cookie2dst.insert(std::make_pair(cookie, dst));
}

void constraint::print_out(std::ostream &os) const
{
	os << "constraint for flow[id=";
	if (0 == this->flow_id)
		os << '-';
	else
		os << this->flow_id;
	os << "] evaluate to " << (eval_result ? "true" : "false") << ": " << std::endl;
	os << std::setw(10) << ' ';
	for (int i = 0; i < this->probe_cookies.size(); i++)
	{
		std::ostringstream oss;
		if (cookie2dpid.count(this->probe_cookies.at(i)) != 0)
			oss << 's' << cookie2dpid.at(this->probe_cookies.at(i));
		else
			oss << '*';
		oss << '[' << std::hex << this->probe_cookies.at(i) << ']';
		os << std::setw(25) << oss.str();
	}
	os << std::endl << std::setw(10) << ' ';
	for (int i = 0; i < this->probe_cookies.size(); i++)
	{
		if (cookie2dst.count(this->probe_cookies.at(i)) != 0)
			os << std::setw(25) << cookie2dst.at(this->probe_cookies.at(i));
		else
			os << std::setw(25) << '*';
	}
	os << std::endl << std::setw(10) << ' ';
	for (int i = 0; i < this->probe_cookies.size(); i++)
		if (cookie2pkt.count(this->probe_cookies.at(i)) == 0)
			os << std::setw(25) << '-';
		else
			os << std::setw(25) << cookie2pkt.at(this->probe_cookies.at(i));
	os << std::endl;
}

constraint::~constraint()
{
	
}
