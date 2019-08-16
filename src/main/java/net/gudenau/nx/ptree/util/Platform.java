package net.gudenau.nx.ptree.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.file.Files;

/**
 * Platform specific stuff.
 * */
public class Platform{
    /**
     * Load natives.
     * */
    public static void init(){
        var os = getOS();
        var arch = getArch();

        if(os == OS.UNKNOWN || arch == Arch.UNKNOWN){
            System.err.println("Unknown configuration");
            System.exit(0);
        }

        String nativePath = "/net/gudenau/nx/ptree/res/natives/" +
            os.name().toLowerCase() + "/" +
            arch.name().toLowerCase() + "." + os.extension;

        try{
            var tempPath = Files.createTempFile("ptree", os.extension);
            var tempFile = tempPath.toFile();

            var codeSource = Platform.class.getProtectionDomain().getCodeSource().getLocation();
            File file = new File(codeSource.toURI());

            if(file.isFile()){
                try(
                    var in = Platform.class.getResourceAsStream(nativePath);
                    var out = new FileOutputStream(tempFile)
                ){
                    out.getChannel().transferFrom(Channels.newChannel(in), 0, Long.MAX_VALUE);
                }
            }else{
                try(
                    var in = new FileInputStream(new File(file, "../../../resources/main" + nativePath));
                    var out = new FileOutputStream(tempFile)
                ){
                    out.getChannel().transferFrom(Channels.newChannel(in), 0, Long.MAX_VALUE);
                }
            }

            System.load(tempFile.getAbsolutePath());
        }catch(URISyntaxException | IOException e){
            throw new RuntimeException(e);
        }
    }

    private static OS os = null;
    /**
     * Gets the current OS type.
     *
     * @return The current OS
     * */
    public static OS getOS(){
        if(os == null){
            String name = System.getProperty("os.name").toLowerCase();
            if(name.contains("linux")){
                os = OS.LINUX;
            }else if(name.contains("window")){
                os = OS.WINDOWS;
            }else{
                System.err.println("Unknown OS: " + System.getProperty("os.name"));
                os = OS.UNKNOWN;
            }
        }
        return os;
    }

    private static Arch arch = null;
    /**
     * Gets the current OS architecture.
     *
     * @return The current OS architecture
     * */
    public static Arch getArch(){
        if(arch == null){
            String name = System.getProperty("os.arch").toLowerCase();
            if(name.contains("amd64")){
                arch = Arch.AMD64;
            }else if(name.contains("x86")){
                arch = Arch.X86;
            }else{
                System.err.println("Unknown arch: " + System.getProperty("os.arch"));
                arch = Arch.UNKNOWN;
            }
        }
        return arch;
    }

    private static PathConverter pathConverter;
    public static String convertPath(String drive, String file){
        if(pathConverter == null){
            pathConverter = getOS().createPathConverter();
        }
        return pathConverter.convertPath(drive, file);
    }

    public enum OS{
        LINUX("so", LinuxPathConverter.class),
        WINDOWS("dll", WindowsPathConverter.class),
        UNKNOWN("", NopPathConverter.class);

        private final String extension;
        private final Class<? extends PathConverter> converter;

        OS(String extension, Class<? extends PathConverter> converter){
            this.extension = extension;
            this.converter = converter;
        }

        public PathConverter createPathConverter(){
            try{
                var constructor = converter.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            }catch(ReflectiveOperationException e){
                throw new RuntimeException(e);
            }
        }
    }

    public enum Arch{
        AMD64,
        X86,
        UNKNOWN
    }

    @FunctionalInterface
    private interface PathConverter{
        String convertPath(String drive, String path);
    }

    private static class NopPathConverter implements PathConverter{
        @Override
        public String convertPath(String drive, String path){
            return drive + ":" + path;
        }
    }

    private static class LinuxPathConverter implements PathConverter{
        @Override
        public String convertPath(String drive, String path){
            return path.replaceAll("\\/{2,}", "/");
        }
    }

    private static class WindowsPathConverter implements PathConverter{
        @Override
        public String convertPath(String drive, String path){
            return drive + ":" + path.replaceAll("/+", "\\");
        }
    }
}
