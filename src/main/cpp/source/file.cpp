#include <net_gudenau_nx_ptree_util_FileHelper.h>

extern "C"{
#include <unistd.h>
}

#define UNUSED(var) ((void)( var ))

JNIEXPORT jint JNICALL Java_net_gudenau_nx_ptree_util_FileHelper_read
(JNIEnv* env, jclass klass, jint fd, jlong handle, jlong buffer, jint size){
    UNUSED(env);
    UNUSED(klass);
    UNUSED(handle);

    return (jint)read((int)fd, (void*)buffer, (size_t)size);
}