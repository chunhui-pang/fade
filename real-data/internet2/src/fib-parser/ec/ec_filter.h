#ifndef __EC_FILTER_H
#define __EC_FILTER_H

#include <ec/equivalent_class.h>
#include <topo/topology.h>
#include <topo/rule_graph.h>

namespace ns_ec
{
	class ec_filter
	{
	public:
		
		ec_filter();

		/**
		 * rewrite the ec
		 */
		virtual equivalent_class* filter(equivalent_class* ec) = 0;

		/**
		 * deconstructor
		 */
		virtual ~ec_filter();
		
	};
}
#endif
