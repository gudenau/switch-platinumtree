#include <net_gudenau_nx_ptree_usb_LibUsb.h>

#include <libusb-1.0/libusb.h>

#include <cstdio>
#include <cstdlib>

#define STRUCT_SIZEOF(javaPrefix, cType) \
extern "C" JNIEXPORT jint JNICALL JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_ ##javaPrefix ##_1SIZEOF \
(void){\
    return (jint)sizeof( cType );\
} \
JNIEXPORT jint JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_ ##javaPrefix ##_1SIZEOF \
(JNIEnv* env, jclass klass){\
    (void)env; (void)klass;\
    return JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_ ##javaPrefix ##_1SIZEOF ();\
}

#define STRUCT_GETTER(javaPrefix, javaType, cName, structType) \
extern "C" JNIEXPORT javaType JNICALL JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_ ##javaPrefix ##_1 ##cName \
(jlong pointer){\
    return ( javaType )(((structType) pointer )-> cName );\
} \
JNIEXPORT javaType JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_ ##javaPrefix ##_1 ##cName \
(JNIEnv* env, jclass klass, jlong pointer){\
    (void)env; (void)klass;\
    return JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_ ##javaPrefix ##_1 ##cName (pointer);\
}

#define UNUSED(var) ((void)( var ))

extern "C" JNIEXPORT jint JNICALL JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1init
(jlong ctx){
    return (jint)libusb_init((libusb_context**)ctx);
}
JNIEXPORT jint JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_libusb_1init
(JNIEnv* env, jclass klass, jlong ctx){
    UNUSED(env);
    UNUSED(klass);

    return JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1init(ctx);
}

extern "C" JNIEXPORT void JNICALL JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1exit
(jlong ctx){
    libusb_exit((libusb_context*)ctx);
}
JNIEXPORT void JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_libusb_1exit
(JNIEnv* env, jclass klass, jlong ctx){
    UNUSED(env);
    UNUSED(klass);

    JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1exit(ctx);
}

extern "C" JNIEXPORT jlong JNICALL JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1get_1device_1list
(jlong ctx, jlong list){
    return (jlong)libusb_get_device_list((libusb_context*)ctx, (libusb_device***)list);
}
JNIEXPORT jlong JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_libusb_1get_1device_1list
(JNIEnv* env, jclass klass, jlong ctx, jlong list){
    UNUSED(env);
    UNUSED(klass);

    return JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1get_1device_1list(ctx, list);
}

extern "C" JNIEXPORT void JNICALL JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1free_1device_1list
(jlong list, jint unref_devices){
    libusb_free_device_list((libusb_device**)list, (int)unref_devices);
}
JNIEXPORT void JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_libusb_1free_1device_1list
(JNIEnv* env, jclass klass, jlong list, jint unref_devices){
    UNUSED(env);
    UNUSED(klass);

    JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1free_1device_1list(list, unref_devices);
}

extern "C" JNIEXPORT jlong JNICALL JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1ref_1device
(jlong dev){
    return (jlong)libusb_ref_device((libusb_device*)dev);
}
JNIEXPORT jlong JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_libusb_1ref_1device
(JNIEnv* env, jclass klass, jlong dev){
    UNUSED(env);
    UNUSED(klass);

    return (jlong)JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1ref_1device(dev);
}

extern "C" JNIEXPORT void JNICALL JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1unref_1device
(jlong dev){
    libusb_unref_device((libusb_device*)dev);
}
JNIEXPORT void JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_libusb_1unref_1device
(JNIEnv* env, jclass klass, jlong dev){
    UNUSED(env);
    UNUSED(klass);

    JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1unref_1device(dev);    
}

extern "C" JNIEXPORT jint JNICALL JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1open
(jlong dev, jlong dev_handle){
    return (jint)libusb_open((libusb_device*)dev, (libusb_device_handle**)dev_handle);
}
JNIEXPORT jint JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_libusb_1open
(JNIEnv* env, jclass klass, jlong dev, jlong dev_handle){
    UNUSED(env);
    UNUSED(klass);
    
    return JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1open(dev, dev_handle);
}

extern "C" JNIEXPORT void JNICALL JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1close
(jlong dev_handle){
    libusb_close((libusb_device_handle*)dev_handle);
}
JNIEXPORT void JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_libusb_1close
(JNIEnv* env, jclass klass, jlong dev_handle){
    UNUSED(env);
    UNUSED(klass);

    JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1close(dev_handle);
}

extern "C" JNIEXPORT jint JNICALL JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1claim_1interface
(jlong dev_handle, jint interface_number){
    return (jint)libusb_claim_interface((libusb_device_handle*)dev_handle, (int)interface_number);
}
JNIEXPORT jint JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_libusb_1claim_1interface
(JNIEnv* env, jclass klass, jlong dev_hande, jint interface_number){
    UNUSED(env);
    UNUSED(klass);

    return JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1claim_1interface(dev_hande, interface_number);
}

extern "C" JNIEXPORT jint JNICALL JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1get_1device_1descriptor
(jlong dev, jlong desc){
    return (jint)libusb_get_device_descriptor((libusb_device*)dev, (libusb_device_descriptor*)desc);
}
JNIEXPORT jint JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_libusb_1get_1device_1descriptor
(JNIEnv* env, jclass klass, jlong dev, jlong desc){
    UNUSED(env);
    UNUSED(klass);

    return JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1get_1device_1descriptor(dev, desc);
}

JNIEXPORT jint JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_libusb_1bulk_1transfer
(JNIEnv* env, jclass klass, jlong dev_handle, jbyte endpoint, jlong data, jint length, jlong actualLength, jint timeout){
    UNUSED(env);
    UNUSED(klass);

    return (jint)libusb_bulk_transfer((libusb_device_handle*)dev_handle, (unsigned char)endpoint, (unsigned char*)data, (int)length, (int*)actualLength, (unsigned int)timeout);
}

extern "C" JNIEXPORT jint JNICALL JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1hotplug_1register_1callback
(jlong ctx, jint events, jint flags, jint vendor_id, jint product_id, jint dev_class, jlong cb_fn, jlong user_data, jlong callback_handle){
    return (jint)libusb_hotplug_register_callback((libusb_context*)ctx, (libusb_hotplug_event)events, (libusb_hotplug_flag)flags, (int)vendor_id, (int)product_id, (int)dev_class, (libusb_hotplug_callback_fn)cb_fn, (void*)user_data, (libusb_hotplug_callback_handle*)callback_handle);
}
JNIEXPORT jint JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_libusb_1hotplug_1register_1callback
(JNIEnv* env, jclass klass, jlong ctx, jint events, jint flags, jint vendor_id, jint product_id, jint dev_class, jlong cb_fn, jlong user_data, jlong callback_handle){
    UNUSED(env);
    UNUSED(klass);

    return JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1hotplug_1register_1callback(ctx, events, flags, vendor_id, product_id, dev_class, cb_fn, user_data, callback_handle);
}

extern "C" JNIEXPORT void JNICALL JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1hotplug_1deregister_1callback
(jlong ctx, jint callback_handle){
    libusb_hotplug_deregister_callback((libusb_context*)ctx, (libusb_hotplug_callback_handle)callback_handle);
}
JNIEXPORT void JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_libusb_1hotplug_1deregister_1callback
(JNIEnv* env, jclass klass, jlong ctx, jint callback_handle){
    UNUSED(env);
    UNUSED(klass);

    JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1hotplug_1deregister_1callback(ctx, callback_handle);
}

extern "C" JNIEXPORT jint JNICALL JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1has_1capability
(jint capability){
    return (jint)libusb_has_capability((uint32_t)capability);
}
JNIEXPORT jint JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_libusb_1has_1capability
(JNIEnv* env, jclass klass, jint capability){
    UNUSED(env);
    UNUSED(klass);

    return JavaCritical_net_gudenau_nx_ptree_usb_LibUsb_libusb_1has_1capability(capability);
}

JNIEXPORT jint JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_libusb_1handle_1events_1completed
(JNIEnv* env, jclass klass, jlong ctx, jlong completed){
    UNUSED(env);
    UNUSED(klass);

    return (jint)libusb_handle_events_completed((libusb_context*)ctx, (int*)completed);
}

STRUCT_SIZEOF(libusb_1device_1descriptor, libusb_device_descriptor);
STRUCT_GETTER(libusb_1device_1descriptor, jbyte, bLength, libusb_device_descriptor*);
STRUCT_GETTER(libusb_1device_1descriptor, jbyte, bDescriptorType, libusb_device_descriptor*);
STRUCT_GETTER(libusb_1device_1descriptor, jshort, bcdUSB, libusb_device_descriptor*);
STRUCT_GETTER(libusb_1device_1descriptor, jbyte, bDeviceClass, libusb_device_descriptor*);
STRUCT_GETTER(libusb_1device_1descriptor, jbyte, bDeviceSubClass, libusb_device_descriptor*);
STRUCT_GETTER(libusb_1device_1descriptor, jbyte, bDeviceProtocol, libusb_device_descriptor*);
STRUCT_GETTER(libusb_1device_1descriptor, jbyte, bMaxPacketSize0, libusb_device_descriptor*);
STRUCT_GETTER(libusb_1device_1descriptor, jshort, idVendor, libusb_device_descriptor*);
STRUCT_GETTER(libusb_1device_1descriptor, jshort, idProduct, libusb_device_descriptor*);
STRUCT_GETTER(libusb_1device_1descriptor, jshort, bcdDevice, libusb_device_descriptor*);
STRUCT_GETTER(libusb_1device_1descriptor, jbyte, iManufacturer, libusb_device_descriptor*);
STRUCT_GETTER(libusb_1device_1descriptor, jbyte, iProduct, libusb_device_descriptor*);
STRUCT_GETTER(libusb_1device_1descriptor, jbyte, bNumConfigurations, libusb_device_descriptor*);

typedef struct{
    JNIEnv* env;
    jmethodID method;
    jobject object;
    jobject user;
} HotplugData;

int hotplugCallback(libusb_context* ctx, libusb_device* device, libusb_hotplug_event event, void* user_data){
    auto data = (HotplugData*)user_data;
    return (int)data->env->CallIntMethod(data->object, data->method, (jlong)ctx, (jlong)device, (jint)event, data->user);
}

JNIEXPORT jlong JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_getHotplugCallback
(JNIEnv* env, jclass klass){
    UNUSED(env);
    UNUSED(klass);

    return (jlong)hotplugCallback;
}

JNIEXPORT jlong JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_createHotplugData
(JNIEnv* env, jclass klass, jobject method, jobject object, jobject user){
    UNUSED(klass);
    
    auto data = (HotplugData*)malloc(sizeof(HotplugData));
    data->env = env;
    data->method = env->FromReflectedMethod(method);
    data->object = env->NewGlobalRef(object);
    data->user = env->NewGlobalRef(user);
    return (jlong)data;
}

JNIEXPORT void JNICALL Java_net_gudenau_nx_ptree_usb_LibUsb_freeHotplugData
(JNIEnv* env, jclass klass, jlong javaData){
    UNUSED(klass);

    auto data = (HotplugData*)javaData;
    env->DeleteGlobalRef(data->object);
    env->DeleteGlobalRef(data->user);
    free((void*)data);
}
