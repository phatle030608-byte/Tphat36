#pragma once
#include <cstdint>
#include <cstddef>
#include <sys/types.h>

namespace mr {
    bool attach(int pid);
    void detach();
    int get_pid();
    ssize_t read_buf(uintptr_t addr, void* out, size_t size);
    ssize_t write_buf(uintptr_t addr, const void* in, size_t size);
    float   read_f32(uintptr_t addr);
    int32_t read_i32(uintptr_t addr);
    bool    write_f32(uintptr_t addr, float v);
    uintptr_t find_module_base(int pid, const char* module_name);
}
