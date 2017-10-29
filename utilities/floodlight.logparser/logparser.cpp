#include <string>
#include <iostream>
#include <fstream>
#include <vector>
#include <set>
#include <iterator>
#include <regex>

#include <dlfcn.h>
#include <unistd.h>
#include <sys/types.h>
#include <dirent.h>

#include <ctype.h>
#include <cstdlib>
#include <iomanip>

#include <parser.h>
#include <entity.h>

using namespace std;

/* dynamically load parsers specified by filters */
int load_parsers_from_directory(const string& dir, const vector<string>& filters, vector<parser*>& parsers, vector<void*>& handles);
/* release all parsers */
void release_parsers(vector<parser*>& parsers, vector<void*>& handles);

int main(int argc, char *argv[])
{
	string log_file_name;
	string parser_folder = "./parsers/";
	vector<string> filters;
	string tmp;
	int pos = -1;
	int c;
	while ((c = getopt (argc, argv, "d:p:")) != -1)
		switch (c)
		{
		case 'd':
			parser_folder = optarg;
			break;
		case 'p':
			tmp = optarg;
			pos = -1;
			while((pos = tmp.find(' ', pos+1)) != string::npos)
				{
					tmp.replace(pos, 1, "\\W");
					// skip \\W
					pos = pos + 1; 
				}
			filters.push_back(tmp);
			break;
		case '?':
			return 1;
		default:
			abort ();
      }
	if (optind != argc - 1)
	{
		cerr << argv[0] << ": too many log files, ";
		while(optind != argc)
		{
			cerr << argv[optind++];
			if (optind != argc)
				cerr << ", ";
		}
		cerr << " (only one is allowed)." << endl;
		return 1;
	}
	log_file_name = argv[optind];
	
	ifstream fis(log_file_name);
	vector<parser*> parsers;
	vector<void*> handles;
	if (fis.fail())
	{
		cerr << "file '" << log_file_name << "' not exists." << endl;
		return 1;
	}
	if(load_parsers_from_directory(parser_folder, filters, parsers, handles) < 0)
	{
		cerr << "fails to load parsers from directory '" << parser_folder << "'." << endl;
		return 2;
	}

	regex empty_line("\\W*");
	string line;
	int line_counter = 0;
	set<parser*> workings;
	while (getline(fis, line)) {
		line_counter++;
		// skip empty lines
		if (regex_match(line, empty_line))
		{
			continue;
		}
		parser::parse_result result = parser::SKIP;
		// pass the lines to working queue firstly
		for(set<parser*>::iterator it = workings.begin(); (parser::SKIP == result) && it != workings.end(); it++)
		{
			result = (*it)->parse_line(line, line_counter);
			const entity* info;
			switch (result)
			{
			case parser::SUCCESS_AND_STOP:
				// output
				info = (*it)->pop_parse_result();
				info->print_out(cout);
				delete info;
				workings.erase(it);
				break;
			case parser::SUCCESS_AND_CONTINUE:
			case parser::SKIP:
			default:
				break;
			}
		}

		
		if (parser::SKIP != result)
		{
			continue;
		}
		for (vector<parser*>::iterator it = parsers.begin(); workings.count(*it) == 0 && parser::SKIP == result && it != parsers.end(); it++)
		{
			result = (*it)->parse_line(line, line_counter);
			const entity* info;
			switch (result)
			{
			case parser::SUCCESS_AND_STOP:
				info = (*it)->pop_parse_result();
				info->print_out(cout);
				delete info;
				break;
			case parser::SUCCESS_AND_CONTINUE:
				workings.insert(*it);
				break;
			case parser::SKIP:
			default:
				break;
			}
		}
	}
	release_parsers(parsers, handles);
    return 0;
}

/**
 * This function must be customized to support new features
 */
int load_parsers_from_directory(const string& dir, const vector<string>& filters, vector<parser*>& parsers, vector<void*>& handles)
{
	static const string ALLOWED_EXTENSSION = ".so";
	vector<regex> reg_filters;
	for(vector<string>::const_iterator it = filters.begin(); it != filters.end(); it++)
	{
		reg_filters.push_back(regex(*it));
		// auto fill with ' parser'
		reg_filters.push_back(regex(*it + " parser"));
	}

	
	if (dir.length() == 0)
		return -1;
	string mydir = dir;
	if (mydir.at(mydir.size()-1) != '/')
		mydir.append("/");
	DIR *directory;
	struct dirent* entry;
	if (!(directory = opendir(dir.c_str())))
		return -1;
	if (!(entry = readdir(directory)))
		return -1;
	do {
        if (entry->d_type == DT_REG)
			{
				string filename(entry->d_name);
				if (filename.size() - ALLOWED_EXTENSSION.size() != filename.rfind(ALLOWED_EXTENSSION))
					continue;
				filename = mydir + filename;
				void* handle = dlopen(filename.c_str(), RTLD_NOW);
				if (NULL == handle)
				{
					cerr << "open file " << filename << " fails:" << dlerror() << endl;
					continue;
				}
				void* obj_creator = dlsym(handle, "create_parser");
				parser_factory* factory = (parser_factory*)(obj_creator);
				parser* log_parser = (*factory)();
				const string& name = log_parser->get_name();
				clog << "Found parser " << std::left << setw(30) << ("'" + name + "':");
				bool loaded = false;
				for(vector<regex>::iterator it = reg_filters.begin(); !loaded && it != reg_filters.end(); it++)
					{
						if (regex_match(name, *it))
							{
								loaded = true;
								parsers.push_back(log_parser);
								handles.push_back(handle);
							}
					}
				clog << (loaded ? "LOADED" : "SKIPED") << endl;
			}
    } while (entry = readdir(directory));
	closedir(directory);
	return 0;
}

void release_parsers(vector<parser*>& parsers, vector<void*>& handles)
{
	for (vector<parser*>::iterator it = parsers.begin(); it != parsers.end(); it++)
		{
			delete (*it);
		}
	for (vector<void*>::iterator it = handles.begin(); it != handles.end(); it++)
		{
			dlclose(*it);
		}
}
