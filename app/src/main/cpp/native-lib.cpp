#include <jni.h>
#include <android/log.h>
#include <thread>
#include <atomic>
#include <cmath>
#include <cstring>
#include <vector>
#include <mutex>
#include <random>
#include <chrono>
#include "mem_reader.h"

#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "sysh", __VA_ARGS__)

struct Offsets {
    uintptr_t localBase = 0, localPos = 0;
    uintptr_t yaw = 0, pitch = 0;
    uintptr_t plistBase = 0, plistCount = 0;
    uintptr_t plistSize = 0, plistPos = 0;
    uintptr_t plistTeam = 0, plistHp = 0;
} g_off;

struct PlayerOut { float x, y, z, hp, team, dist; };

static std::atomic<bool> g_run{false};
static std::atomic<bool> g_aim{false};
static std::atomic<bool> g_esp{true};
static std::atomic<bool> g_teamCheck{true};
static float g_fovPx = 200.f;
static float g_smooth = 0.4f;
static float g_screenW = 2400.f, g_screenH = 1080.f, g_fovY = 90.f;
static float g_aimHeight = 45.0f;

static std::vector<PlayerOut> g_players;
static std::mutex g_plMutex;
static std::mt19937 g_rng(std::random_device{}());

static float jitter(float base, float pct) {
    std::uniform_real_distribution<float> d(-pct, pct);
    return base * (1.f + d(g_rng));
}

static void loop() {
    int tick = 0;
    while (g_run.load()) {
        if (!g_off.localBase) {
            std::this_thread::sleep_for(std::chrono::milliseconds(500));
            continue;
        }

        uintptr_t local = (uintptr_t)mr::read_i32(g_off.localBase);
        if (!local) {
            std::this_thread::sleep_for(std::chrono::milliseconds(200));
            continue;
        }

        float lx = mr::read_f32(local + g_off.localPos);
        float ly = mr::read_f32(local + g_off.localPos + 4);
        float lz = mr::read_f32(local + g_off.localPos + 8);

        // --- ĐỌC CHUẨN CẤU TRÚC LIST<PLAYER> CỦA UNITY ---
        uintptr_t plist_obj = (uintptr_t)mr::read_i32(g_off.plistBase);
        if (!plist_obj) {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
            continue;
        }
        
        int count = mr::read_i32(plist_obj + g_off.plistCount); // List Count (thường 0x18)
        if (count <= 0 || count > 60) {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
            continue;
        }

        // List chứa con trỏ tới mảng _items (thường 0x10)
        uintptr_t items_ptr = (uintptr_t)mr::read_i32(plist_obj + 0x10); 
        if (!items_ptr) {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
            continue;
        }

        int32_t lteam = mr::read_i32(local + g_off.plistTeam);

        std::vector<PlayerOut> out;
        out.reserve(count);

        float bestD2 = g_fovPx * g_fovPx;
        float bestYaw = 0, bestPitch = 0;
        bool haveTarget = false;

        for (int i = 0; i < count; i++) {
            // Đọc từng con trỏ Player trong mảng (64-bit là 8 byte)
            uintptr_t p = (uintptr_t)mr::read_i32(items_ptr + i * 8); 
            if (!p) continue;

            int32_t team = mr::read_i32(p + g_off.plistTeam);
            float hp = mr::read_f32(p + g_off.plistHp);
            if (hp <= 0.f || hp > 1000.f) continue;

            float px = mr::read_f32(p + g_off.plistPos);
            float py = mr::read_f32(p + g_off.plistPos + 4);
            float pz = mr::read_f32(p + g_off.plistPos + 8);
            
            if (std::abs(px) > 10000.f || std::abs(pz) > 10000.f) continue;

            float dx = px - lx, dy = py - ly, dz = pz - lz;
            float d2 = dx * dx + dz * dz;
            if (d2 < 0.25f || d2 > 400.f * 400.f) continue;
            float dist = std::sqrt(d2);

            out.push_back({px, py, pz, hp, (float)team, dist});

            if (!g_aim.load()) continue;
            if (g_teamCheck.load() && team == lteam) continue;

            float hYaw = std::atan2(dx, dz) * 57.2958f;
            float hPitch = std::atan2((dy + g_aimHeight), std::hypot(dx, dz)) * 57.2958f;
            
            float curYaw = mr::read_f32(local + g_off.yaw);
            float curPitch = mr::read_f32(local + g_off.pitch);
            float dYaw = hYaw - curYaw;
            while (dYaw > 180) dYaw -= 360;
            while (dYaw < -180) dYaw += 360;
            float dPitch = hPitch - curPitch;
            float pxPerDeg = g_screenW / g_fovY;
            float sx = dYaw * pxPerDeg;
            float sy = dPitch * pxPerDeg;
            float screenD2 = sx * sx + sy * sy;
            if (screenD2 < bestD2) {
                bestD2 = screenD2;
                bestYaw = hYaw; bestPitch = hPitch;
                haveTarget = true;
            }
        }

        {
            std::lock_guard<std::mutex> lk(g_plMutex);
            g_players = std::move(out);
        }

        if (g_aim.load() && haveTarget) {
            float curYaw = mr::read_f32(local + g_off.yaw);
            float curPitch = mr::read_f32(local + g_off.pitch);
            float nYaw = curYaw + (bestYaw - curYaw) * (1.f - g_smooth);
            float nPitch = curPitch + (bestPitch - curPitch) * (1.f - g_smooth);
            float dY = nYaw - curYaw, dP = nPitch - curPitch;
            if (dY > 5) nYaw = curYaw + 5;
            if (dY < -5) nYaw = curYaw - 5;
            if (dP > 5) nPitch = curPitch + 5;
            if (dP < -5) nPitch = curPitch - 5;
            if (++tick % 3 == 0) {
                mr::write_f32(local + g_off.yaw, nYaw);
                mr::write_f32(local + g_off.pitch, nPitch);
            }
        }

        int sleepMs = (int)jitter(30.f, 0.2f);
        std::this_thread::sleep_for(std::chrono::milliseconds(sleepMs));
    }
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_frmod_NativeBridge_attach(JNIEnv*, jclass, jint pid) {
    return mr::attach(pid) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_com_frmod_NativeBridge_getModuleBase(JNIEnv* env, jclass, jstring name) {
    const char* n = env->GetStringUTFChars(name, nullptr);
    uintptr_t b = mr::find_module_base(mr::get_pid(), n);
    env->ReleaseStringUTFChars(name, n);
    return (jlong)b;
}

JNIEXPORT void JNICALL
Java_com_frmod_NativeBridge_setOffsets(JNIEnv*, jclass,
    jlong lb, jlong lp, jlong y, jlong p,
    jlong pb, jlong pc, jlong ps, jlong pp, jlong pt, jlong ph) {
    g_off.localBase = (uintptr_t)lb; g_off.localPos = (uintptr_t)lp;
    g_off.yaw = (uintptr_t)y; g_off.pitch = (uintptr_t)p;
    g_off.plistBase = (uintptr_t)pb; g_off.plistCount = (uintptr_t)pc;
    g_off.plistSize = (uintptr_t)ps; g_off.plistPos = (uintptr_t)pp;
    g_off.plistTeam = (uintptr_t)pt; g_off.plistHp = (uintptr_t)ph;
}

JNIEXPORT jboolean JNICALL
Java_com_frmod_NativeBridge_startLoop(JNIEnv*, jclass) {
    if (g_run.exchange(true)) return JNI_TRUE;
    std::thread(loop).detach();
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_frmod_NativeBridge_stopLoop(JNIEnv*, jclass) {
    g_run.store(false);
    mr::detach();
}

JNIEXPORT void JNICALL
Java_com_frmod_NativeBridge_setAim(JNIEnv*, jclass, jboolean en, jfloat fov, jfloat sm, jboolean tc) {
    g_aim.store(en); g_fovPx = fov; g_smooth = sm; g_teamCheck.store(tc);
}

JNIEXPORT void JNICALL
Java_com_frmod_NativeBridge_setEsp(JNIEnv*, jclass, jboolean en) {
    g_esp.store(en);
}

JNIEXPORT jfloatArray JNICALL
Java_com_frmod_NativeBridge_getPlayers(JNIEnv* env, jclass) {
    std::lock_guard<std::mutex> lk(g_plMutex);
    size_t n = g_players.size();
    jfloatArray arr = env->NewFloatArray((jsize)(n * 6));
    if (n) {
        std::vector<float> flat(n * 6);
        for (size_t i = 0; i < n; i++) {
            flat[i * 6 + 0] = g_players[i].x;
            flat[i * 6 + 1] = g_players[i].y;
            flat[i * 6 + 2] = g_players[i].z;
            flat[i * 6 + 3] = g_players[i].hp;
            flat[i * 6 + 4] = g_players[i].team;
            flat[i * 6 + 5] = g_players[i].dist;
        }
        env->SetFloatArrayRegion(arr, 0, (jsize)flat.size(), flat.data());
    }
    return arr;
}

JNIEXPORT jfloatArray JNICALL
Java_com_frmod_NativeBridge_getLocalPos(JNIEnv* env, jclass) {
    jfloatArray arr = env->NewFloatArray(3);
    float v[3] = {0, 0, 0};
    if (g_off.localBase) {
        uintptr_t local = (uintptr_t)mr::read_i32(g_off.localBase);
        if (local) {
            v[0] = mr::read_f32(local + g_off.localPos);
            v[1] = mr::read_f32(local + g_off.localPos + 4);
            v[2] = mr::read_f32(local + g_off.localPos + 8);
        }
    }
    env->SetFloatArrayRegion(arr, 0, 3, v);
    return arr;
}

JNIEXPORT jfloatArray JNICALL
Java_com_frmod_NativeBridge_getCam(JNIEnv* env, jclass) {
    jfloatArray arr = env->NewFloatArray(2);
    float v[2] = {0, 0};
    if (g_off.localBase) {
        uintptr_t local = (uintptr_t)mr::read_i32(g_off.localBase);
        if (local) {
            v[0] = mr::read_f32(local + g_off.yaw);
            v[1] = mr::read_f32(local + g_off.pitch);
        }
    }
    env->SetFloatArrayRegion(arr, 0, 2, v);
    return arr;
}

JNIEXPORT void JNICALL
Java_com_frmod_NativeBridge_setViewport(JNIEnv*, jclass, jfloat w, jfloat h, jfloat fov) {
    g_screenW = w; g_screenH = h; g_fovY = fov;
}

} // extern "C"
