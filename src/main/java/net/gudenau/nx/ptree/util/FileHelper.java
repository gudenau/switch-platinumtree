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

    /**
     * Directly read from a file into a direct buffer.
     *
     * @param file The file to read
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

    private static native int read(int fd, long handle, long bufferAddress, int limit);
}
