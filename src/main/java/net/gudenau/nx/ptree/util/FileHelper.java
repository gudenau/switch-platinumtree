package net.gudenau.nx.ptree.util;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * File stuff.
 * */
public class FileHelper{
    private static final long FileDescriptor_fd = UnsafeHelper.objectFieldOffset(FileDescriptor.class, "fd");
    private static final long FileDescriptor_handle = UnsafeHelper.objectFieldOffset(FileDescriptor.class, "handle");

    private static native int read(int fd, long handle, long bufferAddress, int limit);
    /**
     * Directly read from a file into a direct buffer.
     *
     * @param file The file to read from
     * @param buffer The buffer to write to
     *
     * @return The amount read
     * */
    public static int read(RandomAccessFile file, ByteBuffer buffer) throws IOException{
        var fileDescriptor = file.getFD();
        int fd = UnsafeHelper.getInt(fileDescriptor, FileDescriptor_fd);
        long handle = UnsafeHelper.getLong(fileDescriptor, FileDescriptor_handle);
        return read(
            fd,
            handle,
            Memory.getBufferAddress(buffer) + buffer.position(),
            buffer.limit()
        );
    }

    private static native int write(int fd, long handle, long bufferAddress, int limit);
    /**
     * Directly write to a file into a direct buffer.
     *
     * @param file The file to write to
     * @param buffer The buffer to read from
     *
     * @return The amount written
     * */
    public static int write(RandomAccessFile file, ByteBuffer buffer) throws IOException{
        var fileDescriptor = file.getFD();
        int fd = UnsafeHelper.getInt(fileDescriptor, FileDescriptor_fd);
        long handle = UnsafeHelper.getLong(fileDescriptor, FileDescriptor_handle);
        return write(
            fd,
            handle,
            Memory.getBufferAddress(buffer) + buffer.position(),
            buffer.limit()
        );
    }
}
