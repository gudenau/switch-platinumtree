package net.gudenau.nx.ptree.util;

import java.lang.ref.Cleaner;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Memory{
    public static final long NULL = 0L;

    private static final Cleaner CLEANER = Cleaner.create();
    public static final int ADDRESS_SIZE = UnsafeHelper.ADDRESS_SIZE;

    private static final Class<? extends ByteBuffer> DirectByteBuffer = ByteBuffer.allocateDirect(1).getClass();

    private static final long Buffer_address = UnsafeHelper.objectFieldOffset(Buffer.class, "address");
    private static final long Buffer_capacity = UnsafeHelper.objectFieldOffset(Buffer.class, "capacity");
    private static final long Buffer_limit = UnsafeHelper.objectFieldOffset(Buffer.class, "limit");

    public static Cleaner.Cleanable registerCleaner(Object instance, Runnable thunk){
        return CLEANER.register(instance, thunk);
    }

    public static ByteBuffer createBuffer(long address, int size){
        ByteBuffer buffer = UnsafeHelper.allocateInstance(DirectByteBuffer);
        UnsafeHelper.putLong(buffer, Buffer_address, address);
        UnsafeHelper.putInt(buffer, Buffer_capacity, size);
        // Less checks.
        UnsafeHelper.putInt(buffer, Buffer_limit, size);
        return buffer.order(ByteOrder.nativeOrder());
    }

    public static ByteBuffer allocateBuffer(int size){
        return createBuffer(UnsafeHelper.allocateMemory(size), size);
    }

    public static long getBufferAddress(Buffer buffer){
        return UnsafeHelper.getLong(buffer, Buffer_address);
    }

    public static Pointer allocatePointer(){
        return new Pointer(allocateBuffer(ADDRESS_SIZE));
    }

    public static long getPointer(ByteBuffer buffer){
        if(ADDRESS_SIZE == Integer.BYTES){
            return buffer.getInt();
        }else{
            return buffer.getLong();
        }
    }

    public static void freeBuffer(Buffer buffer){
        UnsafeHelper.freeMemory(getBufferAddress(buffer));
    }

    public static void memset(ByteBuffer data, byte value){
        UnsafeHelper.setMemory(getBufferAddress(data), data.limit(), value);
    }

    public static final class Pointer implements AutoCloseable{
        private final ByteBuffer buffer;
        private final long pointer;
        private final Cleaner.Cleanable cleaner;

        Pointer(ByteBuffer buffer){
            this.buffer = buffer;
            this.pointer = getBufferAddress(buffer);
            this.cleaner = registerCleaner(this, new State(pointer));
        }

        public long getAddress(){
            return pointer;
        }

        public long getPointer(){
            if(ADDRESS_SIZE == Integer.BYTES){
                return buffer.getInt(0);
            }else{
                return buffer.getLong(0);
            }
        }

        public ByteBuffer dereference(int size){
            return createBuffer(getPointer(), size);
        }

        @Override
        public void close(){
            cleaner.clean();
        }

        public void setPointer(long pointer){
            if(ADDRESS_SIZE == Integer.BYTES){
                buffer.putInt(0, (int)pointer);
            }else{
                buffer.putLong(0, pointer);
            }
        }

        public int getInt(){
            return buffer.getInt(0);
        }

        private static class State implements Runnable{
            private final long pointer;

            State(long pointer){
                this.pointer = pointer;
            }

            @Override
            public void run(){
                UnsafeHelper.freeMemory(pointer);
            }
        }
    }
}
