#include <iostream>
#include <fstream>
#include <sstream>
#include <vector>
#include <regex>
#include <iomanip>
#include <set>
#include <iomanip>
#include <map>
#include <list>

#include <pthread.h>
#include <unistd.h>
#include <sched.h>
#include <time.h>

#include <network/pswitch.h>
#include <network/rule.h>
#include <network/interface.h>
#include <topo/topology.h>
#include <topo/rule_graph.h>
#include <common/util.h>
#include <common/exception.h>
#include <parser/option_parser.h>
#include <parser/boost_option_parser.h>
#include <parser/juniper_interface_parser.h>
#include <parser/juniper_rule_parser.h>
#include <ec/switch_rule_analyzer.h>
#include <ec/info_file_ec_emitter.h>
#include <ec/len_ec_filter.h>
#include <ec/mininet_conf_ec_emitter.h>


using namespace std;
using namespace ns_network;
using namespace ns_common;
using namespace ns_topo;
using namespace ns_parser;
using namespace ns_ec;

void construct_rule_graph(rule_graph& graph, const topology& topo);
void output_slices_and_paths(const rule_graph& graph, const topology& topo);
void* parse_switch_thread_func(void*);

#define OUTPUT_DELIMITER "=============================================================================="

class pswitch_comparator
{
public:
	bool operator () (const pswitch* lhs, const pswitch* rhs)
	{
		return lhs->get_name() < rhs->get_name();
	}
};

int main(int argc, char *argv[])
{
	option_parser* opt_parser = option_parser::get_instance(new boost_option_parser(argc, argv));
	if(true == opt_parser->print_error_or_help_msg(std::cout)){
		return 1;
	}
	opt_parser->print_configurations(std::cout);
	const bool show_warnings = opt_parser->get_show_warnings();
	const map<string, pair<string, string> > switch_confs = opt_parser->get_switch_configurations();

	pthread_barrier_t barrier;
	pthread_mutex_t lock;
	if(pthread_barrier_init(&barrier, NULL, switch_confs.size()+1) != 0){/* add one for current thread also should wait */
		perror("cannot create barrier...");
		exit(1);
	}
	if(pthread_mutex_init(&lock, NULL) != 0)
		{
			perror("cannot create mutex lock...");
			exit(1);
		}
	
	topology topo;

	for(auto it = switch_confs.begin(); it != switch_confs.end(); it++){
		void** params = new void*[5];
		params[0] = (void*)(&(it->first));
		params[1] = (void*)(&(it->second));
		params[2] = (void*)(&topo);
		params[3] = (void*)(&barrier);
		params[4] = (void*)(&lock);
		pthread_t pid;
		pthread_create(&pid, NULL, parse_switch_thread_func, (void*)params);
	}
	pthread_barrier_wait(&barrier);
	pthread_barrier_destroy(&barrier);
	pthread_mutex_destroy(&lock);
	cout << "\t\tDONE  ^_^" << endl;

	int total[2] = {0};
	cout << std::left << setw(15) << "switch" << setw(12) << "interfaces" << setw(12) << "rules" << endl;
	set<pswitch*, pswitch_comparator> all_sw(topo.get_switches().begin(), topo.get_switches().end(), pswitch_comparator());
	for(const pswitch* sw : all_sw){
		total[0] += sw->get_interfaces().size();
		total[1] += sw->get_rules().size();
		cout << setw(15) << sw->get_name() << setw(12) << sw->get_interfaces().size() << setw(12) << sw->get_rules().size() << endl;
	}
	cout << setw(15) << "sum" << setw(12) << total[0] << setw(12) << total[1] << '[' << topo.get_links().size() << " links]" << endl;
	set< pair<const pswitch*, const pswitch*> > unique_links;
	for(auto it = topo.get_links().begin(); it != topo.get_links().end(); it++)
		{
			// cout << it->first->get_switch()->get_name() << ':' << it->first->get_name() << "  -------  "
			// 	 << it->second->get_switch()->get_name() << ':' << it->second->get_name() << endl;
			unique_links.insert(make_pair(it->first->get_switch(), it->second->get_switch()));
		}
	// cout << unique_links.size();
	// for(auto it = unique_links.begin(); it != unique_links.end();it++)
	// 	{
	// 		cout << it->first->get_name() << "  ----  " << it->second->get_name() << endl;
	// 	}
	cout << OUTPUT_DELIMITER << endl;

	rule_graph graph;
	graph.parse_topology(&topo);
	const set<pswitch*>& sws = topo.get_switches();
	cout << graph << endl;
	output_slices_and_paths(graph, topo);
	delete opt_parser;
}

void* parse_switch_thread_func(void* args)
{
    /* parse parameters */
    void** params = (void**)args;
    string name = *(string*)(params[0]);
    pair<string,string> confs = *(pair<string, string>*)(params[1]);
    topology* topo = (topology*)(params[2]);
    pthread_barrier_t* barrier = (pthread_barrier_t*)params[3];
	pthread_mutex_t* lock = (pthread_mutex_t*)params[4];
    delete []params;

    pswitch* sw = new pswitch();
    sw->set_name(name);
    ifstream f_intf(confs.first);
    interface_parser* intf_parser = new juniper_interface_parser(f_intf);
    interface* intf = nullptr;
    while((intf = intf_parser->get_next()) != nullptr){
		sw->add_interface(intf);
    }
    delete intf_parser;
    ifstream f_fib(confs.second);
    rule_parser* rule_parser = new juniper_rule_parser(f_fib, sw);
    rule* rule = nullptr;
    while((rule = rule_parser->get_next()) != nullptr){
		sw->add_rule(rule);
    }
    delete rule_parser;
    cout << sw->get_name() << "  ";
	cout.flush();
	pthread_mutex_lock(lock);
    topo->add_switch(sw);
	pthread_mutex_unlock(lock);

    pthread_barrier_wait(barrier);
    return NULL;
}

void output_slices_and_paths(const rule_graph& rg, const topology& topo)
{
	set<pswitch*, pswitch_comparator> all_sw(topo.get_switches().begin(), topo.get_switches().end(), pswitch_comparator());
	map<const pswitch*, multimap<unsigned int, list<const pswitch*> > > details;
	cout << setw(15) << "Name" << setw(12) << "used rules" << setw(12) << "total_slices" << setw(12) << "avg_slices" << setw(12) << "avg_paths" << endl;
	int slice_id = 0;
	string output_filename = option_parser::get_instance()->get_output_slice_info();
	info_file_ec_emitter* info_emitter = nullptr;
	mininet_conf_ec_emitter* mininet_emitter = nullptr;
	ec_filter* len_filter = nullptr;
	if(output_filename.size() != 0)
		info_emitter = new info_file_ec_emitter(output_filename);
	if(option_parser::get_instance()->get_output_minient_host_config().size() != 0 &&
	   option_parser::get_instance()->get_output_mininet_topo_config().size() != 0 &&
	   option_parser::get_instance()->get_output_minient_rules_config().size() != 0)
		{
			mininet_emitter = new mininet_conf_ec_emitter(
														  option_parser::get_instance()->get_output_mininet_topo_config(),
														  option_parser::get_instance()->get_output_minient_host_config(),
														  option_parser::get_instance()->get_output_minient_rules_config(),
														  &topo, &rg);
			len_filter = new len_ec_filter(3);
			mininet_emitter->add_ec_filter(len_filter);
		}

	for(pswitch* psw : all_sw){
		switch_rule_analyzer analyzer(psw, &rg);
		analyzer.analyze();
		const list< set<const rule*> >& slices = analyzer.get_result_slices();
		const list< list<const pswitch*> >& paths = analyzer.get_result_paths();
		const vector<equivalent_class*> ecs = analyzer.get_equivalent_classes();
		if(nullptr != info_emitter)
			for(auto it = ecs.begin(); it != ecs.end(); it++)
				info_emitter->emit(*it);
		if(nullptr != mininet_emitter)
			for(auto it = ecs.begin(); it != ecs.end(); it++)
				if(!mininet_emitter->emit(*it))
					cerr << "output ec to mininet configuration fails." << endl;
		double used_rules = 0, total_path_len = 0;
		//list< pair<unsigned int, list<const pswitch*> > > tmp;
		multimap<unsigned int, list<const pswitch*> > tmp;
		auto it1 = slices.begin();
		auto it2 = paths.begin();
		for(; it1 != slices.end() && it2 != paths.end(); it1++, it2++){
			tmp.insert(make_pair(it1->size(), *it2));
			//tmp.push_back(make_pair(it1->size(), *it2));
			used_rules += it1->size();
			total_path_len += it2->size();
		}
		details.insert(make_pair(psw, tmp));
		cout << setw(15) << psw->get_name() << setw(12) << used_rules << setw(12) << slices.size() << setw(12) << used_rules/slices.size() << setw(12) << total_path_len/paths.size() <<endl;
	}
	cout << OUTPUT_DELIMITER << endl;
	
	cout << setw(15) << "Name" << setw(12) << "id" << setw(12) << "size" << setw(12) << "path len" << "path" <<endl;
	for(pswitch* psw : all_sw){
		int id = 1;
		multimap<unsigned int, list<const pswitch*> >& slices = details.at(psw);
		for(auto it = slices.begin(); it != slices.end(); it++){
			cout << setw(15) << psw->get_name() << setw(12) << (id++) << setw(12) << it->first << setw(12) << it->second.size();
			for(const pswitch* sw : it->second){
				cout << sw->get_name() << " ";
			}
			cout << endl;
		}
	}
	if(nullptr != info_emitter)
		delete info_emitter;
	if(nullptr != mininet_emitter)
		delete mininet_emitter;
	if(nullptr != len_filter)
		delete len_filter;
}
	      
