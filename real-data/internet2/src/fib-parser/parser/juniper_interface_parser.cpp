#include <string>
#include <regex>

#include <parser/juniper_interface_parser.h>
#include <parser/option_parser.h>
#include <common/util.h>

namespace ns_parser
{
	using std::regex;
	using std::smatch;
	using std::string;

	const regex juniper_interface_parser::intf_start = regex("\\W*(Physical|Logical)\\W+interface:?\\W*([[:print:]]+?),?[[:blank:]]+.*");
	const regex juniper_interface_parser::intf_ipv4_addr = regex("\\W*(Destination:\\W*([0-9.]+)(/([0-9]{1,2}))?,\\W*)?Local:\\W*([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}).*");

	juniper_interface_parser::juniper_interface_parser(istream& is) : interface_parser(is)
	{
		this->id = 0;
		this->has_name = false;
		this->has_ip = false;
		this->current = nullptr;
	}

	juniper_interface_parser::~juniper_interface_parser()
	{
		
	}

	void juniper_interface_parser::report_error() const
	{
		if(this->current != nullptr && option_parser::get_instance()->get_show_unknown_interfaces()){
			std::cout << "Unrecongized interface: '" << this->current->get_name() << std::endl;
		}
	}

	interface* juniper_interface_parser::get_next()
	{
		string line;
		smatch match;
		while(getline(is, line)){
			if(line.size() == 0){
				if(this->has_name && this->has_ip){
					return current;
				}else{
					report_error();
					if(this->current != nullptr){
						delete this->current;
						this->current = nullptr;
					}
				}
				this->has_name = false;
				this->has_ip = false;
			}else{
				if(std::regex_match(line, match, intf_start)){
					this->current = new interface();
					this->current->set_id(++id);
					this->current->set_name(match[2].str());
					this->has_name = true;
				}else if(std::regex_match(line, match, intf_ipv4_addr)){
					unsigned int local_addr = ns_common::str2ip(match[5].str().c_str());
					unsigned int destination = local_addr;
					unsigned int mask_len = 32;
					if("" != match[2].str()){
						destination = ns_common::str2ip(match[2].str().c_str());
						if("" != match[4].str()){
							mask_len = atoi(match[4].str().c_str());
						}
					}
					if(this->has_name == true){
						this->current->add_addr(destination, mask_len, local_addr);
						this->has_ip = true;
					}
				}
			}
		}
		return nullptr;
	}

}
