cmake_minimum_required(VERSION 2.6)
project (fib-parser)

include_directories (".")

add_executable (fib-parser fib-parser.cpp)
target_link_libraries(fib-parser -lpthread)

add_subdirectory("parser")
target_link_libraries(fib-parser option_parser boost_option_parser juniper_interface_parser juniper_rule_parser)

add_subdirectory ("common")
target_link_libraries (fib-parser util exception)

add_subdirectory ("network")
target_link_libraries (fib-parser interface rule pswitch)

add_subdirectory ("topo")
target_link_libraries (fib-parser topology rule_graph)

add_subdirectory ("ec")
target_link_libraries (fib-parser equivalent_class ec_filter len_ec_filter ec_emitter info_file_ec_emitter mininet_conf_ec_emitter switch_rule_analyzer)

# add_library(HEADER_ONLY_TARGET STATIC "trie/trie.hpp" "trie/trie_node.hpp")
# set_target_properties(HEADER_ONLY_TARGET PROPERTIES LINKER_LANGUAGE CXX)

