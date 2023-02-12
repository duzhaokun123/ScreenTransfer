cmake_minimum_required(VERSION 3.10)
project(getlinker)

enable_language(CXX C)
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_C_STANDARD 11)

add_library(getlinker SHARED
        getlinker.cpp)
