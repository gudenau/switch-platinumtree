package net.gudenau.nx.ptree.usb;

import java.lang.reflect.Method;

/**
 * The place where all the libusb calls go to be proxied to the actual library.
 * */
class LibUsb{
    public static final int LIBUSB_CAP_HAS_HOTPLUG = 0x0001;

    public static final int LIBUSB_HOTPLUG_EVENT_DEVICE_ARRIVED = 0x01;
    public static final int LIBUSB_HOTPLUG_EVENT_DEVICE_LEFT = 0x02;

    static final int LIBUSB_SUCCESS = 0;
    static final int LIBUSB_ERROR_IO = -1;
    static final int LIBUSB_ERROR_INVALID_PARAM = -2;
    static final int LIBUSB_ERROR_ACCESS = -3;
    static final int LIBUSB_ERROR_NO_DEVICE = -4;
    static final int LIBUSB_ERROR_NOT_FOUND = -5;
    static final int LIBUSB_ERROR_BUSY = -6;
    static final int LIBUSB_ERROR_TIMEOUT = -7;
    static final int LIBUSB_ERROR_OVERFLOW = -8;
    static final int LIBUSB_ERROR_PIPE = -9;
    static final int LIBUSB_ERROR_INTERRUPTED = -10;
    static final int LIBUSB_ERROR_NO_MEM = -11;
    static final int LIBUSB_ERROR_NOT_SUPPORTED = -12;
    static final int LIBUSB_ERROR_OTHER = -99;

    static native int libusb_init(long ctx);
    static native void libusb_exit(long ctx);

    static native long libusb_get_device_list(long ctx, long list);
    static native void libusb_free_device_list(long list, int unref_devices);
    static native long libusb_ref_device(long device);
    static native void libusb_unref_device(long device);
    static native int libusb_open(long dev, long dev_hande);
    static native void libusb_close(long dev_handle);
    static native int libusb_claim_interface(long dev_handle, int interface_number);

    static native int libusb_get_device_descriptor(long dev, long desc);

    static native int libusb_bulk_transfer(long dev_handle, byte endpoint, long data, int length, long transferred, int timeout);

    static native int libusb_hotplug_register_callback(long ctx, int events, int flags, int vendor_id, int product_id, int dev_class, long cb_fn, long user_data, long callback_handle);
    static native void libusb_hotplug_deregister_callback(long ctx, int callback_handle);

    static native int libusb_has_capability(int capability);

    static native int libusb_handle_events_completed(long ctx, long completed);

    static native int libusb_device_descriptor_SIZEOF();
    static native byte libusb_device_descriptor_bLength(long struct);
    static native byte libusb_device_descriptor_bDescriptorType(long struct);
    static native short libusb_device_descriptor_bcdUSB(long struct);
    static native byte libusb_device_descriptor_bDeviceClass(long struct);
    static native byte libusb_device_descriptor_bDeviceSubClass(long struct);
    static native byte libusb_device_descriptor_bDeviceProtocol(long struct);
    static native byte libusb_device_descriptor_bMaxPacketSize0(long struct);
    static native short libusb_device_descriptor_idVendor(long struct);
    static native short libusb_device_descriptor_idProduct(long struct);
    static native short libusb_device_descriptor_bcdDevice(long struct);
    static native byte libusb_device_descriptor_iManufacturer(long struct);
    static native byte libusb_device_descriptor_iProduct(long struct);
    static native byte libusb_device_descriptor_iSerialNumber(long struct);
    static native byte libusb_device_descriptor_bNumConfigurations(long struct);

    static native long getHotplugCallback();
    static native long createHotplugData(Method method, Object callback, Object user);
    static native void freeHotplugData(long data);
}
