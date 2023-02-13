cmake_minimum_required(VERSION 3.10)
project(ffmpegdecoder)

enable_language(CXX C)
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_C_STANDARD 11)

find_library( # Defines the name of the path variable that stores the
        # location of the NDK library.
        ANDROID_LIBS
        # Specifies the name of the NDK library that
        # CMake needs to locate.
        log)

add_library(ffmpegdecoder SHARED
        ffmpegdecoder.cpp
        Decoder.cpp)

target_include_directories(ffmpegdecoder PUBLIC ffmpeg/include/${CMAKE_ANDROID_ARCH_ABI})

target_link_libraries(ffmpegdecoder ${ANDROID_LIBS})