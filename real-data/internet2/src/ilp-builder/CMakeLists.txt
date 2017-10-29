cmake_minimum_required(VERSION 2.6)
project (ilp-builder)

include_directories(".")

add_executable (ilp-builder ilp-builder.cpp)

configure_file(solver.sh solver.sh COPYONLY)

add_subdirectory(model)
target_link_libraries(ilp-builder switch aggregated_flow ilp_model)

add_subdirectory(parser)
target_link_libraries(ilp-builder ilp_option_parser ilp_config_parser)

add_subdirectory(builder)
target_link_libraries(ilp-builder ilp_model_builder gurobi_emitter)

add_subdirectory(solver)
target_link_libraries(ilp-builder heuristic_solver greedy_fill_ingress)
