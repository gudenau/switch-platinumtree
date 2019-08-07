package net.gudenau.nx.ptree.util;

import java.lang.ref.Cleaner;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Memory stuff.
 * */
public class Memory{
    /**
     * Native null.
     * */
    public static final long NULL = 0L;

    /**
     * The size in bytes of addresses on this system.
     * */
    public static final int ADDRESS_SIZE = UnsafeHelper.ADDRESS_SIZE;

    private static final Cleaner CLEANER = Cleaner.create();
    private static final Class<? extends ByteBuffer> DirectByteBuffer = ByteBuffer.allocateDirect(1).getClass();
    private static final long Buffer_address = UnsafeHelper.objectFieldOffset(Buffer.class, "address");
    private static final long Buffer_capacity = UnsafeHelper.objectFieldOffset(Buffer.class, "capacity");
    private static final long Buffer_limit = UnsafeHelper.objectFieldOffset(Buffer.class, "limit");

    /**
     * Registers a cleaner.
     *
     * @param instance The object that needs cleaning
     * @param thunk The thing that does the cleaner
     *
     * @return The cleaner instance
     * */
    public static Cleaner.Cleanable registerCleaner(Object instance, Runnable thunk){
        return CLEANER.register(instance, thunk);
    }

    /**
     * Creates a buffer from an address and a size.
     *
     * @param address The address
     * @param size The size
     *
     * @return The buffer
     * */
    private static ByteBuffer createBuffer(long address, int size){
        ByteBuffer buffer = UnsafeHelper.allocateInstance(DirectByteBuffer);
        UnsafeHelper.putLong(buffer, Buffer_address, address);
        UnsafeHelper.putInt(buffer, Buffer_capacity, size);
        // Less checks.
        UnsafeHelper.putInt(buffer, Buffer_limit, size);
        return buffer.order(ByteOrder.nativeOrder());
    }

    /**
     * Allocates a native buffer.
     *
     * @param size The size of the buffer
     *
     * @return The allocated buffer
     * */
    public static ByteBuffer allocateBuffer(int size){
        return createBuffer(UnsafeHelper.allocateMemory(size), size);
    }

    /**
     * Gets the address of a direct buffer.
     *
     * @param buffer The buffer
     *
     * @return The pointer
     * */
    public static long getBufferAddress(Buffer buffer){
        return UnsafeHelper.getLong(buffer, Buffer_address);
    }

    /**
     * Allocates a pointer.
     *
     * @return The pointer
     * */
    public static Pointer allocatePointer(){
        return new Pointer(allocateBuffer(ADDRESS_SIZE));
    }

    /**
     * Gets a pointer from a buffer.
     *
     * @param buffer The buffer
     *
     * @return A pointer
     * */
    public static long getPointer(ByteBuffer buffer){
        if(ADDRESS_SIZE == Integer.BYTES){
            return buffer.getInt();
        }else{
            return buffer.getLong();
        }
    }

    /**
     * Frees a buffer we allocated.
     *
     * @param buffer The buffer to free
     * */
    public static void freeBuffer(Buffer buffer){
        UnsafeHelper.freeMemory(getBufferAddress(buffer));
    }

    /**
     * Sets a buffer to a specific value.
     *
     * @param buffer The buffer
     * @param value The value
     * */
    public static void memset(ByteBuffer buffer, byte value){
        UnsafeHelper.setMemory(getBufferAddress(buffer), buffer.limit(), value);
    }

    /**
     * A basic Java version of a native pointer.
     * */
    public static final class Pointer implements AutoCloseable{
        private final ByteBuffer buffer;
        private final long pointer;
        private final Cleaner.Cleanable cleaner;

        Pointer(ByteBuffer buffer){
            this.buffer = buffer;
            this.pointer = getBufferAddress(buffer);
            this.cleaner = registerCleaner(this, new State(pointer));
        }

        /**
         * Gets the address of the pointer.
         *
         * @return The address
         * */
        public long getAddress(){
            return pointer;
        }

        /**
         * Dereferences a pointer to a pointer.
         *
         * @return The pointer
         * */
        public long getPointer(){
            if(ADDRESS_SIZE == Integer.BYTES){
                return buffer.getInt(0);
            }else{
                return buffer.getLong(0);
            }
        }

        /**
         * Dereferences a pointer to a buffer.
         *
         * @param size The size of the buffer
         *
         * @return The pointer
         * */
        public ByteBuffer dereference(int size){
            return createBuffer(getPointer(), size);
        }

        /**
         * Dereferences a pointer to an int.
         *
         * @return The int
         * */
        public int getInt(){
            return buffer.getInt(0);
        }

        @Override
        public void close(){
            cleaner.clean();
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
