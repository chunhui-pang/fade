PROJECT(anomaly_parser)
CMAKE_MINIMUM_REQUIRED(VERSION 2.6)

add_library(anomaly SHARED anomaly.cpp)
target_link_libraries(anomaly entity)
add_library(anomaly_parser SHARED anomaly_parser.cpp)
target_link_libraries(anomaly_parser anomaly parser)

