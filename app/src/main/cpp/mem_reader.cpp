#include "mem_reader.h"
#include <fcntl.h>
#include <unistd.h>
#include <sys/uio.h>
#include <sys/syscall.h>
#include <cstring>
#include <cerrno>
#include <cstdio>
#include <fstream>
#include <string>

namespace mr {

static int g_mem_fd = -1;
static int g_pid = 0;

bool attach(int pid) {
    g_pid = pid;
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/mem", pid);
    g_mem_fd = open(path, O_RDWR);
    return g_mem_fd >= 0;
}

void detach() {
    if (g_mem_fd >= 0) { close(g_mem_fd); g_mem_fd = -1; }
    g_pid = 0;
}

int get_pid() { return g_pid; }

ssize_t read_buf(uintptr_t addr, void* out, size_t size) {
    if (g_mem_fd < 0) return -1;
    struct iovec local { out, size };
    struct iovec remote { reinterpret_cast<void*>(addr), size };
    return syscall(__NR_process_vm_readv, g_pid, &local, 1, &remote, 1, 0);
}

ssize_t write_buf(uintptr_t addr, const void* in, size_t size) {
    if (g_mem_fd < 0) return -1;
    struct iovec local { const_cast<void*>(in), size };
    struct iovec remote { reinterpret_cast<void*>(addr), size };
    return syscall(__NR_process_vm_writev, g_pid, &local, 1, &remote, 1, 0);
}

float read_f32(uintptr_t addr) {
    float v = 0; read_buf(addr, &v, 4); return v;
}

int32_t read_i32(uintptr_t addr) {
    int32_t v = 0; read_buf(addr, &v, 4); return v;
}

bool write_f32(uintptr_t addr, float v) {
    return write_buf(addr, &v, 4) == 4;
}

uintptr_t find_module_base(int pid, const char* module_name) {
    if (pid <= 0 || !module_name) return 0;
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    std::ifstream f(path);
    if (!f) return 0;
    std::string line;
    std::string needle(module_name);
    while (std::getline(f, line)) {
        if (line.find(needle) == std::string::npos) continue;
        auto dash = line.find('-');
        if (dash == std::string::npos) continue;
        std::string start = line.substr(0, dash);
        try { return std::stoull(start, nullptr, 16); }
        catch (...) { continue; }
    }
    return 0;
}

} // namespace mr
