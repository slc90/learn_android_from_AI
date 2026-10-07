cmake_minimum_required(VERSION 3.22.1)

project("learnandroidfromai")

include(FetchContent)

FetchContent_Declare(
        eigen
        GIT_REPOSITORY https://gitlab.com/libeigen/eigen.git
        GIT_TAG 5.0.1
        GIT_SHALLOW TRUE
)

FetchContent_MakeAvailable(eigen)

add_library(
        native-lib
        SHARED
        native-lib.cpp
)

target_link_libraries(
        native-lib
        PRIVATE
        Eigen3::Eigen
)