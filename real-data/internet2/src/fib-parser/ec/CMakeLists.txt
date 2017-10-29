add_library (equivalent_class equivalent_class.cpp)

add_library (ec_filter ec_filter.cpp)
target_link_libraries(ec_filter equivalent_class)

add_library (len_ec_filter len_ec_filter.cpp)
target_link_libraries(len_ec_filter ec_filter)

add_library (ec_emitter ec_emitter.cpp)
target_link_libraries(ec_emitter equivalent_class)

add_library (info_file_ec_emitter info_file_ec_emitter.cpp)
target_link_libraries(info_file_ec_emitter ec_emitter)

add_library (mininet_conf_ec_emitter mininet_conf_ec_emitter.cpp)
# link util from common/
target_link_libraries(mininet_conf_ec_emitter ec_emitter util)

add_library (switch_rule_analyzer switch_rule_analyzer.cpp)
target_link_libraries(switch_rule_analyzer equivalent_class)

