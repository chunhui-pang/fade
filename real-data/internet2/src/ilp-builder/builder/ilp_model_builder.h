#ifndef __ILP_MODEL_BUILDER_H
#define __ILP_MODEL_BUILDER_H

#include <parser/config_parser.h>
#include <model/ilp_model.h>

/**
 * generate ilp model for this flow selection problem
 */
class ilp_model_builder
{
public:
	ilp_model_builder(config_parser* config);

	bool build_model();

	/* please free this model after used */
	const ilp_model* get_built_model();

private:
	config_parser* config;
	ilp_model* model;
	enum {BUILD_FINISHED, BUILDING} state;

	/* selects intermediate switches to probing */
	void gen_random_probes();
	/* gen variables */
	void gen_variables();
	/* gen cost function */
	void gen_cost_func();
	/* gen tcam usage constraints: \sum_{f \in F}i_{sf}n_{fr} + \sum_{f \in F}m_{sfr} = u_{sr} */
	void gen_tcam_usage_constraints();
	/* u_{sr} <= t_{s}, u_{sr} <= t_m */
	void gen_tcam_limit_constraints();
	/* gen flow size constriants */
	void gen_flow_size_constraints();
};
#endif
