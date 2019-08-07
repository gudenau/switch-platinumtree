package net.gudenau.nx.ptree.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * An Unsafe proxy, be careful!
 * */
class UnsafeHelper{
    private static final Unsafe UNSAFE = getUnsafe();

    static final int ADDRESS_SIZE = Unsafe.ADDRESS_SIZE;

    private static Unsafe getUnsafe(){
        for(var field : Unsafe.class.getDeclaredFields()){
            if(
                field.getType() == Unsafe.class &&
                    field.getModifiers() == (Modifier.STATIC | Modifier.PRIVATE | Modifier.FINAL)
            ){
                try{
                    field.setAccessible(true);
                    return (Unsafe)field.get(null);
                }catch(ReflectiveOperationException e){
                    throw new RuntimeException(
                        "Failed to get Unsafe"
                        , e);
                }
            }
        }
        throw new RuntimeException("Failed to find Unsafe");
    }

    static long allocateMemory(long bytes){
        return UNSAFE.allocateMemory(bytes);
    }

    static void freeMemory(long address){
        UNSAFE.freeMemory(address);
    }

    static long objectFieldOffset(Field field){
        return UNSAFE.objectFieldOffset(field);
    }

    static long objectFieldOffset(Class<?> type, String name){
        try{
            return objectFieldOffset(type.getDeclaredField(name));
        }catch(NoSuchFieldException e){
            throw new RuntimeException(String.format(
                "Failed to find field %s in type %s",
                name,
                type.getName()
            ), e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T allocateInstance(Class<T> type){
        try{
            return (T)UNSAFE.allocateInstance(type);
        }catch(InstantiationException e){
            throw new RuntimeException(String.format(
                "Failed to allocate %s",
                type.getName()
            ), e);
        }
    }

    static void putLong(Object o, long offset, long x){
        UNSAFE.putLong(o, offset, x);
    }

    static long getLong(Object o, long offset){
        return UNSAFE.getLong(o, offset);
    }

    static void putInt(Object o, long offset, int x){
        UNSAFE.putInt(o, offset, x);
    }

    static int getInt(Object o, long offset){
        return UNSAFE.getInt(o, offset);
    }

    static long getAddress(long address){
        return UNSAFE.getAddress(address);
    }

    static void putAddress(long address, long x){
        UNSAFE.putAddress(address, x);
    }

    static void setMemory(long address, long bytes, byte value){
        UNSAFE.setMemory(address, bytes, value);
    }
}
