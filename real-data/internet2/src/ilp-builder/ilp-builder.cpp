#include <iostream>
#include <iomanip>
#include <sstream>
#include <fstream>
#include <vector>
#include <map>

#include <parser/option_parser.h>
#include <parser/config_parser.h>
#include <model/pswitch.h>
#include <model/aggregated_flow.h>
#include <model/ilp_model.h>
#include <builder/ilp_model_builder.h>
#include <builder/gurobi_emitter.h>
#include <solver/heuristic_solver.h>
#include <solver/greedy_fill_ingress.h>

using namespace std;

int return_code = 0;
#define RETURN_WHEN_TRUE_WITH_FREE(val, msg, code) {	\
		if((val)){										\
			cerr << (msg) << endl;						\
			return_code = (code);						\
			goto free_resources;						\
		}												\
	}
#define FREE_IF_INITIALIZED(p) {				\
		if((p)) {								\
			delete (p);							\
		}										\
	}

void output_configs(const config_parser* config);

int main(int argc, char *argv[])
{
	option_parser parser(argc, argv);

	/* pointers cannot be defined after "goto" */
	config_parser* config = nullptr;
	ilp_model_builder* builder = nullptr;
	const ilp_model* model = nullptr;
	gurobi_emitter* emitter = nullptr;
	heuristic_solver* solver = nullptr;

	string err_msg = parser.parse_options();
	RETURN_WHEN_TRUE_WITH_FREE(err_msg.length() != 0, err_msg, 1);
	
	config = new config_parser(parser);
	err_msg = config->parse_configs();
	RETURN_WHEN_TRUE_WITH_FREE(err_msg.length() != 0, err_msg, 1);

	output_configs(config);
	
	builder = new ilp_model_builder(config);
	RETURN_WHEN_TRUE_WITH_FREE(!builder->build_model(), "fail to build ilp model!", 2);
	
	model = builder->get_built_model();
	RETURN_WHEN_TRUE_WITH_FREE(model == nullptr, "fail to build ilp model!", 2);
	
	emitter = new gurobi_emitter(model);
	RETURN_WHEN_TRUE_WITH_FREE(!emitter->generate_code(config->get_model_output_stream()), "fail to generate gurobi model!", 3);

	solver = new greedy_fill_ingress(config->get_switches());
	RETURN_WHEN_TRUE_WITH_FREE(!solver->solve(config->get_aggregated_flows(), config->get_solver_output_stream()), "find no solution with heuristic solver!", 4);

 free_resources:
	FREE_IF_INITIALIZED(emitter);
	FREE_IF_INITIALIZED(builder);
	FREE_IF_INITIALIZED(model);
	FREE_IF_INITIALIZED(config);
	FREE_IF_INITIALIZED(solver);

    return return_code;
}

void output_configs(const config_parser* config)
{
	cout << std::left;
	cout << "Configurations:" << endl;
	cout << "\t" << setw(20) << "maximum run:" << config->get_maximum_run() << endl;
	
	cout << "\t" << setw(20) << "switches:";
	for(const pswitch* psw : config->get_switches()){
		cout << setw(11) << psw->get_name();
	}
	cout << endl;
	
	cout << "\t" << setw(20) << "maximum tcam:";
	for(int max_tcam : config->get_maximum_tcams()){
		cout << setw(11) << max_tcam;
	}
	cout << endl;

	cout << "\t" << setw(20) << "number of flows:";
	map<pswitch*, int> avg_flow_size;
	map<pswitch*, int> avg_flow_len;
	for(pswitch* psw : config->get_switches()){
		cout << setw(11) << config->get_aggregated_flows_by_switch(psw).size();
		for(auto it = config->get_aggregated_flows_by_switch(psw).begin(); it != config->get_aggregated_flows_by_switch(psw).end(); it++){
			//for(const aggregated_flow* af : config->get_aggregated_flows_by_switch(psw)){
			auto af = *it;
			if(avg_flow_size.count(psw) == 0){
				avg_flow_size[psw] = af->get_size();
			} else {
				avg_flow_size[psw] = af->get_size() + avg_flow_size[psw];
			}
			
			if(avg_flow_len.count(psw) == 0){
				avg_flow_len[psw] = af->get_path().size();
			} else {
				avg_flow_len[psw] = af->get_path().size() + avg_flow_len[psw];
			}
		}
	}
	cout << endl;
	
	cout << "\t" << setw(20) << "avg flow size:";
	cout << std::fixed << setprecision(2);
	for(pswitch* psw : config->get_switches()){
		if(config->get_aggregated_flows_by_switch(psw).size() == 0){
			cout << setw(11) << '-';
		} else {
			cout << setw(11) << (avg_flow_size.count(psw) == 0 ? 0 : avg_flow_size.at(psw))/(float)config->get_aggregated_flows_by_switch(psw).size();			
		}

	}
	cout << endl;

	cout << "\t" << setw(20) << "avg path len:";
	for(pswitch* psw : config->get_switches()){
		if(config->get_aggregated_flows_by_switch(psw).size() == 0){
			cout << setw(11) << '-';
		} else {
			cout << setw(11) << (avg_flow_len.count(psw) == 0 ? 0 : avg_flow_len.at(psw))/(float)config->get_aggregated_flows_by_switch(psw).size();
		}
	}
	cout << endl;
}
