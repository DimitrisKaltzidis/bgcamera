#include <jni.h>
#include <string>
#include <android/log.h>

#define  LOG_TAG    "NDK_DEBUG: "
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

extern "C"
JNIEXPORT jstring

JNICALL
Java_com_admobilize_bgtest_service_BuiltinCameraService_stringFromJNI(JNIEnv *env, jobject obj, jbyteArray frame) {
    int length = (int) env->GetArrayLength(frame);
    jbyte *pFrameData = env->GetByteArrayElements(frame, 0);
    std::string ret = std::string((char *)pFrameData, length);
    LOGI("frame lenght :%d", length );
    std::string output = "process frames ";
    return env->NewStringUTF(output.c_str());
}
