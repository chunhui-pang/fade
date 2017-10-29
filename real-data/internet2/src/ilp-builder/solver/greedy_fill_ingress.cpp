#include <solver/greedy_fill_ingress.h>
#include <set>
#include <map>
#include <algorithm>
#include <iomanip>
#include <sstream>
#include <stdexcept>

const int greedy_fill_ingress::RESERVE_RATIO = 2;
greedy_fill_ingress::greedy_fill_ingress(const std::vector<pswitch*>& switches) : switches(switches.begin(), switches.end())
{
	
}

greedy_fill_ingress::~greedy_fill_ingress()
{
	
}

bool greedy_fill_ingress::solve(const std::vector<aggregated_flow*>& flows, std::ostream &os)
{
	/* sort all flows */
	std::set<aggregated_flow*, aggregated_flow_comparator> sorted_flows;
	for(std::vector<aggregated_flow*>::const_iterator it = flows.begin(); it != flows.end(); it++)
		sorted_flows.insert(*it);
	/* initialize remaining tcam */
	solve_status status(switches, flows);
	std::set<aggregated_flow*, aggregated_flow_comparator>::const_iterator it = sorted_flows.begin();
	while(it != sorted_flows.end())
	{
		int run_id = 1;
		int remaining = 0;
		while((remaining = status.get_remaining_size(*it)) != 0) {
			run_id = status.make_run_if_not_exist(run_id);
			const pswitch* _1st_node = nullptr;
			try {
				_1st_node = (*it)->get_probes(run_id-1).at(0);
			} catch (const std::logic_error& error) {
				return false;
			}
			int free_tcam = status.get_remaining_tcam(run_id, _1st_node);
			if(free_tcam > RESERVE_RATIO * this->switches.size()) {
				int avail = free_tcam - RESERVE_RATIO * this->switches.size();
				int assign = remaining > avail ? avail : remaining;
				status.add_detection(run_id, *it, assign);
			}
			run_id++;
		}
		it++;
	}
	std::ios::fmtflags osflags = os.flags();
	os << std::setw(20) << "Total run: "         << status.get_total_run() << std::endl;
	os << std::setw(20) << "Max tcam usage: "    << status.get_max_tcam_usage() << std::endl;
	for(int run_id = 1; run_id <= status.get_total_run(); run_id++) {
		os << "============== run " << std::setw(3) << run_id << " ===============" << std::endl;
		os << std::setw(20) << "Assign flows: " << status.get_assign_flows(run_id) << std::endl;
		std::ostringstream oss;
		os << std::setw(15) <<  "Switch name: ";
		oss << std::setw(15) << "tcam usage: ";
		for(std::set<pswitch*>::iterator sit = this->switches.begin(); sit != this->switches.end(); sit++) {
			os << std::setw(12) << (*sit)->get_name();
			oss << std::setw(12) << status.get_tcam_usage(run_id).at(*sit);
		}
		os << std::endl;
		os << oss.str() << std::endl;
	}
	os.flags(osflags);
	return true;
}

bool greedy_fill_ingress::aggregated_flow_comparator::operator() (const aggregated_flow* left, const aggregated_flow* right) const
{
	if( left->get_size() != right->get_size() )
		return left->get_size() > right->get_size();
	if(left->get_id() != right->get_id())
		return left->get_id() > right->get_id();
	return true;
}

////////////////////////////// run_status /////////////////////////////////////////
greedy_fill_ingress::run_status::run_status(const std::set<const pswitch*>& switches, int id) : id(id), remaining_tcam(), tcam_usage(), assigned(), total_assign_flows(0), total_tcam_usage(0)
{
	for(std::set<const pswitch*>::const_iterator it = switches.begin(); it != switches.end(); it++) {
		this->remaining_tcam.insert(std::make_pair(*it, (*it)->get_max_tcam()));
		this->tcam_usage.insert(std::make_pair(*it, 0));
	}
}

int greedy_fill_ingress::run_status::get_id() const
{
	return this->id;
}

int greedy_fill_ingress::run_status::get_remaining_tcam(const pswitch *sw) const
{
	return this->remaining_tcam.at(sw);
}

bool greedy_fill_ingress::run_status::add_detection(const aggregated_flow *af, int size)
{
	/* check if there is enough resources */
	const std::vector<const pswitch*> probes = af->get_probes(this->id-1);
	std::vector<const pswitch*>::const_iterator it = probes.begin();
	if(this->get_remaining_tcam(*it) < size)  // $size$ DFR would be install to the first node
		return false;
	while(++it != probes.end())
		if(this->get_remaining_tcam(*it) < 1)  // 1 DFR would be install to other probes(node)
			return false;
	/* ready */
	it = probes.begin();
	this->remaining_tcam.at(*it) -= size;
	this->tcam_usage.at(*it) += size;
	while(++it != probes.end()) {
		this->remaining_tcam.at(*it) -= 1;
		this->tcam_usage.at(*it) += 1;
	}
	if(this->assigned.count(af) != 0)
		this->assigned.at(af) += size;
	else
		this->assigned.insert(std::make_pair(af, size));
	this->total_tcam_usage += (size + probes.size() - 1);
	this->total_assign_flows += size;
	return true;
}

const std::map<const pswitch*, int>& greedy_fill_ingress::run_status::get_tcam_usage() const
{
	return this->tcam_usage;
}

long greedy_fill_ingress::run_status::get_total_assign_flows() const
{
	return this->total_assign_flows;
}

///////////////////////////////////////////////////////////////////////////////////

////////////////////////////// solve_status ///////////////////////////////////////
greedy_fill_ingress::solve_status::solve_status(const std::set<const pswitch*>& switches, const std::vector<const aggregated_flow*>& flows) :
	remaining_size(), runs(), switches(switches)
{
	for(std::vector<const aggregated_flow*>::const_iterator it = flows.begin(); it != flows.end(); it++)
		this->remaining_size.insert(std::make_pair(*it, (*it)->get_size()));
}

greedy_fill_ingress::solve_status::solve_status(const std::set<pswitch*>& switches, const std::vector<aggregated_flow*>& flows) :
	remaining_size(), runs(), switches(switches.begin(), switches.end())
{
	for(std::vector<aggregated_flow*>::const_iterator it = flows.begin(); it != flows.end(); it++)
		this->remaining_size.insert(std::make_pair(*it, (*it)->get_size()));
}

int greedy_fill_ingress::solve_status::make_run_if_not_exist(int run)
{
	for(std::vector<run_status>::iterator it = this->runs.begin(); it != this->runs.end(); it++) {
		if (it->get_id() == run)
			return it->get_id();
		if (it->get_id() > run) {
			this->runs.insert(it, run_status(this->switches, run));
			return run;
		}
	}
	this->runs.push_back(run_status(this->switches, run));
	return run;
}

int greedy_fill_ingress::solve_status::get_remaining_size(const aggregated_flow *af) const
{
	if(this->remaining_size.count(af) == 0)
		return 0;
	return remaining_size.at(af);
}

int greedy_fill_ingress::solve_status::get_remaining_tcam(int run_id, const pswitch *sw) const
{
	for(std::vector<run_status>::const_iterator it = this->runs.begin(); it != this->runs.end(); it++) {
		if(it->get_id() == run_id)
			return it->get_remaining_tcam(sw);
	}
	return 0;
}

bool greedy_fill_ingress::solve_status::add_detection(int run_id, const aggregated_flow *af, int size)
{
	if(this->remaining_size.at(af) < size)
		return false;
	for(std::vector<run_status>::iterator it = this->runs.begin(); it != this->runs.end(); it++) {
		if(it->get_id() == run_id) {
			if(it->add_detection(af, size)) {
				this->remaining_size.at(af) -= size;
				return true;
			}
			return false;
		}
	}
	return false;
}

int greedy_fill_ingress::solve_status::get_total_run() const
{
	return this->runs.size();
}

const std::map<const pswitch*, int>& greedy_fill_ingress::solve_status::get_tcam_usage(int run) const
{
	for(std::vector<run_status>::const_iterator it = this->runs.begin(); it != this->runs.end(); it++) {
		if(it->get_id() == run)
			return it->get_tcam_usage();
	}
	throw std::logic_error("this run hasn't been created.");
}

int greedy_fill_ingress::solve_status::get_max_tcam_usage() const
{
	int max_tcam_usage = 0;
	for(std::vector<run_status>::const_iterator it = this->runs.begin(); it != this->runs.end(); it++) {
		for(std::map<const pswitch*, int>::const_iterator uit = it->get_tcam_usage().begin(); uit != it->get_tcam_usage().end(); uit++){
			if(max_tcam_usage < uit->second)
				max_tcam_usage = uit->second;
		}
	}
	return max_tcam_usage;
}

int greedy_fill_ingress::solve_status::get_assign_flows(int run) const
{
	for(std::vector<run_status>::const_iterator it = this->runs.begin(); it != this->runs.end(); it++) {
		if(it->get_id() == run)
			return it->get_total_assign_flows();
	}
	return 0;
}

///////////////////////////////////////////////////////////////////////////////////
