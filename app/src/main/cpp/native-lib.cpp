#include <jni.h>
#include <string>
#include <vector>
#include <thread>

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_learnandroidfromai_NativeBridge_add(
        JNIEnv* env,
        jobject thiz,
        jint a,
        jint b
) {
    return a + b;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_learnandroidfromai_NativeBridge_greet(
        JNIEnv* env,
        jobject thiz,
        jstring name
) {
    const char* nativeName = env->GetStringUTFChars(name, nullptr);

    std::string result = "Hello, ";
    result += nativeName;

    env->ReleaseStringUTFChars(name, nativeName);

    return env->NewStringUTF(result.c_str());
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_learnandroidfromai_NativeBridge_sum(
        JNIEnv* env,
        jobject thiz,
        jintArray values
) {
    jsize length = env->GetArrayLength(values);

    jint* nativeValues =
            env->GetIntArrayElements(values, nullptr);

    jint result = 0;

    for (jsize i = 0; i < length; ++i) {
        result += nativeValues[i];
    }

    env->ReleaseIntArrayElements(
            values,
            nativeValues,
            JNI_ABORT
    );

    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_learnandroidfromai_NativeBridge_doubleInPlace(
        JNIEnv* env,
        jobject thiz,
        jintArray values
) {
    jsize length = env->GetArrayLength(values);

    jint* data =
            env->GetIntArrayElements(values, nullptr);

    for (jsize i = 0; i < length; ++i) {
        data[i] *= 2;
    }

    env->ReleaseIntArrayElements(
            values,
            data,
            0
    );
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_learnandroidfromai_NativeBridge_sumRegion(
        JNIEnv* env,
        jobject thiz,
        jintArray values
) {
    jsize length = env->GetArrayLength(values);

    std::vector<jint> buffer(length);

    env->GetIntArrayRegion(
            values,
            0,
            length,
            buffer.data()
    );

    jint result = 0;

    for (jint value : buffer) {
        result += value;
    }

    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_learnandroidfromai_NativeBridge_startWork(
        JNIEnv* env,
        jobject thiz
) {
    JavaVM* vm = nullptr;
    env->GetJavaVM(&vm);

    jobject callback = env->NewGlobalRef(thiz);

    std::thread([vm, callback]() {

        JNIEnv* env = nullptr;

        vm->AttachCurrentThread(&env, nullptr);

        jclass clazz = env->GetObjectClass(callback);

        jmethodID method = env->GetMethodID(
                clazz,
                "onNativeResult",
                "(I)V"
        );

        env->CallVoidMethod(
                callback,
                method,
                42
        );

        env->DeleteGlobalRef(callback);

        vm->DetachCurrentThread();

    }).detach();
}