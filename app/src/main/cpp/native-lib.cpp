#include <jni.h>
#include <string>
#include <vector>
#include <thread>
#include <Eigen/Dense>
#include "matrix_compute.h"

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


extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_learnandroidfromai_NativeBridge_describePerson(
        JNIEnv* env,
        jobject thiz,
        jobject person
) {
    jclass personClass = env->GetObjectClass(person);

    jfieldID nameField = env->GetFieldID(
            personClass,
            "name",
            "Ljava/lang/String;"
    );

    jfieldID ageField = env->GetFieldID(
            personClass,
            "age",
            "I"
    );

    jstring name = (jstring) env->GetObjectField(
            person,
            nameField
    );

    jint age = env->GetIntField(
            person,
            ageField
    );

    const char* nativeName =
            env->GetStringUTFChars(name, nullptr);

    std::string result = "Person(name=";
    result += nativeName;
    result += ", age=";
    result += std::to_string(age);
    result += ")";

    env->ReleaseStringUTFChars(
            name,
            nativeName
    );

    return env->NewStringUTF(result.c_str());
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_example_learnandroidfromai_NativeBridge_createPerson(
        JNIEnv* env,
        jobject thiz
) {
    jclass personClass = env->FindClass(
            "com/example/learnandroidfromai/model/Person"
    );

    jmethodID constructor = env->GetMethodID(
            personClass,
            "<init>",
            "(Ljava/lang/String;I)V"
    );

    jstring name = env->NewStringUTF("Native");

    jobject person = env->NewObject(
            personClass,
            constructor,
            name,
            42
    );

    return person;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_learnandroidfromai_NativeBridge_describePeople(
        JNIEnv* env,
        jobject thiz,
        jobject people
) {
    jclass listClass = env->GetObjectClass(people);

    jmethodID sizeMethod = env->GetMethodID(
            listClass,
            "size",
            "()I"
    );

    jmethodID getMethod = env->GetMethodID(
            listClass,
            "get",
            "(I)Ljava/lang/Object;"
    );

    jint size = env->CallIntMethod(
            people,
            sizeMethod
    );

    jclass personClass = env->FindClass(
            "com/example/learnandroidfromai/model/Person"
    );

    jfieldID nameField = env->GetFieldID(
            personClass,
            "name",
            "Ljava/lang/String;"
    );

    jfieldID ageField = env->GetFieldID(
            personClass,
            "age",
            "I"
    );

    std::string result = "[";

    for (jint i = 0; i < size; ++i) {
        jobject person = env->CallObjectMethod(
                people,
                getMethod,
                i
        );

        jstring name = (jstring) env->GetObjectField(
                person,
                nameField
        );

        jint age = env->GetIntField(
                person,
                ageField
        );

        const char* nativeName =
                env->GetStringUTFChars(name, nullptr);

        if (i > 0) {
            result += ", ";
        }

        result += "Person(name=";
        result += nativeName;
        result += ", age=";
        result += std::to_string(age);
        result += ")";

        env->ReleaseStringUTFChars(
                name,
                nativeName
        );

        env->DeleteLocalRef(name);
        env->DeleteLocalRef(person);
    }

    result += "]";

    return env->NewStringUTF(result.c_str());
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_example_learnandroidfromai_NativeBridge_createPeople(
        JNIEnv* env,
        jobject thiz
) {
    jclass listClass = env->FindClass("java/util/ArrayList");

    jmethodID listConstructor = env->GetMethodID(
            listClass,
            "<init>",
            "()V"
    );

    jmethodID addMethod = env->GetMethodID(
            listClass,
            "add",
            "(Ljava/lang/Object;)Z"
    );

    jobject list = env->NewObject(
            listClass,
            listConstructor
    );

    jclass personClass = env->FindClass(
            "com/example/learnandroidfromai/model/Person"
    );

    jmethodID personConstructor = env->GetMethodID(
            personClass,
            "<init>",
            "(Ljava/lang/String;I)V"
    );

    jstring name1 = env->NewStringUTF("Native Alice");
    jobject person1 = env->NewObject(
            personClass,
            personConstructor,
            name1,
            21
    );

    env->CallBooleanMethod(
            list,
            addMethod,
            person1
    );

    jstring name2 = env->NewStringUTF("Native Bob");
    jobject person2 = env->NewObject(
            personClass,
            personConstructor,
            name2,
            31
    );

    env->CallBooleanMethod(
            list,
            addMethod,
            person2
    );

    env->DeleteLocalRef(name1);
    env->DeleteLocalRef(person1);
    env->DeleteLocalRef(name2);
    env->DeleteLocalRef(person2);

    return list;
}

extern "C"
JNIEXPORT jdouble JNICALL
Java_com_example_learnandroidfromai_NativeBridge_runMatrixMultiply(
        JNIEnv* env,
        jobject thiz,
        jint size
) {
    return runMatrixMultiply(size);
}