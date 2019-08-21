package net.gudenau.nx.ptree;

import net.gudenau.nx.ptree.util.Platform;

import java.io.File;
import java.net.URISyntaxException;

public class Launcher{
    public static void main(String[] args) throws Throwable{
        String executableName = Platform.getOS() == Platform.OS.WINDOWS ? "javaws.exe" : "java";
        String javaHome = System.getProperty("java.home");

        var processBuilder = new ProcessBuilder();
        processBuilder.command(
            javaHome + "/bin/" + executableName,
            "-p",
            new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath(),
            "-m",
            "PlatinumTree/net.gudenau.nx.ptree.PlatinumTree"
        );
        processBuilder.inheritIO();
        processBuilder.start();
    }
}
