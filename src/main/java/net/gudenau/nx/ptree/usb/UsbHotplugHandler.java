package net.gudenau.nx.ptree.usb;

import net.gudenau.nx.ptree.util.Memory;

import java.lang.ref.Cleaner;

public class UsbHotplugHandler implements AutoCloseable{
    private final Cleaner.Cleanable cleaner;

    UsbHotplugHandler(long ctx, int handle, long data){
        cleaner = Memory.registerCleaner(this, new State(ctx, handle, data));
    }

    @Override
    public void close(){
        cleaner.clean();
    }

    private static class State implements Runnable{
        private final long ctx;
        private final int handle;
        private final long data;

        State(long ctx, int handle, long data){
            this.ctx = ctx;
            this.handle = handle;
            this.data = data;
        }

        @Override
        public void run(){
            LibUsb.libusb_hotplug_deregister_callback(ctx, handle);
            LibUsb.freeHotplugData(data);
        }
    }

    @FunctionalInterface
    public interface Callback{
        int invoke(long ctx, long device, int event, Object user);
    }
}
