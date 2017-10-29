#ifndef __LEN_EC_FILTER_H
#define __LEN_EC_FILTER_H

#include <ec/ec_filter.h>

namespace ns_ec
{
	/**
	 * filter out all ec whose length is shorter than a threshold
	 */
	class len_ec_filter : public ec_filter
	{
	private:
		int min_len;
		
	public:
		len_ec_filter(int min_len=3);
		
		virtual equivalent_class* filter(equivalent_class* ec);
		
		~len_ec_filter();
	};
}

#endif
