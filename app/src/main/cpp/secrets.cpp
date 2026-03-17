#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_modvault_app_NativeSecrets_getCurseForgeKey(JNIEnv* env, jclass clazz) {
    // Key is split and obfuscated to prevent simple string scanning
    std::string part1 = "$2a$10$";  // replace with your actual key parts
    std::string part2 = "REPLACE";
    std::string part3 = "_ME";
    return env->NewStringUTF((part1 + part2 + part3).c_str());
}
