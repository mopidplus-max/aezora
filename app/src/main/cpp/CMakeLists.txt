cmake_minimum_required(VERSION 3.22.1)

project(aezora_audio)

set(SOUNDTOUCH_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/SoundTouch)

file(GLOB ST_SRC
    ${SOUNDTOUCH_DIR}/source/SoundTouch/*.cpp
)

add_library(
    aezora_audio
    SHARED
    native_audio.cpp
    ${ST_SRC}
)

target_include_directories(aezora_audio PRIVATE
    ${SOUNDTOUCH_DIR}/include
    ${SOUNDTOUCH_DIR}/source/SoundTouch
)

find_library(log-lib log)

target_link_libraries(
    aezora_audio
    ${log-lib}
)
