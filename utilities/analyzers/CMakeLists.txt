cmake_minimum_required(VERSION 2.6)
project (analyzers)
SET (GCC_COVERAGE_COMPILE_FLAGS "-std=c++11")
SET (CMAKE_CXX_FLAGS "${GCC_COVERAGE_COMPILE_FLAGS}")

if (DEBUG)
  message ("enable debug mode...")
  SET (GCC_COVERAGE_COMPILE_FLAGS "-std=c++11 -g")
else ()
  message ("g++ optimize to -O3")
  SET (GCC_COVERAGE_COMPILE_FLAGS "-std=c++11 -O3")
endif ()

SET (CMAKE_CXX_FLAGS  "${GCC_COVERAGE_COMPILE_FLAGS}" )

include_directories(".")
add_subdirectory("common")

add_subdirectory("accuracy")
add_custom_target(copy_accuracy_analyzer ALL cp accuracy/accuracy_analyzer accuracy_analyzer DEPENDS accuracy_analyzer)
add_subdirectory("delay")
add_custom_target(copy_delay_analyzer ALL cp delay/delay_analyzer delay_analyzer DEPENDS delay_analyzer)
add_subdirectory("dedicated_rule_usage")
add_custom_target(copy_dedicated_rule_usage_analyzer ALL cp dedicated_rule_usage/dedicated_rule_usage_analyzer dedicated_rule_usage_analyzer DEPENDS dedicated_rule_usage_analyzer)
add_subdirectory("throughput")
add_custom_target(copy_throughput_analyzer ALL cp throughput/throughput_analyzer throughput_analyzer DEPENDS throughput_analyzer)
add_subdirectory("pktin_throughput")
add_custom_target(copy_pktin_throughput_analyzer ALL cp pktin_throughput/pktin_throughput_analyzer pktin_throughput_analyzer DEPENDS pktin_throughput_analyzer)

