#ifndef __INFO_FILE_EC_EMITTER_H
#define __INFO_FILE_EC_EMITTER_H
#include <ec/ec_emitter.h>
#include <string>
#include <fstream>

namespace ns_ec
{
	class info_file_ec_emitter : public ec_emitter
	{
	private:
		static const std::string INDENTION_STR;
		std::ofstream os;
		int slice_id;
		
	public:
		info_file_ec_emitter(const std::string& filename, int start_slice_id=1);

		virtual bool emit(equivalent_class* ec);

		~info_file_ec_emitter();
	};
}
#endif
