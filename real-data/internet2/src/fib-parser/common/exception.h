#ifndef __EXCEPTION_H
#define __EXCEPTION_H
#include <stdexcept>
#include <string>

namespace ns_common {
	class invalid_element : std::logic_error
	{
	public:
		invalid_element(std::string msg);
	};

}



#endif
