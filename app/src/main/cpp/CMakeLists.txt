cmake_minimum_required(VERSION 3.18)
project(syshelper CXX)

add_library(syshelper SHARED
    native-lib.cpp
    mem_reader.cpp)

target_compile_options(syshelper PRIVATE -O2 -fvisibility=hidden -s -std=c++17)
target_link_libraries(syshelper log)
