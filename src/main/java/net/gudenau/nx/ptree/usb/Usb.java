package net.gudenau.nx.ptree.usb;

import net.gudenau.nx.ptree.util.Memory;

import java.lang.ref.Cleaner;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static net.gudenau.nx.ptree.usb.LibUsb.*;
import static net.gudenau.nx.ptree.util.Memory.NULL;

/**
 * A more Java like wrapper for libusb, still fairly low level however.
 * */
public class Usb{
    public static final int HOTPLUG_ARRIVED = LibUsb.LIBUSB_HOTPLUG_EVENT_DEVICE_ARRIVED;

    /**
     * Make sure this is cleaned up.
     * */
    private final Cleaner.Cleanable cleaner;

    /**
     * The context for libusb
     * */
    private final long context;

    /**
     * Create a new instance of Usb, creates a manages a corresponding
     * libusb context.
     * */
    public Usb(){
        try(var pointer = Memory.allocatePointer()){
            if(libusb_init(pointer.getAddress()) != LIBUSB_SUCCESS){
                throw new RuntimeException("Failed to init libusb");
            }
            context = pointer.getPointer();
        }

        cleaner = Memory.registerCleaner(this, new State(context));
    }

    /**
     * Finds a device with the provided vid and pid.
     *
     * @param vid The device VID
     * @param pid The device PID
     *
     * @return NULL or the device pointer
     * */
    public long findDevice(short vid, short pid){
        var devices = findDevices(vid, pid);
        if(devices.isEmpty()){
            return NULL;
        }else{
            return devices.iterator().next();
        }
    }

    /**
     * Finds devices with the provided vid and pid.
     *
     * @param vid The device VID
     * @param pid The device PID
     *
     * @return A set of found devices
     * */
    public Set<Long> findDevices(short vid, short pid){
        try(var pointer = Memory.allocatePointer()){
            long listResult = libusb_get_device_list(context, pointer.getAddress());

            if(listResult < 0){
                return Set.of();
            }

            // Try to make everything we can inside the try/final block to reduce leaks
            int count = (int)listResult;
            var descriptor = Memory.allocateBuffer(LibUsb.libusb_device_descriptor_SIZEOF());
            var list = pointer.dereference(count * Memory.ADDRESS_SIZE);

            try{
                long descriptorPointer = Memory.getBufferAddress(descriptor);
                Set<Long> devices = new HashSet<>();

                for(int i = 0; i < count; i++){
                    long device = Memory.getPointer(list);

                    if(libusb_get_device_descriptor(device, descriptorPointer) != LIBUSB_SUCCESS){
                        continue;
                    }

                    if(
                        vid == libusb_device_descriptor_idVendor(descriptorPointer) &&
                        pid == libusb_device_descriptor_idProduct(descriptorPointer)
                    ){
                        devices.add(libusb_ref_device(device));
                    }
                }

                return Collections.unmodifiableSet(devices);
            }finally{
                Memory.freeBuffer(descriptor);
                libusb_free_device_list(Memory.getBufferAddress(list), 1);
            }
        }
    }

    /**
     * Attempts to open a USB device.
     *
     * @param device The device pointer
     *
     * @return The device handle or NULL on error
     * */
    public long open(long device){
        try(var pointer = Memory.allocatePointer()){
            if(libusb_open(device, pointer.getAddress()) != LIBUSB_SUCCESS){
                return NULL;
            }
            return pointer.getPointer();
        }
    }

    /**
     * Closes a USB device.
     *
     * @param deviceHandle The device handle
     * */
    public void close(long deviceHandle){
        libusb_close(deviceHandle);
    }

    /**
     * Attempts to claim an interface.
     *
     * @param dev_handle The handle of the device
     * @param iface The interface to claim
     *
     * @return true if successful, false if failed
     * */
    public boolean claimInterface(long dev_handle, int iface){
        return libusb_claim_interface(dev_handle, iface) == LIBUSB_SUCCESS;
    }

    /**
     * Performs a bulk transfer.
     *
     * @param dev_handle The handle of the device
     * @param endpoint The endpoint
     * @param data The buffer to read from or write to
     * @param timeout Transfer timeout
     *
     * @return The amount transferred or -1 on error
     * */
    public int bulkTransfer(long dev_handle, byte endpoint, ByteBuffer data, int timeout){
        try(var transferred = Memory.allocatePointer()){
            int result = libusb_bulk_transfer(
                dev_handle, endpoint, Memory.getBufferAddress(data), data.limit(), transferred.getAddress(), timeout
            );
            if(result == LIBUSB_SUCCESS){
                return transferred.getInt();
            }else{
                return -1;
            }
        }
    }

    /**
     * Reduces the reference count on a device.
     *
     * @param device The device
     * */
    public void unrefDevice(long device){
        libusb_unref_device(device);
    }

    // Setup some static stuff for the callbacks
    private static final long CALLBACK_POINTER = LibUsb.getHotplugCallback();
    private static final Method CALLBACK_METHOD;
    static{
        Method method;
        try{
            method = UsbHotplugHandler.Callback.class.getDeclaredMethod("invoke", long.class, long.class, int.class, Object.class);
        }catch(ReflectiveOperationException e){
            throw new RuntimeException(e);
        }
        CALLBACK_METHOD = method;
    }

    /**
     * Registers a hotplug handler.
     *
     * It is automatically freed when the handle is GC'ed
     *
     * @param events The events to listen for
     * @param flags The callback flags
     * @param deviceVid The VID to look for
     * @param devicePid The PID to look for
     * @param deviceClass The class of the device to look for
     * @param callback The callback
     * @param userData A user object
     *
     * @return A handle to the hotplug handler
     * */
    public UsbHotplugHandler registerHotplugHandler(
        int events, int flags,
        int deviceVid, int devicePid, int deviceClass,
        UsbHotplugHandler.Callback callback, Object userData
    ){
        long data = LibUsb.createHotplugData(CALLBACK_METHOD, callback, userData);
        try(var pointer = Memory.allocatePointer()){
            int result = LibUsb.libusb_hotplug_register_callback(
                context,
                events, flags,
                deviceVid, devicePid, deviceClass,
                CALLBACK_POINTER, data,
                pointer.getAddress()
            );
            if(result != LIBUSB_SUCCESS){
                LibUsb.freeHotplugData(data);
                return null;
            }else{
                return new UsbHotplugHandler(context, pointer.getInt(), data);
            }
        }
    }

    /**
     * Checks if libusb supports hotplugging.
     *
     * @return Is hotplug supported?
     * */
    public boolean isHotplugSupported(){
        return LibUsb.libusb_has_capability(LibUsb.LIBUSB_CAP_HAS_HOTPLUG) != 0;
    }

    /**
     * Handle the libusb callbacks, needs to be called often.
     * */
    public void handleCallbacks(){
        LibUsb.libusb_handle_events_completed(context, NULL);
    }

    private static class State implements Runnable{
        private final long context;

        State(long context){
            this.context = context;
        }

        @Override
        public void run(){
            libusb_exit(context);
        }
    }
}
