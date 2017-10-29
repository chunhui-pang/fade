#ifndef __EC_EMITTER_H
#define __EC_EMITTER_H

#include <ec/equivalent_class.h>
#include <ec/ec_filter.h>
#include <topo/topology.h>
#include <topo/rule_graph.h>
#include <vector>

namespace ns_ec
{
	class ec_emitter
	{
	private:
		std::vector<ec_filter*> filters;
		
	public:
		ec_emitter();

		virtual ec_emitter* add_ec_filter(ec_filter* filter);

		virtual equivalent_class* do_filter(equivalent_class* ec);

		virtual bool emit(equivalent_class* ec) = 0;
		
		virtual ~ec_emitter();
	};
}
#endif
