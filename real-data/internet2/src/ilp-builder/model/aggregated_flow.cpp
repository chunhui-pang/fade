#include <model/aggregated_flow.h>

#include <algorithm>
#include <set>
#include <random>
#include <stdexcept>

int aggregated_flow::next_id = 1;
/* idx starts from 0. path len[] = {0, 1, 2, 3, 4, } */
static const int tmp_probe_lens[] = {0, 0, 0, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6};
const int* aggregated_flow::probe_lens = tmp_probe_lens;

aggregated_flow::aggregated_flow(int size, const std::vector<const pswitch*>& path) : id(generate_next_id()), size(size), path(path.begin(), path.end()), probes(),
																				random_engine(time(NULL)), generator(1, path.size()-1)

{
	
}

int aggregated_flow::generate_next_id()
{
	return next_id++;
}

int aggregated_flow::get_id() const
{
	return this->id;
}

int aggregated_flow::get_size() const
{
	return this->size;
}

const std::vector<const pswitch*>& aggregated_flow::get_path() const
{
	return this->path;
}

const std::vector< std::vector<const pswitch*> >& aggregated_flow::get_probes() const
{
	return this->probes;
}

const std::vector<const pswitch*>& aggregated_flow::get_probes(int run) const
{
	if(run >= this->probes.size()){
		throw std::logic_error("probes for run " + std::to_string(run) + " hasn't been generated!");
	} else {
		return this->probes.at(run);
	}
}

aggregated_flow* aggregated_flow::set_probes(const std::vector<std::vector<const pswitch *> > &probes)
{
	this->probes = probes;
	return this;
}

aggregated_flow* aggregated_flow::add_probes(const std::vector<const pswitch*> &probe, int run)
{
	auto it = this->probes.begin();
	for(; run != 0 && it != this->probes.end(); run--, it++);
	this->probes.insert(it, probe);
	return this;
}

aggregated_flow* aggregated_flow::generate_random_probes(int run)
{
	std::vector<const pswitch*> probe;
	int probe_len = probe_lens[this->path.size()];
	std::set<int> selected_idx;
	/* selected the first node, last node */
	selected_idx.insert(0);
	selected_idx.insert(this->path.size()-1);
	probe_len -= 2;
	while(probe_len != 0){
		int idx = generator(random_engine);
		probe_len -= (selected_idx.insert(idx).second == true ? 1 : 0);
	}
	for(int idx : selected_idx){
		probe.push_back(this->path[idx]);
	}
	this->add_probes(probe, run);
	return this;
}

aggregated_flow* aggregated_flow::remove_probes(int run)
{
	auto it = this->probes.begin();
	for(; run != 1 && it != this->probes.end(); run--, it++);
	if(it != this->probes.end()){
		this->probes.erase(it);
	}
	return this;
}
