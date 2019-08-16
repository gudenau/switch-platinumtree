#include <net_gudenau_nx_ptree_util_FileHelper.h>

extern "C"{
#ifdef __linux__
#include <unistd.h>
#elif _WIN32
#include <Windows.h>
#endif
}

#define UNUSED(var) ((void)( var ))

JNIEXPORT jint JNICALL Java_net_gudenau_nx_ptree_util_FileHelper_read
(JNIEnv* env, jclass klass, jint fd, jlong handle, jlong buffer, jint size){
    UNUSED(env);
    UNUSED(klass);

#ifdef __linux__
    UNUSED(handle);
    return (jint)read((int)fd, (void*)buffer, (size_t)size);
#elif _WIN32
    UNUSED(fd);
    DWORD read = 0;
    if(ReadFile((HANDLE)handle, (LPVOID)buffer, (DWORD)size, &read, nullptr)){
        return (jint)read;
    }else{
        return -1;
    }
#endif
}

JNIEXPORT jint JNICALL Java_net_gudenau_nx_ptree_util_FileHelper_write
(JNIEnv* env, jclass klass, jint fd, jlong handle, jlong buffer, jint size){
    UNUSED(env);
    UNUSED(klass);

#ifdef __linux__
    UNUSED(handle);
    return (jint)write((int)fd, (void*)buffer, (size_t)size);
#elif _WIN32
    UNUSED(fd);
    DWORD written = 0;
    if(WriteFile((HANDLE)handle, (LPCVOID)buffer, (DWORD)size, &written, nullptr)){
        return (jint)written;
    }else{
        return -1;
    }
#endif
}
