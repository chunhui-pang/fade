#ifndef __AGGREGATED_FLOW_H
#define __AGGREGATED_FLOW_H

#include <vector>
#include <random>
#include <model/pswitch.h>

/**
 * aggregated flow (immutable)
 * Main attributes: id, size, path, intermediate switches.
 * run starts with 0, probes include the first node and the last node
 */
class aggregated_flow
{
public:
	aggregated_flow(int size, const std::vector<const pswitch*>& path);

	static int generate_next_id();

	int get_id() const;

	int get_size() const;

	const std::vector<const pswitch*>& get_path() const;

	const std::vector< std::vector<const pswitch*> >& get_probes() const;
	const std::vector<const pswitch*>& get_probes(int run) const;
	/* set all probes for all run */
	aggregated_flow* set_probes(const std::vector< std::vector<const pswitch*> >& probes);
	/* insert probes for a given run */
	aggregated_flow* add_probes(const std::vector<const pswitch*>& probe, int run);
	/* generate random probes for a given run */
	aggregated_flow* generate_random_probes(int run);
	/* remove probes for a given run */
	aggregated_flow* remove_probes(int run);
	
private:
	static int next_id;
	static const int* probe_lens;
	int id;
	int size;
	std::vector<const pswitch*> path;
	std::vector< std::vector<const pswitch*> > probes;

	std::default_random_engine random_engine;
	std::uniform_int_distribution<int> generator;
};
#endif
