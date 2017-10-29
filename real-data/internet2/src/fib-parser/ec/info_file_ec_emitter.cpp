#include <ec/info_file_ec_emitter.h>

#include <list>
#include <sstream>
#include <network/pswitch.h>

namespace ns_ec
{
	const std::string info_file_ec_emitter::INDENTION_STR = "    ";
	info_file_ec_emitter::info_file_ec_emitter(const std::string& filename, int start_slice_id) : os(filename), slice_id(start_slice_id)
	{
		
	}

	bool info_file_ec_emitter::emit(ns_ec::equivalent_class *ec)
	{
		if(os.fail())
			return false;
		ec = this->do_filter(ec);
		if(nullptr != ec)
		{
			const std::list<const ns_network::pswitch*>& path = ec->get_path();
			os << "slice " << (slice_id++) << ": ";
			std::ostringstream path_oss;
			std::ostringstream len_oss;
			for(auto pit = path.begin(); pit != path.end(); pit++)
			{
				path_oss << (*pit)->get_name() << ", ";
				len_oss << ec->get_rules(*pit).size() << ", ";
			}
			os << "path=[" << path_oss.str().substr(0, path_oss.str().size()-2) << "], ";
			os << "size=[" << len_oss.str().substr(0, len_oss.str().size()-2) << "]" << std::endl;
			auto pit = path.begin();
			while(pit != path.end())
			{
				const std::set<const rule*> rules = ec->get_rules(*pit);
				os << INDENTION_STR << (*pit)->get_name() << " [size=" << rules.size() << "]:" << std::endl;
				for(auto rit = rules.begin(); rit != rules.end(); rit++)
				{
					os << INDENTION_STR << INDENTION_STR << *(*rit) << std::endl;
				}
				pit++;
			}
		}
		return true;
	}

	info_file_ec_emitter::~info_file_ec_emitter()
	{
		os.close();
	}
	
}
