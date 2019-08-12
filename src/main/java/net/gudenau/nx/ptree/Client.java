package net.gudenau.nx.ptree;

import net.gudenau.nx.ptree.usb.Usb;
import net.gudenau.nx.ptree.util.FileHelper;
import net.gudenau.nx.ptree.util.Memory;
import net.gudenau.nx.ptree.util.Platform;

import javax.swing.JFileChooser;
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
import java.util.Optional;

import static net.gudenau.nx.ptree.util.Memory.NULL;

/**
 * A Switch client, the primary chunk of code.
 * */
class Client implements Runnable, AutoCloseable{
    private static final int COMMAND_BUFFER_SIZE = 0x1000;
    private static final int LIBUSB_BUG_SIZE = 0x00004000;
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

    //TODO
    private final boolean readOnly = false;

    private final List<Favorite> favorites = new ArrayList<>();

    private FileSystem filesystem = FileSystems.getDefault();;
    private List<Path> roots;

    Client(Usb usb, long device){
        this.usb = usb;
        this.device = device;
        state = new State(usb, device);
        cleaner = Memory.registerCleaner(this, state);

        favorites.add(new Favorite("Home", "Root", System.getProperty("user.home")));
        favorites.add(new Favorite("Downloads", "Root", System.getProperty("user.home") + "/Downloads"));
        favorites.add(new Favorite("Temp", "Root", System.getProperty("java.io.tmpdir")));
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
                int transferred = transfer(PIPE_READ, inputBuffer);
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
                    //System.err.println("Unknown command: " + commandId);
                    continue;
                }
                Command command = Command.values()[commandId];
                //System.out.println("Handling " + command.name());

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
                        handleSelectFile(outputBuffer);
                        break;
                    case Max:
                        handleMax(inputBuffer, outputBuffer);
                        break;
                }

                inputBuffer.position(0);
                outputBuffer.position(0);
                transfer(PIPE_WRITE, outputBuffer);
                for(var buffer : extraBuffers){
                    transfer(PIPE_WRITE, buffer);
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

    private int transfer(byte endpoint, ByteBuffer buffer){
        int transferSize = buffer.limit() - buffer.position();

        if(transferSize > LIBUSB_BUG_SIZE){
            int oldLimit = buffer.limit();
            int oldPosition = buffer.position();

            int position = oldPosition;

            while(transferSize > LIBUSB_BUG_SIZE){
                buffer.limit(position + LIBUSB_BUG_SIZE);
                buffer.position(position);
                if(usb.bulkTransfer(handle, endpoint, buffer, 0) <= 0){
                    return -1;
                }
                transferSize -= LIBUSB_BUG_SIZE;
                position += LIBUSB_BUG_SIZE;
            }
            if(transferSize > 0){
                buffer.position(position);
                buffer.limit(oldLimit);
                if(usb.bulkTransfer(handle, endpoint, buffer, 0) <= 0){
                    return -1;
                }
            }
            buffer.position(oldPosition).limit(oldLimit);
            return transferSize;
        }else{
            return usb.bulkTransfer(handle, endpoint, buffer, 0) <= 0 ? -1 : transferSize;
        }
    }

    private void writePreamble(ByteBuffer buffer, int result){
        buffer.putInt(MAGIC_WRITE);
        buffer.putInt(result);
    }

    private void writePreamble(ByteBuffer buffer){
        writePreamble(buffer, RESULT_SUCCESS);
    }

    private void writeString(ByteBuffer buffer, String value){
        var bytes = value.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }

    private String readString(ByteBuffer buffer){
        int size = buffer.getInt();
        var data = new byte[size];
        buffer.get(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    private String readPath(ByteBuffer buffer){
        var path = readString(buffer);

        int separator = path.indexOf(":");
        var drive = path.substring(0, separator);
        var file = path.substring(separator + 1);

        System.out.printf("    path: %s:%s\n", drive, file);

        final String finalDrive = drive;
        Optional<Favorite> favorite = favorites.stream()
            .filter((fav)->fav.getName().equals(finalDrive))
            .findFirst();
        if(favorite.isPresent()){
            var favoriteValue = favorite.get();
            file = favoriteValue.getPath() + "/" + file;
            drive = favoriteValue.getDrive();
        }

        System.out.printf("new path: %s:%s\n", drive, file);
        String newPath = Platform.convertPath(drive, file);
        System.out.printf(" convert: %s\n", newPath);
        return newPath;
    }

    private ByteBuffer readBuffer(int size){
        var buffer = Memory.allocateBuffer(size);
        transfer(PIPE_READ, buffer);
        return buffer;
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

    private void handleStatPath(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        var file = new File(readPath(inputBuffer));
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
        var path = readPath(inputBuffer);
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
        var path = readPath(inputBuffer);
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
        var path = readPath(inputBuffer);
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
        var path = readPath(inputBuffer);
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
            var path = readPath(inputBuffer);
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

                lastReadFile.seek(offset);
                ByteBuffer buffer = Memory.allocateBuffer((int)size);
                int position = 0;
                while(position < size){
                    buffer.position(position);
                    int read = FileHelper.read(lastReadFile, buffer);
                    if(read <= 0){
                        break;
                    }
                    position += read;
                }

                buffer.limit(position);
                buffer.position(0);
                writePreamble(outputBuffer);
                outputBuffer.putLong(position);
                extraBuffers.add(buffer);
            }else{
                writePreamble(outputBuffer, RESULT_INVALID_INPUT);
            }
        }catch(IOException e){
            e.printStackTrace();
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        }
    }

    private String lastWritePath;
    private RandomAccessFile lastWriteFile;
    private void handleWriteFile(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        if(readOnly){
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
            return;
        }
        try{
            var path = readPath(inputBuffer);
            var size = inputBuffer.getLong();
            var file = new File(path);
            var buffer = readBuffer((int)size);
            System.out.println("Writing " + path);
            try{
                if(!path.equals(lastWritePath)){
                    lastWritePath = path;
                    var parent = file.getParentFile();
                    if(parent != null && !parent.exists()){
                        if(!parent.mkdirs()){
                            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
                            return;
                        }
                    }
                    if(!file.exists()){
                        if(!file.createNewFile()){
                            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
                            return;
                        }
                    }
                    if(lastWriteFile != null){
                        lastWriteFile.close();
                    }
                    lastWriteFile = new RandomAccessFile(file, "rw");
                }

                while(buffer.hasRemaining()){
                    int transferred = FileHelper.write(lastWriteFile, buffer);
                    if(transferred < 0){
                        writePreamble(outputBuffer, RESULT_INVALID_INPUT);
                        return;
                    }
                    buffer.position(buffer.position() + transferred);
                }
            }finally{
                Memory.freeBuffer(buffer);
            }
        }catch(IOException e){
            e.printStackTrace();
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        }
    }

    private void handleCreate(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        if(readOnly){
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
            return;
        }
        try{
            var type = inputBuffer.getInt();
            var path = readPath(inputBuffer);
            var file = new File(path);
            if(type == 1){
                var parent = file.getParentFile();
                if(parent != null && !parent.exists()){
                    if(!parent.mkdirs()){
                        writePreamble(outputBuffer, RESULT_INVALID_INPUT);
                        return;
                    }
                }
                if(file.exists()){
                    if(!file.delete()){
                        writePreamble(outputBuffer, RESULT_INVALID_INPUT);
                        return;
                    }
                }
                if(file.createNewFile()){
                    writePreamble(outputBuffer);
                }else{
                    writePreamble(outputBuffer, RESULT_INVALID_INPUT);
                }
            }else if(type == 2){
                if(!file.exists()){
                    if(file.mkdirs()){
                        writePreamble(outputBuffer);
                    }else{
                        writePreamble(outputBuffer, RESULT_INVALID_INPUT);
                    }
                }
            }else{
                writePreamble(outputBuffer, RESULT_INVALID_INPUT);
            }
        }catch(IOException e){
            e.printStackTrace();
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        }
    }

    private void handleDelete(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        if(readOnly){
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
            return;
        }
        var type = inputBuffer.getInt();
        var path = readPath(inputBuffer);
        var file = new File(path);
        if(type != 1 && type != 2){
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        }else{
            if(delete(file)){
                writePreamble(outputBuffer);
            }else{
                writePreamble(outputBuffer, RESULT_INVALID_INPUT);
            }
        }
    }

    private boolean delete(File file){
        if(file.isDirectory()){
            var files = file.listFiles();
            if(files != null){
                for(var child : files){
                    if(!delete(child)){
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    private void handleRename(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        if(readOnly){
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
            return;
        }
        var type = inputBuffer.getInt();
        var oldPath = readPath(inputBuffer);
        var newPath = readPath(inputBuffer);
        var oldFile = new File(oldPath);
        var newFile = new File(newPath);
        if(type != 1 && type != 2){
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        }else{
            if(oldFile.renameTo(newFile)){
                writePreamble(outputBuffer);
            }else{
                writePreamble(outputBuffer, RESULT_INVALID_INPUT);
            }
        }
    }

    private void handleGetSpecialPatchCount(ByteBuffer outputBuffer){
        writePreamble(outputBuffer);
        outputBuffer.putInt(favorites.size());
    }

    private void handleGetSpecialPath(ByteBuffer inputBuffer, ByteBuffer outputBuffer){
        int index = inputBuffer.getInt();
        if(index < 0 || index >= favorites.size()){
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        }else{
            writePreamble(outputBuffer);
            var favorite = favorites.get(index);
            var name = favorite.getName();
            writeString(outputBuffer, name);
            writeString(outputBuffer, name);
        }
    }

    private void handleSelectFile(ByteBuffer outputBuffer){
        var chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        if(chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
            writePreamble(outputBuffer);
            writeString(outputBuffer, chooser.getSelectedFile().getAbsolutePath());
        }else{
            writePreamble(outputBuffer, RESULT_INVALID_INPUT);
        }
    }

    // What is this even for?
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
