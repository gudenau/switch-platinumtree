package net.gudenau.nx.ptree;

import net.gudenau.nx.ptree.usb.Usb;
import net.gudenau.nx.ptree.util.FileHelper;
import net.gudenau.nx.ptree.util.Memory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.gudenau.nx.ptree.util.Memory.NULL;

class Client implements Runnable, AutoCloseable{
    private static final int COMMAND_BUFFER_SIZE = 0x1000;
    private static final int COMMAND_MAX_ID = Command.values().length - 1;

    private static final byte PIPE_READ = (byte)(0x80 | 0x01);
    private static final byte PIPE_WRITE = (byte)0x01;

    private static final int MAGIC_READ = 0x49434C47;
    private static final int MAGIC_WRITE = 0x4F434C47;

    private static final int RESULT_SUCCESS = 0;
    private static final int RESULT_MODULE = 356;
    private static final int RESULT_INVALID_INPUT = ((RESULT_MODULE) & 0x1FF) | ((1 + 100) & 0x1FFF) << 9;

    private final Usb usb;
    private final long device;
    private long handle;

    private final State state;
    private final Cleaner.Cleanable cleaner;

    private FileSystem filesystem = FileSystems.getDefault();;
    private List<Path> roots;

    Client(Usb usb, long device){
        this.usb = usb;
        this.device = device;
        state = new State(usb, device);
        cleaner = Memory.registerCleaner(this, state);
    }

    @Override
    public void run(){
        var inputBuffer = Memory.allocateBuffer(COMMAND_BUFFER_SIZE);
        var outputBuffer = Memory.allocateBuffer(COMMAND_BUFFER_SIZE);
        List<ByteBuffer> extraBuffers = new ArrayList<>();

        try{
            handle = usb.open(device);
            if(handle == NULL){
                return;
            }
            state.setHandle(handle);

            while(true){
                Memory.memset(inputBuffer, (byte)0);
                Memory.memset(outputBuffer, (byte)0);
                int transferred = usb.bulkTransfer(handle, PIPE_READ, inputBuffer, 0);
                if(transferred < 0){
                    System.err.println("Error reading from Switch");
                    break;
                }
                if(transferred == 0){
                    System.err.println("Empty request");
                    continue;
                }
                if(transferred != COMMAND_BUFFER_SIZE){
                    System.err.println("Invalid transfer size");
                    continue;
                }

                if(inputBuffer.getInt() != MAGIC_READ){
                    System.err.println("Invalid request magic");
                    continue;
                }
                int commandId = inputBuffer.getInt();
                if(commandId < 0 || commandId > COMMAND_MAX_ID){
                    System.err.println("Unknown command: " + commandId);
                    continue;
                }
                Command command = Command.values()[commandId];
                System.out.println("Handling " + command.name());

                switch(command){
                    case GetDriveCount:
                        handleGetDriveCount(outputBuffer);
                        break;
                    case GetDriveInfo:
                        handleGetDriveInfo(inputBuffer, outputBuffer);
                        break;
                    case StatPath:
                        handleStatPath(inputBuffer, outputBuffer);
                        break;
                    case GetFileCount:
                        handleGetFileCount(inputBuffer, outputBuffer);
                        break;
                    case GetFile:
                        handleGetFile(inputBuffer, outputBuffer);
                        break;
                    case GetDirectoryCount:
                        handleGetDirectoryCount(inputBuffer, outputBuffer);
                        break;
                    case GetDirectory:
                        handleGetDirectory(inputBuffer, outputBuffer);
                        break;
                    case ReadFile:
                        handleReadFile(inputBuffer, outputBuffer, extraBuffers);
                        break;
                    case WriteFile:
                        handleWriteFile(inputBuffer, outputBuffer);
                        break;
                    case Create:
                        handleCreate(inputBuffer, outputBuffer);
                        break;
                    case Delete:
                        handleDelete(inputBuffer, outputBuffer);
                        break;
                    case Rename:
                        handleRename(inputBuffer, outputBuffer);
                        break;
                    case GetSpecialPathCount:
                        handleGetSpecialPatchCount(outputBuffer);
                        break;
                    case GetSpecialPath:
                        handleGetSpecialPath(inputBuffer, outputBuffer);
                        break;
                    case SelectFile:
                        handleSelectFile(inputBuffer, outputBuffer);
                        break;
                    case Max:
                        handleMax(inputBuffer, outputBuffer);
                        break;
                }

                inputBuffer.position(0);
                outputBuffer.position(0);
                usb.bulkTransfer(handle, PIPE_WRITE, outputBuffer, 0);
                for(var buffer : extraBuffers){
                    usb.bulkTransfer(handle, PIPE_WRITE, buffer, 0);
                    Memory.freeBuffer(buffer);
                }
                extraBuffers.clear();
            }
        }finally{
            close();
            Memory.freeBuffer(inputBuffer);
            Memory.freeBuffer(outputBuffer);
        }
    }

    private void writePreamble(ByteBuffer buffer, int result){
        buffer.putInt(MAGIC_WRITE);
        buffer.putInt(result);
    }

    private void writePreamble(ByteBuffer buffer){
        writePreamble(buffer, RESULT_SUCCESS);
    }

    private void writeString(ByteBuffer buffer, String format, Object... params){
        writeString(buffer, String.format(format, params));
    }

    private void writeString(ByteBuffer buffer, String value){
        System.out.println(value);
        var bytes = value.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }

    private String readString(ByteBuffer buffer){
        int size = buffer.getInt();
        var data = new byte[size];
        buffer.get(data);
        System.out.println(new String(data, StandardCharsets.UTF_8));
        return new String(data, StandardCharsets.UTF_8);
    }

    private void handleGetDriveCount(ByteBuffer outputBuffer){
        roots = new ArrayList<>();
        filesystem.getRootDirectories().forEach(roots::add);
        writePreamble(outputBuffer);
        outputBuffer.putInt(roots.size());
    }

    private void handleGetDriveInfo(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        if(roots == null){
            roots = new ArrayList<>();
            filesystem.getRootDirectories().forEach(roots::add);
        }

        int filesystemIndex = inputBuffer.getInt();
        if(filesystemIndex < 0 || filesystemIndex >= roots.size()){
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        }else{
            writePreamble(outputBuffer, RESULT_SUCCESS);
            var root = roots.get(filesystemIndex);
            var file = root.toFile();
            //FIXME?
            writeString(outputBuffer, "root");
            writeString(outputBuffer, "root");
            outputBuffer.putInt((int)file.getFreeSpace());
            outputBuffer.putInt((int)file.getTotalSpace());
        }
    }

    private String fixPath(String path){
        path = path.replaceAll("\\\\", "/");
        int index = path.indexOf(":");
        if(index > -1){
            String key = path.substring(0, index);
            path = path.substring(index + 1);
            if("Home".equals(key)){
                path = System.getProperty("user.home") + path;
            }else if("Downloads".equals(key)){
                path = System.getProperty("user.home") + "/Downloads" + path;
            }
            return path;
        }else{
            return path;
        }
    }

    private void handleStatPath(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        var path = fixPath(readString(inputBuffer));
        var file = new File(path);
        if(file.exists()){
            int type;
            long fileSize;
            if(file.isDirectory()){
                type = 2;
                fileSize = 0;
            }else{
                type = 1;
                fileSize = file.length();
            }
            writePreamble(outputBuffer);
            outputBuffer.putInt(type);
            outputBuffer.putLong(fileSize);
        }else{
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        }
    }

    private String lastFilePath = null;
    private File[] lastFileResult = null;
    private void handleGetFileCount(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        var path = fixPath(readString(inputBuffer));
        var file = new File(path);
        if(file.exists() && file.isDirectory()){
            if(!path.equals(lastFilePath)){
                lastFilePath = path;
                lastFileResult = file.listFiles(File::isFile);
            }
            writePreamble(outputBuffer);
            outputBuffer.putInt(lastFileResult == null ? 0 : lastFileResult.length);
        }else{
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        }
    }

    private void handleGetFile(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        var path = fixPath(readString(inputBuffer));
        var file = new File(path);
        if(file.exists() && file.isDirectory()){
            if(!path.equals(lastFilePath)){
                lastFilePath = path;
                lastFileResult = file.listFiles(File::isFile);
            }
            int fileIndex = inputBuffer.getInt();
            if(lastFileResult == null || fileIndex < 0 || fileIndex >= lastFileResult.length){
                writePreamble(outputBuffer, RESULT_INVALID_INPUT);
            }else{
                writePreamble(outputBuffer);
                writeString(outputBuffer, lastFileResult[fileIndex].getName());
            }
        }else{
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        }
    }

    private String lastDirectoryPath = null;
    private File[] lastDirectoryResult = null;
    private void handleGetDirectoryCount(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        var path = fixPath(readString(inputBuffer));
        var file = new File(path);
        if(file.exists() && file.isDirectory()){
            if(!path.equals(lastDirectoryPath)){
                lastDirectoryPath = path;
                lastDirectoryResult = file.listFiles(File::isDirectory);
            }
            writePreamble(outputBuffer);
            outputBuffer.putInt(lastDirectoryResult == null ? 0 : lastDirectoryResult.length);
        }else{
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        }
    }

    private void handleGetDirectory(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        var path = fixPath(readString(inputBuffer));
        var file = new File(path);
        if(file.exists() && file.isDirectory()){
            if(!path.equals(lastDirectoryPath)){
                lastDirectoryPath = path;
                lastDirectoryResult = file.listFiles(File::isFile);
            }
            int fileIndex = inputBuffer.getInt();
            if(lastDirectoryResult == null || fileIndex < 0 || fileIndex >= lastDirectoryResult.length){
                writePreamble(outputBuffer, RESULT_INVALID_INPUT);
            }else{
                writePreamble(outputBuffer);
                writeString(outputBuffer, lastDirectoryResult[fileIndex].getName());
            }
        }else{
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        }
    }

    private String lastReadPath;
    private RandomAccessFile lastReadFile;
    private void handleReadFile(ByteBuffer inputBuffer, ByteBuffer outputBuffer, List<ByteBuffer> extraBuffers){
        try{
            var path = fixPath(readString(inputBuffer));
            var file = new File(path);
            long offset = inputBuffer.getLong();
            long size = inputBuffer.getLong();
            if(file.exists() && file.isFile()){
                if(!path.equals(lastReadPath)){
                    lastReadPath = path;
                    if(lastReadFile != null){
                        lastReadFile.close();
                    }
                    lastReadFile = new RandomAccessFile(file, "r");
                }

                if(offset > lastReadFile.length()){
                    writePreamble(outputBuffer, RESULT_INVALID_INPUT);
                    return;
                }
                lastReadFile.seek(offset);
                System.out.println(size);
                // 2GiB limit
                if(size > Integer.MAX_VALUE){
                    size = Integer.MAX_VALUE;
                }
                ByteBuffer buffer = Memory.allocateBuffer((int)size);
                long remaining = size;
                byte[] data = new byte[1024*1024];
                while(remaining > 1024*1024){
                    lastReadFile.readFully(data);
                    buffer.put(data);
                    remaining -= 1024*1024;
                }
                if(remaining > 0){
                    lastReadFile.readFully(data, 0, (int)remaining);
                    buffer.put(data, 0, (int)remaining);
                }

                buffer.limit((int)size);
                writePreamble(outputBuffer);
                outputBuffer.putLong(size);
                extraBuffers.add(buffer);
            }else{
                writePreamble(outputBuffer, RESULT_INVALID_INPUT);
            }
        }catch(IOException e){
            e.printStackTrace();
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        }
    }

    private void handleWriteFile(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        System.err.println("UNIMPLEMENTED");
    }

    private void handleCreate(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        System.err.println("UNIMPLEMENTED");
    }

    private void handleDelete(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        System.err.println("UNIMPLEMENTED");
    }

    private void handleRename(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        System.err.println("UNIMPLEMENTED");
    }

    //TODO
    private void handleGetSpecialPatchCount(ByteBuffer outputBuffer){
        writePreamble(outputBuffer);
        outputBuffer.putInt(2);
    }

    //TODO
    private void handleGetSpecialPath(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        int index = inputBuffer.getInt();
        if(index < 0 || index >= 2){
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        }else{
            writePreamble(outputBuffer);
            if(index == 0){
                writeString(outputBuffer, "Home");
                writeString(outputBuffer, "Home");
            }else{
                writeString(outputBuffer, "Downloads");
                writeString(outputBuffer, "Downloads");
            }
        }
    }

    private void handleSelectFile(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        System.err.println("UNIMPLEMENTED");
    }

    private void handleMax(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        System.err.println("UNIMPLEMENTED");
    }

    @Override
    public void close(){
        cleaner.clean();
    }

    private static final class State implements Runnable{
        private final Usb usb;
        private final long device;
        private long handle = NULL;

        private State(Usb usb, long device){
            this.usb = usb;
            this.device = device;
        }

        private void setHandle(long handle){
            this.handle = handle;
        }

        @Override
        public void run(){
            if(handle != NULL){
                usb.close(handle);
            }
            usb.unrefDevice(device);
        }
    }
}
