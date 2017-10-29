#ifndef __GREEDY_FILL_INGRESS_H
#define __GREEDY_FILL_INGRESS_H

#include <vector>
#include <set>
#include <map>
#include <solver/heuristic_solver.h>
#include <model/pswitch.h>

class greedy_fill_ingress : public heuristic_solver
{
public:
	greedy_fill_ingress(const std::vector<pswitch*>& switches);

	virtual ~greedy_fill_ingress();
	
	virtual bool solve(const std::vector<aggregated_flow*>& flows, std::ostream& os);
	
private:
	static const int RESERVE_RATIO;
	std::set<pswitch*> switches;

	class aggregated_flow_comparator
	{
	public:
		bool operator() (const aggregated_flow* left, const aggregated_flow* right) const;
	};

	class run_status
	{
	public:
		run_status(const std::set<const pswitch*>& switches, int id);

		int get_id() const;
		int get_remaining_tcam(const pswitch* sw) const;
		bool add_detection(const aggregated_flow* af, int size);

		const std::map<const pswitch*, int>& get_tcam_usage() const;
		long get_total_assign_flows() const;
		
	private:
		int id;
		std::map<const pswitch*, int> remaining_tcam;
		std::map<const pswitch*, int> tcam_usage;
		std::map<const aggregated_flow*, int> assigned;
		long total_assign_flows; /* total single flow */
		int total_tcam_usage;  /* total tcam usage */
	};

	/**
	 * run start with 1
	 */
	class solve_status
	{
	public:
		solve_status(const std::set<const pswitch*>& switches, const std::vector<const aggregated_flow*>& flows);
		solve_status(const std::set<pswitch*>& switches, const std::vector<aggregated_flow*>& flows);
		
		int make_run_if_not_exist(int run);
		int get_remaining_size(const aggregated_flow* af) const;
		int get_remaining_tcam(int run_id, const pswitch* sw) const;
		bool add_detection(int run_id, const aggregated_flow* af, int size);

		int get_total_run() const;
		const std::map<const pswitch*, int>& get_tcam_usage(int run) const;
		int get_max_tcam_usage() const;
		int get_assign_flows(int run) const;
		
	private:
		std::map<const aggregated_flow*, int> remaining_size;
		std::vector<run_status> runs;

		const std::set<const pswitch*> switches;
	};

};
#endif
